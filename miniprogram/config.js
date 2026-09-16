/**
 * 全局配置
 * 真机调试时把 BASE_URL 换成电脑的局域网 IP，例如 http://192.168.1.10:8080
 * 开发者工具里需要在「详情 → 本地设置」勾选「不校验合法域名」
 */
const BASE_URL = 'http://localhost:8080';

const TOKEN_KEY = 'lf_token';
const USER_KEY = 'lf_user';
const DEVICE_KEY = 'lf_device_id';

const TYPE_LOST = 'LOST';
const TYPE_FOUND = 'FOUND';

/**
 * 订阅消息模板 id：在微信公众平台「订阅消息」里申请模板后填进来。
 * 留空时前端不会调 wx.requestSubscribeMessage，后端也只写站内信（app.subscribe.enabled=false）。
 */
const SUBSCRIBE_TEMPLATE_IDS = [];

module.exports = {
  BASE_URL,
  TOKEN_KEY,
  USER_KEY,
  DEVICE_KEY,
  TYPE_LOST,
  TYPE_FOUND,
  SUBSCRIBE_TEMPLATE_IDS
};
