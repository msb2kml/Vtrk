package org.js.vtrk;

import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
        ERROR
    }

    public enum Request {
        ENUM,
        SAVE
    }

    Long size;
    Long lastPos;
    int nbWpt=0;
    int nbTrk=0;
    int nbRte=0;

    public void process(Handler handler, Request request, String path, FileWriter outGpx,
                        ArrayList<Location> itemsList, boolean[] setOptions){
        hand=handler;
        if (request==Request.ENUM){
            enumerator(path);
            msg=hand.obtainMessage(msgTyp.END.ordinal());
            hand.sendMessage(msg);
        } else if (request==Request.SAVE){
            nbRte=0;
            nbTrk=0;
            nbWpt=0;
            saver(path,outGpx,itemsList,setOptions);
            try {
                outGpx.flush();
            } catch (IOException e) {
                msg = hand.obtainMessage(msgTyp.ERROR.ordinal(), e.getMessage());
                hand.sendMessage(msg);
            }
            Integer NbWpt=valueOf(nbWpt);
            msg=hand.obtainMessage(msgTyp.ENDS.ordinal(),nbRte,nbTrk,NbWpt);
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

    void saver(String path, FileWriter outGpx, ArrayList<Location> itemsList, boolean[] setOptions){
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
                    if (setOptions[index++]){
                        nbWpt++;
                        if (!wWpt(outGpx,loc)) return;
                    }
                    break;
                case RTE:
                    if (!setOptions[index++]){
                        loc=meterNext(track);
                        continue;
                    }
                    nbRte++;
                    if (!wRte(outGpx,loc)) return;
                    do {
                        loc=track.nextPt();
                        if (loc!=null) {
                            entity = (Track.enttGpx) loc.getExtras().getSerializable("ENTITY");
                            if (entity == Track.enttGpx.RTEWPT) {
                                if (!wWpt(outGpx, loc)) return;
                            }
                        }
                    } while (loc!=null && entity== Track.enttGpx.RTEWPT);
                    if (!eRte(outGpx)) return;
                    continue;
                case TRK:
                    if (!setOptions[index++]){
                        loc=meterNext(track);
                        continue;
                    }
                    nbTrk++;
                    if (!wTrk(outGpx,loc)) return;
                    do {
                        loc=meterNext(track);
                        if (loc!=null) {
                            entity = (Track.enttGpx) loc.getExtras().getSerializable("ENTITY");
                            if (entity == Track.enttGpx.TRKWPT) {
                                if (!wWpt(outGpx, loc)) return;
                            }
                        }
                    } while (loc!=null && entity== Track.enttGpx.TRKWPT);
                    if (!eTrk(outGpx)) return;
                    continue;
            }
            loc=meterNext(track);
        }
        track.close();
    }

    boolean wWpt(FileWriter outGpx, Location loc){
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
                    outGpx.write(" <trkpt ");
                    break;
            }
            outGpx.write(String.format(Locale.ENGLISH,"lat=\"%.8f\" ",
                             loc.getLatitude()));
            outGpx.write(String.format(Locale.ENGLISH,"lon=\"%.8f\">\n",
                             loc.getLongitude()));
            if (loc.hasAltitude()) outGpx.write(String.format(Locale.ENGLISH,
                             "  <ele>%.3f</ele>\n",loc.getAltitude()));
            Long when=loc.getTime();
            if (when!=null && when>0){
                cal.setTimeInMillis(when);
                outGpx.write(String.format(Locale.ENGLISH," <time>%tFT%tTZ</time>\n",
                        cal,cal));
            }
            String name=loc.getExtras().getString("name",null);
            if (name!=null) outGpx.write("  <name>"+name+"</name>");
            switch (entity){
                case WPT:
                    outGpx.write("  </wpt>\n");
                    break;
                case RTEWPT:
                    outGpx.write("  </rtept>\n");
                    break;
                case TRKWPT:
                    outGpx.write("  </trkpt>\n");
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


}
