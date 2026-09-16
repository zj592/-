const { api, toast } = require('../../utils/request');
const { fromNow } = require('../../utils/util');

const TYPE_TEXT = {
  CLAIM_RECEIVED: '认领申请',
  CLAIM_RESULT: '认领结果',
  ITEM_AUDIT: '审核结果'
};

const TYPE_CLASS = {
  CLAIM_RECEIVED: 'tag-warning',
  CLAIM_RESULT: 'tag-success',
  ITEM_AUDIT: 'tag-primary'
};

Page({
  data: {
    list: [],
    page: 1,
    size: 10,
    total: 0,
    hasMore: true,
    loading: false,
    unread: 0
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

  load(reset) {
    if (this.data.loading) {
      return;
    }
    const page = reset ? 1 : this.data.page;
    this.setData({ loading: true });
    api.notices({ page, size: this.data.size })
      .then((data) => {
        const records = (data.records || []).map((n) => ({
          ...n,
          timeText: fromNow(n.createTime),
          typeText: TYPE_TEXT[n.type] || '通知',
          typeClass: TYPE_CLASS[n.type] || 'tag-info'
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
        this.loadUnread();
      });
  },

  loadUnread() {
    api.unreadCount()
      .then((data) => this.setData({ unread: data.count || 0 }))
      .catch(() => {});
  },

  open(e) {
    const notice = e.currentTarget.dataset.item;
    if (!notice.isRead) {
      api.readNotice(notice.id).catch(() => {});
    }
    // 根据通知类型跳到对应页面，用户不用自己找入口
    if (notice.type === 'CLAIM_RESULT') {
      wx.navigateTo({ url: '/pages/my-claims/my-claims?tab=mine' });
    } else if (notice.type === 'CLAIM_RECEIVED') {
      wx.navigateTo({ url: '/pages/my-claims/my-claims?tab=received' });
    } else if (notice.relatedId) {
      wx.navigateTo({ url: '/pages/detail/detail?id=' + notice.relatedId });
    }
    setTimeout(() => this.load(true), 400);
  },

  readAll() {
    if (!this.data.unread) {
      toast('没有未读消息');
      return;
    }
    api.readAllNotices().then(() => {
      wx.showToast({ title: '已全部标记已读', icon: 'success' });
      this.load(true);
    }).catch(() => {});
  }
});
