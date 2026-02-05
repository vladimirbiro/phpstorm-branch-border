package com.vladimirbiro.branchborder

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.IdeGlassPaneUtil
import com.intellij.openapi.wm.WindowManager
import java.awt.Color
import javax.swing.JRootPane

@Service(Service.Level.PROJECT)
class BranchBorderService(private val project: Project) : Disposable {

    private val log = Logger.getInstance(BranchBorderService::class.java)

    private var config: BranchColorConfig? = null
    private var painter: BorderPainter? = null
    private var gitListener: GitBranchListener? = null
    private var configWatcher: ConfigFileWatcher? = null
    private var currentBranch: String? = null
    private var rootPane: JRootPane? = null

    fun initialize() {
        log.info("Initializing BranchBorderService for project: ${project.name}")

        config = BranchColorConfig(project)

        // Load initial config
        if (!config!!.loadConfig()) {
            log.info("No config file found, plugin inactive")
            return
        }

        // Setup painter
        setupPainter()

        // Setup git listener
        gitListener = GitBranchListener(project) { branch ->
            currentBranch = branch
            updateBorder()
        }
        Disposer.register(this, gitListener!!)
        gitListener!!.start()

        // Setup config file watcher
        configWatcher = ConfigFileWatcher(project, config!!.configFileName) {
            onConfigChanged()
        }
        Disposer.register(this, configWatcher!!)
        configWatcher!!.start()

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
        val cfg = config ?: return
        val p = painter ?: return

        val settings = cfg.getSettingsForBranch(currentBranch)
        p.updateBorder(settings.color, cfg.borderWidth, settings.blinking)

        rootPane?.repaint()

        log.info("Border updated: branch=$currentBranch, color=${settings.color}, blinking=${settings.blinking?.enabled}")
    }

    private fun onConfigChanged() {
        log.info("Config file changed, reloading...")

        val cfg = config ?: return

        if (cfg.loadConfig()) {
            // Config loaded successfully, check if we need to setup painter
            if (painter == null) {
                setupPainter()
            }
            updateBorder()
        } else {
            // Config removed or invalid, hide border
            painter?.updateBorder(null, 4)
            rootPane?.repaint()
        }
    }

    override fun dispose() {
        log.info("Disposing BranchBorderService")
        painter = null
        config = null
        rootPane = null
    }

    companion object {
        fun getInstance(project: Project): BranchBorderService {
            return project.getService(BranchBorderService::class.java)
        }
    }
}
