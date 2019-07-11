package com.xxl.job.admin.service;

import com.xxl.job.admin.core.model.SubTask;
import com.xxl.job.admin.core.model.XxlJobSubTask;
import com.xxl.job.core.biz.model.ReturnT;

import java.util.List;
import java.util.Map;

public interface SubTaskService {


    ReturnT<String> suspendSubTask(Long subTaskId);

    ReturnT<String> openSubTask(Long subTaskId);

    ReturnT<String> addOrUpdateSubTask(XxlJobSubTask subTask);

    ReturnT<String> deleteSubTask(Long subTaskId);

    ReturnT<String> recoverySubTask(Long subTaskId);

    ReturnT<Map<String, Object>> pageList(int start, int pageSize, XxlJobSubTask subTask);

    SubTask getSubTask(String jobId, String subTaskId);

}
