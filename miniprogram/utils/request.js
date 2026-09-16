const { BASE_URL, TOKEN_KEY } = require('../config');

function toast(title) {
  wx.showToast({ title: title || '操作失败', icon: 'none' });
}

/**
 * 统一请求封装
 * 后端返回结构固定为 { code, msg, data }
 * code=200 时 resolve(data)，否则 reject(整个响应体)
 */
function request(options) {
  const { url, method = 'GET', data = {}, silent = false, loading = false } = options;
  return new Promise((resolve, reject) => {
    const header = { 'content-type': 'application/json' };
    const token = wx.getStorageSync(TOKEN_KEY);
    if (token) {
      header.Authorization = 'Bearer ' + token;
    }
    if (loading) {
      wx.showLoading({ title: '加载中', mask: true });
    }
    wx.request({
      url: BASE_URL + url,
      method,
      data,
      header,
      success(res) {
        const body = res.data || {};
        if (body.code === 200) {
          resolve(body.data);
          return;
        }
        if (body.code === 401) {
          const app = getApp();
          if (app) {
            app.logout();
          }
          if (!silent) {
            toast('登录已失效，请重新登录');
          }
          reject(body);
          return;
        }
        if (!silent) {
          toast(body.msg);
        }
        reject(body);
      },
      fail(err) {
        if (!silent) {
          toast('网络异常，请确认后端服务已启动');
        }
        reject(err);
      },
      complete() {
        if (loading) {
          wx.hideLoading();
        }
      }
    });
  });
}

/** 图片上传，返回图片可访问地址 */
function uploadImage(filePath) {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync(TOKEN_KEY);
    wx.uploadFile({
      url: BASE_URL + '/api/files/upload',
      filePath,
      name: 'file',
      header: token ? { Authorization: 'Bearer ' + token } : {},
      success(res) {
        let body = {};
        try {
          body = JSON.parse(res.data);
        } catch (e) {
          toast('上传失败');
          reject(e);
          return;
        }
        if (body.code === 200) {
          resolve(body.data);
        } else {
          toast(body.msg);
          reject(body);
        }
      },
      fail(err) {
        toast('图片上传失败');
        reject(err);
      }
    });
  });
}

const api = {
  // 公共
  meta: () => request({ url: '/api/items/meta' }),
  itemList: (data) => request({ url: '/api/items', data }),
  itemDetail: (id) => request({ url: '/api/items/' + id }),

  // 认证与个人
  login: (data) => request({ url: '/api/auth/login', method: 'POST', data }),
  register: (data) => request({ url: '/api/auth/register', method: 'POST', data }),
  wxLogin: (data) => request({ url: '/api/auth/wx-login', method: 'POST', data, loading: true }),
  profile: () => request({ url: '/api/user/profile' }),
  updateProfile: (data) => request({ url: '/api/user/profile', method: 'PUT', data }),

  // 信息
  publish: (data) => request({ url: '/api/items', method: 'POST', data, loading: true }),
  updateItem: (id, data) => request({ url: '/api/items/' + id, method: 'PUT', data, loading: true }),
  deleteItem: (id) => request({ url: '/api/items/' + id, method: 'DELETE' }),
  finishItem: (id) => request({ url: '/api/items/' + id + '/finish', method: 'POST' }),

  // 个人中心
  myItems: (data) => request({ url: '/api/my/items', data }),
  myStats: () => request({ url: '/api/my/stats' }),

  // 认领申请
  submitClaim: (data) => request({ url: '/api/claims', method: 'POST', data, loading: true }),
  myClaims: (data) => request({ url: '/api/claims/mine', data }),
  receivedClaims: (data) => request({ url: '/api/claims/received', data }),
  cancelClaim: (id) => request({ url: '/api/claims/' + id + '/cancel', method: 'POST' }),
  handleClaim: (id, data) => request({ url: '/api/claims/' + id + '/handle', method: 'POST', data }),

  // 消息通知
  notices: (data) => request({ url: '/api/notifications', data }),
  unreadCount: () => request({ url: '/api/notifications/unread-count', silent: true }),
  readNotice: (id) => request({ url: '/api/notifications/' + id + '/read', method: 'POST' }),
  readAllNotices: () => request({ url: '/api/notifications/read-all', method: 'POST' }),

  // 后台
  adminItems: (data) => request({ url: '/api/admin/items', data }),
  adminAudit: (id, data) => request({ url: '/api/admin/items/' + id + '/audit', method: 'POST', data }),
  adminStats: () => request({ url: '/api/admin/stats' }),
  adminClaims: (data) => request({ url: '/api/admin/claims', data })
};

module.exports = {
  request,
  uploadImage,
  api,
  toast
};
