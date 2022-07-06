package it.unife.isa.symphony


import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.*
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import it.unife.isa.symphony.content.SongModel
import it.unife.isa.symphony.content.SongModel.Song
import java.util.*

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
    private var tvArtista:TextView?=null
    private var tvDuration:TextView?=null
    private lateinit var btnPlay: ImageButton
    private lateinit var current_time: TextView
    private lateinit var song_image: ImageView
    private var handlare= Handler(Looper.getMainLooper()) //Gestore per l'inserimento nella coda messaggi dell'UI thread (main thread)
    private lateinit var runnable:Runnable               //dell'oggetto runnable, contenente il codice da eseguire
    private var paused: Boolean = false

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

    //Broadcast receiver che ha il compito di ricevere l'intent locale inviato dal service quando
    //completa la riproduzione della canzone. Se ci sono almeno 2 canzoni si
    //passa a riprodurre la canzone successiva
    private val fragmentReceiver: BroadcastReceiver =object: BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            if(intent?.action=="SongComplete")
            {
                if(SongModel.SONG_ITEMS.size>1)
                {
                    //Completamento riproduzione da parte del mediaplayer-->Si cambia la canzone
                    val nextSong=SongModel.getNext(selectedSong!!)
                    changeSong(nextSong)
                }
            }
            // Intent ricevuto quando desideriamo riprodurre una canzone che è gia in esecuzione (o in pausa)
            // oppure in risposta ad un evento di "Audio Becoming Noisy"
            if(intent?.action=="ChangeButton") {

                // Chiediamo al service lo stato di pausa della canzone
                paused = binder!!.getPause()

                // Modifichiamo di conseguenza l'aspetto del bottone play/pause
                if (paused) btnPlay.setImageResource(R.drawable.play_button)
                else btnPlay.setImageResource(R.drawable.pause_button)

            }

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
            paused = savedInstanceState.getBoolean(PAUSED, false)
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
        //Registrazione del broadcast receiver per la notifica da parte del service del completamento della riproduzione.
        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(fragmentReceiver, IntentFilter("SongComplete"))
        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(fragmentReceiver, IntentFilter("ChangeButton"))
        val rootView = inflater.inflate(R.layout.fragment_play, container, false)

        // Visualizza titolo nell'interfaccia di riproduzione
        selectedSong?.let {
            rootView.findViewById<TextView>(R.id.tv_palyingSong).text = it.titolo
            rootView.findViewById<TextView>(R.id.song_artist).text = it.artista
            rootView.findViewById<TextView>(R.id.tv_duration).text = convertDuration(it.durata.toLong())
        }
        binder?.setSong(selectedSong!!)

        //Si recuperano elementi grafici dell'interfaccia: bottoni play, pausa, next, prev, stop
        //e si agganciano i listener
        btnPlay=rootView.findViewById(R.id.btn_play)
        val btnNext=rootView.findViewById<ImageButton>(R.id.btn_next)
        val btnPrev=rootView.findViewById<ImageButton>(R.id.btn_previous)
        val btnStop=rootView.findViewById<ImageButton>(R.id.btn_stop)
        val sec_forward=rootView.findViewById<ImageButton>(R.id.skip_10)
        val sec_backward=rootView.findViewById<ImageButton>(R.id.rewind_10)
        tvTitolo=rootView.findViewById(R.id.tv_palyingSong)
        tvArtista=rootView.findViewById(R.id.song_artist)
        current_time = rootView.findViewById(R.id.current_time)
        tvDuration=rootView.findViewById(R.id.tv_duration)
        tvDuration?.text=convertDuration(selectedSong?.durata?.toLong()!!)
        seekbar=rootView.findViewById(R.id.seekBar)
        song_image = rootView.findViewById(R.id.song_image)
        //Impostazione del limite superiore della seekbar, cioè il massimo valore possibile in ms
        seekbar?.max=selectedSong?.durata?.toInt()!!

        updateThumbnail(selectedSong!!.uri) // Aggiorno la copertina della canzone

        // Faccio in modo che il titolo e l'artista siano "selected". Questo permette di ottenere
        // uno slide del contenuto di queste View quando risulta troppo lungo per essere interamente
        // contenuto nella TextView.
        tvTitolo!!.setSelected(true)
        tvArtista!!.setSelected(true)

        if(paused) btnPlay.setImageResource(R.drawable.play_button) // modifico l'icona del button
        // Listener per il bottone di Pause/Play
        btnPlay.setOnClickListener {
            if(paused) { // Se la canzone è in pausa e il pulsante viene premuto...
                binder!!.resume() //... chiedo al service di riprendere l'esecuzione,
                paused = binder!!.getPause() // aggiorno la variabile paused
                btnPlay.setImageResource(R.drawable.pause_button) // modifico l'icona del button
            }
            else // al contrario nel caso in cui la canzone sia in riproduzione e il pulsante venga premuto
            {
                binder!!.pause()
                paused = binder!!.getPause()
                btnPlay.setImageResource(R.drawable.play_button)
            }
        }

        //Si riprocude la canzone successiva
        btnNext.setOnClickListener {
            if(mBound)
            {
                //canzone successiva, se ci sono almeno 2 canzoni
                if(SongModel.SONG_ITEMS.size>1)
                {
                    val nextSong=SongModel.getNext(selectedSong!!)
                    changeSong(nextSong)
                    paused=false
                    btnPlay.setImageResource(R.drawable.pause_button)
                    binder?.setSong(nextSong)
                }
            }
        }

        //Si riproduce la canzone precedente
        btnPrev.setOnClickListener {
            if(mBound)
            {
                //canzone precedente, se ci sono almeno 2 canzoni
                if(SongModel.SONG_ITEMS.size>1)
                {
                    val prevSong=SongModel.getPreviuos(selectedSong!!)
                    changeSong(prevSong)
                    paused=false
                    btnPlay.setImageResource(R.drawable.pause_button)
                    binder?.setSong(prevSong)
                }
            }
        }

        // Listener per il bottone di Avanti 10 secondi
        sec_forward.setOnClickListener {
            binder!!.goAhead(9200) // chiede al service di avanzare di 9200msec con la riproduzione della canzone
        } // NB: Non uso 10000msec perché l'aggiornamento della seekbar avviene ogni secondo e l'utente avrebbe l'impressione
        // aver avanzato 11sec e non 10sec

        // Listener per il bottone di Indietro 10 secondi
        sec_backward.setOnClickListener {
            binder!!.goBack(9200) // chiede al service di tornare indietro di 9200msec con la riproduzione della canzone
        }

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
                    current_time.text = convertDuration(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) { }

            override fun onStopTrackingTouch(seekBar: SeekBar?) { }
        })

        btnStop.setOnClickListener{
            if(mBound)
            {
                binder?.stop()
                seekbar?.progress=0// si riporta la seek bar all'inizio
                //Interruzione dell'inserimento di messaggi di esecuzione in coda all'UI thread
                handlare.removeCallbacks(runnable)
                if(SongsListActivity.twoPane)
                {
                    // Se il layout è two-pane, semplicemente svuoto il layout staccando il fragment
                    requireActivity().supportFragmentManager.beginTransaction()
                        .detach(this)
                        .commit()
                }
                // Se invece il layout è single-pane, chiedo di tornare all'activity che mostra la lista di elementi
                else {
                    getActivity()?.onBackPressed()
                }
            }
        }

        // Listener per lo Swipe Left e Right
        rootView.setOnTouchListener(object : OnSwipeTouchListener() {
            override fun onSwipeLeft() { // Swipe Left == Skip Next
                if(mBound)
                {
                    //canzone successiva, se ci sono almeno 2 canzoni
                    if(SongModel.SONG_ITEMS.size>1)
                    {
                        val nextSong=SongModel.getNext(selectedSong!!)
                        changeSong(nextSong)
                        paused=false
                        btnPlay.setImageResource(R.drawable.pause_button)
                        binder?.setSong(nextSong)
                    }
                }
            }
            override fun onSwipeRight() { // Swipe Right == Skip Previous
                if(mBound)
                {
                    //canzone precedente, se ci sono almeno 2 canzoni
                    if(SongModel.SONG_ITEMS.size>1)
                    {
                        val prevSong=SongModel.getPreviuos(selectedSong!!)
                        changeSong(prevSong)
                        paused=false
                        btnPlay.setImageResource(R.drawable.pause_button)
                        binder?.setSong(prevSong)
                    }
                }
            }
        })

        return rootView
    }

    //Aggiornamento titolo, durata, massimo valore seekbar e minimo valore seekbar
    private fun changeSong(s:Song)
    {
        selectedSong=s
        tvTitolo?.text=selectedSong?.titolo
        tvArtista?.text=selectedSong?.artista
        tvDuration?.text=convertDuration(selectedSong?.durata?.toLong()!!)
        seekbar?.max=selectedSong?.durata?.toInt()!!
        seekbar?.progress=0
        updateThumbnail(selectedSong!!.uri) // Aggiorno la copertina della canzone
    }


    //Quando si distrgge il fragment ci si scollega dal servizio
    override fun onDestroy() {
        super.onDestroy()
        activity?.unbindService(mConnection)
        handlare.removeCallbacksAndMessages(null)
    }


    override fun onDestroyView() {
        LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(fragmentReceiver)
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
        outState.putBoolean(PAUSED, paused)
        super.onSaveInstanceState(outState)
    }

    // Listener per gli eventi di Swipe
    open class OnSwipeTouchListener : View.OnTouchListener {

        private val gestureDetector = GestureDetector(GestureListener())

        fun onTouch(event: MotionEvent): Boolean {
            return gestureDetector.onTouchEvent(event)
        }

        private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            override fun onDown(e: MotionEvent): Boolean {
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                onTouch(e)
                return true
            }

            override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                val result = false
                try {
                    val diffY = e2.y - e1.y
                    val diffX = e2.x - e1.x
                    if (Math.abs(diffX) > Math.abs(diffY)) {
                        if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffX > 0) {
                                onSwipeRight()
                            } else {
                                onSwipeLeft()
                            }
                        }
                    } else {}
                }
                catch (e: Exception) {
                    e.printStackTrace()
                }
                return result
            }
        }

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            return gestureDetector.onTouchEvent(event)
        }

        open fun onSwipeRight() {}

        open fun onSwipeLeft() {}

    }

    // Funzione che converte gli istanti di tempo da msec al formato stringa "HH:MM:SS"
    private fun convertDuration(duration: Long): String {
        var out: String = ""
        var hours: Long = 0
        hours = duration / 3600000

        val remaining_minutes = (duration - hours * 3600000) / 60000
        var minutes = remaining_minutes.toString()
        val remaining_seconds = (duration - hours * 3600000 - remaining_minutes * 60000) / 1000
        var seconds = remaining_seconds.toString()
        if (seconds.length == 1) seconds = "0"+"$seconds"
        out = if (hours > 0) {
            "$hours:$minutes:$seconds"
        } else {
            "$minutes:$seconds"
        }
        return out
    }

    // Funzione che si occupa di aggiornare la copertina della canzone provando a cercarla in memoria
    private fun updateThumbnail(uri: Uri) {
        Thread{ // Eseguo in un thread separato
            song_image.post(Runnable {
                val mmr = MediaMetadataRetriever()
                val rawArt: ByteArray?
                val art: Bitmap
                val bfo = BitmapFactory.Options()
                try{
                    mmr.setDataSource(requireContext(), uri) // Accedo ai metadati della canzone
                    rawArt = mmr.embeddedPicture // Chiedo di accedere all'immagine associata al file e la salvo in un ByteArray
                    if (null != rawArt) { // Se trovo un'immagine di copertina...
                        art = BitmapFactory.decodeByteArray(rawArt, 0, rawArt.size, bfo) //...decodifico l'array in un BitMap...
                        song_image.setImageBitmap(art) //...e modifico l'interfaccia.
                    } else song_image.setImageResource(R.drawable.no_image3) // Altrimenti uso l'immagine standard
                }
                catch (e: Exception) {
                    song_image.setImageResource(R.drawable.no_image3) // se catturo un'eccezione semplicemente mostro l'immagine standard
                }
            })}.start()
    }


    companion object {
        //Chiavi per il salvataggio dello stato dell'istanza nel bundle
        const val SONG_ID = "song_id"
        const val SONG_TITLE = "song_title"
        const val SONG_ARTIST = "song_artist"
        const val SONG_URI = "song_uri"
        const val SONG_DURATION = "song_duration"
        const val SONG_GENRE = "song_genre"
        const val PAUSED = "paused"
    }
}