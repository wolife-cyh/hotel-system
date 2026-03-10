<template>
  <Auth 
    :formData="loginForm" 
    :rules="rules" 
    :loading="loading"
    submitText="登录"
    @submit="handleSubmit"
  >
    <template #form-items>
      <el-form-item prop="username">
        <el-input 
          v-model="loginForm.username"
          placeholder="用户名"
          class="auth-input">
          <template #prefix>
            <el-icon><User /></el-icon>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item prop="password">
        <el-input 
          v-model="loginForm.password"
          type="password"
          placeholder="密码"
          class="auth-input">
          <template #prefix>
            <el-icon><Lock /></el-icon>
          </template>
        </el-input>
      </el-form-item>
    </template>

    <template #auth-links>
      <div class="links-container">

        <router-link to="/register" class="register-link">注册账号</router-link>
      </div>
    </template>
  </Auth>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'
import { User, Lock } from '@element-plus/icons-vue'
import Auth from './Auth.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

const handleSubmit = (form) => {
  formRef.value = form.value
  loginFormRef.value = form.value
  handleLogin()
}

const loginFormRef = ref(null)

const handleLogin = () => {
  loginFormRef.value.validate(async valid => {
    if (valid) {
      loading.value = true
      try {
        // 统一使用用户登录接口
        const res = await request.post("/user/login", loginForm, {
          successMsg: "登录成功",
          showDefaultMsg: true,
          onSuccess: async (data) => {
            userStore.setUserInfo(data)
            
            // 根据返回的角色决定跳转路径
            if (data.roleCode !== 'USER') {
              // 管理员登录，设置菜单和路由
              await router.isReady()
              router.push(route.query.redirect || '/back/dashboard')
            } else {
              // 普通用户登录，直接跳转到前台
              const redirect = route.query.redirect || '/'
              router.push(redirect)
            }
          },
          onError: (error) => {
            console.error('登录失败:', error)
          }
        })
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.auth-input {
  :deep(.el-input__wrapper) {
    padding-left: 12px;
  }
  
  :deep(.el-input__prefix) {
    margin-right: 10px;
    color: #9aa6af;
  }
}

.links-container {
  display: flex;
  justify-content: space-between;
  width: 100%;
  
  a {
    font-size: 14px;
  }
  
  .forget-link {
    color: #9aa6af;
  }
  
  .register-link {
    font-weight: 500;
  }
}
</style> 