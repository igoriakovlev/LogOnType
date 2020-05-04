/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.plugins.logOnType

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.Disposer
import com.intellij.refactoring.copy.CopyFilesOrDirectoriesDialog
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBLabelDecorator
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.UIUtil
import javax.swing.InputVerifier
import javax.swing.JComponent
import javax.swing.event.DocumentEvent
import javax.swing.text.JTextComponent

class LogOnTypeActionDialog(
    private val project: Project, private val defaultDirectory: String
) : DialogWrapper(project, true) {

    companion object {
        const val WINDOW_TITLE = "LogOnType"
        const val LOG_FILE_WILL_BE_PLACED_HERE = "Results log files will be placed here"
        const val TIMEOUT_LABEL_TEXT = "Timeout to flush new change in ms"
    }

    private val nameLabel = JBLabelDecorator.createJBLabelDecorator().setBold(true)
    private val tfTargetDirectory = TextFieldWithBrowseButton()
    private val tfTimeout = JBTextField()

    init {
        title = WINDOW_TITLE
        init()
        initializeData()
    }

    fun JTextComponent.onTextChange(action: (DocumentEvent) -> Unit) {
        document.addDocumentListener(
                object : DocumentAdapter() {
                    override fun textChanged(e: DocumentEvent) {
                        action(e)
                    }
                }
        )
    }

    override fun createActions() = arrayOf(okAction, cancelAction)

    override fun getPreferredFocusedComponent() = tfTargetDirectory.childComponent

    override fun createCenterPanel(): JComponent? = null

    override fun createNorthPanel(): JComponent {
        val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
        tfTargetDirectory.addBrowseFolderListener(
            WINDOW_TITLE,
            LOG_FILE_WILL_BE_PLACED_HERE,
            project,
            descriptor
        )


        tfTargetDirectory.setTextFieldPreferredWidth(CopyFilesOrDirectoriesDialog.MAX_PATH_LENGTH)
        tfTargetDirectory.textField.onTextChange { validateOKButton() }
        Disposer.register(disposable, tfTargetDirectory)

        tfTimeout.inputVerifier = object : InputVerifier() {
            override fun verify(input: JComponent?) = tfTimeout.text.toIntOrNull()?.let { it > 0 } ?: false
        }
        tfTimeout.onTextChange { validateOKButton() }

        return FormBuilder.createFormBuilder()
            .addComponent(nameLabel)
            .addLabeledComponent(LOG_FILE_WILL_BE_PLACED_HERE, tfTargetDirectory, UIUtil.LARGE_VGAP)
             .addLabeledComponent(TIMEOUT_LABEL_TEXT, tfTimeout)
            .panel
    }

    private fun initializeData() {
        tfTargetDirectory.childComponent.text = defaultDirectory
        tfTimeout.text = "1000"
        validateOKButton()
    }

    private fun validateOKButton() {
        val isCorrectCount = tfTimeout.text.toIntOrNull()?.let { it > 0 } ?: false
        if (!isCorrectCount) {
            isOKActionEnabled = false
            return
        }

        val isCorrectPath = tfTargetDirectory.childComponent.text
            ?.let { it.isNotEmpty() }
            ?: false

        isOKActionEnabled = isCorrectPath
    }

    val selectedDirectoryName get() = tfTargetDirectory.childComponent.text!!
    val selectedTimeout get() = tfTimeout.text.toInt()

    override fun doOKAction() {
        close(OK_EXIT_CODE, /* isOk = */ true)
    }
}