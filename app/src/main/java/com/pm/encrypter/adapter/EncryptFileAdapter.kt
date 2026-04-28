package com.pm.encrypter.adapter

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.media.ThumbnailUtils
import android.os.Build
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.pm.encrypter.R
import com.pm.encrypter.utils.formatDate
import com.pm.encrypter.utils.formatSize
import com.pm.encrypter.model.FileItem
import java.io.File
import androidx.core.graphics.createBitmap
import com.bumptech.glide.Glide

class EncryptFileAdapter(
    private var files: List<FileItem>,
    private val context: Context,
    private val onChipClick: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_CHIPS = 0
        const val TYPE_FILE = 1
    }

    private var fullListUri = files.toList()

    // 🔹 FILE VIEW HOLDER
    inner class FileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val fileName: TextView = view.findViewById(R.id.tvTitle)
        private val metaText: TextView = view.findViewById(R.id.tvMetadata)
        private val icon: ImageView = view.findViewById(R.id.ivIcon)


        fun bind(file: FileItem) {
            fileName.text = file.name
            val meta = "${formatSize(file.size)} • ${formatDate(file.date)}"
            metaText.text = meta

            val name = file.name.lowercase()

            when {
                name.endsWith(".jpg") || file.name.endsWith(".png") -> {
                    Glide.with(itemView.context)
                        .load(file.uri)
                        .into(icon)
                }

                name.endsWith(".mp4") -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val bitmap = ThumbnailUtils.createVideoThumbnail(
                            File(file.uri.path.toString()),
                            Size(200, 200),
                            null
                        )
                        icon.setImageBitmap(bitmap)
                    }else{
                        icon.setImageResource(android.R.drawable.btn_minus)
                    }
                }

                name.endsWith(".pdf") -> {
                    val fileDescriptor = context.contentResolver.openFileDescriptor(file.uri, "r")
                    val renderer = PdfRenderer(fileDescriptor!!)
                    val page = renderer.openPage(0)

                    val bitmap = createBitmap(page.width, page.height)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    icon.setImageBitmap(bitmap)

                    page.close()
                    renderer.close()
                }
                name.endsWith(".enc") -> {
                        icon.setImageResource(R.drawable.enc)
                }

                    else -> {
                    icon.setImageResource(R.drawable.ic_docs_24px)
                }
            }
        }
    }

    // 🔹 CHIPS VIEW HOLDER
    inner class ChipsViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val chipAll = view.findViewById<Chip>(R.id.chipAll)
        private val chipEncrypted = view.findViewById<Chip>(R.id.chipEncrypted)
        private val chipDecrypted = view.findViewById<Chip>(R.id.chipDecrypted)
        private val chipPrivate = view.findViewById<Chip>(R.id.chipPrivate)

        fun bind() {

            chipAll.setOnClickListener {
                onChipClick("all")
            }
            chipEncrypted.setOnClickListener {
                onChipClick("encrypted")
            }
            chipDecrypted.setOnClickListener {
                onChipClick("decrypted")
            }
            chipPrivate.setOnClickListener {
                onChipClick("private")
            }

        }
    }

    // 🔹 UPDATE LIST (SEARCH)
    fun updateList(newList: List<FileItem>) {
        files = newList
        fullListUri = newList
        notifyDataSetChanged()
    }

    // 🔹 VIEW TYPE
    override fun getItemViewType(position: Int): Int {
        return if (position == 0) TYPE_CHIPS else TYPE_FILE
    }

    // 🔹 CREATE VIEW HOLDER
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return if (viewType == TYPE_CHIPS) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chips, parent, false)
            ChipsViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_encrypted_file, parent, false)
            FileViewHolder(view)
        }
    }

    // 🔹 BIND VIEW HOLDER
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        when (holder) {
            is FileViewHolder -> {
                val uri = files[position - 1] // offset because of chips
                holder.bind(uri)
            }

            is ChipsViewHolder -> {
                holder.bind()
            }
        }
    }

    // 🔹 ITEM COUNT
    override fun getItemCount(): Int {
        return files.size + 1
    }
}