<template>
  <div class="cyber-login">
    <canvas id="matrix-canvas" class="matrix-bg"></canvas>

    <div class="scan-lines"></div>

    <div class="grid-overlay"></div>

    <div class="glow-orb glow-orb-1"></div>
    <div class="glow-orb glow-orb-2"></div>

    <div class="login-container">
      <div class="system-header">
        <div class="header-line"></div>
        <div class="system-status">
          <span class="status-dot"></span>
          <span class="status-text">SYSTEM ACCESS TERMINAL</span>
        </div>
        <div class="header-line"></div>
      </div>

      <div class="login-box">
        <div class="corner corner-tl"></div>
        <div class="corner corner-tr"></div>
        <div class="corner corner-bl"></div>
        <div class="corner corner-br"></div>

        <div class="scan-bar"></div>

        <div class="title-section">
          <h1 class="main-title">课程达成度分析系统</h1>
          <div class="subtitle">
            <span class="subtitle-text">Course Achievement Analysis System</span>
            <span class="version">v1.0</span>
          </div>
        </div>

        <el-form
          ref="loginForm"
          :model="loginForm"
          :rules="loginRules"
          class="cyber-form"
        >
          <div class="form-group">
            <label class="form-label">
              <i class="el-icon-user"></i>
              <span>USER IDENTITY</span>
            </label>
            <el-form-item prop="username">
              <el-input
                v-model="loginForm.username"
                type="text"
                auto-complete="off"
                placeholder="请输入用户名"
                class="cyber-input"
                @keyup.enter.native="handleLogin"
              >
                <span slot="prefix" class="input-prefix">&gt;</span>
              </el-input>
            </el-form-item>
          </div>

          <div class="form-group">
            <label class="form-label">
              <i class="el-icon-lock"></i>
              <span>ACCESS KEY</span>
            </label>
            <el-form-item prop="password">
              <el-input
                v-model="loginForm.password"
                type="password"
                auto-complete="off"
                placeholder="请输入密码"
                class="cyber-input"
                @keyup.enter.native="handleLogin"
              >
                <span slot="prefix" class="input-prefix">&gt;</span>
              </el-input>
            </el-form-item>
          </div>

          <div v-if="captchaEnabled" class="form-group">
            <label class="form-label">
              <i class="el-icon-picture"></i>
              <span>VERIFICATION</span>
            </label>
            <el-form-item prop="code">
              <div class="code-wrapper">
                <el-input
                  v-model="loginForm.code"
                  auto-complete="off"
                  placeholder="验证码"
                  class="cyber-input code-input"
                  @keyup.enter.native="handleLogin"
                >
                  <span slot="prefix" class="input-prefix">&gt;</span>
                </el-input>
                <div class="code-image" @click="getCode">
                  <img v-if="codeUrl" :src="codeUrl" class="code-img" alt="captcha" />
                  <div class="code-overlay">
                    <i class="el-icon-refresh"></i>
                  </div>
                </div>
              </div>
            </el-form-item>
          </div>

          <div class="form-options">
            <el-checkbox v-model="loginForm.rememberMe" class="cyber-checkbox">
              <span class="checkbox-label">记住密码</span>
            </el-checkbox>
          </div>

          <el-form-item>
            <el-button
              :loading="loading"
              type="primary"
              class="cyber-submit-btn"
              @click.native.prevent="handleLogin"
            >
              <span v-if="!loading">
                <i class="el-icon-right"></i>
                INITIALIZE ACCESS
              </span>
              <span v-else class="loading-text">
                <i class="el-icon-loading"></i>
                AUTHENTICATING...
              </span>
            </el-button>
            <div v-if="register" class="register-wrap">
              <router-link to="/register" class="register-link">立即注册</router-link>
            </div>
          </el-form-item>
        </el-form>

        <div class="login-footer">
          <div class="footer-line"></div>
          <div class="footer-info">
            <span class="info-item">
              <i class="el-icon-cpu"></i>
              系统状态: <em class="status-online">在线</em>
            </span>
            <span class="info-divider">|</span>
            <span class="info-item">
              <i class="el-icon-connection"></i>
              连接: <em class="status-secure">安全</em>
            </span>
          </div>
        </div>
      </div>

      <div class="copyright">
        <div class="copyright-line"></div>
        <p class="copyright-text">
          Copyright © 2024 课程达成度分析团队 · All Rights Reserved
        </p>
      </div>
    </div>
  </div>
</template>

<script>
import { getCodeImg } from "@/api/login";
import Cookies from "js-cookie";
import { encrypt, decrypt } from "@/utils/jsencrypt";

const MATRIX_CHARS =
  "01アイウエオカキクケコサシスセソタチツテトナニヌネノハヒフヘホマミムメモヤユヨラリルレロワヲン";

export default {
  name: "Login",
  data() {
    return {
      _matrixInterval: null,
      _matrixResize: null,
      _loginUnmounted: false,
      codeUrl: "",
      loginForm: {
        username: "admin",
        password: "admin123",
        rememberMe: false,
        code: "",
        uuid: ""
      },
      loginRules: {
        username: [
          { required: true, trigger: "blur", message: "请输入您的账号" }
        ],
        password: [
          { required: true, trigger: "blur", message: "请输入您的密码" }
        ],
        code: [{ required: true, trigger: "change", message: "请输入验证码" }]
      },
      loading: false,
      captchaEnabled: true,
      register: false,
      redirect: undefined
    };
  },
  watch: {
    $route: {
      handler: function(route) {
        this.redirect = route.query && route.query.redirect;
      },
      immediate: true
    }
  },
  created() {
    if (this.$store) {
      this.$store.commit("SET_TOKEN", "");
      this.$store.commit("SET_ROLES", []);
      this.$store.commit("SET_PERMISSIONS", []);
    }
    Cookies.remove("Admin-Token");
    this.getCookie();
    this.$nextTick(() => {
      this.getCode();
    });
  },
  mounted() {
    this.$nextTick(() => {
      this.initMatrixRain();
    });
  },
  beforeDestroy() {
    this._loginUnmounted = true;
    if (this._matrixInterval != null) {
      clearInterval(this._matrixInterval);
      this._matrixInterval = null;
    }
    if (this._matrixResize) {
      window.removeEventListener("resize", this._matrixResize);
    }
  },
  methods: {
    getCookie() {
      const username = Cookies.get("username");
      const password = Cookies.get("password");
      const rememberMe = Cookies.get("rememberMe");
      this.loginForm = {
        username: username === undefined ? this.loginForm.username : username,
        password: password === undefined ? this.loginForm.password : decrypt(password),
        rememberMe: rememberMe === undefined ? false : Boolean(rememberMe)
      };
    },
    getCode() {
      getCodeImg()
        .then(res => {
          this.captchaEnabled = true;
          if (res.img) {
            this.codeUrl = "data:image/gif;base64," + res.img;
            this.loginForm.uuid = res.uuid;
          } else {
            this.codeUrl = "";
          }
        })
        .catch(() => {
          this.captchaEnabled = true;
          this.codeUrl = "";
        });
    },
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (valid) {
          this.loading = true;
          if (this.loginForm.rememberMe) {
            Cookies.set("username", this.loginForm.username, { expires: 30 });
            Cookies.set("password", encrypt(this.loginForm.password), { expires: 30 });
            Cookies.set("rememberMe", this.loginForm.rememberMe, { expires: 30 });
          } else {
            Cookies.remove("username");
            Cookies.remove("password");
            Cookies.remove("rememberMe");
          }
          this.$store
            .dispatch("Login", this.loginForm)
            .then(() => {
              this.$router.push({ path: this.redirect || "/" }).catch(() => {});
            })
            .catch(() => {
              this.loading = false;
              if (this.captchaEnabled) {
                this.getCode();
              }
            });
        }
      });
    },
    initMatrixRain() {
      const canvas = this.$el
        ? this.$el.querySelector("#matrix-canvas")
        : document.getElementById("matrix-canvas");
      if (!canvas) return;

      const ctx = canvas.getContext("2d");
      const fontSize = 14;
      let drops = [];

      const reinitDrops = () => {
        const columns = Math.max(0, Math.floor(canvas.width / fontSize));
        drops = Array(Math.floor(columns)).fill(1);
      };

      const setSize = () => {
        canvas.width = window.innerWidth;
        canvas.height = window.innerHeight;
        reinitDrops();
      };

      setSize();
      this._matrixResize = setSize;
      window.addEventListener("resize", this._matrixResize);

      const draw = () => {
        if (this._loginUnmounted) return;
        ctx.fillStyle = "rgba(5, 5, 5, 0.05)";
        ctx.fillRect(0, 0, canvas.width, canvas.height);
        ctx.font = fontSize + "px monospace";

        for (let i = 0; i < drops.length; i++) {
          const text =
            MATRIX_CHARS[Math.floor(Math.random() * MATRIX_CHARS.length)];
          const x = i * fontSize;
          const y = drops[i] * fontSize;
          ctx.fillStyle = "rgba(102, 126, 234, " + (Math.random() * 0.5 + 0.5) + ")";
          ctx.fillText(text, x, y);
          if (y > canvas.height && Math.random() > 0.975) {
            drops[i] = 0;
          }
          drops[i]++;
        }
      };

      this._matrixInterval = setInterval(draw, 33);
    }
  }
};
</script>

<style scoped lang="scss">
$cyber-primary: #667eea;
$cyber-secondary: #764ba2;
$cyber-accent: #00f2fe;
$text-primary: #f8f5f0;
$text-secondary: #c2bcb0;

.cyber-login {
  position: relative;
  width: 100%;
  min-height: 100vh;
  background: #050505;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.matrix-bg {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
  opacity: 0.4;
  pointer-events: none;
}

.scan-lines {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  background: repeating-linear-gradient(
    0deg,
    transparent,
    transparent 2px,
    rgba(102, 126, 234, 0.03) 2px,
    rgba(102, 126, 234, 0.03) 4px
  );
  z-index: 2;
  pointer-events: none;
  animation: scanLineMove 8s linear infinite;
}

@keyframes scanLineMove {
  0% {
    transform: translateY(0);
  }
  100% {
    transform: translateY(100px);
  }
}

.grid-overlay {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  background-image: linear-gradient(
      rgba(102, 126, 234, 0.03) 1px,
      transparent 1px
    ),
    linear-gradient(90deg, rgba(102, 126, 234, 0.03) 1px, transparent 1px);
  background-size: 50px 50px;
  z-index: 2;
  pointer-events: none;
}

.glow-orb {
  position: fixed;
  width: 600px;
  height: 600px;
  border-radius: 50%;
  filter: blur(100px);
  opacity: 0.15;
  z-index: 1;
  pointer-events: none;
  animation: orbFloat 20s infinite;
}

.glow-orb-1 {
  background: radial-gradient(circle, $cyber-primary, transparent);
  top: -300px;
  right: -200px;
}

.glow-orb-2 {
  background: radial-gradient(circle, $cyber-secondary, transparent);
  bottom: -300px;
  left: -200px;
  animation-delay: -10s;
}

@keyframes orbFloat {
  0%,
  100% {
    transform: translate(0, 0) scale(1);
  }
  33% {
    transform: translate(50px, -50px) scale(1.1);
  }
  66% {
    transform: translate(-50px, 50px) scale(0.9);
  }
}

.login-container {
  position: relative;
  z-index: 10;
  width: 90%;
  max-width: 480px;
  animation: fadeInUp 0.8s ease-out;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(40px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.system-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 40px;
}

.header-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(to right, transparent, $cyber-primary, transparent);
}

.system-status {
  display: flex;
  align-items: center;
  gap: 10px;
  font-family: Monaco, "Courier New", monospace;
  font-size: 10px;
  letter-spacing: 2px;
  color: $text-secondary;
  text-transform: uppercase;
}

.status-dot {
  width: 8px;
  height: 8px;
  background: $cyber-accent;
  border-radius: 50%;
  box-shadow: 0 0 20px $cyber-accent;
  animation: statusPulse 2s infinite;
}

@keyframes statusPulse {
  0%,
  100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.6;
    transform: scale(1.2);
  }
}

.login-box {
  position: relative;
  background: rgba(12, 12, 12, 0.85);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(102, 126, 234, 0.2);
  padding: 50px 40px;
  box-shadow: 0 0 50px rgba(102, 126, 234, 0.1), inset 0 0 50px rgba(102, 126, 234, 0.02);
}

.corner {
  position: absolute;
  width: 20px;
  height: 20px;
  border-style: solid;
  border-color: $cyber-primary;
  z-index: 1;

  &.corner-tl {
    top: -1px;
    left: -1px;
    border-width: 2px 0 0 2px;
  }

  &.corner-tr {
    top: -1px;
    right: -1px;
    border-width: 2px 2px 0 0;
  }

  &.corner-bl {
    bottom: -1px;
    left: -1px;
    border-width: 0 0 2px 2px;
  }

  &.corner-br {
    bottom: -1px;
    right: -1px;
    border-width: 0 2px 2px 0;
  }
}

.scan-bar {
  position: absolute;
  top: 0;
  right: 0;
  left: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, $cyber-accent, transparent);
  animation: boxScan 3s linear infinite;
  pointer-events: none;
  z-index: 2;
}

@keyframes boxScan {
  0% {
    transform: translateY(0);
    opacity: 0;
  }
  50% {
    opacity: 1;
  }
  100% {
    transform: translateY(500px);
    opacity: 0;
  }
}

.title-section {
  text-align: center;
  margin-bottom: 40px;
}

.main-title {
  font-size: 28px;
  font-weight: 300;
  color: $text-primary;
  margin: 0 0 12px;
  letter-spacing: 2px;
  text-shadow: 0 0 20px rgba(102, 126, 234, 0.3);
}

.subtitle {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  font-family: Monaco, monospace;
  font-size: 11px;
  color: $text-secondary;
  letter-spacing: 1px;
  flex-wrap: wrap;
}

.version {
  padding: 2px 8px;
  background: rgba(102, 126, 234, 0.1);
  border: 1px solid rgba(102, 126, 234, 0.3);
  border-radius: 3px;
  color: $cyber-primary;
  font-size: 9px;
}

.cyber-form {
  .form-group {
    margin-bottom: 28px;
  }

  .form-label {
    display: flex;
    align-items: center;
    gap: 8px;
    font-family: Monaco, "Courier New", monospace;
    font-size: 11px;
    color: $text-secondary;
    margin-bottom: 10px;
    letter-spacing: 1px;
    text-transform: uppercase;

    i {
      color: $cyber-primary;
    }
  }

  ::v-deep .el-form-item {
    margin-bottom: 0;
  }

  ::v-deep .el-input__inner {
    height: 48px;
    line-height: 48px;
    background: rgba(5, 5, 5, 0.6);
    border: 1px solid rgba(102, 126, 234, 0.2);
    color: $text-primary;
    font-size: 14px;
    padding-left: 40px;
    transition: all 0.3s;
    border-radius: 4px;

    &::placeholder {
      color: rgba(255, 255, 255, 0.3);
    }

    &:focus {
      background: rgba(5, 5, 5, 0.8);
      border-color: $cyber-primary;
      box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.1), inset 0 0 20px rgba(102, 126, 234, 0.05);
    }
  }

  .input-prefix {
    color: $cyber-primary;
    font-family: Monaco, "Courier New", monospace;
    margin-right: 8px;
    animation: prefixBlink 1.5s infinite;
  }
}

@keyframes prefixBlink {
  0%,
  49% {
    opacity: 1;
  }
  50%,
  100% {
    opacity: 0;
  }
}

.code-wrapper {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.code-wrapper .code-input {
  flex: 1;
  min-width: 0;
}

.code-image {
  position: relative;
  width: 120px;
  height: 48px;
  flex-shrink: 0;
  cursor: pointer;
  overflow: hidden;
  border: 1px solid rgba(102, 126, 234, 0.2);
  border-radius: 4px;
  transition: all 0.3s;
}

.code-image:hover {
  border-color: $cyber-primary;
}

.code-image:hover .code-overlay {
  opacity: 1;
}

.code-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.code-overlay {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  background: rgba(102, 126, 234, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.code-overlay i {
  font-size: 20px;
  color: white;
}

.form-options {
  margin-bottom: 30px;
}

::v-deep .form-options .el-checkbox {
  .el-checkbox__input .el-checkbox__inner {
    background: rgba(5, 5, 5, 0.6);
    border-color: rgba(102, 126, 234, 0.3);
  }

  .el-checkbox__input .el-checkbox__inner:hover {
    border-color: $cyber-primary;
  }

  .el-checkbox__input.is-checked .el-checkbox__inner {
    background: $cyber-primary;
    border-color: $cyber-primary;
  }

  .el-checkbox__label {
    color: $text-secondary;
    font-size: 13px;
  }
}

::v-deep .cyber-submit-btn {
  width: 100%;
  height: 50px;
  background: linear-gradient(135deg, $cyber-primary, $cyber-secondary);
  border: none;
  color: white;
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 3px;
  text-transform: uppercase;
  position: relative;
  overflow: hidden;
  border-radius: 4px;
  padding: 0 16px;
  transition: all 0.3s;
}

::v-deep .cyber-submit-btn::before {
  content: "";
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(
    90deg,
    transparent,
    rgba(255, 255, 255, 0.2),
    transparent
  );
  transition: left 0.5s;
}

::v-deep .cyber-submit-btn:hover,
::v-deep .cyber-submit-btn:focus {
  transform: translateY(-2px);
  box-shadow: 0 8px 30px rgba(102, 126, 234, 0.4), 0 0 50px rgba(102, 126, 234, 0.2);
  background: linear-gradient(135deg, $cyber-primary, $cyber-secondary);
  color: #fff;
  border: none;
}

::v-deep .cyber-submit-btn:hover::before,
::v-deep .cyber-submit-btn:focus::before {
  left: 100%;
}

::v-deep .cyber-submit-btn:active {
  transform: translateY(0);
}

::v-deep .cyber-submit-btn .loading-text {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.register-wrap {
  text-align: center;
  margin-top: 12px;
}

.register-link {
  color: $cyber-primary;
  font-size: 13px;
  text-decoration: none;
  transition: color 0.2s;
}

.register-link:hover {
  color: $cyber-secondary;
  text-decoration: underline;
}

.login-footer {
  margin-top: 30px;
}

.footer-line {
  height: 1px;
  background: linear-gradient(
    to right,
    transparent,
    rgba(102, 126, 234, 0.3),
    transparent
  );
  margin-bottom: 20px;
}

.footer-info {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  flex-wrap: wrap;
  font-family: Monaco, "Courier New", monospace;
  font-size: 10px;
  color: $text-secondary;
  letter-spacing: 1px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.info-item i {
  color: $cyber-primary;
}

.info-item em {
  font-style: normal;
  color: $cyber-accent;
}

.status-secure {
  color: $cyber-accent;
}

.status-online {
  color: $cyber-accent;
}

.info-divider {
  color: rgba(255, 255, 255, 0.2);
}

.copyright {
  margin-top: 40px;
  text-align: center;
}

.copyright-line {
  height: 1px;
  background: linear-gradient(
    to right,
    transparent,
    rgba(102, 126, 234, 0.2),
    transparent
  );
  margin-bottom: 20px;
}

.copyright-text {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
  letter-spacing: 0.5px;
  margin: 0;
}

@media (max-width: 768px) {
  .login-container {
    width: 95%;
  }

  .login-box {
    padding: 40px 25px;
  }

  .main-title {
    font-size: 22px;
  }

  .code-wrapper {
    flex-direction: column;
  }

  .code-image {
    width: 100%;
  }
}
</style>
