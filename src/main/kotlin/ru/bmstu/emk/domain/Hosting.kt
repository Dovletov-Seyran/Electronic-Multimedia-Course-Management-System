package ru.bmstu.emk.domain

import jakarta.persistence.*

@Entity
@Table(name = "hostings")
class Hosting(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(nullable = false, length = 100)
    var name: String = "",

    @Column(nullable = false, length = 255)
    var webAddress: String = "",

    @OneToMany(mappedBy = "hosting", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var courses: MutableList<Course> = mutableListOf()
)