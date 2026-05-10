package ru.bmstu.emk

import javafx.application.Application
import ru.bmstu.emk.util.DataSeeder

fun main() {
    DataSeeder.seed()
    Application.launch(EmkApplication::class.java)
}