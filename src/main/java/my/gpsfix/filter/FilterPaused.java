package my.gpsfix.filter;

import my.gpsfix.model.GPXpt;

public class FilterPaused implements FilterStrategy {
    @Override
    public boolean isFake(GPXpt prevGPXpt, GPXpt GPXpt) {
        //Paused filter
        double dist = prevGPXpt.distanceTo(GPXpt);
        if (dist == 0) {
            return true;
        }
        return false;
    }
}
