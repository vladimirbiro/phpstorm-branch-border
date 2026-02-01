package com.vladimirbiro.branchborder

import com.intellij.openapi.ui.AbstractPainter
import java.awt.Color
import java.awt.Component
import java.awt.Graphics2D

class BorderPainter : AbstractPainter() {

    var borderColor: Color? = null
    var borderWidth: Int = 4

    override fun needsRepaint(): Boolean = borderColor != null

    override fun executePaint(component: Component, g: Graphics2D) {
        val color = borderColor ?: return

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

    fun updateBorder(color: Color?, width: Int) {
        borderWidth = width
        borderColor = color
    }
}
