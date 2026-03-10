package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.springboot.entity.Room;
import org.example.springboot.entity.RoomType;
import org.example.springboot.entity.Reservation;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.RoomMapper;
import org.example.springboot.mapper.RoomTypeMapper;
import org.example.springboot.mapper.ReservationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 房间服务类
 */
@Service
public class RoomService {
    @Resource
    private RoomMapper roomMapper;
    
    @Resource
    private RoomTypeMapper roomTypeMapper;
    
    @Resource
    private ReservationMapper reservationMapper;
    
    /**
     * 分页查询房间
     */
    public Page<Room> getRoomsByPage(String roomNumber, Long roomTypeId, Integer status, 
                                    Integer floor, Integer currentPage, Integer size) {
        LambdaQueryWrapper<Room> queryWrapper = new LambdaQueryWrapper<>();
        
        // 添加查询条件
        if (StringUtils.isNotBlank(roomNumber)) {
            queryWrapper.like(Room::getRoomNumber, roomNumber);
        }
        if (roomTypeId != null) {
            queryWrapper.eq(Room::getRoomTypeId, roomTypeId);
        }
        if (status != null) {
            queryWrapper.eq(Room::getStatus, status);
        }
        if (floor != null) {
            queryWrapper.eq(Room::getFloor, floor);
        }
        
        // 排序
        queryWrapper.orderByAsc(Room::getFloor).orderByAsc(Room::getRoomNumber);
        
        // 分页查询
        Page<Room> page = roomMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        
        // 查询房间类型信息
        if (!page.getRecords().isEmpty()) {
            // 获取所有房间类型ID
            List<Long> roomTypeIds = page.getRecords().stream()
                .map(Room::getRoomTypeId)
                .distinct()
                .collect(Collectors.toList());
            
            // 查询所有相关的房间类型
            List<RoomType> roomTypes = roomTypeMapper.selectList(
                new LambdaQueryWrapper<RoomType>()
                    .in(RoomType::getId, roomTypeIds)
            );
            
            // 将房间类型信息转为Map，方便查找
            Map<Long, RoomType> roomTypeMap = roomTypes.stream()
                .collect(Collectors.toMap(RoomType::getId, roomType -> roomType));
            
            // 为每个房间设置房间类型信息和价格
            page.getRecords().forEach(room -> {
                RoomType roomType = roomTypeMap.get(room.getRoomTypeId());
                if (roomType != null) {
                    room.setRoomType(roomType);
                    room.setPrice(roomType.getPrice());
                }
            });
        }
        
        return page;
    }
    
    /**
     * 查询可预约的房间
     */
    public List<Room> getAvailableRooms(Long roomTypeId, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new ServiceException("入住日期和退房日期不能为空");
        }
        
        if (startDate.isAfter(endDate)) {
            throw new ServiceException("入住日期不能晚于退房日期");
        }
        
        if (startDate.isBefore(LocalDate.now())) {
            throw new ServiceException("入住日期不能早于今天");
        }
        
        // 查询指定类型的所有可用房间
        LambdaQueryWrapper<Room> roomQueryWrapper = new LambdaQueryWrapper<>();
        roomQueryWrapper.eq(Room::getStatus, 1); // 状态为可用
        if (roomTypeId != null) {
            roomQueryWrapper.eq(Room::getRoomTypeId, roomTypeId);
        }
        
        List<Room> allAvailableRooms = roomMapper.selectList(roomQueryWrapper);
        
        if (allAvailableRooms.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 获取所有房间ID
        List<Long> roomIds = allAvailableRooms.stream()
            .map(Room::getId)
            .collect(Collectors.toList());
        
        // 查询在指定日期范围内已被预约的房间
        LambdaQueryWrapper<Reservation> reservationQueryWrapper = new LambdaQueryWrapper<>();
        reservationQueryWrapper.in(Reservation::getRoomId, roomIds)
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
        
        List<Reservation> conflictReservations = reservationMapper.selectList(reservationQueryWrapper);
        
        // 获取已被预约的房间ID
        List<Long> reservedRoomIds = conflictReservations.stream()
            .map(Reservation::getRoomId)
            .distinct()
            .collect(Collectors.toList());
        
        // 过滤掉已被预约的房间
        List<Room> availableRooms = allAvailableRooms.stream()
            .filter(room -> !reservedRoomIds.contains(room.getId()))
            .collect(Collectors.toList());
        
        // 查询房间类型信息
        if (!availableRooms.isEmpty()) {
            // 获取所有房间类型ID
            List<Long> roomTypeIds = availableRooms.stream()
                .map(Room::getRoomTypeId)
                .distinct()
                .collect(Collectors.toList());
            
            // 查询所有相关的房间类型
            List<RoomType> roomTypes = roomTypeMapper.selectList(
                new LambdaQueryWrapper<RoomType>()
                    .in(RoomType::getId, roomTypeIds)
            );
            
            // 将房间类型信息转为Map，方便查找
            Map<Long, RoomType> roomTypeMap = roomTypes.stream()
                .collect(Collectors.toMap(RoomType::getId, roomType -> roomType));
            
            // 为每个房间设置房间类型信息和价格
            availableRooms.forEach(room -> {
                RoomType roomType = roomTypeMap.get(room.getRoomTypeId());
                if (roomType != null) {
                    room.setRoomType(roomType);
                    room.setPrice(roomType.getPrice());
                }
            });
        }
        
        return availableRooms;
    }
    
    /**
     * 根据ID获取房间
     */
    public Room getRoomById(Long id) {
        Room room = roomMapper.selectById(id);
        if (room == null) {
            throw new ServiceException("房间不存在");
        }
        
        // 查询房间类型信息
        RoomType roomType = roomTypeMapper.selectById(room.getRoomTypeId());
        if (roomType != null) {
            room.setRoomType(roomType);
            room.setPrice(roomType.getPrice());
        }
        
        return room;
    }
    
    /**
     * 创建房间
     */
    @Transactional
    public void createRoom(Room room) {
        // 检查房间号是否已存在
        if (roomMapper.selectOne(
                new LambdaQueryWrapper<Room>()
                    .eq(Room::getRoomNumber, room.getRoomNumber())
            ) != null) {
            throw new ServiceException("房间号已存在");
        }
        
        // 检查房间类型是否存在
        if (roomTypeMapper.selectById(room.getRoomTypeId()) == null) {
            throw new ServiceException("房间类型不存在");
        }
        
        // 设置默认状态
        if (room.getStatus() == null) {
            room.setStatus(1); // 默认可用
        }
        
        if (roomMapper.insert(room) <= 0) {
            throw new ServiceException("创建房间失败");
        }
    }
    
    /**
     * 更新房间
     */
    @Transactional
    public void updateRoom(Long id, Room room) {
        // 检查房间是否存在
        Room existingRoom = roomMapper.selectById(id);
        if (existingRoom == null) {
            throw new ServiceException("要更新的房间不存在");
        }
        
        // 检查房间号是否与其他房间重复
        if (room.getRoomNumber() != null && !room.getRoomNumber().equals(existingRoom.getRoomNumber())) {
            Room duplicateRoom = roomMapper.selectOne(
                new LambdaQueryWrapper<Room>()
                    .eq(Room::getRoomNumber, room.getRoomNumber())
            );
            if (duplicateRoom != null && !duplicateRoom.getId().equals(id)) {
                throw new ServiceException("房间号已被使用");
            }
        }
        
        // 检查房间类型是否存在
        if (room.getRoomTypeId() != null && !room.getRoomTypeId().equals(existingRoom.getRoomTypeId())) {
            if (roomTypeMapper.selectById(room.getRoomTypeId()) == null) {
                throw new ServiceException("房间类型不存在");
            }
        }
        
        room.setId(id);
        if (roomMapper.updateById(room) <= 0) {
            throw new ServiceException("更新房间失败");
        }
    }
    
    /**
     * 删除房间
     */
    @Transactional
    public void deleteRoom(Long id) {
        // 检查房间是否存在
        if (roomMapper.selectById(id) == null) {
            throw new ServiceException("要删除的房间不存在");
        }
        
        // 检查是否有关联的预约
        int reservationCount = Math.toIntExact(reservationMapper.selectCount(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getRoomId, id)
                        .in(Reservation::getStatus, 0, 1) // 待确认或已确认的预约
        ));
        if (reservationCount > 0) {
            throw new ServiceException("该房间有未完成的预约，无法删除");
        }
        
        if (roomMapper.deleteById(id) <= 0) {
            throw new ServiceException("删除房间失败");
        }
    }
    
    /**
     * 更新房间状态
     */
    @Transactional
    public void updateRoomStatus(Long id, Integer status) {
        Room room = roomMapper.selectById(id);
        if (room == null) {
            throw new ServiceException("房间不存在");
        }
        
        room.setStatus(status);
        if (roomMapper.updateById(room) <= 0) {
            throw new ServiceException("更新房间状态失败");
        }
    }
    
    /**
     * 根据房间号查询房间
     */
    public Room getRoomByNumber(String roomNumber) {
        if (StringUtils.isBlank(roomNumber)) {
            return null;
        }
        
        LambdaQueryWrapper<Room> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Room::getRoomNumber, roomNumber);
        
        Room room = roomMapper.selectOne(queryWrapper);
        
        // 查询房间类型信息
        if (room != null) {
            RoomType roomType = roomTypeMapper.selectById(room.getRoomTypeId());
            if (roomType != null) {
                room.setRoomType(roomType);
                room.setPrice(roomType.getPrice());
            }
        }
        
        return room;
    }
    
    /**
     * 批量创建房间
     */
    @Transactional
    public void batchCreateRooms(Long roomTypeId, Integer floor, String roomNumberPrefix, Integer startNumber, Integer count) {
        // 检查房间类型是否存在
        if (roomTypeMapper.selectById(roomTypeId) == null) {
            throw new ServiceException("房间类型不存在");
        }
        
        // 检查参数
        if (startNumber < 0 || count <= 0) {
            throw new ServiceException("参数错误");
        }
        
        for (int i = 0; i < count; i++) {
            String roomNumber = roomNumberPrefix + (startNumber + i);
            
            // 检查房间号是否已存在
            if (roomMapper.selectOne(
                    new LambdaQueryWrapper<Room>()
                        .eq(Room::getRoomNumber, roomNumber)
                ) != null) {
                continue; // 跳过已存在的房间号
            }
            
            Room room = new Room();
            room.setRoomNumber(roomNumber);
            room.setRoomTypeId(roomTypeId);
            room.setFloor(floor);
            room.setStatus(1); // 默认可用
            
            roomMapper.insert(room);
        }
    }
} 