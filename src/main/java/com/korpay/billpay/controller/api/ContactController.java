package com.korpay.billpay.controller.api;

import com.korpay.billpay.domain.entity.Contact;
import com.korpay.billpay.domain.enums.ContactEntityType;
import com.korpay.billpay.dto.request.ContactCreateRequest;
import com.korpay.billpay.dto.response.ApiResponse;
import com.korpay.billpay.dto.response.ContactResponse;
import com.korpay.billpay.service.contact.ContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/v1/contacts")
@RequiredArgsConstructor
@Validated
public class ContactController {

    private final ContactService contactService;

    @PostMapping
    public ResponseEntity<ApiResponse<ContactResponse>> createContact(
            @Valid @RequestBody ContactCreateRequest request) {
        Contact contact = contactService.create(request);
        ContactResponse response = ContactResponse.from(contact);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ContactResponse>> getContact(@PathVariable UUID id) {
        Contact contact = contactService.findById(id);
        ContactResponse response = ContactResponse.from(contact);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ContactResponse>> updateContact(
            @PathVariable UUID id,
            @Valid @RequestBody ContactCreateRequest request) {
        Contact contact = contactService.update(id, request);
        ContactResponse response = ContactResponse.from(contact);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteContact(@PathVariable UUID id) {
        contactService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<ApiResponse<List<ContactResponse>>> getContactsByEntity(
            @PathVariable ContactEntityType entityType,
            @PathVariable UUID entityId) {
        List<Contact> contacts = contactService.findByEntity(entityType, entityId);
        List<ContactResponse> responses = contacts.stream()
                .map(ContactResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/entity/{entityType}/{entityId}/primary")
    public ResponseEntity<ApiResponse<ContactResponse>> getPrimaryContactByEntity(
            @PathVariable ContactEntityType entityType,
            @PathVariable UUID entityId) {
        Contact contact = contactService.findPrimaryByEntity(entityType, entityId)
                .orElse(null);
        ContactResponse response = contact != null ? ContactResponse.from(contact) : null;
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}/set-primary")
    public ResponseEntity<ApiResponse<ContactResponse>> setPrimaryContact(@PathVariable UUID id) {
        Contact contact = contactService.setPrimary(id);
        ContactResponse response = ContactResponse.from(contact);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
