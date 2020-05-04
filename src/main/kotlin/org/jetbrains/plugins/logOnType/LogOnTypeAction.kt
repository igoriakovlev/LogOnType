package org.jetbrains.plugins.logOnType

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class LogOnTypeAction : AnAction() {

    fun Project.guessProjectDir() : VirtualFile? {
        if (isDefault) {
            return null
        }

        val modules = ModuleManager.getInstance(this).modules
        val module = if (modules.size == 1) modules.first() else modules.firstOrNull { it.name == this.name }
        module?.guessModuleDir()?.let { return it }
        return LocalFileSystem.getInstance().findFileByPath(basePath!!)
    }

    fun Module.guessModuleDir(): VirtualFile? {
        val contentRoots = rootManager.contentRoots.filter { it.isDirectory }
        return contentRoots.find { it.name == name } ?: contentRoots.firstOrNull()
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project

        if (!LogOnTypeHandler.isEnabled) {
            val projectRoot = project?.guessProjectDir()

            if (projectRoot === null) {
                Messages.showErrorDialog(project, "Cannot get project root directory", "Error")
                return
            }

            val stamp = DateTimeFormatter
                    .ofPattern("yyyy-MM-dd-HH-mm-ss")
                    .withZone(ZoneOffset.UTC)
                    .format(Instant.now())
                    .toString()

            val pathWithStamp = File(projectRoot.parent.path, "SESSION-$stamp")

            val dialog = LogOnTypeActionDialog(project, pathWithStamp.path)
            dialog.show()
            if (!dialog.isOK) return

            val timeout = dialog.selectedTimeout
            if (timeout < 0) {
                Messages.showErrorDialog(project, "Selected invalid timeout $timeout, BUT must be > 0", "Error")
                return

            }

            val targetPath = File(dialog.selectedDirectoryName)

            if (targetPath.startsWith(projectRoot.path)) {
                Messages.showErrorDialog(project, "Do not use project path for report directory", "Error")
                return
            }

            if (targetPath.exists()) {
                if (!targetPath.isDirectory) {
                    Messages.showErrorDialog(project, "Cannot apply file path. Directory expected", "Error")
                    return
                }
            } else {
                if (!targetPath.mkdir()) {
                    Messages.showErrorDialog(project, "Could not create specified directory", "Error")
                    return
                }
            }

            LogOnTypeHandler.enable(targetPath, timeout)
            e.presentation.text = "LogOnType [Disable]"

            Messages.showInfoMessage(project, "LogOnType Enabled", "Enabled")

        } else {
            LogOnTypeHandler.disable()
            e.presentation.text = "LogOnType [Enable]"
            Messages.showInfoMessage(project, "LogOnType Disabled", "Disabled")
        }
    }
}