package com.picnee.travel.domain.place.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.picnee.travel.domain.place.dto.res.FindPlaceRes;
import com.picnee.travel.domain.place.dto.res.OpeningHoursRes;
import com.picnee.travel.domain.place.entity.PlaceType;
import com.picnee.travel.domain.place.properties.GooglePlaceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceCacheService {

    private static final String PLACE_CACHE_PREFIX = "place:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final GooglePlaceProperties googlePlaceProperties;

    public Optional<FindPlaceRes> get(String placeId) {
        try {
            String cachedValue = redisTemplate.opsForValue().get(generateKey(placeId));
            if (!StringUtils.hasText(cachedValue)) {
                return Optional.empty();
            }

            CachedPlace cachedPlace = objectMapper.readValue(cachedValue, CachedPlace.class);
            return Optional.of(cachedPlace.toResponse());
        } catch (Exception e) {
            log.warn("Failed to read place cache for placeId={}", placeId, e);
            return Optional.empty();
        }
    }

    public void save(String placeId, FindPlaceRes place) {
        try {
            String payload = objectMapper.writeValueAsString(CachedPlace.from(place));
            redisTemplate.opsForValue().set(
                    generateKey(placeId),
                    payload,
                    Duration.ofDays(googlePlaceProperties.getCacheTtlDays())
            );
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize place cache for placeId={}", placeId, e);
        } catch (Exception e) {
            log.warn("Failed to write place cache for placeId={}", placeId, e);
        }
    }

    private String generateKey(String placeId) {
        return PLACE_CACHE_PREFIX + placeId;
    }

    private record CachedPlace(
            String placeId,
            String placeName,
            String url,
            String formattedAddress,
            String formattedPhoneNumber,
            Double rating,
            String website,
            String lat,
            String lng,
            PlaceType type,
            java.util.List<CachedOpeningHours> openingHoursRes
    ) {
        private static CachedPlace from(FindPlaceRes response) {
            return new CachedPlace(
                    response.getPlaceId(),
                    response.getPlaceName(),
                    response.getUrl(),
                    response.getFormattedAddress(),
                    response.getFormattedPhoneNumber(),
                    response.getRating(),
                    response.getWebsite(),
                    response.getLat(),
                    response.getLng(),
                    response.getType(),
                    response.getOpeningHoursRes() == null
                            ? java.util.List.of()
                            : response.getOpeningHoursRes().stream()
                            .map(CachedOpeningHours::from)
                            .toList()
            );
        }

        private FindPlaceRes toResponse() {
            return FindPlaceRes.builder()
                    .placeId(placeId)
                    .placeName(placeName)
                    .url(url)
                    .formattedAddress(formattedAddress)
                    .formattedPhoneNumber(formattedPhoneNumber)
                    .rating(rating)
                    .website(website)
                    .lat(lat)
                    .lng(lng)
                    .type(type)
                    .openingHoursRes(openingHoursRes == null
                            ? java.util.List.of()
                            : openingHoursRes.stream()
                            .map(CachedOpeningHours::toResponse)
                            .toList())
                    .build();
        }
    }

    private record CachedOpeningHours(
            String day,
            String time
    ) {
        private static CachedOpeningHours from(OpeningHoursRes response) {
            return new CachedOpeningHours(response.getDay(), response.getTime());
        }

        private OpeningHoursRes toResponse() {
            return OpeningHoursRes.builder()
                    .day(day)
                    .time(time)
                    .build();
        }
    }
}
