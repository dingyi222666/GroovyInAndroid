# GroovyInAndroid

Run Groovy In Android

### How to use

```groovy
implementation "io.github.dingyi222666:groovy-android:1.0.1"
```

```kotlin
 val scriptFactory = GroovyScriptFactory(
    getExternalFilesDir("groovy-test")?.apply {
        mkdirs()
    } ?: error("")
)
scriptFactory
    .evaluate(
        """
                import android.widget.Toast
                import xx.xx.YouApplication
                Toast.makeText(YouApplication.instance, "Hello World for groovy", Toast.LENGTH_SHORT).show()
            """.trimIndent()
    )
```

