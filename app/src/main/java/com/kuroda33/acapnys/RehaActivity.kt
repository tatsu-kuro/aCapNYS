package com.kuroda33.acapnys

//import android.hardware.Sensor
//import android.hardware.SensorManager
//import com.kuroda33.acapnys.GyroActivity.soundSpinnerSelectedListener
//import com.kuroda33.acapnys.GyroActivity.vibrationSpinnerSelectedListener
import android.media.SoundPool
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.kuroda33.acapnys.databinding.ActivityRehaBinding
import com.kuroda33.acapnys.databinding.ActivityRehaBinding.*


//import java.util.prefs.Preferences

class RehaActivity : AppCompatActivity() {

  //  var sensorManager: SensorManager? = null
    private var soundPool: SoundPool? = null
    private var sound1 = 0
    private var sound2:kotlin.Int = 0
    private var sound3:kotlin.Int = 0

    //  long millis0 = TimeUnit.MILLISECONDS.ordinal();
    private val TAG = "MainActivity"
  //  var inetSocketAddress: InetSocketAddress? = null

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
    var yaws: FloatArray? = floatArrayOf(0f, 0f, 0f, 0f, 0f)
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
    //        inetSocketAddress = InetSocketAddress(ipad, portn)
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
     //   sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        //     sma.registerListener(this,sma.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR),SensorManager.SENSOR_DELAY_FASTEST);
        //     sma.registerListener(this,sma.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR),SensorManager.SENSOR_DELAY_FASTEST);
       /* sensorManager.registerListener(
            this,
            sensorManager!!.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR),
            SensorManager.SENSOR_DELAY_FASTEST
        )*/
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
        //inetSocketAddress = InetSocketAddress(ipad, portn)
    }

}

