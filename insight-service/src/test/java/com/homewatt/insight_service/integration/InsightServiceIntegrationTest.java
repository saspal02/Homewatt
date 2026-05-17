package com.homewatt.insight_service.integration;

import com.homewatt.insight_service.client.UsageClient;
import com.homewatt.insight_service.dto.DeviceDto;
import com.homewatt.insight_service.dto.InsightDto;
import com.homewatt.insight_service.dto.UsageDto;
import com.homewatt.insight_service.service.InsightService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class InsightServiceIntegrationTest {

    @Autowired
    private InsightService insightService;

    @Autowired
    private UsageClient usageClient;

    @Autowired
    private ChatClient chatClient;

    @Test
    void getSavingTips_returnsInsightDto() {
        Long userId = 1L;
        when(usageClient.getXDaysUsageForUser(userId, 3))
                .thenReturn(new UsageDto(userId, List.of(
                        new DeviceDto(10L, "Fridge", "SPEAKER", "Kitchen", userId, 500.0),
                        new DeviceDto(11L, "TV", "SPEAKER", "Living Room", userId, 300.0)
                )));
        when(chatClient.prompt().user(anyString()).call().content())
                .thenReturn("Reduce your fridge temperature by 2 degrees to save energy.");

        InsightDto result = insightService.getSavingsTips(userId);

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.tips()).isEqualTo("Reduce your fridge temperature by 2 degrees to save energy.");
        assertThat(result.energyUsage()).isEqualTo(800.0);
    }

    @Test
    void getOverview_returnsInsightDto() {
        Long userId = 2L;
        when(usageClient.getXDaysUsageForUser(userId, 3))
                .thenReturn(new UsageDto(userId, List.of(
                        new DeviceDto(20L, "Heater", "THERMOSTAT", "Bedroom", userId, 1200.0)
                )));
        when(chatClient.prompt().user(anyString()).call().content())
                .thenReturn("Your heater is consuming significant energy.");

        InsightDto result = insightService.getOverview(userId);

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.tips()).isEqualTo("Your heater is consuming significant energy.");
        assertThat(result.energyUsage()).isEqualTo(1200.0);
    }

    @TestConfiguration
    static class TestMockConfiguration {

        @Bean
        @Primary
        UsageClient mockUsageClient() {
            return Mockito.mock(UsageClient.class);
        }

        @Bean
        @Primary
        ChatClient mockChatClient() {
            return Mockito.mock(ChatClient.class, Mockito.RETURNS_DEEP_STUBS);
        }
    }
}
