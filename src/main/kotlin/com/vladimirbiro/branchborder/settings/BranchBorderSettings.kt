package com.vladimirbiro.branchborder.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

enum class BorderStyle {
    SOLID, DASHED
}

data class BranchRuleState(
    var pattern: String = "",
    var matchType: String = "exact",
    var color: String = "#FF0000",
    var borderStyle: BorderStyle? = null,
    var blinking: Boolean = false
)

@State(
    name = "BranchBorderSettings",
    storages = [Storage("branchBorder.xml")]
)
@Service(Service.Level.PROJECT)
class BranchBorderSettings : PersistentStateComponent<BranchBorderSettings> {

    var enabled: Boolean = true
    var borderWidth: Int = 4
    var blinkInterval: Int = 500

    var defaultColor: String = "#FF6600"
    var defaultBorderStyle: BorderStyle = BorderStyle.SOLID
    var defaultBlinking: Boolean = false
    var noBorderIfUnmatched: Boolean = false

    var branchRules: MutableList<BranchRuleState> = mutableListOf()

    override fun getState(): BranchBorderSettings = this

    override fun loadState(state: BranchBorderSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        fun getInstance(project: Project): BranchBorderSettings {
            return project.getService(BranchBorderSettings::class.java)
        }
    }
}
