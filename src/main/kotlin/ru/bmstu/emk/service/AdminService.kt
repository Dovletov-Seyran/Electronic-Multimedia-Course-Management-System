package ru.bmstu.emk.service

import ru.bmstu.emk.domain.*
import ru.bmstu.emk.util.HibernateUtil

data class PlatformStats(
    val hostingName: String,
    val webAddress: String,
    val courseCount: Int,
    val studentCount: Int
)

data class CourseStats(
    val courseName: String,
    val teacherName: String,
    val hostingName: String,
    val duration: Int,
    val enrolledCount: Int,
    val finishedCount: Int,
    val avgProgress: Double
)

object AdminService {

    fun getAllHostings(): List<Hosting> {
        val session = HibernateUtil.sessionFactory.openSession()
        return try {
            session.createQuery("FROM Hosting ORDER BY name", Hosting::class.java).resultList
        } finally {
            session.close()
        }
    }

    fun createHosting(name: String, webAddress: String): Boolean {
        val session = HibernateUtil.sessionFactory.openSession()
        val tx = session.beginTransaction()
        return try {
            val h = Hosting().apply { this.name = name; this.webAddress = webAddress }
            session.persist(h); tx.commit(); true
        } catch (e: Exception) { tx.rollback(); false }
        finally { session.close() }
    }

    fun updateHosting(id: Long, name: String, webAddress: String): Boolean {
        val session = HibernateUtil.sessionFactory.openSession()
        val tx = session.beginTransaction()
        return try {
            val h = session.get(Hosting::class.java, id)
            h.name = name; h.webAddress = webAddress
            session.merge(h); tx.commit(); true
        } catch (e: Exception) { tx.rollback(); false }
        finally { session.close() }
    }

    fun deleteHosting(id: Long): Pair<Boolean, String> {
        val session = HibernateUtil.sessionFactory.openSession()
        return try {
            val courseCount = session.createQuery(
                "SELECT COUNT(c) FROM Course c WHERE c.hosting.id = :hid", Long::class.java
            ).setParameter("hid", id).singleResult
            if (courseCount > 0) {
                return Pair(false, "Нельзя удалить хостинг: к нему привязано $courseCount курсов")
            }
            val tx = session.beginTransaction()
            session.createNativeMutationQuery("DELETE FROM hostings WHERE id = :hid").setParameter("hid", id).executeUpdate()
            tx.commit()
            Pair(true, "")
        } catch (e: Exception) { Pair(false, e.message ?: "Ошибка") }
        finally { session.close() }
    }

    fun getTotalStudents(): Long {
        val session = HibernateUtil.sessionFactory.openSession()
        return try {
            session.createQuery("SELECT COUNT(u) FROM User u WHERE u.role = 'STUDENT'", Long::class.java).singleResult
        } finally { session.close() }
    }

    fun getTotalTeachers(): Long {
        val session = HibernateUtil.sessionFactory.openSession()
        return try {
            session.createQuery("SELECT COUNT(t) FROM Teacher t", Long::class.java).singleResult
        } finally { session.close() }
    }

    fun getTotalCourses(): Long {
        val session = HibernateUtil.sessionFactory.openSession()
        return try {
            session.createQuery("SELECT COUNT(c) FROM Course c", Long::class.java).singleResult
        } finally { session.close() }
    }

    fun getTotalTracks(): Long {
        val session = HibernateUtil.sessionFactory.openSession()
        return try {
            session.createQuery("SELECT COUNT(pt) FROM ProgressTrack pt", Long::class.java).singleResult
        } finally { session.close() }
    }

    fun getFinishedTracks(): Long {
        val session = HibernateUtil.sessionFactory.openSession()
        return try {
            session.createQuery("SELECT COUNT(pt) FROM ProgressTrack pt WHERE pt.status = 'завершён'", Long::class.java).singleResult
        } finally { session.close() }
    }

    fun getPlatformStats(): List<PlatformStats> {
        val session = HibernateUtil.sessionFactory.openSession()
        return try {
            val hostings = session.createQuery("FROM Hosting ORDER BY name", Hosting::class.java).resultList
            hostings.map { h ->
                val courseCount = session.createQuery(
                    "SELECT COUNT(c) FROM Course c WHERE c.hosting.id = :hid", Long::class.java
                ).setParameter("hid", h.id).singleResult.toInt()
                val studentCount = session.createQuery(
                    "SELECT COUNT(DISTINCT pt.user.id) FROM ProgressTrack pt WHERE pt.course.hosting.id = :hid", Long::class.java
                ).setParameter("hid", h.id).singleResult.toInt()
                PlatformStats(h.name, h.webAddress, courseCount, studentCount)
            }
        } finally { session.close() }
    }

    fun getCourseStats(): List<CourseStats> {
        val session = HibernateUtil.sessionFactory.openSession()
        return try {
            val courses = session.createQuery(
                "FROM Course c JOIN FETCH c.teacher JOIN FETCH c.hosting ORDER BY c.name", Course::class.java
            ).resultList
            courses.map { c ->
                val tracks = session.createQuery(
                    "FROM ProgressTrack pt WHERE pt.course.id = :cid", ProgressTrack::class.java
                ).setParameter("cid", c.id).resultList
                val enrolled = tracks.size
                val finished = tracks.count { it.status == "завершён" }
                val totalLessons = session.createQuery(
                    "SELECT COUNT(l) FROM Lesson l WHERE l.module.course.id = :cid", Long::class.java
                ).setParameter("cid", c.id).singleResult.toInt()
                val avgProgress = if (enrolled > 0 && totalLessons > 0) {
                    tracks.map { t ->
                        org.hibernate.Hibernate.initialize(t.completedLessons)
                        t.completedLessons.size.toDouble() / totalLessons * 100
                    }.average()
                } else 0.0
                CourseStats(c.name, c.teacher.fullName, c.hosting.name, c.duration, enrolled, finished, avgProgress)
            }
        } finally { session.close() }
    }

    fun getAllStudents(): List<User> {
        val session = HibernateUtil.sessionFactory.openSession()
        return try {
            session.createQuery("FROM User u WHERE u.role = 'STUDENT' ORDER BY u.login", User::class.java).resultList
        } finally { session.close() }
    }

    fun getAllUsers(): List<User> {
        val session = HibernateUtil.sessionFactory.openSession()
        return try {
            session.createQuery("FROM User u ORDER BY u.login", User::class.java).resultList
        } finally { session.close() }
    }

    fun promoteToTeacher(userId: Long, fullName: String, email: String): Boolean {
        val session = HibernateUtil.sessionFactory.openSession()
        val tx = session.beginTransaction()
        return try {
            val user = session.get(User::class.java, userId) ?: return false
            user.role = "TEACHER"
            session.merge(user)

            val teacher = Teacher().apply {
                this.fullName = fullName
                this.email = email
                this.user = user
            }
            session.persist(teacher)
            tx.commit()
            true
        } catch (e: Exception) { tx.rollback(); false }
        finally { session.close() }
    }

    fun promoteToAdmin(userId: Long): Boolean {
        val session = HibernateUtil.sessionFactory.openSession()
        val tx = session.beginTransaction()
        return try {
            val user = session.get(User::class.java, userId) ?: return false

            // Если был преподавателем — удаляем Teacher запись (если нет курсов)
            if (user.role == "TEACHER") {
                val teacher = session.createQuery(
                    "FROM Teacher t WHERE t.user.id = :uid", Teacher::class.java
                ).setParameter("uid", userId).uniqueResult()
                if (teacher != null) {
                    val courseCount = session.createQuery(
                        "SELECT COUNT(c) FROM Course c WHERE c.teacher.id = :tid", Long::class.java
                    ).setParameter("tid", teacher.id).singleResult
                    if (courseCount > 0) return false
                    session.remove(teacher)
                }
            }

            user.role = "ADMIN"
            session.merge(user)
            tx.commit()
            true
        } catch (e: Exception) { tx.rollback(); false }
        finally { session.close() }
    }

    fun demoteToStudent(userId: Long): Boolean {
        val session = HibernateUtil.sessionFactory.openSession()
        val tx = session.beginTransaction()
        return try {
            val user = session.get(User::class.java, userId) ?: return false

            // Удаляем запись Teacher если есть
            val teacher = session.createQuery(
                "FROM Teacher t WHERE t.user.id = :uid", Teacher::class.java
            ).setParameter("uid", userId).uniqueResult()

            if (teacher != null) {
                // Проверяем, нет ли курсов у преподавателя
                val courseCount = session.createQuery(
                    "SELECT COUNT(c) FROM Course c WHERE c.teacher.id = :tid", Long::class.java
                ).setParameter("tid", teacher.id).singleResult
                if (courseCount > 0) return false // Нельзя понизить — есть курсы
                session.remove(teacher)
            }

            user.role = "STUDENT"
            session.merge(user)
            tx.commit()
            true
        } catch (e: Exception) { tx.rollback(); false }
        finally { session.close() }
    }
}