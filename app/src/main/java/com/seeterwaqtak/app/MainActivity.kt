package com.seeterwaqtak.app

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.app.TimePickerDialog
import android.media.RingtoneManager
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: android.os.Bundle?) {
  super.onCreate(savedInstanceState)
  setContent { AlarmApp() }
 }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun AlarmApp() {
 val context = LocalContext.current
 val prefs = remember { context.getSharedPreferences("seeter_alarms", Context.MODE_PRIVATE) }
 var alarms by remember { mutableStateOf(AlarmStore.load(context)) }
 var showDialog by remember { mutableStateOf(false) }
 var title by remember { mutableStateOf("") }
 var hour by remember { mutableIntStateOf(8) }
 var minute by remember { mutableIntStateOf(0) }
 var days by remember { mutableStateOf(setOf<Int>(Calendar.MONDAY,Calendar.TUESDAY,Calendar.WEDNESDAY,Calendar.THURSDAY,Calendar.FRIDAY,Calendar.SATURDAY,Calendar.SUNDAY)) }
 var sound by remember { mutableStateOf(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.toString() ?: "") }
 var soundName by remember { mutableStateOf("نغمة المنبّه الافتراضية") }
 var dark by remember { mutableStateOf(prefs.getBoolean("dark", true)) }
 val soundPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
  if (result.resultCode == android.app.Activity.RESULT_OK) {
   val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
   if (uri != null) {
    sound = uri.toString()
    soundName = RingtoneManager.getRingtone(context, uri)?.getTitle(context) ?: "نغمة مختارة"
   }
  }
 }
 val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
 LaunchedEffect(Unit) {
  if (Build.VERSION.SDK_INT >= 33) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
 }
 fun refresh() { alarms = AlarmStore.load(context) }
 fun saveAlarm(item: AlarmItem) {
  val updated = alarms.filterNot { it.id == item.id } + item
  AlarmStore.save(context, updated)
  if (item.enabled) AlarmStore.schedule(context, item) else AlarmStore.cancel(context, item.id)
  refresh()
 }
 val wine = Color(0xFF8B1E3F)
 MaterialTheme(colorScheme=lightColorScheme(primary=wine,onPrimary=Color.White,secondary=Color(0xFFB64B68),background=Color(0xFFFFF9FB),surface=Color.White,onSurface=Color(0xFF321820),surfaceVariant=Color(0xFFF7E9EE),outline=Color(0xFFE8CCD5))) {
  Scaffold(
   containerColor=Color(0xFFFFF9FB),
   topBar={ TopAppBar(colors=TopAppBarDefaults.topAppBarColors(containerColor=Color.White,titleContentColor=wine),title={ Column { Text("سيطر على وقتك",fontWeight=FontWeight.ExtraBold); Text("منبّهات مهامك، في وقتها تماماً",style=MaterialTheme.typography.labelMedium,color=Color(0xFF9A6475)) } }) },
   floatingActionButton={ FloatingActionButton(containerColor=wine,contentColor=Color.White,shape=RoundedCornerShape(18.dp),onClick={ title="";hour=8;minute=0;days=setOf(Calendar.MONDAY,Calendar.TUESDAY,Calendar.WEDNESDAY,Calendar.THURSDAY,Calendar.FRIDAY,Calendar.SATURDAY,Calendar.SUNDAY);sound=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.toString() ?: "";soundName="نغمة المنبّه الافتراضية";showDialog=true }) { Icon(Icons.Default.Add,contentDescription="منبّه جديد") } }
  ) { padding ->
   LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=90.dp)) {
    item {
     Card(Modifier.fillMaxWidth().shadow(5.dp,RoundedCornerShape(24.dp)),shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=Color.White),elevation=CardDefaults.cardElevation(defaultElevation=2.dp)) { Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
      Text("حوّل كل مهمة إلى منبّه",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.ExtraBold,color=wine)
      Text("اختر المهمة والوقت وأيام التكرار وصوت التنبيه. سيوقظك الهاتف عند الموعد حتى عندما تكون الشاشة مغلقة.")
      Text("عدد المنبّهات: ${alarms.size}",style=MaterialTheme.typography.labelLarge,color=wine,fontWeight=FontWeight.Bold)
     } }
    }
    if(alarms.isEmpty()) item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) { Text("لا توجد منبّهات بعد",fontWeight=FontWeight.Bold);Text("اضغط زر + لإنشاء أول منبّه لمهمتك.") } } }
    items(alarms.sortedWith(compareBy<AlarmItem>{it.hour}.thenBy{it.minute}),key={it.id}) { alarm ->
     Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
      Row(verticalAlignment=Alignment.CenterVertically) {
       Column(Modifier.weight(1f)) {
        Text(alarm.title,fontWeight=FontWeight.ExtraBold,style=MaterialTheme.typography.titleMedium,color=wine)
        Text(String.format(Locale.getDefault(),"%02d:%02d",alarm.hour,alarm.minute),style=MaterialTheme.typography.headlineMedium,color=wine,fontWeight=FontWeight.ExtraBold)
       }
       Switch(checked=alarm.enabled,onCheckedChange={saveAlarm(alarm.copy(enabled=it))})
       IconButton(onClick={AlarmStore.cancel(context,alarm.id);AlarmStore.save(context,alarms.filterNot{it.id==alarm.id});refresh()}) { Icon(Icons.Default.Delete,contentDescription="حذف") }
      }
      Text(if(alarm.days.isEmpty()) "مرة واحدة" else "التكرار: "+AlarmStore.daysLabel(alarm.days),style=MaterialTheme.typography.bodyMedium)
      Text("الصوت: ${alarm.soundName}",style=MaterialTheme.typography.bodySmall)
     } }
    }
    item { Text("ملاحظة: اسمح للتطبيق بالإشعارات والمنبّهات الدقيقة من إعدادات الهاتف حتى يعمل التنبيه في موعده.",style=MaterialTheme.typography.bodySmall) }
   }
  }
  if(showDialog) AlertDialog(
   onDismissRequest={showDialog=false},
   title={Text("إنشاء منبّه لمهمة")},
   text={
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
     OutlinedTextField(value=title,onValueChange={title=it},label={Text("اسم المهمة")},singleLine=true)
     Button(onClick={TimePickerDialog(context,{_,h,m->hour=h;minute=m},hour,minute,true).show()},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),colors=ButtonDefaults.buttonColors(containerColor=wine)) { Text("وقت التنبيه: "+String.format(Locale.getDefault(),"%02d:%02d",hour,minute)) }
     Text("أيام التكرار",fontWeight=FontWeight.Bold)
     Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
      listOf(Calendar.SATURDAY to "س",Calendar.SUNDAY to "ح",Calendar.MONDAY to "ن",Calendar.TUESDAY to "ث",Calendar.WEDNESDAY to "ر",Calendar.THURSDAY to "خ",Calendar.FRIDAY to "ج").forEach { (day,label) ->
       FilterChip(selected=day in days,onClick={days=if(day in days) days-day else days+day},label={Text(label)})
      }
     }
     Text(if(days.isEmpty()) "لن يتكرر — تنبيه لمرة واحدة" else "سيتكرر في الأيام المحددة")
     OutlinedButton(onClick={
      val intent=Intent(RingtoneManager.ACTION_RINGTONE_PICKER).putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,RingtoneManager.TYPE_ALARM).putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE,"اختر صوت المنبّه").putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,if(sound.isNotBlank()) Uri.parse(sound) else null)
      soundPicker.launch(intent)
     },modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),colors=ButtonDefaults.buttonColors(containerColor=wine)) { Text("♫ اختيار صوت أو نغمة: $soundName") }
    }
   },
   confirmButton={ Button(onClick={
    if(title.isNotBlank()) {
     val item=AlarmItem(System.currentTimeMillis(),title.trim(),hour,minute,days,sound,soundName,true)
     saveAlarm(item)
     if(Build.VERSION.SDK_INT>=31) {
      val manager=context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
      if(!manager.canScheduleExactAlarms()) context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
     }
    }
    showDialog=false
   },shape=RoundedCornerShape(16.dp),colors=ButtonDefaults.buttonColors(containerColor=wine)) { Text("حفظ وتفعيل") } },
   dismissButton={TextButton(onClick={showDialog=false}){Text("إلغاء")}}
  )
 }
}
