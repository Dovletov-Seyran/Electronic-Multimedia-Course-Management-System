package ru.bmstu.emk.ui

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.*
import ru.bmstu.emk.EmkApplication
import ru.bmstu.emk.service.AuthService
import ru.bmstu.emk.util.SessionManager

class LoginScreen(private val expectedRole: String) : VBox() {

    private val loginField = TextField().apply {
        promptText = "Введите логин"
        prefWidth = 300.0
    }
    private val passwordField = PasswordField().apply {
        promptText = "Введите пароль"
        prefWidth = 300.0
    }
    private val errorLabel = Label().apply {
        styleClass.add("label-muted")
        style = "-fx-text-fill: #f87171;"
        isVisible = false
    }

    init {
        alignment = Pos.CENTER
        spacing = 20.0
        padding = Insets(60.0)

        val roleName = when (expectedRole) {
            "STUDENT" -> "Ученик"
            "TEACHER" -> "Преподаватель"
            "ADMIN" -> "Администратор"
            else -> ""
        }

        val icon = when (expectedRole) {
            "STUDENT" -> "👨‍🎓"
            "TEACHER" -> "👨‍🏫"
            "ADMIN" -> "⚙️"
            else -> ""
        }

        val iconLabel = Label(icon).apply { style = "-fx-font-size: 48px;" }
        val title = Label("Вход — $roleName").apply { styleClass.add("label-title") }
        val subtitle = Label("Введите учётные данные для входа в систему").apply {
            styleClass.add("label-secondary")
        }

        val loginLabel = Label("Логин").apply { styleClass.add("label-heading") }
        val passLabel = Label("Пароль").apply { styleClass.add("label-heading") }

        val loginBtn = Button("Войти").apply {
            styleClass.addAll("button", "btn-primary")
            prefWidth = 300.0
            prefHeight = 44.0
            setOnAction { doLogin() }
        }

        val backBtn = Button("← Назад").apply {
            styleClass.addAll("button", "btn-ghost")
            setOnAction { EmkApplication.navigateTo(RoleSelectScreen()) }
        }

        // Enter нажатие
        passwordField.setOnAction { doLogin() }
        loginField.setOnAction { passwordField.requestFocus() }

        val formBox = VBox(12.0,
            loginLabel, loginField,
            passLabel, passwordField,
            errorLabel,
            Region().apply { prefHeight = 8.0 },
            loginBtn
        ).apply {
            alignment = Pos.CENTER
            maxWidth = 340.0
        }

        val cardBox = VBox(24.0, iconLabel, title, subtitle, formBox, backBtn).apply {
            alignment = Pos.CENTER
            styleClass.add("card-static")
            padding = Insets(40.0)
            maxWidth = 440.0
        }

        children.add(cardBox)

        // Фокус на логин
        javafx.application.Platform.runLater { loginField.requestFocus() }
    }

    private fun doLogin() {
        val login = loginField.text.trim()
        val password = passwordField.text.trim()

        if (login.isEmpty()) {
            showError("Введите логин")
            return
        }
        if (password.isEmpty()) {
            showError("Введите пароль")
            return
        }

        val user = AuthService.login(login, password)

        if (user == null) {
            showError("Неверный логин или пароль")
            return
        }

        if (user.role != expectedRole) {
            val roleName = when (expectedRole) {
                "STUDENT" -> "ученика"
                "TEACHER" -> "преподавателя"
                "ADMIN" -> "администратора"
                else -> ""
            }
            showError("Этот аккаунт не является аккаунтом $roleName")
            return
        }

        // Успешный вход
        SessionManager.currentUser = user
        if (user.role == "TEACHER") {
            SessionManager.currentTeacher = AuthService.getTeacherByUser(user)
        }

        // Переходим в личный кабинет
        when (user.role) {
            "STUDENT" -> EmkApplication.navigateTo(StudentDashboard())
            "TEACHER" -> EmkApplication.navigateTo(TeacherDashboard())
            "ADMIN" -> EmkApplication.navigateTo(AdminDashboard())
        }
    }

    private fun showError(message: String) {
        errorLabel.text = message
        errorLabel.isVisible = true
        // Трясём поле
        val shake = javafx.animation.TranslateTransition(
            javafx.util.Duration.millis(50.0), loginField.parent
        )
        shake.fromX = -10.0
        shake.toX = 10.0
        shake.cycleCount = 4
        shake.isAutoReverse = true
        shake.setOnFinished { loginField.parent.translateX = 0.0 }
        shake.play()
    }
}