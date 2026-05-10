package ru.bmstu.emk.domain

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(
    name = "progress_tracks",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "course_id"])]
)
class ProgressTrack(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(nullable = false, length = 50)
    var status: String = "начат",  // "начат" или "завершён"

    @Column(name = "start_date", nullable = false)
    var startDate: LocalDate = LocalDate.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User = User(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    var course: Course = Course(),

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "progress_track_lessons",
        joinColumns = [JoinColumn(name = "progress_track_id")],
        inverseJoinColumns = [JoinColumn(name = "lesson_id")]
    )
    var completedLessons: MutableSet<Lesson> = mutableSetOf()
)