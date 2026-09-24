
package com.example.presence

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.presence.databinding.ItemEtudiantBinding
import com.example.presence.databinding.ItemEtudiantPresenceBinding
import com.example.presence.databinding.ItemRapportBinding
import com.example.presence.databinding.ItemSeanceBinding

// ----- Liste des étudiants (gestion + QR) -----
class EtudiantAdapter(
    private val liste: List<Etudiant>,
    private val onQr: (Etudiant) -> Unit,
    private val onSupprimer: (Etudiant) -> Unit
) : RecyclerView.Adapter<EtudiantAdapter.VH>() {

    class VH(val b: ItemEtudiantBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(ItemEtudiantBinding.inflate(LayoutInflater.from(p.context), p, false))

    override fun getItemCount() = liste.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val e = liste[pos]
        h.b.txtEtudiant.text = "${e.nom} ${e.prenom} — ${e.matricule}"
        h.b.btnQr.setOnClickListener { onQr(e) }
        h.b.btnSupprimer.setOnClickListener { onSupprimer(e) }
    }
}

// ----- Feuille de présence (interrupteurs manuels) -----
class EtudiantPresenceAdapter(
    private val liste: List<Etudiant>,
    private val etats: Map<Long, Boolean>,
    private val onChange: (Long, Boolean) -> Unit
) : RecyclerView.Adapter<EtudiantPresenceAdapter.VH>() {

    class VH(val b: ItemEtudiantPresenceBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(ItemEtudiantPresenceBinding.inflate(LayoutInflater.from(p.context), p, false))

    override fun getItemCount() = liste.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val e = liste[pos]
        h.b.txtEtudiant.text = "${e.nom} ${e.prenom}"
        h.b.switchPresent.isChecked = etats[e.id] ?: true
        h.b.switchPresent.setOnCheckedChangeListener { _, c -> onChange(e.id, c) }
    }
}

// ----- Liste des séances -----
class SeanceAdapter(
    private val liste: List<Seance>,
    private val onClick: (Seance) -> Unit
) : RecyclerView.Adapter<SeanceAdapter.VH>() {

    class VH(val b: ItemSeanceBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(ItemSeanceBinding.inflate(LayoutInflater.from(p.context), p, false))

    override fun getItemCount() = liste.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val s = liste[pos]
        h.b.txtSeance.text = s.cours
        h.b.root.setOnClickListener { onClick(s) }
    }
}

// ----- Rapport d'absence -----
class RapportAdapter(
    private val lignes: List<RapportActivity.LigneRapport>
) : RecyclerView.Adapter<RapportAdapter.VH>() {

    class VH(val b: ItemRapportBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(ItemRapportBinding.inflate(LayoutInflater.from(p.context), p, false))

    override fun getItemCount() = lignes.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val l = lignes[pos]
        h.b.txtRapport.text = "${l.etudiant.nom} ${l.etudiant.prenom} — ${l.etudiant.matricule}"
        if (l.present) {
            h.b.txtStatut.text = "✅ Présent" + if (l.heure.isNotEmpty()) " (${l.heure})" else ""
            h.b.txtStatut.setTextColor(Color.parseColor("#2E7D32"))
        } else {
            h.b.txtStatut.text = "❌ ABSENT"
            h.b.txtStatut.setTextColor(Color.parseColor("#C62828"))
        }
    }
}
