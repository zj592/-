const { api, toast } = require('../../utils/request');
const { fromNow, statusClass } = require('../../utils/util');

Page({
  data: {
    tab: 'mine',
    list: [],
    page: 1,
    size: 10,
    total: 0,
    hasMore: true,
    loading: false
  },

  onLoad(options) {
    if (options && options.tab) {
      this.setData({ tab: options.tab });
    }
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

  switchTab(e) {
    const tab = e.currentTarget.dataset.tab;
    if (tab === this.data.tab) {
      return;
    }
    this.setData({ tab, list: [] });
    this.load(true);
  },

  load(reset) {
    if (this.data.loading) {
      return;
    }
    const page = reset ? 1 : this.data.page;
    this.setData({ loading: true });

    const params = { page, size: this.data.size };
    const task = this.data.tab === 'mine' ? api.myClaims(params) : api.receivedClaims(params);

    task
      .then((data) => {
        const records = (data.records || []).map((claim) => ({
          ...claim,
          timeText: fromNow(claim.createTime),
          statusCls: statusClass(claim.status)
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
    wx.navigateTo({ url: '/pages/detail/detail?id=' + e.currentTarget.dataset.itemId });
  },

  cancel(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '撤销申请',
      content: '确定撤销这条认领申请吗？',
      success: (res) => {
        if (!res.confirm) {
          return;
        }
        api.cancelClaim(id).then(() => {
          wx.showToast({ title: '已撤销', icon: 'success' });
          this.load(true);
        }).catch(() => {});
      }
    });
  },

  approve(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '同意认领',
      content: '同意后该信息会标记为「已完成」，其他待处理申请将自动驳回。',
      success: (res) => {
        if (!res.confirm) {
          return;
        }
        api.handleClaim(id, { approved: true, remark: '已确认，请联系我取回' }).then(() => {
          wx.showToast({ title: '已同意', icon: 'success' });
          getApp().globalData.needRefresh = true;
          this.load(true);
        }).catch(() => {});
      }
    });
  },

  reject(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '驳回申请',
      editable: true,
      placeholderText: '填写驳回原因，会展示给申请人',
      success: (res) => {
        if (!res.confirm) {
          return;
        }
        const remark = (res.content || '').trim() || '信息不符，暂不通过';
        api.handleClaim(id, { approved: false, remark }).then(() => {
          wx.showToast({ title: '已驳回', icon: 'success' });
          this.load(true);
        }).catch(() => {});
      }
    });
  },

  contact(e) {
    const contact = e.currentTarget.dataset.contact;
    if (!contact) {
      toast('对方未填写联系方式');
      return;
    }
    wx.setClipboardData({
      data: contact,
      success: () => toast('联系方式已复制')
    });
  }
});
