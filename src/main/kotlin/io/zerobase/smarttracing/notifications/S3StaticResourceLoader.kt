package io.zerobase.smarttracing.notifications

import software.amazon.awssdk.services.s3.S3Client
import java.io.InputStream

class S3StaticResourceLoader(private val s3: S3Client, private val bucketName: String): StaticResourceLoader {
    override fun load(path: String): InputStream = s3.getObjectAsBytes { it.bucket(bucketName).key(path) }.asInputStream()
}
