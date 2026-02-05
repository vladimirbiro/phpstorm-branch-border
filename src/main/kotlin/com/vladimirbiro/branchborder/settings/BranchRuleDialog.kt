package com.vladimirbiro.branchborder.settings

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.ColorPanel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import java.awt.*
import javax.swing.*

class BranchRuleDialog(
    parent: Component,
    private val rule: BranchRuleState?,
    private val defaultStyle: BorderStyle
) : DialogWrapper(parent, true) {

    private val patternField = JBTextField(20)
    private val matchTypeCombo = JComboBox(arrayOf("exact", "prefix", "regex"))
    private val colorPanel = ColorPanel()
    private val styleCombo = JComboBox(arrayOf("Default", "Solid", "Dashed"))
    private val blinkingCheckbox = JBCheckBox("Enable blinking")

    private val previewPanel = BorderPreviewPanel()

    init {
        title = if (rule == null) "Add Branch Rule" else "Edit Branch Rule"
        init()
        loadRule()
        updatePreview()
    }

    private fun loadRule() {
        rule?.let {
            patternField.text = it.pattern
            matchTypeCombo.selectedItem = it.matchType
            colorPanel.selectedColor = Color.decode(it.color)
            styleCombo.selectedIndex = when (it.borderStyle) {
                null -> 0
                BorderStyle.SOLID -> 1
                BorderStyle.DASHED -> 2
            }
            blinkingCheckbox.isSelected = it.blinking
        } ?: run {
            colorPanel.selectedColor = Color.RED
        }
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            insets = Insets(4, 4, 4, 4)
            anchor = GridBagConstraints.WEST
        }

        // Pattern
        gbc.gridx = 0; gbc.gridy = 0
        panel.add(JBLabel("Pattern:"), gbc)
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0
        panel.add(patternField, gbc)

        // Match type
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0
        panel.add(JBLabel("Match type:"), gbc)
        gbc.gridx = 1
        panel.add(matchTypeCombo, gbc)

        // Color
        gbc.gridx = 0; gbc.gridy = 2
        panel.add(JBLabel("Color:"), gbc)
        gbc.gridx = 1
        colorPanel.addActionListener { updatePreview() }
        panel.add(colorPanel, gbc)

        // Border style
        gbc.gridx = 0; gbc.gridy = 3
        panel.add(JBLabel("Border style:"), gbc)
        gbc.gridx = 1
        styleCombo.addActionListener { updatePreview() }
        panel.add(styleCombo, gbc)

        // Blinking
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2
        panel.add(blinkingCheckbox, gbc)

        // Preview
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2
        gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1.0
        panel.add(createPreviewSection(), gbc)

        return panel
    }

    private fun createPreviewSection(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createTitledBorder("Preview")

        previewPanel.preferredSize = Dimension(200, 100)
        panel.add(previewPanel, BorderLayout.CENTER)

        return panel
    }

    private fun updatePreview() {
        val color = colorPanel.selectedColor ?: Color.RED
        val style = when (styleCombo.selectedIndex) {
            0 -> defaultStyle
            1 -> BorderStyle.SOLID
            else -> BorderStyle.DASHED
        }
        previewPanel.updateBorder(color, style)
    }

    fun getResult(): BranchRuleState {
        return BranchRuleState(
            pattern = patternField.text,
            matchType = matchTypeCombo.selectedItem as String,
            color = String.format("#%06X", colorPanel.selectedColor?.rgb?.and(0xFFFFFF) ?: 0xFF0000),
            borderStyle = when (styleCombo.selectedIndex) {
                0 -> null
                1 -> BorderStyle.SOLID
                else -> BorderStyle.DASHED
            },
            blinking = blinkingCheckbox.isSelected
        )
    }

    override fun doValidate(): ValidationInfo? {
        if (patternField.text.isBlank()) {
            return ValidationInfo("Pattern cannot be empty", patternField)
        }
        if (matchTypeCombo.selectedItem == "regex") {
            try {
                Regex(patternField.text)
            } catch (e: Exception) {
                return ValidationInfo("Invalid regex pattern", patternField)
            }
        }
        return null
    }
}

class BorderPreviewPanel : JPanel() {

    private var borderColor: Color = Color.RED
    private var borderStyle: BorderStyle = BorderStyle.SOLID
    private val borderWidth = 4

    fun updateBorder(color: Color, style: BorderStyle) {
        borderColor = color
        borderStyle = style
        repaint()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D

        // Background
        g2.color = background
        g2.fillRect(0, 0, width, height)

        // Border
        g2.color = borderColor

        if (borderStyle == BorderStyle.DASHED) {
            g2.stroke = BasicStroke(
                borderWidth.toFloat(),
                BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER,
                10.0f,
                floatArrayOf(10.0f, 5.0f),
                0.0f
            )
            val half = borderWidth / 2
            g2.drawRect(half, half, width - borderWidth, height - borderWidth)
        } else {
            g2.fillRect(0, 0, width, borderWidth)
            g2.fillRect(0, height - borderWidth, width, borderWidth)
            g2.fillRect(0, 0, borderWidth, height)
            g2.fillRect(width - borderWidth, 0, borderWidth, height)
        }

        // Center text
        g2.color = foreground
        g2.font = Font("Dialog", Font.PLAIN, 12)
        val text = "branch-name"
        val fm = g2.fontMetrics
        val textX = (width - fm.stringWidth(text)) / 2
        val textY = (height + fm.ascent - fm.descent) / 2
        g2.drawString(text, textX, textY)
    }
}
