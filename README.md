# GroovyInAndroid

Run Groovy on Android , compile groovy code to dex using D8 and run it in Android.


### How to use

```groovy
implementation "io.github.dingyi222666:groovy-android:1.0.3"
```

```kotlin
 val scriptFactory = GroovyScriptFactory()
scriptFactory
    .evaluate(
        """
                import android.widget.Toast
                import xx.xx.YouApplication
                Toast.makeText(YouApplication.instance, "Hello World for groovy", Toast.LENGTH_SHORT).show()
            """.trimIndent()
    )
```

