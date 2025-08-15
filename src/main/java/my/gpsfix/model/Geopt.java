package my.gpsfix.model;

import java.util.Date;

//TODO переименовать в TrackPt
public abstract class Geopt {
    private Double lat = null;
    private Double lon = null;
    private Date time = null;
    private Float totalDist = null;

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public static double wrap360(double deg) {
        if (deg < 0) {
            return 360 + deg;
        } else if (deg > 360) {
            return deg % 360;
        }
        return deg;
    }


}
