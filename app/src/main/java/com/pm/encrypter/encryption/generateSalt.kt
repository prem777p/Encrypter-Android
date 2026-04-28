package com.pm.encrypter.encryption

import java.security.SecureRandom
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

// Generates a random 16-byte salt
fun generateSalt(): ByteArray {
    val salt = ByteArray(16)
    SecureRandom().nextBytes(salt)
    return salt
}

// Turns the text password and the salt into a 256-bit AES key
fun deriveKeyFromPassword(password: String, salt: ByteArray): SecretKey {
    val iterationCount = 65536
    val keyLength = 256
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val spec = PBEKeySpec(password.toCharArray(), salt, iterationCount, keyLength)
    val secretKeyBytes = factory.generateSecret(spec).encoded
    return SecretKeySpec(secretKeyBytes, "AES")
}