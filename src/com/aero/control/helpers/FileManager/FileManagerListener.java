package com.aero.control.helpers.FileManager;

import java.io.File;

public interface FileManagerListener {
    void OnCannotFileRead(File file);

    void OnFileClicked(File file);
}
