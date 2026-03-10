<template>
  <div class="room-display-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-content">
        <h1>房间预订</h1>
        <p>为您献上温馨的小屋</p>
      </div>
    </div>
    
    <!-- 过滤器 -->
    <div class="filter-section">
      <div class="filter-card">
        <el-form :inline="true" :model="filterForm" class="filter-form">
          <el-form-item label="入住日期">
            <el-date-picker
              v-model="filterForm.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="入住日期"
              end-placeholder="退房日期"
              :disabled-date="disabledDate"
              value-format="YYYY-MM-DD"
              @change="handleDateChange"
            />
          </el-form-item>
          <el-form-item label="房型">
            <el-select v-model="filterForm.roomTypeId" placeholder="全部房型" clearable>
              <el-option v-for="item in roomTypes" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="入住人数">
            <el-select v-model="filterForm.guestCount" placeholder="全部" clearable>
              <el-option label="1人" :value="1" />
              <el-option label="2人" :value="2" />
              <el-option label="3人" :value="3" />
              <el-option label="4人及以上" :value="4" />
            </el-select>
          </el-form-item>
          <el-form-item label="价格区间" style="width: 200px;">
            <el-slider
              v-model="filterForm.priceRange"
              range
              :min="minPrice"
              :max="maxPrice"
              :step="50"
              :marks="priceMarks"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
            <el-button @click="resetFilter">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
    
    <!-- 房间列表 -->
    <div class="room-list-section" v-loading="loading">
      <div v-if="filteredRoomTypes.length === 0" class="no-data">
        <el-empty description="暂无符合条件的房型" />
      </div>
      
      <el-row :gutter="30" v-else>
        <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="roomType in filteredRoomTypes" :key="roomType.id" class="room-col">
          <div class="room-card">
            <div class="room-image">
              <el-image :src="getImageUrl(roomType.image)" fit="cover" :preview-src-list="getPreviewImages(roomType)"  :preview-teleported="true">
                <template #error>
                  <div class="image-error">
                    <el-icon><Picture /></el-icon>
                  </div>
                </template>
              </el-image>
              <div class="room-price">
                <span class="price">¥{{ roomType.price }}</span>
                <span class="unit">/晚</span>
              </div>
            </div>
            
            <div class="room-info">
              <h3 class="room-name">{{ roomType.name }}</h3>
              <div class="room-tags">
                <el-tag size="small" effect="plain" type="info">{{ roomType.bedType }}</el-tag>
                <el-tag size="small" effect="plain" type="success">{{ roomType.maxPeople }}人</el-tag>
                <el-tag size="small" effect="plain" type="warning" v-if="roomType.availableRoomCount > 0">
                  剩余{{ roomType.availableRoomCount }}间
                </el-tag>
                <el-tag size="small" effect="plain" type="danger" v-else>已满</el-tag>
              </div>
              <p class="room-desc">{{ truncateText(roomType.description, 50) }}</p>
              <div class="room-features">
                <span class="feature" v-for="(feature, index) in getFeaturesArray(roomType.facilities)" :key="index">
                  <el-icon><Check /></el-icon> {{ feature }}
                </span>
              </div>
              <div class="room-actions">
                <el-button type="primary" plain @click="viewRoomDetail(roomType)">查看详情</el-button>
                <el-button 
                  type="primary" 
                  :disabled="roomType.availableRoomCount <= 0 || !filterForm.dateRange || filterForm.dateRange.length !== 2" 
                  @click="bookRoom(roomType)"
                >
                  立即预订
                </el-button>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>
    
    <!-- 房型详情对话框 -->
    <el-dialog v-model="detailDialogVisible" :title="currentRoomType.name" width="700px">
      <div class="room-detail">
        <el-carousel height="300px">
          <el-carousel-item v-if="currentRoomType.image">
            <el-image :src="getImageUrl(currentRoomType.image)" fit="cover" style="width: 100%; height: 100%;" />
          </el-carousel-item>
          <template v-if="currentRoomType.images && currentRoomType.images.length > 0">
            <el-carousel-item v-for="(image, index) in currentRoomType.images" :key="index">
              <el-image :src="getImageUrl(image.imageUrl)" fit="cover" style="width: 100%; height: 100%;" />
            </el-carousel-item>
          </template>
          <el-carousel-item v-if="!currentRoomType.image && (!currentRoomType.images || currentRoomType.images.length === 0)">
            <div class="image-error">
              <el-icon><Picture /></el-icon>
              <p>暂无图片</p>
            </div>
          </el-carousel-item>
        </el-carousel>
        
        <div class="detail-info">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="房型">{{ currentRoomType.name }}</el-descriptions-item>
            <el-descriptions-item label="价格">
              <span class="price-highlight">¥{{ currentRoomType.price }}</span>/晚
            </el-descriptions-item>
            <el-descriptions-item label="床型">{{ currentRoomType.bedType }}</el-descriptions-item>
            <el-descriptions-item label="最大入住人数">{{ currentRoomType.maxPeople }}人</el-descriptions-item>
            <el-descriptions-item label="可用房间数" :span="2">
              <el-tag type="success" v-if="currentRoomType.availableRoomCount > 5">充足</el-tag>
              <el-tag type="warning" v-else-if="currentRoomType.availableRoomCount > 0">
                仅剩{{ currentRoomType.availableRoomCount }}间
              </el-tag>
              <el-tag type="danger" v-else>已满</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="设施" :span="2">
              <div class="facilities-list">
                <span class="facility-item" v-for="(facility, index) in getFeaturesArray(currentRoomType.facilities)" :key="index">
                  <el-icon><Check /></el-icon> {{ facility }}
                </span>
              </div>
            </el-descriptions-item>
            <el-descriptions-item label="房间描述" :span="2">
              {{ currentRoomType.description }}
            </el-descriptions-item>
          </el-descriptions>
        </div>
        
        <div class="booking-section" v-if="filterForm.dateRange && filterForm.dateRange.length === 2">
          <el-divider content-position="center">预订信息</el-divider>
          <div class="booking-info">
            <p>入住日期: {{ filterForm.dateRange[0] }}</p>
            <p>退房日期: {{ filterForm.dateRange[1] }}</p>
            <p>入住天数: {{ calculateDays() }}晚</p>
            <p>总价: <span class="price-highlight">¥{{ calculateTotalPrice(currentRoomType.price) }}</span></p>
          </div>
        </div>
        
        <!-- 用户评价部分 -->
        <div class="reviews-section">
          <el-divider content-position="center">用户评价</el-divider>
          <RoomReviews :room-id="currentRoomType.id" />
        </div>
      </div>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="detailDialogVisible = false">关闭</el-button>
          <el-button 
            type="primary" 
            :disabled="currentRoomType.availableRoomCount <= 0 || !filterForm.dateRange || filterForm.dateRange.length !== 2" 
            @click="bookCurrentRoom"
          >
            立即预订
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Check, Picture } from '@element-plus/icons-vue'
import request from '@/utils/request'
import RoomReviews from '@/components/frontend/RoomReviews.vue'

const router = useRouter()
const loading = ref(false)
const roomTypes = ref([])
const detailDialogVisible = ref(false)
const currentRoomType = ref({})

// 过滤表单
const filterForm = reactive({
  dateRange: null,
  roomTypeId: null,
  guestCount: null,
  priceRange: [0, 1000]
})

// 初始化过滤表单
const initFilterForm = () => {
  // 设置默认日期范围为今天和明天
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
  
  filterForm.dateRange = [formatDate(today), formatDate(tomorrow)]
  console.log('初始化过滤表单，设置默认日期:', filterForm.dateRange)
}

// 价格范围计算
const minPrice = ref(0)
const maxPrice = ref(1000)
const priceMarks = computed(() => {
  return {
    [minPrice.value]: minPrice.value + '元',
    [maxPrice.value]: maxPrice.value + '元'
  }
})

// 过滤后的房型列表
const filteredRoomTypes = computed(() => {
  if (!roomTypes.value.length) return []
  
  return roomTypes.value.filter(roomType => {
    // 房型过滤
    if (filterForm.roomTypeId && roomType.id !== filterForm.roomTypeId) {
      return false
    }
    
    // 人数过滤
    if (filterForm.guestCount && roomType.maxPeople < filterForm.guestCount) {
      return false
    }
    
    // 价格过滤
    if (roomType.price < filterForm.priceRange[0] || roomType.price > filterForm.priceRange[1]) {
      return false
    }
    
    return true
  })
})

// 获取图片URL
const baseAPI = process.env.VUE_APP_BASE_API || '/api'
const getImageUrl = (image) => {
  if (!image) return ''
  if (image.startsWith('http')) return image
  return baseAPI + image
}

// 获取预览图片列表
const getPreviewImages = (roomType) => {
  const images = []
  
  // 添加主图
  if (roomType.image) {
    images.push(getImageUrl(roomType.image))
  }
  
  // 添加附加图片
  if (roomType.images && roomType.images.length > 0) {
    roomType.images.forEach(img => {
      if (img.imageUrl) {
        images.push(getImageUrl(img.imageUrl))
      }
    })
  }
  
  // 如果没有图片，添加一个占位图
  if (images.length === 0) {
    images.push('https://via.placeholder.com/800x450?text=No+Image')
  }
  
  return images
}

// 禁用的日期（今天之前的日期不可选）
const disabledDate = (time) => {
  return time.getTime() < Date.now() - 8.64e7 // 禁用今天之前的日期
}

// 加载房型数据
const fetchRoomTypes = async () => {
  loading.value = true
  try {
    await request.get('/roomType/all', null, {
      onSuccess: (res) => {
        roomTypes.value = res
        
        // 更新价格范围
        if (roomTypes.value.length > 0) {
          const prices = roomTypes.value.map(item => item.price)
          minPrice.value = Math.floor(Math.min(...prices))
          maxPrice.value = Math.ceil(Math.max(...prices))
          filterForm.priceRange = [minPrice.value, maxPrice.value]
        }
        
        console.log('房型数据加载成功，总数:', roomTypes.value.length)
        
        // 查询可用房间数
        checkAvailableRooms()
      }
    })
  } catch (error) {
    console.error('获取房型列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 检查可用房间数
const checkAvailableRooms = async () => {
  if (!filterForm.dateRange || filterForm.dateRange.length !== 2) {
    // 如果没有选择日期，默认所有房间都有可用数量
    roomTypes.value.forEach(roomType => {
      roomType.availableRoomCount = 5 // 默认每种房型有5间可用
    })
    return
  }
  
  try {
    console.log('检查可用房间，日期范围:', filterForm.dateRange)
    await request.get('/room/available', {
      startDate: filterForm.dateRange[0],
      endDate: filterForm.dateRange[1]
    }, {
      onSuccess: (res) => {
        console.log('可用房间结果:', res)
        // 更新每种房型的可用房间数
        roomTypes.value.forEach(roomType => {
          const availableRooms = res.filter(room => room.roomTypeId === roomType.id)
          roomType.availableRoomCount = availableRooms.length
          console.log(`房型${roomType.id}可用房间数:`, roomType.availableRoomCount)
        })
      }
    })
  } catch (error) {
    console.error('获取可用房间失败:', error)
  }
}

// 搜索
const handleSearch = async () => {
  console.log('执行搜索，过滤条件:', filterForm)
  await checkAvailableRooms()
}

// 重置过滤器
const resetFilter = () => {
  filterForm.dateRange = null
  filterForm.roomTypeId = null
  filterForm.guestCount = null
  filterForm.priceRange = [minPrice.value, maxPrice.value]
  console.log('重置过滤器:', filterForm)
  roomTypes.value.forEach(roomType => {
    roomType.availableRoomCount = 5 // 默认每种房型有5间可用
  })
}

// 查看房型详情
const viewRoomDetail = (roomType) => {
  router.push(`/room/${roomType.id}`)
}

// 预订房间
const bookRoom = (roomType) => {
  console.log('尝试预订房间:', roomType.id, '日期范围:', filterForm.dateRange)
  if (!filterForm.dateRange || filterForm.dateRange.length !== 2) {
    ElMessage.warning('请先选择入住日期')
    return
  }
  
  // 跳转到预订页面，并传递相关参数
  router.push({
    path: '/reservation',
    query: {
      roomTypeId: roomType.id,
      startDate: filterForm.dateRange[0],
      endDate: filterForm.dateRange[1]
    }
  })
}

// 从详情对话框预订当前房间
const bookCurrentRoom = () => {
  bookRoom(currentRoomType.value)
}

// 辅助函数：截断文本
const truncateText = (text, length) => {
  if (!text) return ''
  if (text.length <= length) return text
  return text.substring(0, length) + '...'
}

// 辅助函数：将设施字符串转换为数组
const getFeaturesArray = (facilities) => {
  if (!facilities) return []
  return facilities.split('、')
}

// 计算入住天数
const calculateDays = () => {
  if (!filterForm.dateRange || filterForm.dateRange.length !== 2) return 0
  
  try {
    console.log('计算天数，日期范围:', filterForm.dateRange)
    
    // 解析日期字符串为Date对象
    const startDate = new Date(filterForm.dateRange[0])
    const endDate = new Date(filterForm.dateRange[1])
    
    console.log('解析后的日期:', startDate, endDate)
    
    // 检查日期是否有效
    if (isNaN(startDate.getTime()) || isNaN(endDate.getTime())) {
      console.error('日期解析失败')
      return 0
    }
    
    const diffTime = Math.abs(endDate - startDate)
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))
    
    console.log('计算得到的天数:', diffDays)
    return diffDays
  } catch (error) {
    console.error('计算天数出错:', error)
    return 0
  }
}

// 计算总价
const calculateTotalPrice = (price) => {
  const days = calculateDays()
  return (price * days).toFixed(2)
}

// 处理日期变化
const handleDateChange = (val) => {
  console.log('日期变化:', val)
  if (val && val.length === 2) {
    checkAvailableRooms()
  }
}

// 页面加载时获取数据
onMounted(() => {
  console.log('组件挂载，初始化数据')
  
  // 如果URL中有日期参数，自动设置日期范围
  const params = new URLSearchParams(window.location.search)
  const startDate = params.get('startDate')
  const endDate = params.get('endDate')
  if (startDate && endDate) {
    filterForm.dateRange = [startDate, endDate]
    console.log('从URL设置日期范围:', filterForm.dateRange)
  } else {
    // 否则设置默认日期范围
    initFilterForm()
  }
  
  // 获取房型数据
  fetchRoomTypes()
})
</script>

<style lang="scss" scoped>
.room-display-container {
  font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
  color: #333;
  
  .page-header {
    height: 200px;
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
  
  .filter-section {
    margin-bottom: 40px;
    width: 90%;
    max-width: 1200px;
    margin-left: auto;
    margin-right: auto;
    
    .filter-card {
      background-color: #fff;
      padding: 24px;
      border-radius: 8px;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
      
      .filter-form {
        display: flex;
        flex-wrap: wrap;
        align-items: flex-end;
        
        .el-form-item {
          margin-right: 20px;
          margin-bottom: 15px;
        }
        
        .el-button {
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
  }
  
  .room-list-section {
    width: 90%;
    max-width: 1200px;
    margin: 0 auto 60px;
    
    .room-col {
      margin-bottom: 30px;
    }
    
    .room-card {
      height: 100%;
      border-radius: 8px;
      overflow: hidden;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
      background-color: #fff;
      transition: all 0.3s ease;
      
      &:hover {
        transform: translateY(-10px);
        box-shadow: 0 15px 30px rgba(0, 0, 0, 0.1);
        
        .room-image .el-image {
          transform: scale(1.05);
        }
      }
      
      .room-image {
        height: 220px;
        position: relative;
        overflow: hidden;
        
        .el-image {
          width: 100%;
          height: 100%;
          transition: transform 0.5s;
        }
        
        .room-price {
          position: absolute;
          bottom: 15px;
          right: 15px;
          background-color: rgba(0, 0, 0, 0.7);
          color: white;
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
      
      .room-info {
        padding: 20px;
        
        .room-name {
          margin-top: 0;
          margin-bottom: 15px;
          font-size: 20px;
          font-weight: 500;
          color: #333;
        }
        
        .room-tags {
          margin-bottom: 15px;
          
          .el-tag {
            margin-right: 8px;
            margin-bottom: 5px;
            font-weight: 400;
          }
        }
        
        .room-desc {
          color: #666;
          margin-bottom: 15px;
          height: 42px;
          overflow: hidden;
          font-size: 14px;
          line-height: 1.5;
        }
        
        .room-features {
          margin-bottom: 20px;
          display: flex;
          flex-wrap: wrap;
          
          .feature {
            display: flex;
            align-items: center;
            font-size: 12px;
            color: #666;
            margin-right: 15px;
            margin-bottom: 8px;
            
            .el-icon {
              margin-right: 5px;
              color: #1890ff;
            }
          }
        }
        
        .room-actions {
          display: flex;
          justify-content: space-between;
          
          .el-button {
            flex: 1;
            margin-right: 10px;
            padding: 10px 15px;
            border-radius: 4px;
            
            &:last-child {
              margin-right: 0;
            }
            
            &.el-button--primary {
              background-color: #1890ff;
              border-color: #1890ff;
              
              &:hover, &:focus {
                background-color: #40a9ff;
                border-color: #40a9ff;
              }
              
              &.is-plain {
                color: #1890ff;
                background: #e6f7ff;
                border-color: #91d5ff;
                
                &:hover, &:focus {
                  background: #cce9ff;
                  border-color: #1890ff;
                }
              }
            }
          }
        }
      }
    }
  }
  
  .room-detail {
    .detail-info {
      margin-top: 25px;
      
      .price-highlight {
        color: #1890ff;
        font-size: 20px;
        font-weight: bold;
      }
      
      .facilities-list {
        display: flex;
        flex-wrap: wrap;
        
        .facility-item {
          width: 33.33%;
          display: flex;
          align-items: center;
          margin-bottom: 8px;
          
          .el-icon {
            color: #1890ff;
            margin-right: 5px;
          }
        }
      }
    }
    
    .booking-section {
      margin-top: 25px;
      
      .booking-info {
        background-color: #f8f9fa;
        padding: 20px;
        border-radius: 6px;
        
        p {
          margin: 10px 0;
          
          &:last-child {
            font-weight: bold;
          }
        }
        
        .price-highlight {
          color: #1890ff;
          font-size: 20px;
          font-weight: bold;
        }
      }
    }
    
    .reviews-section {
      margin-top: 25px;
    }
  }
  
  .no-data {
    padding: 60px 0;
    text-align: center;
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
      font-size: 40px;
      margin-bottom: 10px;
    }
    
    p {
      margin: 0;
      font-size: 14px;
    }
  }
}

@media screen and (max-width: 768px) {
  .room-display-container {
    .filter-section {
      .filter-form {
        flex-direction: column;
        
        .el-form-item {
          width: 100%;
          margin-right: 0;
        }
      }
    }
  }
}
</style> 