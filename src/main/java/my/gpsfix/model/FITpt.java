package my.gpsfix.model;


import com.garmin.fit.DateTime;
import com.garmin.fit.Mesg;
import com.garmin.fit.RecordMesg;

import java.text.SimpleDateFormat;
import java.util.Date;

import static java.lang.Math.*;
import static java.lang.Math.toDegrees;

public class FITpt {
    private Double lat = null;
    private Double lon = null;
    private Date time = null;

    private Float totalDist = null;
    private static final double SEMICIRCLES_TO_DEGREES = 180.0 / (1L << 31);
    private static final double EARTH_RADIUS = 6371000; // meters

    public FITpt(double lat, double lon, Date time) {
        this.lat = lat;
        this.lon = lon;
        this.time = time;
    }

    public FITpt(Mesg mesg) {
        Integer latRaw = mesg.getFieldIntegerValue(RecordMesg.PositionLatFieldNum);
        Integer lonRaw = mesg.getFieldIntegerValue(RecordMesg.PositionLongFieldNum);
        // Преобразование координат
        if (latRaw != null && lonRaw != null) {
            this.lat = latRaw * SEMICIRCLES_TO_DEGREES;
            this.lon = lonRaw * SEMICIRCLES_TO_DEGREES;
        }

        this.totalDist = mesg.getFieldFloatValue(RecordMesg.DistanceFieldNum);
        // DateTime - Garmin тип
        DateTime fitDateTime = mesg.timestampToDateTime(mesg.getFieldLongValue(RecordMesg.TimestampFieldNum));
        long ts = fitDateTime.getTimestamp();
        this.time = fitDateTime.getDate(); // А Это тип Date из java.util
    }

    public Float getTotalDist() {
        return totalDist;
    }

    public boolean isGeoCoded() {
        return (lon != null && lat != null);
    }

    public double distanceTo(FITpt p2) {
        return p2.totalDist - this.totalDist;
    }

    public Double geoDistanceTo_less_accuracy(FITpt p2) {

        Double lon1 = this.lon;
        Double lat1 = this.lat;
        Double lon2 = p2.lon;
        Double lat2 = p2.lat;

        if (lon1 != null && lat1 != null && lon2 != null && lat2 != null) {
            double result = 111.2 * Math.sqrt((lon1 - lon2) * (lon1 - lon2) + (lat1 - lat2) * cos(Math.PI * lon1 / 180) * (lat1 - lat2) * cos(Math.PI * lon1 / 180));
            return result * 1000;//in meters
        }
        return null;
    }

    public Double geoDistanceTo(FITpt p2) {
        Double lon1 = this.lon;
        Double lat1 = this.lat;
        Double lon2 = p2.lon;
        Double lat2 = p2.lat;
        if (lon != null && lat != null && lon2 != null && lat2 != null) {
            double dLat = Math.toRadians(lat2 - lat1);
            double dLon = Math.toRadians(lon2 - lon1);

            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                            Math.sin(dLon / 2) * Math.sin(dLon / 2);

            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            return EARTH_RADIUS * c;
        }
        return null;
    }

    public static double wrap360(double deg) {
        if (deg < 0) {
            return 360 + deg;
        } else if (deg > 360) {
            return deg % 360;
        }
        return deg;
    }

    public Double bearingTo(FITpt p2) {
        Double lon1 = this.lon;
        Double lat1 = this.lat;
        Double lon2 = p2.lon;
        Double lat2 = p2.lat;

        if (lon1 != null && lat1 != null && lon2 != null && lat2 != null) {
            if (this == p2) return 0.0; // same points

            double deltaLon = toRadians(p2.lon) - toRadians(this.lon);
            double y = sin(deltaLon) * cos(toRadians(p2.lat));
            double x = (cos(toRadians(this.lat)) * sin(toRadians(p2.lat))) -
                    (sin(toRadians(this.lat)) * cos(toRadians(p2.lat)) * cos(deltaLon));
            double phi = atan2(y, x);
            return wrap360(toDegrees(phi));
        } else {
            return null;
        }
    }

    public long diffTimeTo(FITpt p2) {
        Date date1 = this.time;
        Date date2 = p2.time;
        if (date2 == null) {
            return 0;
        }
        long differenceInMilliseconds = date2.getTime() - date1.getTime();
        long differenceInSeconds = differenceInMilliseconds / 1000;
        return differenceInSeconds;
    }

    @Override
    public String toString() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedDate = formatter.format(time);
        return "FITpt{%.6f, %.6f, at=%s, total dist=%s}".formatted(lat, lon, formattedDate, totalDist);
    }
}
