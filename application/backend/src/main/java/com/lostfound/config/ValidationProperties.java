package com.lostfound.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** All validation limits live here so they can be changed in application.yml and tested at their boundaries. */
@Data
@Component
@ConfigurationProperties(prefix = "app.validation")
public class ValidationProperties {
    private int nameMinLength = 2;
    private int nameMaxLength = 100;
    private int itemNameMinLength = 2;
    private int itemNameMaxLength = 100;
    private int passwordMinLength = 8;
    private int passwordMaxLength = 64;
    private int descriptionMinLength = 10;
    private int descriptionMaxLength = 1000;
    private int locationMaxLength = 150;
    private int claimExplanationMinLength = 20;
    private int claimExplanationMaxLength = 1000;
    private long imageMaxSizeBytes = 2L * 1024 * 1024;
    private int maxImagesPerItem = 3;
    private int dateMaxAgeDays = 730;
    private List<String> allowedImageTypes = new ArrayList<>(List.of("image/jpeg", "image/png", "image/webp"));
    private List<String> allowedImageExtensions = new ArrayList<>(List.of("jpg", "jpeg", "png", "webp"));
    private String phonePattern = "^\\+?[0-9]{10,15}$";
    private String studentIdPattern = "^[A-Za-z0-9]{5,20}$";
}
