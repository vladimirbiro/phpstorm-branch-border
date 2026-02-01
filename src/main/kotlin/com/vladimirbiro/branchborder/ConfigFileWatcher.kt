package com.vladimirbiro.branchborder

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent

class ConfigFileWatcher(
    private val project: Project,
    private val configFileName: String,
    private val onConfigChanged: () -> Unit
) : BulkFileListener, Disposable {

    private val log = Logger.getInstance(ConfigFileWatcher::class.java)
    private val connection = project.messageBus.connect(this)

    fun start() {
        connection.subscribe(VirtualFileManager.VFS_CHANGES, this)
        log.info("ConfigFileWatcher started for $configFileName")
    }

    override fun after(events: List<VFileEvent>) {
        val projectBasePath = project.basePath ?: return

        for (event in events) {
            val file = event.file ?: continue

            // Check if this is our config file in project root
            if (file.name == configFileName && file.parent?.path == projectBasePath) {
                when (event) {
                    is VFileContentChangeEvent,
                    is VFileCreateEvent,
                    is VFileDeleteEvent -> {
                        log.info("Config file changed: ${event.javaClass.simpleName}")
                        onConfigChanged()
                        return
                    }
                }
            }
        }
    }

    override fun dispose() {
        // Connection is auto-disposed via parent disposable
    }
}
