package com.gloomstone.clockin.iam.repository

import com.gloomstone.clockin.iam.domain.User
import net.datafaker.Faker
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest

/**
 * Created by daniel on 13.06.17.
 */
@DataJpaTest
class UserRepositoryTests {
    @Autowired
    lateinit var userRepository: UserRepository

    val faker = Faker()

    @Test
    fun testCreate() {
        val email = faker.internet().emailAddress()
        var user = User(email, email, email)
        user = userRepository.save(user)
        assertThat(user.id).isNotEmpty()
        assertThat(user.email).isEqualTo(email)
    }

    @Test
    fun testFindOne() {
        val email = faker.internet().emailAddress()
        val user = userRepository.save(User(email, email, email, active = true))
        val foundUser = userRepository.findById(user.id)
        assertThat(foundUser?.id).isEqualTo(user.id)
    }

    @Test
    fun testFind() {
        val email = faker.internet().emailAddress()
        userRepository.save(User(email, email, faker.internet().uuid()))
        val users = userRepository.findAll()
        assertThat(users).isNotEmpty
    }

    @Test
    fun testFindByName() {
        val email = faker.internet().emailAddress()
        var user = User(email, email, faker.internet().uuid(), active = true)
        user = userRepository.save(user)
        val foundUser = userRepository.findOneByEmail(email)
        if (foundUser == null) {
            fail("user not found")
        } else {
            assertThat(foundUser.id).isEqualTo(user.id)
            assertThat(foundUser.email).isEqualTo(user.email)
        }
    }

    @Test
    fun testUpdate() {
        val email = faker.internet().emailAddress()
        var user = User(email, email, faker.internet().uuid())
        user = userRepository.save(user)
        val newEmail = faker.internet().emailAddress()
        user.email = newEmail
        val updatedUser = userRepository.save(user)
        assertThat(updatedUser.id).isEqualTo(user.id)
        assertThat(updatedUser.email).isEqualTo(user.email)
    }

    @Test
    fun testRemove() {
        val email = faker.internet().emailAddress()
        var user = User(email, email, faker.internet().uuid())
        user = userRepository.save(user)
        assertThat(user.id).isNotEmpty()
        userRepository.delete(user)

        val foundUser = userRepository.findById(user.id)
        assertThat(foundUser).isNull()
    }
}
