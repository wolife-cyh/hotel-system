<template>
  <div class="banner-manage">
    <div class="page-header">
      <div class="title-container">
        <h2 class="page-title">广告管理</h2>
        <div class="title-decoration"></div>
      </div>
    </div>

    <div class="search-container">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="标题">
          <el-input v-model="searchForm.title" placeholder="请输入广告标题" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchBanners">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="action-container">
      <el-button type="primary" class="add-button" @click="openDialog()">
        <el-icon><Plus /></el-icon>新增广告
      </el-button>
    </div>

    <div class="table-container">
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        style="width: 100%"
        :header-cell-style="{ backgroundColor: '#f5f7fa', color: '#606266' }"
      >
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column prop="title" label="标题" min-width="150" />
        <el-table-column label="图片" min-width="150">
          <template #default="{ row }">
            <el-image
              :src="getImageUrl(row.imageUrl)"
              style="width: 100px; height: 60px"
              fit="cover"
              :preview-teleported="true"
              :preview-src-list="[getImageUrl(row.imageUrl)]"
            />
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.status === 1 ? 'success' : 'danger'"
              effect="plain"
            >
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button type="primary" size="small" plain @click="openDialog(row)">
                <el-icon><Edit /></el-icon>编辑
              </el-button>
              <el-button 
                :type="row.status === 1 ? 'danger' : 'success'" 
                size="small" 
                plain
                @click="changeStatus(row)"
              >
                {{ row.status === 1 ? '禁用' : '启用' }}
              </el-button>
              <el-button type="danger" size="small" plain @click="handleDelete(row)">
                <el-icon><Delete /></el-icon>删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="pagination-container">
      <el-pagination
        :current-page="currentPage"
        :page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        @update:current-page="currentPage = $event"
        @update:page-size="pageSize = $event"
      />
    </div>

    <!-- 广告表单对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="formType === 'add' ? '新增广告' : '编辑广告'"
      width="650px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form
        ref="bannerFormRef"
        :model="bannerForm"
        :rules="rules"
        label-width="100px"
        class="banner-form"
      >
        <el-form-item label="标题" prop="title">
          <el-input v-model="bannerForm.title" placeholder="请输入广告标题" />
        </el-form-item>
        <el-form-item label="图片" prop="imageUrl">
          <el-upload
            class="banner-uploader"
            action="#"
            :auto-upload="true"
            :show-file-list="false"
            :http-request="customUploadImage"
            :before-upload="beforeImageUpload"
          >
            <img v-if="bannerForm.imageUrl" :src="getImageUrl(bannerForm.imageUrl)" class="banner-image" />
            <div v-else class="banner-uploader-icon">
              <el-icon><Plus /></el-icon>
              <p>点击上传图片</p>
            </div>
          </el-upload>
          <div class="upload-tip">格式为JPG、PNG</div>
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="bannerForm.sortOrder" :min="0" :max="999" />
          <div class="form-tip">数字越小越靠前</div>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="bannerForm.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import request from '@/utils/request'

// 基础API路径
const baseAPI = process.env.VUE_APP_BASE_API || "/api"

// 数据加载状态
const loading = ref(false)
// 表格数据
const tableData = ref([])
// 分页参数
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
// 搜索表单
const searchForm = reactive({
  title: '',
  status: null
})
// 广告表单对话框
const dialogVisible = ref(false)
const formType = ref('add') // add或edit
const bannerFormRef = ref(null)
const bannerForm = reactive({
  id: null,
  title: '',
  imageUrl: '',
  sortOrder: 0,
  status: 1
})
// 表单验证规则
const rules = {
  title: [
    { required: true, message: '请输入广告标题', trigger: 'blur' },
    { max: 100, message: '标题长度不能超过100个字符', trigger: 'blur' }
  ],
  imageUrl: [
    { required: true, message: '请上传广告图片', trigger: 'change' }
  ]
}

// 获取广告列表
const fetchBanners = async () => {
  loading.value = true
  try {
    await request.get('/banner/page', {
      title: searchForm.title,
      status: searchForm.status,
      currentPage: currentPage.value,
      size: pageSize.value
    }, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        tableData.value = res.records
        total.value = res.total
      }
    })
  } catch (error) {
    console.error('获取广告列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 重置搜索条件
const resetSearch = () => {
  searchForm.title = ''
  searchForm.status = null
  fetchBanners()
}

// 处理页大小变化
const handleSizeChange = (val) => {
  pageSize.value = val
  fetchBanners()
}

// 处理当前页变化
const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchBanners()
}

// 获取图片URL
const getImageUrl = (imageUrl) => {
  if (!imageUrl) return ''
  if (imageUrl.startsWith('http')) {
    return imageUrl
  }
  return baseAPI + imageUrl
}

// 打开对话框
const openDialog = (row) => {
  if (row) {
    // 编辑模式
    formType.value = 'edit'
    Object.assign(bannerForm, row)
  } else {
    // 新增模式
    formType.value = 'add'
    Object.assign(bannerForm, {
      id: null,
      title: '',
      imageUrl: '',
      sortOrder: 0,
      status: 1
    })
  }
  dialogVisible.value = true
}

// 上传图片前的校验
const beforeImageUpload = (file) => {
  const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png'
  const isLt5M = file.size / 1024 / 1024 < 5

  if (!isJpgOrPng) {
    ElMessage.error('广告只能是JPG或PNG格式!')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('广告大小不能超过5MB!')
    return false
  }
  return true
}

// 自定义图片上传
const customUploadImage = async (options) => {
  try {
    const { file } = options
    const formData = new FormData()
    formData.append('file', file)

    await request.post('/file/upload/img', formData, {
      headers: {
        token: localStorage.getItem('token') || ''
      },
      transformRequest: [(data) => data],
      successMsg: '图片上传成功',
      errorMsg: '图片上传失败',
      onSuccess: (data) => {
        bannerForm.imageUrl = data
        options.onSuccess()
      },
      onError: (error) => {
        console.error('图片上传错误:', error)
        options.onError(error)
      }
    })
  } catch (error) {
    options.onError(error)
  }
}

// 提交表单
const submitForm = () => {
  bannerFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const isEdit = formType.value === 'edit'
        const url = isEdit ? `/banner/${bannerForm.id}` : '/banner'
        const method = isEdit ? 'put' : 'post'
        
        await request[method](url, bannerForm, {
          successMsg: isEdit ? '更新成功' : '添加成功',
          onSuccess: () => {
            dialogVisible.value = false
            fetchBanners()
          }
        })
      } catch (error) {
        console.error('保存广告失败:', error)
      }
    }
  })
}

// 删除广告
const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该广告吗?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await request.delete(`/banner/${row.id}`, {}, {
        successMsg: '删除成功',
        onSuccess: () => {
          fetchBanners()
        }
      })
    } catch (error) {
      console.error('删除广告失败:', error)
    }
  }).catch(() => {})
}

// 更改广告状态
const changeStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  try {
    await request.put(`/banner/${row.id}/status/${newStatus}`, {}, {
      successMsg: newStatus === 1 ? '已启用' : '已禁用',
      onSuccess: () => {
        row.status = newStatus
      }
    })
  } catch (error) {
    console.error('更新广告状态失败:', error)
  }
}

onMounted(() => {
  fetchBanners()
})
</script>

<style lang="scss" scoped>
.banner-manage {
  padding: 0 20px 20px;
  font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
  
  .page-header {
    margin-bottom: 30px;
    padding: 20px 0;
    
    .title-container {
      position: relative;
      display: inline-block;
      
      .page-title {
        font-size: 24px;
        font-weight: 300;
        color: #333;
        margin: 0 0 10px;
        letter-spacing: 1px;
      }
      
      .title-decoration {
        width: 40px;
        height: 3px;
        background: #1890ff;
      }
    }
  }
  
  .search-container {
    background-color: #fff;
    padding: 24px;
    border-radius: 6px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
    margin-bottom: 20px;
    
    .search-form {
      display: flex;
      flex-wrap: wrap;
      
      .el-form-item {
        margin-right: 20px;
        margin-bottom: 0;
      }
    }
  }
  
  .action-container {
    margin-bottom: 20px;
    display: flex;
    justify-content: flex-end;
    
    .add-button {
      background-color: #1890ff;
      border-color: #1890ff;
      display: flex;
      align-items: center;
      
      &:hover, &:focus {
        background-color: #40a9ff;
        border-color: #40a9ff;
      }
      
      .el-icon {
        margin-right: 4px;
      }
    }
  }
  
  .table-container {
    background-color: #fff;
    border-radius: 6px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
    padding: 0;
    overflow: hidden;
    
    .table-actions {
      display: flex;
      gap: 8px;
      
      .el-button {
        display: flex;
        align-items: center;
        
        .el-icon {
          margin-right: 4px;
        }
      }
    }
  }
  
  .pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
  
  .banner-form {
    padding: 10px 20px;
    
    .el-form-item {
      margin-bottom: 25px;
    }
  }
  
  .banner-uploader {
    border: 1px dashed #d9d9d9;
    border-radius: 6px;
    cursor: pointer;
    position: relative;
    overflow: hidden;
    width: 360px;
    height: 220px;
    display: flex;
    justify-content: center;
    align-items: center;
    transition: border-color 0.3s;
    
    &:hover {
      border-color: #1890ff;
    }
    
    .banner-image {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    
    .banner-uploader-icon {
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;
      color: #8c939d;
      width: 100%;
      height: 100%;
      
      .el-icon {
        font-size: 40px;
        margin-bottom: 10px;
      }
      
      p {
        margin: 0;
        font-size: 14px;
      }
    }
  }
  
  .upload-tip, .form-tip {
    color: #8c939d;
    font-size: 12px;
    margin-top: 8px;
  }
  
  .dialog-footer {
    display: flex;
    justify-content: flex-end;
    
    .el-button {
      &.el-button--primary {
        background-color: #1890ff;
        border-color: #1890ff;
        
        &:hover, &:focus {
          background-color: #40a9ff;
          border-color: #40a9ff;
        }
      }
    }
  }
}
</style> 