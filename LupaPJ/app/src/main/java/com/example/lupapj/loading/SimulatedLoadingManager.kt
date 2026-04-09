package com.example.lupapj.loading

import kotlinx.coroutines.delay

class SimulatedLoadingManager(private val controller: LoadingController) {

    suspend fun startLoading() {
        var progress = 0
        while (progress < 100) {
            // Adds slight easing/variance
            val increment = (3..8).random()
            progress += increment
            if (progress > 100) {
                progress = 100
            }
            controller.onProgressUpdate(progress)
            
            val delayMs = if (progress > 80) 150L else 80L
            delay(delayMs)
        }
        
        // Short pause at 100% before triggering completion
        delay(500)
        controller.onLoadingComplete()
    }
}
