package org.js.vtrk;

import android.location.Location;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.exp;
import static java.lang.Math.round;

public class Track {

    String patrnLat="lat=\"(-?[0-9.-]+)\"";
    String patrnLon="lon=\"(-?[0-9.-]+)\"";
    String patrnNam="<name>(.+)</name>";
    String patrnEle="<ele>(.+)</ele>";
    String patrnTim="<time>(.+)</time>";
    Pattern pLat;
    Pattern pLon;
    Pattern pNam;
    Pattern pEle;
    Pattern pTim;


    String filePath=null;
    BufferedReader f=null;
    boolean lkTpt;
    boolean lkEtpt;
    boolean lkCtpt;
    Integer len;
    Integer curnt;
    String expr;
    Double alt=null;
    Long tim=null;
    String line;
    Long size=0L;
    Long position;

    public Long open(String path){
        pLat=Pattern.compile(patrnLat);
        pLon=Pattern.compile(patrnLon);
        pNam=Pattern.compile(patrnNam);
        pEle=Pattern.compile(patrnEle);
        pTim=Pattern.compile(patrnTim);
        if (f!=null) close();
        filePath=path;
        File fi=new File(filePath);
        if (!fi.exists() || !fi.canRead()) return 0L;
        try {
            FileInputStream input=new FileInputStream(filePath);
            InputStreamReader reader=new InputStreamReader(input);
            f=new BufferedReader(reader);
            FileChannel chan=input.getChannel();
            size=chan.size();
            position=0L;
            line="";
            lkTpt=true;
            lkEtpt=false;
            lkCtpt=false;
            expr="";
            len=0;
            curnt=0;
            while ((line!=null) && (len==0)){
                line=f.readLine();
                if (line!=null){
                    len=line.length();
                } else { position+=1l;}
            }
        } catch (Exception e) { return 0L;}
        return size;
    }

    public void close(){
        if (f!=null){
            try {
                f.close();
                f=null;
            } catch (Exception e) {
                f=null;
            }
        }
        filePath=null;
    }

    public Long getPos(){
        return position;
    }

    public Location nextPt(){
        Location loc=null;
        if (f==null || line==null) return null;
        while (true) {
            while (curnt < len) {
                if (lkTpt) {
                    int i=line.indexOf("<trkpt",curnt);
                    if (i<0){
                        position+=(len-curnt);
                        curnt=len;
                        continue;
                    }
                    position+=6L+i-curnt;
                    expr="";
                    curnt=i+6;
                    lkTpt=false;
                    lkEtpt=true;
                    loc=null;
                } else if (lkEtpt) {
                    Integer j=line.indexOf(">",curnt);
                    if (j<0){
                        position+=(len-curnt);
                        expr=expr+line.substring(curnt);
                        curnt=len;
                    } else {
                        expr=expr+line.substring(curnt,j);
                        position+=(j-curnt);
                        curnt=j+1;
                        loc=LatLon(expr);
                        lkEtpt=false;
                        if (loc!=null){
                            lkCtpt=true;
                            expr="";
                            loc.setAltitude(0d);
                        } else {
                            lkTpt=true;
                        }
                    }
                } else if (lkCtpt) {
                    int i=line.indexOf("</trkpt>",curnt);
                    if (i<0){
                        position+=(len-curnt);
                        alt=rEle(line.substring(curnt));
                        tim=rTime(line.substring(curnt));
                        if (alt!=null) loc.setAltitude(alt);
                        if (tim!=null) loc.setTime(tim);
                        curnt=len;
                        continue;
                    } else {
                        alt=rEle(line.substring(curnt,i));
                        tim=rTime(line.substring(curnt,i));
                        if (alt!=null) loc.setAltitude(alt);
                        if (tim!=null) loc.setTime(tim);
                        position+=i-curnt+8;
                        curnt=i+8;
                        lkCtpt=false;
                        lkTpt=true;
                        return loc;
                    }
                }
            }
            try {
                position+=1l;
                line = f.readLine();
                if (line == null) return null;
                len = line.length();
                curnt=0;
            } catch(Exception e) {
                return null;
            }
        }
    }


    private Location LatLon(String expr){
        Location loc=new Location("");
        Double lat=null;
        Double lon=null;
        Matcher m;
        m=pLat.matcher(expr);
        if (m.find()){
            try {
                lat=Double.parseDouble(m.group(1));
            } catch (NumberFormatException e){
                return null;
            }
        } else return null;
        m=pLon.matcher(expr);
        if (m.find()){
            try {
                lon=Double.parseDouble(m.group(1));
            } catch (NumberFormatException e){
                return null;
            }
        } else return null;
        if (lat>180 || lat<-180 || lon>180 || lon<-180) return null;
        loc.setLongitude(lon);
        loc.setLatitude(lat);
        return loc;
    }

    private Double rEle(String expr){
        Double alt;
        Matcher m;
        m=pEle.matcher(expr);
        if (m.find()){
            try {
                alt=Double.parseDouble(m.group(1));
                return alt;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private Long rTime(String expr){
        Matcher m;
        Long tim;
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        m=pTim.matcher(expr);
        if (m.find()){
            try {
                Date date=sdf.parse(m.group(1));
                tim=date.getTime();
                return tim;
            } catch (Exception e){
                return null;
            }
        } else return null;
    }
}
