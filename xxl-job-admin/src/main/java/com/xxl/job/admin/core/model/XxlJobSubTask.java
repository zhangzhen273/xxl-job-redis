package com.xxl.job.admin.core.model;

import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

/**
 * @author xuxueli 2019-05-04 16:43:12
 */
public class XxlJobSubTask implements Serializable{

    private Long subTaskId; // '子任务ID',
    @NotNull(message = "任务名称不能为空")
    private String subTaskName;//varchar(64) DEFAULT NULL COMMENT '任务名称',
    @NotNull(message = "JOB-ID不能为空")
    private Integer jobId;// int(11) DEFAULT NULL COMMENT '主任务ID',
    private String taskType;// varchar(32) DEFAULT NULL COMMENT '任务类型；0：周期性；1：单次性任务',
    @NotNull(message = "Cron表达式不能为空")
    private String taskCron;// varchar(16) DEFAULT NULL COMMENT '任务Cron 表达式',
    private String taskParamter;// varchar(512) DEFAULT NULL COMMENT '任务参数',
    private String taskStatus;// varchar(32) DEFAULT '1' COMMENT '任务状态；0-失效，2-暂停；1：开启',
    @NotNull(message = "任务来源不能为空")
    private String appId;// varchar(32) DEFAULT '' COMMENT '任务创建平台方',
    private Date createTime;// datetime DEFAULT NULL COMMENT '创建时间',
    private String createUserId;// varchar(64) DEFAULT NULL COMMENT '创建任务y用户ID',
    private Date updateTime;// datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    private String updateUserId;// varchar(64) DEFAULT NULL COMMENT '更新任务用户ID',
    private Integer optVersion;// int(11) DEFAULT NULL COMMENT '乐观锁',

    public Long getSubTaskId() {
        return subTaskId;
    }

    public void setSubTaskId(Long subTaskId) {
        this.subTaskId = subTaskId;
    }

    public String getSubTaskName() {
        return subTaskName;
    }

    public void setSubTaskName(String subTaskName) {
        this.subTaskName = subTaskName;
    }

    public Integer getJobId() {
        return jobId;
    }

    public void setJobId(Integer jobId) {
        this.jobId = jobId;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getTaskCron() {
        return taskCron;
    }

    public void setTaskCron(String taskCron) {
        this.taskCron = taskCron;
    }

    public String getTaskParamter() {
        return taskParamter;
    }

    public void setTaskParamter(String taskParamter) {
        this.taskParamter = taskParamter;
    }

    public String getTaskStatus() {
        return taskStatus;
    }

    public void setTaskStatus(String taskStatus) {
        this.taskStatus = taskStatus;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(String createUserId) {
        this.createUserId = createUserId;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getUpdateUserId() {
        return updateUserId;
    }

    public void setUpdateUserId(String updateUserId) {
        this.updateUserId = updateUserId;
    }

    public Integer getOptVersion() {
        return optVersion;
    }

    public void setOptVersion(Integer optVersion) {
        this.optVersion = optVersion;
    }
}
