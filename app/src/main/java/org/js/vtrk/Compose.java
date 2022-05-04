package org.js.vtrk;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Locale;

public class Compose extends AppCompatActivity {

    String orgnGpx=null;
    String curDir=null;
    String outGpx=null;
    Context context;
    FileWriter tmpGpx=null;
    File tmpFi;
    String tmpPath;
    TextView tv;
    ProgressBar progress;
    int nbWpt=0;
    int nbTrk=0;
    int nbRte=0;
    Boolean lastSave=true;
    public Nume nume=null;
    Boolean mkRte=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ArrayList<Location> itemsList=new ArrayList<>();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose);
        progress=(ProgressBar)findViewById(R.id.progressBar);
        context=getApplicationContext();
        tv=(TextView) findViewById(R.id.info);
        Intent intent=getIntent();
        curDir=intent.getStringExtra("Directory");
        orgnGpx=intent.getStringExtra("pathGPX");
        mkRte=intent.getBooleanExtra("Convert",false);
        String subst=intent.getStringExtra("Subst");
        String org=intent.getStringExtra("Org");
        if (subst!=null && org!=null) renamer(orgnGpx,org,subst);
        else if (orgnGpx==null) other(curDir);
        else searchItems(orgnGpx);
    }

    void other(String directory){
        Intent intent=new Intent(Compose.this, Selector.class);
        if (directory !=null) intent.putExtra("CurrentDir", directory);
        intent.putExtra("WithDir",false);
        intent.putExtra("Mask","(?i).+\\.gpx");
        intent.putExtra("Title","Other GPX to merge?      ");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivityForResult(intent,23);
    }

    void searchItems(String path){
        if (nume==null) nume=new Nume();
        nume.Nume(path);
    }

    void showItems(final String path, final ArrayList<Location>itemsList){
        File f=new File(path);
        String fname=f.getName();
        final String directory=f.getParent();
        if (itemsList==null || itemsList.isEmpty()) {
            Toast.makeText(this,"No item found",Toast.LENGTH_LONG).show();
            other(directory);
            return;
        }
        final CharSequence theList[]=new CharSequence[itemsList.size()];
        final boolean[] setOptions=new boolean[itemsList.size()];
        String pre=null;
        for (int i=0;i<itemsList.size();i++){
            Track.enttGpx entity=(Track.enttGpx)
                    itemsList.get(i).getExtras().getSerializable("ENTITY");
            switch (entity){
                case WPT:
                    pre=" WPT: ";
                    break;
                case TRK:
                    if (mkRte) pre="!TRK: ";
                    else pre=" TRK: ";
                    break;
                case RTE:
                    pre=" RTE: ";
                    break;
            }
            String name=itemsList.get(i).getExtras().getString("name",null);
            theList[i]=pre+name;
            setOptions[i]=true;
        }
        final DialogInterface.OnMultiChoiceClickListener onclick=
                new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                setOptions[which]=isChecked;
            }
        };
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setTitle("Items to keep from "+fname);
        builder.setMultiChoiceItems(theList,setOptions,onclick);
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                conclusion(orgnGpx);
            }
        })
                .setPositiveButton("Proceed", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        lastSave=true;
                        saver(path,itemsList,setOptions);
                    }
                });
        builder.show();
    }

    void moreQ(){
        String total=String.format(Locale.ENGLISH,
                    "Total: Route:%d Track:%d Waypoint:%d", nbRte,nbTrk,nbWpt);
        AlertDialog.Builder build=new AlertDialog.Builder(this);
        build.setTitle("More GPX files to merge?");
        build.setMessage(total);
        build.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                lastSave=false;
                other(curDir);
            }
        })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        closeTemp();
                        selectOut();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        conclusion(orgnGpx);
                    }
                });
        build.show();
    }

    private static class MyHandler extends Handler {

        public final WeakReference<Compose> mActivity;

        public MyHandler(Compose activity){
            mActivity=new WeakReference<Compose>(activity);
        }

        @Override
        public void handleMessage(Message msg){
            int code=msg.what;
            if (code==fileProcess.msgTyp.SZ.ordinal()){
                mActivity.get().nume.setSz(msg);
            } else if (code==fileProcess.msgTyp.LOC.ordinal()){
                mActivity.get().nume.setLoc(msg);
            } else if (code==fileProcess.msgTyp.PROG.ordinal()){
                mActivity.get().nume.setProg(msg);
            } else if (code==fileProcess.msgTyp.END.ordinal()) {
                mActivity.get().nume.setEnd();
            } else if (code==fileProcess.msgTyp.ENDS.ordinal()) {
                mActivity.get().nume.setEndSave(msg);
            } else if (code==fileProcess.msgTyp.ENDREN.ordinal()){
                mActivity.get().nume.setEndRen(msg);
            } else if (code==fileProcess.msgTyp.ERROR.ordinal()){
                mActivity.get().nume.error(msg);
            }
        }
    }



    public class Nume {

        Long size=null;
        ArrayList<Location> list=new ArrayList<>();
        String Path;
        String fName;


        void Nume(final String path){
            Path=path;
            File f=new File(Path);
            curDir=f.getParent();
            list=new ArrayList<>();
            fName=f.getName();
            final MyHandler mHandler=new MyHandler(Compose.this);
            new Thread(new Runnable(){
                @Override
                public void run(){
                    Looper.prepare();
                    fileProcess fp=new fileProcess();
                    fp.process(mHandler,fileProcess.Request.ENUM,path,null,
                            null,null, mkRte,null);
                    Looper.loop();
                }
            }).start();
        }

        void Nume(final String path, final FileWriter outGpx,
                  final ArrayList<Location> itemsList, final boolean[] setOptions,
                  final String orgName, final String subst){
            Path=path;
            File f=new File(Path);
            curDir=f.getParent();
            list=new ArrayList<>();
            fName=f.getName();
            list=itemsList;
            final MyHandler mHandler=new MyHandler(Compose.this);
            new Thread(new Runnable(){
                @Override
                public void run(){
                    Looper.prepare();
                    fileProcess fp=new fileProcess();
                    fileProcess.Request thisReq;
                    if (orgName!=null && subst!=null) thisReq=fileProcess.Request.RENAME;
                    else thisReq=fileProcess.Request.SAVE;
                    fp.process(mHandler,thisReq,path,subst,outGpx,
                            setOptions,mkRte,orgName);
                    Looper.loop();
                }
            }).start();
        }

        void setSz(Message msg){
            size=(Long)msg.obj;
            tv.setText(fName);
            progress.setProgress(0);
        }

        void  setLoc(Message msg){
            Location loc=(Location)msg.obj;
            if (loc!=null) list.add(loc);
        }

        void setProg(Message msg){
            Long pos=(Long)msg.obj;
            Long perc=(100*pos/size);
            progress.setProgress(perc.intValue());
        }

        void setEnd(){
            showItems(Path,list);
        }

        void setEndSave(Message msg){
            int n1=msg.arg1;
            int n2=msg.arg2;
            Integer n3=(Integer) msg.obj;
            nbRte+=n1;
            nbTrk+=n2;
            nbWpt+=n3;
            if (nbWpt+nbTrk+nbRte<1) other(curDir);
            else moreQ();
        }

        void setEndRen(Message msg){
            nbRte=msg.arg1;
            nbTrk=msg.arg2;
            nbWpt=(Integer)msg.obj;
            closeTemp();
            selectOut();
        }

        void error(Message msg){
            String ermes=(String)msg.obj;
            errorShow(ermes);
        }

    }

    void renamer(String path,String org, String subst){
        if (tmpGpx==null) openTemp();
        if (nume==null) nume=new Nume();
        nume.Nume(path,tmpGpx,null,null,org,subst);
    }

    void saver(String path,ArrayList<Location> itemsList, boolean[] setOptions){
        if (tmpGpx==null) openTemp();
        if (nume==null) nume=new Nume();
        nume.Nume(path,tmpGpx,itemsList,setOptions,null,null);
    }

    void openTemp(){
        try {
            tmpFi=File.createTempFile("xxx", ".gpx");
            tmpPath=tmpFi.getPath();
            tmpGpx=new FileWriter(tmpFi);
            tmpFi.deleteOnExit();
            tmpGpx.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            tmpGpx.write("<gpx version=\"1.0\"\n");
            tmpGpx.write("   xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
            tmpGpx.write("   creator=\"Msb2Kml\"\n");
            tmpGpx.write("   xmlns=\"http://www.topografix.com/GPX/1/0\"\n");
            tmpGpx.write("   xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0");
            tmpGpx.write("http://www.topografix.com/GPX/1/0/gpx.xsd\">\n");
        } catch (IOException e){
                errorShow(e.getMessage());
        }
    }

    void closeTemp(){
        if (tmpGpx!=null){
            try {
                tmpGpx.write("</gpx>\n");
                tmpGpx.close();
                tmpGpx=null;
            } catch (IOException e) {
                errorShow(e.getMessage());
            }
        }
    }


    void selectOut(){
        Intent intent=new Intent(context,Selector.class);
        intent.putExtra("CurrentDir", curDir);
        intent.putExtra("WithDir", false);
        intent.putExtra("Mask", "(?i).+\\.gpx");
        if (orgnGpx!=null) intent.putExtra("Previous",orgnGpx);
        intent.putExtra("Creable",true);
        intent.putExtra("Suffix",".gpx");
        intent.putExtra("Title","Save as existing or new file in ");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivityForResult(intent,24);
    }

    void ckOver(){
        if (outGpx==null){
            conclusion(orgnGpx);
            return;
        }
        File f=new File(outGpx);
        if (f.exists()){
            AlertDialog.Builder build=new AlertDialog.Builder(this);
            build.setTitle("Existing file");
            build.setMessage(outGpx+" exists");
            build.setPositiveButton("Overwrite?", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    copy();
                }
            })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            selectOut();
                        }
                    })
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            conclusion(orgnGpx);
                        }
                    });
            build.show();
        } else copy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        String filePath = null;
        switch (requestCode) {
            case 23:
                if (resultCode == RESULT_OK) {
                    filePath = data.getStringExtra("Path");
                    if (filePath == null || filePath.isEmpty()) conclusion(orgnGpx);
                    else{
                        if (orgnGpx==null) orgnGpx=filePath;
                        searchItems(filePath);
                    }
                } else conclusion(orgnGpx);
                break;
            case 24:
                if (resultCode==RESULT_OK) {
                    outGpx=data.getStringExtra("Path");
                    ckOver();
                } else conclusion(orgnGpx);
                break;
        }
    }

    void copy(){
        Toast.makeText(context,outGpx,Toast.LENGTH_LONG).show();
        File ft=new File(tmpPath);
        File fo=new File(outGpx);
        FileChannel inputC;
        FileChannel outputC;
        try {
            FileInputStream fit=new FileInputStream(ft);
            FileOutputStream foo=new FileOutputStream(outGpx);
            outputC=foo.getChannel();
            inputC=fit.getChannel();
            inputC.transferTo(0,inputC.size(),outputC);
            inputC.close();
            outputC.close();
            ft.delete();
        } catch (Exception e){
            errorShow(e.getMessage());
            conclusion(orgnGpx);
        }
        conclusion(outGpx);
    }



    void conclusion (String path){
        Intent result=new Intent();
        result.putExtra("Path",path);
        setResult(RESULT_OK,result);
        finish();
    }

    void errorShow(String message){
        AlertDialog.Builder build=new AlertDialog.Builder(this);
        build.setTitle("Error");
        build.setMessage(message);
        build.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                conclusion(orgnGpx);
            }
        });
        build.show();
    }
}
