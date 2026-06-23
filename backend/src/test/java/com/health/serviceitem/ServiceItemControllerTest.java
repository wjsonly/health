package com.health.serviceitem;

import java.sql.PreparedStatement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:items;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=none"
})
@AutoConfigureMockMvc
class ServiceItemControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    Long inactiveItemId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from service_items where name in (?, ?)", "测试排序优先项目", "测试下架项目");
        insertServiceItem("测试排序优先项目", "ACTIVE", -10);
        inactiveItemId = insertServiceItem("测试下架项目", "INACTIVE", -20);
    }

    @Test
    void listsActiveServiceItems() throws Exception {
        mockMvc.perform(get("/api/service-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.data[*].status", everyItem(is("ACTIVE"))))
                .andExpect(jsonPath("$.data[*].name", not(hasItem("测试下架项目"))))
                .andExpect(jsonPath("$.data[0].name").value("测试排序优先项目"))
                .andExpect(jsonPath("$.data[0].sortOrder").value(-10));
    }

    @Test
    void returnsActiveServiceItemDetail() throws Exception {
        mockMvc.perform(get("/api/service-items/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("肩颈舒缓推拿"))
                .andExpect(jsonPath("$.data.categoryName").value("推拿理疗"));
    }

    @Test
    void rejectsMissingServiceItemDetail() throws Exception {
        mockMvc.perform(get("/api/service-items/999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("服务项目不存在或已下架"));
    }

    @Test
    void rejectsInactiveServiceItemDetail() throws Exception {
        mockMvc.perform(get("/api/service-items/{id}", inactiveItemId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("服务项目不存在或已下架"));
    }

    @Test
    void returnsStoreInfo() throws Exception {
        mockMvc.perform(get("/api/store"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("静养堂养生馆"))
                .andExpect(jsonPath("$.data.businessStart").value("10:00:00"));
    }

    private Long insertServiceItem(String name, String status, int sortOrder) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    insert into service_items (
                        category_id, name, image_url, duration_minutes, original_price, sale_price,
                        suitable_people, notice, hot, recommended, status, sort_order
                    )
                    values (?, ?, '', 30, 99.00, 88.00, '测试人群', '测试须知', false, false, ?, ?)
                    """, new String[] {"id"});
            statement.setLong(1, 1L);
            statement.setString(2, name);
            statement.setString(3, status);
            statement.setInt(4, sortOrder);
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }
}
