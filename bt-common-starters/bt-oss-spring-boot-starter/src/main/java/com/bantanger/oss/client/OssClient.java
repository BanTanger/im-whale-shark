package com.bantanger.oss.client;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author chensongmin
 * @description 统一 OSS 客户端接口
 * <pre>
 * 基于 AmazonS3 对对象存储操作进行统一封装
 * 参考：https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/java_s3_code_examples.html
 * </pre>
 * @date 2025/2/5
 */
public interface OssClient {

    /**
     * 创建存储桶
     * @param bucketName 存储桶名称
     */
    void createBucket(String bucketName);

    /**
     * 获取对象 URL
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @return 对象 URL
     */
    String getObjectURL(String bucketName, String objectName);

    /**
     * 获取对象信息
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @return 对象信息
     */
    S3Object getObjectInfo(String bucketName, String objectName);

    /**
     * 上传对象
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @param stream 对象输入流
     * @param size 对象大小
     * @param contextType 对象类型
     * @return 上传结果
     */
    PutObjectResult putObject(String bucketName, String objectName, InputStream stream, long size, String contextType) throws IOException;

    /**
     * 上传对象
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @param stream 对象输入流
     * @return 上传结果
     */
    default PutObjectResult putObject(String bucketName, String objectName, InputStream stream) throws IOException {
        return putObject(bucketName, objectName, stream, stream.available(), "application/octet-stream");
    }

    /**
     * 获取 AmazonS3 客户端
     * @return AmazonS3 客户端
     */
    AmazonS3 getS3Client();
}
