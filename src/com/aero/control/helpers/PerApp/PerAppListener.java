package com.aero.control.helpers.PerApp;

/**
 * Listener interface for per-app list item click events.
 */
public interface PerAppListener {
    /**
     * Called when an app item is clicked in the per-app configuration list.
     *
     * @param i the position of the clicked item
     * @param z the new checked state
     */
    void OnAppItemClicked(int i, boolean z);
}
