package com.xxl.job.admin.dao;

import com.xxl.job.admin.core.model.XxlJobSubTask;
import com.xxl.job.admin.core.model.XxlJobUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author xuxueli 2019-05-04 16:44:59
 */
@Mapper
public interface XxlJobSubTaskDao {


    List<XxlJobSubTask> pageList(@Param("offset") int offset,
                                 @Param("pagesize") int pagesize,
                                 @Param("username") String subTaskName,
                                 @Param("appId") String appId);

    Integer pageListCount(@Param("username") String subTaskName,
                          @Param("appId") String appId);

    Integer save(XxlJobSubTask subTask);

    Integer update(XxlJobSubTask subTask);

    Integer delete(@Param("subTaskId") Long subTaskId);

    XxlJobSubTask getById(@Param("subTaskId") Long subTaskId);

}
