package ru.bmstu.emk.ui

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.*
import ru.bmstu.emk.EmkApplication
import ru.bmstu.emk.domain.Course
import ru.bmstu.emk.service.*
import ru.bmstu.emk.util.SessionManager

class TeacherDashboard : HBox() {

    private val contentArea = StackPane()
    private var activeButton: Button? = null

    init {
        val sidebar = VBox().apply {
            styleClass.add("sidebar"); prefWidth = 240.0; minWidth = 240.0
        }

        val teacherName = SessionManager.currentTeacher?.fullName ?: SessionManager.currentUser?.login ?: ""
        val logoBox = VBox(4.0,
            Label("Преподаватель").apply { styleClass.add("label-heading") },
            Label(teacherName).apply { styleClass.add("label-muted"); isWrapText = true; maxWidth = 200.0 }
        ).apply {
            alignment = Pos.CENTER; padding = Insets(24.0, 16.0, 24.0, 16.0)
            style = "-fx-border-color: #3d3d5c; -fx-border-width: 0 0 1 0;"
        }

        val menuItems = listOf("Мои курсы", "Мои студенты", "Статистика", "Чат", "Профиль")
        val menuButtons = menuItems.map { label ->
            Button(label).apply {
                styleClass.addAll("sidebar-item")
                setOnAction {
                    setActiveButton(this)
                    when (label) {
                        "Мои курсы" -> showMyCourses()
                        "Мои студенты" -> showStudents()
                        "Статистика" -> showStats()
                        "Чат" -> showChat()
                        "Профиль" -> showProfile()
                    }
                }
            }
        }

        val logoutBtn = Button("Выйти").apply {
            styleClass.addAll("sidebar-item"); style = "-fx-text-fill: #f87171;"
            setOnAction { SessionManager.logout(); EmkApplication.navigateTo(LoginScreen()) }
        }
        val spacer = Region().apply { VBox.setVgrow(this, Priority.ALWAYS) }

        sidebar.children.addAll(logoBox); sidebar.children.addAll(menuButtons); sidebar.children.addAll(spacer, logoutBtn)
        contentArea.apply { HBox.setHgrow(this, Priority.ALWAYS) }
        children.addAll(sidebar, contentArea)

        setActiveButton(menuButtons[0]); showMyCourses()
    }

    private fun setActiveButton(btn: Button) {
        activeButton?.styleClass?.remove("sidebar-item-active"); btn.styleClass.add("sidebar-item-active"); activeButton = btn
    }

    private fun showMyCourses() {
        val teacherId = SessionManager.currentTeacher?.id ?: return
        val courses = CourseService.getCoursesForTeacher(teacherId)

        val container = VBox(20.0).apply { padding = Insets(32.0) }
        val header = Label("Мои курсы").apply { styleClass.add("label-title") }

        val addBtn = Button("+ Создать курс").apply {
            styleClass.addAll("button", "btn-primary")
            setOnAction { showCreateCourseForm() }
        }

        val topRow = HBox(16.0, header, Region().apply { HBox.setHgrow(this, Priority.ALWAYS) }, addBtn).apply {
            alignment = Pos.CENTER_LEFT
        }

        container.children.add(topRow)

        if (courses.isEmpty()) {
            container.children.add(VBox(12.0,
                Label("У вас пока нет курсов").apply { styleClass.add("label-secondary") }
            ).apply { alignment = Pos.CENTER; padding = Insets(60.0) })
        } else {
            val grid = FlowPane(16.0, 16.0)
            for (course in courses) {
                grid.children.add(createTeacherCourseCard(course))
            }
            container.children.add(grid)
        }

        val scrollPane = ScrollPane(container).apply { isFitToWidth = true; hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER }
        contentArea.children.setAll(scrollPane)
    }

    private fun createTeacherCourseCard(course: Course): VBox {
        val name = Label(course.name).apply { styleClass.add("label-heading"); isWrapText = true }
        val desc = Label(course.description).apply { styleClass.add("label-secondary"); isWrapText = true; maxWidth = 260.0 }
        val hosting = Label("Платформа: ${course.hosting.name}").apply { styleClass.add("label-muted") }
        val duration = Label("${course.duration} ч.").apply { styleClass.add("label-muted") }

        val editBtn = Button("Редактировать").apply {
            styleClass.addAll("button"); maxWidth = Double.MAX_VALUE
            setOnAction { showEditCourse(course.id) }
        }
        val deleteBtn = Button("Удалить").apply {
            styleClass.addAll("button", "btn-danger"); maxWidth = Double.MAX_VALUE
            setOnAction {
                val alert = Alert(Alert.AlertType.CONFIRMATION, "Удалить курс «${course.name}»?\nВсе модули, уроки и треки прохождения будут удалены.", ButtonType.YES, ButtonType.NO)
                alert.title = "Подтверждение удаления"
                if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                    TeacherService.deleteCourse(course.id)
                    showMyCourses()
                }
            }
        }

        return VBox(8.0, name, desc, hosting, duration, Region().apply { prefHeight = 4.0 }, editBtn, deleteBtn).apply {
            styleClass.add("card"); prefWidth = 300.0; padding = Insets(20.0)
        }
    }

    private fun showCreateCourseForm() {
        val hostings = TeacherService.getAllHostings()
        val container = VBox(16.0).apply { padding = Insets(32.0); maxWidth = 500.0 }

        val backBtn = Button("← Назад").apply { styleClass.addAll("button", "btn-ghost"); setOnAction { showMyCourses() } }
        val header = Label("Создание нового курса").apply { styleClass.add("label-title") }

        val nameField = TextField().apply { promptText = "Название курса" }
        val descField = TextField().apply { promptText = "Описание" }
        val durationField = TextField().apply { promptText = "Длительность (часы)" }
        val hostingCombo = ComboBox<String>().apply {
            items.addAll(hostings.map { it.name }); selectionModel.selectFirst(); maxWidth = Double.MAX_VALUE
        }

        val saveBtn = Button("Создать курс").apply {
            styleClass.addAll("button", "btn-primary"); prefWidth = 300.0
            setOnAction {
                val n = nameField.text.trim(); val d = descField.text.trim()
                val dur = durationField.text.trim().toIntOrNull() ?: 0
                val hIdx = hostingCombo.selectionModel.selectedIndex
                if (n.isEmpty()) return@setOnAction
                val teacherId = SessionManager.currentTeacher?.id ?: return@setOnAction
                TeacherService.createCourse(n, d, dur, teacherId, hostings[hIdx].id)
                showMyCourses()
            }
        }

        container.children.addAll(backBtn, header,
            Label("Название").apply { styleClass.add("label-heading") }, nameField,
            Label("Описание").apply { styleClass.add("label-heading") }, descField,
            Label("Длительность (ч.)").apply { styleClass.add("label-heading") }, durationField,
            Label("Хостинг").apply { styleClass.add("label-heading") }, hostingCombo,
            Region().apply { prefHeight = 8.0 }, saveBtn
        )
        contentArea.children.setAll(ScrollPane(container).apply { isFitToWidth = true })
    }

    private fun showEditCourse(courseId: Long) {
        val course = CourseService.getCourseWithModules(courseId) ?: return
        val hostings = TeacherService.getAllHostings()

        val container = VBox(16.0).apply { padding = Insets(32.0) }
        val backBtn = Button("← Назад к моим курсам").apply { styleClass.addAll("button", "btn-ghost"); setOnAction { showMyCourses() } }
        val header = Label(course.name).apply { styleClass.add("label-title") }

        // Редактирование основных полей
        val nameField = TextField(course.name)
        val descField = TextField(course.description)
        val durationField = TextField(course.duration.toString())
        val hostingCombo = ComboBox<String>().apply {
            items.addAll(hostings.map { it.name })
            val idx = hostings.indexOfFirst { it.id == course.hosting.id }
            selectionModel.select(if (idx >= 0) idx else 0); maxWidth = Double.MAX_VALUE
        }

        val updateBtn = Button("Сохранить изменения").apply {
            styleClass.addAll("button", "btn-primary")
            setOnAction {
                val hIdx = hostingCombo.selectionModel.selectedIndex
                TeacherService.updateCourse(courseId, nameField.text.trim(), descField.text.trim(), durationField.text.trim().toIntOrNull() ?: 0, hostings[hIdx].id)
                showEditCourse(courseId)
            }
        }

        val infoBox = VBox(12.0,
            Label("Название").apply { styleClass.add("label-heading") }, nameField,
            Label("Описание").apply { styleClass.add("label-heading") }, descField,
            HBox(16.0,
                VBox(4.0, Label("Длительность (ч.)").apply { styleClass.add("label-heading") }, durationField),
                VBox(4.0, Label("Хостинг").apply { styleClass.add("label-heading") }, hostingCombo)
            ), updateBtn
        ).apply { styleClass.add("card-static"); padding = Insets(20.0) }

        container.children.addAll(backBtn, header, infoBox)

        // Модули и уроки
        container.children.add(Label("Модули и уроки").apply { styleClass.add("label-subtitle"); padding = Insets(12.0, 0.0, 4.0, 0.0) })

        // Inline-форма добавления модуля
        val addModuleField = TextField().apply { promptText = "Название нового модуля"; HBox.setHgrow(this, Priority.ALWAYS) }
        val addModuleBtn = Button("Добавить модуль").apply {
            styleClass.addAll("button", "btn-primary"); style = "-fx-padding: 6 16; -fx-font-size: 13px;"
            setOnAction {
                val name = addModuleField.text.trim()
                if (name.isNotBlank()) { TeacherService.addModule(courseId, name); showEditCourse(courseId) }
            }
        }
        addModuleField.setOnAction { addModuleBtn.fire() }
        val addModuleRow = HBox(8.0, addModuleField, addModuleBtn).apply {
            alignment = Pos.CENTER_LEFT; padding = Insets(0.0, 0.0, 8.0, 0.0)
        }
        container.children.add(addModuleRow)

        for (module in course.modules) {
            val moduleBox = VBox(8.0).apply { styleClass.add("card-static"); padding = Insets(16.0) }

            val moduleHeader = HBox(12.0,
                Label(module.name).apply { styleClass.add("label-heading") },
                Region().apply { HBox.setHgrow(this, Priority.ALWAYS) },
                Button("X").apply {
                    styleClass.addAll("button", "btn-danger"); style = "-fx-padding: 4 10; -fx-font-size: 12px;"
                    setOnAction {
                        val a = Alert(Alert.AlertType.CONFIRMATION, "Удалить модуль «${module.name}» и все его уроки?", ButtonType.YES, ButtonType.NO)
                        if (a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) { TeacherService.deleteModule(module.id); showEditCourse(courseId) }
                    }
                }
            ).apply { alignment = Pos.CENTER_LEFT }

            moduleBox.children.add(moduleHeader)

            for (lesson in module.lessons) {
                val lessonRow = HBox(12.0,
                    Label(lesson.name).apply { styleClass.add("label") },
                    Label(lesson.type).apply { styleClass.add("label-muted") },
                    Region().apply { HBox.setHgrow(this, Priority.ALWAYS) },
                    Button("X").apply {
                        styleClass.addAll("button", "btn-danger"); style = "-fx-padding: 2 8; -fx-font-size: 11px;"
                        setOnAction { TeacherService.deleteLesson(lesson.id); showEditCourse(courseId) }
                    }
                ).apply { alignment = Pos.CENTER_LEFT; padding = Insets(4.0, 8.0, 4.0, 24.0) }
                moduleBox.children.add(lessonRow)
            }

            // Inline-форма добавления урока внутри модуля
            val addLessonField = TextField().apply { promptText = "Название урока"; HBox.setHgrow(this, Priority.ALWAYS) }
            val typeCombo = ComboBox<String>().apply { items.addAll("video", "pdf", "text"); selectionModel.selectFirst(); prefWidth = 90.0 }
            val addLessonBtn = Button("Добавить").apply {
                styleClass.addAll("button"); style = "-fx-padding: 4 12; -fx-font-size: 12px;"
                setOnAction {
                    val name = addLessonField.text.trim()
                    if (name.isNotBlank()) {
                        TeacherService.addLesson(module.id, name, typeCombo.value)
                        showEditCourse(courseId)
                    }
                }
            }
            addLessonField.setOnAction { addLessonBtn.fire() }
            val addLessonRow = HBox(6.0, addLessonField, typeCombo, addLessonBtn).apply {
                alignment = Pos.CENTER_LEFT; padding = Insets(6.0, 8.0, 2.0, 24.0)
            }
            moduleBox.children.add(addLessonRow)

            container.children.add(moduleBox)
        }

        contentArea.children.setAll(ScrollPane(container).apply { isFitToWidth = true; hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER })
    }

    private fun showStudents() {
        val teacherId = SessionManager.currentTeacher?.id ?: return
        val students = TeacherService.getStudentsForTeacher(teacherId)

        val container = VBox(20.0).apply { padding = Insets(32.0) }
        val header = Label("Мои студенты").apply { styleClass.add("label-title") }
        val subtitle = Label("Студенты, записанные на ваши курсы: ${students.size}").apply { styleClass.add("label-secondary") }

        val searchField = TextField().apply { promptText = "Поиск по студенту или курсу..."; prefWidth = 300.0 }

        val table = TableView<StudentProgress>().apply {
            prefHeight = 500.0
            val loginCol = TableColumn<StudentProgress, String>("Студент").apply {
                setCellValueFactory { javafx.beans.property.SimpleStringProperty(it.value.login) }; prefWidth = 150.0
            }
            val courseCol = TableColumn<StudentProgress, String>("Курс").apply {
                setCellValueFactory { javafx.beans.property.SimpleStringProperty(it.value.courseName) }; prefWidth = 250.0
            }
            val statusCol = TableColumn<StudentProgress, String>("Статус").apply {
                setCellValueFactory { javafx.beans.property.SimpleStringProperty(it.value.status) }; prefWidth = 110.0
            }
            val progressCol = TableColumn<StudentProgress, String>("Прогресс").apply {
                setCellValueFactory { javafx.beans.property.SimpleStringProperty("${it.value.completedLessons}/${it.value.totalLessons} (${String.format("%.0f", it.value.progressPercent)}%)") }; prefWidth = 150.0
            }
            val dateCol = TableColumn<StudentProgress, String>("Дата записи").apply {
                setCellValueFactory { javafx.beans.property.SimpleStringProperty(it.value.startDate) }; prefWidth = 120.0
            }
            columns.addAll(loginCol, courseCol, statusCol, progressCol, dateCol)
            items.addAll(students)
        }

        searchField.textProperty().addListener { _, _, newVal ->
            val filtered = students.filter {
                it.login.contains(newVal, ignoreCase = true) || it.courseName.contains(newVal, ignoreCase = true)
            }
            table.items.setAll(filtered)
        }

        container.children.addAll(header, subtitle, searchField, table)
        contentArea.children.setAll(ScrollPane(container).apply { isFitToWidth = true })
    }

    private fun showStats() {
        val teacherId = SessionManager.currentTeacher?.id ?: return
        val courses = CourseService.getCoursesForTeacher(teacherId)
        val students = TeacherService.getStudentsForTeacher(teacherId)

        val container = VBox(20.0).apply { padding = Insets(32.0) }
        val header = Label("Статистика").apply { styleClass.add("label-title") }

        val totalStudents = students.map { it.login }.distinct().size
        val finishedCount = students.count { it.status == "завершён" }
        val avgProgress = if (students.isNotEmpty()) students.map { it.progressPercent }.average() else 0.0

        fun statCard(value: String, label: String): VBox {
            return VBox(4.0,
                Label(value).apply { styleClass.add("stat-value") },
                Label(label).apply { styleClass.add("stat-label") }
            ).apply { styleClass.add("card-static"); alignment = Pos.CENTER; prefWidth = 200.0; padding = Insets(20.0) }
        }

        val statsBox = HBox(24.0,
            statCard("${courses.size}", "Моих курсов"),
            statCard("$totalStudents", "Уникальных студентов"),
            statCard("$finishedCount", "Завершили курс"),
            statCard("${String.format("%.0f", avgProgress)}%", "Средний прогресс")
        ).apply { alignment = Pos.CENTER_LEFT }

        // Прогресс по курсам
        val courseStatsLabel = Label("По курсам").apply { styleClass.add("label-subtitle"); padding = Insets(12.0, 0.0, 0.0, 0.0) }

        val courseStatsBox = VBox(12.0)
        for (course in courses) {
            val courseStudents = students.filter { it.courseName == course.name }
            val enrolled = courseStudents.size
            val finished = courseStudents.count { it.status == "завершён" }
            val avg = if (courseStudents.isNotEmpty()) courseStudents.map { it.progressPercent }.average() else 0.0

            val row = HBox(16.0,
                Label(course.name).apply { styleClass.add("label-heading"); prefWidth = 300.0; isWrapText = true },
                Label("Записано: $enrolled").apply { styleClass.add("label-secondary") },
                Label("Завершили: $finished").apply { styleClass.add("label-secondary") },
                ProgressBar(avg / 100.0).apply { prefWidth = 150.0; prefHeight = 8.0 },
                Label("${String.format("%.0f", avg)}%").apply { styleClass.add("label-muted") }
            ).apply { alignment = Pos.CENTER_LEFT; styleClass.add("card-static"); padding = Insets(12.0) }
            courseStatsBox.children.add(row)
        }

        container.children.addAll(header, statsBox, courseStatsLabel, courseStatsBox)
        contentArea.children.setAll(ScrollPane(container).apply { isFitToWidth = true; hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER })
    }

    // ===================== ЧАТ =====================
    private fun showChat() {
        val teacherUserId = SessionManager.currentUser?.id ?: return
        val teacherId = SessionManager.currentTeacher?.id ?: return
        val students = TeacherService.getStudentsForTeacher(teacherId)
        val uniqueStudents = students.map { it.login }.distinct()

        val container = HBox(0.0)
        container.prefHeight = Double.MAX_VALUE

        val studentList = VBox(0.0).apply {
            prefWidth = 260.0; minWidth = 260.0
            style = "-fx-background-color: #1c1d2b; -fx-border-color: #2a2b3d; -fx-border-width: 0 1 0 0;"
        }
        studentList.children.add(Label("Чат").apply {
            styleClass.add("label-subtitle"); padding = Insets(16.0)
        })

        val chatArea = VBox().apply { HBox.setHgrow(this, Priority.ALWAYS) }

        if (uniqueStudents.isEmpty()) {
            chatArea.children.add(VBox(8.0,
                Label("Пока нет студентов для переписки").apply { styleClass.add("label-secondary") }
            ).apply { alignment = Pos.CENTER; padding = Insets(60.0) })
        } else {
            val studentUsers = AdminService.getAllStudents()
            val relevantStudents = studentUsers.filter { it.login in uniqueStudents }

            // Собираем инфо: непрочитанные + время последнего сообщения
            data class StudentContact(val id: Long, val login: String, val unread: Int, val lastTime: java.time.LocalDateTime?)
            val contacts = relevantStudents.map { s ->
                val unread = MessageService.getUnreadCount(teacherUserId, s.id)
                val lastTime = MessageService.getLastMessageTime(teacherUserId, s.id)
                StudentContact(s.id, s.login, unread, lastTime)
            }.sortedWith(compareByDescending<StudentContact> { it.unread > 0 }.thenByDescending { it.lastTime })

            var activeContactBtn: Button? = null

            for (contact in contacts) {
                val nameLabel = Label(contact.login).apply {
                    style = if (contact.unread > 0) "-fx-text-fill: #dfe6ee; -fx-font-weight: bold;" else "-fx-text-fill: #5f6a7a;"
                    HBox.setHgrow(this, Priority.ALWAYS); maxWidth = Double.MAX_VALUE
                }
                val row = HBox(8.0).apply {
                    alignment = Pos.CENTER_LEFT; padding = Insets(0.0)
                    children.add(nameLabel)
                }
                if (contact.unread > 0) {
                    row.children.add(Label("${contact.unread}").apply { styleClass.add("unread-badge") })
                }

                val btn = Button().apply {
                    graphic = row; styleClass.add("chat-contact"); maxWidth = Double.MAX_VALUE
                    contentDisplay = javafx.scene.control.ContentDisplay.GRAPHIC_ONLY
                    setOnAction {
                        activeContactBtn?.styleClass?.remove("chat-contact-active")
                        styleClass.add("chat-contact-active")
                        activeContactBtn = this
                        MessageService.markAsRead(teacherUserId, contact.id)
                        // Убираем бейдж непрочитанных после открытия
                        nameLabel.style = "-fx-text-fill: #dfe6ee;"
                        row.children.removeIf { it is Label && it.styleClass.contains("unread-badge") }
                        showChatWith(teacherUserId, contact.id, contact.login, chatArea)
                    }
                }
                studentList.children.add(btn)
            }
            // Открываем первый чат по умолчанию
            if (contacts.isNotEmpty()) {
                val first = contacts[0]
                MessageService.markAsRead(teacherUserId, first.id)
                showChatWith(teacherUserId, first.id, first.login, chatArea)
                // Активируем первую кнопку
                val firstBtn = studentList.children.filterIsInstance<Button>().firstOrNull { it.styleClass.contains("chat-contact") }
                firstBtn?.styleClass?.add("chat-contact-active")
                activeContactBtn = firstBtn
            }
        }

        container.children.addAll(studentList, chatArea)
        contentArea.children.setAll(container)
    }

    private fun showChatWith(myUserId: Long, otherUserId: Long, otherName: String, chatArea: VBox) {
        chatArea.children.clear()
        MessageService.markAsRead(myUserId, otherUserId)

        val header = Label(otherName).apply {
            styleClass.add("label-subtitle"); padding = Insets(12.0, 16.0, 12.0, 16.0)
            style = "-fx-border-color: #2a2b3d; -fx-border-width: 0 0 1 0; -fx-background-color: #1c1d2b;"
            maxWidth = Double.MAX_VALUE
        }

        val messagesBox = VBox(8.0).apply { padding = Insets(16.0) }

        fun loadMessages() {
            messagesBox.children.clear()
            val messages = MessageService.getMessages(myUserId, otherUserId)
            for (msg in messages) {
                val isMe = msg.senderId == myUserId
                val bubble = VBox(2.0,
                    Label(msg.text).apply {
                        isWrapText = true; maxWidth = 400.0
                        style = if (isMe) "-fx-background-color: #6c5ce7; -fx-text-fill: white; -fx-padding: 8 12; -fx-background-radius: 8;"
                        else "-fx-background-color: #252636; -fx-text-fill: #dfe6ee; -fx-padding: 8 12; -fx-background-radius: 8;"
                    },
                    Label(msg.timestamp.toString().substring(0, 16)).apply { style = "-fx-font-size: 11px; -fx-text-fill: #3a3b50;" }
                ).apply { alignment = if (isMe) Pos.CENTER_RIGHT else Pos.CENTER_LEFT }
                messagesBox.children.add(bubble)
            }
        }

        loadMessages()

        val scrollPane = ScrollPane(messagesBox).apply {
            isFitToWidth = true; hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
            VBox.setVgrow(this, Priority.ALWAYS)
        }

        val inputField = TextField().apply { promptText = "Введите сообщение..."; HBox.setHgrow(this, Priority.ALWAYS) }
        val sendBtn = Button("Отправить").apply {
            styleClass.addAll("button", "btn-primary")
            setOnAction {
                val text = inputField.text.trim()
                if (text.isNotEmpty()) {
                    MessageService.sendMessage(myUserId, otherUserId, text)
                    inputField.clear()
                    loadMessages()
                    scrollPane.vvalue = 1.0
                }
            }
        }
        inputField.setOnAction { sendBtn.fire() }

        val inputBox = HBox(8.0, inputField, sendBtn).apply {
            padding = Insets(12.0, 16.0, 12.0, 16.0)
            style = "-fx-border-color: #2a2b3d; -fx-border-width: 1 0 0 0; -fx-background-color: #1c1d2b;"
        }

        chatArea.children.addAll(header, scrollPane, inputBox)
    }

    // ===================== ПРОФИЛЬ =====================
    private fun showProfile() {
        val user = SessionManager.currentUser ?: return
        val teacher = SessionManager.currentTeacher

        val container = VBox(20.0).apply { padding = Insets(32.0); maxWidth = 500.0 }
        val header = Label("Профиль").apply { styleClass.add("label-title") }
        val roleLabel = Label("Роль: Преподаватель").apply { styleClass.add("label-secondary") }

        // Смена логина
        val loginSection = Label("Логин").apply { styleClass.add("label-subtitle"); padding = Insets(12.0, 0.0, 0.0, 0.0) }
        val loginField = TextField(user.login).apply { promptText = "Логин" }
        val loginError = Label().apply { style = "-fx-text-fill: #e17055; -fx-font-size: 13px;"; isVisible = false }
        val loginSuccess = Label().apply { style = "-fx-text-fill: #00b894; -fx-font-size: 13px;"; isVisible = false }

        val saveLoginBtn = Button("Сохранить логин").apply {
            styleClass.addAll("button", "btn-primary"); prefWidth = 300.0
            setOnAction {
                loginError.isVisible = false; loginSuccess.isVisible = false
                val newLogin = loginField.text.trim()
                if (newLogin.isEmpty()) { loginError.text = "Логин не может быть пустым"; loginError.isVisible = true; return@setOnAction }
                if (newLogin.length < 3) { loginError.text = "Логин — минимум 3 символа"; loginError.isVisible = true; return@setOnAction }
                if (newLogin == user.login) { loginSuccess.text = "Логин не изменился"; loginSuccess.isVisible = true; return@setOnAction }
                val ok = AuthService.changeLogin(user.id, newLogin)
                if (ok) {
                    user.login = newLogin
                    loginSuccess.text = "Логин изменён"; loginSuccess.isVisible = true
                } else {
                    loginError.text = "Этот логин уже занят"; loginError.isVisible = true
                }
            }
        }

        // Редактирование ФИО и Email
        val infoSection = Label("Личные данные").apply { styleClass.add("label-subtitle"); padding = Insets(16.0, 0.0, 0.0, 0.0) }
        val nameField = TextField(teacher?.fullName ?: "").apply { promptText = "ФИО" }
        val emailField = TextField(teacher?.email ?: "").apply { promptText = "Email" }
        val infoError = Label().apply { style = "-fx-text-fill: #e17055; -fx-font-size: 13px;"; isVisible = false }
        val infoSuccess = Label().apply { style = "-fx-text-fill: #00b894; -fx-font-size: 13px;"; isVisible = false }

        val saveInfoBtn = Button("Сохранить данные").apply {
            styleClass.addAll("button", "btn-primary"); prefWidth = 300.0
            setOnAction {
                infoError.isVisible = false; infoSuccess.isVisible = false
                val name = nameField.text.trim()
                val email = emailField.text.trim()
                if (name.isEmpty()) { infoError.text = "Введите ФИО"; infoError.isVisible = true; return@setOnAction }
                if (email.isEmpty()) { infoError.text = "Введите email"; infoError.isVisible = true; return@setOnAction }
                if (teacher != null) {
                    val ok = AuthService.updateTeacherInfo(teacher.id, name, email)
                    if (ok) {
                        teacher.fullName = name; teacher.email = email
                        infoSuccess.text = "Данные сохранены"; infoSuccess.isVisible = true
                    } else {
                        infoError.text = "Ошибка сохранения"; infoError.isVisible = true
                    }
                }
            }
        }

        // Смена пароля
        val passSection = Label("Смена пароля").apply { styleClass.add("label-subtitle"); padding = Insets(16.0, 0.0, 0.0, 0.0) }
        val oldPassField = PasswordField().apply { promptText = "Текущий пароль" }
        val newPassField = PasswordField().apply { promptText = "Новый пароль" }
        val confirmPassField = PasswordField().apply { promptText = "Подтверждение нового пароля" }
        val passError = Label().apply { style = "-fx-text-fill: #e17055; -fx-font-size: 13px;"; isVisible = false }
        val passSuccess = Label().apply { style = "-fx-text-fill: #00b894; -fx-font-size: 13px;"; isVisible = false }

        val changePassBtn = Button("Сменить пароль").apply {
            styleClass.addAll("button", "btn-primary"); prefWidth = 300.0
            setOnAction {
                passError.isVisible = false; passSuccess.isVisible = false
                val oldP = oldPassField.text.trim()
                val newP = newPassField.text.trim()
                val confP = confirmPassField.text.trim()
                if (oldP.isEmpty() || newP.isEmpty()) { passError.text = "Заполните все поля"; passError.isVisible = true; return@setOnAction }
                if (newP.length < 4) { passError.text = "Новый пароль — минимум 4 символа"; passError.isVisible = true; return@setOnAction }
                if (newP != confP) { passError.text = "Пароли не совпадают"; passError.isVisible = true; return@setOnAction }
                val ok = AuthService.changePassword(user.id, oldP, newP)
                if (ok) {
                    passSuccess.text = "Пароль успешно изменён"; passSuccess.isVisible = true
                    oldPassField.clear(); newPassField.clear(); confirmPassField.clear()
                } else {
                    passError.text = "Неверный текущий пароль"; passError.isVisible = true
                }
            }
        }

        container.children.addAll(
            header, roleLabel,
            loginSection,
            Label("Логин").apply { styleClass.add("label-heading") }, loginField,
            loginError, loginSuccess, saveLoginBtn,
            infoSection,
            Label("ФИО").apply { styleClass.add("label-heading") }, nameField,
            Label("Email").apply { styleClass.add("label-heading") }, emailField,
            infoError, infoSuccess, saveInfoBtn,
            passSection,
            Label("Текущий пароль").apply { styleClass.add("label-heading") }, oldPassField,
            Label("Новый пароль").apply { styleClass.add("label-heading") }, newPassField,
            Label("Подтверждение").apply { styleClass.add("label-heading") }, confirmPassField,
            passError, passSuccess,
            Region().apply { prefHeight = 4.0 },
            changePassBtn
        )
        contentArea.children.setAll(ScrollPane(container).apply { isFitToWidth = true; hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER })
    }
}