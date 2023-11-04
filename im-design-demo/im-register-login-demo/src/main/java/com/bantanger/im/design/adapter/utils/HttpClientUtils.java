package com.bantanger.im.design.adapter.utils;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.http.HttpMethod;

import java.util.Objects;

/**
 * @author BanTanger 半糖
 * @Date 2023/11/4 12:03
 */
@Slf4j
public class HttpClientUtils {
    public static JSONObject execute(String url, HttpMethod httpMethod) {
        HttpRequestBase http = null;
        try {
            HttpClient client = HttpClients.createDefault();
            if (httpMethod == HttpMethod.GET) {
                http = new HttpGet(url);
            } else if (httpMethod == HttpMethod.POST) {
                http = new HttpPost(url);
            }
            HttpEntity entity = client.execute(http).getEntity();
            String accessToken = EntityUtils.toString(entity);
            return JSONObject.parseObject(accessToken);
        } catch (Exception e) {
            log.error("请求失败，url 为: {}, 错误信息为 {}", url, e.getMessage());
            throw new RuntimeException("请求失败！" + e.getMessage());
        } finally {
            http.releaseConnection();
        }
    }
}
