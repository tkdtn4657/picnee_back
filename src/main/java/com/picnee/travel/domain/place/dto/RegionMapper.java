package com.picnee.travel.domain.place.dto;

import com.picnee.travel.domain.place.entity.Region;
import java.util.Arrays;

public enum RegionMapper {
    OSAKA("Osaka", "오사카"),          // 오사카
    KYOTO("Kyoto", "교토"),           // 교토
    KOBE("Kobe", "고베"),             // 고베
    FUKUOKA("Fukuoka", "후쿠오카"),    // 후쿠오카
    TOKYO("Tokyo", "도쿄"),           // 도쿄
    SAPPORO("Sapporo", "삿포로");      // 삿포로

    private final String regionEngName;
    private final String regionKorName;

    RegionMapper(String regionEngName, String regionKorName) {
        this.regionEngName = regionEngName;
        this.regionKorName = regionKorName;
    }

    public boolean matches(String address) {
        return address != null && (address.contains(regionEngName) || address.contains(regionKorName));
    }

    public static Region getRegion(String address) {
        return Arrays.stream(RegionMapper.values())
                .filter(region -> region.matches(address))
                .findFirst()
                .map(region -> Region.valueOf(region.name()))
                .orElse(Region.UNKNOWN);
    }
}
