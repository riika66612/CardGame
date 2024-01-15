import request from '@/utils/request'

// 生成身份牌堆
export function getIpAddr() {
  return request({
    url: '/project/ip',
    method: 'post'
  })
}
