package com.xxl.job.admin.service.impl;

import com.xxl.job.admin.core.model.XxlJobSubTask;
import com.xxl.job.admin.dao.XxlJobSubTaskDao;
import com.xxl.job.admin.service.SubTaskService;
import com.xxl.job.core.biz.model.ReturnT;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SubTaskServiceImpl implements SubTaskService{

    @Resource
    private XxlJobSubTaskDao subTaskDao;

    @Override
    public ReturnT<String> addOrUpdateSubTask(XxlJobSubTask subTask) {
        if(subTask == null){
            return new ReturnT<>(ReturnT.FAIL_CODE,"参数异常");
        }
        int count = 0;
        if(subTask.getSubTaskId() == null){//插入
            count = subTaskDao.save(subTask);
            // 插入第一次运行时间到redis
        }else{//更新
            count = subTaskDao.update(subTask);
            // 如果cron 表达式有更新，先删除redis中任务，然后在添加
        }
        if(count > 0){
            return new ReturnT<>("更新成功");
        }
        return new ReturnT<>(ReturnT.FAIL_CODE,"更新失败");
    }

    @Override
    public ReturnT<String> deleteSubTask(Long subTaskId) {
        if(subTaskId == null){
            return new ReturnT<>(ReturnT.FAIL_CODE,"参数异常");
        }
        int count = subTaskDao.delete(subTaskId);
        if(count > 0){
            return new ReturnT<>("删除成功");
        }
        return new ReturnT<>(ReturnT.FAIL_CODE,"删除失败");
    }

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
}
