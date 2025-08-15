package my.gpsfix.model;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Date;
import java.text.SimpleDateFormat;

import static java.lang.Math.*;

public class GPXpt {
    private double lat;
    private double lon;
    private Date time;

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss'Z'");
    private static final SimpleDateFormat msdateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS'Z'"); // allow millis too

    public GPXpt(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public GPXpt(NamedNodeMap latlon) {
        this.lat = Double.parseDouble(latlon.getNamedItem("lat").getNodeValue());
        this.lon = Double.parseDouble(latlon.getNamedItem("lon").getNodeValue());
    }

    public GPXpt(Node node) {
        NamedNodeMap attrs = node.getAttributes();
        this.lat = Double.parseDouble(attrs.getNamedItem("lat").getNodeValue());
        this.lon = Double.parseDouble(attrs.getNamedItem("lon").getNodeValue());
        this.time = null;// по умолчанию. А то бывает отметки времени нет в файле
        NodeList childNodes = node.getChildNodes();
        for (int idx = 0; idx < childNodes.getLength(); idx++) {
            Node currentNode = childNodes.item(idx);
            if (currentNode.getNodeName().equals("time")) {
                String t = currentNode.getTextContent();
                try {
                    if (t.length() == 20) {
                        this.time = dateFormat.parse(t);
                    } else if (t.length() == 24) {
                        this.time = msdateFormat.parse(t);
                    } else {
                        this.time = new Date(Long.parseLong(t)); // try for unix time
                    }
                } catch (Exception e) {
                    this.time = new Date();
                }
            }
        }
    }

    @Override
    public String toString() {
        return "Trkpt{%.6f, %.6f at: %s}".formatted(this.lat, this.lon, this.time);
    }

    public static double wrap360(double deg) {
        if (deg < 0) {
            return 360 + deg;
        } else if (deg > 360) {
            return deg % 360;
        }
        return deg;
    }

    public double distanceTo(GPXpt p2) {
        //умножать arccos этой формулы нужно на 6371(радиус земли в км) только в случае, если расчёты были в радианах.
        //Если же расчёты были произведены в градусах, то следует умножать полученное значение на длину дуги 1° меридиана — это примерно 111,1 км.
        double lon = this.lon;
        double lat = this.lat;
        double lon2 = p2.lon;
        double lat2 = p2.lat;

        double result = 111.2 * Math.sqrt((lon - lon2) * (lon - lon2) + (lat - lat2) * cos(Math.PI * lon / 180) * (lat - lat2) * cos(Math.PI * lon / 180));
        return result * 1000;//in meters
    }

    public double bearingTo(GPXpt p2) {
        //bearing - пеленг heading - направление
        //Formula: 	θ = atan2( sin Δλ ⋅ cos φ2 , cos φ1 ⋅ sin φ2 − sin φ1 ⋅ cos φ2 ⋅ cos Δλ )
        //where 	φ1,λ1 is the start point, φ2,λ2 the end point (Δλ is the difference in longitude)
        if (this == p2) return 0.0; // same points
        double deltaLon = toRadians(p2.lon) - toRadians(this.lon);
        double y = sin(deltaLon) * cos(toRadians(p2.lat));
        double x = (cos(toRadians(this.lat)) * sin(toRadians(p2.lat))) -
                (sin(toRadians(this.lat)) * cos(toRadians(p2.lat)) * cos(deltaLon));
        double phi = atan2(y, x);
        return wrap360(toDegrees(phi));
    }

    public long diffTimeTo(GPXpt p2) {
        Date date1 = this.time;
        Date date2 = p2.time;
        if (date2 == null) {
            return 0;
        }
        long differenceInMilliseconds = date2.getTime() - date1.getTime();
        long differenceInSeconds = differenceInMilliseconds / 1000;
        return differenceInSeconds;
    }
}
