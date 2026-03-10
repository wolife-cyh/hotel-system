<template>
  <div class="reservation-management">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>预约管理</span>
        </div>
      </template>
      
      <!-- 搜索区域 -->
      <div class="search-container">
        <el-form :inline="true" :model="searchForm" class="demo-form-inline">
          <el-form-item label="房间号">
            <el-input v-model="searchForm.roomNumber" placeholder="请输入房间号" clearable />
          </el-form-item>
          <el-form-item label="预约状态">
            <el-select v-model="searchForm.status" placeholder="请选择" clearable>
              <el-option label="待确认" :value="0" />
              <el-option label="已确认" :value="1" />
              <el-option label="已取消" :value="2" />
              <el-option label="已完成" :value="3" />
            </el-select>
          </el-form-item>
          <el-form-item label="支付状态">
            <el-select v-model="searchForm.payStatus" placeholder="请选择" clearable>
              <el-option label="未支付" :value="0" />
              <el-option label="已支付" :value="1" />
              <el-option label="已退款" :value="2" />
            </el-select>
          </el-form-item>
          <el-form-item label="入住日期">
            <el-date-picker
              v-model="searchForm.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="fetchReservations">查询</el-button>
            <el-button @click="resetSearch">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
      
      <!-- 预约列表 -->
      <el-table :data="tableData" style="width: 100%" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
<!--        <el-table-column label="用户信息" width="120">-->
<!--          <template #default="scope">-->
<!--            {{ scope.row.user ? scope.row.user.username : '未知' }}-->
<!--          </template>-->
<!--        </el-table-column>-->
        <el-table-column label="房间信息" width="120">
          <template #default="scope">
            <div v-if="scope.row.room">
              {{ scope.row.room.roomNumber }}
              <div class="room-type">{{ scope.row.room.roomType ? scope.row.room.roomType.name : '' }}</div>
            </div>
            <span v-else>未知</span>
          </template>
        </el-table-column>
        <el-table-column label="入住日期" width="120">
          <template #default="scope">
            {{ scope.row.startDate }}
          </template>
        </el-table-column>
        <el-table-column label="退房日期" width="120">
          <template #default="scope">
            {{ scope.row.endDate }}
          </template>
        </el-table-column>
        <el-table-column label="预约状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ scope.row.statusName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="支付状态" width="100">
          <template #default="scope">
            <el-tag :type="getPayStatusType(scope.row.payStatus)">
              {{ scope.row.payStatusName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="总价" width="100">
          <template #default="scope">
            ¥{{ scope.row.price ? scope.row.price.toFixed(2) : '0.00' }}
          </template>
        </el-table-column>
        <el-table-column label="入住人" width="100">
          <template #default="scope">
            {{ scope.row.guestName }}
          </template>
        </el-table-column>
        <el-table-column label="联系电话" width="120">
          <template #default="scope">
            {{ scope.row.guestPhone }}
          </template>
        </el-table-column>
        <el-table-column prop="notes" label="备注" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" fixed="right" width="320">
          <template #default="scope">
            <el-button 
              type="primary" 
              size="small" 
              @click="handleViewDetail(scope.row)">
              详情
            </el-button>
            <el-button 
              type="success" 
              size="small" 
              v-if="scope.row.status === 0" 
              @click="handleUpdateStatus(scope.row, 1)">
              确认
            </el-button>
            <el-button 
              type="success" 
              size="small" 
              v-if="scope.row.status === 0 && scope.row.payStatus === 1" 
              @click="handleUpdateStatus(scope.row, 1)"
              class="highlight-button">
              确认已付订单
            </el-button>
            <el-button 
              type="warning" 
              size="small" 
              v-if="scope.row.status === 1" 
              @click="handleUpdateStatus(scope.row, 3)">
              完成
            </el-button>
            <el-button 
              type="danger" 
              size="small" 
              v-if="scope.row.status !== 2 && scope.row.status !== 3" 
              @click="handleCancel(scope.row)">
              取消
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
      
      <!-- 预约详情对话框 -->
      <el-dialog v-model="dialogVisible" title="预约详情" width="650px">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="预约ID">{{ currentReservation.id }}</el-descriptions-item>
          <el-descriptions-item label="用户名">{{ currentReservation.user ? currentReservation.user.username : '未知' }}</el-descriptions-item>
          <el-descriptions-item label="房间号">{{ currentReservation.room ? currentReservation.room.roomNumber : '未知' }}</el-descriptions-item>
          <el-descriptions-item label="房间类型">{{ currentReservation.room && currentReservation.room.roomType ? currentReservation.room.roomType.name : '未知' }}</el-descriptions-item>
          <el-descriptions-item label="入住日期">{{ currentReservation.startDate }}</el-descriptions-item>
          <el-descriptions-item label="退房日期">{{ currentReservation.endDate }}</el-descriptions-item>
          <el-descriptions-item label="预约状态">
            <el-tag :type="getStatusType(currentReservation.status)">{{ currentReservation.statusName }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="支付状态">
            <el-tag :type="getPayStatusType(currentReservation.payStatus)">{{ currentReservation.payStatusName }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="总价">¥{{ currentReservation.price ? currentReservation.price.toFixed(2) : '0.00' }}</el-descriptions-item>
          <el-descriptions-item label="入住人数">{{ currentReservation.guestCount }} 人</el-descriptions-item>
          <el-descriptions-item label="入住人">{{ currentReservation.guestName }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ currentReservation.guestPhone }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ currentReservation.notes }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ currentReservation.createTime }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ currentReservation.updateTime }}</el-descriptions-item>
        </el-descriptions>
        
        <div class="operation-container" v-if="currentReservation.status !== 2 && currentReservation.status !== 3">
          <el-divider content-position="center">操作</el-divider>
          <div class="operation-buttons">
            <el-button 
              type="success" 
              v-if="currentReservation.status === 0" 
              @click="handleUpdateStatus(currentReservation, 1)">
              确认预约
            </el-button>
            <el-button 
              type="success" 
              v-if="currentReservation.status === 0 && currentReservation.payStatus === 1" 
              @click="handleUpdateStatus(currentReservation, 1)"
              class="highlight-button">
              确认已付订单
            </el-button>
            <el-button 
              type="warning" 
              v-if="currentReservation.status === 1" 
              @click="handleUpdateStatus(currentReservation, 3)">
              完成预约
            </el-button>
            <el-button 
              type="danger" 
              @click="handleCancel(currentReservation)">
              取消预约
            </el-button>
<!--            <el-button -->
<!--              type="primary" -->
<!--              v-if="currentReservation.payStatus === 0" -->
<!--              @click="handleCreateOrder(currentReservation)">-->
<!--              创建订单-->
<!--            </el-button>-->
            <el-button 
              type="danger" 
              v-if="currentReservation.payStatus === 1" 
              @click="handleRefund(currentReservation)">
              退款
            </el-button>
          </div>
        </div>
        
        <template #footer>
          <span class="dialog-footer">
            <el-button @click="dialogVisible = false">关闭</el-button>
          </span>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

// 数据列表
const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 搜索表单
const searchForm = reactive({
  roomNumber: '',
  status: null,
  payStatus: null,
  dateRange: null
})

// 预约详情对话框
const dialogVisible = ref(false)
const currentReservation = ref({})

// 获取预约列表
const fetchReservations = async () => {
  loading.value = true
  try {
    const params = {
      currentPage: currentPage.value,
      size: pageSize.value,
      status: searchForm.status,
      payStatus: searchForm.payStatus
    }
    
    // 处理日期范围
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.startDate = searchForm.dateRange[0]
      params.endDate = searchForm.dateRange[1]
    }
    
    // 如果输入了房间号，直接传递给后端
    if (searchForm.roomNumber) {
      params.roomNumber = searchForm.roomNumber
    }
    
    await request.get('/reservation/page', 
      params,
      {
      onSuccess: (res) => {
        tableData.value = res.records
        total.value = res.total
      }
    })
  } catch (error) {
    console.error('获取预约列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 重置搜索
const resetSearch = () => {
  searchForm.roomNumber = ''
  searchForm.status = null
  searchForm.payStatus = null
  searchForm.dateRange = null
  fetchReservations()
}

// 处理分页大小变化
const handleSizeChange = (val) => {
  pageSize.value = val
  fetchReservations()
}

// 处理页码变化
const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchReservations()
}

// 查看预约详情
const handleViewDetail = (row) => {
  currentReservation.value = { ...row }
  dialogVisible.value = true
}

// 更新预约状态
const handleUpdateStatus = async (reservation, status) => {
  const statusText = {
    0: '待确认',
    1: '已确认',
    2: '已取消',
    3: '已完成'
  }[status]
  
  try {
    await request.put(`/reservation/status/${reservation.id}?status=${status}`, null, {
      successMsg: `预约状态已更新为${statusText}`,
      onSuccess: () => {
        fetchReservations()
        if (dialogVisible.value) {
          // 如果在详情页面，需要更新当前预约信息
          currentReservation.value.status = status
          currentReservation.value.statusName = statusText
        }
      }
    })
  } catch (error) {
    console.error('更新预约状态失败:', error)
  }
}

// 取消预约
const handleCancel = (reservation) => {
  ElMessageBox.confirm(
    '确定要取消此预约吗？',
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await request.put(`/reservation/cancel/${reservation.id}`, null, {
        successMsg: '预约已取消',
        onSuccess: () => {
          fetchReservations()
          if (dialogVisible.value) {
            // 如果在详情页面，需要更新当前预约信息
            currentReservation.value.status = 2
            currentReservation.value.statusName = '已取消'
          }
        }
      })
    } catch (error) {
      console.error('取消预约失败:', error)
    }
  }).catch(() => {})
}

// 创建订单
const handleCreateOrder = (reservation) => {
  ElMessageBox.confirm(
    '确定要为此预约创建订单吗？',
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    }
  ).then(async () => {
    try {
      await request.post('/order/create', { reservationId: reservation.id }, {
        successMsg: '订单创建成功',
        onSuccess: () => {
          fetchReservations()
        }
      })
    } catch (error) {
      console.error('创建订单失败:', error)
    }
  }).catch(() => {})
}

// 处理退款
const handleRefund = (reservation) => {
  ElMessageBox.confirm(
    '确定要为此预约办理退款吗？',
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      // 先查询订单ID
      await request.get('/reservation/page', {
        params: {
          reservationId: reservation.id,
          currentPage: 1,
          size: 1
        },
        onSuccess: async (res) => {
          if (res.records && res.records.length > 0 && res.records[0].order) {
            const orderId = res.records[0].order.id
            
            // 执行退款
            await request.put(`/order/refund/${orderId}`, null, {
              successMsg: '退款成功',
              onSuccess: () => {
                fetchReservations()
                if (dialogVisible.value) {
                  // 如果在详情页面，需要更新当前预约信息
                  currentReservation.value.payStatus = 2
                  currentReservation.value.payStatusName = '已退款'
                }
              }
            })
          } else {
            ElMessage.error('未找到关联订单')
          }
        }
      })
    } catch (error) {
      console.error('退款失败:', error)
    }
  }).catch(() => {})
}

// 获取状态对应的类型
const getStatusType = (status) => {
  switch (status) {
    case 0: return 'info'    // 待确认
    case 1: return 'success' // 已确认
    case 2: return 'danger'  // 已取消
    case 3: return 'warning' // 已完成
    default: return 'info'
  }
}

// 获取支付状态对应的类型
const getPayStatusType = (payStatus) => {
  switch (payStatus) {
    case 0: return 'info'    // 未支付
    case 1: return 'success' // 已支付
    case 2: return 'warning' // 已退款
    default: return 'info'
  }
}

// 生命周期钩子
onMounted(() => {
  fetchReservations()
})
</script>

<style lang="scss" scoped>
.reservation-management {
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
  
  .room-type {
    font-size: 12px;
    color: #909399;
  }
  
  .operation-container {
    margin-top: 20px;
    
    .operation-buttons {
      display: flex;
      flex-wrap: wrap;
      gap: 10px;
      justify-content: center;
      margin-top: 15px;
    }
  }
  
  .highlight-button {
    background-color: #67c23a !important;
    border-color: #67c23a !important;
    color: #fff !important;
    font-weight: bold;
    position: relative;
    animation: pulse 2s infinite;
    box-shadow: 0 0 5px rgba(103, 194, 58, 0.5);
    
    &:hover, &:focus {
      background-color: #85ce61 !important;
      border-color: #85ce61 !important;
      animation: none;
      box-shadow: 0 0 8px rgba(103, 194, 58, 0.8);
    }
  }
  
  @keyframes pulse {
    0% {
      box-shadow: 0 0 0 0 rgba(103, 194, 58, 0.7);
    }
    70% {
      box-shadow: 0 0 0 10px rgba(103, 194, 58, 0);
    }
    100% {
      box-shadow: 0 0 0 0 rgba(103, 194, 58, 0);
    }
  }
}
</style> 