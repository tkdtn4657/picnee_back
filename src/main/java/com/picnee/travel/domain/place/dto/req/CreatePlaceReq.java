package com.picnee.travel.domain.place.dto.req;

import com.picnee.travel.domain.place.dto.PlaceTypeWeight;
import com.picnee.travel.domain.place.dto.RegionMapper;
import com.picnee.travel.domain.place.entity.Place;
import com.picnee.travel.domain.place.entity.PlaceType;
import com.picnee.travel.domain.place.entity.Region;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CreatePlaceReq {

    @NotNull(message = "장소는 고유번호는 필수입니다.")
    private String placeId;
    @NotNull(message = "장소 이름은 필수입니다.")
    private String placeName;
    @NotNull(message = "URL은 필수 값입니다.")
    private String url;
    private String formattedAddress;
    private String formattedPhoneNumber;
    private Double rating;
    private String website;
    @NotNull(message = "위도는 필수입니다.")
    private String lat;
    @NotNull(message = "경도는 필수입니다.")
    private String lng;
    @NotNull(message = "카테고리는 필수입니다.")
    private List<String> types;
    private List<OpeningHoursReq> openingHoursList;
    

    public static Place toEntity(CreatePlaceReq dto) {
        PlaceType   type     = PlaceTypeWeight.filterType(dto.getTypes());
        Region      region   = RegionMapper.getRegion(dto.getFormattedAddress());

        return Place.builder()
                .id(dto.getPlaceId())
                .placeName(dto.getPlaceName())
                .url(dto.getUrl())
                .formattedAddress(dto.getFormattedAddress())
                .formattedPhoneNumber(dto.getFormattedPhoneNumber())
                .rating(dto.getRating())
                .website(dto.getWebsite())
                .lat(dto.getLat())
                .lng(dto.getLng())
                .types(type)
                .region(region)
                .googleSyncedAt(LocalDateTime.now())
                .build();
    }
}
