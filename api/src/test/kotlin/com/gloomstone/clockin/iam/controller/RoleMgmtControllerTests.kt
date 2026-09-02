package com.gloomstone.clockin.iam.controller

import com.gloomstone.clockin.iam.controller.mgmt.dto.RoleDto
import com.gloomstone.clockin.iam.service.RoleService
import com.gloomstone.clockin.shared.testutil.token
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.json.JsonMapper

/**
 * Created by daniel on 20.06.2017.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RoleMgmtControllerTests {
    @Autowired
    lateinit var mvc: MockMvc

    @Autowired
    lateinit var mapper: JsonMapper

    @Autowired
    lateinit var roleService: RoleService

    @Test
    fun testPost() {
        val role = RoleDto(
            name = "test role",
        )
        mvc.perform(
            post("/mgmt/roles")
                .with(token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(role))
        )
            .andExpect(status().isOk)
    }

    @Test
    fun testPostWithPermissions() {
        val role = RoleDto(
            name = "test role with permissions",
        )
        mvc.perform(
            post("/mgmt/roles")
                .with(token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(role))
        )
            .andExpect(status().isOk)
    }

    @Test
    fun testGet() {
        mvc.perform(get("/mgmt/roles").with(token()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
    }

    @Test
    fun testGetOne() {
        val name = "test role name"
        val role = roleService.create(RoleDto(name = name))
        mvc.perform(get("/mgmt/roles/" + role.id).with(token()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.id").value(role.id))
    }

    @Test
    fun testPut() {
        val role = roleService.create(RoleDto(name = "test put role"))
        val request = RoleDto(
            name = "updated role name"
        )
        mvc.perform(
            put("/mgmt/roles/" + role.id)
                .with(token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
    }
}
