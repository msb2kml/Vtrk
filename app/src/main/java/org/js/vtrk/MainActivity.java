package org.js.vtrk;

import android.Manifest;
import android.app.Activity;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    Button bActions;
    Button bWpt;
    Button bInf;
    Button bcolBy;
    Button bRef;
    Button bCenter;
    EditText etRed;
    EditText etBlue;
    CheckBox ckUp;
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
    Float prevAlt=null;
    Location dispLoc=null;
    Location prevLoc=null;
    Boolean startLine=true;
    Track.enttGpx curEntity= Track.enttGpx.ALIEN;
    String curEntName=null;
    String fileName;
    String refPath =null;
    String refDirectory =null;
    Boolean inRef =false;
    Boolean Tail=true;
    Float minAlt=null;
    Float maxAlt=null;
    Float valBlue=null;
    Float valRed=null;
    Float minVal=null;
    Float maxVal=null;
    Double totDist=null;
    Double totGain=null;
    Double totDrop=null;
    static int colNone=0;
    static int colHeight=1;
    static int colMpS=2;
    static int colMpH=3;
    static int colKpH=4;
    static int colSlp=5;
    static int colDist=6;
    static int colVgain=7;
    static int colVdrop=8;
    String[] defBlue={"-","-200.0","-2.0","-400.0","0.0","-40.0","0.0","0","0"};
    String[] defRed={"-","200.0","2.0","400.0","10.0","40.0","10.0","1000","1000"};
    Integer colSrc=colNone;
    compute CurComp=null;
    String[] head={ " - (none) ",
                    " height above start ",
                    " climb rate m/s ",
                    " climb rate m/h ",
                    " speed km/h ",
                    " slope %",
                    " distance ",
                    " Vert. gain",
                    " Vert. drop"};
    String[] Labels={"Alt.",
                     "Height",
                     "m/s",
                     "m/h",
                     "km/h",
                     "%",
                     "Km",
                     "m +",
                     "m -"};
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
    int HalfMagenta=Color.argb(0x80,0xFF,0x00,0xFF);
    LinkedList<Location> stack=new LinkedList<>();
    Boolean picking=false;
    class NamedLoc {
        String name;
        Location loc;
    }
    Map<Integer,NamedLoc> picked=new HashMap();
    Boolean asWpt=true;
    String pkdRteName=null;
    Location arrowOrg=null;
    Boolean rotMap=false;
    Map<String,Location> centerPos=new HashMap<>();
    String centerName=null;
    Boolean singleLoc=false;


    WeakReference<MainActivity> mAct;

    Haversine haver=new Haversine();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context=getApplicationContext();
        mAct=new WeakReference<MainActivity>(this);
        fetchPref();
        PackageManager Pm=getPackageManager();
        List<PackageInfo> allPack=Pm.getInstalledPackages(0);
        for (PackageInfo AI :allPack) {
            String zz=AI.packageName;
            if (zz.matches("org.js.Msb2Map")){
                intentMap=Pm.getLaunchIntentForPackage(zz);
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
                } else if (sc.contentEquals("geo")){
                    String geo=uri.getSchemeSpecificPart();
                    singleLoc=validGeo(geo);
                    if (!singleLoc){
                        Toast.makeText(context,"Unknown "+geo,Toast.LENGTH_LONG);
                    }
                }

            }
        }
    }

    Boolean validGeo(String geo){
        String patrnLatLon="(-?[0-9.]+)";
        String patrnAndro="q=(-?[0-9.]+),(-?[0-9.]+)\\(([^(]+)\\)";
        Pattern pLatLon=Pattern.compile(patrnLatLon);
        Pattern pAndro=Pattern.compile(patrnAndro);
        Matcher m;
        Location where=null;
        String name=null;
        String[] part=geo.split("\\?");
        String LatLon;
        if (part.length<1) LatLon=geo;
        else LatLon=part[0];
        if (!LatLon.contentEquals("0,0")){
            where=new Location("");
            name=LatLon;
            String[] coord=LatLon.split(",");
            if (coord.length<2) return false;
            m=pLatLon.matcher(coord[0]);
            if (!m.find()) return false;
            try {
                where.setLatitude(Double.parseDouble(m.group(1)));
            } catch (NumberFormatException e){
                return false;
            }
            m=pLatLon.matcher(coord[1]);
            if (!m.find()) return false;
            try {
                where.setLongitude(Double.parseDouble(m.group(1)));
            } catch (NumberFormatException e){
                return false;
            }
            if (coord.length>2){
                m=pLatLon.matcher(coord[2]);
                if (!m.find()) return false;
                try {
                    where.setAltitude(Double.parseDouble(m.group(1)));
                } catch (NumberFormatException e){
                    return false;
                }
            }
        }
        if (part.length>1){
            m=pAndro.matcher(part[1]);
            if (m.find()){
                int n=m.groupCount();
                if (n==3){
                    name=m.group(3);
                } else if (name==null) name=part[1];
                Double lat=null;
                Double lon=null;
                String s=m.group(1);
                try {
                    lat=Double.parseDouble(s);
                } catch (NumberFormatException e){
                    lat=null;
                }
                s=m.group(2);
                try {
                    lon=Double.parseDouble(s);
                } catch (NumberFormatException e){
                    lon=null;
                }
                if (lat!=null && lon!=null){
                    if (where==null) where=new Location("");
                    where.setLatitude(lat);
                    where.setLongitude(lon);
                }
            }
        }
        if (where==null) return false;
        Bundle bundle=new Bundle();
        where.setExtras(bundle);
        where.getExtras().putString("name",name);
        centerPos.put("P: "+name,where);
        centerName=name;
        return true;
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
        Boolean ronlySD=Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
        if (!mountedSD || ronlySD) {
            Toast.makeText(context, exPath + " not mounted!", Toast.LENGTH_LONG).show();
            finish();
        }
        Boolean hasPermission = (ContextCompat.checkSelfPermission(context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        if (!hasPermission) {
            Toast.makeText(context,
                        "This application need to read/write " + exPath, Toast.LENGTH_LONG).show();
            ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
        } else start1();
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
        bActions=(Button) findViewById(R.id.actions);
        bcolBy=(Button) findViewById(R.id.colBy);
        etBlue=(EditText) findViewById(R.id.blueVal);
        etRed=(EditText) findViewById(R.id.redVal);
        bRef =(Button) findViewById(R.id.bBg);
        ckUp=(CheckBox) findViewById(R.id.upCheck);
        bCenter=(Button) findViewById(R.id.centerPos);
        bInf=(Button) findViewById(R.id.info);
        refPath =null;
        bRef.setText("-none-");
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
        bInf.setEnabled(false);
        bInf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                info();
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
                picking=false;
                dispatch(0);
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
        bRef.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectRef();
            }
        });
        bActions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action();
            }
        });
        bCenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                slctCenter();
            }
        });
        mngCenter();
        if (singleLoc) start3();
        else if (filePath==null) {
            taskChoice();
        } else {
            start2(filePath);
        }
    }

    void info(){
        if (centerName==null || !centerPos.containsKey(centerName)) return;
        String name=centerName.substring(3);
        if (centerName.startsWith("T:")) graph(name,centerPos.get(centerName));
        else if (centerName.startsWith("R:")) infoRoute(name,centerPos.get(centerName));
        else if (centerName.startsWith("P:")) infoPoint(name,centerPos.get(centerName));
    }

    void infoRoute(String name, Location where){
        Intent intent=new Intent(MainActivity.this,Graph.class);
        intent.putExtra("File",filePath);
        intent.putExtra("Track",name);
        intent.putExtra("Route",true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    void infoPoint(String name,Location where){
        Double lat=where.getLatitude();
        Double lon=where.getLongitude();
        String inf=String.format(Locale.ENGLISH,
                    "Latitude   %.6f\n"+
                           "Longitude  %.6f\n",lat,lon);
        if (where.hasAltitude()){
            Double alt=where.getAltitude();
            inf+=String.format(Locale.ENGLISH,
                    "Altitude      %.2f",alt);
        }
        infoMsg("Information about a waypoint",
                "Name "+name+"\n"+inf);
    }

    AlertDialog dialog=null;

    void infoMsg(String title, String Message){
        AlertDialog.Builder build=new AlertDialog.Builder(this);
        View vi=View.inflate(this,R.layout.info,null);
        TextView tinfo=vi.findViewById(R.id.infoTitle);
        TextView tMsg=vi.findViewById(R.id.info);
        Button ok=vi.findViewById(R.id.okInfo);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cOk();
            }
        });
        tinfo.setText(title);
        tMsg.setText(Message);
        build.setView(vi);
        dialog=build.create();
        dialog.show();
    }

    void cOk(){
        if (dialog!=null) dialog.dismiss();
        dialog=null;
    }

    void graph(String trackName, Location where){
        Intent intent=new Intent(MainActivity.this,Graph.class);
        intent.putExtra("File",filePath);
        intent.putExtra("Track",trackName);
        intent.putExtra("Route",false);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    void mngCenter(){
        if (centerPos.isEmpty() || centerName==null){
            bCenter.setText(" - ");
            centerName=null;
            bInf.setEnabled(false);
        } else {
            if (!centerPos.containsKey(centerName)){
                TreeSet<String> s=new TreeSet(centerPos.keySet());
                centerName=s.first();
            }
            bInf.setEnabled(true);
            bCenter.setText(centerName);
        }
        bCenter.setEnabled(!centerPos.isEmpty());
    }

    void slctCenter(){
        final String[] theList;
        if (centerPos.isEmpty()) return;
        theList=centerPos.keySet().toArray(new String[centerPos.size()]);
        Arrays.sort(theList);
        AlertDialog.Builder build=new AlertDialog.Builder(this);
        build.setTitle("Select the focus");
        build.setItems(theList, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                centerName=theList[which];
                mngCenter();
            }
        });
        build.setNegativeButton("None", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                centerName=null;
                mngCenter();
            }
        });
        build.show();
    }

    void action(){
        String prevName=(new File(filePath)).getName();
        String[] actions;
        if (centerName==null) {
            String[] actionsX = {"Add a route",
                    "Add waypoints",
                    "Selective merge of GPX files with " + prevName,
                    " ...and Convert Tracks to Routes"};
            actions=actionsX;
        } else {
            String type;
            String name=centerName.substring(3);
            if (centerName.startsWith("R:")) type="Rename Route ";
            else if (centerName.startsWith("T:")) type="Rename Track ";
            else type="Rename Waypoint ";
            String[] actionsX={type+name,
                    "Add a route",
                    "Add waypoints",
                    "Selective merge of GPX files with " + prevName,
                    " ...and Convert Tracks to Routes"};
            actions=actionsX;
        }
        AlertDialog.Builder build=new AlertDialog.Builder(this);
        build.setTitle("Select the action to perform");
        build.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {

            }
        });
        build.setItems(actions, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent;
                if (centerName!=null) {
                    if (which==0) {
                        entSubst();
                        return;
                    }
                    which--;
                }
                switch (which){
                    case 0:
                        if (colSrc!=colNone){
                            if (!getValCol()){
                                Toast.makeText(context,"Please check the Blue and Red values.",
                                          Toast.LENGTH_LONG).show();
                                return;
                            }
                        }
                        picking=true;
                        asWpt=false;
                        dispatch(0);
                        break;
                    case 1:
                        if (colSrc!=colNone){
                            if (!getValCol()){
                                Toast.makeText(context,"Please check the Blue and Red values.",
                                      Toast.LENGTH_LONG).show();
                                return;
                            }
                        }
                        picking=true;
                        asWpt=true;
                        dispatch(0);
                        break;
                    case 2:
                        intent=new Intent(MainActivity.this,Compose.class);
                        intent.putExtra("pathGPX",filePath);
                        intent.putExtra("Directory",Directory);
                        intent.putExtra("Convert",false);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivityForResult(intent, 5);
                        break;
                    case 3:
                        intent=new Intent(MainActivity.this,Compose.class);
                        intent.putExtra("pathGPX",filePath);
                        intent.putExtra("Directory",Directory);
                        intent.putExtra("Convert",true);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivityForResult(intent, 5);
                        break;
                }
            }
        });
        build.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        build.show();
    }

    void entSubst(){
        String type;
        String name=centerName.substring(3);
        if (centerName.startsWith("R:")) type="Rename Route ";
        else if (centerName.startsWith("T:")) type="Rename Track ";
        else type="Rename Waypoint ";
        type+=name+" to:";
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                noChange();
            }
        });
        View renDiag=View.inflate(this,R.layout.rename,null);
        TextView title=renDiag.findViewById(R.id.reTitle);
        final EditText vName=renDiag.findViewById(R.id.nwName);
        title.setText(type);
        builder.setView(renDiag)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newName=vName.getText().toString();
                        InputMethodManager imm = (InputMethodManager)
                                    context.getSystemService(Activity.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(vName.getWindowToken(),0);
                        if (newName!=null) newName=newName.trim();
                        if (newName==null || newName.isEmpty()) noChange();
                        else doSubst(newName);
                    }
                });
        builder.show();
    }

    void noChange(){
        Toast.makeText(context,"No Change",Toast.LENGTH_LONG).show();
    }

    void doSubst(String newName){
        Intent intent=new Intent(MainActivity.this,Compose.class);
        intent.putExtra("pathGPX",filePath);
        intent.putExtra("Directory",Directory);
        intent.putExtra("Convert",false);
        intent.putExtra("Org",centerName);
        intent.putExtra("Subst",newName);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivityForResult(intent, 5);
    }

    void taskChoice(){
        String[] tasks={"Display a GPX file",
                        "Create waypoints",
                         "Create a route",
                         "Compose: Selective merge of GPX files",
                         "...and Convert Tracks to Routes"};
        AlertDialog.Builder build=new AlertDialog.Builder(this);
        build.setTitle("Task selector")
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                })
                .setItems(tasks, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(MainActivity.this, Compose.class);
                        switch (which){
                            case 0:
                                selGpx();
                                break;
                            case 1:
                                asWpt=true;
                                initPick();
                                break;
                            case 2:
                                asWpt=false;
                                initPick();
                                break;
                            case 3:
                                intent.putExtra("pathGPX",filePath);
                                intent.putExtra("Directory",Directory);
                                intent.putExtra("Convert",false);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivityForResult(intent, 5);
                                break;
                            case 4:
                                intent.putExtra("pathGPX",filePath);
                                intent.putExtra("Directory",Directory);
                                intent.putExtra("Convert",true);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivityForResult(intent, 5);
                                break;
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
        build.show();
    }

    public void start2(String path){
        String note;
        singleLoc=false;
        filePath=path;
        File f=new File(filePath);
        Location first=introTyp();
        centerPos.clear();
        centerName=null;
        mngCenter();
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

    public void start3(){
        filePath=null;
        tType.setText("Waypoint "+centerName);
        tFile.setText("Single waypoint");
        pBar.setProgress(0);
    }

    void launchMap(Location centerLoc){
        stack.clear();
        setStart=true;
        Intent nt=(Intent) intentMap.clone();
        nt.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        nt.putExtra("CALLER",context.getString(R.string.app_name));
        nt.putExtra("CENTER",centerLoc);
        nt.putExtra("Tail",Tail);
        if (zoom!=null) nt.putExtra("ZOOM",zoom);
        zoom=null;
        runningMap=true;
        startActivity(nt);
        waitMap=true;
        registerReceiver(mReceiver,filter);
        return;
    }

    void ckVcMap(int vc){
        if (vc<17){
            Toast.makeText(context,"Msb2Map revision should be at least 1.7",
                    Toast.LENGTH_LONG).show();
        }
    }

///////////////////       DRAWING

    Location introTyp(){
        Track.enttGpx entity;
        if (track!=null) track.close();
        centerPos.clear();
        centerName=null;
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
            valBlue=num.floatValue();
        } catch (ParseException e) {return false;}
        defBlue[colSrc]=field;
        field=etRed.getText().toString();
        if (field!=null) field=field.trim();
        if (field==null || field.isEmpty()){ return false; }
        try {
            num=nfe.parse(field);
            valRed=num.floatValue();
        } catch (ParseException e) {return false; }
        defRed[colSrc]=field;
        if (Math.abs(valBlue-valRed)<0.001f) return false;
        return true;
    }

    Location initTrack() {
        Location firstLoc = null;
        if (track != null) track.close();
        track = new Track();
        size = track.open(filePath);
        nbWpt = 0;
        nbRte = 0;
        nbTrk = 0;
        firstLoc = readTrk();
        if (firstLoc == null) {
            Toast.makeText(context, "No valid item in " + filePath, Toast.LENGTH_LONG).show();
            return null;
        }
        minVal = null;
        maxVal = null;
        totDist = null;
        totDrop=null;
        totGain=null;
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
            if (!inRef) {
                position = track.getPos();
                Float prog = (100.0f * Float.valueOf(position)) / Float.valueOf(size);
                pBar.setProgress(prog.intValue());
                if (startTime != null && startTime > 0L && loc.getTime() != 0L) {
                    Long sec = (loc.getTime() - startTime) / 1000L;
                    Long hour = sec / 3600L;
                    Long min = (sec - hour * 3600L) / 60L;
                    Long s = (sec - hour * 3600L - min * 60L);
                    tTime.setText(String.format("%02d:%02d:%02d", hour, min, s));
                } else tTime.setText("0");
            }
            return loc;
        }
    }

    void eof(){
        if (track!=null) track.close();
        if (inRef && saved!=null){
            track=saved.track;
            dispLoc=saved.dispLoc;
            curEntity=saved.curEntity;
            startLoc=saved.startLoc;
            curEntName=saved.curEntName;
            saved=null;
            inRef =false;
            setStart=true;
            dispatch(5);
        } else {
            running = false;
            Toast.makeText(context, "END OF FILE", Toast.LENGTH_LONG).show();
            track = null;
            if (picking) initPick();
        }
    }

    public Location skip(Location currentLoc){
        Location loc;
        if (toSkip==0l || currentLoc.getTime()==0L) return currentLoc;
        loc=currentLoc;
        Long here=currentLoc.getTime();
        Long target=here+toSkip;
        while (here<target){
            loc=readTrk();
            if (loc==null ||
                    loc.getExtras().getSerializable("ENTITY")!=Track.enttGpx.TRKWPT){
                toSkip=0L;
                return loc;
            } else {
                here=loc.getTime();
            }
        }
        toSkip=0L;
        return loc;
    }

    void setComp(Location initial){
        if (colSrc==colHeight){
            CurComp=new Vheight(initial);
        } else if (colSrc==colMpS){
            CurComp=new Vmps(initial);
        } else if (colSrc==colMpH){
            CurComp=new Vmph(initial);
        } else if (colSrc==colKpH){
            CurComp=new Vspd(initial);
        } else if (colSrc==colSlp){
            CurComp=new Vslp(initial);
        } else if (colSrc==colDist){
            CurComp=new Vdist(initial);
        } else if (colSrc==colVgain){
            CurComp=new Vgain(initial);
        } else if (colSrc==colVdrop){
            CurComp=new Vdrop(initial);
        } else {
            CurComp=null;
        }
    }

    int colorz(Float val){
        if (val==null) return Color.BLACK;
        float norm=(val-valBlue)/(valRed-valBlue);
        int v=(int)Math.round(norm*nColor);
        v=Math.max(1,Math.min(nColor,v))-1;
        return lineColor[v];
    }

    Float hereVal(Location thisLoc){
        if (CurComp==null){
            Double alt=null;
            if (thisLoc.hasAltitude()) {
                alt=thisLoc.getAltitude();
                return alt.floatValue();
            } else return null;
        }
        return CurComp.value(thisLoc);
    }

    void dispatch(int from){
        if (from==0){
            Tail=false;
            nbTrk=0;
            nbRte=0;
            nbWpt=0;
            if (track!=null){
                  track.close();
                  track=null;
            }
            running=true;
        }
        if (singleLoc && !inRef){
            drwSingle();
            return;
        }
        if (!running){
            if (picking) initPick();
            return;
        }
        if (track==null){
            dispLoc=initTrack();
            if (dispLoc==null){
                eof();
                selGpx();
                return;
            }
        }
        while (dispLoc!=null) {
            Track.enttGpx entity = (Track.enttGpx) dispLoc.getExtras().getSerializable("ENTITY");
            if (!inRef && Tail && (entity == Track.enttGpx.TRK || entity == Track.enttGpx.TRKWPT)) {
                if (withTail()) return;
            } else {
                if (noTail()) return;
            }
        }
        eof();
    }


    void drwSingle(){
        if (!runningMap){
            launchMap(centerPos.get(centerName));
            return;
        }
        dispWpt(centerPos.get(centerName),centerName,0);
        eof();
    }

    Boolean noTail() {
        Double alt;
        if (dispLoc == null) return true;
        int nbBroadcast = 0;
        Track.enttGpx entity = (Track.enttGpx) dispLoc.getExtras().getSerializable("ENTITY");
        switch (entity) {
            case WPT:
                curEntity = entity;
            case RTEWPT:
            case TRKWPT:
                if (!runningMap) {
                    if (centerName==null) {
                        launchMap(dispLoc);
                    } else {
                        launchMap(centerPos.get(centerName));
                    }
                    centerPos.clear();
                    return true;
                }
                break;
            case TRK:
                curEntName = dispLoc.getExtras().getString("name", null);
                setStart = true;
                startLoc = null;
                stack.clear();
                setComp(dispLoc);
                minVal = null;
                maxVal = null;
                minAlt = null;
                maxAlt = null;
                curEntity = entity;
                dispLoc = readTrk();
                return false;
            case RTE:
                curEntName = dispLoc.getExtras().getString("name", null);
                setStart = true;
                startLoc = null;
                stack.clear();
                setComp(dispLoc);
                minVal = null;
                maxVal = null;
                minAlt = null;
                maxAlt = null;
                curEntity = entity;
                dispLoc = readTrk();
                return false;
            case ALIEN:
                track.close();
                Toast.makeText(context, "Sorry, " + fileName + " is not compatible.",
                        Toast.LENGTH_LONG).show();
                eof();
                selGpx();
                return true;

        }
        while (true) {
            nbBroadcast++;
            if (nbBroadcast > 100) {
                mHandler.postDelayed(timerTask, 100L);
                return true;
            }
            entity = (Track.enttGpx) dispLoc.getExtras().getSerializable("ENTITY");
            switch (curEntity) {
                case WPT:
                    if (entity != curEntity) return false;
                    nbWpt++;
                    int tp=0;
                    if (inRef) tp=2;
                    dispWpt(dispLoc, String.format(Locale.ENGLISH, "%d waypoints", nbWpt),
                            tp);
                    String wName=dispLoc.getExtras().getString("name");
                    if (wName==null) wName="WPT "+nbWpt.toString();
                    if (!inRef){
                        if (centerName==null){
                            centerName=wName;
                        }
                        centerPos.put("P: "+wName,dispLoc);
                    }
                    break;
                case TRKWPT:
                case RTEWPT:
                    if (entity != curEntity) {
                        return false;
                    }
                    dispTrk(dispLoc, false);
                    break;
                case RTE:
                    if (entity != Track.enttGpx.RTEWPT) return false;
                    if (setStart) {
                        if (startLoc == null) {
                            startLoc = dispLoc;
                            startLoc.getExtras().putString("name", curEntName);
                            if (startLoc.hasAltitude()) {
                                alt=startLoc.getAltitude();
                                minAlt =alt.floatValue();
                                maxAlt = minAlt;
                            }
                        }
                        if (!inRef) startTime = startLoc.getTime();
                        startLine = true;
                        prevAlt = null;
                        prevLoc = null;
                        totDist = null;
                        totDrop=null;
                        totGain=null;
                    }
                    dispTrk(dispLoc, false);
                    if (setStart) {
                        nbRte++;
                        if (curEntName==null) curEntName="RTE "+nbRte.toString();
                        dispWpt(startLoc, String.valueOf(nbRte) + ": " + curEntName, 1);
                        if (!inRef) {
                            if (centerName==null){
                                centerName=curEntName;
                            }
                            centerPos.put("R: "+curEntName,startLoc);
                        }
                        setStart = false;
                    }
                    curEntity = entity;
                    break;
                case TRK:
                    if (entity != Track.enttGpx.TRKWPT) return false;
                    if (setStart) {
                        nbTrk++;
                        if (curEntName==null) curEntName="TRK "+nbTrk.toString();
                        if (startLoc == null) {
                            startLoc = dispLoc;
                            startLoc.getExtras().putString("name", curEntName);
                            if (startLoc.hasAltitude()) {
                                alt=startLoc.getAltitude();
                                minAlt =alt.floatValue();
                                maxAlt = minAlt;
                            }
                        }
                        if (!inRef) startTime = startLoc.getTime();
                        startLine = true;
                        prevAlt = null;
                        prevLoc = null;
                        totDist = null;
                        totDrop=null;
                        totGain=null;
                    }
                    dispTrk(dispLoc, false);
                    if (setStart) {
                        dispWpt(startLoc, String.valueOf(nbTrk) + ": " + curEntName, 1);
                        if (!inRef) {
                            if (centerName==null){
                                centerName=curEntName;
                            }
                            centerPos.put("T: "+curEntName,startLoc);
                        }
                        setStart = false;
                    }
                    curEntity = entity;
                    break;
            }
            if (!runningMap) return true;
            dispLoc = readTrk();
            if (dispLoc == null) {
                return false;
            }
        }
    }

    Boolean withTail(){
        Double alt;
        if (dispLoc==null || !Tail) return false;
        Track.enttGpx entity=(Track.enttGpx)dispLoc.getExtras().getSerializable("ENTITY");
        switch (entity){
            case TRKWPT:
                if (setStart){
                    if (startLoc==null){
                        if (dispLoc.getTime()==0L || !dispLoc.hasAltitude()){
                            Tail=false;
                            return false;
                        }
                        startLoc=dispLoc;
                        startTime=startLoc.getTime();
                        startLoc.getExtras().putString("name",curEntName);
                        if (startLoc.hasAltitude()) {
                                alt=startLoc.getAltitude();
                                minAlt =alt.floatValue();
                                maxAlt = minAlt;
                            }
                    }
                    startLine=true;
                    prevAlt=null;
                    prevLoc=null;
                    totDist=null;
                    totDrop=null;
                    totGain=null;
                }
                if (toSkip>0){
                    dispLoc=skip(dispLoc);
                    if (dispLoc==null) return false;
                    entity=(Track.enttGpx) dispLoc.getExtras().getSerializable("ENTITY");
                    if (entity!= Track.enttGpx.TRKWPT) return false;
                }
                if (!runningMap){
                    launchMap(dispLoc);
                    return true;
                }
                break;
            case TRK:
                curEntName=dispLoc.getExtras().getString("name",null);
                setStart=true;
                startLoc=null;
                stack.clear();
                setComp(dispLoc);
                minVal=null;
                maxVal=null;
                minAlt=null;
                maxAlt=null;
                curEntity=entity;
                arrowOrg=null;
                dispLoc=readTrk();
                return false;
            default:
                return false;
        }
        if (entity!= Track.enttGpx.TRKWPT) return false;
        dispTrk(dispLoc,true);
        if (setStart){
            nbTrk++;
            if (curEntName==null) curEntName="TRK "+nbTrk.toString();
            dispWpt(startLoc,String.valueOf(nbTrk)+": "+ curEntName,1);
            setStart=false;
        }
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

    void dispWpt(Location loc, String infoBubble, int typ) {
        Intent nt = new Intent();
        nt.setAction("org.js.LOC");
        nt.putExtra("WPT", loc);
        String namWpt = loc.getExtras().getString("name", "?");
        nt.putExtra("TYPE", typ);
        if (infoBubble == null) {
            if (loc.hasAltitude()) {
                namWpt = String.format(Locale.ENGLISH, "%s (%.1f)",
                        loc.getExtras().getString("name", "?"), loc.getAltitude());
            }
            nt.putExtra("BUBBLE", namWpt);
        } else {
            nt.putExtra("BUBBLE", infoBubble);
        }
        nt.putExtra("WPT_NAME", namWpt);
        sendBroadcast(nt);
    }

    void dispTrk(Location loc, Boolean actTail){
        Float val=0.0f;
        String label="";
        Integer col=Color.BLACK;
        String bubbleMap=" - ";
        Intent nt=new Intent();
        label=Labels[colSrc];
        if (inRef) col=HalfMagenta;
        else {
            val=hereVal(loc);
            if (val == null) {
                col = Color.BLACK;
                val = 0.0f;
            } else {
                if (colSrc == colNone) {
                    if (prevAlt == null || val > prevAlt) {
                        col = Color.rgb(0xFF, 0x00, 0x00);
                    } else {
                        col = Color.rgb(0x00, 0x00, 0xFF);
                    }
                    prevAlt = val;
                } else {
                    col = colorz(val);
                }
                if (minVal == null) {
                    minVal = val;
                    maxVal = val;
                } else {
                    minVal = Math.min(minVal, val);
                    maxVal = Math.max(maxVal, val);
                }
            }
            if (minVal == null || maxVal == null) {
                bubbleMap = null;
            } else {
                if (actTail) {
                    bubbleMap = String.format(Locale.ENGLISH, "%s %.1f", label, val);
                } else {
                    bubbleMap = String.format(Locale.ENGLISH, "%s %.1f to %.1f", label,
                            minVal, maxVal);
                }
            }
        }
        nt.setAction("org.js.LOC");
        nt.putExtra("LOC",loc);
        nt.putExtra("COLOR",col);
        nt.putExtra("BUBBLE",bubbleMap);
        if (actTail && rotMap){
            if (arrowOrg!=null){
                float dist=arrowOrg.distanceTo(loc);
                if (dist>10.0){
                    float bearing=-arrowOrg.bearingTo(loc);
                    nt.putExtra("ORIENT",bearing);
                    arrowOrg=loc;
                }
            } else arrowOrg=loc;
        }
        if (startLine){
            nt.putExtra("ORIENT",0.0f);
            nt.putExtra("START",startLine);
            nt.putExtra("Tail",actTail);
            startLine=false;
        }
        sendBroadcast(nt);
    }

///////////////////

///////////////////       SETTINGS

    void selGpx(){
        tTime.setText("0");
        pBar.setProgress(0);
        if (track!=null) track.close();
        nbWpt =0;
        nbRte=0;
        nbTrk=0;
        running=false;
        track=null;
        centerPos.clear();
        centerName=null;
        mngCenter();
        Intent intent = new Intent(MainActivity.this, Selector.class);
        intent.putExtra("CurrentDir", Directory);
        intent.putExtra("WithDir", false);
        intent.putExtra("Mask", "(?i).+\\.gpx");
        intent.putExtra("Title","Read from ");
        if (filePath!=null) intent.putExtra("Previous",filePath);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivityForResult(intent, 2);
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

    public void getSpeed(){
        int id=rSpeed.getCheckedRadioButtonId();
        switch (id){
            case R.id.sp1:
                divisor=1l;
                rotMap=ckUp.isChecked();
                break;
            case R.id.sp2:
                divisor=2l;
                rotMap=ckUp.isChecked();
                break;
            case R.id.sp10:
                divisor=10l;
                rotMap=ckUp.isChecked();
                break;
        }
    }

///////////////////

///////////////////       RETURNS

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
            int vc=intent.getIntExtra("VERSION",0);
            unregisterReceiver(mReceiver);
            waitMap=false;
            ckVcMap(vc);
            if (refPath ==null) {
                dispatch(1);
            } else {
                setRefG();
            }
        }
    };

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
            mngCenter();
//            Toast.makeText(context, "Return from map", Toast.LENGTH_LONG).show();
            if (picking){
                unregisterReceiver(pReceiver);
                prcsPick();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 2:       // selector
                if (resultCode == RESULT_OK) {
                    filePath = data.getStringExtra("Path");
                    if (filePath == null || filePath.isEmpty()) finish();
                    else {
                        start2(filePath);
                    }
                } else finish();
                break;
            case 3:     // gpx gen
                if (resultCode == RESULT_OK) {
                    String fGpx = data.getStringExtra("Path");
                    gpx.ckOver(fGpx);
                } else taskChoice();
                break;
            case 4:      // select reference
                if (resultCode == RESULT_OK) {
                    refPath = data.getStringExtra("Path");
                    if (refPath == null || refPath.isEmpty()) refPath = null;
                } else refPath = null;
                if (refPath ==null) bRef.setText("-none-");
                else {
                    File f=new File(refPath);
                    refDirectory =f.getParent();
                    String bGname = (f.getName());
                    bRef.setText(bGname);
                }
                break;
            case 5:        // compose
                if (resultCode==RESULT_OK){
                    filePath=data.getStringExtra("Path");
                    if (filePath == null || filePath.isEmpty()) finish();
                    else {
                        start2(filePath);
                    }
                } else finish();
                break;
        }
    }


///////////////////

///////////////////       PICKING

    gpxGen gpx;

    void nkdPick(){
        AlertDialog.Builder build=new AlertDialog.Builder(this);
        build.setMessage("Preparation of new route/waypoints ?")
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                })
                .setTitle("New GPX file")
                .setNeutralButton("NO", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setPositiveButton("Waypoints", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        asWpt=true;
                        initPick();
                    }
                })
                .setNegativeButton("Route", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        asWpt=false;
                        initPick();
                    }
                });
        build.show();
    }

    void initPick(){
        picking=true;
        picked.clear();
        if (!runningMap){
            launchMap(null);
            return;
        }
        Intent nt = new Intent();
        nt.setAction("org.js.LOC");
        nt.putExtra("PICKING",picking);
        nt.putExtra("PICKWPT",asWpt);
        registerReceiver(pReceiver,filterPick);
        sendBroadcast(nt);
    }

    IntentFilter filterPick=new IntentFilter("org.js.PICKED");
    private final BroadcastReceiver pReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Integer index=intent.getIntExtra("INDEX",0);
            String name=intent.getStringExtra("NAME");
            Location loc=(Location)intent.getParcelableExtra("LOC");
            if (loc==null) picked.remove(index);
            else {
                NamedLoc nl=new NamedLoc();
                nl.name=name;
                nl.loc=loc;
                picked.put(index,nl);
            }
        }
    };

    void prcsPick(){
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        Calendar now=Calendar.getInstance();
        pkdRteName=null;
        final String defName=sdf.format(now.getTime());
        Integer nbLoc=picked.size();
        if (nbLoc<1){
            Toast.makeText(context,"No registered location!",Toast.LENGTH_LONG).show();
            picking=false;
            return;
        }
        if (asWpt){
            Toast.makeText(context,nbLoc.toString()+" registered waypoints",
                    Toast.LENGTH_LONG).show();
            recordWR();
        } else {
            if (nbLoc<2){
                Toast.makeText(context,"At least 2 points for a route!",
                        Toast.LENGTH_LONG).show();
                picking=false;
                return;
            }
            Double totLn=0.0;
            SortedSet<Integer> keys=new TreeSet<>();
            keys.addAll(picked.keySet());
            Iterator<Integer> itr=keys.iterator();
            NamedLoc namedLoc=null;
            Integer indx=null;
            Location loc=null;
            indx=itr.next();
            namedLoc=picked.get(indx);
            Location prevLoc=namedLoc.loc;
            while (itr.hasNext()){
                indx=itr.next();
                namedLoc=picked.get(indx);
                loc=namedLoc.loc;
                totLn+=haver.lHaversine(prevLoc,loc);
                prevLoc=loc;
            }
            String km=String.format(Locale.ENGLISH,"length: %.1f km",totLn);
            AlertDialog.Builder builder=new AlertDialog.Builder(this);
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    finish();
                }
            });
            View rteDiag=View.inflate(this,R.layout.route,null);
            TextView vRteNb=rteDiag.findViewById(R.id.rteNb);
            final EditText vRteName=rteDiag.findViewById(R.id.rteName);
            vRteNb.setText("Set a significative route name like \"GR5 Day1\"");
            vRteName.setHint(defName);
            builder.setView(rteDiag)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            pkdRteName=vRteName.getText().toString();
                            if (pkdRteName!=null) pkdRteName=pkdRteName.trim();
                            if (pkdRteName==null || pkdRteName.isEmpty()) pkdRteName=defName;
                            InputMethodManager imm = (InputMethodManager)
                                    context.getSystemService(Activity.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(vRteName.getWindowToken(),0);
                            picking=false;
                            recordWR();
                        }
                    })
                    .setTitle("Route: "+nbLoc+" points ("+km+")");
            builder.show();
        }
        picking=false;
    }

    void recordWR(){
        gpx=new gpxGen();
        gpx.outChoice(mAct,Directory,filePath,asWpt,pkdRteName,picked);
    }

    void recordWR0(){
        if (filePath==null) taskChoice();
    }

///////////////////

///////////////////      REFERENCE GPX

        class SaveGpx {
        Track track=null;
        Location dispLoc=null;
        Location startLoc=null;
        Track.enttGpx curEntity=null;
        String curEntName=null;
    }
    SaveGpx saved=null;

    void setRefG() {
        Location firstLoc = null;
        Long sizeRef = null;
        saved = new SaveGpx();
        saved.track = track;
        saved.dispLoc = dispLoc;
        saved.curEntity = curEntity;
        saved.startLoc = startLoc;
        saved.curEntName = curEntName;
        inRef=true;
        track = new Track();
        sizeRef = track.open(refPath);
        firstLoc = readTrk();
        if (firstLoc == null) {
            Toast.makeText(context, "No valid item in " + refPath, Toast.LENGTH_LONG).show();
            track.close();
            track = saved.track;
            dispLoc = saved.dispLoc;
            curEntity = saved.curEntity;
            startLoc = saved.startLoc;
            curEntName = saved.curEntName;
            saved = null;
            inRef = false;
            setStart = true;
            dispatch(5);
        } else {
            dispLoc = firstLoc;
            dispatch(6);
        }
    }

    void selectRef(){
        Intent intent=new Intent(MainActivity.this, Selector.class);
        if (refDirectory==null) refDirectory=Directory;
        if (refDirectory !=null) intent.putExtra("CurrentDir", refDirectory);
        intent.putExtra("WithDir",false);
        intent.putExtra("Mask","(?i).+\\.gpx");
        intent.putExtra("Title","Reference GPX?      ");
        if (refPath !=null) intent.putExtra("Previous", refPath);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivityForResult(intent,4);
    }


///////////////////

}
