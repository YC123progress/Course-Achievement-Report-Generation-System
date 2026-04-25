<template>
  <div class="iframe-container">
    <iframe
      :src="url"
      frameborder="0"
      width="100%"
      height="100%"
      @load="onIframeLoad"
    ></iframe>
  </div>
</template>

<script>
export default {
  name: "IframePage",
  data() {
    return {
      url: ""
    };
  },
  created() {
    // 从路由参数中获取外链地址
    this.url = this.$route.query.url || "";
  },
  methods: {
    onIframeLoad() {
      console.log("Iframe加载完成");
    }
  }
};
</script>

<style scoped>
.iframe-container {
  width: 100%;
  height: calc(100vh - 84px); /* 根据若依布局调整高度 */
}
</style>
