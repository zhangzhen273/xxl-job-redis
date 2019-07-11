package com.xxl.job.admin.core.trigger;

import com.alibaba.fastjson.JSON;
import com.clife.utils.cache.memorystorage.IStorageRegion;
import com.clife.utils.cache.memorystorage.RedisOps;
import com.clife.utils.cache.memorystorage.StorageRegion;
import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.model.SubTask;
import com.xxl.job.admin.core.model.SubTaskZset;
import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.schedule.XxlJobDynamicScheduler;
import com.xxl.job.admin.core.util.CronUtils;
import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.TriggerParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.util.StringUtils;

import java.util.*;

public class SubTaskTrigger {

    public static String SUB_TASK_ZSET_PREFIX = "SUB_TASK_ZSET_";
    public static String SUB_TASK_MAP_PREFIX = "SUB_TASK_MAP_";

    public static int BATCH_EXCUTE_SIZE  = 1;

    public static IStorageRegion scheduler = StorageRegion.SCHEDULER;
    public static IStorageRegion trigger = StorageRegion.TRIGGER;
    public static RedisOps.RedisZSetOps zSetOps = scheduler.getOperations(RedisOps.RedisZSetOps.class);
    public static RedisOps.RedisHashOps hashOps = trigger.getOperations(RedisOps.RedisHashOps.class);

    private static Logger logger = LoggerFactory.getLogger(SubTaskTrigger.class);


    /**
     * 分片执行任务，获取当前需要执行任务的size，平均分配到已注册上来的执行器
     */
    public static void blockingExcuteTask(XxlJobGroup group,TriggerParam triggerParam){
        final String key = keyForZSet(String.valueOf(triggerParam.getJobId()));
        final double end = System.currentTimeMillis();
        long jobSize = XxlJobAdminConfig.getAdminConfig().getRedisUtil().countRangeByScore(key,0,end);
        logger.info("当前任务数："+jobSize);
        if(jobSize == 0){
            return;
        }
        List<String> registryList = group.getRegistryList();
        int serviceSize = registryList.size();
        long count = jobSize/serviceSize+1;
        count = count > BATCH_EXCUTE_SIZE ? BATCH_EXCUTE_SIZE : count;
        if (registryList != null && !registryList.isEmpty()) {
            String address ;
            long sum = 0;
            while(sum <= jobSize){
                for(Iterator<String> it = registryList.iterator();it.hasNext();){
                    sum = sum+count;
                    address = it.next();
                    batchExcuteSubTask(group,address,triggerParam,count,key);
                }
            }
        }
    }

    /**
     * 批量执行任务
     * 1 获取需要执行的任务列表
     * 2 删除已获取的任务列表
     * 3 执行任务
     * 4 添加下一任务的执行点
     * @param group
     * @param address 执行器地址
     * @param triggerParam 执行参数
     * @param limit 执行任务最大限制
     * @param key zSet key
     */
    public static void batchExcuteSubTask(final XxlJobGroup group,final String address, final TriggerParam triggerParam,final long limit,final String key){
        zSetOps.multiExec(new SessionCallback() {
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                boolean taskWasTriggered = false;
                redisOperations.watch(key);
                List<String> tasks = getSubTaskList(redisOperations, key,limit);
                if (tasks == null || tasks.size()==0) {
                    redisOperations.unwatch();
                } else {
                    try{
                        ExecutorBiz executorBiz = XxlJobDynamicScheduler.getExecutorBiz(address);
                        Set<ZSetOperations.TypedTuple<Object>> addSet = new HashSet<>();
                        List<TriggerParam> triggerParams = new ArrayList<>();
                        TriggerParam tmp;
                        String subTaskId;
                        SubTask subTask;
                        ZSetOperations.TypedTuple<Object> subTaskZset;
                        List<String> taskStrs = new ArrayList<>();
                        for(Iterator<String> it = tasks.iterator(); it.hasNext();){
                            subTaskId = it.next();
                            subTask = XxlJobAdminConfig.getAdminConfig().getSubTaskService().getSubTask(String.valueOf(triggerParam.getJobId()),subTaskId);
                            if(subTask == null){
                                logger.error("没有找到对应的任务信息，subTaskId："+subTaskId);
                                continue;
                            }
                            Long nextTime = getCronNextExcuteTime(subTask.getCron());
                            if(nextTime<=0){
                                logger.error("cron表达式没有获取到下次执行时间，subTaskId："+subTaskId);
                                continue;
                            }
                            tmp = new TriggerParam();
                            BeanUtils.copyProperties(triggerParam,tmp);
                            tmp.setExcuteId(String.valueOf(UUID.randomUUID()).replace("-",""));
                            tmp.setSubTaskId(subTask.getSubTaskId());
                            triggerParams.add(tmp);
                            // 添加下一个任务
                            subTaskZset = new SubTaskZset(Long.parseLong(subTask.getSubTaskId()),(double)nextTime);
                            addSet.add(subTaskZset);
                            taskStrs.add(subTaskId);
                        }
                        if(taskStrs.size() <= 0){
                            logger.warn("该批次没有找到能够执行的任务");
                            return true;
                        }
                        if(taskStrs.size() != tasks.size()){
                            logger.warn("任务初始化警告：初始化任务个数与处理后的任务个数不一致！");
                        }
                        logger.info("此次执行任务Id："+JSON.toJSONString(taskStrs));
                        redisOperations.multi();
                        SubTaskTrigger.zSetOps.remove(SubTaskTrigger.SUB_TASK_ZSET_PREFIX+String.valueOf(triggerParam.getJobId()), taskStrs.toArray());
                        boolean executionSuccess =(redisOperations.exec() != null);
                        int queueSize = excuteSubTask(group,executorBiz,triggerParams);
                        redisOperations.opsForZSet().add(key,addSet);
                        taskWasTriggered = executionSuccess;
                    }catch (Exception e){
                        logger.error("任务执行异常：blockingExcuteTask；",e);
                    }
                }
                return taskWasTriggered;
            }
        });
    }

    /**
     * 执行任务，执行失败时会轮询已注册的执行器，如果都执行失败，则放弃此次任务的执行，并记录失败日志
     * @param group
     * @param executorBiz
     * @param triggerParams
     */
    private static int excuteSubTask(XxlJobGroup group,ExecutorBiz executorBiz,List<TriggerParam> triggerParams){
        // 批量执行任务
        ReturnT<String> returnT ;
        try{
            returnT = executorBiz.batchRun(triggerParams);
            if(ReturnT.SUCCESS.getCode() == returnT.getCode()){
                return Integer.parseInt(returnT.getMsg());
            }
            logger.error("执行任务返回异常，开始重试",returnT.getMsg());
            returnT = retryExcute(group,triggerParams);
        }catch (Exception e){
            logger.error("执行任务异常，开始重试",e);
            returnT = retryExcute(group,triggerParams);
        }
        if(ReturnT.SUCCESS.getCode() == returnT.getCode()){
            return Integer.parseInt(returnT.getMsg());
        } else {// 没有成功调用，记录日志
            logger.error("执行任务失败");
            saveErrorLog(triggerParams,returnT);
            return -1;
        }
    }

    /**
     *  记录任务执行失败日志
     * @param triggerParams
     * @param returnT
     */
    private static void saveErrorLog(List<TriggerParam> triggerParams,ReturnT<String> returnT){
        TriggerParam triggerParam;
        for(Iterator<TriggerParam> it = triggerParams.iterator();it.hasNext();){
            triggerParam = it.next();
            logger.error("任务执行异常："+ JSON.toJSONString(triggerParam)+";异常信息："+JSON.toJSONString(returnT));
        }
    }


    /**
     * 重试，轮询所有执行器
     * @param group
     * @param triggerParams
     */
    private static ReturnT<String> retryExcute(XxlJobGroup group,List<TriggerParam> triggerParams){
        String address;
        ExecutorBiz executorBiz;
        ReturnT<String> returnT ;
        List<String> registryList = group.getRegistryList();
        if (registryList != null && !registryList.isEmpty()) {
            int retryCount = 1;
            for(Iterator<String> it = registryList.iterator();it.hasNext();){
                try {
                    address = it.next();
                    executorBiz = XxlJobDynamicScheduler.getExecutorBiz(address);
                    returnT = executorBiz.batchRun(triggerParams);
                    if(ReturnT.SUCCESS.getCode() == returnT.getCode()){
                        return returnT;
                    }
                } catch (Exception e) {
                    logger.error("任务重试异常,第"+ retryCount++ +"次重试",e);
                }
            }
            return ReturnT.FAIL;
        }
        return ReturnT.FAIL;
    }


    /**
     * TODO
     * 获取zSert key
     * @param jobId
     * @return
     */
    public static String keyForZSet(final String jobId) {
        String prefix = "";
        if (!StringUtils.isEmpty(scheduler.getStorageUnited().getPrefix())) {
            prefix = scheduler.getStorageUnited().getPrefix() + ":";
        }
        return prefix + scheduler.getName()+":"+SUB_TASK_ZSET_PREFIX+jobId;
    }

    /**
     * TODO
     * 获取zSert key
     * @param jobId
     * @return
     */
    public static String keyForHashMap(final String jobId) {
        String prefix = "";
        if (!StringUtils.isEmpty(trigger.getStorageUnited().getPrefix())) {
            prefix = trigger.getStorageUnited().getPrefix() + ":";
        }
        return prefix + trigger.getName()+":"+SUB_TASK_MAP_PREFIX+jobId;
    }

    /**
     * 计算cron表达式下一执行时间点
     * @param cron
     * @return
     */
    private static long getCronNextExcuteTime(String cron){
        if(StringUtils.isEmpty(cron)){
            return -1;
        }
        long nextTime = CronUtils.computeNextTriggerTime(cron);
        if(nextTime<=0){
            return nextTime;
        }
      /*  if(periodicTaskWithShift && trigger.startsWith("0 0 ")){
            nextTime = nextTime + now.get(Calendar.SECOND)*1000 + now.get(Calendar.MINUTE)*60*1000;
        }*/
        return nextTime;
    }



    /**
     * TODO
     * 获取有限的任务列表
     * @param ops
     * @param key
     * @param limit
     * @return
     */
    private static List<String> getSubTaskList(RedisOperations ops,final String key,final long limit){
        final long minScore = 0;
        final long maxScore = System.currentTimeMillis();

        Set<byte[]> found = (Set<byte[]>) ops.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
                return redisConnection.zRangeByScore(key.getBytes(), minScore, maxScore, 0, limit);
            }
        });
        List<String> foundTasks = new ArrayList<>();
        if (found != null && !found.isEmpty()) {
            Iterator<byte[]> it = found.iterator();
            while(it.hasNext()){
                byte[] valueRaw = it.next();
                String str = RedisOps.getStringSerializer().deserialize(valueRaw);
                if(str != null){
                    foundTasks.add(str);
                }
            }
        }
        return foundTasks;
    }

    public static void main(String[] args) {
        String str = "{\"cron\":\"0/59 * * * * ?\",\"jobId\":\"2\",\"subTaskId\":\"ecf2b11c105a4a91981bee174ec0ef81\"}";
        SubTask task = JSON.parseObject(str,SubTask.class);
        System.out.print(task);
    }

}
