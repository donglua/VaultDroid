package com.github.donglua.obsidian.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class FileAdapter(
    var files: List<File> = emptyList(),
    private val onClick: (File) -> Unit,
    private val onLongClick: (File) -> Unit
) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    fun updateFiles(newFiles: List<File>) {
        files = newFiles
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(com.github.donglua.obsidian.R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val file = files[position]
        holder.bind(file, onClick, onLongClick)
    }

    override fun getItemCount() = files.size

    class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameView: TextView = itemView.findViewById(com.github.donglua.obsidian.R.id.name)
        private val iconView: ImageView = itemView.findViewById(com.github.donglua.obsidian.R.id.icon)

        fun bind(file: File, onClick: (File) -> Unit, onLongClick: (File) -> Unit) {
            nameView.text = file.name

            if (file.isDirectory) {
                iconView.setImageResource(android.R.drawable.ic_menu_agenda) // Generic folder icon
                iconView.alpha = 0.7f
            } else {
                iconView.setImageResource(android.R.drawable.ic_menu_edit) // File icon
                iconView.alpha = 1.0f
            }

            itemView.setOnClickListener { onClick(file) }
            itemView.setOnLongClickListener {
                onLongClick(file)
                true
            }
        }
    }
}
