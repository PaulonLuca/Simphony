package it.unife.isa.symphony


import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import it.unife.isa.symphony.content.SongModel
import it.unife.isa.symphony.content.SongModel.Song
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Frammento che rappresenta l'interfaccia di riproduzione.
 * Tale frammento è contenuto sia in [SongsListActivity] su tablet
 * e sia in [SongDetailActivity] su smartphone.
 */
class PlayFragment : Fragment() {

    private var selectedSong: Song? = null //Canzone in riproduzione
    private var binder: PlayerService.PlayBinder?=null   //Interfaccia di comunicazione con il servizio
    private var mBound=false               //Booleno che identifica lo stato della connessione
    private var seekbar:SeekBar?=null      //Riferimento alla seekbar
    private var tvTitolo:TextView?=null
    private var tvDuration:TextView?=null
    private var handlare= Handler(Looper.getMainLooper()) //Gestore per l'inserimento nella coda messaggi dell'UI thread (main thread)
    private lateinit var runnable:Runnable               //dell'oggetto runnable, contenente il codice da eseguire

    //Bindig dal fragment per comunicare con il servizio: play, pausa, stop, next, prev, seek
    //Si esegue il bind quando l'app risulta in foregeround e ci si scollega quando il fragment
    //viene distrutto
    private val mConnection: ServiceConnection =object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            mBound=false
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            binder=service as PlayerService.PlayBinder
            mBound=true

            //Si mette in play la canzone selezionata
            binder?.setSong(selectedSong!!)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(savedInstanceState==null)
        {
            arguments?.let {
                if (it.containsKey(SONG_ID)) {
                    //Imposta la canzone selezionata come canzone corrente
                    selectedSong = SongModel.SONG_MAP[UUID.fromString(it.getString(SONG_ID))]
                }
            }
        }
        else
        {
            //Ripristino dello stato dell'istanza, cioè della canzone corrente in riproduzione
            val songId=savedInstanceState.getString(SONG_ID)
            val songTitle=savedInstanceState.getString(SONG_TITLE)
            val songArtist=savedInstanceState.getString(SONG_ARTIST)
            val songUri=savedInstanceState.getString(SONG_URI)
            val songDuration=savedInstanceState.getString(SONG_DURATION)
            val songGenre=savedInstanceState.getString(SONG_GENRE)
            selectedSong= Song(UUID.fromString(songId),songTitle!!,songArtist!!, Uri.parse(songUri),songDuration!!,songGenre!!)
        }
    }

    //Quando il frammento è in foreground ci si collega in modalità bound al servizio
    override fun onResume() {
        super.onResume()
        val i= Intent(context?.applicationContext,PlayerService::class.java)
        activity?.bindService(i,mConnection, Context.BIND_AUTO_CREATE)

        //Codice da mettere periodicamente in coda, ogni secondo, all'UI thread per spostare la seekbar
        runnable= Runnable {
            if(mBound)
                seekbar?.progress=binder?.currentPosition()!!
            handlare.postDelayed(runnable,1000)
        }
        handlare.postDelayed(runnable,1000)
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        val rootView = inflater.inflate(R.layout.fragment_play, container, false)

        // Visualizza titolo nell'interfaccia di riproduzione
        selectedSong?.let {
            rootView.findViewById<TextView>(R.id.tv_palyingSong).text = it.titolo
        }

        seekbar=rootView.findViewById(R.id.seekBar)
        //Impostazione del limite superiore della seekbar, cioè il massimo valore possibile in ms
        seekbar?.max=selectedSong?.durata?.toInt()!!

        //Muovendo la seekbar avanti o indietro ci si muove nella canzone e si aggiorna il mediaplayer
        seekbar?.setOnSeekBarChangeListener(object :SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if(mBound)
                {
                    if(fromUser)//se è l'utente a modificare la posizone nella seekbar
                    {
                        binder?.seekTo(progress)
                        seekBar?.progress=progress
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) { }

            override fun onStopTrackingTouch(seekBar: SeekBar?) { }
        })

        return rootView
    }


    //Quando si distrgge il fragment ci si scollega dal servizio
    override fun onDestroy() {
        super.onDestroy()
        activity?.unbindService(mConnection)
        handlare.removeCallbacksAndMessages(null)
    }


    override fun onDestroyView() {
        super.onDestroyView()
    }

    //Salvataggio stato della stato dell'istanza cioe della canzone corrente
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(SONG_ID,selectedSong?.id.toString())
        outState.putString(SONG_TITLE,selectedSong?.titolo)
        outState.putString(SONG_ARTIST,selectedSong?.artista)
        outState.putString(SONG_URI,selectedSong?.uri.toString())
        outState.putString(SONG_DURATION,selectedSong?.durata)
        outState.putString(SONG_GENRE,selectedSong?.genre)
        super.onSaveInstanceState(outState)
    }

    companion object {
        //Chiavi per il salvataggio dello stato dell'istanza nel bundle
        const val SONG_ID = "song_id"
        const val SONG_TITLE = "song_title"
        const val SONG_ARTIST = "song_artist"
        const val SONG_URI = "song_uri"
        const val SONG_DURATION = "song_duration"
        const val SONG_GENRE = "song_genre"
    }
}