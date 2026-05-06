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
        val saltLength = encryptedFileStream.read()
        val salt = ByteArray(saltLength)
        readFully(encryptedFileStream, salt)

        val ivLength = encryptedFileStream.read()
        val iv = ByteArray(ivLength)
        readFully(encryptedFileStream, iv)

        val key = deriveKeyFromPassword(password, salt)

        // CHANGE: Use CBC and IvParameterSpec
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, key, javax.crypto.spec.IvParameterSpec(iv))

        CipherInputStream(encryptedFileStream, cipher).use { cipherIn ->
            decryptedFileOutputStream.use { output ->
                val buffer = ByteArray(64 * 1024)
                var bytes = cipherIn.read(buffer)
                while (bytes != -1) {
                    output.write(buffer, 0, bytes)
                    bytes = cipherIn.read(buffer)
                }
            }
        }
    }
}

fun readFully(input: InputStream, buffer: ByteArray) {
    var offset = 0
    while (offset < buffer.size) {
        val read = input.read(buffer, offset, buffer.size - offset)
        if (read == -1) throw Exception("Unexpected EOF")
        offset += read
    }
}