package com.github.donglua.obsidian.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.donglua.obsidian.data.FileRepository
import io.noties.markwon.LinkResolver
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin

class ViewerActivity : AppCompatActivity() {

    private lateinit var textView: TextView
    private lateinit var repo: FileRepository
    private lateinit var markwon: Markwon
    private var path: String = ""
    private var originalContent: String = ""

    // Regex to match task markers: "- [ ]" or "- [x]" or "- [X]"
    private val taskRegex = Regex("(-\\s\\[([ xX])\\])")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.github.donglua.obsidian.R.layout.activity_viewer)

        path = intent.getStringExtra("path") ?: ""
        if (path.isEmpty()) {
            finish()
            return
        }
        title = path.substringAfterLast("/")

        textView = findViewById(com.github.donglua.obsidian.R.id.markdown_view)
        textView.movementMethod = LinkMovementMethod.getInstance()

        repo = FileRepository(this)

        // Configure Markwon
        markwon = Markwon.builder(this)
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(this))
            .usePlugin(object : AbstractMarkwonPlugin() {
                override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                    builder.linkResolver(object : LinkResolver {
                        override fun resolve(view: View, link: String) {
                            if (link.startsWith("cmd:toggle:")) {
                                val offsetStr = link.substringAfter("cmd:toggle:")
                                val offset = offsetStr.toIntOrNull()
                                if (offset != null) {
                                    toggleTask(offset)
                                }
                            } else {
                                // Default link handling
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                                    view.context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(view.context, "Cannot open link", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    })
                }
            })
            .build()

        loadContent()
    }

    private fun loadContent() {
        originalContent = repo.getFileContent(path)
        renderContent()
    }

    private fun renderContent() {
        val processedContent = processMarkdown(originalContent)
        markwon.setMarkdown(textView, processedContent)
    }

    private fun processMarkdown(text: String): String {
        val sb = StringBuilder()
        var lastIndex = 0

        taskRegex.findAll(text).forEach { match ->
            sb.append(text.substring(lastIndex, match.range.first))

            val isChecked = match.value.contains("x", ignoreCase = true)
            val displayChar = if (isChecked) "☑" else "☐"

            val relativeOffset = match.value.indexOf('[')
            val absoluteOffset = match.range.first + relativeOffset

            // Generate link. Note that we replace the whole match "- [ ]" with "- [link](...)"
            // Markwon renders this as a list item starting with a link.
            sb.append("- [$displayChar](cmd:toggle:$absoluteOffset)")

            lastIndex = match.range.last + 1
        }
        sb.append(text.substring(lastIndex))

        return sb.toString()
    }

    private fun toggleTask(offset: Int) {
        if (offset < 0 || offset + 1 >= originalContent.length) return

        // offset points to '['. offset+1 is space or x
        val charAt = originalContent[offset + 1]

        // Toggle logic
        val newChar = if (charAt == ' ') 'x' else ' '

        val sb = StringBuilder(originalContent)
        sb.setCharAt(offset + 1, newChar)
        originalContent = sb.toString()

        repo.saveLocalFile(path, originalContent)
        renderContent()
    }
}
