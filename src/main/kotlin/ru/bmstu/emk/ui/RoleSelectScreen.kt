package ru.bmstu.emk.ui

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.*
import ru.bmstu.emk.EmkApplication

class RoleSelectScreen : VBox() {

    init {
        alignment = Pos.CENTER
        spacing = 0.0
        padding = Insets(0.0)

        val centerBox = VBox(32.0).apply {
            alignment = Pos.CENTER
            maxWidth = 420.0
        }

        val title = Label("АИС ЭМК").apply {
            style = "-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #dfe6ee;"
        }
        val subtitle = Label("Система управления электронными\nмультимедийными курсами").apply {
            styleClass.add("label-secondary")
            style = "-fx-text-alignment: center; -fx-font-size: 13px;"
        }
        val headerBox = VBox(8.0, title, subtitle).apply { alignment = Pos.CENTER }

        val rolesBox = VBox(0.0).apply {
            style = "-fx-background-color: #1c1d2b; -fx-background-radius: 8; -fx-border-color: #2a2b3d; -fx-border-radius: 8;"
        }

        val roles = listOf(
            Triple("Ученик", "Проходите курсы и отслеживайте прогресс", "STUDENT"),
            Triple("Преподаватель", "Создавайте курсы и следите за студентами", "TEACHER"),
            Triple("Администратор", "Управляйте платформой и аналитикой", "ADMIN"),
        )

        for ((i, triple) in roles.withIndex()) {
            val (roleName, desc, roleCode) = triple
            val row = HBox(12.0).apply {
                alignment = Pos.CENTER_LEFT
                padding = Insets(14.0, 20.0, 14.0, 20.0)
                style = if (i < roles.size - 1) "-fx-border-color: #222336; -fx-border-width: 0 0 1 0;" else ""
                styleClass.add("list-row")
                cursor = javafx.scene.Cursor.HAND

                val textBox = VBox(2.0,
                    Label(roleName).apply { style = "-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #dfe6ee;" },
                    Label(desc).apply { style = "-fx-font-size: 13px; -fx-text-fill: #5f6a7a;" }
                ).apply { HBox.setHgrow(this, Priority.ALWAYS) }

                children.addAll(textBox)
                setOnMouseClicked { EmkApplication.navigateTo(LoginScreen(roleCode)) }
            }
            rolesBox.children.add(row)
        }

        val footer = VBox(12.0,
            Label("МГТУ им. Н.Э. Баумана · ИУ5-35Б · Довлетов С.").apply {
                style = "-fx-text-fill: #3a3b50; -fx-font-size: 11px;"
            }
        ).apply { alignment = Pos.CENTER; padding = Insets(24.0, 0.0, 0.0, 0.0) }

        centerBox.children.addAll(headerBox, rolesBox, footer)
        children.add(centerBox)
    }
}