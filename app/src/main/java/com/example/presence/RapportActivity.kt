
package com.example.presence

import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.presence.databinding.ActivityRapportBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RapportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRapportBinding
    private lateinit var db: AppDatabase
    private var seanceId: Long = 0
    private var lignes = listOf<LigneRapport>()

    data class LigneRapport(val etudiant: Etudiant, val present: Boolean, val heure: String)

    private val exporter = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    val sb = StringBuilder("Nom;Prénom;Matricule;E-mail;Statut;Heure de pointage\n")
                    lignes.forEach { l ->
                        sb.append("${l.etudiant.nom};${l.etudiant.prenom};${l.etudiant.matricule};${l.etudiant.email};")
                        sb.append(if (l.present) "Présent;${l.heure}" else "ABSENT;")
                        sb.append("\n")
                    }
                    contentResolver.openOutputStream(uri)?.use {
                        it.write(sb.toString().toByteArray(Charsets.UTF_8))
                    }
                }
                Toast.makeText(this@RapportActivity, "Rapport exporté ✅", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRapportBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = AppDatabase.get(this)
        seanceId = intent.getLongExtra("seanceId", 0)

        binding.recyclerRapport.layoutManager = LinearLayoutManager(this)

        binding.btnExporter.setOnClickListener {
            exporter.launch("rapport_absences_$seanceId.csv")
        }

        lifecycleScope.launch {
            val seance = db.dao().getSeance(seanceId)
            val etudiants = db.dao().getEtudiants()
            val presences = db.dao().getPresences(seanceId)
                .associateBy({ it.etudiantId }, { it })

            lignes = etudiants.map { e ->
                val p = presences[e.id]
                LigneRapport(e, p?.present ?: false, p?.heure ?: "")
            }
            val absents = lignes.count { !it.present }

            binding.txtTitreRapport.text =
                "Rapport — ${seance?.cours ?: ""}\n${lignes.size - absents} présent(s), $absents ABSENT(S)"

            binding.recyclerRapport.adapter = RapportAdapter(lignes)
        }
    }
}
