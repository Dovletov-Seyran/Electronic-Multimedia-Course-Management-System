package ru.bmstu.emk.service

import ru.bmstu.emk.domain.*
import ru.bmstu.emk.util.HibernateUtil

data class StudentProgress(
    val login: String,
    val courseName: String,
    val status: String,
    val completedLessons: Int,
    val totalLessons: Int,
    val startDate: String
) {
    val progressPercent: Double get() = if (totalLessons > 0) completedLessons.toDouble() / totalLessons * 100 else 0.0
}

object TeacherService {

    fun getStudentsForTeacher(teacherId: Long): List<StudentProgress> {
        val session = HibernateUtil.sessionFactory.openSession()
        return try {
            val tracks = session.createQuery(
                "FROM ProgressTrack pt JOIN FETCH pt.user JOIN FETCH pt.course c WHERE c.teacher.id = :tid ORDER BY c.name, pt.user.login",
                ProgressTrack::class.java
            ).setParameter("tid", teacherId).resultList

            tracks.map { track ->
                org.hibernate.Hibernate.initialize(track.completedLessons)
                val totalLessons = session.createQuery(
                    "SELECT COUNT(l) FROM Lesson l WHERE l.module.course.id = :cid", Long::class.java
                ).setParameter("cid", track.course.id).singleResult.toInt()

                StudentProgress(
                    login = track.user.login,
                    courseName = track.course.name,
                    status = track.status,
                    completedLessons = track.completedLessons.size,
                    totalLessons = totalLessons,
                    startDate = track.startDate.toString()
                )
            }
        } finally {
            session.close()
        }
    }

    fun createCourse(name: String, description: String, duration: Int, teacherId: Long, hostingId: Long): Course? {
        val session = HibernateUtil.sessionFactory.openSession()
        val tx = session.beginTransaction()
        return try {
            val teacher = session.get(Teacher::class.java, teacherId)
            val hosting = session.get(Hosting::class.java, hostingId)
            val course = Course().apply {
                this.name = name; this.description = description; this.duration = duration
                this.teacher = teacher; this.hosting = hosting
            }
            session.persist(course)
            tx.commit()
            course
        } catch (e: Exception) {
            tx.rollback(); null
        } finally {
            session.close()
        }
    }

    fun updateCourse(courseId: Long, name: String, description: String, duration: Int, hostingId: Long): Boolean {
        val session = HibernateUtil.sessionFactory.openSession()
        val tx = session.beginTransaction()
        return try {
            val course = session.get(Course::class.java, courseId)
            val hosting = session.get(Hosting::class.java, hostingId)
            course.name = name; course.description = description; course.duration = duration; course.hosting = hosting
            session.merge(course)
            tx.commit(); true
        } catch (e: Exception) {
            tx.rollback(); false
        } finally {
            session.close()
        }
    }

    fun deleteCourse(courseId: Long): Boolean {
        val session = HibernateUtil.sessionFactory.openSession()
        val tx = session.beginTransaction()
        return try {
            session.createNativeMutationQuery("DELETE FROM progress_track_lessons WHERE progress_track_id IN (SELECT id FROM progress_tracks WHERE course_id = :cid)").setParameter("cid", courseId).executeUpdate()
            session.createNativeMutationQuery("DELETE FROM progress_tracks WHERE course_id = :cid").setParameter("cid", courseId).executeUpdate()
            session.createNativeMutationQuery("DELETE FROM lessons WHERE module_id IN (SELECT id FROM modules WHERE course_id = :cid)").setParameter("cid", courseId).executeUpdate()
            session.createNativeMutationQuery("DELETE FROM modules WHERE course_id = :cid").setParameter("cid", courseId).executeUpdate()
            session.createNativeMutationQuery("DELETE FROM courses WHERE id = :cid").setParameter("cid", courseId).executeUpdate()
            tx.commit(); true
        } catch (e: Exception) {
            tx.rollback(); false
        } finally {
            session.close()
        }
    }

    fun addModule(courseId: Long, name: String): Module? {
        val session = HibernateUtil.sessionFactory.openSession()
        val tx = session.beginTransaction()
        return try {
            val course = session.get(Course::class.java, courseId)
            val module = Module().apply { this.name = name; this.course = course }
            session.persist(module)
            tx.commit(); module
        } catch (e: Exception) {
            tx.rollback(); null
        } finally {
            session.close()
        }
    }

    fun deleteModule(moduleId: Long): Boolean {
        val session = HibernateUtil.sessionFactory.openSession()
        val tx = session.beginTransaction()
        return try {
            session.createNativeMutationQuery("DELETE FROM progress_track_lessons WHERE lesson_id IN (SELECT id FROM lessons WHERE module_id = :mid)").setParameter("mid", moduleId).executeUpdate()
            session.createNativeMutationQuery("DELETE FROM lessons WHERE module_id = :mid").setParameter("mid", moduleId).executeUpdate()
            session.createNativeMutationQuery("DELETE FROM modules WHERE id = :mid").setParameter("mid", moduleId).executeUpdate()
            tx.commit(); true
        } catch (e: Exception) {
            tx.rollback(); false
        } finally {
            session.close()
        }
    }

    fun addLesson(moduleId: Long, name: String, type: String): Lesson? {
        val session = HibernateUtil.sessionFactory.openSession()
        val tx = session.beginTransaction()
        return try {
            val module = session.get(Module::class.java, moduleId)
            val lesson = Lesson().apply { this.name = name; this.type = type; this.module = module }
            session.persist(lesson)
            tx.commit(); lesson
        } catch (e: Exception) {
            tx.rollback(); null
        } finally {
            session.close()
        }
    }

    fun deleteLesson(lessonId: Long): Boolean {
        val session = HibernateUtil.sessionFactory.openSession()
        val tx = session.beginTransaction()
        return try {
            session.createNativeMutationQuery("DELETE FROM progress_track_lessons WHERE lesson_id = :lid").setParameter("lid", lessonId).executeUpdate()
            session.createNativeMutationQuery("DELETE FROM lessons WHERE id = :lid").setParameter("lid", lessonId).executeUpdate()
            tx.commit(); true
        } catch (e: Exception) {
            tx.rollback(); false
        } finally {
            session.close()
        }
    }

    fun getAllHostings(): List<Hosting> {
        val session = HibernateUtil.sessionFactory.openSession()
        return try {
            session.createQuery("FROM Hosting ORDER BY name", Hosting::class.java).resultList
        } finally {
            session.close()
        }
    }
}