package com.aero.control.helpers.FileManager;

import java.io.File;

/**
 * Listener interface for file manager events.
 */
public interface FileManagerListener {
    /**
     * Called when a file cannot be read.
     *
     * @param file the file that cannot be read
     */
    void OnCannotFileRead(File file);

    /**
     * Called when a file is clicked.
     *
     * @param file the clicked file
     */
    void OnFileClicked(File file);
}
