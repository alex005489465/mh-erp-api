package com.morningharvest.erp.option.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.morningharvest.erp.option.dto.CreateOptionTemplateGroupRequest;
import com.morningharvest.erp.option.dto.UpdateOptionTemplateGroupRequest;
import com.morningharvest.erp.option.entity.OptionTemplateGroup;
import com.morningharvest.erp.option.entity.OptionTemplateValue;
import com.morningharvest.erp.option.repository.OptionTemplateGroupRepository;
import com.morningharvest.erp.option.repository.OptionTemplateValueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("OptionTemplateGroupController 整合測試")
class OptionTemplateGroupControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OptionTemplateGroupRepository groupRepository;

    @Autowired
    private OptionTemplateValueRepository valueRepository;

    private OptionTemplateGroup testGroup;
    private OptionTemplateValue testValue;

    @BeforeEach
    void setUp() {
        valueRepository.deleteAll();
        groupRepository.deleteAll();

        testGroup = OptionTemplateGroup.builder()
                .name("甜度")
                .minSelections(1)
                .maxSelections(1)
                .sortOrder(0)
                .isActive(true)
                .build();
        testGroup = groupRepository.save(testGroup);

        testValue = OptionTemplateValue.builder()
                .groupId(testGroup.getId())
                .name("半糖")
                .priceAdjustment(BigDecimal.ZERO)
                .sortOrder(0)
                .isActive(true)
                .build();
        testValue = valueRepository.save(testValue);
    }

    @Test
    @DisplayName("POST /api/options/templates/groups/create - 建立群組成功")
    void createGroup_Success() throws Exception {
        CreateOptionTemplateGroupRequest request = CreateOptionTemplateGroupRequest.builder()
                .name("加料")
                .minSelections(0)
                .maxSelections(3)
                .sortOrder(1)
                .build();

        mockMvc.perform(post("/api/options/templates/groups/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("加料"))
                .andExpect(jsonPath("$.data.minSelections").value(0))
                .andExpect(jsonPath("$.data.maxSelections").value(3));
    }

    @Test
    @DisplayName("POST /api/options/templates/groups/create - 參數驗證失敗 (code=2001)")
    void createGroup_ValidationError() throws Exception {
        String invalidRequest = "{ \"minSelections\": 1, \"maxSelections\": 1 }";

        mockMvc.perform(post("/api/options/templates/groups/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.name").exists());
    }

    @Test
    @DisplayName("POST /api/options/templates/groups/create - 名稱重複 (code=2002)")
    void createGroup_DuplicateName() throws Exception {
        CreateOptionTemplateGroupRequest request = CreateOptionTemplateGroupRequest.builder()
                .name("甜度")
                .minSelections(1)
                .maxSelections(1)
                .build();

        mockMvc.perform(post("/api/options/templates/groups/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2002))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("群組名稱已存在: 甜度"));
    }

    @Test
    @DisplayName("POST /api/options/templates/groups/create - minSelections > maxSelections (code=2002)")
    void createGroup_InvalidSelections() throws Exception {
        CreateOptionTemplateGroupRequest request = CreateOptionTemplateGroupRequest.builder()
                .name("測試")
                .minSelections(5)
                .maxSelections(1)
                .build();

        mockMvc.perform(post("/api/options/templates/groups/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2002))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("最少選擇數不可大於最多選擇數"));
    }

    @Test
    @DisplayName("GET /api/options/templates/groups/detail - 查詢群組成功（含選項值）")
    void getGroupDetail_Success() throws Exception {
        mockMvc.perform(get("/api/options/templates/groups/detail")
                        .param("id", testGroup.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(testGroup.getId()))
                .andExpect(jsonPath("$.data.name").value("甜度"))
                .andExpect(jsonPath("$.data.values").isArray())
                .andExpect(jsonPath("$.data.values[0].name").value("半糖"));
    }

    @Test
    @DisplayName("GET /api/options/templates/groups/detail - 群組不存在 (code=3001)")
    void getGroupDetail_NotFound() throws Exception {
        mockMvc.perform(get("/api/options/templates/groups/detail")
                        .param("id", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("選項範本群組不存在: 99999"));
    }

    @Test
    @DisplayName("GET /api/options/templates/groups/list - 分頁查詢成功")
    void listGroups_Success() throws Exception {
        mockMvc.perform(get("/api/options/templates/groups/list")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/options/templates/groups/list - 篩選啟用群組")
    void listGroups_FilterByIsActive() throws Exception {
        OptionTemplateGroup inactiveGroup = OptionTemplateGroup.builder()
                .name("停用群組")
                .minSelections(0)
                .maxSelections(1)
                .isActive(false)
                .sortOrder(1)
                .build();
        groupRepository.save(inactiveGroup);

        mockMvc.perform(get("/api/options/templates/groups/list")
                        .param("page", "1")
                        .param("size", "10")
                        .param("isActive", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("甜度"));
    }

    @Test
    @DisplayName("POST /api/options/templates/groups/update - 更新群組成功")
    void updateGroup_Success() throws Exception {
        UpdateOptionTemplateGroupRequest request = UpdateOptionTemplateGroupRequest.builder()
                .id(testGroup.getId())
                .name("更新甜度")
                .minSelections(0)
                .maxSelections(1)
                .sortOrder(5)
                .build();

        mockMvc.perform(post("/api/options/templates/groups/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("更新甜度"));
    }

    @Test
    @DisplayName("POST /api/options/templates/groups/delete - 刪除群組成功")
    void deleteGroup_Success() throws Exception {
        mockMvc.perform(post("/api/options/templates/groups/delete")
                        .param("id", testGroup.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true));

        assertThat(groupRepository.findById(testGroup.getId())).isEmpty();
        assertThat(valueRepository.findByGroupIdOrderBySortOrder(testGroup.getId())).isEmpty();
    }

    @Test
    @DisplayName("POST /api/options/templates/groups/activate - 啟用群組成功")
    void activateGroup_Success() throws Exception {
        testGroup.setIsActive(false);
        groupRepository.save(testGroup);

        mockMvc.perform(post("/api/options/templates/groups/activate")
                        .param("id", testGroup.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    @Test
    @DisplayName("POST /api/options/templates/groups/deactivate - 停用群組成功")
    void deactivateGroup_Success() throws Exception {
        mockMvc.perform(post("/api/options/templates/groups/deactivate")
                        .param("id", testGroup.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isActive").value(false));
    }
}
