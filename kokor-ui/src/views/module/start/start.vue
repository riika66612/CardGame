<template>
  <div class="b">
    <button v-on:click="startGame" class="but">开始</button>
  </div>
</template>

<script>
export default {
  name: "start",
  data() {
    return {}
  },
  methods: {
    startGame() {
      this.$router.push({path: '/game/room'})
    }
  }
}
</script>

<style scoped lang="scss">
.b {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  background-size: cover;
  background: black url("./pic/background.jpg") no-repeat center center;
}

.but {
  //margin: 0 auto;
  //position: fixed;
  //left: 48.5%;
  //top: 10%;
  width: 180px;
  height: 60px;
  background-color: rgba(30,30,30,0.7);
  color: white;
  font-size: 20px;
}
</style>
