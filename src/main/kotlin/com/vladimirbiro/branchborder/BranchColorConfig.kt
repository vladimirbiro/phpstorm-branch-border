package com.vladimirbiro.branchborder

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.awt.Color

data class BlinkingConfig(
    val enabled: Boolean = false,
    val interval: Int = 500
)

data class BranchRule(
    val pattern: String,
    val match: String = "exact",
    val color: String,
    val blinking: BlinkingConfig? = null
)

data class BranchColorConfigData(
    val borderWidth: Int = 4,
    val defaultColor: String? = null,
    val blinking: BlinkingConfig? = null,
    @SerializedName("branches")
    val branches: List<BranchRule> = emptyList()
)

class BranchColorConfig(private val project: Project) {

    private val log = Logger.getInstance(BranchColorConfig::class.java)
    private val gson = Gson()

    var borderWidth: Int = 4
        private set
    var defaultColor: Color? = null
        private set
    var defaultBlinking: BlinkingConfig? = null
        private set
    private var branches: List<BranchRule> = emptyList()

    val configFileName = ".branch-colors.json"

    fun loadConfig(): Boolean {
        val configFile = findConfigFile() ?: return false

        return try {
            val content = String(configFile.contentsToByteArray())
            val data = gson.fromJson(content, BranchColorConfigData::class.java)

            borderWidth = data.borderWidth.coerceIn(1, 20)
            defaultColor = data.defaultColor?.let { parseColor(it) }
            defaultBlinking = data.blinking
            branches = data.branches

            log.info("Loaded branch-colors config: ${branches.size} rules, borderWidth=$borderWidth")
            true
        } catch (e: Exception) {
            log.warn("Failed to parse $configFileName: ${e.message}")
            false
        }
    }

    fun findConfigFile(): VirtualFile? {
        return project.baseDir?.findChild(configFileName)
    }

    data class BranchSettings(
        val color: Color?,
        val blinking: BlinkingConfig?
    )

    fun getSettingsForBranch(branchName: String?): BranchSettings {
        if (branchName == null) {
            return BranchSettings(defaultColor, defaultBlinking)
        }

        for (rule in branches) {
            val matches = when (rule.match) {
                "exact" -> branchName == rule.pattern
                "prefix" -> branchName.startsWith(rule.pattern)
                "regex" -> {
                    try {
                        Regex(rule.pattern).matches(branchName)
                    } catch (e: Exception) {
                        log.warn("Invalid regex pattern: ${rule.pattern}")
                        false
                    }
                }
                else -> false
            }

            if (matches) {
                val blinking = rule.blinking ?: defaultBlinking
                return BranchSettings(parseColor(rule.color), blinking)
            }
        }

        return BranchSettings(defaultColor, defaultBlinking)
    }

    fun getColorForBranch(branchName: String?): Color? {
        return getSettingsForBranch(branchName).color
    }

    private fun parseColor(hex: String): Color? {
        return try {
            val cleanHex = hex.removePrefix("#")
            Color(cleanHex.toInt(16))
        } catch (e: Exception) {
            log.warn("Invalid color: $hex")
            null
        }
    }
}
