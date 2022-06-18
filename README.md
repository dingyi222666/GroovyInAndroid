# GroovyInAndroid

Run Groovy in Android, compile groovy code to dex using D8 and run it in Android.


## Quick Start

```groovy
implementation "io.github.dingyi222666:groovy-android:1.0.7-beta1"
```

### ~~Using ScriptFactory~~

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

### Using DynamicGrooidClassLoader

```kotlin
val classLoader = DynamicGrooidClassLoader(this.classLoader)
val scriptClass =
    kotlin.runCatching { classLoader.parseClass(groovyCode) as Class<Script> }
        .onFailure {
            //do something in parseClass failure
            it.printStackTrace()
        }
        .getOrNull() ?: error("parseClass failure")

scriptClass.newInstance().run()
```


### TODO
 - [x] Support caching dex 
    - [x] always compile the dex into memory, we don't need to be concerned about repeatedly compiling the dex causing much cache dex