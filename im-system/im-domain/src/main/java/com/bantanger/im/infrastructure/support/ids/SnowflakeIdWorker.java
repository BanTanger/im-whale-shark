package com.bantanger.im.infrastructure.support.ids;

import cn.hutool.core.date.SystemClock;
import lombok.extern.slf4j.Slf4j;

/**
 * 雪花算法生成分布式 ID
 * @author BanTanger 半糖
 * @Date 2023/4/5 12:56
 */
@Slf4j
public class SnowflakeIdWorker {

    /**
     * 初始偏移时间戳
     */
    private static final long OFFSET = 1546300800L;

    /**
     * 机器id (0~15 保留 16~31作为备份机器)
     */
    private static long WORKER_ID;

    /**
     * 机器id所占位数 (5bit, 支持最大机器数 2^5 = 32)
     */
    private static final long WORKER_ID_BITS = 5L;
    /**
     * 自增序列所占位数 (16bit, 支持最大每秒生成 2^16 = 65536)
     */
    private static final long SEQUENCE_ID_BITS = 16L;
    /**
     * 机器id偏移位数
     */
    private static final long WORKER_SHIFT_BITS = SEQUENCE_ID_BITS;
    /**
     * 自增序列偏移位数
     */
    private static final long OFFSET_SHIFT_BITS = SEQUENCE_ID_BITS + WORKER_ID_BITS;
    /**
     * 机器标识最大值 (2^5 / 2 - 1 = 15)
     */
    private static final long WORKER_ID_MAX = ((1 << WORKER_ID_BITS) - 1) >> 1;
    /**
     * 备份机器ID开始位置 (2^5 / 2 = 16)
     */
    private static final long BACK_WORKER_ID_BEGIN = (1 << WORKER_ID_BITS) >> 1;
    /**
     * 自增序列最大值 (2^16 - 1 = 65535)
     */
    private static final long SEQUENCE_MAX = (1 << SEQUENCE_ID_BITS) - 1;
    /**
     * 发生时间回拨时容忍的最大回拨时间 (秒)
     */
    private static final long BACK_TIME_MAX = 1L;

    /**
     * 上次生成ID的时间戳 (秒)
     */
    private static long lastTimestamp = 0L;
    /**
     * 当前秒内序列 (2^16)
     */
    private static long sequence = 0L;
    /**
     * 备份机器上次生成ID的时间戳 (秒)
     */
    private static long lastTimestampBak = 0L;
    /**
     * 备份机器当前秒内序列 (2^16)
     */
    private static long sequenceBak = 0L;

    //==============================Constructors====================

    /**
     * 构造函数
     *
     * @param workerId     工作ID (0~31)
     */
    public SnowflakeIdWorker(long workerId) {
        if (workerId < 0 || workerId > WORKER_ID_MAX) {
            throw new IllegalArgumentException(String.format("cmallshop.workerId范围: 0 ~ %d 目前: %d", WORKER_ID_MAX, workerId));
        }
        WORKER_ID = workerId;
    }

    // ==============================Methods=================================
    public static long nextId() {
        return nextId(SystemClock.now() / 1000);
    }

    /**
     * 主机器自增序列
     *
     * @param timestamp 当前Unix时间戳
     * @return long
     */
    private static synchronized long nextId(long timestamp) {
        // 时钟回拨检查
        if (timestamp < lastTimestamp) {
            // 发生时钟回拨
            log.warn("时钟回拨, 启用备份机器ID: now: [{}] last: [{}]", timestamp, lastTimestamp);
            return nextIdBackup(timestamp);
        }

        // 开始下一秒
        if (timestamp != lastTimestamp) {
            lastTimestamp = timestamp;
            sequence = 0L;
        }
        if (0L == (++sequence & SEQUENCE_MAX)) {
            // 秒内序列用尽
//            log.warn("秒内[{}]序列用尽, 启用备份机器ID序列", timestamp);
            sequence--;
            return nextIdBackup(timestamp);
        }

        return ((timestamp - OFFSET) << OFFSET_SHIFT_BITS) | (WORKER_ID << WORKER_SHIFT_BITS) | sequence;
    }

    /**
     * 阻塞到下一个毫秒，直到获得新的时间戳
     *
     * @param lastTimestamp 上次生成ID的时间截
     * @return 当前时间戳
     */
    protected long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    /**
     * 备份机器自增序列
     * @param timestamp timestamp 当前Unix时间戳
     * @return long
     */
    private static long nextIdBackup(long timestamp) {
        if (timestamp < lastTimestampBak) {
            if (lastTimestampBak - SystemClock.now() / 1000 <= BACK_TIME_MAX) {
                timestamp = lastTimestampBak;
            } else {
                throw new RuntimeException(String.format("时钟回拨: now: [%d] last: [%d]", timestamp, lastTimestampBak));
            }
        }

        if (timestamp != lastTimestampBak) {
            lastTimestampBak = timestamp;
            sequenceBak = 0L;
        }

        if (0L == (++sequenceBak & SEQUENCE_MAX)) {
            // 秒内序列用尽
//            logger.warn("秒内[{}]序列用尽, 备份机器ID借取下一秒序列", timestamp);
            return nextIdBackup(timestamp + 1);
        }

        return ((timestamp - OFFSET) << OFFSET_SHIFT_BITS) | ((WORKER_ID ^ BACK_WORKER_ID_BEGIN) << WORKER_SHIFT_BITS) | sequenceBak;
    }


    /**
     * 返回以毫秒为单位的当前时间
     *
     * @return 当前时间(毫秒)
     */
    protected long timeGen() {
        return System.currentTimeMillis();
    }

}
