<template>
  <div class="room-management">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>房间管理</span>
          <div class="button-group">
            <el-button type="success" @click="handleBatchAdd">批量添加房间</el-button>
            <el-button type="primary" @click="handleAddRoom">添加房间</el-button>
          </div>
        </div>
      </template>
      
      <!-- 搜索区域 -->
      <div class="search-container">
        <el-form :inline="true" :model="searchForm" class="demo-form-inline">
          <el-form-item label="房间号">
            <el-input v-model="searchForm.roomNumber" placeholder="请输入房间号" clearable />
          </el-form-item>
          <el-form-item label="房间类型">
            <el-select v-model="searchForm.roomTypeId" placeholder="请选择" clearable>
              <el-option v-for="item in roomTypes" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="楼层">
            <el-select v-model="searchForm.floor" placeholder="请选择" clearable>
              <el-option v-for="i in 10" :key="i" :label="`${i}楼`" :value="i" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="searchForm.status" placeholder="请选择" clearable>
              <el-option label="可用" :value="1" />
              <el-option label="维护中" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="fetchRooms">查询</el-button>
            <el-button @click="resetSearch">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
      
      <!-- 房间列表 -->
      <el-table :data="tableData" style="width: 100%" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="roomNumber" label="房间号" width="120" />
        <el-table-column label="房间类型" width="120">
          <template #default="scope">
            {{ scope.row.roomType ? scope.row.roomType.name : '未知' }}
          </template>
        </el-table-column>
        <el-table-column label="价格(元/晚)" width="120">
          <template #default="scope">
            {{ scope.row.price ? scope.row.price.toFixed(2) : '0.00' }}
          </template>
        </el-table-column>
        <el-table-column prop="floor" label="楼层" width="80">
          <template #default="scope">
            {{ scope.row.floor }}楼
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
              {{ scope.row.statusName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="图片" width="100">
          <template #default="scope">
            <el-image 
              v-if="scope.row.roomType && scope.row.roomType.image"
              style="width: 80px; height: 50px" 
              :src="getImageUrl(scope.row.roomType.image)" 
              :preview-src-list="[getImageUrl(scope.row.roomType.image)]"
              fit="cover"
              :preview-teleported="true"
            >
              <template #error>
                <div class="image-error">
                  <el-icon><Picture /></el-icon>
                </div>
              </template>
            </el-image>
            <div v-else class="image-error" style="width: 80px; height: 50px">
              <el-icon><Picture /></el-icon>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
<!--        <el-table-column prop="createTime" label="创建时间" width="180" />-->
        <el-table-column label="操作" fixed="right" width="250">
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
              v-if="scope.row.status === 1" 
              @click="handleUpdateStatus(scope.row, 0)">
              维护
            </el-button>
            <el-button 
              type="danger" 
              size="small" 
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
      
      <!-- 房间表单对话框 -->
      <el-dialog v-model="dialogVisible" :title="dialogType === 'add' ? '添加房间' : '编辑房间'" width="500px">
        <el-form :model="roomForm" :rules="rules" ref="roomFormRef" label-width="100px">
          <el-form-item label="房间号" prop="roomNumber">
            <el-input v-model="roomForm.roomNumber" :disabled="dialogType === 'edit'" />
          </el-form-item>
          <el-form-item label="房间类型" prop="roomTypeId">
            <el-select v-model="roomForm.roomTypeId" placeholder="请选择房间类型">
              <el-option v-for="item in roomTypes" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="楼层" prop="floor">
            <el-input-number v-model="roomForm.floor" :min="1" :max="50" />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-radio-group v-model="roomForm.status">
              <el-radio :label="1">可用</el-radio>
              <el-radio :label="0">维护中</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="描述" prop="description">
            <el-input type="textarea" v-model="roomForm.description" :rows="3" />
          </el-form-item>
        </el-form>
        <template #footer>
          <span class="dialog-footer">
            <el-button @click="dialogVisible = false">取消</el-button>
            <el-button type="primary" @click="submitForm" :loading="submitLoading">确定</el-button>
          </span>
        </template>
      </el-dialog>
      
      <!-- 批量添加房间对话框 -->
      <el-dialog v-model="batchDialogVisible" title="批量添加房间" width="500px">
        <el-form :model="batchForm" :rules="batchRules" ref="batchFormRef" label-width="140px">
          <el-form-item label="房间类型" prop="roomTypeId">
            <el-select v-model="batchForm.roomTypeId" placeholder="请选择房间类型">
              <el-option v-for="item in roomTypes" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="楼层" prop="floor">
            <el-input-number v-model="batchForm.floor" :min="1" :max="50" />
          </el-form-item>
          <el-form-item label="房间号前缀" prop="roomNumberPrefix">
            <el-input v-model="batchForm.roomNumberPrefix" placeholder="例如：A-" />
          </el-form-item>
          <el-form-item label="起始编号" prop="startNumber">
            <el-input-number v-model="batchForm.startNumber" :min="0" />
          </el-form-item>
          <el-form-item label="房间数量" prop="count">
            <el-input-number v-model="batchForm.count" :min="1" :max="50" />
          </el-form-item>
          <el-form-item label="预览">
            <el-alert
              title="将会添加的房间号"
              type="info"
              :closable="false"
            >
              <div class="preview-list">
                <span v-for="(room, index) in previewRoomNumbers" :key="index" class="preview-item">
                  {{ room }}
                </span>
              </div>
            </el-alert>
          </el-form-item>
        </el-form>
        <template #footer>
          <span class="dialog-footer">
            <el-button @click="batchDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="submitBatchForm" :loading="submitLoading">确定</el-button>
          </span>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Picture } from '@element-plus/icons-vue'
import request from '@/utils/request'

// 数据列表
const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const submitLoading = ref(false)
const roomTypes = ref([])

// 搜索表单
const searchForm = reactive({
  roomNumber: '',
  roomTypeId: null,
  status: null,
  floor: null
})

// 房间表单
const dialogVisible = ref(false)
const dialogType = ref('add') // 'add' 或 'edit'
const roomFormRef = ref(null)
const roomForm = reactive({
  id: null,
  roomNumber: '',
  roomTypeId: null,
  status: 1,
  floor: 1,
  description: ''
})

// 批量添加表单
const batchDialogVisible = ref(false)
const batchFormRef = ref(null)
const batchForm = reactive({
  roomTypeId: null,
  floor: 1,
  roomNumberPrefix: '',
  startNumber: 1,
  count: 1
})

// 预览房间号
const previewRoomNumbers = computed(() => {
  const result = []
  if (batchForm.roomNumberPrefix !== null && batchForm.startNumber !== null && batchForm.count !== null) {
    for (let i = 0; i < batchForm.count; i++) {
      result.push(batchForm.roomNumberPrefix + (batchForm.startNumber + i))
    }
  }
  return result
})

// 表单验证规则
const rules = {
  roomNumber: [
    { required: true, message: '请输入房间号', trigger: 'blur' },
    { max: 20, message: '长度不能超过20个字符', trigger: 'blur' }
  ],
  roomTypeId: [
    { required: true, message: '请选择房间类型', trigger: 'change' }
  ],
  floor: [
    { required: true, message: '请输入楼层', trigger: 'blur' },
    { type: 'number', min: 1, message: '楼层不能小于1', trigger: 'blur' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
}

// 批量添加表单验证规则
const batchRules = {
  roomTypeId: [
    { required: true, message: '请选择房间类型', trigger: 'change' }
  ],
  floor: [
    { required: true, message: '请输入楼层', trigger: 'blur' },
    { type: 'number', min: 1, message: '楼层不能小于1', trigger: 'blur' }
  ],
  roomNumberPrefix: [
    { required: true, message: '请输入房间号前缀', trigger: 'blur' }
  ],
  startNumber: [
    { required: true, message: '请输入起始编号', trigger: 'blur' },
    { type: 'number', min: 0, message: '起始编号不能小于0', trigger: 'blur' }
  ],
  count: [
    { required: true, message: '请输入房间数量', trigger: 'blur' },
    { type: 'number', min: 1, message: '房间数量不能小于1', trigger: 'blur' }
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
  try {
    await request.get('/roomType/all', null,{
      onSuccess: (res) => {
        roomTypes.value = res
      }
    })
  } catch (error) {
    console.error('获取房型列表失败:', error)
  }
}

// 获取房间列表
const fetchRooms = async () => {
  loading.value = true
  try {
    await request.get('/room/page', {
     
        currentPage: currentPage.value,
        size: pageSize.value,
        roomNumber: searchForm.roomNumber,
        roomTypeId: searchForm.roomTypeId,
        status: searchForm.status,
        floor: searchForm.floor
      },{
      onSuccess: (res) => {
        tableData.value = res.records
        total.value = res.total
      }
    })
  } catch (error) {
    console.error('获取房间列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 重置搜索
const resetSearch = () => {
  searchForm.roomNumber = ''
  searchForm.roomTypeId = null
  searchForm.status = null
  searchForm.floor = null
  fetchRooms()
}

// 处理分页大小变化
const handleSizeChange = (val) => {
  pageSize.value = val
  fetchRooms()
}

// 处理页码变化
const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchRooms()
}

// 添加房间
const handleAddRoom = () => {
  dialogType.value = 'add'
  resetRoomForm()
  dialogVisible.value = true
}

// 编辑房间
const handleEdit = (row) => {
  dialogType.value = 'edit'
  Object.keys(roomForm).forEach(key => {
    if (key in row) {
      roomForm[key] = row[key]
    }
  })
  dialogVisible.value = true
}

// 重置房间表单
const resetRoomForm = () => {
  roomForm.id = null
  roomForm.roomNumber = ''
  roomForm.roomTypeId = null
  roomForm.status = 1
  roomForm.floor = 1
  roomForm.description = ''
  if (roomFormRef.value) {
    roomFormRef.value.resetFields()
  }
}

// 提交表单
const submitForm = async () => {
  if (!roomFormRef.value) return
  
  await roomFormRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        if (dialogType.value === 'add') {
          // 添加房间
          await request.post('/room/add', roomForm, {
            successMsg: '添加房间成功',
            onSuccess: () => {
              dialogVisible.value = false
              fetchRooms()
            }
          })
        } else {
          // 编辑房间
          await request.put(`/room/${roomForm.id}`, roomForm, {
            successMsg: '更新房间成功',
            onSuccess: () => {
              dialogVisible.value = false
              fetchRooms()
            }
          })
        }
      } finally {
        submitLoading.value = false
      }
    }
  })
}

// 删除房间
const handleDelete = (row) => {
  ElMessageBox.confirm(
    '确定要删除该房间吗？删除后无法恢复',
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    await request.delete(`/room/${row.id}`, null, {
      successMsg: '删除房间成功',
      onSuccess: () => {
        fetchRooms()
      }
    })
  }).catch(() => {})
}

// 更新房间状态
const handleUpdateStatus = async (row, status) => {
  const statusText = status === 1 ? '启用' : '设为维护中'
  
  await request.put(`/room/status/${row.id}?status=${status}`, null, {
    successMsg: `${statusText}成功`,
    onSuccess: () => {
      fetchRooms()
    }
  })
}

// 批量添加房间
const handleBatchAdd = () => {
  batchDialogVisible.value = true
}

// 提交批量添加表单
const submitBatchForm = async () => {
  if (!batchFormRef.value) return
  
  await batchFormRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        await request.post('/room/batchAdd', batchForm, {
          successMsg: '批量添加房间成功',
          onSuccess: () => {
            batchDialogVisible.value = false
            fetchRooms()
          }
        })
      } finally {
        submitLoading.value = false
      }
    }
  })
}

// 生命周期钩子
onMounted(() => {
  fetchRoomTypes()
  fetchRooms()
})
</script>

<style lang="scss" scoped>
.room-management {
  padding: 20px 0;
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .button-group {
      display: flex;
      gap: 10px;
    }
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
  
  .preview-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-top: 8px;
    
    .preview-item {
      padding: 2px 8px;
      background-color: #f0f9eb;
      border-radius: 4px;
      font-size: 12px;
      color: #67c23a;
    }
  }
}
</style> 