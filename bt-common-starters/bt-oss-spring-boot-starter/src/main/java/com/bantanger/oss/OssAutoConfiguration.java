package com.bantanger.oss;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.bantanger.oss.client.OssClient;
import com.bantanger.oss.client.S3OssClient;
import com.bantanger.oss.config.OssProperties;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author chensongmin
 * @description
 * @date 2025/2/5
 */
@Configuration
@EnableConfigurationProperties(OssProperties.class)
public class OssAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(S3OssClient.class)
    public OssClient ossClient(AmazonS3 amazonS3) {
        return new S3OssClient(amazonS3);
    }

    @Bean
    @ConditionalOnMissingBean(AmazonS3.class)
    @ConditionalOnProperty(prefix = "oss", name = "enable", havingValue = "true")
    public AmazonS3 amazonS3(OssProperties ossProperties) {
        long nullSize = Stream.<String>builder()
            .add(ossProperties.getEndpoint())
            .add(ossProperties.getAccessKey())
            .add(ossProperties.getAccessSecret())
            .build()
            .filter(Objects::isNull)
            .count();
        if (nullSize > 0) {
            throw new RuntimeException("oss 配置项错误，请检查配置文件 prefix: oss 相关信息");
        }

        AWSCredentials awsCredentials = new BasicAWSCredentials(
            ossProperties.getAccessKey(), ossProperties.getAccessSecret());

        return AmazonS3Client.builder()
            .withEndpointConfiguration(new EndpointConfiguration(ossProperties.getEndpoint(), ossProperties.getRegion()))
            .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
            .withPathStyleAccessEnabled(ossProperties.isPathStyleAccess())
            .disableChunkedEncoding()
            .build();
    }

}
