package com.xxl.job.admin.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {
    private static Logger logger = LoggerFactory.getLogger(RedisUtil.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
        // =============================common============================
        /**
         26
         * 指定缓存失效时间
         27
         * @param key 键
        28
         * @param time 时间(秒)
        29
         * @return
        30
         */
    public boolean expire(String key, long time) {
        try {
            if (time > 0) {
                redisTemplate.expire(key, time, TimeUnit.SECONDS);
            }
            return true;
        } catch (Exception e) {
            logger.error("redis 操作异常",e);
            return false;
        }
    }
        /**
         44
         * 根据key 获取过期时间
         45
         * @param key 键 不能为null
        46
         * @return 时间(秒) 返回0代表为永久有效
        47
         */
    public long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }
        /**
         53
         * 判断key是否存在
         54
         * @param key 键
        55
         * @return true 存在 false不存在
        56
         */
    public boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            logger.error("redis 操作异常",e);
            return false;
        }
    }
        /**
         67
         * 删除缓存
         68
         * @param key 可以传一个值 或多个
        69
         */
    @SuppressWarnings("unchecked")
    public void del(String... key) {
        if (key != null && key.length > 0) {
            if (key.length == 1) {
                redisTemplate.delete(key[0]);
            } else {
                redisTemplate.delete(CollectionUtils.arrayToList(key));
            }
        }
    }
        // ============================String=============================
        /**
         83
         * 普通缓存获取
         84
         * @param key 键
        85
         * @return 值
        86
         */
    public Object get(String key) {
        return key == null ? null : redisTemplate.opsForValue().get(key);
    }
        /**
         92
         * 普通缓存放入
         93
         * @param key 键
        94
         * @param value 值
        95
         * @return true成功 false失败
        96
         */
    public boolean set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            logger.error("redis 操作异常",e);
            return false;
        }
    }
        /**
         109
         * 普通缓存放入并设置时间
         110
         * @param key 键
        111
         * @param value 值
        112
         * @param time 时间(秒) time要大于0 如果time小于等于0 将设置无限期
        113
         * @return true成功 false 失败
        114
         */
    public boolean set(String key, Object value, long time) {
        try {
            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
            } else {
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            logger.error("redis 操作异常",e);
            return false;
        }
    }
        /**
         130
         * 递增
         131
         * @param key 键
        132
         * @param delta 要增加几(大于0)
        133
         * @return
        134
         */
    public long incr(String key, long delta) {
        if (delta < 0) {
            throw new RuntimeException("递增因子必须大于0");
        }
        return redisTemplate.opsForValue().increment(key, delta);
    }
        /**
         143
         * 递减
         144
         * @param key 键
        145
         * @param delta 要减少几(小于0)
        146
         * @return
        147
         */
    public long decr(String key, long delta) {
        if (delta < 0) {
            throw new RuntimeException("递减因子必须大于0");
        }
        return redisTemplate.opsForValue().increment(key, -delta);
    }
        // ================================Map=================================
        /**
         157
         * HashGet
         158
         * @param key 键 不能为null
        159
         * @param item 项 不能为null
        160
         * @return 值
        161
         */
    public Object hget(String key, String item) {
        return redisTemplate.opsForHash().get(key, item);
    }
        /**
         167
         * 获取hashKey对应的所有键值
         168
         * @param key 键
        169
         * @return 对应的多个键值
        170
         */
    public Map<Object, Object> hmget(String key) {
        return redisTemplate.opsForHash().entries(key);
    }
        /**
         * HashSet
         * @param key 键
         * @param map 对应多个键值
         * @return true 成功 false 失败
         */
    public boolean hmset(String key, Map<String, Object> map) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            return true;
        } catch (Exception e) {
            logger.error("redis 操作异常",e);
            return false;
        }
    }
        /**
         * HashSet 并设置时间
         * @param key 键
         * @param map 对应多个键值
         * @param time 时间(秒)
         * @return true成功 false失败
         */
    public boolean hmset(String key, Map<String, Object> map, long time) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            logger.error("redis 操作异常",e);
            return false;
        }
    }
        /**
         * 向一张hash表中放入数据,如果不存在将创建
         * @param key 键
         * @param item 项
         * @param value 值
         * @return true 成功 false失败
         */
    public boolean hset(String key, String item, Object value) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
            return true;
        } catch (Exception e) {
            logger.error("redis 操作异常",e);
            return false;
        }
    }
        /**
         * 向一张hash表中放入数据,如果不存在将创建
         * @param key 键
         * @param item 项
         * @param value 值
         * @param time 时间(秒) 注意:如果已存在的hash表有时间,这里将会替换原有的时间
         * @return true 成功 false失败
         */
    public boolean hset(String key, String item, Object value, long time) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            logger.error("redis 操作异常",e);
            return false;
        }
    }
        /**
         * 删除hash表中的值
         * @param key 键 不能为null
         * @param item 项 可以使多个 不能为null
         */
    public void hdel(String key, Object... item) {
        redisTemplate.opsForHash().delete(key, item);
    }
        /**
         * 判断hash表中是否有该项的值
         * @param key 键 不能为null
         * @param item 项 不能为null
         * @return true 存在 false不存在
         */
    public boolean hHasKey(String key, String item) {
        return redisTemplate.opsForHash().hasKey(key, item);
    }
        /**
         * hash递增 如果不存在,就会创建一个 并把新增后的值返回
         * @param key 键
         * @param item 项
         * @param by 要增加几(大于0)
         * @return
         */
    public double hincr(String key, String item, double by) {
        return redisTemplate.opsForHash().increment(key, item, by);
    }
        /**
         * hash递减
         * @param key 键
         * @param item 项
         * @param by 要减少记(小于0)
         * @return
         */
    public double hdecr(String key, String item, double by) {
        return redisTemplate.opsForHash().increment(key, item, -by);
    }
        // ============================set=============================
        /**
         * 根据key获取Set中的所有值
         * @param key 键
         * @return
         */
    public Set<Object> sGet(String key) {
        try {
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            logger.error("redis 操作异常",e);
            return null;
        }
    }
        /**
         * 根据value从一个set中查询,是否存在
         * @param key 键
         * @param value 值
         * @return true 存在 false不存在
         */
    public boolean sHasKey(String key, Object value) {
        try {
            return redisTemplate.opsForSet().isMember(key, value);
        } catch (Exception e) {
            logger.error("redis 操作异常",e);
            return false;
        }
    }
        /**
         * 将数据放入set缓存
         * @param key 键
         * @param values 值 可以是多个
         * @return 成功个数
         */
    public long sSet(String key, Object... values) {
        try {
            return redisTemplate.opsForSet().add(key, values);
        } catch (Exception e) {
            logger.error("redis 操作异常",e);
            return 0;
        }
    }
        /**
         * 将set数据放入缓存
         * @param key 键
         * @param time 时间(秒)
         * @param values 值 可以是多个
         * @return 成功个数
         */
    public long sSetAndTime(String key, long time, Object... values) {
        try {
            Long count = redisTemplate.opsForSet().add(key, values);
            if (time > 0)
            expire(key, time);
            return count;
        } catch (Exception e) {
            logger.error("redis 操作异常",e);
            return 0;
        }
    }
        /**
         * 获取set缓存的长度
         * @param key 键
         * @return
         */
    public long sGetSetSize(String key) {
        try {
            return redisTemplate.opsForSet().size(key);
        } catch (Exception e) {
            logger.error("redis 操作异常",e);
            return 0;
        }
    }
        /**
         * 移除值为value的
         * @param key 键
         * @param values 值 可以是多个
         * @return 移除的个数
         */
    public long setRemove(String key, Object... values) {
        try {
            Long count = redisTemplate.opsForSet().remove(key, values);
            return count;
        } catch (Exception e) {
            logger.error("redis 操作异常",e);
            return 0;
        }
    }
        // ===============================list=================================
        /**
         * 获取list缓存的内容
         * @param key 键
         * @param start 开始
         * @param end 结束 0 到 -1代表所有值
         * @return
         */
    public List<Object> lGet(String key, long start, long end) {
        try {
            return redisTemplate.opsForList().range(key, start, end);
        } catch (Exception e) {
            logger.error("redis 操作异常",e);
            return null;
        }
    }
        /**
         402
         * 获取list缓存的长度
         403
         * @param key 键
        404
         * @return
        405
         */
    public long lGetListSize(String key) {
        try {
            return redisTemplate.opsForList().size(key);
        } catch (Exception e) {
            logger.error("redis 操作异常",e);
            return 0;
        }
    }
        /**
         416
         * 通过索引 获取list中的值
         417
         * @param key 键
        418
         * @param index 索引 index>=0时， 0 表头，1 第二个元素，依次类推；index<0时，-1，表尾，-2倒数第二个元素，依次类推
        419
         * @return
        420
         */
    public Object lGetIndex(String key, long index) {
        try {
            return redisTemplate.opsForList().index(key, index);
        } catch (Exception e) {
            logger.error("redis 操作异常",e);
            return null;
        }
    }
        /**
         * 将list放入缓存
         * @param key 键
         * @param value 值
         * @return
         */
    public boolean lSet(String key, Object value) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            return true;
        } catch (Exception e) {
            logger.error("redis 操作异常",e);
            return false;
        }
    }
        /**
         * 将list放入缓存
         * @param key 键
         * @param value 值
         * @param time 时间(秒)
         * @return
         */
    public boolean lSet(String key, Object value, long time) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            if (time > 0)
            expire(key, time);
            return true;
        } catch (Exception e) {
            logger.error("redis 操作异常",e);
            return false;
        }
    }
        /**
         * 将list放入缓存
         * @param key 键
         * @param value 值
         * @return
         */
    public boolean lSet(String key, List<Object> value) {
        try {
            redisTemplate.opsForList().rightPushAll(key, value);
            return true;
        } catch (Exception e) {
            logger.error("redis 操作异常",e);
            return false;
        }
    }
        /**
         484
         * 将list放入缓存
         *
         * @param key 键
         * @param value 值
         * @param time 时间(秒)
         * @return
         */
    public boolean lSet(String key, List<Object> value, long time) {
        try {
            redisTemplate.opsForList().rightPushAll(key, value);
            if (time > 0)
            expire(key, time);
            return true;
        } catch (Exception e) {
            logger.error("redis 操作异常",e);
            return false;
        }
    }
        /**
         504
         * 根据索引修改list中的某条数据
         505
         * @param key 键
        506
         * @param index 索引
        507
         * @param value 值
        508
         * @return
        509
         */
    public boolean lUpdateIndex(String key, long index, Object value) {
        try {
            redisTemplate.opsForList().set(key, index, value);
            return true;
        } catch (Exception e) {
            logger.error("redis 操作异常",e);
            return false;
        }
    }
        /**
         * 移除N个值为value
         * @param key 键
         * @param count 移除多少个
         * @param value 值
         * @return 移除的个数
         */
    public long lRemove(String key, long count, Object value) {
        try {
            Long remove = redisTemplate.opsForList().remove(key, count, value);
            return remove;
        } catch (Exception e) {
            logger.error("redis 操作异常",e);
            return 0;
        }
    }


    /************************************************* zset 操作start ************************************************/
    /**
     * 添加zset值为value
     * @param key 键
     * @param value 值
     * @param score 排序分值
     * @return 移除的个数
     */
    public boolean addZset(String key,Object value,Double score) {
        try {
            boolean result = redisTemplate.opsForZSet().add(key,value, score);
            return result;
        } catch (Exception e) {
            logger.error("redis 操作异常",e);
            return false;
        }
    }

    public Long addZset(String key, Set<ZSetOperations.TypedTuple<Object>> set) {
        try {
            Long result = redisTemplate.opsForZSet().add(key,set);
            return result;
        } catch (Exception e) {
            logger.error("redis 操作异常",e);
            return 0L;
        }
    }

    /**
     * 删除zset中的值
     * @param key
     * @param value
     * @return
     */
    public long removeZset(String key,Object value) {
        try {
            long result = redisTemplate.opsForZSet().remove(key,value);
            return result;
        } catch (Exception e) {
            logger.error("redis 操作异常",e);
            return 0;
        }
    }

    /**
     * 根据zset中排序删除
     * @param key
     * @param start 开始位置
     * @param end 结束位置
     * @return
     */
    public long removeZsetRange(String key,long start, long end) {
        try {
            long result = redisTemplate.opsForZSet().removeRange(key,start,end);
            return result;
        } catch (Exception e) {
            logger.error("redis 操作异常",e);
            return 0;
        }
    }

    /**
     * 根据zset中分数删除
     * @param key
     * @param start 最小分数
     * @param end 最大分数
     * @return
     */
    public long removeZsetRangeScore(String key,double start, double end) {
        try {
            long result = redisTemplate.opsForZSet().removeRangeByScore(key,start,end);
            return result;
        } catch (Exception e) {
            logger.error("redis 操作异常",e);
            return 0;
        }
    }

    public long removeZset(String key,Object ...objects) {
        try {
            long result = redisTemplate.opsForZSet().remove(key,objects);
            return result;
        } catch (Exception e) {
            logger.error("redis 操作异常",e);
            return 0;
        }
    }


    public Set<Object> getZsetRangeByScore(String key,double start,double end) {
        try {
            Set<Object> result = redisTemplate.opsForZSet().rangeByScore(key,start,end);
            return result;
        } catch (Exception e) {
            logger.error("redis getZset exception",e);
            return null;
        }
    }

    public Set<Object> getZsetRange(String key,long start,long end) {
        try {
            Set<Object> result = redisTemplate.opsForZSet().range(key,start,end);
            return result;
        } catch (Exception e) {
            logger.error("redis getZset exception",e);
            return null;
        }
    }

    public long countRangeByScore(String key,double start,double end) {
        try {
            long count = redisTemplate.opsForZSet().count(key,start,end);
            return count;
        } catch (Exception e) {
            logger.error("redis getZset exception",e);
            return 0;
        }
    }


    /************************************************* zset 操作end **************************************************/

    public RedisTemplate<String, Object> getRedisTemplate() {
        return redisTemplate;
    }

    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
}
