package com.health.therapist;

import com.health.common.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:therapists;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=none"
})
class TherapistServiceTest {
    @Autowired
    TherapistService therapistService;

    @Test
    void publicListOnlyShowsActiveVisibleTherapists() {
        List<Therapist> therapists = therapistService.listPublicTherapists(1L);
        assertThat(therapists).extracting(Therapist::getName).contains("李静", "王明");
    }

    @Test
    void cannotCreateTherapistWithDuplicateEmployeeNumberInSameStore() {
        TherapistCreateRequest request = new TherapistCreateRequest(
                1L, "重复工号", "", "FEMALE", "13800000003", "T001", 3,
                "NORMAL", "ACTIVE", "测试", "肩颈", "手法稳", "", true, true, 30
        );

        assertThatThrownBy(() -> therapistService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("同一门店下技师工号不能重复");
    }
}
