package com.bantanger.im.service.zookeeper;

import com.bantanger.im.codec.config.ImBootstrapConfig;
import com.bantanger.im.common.constant.Constants;
import com.bantanger.im.service.zookeeper.CuratorZkClient;
import com.bantanger.im.service.zookeeper.ZkManager;
import lombok.extern.slf4j.Slf4j;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/26 11:29
 */
@Slf4j
public class ZkRegistry extends Constants.ZkConstants implements Runnable {

    private ZkManager zkManager;

    private String ip;

    private ImBootstrapConfig.TcpConfig tcpConfig;

    public ZkRegistry(ZkManager zkManager, String ip, ImBootstrapConfig.TcpConfig tcpConfig) {
        this.zkManager = zkManager;
        this.ip = ip;
        this.tcpConfig = tcpConfig;
    }

    @Override
    public void run() {
        CuratorZkClient zkClient = zkManager.getZkClient();
        String tcpPath = ImCoreZkRoot + ImCoreZkRootTcp + "/" + ip + ":" + tcpConfig.getTcpPort();
        if (zkClient.createNode(tcpPath, String.valueOf(tcpConfig.getTcpPort()))) {
            log.info("注册 Zk tcpPath 成功, 消息=[{}]", tcpPath);
        } else {
            log.error("注册 zk tcpPath 失败");
        }

        String websocketPath = ImCoreZkRoot + ImCoreZkRootWeb + "/" + ip + ":" + tcpConfig.getWebSocketPort();
        if (zkClient.createNode(websocketPath, String.valueOf(tcpConfig.getWebSocketPort()))) {
            log.info("注册 Zk websocketPath 成功, 消息=[{}]", websocketPath);
        } else {
            log.error("注册 zk websocketPath 失败");
        }
    }

}
