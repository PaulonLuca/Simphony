package it.unife.isa.symphony

import android.Manifest
import android.app.SearchManager
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import it.unife.isa.symphony.content.SongModel
import it.unife.isa.symphony.content.SongModel.ARTIST
import it.unife.isa.symphony.content.SongModel.GENRE
import it.unife.isa.symphony.content.SongModel.Song
import it.unife.isa.symphony.content.SongModel.TITLE
import it.unife.isa.symphony.content.SongModel.resetStateSearch
import it.unife.isa.symphony.content.SongModel.searchFor
import it.unife.isa.symphony.content.SongModel.searched
import java.util.*


/**
 * Activity che rappresenta una lista di canzoni.
 * Su smartphone rappresenta una lista di canzoni, quando una viene toccata si
 * chiama l'activity [SongDetailActivity] che visualizza i dettagli della canzone
 * Sui tablet l'activity visualizza la lista e l'interfaccia di riproduzione contemporaneamente
 */
class SongsListActivity : AppCompatActivity() {

    //Se l'activity è in modalità tablet o smartphone
    //tablet: twoPane=true
    //smartphone: twoPane=false
    //private var twoPane: Boolean = false
    private val pickAudioCode=200
    private var binder: PlayerService.PlayBinder?=null
    private var mBound=false

    //Connessione usata per comuincare con il servizio quando si cancella una canzone
    private val mConnection: ServiceConnection =object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            mBound=false
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            binder=service as PlayerService.PlayBinder
            mBound=true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Richiesta permesso di accesso alla memoria esterna all'app
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_READ_EXTERNAL_STORAGE
            )
        }

        setContentView(R.layout.activity_songs_list)

        //caricamento canzoni nella lista dal DB
        if (SongModel.SONG_ITEMS.size == 0)
            SongModel.loadSongs(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.title = title
        toolbar.setLogo(R.mipmap.ic_logo_foreground)

        //Click listener per aggiungere una canzone alla lista.
        //Viene richiamata l'activity di sistema ACTION_PICK per recuperare la canzone
        val pickSong = object : View.OnClickListener {
            override fun onClick(v: View?) {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
                try {
                    startActivityForResult(intent, pickAudioCode)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(applicationContext, R.string.no_picked_audio, Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        //Si aggiunge al floating button l'azione per aggiungere una canzone alla lista
        //e si collega al listener pickSong.
        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->
            Snackbar.make(view, "Add song", Snackbar.LENGTH_LONG)
                .setAction("ADD", pickSong).show()
        }


        if (findViewById<FrameLayout>(R.id.fragment_container) != null) {
            //Se è presente un fragment nel Framelayout allora l'app è in modalità tablet
            twoPane = true
        }

        //Avvio servizio in modalità start per mantenerlo attivo durante
        //tutto il ciclo di vita dell'applicazione. Il servizio verrà stoppato alla
        //chiusura dell'app poichè nel manifest si è specificato: stopWithTask=true
        val i = Intent(applicationContext, PlayerService::class.java)
        //Creazione connessione per comuniare con service in caso di cancellazione canzone in riproduzione
        bindService(i, mConnection, Context.BIND_AUTO_CREATE)
        startService(i)

        //Si aggancia l'adapter alla recycler view
        setupRecyclerView(findViewById(R.id.item_list))

    }

    //Quando viene distrutta l'activity ci si scollega dal servizio in modalità bound.
    override fun onDestroy() {
        unbindService(mConnection)
        super.onDestroy()
    }

    //Quando l'utente ha approvato l'accesso alla memoria
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if(requestCode== REQUEST_READ_EXTERNAL_STORAGE)
        {
            if(grantResults.size!=1 || grantResults[0]!= PackageManager.PERMISSION_GRANTED)
            {
                //Permesso non garantito, chiusura activity
                val builer= AlertDialog.Builder(this)
                builer.setTitle(R.string.no_permission)
                builer.setMessage(R.string.no_permission_message)
                builer.setPositiveButton(R.string.confirm_dialog,
                    DialogInterface.OnClickListener{ dialogInterface: DialogInterface, i: Int -> finish()})
                builer.show()
            }
            else
            {
                //Permesso concesso, si mostra come utilizzare l'applicazione al primo avvio
                val builer= AlertDialog.Builder(this)
                builer.setTitle(R.string.info)
                builer.setMessage(R.string.info_message)
                builer.setPositiveButton(R.string.confirm_dialog,
                    DialogInterface.OnClickListener{ dialogInterface: DialogInterface, i: Int ->})
                builer.show()
            }
        }
        else
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    //Quando ritorna l'activity ACTION_PICK
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        var s = Song(UUID.randomUUID(), "title", "artist", Uri.parse("Uri"), "duration", "genre")

        if (resultCode == RESULT_OK)
        {
            //Dopo aver ottenuto l'uri della canzone attarvero media picker si ricavano tutte le informazioni della canzone
            //interrogando, attraverso una query, il content provider di sistema tramite content resolver
            val songUri: Uri? = data?.data
            //Colonne su cui proiettare
            val projection= arrayOf(MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DURATION,MediaStore.Audio.Media._ID)
            val cursor=contentResolver.query(songUri!!,projection,null,null,null)

            if(cursor!=null && cursor.moveToFirst())
            {
                val musicId: Int = cursor.getString(3).toInt()
                val uri = MediaStore.Audio.Genres.getContentUriForAudioId("external", musicId)
                val projectionGenres= arrayOf(MediaStore.Audio.Genres.NAME)
                val genresCursor = contentResolver.query(uri,projectionGenres, null, null, null)

                var genre = ""
                if (genresCursor!!.moveToFirst()) {

                   // do {
                        genre = genresCursor.getString(0).toString()
                   // } while (genresCursor.moveToNext())
                }

                val title=cursor.getString(0)
                val artist=cursor.getString(1)
                val duration=cursor.getString(2)

                s=Song(UUID.randomUUID(),title,artist,songUri,duration,genre)
                cursor.close()
            }

            //Se la canzone è già presente nella lista non viene aggiunta ma
            //viene mostrato un messaggio che notifica che è già stata inserita
            if(!SongModel.isAlreadyIn(s))
            {
                //Salvataggio della canzone nel db, nella lista e nella mappa
                SongModel.saveSong(this,s)
                val recyclerView= findViewById<RecyclerView>(R.id.item_list)
                recyclerView.adapter?.notifyDataSetChanged()
            }
            else
                Toast.makeText(applicationContext, R.string.already_in, Toast.LENGTH_SHORT).show()
        }
    }


    private fun setupRecyclerView(recyclerView: RecyclerView)
    {
        if (searched)
            recyclerView.adapter = SongRecyclerViewAdapter(this, SongModel.SEARCH_SONG_ITEMS, twoPane)
        else
            recyclerView.adapter = SongRecyclerViewAdapter(this, SongModel.SONG_ITEMS, twoPane)

        //Listener per rimuovere un elemento dalla recycler view con slide verso sinistra
        val deleteListener=object: ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT)
        {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                TODO("Not yet implemented")
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                //Si recupera la canzone associata al cassetto
                val song=viewHolder.itemView.tag as Song

                //Rimozione della canzone selezionata dal db dalla lista e dalla mappa
                SongModel.deleteSong(applicationContext,song.id,viewHolder.absoluteAdapterPosition)

                //Si notifica all'adapter della recycler view che è stato rimosso un elemento nella
                //posizione specificata
                recyclerView.adapter?.notifyItemRemoved(viewHolder.absoluteAdapterPosition)

                //Quando si elimina una canzone si possono verificare 3 situazioni:
                //1) Eliminazione di una qualsiasi canzone NON in riproduzione-->Si elimina senza problemi
                //2) Eliminazione di una canzone in riproduzione su samrtphone-->Si interrompe la riproduzione
                //3) Eliminazione da una canzone in riproduzione su tablet-->si interrope la riproduzione e si elimina il fragment
                //sostituendolo con uno vuoto
                if(mBound && song.id==binder?.currentSong()?.id)
                {
                    if(twoPane)
                    {
                        binder?.stop()
                        //Recupero fragment corrente
                        val curFragment=supportFragmentManager.findFragmentById(R.id.fragment_container)
                        if(curFragment!=null)
                            supportFragmentManager.beginTransaction().remove(curFragment).commit()
                        //Eliminazione del vecchio fragment e sostituzione con uno nuovo
                        supportFragmentManager.beginTransaction().replace(R.id.fragment_container,PlayFragment())
                    }
                    else
                    {
                        //Interruzione riproduzione
                        binder?.stop()
                    }
                    Toast.makeText(applicationContext, R.string.play_song_deleted, Toast.LENGTH_SHORT).show()
                }
            }
        }

        val swipeListener= ItemTouchHelper(deleteListener)
        //Si aggancia il listener alla recycler view
        swipeListener.attachToRecyclerView(recyclerView)
    }

    class SongRecyclerViewAdapter(private val parentActivity: SongsListActivity,
                                  private val values: List<Song>,
                                  private val twoPane: Boolean) :
        RecyclerView.Adapter<SongRecyclerViewAdapter.ViewHolder>()
    {
        /*
            * Listener applicato ad ogni cassetto della lista, ogni qualvolta si clicca su un holder
            * viene viene verificato su quale dispositivo ci si trova ovvero smartphone o table.
            * Su smartphone si chiama l'activity: Song_detail_activity
            * Su tablet si crea un nuovo fragment e si avvia la transazione per aggiungerlo
        */
        private val onClickListener: View.OnClickListener

        init {
            onClickListener = View.OnClickListener { v ->
                val item = v.tag as Song //Si recupera dal tag del cassetto la canzone
                if (twoPane)
                {
                    //Creazione nuovo fragment, si passa l'id della canzone come parametro
                    val fragment = PlayFragment().apply {
                        arguments = Bundle().apply {
                            putString(PlayFragment.SONG_ID, item.id.toString())
                        }
                    }
                    parentActivity.supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .commit()
                }
                else
                {
                    //Avvio activity SongDetailActivity se su smatphone
                    val intent = Intent(v.context, SongDetailActivity::class.java).apply {
                        putExtra(PlayFragment.SONG_ID, item.id.toString())
                    }
                    v.context.startActivity(intent)
                }
            }
        }

        //Creazione grafica dal cassetto e l'oggetto cassetto
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.song_list_holder, parent, false)
            return ViewHolder(view)
        }

        //Assegnazione delle informazioni da visualizzare sul cassetto: titolo, artista
        //Assegnazione della canzone al tag del cassetto
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = values[position]
            holder.tvTitolo.text = item.titolo
            holder.tvArtista.text=item.artista

            with(holder.itemView) {
                tag = item
                setOnClickListener(onClickListener)
            }
        }

        //Ritorna numero elementi della lista di canzoni
        override fun getItemCount() = values.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
        {
            val tvTitolo: TextView = view.findViewById(R.id.tv_titolo)
            val tvArtista: TextView =view.findViewById(R.id.tv_artista)
        }
    }

    //Gonfiaggio dell'interfaccia grafica del menù
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main,menu)
        // Associate searchable configuration with the SearchView
        val cn = ComponentName(this, SearchActivity::class.java)
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        (menu!!.findItem(R.id.search).actionView as SearchView).apply {
            setSearchableInfo(searchManager.getSearchableInfo(cn))
        }

        if(searched)
        {
            menu.removeItem(R.id.search)
            menu.findItem(R.id.recreate).setVisible(true)
        }
        return super.onCreateOptionsMenu(menu)
    }

    //Aggancio delle azioni ai vari elementi del menù
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId)
        {
            R.id.menu_guide->{
                //Informazioni su come usare l'app
                val builer=AlertDialog.Builder(this)
                builer.setTitle(R.string.info)
                builer.setMessage(R.string.info_message)
                builer.setPositiveButton(R.string.confirm_dialog,DialogInterface.OnClickListener{ dialogInterface: DialogInterface, i: Int ->})
                builer.show()
            }
            R.id.menu_add->{
                //Aggiunta canzone (secondo modo)
                try {
                    startActivityForResult(Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI), pickAudioCode)
                } catch(e: ActivityNotFoundException) {
                    Toast.makeText(applicationContext, R.string.no_picked_audio, Toast.LENGTH_SHORT).show()
                }
            }

            //Selezione modalità di ricerca
            R.id.artist->{
                if(item.isChecked)
                    item.setChecked(false)
                else
                {
                    item.setChecked(true)
                    searchFor= ARTIST;
                }
            }
            R.id.genre->{
                if(item.isChecked)
                    item.setChecked(false)
                else
                {
                    item.setChecked(true)
                    searchFor= GENRE;
                }
            }
            R.id.title->{
                if(item.isChecked)
                    item.setChecked(false)
                else
                {
                    item.setChecked(true)
                    searchFor= TITLE;
                }
            }
            R.id.recreate->{
                recreate()
                resetStateSearch()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    //Codice della richiesta del permesso per accedere alla memoria
    companion object{
        private const val REQUEST_READ_EXTERNAL_STORAGE:Int=1
        var twoPane: Boolean = false
    }
}
