package com.bantanger.im.infrastructure.interceptor;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/2 22:14
 */
public class RequestHolder {

    private final static ThreadLocal<Boolean> requestHolder = new ThreadLocal<>();

    public static void set(Boolean isadmin) {
        requestHolder.set(isadmin);
    }

    public static Boolean get() {
        return requestHolder.get();
    }

    public static void remove() {
        requestHolder.remove();
    }
}
