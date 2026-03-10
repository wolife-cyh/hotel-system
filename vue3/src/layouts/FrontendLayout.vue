<template>
    <div class="frontend-layout">
      <!-- 顶部导航栏 -->
      <div class="header-wrapper">
        <el-header class="header">
          <div class="container">
        <div class="logo">
              <router-link to="/">
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64" width="64" height="64" aria-label="Hotel icon" role="img">
                  <!-- 背景泪点可省略，如需透明背景请移除下面的矩形 -->
                  <!-- <rect width="64" height="64" fill="none" /> -->
                  <!-- 房屋主体 -->
                  <path d="M8 28V52a2 2 0 0 0 2 2h44a2 2 0 0 0 2-2V28h-6v20H12V28h-4z" fill="currentColor"/>
                  <!-- 屋顶 -->
                  <path d="M6 28l10-11h16l10 11H6z" fill="#333"/>
                  <!-- 门 -->
                  <rect x="28" y="38" width="8" height="14" rx="2" ry="2" fill="#fff" stroke="#333" stroke-width="2"/>
                  <!-- 窗户（左） -->
                  <rect x="14" y="34" width="4" height="6" fill="#fff" stroke="#333" stroke-width="2"/>
                  <rect x="24" y="34" width="4" height="6" fill="#fff" stroke="#333" stroke-width="2"/>
                  <!-- 窗户（右） -->
                  <rect x="40" y="34" width="4" height="6" fill="#fff" stroke="#333" stroke-width="2"/>
                  <!-- 可选：圆点灯具或装饰 -->
                  <circle cx="52" cy="24" r="3" fill="#FFD700" opacity="0.9"/>
                </svg>
                <span>小型宾馆预约系统</span>
              </router-link>
            </div>
            
            <div class="nav-wrapper">
              <div class="nav-links">
                <router-link to="/" :class="{ active: $route.path === '/' }">首页</router-link>
                <router-link to="/rooms" :class="{ active: $route.path.includes('/rooms') }">客房展示</router-link>
                <router-link v-if="isLoggedIn" to="/profile" :class="{ active: $route.path.includes('/profile') }">个人中心</router-link>
              </div>
              <div class="auth-buttons">
                <el-dropdown v-if="isLoggedIn" trigger="click">
                  <div class="user-avatar-container">
                    <div class="user-avatar">
                      <el-avatar 
                        :size="40" 
                        :src="userAvatarUrl" 
                        @error="avatarError">
                        {{ userInitials }}
                      </el-avatar>
                    </div>
                    <div class="user-info">
                      <span class="username">{{ userStore.userInfo?.name || userStore.userInfo?.username }}</span>
                      <el-icon><ArrowDown /></el-icon>
                    </div>
        </div>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item @click="$router.push('/profile')">
                        <el-icon><User /></el-icon>
                        <span>个人中心</span>
                      </el-dropdown-item>
                      <el-dropdown-item @click="handleLogout">
                        <el-icon><SwitchButton /></el-icon>
                        <span>退出登录</span>
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
                
                <el-button v-else type="primary" @click="$router.push('/login')" size="large">
                  <el-icon><User /></el-icon>
                  <span>登录 / 注册</span>
                </el-button>
              </div>
            </div>
          </div>
        </el-header>
      </div>
  
      <!-- 主要内容区域 -->
      <el-main class="main-content">
        <router-view />
      </el-main>
  
      <!-- 页脚 -->
      <el-footer class="footer">
        <div class="footer-content container">
          <div class="footer-info">
            <h3>联系我们</h3>
            <p><el-icon><Location /></el-icon> SDUSOFTWARE</p>
            <p><el-icon><Phone /></el-icon> XXX-XXXX-XXXX</p>
            <p><el-icon><Message /></el-icon> SDU@XX.COM</p>
          </div>
          <div class="footer-links">
            <h3></h3>
            <ul>
              <li><router-link to="/">首页</router-link></li>
              <li><router-link to="/rooms">客房展示</router-link></li>
              <li><router-link to="/login">登录</router-link></li>
            </ul>
          </div>
        </div>
        <div class="copyright">
          <p>&copy; 小型宾馆预约系统</p>
        </div>
      </el-footer>
    </div>
  </template>
  
  <script setup>
  import { computed } from 'vue'
  import { useUserStore } from '@/store/user'
  import { useRouter } from 'vue-router'
  import { Location, Phone, Message, User, SwitchButton, ArrowDown } from '@element-plus/icons-vue'
  import { ElMessage } from 'element-plus'
  
  const userStore = useUserStore()
  const router = useRouter()
  
  const isLoggedIn = computed(() => !!userStore.token)
  
  const baseAPI = process.env.VUE_APP_BASE_API || '/api'
  const userAvatarUrl = computed(() => {
    if (!userStore.userInfo?.avatar) return '';
    if (userStore.userInfo.avatar.startsWith('http')) {
      return userStore.userInfo.avatar;
    }
    return baseAPI + userStore.userInfo.avatar;
  })
  
  const userInitials = computed(() => {
    const name = userStore.userInfo?.name || userStore.userInfo?.username || '';
    if (!name) return '';
    return name.substring(0, 1).toUpperCase();
  })
  
  const avatarError = () => {
    console.log('Avatar loading failed');
  }
  
  const handleLogout = () => {
    userStore.clearUserInfo()
    ElMessage.success('退出登录成功')
    router.push('/login')
  }
  </script>
  
  <style lang="scss" scoped>
  .frontend-layout {
    display: flex;
    flex-direction: column;
    min-height: 100vh;
    background-color: #f9f9f9;
  }
  
  .container {
    max-width: 1200px;
    margin: 0 auto;
    width: 100%;
    padding: 0 20px;
  }
  
  .header-wrapper {
    position: sticky;
    top: 0;
    z-index: 100;
    background-color: #91eada;
    box-shadow: 0 1px 15px rgba(0, 0, 0, 0.04);
  }
  
  .header {
    padding: 0;
    height: auto;
    
    .container {
    display: flex;
    justify-content: space-between;
    align-items: center;
      height: 80px;
    }
    
    .logo {
      a {
        display: flex;
        align-items: center;
        text-decoration: none;
        color: #344955;
        transition: transform 0.3s ease;
        
        &:hover {
          transform: translateY(-2px);
        }
        
        svg {
          margin-right: 12px;
          color: #4a6572;
        }
        
        span {
          font-size: 1.3rem;
          font-weight: 500;
          letter-spacing: 0.5px;
        }
      }
    }
    
    .nav-wrapper {
      display: flex;
      align-items: center;
      gap: 30px;
    }
    
    .nav-links {
      display: flex;
      gap: 30px;
      
      a {
        text-decoration: none;
        color: #4a6572;
        font-size: 16px;
        font-weight: 500;
        position: relative;
        padding: 8px 0;
        transition: color 0.3s ease;
        
        &::after {
          content: '';
          position: absolute;
          bottom: 0;
          left: 0;
          width: 0;
          height: 2px;
          background-color: #4a6572;
          transition: width 0.3s ease;
        }
        
        &:hover, &.active {
          color: #344955;
          
          &::after {
          width: 100%;
        }
        }
      }
      }
      
    .auth-buttons {
      .el-button {
        display: flex;
        align-items: center;
        gap: 8px;
        height: 40px;
        border-radius: 8px;
        font-weight: 500;
        
        .el-icon {
          font-size: 16px;
        }
        
        &--default {
          color: #4a6572;
          border-color: #4a6572;
          
          &:hover {
            background-color: rgba(74, 101, 114, 0.1);
            color: #344955;
          }
        }
        
        &--primary {
          background-color: #4a6572;
          border-color: #4a6572;
        
        &:hover {
            background-color: #344955;
            border-color: #344955;
          }
        }
      }
      
      .user-avatar-container {
        display: flex;
        align-items: center;
        gap: 10px;
        cursor: pointer;
        padding: 4px 8px;
        border-radius: 8px;
        transition: background-color 0.2s;
        
        &:hover {
          background-color: rgba(74, 101, 114, 0.05);
        }
        
        .user-info {
          display: flex;
          align-items: center;
          gap: 4px;
          
          .username {
            font-size: 15px;
            font-weight: 500;
            color: #344955;
            max-width: 120px;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
          }
          
          .el-icon {
            color: #4a6572;
            font-size: 12px;
          }
        }
      }
    }
  }
  
  .main-content {
    flex: 1;
    padding: 0;
  }
  
  .footer {
    background-color: #f5f5f5;
    color: #4a6572;
    padding: 40px 0 20px;
    height: auto;
    
    .footer-content {
      display: flex;
      justify-content: space-between;
      flex-wrap: wrap;
      gap: 40px;
      
      .footer-info, .footer-links {
        h3 {
          margin-bottom: 20px;
          font-size: 16px;
          font-weight: 500;
          color: #344955;
        }
        
        p {
          margin: 10px 0;
          display: flex;
          align-items: center;
          gap: 8px;
        }
        
        .el-icon {
          font-size: 18px;
          color: #4a6572;
        }
      }
      
      .footer-links {
        ul {
          list-style: none;
          padding: 0;
          margin: 0;
          
          li {
            margin-bottom: 10px;
        
        a {
              color: #4a6572;
          text-decoration: none;
              transition: color 0.2s;
          
          &:hover {
                color: #344955;
              }
            }
          }
        }
      }
    }
    
    .copyright {
      text-align: center;
      border-top: 1px solid #e4e7ed;
      padding-top: 20px;
      margin-top: 30px;
      
      p {
        font-size: 14px;
        color: #909399;
      }
    }
  }
  
  /* 响应式设计 */
  @media (max-width: 768px) {
    .header .container {
      flex-direction: column;
      height: auto;
      padding: 16px 20px;
      gap: 20px;
      
      .nav-wrapper {
        width: 100%;
        flex-direction: column;
        gap: 16px;
      }
      
      .nav-links {
        width: 100%;
        justify-content: space-between;
        gap: 10px;
      }
      
      .auth-buttons {
        width: 100%;
        
        .el-button {
          width: 100%;
          justify-content: center;
        }
        
        .user-avatar-container {
        justify-content: center;
          width: 100%;
          padding: 8px;
        }
      }
    }
    
    .footer .footer-content {
      flex-direction: column;
      gap: 30px;
      
      .footer-info, .footer-links {
        width: 100%;
      }
    }
  }
  </style>