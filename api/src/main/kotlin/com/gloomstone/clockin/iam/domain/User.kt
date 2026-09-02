package com.gloomstone.clockin.iam.domain

import com.aventrix.jnanoid.jnanoid.NanoIdUtils.randomNanoId
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * Created by daniel on 03.05.2017.
 */
@Entity
@Table(name = "users")
data class User(
    @Column(unique = true)
    var email: String,
    @Column(unique = true)
    var username: String,
    var name: String,

    @OneToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "account_id")
    var account: Account? = null,

    @ManyToMany
    @JoinTable(
        name = "user_role",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "role_id")]
    )
    var roles: MutableList<Role> = mutableListOf(),

    @Column(name = "active")
    var active: Boolean = false,

    var createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Id
    var id: String = randomNanoId(),
) {
    override fun toString(): String {
        return "User(email='$email', username='$username', name='$name', active=$active, createdAt=$createdAt, updatedAt=$updatedAt, id='$id')"
    }
}
