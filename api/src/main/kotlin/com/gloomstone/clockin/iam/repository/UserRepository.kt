package com.gloomstone.clockin.iam.repository

import com.gloomstone.clockin.iam.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

/**
 * Created by daniel on 14.05.17.
 */
@Repository
interface UserRepository : JpaRepository<User, Long> {
    @Query("SELECT DISTINCT user FROM User user LEFT JOIN FETCH user.roles")
    override fun findAll(): List<User>

    @Query("SELECT DISTINCT user FROM User user LEFT JOIN FETCH user.roles WHERE user.username LIKE :username")
    fun findAllByUsername(username: String): List<User>

    @Query("SELECT DISTINCT user FROM User user LEFT JOIN FETCH user.roles WHERE user.email=?1")
    fun findOneByEmail(email: String): User?

    @Query("SELECT DISTINCT user FROM User user LEFT JOIN FETCH user.roles WHERE user.username=?1")
    fun findOneByUsername(username: String): User?

    @Query(
        """
        SELECT DISTINCT user
        FROM User user
        LEFT JOIN FETCH user.roles
        WHERE user.id=?1 
        """
    )
    fun findById(id: String): User?

}
