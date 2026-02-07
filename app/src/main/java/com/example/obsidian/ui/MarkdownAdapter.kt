package com.example.obsidian.ui

import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.noties.markwon.Markwon

data class MarkdownLine(
    val originalText: String,
    val isTask: Boolean,
    val isChecked: Boolean,
    val content: String
)

class MarkdownAdapter(
    private var lines: List<MarkdownLine>,
    private val markwon: Markwon,
    private val onTaskToggle: (Int, Boolean) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_TEXT = 0
        private const val TYPE_TASK = 1
    }

    fun updateLines(newLines: List<MarkdownLine>) {
        lines = newLines
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (lines[position].isTask) TYPE_TASK else TYPE_TEXT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_TASK) {
            TaskViewHolder(inflater.inflate(com.example.obsidian.R.layout.item_markdown_task, parent, false))
        } else {
            TextViewHolder(inflater.inflate(com.example.obsidian.R.layout.item_markdown_text, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val line = lines[position]
        if (holder is TaskViewHolder) {
            holder.bind(line, markwon, onTaskToggle)
        } else if (holder is TextViewHolder) {
            holder.bind(line, markwon)
        }
    }

    override fun getItemCount() = lines.size

    class TextViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(com.example.obsidian.R.id.text_content)
        fun bind(line: MarkdownLine, markwon: Markwon) {
            markwon.setMarkdown(textView, line.content)
        }
    }

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkBox: CheckBox = itemView.findViewById(com.example.obsidian.R.id.checkbox)
        private val textView: TextView = itemView.findViewById(com.example.obsidian.R.id.text_content)

        fun bind(line: MarkdownLine, markwon: Markwon, onTaskToggle: (Int, Boolean) -> Unit) {
            // Remove listener to avoid loops during binding
            checkBox.setOnCheckedChangeListener(null)

            checkBox.isChecked = line.isChecked
            markwon.setMarkdown(textView, line.content)

            checkBox.setOnCheckedChangeListener { _, isChecked ->
                onTaskToggle(adapterPosition, isChecked)
            }
        }
    }
}
