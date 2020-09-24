package io.zerobase.smarttracing.api.features.sessions

import io.fusionauth.jwt.Signer
import io.fusionauth.jwt.domain.Algorithm
import software.amazon.awssdk.arns.Arn
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.kms.KmsClient
import software.amazon.awssdk.services.kms.model.SigningAlgorithmSpec
import software.amazon.awssdk.services.kms.model.SigningAlgorithmSpec.*

private fun SigningAlgorithmSpec.into(): Algorithm = when (this) {
    RSASSA_PKCS1_V1_5_SHA_256 -> Algorithm.RS256
    RSASSA_PKCS1_V1_5_SHA_384 -> Algorithm.RS384
    RSASSA_PKCS1_V1_5_SHA_512 -> Algorithm.RS512
    ECDSA_SHA_256 -> Algorithm.ES256
    ECDSA_SHA_384 -> Algorithm.ES384
    ECDSA_SHA_512 -> Algorithm.ES512
    else -> Algorithm.none
}


class KmsJwtSigner(private val key: Arn, private val kms: KmsClient): Signer {

    private val signingAlgorithm: SigningAlgorithmSpec by lazy {
        val response = kms.describeKey { it.keyId(key.resource().resource()) }
        response.keyMetadata().signingAlgorithms().first()
    }

    override fun getKid(): String {
        return key.toString()
    }

    override fun getAlgorithm(): Algorithm {
        return signingAlgorithm.into()
    }

    override fun sign(payload: String): ByteArray {
        val response = kms.sign { it.keyId(kid).message(SdkBytes.fromString(payload, Charsets.UTF_8)).signingAlgorithm(signingAlgorithm) }
        return response.signature().asByteArray()
    }
}
