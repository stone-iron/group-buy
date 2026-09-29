package com.livegroupbuy.backend.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class HealthControllerTest {

    private final HealthController controller = new HealthController();

    @Test
    void healthReturnsUpStatus() {
        Map<String, String> response = controller.health();

        assertThat(response)
                .containsEntry("status", "UP")
                .containsEntry("service", "live-group-buy-backend");
    }
}
