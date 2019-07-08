package com.xxl.job.admin.service.impl;

import com.alibaba.fastjson.JSON;
import com.clife.utils.cache.memorystorage.IStorageRegion;
import com.clife.utils.cache.memorystorage.RedisOps;
import com.clife.utils.cache.memorystorage.StorageRegion;
import com.xxl.job.admin.core.model.SubTask;
import com.xxl.job.admin.core.model.XxlJobSubTask;
import com.xxl.job.admin.core.trigger.SubTaskTrigger;
import com.xxl.job.admin.core.util.CronUtils;
import com.xxl.job.admin.dao.XxlJobSubTaskDao;
import com.xxl.job.admin.service.SubTaskService;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.enums.SubTaskEnum;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SubTaskServiceImpl implements SubTaskService{

    @Resource
    private XxlJobSubTaskDao subTaskDao;


    /**
     * 暂停任务
     * @param subTaskId 子任务ID
     * @return
     */
    @Override
    public ReturnT<String> suspendSubTask(Long subTaskId) {
        XxlJobSubTask querySubTask = subTaskDao.getById(subTaskId);
        ReturnT<String> valid = taskStatusValid(querySubTask);
        if(ReturnT.SUCCESS.getCode() != valid.getCode()){
            return valid;
        }
        if(!SubTaskEnum.RUNNING.getCode().equals(querySubTask.getTaskStatus())){
            return new ReturnT<>(ReturnT.FAIL_CODE,"任务不处于运行状态，不能暂停");
        }
        // 更新redis中的值
        SubTaskTrigger.hashOps.delete(SubTaskTrigger.keyForScheduler(String.valueOf(querySubTask.getJobId())),String.valueOf(querySubTask.getSubTaskId()));
        // 删除redis 执行任务
        SubTaskTrigger.zSetOps.remove(SubTaskTrigger.keyForScheduler(String.valueOf(querySubTask.getJobId())),querySubTask.getSubTaskId());
        querySubTask.setTaskStatus(SubTaskEnum.SUSPEND.getCode());
        subTaskDao.update(querySubTask);
        return ReturnT.SUCCESS;
    }

    /**
     * 开启任务
     * @param subTaskId
     * @return
     */
    @Override
    public ReturnT<String> openSubTask(Long subTaskId) {
        XxlJobSubTask querySubTask = subTaskDao.getById(subTaskId);
        if(querySubTask == null){
            return new ReturnT<>(ReturnT.FAIL_CODE,"更新失败，不存在此任务");
        }
        if(!SubTaskEnum.SUSPEND.getCode().equals(querySubTask.getTaskStatus())){
            return new ReturnT<>(ReturnT.FAIL_CODE,"任务不处于暂停状态，不能开启");
        }
        long time = CronUtils.computeNextTriggerTime(querySubTask.getTaskCron());
        if(time == 0){
            return new ReturnT<>(ReturnT.FAIL_CODE,"Cron 表达式错误，没有下一次执行时间");
        }
        SubTaskTrigger.zSetOps.add(SubTaskTrigger.keyForScheduler(String.valueOf(querySubTask.getJobId())),querySubTask.getSubTaskId(),time);
        SubTaskTrigger.hashOps.put(SubTaskTrigger.keyForScheduler(String.valueOf(querySubTask.getJobId())),String.valueOf(querySubTask.getSubTaskId()),JSON.toJSONString(querySubTask));
        querySubTask.setTaskStatus(SubTaskEnum.RUNNING.getCode());
        subTaskDao.update(querySubTask);
        return ReturnT.SUCCESS;
    }


    /**
     *  新增或更新任务
     * @param subTask
     * @return
     */
    @Override
    public ReturnT<String> addOrUpdateSubTask(XxlJobSubTask subTask) {
        if(subTask == null){
            return new ReturnT<>(ReturnT.FAIL_CODE,"参数异常");
        }
        int count;
        long time = CronUtils.computeNextTriggerTime(subTask.getTaskCron());
        if(time == 0){
            return new ReturnT<>(ReturnT.FAIL_CODE,"Cron 表达式错误，没有下一次执行时间");
        }
        if(subTask.getSubTaskId() == null){//插入
            count = subTaskDao.save(subTask);
            if(count <= 0){
                return new ReturnT<>("更新失败");
            }
            // 插入第一次运行时间到redis
            SubTask task = new SubTask();
            task.setJobId(String.valueOf(subTask.getJobId()));
            task.setCron(subTask.getTaskCron());
            task.setSubTaskId(String.valueOf(subTask.getSubTaskId()));
            task.setSubTaskExcuteUUID();
            SubTaskTrigger.zSetOps.add(SubTaskTrigger.keyForScheduler(String.valueOf(subTask.getJobId())),task.getSubTaskId(),time);
            SubTaskTrigger.hashOps.put(SubTaskTrigger.keyForScheduler(String.valueOf(subTask.getJobId())),task.getSubTaskId(),JSON.toJSONString(task));
        }else{//更新
            XxlJobSubTask querySubTask = subTaskDao.getById(subTask.getSubTaskId());
            if(querySubTask == null){
                return new ReturnT<>(ReturnT.FAIL_CODE,"更新失败，不存在此任务");
            }
            count = subTaskDao.update(subTask);
            if(count <= 0){
                return new ReturnT<>("更新失败");
            }
            // 如果cron 表达式有更新，先删除redis中任务，然后在添加
            if(StringUtils.isEmpty(subTask.getTaskCron()) && !subTask.getTaskCron().equals(querySubTask.getTaskCron())){
                SubTask task = new SubTask();
                task.setJobId(String.valueOf(subTask.getJobId()));
                task.setCron(subTask.getTaskCron());
                task.setSubTaskId(String.valueOf(subTask.getSubTaskId()));
                task.setSubTaskExcuteUUID();
                // 更新redis中的值
                SubTaskTrigger.hashOps.put(SubTaskTrigger.keyForScheduler(String.valueOf(subTask.getJobId())),String.valueOf(subTask.getSubTaskId()),JSON.toJSONString(task));
                // 加入下一次执行时间
                SubTaskTrigger.zSetOps.add(SubTaskTrigger.keyForScheduler(String.valueOf(subTask.getJobId())),task.getSubTaskId(),time);
            }
        }
        return ReturnT.SUCCESS;
    }

    /**
     * 删除任务，逻辑删除
     * @param subTaskId
     * @return
     */
    @Override
    public ReturnT<String> deleteSubTask(Long subTaskId) {
        if(subTaskId == null){
            return new ReturnT<>(ReturnT.FAIL_CODE,"参数异常");
        }
        XxlJobSubTask querySubTask = subTaskDao.getById(subTaskId);
        if(querySubTask == null){
            return new ReturnT<>(ReturnT.FAIL_CODE,"更新失败，不存在此任务");
        }
        int count = subTaskDao.delete(subTaskId);
        if(count <= 0){
            return new ReturnT<>("删除失败");
        }
        // 更新redis中的值
        SubTaskTrigger.hashOps.delete(SubTaskTrigger.keyForScheduler(String.valueOf(querySubTask.getJobId())),String.valueOf(querySubTask.getSubTaskId()));
        // 删除redis 执行任务
        SubTaskTrigger.zSetOps.remove(SubTaskTrigger.keyForScheduler(String.valueOf(querySubTask.getJobId())),querySubTask.getSubTaskId());
        return ReturnT.SUCCESS;
    }


    /**
     * 恢复任务
     * @param subTaskId
     * @return
     */
    @Override
    public ReturnT<String> recoverySubTask(Long subTaskId) {
        XxlJobSubTask querySubTask = subTaskDao.getById(subTaskId);
        if(querySubTask == null){
            return new ReturnT<>(ReturnT.FAIL_CODE,"更新失败，不存在此任务");
        }
        if(!SubTaskEnum.INVALID.getCode().equals(querySubTask.getTaskStatus())){
            return new ReturnT<>(ReturnT.FAIL_CODE,"任务不处于暂停状态，不能开启");
        }
        long time = CronUtils.computeNextTriggerTime(querySubTask.getTaskCron());
        if(time == 0){
            return new ReturnT<>(ReturnT.FAIL_CODE,"Cron 表达式错误，没有下一次执行时间");
        }
        SubTaskTrigger.zSetOps.add(SubTaskTrigger.keyForScheduler(String.valueOf(querySubTask.getJobId())),querySubTask.getSubTaskId(),time);
        SubTaskTrigger.hashOps.put(SubTaskTrigger.keyForScheduler(String.valueOf(querySubTask.getJobId())),String.valueOf(querySubTask.getSubTaskId()),JSON.toJSONString(querySubTask));
        querySubTask.setTaskStatus(SubTaskEnum.RUNNING.getCode());
        subTaskDao.update(querySubTask);
        return ReturnT.SUCCESS;
    }


    /**
     *  分页查询任务列表
     * @param start
     * @param pageSize
     * @param subTask
     * @return
     */
    @Override
    public ReturnT<Map<String, Object>> pageList(int start, int pageSize, XxlJobSubTask subTask) {
        int count = subTaskDao.pageListCount(subTask.getSubTaskName(),subTask.getAppId());
        List<XxlJobSubTask> tasks = subTaskDao.pageList(start,pageSize,subTask.getSubTaskName(),subTask.getAppId());
        // package result
        Map<String, Object> maps = new HashMap<String, Object>();
        maps.put("recordsTotal", count);		// 总记录数
        maps.put("data", tasks);
        return new ReturnT(maps);
    }


    private ReturnT<String> taskStatusValid(XxlJobSubTask querySubTask){
        if(querySubTask == null){
            return new ReturnT<>(ReturnT.FAIL_CODE,"更新失败，不存在此任务");
        }
        long time = CronUtils.computeNextTriggerTime(querySubTask.getTaskCron());
        if(time == 0){
            return new ReturnT<>(ReturnT.FAIL_CODE,"Cron 表达式错误，没有下一次执行时间");
        }
        return ReturnT.SUCCESS;
    }

}
