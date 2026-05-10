package ru.bmstu.emk.service

import ru.bmstu.emk.domain.*
import ru.bmstu.emk.util.HibernateUtil

data class TrackInfo(
    val track: ProgressTrack,
    val course: Course,
    val completedCount: Int,
    val totalLessons: Int
) {
    val progressPercent: Double get() = if (totalLessons > 0) completedCount.toDouble() / totalLessons * 100 else 0.0
    val isFinished: Boolean get() = track.status == "завершён"
}

object ProgressService {

    fun getTracksForUser(userId: Long): List<TrackInfo> {
        val session = HibernateUtil.sessionFactory.openSession()
        return try {
            val tracks = session.createQuery(
                "FROM ProgressTrack pt JOIN FETCH pt.course c JOIN FETCH c.teacher JOIN FETCH c.hosting WHERE pt.user.id = :uid",
                ProgressTrack::class.java
            ).setParameter("uid", userId).resultList

            tracks.map { track ->
                org.hibernate.Hibernate.initialize(track.completedLessons)
                val totalLessons = session.createQuery(
                    "SELECT COUNT(l) FROM Lesson l WHERE l.module.course.id = :cid", Long::class.java
                ).setParameter("cid", track.course.id).singleResult.toInt()

                TrackInfo(track, track.course, track.completedLessons.size, totalLessons)
            }
        } finally {
            session.close()
        }
    }

    fun isEnrolled(userId: Long, courseId: Long): Boolean {
        val session = HibernateUtil.sessionFactory.openSession()
        return try {
            val count = session.createQuery(
                "SELECT COUNT(pt) FROM ProgressTrack pt WHERE pt.user.id = :uid AND pt.course.id = :cid",
                Long::class.java
            ).setParameter("uid", userId).setParameter("cid", courseId).singleResult
            count > 0
        } finally {
            session.close()
        }
    }

    fun enroll(userId: Long, courseId: Long): Boolean {
        if (isEnrolled(userId, courseId)) return false
        val session = HibernateUtil.sessionFactory.openSession()
        val tx = session.beginTransaction()
        return try {
            val user = session.get(User::class.java, userId)
            val course = session.get(Course::class.java, courseId)
            val track = ProgressTrack().apply {
                this.user = user
                this.course = course
                this.status = "начат"
                this.startDate = java.time.LocalDate.now()
            }
            session.persist(track)
            tx.commit()
            true
        } catch (e: Exception) {
            tx.rollback()
            false
        } finally {
            session.close()
        }
    }

    fun markLessonComplete(userId: Long, courseId: Long, lessonId: Long): Boolean {
        val session = HibernateUtil.sessionFactory.openSession()
        val tx = session.beginTransaction()
        return try {
            val track = session.createQuery(
                "FROM ProgressTrack pt WHERE pt.user.id = :uid AND pt.course.id = :cid",
                ProgressTrack::class.java
            ).setParameter("uid", userId).setParameter("cid", courseId).uniqueResult() ?: return false

            if (track.status == "завершён") return false // нельзя менять завершённый трек

            org.hibernate.Hibernate.initialize(track.completedLessons)
            val lesson = session.get(Lesson::class.java, lessonId)
            track.completedLessons.add(lesson)

            // Проверяем, все ли уроки пройдены
            val totalLessons = session.createQuery(
                "SELECT COUNT(l) FROM Lesson l WHERE l.module.course.id = :cid", Long::class.java
            ).setParameter("cid", courseId).singleResult

            if (track.completedLessons.size.toLong() >= totalLessons) {
                track.status = "завершён"
            }

            session.merge(track)
            tx.commit()
            true
        } catch (e: Exception) {
            tx.rollback()
            false
        } finally {
            session.close()
        }
    }
}