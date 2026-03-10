<template>
  <div class="user-management">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <el-button type="primary" @click="handleAddUser" v-if="isAdmin">添加用户</el-button>
        </div>
      </template>
      
      <!-- 搜索区域 -->
      <div class="search-container">
        <el-form :inline="true" :model="searchForm" class="demo-form-inline">
          <el-form-item label="用户名">
            <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable />
          </el-form-item>
          <el-form-item label="姓名">
            <el-input v-model="searchForm.name" placeholder="请输入姓名" clearable />
          </el-form-item>
          <el-form-item label="角色">
            <el-select v-model="searchForm.roleCode" placeholder="请选择角色" clearable>
              <el-option label="管理员" value="ADMIN" />
              <el-option label="普通用户" value="USER" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="fetchUsers">查询</el-button>
            <el-button @click="resetSearch">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
      
      <!-- 用户列表 -->
      <el-table :data="tableData" style="width: 100%" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column label="头像" width="100">
          <template #default="scope">
            <el-avatar :src="getAvatarUrl(scope.row.avatar)" :size="40">
              <el-icon><UserFilled /></el-icon>
            </el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" width="180" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="sex" label="性别" width="80" />
        <el-table-column prop="roleName" label="角色" width="100" />
       
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
              {{ scope.row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" />
        <el-table-column label="操作" fixed="right" width="200">
          <template #default="scope">
            <el-button type="primary" size="small" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button 
              type="success" 
              size="small" 
              v-if="scope.row.status === 0" 
              @click="handleUpdateStatus(scope.row, 1)">
              启用
            </el-button>
            <el-button 
              type="warning" 
              size="small" 
              v-if="scope.row.status === 1 && scope.row.roleCode !== 'ADMIN'" 
              @click="handleUpdateStatus(scope.row, 0)">
              禁用
            </el-button>
            <el-button 
              type="danger" 
              size="small" 
              v-if="scope.row.roleCode !== 'ADMIN'" 
              @click="handleDelete(scope.row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model="currentPage"
          :page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
      
      <!-- 用户表单对话框 -->
      <el-dialog v-model="dialogVisible" :title="dialogType === 'add' ? '添加用户' : '编辑用户'" width="500px" @closed="resetUserForm">
        <el-form :model="userForm" :rules="rules" ref="userFormRef" label-width="100px">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="userForm.username" :disabled="dialogType === 'edit'" />
          </el-form-item>
          <el-form-item label="密码" prop="password" v-if="dialogType === 'add'">
            <el-input v-model="userForm.password" type="password" show-password />
          </el-form-item>
          <el-form-item label="姓名" prop="name">
            <el-input v-model="userForm.name" />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="userForm.email" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="userForm.phone" />
          </el-form-item>
          <el-form-item label="性别" prop="sex">
            <el-radio-group v-model="userForm.sex">
              <el-radio label="男">男</el-radio>
              <el-radio label="女">女</el-radio>
              <el-radio label="其他">其他</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="角色" prop="roleCode">
            <el-select v-model="userForm.roleCode" placeholder="请选择角色">
              <el-option label="管理员" value="ADMIN" />
              <el-option label="普通用户" value="USER" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-switch 
              v-model="userForm.status" 
              :active-value="1" 
              :inactive-value="0"
              active-text="正常"
              inactive-text="禁用"
            />
          </el-form-item>
          <el-form-item label="头像" prop="avatar">
            <el-upload
              class="avatar-uploader"
              action="#"
              :auto-upload="true"
              :show-file-list="false"
              :http-request="customUploadAvatar"
              :before-upload="beforeAvatarUpload"
            >
              <img v-if="userForm.avatar" :src="getAvatarUrl(userForm.avatar)" class="avatar" />
              <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
            </el-upload>
          </el-form-item>
        </el-form>
        <template #footer>
          <span class="dialog-footer">
            <el-button @click="dialogVisible = false">取消</el-button>
            <el-button type="primary" @click="submitForm">确定</el-button>
          </span>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UserFilled, Plus } from '@element-plus/icons-vue'
import request from '@/utils/request'

// 用户存储
const userStore = useUserStore()

// 用户列表数据
const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 搜索表单
const searchForm = reactive({
  username: '',
  name: '',
  roleCode: ''
})

// 用户表单
const dialogVisible = ref(false)
const dialogType = ref('add') // 'add' 或 'edit'
const userFormRef = ref(null)
const userForm = reactive({
  id: null,
  username: '',
  password: '',
  name: '',
  email: '',
  phone: '',
  sex: '男',
  roleCode: 'USER',
  status: 1,
  avatar: ''
})

// 表单验证规则
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '长度在 3 到 50 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  roleCode: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ]
}

// 计算属性
const isAdmin = computed(() => userStore.role === 'ADMIN')

// 获取用户头像URL
const baseAPI = process.env.VUE_APP_BASE_API || '/api'
const getAvatarUrl = (avatar) => {
  if (!avatar) return ''
  if (avatar.startsWith('http')) return avatar
  return baseAPI + avatar
}

// 获取用户列表
const fetchUsers = async () => {
  loading.value = true
  try {
    await request.get('/user/page', {
    
        currentPage: currentPage.value,
        size: pageSize.value,
        username: searchForm.username,
        name: searchForm.name,
        roleCode: searchForm.roleCode
      },{
      onSuccess: (res) => {
        tableData.value = res.records
        total.value = res.total
      }
    })
  } catch (error) {
    console.error('获取用户列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 重置搜索
const resetSearch = () => {
  searchForm.username = ''
  searchForm.name = ''
  searchForm.roleCode = ''
  fetchUsers()
}

// 处理分页大小变化
const handleSizeChange = (val) => {
  pageSize.value = val
  fetchUsers()
}

// 处理页码变化
const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchUsers()
}

// 添加用户
const handleAddUser = () => {
  dialogType.value = 'add'
  resetUserForm()
  dialogVisible.value = true
}

// 编辑用户
const handleEdit = (row) => {
  dialogType.value = 'edit'
  Object.keys(userForm).forEach(key => {
    if (key in row) {
      userForm[key] = row[key]
    }
  })
  dialogVisible.value = true
}

// 重置用户表单
const resetUserForm = () => {
  userForm.id = null
  userForm.username = ''
  userForm.password = ''
  userForm.name = ''
  userForm.email = ''
  userForm.phone = ''
  userForm.sex = '男'
  userForm.roleCode = 'USER'
  userForm.status = 1
  userForm.avatar = ''
  if (userFormRef.value) {
    userFormRef.value.resetFields()
  }
}

// 提交表单
const submitForm = async () => {
  if (!userFormRef.value) return
  
  await userFormRef.value.validate(async (valid) => {
    if (valid) {
      if (dialogType.value === 'add') {
        // 添加用户
        await request.post('/user/add', userForm, {
          successMsg: '添加用户成功',
          onSuccess: () => {
            dialogVisible.value = false
            fetchUsers()
          }
        })
      } else {
        // 编辑用户
        await request.put(`/user/${userForm.id}`, userForm, {
          successMsg: '更新用户成功',
          onSuccess: () => {
            dialogVisible.value = false
            fetchUsers()
          }
        })
      }
    }
  })
}

// 删除用户
const handleDelete = (row) => {
  ElMessageBox.confirm(
    '确定要删除该用户吗？删除后无法恢复',
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    await request.delete(`/user/delete/${row.id}`, {
      successMsg: '删除用户成功',
      onSuccess: () => {
        fetchUsers()
      }
    })
  }).catch(() => {})
}

// 更新用户状态
const handleUpdateStatus = async (row, status) => {
  const statusText = status === 1 ? '启用' : '禁用'
  
  await request.put(`/user/status/${row.id}?status=${status}`, null, {
    successMsg: `${statusText}用户成功`,
    onSuccess: () => {
      fetchUsers()
    }
  })
}

// 头像上传前的校验
const beforeAvatarUpload = (file) => {
  const isJPG = file.type === 'image/jpeg'
  const isPNG = file.type === 'image/png'
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isJPG && !isPNG) {
    ElMessage.error('头像只能是 JPG 或 PNG 格式!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('头像大小不能超过 2MB!')
    return false
  }
  return true
}

// 自定义头像上传方法
const customUploadAvatar = async (options) => {
  try {
    const { file } = options

    // 创建 FormData 对象
    const formData = new FormData()
    formData.append('file', file)

    // 设置自定义上传选项
    const uploadOptions = {
      headers: {
        token: localStorage.getItem('token') || ''
      },
      // 不进行JSON处理
      transformRequest: [(data) => data],
      // 自定义成功消息
      successMsg: '头像上传成功',
      // 自定义错误消息
      errorMsg: '头像上传失败',
      // 成功回调
      onSuccess: async (data) => {
        // 更新用户头像
        userForm.avatar = data

        // 通知上传成功
        options.onSuccess({ data })
      },
      // 错误回调
      onError: (error) => {
        console.error('头像上传错误:', error)
        options.onError(new Error(error.message || '上传失败'))
      }
    }

    // 发送上传请求
    await request.post('/file/upload/img', formData, uploadOptions)
  } catch (error) {
    options.onError(error)
    console.error('头像上传过程发生错误:', error)
  }
}

// 生命周期钩子
onMounted(() => {
  fetchUsers()
})
</script>

<style lang="scss" scoped>
.user-management {
  padding: 20px 0;
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .box-card {
    margin-bottom: 20px;
  }
  
  .search-container {
    margin-bottom: 20px;
  }
  
  .pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: center;
  }
  
  .avatar-uploader {
    :deep(.el-upload) {
      border: 1px dashed #d9d9d9;
      border-radius: 6px;
      cursor: pointer;
      position: relative;
      overflow: hidden;
      transition: var(--el-transition-duration-fast);
      
      &:hover {
        border-color: var(--el-color-primary);
      }
    }
  }
  
  .avatar-uploader-icon {
    font-size: 28px;
    color: #8c939d;
    width: 100px;
    height: 100px;
    text-align: center;
    line-height: 100px;
  }
  
  .avatar {
    width: 100px;
    height: 100px;
    display: block;
    object-fit: cover;
  }
}
</style> 