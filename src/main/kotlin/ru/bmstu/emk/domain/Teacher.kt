package ru.bmstu.emk.domain

import jakarta.persistence.*

@Entity
@Table(name = "teachers")
class Teacher(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(nullable = false, length = 100)
    var fullName: String = "",

    @Column(nullable = false, length = 255)
    var email: String = "",

    @OneToOne
    @JoinColumn(name = "user_id")
    var user: User? = null,

    @OneToMany(mappedBy = "teacher", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var courses: MutableList<Course> = mutableListOf()
)