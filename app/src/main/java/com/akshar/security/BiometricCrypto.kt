package com.akshar.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricPrompt
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.ECGenParameterSpec

object BiometricCrypto {
    private const val KEY_ALIAS = "akspend_biometric_signing_key"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val SIGNATURE_ALGORITHM = "SHA256withECDSA"

    data class AuthenticationRequest(
        val cryptoObject: BiometricPrompt.CryptoObject,
        val challenge: ByteArray,
        val publicKey: PublicKey
    )

    fun createAuthenticationRequest(): AuthenticationRequest {
        return try {
            createRequestWithCurrentKey()
        } catch (_: KeyPermanentlyInvalidatedException) {
            deleteKey()
            createRequestWithCurrentKey()
        }
    }

    internal fun verifySignature(
        publicKey: PublicKey,
        challenge: ByteArray,
        signedChallenge: ByteArray
    ): Boolean {
        return Signature.getInstance(SIGNATURE_ALGORITHM).run {
            initVerify(publicKey)
            update(challenge)
            verify(signedChallenge)
        }
    }

    private fun createRequestWithCurrentKey(): AuthenticationRequest {
        val keyStore = loadKeyStore()
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            generateKeyPair()
        }

        val refreshedKeyStore = loadKeyStore()
        val privateKey = refreshedKeyStore.getKey(KEY_ALIAS, null)
            ?: error("Biometric signing key is unavailable")
        val publicKey = refreshedKeyStore.getCertificate(KEY_ALIAS)?.publicKey
            ?: error("Biometric verification key is unavailable")

        val signature = Signature.getInstance(SIGNATURE_ALGORITHM).apply {
            initSign(privateKey as java.security.PrivateKey)
        }
        val challenge = ByteArray(32).also(SecureRandom()::nextBytes)

        return AuthenticationRequest(
            cryptoObject = BiometricPrompt.CryptoObject(signature),
            challenge = challenge,
            publicKey = publicKey
        )
    }

    private fun generateKeyPair() {
        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN
        )
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(
                0,
                KeyProperties.AUTH_BIOMETRIC_STRONG
            )
        } else {
            @Suppress("DEPRECATION")
            builder.setUserAuthenticationValidityDurationSeconds(-1)
        }

        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEYSTORE).run {
            initialize(builder.build())
            generateKeyPair()
        }
    }

    private fun loadKeyStore(): KeyStore {
        return KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    private fun deleteKey() {
        loadKeyStore().deleteEntry(KEY_ALIAS)
    }
}
