package com.kuroda33.acapnys;

//import static androidx.transition.GhostViewHolder.getHolder;

import static android.app.PendingIntent.getActivity;
import static android.os.VibrationEffect.DEFAULT_AMPLITUDE;
import static android.util.Log.i;
import static android.view.View.SYSTEM_UI_FLAG_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;
import static java.lang.Integer.*;
import static java.lang.Math.PI;
import static java.lang.Math.cos;
import static java.lang.Math.round;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.security.AccessController.getContext;
import static java.sql.Types.NULL;
//import static java.lang.Math.sqrt;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
//import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
//import androidx.recyclerview.widget.DefaultItemAnimator;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
//import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class GyroActivity extends AppCompatActivity implements SensorEventListener{
    SensorManager sensorManager;
    private SoundPool soundPool;
    private int sound1,sound2,sound3;
    long millis = TimeUnit.MILLISECONDS.ordinal();
    private final String TAG = "MainActivity";
    InetSocketAddress inetSocketAddress = null;
    private AlertDialog.Builder mAlertDialog;
    private EditText ipe1,ipe2,ipe3,ipe4;
    private EditText pitchDegreeE,pitchSecE,rollDegreeE,rollSecE,yawDegreeE,yawSecE;
    private EditText pitchCurrentE,pitchCountE,rollCurrentE,rollCountE,yawCurrentE,yawCountE;
    private float pitchDegree,rollDegree,yawDegree;
    private float pitchSec,rollSec,yawSec;
    private Button ipSetBtn;
    private Button rehaStartBtn;
    private Button rehaStopBtn;
    private Button rehaResetBtn;
    private Button pitchDegreeUpBtn;
    private Button pitchDegreeDownBtn;
    private Button pitchSecUpBtn;
    private Button pitchSecDownBtn;

    private Button rollDegreeUpBtn;
    private Button rollDegreeDownBtn;
    private Button rollSecUpBtn;
    private Button rollSecDownBtn;

    private Button yawDegreeUpBtn;
    private Button yawDegreeDownBtn;
    private Button yawSecUpBtn;
    private Button yawSecDownBtn;
    private Spinner soundSpin,vibrationSpin;

    private int soundType;
    private int vibrationType;
    private boolean rehaF=true;

    ArrayList <Float>  pitchA;
    {
        pitchA = new ArrayList<>();
    }
    ArrayList <Float>  rollA;
    {
        rollA = new ArrayList<>();
    }
    ArrayList <Float>  yawA;
    {
        yawA = new ArrayList<>();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gyro);
        AudioAttributes audioAttributes= new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                .build();
        soundPool = new SoundPool.Builder()
                .setAudioAttributes(audioAttributes)
                .setMaxStreams(3)
                .build();
        sound1 = soundPool.load(this,R.raw.beep1,1);
        sound2 = soundPool.load(this,R.raw.beep2,1);
        sound3 = soundPool.load(this,R.raw.beep3,1);


        i(TAG, "onCreate 0");
        //  quaterView = (TextView) findViewById(R.id.quaternionData);
        //  quaterView.setMovementMethod(new ScrollingMovementMethod());
 //       Selbtn = (Button) findViewById(R.id.selectButton);
        ipSetBtn = (Button) findViewById(R.id.ipSetButton);
        rehaStartBtn = (Button) findViewById(R.id.startButton);
        rehaStopBtn = (Button) findViewById(R.id.stopButton);
        rehaResetBtn = (Button) findViewById(R.id.resetButton);
        pitchDegreeDownBtn = (Button) findViewById(R.id.pitchDegreeDownButton);
        pitchDegreeUpBtn = (Button) findViewById(R.id.pitchDegreeUpButton);
        pitchSecDownBtn = (Button) findViewById(R.id.pitchSecDownButton);
        pitchSecUpBtn = (Button) findViewById(R.id.pitchSecUpButton);
        rollDegreeDownBtn = (Button) findViewById(R.id.rollDegreeDownButton);
        rollDegreeUpBtn = (Button) findViewById(R.id.rollDegreeUpButton);
        rollSecDownBtn = (Button) findViewById(R.id.rollSecDownButton);
        rollSecUpBtn = (Button) findViewById(R.id.rollSecUpButton);
        yawDegreeDownBtn = (Button) findViewById(R.id.yawDegreeDownButton);
        yawDegreeUpBtn = (Button) findViewById(R.id.yawDegreeUpButton);
        yawSecDownBtn = (Button) findViewById(R.id.yawSecDownButton);
        yawSecUpBtn = (Button) findViewById(R.id.yawSecUpButton);

        ipe1 = (EditText) findViewById(R.id.ip1);
        ipe2 = (EditText) findViewById(R.id.ip2);
        ipe3 = (EditText) findViewById(R.id.ip3);
        ipe4 = (EditText) findViewById(R.id.ip4);
        pitchCurrentE = (EditText) findViewById(R.id.pitch0);
        pitchCountE = (EditText) findViewById(R.id.pitch1);
        rollCurrentE = (EditText) findViewById(R.id.roll0);
        rollCountE = (EditText) findViewById(R.id.roll1);
        yawCurrentE = (EditText) findViewById(R.id.yaw0);
        yawCountE = (EditText) findViewById(R.id.yaw1);

        pitchDegreeE = (EditText) findViewById(R.id.pitch2);
        pitchSecE= (EditText) findViewById(R.id.pitch3);
        rollDegreeE = (EditText) findViewById(R.id.roll2);
        rollSecE = (EditText) findViewById(R.id.roll3);
        yawDegreeE = (EditText) findViewById(R.id.yaw2);
        yawSecE = (EditText) findViewById(R.id.yaw3);

        soundSpin = (Spinner) findViewById(R.id.soundSpinner);
        vibrationSpin = (Spinner)findViewById(R.id.vibrationSpinner);

        loadData();

        mAlertDialog = new AlertDialog.Builder(this);
        mAlertDialog.setTitle("Alert");
        mAlertDialog.setPositiveButton("OK", null);

  /*      CamSelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                prepareSerialCommunication();
                i(TAG, "onClick CamSelBtn.");
            }
        });
        Selbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                getTargetAddress();
                //   prepareSerialCommunication();
            }
        });
   */
   /*     soundSpin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("spin****", (String) soundSpin.getSelectedItem());
            }
        });*/
        soundSpin.setOnItemSelectedListener(new soundSpinnerSelectedListener());
        vibrationSpin.setOnItemSelectedListener(new vibrationSpinnerSelectedListener());
        ipSetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ips1=ipe1.getText().toString();
                String ips2=ipe2.getText().toString();
                String ips3=ipe3.getText().toString();
                String ips4=ipe4.getText().toString();
          //      saveData();
                SharedPreferences data = getSharedPreferences("Data", MODE_PRIVATE);
                SharedPreferences.Editor editor = data.edit();

                editor.putString("ip1", ipe1.getText().toString());
                editor.putString("ip2", ipe2.getText().toString());
                editor.putString("ip3", ipe3.getText().toString());
                editor.putString("ip4", ipe4.getText().toString());
                ipad=String.format("%s.%s.%s.%s",ipe1.getText().toString(),ipe2.getText().toString(),ipe3.getText().toString(),ipe4.getText().toString());
                Log.i("kuroa****",ipad);

                inetSocketAddress = new InetSocketAddress(ipad, portn);

                editor.commit();
                editor.apply();


           //     i(TAG,ips1+ips2+ips3+ips4);
                Log.d(TAG,"onclick ips");
            }
        });
        pitchDegreeDownBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pitchDegree -= 1;
                saveData();
                loadData();
                //     i(TAG,ips1+ips2+ips3+ips4);
                Log.d(TAG,"onclickStart");
            }
        });
        pitchDegreeUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pitchDegree += 1;
                saveData();
                loadData();
                //     i(TAG,ips1+ips2+ips3+ips4);
                Log.d(TAG,"onclickStart");
            }
        });
        pitchSecDownBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pitchSec -= 0.1;
                saveData();
                loadData();
                //     i(TAG,ips1+ips2+ips3+ips4);
                Log.d(TAG,"onclickStart");
            }
        });
        pitchSecUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pitchSec += 0.1;
                saveData();
                loadData();
                Log.d(TAG,"onclickStart");
            }
        });
        rollDegreeDownBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rollDegree -= 1;
                saveData();
                loadData();
                //     i(TAG,ips1+ips2+ips3+ips4);
                Log.d(TAG,"onclick rolldegreedown");
            }
        });
        rollDegreeUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rollDegree += 1;
                saveData();
                loadData();
                //     i(TAG,ips1+ips2+ips3+ips4);
                Log.d(TAG,"onclickStart");
            }
        });
        rollSecDownBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rollSec -= 0.1;
                saveData();
                loadData();
                Log.d(TAG,"onclickStart");
            }
        });
        rollSecUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rollSec += 0.1;
                saveData();
                loadData();
                Log.d(TAG,"onclick rollsecup");
            }
        });
        yawDegreeDownBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                yawDegree -= 1;
                saveData();
                loadData();
                //     i(TAG,ips1+ips2+ips3+ips4);
                Log.d(TAG,"onclick yawdegreedown");
            }
        });
        yawDegreeUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                yawDegree += 1;
                saveData();
                loadData();
                //     i(TAG,ips1+ips2+ips3+ips4);
                Log.d(TAG,"onclick yawdegreeup");
            }
        });
        yawSecDownBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                yawSec -= 0.1;
                saveData();
                loadData();
                Log.d(TAG,"onclickStart");
            }
        });
        yawSecUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                yawSec += 0.1;
                saveData();
                loadData();
                Log.d(TAG,"onclickStart");
            }
        });

        rehaStartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rehaF=true;
           //     i(TAG,ips1+ips2+ips3+ips4);
                Log.d(TAG,"onclickStart");
            }
        });
        rehaStopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rehaF=false;
                //     i(TAG,ips1+ips2+ips3+ips4);
                Log.d(TAG,"onclickStop");
            }
        });
        rehaResetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //     i(TAG,ips1+ips2+ips3+ips4);
                Log.d(TAG,"onclickReset");
            }
        });
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        //     sma.registerListener(this,sma.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR),SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR),SensorManager.SENSOR_DELAY_FASTEST);
        //set_rpk_ppk();

        Button ExitBtn=findViewById(R.id.exitButton);
        ExitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
             //   Log.d(TAG, "surfaceDestroyed...");
                finish();
            }
        });
    }
    public class soundSpinnerSelectedListener implements AdapterView.OnItemSelectedListener{
        public void onItemSelected(AdapterView parent, View view, int position, long id) {
            // Spinner を取得
            Spinner spinner = (Spinner) parent;
            // 選択されたアイテムのテキストを取得
            //String str = spinner.getSelectedItem().toString();
            //Log.d(TAG,str);
            soundType=Math.toIntExact (spinner.getSelectedItemId());
            saveData();
            loadData();
            //TextView textView1 = (TextView)findViewById(R.id.textView1);
            //textView1.setText(str);
        }

        // 何も選択されなかった時の動作
        public void onNothingSelected(AdapterView parent) {
        }
    }
    public class vibrationSpinnerSelectedListener implements AdapterView.OnItemSelectedListener{
        public void onItemSelected(AdapterView parent, View view, int position, long id) {
            // Spinner を取得
            Spinner spinner = (Spinner) parent;
            // 選択されたアイテムのテキストを取得
        //    String str = String.valueOf(spinner.getSelectedItemId());
            //vibrationType=
            vibrationType=Math.toIntExact (spinner.getSelectedItemId());
            saveData();
            loadData();
     //       Log.d(TAG,str);
            //TextView textView1 = (TextView)findViewById(R.id.textView1);
            //textView1.setText(str);
        }

        // 何も選択されなかった時の動作
        public void onNothingSelected(AdapterView parent) {
        }
    }
    private void saveData() {
        SharedPreferences data = getSharedPreferences("Data", MODE_PRIVATE);
        SharedPreferences.Editor editor = data.edit();
        if(pitchDegree>120)pitchDegree=120;
        else if(pitchDegree<20)pitchDegree=20;
        if(rollDegree>120)rollDegree=120;
        else if(rollDegree<20)rollDegree=20;
        if(yawDegree>120)yawDegree=120;
        else if(yawDegree<20)yawDegree=20;
        if(pitchSec>4)pitchSec=4;
        else if(pitchSec<0.3)pitchSec=0.3F;
        if(rollSec>4)rollSec=4;
        else if(rollSec<0.3)rollSec=0.3F;
        if(yawSec>4)yawSec=4;
        else if(yawSec<0.3)yawSec=0.3F;

        editor.putFloat("pitchDegree",pitchDegree);
        editor.putFloat("pitchSec",pitchSec);
        editor.putFloat("rollDegree",rollDegree);
        editor.putFloat("rollSec",rollSec);
        editor.putFloat("yawDegree",yawDegree);
        editor.putFloat("yawSec",yawSec);
        editor.putInt("soundType",soundType);
        editor.putInt("vibrationType",vibrationType);
        editor.putString("ip1", ipe1.getText().toString());
        editor.putString("ip2", ipe2.getText().toString());
        editor.putString("ip3", ipe3.getText().toString());
        editor.putString("ip4", ipe4.getText().toString());
        editor.commit();
        editor.apply();
    }

    String ipad="192.168.0.209";
    int portn=1108;
/*
vibeSegmentCtl.selectedSegmentIndex=myFunctions().getUserDefaultInt(str:"vibrationType",ret:1)
soundSegmentCtl.selectedSegmentIndex=myFunctions().getUserDefaultInt(str:"soundType",ret:1)
        pitchStepper.value=myFunctions().getUserDefaultDouble(str: "pitchLimit", ret:30)
        rollStepper.value=myFunctions().getUserDefaultDouble(str: "rollLimit", ret:30)
        yawStepper.value=myFunctions().getUserDefaultDouble(str: "yawLimit", ret:30)
        pitchStepper2.value=myFunctions().getUserDefaultDouble(str: "pitchLimit2", ret:2.0)
        rollStepper2.value=myFunctions().getUserDefaultDouble(str: "rollLimit2", ret:2.0)
        yawStepper2.value=myFunctions().getUserDefaultDouble(str: "yawLimit2", ret:2.0)
 */
    private void loadData() {
        SharedPreferences data = getSharedPreferences("Data", MODE_PRIVATE);
        pitchDegree = data.getFloat("pitchDegree",30);
        pitchSec = data.getFloat("pitchSec", 2F);
        rollDegree = data.getFloat("rollDegree",30);
        rollSec = data.getFloat("rollSec",2F);
        yawDegree = data.getFloat("yawDegree",30);
        yawSec = data.getFloat("yawSec", 2F);
        soundType = data.getInt("soundType",1);
        vibrationType = data.getInt("vibrationType",1);

        pitchDegreeE.setText(String.format("%.0fd",pitchDegree));
        pitchSecE.setText(String.format("%.1fs",pitchSec));
        rollDegreeE.setText(String.format("%.0fd",rollDegree));
        rollSecE.setText(String.format("%.1fs", rollSec));
        yawDegreeE.setText(String.format("%.0fd",yawDegree));
        yawSecE.setText(String.format("%.1fs",yawSec));
        soundSpin.setSelection(soundType);
        vibrationSpin.setSelection(vibrationType);

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
    private int yaw180cnt = 0;//180°or -180°を越えた回数
    private float theLastYaw = 0;
    private float getYaw(Float y) {
        if (yawA.size()==0) {
            theLastYaw = y;
            return y;
        }
        else {
            if (theLastYaw > 100 && y < -100){
                yaw180cnt += 1;
            }
            else if (theLastYaw < -100 && y>100){
                yaw180cnt -= 1;
            }
            theLastYaw = y;
            float tmp=yaw180cnt*360F;
            return (y + tmp);
        }
    }

    int getDirection(float a, float b, float c, float d)
    {
        if ((a+1 < b) && (b+1 < c) && (c+1 < d)){  return 1;}
        else if ((a > b+1) && (b > c+1) && (c > d+1)){return -1;}
        else {return 0;}
    }

    float RAD_TO_DEG=180F/3.1415F;
    void QuaternionToEuler(float q0, float q1,float q2, float q3) {
        float pitch,roll,yaw;
        pitch = (float) Math.asin(-2 * q1 * q3 + 2 * q0 * q2);    // pitch
        roll = (float) Math.atan2(2 * q2 * q3 + 2 * q0 * q1, -2 * q1 * q1 - 2 * q2 * q2 + 1);
        yaw = (float) Math.atan2(2 * (q1 * q2 + q0 * q3), q0 * q0 + q1 * q1 - q2 * q2 - q3 * q3);

        pitch *= RAD_TO_DEG;
        yaw *= RAD_TO_DEG;
        // Declination of SparkFun Electronics (40°05'26.6"N 105°11'05.9"W) is
        //     8° 30' E  ± 0° 21' (or 8.5°) on 2016-07-19
        // - http://www.ngdc.noaa.gov/geomag-web/#declination
        //    yawf -= 8.5;
        roll *= RAD_TO_DEG;
        pitchA.add(pitch);//(Kalman(value: pitch, num: 0));
        rollA.add(roll);//(Kalman(value: roll, num: 1));
        float yawtmp=getYaw(yaw);
        yawA.add(yawtmp);//(Kalman(value: yawtmp, num: 2))
//        rollSecE.setText(String.format("%.1fs", rollSec));
        pitchCurrentE.setText(String.format("%.0f",pitch));//);pitchText1.text=Int(pitch).description
        rollCurrentE.setText(String.format("%.0f",roll));//);pitchText1.text=Int(pitch).description
        yawCurrentE.setText(String.format("%.0f",yawtmp));//);pitchText1.text=Int(pitch).description
//        pitch = int(pitchf);
//        roll = int(rollf);
//        yaw = int(yawf);
    }
//    func checkOK( d0:Float,d1:Float,limit:Float,count:Int,ms:Double)->Int

private int checkOK( float d0,float d1,float limit,int count,float ms)
    {
        float d = d0 - d1;
        if (count < 5){return 0;}//5*40ms
        if (d > limit || d < -limit)
        {
            //int i = (count) * 40;
            //long tmp = count.longValue();
            //long tmp=Float(count);

//            print("pitch:",count*40,Int(ms*1000))
            if (count*40F < ms*1000F){
                return 5;
            }
            else{
                return 0;
            }
            //if (count < 50){return 5} // 30度以上１秒以内 25*40ms
            //else {
            //    return 0
            //}
        }
        return 0;
    }

    public void onSensorChanged(SensorEvent event) {
        float[] floats = new float[12];
        //floats[0]:header
        //floats[1-7]:damy
        millis = TimeUnit.MILLISECONDS.ordinal();
        float nq0=floats[8] = (float) event.values[3];
        float nq1=floats[9] = (float) event.values[0];
        float nq2=floats[10] = (float) event.values[1];
        float nq3=floats[11] = (float) event.values[2];
        int b0=(int)((nq0+1.0)*128.0);
        int b1=(int)((nq1+1.0)*128.0);
        int b2=(int)((nq2+1.0)*128.0);
        int b3=(int)((nq3+1.0)*128.0);
        String Str = String.format ("Q:%03d%03d%03d%03d",b0,b1,b2,b3);
        //  send(StringStr, String "192.168.0.209", int 1108) throws IOException {
        byte[] dataStr = Str.getBytes(StandardCharsets.UTF_8);
        //      ipad = "192.168.0.209";
        //   InetSocketAddress inetSocketAddress = new InetSocketAddress(ipad, portn);
        DatagramPacket datagramPacket = new DatagramPacket(dataStr, dataStr.length, inetSocketAddress);

        // Androidではソケット通信は別スレッドにする必要がある。ここで非同期通信。
        AsyncTask<DatagramPacket, Void, Void> task = new AsyncTask<DatagramPacket, Void, Void>() {
            @Override
            protected Void doInBackground(DatagramPacket... datagramPackets) {
                DatagramSocket datagramSocket = null;
                try {
                    datagramSocket = new DatagramSocket();
                    datagramSocket.send(datagramPackets[0]);
                    datagramSocket.close();
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return null;
            }
        };
        if (rehaF==true){
            QuaternionToEuler(nq0,nq1,nq2,nq3);// q1: Float(quat.y), q2: Float(quat.x), q3: Float(quat.w))
 //           pitchCurrentE.setText(String.format("%.2f",nq0));//);pitchText1.text=Int(pitch).description
 //           rollCurrentE.setText(String.format("%.2f",nq1));//);pitchText1.text=Int(pitch).description
 //           yawCurrentE.setText(String.format("%.2f",nq2));//
            checkRotation();
        }
       // if UDPf==true{
       //     send(dataUTF8!)
       // }

        task.execute(datagramPacket);
    }
    int pitchDirection = 0;
    int rollDirection = 0;
    int yawDirection = 0;
    float lastPitch = 0F;
    int lastPitchCount = 0;
    float lastRoll = 0F;
    int lastRollCount = 0;
    float lastYaw = 0F;
    int lastYawCount = 0;
    void incPitchOK(){
        String str = pitchCountE.getText().toString();
        int t = parseInt(str);
        t += 1;
        pitchCountE.setText(String.valueOf(t));
    }


    void incRollOK(){
        String str = rollCountE.getText().toString();
        int t = parseInt(str);
        t += 1;
        rollCountE.setText(String.valueOf(t));
    }
    void incYawOK(){
        String str = yawCountE.getText().toString();
        int t = parseInt(str);
        t += 1;
        yawCountE.setText(String.valueOf(t));
    }
void soundANDvibe(){
        if(soundType==1) {
            soundPool.play(sound1, 1.0F, 1.0F, 1, 0, 1);
        }else if(soundType==2){
            soundPool.play(sound2, 1.0F, 1.0F, 1, 0, 1);
        }else if(soundType==3){
            soundPool.play(sound3, 1.0F, 1.0F, 1, 0, 1);
        }
        long ms = 0;
         if(vibrationType==1)ms=50;
        else if(vibrationType==2)ms=100;
        else if(vibrationType==3)ms=150;
// Vibrate for 500 milliseconds
    if(ms!=0) {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            //deprecated in API 26
            v.vibrate(ms);
        }
    }
    /*val vibrationEffect = VibrationEffect.createOneShot(300, DEFAULT_AMPLITUDE)
    vibrator.vibrate(vibrationEffect)
    vibrationEffect = VibrationEffect.createWaveform(longArrayOf(300, 300), intArrayOf(0, DEFAULT_AMPLITUDE), -1)
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        val vibrationEffect = VibrationEffect.createOneShot(300, VDEFAULT_AMPLITUDE)
        vibrator.vibrate(vibrationEffect)
    } else {
        vibrator.vibrate(300);*/
}
    private void checkRotation()
    {
        int tempDirection=0;
        int cnt=pitchA.size() -1;
        if(cnt<5){return;}
        // pitch
        tempDirection = getDirection(pitchA.get(cnt-3),pitchA.get(cnt-2),pitchA.get(cnt-1),pitchA.get(cnt));
        if((tempDirection == -1 && pitchDirection == 1)||(tempDirection == 1 && pitchDirection == -1))//向きが代わった時
        {
            pitchDirection = tempDirection;  //向きを新しくする
            if(checkOK(lastPitch,pitchA.get(cnt-3),pitchDegree, cnt-3 - lastPitchCount,pitchSec) == 5)
            {
                incPitchOK();
                soundANDvibe();
//                AudioServicesPlaySystemSound(1519)
                //              print("o:",lastPitch-pitchA[cnt-3],cnt-3-lastPitchCount)
            }
            lastPitch = pitchA.get(cnt-3);
            lastPitchCount = cnt-3;
        }
        if (tempDirection != 0){pitchDirection = tempDirection;}

        // roll
        tempDirection = getDirection(rollA.get(cnt - 3),rollA.get(cnt - 2),rollA.get(cnt - 1),rollA.get(cnt));
        if ((tempDirection == -1 && rollDirection == 1)||(tempDirection == 1 && rollDirection == -1))
        {
            rollDirection = tempDirection;
            if (checkOK(lastRoll,rollA.get(cnt - 3),rollDegree, cnt - 3 - lastRollCount,rollSec) == 5)
            {
                incRollOK();
                soundANDvibe();
//                AudioServicesPlaySystemSound(1103)//1519)
            }
            lastRoll = rollA.get(cnt-3);
            lastRollCount = cnt-3;
        }
        if (tempDirection != 0){rollDirection = tempDirection;}

        // yaw
        tempDirection = getDirection(yawA.get(cnt-3), yawA.get(cnt - 2),yawA.get(cnt - 1),yawA.get(cnt));
        if ((tempDirection == -1 && yawDirection == 1)||(tempDirection == 1 && yawDirection == -1))
        {
            yawDirection = tempDirection;
            if (checkOK(lastYaw,yawA.get(cnt-3),yawDegree, cnt-3 - lastYawCount,yawSec) == 5)
            {
                incYawOK();
                soundANDvibe();
//                AudioServicesPlaySystemSound(1519)
            }
            lastYaw = yawA.get(cnt-3);
            lastYawCount = cnt-3;
        }
        if (tempDirection != 0){yawDirection = tempDirection;}
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
        if (hasFocus) hideSystemUI();
    }

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
        );
    }

}