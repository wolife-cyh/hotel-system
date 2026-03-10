<template>
  <div class="order-management">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>订单管理</span>
        </div>
      </template>
      
      <!-- 搜索区域 -->
      <div class="search-container">
        <el-form :inline="true" :model="searchForm" class="demo-form-inline">
          <el-form-item label="订单号">
            <el-input v-model="searchForm.orderNo" placeholder="请输入订单号" clearable />
          </el-form-item>
          <el-form-item label="用户名">
            <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable />
          </el-form-item>
          <el-form-item label="订单状态">
            <el-select v-model="searchForm.status" placeholder="请选择" clearable>
              <el-option label="未支付" :value="0" />
              <el-option label="已支付" :value="1" />
              <el-option label="已取消" :value="2" />
              <el-option label="已退款" :value="3" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="fetchOrders">查询</el-button>
            <el-button @click="resetSearch">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
      
      <!-- 统计信息 -->
      <div class="stats-container">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-card shadow="hover" class="stats-card">
              <div class="stats-item">
                <div class="stats-title">总订单金额</div>
                <div class="stats-value">￥{{ formatAmount(totalAmount) }}</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover" class="stats-card">
              <div class="stats-item">
                <div class="stats-title">已支付金额</div>
                <div class="stats-value primary">￥{{ formatAmount(paidAmount) }}</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover" class="stats-card">
              <div class="stats-item">
                <div class="stats-title">已退款金额</div>
                <div class="stats-value warning">￥{{ formatAmount(refundAmount) }}</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover" class="stats-card">
              <div class="stats-item">
                <div class="stats-title">订单总数</div>
                <div class="stats-value">{{ total }}</div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
      
      <!-- 订单列表 -->
      <el-table :data="tableData" style="width: 100%" v-loading="loading" border stripe>
        <el-table-column prop="orderNo" label="订单号" width="200" />
        <el-table-column label="用户信息" width="120">
          <template #default="scope">
            {{ scope.row.user ? scope.row.user.username : '未知' }}
          </template>
        </el-table-column>
        <el-table-column label="预约信息" width="160">
          <template #default="scope">
            <div v-if="scope.row.reservation && scope.row.reservation.room">
              {{ scope.row.reservation.room.roomNumber }}
              <div class="room-type">
                {{ scope.row.reservation.room.roomType ? scope.row.reservation.room.roomType.name : '' }}
              </div>
            </div>
            <span v-else>未知</span>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="100">
          <template #default="scope">
            <span class="amount">￥{{ formatAmount(scope.row.amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ scope.row.statusName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="支付方式" width="120">
          <template #default="scope">
            {{ scope.row.payMethod || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间"  />
        <el-table-column prop="payTime" label="支付时间" width="180">
          <template #default="scope">
            {{ scope.row.payTime || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="260">
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
              @click="handlePay(scope.row)">
              支付
            </el-button>
            <el-button 
              type="warning" 
              size="small" 
              v-if="scope.row.status === 0" 
              @click="handleCancel(scope.row)">
              取消
            </el-button>
            <el-button 
              type="danger" 
              size="small" 
              v-if="scope.row.status === 1" 
              @click="handleRefund(scope.row)">
              退款
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
      
      <!-- 订单详情对话框 -->
      <el-dialog v-model="detailDialogVisible" title="订单详情" width="650px">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单ID">{{ currentOrder.id }}</el-descriptions-item>
          <el-descriptions-item label="订单号">{{ currentOrder.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="用户名">{{ currentOrder.user ? currentOrder.user.username : '未知' }}</el-descriptions-item>
          <el-descriptions-item label="订单金额">￥{{ formatAmount(currentOrder.amount) }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <el-tag :type="getStatusType(currentOrder.status)">
              {{ currentOrder.statusName }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="支付方式">{{ currentOrder.payMethod || '-' }}</el-descriptions-item>
          <el-descriptions-item label="支付流水号">{{ currentOrder.payNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="支付时间">{{ currentOrder.payTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ currentOrder.createTime }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ currentOrder.updateTime }}</el-descriptions-item>
        </el-descriptions>
        
        <div v-if="currentOrder.reservation" class="reservation-details">
          <el-divider content-position="center">预约信息</el-divider>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="预约ID">{{ currentOrder.reservation.id }}</el-descriptions-item>
            <el-descriptions-item label="预约状态">
              <el-tag :type="getReservationStatusType(currentOrder.reservation.status)">
                {{ getReservationStatusName(currentOrder.reservation.status) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="房间号">
              {{ currentOrder.reservation.room ? currentOrder.reservation.room.roomNumber : '未知' }}
            </el-descriptions-item>
            <el-descriptions-item label="房间类型">
              {{ currentOrder.reservation.room && currentOrder.reservation.room.roomType 
                 ? currentOrder.reservation.room.roomType.name : '未知' }}
            </el-descriptions-item>
            <el-descriptions-item label="入住日期">{{ currentOrder.reservation.startDate }}</el-descriptions-item>
            <el-descriptions-item label="退房日期">{{ currentOrder.reservation.endDate }}</el-descriptions-item>
            <el-descriptions-item label="入住人">{{ currentOrder.reservation.guestName }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ currentOrder.reservation.guestPhone }}</el-descriptions-item>
            <el-descriptions-item label="备注" :span="2">{{ currentOrder.reservation.notes || '无' }}</el-descriptions-item>
          </el-descriptions>
        </div>
        
        <div class="operation-container">
          <el-divider content-position="center">操作</el-divider>
          <div class="operation-buttons">
            <el-button 
              type="success" 
              v-if="currentOrder.status === 0" 
              @click="handlePay(currentOrder)">
              支付
            </el-button>
            <el-button 
              type="warning" 
              v-if="currentOrder.status === 0" 
              @click="handleCancel(currentOrder)">
              取消
            </el-button>
            <el-button 
              type="danger" 
              v-if="currentOrder.status === 1" 
              @click="handleRefund(currentOrder)">
              退款
            </el-button>
          </div>
        </div>
        
        <template #footer>
          <span class="dialog-footer">
            <el-button @click="detailDialogVisible = false">关闭</el-button>
          </span>
        </template>
      </el-dialog>
      
      <!-- 支付对话框 -->
      <el-dialog v-model="payDialogVisible" title="支付订单" width="500px">
        <el-form :model="payForm" ref="payFormRef" label-width="100px">
          <el-form-item label="订单号">
            <span>{{ currentOrder.orderNo }}</span>
          </el-form-item>
          <el-form-item label="支付金额">
            <span class="amount">￥{{ formatAmount(currentOrder.amount) }}</span>
          </el-form-item>
          <el-form-item label="支付方式" prop="payMethod">
            <el-select v-model="payForm.payMethod" placeholder="请选择支付方式">
              <el-option label="微信支付" value="微信支付" />
              <el-option label="支付宝" value="支付宝" />
              <el-option label="银行卡" value="银行卡" />
              <el-option label="现金" value="现金" />
            </el-select>
          </el-form-item>
          <el-form-item label="支付流水号" prop="payNo">
            <el-input v-model="payForm.payNo" placeholder="请输入支付流水号(选填)" />
          </el-form-item>
        </el-form>
        <template #footer>
          <span class="dialog-footer">
            <el-button @click="payDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="submitPay" :loading="submitting">确认支付</el-button>
          </span>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

// 数据列表
const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const submitting = ref(false)

// 统计数据
const totalAmount = ref(0)
const paidAmount = ref(0)
const refundAmount = ref(0)

// 搜索表单
const searchForm = reactive({
  orderNo: '',
  username: '',
  status: null
})

// 订单详情对话框
const detailDialogVisible = ref(false)
const currentOrder = ref({})

// 支付对话框
const payDialogVisible = ref(false)
const payFormRef = ref(null)
const payForm = reactive({
  payMethod: '',
  payNo: ''
})

// 获取订单列表
const fetchOrders = async () => {
  loading.value = true
  try {
    const params = {
      currentPage: currentPage.value,
      size: pageSize.value,
      orderNo: searchForm.orderNo,
      status: searchForm.status
    }
    
    // 如果输入了用户名，需要先查询用户ID
    if (searchForm.username) {
      try {
        await request.get('/user/page', {
          username: searchForm.username,
          currentPage: 1,
          size: 10
        }, {
          onSuccess: (res) => {
            if (res.records && res.records.length > 0) {
              params.userId = res.records[0].id
            }
          }
        })
      } catch (error) {
        console.error('查询用户失败:', error)
      }
    }
    
    await request.get('/order/page', params, {
      onSuccess: (res) => {
        tableData.value = res.records || []
        total.value = res.total || 0
      }
    })
  } catch (error) {
    console.error('获取订单列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 获取统计数据
const fetchStats = async () => {
  try {
    // 获取总金额
    await request.get('/order/stats/amount', null,{
      onSuccess: (res) => {
        totalAmount.value = res
      }
    })
    
    // 获取已支付金额
    await request.get('/order/stats/amount', null,{
      params: { status: 1 },
      onSuccess: (res) => {
        paidAmount.value = res
      }
    })
    
    // 获取已退款金额
    await request.get('/order/stats/amount', null,{
      params: { status: 3 },
      onSuccess: (res) => {
        refundAmount.value = res
      }
    })
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

// 重置搜索
const resetSearch = () => {
  searchForm.orderNo = ''
  searchForm.username = ''
  searchForm.status = null
  fetchOrders()
}

// 处理分页大小变化
const handleSizeChange = (val) => {
  pageSize.value = val
  fetchOrders()
}

// 处理页码变化
const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchOrders()
}

// 查看订单详情
const handleViewDetail = (row) => {
  currentOrder.value = { ...row }
  detailDialogVisible.value = true
}

// 处理支付订单
const handlePay = (row) => {
  currentOrder.value = { ...row }
  payForm.payMethod = ''
  payForm.payNo = ''
  payDialogVisible.value = true
}

// 提交支付
const submitPay = async () => {
  if (!payForm.payMethod) {
    ElMessage.warning('请选择支付方式')
    return
  }
  
  submitting.value = true
  try {
    await request.post(`/order/pay/${currentOrder.value.id}`, {
      payMethod: payForm.payMethod,
      payNo: payForm.payNo
    }, {
      successMsg: '支付成功',
      onSuccess: () => {
        payDialogVisible.value = false
        fetchOrders()
        fetchStats()
        
        if (detailDialogVisible.value) {
          // 刷新当前订单详情
          request.get(`/order/${currentOrder.value.id}`, {
            onSuccess: (data) => {
              currentOrder.value = data
            }
          })
        }
      }
    })
  } finally {
    submitting.value = false
  }
}

// 处理取消订单
const handleCancel = (row) => {
  ElMessageBox.confirm(
    '确定要取消此订单吗？',
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await request.post(`/order/cancel/${row.id}`, null, {
        successMsg: '订单已取消',
        onSuccess: () => {
          fetchOrders()
          fetchStats()
          
          if (detailDialogVisible.value && currentOrder.value.id === row.id) {
            // 刷新当前订单详情
            request.get(`/order/${row.id}`, {
              onSuccess: (data) => {
                currentOrder.value = data
              }
            })
          }
        }
      })
    } catch (error) {
      console.error('取消订单失败:', error)
    }
  }).catch(() => {})
}

// 处理退款
const handleRefund = (row) => {
  ElMessageBox.confirm(
    '确定要为此订单退款吗？',
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await request.post(`/order/refund/${row.id}`, null, {
        successMsg: '退款成功',
        onSuccess: () => {
          fetchOrders()
          fetchStats()
          
          if (detailDialogVisible.value && currentOrder.value.id === row.id) {
            // 刷新当前订单详情
            request.get(`/order/${row.id}`, {
              onSuccess: (data) => {
                currentOrder.value = data
              }
            })
          }
        }
      })
    } catch (error) {
      console.error('退款失败:', error)
    }
  }).catch(() => {})
}

// 格式化金额
const formatAmount = (amount) => {
  if (amount === undefined || amount === null) return '0.00'
  return parseFloat(amount).toFixed(2)
}

// 获取订单状态对应的类型
const getStatusType = (status) => {
  switch (status) {
    case 0: return 'info'
    case 1: return 'success'
    case 2: return 'warning'
    case 3: return 'danger'
    default: return 'info'
  }
}

// 获取预约状态对应的类型
const getReservationStatusType = (status) => {
  switch (status) {
    case 0: return 'info'
    case 1: return 'success'
    case 2: return 'warning'
    case 3: return 'primary'
    default: return 'info'
  }
}

// 获取预约状态名称
const getReservationStatusName = (status) => {
  switch (status) {
    case 0: return '待确认'
    case 1: return '已确认'
    case 2: return '已取消'
    case 3: return '已完成'
    default: return '未知'
  }
}

// 生命周期钩子
onMounted(() => {
  fetchOrders()
  fetchStats()
})
</script>

<style lang="scss" scoped>
.order-management {
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
  
  .stats-container {
    margin-bottom: 20px;
    
    .stats-card {
      height: 100px;
      
      .stats-item {
        display: flex;
        flex-direction: column;
        justify-content: center;
        align-items: center;
        height: 100%;
        
        .stats-title {
          font-size: 14px;
          color: #909399;
          margin-bottom: 10px;
        }
        
        .stats-value {
          font-size: 24px;
          font-weight: bold;
          
          &.primary {
            color: #409EFF;
          }
          
          &.warning {
            color: #E6A23C;
          }
        }
      }
    }
  }
  
  .amount {
    color: #F56C6C;
    font-weight: bold;
  }
  
  .room-type {
    font-size: 12px;
    color: #909399;
  }
  
  .pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: center;
  }
  
  .reservation-details {
    margin-top: 20px;
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
}
</style> 