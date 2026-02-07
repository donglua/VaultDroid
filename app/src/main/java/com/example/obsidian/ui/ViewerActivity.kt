package com.example.obsidian.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.obsidian.data.FileRepository
import com.example.obsidian.databinding.ActivityViewerBinding
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin

class ViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityViewerBinding
    private lateinit var repo: FileRepository
    private lateinit var adapter: MarkdownAdapter
    private lateinit var markwon: Markwon
    private var path: String = ""
    private var lines: MutableList<MarkdownLine> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        path = intent.getStringExtra("path") ?: ""
        if (path.isEmpty()) {
            finish()
            return
        }
        title = path.substringAfterLast("/")

        repo = FileRepository(this)

        // Configure Markwon with plugins
        markwon = Markwon.builder(this)
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(this))
            .build()

        adapter = MarkdownAdapter(emptyList(), markwon) { position, isChecked ->
            onTaskToggle(position, isChecked)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        loadFile()
    }

    private fun loadFile() {
        val content = repo.getFileContent(path)
        lines = parseLines(content).toMutableList()
        adapter.updateLines(lines)
    }

    private fun parseLines(text: String): List<MarkdownLine> {
        return text.split("\n").map { line ->
            val trimmed = line.trimStart()
            val isUnchecked = trimmed.startsWith("- [ ]")
            val isChecked = trimmed.startsWith("- [x]")
            val isTask = isUnchecked || isChecked

            val content = if (isTask) {
                val bracketIndex = line.indexOf(']')
                val rawContent = if (bracketIndex != -1 && bracketIndex + 2 <= line.length) {
                    line.substring(bracketIndex + 2)
                } else {
                    line
                }
                // visual feedback for completed task
                if (isChecked) "~~$rawContent~~" else rawContent
            } else {
                line
            }

            MarkdownLine(line, isTask, isChecked, content)
        }
    }

    private fun onTaskToggle(position: Int, isChecked: Boolean) {
        val line = lines[position]
        val newText = if (isChecked) {
            line.originalText.replaceFirst("- [ ]", "- [x]")
        } else {
            line.originalText.replaceFirst("- [x]", "- [ ]")
        }

        // Update content visual (toggle strikethrough)
        val rawContent = if (line.content.startsWith("~~") && line.content.endsWith("~~")) {
             line.content.substring(2, line.content.length - 2)
        } else {
             line.content
        }

        val newContent = if (isChecked) "~~$rawContent~~" else rawContent

        val newLine = line.copy(isChecked = isChecked, originalText = newText, content = newContent)
        lines[position] = newLine
        adapter.notifyItemChanged(position)

        // Save to file
        val fileContent = lines.joinToString("\n") { it.originalText }
        repo.saveLocalFile(path, fileContent)
    }
}
