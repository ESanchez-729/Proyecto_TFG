package com.example.proyecto_tfg.models

data class ProfileSB (
    val user_id : String,
    var username : String,
    var avatar_url : String,
    var description : String?,
    var country : Number?,
    val related_accounts : List<String>?
        )



