package ru.bmstu.emk.domain

import jakarta.persistence.*

@Entity
@Table(name = "courses")
class Course(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(nullable = false, length = 100)
    var name: String = "",

    @Column(length = 255)
    var description: String = "",

    @Column(nullable = false)
    var duration: Int = 0,  // в часах

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    var teacher: Teacher = Teacher(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hosting_id", nullable = false)
    var hosting: Hosting = Hosting(),

    @OneToMany(mappedBy = "course", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var modules: MutableList<Module> = mutableListOf(),

    @OneToMany(mappedBy = "course", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var progressTracks: MutableList<ProgressTrack> = mutableListOf()
)