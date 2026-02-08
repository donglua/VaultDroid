package com.github.donglua.obsidian.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.donglua.obsidian.R
import java.io.File

class TreeFileAdapter(
    private var nodes: List<FileNode> = emptyList(),
    private val onClick: (FileNode) -> Unit,
    private val onLongClick: (View, FileNode) -> Unit
) : RecyclerView.Adapter<TreeFileAdapter.NodeViewHolder>() {

    fun updateNodes(newNodes: List<FileNode>) {
        nodes = newNodes
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NodeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_file_node, parent, false)
        return NodeViewHolder(view)
    }

    override fun onBindViewHolder(holder: NodeViewHolder, position: Int) {
        val node = nodes[position]
        holder.bind(node, onClick, onLongClick)
    }

    override fun getItemCount() = nodes.size

    class NodeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val expandIcon: ImageView = itemView.findViewById(R.id.iv_expand)
        private val typeIcon: ImageView = itemView.findViewById(R.id.iv_icon)
        private val nameView: TextView = itemView.findViewById(R.id.tv_name)
        private val container: View = itemView.findViewById(R.id.node_container)

        fun bind(node: FileNode, onClick: (FileNode) -> Unit, onLongClick: (View, FileNode) -> Unit) {
            nameView.text = node.file.name

            // Indentation (16dp base + 16dp per level)
            // dp to px conversion
            val density = itemView.context.resources.displayMetrics.density
            val indent = ((node.level * 24 + 16) * density).toInt()

            // Preserve other paddings
            container.setPaddingRelative(
                indent,
                container.paddingTop,
                container.paddingEnd,
                container.paddingBottom
            )

            if (node.file.isDirectory) {
                expandIcon.visibility = View.VISIBLE
                if (node.isExpanded) {
                    expandIcon.rotation = 0f
                } else {
                    expandIcon.rotation = -90f
                }
                typeIcon.setImageResource(android.R.drawable.ic_menu_agenda)
                typeIcon.alpha = 0.7f
            } else {
                expandIcon.visibility = View.INVISIBLE
                typeIcon.setImageResource(android.R.drawable.ic_menu_edit)
                typeIcon.alpha = 1.0f
            }

            itemView.setOnClickListener { onClick(node) }
            itemView.setOnLongClickListener {
                onLongClick(itemView, node)
                true
            }
        }
    }
}
