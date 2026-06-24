package com.microgo.driver_location_generator.service.serviceimpl;

import com.microgo.driver_location_generator.businessrule.LondonZoneBusinessRules;
import com.microgo.driver_location_generator.domain.GeoPoint;
import com.microgo.driver_location_generator.enums.LondonZone;
import com.microgo.driver_location_generator.service.LondonZoneService;
import org.springframework.stereotype.Service;

@Service
public class LondonZoneServiceImpl implements LondonZoneService {

    @Override
    public LondonZone resolveZone(GeoPoint point) {
        return LondonZoneBusinessRules.resolveZone(point);
    }
}
