package com.korpay.billpay.controller.api;

import com.korpay.billpay.domain.entity.User;
import com.korpay.billpay.dto.request.ChangePasswordRequest;
import com.korpay.billpay.dto.request.UserCreateRequest;
import com.korpay.billpay.dto.request.UserUpdateRequest;
import com.korpay.billpay.dto.response.ApiResponse;
import com.korpay.billpay.dto.response.PagedResponse;
import com.korpay.billpay.dto.response.UserResponse;
import com.korpay.billpay.service.auth.UserContextHolder;
import com.korpay.billpay.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;
    private final UserContextHolder userContextHolder;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        
        User currentUser = userContextHolder.getCurrentUser();
        
        if (size > 100) {
            size = 100;
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<User> usersPage = userService.findAccessibleUsers(currentUser, pageable);
        
        Page<UserResponse> responsePage = usersPage.map(UserResponse::from);
        PagedResponse<UserResponse> pagedResponse = PagedResponse.of(responsePage, responsePage.getContent());
        
        return ResponseEntity.ok(ApiResponse.success(pagedResponse));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable UUID id) {
        User currentUser = userContextHolder.getCurrentUser();
        
        User user = userService.findById(id, currentUser);
        UserResponse response = UserResponse.from(user);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody UserCreateRequest request) {
        
        User currentUser = userContextHolder.getCurrentUser();
        
        User user = userService.create(request, currentUser);
        UserResponse response = UserResponse.from(user);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UserUpdateRequest request) {
        
        User currentUser = userContextHolder.getCurrentUser();
        
        User user = userService.update(id, request, currentUser);
        UserResponse response = UserResponse.from(user);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID id) {
        User currentUser = userContextHolder.getCurrentUser();
        
        userService.delete(id, currentUser);
        
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @PathVariable UUID id,
            @Valid @RequestBody ChangePasswordRequest request) {
        
        User currentUser = userContextHolder.getCurrentUser();
        
        userService.changePassword(id, request.getOldPassword(), request.getNewPassword(), currentUser);
        
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
