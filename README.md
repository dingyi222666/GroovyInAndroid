# GroovyInAndroid

Run Groovy on Android, compile groovy code to dex using D8 and run it in Android.

## Quick Start

```groovy
implementation "io.github.dingyi222666:groovy-android:1.0.5"
```

### Using ScriptFactory

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

### Using DynamicGrooidDexClassLoader

```kotlin
val classLoader = DynamicGrooidDexClassLoader(this.classLoader)
val scriptClass =
    classLoader.parseClass(
        """
                import android.widget.Toast
                import xx.xx.YouApplication
                Toast.makeText(YouApplication.instance, "Hello World for groovy", Toast.LENGTH_SHORT).show()
            """.trimIndent()
    ) as Class<Script>

scriptClass.newInstance().run()

```


### TODO
 - [ ] Support caching dex