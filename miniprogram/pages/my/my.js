const { api, toast } = require('../../utils/request');

Page({
  data: {
    isLogin: false,
    isAdmin: false,
    user: null,
    unread: 0,
    stats: {
      published: 0,
      pending: 0,
      finished: 0,
      claims: 0,
      pendingReceived: 0
    }
  },

  onShow() {
    const app = getApp();
    const isLogin = app.isLogin();
    const user = app.globalData.user;
    this.setData({
      isLogin,
      user,
      isAdmin: !!(user && user.role === 'ADMIN')
    });
    if (isLogin) {
      this.loadProfile();
      this.loadStats();
      this.loadUnread();
    } else {
      this.setData({
        unread: 0,
        stats: { published: 0, pending: 0, finished: 0, claims: 0, pendingReceived: 0 }
      });
    }
  },

  loadUnread() {
    api.unreadCount()
      .then((data) => this.setData({ unread: data.count || 0 }))
      .catch(() => {});
  },

  loadProfile() {
    api.profile().then((user) => {
      const app = getApp();
      app.globalData.user = {
        userId: user.id,
        username: user.username,
        nickname: user.nickname,
        role: user.role,
        avatar: user.avatar
      };
      this.setData({ user: app.globalData.user });
    }).catch(() => {});
  },

  loadStats() {
    api.myStats().then((stats) => this.setData({ stats })).catch(() => {});
  },

  goLogin() {
    wx.navigateTo({ url: '/pages/login/login' });
  },

  goMyItems() {
    if (!getApp().requireLogin()) {
      return;
    }
    wx.navigateTo({ url: '/pages/my-items/my-items' });
  },

  goMyClaims() {
    if (!getApp().requireLogin()) {
      return;
    }
    wx.navigateTo({ url: '/pages/my-claims/my-claims?tab=mine' });
  },

  goReceived() {
    if (!getApp().requireLogin()) {
      return;
    }
    wx.navigateTo({ url: '/pages/my-claims/my-claims?tab=received' });
  },

  goNotices() {
    if (!getApp().requireLogin()) {
      return;
    }
    wx.navigateTo({ url: '/pages/notices/notices' });
  },

  goAudit() {
    wx.navigateTo({ url: '/pages/audit/audit' });
  },

  editNickname() {
    const current = this.data.user ? this.data.user.nickname : '';
    wx.showModal({
      title: '修改昵称',
      editable: true,
      placeholderText: '请输入新的昵称',
      content: current,
      success: (res) => {
        if (!res.confirm) {
          return;
        }
        const nickname = (res.content || '').trim();
        if (!nickname) {
          toast('昵称不能为空');
          return;
        }
        api.updateProfile({ nickname }).then(() => {
          wx.showToast({ title: '已保存', icon: 'success' });
          this.loadProfile();
        }).catch(() => {});
      }
    });
  },

  logout() {
    wx.showModal({
      title: '退出登录',
      content: '确定要退出当前账号吗？',
      success: (res) => {
        if (!res.confirm) {
          return;
        }
        getApp().logout();
        this.setData({
          isLogin: false,
          isAdmin: false,
          user: null,
          unread: 0,
          stats: { published: 0, pending: 0, finished: 0, claims: 0, pendingReceived: 0 }
        });
        wx.showToast({ title: '已退出', icon: 'success' });
      }
    });
  }
});
