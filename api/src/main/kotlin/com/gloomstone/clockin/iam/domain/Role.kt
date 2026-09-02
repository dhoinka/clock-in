package com.gloomstone.clockin.iam.domain

import jakarta.persistence.*

@Entity
@Table(name = "role")
data class Role(
    var name: String,

    @ManyToMany(mappedBy = "roles")
    var users: MutableList<User> = mutableListOf(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
)
