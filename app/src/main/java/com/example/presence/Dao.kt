
package com.example.presence

import androidx.room.*

@Dao
interface PresenceDao {

    @Query("SELECT * FROM etudiants ORDER BY nom")
    suspend fun getEtudiants(): List<Etudiant>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun ajouterEtudiant(e: Etudiant): Long

    @Delete
    suspend fun supprimerEtudiant(e: Etudiant)

    @Query("SELECT * FROM etudiants WHERE id = :id")
    suspend fun getEtudiant(id: Long): Etudiant?

    @Query("SELECT * FROM etudiants WHERE matricule = :matricule LIMIT 1")
    suspend fun trouverEtudiant(matricule: String): Etudiant?

    @Query("SELECT * FROM seances ORDER BY id DESC")
    suspend fun getSeances(): List<Seance>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun ajouterSeance(s: Seance): Long

    @Query("SELECT * FROM seances WHERE id = :id")
    suspend fun getSeance(id: Long): Seance?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enregistrerPresences(list: List<Presence>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun pointer(p: Presence): Long

    @Query("SELECT * FROM presences WHERE seanceId = :seanceId")
    suspend fun getPresences(seanceId: Long): List<Presence>
}
