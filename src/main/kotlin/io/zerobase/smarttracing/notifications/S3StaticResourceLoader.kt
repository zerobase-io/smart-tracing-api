package io.zerobase.smarttracing.notifications

import com.google.inject.Inject
import software.amazon.awssdk.services.s3.S3Client
import java.io.InputStream

class S3StaticResourceLoader
    @Inject constructor(
        private val s3: S3Client,
        private val bucketName: String
    ): StaticResourceLoader {

    override fun load(path: String): InputStream = s3.getObjectAsBytes { it.bucket(bucketName).key(path) }.asInputStream()
}
