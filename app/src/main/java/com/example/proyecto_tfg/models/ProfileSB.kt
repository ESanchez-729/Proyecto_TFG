package com.example.proyecto_tfg.models

data class ProfileSB (
    val user_id : String,
    val username : String,
    val avatar_url : String,
    val description : String?,
    val country : Number?,
    val related_accounts : List<String>?
        )



