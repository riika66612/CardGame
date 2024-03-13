package com.ruoyi.framework.websocket;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import com.alibaba.nacos.shaded.com.google.gson.JsonObject;
import com.alibaba.nacos.shaded.com.google.gson.JsonParser;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.framework.websocket.vo.GamePlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * websocket 消息处理
 *
 * @author kokor
 */
@Component
@ServerEndpoint("/websocket/message/{userName}")
public class WebSocketServer {
    /**
     * WebSocketServer 日志控制器
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketServer.class);

    /**
     * 默认最多允许同时在线人数100
     */
    public static int socketMaxOnlineCount = 8;
    public static boolean gameStarted = false;

    private static Semaphore socketSemaphore = new Semaphore(socketMaxOnlineCount);

    // 在线用户
    private static List<String> userNameList = new ArrayList<>();
    private static List<GamePlayer> players = new ArrayList<>();
    // 游戏中退出的玩家
    private static List<String> exitPlayers = new ArrayList<>();

    // 确认数量
    private static int confirm = 0;
    // 存活人数
    private static int livedPlayer = 0;
    // 顺序（true为+，false为-）
    private static boolean order = true;
    // “这不合理”使用数量统计
    private static int okNumber = 0;
    // 掀被子抽牌统计
    private static int drawCardCount = 0;

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("userName") String userName) throws Exception {
        if (gameStarted) {
            LOGGER.error("游戏已开始");
            session.close();
            throw new ServiceException("游戏已经开始");
        } else {
            boolean semaphoreFlag = false;
            // 尝试获取信号量
            semaphoreFlag = SemaphoreUtils.tryAcquire(socketSemaphore);
            if (!semaphoreFlag) {
                // 未获取到信号量
                LOGGER.error("当前在线人数超过限制数- {}", socketMaxOnlineCount);
                // WebSocketUsers.sendMessageToUserByText(session, "当前在线人数超过限制数：" + socketMaxOnlineCount);
                session.close();
                throw new ServiceException("玩家数已满");
            } else {
                // 添加用户
                WebSocketUsers.put(userName, session);

                LOGGER.info("用户" + userName + "连接，当前在线的用户数为" + WebSocketUsers.getUsers().size());
                // 不存在则添加
                if (!userNameList.contains(userName)) {
                    // 添加用户至列表
                    userNameList.add(userName);
                    LOGGER.info(userNameList.toString());

                    // 创建一个新的玩家
                    GamePlayer player = new GamePlayer();
                    player.setUserName(userName).setUserNumber(userNameList.indexOf(userName) + 1).setHand(0);
                    // 添加至玩家列表
                    players.add(player);
                }
                //LOGGER.info("\n 建立连接 - {}", session);
                //LOGGER.info("\n 当前人数 - {}", WebSocketUsers.getUsers().size());
                // WebSocketUsers.sendMessageToUserByText(session, "连接成功");
            }
        }
    }

    /**
     * 连接关闭时处理
     */
    @OnClose
    public void onClose(Session session, @PathParam("userName") String userName) {
        //LOGGER.info("\n 关闭连接 - {}", session);
        // 移除用户
        //WebSocketUsers.remove(session.getId());
        WebSocketUsers.remove(userName);
        // 获取到信号量则需释放
        SemaphoreUtils.release(socketSemaphore);
        LOGGER.info("用户" + userName + "断开连接，当前在线的用户数为" + WebSocketUsers.getUsers().size());
        if (gameStarted) {
            if (!exitPlayers.contains(userName)) {
                // 记住该用户
                exitPlayers.add(userName);
                // 标记玩家为已出局
                players.get(userNameList.indexOf(userName)).setLive(false);
                // 存活玩家人数减少
                livedPlayer -= 1;
            }
        } else {
            if (userNameList.contains(userName)) {
                int idx = userNameList.indexOf(userName);
                for (int i = idx; i < userNameList.size(); i++) {
                    players.get(i).setUserNumber(players.get(i).getUserNumber() - 1);
                }
                // 移除玩家
                players.remove(idx);
                // 将用户从列表移除
                userNameList.remove(userName);
            }
            LOGGER.info(userNameList.toString());
        }

        if (userNameList.size() == 0) {
            gameStarted = false;
        }

        // 用户退出后，更新其它用户的列表显示
        JsonObject sending = new JsonObject();
        sending.addProperty("msgType", "-1");
        sending.addProperty("msgContent", players.toString());
        sending.addProperty("msgTo", "All Players");
        WebSocketUsers.sendMessageToUsersByText(sending.toString());

        // 用户退出导致游戏结束
        victoryCheck();
    }

    /**
     * 抛出异常时处理
     */
    @OnError
    public void onError(Session session, Throwable exception) throws Exception {
        if (session.isOpen()) {
            // 关闭连接
            session.close();
        }
        String sessionId = session.getId();
        //LOGGER.info("\n 连接异常 - {}", sessionId);
        //LOGGER.info("\n 异常信息 - {}", exception);
        // 移出用户
        WebSocketUsers.remove(sessionId);
        // 获取到信号量则需释放
        SemaphoreUtils.release(socketSemaphore);
    }

    /**
     * 服务器接收到客户端消息时调用的方法
     */
    @OnMessage
    public void onMessage(String message, Session session, @PathParam("userName") String userName) {
        //String msg = message.replace("你", "我").replace("吗", "");
        //WebSocketUsers.sendMessageToUserByText(session, message);
        //WebSocketUsers.sendMessageToUsersByText(message);
        //WebSocketUsers.sendMessageToOtherUserByText(message, "ry");

        JsonParser jsonParser = new JsonParser();
        JsonObject receive = jsonParser.parse(message).getAsJsonObject();
        String msgType = receive.get("msgType").getAsString();

        /*
        接收到的消息类别
            0: 有人进入房间
            1: 开始游戏
            2: 身份选择
            3: 起始手牌抽取
            4: 回合结束
            5: 有人使用了“求饶”
            6: 有人因宿管或弱点而出局
            7: 有人的手牌数发生变化/身份变化
            8: 有人放了张弱点在牌堆顶
            9: 有人打出了一张手牌 -> “拿来吧你”
            10: 统计“这不合理”的使用 -> “拿来吧你”
            11: 可继续放置弱点
            90: 身份抽取提醒（防异步）
            99: 游戏结束

            023: “拿来吧你”
            024: “掀被子”
            026: “反转”
            027: “猜你想要”
            028: “性情大变”

            100: “我是好人”
            101: “夺笋”
            103: “对子”
            104: “三条”
        */
        switch (msgType) {
            case "0": {
                JsonObject sending = new JsonObject();
                sending.addProperty("msgType", "0");
                sending.addProperty("msgContent", players.toString());
                sending.addProperty("msgTo", "All Players");
                WebSocketUsers.sendMessageToUsersByText(sending.toString());
                break;
            }
            case "1": {
                gameStarted = true;
                livedPlayer = userNameList.size();
                drawCardCount = 0;
                confirm = 0;
                okNumber = 0;

                JsonObject start = new JsonObject();
                start.addProperty("msgType", "1");
                start.addProperty("msgContent", "Game Start");
                start.addProperty("msgTo", "All Player");
                start.addProperty("msgExtra", receive.get("msgExtra").getAsInt());
                WebSocketUsers.sendMessageToUsersByText(start.toString());

                JsonObject sending = new JsonObject();
                sending.addProperty("msgType", "90");
                sending.addProperty("msgContent", "Game Start");
                sending.addProperty("msgTo", 1);
                sending.addProperty("msgExtra", receive.get("msgExtra").getAsInt());
                WebSocketUsers.sendMessageToOtherUserByText(sending.toString(), userNameList.get(0));
                break;
            }
            case "2": {
                // 修改身份信息
                players.get(userNameList.indexOf(userName)).setIdentity(receive.get("msgContent").getAsInt());
                players.get(userNameList.indexOf(userName)).setSubIdentity(receive.get("msgExtra").getAsInt());

                confirm += 1;
                if (confirm == userNameList.size()) {
                    JsonObject sending = new JsonObject();
                    sending.addProperty("msgType", "2");
                    sending.addProperty("msgContent", players.toString());
                    sending.addProperty("msgTo", "All Players");
                    WebSocketUsers.sendMessageToUsersByText(sending.toString());
                    confirm = 0;

                    JsonObject draw = new JsonObject();
                    draw.addProperty("msgType", "91");
                    draw.addProperty("msgContent", "Draw Initial Card");
                    draw.addProperty("msgTo", 1);
                    WebSocketUsers.sendMessageToOtherUserByText(draw.toString(), userNameList.get(0));
                }
                break;
            }
            case "3": {
                // 修改手牌信息
                players.get(userNameList.indexOf(userName)).setHand(5);

                confirm += 1;
                if (receive.get("msgTo").getAsInt() <= userNameList.size()) {
                    JsonObject sending = new JsonObject();
                    sending.addProperty("msgType", "91");
                    sending.addProperty("msgContent", "Draw Initial Card");
                    sending.addProperty("msgTo", receive.get("msgTo").getAsInt());
                    WebSocketUsers.sendMessageToOtherUserByText(sending.toString(), userNameList.get(receive.get("msgTo").getAsInt() - 1));
                }

                if (confirm == userNameList.size()) {
                    // 创建一个Random对象
                    Random random = new Random();
                    // 从随机玩家开始
                    int randomInt = random.nextInt(userNameList.size());
                    // 修改起始玩家信息
                    players.get(randomInt).setTurn(true);

                    JsonObject sending = new JsonObject();
                    sending.addProperty("msgType", "3");
                    sending.addProperty("msgContent", players.toString());
                    sending.addProperty("msgTo", "All Players");
                    WebSocketUsers.sendMessageToUsersByText(sending.toString());
                    confirm = 0;
                    // 每次游戏开始都重置成正常顺序
                    order = true;
                }
                break;
            }
            case "4": {
                int nowTurn = receive.get("msgFrom").getAsInt();
                // 声明当前用户的回合结束
                players.get(nowTurn).setTurn(false);
                changeTurn(nowTurn);
                break;
            }
            case "5": {
                // 被炸的人手牌 -2
                players.get(receive.get("msgFrom").getAsInt()).setHand(players.get(receive.get("msgFrom").getAsInt()).getHand() - 2);

                JsonObject sending = new JsonObject();
                sending.addProperty("msgType", "5");
                sending.addProperty("msgContent", userName + "差点就炸了");
                sending.addProperty("msgExtra", players.toString());
                sending.addProperty("msgTo", "All Players");
                WebSocketUsers.sendMessageToUsersByText(sending.toString());
                break;
            }
            case "6": {
                // “掀被子”抽牌计数清空
                drawCardCount = 0;

                int nowTurn = receive.get("msgFrom").getAsInt();
                // 标记玩家已阵亡
                players.get(nowTurn).setLive(false);
                // 该玩家回合结束
                players.get(nowTurn).setTurn(false);
                // 该玩家手牌清空
                players.get(nowTurn).setHand(0);

                // 更新前台显示
                JsonObject sending = new JsonObject();
                sending.addProperty("msgType", "6");
                sending.addProperty("msgContent", userName + "被送走了");
                sending.addProperty("msgTo", "All Players");
                sending.addProperty("msgExtra", players.toString());
                WebSocketUsers.sendMessageToUsersByText(sending.toString());

                // 转移到下一位玩家的回合
                changeTurn(nowTurn);

                // 存活人数减少
                livedPlayer -= 1;
                // 判断游戏是否结束
                victoryCheck();
                break;
            }
            case "7": {
                if (receive.get("msgContent").getAsString().startsWith("Drop") || receive.get("msgContent").getAsString().startsWith("Set")) {
                    // 如果是弃牌
                    // 当前玩家手牌数 -1
                    players.get(receive.get("msgFrom").getAsInt()).setHand(players.get(receive.get("msgFrom").getAsInt()).getHand() - 1);
                } else if (receive.get("msgContent").getAsString().startsWith("Draw")) {
                    // 如果是抽牌
                    // 当前玩家手牌数 +1
                    players.get(receive.get("msgFrom").getAsInt()).setHand(players.get(receive.get("msgFrom").getAsInt()).getHand() + 1);
                } else if (receive.get("msgContent").getAsString().startsWith("Use")) {
                    // 如果是出牌
                    // 当前玩家手牌数 -X (X为同时打出的牌的张数)
                    players.get(receive.get("msgFrom").getAsInt()).setHand(players.get(receive.get("msgFrom").getAsInt()).getHand() - receive.get("msgExtra").getAsInt());
                }

                // 更新前台显示
                JsonObject sending = new JsonObject();
                sending.addProperty("msgType", "7");
                sending.addProperty("msgContent", "Hand Change");
                sending.addProperty("msgTo", "All Player");
                sending.addProperty("msgExtra", players.toString());
                WebSocketUsers.sendMessageToUsersByText(sending.toString());
                break;
            }
            case "8": {
                // 通知全场
                JsonObject sending = new JsonObject();
                sending.addProperty("msgType", "8");
                sending.addProperty("msgContent", userName + "很坏，在牌堆顶放了张弱点牌");
                sending.addProperty("msgTo", "All Player");
                WebSocketUsers.sendMessageToUsersByText(sending.toString());
                break;
            }
            case "9": {
                //int nowTurn = receive.get("msgFrom").getAsInt();
                //if (order) {
                //    if (++nowTurn >= players.size()) {
                //        nowTurn = 0;
                //    }
                //} else {
                //    if (--nowTurn < 0) {
                //        nowTurn = players.size() - 1;
                //    }
                //}

                // 询问“这不合理”
                JsonObject sending = new JsonObject();
                sending.addProperty("msgType", "9");
                if (receive.get("msgExtra").getAsString().equals("好人")) {
                    if (receive.get("msgContent").getAsString().startsWith("0")) {
                        sending.addProperty("msgContent", userName + " 使用了一张牌");
                    } else {
                        sending.addProperty("msgContent", userName + " 使用了一个组合技");
                    }
                } else {
                    if (receive.get("msgContent").getAsString().startsWith("0")) {
                        sending.addProperty("msgContent", userName + " 对 " + receive.get("msgExtra").getAsString() + " 使用了一张牌");
                    } else {
                        sending.addProperty("msgContent", userName + " 对 " + receive.get("msgExtra").getAsString() + " 使用了一个组合技");
                    }
                }
                sending.addProperty("msgExtra", receive.get("msgContent").getAsString());
                //sending.addProperty("msgTo", userNameList.get(nowTurn));
                sending.addProperty("msgTo", "Other Player");
                sending.addProperty("msgFrom", userName);
                //WebSocketUsers.sendMessageToOtherUserByText(sending.toString(), userNameList.get(nowTurn));
                WebSocketUsers.sendMessageToUsersByText(sending.toString());
                break;
            }
            case "10": {
                if (receive.get("msgContent").getAsString().startsWith("OK")) {
                    confirm += 1;

                    if ((confirm == livedPlayer - 1) && (okNumber % 2 == 0)) {
                        // 可以使用该卡牌
                        JsonObject sending = new JsonObject();
                        sending.addProperty("msgType", "10");
                        sending.addProperty("msgContent", "Can Use");
                        sending.addProperty("msgTo", "You");
                        WebSocketUsers.sendMessageToUsersByText(sending.toString());

                        confirm = 0;
                        okNumber = 0;
                    } else if ((confirm == livedPlayer - 1) && (okNumber % 2 == 1)) {
                        //LOGGER.info("行为无效");

                        // 不可以使用该卡牌
                        JsonObject sending = new JsonObject();
                        sending.addProperty("msgType", "10");
                        sending.addProperty("msgContent", "No");
                        sending.addProperty("msgTo", "You");
                        WebSocketUsers.sendMessageToUsersByText(sending.toString());

                        confirm = 0;
                        okNumber = 0;
                    }
                } else if (receive.get("msgContent").getAsString().startsWith("Not")) {
                    okNumber += 1;

                    // 不可以使用该卡牌 => 打出了一张“这不合理”
                    JsonObject sending = new JsonObject();
                    sending.addProperty("msgType", "9");
                    sending.addProperty("msgContent", userName + " 觉得这不合理");
                    sending.addProperty("msgExtra", "022");
                    sending.addProperty("msgTo", "Other Player");
                    sending.addProperty("msgFrom", userName);
                    WebSocketUsers.sendMessageToUsersByText(sending.toString());

                    confirm = 0;
                }
                break;
            }
            case "11": {
                JsonObject sending = new JsonObject();
                sending.addProperty("msgType", "11");
                WebSocketUsers.sendMessageToUsersByText(sending.toString());
                break;
            }
            case "90": {
                if (receive.get("msgTo").getAsInt() <= userNameList.size()) {
                    JsonObject sending = new JsonObject();
                    sending.addProperty("msgType", "90");
                    sending.addProperty("msgContent", "Draw Identity");
                    sending.addProperty("msgTo", receive.get("msgTo").getAsInt());
                    WebSocketUsers.sendMessageToOtherUserByText(sending.toString(), userNameList.get(receive.get("msgTo").getAsInt() - 1));
                }
                break;
            }
            case "99": {
                JsonObject sending = new JsonObject();
                sending.addProperty("msgType", "0");
                sending.addProperty("msgContent", players.toString());
                sending.addProperty("msgTo", userName);
                WebSocketUsers.sendMessageToOtherUserByText(sending.toString(), userName);
                break;
            }
            case "023": {
                // 交换手牌
                //LOGGER.info(receive.get("msgContent").getAsString());
                JsonObject sending = new JsonObject();
                sending.addProperty("msgType", "023");
                sending.addProperty("msgContent", receive.get("msgContent").getAsString());
                sending.addProperty("msgTo", receive.get("msgTo").getAsString());
                sending.addProperty("msgFrom", userName);
                WebSocketUsers.sendMessageToOtherUserByText(sending.toString(), receive.get("msgTo").getAsString());

                if (receive.get("msgExtra").getAsString().equals("0")) {
                    int handFrom = players.get(userNameList.indexOf(userName)).getHand();
                    int handTo = players.get(userNameList.indexOf(receive.get("msgTo").getAsString())).getHand();
                    // 更新手牌数量
                    players.get(userNameList.indexOf(userName)).setHand(handTo);
                    players.get(userNameList.indexOf(receive.get("msgTo").getAsString())).setHand(handFrom);
                    // 更新前台显示
                    JsonObject subSending = new JsonObject();
                    subSending.addProperty("msgType", "7");
                    subSending.addProperty("msgContent", "Hand Change");
                    subSending.addProperty("msgTo", "All Player");
                    subSending.addProperty("msgExtra", players.toString());
                    WebSocketUsers.sendMessageToUsersByText(subSending.toString());
                }
                break;
            }
            case "024": {
                if (receive.get("msgContent").getAsString().startsWith("Clear")) {
                    // 清空抽牌数
                    drawCardCount = 0;
                } else {
                    // 抽牌数增加
                    drawCardCount += 2;

                    int nowTurn = receive.get("msgFrom").getAsInt();
                    // 结束当前玩家回合
                    players.get(nowTurn).setTurn(false);
                    // 切换到下一个人的回合
                    nowTurn = changeTurn(nowTurn);

                    JsonObject sending = new JsonObject();
                    sending.addProperty("msgType", "024");
                    sending.addProperty("msgContent", drawCardCount);
                    sending.addProperty("msgTo", userNameList.get(nowTurn));
                    sending.addProperty("msgFrom", userName);
                    WebSocketUsers.sendMessageToOtherUserByText(sending.toString(), userNameList.get(nowTurn));
                }
                break;
            }
            case "026": {
                // 反转顺序
                order = !order;

                JsonObject sending = new JsonObject();
                sending.addProperty("msgType", "026");
                WebSocketUsers.sendMessageToOtherUserByText(sending.toString(), userName);
                break;
            }
            case "027": {
                // 发送手牌
                JsonObject sending = new JsonObject();
                sending.addProperty("msgType", "027");
                sending.addProperty("msgContent", receive.get("msgContent").getAsString());
                sending.addProperty("msgTo", receive.get("msgTo").getAsString());
                sending.addProperty("msgFrom", userName);
                WebSocketUsers.sendMessageToOtherUserByText(sending.toString(), receive.get("msgTo").getAsString());

                // 更新手牌数量
                // 送出卡牌的人手牌 -1
                players.get(userNameList.indexOf(userName)).setHand(players.get(userNameList.indexOf(userName)).getHand() - 1);
                // 收到卡牌的人手牌 +1
                players.get(userNameList.indexOf(receive.get("msgTo").getAsString())).setHand(players.get(userNameList.indexOf(receive.get("msgTo").getAsString())).getHand() + 1);
                // 更新前台显示
                JsonObject subSending = new JsonObject();
                subSending.addProperty("msgType", "7");
                subSending.addProperty("msgContent", "Hand Change");
                subSending.addProperty("msgTo", "All Player");
                subSending.addProperty("msgExtra", players.toString());
                WebSocketUsers.sendMessageToUsersByText(subSending.toString());
                break;
            }
            case "028": {
                int selectedIndex = userNameList.indexOf(receive.get("msgExtra").getAsString());
                // 获取某人的隐藏身份
                int subId = players.get(selectedIndex).getSubIdentity();
                // 将其隐藏身份去除
                players.get(selectedIndex).setSubIdentity(0);

                // 将其隐藏身份给出牌人
                players.get(receive.get("msgFrom").getAsInt()).setIdentity(subId);

                // 更新前台显示
                JsonObject sending = new JsonObject();
                sending.addProperty("msgType", "028");
                sending.addProperty("msgContent", "Identity Change");
                sending.addProperty("msgTo", "All Player");
                sending.addProperty("msgExtra", players.toString());
                WebSocketUsers.sendMessageToUsersByText(sending.toString());
                break;
            }
            case "100":
                // 获得当前回合玩家
                int nowTurn = receive.get("msgFrom").getAsInt();
                // 拿到了牌
                if (receive.get("msgTo").getAsString().startsWith("Thank")) {
                    players.get(nowTurn).setHand(players.get(nowTurn).getHand() + 1);

                    // 更新前台显示
                    JsonObject subSending = new JsonObject();
                    subSending.addProperty("msgType", "7");
                    subSending.addProperty("msgContent", "Hand Change");
                    subSending.addProperty("msgTo", "All Player");
                    subSending.addProperty("msgExtra", players.toString());
                    WebSocketUsers.sendMessageToUsersByText(subSending.toString());
                }
                // 寻找下一位活人
                nowTurn = takeCardByTurn(nowTurn, receive);

                // 通知一下位玩家
                JsonObject sending = new JsonObject();
                sending.addProperty("msgType", "100");
                sending.addProperty("msgContent", receive.get("msgContent").getAsInt());
                sending.addProperty("msgTo", userNameList.get(nowTurn));
                sending.addProperty("msgExtra", receive.get("msgExtra").getAsString());
                WebSocketUsers.sendMessageToOtherUserByText(sending.toString(), userNameList.get(nowTurn));
                break;
            case "101": {
                // 转发给所有玩家
                WebSocketUsers.sendMessageToUsersByText(receive.toString());
                break;
            }
            case "103": {
                if (receive.get("msgExtra").getAsString().equals("Send")) {
                    WebSocketUsers.sendMessageToOtherUserByText(receive.toString(), receive.get("msgTo").getAsString());
                } else {
                    // 送出卡牌的人手牌 -1
                    players.get(receive.get("msgFrom").getAsInt()).setHand(players.get(receive.get("msgFrom").getAsInt()).getHand() - 1);
                    // 收到卡牌的人手牌 +1
                    players.get(receive.get("msgTo").getAsInt()).setHand(players.get(receive.get("msgTo").getAsInt()).getHand() + 1);

                    // 更新前台显示（同时将卡牌送出）
                    receive.addProperty("msgSub", players.toString());
                    WebSocketUsers.sendMessageToUsersByText(receive.toString());
                }
                break;
            }
            case "104": {
                if (receive.get("msgExtra").getAsString().equals("Send")) {
                    WebSocketUsers.sendMessageToOtherUserByText(receive.toString(), receive.get("msgTo").getAsString());
                } else {
                    // 如果对方有索要的手牌，两人手牌发生变化
                    if (receive.get("msgContent").getAsString().startsWith("0")) {
                        // 送出卡牌的人手牌 -1
                        players.get(receive.get("msgFrom").getAsInt()).setHand(players.get(receive.get("msgFrom").getAsInt()).getHand() - 1);
                        // 收到卡牌的人手牌 +1
                        players.get(receive.get("msgTo").getAsInt()).setHand(players.get(receive.get("msgTo").getAsInt()).getHand() + 1);
                    }

                    // 更新前台显示（同时将卡牌送出）
                    receive.addProperty("msgSub", players.toString());
                    WebSocketUsers.sendMessageToUsersByText(receive.toString());
                }
                break;
            }
        }
    }

    // 轮流拿牌
    public int takeCardByTurn(int nowTurn, JsonObject receive) {
        // 判断顺序
        if (receive.get("msgExtra").getAsString().equals("0")) {
            // 当前回合顺序
            if (order) {
                if (++nowTurn >= players.size()) {
                    nowTurn = 0;
                }
            } else {
                if (--nowTurn < 0) {
                    nowTurn = players.size() - 1;
                }
            }
        } else {
            // 当前回合逆序
            if (!order) {
                if (++nowTurn >= players.size()) {
                    nowTurn = 0;
                }
            } else {
                if (--nowTurn < 0) {
                    nowTurn = players.size() - 1;
                }
            }
        }
        // 如果这个人还活着
        if (players.get(nowTurn).getLive()) {
            // 直接返回结果
            return nowTurn;
        } else {
            // 如果这个人寄了，就返回再下一家
            return takeCardByTurn(nowTurn, receive);
        }
    }

    // 回合转移
    public int changeTurn(int nowTurn) {
        if (order) {
            if (++nowTurn >= players.size()) {
                nowTurn = 0;
            }
        } else {
            if (--nowTurn < 0) {
                nowTurn = players.size() - 1;
            }
        }
        if (players.get(nowTurn).getLive()) {
            players.get(nowTurn).setTurn(true);

            JsonObject sending = new JsonObject();
            sending.addProperty("msgType", "4");
            sending.addProperty("msgContent", players.toString());
            sending.addProperty("msgTo", "All Players");
            WebSocketUsers.sendMessageToUsersByText(sending.toString());
            return nowTurn;
        } else {
            nowTurn = changeTurn(nowTurn);
            return nowTurn;
        }
    }

    // 判断游戏是否结束
    public void victoryCheck() {
        LOGGER.info(livedPlayer + "");
        if (livedPlayer <= 1 && gameStarted) {
            gameStarted = false;

            // 最后留下的胜利者
            String victory = "";
            // 找出胜利玩家
            for (GamePlayer player : players) {
                if (player.getLive()) {
                    victory = player.getUserName();
                }
            }

            // 初始化玩家列表
            userNameList.removeAll(exitPlayers);
            exitPlayers.clear();
            players.clear();
            for (String name : userNameList) {
                players.add(new GamePlayer(name, userNameList.indexOf(name) + 1, 0, 0, 0));
            }

            // 向前台发送消息提醒
            JsonObject end = new JsonObject();
            end.addProperty("msgType", "99");
            end.addProperty("msgContent", victory);
            end.addProperty("msgTo", "All Players");
            WebSocketUsers.sendMessageToUsersByText(end.toString());
        }
    }
}


