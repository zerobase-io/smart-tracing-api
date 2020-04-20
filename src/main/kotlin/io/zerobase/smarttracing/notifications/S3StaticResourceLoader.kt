package io.zerobase.smarttracing.notifications

import com.google.inject.Inject
import ru.vyarus.dropwizard.guice.module.yaml.bind.Config
import software.amazon.awssdk.services.s3.S3Client
import java.io.InputStream

class S3StaticResourceLoader
    @Inject constructor(
        private val s3: S3Client,
        @Config("notifications.staticResourcesBucket") private val bucketName: String
    ): StaticResourceLoader {

    override fun load(path: String): InputStream = s3.getObjectAsBytes { it.bucket(bucketName).key(path) }.asInputStream()
}
