package com.picnee.travel.domain.place.service;

import com.picnee.travel.domain.place.dto.req.OpeningHoursReq;
import com.picnee.travel.domain.place.entity.OpeningHours;
import com.picnee.travel.domain.place.entity.Place;
import com.picnee.travel.domain.place.repository.OpeningHoursRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Collections;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OpeningHoursService {

    private final OpeningHoursRepository openingHoursRepository;

    @Transactional
    public void create(List<OpeningHoursReq> dto, Place place) {
        List<OpeningHours> openingHoursEntities = toEntities(dto, place);

        place.replaceOpeningHours(openingHoursEntities);

        if (!openingHoursEntities.isEmpty()) {
            openingHoursRepository.saveAll(openingHoursEntities);
        }
    }

    @Transactional
    public void replace(List<OpeningHoursReq> dto, Place place) {
        openingHoursRepository.deleteByPlaceId(place.getId());

        List<OpeningHours> openingHoursEntities = toEntities(dto, place);
        place.replaceOpeningHours(openingHoursEntities);

        if (!openingHoursEntities.isEmpty()) {
            openingHoursRepository.saveAll(openingHoursEntities);
        }
    }

    private List<OpeningHours> toEntities(List<OpeningHoursReq> dto, Place place) {
        if (dto == null || dto.isEmpty()) {
            return Collections.emptyList();
        }

        return dto.stream()
                .map(req -> OpeningHoursReq.toEntity(req, place))
                .toList();
    }
}
