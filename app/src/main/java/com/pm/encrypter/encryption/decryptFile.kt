package com.pm.encrypter.encryption

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.spec.GCMParameterSpec

suspend fun decryptFileWithPassword(
    encryptedFileStream: InputStream,
    decryptedFileOutputStream: OutputStream,
    password: String
) {
    withContext(Dispatchers.IO) {
        // 1. Read Salt
        val saltLength = encryptedFileStream.read()
        val salt = ByteArray(saltLength)
        encryptedFileStream.read(salt)

        // 2. Read IV
        val ivLength = encryptedFileStream.read()
        val iv = ByteArray(ivLength)
        encryptedFileStream.read(iv)

        // 3. Derive the exact same key using the password and the read Salt
        val key = deriveKeyFromPassword(password, salt)

        // 4. Initialize Cipher
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)

        // 5. Decrypt the file data
        CipherInputStream(encryptedFileStream, cipher).use { cipherIn ->
            decryptedFileOutputStream.use { output ->
                cipherIn.copyTo(output, bufferSize = 8192)
            }
        }
    }
}