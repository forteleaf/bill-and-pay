package com.korpay.billpay.controller.api;

import com.korpay.billpay.dto.response.ApiResponse;
import com.korpay.billpay.dto.response.PreferentialBusinessResponse;
import com.korpay.billpay.service.business.PreferentialBusinessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/preferential-business")
@RequiredArgsConstructor
@Validated
public class PreferentialBusinessController {

    private final PreferentialBusinessService preferentialBusinessService;

    @GetMapping("/query")
    public ResponseEntity<ApiResponse<List<PreferentialBusinessResponse>>> queryPreferentialBusiness(
            @RequestParam String businessNumbers) {

        log.info("Querying preferential business: {}", businessNumbers);

        List<PreferentialBusinessResponse> responses = preferentialBusinessService.queryPreferentialBusiness(businessNumbers);

        return ResponseEntity.ok(ApiResponse.success(responses));
    }
}
