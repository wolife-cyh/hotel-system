<template>
  <div class="review-management">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>用户反馈</span>
        </div>
      </template>
      
      <!-- 搜索区域 -->
      <div class="search-container">
        <el-form :inline="true" :model="searchForm" class="demo-form-inline">
          <el-form-item label="房间号">
            <el-input v-model="searchForm.roomNumber" placeholder="请输入房间号" clearable />
          </el-form-item>
          <el-form-item label="用户名">
            <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable />
          </el-form-item>
          <el-form-item label="评价状态">
            <el-select v-model="searchForm.status" placeholder="请选择" clearable>
              <el-option label="显示" :value="1" />
              <el-option label="隐藏" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="fetchReviews">查询</el-button>
            <el-button @click="resetSearch">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
      
      <!-- 评价列表 -->
      <el-table :data="tableData" style="width: 100%" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column label="用户信息" width="120">
          <template #default="scope">
            {{ scope.row.user ? scope.row.user.username : '未知' }}
          </template>
        </el-table-column>
        <el-table-column label="房间信息" width="120">
          <template #default="scope">
            <div v-if="scope.row.room">
              {{ scope.row.room.roomNumber }}
              <div class="room-type">{{ scope.row.room.roomType ? scope.row.room.roomType.name : '' }}</div>
            </div>
            <span v-else>未知</span>
          </template>
        </el-table-column>
        <el-table-column label="评分" width="100">
          <template #default="scope">
            <el-rate
              v-model="scope.row.score"
              disabled
              show-score
              text-color="#ff9900"
              score-template="{value}"
            />
          </template>
        </el-table-column>
        <el-table-column prop="content" label="评价内容" show-overflow-tooltip min-width="150" />
        <el-table-column label="回复" min-width="150" show-overflow-tooltip>
          <template #default="scope">
            <span v-if="scope.row.reply">{{ scope.row.reply }}</span>
            <el-tag v-else type="info" size="small">未回复</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
              {{ scope.row.status === 1 ? '显示' : '隐藏' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="评价时间" width="100" />
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
              v-if="!scope.row.reply" 
              @click="handleReply(scope.row)">
              回复
            </el-button>
            <el-button 
              :type="scope.row.status === 1 ? 'warning' : 'success'" 
              size="small" 
              @click="handleToggleStatus(scope.row)">
              {{ scope.row.status === 1 ? '隐藏' : '显示' }}
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
      
      <!-- 评价详情对话框 -->
      <el-dialog v-model="detailDialogVisible" title="评价详情" width="700px">
        <div class="review-detail">
          <div class="review-header">
            <div class="user-room-info">
              <div class="user-info">
                <h4>用户信息</h4>
                <p>{{ currentReview.user ? currentReview.user.username : '未知用户' }}</p>
              </div>
              
              <div class="room-info">
                <h4>房间信息</h4>
                <div class="room-number">{{ currentReview.room ? currentReview.room.roomNumber : '未知' }}</div>
                <div class="room-type">{{ currentReview.room && currentReview.room.roomType ? currentReview.room.roomType.name : '未知' }}</div>
              </div>
            </div>
            
            <div class="review-meta">
              <div class="review-score">
                <span class="score-label">评分:</span>
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
              <div class="review-status">
                <el-tag :type="currentReview.status === 1 ? 'success' : 'info'" effect="plain">
                  {{ currentReview.status === 1 ? '显示中' : '已隐藏' }}
                </el-tag>
              </div>
            </div>
          </div>
          
          <el-divider />
          
          <div class="review-body">
            <div class="review-content">
              <h4>评价内容</h4>
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
              <h4>回复内容</h4>
              <div class="reply-box">
                <div class="reply-content">{{ currentReview.reply }}</div>
                <div class="reply-time" v-if="currentReview.replyTime">{{ formatDate(currentReview.replyTime) }}</div>
              </div>
            </div>
            <div class="no-reply" v-else>
              <el-empty description="暂无回复" :image-size="80">
                <el-button type="primary" @click="handleReply(currentReview)">立即回复</el-button>
              </el-empty>
            </div>
          </div>
        </div>
        
        <div class="operation-container">
          <el-divider content-position="center">操作</el-divider>
          <div class="operation-buttons">
            <el-button 
              type="success" 
              v-if="!currentReview.reply" 
              @click="handleReply(currentReview)">
              回复
            </el-button>
            <el-button 
              :type="currentReview.status === 1 ? 'warning' : 'success'" 
              @click="handleToggleStatus(currentReview)">
              {{ currentReview.status === 1 ? '隐藏' : '显示' }}
            </el-button>
            <el-button 
              type="danger" 
              @click="handleDelete(currentReview)">
              删除
            </el-button>
          </div>
        </div>
        
        <template #footer>
          <span class="dialog-footer">
            <el-button @click="detailDialogVisible = false">关闭</el-button>
          </span>
        </template>
      </el-dialog>
      
      <!-- 回复对话框 -->
      <el-dialog v-model="replyDialogVisible" title="回复评价" width="500px">
        <el-form :model="replyForm" ref="replyFormRef">
          <el-form-item label="评价内容" prop="content">
            <div class="review-content-box">{{ currentReview.content }}</div>
          </el-form-item>
          <el-form-item label="回复内容" prop="reply" :rules="[{ required: true, message: '请输入回复内容', trigger: 'blur' }]">
            <el-input
              v-model="replyForm.reply"
              type="textarea"
              :rows="4"
              placeholder="请输入回复内容"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <span class="dialog-footer">
            <el-button @click="replyDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="submitReply" :loading="submitting">提交</el-button>
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
import { Calendar, Picture } from '@element-plus/icons-vue'

// 数据列表
const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const submitting = ref(false)

// 搜索表单
const searchForm = reactive({
  roomNumber: '',
  username: '',
  status: null
})

// 评价详情对话框
const detailDialogVisible = ref(false)
const currentReview = ref({})

// 回复对话框
const replyDialogVisible = ref(false)
const replyFormRef = ref(null)
const replyForm = reactive({
  reply: ''
})

// 获取评价列表
const fetchReviews = async () => {
  loading.value = true
  try {
    const params = {
      currentPage: currentPage.value,
      size: pageSize.value,
      status: searchForm.status
    }
    
    // 如果输入了房间号，需要先查询房间ID
    if (searchForm.roomNumber) {
      try {
        await request.get('/room/page', {
  
            roomNumber: searchForm.roomNumber,
            currentPage: 1,
            size: 10
          },{
          onSuccess: (res) => {
            if (res.records && res.records.length > 0) {
              params.roomId = res.records[0].id
            }
          }
        })
      } catch (error) {
        console.error('查询房间失败:', error)
      }
    }
    
    // 如果输入了用户名，需要先查询用户ID
    if (searchForm.username) {
      try {
        await request.get('/user/page', {
            
            username: searchForm.username,
            currentPage: 1,
            size: 10
          },{
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
    
    await request.get('/review/page', 
      params,
      {
      onSuccess: (res) => {
        tableData.value = res.records||[]
        total.value = res.total||0
      }
    })
  } catch (error) {
    console.error('获取评价列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 重置搜索
const resetSearch = () => {
  searchForm.roomNumber = ''
  searchForm.username = ''
  searchForm.status = null
  fetchReviews()
}

// 处理分页大小变化
const handleSizeChange = (val) => {
  pageSize.value = val
  fetchReviews()
}

// 处理页码变化
const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchReviews()
}

// 查看评价详情
const handleViewDetail = (row) => {
  currentReview.value = { ...row }
  detailDialogVisible.value = true
}

// 处理回复评价
const handleReply = (row) => {
  currentReview.value = { ...row }
  replyForm.reply = ''
  replyDialogVisible.value = true
}

// 提交回复
const submitReply = async () => {
  if (!replyFormRef.value) return
  
  await replyFormRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        await request.put(`/review/reply/${currentReview.value.id}`, {
          reply: replyForm.reply
        }, {
          successMsg: '回复成功',
          onSuccess: () => {
            replyDialogVisible.value = false
            fetchReviews()
          }
        })
      } finally {
        submitting.value = false
      }
    }
  })
}

// 切换评价状态
const handleToggleStatus = (review) => {
  const newStatus = review.status === 1 ? 0 : 1
  const statusText = newStatus === 1 ? '显示' : '隐藏'
  
  ElMessageBox.confirm(
    `确定要将此评价设为${statusText}吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await request.put(`/review/status/${review.id}?status=${newStatus}`, null, {
        successMsg: `评价已${statusText}`,
        onSuccess: () => {
          fetchReviews()
          if (detailDialogVisible.value && currentReview.value.id === review.id) {
            currentReview.value.status = newStatus
          }
        }
      })
    } catch (error) {
      console.error('更新评价状态失败:', error)
    }
  }).catch(() => {})
}

// 删除评价
const handleDelete = (review) => {
  ElMessageBox.confirm(
    '确定要删除此评价吗？此操作不可恢复',
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await request.delete(`/review/${review.id}`, {
        successMsg: '评价已删除',
        onSuccess: () => {
          fetchReviews()
          if (detailDialogVisible.value && currentReview.value.id === review.id) {
            detailDialogVisible.value = false
          }
        }
      })
    } catch (error) {
      console.error('删除评价失败:', error)
    }
  }).catch(() => {})
}

// 获取评价图片数组
const reviewImages = computed(() => {
  if (!currentReview.value.images) return []
  return currentReview.value.images.split(',')
})

// 获取评价图片URL数组（用于预览）
const reviewImageUrls = computed(() => {
  return reviewImages.value.map(img => getImageUrl(img))
})

// 获取图片完整URL
const baseAPI = process.env.VUE_APP_BASE_API || '/api'
const getImageUrl = (path) => {
  if (!path) return ''
  return baseAPI + path
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
  fetchReviews()
})
</script>

<style lang="scss" scoped>
.review-management {
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
  
  .pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: center;
  }
  
  .room-type {
    font-size: 12px;
    color: #909399;
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
  
  .review-content-box {
    padding: 10px;
    background-color: #f8f8f8;
    border-radius: 4px;
    border: 1px solid #e0e0e0;
  }
  
  .review-images {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    
    .review-image {
      width: 100px;
      height: 100px;
      border-radius: 4px;
      object-fit: cover;
    }
  }
}

.review-detail {
  .review-header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 15px;
    
    .user-room-info {
      display: flex;
      gap: 30px;
      
      h4 {
        margin: 0 0 8px 0;
        font-size: 16px;
        color: #1890ff;
      }
      
      p {
        margin: 0;
      }
      
      .room-info {
        .room-number {
          font-weight: 500;
        }
        
        .room-type {
          font-size: 14px;
          color: #606266;
          margin-top: 4px;
        }
      }
    }
    
    .review-meta {
      text-align: right;
      
      .review-score {
        display: flex;
        align-items: center;
        margin-bottom: 8px;
        
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
        margin-bottom: 8px;
        
        .el-icon {
          margin-right: 5px;
        }
      }
      
      .review-status {
        text-align: right;
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
        background-color: #f8f9fa;
        padding: 15px;
        border-radius: 8px;
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
    
    .no-reply {
      margin: 20px 0;
      background-color: #f8f9fa;
      border-radius: 8px;
      padding: 20px;
      text-align: center;
    }
  }
}

.operation-container {
  margin-top: 20px;
  
  .operation-buttons {
    display: flex;
    justify-content: center;
    gap: 15px;
    margin-top: 15px;
  }
}
</style> 