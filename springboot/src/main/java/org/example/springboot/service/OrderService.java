package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.springboot.entity.Order;
import org.example.springboot.entity.Reservation;
import org.example.springboot.entity.Room;
import org.example.springboot.entity.RoomType;
import org.example.springboot.entity.User;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.OrderMapper;
import org.example.springboot.mapper.ReservationMapper;
import org.example.springboot.mapper.RoomMapper;
import org.example.springboot.mapper.RoomTypeMapper;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 订单服务类
 */
@Service
public class OrderService {
    @Resource
    private OrderMapper orderMapper;
    
    @Resource
    private ReservationMapper reservationMapper;
    
    @Resource
    private UserMapper userMapper;
    
    @Resource
    private RoomMapper roomMapper;
    
    @Resource
    private RoomTypeMapper roomTypeMapper;
    
    /**
     * 分页查询订单
     */
    public Page<Order> getOrdersByPage(String orderNo, Long userId, Integer status, 
                                    Integer currentPage, Integer size) {
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        
        // 添加查询条件
        if (StringUtils.isNotBlank(orderNo)) {
            queryWrapper.like(Order::getOrderNo, orderNo);
        }
        if (userId != null) {
            queryWrapper.eq(Order::getUserId, userId);
        }
        if (status != null) {
            queryWrapper.eq(Order::getStatus, status);
        }
        
        // 按创建时间降序排序
        queryWrapper.orderByDesc(Order::getCreateTime);
        
        Page<Order> page = orderMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        
        // 查询关联数据
        for (Order order : page.getRecords()) {
            loadOrderAssociations(order);
        }
        
        return page;
    }
    
    /**
     * 根据ID获取订单
     */
    public Order getOrderById(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        
        // 查询关联数据
        loadOrderAssociations(order);
        
        return order;
    }
    
    /**
     * 根据订单号获取订单
     */
    public Order getOrderByOrderNo(String orderNo) {
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getOrderNo, orderNo);
        
        Order order = orderMapper.selectOne(queryWrapper);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        
        // 查询关联数据
        loadOrderAssociations(order);
        
        return order;
    }
    
    /**
     * 查询预约的订单
     */
    public Order getOrderByReservationId(Long reservationId) {
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getReservationId, reservationId);
        
        Order order = orderMapper.selectOne(queryWrapper);
        if (order != null) {
            // 查询关联数据
            loadOrderAssociations(order);
        }
        
        return order;
    }
    
    /**
     * 创建订单
     */
    @Transactional
    public Order createOrder(Long reservationId) {
        // 检查预约是否存在
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new ServiceException("预约不存在");
        }
        
        // 获取当前登录用户
        User currentUser = JwtTokenUtils.getCurrentUser();
        
        // 检查预约是否属于当前用户
        if (!reservation.getUserId().equals(currentUser.getId())) {
            throw new ServiceException("无权为此预约创建订单");
        }
        
        // 检查是否已有订单
        Order existingOrder = getOrderByReservationId(reservationId);
        if (existingOrder != null) {
            return existingOrder;
        }
        
        // 创建订单
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(currentUser.getId());
        order.setReservationId(reservationId);
        order.setAmount(reservation.getPrice());
        order.setStatus(0); // 未支付
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        
        if (orderMapper.insert(order) <= 0) {
            throw new ServiceException("创建订单失败");
        }
        
        return order;
    }
    
    /**
     * 更新订单状态
     */
    @Transactional
    public void updateOrderStatus(Long id, Integer status) {
        // 检查订单是否存在
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        
        // 获取当前登录用户
        User currentUser = JwtTokenUtils.getCurrentUser();
        
        // 非管理员只能操作自己的订单
        if (!"ADMIN".equals(currentUser.getRoleCode()) && !order.getUserId().equals(currentUser.getId())) {
            throw new ServiceException("无权操作此订单");
        }
        
        // 更新订单状态
        Order updateOrder = new Order();
        updateOrder.setId(id);
        updateOrder.setStatus(status);
        updateOrder.setUpdateTime(LocalDateTime.now());
        
        // 如果是支付状态，设置支付时间
        if (status == 1) {
            updateOrder.setPayTime(LocalDateTime.now());
        }
        
        if (orderMapper.updateById(updateOrder) <= 0) {
            throw new ServiceException("更新订单状态失败");
        }
        
        // 同时更新预约的支付状态
        updateReservationPayStatus(order.getReservationId(), status);
    }
    
    /**
     * 支付订单
     */
    @Transactional
    public void payOrder(Long id, String payMethod, String payNo) {
        // 检查订单是否存在
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        
        // 获取当前登录用户
        User currentUser = JwtTokenUtils.getCurrentUser();
        
        // 检查订单是否属于当前用户
        if (!order.getUserId().equals(currentUser.getId())) {
            throw new ServiceException("无权操作此订单");
        }
        
        // 检查订单状态
        if (order.getStatus() != 0) {
            throw new ServiceException("订单状态不允许支付");
        }
        
        // 更新订单信息
        Order updateOrder = new Order();
        updateOrder.setId(id);
        updateOrder.setStatus(1); // 已支付
        updateOrder.setPayMethod(payMethod);
        updateOrder.setPayNo(payNo);
        updateOrder.setPayTime(LocalDateTime.now());
        updateOrder.setUpdateTime(LocalDateTime.now());
        
        if (orderMapper.updateById(updateOrder) <= 0) {
            throw new ServiceException("支付订单失败");
        }
        
        // 同时更新预约的支付状态
        updateReservationPayStatus(order.getReservationId(), 1);
    }
    
    /**
     * 取消订单
     */
    @Transactional
    public void cancelOrder(Long id) {
        // 检查订单是否存在
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        
        // 获取当前登录用户
        User currentUser = JwtTokenUtils.getCurrentUser();
        
        // 非管理员只能取消自己的订单
        if (!"ADMIN".equals(currentUser.getRoleCode()) && !order.getUserId().equals(currentUser.getId())) {
            throw new ServiceException("无权取消此订单");
        }
        
        // 检查订单状态
        if (order.getStatus() != 0) {
            throw new ServiceException("只能取消未支付的订单");
        }
        
        // 更新订单状态
        Order updateOrder = new Order();
        updateOrder.setId(id);
        updateOrder.setStatus(2); // 已取消
        updateOrder.setUpdateTime(LocalDateTime.now());
        
        if (orderMapper.updateById(updateOrder) <= 0) {
            throw new ServiceException("取消订单失败");
        }
        
        // 同时更新预约的支付状态
        Reservation reservation = reservationMapper.selectById(order.getReservationId());
        if (reservation != null) {
            Reservation updateReservation = new Reservation();
            updateReservation.setId(reservation.getId());
            updateReservation.setStatus(2); // 已取消
            updateReservation.setPayStatus(2); // 已取消
            updateReservation.setUpdateTime(LocalDateTime.now());
            
            if (reservationMapper.updateById(updateReservation) <= 0) {
                throw new ServiceException("更新预约状态失败");
            }
        }
    }
    
    /**
     * 退款
     */
    @Transactional
    public void refundOrder(Long id) {
        // 检查订单是否存在
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        
        // 获取当前登录用户
        User currentUser = JwtTokenUtils.getCurrentUser();
        
        // 只有管理员能退款
        if (!"ADMIN".equals(currentUser.getRoleCode())) {
            throw new ServiceException("无权操作退款");
        }
        
        // 检查订单状态
        if (order.getStatus() != 1) {
            throw new ServiceException("只能对已支付的订单进行退款");
        }
        
        // 更新订单状态
        Order updateOrder = new Order();
        updateOrder.setId(id);
        updateOrder.setStatus(3); // 已退款
        updateOrder.setUpdateTime(LocalDateTime.now());
        
        if (orderMapper.updateById(updateOrder) <= 0) {
            throw new ServiceException("退款操作失败");
        }
        
        // 同时更新预约的支付状态
        updateReservationPayStatus(order.getReservationId(), 2); // 已退款
    }
    
    /**
     * 获取用户的订单列表
     */
    public List<Order> getUserOrders(Long userId) {
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getUserId, userId);
        queryWrapper.orderByDesc(Order::getCreateTime);
        
        List<Order> orders = orderMapper.selectList(queryWrapper);
        
        // 查询关联数据
        for (Order order : orders) {
            loadOrderAssociations(order);
        }
        
        return orders;
    }
    
    /**
     * 统计订单
     */
    public BigDecimal calculateTotalAmount(Integer status) {
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            queryWrapper.eq(Order::getStatus, status);
        }
        
        List<Order> orders = orderMapper.selectList(queryWrapper);
        
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Order order : orders) {
            if (order.getAmount() != null) {
                totalAmount = totalAmount.add(order.getAmount());
            }
        }
        
        return totalAmount;
    }
    
    /**
     * 查询关联数据
     */
    private void loadOrderAssociations(Order order) {
        // 查询用户
        User user = userMapper.selectById(order.getUserId());
        order.setUser(user);
        
        // 查询预约
        Reservation reservation = reservationMapper.selectById(order.getReservationId());
        order.setReservation(reservation);
        
        // 加载预约关联的房间和房型信息
        if (reservation != null) {
            // 查询房间信息
            Room room = roomMapper.selectById(reservation.getRoomId());
            if (room != null) {
                // 查询房间类型信息
                RoomType roomType = roomTypeMapper.selectById(room.getRoomTypeId());
                room.setRoomType(roomType);
                reservation.setRoom(room);
            }
        }
    }
    
    /**
     * 更新预约的支付状态
     */
    private void updateReservationPayStatus(Long reservationId, Integer status) {
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation != null) {
            Reservation updateReservation = new Reservation();
            updateReservation.setId(reservationId);
            
            // 状态映射: 0-未支付，1-已支付，2-已退款
            if (status == 0) {
                updateReservation.setPayStatus(0); // 未支付
            } else if (status == 1) {
                updateReservation.setPayStatus(1); // 已支付
                // 支付成功后不自动确认预约，需要管理员手动确认
            } else if (status == 2) {
                // 订单取消时不改变预约状态，只在取消订单方法中处理
            } else if (status == 3) {
                updateReservation.setPayStatus(2); // 已退款
            }
            
            updateReservation.setUpdateTime(LocalDateTime.now());
            
            reservationMapper.updateById(updateReservation);
        }
    }
    
    /**
     * 生成订单编号
     */
    private String generateOrderNo() {
        // 生成格式为: 时间戳(14位) + UUID(8位)
        String timestamp = LocalDateTime.now().toString().replaceAll("[^0-9]", "").substring(0, 14);
        String uuid = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 8);
        return timestamp + uuid;
    }
    
    /**
     * 获取OrderMapper实例，用于控制器中的统计查询
     */
    public OrderMapper getOrderMapper() {
        return orderMapper;
    }
}