<template>
  <div class="room-type-management">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>新建房间管理</span>
          <el-button type="primary" @click="handleAddRoomType">添加房型</el-button>
        </div>
      </template>
      
      <!-- 搜索区域 -->
      <div class="search-container">
        <el-form :inline="true" :model="searchForm" class="demo-form-inline">
          <el-form-item label="房型名称">
            <el-input v-model="searchForm.name" placeholder="请输入房型名称" clearable />
          </el-form-item>
          <el-form-item label="价格区间">
            <el-input-number v-model="searchForm.minPrice" :min="0" placeholder="最低价" />
            <span class="mx-2">-</span>
            <el-input-number v-model="searchForm.maxPrice" :min="0" placeholder="最高价" />
          </el-form-item>
          <el-form-item label="最大入住人数">
            <el-select v-model="searchForm.maxPeople" placeholder="请选择" clearable>
              <el-option v-for="i in 6" :key="i" :label="i + '人'" :value="i" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="fetchRoomTypes">查询</el-button>
            <el-button @click="resetSearch">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
      
      <!-- 房型列表 -->
      <el-table :data="tableData" style="width: 100%" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="房型名称" width="120" />
        <el-table-column prop="price" label="价格(元/晚)" width="120">
          <template #default="scope">
            {{ scope.row.price.toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="maxPeople" label="最大入住人数" width="120">
          <template #default="scope">
            {{ scope.row.maxPeople }}人
          </template>
        </el-table-column>
        <el-table-column prop="bedType" label="床型" width="120" />
        <el-table-column label="可用房间数量" width="120">
          <template #default="scope">
            <el-tooltip content="可用房间数/总房间数" placement="top">
              <span>{{ scope.row.availableRoomCount }}/{{ scope.row.roomCount }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="图片" width="100">
          <template #default="scope">
            <el-image 
              style="width: 80px; height: 50px" 
              :src="getImageUrl(scope.row.image)" 
              :preview-teleported="true"
              :preview-src-list="[getImageUrl(scope.row.image)]"
              fit="cover"
            >
              <template #error>
                <div class="image-error">
                  <el-icon><Picture /></el-icon>
                </div>
              </template>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column prop="facilities" label="设施" width="200" show-overflow-tooltip />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
<!--        <el-table-column prop="createTime" label="创建时间" width="180" />-->
        <el-table-column label="操作" fixed="right" width="200">
          <template #default="scope">
            <el-button type="primary" size="small" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(scope.row)" :disabled="scope.row.roomCount > 0">删除</el-button>
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
      
      <!-- 房型表单对话框 -->
      <el-dialog v-model="dialogVisible" :title="dialogType === 'add' ? '添加房型' : '编辑房型'" width="650px">
        <el-form :model="roomTypeForm" :rules="rules" ref="roomTypeFormRef" label-width="120px">
          <el-form-item label="房型名称" prop="name">
            <el-input v-model="roomTypeForm.name" />
          </el-form-item>
          <el-form-item label="价格(元/晚)" prop="price">
            <el-input-number v-model="roomTypeForm.price" :min="0" :precision="2" :step="10" />
          </el-form-item>
          <el-form-item label="最大入住人数" prop="maxPeople">
            <el-input-number v-model="roomTypeForm.maxPeople" :min="1" :max="10" />
          </el-form-item>
          <el-form-item label="床型" prop="bedType">
            <el-input v-model="roomTypeForm.bedType" placeholder="例如：双人床1.8m" />
          </el-form-item>
          <el-form-item label="设施" prop="facilities">
            <el-input type="textarea" v-model="roomTypeForm.facilities" placeholder="例如：空调、电视、WiFi、独立卫浴" :rows="2" />
          </el-form-item>
          <el-form-item label="描述" prop="description">
            <el-input type="textarea" v-model="roomTypeForm.description" :rows="3" />
          </el-form-item>
          <el-form-item label="图片" prop="image">
            <el-upload
              class="room-type-uploader"
              action="#"
              :auto-upload="true"
              
              :show-file-list="false"
              :http-request="customUploadImage"
              :before-upload="beforeImageUpload"
            >
              <img v-if="roomTypeForm.image" :src="getImageUrl(roomTypeForm.image)" class="room-image" />
              <el-icon v-else class="room-type-uploader-icon"><Plus /></el-icon>
            </el-upload>
            <div class="upload-tip">
              <span></span>
            </div>
          </el-form-item>
          
          <!-- 多图片管理 -->
          <el-form-item label="附加图片" v-if="dialogType === 'edit' && roomTypeForm.id">
            <div class="additional-images">
              <div v-if="additionalImages.length > 0" class="image-list">
                <div v-for="(image, index) in additionalImages" :key="index" class="image-item">
                  <el-image :src="getImageUrl(image.imageUrl)" class="additional-image" fit="cover">
                    <template #error>
                      <div class="image-error">
                        <el-icon><Picture /></el-icon>
                      </div>
                    </template>
                  </el-image>
                  <div class="image-actions">
                    <el-button 
                      type="primary" 
                      size="small" 
                      circle 
                      @click="setAsMainImage(image)"
                      :disabled="image.isMain === 1"
                      title="设为主图"
                    >
                      <el-icon><Star /></el-icon>
                    </el-button>
                    <el-button 
                      type="danger" 
                      size="small" 
                      circle 
                      @click="deleteImage(image)"
                      title="删除图片"
                    >
                      <el-icon><Delete /></el-icon>
                    </el-button>
                  </div>
                  <div class="image-badge" v-if="image.isMain === 1">主图</div>
                </div>
              </div>
              <div class="image-uploader">
                <el-upload
                  action="#"
                  :auto-upload="true"
                  :show-file-list="false"
                  :http-request="uploadAdditionalImage"
                  :before-upload="beforeImageUpload"
                >
                  <div class="upload-box">
                    <el-icon class="upload-icon"><Plus /></el-icon>
                    <span>添加图片</span>
                  </div>
                </el-upload>
              </div>
            </div>
            <div class="upload-tip">
              <span></span>
            </div>
          </el-form-item>
        </el-form>
        <template #footer>
          <span class="dialog-footer">
            <el-button @click="dialogVisible = false">取消</el-button>
            <el-button type="primary" @click="submitForm" :loading="submitLoading">确定</el-button>
          </span>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Picture, Star, Delete } from '@element-plus/icons-vue'
import request from '@/utils/request'

// 数据列表
const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const submitLoading = ref(false)
const additionalImages = ref([]) // 附加图片列表

// 搜索表单
const searchForm = reactive({
  name: '',
  minPrice: null,
  maxPrice: null,
  maxPeople: null
})

// 房型表单
const dialogVisible = ref(false)
const dialogType = ref('add') // 'add' 或 'edit'
const roomTypeFormRef = ref(null)
const roomTypeForm = reactive({
  id: null,
  name: '',
  price: 0,
  maxPeople: 1,
  bedType: '',
  facilities: '',
  description: '',
  image: ''
})

// 表单验证规则
const rules = {
  name: [
    { required: true, message: '请输入房型名称', trigger: 'blur' },
    { max: 50, message: '长度不能超过50个字符', trigger: 'blur' }
  ],
  price: [
    { required: true, message: '请输入价格', trigger: 'blur' },
    { type: 'number', min: 0, message: '价格不能小于0', trigger: 'blur' }
  ],
  maxPeople: [
    { required: true, message: '请输入最大入住人数', trigger: 'blur' },
    { type: 'number', min: 1, message: '最大入住人数不能小于1', trigger: 'blur' }
  ],
  bedType: [
    { required: true, message: '请输入床型', trigger: 'blur' },
    { max: 50, message: '长度不能超过50个字符', trigger: 'blur' }
  ]
}

// 获取图片URL
const baseAPI = process.env.VUE_APP_BASE_API || '/api'
const getImageUrl = (image) => {
  if (!image) return ''
  if (image.startsWith('http')) return image
  return baseAPI + image
}

// 获取房型列表
const fetchRoomTypes = async () => {
  loading.value = true
  try {
    await request.get('/roomType/page', {
      
        currentPage: currentPage.value,
        size: pageSize.value,
        name: searchForm.name,
        minPrice: searchForm.minPrice,
        maxPrice: searchForm.maxPrice,
        maxPeople: searchForm.maxPeople
      },{
      onSuccess: (res) => {
        tableData.value = res.records
        total.value = res.total
      }
    })
  } catch (error) {
    console.error('获取房型列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 重置搜索
const resetSearch = () => {
  searchForm.name = ''
  searchForm.minPrice = null
  searchForm.maxPrice = null
  searchForm.maxPeople = null
  fetchRoomTypes()
}

// 处理分页大小变化
const handleSizeChange = (val) => {
  pageSize.value = val
  fetchRoomTypes()
}

// 处理页码变化
const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchRoomTypes()
}

// 添加房型
const handleAddRoomType = () => {
  dialogType.value = 'add'
  resetRoomTypeForm()
  dialogVisible.value = true
}

// 编辑房型
const handleEdit = (row) => {
  dialogType.value = 'edit'
  Object.keys(roomTypeForm).forEach(key => {
    if (key in row) {
      roomTypeForm[key] = row[key]
    }
  })
  dialogVisible.value = true
  
  // 加载附加图片
  if (row.id) {
    loadAdditionalImages(row.id)
  }
}

// 加载附加图片
const loadAdditionalImages = async (roomTypeId) => {
  try {
    await request.get(`/roomType/${roomTypeId}`, null, {
      onSuccess: (res) => {
        if (res.images && Array.isArray(res.images)) {
          additionalImages.value = res.images
        } else {
          additionalImages.value = []
        }
      }
    })
  } catch (error) {
    console.error('加载房型图片失败:', error)
    additionalImages.value = []
  }
}

// 设置为主图
const setAsMainImage = async (image) => {
  try {
    await request.put(`/roomTypeImage/setMain/${image.id}`, null, {
      successMsg: '设置主图成功',
      onSuccess: () => {
        // 更新主图
        roomTypeForm.image = image.imageUrl
        // 重新加载图片列表
        loadAdditionalImages(roomTypeForm.id)
      }
    })
  } catch (error) {
    console.error('设置主图失败:', error)
  }
}

// 删除图片
const deleteImage = async (image) => {
  ElMessageBox.confirm('确定要删除这张图片吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await request.delete(`/roomTypeImage/${image.id}`, null, {
        successMsg: '删除图片成功',
        onSuccess: () => {
          // 如果删除的是主图，更新主图
          if (image.isMain === 1) {
            roomTypeForm.image = ''
          }
          // 重新加载图片列表
          loadAdditionalImages(roomTypeForm.id)
        }
      })
    } catch (error) {
      console.error('删除图片失败:', error)
    }
  }).catch(() => {})
}

// 上传附加图片
const uploadAdditionalImage = async (options) => {
  try {
    const { file } = options
    
    // 创建 FormData 对象
    const formData = new FormData()
    formData.append('file', file)
    
    // 设置自定义上传选项
    const uploadOptions = {
      headers: {
        token: localStorage.getItem('token') || '',
      },
      // 不进行JSON处理
      transformRequest: [(data) => data],
      // 自定义成功消息
      successMsg: '图片上传成功',
      // 自定义错误消息
      errorMsg: '图片上传失败',
      // 成功回调
      onSuccess: async (imageUrl) => {
        // 保存图片到房型
        if (roomTypeForm.id) {
          try {
            const imageData = {
              roomTypeId: roomTypeForm.id,
              imageUrl: imageUrl
            }
            await request.post('/roomTypeImage/add', imageData, {
              onSuccess: () => {
                // 重新加载图片列表
                loadAdditionalImages(roomTypeForm.id)
                options.onSuccess({ imageUrl })
              }
            })
          } catch (error) {
            console.error('保存图片失败:', error)
            options.onError(new Error(error.message || '保存失败'))
          }
        }
      },
      // 错误回调
      onError: (error) => {
        console.error('图片上传错误:', error)
        options.onError(new Error(error.message || '上传失败'))
      },
    }
    
    // 发送上传请求
    await request.post('/file/upload/img', formData, uploadOptions)
  } catch (error) {
    options.onError(error)
    console.error('图片上传过程发生错误:', error)
  }
}

// 重置房型表单
const resetRoomTypeForm = () => {
  roomTypeForm.id = null
  roomTypeForm.name = ''
  roomTypeForm.price = 0
  roomTypeForm.maxPeople = 1
  roomTypeForm.bedType = ''
  roomTypeForm.facilities = ''
  roomTypeForm.description = ''
  roomTypeForm.image = ''
  if (roomTypeFormRef.value) {
    roomTypeFormRef.value.resetFields()
  }
}

// 提交表单
const submitForm = async () => {
  if (!roomTypeFormRef.value) return
  
  await roomTypeFormRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        if (dialogType.value === 'add') {
          // 添加房型
          await request.post('/roomType/add', roomTypeForm, {
            successMsg: '添加房型成功',
            onSuccess: () => {
              dialogVisible.value = false
              fetchRoomTypes()
            }
          })
        } else {
          // 编辑房型
          await request.put(`/roomType/${roomTypeForm.id}`, roomTypeForm, {
            successMsg: '更新房型成功',
            onSuccess: () => {
              dialogVisible.value = false
              fetchRoomTypes()
            }
          })
        }
      } finally {
        submitLoading.value = false
      }
    }
  })
}

// 删除房型
const handleDelete = (row) => {
  if (row.roomCount > 0) {
    ElMessage.warning('该房型下有关联的房间，无法删除')
    return
  }
  
  ElMessageBox.confirm(
    '确定要删除该房型吗？删除后无法恢复',
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    await request.delete(`/roomType/${row.id}`, null, {
      successMsg: '删除房型成功',
      onSuccess: () => {
        fetchRoomTypes()
      }
    })
  }).catch(() => {})
}

// 图片上传前的校验
const beforeImageUpload = (file) => {
  const isJPG = file.type === 'image/jpeg'
  const isPNG = file.type === 'image/png'
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isJPG && !isPNG) {
    ElMessage.error('图片只能是 JPG 或 PNG 格式!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB!')
    return false
  }
  return true
}

// 自定义图片上传方法
const customUploadImage = async (options) => {
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
      successMsg: '图片上传成功',
      // 自定义错误消息
      errorMsg: '图片上传失败',
      // 成功回调
      onSuccess: async (data) => {
        // 更新房型图片
        roomTypeForm.image = data

        // 通知上传成功
        options.onSuccess({ data })
      },
      // 错误回调
      onError: (error) => {
        console.error('图片上传错误:', error)
        options.onError(new Error(error.message || '上传失败'))
      }
    }

    // 发送上传请求
    await request.post('/file/upload/img', formData, uploadOptions)
  } catch (error) {
    options.onError(error)
    console.error('图片上传过程发生错误:', error)
  }
}

// 生命周期钩子
onMounted(() => {
  fetchRoomTypes()
})
</script>

<style lang="scss" scoped>
.room-type-management {
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
  
  .mx-2 {
    margin: 0 8px;
  }
  
  .room-type-uploader {
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
  
  .room-type-uploader-icon {
    font-size: 28px;
    color: #8c939d;
    width: 180px;
    height: 120px;
    text-align: center;
    line-height: 120px;
  }
  
  .room-image {
    width: 180px;
    height: 120px;
    display: block;
    object-fit: cover;
  }
  
  .upload-tip {
    color: #909399;
    font-size: 12px;
    margin-top: 5px;
  }
  
  .image-error {
    display: flex;
    justify-content: center;
    align-items: center;
    width: 100%;
    height: 100%;
    background-color: #f5f7fa;
    color: #909399;
    font-size: 20px;
  }
  
  .additional-images {
    display: flex;
    flex-wrap: wrap;
    gap: 15px;
    
    .image-list {
      display: flex;
      flex-wrap: wrap;
      gap: 15px;
    }
    
    .image-item {
      position: relative;
      width: 150px;
      height: 100px;
      border: 1px solid #dcdfe6;
      border-radius: 4px;
      overflow: hidden;
      
      &:hover .image-actions {
        opacity: 1;
      }
      
      .additional-image {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }
      
      .image-actions {
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background-color: rgba(0, 0, 0, 0.5);
        display: flex;
        justify-content: center;
        align-items: center;
        gap: 10px;
        opacity: 0;
        transition: opacity 0.3s;
      }
      
      .image-badge {
        position: absolute;
        top: 5px;
        right: 5px;
        background-color: #409EFF;
        color: white;
        padding: 2px 6px;
        border-radius: 3px;
        font-size: 12px;
      }
    }
    
    .image-uploader {
      width: 150px;
      height: 100px;
      
      .upload-box {
        display: flex;
        flex-direction: column;
        justify-content: center;
        align-items: center;
        width: 100%;
        height: 100%;
        border: 1px dashed #d9d9d9;
        border-radius: 4px;
        cursor: pointer;
        
        &:hover {
          border-color: #409EFF;
          color: #409EFF;
        }
        
        .upload-icon {
          font-size: 28px;
          margin-bottom: 5px;
        }
      }
    }
  }
}
</style> 