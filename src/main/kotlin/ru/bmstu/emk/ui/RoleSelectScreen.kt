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
        spacing = 20.0
        padding = Insets(60.0)
        styleClass.add("root")

        // Логотип / заголовок
        val logo = Label("🎓").apply { style = "-fx-font-size: 64px;" }
        val title = Label("АИС ЭМК").apply { styleClass.add("label-title") }
        val subtitle = Label("Система управления электронными мультимедийными курсами").apply {
            styleClass.add("label-secondary")
            style = "-fx-font-size: 15px;"
        }
        val uni = Label("МГТУ им. Н.Э. Баумана").apply {
            styleClass.add("label-muted")
        }

        val headerBox = VBox(8.0, logo, title, subtitle, uni).apply {
            alignment = Pos.CENTER
            padding = Insets(0.0, 0.0, 30.0, 0.0)
        }

        // Карточки ролей
        val rolesBox = HBox(20.0).apply {
            alignment = Pos.CENTER
        }

        val roles = listOf(
            Triple("👨‍🎓", "Ученик", "Проходите курсы и отслеживайте прогресс"),
            Triple("👨‍🏫", "Преподаватель", "Создавайте курсы и следите за студентами"),
            Triple("⚙️", "Администратор", "Управляйте платформой и аналитикой"),
        )

        for ((icon, roleName, desc) in roles) {
            val card = createRoleCard(icon, roleName, desc)
            rolesBox.children.add(card)
        }

        // Нижняя часть
        val infoBtn = Button("ℹ Информация о создателе").apply {
            styleClass.addAll("button", "btn-ghost")
            setOnAction { showAuthorInfo() }
        }

        val exitBtn = Button("Выход").apply {
            styleClass.addAll("button")
            setOnAction { javafx.application.Platform.exit() }
        }

        val bottomBox = HBox(16.0, infoBtn, exitBtn).apply {
            alignment = Pos.CENTER
            padding = Insets(30.0, 0.0, 0.0, 0.0)
        }

        children.addAll(headerBox, rolesBox, bottomBox)
    }

    private fun createRoleCard(icon: String, roleName: String, desc: String): VBox {
        val iconLabel = Label(icon).apply { style = "-fx-font-size: 48px;" }
        val nameLabel = Label(roleName).apply { styleClass.add("label-subtitle") }
        val descLabel = Label(desc).apply {
            styleClass.add("label-secondary")
            isWrapText = true
            maxWidth = 200.0
            style = "-fx-text-alignment: center;"
        }
        val enterBtn = Button("Войти →").apply {
            styleClass.addAll("button", "btn-primary")
            maxWidth = Double.MAX_VALUE
            setOnAction {
                val role = when (roleName) {
                    "Ученик" -> "STUDENT"
                    "Преподаватель" -> "TEACHER"
                    "Администратор" -> "ADMIN"
                    else -> "STUDENT"
                }
                EmkApplication.navigateTo(LoginScreen(role))
            }
        }

        return VBox(16.0, iconLabel, nameLabel, descLabel, enterBtn).apply {
            alignment = Pos.CENTER
            styleClass.add("card")
            padding = Insets(30.0)
            prefWidth = 260.0
            prefHeight = 320.0
        }
    }

    private fun showAuthorInfo() {
        val alert = javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION)
        alert.title = "Информация о создателе"
        alert.headerText = "АИС ЭМК"
        alert.contentText = """
            Разработчик: Довлетов Сейран
            Группа: ИУ5-35Б
            МГТУ им. Н.Э. Баумана
            Кафедра ИУ-5 «Системы обработки информации и управления»
            2025 г.
        """.trimIndent()
        alert.dialogPane.style = """
            -fx-background-color: #2a2a3d;
        """
        alert.dialogPane.lookup(".content.label")?.style = "-fx-text-fill: #e4e4ef;"
        alert.showAndWait()
    }
}