<template>
  <div class="banner-carousel" :style="carouselStyle">
    <el-carousel 
      :height="height" 
      :interval="interval" 
      :autoplay="autoplay" 
      :indicator-position="indicatorPosition"
      :type="type"
      arrow="always"
    >
      <el-carousel-item v-for="item in bannerList" :key="item.id">
        <div class="carousel-item">
          <el-image 
            :src="getImageUrl(item.imageUrl)" 
            fit="cover" 
            class="carousel-image"
            :style="{ 'object-position': position }"
          />
          <div v-if="showTitle" class="carousel-title">
            <h3>{{ item.title }}</h3>
          </div>
        </div>
      </el-carousel-item>
    </el-carousel>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import request from '@/utils/request'

const props = defineProps({
  // 轮播图高度
  height: {
    type: String,
    default: '500px'
  },
  // 轮播图自动播放间隔(毫秒)
  interval: {
    type: Number,
    default: 5000
  },
  // 是否自动播放
  autoplay: {
    type: Boolean,
    default: true
  },
  // 指示器位置
  indicatorPosition: {
    type: String,
    default: 'outside'
  },
  // 显示标题
  showTitle: {
    type: Boolean,
    default: true
  },
  // 轮播图类型
  type: {
    type: String,
    default: ''
  },
  // 图片位置
  imgPosition: {
    type: String,
    default: 'center center'
  },
  // 自定义样式
  customStyle: {
    type: Object,
    default: () => ({})
  }
})

const bannerList = ref([])
// 基础API路径
const baseAPI = process.env.VUE_APP_BASE_API || "/api"

// 加载轮播图数据
const loadBanners = async () => {
  try {
    await request.get('/banner/enabled', {}, {
      showDefaultMsg: false,
      onSuccess: (data) => {
        bannerList.value = data || []
      }
    })
  } catch (error) {
    console.error('获取轮播图失败:', error)
  }
}

// 获取图片URL
const getImageUrl = (imageUrl) => {
  if (!imageUrl) return ''
  if (imageUrl.startsWith('http')) {
    return imageUrl
  }
  return baseAPI + imageUrl
}

// 计算自定义样式
const carouselStyle = computed(() => {
  return {
    ...props.customStyle
  }
})

// 计算图片位置
const position = computed(() => props.imgPosition)

onMounted(() => {
  loadBanners()
})
</script>

<style lang="scss" scoped>
.banner-carousel {
  width: 90%;
  max-width: 1200px;
  margin: 0 auto;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  
  .carousel-item {
    width: 100%;
    height: 100%;
    position: relative;
    
    .carousel-image {
      width: 100%;
      height: 100%;
      transition: transform 0.5s ease;
    }
    
    .carousel-title {
      position: absolute;
      bottom: 0;
      left: 0;
      right: 0;
      background: linear-gradient(to top, rgba(0, 0, 0, 0.6), transparent);
      color: white;
      padding: 30px 20px 20px;
      
      h3 {
        margin: 0;
        font-size: 20px;
        font-weight: 300;
        line-height: 1.5;
        letter-spacing: 1px;
      }
    }
  }
  
  :deep(.el-carousel__arrow) {
    background-color: rgba(0, 0, 0, 0.3);
    border-radius: 50%;
    width: 44px;
    height: 44px;
    
    &:hover {
      background-color: rgba(24, 144, 255, 0.7);
    }
  }
  
  :deep(.el-carousel__indicators) {
    &.el-carousel__indicators--outside {
      margin-top: 12px;
      
      .el-carousel__indicator {
        .el-carousel__button {
          background-color: #ccc;
          width: 10px;
          height: 10px;
          border-radius: 50%;
          
          &:hover {
            background-color: #1890ff;
          }
        }
        
        &.is-active {
          .el-carousel__button {
            background-color: #1890ff;
          }
        }
      }
    }
  }
}
</style> 