package com.example.proyecto_tfg.models

import java.util.*

data class ProfileSB (
    val user_id : String,
    val username : String,
    val avatar_url : String,
    val description : String?,
    val country : Number?,
    val relatedAccounts : List<String>?,
    val friends : List<UUID>?
        )



