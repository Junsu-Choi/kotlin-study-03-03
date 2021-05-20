package com.jschoi.develop.aop_part03_chapter03

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.*

/**
 *  알람 앱
 *
 *  AlarmManager
 *  Notification
 *  Broadcast receiver
 *  WorkManager
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val SHARED_PREFERENCES_NAME = "time"
        private const val ALARM_KEY = "alarm"
        private const val ONOFF_KEY = "onOff"
        private const val ALARM_REQUEST_CODE = 1000
    }

    private val tvTime: TextView by lazy {
        findViewById(R.id.tv_time)
    }
    private val tvAmPm: TextView by lazy {
        findViewById(R.id.tv_am_pm)
    }
    private val btnOnOff: Button by lazy {
        findViewById(R.id.btn_on_off)
    }
    private val btnChangeAlarmTime: Button by lazy {
        findViewById(R.id.btn_change_alarm_time)
    }

    private val onClickListener = View.OnClickListener { v ->
        when (v) {
            btnOnOff -> {
                initOnOffButton(v)
            }
            btnChangeAlarmTime -> {
                initChangeAlarmTimeButton()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()

        val model = fetchDataFromSharedPreferences()
        renderView(model)
    }

    private fun initViews() {
        btnOnOff.setOnClickListener(onClickListener)
        btnChangeAlarmTime.setOnClickListener(onClickListener)

        val model = fetchDataFromSharedPreferences()
        renderView(model)
    }

    /**
     * 알람 On/Off 버튼
     */
    private fun initOnOffButton(view: View) {

        val model = view.tag as? AlarmDisplayModel ?: return
        val newModel = saveAlarmModel(model.hour, model.minute, model.onOff.not())

        renderView(newModel)
        // on, off
        if (newModel.onOff) {   // 켜진 경우 -> 알람을 등록
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, newModel.hour)
                set(Calendar.MINUTE, newModel.minute)

                if (before(Calendar.getInstance())) { //  현재 시간보다 이전이면 하루 추가
                    add(Calendar.DATE, 1)
                }
            }

            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            val intent = Intent(this, AlarmReceiver::class.java)
            // FLAG_UPDATE_CURRENT - 기존데이터 있으면 현재 데이터로 덮는다.
            val pendingIntent = PendingIntent.getBroadcast(this, ALARM_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT)

            alarmManager.setInexactRepeating(
                    /*AlarmManager.ELAPSED_REALTIME_WAKEUP // 휴대폰이 부팅된 이후 ~ */
                    AlarmManager.RTC_WAKEUP, // 실제 시간 기준
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY, // 하루에 한번씩
                    pendingIntent
            )
        } else {
            cancelAlarm()   // 꺼진 경우 -> 알람을 제거
        }
    }

    /**
     * 시간 재설정 버튼
     */
    private fun initChangeAlarmTimeButton() {
        val calendar = Calendar.getInstance()

        TimePickerDialog(this, { picker, hour, minute ->
            val model = saveAlarmModel(hour, minute, false)
            renderView(model)
            cancelAlarm()

        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
    }

    /**
     * Pref 데이터 가져오기
     */
    private fun fetchDataFromSharedPreferences(): AlarmDisplayModel {
        val pref = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)

        val timeDBValue = pref.getString(ALARM_KEY, "9:30") ?: "9:30"
        val onOffDBValue = pref.getBoolean(ONOFF_KEY, false)
        val alarmData = timeDBValue.split(":")

        val alarmModel = AlarmDisplayModel(alarmData[0].toInt(), alarmData[1].toInt(), onOffDBValue)

        // 보정 예외처리
        val pendingIntent = PendingIntent.getBroadcast(this, ALARM_REQUEST_CODE, Intent(this, AlarmReceiver::class.java), PendingIntent.FLAG_NO_CREATE)

        if ((pendingIntent == null) and alarmModel.onOff) {
            // 알람은 꺼져있는데, 데이터는 켜져있는 경우
            alarmModel.onOff = false
        } else if ((pendingIntent != null) and alarmModel.onOff.not()) {
            pendingIntent.cancel()
        }

        return alarmModel
    }

    /**
     * Pref 데이터 저장하기
     */
    private fun saveAlarmModel(hour: Int, minute: Int, ofOff: Boolean): AlarmDisplayModel {
        val model = AlarmDisplayModel(hour, minute, ofOff)

        getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit().apply {
            putString(ALARM_KEY, model.makeDataForDB())
            putBoolean(ONOFF_KEY, model.onOff)
            commit()
        }

        return model
    }


    private fun renderView(model: AlarmDisplayModel) {
        tvAmPm.text = model.ampmText
        tvTime.text = model.timeText
        btnOnOff.run {
            text = model.onOffText
            tag = model
        }
    }

    private fun cancelAlarm() {
        val pendingIntent = PendingIntent.getBroadcast(this, ALARM_REQUEST_CODE, Intent(this, AlarmReceiver::class.java), PendingIntent.FLAG_NO_CREATE)
        pendingIntent?.cancel()

    }
}