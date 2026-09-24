
package com.example.presence

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "etudiants")
data class Etudiant(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nom: String,
    val prenom: String,
    val matricule: String,
    val email: String = "" // utilisé pour l'envoi du QR par e-mail
)

@Entity(tableName = "seances")
data class Seance(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cours: String,
    val date: String // format JJ/MM/AAAA
)

@Entity(tableName = "presences", primaryKeys = ["seanceId", "etudiantId"])
data class Presence(
    val seanceId: Long,
    val etudiantId: Long,
    val present: Boolean,
    val heure: String = "" // heure du scan par le professeur
)

object QrCodes {
    // Chaque étudiant a SON propre QR, permanent, généré par l'app
    const val PREFIX = "presence://etudiant/"
    fun encode(etudiantId: Long) = "$PREFIX$etudiantId"
    fun decode(contenu: String): Long? =
        if (contenu.startsWith(PREFIX)) contenu.removePrefix(PREFIX).toLongOrNull() else null
}
