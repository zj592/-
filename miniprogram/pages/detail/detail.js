const { api, toast } = require('../../utils/request');
const { formatTime, fromNow } = require('../../utils/util');

Page({
  data: {
    id: null,
    item: null,
    loading: true,
    isLogin: false,
    showClaim: false,
    claimForm: {
      description: '',
      contact: ''
    }
  },

  onLoad(options) {
    this.setData({ id: options.id });
  },

  onShow() {
    const isLogin = getApp().isLogin();
    // 登录状态变化会影响联系方式是否打码，所以每次进页面都重新拉一次
    this.setData({ isLogin });
    this.load();
  },

  load() {
    api.itemDetail(this.data.id)
      .then((item) => {
        this.setData({
          item: {
            ...item,
            createTimeText: fromNow(item.createTime),
            lostTimeText: formatTime(item.lostTime, true)
          },
          loading: false
        });
      })
      .catch(() => {
        this.setData({ item: null, loading: false });
      });
  },

  previewImage(e) {
    const index = e.currentTarget.dataset.index;
    wx.previewImage({
      current: this.data.item.images[index],
      urls: this.data.item.images
    });
  },

  // ---------------- 认领申请 ----------------

  openClaim() {
    if (!getApp().requireLogin()) {
      return;
    }
    if (this.data.isLogin !== true) {
      this.setData({ isLogin: true });
      this.load();
    }
    this.setData({ showClaim: true });
  },

  closeClaim() {
    this.setData({ showClaim: false });
  },

  noop() {
    // 阻止弹窗外层关闭时的冒泡
  },

  onClaimInput(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({ ['claimForm.' + field]: e.detail.value });
  },

  submitClaim() {
    const description = this.data.claimForm.description.trim();
    if (description.length < 5) {
      toast('请描述物品特征，至少 5 个字');
      return;
    }
    api.submitClaim({
      itemId: this.data.id,
      description,
      contact: this.data.claimForm.contact.trim()
    })
      .then(() => {
        this.setData({ showClaim: false, claimForm: { description: '', contact: '' } });
        wx.showToast({ title: '已提交申请', icon: 'success' });
        this.load();
      })
      .catch(() => {});
  },

  // ---------------- 发布者操作 ----------------

  markFinish() {
    wx.showModal({
      title: '确认完成',
      content: '确认东西已经找回来 / 归还给失主了吗？',
      success: (res) => {
        if (!res.confirm) {
          return;
        }
        api.finishItem(this.data.id).then(() => {
          wx.showToast({ title: '已标记完成', icon: 'success' });
          getApp().globalData.needRefresh = true;
          this.load();
        }).catch(() => {});
      }
    });
  },

  edit() {
    wx.navigateTo({ url: '/pages/publish/publish?id=' + this.data.id });
  },

  remove() {
    wx.showModal({
      title: '删除信息',
      content: '删除后无法恢复，确定要删除吗？',
      confirmColor: '#f2544b',
      success: (res) => {
        if (!res.confirm) {
          return;
        }
        api.deleteItem(this.data.id).then(() => {
          wx.showToast({ title: '已删除', icon: 'success' });
          getApp().globalData.needRefresh = true;
          setTimeout(() => wx.navigateBack(), 600);
        }).catch(() => {});
      }
    });
  },

  goReceivedClaims() {
    wx.navigateTo({ url: '/pages/my-claims/my-claims?tab=received' });
  }
});
