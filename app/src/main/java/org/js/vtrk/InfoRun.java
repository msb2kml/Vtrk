package org.js.vtrk;

import android.location.Location;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class InfoRun {

    Location startPos=null;
    Location endPos=null;
    Location prevPos=null;
    Location farPos=null;
    Float distance=0.0f;
    Float farDist=0.0f;
    int nbPt=0;

    String print(){
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if (startPos==null) return "No data";
        Long startTime=startPos.getTime();
        String dateLine="";
        if (startTime>0L) {
            dateLine="Starting "+sdf.format(startTime)+"\n";
        }
        String line=dateLine+locForm("Start",endPos);
        line+=locForm("End",endPos);
        if (distance>0.0f){
            line+=String.format(Locale.ENGLISH,"Length: %.2f km\n",distance);
        }
        if (endPos!=null) {
            Long endTime = endPos.getTime();
            if (startTime > 0L && endTime > 0L) {
                Long dur = (endTime - startTime) / 60000L;
                line += String.format(Locale.ENGLISH, "Duration: %d minutes\n", dur);
            }
            if (nbPt > 0) {
                line += String.format(Locale.ENGLISH, "Nb. of points: %d\n", nbPt);
            }
        }
        if (farPos!=null) line+=locForm("Farthest",farPos);
        if (farDist>0.0f) line+=String.format(Locale.ENGLISH,
                "Max distance: %.2f km\n",farDist);
        return line;
    }

    String locForm(String heading, Location where){
        Double lat=startPos.getLatitude();
        Double lon=startPos.getLongitude();
        Double altitude=null;
        String form=String.format(Locale.ENGLISH,"%s: %.6f, %.6f",heading,lat,lon);
        if (where.hasAltitude()){
            altitude=where.getAltitude();
            form+=String.format(Locale.ENGLISH,", %.2f\n",altitude);
        } else form+="\n";
        return form;
    }
}
