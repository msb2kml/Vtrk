package org.js.vtrk;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    String exPath=Environment.getExternalStorageDirectory().getAbsolutePath();
    Context context;
    String Directory;
    String filePath=null;

    TextView tFile=null;
    TextView tTime;
    TextView tType;
    ProgressBar pBar;
    RadioGroup rSpeed;
    RadioButton rSp1;
    RadioButton rSp2;
    RadioButton rSp10;
    Button bSkp0;
    Button bSkp2;
    Button bSkp10;
    Button bStop;
    Button bSelect;
    Button bEntire;
    Button bcolBy;
    EditText etRed;
    EditText etBlue;
    Track track=null;
    Long size;
    Integer nbWpt =0;
    Integer nbTrk=0;
    Integer nbRte=0;
    boolean running=false;
    Long lastTrk=null;
    Long divisor=1L;
    Long toSkip=0L;
    Long startTime=null;
    Intent intentMap=null;
    Boolean runningMap=false;
    Boolean waitMap=false;
    Double zoom=15.0;
    Boolean setStart=true;
    Location startLoc=null;
    Location centerLoc=null;
    Double prevAlt=null;
    Location dispLoc=null;
    Location prevLoc=null;
    Boolean startLine=true;
    Track.enttGpx curEntity= Track.enttGpx.ALIEN;
    String curEntName=null;
    String fileName;
    Boolean Tail=true;
    Double minAlt=null;
    Double maxAlt=null;
    Double blueHeight;
    Double redHeight;
    Double valBlue=null;
    Double valRed=null;
    Double minVal=null;
    Double maxVal=null;
    static int colNone=0;
    static int colHeight=1;
    static int colMpS=2;
    static int colMpH=3;
    static int colKpH=4;
    static int colSlp=5;
    String[] defBlue={"-","-200.0","-2.0","-400.0","0.0","-40.0"};
    String[] defRed={"-","200.0","2.0","400.0","10.0","40.0"};
    Integer colSrc=colNone;
    String[] head={ " - (none) ",
                    " height above start ",
                    " climb rate m/s ",
                    " climb rate m/h ",
                    " speed km/h ",
                    " slope %"};
    String[] Labels={"Alt.",
                     "Height",
                     "m/s",
                     "m/h",
                     "km/h",
                     "%"};
    Integer[] lineColor={ Color.rgb(0x00,0x00,0xFF),
                          Color.rgb(0x00,0x63,0xF3),
                          Color.rgb(0x00,0x92,0xDE),
                          Color.rgb(0x00,0xB7,0xC2),
                          Color.rgb(0x00,0xD6,0xA0),
                          Color.rgb(0x54,0xDD,0x74),
                          Color.rgb(0x85,0xE0,0x46),
                          Color.rgb(0xAD,0xE1,0x00),
                          Color.rgb(0xD9,0xC6,0x00),
                          Color.rgb(0xFF,0xA5,0x00),
                          Color.rgb(0xFF,0x78,0x00),
                          Color.rgb(0xFF,0x00,0x00)};
    int nColor=lineColor.length;
    LinkedList<Location> stack=new LinkedList<>();

    Haversine haver=new Haversine();

    IntentFilter filter=new IntentFilter("org.js.ACK");
    private Handler mHandler=new Handler();
    private Runnable timerTask=new Runnable() {
        @Override
        public void run() {
            dispatch(2);
        }
    };

    private final BroadcastReceiver mReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String origin=intent.getStringExtra("NAME");
            unregisterReceiver(mReceiver);
            waitMap=false;
            dispatch(1);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context=getApplicationContext();
        fetchPref();
        PackageManager Pm=getPackageManager();
        List<PackageInfo> allPack=Pm.getInstalledPackages(0);
        for (PackageInfo AI :allPack) {
            String zz=AI.packageName;
            if (zz.matches("org.js.Msb2Map")){
                intentMap=Pm.getLaunchIntentForPackage(zz);
                break;
            }
        }
        if (intentMap==null){
            Toast.makeText(context,"Missing Msb2Map application",Toast.LENGTH_LONG).show();
            finish();
        }
        Intent intent=getIntent();
        if (intent!=null){
            Uri uri=intent.getData();
            if (uri!=null){
                String sc=uri.getScheme();
                if (sc.contentEquals("file")){
                    filePath=uri.getPath();
                }

            }
        }
    }

    void fetchPref(){
        SharedPreferences pref=context.getSharedPreferences(
                context.getString(R.string.PrefName),0);
        Directory=pref.getString("Directory",exPath);
        File d=new File(Directory);
        if (!d.exists()) Directory=exPath;
    }

    void putPref(){
        SharedPreferences pref=context.getSharedPreferences(
                context.getString(R.string.PrefName),0);
        SharedPreferences.Editor edit=pref.edit();
        edit.putString("Directory",Directory);
        edit.apply();
    }

    @Override
    protected void onStart(){
        super.onStart();
        String state = Environment.getExternalStorageState();
        Boolean mountedSD = state.contains(Environment.MEDIA_MOUNTED);
        if (!mountedSD) {
            Toast.makeText(context, exPath + " not mounted!", Toast.LENGTH_LONG).show();
            finish();
        }
        Boolean hasPermission = (ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        if (!hasPermission) {
            Toast.makeText(context,
                        "This application need to read " + exPath, Toast.LENGTH_LONG).show();
            ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
        } else start1();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                          String permissions[], int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if (requestCode==100){
            if (grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                start1();
            } else finish();
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        fromMap();
    }

    public void fromMap(){
        if (runningMap) {
            running = false;
            runningMap = false;
//            Toast.makeText(context, "Return from map", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        switch (requestCode){
            case 2:
                if (resultCode==RESULT_OK){
                    filePath=data.getStringExtra("Path");
                    if (filePath==null || filePath.isEmpty()) finish();
                    else {
                        start2();
                    }
                } else finish();
                break;
        }
    }

    public void start1(){
        if (tFile!=null) return;
        tFile=(TextView) findViewById(R.id.title);
        tType=(TextView) findViewById(R.id.type);
        tTime=(TextView) findViewById(R.id.timeTrack);
        pBar=(ProgressBar) findViewById(R.id.progress);
        rSpeed=(RadioGroup) findViewById(R.id.speed);
        rSp1=(RadioButton) findViewById(R.id.sp1);
        rSp2=(RadioButton) findViewById(R.id.sp2);
        rSp10=(RadioButton) findViewById(R.id.sp10);
        bSkp0=(Button) findViewById(R.id.skp0);
        bSkp2=(Button) findViewById(R.id.skp2);
        bSkp10=(Button) findViewById(R.id.skp10);
        bStop=(Button) findViewById(R.id.stop);
        bSelect=(Button) findViewById(R.id.selGpx);
        bEntire=(Button) findViewById(R.id.entire);
        bcolBy=(Button) findViewById(R.id.colBy);
        etBlue=(EditText) findViewById(R.id.blueVal);
        etRed=(EditText) findViewById(R.id.redVal);
        bcolBy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectCol();
            }
        });
        bStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        bSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selGpx();
            }
        });
        bEntire.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (colSrc!=colNone){
                    if (!getValCol()){
                        Toast.makeText(context,"Please check the Blue and Red values.",
                             Toast.LENGTH_LONG).show();
                        return;
                    }
                }
                Tail=false;
                nbWpt =0;
                nbTrk=0;
                nbRte=0;
                if (track!=null){
                    track.close();
                    track=null;
                }
                running=true;
                dispatch(0);
//                entireTrack();
            }
        });
        bSkp0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (colSrc!=colNone){
                    if (!getValCol()){
                        Toast.makeText(context,"Please check the Blue and Red values.",
                             Toast.LENGTH_LONG).show();
                        return;
                    }
                }
                Tail=true;
                running=true;
                getSpeed();
                toSkip=0L;
                mHandler.postDelayed(timerTask,300L);
            }
        });
        bSkp2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (colSrc!=colNone){
                    if (!getValCol()){
                        Toast.makeText(context,"Please check the Blue and Red values.",
                             Toast.LENGTH_LONG).show();
                        return;
                    }
                }
                Tail=true;
                running=true;
                getSpeed();
                toSkip=120000L;
                mHandler.postDelayed(timerTask,300L);
            }
        });
        bSkp10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (colSrc!=colNone){
                    if (!getValCol()){
                        Toast.makeText(context,"Please check the Blue and Red values.",
                             Toast.LENGTH_LONG).show();
                        return;
                    }
                }
                Tail=true;
                running=true;
                getSpeed();
                toSkip=600000L;
                mHandler.postDelayed(timerTask,300L);
            }
        });
        if (filePath==null) {
            selGpx();
        } else {
            start2();
        }
    }

    void selGpx(){
        tTime.setText("0");
        pBar.setProgress(0);
        if (track!=null) track.close();
        nbWpt =0;
        nbRte=0;
        nbTrk=0;
        running=false;
        track=null;
        Intent intent = new Intent(MainActivity.this, Selector.class);
        intent.putExtra("CurrentDir", Directory);
        intent.putExtra("WithDir", false);
        intent.putExtra("Mask", ".+\\.gpx");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivityForResult(intent, 2);
    }

    Location introTyp(){
        Track.enttGpx entity;
        if (track!=null) track.close();
        Location first=initTrack();
        if (first==null) return null;
        Track.enttGpx first_ent=(Track.enttGpx) first.getExtras().getSerializable("ENTITY");
        Long position=track.getPos();
        Location chlngr=first;
        while (position<3000 && chlngr!=null){
            entity=(Track.enttGpx) chlngr.getExtras().getSerializable("ENTITY");
            switch (entity){
                case TRK:
                case RTE:
                    track.close();
                    track=null;
                    return chlngr;
                case WPT:
                    if (first_ent!=Track.enttGpx.WPT){
                        first=chlngr;
                        first_ent=entity;
                    }
                    break;
            }
            chlngr=readTrk();
            position=track.getPos();
        }
        track.close();
        track=null;
        return first;
    }

    void selectCol(){
        AlertDialog.Builder build=new AlertDialog.Builder(this);
        build.setTitle("Select a method for coloring")
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        setColorz(null);
                    }
                })
                .setItems(head, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setColorz(which);
                    }
                });
        build.show();
    }

    void setColorz(Integer which){
        if (which==null) return;
        bcolBy.setText(head[which]);
        colSrc=which;
        if (colSrc==colNone){
            etBlue.setEnabled(false);
            etRed.setEnabled(false);
        } else {
            etBlue.setEnabled(true);
            etRed.setEnabled(true);
        }
        etBlue.setText(defBlue[colSrc]);
        etRed.setText(defRed[colSrc]);
    }

    Boolean getValCol(){
        String field;
        NumberFormat nfe=NumberFormat.getInstance(Locale.ENGLISH);
        Number num;
        valBlue=null;
        valRed=null;
        field=etBlue.getText().toString();
        if (field!=null) field=field.trim();
        if (field==null || field.isEmpty()){ return false; }
        try {
            num=nfe.parse(field);
            valBlue=num.doubleValue();
        } catch (ParseException e) {return false;}
        defBlue[colSrc]=field;
        field=etRed.getText().toString();
        if (field!=null) field=field.trim();
        if (field==null || field.isEmpty()){ return false; }
        try {
            num=nfe.parse(field);
            valRed=num.doubleValue();
        } catch (ParseException e) {return false; }
        defRed[colSrc]=field;
        if (Math.abs(valBlue-valRed)<0.001f) return false;
        return true;
    }

    public void start2(){
        String note;
        File f=new File(filePath);
        Location first=introTyp();
        if (first==null){
            note="??";
        } else {
            String nameTyp=first.getExtras().getString("name",null);
            Track.enttGpx entity=(Track.enttGpx)first.getExtras().getSerializable("ENTITY");
            switch (entity){
                case WPT:
                    note="Waypoint: "+nameTyp;
                    break;
                case TRK:
                    note="Track: "+nameTyp;
                    break;
                case RTE:
                    note="Route: "+nameTyp;
                    break;
                default:
                    note="??: "+nameTyp;
            }
        }
        tType.setText(note);
        Directory=f.getParent();
        fileName=f.getName();
        tFile.setText(fileName);
        pBar.setProgress(0);
        putPref();
    }

    public void getSpeed(){
        int id=rSpeed.getCheckedRadioButtonId();
        switch (id){
            case R.id.sp1:
                divisor=1l;
                break;
            case R.id.sp2:
                divisor=2l;
                break;
            case R.id.sp10:
                divisor=10l;
                break;
        }
    }

    Location initTrack(){
        Location firstLoc=null;
        if (track!=null) track.close();
        track=new Track();
        size=track.open(filePath);
        nbWpt =0;
        nbRte=0;
        nbTrk=0;
        firstLoc=readTrk();
        if (firstLoc==null){
            eof();
            Toast.makeText(context,"No valid item in "+fileName,Toast.LENGTH_LONG).show();
            return null;
        }
        minVal=null;
        maxVal=null;
        return firstLoc;
    }

    public Location readTrk(){
        Location loc;
        Long position;
        Track.enttGpx entity=null;
        String name;
        String entName;
        if (track==null) return null;
        while (true) {
            loc = track.nextPt();
            if (loc == null) return loc;
            position = track.getPos();
            Float prog = (100.0f * Float.valueOf(position)) / Float.valueOf(size);
            pBar.setProgress(prog.intValue());
            if (startTime != null && loc.getTime() != 0L) {
                Long sec = (loc.getTime() - startTime) / 1000L;
                Long hour = sec / 3600L;
                Long min = (sec - hour * 3600L) / 60L;
                Long s = (sec - hour * 3600L - min * 60L);
                tTime.setText(String.format("%02d:%02d:%02d", hour, min, s));
            }
            return loc;
        }
    }

    void eof(){
        track.close();
        running=false;
        Toast.makeText(context,"END OF FILE",Toast.LENGTH_LONG).show();
        track=null;
    }

    public Location skip(Location currentLoc){
        Location loc;
        if (toSkip==0l || currentLoc.getTime()==0L) return currentLoc;
        loc=currentLoc;
        Long target=currentLoc.getTime()+toSkip;
        while (loc.getTime()<target){
            loc=readTrk();
            if (loc==null ||
                    loc.getExtras().getSerializable("ENTITY")!=Track.enttGpx.TRKWPT){
                toSkip=0L;
                return loc;
            }
        }
        toSkip=0L;
        return loc;
    }

    Location smooth(Location disploc){
        Location refLoc;
        if (colSrc==colNone) return null;
        if ((colSrc==colHeight || colSrc==colSlp) && !disploc.hasAltitude()) return null;
        if (stack.size()==0) {
            stack.addFirst(disploc);
            return null;
        }
        if (colSrc==colHeight || colSrc==colSlp){
            Double alt=disploc.getAltitude();
            Double alt0=stack.get(0).getAltitude();
            if (Math.abs(alt-alt0)>1.0) stack.addFirst(disploc);
            Double altL=stack.getLast().getAltitude();
//            if (Math.abs(alt-altL)<1.0) return null;
        } else {
            if ((disploc.getTime() - stack.get(0).getTime() > 1000L)) {
                stack.addFirst(disploc);
            }
            Long dif = disploc.getTime() - stack.getLast().getTime();
            if (dif < 10000L) {
                return null;
            }
        }
        refLoc = stack.getLast();
        if (stack.size() > 10) stack.removeLast();
        return refLoc;
    }

    int colorz(Double val){
        if (val==null) return Color.BLACK;
        double norm=(val-valBlue)/(valRed-valBlue);
        int v=(int)Math.round(norm*nColor);
        v=Math.max(1,Math.min(nColor,v))-1;
        return lineColor[v];
    }

    void launchMap(){
        stack.clear();
        setStart=true;
        Intent nt=(Intent) intentMap.clone();
        nt.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        nt.putExtra("CALLER",context.getString(R.string.app_name));
        nt.putExtra("CENTER",centerLoc);
        nt.putExtra("StartGPS",false);
        nt.putExtra("Tail",Tail);
        if (zoom!=null) nt.putExtra("ZOOM",zoom);
        zoom=null;
        runningMap=true;
        startActivity(nt);
        waitMap=true;
        registerReceiver(mReceiver,filter);
        return;
    }

    Double getVal(Location thisLoc){
        Double alt;
        if (thisLoc.hasAltitude() && startLoc.hasAltitude()) alt=thisLoc.getAltitude();
        else alt=null;
        if (colSrc==colNone){
            return alt;
        } else if (colSrc==colHeight){
            if (alt!=null) return alt-startLoc.getAltitude();
            else return null;
        } else if (colSrc==colMpS){
            if (alt==null) return null;
            else {
                Location refLoc = smooth(thisLoc);
                if (refLoc == null) return null;
                else {
                    Long dif = thisLoc.getTime() - refLoc.getTime();
                    Double h = alt - refLoc.getAltitude();
                    return h / dif.doubleValue() * 1000.0;
                }
            }
        } else if (colSrc==colMpH){
            if (alt==null) return null;
            else {
                Location refLoc = smooth(thisLoc);
                if (refLoc == null) return null;
                else {
                    Long dif = thisLoc.getTime() - refLoc.getTime();
                    Double h = alt - refLoc.getAltitude();
                    return h / dif.doubleValue() * 1000.0 * 3600.0;
                }
            }
        } else if (colSrc==colKpH){
            if (startLoc.getTime()==0L || thisLoc.getTime()==0L) return null;
            else {
                Location refLoc = smooth(thisLoc);
                if (refLoc == null) return null;
                else {
                    Long dif = thisLoc.getTime() - refLoc.getTime();
                    Double dist = haver.lHaversine(refLoc, thisLoc);
                    return dist / dif.doubleValue() * 1000.0 * 3600.0;
                }
            }
        } else if (colSrc==colSlp){
            if (alt==null) return null;
            else {
                Location refLoc = smooth(thisLoc);
                if (refLoc == null) return null;
                else {
                    Double dis = haver.lHaversine(refLoc, thisLoc) * 1000.0;
                    if (dis < 1.0) return null;
                    else {
                        return (alt - refLoc.getAltitude()) / dis * 100.0;
                    }
                }
            }
        } else return null;
    }

    void dispatch(int from){
        if (!running) return;
        if (track==null){
            dispLoc=initTrack();
            if (dispLoc==null){
                selGpx();
                return;
            }
        }
        while (dispLoc!=null) {
            Track.enttGpx entity = (Track.enttGpx) dispLoc.getExtras().getSerializable("ENTITY");
            if (Tail && (entity == Track.enttGpx.TRK || entity == Track.enttGpx.TRKWPT)) {
                if (withTail()) return;
            } else {
                if (noTail()) return;
            }
        }
        eof();
    }

    Boolean noTail(){
        if (dispLoc==null) return true;
        int nbBroadcast=0;
        Track.enttGpx entity=(Track.enttGpx)dispLoc.getExtras().getSerializable("ENTITY");
        switch (entity){
            case WPT:
                curEntity=entity;
            case RTEWPT:
            case TRKWPT:
                if (!runningMap) {
                    centerLoc=dispLoc;
                    launchMap();
                    return true;
                }
                break;
            case TRK:
                curEntName="Track "+dispLoc.getExtras().getString("name",null);
                setStart=true;
                startLoc=null;
                stack.clear();
                minVal=null;
                maxVal=null;
                minAlt=null;
                maxAlt=null;
                curEntity=entity;
                dispLoc=readTrk();
                return false;
            case RTE:
                curEntName="Route "+dispLoc.getExtras().getString("name",null);
                setStart=true;
                startLoc=null;
                stack.clear();
                minVal=null;
                maxVal=null;
                minAlt=null;
                maxAlt=null;
                curEntity=entity;
                dispLoc=readTrk();
                return false;
            case ALIEN:
                track.close();
                Toast.makeText(context,"Sorry, "+fileName+" is not compatible.",
                        Toast.LENGTH_LONG).show();
                selGpx();
                return true;

        }
        while (true){
            nbBroadcast++;
            if (nbBroadcast>100){
                mHandler.postDelayed(timerTask,100L);
                return true;
            }
            entity=(Track.enttGpx) dispLoc.getExtras().getSerializable("ENTITY");
            switch (curEntity){
                case WPT:
                    if (entity!=curEntity) return false;
                    nbWpt++;
                    dispWpt(dispLoc,null);
                    break;
                case TRKWPT:
                case RTEWPT:
                    if (entity!=curEntity) {
                        return false;
                    }
                    dispTrk(dispLoc,false);
                    break;
                case RTE:
                    if (entity!= Track.enttGpx.RTEWPT) return false;
                    if (setStart){
                        if (startLoc==null) {
                            startLoc=dispLoc;
                            startLoc.getExtras().putString("name",curEntName);
                            if (startLoc.hasAltitude()){
                                minAlt=startLoc.getAltitude();
                                maxAlt=minAlt;
                            }
                        }
                        startLine=true;
                        prevAlt=null;
                    }
                    dispTrk(dispLoc,false);
                    if (setStart){
                        nbRte++;
                        dispWpt(startLoc,String.valueOf(nbRte)+": "+ curEntName);
                        setStart=false;
                    }
                    prevLoc=dispLoc;
                    curEntity=entity;
                    break;
                case TRK:
                    if (entity!= Track.enttGpx.TRKWPT) return false;
                    if (setStart){
                        if (startLoc==null) {
                            startLoc=dispLoc;
                            startLoc.getExtras().putString("name",curEntName);
                            if (startLoc.hasAltitude()){
                                minAlt=startLoc.getAltitude();
                                maxAlt=minAlt;
                            }
                        }
                        startLine=true;
                        prevAlt=null;
                    }
                    dispTrk(dispLoc,false);
                    if (setStart){
                        nbTrk++;
                        dispWpt(startLoc,String.valueOf(nbTrk)+": "+ curEntName);
                        setStart=false;
                    }
                    prevLoc=dispLoc;
                    curEntity=entity;
                    break;
            }
            if (!runningMap) return true;
            dispLoc=readTrk();
            if (dispLoc==null) {
                return false;
            }
        }
    }

    Boolean withTail(){
        if (dispLoc==null || !Tail) return false;
        Track.enttGpx entity=(Track.enttGpx)dispLoc.getExtras().getSerializable("ENTITY");
        switch (entity){
            case TRKWPT:
                if (!runningMap){
                    centerLoc=dispLoc;
                    launchMap();
                    return true;
                }
                break;
            case TRK:
                curEntName="Track "+dispLoc.getExtras().getString("name",null);
                setStart=true;
                startLoc=null;
                stack.clear();
                minVal=null;
                maxVal=null;
                minAlt=null;
                maxAlt=null;
                curEntity=entity;
                dispLoc=readTrk();
                return false;
            default:
                return false;
        }
        entity=(Track.enttGpx) dispLoc.getExtras().getSerializable("ENTITY");
        if (entity!= Track.enttGpx.TRKWPT) return false;
        if (setStart){
            if (startLoc==null){
                if (dispLoc.getTime()==0L || !dispLoc.hasAltitude()){
                    Tail=false;
                    return false;
                }
                startLoc=dispLoc;
                startTime=startLoc.getTime();
                startLoc.getExtras().putString("name",curEntName);
                minAlt=startLoc.getAltitude();
                maxAlt=minAlt;
                if (toSkip>0){
                    dispLoc=skip(dispLoc);
                    if (dispLoc==null) return false;
                    entity=(Track.enttGpx) dispLoc.getExtras().getSerializable("ENTITY");
                    if (entity!= Track.enttGpx.TRKWPT) return false;
                }
            }
            startLine=true;
            prevAlt=null;
        }
        dispTrk(dispLoc,true);
        if (setStart){
            nbTrk++;
            dispWpt(startLoc,String.valueOf(nbTrk)+": "+ curEntName);
            setStart=false;
        }
        prevLoc=dispLoc;
        curEntity=entity;
        lastTrk=dispLoc.getTime();
        Long toWait=0L;
        while (toWait<300L){
            dispLoc=readTrk();
            if (dispLoc==null) return false;
            entity=(Track.enttGpx) dispLoc.getExtras().getSerializable("ENTITY");
            if (entity!=Track.enttGpx.TRKWPT) return false;
            if (dispLoc.getTime()>0L) toWait=(dispLoc.getTime()-lastTrk)/divisor;
        }
        mHandler.postDelayed(timerTask,toWait);
        return true;
    }

    void dispWpt(Location loc, String infoBubble){
        Intent nt = new Intent();
        nt.setAction("org.js.LOC");
        nt.putExtra("WPT",loc);
        String namWpt=loc.getExtras().getString("name", "?");;
        if (infoBubble==null) {
            if (loc.hasAltitude()) {
                namWpt = String.format(Locale.ENGLISH, "%s (%.1f)",
                        loc.getExtras().getString("name", "?"), loc.getAltitude());
            }
            nt.putExtra("BUBBLE",namWpt);
        } else {
            nt.putExtra("BUBBLE",infoBubble);
        }
        nt.putExtra("WPT_NAME", namWpt);
        sendBroadcast(nt);
    }

    void dispTrk(Location loc, Boolean actTail){
        Double val=0.0;
        String label="";
        Integer col=Color.BLACK;
        String bubbleMap;
        Intent nt=new Intent();
        label=Labels[colSrc];
        val=getVal(loc);
        if (val==null){
            col=Color.BLACK;
            val=0.0;
        } else {
            if (colSrc==colNone){
                if (prevAlt == null || val > prevAlt) {
                    col=Color.rgb(0xFF, 0x00, 0x00);
                } else {
                    col=Color.rgb(0x00, 0x00, 0xFF);
                }
                prevAlt=val;
            } else {
                col=colorz(val);
            }
            if (minVal==null){
                minVal=val;
                maxVal=val;
            } else {
                minVal=Math.min(minVal,val);
                maxVal=Math.max(maxVal,val);
            }
        }
        if (minVal==null || maxVal==null){
            bubbleMap=null;
        } else {
            if (actTail){
                bubbleMap=String.format(Locale.ENGLISH,"%s %.1f",label,val);
            } else {
                bubbleMap = String.format(Locale.ENGLISH, "%s %.1f to %.1f", label,
                        minVal, maxVal);
            }
        }
        nt.setAction("org.js.LOC");
        nt.putExtra("LOC",loc);
        nt.putExtra("COLOR",col);
        nt.putExtra("BUBBLE",bubbleMap);
        if (startLine){
            nt.putExtra("START",startLine);
            nt.putExtra("Tail",actTail);
            startLine=false;
        }
        sendBroadcast(nt);
    }



}
