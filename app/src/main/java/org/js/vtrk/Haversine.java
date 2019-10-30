package org.js.vtrk;

import android.location.Location;

public class Haversine {
    private static final double R = 6372.8; // In kilometers

    public double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);
        double a = Math.pow(Math.sin(dLat / 2), 2) +
                    Math.pow(Math.sin(dLon / 2), 2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return R * c;
    }

    public double lHaversine(Location loc1, Location loc2){
        double dLat=Math.toRadians(loc2.getLatitude()-loc1.getLatitude());
        double dLon=Math.toRadians(loc2.getLongitude()-loc1.getLongitude());
        double lat1=Math.toRadians(loc1.getLatitude());
        double lat2=Math.toRadians(loc2.getLatitude());
        double a = Math.pow(Math.sin(dLat / 2), 2) +
                    Math.pow(Math.sin(dLon / 2), 2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return R * c;
    }

    public Location invHaver(Location startLoc, Float dist, Float bearing, Float alt){
        Location dest=new Location("");
        dest.set(startLoc);
        Double bgRad=Math.toRadians(bearing.doubleValue());
        Double latRad=Math.toRadians(startLoc.getLatitude());
        Double lonRad=Math.toRadians(startLoc.getLongitude());
        Double distFrac=dist.doubleValue()/(R*1000.0);
        Double sinLat=Math.sin(latRad);
        Double cosDist=Math.cos(distFrac);
        Double cosLat=Math.cos(latRad);
        Double sinDist=Math.sin(distFrac);
        Double latRes=Math.asin(sinLat*cosDist+cosLat*sinDist*Math.cos(bgRad));
        Double a=Math.atan2(Math.sin(bgRad)*sinDist*cosLat,
                cosDist-sinLat*Math.sin(latRes));
        Double lonRes=(lonRad+a+3.0*Math.PI)%(2.0*Math.PI)-Math.PI;
        dest.setLatitude(Math.toDegrees(latRes));
        dest.setLongitude(Math.toDegrees(lonRes));
        dest.setAltitude(dest.getAltitude()+alt.doubleValue());
        return dest;
    }

}
