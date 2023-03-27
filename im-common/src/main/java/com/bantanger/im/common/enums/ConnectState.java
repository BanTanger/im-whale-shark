package com.bantanger.im.common.enums;

import com.bantanger.im.common.enums.CodeAdapter;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/24 21:49
 */
public enum ConnectState implements CodeAdapter {
    // 1.在线 2.离线
    CONNECT_STATE_ONLINE(1),
    CONNECT_STATE_OFFLINE(2);

    private Integer state;

    ConnectState(Integer state) {
        this.state = state;
    }

    @Override
    public Integer getCode() {
        return state;
    }
}
