package com.example.proyecto_tfg.models

import com.example.proyecto_tfg.enums.StatusEnum

data class LibrarySB(
    val user_id : String,
    val game_id : Number,
    var status : StatusEnum,
    val review : String?,
    val score : Number?,
    val recommended : Boolean?
    )
