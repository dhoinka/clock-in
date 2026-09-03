package com.gloomstone.clockin.iam.controller

import com.gloomstone.clockin.config.AppConfig
import com.gloomstone.clockin.config.security.JwtResourceServerConfig
import com.gloomstone.clockin.config.security.WebSecurityConfig
import com.gloomstone.clockin.iam.controller.mgmt.RoleMgmtController
import com.gloomstone.clockin.iam.controller.mgmt.dto.RoleDto
import com.gloomstone.clockin.iam.domain.Role
import com.gloomstone.clockin.iam.mapper.RoleMapper
import com.gloomstone.clockin.iam.service.RoleService
import com.gloomstone.clockin.shared.testutil.token
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.json.JsonMapper

@WebMvcTest(RoleMgmtController::class)
@Import(WebSecurityConfig::class, JwtResourceServerConfig::class, AppConfig::class)
class RoleMgmtControllerTests {
    @Autowired
    lateinit var mvc: MockMvc

    @Autowired
    lateinit var mapper: JsonMapper

    @MockitoBean
    lateinit var roleService: RoleService

    @MockitoBean
    lateinit var roleMapper: RoleMapper

    @Test
    fun `POST creates a role for an administrator`() {
        val request = RoleDto(name = "test role")
        val role = Role(name = request.name!!, id = 1)
        given(roleService.create(request)).willReturn(role)
        given(roleMapper.toDto(role)).willReturn(RoleDto(id = 1, name = request.name))

        mvc.perform(
            post("/mgmt/roles")
                .with(token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("test role"))
    }

    @Test
    fun `GET returns roles for an administrator`() {
        val role = Role(name = "manager", id = 2)
        given(roleService.find()).willReturn(listOf(role))
        given(roleMapper.toDto(role)).willReturn(RoleDto(id = 2, name = "manager"))

        mvc.perform(get("/mgmt/roles").with(token()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].name").value("manager"))
    }

    @Test
    fun `GET one returns the requested role`() {
        val role = Role(name = "manager", id = 3)
        given(roleService.findById(3)).willReturn(role)
        given(roleMapper.toDto(role)).willReturn(RoleDto(id = 3, name = "manager"))

        mvc.perform(get("/mgmt/roles/3").with(token()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(3))
    }

    @Test
    fun `PUT updates a role`() {
        val request = RoleDto(name = "updated role")
        val role = Role(name = request.name!!, id = 4)
        given(roleService.update(4, request)).willReturn(role)
        given(roleMapper.toDto(role)).willReturn(RoleDto(id = 4, name = request.name))

        mvc.perform(
            put("/mgmt/roles/4")
                .with(token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("updated role"))
    }

    @Test
    fun `management endpoints reject authenticated users without admin authority`() {
        mvc.perform(get("/mgmt/roles").with(token("alice")))
            .andExpect(status().isForbidden)
    }

}
