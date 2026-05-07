package com.picnee.travel.domain.place.entity;

import com.picnee.travel.domain.base.entity.BaseEntity;
import com.picnee.travel.domain.post.entity.Post;
import com.picnee.travel.domain.postComment.entity.PostComment;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

import static jakarta.persistence.FetchType.LAZY;
import static lombok.AccessLevel.PROTECTED;

@Getter
@Entity
@Table(name = "place")
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class Place extends BaseEntity {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "place_id")
    private String id;
    @Column(name = "place_name")
    private String placeName;
    @Column(name = "url")
    private String url;
    @Column(name = "formatted_address")
    private String formattedAddress;
    @Column(name = "formatted_phone_number")
    private String formattedPhoneNumber;
    @Column(name = "rating")
    private Double rating;
    @Column(name = "website")
    private String website;
    @Column(name = "lat")
    private String lat;
    @Column(name = "lng")
    private String lng;
    @Column(name = "types")
    @Enumerated(EnumType.STRING)
    private PlaceType types;
    @Column(name = "region")
    @Enumerated(EnumType.STRING)
    private Region region;
    @Column(name = "google_synced_at")
    private LocalDateTime googleSyncedAt;
    @Builder.Default
    @OneToMany(mappedBy = "place", cascade = CascadeType.PERSIST, orphanRemoval = true)
    private List<OpeningHours> openingHours = new ArrayList<>();

    public boolean isGoogleSyncExpired(long refreshAfterDays) {
        return googleSyncedAt == null || googleSyncedAt.isBefore(LocalDateTime.now().minusDays(refreshAfterDays));
    }

    public void updatePlaceInfo(
            String placeName,
            String url,
            String formattedAddress,
            String formattedPhoneNumber,
            Double rating,
            String website,
            String lat,
            String lng,
            PlaceType type,
            Region region,
            LocalDateTime googleSyncedAt
    ) {
        this.placeName = placeName;
        this.url = url;
        this.formattedAddress = formattedAddress;
        this.formattedPhoneNumber = formattedPhoneNumber;
        this.rating = rating;
        this.website = website;
        this.lat = lat;
        this.lng = lng;
        this.types = type;
        this.region = region;
        this.googleSyncedAt = googleSyncedAt;
    }

    public void replaceOpeningHours(List<OpeningHours> openingHours) {
        this.openingHours.clear();
        this.openingHours.addAll(openingHours);
    }
}
