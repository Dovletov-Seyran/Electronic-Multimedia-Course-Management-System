package ru.bmstu.emk.ui

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.*
import ru.bmstu.emk.EmkApplication
import ru.bmstu.emk.service.AuthService
import ru.bmstu.emk.util.SessionManager

class RegisterScreen : VBox() {

    private val loginField = TextField().apply { promptText = "Логин" }
    private val passwordField = PasswordField().apply { promptText = "Пароль" }
    private val confirmField = PasswordField().apply { promptText = "Подтверждение пароля" }
    private val errorLabel = Label().apply { style = "-fx-text-fill: #e17055; -fx-font-size: 13px;"; isVisible = false }
    private val successLabel = Label().apply { style = "-fx-text-fill: #00b894; -fx-font-size: 13px;"; isVisible = false }

    init {
        alignment = Pos.CENTER

        val formBox = VBox(16.0).apply {
            maxWidth = 360.0
            alignment = Pos.CENTER_LEFT
            style = "-fx-background-color: #1c1d2b; -fx-background-radius: 8; -fx-border-color: #2a2b3d; -fx-border-radius: 8;"
            padding = Insets(32.0)
        }

        val title = Label("Регистрация").apply { style = "-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #dfe6ee;" }
        val subtitle = Label("Создайте аккаунт ученика").apply { styleClass.add("label-secondary") }

        val registerBtn = Button("Зарегистрироваться").apply {
            styleClass.addAll("button", "btn-primary"); maxWidth = Double.MAX_VALUE; prefHeight = 36.0
            setOnAction { doRegister() }
        }

        val backBtn = Button("← Назад ко входу").apply {
            styleClass.addAll("button", "btn-ghost")
            setOnAction { EmkApplication.navigateTo(LoginScreen()) }
        }

        confirmField.setOnAction { doRegister() }
        loginField.setOnAction { passwordField.requestFocus() }
        passwordField.setOnAction { confirmField.requestFocus() }

        formBox.children.addAll(
            title, subtitle,
            Region().apply { prefHeight = 8.0 },
            Label("Логин").apply { styleClass.add("label-heading") }, loginField,
            Label("Пароль").apply { styleClass.add("label-heading") }, passwordField,
            Label("Подтверждение пароля").apply { styleClass.add("label-heading") }, confirmField,
            errorLabel, successLabel,
            Region().apply { prefHeight = 4.0 },
            registerBtn, backBtn
        )

        children.add(formBox)
        javafx.application.Platform.runLater { loginField.requestFocus() }
    }

    private fun doRegister() {
        val login = loginField.text.trim()
        val password = passwordField.text.trim()
        val confirm = confirmField.text.trim()

        errorLabel.isVisible = false
        successLabel.isVisible = false

        if (login.isEmpty()) { showError("Введите логин"); return }
        if (login.length < 3) { showError("Логин должен быть не менее 3 символов"); return }
        if (password.isEmpty()) { showError("Введите пароль"); return }
        if (password.length < 4) { showError("Пароль должен быть не менее 4 символов"); return }
        if (password != confirm) { showError("Пароли не совпадают"); return }

        val user = AuthService.register(login, password, "STUDENT")
        if (user == null) {
            showError("Пользователь с таким логином уже существует")
            return
        }

        // Auto-login after registration
        SessionManager.currentUser = user
        EmkApplication.navigateTo(StudentDashboard())
    }

    private fun showError(msg: String) {
        errorLabel.text = msg; errorLabel.isVisible = true
    }
}
