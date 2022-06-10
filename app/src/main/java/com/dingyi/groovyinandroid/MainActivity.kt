package com.dingyi.groovyinandroid

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.dingyi.groovyinandroid.R
import com.dingyi.groovy.android.GroovyScriptFactory
import java.io.File

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var scriptFactory: GroovyScriptFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        findViewById<View>(R.id.test).setOnClickListener(this)

        scriptFactory = GroovyScriptFactory()

    }

    override fun onClick(v: View) {
        scriptFactory
            .evaluate(
                """
                import android.widget.Toast
                import com.dingyi.groovyinandroid.MainApplication
                Toast.makeText(MainApplication.instance, "Hello World for groovy", Toast.LENGTH_SHORT).show()
            """.trimIndent()
            )
    }


}