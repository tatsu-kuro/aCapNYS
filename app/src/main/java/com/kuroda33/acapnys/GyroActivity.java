package com.kuroda33.acapnys;

//import static androidx.transition.GhostViewHolder.getHolder;

import static android.app.PendingIntent.getActivity;
import static android.util.Log.i;
import static android.view.View.SYSTEM_UI_FLAG_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;
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
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import java.util.Set;
import java.util.UUID;

public class GyroActivity extends AppCompatActivity implements SensorEventListener{
    SensorManager sensorManager;
    private final String TAG = "MainActivity";
    InetSocketAddress inetSocketAddress = null;
    private AlertDialog.Builder mAlertDialog;
    private EditText ipe1,ipe2,ipe3,ipe4;
    private EditText pitchDegreeE,pitchSecE,rollDegreeE,rollSecE,yawDegreeE,yawSecE;
    private EditText pitchCurrentE,pitchCountE,rollCurrentE,rollCountE,yawCurrentE,yawCountE;
    private int pitchDegree,rollDegree,yawDegree;
    private double pitchSec,rollSec,yawSec;
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

    private int soundType;
    private int vibrationType;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gyro);


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
           //     i(TAG,ips1+ips2+ips3+ips4);
                Log.d(TAG,"onclickStart");
            }
        });
        rehaStopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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

    private void saveData() {
        SharedPreferences data = getSharedPreferences("Data", MODE_PRIVATE);
        SharedPreferences.Editor editor = data.edit();
        editor.putString("pitchDegree",String.valueOf(pitchDegree));
        editor.putString("pitchSec",String.valueOf(pitchSec));
        editor.putString("rollDegree",String.valueOf(rollDegree));
        editor.putString("rollSec",String.valueOf(rollSec));
        editor.putString("yawDegree",String.valueOf(yawDegree));
        editor.putString("yawSec",String.valueOf(yawSec));
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
        String pd = data.getString("pitchDegree","30");
        String ps = data.getString("pitchSec","2.0");
        String rd = data.getString("rollDegree","30");
        String rs = data.getString("rollSec","2.0");
        String yd = data.getString("yawDegree","30");
        String ys = data.getString("yawSec","2.0");
        // pitchText4.text=(round(pitchStepper2.value*10)/10).description + "s"
        pitchDegree= Integer.parseInt(pd);
        pitchSec=Double.parseDouble(ps);
        rollDegree=Integer.parseInt(rd);
        rollSec=Double.parseDouble(rs);
        yawDegree=Integer.parseInt(yd);
        yawSec=Double.parseDouble(ys);
        pitchDegreeE.setText(pd + "d");
        pitchSecE.setText(String.format("%.1fs",pitchSec));
        rollDegreeE.setText(rd + "d");
        rollSecE.setText(String.format("%.1fs", rollSec));
        yawDegreeE.setText(yd + "d");
        yawSecE.setText(String.format("%.1fs",yawSec));

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
        float[] floats = new float[12];
        //floats[0]:header
        //floats[1-7]:damy
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
        task.execute(datagramPacket);
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