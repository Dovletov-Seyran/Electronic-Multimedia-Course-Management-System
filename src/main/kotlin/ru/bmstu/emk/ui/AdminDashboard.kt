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
            Label("Администратор").apply { styleClass.add("label-heading") },
            Label(SessionManager.currentUser?.login ?: "").apply { styleClass.add("label-muted") }
        ).apply {
            alignment = Pos.CENTER; padding = Insets(24.0, 16.0, 24.0, 16.0)
            style = "-fx-border-color: #3d3d5c; -fx-border-width: 0 0 1 0;"
        }

        val menuItems = listOf("Сводка", "Пользователи", "Хостинги", "Курсы")

        val menuButtons = menuItems.map { label ->
            Button(label).apply {
                styleClass.addAll("sidebar-item")
                setOnAction {
                    setActiveButton(this)
                    when (label) {
                        "Сводка" -> showSummary()
                        "Пользователи" -> showUsers()
                        "Хостинги" -> showHostings()
                        "Курсы" -> showCourses()
                    }
                }
            }
        }

        val logoutBtn = Button("Выйти").apply {
            styleClass.addAll("sidebar-item"); style = "-fx-text-fill: #f87171;"
            setOnAction { SessionManager.logout(); EmkApplication.navigateTo(LoginScreen()) }
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
        val header = Label("Сводная аналитика").apply { styleClass.add("label-title") }

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
        val platformLabel = Label("Платформы").apply { styleClass.add("label-subtitle"); padding = Insets(8.0, 0.0, 0.0, 0.0) }
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
        val header = Label("Управление хостингами").apply { styleClass.add("label-title") }

        val addBtn = Button("+ Добавить хостинг").apply {
            styleClass.addAll("button", "btn-primary")
            setOnAction { showHostingForm(null) }
        }

        val topRow = HBox(16.0, header, Region().apply { HBox.setHgrow(this, Priority.ALWAYS) }, addBtn).apply { alignment = Pos.CENTER_LEFT }

        val hostings = AdminService.getAllHostings()
        val grid = FlowPane(16.0, 16.0)

        for (h in hostings) {
            val nameLabel = Label(h.name).apply { styleClass.add("label-heading") }
            val urlLabel = Label(h.webAddress).apply { styleClass.add("label-secondary"); isWrapText = true; maxWidth = 240.0 }

            val editBtn = Button("Редактировать").apply {
                styleClass.addAll("button"); maxWidth = Double.MAX_VALUE
                setOnAction { showHostingForm(h.id) }
            }
            val deleteBtn = Button("Удалить").apply {
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

        val saveBtn = Button(if (isEdit) "Сохранить" else "Создать").apply {
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
        val header = Label("Все курсы — аналитика").apply { styleClass.add("label-title") }

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

        val searchField = TextField().apply { promptText = "Поиск по курсу, преподавателю или хостингу..."; prefWidth = 350.0 }
        searchField.textProperty().addListener { _, _, newVal ->
            val filtered = courseStats.filter {
                it.courseName.contains(newVal, ignoreCase = true) ||
                it.teacherName.contains(newVal, ignoreCase = true) ||
                it.hostingName.contains(newVal, ignoreCase = true)
            }
            table.items.setAll(filtered)
        }

        container.children.addAll(header, subtitle, searchField, table)
        contentArea.children.setAll(ScrollPane(container).apply { isFitToWidth = true })
    }

    private fun showUsers() {
        val container = VBox(20.0).apply { padding = Insets(32.0) }
        val header = Label("Управление пользователями").apply { styleClass.add("label-title") }

        val currentUserId = SessionManager.currentUser?.id ?: 0
        val allUsers = AdminService.getAllUsers().filter { it.id != currentUserId }
        val subtitle = Label("Всего пользователей: ${allUsers.size}").apply { styleClass.add("label-secondary") }

        val searchField = TextField().apply { promptText = "Поиск по логину..."; prefWidth = 350.0 }

        val listBox = VBox(0.0).apply {
            style = "-fx-background-color: #1c1d2b; -fx-background-radius: 8; -fx-border-color: #2a2b3d; -fx-border-radius: 8;"
        }

        fun renderList(users: List<ru.bmstu.emk.domain.User>) {
            listBox.children.clear()
            for ((i, u) in users.withIndex()) {
                val roleName = when (u.role) { "STUDENT" -> "Ученик"; "TEACHER" -> "Преподаватель"; "ADMIN" -> "Администратор"; else -> u.role }
                val badgeClass = when (u.role) { "TEACHER" -> "badge-green"; "ADMIN" -> "badge-red"; else -> "badge-blue" }

                val loginLabel = Label(u.login).apply { style = "-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #dfe6ee;" }
                val roleLabel = Label(roleName).apply { styleClass.addAll("badge", badgeClass) }
                val idLabel = Label("ID: ${u.id}").apply { styleClass.add("label-muted") }

                val infoBox = VBox(4.0, HBox(10.0, loginLabel, roleLabel).apply { alignment = Pos.CENTER_LEFT }, idLabel)
                HBox.setHgrow(infoBox, Priority.ALWAYS)

                val buttonsBox = HBox(8.0).apply { alignment = Pos.CENTER_RIGHT }

                when (u.role) {
                    "STUDENT" -> {
                        buttonsBox.children.add(Button("Преподаватель").apply {
                            styleClass.addAll("button", "btn-primary", "btn-sm")
                            setOnAction { showPromoteForm(u.id, u.login) }
                        })
                        buttonsBox.children.add(Button("Админ").apply {
                            styleClass.addAll("button", "btn-sm")
                            style = "-fx-background-color: #c0392b; -fx-text-fill: white;"
                            setOnAction {
                                val ok = AdminService.promoteToAdmin(u.id)
                                if (ok) showUsers()
                            }
                        })
                    }
                    "TEACHER" -> {
                        buttonsBox.children.add(Button("В ученики").apply {
                            styleClass.addAll("button", "btn-danger", "btn-sm")
                            setOnAction {
                                val ok = AdminService.demoteToStudent(u.id)
                                if (!ok) {
                                    val alert = Alert(Alert.AlertType.WARNING,
                                        "Невозможно понизить: у преподавателя есть курсы", ButtonType.OK)
                                    alert.title = "Ошибка"; alert.showAndWait()
                                } else {
                                    showUsers()
                                }
                            }
                        })
                        buttonsBox.children.add(Button("Админ").apply {
                            styleClass.addAll("button", "btn-sm")
                            style = "-fx-background-color: #c0392b; -fx-text-fill: white;"
                            setOnAction {
                                val ok = AdminService.promoteToAdmin(u.id)
                                if (!ok) {
                                    val alert = Alert(Alert.AlertType.WARNING,
                                        "Невозможно назначить: у преподавателя есть курсы", ButtonType.OK)
                                    alert.title = "Ошибка"; alert.showAndWait()
                                } else {
                                    showUsers()
                                }
                            }
                        })
                    }
                    "ADMIN" -> {
                        buttonsBox.children.add(Button("В ученики").apply {
                            styleClass.addAll("button", "btn-danger", "btn-sm")
                            setOnAction {
                                val ok = AdminService.demoteToStudent(u.id)
                                if (ok) showUsers()
                            }
                        })
                        buttonsBox.children.add(Button("Преподаватель").apply {
                            styleClass.addAll("button", "btn-primary", "btn-sm")
                            setOnAction { showPromoteForm(u.id, u.login) }
                        })
                    }
                }

                val row = HBox(16.0, infoBox, buttonsBox).apply {
                    alignment = Pos.CENTER_LEFT
                    padding = Insets(12.0, 20.0, 12.0, 20.0)
                    if (i < users.size - 1) style = "-fx-border-color: #222336; -fx-border-width: 0 0 1 0;"
                }
                row.styleClass.add("list-row")
                listBox.children.add(row)
            }
            if (users.isEmpty()) {
                listBox.children.add(Label("Пользователи не найдены").apply {
                    styleClass.add("label-muted"); padding = Insets(20.0)
                })
            }
        }

        renderList(allUsers)

        searchField.textProperty().addListener { _, _, newVal ->
            val filtered = allUsers.filter { it.login.contains(newVal, ignoreCase = true) }
            renderList(filtered)
        }

        container.children.addAll(header, subtitle, searchField, listBox)
        contentArea.children.setAll(ScrollPane(container).apply { isFitToWidth = true; hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER })
    }

    private fun showPromoteForm(userId: Long, userLogin: String) {
        val container = VBox(16.0).apply { padding = Insets(32.0); maxWidth = 500.0 }
        val backBtn = Button("← Назад").apply { styleClass.addAll("button", "btn-ghost"); setOnAction { showUsers() } }
        val header = Label("Назначение преподавателем").apply { styleClass.add("label-title") }
        val info = Label("Пользователь: $userLogin").apply { styleClass.add("label-secondary") }

        val nameField = TextField().apply { promptText = "ФИО преподавателя (например, Иванов И.И.)" }
        val emailField = TextField().apply { promptText = "Email" }

        val errorLabel = Label().apply { style = "-fx-text-fill: #e17055; -fx-font-size: 13px;"; isVisible = false }

        val saveBtn = Button("Назначить").apply {
            styleClass.addAll("button", "btn-primary"); prefWidth = 300.0
            setOnAction {
                val name = nameField.text.trim()
                val email = emailField.text.trim()
                if (name.isEmpty()) { errorLabel.text = "Введите ФИО"; errorLabel.isVisible = true; return@setOnAction }
                if (email.isEmpty()) { errorLabel.text = "Введите email"; errorLabel.isVisible = true; return@setOnAction }
                val ok = AdminService.promoteToTeacher(userId, name, email)
                if (ok) showUsers()
                else { errorLabel.text = "Ошибка назначения"; errorLabel.isVisible = true }
            }
        }

        container.children.addAll(backBtn, header, info,
            Region().apply { prefHeight = 8.0 },
            Label("ФИО").apply { styleClass.add("label-heading") }, nameField,
            Label("Email").apply { styleClass.add("label-heading") }, emailField,
            errorLabel,
            Region().apply { prefHeight = 8.0 }, saveBtn)
        contentArea.children.setAll(container)
    }
}