/**
 * 通用工具方法
 */

function pad(n) {
  return n < 10 ? '0' + n : '' + n;
}

/** 把后端返回的 "2026-09-15 08:30:00" 或时间戳转成展示文案 */
function formatTime(value, withTime) {
  if (!value) {
    return '';
  }
  let date;
  if (typeof value === 'string') {
    date = new Date(value.replace(/-/g, '/'));
  } else {
    date = new Date(value);
  }
  if (isNaN(date.getTime())) {
    return String(value);
  }
  const y = date.getFullYear();
  const m = pad(date.getMonth() + 1);
  const d = pad(date.getDate());
  if (!withTime) {
    return y + '-' + m + '-' + d;
  }
  return y + '-' + m + '-' + d + ' ' + pad(date.getHours()) + ':' + pad(date.getMinutes());
}

/** 相对时间：刚刚 / 3 小时前 / 2 天前 */
function fromNow(value) {
  if (!value) {
    return '';
  }
  const date = new Date(String(value).replace(/-/g, '/'));
  if (isNaN(date.getTime())) {
    return '';
  }
  const diff = Date.now() - date.getTime();
  const minute = 60 * 1000;
  const hour = 60 * minute;
  const day = 24 * hour;
  if (diff < minute) {
    return '刚刚';
  }
  if (diff < hour) {
    return Math.floor(diff / minute) + ' 分钟前';
  }
  if (diff < day) {
    return Math.floor(diff / hour) + ' 小时前';
  }
  if (diff < 30 * day) {
    return Math.floor(diff / day) + ' 天前';
  }
  return formatTime(value);
}

/** 状态对应的样式后缀，配合 app.wxss 里的 .tag-xx 使用 */
function statusClass(status) {
  const map = {
    PENDING: 'warning',
    APPROVED: 'success',
    FINISHED: 'info',
    REJECTED: 'danger',
    CANCELED: 'info'
  };
  return map[status] || 'info';
}

/** 当前时间字符串，用于发布表单默认值 */
function nowText() {
  const d = new Date();
  return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate())
    + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes()) + ':00';
}

/** 今天日期，用于日期选择器默认值 */
function today() {
  const d = new Date();
  return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate());
}

module.exports = {
  formatTime,
  fromNow,
  statusClass,
  nowText,
  today
};
