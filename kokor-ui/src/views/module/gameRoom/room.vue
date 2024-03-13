<template>
  <div>
    <!--按钮行-->
    <div>
      <el-button class="back-button" icon="el-icon-d-arrow-left" size="mini" v-on:click="returnBack">退出</el-button>
      <span class="tips" v-show="currentUserNumber===1 && !start">
        手牌上限为：
        <span style="font-weight: bold">
        {{ handLimit }}
        </span>
      </span>
      <span style="margin-left: 3%" v-show="currentUserNumber===1 && !start">
        <span style="font-weight: bold" v-if="useChange==='0'">不</span>
        <span style="font-weight: bold">使用 </span>
        绿枸杞
      </span>
      <el-button class="setting-button" icon="el-icon-setting" type="success" v-show="currentUserNumber===1 && !start"
                 v-on:click="showSetting = true">高级设置
      </el-button>
      <el-button class="start-button" icon="el-icon-coordinate" type="primary" v-show="currentUserNumber===1 && !start"
                 :disabled="userList.length<2" v-on:click="startGame">开始游戏
      </el-button>
    </div>

    <!--高级设置 弹窗-->
    <el-dialog title="高级设置" :visible.sync="showSetting" :show-close="false" :close-on-click-modal="false">
      <el-form ref="settings" :model="settingForm" label-width="100px" :rules="rules">
        <!--手牌上限-->
        <el-form-item label="手牌上限：" prop="limit">
          <el-input v-model.number="settingForm.limit" placeholder="请输入1到12之间的数字"/>
        </el-form-item>

        <el-form-item label="使用绿枸杞" prop="change">
          <el-radio v-model="useChange" label="0">不启用</el-radio>
          <el-radio v-model="useChange" label="1">启用</el-radio>
        </el-form-item>
      </el-form>

      <!--确认按钮-->
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" v-on:click="closeSetting">确定</el-button>
      </div>
    </el-dialog>

    <!--测试用作弊-->
    <el-dialog :visible.sync="showAdd">
      <el-input v-model="addCard"/>
      <el-button @click="addToHand">确定</el-button>
    </el-dialog>

    <!--主要区域-->
    <div class="main-room" v-on:click="closeOperate">
      <!--左侧用户-->
      <div class="user-side">
        <el-tooltip effect="dark" content="点击查看身份" placement="right" v-for="(item,index) in userList" :key="index">
          <el-card :class="item.identity===0?'user':('user user-background-'+item.identity%4)"
                   :style="item.turn?'border:5px solid;borderTopColor:#5A834C;borderRightColor:#3A9FBB;borderBottomColor:#CFAA1A;borderLeftColor:#5E3E58':''"
                   @click.prevent.native.stop="showIdentity(item.identity)">
            {{ item.userName }} ({{ item.hand }})
            <span style=" color: red;font-weight: bold" v-if="!item.live">出 局</span>
          </el-card>
        </el-tooltip>
      </div>

      <!--右侧游戏区-->
      <el-card class="game-main">
        <!--功能按钮区-->
        <div class="function-zone" v-show="start">
          <el-button type="primary" plain v-on:click="drawer = true">组合技</el-button>
          <el-button :disabled="!myTurn" v-on:click="drawCard">抽牌</el-button>
          <el-button :disabled="!myTurn" v-on:click="showAdd=true">添加</el-button>
          <el-button :disabled="!myTurn || wait || needDrop" v-on:click="wantToEndMyTurn">回合结束</el-button>
        </div>

        <!--手牌大图显示-->
        <el-card class="show-card" v-if="selectedCardNumber!==''">
          <img class="card-pic" :src="require('./pic/card/'+selectedCardNumber+'.jpg')"
               :alt="selectedCardNumber+'加载失败'"/>
        </el-card>

        <!--牌堆区-->
        <div class="cards" v-show="start">
          <!--抽牌堆-->
          <el-tooltip effect="dark" content="点击显示牌堆剩余张数" placement="bottom">
            <el-card class="deck-zone" @click.stop.prevent.native="showDeck">
              <img class="card-pic" :src="require('./pic/bg/bg_2.jpg')" alt="背景图片加载失败"/>
            </el-card>
          </el-tooltip>

          <!--弃牌堆-->
          <el-tooltip effect="dark" content="点击查看弃牌堆卡牌" placement="bottom">
            <el-card class="drop-zone" @click.stop.prevent.native="showDropCards">
              <img class="card-pic" :src="require('./pic/bg/bg_2.jpg')" alt="图片加载失败"/>
            </el-card>
          </el-tooltip>

          <!--隐藏身份-->
          <el-tooltip effect="dark" content="点击查看自己隐藏身份" placement="bottom">
            <el-card class="drop-zone" @click.stop.prevent.native="showIdentity(subIdentity)">
              <img class="card-pic" :src="require('./pic/bg/bg_1.jpg')" alt="图片加载失败"/>
            </el-card>
          </el-tooltip>
        </div>

        <!--手牌区-->
        <div class="hand-zone" v-if="handList.length>0">
          <el-card class="hand-card" :style="handList.length>=9?'marginRight:-3%':'marginRight:1%'"
                   v-for="(item,index) in handList" :key="index"
                   @click.stop.prevent.native="operateCard($event,item,index)">
            <img class="card-pic" :src="require('./pic/card/'+item+'.jpg')" :alt="item+'加载失败'"/>
          </el-card>
        </div>

        <!--功能弹窗-->
        <el-card class="operate-dialog" v-show="showOperate"
                 :style="{left:mX+'px', top:mY+'px', position:'fixed', zIndex:999}">
          <el-button v-if="!needDrop&&!setWeakness" v-on:click="selectTarget">出牌</el-button>
          <el-button v-if="needDrop&&!setWeakness" v-on:click="dropCard">弃牌</el-button>
          <el-button v-if="setWeakness" v-on:click="setCard">放置</el-button>
        </el-card>
      </el-card>
    </div>

    <!--身份选择-->
    <el-dialog title="选择你的身份" :visible.sync="identitySelect" :show-close="false" :close-on-click-modal="false">
      <!--身份卡-->
      <div style="display: flex">
        <el-card :class="mainIdentity===item?'identity-card selected':'identity-card'"
                 v-for="(item,index) in identity" :key="index"
                 @click.prevent.native.stop="selectIdentity(item)">
          <img class="identity-pic" :src="require('./pic/character/'+item+'.jpg')" :alt="item+'加载失败'"/>
        </el-card>
      </div>

      <!--确认按钮-->
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :disabled="mainIdentity===0" v-on:click="confirmIdentity">确定</el-button>
      </div>
    </el-dialog>

    <!--身份查看-->
    <el-dialog title="查看身份" :visible.sync="showId" width="30%" @close="id=0">
      <!--身份卡-->
      <el-card class="show-identity-card" v-if="id!==0">
        <img class="identity-pic" :src="require('./pic/character/'+id+'.jpg')" :alt="id+'加载失败'"/>
      </el-card>
    </el-dialog>

    <!--回合结束选择-->
    <el-dialog class="select-end-method" title="选择结束方式" :visible.sync="showEndSelect" width="20%">
      <el-button type="primary" style="margin-left: 20%" v-on:click="endWithDraw">抽一张牌</el-button>
      <el-button type="danger" v-on:click="endWithWeak"
                 :disabled="!(handList.includes('010')||handList.includes('011')||handList.includes('012')||handList.includes('013')) || set">
        放张弱点
      </el-button>
    </el-dialog>

    <!--查看弃牌堆-->
    <el-dialog title="查看弃牌堆" :visible.sync="showDrop" width="80%" @close="selectedDropNumber=''">
      <div class="show-drop">
        <!--确认区-->
        <div class="drop-confirm-zone">
          <el-card class="drop-confirm">
            <img v-if="selectedDropNumber===''" class="card-pic" :src="require('./pic/bg/bg_2.jpg')" alt="背景图片加载失败"/>
            <img v-else class="card-pic" :src="require('./pic/card/'+selectedDropNumber+'.jpg')" alt="背景图片加载失败"/>
          </el-card>
        </div>

        <!--卡片区-->
        <div class="drop-cards">
          <el-card class="one-drop-card" v-for="(item,index) in dropList" :key="index"
                   @click.prevent.native.stop="selectDrop(item)">
            <img class="drop-pic" :src="require('./pic/card/'+item+'.jpg')" :alt="item+'加载失败'"/>
          </el-card>
        </div>
      </div>
    </el-dialog>

    <!--选择施法目标-->
    <el-dialog title="选择目标" :visible.sync="showTarget" :show-close="false" :close-on-click-modal="false">
      <!--目标-->
      <div style="display: flex">
        <el-card :class="selectedUserIndex===index?'target-card selected':'target-card'"
                 v-for="(item,index) in userList" :key="index"
                 v-if="item.live && !item.turn && ((selectedCardNumber==='028'&&item.subIdentity!==0) || selectedCardNumber!=='028') && ((selectedCombo!=='103'&&selectedCombo!=='104')||((selectedCombo==='103'||selectedCombo==='104')&&item.hand>0))"
                 @click.prevent.native.stop="selectUserTarget(item.userName,index)">
          <div slot="header" class="clearfix">
            <span>{{ item.userName }}</span>
            <span style="float: right; padding: 3px 0">共{{ item.hand }}张手牌</span>
          </div>
          <img class="identity-pic" :src="require('./pic/character/'+item.identity+'.jpg')"
               :alt="item.identity+'加载失败'" v-if="item.identity !== 0"/>
        </el-card>
      </div>

      <!--确认按钮-->
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :disabled="selectedUserIndex===-1" v-on:click="confirmTarget">确定</el-button>
        <el-button type="" v-on:click="resetParam">取消</el-button>
      </div>
    </el-dialog>

    <!--把风看牌-->
    <el-dialog style="margin-top: -2%" title="把风中" :visible.sync="showTop" :show-close="false"
               :close-on-click-modal="false">
      <!--提示文字-->
      <span>< 顶 - 中 - 底 ></span>

      <!--原卡牌顺序-->
      <div style="display: flex">
        <el-card class="show-top-card" v-for="(item,index) in topCards" :key="index"
                 @click.prevent.native.stop="changeCardOrder(item)">
          <img class="identity-pic" :src="require('./pic/card/'+item+'.jpg')" :alt="item+'加载失败'"/>
        </el-card>
      </div>

      <!--提示文字-->
      <span style="margin-top: 12%">V改变后的顺序V</span>

      <!--修改后卡牌顺序-->
      <div style="display: flex">
        <el-card class="show-top-card" v-for="(item,index) in changedTopCards" :key="index"
                 @click.prevent.native.stop="removeFromChange(index)">
          <img class="identity-pic" :src="require('./pic/card/'+item+'.jpg')" :alt="item+'加载失败'"/>
        </el-card>
      </div>

      <!--确认按钮-->
      <div slot="footer" class="dialog-footer" style="margin-top: -5%">
        <el-button type="primary" :disabled="changedTopCards.length!==3" v-on:click="confirmCardOrder">确定</el-button>
      </div>
    </el-dialog>

    <!--“这不合理”使用提示-->
    <el-dialog title="用牌提醒" :visible.sync="cardUsed" :show-close="false" :close-on-click-modal="false">
      <div class="show-drop">
        <!--确认区-->
        <div :class="usedCardNumber.startsWith('1')?'drop-confirm-zone-combo':'drop-confirm-zone'">
          <el-card class="drop-confirm" shadow="never">
            <!--无-->
            <img v-if="usedCardNumber===''" class="card-pic" :src="require('./pic/bg/bg_2.jpg')" alt="背景图片加载失败"/>
            <!--组合技1：我是好人-->
            <div v-else-if="usedCardNumber==='100'">
              <img class="combo-example" :src="require('./pic/card/035.jpg')" alt="示例加载失败"/>
              <span class="combo-example-text">+</span>
              <img class="combo-example" :src="require('./pic/card/031.jpg')" alt="示例加载失败"/>
              <img class="combo-example" style="margin-left: -20%" :src="require('./pic/card/031.jpg')" alt="示例加载失败"/>
            </div>
            <!--组合技2：夺笋-->
            <div v-else-if="usedCardNumber==='101'">
              <img class="combo-example" :src="require('./pic/card/034.jpg')" alt="示例加载失败"/>
              <span class="combo-example-text">+</span>
              <img class="combo-example" :src="require('./pic/card/030.jpg')" alt="示例加载失败"/>
              <img class="combo-example" style="margin-left: -20%" :src="require('./pic/card/030.jpg')" alt="示例加载失败"/>
            </div>
            <!--组合技3：我是老六-->
            <div v-else-if="usedCardNumber==='102'">
              <img class="combo-example" :src="require('./pic/card/035.jpg')" alt="加载失败"/>
              <span class="combo-example-text">+</span>
              <img class="combo-example" :src="require('./pic/card/034.jpg')" alt="加载失败"/>
            </div>
            <!--组合技4：对子-->
            <div v-else-if="usedCardNumber==='103'">
              <img class="combo-example" :src="require('./pic/card/032.jpg')" alt="加载失败"/>
              <span class="combo-example-text">+</span>
              <img class="combo-example" :src="require('./pic/card/032.jpg')" alt="加载失败"/>
            </div>
            <!--组合技5：三条-->
            <div v-else-if="usedCardNumber==='104'">
              <img class="combo-example" style="margin-right: 2%" :src="require('./pic/card/033.jpg')" alt="加载失败"/>
              <img class="combo-example" style="margin-right: 2%" :src="require('./pic/card/033.jpg')" alt="加载失败"/>
              <img class="combo-example" :src="require('./pic/card/033.jpg')" alt="加载失败"/>
            </div>
            <!--单张卡牌-->
            <img v-else class="card-pic" :src="require('./pic/card/'+usedCardNumber+'.jpg')" alt="图片加载失败"/>
          </el-card>
        </div>

        <!--消息区-->
        <div class="drop-cards">
          <span>{{ useMessage }}<br>选"是"使用“这不合理”，选"否"不使用</span>
        </div>
      </div>

      <!--按钮-->
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :disabled="!handList.includes('022')" v-on:click="isNotOk">是</el-button>
        <el-button type="danger" v-on:click="isOk">否</el-button>
      </div>
    </el-dialog>

    <!--“掀被子”提示-->
    <el-dialog title="掀被子" :visible.sync="drawTip" :show-close="false" :close-on-click-modal="false">
      <div class="show-drop">
        <!--确认区-->
        <div class="drop-confirm-zone">
          <el-card class="drop-confirm">
            <img class="card-pic" :src="require('./pic/card/024.jpg')" alt="背景图片加载失败"/>
          </el-card>
        </div>

        <!--消息区-->
        <div class="drop-cards">
          <span>{{ drawMessage }}<br>选"是"继续“掀被子”，选"否"抽牌</span>
        </div>
      </div>

      <!--按钮-->
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :disabled="!handList.includes('024')" v-on:click="iWantToLift">是</el-button>
        <el-button type="danger" v-on:click="iWantToDraw(0)">否</el-button>
      </div>
    </el-dialog>

    <!--“猜你想要”选择手牌-->
    <el-dialog style="margin-top: -1%" title="选择手牌" :visible.sync="giveCard" width="70%" :show-close="false"
               :close-on-click-modal="false">
      <!--手牌-->
      <div style="display: inline-block;width: 100%">
        <el-card :class="selectedCardIndex===index?'target-card-hand selected':'target-card-hand'"
                 v-for="(item,index) in handList" :key="index"
                 @click.prevent.native.stop="selectAHandCard(item,index)">
          <img class="identity-pic" :src="require('./pic/card/'+item+'.jpg')" :alt="item+'加载失败'"/>
        </el-card>
      </div>

      <!--确认按钮-->
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :disabled="selectedCardIndex===-1" v-on:click="confirmCard">确定</el-button>
      </div>
    </el-dialog>

    <!--组合技选择-->
    <el-drawer title="选择组合技" :visible.sync="drawer" direction="rtl" size="21%">
      <!--提示文字-->
      <div>
        <span style="color: red;font-weight: bold;margin-left: 5%">注</span>：图中是使用示例，仅供参考。
      </div>

      <!--我是好人-->
      <el-card class="combo-card" style="margin-top: 3%" v-if="useChange==='0'">
        <!--组合技名称-->
        <div slot="header" class="clearfix">
          <span style="font-weight: bold">我是好人</span>
          <el-button style="float: right;margin-top: -1%" type="primary" size="mini"
                     :disabled="!myTurn || wait || needDrop || comboUseable('100',handList)"
                     v-on:click="selectCombo('100')">使用
          </el-button>
        </div>

        <!--组合示例-->
        <el-tooltip effect="dark" :content="comboOne" placement="left">
          <div>
            <img class="combo-example" :src="require('./pic/card/035.jpg')" alt="加载失败"/>
            <span class="combo-example-text">+</span>
            <img class="combo-example" :src="require('./pic/card/031.jpg')" alt="加载失败"/>
            <img class="combo-example" style="margin-left: -20%" :src="require('./pic/card/031.jpg')" alt="加载失败"/>
          </div>
        </el-tooltip>
      </el-card>

      <!--我是好人(变体)-->
      <el-card class="combo-card" style="margin-top: 3%" v-else-if="useChange==='1'">
        <!--组合技名称-->
        <div slot="header" class="clearfix">
          <span style="font-weight: bold">我是好人（变体）</span>
          <el-button style="float: right;margin-top: -1%" type="primary" size="mini"
                     :disabled="!myTurn || wait || needDrop || comboUseable('100',handList)"
                     v-on:click="selectCombo('100')">使用
          </el-button>
        </div>

        <!--组合示例-->
        <el-tooltip effect="dark" :content="comboOneChange" placement="left">
          <div>
            <img class="combo-example" :src="require('./pic/card/035.jpg')" alt="加载失败"/>
            <span class="combo-example-text">+</span>
            <img class="combo-example" :src="require('./pic/card/031.jpg')" alt="加载失败"/>
            <img class="combo-example" style="margin-left: -20%" :src="require('./pic/card/031.jpg')" alt="加载失败"/>
          </div>
        </el-tooltip>
      </el-card>

      <!--夺笋-->
      <el-card class="combo-card">
        <!--组合技名称-->
        <div slot="header" class="clearfix">
          <span style="font-weight: bold">夺笋</span>
          <el-button style="float: right;margin-top: -1%" type="primary" size="mini"
                     :disabled="!myTurn || wait || needDrop || comboUseable('101',handList)"
                     v-on:click="selectCombo('101')">使用
          </el-button>
        </div>

        <!--组合示例-->
        <el-tooltip effect="dark" :content="comboTwo" placement="left">
          <div>
            <img class="combo-example" :src="require('./pic/card/034.jpg')" alt="加载失败"/>
            <span class="combo-example-text">+</span>
            <img class="combo-example" :src="require('./pic/card/030.jpg')" alt="加载失败"/>
            <img class="combo-example" style="margin-left: -20%" :src="require('./pic/card/030.jpg')" alt="加载失败"/>
          </div>
        </el-tooltip>
      </el-card>

      <!--我是老六-->
      <el-card class="combo-card">
        <!--组合技名称-->
        <div slot="header" class="clearfix">
          <span style="font-weight: bold">我是老六</span>
          <el-button style="float: right;margin-top: -1%" type="primary" size="mini"
                     :disabled="!myTurn || wait || needDrop || comboUseable('102',handList)"
                     v-on:click="selectCombo('102')">使用
          </el-button>
        </div>

        <!--组合示例-->
        <el-tooltip effect="dark" :content="comboThree" placement="left">
          <div style="margin-left: 8%">
            <img class="combo-example" :src="require('./pic/card/035.jpg')" alt="加载失败"/>
            <span class="combo-example-text">+</span>
            <img class="combo-example" :src="require('./pic/card/034.jpg')" alt="加载失败"/>
          </div>
        </el-tooltip>
      </el-card>

      <!--对子-->
      <el-card class="combo-card">
        <!--组合技名称-->
        <div slot="header" class="clearfix">
          <span style="font-weight: bold">对子</span>
          <el-button style="float: right;margin-top: -1%" type="primary" size="mini"
                     :disabled="!myTurn || wait || needDrop || comboUseable('103',handList)"
                     v-on:click="selectCombo('103')">使用
          </el-button>
        </div>

        <!--组合示例-->
        <el-tooltip effect="dark" :content="comboFour" placement="left">
          <div style="margin-left: 8%">
            <img class="combo-example" :src="require('./pic/card/032.jpg')" alt="加载失败"/>
            <span class="combo-example-text">+</span>
            <img class="combo-example" :src="require('./pic/card/032.jpg')" alt="加载失败"/>
          </div>
        </el-tooltip>
      </el-card>

      <!--三条-->
      <el-card class="combo-card">
        <!--组合技名称-->
        <div slot="header" class="clearfix">
          <span style="font-weight: bold">三条</span>
          <el-button style="float: right;margin-top: -1%" type="primary" size="mini"
                     :disabled="!myTurn || wait || needDrop || comboUseable('104',handList)"
                     v-on:click="selectCombo('104')">使用
          </el-button>
        </div>

        <!--组合示例-->
        <el-tooltip effect="dark" :content="comboFive" placement="left">
          <div>
            <img class="combo-example" style="margin-right: 2%" :src="require('./pic/card/033.jpg')" alt="加载失败"/>
            <img class="combo-example" style="margin-right: 2%" :src="require('./pic/card/033.jpg')" alt="加载失败"/>
            <img class="combo-example" :src="require('./pic/card/033.jpg')" alt="加载失败"/>
          </div>
        </el-tooltip>
      </el-card>
    </el-drawer>

    <!--组合技选择手牌-->
    <el-dialog style="margin-top: -1%" title="选择手牌" :visible.sync="showCombo" width="70%" :show-close="false"
               :close-on-click-modal="false">
      <!--手牌-->
      <div style="display: inline-block;width: 100%">
        <el-card :class="selectedCardsIndex.includes(index)?'target-card-hand selected':'target-card-hand'"
                 v-for="(item,index) in handList" :key="index"
                 @click.prevent.native.stop="selectSomeCards(item,index)">
          <img class="identity-pic" :src="require('./pic/card/'+item+'.jpg')" :alt="item+'加载失败'"/>
        </el-card>
      </div>

      <!--确认按钮-->
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :disabled="selectedCardsIndex.length<2" v-on:click="confirmCards">确定</el-button>
        <el-button type="" v-on:click="resetCombo">取消</el-button>
      </div>
    </el-dialog>

    <!--“我是好人”拿牌顺序选择-->
    <el-dialog class="select-end-method" title="请选择获得牌的方向" :visible.sync="showOrder" width="20%" :show-close="false"
               :close-on-click-modal="false">
      <div style="margin-top: -5%"><span style="font-weight: bold;color: red">注：</span>无论什么顺序都是自己先拿牌</div>
      <!--操作按钮-->
      <div style="margin: 3% 0 0 10%">
        <el-button type="primary" plain v-on:click="iAmHero('0')">当前回合顺序</el-button>
        <el-button type="primary" plain v-on:click="iAmHero('1')">当前回合逆序</el-button>
      </div>
    </el-dialog>

    <!--“我是老六”选牌-->
    <el-dialog title="选择你想要的牌" :visible.sync="seeDrop" width="80%" :show-close="false" :close-on-click-modal="false">
      <div class="show-drop">
        <!--确认区-->
        <div class="drop-confirm-zone">
          <el-card class="drop-confirm">
            <img v-if="cardIndexIWant===''" class="card-pic" :src="require('./pic/bg/bg_2.jpg')" alt="背景图片加载失败"/>
            <img v-else class="card-pic" :src="require('./pic/card/'+dropList[cardIndexIWant]+'.jpg')" alt="背景图片加载失败"/>
          </el-card>
        </div>

        <!--卡片区-->
        <div class="drop-cards">
          <el-card :class="index===cardIndexIWant?'one-drop-card selected':'one-drop-card'"
                   v-for="(item,index) in dropList" :key="index" @click.prevent.native.stop="selectIndexWant(index)">
            <img class="drop-pic" :src="require('./pic/card/'+item+'.jpg')" :alt="item+'加载失败'"/>
          </el-card>
        </div>
      </div>

      <!--确认按钮-->
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :disabled="cardIndexIWant===''" v-on:click="confirmIWantFromDrop">确定</el-button>
      </div>
    </el-dialog>

    <!--“三条”指定卡牌-->
    <el-dialog style="margin-top: -2%" title="指定卡牌" :visible.sync="showCards" width="60%" :show-close="false"
               :close-on-click-modal="false">
      <div class="show-drop">
        <!--确认区-->
        <div class="drop-confirm-zone">
          <el-card class="drop-confirm">
            <img v-if="cardIWant===''" class="card-pic" :src="require('./pic/bg/bg_2.jpg')" alt="背景图片加载失败"/>
            <img v-else class="card-pic" :src="require('./pic/card/'+cardIWant+'.jpg')" alt="背景图片加载失败"/>
          </el-card>
        </div>

        <!--卡片区-->
        <div class="drop-cards">
          <el-card :class="item===cardIWant?'one-drop-card selected':'one-drop-card'" v-for="(item,index) in cardList"
                   :key="index" @click.prevent.native.stop="selectWantCard(item)">
            <img class="drop-pic" :src="require('./pic/card/'+item+'.jpg')" :alt="item+'加载失败'"/>
          </el-card>
        </div>
      </div>

      <!--确认按钮-->
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :disabled="cardIWant===''" v-on:click="confirmIWant">确定</el-button>
      </div>
    </el-dialog>

    <!--抓到雷-->
    <el-dialog title="出局警告" :visible.sync="getBoom" :show-close="false" :close-on-click-modal="false">
      <!--文本内容-->
      <span>
        {{ killBy }}
      </span>

      <!--按钮-->
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :disabled="!(handList.includes('001')||handList.includes('000'))"
                   v-on:click="iWantToLive">是
        </el-button>
        <el-button type="danger" v-on:click="iWantToDie">否</el-button>
      </div>
    </el-dialog>

    <!--出局-->
    <el-dialog title="出局" :visible.sync="beKilled" :show-close="false" :close-on-click-modal="false">
      <!--文本内容-->
      <span>
        宝，你寄了！
      </span>

      <!--按钮-->
      <div slot="footer" class="dialog-footer">
        <el-button type="" v-on:click="exitRoom">退出</el-button>
        <el-button type="primary" v-on:click="seeGame">观战</el-button>
      </div>
    </el-dialog>

    <!--游戏结束-->
    <el-dialog title="游戏结束" :visible.sync="gameEnd" :show-close="false" :close-on-click-modal="false">
      <!--文本内容-->
      <span>
        本局游戏的赢家是：{{ victory }}
      </span>

      <!--按钮-->
      <div slot="footer" class="dialog-footer">
        <el-button type="" v-on:click="exitRoom">退出游戏</el-button>
        <el-button type="primary" v-on:click="playAgain">再来一局</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  creatDeck,
  createCharacters,
  creatInitialDeck,
  drawACard,
  dropACard,
  dropCards,
  getCharacters,
  getInitialCard,
  setACard,
  showDeckNumber,
  showDroppedCards,
  useBeg,
  useGreen,
  wantToDie,
  seeTopCards,
  endSeeTopCards,
  shuffleCards,
  getLife,
  getCardFromDrop,
} from "@/api/project/card"
import {getIpAddr} from "@/api/project/ipAddr"

export default {
  name: "room",
  data() {
    var checkLimit = (rule, value, callback) => {
      if (!value) {
        return callback(new Error('上限不能为空'));
      }
      if (!Number.isInteger(value)) {
        callback(new Error('请输入数字值'));
      }
      callback()
    }
    return {
      // 服务端ip
      serverIp: '127.0.0.1',
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
      // 游戏开始标志
      start: false,
      // 当前参加游戏的用户列表
      userList: [],
      // 当前用户的编号（座位号）
      currentUserNumber: '',
      // 牌堆数量
      deck: 0,
      // 手牌上限
      handLimit: 8,
      // 使用变体
      useChange: '0',
      // 测试作弊
      showAdd: false,
      addCard: '',
      // 高级设置 弹窗控制
      showSetting: false,
      // 设置表单
      settingForm: {
        limit: 8,
      },
      // 表单校验
      rules: {
        limit: [
          {required: true, validator: checkLimit, trigger: "blur"}
        ]
      },
      // 手牌
      handList: [],
      // 抽取到的两张身份
      identity: [],
      /*
      主要身份
        1,5,9,13 蓝人
        2,6,10,14 黄人
        3,7,11,15 紫人
        4,8,12,16 绿人
       */
      mainIdentity: 0,
      // 隐藏身份
      subIdentity: 0,
      // 身份选择 弹窗控制
      identitySelect: false,
      // 查看身份 弹窗控制
      showId: false,
      // 查看到的身份
      id: 0,
      // 是否是自己回合
      myTurn: false,
      // 是否弃牌
      needDrop: false,
      // 放一张弱点
      setWeakness: false,
      // 上一位玩家是否放置弱点
      set: false,
      // 出牌 弹窗控制
      showOperate: false,
      // 点击时x坐标
      mX: 0,
      // 点击时x坐标
      mY: 0,
      // 当前选中卡牌编号
      selectedCardNumber: '',
      // 在手牌中的索引
      selectedCardIndex: -1,
      // 施法目标名称
      selectedUserName: '好人',
      // 施法目标索引
      selectedUserIndex: -1,
      // 展示可选目标
      showTarget: false,
      // 等待状态（限制一些操作）
      wait: false,
      // 游戏结束 弹窗控制
      gameEnd: false,
      // 胜利玩家
      victory: '',
      // 回合结束方式选择 弹窗控制
      showEndSelect: false,
      // 抓到宿管还是弱点
      killBy: '',
      // 是否抓到宿管或弱点
      getBoom: false,
      // 抓到宿管或弱点的时机 '0':回合中；'1':回合结束时；'2':回合外
      boomTime: '0',
      // 正常游戏出局
      beKilled: false,
      // 弃牌堆
      dropList: [],
      // 查看弃牌堆 窗口控制
      showDrop: false,
      // 选中的弃牌区中的卡的编号
      selectedDropNumber: '',
      // 有卡被使用了
      cardUsed: false,
      // 被使用的卡
      usedCardNumber: '',
      // 卡牌使用提醒
      useMessage: '',
      // 看顶部
      showTop: false,
      // 顶部牌
      topCards: [],
      // 改变后的顶牌顺序
      changedTopCards: [],
      // 选择手牌（猜你想要） 弹窗控制
      giveCard: false,
      // 掀被子提示 弹窗控制
      drawTip: false,
      // 掀被子提示文字
      drawMessage: '',
      // 掀被子抽卡数量
      drawCardNumber: 0,
      // 临时暂存计数
      tempCount: 0,
      // 组合技 抽屉控制
      drawer: false,
      // “我是好人” 说明文字
      comboOne: <span>打出<strong>1张白颜色卡+2张同色的<br/>
        颜色卡</strong>，该颜色的所有玩家<br/>
        依次从弃牌堆获得<strong>1张</strong>求饶卡，<br/>
        直到拿完为止。</span>,
      // “我是好人(变体)” 说明文字
      comboOneChange: <span>打出<strong>1张白颜色卡+2张同色的<br/>
        颜色卡</strong>，该颜色的所有玩家<br/>
        皆可获得<strong>1张</strong>“绿枸杞”（等效于求饶）。</span>,
      // “夺笋” 说明文字
      comboTwo: <span>打出<strong>1张<span style="color:red">红</span>颜色卡+2张同色的<br/>
        颜色卡</strong>，该颜色的所有玩家<br/>
        将被你举报，需要使用<strong>求饶卡</strong><br/>
        才能存活。</span>,
      // “我是老六” 说明文字
      comboThree: <span>打出<strong>1张白颜色卡+1张<span style="color:red">红</span>颜色卡</strong>，<br/>
        从<strong>弃牌堆</strong>里获得一张你想要的卡牌。</span>,
      // “对子” 说明文字
      comboFour: <span>打出<strong>2张相同的牌</strong>，<br/>
        从任意一名玩家的手牌中<br/>
        随机抽取<strong>1张</strong>手牌。</span>,
      // “三条” 说明文字
      comboFive: <span>打出<strong>3张相同的牌</strong>，<br/>
        向任意玩家索要一张<strong>指定卡牌</strong>。<br/>
        若该玩家有则给你<strong>1张</strong>,<br/>
        若没有，则你浪费3张手牌。</span>,
      // 想要使用的组合技
      selectedCombo: '',
      // 组合技选择手牌 弹窗控制
      showCombo: false,
      // 被选择的手牌们
      selectedCardsNumber: [],
      selectedCardsIndex: [],
      // “我是好人”顺序选择 弹窗控制
      showOrder: false,
      // 选择的想要的卡
      cardIWant: '',
      // “我是老六”选择卡牌 弹窗控制
      seeDrop: false,
      // 想要卡牌的索引
      cardIndexIWant: '',
      // “三条”指定卡牌 弹窗控制
      showCards: false,
      // 所有卡牌的编号
      cardList: ['000', '001', '002', '003', '010', '011', '012', '013', '020', '021', '022', '023', '024', '025', '026', '027', '028', '030', '031', '032', '033', '034', '035'],
    }
  },
  created() {
    // 初始化ws参数
    this.wsIsRun = true
    this.wsNumber = 1
    getIpAddr().then(res => {
      this.serverIp = res
      this.wsInit()
    })
  },
  onUnload() {

  },
  computed: {},
  methods: {
    // 测试用作弊
    addToHand() {
      this.showAdd = false
      this.handList.push(this.addCard)
    },
    // 返回
    returnBack() {
      this.closeOperate()
      this.$modal.confirm('你确定要退出吗？').then(function () {

      }).then(() => {
        this.wsDestroy()
        this.$router.push({path: '/game/start'})
      }).catch(() => {

      })
    },
    // 关闭设置弹窗
    closeSetting() {
      this.$refs["settings"].validate(valid => {
        if (valid) {
          if (this.settingForm.limit < 1 || this.settingForm.limit > 12) {
            this.$modal.msgError("请将手牌上限设置到1到12之间的数字")
          } else {
            this.handLimit = this.settingForm.limit
            this.showSetting = false
          }
        }
      })
    },
    // 游戏开始
    startGame() {
      // console.log("游戏开始")
      this.start = true
      createCharacters().then(() => {
        this.webSocket.send('{"msgType":"1","msgContent":"Start Game","msgTo":"System","handLimit":' + this.handLimit + ',"useChange":' + this.useChange + '}')
      })
      creatInitialDeck()
    },
    // 选择身份
    selectIdentity(item) {
      this.mainIdentity = item
    },
    // 确认身份
    confirmIdentity() {
      if (this.identity[0] === this.mainIdentity) {
        this.subIdentity = this.identity[1]
      } else {
        this.subIdentity = this.identity[0]
      }
      this.webSocket.send('{"msgType":"2","msgContent":' + this.mainIdentity + ',"msgExtra":' + this.subIdentity + ',"msgTo":"System"}')
      this.identitySelect = false
    },
    // 选择手牌
    operateCard(event, number, index) {
      if ((this.wait && this.needDrop) || !this.wait) {
        // 所选择的手牌
        this.selectedCardNumber = number
        this.selectedCardIndex = index
      }

      // this.showOperate = false
      if (this.myTurn) {
        if (this.needDrop && (number === '010' || number === '011' || number === '012' || number === '013') && !this.setWeakness) {
          // 弃牌时 -> 弱点牌不能弃
          this.$modal.msgError("弱点牌不可丢弃")
        } else if (!this.needDrop && this.setWeakness && (number !== '010' && number !== '011' && number !== '012' && number !== '013')) {
          // 盖放弱点时 -> 其它牌不能盖放
          this.showOperate = false
        } else if (((number === '010' || number === '011' || number === '012' || number === '013' || number === '000' || number === '001'
          || number === '002' || number === '003' || number === '022' || number === '030' || number === '031' || number === '032'
          || number === '033' || number === '034' || number === '035') || this.wait) && !this.setWeakness && !this.needDrop) {
          // 使用牌时 -> 弱点、颜色、求饶、绿枸杞、宿管不能使用
          this.showOperate = false
        } else {
          // 获取当前鼠标坐标
          this.mX = event.clientX
          this.mY = event.clientY
          // 显示弹出窗
          this.showOperate = true
        }
      }
    },
    // 关闭操作弹窗
    closeOperate() {
      this.showOperate = false
    },
    // 查看牌堆数量
    showDeck() {
      this.closeOperate()
      showDeckNumber().then(res => {
        this.$modal.msg("牌堆剩余" + res + "张牌")
      })
    },
    // 查看弃牌堆
    showDropCards() {
      this.closeOperate()
      showDroppedCards().then(res => {
        this.dropList = res.rows
        this.showDrop = true
      })
    },
    // 查看身份
    showIdentity(id) {
      if (this.start) {
        if (id === 0) {
          this.$modal.msg("你已经没有隐藏身份啦")
        } else {
          this.id = id
          this.showId = true
        }
      }
    },
    // 查看弃牌区大图
    selectDrop(number) {
      this.selectedDropNumber = number
    },
    // 选取施法目标
    selectTarget() {
      // console.log(this.userList)
      if (this.selectedCardNumber === "023" || this.selectedCardNumber === "027" || this.selectedCardNumber === "028") {
        this.selectedUserName = "好人"
        this.showTarget = true
      } else {
        this.selectedUserName = "好人"
        this.useCard()
      }
    },
    // 选择一个用户为目标
    selectUserTarget(name, index) {
      this.selectedUserName = name
      this.selectedUserIndex = index
    },
    // 确认施法目标
    confirmTarget() {
      // this.selectedUserName = this.userList[this.selectedUserIndex].userName
      this.showTarget = false
      this.showCombo = false
      this.drawer = false
      if (this.selectedCombo === '') {
        this.useCard()
      } else {
        this.useCombo()
      }
    },
    // 使用卡牌
    useCard() {
      // 进入等待状态
      this.wait = true
      // 将牌放入弃牌堆
      dropACard(this.selectedCardNumber).then(() => {
        // 从手牌中将其移除
        this.handList.splice(this.selectedCardIndex, 1)
        // 更新全场手牌数显示
        this.webSocket.send('{"msgType":"7","msgContent":"Use a Card","msgTo":"System","msgFrom":' + (this.currentUserNumber - 1) + ',"msgExtra":1}')
        // 询问全场是否使用“这不合理”
        this.webSocket.send('{"msgType":"9","msgContent":' + this.selectedCardNumber + ',"msgExtra":' + this.selectedUserName + ',"msgTo":"System","msgFrom":' + (this.currentUserNumber - 1) + '}')
      })
    },
    // 弃牌
    dropCard() {
      // 将牌放入弃牌堆中
      dropACard(this.selectedCardNumber).then(() => {
        // 将牌从手中移除
        this.handList.splice(this.selectedCardIndex, 1)
        // 更新全场手牌
        this.webSocket.send('{"msgType":"7","msgContent":"Drop a Card","msgTo":"System","msgFrom":' + (this.currentUserNumber - 1) + '}')
        // 重置参数
        this.selectedCardNumber = ''
        this.selectedCardIndex = -1
        // 回合结束
        this.turnEnd()
      })
    },
    // 抽牌
    drawCard() {
      // 抽牌
      drawACard().then(res => {
        this.handList.push(res)
        // 更新全场手牌
        this.webSocket.send('{"msgType":"7","msgContent":"Draw a Card","msgTo":"System","msgFrom":' + (this.currentUserNumber - 1) + '}')

        this.confirmSurvival()
      })
    },
    // 确认是否存活
    confirmSurvival() {
      if (this.handList.includes("002") || this.handList.includes("003")) {
        // 如果手牌中有宿管阿姨
        this.killBy = "被宿管阿姨抓包了，选\"是\"使用求饶，选\"否\"被赶出宿舍。"
        this.boomTime = '0'
        this.getBoom = true
      } else if (this.handList.includes("01" + this.mainIdentity % 4)) {
        // 如果手牌中有自身弱点
        this.killBy = "被室友戳中了弱点，选\"是\"使用求饶，选\"否\"被赶出宿舍。"
        this.boomTime = '0'
        this.getBoom = true
      } else {
        this.iWantToDraw(this.tempCount)
      }
    },
    // 准备回合结束
    wantToEndMyTurn() {
      this.showEndSelect = true
      this.setWeakness = false
    },
    // 回合结束时抽牌
    async endWithDraw() {
      if (this.set) {
        this.webSocket.send('{"msgType":"11"}')
      }
      this.showEndSelect = false
      // 抽牌
      drawACard().then(res => {
        this.handList.push(res)
        // 更新全场手牌
        this.webSocket.send('{"msgType":"7","msgContent":"Draw a Card","msgTo":"System","msgFrom":' + (this.currentUserNumber - 1) + '}')

        if (this.handList.includes("002") || this.handList.includes("003")) {
          // 如果手牌中有宿管阿姨
          this.killBy = "被宿管阿姨抓包了，选\"是\"使用求饶，选\"否\"被赶出宿舍。"
          this.boomTime = '1'
          this.getBoom = true
        } else if (this.handList.includes("01" + this.mainIdentity % 4)) {
          // 如果手牌中有自身弱点
          this.killBy = "被室友戳中了弱点，选\"是\"使用求饶，选\"否\"被赶出宿舍。"
          this.boomTime = '1'
          this.getBoom = true
        } else {
          this.turnEnd()
        }
      })
    },
    // 使用求饶
    iWantToLive() {
      // 关闭弹窗
      this.getBoom = false
      // 优先使用“绿枸杞” 因为是明牌
      if (this.handList.includes("000")) {
        // 将刚抽到的宿管或弱点也移除
        useGreen(this.handList.pop()).then(() => {
          // 找到第一张“求饶”的索引
          let index = this.handList.indexOf("000")
          // 将其删除
          this.handList.splice(index, 1)
          // 通知全场你炸了
          this.webSocket.send('{"msgType":"5","msgContent":"Just a Little Short of Death","msgExtra":' + this.boomTime +
            ',"msgTo":"System","msgFrom":' + (this.currentUserNumber - 1) + '}')

          // 如果“掀被子”抽牌没有结束，继续抽
          if (this.drawCardNumber !== 0) {
            this.iWantToDraw(this.tempCount)
          }
        })
      } else {
        // 将刚抽到的宿管或弱点也移除
        useBeg(this.handList.pop()).then(() => {
          // 找到第一张“求饶”的索引
          let index = this.handList.indexOf("001")
          // 将其删除
          this.handList.splice(index, 1)
          // 通知全场你炸了
          this.webSocket.send('{"msgType":"5","msgContent":"Just a Little Short of Death","msgExtra":' + this.boomTime +
            ',"msgTo":"System","msgFrom":' + (this.currentUserNumber - 1) + '}')

          // 如果“掀被子”抽牌没有结束，继续抽
          if (this.drawCardNumber !== 0) {
            this.iWantToDraw(this.tempCount)
          }
        })
      }
    },
    // 不使用求饶
    iWantToDie() {
      this.getBoom = false
      // 将手牌全部丢弃，并将宿管或弱点洗回牌堆
      wantToDie(this.handList).then(() => {
        this.myTurn = false
        // 通知全场你真的炸了
        this.webSocket.send('{"msgType":"6","msgContent":"Death","msgTo":"System","msgFrom":' + (this.currentUserNumber - 1) + '}')
        this.beKilled = true
      })
    },
    // 使用“这不合理”
    isNotOk() {
      // 重置参数
      this.cardUsed = false
      this.usedCardNumber = ''
      this.useMessage = ''

      // 将手牌中一张”这不合理“丢弃
      dropACard("022").then(() => {
        // 找到手牌中第一张“这不合理”的索引
        let index = this.handList.indexOf("022")
        // 将其删除
        this.handList.splice(index, 1)
        // 更新全场手牌数显示
        this.webSocket.send('{"msgType":"7","msgContent":"Use a Card","msgTo":"System","msgFrom":' + (this.currentUserNumber - 1) + ',"msgExtra":1}')
        // 提醒全场自己使用了“这不合理”
        this.webSocket.send('{"msgType":"10","msgContent":"Not Ok","msgTo":"System","msgFrom":' + (this.currentUserNumber - 1) + '}')
      })
    },
    // 不使用“这不合理”
    isOk() {
      this.webSocket.send('{"msgType":"10","msgContent":"OK","msgTo":"System","msgFrom":' + (this.currentUserNumber - 1) + '}')
      // 重置参数
      this.cardUsed = false
      this.usedCardNumber = ''
      this.useMessage = ''

      this.resetParam()
    },
    /*
    效果生效
      020: 把风
      021: 装睡
      022: 这不合理
      023: 拿来吧你
      024: 掀被子
      025: 洗牌
      026: 反转
      027: 猜你想要
      028: 性情大变

      100: 我是好人
      101: 夺笋
      102: 我是老六
      103: 对子
      104: 三条
      */
    cardEffect() {
      // 等待状态结束
      this.wait = false
      if (this.selectedCardNumber !== '') {
        switch (this.selectedCardNumber) {
          case "020":
            seeTopCards().then(res => {
              this.topCards = res.rows
              this.showTop = true
            })
            this.resetSelect()
            break
          case "021":
            this.resetSelect()
            // 直接回合结束
            this.turnEnd()
            break
          case "022":
            break
          case "023":
            // 将手牌发送出去
            this.webSocket.send('{"msgType":"023","msgContent":"[' + this.handList.toString() + ']","msgTo":' + this.selectedUserName + ',"msgExtra":"0"}')
            this.resetSelect()
            break
          case "024":
            // 清空自己需要抽的牌数
            this.drawCardNumber = 0
            // 掀下家被子
            this.webSocket.send('{"msgType":"024","msgContent":"Lift the Quilt","msgTo":"System","msgFrom":' + (this.currentUserNumber - 1) + '}')
            this.resetSelect()
            break
          case "025":
            // 洗牌
            shuffleCards()
            this.resetSelect()
            break
          case "026":
            // 将回合顺序反转
            this.webSocket.send('{"msgType":"026","msgContent":"Reversal","msgTo":"System","msgFrom":' + (this.currentUserNumber - 1) + '}')
            this.resetSelect()
            break
          case "027":
            // 清空参数
            this.selectedCardNumber = ''
            this.selectedCardIndex = -1
            // 打开弹窗
            this.giveCard = true
            break
          case "028":
            // 改变身份
            this.webSocket.send('{"msgType":"028","msgContent":"Change Identity","msgExtra":' + this.selectedUserName + ',"msgTo":"System","msgFrom":' + (this.currentUserNumber - 1) + '}')
            this.resetSelect()
            break
        }
      } else if (this.selectedCombo !== '') {
        switch (this.selectedCombo) {
          case "100":
            if (this.useChange === '0') {
              this.showOrder = true
            } else {
              // 确认要拯救的阵营
              if (this.selectedCardsNumber.includes("030")) {
                // 发送消息
                this.webSocket.send('{"msgType":"105","msgContent":0,"msgTo":"All Players","msgFrom":' + (this.currentUserNumber - 1) + '}')
              } else if (this.selectedCardsNumber.includes("031")) {
                // 发送消息
                this.webSocket.send('{"msgType":"105","msgContent":1,"msgTo":"All Players","msgFrom":' + (this.currentUserNumber - 1) + '}')
              } else if (this.selectedCardsNumber.includes("032")) {
                // 发送消息
                this.webSocket.send('{"msgType":"105","msgContent":2,"msgTo":"All Players","msgFrom":' + (this.currentUserNumber - 1) + '}')
              } else if (this.selectedCardsNumber.includes("033")) {
                // 发送消息
                this.webSocket.send('{"msgType":"105","msgContent":3,"msgTo":"All Players","msgFrom":' + (this.currentUserNumber - 1) + '}')
              }
              this.resetParam()
            }
            break
          case "101":
            // 确认要击毙的阵营
            if (this.selectedCardsNumber.includes("030")) {
              // 发送消息
              this.webSocket.send('{"msgType":"101","msgContent":0,"msgTo":"All Players","msgFrom":' + (this.currentUserNumber - 1) + '}')
            } else if (this.selectedCardsNumber.includes("031")) {
              // 发送消息
              this.webSocket.send('{"msgType":"101","msgContent":1,"msgTo":"All Players","msgFrom":' + (this.currentUserNumber - 1) + '}')
            } else if (this.selectedCardsNumber.includes("032")) {
              // 发送消息
              this.webSocket.send('{"msgType":"101","msgContent":2,"msgTo":"All Players","msgFrom":' + (this.currentUserNumber - 1) + '}')
            } else if (this.selectedCardsNumber.includes("033")) {
              // 发送消息
              this.webSocket.send('{"msgType":"101","msgContent":3,"msgTo":"All Players","msgFrom":' + (this.currentUserNumber - 1) + '}')
            }
            this.resetParam()
            break
          case "102":
            showDroppedCards().then(res => {
              this.dropList = res.rows
              this.seeDrop = true
            })
            break
          case "103":
            this.webSocket.send('{"msgType":"103","msgContent":"Double","msgTo":' + this.selectedUserName + ',"msgFrom":' + (this.currentUserNumber - 1) + ',"msgExtra":"Send"}')
            this.resetParam()
            break
          case "104":
            this.showCards = true
            break
        }
      } else {
        this.$modal.msgError("bu~bu~出错啦!")
      }
    },
    // 重置参数（全部）
    resetParam() {
      this.resetSelect()
      this.resetCombo()
    },
    // 重置参数（出牌）
    resetSelect() {
      this.selectedCardNumber = ''
      this.selectedCardIndex = -1

      this.selectedUserName = '好人'
      this.selectedUserIndex = -1

      // this.selectedCombo = ''
      // this.selectedCardsNumber = []
      // this.selectedCardsIndex = []
      this.showTarget = false
    },
    // 把风中改变顺序（添加）
    changeCardOrder(cardNumber) {
      if (this.changedTopCards.includes(cardNumber)) {
        // 有，则判断重复卡的情况
        let lenTop = 0
        let lenChange = 0
        // 获取长度
        for (let i = 0; i < this.topCards.length; i++) {
          if (this.topCards[i] === cardNumber) {
            lenTop += 1
          }
        }
        for (let i = 0; i < this.changedTopCards.length; i++) {
          if (this.changedTopCards[i] === cardNumber) {
            lenChange += 1
          }
        }

        if (lenChange < lenTop) {
          // 未全部放入，则放入一张
          this.changedTopCards.push(cardNumber)
        }
      } else {
        // 没有就添加
        this.changedTopCards.push(cardNumber)
      }
    },
    // 把风中改变顺序（移除）
    removeFromChange(index) {
      this.changedTopCards.splice(index, 1)
    },
    // 把风结束，将卡牌放回
    confirmCardOrder() {
      endSeeTopCards(this.changedTopCards).then(() => {
        this.showTop = false
        this.topCards = []
        this.changedTopCards = []
      })
    },
    // 抽牌（掀被子）
    iWantToDraw(start) {
      this.drawTip = false

      if (start < this.drawCardNumber) {
        this.tempCount = start + 1
        this.drawCard()
        return
      }

      this.drawMessage = ''
      this.tempCount = 0
      this.drawCardNumber = 0
      this.webSocket.send('{"msgType":"024","msgContent":"Clear Draw Count","msgTo":"System"}')
    },
    // 继续掀被子
    iWantToLift() {
      this.drawTip = false

      // 选中手牌中的一张“掀被子”
      this.selectedCardNumber = "024"
      // 锁定手牌中第一张“掀被子”的索引
      this.selectedCardIndex = this.handList.indexOf("024")

      this.useCard()
    },
    // 选牌（猜你想要）
    selectAHandCard(number, index) {
      this.selectedCardNumber = number
      this.selectedCardIndex = index
    },
    // 确定选牌（猜你想要）
    confirmCard() {
      // 将选择的牌从手里移除
      this.handList.splice(this.selectedCardIndex, 1)
      this.giveCard = false
      // 将手牌发射出去
      this.webSocket.send('{"msgType":"027","msgContent":' + this.selectedCardNumber + ',"msgTo":' + this.selectedUserName + '}')

      this.resetSelect()
    },
    /*
    使用判断
      100: 我是好人
      101: 夺笋
      102: 我是老六
      103: 对子
      104: 三条
     */
    comboUseable(number, list, use = false) {
      switch (number) {
        case "100":
          if (use) {
            return !(list.includes("035") && (this.getCardCount("030", list) === 2 || this.getCardCount("031", list) === 2
              || this.getCardCount("032", list) === 2 || this.getCardCount("033", list) === 2) && list.length === 3)
          } else {
            return !(list.includes("035") && (this.getCardCount("030", list) >= 2 || this.getCardCount("031", list) >= 2
              || this.getCardCount("032", list) >= 2 || this.getCardCount("033", list) >= 2))
          }
        case "101":
          if (use) {
            return !(list.includes("034") && (this.getCardCount("030", list) === 2 || this.getCardCount("031", list) === 2
              || this.getCardCount("032", list) === 2 || this.getCardCount("033", list) === 2) && list.length === 3)
          } else {
            return !(list.includes("034") && (this.getCardCount("030", list) >= 2 || this.getCardCount("031", list) >= 2
              || this.getCardCount("032", list) >= 2 || this.getCardCount("033", list) >= 2));
          }
        case "102":
          if (use) {
            return !(list.includes("034") && list.includes("035") && list.length === 2)
          } else {
            return !(list.includes("034") && list.includes("035"))
          }
        case "103":
          if (use) {
            return !((this.getCardCount("020", list) === 2 || this.getCardCount("021", list) === 2 || this.getCardCount("022", list) === 2
              || this.getCardCount("023", list) === 2 || this.getCardCount("024", list) === 2 || this.getCardCount("025", list) === 2
              || this.getCardCount("026", list) === 2 || this.getCardCount("027", list) === 2 || this.getCardCount("028", list) === 2
              || this.getCardCount("030", list) === 2 || this.getCardCount("031", list) === 2 || this.getCardCount("032", list) === 2
              || this.getCardCount("033", list) === 2 || this.getCardCount("034", list) === 2 || this.getCardCount("035", list) === 2
              || this.getCardCount("000", list) === 2 || this.getCardCount("001", list) === 2) && list.length === 2)
          } else {
            return !(this.getCardCount("020", list) >= 2 || this.getCardCount("021", list) >= 2 || this.getCardCount("022", list) >= 2
              || this.getCardCount("023", list) >= 2 || this.getCardCount("024", list) >= 2 || this.getCardCount("025", list) >= 2
              || this.getCardCount("026", list) >= 2 || this.getCardCount("027", list) >= 2 || this.getCardCount("028", list) >= 2
              || this.getCardCount("030", list) >= 2 || this.getCardCount("031", list) >= 2 || this.getCardCount("032", list) >= 2
              || this.getCardCount("033", list) >= 2 || this.getCardCount("034", list) >= 2 || this.getCardCount("035", list) >= 2
              || this.getCardCount("000", list) >= 2 || this.getCardCount("001", list) >= 2)
          }
        case "104":
          if (use) {
            return !((this.getCardCount("020", list) === 3 || this.getCardCount("021", list) === 3 || this.getCardCount("022", list) === 3
              || this.getCardCount("023", list) === 3 || this.getCardCount("024", list) === 3 || this.getCardCount("025", list) === 3
              || this.getCardCount("026", list) === 3 || this.getCardCount("027", list) === 3 || this.getCardCount("028", list) === 3
              || this.getCardCount("030", list) === 3 || this.getCardCount("031", list) === 3 || this.getCardCount("032", list) === 3
              || this.getCardCount("033", list) === 3 || this.getCardCount("034", list) === 3 || this.getCardCount("035", list) === 3
              || this.getCardCount("000", list) === 3 || this.getCardCount("001", list) === 3) && list.length === 3)
          } else {
            return !(this.getCardCount("020", list) >= 3 || this.getCardCount("021", list) >= 3 || this.getCardCount("022", list) >= 3
              || this.getCardCount("023", list) >= 3 || this.getCardCount("024", list) >= 3 || this.getCardCount("025", list) >= 3
              || this.getCardCount("026", list) >= 3 || this.getCardCount("027", list) >= 3 || this.getCardCount("028", list) >= 3
              || this.getCardCount("030", list) >= 3 || this.getCardCount("031", list) >= 3 || this.getCardCount("032", list) >= 3
              || this.getCardCount("033", list) >= 3 || this.getCardCount("034", list) >= 3 || this.getCardCount("035", list) >= 3
              || this.getCardCount("000", list) >= 3 || this.getCardCount("001", list) >= 3)
          }
      }
    },
    // 返回手牌中某张卡牌的张数
    getCardCount(cardNumber, list) {
      return list.filter(function (card) {
        return card === cardNumber
      }).length
    },
    // 选择组合技
    selectCombo(number) {
      this.selectedCombo = number
      this.showCombo = true
    },
    // 组合技选择多张手牌
    selectSomeCards(item, index) {
      // 如果这张牌已经被选中，则取消选中，从数组中去除；未被选中，则放入数组中
      if (this.selectedCardsIndex.includes(index)) {
        this.selectedCardsNumber.splice(this.selectedCardsIndex.indexOf(index), 1)
        for (let i = 0; i < this.selectedCardsIndex.length; i++) {
          if (this.selectedCardsIndex[i] === index) {
            this.selectedCardsIndex.splice(i, 1)
            break
          }
        }
      } else {
        this.selectedCardsNumber.push(item)
        this.selectedCardsIndex.push(index)
      }
    },
    // 确认使出组合技
    confirmCards() {
      // 判断能否使用
      if (this.comboUseable(this.selectedCombo, this.selectedCardsNumber, true)) {
        // 不可以使用
        this.$modal.msgError("不符合使用条件")
      } else {
        // 可以使用
        // 关闭弹窗
        this.showCombo = false
        this.drawer = false

        this.selectedCardNumber = ''

        if (this.selectedCombo === "103" || this.selectedCombo === "104") {
          this.selectedUserName = "好人"
          this.showTarget = true
        }
        /*else if (this.selectedCombo === "102") {
          this.selectedUserName = "自己"
        }*/
        else {
          this.selectedUserName = "好人"
          this.useCombo()
        }
      }
    },
    // 重置组合技相关参数
    resetCombo() {
      this.selectedCombo = ''
      this.selectedCardsNumber = []
      this.selectedCardsIndex = []

      this.cardIWant = ''

      this.showCombo = false
      this.drawer = false
    },
    // 使用组合技
    useCombo() {
      // 进入等待状态
      this.wait = true
      // 将组合技选中的牌放入弃牌堆
      dropCards(this.selectedCardsNumber).then(() => {
        // 从手牌中将其移除
        // 有多个会全部删除
        // this.handList = this.handList.filter(item => !this.selectedCardsNumber.includes(item))
        // 可能存在会有一个删除不掉
        // this.selectedCardsIndex.sort()
        // for (let i = this.selectedCardsIndex.length - 1; i >= 0; i--) {
        //   this.handList.splice(this.selectedCardsIndex[i], 1)
        // }
        // 还得是ChatGPT
        this.selectedCardsIndex.sort((a, b) => b - a); // 降序排列索引，确保正确删除元素
        this.selectedCardsIndex.forEach(index => {
          if (index >= 0 && index < this.handList.length) {
            this.handList.splice(index, 1)
          }
        })

        // 更新全场手牌数显示
        this.webSocket.send('{"msgType":"7","msgContent":"Use a Combo","msgTo":"System","msgFrom":' + (this.currentUserNumber - 1) + ',"msgExtra":' + (this.selectedCardsNumber.length) + '}')
        // 询问全场是否使用“这不合理”
        this.webSocket.send('{"msgType":"9","msgContent":' + this.selectedCombo + ',"msgExtra":' + this.selectedUserName + ',"msgTo":"System","msgFrom":' + (this.currentUserNumber - 1) + '}')
      })
    },
    // “我是好人”生效  '0':正序  '1':逆序
    iAmHero(order) {
      this.showOrder = false
      // 确认要拯救的阵营
      if (this.selectedCardsNumber.includes("030")) {
        // 自己先拿牌
        if (this.mainIdentity % 4 === 0) {
          this.getLifeFromDrop(0, order)
        } else {
          // 发送消息
          this.webSocket.send('{"msgType":"100","msgContent":0,"msgTo":"My Party","msgFrom":' + (this.currentUserNumber - 1) + ',"msgExtra":' + order + '}')
        }
      } else if (this.selectedCardsNumber.includes("031")) {
        // 自己先拿牌
        if (this.mainIdentity % 4 === 1) {
          this.getLifeFromDrop(1, order)
        } else {
          // 发送消息
          this.webSocket.send('{"msgType":"100","msgContent":1,"msgTo":"My Party","msgFrom":' + (this.currentUserNumber - 1) + ',"msgExtra":' + order + '}')
        }
      } else if (this.selectedCardsNumber.includes("032")) {
        // 自己先拿牌
        if (this.mainIdentity % 4 === 2) {
          this.getLifeFromDrop(2, order)
        } else {
          // 发送消息
          this.webSocket.send('{"msgType":"100","msgContent":2,"msgTo":"My Party","msgFrom":' + (this.currentUserNumber - 1) + ',"msgExtra":' + order + '}')
        }
      } else if (this.selectedCardsNumber.includes("033")) {
        // 自己先拿牌
        if (this.mainIdentity % 4 === 3) {
          this.getLifeFromDrop(3, order)
        } else {
          // 发送消息
          this.webSocket.send('{"msgType":"100","msgContent":3,"msgTo":"My Party","msgFrom":' + (this.currentUserNumber - 1) + ',"msgExtra":' + order + '}')
        }
      }
      this.resetParam()
    },
    // “我是好人”效果 -> 从弃牌区拿“求饶”
    getLifeFromDrop(number, order) {
      // 选知道弃牌区里有什么
      showDroppedCards().then(res => {
        this.dropList = res.rows
        // console.log(this.dropList)
        if (this.dropList.includes("001")) {
          // 有“求饶”牌
          // 放到手里
          this.handList.push("001")
          // 从弃牌堆中移除一张
          getLife().then(() => {
            // 发送消息
            this.webSocket.send('{"msgType":"100","msgContent":' + number + ',"msgTo":"Thank you","msgFrom":' + (this.currentUserNumber - 1) + ',"msgExtra":' + order + '}')
          })
        } else {
          // 如果没有
          this.$modal.msgError("弃牌堆已经没有“求饶”啦")
          // 发送消息
          this.webSocket.send('{"msgType":"100","msgContent":' + number + ',"msgTo":"My Party","msgFrom":' + (this.currentUserNumber - 1) + ',"msgExtra":' + order + '}')
        }
      })
    },
    // 选择想要的卡牌的索引
    selectIndexWant(index) {
      this.cardIndexIWant = index
    },
    // “我是老六”指定卡牌 生效
    confirmIWantFromDrop() {
      this.seeDrop = false
      getCardFromDrop(this.cardIndexIWant).then(() => {
        this.handList.push(this.dropList[this.cardIndexIWant])
        this.cardIndexIWant = ''
        // 更新全场手牌数显示
        this.webSocket.send('{"msgType":"7","msgContent":"Draw A Card From Dropzone","msgTo":"System","msgFrom":' + (this.currentUserNumber - 1) + '}')
        this.resetParam()
      })

    },
    // 选择想要的卡
    selectWantCard(number) {
      this.cardIWant = number
    },
    // “三条”指定卡牌 生效
    confirmIWant() {
      this.showCards = false
      this.webSocket.send('{"msgType":"104","msgContent":' + this.cardIWant + ',"msgTo":' + this.selectedUserName + ',"msgFrom":' + (this.currentUserNumber - 1) + ',"msgExtra":"Send"}')
      this.resetParam()
    },
    // 回合结束时放一张弱点牌在牌堆顶
    endWithWeak() {
      this.showEndSelect = false
      this.setWeakness = true
      this.$modal.msg("请放置一张弱点牌，或点\"回合结束\"重新选择。")
    },
    // 放置弱点
    setCard() {
      // 将牌放到牌堆顶
      setACard(this.selectedCardNumber).then(() => {
        // 将选中的牌从手牌中移除
        this.handList.splice(this.selectedCardIndex, 1)
        // 更新全场手牌
        this.webSocket.send('{"msgType":"7","msgContent":"Set a Card","msgTo":"System","msgFrom":' + (this.currentUserNumber - 1) + '}')
        // 通知全场放弱点
        this.webSocket.send('{"msgType":"8","msgContent":"Set A Weakness","msgTo":"System","msgFrom":' + (this.currentUserNumber - 1) + '}')

        // 重置参数
        this.selectedCardNumber = ''
        this.selectedCardIndex = -1
        this.setWeakness = false
        // 放置之后结束回合
        this.turnEnd()
      })
    },
    // 回合结束
    turnEnd() {
      // 进入等待状态
      this.wait = true
      // 手牌过多 -> 弃牌
      if (this.handList.length > this.handLimit) {
        this.needDrop = true
        this.$modal.msg("手牌大于" + this.handLimit + "张，需要弃牌")
      } else {
        this.needDrop = false
        // 等待状态结束
        this.wait = false
        // 回合结束
        this.myTurn = false
        // 通知下一位玩家
        this.webSocket.send('{"msgType":"4","msgContent":"Next Turn","msgTo":"System","msgFrom":' + (this.currentUserNumber - 1) + '}')
      }
    },
    // 留下来观战
    seeGame() {
      this.beKilled = false
      this.handList = []
    },
    // 再来一局
    playAgain() {
      // 重置所有需要重置的变量
      this.gameEnd = false
      this.victory = ''
      this.start = false
      this.handList = []
      this.identity = []
      this.mainIdentity = 0
      this.subIdentity = 0
      this.identitySelect = false
      this.myTurn = false
      this.killBy = ''
      this.beKilled = false
      this.selectedCardNumber = ''
      this.selectedCardIndex = -1
      // 向后台请求新的玩家列表
      this.webSocket.send('{"msgType":"99","msgContent":"Game End","msgTo":"System"}')
    },
    // 退出游戏
    exitRoom() {
      this.beKilled = false
      this.gameEnd = false
      this.handList = []
      this.wsDestroy()
      this.$router.push({path: '/game/start'})
    },
    /**
     * 初始化ws
     */
    wsInit() {
      // console.log("userName: " + this.$store.state.user.ipp)
      // console.log("user: " + this.ipAddr)
      this.ws = 'ws://' + this.serverIp + ':8080/websocket/message/' + this.$store.state.user.name
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
    /* 发送消息
      msgType: 消息类型
      msgContent: 消息内容
      msgExtra: 额外内容
      msgTo: 消息接收对象
      msgFrom: 消息发送人
      msgSub: 多出的内容
     */
    subSend() {
      // this.webSocket.send("11111")
    },
    wsOpenHanler(event) {
      console.log('ws建立连接成功')
      this.webSocket.send('{"msgType":"0","msgContent":"Link Start","msgTo":"System"}')
    },
    /*
    接收消息
      -1: 有人断开连接
      0: 进入房间
      1: 开始游戏
      2: 身份选择完成
      3: 起始手牌抽取完毕
      4: 回合转移
      5: 有人使用了“求饶”
      6: 有人因宿管或弱点出局
      7: 有人手牌发生变化
      8: 有人放了张弱点
      9: 询问是否合理
      10: 出牌有效 -> 执行卡牌效果
      11: 可继续放置弱点
      90: 轮流抽取身份
      91: 轮流抽取手牌
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
      105: “我是好人” 变体
      */
    wsMessageHanler(e) {
      // console.log(e.data)
      const msg = JSON.parse(e.data)
      switch (msg.msgType) {
        case '-1':
          this.userList = msg.msgContent.substring(1, msg.msgContent.length - 1).split(', ')
          for (let i = 0; i < this.userList.length; i++) {
            this.userList[i] = JSON.parse(this.userList[i])
            if (this.userList[i].userName === this.$store.state.user.name) {
              this.currentUserNumber = this.userList[i].userNumber
            }
          }
          break
        case '0':
          this.userList = msg.msgContent.substring(1, msg.msgContent.length - 1).split(', ')
          for (let i = 0; i < this.userList.length; i++) {
            this.userList[i] = JSON.parse(this.userList[i])
            if (this.userList[i].userName === this.$store.state.user.name) {
              this.currentUserNumber = this.userList[i].userNumber
            }
          }
          // console.log(this.userList)
          break
        case '1':
          this.handLimit = msg.handLimit
          this.useChange = msg.useChange
          this.$modal.msgSuccess("游戏开始")
          this.start = true
          break
        case '2':
          this.userList = msg.msgContent.substring(1, msg.msgContent.length - 1).split(', ')
          for (let i = 0; i < this.userList.length; i++) {
            this.userList[i] = JSON.parse(this.userList[i])
          }
          break
        case '3':
          if (this.currentUserNumber === 1) {
            creatDeck(this.userList.length)
          }
          this.userList = msg.msgContent.substring(1, msg.msgContent.length - 1).split(', ')
          for (let i = 0; i < this.userList.length; i++) {
            this.userList[i] = JSON.parse(this.userList[i])
            if (this.userList[i].userName === this.$store.state.user.name) {
              this.myTurn = this.userList[i].turn
            }
          }
          break
        case '4':
          this.userList = msg.msgContent.substring(1, msg.msgContent.length - 1).split(', ')
          for (let i = 0; i < this.userList.length; i++) {
            this.userList[i] = JSON.parse(this.userList[i])
            if (this.userList[i].userName === this.$store.state.user.name) {
              this.myTurn = this.userList[i].turn
              if (this.myTurn) {
                this.$modal.msgSuccess("你的回合，抽牌")
              }
            }
          }
          break
        case '5':
          this.$modal.msgError(msg.msgContent)
          this.userList = msg.msgExtra.substring(1, msg.msgExtra.length - 1).split(', ')
          for (let i = 0; i < this.userList.length; i++) {
            this.userList[i] = JSON.parse(this.userList[i])
          }
          if (this.myTurn && this.boomTime === '1') {
            this.turnEnd()
            this.boomTime = '0'
          }
          break
        case '6':
          this.$modal.msgError(msg.msgContent)
          this.userList = msg.msgExtra.substring(1, msg.msgExtra.length - 1).split(', ')
          for (let i = 0; i < this.userList.length; i++) {
            this.userList[i] = JSON.parse(this.userList[i])
            if (this.userList[i].userName === this.$store.state.user.name) {
              this.myTurn = this.userList[i].turn
              this.$modal.msgSuccess("你的回合")
            }
          }
          break
        case '7':
          this.userList = msg.msgExtra.substring(1, msg.msgExtra.length - 1).split(', ')
          for (let i = 0; i < this.userList.length; i++) {
            this.userList[i] = JSON.parse(this.userList[i])
          }
          break
        case '8':
          this.$modal.msgError(msg.msgContent)
          this.set = true
          break
        case '9':
          this.cardUsed = false
          this.usedCardNumber = ''
          this.useMessage = ''

          if (this.$store.state.user.name !== msg.msgFrom) {
            this.usedCardNumber = msg.msgExtra
            this.useMessage = msg.msgContent
            this.cardUsed = true
          }
          break
        case '10':
          if (this.myTurn && msg.msgContent.startsWith("Can")) {
            this.$modal.msgSuccess("行为有效")
            this.cardEffect()
          } else if (this.myTurn && !msg.msgContent.startsWith("Can")) {
            this.$modal.msgError("行为无效")
            if (this.drawCardNumber !== 0) {
              this.drawTip = true
            }
            // 退出等待状态
            this.wait = false
          }
          break
        case "11":
          this.set = false
          break
        case '90':
          getCharacters().then(res => {
            this.identity = res.rows
            this.identitySelect = true
            this.webSocket.send('{"msgType":"90","msgContent":"Draw Identity","msgTo":' + (this.currentUserNumber + 1) + '}')
          })
          break
        case '91':
          getInitialCard().then(res => {
            this.handList = res.rows
            this.webSocket.send('{"msgType":"3","msgContent":"Draw Initial Card","msgTo":' + (this.currentUserNumber + 1) + '}')
          })
          break
        case '99':
          this.victory = msg.msgContent
          this.gameEnd = true
          break
        case '023':
          // 暂存手牌
          const hand = this.handList
          const from = msg.msgContent.substring(1, msg.msgContent.length - 1).split(',')
          if (from[0] === "") {
            this.handList = []
          } else {
            // 替换成新手牌
            this.handList = msg.msgContent.substring(1, msg.msgContent.length - 1).split(',')
          }
          // 自己是被替换的那一方
          if (!this.myTurn) {
            // 将自己的手牌发给对面
            this.webSocket.send('{"msgType":"023","msgContent":"[' + hand.toString() + ']","msgTo":' + msg.msgFrom + ',"msgExtra":"1"}')
          }

          // 判断有没有坑
          this.confirmSurvival()
          break
        case '024':
          this.drawCardNumber = parseInt(msg.msgContent)
          this.drawMessage = "你被" + msg.msgFrom + "掀了被子，将要抽" + msg.msgContent + "张牌。"
          this.drawTip = true
          break
        case '026':
          this.turnEnd()
          break
        case '027':
          const card = msg.msgContent
          this.handList.push(card)
          // 判断有没有坑
          this.confirmSurvival()
          break
        case '028':
          this.userList = msg.msgExtra.substring(1, msg.msgExtra.length - 1).split(', ')
          for (let i = 0; i < this.userList.length; i++) {
            this.userList[i] = JSON.parse(this.userList[i])
            if (this.userList[i].userName === this.$store.state.user.name) {
              // 改变身份
              this.mainIdentity = this.userList[i].identity
              this.subIdentity = this.userList[i].subIdentity
            }
          }
          break
        case '100':
          // 如果不是出牌的玩家
          if (!this.myTurn) {
            // 获取被救阵营
            let lifeNumber = parseInt(msg.msgContent)
            // 如果自己是该阵营
            if (this.mainIdentity % 4 === lifeNumber) {
              this.getLifeFromDrop(lifeNumber, msg.msgExtra)
            } else {
              // 发送消息
              this.webSocket.send('{"msgType":"100","msgContent":' + lifeNumber + ',"msgTo":"My Party","msgFrom":' + (this.currentUserNumber - 1) + ',"msgExtra":' + msg.msgExtra + '}')
            }
          }
          break
        case '101':
          // 获取死亡阵营
          let deathNumber = parseInt(msg.msgContent)
          // 如果自己是该阵营
          if (this.mainIdentity % 4 === deathNumber) {
            this.killBy = "你被" + this.userList[msg.msgFrom].userName + "举报了，选\"是\"使用求饶，选\"否\"被赶出宿舍。"
            this.boomTime = '2'
            this.getBoom = true
          }
          break
        case '103':
          if (msg.msgExtra === 'Send') {
            // 交出卡牌的一方
            // 生成一个基于时间的随机数
            let now = new Date()
            let randomIndex = now.getSeconds() % this.handList.length
            // 将随机的卡牌取出
            let card = this.handList[randomIndex]
            // 将该卡牌从手牌中去除
            this.handList.splice(randomIndex, 1)
            // 将这张手牌发送出去
            this.webSocket.send('{"msgType":"103","msgContent":' + card + ',"msgTo":' + msg.msgFrom + ',"msgFrom":' + (this.currentUserNumber - 1) + ',"msgExtra":"Receive"}')
          } else if (msg.msgExtra === 'Receive') {
            // 接收卡牌的一方
            // 所有玩家更新前台手牌数显示
            this.userList = msg.msgSub.substring(1, msg.msgSub.length - 1).split(', ')
            for (let i = 0; i < this.userList.length; i++) {
              this.userList[i] = JSON.parse(this.userList[i])
            }
            // 将卡牌放入手牌中
            if (this.currentUserNumber - 1 === msg.msgTo) {
              this.handList.push(msg.msgContent)
              // 进行死亡判断
              this.confirmSurvival()
            }
          }
          break
        case '104':
          if (msg.msgExtra === 'Send') {
            // 交出卡牌的一方
            let cardIndex = this.handList.indexOf(msg.msgContent)
            if (cardIndex > -1) {
              // 如果手中有对面需要的牌
              // 将牌从手中移除
              this.handList.splice(cardIndex, 1)
              // 将牌发送出去
              this.webSocket.send('{"msgType":"104","msgContent":' + msg.msgContent + ',"msgTo":' + msg.msgFrom + ',"msgFrom":' + (this.currentUserNumber - 1) + ',"msgExtra":"Receive"}')
            } else {
              // 如果没有这张牌
              this.webSocket.send('{"msgType":"104","msgContent":"哈哈哈哈，对面根本没有这牌","msgTo":' + msg.msgFrom + ',"msgFrom":' + (this.currentUserNumber - 1) + ',"msgExtra":"Receive"}')
            }
          } else {
            // 接收卡牌的一方
            // 所有玩家更新前台手牌数显示
            this.userList = msg.msgSub.substring(1, msg.msgSub.length - 1).split(', ')
            for (let i = 0; i < this.userList.length; i++) {
              this.userList[i] = JSON.parse(this.userList[i])
            }

            // 判断卡牌接收者
            if (this.currentUserNumber - 1 === msg.msgTo) {
              if (msg.msgContent.startsWith("0")) {
                // 获得了卡牌
                this.handList.push(msg.msgContent)
                // 进行死亡判断
                this.confirmSurvival()
              } else {
                // 没有获得卡牌
                this.$modal.msgError(msg.msgContent)
              }
            }
          }
          break
        case '105':
          // 获取被救阵营
          let greenNumber = parseInt(msg.msgContent)
          // 如果自己是该阵营
          if (this.mainIdentity % 4 === greenNumber) {
            this.handList.push("000")
          }
          break
      }
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
  }
}
</script>

<style scoped lang="scss">
.main-room {
  margin: 0.5% 1% 0 1%;
  height: 820px;
  display: flex;
}

.user-side {
  height: 100%;
  width: 10%;
  overflow: auto;
  // 阴影
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.game-main {
  margin-left: 1%;
  width: 90%;
}

.back-button {
  margin: 0.5% 0 0 0.5%;
}

.setting-button {
  position: absolute;
  margin: 0.3% 0.5% 0 0;
  right: 7%;
}

.start-button {
  position: absolute;
  margin: 0.3% 0.5% 0 0;
  right: 0.5%;
}

.tips {
  margin-left: 10%;
}

.user {
  margin: 3% 4% 0 4%;
}

.user-background-0 {
  background-color: #5A834C;
  color: white;
}

.user-background-1 {
  background-color: #3A9FBB;
  color: white;
}

.user-background-2 {
  background-color: #CFAA1A;
  color: white;
}

.user-background-3 {
  background-color: #5E3E58;
  color: white;
}

.function-zone {
  position: absolute;
  right: 2%;
}

.show-card {
  width: 22%;

  ::v-deep .el-card__body {
    padding: 0;
  }
}

.select-end-method {
  //width: 40%;
  display: flex;
  justify-content: center;
  align-items: center;
  //left: 30%;
  top: -30%;
}

.cards {
  display: flex;
  position: fixed;
  left: 41%;
  top: 20%;
}

.deck-zone {
  width: 180px;
  margin-right: 5%;

  ::v-deep .el-card__body {
    padding: 0;
  }
}

.drop-zone {
  width: 180px;
  margin-left: 5%;
  margin-right: 5%;

  ::v-deep .el-card__body {
    padding: 0;
  }
}

.show-drop {
  display: flex;
}

.drop-cards {
  width: 70%;
  margin-left: 5%;
  display: inline-block;

  ::v-deep .el-card__body {
    padding: 0;
  }
}

.one-drop-card {
  display: inline-block;
  width: 100px;
  height: 150px;
  margin-right: 1%;
  margin-bottom: 0.5%;
}

.drop-pic {
  width: 100%;
}

.drop-confirm-zone-combo {
  width: 42%;
}

.drop-confirm-zone {
  width: 20%;
}

.drop-confirm {
  border: 0;

  ::v-deep .el-card__body {
    padding: 0;
  }
}

.hand-zone {
  display: flex;
  position: fixed;
  width: 100%;
  top: 68%;
}

.hand-card {
  width: 180px;
  height: 272px;

  ::v-deep .el-card__body {
    padding: 0;
  }
}

.card-pic {
  width: 100%;
}

.identity-card {
  width: 45%;
  margin: 0 5%;

  ::v-deep .el-card__body {
    padding: 0;
  }
}

.show-identity-card {
  width: 50%;
  margin: 0 25%;

  ::v-deep .el-card__body {
    padding: 0;
  }
}

.show-top-card {
  width: 180px;
  margin: 3% 5% 3% 5%;

  ::v-deep .el-card__body {
    padding: 0;
  }
}

.identity-pic {
  width: 100%;
}

.selected {
  border: #00afff 2px solid;
}

.operate-dialog {
  ::v-deep .el-card__body {
    padding: 10px 8px;
  }
}

.target-card {
  width: 180px;
  margin: 0 1%;

  ::v-deep .el-card__body {
    padding: 0;
  }
}

.target-card-hand {
  display: inline-block;
  width: 160px;
  margin: 0 1%;

  ::v-deep .el-card__body {
    padding: 0;
  }
}

.clearfix:before,
.clearfix:after {
  display: table;
  content: "";
}

.clearfix:after {
  clear: both
}

.combo-card {
  width: 360px;
  margin: 0 0 3% 5%;
}

.combo-example {
  width: 100px;
  vertical-align: middle;
}

.combo-example-text {
  font-size: 100px;
  vertical-align: middle;
  margin: 0 1%;
}
</style>
