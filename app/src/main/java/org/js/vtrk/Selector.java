package org.js.vtrk;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class Selector extends AppCompatActivity {

    navDir nav=null;
    String theList[];
    int theSelected=-1;
    String exPath=Environment.getExternalStorageDirectory().getAbsolutePath();
    String rmvPath=null;
    final String fixLbl="[*Local*]";
    final String rmvLbl="[*Removable*]";
    String prevPath=null;
    String prevName=null;
    Boolean creable=false;
    String Title=null;
    String Suffix=null;
    Context context=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context=getApplicationContext();
        setContentView(R.layout.activity_selector);
        Intent intent=getIntent();
        String currentDir=intent.getStringExtra("CurrentDir");
        rmvPath=System.getenv("SECONDARY_STORAGE");
        if (rmvPath!=null){
            File f=new File(rmvPath);
            if (!f.exists() || !f.isDirectory() || !f.canRead()) rmvPath=null;
            else {
                String parent=f.getParent();
                if (parent!=null && !parent.contentEquals("/")){
                   File f2=new File(parent);
                   if (f2.exists() && f2.isDirectory() && f2.canRead()) rmvPath=parent;
                }
            }
        }
        nav=new navDir(exPath,rmvPath);
        nav.setCurDir(currentDir);
        String Mask=intent.getStringExtra("Mask");
        nav.setMask(Mask);
        Boolean WithDir=intent.getBooleanExtra("WithDir",false);
        prevPath=intent.getStringExtra("Previous");
        if (prevPath!=null) prevName=(new File(prevPath)).getName();
        creable=intent.getBooleanExtra("Creable",false);
        Suffix=intent.getStringExtra("Suffix");
        Title=intent.getStringExtra("Title");
        if (WithDir) selWtDir();
        else selNoDir();
    }

    String toLabel(String path){
        if (path==null) return null;
        if (path.startsWith(exPath)){
            return fixLbl+path.substring(exPath.length());
        } else if (rmvPath!=null && path.startsWith(rmvPath)){
            return rmvLbl+path.substring(rmvPath.length());
        } else return path;
    }

    String fromLabel(String path){
        if (path==null) return null;
        if (path.startsWith(fixLbl)){
            return exPath+path.substring(fixLbl.length());
        } else if (rmvPath!=null && path.startsWith(rmvLbl)){
            return rmvPath+path.substring(rmvLbl.length());
        } else return path;
    }

    void selWtDir() {
        theList = nav.get();
        for (int i=0;i<theList.length;i++){
            theList[i]=toLabel(theList[i]);
        }
        theSelected = -1;
        AlertDialog.Builder build=new AlertDialog.Builder(this);
        if (Title==null) build.setTitle(toLabel(nav.getDir()));
        else build.setTitle((Title+toLabel(nav.getDir())));
        build.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            finish();
                        }
                })
           .setSingleChoiceItems(theList, theSelected, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        theSelected=which;
                    }
                })
           .setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                })
           .setPositiveButton("Select", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        show();
                    }
                })
           .setNegativeButton("Follow Directory", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (theList[theSelected].endsWith("/")){
                            if (theSelected==0){
                                nav.upDir();
                            }
                            else {
                                nav.dnDir(fromLabel(theList[theSelected]));
                            }
                        }
                        selWtDir();
                    }
                });
        build.show();
    }

    void selNoDir(){
        theList=nav.get();
        for (int i=0;i<theList.length;i++){
            theList[i]=toLabel(theList[i]);
        }
        theSelected=-1;
        AlertDialog.Builder build=new AlertDialog.Builder(this);
        if (Title==null) build.setTitle(toLabel(nav.getDir()));
        else build.setTitle((Title+toLabel(nav.getDir())));
        build.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            finish();
                        }
                })
           .setItems(theList, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        theSelected=which;
                        if (which==0){
                            nav.upDir();
                            selNoDir();
                        } else if (theList[which].endsWith("/")){
                            nav.dnDir(fromLabel(theList[which]));
                            selNoDir();
                        } else {
                            show();
                        }
                    }
                })
           .setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                });
        if (prevPath!=null) build.setNegativeButton("["+prevName+"]",
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                conclusion(prevPath);
            }
        });
        String bNew="New";
        if (!nav.getWriteable()) bNew="not writable";
        if (creable) build.setPositiveButton(bNew,
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                create();
            }
        });
        build.show();
    }

    void show(){
       if (! (theSelected<0)) {
            String path;
            if (theSelected==0) {
                 path = nav.upDir();
            }
            else {
                path=nav.getDir()+"/"+fromLabel(theList[theSelected]);
            }
            Intent result=new Intent();
            result.putExtra("Path",path);
           setResult(RESULT_OK,result);
       }
       finish();
    }

    void conclusion(String path){
        Intent result=new Intent();
        result.putExtra("Path",path);
        setResult(RESULT_OK,result);
        finish();
    }

    void create(){
        if (!nav.getWriteable()){
            Toast.makeText(context,"Repertory not writable",Toast.LENGTH_LONG).show();
            selNoDir();
            return;
        }
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                selNoDir();
            }
        });
        View creaView=View.inflate(this,R.layout.creation,null);
        TextView vTitle=creaView.findViewById(R.id.creatitle);
        TextView vSufx=creaView.findViewById(R.id.creagpx);
        final EditText vName=creaView.findViewById(R.id.creaname);
        vTitle.setText("Create new file in "+toLabel(nav.getDir()));
        if (Suffix==null) vSufx.setText("");
        else vSufx.setText(Suffix);
        builder.setView(creaView);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String inName=vName.getText().toString();
                if (inName!=null) inName=inName.trim();
                InputMethodManager imm = (InputMethodManager)
                                context.getSystemService(Activity.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(vName.getWindowToken(),0);
                if (inName==null || inName.isEmpty()){
                    selNoDir();

                }
                String path=nav.getDir()+"/"+inName+Suffix;
                conclusion(path);
            }
        });
        builder.show();
    }

}
