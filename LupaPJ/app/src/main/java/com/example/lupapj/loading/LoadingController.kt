package com.example.lupapj.loading

/**
 * Interface that allows swapping out simulated loading logic
 * with real asset/network loading logic later.
 */
interface LoadingController {
    /**
     * Called when the progress updates.
     * @param progress A value between 0 and 100.
     */
    fun onProgressUpdate(progress: Int)

    /**
     * Called when loading has reached 100% and is completely finished.
     */
    fun onLoadingComplete()
}
