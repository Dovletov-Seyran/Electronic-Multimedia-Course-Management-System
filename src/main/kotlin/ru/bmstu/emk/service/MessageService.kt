package ru.bmstu.emk.service

import ru.bmstu.emk.domain.Message
import ru.bmstu.emk.util.HibernateUtil
import java.time.LocalDateTime

data class ChatMessage(
    val id: Long,
    val senderId: Long,
    val receiverId: Long,
    val text: String,
    val timestamp: LocalDateTime,
    val isRead: Boolean
)

data class ContactInfo(
    val userId: Long,
    val name: String,
    val lastMessageTime: LocalDateTime?,
    val unreadCount: Int
)

object MessageService {

    fun getMessages(userId1: Long, userId2: Long): List<ChatMessage> {
        val session = HibernateUtil.sessionFactory.openSession()
        return try {
            val messages = session.createQuery(
                "FROM Message m WHERE (m.senderId = :u1 AND m.receiverId = :u2) OR (m.senderId = :u2 AND m.receiverId = :u1) ORDER BY m.timestamp ASC",
                Message::class.java
            ).setParameter("u1", userId1).setParameter("u2", userId2).resultList

            messages.map { ChatMessage(it.id, it.senderId, it.receiverId, it.text, it.timestamp, it.isRead) }
        } finally {
            session.close()
        }
    }

    fun sendMessage(senderId: Long, receiverId: Long, text: String): Boolean {
        val session = HibernateUtil.sessionFactory.openSession()
        val tx = session.beginTransaction()
        return try {
            val msg = Message().apply {
                this.senderId = senderId
                this.receiverId = receiverId
                this.text = text
                this.timestamp = LocalDateTime.now()
                this.isRead = false
            }
            session.persist(msg)
            tx.commit()
            true
        } catch (e: Exception) {
            tx.rollback()
            false
        } finally {
            session.close()
        }
    }

    /** Помечает все сообщения от otherUserId к myUserId как прочитанные */
    fun markAsRead(myUserId: Long, otherUserId: Long) {
        val session = HibernateUtil.sessionFactory.openSession()
        val tx = session.beginTransaction()
        try {
            session.createMutationQuery(
                "UPDATE Message m SET m.isRead = true WHERE m.senderId = :sender AND m.receiverId = :receiver AND m.isRead = false"
            ).setParameter("sender", otherUserId).setParameter("receiver", myUserId).executeUpdate()
            tx.commit()
        } catch (e: Exception) {
            tx.rollback()
        } finally {
            session.close()
        }
    }

    /** Количество непрочитанных сообщений от otherUserId к myUserId */
    fun getUnreadCount(myUserId: Long, otherUserId: Long): Int {
        val session = HibernateUtil.sessionFactory.openSession()
        return try {
            session.createQuery(
                "SELECT COUNT(m) FROM Message m WHERE m.senderId = :sender AND m.receiverId = :receiver AND m.isRead = false",
                Long::class.java
            ).setParameter("sender", otherUserId).setParameter("receiver", myUserId).singleResult.toInt()
        } finally {
            session.close()
        }
    }

    /** Время последнего сообщения между двумя пользователями */
    fun getLastMessageTime(userId1: Long, userId2: Long): LocalDateTime? {
        val session = HibernateUtil.sessionFactory.openSession()
        return try {
            session.createQuery(
                "SELECT MAX(m.timestamp) FROM Message m WHERE (m.senderId = :u1 AND m.receiverId = :u2) OR (m.senderId = :u2 AND m.receiverId = :u1)",
                LocalDateTime::class.java
            ).setParameter("u1", userId1).setParameter("u2", userId2).singleResult
        } finally {
            session.close()
        }
    }

    fun getConversationsForUser(userId: Long): List<Long> {
        val session = HibernateUtil.sessionFactory.openSession()
        return try {
            val senderIds = session.createQuery(
                "SELECT DISTINCT m.senderId FROM Message m WHERE m.receiverId = :uid", Long::class.java
            ).setParameter("uid", userId).resultList

            val receiverIds = session.createQuery(
                "SELECT DISTINCT m.receiverId FROM Message m WHERE m.senderId = :uid", Long::class.java
            ).setParameter("uid", userId).resultList

            (senderIds + receiverIds).distinct().filter { it != userId }
        } finally {
            session.close()
        }
    }
}
