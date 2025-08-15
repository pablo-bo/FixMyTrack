package my.gpsfix.filter;

import my.gpsfix.model.GPXpt;

public class FilterMaxAllowed implements FilterStrategy {
    @Override
    public boolean isFake(GPXpt prevGPXpt, GPXpt GPXpt) {
        double dist = prevGPXpt.distanceTo(GPXpt);
        long   deltaT = prevGPXpt.diffTimeTo(GPXpt);
        double vMax = 25; // максимально возможная скорость 25м/с = 90 км/ч
        double maxAllowed = vMax * deltaT; // соответственно максимально возможная дистанция между двумя точками с учетом времени между ними
        if (dist > maxAllowed) {
            return true;
        }
        return false;
    }
}
