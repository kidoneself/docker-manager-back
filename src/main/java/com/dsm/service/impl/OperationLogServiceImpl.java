package com.dsm.service.impl;

import com.dsm.pojo.entity.OperationLog;
import com.dsm.service.OperationLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志数据访问实现类
 * 使用Spring JDBC Template实现数据库操作
 */
@Slf4j
@Repository
public class OperationLogServiceImpl implements OperationLogService {

    /**
     * 操作日志表名
     */
    private static final String TABLE_NAME = "operation_logs";
    /**
     * 操作日志行映射器
     */
    private static final RowMapper<OperationLog> ROW_MAPPER = new OperationLogRowMapper();
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void save(OperationLog logs) {
        String sql = "INSERT INTO " + TABLE_NAME + " (operation_type, operation_time, details, status) VALUES (?, ?, ?, ?)";
        try {
            jdbcTemplate.update(sql,
                    logs.getOperationType(),
                    logs.getOperationTime(),
                    logs.getDetails(),
                    logs.getStatus());
        } catch (Exception e) {
            log.error("保存操作日志失败", e);
            throw new RuntimeException("保存操作日志失败", e);
        }
    }

    @Override
    public List<OperationLog> findRecentLogs(int limit) {
        String sql = "SELECT * FROM " + TABLE_NAME + " ORDER BY operation_time DESC LIMIT ?";
        try {
            return jdbcTemplate.query(sql, ROW_MAPPER, limit);
        } catch (Exception e) {
            log.error("查询最近操作日志失败", e);
            throw new RuntimeException("查询最近操作日志失败", e);
        }
    }

    @Override
    public List<OperationLog> findByOperationType(String operationType) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE operation_type = ? ORDER BY operation_time DESC";
        try {
            return jdbcTemplate.query(sql, ROW_MAPPER, operationType);
        } catch (Exception e) {
            log.error("根据操作类型查询日志失败", e);
            throw new RuntimeException("根据操作类型查询日志失败", e);
        }
    }

    @Override
    public void cleanupOldLogs(int days) {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE operation_time < ?";
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        try {
            int affected = jdbcTemplate.update(sql, cutoffDate);
            log.info("清理了{}条{}天前的操作日志", affected, days);
        } catch (Exception e) {
            log.error("清理旧操作日志失败", e);
            throw new RuntimeException("清理旧操作日志失败", e);
        }
    }

    /**
     * 操作日志行映射器
     * 用于将数据库结果集映射为OperationLog对象
     */
    private static class OperationLogRowMapper implements RowMapper<OperationLog> {
        @Override
        public OperationLog mapRow(ResultSet rs, int rowNum) throws SQLException {
            OperationLog log = new OperationLog();
            log.setId(rs.getLong("id"));
            log.setOperationType(rs.getString("operation_type"));
            log.setOperationTime(rs.getTimestamp("operation_time").toLocalDateTime());
            log.setDetails(rs.getString("details"));
            log.setStatus(rs.getString("status"));
            return log;
        }
    }
}