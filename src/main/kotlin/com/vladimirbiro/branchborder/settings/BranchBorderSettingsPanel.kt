package com.vladimirbiro.branchborder.settings

import com.google.gson.Gson
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.ColorPanel
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.table.JBTable
import com.vladimirbiro.branchborder.BranchColorConfigData
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Graphics
import javax.swing.*
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer

class BranchBorderSettingsPanel(private val project: Project) {

    private val settings = BranchBorderSettings.getInstance(project)

    val panel: JPanel

    private val enabledCheckbox = JBCheckBox("Enable Git Branch Border")
    private val borderWidthSpinner = JSpinner(SpinnerNumberModel(4, 1, 20, 1))
    private val blinkIntervalSpinner = JSpinner(SpinnerNumberModel(500, 100, 5000, 50))

    private val defaultColorPanel = ColorPanel()
    private val defaultStyleCombo = JComboBox(arrayOf("Solid", "Dashed"))
    private val defaultBlinkingCheckbox = JBCheckBox("Enable blinking")
    private val noBorderIfUnmatchedCheckbox = JBCheckBox("No border if unmatched")

    private val rulesTableModel = BranchRulesTableModel()
    private val rulesTable = JBTable(rulesTableModel)

    init {
        panel = createPanel()
        loadSettings()
    }

    private fun createPanel(): JPanel {
        return panel {
            row {
                cell(enabledCheckbox)
            }

            group("General") {
                row("Border width:") {
                    cell(borderWidthSpinner)
                    label("px")
                }
                row("Blink interval:") {
                    cell(blinkIntervalSpinner)
                    label("ms")
                }
            }

            group("Default (branches without rule)") {
                row("Color:") {
                    cell(defaultColorPanel)
                }
                row("Border style:") {
                    cell(defaultStyleCombo)
                }
                row {
                    cell(defaultBlinkingCheckbox)
                }
                row {
                    cell(noBorderIfUnmatchedCheckbox)
                }
            }

            group("Branch Rules") {
                row {
                    cell(createRulesPanel())
                        .align(Align.FILL)
                }.resizableRow()
                row {
                    comment("Rules are evaluated top to bottom. First match wins.")
                }
            }

            group("Import") {
                row {
                    button("Import from JSON...") { importFromJson() }
                }
            }
        }
    }

    private fun createRulesPanel(): JComponent {
        rulesTable.setShowGrid(true)
        rulesTable.rowHeight = 25

        rulesTable.columnModel.getColumn(0).preferredWidth = 150
        rulesTable.columnModel.getColumn(1).preferredWidth = 70
        rulesTable.columnModel.getColumn(2).preferredWidth = 80
        rulesTable.columnModel.getColumn(3).preferredWidth = 70
        rulesTable.columnModel.getColumn(4).preferredWidth = 50

        rulesTable.columnModel.getColumn(2).cellRenderer = ColorCellRenderer()
        rulesTable.columnModel.getColumn(4).cellRenderer = BooleanCellRenderer()

        val decorator = ToolbarDecorator.createDecorator(rulesTable)
            .setAddAction { addRule() }
            .setEditAction { editRule() }
            .setRemoveAction { removeRule() }
            .setMoveUpAction { moveRuleUp() }
            .setMoveDownAction { moveRuleDown() }

        val panel = decorator.createPanel()
        panel.preferredSize = Dimension(600, 200)
        return panel
    }

    private fun addRule() {
        val dialog = BranchRuleDialog(panel, null, getDefaultStyle())
        if (dialog.showAndGet()) {
            rulesTableModel.addRule(dialog.getResult())
        }
    }

    private fun editRule() {
        val selectedRow = rulesTable.selectedRow
        if (selectedRow >= 0) {
            val rule = rulesTableModel.getRule(selectedRow)
            val dialog = BranchRuleDialog(panel, rule, getDefaultStyle())
            if (dialog.showAndGet()) {
                rulesTableModel.updateRule(selectedRow, dialog.getResult())
            }
        }
    }

    private fun removeRule() {
        val selectedRow = rulesTable.selectedRow
        if (selectedRow >= 0) {
            rulesTableModel.removeRule(selectedRow)
        }
    }

    private fun moveRuleUp() {
        val selectedRow = rulesTable.selectedRow
        if (selectedRow > 0) {
            rulesTableModel.moveRule(selectedRow, selectedRow - 1)
            rulesTable.setRowSelectionInterval(selectedRow - 1, selectedRow - 1)
        }
    }

    private fun moveRuleDown() {
        val selectedRow = rulesTable.selectedRow
        if (selectedRow >= 0 && selectedRow < rulesTableModel.rowCount - 1) {
            rulesTableModel.moveRule(selectedRow, selectedRow + 1)
            rulesTable.setRowSelectionInterval(selectedRow + 1, selectedRow + 1)
        }
    }

    private fun getDefaultStyle(): BorderStyle {
        return if (defaultStyleCombo.selectedIndex == 0) BorderStyle.SOLID else BorderStyle.DASHED
    }

    private fun importFromJson() {
        val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("json")
        descriptor.title = "Select .branch-colors.json"

        val file = FileChooser.chooseFile(descriptor, project, project.baseDir?.findChild(".branch-colors.json"))
        if (file != null) {
            try {
                val content = String(file.contentsToByteArray())
                val gson = Gson()
                val data = gson.fromJson(content, BranchColorConfigData::class.java)

                borderWidthSpinner.value = data.borderWidth.coerceIn(1, 20)
                data.defaultColor?.let {
                    defaultColorPanel.selectedColor = Color.decode(it)
                }
                data.blinking?.let {
                    defaultBlinkingCheckbox.isSelected = it.enabled
                    blinkIntervalSpinner.value = it.interval.coerceIn(100, 5000)
                }

                rulesTableModel.clear()
                data.branches.forEach { rule ->
                    rulesTableModel.addRule(
                        BranchRuleState(
                            pattern = rule.pattern,
                            matchType = rule.match,
                            color = rule.color,
                            borderStyle = null,
                            blinking = rule.blinking?.enabled ?: false
                        )
                    )
                }

                Messages.showInfoMessage(project, "Configuration imported successfully.", "Import Complete")
            } catch (e: Exception) {
                Messages.showErrorDialog(project, "Failed to import: ${e.message}", "Import Error")
            }
        }
    }

    fun loadSettings() {
        enabledCheckbox.isSelected = settings.enabled
        borderWidthSpinner.value = settings.borderWidth
        blinkIntervalSpinner.value = settings.blinkInterval

        try {
            defaultColorPanel.selectedColor = Color.decode(settings.defaultColor)
        } catch (e: Exception) {
            defaultColorPanel.selectedColor = Color.decode("#FF6600")
        }
        defaultStyleCombo.selectedIndex = if (settings.defaultBorderStyle == BorderStyle.SOLID) 0 else 1
        defaultBlinkingCheckbox.isSelected = settings.defaultBlinking
        noBorderIfUnmatchedCheckbox.isSelected = settings.noBorderIfUnmatched

        rulesTableModel.clear()
        settings.branchRules.forEach { rulesTableModel.addRule(it.copy()) }
    }

    fun applySettings() {
        settings.enabled = enabledCheckbox.isSelected
        settings.borderWidth = borderWidthSpinner.value as Int
        settings.blinkInterval = blinkIntervalSpinner.value as Int

        settings.defaultColor = String.format("#%06X", defaultColorPanel.selectedColor?.rgb?.and(0xFFFFFF) ?: 0xFF6600)
        settings.defaultBorderStyle = if (defaultStyleCombo.selectedIndex == 0) BorderStyle.SOLID else BorderStyle.DASHED
        settings.defaultBlinking = defaultBlinkingCheckbox.isSelected
        settings.noBorderIfUnmatched = noBorderIfUnmatchedCheckbox.isSelected

        settings.branchRules.clear()
        settings.branchRules.addAll(rulesTableModel.getRules())
    }

    fun isModified(): Boolean {
        if (enabledCheckbox.isSelected != settings.enabled) return true
        if (borderWidthSpinner.value != settings.borderWidth) return true
        if (blinkIntervalSpinner.value != settings.blinkInterval) return true

        val currentColor = String.format("#%06X", defaultColorPanel.selectedColor?.rgb?.and(0xFFFFFF) ?: 0xFF6600)
        if (currentColor != settings.defaultColor) return true

        val currentStyle = if (defaultStyleCombo.selectedIndex == 0) BorderStyle.SOLID else BorderStyle.DASHED
        if (currentStyle != settings.defaultBorderStyle) return true

        if (defaultBlinkingCheckbox.isSelected != settings.defaultBlinking) return true
        if (noBorderIfUnmatchedCheckbox.isSelected != settings.noBorderIfUnmatched) return true

        val currentRules = rulesTableModel.getRules()
        if (currentRules.size != settings.branchRules.size) return true

        for (i in currentRules.indices) {
            if (currentRules[i] != settings.branchRules[i]) return true
        }

        return false
    }
}

class BranchRulesTableModel : AbstractTableModel() {

    private val columns = arrayOf("Pattern", "Match", "Color", "Style", "Blink")
    private val rules = mutableListOf<BranchRuleState>()

    override fun getRowCount(): Int = rules.size
    override fun getColumnCount(): Int = columns.size
    override fun getColumnName(column: Int): String = columns[column]

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        val rule = rules[rowIndex]
        return when (columnIndex) {
            0 -> rule.pattern
            1 -> rule.matchType
            2 -> rule.color
            3 -> rule.borderStyle?.name?.lowercase() ?: "default"
            4 -> rule.blinking
            else -> ""
        }
    }

    override fun getColumnClass(columnIndex: Int): Class<*> {
        return when (columnIndex) {
            4 -> java.lang.Boolean::class.java
            else -> String::class.java
        }
    }

    fun addRule(rule: BranchRuleState) {
        rules.add(rule)
        fireTableRowsInserted(rules.size - 1, rules.size - 1)
    }

    fun updateRule(index: Int, rule: BranchRuleState) {
        rules[index] = rule
        fireTableRowsUpdated(index, index)
    }

    fun removeRule(index: Int) {
        rules.removeAt(index)
        fireTableRowsDeleted(index, index)
    }

    fun moveRule(from: Int, to: Int) {
        val rule = rules.removeAt(from)
        rules.add(to, rule)
        fireTableDataChanged()
    }

    fun getRule(index: Int): BranchRuleState = rules[index]

    fun getRules(): List<BranchRuleState> = rules.map { it.copy() }

    fun clear() {
        val size = rules.size
        rules.clear()
        if (size > 0) {
            fireTableRowsDeleted(0, size - 1)
        }
    }
}

class ColorCellRenderer : DefaultTableCellRenderer() {
    override fun getTableCellRendererComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
        if (value is String && value.startsWith("#")) {
            try {
                val color = Color.decode(value)
                icon = ColorIcon(color)
                text = value
            } catch (e: Exception) {
                icon = null
            }
        } else {
            icon = null
        }
        return component
    }
}

class ColorIcon(private val color: Color) : Icon {
    override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
        g.color = color
        g.fillRect(x, y, iconWidth, iconHeight)
        g.color = Color.BLACK
        g.drawRect(x, y, iconWidth - 1, iconHeight - 1)
    }

    override fun getIconWidth(): Int = 16
    override fun getIconHeight(): Int = 16
}

class BooleanCellRenderer : DefaultTableCellRenderer() {
    override fun getTableCellRendererComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
        text = if (value == true) "✓" else ""
        horizontalAlignment = CENTER
        return component
    }
}
