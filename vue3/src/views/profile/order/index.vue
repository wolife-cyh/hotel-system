<template>
  <div class="user-order">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <div class="header-title">
            <div class="title-decoration"></div>
            <h3>我的订单</h3>
          </div>
        </div>
      </template>
      
      <!-- 订单统计 -->
      <div class="order-stats">
        <el-row :gutter="30">
          <el-col :span="6">
            <div class="stats-card">
              <div class="stats-item">
                <div class="stats-icon all-icon">
                  <el-icon><Tickets /></el-icon>
                </div>
                <div class="stats-content">
                  <div class="stats-title">全部订单</div>
                  <div class="stats-value">{{ total }}</div>
                </div>
              </div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stats-card">
              <div class="stats-item">
                <div class="stats-icon pending-icon">
                  <el-icon><Clock /></el-icon>
                </div>
                <div class="stats-content">
                  <div class="stats-title">待支付</div>
                  <div class="stats-value warning">{{ pendingCount }}</div>
                </div>
              </div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stats-card">
              <div class="stats-item">
                <div class="stats-icon completed-icon">
                  <el-icon><CircleCheck /></el-icon>
                </div>
                <div class="stats-content">
                  <div class="stats-title">已完成</div>
                  <div class="stats-value success">{{ completedCount }}</div>
                </div>
              </div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stats-card">
              <div class="stats-item">
                <div class="stats-icon amount-icon">
                  <el-icon><Money /></el-icon>
                </div>
                <div class="stats-content">
                  <div class="stats-title">消费总额</div>
                  <div class="stats-value primary">￥{{ formatAmount(totalAmount) }}</div>
                </div>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>
      
      <!-- 订单筛选 -->
      <div class="filter-container">
        <el-radio-group v-model="statusFilter" @change="handleFilterChange" size="large">
          <el-radio-button :label="null">全部</el-radio-button>
          <el-radio-button :label="0">待支付</el-radio-button>
          <el-radio-button :label="1">已支付</el-radio-button>
          <el-radio-button :label="2">已取消</el-radio-button>
          <el-radio-button :label="3">已退款</el-radio-button>
        </el-radio-group>
      </div>
      
      <!-- 订单列表 -->
      <div v-loading="loading">
        <div v-if="orders.length === 0" class="empty-container">
          <el-empty description="暂无订单" />
        </div>
        
        <div v-else class="order-list">
          <div class="order-item" v-for="order in orders" :key="order.id">
            <div class="order-header">
              <div class="order-no">
                <el-icon><Tickets /></el-icon>
                <span>订单号：{{ order.orderNo }}</span>
              </div>
              <div class="order-status">
                <el-tag :type="getStatusType(order.status)" effect="light">{{ order.statusName }}</el-tag>
              </div>
            </div>
            
            <div class="order-content" v-if="order.reservation">
              <div class="room-info">
                <div class="room-image" v-if="order.reservation.room && order.reservation.room.roomType">
                  <el-image 
                    :src="getImageUrl(order.reservation.room.roomType.image)" 
                    fit="cover"
                  />
                </div>
                <div class="room-details">
                  <div class="room-name">
                    {{ order.reservation.room && order.reservation.room.roomType ? order.reservation.room.roomType.name : '未知房型' }}
                  </div>
                  <div class="room-number">
                    房间号：{{ order.reservation.room ? order.reservation.room.roomNumber : '未知' }}
                  </div>
                  <div class="stay-dates">
                    <el-icon><Calendar /></el-icon>
                    <span>{{ order.reservation.startDate }} 至 {{ order.reservation.endDate }}</span>
                  </div>
                </div>
              </div>
              
              <div class="order-price">
                <div class="price-amount">￥{{ formatAmount(order.amount) }}</div>
              </div>
            </div>
            
            <div class="order-footer">
              <div class="order-time">
                <el-icon><Clock /></el-icon>
                <span>下单时间：{{ formatDateTime(order.createTime) }}</span>
              </div>
              <div class="order-actions">
                <el-button 
                  type="primary" 
                  plain
                  size="small" 
                  @click="handleViewDetail(order)">
                  <el-icon><View /></el-icon>
                  <span>查看详情</span>
                </el-button>
                <el-button 
                  type="success" 
                  size="small" 
                  v-if="order.status === 0" 
                  @click="handlePay(order)">
                  <el-icon><Wallet /></el-icon>
                  <span>去支付</span>
                </el-button>
                <el-button 
                  type="warning" 
                  plain
                  size="small" 
                  v-if="order.status === 0" 
                  @click="handleCancel(order)">
                  <el-icon><Close /></el-icon>
                  <span>取消订单</span>
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 订单详情对话框 -->
      <el-dialog v-model="detailDialogVisible" title="订单详情" width="650px">
        <div class="dialog-content">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="订单号">{{ currentOrder.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="订单金额">￥{{ formatAmount(currentOrder.amount) }}</el-descriptions-item>
            <el-descriptions-item label="订单状态">
              <el-tag :type="getStatusType(currentOrder.status)" effect="light">
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
            <div class="section-title">
              <div class="title-decoration"></div>
              <h4>预约信息</h4>
            </div>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="预约状态">
                <el-tag :type="getReservationStatusType(currentOrder.reservation.status)" effect="light">
                  {{ getReservationStatusName(currentOrder.reservation.status) }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="房间类型">
                {{ currentOrder.reservation.room && currentOrder.reservation.room.roomType 
                   ? currentOrder.reservation.room.roomType.name : '未知' }}
              </el-descriptions-item>
              <el-descriptions-item label="房间号">
                {{ currentOrder.reservation.room ? currentOrder.reservation.room.roomNumber : '未知' }}
              </el-descriptions-item>
              <el-descriptions-item label="单价/晚">
                ￥{{ currentOrder.reservation.room && currentOrder.reservation.room.roomType 
                   ? formatAmount(currentOrder.reservation.room.roomType.price) : '0.00' }}
              </el-descriptions-item>
              <el-descriptions-item label="入住日期">{{ currentOrder.reservation.startDate }}</el-descriptions-item>
              <el-descriptions-item label="退房日期">{{ currentOrder.reservation.endDate }}</el-descriptions-item>
              <el-descriptions-item label="入住人">{{ currentOrder.reservation.guestName }}</el-descriptions-item>
              <el-descriptions-item label="联系电话">{{ currentOrder.reservation.guestPhone }}</el-descriptions-item>
              <el-descriptions-item label="备注" :span="2">{{ currentOrder.reservation.notes || '无' }}</el-descriptions-item>
            </el-descriptions>
          </div>
          
          <div class="operation-container" v-if="currentOrder.status === 0">
            <div class="section-title">
              <div class="title-decoration"></div>
              <h4>操作</h4>
            </div>
            <div class="operation-buttons">
              <el-button 
                type="success" 
                @click="handlePay(currentOrder)">
                <el-icon><Wallet /></el-icon>
                <span>去支付</span>
              </el-button>
              <el-button 
                type="warning" 
                plain
                @click="handleCancel(currentOrder)">
                <el-icon><Close /></el-icon>
                <span>取消订单</span>
              </el-button>
            </div>
          </div>
        </div>
        
        <template #footer>
          <span class="dialog-footer">
            <el-button @click="detailDialogVisible = false">关闭</el-button>
          </span>
        </template>
      </el-dialog>
      
      <!-- 支付对话框 -->
      <el-dialog v-model="payDialogVisible" title="订单支付" width="500px">
        <div class="payment-dialog">
          <div class="payment-header">
            <div class="payment-amount">￥{{ formatAmount(currentOrder.amount) }}</div>
            <div class="payment-desc">订单号：{{ currentOrder.orderNo }}</div>
          </div>
          
          <div class="section-title payment-title">
            <div class="title-decoration"></div>
            <h4>选择支付方式</h4>
          </div>
          
          <el-form :model="payForm" ref="payFormRef" label-width="100px">
            <el-form-item label="支付方式" prop="payMethod">
              <el-radio-group v-model="payForm.payMethod" class="payment-methods">
                <el-radio label="微信支付">
                  <div class="payment-method-item">
                    <div class="payment-icon wechat-icon">
                      <el-icon><Coin /></el-icon>
                    </div>
                    <span>微信支付</span>
                  </div>
                </el-radio>
                <el-radio label="支付宝">
                  <div class="payment-method-item">
                    <div class="payment-icon alipay-icon">
                      <el-icon><ShoppingCart /></el-icon>
                    </div>
                    <span>支付宝</span>
                  </div>
                </el-radio>
                <el-radio label="银行卡">
                  <div class="payment-method-item">
                    <div class="payment-icon bank-icon">
                      <el-icon><CreditCard /></el-icon>
                    </div>
                    <span>银行卡支付</span>
                  </div>
                </el-radio>
              </el-radio-group>
            </el-form-item>
            
<!--            <el-form-item label="支付流水号" prop="payNo">-->
<!--              <el-input v-model="payForm.payNo" placeholder="请输入支付流水号(选填)" />-->
<!--            </el-form-item>-->
          </el-form>
          
          <div class="payment-notice">
            <el-alert
              type="info"
              :closable="false"
              show-icon
            >
              <p>仅作为本项目模拟运行使用</p>
            </el-alert>
          </div>
        </div>
        <template #footer>
          <span class="dialog-footer">
            <el-button @click="payDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="submitPay" :loading="submitting">
              <el-icon><Check /></el-icon>
              <span>确认支付</span>
            </el-button>
          </span>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Tickets, 
  Clock, 
  CircleCheck, 
  Money, 
  Calendar, 
  View, 
  Wallet, 
  Close, 
  Connection, 
  CreditCard, 
  Check 
} from '@element-plus/icons-vue'
import request from '@/utils/request'

// 数据
const orders = ref([])
const loading = ref(false)
const submitting = ref(false)
const statusFilter = ref(null)
const total = ref(0)

// 统计数据
const pendingCount = computed(() => orders.value.filter(order => order.status === 0).length)
const completedCount = computed(() => orders.value.filter(order => order.status === 1).length)
const totalAmount = computed(() => {
  let sum = 0
  orders.value.forEach(order => {
    if (order.status === 1 && order.amount) {
      sum += parseFloat(order.amount)
    }
  })
  return sum
})

// 订单详情对话框
const detailDialogVisible = ref(false)
const currentOrder = ref({})

// 支付对话框
const payDialogVisible = ref(false)
const payFormRef = ref(null)
const payForm = reactive({
  payMethod: '微信支付',
  payNo: ''
})

// 获取用户订单
const fetchUserOrders = async () => {
  loading.value = true
  try {
    await request.get('/order/user',null, {
      onSuccess: (res) => {
        orders.value = res || []
        total.value = orders.value.length
        
        // 如果有状态筛选，应用筛选
        if (statusFilter.value !== null) {
          orders.value = orders.value.filter(order => order.status === statusFilter.value)
        }
      }
    })
  } catch (error) {
    console.error('获取用户订单失败:', error)
  } finally {
    loading.value = false
  }
}

// 处理状态筛选变化
const handleFilterChange = () => {
  fetchUserOrders()
}

// 查看订单详情
const handleViewDetail = (order) => {
  currentOrder.value = { ...order }
  detailDialogVisible.value = true
}

// 处理支付订单
const handlePay = (order) => {
  currentOrder.value = { ...order }
  payForm.payMethod = '微信支付'
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
        detailDialogVisible.value = false
        fetchUserOrders()
      }
    })
  } finally {
    submitting.value = false
  }
}

// 处理取消订单
const handleCancel = (order) => {
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
      await request.post(`/order/cancel/${order.id}`, null, {
        successMsg: '订单已取消',
        onSuccess: () => {
          fetchUserOrders()
          
          if (detailDialogVisible.value && currentOrder.value.id === order.id) {
            // 刷新当前订单详情
            request.get(`/order/${order.id}`, {
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

// 格式化金额
const formatAmount = (amount) => {
  if (amount === undefined || amount === null) return '0.00'
  return parseFloat(amount).toFixed(2)
}

// 格式化日期时间
const formatDateTime = (dateTime) => {
  if (!dateTime) return ''
  const date = new Date(dateTime)
  return date.toLocaleString()
}

// 获取图片完整URL
const baseAPI = process.env.VUE_APP_BASE_API || '/api'
const getImageUrl = (path) => {
  if (!path) return ''
  return baseAPI + path
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
  fetchUserOrders()
})
</script>

<style lang="scss" scoped>
.user-order {
  font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
  
  .box-card {
    border-radius: 8px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
    transition: all 0.3s ease;
    
    &:hover {
      box-shadow: 0 6px 30px rgba(0, 0, 0, 0.1);
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
  }
  
  .order-stats {
    margin-bottom: 30px;
    
    .stats-card {
      height: 100px;
      background-color: #fff;
      border-radius: 8px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
      transition: all 0.3s ease;
      
      &:hover {
        transform: translateY(-5px);
        box-shadow: 0 8px 15px rgba(0, 0, 0, 0.1);
      }
      
      .stats-item {
        display: flex;
        align-items: center;
        height: 100%;
        padding: 0 20px;
        
        .stats-icon {
          width: 50px;
          height: 50px;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          margin-right: 15px;
          
          .el-icon {
            font-size: 24px;
            color: #fff;
          }
          
          &.all-icon {
            background: linear-gradient(135deg, #1890ff, #36a3ff);
          }
          
          &.pending-icon {
            background: linear-gradient(135deg, #e6a23c, #f3b760);
          }
          
          &.completed-icon {
            background: linear-gradient(135deg, #67c23a, #85ce61);
          }
          
          &.amount-icon {
            background: linear-gradient(135deg, #409eff, #53a8ff);
          }
        }
        
        .stats-content {
          flex: 1;
          
          .stats-title {
            font-size: 14px;
            color: #909399;
            margin-bottom: 8px;
          }
          
          .stats-value {
            font-size: 24px;
            font-weight: 300;
            
            &.primary {
              color: #1890ff;
            }
            
            &.success {
              color: #67C23A;
            }
            
            &.warning {
              color: #E6A23C;
            }
          }
        }
      }
    }
  }
  
  .filter-container {
    margin-bottom: 25px;
    
    :deep(.el-radio-group) {
      .el-radio-button__inner {
        border-color: #e4e7ed;
        color: #606266;
        transition: all 0.3s;
        
        &:hover {
          color: #1890ff;
        }
      }
      
      .el-radio-button__original-radio:checked + .el-radio-button__inner {
        background-color: #1890ff;
        border-color: #1890ff;
        box-shadow: -1px 0 0 0 #1890ff;
      }
    }
  }
  
  .empty-container {
    padding: 60px 0;
    text-align: center;
  }
  
  .order-list {
    .order-item {
      background-color: #fff;
      border-radius: 8px;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
      margin-bottom: 25px;
      overflow: hidden;
      transition: all 0.3s ease;
      
      &:hover {
        transform: translateY(-5px);
        box-shadow: 0 8px 15px rgba(0, 0, 0, 0.1);
      }
      
      .order-header {
        background-color: #f8fafb;
        padding: 15px 20px;
        display: flex;
        justify-content: space-between;
        align-items: center;
        border-bottom: 1px solid #eaeaea;
        
        .order-no {
          font-size: 14px;
          color: #606266;
          display: flex;
          align-items: center;
          
          .el-icon {
            color: #1890ff;
            margin-right: 5px;
          }
        }
      }
      
      .order-content {
        padding: 20px;
        display: flex;
        border-bottom: 1px solid #eaeaea;
        
        .room-info {
          display: flex;
          flex: 1;
          
          .room-image {
            width: 120px;
            height: 80px;
            margin-right: 20px;
            border-radius: 8px;
            overflow: hidden;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
            
            .el-image {
              width: 100%;
              height: 100%;
              transition: transform 0.5s;
              
              &:hover {
                transform: scale(1.05);
              }
            }
          }
          
          .room-details {
            flex: 1;
            
            .room-name {
              font-size: 18px;
              font-weight: 500;
              color: #333;
              margin-bottom: 8px;
            }
            
            .room-number, .stay-dates {
              font-size: 14px;
              color: #606266;
              margin-top: 8px;
              display: flex;
              align-items: center;
              
              .el-icon {
                color: #1890ff;
                margin-right: 5px;
              }
            }
          }
        }
        
        .order-price {
          display: flex;
          flex-direction: column;
          justify-content: center;
          align-items: flex-end;
          min-width: 120px;
          
          .price-amount {
            font-size: 24px;
            font-weight: 500;
            color: #1890ff;
          }
        }
      }
      
      .order-footer {
        padding: 15px 20px;
        display: flex;
        justify-content: space-between;
        align-items: center;
        
        .order-time {
          font-size: 14px;
          color: #909399;
          display: flex;
          align-items: center;
          
          .el-icon {
            margin-right: 5px;
          }
        }
        
        .order-actions {
          display: flex;
          gap: 10px;
          
          .el-button {
            display: flex;
            align-items: center;
            
            .el-icon {
              margin-right: 5px;
            }
          }
        }
      }
    }
  }
  
  .dialog-content {
    :deep(.el-descriptions) {
      margin-bottom: 20px;
      
      .el-descriptions__header {
        margin-bottom: 15px;
      }
      
      .el-descriptions__label {
        background-color: #f8f9fa;
        color: #555;
        font-weight: 500;
      }
      
      .el-descriptions__content {
        color: #333;
      }
      
      .el-descriptions__cell {
        padding: 12px 15px;
      }
    }
    
    .section-title {
      display: flex;
      align-items: center;
      margin: 25px 0 15px;
      
      .title-decoration {
        width: 3px;
        height: 16px;
        background-color: #1890ff;
        margin-right: 10px;
        border-radius: 3px;
      }
      
      h4 {
        margin: 0;
        font-size: 16px;
        font-weight: 500;
        color: #333;
      }
      
      &.payment-title {
        margin-top: 0;
      }
    }
  }
  
  .reservation-details {
    margin-top: 20px;
  }
  
  .operation-container {
    margin-top: 20px;
    
    .operation-buttons {
      display: flex;
      flex-wrap: wrap;
      gap: 15px;
      justify-content: center;
      margin-top: 15px;
      
      .el-button {
        padding: 10px 20px;
        display: flex;
        align-items: center;
        
        .el-icon {
          margin-right: 5px;
        }
      }
    }
  }
  
  .payment-dialog {
    .payment-header {
      text-align: center;
      margin-bottom: 25px;
      
      .payment-amount {
        font-size: 36px;
        font-weight: 300;
        color: #1890ff;
      }
      
      .payment-desc {
        margin-top: 10px;
        color: #606266;
      }
    }
    
    .payment-methods {
      display: flex;
      flex-direction: column;
      gap: 15px;
      padding: 0 10px;
      
      .el-radio {
        height: auto;
        margin-right: 0;
        
        :deep(.el-radio__input) {
          align-self: flex-start;
          margin-top: 13px;
        }
      }
      
      .payment-method-item {
        display: flex;
        align-items: center;
        padding: 10px;
        border-radius: 8px;
        transition: all 0.3s;
        
        &:hover {
          background-color: rgba(24, 144, 255, 0.05);
        }
        
        .payment-icon {
          display: flex;
          justify-content: center;
          align-items: center;
          width: 40px;
          height: 40px;
          margin-right: 15px;
          border-radius: 8px;
          transition: all 0.3s;
          
          .el-icon {
            font-size: 20px;
            color: white;
          }
          
          &.wechat-icon {
            background: linear-gradient(135deg, #07C160, #10e375);
          }
          
          &.alipay-icon {
            background: linear-gradient(135deg, #1677FF, #4a93ff);
          }
          
          &.bank-icon {
            background: linear-gradient(135deg, #F56C6C, #f78989);
          }
        }
        
        span {
          font-size: 15px;
        }
      }
    }
    
    :deep(.el-form-item) {
      margin-bottom: 25px;
      
      .el-form-item__label {
        color: #555;
        font-weight: 500;
      }
      
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
    
    .payment-notice {
      margin-top: 20px;
      
      :deep(.el-alert) {
        background-color: #f0f7ff;
        border: 1px solid #d4e8ff;
        
        .el-alert__icon {
          color: #1890ff;
        }
        
        .el-alert__content {
          padding: 5px 0;
          
          p {
            margin: 5px 0;
            color: #666;
          }
        }
      }
    }
  }
}

@media (max-width: 768px) {
  .user-order {
    .order-stats {
      .stats-card {
        margin-bottom: 15px;
      }
    }
    
    .order-list {
      .order-item {
        .order-content {
          flex-direction: column;
          
          .room-info {
            margin-bottom: 15px;
          }
          
          .order-price {
            align-items: flex-start;
          }
        }
        
        .order-footer {
          flex-direction: column;
          gap: 15px;
          
          .order-time {
            margin-bottom: 10px;
          }
        }
      }
    }
  }
}
</style> 