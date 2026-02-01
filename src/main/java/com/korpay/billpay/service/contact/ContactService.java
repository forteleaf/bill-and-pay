package com.korpay.billpay.service.contact;

import com.korpay.billpay.domain.entity.Contact;
import com.korpay.billpay.domain.enums.ContactEntityType;
import com.korpay.billpay.dto.request.ContactCreateRequest;
import com.korpay.billpay.exception.EntityNotFoundException;
import com.korpay.billpay.exception.ValidationException;
import com.korpay.billpay.repository.ContactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContactService {

    private final ContactRepository contactRepository;

    /**
     * Create a new contact
     */
    @Transactional
    public Contact create(ContactCreateRequest request) {
        // If this contact is marked as primary, unset any existing primary for the same entity
        if (Boolean.TRUE.equals(request.getIsPrimary())) {
            unsetPrimaryForEntity(request.getEntityType(), request.getEntityId());
        }

        Contact contact = Contact.builder()
                .name(request.getName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .role(request.getRole())
                .entityType(request.getEntityType())
                .entityId(request.getEntityId())
                .isPrimary(request.getIsPrimary() != null ? request.getIsPrimary() : false)
                .build();

        return contactRepository.save(contact);
    }

    /**
     * Find contact by ID (non-deleted only)
     */
    public Contact findById(UUID id) {
        return contactRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new EntityNotFoundException("Contact not found: " + id));
    }

    /**
     * Update contact by ID
     */
    @Transactional
    public Contact update(UUID id, ContactCreateRequest request) {
        Contact contact = findById(id);

        // If changing primary status to true, unset other primary contacts
        if (Boolean.TRUE.equals(request.getIsPrimary()) && !contact.getIsPrimary()) {
            unsetPrimaryForEntity(request.getEntityType(), request.getEntityId());
        }

        contact.setName(request.getName());
        contact.setPhone(request.getPhone());
        contact.setEmail(request.getEmail());
        contact.setRole(request.getRole());
        contact.setEntityType(request.getEntityType());
        contact.setEntityId(request.getEntityId());
        contact.setIsPrimary(request.getIsPrimary() != null ? request.getIsPrimary() : false);

        return contactRepository.save(contact);
    }

    /**
     * Soft delete contact by ID
     */
    @Transactional
    public void delete(UUID id) {
        Contact contact = findById(id);
        contact.softDelete();
        contactRepository.save(contact);
    }

    /**
     * Find all contacts for an entity (non-deleted only)
     */
    public List<Contact> findByEntity(ContactEntityType entityType, UUID entityId) {
        return contactRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    /**
     * Find primary contact for an entity
     */
    public Optional<Contact> findPrimaryByEntity(ContactEntityType entityType, UUID entityId) {
        return contactRepository.findPrimaryByEntityTypeAndEntityId(entityType, entityId);
    }

    /**
     * Set a contact as primary (unsets other primary contacts for the same entity)
     */
    @Transactional
    public Contact setPrimary(UUID id) {
        Contact contact = findById(id);

        // Unset primary for all other contacts of the same entity
        unsetPrimaryForEntity(contact.getEntityType(), contact.getEntityId());

        // Set this contact as primary
        contact.setIsPrimary(true);
        return contactRepository.save(contact);
    }

    /**
     * Helper method: unset primary for all contacts of an entity
     */
    @Transactional
    private void unsetPrimaryForEntity(ContactEntityType entityType, UUID entityId) {
        List<Contact> contacts = contactRepository.findByEntityTypeAndEntityId(entityType, entityId);
        for (Contact c : contacts) {
            if (Boolean.TRUE.equals(c.getIsPrimary())) {
                c.setIsPrimary(false);
                contactRepository.save(c);
            }
        }
    }
}
