package my.gpsfix.filter;

import my.gpsfix.model.GPXpt;

public class FilterJump implements FilterStrategy {
    @Override
    public boolean isFake(GPXpt prevGPXpt, GPXpt GPXpt) {
        double threshold_dist = 20000;// meters
        double threshold_speed_KmHour = 90;// максимально возможная скорость велосипедиста Km/hour

        double dist = prevGPXpt.distanceTo(GPXpt);
        long deltaT = prevGPXpt.diffTimeTo(GPXpt); // может быть меньше секунды - тогда ОШИБКА будет
        double speed = dist / deltaT;
        double speedKmHour = speed * 3.6;

        if (dist > threshold_dist) {
            return true;
        }
        // магические константы - это конечно плохо, но мы назовем это эвристикой
        if (deltaT > 0 && (speedKmHour > threshold_speed_KmHour
                || (dist > 4000 && speedKmHour > 50))) {
            return true;
        }
        return false;
    }
}