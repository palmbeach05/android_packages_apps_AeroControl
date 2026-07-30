package com.aero.control.helpers.FileManager;

import java.io.File;

/* JADX INFO: loaded from: classes.dex */
public interface FileManagerListener {
    void OnCannotFileRead(File file);

    void OnFileClicked(File file);
}
