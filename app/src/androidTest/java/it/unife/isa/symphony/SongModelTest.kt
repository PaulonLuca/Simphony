package it.unife.isa.symphony

import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.unife.isa.symphony.content.SongModel
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import org.junit.Before
import java.util.*

@RunWith(AndroidJUnit4::class)
class SongModelTest {
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup()
    {
        SongModel.SONG_ITEMS =ArrayList()
        SongModel.SONG_MAP = HashMap()
        SongModel.resetStateSearch()
        SongModel.loadSongs(appContext)
    }

    @Test
    fun addSong() {
        val startSize=SongModel.SONG_ITEMS.size
        SongModel.saveSong(appContext,
            SongModel.Song(
                UUID.randomUUID(),
                "title",
                "artist",
                Uri.parse("Uri"),
                "duration",
                "genre"
            )
        )
        assertEquals(SongModel.SONG_ITEMS.size,startSize+1)
    }

    @Test
    fun deleteSong() {
        val startSize=SongModel.SONG_ITEMS.size
        if(startSize>0)
        {
            val pos=(0..startSize-1).random()
            SongModel.deleteSong(appContext,SongModel.SONG_ITEMS[pos].id,pos)
            assertEquals(SongModel.SONG_ITEMS.size,startSize-1)
        }
        else assertEquals(SongModel.SONG_ITEMS.size,startSize)
    }

    @Test
    fun songAlreadyInTrue() {
        val s=SongModel.Song(
            UUID.randomUUID(),
            "title",
            "artist",
            Uri.parse("Uri"),
            "duration",
            "genre"
        )
        SongModel.saveSong(appContext,s)
        assertTrue(SongModel.isAlreadyIn(s))

    }

    @Test
    fun songAlreadyInFalse() {
        val s=SongModel.Song(
            UUID.randomUUID(),
            "different title",
            "artist",
            Uri.parse("Uri"),
            "duration",
            "genre"
        )
        assertFalse(SongModel.isAlreadyIn(s))
    }

    @Test
    fun getNextSong() {
        val startSize=SongModel.SONG_ITEMS.size
        if(startSize>0)
        {
            val pos=(0..startSize-1).random()
            assertEquals(SongModel.getNext(SongModel.SONG_ITEMS[pos]),SongModel.SONG_ITEMS[(pos+1)%startSize])
        }
        else assertTrue(true)
    }

    @Test
    fun getPrevSong() {
        val startSize=SongModel.SONG_ITEMS.size
        if(startSize>0)
        {
            var pos=(0..startSize-1).random()
            if(pos>0)
                assertEquals(SongModel.getPreviuos(SongModel.SONG_ITEMS[pos]),SongModel.SONG_ITEMS[pos-1])
            else
                assertEquals(SongModel.getPreviuos(SongModel.SONG_ITEMS[pos]),SongModel.SONG_ITEMS[startSize-1])
        }
        else assertTrue(true)
    }

    @Test
    fun searchSongByTitleTrue()
    {
        val startSize=SongModel.SONG_ITEMS.size
        if(startSize>0)
        {
            var pos=(0..startSize-1).random()
            val s = SongModel.SONG_ITEMS[pos]
            SongModel.searchBy(SongModel.TITLE,s.titolo)
            assertEquals(SongModel.SEARCH_SONG_ITEMS[0].titolo,s.titolo)
        }
        else assertTrue(true)
    }

    @Test
    fun searchSongByArtistTrue()
    {
        val startSize=SongModel.SONG_ITEMS.size
        if(startSize>0)
        {
            var pos=(0..startSize-1).random()
            val s = SongModel.SONG_ITEMS[pos]
            SongModel.searchBy(SongModel.ARTIST,s.artista)
            assertEquals(SongModel.SEARCH_SONG_ITEMS[0].artista,s.artista)
        }
        else assertTrue(true)
    }

    @Test
    fun searchSongByGenreTrue()
    {
        val startSize=SongModel.SONG_ITEMS.size
        if(startSize>0)
        {
            var pos=(0..startSize-1).random()
            val s = SongModel.SONG_ITEMS[pos]
            SongModel.searchBy(SongModel.GENRE,s.genre)
            assertEquals(SongModel.SEARCH_SONG_ITEMS[0].genre,s.genre)
        }
        else assertTrue(true)
    }

    @Test
    fun searchSongByTitleFalse()
    {
        val startSize=SongModel.SONG_ITEMS.size
        if(startSize>0)
        {
            val s = "vuota"
            SongModel.searchBy(SongModel.TITLE,s)
            assertEquals(0,SongModel.SEARCH_SONG_ITEMS.size)
        }
        else assertTrue(true)
    }

    @Test
    fun searchSongByArtistFalse()
    {
        val startSize=SongModel.SONG_ITEMS.size
        if(startSize>0)
        {
            val s = "vuota"
            SongModel.searchBy(SongModel.ARTIST,s)
            assertEquals(0,SongModel.SEARCH_SONG_ITEMS.size)
        }
        else assertTrue(true)
    }

    @Test
    fun searchSongByGenreFalse()
    {
        val startSize=SongModel.SONG_ITEMS.size
        if(startSize>0)
        {
            val s = "vuota"
            SongModel.searchBy(SongModel.GENRE,s)
            assertEquals(0,SongModel.SEARCH_SONG_ITEMS.size)
        }
        else assertTrue(true)
    }



}