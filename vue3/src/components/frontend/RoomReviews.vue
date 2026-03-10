<template>
  <div class="room-reviews">
    <div class="reviews-header">
      <h3>用户评价 ({{ total }})</h3>
      <div class="average-score" v-if="total > 0">
        <span class="score">{{ averageScore.toFixed(1) }}</span>
        <el-rate
          v-model="averageScore"
          disabled
          text-color="#ff9900"
        />
      </div>
    </div>
    
    <div class="reviews-empty" v-if="reviews.length === 0">
      <el-empty description="暂无评价">
        <template #description>
          <p>目前还没有用户评价，预订入住后可以添加您的评价！</p>
        </template>
      </el-empty>
    </div>
    
    <div class="reviews-list" v-else>
      <div class="review-item" v-for="(review, index) in reviews" :key="review.id">
        <div class="review-user">
          <el-avatar :size="40" :src="getUserAvatar(review.user)">
            {{ getUserInitial(review.user) }}
          </el-avatar>
          <div class="user-info">
            <div class="username">{{ review.user ? review.user.username : '匿名用户' }}</div>
            <div class="review-date">{{ formatDate(review.createTime) }}</div>
          </div>
        </div>
        
        <div class="review-content">
          <div class="review-score">
            <el-rate
              v-model="review.score"
              disabled
              text-color="#ff9900"
            />
            <span class="score-text">{{ getScoreText(review.score) }}</span>
          </div>
          
          <div class="review-text">{{ review.content }}</div>
          
          <div class="review-images" v-if="review.images">
            <el-image
              v-for="(image, imgIndex) in getReviewImages(review)"
              :key="imgIndex"
              :src="getImageUrl(image)"
              :preview-src-list="getReviewImageUrls(review)"
              :preview-teleported="true"
              fit="cover"
              class="review-image"
            >
              <template #error>
                <div class="image-error">
                  <el-icon><Picture /></el-icon>
                </div>
              </template>
            </el-image>
          </div>
          
          <div class="review-reply" v-if="review.reply">
            <div class="reply-header">
              <el-icon><ChatDotRound /></el-icon>
              <span>酒店回复:</span>
            </div>
            <div class="reply-content">{{ review.reply }}</div>
            <div class="reply-time" v-if="review.replyTime">{{ formatDate(review.replyTime) }}</div>
          </div>
        </div>
        
        <el-divider v-if="index < reviews.length - 1" />
      </div>
    </div>
    
    <div class="reviews-pagination" v-if="total > pageSize">
      <el-pagination
        v-model="currentPage"
        :page-size="pageSize"
        layout="prev, pager, next"
        :total="total"
        @current-change="handlePageChange"
      />
    </div>
    
    <div class="add-review-section" v-if="showAddReviewButton">
      <div v-if="checkingReservation" class="loading-reservation">
        <el-skeleton style="width: 240px" :rows="1" animated />
      </div>
      <div v-else-if="canReview" class="can-review">
        <el-button type="primary" @click="showAddReviewDialog">写评价</el-button>
      </div>
      <div v-else class="cannot-review">
        <el-alert
          title="您尚未入住过此房型或已经评价过，无法添加评价"
          type="info"
          :closable="false"
          show-icon
        />
      </div>
      
      <el-dialog v-model="addReviewDialogVisible" title="添加评价" width="600px">
        <el-form :model="reviewForm" :rules="reviewRules" ref="reviewFormRef" label-width="100px">
          <el-form-item label="评分" prop="score">
            <el-rate
              v-model="reviewForm.score"
              show-score
              text-color="#ff9900"
              score-template="{value}"
            />
          </el-form-item>
          <el-form-item label="评价内容" prop="content">
            <el-input
              v-model="reviewForm.content"
              type="textarea"
              :rows="4"
              placeholder="请分享您的入住体验..."
            />
          </el-form-item>
          <el-form-item label="上传图片">
            <el-upload
              class="review-image-upload"
              action="#"
              :auto-upload="true"
              list-type="picture-card"
              :on-preview="handlePictureCardPreview"
              :on-remove="handleRemove"
              :http-request="customUploadImage"
              :before-upload="beforeImageUpload"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
            <el-dialog v-model="imagePreviewVisible">
              <img w-full :src="imagePreviewUrl" alt="Preview Image" />
            </el-dialog>
          </el-form-item>
        </el-form>
        <template #footer>
          <span class="dialog-footer">
            <el-button @click="addReviewDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="submitReviewForm" :loading="submitting">提交</el-button>
          </span>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Picture, ChatDotRound, Plus } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'

// 接收房间ID作为props
const props = defineProps({
  roomId: {
    type: [Number, String],
    required: true
  },
  showAddReviewButton: {
    type: Boolean,
    default: false
  }
})

// 用户信息
const userStore = useUserStore()

// 数据
const reviews = ref([])
const loading = ref(false)
const submitting = ref(false)
const currentPage = ref(1)
const pageSize = ref(5)
const total = ref(0)
const averageScore = ref(0)
const canReview = ref(false)
const completedReservation = ref(null)
const checkingReservation = ref(false)

// 评价表单
const addReviewDialogVisible = ref(false)
const reviewFormRef = ref(null)
const reviewForm = reactive({
  score: 5,
  content: '',
  images: []
})

// 评价表单验证规则
const reviewRules = {
  score: [
    { required: true, message: '请选择评分', trigger: 'change' }
  ],
  content: [
    { required: true, message: '请输入评价内容', trigger: 'blur' },
    { min: 5, message: '评价内容不能少于5个字符', trigger: 'blur' }
  ]
}

// 图片预览相关
const imagePreviewVisible = ref(false)
const imagePreviewUrl = ref('')

// 获取房间评价列表
const fetchRoomReviews = async () => {
  if (!props.roomId) return
  
  loading.value = true
  try {
    await request.get('/review/page', {
        roomId: props.roomId,
        status: 1, // 只查询显示状态的评价
        currentPage: currentPage.value,
        size: pageSize.value
    }, {
      onSuccess: (res) => {
        reviews.value = res.records || []
        total.value = res.total || 0
      }
    })
    
    // 获取平均评分
    await request.get(`/review/score/${props.roomId}`,null, {
      onSuccess: (score) => {
        averageScore.value = score || 0
      }
    })
  } catch (error) {
    console.error('获取房间评价失败:', error)
  } finally {
    loading.value = false
  }
}

// 检查用户是否有权限评价该房型
const checkUserCanReview = async () => {
  if (!userStore.isLoggedIn || !props.roomId) {
    canReview.value = false
    completedReservation.value = null
    return
  }
  
  checkingReservation.value = true
  try {
    // 查询用户已完成的预约
    await request.get('/reservation/user/completed', null, {
      onSuccess: async (res) => {
        if (!res || res.length === 0) {
          canReview.value = false
          completedReservation.value = null
          return
        }
        
        // 找到当前房型的已完成预约
        const matchedReservation = res.find(reservation => 
          reservation.room && 
          reservation.room.roomTypeId === parseInt(props.roomId)
        )
        
        if (!matchedReservation) {
          canReview.value = false
          completedReservation.value = null
          return
        }
        
        // 检查预约是否已有评价
        let hasExistingReview = false
        await request.get(`/review/reservation/${matchedReservation.id}`, null, {
          onSuccess: (review) => {
            hasExistingReview = !!review
          }
        })
        
        if (hasExistingReview) {
          canReview.value = false
          completedReservation.value = null
        } else {
          canReview.value = true
          completedReservation.value = matchedReservation
        }
      }
    })
  } catch (error) {
    console.error('检查评价权限失败:', error)
    canReview.value = false
    completedReservation.value = null
  } finally {
    checkingReservation.value = false
  }
}

// 处理页码变化
const handlePageChange = (page) => {
  currentPage.value = page
  fetchRoomReviews()
}

// 获取用户头像
const baseAPI = process.env.VUE_APP_BASE_API || '/api'
const getUserAvatar = (user) => {
  if (!user || !user.avatar) return ''
  return baseAPI + user.avatar
}

// 获取用户名首字母（用于头像显示）
const getUserInitial = (user) => {
  if (!user || !user.username) return '?'
  return user.username.substring(0, 1).toUpperCase()
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', { 
    year: 'numeric', 
    month: 'long', 
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 获取评分文字描述
const getScoreText = (score) => {
  if (score >= 5) return '非常满意'
  if (score >= 4) return '满意'
  if (score >= 3) return '一般'
  if (score >= 2) return '不满意'
  return '非常不满意'
}

// 获取评价图片数组
const getReviewImages = (review) => {
  if (!review.images) return []
  return review.images.split(',')
}

// 获取评价图片URL数组（用于预览）
const getReviewImageUrls = (review) => {
  return getReviewImages(review).map(img => getImageUrl(img))
}

// 获取图片完整URL
const getImageUrl = (path) => {
  if (!path) return ''
  return baseAPI + path
}

// 显示添加评价对话框
const showAddReviewDialog = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再添加评价')
    return
  }
  
  // 检查用户是否有权限评价
  await checkUserCanReview()
  
  if (!canReview.value) {
    ElMessage.warning('您尚未入住过此房型或已经评价过，无法添加评价')
    return
  }
  
  reviewForm.score = 5
  reviewForm.content = ''
  reviewForm.images = []
  addReviewDialogVisible.value = true
}

// 图片上传前的校验
const beforeImageUpload = (file) => {
  const isJPG = file.type === 'image/jpeg'
  const isPNG = file.type === 'image/png'
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isJPG && !isPNG) {
    ElMessage.error('图片只能是JPG或PNG格式!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过2MB!')
    return false
  }
  return true
}

// 自定义图片上传
const customUploadImage = async (options) => {
  try {
    const { file } = options

    // 创建FormData对象
    const formData = new FormData()
    formData.append('file', file)

    // 设置自定义上传选项
    const uploadOptions = {
      headers: {
        token: localStorage.getItem('token') || ''
      },
      transformRequest: [(data) => data],
      successMsg: '图片上传成功',
      errorMsg: '图片上传失败',
      onSuccess: (data) => {
        // 将上传成功的图片路径添加到图片数组中
        reviewForm.images.push(data)
        options.onSuccess({ data })
      },
      onError: (error) => {
        console.error('图片上传错误:', error)
        options.onError(new Error(error.message || '上传失败'))
      }
    }

    // 发送上传请求
    await request.post('/file/upload/img', formData, uploadOptions)
  } catch (error) {
    options.onError(error)
    console.error('图片上传过程发生错误:', error)
  }
}

// 处理图片预览
const handlePictureCardPreview = (file) => {
  imagePreviewUrl.value = file.url || getImageUrl(file.response?.data)
  imagePreviewVisible.value = true
}

// 处理移除图片
const handleRemove = (file, fileList) => {
  // 从images数组中移除对应的图片路径
  const index = reviewForm.images.findIndex(img => img === file.response?.data)
  if (index !== -1) {
    reviewForm.images.splice(index, 1)
  }
}

// 提交评价表单
const submitReviewForm = async () => {
  if (!reviewFormRef.value) return
  
  await reviewFormRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        // 再次检查是否有权限评价
        if (!completedReservation.value) {
          await checkUserCanReview()
          
          if (!canReview.value) {
            ElMessage.warning('您尚未入住过此房型或已经评价过，无法添加评价')
            return
          }
        }
        
        // 构建评价对象
        const review = {
          reservationId: completedReservation.value.id,
          score: reviewForm.score,
          content: reviewForm.content,
          images: reviewForm.images.join(',')
        }
        
        await request.post('/review/create', review, {
          successMsg: '评价提交成功',
          onSuccess: () => {
            addReviewDialogVisible.value = false
            fetchRoomReviews()
            // 提交后重置评价权限
            canReview.value = false
            completedReservation.value = null
          }
        })
      } catch (error) {
        console.error('提交评价失败:', error)
      } finally {
        submitting.value = false
      }
    }
  })
}

// 监听roomId变化重新加载评价
watch(() => props.roomId, () => {
  if (props.roomId) {
  currentPage.value = 1
  fetchRoomReviews()
    if (props.showAddReviewButton) {
      checkUserCanReview()
    }
  }
})

// 生命周期钩子
onMounted(() => {
  if (props.roomId) {
  fetchRoomReviews()
    if (props.showAddReviewButton) {
      checkUserCanReview()
    }
  }
})
</script>

<style lang="scss" scoped>
.room-reviews {
  margin: 20px 0;
  
  .reviews-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    
    h3 {
      margin: 0;
      font-size: 18px;
      font-weight: 600;
      color: #1890ff;
    }
    
    .average-score {
      display: flex;
      align-items: center;
      
      .score {
        font-size: 24px;
        font-weight: bold;
        margin-right: 10px;
        color: #ff9900;
      }
    }
  }
  
  .reviews-empty {
    padding: 30px 0;
    text-align: center;
    color: #909399;
    
    p {
      margin-top: 10px;
      font-size: 14px;
    }
  }
  
  .reviews-list {
    .review-item {
      margin-bottom: 20px;
      
      .review-user {
        display: flex;
        align-items: center;
        margin-bottom: 15px;
        
        .user-info {
          margin-left: 15px;
          
          .username {
            font-weight: 500;
            font-size: 16px;
            color: #303133;
          }
          
          .review-date {
            font-size: 12px;
            color: #909399;
            margin-top: 4px;
          }
        }
      }
      
      .review-content {
        padding-left: 55px;
        
        .review-score {
          margin-bottom: 10px;
          display: flex;
          align-items: center;
          
          .score-text {
            margin-left: 10px;
            color: #ff9900;
            font-weight: 500;
          }
        }
        
        .review-text {
          margin-bottom: 15px;
          line-height: 1.6;
          color: #303133;
          font-size: 15px;
        }
        
        .review-images {
          display: flex;
          flex-wrap: wrap;
          gap: 10px;
          margin-bottom: 15px;
          
          .review-image {
            width: 80px;
            height: 80px;
            border-radius: 4px;
            object-fit: cover;
            cursor: pointer;
            transition: all 0.3s;
            border: 1px solid #ebeef5;
            
            &:hover {
              transform: scale(1.05);
              box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
            }
          }
          
          .image-error {
            width: 80px;
            height: 80px;
            display: flex;
            align-items: center;
            justify-content: center;
            background-color: #f5f7fa;
            color: #909399;
            font-size: 20px;
            border-radius: 4px;
          }
        }
        
        .review-reply {
          background-color: #f5f7fa;
          border-radius: 8px;
          padding: 15px;
          margin-top: 15px;
          position: relative;
          
          &:before {
            content: '';
            position: absolute;
            top: -8px;
            left: 20px;
            width: 0;
            height: 0;
            border-left: 8px solid transparent;
            border-right: 8px solid transparent;
            border-bottom: 8px solid #f5f7fa;
          }
          
          .reply-header {
            display: flex;
            align-items: center;
            font-weight: 500;
            color: #1890ff;
            margin-bottom: 8px;
            
            .el-icon {
              margin-right: 5px;
            }
          }
          
          .reply-content {
            color: #606266;
            line-height: 1.5;
            font-size: 14px;
          }
          
          .reply-time {
            margin-top: 8px;
            font-size: 12px;
            color: #909399;
            text-align: right;
          }
        }
      }
    }
  }
  
  .reviews-pagination {
    margin-top: 20px;
    display: flex;
    justify-content: center;
  }
  
  .add-review-section {
    margin-top: 30px;
    text-align: center;
    
    .loading-reservation {
      padding: 15px;
      border-radius: 8px;
      background-color: #f8f9fa;
      margin-bottom: 20px;
    }
    
    .can-review {
      margin: 10px 0;
    }
    
    .cannot-review {
      margin: 15px 0;
      
      .el-alert {
        display: inline-flex;
        width: auto;
        padding: 10px 20px;
      }
    }
    
    .el-button {
      padding: 10px 30px;
      font-size: 16px;
      background-color: #1890ff;
      border-color: #1890ff;
      
      &:hover, &:focus {
        background-color: #40a9ff;
        border-color: #40a9ff;
      }
    }
  }
  
  .review-image-upload {
    :deep(.el-upload--picture-card) {
      width: 100px;
      height: 100px;
      line-height: 100px;
      background-color: #f8f9fa;
      border-color: #e9ecef;
      
      &:hover {
        border-color: #1890ff;
        color: #1890ff;
      }
    }
  }
}
</style> 