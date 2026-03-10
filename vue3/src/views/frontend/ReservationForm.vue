<template>
  <div class="reservation-form-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-content">
        <h1>房间预订</h1>
        <p>填写信息，完成您的预订</p>
      </div>
    </div>
    
    <div class="main-content" v-loading="loading">
      <el-row :gutter="30">
        <!-- 左侧：预订表单 -->
        <el-col :xs="24" :md="16">
          <div class="form-card">
              <div class="form-header">
              <div class="title-container">
                <h2>预订信息</h2>
                <div class="title-decoration"></div>
              </div>
              <el-tag v-if="roomType" effect="plain" type="success">{{ roomType.name }}</el-tag>
            </div>
            
            <el-form 
              ref="reservationFormRef" 
              :model="reservationForm" 
              :rules="rules" 
              label-width="100px"
              class="reservation-form"
            >
              <!-- 入住日期信息（不可修改，从上一页带来） -->
              <el-form-item label="入住日期" prop="dateRange">
                <el-date-picker
                  v-model="reservationForm.dateRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="入住日期"
                  end-placeholder="退房日期"
                  disabled
                  value-format="YYYY-MM-DD"
                />
              </el-form-item>
              
              <!-- 入住人信息 -->
              <el-form-item label="入住人姓名" prop="guestName">
                <el-input v-model="reservationForm.guestName" placeholder="请输入入住人姓名" />
              </el-form-item>
              
              <el-form-item label="联系电话" prop="guestPhone">
                <el-input v-model="reservationForm.guestPhone" placeholder="请输入联系电话" />
              </el-form-item>
              
              <el-form-item label="入住人数" prop="guestCount">
                <el-input-number v-model="reservationForm.guestCount" :min="1" :max="maxPeople" />
                <span class="input-tip">最多{{ maxPeople }}人</span>
              </el-form-item>
              
              <!-- 特殊要求 -->
              <el-form-item label="特殊要求" prop="notes">
                <el-input
                  v-model="reservationForm.notes"
                  type="textarea"
                  placeholder="如有特殊要求，请在此说明"
                  :rows="3"
                  maxlength="200"
                  show-word-limit
                />
              </el-form-item>
              
              <!-- 支付方式 -->
              <el-form-item label="支付方式" prop="payMethod">
                <el-radio-group v-model="reservationForm.payMethod">
                  <el-radio label="online">在线支付</el-radio>
                  <el-radio label="arrival">到店支付</el-radio>
                </el-radio-group>
              </el-form-item>
              
              <!-- 提交按钮 -->
              <el-form-item>
                <el-button type="primary" @click="submitReservation" :loading="submitLoading">提交预订</el-button>
                <el-button @click="goBack">返回</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-col>
        
        <!-- 右侧：房型信息和费用明细 -->
        <el-col :xs="24" :md="8">
          <div class="info-card room-info-card">
              <div class="card-header">
              <div class="title-container">
                <h3>房型信息</h3>
                <div class="title-decoration"></div>
              </div>
            </div>
            
            <div v-if="roomType" class="room-info">
              <el-image 
                :src="getImageUrl(roomType.image)" 
                fit="cover" 
                class="room-image"
              >
                <template #error>
                  <div class="image-error">
                    <el-icon><Picture /></el-icon>
                  </div>
                </template>
              </el-image>
              
              <h3>{{ roomType.name }}</h3>
              <div class="room-tags">
                <el-tag size="small" effect="plain" type="info">{{ roomType.bedType }}</el-tag>
                <el-tag size="small" effect="plain" type="success">{{ roomType.maxPeople }}人</el-tag>
              </div>
              
              <div class="room-features">
                <p><strong>设施:</strong> {{ roomType.facilities }}</p>
                <p><strong>描述:</strong> {{ roomType.description }}</p>
              </div>
            </div>
            
            <el-divider content-position="center">费用明细</el-divider>
            
            <div class="price-details">
              <div class="price-item">
                <span>房间单价</span>
                <span>¥{{ roomPrice }} / 晚</span>
              </div>
              <div class="price-item">
                <span>入住天数</span>
                <span>{{ stayDays }}晚</span>
              </div>
              <div class="price-item total">
                <span>总价</span>
                <span class="total-price">¥{{ totalPrice }}</span>
              </div>
            </div>
          </div>
          
          <div class="info-card notes-card">
            <div class="card-header">
              <div class="title-container">
                <h3>预订须知</h3>
                <div class="title-decoration"></div>
              </div>
            </div>
            
            <ul class="notes-list">
              <li>入住时间为14:00后，退房时间为12:00前</li>
              <li>请携带有效身份证件办理入住手续</li>
              <li>如需取消预订，请提前24小时通知</li>
              <li>酒店内禁止吸烟</li>
              <li>如有其他问题，请联系客服电话：400-123-4567</li>
            </ul>
          </div>
        </el-col>
      </el-row>
    </div>
    
    <!-- 预订成功对话框 -->
    <el-dialog
      v-model="successDialogVisible"
      title="预订成功"
      width="400px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
    >
      <div class="success-content">
        <el-icon class="success-icon" color="#1890ff"><CircleCheckFilled /></el-icon>
        <h2>您的房间已预订成功！</h2>
        <p>预订编号：{{ reservationNo }}</p>
        <p>您可以在个人中心查看预订详情</p>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="goToHome">返回首页</el-button>
          <el-button type="primary" @click="goToProfile">查看订单</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Picture, CircleCheckFilled } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 状态变量
const loading = ref(true)
const submitLoading = ref(false)
const roomType = ref(null)
const reservationFormRef = ref(null)
const successDialogVisible = ref(false)
const reservationNo = ref('')

// 计算属性
const roomPrice = computed(() => roomType.value ? roomType.value.price : 0)
const maxPeople = computed(() => roomType.value ? roomType.value.maxPeople : 1)

// 计算入住天数
const stayDays = computed(() => {
  if (!reservationForm.dateRange || reservationForm.dateRange.length !== 2) return 0
  
  try {
    const startDate = new Date(reservationForm.dateRange[0])
    const endDate = new Date(reservationForm.dateRange[1])
    
    // 检查日期是否有效
    if (isNaN(startDate.getTime()) || isNaN(endDate.getTime())) {
      return 0
    }
    
    const diffTime = Math.abs(endDate - startDate)
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))
    
    return diffDays
  } catch (error) {
    console.error('计算天数出错:', error)
    return 0
  }
})

// 计算总价
const totalPrice = computed(() => {
  return (roomPrice.value * stayDays.value).toFixed(2)
})

// 表单数据
const reservationForm = reactive({
  dateRange: [],
  roomTypeId: null,
  guestName: '',
  guestPhone: '',
  guestCount: 1,
  notes: '',
  payMethod: 'online'
})

// 表单验证规则
const rules = {
  guestName: [
    { required: true, message: '请输入入住人姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '姓名长度应为2-20个字符', trigger: 'blur' }
  ],
  guestPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  guestCount: [
    { required: true, message: '请选择入住人数', trigger: 'change' },
    { type: 'number', min: 1, message: '入住人数不能小于1', trigger: 'change' }
  ],
  payMethod: [
    { required: true, message: '请选择支付方式', trigger: 'change' }
  ]
}

// 获取图片URL
const baseAPI = process.env.VUE_APP_BASE_API || '/api'
const getImageUrl = (image) => {
  if (!image) return ''
  if (image.startsWith('http')) return image
  return baseAPI + image
}

// 初始化页面数据
const initPageData = async () => {
  loading.value = true
  
  try {
    // 从路由参数中获取房型ID和日期
    const roomTypeId = route.query.roomTypeId
    const startDate = route.query.startDate
    const endDate = route.query.endDate
    
    if (!roomTypeId || !startDate || !endDate) {
      ElMessage.error('预订参数不完整，请重新选择房间')
      goBack()
      return
    }
    
    // 设置表单初始值
    reservationForm.roomTypeId = roomTypeId
    reservationForm.dateRange = [startDate, endDate]
    
    // 如果用户已登录，自动填充联系人信息
    if (userStore.userInfo) {
      reservationForm.guestName = userStore.userInfo.name || ''
      reservationForm.guestPhone = userStore.userInfo.phone || ''
    }
    
    // 获取房型详情
    await fetchRoomType(roomTypeId)
    
    // 检查房间可用性
    await checkRoomAvailability(roomTypeId, startDate, endDate)
  } catch (error) {
    console.error('初始化页面数据失败:', error)
    ElMessage.error('加载数据失败，请重试')
  } finally {
    loading.value = false
  }
}

// 获取房型详情
const fetchRoomType = async (roomTypeId) => {
  try {
    await request.get(`/roomType/${roomTypeId}`, null, {
      onSuccess: (res) => {
        roomType.value = res
        // 初始化入住人数为1
        reservationForm.guestCount = 1
      }
    })
  } catch (error) {
    console.error('获取房型详情失败:', error)
    throw error
  }
}

// 检查房间可用性
const checkRoomAvailability = async (roomTypeId, startDate, endDate) => {
  try {
    await request.get('/room/available', {
      startDate: startDate,
      endDate: endDate
    }, {
      onSuccess: (res) => {
        // 查找该房型的可用房间
        const availableRooms = res.filter(room => room.roomTypeId == roomTypeId)
        
        if (availableRooms.length === 0) {
          ElMessageBox.alert('抱歉，该时间段内所选房型已无可用房间，请选择其他日期或房型', '无可用房间', {
            confirmButtonText: '返回',
            callback: () => {
              goBack()
            }
          })
          return
        }
      }
    })
  } catch (error) {
    console.error('检查房间可用性失败:', error)
    throw error
  }
}

// 提交预订
const submitReservation = async () => {
  if (!userStore.isLoggedIn) {
    ElMessageBox.confirm('预订需要先登录，是否立即登录？', '提示', {
      confirmButtonText: '去登录',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      router.push({
        path: '/login',
        query: { redirect: route.fullPath }
      })
    }).catch(() => {})
    return
  }
  
  if (!reservationFormRef.value) return
  
  await reservationFormRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      
      try {
        // 构建预约数据
        const reservationData = {
          roomTypeId: reservationForm.roomTypeId,
          startDate: reservationForm.dateRange[0],
          endDate: reservationForm.dateRange[1],
          guestName: reservationForm.guestName,
          guestPhone: reservationForm.guestPhone,
          guestCount: reservationForm.guestCount,
          notes: reservationForm.notes,
          price: parseFloat(totalPrice.value),
          payMethod: reservationForm.payMethod
        }
        
        // 发送预约请求
        await request.post('/reservation/create', reservationData, {
          successMsg: '预订提交成功',
          onSuccess: (res) => {
            // 显示成功对话框
            reservationNo.value = res.orderNo || res.id
            successDialogVisible.value = true
          }
        })
      } catch (error) {
        console.error('提交预订失败:', error)
        ElMessage.error('预订提交失败，请重试')
      } finally {
        submitLoading.value = false
      }
    }
  })
}

// 返回上一页
const goBack = () => {
  router.back()
}

// 返回首页
const goToHome = () => {
  successDialogVisible.value = false
  router.push('/')
}

// 跳转到个人中心
const goToProfile = () => {
  successDialogVisible.value = false
  router.push('/profile/reservation')
}

// 生命周期钩子
onMounted(() => {
  initPageData()
})
</script>

<style lang="scss" scoped>
.reservation-form-container {
  font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
  color: #333;
  
  .page-header {
    height: 180px;
    background: linear-gradient(135deg, #f5f7fa 0%, #1890ff 100%);
    color: white;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 40px;
    position: relative;
    overflow: hidden;
    
    &:before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.2);
      z-index: 1;
    }
    
    .header-content {
      text-align: center;
      position: relative;
      z-index: 2;
      
      h1 {
        font-size: 36px;
        margin-bottom: 10px;
        text-shadow: 1px 1px 3px rgba(0, 0, 0, 0.3);
        font-weight: 300;
        letter-spacing: 2px;
      }
      
      p {
        font-size: 18px;
        text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.3);
        font-weight: 300;
      }
    }
  }
  
  .main-content {
    padding-bottom: 60px;
    width: 90%;
    max-width: 1200px;
    margin: 0 auto;
    
    .el-row {
      margin-bottom: 20px;
    }
  }
  
  .title-container {
    position: relative;
    margin-bottom: 15px;
    
    h2, h3 {
      margin: 0 0 10px;
      font-weight: 400;
      color: #333;
    }
    
    .title-decoration {
      width: 40px;
      height: 3px;
      background: #1890ff;
    }
  }
  
  .form-card, .info-card {
    background-color: #fff;
    border-radius: 8px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
    padding: 25px;
    margin-bottom: 30px;
  }
  
  .form-card {
    .form-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 25px;
    }
    
    .reservation-form {
      .el-form-item {
        margin-bottom: 25px;
      }
      
      .input-tip {
        margin-left: 10px;
        color: #888;
        font-size: 13px;
      }
      
      :deep(.el-form-item__label) {
        color: #555;
      }
      
      .el-button {
        padding: 12px 25px;
        font-size: 16px;
        border-radius: 4px;
        
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
  
  .room-info-card {
    .card-header {
    margin-bottom: 20px;
    }
    
    .room-info {
      .room-image {
        width: 100%;
        height: 180px;
        margin-bottom: 20px;
        border-radius: 8px;
        overflow: hidden;
      }
      
      h3 {
        margin-top: 0;
        margin-bottom: 15px;
        color: #333;
      }
      
      .room-tags {
        margin-bottom: 20px;
        
        .el-tag {
          margin-right: 8px;
          font-weight: 400;
        }
      }
      
      .room-features {
        font-size: 14px;
        color: #666;
        margin-bottom: 20px;
        
        p {
          margin: 10px 0;
          line-height: 1.5;
        }
      }
    }
    
    .price-details {
      .price-item {
        display: flex;
        justify-content: space-between;
        margin-bottom: 12px;
        font-size: 14px;
        color: #666;
        
        &.total {
          margin-top: 15px;
          padding-top: 15px;
          border-top: 1px dashed #e0e0e0;
          font-weight: bold;
          color: #333;
          
          .total-price {
            color: #1890ff;
            font-size: 20px;
          }
        }
      }
    }
  }
  
  .notes-card {
    .card-header {
      margin-bottom: 20px;
    }
    
    .notes-list {
      padding-left: 20px;
      margin: 0;
      
      li {
        margin-bottom: 12px;
        font-size: 14px;
        color: #666;
        line-height: 1.6;
      }
    }
  }
  
  .success-content {
    text-align: center;
    padding: 30px 0;
    
    .success-icon {
      font-size: 60px;
      margin-bottom: 20px;
    }
    
    h2 {
      margin-bottom: 20px;
      color: #1890ff;
      font-weight: 400;
    }
    
    p {
      margin: 10px 0;
      color: #666;
      font-size: 16px;
    }
  }
  
  .dialog-footer {
    display: flex;
    justify-content: center;
    gap: 20px;
    
    .el-button {
      padding: 10px 25px;
      
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
  
  .image-error {
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    width: 100%;
    height: 100%;
    background-color: #f5f7fa;
    color: #909399;
    
    .el-icon {
      font-size: 30px;
      margin-bottom: 10px;
    }
  }
}

@media screen and (max-width: 768px) {
  .reservation-form-container {
    .page-header {
      height: 150px;
      
      h1 {
        font-size: 28px;
      }
      
      p {
        font-size: 16px;
      }
    }
    
    .main-content {
      width: 95%;
    }
  }
}
</style> 