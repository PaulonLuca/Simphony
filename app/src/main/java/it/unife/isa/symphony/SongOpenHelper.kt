package it.unife.isa.symphony

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

//Classe helper per la gestione del database
class SongOpenHelper(context: Context): SQLiteOpenHelper(context, DB_NAME,null, DB_VERSION)
{
    //Creazione della tabella per la memorizzazione delle canzoni con le colonne:
    //ID | Titolo | Artista | URI | Durata
    override fun onCreate(db: SQLiteDatabase?) {
        val sql="CREATE TABLE $TABLE ( $id VARCHAR(255), $title VARCHAR(255), $artist VARCHAR(255),${uri} VARCHAR(255),${duration} VARCHAR(255),${genre} VARCHAR(255),PRIMARY KEY(${id}) );"
        // TODO aggiungere immagine alla tabella
        db?.execSQL(sql)
    }

    //Da implementare nel caso di futuri aggiornamenti al database
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
    }

    //Definizione delle costanti che definiscono i nomi del database, della tabella e delle colonne
    companion object
    {
        private const val DB_NAME="songdb.db"
        private const val DB_VERSION=1
        const val TABLE="song"
        const val id="id"
        const val title="title"
        const val artist="artist"
        const val uri="uri"
        const val duration="duration"
        const val genre="genre"
    }
}