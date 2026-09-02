package com.gloomstone.clockin.iam.domain

import jakarta.persistence.*

/**
 * Created by daniel on 12.05.17.
 */
@Entity
@Table(name = "account")
data class Account(
    @OneToOne(mappedBy = "account")
    var user: User? = null,
    var password: String? = null,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
)

