package ru.bmstu.emk.util

import org.hibernate.SessionFactory
import org.hibernate.cfg.Configuration

object HibernateUtil {
    val sessionFactory: SessionFactory by lazy {
        try {
            Configuration()
                .configure("hibernate.cfg.xml")
                .buildSessionFactory()
        } catch (ex: Throwable) {
            System.err.println("SessionFactory creation failed: $ex")
            throw ExceptionInInitializerError(ex)
        }
    }

    fun shutdown() {
        sessionFactory.close()
    }
}