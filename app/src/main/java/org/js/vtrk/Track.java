package org.js.vtrk;

import android.location.Location;
import android.os.Bundle;

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
    String nam=null;
    String line;
    Long size=0L;
    Long position;
    String opening=null;
    String ending=null;

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


    public enum enttGpx {
        ALIEN,
        WPT,
        TRK,
        RTE,
        TRKWPT,
        RTEWPT
    }

    enttGpx curEntity=enttGpx.ALIEN;
    String begin="(<wpt|<trk|<rte)([^>]*)(>)";
    String endwpt="(.*?)(</wpt>)";
    String endtrk="(.*?)(<trkpt|</trk>)";
    String endtrkpt="(.*?)(</trkpt>)";
    String endrte="(.*?)(<rtept|</rte>)";
    String endrtept="(.*?)(</rtept>)";
    String endpt="([^>]*?)(>)";

    public Location nextPt(){
        Location loc=null;
        Location locSup=null;
        Pattern probe=null;
        String entity;
        if (f==null || line==null) return null;
        switch (curEntity){
            case ALIEN:
            case WPT:
                probe=Pattern.compile(begin);
                break;
            case TRK:
                probe=Pattern.compile(endtrk);
                break;
            case RTE:
                probe=Pattern.compile(endrte);
                break;
            case TRKWPT:
                probe=Pattern.compile(endpt);
                break;
            case RTEWPT:
                probe=Pattern.compile(endpt);
                break;
        }
        while (true){
            while (curnt<len){
                Matcher m=probe.matcher(line.substring(curnt,len));
                if (m.find()){
                    switch (curEntity){
                        case ALIEN:                   //"(<wpt|<trk|<rte)([^>]*)(>)"
                            entity=m.group(1);
                            expr=m.group(2);
                            switch (entity){
                                case "<wpt":
                                    loc=LatLon(expr);
                                    curnt+=m.end(3);
                                    position+=m.end(3);
                                    if (loc!=null){
                                        curEntity=enttGpx.WPT;
                                        probe=Pattern.compile(endwpt);
                                    }
                                    break;
                                case "<trk":
                                    if (m.group(2).length()==0) {
                                        locSup = new Location("");
                                        Bundle bundle = new Bundle();
                                        bundle.putSerializable("ENTITY", enttGpx.TRK);
                                        locSup.setExtras(bundle);
                                        curnt += m.end(3);
                                        position += m.end(3);
                                        curEntity = enttGpx.TRK;
                                        probe = Pattern.compile(endtrk);
                                    }
                                    break;
                                case "<rte":
                                    if (m.group(2).length()==0) {
                                        locSup = new Location("");
                                        Bundle bundle = new Bundle();
                                        bundle.putSerializable("ENTITY", enttGpx.RTE);
                                        locSup.setExtras(bundle);
                                        curnt += m.end(3);
                                        position += m.end(3);
                                        curEntity = enttGpx.RTE;
                                        probe = Pattern.compile(endrte);
                                    }
                                    break;
                            }
                            break;
                        case WPT:                //"(.*?)(</wpt>)"
                            expr=m.group(1);
                            curnt+=m.end(2);
                            position+=m.end(2);
                            if (expr.length()>0) {
                                alt = rEle(expr);
                                tim = rTime(expr);
                                nam = rNam(expr);
                                if (alt != null) loc.setAltitude(alt);
                                if (tim != null) loc.setTime(tim);
                                if (nam != null) loc.getExtras().putString("name", nam);
                            }
                            loc.getExtras().putSerializable("ENTITY",curEntity);
                            curEntity=enttGpx.ALIEN;
                            probe=Pattern.compile(begin);
                            return loc;
                        case TRK:              //"(.*?)(<trkpt|</trk>)"
                            expr=m.group(1);
                            entity=m.group(2);
                            curnt+=m.end(2);
                            position+=m.end(2);
                            if (expr.length()>0){
                                nam=rNam(expr);
                                if (nam!=null) locSup.getExtras().putString("name",nam);
                            }
                            switch (entity){
                                case "</trk>":
                                   curEntity=enttGpx.ALIEN;
                                   probe=Pattern.compile(begin);
                                   break;
                                case "<trkpt":
                                    loc=null;
                                    curEntity=enttGpx.TRKWPT;
                                    probe=Pattern.compile(endpt);
                                    if (locSup!=null) return locSup;
                            }
                            break;
                        case TRKWPT:                  //"([^>]*?)(>)"
                                                      //"(.*?)(</trkpt>)"
                            curnt+=m.end(2);
                            position+=m.end(2);
                            expr=m.group(1);
                            if (loc==null){
                                loc=LatLon(expr);
                                if (loc==null) curEntity=enttGpx.TRK;
                                probe=Pattern.compile(endtrkpt);
                            } else {
                                if (expr.length()>0) {
                                    alt = rEle(expr);
                                    tim = rTime(expr);
                                    nam = rNam(expr);
                                    if (alt != null) loc.setAltitude(alt);
                                    if (tim != null) loc.setTime(tim);
                                    if (nam != null) loc.getExtras().putString("name", nam);
                                }
                                loc.getExtras().putSerializable("ENTITY",curEntity);
                                curEntity=enttGpx.TRK;
                                return loc;
                            }
                            break;
                        case RTE:             //"(.*?)(<rtept|</rte>)"
                            expr=m.group(1);
                            entity=m.group(2);
                            curnt+=m.end(2);
                            position+=m.end(2);
                             if (expr.length()>0){
                                nam=rNam(expr);
                                if (nam!=null) locSup.getExtras().putString("name",nam);
                            }
                             switch (entity){
                                 case "</rte>":
                                     curEntity=enttGpx.ALIEN;
                                     probe=Pattern.compile(begin);
                                     break;
                                 case "<rtept":
                                     loc=null;
                                     curEntity=enttGpx.RTEWPT;
                                     probe=Pattern.compile(endpt);
                                     if (locSup!=null) return locSup;
                             }
                            break;
                        case RTEWPT:          //"([^>]*?)(>)"
                                              //"(.*?)(</rtept>)"
                            curnt+=m.end(2);
                            position+=m.end(2);
                            expr=m.group(1);
                            if (loc==null){
                                loc=LatLon(expr);
                                if (loc==null) curEntity=enttGpx.RTE;
                                probe=Pattern.compile(endrtept);
                            } else {
                                if (expr.length()>0) {
                                    alt = rEle(expr);
                                    tim = rTime(expr);
                                    nam = rNam(expr);
                                    if (alt != null) loc.setAltitude(alt);
                                    if (tim != null) loc.setTime(tim);
                                    if (nam != null) loc.getExtras().putString("name", nam);
                                }
                                loc.getExtras().putSerializable("ENTITY",curEntity);
                                curEntity=enttGpx.RTE;
                                return loc;
                            }
                            break;
                    }
                } else {
                    expr=line.substring(curnt,len);
                    position+=len-curnt;
                    curnt=len;
                    switch (curEntity){
                        case ALIEN:              //"(<wpt|<trk|<rte)([^>]*)(>)"
                            break;
                        case WPT:                //"(.*?)(</wpt>)"
                            if (expr.length()>0) {
                                alt = rEle(expr);
                                tim = rTime(expr);
                                nam = rNam(expr);
                                if (alt != null) loc.setAltitude(alt);
                                if (tim != null) loc.setTime(tim);
                                if (nam != null) loc.getExtras().putString("name", nam);
                            }
                            break;
                        case TRK:                //"(.*?)(<trkpt|</trk>)"
                            if (expr.length()>0){
                                nam=rNam(expr);
                                if (nam!=null && locSup!=null)
                                    locSup.getExtras().putString("name",nam);
                            }
                            break;
                        case TRKWPT:          //"([^>]*?)(>)"
                                              //"(.*?)(</trkpt>)"
                            if (loc==null){
                                curEntity=enttGpx.TRK;
                                probe=Pattern.compile(endtrk);
                            } else {
                                if (expr.length()>0) {
                                    alt = rEle(expr);
                                    tim = rTime(expr);
                                    nam = rNam(expr);
                                    if (alt != null) loc.setAltitude(alt);
                                    if (tim != null) loc.setTime(tim);
                                    if (nam != null) loc.getExtras().putString("name", nam);
                                }
                            }
                            break;
                        case RTE:          //"(.*?)(<rtept|</rte>)"
                            if (expr.length()>0){
                                nam=rNam(expr);
                                if (nam!=null && locSup!=null)
                                    locSup.getExtras().putString("name",nam);
                            }
                            break;
                        case RTEWPT:             //"([^>]*)(>)"
                                                 //"(.*?)(</rtept>)"
                            if (loc==null){
                                curEntity=enttGpx.RTE;
                                probe=Pattern.compile(endrte);
                            } else {
                                if (expr.length()>0) {
                                    alt = rEle(expr);
                                    tim = rTime(expr);
                                    nam = rNam(expr);
                                    if (alt != null) loc.setAltitude(alt);
                                    if (tim != null) loc.setTime(tim);
                                    if (nam != null) loc.getExtras().putString("name", nam);
                                }
                            }
                            break;

                    }
                }
            }
            try {
                position+=1l;
                line=f.readLine();
                if (line==null) return null;
                len=line.length();
                curnt=0;
            } catch (Exception e){
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
        Bundle bundle=new Bundle();
        loc.setExtras(bundle);
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

    private String rNam(String expr){
        Matcher m;
        m=pNam.matcher(expr);
        if (m.find()){
            return m.group(1);
        } else return null;
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
