package ru.bmstu.emk.ui

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.*
import ru.bmstu.emk.EmkApplication
import ru.bmstu.emk.service.AuthService
import ru.bmstu.emk.util.SessionManager

class LoginScreen(private val expectedRole: String) : VBox() {

    private val loginField = TextField().apply { promptText = "Логин" }
    private val passwordField = PasswordField().apply { promptText = "Пароль" }
    private val errorLabel = Label().apply { style = "-fx-text-fill: #e17055; -fx-font-size: 12px;"; isVisible = false }

    init {
        alignment = Pos.CENTER
        val roleName = when (expectedRole) { "STUDENT" -> "Ученик"; "TEACHER" -> "Преподаватель"; "ADMIN" -> "Администратор"; else -> "" }

        val formBox = VBox(16.0).apply {
            maxWidth = 340.0
            alignment = Pos.CENTER_LEFT
            style = "-fx-background-color: #1c1d2b; -fx-background-radius: 8; -fx-border-color: #2a2b3d; -fx-border-radius: 8;"
            padding = Insets(32.0)
        }

        val title = Label("Вход · $roleName").apply { style = "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #dfe6ee;" }

        val loginBtn = Button("Войти").apply {
            styleClass.addAll("button", "btn-primary"); maxWidth = Double.MAX_VALUE; prefHeight = 36.0
            setOnAction { doLogin() }
        }

        val backBtn = Button("← Назад").apply {
            styleClass.addAll("button", "btn-ghost")
            setOnAction { EmkApplication.navigateTo(RoleSelectScreen()) }
        }

        passwordField.setOnAction { doLogin() }
        loginField.setOnAction { passwordField.requestFocus() }

        formBox.children.addAll(
            title,
            Region().apply { prefHeight = 8.0 },
            Label("Логин").apply { styleClass.add("label-heading") }, loginField,
            Label("Пароль").apply { styleClass.add("label-heading") }, passwordField,
            errorLabel,
            Region().apply { prefHeight = 4.0 },
            loginBtn
        )

        if (expectedRole == "STUDENT") {
            val registerBtn = Button("Регистрация").apply {
                styleClass.addAll("button", "btn-ghost"); maxWidth = Double.MAX_VALUE
                setOnAction { EmkApplication.navigateTo(RegisterScreen()) }
            }
            formBox.children.add(registerBtn)
        }

        formBox.children.add(backBtn)

        children.add(formBox)
        javafx.application.Platform.runLater { loginField.requestFocus() }
    }

    private fun doLogin() {
        val login = loginField.text.trim()
        val password = passwordField.text.trim()
        if (login.isEmpty()) { showError("Введите логин"); return }
        if (password.isEmpty()) { showError("Введите пароль"); return }

        val user = AuthService.login(login, password)
        if (user == null) { showError("Неверный логин или пароль"); return }
        if (user.role != expectedRole) {
            showError("Этот аккаунт не для роли «${when (expectedRole) { "STUDENT" -> "Ученик"; "TEACHER" -> "Преподаватель"; else -> "Администратор" }}»")
            return
        }

        SessionManager.currentUser = user
        if (user.role == "TEACHER") SessionManager.currentTeacher = AuthService.getTeacherByUser(user)

        when (user.role) {
            "STUDENT" -> EmkApplication.navigateTo(StudentDashboard())
            "TEACHER" -> EmkApplication.navigateTo(TeacherDashboard())
            "ADMIN" -> EmkApplication.navigateTo(AdminDashboard())
        }
    }

    private fun showError(msg: String) {
        errorLabel.text = msg; errorLabel.isVisible = true
    }
}