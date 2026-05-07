package com.picnee.travel.domain.place.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.picnee.travel.domain.place.client.dto.GooglePlacePayload;
import com.picnee.travel.domain.place.dto.req.OpeningHoursReq;
import com.picnee.travel.domain.place.properties.GooglePlaceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class GooglePlaceHttpClient implements GooglePlaceClient {

    private static final String FIELDS = String.join(",",
            "place_id",
            "name",
            "url",
            "formatted_address",
            "formatted_phone_number",
            "rating",
            "website",
            "geometry/location",
            "types",
            "current_opening_hours/weekday_text"
    );

    private final GooglePlaceProperties googlePlaceProperties;
    private final RestClient restClient = RestClient.create();

    @Override
    public Optional<GooglePlacePayload> fetchPlace(String placeId) {
        if (!StringUtils.hasText(googlePlaceProperties.getApiKey())) {
            log.info("Google Place API key is not configured. Skip place sync for {}", placeId);
            return Optional.empty();
        }

        try {
            String uri = UriComponentsBuilder.fromUriString(googlePlaceProperties.getBaseUrl())
                    .queryParam("place_id", placeId)
                    .queryParam("fields", FIELDS)
                    .queryParam("language", googlePlaceProperties.getLanguage())
                    .queryParam("key", googlePlaceProperties.getApiKey())
                    .build()
                    .toUriString();

            GooglePlaceResponse response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(GooglePlaceResponse.class);

            return mapResponse(response);
        } catch (Exception e) {
            log.warn("Failed to fetch Google place details for placeId={}", placeId, e);
            return Optional.empty();
        }
    }

    private Optional<GooglePlacePayload> mapResponse(GooglePlaceResponse response) {
        if (response == null || response.result() == null || !"OK".equalsIgnoreCase(response.status())) {
            return Optional.empty();
        }

        GooglePlaceResult result = response.result();

        return Optional.of(GooglePlacePayload.builder()
                .placeId(result.placeId())
                .placeName(result.name())
                .url(result.url())
                .formattedAddress(result.formattedAddress())
                .formattedPhoneNumber(result.formattedPhoneNumber())
                .rating(result.rating())
                .website(result.website())
                .lat(result.geometry() == null || result.geometry().location() == null ? null : String.valueOf(result.geometry().location().lat()))
                .lng(result.geometry() == null || result.geometry().location() == null ? null : String.valueOf(result.geometry().location().lng()))
                .types(result.types() == null ? Collections.emptyList() : result.types())
                .openingHoursList(toOpeningHours(result.currentOpeningHours()))
                .build());
    }

    private List<OpeningHoursReq> toOpeningHours(GooglePlaceOpeningHours openingHours) {
        if (openingHours == null || openingHours.weekdayText() == null) {
            return Collections.emptyList();
        }

        return openingHours.weekdayText().stream()
                .map(this::toOpeningHour)
                .toList();
    }

    private OpeningHoursReq toOpeningHour(String weekdayText) {
        String[] split = weekdayText.split(":", 2);
        String day = split[0].trim();
        String time = split.length > 1 ? split[1].trim() : "";

        return OpeningHoursReq.builder()
                .day(day)
                .time(time)
                .build();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GooglePlaceResponse(
            String status,
            GooglePlaceResult result
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GooglePlaceResult(
            @JsonProperty("place_id") String placeId,
            String name,
            String url,
            @JsonProperty("formatted_address") String formattedAddress,
            @JsonProperty("formatted_phone_number") String formattedPhoneNumber,
            Double rating,
            String website,
            GooglePlaceGeometry geometry,
            List<String> types,
            @JsonProperty("current_opening_hours") GooglePlaceOpeningHours currentOpeningHours
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GooglePlaceGeometry(
            GooglePlaceLocation location
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GooglePlaceLocation(
            Double lat,
            Double lng
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GooglePlaceOpeningHours(
            @JsonProperty("weekday_text") List<String> weekdayText
    ) {
    }
}
