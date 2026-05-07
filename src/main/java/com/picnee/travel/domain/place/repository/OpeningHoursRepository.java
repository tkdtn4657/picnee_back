package com.picnee.travel.domain.place.repository;

import com.picnee.travel.domain.place.entity.OpeningHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OpeningHoursRepository extends JpaRepository<OpeningHours, UUID> {
    void deleteByPlaceId(String placeId);
}
