package com.gutierrez.mitiemposemanal

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper(context: Context) : SQLiteOpenHelper(context, "mitiempo.db", null, 2) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE categorias (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL UNIQUE
            )
        """)

        db.execSQL("""
            CREATE TABLE tareas (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL UNIQUE,
                categoria_id INTEGER NOT NULL,
                FOREIGN KEY (categoria_id) REFERENCES categorias(id)
            )
        """)

        db.execSQL("""
            CREATE TABLE eventos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                tarea_id INTEGER NOT NULL,
                inicio TEXT,
                fin TEXT,
                duracion INTEGER,
                FOREIGN KEY (tarea_id) REFERENCES tareas(id)
            )
        """)

        val categoriasIniciales = listOf("Estudio", "Trabajo", "Descanso", "Ocio")
        for (nombre in categoriasIniciales) {
            val values = ContentValues().apply { put("nombre", nombre) }
            db.insert("categorias", null, values)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Sprint 2: solo agrega tabla eventos sin perder datos del Sprint 1
        if (oldVersion < 2) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS eventos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    tarea_id INTEGER NOT NULL,
                    inicio TEXT,
                    fin TEXT,
                    duracion INTEGER,
                    FOREIGN KEY (tarea_id) REFERENCES tareas(id)
                )
            """)
        }
    }

    // ==================== CATEGORÍAS ====================
    fun listarCategorias(): List<Categoria> {
        val lista = mutableListOf<Categoria>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT id, nombre FROM categorias ORDER BY nombre", null)
        while (cursor.moveToNext()) {
            lista.add(Categoria(cursor.getInt(0), cursor.getString(1)))
        }
        cursor.close()
        return lista
    }

    fun insertarCategoria(nombre: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply { put("nombre", nombre) }
        return db.insert("categorias", null, values) != -1L
    }

    fun actualizarCategoria(id: Int, nombre: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply { put("nombre", nombre) }
        return db.update("categorias", values, "id = ?", arrayOf(id.toString())) > 0
    }

    fun categoriaTieneTareas(id: Int): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM tareas WHERE categoria_id = ?", arrayOf(id.toString()))
        cursor.moveToFirst()
        val cantidad = cursor.getInt(0)
        cursor.close()
        return cantidad > 0
    }

    fun eliminarCategoria(id: Int): Boolean {
        val db = writableDatabase
        return db.delete("categorias", "id = ?", arrayOf(id.toString())) > 0
    }

    // ==================== TAREAS ====================
    fun listarTareas(): List<Tarea> {
        val lista = mutableListOf<Tarea>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT id, nombre, categoria_id FROM tareas ORDER BY nombre", null)
        while (cursor.moveToNext()) {
            lista.add(Tarea(cursor.getInt(0), cursor.getString(1), cursor.getInt(2)))
        }
        cursor.close()
        return lista
    }

    fun insertarTarea(nombre: String, categoriaId: Int): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("nombre", nombre)
            put("categoria_id", categoriaId)
        }
        return db.insert("tareas", null, values) != -1L
    }

    fun actualizarTarea(id: Int, nombre: String, categoriaId: Int): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("nombre", nombre)
            put("categoria_id", categoriaId)
        }
        return db.update("tareas", values, "id = ?", arrayOf(id.toString())) > 0
    }

    fun eliminarTarea(id: Int): Boolean {
        val db = writableDatabase
        return db.delete("tareas", "id = ?", arrayOf(id.toString())) > 0
    }

    fun nombreCategoria(id: Int): String {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT nombre FROM categorias WHERE id = ?", arrayOf(id.toString()))
        var nombre = ""
        if (cursor.moveToFirst()) nombre = cursor.getString(0)
        cursor.close()
        return nombre
    }

    // ==================== EVENTOS ====================
    fun insertarEvento(tareaId: Int, inicio: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("tarea_id", tareaId)
            put("inicio", inicio)
        }
        return db.insert("eventos", null, values)
    }

    fun finalizarEvento(id: Int, fin: String, duracion: Long): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("fin", fin)
            put("duracion", duracion)
        }
        return db.update("eventos", values, "id = ?", arrayOf(id.toString())) > 0
    }

    fun eventoEnCurso(): Evento? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT id, tarea_id, inicio, fin, duracion FROM eventos WHERE fin IS NULL LIMIT 1", null
        )
        var evento: Evento? = null
        if (cursor.moveToFirst()) {
            evento = Evento(
                cursor.getInt(0),
                cursor.getInt(1),
                cursor.getString(2) ?: "",
                cursor.getString(3),
                if (cursor.isNull(4)) null else cursor.getLong(4)
            )
        }
        cursor.close()
        return evento
    }

    fun listarEventos(): List<Evento> {
        val lista = mutableListOf<Evento>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT id, tarea_id, inicio, fin, duracion FROM eventos ORDER BY id DESC", null
        )
        while (cursor.moveToNext()) {
            lista.add(
                Evento(
                    cursor.getInt(0),
                    cursor.getInt(1),
                    cursor.getString(2) ?: "",
                    cursor.getString(3),
                    if (cursor.isNull(4)) null else cursor.getLong(4)
                )
            )
        }
        cursor.close()
        return lista
    }

    fun nombreTarea(id: Int): String {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT nombre FROM tareas WHERE id = ?", arrayOf(id.toString()))
        var nombre = ""
        if (cursor.moveToFirst()) nombre = cursor.getString(0)
        cursor.close()
        return nombre
    }

    fun categoriaDeTarea(tareaId: Int): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT categoria_id FROM tareas WHERE id = ?", arrayOf(tareaId.toString()))
        var catId = -1
        if (cursor.moveToFirst()) catId = cursor.getInt(0)
        cursor.close()
        return catId
    }
}