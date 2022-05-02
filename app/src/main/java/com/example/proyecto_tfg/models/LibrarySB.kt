package com.example.proyecto_tfg.models

import com.example.proyecto_tfg.enums.StatusEnum
import java.util.*

data class LibrarySB(
    val user_id : UUID,
    val game_id : Number,
    val status : StatusEnum,
    val review : String,
    val score : Number,
    val recommended : Boolean
    )
