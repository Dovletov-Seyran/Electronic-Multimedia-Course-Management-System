package ru.bmstu.emk.ui

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.*
import ru.bmstu.emk.EmkApplication
import ru.bmstu.emk.domain.Course
import ru.bmstu.emk.service.CourseService
import ru.bmstu.emk.service.ProgressService
import ru.bmstu.emk.service.TrackInfo
import ru.bmstu.emk.util.SessionManager

class StudentDashboard : HBox() {

    private val contentArea = StackPane()
    private var activeButton: Button? = null

    init {
        // === Sidebar ===
        val sidebar = VBox().apply {
            styleClass.add("sidebar")
            prefWidth = 240.0
            minWidth = 240.0
        }

        val logoBox = VBox(4.0,
            Label("🎓").apply { style = "-fx-font-size: 28px;" },
            Label("АИС ЭМК").apply { styleClass.add("label-heading") },
            Label(SessionManager.currentUser?.login ?: "").apply { styleClass.add("label-muted") }
        ).apply {
            alignment = Pos.CENTER
            padding = Insets(24.0, 16.0, 24.0, 16.0)
            style = "-fx-border-color: #3d3d5c; -fx-border-width: 0 0 1 0;"
        }

        val menuItems = listOf(
            "📚" to "Мои курсы",
            "🔍" to "Каталог курсов",
            "📊" to "Прогресс",
        )

        val menuButtons = menuItems.map { (icon, label) ->
            Button("$icon  $label").apply {
                styleClass.addAll("sidebar-item")
                setOnAction {
                    setActiveButton(this)
                    when (label) {
                        "Мои курсы" -> showMyCourses()
                        "Каталог курсов" -> showCatalog()
                        "Прогресс" -> showProgress()
                    }
                }
            }
        }

        val logoutBtn = Button("🚪  Выйти").apply {
            styleClass.addAll("sidebar-item")
            style = "-fx-text-fill: #f87171;"
            setOnAction {
                SessionManager.logout()
                EmkApplication.navigateTo(RoleSelectScreen())
            }
        }

        val spacer = Region().apply { VBox.setVgrow(this, Priority.ALWAYS) }

        sidebar.children.addAll(logoBox)
        sidebar.children.addAll(menuButtons)
        sidebar.children.addAll(spacer, logoutBtn)

        // === Content ===
        contentArea.apply {
            padding = Insets(0.0)
            HBox.setHgrow(this, Priority.ALWAYS)
        }

        children.addAll(sidebar, contentArea)

        // Показываем "Мои курсы" по умолчанию
        setActiveButton(menuButtons[0])
        showMyCourses()
    }

    private fun setActiveButton(btn: Button) {
        activeButton?.styleClass?.remove("sidebar-item-active")
        btn.styleClass.add("sidebar-item-active")
        activeButton = btn
    }

    private fun showMyCourses() {
        val userId = SessionManager.currentUser?.id ?: return
        val tracks = ProgressService.getTracksForUser(userId)

        val scrollPane = ScrollPane().apply {
            isFitToWidth = true
            hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
        }

        val container = VBox(20.0).apply {
            padding = Insets(32.0)
        }

        val header = Label("📚 Мои курсы").apply { styleClass.add("label-title") }
        val subtitle = Label("Курсы, на которые вы записаны").apply { styleClass.add("label-secondary") }

        container.children.addAll(header, subtitle)

        if (tracks.isEmpty()) {
            val emptyBox = VBox(12.0,
                Label("😔").apply { style = "-fx-font-size: 48px;" },
                Label("Вы пока не записаны ни на один курс").apply { styleClass.add("label-secondary") },
                Button("Перейти в каталог →").apply {
                    styleClass.addAll("button", "btn-primary")
                    setOnAction { showCatalog() }
                }
            ).apply {
                alignment = Pos.CENTER
                padding = Insets(60.0)
            }
            container.children.add(emptyBox)
        } else {
            val grid = FlowPane(16.0, 16.0)
            for (trackInfo in tracks) {
                grid.children.add(createMyCourseCard(trackInfo))
            }
            container.children.add(grid)
        }

        scrollPane.content = container
        contentArea.children.setAll(scrollPane)
    }

    private fun createMyCourseCard(info: TrackInfo): VBox {
        val courseName = Label(info.course.name).apply { styleClass.add("label-heading"); isWrapText = true }
        val teacherName = Label("👨‍🏫 ${info.course.teacher.fullName}").apply { styleClass.add("label-secondary") }
        val hostingName = Label("🏠 ${info.course.hosting.name}").apply { styleClass.add("label-muted") }

        val progressBar = ProgressBar(info.progressPercent / 100.0).apply {
            prefWidth = 260.0; prefHeight = 8.0
        }
        val progressLabel = Label("${info.completedCount} / ${info.totalLessons} уроков (${String.format("%.0f", info.progressPercent)}%)").apply {
            styleClass.add("label-muted")
        }

        val statusBadge = Label(if (info.isFinished) "✅ Завершён" else "📖 В процессе").apply {
            styleClass.addAll("badge", if (info.isFinished) "badge-green" else "badge-blue")
        }

        val openBtn = Button("Открыть курс →").apply {
            styleClass.addAll("button", "btn-primary")
            maxWidth = Double.MAX_VALUE
            setOnAction { showCourseDetail(info.course.id) }
        }

        return VBox(10.0, statusBadge, courseName, teacherName, hostingName, progressBar, progressLabel, openBtn).apply {
            styleClass.add("card")
            prefWidth = 300.0
            padding = Insets(20.0)
        }
    }

    private fun showCatalog() {
        val scrollPane = ScrollPane().apply {
            isFitToWidth = true
            hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
        }

        val container = VBox(20.0).apply { padding = Insets(32.0) }

        val header = Label("🔍 Каталог курсов").apply { styleClass.add("label-title") }

        val searchField = TextField().apply {
            promptText = "Поиск по названию или описанию..."
            prefWidth = 400.0
        }

        val resultsBox = FlowPane(16.0, 16.0)

        fun loadCourses(query: String = "") {
            val courses = if (query.isBlank()) CourseService.getAllCourses() else CourseService.searchCourses(query)
            resultsBox.children.clear()
            val countLabel = Label("Найдено курсов: ${courses.size}").apply { styleClass.add("label-muted") }
            resultsBox.children.add(countLabel)
            // Хак: countLabel в FlowPane некрасиво, поставим отдельно
            resultsBox.children.clear()
            for (course in courses) {
                resultsBox.children.add(createCatalogCard(course))
            }
        }

        searchField.textProperty().addListener { _, _, newVal ->
            loadCourses(newVal)
        }

        val allCourses = CourseService.getAllCourses()
        val countLabel = Label("Всего курсов: ${allCourses.size}").apply { styleClass.add("label-secondary") }

        container.children.addAll(header, countLabel, searchField, resultsBox)
        loadCourses()

        scrollPane.content = container
        contentArea.children.setAll(scrollPane)
    }

    private fun createCatalogCard(course: Course): VBox {
        val courseName = Label(course.name).apply { styleClass.add("label-heading"); isWrapText = true }
        val desc = Label(course.description).apply { styleClass.add("label-secondary"); isWrapText = true; maxWidth = 260.0 }
        val teacher = Label("👨‍🏫 ${course.teacher.fullName}").apply { styleClass.add("label-muted") }
        val hosting = Label("🏠 ${course.hosting.name}").apply { styleClass.add("label-muted") }
        val duration = Label("⏱ ${course.duration} ч.").apply { styleClass.add("label-muted") }

        val userId = SessionManager.currentUser?.id ?: 0
        val enrolled = ProgressService.isEnrolled(userId, course.id)

        val actionBtn = if (enrolled) {
            Button("📖 Открыть").apply {
                styleClass.addAll("button", "btn-success")
                maxWidth = Double.MAX_VALUE
                setOnAction { showCourseDetail(course.id) }
            }
        } else {
            Button("✚ Записаться").apply {
                styleClass.addAll("button", "btn-primary")
                maxWidth = Double.MAX_VALUE
                setOnAction {
                    val success = ProgressService.enroll(userId, course.id)
                    if (success) {
                        text = "✅ Записан!"
                        isDisable = true
                        styleClass.remove("btn-primary")
                        styleClass.add("btn-success")
                    }
                }
            }
        }

        return VBox(8.0, courseName, desc, teacher, hosting, duration, Region().apply { prefHeight = 4.0 }, actionBtn).apply {
            styleClass.add("card")
            prefWidth = 300.0
            padding = Insets(20.0)
        }
    }

    private fun showProgress() {
        val userId = SessionManager.currentUser?.id ?: return
        val tracks = ProgressService.getTracksForUser(userId)

        val container = VBox(20.0).apply { padding = Insets(32.0) }

        val header = Label("📊 Мой прогресс").apply { styleClass.add("label-title") }

        // Статистика
        val totalCourses = tracks.size
        val finishedCourses = tracks.count { it.isFinished }
        val totalLessons = tracks.sumOf { it.totalLessons }
        val completedLessons = tracks.sumOf { it.completedCount }
        val avgProgress = if (tracks.isNotEmpty()) tracks.map { it.progressPercent }.average() else 0.0

        val statsBox = HBox(24.0).apply {
            alignment = Pos.CENTER_LEFT
        }

        fun statCard(value: String, label: String): VBox {
            return VBox(4.0,
                Label(value).apply { styleClass.add("stat-value") },
                Label(label).apply { styleClass.add("stat-label") }
            ).apply {
                styleClass.add("card-static")
                alignment = Pos.CENTER
                prefWidth = 180.0
                padding = Insets(20.0)
            }
        }

        statsBox.children.addAll(
            statCard("$totalCourses", "Курсов"),
            statCard("$finishedCourses", "Завершено"),
            statCard("$completedLessons/$totalLessons", "Уроков пройдено"),
            statCard("${String.format("%.0f", avgProgress)}%", "Средний прогресс")
        )

        container.children.addAll(header, statsBox)

        // Таблица прогресса
        if (tracks.isNotEmpty()) {
            val tableLabel = Label("Детализация по курсам").apply { styleClass.add("label-subtitle"); padding = Insets(12.0, 0.0, 0.0, 0.0) }

            val table = TableView<TrackInfo>().apply {
                prefHeight = 400.0

                val nameCol = TableColumn<TrackInfo, String>("Курс").apply {
                    setCellValueFactory { javafx.beans.property.SimpleStringProperty(it.value.course.name) }
                    prefWidth = 250.0
                }
                val statusCol = TableColumn<TrackInfo, String>("Статус").apply {
                    setCellValueFactory { javafx.beans.property.SimpleStringProperty(it.value.track.status) }
                    prefWidth = 120.0
                }
                val progressCol = TableColumn<TrackInfo, String>("Прогресс").apply {
                    setCellValueFactory {
                        javafx.beans.property.SimpleStringProperty(
                            "${it.value.completedCount}/${it.value.totalLessons} (${String.format("%.0f", it.value.progressPercent)}%)"
                        )
                    }
                    prefWidth = 160.0
                }
                val dateCol = TableColumn<TrackInfo, String>("Дата записи").apply {
                    setCellValueFactory { javafx.beans.property.SimpleStringProperty(it.value.track.startDate.toString()) }
                    prefWidth = 130.0
                }

                columns.addAll(nameCol, statusCol, progressCol, dateCol)
                items.addAll(tracks)
            }

            container.children.addAll(tableLabel, table)
        }

        val scrollPane = ScrollPane(container).apply {
            isFitToWidth = true
            hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
        }
        contentArea.children.setAll(scrollPane)
    }

    private fun showCourseDetail(courseId: Long) {
        val course = CourseService.getCourseWithModules(courseId) ?: return
        val userId = SessionManager.currentUser?.id ?: return

        val tracks = ProgressService.getTracksForUser(userId)
        val trackInfo = tracks.find { it.course.id == courseId }
        val completedIds = trackInfo?.track?.completedLessons?.map { it.id }?.toSet() ?: emptySet()

        val container = VBox(20.0).apply { padding = Insets(32.0) }

        val backBtn = Button("← Назад к моим курсам").apply {
            styleClass.addAll("button", "btn-ghost")
            setOnAction { showMyCourses() }
        }

        val header = Label(course.name).apply { styleClass.add("label-title") }
        val desc = Label(course.description).apply { styleClass.add("label-secondary") }
        val meta = HBox(16.0,
            Label("👨‍🏫 ${course.teacher.fullName}").apply { styleClass.add("label-muted") },
            Label("🏠 ${course.hosting.name}").apply { styleClass.add("label-muted") },
            Label("⏱ ${course.duration} ч.").apply { styleClass.add("label-muted") }
        )

        if (trackInfo != null) {
            val progressBar = ProgressBar(trackInfo.progressPercent / 100.0).apply { prefWidth = 400.0; prefHeight = 10.0 }
            val progressLabel = Label("Прогресс: ${trackInfo.completedCount}/${trackInfo.totalLessons} (${String.format("%.0f", trackInfo.progressPercent)}%)").apply {
                styleClass.add("label-secondary")
            }
            container.children.addAll(backBtn, header, desc, meta, progressBar, progressLabel)
        } else {
            container.children.addAll(backBtn, header, desc, meta)
        }

        // Модули и уроки
        for (module in course.modules) {
            val moduleBox = VBox(8.0).apply {
                styleClass.add("card-static")
                padding = Insets(16.0)
            }
            val moduleTitle = Label("📦 ${module.name}").apply { styleClass.add("label-subtitle") }
            moduleBox.children.add(moduleTitle)

            for (lesson in module.lessons) {
                val isCompleted = lesson.id in completedIds
                val icon = when {
                    isCompleted -> "✅"
                    lesson.type == "video" -> "🎬"
                    lesson.type == "pdf" -> "📄"
                    else -> "📝"
                }

                val lessonRow = HBox(12.0).apply {
                    alignment = Pos.CENTER_LEFT
                    padding = Insets(8.0, 12.0, 8.0, 12.0)
                    style = if (isCompleted) "-fx-background-color: rgba(74, 222, 128, 0.08); -fx-background-radius: 8;" else ""
                }

                val lessonLabel = Label("$icon ${lesson.name}").apply {
                    styleClass.add(if (isCompleted) "label-muted" else "label")
                    if (isCompleted) style = "-fx-strikethrough: false; -fx-text-fill: #4ade80;"
                }
                val typeLabel = Label(lesson.type).apply { styleClass.add("label-muted") }
                val spacer = Region().apply { HBox.setHgrow(this, Priority.ALWAYS) }

                lessonRow.children.addAll(lessonLabel, spacer, typeLabel)

                if (!isCompleted && trackInfo != null && !trackInfo.isFinished) {
                    val completeBtn = Button("Отметить ✓").apply {
                        styleClass.addAll("button")
                        style = "-fx-padding: 4 12; -fx-font-size: 12px; -fx-background-color: #16a34a; -fx-text-fill: white;"
                        setOnAction {
                            val success = ProgressService.markLessonComplete(userId, courseId, lesson.id)
                            if (success) {
                                showCourseDetail(courseId) // перезагрузим экран
                            }
                        }
                    }
                    lessonRow.children.add(completeBtn)
                }

                moduleBox.children.add(lessonRow)
            }

            container.children.add(moduleBox)
        }

        val scrollPane = ScrollPane(container).apply {
            isFitToWidth = true
            hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
        }
        contentArea.children.setAll(scrollPane)
    }
}