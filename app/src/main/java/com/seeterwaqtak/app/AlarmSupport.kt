package com.seeterwaqtak.app

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

data class AlarmItem(
 val id: Long, val title: String, val hour: Int, val minute: Int,
 val days: Set<Int>, val sound: String, val soundName: String, val enabled: Boolean
)

object AlarmStore {
 private const val PREFS="seeter_alarms"
 fun load(context: Context): List<AlarmItem> {
  val raw=context.getSharedPreferences(PREFS,0).getString("items","[]") ?: "[]"
  val a=JSONArray(raw)
  return (0 until a.length()).mapNotNull { i -> try {
   val o=a.getJSONObject(i); val d=o.getJSONArray("days")
   AlarmItem(o.getLong("id"),o.getString("title"),o.getInt("hour"),o.getInt("minute"),
    (0 until d.length()).map{d.getInt(it)}.toSet(),o.optString("sound",""),o.optString("soundName","نغمة المنبّه"),o.optBoolean("enabled",true))
  } catch(_:Exception){null} }
 }
 fun save(context: Context, items: List<AlarmItem>) {
  val a=JSONArray()
  items.forEach { item ->
   val d=JSONArray();item.days.sorted().forEach{d.put(it)}
   a.put(JSONObject().put("id",item.id).put("title",item.title).put("hour",item.hour).put("minute",item.minute).put("days",d).put("sound",item.sound).put("soundName",item.soundName).put("enabled",item.enabled))
  }
  context.getSharedPreferences(PREFS,0).edit().putString("items",a.toString()).apply()
 }
 fun daysLabel(days:Set<Int>):String {
  val names=mapOf(Calendar.SATURDAY to "السبت",Calendar.SUNDAY to "الأحد",Calendar.MONDAY to "الاثنين",Calendar.TUESDAY to "الثلاثاء",Calendar.WEDNESDAY to "الأربعاء",Calendar.THURSDAY to "الخميس",Calendar.FRIDAY to "الجمعة")
  return days.sorted().joinToString("، "){names[it] ?: ""}
 }
 fun cancel(context:Context,id:Long) {
  val am=context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
  am.cancel(pending(context,id))
 }
 private fun pending(context:Context,id:Long):PendingIntent {
  val i=Intent(context,AlarmReceiver::class.java).setAction("com.seeterwaqtak.ALARM").putExtra("id",id)
  return PendingIntent.getBroadcast(context,id.toInt(),i,PendingIntent.FLAG_UPDATE_CURRENT or if(Build.VERSION.SDK_INT>=23) PendingIntent.FLAG_IMMUTABLE else 0)
 }
 fun schedule(context:Context,item:AlarmItem) {
  if(!item.enabled)return
  val am=context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
  val now=Calendar.getInstance()
  val target=Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY,item.hour);set(Calendar.MINUTE,item.minute);set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0) }
  if(item.days.isEmpty()) { if(target.timeInMillis<=now.timeInMillis) target.add(Calendar.DAY_OF_YEAR,1) }
  else {
   var found=false
   for(offset in 0..7) {
    val candidate=(now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR,offset);set(Calendar.HOUR_OF_DAY,item.hour);set(Calendar.MINUTE,item.minute);set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0) }
    if(candidate.get(Calendar.DAY_OF_WEEK) in item.days && candidate.timeInMillis>now.timeInMillis) { target.timeInMillis=candidate.timeInMillis;found=true;break }
   }
   if(!found) target.add(Calendar.DAY_OF_YEAR,1)
  }
  val pi=pending(context,item.id)
  if(Build.VERSION.SDK_INT>=23) {
   if(Build.VERSION.SDK_INT<31 || am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,target.timeInMillis,pi)
   else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,target.timeInMillis,pi)
  } else am.setExact(AlarmManager.RTC_WAKEUP,target.timeInMillis,pi)
 }
}

class AlarmReceiver: BroadcastReceiver() {
 override fun onReceive(context:Context,intent:Intent?) {
  val id=intent?.getLongExtra("id",-1L) ?: -1L
  val item=AlarmStore.load(context).firstOrNull{it.id==id} ?: return
  if(!item.enabled)return
  showAlarm(context,item)
  if(item.days.isNotEmpty()) AlarmStore.schedule(context,item)
  else {
   AlarmStore.save(context,AlarmStore.load(context).map{if(it.id==id)it.copy(enabled=false)else it})
  }
 }
 private fun showAlarm(context:Context,item:AlarmItem) {
  val manager=context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  val channelId="alarm_${item.id}"
  if(Build.VERSION.SDK_INT>=26 && manager.getNotificationChannel(channelId)==null) {
   val channel=NotificationChannel(channelId,"منبّه: ${item.title}",NotificationManager.IMPORTANCE_HIGH)
   channel.description="تنبيه المهمة ${item.title}"
   val uri=if(item.sound.isNotBlank()) android.net.Uri.parse(item.sound) else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
   channel.setSound(uri,AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
   channel.enableVibration(true)
   manager.createNotificationChannel(channel)
  }
  val full=Intent(context,AlarmRingingActivity::class.java).putExtra("id",item.id).putExtra("title",item.title).putExtra("sound",item.sound)
  val fullPi=PendingIntent.getActivity(context,item.id.toInt(),full,PendingIntent.FLAG_UPDATE_CURRENT or if(Build.VERSION.SDK_INT>=23) PendingIntent.FLAG_IMMUTABLE else 0)
  val notification=NotificationCompat.Builder(context,channelId)
   .setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("حان وقت: ${item.title}")
   .setContentText("افتح المنبّه لإيقاف الصوت أو تأجيله خمس دقائق")
   .setCategory(NotificationCompat.CATEGORY_ALARM).setPriority(NotificationCompat.PRIORITY_MAX)
   .setFullScreenIntent(fullPi,true).setContentIntent(fullPi).setAutoCancel(true).setOngoing(true).build()
  manager.notify(item.id.toInt(),notification)
 }
}

class BootReceiver: BroadcastReceiver() {
 override fun onReceive(context:Context,intent:Intent?) {
  if(intent?.action==Intent.ACTION_BOOT_COMPLETED || intent?.action=="android.intent.action.MY_PACKAGE_REPLACED") {
   AlarmStore.load(context).filter{it.enabled}.forEach{AlarmStore.schedule(context,it)}
  }
 }
}
