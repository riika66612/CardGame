package com.ruoyi.web.controller.project;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.server.PathParam;

import io.swagger.models.auth.In;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;

/**
 * 卡牌处理
 *
 * @author kokor
 */
@RestController
@RequestMapping("/project/card")
public class CardController extends BaseController {
    // 底牌堆
    private static final List<String> deck = new ArrayList<>();
    // 角色牌堆
    private static final List<Integer> character = new ArrayList<>();
    // 弃牌堆
    private static final List<String> drop = new ArrayList<>();

    /*
    生成初始牌堆（不包含弱点、宿管、求饶、绿枸杞）
        000: 绿枸杞 * 10
        001: 求饶 * 16
        002: 宿管 * 4
        003: 宿管 * 4
        010: 绿人弱点 * 2
        011: 蓝人弱点 * 2
        012: 黄人弱点 * 2
        013: 紫人弱点 * 2
        020: 把风 * 8
        021: 装睡 * 8
        022: 这不合理 * 10
        023: 拿来吧你 * 2
        024: 掀被子 * 6
        025: 洗牌 * 8
        026: 反转 * 10
        027: 猜你想要 * 6
        028: 性情大变 * 4
        030：绿 * 8
        031：蓝 * 8
        032：黄 * 8
        033：紫 * 8
        034：红 * 4
        035：白 * 4
     */
    @PostMapping("/create/deck/initial")
    public void createInitialDeck() {
        // 清空之前的弃牌堆
        drop.clear();
        // 清空之前的牌堆
        deck.clear();

        // 生成牌堆
        for (int i = 0; i < 2; i++) {
            // 拿来吧你
            deck.add("023");
        }
        for (int i = 0; i < 4; i++) {
            // 性情大变
            deck.add("028");
            // 红
            deck.add("034");
            // 白
            deck.add("035");
        }
        for (int i = 0; i < 6; i++) {
            // 掀被子
            deck.add("024");
            // 猜你想要
            deck.add("027");
        }
        for (int i = 0; i < 8; i++) {
            // 把风
            deck.add("020");
            // 装睡
            deck.add("021");
            // 洗牌
            deck.add("025");
            // 绿
            deck.add("030");
            // 蓝
            deck.add("031");
            // 黄
            deck.add("032");
            // 紫
            deck.add("033");
        }
        for (int i = 0; i < 10; i++) {
            // 这不合理
            deck.add("022");
            // 反转
            deck.add("026");
        }

        // 洗牌
        Collections.shuffle(deck);
        Collections.shuffle(deck);
    }

    // 生成所有牌堆
    @PostMapping("/create/deck")
    public void createFullDeck(@RequestBody Integer number) {
        for (int i = 0; i < 2; i++) {
            // 绿人弱点
            deck.add("010");
            // 蓝人弱点
            deck.add("011");
            // 黄人弱点
            deck.add("012");
            // 紫人弱点
            deck.add("013");
        }
        for (int i = 0; i < 4; i++) {
            // 宿管阿姨
            deck.add("002");
            deck.add("003");
        }
        for (int i = 0; i < 16 - number; i++) {
            // 求饶
            deck.add("001");
        }

        // 洗牌
        Collections.shuffle(deck);
        Collections.shuffle(deck);
    }

    /*
    生成身份牌堆
        1,5,9,13 蓝人
        2,6,10,14 黄人
        3,7,11,15 紫人
        4,8,12,16 绿人
     */
    @PostMapping("/create/character")
    public void createCharacter() {
        character.clear();
        for (int i = 1; i <= 16; i++) {
            character.add(i);
        }
        Collections.shuffle(character);
        Collections.shuffle(character);
    }

    // 抽取角色
    @GetMapping("/character")
    public TableDataInfo getCharacters() {
        List<Integer> cs = new ArrayList<>();
        // 抽取两张身份牌
        cs.add(character.remove(character.size() - 1));
        cs.add(character.remove(character.size() - 1));

        return getDataTable(cs);
    }

    // 抽取起始手牌
    @GetMapping("/deal/initial")
    public TableDataInfo dealInitialCards() {
        List<String> hand = new ArrayList<>();
        // 每位玩家获得 1张“求饶”和5张随机牌
        hand.add("001");
        hand.add(deck.remove(deck.size() - 1));
        hand.add(deck.remove(deck.size() - 1));
        hand.add(deck.remove(deck.size() - 1));
        hand.add(deck.remove(deck.size() - 1));

        return getDataTable(hand);
    }

    // 抽牌
    @GetMapping("/deal")
    public String dealCard() {
        // 抽1张牌
        //return "023";
        return deck.remove(deck.size() - 1);
    }

    // 使用卡牌
    @PostMapping("/use")
    public void useCard(@RequestBody String cardNumber) {
        drop.add(cardNumber);
    }

    // 使用求饶
    @PostMapping("/use/beg")
    public void begForMercy(@RequestBody String cardNumber) {
        // 将一张“求饶”放入弃牌区
        drop.add("001");
        // 将抽到的宿管或弱点洗回去
        deck.add(cardNumber);
        Collections.shuffle(deck);
        Collections.shuffle(deck);
    }

    // 我选择死亡
    @PostMapping("/die")
    public void iWantToDie(@RequestBody List<String> handList) {
        // 将宿管或弱点牌洗回牌堆
        for (int i = 0; i < handList.size(); i++) {
            if (handList.get(i).equals("002") || handList.get(i).equals("003") || handList.get(i).equals("010") || handList.get(i).equals("011") || handList.get(i).equals("012") || handList.get(i).equals("013")) {
                deck.add(handList.remove(i));
                i--;
            }
        }
        // 洗牌
        Collections.shuffle(deck);
        Collections.shuffle(deck);
        // 将手牌全部放入弃牌堆
        drop.addAll(handList);
    }

    // 弃牌
    @PostMapping("/drop")
    public void dropCard(@RequestBody String cardNumber) {
        // 将弃掉的牌放入弃牌堆
        drop.add(cardNumber);
    }

    // 弃好多牌
    @PostMapping("/drop/cards")
    public void dropCards(@RequestBody List<String> cards) {
        for (String card : cards) {
            dropCard(card);
        }
    }

    // 放置弱点
    @PostMapping("/set")
    public void setCard(@RequestBody String cardNumber) {
        // 将弱点牌放到牌堆顶
        deck.add(cardNumber);
    }

    // 查看弃牌堆
    @GetMapping("/show/drop")
    public TableDataInfo showDropCards() {
        return getDataTable(drop);
    }

    // 查看牌堆数量
    @GetMapping("/show/deck")
    public int showDeckNumber() {
        return deck.size();
    }

    // 020 把风
    @PostMapping("/use/wind")
    public TableDataInfo seeTopCards() {
        List<String> topCards = new ArrayList<>();
        topCards.add(deck.remove(deck.size() - 1));
        topCards.add(deck.remove(deck.size() - 1));
        topCards.add(deck.remove(deck.size() - 1));

        return getDataTable(topCards);
    }

    // 020 把风结束
    @PostMapping("/use/wind/end")
    public void endSeeTopCards(@RequestBody List<String> cards) {
        deck.add(cards.remove(cards.size() - 1));
        deck.add(cards.remove(cards.size() - 1));
        deck.add(cards.remove(cards.size() - 1));
    }

    // 025 洗牌
    @PostMapping("/use/shuffle")
    public void shuffleDeck() {
        Collections.shuffle(deck);
        Collections.shuffle(deck);
    }

    // 100 “我是好人”
    @PostMapping("/use/combo/hero")
    public void getLifeFromDrop() {
        for (int i = drop.size() - 1; i >= 0; i--) {
            if (drop.get(i).equals("001")) {
                drop.remove(i);
                break;
            }
        }
    }

    // 102 “我是老六”
    @PostMapping("/use/combo/six")
    public void getCardFromDrop(@RequestBody Integer index) {
        int idx = (int) index;
        drop.remove(idx);
    }
}
