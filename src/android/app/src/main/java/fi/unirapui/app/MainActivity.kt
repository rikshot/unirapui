package fi.unirapui.app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import fi.unirapui.lib.Unirapui

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(Unirapui.create(applicationContext, 8000U))
    }
}