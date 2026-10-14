package com.lipari.bank.shared.exception;

import org.springframework.http.HttpStatus;

public final class ResourceNotFoundException extends LipariBankException {

    private final String resourceType;
    private final Object resourceId;

    public ResourceNotFoundException(String resourceType, Object resourceId) {
        super(
                String.format("%s con ID '%s' non trovato", resourceType, resourceId),
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND"
        );
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public Object getResourceId() {
        return resourceId;
    }
}
