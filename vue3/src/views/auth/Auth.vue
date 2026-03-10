<template>
  <div class="auth-container">
    <div class="auth-box">
      <div class="auth-header" v-if="showHeader">
        <div class="logo">
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
        </div>
        <h1 class="title">小型宾馆预约系统</h1>
        <div class="subtitle">HOTEL RESERVATION SYSTEM</div>
      </div>
      
      <el-form :model="formData" :rules="rules" ref="formRef" class="auth-form">
        <slot name="form-items"></slot>
        
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSubmit" class="auth-button">
            {{ submitText }}
          </el-button>
        </el-form-item>
        
        <div class="auth-links">
          <slot name="auth-links"></slot>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  formData: {
    type: Object,
    required: true
  },
  rules: {
    type: Object,
    required: true
  },
  loading: {
    type: Boolean,
    default: false
  },
  submitText: {
    type: String,
    default: '提交'
  },
  showHeader: {
    type: Boolean,
    default: true
  }
})

const formRef = ref(null)

const emit = defineEmits(['submit'])

const handleSubmit = () => {
  formRef.value.validate(valid => {
    if (valid) {
      emit('submit', formRef)
    }
  })
}

defineExpose({
  formRef
})
</script>

<style lang="scss" scoped>
.auth-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f9f9f9;
  background-image: url('@/assets/images/hotel-bg.png');
  background-size: cover;
  background-position: center;
}

.auth-box {
  width: 380px;
  padding: 40px;
  background-color: #ffffff;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.auth-header {
  text-align: center;
  margin-bottom: 30px;
  
  .logo {
    color: #4a6572;
    margin-bottom: 16px;
  }
  
  .title {
    font-size: 24px;
    font-weight: 500;
    color: #344955;
    margin: 0 0 8px 0;
  }
  
  .subtitle {
    font-size: 12px;
    color: #4a6572;
    letter-spacing: 1px;
  }
}

.auth-form {
  .el-form-item {
    margin-bottom: 20px;
  }
  
  :deep(.el-input__wrapper) {
    border-radius: 4px;
    box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.1) inset;
    transition: box-shadow 0.2s;
    
    &:hover, &.is-focus {
      box-shadow: 0 0 0 1px #4a6572 inset;
    }
  }
  
  :deep(.el-input__inner) {
    height: 40px;
  }
}

.auth-button {
  width: 100%;
  height: 40px;
  border-radius: 4px;
  background-color: #4a6572;
  border-color: #4a6572;
  
  &:hover, &:focus {
    background-color: #344955;
    border-color: #344955;
  }
}

.auth-links {
  text-align: center;
  margin-top: 16px;
  font-size: 14px;
  color: #909399;
  
  a {
    color: #4a6572;
    text-decoration: none;
    transition: color 0.2s;
    
    &:hover {
      color: #344955;
    }
  }
}
</style>