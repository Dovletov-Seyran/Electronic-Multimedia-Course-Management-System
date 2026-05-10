package ru.bmstu.emk

import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage
import ru.bmstu.emk.ui.LoginScreen
import ru.bmstu.emk.ui.RoleSelectScreen
import ru.bmstu.emk.util.DataSeeder
import ru.bmstu.emk.util.HibernateUtil

class EmkApplication : Application() {

    companion object {
        lateinit var primaryStage: Stage
        lateinit var mainScene: Scene

        fun navigateTo(screen: javafx.scene.Parent) {
            mainScene.root = screen
        }
    }

    override fun start(stage: Stage) {
        primaryStage = stage

        val root = RoleSelectScreen()
        mainScene = Scene(root, 1200.0, 750.0)

        // Подключаем тёмную тему
        val css = javaClass.getResource("/dark-theme.css")?.toExternalForm()
        if (css != null) mainScene.stylesheets.add(css)

        stage.title = "АИС ЭМК — Система управления курсами"
        stage.scene = mainScene
        stage.minWidth = 1000.0
        stage.minHeight = 650.0
        stage.show()
    }

    override fun stop() {
        HibernateUtil.shutdown()
    }
}