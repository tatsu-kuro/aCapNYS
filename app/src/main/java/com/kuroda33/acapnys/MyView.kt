package com.kuroda33.acapnys

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.preference.PreferenceManager
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import java.util.Timer

class MyView(context: Context?, attrs: AttributeSet?) : View(context, attrs){
    private var path : Path = Path()//線を引く、図形を描く、グラフィック描画
    private var paint : Paint = Paint()//色とか太さ
    private var drawX:Float= 0F
    private var drawY:Float= 0F
    private var mTimer: Timer? = null
    //２）onDraw(描画の準備)f
    private var mIsDrawing = false
    private var mPaint: Paint = Paint() //画?
    private var mPaint2: Paint = Paint()

    //路径
    private var mPath: Path = Path()
    var initFlag: Boolean = true
    var camera_num: Int = 0
    fun setCamera(cameran:Int){
        camera_num=cameran
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas!!)
        if (initFlag) {
            mPaint.style = Paint.Style.STROKE
            mPaint.strokeWidth = 6f
            mPaint.isAntiAlias = false
            mPaint.color = Color.BLACK
            //           mPaint2.setStyle(Paint.Style.STROKE);
            mPaint2.strokeWidth = 6f
            mPaint2.isAntiAlias = false
            mPaint2.color = Color.LTGRAY
            initFlag = false
        }
        mPath.reset()
        canvas.drawARGB(255, 255, 255, 255);
        canvas.drawCircle( (canvas.width / 2).toFloat(),(canvas.height / 2).toFloat(),(canvas.height / 3).toFloat(), mPaint2 )
        drawHead(canvas.width, canvas.height, canvas.height / 3, mnq0, mnq1, mnq2, mnq3)
        canvas.drawPath(mPath, mPaint)
    }

    //３）実際の描画（押した時、動かした時）
   /* override fun onTouchEvent(event: MotionEvent?): Boolean {
        //タッチポジション（x座標、y座標）
        //drawX = event!!.x
        //drawY = event.y
        if (event != null) {
            when(event!!.action){
                MotionEvent.ACTION_DOWN -> clearCanvas()
                //   MotionEvent.ACTION_MOVE -> path.lineTo(drawX,drawY)
                MotionEvent.ACTION_UP -> clearCanvas()
            }
        }
        //再描画を実行させる呪文
        //invalidate()
        return true
    }*/
    var mnq0 = 0f
    var mnq1:kotlin.Float = 0f
    var mnq2:kotlin.Float = 0f
    var mnq3:kotlin.Float = 0f
    var a0 = 0f
    var a1 = 0f
    var a2 = 0f
    var a3 = 0f
    var cq0 = 0.99f
    var cq1 = 0.0f
    var cq2 = 0.0f
    var cq3 = 0.0f //spaceを押した時のcenter quatrnion
    var nq0 = 0.01f
    var nq1 = 0.0f
    var nq2 = 0.0f
    var nq3 = 0.0f //現在のquaternion
    fun initData(){
        cq0 = 0.99f
        cq1 = 0.0f
        cq2 = 0.0f
        cq3 = 0.0f //spaceを押した時のcenter quatrnion
        nq0 = 0.01f
        nq1 = 0.0f
        nq2 = 0.0f
        nq3 = 0.0f //現在のquaternion
    }
  //  fun clearCanvas(){
    //    cq0 =  nq0
      //  cq3 = -nq3
   // }
    private fun MultQuat(
        q0: Float,
        q1: Float,
        q2: Float,
        q3: Float,
        p0: Float,
        p1: Float,
        p2: Float,
        p3: Float
    ) {
        a0 = q0 * p0 - q1 * p1 - q2 * p2 - q3 * p3
        a1 = q1 * p0 + q0 * p1 - q3 * p2 + q2 * p3
        a2 = q2 * p0 + q3 * p1 + q0 * p2 - q1 * p3
        a3 = q3 * p0 - q2 * p1 + q1 * p2 + q0 * p3
    }

    fun setQuats(
        tempnq0: Float,
        tempnq1: Float,
        tempnq2: Float,
        tempnq3: Float,
    ) {
        nq0 = tempnq0
        nq1 = tempnq1
        nq2 = tempnq2
        nq3 = tempnq3
        MultQuat(cq0, cq1, cq2, cq3, nq0, nq1, nq2, nq3) //set a0~a3
        // QuatXchan(a0,a1, a2, a3);//set mnq0~mnq3
        val norm: Float
        val mag: Float
        mag = Math.sqrt((a0 * a0 + a1 * a1 + a2 * a2 + a3 * a3).toDouble()).toFloat()
        if (mag > 1.192092896e-07f) {
            norm = 1 / mag
            a0 *= norm
            a1 *= norm
            a2 *= norm
            a3 *= norm
        }
        mnq0 = a0
        mnq1 = a2
        mnq2 = a3
        mnq3 = a1

        invalidate()
    }

    var rpk12 = Array(600) { FloatArray(3) }

    //float[] ppk1 = new float[600];
    var ppk12 = Array(600) { FloatArray(3) }

    var facePoints2 = arrayOf(
        intArrayOf(0, 0, 0),
        intArrayOf(15, 0, 0),
        intArrayOf(30, 0, 0),
        intArrayOf(45, 0, 0),
        intArrayOf(60, 0, 0),
        intArrayOf(75, 0, 0),
        intArrayOf(90, 0, 0),
        intArrayOf(105, 0, 0),
        intArrayOf(120, 0, 0),
        intArrayOf(135, 0, 0),
        intArrayOf(150, 0, 0),
        intArrayOf(165, 0, 0),
        intArrayOf(180, 0, 0),
        intArrayOf(195, 0, 0),
        intArrayOf(210, 0, 0),
        intArrayOf(225, 0, 0),
        intArrayOf(240, 0, 0),
        intArrayOf(255, 0, 0),
        intArrayOf(270, 0, 0),
        intArrayOf(285, 0, 0),
        intArrayOf(300, 0, 0),
        intArrayOf(315, 0, 0),
        intArrayOf(330, 0, 0),
        intArrayOf(345, 0, 0),
        intArrayOf(360, 0, 1),
        intArrayOf(0, 0, 0),
        intArrayOf(0, 15, 0),
        intArrayOf(0, 30, 0),
        intArrayOf(0, 45, 0),
        intArrayOf(0, 60, 0),
        intArrayOf(0, 75, 0),
        intArrayOf(0, 90, 0),
        intArrayOf(0, 105, 0),
        intArrayOf(0, 120, 0),
        intArrayOf(0, 135, 0),
        intArrayOf(0, 150, 0),
        intArrayOf(0, 165, 0),
        intArrayOf(0, 180, 0),
        intArrayOf(0, 195, 0),
        intArrayOf(0, 210, 0),
        intArrayOf(0, 225, 0),
        intArrayOf(0, 240, 0),
        intArrayOf(0, 255, 0),
        intArrayOf(0, 270, 0),
        intArrayOf(0, 285, 0),
        intArrayOf(0, 300, 0),
        intArrayOf(0, 315, 0),
        intArrayOf(0, 330, 0),
        intArrayOf(0, 345, 0),
        intArrayOf(0, 360, 1),
        intArrayOf(0, 90, 0),
        intArrayOf(15, 90, 0),
        intArrayOf(30, 90, 0),
        intArrayOf(45, 90, 0),
        intArrayOf(60, 90, 0),
        intArrayOf(75, 90, 0),
        intArrayOf(90, 90, 0),
        intArrayOf(105, 90, 0),
        intArrayOf(120, 90, 0),
        intArrayOf(135, 90, 0),
        intArrayOf(150, 90, 0),
        intArrayOf(165, 90, 0),
        intArrayOf(180, 90, 0),
        intArrayOf(195, 90, 0),
        intArrayOf(210, 90, 0),
        intArrayOf(225, 90, 0),
        intArrayOf(240, 90, 0),
        intArrayOf(255, 90, 0),
        intArrayOf(270, 90, 0),
        intArrayOf(285, 90, 0),
        intArrayOf(300, 90, 0),
        intArrayOf(315, 90, 0),
        intArrayOf(330, 90, 0),
        intArrayOf(345, 90, 90),
        intArrayOf(360, 90, 1),
        intArrayOf(20, -90, 0),
        intArrayOf(20, -105, 0),
        intArrayOf(20, -120, 0),
        intArrayOf(20, -135, 0),
        intArrayOf(20, -150, 0),
        intArrayOf(20, -165, 0),
        intArrayOf(20, -180, 1),
        intArrayOf(-20, -90, 0),
        intArrayOf(-20, -105, 0),
        intArrayOf(-20, -120, 0),
        intArrayOf(-20, -135, 0),
        intArrayOf(-20, -150, 0),
        intArrayOf(-20, -165, 0),
        intArrayOf(-20, -180, 1),
        intArrayOf(40, -90, 0),
        intArrayOf(40, -105, 0),
        intArrayOf(40, -120, 0),
        intArrayOf(40, -135, 0),
        intArrayOf(40, -150, 0),
        intArrayOf(40, -165, 0),
        intArrayOf(40, -180, 1),
        intArrayOf(-40, -90, 0),
        intArrayOf(-40, -105, 0),
        intArrayOf(-40, -120, 0),
        intArrayOf(-40, -135, 0),
        intArrayOf(-40, -150, 0),
        intArrayOf(-40, -165, 0),
        intArrayOf(-40, -180, 1),
        intArrayOf(23, -9, 0),
        intArrayOf(31, -12, 0),
        intArrayOf(38, -20, 0),
        intArrayOf(40, -31, 0),
        intArrayOf(38, -41, 0),
        intArrayOf(31, -46, 0),
        intArrayOf(23, -45, 0),
        intArrayOf(15, -39, 0),
        intArrayOf(10, -32, 0),
        intArrayOf(8, -23, 0),
        intArrayOf(10, -16, 0),
        intArrayOf(15, -10, 0),
        intArrayOf(23, -9, 1),
        intArrayOf(-23, -9, 0),
        intArrayOf(-31, -12, 0),
        intArrayOf(-38, -20, 0),
        intArrayOf(-40, -31, 0),
        intArrayOf(-38, -41, 0),
        intArrayOf(-31, -46, 0),
        intArrayOf(-23, -45, 0),
        intArrayOf(-15, -39, 0),
        intArrayOf(-10, -32, 0),
        intArrayOf(-8, -23, 0),
        intArrayOf(-10, -16, 0),
        intArrayOf(-15, -10, 0),
        intArrayOf(-23, -9, 1),
        intArrayOf(22, -26, 0),
        intArrayOf(23, -25, 0),
        intArrayOf(24, -24, 1),
        intArrayOf(-22, -26, 0),
        intArrayOf(-23, -25, 0),
        intArrayOf(-24, -24, 1),
        intArrayOf(-19, 32, 0),
        intArrayOf(-14, 31, 0),
        intArrayOf(-9, 31, 0),
        intArrayOf(-4, 31, 0),
        intArrayOf(0, 30, 0),
        intArrayOf(4, 31, 0),
        intArrayOf(9, 31, 0),
        intArrayOf(14, 31, 0),
        intArrayOf(19, 32, 1),
        intArrayOf(1000, 1000, 1000)
    ) //mouse 9

    var pi180 = Math.PI.toFloat() / 180.0f

    fun set_rpk_ppk() {
        val r = 40f //hankei
        var dx: Float
        var dy: Float
        var dz: Float
        // convert draw data to radian
        run {
            var i = 0
            while (facePoints2[i][0] != 1000) {
                rpk12[i][0] = facePoints2[i][0].toFloat() * pi180
                rpk12[i][1] = facePoints2[i][1].toFloat() * pi180
                i++
            }
        }

        // move (1,0,0) to each draw point
        run {
            var i = 0
            while (facePoints2[i][0] != 1000) {
                ppk12[i][0] = 0f
                ppk12[i][1] = 1.0f * r
                ppk12[i][2] = 0f
                i++
            }
        }

        // rotate all draw point based on draw data
        var i = 0
        while (facePoints2[i][0] != 1000) {

            //rotateX
            dy = ppk12[i][1] * Math.cos(rpk12[i][0].toDouble()).toFloat() - ppk12[i][2] * Math.sin(
                rpk12[i][0].toDouble()
            ).toFloat()
            dz = ppk12[i][1] * Math.sin(rpk12[i][0].toDouble()).toFloat() + ppk12[i][2] * Math.cos(
                rpk12[i][0].toDouble()
            ).toFloat()
            ppk12[i][1] = dy
            ppk12[i][2] = dz
            //rotateZ
            dx = ppk12[i][0] * Math.cos(rpk12[i][1].toDouble()).toFloat() - ppk12[i][1] * Math.sin(
                rpk12[i][1].toDouble()
            ).toFloat()
            dy = ppk12[i][0] * Math.sin(rpk12[i][1].toDouble()).toFloat() + ppk12[i][1] * Math.cos(
                rpk12[i][1].toDouble()
            ).toFloat()
            ppk12[i][0] = dx
            ppk12[i][1] = dy
            //rotateY
            dx = ppk12[i][0] * Math.cos(1.5707963).toFloat() - ppk12[i][2] * Math.sin(1.5707963)
                .toFloat()
            dz = ppk12[i][0] * Math.sin(1.5707963).toFloat() + ppk12[i][2] * Math.cos(1.5707963)
                .toFloat()
            ppk12[i][0] = dx
            ppk12[i][2] = dz
            i++
        }
    }
    //モーションセンサーをリセットするときに-1とする。リセット時に-1なら,角度から０か１をセット
    var degreeAtResetHead = 0 //0:-90<&&<90 1:<-90||>90 -1:flag for get degree


    private fun RotateQu(
        i: Int,
        x0: Float,
        y0: Float,
        z0: Float,
        q0: Float,
        q1: Float,
        q2: Float,
        q3: Float
    ) {
        /*  float norm, mag;
        mag = (float)sqrt((q0 * q0) + (q1 * q1) + (q2 * q2) + (q3 * q3));
        if (mag>1.192092896e-07F){
            norm = 1 / mag;
            q0 *= norm;
            q1 *= norm;
            q2 *= norm;
            q3 *= norm;
        }*/
        ppk[i][0] =
            x0 * (q0 * q0 + q1 * q1 - q2 * q2 - q3 * q3) + y0 * (2f * (q1 * q2 - q0 * q3)) + z0 * (2f * (q1 * q3 + q0 * q2))
        ppk[i][1] =
            x0 * (2f * (q1 * q2 + q0 * q3)) + y0 * (q0 * q0 - q1 * q1 + q2 * q2 - q3 * q3) + z0 * (2f * (q2 * q3 - q0 * q1))
        ppk[i][2] =
            x0 * (2f * (q1 * q3 - q0 * q2)) + y0 * (2f * (q2 * q3 + q0 * q1)) + z0 * (q0 * q0 - q1 * q1 - q2 * q2 + q3 * q3)
    }

    var ppk = Array(600) { FloatArray(3) }
    fun drawHead(
        w: Int,
        h: Int,
        faceR: Int,
        qOld0: Float,
        qOld1: Float,
        qOld2: Float,
        qOld3: Float
    ) {
        val faceX0 = w / 2
        val faceY0 = h / 2 //center
        //    float faceR = r;//hankei
        val defaultRadius = 40
        //     let size = CGSize(width:w, height:h)
        var i = 0
        while (facePoints2.get(i).get(0) != 1000) {
            if (camera_num == 1) {
                RotateQu(
                    i,
                    ppk12.get(i).get(0),
                    ppk12.get(i).get(1),
                    ppk12.get(i).get(2),
                    qOld0,
                    qOld1,
                    qOld2,
                    qOld3
                )
            } else {
                RotateQu(
                    i,
                    -ppk12.get(i).get(0),
                    -ppk12.get(i).get(1),
                    ppk12.get(i).get(2),
                    qOld0,
                    qOld1,
                    qOld2,
                    qOld3
                )
            }
            i++
        }
        val uraPoint = faceR / 50.0f //この値の意味がよくわからなかった
        var endpointF = true //終点でtrueとする
        if (camera_num == 0) { //iPhoneが >90||<-90 垂直以上に傾いた時
            var i = 0
            while (facePoints2.get(i).get(0) != 1000) {
                if (endpointF == true) { //始点に移動する
                    endpointF = if (ppk.get(i).get(1) < uraPoint) {
                        true
                    } else {
                        false
                    }
                    mPath!!.moveTo(
                        faceX0 + ppk.get(i).get(0) * faceR / defaultRadius,
                        faceY0 + ppk.get(i).get(2) * faceR / defaultRadius
                    )
                } else {
                //    if (ppk.get(i).get(1) >= uraPoint) {
                        if (ppk.get(i).get(1) > uraPoint) {
                            mPath!!.lineTo(
                                faceX0 + ppk.get(i).get(0) * faceR / defaultRadius,
                                faceY0 + ppk.get(i).get(2) * faceR / defaultRadius
                            )
                        } else {
                            mPath!!.moveTo(
                                faceX0 + ppk.get(i).get(0) * faceR / defaultRadius,
                                faceY0 + ppk.get(i).get(2) * faceR / defaultRadius
                            )
                        }
                        if (facePoints2.get(i).get(2) == 1) {
                            endpointF = true
                        }
                  //  }
                }
                i++
            }
        } else { //iPhoneが-90~+90の時
            var i = 0
            while (facePoints2.get(i).get(0) != 1000) {
                if (endpointF == true) { //始点に移動する
                    endpointF = if (ppk.get(i).get(1) < uraPoint) {
                        true
                    } else {
                        false
                    }
                    mPath!!.moveTo(
                        faceX0 - ppk.get(i).get(0) * faceR / defaultRadius,
                        faceY0 - ppk.get(i).get(2) * faceR / defaultRadius
                    )
                } else {
                    if (ppk.get(i).get(1) > uraPoint) {
                        mPath!!.lineTo(
                            faceX0 - ppk.get(i).get(0) * faceR / defaultRadius,
                            faceY0 - ppk.get(i).get(2) * faceR / defaultRadius
                        )
                    } else {
                        mPath!!.moveTo(
                            faceX0 - ppk.get(i).get(0) * faceR / defaultRadius,
                            faceY0 - ppk.get(i).get(2) * faceR / defaultRadius
                        )
                    }
                    if (facePoints2.get(i).get(2) == 1) {
                        endpointF = true
                    }
                }
                i++
            }
        }
    }

}