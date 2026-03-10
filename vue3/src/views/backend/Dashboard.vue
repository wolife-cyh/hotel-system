<template>
  <div class="dashboard">
    <!-- 欢迎卡片 -->
    <el-card class="welcome-card">
      <template #header>
        <div class="welcome-header">
          <el-avatar :size="64" :src="avatarUrl">
            {{ userInfo?.name?.charAt(0) }}
          </el-avatar>
          <div class="welcome-info">
            <h2>欢迎回来, {{ userInfo?.name || userInfo?.username }}</h2>
            <p>{{ currentTime }}</p>
          </div>
        </div>
      </template>
      <div class="role-info">
        <el-tag>{{ roleLabel }}</el-tag>
      </div>
    </el-card>
    
    <!-- 统计卡片 -->
    <div class="stats-cards">
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :md="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-title">用户总数</div>
            <div class="stat-value">{{ userStats.totalUsers }}</div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-title">普通用户</div>
            <div class="stat-value">{{ userStats.normalUsers }}</div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-title">管理员</div>
            <div class="stat-value">{{ userStats.adminUsers }}</div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-title">订单总金额(￥)</div>
            <div class="stat-value primary">{{ formatAmount(orderAmounts.total) }}</div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 图表区域 -->
    <div class="charts-row">
      <el-row :gutter="20">
        <el-col :xs="24" :md="12">
          <el-card shadow="hover" class="chart-card">
            <template #header>
              <div class="card-header">用户角色分布</div>
            </template>
            <div ref="rolePieRef" class="chart-container"></div>
          </el-card>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-card shadow="hover" class="chart-card">
            <template #header>
              <div class="card-header">订单金额统计</div>
            </template>
            <div ref="amountBarRef" class="chart-container"></div>
          </el-card>
        </el-col>
      </el-row>
      
      <!-- 新增图表 -->
      <el-row :gutter="20" style="margin-top: 20px;">
        <el-col :xs="24" :md="8">
          <el-card shadow="hover" class="chart-card">
            <template #header>
              <div class="card-header">房间状态分布</div>
            </template>
            <div ref="roomStatusRef" class="chart-container"></div>
          </el-card>
        </el-col>
        <el-col :xs="24" :md="8">
          <el-card shadow="hover" class="chart-card">
            <template #header>
              <div class="card-header">预订状态分布</div>
            </template>
            <div ref="reservationStatusRef" class="chart-container"></div>
          </el-card>
        </el-col>
        <el-col :xs="24" :md="8">
          <el-card shadow="hover" class="chart-card">
            <template #header>
              <div class="card-header">订单状态分布</div>
            </template>
            <div ref="orderStatusRef" class="chart-container"></div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import * as echarts from 'echarts'
import request from '@/utils/request'

const router = useRouter()
const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)
const baseAPI = process.env.VUE_APP_BASE_API || '/api'

// 角色标签
const roleLabel = computed(() => {
  const roleMap = {
    'ADMIN': '系统管理员',
    'USER': '普通用户'
  }
  return roleMap[userInfo.value?.roleCode] || '未知角色'
})

const avatarUrl = computed(() => {
  return userInfo.value?.avatar ? baseAPI + userInfo.value.avatar : '';
})

// 当前时间
const currentTime = ref('')
let timeInterval = null

const updateTime = () => {
  const now = new Date()
  const options = { 
    year: 'numeric', 
    month: 'long', 
    day: 'numeric', 
    weekday: 'long',
    hour: '2-digit',
    minute: '2-digit'
  }
  currentTime.value = now.toLocaleDateString('zh-CN', options)
}

// 统计数据
const userStats = ref({
  totalUsers: 0,
  normalUsers: 0,
  adminUsers: 0
})

const orderAmounts = ref({
  total: 0,
  paid: 0,
  refund: 0
})

const roomStats = ref({
  availableRooms: 0,
  maintenanceRooms: 0
})

const reservationStats = ref({
  pendingReservations: 0,
  confirmedReservations: 0,
  cancelledReservations: 0,
  completedReservations: 0
})

const orderStatusStats = ref({
  unpaidOrders: 0,
  paidOrders: 0,
  cancelledOrders: 0,
  refundedOrders: 0
})

// DOM 引用
const rolePieRef = ref(null)
const amountBarRef = ref(null)
const roomStatusRef = ref(null)
const reservationStatusRef = ref(null)
const orderStatusRef = ref(null)

// ECharts 实例
let rolePieChart = null
let amountBarChart = null
let roomStatusChart = null
let reservationStatusChart = null
let orderStatusChart = null

const formatAmount = (val) => {
  try {
    const num = Number(val || 0)
    return num.toFixed(2)
  } catch {
    return '0.00'
  }
}

// 数据获取函数
const fetchUserStats = async () => {
  try {
    const res = await request.get('/user/stats')
    userStats.value = {
      totalUsers: res?.totalUsers || 0,
      normalUsers: res?.normalUsers || 0,
      adminUsers: res?.adminUsers || 0
    }
  } catch (e) {
    userStats.value = { totalUsers: 0, normalUsers: 0, adminUsers: 0 }
  }
}

const fetchOrderAmounts = async () => {
  try {
    const total = await request.get('/order/stats/amount', null)
    const paid = await request.get('/order/stats/amount', { status: 1 })
    const refund = await request.get('/order/stats/amount', { status: 3 })
    orderAmounts.value = {
      total: Number(total || 0),
      paid: Number(paid || 0),
      refund: Number(refund || 0)
    }
  } catch (e) {
    orderAmounts.value = { total: 0, paid: 0, refund: 0 }
  }
}

const fetchRoomStats = async () => {
  try {
    const res = await request.get('/room/stats')
    roomStats.value = {
      availableRooms: res?.availableRooms || 0,
      maintenanceRooms: res?.maintenanceRooms || 0
    }
  } catch (e) {
    roomStats.value = { availableRooms: 0, maintenanceRooms: 0 }
  }
}

const fetchReservationStats = async () => {
  try {
    const res = await request.get('/reservation/stats')
    reservationStats.value = {
      pendingReservations: res?.pendingReservations || 0,
      confirmedReservations: res?.confirmedReservations || 0,
      cancelledReservations: res?.cancelledReservations || 0,
      completedReservations: res?.completedReservations || 0
    }
  } catch (e) {
    reservationStats.value = { pendingReservations: 0, confirmedReservations: 0, cancelledReservations: 0, completedReservations: 0 }
  }
}

const fetchOrderStatusStats = async () => {
  try {
    const res = await request.get('/order/stats/status')
    orderStatusStats.value = {
      unpaidOrders: res?.unpaidOrders || 0,
      paidOrders: res?.paidOrders || 0,
      cancelledOrders: res?.cancelledOrders || 0,
      refundedOrders: res?.refundedOrders || 0
    }
  } catch (e) {
    orderStatusStats.value = { unpaidOrders: 0, paidOrders: 0, cancelledOrders: 0, refundedOrders: 0 }
  }
}

// 图表初始化函数
const initRolePie = () => {
  if (!rolePieRef.value) return
  if (rolePieChart) {
    rolePieChart.dispose()
  }
  rolePieChart = echarts.init(rolePieRef.value)
  rolePieChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { orient: 'horizontal', bottom: 0 },
    series: [
      {
        name: '用户角色',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}: {c} ({d}%)' },
        data: [
          { value: userStats.value.adminUsers, name: '管理员' },
          { value: userStats.value.normalUsers, name: '普通用户' }
        ]
      }
    ]
  })
}

const initAmountBar = () => {
  if (!amountBarRef.value) return
  if (amountBarChart) {
    amountBarChart.dispose()
  }
  amountBarChart = echarts.init(amountBarRef.value)
  amountBarChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '8%', containLabel: true },
    xAxis: {
      type: 'category',
      data: ['总金额', '已支付', '已退款']
    },
    yAxis: {
      type: 'value',
      axisLabel: { formatter: '￥{value}' }
    },
    series: [
      {
        name: '金额',
        type: 'bar',
        barWidth: '50%',
        data: [
          orderAmounts.value.total,
          orderAmounts.value.paid,
          orderAmounts.value.refund
        ],
        itemStyle: {
          color: (params) => {
            const colors = ['#409EFF', '#67C23A', '#E6A23C']
            return colors[params.dataIndex] || '#409EFF'
          }
        }
      }
    ]
  })
}

const initRoomStatus = () => {
  if (!roomStatusRef.value) return
  if (roomStatusChart) {
    roomStatusChart.dispose()
  }
  roomStatusChart = echarts.init(roomStatusRef.value)
  roomStatusChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { orient: 'horizontal', bottom: 0 },
    series: [
      {
        name: '房间状态',
        type: 'pie',
        radius: ['30%', '60%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 5, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}: {c}' },
        data: [
          { value: roomStats.value.availableRooms, name: '可用', itemStyle: { color: '#67C23A' } },
          { value: roomStats.value.maintenanceRooms, name: '维护中', itemStyle: { color: '#F56C6C' } }
        ]
      }
    ]
  })
}

const initReservationStatus = () => {
  if (!reservationStatusRef.value) return
  if (reservationStatusChart) {
    reservationStatusChart.dispose()
  }
  reservationStatusChart = echarts.init(reservationStatusRef.value)
  reservationStatusChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { orient: 'horizontal', bottom: 0 },
    series: [
      {
        name: '预订状态',
        type: 'pie',
        radius: ['30%', '60%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 5, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}: {c}' },
        data: [
          { value: reservationStats.value.pendingReservations, name: '待确认', itemStyle: { color: '#409EFF' } },
          { value: reservationStats.value.confirmedReservations, name: '已确认', itemStyle: { color: '#67C23A' } },
          { value: reservationStats.value.cancelledReservations, name: '已取消', itemStyle: { color: '#F56C6C' } },
          { value: reservationStats.value.completedReservations, name: '已完成', itemStyle: { color: '#E6A23C' } }
        ]
      }
    ]
  })
}

const initOrderStatus = () => {
  if (!orderStatusRef.value) return
  if (orderStatusChart) {
    orderStatusChart.dispose()
  }
  orderStatusChart = echarts.init(orderStatusRef.value)
  orderStatusChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { orient: 'horizontal', bottom: 0 },
    series: [
      {
        name: '订单状态',
        type: 'pie',
        radius: ['30%', '60%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 5, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}: {c}' },
        data: [
          { value: orderStatusStats.value.unpaidOrders, name: '未支付', itemStyle: { color: '#909399' } },
          { value: orderStatusStats.value.paidOrders, name: '已支付', itemStyle: { color: '#67C23A' } },
          { value: orderStatusStats.value.cancelledOrders, name: '已取消', itemStyle: { color: '#F56C6C' } },
          { value: orderStatusStats.value.refundedOrders, name: '已退款', itemStyle: { color: '#E6A23C' } }
        ]
      }
    ]
  })
}

const initCharts = () => {
  nextTick(() => {
    initRolePie()
    initAmountBar()
    initRoomStatus()
    initReservationStatus()
    initOrderStatus()
    
    // 自适应
    setTimeout(() => {
      rolePieChart && rolePieChart.resize()
      amountBarChart && amountBarChart.resize()
      roomStatusChart && roomStatusChart.resize()
      reservationStatusChart && reservationStatusChart.resize()
      orderStatusChart && orderStatusChart.resize()
    }, 0)
  })
}

const resizeHandler = () => {
  rolePieChart && rolePieChart.resize()
  amountBarChart && amountBarChart.resize()
  roomStatusChart && roomStatusChart.resize()
  reservationStatusChart && reservationStatusChart.resize()
  orderStatusChart && orderStatusChart.resize()
}

onMounted(async () => {
  updateTime()
  timeInterval = setInterval(updateTime, 60000)

  // 拉取所有统计数据并初始化图表
  await Promise.all([
    fetchUserStats(),
    fetchOrderAmounts(),
    fetchRoomStats(),
    fetchReservationStats(),
    fetchOrderStatusStats()
  ])
  
  initCharts()
  window.addEventListener('resize', resizeHandler)
})

onUnmounted(() => {
  if (timeInterval) {
    clearInterval(timeInterval)
    timeInterval = null
  }
  window.removeEventListener('resize', resizeHandler)
  
  // 销毁所有图表实例
  const charts = [rolePieChart, amountBarChart, roomStatusChart, reservationStatusChart, orderStatusChart]
  charts.forEach(chart => {
    if (chart) {
      chart.dispose()
    }
  })
  
  rolePieChart = null
  amountBarChart = null
  roomStatusChart = null
  reservationStatusChart = null
  orderStatusChart = null
})
</script>

<style lang="scss" scoped>
.dashboard {
  .welcome-card {
    margin-bottom: 20px;
    transition: all 0.3s ease;
    
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    }
    
    .welcome-header {
      display: flex;
      align-items: center;
      gap: 20px;
      
      .el-avatar {
        transition: transform 0.3s ease;
        
        &:hover {
          transform: scale(1.1);
        }
      }
      
      .welcome-info {
        h2 {
          margin: 0 0 8px 0;
          font-size: 24px;
          background: linear-gradient(to right, #409eff, #67c23a);
          -webkit-background-clip: text;
          -webkit-text-fill-color: transparent;
        }
        p {
          margin: 0;
          color: #666;
        }
      }
    }
    
    .role-info {
      margin-top: 16px;
    }
  }

  .stats-cards {
    margin-bottom: 20px;

    .stat-card {
      transition: all 0.3s ease;
      
      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
      }
      
      .stat-title {
        font-size: 14px;
        color: #999;
        margin-bottom: 8px;
      }
      .stat-value {
        font-size: 24px;
        font-weight: 600;
        color: #333;
        &.primary {
          color: #409EFF;
        }
      }
    }
  }

  .charts-row {
    .chart-card {
      margin-bottom: 20px;
      transition: all 0.3s ease;
      
      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
      }
    }
    
    .chart-container {
      width: 100%;
      height: 280px;
    }
    
    .card-header {
      font-size: 16px;
      font-weight: 600;
      color: #333;
    }
  }
}
</style>