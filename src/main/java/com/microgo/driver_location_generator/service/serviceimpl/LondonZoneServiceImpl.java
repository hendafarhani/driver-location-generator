package com.microgo.driver_location_generator.service.serviceimpl;

import com.microgo.driver_location_generator.domain.GeoPoint;
import com.microgo.driver_location_generator.enums.LondonZone;
import com.microgo.driver_location_generator.service.LondonZoneService;
import org.springframework.stereotype.Service;

@Service
public class LondonZoneServiceImpl implements LondonZoneService {

    private static final double WEMBLEY_CENTER_LATITUDE = 51.5560d;
    private static final double WEMBLEY_CENTER_LONGITUDE = -0.2796d;
    private static final double WEMBLEY_LATITUDE_DELTA = 0.0200d;
    private static final double WEMBLEY_LONGITUDE_DELTA = 0.0300d;

    private static final double HEATHROW_CENTER_LATITUDE = 51.4700d;
    private static final double HEATHROW_CENTER_LONGITUDE = -0.4543d;
    private static final double HEATHROW_LATITUDE_DELTA = 0.0350d;
    private static final double HEATHROW_LONGITUDE_DELTA = 0.0500d;

    private static final double CENTRAL_LONDON_CENTER_LATITUDE = 51.5074d;
    private static final double CENTRAL_LONDON_CENTER_LONGITUDE = -0.1278d;
    private static final double CENTRAL_LONDON_LATITUDE_DELTA = 0.0400d;
    private static final double CENTRAL_LONDON_LONGITUDE_DELTA = 0.0700d;

    @Override
    public LondonZone resolveZone(GeoPoint point) {
        if (isWithinWembleyEventZone(point)) {
            return LondonZone.WEMBLEY_EVENT_ZONE;
        }

        if (isWithinHeathrowCorridor(point)) {
            return LondonZone.HEATHROW_CORRIDOR;
        }

        if (isWithinCentralLondon(point)) {
            return LondonZone.CENTRAL_LONDON;
        }

        return LondonZone.GENERAL_LONDON;
    }

    private boolean isWithinWembleyEventZone(GeoPoint point) {
        return isWithinZoneBoundaries(
                point,
                WEMBLEY_CENTER_LATITUDE,
                WEMBLEY_CENTER_LONGITUDE,
                WEMBLEY_LATITUDE_DELTA,
                WEMBLEY_LONGITUDE_DELTA
        );
    }

    private boolean isWithinHeathrowCorridor(GeoPoint point) {
        return isWithinZoneBoundaries(
                point,
                HEATHROW_CENTER_LATITUDE,
                HEATHROW_CENTER_LONGITUDE,
                HEATHROW_LATITUDE_DELTA,
                HEATHROW_LONGITUDE_DELTA
        );
    }

    private boolean isWithinCentralLondon(GeoPoint point) {
        return isWithinZoneBoundaries(
                point,
                CENTRAL_LONDON_CENTER_LATITUDE,
                CENTRAL_LONDON_CENTER_LONGITUDE,
                CENTRAL_LONDON_LATITUDE_DELTA,
                CENTRAL_LONDON_LONGITUDE_DELTA
        );
    }

    private boolean isWithinZoneBoundaries(
            GeoPoint point,
            double centerLatitude,
            double centerLongitude,
            double latitudeDelta,
            double longitudeDelta
    ) {
        return isCoordinateWithinRange(point.getLatitude(), centerLatitude, latitudeDelta)
                && isCoordinateWithinRange(point.getLongitude(), centerLongitude, longitudeDelta);
    }

    private boolean isCoordinateWithinRange(double coordinate, double center, double allowedDelta) {
        return Math.abs(coordinate - center) <= allowedDelta;
    }
}
