<template>
  <div class="user-review">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <h3>退换反馈</h3>
          <div class="header-decoration"></div>
        </div>
      </template>
      
      <!-- 评价列表 -->
      <el-table :data="reviews" style="width: 100%" v-loading="loading" border>
        <el-table-column label="房间信息" width="150">
          <template #default="scope">
            <div v-if="scope.row.room" class="room-info-cell">
              <div class="room-number">{{ scope.row.room.roomNumber }}</div>
              <div class="room-type">{{ scope.row.room.roomType ? scope.row.room.roomType.name : '' }}</div>
            </div>
            <span v-else>未知</span>
          </template>
        </el-table-column>
        <el-table-column label="评分" width="150">
          <template #default="scope">
            <div class="score-cell">
              <el-rate
                v-model="scope.row.score"
                disabled
                text-color="#ff9900"
              />
              <span class="score-text">{{ getScoreText(scope.row.score) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="评价内容" show-overflow-tooltip min-width="120" />
        <el-table-column label="评价图片" width="100">
          <template #default="scope">
            <div v-if="scope.row.images" class="preview-images">
              <el-image
                :src="getImageUrl(getFirstImage(scope.row.images))"
                :preview-src-list="getImageUrls(scope.row.images)"
                fit="cover"
                :preview-teleported="true"
                class="preview-image"
              >
                <template #error>
                  <div class="image-error">
                    <el-icon><Picture /></el-icon>
                  </div>
                </template>
              </el-image>
              <span v-if="getImageCount(scope.row.images) > 1" class="image-count">+{{ getImageCount(scope.row.images) - 1 }}</span>
            </div>
            <span v-else>无图片</span>
          </template>
        </el-table-column>
        <el-table-column label="酒店回复" min-width="120" show-overflow-tooltip>
          <template #default="scope">
            <div v-if="scope.row.reply" class="reply-cell">
              <el-icon><ChatDotRound /></el-icon>
              <span>{{ scope.row.reply }}</span>
            </div>
            <el-tag v-else type="info" size="small" effect="plain">暂无回复</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag 
              :type="scope.row.status === 1 ? 'success' : 'info'" 
              effect="plain"
            >
              {{ scope.row.status === 1 ? '已显示' : '待审核' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="评价时间" width="180" />
        <el-table-column label="操作" fixed="right" width="180">
          <template #default="scope">
            <el-button 
              type="primary" 
              size="small" 
              @click="handleViewDetail(scope.row)">
              详情
            </el-button>
            <el-button 
              type="danger" 
              size="small" 
              @click="handleDelete(scope.row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <el-empty description="暂无评价记录" v-if="reviews.length === 0 && !loading"></el-empty>
      
      <el-pagination
        v-if="reviews.length > 0"
        class="pagination"
      :current-page="currentPage"
        :page-size="pageSize"
        layout="total, prev, pager, next"
        :total="total"
        @current-change="handlePageChange"
      />
      
      <!-- 待评价预约列表 -->
      <div class="pending-reviews" v-if="pendingReservations.length > 0">
        <div class="section-title">
          <h3>待反馈的订单</h3>
          <div class="title-decoration"></div>
        </div>
        
        <el-table :data="pendingReservations" style="width: 100%" v-loading="loadingPending" border>
          <el-table-column label="房间信息" width="150">
            <template #default="scope">
              <div v-if="scope.row.room" class="room-info-cell">
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
          <el-table-column prop="createTime" label="预约时间"  />
          <el-table-column label="操作" fixed="right" width="240">
            <template #default="scope">
              <el-button 
                type="primary" 
                size="small" 
                @click="handleAddReview(scope.row)">
                去反馈
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      
      <!-- 评价详情对话框 -->
      <el-dialog v-model="detailDialogVisible" title="评价详情" width="650px">
        <div class="review-detail">
          <div class="review-header">
            <div class="room-info">
              <h4>{{ currentReview.room && currentReview.room.roomType ? currentReview.room.roomType.name : '未知房型' }}</h4>
              <div class="room-number">房间号: {{ currentReview.room ? currentReview.room.roomNumber : '未知' }}</div>
            </div>
            <div class="review-meta">
              <div class="review-score">
                <span class="score-label">我的评分:</span>
                <el-rate
                  v-model="currentReview.score"
                  disabled
                  text-color="#ff9900"
                />
                <span class="score-text">{{ getScoreText(currentReview.score) }}</span>
              </div>
              <div class="review-date">
                <el-icon><Calendar /></el-icon>
                <span>{{ formatDate(currentReview.createTime) }}</span>
              </div>
            </div>
          </div>
          
          <el-divider />
          
          <div class="review-body">
            <div class="review-content">
              <h4>退换反馈</h4>
              <p>{{ currentReview.content }}</p>
            </div>
            
            <div class="review-images" v-if="currentReview.images">
              <h4>评价图片</h4>
              <div class="images-grid">
                <el-image
                  v-for="(image, index) in reviewImages"
                  :key="index"
                  :src="getImageUrl(image)"
                  :preview-src-list="reviewImageUrls"
                  fit="cover"
                  :preview-teleported="true"
                  class="review-image"
                >
                  <template #error>
                    <div class="image-error">
                      <el-icon><Picture /></el-icon>
                    </div>
                  </template>
                </el-image>
              </div>
            </div>
            
            <div class="review-reply" v-if="currentReview.reply">
              <h4>酒店回复</h4>
              <div class="reply-box">
                <div class="reply-content">{{ currentReview.reply }}</div>
                <div class="reply-time" v-if="currentReview.replyTime">{{ formatDate(currentReview.replyTime) }}</div>
              </div>
            </div>
          </div>
        </div>
        
        <template #footer>
          <span class="dialog-footer">
            <el-button @click="detailDialogVisible = false">关闭</el-button>
            <el-button type="danger" @click="handleDelete(currentReview)">删除</el-button>
          </span>
        </template>
      </el-dialog>
      
      <!-- 添加评价对话框 -->
      <el-dialog v-model="addReviewDialogVisible" title="添加评价" width="600px">
        <el-form :model="reviewForm" :rules="reviewRules" ref="reviewFormRef" label-width="100px">
          <el-form-item label="房间信息">
            <div class="room-info-form">
              <div class="room-number">房间号: {{ selectedReservation.room ? selectedReservation.room.roomNumber : '' }}</div>
              <div class="room-type">{{ selectedReservation.room && selectedReservation.room.roomType ? selectedReservation.room.roomType.name : '' }}</div>
            </div>
          </el-form-item>
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
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Picture, ChatDotRound, Calendar } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'
const userStore = useUserStore()

// 数据
const reviews = ref([])
const pendingReservations = ref([])
const loading = ref(false)
const loadingPending = ref(false)
const submitting = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 评价详情对话框
const detailDialogVisible = ref(false)
const currentReview = ref({})
const reviewImages = computed(() => {
  if (!currentReview.value.images) return []
  return currentReview.value.images.split(',')
})
const reviewImageUrls = computed(() => {
  return reviewImages.value.map(img => getImageUrl(img))
})

// 添加评价对话框
const addReviewDialogVisible = ref(false)
const reviewFormRef = ref(null)
const selectedReservation = ref({})
const reviewForm = reactive({
  reservationId: null,
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

// 获取用户评价列表
const fetchUserReviews = async () => {
  loading.value = true
  try {
    await request.get('/review/page', {
      userId: userStore.userInfo?.id,
      currentPage: currentPage.value,
      size: pageSize.value
    }, {
      onSuccess: (res) => {
        reviews.value = res.records || []
        total.value = res.total || 0
      }
    })
  } catch (error) {
    console.error('获取用户评价列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 获取待评价的预约
const fetchPendingReviews = async () => {
  loadingPending.value = true
  try {
    await request.get('/reservation/user/completed',null, {
      onSuccess: (res) => {
        // 过滤出未评价的预约
        pendingReservations.value = []
        
        if (res && res.length > 0) {
          const checkReservation = async (reservation) => {
            try {
              // 检查是否已有评价
              const hasReview = await checkReservationReview(reservation.id)
              if (!hasReview) {
                pendingReservations.value.push(reservation)
              }
            } catch (error) {
              console.error('检查预约评价状态失败:', error)
            }
          }
          
          const promises = res.map(checkReservation)
          Promise.all(promises).finally(() => {
            loadingPending.value = false
          })
        } else {
          loadingPending.value = false
        }
      }
    })
  } catch (error) {
    console.error('获取待评价预约失败:', error)
    loadingPending.value = false
  }
}

// 检查预约是否已有评价
const checkReservationReview = async (reservationId) => {
  try {
    let hasReview = false
    await request.get(`/review/reservation/${reservationId}`,null, {
      onSuccess: (review) => {
        hasReview = !!review
      }
    })
    return hasReview
  } catch (error) {
    console.error('检查预约评价状态失败:', error)
    return false
  }
}

// 查看评价详情
const handleViewDetail = (review) => {
  currentReview.value = review
  detailDialogVisible.value = true
}

// 删除评价
const handleDelete = (review) => {
  ElMessageBox.confirm('确定要删除这条评价吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await request.delete(`/review/${review.id}`, {
        successMsg: '删除成功',
        onSuccess: () => {
          detailDialogVisible.value = false
          fetchUserReviews() // 重新加载列表
        }
      })
    } catch (error) {
      console.error('删除评价失败:', error)
    }
  }).catch(() => {})
}

// 添加评价
const handleAddReview = (reservation) => {
  selectedReservation.value = reservation
  reviewForm.reservationId = reservation.id
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
        // 构建评价对象
        const review = {
          reservationId: reviewForm.reservationId,
          score: reviewForm.score,
          content: reviewForm.content,
          images: reviewForm.images.join(',')
        }
        
        await request.post('/review/create', review, {
          successMsg: '评价提交成功',
          onSuccess: () => {
            addReviewDialogVisible.value = false
            // 重新加载数据
            fetchUserReviews()
            fetchPendingReviews()
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

// 分页处理
const handlePageChange = (page) => {
  currentPage.value = page
  fetchUserReviews()
}

// 获取图片完整URL
const baseAPI = process.env.VUE_APP_BASE_API || '/api'
const getImageUrl = (path) => {
  if (!path) return ''
  return baseAPI + path
}

// 从图片字符串中获取第一张图片
const getFirstImage = (imagesStr) => {
  if (!imagesStr) return ''
  const images = imagesStr.split(',')
  return images[0]
}

// 获取图片数量
const getImageCount = (imagesStr) => {
  if (!imagesStr) return 0
  return imagesStr.split(',').length
}

// 获取图片URL数组
const getImageUrls = (imagesStr) => {
  if (!imagesStr) return []
  const images = imagesStr.split(',')
  return images.map(img => getImageUrl(img))
}

// 获取评分文字描述
const getScoreText = (score) => {
  if (score >= 5) return '非常满意'
  if (score >= 4) return '满意'
  if (score >= 3) return '一般'
  if (score >= 2) return '不满意'
  return '非常不满意'
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  try {
    const date = new Date(dateStr)
    return date.toLocaleDateString('zh-CN', { 
      year: 'numeric', 
      month: 'long', 
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch (error) {
    return dateStr
  }
}

// 生命周期钩子
onMounted(() => {
  fetchUserReviews()
  fetchPendingReviews()
})
</script>

<style lang="scss" scoped>
.user-review {
  .box-card {
    .card-header {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      
      h3 {
        margin: 0 0 10px 0;
        font-size: 20px;
        font-weight: 500;
      }
      
      .header-decoration {
        width: 40px;
        height: 3px;
        background-color: #1890ff;
      }
    }
  }
  
  .room-info-cell {
    .room-number {
      font-weight: 500;
    }
    
    .room-type {
      font-size: 12px;
      color: #909399;
      margin-top: 4px;
    }
  }
  
  .score-cell {
    display: flex;
    flex-direction: column;
    
    .score-text {
      font-size: 12px;
      color: #ff9900;
      margin-top: 4px;
    }
  }
  
  .reply-cell {
    display: flex;
    align-items: center;
    
    .el-icon {
      color: #1890ff;
      margin-right: 5px;
    }
  }
  
  .preview-images {
    position: relative;
    
    .preview-image {
      width: 50px;
      height: 50px;
      border-radius: 4px;
      object-fit: cover;
      cursor: pointer;
    }
    
    .image-error {
      width: 50px;
      height: 50px;
      display: flex;
      align-items: center;
      justify-content: center;
      background-color: #f5f7fa;
      color: #909399;
    }
    
    .image-count {
      position: absolute;
      right: 0;
      bottom: 0;
      background-color: rgba(0, 0, 0, 0.5);
      color: white;
      padding: 2px 5px;
      border-radius: 2px;
      font-size: 12px;
    }
  }
  
  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
  
  .pending-reviews {
    margin-top: 30px;
    
    .section-title {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      margin-bottom: 20px;
      
      h3 {
        margin: 0 0 10px 0;
        font-size: 16px;
        font-weight: 500;
      }
      
      .title-decoration {
        width: 30px;
        height: 2px;
        background-color: #1890ff;
      }
    }
  }
  
  .review-detail {
    .review-header {
      display: flex;
      justify-content: space-between;
      margin-bottom: 15px;
      
      .room-info {
        h4 {
          margin: 0 0 5px 0;
          font-size: 18px;
          color: #1890ff;
        }
        
        .room-number {
          font-size: 14px;
          color: #606266;
        }
      }
      
      .review-meta {
        text-align: right;
        
        .review-score {
          display: flex;
          align-items: center;
          margin-bottom: 5px;
          
          .score-label {
            margin-right: 5px;
          }
          
          .score-text {
            margin-left: 5px;
            color: #ff9900;
          }
        }
        
        .review-date {
          display: flex;
          align-items: center;
          justify-content: flex-end;
          color: #909399;
          font-size: 14px;
          
          .el-icon {
            margin-right: 5px;
          }
        }
      }
    }
    
    .review-body {
      h4 {
        margin: 20px 0 10px 0;
        font-size: 16px;
        color: #1890ff;
      }
      
      .review-content {
        p {
          margin: 0;
          line-height: 1.6;
        }
      }
      
      .images-grid {
        display: flex;
        flex-wrap: wrap;
        gap: 10px;
        
        .review-image {
          width: 100px;
          height: 100px;
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
          width: 100px;
          height: 100px;
          display: flex;
          align-items: center;
          justify-content: center;
          background-color: #f5f7fa;
          color: #909399;
        }
      }
      
      .review-reply {
        .reply-box {
          background-color: #f5f7fa;
          border-radius: 8px;
          padding: 15px;
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
          
          .reply-content {
            color: #606266;
            line-height: 1.5;
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
  
  .room-info-form {
    .room-number {
      font-weight: 500;
    }
    
    .room-type {
      font-size: 16px;
      color: #1890ff;
      margin-top: 5px;
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