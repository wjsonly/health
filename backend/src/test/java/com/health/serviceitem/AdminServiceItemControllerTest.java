package com.health.serviceitem;

import com.health.admin.AdminAuthTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin-items;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=none"
})
@AutoConfigureMockMvc
class AdminServiceItemControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    String authorization;

    @BeforeEach
    void setUp() throws Exception {
        authorization = AdminAuthTestSupport.adminBearerToken(mockMvc);
        jdbcTemplate.update("delete from service_items where name like '后台测试%'");
        jdbcTemplate.update("delete from service_categories where name like '后台测试%'");
    }

    @Test
    void adminListIncludesInactiveItems() throws Exception {
        jdbcTemplate.update("""
                insert into service_items (
                    category_id, name, image_url, duration_minutes, original_price, sale_price,
                    suitable_people, highlights, notice, hot, recommended, status, sort_order
                ) values (1, '后台测试下架项目', '', 30, 99, 88, '测试人群', '亮点一\n亮点二',
                          '测试须知', false, false, 'INACTIVE', -50)
                """);

        mockMvc.perform(get("/api/admin/service-items").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].name", hasItem("后台测试下架项目")))
                .andExpect(jsonPath("$.data[0].name").value("后台测试下架项目"))
                .andExpect(jsonPath("$.data[0].highlights").value("亮点一\n亮点二"));

        mockMvc.perform(get("/api/service-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].name", not(hasItem("后台测试下架项目"))));
    }

    @Test
    void createsAndUpdatesAllServiceItemFields() throws Exception {
        mockMvc.perform(post("/api/admin/service-items")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(serviceItemJson("后台测试新项目", "ACTIVE", 15)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("后台测试新项目"))
                .andExpect(jsonPath("$.data.imageUrl").value("/uploads/demo.webp"))
                .andExpect(jsonPath("$.data.highlights").value("舒缓放松\n专业服务"))
                .andExpect(jsonPath("$.data.hot").value(true));

        Long id = jdbcTemplate.queryForObject(
                "select id from service_items where name = '后台测试新项目'", Long.class);

        mockMvc.perform(put("/api/admin/service-items/{id}", id)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(serviceItemJson("后台测试已编辑项目", "INACTIVE", 25)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("后台测试已编辑项目"))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"))
                .andExpect(jsonPath("$.data.sortOrder").value(25));

        mockMvc.perform(patch("/api/admin/service-items/{id}/status", id)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void rejectsInvalidServiceItemValuesAndDisabledCategory() throws Exception {
        jdbcTemplate.update("insert into service_categories (name, sort_order, enabled) values ('后台测试停用分类', 90, false)");
        Long categoryId = jdbcTemplate.queryForObject(
                "select id from service_categories where name = '后台测试停用分类'", Long.class);

        mockMvc.perform(post("/api/admin/service-items")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(serviceItemJson("后台测试非法项目", "ACTIVE", 10)
                                .replace("\"categoryId\":1", "\"categoryId\":" + categoryId)
                                .replace("\"durationMinutes\":60", "\"durationMinutes\":0")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/admin/service-items")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(serviceItemJson("后台测试停用分类项目", "ACTIVE", 10)
                                .replace("\"categoryId\":1", "\"categoryId\":" + categoryId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("服务分类不存在或已停用"));
    }

    @Test
    void managesCategoriesAndRejectsDuplicateNames() throws Exception {
        mockMvc.perform(post("/api/admin/service-categories")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"后台测试分类\",\"sortOrder\":80,\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("后台测试分类"));

        Long categoryId = jdbcTemplate.queryForObject(
                "select id from service_categories where name = '后台测试分类'", Long.class);

        mockMvc.perform(put("/api/admin/service-categories/{id}", categoryId)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"后台测试分类已编辑\",\"sortOrder\":81,\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sortOrder").value(81));

        mockMvc.perform(post("/api/admin/service-categories")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"后台测试分类已编辑\",\"sortOrder\":82,\"enabled\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("服务分类名称不能重复"));
    }

    @Test
    void cannotDisableCategoryContainingActiveItems() throws Exception {
        mockMvc.perform(patch("/api/admin/service-categories/1/status")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("分类下存在上架项目，不能停用"));
    }

    private String serviceItemJson(String name, String status, int sortOrder) {
        return """
                {
                  "categoryId":1,
                  "name":"%s",
                  "imageUrl":"/uploads/demo.webp",
                  "durationMinutes":60,
                  "originalPrice":198.00,
                  "salePrice":168.00,
                  "suitablePeople":"久坐与肩颈疲劳人群",
                  "highlights":"舒缓放松\\n专业服务",
                  "notice":"请提前十分钟到店",
                  "hot":true,
                  "recommended":false,
                  "status":"%s",
                  "sortOrder":%d
                }
                """.formatted(name, status, sortOrder);
    }
}
