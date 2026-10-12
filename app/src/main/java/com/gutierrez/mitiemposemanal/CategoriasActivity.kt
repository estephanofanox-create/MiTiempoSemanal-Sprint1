package com.gutierrez.mitiemposemanal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class CategoriasActivity : AppCompatActivity() {

    private lateinit var db: DBHelper
    private lateinit var rvCategorias: RecyclerView
    private lateinit var adaptador: CategoriasAdapter
    private var lista = listOf<Categoria>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categorias)

        db = DBHelper(this)
        rvCategorias = findViewById(R.id.rvCategorias)
        rvCategorias.layoutManager = LinearLayoutManager(this)

        adaptador = CategoriasAdapter(lista)
        rvCategorias.adapter = adaptador

        findViewById<FloatingActionButton>(R.id.fabAgregar).setOnClickListener {
            dialogoAgregar()
        }

        cargarCategorias()
    }

    private fun cargarCategorias() {
        lista = db.listarCategorias()
        adaptador.actualizar(lista)
    }

    private fun dialogoAgregar() {
        val input = android.widget.EditText(this)
        input.hint = "Nombre de la categoría"
        AlertDialog.Builder(this)
            .setTitle("Nueva categoría")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = input.text.toString().trim()
                if (nombre.isEmpty()) {
                    Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                } else if (db.insertarCategoria(nombre)) {
                    Toast.makeText(this, "Categoría creada", Toast.LENGTH_SHORT).show()
                    cargarCategorias()
                } else {
                    Toast.makeText(this, "Esa categoría ya existe", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    inner class CategoriasAdapter(private var items: List<Categoria>) :
        RecyclerView.Adapter<CategoriasAdapter.VH>() {

        fun actualizar(nueva: List<Categoria>) {
            items = nueva
            notifyDataSetChanged()
        }

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvNombre: TextView = v.findViewById(R.id.tvNombre)
            val btnEditar: Button = v.findViewById(R.id.btnEditar)
            val btnEliminar: Button = v.findViewById(R.id.btnEliminar)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_categoria, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val cat = items[position]
            holder.tvNombre.text = cat.nombre

            holder.btnEditar.setOnClickListener {
                val input = android.widget.EditText(this@CategoriasActivity)
                input.setText(cat.nombre)
                AlertDialog.Builder(this@CategoriasActivity)
                    .setTitle("Editar categoría")
                    .setView(input)
                    .setPositiveButton("Guardar") { _, _ ->
                        val nuevo = input.text.toString().trim()
                        if (nuevo.isEmpty()) {
                            Toast.makeText(this@CategoriasActivity, "Nombre vacío", Toast.LENGTH_SHORT).show()
                        } else if (db.actualizarCategoria(cat.id, nuevo)) {
                            Toast.makeText(this@CategoriasActivity, "Actualizada", Toast.LENGTH_SHORT).show()
                            cargarCategorias()
                        } else {
                            Toast.makeText(this@CategoriasActivity, "Ese nombre ya existe", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }

            holder.btnEliminar.setOnClickListener {
                if (db.categoriaTieneTareas(cat.id)) {
                    Toast.makeText(this@CategoriasActivity,
                        "No se puede eliminar: tiene tareas", Toast.LENGTH_LONG).show()
                } else {
                    AlertDialog.Builder(this@CategoriasActivity)
                        .setTitle("Eliminar")
                        .setMessage("¿Eliminar '${cat.nombre}'?")
                        .setPositiveButton("Sí") { _, _ ->
                            db.eliminarCategoria(cat.id)
                            Toast.makeText(this@CategoriasActivity, "Eliminada", Toast.LENGTH_SHORT).show()
                            cargarCategorias()
                        }
                        .setNegativeButton("No", null)
                        .show()
                }
            }
        }

        override fun getItemCount() = items.size
    }
}