package ru.bmstu.emk.ui

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.*
import ru.bmstu.emk.EmkApplication
import ru.bmstu.emk.domain.Course
import ru.bmstu.emk.service.AuthService
import ru.bmstu.emk.service.CourseService
import ru.bmstu.emk.service.MessageService
import ru.bmstu.emk.service.ProgressService
import ru.bmstu.emk.service.TrackInfo
import ru.bmstu.emk.util.SessionManager

class StudentDashboard : HBox() {

    private val contentArea = StackPane()
    private var activeButton: Button? = null

    init {
        val sidebar = createSidebar()
        contentArea.apply { HBox.setHgrow(this, Priority.ALWAYS) }
        children.addAll(sidebar, contentArea)
    }

    private fun createSidebar(): VBox {
        val sidebar = VBox(2.0).apply {
            styleClass.add("sidebar"); prefWidth = 220.0; minWidth = 220.0; padding = Insets(0.0)
        }

        val logoBox = VBox(2.0,
            Label("АИС ЭМК").apply { style = "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #dfe6ee;" },
            Label(SessionManager.currentUser?.login ?: "").apply { style = "-fx-font-size: 13px; -fx-text-fill: #5f6a7a;" }
        ).apply { padding = Insets(20.0, 16.0, 16.0, 16.0); style = "-fx-border-color: #2a2b3d; -fx-border-width: 0 0 1 0;" }

        val menuBox = VBox(2.0).apply { padding = Insets(8.0) }
        val items = listOf("Мои курсы", "Каталог", "Прогресс", "Чат", "Профиль")
        val buttons = items.map { label ->
            Button(label).apply {
                styleClass.add("sidebar-item"); maxWidth = Double.MAX_VALUE
                setOnAction {
                    setActiveBtn(this)
                    when (label) {
                        "Мои курсы" -> showMyCourses()
                        "Каталог" -> showCatalog()
                        "Прогресс" -> showProgress()
                        "Чат" -> showChat()
                        "Профиль" -> showProfile()
                    }
                }
            }
        }
        menuBox.children.addAll(buttons)

        val spacer = Region().apply { VBox.setVgrow(this, Priority.ALWAYS) }
        val logoutBtn = Button("Выйти").apply {
            styleClass.add("sidebar-item"); maxWidth = Double.MAX_VALUE; style = "-fx-text-fill: #e17055;"
            setOnAction { SessionManager.logout(); EmkApplication.navigateTo(LoginScreen()) }
        }
        val bottomBox = VBox(logoutBtn).apply { padding = Insets(8.0) }

        sidebar.children.addAll(logoBox, menuBox, spacer, bottomBox)
        setActiveBtn(buttons[0]); showMyCourses()
        return sidebar
    }

    private fun setActiveBtn(btn: Button) {
        activeButton?.styleClass?.remove("sidebar-item-active"); btn.styleClass.add("sidebar-item-active"); activeButton = btn
    }

    private fun setContent(node: javafx.scene.Node) { contentArea.children.setAll(node) }

    // ===================== МОИ КУРСЫ =====================
    private fun showMyCourses() {
        val userId = SessionManager.currentUser?.id ?: return
        val tracks = ProgressService.getTracksForUser(userId)

        val container = VBox(0.0).apply { padding = Insets(24.0, 32.0, 24.0, 32.0) }
        container.children.add(Label("Мои курсы").apply { styleClass.add("page-title"); padding = Insets(0.0, 0.0, 16.0, 0.0) })

        if (tracks.isEmpty()) {
            container.children.add(VBox(8.0,
                Label("Вы пока не записаны ни на один курс").apply { styleClass.add("label-secondary") },
                Button("Открыть каталог").apply { styleClass.addAll("button", "btn-primary"); setOnAction { showCatalog() } }
            ).apply { padding = Insets(40.0, 0.0, 0.0, 0.0) })
        } else {
            val active = tracks.filter { !it.isFinished }
            val finished = tracks.filter { it.isFinished }

            if (active.isNotEmpty()) {
                container.children.add(Label("Активные").apply { styleClass.add("label-subtitle"); padding = Insets(0.0, 0.0, 8.0, 0.0) })
                val activeBox = VBox(0.0).apply {
                    style = "-fx-background-color: #1c1d2b; -fx-background-radius: 8; -fx-border-color: #2a2b3d; -fx-border-radius: 8;"
                }
                for ((i, info) in active.withIndex()) {
                    activeBox.children.add(createCourseRow(info, i < active.size - 1))
                }
                container.children.add(activeBox)
            }

            if (finished.isNotEmpty()) {
                container.children.add(Region().apply { prefHeight = 20.0 })
                val toggleBtn = Button("Завершённые (${finished.size})").apply {
                    styleClass.addAll("button", "btn-ghost")
                    style = "-fx-text-fill: #5f6a7a; -fx-font-size: 13px;"
                }
                val finishedBox = VBox(0.0).apply {
                    style = "-fx-background-color: #1c1d2b; -fx-background-radius: 8; -fx-border-color: #2a2b3d; -fx-border-radius: 8;"
                    isVisible = false; isManaged = false
                }
                for ((i, info) in finished.withIndex()) {
                    finishedBox.children.add(createCourseRow(info, i < finished.size - 1))
                }
                toggleBtn.setOnAction {
                    finishedBox.isVisible = !finishedBox.isVisible
                    finishedBox.isManaged = finishedBox.isVisible
                    toggleBtn.text = if (finishedBox.isVisible) "Скрыть завершённые" else "Завершённые (${finished.size})"
                }
                container.children.addAll(toggleBtn, finishedBox)
            }
        }

        setContent(ScrollPane(container).apply { isFitToWidth = true; hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER })
    }

    private fun createCourseRow(info: TrackInfo, showBorder: Boolean): HBox {
        val row = HBox(12.0).apply {
            alignment = Pos.CENTER_LEFT; padding = Insets(12.0, 16.0, 12.0, 16.0); cursor = javafx.scene.Cursor.HAND
            if (showBorder) style = "-fx-border-color: #222336; -fx-border-width: 0 0 1 0;"
            setOnMouseClicked { showCourseDetail(info.course.id) }
        }
        row.setOnMouseEntered { row.style = (if (showBorder) "-fx-border-color: #222336; -fx-border-width: 0 0 1 0;" else "") + "-fx-background-color: #2a2b3d; -fx-background-radius: 0;" }
        row.setOnMouseExited { row.style = if (showBorder) "-fx-border-color: #222336; -fx-border-width: 0 0 1 0;" else "" }

        val nameBox = VBox(2.0,
            Label(info.course.name).apply { style = "-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #dfe6ee;" },
            Label("${info.course.teacher.fullName} · ${info.course.hosting.name}").apply { style = "-fx-font-size: 13px; -fx-text-fill: #5f6a7a;" }
        ).apply { HBox.setHgrow(this, Priority.ALWAYS) }

        val progressBox = VBox(3.0,
            ProgressBar(info.progressPercent / 100.0).apply { prefWidth = 140.0; prefHeight = 6.0 },
            Label("${info.completedCount}/${info.totalLessons}").apply { style = "-fx-font-size: 12px; -fx-text-fill: #5f6a7a;" }
        ).apply { alignment = Pos.CENTER_RIGHT }

        val badge = Label(if (info.isFinished) "завершён" else "в процессе").apply {
            styleClass.addAll("badge", if (info.isFinished) "badge-green" else "badge-blue")
        }

        row.children.addAll(nameBox, progressBox, badge)
        return row
    }

    // ===================== КАТАЛОГ =====================
    private fun showCatalog() {
        val container = VBox(0.0).apply { padding = Insets(24.0, 32.0, 24.0, 32.0) }
        container.children.add(Label("Каталог курсов").apply { styleClass.add("page-title"); padding = Insets(0.0, 0.0, 12.0, 0.0) })

        val searchField = TextField().apply { promptText = "Поиск..."; prefWidth = 300.0 }
        container.children.add(HBox(searchField).apply { padding = Insets(0.0, 0.0, 12.0, 0.0) })

        val listBox = VBox(0.0).apply {
            style = "-fx-background-color: #1c1d2b; -fx-background-radius: 8; -fx-border-color: #2a2b3d; -fx-border-radius: 8;"
        }

        val userId = SessionManager.currentUser?.id ?: 0

        fun loadCourses(query: String = "") {
            val courses = if (query.isBlank()) CourseService.getAllCourses() else CourseService.searchCourses(query)
            listBox.children.clear()
            for ((i, course) in courses.withIndex()) {
                val enrolled = ProgressService.isEnrolled(userId, course.id)
                listBox.children.add(createCatalogRow(course, enrolled, i < courses.size - 1))
            }
        }

        searchField.textProperty().addListener { _, _, v -> loadCourses(v) }
        loadCourses()

        container.children.add(listBox)
        setContent(ScrollPane(container).apply { isFitToWidth = true; hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER })
    }

    private fun createCatalogRow(course: Course, enrolled: Boolean, showBorder: Boolean): HBox {
        val row = HBox(12.0).apply {
            alignment = Pos.CENTER_LEFT; padding = Insets(10.0, 16.0, 10.0, 16.0)
            if (showBorder) style = "-fx-border-color: #222336; -fx-border-width: 0 0 1 0;"
        }

        val nameBox = VBox(2.0,
            Label(course.name).apply { style = "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #dfe6ee;" },
            Label("${course.teacher.fullName} · ${course.hosting.name} · ${course.duration}ч").apply { style = "-fx-font-size: 12px; -fx-text-fill: #5f6a7a;" }
        ).apply { HBox.setHgrow(this, Priority.ALWAYS) }

        val actionBtn = if (enrolled) {
            Button("Открыть").apply {
                styleClass.addAll("button", "btn-sm"); style = "-fx-background-color: #00a381; -fx-text-fill: white; -fx-padding: 4 10; -fx-font-size: 13px;"
                setOnAction { showCourseDetail(course.id) }
            }
        } else {
            Button("Записаться").apply {
                styleClass.addAll("button", "btn-sm", "btn-primary"); style = "-fx-padding: 4 10; -fx-font-size: 13px;"
                setOnAction {
                    val ok = ProgressService.enroll(SessionManager.currentUser?.id ?: 0, course.id)
                    if (ok) { text = "Записан"; isDisable = true; style = "-fx-background-color: #00a381; -fx-text-fill: white; -fx-padding: 4 10; -fx-font-size: 13px;" }
                }
            }
        }

        row.children.addAll(nameBox, actionBtn)
        return row
    }

    // ===================== ПРОГРЕСС =====================
    private fun showProgress() {
        val userId = SessionManager.currentUser?.id ?: return
        val tracks = ProgressService.getTracksForUser(userId)

        val container = VBox(20.0).apply { padding = Insets(24.0, 32.0, 24.0, 32.0) }
        container.children.add(Label("Прогресс").apply { styleClass.add("page-title") })

        val total = tracks.size; val finished = tracks.count { it.isFinished }
        val lessonsTotal = tracks.sumOf { it.totalLessons }; val lessonsDone = tracks.sumOf { it.completedCount }
        val avg = if (tracks.isNotEmpty()) tracks.map { it.progressPercent }.average() else 0.0

        // Общая статистика сверху
        val statsRow = HBox(16.0).apply { alignment = Pos.CENTER_LEFT }
        fun stat(v: String, l: String, color: String = "#dfe6ee") = VBox(4.0,
            Label(v).apply { style = "-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: $color;" },
            Label(l).apply { style = "-fx-font-size: 12px; -fx-text-fill: #5f6a7a;" }
        ).apply { styleClass.add("card-static"); alignment = Pos.CENTER; prefWidth = 160.0; padding = Insets(18.0) }

        statsRow.children.addAll(
            stat("$total", "Курсов", "#74b9ff"),
            stat("$finished", "Завершено", "#00b894"),
            stat("$lessonsDone/$lessonsTotal", "Уроков", "#dfe6ee"),
            stat("${String.format("%.0f", avg)}%", "Средний прогресс", "#6c5ce7")
        )
        container.children.add(statsRow)

        // Карточки по каждому курсу
        if (tracks.isNotEmpty()) {
            container.children.add(Label("По курсам").apply { styleClass.add("label-subtitle"); padding = Insets(4.0, 0.0, 0.0, 0.0) })

            val cardsBox = VBox(12.0)
            for (info in tracks) {
                val percent = info.progressPercent
                val barColor = when {
                    info.isFinished -> "#00b894"
                    percent >= 50 -> "#6c5ce7"
                    percent > 0 -> "#74b9ff"
                    else -> "#3a3b50"
                }
                val statusText = if (info.isFinished) "Завершён" else "${String.format("%.0f", percent)}%"

                val card = HBox(16.0).apply {
                    styleClass.add("card-static"); padding = Insets(16.0); alignment = Pos.CENTER_LEFT
                }

                val infoBox = VBox(4.0,
                    Label(info.course.name).apply { style = "-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #dfe6ee;"; isWrapText = true },
                    Label("${info.course.teacher.fullName} · ${info.completedCount}/${info.totalLessons} уроков").apply { style = "-fx-font-size: 13px; -fx-text-fill: #5f6a7a;" },
                    Region().apply { prefHeight = 4.0 },
                    ProgressBar(percent / 100.0).apply { prefWidth = 300.0; prefHeight = 8.0; style = "-fx-accent: $barColor;" },
                    Label("Начат: ${info.track.startDate}").apply { style = "-fx-font-size: 12px; -fx-text-fill: #3a3b50;" }
                ).apply { HBox.setHgrow(this, Priority.ALWAYS) }

                val statusLabel = Label(statusText).apply {
                    style = "-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: $barColor;"
                    minWidth = 80.0; alignment = Pos.CENTER
                }

                card.children.addAll(infoBox, statusLabel)
                cardsBox.children.add(card)
            }
            container.children.add(cardsBox)
        }

        setContent(ScrollPane(container).apply { isFitToWidth = true; hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER })
    }

    // ===================== ЧАТ =====================
    private fun showChat() {
        val userId = SessionManager.currentUser?.id ?: return
        val tracks = ProgressService.getTracksForUser(userId)
        val teachers = tracks.map { it.course.teacher }.distinctBy { it.id }

        val container = HBox(0.0)
        container.prefHeight = Double.MAX_VALUE

        val teacherList = VBox(0.0).apply {
            prefWidth = 260.0; minWidth = 260.0
            style = "-fx-background-color: #1c1d2b; -fx-border-color: #2a2b3d; -fx-border-width: 0 1 0 0;"
        }
        teacherList.children.add(Label("Чат").apply {
            styleClass.add("label-subtitle"); padding = Insets(16.0)
        })

        val chatArea = VBox().apply { HBox.setHgrow(this, Priority.ALWAYS) }

        if (teachers.isEmpty()) {
            chatArea.children.add(VBox(8.0,
                Label("Запишитесь на курс, чтобы написать преподавателю").apply { styleClass.add("label-secondary") }
            ).apply { alignment = Pos.CENTER; padding = Insets(60.0) })
        } else {
            // Собираем контакты с инфо о непрочитанных и времени последнего сообщения
            data class TeacherContact(val teacherUserId: Long, val name: String, val unread: Int, val lastTime: java.time.LocalDateTime?)
            val contacts = teachers.mapNotNull { teacher ->
                val tuid = teacher.user?.id ?: return@mapNotNull null
                val unread = MessageService.getUnreadCount(userId, tuid)
                val lastTime = MessageService.getLastMessageTime(userId, tuid)
                TeacherContact(tuid, teacher.fullName, unread, lastTime)
            }.sortedWith(compareByDescending<TeacherContact> { it.unread > 0 }.thenByDescending { it.lastTime })

            var activeContactBtn: Button? = null

            for (contact in contacts) {
                val nameLabel = Label(contact.name).apply {
                    style = if (contact.unread > 0) "-fx-text-fill: #dfe6ee; -fx-font-weight: bold;" else "-fx-text-fill: #5f6a7a;"
                    HBox.setHgrow(this, Priority.ALWAYS); maxWidth = Double.MAX_VALUE
                }
                val row = HBox(8.0).apply {
                    alignment = Pos.CENTER_LEFT
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
                        MessageService.markAsRead(userId, contact.teacherUserId)
                        nameLabel.style = "-fx-text-fill: #dfe6ee;"
                        row.children.removeIf { it is Label && it.styleClass.contains("unread-badge") }
                        showChatWith(contact.teacherUserId, contact.name, chatArea)
                    }
                }
                teacherList.children.add(btn)
            }

            if (contacts.isNotEmpty()) {
                val first = contacts[0]
                MessageService.markAsRead(userId, first.teacherUserId)
                showChatWith(first.teacherUserId, first.name, chatArea)
                val firstBtn = teacherList.children.filterIsInstance<Button>().firstOrNull { it.styleClass.contains("chat-contact") }
                firstBtn?.styleClass?.add("chat-contact-active")
                activeContactBtn = firstBtn
            }
        }

        container.children.addAll(teacherList, chatArea)
        setContent(container)
    }

    private fun showChatWith(teacherId: Long, teacherName: String, chatArea: VBox) {
        val userId = SessionManager.currentUser?.id ?: return
        chatArea.children.clear()
        MessageService.markAsRead(userId, teacherId)

        val header = Label(teacherName).apply {
            styleClass.add("label-subtitle"); padding = Insets(12.0, 16.0, 12.0, 16.0)
            style = "-fx-border-color: #2a2b3d; -fx-border-width: 0 0 1 0; -fx-background-color: #1c1d2b;"
            maxWidth = Double.MAX_VALUE
        }

        val messagesBox = VBox(8.0).apply { padding = Insets(16.0) }

        fun loadMessages() {
            messagesBox.children.clear()
            val messages = MessageService.getMessages(userId, teacherId)
            for (msg in messages) {
                val isMe = msg.senderId == userId
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
                    MessageService.sendMessage(userId, teacherId, text)
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

    // ===================== ДЕТАЛИ КУРСА =====================
    private fun showCourseDetail(courseId: Long) {
        val course = CourseService.getCourseWithModules(courseId) ?: return
        val userId = SessionManager.currentUser?.id ?: return
        val tracks = ProgressService.getTracksForUser(userId)
        val trackInfo = tracks.find { it.course.id == courseId }
        val completedIds = trackInfo?.track?.completedLessons?.map { it.id }?.toSet() ?: emptySet()

        val container = VBox(16.0).apply { padding = Insets(24.0, 32.0, 24.0, 32.0) }

        val backBtn = Button("← Назад").apply { styleClass.addAll("button", "btn-ghost"); setOnAction { showMyCourses() } }
        val title = Label(course.name).apply { styleClass.add("page-title") }
        val meta = Label("${course.teacher.fullName} · ${course.hosting.name} · ${course.duration}ч").apply { styleClass.add("label-secondary") }

        container.children.addAll(backBtn, title, meta)

        if (trackInfo != null) {
            val pb = ProgressBar(trackInfo.progressPercent / 100.0).apply { prefWidth = 300.0; prefHeight = 6.0 }
            val pl = Label("${trackInfo.completedCount}/${trackInfo.totalLessons} уроков · ${String.format("%.0f", trackInfo.progressPercent)}%").apply { styleClass.add("label-muted") }
            container.children.addAll(pb, pl)
        }

        for (module in course.modules) {
            val section = Label(module.name).apply { style = "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #8b95a5; -fx-padding: 8 0 4 0;" }
            container.children.add(section)

            val lessonBox = VBox(0.0).apply {
                style = "-fx-background-color: #1c1d2b; -fx-background-radius: 6; -fx-border-color: #2a2b3d; -fx-border-radius: 6;"
            }

            for ((i, lesson) in module.lessons.withIndex()) {
                val done = lesson.id in completedIds

                val row = HBox(10.0).apply {
                    alignment = Pos.CENTER_LEFT; padding = Insets(8.0, 12.0, 8.0, 12.0)
                    if (i < module.lessons.size - 1) style = "-fx-border-color: #222336; -fx-border-width: 0 0 1 0;"
                }

                val statusIndicator = Label(if (done) "+" else "-").apply {
                    style = "-fx-text-fill: ${if (done) "#00b894" else "#5f6a7a"}; -fx-font-size: 14px; -fx-font-weight: bold;"
                    minWidth = 16.0
                }
                val nameLbl = Label(lesson.name).apply {
                    style = if (done) "-fx-text-fill: #5f6a7a;" else "-fx-text-fill: #dfe6ee;"
                    HBox.setHgrow(this, Priority.ALWAYS)
                }
                val typeLbl = Label(lesson.type).apply { style = "-fx-text-fill: #3a3b50; -fx-font-size: 12px;" }

                row.children.addAll(statusIndicator, nameLbl, typeLbl)

                if (!done && trackInfo != null && !trackInfo.isFinished) {
                    val btn = Button("Выполнить").apply {
                        styleClass.addAll("button", "btn-sm"); style = "-fx-padding: 2 8; -fx-font-size: 12px; -fx-background-color: #6c5ce7; -fx-text-fill: white;"
                        setOnAction { ProgressService.markLessonComplete(userId, courseId, lesson.id); showCourseDetail(courseId) }
                    }
                    row.children.add(btn)
                }

                lessonBox.children.add(row)
            }
            container.children.add(lessonBox)
        }

        setContent(ScrollPane(container).apply { isFitToWidth = true; hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER })
    }

    // ===================== ПРОФИЛЬ =====================
    private fun showProfile() {
        val user = SessionManager.currentUser ?: return
        val container = VBox(20.0).apply { padding = Insets(32.0); maxWidth = 500.0 }

        val header = Label("Профиль").apply { styleClass.add("label-title") }
        val roleLabel = Label("Роль: Ученик").apply { styleClass.add("label-secondary") }

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
            passSection,
            Label("Текущий пароль").apply { styleClass.add("label-heading") }, oldPassField,
            Label("Новый пароль").apply { styleClass.add("label-heading") }, newPassField,
            Label("Подтверждение").apply { styleClass.add("label-heading") }, confirmPassField,
            passError, passSuccess,
            Region().apply { prefHeight = 4.0 },
            changePassBtn
        )
        setContent(container)
    }
}
