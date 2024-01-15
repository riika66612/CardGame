import request from '@/utils/request'

// 生成身份牌堆
export function createCharacters() {
  return request({
    url: '/project/card/create/character',
    method: 'post'
  })
}

// 生成起始牌堆
export function creatInitialDeck() {
  return request({
    url: '/project/card/create/deck/initial',
    method: 'post'
  })
}

// 生成完整牌堆
export function creatDeck(number) {
  return request({
    url: '/project/card/create/deck',
    method: 'post',
    data: number
  })
}

// 抽身份
export function getCharacters() {
  return request({
    url: '/project/card/character',
    method: 'get'
  })
}

// 抽起始手牌
export function getInitialCard() {
  return request({
    url: '/project/card/deal/initial',
    method: 'get'
  })
}

// 抽牌
export function drawACard() {
  return request({
    url: '/project/card/deal',
    method: 'get'
  })
}

// 使用求饶
export function useBeg(number) {
  return request({
    url: '/project/card/use/beg',
    method: 'post',
    data: number
  })
}

// 不使用求饶
export function wantToDie(hand) {
  return request({
    url: '/project/card/die',
    method: 'post',
    data: hand
  })
}

// 弃一张牌
export function dropACard(cardNumber) {
  return request({
    url: '/project/card/drop',
    method: 'post',
    data: cardNumber
  })
}

// 放置一张牌到牌堆顶
export function setACard(cardNumber) {
  return request({
    url: '/project/card/set',
    method: 'post',
    data: cardNumber
  })
}

// 查看弃牌堆
export function showDroppedCards() {
  return request({
    url: '/project/card/show/drop',
    method: 'get'
  })
}

// 查看牌堆数量
export function showDeckNumber() {
  return request({
    url: '/project/card/show/deck',
    method: 'get'
  })
}

// 使用 020 把风
export function seeTopCards() {
  return request({
    url: '/project/card/use/wind',
    method: 'post'
  })
}

// 使用 020 把风结束
export function endSeeTopCards(cards) {
  return request({
    url: '/project/card/use/wind/end',
    method: 'post',
    data: cards
  })
}

// 使用 025 洗牌
export function shuffleCards() {
  return request({
    url: '/project/card/use/shuffle',
    method: 'post'
  })
}
