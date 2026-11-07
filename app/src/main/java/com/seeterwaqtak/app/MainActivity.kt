package com.seeterwaqtak.app
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class Entry(val id: Long, val title: String, val done: Boolean = false, val progress: Int = 0)
class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { App() } }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun App() {
 val ctx = LocalContext.current
 val prefs = remember { ctx.getSharedPreferences("seeter_data", 0) }
 fun read(key: String): List<Entry> {
  val a = JSONArray(prefs.getString(key, "[]") ?: "[]")
  return (0 until a.length()).map { i -> a.getJSONObject(i).let { Entry(it.getLong("id"), it.getString("title"), it.optBoolean("done"), it.optInt("progress")) } }
 }
 var tasks by remember { mutableStateOf(read("tasks")) }
 var habits by remember { mutableStateOf(read("habits")) }
 var goals by remember { mutableStateOf(read("goals")) }
 var dark by remember { mutableStateOf(prefs.getBoolean("dark", true)) }
 var tab by remember { mutableStateOf("الرئيسية") }
 var dialog by remember { mutableStateOf("") }
 var entry by remember { mutableStateOf("") }
 fun save(key: String, list: List<Entry>) {
  val a = JSONArray()
  list.forEach { a.put(JSONObject().put("id",it.id).put("title",it.title).put("done",it.done).put("progress",it.progress)) }
  prefs.edit().putString(key,a.toString()).apply()
  when(key) { "tasks" -> tasks=list; "habits" -> habits=list; else -> goals=list }
 }
 MaterialTheme(colorScheme = if(dark) darkColorScheme() else lightColorScheme()) {
  Scaffold(topBar = { TopAppBar(title={ Column { Text("سيطر على وقتك",fontWeight=FontWeight.Bold); Text("خطّط ليومك واصنع إنجازك",style=MaterialTheme.typography.labelMedium) } },actions={ TextButton(onClick={dark=!dark;prefs.edit().putBoolean("dark",dark).apply()}) { Text(if(dark)"فاتح" else "داكن") } }) },
   bottomBar={ NavigationBar { listOf("الرئيسية","المهام","العادات","الأهداف","الإحصاءات","الإعدادات").forEach { t -> NavigationBarItem(selected=tab==t,onClick={tab=t},icon={Text(when(t){"الرئيسية"->"⌂";"المهام"->"✓";"العادات"->"↻";"الأهداف"->"◎";"الإحصاءات"->"▥";else->"⚙"})},label={Text(t,style=MaterialTheme.typography.labelSmall)}) } } }
  ) { pad ->
   LazyColumn(Modifier.fillMaxSize().padding(pad).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=16.dp)) {
    when(tab) {
     "الرئيسية" -> {
      item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) { Text("أهلاً بك 👋",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold); Text(SimpleDateFormat("EEEE، d MMMM yyyy",Locale("ar")).format(Date())); Text("إنجازك: ${tasks.count{it.done}} من ${tasks.size} مهام"); LinearProgressIndicator(progress={if(tasks.isEmpty())0f else tasks.count{it.done}.toFloat()/tasks.size},modifier=Modifier.fillMaxWidth()) } } }
      item { Summary("العادات المكتملة اليوم","${habits.count{it.done}} من ${habits.size}") }
      item { Summary("الأهداف المكتملة","${goals.count{it.progress>=100}} من ${goals.size}") }
      item { Text("مهام اليوم",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold) }
      items(tasks.take(5),key={it.id}) { e -> EntryRow(e,{v->save("tasks",tasks.map{if(it.id==e.id)it.copy(done=v)else it})},{save("tasks",tasks.filterNot{it.id==e.id})}) }
     }
     "المهام","العادات","الأهداف" -> {
      val key=when(tab){"المهام"->"tasks";"العادات"->"habits";else->"goals"}
      val list=when(key){"tasks"->tasks;"habits"->habits;else->goals}
      item { Row(verticalAlignment=Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(tab,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold); Text(when(key){"tasks"->"نظّم أعمالك وأنجزها";"habits"->"تابع عاداتك اليومية";else->"قسّم أهدافك إلى خطوات"}) }; FilledIconButton(onClick={dialog=key;entry=""}) { Icon(Icons.Default.Add,contentDescription="إضافة") } } }
      if(list.isEmpty()) item { Card(Modifier.fillMaxWidth()) { Text("لا توجد عناصر بعد. اضغط + للإضافة.",Modifier.padding(18.dp)) } }
      items(list,key={it.id}) { e ->
       if(key=="goals") Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) { Row(verticalAlignment=Alignment.CenterVertically) { Text(e.title,Modifier.weight(1f),fontWeight=FontWeight.Bold); Text("${e.progress}%"); IconButton(onClick={save(key,list.filterNot{it.id==e.id})}) { Icon(Icons.Default.Delete,contentDescription="حذف") } }; LinearProgressIndicator(progress={e.progress/100f},modifier=Modifier.fillMaxWidth()); Row { OutlinedButton(onClick={save(key,list.map{if(it.id==e.id)it.copy(progress=(it.progress-10).coerceAtLeast(0))else it})}) { Text("−10٪") }; Spacer(Modifier.width(8.dp)); Button(onClick={save(key,list.map{if(it.id==e.id)it.copy(progress=(it.progress+10).coerceAtMost(100))else it})}) { Text("+10٪") } } } }
       else EntryRow(e,{v->save(key,list.map{if(it.id==e.id)it.copy(done=v)else it})},{save(key,list.filterNot{it.id==e.id})})
      }
     }
     "الإحصاءات" -> {
      item { Text("إحصاءاتك",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold) }
      item { Summary("المهام المكتملة","${tasks.count{it.done}} / ${tasks.size}") }
      item { Summary("نسبة إنجاز المهام",if(tasks.isEmpty())"لا توجد بيانات" else "${tasks.count{it.done}*100/tasks.size}٪") }
      item { Summary("العادات المنجزة اليوم","${habits.count{it.done}} / ${habits.size}") }
      item { Summary("الأهداف المحققة","${goals.count{it.progress>=100}} / ${goals.size}") }
     }
     "الإعدادات" -> {
      item { Text("الإعدادات والخصوصية",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold) }
      item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Row(verticalAlignment=Alignment.CenterVertically) { Text("الوضع الداكن",Modifier.weight(1f)); Switch(dark,{dark=it;prefs.edit().putBoolean("dark",it).apply()}) }; Text("بياناتك محفوظة محلياً على هذا الجهاز ولا تحتاج إلى حساب.") } } }
      item { OutlinedButton(onClick={val data=JSONObject().put("tasks",JSONArray(prefs.getString("tasks","[]"))).put("habits",JSONArray(prefs.getString("habits","[]"))).put("goals",JSONArray(prefs.getString("goals","[]"))).toString(2); val send=android.content.Intent(android.content.Intent.ACTION_SEND).setType("application/json").putExtra(android.content.Intent.EXTRA_TEXT,data);ctx.startActivity(android.content.Intent.createChooser(send,"تصدير بياناتك"))},modifier=Modifier.fillMaxWidth()) { Text("تصدير نسخة من بياناتي") } }
      item { OutlinedButton(onClick={dialog="reset"},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.outlinedButtonColors(contentColor=MaterialTheme.colorScheme.error)) { Text("حذف جميع البيانات") } }
      item { Text("الإصدار 1.1.0",style=MaterialTheme.typography.labelSmall) }
     }
    }
   }
  }
  if(dialog in listOf("tasks","habits","goals")) AlertDialog(onDismissRequest={dialog=""},title={Text(when(dialog){"tasks"->"مهمة جديدة";"habits"->"عادة جديدة";else->"هدف جديد"})},text={OutlinedTextField(entry,{entry=it},label={Text("اكتب الاسم")},singleLine=true)},confirmButton={TextButton(onClick={if(entry.isNotBlank()){val key=dialog;val list=when(key){"tasks"->tasks;"habits"->habits;else->goals};save(key,list+Entry(System.currentTimeMillis(),entry.trim()))};dialog=""}){Text("إضافة")}},dismissButton={TextButton(onClick={dialog=""}){Text("إلغاء")}})
  if(dialog=="reset") AlertDialog(onDismissRequest={dialog=""},title={Text("حذف جميع البيانات؟")},text={Text("سيتم حذف المهام والعادات والأهداف من هذا الجهاز.")},confirmButton={TextButton(onClick={save("tasks",emptyList());save("habits",emptyList());save("goals",emptyList());dialog=""},colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.error)){Text("حذف نهائياً")}},dismissButton={TextButton(onClick={dialog=""}){Text("إلغاء")}})
 }
}
@Composable private fun EntryRow(e:Entry,onToggle:(Boolean)->Unit,onDelete:()->Unit) {
 Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(10.dp),verticalAlignment=Alignment.CenterVertically) { Checkbox(e.done,onToggle); Text(e.title,Modifier.weight(1f),fontWeight=FontWeight.Medium); IconButton(onClick=onDelete){Icon(Icons.Default.Delete,contentDescription="حذف")} } }
}
@Composable private fun Summary(title:String,value:String) {
 Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(title,style=MaterialTheme.typography.bodyMedium); Text(value,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold) } }
}
