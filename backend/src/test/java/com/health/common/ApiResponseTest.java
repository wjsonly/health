package com.health.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiResponseTest {
    @Test
    void okWrapsDataWithSuccessEnvelope() {
        ApiResponse<String> response = ApiResponse.ok("payload");

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isEqualTo("payload");
        assertThat(response.message()).isEmpty();
    }

    @Test
    void errorWrapsMessageWithFailureEnvelope() {
        ApiResponse<Void> response = ApiResponse.error("invalid request");

        assertThat(response.success()).isFalse();
        assertThat(response.data()).isNull();
        assertThat(response.message()).isEqualTo("invalid request");
    }
}
