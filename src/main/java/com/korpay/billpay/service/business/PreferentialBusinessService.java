package com.korpay.billpay.service.business;

import com.korpay.billpay.dto.response.PreferentialBusinessResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PreferentialBusinessService {

    private static final int MAX_BUSINESS_NUMBERS = 10;

    public List<PreferentialBusinessResponse> queryPreferentialBusiness(String businessNumbers) {
        String[] numbers = businessNumbers.split(",");
        List<PreferentialBusinessResponse> results = new ArrayList<>();

        for (String number : numbers) {
            String trimmed = number.trim();
            if (trimmed.isEmpty()) continue;
            if (results.size() >= MAX_BUSINESS_NUMBERS) break;

            PreferentialBusinessResponse response = createMockResponse(trimmed);
            results.add(response);
        }

        return results;
    }

    private PreferentialBusinessResponse createMockResponse(String businessNumber) {
        return PreferentialBusinessResponse.builder()
                .businessNumber(businessNumber)
                .smeCategoryGrade("일반")
                .firstHalf2025Grade("일반")
                .secondHalf2025Grade("일반")
                .build();
    }
}
