package ru.bmstu.emk.ui

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.*
import ru.bmstu.emk.EmkApplication
import ru.bmstu.emk.service.AdminService
import ru.bmstu.emk.service.CourseStats
import ru.bmstu.emk.service.PlatformStats
import ru.bmstu.emk.util.SessionManager

class AdminDashboard : HBox() {

    private val contentArea = StackPane()
    private var activeButton: Button? = null

    init {
        val sidebar = VBox().apply {
            styleClass.add("sidebar"); prefWidth = 240.0; minWidth = 240.0
        }

        val logoBox = VBox(4.0,
            Label("⚙️").apply { style = "-fx-font-size: 28px;" },
            Label("Администратор").apply { styleClass.add("label-heading") },
            Label(SessionManager.currentUser?.login ?: "").apply { styleClass.add("label-muted") }
        ).apply {
            alignment = Pos.CENTER; padding = Insets(24.0, 16.0, 24.0, 16.0)
            style = "-fx-border-color: #3d3d5c; -fx-border-width: 0 0 1 0;"
        }

        val menuItems = listOf(
            "📊" to "Сводка",
            "🏠" to "Хостинги",
            "📚" to "Курсы",
            "👥" to "Ученики",
        )

        val menuButtons = menuItems.map { (icon, label) ->
            Button("$icon  $label").apply {
                styleClass.addAll("sidebar-item")
                setOnAction {
                    setActiveButton(this)
                    when (label) {
                        "Сводка" -> showSummary()
                        "Хостинги" -> showHostings()
                        "Курсы" -> showCourses()
                        "Ученики" -> showStudents()
                    }
                }
            }
        }

        val logoutBtn = Button("🚪  Выйти").apply {
            styleClass.addAll("sidebar-item"); style = "-fx-text-fill: #f87171;"
            setOnAction { SessionManager.logout(); EmkApplication.navigateTo(RoleSelectScreen()) }
        }
        val spacer = Region().apply { VBox.setVgrow(this, Priority.ALWAYS) }

        sidebar.children.addAll(logoBox)
        sidebar.children.addAll(menuButtons)
        sidebar.children.addAll(spacer, logoutBtn)
        contentArea.apply { HBox.setHgrow(this, Priority.ALWAYS) }
        children.addAll(sidebar, contentArea)

        setActiveButton(menuButtons[0]); showSummary()
    }

    private fun setActiveButton(btn: Button) {
        activeButton?.styleClass?.remove("sidebar-item-active"); btn.styleClass.add("sidebar-item-active"); activeButton = btn
    }

    private fun showSummary() {
        val container = VBox(24.0).apply { padding = Insets(32.0) }
        val header = Label("📊 Сводная аналитика").apply { styleClass.add("label-title") }

        val totalStudents = AdminService.getTotalStudents()
        val totalTeachers = AdminService.getTotalTeachers()
        val totalCourses = AdminService.getTotalCourses()
        val totalTracks = AdminService.getTotalTracks()
        val finishedTracks = AdminService.getFinishedTracks()

        fun statCard(value: String, label: String, color: String = "-accent"): VBox {
            return VBox(4.0,
                Label(value).apply { style = "-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: $color;" },
                Label(label).apply { styleClass.add("stat-label") }
            ).apply { styleClass.add("card-static"); alignment = Pos.CENTER; prefWidth = 180.0; padding = Insets(24.0) }
        }

        val statsRow1 = HBox(20.0,
            statCard("$totalStudents", "Учеников", "#60a5fa"),
            statCard("$totalTeachers", "Преподавателей", "#4ade80"),
            statCard("$totalCourses", "Курсов", "#7c6ff5"),
            statCard("$totalTracks", "Записей на курсы", "#fb923c"),
            statCard("$finishedTracks", "Завершено", "#4ade80")
        ).apply { alignment = Pos.CENTER_LEFT }

        // Сводка по хостингам
        val platformLabel = Label("📡 Платформы").apply { styleClass.add("label-subtitle"); padding = Insets(8.0, 0.0, 0.0, 0.0) }
        val platformStats = AdminService.getPlatformStats()
        val platformTable = TableView<PlatformStats>().apply {
            prefHeight = 250.0
            val nameCol = TableColumn<PlatformStats, String>("Платформа").apply {
                setCellValueFactory { javafx.beans.property.SimpleStringProperty(it.value.hostingName) }; prefWidth = 200.0
            }
            val urlCol = TableColumn<PlatformStats, String>("Адрес").apply {
                setCellValueFactory { javafx.beans.property.SimpleStringProperty(it.value.webAddress) }; prefWidth = 250.0
            }
            val coursesCol = TableColumn<PlatformStats, String>("Курсов").apply {
                setCellValueFactory { javafx.beans.property.SimpleStringProperty(it.value.courseCount.toString()) }; prefWidth = 100.0
            }
            val studentsCol = TableColumn<PlatformStats, String>("Студентов").apply {
                setCellValueFactory { javafx.beans.property.SimpleStringProperty(it.value.studentCount.toString()) }; prefWidth = 100.0
            }
            columns.addAll(nameCol, urlCol, coursesCol, studentsCol)
            items.addAll(platformStats)
        }

        container.children.addAll(header, statsRow1, platformLabel, platformTable)
        contentArea.children.setAll(ScrollPane(container).apply { isFitToWidth = true; hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER })
    }

    private fun showHostings() {
        val container = VBox(20.0).apply { padding = Insets(32.0) }
        val header = Label("🏠 Управление хостингами").apply { styleClass.add("label-title") }

        val addBtn = Button("＋ Добавить хостинг").apply {
            styleClass.addAll("button", "btn-primary")
            setOnAction { showHostingForm(null) }
        }

        val topRow = HBox(16.0, header, Region().apply { HBox.setHgrow(this, Priority.ALWAYS) }, addBtn).apply { alignment = Pos.CENTER_LEFT }

        val hostings = AdminService.getAllHostings()
        val grid = FlowPane(16.0, 16.0)

        for (h in hostings) {
            val nameLabel = Label(h.name).apply { styleClass.add("label-heading") }
            val urlLabel = Label(h.webAddress).apply { styleClass.add("label-secondary"); isWrapText = true; maxWidth = 240.0 }

            val editBtn = Button("✏️ Редактировать").apply {
                styleClass.addAll("button"); maxWidth = Double.MAX_VALUE
                setOnAction { showHostingForm(h.id) }
            }
            val deleteBtn = Button("🗑 Удалить").apply {
                styleClass.addAll("button", "btn-danger"); maxWidth = Double.MAX_VALUE
                setOnAction {
                    val (success, msg) = AdminService.deleteHosting(h.id)
                    if (!success) {
                        val alert = Alert(Alert.AlertType.WARNING, msg, ButtonType.OK)
                        alert.title = "Невозможно удалить"; alert.showAndWait()
                    } else {
                        showHostings()
                    }
                }
            }

            val card = VBox(10.0, nameLabel, urlLabel, Region().apply { prefHeight = 4.0 }, editBtn, deleteBtn).apply {
                styleClass.add("card"); prefWidth = 300.0; padding = Insets(20.0)
            }
            grid.children.add(card)
        }

        container.children.addAll(topRow, grid)
        contentArea.children.setAll(ScrollPane(container).apply { isFitToWidth = true; hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER })
    }

    private fun showHostingForm(hostingId: Long?) {
        val container = VBox(16.0).apply { padding = Insets(32.0); maxWidth = 500.0 }
        val backBtn = Button("← Назад").apply { styleClass.addAll("button", "btn-ghost"); setOnAction { showHostings() } }
        val isEdit = hostingId != null
        val header = Label(if (isEdit) "Редактирование хостинга" else "Новый хостинг").apply { styleClass.add("label-title") }

        var currentName = ""
        var currentUrl = ""
        if (isEdit) {
            val hostings = AdminService.getAllHostings()
            val h = hostings.find { it.id == hostingId }
            if (h != null) { currentName = h.name; currentUrl = h.webAddress }
        }

        val nameField = TextField(currentName).apply { promptText = "Название платформы" }
        val urlField = TextField(currentUrl).apply { promptText = "Веб-адрес (URL)" }

        val saveBtn = Button(if (isEdit) "💾 Сохранить" else "💾 Создать").apply {
            styleClass.addAll("button", "btn-primary"); prefWidth = 300.0
            setOnAction {
                val n = nameField.text.trim(); val u = urlField.text.trim()
                if (n.isEmpty() || u.isEmpty()) return@setOnAction
                if (isEdit) AdminService.updateHosting(hostingId!!, n, u)
                else AdminService.createHosting(n, u)
                showHostings()
            }
        }

        container.children.addAll(backBtn, header,
            Label("Название").apply { styleClass.add("label-heading") }, nameField,
            Label("Веб-адрес").apply { styleClass.add("label-heading") }, urlField,
            Region().apply { prefHeight = 8.0 }, saveBtn)
        contentArea.children.setAll(container)
    }

    private fun showCourses() {
        val container = VBox(20.0).apply { padding = Insets(32.0) }
        val header = Label("📚 Все курсы — аналитика").apply { styleClass.add("label-title") }

        val courseStats = AdminService.getCourseStats()
        val subtitle = Label("Всего курсов: ${courseStats.size}").apply { styleClass.add("label-secondary") }

        val table = TableView<CourseStats>().apply {
            prefHeight = 500.0
            val nameCol = TableColumn<CourseStats, String>("Курс").apply {
                setCellValueFactory { javafx.beans.property.SimpleStringProperty(it.value.courseName) }; prefWidth = 230.0
            }
            val teacherCol = TableColumn<CourseStats, String>("Преподаватель").apply {
                setCellValueFactory { javafx.beans.property.SimpleStringProperty(it.value.teacherName) }; prefWidth = 200.0
            }
            val hostCol = TableColumn<CourseStats, String>("Хостинг").apply {
                setCellValueFactory { javafx.beans.property.SimpleStringProperty(it.value.hostingName) }; prefWidth = 130.0
            }
            val durCol = TableColumn<CourseStats, String>("Часов").apply {
                setCellValueFactory { javafx.beans.property.SimpleStringProperty(it.value.duration.toString()) }; prefWidth = 70.0
            }
            val enrollCol = TableColumn<CourseStats, String>("Записано").apply {
                setCellValueFactory { javafx.beans.property.SimpleStringProperty(it.value.enrolledCount.toString()) }; prefWidth = 90.0
            }
            val finCol = TableColumn<CourseStats, String>("Завершили").apply {
                setCellValueFactory { javafx.beans.property.SimpleStringProperty(it.value.finishedCount.toString()) }; prefWidth = 90.0
            }
            val avgCol = TableColumn<CourseStats, String>("Ср. прогресс").apply {
                setCellValueFactory { javafx.beans.property.SimpleStringProperty("${String.format("%.0f", it.value.avgProgress)}%") }; prefWidth = 100.0
            }
            columns.addAll(nameCol, teacherCol, hostCol, durCol, enrollCol, finCol, avgCol)
            items.addAll(courseStats)
        }

        container.children.addAll(header, subtitle, table)
        contentArea.children.setAll(ScrollPane(container).apply { isFitToWidth = true })
    }

    private fun showStudents() {
        val container = VBox(20.0).apply { padding = Insets(32.0) }
        val header = Label("👥 Все ученики").apply { styleClass.add("label-title") }

        val students = AdminService.getAllStudents()
        val subtitle = Label("Всего учеников: ${students.size}").apply { styleClass.add("label-secondary") }

        val searchField = TextField().apply { promptText = "Поиск по логину..."; prefWidth = 300.0 }

        val table = TableView<ru.bmstu.emk.domain.User>().apply {
            prefHeight = 500.0
            val idCol = TableColumn<ru.bmstu.emk.domain.User, String>("ID").apply {
                setCellValueFactory { javafx.beans.property.SimpleStringProperty(it.value.id.toString()) }; prefWidth = 60.0
            }
            val loginCol = TableColumn<ru.bmstu.emk.domain.User, String>("Логин").apply {
                setCellValueFactory { javafx.beans.property.SimpleStringProperty(it.value.login) }; prefWidth = 200.0
            }
            val roleCol = TableColumn<ru.bmstu.emk.domain.User, String>("Роль").apply {
                setCellValueFactory { javafx.beans.property.SimpleStringProperty(it.value.role) }; prefWidth = 120.0
            }
            columns.addAll(idCol, loginCol, roleCol)
            items.addAll(students)
        }

        searchField.textProperty().addListener { _, _, newVal ->
            val filtered = students.filter { it.login.contains(newVal, ignoreCase = true) }
            table.items.setAll(filtered)
        }

        container.children.addAll(header, subtitle, searchField, table)
        contentArea.children.setAll(ScrollPane(container).apply { isFitToWidth = true })
    }
}