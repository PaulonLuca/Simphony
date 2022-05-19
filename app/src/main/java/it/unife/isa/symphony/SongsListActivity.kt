package it.unife.isa.symphony

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import it.unife.isa.symphony.content.SongModel
import it.unife.isa.symphony.content.SongModel.Song
import java.util.*

/**
 * Activity che rappresenta una lista di canzoni.
 * Su smartphone rappresenta una lista di canzoni, quando una viene toccata si
 * chiama l'activity [SongDetailActivity] che visualizza i dettagli della canzone
 * Sui tablet l'activity visualizza la lista e l'interfaccia di riproduzione contemporaneamente
 */
class SongsListActivity : AppCompatActivity() {

    private var twoPane: Boolean = false
    private val pickAudioCode=200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Richiesta permesso di accesso alla memoria esterna all'app
        if(ActivityCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_READ_EXTERNAL_STORAGE)
        }

        setContentView(R.layout.activity_songs_list)

        //TODO caricamento canzoni nella lista dal DB

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.title = title
        toolbar.setLogo(R.drawable.ic_launcher_foreground)

        //Click listener per aggiungere una canzone alla lista.
        //Viene richiamata l'activity di sistema ACTION_PICK per recuperare la canzone
        val pickSong = object : View.OnClickListener{
            override fun onClick(v: View?) {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
                try {
                    startActivityForResult(intent, pickAudioCode)
                } catch(e: ActivityNotFoundException) {
                    Toast.makeText(applicationContext, R.string.no_picked_audio, Toast.LENGTH_SHORT).show()
                }
            }
        }

        //Si aggiunge al floating button l'azione per aggiungere una canzone alla lista
        //e si collega al listener pickSong.
        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->
            Snackbar.make(view, "Add song", Snackbar.LENGTH_LONG)
                .setAction("ADD",pickSong).show()
        }

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
            val projection= arrayOf(MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.GENRE)
            val cursor=contentResolver.query(songUri!!,projection,null,null,null)
            if(cursor!=null && cursor.moveToFirst())
            {
                val title=cursor.getString(0)
                val artist=cursor.getString(1)
                val duration=cursor.getString(2)
                val genre=cursor.getString(3)
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

    //Gonfiaggio dell'interfaccia grafica del menù
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main,menu)
        return super.onCreateOptionsMenu(menu)
    }

    //Codice della richiesta del permesso per accedere alla memoria
    companion object{
        private const val REQUEST_READ_EXTERNAL_STORAGE:Int=1
    }
}
