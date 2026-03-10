<template>
  <div class="home-container">
    <!-- 轮播图 -->
    <div class="banner-section">
      <banner-carousel height="600px" />
        </div>
    
    <!-- 房型展示 -->
    <section class="section" id="room-type-section">
      <div class="section-title">
        <div class="title-decoration"></div>
        <h2>精选房型</h2>
        <p>为您提供多种舒适的住宿选择</p>
        <div class="view-all-link">
          <router-link to="/rooms" class="view-all-btn">
            查看全部房型 <el-icon><ArrowRight /></el-icon>
          </router-link>
        </div>
      </div>
      
      <div class="room-types" v-loading="loading">
        <el-row :gutter="30">
          <el-col :xs="24" :sm="12" :md="8" v-for="roomType in roomTypes" :key="roomType.id" class="room-col">
            <div class="room-type-card">
              <div class="room-type-image">
                <el-image :src="getImageUrl(roomType.image)" fit="cover">
                  <template #error>
                    <div class="image-error">
                      <el-icon><Picture /></el-icon>
                    </div>
                  </template>
                </el-image>
                <div class="room-type-price">
                  <span class="price">¥{{ roomType.price }}</span>
                  <span class="unit">/晚</span>
                </div>
              </div>
              <div class="room-type-content">
                <h3 class="room-type-name">{{ roomType.name }}</h3>
                <div class="room-type-info">
                  <p><el-icon><User /></el-icon> 最多入住{{ roomType.maxPeople }}人</p>
                  <p><el-icon><Bed /></el-icon> {{ roomType.bedType }}</p>
                </div>
                <p class="room-type-facilities">{{ roomType.facilities }}</p>
                <div class="room-type-footer">
                  <el-button type="primary" @click="selectRoomType(roomType.id)">
                    立即预约
                  </el-button>
                  <span class="available-count">
                    {{ roomType.availableRoomCount }}/{{ roomType.roomCount }}间可用
                  </span>
                </div>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>
    </section>
    
    <!-- 预约表单 -->
    <div class="section booking-section" id="booking-section">
      <div class="section-title">
        <div class="title-decoration"></div>
        <h2>在线预约</h2>
        <p>填写以下信息即可完成预约</p>
      </div>
      
      <div class="booking-card">
        <el-form :model="bookingForm" :rules="bookingRules" ref="bookingFormRef" label-width="100px">
          <el-row :gutter="30">
            <el-col :xs="24" :sm="12">
              <el-form-item label="房间类型" prop="roomTypeId">
                <el-select v-model="bookingForm.roomTypeId" placeholder="请选择房型" style="width: 100%">
                  <el-option v-for="item in roomTypes" :key="item.id" :label="item.name" :value="item.id" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="入住日期" prop="dateRange">
                <el-date-picker
                  v-model="bookingForm.dateRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="入住日期"
                  end-placeholder="退房日期"
                  :disabled-date="disabledDate"
                  value-format="YYYY-MM-DD"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="30">
            <el-col :xs="24" :sm="12">
              <el-form-item label="入住人数" prop="guestCount">
                <el-input-number v-model="bookingForm.guestCount" :min="1" :max="10" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="联系人" prop="guestName">
                <el-input v-model="bookingForm.guestName" placeholder="请输入联系人姓名" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="30">
            <el-col :xs="24" :sm="12">
              <el-form-item label="联系电话" prop="guestPhone">
                <el-input v-model="bookingForm.guestPhone" placeholder="请输入联系电话" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="总价">
                <div class="price-display">
                  <span class="total-price">¥{{ calculateTotalPrice() }}</span>
                  <span class="price-desc" v-if="bookingForm.dateRange && bookingForm.dateRange.length === 2">
                    {{ calculateDays() }}晚
                  </span>
                </div>
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="备注" prop="notes">
            <el-input type="textarea" v-model="bookingForm.notes" placeholder="请输入备注信息" rows="3" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="submitReservation" :loading="submitLoading">
              提交预约
            </el-button>
            <el-button @click="resetBookingForm">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
    
    <!-- 酒店信息 -->
    <div class="section hotel-info-section">
      <div class="section-title">
        <div class="title-decoration"></div>
        <h2>酒店信息</h2>
      </div>
      <el-row :gutter="30">
        <el-col :xs="24" :sm="12" :md="8">
          <div class="info-card">
            <el-icon class="info-icon"><Location /></el-icon>
            <h3>酒店地址</h3>
            <p>SDJNSDU1500</p>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8">
          <div class="info-card">
            <el-icon class="info-icon"><Phone /></el-icon>
            <h3>联系电话</h3>
            <p>000-8888</p>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8">
          <div class="info-card">
            <el-icon class="info-icon"><Message /></el-icon>
            <h3>电子邮箱</h3>
            <p>XXX@XX.com</p>
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Bed, Location, Phone, Message, Picture, ArrowRight } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'
import BannerCarousel from '@/components/BannerCarousel.vue'

const router = useRouter()
const userStore = useUserStore()

// 轮播图
const carouselImages = ref([
  '/img/carousel/hotel1.jpg',
  '/img/carousel/hotel2.jpg',
  '/img/carousel/hotel3.jpg'
])

// 房型列表
const roomTypes = ref([])
const loading = ref(false)
const submitLoading = ref(false)

// 预约表单
const bookingFormRef = ref(null)
const bookingForm = reactive({
  roomTypeId: null,
  dateRange: [],
  guestCount: 1,
  guestName: '',
  guestPhone: '',
  notes: ''
})

// 表单验证规则
const bookingRules = {
  roomTypeId: [
    { required: true, message: '请选择房间类型', trigger: 'change' }
  ],
  dateRange: [
    { required: true, message: '请选择入住日期和退房日期', trigger: 'change' }
  ],
  guestCount: [
    { required: true, message: '请输入入住人数', trigger: 'blur' },
    { type: 'number', min: 1, message: '入住人数不能小于1', trigger: 'blur' }
  ],
  guestName: [
    { required: true, message: '请输入联系人姓名', trigger: 'blur' }
  ],
  guestPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
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
    await request.get('/roomType/all',null, {
      onSuccess: (res) => {
        roomTypes.value = res
        
        // 如果URL参数中有预选的房型ID，则自动选中
        const params = new URLSearchParams(window.location.search)
        const roomTypeId = params.get('roomTypeId')
        if (roomTypeId) {
          bookingForm.roomTypeId = Number(roomTypeId)
          scrollToBooking()
        }
      }
    })
  } catch (error) {
    console.error('获取房型列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 计算总价
const calculateTotalPrice = () => {
  if (!bookingForm.roomTypeId || !bookingForm.dateRange || bookingForm.dateRange.length !== 2) {
    return '0.00'
  }
  
  const roomType = roomTypes.value.find(item => item.id === bookingForm.roomTypeId)
  if (!roomType) return '0.00'
  
  const days = calculateDays()
  const total = roomType.price * days
  
  return total.toFixed(2)
}

// 计算入住天数
const calculateDays = () => {
  if (!bookingForm.dateRange || bookingForm.dateRange.length !== 2) return 0
  
  const startDate = new Date(bookingForm.dateRange[0])
  const endDate = new Date(bookingForm.dateRange[1])
  const diffTime = Math.abs(endDate - startDate)
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))
  
  return diffDays
}

// 禁用的日期（今天之前的日期不可选）
const disabledDate = (time) => {
  return time.getTime() < Date.now() - 8.64e7 // 禁用今天之前的日期
}

// 选择房型
const selectRoomType = (id) => {
  bookingForm.roomTypeId = id
  scrollToBooking()
}

// 滚动到预约表单
const scrollToBooking = () => {
  const bookingSection = document.getElementById('booking-section')
  if (bookingSection) {
    bookingSection.scrollIntoView({ behavior: 'smooth' })
  }
}

// 跳转到房间展示页面
const goToRoomDisplay = () => {
  router.push('/rooms')
}

// 滚动到房间列表
const scrollToRoomList = () => {
  const roomListSection = document.getElementById('room-type-section')
  if (roomListSection) {
    roomListSection.scrollIntoView({ behavior: 'smooth' })
  }
}

// 提交预约
const submitReservation = async () => {
  if (!bookingForm.dateRange || bookingForm.dateRange.length !== 2) {
    ElMessage.warning('请选择入住和退房日期')
    return
  }

  if (!bookingForm.guestName || !bookingForm.guestPhone) {
    ElMessage.warning('请填写入住人信息')
    return
  }

  // 检查是否登录
  if (!userStore.isLoggedIn) {
    ElMessageBox.confirm(
      '预约需要登录，是否前往登录页面？',
      '提示',
      {
        confirmButtonText: '去登录',
        cancelButtonText: '取消',
        type: 'warning'
      }
    ).then(() => {
      router.push('/login')
    }).catch(() => {})
    return
  }

  submitLoading.value = true
  
  try {
    // 准备预约数据
    const reservationData = {
      roomTypeId: bookingForm.roomTypeId,
      startDate: bookingForm.dateRange[0],
      endDate: bookingForm.dateRange[1],
      guestCount: bookingForm.guestCount,
      guestName: bookingForm.guestName,
      guestPhone: bookingForm.guestPhone,
      notes: bookingForm.notes
    }
    
    // 提交预约
    let reservationId = null
    await request.post('/reservation/create', reservationData, {
      successMsg: '预约提交成功',
      onSuccess: async (res) => {
        reservationId = res.id
        
        // 创建订单
        if (reservationId) {
          try {
            await request.post(`/order/create/${reservationId}`, null, {
              successMsg: '订单已创建',
              onSuccess: (orderRes) => {
                // 询问是否跳转到支付页面
                ElMessageBox.confirm(
                  '预约已提交，是否立即支付？',
                  '预约成功',
                  {
                    confirmButtonText: '去支付',
                    cancelButtonText: '稍后支付',
                    type: 'success'
                  }
                ).then(() => {
                  // 跳转到订单页面
                  router.push('/profile/order')
                }).catch(() => {
                  // 用户选择稍后支付，跳转到预约列表
                  router.push('/profile/reservation')
                })
              }
            })
          } catch (error) {
            console.error('创建订单失败:', error)
            // 如果创建订单失败，仍然跳转到预约列表
            router.push('/profile/reservation')
          }
        }
      }
    })
  } catch (error) {
    console.error('提交预约失败:', error)
  } finally {
    submitLoading.value = false
  }
}

// 重置预约表单
const resetBookingForm = () => {
  if (bookingFormRef.value) {
    bookingFormRef.value.resetFields()
  }
}

// 生命周期钩子
onMounted(() => {
  fetchRoomTypes()
  
  // 如果用户已登录，自动填充联系人信息
  if (userStore.isLoggedIn && userStore.userInfo) {
    bookingForm.guestName = userStore.userInfo.name || ''
    bookingForm.guestPhone = userStore.userInfo.phone || ''
  }
})
</script>

<style lang="scss" scoped>
// 极简风格 + 蓝色点缀样式
.home-container {
  background-color: #ffffff;
  color: #333;
  font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
  
  .banner-section {
    width: 90%;
    max-width: 1200px;
    margin: 0 auto 60px;
  }
  
  .section {
    padding: 80px 20px;
    max-width: 1200px;
    margin: 0 auto;
    position: relative;
    
    &:nth-child(even) {
      background-color: #f8fafb;
    }
    
    .section-title {
      text-align: center;
      margin-bottom: 50px;
      position: relative;
      
      .title-decoration {
        width: 60px;
        height: 3px;
        background: linear-gradient(90deg, #ffffff, #1890ff, #ffffff);
        margin: 0 auto 20px;
      }
      
      h2 {
        font-size: 32px;
        margin-bottom: 12px;
        color: #333;
        font-weight: 300;
        letter-spacing: 1px;
      }
      
      p {
        font-size: 16px;
        color: #888;
        max-width: 500px;
        margin: 0 auto;
      }
      
      .view-all-link {
        margin-top: 20px;
        
        .view-all-btn {
          display: inline-flex;
          align-items: center;
          color: #1890ff;
          font-size: 16px;
          text-decoration: none;
          transition: all 0.3s;
          
          &:hover {
            color: #40a9ff;
            
            .el-icon {
              transform: translateX(5px);
            }
          }
          
          .el-icon {
            margin-left: 5px;
            transition: transform 0.3s;
          }
        }
      }
    }
    
    .room-types {
      .room-col {
        margin-bottom: 40px;
      }
      
      .room-type-card {
        height: 100%;
        border-radius: 8px;
        overflow: hidden;
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
        transition: all 0.3s ease;
        background-color: #fff;
        
        &:hover {
          transform: translateY(-10px);
          box-shadow: 0 15px 30px rgba(0, 0, 0, 0.1);
          
          .room-type-image .el-image {
            transform: scale(1.05);
          }
        }
        
        .room-type-image {
          height: 220px;
          overflow: hidden;
          position: relative;
          
          .el-image {
            width: 100%;
            height: 100%;
            transition: transform 0.5s ease;
          }
          
          .room-type-price {
            position: absolute;
            bottom: 15px;
            right: 15px;
            background-color: rgba(0, 0, 0, 0.7);
            color: #fff;
            padding: 8px 15px;
            border-radius: 4px;
            
            .price {
              font-size: 20px;
              font-weight: bold;
            }
            
            .unit {
              font-size: 14px;
              opacity: 0.8;
            }
          }
        }
        
        .room-type-content {
          padding: 20px;
          
          .room-type-name {
            margin: 0 0 15px;
            font-size: 20px;
            color: #333;
          }
          
          .room-type-info {
            display: flex;
            justify-content: space-between;
            margin-bottom: 15px;
            
            p {
              margin: 0;
              color: #666;
              display: flex;
              align-items: center;
              
              .el-icon {
                margin-right: 5px;
                color: #1890ff;
              }
            }
          }
          
          .room-type-facilities {
            margin: 0 0 20px;
            color: #888;
            font-size: 14px;
            line-height: 1.5;
            height: 42px;
            overflow: hidden;
            text-overflow: ellipsis;
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;
          }
          
          .room-type-footer {
            display: flex;
            justify-content: space-between;
            align-items: center;
            
            .el-button {
              border-radius: 4px;
              padding: 10px 20px;
            }
            
            .available-count {
              font-size: 14px;
              color: #888;
            }
          }
        }
      }
    }
  }
  
  .booking-section {
    background-color: #f8fafb;
    
    .booking-card {
      max-width: 900px;
      margin: 0 auto;
      padding: 40px;
      background-color: #fff;
      border-radius: 8px;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
    }
    
    .price-display {
      display: flex;
      align-items: baseline;
      
      .total-price {
        font-size: 24px;
        font-weight: bold;
        color: #1890ff;
      }
      
      .price-desc {
        margin-left: 10px;
        color: #888;
      }
    }
    
    .el-form-item {
      margin-bottom: 25px;
      
      :deep(.el-form-item__label) {
        color: #555;
      }
    }
    
    .el-button {
      padding: 12px 25px;
      font-size: 16px;
      border-radius: 4px;
      
      &.el-button--primary {
        background-color: #1890ff;
        border-color: #1890ff;
        
        &:hover {
          background-color: #40a9ff;
          border-color: #40a9ff;
        }
      }
    }
  }
  
  .hotel-info-section {
    .info-card {
      text-align: center;
      padding: 40px 20px;
      margin-bottom: 30px;
      border-radius: 8px;
      background-color: #fff;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
      transition: all 0.3s;
      
      &:hover {
        transform: translateY(-5px);
        box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
        
        .info-icon {
          color: #1890ff;
          transform: scale(1.1);
        }
      }
      
      .info-icon {
        font-size: 40px;
        color: #555;
        margin-bottom: 20px;
        transition: all 0.3s;
      }
      
      h3 {
        margin-bottom: 15px;
        color: #333;
        font-weight: 500;
      }
      
      p {
        color: #666;
        margin: 0;
      }
    }
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
}
</style>


