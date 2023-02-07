package it.unife.isa.symphony


import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import it.unife.isa.symphony.content.SongModel
import it.unife.isa.symphony.content.SongModel.Song
import java.lang.Exception


class PlayerService : Service() {

    private var mp:MediaPlayer
    private var isPlaying=false //verifica se si è in riproduzione, poichè mp.isPlaying()
    // si può chiamare solo se si è in started state non in prepared state
    private var isPaused = false

    private var isStopped=false //verifica che il mediaplayer è stato stoppato, cioè si è azzerata la canzone corrente
    private var notificationBuilder: Notification.Builder?=null //Costruttore della notifica per l'utente
    private val binder=PlayBinder()      //Riferimento all'interfaccia di comunicazione con il servizio
    private var currentSong: Song?=null  //Canzone in riproduzione

    //Appena il servizio parte si crea il mediaplayer e si avvia un thread
    //che ha il compito di preparare il media player, poichè tale azione potrebbe richiedere
    //diverso tempo
    init{
        mp= MediaPlayer()//Creazione media player
        setAttributes()  //Impostazione attributi audio per ottimizzare la riproduzione in base al tipo di sorgente

        //Quando si termina la canzone si passa alla successiva e si invia un'intent locale al fragment
        //che se è visualizzato si deve modificare
        mp.setOnCompletionListener {
            //Notifico che ho terminato la riproduzione della canzone con un intent
            //ed avvio la canzone successiva se ci sono almeno 2 canzoni
            LocalBroadcastManager.getInstance(this).sendBroadcast(Intent("SongComplete"))
            if(SongModel.SONG_ITEMS.size>1)
                binder.setSong(SongModel.getNext(currentSong!!))
            else
                binder.stop()
        }
    }

    private fun setAttributes()
    {
        //Impostazione tipologia di audio riporodotta dal mediaplayer per ottimizzare la riproduzione
        mp.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)//riproduzione di un media
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)//riproduzione di una canzone
                .build())
    }

    override fun onCreate() {
        super.onCreate()

        //In base all'API level si decide quale costruttore usare per la notifica
        notificationBuilder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                Notification.Builder(this, CHANNEL_ID)
            else
                Notification.Builder(this)

        //Creazione del channel per mostrare la notifica se si è su un versione di API da 26 in su.
        //Si controlla se versione di API sul dispositivo SDK_INT è >= API 26
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //nome del canale
            val name: CharSequence = getString(R.string.channel_name)
            //descrizione del canale
            val description = getString(R.string.channel_desc)
            //imposratanza del canale
            val importance = NotificationManager.IMPORTANCE_MIN
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            channel.description = description
            // Registrazione del canale nel sistema attraverso notification manager
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    //Ritorna l'interfaccia per comunicare con il servizio
    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_REDELIVER_INTENT
    }

    //Quando il servizio viene distrutto si ferma il mediaplayer, si rilascia la risorsa
    //si toglie il servizio dal foreground e si rimuove la notifica
    override fun onDestroy() {
        super.onDestroy()
        mp.stop()
        mp.release()
        stopForeground(true)
    }

    inner class PlayBinder: Binder()
    {
        //Chiamata appena viene visualizzata l'interfaccia di riproduzione, per impostare la canzone selezionata
        //subito dopo tale chiamata si invoca sempre il metodo play()
        fun setSong(s:Song)
        {
            //Se è stata selezionata una canzone diversa da quella corrente, allora va interrotta
            //quella corrente e inizializzato nuovamente il mediaplayer.
            //Se invece la canzone è la stessa si continua la riproduzione con play/pause
            if(s.id!=currentSong?.id )
            {
                try {
                    if(isPlaying)
                    {
                        mp.stop()
                        isPlaying=false
                    }
                    if(isStopped)
                        isStopped=false;

                    //1)stato Idle
                    mp.reset()
                    //2)Stato initialized, si imposta l'uri della canzone da suonare
                    setAttributes()//reimposta la tipologia di file ripodotto
                    mp.setDataSource(applicationContext,s.uri) //impostazione sorgente dati

                    //metodo play() chiamato quando il mediaplayer è pronto per riprodurre
                    mp.setOnPreparedListener{
                        binder.play()
                    }

                    currentSong=s
                    //3)Stato prepared
                    mp.prepareAsync() //Inizializzazione del mediaplayer in thread a parte, quando è pronto viene chiamato il listener
                    //mp.isLooping=true
                    //mp.setWakeMode(applicationContext,PowerManager.PARTIAL_WAKE_LOCK)
                }
                catch (ex: Exception)
                {
                    Toast.makeText(applicationContext, "Errore", Toast.LENGTH_SHORT).show()
                }
            }
            else {
                LocalBroadcastManager.getInstance(this@PlayerService).sendBroadcast(Intent("ChangeButton"))
            }
        }

        fun play()
        {
            //Se il mediaplayer era in stato stop per ripartire deve essere prima resettato, si imposta nuovamente
            //la canzone corrente e si lancia la preparazione, appena il media player è pronto si invoca attraverso
            //il listener il metodo play() e si imposta isStopped=false. Ora il primo if non verrà più considerato
            //ma basta solo far partire il mediaplayer
            if(isStopped)
            {
                mp.reset()
                setAttributes()//reimposta la tipologia di file ripodotto
                //mp.isLooping=true
                mp.setDataSource(applicationContext,currentSong!!.uri)
                mp.setOnPreparedListener{
                    isStopped=false
                    binder.play()
                }
                mp.prepareAsync()
            }

            //Se la canzone è in pausa viene fatta ripartire, se era già in riproduzione non si fa nulla
            if(!isPlaying)
            {
                if(!isStopped)
                {
                    mp.setWakeMode(applicationContext,PowerManager.PARTIAL_WAKE_LOCK) //Richiesta di non togliere CPU
                    mp.start()
                    isPlaying=true
                }
                //creazione notifica con informazioni base sulla canzone
                notificationBuilder?.setContentTitle(currentSong?.titolo)
                notificationBuilder?.setContentText(currentSong?.artista)
                notificationBuilder?.setSmallIcon(R.drawable.ic_launcher_foreground)
                notificationBuilder?.setVisibility(Notification.VISIBILITY_PUBLIC)//Notifica visibile anche in bloccoschermo
                val notification = notificationBuilder?.build()

                //ID notifica univoco per la notifica nell'applicazione
                val notificationID = 123456

                //Servizio impostato in foreground. Serve per notificare al sistema che
                //l'uccisione del servizio è distruttiva per l'utente. Si imposta in oltre la notifica.
                startForeground(notificationID, notification)
            }
        }

        fun pause()
        {
            //Se la canzone è in riproduzione si mette in pausa, se era già in pausa non si fa nulla
            if(isPlaying)
            {
                mp.pause()
                isPlaying=false
                isPaused=true
            }
        }

        fun resume()
        {
            //Se la canzone è in pausa si mette in riproduzione, se era già in riproduzione non si fa nulla
            isPaused = false
            isPlaying=true
            mp.start()
        }


        fun getPause(): Boolean
        {
            //Funzione che restituisce lo stato di pausa della canzone
            return isPaused
        }

        fun stop()
        {
            //Si mette in stato stop il mediaplayer, si interrompe quindi la riproduzione della canzone
            mp.stop()
            isStopped=true
            isPaused = false
            isPlaying=false
            currentSong=null
            stopForeground(true)//Servizio non più in foreground e rimozione notifica
        }

        //Ritorna la canzone che è in riproduzione
        fun currentSong(): Song?
        { return currentSong }

        //Ritorna la posizione corrente in ms in cui si trova il mediaplayer
        fun currentPosition(): Int
        { return mp.currentPosition }

        //Si setta la posizione del mediaplayer in base alla posizione in cui si trova la seekbar
        fun seekTo(progress:Int)
        { mp.seekTo(progress) }

        // Funzione che modifica la posizione corrente in avanti sulla base di un parametro fornito in input
        fun goAhead(msec: Int) {
            if (currentPosition()+msec <mp.duration) mp.seekTo(currentPosition() + msec)
            else mp.seekTo(mp.duration-1)
        }

        // Funzione che modifica la posizione corrente all'indietro sulla base di un parametro fornito in input
        fun goBack(msec: Int) {
            if (currentPosition()-msec > 0) mp.seekTo(currentPosition()!! - msec)
            else mp.seekTo(0)
        }
    }

    //Costante contenente l'id del canale di notifica
    companion object
    { private  const val CHANNEL_ID="SymphonyBGPlayer" }
}