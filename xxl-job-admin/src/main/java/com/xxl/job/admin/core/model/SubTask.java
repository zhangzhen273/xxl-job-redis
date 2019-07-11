package com.xxl.job.admin.core.model;

import com.alibaba.fastjson.JSON;
import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.trigger.SubTaskTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;

import java.io.Serializable;
import java.util.UUID;

public class SubTask implements Serializable{

    private String jobId;
    private String subTaskExcuteId;
    private String subTaskId;
    private String cron;

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getSubTaskId() {
        return subTaskId;
    }

    public void setSubTaskId(String subTaskId) {
        this.subTaskId = subTaskId;
    }

    public void setSubTaskExcuteUUID(){
        this.subTaskExcuteId = String.valueOf(UUID.randomUUID()).replace("-","");
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public String getSubTaskExcuteId() {
        return subTaskExcuteId;
    }

    public void setSubTaskExcuteId(String subTaskExcuteId) {
        this.subTaskExcuteId = subTaskExcuteId;
    }


}
