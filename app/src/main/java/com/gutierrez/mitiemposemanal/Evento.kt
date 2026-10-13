package com.gutierrez.mitiemposemanal

data class Evento(
    val id: Int,
    val tareaId: Int,
    val inicio: String,
    val fin: String?,
    val duracion: Long?
)