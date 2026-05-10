package ru.bmstu.emk.util

import ru.bmstu.emk.domain.Teacher
import ru.bmstu.emk.domain.User

object SessionManager {
    var currentUser: User? = null
    var currentTeacher: Teacher? = null

    fun logout() {
        currentUser = null
        currentTeacher = null
    }

    val isStudent get() = currentUser?.role == "STUDENT"
    val isTeacher get() = currentUser?.role == "TEACHER"
    val isAdmin get() = currentUser?.role == "ADMIN"
}