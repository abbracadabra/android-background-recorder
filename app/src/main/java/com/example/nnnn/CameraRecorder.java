package com.example.nnnn;

import android.annotation.SuppressLint;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;
import android.widget.Toast;

import com.alibaba.fastjson.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CameraRecorder {

    MediaRecorder mMediaRecorder;
    CameraDevice mCamera;
    CameraCaptureSession mSession;
    CameraManager manager = MainActivity.manager;

    public String cameraid;
    public boolean isfree;
    public boolean isrecording;
    public boolean isConnected;
    Looper looper;
    Handler handler;
    Thread t_t;

    int bitrate;
    int framerate;
    int width;
    int height;
    String cameraname;

    CameraRecorder(final String cameraid){
        this.cameraid = cameraid;
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                looper = Looper.myLooper();
                handler = new Handler();
                Looper.loop();
            }
        }).start();

        t_t = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    if(Thread.currentThread().isInterrupted()){
                        break;
                    }
                    if(isfree && isrecording){
                        pause();
                        try {
                            System.out.println("about to open camera:"+cameraid);
                            opencamera();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if(isConnected && isrecording){
                        resume();
                    }
                }
            }
        });
        t_t.start();
    }

    private void initparam() {
        try{
            JSONObject cas = MainActivity.config.getJSONObject("cameras");
            JSONObject copt = cas.getJSONObject(cameraid);
            this.bitrate = Integer.valueOf(copt.getString("bitrate"));
            this.framerate = Integer.valueOf(copt.getString("framerate"));
            this.width = Integer.valueOf(copt.getJSONObject("selectedsize").getString("width"));
            this.height = Integer.valueOf(copt.getJSONObject("selectedsize").getString("height"));
            this.cameraname = copt.getString("cameraname");
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @SuppressLint("MissingPermission")
    private void opencamera() throws CameraAccessException {
        manager.openCamera(cameraid, new CameraDevice.StateCallback() {
            @Override
            public void onOpened(CameraDevice camera) {
                mCamera = camera;
                try {
                    List<Surface> targets = new ArrayList<>();
                    targets.add(mMediaRecorder.getSurface());
                    //create session
                    mCamera.createCaptureSession(targets, new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            mSession = session;
                            try {
                                //create and configure request
                                CaptureRequest.Builder captureRequest = null;
                                captureRequest = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                                captureRequest.addTarget(mMediaRecorder.getSurface());
                                CaptureRequest mCaptureRequest = captureRequest.build();
                                //issue request
                                mSession.setRepeatingRequest(mCaptureRequest, null, null);
                                System.out.println("inging");
                                isConnected = true;
                            } catch (CameraAccessException e) {
                                isConnected = false;
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {
                            isConnected = false;
                            mSession = session;
                            System.out.println("configure failed:"+cameraid);
                        }
                    }, handler);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onDisconnected(CameraDevice camera) {
                isConnected = false;
                mCamera = camera;
                System.out.println("disconnected:"+cameraid);
            }

            @Override
            public void onError(CameraDevice camera, int error) {
                isConnected = false;
                mCamera = camera;
                System.out.println("open camera errored:"+cameraid);
            }
        }, handler);
    }

    public synchronized void start() {
        try{
            if(isrecording == true){
                stop();
            }
            initparam();
            mMediaRecorder = new MediaRecorder();
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mMediaRecorder.setVideoEncodingBitRate(bitrate);
            mMediaRecorder.setVideoFrameRate(framerate);
            mMediaRecorder.setVideoSize(width,height);
            mMediaRecorder.setMaxFileSize(0);
            mMediaRecorder.setOrientationHint(0);
            File outputfile = new File(MainActivity.savedir,new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS'.mp4'").format(new Date()));
            mMediaRecorder.setOutputFile(outputfile.getCanonicalPath());
            outputfile.createNewFile();
            mMediaRecorder.prepare();
            mMediaRecorder.start();
            isrecording = true;
            Toast.makeText(MainActivity.context,"ok", Toast.LENGTH_SHORT).show();
            System.out.println("inging");
        }catch (Exception e){
            isrecording = false;
            Toast.makeText(MainActivity.context,"error", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public void pause(){
        mMediaRecorder.pause();
    }

    public void resume(){
        try{
            mMediaRecorder.resume();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public synchronized void stop(){
        isrecording = false;
        isConnected = false;
        try{
            mMediaRecorder.stop();
            mMediaRecorder.release();
        }catch (Exception e){
            e.printStackTrace();
        }
        try {
            mCamera.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void finalize(){
        System.out.println("recorder finalize");
        stop();
        t_t.interrupt();
        looper.quit();
        try{
            mCamera.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        try{
            mSession.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
