package it.unife.isa.symphony


import android.net.Uri
import android.os.Bundle
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

    override fun onResume() {
        super.onResume()
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        val rootView = inflater.inflate(R.layout.fragment_play, container, false)

        // Visualizza titolo nell'interfaccia di riproduzione
        selectedSong?.let {
            rootView.findViewById<TextView>(R.id.tv_palyingSong).text = it.titolo
        }

        return rootView
    }


    //Quando si distrgge il fragment ci si scollega dal servizio
    override fun onDestroy() {
        super.onDestroy()
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