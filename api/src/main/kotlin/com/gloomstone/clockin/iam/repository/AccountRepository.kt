package com.gloomstone.clockin.iam.repository

import com.gloomstone.clockin.iam.domain.Account
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Created by daniel on 14.05.17.
 */
@Repository
interface AccountRepository : JpaRepository<Account, Long>
