package ru.bmstu.emk.ui

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.*
import ru.bmstu.emk.EmkApplication
import ru.bmstu.emk.service.AuthService

class ForgotPasswordScreen : VBox() {

    private val loginField = TextField().apply { promptText = "Логин" }
    private val newPassField = PasswordField().apply { promptText = "Новый пароль" }
    private val confirmPassField = PasswordField().apply { promptText = "Подтверждение пароля" }
    private val errorLabel = Label().apply { style = "-fx-text-fill: #e17055; -fx-font-size: 13px;"; isVisible = false }
    private val successLabel = Label().apply { style = "-fx-text-fill: #00b894; -fx-font-size: 13px;"; isVisible = false }

    // Двухшаговый процесс: сначала проверяем логин, потом показываем поля пароля
    private var loginVerified = false
    private val passBox = VBox(12.0).apply { isVisible = false; isManaged = false }

    init {
        alignment = Pos.CENTER

        val formBox = VBox(16.0).apply {
            maxWidth = 360.0
            alignment = Pos.CENTER_LEFT
            style = "-fx-background-color: #1c1d2b; -fx-background-radius: 8; -fx-border-color: #2a2b3d; -fx-border-radius: 8;"
            padding = Insets(32.0)
        }

        val title = Label("Сброс пароля").apply {
            style = "-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #dfe6ee;"
        }
        val subtitle = Label("Введите логин для проверки").apply { styleClass.add("label-secondary") }

        val checkLoginBtn = Button("Проверить логин").apply {
            styleClass.addAll("button", "btn-primary"); maxWidth = Double.MAX_VALUE; prefHeight = 36.0
            setOnAction { checkLogin() }
        }

        val resetBtn = Button("Установить новый пароль").apply {
            styleClass.addAll("button", "btn-primary"); maxWidth = Double.MAX_VALUE; prefHeight = 36.0
            setOnAction { doReset() }
        }

        confirmPassField.setOnAction { doReset() }
        newPassField.setOnAction { confirmPassField.requestFocus() }
        loginField.setOnAction { checkLogin() }

        passBox.children.addAll(
            Label("Новый пароль").apply { styleClass.add("label-heading") }, newPassField,
            Label("Подтверждение пароля").apply { styleClass.add("label-heading") }, confirmPassField,
            resetBtn
        )

        val backBtn = Button("← Назад ко входу").apply {
            styleClass.addAll("button", "btn-ghost")
            setOnAction { EmkApplication.navigateTo(LoginScreen()) }
        }

        formBox.children.addAll(
            title, subtitle,
            Region().apply { prefHeight = 8.0 },
            Label("Логин").apply { styleClass.add("label-heading") }, loginField,
            errorLabel, successLabel,
            checkLoginBtn,
            passBox,
            backBtn
        )

        children.add(formBox)
        javafx.application.Platform.runLater { loginField.requestFocus() }
    }

    private fun checkLogin() {
        errorLabel.isVisible = false; successLabel.isVisible = false
        val login = loginField.text.trim()
        if (login.isEmpty()) { showError("Введите логин"); return }

        if (!AuthService.userExists(login)) {
            showError("Пользователь с таким логином не найден")
            return
        }

        // Логин найден — показываем поля для нового пароля
        loginVerified = true
        loginField.isEditable = false
        loginField.style = "-fx-opacity: 0.6;"
        successLabel.text = "Пользователь найден. Введите новый пароль."
        successLabel.isVisible = true
        passBox.isVisible = true
        passBox.isManaged = true
        newPassField.requestFocus()
    }

    private fun doReset() {
        errorLabel.isVisible = false; successLabel.isVisible = false
        if (!loginVerified) { showError("Сначала проверьте логин"); return }

        val login = loginField.text.trim()
        val newP = newPassField.text.trim()
        val confP = confirmPassField.text.trim()

        if (newP.isEmpty()) { showError("Введите новый пароль"); return }
        if (newP.length < 4) { showError("Пароль — минимум 4 символа"); return }
        if (newP != confP) { showError("Пароли не совпадают"); return }

        val ok = AuthService.resetPassword(login, newP)
        if (ok) {
            successLabel.text = "Пароль успешно изменён! Перенаправляем..."
            successLabel.isVisible = true
            javafx.application.Platform.runLater {
                Thread.sleep(1000)
                javafx.application.Platform.runLater { EmkApplication.navigateTo(LoginScreen()) }
            }
        } else {
            showError("Ошибка сброса пароля")
        }
    }

    private fun showError(msg: String) {
        errorLabel.text = msg; errorLabel.isVisible = true
    }
}
