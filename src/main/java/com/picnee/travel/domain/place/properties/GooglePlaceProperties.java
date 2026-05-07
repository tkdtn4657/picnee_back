package com.picnee.travel.domain.place.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "google.place")
public class GooglePlaceProperties {

    private String baseUrl = "https://maps.googleapis.com/maps/api/place/details/json";
    private String apiKey = "";
    private String language = "ko";
    private long refreshAfterDays = 30;
    private long cacheTtlDays = 5;
}
