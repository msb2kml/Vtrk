package org.js.vtrk;

import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import static java.lang.Integer.valueOf;

public class fileProcess {

    Message msg;
    Handler hand;

    public enum msgTyp {
        SZ,
        LOC,
        END,
        PROG,
        ENDS,
        MISTRK,
        ENDLOC,
        ENDLOCR,
        ENDREN,
        ERROR
    }

    public enum Request {
        ENUM,
        SAVE,
        READ,
        RENAME
    }

    Long size;
    Long lastPos=0l;
    Integer nbWpt=0;
    Integer nbTrk=0;
    Integer nbLoc=0;
    Integer nbRte=0;
    Location prevRtePt=null;
    Integer rtePtNb=0;
    String filePath=null;
    String trackName=null;
    Boolean route=false;
    Boolean reName=false;
    String n2find=null;
    String subst=null;
    Track.enttGpx substEntity= Track.enttGpx.ALIEN;

    public void process(Handler handler, Request request, String path, String name,
                        FileWriter outGpx,
                        boolean[] setOptions, boolean mkRte,String orgName){
        hand=handler;
        if (request==Request.ENUM){
            enumerator(path);
            msg=hand.obtainMessage(msgTyp.END.ordinal());
            hand.sendMessage(msg);
        } else if (request==Request.SAVE){
            nbRte=0;
            nbTrk=0;
            nbWpt=0;
            saver(path,outGpx,setOptions,mkRte,null);
            try {
                outGpx.flush();
            } catch (IOException e) {
                msg = hand.obtainMessage(msgTyp.ERROR.ordinal(), e.getMessage());
                hand.sendMessage(msg);
            }
            Integer NbWpt=valueOf(nbWpt);
            msg=hand.obtainMessage(msgTyp.ENDS.ordinal(),nbRte,nbTrk,NbWpt);
            hand.sendMessage(msg);
        } else if (request==Request.READ) {
            filePath=path;
            trackName=name;
            route=mkRte;
            rdTrk();
        } else if (request==Request.RENAME){
            nbRte=0;
            nbTrk=0;
            nbWpt=0;
            reName=true;
            subst=name;
            saver(path,outGpx,setOptions,mkRte,orgName);
             try {
                outGpx.flush();
            } catch (IOException e) {
                msg = hand.obtainMessage(msgTyp.ERROR.ordinal(), e.getMessage());
                hand.sendMessage(msg);
            }
            msg=hand.obtainMessage(msgTyp.ENDREN.ordinal(),nbRte,nbTrk,nbWpt);
            hand.sendMessage(msg);
        }

    }

    void enumerator(String path){
        Location loc=null;
        Track.enttGpx entity= Track.enttGpx.ALIEN;
        Track.enttGpx curEnt=null;
        Track track=new Track();
        size=track.open(path);
        lastPos=0L;
        msg=hand.obtainMessage(msgTyp.SZ.ordinal(),size);
        hand.sendMessage(msg);
        if (size==null || size<1) return;
        loc=meterNext(track);
        while (loc!=null){
            entity=(Track.enttGpx)loc.getExtras().getSerializable("ENTITY");
            if (entity== Track.enttGpx.ALIEN) continue;
            if (curEnt==null){
                curEnt=entity;
                msg=hand.obtainMessage(msgTyp.LOC.ordinal(),loc);
                hand.sendMessage(msg);
            } else {
                switch (curEnt){
                    case WPT:
                        curEnt=entity;
                        msg=hand.obtainMessage(msgTyp.LOC.ordinal(),loc);
                        hand.sendMessage(msg);
                        break;
                    case RTE:
                        if (entity== Track.enttGpx.RTEWPT){
                            loc=meterNext(track);
                            continue;
                        }
                        curEnt=entity;
                        msg=hand.obtainMessage(msgTyp.LOC.ordinal(),loc);
                        hand.sendMessage(msg);
                        break;
                    case TRK:
                        if (entity== Track.enttGpx.TRKWPT){
                            loc=meterNext(track);
                            continue;
                        }
                        curEnt=entity;
                        msg=hand.obtainMessage(msgTyp.LOC.ordinal(),loc);
                        hand.sendMessage(msg);
                        break;
                    default:
                        loc=meterNext(track);
                        continue;
                }
            }
            loc=meterNext(track);
        }
        track.close();
    }

    void saver(String path, FileWriter outGpx,
               boolean[] setOptions, boolean mkRte, String orgName){
        if (orgName!=null && subst!=null){
            n2find=orgName.substring(3);
            if (orgName.startsWith("T:")){
                substEntity= Track.enttGpx.TRK;
                reName=true;
            } else if (orgName.startsWith("R:")){
                substEntity= Track.enttGpx.RTE;
                reName=true;
            } else if (orgName.startsWith("P:")){
                substEntity= Track.enttGpx.WPT;
                reName=true;
            }
        }
        Location loc=null;
        Track.enttGpx entity= Track.enttGpx.ALIEN;
        Track track=new Track();
        size=track.open(path);
        lastPos=0L;
        msg=hand.obtainMessage(msgTyp.SZ.ordinal(),size);
        hand.sendMessage(msg);
        if (size==null || size<1) return;
        int index=0;
        loc=meterNext(track);
        while (loc!=null){
            entity=(Track.enttGpx)loc.getExtras().getSerializable("ENTITY");
            if (entity== Track.enttGpx.ALIEN){
                loc=meterNext(track);
                continue;
            }
            switch (entity) {
                case WPT:
                    if (reName || setOptions[index++]){
                        nbWpt++;
                        if (!wWpt(outGpx, loc, false)) return;
                    }
                    break;
                case RTE:
                    if (!reName  && !setOptions[index++]){
                        loc = meterNext(track);
                        continue;
                    }
                    nbRte++;
                    rtePtNb=0;
                    if (!wRte(outGpx,loc)) return;
                    do {
                        loc=track.nextPt();
                        if (loc!=null) {
                            entity = (Track.enttGpx) loc.getExtras().getSerializable("ENTITY");
                            if (entity == Track.enttGpx.RTEWPT) {
                                rtePtNb++;
                                if (!wWpt(outGpx, loc,false)) return;
                            }
                        }
                    } while (loc!=null && entity== Track.enttGpx.RTEWPT);
                    if (!eRte(outGpx)) return;
                    continue;
                case TRK:
                    if (!reName && !setOptions[index++]){
                        loc = meterNext(track);
                        continue;
                    }
                    nbTrk++;
                    rtePtNb=0;
                    if (mkRte) {
                        prevRtePt=null;
                        if (!wRte(outGpx, loc)) return;
                    } else {
                        if (!wTrk(outGpx, loc)) return;
                    }
                    do {
                        loc=meterNext(track);
                        if (loc!=null) {
                            entity = (Track.enttGpx) loc.getExtras().getSerializable("ENTITY");
                            if (entity == Track.enttGpx.TRKWPT) {
                                rtePtNb++;
                                if (!wWpt(outGpx, loc,mkRte)) return;
                            }
                        }
                    } while (loc!=null && entity== Track.enttGpx.TRKWPT);
                    if (mkRte){
                        if (!eRte(outGpx)) return;
                    } else {
                        if (!eTrk(outGpx)) return;
                    }
                    continue;
            }
            loc=meterNext(track);
        }
        track.close();
    }

    boolean lwDist(Location loc){
        if (prevRtePt==null){
            prevRtePt=loc;
            return false;
        }
        if (prevRtePt.distanceTo(loc)<50.0) return true;
        prevRtePt=loc;
        return false;
    }

    boolean wWpt(FileWriter outGpx, Location loc, boolean mkRte){
        Calendar cal=Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        Track.enttGpx entity=(Track.enttGpx)loc.getExtras().getSerializable("ENTITY");
        try {
            switch (entity){
                case WPT:
                    outGpx.write(" <wpt ");
                    break;
                case RTEWPT:
                    outGpx.write(" <rtept ");
                    break;
                case TRKWPT:
                    if (mkRte) {
                        if (lwDist(loc)) return true;
                        outGpx.write(" <rtept ");
                    }
                    else outGpx.write(" <trkpt ");
                    break;
            }
            outGpx.write(String.format(Locale.ENGLISH,"lat=\"%.8f\" ",
                             loc.getLatitude()));
            outGpx.write(String.format(Locale.ENGLISH,"lon=\"%.8f\">",
                             loc.getLongitude()));
            if (loc.hasAltitude()) outGpx.write(String.format(Locale.ENGLISH,
                             "  <ele>%.3f</ele>",loc.getAltitude()));
            Long when=loc.getTime();
            if (entity==Track.enttGpx.TRKWPT && mkRte) when=null;
            if (when!=null && when>0){
                cal.setTimeInMillis(when);
                outGpx.write(String.format(Locale.ENGLISH," <time>%tFT%tTZ</time>",
                        cal,cal));
            }
            String name=loc.getExtras().getString("name",null);
            if (entity==Track.enttGpx.WPT && name==null){
                if (name==null) name="WPT "+nbWpt.toString();
                if (reName && substEntity== Track.enttGpx.WPT && n2find.contentEquals(name)){
                    name=subst;
                }
            }
            name=nameNorm(name);
            if (name!=null) outGpx.write("  <name>"+name+"</name>");
//            else if (mkRte) outGpx.write("  <cmt>"+rtePtNb.toString()+"</cmt>");
            switch (entity){
                case WPT:
                    outGpx.write("  </wpt>\n");
                    break;
                case RTEWPT:
                    outGpx.write("  </rtept>\n");
                    break;
                case TRKWPT:
                    if (mkRte) outGpx.write("  </rtept>\n");
                    else outGpx.write("  </trkpt>\n");
                    break;
            }
            return true;
        } catch (IOException e){
            msg=hand.obtainMessage(msgTyp.ERROR.ordinal(),e.getMessage());
            hand.sendMessage(msg);
            return false;
        }
    }

    boolean wRte(FileWriter outGpx, Location loc){
        try {
            outGpx.write(" <rte>\n");
            String name=loc.getExtras().getString("name",null);
            if (name==null) name="RTE "+nbRte.toString();
            if (reName && substEntity== Track.enttGpx.RTE && n2find.contentEquals(name)){
                name=subst;
            }
            name=nameNorm(name);
            if (name!=null) outGpx.write("  <name>"+name+"</name>");
            outGpx.write("\n");
            return true;
        } catch (IOException e){
            msg=hand.obtainMessage(msgTyp.ERROR.ordinal(),e.getMessage());
            hand.sendMessage(msg);
            return false;
        }
    }

    boolean eRte(FileWriter outGpx){
        try {
            outGpx.write(" </rte>\n");
            return true;
        } catch (IOException e){
            msg=hand.obtainMessage(msgTyp.ERROR.ordinal(),e.getMessage());
            hand.sendMessage(msg);
            return false;
        }
    }

    boolean wTrk(FileWriter outGpx, Location loc){
        try {
            outGpx.write(" <trk>\n");
            String name=loc.getExtras().getString("name",null);
            if (name==null) name="TRK "+nbTrk.toString();
            if (reName && substEntity== Track.enttGpx.TRK && n2find.contentEquals(name)){
                name=subst;
            }
            name=nameNorm(name);
            if (name!=null) outGpx.write("  <name>"+name+"</name>");
            outGpx.write("<trkseg>\n");
            return true;
        } catch (IOException e){
            msg=hand.obtainMessage(msgTyp.ERROR.ordinal(),e.getMessage());
            hand.sendMessage(msg);
            return false;
        }
    }

    boolean eTrk(FileWriter outGpx){
        try {
            outGpx.write("</trkseg></trk>\n");
            return true;
        } catch (IOException e){
            msg=hand.obtainMessage(msgTyp.ERROR.ordinal(),e.getMessage());
            hand.sendMessage(msg);
            return false;
        }
    }

    Location meterNext(Track track){
        Location loc=track.nextPt();
        if (loc==null) return loc;
        Long pos=track.getPos();
        if (pos>lastPos+3000L){
            msg=hand.obtainMessage(msgTyp.PROG.ordinal(),pos);
            hand.sendMessage(msg);
            lastPos=pos;
        }
        return loc;
    }

    String nameNorm(String name){
        if (name==null) return null;
        byte[] in=name.getBytes(Charset.forName("UTF-8"));
        String norm=new String(in);
        return norm;
    }



    void rdTrk(){
        Location somewhere;
        Location startLoc=null;
        Long startTime=null;
        Long prevTime=null;
        Long thisTime=null;
        Location prevLoc=null;
        Float elapsed=null;
        String name;
        Track.enttGpx entity=null;
        Track.enttGpx outEnt=Track.enttGpx.TRK;
        Track.enttGpx inEnt=Track.enttGpx.TRKWPT;
        String prefix="TRK ";
        if (route){
            outEnt=Track.enttGpx.RTE;
            inEnt=Track.enttGpx.RTEWPT;
            prefix="RTE ";
        }
        nbTrk=0;
        nbRte=0;
        nbWpt=0;
        Track track=new Track();
        size=track.open(filePath);
        msg=hand.obtainMessage(msgTyp.SZ.ordinal(),size);
        hand.sendMessage(msg);
        if (size==null || size<1) return;
        somewhere=meterNext(track);
        Boolean found=false;
        while (!found){
            if (somewhere==null){
                track.close();
                track=null;
                msg=hand.obtainMessage(msgTyp.MISTRK.ordinal());
                hand.sendMessage(msg);
                return;
            }
            entity=(Track.enttGpx) somewhere.getExtras().getSerializable("ENTITY");
            if (entity== outEnt){
                nbTrk++;
                name=somewhere.getExtras().getString("name");
                if (name==null) name=prefix+nbTrk.toString();
                if (trackName.contentEquals(name)) found=true;
            }
            if (!found) somewhere=meterNext(track);
        }
        Boolean inTrk=true;
        nbLoc=0;
        while (inTrk){
            somewhere=meterNext(track);
            if (somewhere==null) break;
            entity=(Track.enttGpx) somewhere.getExtras().getSerializable("ENTITY");
            if (entity!= inEnt) break;
            if (route){
                thisTime=0L;
            } else {
                if (startTime == null) {
                    startLoc = somewhere;
                    startTime = startLoc.getTime();
                    if (startTime == null || startTime <= 0l) break;
                    prevLoc = startLoc;
                    prevTime = startTime;
                }
                thisTime = somewhere.getTime();
                if (thisTime == null || thisTime < prevTime) somewhere.setTime(prevTime);
            }
            msg=hand.obtainMessage(msgTyp.LOC.ordinal(),nbLoc,0,somewhere);
            hand.sendMessage(msg);
            nbLoc++;
            prevLoc=somewhere;
            prevTime=thisTime;
        }
        track.close();
        track=null;
        if (route){
            msg=hand.obtainMessage(msgTyp.ENDLOCR.ordinal());
        } else {
            msg = hand.obtainMessage(msgTyp.ENDLOC.ordinal());
        }
        hand.sendMessage(msg);
    }


}
