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

    fun register(login: String, password: String, role: String): User? {
        val session = HibernateUtil.sessionFactory.openSession()
        val tx = session.beginTransaction()
        return try {
            // check if login already exists
            val existing = session.createQuery(
                "FROM User u WHERE u.login = :login", User::class.java
            ).setParameter("login", login).uniqueResult()
            if (existing != null) return null

            val user = User().apply {
                this.login = login
                this.password = password
                this.role = role
            }
            session.persist(user)
            tx.commit()
            user
        } catch (e: Exception) {
            tx.rollback()
            null
        } finally {
            session.close()
        }
    }

    fun changeLogin(userId: Long, newLogin: String): Boolean {
        val session = HibernateUtil.sessionFactory.openSession()
        val tx = session.beginTransaction()
        return try {
            // Проверяем уникальность
            val existing = session.createQuery(
                "FROM User u WHERE u.login = :login AND u.id != :uid", User::class.java
            ).setParameter("login", newLogin).setParameter("uid", userId).uniqueResult()
            if (existing != null) return false

            val user = session.get(User::class.java, userId) ?: return false
            user.login = newLogin
            session.merge(user)
            tx.commit()
            true
        } catch (e: Exception) { tx.rollback(); false }
        finally { session.close() }
    }

    fun userExists(login: String): Boolean {
        val session = HibernateUtil.sessionFactory.openSession()
        return try {
            session.createQuery(
                "SELECT COUNT(u) FROM User u WHERE u.login = :login", Long::class.java
            ).setParameter("login", login).singleResult > 0
        } finally { session.close() }
    }

    fun changePassword(userId: Long, oldPassword: String, newPassword: String): Boolean {
        val session = HibernateUtil.sessionFactory.openSession()
        val tx = session.beginTransaction()
        return try {
            val user = session.get(User::class.java, userId) ?: return false
            if (user.password != oldPassword) return false
            user.password = newPassword
            session.merge(user)
            tx.commit()
            true
        } catch (e: Exception) { tx.rollback(); false }
        finally { session.close() }
    }

    fun resetPassword(login: String, newPassword: String): Boolean {
        val session = HibernateUtil.sessionFactory.openSession()
        val tx = session.beginTransaction()
        return try {
            val user = session.createQuery(
                "FROM User u WHERE u.login = :login", User::class.java
            ).setParameter("login", login).uniqueResult() ?: return false
            user.password = newPassword
            session.merge(user)
            tx.commit()
            true
        } catch (e: Exception) { tx.rollback(); false }
        finally { session.close() }
    }

    fun updateTeacherInfo(teacherId: Long, fullName: String, email: String): Boolean {
        val session = HibernateUtil.sessionFactory.openSession()
        val tx = session.beginTransaction()
        return try {
            val teacher = session.get(Teacher::class.java, teacherId) ?: return false
            teacher.fullName = fullName
            teacher.email = email
            session.merge(teacher)
            tx.commit()
            true
        } catch (e: Exception) { tx.rollback(); false }
        finally { session.close() }
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