package ru.bmstu.emk.service

import ru.bmstu.emk.domain.Teacher
import ru.bmstu.emk.domain.User
import ru.bmstu.emk.util.HibernateUtil

object AuthService {

    fun login(login: String, password: String): User? {
        val session = HibernateUtil.sessionFactory.openSession()
        return try {
            session.createQuery(
                "FROM User u WHERE u.login = :login AND u.password = :password", User::class.java
            ).setParameter("login", login)
                .setParameter("password", password)
                .uniqueResult()
        } finally {
            session.close()
        }
    }

    fun getTeacherByUser(user: User): Teacher? {
        val session = HibernateUtil.sessionFactory.openSession()
        return try {
            session.createQuery(
                "FROM Teacher t WHERE t.user.id = :userId", Teacher::class.java
            ).setParameter("userId", user.id)
                .uniqueResult()
        } finally {
            session.close()
        }
    }
}