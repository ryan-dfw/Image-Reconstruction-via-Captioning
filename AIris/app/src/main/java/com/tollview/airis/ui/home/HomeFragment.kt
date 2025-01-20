package com.tollview.airis.ui.home

import android.content.ContentValues
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.tollview.airis.databinding.FragmentHomeBinding
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var homeViewModel: HomeViewModel

    private val CAMERA_ENDPOINT = "http://10.0.0.1:10000/sony/camera"
    private var photoUrl: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        observeViewModel()
        buildViews()

        readout("Ready to capture!")

        Thread { pollForPhotoUrls() }.start()
    }

    private fun observeViewModel() {
        homeViewModel.statusMessage.observe(viewLifecycleOwner) { message ->
            binding.readout.alpha = 0f
            binding.readout.text = message
            binding.readout.animate().alpha(1f).setDuration(500).start()
        }
    }

    private fun buildViews() {
        binding.root.post {
            val insets = ViewCompat.getRootWindowInsets(binding.root)
                ?.getInsets(WindowInsetsCompat.Type.systemBars()) ?: return@post

            val activeAreaHeight = binding.root.height - (insets.top + insets.bottom)
            val screenWidth = binding.root.width

            binding.viewer.layoutParams.apply {
                width = screenWidth
                height = screenWidth
            }

            val remainingHeight = activeAreaHeight - screenWidth
            val splitHeight = remainingHeight / 2

            binding.readout.layoutParams.height = splitHeight
            binding.controls.layoutParams.height = splitHeight

            binding.readout.requestLayout()
            binding.viewer.requestLayout()
            binding.controls.requestLayout()
        }
    }

    private fun readout(message: String) {
        requireActivity().runOnUiThread {
            homeViewModel.setStatus(message)
        }
    }

    private fun pollForPhotoUrls() {
        while (true) {
            try {
                val payload = """
                {
                    "method": "getEvent",
                    "params": [false],
                    "id": 1,
                    "version": "1.0"
                }
                """.trimIndent()

                val connection = URL(CAMERA_ENDPOINT).openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                }

                connection.outputStream.use { os: OutputStream ->
                    os.write(payload.toByteArray(Charsets.UTF_8))
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                    val jsonResponse = JSONObject(response)
                    val takePictureUrls = jsonResponse.optJSONArray("result")?.optJSONArray(5)
                        ?.optJSONObject(0)?.optJSONArray("takePictureUrl")

                    val newUrl = takePictureUrls?.let { urls ->
                        (0 until urls.length()).asSequence()
                            .map { urls.optString(it) }
                            .firstOrNull { it.isNotEmpty() }
                    }

                    if (newUrl != null && newUrl != photoUrl) {
                        photoUrl = newUrl
                        readout("$photoUrl")
                        saveImageToGallery(photoUrl!!)
                    }
                } else {
                    readout("Error: ${connection.responseCode}")
                }
            } catch (e: Exception) {
                readout("Error polling for photo: ${e.message}")
            }
            Thread.sleep(1000)
        }
    }

    private fun saveImageToGallery(imageUrl: String) {
        try {
            val inputStream: InputStream = URL(imageUrl).openStream()
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "AIris_${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/AIrisUnprocessed")
            }

            val resolver = requireContext().contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                readout("Image saved to gallery")
            }
        } catch (e: Exception) {
            readout("Error saving image: ${e.message}")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
