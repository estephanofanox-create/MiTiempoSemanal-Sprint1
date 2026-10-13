package com.gutierrez.mitiemposemanal

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class EventosActivity : AppCompatActivity() {

    private lateinit var db: DBHelper
    private lateinit var etBuscar: EditText
    private lateinit var tvSugerencias: TextView
    private lateinit var tvTareaSeleccionada: TextView
    private lateinit var btnIniciar: Button
    private lateinit var btnFinalizar: Button
    private lateinit var rvEventos: RecyclerView
    private lateinit var adaptador: EventosAdapter

    private var tareaSeleccionada: Tarea? = null
    private var eventoActual: Evento? = null

    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_eventos)

        db = DBHelper(this)

        etBuscar = findViewById(R.id.etBuscar)
        tvSugerencias = findViewById(R.id.tvSugerencias)
        tvTareaSeleccionada = findViewById(R.id.tvTareaSeleccionada)
        btnIniciar = findViewById(R.id.btnIniciar)
        btnFinalizar = findViewById(R.id.btnFinalizar)
        rvEventos = findViewById(R.id.rvEventos)

        rvEventos.layoutManager = LinearLayoutManager(this)
        adaptador = EventosAdapter(listOf())
        rvEventos.adapter = adaptador

        // Buscar mientras escribe
        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {
                buscar(s?.toString()?.trim() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnIniciar.setOnClickListener { iniciarEvento() }
        btnFinalizar.setOnClickListener { finalizarEvento() }

        recuperarEstado()
        cargarEventos()
    }

    private fun buscar(texto: String) {
        if (texto.length < 2) {
            tvSugerencias.text = ""
            return
        }
        val tareas = db.listarTareas().filter { it.nombre.contains(texto, ignoreCase = true) }
        if (tareas.isEmpty()) {
            tvSugerencias.text = "No existe. Pulsa Iniciar para crearla."
        } else {
            tvSugerencias.text = tareas.joinToString("\n") { "${it.nombre} · ${db.nombreCategoria(it.categoriaId)}" }
        }
    }

    private fun iniciarEvento() {
        if (eventoActual != null) {
            Toast.makeText(this, "Ya hay un evento en curso", Toast.LENGTH_SHORT).show()
            return
        }
        val nombre = etBuscar.text.toString().trim()
        if (nombre.isEmpty()) {
            Toast.makeText(this, "Escribe el nombre de la tarea", Toast.LENGTH_SHORT).show()
            return
        }

        // Buscar tarea existente
        val existente = db.listarTareas().firstOrNull { it.nombre.equals(nombre, ignoreCase = true) }

        if (existente != null) {
            // Ya existe: usarla
            tareaSeleccionada = existente
            crearEvento(existente.id)
        } else {
            // No existe: pedir categoría y crearla
            pedirCategoriaYCrear(nombre)
        }
    }

    private fun pedirCategoriaYCrear(nombre: String) {
        val categorias = db.listarCategorias()
        if (categorias.isEmpty()) {
            Toast.makeText(this, "Primero crea una categoría", Toast.LENGTH_SHORT).show()
            return
        }

        val spinner = Spinner(this)
        val nombres = categorias.map { it.nombre }
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, nombres)

        val contenedor = LinearLayout(this)
        contenedor.orientation = LinearLayout.VERTICAL
        contenedor.setPadding(40, 20, 40, 20)
        contenedor.addView(spinner)

        AlertDialog.Builder(this)
            .setTitle("Crear tarea '$nombre'")
            .setMessage("Elige su categoría:")
            .setView(contenedor)
            .setPositiveButton("Crear e Iniciar") { _, _ ->
                val catId = categorias[spinner.selectedItemPosition].id
                if (db.insertarTarea(nombre, catId)) {
                    val nueva = db.listarTareas().first { it.nombre.equals(nombre, ignoreCase = true) }
                    tareaSeleccionada = nueva
                    crearEvento(nueva.id)
                } else {
                    Toast.makeText(this, "No se pudo crear", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun crearEvento(tareaId: Int) {
        val inicio = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val id = db.insertarEvento(tareaId, inicio)
        if (id > 0) {
            eventoActual = db.eventoEnCurso()
            actualizarBotones()
            cargarEventos()
            Toast.makeText(this, "Evento iniciado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun finalizarEvento() {
        val evento = eventoActual ?: return
        val fin = LocalDateTime.now()
        val inicio = LocalDateTime.parse(evento.inicio, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val duracion = Duration.between(inicio, fin).seconds
        if (db.finalizarEvento(evento.id, fin.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), duracion)) {
            eventoActual = null
            tareaSeleccionada = null
            etBuscar.setText("")
            tvSugerencias.text = ""
            tvTareaSeleccionada.text = "Ninguna tarea seleccionada"
            actualizarBotones()
            cargarEventos()
            Toast.makeText(this, "Evento finalizado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun recuperarEstado() {
        eventoActual = db.eventoEnCurso()
        if (eventoActual != null) {
            val tarea = db.listarTareas().firstOrNull { it.id == eventoActual!!.tareaId }
            tareaSeleccionada = tarea
            if (tarea != null) {
                etBuscar.setText(tarea.nombre)
                tvTareaSeleccionada.text = "${tarea.nombre} · ${db.nombreCategoria(tarea.categoriaId)}"
            }
        }
        actualizarBotones()
    }

    private fun actualizarBotones() {
        val enCurso = eventoActual != null
        btnIniciar.isEnabled = !enCurso
        btnFinalizar.isEnabled = enCurso

        if (enCurso) {
            val inicio = LocalDateTime.parse(eventoActual!!.inicio, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val tarea = tareaSeleccionada
            if (tarea != null) {
                tvTareaSeleccionada.text =
                    "▶ ${tarea.nombre} · ${db.nombreCategoria(tarea.categoriaId)}\nInicio: ${inicio.format(fmt)}"
            }
        }
    }

    private fun cargarEventos() {
        adaptador.actualizar(db.listarEventos())
    }

    inner class EventosAdapter(private var items: List<Evento>) :
        RecyclerView.Adapter<EventosAdapter.VH>() {

        fun actualizar(nueva: List<Evento>) {
            items = nueva
            notifyDataSetChanged()
        }

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvTarea: TextView = v.findViewById(R.id.tvEventoTarea)
            val tvInicio: TextView = v.findViewById(R.id.tvEventoInicio)
            val tvFin: TextView = v.findViewById(R.id.tvEventoFin)
            val tvDuracion: TextView = v.findViewById(R.id.tvEventoDuracion)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_evento, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val ev = items[position]
            val nombreTarea = db.nombreTarea(ev.tareaId)
            val catId = db.categoriaDeTarea(ev.tareaId)
            val nombreCat = db.nombreCategoria(catId)

            holder.tvTarea.text = "$nombreTarea · $nombreCat"

            val inicio = LocalDateTime.parse(ev.inicio, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            holder.tvInicio.text = "Inicio: ${inicio.format(fmt)}"

            if (ev.fin != null) {
                val fin = LocalDateTime.parse(ev.fin, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                holder.tvFin.text = "Fin: ${fin.format(fmt)}"
                val min = (ev.duracion ?: 0) / 60
                holder.tvDuracion.text = "Duración: $min min"
            } else {
                holder.tvFin.text = "Fin: (en curso)"
                holder.tvDuracion.text = "Duración: --"
            }
        }

        override fun getItemCount() = items.size
    }
}