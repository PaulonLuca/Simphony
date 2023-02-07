package it.unife.isa.symphony

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.MenuItem

/**
 * Activity che rappresenta l'interfaccia di riproduzione. Viene visualizzata solo sugli smartphone.
 */
class SongDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song_detail)
        setSupportActionBar(findViewById(R.id.detail_toolbar))

        // Visualizza il bottone per tornare indietro nella AppBar.
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null)
        {
            //Se non c'era uno stato precedente si crea un nuovo frammento
            //e si visualizza attraverso la transazione
            val fragment = PlayFragment().apply {
                arguments = Bundle().apply {
                    putString(PlayFragment.SONG_ID,
                        intent.getStringExtra(PlayFragment.SONG_ID))
                }
            }

            supportFragmentManager.beginTransaction()
                .add(R.id.song_detail_container, fragment)
                .commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        when (item.itemId)
        {
            //Cliccando sul pulsante <-- nella action bar si ritorna all'activity precedente
            //che visualizza la lista di canzoni.
            android.R.id.home -> {
                navigateUpTo(Intent(this, SongsListActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
}