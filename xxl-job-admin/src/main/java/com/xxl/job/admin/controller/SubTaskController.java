package com.xxl.job.admin.controller;

import com.xxl.job.admin.core.model.SubTask;
import com.xxl.job.admin.core.model.XxlJobSubTask;
import com.xxl.job.admin.service.SubTaskService;
import com.xxl.job.core.biz.model.ReturnT;
import io.swagger.annotations.Api;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Map;

/**
 * @author xuxueli 2019-05-04 16:39:50
 */
@Api(description = "子任务相关接口")
@Controller
@RequestMapping("/subTask")
public class SubTaskController {

    @Resource
    private SubTaskService subTaskService;


    @PostMapping("/addOrUpdateSubTask")
    @ResponseBody
    public ReturnT<String> addOrUpdateSubTask(@RequestBody @Valid XxlJobSubTask subTask){
        if(subTask == null){
            return new ReturnT(ReturnT.FAIL_CODE,"参数错误！");
        }
        return subTaskService.addOrUpdateSubTask(subTask);
    }

    @GetMapping("/getSubTask")
    @ResponseBody
    public SubTask getSubTask(String subTaskId, String jobId){
        return subTaskService.getSubTask(jobId,subTaskId);
    }


    @GetMapping("/deleteSubTask")
    @ResponseBody
    public ReturnT<String> deleteSubTask(Long subTaskId){
        if(subTaskId == null){
            return new ReturnT(ReturnT.FAIL_CODE,"参数错误！");
        }
        return subTaskService.deleteSubTask(subTaskId);
    }
    @GetMapping("/pageList")
    @ResponseBody
    public ReturnT<Map<String, Object>> pageList(int start, int pageSize, XxlJobSubTask subTask){
        return subTaskService.pageList(start,pageSize,subTask);
    }

    @GetMapping("/startSubTask")
    @ResponseBody
    public ReturnT<String> startSubTask(Long subTaskId){
        if(subTaskId == null){
            return new ReturnT(ReturnT.FAIL_CODE,"参数错误！");
        }
        return subTaskService.openSubTask(subTaskId);
    }

    @GetMapping("/suspendSubTask")
    @ResponseBody
    public ReturnT<String> suspendSubTask(Long subTaskId){
        if(subTaskId == null){
            return new ReturnT(ReturnT.FAIL_CODE,"参数错误！");
        }
        return subTaskService.suspendSubTask(subTaskId);
    }

    @GetMapping("/recoverySubTask")
    @ResponseBody
    public ReturnT<String> recoverySubTask(Long subTaskId){
        if(subTaskId == null){
            return new ReturnT(ReturnT.FAIL_CODE,"参数错误！");
        }
        return subTaskService.recoverySubTask(subTaskId);
    }

}
