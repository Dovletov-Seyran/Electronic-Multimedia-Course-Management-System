package ru.bmstu.emk.service

import ru.bmstu.emk.domain.*
import ru.bmstu.emk.util.HibernateUtil

object CourseService {

    fun getAllCourses(): List<Course> {
        val session = HibernateUtil.sessionFactory.openSession()
        return try {
            session.createQuery(
                "FROM Course c JOIN FETCH c.teacher JOIN FETCH c.hosting ORDER BY c.name",
                Course::class.java
            ).resultList
        } finally {
            session.close()
        }
    }

    fun searchCourses(query: String): List<Course> {
        val session = HibernateUtil.sessionFactory.openSession()
        return try {
            session.createQuery(
                "FROM Course c JOIN FETCH c.teacher JOIN FETCH c.hosting WHERE LOWER(c.name) LIKE :q OR LOWER(c.description) LIKE :q ORDER BY c.name",
                Course::class.java
            ).setParameter("q", "%${query.lowercase()}%")
                .resultList
        } finally {
            session.close()
        }
    }

    fun getCourseWithModules(courseId: Long): Course? {
        val session = HibernateUtil.sessionFactory.openSession()
        return try {
            val course = session.createQuery(
                "FROM Course c JOIN FETCH c.teacher JOIN FETCH c.hosting WHERE c.id = :id",
                Course::class.java
            ).setParameter("id", courseId).uniqueResult() ?: return null

            // Подгружаем модули и уроки
            org.hibernate.Hibernate.initialize(course.modules)
            course.modules.forEach { org.hibernate.Hibernate.initialize(it.lessons) }
            course
        } finally {
            session.close()
        }
    }

    fun getCoursesForTeacher(teacherId: Long): List<Course> {
        val session = HibernateUtil.sessionFactory.openSession()
        return try {
            session.createQuery(
                "FROM Course c JOIN FETCH c.teacher JOIN FETCH c.hosting WHERE c.teacher.id = :tid ORDER BY c.name",
                Course::class.java
            ).setParameter("tid", teacherId).resultList
        } finally {
            session.close()
        }
    }
}