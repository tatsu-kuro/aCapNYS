package com.kuroda33.acapnys;

import androidx.appcompat.app.AppCompatActivity;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Button;
import android.widget.EditText;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GyroActivity extends AppCompatActivity implements SensorEventListener{
    SensorManager sensorManager;

    private final String TAG = "MainActivity";
    InetSocketAddress inetSocketAddress = null;

    private EditText ipe1,ipe2,ipe3,ipe4;

    Button ipSetBtn;
    ImageButton exitBtn;


    private void setNavigationBar(){// no-op: keep system bars visible
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setNavigationBar();

        setContentView(R.layout.activity_gyro);

        exitBtn = findViewById(R.id.exitButton);
        ipSetBtn =  findViewById(R.id.ipSetButton);
        exitBtn =  findViewById(R.id.exitButton);
        ipe1 =  findViewById(R.id.ip1);
        ipe2 =  findViewById(R.id.ip2);
        ipe3 =  findViewById(R.id.ip3);
        ipe4 =  findViewById(R.id.ip4);
        loadData();
        exitBtn.setOnClickListener(v -> {
            finish();
        });
        ipSetBtn.setOnClickListener(v -> {
            SharedPreferences data = getSharedPreferences("Data", MODE_PRIVATE);
            SharedPreferences.Editor editor = data.edit();

            editor.putString("ip1", ipe1.getText().toString());
            editor.putString("ip2", ipe2.getText().toString());
            editor.putString("ip3", ipe3.getText().toString());
            editor.putString("ip4", ipe4.getText().toString());
            ipad=String.format("%s.%s.%s.%s",ipe1.getText().toString(),ipe2.getText().toString(),ipe3.getText().toString(),ipe4.getText().toString());

            inetSocketAddress = new InetSocketAddress(ipad, portn);

            editor.commit();
            editor.apply();
            Log.d(TAG,"onclick ips");
        });
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
       // //     sma.registerListener(this,sma.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR),SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR),SensorManager.SENSOR_DELAY_GAME);// SENSOR_DELAY_NORMAL);// SENSOR_DELAY_FASTEST);

    }

     String ipad="192.168.0.209";
    int portn=1108;

    @SuppressLint("DefaultLocale")
    private void loadData() {
        SharedPreferences data = getSharedPreferences("Data", MODE_PRIVATE);


        ipe1.setText(data.getString("ip1","192"));
        ipe2.setText(data.getString("ip2","168"));
        ipe3.setText(data.getString("ip3","0"));
        ipe4.setText(data.getString("ip4","99"));
        //    ipad = String.format ("%s.%s.%s.%s",ipe1,ipe2,ipe3,ipe4);//ipe1 + "." "192.168.0.209";
        ipad=String.format("%s.%s.%s.%s",ipe1.getText().toString(),ipe2.getText().toString(),ipe3.getText().toString(),ipe4.getText().toString());
        Log.i("kuroa****",ipad);
        inetSocketAddress = new InetSocketAddress(ipad, portn);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    public void onSensorChanged(SensorEvent event) {

        float nq0 = event.values[3];
        float nq1 = event.values[0];
        float nq2 = event.values[1];
        float nq3 = event.values[2];
        int b0 = (int) ((nq0 + 1.0) * 128.0);
        int b1 = (int) ((nq1 + 1.0) * 128.0);
        int b2 = (int) ((nq2 + 1.0) * 128.0);
        int b3 = (int) ((nq3 + 1.0) * 128.0);
        String Str;
        Str = String.format("Q:%03d%03d%03d%03d", b0, b1, b2, b3);

        byte[] dataStr = Str.getBytes(StandardCharsets.UTF_8);

        DatagramPacket datagramPacket = new DatagramPacket(dataStr, dataStr.length, inetSocketAddress);

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        executorService.submit(new Runnable() {
            @Override
            public void run() {
                DatagramSocket datagramSocket;
                try {
                    datagramSocket = new DatagramSocket();
                    datagramSocket.send(datagramPacket);
                    datagramSocket.close();
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // センサーの精度が変更されると呼ばれる
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x=(int) event.getX();
        int y= (int) event.getY();
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:

                String Str;
                Str = String.format ("eventX:%03d Y:%03d",x,y);
                Log.d(TAG, Str);
                // mPath.moveTo(x,y);
                break;
            case MotionEvent.ACTION_MOVE:
                Log.d(TAG, "onTouchEvent: move22");
                // mPath.lineTo(x,y);
                break;
            case MotionEvent.ACTION_UP:
                Log.d(TAG, "onTouchEvent: up33");
                break;
        }
        return true;
    }

   @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
    }
}
