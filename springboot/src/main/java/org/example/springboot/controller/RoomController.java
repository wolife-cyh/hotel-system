package org.example.springboot.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.entity.Room;
import org.example.springboot.entity.User;
import org.example.springboot.mapper.RoomMapper;
import org.example.springboot.service.RoomService;
import org.example.springboot.util.JwtTokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name="房间管理接口")
@RestController
@RequestMapping("/room")
public class RoomController {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoomController.class);
    
    @Resource
    private RoomService roomService;
    
    @Resource
    private RoomMapper roomMapper;
    
    @Operation(summary = "分页查询房间")
    @GetMapping("/page")
    public Result<?> getRoomsByPage(
            @RequestParam(required = false) String roomNumber,
            @RequestParam(required = false) Long roomTypeId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer floor,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<Room> page = roomService.getRoomsByPage(roomNumber, roomTypeId, status, floor, currentPage, size);
        return Result.success(page);
    }
    
    @Operation(summary = "查询可预约的房间")
    @GetMapping("/available")
    public Result<?> getAvailableRooms(
            @RequestParam(required = false) Long roomTypeId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        List<Room> rooms = roomService.getAvailableRooms(roomTypeId, startDate, endDate);
        return Result.success(rooms);
    }
    
    @Operation(summary = "根据id获取房间")
    @GetMapping("/{id}")
    public Result<?> getRoomById(@PathVariable Long id) {
        Room room = roomService.getRoomById(id);
        return Result.success(room);
    }
    
    @Operation(summary = "创建房间")
    @PostMapping("/add")
    public Result<?> createRoom(@RequestBody Room room) {
        roomService.createRoom(room);
        return Result.success("创建成功");
    }
    
    @Operation(summary = "更新房间")
    @PutMapping("/{id}")
    public Result<?> updateRoom(@PathVariable Long id, @RequestBody Room room) {
        roomService.updateRoom(id, room);
        return Result.success("更新成功");
    }
    
    @Operation(summary = "删除房间")
    @DeleteMapping("/{id}")
    public Result<?> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return Result.success("删除成功");
    }
    
    @Operation(summary = "更新房间状态")
    @PutMapping("/status/{id}")
    public Result<?> updateRoomStatus(@PathVariable Long id, @RequestParam Integer status) {
        roomService.updateRoomStatus(id, status);
        return Result.success("状态更新成功");
    }
    
    @Operation(summary = "批量创建房间")
    @PostMapping("/batchAdd")
    public Result<?> batchCreateRooms(@RequestBody Map<String, Object> params) {
        Long roomTypeId = Long.valueOf(params.get("roomTypeId").toString());
        Integer floor = Integer.valueOf(params.get("floor").toString());
        String roomNumberPrefix = params.get("roomNumberPrefix").toString();
        Integer startNumber = Integer.valueOf(params.get("startNumber").toString());
        Integer count = Integer.valueOf(params.get("count").toString());
        
        roomService.batchCreateRooms(roomTypeId, floor, roomNumberPrefix, startNumber, count);
        return Result.success("批量创建成功");
    }
    
    @Operation(summary = "获取房间状态统计")
    @GetMapping("/stats")
    public Result<?> getRoomStats() {
        // 只有管理员可以查看统计数据
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (!"ADMIN".equals(currentUser.getRoleCode())) {
            return Result.error("无权查看统计数据");
        }
        
        // 获取维护中的房间数
        int maintenanceRooms = Math.toIntExact(roomMapper.selectCount(
                new LambdaQueryWrapper<Room>()
                        .eq(Room::getStatus, 0)
        ));
        
        // 获取可用的房间数
        int availableRooms = Math.toIntExact(roomMapper.selectCount(
                new LambdaQueryWrapper<Room>()
                        .eq(Room::getStatus, 1)
        ));
        
        Map<String, Integer> stats = Map.of(
            "maintenanceRooms", maintenanceRooms,
            "availableRooms", availableRooms
        );
        
        return Result.success(stats);
    }
}