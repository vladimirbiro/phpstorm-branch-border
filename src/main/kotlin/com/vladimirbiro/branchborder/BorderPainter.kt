package com.vladimirbiro.branchborder

import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.AbstractPainter
import com.vladimirbiro.branchborder.settings.BorderStyle
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Component
import java.awt.Graphics2D
import javax.swing.Timer

class BorderPainter : AbstractPainter(), Disposable {

    var borderColor: Color? = null
    var borderWidth: Int = 4
    var borderStyle: BorderStyle = BorderStyle.SOLID

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

        when (borderStyle) {
            BorderStyle.SOLID -> paintSolidBorder(g, width, height)
            BorderStyle.DASHED -> paintDashedBorder(g, width, height)
        }
    }

    private fun paintSolidBorder(g: Graphics2D, width: Int, height: Int) {
        // Top border
        g.fillRect(0, 0, width, borderWidth)
        // Bottom border
        g.fillRect(0, height - borderWidth, width, borderWidth)
        // Left border
        g.fillRect(0, 0, borderWidth, height)
        // Right border
        g.fillRect(width - borderWidth, 0, borderWidth, height)
    }

    private fun paintDashedBorder(g: Graphics2D, width: Int, height: Int) {
        val originalStroke = g.stroke
        g.stroke = BasicStroke(
            borderWidth.toFloat(),
            BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_MITER,
            10.0f,
            floatArrayOf(10.0f, 5.0f),
            0.0f
        )

        val half = borderWidth / 2

        // Top
        g.drawLine(0, half, width, half)
        // Bottom
        g.drawLine(0, height - half, width, height - half)
        // Left
        g.drawLine(half, 0, half, height)
        // Right
        g.drawLine(width - half, 0, width - half, height)

        g.stroke = originalStroke
    }

    fun setRepaintCallback(callback: () -> Unit) {
        repaintCallback = callback
    }

    fun updateBorder(
        color: Color?,
        width: Int,
        style: BorderStyle = BorderStyle.SOLID,
        blinking: Boolean = false,
        interval: Int = 500
    ) {
        borderWidth = width
        borderColor = color
        borderStyle = style

        val shouldBlink = blinking && color != null

        if (shouldBlink) {
            if (!blinkingEnabled || blinkingInterval != interval) {
                startBlinking(interval)
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
