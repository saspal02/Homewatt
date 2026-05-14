package com.homewatt.insight_service.service;

import com.homewatt.insight_service.client.UsageClient;
import com.homewatt.insight_service.dto.DeviceDto;
import com.homewatt.insight_service.dto.InsightDto;
import com.homewatt.insight_service.dto.UsageDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class InsightService {

    private final UsageClient usageClient;
    private final ChatClient chatClient;

    public InsightService(UsageClient usageClient,
                          ChatClient chatClient) {

        this.usageClient = usageClient;
        this.chatClient = chatClient;
    }

    public InsightDto getSavingsTips(Long userId) {

        final UsageDto usageData =
                usageClient.getXDaysUsageForUser(userId, 3);

        double totalUsage = usageData.devices().stream()
                .mapToDouble(DeviceDto::energyConsumed)
                .sum();

        log.info(
                "Calling Gemini for userId {} with total usage {}",
                userId,
                totalUsage
        );

        String prompt = """
                Analyse the following household energy consumption data
                from the past 3 days.

                Total Energy Usage:
                %.2f kWh

                Generate a user-friendly response with the following structure:

                1. Overall Energy Consumption Summary
                2. Comparison With Average Household Usage
                3. Energy Saving Recommendations
                4. Devices or habits that may consume high energy
                5. Short concluding advice

                Keep the response:
                - Clear and easy to understand
                - Well structured
                - Concise
                - No markdown
                - No bullet symbols like * or #
                """.formatted(totalUsage);

        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        return InsightDto.builder()
                .userId(userId)
                .tips(response)
                .energyUsage(totalUsage)
                .build();
    }

    public InsightDto getOverview(Long userId) {

        final UsageDto usageData =
                usageClient.getXDaysUsageForUser(userId, 3);

        double totalUsage = usageData.devices().stream()
                .mapToDouble(DeviceDto::energyConsumed)
                .sum();

        log.info(
                "Calling Gemini for userId {} with total usage {}",
                userId,
                totalUsage
        );

        String prompt = """
                Analyse the following household energy usage data
                collected over the past 3 days.

                Device Usage Data:
                %s

                Generate a structured and user-friendly overview with:

                1. Overall usage analysis
                2. High energy consuming devices
                3. Usage patterns or observations
                4. Recommendations for reducing consumption
                5. Final energy efficiency summary

                Keep the response:
                - Clear and concise
                - Easy for non-technical users
                - Properly structured
                - No markdown formatting
                - No unnecessary symbols
                """.formatted(usageData.devices());

        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        return InsightDto.builder()
                .userId(userId)
                .tips(response)
                .energyUsage(totalUsage)
                .build();
    }
}