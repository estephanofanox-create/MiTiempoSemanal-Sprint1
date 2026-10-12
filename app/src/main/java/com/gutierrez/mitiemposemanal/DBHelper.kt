package com.gutierrez.mitiemposemanal

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper(context: Context) : SQLiteOpenHelper(context, "mitiempo.db", null, 1) {

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

        val categoriasIniciales = listOf("Estudio", "Trabajo", "Descanso", "Ocio")
        for (nombre in categoriasIniciales) {
            val values = ContentValues().apply { put("nombre", nombre) }
            db.insert("categorias", null, values)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS tareas")
        db.execSQL("DROP TABLE IF EXISTS categorias")
        onCreate(db)
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
        val resultado = db.insert("categorias", null, values)
        return resultado != -1L
    }

    fun actualizarCategoria(id: Int, nombre: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply { put("nombre", nombre) }
        val filas = db.update("categorias", values, "id = ?", arrayOf(id.toString()))
        return filas > 0
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
        val filas = db.delete("categorias", "id = ?", arrayOf(id.toString()))
        return filas > 0
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
        val resultado = db.insert("tareas", null, values)
        return resultado != -1L
    }

    fun actualizarTarea(id: Int, nombre: String, categoriaId: Int): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("nombre", nombre)
            put("categoria_id", categoriaId)
        }
        val filas = db.update("tareas", values, "id = ?", arrayOf(id.toString()))
        return filas > 0
    }

    fun eliminarTarea(id: Int): Boolean {
        val db = writableDatabase
        val filas = db.delete("tareas", "id = ?", arrayOf(id.toString()))
        return filas > 0
    }

    fun nombreCategoria(id: Int): String {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT nombre FROM categorias WHERE id = ?", arrayOf(id.toString()))
        var nombre = ""
        if (cursor.moveToFirst()) nombre = cursor.getString(0)
        cursor.close()
        return nombre
    }
}