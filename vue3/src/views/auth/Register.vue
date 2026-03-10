<template>
  <Auth 
    :formData="registerForm" 
    :rules="rules" 
    :loading="loading"
    submitText="注册"
    @submit="handleSubmit"
  >
    <template #form-items>
      <el-form-item prop="username">
        <el-input 
          v-model="registerForm.username"
          placeholder="用户名"
          class="auth-input">
          <template #prefix>
            <el-icon><User /></el-icon>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item prop="password">
        <el-input 
          v-model="registerForm.password"
          type="password"
          placeholder="密码"
          class="auth-input">
          <template #prefix>
            <el-icon><Lock /></el-icon>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item prop="confirmPassword">
        <el-input 
          v-model="registerForm.confirmPassword"
          type="password"
          placeholder="确认密码"
          class="auth-input">
          <template #prefix>
            <el-icon><Lock /></el-icon>
          </template>
        </el-input>
      </el-form-item>
      
      <div class="form-divider">
        <span>个人信息</span>
      </div>
      
      <el-form-item prop="name">
        <el-input 
          v-model="registerForm.name"
          placeholder="姓名"
          class="auth-input">
          <template #prefix>
            <el-icon><UserFilled /></el-icon>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item prop="phone">
        <el-input 
          v-model="registerForm.phone"
          placeholder="手机号"
          class="auth-input">
          <template #prefix>
            <el-icon><Phone /></el-icon>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item prop="email">
        <el-input 
          v-model="registerForm.email"
          placeholder="邮箱"
          class="auth-input">
          <template #prefix>
            <el-icon><Message /></el-icon>
          </template>
        </el-input>
      </el-form-item>
    </template>

    <template #auth-links>
      <div class="login-link">
        已有账号？<router-link to="/login">登录</router-link>
      </div>
    </template>
  </Auth>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock, Message, Phone, UserFilled } from '@element-plus/icons-vue'
import request from '@/utils/request'
import Auth from './Auth.vue'

const router = useRouter()
const formRef = ref(null)
const registerFormRef = ref(null)
const loading = ref(false)

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  phone: '',
  name: '',
  roleCode: 'USER', // 默认注册为普通用户
})

const validatePass2 = (rule, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入密码不一致!'))
  } else {
    callback()
  }
}

const validateEmail = (rule, value, callback) => {
  const emailRegex = /^[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)*@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/
  if (!emailRegex.test(value)) {
    callback(new Error('邮箱格式不正确'))
  } else {
    callback()
  }
}

const validatePhone = (rule, value, callback) => {
  if (value && !/^1[3-9]\d{9}$/.test(value)) {
    callback(new Error('手机号格式不正确'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度必须在3到50个字符之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 100, message: '密码长度必须在6到100个字符之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validatePass2, trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { validator: validateEmail, trigger: 'blur' }
  ],
  phone: [
    { validator: validatePhone, trigger: 'blur' }
  ],
  name: [
    { required: false }
  ]
}

const handleSubmit = (form) => {
  formRef.value = form.value
  registerFormRef.value = form.value
  handleRegister()
}

const handleRegister = () => {
  registerFormRef.value.validate(async valid => {
    if (valid) {
      loading.value = true
      try {
        const { confirmPassword, ...registerData } = registerForm
        await request.post("/user/add", registerData, {
          successMsg: "注册成功",
          showDefaultMsg: true,
          onSuccess: () => {
            router.push('/login')
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

.form-divider {
  display: flex;
  align-items: center;
  margin: 24px 0;
  color: #9aa6af;
  font-size: 14px;
  
  &::before,
  &::after {
    content: '';
    flex: 1;
    height: 1px;
    background-color: #e4e7ed;
  }
  
  span {
    padding: 0 12px;
  }
}

.login-link {
  text-align: center;
  color: #9aa6af;
  
  a {
    color: #4a6572;
    font-weight: 500;
    margin-left: 4px;
  }
}
</style> 