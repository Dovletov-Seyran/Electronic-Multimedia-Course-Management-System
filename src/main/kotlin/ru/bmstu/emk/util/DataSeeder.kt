package ru.bmstu.emk.util

import org.hibernate.Session
import ru.bmstu.emk.domain.*
import java.time.LocalDate
import kotlin.random.Random

object DataSeeder {

    fun seed() {
        val sf = HibernateUtil.sessionFactory
        val session = sf.openSession()
        val tx = session.beginTransaction()

        try {
            val userCount = session.createQuery("SELECT COUNT(u) FROM User u", Long::class.java).singleResult
            if (userCount > 0) {
                println("📦 Данные уже существуют, пропускаем сидинг")
                tx.rollback()
                session.close()
                return
            }

            // ==================== ПОЛЬЗОВАТЕЛИ ====================
            // Админы
            val admins = listOf(
                createUser(session, "admin", "admin", "ADMIN"),
                createUser(session, "superadmin", "admin", "ADMIN"),
            )

            // Преподаватели (15 штук)
            val teacherData = listOf(
                Triple("ivanov", "Иванов Иван Иванович", "ivanov@bmstu.ru"),
                Triple("petrova", "Петрова Анна Сергеевна", "petrova@bmstu.ru"),
                Triple("sidorov", "Сидоров Алексей Петрович", "sidorov@bmstu.ru"),
                Triple("kozlova", "Козлова Мария Дмитриевна", "kozlova@bmstu.ru"),
                Triple("novikov", "Новиков Дмитрий Александрович", "novikov@bmstu.ru"),
                Triple("morozova", "Морозова Елена Викторовна", "morozova@bmstu.ru"),
                Triple("volkov", "Волков Сергей Николаевич", "volkov@bmstu.ru"),
                Triple("sokolova", "Соколова Татьяна Андреевна", "sokolova@bmstu.ru"),
                Triple("lebedev", "Лебедев Андрей Игоревич", "lebedev@bmstu.ru"),
                Triple("kuznetsova", "Кузнецова Ольга Владимировна", "kuznetsova@bmstu.ru"),
                Triple("popov", "Попов Михаил Юрьевич", "popov@bmstu.ru"),
                Triple("fedorova", "Фёдорова Наталья Сергеевна", "fedorova@bmstu.ru"),
                Triple("orlov", "Орлов Владимир Константинович", "orlov@bmstu.ru"),
                Triple("pavlova", "Павлова Ирина Александровна", "pavlova@bmstu.ru"),
                Triple("semenov", "Семёнов Константин Петрович", "semenov@bmstu.ru"),
            )

            val teachers = teacherData.map { (login, fullName, email) ->
                val user = createUser(session, login, "123", "TEACHER")
                val teacher = Teacher().apply {
                    this.fullName = fullName
                    this.email = email
                    this.user = user
                }
                session.persist(teacher)
                teacher
            }

            // Студенты (120 штук)
            val firstNames = listOf("Александр", "Дмитрий", "Максим", "Артём", "Иван", "Кирилл", "Даниил", "Михаил", "Егор", "Матвей",
                "Андрей", "Илья", "Алексей", "Роман", "Тимофей", "Арсений", "Никита", "Владислав", "Марк", "Денис",
                "Анна", "Мария", "Елена", "Ольга", "Екатерина", "Дарья", "Алиса", "Полина", "София", "Виктория",
                "Ксения", "Валерия", "Анастасия", "Вероника", "Диана", "Кристина", "Милана", "Арина", "Ева", "Татьяна")
            val lastNames = listOf("Смирнов", "Кузнецов", "Попов", "Васильев", "Соколов", "Михайлов", "Новиков", "Фёдоров", "Морозов", "Волков",
                "Алексеев", "Лебедев", "Семёнов", "Егоров", "Павлов", "Козлов", "Степанов", "Николаев", "Орлов", "Андреев",
                "Макаров", "Никитин", "Захаров", "Зайцев", "Соловьёв", "Борисов", "Яковлев", "Григорьев", "Романов", "Воробьёв",
                "Сергеев", "Кузьмин", "Фролов", "Александров", "Дмитриев", "Королёв", "Гусев", "Киселёв", "Ильин", "Максимов")

            val students = (1..120).map { i ->
                val fn = firstNames[i % firstNames.size]
                val ln = lastNames[i % lastNames.size]
                createUser(session, "student$i", "123", "STUDENT")
            }

            // ==================== ХОСТИНГИ ====================
            val hostings = listOf(
                createHosting(session, "Coursera", "https://coursera.org"),
                createHosting(session, "Stepik", "https://stepik.org"),
                createHosting(session, "МГТУ ЭОС", "https://lms.bmstu.ru"),
                createHosting(session, "Открытое образование", "https://openedu.ru"),
                createHosting(session, "Яндекс Практикум", "https://practicum.yandex.ru"),
                createHosting(session, "GeekBrains", "https://geekbrains.ru"),
                createHosting(session, "Нетология", "https://netology.ru"),
                createHosting(session, "Skillbox", "https://skillbox.ru"),
            )

            // ==================== КУРСЫ (50 штук) ====================
            data class CourseTemplate(
                val name: String, val desc: String, val duration: Int,
                val teacherIdx: Int, val hostingIdx: Int,
                val modules: List<Pair<String, List<Pair<String, String>>>>
            )

            val courseTemplates = listOf(
                // --- Программирование ---
                CourseTemplate("Основы программирования на Python", "Введение в Python для начинающих", 40, 0, 1,
                    listOf("Введение" to listOf("Что такое программирование" to "video", "Установка Python" to "pdf", "Первая программа" to "text"),
                        "Переменные и типы" to listOf("Числа и строки" to "video", "Списки и кортежи" to "video", "Словари" to "video", "Практика: переменные" to "text"),
                        "Управляющие конструкции" to listOf("Условный оператор if" to "video", "Цикл for" to "video", "Цикл while" to "video", "Задачи на циклы" to "text"),
                        "Функции" to listOf("Определение функций" to "video", "Аргументы и возврат" to "video", "Лямбда-функции" to "video"))),
                CourseTemplate("Продвинутый Python", "ООП, декораторы, генераторы и многопоточность", 60, 0, 1,
                    listOf("ООП в Python" to listOf("Классы и объекты" to "video", "Наследование" to "video", "Полиморфизм" to "video", "Магические методы" to "text"),
                        "Продвинутые темы" to listOf("Декораторы" to "video", "Генераторы" to "video", "Контекстные менеджеры" to "video"),
                        "Многопоточность" to listOf("Threading" to "video", "Asyncio" to "video", "Multiprocessing" to "video"))),
                CourseTemplate("Java для начинающих", "Основы Java: от Hello World до ООП", 50, 1, 0,
                    listOf("Введение в Java" to listOf("Установка JDK" to "pdf", "Первая программа" to "video", "Типы данных" to "video"),
                        "Управляющие конструкции" to listOf("if/else" to "video", "switch" to "video", "Циклы" to "video"),
                        "ООП" to listOf("Классы" to "video", "Интерфейсы" to "video", "Абстрактные классы" to "video", "Коллекции" to "video"))),
                CourseTemplate("Kotlin с нуля", "Современный язык для JVM и Android", 45, 1, 4,
                    listOf("Основы Kotlin" to listOf("Зачем Kotlin" to "video", "Переменные и типы" to "video", "Функции" to "video"),
                        "ООП в Kotlin" to listOf("Классы и data class" to "video", "Sealed class" to "video", "Companion object" to "video"),
                        "Kotlin и коллекции" to listOf("Списки и множества" to "video", "map/filter/reduce" to "video", "Sequences" to "video"))),
                CourseTemplate("C/C++ для системного программирования", "Низкоуровневое программирование", 80, 2, 2,
                    listOf("Основы C" to listOf("Компиляция и линковка" to "video", "Типы данных" to "video", "Указатели" to "video", "Массивы" to "video"),
                        "Продвинутый C" to listOf("Структуры" to "video", "Динамическая память" to "video", "Файловый ввод-вывод" to "video"),
                        "Основы C++" to listOf("Классы в C++" to "video", "STL контейнеры" to "video", "Шаблоны" to "video", "Умные указатели" to "video"))),
                CourseTemplate("Алгоритмы и структуры данных", "Фундаментальный курс по алгоритмам", 70, 2, 3,
                    listOf("Базовые структуры" to listOf("Массивы и связные списки" to "video", "Стеки и очереди" to "video", "Хеш-таблицы" to "video"),
                        "Сортировки" to listOf("Пузырёк и вставки" to "video", "Быстрая сортировка" to "video", "Сортировка слиянием" to "video"),
                        "Графы" to listOf("Представление графов" to "video", "BFS и DFS" to "video", "Дейкстра" to "video", "Флойд-Уоршелл" to "video"),
                        "Динамическое программирование" to listOf("Основы ДП" to "video", "Рюкзак" to "video", "Наибольшая подпоследовательность" to "video"))),
                CourseTemplate("JavaScript: полный курс", "От основ до асинхронности", 55, 3, 0,
                    listOf("Основы JS" to listOf("Переменные и типы" to "video", "Функции" to "video", "Объекты" to "video"),
                        "DOM и события" to listOf("Работа с DOM" to "video", "События" to "video", "Формы" to "video"),
                        "Асинхронность" to listOf("Callbacks" to "video", "Promises" to "video", "async/await" to "video"))),
                CourseTemplate("TypeScript для разработчиков", "Типизация JavaScript", 35, 3, 4,
                    listOf("Основы TS" to listOf("Зачем TypeScript" to "video", "Типы данных" to "video", "Интерфейсы" to "video"),
                        "Продвинутый TS" to listOf("Generics" to "video", "Utility Types" to "video", "Декораторы" to "video"))),
                CourseTemplate("React.js", "Разработка SPA на React", 50, 3, 5,
                    listOf("Основы React" to listOf("JSX и компоненты" to "video", "Props и State" to "video", "Жизненный цикл" to "video"),
                        "Hooks" to listOf("useState" to "video", "useEffect" to "video", "useContext" to "video", "Custom hooks" to "video"),
                        "Роутинг и состояние" to listOf("React Router" to "video", "Redux Toolkit" to "video", "React Query" to "video"))),
                CourseTemplate("Node.js Backend", "Серверная разработка на Node.js", 45, 4, 5,
                    listOf("Основы Node.js" to listOf("Установка и npm" to "pdf", "Модули" to "video", "Файловая система" to "video"),
                        "Express.js" to listOf("Роутинг" to "video", "Middleware" to "video", "REST API" to "video"),
                        "Базы данных" to listOf("MongoDB" to "video", "PostgreSQL" to "video", "ORM Prisma" to "video"))),

                // --- Базы данных ---
                CourseTemplate("Базы данных: основы", "Реляционные БД и SQL", 60, 4, 2,
                    listOf("Введение в БД" to listOf("Что такое СУБД" to "video", "Реляционная модель" to "pdf", "Нормальные формы" to "video"),
                        "SQL запросы" to listOf("SELECT и WHERE" to "video", "JOIN таблиц" to "video", "Подзапросы" to "video", "Агрегатные функции" to "text"),
                        "Проектирование" to listOf("ER-диаграммы" to "video", "Нормализация" to "video", "Индексы" to "video"))),
                CourseTemplate("PostgreSQL для профессионалов", "Продвинутые возможности PostgreSQL", 50, 4, 2,
                    listOf("Архитектура" to listOf("Процессы и память" to "video", "WAL и восстановление" to "video", "MVCC" to "video"),
                        "Производительность" to listOf("EXPLAIN ANALYZE" to "video", "Оптимизация запросов" to "video", "Партиционирование" to "video"),
                        "Расширения" to listOf("JSON/JSONB" to "video", "Full-text search" to "video", "PostGIS" to "video"))),
                CourseTemplate("NoSQL базы данных", "MongoDB, Redis, Cassandra", 40, 5, 3,
                    listOf("MongoDB" to listOf("Документная модель" to "video", "CRUD операции" to "video", "Агрегации" to "video"),
                        "Redis" to listOf("Структуры данных" to "video", "Кеширование" to "video", "Pub/Sub" to "video"),
                        "Cassandra" to listOf("Колоночная модель" to "video", "CQL" to "video", "Репликация" to "video"))),

                // --- Веб-разработка ---
                CourseTemplate("HTML и CSS с нуля", "Вёрстка современных сайтов", 30, 5, 0,
                    listOf("HTML" to listOf("Структура документа" to "video", "Семантические теги" to "video", "Формы и таблицы" to "video"),
                        "CSS основы" to listOf("Селекторы" to "video", "Box Model" to "video", "Позиционирование" to "video"),
                        "Современный CSS" to listOf("Flexbox" to "video", "Grid" to "video", "Анимации" to "video", "Адаптивность" to "video"))),
                CourseTemplate("Vue.js 3", "Прогрессивный фреймворк для UI", 40, 6, 6,
                    listOf("Основы Vue" to listOf("Установка и CLI" to "video", "Шаблоны" to "video", "Реактивность" to "video"),
                        "Компоненты" to listOf("Props и Events" to "video", "Slots" to "video", "Composition API" to "video"),
                        "Экосистема" to listOf("Vue Router" to "video", "Pinia" to "video", "Nuxt.js" to "video"))),
                CourseTemplate("Angular", "Enterprise фреймворк от Google", 55, 6, 7,
                    listOf("Основы Angular" to listOf("Компоненты" to "video", "Модули" to "video", "Директивы" to "video"),
                        "Сервисы и DI" to listOf("Services" to "video", "Dependency Injection" to "video", "HTTP Client" to "video"),
                        "Роутинг" to listOf("Router" to "video", "Guards" to "video", "Lazy Loading" to "video"))),
                CourseTemplate("Fullstack веб-разработка", "Frontend + Backend + DevOps", 100, 7, 4,
                    listOf("Frontend" to listOf("HTML/CSS/JS обзор" to "video", "React основы" to "video", "Сборка проекта" to "video"),
                        "Backend" to listOf("REST API" to "video", "Авторизация JWT" to "video", "WebSocket" to "video"),
                        "DevOps" to listOf("Docker" to "video", "CI/CD" to "video", "Деплой" to "video"))),

                // --- Мобильная разработка ---
                CourseTemplate("Android разработка на Kotlin", "Нативные приложения для Android", 70, 7, 4,
                    listOf("Основы Android" to listOf("Android Studio" to "pdf", "Activity и Fragment" to "video", "Layouts" to "video"),
                        "UI компоненты" to listOf("RecyclerView" to "video", "Navigation" to "video", "Material Design" to "video"),
                        "Данные" to listOf("Room" to "video", "Retrofit" to "video", "DataStore" to "video"),
                        "Архитектура" to listOf("MVVM" to "video", "Hilt/Dagger" to "video", "Coroutines" to "video"))),
                CourseTemplate("iOS разработка на Swift", "Нативные приложения для iPhone", 65, 8, 0,
                    listOf("Основы Swift" to listOf("Xcode" to "pdf", "Типы и переменные" to "video", "Опционалы" to "video"),
                        "UIKit" to listOf("ViewController" to "video", "TableView" to "video", "Auto Layout" to "video"),
                        "SwiftUI" to listOf("Декларативный UI" to "video", "Стейт-менеджмент" to "video", "Навигация" to "video"))),
                CourseTemplate("Flutter и Dart", "Кроссплатформенная мобильная разработка", 50, 8, 7,
                    listOf("Dart" to listOf("Основы Dart" to "video", "ООП в Dart" to "video", "Асинхронность" to "video"),
                        "Flutter основы" to listOf("Виджеты" to "video", "Layouts" to "video", "Навигация" to "video"),
                        "Продвинутый Flutter" to listOf("State Management" to "video", "HTTP и API" to "video", "Firebase" to "video"))),

                // --- DevOps и инфраструктура ---
                CourseTemplate("Docker и контейнеризация", "Контейнеры для разработчиков", 25, 9, 1,
                    listOf("Основы Docker" to listOf("Что такое контейнеры" to "video", "Dockerfile" to "video", "Docker Compose" to "video"),
                        "Практика" to listOf("Многоконтейнерные приложения" to "video", "Volumes и Networks" to "video", "Best practices" to "video"))),
                CourseTemplate("Kubernetes", "Оркестрация контейнеров", 45, 9, 3,
                    listOf("Архитектура K8s" to listOf("Компоненты кластера" to "video", "Pods и Deployments" to "video", "Services" to "video"),
                        "Конфигурация" to listOf("ConfigMaps и Secrets" to "video", "Volumes" to "video", "Namespaces" to "video"),
                        "Продвинутые темы" to listOf("Helm" to "video", "Ingress" to "video", "Мониторинг" to "video"))),
                CourseTemplate("CI/CD и автоматизация", "Непрерывная интеграция и доставка", 30, 10, 2,
                    listOf("Основы CI/CD" to listOf("Концепции" to "video", "GitLab CI" to "video", "GitHub Actions" to "video"),
                        "Практика" to listOf("Пайплайны" to "video", "Тестирование" to "video", "Деплой" to "video"))),
                CourseTemplate("Linux для разработчиков", "Командная строка и администрирование", 35, 10, 1,
                    listOf("Командная строка" to listOf("Навигация" to "video", "Работа с файлами" to "video", "Pipes и grep" to "video"),
                        "Администрирование" to listOf("Пользователи и права" to "video", "Процессы" to "video", "Systemd" to "video"),
                        "Сети" to listOf("SSH" to "video", "Firewall" to "video", "DNS" to "video"))),
                CourseTemplate("Git и системы контроля версий", "Профессиональная работа с Git", 20, 10, 2,
                    listOf("Основы Git" to listOf("init, add, commit" to "video", "Ветвление" to "video", "Merge и Rebase" to "video"),
                        "Командная работа" to listOf("Pull Requests" to "video", "Code Review" to "video", "Git Flow" to "video"))),

                // --- Машинное обучение и данные ---
                CourseTemplate("Введение в Machine Learning", "Основы машинного обучения", 60, 11, 0,
                    listOf("Основы ML" to listOf("Типы задач" to "video", "Линейная регрессия" to "video", "Логистическая регрессия" to "video"),
                        "Классификация" to listOf("Деревья решений" to "video", "Random Forest" to "video", "SVM" to "video"),
                        "Кластеризация" to listOf("K-means" to "video", "DBSCAN" to "video", "Иерархическая" to "video"),
                        "Оценка моделей" to listOf("Метрики" to "video", "Кросс-валидация" to "video", "Переобучение" to "video"))),
                CourseTemplate("Deep Learning", "Нейронные сети и глубокое обучение", 70, 11, 0,
                    listOf("Основы нейросетей" to listOf("Перцептрон" to "video", "Обратное распространение" to "video", "Функции активации" to "video"),
                        "CNN" to listOf("Свёрточные слои" to "video", "Пулинг" to "video", "Архитектуры CNN" to "video"),
                        "RNN и трансформеры" to listOf("LSTM и GRU" to "video", "Attention" to "video", "Transformer" to "video"),
                        "Практика" to listOf("PyTorch основы" to "video", "Transfer Learning" to "video", "Обучение моделей" to "video"))),
                CourseTemplate("Анализ данных с Python", "Pandas, NumPy, визуализация", 40, 12, 1,
                    listOf("NumPy" to listOf("Массивы" to "video", "Операции" to "video", "Линейная алгебра" to "video"),
                        "Pandas" to listOf("DataFrame" to "video", "Фильтрация" to "video", "Группировка" to "video", "Merge" to "video"),
                        "Визуализация" to listOf("Matplotlib" to "video", "Seaborn" to "video", "Plotly" to "video"))),
                CourseTemplate("NLP: обработка естественного языка", "Текстовая аналитика и LLM", 55, 12, 3,
                    listOf("Основы NLP" to listOf("Токенизация" to "video", "TF-IDF" to "video", "Word2Vec" to "video"),
                        "Модели" to listOf("BERT" to "video", "GPT" to "video", "Fine-tuning" to "video"),
                        "Практика" to listOf("Классификация текстов" to "video", "Чат-боты" to "video", "Summarization" to "video"))),
                CourseTemplate("Computer Vision", "Компьютерное зрение", 50, 11, 3,
                    listOf("Основы CV" to listOf("Обработка изображений" to "video", "OpenCV" to "video", "Фильтры" to "video"),
                        "Детекция" to listOf("YOLO" to "video", "SSD" to "video", "Сегментация" to "video"),
                        "Генерация" to listOf("GAN" to "video", "VAE" to "video", "Stable Diffusion обзор" to "video"))),

                // --- Математика ---
                CourseTemplate("Линейная алгебра", "Векторы, матрицы, пространства", 50, 13, 3,
                    listOf("Векторы" to listOf("Определение вектора" to "video", "Операции с векторами" to "video", "Скалярное произведение" to "video"),
                        "Матрицы" to listOf("Операции с матрицами" to "video", "Определитель" to "video", "Обратная матрица" to "video"),
                        "Пространства" to listOf("Линейное пространство" to "video", "Базис" to "video", "Собственные значения" to "video"))),
                CourseTemplate("Математический анализ", "Пределы, производные, интегралы", 60, 13, 2,
                    listOf("Пределы" to listOf("Определение предела" to "video", "Замечательные пределы" to "video", "Непрерывность" to "video"),
                        "Дифференцирование" to listOf("Производная" to "video", "Правила дифференцирования" to "video", "Приложения" to "video"),
                        "Интегрирование" to listOf("Неопределённый интеграл" to "video", "Определённый интеграл" to "video", "Приложения интегралов" to "video"))),
                CourseTemplate("Теория вероятностей и статистика", "Вероятности, распределения, проверка гипотез", 45, 14, 3,
                    listOf("Вероятность" to listOf("Аксиомы вероятности" to "video", "Условная вероятность" to "video", "Формула Байеса" to "video"),
                        "Распределения" to listOf("Дискретные распределения" to "video", "Непрерывные распределения" to "video", "ЦПТ" to "video"),
                        "Статистика" to listOf("Оценки параметров" to "video", "Доверительные интервалы" to "video", "Проверка гипотез" to "video"))),
                CourseTemplate("Дискретная математика", "Логика, графы, комбинаторика", 40, 14, 2,
                    listOf("Логика" to listOf("Высказывания" to "video", "Предикаты" to "video", "Доказательства" to "video"),
                        "Комбинаторика" to listOf("Перестановки" to "video", "Сочетания" to "video", "Рекуррентные соотношения" to "video"),
                        "Графы" to listOf("Основные понятия" to "video", "Деревья" to "video", "Планарность" to "video"))),

                // --- Информационная безопасность ---
                CourseTemplate("Основы информационной безопасности", "Защита информации и криптография", 40, 9, 2,
                    listOf("Основы ИБ" to listOf("Угрозы и уязвимости" to "video", "Модели безопасности" to "video", "Аутентификация" to "video"),
                        "Криптография" to listOf("Симметричное шифрование" to "video", "Асимметричное шифрование" to "video", "Хеш-функции" to "video"),
                        "Сетевая безопасность" to listOf("Firewalls" to "video", "VPN" to "video", "SSL/TLS" to "video"))),
                CourseTemplate("Ethical Hacking", "Тестирование на проникновение", 50, 12, 7,
                    listOf("Разведка" to listOf("Пассивная разведка" to "video", "Активная разведка" to "video", "OSINT" to "video"),
                        "Эксплуатация" to listOf("Web-уязвимости" to "video", "SQL Injection" to "video", "XSS" to "video"),
                        "Пост-эксплуатация" to listOf("Повышение привилегий" to "video", "Сохранение доступа" to "video", "Отчётность" to "video"))),

                // --- Управление проектами ---
                CourseTemplate("Agile и Scrum", "Гибкая разработка программного обеспечения", 20, 7, 6,
                    listOf("Agile" to listOf("Манифест Agile" to "video", "Принципы" to "video", "Kanban" to "video"),
                        "Scrum" to listOf("Роли" to "video", "Церемонии" to "video", "Артефакты" to "video"))),
                CourseTemplate("Управление IT-проектами", "PMI, риски, планирование", 35, 8, 6,
                    listOf("Основы PM" to listOf("Жизненный цикл проекта" to "video", "Заинтересованные стороны" to "video", "Устав проекта" to "video"),
                        "Планирование" to listOf("WBS" to "video", "Диаграмма Ганта" to "video", "Оценка сроков" to "video"),
                        "Контроль" to listOf("Управление рисками" to "video", "Earned Value" to "video", "Ретроспектива" to "video"))),

                // --- Другие IT курсы ---
                CourseTemplate("Компьютерные сети", "Модель OSI, TCP/IP, маршрутизация", 45, 13, 2,
                    listOf("Основы сетей" to listOf("Модель OSI" to "video", "TCP/IP" to "video", "DNS и DHCP" to "video"),
                        "Маршрутизация" to listOf("IP адресация" to "video", "Маршрутизация" to "video", "NAT" to "video"),
                        "Прикладной уровень" to listOf("HTTP/HTTPS" to "video", "FTP и SSH" to "video", "WebSocket" to "video"))),
                CourseTemplate("Операционные системы", "Процессы, память, файловые системы", 55, 14, 2,
                    listOf("Процессы" to listOf("Процессы и потоки" to "video", "Планирование" to "video", "Синхронизация" to "video"),
                        "Память" to listOf("Виртуальная память" to "video", "Страничная организация" to "video", "Сегментация" to "video"),
                        "Файловые системы" to listOf("Структура ФС" to "video", "Журналирование" to "video", "RAID" to "video"))),
                CourseTemplate("Архитектура ПО", "Паттерны, микросервисы, DDD", 50, 6, 3,
                    listOf("Паттерны" to listOf("SOLID" to "video", "GoF паттерны" to "video", "Архитектурные паттерны" to "video"),
                        "Микросервисы" to listOf("Монолит vs Микросервисы" to "video", "API Gateway" to "video", "Event-driven" to "video"),
                        "DDD" to listOf("Домен и контексты" to "video", "Агрегаты" to "video", "CQRS и Event Sourcing" to "video"))),
                CourseTemplate("Тестирование ПО", "Unit-тесты, TDD, автоматизация", 35, 5, 6,
                    listOf("Основы тестирования" to listOf("Виды тестирования" to "video", "Test Plan" to "video", "Баг-репорты" to "video"),
                        "Автоматизация" to listOf("JUnit/TestNG" to "video", "Selenium" to "video", "Cypress" to "video"),
                        "TDD и BDD" to listOf("Test-Driven Development" to "video", "Behaviour-Driven" to "video", "Моки и стабы" to "video"))),
                CourseTemplate("UX/UI дизайн", "Проектирование интерфейсов", 30, 8, 7,
                    listOf("UX Research" to listOf("Персоны" to "video", "User Journey" to "video", "Юзабилити-тестирование" to "video"),
                        "UI Design" to listOf("Типографика" to "video", "Цветовые схемы" to "video", "Компонентный подход" to "video"),
                        "Инструменты" to listOf("Figma основы" to "video", "Прототипирование" to "video", "Design System" to "video"))),
                CourseTemplate("Облачные технологии", "AWS, GCP, Azure", 45, 10, 3,
                    listOf("AWS" to listOf("EC2 и S3" to "video", "Lambda" to "video", "RDS" to "video"),
                        "GCP" to listOf("Compute Engine" to "video", "Cloud Functions" to "video", "BigQuery" to "video"),
                        "Azure" to listOf("Virtual Machines" to "video", "Azure Functions" to "video", "Cosmos DB" to "video"))),
                CourseTemplate("GraphQL", "Альтернатива REST API", 25, 4, 5,
                    listOf("Основы GraphQL" to listOf("Схема и типы" to "video", "Queries" to "video", "Mutations" to "video"),
                        "Практика" to listOf("Apollo Server" to "video", "Apollo Client" to "video", "Subscriptions" to "video"))),
                CourseTemplate("Микросервисы на Go", "Бэкенд на Go с gRPC", 45, 2, 1,
                    listOf("Go основы" to listOf("Установка Go" to "pdf", "Типы и структуры" to "video", "Горутины" to "video"),
                        "Веб-сервисы" to listOf("HTTP сервер" to "video", "REST API" to "video", "gRPC" to "video"),
                        "Инфраструктура" to listOf("Docker и Go" to "video", "Мониторинг" to "video", "Тестирование" to "video"))),
                CourseTemplate("Rust для системных программистов", "Безопасное системное программирование", 55, 2, 1,
                    listOf("Основы Rust" to listOf("Ownership" to "video", "Borrowing" to "video", "Lifetimes" to "video"),
                        "Структуры данных" to listOf("Enum и Pattern Matching" to "video", "Traits" to "video", "Generics" to "video"),
                        "Практика" to listOf("CLI приложения" to "video", "Сетевое программирование" to "video", "Unsafe Rust" to "video"))),
            )

            // Создаём курсы
            val courses = courseTemplates.map { tmpl ->
                createCourse(
                    session, tmpl.name, tmpl.desc, tmpl.duration,
                    teachers[tmpl.teacherIdx], hostings[tmpl.hostingIdx],
                    tmpl.modules
                )
            }

            println("   📚 Создано курсов: ${courses.size}")
            println("   📦 Модулей: ${courses.sumOf { it.modules.size }}")
            println("   📝 Уроков: ${courses.sumOf { it.modules.sumOf { m -> m.lessons.size } }}")

            // ==================== ТРЕКИ ПРОХОЖДЕНИЯ ====================
            val random = Random(42) // фиксированный seed для воспроизводимости
            var trackCount = 0

            for (student in students) {
                // Каждый студент записан на 2-6 случайных курсов
                val numCourses = random.nextInt(2, 7)
                val selectedCourses = courses.shuffled(random).take(numCourses)

                for (course in selectedCourses) {
                    val allLessons = getAllLessons(course)
                    if (allLessons.isEmpty()) continue

                    // Случайный прогресс: 0-100% уроков завершено
                    val completionRate = random.nextDouble()
                    val completedCount = (allLessons.size * completionRate).toInt()
                    val isFinished = completedCount == allLessons.size

                    val startDate = LocalDate.of(2025, random.nextInt(1, 13), random.nextInt(1, 29))

                    val track = ProgressTrack().apply {
                        this.user = student
                        this.course = course
                        this.status = if (isFinished) "завершён" else "начат"
                        this.startDate = startDate
                    }
                    session.persist(track)

                    if (completedCount > 0) {
                        track.completedLessons.addAll(allLessons.take(completedCount))
                        session.merge(track)
                    }
                    trackCount++
                }
            }

            tx.commit()
            println("✅ Тестовые данные загружены!")
            println("   👤 Пользователей: ${2 + teachers.size + students.size} (2 админа, ${teachers.size} преподавателей, ${students.size} студентов)")
            println("   🏠 Хостингов: ${hostings.size}")
            println("   📚 Курсов: ${courses.size}")
            println("   📈 Треков прохождения: $trackCount")

        } catch (e: Exception) {
            tx.rollback()
            println("❌ Ошибка при сидинге: ${e.message}")
            e.printStackTrace()
        } finally {
            session.close()
        }
    }

    private fun createUser(session: Session, login: String, password: String, role: String): User {
        val user = User().apply { this.login = login; this.password = password; this.role = role }
        session.persist(user)
        return user
    }

    private fun createHosting(session: Session, name: String, url: String): Hosting {
        val hosting = Hosting().apply { this.name = name; this.webAddress = url }
        session.persist(hosting)
        return hosting
    }

    private fun createCourse(
        session: Session, name: String, desc: String, duration: Int,
        teacher: Teacher, hosting: Hosting,
        structure: List<Pair<String, List<Pair<String, String>>>>
    ): Course {
        val course = Course().apply {
            this.name = name; description = desc; this.duration = duration
            this.teacher = teacher; this.hosting = hosting
        }
        session.persist(course)

        for ((modName, lessons) in structure) {
            val module = Module().apply { this.name = modName; this.course = course }
            session.persist(module)
            for ((lesName, lesType) in lessons) {
                val lesson = Lesson().apply { this.name = lesName; type = lesType; this.module = module }
                session.persist(lesson)
                module.lessons.add(lesson)
            }
            course.modules.add(module)
        }
        return course
    }

    private fun getAllLessons(course: Course): List<Lesson> {
        return course.modules.flatMap { it.lessons }
    }
}