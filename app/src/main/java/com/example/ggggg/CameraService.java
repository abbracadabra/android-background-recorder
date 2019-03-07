package com.example.ggggg;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;

import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraManager.AvailabilityCallback;
import android.os.IBinder;

import java.util.HashMap;

public class CameraService extends Service {

    CameraManager manager = MainActivity.manager;
    AvailabilityCallback callback;
    HashMap<String,CameraRecorder> cameras = MainActivity.cameras;

    @SuppressLint("MissingPermission")
    @Override
    public void onCreate() {
        super.onCreate();
        callback = new CameraAvalabilityCallback();
        manager.registerAvailabilityCallback(callback,null);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println("commandededed");
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        System.out.println("service destroy");
        super.onDestroy();
        manager.unregisterAvailabilityCallback(callback);
        for(String k : cameras.keySet()){
            cameras.get(k).finalize();
        }
    }


    class CameraAvalabilityCallback extends AvailabilityCallback{

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
}
