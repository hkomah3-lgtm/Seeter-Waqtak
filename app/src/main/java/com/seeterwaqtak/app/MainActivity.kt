package com.seeterwaqtak.app
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
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

data class Task(val id: Long, val title: String, val done: Boolean)
class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { SeeterApp() } }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeeterApp() {
 val context = LocalContext.current
 val prefs = remember { context.getSharedPreferences("seeter_data", 0) }
 fun readTasks(): List<Task> {
  val arr = JSONArray(prefs.getString("tasks", "[]"))
  return (0 until arr.length()).map { i -> val o = arr.getJSONObject(i); Task(o.getLong("id"), o.getString("title"), o.getBoolean("done")) }
 }
 var tasks by remember { mutableStateOf(readTasks()) }
 var entry by remember { mutableStateOf("") }
 var tab by remember { mutableStateOf("المهام") }
 var dark by remember { mutableStateOf(prefs.getBoolean("dark", false)) }
 fun save(list: List<Task>) {
  val arr = JSONArray()
  list.forEach { t -> arr.put(JSONObject().put("id", t.id).put("title", t.title).put("done", t.done)) }
  prefs.edit().putString("tasks", arr.toString()).apply()
  tasks = list
 }
 MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
  Scaffold(topBar = {
   TopAppBar(title = { Column { Text("سيطر على وقتك", fontWeight = FontWeight.Bold); Text("خطوة صغيرة اليوم تصنع فرقًا كبيرًا", style = MaterialTheme.typography.labelMedium) } },
    actions = { TextButton(onClick = { dark = !dark; prefs.edit().putBoolean("dark", dark).apply() }) { Text(if (dark) "فاتح" else "داكن") } })
  }, bottomBar = {
   NavigationBar { listOf("المهام", "العادات", "الأهداف").forEach { item ->
    NavigationBarItem(selected = tab == item, onClick = { tab = item }, icon = { Text(when(item) { "المهام" -> "✓"; "العادات" -> "↻"; else -> "◎" }) }, label = { Text(item) })
   } }
  }) { padding ->
   Column(Modifier.fillMaxSize().padding(padding).padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
     Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text("إنجازك يبدأ الآن", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
      Text("أنجز ${tasks.count { it.done }} من ${tasks.size} مهام اليوم")
      LinearProgressIndicator(progress = { if (tasks.isEmpty()) 0f else tasks.count { it.done }.toFloat() / tasks.size }, modifier = Modifier.fillMaxWidth())
     }
    }
    when(tab) {
     "المهام" -> {
      Text("مهام اليوم", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
       OutlinedTextField(value = entry, onValueChange = { entry = it }, modifier = Modifier.weight(1f), singleLine = true, label = { Text("أضف مهمة جديدة") })
       FilledIconButton(onClick = { if (entry.isNotBlank()) { save(tasks + Task(System.currentTimeMillis(), entry.trim(), false)); entry = "" } }) { Icon(Icons.Default.Add, contentDescription = "إضافة") }
      }
      if (tasks.isEmpty()) Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { Text("لا توجد مهام بعد. أضف أول مهمة!") }
      else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
       items(tasks, key = { it.id }) { task ->
        Card { Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
         Checkbox(checked = task.done, onCheckedChange = { checked -> save(tasks.map { if (it.id == task.id) it.copy(done = checked) else it }) })
         Text(task.title, Modifier.weight(1f))
         IconButton(onClick = { save(tasks.filterNot { it.id == task.id }) }) { Icon(Icons.Default.Delete, contentDescription = "حذف") }
        } }
       }
      }
     }
     "العادات" -> FeaturePlaceholder("العادات اليومية", "ابدأ بعادة بسيطة وكررها كل يوم. ستضاف متابعة السلاسل والتذكير في التحديث القادم.")
     else -> FeaturePlaceholder("أهدافك", "قسّم أهدافك الكبيرة إلى خطوات صغيرة قابلة للإنجاز.")
    }
   }
  }
 }
}
@Composable
fun FeaturePlaceholder(title: String, message: String) {
 Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
  Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
  Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
  Text(message)
 } }
}
