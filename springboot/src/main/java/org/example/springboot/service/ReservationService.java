package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.springboot.entity.Reservation;
import org.example.springboot.entity.Room;
import org.example.springboot.entity.RoomType;
import org.example.springboot.entity.User;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.ReservationMapper;
import org.example.springboot.mapper.RoomMapper;
import org.example.springboot.mapper.RoomTypeMapper;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 预约服务类
 */
@Service
public class ReservationService {
    @Resource
    private ReservationMapper reservationMapper;
    
    @Resource
    private RoomMapper roomMapper;
    
    @Resource
    private RoomTypeMapper roomTypeMapper;
    
    @Resource
    private UserMapper userMapper;
    
    /**
     * 分页查询预约
     */
    public Page<Reservation> getReservationsByPage(Long userId, Long roomId, Integer status, 
                                                Integer payStatus, LocalDate startDate, 
                                                LocalDate endDate, Integer currentPage, Integer size) {
        LambdaQueryWrapper<Reservation> queryWrapper = new LambdaQueryWrapper<>();
        
        // 添加查询条件
        if (userId != null) {
            queryWrapper.eq(Reservation::getUserId, userId);
        }
        if (roomId != null) {
            queryWrapper.eq(Reservation::getRoomId, roomId);
        }
        if (status != null) {
            queryWrapper.eq(Reservation::getStatus, status);
        }
        if (payStatus != null) {
            queryWrapper.eq(Reservation::getPayStatus, payStatus);
        }
        if (startDate != null) {
            queryWrapper.ge(Reservation::getStartDate, startDate);
        }
        if (endDate != null) {
            queryWrapper.le(Reservation::getEndDate, endDate);
        }
        
        // 排序
        queryWrapper.orderByDesc(Reservation::getCreateTime);
        
        // 分页查询
        Page<Reservation> page = reservationMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        
        // 查询关联信息
        for (Reservation reservation : page.getRecords()) {
            // 查询用户信息
            User user = userMapper.selectById(reservation.getUserId());
            reservation.setUser(user);
            
            // 查询房间信息
            Room room = roomMapper.selectById(reservation.getRoomId());
            if (room != null) {
                // 查询房间类型信息
                RoomType roomType = roomTypeMapper.selectById(room.getRoomTypeId());
                room.setRoomType(roomType);
                reservation.setRoom(room);
            }
        }
        
        return page;
    }
    
    /**
     * 根据ID获取预约
     */
    public Reservation getReservationById(Long id) {
        Reservation reservation = reservationMapper.selectById(id);
        if (reservation == null) {
            throw new ServiceException("预约不存在");
        }
        
        // 查询用户信息
        User user = userMapper.selectById(reservation.getUserId());
        reservation.setUser(user);
        
        // 查询房间信息
        Room room = roomMapper.selectById(reservation.getRoomId());
        if (room != null) {
            // 查询房间类型信息
            RoomType roomType = roomTypeMapper.selectById(room.getRoomTypeId());
            room.setRoomType(roomType);
            reservation.setRoom(room);
        }
        
        return reservation;
    }
    
    /**
     * 创建预约
     */
    @Transactional
    public Reservation createReservation(Reservation reservation) {
        // 获取当前登录用户
        User currentUser = JwtTokenUtils.getCurrentUser();
        reservation.setUserId(currentUser.getId());
        
        // 验证预约信息
        validateReservation(reservation);
        
        // 检查房间是否可用
        checkRoomAvailability(reservation.getRoomId(), reservation.getStartDate(), reservation.getEndDate());
        
        // 设置预约状态
        reservation.setStatus(0); // 待确认
        reservation.setPayStatus(0); // 未支付
        
        // 计算总价
        Room room = roomMapper.selectById(reservation.getRoomId());
        if (room == null) {
            throw new ServiceException("房间不存在");
        }
        
        RoomType roomType = roomTypeMapper.selectById(room.getRoomTypeId());
        if (roomType == null) {
            throw new ServiceException("房间类型不存在");
        }
        
        // 计算入住天数
        long days = ChronoUnit.DAYS.between(reservation.getStartDate(), reservation.getEndDate());
        if (days <= 0) {
            throw new ServiceException("入住天数必须大于0");
        }
        
        // 计算总价
        BigDecimal totalPrice = roomType.getPrice().multiply(new BigDecimal(days));
        reservation.setPrice(totalPrice);
        
        // 保存预约
        if (reservationMapper.insert(reservation) <= 0) {
            throw new ServiceException("创建预约失败");
        }
        
        return reservation;
    }
    
    /**
     * 更新预约
     */
    @Transactional
    public void updateReservation(Long id, Reservation reservation) {
        // 检查预约是否存在
        Reservation existingReservation = reservationMapper.selectById(id);
        if (existingReservation == null) {
            throw new ServiceException("要更新的预约不存在");
        }
        
        // 检查是否可以更新
        if (existingReservation.getStatus() == 2 || existingReservation.getStatus() == 3) {
            throw new ServiceException("已取消或已完成的预约不能修改");
        }
        
        // 验证预约信息
        if (reservation.getRoomId() != null && !reservation.getRoomId().equals(existingReservation.getRoomId()) ||
            reservation.getStartDate() != null && !reservation.getStartDate().equals(existingReservation.getStartDate()) ||
            reservation.getEndDate() != null && !reservation.getEndDate().equals(existingReservation.getEndDate())) {
            // 如果修改了房间或日期，需要重新检查可用性
            Long roomId = reservation.getRoomId() != null ? reservation.getRoomId() : existingReservation.getRoomId();
            LocalDate startDate = reservation.getStartDate() != null ? reservation.getStartDate() : existingReservation.getStartDate();
            LocalDate endDate = reservation.getEndDate() != null ? reservation.getEndDate() : existingReservation.getEndDate();
            
            checkRoomAvailability(roomId, startDate, endDate);
            
            // 重新计算总价
            Room room = roomMapper.selectById(roomId);
            if (room == null) {
                throw new ServiceException("房间不存在");
            }
            
            RoomType roomType = roomTypeMapper.selectById(room.getRoomTypeId());
            if (roomType == null) {
                throw new ServiceException("房间类型不存在");
            }
            
            // 计算入住天数
            long days = ChronoUnit.DAYS.between(startDate, endDate);
            if (days <= 0) {
                throw new ServiceException("入住天数必须大于0");
            }
            
            // 计算总价
            BigDecimal totalPrice = roomType.getPrice().multiply(new BigDecimal(days));
            reservation.setPrice(totalPrice);
        }
        
        reservation.setId(id);
        if (reservationMapper.updateById(reservation) <= 0) {
            throw new ServiceException("更新预约失败");
        }
    }
    
    /**
     * 取消预约
     */
    @Transactional
    public void cancelReservation(Long id) {
        // 检查预约是否存在
        Reservation reservation = reservationMapper.selectById(id);
        if (reservation == null) {
            throw new ServiceException("要取消的预约不存在");
        }
        
        // 检查是否可以取消
        if (reservation.getStatus() == 2 || reservation.getStatus() == 3) {
            throw new ServiceException("已取消或已完成的预约不能再次取消");
        }
        
        // 更新预约状态
        reservation.setStatus(2); // 已取消
        
        // 如果已支付，则设置为已退款
        if (reservation.getPayStatus() == 1) {
            reservation.setPayStatus(2); // 已退款
        }
        
        if (reservationMapper.updateById(reservation) <= 0) {
            throw new ServiceException("取消预约失败");
        }
    }
    
    /**
     * 更新预约状态
     */
    @Transactional
    public void updateReservationStatus(Long id, Integer status) {
        // 检查预约是否存在
        Reservation reservation = reservationMapper.selectById(id);
        if (reservation == null) {
            throw new ServiceException("预约不存在");
        }
        
        // 更新状态
        reservation.setStatus(status);
        
        if (reservationMapper.updateById(reservation) <= 0) {
            throw new ServiceException("更新预约状态失败");
        }
    }
    
    /**
     * 更新支付状态
     */
    @Transactional
    public void updatePayStatus(Long id, Integer payStatus) {
        // 检查预约是否存在
        Reservation reservation = reservationMapper.selectById(id);
        if (reservation == null) {
            throw new ServiceException("预约不存在");
        }
        
        // 更新支付状态
        reservation.setPayStatus(payStatus);
        
        if (reservationMapper.updateById(reservation) <= 0) {
            throw new ServiceException("更新支付状态失败");
        }
    }
    
    /**
     * 查询用户的预约记录
     */
    public List<Reservation> getUserReservations(Long userId) {
        LambdaQueryWrapper<Reservation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Reservation::getUserId, userId);
        queryWrapper.orderByDesc(Reservation::getCreateTime);
        
        List<Reservation> reservations = reservationMapper.selectList(queryWrapper);
        
        // 查询关联信息
        for (Reservation reservation : reservations) {
            // 查询房间信息
            Room room = roomMapper.selectById(reservation.getRoomId());
            if (room != null) {
                // 查询房间类型信息
                RoomType roomType = roomTypeMapper.selectById(room.getRoomTypeId());
                room.setRoomType(roomType);
                reservation.setRoom(room);
            }
        }
        
        return reservations;
    }
    
    /**
     * 查询用户指定状态的预约记录
     */
    public List<Reservation> getUserReservationsByStatus(Long userId, Integer status) {
        LambdaQueryWrapper<Reservation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Reservation::getUserId, userId);
        if (status != null) {
            queryWrapper.eq(Reservation::getStatus, status);
        }
        queryWrapper.orderByDesc(Reservation::getCreateTime);
        
        List<Reservation> reservations = reservationMapper.selectList(queryWrapper);
        
        // 查询关联信息
        for (Reservation reservation : reservations) {
            // 查询房间信息
            Room room = roomMapper.selectById(reservation.getRoomId());
            if (room != null) {
                // 查询房间类型信息
                RoomType roomType = roomTypeMapper.selectById(room.getRoomTypeId());
                room.setRoomType(roomType);
                reservation.setRoom(room);
            }
        }
        
        return reservations;
    }
    
    /**
     * 验证预约信息
     */
    private void validateReservation(Reservation reservation) {
        if (reservation.getRoomId() == null) {
            throw new ServiceException("房间ID不能为空");
        }
        
        if (reservation.getStartDate() == null) {
            throw new ServiceException("入住日期不能为空");
        }
        
        if (reservation.getEndDate() == null) {
            throw new ServiceException("退房日期不能为空");
        }
        
        if (reservation.getStartDate().isAfter(reservation.getEndDate())) {
            throw new ServiceException("入住日期不能晚于退房日期");
        }
        
        if (reservation.getStartDate().isBefore(LocalDate.now())) {
            throw new ServiceException("入住日期不能早于今天");
        }
        
        if (reservation.getGuestCount() == null || reservation.getGuestCount() <= 0) {
            throw new ServiceException("入住人数必须大于0");
        }
        
        if (StringUtils.isBlank(reservation.getGuestName())) {
            throw new ServiceException("入住人姓名不能为空");
        }
        
        if (StringUtils.isBlank(reservation.getGuestPhone())) {
            throw new ServiceException("入住人电话不能为空");
        }
    }
    
    /**
     * 检查房间是否可用
     */
    private void checkRoomAvailability(Long roomId, LocalDate startDate, LocalDate endDate) {
        // 检查房间是否存在且可用
        Room room = roomMapper.selectById(roomId);
        if (room == null) {
            throw new ServiceException("房间不存在");
        }
        
        if (room.getStatus() != 1) {
            throw new ServiceException("房间不可用");
        }
        
        // 检查该房间在所选日期范围内是否已被预约
        LambdaQueryWrapper<Reservation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Reservation::getRoomId, roomId)
            .and(wrapper -> wrapper
                // 预约已确认或待确认
                .in(Reservation::getStatus, 0, 1)
                // 预约时间与查询时间有交集
                .and(timeWrapper -> timeWrapper
                    // 开始日期在查询范围内
                    .between(Reservation::getStartDate, startDate, endDate.minusDays(1))
                    .or()
                    // 结束日期在查询范围内
                    .between(Reservation::getEndDate, startDate.plusDays(1), endDate)
                    .or()
                    // 查询范围完全被预约范围包含
                    .and(containWrapper -> containWrapper
                        .le(Reservation::getStartDate, startDate)
                        .ge(Reservation::getEndDate, endDate)
                    )
                )
            );
        
        int count = Math.toIntExact(reservationMapper.selectCount(queryWrapper));
        if (count > 0) {
            throw new ServiceException("所选日期该房间已被预约，请选择其他日期或房间");
        }
    }
} 