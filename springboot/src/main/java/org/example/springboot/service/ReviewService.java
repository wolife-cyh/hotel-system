package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.springboot.entity.Reservation;
import org.example.springboot.entity.Review;
import org.example.springboot.entity.Room;
import org.example.springboot.entity.User;
import org.example.springboot.entity.RoomType;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.ReservationMapper;
import org.example.springboot.mapper.ReviewMapper;
import org.example.springboot.mapper.RoomMapper;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.mapper.RoomTypeMapper;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评价服务类
 */
@Service
public class ReviewService {
    @Resource
    private ReviewMapper reviewMapper;
    
    @Resource
    private UserMapper userMapper;
    
    @Resource
    private RoomMapper roomMapper;
    
    @Resource
    private ReservationMapper reservationMapper;
    
    @Resource
    private RoomTypeMapper roomTypeMapper;
    
    /**
     * 分页查询评价
     */
    public Page<Review> getReviewsByPage(Long userId, Long roomId, Long reservationId, Integer status, 
                                      Integer currentPage, Integer size) {
        LambdaQueryWrapper<Review> queryWrapper = new LambdaQueryWrapper<>();
        
        // 添加查询条件
        if (userId != null) {
            queryWrapper.eq(Review::getUserId, userId);
        }
        if (roomId != null) {
            queryWrapper.eq(Review::getRoomId, roomId);
        }
        if (reservationId != null) {
            queryWrapper.eq(Review::getReservationId, reservationId);
        }
        if (status != null) {
            queryWrapper.eq(Review::getStatus, status);
        }
        
        // 按创建时间降序排序
        queryWrapper.orderByDesc(Review::getCreateTime);
        
        Page<Review> page = reviewMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        
        // 查询关联数据
        for (Review review : page.getRecords()) {
            loadReviewAssociations(review);
        }
        
        return page;
    }
    
    /**
     * 根据ID获取评价
     */
    public Review getReviewById(Long id) {
        Review review = reviewMapper.selectById(id);
        if (review == null) {
            throw new ServiceException("评价不存在");
        }
        
        // 查询关联数据
        loadReviewAssociations(review);
        
        return review;
    }
    
    /**
     * 查询预约的评价
     */
    public Review getReviewByReservationId(Long reservationId) {
        LambdaQueryWrapper<Review> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Review::getReservationId, reservationId);
        
        Review review = reviewMapper.selectOne(queryWrapper);
        if (review != null) {
            // 查询关联数据
            loadReviewAssociations(review);
        }
        
        return review;
    }
    
    /**
     * 创建评价
     */
    @Transactional
    public Review createReview(Review review) {
        // 获取当前登录用户
        User currentUser = JwtTokenUtils.getCurrentUser();
        review.setUserId(currentUser.getId());
        
        // 检查预约是否存在
        Reservation reservation = reservationMapper.selectById(review.getReservationId());
        if (reservation == null) {
            throw new ServiceException("预约不存在");
        }
        
        // 检查预约是否属于当前用户
        if (!reservation.getUserId().equals(currentUser.getId())) {
            throw new ServiceException("无权为此预约创建评价");
        }
        
        // 检查预约状态是否为已完成
        if (reservation.getStatus() != 3) {
            throw new ServiceException("只能为已完成的预约创建评价");
        }
        
        // 检查是否已经评价
        Review existingReview = getReviewByReservationId(review.getReservationId());
        if (existingReview != null) {
            throw new ServiceException("已经评价过此预约");
        }
        
        // 设置房间ID
        review.setRoomId(reservation.getRoomId());
        
        // 验证评价内容
        validateReview(review);
        
        // 设置默认值
        review.setStatus(0); // 显示
        review.setCreateTime(LocalDateTime.now());
        review.setUpdateTime(LocalDateTime.now());
        
        // 保存评价
        if (reviewMapper.insert(review) <= 0) {
            throw new ServiceException("创建评价失败");
        }
        
        return review;
    }
    
    /**
     * 更新评价
     */
    @Transactional
    public void updateReview(Long id, Review review) {
        // 检查评价是否存在
        Review existingReview = reviewMapper.selectById(id);
        if (existingReview == null) {
            throw new ServiceException("评价不存在");
        }
        
        // 设置ID和不允许修改的字段
        review.setId(id);
        review.setUserId(existingReview.getUserId());
        review.setRoomId(existingReview.getRoomId());
        review.setReservationId(existingReview.getReservationId());
        review.setCreateTime(existingReview.getCreateTime());
        review.setUpdateTime(LocalDateTime.now());
        
        if (reviewMapper.updateById(review) <= 0) {
            throw new ServiceException("更新评价失败");
        }
    }
    
    /**
     * 回复评价
     */
    @Transactional
    public void replyReview(Long id, String reply) {
        // 检查评价是否存在
        Review existingReview = reviewMapper.selectById(id);
        if (existingReview == null) {
            throw new ServiceException("评价不存在");
        }
        
        // 更新回复信息
        Review review = new Review();
        review.setId(id);
        review.setReply(reply);
        review.setReplyTime(LocalDateTime.now());
        review.setUpdateTime(LocalDateTime.now());
        
        if (reviewMapper.updateById(review) <= 0) {
            throw new ServiceException("回复评价失败");
        }
    }
    
    /**
     * 更改评价状态
     */
    @Transactional
    public void updateReviewStatus(Long id, Integer status) {
        // 检查评价是否存在
        Review existingReview = reviewMapper.selectById(id);
        if (existingReview == null) {
            throw new ServiceException("评价不存在");
        }
        
        // 更新状态
        Review review = new Review();
        review.setId(id);
        review.setStatus(status);
        review.setUpdateTime(LocalDateTime.now());
        
        if (reviewMapper.updateById(review) <= 0) {
            throw new ServiceException("更新评价状态失败");
        }
    }
    
    /**
     * 删除评价
     */
    @Transactional
    public void deleteReview(Long id) {
        // 检查评价是否存在
        Review existingReview = reviewMapper.selectById(id);
        if (existingReview == null) {
            throw new ServiceException("评价不存在");
        }
        
        if (reviewMapper.deleteById(id) <= 0) {
            throw new ServiceException("删除评价失败");
        }
    }
    
    /**
     * 获取用户的评价列表
     */
    public List<Review> getUserReviews(Long userId) {
        LambdaQueryWrapper<Review> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Review::getUserId, userId);
        queryWrapper.eq(Review::getStatus, 1); // 只查询显示的评价
        queryWrapper.orderByDesc(Review::getCreateTime);
        
        List<Review> reviews = reviewMapper.selectList(queryWrapper);
        
        // 查询关联数据
        for (Review review : reviews) {
            loadReviewAssociations(review);
        }
        
        return reviews;
    }
    
    /**
     * 获取房间的评价列表
     */
    public List<Review> getRoomReviews(Long roomId) {
        LambdaQueryWrapper<Review> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Review::getRoomId, roomId);
        queryWrapper.eq(Review::getStatus, 1); // 只查询显示的评价
        queryWrapper.orderByDesc(Review::getCreateTime);
        
        List<Review> reviews = reviewMapper.selectList(queryWrapper);
        
        // 查询关联数据
        for (Review review : reviews) {
            loadReviewAssociations(review);
        }
        
        return reviews;
    }
    
    /**
     * 计算房间的平均评分
     */
    public double calculateRoomAverageScore(Long roomId) {
        LambdaQueryWrapper<Review> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Review::getRoomId, roomId);
        queryWrapper.eq(Review::getStatus, 1); // 只查询显示的评价
        
        List<Review> reviews = reviewMapper.selectList(queryWrapper);
        
        if (reviews.isEmpty()) {
            return 0;
        }
        
        int totalScore = 0;
        for (Review review : reviews) {
            totalScore += review.getScore();
        }
        
        return (double) totalScore / reviews.size();
    }
    
    /**
     * 查询关联数据
     */
    private void loadReviewAssociations(Review review) {
        // 查询用户
        User user = userMapper.selectById(review.getUserId());
        review.setUser(user);
        
        // 查询房间
        Room room = roomMapper.selectById(review.getRoomId());
        if (room != null) {
            // 查询房间类型
            RoomType roomType = roomTypeMapper.selectById(room.getRoomTypeId());
            room.setRoomType(roomType);
        }
        review.setRoom(room);
        
        // 查询预约
        Reservation reservation = reservationMapper.selectById(review.getReservationId());
        review.setReservation(reservation);
    }
    
    /**
     * 验证评价内容
     */
    private void validateReview(Review review) {
        if (review.getReservationId() == null) {
            throw new ServiceException("预约ID不能为空");
        }
        
        if (StringUtils.isBlank(review.getContent())) {
            throw new ServiceException("评价内容不能为空");
        }
        
        if (review.getScore() == null) {
            throw new ServiceException("评分不能为空");
        }
        
        if (review.getScore() < 1 || review.getScore() > 5) {
            throw new ServiceException("评分必须在1-5之间");
        }
    }
} 