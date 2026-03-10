import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'
import BackendLayout from '@/layouts/BackendLayout.vue'

// 后台路由
export const backendRoutes = [
  {
    path: '/back',
    component: BackendLayout,
    redirect: '/back/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/backend/Dashboard.vue'),
        meta: { title: '首页', icon: 'HomeFilled' }
      },
      {
        path: 'user',
        name: 'UserManagement',
        component: () => import('@/views/backend/user/index.vue'),
        meta: { title: '用户管理', icon: 'User' }
      },
      {
        path: 'room',
        name: 'RoomManagement',
        component: null,
        meta: { title: '房间管理', icon: 'House' },
        children: [
          {
            path: 'type',
            name: 'RoomTypeManagement',
            component: () => import('@/views/backend/room/type/index.vue'),
            meta: { title: '房型管理' }
          },
          {
            path: 'list',
            name: 'RoomListManagement',
            component: () => import('@/views/backend/room/index.vue'),
            meta: { title: '房间列表' }
          }
        ]
      },
      {
        path: 'reservation',
        name: 'ReservationManagement',
        component: () => import('@/views/backend/reservation/index.vue'),
        meta: { title: '预约管理', icon: 'Calendar' }
      },
      {
        path: 'order',
        name: 'OrderManagement',
        component: () => import('@/views/backend/order/index.vue'),
        meta: { title: '订单管理', icon: 'Money' }
      },
      {
        path: 'review',
        name: 'ReviewManagement',
        component: () => import('@/views/backend/review/index.vue'),
        meta: { title: '评价管理', icon: 'Comment' }
      },
      {
        path: 'banner',
        name: 'BannerManagement',
        component: () => import('@/views/backend/banner/index.vue'),
        meta: { title: '轮播图管理', icon: 'Picture' }
      },
      {
        path: 'profile',
        name: 'BackendProfile',
        component: () => import('@/views/backend/user/PersonInfo.vue'),
        meta: { title: '个人信息', icon: 'UserFilled' }
      }
    ]
  }
]

// 前台路由配置
const frontendRoutes = [
  {
    path: '/',
    component: () => import('@/layouts/FrontendLayout.vue'),
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/frontend/Home.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'rooms',
        name: 'RoomDisplay',
        component: () => import('@/views/frontend/RoomDisplay.vue'),
        meta: { title: '客房展示' }
      },
      {
        path: 'room/:id',
        name: 'RoomDetail',
        component: () => import('@/views/frontend/RoomDetail.vue'),
        meta: { title: '房间详情' }
      },
      {
        path: 'reservation',
        name: 'ReservationForm',
        component: () => import('@/views/frontend/ReservationForm.vue'),
        meta: { title: '房间预订' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/index.vue'),
        meta: { title: '个人中心', requiresAuth: true },
        children: [
          {
            path: '',
            redirect: '/profile/info'
          },
          {
            path: 'info',
            name: 'UserInfo',
            component: () => import('@/views/profile/info/index.vue'),
            meta: { title: '个人资料' }
          },
          {
            path: 'reservation',
            name: 'UserReservation',
            component: () => import('@/views/profile/reservation/index.vue'),
            meta: { title: '我的预约' }
          },
          {
            path: 'order',
            name: 'UserOrder',
            component: () => import('@/views/profile/order/index.vue'),
            meta: { title: '我的订单' }
          },
          {
            path: 'review',
            name: 'UserReview',
            component: () => import('@/views/profile/review/index.vue'),
            meta: { title: '退换反馈' }
          }
        ]
      }
    ] 
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/Register.vue'),
    meta: { title: '注册' }
  }
]

// 错误页面路由
const errorRoutes = [
  {
    path: '/404',
    name: '404',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '404' }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404'
  }
]

// 路由配置
const router = createRouter({
  history: createWebHistory(),
  routes: [
    ...frontendRoutes,
    ...backendRoutes,
    ...errorRoutes
  ]
})

// 路由守卫
router.beforeEach((to, from, next) => {
  // 设置页面标题
  if (to.meta.title) {
    document.title = `${to.meta.title} - 小型宾馆预约系统`
  }

  const userStore = useUserStore()
  console.log("Current route:", to.path)
  console.log("User status:", {
    isLoggedIn: userStore.isLoggedIn,
    isUser: userStore.isUser
  })

  // 检查是否需要登录权限
  if (to.matched.some(record => record.meta.requiresAuth) && !userStore.isLoggedIn) {
    next({
      path: '/login',
      query: { redirect: to.fullPath }
    })
    return
  }

  // 已登录用户的路由控制
  if (userStore.isLoggedIn) {
    // 处理登录页面访问
    if (to.path === '/login') {
      next(userStore.isUser ? '/' : '/back/dashboard')
      return
    }

    if (!userStore.isUser) {
      // 非普通用户只能访问后台路由
      if (to.path.startsWith('/back')) {
        next()
      } else {
        next('/back/dashboard')
      }
      return
    } else {
      // 普通用户只能访问前台路由
      if (to.path.startsWith('/back')) {
        next('/')
      } else {
        next()
      }
      return
    }
  } else {
    // 未登录用户
    if (to.path.startsWith('/back')) {
      next('/login')
      return
    }
  }

  next()
})

export default router
