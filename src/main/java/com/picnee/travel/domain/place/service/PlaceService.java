package com.picnee.travel.domain.place.service;

import com.picnee.travel.domain.place.client.GooglePlaceClient;
import com.picnee.travel.domain.place.client.dto.GooglePlacePayload;
import com.picnee.travel.domain.place.dto.req.CreatePlaceReq;
import com.picnee.travel.domain.place.dto.res.FilterPlaceRes;
import com.picnee.travel.domain.place.dto.res.FindPlaceRes;
import com.picnee.travel.domain.place.entity.Place;
import com.picnee.travel.domain.place.entity.PlaceType;
import com.picnee.travel.domain.place.entity.Region;
import com.picnee.travel.domain.place.exception.NotFoundPlaceException;
import com.picnee.travel.domain.place.properties.GooglePlaceProperties;
import com.picnee.travel.domain.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.picnee.travel.global.exception.ErrorCode.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final OpeningHoursService openingHoursService;
    private final PlaceCacheService placeCacheService;
    private final GooglePlaceClient googlePlaceClient;
    private final GooglePlaceProperties googlePlaceProperties;

    @Transactional
    public String create(CreatePlaceReq dto) {
        Place place = placeRepository.save(CreatePlaceReq.toEntity(dto));
        openingHoursService.create(dto.getOpeningHoursList(), place);
        placeCacheService.save(place.getId(), FindPlaceRes.from(place));

        return place.getId();
    }

    @Transactional
    public FindPlaceRes getPlace(String placeId){
        return placeCacheService.get(placeId)
                .orElseGet(() -> loadPlaceAndCache(placeId));
    }

    public Place findById(String placeId){
        return placeRepository.findById(placeId)
                .orElseThrow(() -> new NotFoundPlaceException(NOT_FOUND_PLACE_EXCEPTION));
    }

    public List<FilterPlaceRes> getPlaces(String region, String type, String sort, Map<String, Boolean> filters) {
        return placeRepository.filterPlaces(region, type, sort, filters);
    }

    private FindPlaceRes loadPlaceAndCache(String placeId) {
        Place place = placeRepository.findById(placeId)
                .map(this::refreshIfExpired)
                .orElseGet(() -> syncFromGoogleOrThrow(placeId));

        FindPlaceRes res = FindPlaceRes.from(place);
        placeCacheService.save(placeId, res);
        return res;
    }

    private Place refreshIfExpired(Place place) {
        if (!place.isGoogleSyncExpired(googlePlaceProperties.getRefreshAfterDays())) {
            return place;
        }

        return syncFromGoogle(place.getId())
                .orElse(place);
    }

    private Place syncFromGoogleOrThrow(String placeId) {
        return syncFromGoogle(placeId)
                .orElseThrow(() -> new NotFoundPlaceException(NOT_FOUND_PLACE_EXCEPTION));
    }

    private Optional<Place> syncFromGoogle(String placeId) {
        return googlePlaceClient.fetchPlace(placeId)
                .map(this::upsertPlaceFromGoogle);
    }

    private Place upsertPlaceFromGoogle(GooglePlacePayload payload) {
        return placeRepository.findById(payload.getPlaceId())
                .map(place -> updatePlaceFromGoogle(place, payload))
                .orElseGet(() -> createPlaceFromGoogle(payload));
    }

    private Place createPlaceFromGoogle(GooglePlacePayload payload) {
        Place place = placeRepository.save(payload.toEntity());
        openingHoursService.create(payload.getOpeningHoursList(), place);
        return place;
    }

    private Place updatePlaceFromGoogle(Place place, GooglePlacePayload payload) {
        PlaceType placeType = payload.resolvePlaceType();
        Region region = payload.resolveRegion();

        place.updatePlaceInfo(
                payload.getPlaceName(),
                payload.getUrl(),
                payload.getFormattedAddress(),
                payload.getFormattedPhoneNumber(),
                payload.getRating(),
                payload.getWebsite(),
                payload.getLat(),
                payload.getLng(),
                placeType,
                region,
                LocalDateTime.now()
        );

        openingHoursService.replace(payload.getOpeningHoursList(), place);
        return place;
    }
}
