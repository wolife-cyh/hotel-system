<template>
  <div class="room-detail-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-content">
        <h1>{{ roomType ? roomType.name : '房间详情' }}</h1>
        <div class="title-decoration"></div>
      </div>
    </div>
    
    <div class="room-detail-content" v-loading="loading">
      <div v-if="!roomType" class="no-data">
        <el-empty description="房间信息不存在" />
      </div>
      
      <div v-else class="room-detail-wrapper">
        <!-- 房型图片轮播 -->
        <div class="room-gallery">
          <el-carousel height="400px" arrow="always" indicator-position="outside">
            <el-carousel-item v-if="roomType.image">
              <el-image :src="getImageUrl(roomType.image)" fit="cover" style="width: 100%; height: 100%;" />
            </el-carousel-item>
            <template v-if="roomType.images && roomType.images.length > 0">
              <el-carousel-item v-for="(image, index) in roomType.images" :key="index">
                <el-image :src="getImageUrl(image.imageUrl)" fit="cover" style="width: 100%; height: 100%;" />
              </el-carousel-item>
            </template>
            <el-carousel-item v-if="!roomType.image && (!roomType.images || roomType.images.length === 0)">
              <div class="image-error">
                <el-icon><Picture /></el-icon>
                <p>暂无图片</p>
              </div>
            </el-carousel-item>
          </el-carousel>
        </div>
        
        <!-- 房型信息与预订板块 -->
        <div class="room-info-booking">
          <div class="room-info-section">
            <h2 class="room-name">{{ roomType.name }}</h2>
            
            <div class="room-tags">
              <el-tag size="medium" effect="plain" type="info">{{ roomType.bedType }}</el-tag>
              <el-tag size="medium" effect="plain" type="success">{{ roomType.maxPeople }}人</el-tag>
              <el-tag size="medium" effect="plain" type="warning" v-if="availableRoomCount > 0">
                剩余{{ availableRoomCount }}间
              </el-tag>
              <el-tag size="medium" effect="plain" type="danger" v-else>已满</el-tag>
            </div>
            
            <div class="room-price">
              <span class="price-label">价格:</span>
              <span class="price-value">¥{{ roomType.price }}</span>
              <span class="price-unit">/晚</span>
            </div>
            
            <el-divider />
            
            <div class="room-facilities">
              <h3>设施与服务</h3>
              <div class="facilities-list">
                <span class="facility-item" v-for="(facility, index) in getFeaturesArray(roomType.facilities)" :key="index">
                  <el-icon><Check /></el-icon> {{ facility }}
                </span>
              </div>
            </div>
            
            <el-divider />
            
            <div class="room-description">
              <h3>房间描述</h3>
              <p>{{ roomType.description }}</p>
            </div>
          </div>
          
          <div class="booking-section">
            <el-card shadow="hover">
              <template #header>
                <div class="booking-header">
                  <h3>预订信息</h3>
                </div>
              </template>
              
              <el-form :model="bookingForm" :rules="bookingRules" ref="bookingFormRef">
                <el-form-item label="入住日期" prop="dateRange" required>
                  <el-date-picker
                    v-model="bookingForm.dateRange"
                    type="daterange"
                    range-separator="至"
                    start-placeholder="入住日期"
                    end-placeholder="退房日期"
                    :disabled-date="disabledDate"
                    value-format="YYYY-MM-DD"
                    style="width: 100%;"
                    @change="handleDateChange"
                  />
                </el-form-item>
                
                <div class="price-summary" v-if="bookingForm.dateRange && bookingForm.dateRange.length === 2">
                  <div class="price-item">
                    <span>房间单价</span>
                    <span>¥{{ roomType.price }} / 晚</span>
                  </div>
                  <div class="price-item">
                    <span>入住天数</span>
                    <span>{{ calculateDays() }}晚</span>
                  </div>
                  <div class="price-item total">
                    <span>总价</span>
                    <span class="total-price">¥{{ calculateTotalPrice() }}</span>
                  </div>
                </div>
                
                <el-form-item>
                  <el-button 
                    type="primary" 
                    :disabled="availableRoomCount <= 0 || !bookingForm.dateRange || bookingForm.dateRange.length !== 2" 
                    @click="handleBooking"
                    style="width: 100%;"
                  >
                    立即预订
                  </el-button>
                </el-form-item>
              </el-form>
            </el-card>
          </div>
        </div>
        
        <!-- 用户评价部分 -->
        <div class="reviews-section">
          <h2 class="section-title">用户评价</h2>
          <div class="title-decoration"></div>
          <RoomReviews :room-id="roomTypeId" :show-add-review-button="true" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Check, Picture } from '@element-plus/icons-vue'
import request from '@/utils/request'
import RoomReviews from '@/components/frontend/RoomReviews.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const roomType = ref(null)
const roomTypeId = computed(() => route.params.id)
const availableRoomCount = ref(0)
const bookingFormRef = ref(null)

// 预订表单
const bookingForm = reactive({
  dateRange: null
})

// 表单验证规则
const bookingRules = {
  dateRange: [
    { required: true, message: '请选择入住日期', trigger: 'change' }
  ]
}

// 获取房型信息
const fetchRoomType = async () => {
  if (!roomTypeId.value) return
  
  loading.value = true
  try {
    await request.get(`/roomType/${roomTypeId.value}`, null, {
      onSuccess: (res) => {
        roomType.value = res
        // 获取可用房间数
        checkAvailableRooms()
      }
    })
  } catch (error) {
    console.error('获取房型信息失败:', error)
  } finally {
    loading.value = false
  }
}

// 禁用的日期（今天之前的日期不可选）
const disabledDate = (time) => {
  return time.getTime() < Date.now() - 8.64e7 // 禁用今天之前的日期
}

// 检查可用房间数
const checkAvailableRooms = async () => {
  if (!bookingForm.dateRange || bookingForm.dateRange.length !== 2) {
    // 如果没有选择日期，默认有5间可用
    availableRoomCount.value = 5
    return
  }
  
  try {
    await request.get('/room/available', {
      startDate: bookingForm.dateRange[0],
      endDate: bookingForm.dateRange[1]
    }, {
      onSuccess: (res) => {
        const availableRooms = res.filter(room => room.roomTypeId === parseInt(roomTypeId.value))
        availableRoomCount.value = availableRooms.length
      }
    })
  } catch (error) {
    console.error('获取可用房间失败:', error)
  }
}

// 处理日期变化
const handleDateChange = () => {
  checkAvailableRooms()
}

// 计算入住天数
const calculateDays = () => {
  if (!bookingForm.dateRange || bookingForm.dateRange.length !== 2) return 0
  
  try {
    // 解析日期字符串为Date对象
    const startDate = new Date(bookingForm.dateRange[0])
    const endDate = new Date(bookingForm.dateRange[1])
    
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
}

// 计算总价
const calculateTotalPrice = () => {
  if (!roomType.value) return '0.00'
  const days = calculateDays()
  return (roomType.value.price * days).toFixed(2)
}

// 处理预订
const handleBooking = () => {
  if (!bookingFormRef.value) return
  
  bookingFormRef.value.validate(valid => {
    if (valid) {
      if (availableRoomCount.value <= 0) {
        ElMessage.warning('该房型已满，请选择其他房型或日期')
        return
      }
      
      // 跳转到预订页面，并传递相关参数
      router.push({
        path: '/reservation',
        query: {
          roomTypeId: roomTypeId.value,
          startDate: bookingForm.dateRange[0],
          endDate: bookingForm.dateRange[1]
        }
      })
    }
  })
}

// 获取图片完整URL
const baseAPI = process.env.VUE_APP_BASE_API || '/api'
const getImageUrl = (image) => {
  if (!image) return ''
  if (image.startsWith('http')) return image
  return baseAPI + image
}

// 辅助函数：将设施字符串转换为数组
const getFeaturesArray = (facilities) => {
  if (!facilities) return []
  return facilities.split('、')
}

// 初始化日期范围
const initDateRange = () => {
  const today = new Date()
  const tomorrow = new Date()
  tomorrow.setDate(today.getDate() + 1)
  
  // 格式化为YYYY-MM-DD
  const formatDate = (date) => {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }
  
  bookingForm.dateRange = [formatDate(today), formatDate(tomorrow)]
}

// 生命周期钩子
onMounted(() => {
  initDateRange()
  fetchRoomType()
})
</script>

<style lang="scss" scoped>
.room-detail-container {
  font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
  color: #333;
  
  .page-header {
    height: 150px;
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
      
      .title-decoration {
        width: 60px;
        height: 3px;
        background-color: #1890ff;
        margin: 10px auto;
      }
    }
  }
  
  .room-detail-content {
    width: 90%;
    max-width: 1200px;
    margin: 0 auto 60px;
    
    .no-data {
      padding: 100px 0;
      text-align: center;
    }
    
    .room-detail-wrapper {
      .room-gallery {
        margin-bottom: 30px;
        border-radius: 8px;
        overflow: hidden;
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
        
        .el-carousel {
          height: 400px;
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
            font-size: 60px;
            margin-bottom: 20px;
          }
          
          p {
            margin: 0;
            font-size: 16px;
          }
        }
      }
      
      .room-info-booking {
        display: flex;
        gap: 30px;
        margin-bottom: 50px;
        
        .room-info-section {
          flex: 2;
          background-color: #fff;
          border-radius: 8px;
          padding: 30px;
          box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
          
          .room-name {
            font-size: 28px;
            font-weight: 600;
            margin-top: 0;
            margin-bottom: 20px;
            color: #1890ff;
          }
          
          .room-tags {
            margin-bottom: 20px;
            
            .el-tag {
              margin-right: 10px;
              padding: 6px 12px;
              font-size: 14px;
            }
          }
          
          .room-price {
            margin-bottom: 20px;
            font-size: 16px;
            
            .price-label {
              margin-right: 10px;
              font-weight: 500;
            }
            
            .price-value {
              font-size: 28px;
              font-weight: bold;
              color: #1890ff;
            }
            
            .price-unit {
              margin-left: 5px;
              color: #606266;
            }
          }
          
          h3 {
            font-size: 20px;
            margin-top: 0;
            margin-bottom: 15px;
            font-weight: 500;
            color: #1890ff;
          }
          
          .facilities-list {
            display: flex;
            flex-wrap: wrap;
            
            .facility-item {
              width: 33.33%;
              display: flex;
              align-items: center;
              margin-bottom: 12px;
              font-size: 14px;
              
              .el-icon {
                color: #1890ff;
                margin-right: 8px;
              }
            }
          }
          
          .room-description p {
            line-height: 1.8;
            color: #606266;
            margin-top: 0;
          }
        }
        
        .booking-section {
          flex: 1;
          
          .el-card {
            border-radius: 8px;
            
            .booking-header {
              h3 {
                margin: 0;
                font-size: 18px;
                font-weight: 600;
                color: #1890ff;
              }
            }
            
            .price-summary {
              background-color: #f8f9fa;
              border-radius: 6px;
              padding: 15px;
              margin: 15px 0;
              
              .price-item {
                display: flex;
                justify-content: space-between;
                margin-bottom: 10px;
                
                &.total {
                  margin-top: 15px;
                  padding-top: 10px;
                  border-top: 1px dashed #dcdfe6;
                  font-weight: bold;
                  
                  .total-price {
                    color: #1890ff;
                    font-size: 20px;
                  }
                }
              }
            }
          }
        }
      }
      
      .reviews-section {
        background-color: #fff;
        border-radius: 8px;
        padding: 30px;
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
        
        .section-title {
          font-size: 24px;
          font-weight: 600;
          margin-top: 0;
          margin-bottom: 10px;
          text-align: center;
        }
        
        .title-decoration {
          width: 60px;
          height: 3px;
          background-color: #1890ff;
          margin: 0 auto 30px;
        }
      }
    }
  }
}

@media (max-width: 992px) {
  .room-detail-container {
    .room-detail-content {
      .room-detail-wrapper {
        .room-info-booking {
          flex-direction: column;
        }
      }
    }
  }
}
</style> 