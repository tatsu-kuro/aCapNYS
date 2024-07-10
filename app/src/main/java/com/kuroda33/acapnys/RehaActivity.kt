package com.kuroda33.acapnys

//import android.hardware.Sensor
//import android.hardware.SensorManager
//import com.kuroda33.acapnys.GyroActivity.soundSpinnerSelectedListener
//import com.kuroda33.acapnys.GyroActivity.vibrationSpinnerSelectedListener

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.SoundPool
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.preference.PreferenceManager
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.kuroda33.acapnys.databinding.ActivityRehaBinding
import com.kuroda33.acapnys.databinding.ActivityRehaBinding.*
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.SocketException
import java.nio.charset.StandardCharsets


//import java.util.prefs.Preferences

class RehaActivity : AppCompatActivity() , SensorEventListener {
    private lateinit var sensorManager: SensorManager
  //  var sensorManager: SensorManager? = null
    private var soundPool: SoundPool? = null
    private var sound1 = 0
    private var sound2:kotlin.Int = 0
    private var sound3:kotlin.Int = 0

    //  long millis0 = TimeUnit.MILLISECONDS.ordinal();
    private val TAG = "MainActivity"
    var inetSocketAddress: InetSocketAddress? = null

    //  private AlertDialog.Builder mAlertDialog;
    private var ipe1: EditText? = null //  private AlertDialog.Builder mAlertDialog;
    private var ipe2: EditText? = null //  private AlertDialog.Builder mAlertDialog;
    private var ipe3: EditText? = null //  private AlertDialog.Builder mAlertDialog;
    private var ipe4: EditText? = null
    private var pitchDegreeE: EditText? = null
    private var pitchSecE:EditText? = null
    private var rollDegreeE:EditText? = null
    private var rollSecE:EditText? = null
    private var yawDegreeE:EditText? = null
    private var yawSecE:EditText? = null
    private var pitchCurrentE: EditText? = null
    private var pitchCountE:EditText? = null
    private var rollCurrentE:EditText? = null
    private var rollCountE:EditText? = null
    private var yawCurrentE:EditText? = null
    private var yawCountE:EditText? = null
    private var pitchDegree = 0f
    private var rollDegree:kotlin.Float = 0f
    private var yawDegree:kotlin.Float = 0f
    var pitchSec = 0f
    var rollSec:kotlin.Float = 0f
    var yawSec:kotlin.Float = 0f
    private var ipSetBtn: Button? = null
    private var exitBtn: Button? = null
    private var rehaStartBtn: Button? = null
    private var rehaStopBtn: Button? = null
    private var rehaResetBtn: Button? = null
    private var pitchDegreeUpBtn: Button? = null
    private var pitchDegreeDownBtn: Button? = null
    private var pitchSecUpBtn: Button? = null
    private var pitchSecDownBtn: Button? = null

    private var rollDegreeUpBtn: Button? = null
    private var rollDegreeDownBtn: Button? = null
    private var rollSecUpBtn: Button? = null
    private var rollSecDownBtn: Button? = null

    private var yawDegreeUpBtn: Button? = null
    private var yawDegreeDownBtn: Button? = null
    private var yawSecUpBtn: Button? = null
    private var yawSecDownBtn: Button? = null
    private var soundSpin: Spinner? = null
    private var vibrationSpin:Spinner? = null

    var soundType = 0
    var vibrationType = 0
    private var rehaF = true

    var pitchs = floatArrayOf(0f, 0f, 0f, 0f, 0f)
    var rolls = floatArrayOf(0f, 0f, 0f, 0f, 0f)
    var yaws = floatArrayOf(0f, 0f, 0f, 0f, 0f)
    //var arr1 = arrayOf(8)(float)
    var kalVs = arrayOf(
        floatArrayOf(0.0001f, 0.001f, 0f, 0f, 0f),
        floatArrayOf(0.0001f, 0.001f, 0f, 0f, 0f),
        floatArrayOf(0.0001f, 0.001f, 0f, 0f, 0f),
        floatArrayOf(0.0001f, 0.001f, 0f, 0f, 0f),
        floatArrayOf(0.0001f, 0.001f, 0f, 0f, 0f),
        floatArrayOf(0.0001f, 0.001f, 0f, 0f, 0f),
        floatArrayOf(0.0001f, 0.001f, 0f, 0f, 0f),
        floatArrayOf(0.0001f, 0.001f, 0f, 0f, 0f)
    )

    fun KalmanS(Q: Float, R: Float, num: Int) {
        kalVs[num][4] = (kalVs[num][3] + Q) / (kalVs[num][3] + Q + R)
        kalVs[num][3] = R * (kalVs[num][3] + Q) / (R + kalVs[num][3] + Q)
    }

    fun Kalman(value: Float, num: Int): Float {
        KalmanS(kalVs[num][0], kalVs[num][1], num)
        val result = kalVs[num][2] + (value - kalVs[num][2]) * kalVs[num][4]
        kalVs[num][2] = result
        return result
    }

    fun KalmanInit() {
        for (i in 0..5) {
            kalVs[i][2] = 0f
            kalVs[i][3] = 0f
            kalVs[i][4] = 0f
        }
    }
    private var yaw180cnt = 0 //180°or -180°を越えた回数

    private var theLastYaw = 0f
    private var getYawInitF = true
    private fun getYaw(yaw: Float): Float {
        if (getYawInitF) {
            theLastYaw = yaw
            getYawInitF = false
            return yaw
        }
        if (theLastYaw > 100 && yaw < -100) {
            yaw180cnt += 1
        } else if (theLastYaw < -100 && yaw > 100) {
            yaw180cnt -= 1
        }
        theLastYaw = yaw
        val tmp = yaw180cnt * 360f
        return yaw + tmp
    }

    fun getDirection(a: Float, b: Float, c: Float, d: Float): Int {
        return if (a + 0.1f < b && b + 0.1f < c && c + 0.1f < d) {
            1
        } else if (a > b + 0.1f && b > c + 0.1f && c > d + 0.1f) {
            -1
        } else {
            0
        }
    }

    fun pushPRY(p: Float, r: Float, y: Float) {
        for (i in 0..2) {
            pitchs[i] = pitchs[i + 1]
            rolls[i] = rolls[i + 1]
            yaws!![i] = yaws!![i + 1]
        }
        pitchs[3] = p
        rolls[3] = r
        yaws!![3] = y
    }
    private lateinit var viewBinding: ActivityRehaBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityRehaBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        KalmanInit()
    /*    val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
            .build()
        soundPool = SoundPool.Builder()
            .setAudioAttributes(audioAttributes)
            .setMaxStreams(3)
            .build()
      //  sound1 = soundPool.load(this, R.raw.beep1, 1)
      //  sound2 = soundPool.load(this, R.raw.beep2, 1)
      //  sound3 = soundPool.load(this, R.raw.beep3, 1)
*/
       rehaStartBtn = findViewById<View>(R.id.startButton) as Button
        rehaStopBtn = findViewById<View>(R.id.stopButton) as Button
        rehaResetBtn = findViewById<View>(R.id.resetButton) as Button
        pitchDegreeDownBtn = findViewById<View>(R.id.pitchDegreeDownButton) as Button
        pitchDegreeUpBtn = findViewById<View>(R.id.pitchDegreeUpButton) as Button
        pitchSecDownBtn = findViewById<View>(R.id.pitchSecDownButton) as Button
        pitchSecUpBtn = findViewById<View>(R.id.pitchSecUpButton) as Button
        rollDegreeDownBtn = findViewById<View>(R.id.rollDegreeDownButton) as Button
        rollDegreeUpBtn = findViewById<View>(R.id.rollDegreeUpButton) as Button
        rollSecDownBtn = findViewById<View>(R.id.rollSecDownButton) as Button
        rollSecUpBtn = findViewById<View>(R.id.rollSecUpButton) as Button
        yawDegreeDownBtn = findViewById<View>(R.id.yawDegreeDownButton) as Button
        yawDegreeUpBtn = findViewById<View>(R.id.yawDegreeUpButton) as Button
        yawSecDownBtn = findViewById<View>(R.id.yawSecDownButton) as Button
        yawSecUpBtn = findViewById<View>(R.id.yawSecUpButton) as Button
        exitBtn = findViewById<View>(R.id.exitButton) as Button
        ipe1 = findViewById<View>(R.id.ip1) as EditText
        ipe2 = findViewById<View>(R.id.ip2) as EditText
        ipe3 = findViewById<View>(R.id.ip3) as EditText
        ipe4 = findViewById<View>(R.id.ip4) as EditText
        pitchCurrentE = findViewById<View>(R.id.pitch0) as EditText
        pitchCountE = findViewById<View>(R.id.pitch1) as EditText
        rollCurrentE = findViewById<View>(R.id.roll0) as EditText
        rollCountE = findViewById<View>(R.id.roll1) as EditText
        yawCurrentE = findViewById<View>(R.id.yaw0) as EditText
        yawCountE = findViewById<View>(R.id.yaw1) as EditText

        pitchDegreeE = findViewById<View>(R.id.pitch2) as EditText
        pitchSecE = findViewById<View>(R.id.pitch3) as EditText
        rollDegreeE = findViewById<View>(R.id.roll2) as EditText
        rollSecE = findViewById<View>(R.id.roll3) as EditText
        yawDegreeE = findViewById<View>(R.id.yaw2) as EditText
        yawSecE = findViewById<View>(R.id.yaw3) as EditText
        soundSpin = findViewById<View>(R.id.soundSpinner) as Spinner
        vibrationSpin = findViewById<View>(R.id.vibrationSpinner) as Spinner
        //soundSpin!!.onItemSelectedListener = soundSpinnerSelectedListener()
        //vibrationSpin!!.onItemSelectedListener = vibrationSpinnerSelectedListener()
        // リスナーを登録
        soundSpin!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            //　アイテムが選択された時
            override fun onItemSelected(parent: AdapterView<*>?,
                                        view: View?, position: Int, id: Long) {
                val spinnerParent = parent as Spinner
                val item = spinnerParent.selectedItem as String
                soundType = Math.toIntExact(spinnerParent.selectedItemId)
                saveData()
            }
            //　アイテムが選択されなかった
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
        vibrationSpin!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            //　アイテムが選択された時
            override fun onItemSelected(parent: AdapterView<*>?,
                                        view: View?, position: Int, id: Long) {
                val spinnerParent = parent as Spinner
                val item = spinnerParent.selectedItem as String
                vibrationType = Math.toIntExact(spinnerParent.selectedItemId)
                saveData()
            }
            //　アイテムが選択されなかった
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
        loadData();

        //      mAlertDialog = new AlertDialog.Builder(this);
        //      mAlertDialog.setTitle("Alert");
        //      mAlertDialog.setPositiveButton("OK", null);
      //  soundSpin!!.onItemSelectedListener = soundSpinnerSelectedListener()
      //  vibrationSpin!!.onItemSelectedListener = vibrationSpinnerSelectedListener()
        viewBinding.ipSetButton.setOnClickListener {
            val ips1 = ipe1.toString()
            val ips2 = ipe2.toString()
            val ips3 = ipe3.toString()
            val ips4 = ipe4.toString()
            //      saveData();
            val data = getSharedPreferences("Data", MODE_PRIVATE)
            val editor = data.edit()
            editor.putString("ip1", ips1)
            editor.putString("ip2", ips2)
            editor.putString("ip3", ips3)
            editor.putString("ip4", ips4)
            ipad = String.format("%s.%s.%s.%s",ips1,ips2,ips3,ips4)
            Log.i("kuroa****", ipad)
            inetSocketAddress = InetSocketAddress(ipad, portn)
            editor.commit()
            editor.apply()
            Log.d(TAG, "onclick ips")
        }
        viewBinding.pitchDegreeDownButton.setOnClickListener {
            pitchDegree -= 1f
            Log.d(TAG, "onDegreeDown")
           saveData()
        }
        viewBinding.pitchDegreeUpButton.setOnClickListener {
            pitchDegree += 1f
            Log.d(TAG, "PitchDegreeUpbutton")
         //   (pitchDegreeE as TextView).text="iii"//String.format("%.0fd", pitchDegree)
            saveData()

        }
        viewBinding.pitchSecDownButton.setOnClickListener {
            pitchSec -= 0.1.toFloat()
            Log.d(TAG, "onPitchSecDown")
            saveData()

        }
        viewBinding.pitchSecUpButton.setOnClickListener {
            pitchSec += 0.1.toFloat()
            saveData()
            Log.d(TAG, "onclickStart")
        }
        viewBinding.rollDegreeDownButton.setOnClickListener {
            rollDegree -= 1f
            saveData()
            Log.d(TAG, "onclick rolldegreedown")
        }
        viewBinding.rollDegreeUpButton.setOnClickListener {
            rollDegree += 1f
            saveData()
            Log.d(TAG, "onclickStart")
        }

        viewBinding.rollSecUpButton.setOnClickListener {
            rollSec += 0.1.toFloat()
            saveData()
            Log.d(TAG, "onclick rollsecup")
        }
        viewBinding.yawDegreeDownButton.setOnClickListener {
            yawDegree -= 1f
            saveData()
            Log.d(TAG, "onclick yawdegreedown")
        }
        viewBinding.yawDegreeUpButton.setOnClickListener {
            yawDegree += 1f
            saveData()
            Log.d(TAG, "onclick yawdegreeup")
        }
        viewBinding.yawSecDownButton.setOnClickListener {
            yawSec -= 0.1.toFloat()
            saveData()
            Log.d(TAG, "onclickStart")
        }
        viewBinding.yawSecUpButton.setOnClickListener {
            yawSec += 0.1.toFloat()
            saveData()
            Log.d(TAG, "onclickStart")
        }

        viewBinding.startButton.setOnClickListener {
            rehaF = true
            Log.d(TAG, "onclickStart")
        }
        viewBinding.stopButton.setOnClickListener {
            rehaF = false
            Log.d(TAG, "onclickStop")
        }
        viewBinding.resetButton.setOnClickListener {
            pitchCountE!!.setText("0")
            rollCountE!!.setText("0")
            yawCountE!!.setText("0")
        }

        viewBinding.exitButton.setOnClickListener { //   Log.d(TAG, "surfaceDestroyed...");
            rehaF = false
            finish()
        }

   /*     sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR),
            SensorManager.SENSOR_DELAY_FASTEST
        )
*/

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        //     sma.registerListener(this,sma.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR),SensorManager.SENSOR_DELAY_FASTEST);
        //     sma.registerListener(this,sma.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR),SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(
            this,
            sensorManager!!.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR),
            SensorManager.SENSOR_DELAY_FASTEST
        )
      //  loadData()
    }

    private fun saveData() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = sharedPreferences.edit()
        if (pitchDegree > 120) pitchDegree = 120f else if (pitchDegree < 20) pitchDegree = 20f
        if (rollDegree > 120) rollDegree = 120f else if (rollDegree < 20) rollDegree = 20f
        if (yawDegree > 120) yawDegree = 120f else if (yawDegree < 20) yawDegree = 20f
        if (pitchSec > 4) pitchSec = 4f else if (pitchSec < 0.3) pitchSec = 0.3f
        if (rollSec > 4) rollSec = 4f else if (rollSec < 0.3) rollSec = 0.3f
        if (yawSec > 4) yawSec = 4f else if (yawSec < 0.3) yawSec = 0.3f
        Log.e("test", java.lang.Float.toString(pitchSec))
        editor.putFloat("pitchDegree", pitchDegree)
        editor.putFloat("pitchSec", pitchSec)
        editor.putFloat("rollDegree", rollDegree)
        editor.putFloat("rollSec", rollSec)
        editor.putFloat("yawDegree", yawDegree)
        editor.putFloat("yawSec", yawSec)
        editor.putInt("soundType", soundType)
        editor.putInt("vibrationType", vibrationType)
        editor.putString("ip1", ipe1.toString())
        editor.putString("ip2", ipe2.toString())
        editor.putString("ip3", ipe3.toString())
        editor.putString("ip4", ipe4.toString())
        editor.commit()
        editor.apply()
        pitchDegreeE!!.setText(String.format("%.0fd", pitchDegree))
        pitchSecE!!.setText(String.format("%.1fs", pitchSec))
        rollDegreeE!!.setText(String.format("%.0fd", rollDegree))
        rollSecE!!.setText(String.format("%.1fs", rollSec))
        yawDegreeE!!.setText(String.format("%.0fd", yawDegree))
        yawSecE!!.setText(String.format("%.1fs", yawSec))
        soundSpin!!.setSelection(soundType)
        vibrationSpin!!.setSelection(vibrationType)
   //     ipe1.setText(data.getString("ip1", "192"))
   //     ipe2.setText(data.getString("ip2", "168"))
   //     ipe3.setText(data.getString("ip3", "0"))
   //     ipe4.setText(data.getString("ip4", "99"))
    }

    var ipad = "192.168.0.209"
    var portn = 1108
   // val sharedPref = activity?.getSharedPreferences("com.example.preferences.preference_file_key", Context.MODE_PRIVATE)
    private fun loadData() {
       val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        pitchDegree = sharedPreferences.getFloat("pitchDegree", 30f)
        pitchSec = sharedPreferences.getFloat("pitchSec", 2f)
        rollDegree = sharedPreferences.getFloat("rollDegree", 30f)
        rollSec = sharedPreferences.getFloat("rollSec", 2f)
        yawDegree = sharedPreferences.getFloat("yawDegree", 30f)
        yawSec = sharedPreferences.getFloat("yawSec", 2f)
        soundType = sharedPreferences.getInt("soundType", 1)
        vibrationType = sharedPreferences.getInt("vibrationType", 1)
        pitchDegreeE!!.setText(String.format("%.0fd", pitchDegree))
        pitchSecE!!.setText(String.format("%.1fs", pitchSec))
        rollDegreeE!!.setText(String.format("%.0fd", rollDegree))
        rollSecE!!.setText(String.format("%.1fs", rollSec))
        yawDegreeE!!.setText(String.format("%.0fd", yawDegree))
        yawSecE!!.setText(String.format("%.1fs", yawSec))
        soundSpin!!.setSelection(soundType)
        vibrationSpin!!.setSelection(vibrationType)
        ipe1!!.setText(sharedPreferences.getString("ip1", "192"))
        ipe2!!.setText(sharedPreferences.getString("ip2", "168"))
        ipe3!!.setText(sharedPreferences.getString("ip3", "0"))
        ipe4!!.setText(sharedPreferences.getString("ip4", "99"))
        ipad = String.format("%s.%s.%s.%s",ipe1.toString(),ipe2.toString(),ipe3.toString(),ipe4.toString())
   //     Log.i("kuroa****", ipad)
        inetSocketAddress = InetSocketAddress(ipad, portn)
    }
   /* private var yaw180cnt = 0 //180°or -180°を越えた回数

    private var theLastYaw = 0f
    private var getYawInitF = true
    private fun getYaw(yaw: Float): Float {
        if (getYawInitF) {
            theLastYaw = yaw
            getYawInitF = false
            return yaw
        }
        if (theLastYaw > 100 && yaw < -100) {
            yaw180cnt += 1
        } else if (theLastYaw < -100 && yaw > 100) {
            yaw180cnt -= 1
        }
        theLastYaw = yaw
        val tmp: Float = yaw180cnt * 360f
        return yaw + tmp
    }

    fun getDirection(a: Float, b: Float, c: Float, d: Float): Int {
        return if (a + 0.1f < b && b + 0.1f < c && c + 0.1f < d) {
            1
        } else if (a > b + 0.1f && b > c + 0.1f && c > d + 0.1f) {
            -1
        } else {
            0
        }
    }

    fun pushPRY(p: Float, r: Float, y: Float) {
        for (i in 0..2) {
            pitchs[i] = pitchs[i + 1]
            rolls[i] = rolls[i + 1]
            yaws!![i] = yaws!![i + 1]
        }
        pitchs[3] = p
        rolls[3] = r
        yaws!![3] = y
    }

    var arr = arrayOf(intArrayOf(1, 2, 3), intArrayOf(4, 5, 6), intArrayOf(7, 8, 9))*/
  /*  var kalVs = arrayOf(
        floatArrayOf(0.0001f, 0.001f, 0f, 0f, 0f),
        floatArrayOf(0.0001f, 0.001f, 0f, 0f, 0f),
        floatArrayOf(0.0001f, 0.001f, 0f, 0f, 0f),
        floatArrayOf(0.0001f, 0.001f, 0f, 0f, 0f),
        floatArrayOf(0.0001f, 0.001f, 0f, 0f, 0f),
        floatArrayOf(0.0001f, 0.001f, 0f, 0f, 0f),
        floatArrayOf(0.0001f, 0.001f, 0f, 0f, 0f),
        floatArrayOf(0.0001f, 0.001f, 0f, 0f, 0f)
    )

    fun KalmanS(Q: Float, R: Float, num: Int) {
        kalVs.get(num).get(4) = (kalVs.get(num).get(3) + Q) / (kalVs.get(num).get(3) + Q + R)
        kalVs.get(num).get(3) = R * (kalVs.get(num).get(3) + Q) / (R + kalVs.get(num).get(3) + Q)
    }

    fun Kalman(value: Float, num: Int): Float {
        KalmanS(kalVs.get(num).get(0), kalVs.get(num).get(1), num)
        val result: Float =
            kalVs.get(num).get(2) + (value - kalVs.get(num).get(2)) * kalVs.get(num).get(4)
        kalVs.get(num).get(2) = result
        return result
    }

    fun KalmanInit() {
        for (i in 0..5) {
            kalVs.get(i).get(2) = 0
            kalVs.get(i).get(3) = 0
            kalVs.get(i).get(4) = 0
        }
    }*/

    var RAD_TO_DEG = 180f / 3.1415f
    fun QuaternionToEuler(q0: Float, q1: Float, q2: Float, q3: Float) {
        var pitch: Float
        var roll: Float
        var yaw: Float
        pitch = Math.asin((-2 * q1 * q3 + 2 * q0 * q2).toDouble()).toFloat() // pitch
        roll = Math.atan2(
            (2 * q2 * q3 + 2 * q0 * q1).toDouble(),
            (-2 * q1 * q1 - 2 * q2 * q2 + 1).toDouble()
        ).toFloat()
        yaw = Math.atan2(
            (2 * (q1 * q2 + q0 * q3)).toDouble(),
            (q0 * q0 + q1 * q1 - q2 * q2 - q3 * q3).toDouble()
        ).toFloat()
        pitch *= RAD_TO_DEG
        yaw *= RAD_TO_DEG
        // Declination of SparkFun Electronics (40°05'26.6"N 105°11'05.9"W) is
        //     8° 30' E  ± 0° 21' (or 8.5°) on 2016-07-19
        // - http://www.ngdc.noaa.gov/geomag-web/#declination
        //    yawf -= 8.5;
        roll *= RAD_TO_DEG
        //  pitchA.add(pitch);//(Kalman(value: pitch, num: 0));
        // rollA.add(roll);//(Kalman(value: roll, num: 1));
        pitch = Kalman(pitch, 0)
        roll = Kalman(roll, 1)
        yaw = Kalman(getYaw(yaw), 2)
        pushPRY(pitch, roll, yaw)
        pitchCurrentE!!.setText(String.format("%.0f", pitch))
        rollCurrentE!!.setText(String.format("%.0f", roll))
        yawCurrentE!!.setText(String.format("%.0f", yaw))
    }
//    func checkOK( d0:Float,d1:Float,limit:Float,count:Int,ms:Double)->Int

    //    func checkOK( d0:Float,d1:Float,limit:Float,count:Int,ms:Double)->Int
    override fun onSensorChanged(event: SensorEvent){
        val floats = FloatArray(12)
        floats[8] = event.values[3]
        val nq0 = floats[8]
        floats[9] = event.values[0]
        val nq1 = floats[9]
        floats[10] = event.values[1]
        val nq2 = floats[10]
        floats[11] = event.values[2]
        val nq3 = floats[11]
        val b0 = ((nq0 + 1.0) * 128.0).toInt()
        val b1 = ((nq1 + 1.0) * 128.0).toInt()
        val b2 = ((nq2 + 1.0) * 128.0).toInt()
        val b3 = ((nq3 + 1.0) * 128.0).toInt()
        val Str = String.format("Q:%03d%03d%03d%03d", b0, b1, b2, b3)
        //  send(StringStr, String "192.168.0.209", int 1108) throws IOException {
        val dataStr = Str.toByteArray(StandardCharsets.UTF_8)
         //     ipad = "192.168.0.209";
         //  InetSocketAddress inetSocketAddress = new InetSocketAddress(ipad, portn);
        val datagramPacket: DatagramPacket =
            DatagramPacket(dataStr, dataStr.size, inetSocketAddress)

        // Androidではソケット通信は別スレッドにする必要がある。ここで非同期通信。
        val task: AsyncTask<DatagramPacket, Void, Void> =
            object : AsyncTask<DatagramPacket?, Void?, Void?>() {
                protected fun doInBackground(vararg datagramPackets: DatagramPacket): Void? {
                    var datagramSocket: DatagramSocket? = null
                    try {
                        datagramSocket = DatagramSocket()
                        datagramSocket.send(datagramPackets[0])
                        datagramSocket.close()
                    } catch (e: SocketException) {
                        e.printStackTrace()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                    return null
                }
            }
        task.execute(datagramPacket)
        if (rehaF == true) {
            QuaternionToEuler(nq0, nq1, nq2, nq3)
            checkRotation()
        }
    }
/*
        button_send.setOnClickListener()
        {
            val IPAddress = spinner1.selectedItem.toString() + '.'.toString() +
                            spinner2.selectedItem.toString() + '.'.toString() +
                            spinner3.selectedItem.toString() + '.'.toString() +
                            spinner4.selectedItem.toString()
            // ② ポートはC#側とあわせています。
            // ソケット通信用にポート設定。送信したいデータとIPアドレス設定。
            val inetSocketAddress = InetSocketAddress(IPAddress, 60001)
            // Androidではソケット通信は別スレッドで非同期通信。
            val task = object : AsyncTask<InetSocketAddress, Void, Void>() {
                override fun doInBackground(vararg inetSocketAddresses: InetSocketAddress): Void? {
                    var socket: Socket? = null
                    try {
                        // 接続
                        socket = Socket()
                        socket!!.connect(inetSocketAddresses[0])
                        val writer = BufferedWriter(OutputStreamWriter(socket!!.getOutputStream()))
                        //　① kotolin extensionsで便利。
                        // データを送信。
                        writer.write(edit_text.text.toString())
                        // クローズ
                        writer.close()
                        socket!!.close()
                    } catch (e: SocketException) {
                        e.printStackTrace()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                    return null
                }
            }
            task.execute(inetSocketAddress)
        }

 */
    var pitchDirection = 0
    var rollDirection = 0
    var yawDirection = 0
    var lastPitch = 0f

    var lastPitchMilli: Long = 0
    var lastRollMilli: Long = 0
    var lastYawMilli: Long = 0
    var lastRoll = 0f

    var lastYaw = 0f

    fun incPitchOK() {
        val str = pitchCountE!!.text.toString()
        var t: Int = str.toInt()
        t += 1
        pitchCountE!!.setText(t.toString())
    }

    private fun checkOK(d0: Float, d1: Float, limit: Float, millis: Long, ms: Float): Int {
        val milli = millis / 1000000
        val mslong = (ms * 1000f).toLong()
        val d = d0 - d1
        //   if (millis < 100){return 0;}//5*40ms
        return if (d > limit || d < -limit) {
            if (milli < mslong) { // && milli>200){
                val Str = String.format(
                    "d:%f ms:%S limit:%s",
                    d0 - d1,
                    java.lang.Long.toString(milli),
                    java.lang.Long.toString(mslong)
                )
                Log.e("test", Str)
                5
            } else {
                0
            }
        } else 0
    }

    fun incRollOK() {
        val str = rollCountE!!.text.toString()
        var t: Int = str.toInt()
        t += 1
        rollCountE!!.setText(t.toString())
    }

    fun incYawOK() {
        val str = yawCountE!!.text.toString()
        var t: Int = str.toInt()
        t += 1
        yawCountE!!.setText(t.toString())
    }

    fun soundANDvibe() {
        if (soundType == 1) {
            soundPool!!.play(sound1, 1.0f, 1.0f, 1, 0, 1f)
        } else if (soundType == 2) {
            soundPool!!.play(sound2, 1.0f, 1.0f, 1, 0, 1f)
        } else if (soundType == 3) {
            soundPool!!.play(sound3, 1.0f, 1.0f, 1, 0, 1f)
        }
        var ms: Long = 0
        if (vibrationType == 1) ms = 50 else if (vibrationType == 2) ms =
            100 else if (vibrationType == 3) ms = 150
        // Vibrate for 500 milliseconds
        if (ms != 0L) {
            val v = getSystemService(VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                //deprecated in API 26
                v.vibrate(ms)
            }
        }
    }

    private fun checkRotation() {
        //int tempDirection=0;
        // pitch
        val tempPitchDirection: Int = getDirection(pitchs[0], pitchs[1], pitchs[2], pitchs[3])
        if (tempPitchDirection == -1 && pitchDirection == 1 || tempPitchDirection == 1 && pitchDirection == -1) //向きが代わった時
        {
            pitchDirection = tempPitchDirection //向きを新しくする
            val nowTime = System.nanoTime()
            if (checkOK(
                    lastPitch,
                    pitchs[0],
                    pitchDegree,
                    nowTime - lastPitchMilli,
                    pitchSec
                ) == 5
            ) {
                incPitchOK()
                soundANDvibe()
            }
            //       String Str = String.format ("1:%f 2:%f",lastPitch,pitchs[0]);
            //       Log.e("test",Str);
            lastPitch = pitchs[0]
            lastPitchMilli = System.nanoTime() // bTimeUnit.MILLISECONDS.ordinal();
        }
        if (tempPitchDirection != 0) {
            pitchDirection = tempPitchDirection
        }
        // roll
        val tempRollDirection: Int = getDirection(rolls[0], rolls[1], rolls[2], rolls[3])
        if (tempRollDirection == -1 && rollDirection == 1 || tempRollDirection == 1 && rollDirection == -1) {
            rollDirection = tempRollDirection
            val nowTime = System.nanoTime()
            if (checkOK(lastRoll, rolls[0], rollDegree, nowTime - lastRollMilli, rollSec) == 5) {
                incRollOK()
                soundANDvibe()
            }
            lastRoll = rolls[0]
            lastRollMilli = System.nanoTime()
        }
        if (tempRollDirection != 0) {
            rollDirection = tempRollDirection
        }
        // yaw
        val tempYawDirection: Int = getDirection(yaws!![0], yaws!![1], yaws!![2], yaws!![3])
        if (tempYawDirection == -1 && yawDirection == 1 || tempYawDirection == 1 && yawDirection == -1) {
            yawDirection = tempYawDirection
            val nowTime = System.nanoTime()
            if (checkOK(lastYaw, yaws!![0], yawDegree, nowTime - lastYawMilli, yawSec) == 5) {
                incYawOK()
                soundANDvibe()
            }
            lastYaw = yaws!![0]
            lastYawMilli = System.nanoTime()
        }
        if (tempYawDirection != 0) {
            yawDirection = tempYawDirection
        }
    }
   /* private fun sensorReset(){
        sensorManager.unregisterListener(this)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR),
            SensorManager.SENSOR_DELAY_FASTEST
        )
        viewBinding.myView.initData()
    }
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) {
            when(event.action){
                MotionEvent.ACTION_DOWN -> sensorReset()
            }
        }

        //再描画を実行させる呪文
        //   Log.e("kdiidiid","motion touch")
        return super.onTouchEvent(event)
    }*/
    //センサの精度が変更されたときに呼ばれる
    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }

    override fun onResume() {
        super.onResume()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR),
            SensorManager.SENSOR_DELAY_FASTEST
        )
        //リスナーとセンサーオブジェクトを渡す
        //第一引数はインターフェースを継承したクラス、今回はthis
        //第二引数は取得したセンサーオブジェクト
        //第三引数は更新頻度 UIはUI表示向き、FASTはできるだけ早く、GAMEはゲーム向き
        //sensorManager.registerListener(this, quaternionSensor, SensorManager.SENSOR_DELAY_FASTEST)
    }

    //アクティビティが閉じられたときにリスナーを解除する
    override fun onPause() {
        super.onPause()
        //リスナーを解除しないとバックグラウンドにいるとき常にコールバックされ続ける
        sensorManager.unregisterListener(this)
    }
}

