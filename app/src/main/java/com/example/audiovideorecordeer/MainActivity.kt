package com.example.audiovideorecordeer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.audiovideorecordeer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var myAdapter: MyAdapter
    private var myList: ArrayList<MyItem> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializa tu adaptador
        myAdapter = MyAdapter(myList)

        // Configura tu RecyclerView
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = myAdapter
        }

        binding.fab.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.options_menu, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.audio_option -> {
                        // Inicia RecordingActivity para grabar audio
                        val intent = Intent(this, RecordActivity::class.java).apply {
                            putExtra("isRecordingVideo", false)
                        }
                        startActivityForResult(intent, REQUEST_CODE_RECORD_AUDIO)
                        true
                    }
                    R.id.video_option -> {
                        // Inicia RecordingActivity para grabar video
                        val intent = Intent(this, RecordActivity::class.java).apply {
                            putExtra("isRecordingVideo", true)
                        }
                        startActivityForResult(intent, REQUEST_CODE_RECORD_VIDEO)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_RECORD_AUDIO && resultCode == RESULT_OK) {
            // Recibe el resultado de la grabación de audio
            val filePath = data?.getStringExtra("filePath")
            filePath?.let {
                addToList("Recording", "audio", it)
            }
        } else if (requestCode == REQUEST_CODE_RECORD_VIDEO && resultCode == RESULT_OK) {
            // Recibe el resultado de la grabación de video
            val filePath = data?.getStringExtra("filePath")
            filePath?.let {
                addToList("Recording", "video", it)
            }
        }
    }

    fun addToList(title: String, type: String, filePath: String) {
        // Añadir elementos a tu lista
        myList.add(MyItem(title, type, filePath))

        // Notificar al adaptador que los datos han cambiado
        myAdapter.notifyDataSetChanged()
    }

    companion object {
        const val REQUEST_CODE_RECORD_AUDIO = 100
        const val REQUEST_CODE_RECORD_VIDEO = 101
    }
}
