package com.health.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {
    private MockMvc mockMvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new ContractController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void invalidRequestBodyReturnsFailureEnvelopeWithFirstValidationMessage() throws Exception {
        mockMvc.perform(post("/contract/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.message").value("name is required"));
    }

    @Test
    void invalidRequestParamReturnsFailureEnvelopeWithFirstValidationMessage() throws Exception {
        mockMvc.perform(get("/contract/query")
                        .queryParam("keyword", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.message").value("keyword is required"));
    }

    @Test
    void directConstraintViolationReturnsFailureEnvelopeWithFirstValidationMessage() {
        Set<ConstraintViolation<QueryRequest>> violations = validator.validate(new QueryRequest(""));
        ConstraintViolationException exception = new ConstraintViolationException(violations);

        ApiResponse<Void> response = new GlobalExceptionHandler().constraintViolation(exception);

        assertThat(response.success()).isFalse();
        assertThat(response.data()).isNull();
        assertThat(response.message()).isEqualTo("keyword is required");
    }

    @Test
    void blankBadRequestMessageReturnsFallback() {
        ApiResponse<Void> response = new GlobalExceptionHandler().badRequest(new BadRequestException("  "));

        assertThat(response.success()).isFalse();
        assertThat(response.data()).isNull();
        assertThat(response.message()).isEqualTo("请求参数不合法");
    }

    @RestController
    @RequestMapping("/contract")
    static class ContractController {
        @PostMapping("/body")
        ApiResponse<Void> body(@Valid @RequestBody ContractRequest request) {
            return ApiResponse.ok(null);
        }

        @GetMapping("/query")
        ApiResponse<Void> query(@RequestParam @NotBlank(message = "keyword is required") String keyword) {
            return ApiResponse.ok(null);
        }
    }

    record ContractRequest(@NotBlank(message = "name is required") String name) {
    }

    record QueryRequest(@NotBlank(message = "keyword is required") String keyword) {
    }
}
