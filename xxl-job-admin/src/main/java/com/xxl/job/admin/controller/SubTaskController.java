package com.xxl.job.admin.controller;

import com.xxl.job.admin.core.model.XxlJobSubTask;
import com.xxl.job.admin.service.SubTaskService;
import com.xxl.job.core.biz.model.ReturnT;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Map;

/**
 * @author xuxueli 2019-05-04 16:39:50
 */
@Controller
@RequestMapping("/subTask")
public class SubTaskController {

    @Resource
    private SubTaskService subTaskService;

    @RequestMapping("/addOrUpdateSubTask")
    @ResponseBody
    public ReturnT<String> addOrUpdateSubTask(@RequestBody @Valid XxlJobSubTask subTask){
        if(subTask == null){
            return new ReturnT(ReturnT.FAIL_CODE,"参数错误！");
        }
        return subTaskService.addOrUpdateSubTask(subTask);
    }

    @RequestMapping("/deleteSubTask")
    @ResponseBody
    public ReturnT<String> deleteSubTask(Long subTaskId){
        if(subTaskId == null){
            return new ReturnT(ReturnT.FAIL_CODE,"参数错误！");
        }
        return subTaskService.deleteSubTask(subTaskId);
    }
    @RequestMapping("/pageList")
    @ResponseBody
    public ReturnT<Map<String, Object>> pageList(int start, int pageSize, XxlJobSubTask subTask){
        ReturnT<Map<String, Object>> result;
        result = subTaskService.pageList(start,pageSize,subTask);
        return result;
    }

    @RequestMapping("/startSubTask")
    @ResponseBody
    public ReturnT<String> startSubTask(Long subTaskId){
        if(subTaskId == null){
            return new ReturnT(ReturnT.FAIL_CODE,"参数错误！");
        }
        return subTaskService.openSubTask(subTaskId);
    }

    @RequestMapping("/suspendSubTask")
    @ResponseBody
    public ReturnT<String> suspendSubTask(Long subTaskId){
        if(subTaskId == null){
            return new ReturnT(ReturnT.FAIL_CODE,"参数错误！");
        }
        return subTaskService.suspendSubTask(subTaskId);
    }

    @RequestMapping("/recoverySubTask")
    @ResponseBody
    public ReturnT<String> recoverySubTask(Long subTaskId){
        if(subTaskId == null){
            return new ReturnT(ReturnT.FAIL_CODE,"参数错误！");
        }
        return subTaskService.recoverySubTask(subTaskId);
    }

}
