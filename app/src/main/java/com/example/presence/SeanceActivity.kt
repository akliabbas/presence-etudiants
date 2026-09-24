
package com.example.presence

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.presence.databinding.ActivitySeanceBinding
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SeanceActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySeanceBinding
    private lateinit var db: AppDatabase
    private var seanceId: Long = 0
    private val etats = mutableMapOf<Long, Boolean>()   // présent / absent (manuel)
    private val heures = mutableMapOf<Long, String>()   // heure de scan QR

    // Scan continu : après chaque QR reconnu, on relance automatiquement
    private val scanner = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            traiterScan(result.contents)
            // Relance automatique pour scanner l'étudiant suivant
            Handler(Looper.getMainLooper()).postDelayed({ lancerScan() }, 600)
        }
        // Si l'utilisateur annule (back), on sort du mode scan
    }

    private val demandePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { accordée -> if (accordée) lancerScan() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySeanceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = AppDatabase.get(this)
        seanceId = intent.getLongExtra("seanceId", 0)

        binding.recyclerEtudiants.layoutManager = LinearLayoutManager(this)

        binding.btnScanner.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) lancerScan()
            else demandePermission.launch(Manifest.permission.CAMERA)
        }

        binding.btnRapport.setOnClickListener {
            startActivity(Intent(this, RapportActivity::class.java)
                .putExtra("seanceId", seanceId))
        }

        binding.btnValider.setOnClickListener {
            lifecycleScope.launch {
                val maintenant = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                val liste = etats.map { (eid, present) ->
                    Presence(seanceId, eid, present, heures[eid] ?: if (present) maintenant else "")
                }
                db.dao().enregistrerPresences(liste)
                val presents = etats.values.count { it }
                Toast.makeText(this@SeanceActivity,
                    "Enregistré : $presents/${etats.size} présents", Toast.LENGTH_LONG).show()
                finish()
            }
        }

        lifecycleScope.launch {
            val etudiants = db.dao().getEtudiants()
            val existantes = db.dao().getPresences(seanceId)
                .associateBy({ it.etudiantId }, { it })
            etudiants.forEach { e ->
                etats[e.id] = existantes[e.id]?.present ?: false
                existantes[e.id]?.heure?.takeIf { it.isNotEmpty() }?.let { heures[e.id] = it }
            }

            binding.recyclerEtudiants.adapter = EtudiantPresenceAdapter(
                etudiants, etats,
                onChange = { eid, present -> etats[eid] = present }
            )
            majResume()
        }
    }

    private fun lancerScan() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt("Scannez le QR de chaque étudiant présent")
        options.setBeepEnabled(true)
        options.setOrientationLocked(true)
        scanner.launch(options)
    }

    private fun traiterScan(contenu: String) {
        val etudiantId = QrCodes.decode(contenu)
        if (etudiantId == null) {
            Toast.makeText(this, "QR invalide", Toast.LENGTH_SHORT).show()
            return
        }
        if (etats[etudiantId] == true) {
            Toast.makeText(this, "Déjà pointé", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            val e = db.dao().getEtudiant(etudiantId)
            if (e == null) {
                Toast.makeText(this@SeanceActivity, "Étudiant inconnu", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val heure = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            etats[etudiantId] = true
            heures[etudiantId] = heure
            binding.recyclerEtudiants.adapter?.notifyDataSetChanged()
            majResume()
            Toast.makeText(this@SeanceActivity,
                "✅ ${e.prenom} ${e.nom} pointé à $heure", Toast.LENGTH_SHORT).show()
        }
    }

    private fun majResume() {
        val presents = etats.values.count { it }
        binding.txtResume.text = "$presents présent(s) / ${etats.size}"
    }
}
