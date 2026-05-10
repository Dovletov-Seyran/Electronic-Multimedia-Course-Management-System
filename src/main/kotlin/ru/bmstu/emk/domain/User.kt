package ru.bmstu.emk.domain

import jakarta.persistence.*

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(nullable = false, unique = true, length = 100)
    var login: String = "",

    @Column(nullable = false, length = 100)
    var password: String = "",

    @Column(nullable = false, length = 100)
    var role: String = "",  // "STUDENT", "TEACHER", "ADMIN"

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var progressTracks: MutableList<ProgressTrack> = mutableListOf()
)