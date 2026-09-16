const { TOKEN_KEY, USER_KEY, DEVICE_KEY } = require('./config');

App({
  globalData: {
    token: '',
    user: null,
    // 发布 / 编辑成功后退回列表时，需要刷新首页
    needRefresh: false
  },

  onLaunch() {
    this.globalData.token = wx.getStorageSync(TOKEN_KEY) || '';
    this.globalData.user = wx.getStorageSync(USER_KEY) || null;
  },

  /** 是否已登录 */
  isLogin() {
    return !!this.globalData.token;
  },

  /** 登录成功后保存登录态 */
  setLogin(data) {
    this.globalData.token = data.token;
    this.globalData.user = {
      userId: data.userId,
      username: data.username,
      nickname: data.nickname,
      role: data.role,
      avatar: data.avatar
    };
    wx.setStorageSync(TOKEN_KEY, data.token);
    wx.setStorageSync(USER_KEY, this.globalData.user);
  },

  /** 退出登录 */
  logout() {
    this.globalData.token = '';
    this.globalData.user = null;
    wx.removeStorageSync(TOKEN_KEY);
    wx.removeStorageSync(USER_KEY);
  },

  /** 需要登录的操作统一走这里 */
  requireLogin() {
    if (this.isLogin()) {
      return true;
    }
    wx.navigateTo({ url: '/pages/login/login' });
    return false;
  },

  /**
   * 设备标识：只在本机生成一次并持久化。
   * 后端未配置微信 appid/secret 时（mock 模式）用它派生稳定的 openid，
   * 保证同一台设备反复登录始终是同一个账号。
   */
  getDeviceId() {
    let deviceId = wx.getStorageSync(DEVICE_KEY);
    if (!deviceId) {
      deviceId = 'dev-' + Date.now() + '-' + Math.random().toString(36).slice(2, 10);
      wx.setStorageSync(DEVICE_KEY, deviceId);
    }
    return deviceId;
  }
});
