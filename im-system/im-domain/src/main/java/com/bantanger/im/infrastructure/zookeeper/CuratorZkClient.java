package com.bantanger.im.infrastructure.zookeeper;

import com.bantanger.im.codec.config.ImBootstrapConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.utils.CloseableUtils;
import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Curator 方式实现 Zk 客户端
 * @author BanTanger 半糖
 * @Date 2023/12/12 12:23
 */
@Slf4j
public class CuratorZkClient {

    private final ImBootstrapConfig.ZkConfig zkConfig;

    private CuratorFramework client;

    public CuratorZkClient(ImBootstrapConfig.ZkConfig zkConfig) {
        this.zkConfig = zkConfig;
        init();
    }

    /**
     * 初始化客户端
     */
    public void init() {
        if (null != client) {
            return ;
        }
        // 创建 zk 客户端
        client = ClientFactory.createCurator(zkConfig);

        // 设置监听器
        client.getConnectionStateListenable().addListener((client, newState) -> {
            if (newState == ConnectionState.LOST) {
                // 处理连接丢失
                log.info("连接丢失。正在重新连接...");
            }
        });

        // 启动客户端实例，连接服务器
        client.start();
    }

    /**
     * 销毁客户端
     */
    public void destroy() {
        CloseableUtils.closeQuietly(client);
    }

    /* ============================= IM-WhaleShark ================================
     * ============================== 创建节点方法 ==================================
     *
     * withMode 节点类型
     * 1. PERSISTENT 持久化节点: 一直存在，直到有删除操作主动删除
     *
     * 2. PERSISTENT_SEQUENTIAL 持久化顺序节点: ZK会自动在创建节点时为节点名加上一个次序数字
     * 如创建节点 "/test_"，ZK 会自动补充数字次序
     *
     * 3. EPHEMERAL 临时节点: 临时节点的生命周期与客户端会话绑定，当客户端会话失效，这个节点会被自动清除，
     * 注意: a. 这里指的是会话失效而非连接断开；b. 临时节点下不能创建子节点
     *
     * 4. EPHEMERAL_SEQUENTIAL 临时顺序节点: 带有顺序编号
     * ============================================================================
     */

    /**
     * 创建节点，默认创造持久化节点
     * 如果节点不存在会自动创建，节点存在不会受到影响
     * @param zkPath
     * @param data
     */
    public boolean createNode(String zkPath, String data) {
        return createPersistentNode(zkPath, data);
    }

    /**
     * 创建持久化节点
     * @param zkPath
     * @param data
     */
    public boolean createPersistentNode(String zkPath, String data) {
        try {
            // 创建 zkNode 节点
            // 节点数据为 payload
            byte[] payload = "".getBytes(StandardCharsets.UTF_8);
            if (data != null) {
                payload = data.getBytes(StandardCharsets.UTF_8);
            }
            client.create()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.PERSISTENT)
                    .forPath(zkPath, payload);
            return true;
        } catch (Exception e) {
            log.error("zk 创建持久化节点错误: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 创建临时顺序节点
     * @param zkPath
     * @return
     */
    public String createEphemeralSeqNode(String zkPath) {
        try {
            // 创建临时顺序节点
            client.create()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                    .forPath(zkPath);
            return zkPath;
        } catch (Exception e) {
            log.error("zk 创建临时顺序节点错误: {}", e.getMessage());
        }
        return null;
    }

    /* ============================= IM-WhaleShark ================================
     * ============================== 读取节点方法 ==================================
     *
     * Curator 框架有三个读取节点方法：
     * 1. 判断节点是否存在：   checkExists()
     * 2. 获取节点数据：       getData()
     * 3. 获取子节点列表：     getChildren()
     *
     * 三个方法返回的都是构造者实例，并不会立刻执行，通过构造者实例的链式调用增添具体操作，
     * 最终通过使用 forPath(String zkPath) 在具体节点上执行实际操作
     * ============================================================================
     */

    /**
     * 判断节点是否存在
     * @param zkPath
     * @return
     */
    public boolean isNodeExist(String zkPath) {
        try {
            Stat stat = client.checkExists().forPath(zkPath);
            if (null == stat) {
                log.info("节点: {} 不存在", zkPath);
                return false;
            } else {
                log.info("节点存在 stat is: {}", stat);
                return true;
            }
        } catch (Exception e) {
            log.error("节点查询出错：{}", e.getMessage());
        }
        return false;
    }

    public List<String> getChildren(String zkPath) {
        try {
            return client.getChildren().forPath(zkPath);
        } catch (Exception e) {
            log.error("节点查询出错：{}", e.getMessage());
        }
        return null;
    }

    /* ============================= IM-WhaleShark ================================
     * ============================== 更新节点方法 ==================================
     *
     * Curator 节点更新有两种方式：同步更新和异步更新
     * 同步更新：更新时线程是阻塞的，直至更新操作完成
     * 异步更新：更新时线程是非阻塞的，调用后立刻返回，更新操作异步执行
     *
     * setData() 方法会返回一个 SetDataBuilder 构造者实例，
     * 执行该实例的 forPath(zkPath, payload) 方法实现同步更新操作
     *
     * 而异步更新操作通过 SetDataBuilder 构造者实例的
     * inBackground(AsyncCallback callback) 设置 AsyncCallback 回调实例
     * ============================================================================
     */

    /**
     * 更新节点 【同步模式】
     */
    public void updateNode(String zkPath, String data) {
        if (data == null) {
            log.warn("data 数据为空，zk 无法执行更新操作");
            return ;
        }
        try {
            if (isNodeExist(zkPath)) {
                byte[] payload = data.getBytes(StandardCharsets.UTF_8);
                client.setData().forPath(zkPath, payload);
            } else {
                log.warn("zkPath: {} 有误，zk 无法找到该节点", zkPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 更新节点 【异步模式】
     * @param zkPath
     * @param data
     */
    public void updateNodeAsync(String zkPath, String data) {
        // 设置回调钩子，异步更新操作完成会回调该实例对象方法
        AsyncCallback.StringCallback callback = new AsyncCallback.StringCallback() {
            @Override
            public void processResult(int i, String s, Object o, String s1) {

            }
        };
        if (data == null) {
            log.warn("data 数据为空，zk 无法执行更新操作");
            return ;
        }
        try {
            if (isNodeExist(zkPath)) {
                byte[] payload = data.getBytes(StandardCharsets.UTF_8);
                client.setData()
                        .inBackground(callback)
                        .forPath(zkPath, payload);
            } else {
                log.warn("zkPath: {} 有误，zk 无法找到该节点", zkPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ============================= IM-WhaleShark ================================
     * ============================== 删除节点方法 ==================================
     *
     * Curator 节点删除也有两种方式：同步更新和异步更新，这里不过多赘述
     * ============================================================================
     */

    /**
     * 删除节点 【同步模式】
     * @param zkPath
     */
    public void deleteNode(String zkPath) {
        try {
            if (isNodeExist(zkPath)) {
                client.delete().forPath(zkPath);
            } else {
                log.warn("zkPath: {} 有误，zk 无法找到该节点", zkPath);
            }
        } catch (Exception e) {
            log.error("节点删除出错：{}", e.getMessage());
        }
    }

    /**
     * 删除节点 【异步模式】
     * @param zkPath
     */
    public void deleteNodeAsync(String zkPath) {
        // 设置回调钩子，异步更新操作完成会回调该实例对象方法
        AsyncCallback.StringCallback callback = new AsyncCallback.StringCallback() {
            @Override
            public void processResult(int i, String s, Object o, String s1) {

            }
        };
        try {
            if (isNodeExist(zkPath)) {
                client.delete()
                        .inBackground(callback)
                        .forPath(zkPath);
            } else {
                log.warn("zkPath: {} 有误，zk 无法找到该节点", zkPath);
            }
        } catch (Exception e) {
            log.error("节点删除出错：{}", e.getMessage());
        }
    }

}
