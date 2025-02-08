package com.bantanger.im.service.utils;

import com.alibaba.fastjson.JSON;
import com.bantanger.im.service.config.GlobalHttpClientConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.util.Map;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/30 18:58
 */
@Component
public class HttpRequestUtils {

    @Resource
    private CloseableHttpClient httpClient;

    @Resource
    private RequestConfig requestConfig;

    @Resource
    GlobalHttpClientConfig httpClientConfig;

    public String doGet(String url, Map<String, Object> params, String charset) throws Exception {
        return doGet(url, params, null, charset);
    }

    /**
     * 通过给的url地址，获取服务器数据
     *
     * @param url     服务器地址
     * @param params  封装用户参数
     * @param charset 设定字符编码
     * @return
     */
    public String doGet(String url, Map<String, Object> params, Map<String, Object> header, String charset) throws Exception {

        if (StringUtils.isEmpty(charset)) {
            charset = "utf-8";
        }
        URIBuilder uriBuilder = new URIBuilder(url);
        // 判断是否有参数
        if (params != null) {
            // 遍历map,拼接请求参数
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                uriBuilder.setParameter(entry.getKey(), entry.getValue().toString());
            }
        }
        // 声明 http get 请求
        HttpGet httpGet = new HttpGet(uriBuilder.build());
        httpGet.setConfig(requestConfig);

        if (header != null) {
            // 遍历map,拼接header参数
            for (Map.Entry<String, Object> entry : header.entrySet()) {
                httpGet.addHeader(entry.getKey(), entry.getValue().toString());
            }
        }

        String result = "";
        try {
            // 发起请求
            CloseableHttpResponse response = httpClient.execute(httpGet);
            // 判断状态码是否为200
            if (response.getStatusLine().getStatusCode() == 200) {
                // 返回响应体的内容
                result = EntityUtils.toString(response.getEntity(), charset);
            }

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return result;
    }

    /**
     * GET请求， 含URL 参数
     *
     * @param url
     * @param params
     * @return 如果状态码为200，则返回body，如果不为200，则返回null
     * @throws Exception
     */
    public String doGet(String url, Map<String, Object> params) throws Exception {
        return doGet(url, params, null);
    }

    /**
     * GET 请求，不含URL参数
     *
     * @param url
     * @return
     * @throws Exception
     */
    public String doGet(String url) throws Exception {
        return doGet(url, null, null);
    }

    public String doPost(String url, Map<String, Object> params, String jsonBody, String charset) throws Exception {
        return doPost(url, params, null, jsonBody, charset);
    }

    /**
     * 带参数的post请求
     *
     * @param url
     * @return
     * @throws Exception
     */
    public String doPost(String url, Map<String, Object> params, Map<String, Object> header, String jsonBody, String charset) throws Exception {

        if (StringUtils.isEmpty(charset)) {
            charset = "utf-8";
        }
        URIBuilder uriBuilder = new URIBuilder(url);
        // 判断是否有参数
        if (params != null) {
            // 遍历map,拼接请求参数
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                uriBuilder.setParameter(entry.getKey(), entry.getValue().toString());
            }
        }

        // 声明httpPost请求
        HttpPost httpPost = new HttpPost(uriBuilder.build());
        // 加入配置信息
        httpPost.setConfig(requestConfig);

        // 判断map是否为空，不为空则进行遍历，封装from表单对象
        if (StringUtils.isNotEmpty(jsonBody)) {
            StringEntity s = new StringEntity(jsonBody, charset);
            s.setContentEncoding(charset);
            s.setContentType("application/json");

            // 把json body放到post里
            httpPost.setEntity(s);
        }

        if (header != null) {
            // 遍历map,拼接header参数
            for (Map.Entry<String, Object> entry : header.entrySet()) {
                httpPost.addHeader(entry.getKey(), entry.getValue().toString());
            }
        }

        String result = "";
//		CloseableHttpClient httpClient = HttpClients.createDefault(); // 单个
        CloseableHttpResponse response = null;
        try {
            // 发起请求
            response = httpClient.execute(httpPost);
            // 判断状态码是否为200
            if (response.getStatusLine().getStatusCode() == 200) {
                // 返回响应体的内容
                result = EntityUtils.toString(response.getEntity(), charset);
            }

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return result;
    }

    /**
     * 不带参数post请求
     *
     * @param url
     * @return
     * @throws Exception
     */
    public String doPost(String url) throws Exception {
        return doPost(url, null, null, null);
    }

    /**
     * get 方法调用的通用方式
     *
     * @param url
     * @param tClass
     * @param map
     * @param charSet
     * @return
     * @throws Exception
     */
    public <T> T doGet(String url, Class<T> tClass, Map<String, Object> map, String charSet) throws Exception {

        String result = doGet(url, map, charSet);
        if (StringUtils.isNotEmpty(result))
            return JSON.parseObject(result, tClass);
        return null;

    }

    /**
     * get 方法调用的通用方式
     *
     * @param url
     * @param tClass
     * @param map
     * @param charSet
     * @return
     * @throws Exception
     */
    public <T> T doGet(String url, Class<T> tClass, Map<String, Object> map, Map<String, Object> header, String charSet) throws Exception {

        String result = doGet(url, map, header, charSet);
        if (StringUtils.isNotEmpty(result))
            return JSON.parseObject(result, tClass);
        return null;

    }

    /**
     * post 方法调用的通用方式
     *
     * @param url
     * @param tClass
     * @param map
     * @param jsonBody
     * @param charSet
     * @return
     * @throws Exception
     */
    public <T> T doPost(String url, Class<T> tClass, Map<String, Object> map, String jsonBody, String charSet) throws Exception {

        String result = doPost(url, map, jsonBody, charSet);
        if (StringUtils.isNotEmpty(result))
            return JSON.parseObject(result, tClass);
        return null;

    }

    public <T> T doPost(String url, Class<T> tClass, Map<String, Object> map, Map<String, Object> header, String jsonBody, String charSet) throws Exception {

        String result = doPost(url, map, header, jsonBody, charSet);
        if (StringUtils.isNotEmpty(result))
            return JSON.parseObject(result, tClass);
        return null;

    }

    /**
     * post 方法调用的通用方式
     *
     * @param url
     * @param map
     * @param jsonBody
     * @param charSet
     * @return
     * @throws Exception
     */
    public String doPostString(String url, Map<String, Object> map, String jsonBody, String charSet) throws Exception {
        return doPost(url, map, jsonBody, charSet);
    }

    /**
     * post 方法调用的通用方式
     *
     * @param url
     * @param map
     * @param jsonBody
     * @param charSet
     * @return
     * @throws Exception
     */
    public String doPostString(String url, Map<String, Object> map, Map<String, Object> header, String jsonBody, String charSet) throws Exception {
        return doPost(url, map, header, jsonBody, charSet);
    }

}
