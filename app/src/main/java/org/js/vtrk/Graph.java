package org.js.vtrk;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.fill;

public class Graph extends AppCompatActivity {

    Context context;
    String filePath;
    String trackName;
    Boolean route=false;
    CombinedChart chart=null;
    Button bLY;
    Button bRY;
    Button bX;
    Button bStop;
    int colors[]={Color.BLACK,Color.BLUE,Color.CYAN,Color.MAGENTA,
                                  Color.GREEN,0XFFC05800, Color.RED};
    String[] head={ " Alt. (m) ",
                    " Height (m) ",
                    " Tr. distance (km) ",
                    " slope (%) ",
                    " climb rate (m/h) ",
                    " speed (km/h) ",
                    " V. gain (m) ",
                    " V. drop (m) "};
    compute[] comp=new compute[head.length];
    boolean[] bHeadL=new boolean[head.length];
    boolean[] bHeadR=new boolean[head.length];
    Boolean XDist=true;
    Long size;
    Long startTime=null;
    class Fix {
        Location loc;
        Float since;
    }
    Map<Integer,Fix> all=new HashMap<>();
    Integer nbLoc=null;
    FileStub fileStub=null;
    InfoRun infoR=null;
    AlertDialog dialog=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);
        context=getApplicationContext();
        Intent intent=getIntent();
        filePath=intent.getStringExtra("File");
        trackName=intent.getStringExtra("Track");
        route=intent.getBooleanExtra("Route",false);
        bHeadL[0]=true;
        bHeadR[3]=true;
        fetchPref();
    }

    void fetchPref() {
        SharedPreferences pref = context.getSharedPreferences(
                context.getString(R.string.PrefName), 0);
        XDist = pref.getBoolean("XDIST", XDist);
        for (Integer i = 0; i < head.length; i++) {
            bHeadL[i] = pref.getBoolean("BHL" + i.toString(), bHeadL[i]);
            bHeadR[i] = pref.getBoolean("BHR" + i.toString(), bHeadR[i]);
        }
    }

    void putPref() {
        SharedPreferences pref = context.getSharedPreferences(
                context.getString(R.string.PrefName), 0);
        SharedPreferences.Editor edit = pref.edit();
        edit.putBoolean("XDIST", XDist);
        for (Integer i = 0; i < head.length; i++) {
            edit.putBoolean("BHL" + i.toString(), bHeadL[i]);
            edit.putBoolean("BHR" + i.toString(), bHeadR[i]);
        }
        edit.apply();
    }

    @Override
    protected void onStart(){
        super.onStart();
        if (chart!=null) return;
        chart=(CombinedChart) findViewById(R.id.chart);
        bLY=(Button) findViewById(R.id.button1);
        bRY=(Button) findViewById(R.id.button3);
        bX=(Button) findViewById(R.id.button2);
        bStop=(Button) findViewById(R.id.button4);
        bX.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectX();
            }
        });
        bRY.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectY(true);
            }
        });
        bLY.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectY(false);
            }
        });
        bStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        fileStub=new FileStub();
        fileStub.getAll(filePath,trackName);
    }

    void selectX(){
        AlertDialog.Builder build=new AlertDialog.Builder(this);
        build.setTitle("X axis");
        build.setMessage("Source of X axis");
        build.setPositiveButton("Travel time", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                XDist=false;
                mkChart();
            }
        });
        build.setNegativeButton("Travel distance", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                XDist=true;
                mkChart();
            }
        });
        build.show();
    }

    void selectY(final Boolean right){
        AlertDialog.Builder build=new AlertDialog.Builder(this);
        if (right) {
            build.setTitle("Select values for right Y axis");
            build.setMultiChoiceItems(head, bHeadR, new DialogInterface.OnMultiChoiceClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                    bHeadR[which]=isChecked;
                }
            });
        } else {
            build.setTitle("Select values for left Y axis");
            build.setMultiChoiceItems(head, bHeadL, new DialogInterface.OnMultiChoiceClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                    bHeadL[which]=isChecked;
                }
            });
        }
        build.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mkChart();
            }
        });
        build.setNegativeButton("None", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (right) fill(bHeadR,0,bHeadR.length-1,false);
                else fill(bHeadL,0,bHeadL.length-1,false);
                mkChart();
            }
        });
        build.show();
    }

    private static class MyHandler extends Handler {
        public final WeakReference<Graph> mActivity;
        public MyHandler(Graph activity){
            mActivity=new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg){
            int code=msg.what;
            if (code==fileProcess.msgTyp.PROG.ordinal()){
                mActivity.get().fileStub.setProg(msg);
            } else if (code==fileProcess.msgTyp.SZ.ordinal()){
                mActivity.get().fileStub.setSz(msg);
            } else if (code==fileProcess.msgTyp.LOC.ordinal()){
                mActivity.get().fileStub.addLoc(msg);
            } else if (code==fileProcess.msgTyp.MISTRK.ordinal()){
                mActivity.get().fileStub.misTrk(msg);
            } else if (code==fileProcess.msgTyp.ENDLOC.ordinal()){
                mActivity.get().fileStub.endLoc(msg);
            } else if (code==fileProcess.msgTyp.ENDLOCR.ordinal()){
                mActivity.get().fileStub.endLocR(msg);
            }
        }

    }

    public class FileStub{

        String Path=null;
        ProgressBar pBar;
        AlertDialog dialog;

        void getAll(final String path, final String nameOfTrack){
            Path=path;
            final MyHandler mHandler=new MyHandler(Graph.this);
            new  Thread(new Runnable() {
                @Override
                public void run() {
                    Looper.prepare();
                    fileProcess fp=new fileProcess();
                    fp.process(mHandler, fileProcess.Request.READ,Path,nameOfTrack,null,
                            null,route,null);
                    Looper.loop();
                }
            }).start();

        }

        void setSz(Message msg){
            size=(Long)msg.obj;
            if (size<1){
                Toast.makeText(context,"No valid item in " + Path,
                        Toast.LENGTH_LONG).show();
                finish();
            }
            all.clear();
            nbLoc=0;
            startTime=null;
            infoR=new InfoRun();
            AlertDialog.Builder build=new AlertDialog.Builder(Graph.this);
            View barView=View.inflate(context,R.layout.progress,null);
            TextView tOper=barView.findViewById(R.id.operation);
            pBar=barView.findViewById(R.id.progressBar);
            if (route){
                tOper.setText("Reading route");
            } else {
                tOper.setText("Reading track");
            }
            pBar.setMax(100);
            pBar.setProgress(0);
            build.setView(barView);
            dialog=build.create();
            dialog.show();
        }

        void setProg(Message msg){
            Long pos=(Long)msg.obj;
            Long perc=(100*pos/size);
            pBar.setProgress(perc.intValue());
        }

        void misTrk(Message msg){
            Toast.makeText(context,"Track missing data",Toast.LENGTH_LONG).show();
            finish();
        }

        void addLoc(Message msg){
            nbLoc=msg.arg1;
            Location somewhere=(Location)msg.obj;
            Long thisTime=somewhere.getTime();
            if (startTime==null) startTime=thisTime;
            Long difTime=thisTime-startTime;
            Float elapsed=difTime.floatValue()/60000f;
            Fix fix=new Fix();
            fix.loc=somewhere;
            fix.since=elapsed;
            all.put(nbLoc,fix);
            infoR.nbPt++;
            if (infoR.startPos==null){
                infoR.startPos=somewhere;
                infoR.prevPos=somewhere;
                return;
            }
            Float dist=infoR.prevPos.distanceTo(somewhere)/1000f;
            infoR.distance+=dist;
            infoR.prevPos=somewhere;
            infoR.endPos=somewhere;
            dist=infoR.startPos.distanceTo(somewhere)/1000f;
            if (dist>infoR.farDist){
                infoR.farDist=dist;
                infoR.farPos=somewhere;
            }
        }

        void endLoc(Message msg){
            dialog.dismiss();
            infoTrack(false);
            mkChart();

        }

        void endLocR(Message msg){
            dialog.dismiss();
            infoTrack(true);
        }
    }

    void infoTrack(Boolean route){
        if (infoR==null) return;
        AlertDialog.Builder build=new AlertDialog.Builder(this);
        View vi=View.inflate(this,R.layout.info,null);
        TextView title=vi.findViewById(R.id.infoTitle);
        TextView ti=vi.findViewById(R.id.info);
        Button ok=vi.findViewById(R.id.okInfo);
        if (route){
            ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cOkR();
                }
            });
            title.setText("Information about route " + trackName);
        } else {
            ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cOk();
                }
            });
            title.setText("Information about track " + trackName);
        }
        ti.setText(infoR.print());
        build.setView(vi);
        dialog=build.create();
        dialog.show();
    }

    void cOk(){
        if (dialog!=null){
            dialog.dismiss();
            dialog=null;
        }
    }

    void cOkR(){
        if (dialog!=null){
            dialog.dismiss();
            dialog=null;
        }
        finish();
    }

    void mkChart(){
        if (nbLoc==0){
            Toast.makeText(context,"No data",Toast.LENGTH_LONG).show();
            finish();
        }
        Float[] values=new Float[head.length];
        Location somewhere;
        int nbLeft=0;
        int nbRight=0;
        Valt valt=null;
        Vheight vheight=null;
        CombinedData combData=new CombinedData();
        ArrayList<Entry>[] entryL=new ArrayList[head.length];
        ArrayList<Entry>[] entryR=new ArrayList[head.length];
        Description des=new Description();
        XAxis xAxis = chart.getXAxis();
        YAxis ylAxis=chart.getAxisLeft();
        YAxis yrAxis=chart.getAxisRight();
        xAxis.setLabelRotationAngle(-45f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        comp=new compute[head.length];
        comp=new compute[head.length];
        Float X;
        Float H=0.0f;
        String versus;
        if (XDist) versus="/dist. (km)";
        else versus="/time (min)";
        for (int i=0;i<nbLoc;i++){
            somewhere=all.get(i).loc;
            if (i==0){
                if (bHeadR[0] || bHeadL[0]){
                    comp[0]=new Valt(somewhere);
                    entryL[0]=new ArrayList<>();
                    entryR[0]=new ArrayList<>();
                }
                if (bHeadR[1] || bHeadL[1]){
                    comp[1]=new Vheight(somewhere);
                    entryL[1]=new ArrayList<>();
                    entryR[1]=new ArrayList<>();
                }
                if (bHeadR[2] || bHeadL[2] || XDist){
                    comp[2]=new Vdist(somewhere);
                    entryL[2]=new ArrayList<>();
                    entryR[2]=new ArrayList<>();
                }
                if (bHeadR[3] || bHeadL[3]){
                    comp[3]=new Vslp(somewhere);
                    entryL[3]=new ArrayList<>();
                    entryR[3]=new ArrayList<>();
                }
                if (bHeadR[4] || bHeadL[4]){
                    comp[4]=new Vmph(somewhere);
                    entryL[4]=new ArrayList<>();
                    entryR[4]=new ArrayList<>();
                }
                if (bHeadR[5] || bHeadL[5]){
                    comp[5]=new Vspd(somewhere);
                    entryL[5]=new ArrayList<>();
                    entryR[5]=new ArrayList<>();
                }
                if (bHeadR[6] || bHeadL[6]){
                    comp[6]=new Vgain(somewhere);
                    entryL[6]=new ArrayList<>();
                    entryR[6]=new ArrayList<>();
                }
                if (bHeadR[7] || bHeadL[7]){
                    comp[7]=new Vdrop(somewhere);
                    entryL[7]=new ArrayList<>();
                    entryR[7]=new ArrayList<>();
                }
            }
            for (int j=0;j<head.length;j++){
                if (comp[j]!=null) values[j]=comp[j].value(somewhere);
            }
            if (XDist) X=values[2];
            else  X=all.get(i).since;
            for (int j=0;j<head.length;j++){
                if (bHeadL[j]){
                    entryL[j].add(new Entry(X,values[j]));
                    nbLeft++;
                }
            }
            for (int j=0;j<head.length;j++){
                if (bHeadR[j]){
                    entryR[j].add(new Entry(X,values[j]));
                    nbRight++;
                }
            }
        }
        ArrayList<ILineDataSet> iLineDataSets=new ArrayList<>();
        if (nbLeft>0){
            ylAxis.setEnabled(true);
            LineDataSet[] dataSetL=new LineDataSet[head.length];
            int j=0;
            for (int i=0;i<head.length;i++){
                if (bHeadL[i]){
                    dataSetL[i]=new LineDataSet(entryL[i],head[i]+versus);
                    dataSetL[i].setDrawCircles(false);
//                    dataSetL[i].setLineWidth(0.5f);
                    dataSetL[i].setAxisDependency(YAxis.AxisDependency.LEFT);
                    dataSetL[i].setColor(colors[j%colors.length]);
                    j++;
                    iLineDataSets.add(dataSetL[i]);
                }
            }
            ylAxis.setGridColor(colors[0]);
            ylAxis.setAxisLineColor(colors[0]);
            ylAxis.setTextColor(colors[0]);
        } else ylAxis.setEnabled(false);
        if (nbRight>0){
            yrAxis.setEnabled(true);
            LineDataSet[] dataSetR=new LineDataSet[head.length];
            int j=0;
            for (int i=0;i<head.length;i++){
                if (bHeadR[i]){
                    dataSetR[i]=new LineDataSet(entryR[i],head[i]+versus);
                    dataSetR[i].setDrawCircles(false);
//                    dataSetR[i].setLineWidth(0.5f);
                    dataSetR[i].setAxisDependency(YAxis.AxisDependency.RIGHT);
                    dataSetR[i].setColor(colors[(colors.length-j-1)%colors.length]);
                    j++;
                    iLineDataSets.add(dataSetR[i]);
                }
            }
            yrAxis.setGridColor(colors[colors.length-1]);
            yrAxis.setAxisLineColor(colors[colors.length-1]);
            yrAxis.setTextColor(colors[colors.length-1]);
        } else yrAxis.setEnabled(false);
        LineData lineData=new LineData(iLineDataSets);
        combData.setData(lineData);
        chart.setData(combData);
        des.setText(trackName);
        chart.setDescription(des);
        chart.invalidate();
        putPref();
    }
}
