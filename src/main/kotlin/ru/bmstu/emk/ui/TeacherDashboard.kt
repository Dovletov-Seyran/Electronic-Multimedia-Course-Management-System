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
            Label("👨‍🏫").apply { style = "-fx-font-size: 28px;" },
            Label("Преподаватель").apply { styleClass.add("label-heading") },
            Label(teacherName).apply { styleClass.add("label-muted"); isWrapText = true; maxWidth = 200.0 }
        ).apply {
            alignment = Pos.CENTER; padding = Insets(24.0, 16.0, 24.0, 16.0)
            style = "-fx-border-color: #3d3d5c; -fx-border-width: 0 0 1 0;"
        }

        val menuItems = listOf("📚" to "Мои курсы", "👥" to "Мои студенты", "📊" to "Статистика")
        val menuButtons = menuItems.map { (icon, label) ->
            Button("$icon  $label").apply {
                styleClass.addAll("sidebar-item")
                setOnAction {
                    setActiveButton(this)
                    when (label) {
                        "Мои курсы" -> showMyCourses()
                        "Мои студенты" -> showStudents()
                        "Статистика" -> showStats()
                    }
                }
            }
        }

        val logoutBtn = Button("🚪  Выйти").apply {
            styleClass.addAll("sidebar-item"); style = "-fx-text-fill: #f87171;"
            setOnAction { SessionManager.logout(); EmkApplication.navigateTo(RoleSelectScreen()) }
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
        val header = Label("📚 Мои курсы").apply { styleClass.add("label-title") }

        val addBtn = Button("＋ Создать курс").apply {
            styleClass.addAll("button", "btn-primary")
            setOnAction { showCreateCourseForm() }
        }

        val topRow = HBox(16.0, header, Region().apply { HBox.setHgrow(this, Priority.ALWAYS) }, addBtn).apply {
            alignment = Pos.CENTER_LEFT
        }

        container.children.add(topRow)

        if (courses.isEmpty()) {
            container.children.add(VBox(12.0,
                Label("📭").apply { style = "-fx-font-size: 48px;" },
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
        val hosting = Label("🏠 ${course.hosting.name}").apply { styleClass.add("label-muted") }
        val duration = Label("⏱ ${course.duration} ч.").apply { styleClass.add("label-muted") }

        val editBtn = Button("✏️ Редактировать").apply {
            styleClass.addAll("button"); maxWidth = Double.MAX_VALUE
            setOnAction { showEditCourse(course.id) }
        }
        val deleteBtn = Button("🗑 Удалить").apply {
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

        val saveBtn = Button("💾 Создать курс").apply {
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
        val header = Label("✏️ ${course.name}").apply { styleClass.add("label-title") }

        // Редактирование основных полей
        val nameField = TextField(course.name)
        val descField = TextField(course.description)
        val durationField = TextField(course.duration.toString())
        val hostingCombo = ComboBox<String>().apply {
            items.addAll(hostings.map { it.name })
            val idx = hostings.indexOfFirst { it.id == course.hosting.id }
            selectionModel.select(if (idx >= 0) idx else 0); maxWidth = Double.MAX_VALUE
        }

        val updateBtn = Button("💾 Сохранить изменения").apply {
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
        val modulesHeader = HBox(12.0,
            Label("📦 Модули и уроки").apply { styleClass.add("label-subtitle") },
            Region().apply { HBox.setHgrow(this, Priority.ALWAYS) },
            Button("＋ Добавить модуль").apply {
                styleClass.addAll("button", "btn-primary"); style = "-fx-padding: 6 16; -fx-font-size: 13px;"
                setOnAction {
                    val dialog = TextInputDialog(); dialog.title = "Новый модуль"; dialog.headerText = "Введите название модуля"
                    dialog.showAndWait().ifPresent { name ->
                        if (name.isNotBlank()) { TeacherService.addModule(courseId, name.trim()); showEditCourse(courseId) }
                    }
                }
            }
        ).apply { alignment = Pos.CENTER_LEFT; padding = Insets(12.0, 0.0, 0.0, 0.0) }

        container.children.add(modulesHeader)

        for (module in course.modules) {
            val moduleBox = VBox(8.0).apply { styleClass.add("card-static"); padding = Insets(16.0) }

            val moduleHeader = HBox(12.0,
                Label("📦 ${module.name}").apply { styleClass.add("label-heading") },
                Region().apply { HBox.setHgrow(this, Priority.ALWAYS) },
                Button("＋ Урок").apply {
                    styleClass.addAll("button"); style = "-fx-padding: 4 12; -fx-font-size: 12px;"
                    setOnAction {
                        val dialog = TextInputDialog(); dialog.title = "Новый урок"; dialog.headerText = "Название урока"
                        dialog.showAndWait().ifPresent { name ->
                            if (name.isNotBlank()) { TeacherService.addLesson(module.id, name.trim(), "video"); showEditCourse(courseId) }
                        }
                    }
                },
                Button("🗑").apply {
                    styleClass.addAll("button", "btn-danger"); style = "-fx-padding: 4 10; -fx-font-size: 12px;"
                    setOnAction {
                        val a = Alert(Alert.AlertType.CONFIRMATION, "Удалить модуль «${module.name}» и все его уроки?", ButtonType.YES, ButtonType.NO)
                        if (a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) { TeacherService.deleteModule(module.id); showEditCourse(courseId) }
                    }
                }
            ).apply { alignment = Pos.CENTER_LEFT }

            moduleBox.children.add(moduleHeader)

            for (lesson in module.lessons) {
                val icon = when (lesson.type) { "video" -> "🎬"; "pdf" -> "📄"; else -> "📝" }
                val lessonRow = HBox(12.0,
                    Label("$icon ${lesson.name}").apply { styleClass.add("label") },
                    Label(lesson.type).apply { styleClass.add("label-muted") },
                    Region().apply { HBox.setHgrow(this, Priority.ALWAYS) },
                    Button("🗑").apply {
                        styleClass.addAll("button", "btn-danger"); style = "-fx-padding: 2 8; -fx-font-size: 11px;"
                        setOnAction { TeacherService.deleteLesson(lesson.id); showEditCourse(courseId) }
                    }
                ).apply { alignment = Pos.CENTER_LEFT; padding = Insets(4.0, 8.0, 4.0, 24.0) }
                moduleBox.children.add(lessonRow)
            }

            if (module.lessons.isEmpty()) {
                moduleBox.children.add(Label("  Пока нет уроков").apply { styleClass.add("label-muted"); padding = Insets(4.0, 0.0, 0.0, 24.0) })
            }

            container.children.add(moduleBox)
        }

        contentArea.children.setAll(ScrollPane(container).apply { isFitToWidth = true; hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER })
    }

    private fun showStudents() {
        val teacherId = SessionManager.currentTeacher?.id ?: return
        val students = TeacherService.getStudentsForTeacher(teacherId)

        val container = VBox(20.0).apply { padding = Insets(32.0) }
        val header = Label("👥 Мои студенты").apply { styleClass.add("label-title") }
        val subtitle = Label("Студенты, записанные на ваши курсы: ${students.size}").apply { styleClass.add("label-secondary") }

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

        container.children.addAll(header, subtitle, table)
        contentArea.children.setAll(ScrollPane(container).apply { isFitToWidth = true })
    }

    private fun showStats() {
        val teacherId = SessionManager.currentTeacher?.id ?: return
        val courses = CourseService.getCoursesForTeacher(teacherId)
        val students = TeacherService.getStudentsForTeacher(teacherId)

        val container = VBox(20.0).apply { padding = Insets(32.0) }
        val header = Label("📊 Статистика").apply { styleClass.add("label-title") }

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
                Label("👥 $enrolled").apply { styleClass.add("label-secondary") },
                Label("✅ $finished").apply { styleClass.add("label-secondary") },
                ProgressBar(avg / 100.0).apply { prefWidth = 150.0; prefHeight = 8.0 },
                Label("${String.format("%.0f", avg)}%").apply { styleClass.add("label-muted") }
            ).apply { alignment = Pos.CENTER_LEFT; styleClass.add("card-static"); padding = Insets(12.0) }
            courseStatsBox.children.add(row)
        }

        container.children.addAll(header, statsBox, courseStatsLabel, courseStatsBox)
        contentArea.children.setAll(ScrollPane(container).apply { isFitToWidth = true; hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER })
    }
}