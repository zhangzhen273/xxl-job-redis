package com.xxl.job.admin.core.conf;

import com.xxl.job.admin.core.model.XxlJobSubTask;
import com.xxl.job.admin.core.util.RedisUtil;
import com.xxl.job.admin.dao.*;
import com.xxl.job.admin.service.SubTaskService;
import com.xxl.job.core.biz.AdminBiz;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

import javax.annotation.Resource;

/**
 * xxl-job config
 *
 * @author xuxueli 2017-04-28
 */
@Configuration
public class XxlJobAdminConfig implements InitializingBean{
    private static XxlJobAdminConfig adminConfig = null;
    public static XxlJobAdminConfig getAdminConfig() {
        return adminConfig;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        adminConfig = this;
    }

    // conf
    @Value("${xxl.job.i18n}")
    private String i18n;

    @Value("${xxl.job.accessToken}")
    private String accessToken;

    @Value("${spring.mail.username}")
    private String emailUserName;

    // dao, service

    @Resource
    private XxlJobLogDao xxlJobLogDao;
    @Resource
    private XxlJobInfoDao xxlJobInfoDao;
    @Resource
    private XxlJobRegistryDao xxlJobRegistryDao;
    @Resource
    private XxlJobGroupDao xxlJobGroupDao;
    @Resource
    private XxlJobSubTaskDao xxlJobSubTaskDao;
    @Resource
    private AdminBiz adminBiz;
    @Resource
    private JavaMailSender mailSender;
    @Resource
    private RedisUtil redisUtil;
    @Resource
    private RedissonService redissonService;
    @Resource
    private SubTaskService subTaskService;


    public String getI18n() {
        return i18n;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getEmailUserName() {
        return emailUserName;
    }

    public XxlJobLogDao getXxlJobLogDao() {
        return xxlJobLogDao;
    }

    public XxlJobInfoDao getXxlJobInfoDao() {
        return xxlJobInfoDao;
    }

    public XxlJobRegistryDao getXxlJobRegistryDao() {
        return xxlJobRegistryDao;
    }

    public XxlJobGroupDao getXxlJobGroupDao() {
        return xxlJobGroupDao;
    }

    public AdminBiz getAdminBiz() {
        return adminBiz;
    }

    public JavaMailSender getMailSender() {
        return mailSender;
    }

    public RedisUtil getRedisUtil() {
        return redisUtil;
    }

    public XxlJobSubTaskDao getXxlJobSubTaskDao() {
        return xxlJobSubTaskDao;
    }

    public RedissonService getRedissonService() {
        return redissonService;
    }

    public SubTaskService getSubTaskService() {
        return subTaskService;
    }
}
