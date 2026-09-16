const { api, toast } = require('../../utils/request');
const { SUBSCRIBE_TEMPLATE_IDS } = require('../../config');

Page({
  data: {
    mode: 'login',
    submitting: false,
    form: {
      username: '',
      password: '',
      nickname: '',
      phone: '',
      campus: ''
    }
  },

  switchMode(e) {
    this.setData({ mode: e.currentTarget.dataset.mode });
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({ ['form.' + field]: e.detail.value });
  },

  submit() {
    if (this.data.submitting) {
      return;
    }
    const form = this.data.form;
    const username = form.username.trim();
    const password = form.password;

    if (username.length < 4) {
      toast('账号至少 4 位');
      return;
    }
    if (password.length < 6) {
      toast('密码至少 6 位');
      return;
    }

    const isRegister = this.data.mode === 'register';
    if (isRegister && !form.nickname.trim()) {
      toast('请填写昵称');
      return;
    }

    const payload = isRegister
      ? {
        username,
        password,
        nickname: form.nickname.trim(),
        phone: form.phone.trim(),
        campus: form.campus.trim()
      }
      : { username, password };

    this.setData({ submitting: true });
    const task = isRegister ? api.register(payload) : api.login(payload);
    task
      .then((data) => {
        getApp().setLogin(data);
        wx.showToast({ title: isRegister ? '注册成功' : '登录成功', icon: 'success' });
        setTimeout(() => this.back(), 700);
      })
      .catch(() => {})
      .then(() => this.setData({ submitting: false }));
  },

  fillDemo() {
    this.setData({
      mode: 'login',
      form: { ...this.data.form, username: 'student', password: '123456' }
    });
  },

  /** 微信一键登录：wx.login 拿 code 换 openid，首次登录后端自动建号 */
  wxLogin() {
    if (this.data.submitting) {
      return;
    }
    wx.login({
      success: (res) => {
        if (!res.code) {
          toast('获取微信登录凭证失败，请重试');
          return;
        }
        this.setData({ submitting: true });
        api.wxLogin({ code: res.code, deviceId: getApp().getDeviceId() })
          .then((data) => {
            getApp().setLogin(data);
            wx.showToast({ title: '登录成功', icon: 'success' });
            this.requestSubscribe();
            setTimeout(() => this.back(), 700);
          })
          .catch(() => {})
          .then(() => this.setData({ submitting: false }));
      },
      fail: () => toast('微信登录失败，请检查网络')
    });
  },

  /**
   * 询问订阅授权：同意后后端才能在审核/认领结果时推送服务通知。
   * 模板 id 未配置时跳过，不打扰用户。
   */
  requestSubscribe() {
    if (!SUBSCRIBE_TEMPLATE_IDS.length || !wx.requestSubscribeMessage) {
      return;
    }
    wx.requestSubscribeMessage({
      tmplIds: SUBSCRIBE_TEMPLATE_IDS,
      success: (res) => {
        const accepted = SUBSCRIBE_TEMPLATE_IDS.filter((id) => res[id] === 'accept');
        console.log('订阅授权结果:', accepted.length, '/', SUBSCRIBE_TEMPLATE_IDS.length);
      },
      fail: (err) => console.log('订阅授权失败:', err)
    });
  },

  back() {
    const pages = getCurrentPages();
    if (pages.length > 1) {
      wx.navigateBack();
    } else {
      wx.switchTab({ url: '/pages/index/index' });
    }
  }
});
