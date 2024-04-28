package com.example.audiovideorecordeer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.util.SparseIntArray
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.audiovideorecordeer.databinding.RecordingAudioVideoBinding
import java.io.IOException

class RecordActivity : AppCompatActivity(), SurfaceHolder.Callback {
    private lateinit var binding: RecordingAudioVideoBinding
    private lateinit var mediaRecorder: MediaRecorder
    private var isRecordingVideo: Boolean = false
    private var outputFilePath: String = ""

    private lateinit var cameraManager: CameraManager
    private lateinit var cameraDevice: CameraDevice
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private lateinit var cameraCaptureSession: CameraCaptureSession

    private val ORIENTATIONS = SparseIntArray()

    init {
        ORIENTATIONS.append(Surface.ROTATION_0, 90)
        ORIENTATIONS.append(Surface.ROTATION_90, 0)
        ORIENTATIONS.append(Surface.ROTATION_180, 270)
        ORIENTATIONS.append(Surface.ROTATION_270, 180)
    }

    private val REQUEST_PERMISSION_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = RecordingAudioVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Solicitar permisos necesarios
        requestPermissionsIfNeeded()

        // Determinar si se está grabando audio o video basándose en el Intent
        isRecordingVideo = intent.getBooleanExtra("isRecordingVideo", false)

        if (isRecordingVideo) {
            // Si se está grabando video, ocultar la imagen de la nota musical y mostrar la vista previa de la cámara
            binding.musicNote.visibility = View.GONE
            binding.cameraPreview.visibility = View.VISIBLE
        } else {
            // Si se está grabando audio, ocultar la vista previa de la cámara y mostrar la imagen de la nota musical
            binding.musicNote.visibility = View.VISIBLE
            binding.cameraPreview.visibility = View.GONE
        }

        binding.cameraPreview.holder.addCallback(this)

        binding.startButton.setOnClickListener {
            startRecording()
        }

        binding.stopButton.setOnClickListener {
            stopRecording()
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (isRecordingVideo) {
            try {
                cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
                val cameraId = cameraManager.cameraIdList[0]
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) as StreamConfigurationMap
                val sizes = map.getOutputSizes(SurfaceTexture::class.java)
                val size = sizes[0] // Aquí seleccionamos el tamaño de vista previa

                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.CAMERA
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // Manejar el caso donde no se otorgan los permisos
                    return
                }

                cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        cameraDevice = camera
                        val surface = holder.surface
                        val surfaceTexture = if (surface is SurfaceTexture) {
                            surface
                        } else {
                            surface?.let {
                                SurfaceTexture(it.hashCode())
                            }
                        }

                        // Configurar la orientación de la vista previa
                        val rotation = windowManager.defaultDisplay.rotation
                        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                        captureRequestBuilder.addTarget(surface)
                        captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation))

                        cameraDevice.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                cameraCaptureSession = session
                                captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                                cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
                            }

                            override fun onConfigureFailed(session: CameraCaptureSession) {
                                // Manejar el error de configuración de la sesión
                            }
                        }, null)
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        // Manejar la cámara desconectada
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        // Manejar el error de la cámara
                    }
                }, null)
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        }
    }


    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // Si cambia el tamaño o el formato de la superficie, puedes ajustar la configuración de la cámara aquí
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // Aquí puedes liberar los recursos de la cámara
        if (isRecordingVideo) {
            // Detener y liberar MediaRecorder
            mediaRecorder.stop()
            mediaRecorder.release()
        }
    }

    private fun requestPermissionsIfNeeded() {
        val permissionsToRequest = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        val permissionsNotGranted = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsNotGranted.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNotGranted, REQUEST_PERMISSION_CODE)
        }
    }

    private fun startRecording() {
        // Configurar MediaRecorder para la grabación de audio
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.DEFAULT)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            val fileName = "${System.currentTimeMillis()}.mp4"
            val externalDir = getExternalFilesDir("audio")
            outputFilePath = "${externalDir?.absolutePath}/$fileName"
            setOutputFile(outputFilePath)
            prepare()
            start()
        }
    }

    private fun stopRecording() {
        // Detener y liberar MediaRecorder
        mediaRecorder.stop()
        mediaRecorder.release()

        // Devolver la ruta del archivo grabado a MainActivity
        val intent = Intent().apply {
            putExtra("filePath", outputFilePath)
        }
        setResult(RESULT_OK, intent)

        // Cerrar esta actividad y volver a MainActivity
        finish()
    }
}