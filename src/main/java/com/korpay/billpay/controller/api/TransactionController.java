package com.korpay.billpay.controller.api;

import com.korpay.billpay.domain.entity.Transaction;
import com.korpay.billpay.domain.entity.TransactionEvent;
import com.korpay.billpay.domain.entity.User;
import com.korpay.billpay.domain.enums.TransactionStatus;
import com.korpay.billpay.dto.response.ApiResponse;
import com.korpay.billpay.dto.response.PagedResponse;
import com.korpay.billpay.dto.response.TransactionDto;
import com.korpay.billpay.dto.response.TransactionEventDto;
import com.korpay.billpay.service.auth.UserContextHolder;
import com.korpay.billpay.service.transaction.TransactionQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/v1/transactions")
@RequiredArgsConstructor
@Validated
public class TransactionController {

    private final TransactionQueryService transactionQueryService;
    private final UserContextHolder userContextHolder;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<TransactionDto>>> listTransactions(
            @RequestParam(required = false) UUID merchantId,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime approvedAtStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime approvedAtEnd,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime cancelledAtStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime cancelledAtEnd,
            @RequestParam(required = false) String dateFilterType,
            @RequestParam(required = false) String transactionId,
            @RequestParam(required = false) Long pgConnectionId,
            @RequestParam(required = false) String approvalNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        
        User currentUser = userContextHolder.getCurrentUser();
        
        if (size > 100) {
            size = 100;
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<Transaction> transactionsPage = transactionQueryService.findAccessibleTransactions(
                currentUser, merchantId, status, startDate, endDate,
                approvedAtStart, approvedAtEnd, cancelledAtStart, cancelledAtEnd,
                dateFilterType, transactionId, pgConnectionId, approvalNumber, pageable);
        
        List<TransactionDto> dtos = transactionsPage.getContent().stream()
                .map(TransactionDto::from)
                .collect(Collectors.toList());
        
        PagedResponse<TransactionDto> pagedResponse = PagedResponse.of(transactionsPage, dtos);
        
        return ResponseEntity.ok(ApiResponse.success(pagedResponse));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionDto>> getTransaction(@PathVariable UUID id) {
        User currentUser = userContextHolder.getCurrentUser();
        
        Transaction transaction = transactionQueryService.findById(id, currentUser);
        TransactionDto dto = TransactionDto.from(transaction);
        
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @GetMapping("/{id}/events")
    public ResponseEntity<ApiResponse<List<TransactionEventDto>>> getTransactionEvents(@PathVariable UUID id) {
        User currentUser = userContextHolder.getCurrentUser();
        
        List<TransactionEvent> events = transactionQueryService.findEventsByTransactionId(id, currentUser);
        List<TransactionEventDto> dtos = events.stream()
                .map(TransactionEventDto::from)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }
}
