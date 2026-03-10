<template>
  <div class="user-info">
    <el-card class="box-card info-card">
      <template #header>
        <div class="card-header">
          <div class="header-title">
            <div class="title-decoration"></div>
            <h3>个人资料</h3>
          </div>
          <el-button type="primary" @click="handleEdit" :icon="Edit" :disabled="isEditing">
            {{ isEditing ? '编辑中' : '编辑' }}
          </el-button>
        </div>
      </template>
      
      <el-row :gutter="30">
        <el-col :xs="24" :sm="8" :md="6">
          <div class="avatar-section">
            <div class="avatar-container">
              <el-avatar :size="130" :src="avatarUrl">
                <el-icon><UserFilled /></el-icon>
              </el-avatar>
              <div class="avatar-decoration"></div>
            </div>
            <div class="avatar-upload" v-if="isEditing">
              <el-upload
                class="avatar-uploader"
                action="#"
                :auto-upload="true"
                :show-file-list="false"
                :http-request="customUploadAvatar"
                :before-upload="beforeAvatarUpload"
              >
                <el-button size="default" type="primary" class="upload-btn">
                  <el-icon><Upload /></el-icon>
                  <span>更换头像</span>
                </el-button>
              </el-upload>
            </div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="16" :md="18">
          <div class="info-content">
            <div v-if="!isEditing" class="info-display">
              <el-descriptions :column="2" border>
                <el-descriptions-item label="用户名">{{ userStore.userInfo.username }}</el-descriptions-item>
                <el-descriptions-item label="姓名">{{ userStore.userInfo.name || '未设置' }}</el-descriptions-item>
                <el-descriptions-item label="手机号">{{ userStore.userInfo.phone || '未设置' }}</el-descriptions-item>
                <el-descriptions-item label="邮箱">{{ userStore.userInfo.email }}</el-descriptions-item>
                <el-descriptions-item label="性别">{{ userStore.userInfo.sex || '未设置' }}</el-descriptions-item>
                <el-descriptions-item label="注册时间">{{ userStore.userInfo.createTime }}</el-descriptions-item>
              </el-descriptions>
            </div>
            <div v-else class="info-form">
              <el-form :model="userForm" :rules="rules" ref="userFormRef" label-width="80px">
                <el-form-item label="用户名" prop="username">
                  <el-input v-model="userForm.username" disabled />
                </el-form-item>
                <el-form-item label="姓名" prop="name">
                  <el-input v-model="userForm.name" />
                </el-form-item>
                <el-form-item label="手机号" prop="phone">
                  <el-input v-model="userForm.phone" />
                </el-form-item>
                <el-form-item label="邮箱" prop="email">
                  <el-input v-model="userForm.email" disabled />
                </el-form-item>
                <el-form-item label="性别" prop="sex">
                  <el-select v-model="userForm.sex" placeholder="请选择性别" style="width: 100%">
                    <el-option label="男" value="男" />
                    <el-option label="女" value="女" />
                    <el-option label="其他" value="其他" />
                  </el-select>
                </el-form-item>
                <el-form-item>
                  <div class="form-actions">
                    <el-button type="primary" @click="submitForm" :loading="submitting">保存</el-button>
                    <el-button @click="cancelEdit">取消</el-button>
                  </div>
                </el-form-item>
              </el-form>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>
    
    <el-card class="box-card password-card">
      <template #header>
        <div class="card-header">
          <div class="header-title">
            <div class="title-decoration"></div>
            <h3>修改密码</h3>
          </div>
        </div>
      </template>
      
      <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="120px">
        <el-form-item label="当前密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="changePassword" :loading="submitting">修改密码</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { UserFilled, Edit, Upload } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'

const userStore = useUserStore()
const isEditing = ref(false)
const submitting = ref(false)
const userFormRef = ref(null)
const passwordFormRef = ref(null)

// 用户表单
const userForm = reactive({
  id: '',
  username: '',
  name: '',
  phone: '',
  email: '',
  sex: '',
  avatar: ''
})

// 密码表单
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 表单验证规则
const rules = {
  name: [
    { max: 50, message: '姓名长度不能超过50个字符', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
}

// 密码表单验证规则
const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { 
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      }, 
      trigger: 'blur' 
    }
  ]
}

// 头像（文件）地址
const baseAPI = process.env.VUE_APP_BASE_API || "/api";
const avatarUrl = computed(() => {
  return userForm.avatar ? baseAPI + userForm.avatar : '';
});

// 上传头像前的校验
const beforeAvatarUpload = (file) => {
  const isJPG = file.type === "image/jpeg";
  const isPNG = file.type === "image/png";
  const isLt2M = file.size / 1024 / 1024 < 2;

  if (!isJPG && !isPNG) {
    ElMessage.error("头像只能是 JPG 或 PNG 格式!");
    return false;
  }
  if (!isLt2M) {
    ElMessage.error("头像大小不能超过 2MB!");
    return false;
  }
  return true;
};

// 自定义头像上传方法
const customUploadAvatar = async (options) => {
  try {
    const { file } = options;

    // 创建 FormData 对象
    const formData = new FormData();
    formData.append("file", file);

    // 设置自定义上传选项
    const uploadOptions = {
      headers: {
        token: localStorage.getItem("token") || "",
      },
      // 不进行JSON处理
      transformRequest: [(data) => data],
      // 自定义成功消息
      successMsg: "头像上传成功",
      // 自定义错误消息
      errorMsg: "头像上传失败",
      // 成功回调
      onSuccess: async (data) => {
        // 更新用户头像
        userForm.avatar = data;

        // 保存到后端
        await updateUserAvatar(data);

        // 通知上传成功
        options.onSuccess({ data });
      },
      // 错误回调
      onError: (error) => {
        console.error("头像上传错误:", error);
        options.onError(new Error(error.message || "上传失败"));
      },
    };

    // 发送上传请求
    await request.post("/file/upload/img", formData, uploadOptions);
  } catch (error) {
    options.onError(error);
    console.error("头像上传过程发生错误:", error);
  }
};

// 更新用户头像信息
const updateUserAvatar = async (avatarPath) => {
  try {
    await request.put(
      `/user/${userForm.id}`,
      { avatar: avatarPath },
      {
        showDefaultMsg: false,
        onSuccess: (data) => {
          // 更新本地用户信息
          const updatedUserInfo = { ...userStore.userInfo, avatar: avatarPath };
          userStore.updateUserInfo(updatedUserInfo);
        },
        onError: (error) => {
          console.error("头像信息保存失败", error);
          ElMessage.error("头像信息保存失败");
        },
      }
    );
  } catch (error) {
    console.error("头像信息保存失败", error);
    ElMessage.error("头像信息保存失败");
    throw error;
  }
};

// 编辑用户信息
const handleEdit = () => {
  isEditing.value = true
  
  // 复制用户信息到表单
  const userInfo = userStore.userInfo
  userForm.id = userInfo.id
  userForm.username = userInfo.username
  userForm.name = userInfo.name
  userForm.phone = userInfo.phone
  userForm.email = userInfo.email
  userForm.sex = userInfo.sex
  userForm.avatar = userInfo.avatar
}

// 取消编辑
const cancelEdit = () => {
  isEditing.value = false
}

// 提交表单
const submitForm = async () => {
  if (!userFormRef.value) return
  
  await userFormRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        await request.put(`/user/${userForm.id}`, {
          name: userForm.name,
          phone: userForm.phone,
          sex: userForm.sex
        }, {
          successMsg: '个人信息更新成功',
          onSuccess: (data) => {
            // 更新Vuex中的用户信息
            const updatedUserInfo = { ...userStore.userInfo, ...userForm }
            userStore.updateUserInfo(updatedUserInfo)
            isEditing.value = false
          }
        })
      } finally {
        submitting.value = false
      }
    }
  })
}

// 修改密码
const changePassword = async () => {
  if (!passwordFormRef.value) return
  
  await passwordFormRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        await request.post('/user/changePassword', {
          oldPassword: passwordForm.oldPassword,
          newPassword: passwordForm.newPassword
        }, {
          successMsg: '密码修改成功',
          onSuccess: () => {
            // 重置表单
            passwordFormRef.value.resetFields()
          }
        })
      } finally {
        submitting.value = false
      }
    }
  })
}

// 生命周期钩子
onMounted(() => {
  // 如果用户信息已存在，直接使用
  if (userStore.userInfo) {
    const userInfo = userStore.userInfo
    userForm.id = userInfo.id
    userForm.username = userInfo.username
    userForm.name = userInfo.name
    userForm.phone = userInfo.phone
    userForm.email = userInfo.email
    userForm.sex = userInfo.sex
    userForm.avatar = userInfo.avatar
  } else {
    // 获取用户信息
    userStore.getUserInfo().catch(error => {
      console.error('获取用户信息失败:', error)
    })
  }
})
</script>

<style lang="scss" scoped>
.user-info {
  font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
  
  .box-card {
    margin-bottom: 30px;
    border-radius: 8px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
    transition: all 0.3s ease;
    
    &:hover {
      box-shadow: 0 6px 30px rgba(0, 0, 0, 0.1);
    }
    
    &.info-card {
      margin-bottom: 30px;
    }
    
    &.password-card {
      .card-header {
        border-bottom: 1px solid #eaeaea;
      }
    }
  }
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 15px;
    
    .header-title {
      display: flex;
      align-items: center;
      
      .title-decoration {
        width: 3px;
        height: 20px;
        background-color: #1890ff;
        margin-right: 10px;
        border-radius: 3px;
      }
      
      h3 {
        margin: 0;
        font-size: 18px;
        font-weight: 500;
        color: #333;
      }
    }
    
    .el-button {
      display: flex;
      align-items: center;
      padding: 10px 20px;
      
      .el-icon {
        margin-right: 5px;
      }
    }
  }
  
  .avatar-section {
    display: flex;
    flex-direction: column;
    align-items: center;
    margin-bottom: 20px;
    
    .avatar-container {
      position: relative;
      margin-bottom: 25px;
      
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
    
    .avatar-upload {
      margin-top: 15px;
      
      .upload-btn {
        display: flex;
        align-items: center;
        
        .el-icon {
          margin-right: 5px;
        }
      }
    }
  }
  
  .info-content {
    padding: 10px 0;
    
    .info-display {
      :deep(.el-descriptions) {
        .el-descriptions__header {
          margin-bottom: 15px;
        }
        
        .el-descriptions__body {
          .el-descriptions__table {
            .el-descriptions__cell {
              padding: 12px 15px;
            }
            
            .el-descriptions__label {
              color: #555;
              font-weight: 500;
              background-color: #f8f9fa;
            }
            
            .el-descriptions__content {
              color: #333;
            }
          }
        }
      }
    }
    
    .info-form {
      :deep(.el-form) {
        .el-form-item {
          margin-bottom: 20px;
          
          .el-form-item__label {
            color: #555;
            font-weight: 500;
          }
          
          .el-input, .el-select {
            .el-input__wrapper {
              box-shadow: 0 0 0 1px #dcdfe6 inset;
              transition: all 0.3s;
              
              &:hover {
                box-shadow: 0 0 0 1px #1890ff inset;
              }
              
              &.is-focus {
                box-shadow: 0 0 0 1px #1890ff inset;
              }
            }
          }
        }
        
        .form-actions {
          display: flex;
          gap: 10px;
          
          .el-button {
            padding: 10px 20px;
          }
        }
      }
    }
  }
  
  :deep(.el-form) {
    max-width: 500px;
    margin: 0 auto;
    
    .el-input, .el-select {
      .el-input__wrapper {
        box-shadow: 0 0 0 1px #dcdfe6 inset;
        transition: all 0.3s;
        
        &:hover {
          box-shadow: 0 0 0 1px #1890ff inset;
        }
        
        &.is-focus {
          box-shadow: 0 0 0 1px #1890ff inset;
        }
      }
    }
    
    .el-form-item {
      margin-bottom: 20px;
      
      .el-form-item__label {
        color: #555;
        font-weight: 500;
      }
      
      .el-button {
        padding: 10px 20px;
      }
    }
  }
}

@media (max-width: 768px) {
  .user-info {
    .avatar-section {
      margin-bottom: 30px;
    }
    
    :deep(.el-form) {
      max-width: 100%;
    }
  }
}
</style> 