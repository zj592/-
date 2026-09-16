const { api, toast } = require('../../utils/request');
const { fromNow, statusClass } = require('../../utils/util');

Page({
  data: {
    status: 'ALL',
    list: [],
    page: 1,
    size: 10,
    total: 0,
    hasMore: true,
    loading: false
  },

  onShow() {
    this.load(true);
  },

  onPullDownRefresh() {
    this.load(true);
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.load(false);
    }
  },

  switchStatus(e) {
    const status = e.currentTarget.dataset.status;
    if (status === this.data.status) {
      return;
    }
    this.setData({ status });
    this.load(true);
  },

  load(reset) {
    if (this.data.loading) {
      return;
    }
    const page = reset ? 1 : this.data.page;
    this.setData({ loading: true });
    api.myItems({ status: this.data.status, page, size: this.data.size })
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

  edit(e) {
    wx.navigateTo({ url: '/pages/publish/publish?id=' + e.currentTarget.dataset.id });
  },

  remove(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '删除信息',
      content: '删除后无法恢复，确定删除吗？',
      confirmColor: '#f2544b',
      success: (res) => {
        if (!res.confirm) {
          return;
        }
        api.deleteItem(id).then(() => {
          wx.showToast({ title: '已删除', icon: 'success' });
          getApp().globalData.needRefresh = true;
          this.load(true);
        }).catch(() => {});
      }
    });
  },

  finish(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '标记完成',
      content: '确认东西已经找回或已归还？',
      success: (res) => {
        if (!res.confirm) {
          return;
        }
        api.finishItem(id).then(() => {
          wx.showToast({ title: '已完成', icon: 'success' });
          getApp().globalData.needRefresh = true;
          this.load(true);
        }).catch(() => {});
      }
    });
  }
});
