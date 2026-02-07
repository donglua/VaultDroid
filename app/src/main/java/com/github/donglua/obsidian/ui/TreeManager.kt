package com.github.donglua.obsidian.ui

import java.io.File

object TreeManager {

    val expandedPaths: MutableSet<String> = mutableSetOf()

    fun toggle(file: File) {
        val path = file.absolutePath
        if (expandedPaths.contains(path)) {
            expandedPaths.remove(path)
        } else {
            expandedPaths.add(path)
        }
    }

    fun buildTree(root: File): List<FileNode> {
        val rootFiles = root.listFiles()?.sortedBy { !it.isDirectory }?.toList() ?: emptyList()
        return buildListRecursively(rootFiles, 0)
    }

    private fun buildListRecursively(files: List<File>, level: Int): List<FileNode> {
        val list = mutableListOf<FileNode>()
        for (file in files) {
            val isExpanded = expandedPaths.contains(file.absolutePath)

            // We use the expanded state from our set, but only if it's a directory
            val expandState = if (file.isDirectory) isExpanded else false

            list.add(FileNode(file, level, expandState))

            if (file.isDirectory && isExpanded) {
                 val children = file.listFiles()?.sortedBy { !it.isDirectory }?.toList() ?: emptyList()
                 list.addAll(buildListRecursively(children, level + 1))
            }
        }
        return list
    }
}
