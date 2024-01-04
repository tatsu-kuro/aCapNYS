package com.kuroda33.acapnys;
//import static androidx.transition.GhostViewHolder.getHolder;

import static java.lang.Math.PI;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements SensorEventListener,SurfaceHolder.Callback{
    SensorManager sma;
    private final String TAG = "MainActivity";
    //SerialPort Service UUID (SPP)
    private static final UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private static final int CHECK_PERMISSION = 1001;
    private String TargetMACAddress = "No device is connected";
    private BluetoothAdapter mBtAdapter; //BTアダプタ
    BluetoothDevice mBtDevice;//BTデバイス
    private BluetoothSocket mBtSocket;//BTソケット
    private OutputStream mOutput;//出力ストリーム
    private Intent enableBtIntent;
    private ActivityResultLauncher<Intent> launcher;
    Handler mHandler = new Handler(Looper.getMainLooper());
    Runnable mRunnable;
    boolean CapNYS=false ;
    private AlertDialog.Builder mAlertDialog;
    private TextView quaterView;
    private Button Selbtn;//ボタンselectMacAddress
  //  @RequiresApi(api = Build.VERSION_CODES.M)
  // SurfaceHolderクラスのmHolderというメンバ変数の宣言
  // 一時的に画面を格納するためのホルダーだと思っておけば良い
    private SurfaceView mSurfaceView;
    //SurfaceHolder
    private SurfaceHolder mHolder;
//    int mSurfaceWidth;      // surfaceViewの幅
//    int mSurfaceHeight;     // surfaceViewの高さ
    private boolean mIsDrawing;
    //画?
    private Paint mPaint,mPaint2;
    //路径
    private Path mPath;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);

        Log.i(TAG, "onCreate 0");
        quaterView = (TextView) findViewById(R.id.quaternionData);
        quaterView.setMovementMethod(new ScrollingMovementMethod());
        Selbtn = (Button) findViewById(R.id.buttonSelect);

        mAlertDialog = new AlertDialog.Builder(this);
        mAlertDialog.setTitle("Alert");
        mAlertDialog.setPositiveButton("OK", null);


        Selbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                getTargetAddress();
                prepareSerialCommunication();
            }
        });

        enableBtIntent = new Intent( BluetoothAdapter.ACTION_REQUEST_ENABLE );
        launcher= registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.i(TAG,"result");
                    if (result.getResultCode() == Activity.RESULT_CANCELED) {
                        Toast.makeText(this, "Bluetooth usage was not allowed.", Toast.LENGTH_LONG).show();
                        mAlertDialog.setMessage("Bluetoothの利用を拒否されました。これ以上何もできないので，アプリを終了してください。");
                        mAlertDialog.show();
                        //finish();    // アプリ終了宣言
                    } else {
                        Log.i(TAG, "onActivityResult() Bluetooth function is available.");
                    }
                });

        //permissionをチェックし得られていなかったら取得要求
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN}, CHECK_PERMISSION);
                return;
            }
        }
        launcher.launch(enableBtIntent);
        sma = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        sma.registerListener(this, sma.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_FASTEST);
        set_rpk_ppk();
  //      Log.d("k*************",String.valueOf(ppk1.length));
        Log.i("k+++++++++++++",String.valueOf(facePoints2.length));
    //    cq0 = nq0; cq3 = -nq3;
    }
    private void prepareSerialCommunication() {
        cq0 = nq0; cq3 = -nq3;
        //交信先が指定されたBTデバイスのインスタンスを取得
        CapNYS=true;
        try {
            mBtDevice = mBtAdapter.getRemoteDevice(TargetMACAddress);
        } catch (Exception e) {
            mAlertDialog.setMessage("通信相手の取得に失敗しました。これ以上何もできないので，アプリを終了してください。");
            mAlertDialog.show();
            return;
        }
        // BTソケットのインスタンスを取得
        // 接続に使用するプロファイルを指定
        try {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }
            mBtSocket = mBtDevice.createRfcommSocketToServiceRecord(BT_UUID);
            mBtSocket.connect();
        } catch (IOException e) {
            e.printStackTrace();
            mAlertDialog.setMessage("ソケットへの接続が出来ませんでした。\n"
                    + "CapNYSを起動し、通信が行えるように設定した後に、本アプリを起動してください。");
            mAlertDialog.show();
            Selbtn.setEnabled(true);
            //      btnCon.setEnabled(false);
            CapNYS=false;
            return;
        }

        Log.i(TAG, "connect socket");
        //ソケットを接続する
        try {
            mOutput = mBtSocket.getOutputStream();//出力ストリームオブジェクト
        } catch (IOException e) {
            e.printStackTrace();
        }
        Selbtn.setEnabled(false);
    }

    //permissinの取得要求の結果の取得
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                mAlertDialog.setMessage("権限が許可されませんでした。これ以上何もできないので，アプリを終了してください。");
                mAlertDialog.show();
                return;
            }
        }
        launcher.launch(enableBtIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBtSocket != null) {
            try {
                mBtSocket.close();
            } catch (IOException connectException) {/*ignore*/}
            mBtSocket = null;
        }
        mHandler.removeCallbacks(mRunnable);
    }

    private String selectedDevice = "";
    private void getTargetAddress() {
        cq0 = nq0; cq3 = -nq3;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }
        //BTアダプタのインスタンスを取得
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBtAdapter = bluetoothManager.getAdapter();
        Log.i(TAG,"getTargetAddress() mBtAdapter==null?" + (mBtAdapter==null));
        //交信先の候補の取得（ペアリングリスト中から）
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        int number = pairedDevices.size();
        String[] deviceAddressList = new String[number];
        String[] deviceSelectionList = new String[number];
        if (pairedDevices.size() > 1) {
            int i = 0;
            for (BluetoothDevice device : pairedDevices) {
                deviceSelectionList[i] = device.getName() + " " + device.getAddress();
                deviceAddressList[i] = device.getAddress();
                Log.i(TAG, "getTargetAddress() paired Device " + deviceSelectionList[i]);
                i++;
            }
            TargetMACAddress = deviceAddressList[0];
            selectedDevice = deviceSelectionList[0];
        } else {
            Log.i(TAG,"getTargetAddress() cant found any devices");
            return;
        }

        new android.app.AlertDialog.Builder(MainActivity.this)
                .setTitle("Select Bluetooth Device")
                .setSingleChoiceItems(deviceSelectionList, 0, (dialog, item) -> {
                    //アイテムを選択したらここに入る
                    TargetMACAddress = deviceAddressList[item];
                    selectedDevice = deviceSelectionList[item];
                })
                .setPositiveButton("Select", (dialog, id) -> {
                    //Selectを押したらここに入る
                    if (CapNYS==true ) {
                        Log.i(TAG, "selectDevice() Selected Device " + selectedDevice);
                        Selbtn.setText(selectedDevice);
                        Selbtn.setEnabled(false);
                    }
                })
                .setNegativeButton("Cancel", (dialog, id) -> {
                    //Cancelを押したらここに入る
                })
                .show();
    }

    float mnq0,mnq1,mnq2,mnq3,a0,a1,a2,a3;
    float cq0 = 0.99F, cq1 = 0.0F, cq2 = 0.0F, cq3 = 0.0F;//spaceを押した時のcenter quatrnion
    float nq0 = 0.01F, nq1 = 0.0F, nq2 = 0.0F, nq3 = 0.0F;//現在のquaternion

    private void MultQuat(float q0, float q1, float q2, float q3, float p0, float p1, float p2, float p3) {
        a0 = q0 * p0 - q1 * p1 - q2 * p2 - q3 * p3;
        a1 = q1 * p0 + q0 * p1 - q3 * p2 + q2 * p3;
        a2 = q2 * p0 + q3 * p1 + q0 * p2 - q1 * p3;
        a3 = q3 * p0 - q2 * p1 + q1 * p2 + q0 * p3;
    }
    String[] sxyz = { "1+,3-,2+","2+,3-,1-","2-,3-,1+", "1-,3-,2-", "1-,3+,2+","2-,3+,1-","2+,3+,1+","1+,3+,2-"};
    private void QuatXchan(float q0,float q1, float q2, float q3)
    {
        String Sxyz;
        Sxyz = sxyz[6];
        float tx, ty, tz;
        if (Sxyz.charAt(0) == '1')tx = q1;
	    else if (Sxyz.charAt(0) == '2')tx = q2;
	    else tx = q3;
        if (Sxyz.charAt(1) == '-')tx = -tx;

        if (Sxyz.charAt(3) == '1')ty = q1;
	    else if (Sxyz.charAt(3) == '2')ty = q2;
	    else ty = q3;
        if (Sxyz.charAt(4) == '-')ty = -ty;

        if (Sxyz.charAt(6) == '1')tz = q1;
	    else if (Sxyz.charAt(6) == '2')tz = q2;
	    else tz = q3;
        if (Sxyz.charAt(7) == '-')tz = -tz;
        mnq0 = q0;
	    mnq1 = tx;
	    mnq2 = ty;
	    mnq3 = tz;
    }
    public void onSensorChanged(SensorEvent event) {
        float[] floats = new float[12];
        //floats[0]:header
        //floats[1-7]:damy
        nq0=floats[8] = (float) event.values[3];
        nq1=floats[9] = (float) event.values[0];
        nq2=floats[10] = (float) event.values[1];
        nq3=floats[11] = (float) event.values[2];

        MultQuat(cq0, cq1, cq2, cq3, nq0, nq1, nq2, nq3);//set a0~a3
        QuatXchan(a0,a1, a2, a3);//set mnq0~mnq3

        String str = "x=" + event.values[0] + "\n" + "y=" + event.values[1] + "\n" + "z=" + event.values[2] + "\n" + "w=" + event.values[3];
        quaterView.setText(str);
        if (CapNYS == true) {
            try {
                mOutput.write(new byte[] {(byte)1,(byte)0,(byte)0,(byte)0});
                for (int i = 1; i < floats.length; i++) {
                    int intBits = Float.floatToIntBits(floats[i]);
                    byte[] outBytes = {(byte)(intBits), (byte)(intBits>>8), (byte)(intBits>>16), (byte)(intBits>>24)};
                    mOutput.write(outBytes);
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                mAlertDialog.setMessage("メッセージHelloを送信することが出来ませんでした。");
                mAlertDialog.show();
                e.printStackTrace();
                CapNYS = false;
                Selbtn.setEnabled(true);
                Selbtn.setText("Select CapNYS-PC");
            }
        }
    }

    // センサーの精度が変更されると呼ばれる
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated...");
        if (mHolder == null) {
            mHolder = mSurfaceView.getHolder();
            mHolder.addCallback(this);
        }

        mPath = new Path();
        //初始化画?
        mPaint = new Paint();
        mPaint2 = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(6);
        mPaint.setAntiAlias(false);
        mPaint.setColor(Color.BLACK);
        //mPaint2.setStyle(Paint.Style.STROKE);
        mPaint2.setStrokeWidth(6);
        mPaint2.setAntiAlias(false);
        mPaint2.setColor(Color.LTGRAY);
        mSurfaceView.setFocusable(true);
        mSurfaceView.setFocusableInTouchMode(true);
        mSurfaceView.setKeepScreenOn(true);

        mIsDrawing = true;
        new Thread(runnable).start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged...");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed...");
        mIsDrawing = false;
        mHolder = null;
    }

    private void draw() {
        if (mHolder != null) {
            Canvas canvas = null;
            try{
                //用于??的Canvas, ?定画布并返回画布?象
                canvas = mHolder.lockCanvas();
                //接下去就是在画布上?行一下draw
                canvas.drawColor(Color.WHITE);
              //  Paint paint = new Paint();
               // paint.setColor(Color.argb(255, 255, 255, 255));

                // x=40, y=40 半径 20 の円を描画
               // paint.setAntiAlias(false);
            //    canvas.drawCircle(40.5f, 40.5f, 20.0f, mPaint2);
            //    canvas.drawARGB(255, 55, 55, 55);
                // 画?
                canvas.drawCircle(canvas.getWidth() / 2f, canvas.getHeight() / 2f, canvas.getHeight()/3f, mPaint2);
                drawHead(canvas.getWidth(),canvas.getHeight(),canvas.getHeight()/3f,mnq0,mnq1,mnq2,mnq3);
                canvas.drawPath(mPath,mPaint);
            } catch (Exception e){

            } finally {
                //当画布内容不?空?，才post，避免出?黑屏的情况。
                if(canvas !=null && mHolder != null)
                    mHolder.unlockCanvasAndPost(canvas);
            }
        }
    }

    final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            long start =System.currentTimeMillis();
            while(mIsDrawing){
                mPath.reset();
                draw();
                Log.d(TAG,"drawing??????");
                long end = System.currentTimeMillis();
                if(end-start<100){
                    try{
                        Thread.sleep(100-end+start);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x=(int) event.getX();
        int y= (int) event.getY();
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                cq0 = nq0; cq3 = -nq3;
                Log.d(TAG, "onTouchEvent: down11");
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
   // int cameraType = 1;
   // float[] rpk1 = new float[600];
    float[][] rpk12 = new float[600][3];
    //float[] ppk1 = new float[600];
    float[][] ppk12 = new float[600][3];

    int[][] facePoints2 = {//x1,y1,0, x2,y2,0, x3,y3,1, x4,y4,0  の並びは   MoveTo(x1,y1)  LineTo(x2,y2)  LineTo(x3,y3)  MoveTo(x4,y4) と描画される
            {0,0,0}, {15,0,0}, {30,0,0}, {45,0,0}, {60,0,0}, {75,0,0}, {90,0,0}, {105,0,0}, {120,0,0}, {135,0,0}, {150,0,0}, {165,0,0},//horizon 12
            {180,0,0}, {195,0,0}, {210,0,0}, {225,0,0}, {240,0,0}, {255,0,0}, {270,0,0}, {285,0,0}, {300,0,0}, {315,0,0}, {330,0,0}, {345,0,0}, {360,0,1},//horizon 12+13=25
            {0,0,0}, {0,15,0}, {0,30,0}, {0,45,0}, {0,60,0}, {0,75,0}, {0,90,0}, {0,105,0}, {0,120,0}, {0,135,0}, {0,150,0}, {0,165,0},//vertical 25+12
            {0,180,0}, {0,195,0}, {0,210,0}, {0,225,0}, {0,240,0}, {0,255,0}, {0,270,0}, {0,285,0}, {0,300,0}, {0,315,0}, {0,330,0}, {0,345,0}, {0,360,1},//virtical 37+13=50
            {0,90,0}, {15,90,0}, {30,90,0}, {45,90,0}, {60,90,0}, {75,90,0}, {90,90,0}, {105,90,0}, {120,90,0}, {135,90,0}, {150,90,0}, {165,90,0},//coronal 50+12=62
            {180,90,0}, {195,90,0}, {210,90,0}, {225,90,0}, {240,90,0}, {255,90,0}, {270,90,0}, {285,90,0}, {300,90,0}, {315,90,0}, {330,90,0}, {345,90,90}, {360,90,1},//coronal 62+13=75
            {20,-90,0}, {20,-105,0}, {20,-120,0}, {20,-135,0}, {20,-150,0}, {20,-165,0}, {20,-180,1},//hair 75+7=82
            {-20,-90,0}, {-20,-105,0}, {-20,-120,0}, {-20,-135,0}, {-20,-150,0}, {-20,-165,0}, {-20,-180,1},//hair 82+7=89
            {40,-90,0}, {40,-105,0}, {40,-120,0}, {40,-135,0}, {40,-150,0}, {40,-165,0}, {40,-180,1},//hair 89+7=96
            {-40,-90,0}, {-40,-105,0}, {-40,-120,0}, {-40,-135,0}, {-40,-150,0}, {-40,-165,0}, {-40,-180,1},//hair 96+7=103
            {23,-9,0}, {31,-12,0}, {38,-20,0}, {40,-31,0}, {38,-41,0}, {31,-46,0}, {23,-45,0}, {15,-39,0}, {10,-32,0}, {8,-23,0}, {10,-16,0}, {15,-10,0}, {23,-9,1},//eye +13
            {-23,-9,0}, {-31,-12,0}, {-38,-20,0}, {-40,-31,0}, {-38,-41,0}, {-31,-46,0}, {-23,-45,0}, {-15,-39,0}, {-10,-32,0}, {-8,-23,0}, {-10,-16,0}, {-15,-10,0}, {-23,-9,1},//eye +13
            {22,-26,0}, {23,-25,0}, {24,-24,1},//eye dots 3
            {-22,-26,0}, {-23,-25,0}, {-24,-24,1},//eye dots 3
            {-19,32,0}, {-14,31,0}, {-9,31,0}, {-4,31,0}, {0,30,0}, {4,31,0}, {9,31,0}, {14,31,0}, {19,32,1}, {1000,1000,1000}};//mouse 9
    float pi180 = (float) PI/180.0f;

    private void set_rpk_ppk() {
        float r = 40F;//hankei
        float dx,dy,dz;
           // convert draw data to radian
        for (int i = 0; facePoints2[i][0] != 1000; i++) {
            rpk12[i][0] = (float) facePoints2[i][0] * pi180;
            rpk12[i][1] = (float) facePoints2[i][1] * pi180;
        }

        // move (1,0,0) to each draw point
        for (int i = 0; facePoints2[i][0] != 1000; i++) {
            ppk12[i][0] = 0f;
            ppk12[i][1] = 1.0f * r;
            ppk12[i][2] = 0f;
        }

        // rotate all draw point based on draw data
        for (int i = 0; facePoints2[i][0] != 1000; i++) {
            //rotateX
            dy = ppk12[i][1]*(float)cos(rpk12[i][0]) - ppk12[i][2]*(float)sin(rpk12[i][0]);
            dz = ppk12[i][1]*(float)sin(rpk12[i][0]) + ppk12[i][2]*(float)cos(rpk12[i][0]);
            ppk12[i][1] = dy;
            ppk12[i][2] = dz;
            //rotateZ
            dx = ppk12[i][0]*(float)cos(rpk12[i][1]) - ppk12[i][1]*(float)sin(rpk12[i][1]);
            dy = ppk12[i][0]*(float)sin(rpk12[i][1]) + ppk12[i][1]*(float)cos(rpk12[i][1]);
            ppk12[i][0] = dx;
            ppk12[i][1] = dy;
            //rotateY
            dx = ppk12[i][0]*(float)cos(1.5707963) - ppk12[i][2]*(float)sin(1.5707963);
            dz = ppk12[i][0]*(float)sin(1.5707963) + ppk12[i][2]*(float)cos(1.5707963);
            ppk12[i][0] = dx;
            ppk12[i][2] = dz;
        }
    }
    //モーションセンサーをリセットするときに-1とする。リセット時に-1なら,角度から０か１をセット
    int degreeAtResetHead=0;//0:-90<&&<90 1:<-90||>90 -1:flag for get degree
    private void RotateQu(int i,float x0,float y0,float z0, float q0, float q1, float q2, float q3)
    {
      //  float ax, ay, az, norm, mag;

  /*      mag = (float)sqrt((q0 * q0) + (q1 * q1) + (q2 * q2) + (q3 * q3));
        if (mag>1.192092896e-07F){
            norm = 1 / mag;
            q0 *= norm;
            q1 *= norm;
            q2 *= norm;
            q3 *= norm;
        }*/

        ppk[i][0] = x0 * (q0 * q0 + q1 * q1 - q2 * q2 - q3 * q3) + y0 * (2f * (q1 * q2 - q0 * q3)) + z0 * (2f * (q1 * q3 + q0 * q2));
        ppk[i][1] = x0 * (2f * (q1 * q2 + q0 * q3)) + y0 * (q0 * q0 - q1 * q1 + q2 * q2 - q3 * q3) + z0 * (2f * (q2 * q3 - q0 * q1));
        ppk[i][2] = x0 * (2f * (q1 * q3 - q0 * q2)) + y0 * (2f * (q2 * q3 + q0 * q1)) + z0 * (q0 * q0 - q1 * q1 - q2 * q2 + q3 * q3);
    }
    float[][] ppk = new float[600][3];
  public void drawHead(float w, float h, float faceR, float qOld0, float qOld1, float qOld2, float qOld3){

      float faceX0 = w/2f;
      float faceY0 = h/2f;//center
  //    float faceR = r;//hankei
      float defaultRadius = 40.0f;
      //     let size = CGSize(width:w, height:h)
      for (int i = 0; facePoints2[i][0] != 1000; i++) {
          RotateQu(i,ppk12[i][0],ppk12[i][1],ppk12[i][2], qOld0, qOld1, qOld2, qOld3);
      }

      float uraPoint=faceR/50.0f;//この値の意味がよくわからなかった

      boolean endpointF=true;//終点でtrueとする
      if (degreeAtResetHead == 1){//iPhoneが >90||<-90 垂直以上に傾いた時
          for(int i=0;facePoints2[i][0]!=1000;i++){
              if (endpointF==true){//始点に移動する

                  if (ppk[i][1] < uraPoint){
                      endpointF=true;
                  }else{
                      endpointF=false;
                  }
                  mPath.moveTo(faceX0+ppk[i][0]*faceR/defaultRadius,faceY0+ppk[i][2]*faceR/defaultRadius);
              }else {
                  if (ppk[i][1] > uraPoint) {
                      if (ppk[i][1] > uraPoint){
                          mPath.lineTo(faceX0+ppk[i][0]*faceR/defaultRadius,faceY0+ppk[i][2]*faceR/defaultRadius);
                      }else{
                          mPath.moveTo(faceX0+ppk[i][0]*faceR/defaultRadius,faceY0+ppk[i][2]*faceR/defaultRadius);
                      }
                      if (facePoints2[i][2] == 1) {
                          endpointF = true;
                      }
                  }
              }
          }
      }else{//iPhoneが-90~+90の時
          for(int  i=0;facePoints2[i][0]!=1000;i++){
              if (endpointF==true){//始点に移動する

                  if (ppk[i][1] < uraPoint){
                      endpointF=true;
                  }else{
                      endpointF=false;
                  }
                  mPath.moveTo(faceX0-ppk[i][0]*faceR/defaultRadius,faceY0-ppk[i][2]*faceR/defaultRadius);
              }else{
                  if (ppk[i][1] > uraPoint){
                      mPath.lineTo(faceX0-ppk[i][0]*faceR/defaultRadius,faceY0-ppk[i][2]*faceR/defaultRadius);
                  }else{
                      mPath.moveTo(faceX0-ppk[i][0]*faceR/defaultRadius,faceY0-ppk[i][2]*faceR/defaultRadius);
                  }
                  if (facePoints2[i][2] == 1){
                      endpointF=true;
                  }
              }
          }
      }
  }
}