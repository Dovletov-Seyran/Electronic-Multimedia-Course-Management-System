package ru.bmstu.emk.domain

import jakarta.persistence.*

@Entity
@Table(name = "lessons")
class Lesson(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(nullable = false, length = 100)
    var name: String = "",

    @Column(nullable = false, length = 50)
    var type: String = "",  // "video", "pdf", "text"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    var module: Module = Module()
)