package com.github.donglua.obsidian.ui

import java.io.File

data class FileNode(
    val file: File,
    val level: Int,
    var isExpanded: Boolean = false
)
