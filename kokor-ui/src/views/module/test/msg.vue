<template>
  <div>
    <button v-on:click="wsInit">连接</button>
    <button v-on:click="subSend">发送</button>
    <button v-on:click="wsDestroy">关闭</button>
    <button v-on:click="startGame">启动</button>
    <span>{{ textContent }}</span>
  </div>
</template>

<script>
export default {
  data() {
    return {
      url: 'ws://localhost:8080/websocket/message/',
      message: "",
      // ws请求链接（类似于ws后台地址）
      ws: '',
      wsIsRun: false,
      // 定义ws对象
      webSocket: null,
      // ws连接次数
      wsNumber: 0,
      // ws定时器
      wsTimer: null,
      textContent: ''
    }
  },
  created() {
    // 初始化ws参数
    this.wsIsRun = true
    this.wsNumber = 1
    // this.wsInit()
  },
  onUnload(){
    this.wsDestroy()
  },
  methods: {
    startGame(){
      this.$router.push({path:"/game/start"})
    },
    /**
     * 初始化ws
     */
    wsInit() {
      // console.log("userName: " + this.$store.state.user.ipp)
      // console.log("user: " + this.ipAddr)
      this.ws = this.url+this.$store.state.user.name
      if (!this.wsIsRun)
        return
      // 销毁ws
      this.wsDestroy()
      // 初始化ws
      this.webSocket = new WebSocket(this.ws)
      // ws连接建立时触发
      this.webSocket.addEventListener('open', this.wsOpenHanler)
      // ws服务端给客户端推送消息
      this.webSocket.addEventListener('message', this.wsMessageHanler)
      // ws通信发生错误时触发
      this.webSocket.addEventListener('error', this.wsErrorHanler)
      // ws关闭时触发
      this.webSocket.addEventListener('close', this.wsCloseHanler)

      clearInterval(this.wsTimer);
    },
    // 发送消息
    subSend() {
      //获取msg的value
      // var msg = document.getElementById("msg").value;

      // var route = window.location.href;
      // console.log(route)
      this.webSocket.send("11111")

      // ws服务端给客户端推送消息

      // document.getElementById("msg").value="";
    },
    wsOpenHanler(event) {
      console.log('ws建立连接成功')
    },
    wsMessageHanler(e) {
      // console.log('wsMessageHanler')
      // console.log(e)
      //Cookies.set('unreadNums', '6')

      this.openMsg(e)
      //const redata = JSON.parse(e.data)
      //console.log(redata)
    },
    /**
     * ws通信发生错误
     */
    wsErrorHanler(event) {
      console.log(event, '通信发生错误')
      console.log("第" + this.wsNumber + "次断线重连(5次未连接将自动断开)")
      this.wsNumber++
      if (this.wsNumber < 6) this.wsInit();
      //this.wsInit()
    },
    /**
     * ws关闭
     */
    wsCloseHanler(event) {
      console.log(event, 'ws关闭')
      console.log("websocket连接超时，已自动断开")
      //this.wsInit()
    },
    /**
     * 销毁ws
     */
    wsDestroy() {
      // console.log("hello world")
      if (this.webSocket !== null) {
        this.webSocket.removeEventListener('open', this.wsOpenHanler)
        this.webSocket.removeEventListener('message', this.wsMessageHanler)
        this.webSocket.removeEventListener('error', this.wsErrorHanler)
        this.webSocket.removeEventListener('close', this.wsCloseHanler)
        this.webSocket.close()
        this.webSocket = null
        clearInterval(this.wsTimer)
        // console.log("zoul111")
      } else {
        // console.log("zoul")
      }
    },
    /**
     * 接收消息
     */
    /**
     * 弹窗
     */
    openMsg(e) {
      const h = this.$createElement;
      if (e.data) {
        // console.log(e.data)
        this.textContent = '6'

        var data = JSON.parse(e.data)
        var route = this.$router;
        var msgType = data.msgType;
        if (msgType == "push") {
          // 如果是推送的是一条消息
          this.unreadMsgNums++;
          if (this.unreadMsgNums > 99) {
            this.unreadMsgNumsShow = "99+";
          } else {
            this.unreadMsgNumsShow = String(this.unreadMsgNums);
          }

          this.$notify.info({
            title: data.msgTitle,
            message: data.msgContent,
            onClick() {
              console.log(route)
              route.push({path: "/library/yyg/msg/list"});
            }
          });
        } else if (msgType == "unread") {
          // 如果推送的是未读消息数
          //Cookies.set('unreadNums', data.unreadNums);
          this.unreadMsgNums = Number(data.unreadNums);
          if (this.unreadMsgNums > 0) {
            this.unreadMsgNumsShow = String(this.unreadMsgNums);
            if (this.unreadMsgNums > 99) {
              this.unreadMsgNumsShow = "99+";
            }
            this.$notify.info({
              title: "消息提醒",
              //message: data.msgContent,
              message: "您当前有" + this.unreadMsgNumsShow + "条未读消息，点击此处查看",
              onClick() {
                console.log(route)
                route.push({path: "/library/yyg/msg/list"});
              }
            });
          }
        } else if (msgType == "read") {
          // 如果推送的是用户已读消息
          this.unreadMsgNums--;
          if (this.unreadMsgNums > 0 && this.unreadMsgNums <= 99) {
            this.unreadMsgNumsShow = String(this.unreadMsgNums);
          } else if (this.unreadMsgNums <= 0) {
            this.unreadMsgNumsShow = '';
          }
        }

      }
    },
  }
}
</script>

<style scoped>

</style>
