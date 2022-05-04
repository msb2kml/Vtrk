package org.js.vtrk;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.webkit.WebHistoryItem;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class gpxGen {

    FileWriter outGpx=null;
    String curDir=null;
    String fPath=null;
    String srcPath=null;
    Boolean asWpt=false;
    String rteName=null;
    Integer rtePtNb=0;
    Map<Integer,MainActivity.NamedLoc>picked= new HashMap();
    Boolean overwrite=false;
    WeakReference<MainActivity> mActivity;
    static File temp;
    String tempPath=null;
    String copyLine=null;
    String comment;

    void outChoice(final WeakReference<MainActivity> mActivity, String dir,String orgFile,
                     Boolean asw,String rten, Map<Integer,MainActivity.NamedLoc> pkd){
        this.mActivity=mActivity;
        asWpt=asw;
        rteName=rten;
        picked=pkd;
        curDir=dir;
        srcPath=orgFile;
        fPath=orgFile;
        launchSelect();
    }

    void launchSelect(){
        Intent intent=new Intent(mActivity.get().context,Selector.class);
        intent.putExtra("CurrentDir", curDir);
        intent.putExtra("WithDir", false);
        intent.putExtra("Mask", "(?i).+\\.gpx");
        if (srcPath!=null) intent.putExtra("Previous",srcPath);
        intent.putExtra("Creable",true);
        intent.putExtra("Suffix",".gpx");
        intent.putExtra("Title","Save as existing or new file in ");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mActivity.get().startActivityForResult(intent, 3);
    }

    void ckOver(String file){
        if (file==null) {
            mActivity.get().recordWR0();
            return;
        }
        fPath=file;
        File f=new File(fPath);
        if (f.exists()){
            AlertDialog.Builder build=new AlertDialog.Builder(mActivity.get());
            build.setTitle("Existing file");
            build.setMessage(fPath+" exists");
            build.setPositiveButton("Overwrite?", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    overwrite=true;
                    begin();
                }
            })
                .setNegativeButton("Append?", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            overwrite=false;
                            begin();
                        }
            })
                 .setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            fPath=null;
                            mActivity.get().recordWR0();
                        }
            })
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            fPath=null;
                            mActivity.get().recordWR0();
                        }
            });
            build.show();
        } else {
            overwrite=true;
            begin();
        }
        return;
    }

    void begin(){
        if (overwrite){
            Toast.makeText(mActivity.get(),"Writing "+fPath,Toast.LENGTH_LONG).show();
            try {
                outGpx = new FileWriter(fPath);
                outGpx.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                outGpx.write("<gpx version=\"1.0\"\n");
                outGpx.write("   xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
                outGpx.write("   creator=\"Msb2Kml\"\n");
                outGpx.write("   xmlns=\"http://www.topografix.com/GPX/1/0\"\n");
                outGpx.write("   xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0");
                outGpx.write("http://www.topografix.com/GPX/1/0/gpx.xsd\">\n");
            } catch (IOException e){
                errorShow(e.getMessage());
                if (srcPath!=null && srcPath.contentEquals(fPath)) srcPath=null;
                launchSelect();
                return;
            }
        } else {
            try {
                temp = File.createTempFile("xxx", ".gpx", new File(curDir));
                tempPath=temp.getPath();
                outGpx=new FileWriter(temp);
            } catch (IOException e){
                errorShow(e.getMessage());
                if (srcPath!=null && srcPath.contentEquals(fPath)) srcPath=null;
                launchSelect();
                return;
            }
            if (!copy()){
                Toast.makeText(mActivity.get(),"File not copied",Toast.LENGTH_LONG);
                mActivity.get().recordWR0();
                return;
            }
        }
        SortedSet<Integer> keys=new TreeSet<>();
        keys.addAll(picked.keySet());
        Iterator<Integer> itr=keys.iterator();
        MainActivity.NamedLoc namedLoc=null;
        Integer indx=null;
        Location loc=null;
        if (asWpt) {
            while (itr.hasNext()){
                indx=itr.next();
                namedLoc=picked.get(indx);
                loc=namedLoc.loc;
                try {
                     outGpx.write(" <wpt ");
                     outGpx.write(String.format(Locale.ENGLISH,"lat=\"%.8f\" ",
                             loc.getLatitude()));
                     outGpx.write(String.format(Locale.ENGLISH,"lon=\"%.8f\">\n",
                             loc.getLongitude()));
                     if (loc.hasAltitude()) outGpx.write(String.format(Locale.ENGLISH,
                             "  <ele>%.3f</ele>\n",loc.getAltitude()));
                     outGpx.write("  <name>"+nameNorm(namedLoc.name)+"</name>\n");
                     outGpx.write(" </wpt>\n");
                } catch (IOException e){
                    errorShow(e.getMessage());
                    if (srcPath!=null && srcPath.contentEquals(fPath)) srcPath=null;
                    launchSelect();
                    return;
                }
            }
        } else {
            try {
                outGpx.write(" <rte>\n");;
                rteName=nameNorm(rteName);
                if (rteName!=null) outGpx.write("   <name>"+rteName+"</name>\n");
                rtePtNb=0;
            } catch (IOException e){
                errorShow(e.getMessage());
                if (srcPath!=null && srcPath.contentEquals(fPath)) srcPath=null;
                launchSelect();
                return;
            }
            while (itr.hasNext()){
                indx=itr.next();
                namedLoc=picked.get(indx);
                loc=namedLoc.loc;
                try {
                    rtePtNb++;
                    outGpx.write(" <rtept ");
                     outGpx.write(String.format(Locale.ENGLISH,"lat=\"%.8f\" ",
                             loc.getLatitude()));
                     outGpx.write(String.format(Locale.ENGLISH,"lon=\"%.8f\">",
                             loc.getLongitude()));
                     if (loc.hasAltitude()) outGpx.write(String.format(Locale.ENGLISH,
                             "  <ele>%.3f</ele>",loc.getAltitude()));
//                     comment=namedLoc.name;
//                     if (comment==null) comment=rtePtNb.toString();
//                     outGpx.write("  <cmt>"+comment+"</cmt>");
                     outGpx.write(" </rtept>\n");
                } catch (IOException e){
                    errorShow(e.getMessage());
                    if (srcPath!=null && srcPath.contentEquals(fPath)) srcPath=null;
                    launchSelect();
                    return;
                }
            }
            try {
                outGpx.write(" </rte>\n");;
            } catch (IOException e){
                errorShow(e.getMessage());
                if (srcPath!=null && srcPath.contentEquals(fPath)) srcPath=null;
                launchSelect();
                return;
            }
        }
        if (overwrite){
            try {
                outGpx.write("</gpx>\n");
                outGpx.close();
            } catch (IOException e){
                errorShow(e.getMessage());
                if (srcPath!=null && srcPath.contentEquals(fPath)) srcPath=null;
                launchSelect();
                return;
            }
        } else {
            try {
                outGpx.write("</gpx>\n");
                outGpx.close();
            } catch (IOException e){
                errorShow(e.getMessage());
                if (srcPath!=null && srcPath.contentEquals(fPath)) srcPath=null;
                launchSelect();
                return;
            }
            File fOld=new File(fPath);
            fOld.delete();
            File fNew=new File(tempPath);
            fNew.renameTo(fOld);
            Toast.makeText(mActivity.get(),"Added to "+fPath,Toast.LENGTH_LONG).show();
        }
        mActivity.get().start2(fPath);
    }

    String nameNorm(String name){
        if (name==null) return null;
        byte[] in=name.getBytes(Charset.forName("UTF-8"));
        String norm=new String(in);
        return norm;
    }

    Boolean copy (){
        BufferedReader fIn=null;
        copyLine="";
        Integer curnt=0;
        Integer len=0;
        String expr=null;
        String patrnGpx="(.*?)</gpx>";
        Pattern pGpx=Pattern.compile(patrnGpx);
        try {
            fIn=new BufferedReader(new InputStreamReader(new FileInputStream(fPath)));
        } catch (IOException e){
            errorShow(e.getMessage());
            return false;
        }
        while (true) {
            while (curnt < len) {
                Matcher m = pGpx.matcher(copyLine.substring(curnt, len));
                if (m.find()) {
                    expr = m.group(1);
                    if (expr.length() > 0) {
                        try {
                            outGpx.write(expr + "\n");
                        } catch (IOException e) {
                            errorShow(e.getMessage());
                            return false;
                        }
                    }
                    return true;
                } else {
                    try {
                        outGpx.write(copyLine.substring(curnt, len) + "\n");
                    } catch (IOException e) {
                        errorShow(e.getMessage());
                        return false;
                    }
                    curnt = len;
                }
            }
            try {
                copyLine = fIn.readLine();
                if (copyLine == null) {
                    try {
                        fIn.close();
                    } catch (IOException e) {
                        errorShow(e.getMessage());
                        return false;
                    }
                    return false;
                }
                len=copyLine.length();
                curnt = 0;
            } catch (IOException e) {
                errorShow(e.getMessage());
                return false;
            }
        }

    }

    void errorShow(String message){
        AlertDialog.Builder build=new AlertDialog.Builder(mActivity.get());
        build.setTitle("Error");
        build.setMessage(message);
        build.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        build.show();
    }
}
