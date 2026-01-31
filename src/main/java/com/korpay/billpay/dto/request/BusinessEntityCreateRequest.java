package com.korpay.billpay.dto.request;

import com.korpay.billpay.domain.enums.BusinessType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessEntityCreateRequest {

    @NotNull(message = "사업자 유형은 필수입니다")
    private BusinessType businessType;

    @Pattern(regexp = "^\\d{3}-\\d{2}-\\d{5}$", message = "사업자번호 형식: 000-00-00000")
    private String businessNumber;

    @Pattern(regexp = "^\\d{6}-\\d{7}$", message = "법인등록번호 형식: 000000-0000000")
    private String corporateNumber;

    @NotBlank(message = "상호는 필수입니다")
    @Size(max = 200)
    private String businessName;

    @NotBlank(message = "대표자명은 필수입니다")
    @Size(max = 100)
    private String representativeName;

    private LocalDate openDate;

    @Size(max = 2000)
    private String businessAddress;

    @Size(max = 2000)
    private String actualAddress;

    @Size(max = 100)
    private String businessCategory;

    @Size(max = 100)
    private String businessSubCategory;

    @Size(max = 20)
    private String mainPhone;

    @Size(max = 100)
    private String managerName;

    @Size(max = 20)
    private String managerPhone;

    @Size(max = 255)
    private String email;
}
