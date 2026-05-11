package ru.bmstu.emk.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "messages")
class Message(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(name = "sender_id", nullable = false)
    var senderId: Long = 0,

    @Column(name = "receiver_id", nullable = false)
    var receiverId: Long = 0,

    @Column(nullable = false, length = 2000)
    var text: String = "",

    @Column(nullable = false)
    var timestamp: LocalDateTime = LocalDateTime.now(),

    @Column(name = "is_read", nullable = false, columnDefinition = "boolean default false")
    var isRead: Boolean = false
)
