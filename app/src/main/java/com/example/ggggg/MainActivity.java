package com.example.ggggg;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraManager.AvailabilityCallback;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Size;
import android.view.MenuItem;
import com.alibaba.fastjson.JSONObject;
import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    public static File savedir;
    public static CameraManager manager;

    final Fragment fragment1 = new HomeFragment();
    final Fragment fragment2 = new ConfigFragment();
    final FragmentManager fm = getSupportFragmentManager();
    Fragment active = fragment1;
    AvailabilityCallback callback;
    public static JSONObject config;

    public static HashMap<String,CameraRecorder> cameras = new HashMap<>();

    static Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        context = getApplicationContext();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.bottom_navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        fm.beginTransaction().add(R.id.content_frame, fragment2, "2").hide(fragment2).commit();
        fm.beginTransaction().add(R.id.content_frame,fragment1, "1").commit();

        manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        askPermissionCamera();
        init();


        callback = new CameraAvalabilityCallback();
        manager.registerAvailabilityCallback(callback,null);

        /*Intent intent = new Intent(MainActivity.this, CameraService.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startService(intent);*/
    }

    private void init() {
        try {
            writeConfig();
            config = (JSONObject) JSONObject.parse(PreferenceManager.getDefaultSharedPreferences(this).getString("config",""));
            checkDirExistence();
            JSONObject cas = config.getJSONObject("cameras");
            for(String s : cas.keySet()){
                JSONObject copt = cas.getJSONObject(s);
                cameras.put(s,new CameraRecorder(s));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private void writeConfig() {
        try{
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String _x = prefs.getString("config", "");
            if (!_x.equals("")) {
                return;
            }
            SharedPreferences.Editor editor = prefs.edit();
            JSONObject _config = new JSONObject();
            _config.put("savedir", Paths.get(Environment.getExternalStorageDirectory().getAbsolutePath(), "000000").toString());
            JSONObject camopt = new JSONObject();
            _config.put("cameras", camopt);
            String[] cameraids = manager.getCameraIdList();
            for (String cameraid : cameraids) {
                JSONObject _c = new JSONObject();
                camopt.put(cameraid,_c);
                _c.put("cameraname", cameraid);
                CameraCharacteristics ccs = manager.getCameraCharacteristics(cameraid);
                StreamConfigurationMap scm = ccs.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                Size[] sizes = scm.getOutputSizes(SurfaceTexture.class);
                JSONObject ss = new JSONObject();
                _c.put("selectedsize", ss);
                ss.put("width", String.valueOf(sizes[0].getWidth()));
                ss.put("height", String.valueOf(sizes[0].getHeight()));
                _c.put("framerate", "30");
                _c.put("bitrate", "1000000");
                if (CameraCharacteristics.LENS_FACING_FRONT == ccs.get(CameraCharacteristics.LENS_FACING)) {
                    _c.put("cameraname", "前置" + cameraid);
                }
                if (CameraCharacteristics.LENS_FACING_BACK == ccs.get(CameraCharacteristics.LENS_FACING)) {
                    _c.put("cameraname", "后置" + cameraid);
                }

                String[] _ss = new String[sizes.length];
                for(int i=0;i<sizes.length;i++){
                    Size size = sizes[i];
                    _ss[i] = String.valueOf(size.getWidth())+"*"+String.valueOf(size.getHeight());
                }
                _c.put("sizes",String.join(",", _ss));
            }
            String jstr = _config.toJSONString();
            editor.putString("config", jstr);
            editor.apply();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.nav_home:
                    fm.beginTransaction().hide(active).show(fragment1).commit();
                    active = fragment1;
                    return true;

                case R.id.nav_setting:
                    fm.beginTransaction().hide(active).show(fragment2).commit();
                    active = fragment2;
                    return true;
            }
            return false;
        }
    };

    class CameraAvalabilityCallback extends AvailabilityCallback {

        @SuppressLint("MissingPermission")
        @Override
        public void onCameraAvailable(String cameraId){
            System.out.println("camera free:"+cameraId);
            if(cameras.get(cameraId)!=null){
                cameras.get(cameraId).isfree=true;
            }
        }

        @Override
        public void onCameraUnavailable(String cameraId){
            System.out.println("camera taken:"+cameraId);
            if(cameras.get(cameraId)!=null){
                cameras.get(cameraId).isfree=false;
            }
        }
    }


    @Override
    protected void onDestroy() {
        System.out.println("activity destroy");
        //stopService(new Intent(MainActivity.this, CameraService.class));
        manager.unregisterAvailabilityCallback(callback);
        for(String k : cameras.keySet()){
            cameras.get(k).finalize();
        }
        super.onDestroy();
    }



    private void checkDirExistence() {
        savedir = new File(config.getString("savedir"));
        if(!savedir.exists()) {
            boolean s = savedir.mkdirs();
            if(!s) {
                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setMessage("新建文件夹失败！");
                alert.show();
            }
        }
    }

    private void askPermissionCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{android.Manifest.permission.CAMERA,android.Manifest.permission.RECORD_AUDIO,Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    1);
        }
    }

}
