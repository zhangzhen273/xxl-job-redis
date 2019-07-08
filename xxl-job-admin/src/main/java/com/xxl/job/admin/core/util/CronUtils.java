package com.xxl.job.admin.core.util;

import com.xxl.job.admin.core.trigger.SubTaskTrigger;
import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.Date;

/**
 * cron 表达式工具类
 */
public class CronUtils {

    private static Logger logger = LoggerFactory.getLogger(CronUtils.class);


    /**
     * 根据CRON表达式计算下次触发时间
     * @param cronExpression
     * @return -1-非法的CRON表达式，0-当前时间之后不再可能触发的表达式
     */
    public static long computeNextTriggerTime(String cronExpression){
        try {
            CronExpression expression = new CronExpression(cronExpression);
            Date nextTime = expression.getNextValidTimeAfter(new Date());
            return nextTime==null ? 0 : nextTime.getTime();
        } catch (ParseException e) {
            logger.error("CRON expression({}) pase error!", cronExpression);
            return -1;
        }
    }

    /**
     * 校验cron表达式下一次和下下次的触发时间是否大于间隔时间 param：second
     * @param cronExpression cron 表达式
     * @param second 间隔时间，单位S
     * @return
     */
    public static boolean validateCronSpaceEnough(String cronExpression,int second){
        CronExpression expression;
        try {
            expression = new CronExpression(cronExpression);
            Date frist = expression.getNextValidTimeAfter(new Date());
            if(frist == null){
                return true;
            }
            Date next =  expression.getNextValidTimeAfter(frist);
            if(next == null){
                return true;
            }
            long spaceTime = next.getTime() - frist.getTime();
            if(second*1000 > spaceTime){
                return false;
            }
            return true;
        } catch (ParseException e) {
            logger.error("CRON expression({}) pase error!", cronExpression);
        }
        return  false;
    }


    /**
     * 比较两个cron 五次内执行频率
     * @param cron1
     * @param cron2
     * @return
     */
    public static long compareCronSpace(String cron1,String cron2){
        return getMinExcuteSpace(cron1,5)-getMinExcuteSpace(cron2,5);

    }

    /**
     *  获取cron 下 size 次最小间隔时间
     * @param cron
     * @param size
     * @return
     */
    public static long getMinExcuteSpace(String cron,int size){
        int count = 0;
        long minSpace = 0;
        while(count++ < size){
            long tmp = getCronNextExcuteSpace(cron);
            minSpace = minSpace < tmp ? minSpace:tmp;
        }
        return minSpace;
    }

    /**
     *  获取cron 下次和下下次执行 的间隔时间
     * @param cron
     * @return
     */
    public static long getCronNextExcuteSpace(String cron){
        CronExpression expression;
        try {
            expression = new CronExpression(cron);
            Date frist = expression.getNextValidTimeAfter(new Date());
            if(frist == null){
                return Long.MAX_VALUE;
            }
            Date next =  expression.getNextValidTimeAfter(frist);
            if(next == null){
                return Long.MAX_VALUE;
            }
            return next.getTime() - frist.getTime();
        } catch (ParseException e) {
            logger.error("CRON expression({}) pase error!", e);
            return -1;
        }
    }



}
