package com.ruoyi.framework.websocket.vo;

public class GamePlayer {
    // 玩家名
    private String userName;
    // 座位号
    private int userNumber;
    // 主身份
    private int identity;
    // 副身份
    private int subIdentity;
    // 手牌数
    private int hand;
    // 是否存活
    private boolean live = true;
    // 是否当前玩家回合
    private boolean turn = false;

    public GamePlayer() {
    }

    public GamePlayer(String userName, int userNumber, int identity,int subIdentity, int hand) {
        this.userName = userName;
        this.userNumber = userNumber;
        this.identity = identity;
        this.subIdentity = subIdentity;
        this.hand = hand;
    }

    public String getUserName() {
        return userName;
    }

    public GamePlayer setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public int getUserNumber() {
        return userNumber;
    }

    public GamePlayer setUserNumber(int userNumber) {
        this.userNumber = userNumber;
        return this;
    }

    public int getIdentity() {
        return identity;
    }

    public GamePlayer setIdentity(int identity) {
        this.identity = identity;
        return this;
    }

    public int getSubIdentity() {
        return subIdentity;
    }

    public GamePlayer setSubIdentity(int subIdentity) {
        this.subIdentity = subIdentity;
        return this;
    }

    public int getHand() {
        return hand;
    }

    public GamePlayer setHand(int hand) {
        this.hand = hand;
        return this;
    }

    public boolean getLive() {
        return live;
    }

    public GamePlayer setLive(boolean live) {
        this.live = live;
        return this;
    }

    public boolean getTurn() {
        return turn;
    }

    public GamePlayer setTurn(boolean turn) {
        this.turn = turn;
        return this;
    }

    @Override
    public String toString() {
        return "{" +
                "\"userName\":\"" + userName +
                "\",\"userNumber\":" + userNumber +
                ",\"identity\":" + identity +
                ",\"subIdentity\":" + subIdentity +
                ",\"hand\":" + hand +
                ",\"live\":" + live +
                ",\"turn\":" + turn +
                "}";
    }
}
