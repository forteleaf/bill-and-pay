package com.korpay.billpay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreferentialBusinessResponse {
    private String businessNumber;
    private String smeCategoryGrade;  // 영중소등급
    private String firstHalf2025Grade; // 2025년 상반기
    private String secondHalf2025Grade; // 2025년 하반기
}
