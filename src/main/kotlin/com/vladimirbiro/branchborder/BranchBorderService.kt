package com.vladimirbiro.branchborder

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.IdeGlassPaneUtil
import com.intellij.openapi.wm.WindowManager
import com.vladimirbiro.branchborder.settings.BranchBorderSettings
import com.vladimirbiro.branchborder.settings.BorderStyle
import java.awt.Color
import javax.swing.JRootPane

@Service(Service.Level.PROJECT)
class BranchBorderService(private val project: Project) : Disposable {

    private val log = Logger.getInstance(BranchBorderService::class.java)

    private var painter: BorderPainter? = null
    private var gitListener: GitBranchListener? = null
    private var currentBranch: String? = null
    private var rootPane: JRootPane? = null

    private val settings: BranchBorderSettings
        get() = BranchBorderSettings.getInstance(project)

    fun initialize() {
        log.info("Initializing BranchBorderService for project: ${project.name}")

        setupPainter()

        gitListener = GitBranchListener(project) { branch ->
            currentBranch = branch
            updateBorder()
        }
        Disposer.register(this, gitListener!!)
        gitListener!!.start()

        log.info("BranchBorderService initialized")
    }

    private fun setupPainter() {
        val frame = WindowManager.getInstance().getFrame(project) ?: run {
            log.warn("Cannot get project frame")
            return
        }

        rootPane = frame.rootPane ?: run {
            log.warn("Cannot get root pane")
            return
        }

        painter = BorderPainter()
        painter!!.setRepaintCallback { rootPane?.repaint() }

        try {
            IdeGlassPaneUtil.installPainter(rootPane!!, painter!!, this)
            log.info("Border painter installed")
        } catch (e: Exception) {
            log.error("Failed to install painter", e)
        }
    }

    private fun updateBorder() {
        val p = painter ?: return

        if (!settings.enabled) {
            p.updateBorder(null, settings.borderWidth)
            rootPane?.repaint()
            return
        }

        val branchSettings = getSettingsForBranch(currentBranch)

        p.updateBorder(
            color = branchSettings.color,
            width = settings.borderWidth,
            style = branchSettings.style,
            blinking = branchSettings.blinking,
            interval = settings.blinkInterval
        )

        rootPane?.repaint()

        log.info("Border updated: branch=$currentBranch, color=${branchSettings.color}, style=${branchSettings.style}, blinking=${branchSettings.blinking}")
    }

    private data class BranchSettings(
        val color: Color?,
        val style: BorderStyle,
        val blinking: Boolean
    )

    private fun getSettingsForBranch(branchName: String?): BranchSettings {
        if (branchName == null) {
            return getDefaultSettings()
        }

        for (rule in settings.branchRules) {
            val matches = when (rule.matchType) {
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
                return BranchSettings(
                    color = parseColor(rule.color),
                    style = rule.borderStyle ?: settings.defaultBorderStyle,
                    blinking = rule.blinking
                )
            }
        }

        return getDefaultSettings()
    }

    private fun getDefaultSettings(): BranchSettings {
        return if (settings.noBorderIfUnmatched) {
            BranchSettings(null, settings.defaultBorderStyle, false)
        } else {
            BranchSettings(
                color = parseColor(settings.defaultColor),
                style = settings.defaultBorderStyle,
                blinking = settings.defaultBlinking
            )
        }
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

    fun onSettingsChanged() {
        log.info("Settings changed, updating border...")
        updateBorder()
    }

    override fun dispose() {
        log.info("Disposing BranchBorderService")
        painter = null
        rootPane = null
    }

    companion object {
        fun getInstance(project: Project): BranchBorderService {
            return project.getService(BranchBorderService::class.java)
        }
    }
}
