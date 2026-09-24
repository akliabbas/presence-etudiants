
package com.example.presence

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.presence.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: AppDatabase
    private val seances = mutableListOf<Seance>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = AppDatabase.get(this)

        binding.recyclerSeances.layoutManager = LinearLayoutManager(this)

        binding.btnNouvelleSeance.setOnClickListener {
            val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            lifecycleScope.launch {
                val id = db.dao().ajouterSeance(Seance(cours = "Séance du $date", date = date))
                startActivity(Intent(this@MainActivity, SeanceActivity::class.java)
                    .putExtra("seanceId", id))
            }
        }

        binding.btnGererEtudiants.setOnClickListener {
            startActivity(Intent(this, ListeEtudiantsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            seances.clear()
            seances.addAll(db.dao().getSeances())
            binding.recyclerSeances.adapter = SeanceAdapter(seances) { s ->
                startActivity(Intent(this@MainActivity, SeanceActivity::class.java)
                    .putExtra("seanceId", s.id))
            }
        }
    }
}
