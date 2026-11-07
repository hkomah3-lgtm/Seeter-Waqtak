package com.seeterwaqtak.app

import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class AlarmRingingActivity: ComponentActivity() {
 private var ringtone:Ringtone?=null
 override fun onCreate(savedInstanceState:Bundle?) {
  super.onCreate(savedInstanceState)
  window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
  val id=intent.getLongExtra("id",-1L)
  val title=intent.getStringExtra("title") ?: "حان وقت المهمة"
  val sound=intent.getStringExtra("sound")
  try { ringtone=RingtoneManager.getRingtone(this,if(!sound.isNullOrBlank()) android.net.Uri.parse(sound) else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));ringtone?.play() } catch(_:Exception){}
  setContent {
   MaterialTheme {
    Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.surface) {
     Column(Modifier.fillMaxSize().padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
      Text("حان وقت مهمتك",style=MaterialTheme.typography.headlineMedium)
      Spacer(Modifier.height(18.dp))
      Text(title,style=MaterialTheme.typography.headlineLarge)
      Spacer(Modifier.height(36.dp))
      Button(onClick={stopAlarm(id);finish()},modifier=Modifier.fillMaxWidth()) { Text("إيقاف المنبّه") }
      Spacer(Modifier.height(12.dp))
      OutlinedButton(onClick={snooze(id,title,sound);finish()},modifier=Modifier.fillMaxWidth()) { Text("تأجيل 5 دقائق") }
     }
    }
   }
  }
 }
 private fun stopAlarm(id:Long) {
  ringtone?.stop()
  (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(id.toInt())
 }
 private fun snooze(id:Long,title:String,sound:String?) {
  stopAlarm(id)
  val i=Intent(this,AlarmReceiver::class.java).setAction("com.seeterwaqtak.SNOOZE").putExtra("id",id)
  val pi=PendingIntent.getBroadcast(this,(id xor 0x5A5A5A5A).toInt(),i,PendingIntent.FLAG_UPDATE_CURRENT or if(android.os.Build.VERSION.SDK_INT>=23) PendingIntent.FLAG_IMMUTABLE else 0)
  val am=getSystemService(Context.ALARM_SERVICE) as AlarmManager
  val at=System.currentTimeMillis()+5*60*1000
  if(android.os.Build.VERSION.SDK_INT>=23) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi) else am.setExact(AlarmManager.RTC_WAKEUP,at,pi)
 }
 override fun onDestroy(){ringtone?.stop();super.onDestroy()}
}