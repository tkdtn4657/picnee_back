package com.picnee.travel.domain.place.service;

import com.picnee.travel.domain.place.client.GooglePlaceClient;
import com.picnee.travel.domain.place.client.dto.GooglePlacePayload;
import com.picnee.travel.domain.place.dto.req.OpeningHoursReq;
import com.picnee.travel.domain.place.dto.res.FindPlaceRes;
import com.picnee.travel.domain.place.entity.Place;
import com.picnee.travel.domain.place.entity.PlaceType;
import com.picnee.travel.domain.place.entity.Region;
import com.picnee.travel.domain.place.repository.PlaceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Transactional
@SpringBootTest
@ActiveProfiles("test")
class PlaceServiceTest {

    @Autowired
    private PlaceService placeService;

    @Autowired
    private PlaceRepository placeRepository;

    @MockBean
    private GooglePlaceClient googlePlaceClient;

    @MockBean
    private PlaceCacheService placeCacheService;

    @Test
    @DisplayName("장소 캐시 hit 시 캐시 데이터를 우선 반환한다")
    void getPlace_returnsCachedDataFirst() {
        FindPlaceRes cached = FindPlaceRes.builder()
                .placeId("cached-place")
                .placeName("Cached Place")
                .type(PlaceType.RESTAURANT)
                .openingHoursRes(List.of())
                .build();

        when(placeCacheService.get("cached-place")).thenReturn(Optional.of(cached));

        FindPlaceRes result = placeService.getPlace("cached-place");

        assertThat(result.getPlaceName()).isEqualTo("Cached Place");
        verify(googlePlaceClient, never()).fetchPlace(anyString());
    }

    @Test
    @DisplayName("DB 데이터가 30일 이내면 외부 API를 호출하지 않는다")
    void getPlace_returnsDbDataWhenSyncIsFresh() {
        Place place = placeRepository.save(Place.builder()
                .id("fresh-place")
                .placeName("Fresh Place")
                .url("https://example.com/fresh")
                .formattedAddress("Osaka")
                .formattedPhoneNumber("010-0000-0000")
                .rating(4.3)
                .website("https://example.com")
                .lat("35.1")
                .lng("135.1")
                .types(PlaceType.RESTAURANT)
                .region(Region.OSAKA)
                .googleSyncedAt(LocalDateTime.now().minusDays(10))
                .build());

        when(placeCacheService.get("fresh-place")).thenReturn(Optional.empty());

        FindPlaceRes result = placeService.getPlace(place.getId());

        assertThat(result.getPlaceName()).isEqualTo("Fresh Place");
        verify(googlePlaceClient, never()).fetchPlace(anyString());
        verify(placeCacheService).save("fresh-place", result);
    }

    @Test
    @DisplayName("DB 데이터가 30일을 초과하면 외부 API로 갱신 후 반환한다")
    void getPlace_refreshesStalePlaceFromGoogle() {
        Place place = placeRepository.save(Place.builder()
                .id("stale-place")
                .placeName("Old Place")
                .url("https://example.com/old")
                .formattedAddress("Osaka")
                .formattedPhoneNumber("010-1111-1111")
                .rating(3.1)
                .website("https://example.com/old")
                .lat("35.1")
                .lng("135.1")
                .types(PlaceType.RESTAURANT)
                .region(Region.OSAKA)
                .googleSyncedAt(LocalDateTime.now().minusDays(31))
                .build());

        when(placeCacheService.get("stale-place")).thenReturn(Optional.empty());
        when(googlePlaceClient.fetchPlace("stale-place")).thenReturn(Optional.of(
                GooglePlacePayload.builder()
                        .placeId("stale-place")
                        .placeName("New Place")
                        .url("https://example.com/new")
                        .formattedAddress("Tokyo")
                        .formattedPhoneNumber("010-2222-2222")
                        .rating(4.8)
                        .website("https://example.com/new")
                        .lat("35.6")
                        .lng("139.7")
                        .types(List.of("restaurant"))
                        .openingHoursList(List.of(
                                OpeningHoursReq.builder().day("Monday").time("09:00-18:00").build()
                        ))
                        .build()
        ));

        FindPlaceRes result = placeService.getPlace(place.getId());

        assertThat(result.getPlaceName()).isEqualTo("New Place");
        assertThat(result.getFormattedAddress()).isEqualTo("Tokyo");
        assertThat(result.getOpeningHoursRes()).hasSize(1);

        Place refreshed = placeRepository.findById(place.getId()).orElseThrow();
        assertThat(refreshed.getPlaceName()).isEqualTo("New Place");
        assertThat(refreshed.getGoogleSyncedAt()).isAfter(LocalDateTime.now().minusMinutes(1));
    }

    @Test
    @DisplayName("DB에 없는 장소는 외부 API 조회 후 저장한다")
    void getPlace_createsPlaceFromGoogleWhenNotStored() {
        when(placeCacheService.get("brand-new-place")).thenReturn(Optional.empty());
        when(googlePlaceClient.fetchPlace("brand-new-place")).thenReturn(Optional.of(
                GooglePlacePayload.builder()
                        .placeId("brand-new-place")
                        .placeName("Brand New Place")
                        .url("https://example.com/brand-new")
                        .formattedAddress("Fukuoka")
                        .formattedPhoneNumber("010-3333-3333")
                        .rating(4.5)
                        .website("https://example.com/brand-new")
                        .lat("33.5")
                        .lng("130.4")
                        .types(List.of("lodging"))
                        .openingHoursList(List.of(
                                OpeningHoursReq.builder().day("Tuesday").time("10:00-20:00").build()
                        ))
                        .build()
        ));

        FindPlaceRes result = placeService.getPlace("brand-new-place");

        assertThat(result.getPlaceName()).isEqualTo("Brand New Place");
        assertThat(placeRepository.findById("brand-new-place")).isPresent();
        verify(placeCacheService).save("brand-new-place", result);
    }
}
