package com.picnee.travel.domain.place.client.dto;

import com.picnee.travel.domain.place.dto.PlaceTypeWeight;
import com.picnee.travel.domain.place.dto.RegionMapper;
import com.picnee.travel.domain.place.dto.req.OpeningHoursReq;
import com.picnee.travel.domain.place.entity.Place;
import com.picnee.travel.domain.place.entity.PlaceType;
import com.picnee.travel.domain.place.entity.Region;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class GooglePlacePayload {

    private String placeId;
    private String placeName;
    private String url;
    private String formattedAddress;
    private String formattedPhoneNumber;
    private Double rating;
    private String website;
    private String lat;
    private String lng;
    private List<String> types;
    private List<OpeningHoursReq> openingHoursList;

    public Place toEntity() {
        return Place.builder()
                .id(placeId)
                .placeName(placeName)
                .url(url)
                .formattedAddress(formattedAddress)
                .formattedPhoneNumber(formattedPhoneNumber)
                .rating(rating)
                .website(website)
                .lat(lat)
                .lng(lng)
                .types(resolvePlaceType())
                .region(resolveRegion())
                .googleSyncedAt(LocalDateTime.now())
                .build();
    }

    public PlaceType resolvePlaceType() {
        return PlaceTypeWeight.filterType(getSafeTypes());
    }

    public Region resolveRegion() {
        return RegionMapper.getRegion(formattedAddress);
    }

    public List<OpeningHoursReq> getOpeningHoursList() {
        return openingHoursList == null ? Collections.emptyList() : openingHoursList;
    }

    private List<String> getSafeTypes() {
        return types == null ? Collections.emptyList() : types;
    }
}
