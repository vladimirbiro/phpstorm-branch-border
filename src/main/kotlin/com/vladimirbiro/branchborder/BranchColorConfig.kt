package com.vladimirbiro.branchborder

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.awt.Color

data class BranchRule(
    val pattern: String,
    val match: String = "exact",
    val color: String
)

data class BranchColorConfigData(
    val borderWidth: Int = 4,
    val defaultColor: String? = null,
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
    private var branches: List<BranchRule> = emptyList()

    val configFileName = ".branch-colors.json"

    fun loadConfig(): Boolean {
        val configFile = findConfigFile() ?: return false

        return try {
            val content = String(configFile.contentsToByteArray())
            val data = gson.fromJson(content, BranchColorConfigData::class.java)

            borderWidth = data.borderWidth.coerceIn(1, 20)
            defaultColor = data.defaultColor?.let { parseColor(it) }
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

    fun getColorForBranch(branchName: String?): Color? {
        if (branchName == null) return defaultColor

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
                return parseColor(rule.color)
            }
        }

        return defaultColor
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
