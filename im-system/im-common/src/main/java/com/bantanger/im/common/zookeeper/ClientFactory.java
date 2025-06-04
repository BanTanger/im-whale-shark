package com.bantanger.im.common.zookeeper;

import com.bantanger.im.codec.config.ImBootstrapConfig;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

/**
 * @author BanTanger 半糖
 * @Date 2023/12/12 12:09
 */
public class ClientFactory {

    /**
     * 创建 Zk 客户端 Curator 带选项方式，选项以配置文件方式维护
     * @param zkConfig zk 配置文件，前缀为：im.zkConfig
     * @return
     */
    public static CuratorFramework createCurator(ImBootstrapConfig.ZkConfig zkConfig) {
        // 重试策略: 重试时间随次数翻倍，如第一次重试等待 1s，第二次重试等待 2s，第三次重试等待 4s
        ExponentialBackoffRetry retryPolicy =
                new ExponentialBackoffRetry(zkConfig.getRetryTimeMs(), zkConfig.getMaxRetries());

        // builder 模式创建 CuratorFramework 实例
        return CuratorFrameworkFactory.builder()
                .connectString(zkConfig.getZkAddr())
                .retryPolicy(retryPolicy)
                .connectionTimeoutMs(zkConfig.getZkConnectTimeOut())
                .sessionTimeoutMs(zkConfig.getZkSessionTimeOut())
                // 其他的创建选项
                .build();
    }

}
