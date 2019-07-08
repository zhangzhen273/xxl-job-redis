package com.xxl.job.admin.core.trigger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.clife.utils.cache.memorystorage.IStorageRegion;
import com.clife.utils.cache.memorystorage.RedisClientFlowControl;
import com.clife.utils.cache.memorystorage.RedisOps;
import com.clife.utils.cache.memorystorage.StorageRegion;
import com.clife.utils.cache.scheduler.TaskType;
import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.model.SubTask;
import com.xxl.job.admin.core.model.SubTaskZset;
import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.route.ExecutorRouteStrategyEnum;
import com.xxl.job.admin.core.schedule.XxlJobDynamicScheduler;
import com.xxl.job.admin.core.util.CronUtils;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.admin.core.util.RedisUtil;
import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.TriggerParam;
import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.util.StringUtils;

import java.text.ParseException;
import java.util.*;

public class SubTaskTrigger {

    public static String SUB_TASK_PREFIX = "SUB_TASK_";
    public static IStorageRegion scheduler = StorageRegion.SCHEDULER;
    public static RedisOps.RedisZSetOps zSetOps = scheduler.getOperations(RedisOps.RedisZSetOps.class);

    public static RedisOps.RedisHashOps hashOps = scheduler.getOperations(RedisOps.RedisHashOps.class);


    private static Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);



    private static Logger logger = LoggerFactory.getLogger(SubTaskTrigger.class);


    /**
     * 分片执行任务，获取当前需要执行任务的size，平均分配到已注册上来的执行器
     */
    public static void blockingExcuteTask(XxlJobGroup group,TriggerParam triggerParam){
        final String key = keyForScheduler(String.valueOf(triggerParam.getJobId()));
        final double end = System.currentTimeMillis();
        final long jobSize = XxlJobAdminConfig.getAdminConfig().getRedisUtil().countRangeByScore(key,0,end);
        List<String> registryList = group.getRegistryList();
        int serviceSize = registryList.size();
        long count = jobSize/serviceSize+1;
        if (registryList != null && !registryList.isEmpty()) {
            String address ;
            ExecutorBiz executorBiz;
            for(Iterator<String> it = registryList.iterator();it.hasNext();){
                try {
                    address = it.next();
                    executorBiz = XxlJobDynamicScheduler.getExecutorBiz(address);
                    batchExcuteSubTask(group,executorBiz,triggerParam,count,key);
                } catch (Exception e) {
                    logger.error("任务执行异常：blockingExcuteTask；",e);
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
     * @param executorBiz 执行器
     * @param triggerParam 执行参数
     * @param limit 执行任务最大限制
     * @param key zSet key
     */
    public static void batchExcuteSubTask(final XxlJobGroup group,final ExecutorBiz executorBiz, final TriggerParam triggerParam,final long limit,final String key){
        zSetOps.multiExec(new SessionCallback() {
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                boolean taskWasTriggered = false;
                //final String key = SUB_TASK_PREFIX+triggerParam.getJobId();//keyForScheduler(triggerParam);
                redisOperations.watch(key);
                List<SubTask> tasks = getSubTaskList(redisOperations, key,limit);
                if (tasks == null || tasks.size()==0) {
                    redisOperations.unwatch();
                } else {
                    logger.info("获取子任务列表："+ tasks.size());
                    Set<ZSetOperations.TypedTuple<Object>> addSet = new HashSet<>();
                    List<TriggerParam> triggerParams = new ArrayList<>();
                    TriggerParam tmp;
                    SubTask task;
                    SubTask newTask;
                    ZSetOperations.TypedTuple<Object> subTaskZset;
                    List<String> taskStrs = new ArrayList<>();
                    for(Iterator<SubTask> it = tasks.iterator(); it.hasNext();){
                        task = it.next();
                        taskStrs.add(JSON.toJSONString(task));
                        tmp = new TriggerParam();
                        BeanUtils.copyProperties(triggerParam,tmp);
                        tmp.setExcuteId(task.getSubTaskExcuteId());
                        tmp.setSubTaskId(task.getSubTaskId());
                        triggerParams.add(tmp);
                        // 添加下一个任务
                        newTask = new SubTask();
                        newTask.setCron(task.getCron());
                        newTask.setJobId(task.getJobId());
                        newTask.setSubTaskExcuteId(task.getSubTaskId());
                        newTask.setSubTaskExcuteUUID();
                        subTaskZset = new SubTaskZset(JSON.toJSONString(newTask),(double)getCronNextExcuteTime(newTask.getCron()));
                        addSet.add(subTaskZset);
                    }
                    redisOperations.multi();
                    Long delCount = redisOperations.opsForZSet().remove(key, taskStrs.toArray());
                    boolean executionSuccess = (redisOperations.exec() != null);
                    logger.info("成功删除："+delCount);
                    excuteSubTask(group,executorBiz,triggerParams);
                    logger.info("成功执行："+addSet.size());
                    Long addCount = redisOperations.opsForZSet().add(key,addSet);
                    logger.info("成功添加："+addCount);
                    taskWasTriggered = executionSuccess;
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
    private static void excuteSubTask(XxlJobGroup group,ExecutorBiz executorBiz,List<TriggerParam> triggerParams){
        // 批量执行任务
        ReturnT<String> returnT ;
        try{
            returnT = executorBiz.batchRun(triggerParams);
            if(ReturnT.SUCCESS.getCode() == returnT.getCode()){
                return;
            }
            returnT = retryExcute(group,triggerParams);
        }catch (Exception e){
            logger.error("执行任务异常",e);
            returnT = retryExcute(group,triggerParams);
        }
        // 最后执行失败
        if(ReturnT.SUCCESS.getCode() != returnT.getCode()){
            // 记录日志
            saveErrorLog(triggerParams,returnT);
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
            for(Iterator<String> it = registryList.iterator();it.hasNext();){
                try {
                    address = it.next();
                    executorBiz = XxlJobDynamicScheduler.getExecutorBiz(address);
                    returnT = executorBiz.batchRun(triggerParams);
                    if(ReturnT.SUCCESS.getCode() == returnT.getCode()){
                        return returnT;
                    }
                } catch (Exception e) {
                    logger.error("任务重试异常",e);
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
    public static String keyForScheduler(final String jobId) {
        String prefix = "";
        if (!StringUtils.isEmpty(scheduler.getStorageUnited().getPrefix())) {
            prefix = scheduler.getStorageUnited().getPrefix() + ":";
        }
        return prefix + scheduler.getName()+"_"+SUB_TASK_PREFIX+jobId;
    }


    /**
     * TODO
     * 计算cron表达式下一执行时间点
     * @param cron
     * @return
     */
    private static long getCronNextExcuteTime(String cron){
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
    private static List<SubTask> getSubTaskList(RedisOperations ops,final String key,final long limit){
        final long minScore = 0;
        final long maxScore = System.currentTimeMillis();

        Set<byte[]> found = (Set<byte[]>) ops.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
                return redisConnection.zRangeByScore(key.getBytes(), minScore, maxScore, 0, limit);
            }
        });
        List<SubTask> foundTasks = new ArrayList<>();
        if (found != null && !found.isEmpty()) {
            Iterator<byte[]> it = found.iterator();
            while(it.hasNext()){
                byte[] valueRaw = it.next();
                String str = String.valueOf(jackson2JsonRedisSerializer.deserialize(valueRaw));
                if(str != null){
                    SubTask task = JSON.parseObject(str,SubTask.class);
                    foundTasks.add(task);
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
