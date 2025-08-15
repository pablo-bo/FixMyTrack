package my.gpsfix.filter;

import my.gpsfix.model.GPXpt;

public interface FilterStrategy {
    public boolean isFake(GPXpt prevGPXpt, GPXpt GPXpt);

}
