package org.jetbrains.plugins.logOnType

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import java.io.File
import java.util.*

class LogOnTypeHandler : TypedHandlerDelegate() {

        companion object {
            var lastTypeTimeStamp = Date().time
            val lastTextHashes = mutableMapOf<Int, Int>()

            fun enable(directory: File, timeout: Int) {
                if (isEnabled) return
                lastTypeTimeStamp = Date().time
                this.directory = directory
                this.timeout = timeout
                isEnabled = true
            }
            fun disable() {
                isEnabled = false
                lastTextHashes.clear()
            }

            var isEnabled: Boolean = false
                private set
            var directory: File = File("")
                private set
            var timeout: Int = 1000
                private set
        }

        private fun handleChange(file: PsiFile) {
            if (!isEnabled) return

            val currentTimeStamp = Date().time
            if (currentTimeStamp - lastTypeTimeStamp < timeout) {
                lastTypeTimeStamp = currentTimeStamp
                return
            }

            val fileType = file.fileType
            if (file.isPhysical && !fileType.isReadOnly && !fileType.isBinary && file.isValid){
                if (file.javaClass.name == "org.jetbrains.kotlin.psi.KtFile") { //Just avoid kotlin plugin reference here
                    val fileName = "${file.name}-$lastTypeTimeStamp.kt"
                    val fileNameHash = fileName.hashCode()
                    val lastTextHash = lastTextHashes[fileNameHash]
                    val newText = file.text
                    val newTextHash = newText.hashCode()
                    if (lastTextHash != newTextHash) {
                        File(directory, fileName).writeText(newText)
                        lastTextHashes[fileNameHash] = newTextHash
                    }
                }
            }

            lastTypeTimeStamp = currentTimeStamp
        }

        override fun beforeCharTyped(c: Char, project: Project, editor: Editor, file: PsiFile, fileType: FileType): Result {
            handleChange(file)
            return super.beforeCharTyped(c, project, editor, file, fileType)
        }
}