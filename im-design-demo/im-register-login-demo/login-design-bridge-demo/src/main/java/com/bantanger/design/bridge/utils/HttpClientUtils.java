package com.bantanger.design.bridge.utils;

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

/**
 * @author BanTanger 半糖
 * @Date 2023/11/4 12:03
 */
@Slf4j
public class HttpClientUtils {
    public static JSONObject execute(String url, String accessToken) {
        HttpRequestBase http = null;
        try {
            HttpClient client = HttpClients.createDefault();
            http = new HttpGet(url);
            http.addHeader("Authorization", "token " + accessToken);

            HttpEntity entity = client.execute(http).getEntity();
            return JSONObject.parseObject(EntityUtils.toString(entity));
        } catch (Exception e) {
            log.error("请求失败，url 为: {}, 错误信息为 {}", url, e.getMessage());
            throw new RuntimeException("请求失败！" + e.getMessage());
        } finally {
            http.releaseConnection();
        }
    }

    public static String execute2(String url, HttpMethod httpMethod) {
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
            accessToken = accessToken.substring(0, accessToken.indexOf("&"));
            return accessToken;
        } catch (Exception e) {
            log.error("请求失败，url 为: {}, 错误信息为 {}", url, e.getMessage());
            throw new RuntimeException("请求失败！" + e.getMessage());
        } finally {
            http.releaseConnection();
        }
    }
}
