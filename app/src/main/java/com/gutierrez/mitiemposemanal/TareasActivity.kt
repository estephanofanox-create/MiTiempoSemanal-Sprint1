package com.gutierrez.mitiemposemanal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class TareasActivity : AppCompatActivity() {

    private lateinit var db: DBHelper
    private lateinit var rvTareas: RecyclerView
    private lateinit var adaptador: TareasAdapter
    private var lista = listOf<Tarea>()
    private var categorias = listOf<Categoria>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tareas)

        db = DBHelper(this)
        rvTareas = findViewById(R.id.rvTareas)
        rvTareas.layoutManager = LinearLayoutManager(this)

        adaptador = TareasAdapter(lista)
        rvTareas.adapter = adaptador

        findViewById<FloatingActionButton>(R.id.fabAgregarTarea).setOnClickListener {
            dialogoAgregar()
        }

        cargarTareas()
    }

    private fun cargarTareas() {
        lista = db.listarTareas()
        categorias = db.listarCategorias()
        adaptador.actualizar(lista)
    }

    private fun dialogoAgregar() {
        if (categorias.isEmpty()) {
            Toast.makeText(this, "Primero crea una categoría", Toast.LENGTH_SHORT).show()
            return
        }

        val contenedor = LinearLayout(this)
        contenedor.orientation = LinearLayout.VERTICAL
        contenedor.setPadding(40, 20, 40, 20)

        val input = EditText(this)
        input.hint = "Nombre de la tarea"

        val spinner = Spinner(this)
        val nombres = categorias.map { it.nombre }
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, nombres)

        contenedor.addView(input)
        contenedor.addView(spinner)

        AlertDialog.Builder(this)
            .setTitle("Nueva tarea")
            .setView(contenedor)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = input.text.toString().trim()
                val catId = categorias[spinner.selectedItemPosition].id
                if (nombre.isEmpty()) {
                    Toast.makeText(this, "La tarea no puede estar vacía", Toast.LENGTH_SHORT).show()
                } else if (db.insertarTarea(nombre, catId)) {
                    Toast.makeText(this, "Tarea creada", Toast.LENGTH_SHORT).show()
                    cargarTareas()
                } else {
                    Toast.makeText(this, "Esa tarea ya existe", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    inner class TareasAdapter(private var items: List<Tarea>) :
        RecyclerView.Adapter<TareasAdapter.VH>() {

        fun actualizar(nueva: List<Tarea>) {
            items = nueva
            notifyDataSetChanged()
        }

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvTarea: TextView = v.findViewById(R.id.tvTarea)
            val btnEditar: Button = v.findViewById(R.id.btnEditarTarea)
            val btnEliminar: Button = v.findViewById(R.id.btnEliminarTarea)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_tarea, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val tarea = items[position]
            val nombreCat = db.nombreCategoria(tarea.categoriaId)
            holder.tvTarea.text = "${tarea.nombre} · $nombreCat"

            holder.btnEditar.setOnClickListener {
                val contenedor = LinearLayout(this@TareasActivity)
                contenedor.orientation = LinearLayout.VERTICAL
                contenedor.setPadding(40, 20, 40, 20)

                val input = EditText(this@TareasActivity)
                input.setText(tarea.nombre)

                val spinner = Spinner(this@TareasActivity)
                val nombres = categorias.map { it.nombre }
                spinner.adapter = ArrayAdapter(this@TareasActivity, android.R.layout.simple_spinner_dropdown_item, nombres)
                spinner.setSelection(categorias.indexOfFirst { it.id == tarea.categoriaId })

                contenedor.addView(input)
                contenedor.addView(spinner)

                AlertDialog.Builder(this@TareasActivity)
                    .setTitle("Editar tarea")
                    .setView(contenedor)
                    .setPositiveButton("Guardar") { _, _ ->
                        val nuevo = input.text.toString().trim()
                        val catId = categorias[spinner.selectedItemPosition].id
                        if (nuevo.isEmpty()) {
                            Toast.makeText(this@TareasActivity, "Nombre vacío", Toast.LENGTH_SHORT).show()
                        } else if (db.actualizarTarea(tarea.id, nuevo, catId)) {
                            Toast.makeText(this@TareasActivity, "Actualizada", Toast.LENGTH_SHORT).show()
                            cargarTareas()
                        } else {
                            Toast.makeText(this@TareasActivity, "Ese nombre ya existe", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }

            holder.btnEliminar.setOnClickListener {
                AlertDialog.Builder(this@TareasActivity)
                    .setTitle("Eliminar")
                    .setMessage("¿Eliminar '${tarea.nombre}'?")
                    .setPositiveButton("Sí") { _, _ ->
                        db.eliminarTarea(tarea.id)
                        Toast.makeText(this@TareasActivity, "Eliminada", Toast.LENGTH_SHORT).show()
                        cargarTareas()
                    }
                    .setNegativeButton("No", null)
                    .show()
            }
        }

        override fun getItemCount() = items.size
    }
}