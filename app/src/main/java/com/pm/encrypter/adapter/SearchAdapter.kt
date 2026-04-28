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
                    icon.setImageResource(R.drawable.ic_visibility_lock_24px)
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