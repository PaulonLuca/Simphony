package it.unife.isa.symphony

import android.app.SearchManager
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import it.unife.isa.symphony.content.SongModel

/**
 * Activity che, se avviata, popola le liste di ricerca da
 * visualizzare nella recycler view.
 */

class SearchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
    }

    override fun onMenuOpened(featureId: Int, menu: Menu): Boolean {
        return super.onMenuOpened(featureId, menu)
        Log.d("Test","Test1")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {

        if (Intent.ACTION_SEARCH == intent.action) {
            val query = intent.getStringExtra(SearchManager.QUERY)
            SongModel.searched=true
            SongModel.searchBy(SongModel.searchFor,query!!)
            navigateUpTo(Intent(this, SongsListActivity::class.java))
        }
    }
}