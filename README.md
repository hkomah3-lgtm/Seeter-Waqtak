# سيطر على وقتك — Seeter Waqtak

تطبيق Android عربي لإدارة المهام والعادات والأهداف وبناء روتين يومي أكثر انتظامًا.

## التقنية
- Kotlin
- Jetpack Compose + Material 3
- تخزين محلي عبر SharedPreferences/JSON
- إشعارات Android محلية

## البناء
يتولى GitHub Actions بناء نسخة Debug تلقائيًا عند الدفع إلى `main`، ويمكن تنزيل APK من صفحة Actions بعد نجاح التشغيل.

للبناء محليًا باستخدام Gradle 8.9 وJDK 17:

```bash
gradle assembleDebug
```

## الحالة
هذه نسخة تأسيسية قابلة للتطوير. لا تعتبر الميزات غير الموجودة في الواجهة منفذة بعد.
