package com.kuroda33.acapnys;
//import static androidx.transition.GhostViewHolder.getHolder;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;

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
import android.graphics.PixelFormat;
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
    private SensorManager sma;
    private final String TAG = "MainActivity";
    //SerialPort Service UUID (SPP)
    private static final UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private static final int CHECK_PERMISSION = 1001;
    private String TargetMACAddress = "No device is connected";
    private BluetoothAdapter mBtAdapter; //BTアダプタ
    private BluetoothDevice mBtDevice;//BTデバイス
    private BluetoothSocket mBtSocket;//BTソケット
    private OutputStream mOutput;//出力ストリーム
    private Intent enableBtIntent;
    private ActivityResultLauncher<Intent> launcher;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable mRunnable;
    boolean CapNYS=false ;
    private AlertDialog.Builder mAlertDialog;
    private TextView quaterView;
    private Button Selbtn;//ボタンselectMacAddress
  //  @RequiresApi(api = Build.VERSION_CODES.M)
  // SurfaceHolderクラスのmHolderというメンバ変数の宣言
  // 一時的に画面を格納するためのホルダーだと思っておけば良い
  SurfaceHolder mHolder;
    int mSurfaceWidth;      // surfaceViewの幅
    int mSurfaceHeight;     // surfaceViewの高さ

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 画面レイアウトで配置したsurfaceViewを取得し、メンバ変数mHolderへ格納
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        mHolder = surfaceView.getHolder();
        // surfaceHolderが変更・破棄された時のイベントリスナー
        mHolder.addCallback(this);

        // 背景画像を用意しているため、canvas自体は透明にする
        mHolder.setFormat(PixelFormat.TRANSLUCENT);
        surfaceView.setZOrderOnTop(true);

        //   getHolder().addCallback(this);
      //  val myView = DrawTest(this);
       // setContentView(myView);
     //   setContentView(new DrawTest(this));
        //   CountN = 0;
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


        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        ///輪郭と中身の両方を塗りつぶし

        Path path = new Path();
        path.moveTo(10.0f, 10.0f);
        ///始点を決める。
        path.lineTo(200.0f, 150.0f);
        path.lineTo(150.0f, 200.0f);
        path.close();
        ///パスを閉じる

      //  Canvas.drawPath(path, paint);


    }
    private void prepareSerialCommunication() {
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
    public void onSensorChanged(SensorEvent event) {
        byte[] bytes = new byte[6];
        float[] floats = new float[11];
        bytes[0] = 1;
        bytes[1] = 2;
        bytes[2] = (byte) (event.values[0] * 128);
        bytes[3] = (byte) (event.values[1] * 128);
        bytes[4] = (byte) (event.values[2] * 128);
        bytes[5] = (byte) (event.values[3] * 128);
        floats[0] = 1;
        floats[7] = event.values[0];
        floats[8] = event.values[1];
        floats[9] = event.values[2];
        floats[10] = event.values[3];

        String str = "x=" + event.values[0] + "\n" + "y=" + event.values[1] + "\n" + "z=" + event.values[2] + "\n" + "w=" + event.values[3];
        quaterView.setText(str);
    //    let quaterImage = drawHead(width: realWinHeight/2.5, height: realWinHeight/2.5, radius: realWinHeight/5-1,qOld0:qCG0, qOld1: qCG1, qOld2:qCG2,qOld3:qCG3)
     //   draw1();
        if (CapNYS == true) {

            try {
                //mOutput.write(str.getBytes(StandardCharsets.UTF_8));
                mOutput.write(bytes);
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
    public void surfaceCreated(SurfaceHolder holder) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.FILL);

        Canvas canvas = holder.lockCanvas();
        canvas.drawColor(Color.BLACK);
        canvas.drawCircle(100, 100, 50, paint);
        holder.unlockCanvasAndPost(canvas);
        onDraw(canvas);
    }
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
    public void surfaceDestroyed(SurfaceHolder holder) {}
    // Viewをextendsしたクラスを作成し描画処理をする
    //static public class DrawTest extends View {

      //  public DrawTest(Context context) {
        //    super(context);
        //}

        // 描画処理を記述
      //  @Override
        public void onDraw(Canvas canvas) {
            Paint paint = new Paint();

            // 黒の細い線
            paint.setColor(Color.argb(255, 0, 0, 0));
            canvas.drawLine(0, 0, 50, 50, paint);

            // 黒の中太の線
            paint.setStrokeWidth(5);
            float[] pts = {50, 50, 100, 100};
            canvas.drawLines(pts, paint);

            // 青の太い線
            paint.setColor(Color.BLUE);
            paint.setStrokeWidth(10);
            float[] pts2 = {100, 100, 150, 150};
            canvas.drawLines(pts2, paint);
        }
  //  }
  //  var rpk1 = Array(repeating: CGFloat(0), count:500)
    //var ppk1 = Array(repeating: CGFloat(0), count:500)//144*3
    int[] facePoints = {//x1,y1,0, x2,y2,0, x3,y3,1, x4,y4,0  の並びは   MoveTo(x1,y1)  LineTo(x2,y2)  LineTo(x3,y3)  MoveTo(x4,y4) と描画される
            0,0,0, 15,0,0, 30,0,0, 45,0,0, 60,0,0, 75,0,0, 90,0,0, 105,0,0, 120,0,0, 135,0,0, 150,0,0, 165,0,0,//horizon 12
            180,0,0, 195,0,0, 210,0,0, 225,0,0, 240,0,0, 255,0,0, 270,0,0, 285,0,0, 300,0,0, 315,0,0, 330,0,0, 345,0,0, 360,0,1,//horizon 12+13=25
            0,0,0, 0,15,0, 0,30,0, 0,45,0, 0,60,0, 0,75,0, 0,90,0, 0,105,0, 0,120,0, 0,135,0, 0,150,0, 0,165,0,//vertical 25+12
            0,180,0, 0,195,0, 0,210,0, 0,225,0, 0,240,0, 0,255,0, 0,270,0, 0,285,0, 0,300,0, 0,315,0, 0,330,0, 0,345,0, 0,360,1,//virtical 37+13=50
            0,90,0, 15,90,0, 30,90,0, 45,90,0, 60,90,0, 75,90,0, 90,90,0, 105,90,0, 120,90,0, 135,90,0, 150,90,0, 165,90,0,//coronal 50+12=62
            180,90,0, 195,90,0, 210,90,0, 225,90,0, 240,90,0, 255,90,0, 270,90,0, 285,90,0, 300,90,0, 315,90,0, 330,90,0, 345,90,90, 360,90,1,//coronal 62+13=75
            20,-90,0, 20,-105,0, 20,-120,0, 20,-135,0, 20,-150,0, 20,-165,0, 20,-180,1,
            //hair 75+7=82
            -20,-90,0, -20,-105,0, -20,-120,0, -20,-135,0, -20,-150,0, -20,-165,0, -20,-180,1,//hair 82+7=89
            40,-90,0, 40,-105,0, 40,-120,0, 40,-135,0, 40,-150,0, 40,-165,0, 40,-180,1,
            //hair 89+7=96
            -40,-90,0, -40,-105,0, -40,-120,0, -40,-135,0, -40,-150,0, -40,-165,0, -40,-180,1,//hair 96+7=103
            23,-9,0, 31,-12,0, 38,-20,0, 40,-31,0, 38,-41,0, 31,-46,0, 23,-45,0, 15,-39,0, 10,-32,0, 8,-23,0, 10,-16,0, 15,-10,0, 23,-9,1,//eye +13
            -23,-9,0, -31,-12,0, -38,-20,0, -40,-31,0, -38,-41,0, -31,-46,0, -23,-45,0, -15,-39,0, -10,-32,0, -8,-23,0, -10,-16,0, -15,-10,0, -23,-9,1,//eye +13
            22,-26,0, 23,-25,0, 24,-24,1,//eye dots 3
            -22,-26,0, -23,-25,0, -24,-24,1,//eye dots 3
            -19,32,0, -14,31,0, -9,31,0, -4,31,0, 0,30,0, 4,31,0, 9,31,0, 14,31,0, 19,32,1};//mouse 9
  /*  func set_rpk_ppk() {
        let faceR:CGFloat = 40//hankei
        var frontBack:Int = 0
        let camera = 0//Int(camera.getUserDefaultInt(str: "cameraType", ret: 0))
        if camera == 0{//front camera
            frontBack = 180
        }
        // convert draw data to radian
        print("frontBack",frontBack)
        for i in 0..<facePoints.count/3 {
            rpk1[i*2] = CGFloat(facePoints[3 * i + 0]) * 0.01745329//pi/180
            rpk1[i*2+1] = CGFloat(facePoints[3 * i + 1]+frontBack) * 0.01745329//pi/180
        }
        // move (1,0,0) to each draw point
        for i in 0..<facePoints.count/3{
            ppk1[i*3] = 0
            ppk1[i*3+1] = faceR
            ppk1[i*3+2] = 0
        }
        // rotate all draw point based on draw data
        var dx,dy,dz:CGFloat
        for i in  0..<facePoints.count/3 {
            //rotateX
            dy = ppk1[i*3+1]*cos(rpk1[i*2]) - ppk1[i*3+2]*sin(rpk1[i*2])
            dz = ppk1[i*3+1]*sin(rpk1[i*2]) + ppk1[i*3+2]*cos(rpk1[i*2])
            ppk1[i*3+1] = dy;
            ppk1[i*3+2] = dz;
            //rotateZ
            dx = ppk1[i*3]*cos(rpk1[i*2+1])-ppk1[i*3+1]*sin(rpk1[i*2+1])
            dy = ppk1[i*3]*sin(rpk1[i*2+1]) + ppk1[i*3+1]*cos(rpk1[i*2+1])
            ppk1[i*3] = dx
            ppk1[i*3+1] = dy
            //rotateY
            dx =  ppk1[i*3] * cos(1.5707963) + ppk1[i*3+2] * sin(1.5707963)
            dz = -ppk1[i*3] * sin(1.5707963) + ppk1[i*3+2] * cos(1.5707963)
            ppk1[i*3]=dx
            ppk1[i*3+2]=dz
        }
    }
    //モーションセンサーをリセットするときに-1とする。リセット時に-1なら,角度から０か１をセット
    var degreeAtResetHead:Int=0//0:-90<&&<90 1:<-90||>90 -1:flag for get degree
    func drawHead(width w:CGFloat, height h:CGFloat, radius r:CGFloat, qOld0:CGFloat, qOld1:CGFloat, qOld2:CGFloat, qOld3:CGFloat)->UIImage{
//        print(String(format:"%.3f,%.3f,%.3f,%.3f",qOld0,qOld1,qOld2,qOld3))
        var ppk = Array(repeating: CGFloat(0), count:500)
        let faceX0:CGFloat = w/2;
        let faceY0:CGFloat = h/2;//center
        let faceR:CGFloat = r;//hankei
        let defaultRadius:CGFloat = 40.0
        let size = CGSize(width:w, height:h)

        // イメージ処理の開始
        for i in 0..<facePoints.count/3 {
            let x0:CGFloat = ppk1[i*3]
            let y0:CGFloat = ppk1[i*3+1]
            let z0:CGFloat = cameraType==0 ? -ppk1[i*3+2]:ppk1[i*3+2]
            var q0=qOld0
            var q1=qOld1
            var q2=qOld2
            var q3=qOld3
            var norm,mag:CGFloat!
                    mag = CGFloat(sqrt(q0*q0 + q1*q1 + q2*q2 + q3*q3))
            if mag>CGFloat(Float.ulpOfOne){
                norm = 1 / mag
                q0 *= norm
                q1 *= norm
                q2 *= norm
                q3 *= norm
            }
            ppk[i*3] = x0 * (q0 * q0 + q1 * q1 - q2 * q2 - q3 * q3) + y0 * (2 * (q1 * q2 - q0 * q3)) + z0 * (2 * (q1 * q3 + q0 * q2))
            ppk[i*3+1] = x0 * (2 * (q1 * q2 + q0 * q3)) + y0 * (q0 * q0 - q1 * q1 + q2 * q2 - q3 * q3) + z0 * (2 * (q2 * q3 - q0 * q1))
            ppk[i*3+2] = x0 * (2 * (q1 * q3 - q0 * q2)) + y0 * (2 * (q2 * q3 + q0 * q1)) + z0 * (q0 * q0 - q1 * q1 - q2 * q2 + q3 * q3)
        }
        // イメージ処理の開始
        UIGraphicsBeginImageContextWithOptions(size, false, 1.0)

        let drawPath = UIBezierPath(arcCenter: CGPoint(x: faceX0, y:faceY0), radius: faceR, startAngle: 0, endAngle: CGFloat(Double.pi)*2, clockwise: true)
        // 内側の色
        UIColor.white.setFill()
//        // 内側を塗りつぶす
        drawPath.fill()

        let uraPoint=faceR/40.0//この値の意味がよくわからなかった

        var endpointF=true//終点でtrueとする
        if degreeAtResetHead == 1{//iPhoneが >90||<-90 垂直以上に傾いた時
            for i in 0..<facePoints.count/3-1{
                if endpointF==true{//始点に移動する

                    if ppk[i*3+1] < uraPoint{
                        endpointF=true
                    }else{
                        endpointF=false
                    }
                    drawPath.move(to: CGPoint(x:faceX0+ppk[i*3]*faceR/defaultRadius,y:faceY0-ppk[i*3+2]*faceR/defaultRadius))
                }else{
                    if ppk[i*3+1] > uraPoint{
                        drawPath.addLine(to: CGPoint(x:faceX0+ppk[i*3]*faceR/defaultRadius,y:faceY0-ppk[i*3+2]*faceR/defaultRadius))
                    }else{
                        drawPath.move(to: CGPoint(x:faceX0+ppk[i*3]*faceR/defaultRadius,y:faceY0-ppk[i*3+2]*faceR/defaultRadius))
                    }
                    if facePoints[3*i+2] == 1{
                        endpointF=true
                    }
                }
            }
        }else{//iPhoneが-90~+90の時
            for i in 0..<facePoints.count/3-1{
                if endpointF==true{//始点に移動する

                    if ppk[i*3+1] < uraPoint{
                        endpointF=true
                    }else{
                        endpointF=false
                    }
                    drawPath.move(to: CGPoint(x:faceX0-ppk[i*3]*faceR/defaultRadius,y:faceY0+ppk[i*3+2]*faceR/defaultRadius))
                }else{
                    if ppk[i*3+1] > uraPoint{
                        drawPath.addLine(to: CGPoint(x:faceX0-ppk[i*3]*faceR/defaultRadius,y:faceY0+ppk[i*3+2]*faceR/defaultRadius))
                    }else{
                        drawPath.move(to: CGPoint(x:faceX0-ppk[i*3]*faceR/defaultRadius,y:faceY0+ppk[i*3+2]*faceR/defaultRadius))
                    }
                    if facePoints[3*i+2] == 1{
                        endpointF=true
                    }
                }
            }
        }
        // 線の色
        UIColor.black.setStroke()
        drawPath.lineWidth = 2.0//1.0
        // 線を描く
        drawPath.stroke()
        // イメージコンテキストからUIImageを作る
        let image = UIGraphicsGetImageFromCurrentImageContext()
        // イメージ処理の終了
        UIGraphicsEndImageContext()
        return image!
    }*/
}