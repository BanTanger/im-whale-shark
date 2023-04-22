package com.bantanger.im.service.zookeeper;

import com.bantanger.im.codec.config.ImBootstrapConfig;
import com.bantanger.im.common.constant.Constants;
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
        zkManager.createRootNode();
        String tcpPath = ImCoreZkRoot + ImCoreZkRootTcp + "/" + ip + ":" + tcpConfig.getTcpPort();
        zkManager.createNode(tcpPath);
        log.info("注册 Zk tcpPath 成功, 消息=[{}]", tcpPath);

        String websocketPath = ImCoreZkRoot + ImCoreZkRootWeb + "/" + ip + ":" + tcpConfig.getWebSocketPort();
        zkManager.createNode(websocketPath);
        log.info("注册 Zk websocketPath 成功, 消息=[{}]", websocketPath);
    }

}
