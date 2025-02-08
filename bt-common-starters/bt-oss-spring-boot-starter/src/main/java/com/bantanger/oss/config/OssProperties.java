package com.bantanger.oss.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author chensongmin
 * @description
 * @date 2025/2/5
 */
@ConfigurationProperties(prefix = "oss")
@Setter
@Getter
public class OssProperties {

    private boolean enable = true;

    private boolean pathStyleAccess = true;

    private String accessKey;

    private String accessSecret;

    /**
     * <pre>
     * 通过外网访问OSS服务时，以URL的形式表示访问的OSS资源，详情请参见OSS访问域名使用规则。
     * endpoint 格式：OSS的URL结构为[$Schema]://[$Bucket].[$Endpoint]/[$Object]
     *
     * 例如，您的 Region为华东1（杭州,oss-cn-hangzhou.aliyuncs），Bucket名称为 examplebucket，Object访问路径为 destfolder/example.txt，
     * 则外网访问地址为 https://examplebucket.oss-cn-hangzhou.aliyuncs.com/destfolder/example.txt
     * </pre>
     */
    private String endpoint;

    /**
     * 阿里云 region 对应表
     * https://help.aliyun.com/document_detail/31837.htm?spm=a2c4g.11186623.0.0.695178eb0nD6jp
     */
    private String region;


}
