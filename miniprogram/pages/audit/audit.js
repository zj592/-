const { api, toast } = require('../../utils/request');
const { fromNow, statusClass } = require('../../utils/util');

Page({
  data: {
    status: 'PENDING',
    stats: {
      totalItems: 0,
      pendingItems: 0,
      approvedItems: 0,
      finishedItems: 0,
      totalUsers: 0,
      totalClaims: 0,
      pendingClaims: 0,
      todayItems: 0
    },
    list: [],
    page: 1,
    size: 10,
    total: 0,
    hasMore: true,
    loading: false
  },

  onShow() {
    this.loadStats();
    this.load(true);
  },

  onPullDownRefresh() {
    this.loadStats();
    this.load(true);
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.load(false);
    }
  },

  loadStats() {
    api.adminStats().then((stats) => this.setData({ stats })).catch(() => {});
  },

  switchStatus(e) {
    const status = e.currentTarget.dataset.status;
    if (status === this.data.status) {
      return;
    }
    this.setData({ status, list: [] });
    this.load(true);
  },

  load(reset) {
    if (this.data.loading) {
      return;
    }
    const page = reset ? 1 : this.data.page;
    this.setData({ loading: true });
    api.adminItems({ status: this.data.status, page, size: this.data.size })
      .then((data) => {
        const records = (data.records || []).map((item) => ({
          ...item,
          timeText: fromNow(item.createTime),
          statusCls: statusClass(item.status)
        }));
        const list = reset ? records : this.data.list.concat(records);
        this.setData({
          list,
          page: page + 1,
          total: data.total || 0,
          hasMore: list.length < (data.total || 0)
        });
      })
      .catch(() => {})
      .then(() => {
        this.setData({ loading: false });
        wx.stopPullDownRefresh();
      });
  },

  goDetail(e) {
    wx.navigateTo({ url: '/pages/detail/detail?id=' + e.currentTarget.dataset.id });
  },

  approve(e) {
    const id = e.currentTarget.dataset.id;
    api.adminAudit(id, { approved: true }).then(() => {
      wx.showToast({ title: '已通过', icon: 'success' });
      getApp().globalData.needRefresh = true;
      this.loadStats();
      this.load(true);
    }).catch(() => {});
  },

  reject(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '驳回信息',
      editable: true,
      placeholderText: '请填写驳回原因（必填）',
      success: (res) => {
        if (!res.confirm) {
          return;
        }
        const reason = (res.content || '').trim();
        if (!reason) {
          toast('驳回必须填写原因');
          return;
        }
        api.adminAudit(id, { approved: false, reason }).then(() => {
          wx.showToast({ title: '已驳回', icon: 'success' });
          this.loadStats();
          this.load(true);
        }).catch(() => {});
      }
    });
  }
});
