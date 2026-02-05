package com.vladimirbiro.branchborder

import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.AbstractPainter
import java.awt.Color
import java.awt.Component
import java.awt.Graphics2D
import javax.swing.Timer

class BorderPainter : AbstractPainter(), Disposable {

    var borderColor: Color? = null
    var borderWidth: Int = 4

    private var blinkingEnabled: Boolean = false
    private var blinkingInterval: Int = 500
    private var blinkTimer: Timer? = null
    private var isVisible: Boolean = true
    private var repaintCallback: (() -> Unit)? = null

    override fun needsRepaint(): Boolean = borderColor != null

    override fun executePaint(component: Component, g: Graphics2D) {
        val color = borderColor ?: return

        if (blinkingEnabled && !isVisible) return

        val width = component.width
        val height = component.height

        g.color = color

        // Top border
        g.fillRect(0, 0, width, borderWidth)

        // Bottom border
        g.fillRect(0, height - borderWidth, width, borderWidth)

        // Left border
        g.fillRect(0, 0, borderWidth, height)

        // Right border
        g.fillRect(width - borderWidth, 0, borderWidth, height)
    }

    fun setRepaintCallback(callback: () -> Unit) {
        repaintCallback = callback
    }

    fun updateBorder(color: Color?, width: Int, blinking: BlinkingConfig? = null) {
        borderWidth = width
        borderColor = color

        val shouldBlink = blinking?.enabled == true && color != null
        val newInterval = blinking?.interval?.coerceIn(100, 5000) ?: 500

        if (shouldBlink) {
            if (!blinkingEnabled || blinkingInterval != newInterval) {
                startBlinking(newInterval)
            }
        } else {
            stopBlinking()
        }
    }

    private fun startBlinking(interval: Int) {
        stopBlinking()

        blinkingEnabled = true
        blinkingInterval = interval
        isVisible = true

        blinkTimer = Timer(interval) {
            isVisible = !isVisible
            repaintCallback?.invoke()
        }
        blinkTimer?.start()
    }

    private fun stopBlinking() {
        blinkTimer?.stop()
        blinkTimer = null
        blinkingEnabled = false
        isVisible = true
    }

    override fun dispose() {
        stopBlinking()
        repaintCallback = null
    }
}
