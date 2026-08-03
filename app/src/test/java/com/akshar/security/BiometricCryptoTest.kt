package com.akshar.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec

class BiometricCryptoTest {
    @Test
    fun verifySignature_acceptsSignatureForOriginalChallenge() {
        val keyPair = createKeyPair()
        val challenge = "fresh-biometric-challenge".toByteArray()
        val signature = sign(keyPair.private, challenge)

        assertTrue(BiometricCrypto.verifySignature(keyPair.public, challenge, signature))
    }

    @Test
    fun verifySignature_rejectsSignatureForDifferentChallenge() {
        val keyPair = createKeyPair()
        val challenge = "fresh-biometric-challenge".toByteArray()
        val signature = sign(keyPair.private, challenge)

        assertFalse(
            BiometricCrypto.verifySignature(
                keyPair.public,
                "tampered-challenge".toByteArray(),
                signature
            )
        )
    }

    private fun createKeyPair() = KeyPairGenerator.getInstance("EC").run {
        initialize(ECGenParameterSpec("secp256r1"))
        generateKeyPair()
    }

    private fun sign(privateKey: java.security.PrivateKey, challenge: ByteArray): ByteArray {
        return Signature.getInstance("SHA256withECDSA").run {
            initSign(privateKey)
            update(challenge)
            sign()
        }
    }
}
