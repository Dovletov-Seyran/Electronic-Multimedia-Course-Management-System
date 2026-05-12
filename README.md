# АИС ЭМК — Автоматизированная информационная система «Электронные мультимедийные курсы»

Десктопное приложение на JavaFX для управления электронными курсами. Позволяет администраторам управлять пользователями и площадками, преподавателям — создавать курсы с модулями и уроками, а студентам — проходить курсы и отслеживать прогресс. Встроенный чат между студентами и преподавателями.

## Стек технологий

- **Язык:** Kotlin 2.1.20
- **UI:** JavaFX 21
- **ORM:** Hibernate 6.4.4
- **БД:** PostgreSQL 16
- **Сборка:** Gradle
- **Java:** JDK 21

## Требования

- JDK 21 (например, [Eclipse Temurin](https://adoptium.net/))
- Docker и Docker Compose (для запуска PostgreSQL)
- Git

## Быстрый запуск

### 1. Клонировать репозиторий

```bash
git clone <url-репозитория>
cd emk
```

### 2. Запустить базу данных

```bash
docker-compose up -d
```

Это поднимет контейнер PostgreSQL 16 на порту `5432` с базой `emk`.

### 3. Запустить приложение

Linux / macOS:

```bash
./gradlew run
```

Windows:

```cmd
gradlew.bat run
```

При первом запуске Hibernate автоматически создаст все таблицы, а приложение заполнит базу тестовыми данными (120 студентов, 15 преподавателей, 50 курсов).

## Учётные данные по умолчанию

| Роль          | Логин       | Пароль |
|---------------|-------------|--------|
| Администратор | admin       | admin  |
| Преподаватель | ivanov      | 123    |
| Студент       | student1    | 123    |

Преподаватели: ivanov, petrov, sidorova, kuznetsov, morozova, volkov, lebedeva, novikov, fedorova, egorov, kozlova, dmitriev, sokolova, popov, andreeva.

Студенты: student1 — student120.

Также можно зарегистрировать нового студента через экран регистрации.

## Структура проекта

```
emk/
├── docker-compose.yml              # PostgreSQL в Docker
├── build.gradle.kts                 # Конфигурация сборки
├── src/main/kotlin/ru/bmstu/emk/
│   ├── HelloApplication.kt         # JavaFX Application
│   ├── Launcher.kt                 # Точка входа (seed + launch)
│   ├── domain/                      # JPA-сущности
│   │   ├── User.kt
│   │   ├── Teacher.kt
│   │   ├── Course.kt
│   │   ├── Module.kt
│   │   ├── Lesson.kt
│   │   ├── Hosting.kt
│   │   ├── ProgressTrack.kt
│   │   └── Message.kt
│   ├── service/                     # Бизнес-логика
│   │   ├── AuthService.kt
│   │   ├── CourseService.kt
│   │   ├── ProgressService.kt
│   │   ├── TeacherService.kt
│   │   ├── AdminService.kt
│   │   └── MessageService.kt
│   ├── ui/                          # Экраны интерфейса
│   │   ├── LoginScreen.kt
│   │   ├── RegisterScreen.kt
│   │   ├── AdminDashboard.kt
│   │   ├── TeacherDashboard.kt
│   │   └── StudentDashboard.kt
│   └── util/
│       ├── HibernateUtil.kt
│       └── DataSeeder.kt
├── src/main/resources/
│   ├── hibernate.cfg.xml
│   └── dark-theme.css
```

## Подключение к БД без Docker

Если вы хотите использовать свой экземпляр PostgreSQL, создайте базу данных и при необходимости измените параметры подключения в `src/main/resources/hibernate.cfg.xml`:

```xml
<property name="hibernate.connection.url">jdbc:postgresql://localhost:5432/emk</property>
<property name="hibernate.connection.username">postgres</property>
<property name="hibernate.connection.password">labs</property>
```
