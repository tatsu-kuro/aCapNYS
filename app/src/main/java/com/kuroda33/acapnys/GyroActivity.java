package com.kuroda33.acapnys;

//import static androidx.transition.GhostViewHolder.getHolder;

//import static android.app.PendingIntent.getActivity;
//import static android.os.VibrationEffect.DEFAULT_AMPLITUDE;
//import static android.util.Log.i;
//import static android.view.View.SYSTEM_UI_FLAG_FULLSCREEN;
//import static android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
//import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
//import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
//import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
//import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;
//import static java.lang.Integer.*;
//import static java.lang.Math.PI;
//import static java.lang.Math.cos;
//import static java.lang.Math.round;
//import static java.lang.Math.sin;
//import static java.lang.Math.sqrt;
//import static java.security.AccessController.getContext;
//import static java.sql.Types.NULL;
//import static java.lang.Math.sqrt;

//import androidx.activity.result.ActivityResultLauncher;
//import androidx.activity.result.contract.ActivityResultContracts;
//import androidx.annotation.NonNull;
//import androidx.annotation.RequiresApi;
//import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.app.ActivityCompat;
//import androidx.recyclerview.widget.DefaultItemAnimator;

//import android.Manifest;
//import android.app.Activity;
//import android.bluetooth.BluetoothAdapter;
//import android.bluetooth.BluetoothDevice;
//import android.bluetooth.BluetoothManager;
//import android.bluetooth.BluetoothSocket;
import android.annotation.SuppressLint;
import android.content.Context;
//import android.content.Intent;
import android.content.SharedPreferences;
//import android.content.pm.PackageManager;
//import android.graphics.Canvas;
//import android.graphics.Color;
//import android.graphics.Paint;
//import android.graphics.Path;
//import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
//import android.media.AudioAttributes;
//import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
//import android.os.Handler;
//import android.os.Looper;
//import android.os.VibrationEffect;
//import android.os.Vibrator;
//import android.preference.PreferenceManager;
//import android.text.Editable;
//import android.text.method.ScrollingMovementMethod;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
//import android.view.SurfaceHolder;
//import android.view.SurfaceView;
import android.view.View;
import android.view.WindowInsets;
//import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
//import android.widget.LinearLayout;
//import android.widget.Spinner;
//import android.widget.TextView;
//import android.widget.Toast;

//import java.io.ByteArrayOutputStream;
import java.io.IOException;
//import java.io.ObjectOutput;
//import java.io.ObjectOutputStream;
//import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
//import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

//import java.util.ArrayList;
//import java.util.Set;
//import java.util.UUID;
//import static java.util.concurrent.TimeUnit.NANOSECONDS;
//import static java.util.concurrent.TimeUnit.NANOSECONDS;
public class GyroActivity extends AppCompatActivity implements SensorEventListener{
    SensorManager sensorManager;
   // private SoundPool soundPool;
 //   private int sound1,sound2,sound3;
    long millis0 = TimeUnit.MILLISECONDS.ordinal();
    private final String TAG = "MainActivity";
    InetSocketAddress inetSocketAddress = null;
  //  private AlertDialog.Builder mAlertDialog;
    private EditText ipe1,ipe2,ipe3,ipe4;
  /*  private EditText pitchDegreeE,pitchSecE,rollDegreeE,rollSecE,yawDegreeE,yawSecE;
    private EditText pitchCurrentE,pitchCountE,rollCurrentE,rollCountE,yawCurrentE,yawCountE;
    private float pitchDegree,rollDegree,yawDegree;
    float pitchSec,rollSec,yawSec;*/
    Button ipSetBtn;
    Button exitBtn;
 /*   Button rehaStartBtn;
    Button rehaStopBtn;
    Button rehaResetBtn;
    Button pitchDegreeUpBtn;
    Button pitchDegreeDownBtn;
    Button pitchSecUpBtn;
    Button pitchSecDownBtn;

    Button rollDegreeUpBtn;
    Button rollDegreeDownBtn;
    Button rollSecUpBtn;
    Button rollSecDownBtn;

    Button yawDegreeUpBtn;
    Button yawDegreeDownBtn;
    Button yawSecUpBtn;
    Button yawSecDownBtn;
    private Spinner soundSpin,vibrationSpin;*/

//    private int soundType;
  //  private int vibrationType;
    //private boolean rehaF=true;

    private void setNavigationBar(){//わからない
        View decor = getWindow().getDecorView();
        // API 30以上の場合
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            decor.getWindowInsetsController().hide(WindowInsets.Type.systemBars());
        } else {
            // API 29以下の場合
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
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
            //   Log.d(TAG, "surfaceDestroyed...");
         //   rehaF=false;
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
        String Str = String.format("Q:%03d%03d%03d%03d", b0, b1, b2, b3);
        //   String Str1=String.format("%06f,%06f",nq1,nq2);
        //  Log.e("packet:",Str1);
        //  send(StringStr, String "192.168.0.209", int 1108) throws IOException {
        byte[] dataStr = Str.getBytes(StandardCharsets.UTF_8);
        //      ipad = "192.168.0.209";
        //   InetSocketAddress inetSocketAddress = new InetSocketAddress(ipad, portn);
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
             //   cq0 = nq0; cq3 = -nq3;

                String Str = String.format ("eventX:%03d Y:%03d",x,y);
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