package com.dsm.service;

import com.dsm.pojo.entity.OperationLog;

import java.util.List;

/**
 * 操作日志数据访问接口
 * 定义操作日志的数据库访问方法
 */
public interface OperationLogService {
    /**
     * 保存操作日志
     *
     * @param log 操作日志对象
     */
    void save(OperationLog log);

    /**
     * 查询最近的操作日志
     *
     * @param limit 限制返回的记录数
     * @return 操作日志列表
     */
    List<OperationLog> findRecentLogs(int limit);

    /**
     * 根据操作类型查询日志
     *
     * @param operationType 操作类型
     * @return 操作日志列表
     */
    List<OperationLog> findByOperationType(String operationType);

    /**
     * 清理指定天数前的日志
     *
     * @param days 天数
     */
    void cleanupOldLogs(int days);
}