package com.gloomstone.clockin.iam.service

import com.gloomstone.clockin.iam.domain.Account
import com.gloomstone.clockin.iam.domain.User
import com.gloomstone.clockin.iam.dto.CreateUserRequest
import com.gloomstone.clockin.iam.dto.UpdateUserRequest
import com.gloomstone.clockin.iam.dto.UpdateSelfRequest
import com.gloomstone.clockin.iam.mapper.UserMapper
import com.gloomstone.clockin.iam.repository.AccountRepository
import com.gloomstone.clockin.iam.repository.RoleRepository
import com.gloomstone.clockin.iam.repository.UserRepository
import com.gloomstone.clockin.shared.exception.*
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Created by Daniel Hoinka on 03.05.2017.
 */
@Service
@Transactional
class UserService(
    private val userRepository: UserRepository,
    private val accountRepository: AccountRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder,
    private val userMapper: UserMapper,
) {
    private val logger = LoggerFactory.getLogger(UserService::class.java)

    fun findAll(): List<User> {
        val users = userRepository.findAll()
        return users
    }

    fun findByIdentity(value: String): User? {
        val user = userRepository.findOneByUsername(value)
            ?: userRepository.findOneByEmail(value)
            ?: userRepository.findById(value)

        return user
    }

    fun create(request: CreateUserRequest): User {
        if (request.password != request.passwordRepeat) {
            throw PasswordNotEqualException()
        }
        if (request.password.isEmpty()) {
            throw BadRequestException("Password is required")
        }

        val username = request.username.lowercase()
        val email = request.email.lowercase()

        this.findByIdentity(username)?.let { throw UsernameAlreadyExistsException() }
        var user =
            User(
                email = email,
                username = username,
                name = username,
                active = request.active ?: false,
            ).apply {
                if (request.roles != null) {
                    roles = request.roles.map {
                        roleRepository.findByName(it) ?: throw BadRequestException("Role not found")
                    }
                        .toMutableList()
                }
            }

        var account = Account()
        account.password = passwordEncoder.encode(request.password)
        account = accountRepository.save(account)
        user.account = account
        user = userRepository.save(user)

        logger.info("Created: {}", user)
        return user.also {
            it.roles.size
        }
    }

    fun changePassword(user: User, password: String): User {
        user.account ?: throw InternalServerException("User has no account")
        user.account?.password = passwordEncoder.encode(password)
        userRepository.save(user)
        return user
    }

    fun changePassword(user: User, oldPassword: String, newPassword: String): User {
        val encoded = user.account?.password ?: throw InternalServerException("User has no account")
        if (!passwordEncoder.matches(oldPassword, encoded)) {
            throw AuthenticationException()
        }
        return changePassword(user, newPassword)
    }

    fun delete(value: String) {
        val user = findByIdentity(value) ?: throw NotFoundException()
        if (user.username != "admin") {
            delete(user)
        }
    }

    fun update(id: String, request: UpdateUserRequest): User {
        val user = findByIdentity(id) ?: throw NotFoundException()

        this.userMapper.update(request, user)
        user.roles = request.roles?.map { roleRepository.findByName(it) ?: throw BadRequestException("Role not found") }
            ?.toMutableList() ?: user.roles

        return this.update(user)
    }


    fun updateSelf(id: String, request: UpdateSelfRequest): User {
        val user = findByIdentity(id) ?: throw NotFoundException()
        userMapper.updateSelf(request, user)
        return update(user)
    }

    fun update(user: User): User {
        user.updatedAt = LocalDateTime.now()
        val updatedUser = userRepository.save(user)

        return updatedUser
    }

    fun delete(user: User) {
        this.userRepository.delete(user)
    }
}
