package com.pm.encrypter.ui

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButtonGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pm.encrypter.R
import com.pm.encrypter.encryption.decryptFileWithPassword
import com.pm.encrypter.encryption.encryptFileWithPassword
import com.pm.encrypter.utils.ProgressInputStream
import com.pm.encrypter.utils.deleteFile
import com.pm.encrypter.utils.getFileName
import com.pm.encrypter.utils.openFolderModern
import com.pm.encrypter.utils.shareFile
import com.pm.encrypter.utils.splitFileName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class Progress : AppCompatActivity() {

    private lateinit var title: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var btnShare: Button
    private lateinit var btnDelete: Button
    private lateinit var btnGroup: MaterialButtonGroup
    private lateinit var btnView: Button
    private lateinit var storageInfo: TextView
    private lateinit var progressText: TextView
    private lateinit var progressBar: ProgressBar
    private var processFileUri: Uri? = null
    private lateinit var folderType: String


    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progress)

        val task = intent.getStringExtra("TASK")
        val password = intent.getStringExtra("PASSWORD")
        val fileUri = intent.getParcelableExtra<Uri>("URI")

        title = findViewById(R.id.tvStatusTitle)
        btnBack = findViewById(R.id.btnBack)
        btnShare = findViewById(R.id.btnShare)
        btnDelete = findViewById(R.id.btnDelete)
        btnView = findViewById(R.id.btnOpenFolder)
        storageInfo = findViewById(R.id.tvStorageInfo)
        btnGroup = findViewById(R.id.buttonGroup)
        progressText = findViewById(R.id.tvPercentage)
        progressBar = findViewById(R.id.progressBar)


        btnBack.setOnClickListener {
            finish()
        }

        btnShare.setOnClickListener {
            processFileUri?.let { shareFile(it, this) }
        }

        btnDelete.setOnClickListener {

            showDeleteFileDialog { confirmed ->

                if (confirmed) {
                    processFileUri?.let { deleteFile(it, this) }
                    finish()
                }
            }
        }
        btnView.setOnClickListener {
            openFolderModern(this, folderType)
        }

        if (task.equals("ENCRYPT")) {

            // "video/*" ensures they only pick video files
            encryptAndSaveToDownloads(fileUri!!, password!!)
        } else {

            title.text = "Decrypting..."
            // "*/*" allows picking our custom .enc files
            decryptAndSaveToDownloads(fileUri!!, password!!)
        }
    }


    // --- ENCRYPTION LOGIC ---
    private fun encryptAndSaveToDownloads(inputUri: Uri, password: String) {
        lifecycleScope.launch {

            val fileSize = try {
                contentResolver.openFileDescriptor(inputUri, "r")?.statSize ?: -1L
            } catch (e: Exception) {
                -1L
            }

            progressBar.visibility = View.VISIBLE
            progressText.visibility = View.VISIBLE
            progressBar.progress = 0
            progressText.text = "0%"

            withContext(Dispatchers.IO) {
                try {
                    val originalName = getFileName(inputUri, this@Progress as Context)
                    val (name, ext) = splitFileName(originalName)
                    // 👉 photo.jpg → photo.enc
                    val fileName = "$name-locked_${System.currentTimeMillis()}.enc"

                    contentResolver.openInputStream(inputUri)?.use { inputStream ->

                        // ✅ wrap input stream (THIS is the key)
                        // switch to UI thread safely
                        var lastProgress = -1
                        var lastUpdateTime = 0L

                        val progressStream =
                            ProgressInputStream(inputStream, fileSize) { progress ->

                                val now = System.currentTimeMillis()

                                if (progress != lastProgress && now - lastUpdateTime > 200) {
                                    lastProgress = progress
                                    lastUpdateTime = now

                                    runOnUiThread {
                                        progressBar.progress = progress
                                        progressText.text = "$progress%"
                                    }
                                }
                            }


                        getOutputStreamForDownloads(
                            fileName, "application/octet-stream", "Encrypted"
                        )?.use { outputStream ->

                            DataOutputStream(outputStream).use { dataOut ->

                                val mimeType =
                                    contentResolver.getType(inputUri) ?: "application/octet-stream"

                                dataOut.writeUTF(ext)
                                dataOut.writeUTF(mimeType)

                                encryptFileWithPassword(
                                    progressStream,
                                    dataOut,
                                    password
                                )
                            }
                        }
                    }

                    withContext(Dispatchers.Main) {
                        progressBar.progress = 100
                        progressText.text = "100%"
                        btnGroup.isEnabled = true
                        storageInfo.visibility = View.VISIBLE
                        Toast.makeText(this@Progress, "Encryption Done!", Toast.LENGTH_SHORT).show()
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@Progress, "Encryption failed!", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }
    }


    // --- DECRYPTION LOGIC ---
    private fun decryptAndSaveToDownloads(inputUri: Uri, password: String) {
        lifecycleScope.launch {

            val fileSize = try {
                contentResolver.openFileDescriptor(inputUri, "r")?.statSize ?: -1L
            } catch (e: Exception) {
                -1L
            }

            progressBar.visibility = View.VISIBLE
            progressText.visibility = View.VISIBLE
            progressBar.progress = 0
            progressText.text = "0%"

            withContext(Dispatchers.IO) {
                try {

                    contentResolver.openInputStream(inputUri)?.use { inputStream ->

                        // ✅ Wrap input stream for progress
                        var lastProgress = -1
                        var lastUpdateTime = 0L

                        val progressStream =
                            ProgressInputStream(inputStream, fileSize) { progress ->

                                val now = System.currentTimeMillis()

                                if (progress != lastProgress && now - lastUpdateTime > 200) {
                                    lastProgress = progress
                                    lastUpdateTime = now

                                    runOnUiThread {
                                        progressBar.progress = progress
                                        progressText.text = "$progress%"
                                    }
                                }
                            }

                        DataInputStream(progressStream).use { dataIn ->

                            val ext = dataIn.readUTF()
                            val mimeType = dataIn.readUTF()

                            val encryptedName =
                                getFileName(inputUri, this@Progress)
                            val baseName = encryptedName.removeSuffix(".enc")

                            val finalName = if (ext.isNotEmpty()) {
                                "$baseName.$ext"
                            } else {
                                baseName
                            }

                            getOutputStreamForDownloads(
                                finalName, mimeType, "Decrypted"
                            )?.use { outputStream ->

                                decryptFileWithPassword(
                                    dataIn,
                                    outputStream,
                                    password
                                )
                            }
                        }
                    }

                    withContext(Dispatchers.Main) {
                        progressBar.progress = 100
                        progressText.text = "100%"
                        btnGroup.isEnabled = true
                        storageInfo.visibility = View.VISIBLE
                        Toast.makeText(this@Progress, "Decryption Done!", Toast.LENGTH_SHORT).show()
                    }

                } catch (e: Exception) {
                    e.printStackTrace()

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@Progress,
                            "Decryption failed. Wrong password or corrupted file.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    // --- HELPER FUNCTIONS ---
    // Safely creates a file in the public Downloads folder across all Android versions
    private fun getOutputStreamForDownloads(
        fileName: String, mimeType: String, folderType: String
    ): OutputStream? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + "/Encrypter/$folderType File"
                )
                this@Progress.folderType = "$folderType File"
            }
            val uri = this.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues
            )
            processFileUri = uri
            uri?.let { this.contentResolver.openOutputStream(it) }
        } else {
            @Suppress("DEPRECATION") val downloadsDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val folder = File(downloadsDir, "Encrypter/$folderType")
            if (!folder.exists()) folder.mkdirs()

            val outputFile = File(folder, fileName)
            processFileUri = Uri.fromFile(outputFile)
            FileOutputStream(outputFile)
        }
    }


    /* This can be used to open file or
       may be open encrypted file decrypt
       in private folder and open from there */
    fun openLastFile() {
        processFileUri?.let { uri ->
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, contentResolver.getType(uri))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "No app found to open file", Toast.LENGTH_SHORT).show()
            }
        }
    }


    fun showDeleteFileDialog(onResult: (Boolean) -> Unit) {

        val dialog = MaterialAlertDialogBuilder(this).setTitle("Delete This File")
            .setMessage("Are you sure you want to delete this file?").setCancelable(false)

            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                onResult(false)
            }

            .setPositiveButton("Delete") { dialog, _ ->
                dialog.dismiss()
                onResult(true)
            }

            .create()

        dialog.show()
    }
}
