package com.dsm.service;

import com.dsm.pojo.entity.LogEntry;
import com.dsm.pojo.entity.OperationLog;

import java.util.List;

public interface LogService {

    List<LogEntry> getLogs(String level);

    void clearLogs();

    void addOperationLog(OperationLog operationLog);

    void addLog(String level, String content);
} 