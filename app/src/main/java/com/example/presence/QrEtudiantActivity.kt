
package com.example.presence

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.presence.databinding.ActivityQrBinding
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class QrEtudiantActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQrBinding
    private lateinit var db: AppDatabase
    private var etudiant: Etudiant? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = AppDatabase.get(this)

        val etudiantId = intent.getLongExtra("etudiantId", 0)

        lifecycleScope.launch {
            val e = db.dao().getEtudiant(etudiantId)
            etudiant = e
            if (e == null) { finish(); return@launch }

            binding.txtQrInfo.text =
                "${e.prenom} ${e.nom}\nMatricule : ${e.matricule}" +
                (if (e.email.isNotEmpty()) "\nE-mail : ${e.email}" else "") +
                "\n\nCe QR est permanent : l'étudiant le garde et tu le scannes à chaque séance."
            binding.imgQr.setImageBitmap(genererQr(QrCodes.encode(e.id), 800))
        }

        binding.btnEnvoyer.setOnClickListener { envoyerParMail() }
    }

    private fun envoyerParMail() {
        val e = etudiant
        if (e == null) return
        if (e.email.isEmpty()) {
            Toast.makeText(this,
                "Pas d'e-mail pour cet étudiant (importe la liste avec la colonne E-mail)",
                Toast.LENGTH_LONG).show()
            return
        }

        lifecycleScope.launch {
            // Sauvegarde l'image du QR dans le cache pour la joindre
            val bmp = genererQr(QrCodes.encode(e.id), 800)
            val fichier = withContext(Dispatchers.IO) {
                val dir = File(cacheDir, "qr").apply { mkdirs() }
                val f = File(dir, "qr_${e.matricule}.jpg")
                FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.JPEG, 100, it) }
                f
            }
            val uri: Uri = FileProvider.getUriForFile(
                this@QrEtudiantActivity,
                "com.example.presence.fileprovider", fichier)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(e.email))
                putExtra(Intent.EXTRA_SUBJECT, "Ton QR de présence — ${e.prenom} ${e.nom}")
                putExtra(Intent.EXTRA_TEXT,
                    "Bonjour ${e.prenom},\n\nVoici ton QR code de présence " +
                    "(matricule ${e.matricule}).\n" +
                    "Garde-le précieusement : il sera scanné à chaque séance.\n\n" +
                    "Cordialement.")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Envoyer avec :"))
        }
    }

    private fun genererQr(texte: String, taille: Int): Bitmap {
        val bits = QRCodeWriter().encode(texte, BarcodeFormat.QR_CODE, taille, taille)
        val bmp = Bitmap.createBitmap(taille, taille, Bitmap.Config.RGB_565)
        for (x in 0 until taille)
            for (y in 0 until taille)
                bmp.setPixel(x, y, if (bits.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
        return bmp
    }
}
