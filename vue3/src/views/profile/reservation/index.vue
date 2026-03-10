<template>
  <div class="user-reservation">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>历史订单</span>
        </div>
      </template>
      
      <!-- 预约列表 -->
      <el-table :data="reservations" style="width: 100%" v-loading="loading" border stripe>
        <el-table-column label="房间信息" width="150">
          <template #default="scope">
            <div v-if="scope.row.room">
              <div class="room-number">{{ scope.row.room.roomNumber }}</div>
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
        <el-table-column label="预约状态" width="150">
          <template #default="scope">
            <div>
            <el-tag :type="getStatusType(scope.row.status)">
              {{ scope.row.statusName }}
            </el-tag>
              <div v-if="scope.row.status === 0 && scope.row.payStatus === 1" class="status-note">
                (已支付，等待确认)
              </div>
            </div>
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
        <el-table-column prop="createTime" label="预约时间"  />
        <el-table-column label="操作" fixed="right" width="220">
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
              v-if="scope.row.status === 1 && scope.row.payStatus === 0" 
              @click="handlePay(scope.row)">
              去支付
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
      
      <!-- 预约详情对话框 -->
      <el-dialog v-model="dialogVisible" title="预约详情" width="600px">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="房间号">{{ currentReservation.room ? currentReservation.room.roomNumber : '未知' }}</el-descriptions-item>
          <el-descriptions-item label="房间类型">{{ currentReservation.room && currentReservation.room.roomType ? currentReservation.room.roomType.name : '未知' }}</el-descriptions-item>
          <el-descriptions-item label="入住日期">{{ currentReservation.startDate }}</el-descriptions-item>
          <el-descriptions-item label="退房日期">{{ currentReservation.endDate }}</el-descriptions-item>
          <el-descriptions-item label="预约状态">
            <el-tag :type="getStatusType(currentReservation.status)">{{ currentReservation.statusName }}</el-tag>
            <span v-if="currentReservation.status === 0 && currentReservation.payStatus === 1" class="status-note">
              (已支付，等待管理员确认)
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="支付状态">
            <el-tag :type="getPayStatusType(currentReservation.payStatus)">{{ currentReservation.payStatusName }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="总价">¥{{ currentReservation.price ? currentReservation.price.toFixed(2) : '0.00' }}</el-descriptions-item>
          <el-descriptions-item label="入住人数">{{ currentReservation.guestCount }} 人</el-descriptions-item>
          <el-descriptions-item label="入住人">{{ currentReservation.guestName }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ currentReservation.guestPhone }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ currentReservation.notes }}</el-descriptions-item>
          <el-descriptions-item label="预约时间">{{ currentReservation.createTime }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ currentReservation.updateTime }}</el-descriptions-item>
        </el-descriptions>
        
        <div class="operation-container" v-if="currentReservation.status !== 2 && currentReservation.status !== 3">
          <el-divider content-position="center">操作</el-divider>
          <div class="operation-buttons">
            <el-button 
              type="success" 
              v-if="currentReservation.payStatus === 0" 
              @click="handlePay(currentReservation)">
              去支付
            </el-button>
            <el-button 
              type="danger" 
              @click="handleCancel(currentReservation)">
              取消预约
            </el-button>
          </div>
        </div>
        
        <template #footer>
          <span class="dialog-footer">
            <el-button @click="dialogVisible = false">关闭</el-button>
          </span>
        </template>
      </el-dialog>
      
      <!-- 支付对话框 -->
      <el-dialog v-model="payDialogVisible" title="订单支付" width="500px">
        <div class="payment-container" v-if="currentOrder">
          <div class="order-info">
            <h3>订单信息</h3>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="订单号">{{ currentOrder.orderNo }}</el-descriptions-item>
              <el-descriptions-item label="房间">{{ currentOrder.reservation && currentOrder.reservation.room ? currentOrder.reservation.room.roomNumber : '未知' }}</el-descriptions-item>
              <el-descriptions-item label="入住日期">{{ currentOrder.reservation ? currentOrder.reservation.startDate : '' }}</el-descriptions-item>
              <el-descriptions-item label="退房日期">{{ currentOrder.reservation ? currentOrder.reservation.endDate : '' }}</el-descriptions-item>
              <el-descriptions-item label="订单金额">¥{{ currentOrder.amount ? currentOrder.amount.toFixed(2) : '0.00' }}</el-descriptions-item>
            </el-descriptions>
          </div>
          
          <div class="payment-method">
            <h3>支付方式</h3>
            <el-radio-group v-model="payMethod">
              <el-radio label="微信支付">微信支付</el-radio>
              <el-radio label="支付宝">支付宝</el-radio>
              <el-radio label="银行卡">银行卡</el-radio>
            </el-radio-group>
          </div>
          
          <div class="payment-action">
            <el-button type="primary" :loading="payLoading" @click="handlePay">确认支付</el-button>
          </div>
        </div>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { useRouter } from 'vue-router'

// 数据
const reservations = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const currentReservation = ref({})
const payDialogVisible = ref(false)
const currentOrder = ref(null)
const payMethod = ref('微信支付')
const payLoading = ref(false)
const currentPayReservation = ref({})
const payForm = reactive({
  payMethod: '微信支付',
  payNo: ''
})
const submitting = ref(false)
const router = useRouter()

// 获取预约列表
const fetchReservations = async () => {
  loading.value = true
  try {
    await request.get('/reservation/user', null,{
      onSuccess: (res) => {
        reservations.value = res
      }
    })
  } catch (error) {
    console.error('获取预约列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 查看预约详情
const handleViewDetail = (row) => {
  currentReservation.value = { ...row }
  dialogVisible.value = true
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
const handleCreateOrder = async (reservation) => {
  try {
    await request.post('/order/create', { reservationId: reservation.id }, {
      onSuccess: (order) => {
        currentOrder.value = order
        payDialogVisible.value = true
      }
    })
  } catch (error) {
    console.error('创建订单失败:', error)
  }
}

// 处理支付
const handlePay = async (reservation) => {
  // 查询该预约是否已有订单
  try {
    await request.get(`/order/reservation/${reservation.id}`, null,{
      onSuccess: (res) => {
        if (res) {
          // 有订单，跳转到订单页面
          router.push('/profile/order')
        } else {
          // 没有订单，创建订单
          createOrder(reservation)
        }
      }
    })
  } catch (error) {
    console.error('查询订单失败:', error)
  }
}

const createOrder = async (reservation) => {
  try {
    await request.post(`/order/create/${reservation.id}`, null, {
      successMsg: '订单已创建',
      onSuccess: () => {
        router.push('/profile/order')
      }
    })
  } catch (error) {
    console.error('创建订单失败:', error)
  }
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
.user-reservation {
  padding: 20px 0;
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .box-card {
    margin-bottom: 20px;
  }
  
  .room-number {
    font-weight: bold;
  }
  
  .room-type {
    font-size: 12px;
    color: #909399;
    margin-top: 4px;
  }
  
  .status-note {
    display: block;
    font-size: 12px;
    color: #E6A23C;
    margin-top: 5px;
    font-style: italic;
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
  
  .payment-container {
    .order-info {
      margin-bottom: 20px;
      
      h3 {
        margin-bottom: 15px;
        font-weight: bold;
        color: #333;
      }
    }
    
    .payment-method {
      margin-bottom: 20px;
      
      h3 {
        margin-bottom: 15px;
        font-weight: bold;
        color: #333;
      }
    }
    
    .payment-action {
      margin-top: 20px;
      text-align: center;
    }
  }
}
</style> 