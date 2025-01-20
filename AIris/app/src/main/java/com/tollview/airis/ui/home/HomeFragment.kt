package com.tollview.airis.ui.home

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.tollview.airis.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var homeViewModel: HomeViewModel

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

        // Observe LiveData and update the readout text when it changes
        homeViewModel.statusMessage.observe(viewLifecycleOwner) { message ->
            binding.readout.alpha = 0f  // Set initial transparency
            binding.readout.text = message
            binding.readout.animate().alpha(1f).setDuration(500).start() // Apply fade-in effect
        }

        // Ensure layout calculations happen after the view is laid out
        binding.root.post {
            val insets = ViewCompat.getRootWindowInsets(binding.root)
                ?.getInsets(WindowInsetsCompat.Type.systemBars()) ?: return@post

            // Calculate active area height (excluding system bars)
            val activeAreaHeight = binding.root.height - (insets.top + insets.bottom)

            // Get the screen width for a 1:1 viewer size
            val screenWidth = binding.root.width

            // Set viewer dimensions to be a square
            binding.viewer.layoutParams.apply {
                width = screenWidth
                height = screenWidth
            }

            // Calculate and divide remaining space for readout and controls
            val remainingHeight = activeAreaHeight - screenWidth
            val splitHeight = remainingHeight / 2

            binding.readout.layoutParams.height = splitHeight
            binding.controls.layoutParams.height = splitHeight

            // Force layout updates
            binding.readout.requestLayout()
            binding.viewer.requestLayout()
            binding.controls.requestLayout()

            // Log for debugging
            android.util.Log.d("HomeFragment", "Readout height: $splitHeight, Viewer size: $screenWidth")

            homeViewModel.setStatus("Ready to capture!")

            // Update message after 5 seconds
            Handler(Looper.getMainLooper()).postDelayed({
                homeViewModel.setStatus("Five seconds have passed.")
            }, 5000)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
