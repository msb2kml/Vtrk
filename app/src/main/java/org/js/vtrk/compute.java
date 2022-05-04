package org.js.vtrk;

import android.location.Location;

import java.util.LinkedList;

public class compute {

    Location first=null;

    compute(){

    }

    Float value(Location somewhere){
        return 0.0f;
    }
}

////////////////////////////////////////////////////
class Valt extends compute{

    Float H=0.0f;

    Valt(Location initial){
        first=initial;
        if (first.hasAltitude()){
            Double dH=first.getAltitude();
            H=dH.floatValue();
        }
    }

    Float value(Location somewhere){
        if (first==null) return 0.0f;
        if (somewhere.hasAltitude()){
            Double dH=somewhere.getAltitude();
            H=dH.floatValue();
        }
        return H;
    }
}
////////////////////////////////////////////////////
class Vheight extends compute{

     Float H=null;
     Float nowH=H;

     Vheight(Location initial){
         first=initial;
     }

     Float value(Location somewhere){
         if (first==null) return 0.0f;
         if (somewhere.hasAltitude()){
             Double dH=somewhere.getAltitude();
             if (H==null) H=dH.floatValue();
             nowH=dH.floatValue();
         }
         if (H==null) return 0.0f;
         else return nowH-H;
     }
}
////////////////////////////////////////////////////
class Vdist extends compute{

    Float dist=0.0f;
    Location prevLoc=null;

    Vdist(Location initial){
        first=initial;
    }

    Float value(Location somewhere){
        if (first==null) return 0.0f;
        if (prevLoc!=null) {
            Float step = prevLoc.distanceTo(somewhere) / 1000.0f;
            dist += step;
        }
        prevLoc=somewhere;
        return dist;
    }
}
////////////////////////////////////////////////////
class Vmph extends compute{

    LinkedList<Location> stack=new LinkedList<>();
    Float prevMpH=0.0f;

    Vmph(Location initial){
        first=initial;
        if (first.hasAltitude()) stack.addFirst(first);
    }

    Float value (Location somewhere){
        Location refLoc;
        Double alt;
        if (first==null) return 0.0f;
        if (!somewhere.hasAltitude()) return prevMpH;
        if (stack.size()<1 || (somewhere.getTime()-stack.get(0).getTime()>1000L))
            stack.addFirst(somewhere);
        refLoc=stack.getLast();
        Long dif=somewhere.getTime()-refLoc.getTime();
        if (dif<10000L) return prevMpH;
        if (stack.size()>10) stack.removeLast();
        alt=somewhere.getAltitude();
        Double h=alt-refLoc.getAltitude();
        Double MpH=h/dif.doubleValue()*1000.0*3600.0;
        prevMpH=MpH.floatValue();
        return prevMpH;
    }
}
////////////////////////////////////////////////////
class Vmps extends compute{

    LinkedList<Location> stack=new LinkedList<>();
    Float prevMpS=0.0f;

    Vmps(Location initial){
        first=initial;
        if (first.hasAltitude()) stack.addFirst(first);
    }

    Float value (Location somewhere){
        Location refLoc;
        Double alt;
        if (first==null) return 0.0f;
        if (!somewhere.hasAltitude()) return prevMpS;
        if (stack.size()<1 || (somewhere.getTime()-stack.get(0).getTime()>1000L))
            stack.addFirst(somewhere);
        refLoc=stack.getLast();
        Long dif=somewhere.getTime()-refLoc.getTime();
        if (dif<2000L) return prevMpS;
        if (stack.size()>10) stack.removeLast();
        alt=somewhere.getAltitude();
        Double h=alt-refLoc.getAltitude();
        Double MpS=h/dif.doubleValue()*1000.0;
        prevMpS=MpS.floatValue();
        return prevMpS;
    }
}
////////////////////////////////////////////////////
class Vspd extends compute{

    LinkedList<Location> stack=new LinkedList<>();
    Float prevSpd=0.0f;

    Vspd(Location initial){
        first=initial;
    }

    Float value(Location somewhere){
        Location refLoc;
        if (first==null) return 0.0f;
        if (stack.size()==0 || somewhere.getTime()-stack.get(0).getTime()>1000L)
            stack.addFirst(somewhere);
        refLoc=stack.getLast();
        Long dif=somewhere.getTime()-refLoc.getTime();
        if (dif<5000L) return prevSpd;
        if (stack.size()>10) stack.removeLast();
        Float step=refLoc.distanceTo(somewhere);
        prevSpd=step/dif.floatValue()*3600.0f;
        return prevSpd;
    }
}
////////////////////////////////////////////////////
class Vslp extends compute{

    LinkedList<Location> stack=new LinkedList<>();
    Float prevSlp=0.0f;

    Vslp(Location initial){
        first=initial;
        if (first.hasAltitude()) stack.addFirst(first);
    }

    Float value(Location somewhere){
        Location refLoc;
        if (first==null) return 0.0f;
        if (!somewhere.hasAltitude()) return prevSlp;
        if (stack.size()==0){
            stack.addFirst(somewhere);
            return 0.0f;
        }
        Double alt=somewhere.getAltitude();
        Double alt0=stack.get(0).getAltitude();
        Double difAlt=alt-alt0;
        if (Math.abs(difAlt)>1.0) stack.addFirst(somewhere);
        refLoc=stack.getLast();
        Float dist=refLoc.distanceTo(somewhere);
        difAlt=alt-refLoc.getAltitude();
        if (dist>=3.0f){
            prevSlp=difAlt.floatValue()/dist*100.0f;
            if (stack.size()>3) stack.removeLast();
        }
        return prevSlp;
    }
}
////////////////////////////////////////////////////
class Vgain extends compute{

    Float gain=0.0f;
    Location prevPos=null;
    Double prevAlt;

    Vgain(Location initial){
        first=initial;
        if (initial.hasAltitude()){
            prevPos=first;
            prevAlt=prevPos.getAltitude();
        }
    }

    Float value(Location somewhere){
        if (first==null) return 0.0f;
        if (!somewhere.hasAltitude()) return gain;
        Double alt=somewhere.getAltitude();
        if (prevPos==null){
            prevPos=somewhere;
            prevAlt=alt;
            return 0.0f;
        }
        Double dif=alt-prevAlt;
        if (Math.abs(dif)>1.0){
            if (dif>0.0) gain+=dif.floatValue();
            prevPos=somewhere;
            prevAlt=alt;
        }
        return gain;
    }

}
////////////////////////////////////////////////////
class Vdrop extends compute{

    Float drop=0.0f;
    Location prevPos=null;
    Double prevAlt;

    Vdrop(Location initial){
        first=initial;
        if (initial.hasAltitude()) {
            prevPos = first;
            prevAlt = prevPos.getAltitude();
        }
    }

    Float value(Location somewhere) {
        if (first == null) return 0.0f;
        if (!somewhere.hasAltitude()) return drop;
        Double alt = somewhere.getAltitude();
        if (prevPos == null) {
            prevPos = somewhere;
            prevAlt = alt;
            return 0.0f;
        }
        Double dif = prevAlt - alt;
        if (Math.abs(dif) > 1.0) {
            if (dif > 0.0) drop += dif.floatValue();
            prevPos = somewhere;
            prevAlt = alt;
        }
        return drop;
    }

}
