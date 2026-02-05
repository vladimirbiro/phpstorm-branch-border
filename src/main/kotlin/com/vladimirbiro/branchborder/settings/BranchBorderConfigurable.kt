package com.vladimirbiro.branchborder.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.vladimirbiro.branchborder.BranchBorderService
import javax.swing.JComponent

class BranchBorderConfigurable(private val project: Project) : Configurable {

    private var settingsPanel: BranchBorderSettingsPanel? = null

    override fun getDisplayName(): String = "Git Branch Border"

    override fun createComponent(): JComponent {
        settingsPanel = BranchBorderSettingsPanel(project)
        return settingsPanel!!.panel
    }

    override fun isModified(): Boolean {
        return settingsPanel?.isModified() ?: false
    }

    override fun apply() {
        settingsPanel?.applySettings()
        BranchBorderService.getInstance(project).onSettingsChanged()
    }

    override fun reset() {
        settingsPanel?.loadSettings()
    }

    override fun disposeUIResources() {
        settingsPanel = null
    }
}
