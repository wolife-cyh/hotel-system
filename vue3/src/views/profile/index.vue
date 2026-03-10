<template>
  <div class="profile-container">
    <div class="profile-aside">
      <div class="user-info-panel">
        <div class="avatar-container">
          <el-avatar :size="90" :src="avatarUrl">
            <el-icon><UserFilled /></el-icon>
          </el-avatar>
          <div class="avatar-decoration"></div>
        </div>
        <h3 class="username">{{ userStore.userInfo ? userStore.userInfo.username : '加载中...' }}</h3>
        <div class="user-role" v-if="userStore.userInfo">{{ userStore.userInfo.roleCode === 'ADMIN' ? '管理员' : '用户' }}</div>
      </div>
      <el-menu
        :default-active="activeIndex"
        class="profile-menu"
        router
      >
        <el-menu-item index="/profile/info">
          <el-icon><User /></el-icon>
          <span>个人资料</span>
        </el-menu-item>
        <el-menu-item index="/profile/reservation">
          <el-icon><Calendar /></el-icon>
          <span>历史订单</span>
        </el-menu-item>
        <el-menu-item index="/profile/order">
          <el-icon><Wallet /></el-icon>
          <span>我的订单</span>
        </el-menu-item>
        <el-menu-item index="/profile/review">
          <el-icon><Comment /></el-icon>
          <span>订单评价及反馈</span>
        </el-menu-item>
      </el-menu>
    </div>
    <div class="profile-content">
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { User, UserFilled, Calendar, Comment, Wallet } from '@element-plus/icons-vue'

const route = useRoute()
const userStore = useUserStore()

// 计算当前激活的菜单项
const activeIndex = computed(() => {
  return route.path
})

// 头像URL
const baseAPI = process.env.VUE_APP_BASE_API || "/api";
const avatarUrl = computed(() => {
  return userStore.userInfo && userStore.userInfo.avatar 
    ? baseAPI + userStore.userInfo.avatar 
    : '';
})

// 获取用户信息
onMounted(() => {
  if (!userStore.userInfo) {
    userStore.getUserInfo().catch(error => {
      console.error('获取用户信息失败:', error)
    })
  }
})
</script>

<style lang="scss" scoped>
.profile-container {
  display: flex;
  height: calc(100vh - 80px);
  font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;

  .profile-aside {
    width: 260px;
    background-color: #fff;
    border-right: 1px solid #eaeaea;
    display: flex;
    flex-direction: column;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
    transition: all 0.3s ease;
    
    .user-info-panel {
      padding: 30px 20px;
      text-align: center;
      border-bottom: 1px solid #f0f0f0;
      margin-bottom: 15px;
      
      .avatar-container {
        position: relative;
        width: 90px;
        height: 90px;
        margin: 0 auto 15px;
        
        .el-avatar {
          border: 3px solid rgba(24, 144, 255, 0.1);
          box-shadow: 0 4px 15px rgba(0, 0, 0, 0.08);
          transition: all 0.3s ease;
          
          &:hover {
            transform: translateY(-3px);
            box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12);
          }
        }
        
        .avatar-decoration {
          position: absolute;
          width: 40px;
          height: 3px;
          background: #1890ff;
          bottom: -10px;
          left: 50%;
          transform: translateX(-50%);
          border-radius: 2px;
        }
      }
      
      .username {
        margin: 15px 0 5px;
        font-size: 20px;
        font-weight: 300;
        color: #333;
        letter-spacing: 0.5px;
      }
      
      .user-role {
        font-size: 14px;
        color: #1890ff;
        font-weight: 500;
        margin-bottom: 5px;
      }
    }

    .profile-menu {
      flex: 1;
      border-right: none;
      
      :deep(.el-menu-item) {
        height: 50px;
        line-height: 50px;
        font-size: 15px;
        color: #666;
        margin: 4px 0;
        border-radius: 0 25px 25px 0;
        margin-right: 15px;
        transition: all 0.3s;
        
        &:hover {
          background-color: rgba(24, 144, 255, 0.05);
          color: #1890ff;
        }
        
        &.is-active {
          background-color: rgba(24, 144, 255, 0.1);
          color: #1890ff;
          font-weight: 500;
          
          &::before {
            content: '';
            position: absolute;
            left: 0;
            top: 50%;
            transform: translateY(-50%);
            width: 3px;
            height: 20px;
            background-color: #1890ff;
            border-radius: 0 3px 3px 0;
          }
        }
        
        .el-icon {
          margin-right: 10px;
          font-size: 18px;
        }
      }
    }
  }

  .profile-content {
    flex: 1;
    padding: 30px;
    overflow-y: auto;
    background-color: #f8fafb;
  }
}

@media screen and (max-width: 768px) {
  .profile-container {
    flex-direction: column;
    
    .profile-aside {
      width: 100%;
      height: auto;
      border-right: none;
      border-bottom: 1px solid #eaeaea;
      
      .user-info-panel {
        padding: 20px 15px;
      }
      
      .profile-menu {
        :deep(.el-menu-item) {
          border-radius: 25px;
          margin: 4px 15px;
        }
      }
    }
    
    .profile-content {
      padding: 20px;
    }
  }
}
</style> 