const { api, uploadImage, toast } = require('../../utils/request');
const { today } = require('../../utils/util');

const MAX_IMAGES = 3;

Page({
  data: {
    editId: null,
    submitting: false,
    images: [],
    maxImages: MAX_IMAGES,
    categories: [],
    form: {
      title: '',
      type: 'LOST',
      category: 'card',
      description: '',
      place: '',
      contact: '',
      date: '',
      time: '12:00'
    }
  },

  onLoad(options) {
    this.setData({
      'form.date': today(),
      'form.time': '12:00'
    });
    this.loadMeta();
    if (options && options.id) {
      this.setData({ editId: options.id });
      wx.setNavigationBarTitle({ title: '编辑信息' });
      this.loadDetail(options.id);
    }
  },

  loadMeta() {
    api.meta()
      .then((data) => this.setData({ categories: data.categories || [] }))
      .catch(() => {});
  },

  loadDetail(id) {
    api.itemDetail(id).then((item) => {
      const dateTime = (item.lostTime || '').split(' ');
      this.setData({
        form: {
          title: item.title,
          type: item.type,
          category: item.category,
          description: item.description,
          place: item.place || '',
          contact: item.contact || '',
          date: dateTime[0] || today(),
          time: (dateTime[1] || '12:00').substring(0, 5)
        },
        images: (item.images || []).map((url) => ({ url }))
      });
    }).catch(() => {});
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({ ['form.' + field]: e.detail.value });
  },

  switchType(e) {
    this.setData({ 'form.type': e.currentTarget.dataset.type });
  },

  selectCategory(e) {
    this.setData({ 'form.category': e.currentTarget.dataset.category });
  },

  onDateChange(e) {
    this.setData({ 'form.date': e.detail.value });
  },

  onTimeChange(e) {
    this.setData({ 'form.time': e.detail.value });
  },

  chooseImage() {
    const remain = MAX_IMAGES - this.data.images.length;
    if (remain <= 0) {
      toast('最多上传 ' + MAX_IMAGES + ' 张图片');
      return;
    }
    wx.chooseMedia({
      count: remain,
      mediaType: ['image'],
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const files = res.tempFiles.map((f) => f.tempFilePath);
        wx.showLoading({ title: '上传中', mask: true });
        Promise.all(files.map((path) => uploadImage(path)))
          .then((urls) => {
            const images = this.data.images.concat(urls.map((url) => ({ url })));
            this.setData({ images });
          })
          .catch(() => {})
          .then(() => wx.hideLoading());
      }
    });
  },

  previewImage(e) {
    const index = e.currentTarget.dataset.index;
    wx.previewImage({
      current: this.data.images[index].url,
      urls: this.data.images.map((i) => i.url)
    });
  },

  removeImage(e) {
    const index = e.currentTarget.dataset.index;
    const images = this.data.images.slice();
    images.splice(index, 1);
    this.setData({ images });
  },

  submit() {
    const app = getApp();
    if (!app.requireLogin()) {
      return;
    }
    const form = this.data.form;
    if (!form.title.trim()) {
      toast('请填写标题');
      return;
    }
    if (form.title.trim().length > 50) {
      toast('标题最长 50 个字');
      return;
    }
    if (form.description.trim().length < 5) {
      toast('详细描述至少 5 个字');
      return;
    }
    if (!form.contact.trim()) {
      toast('请填写联系方式');
      return;
    }

    const payload = {
      title: form.title.trim(),
      type: form.type,
      category: form.category,
      description: form.description.trim(),
      place: form.place.trim(),
      contact: form.contact.trim(),
      images: this.data.images.map((i) => i.url),
      lostTime: form.date + ' ' + form.time + ':00'
    };

    const task = this.data.editId
      ? api.updateItem(this.data.editId, payload)
      : api.publish(payload);

    task.then(() => {
      app.globalData.needRefresh = true;
      wx.showToast({ title: '提交成功', icon: 'success' });
      setTimeout(() => {
        if (this.data.editId) {
          wx.navigateBack();
        } else {
          this.resetForm();
          wx.switchTab({ url: '/pages/index/index' });
        }
      }, 800);
    }).catch(() => {});
  },

  resetForm() {
    this.setData({
      images: [],
      form: {
        title: '',
        type: 'LOST',
        category: 'card',
        description: '',
        place: '',
        contact: '',
        date: today(),
        time: '12:00'
      }
    });
  }
});
