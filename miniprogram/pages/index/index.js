const { api } = require('../../utils/request');
const { fromNow, formatTime } = require('../../utils/util');

Page({
  data: {
    keyword: '',
    type: '',
    category: '',
    categories: [],
    list: [],
    page: 1,
    size: 10,
    total: 0,
    hasMore: true,
    loading: false
  },

  onLoad() {
    this.loadMeta();
    this.loadList(true);
  },

  onShow() {
    const app = getApp();
    if (app.globalData.needRefresh) {
      app.globalData.needRefresh = false;
      this.loadList(true);
    }
  },

  onPullDownRefresh() {
    this.loadList(true);
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadList(false);
    }
  },

  loadMeta() {
    api.meta()
      .then((data) => this.setData({ categories: data.categories || [] }))
      .catch(() => {});
  },

  loadList(reset) {
    if (this.data.loading) {
      return;
    }
    const page = reset ? 1 : this.data.page;
    this.setData({ loading: true });
    api.itemList({
      keyword: this.data.keyword,
      type: this.data.type,
      category: this.data.category,
      page,
      size: this.data.size
    })
      .then((data) => {
        const records = (data.records || []).map((item) => ({
          ...item,
          timeText: fromNow(item.createTime),
          lostTimeText: formatTime(item.lostTime, true)
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

  onKeywordInput(e) {
    this.setData({ keyword: e.detail.value });
  },

  onSearch() {
    this.loadList(true);
  },

  clearKeyword() {
    this.setData({ keyword: '' });
    this.loadList(true);
  },

  switchType(e) {
    const type = e.currentTarget.dataset.type || '';
    if (type === this.data.type) {
      return;
    }
    this.setData({ type });
    this.loadList(true);
  },

  selectCategory(e) {
    const category = e.currentTarget.dataset.category || '';
    this.setData({ category: category === this.data.category ? '' : category });
    this.loadList(true);
  },

  goDetail(e) {
    wx.navigateTo({ url: '/pages/detail/detail?id=' + e.currentTarget.dataset.id });
  },

  goPublish() {
    wx.switchTab({ url: '/pages/publish/publish' });
  }
});
