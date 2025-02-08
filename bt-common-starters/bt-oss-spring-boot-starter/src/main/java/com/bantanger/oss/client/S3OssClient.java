package com.bantanger.oss.client;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author chensongmin
 * @description
 * @date 2025/2/5
 */
@Slf4j
@RequiredArgsConstructor
public class S3OssClient implements OssClient{

    private final AmazonS3 amazonS3;

    /**
     * @param bucketName 存储桶名称
     */
    @Override
    public void createBucket(String bucketName) {
        if (this.amazonS3.doesBucketExistV2(bucketName)) {
            log.error("Bucket [{}] already exists.", bucketName);
            return;
        }
        this.amazonS3.createBucket(bucketName);
    }

    /**
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @return
     */
    @Override
    public String getObjectURL(String bucketName, String objectName) {
        URL url = this.amazonS3.getUrl(bucketName, objectName);
        return url.toString();
    }

    /**
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @return
     */
    @Override
    public S3Object getObjectInfo(String bucketName, String objectName) {
        return this.amazonS3.getObject(bucketName, objectName);
    }

    /**
     * @param bucketName  存储桶名称
     * @param objectName  对象名称
     * @param stream      对象输入流
     * @param size        对象大小
     * @param contextType 对象类型
     * @return
     * @throws IOException
     */
    @Override
    public PutObjectResult putObject(String bucketName, String objectName,
        InputStream stream, long size, String contextType) throws IOException
    {
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(size);
        objectMetadata.setContentType(contextType);
        PutObjectRequest putObjectRequest = new PutObjectRequest(
            bucketName, objectName, stream, objectMetadata);
        putObjectRequest.getRequestClientOptions()
            .setReadLimit(Long.valueOf(size).intValue() + 1);

        return this.amazonS3.putObject(putObjectRequest);
    }

    /**
     * @return
     */
    @Override
    public AmazonS3 getS3Client() {
        return this.amazonS3;
    }
}
