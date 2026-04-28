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
import androidx.core.graphics.createBitmap
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.pm.encrypter.R
import com.pm.encrypter.utils.formatDate
import com.pm.encrypter.utils.formatSize
import com.pm.encrypter.model.FileItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SearchAdapter(
    private val context: Context,
    private var list: List<FileItem>,
    private val onClick: (FileItem) -> Unit
) : RecyclerView.Adapter<SearchAdapter.FileViewHolder>() {

    inner class FileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val fileName: TextView = view.findViewById(R.id.tvTitle)
        private val metaText: TextView = view.findViewById(R.id.tvMetadata)
        private val icon: ImageView = view.findViewById(R.id.ivIcon)

        fun bind(file: FileItem) {
            fileName.text = file.name
            val meta = "${formatSize(file.size)} • ${formatDate(file.date)}"
            metaText.text = meta

            val name = file.name.lowercase()

            when {
                name.endsWith(".jpg") || name.endsWith(".png") -> {
                    Glide.with(itemView.context)
                        .load(file.uri)
                        .centerCrop()
                        .placeholder(R.drawable.ic_docs_24px)
                        .into(icon)
                }

                name.endsWith(".mp4") -> {
                    Glide.with(itemView.context)
                        .asBitmap()
                        .load(file.uri)
                        .centerCrop()
                        .placeholder(R.drawable.ic_docs_24px)
                        .into(icon)
                }

                name.endsWith(".pdf") -> {
                    icon.setImageResource(R.drawable.ic_docs_24px)

                    // load thumbnail in background
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val fileDescriptor = itemView.context.contentResolver
                                .openFileDescriptor(file.uri, "r")

                            val renderer = PdfRenderer(fileDescriptor!!)
                            val page = renderer.openPage(0)

                            val bitmap = createBitmap(page.width, page.height)

                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                            page.close()
                            renderer.close()

                            withContext(Dispatchers.Main) {
                                icon.setImageBitmap(bitmap)
                            }

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                name.endsWith(".enc") -> {
                    icon.setImageResource(R.drawable.enc)
                }

                else -> {
                    icon.setImageResource(R.drawable.ic_docs_24px)
                }
            }
            itemView.setOnClickListener { onClick(file) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_encrypted_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount() = list.size

    fun update(newList: List<FileItem>) {
        list = newList
        notifyDataSetChanged()
    }
}