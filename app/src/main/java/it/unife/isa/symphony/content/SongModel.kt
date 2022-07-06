package it.unife.isa.symphony.content

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import it.unife.isa.symphony.SongOpenHelper
import java.util.UUID

/**
 * Classe singleton che incapsula i dati riguardanti le canzoni.
 * Si accupa di eseguire l'accesso ai dati: caricamento, salvataggio, cancellazione
 * delle strutture dati runtime e dal database
 */

object SongModel {

    //Array di canzoni
    var SONG_ITEMS: MutableList<Song> = ArrayList()
    //Mappa di canzioni accessibili tramite l'ID usato come chiave
    var SONG_MAP: MutableMap<UUID, Song> = HashMap()

    //Array di canzoni cercate
    var SEARCH_SONG_ITEMS: MutableList<Song> = ArrayList()
    //Mappa di canzioni cercate
    var SEARCH_SONG_MAP: MutableMap<UUID, Song> = HashMap()

    // Quando si fa la ricerca, si setta searched a True. Nel tornare a @SongsListActivity
    var searched = false
    var searchFor=0

    const val TITLE=0
    const val ARTIST = 1
    const val GENRE = 2

    //Caricamento della lista di canzoni dal db alla lista e alla mappa
    fun loadSongs(context: Context)
    {
        val myHelper=SongOpenHelper(context)
        val db=myHelper.writableDatabase   //Riferimento al database dell'app

        //Selezione di tutti gli elementi della tabella song
        val cursor=db.rawQuery("SELECT * FROM ${SongOpenHelper.TABLE}",null)

        //Per ogni record della tabella si crea un oggetto song e si aggiunge alla lista ed alla mappa
        if(cursor!=null && cursor.moveToFirst())
        {
            do {
                //Creazione oggetto
                val id=UUID.fromString(cursor.getString(0))
                val title=cursor.getString(1)
                val artist=cursor.getString(2)
                val uri=Uri.parse(cursor.getString(3))
                val duration=cursor.getString(4)
                val genre=cursor.getString(5)
                val s=Song(id,title,artist,uri,duration,genre)

                //Inserimento nella lista e nella mappa
                SONG_ITEMS.add(s)
                SONG_MAP.put(s.id, s)
            }while (cursor.moveToNext())

            cursor.close()
        }
        db.close()
    }

    //Salvataggio della canzone inserita nel db
    fun saveSong(context: Context, s:Song)
    {
        SONG_ITEMS.add(s)
        SONG_MAP.put(s.id, s)

        val myHelper=SongOpenHelper(context)
        val db=myHelper.writableDatabase

        val value= ContentValues()
        value.put(SongOpenHelper.id,s.id.toString())
        value.put(SongOpenHelper.title,s.titolo)
        value.put(SongOpenHelper.artist,s.artista)
        value.put(SongOpenHelper.uri,s.uri.toString())
        value.put(SongOpenHelper.duration,s.durata)
        value.put(SongOpenHelper.genre,s.genre)
        db.insert(SongOpenHelper.TABLE,null,value)
        db.close()
    }

    //Si verifica se un titolo di una canzone è già presente nella lista.
    fun isAlreadyIn(s: Song): Boolean
    {
        var added=false
        for(e in SONG_ITEMS)
        {
            if(e.titolo==s.titolo)
                added=true
        }
        return added
    }

    //Rimuove una canzone selezionata rispettivamente della lista, dalla mappa e dal db
    fun deleteSong(context: Context, id:UUID, pos:Int)
    {
        //Si rimuove la canzone dalla HahMap con l'id memorizzato nel cassetto
        SONG_MAP.remove(id)

        //Si rimuove la canzone anche dalla lista utilizzando invece la posizione assoluta dell'adapter
        SONG_ITEMS.removeAt(pos)

        //Rimozione canzone dal db
        val myHelper=SongOpenHelper(context)
        val db=myHelper.writableDatabase
        db.delete(SongOpenHelper.TABLE,"id=?", arrayOf(id.toString()))
        db.close()
    }

    //Restitusce la canzone successiva a quella passata.
    //Se è l'ultima canzone allora si ritorna la prima della lista.
    fun getNext(s:Song):Song
    {
        var index= SONG_ITEMS.indexOf(s)
        if(index+1< SONG_ITEMS.size)
        {
            index+=1
            return SONG_ITEMS[index]
        }
        return SONG_ITEMS[0]
    }

    //Restistuisce la canzone precedente a quella passata.
    //Se è la prima canzone allora si ritorna l'ultima
    fun getPreviuos(s:Song):Song
    {
        var index= SONG_ITEMS.indexOf(s)
        if(index-1>=0)
        {
            index-=1
            return SONG_ITEMS[index]
        }
        return SONG_ITEMS[SONG_ITEMS.size-1]
    }

    //In base all'attributo di ricerca, si inseriscono le canzoni
    //nelle strutture dati di ricerca
    fun searchBy(attributo: Int, keyword: String)
    {
        if(attributo==ARTIST) {
            for (e in SONG_ITEMS)
                if (e.artista == keyword)
                {
                    SEARCH_SONG_ITEMS.add(e)
                    SEARCH_SONG_MAP.put(e.id,e)
                }
        }
        else if(attributo==GENRE) {
            for (e in SONG_ITEMS)
                if (e.genre == keyword)
                {
                    SEARCH_SONG_ITEMS.add(e)
                    SEARCH_SONG_MAP.put(e.id,e)
                }
        }
        else if(attributo==TITLE) {
            for (e in SONG_ITEMS)
                if (e.titolo == keyword)
                {
                    SEARCH_SONG_ITEMS.add(e)
                    SEARCH_SONG_MAP.put(e.id,e)
                }
        }
    }

    fun resetStateSearch()
    {
        searchFor=0
        searched=false
        SEARCH_SONG_ITEMS=ArrayList()
        SEARCH_SONG_MAP= HashMap()
    }

    //Classe dati che rappresenta una canzone
    //L'id della canzone è un valore long UUID generato casualmente
    data class Song(val id: UUID, val titolo: String, val artista: String, val uri: Uri, val durata:String, val genre:String)
}