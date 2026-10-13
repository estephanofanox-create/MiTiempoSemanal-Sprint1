package com.gutierrez.mitiemposemanal

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnCategorias).setOnClickListener {
            startActivity(Intent(this, CategoriasActivity::class.java))
        }
        findViewById<Button>(R.id.btnTareas).setOnClickListener {
            startActivity(Intent(this, TareasActivity::class.java))
        }
        findViewById<Button>(R.id.btnEventos).setOnClickListener {
            startActivity(Intent(this, EventosActivity::class.java))
        }
    }
}