package com.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:health;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=none"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SchemaSmokeTest {
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void migrationCreatesCoreTablesAndSeedStore() {
        Integer storeCount = jdbcTemplate.queryForObject("select count(*) from stores", Integer.class);
        Integer categoryCount = jdbcTemplate.queryForObject("select count(*) from service_categories", Integer.class);
        Integer demoUserCount = jdbcTemplate.queryForObject(
                "select count(*) from users where open_id = 'openid-demo-1'",
                Integer.class
        );
        assertThat(storeCount).isEqualTo(1);
        assertThat(categoryCount).isGreaterThanOrEqualTo(3);
        assertThat(demoUserCount).isEqualTo(1);
    }

    @Test
    void databaseAllowsHistoricalDuplicateCategoryNames() {
        jdbcTemplate.update(
                "insert into service_categories (name, sort_order, enabled) values (?, ?, ?)",
                "推拿理疗",
                999,
                true
        );

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from service_categories where name = ?",
                Integer.class,
                "推拿理疗"
        );
        assertThat(count).isGreaterThan(1);
    }
}
