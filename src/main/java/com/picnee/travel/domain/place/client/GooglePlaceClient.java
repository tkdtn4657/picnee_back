package com.picnee.travel.domain.place.client;

import com.picnee.travel.domain.place.client.dto.GooglePlacePayload;

import java.util.Optional;

public interface GooglePlaceClient {

    Optional<GooglePlacePayload> fetchPlace(String placeId);
}
