
package com.example.presence

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.presence.databinding.ActivityListeEtudiantsBinding
import com.google.android.material.textfield.TextInputEditText
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ListeEtudiantsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListeEtudiantsBinding
    private lateinit var db: AppDatabase

    // File d'attente pour l'envoi groupé des QR
    private var fileAttente: List<Etudiant> = emptyList()
    private var indexEnvoi = 0

    // Quand l\'utilisateur revient de la messagerie, on envoie le suivant
    private val lanceurEnvoi = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { envoyerSuivant() }

    private val choisirFichier = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) importerCsv(uri) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListeEtudiantsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = AppDatabase.get(this)
        binding.recyclerEtudiants.layoutManager = LinearLayoutManager(this)

        binding.btnAjouter.setOnClickListener { dialogAjout() }
        binding.btnImporter.setOnClickListener {
            choisirFichier.launch(arrayOf("text/*", "application/vnd.ms-excel"))
        }
        binding.btnToutEnvoyer.setOnClickListener { toutEnvoyer() }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val liste = db.dao().getEtudiants()
            binding.recyclerEtudiants.adapter = EtudiantAdapter(liste,
                onQr = { e ->
                    startActivity(Intent(this@ListeEtudiantsActivity, QrEtudiantActivity::class.java)
                        .putExtra("etudiantId", e.id))
                },
                onSupprimer = { e ->
                    lifecycleScope.launch { db.dao().supprimerEtudiant(e); onResume() }
                })
        }
    }

    // ----- Envoi groupé des QR -----
    private fun toutEnvoyer() {
        lifecycleScope.launch {
            fileAttente = db.dao().getEtudiants().filter { it.email.isNotEmpty() }
            when {
                fileAttente.isEmpty() ->
                    toast("Aucun étudiant avec e-mail (importe la liste avec la colonne E-mail)")
                else -> {
                    indexEnvoi = 0
                    toast("Envoi de ${fileAttente.size} QR, un par un...")
                    envoyerSuivant()
                }
            }
        }
    }

    private fun envoyerSuivant() {
        if (indexEnvoi >= fileAttente.size) {
            toast("Terminé : ${fileAttente.size} QR envoyé(s) ✅")
            return
        }
        val e = fileAttente[indexEnvoi]
        lifecycleScope.launch {
            val bmp = genererQr(QrCodes.encode(e.id), 800)
            val fichier = withContext(Dispatchers.IO) {
                val dir = File(cacheDir, "qr").apply { mkdirs() }
                val f = File(dir, "qr_${e.matricule}.jpg")
                FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.JPEG, 100, it) }
                f
            }
            val uri: Uri = FileProvider.getUriForFile(
                this@ListeEtudiantsActivity,
                "com.example.presence.fileprovider", fichier)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(e.email))
                putExtra(Intent.EXTRA_SUBJECT, "Ton QR de présence — ${e.prenom} ${e.nom}")
                putExtra(Intent.EXTRA_TEXT,
                    "Bonjour ${e.prenom},\n\nVoici ton QR code de présence " +
                    "(matricule ${e.matricule}).\n" +
                    "Garde-le précieusement : il sera scanné à chaque séance.\n\nCordialement.")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val numero = indexEnvoi + 1
            indexEnvoi++
            lanceurEnvoi.launch(
                Intent.createChooser(intent, "Envoyer à ${e.prenom} ${e.nom} ($numero/${fileAttente.size})"))
        }
    }

    // ----- Import CSV -----
    // Format : Nom;Prénom;Matricule;E-mail   (séparateur ; ou , — l'e-mail est optionnel)
    private fun importerCsv(uri: android.net.Uri) {
        lifecycleScope.launch {
            val texte = withContext(Dispatchers.IO) {
                contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
            }
            var ajoutes = 0
            texte.lines().filter { it.isNotBlank() }.forEachIndexed { i, ligne ->
                if (i == 0 && ligne.lowercase().contains("nom")) return@forEachIndexed
                val parts = if (ligne.contains(";")) ligne.split(";") else ligne.split(",")
                if (parts.size >= 3) {
                    db.dao().ajouterEtudiant(
                        Etudiant(
                            nom = parts[0].trim(),
                            prenom = parts[1].trim(),
                            matricule = parts[2].trim(),
                            email = parts.getOrNull(3)?.trim() ?: ""
                        )
                    )
                    ajoutes++
                }
            }
            toast("$ajoutes étudiant(s) importé(s)")
            onResume()
        }
    }

    private fun dialogAjout() {
        val layout = layoutInflater.inflate(R.layout.dialog_etudiant, null)
        val nom = layout.findViewById<TextInputEditText>(R.id.editNom)
        val prenom = layout.findViewById<TextInputEditText>(R.id.editPrenom)
        val matricule = layout.findViewById<TextInputEditText>(R.id.editMatricule)
        val email = layout.findViewById<TextInputEditText>(R.id.editEmail)

        AlertDialog.Builder(this)
            .setTitle("Ajouter un étudiant")
            .setView(layout)
            .setPositiveButton("Ajouter") { _, _ ->
                lifecycleScope.launch {
                    db.dao().ajouterEtudiant(
                        Etudiant(
                            nom = nom.text.toString(),
                            prenom = prenom.text.toString(),
                            matricule = matricule.text.toString(),
                            email = email.text.toString().trim()
                        )
                    )
                    onResume()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun genererQr(texte: String, taille: Int): Bitmap {
        val bits = QRCodeWriter().encode(texte, BarcodeFormat.QR_CODE, taille, taille)
        val bmp = Bitmap.createBitmap(taille, taille, Bitmap.Config.RGB_565)
        for (x in 0 until taille)
            for (y in 0 until taille)
                bmp.setPixel(x, y, if (bits.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
        return bmp
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
