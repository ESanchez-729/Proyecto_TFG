package com.example.proyecto_tfg.models

data class GameItem (

    val id: Int,
    val image: String,
    val title: String,
    var status: String,
    val platform: String,
    val score: Int
    )