package com.aero.control.helpers.FileManager;

import android.app.Dialog;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import com.aero.control.AeroActivity;
import com.aero.control.R;
import com.aero.control.adapter.AeroData;
import com.aero.control.adapter.FileAdapter;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class FileManager extends RelativeLayout implements AdapterView.OnItemClickListener {
    private static final String mRoot = "/";
    private FileAdapter mAdapter;
    Context mContext;
    private String mCurrentPath;
    private List<AeroData> mData;
    private Dialog mDialog;
    private List<FileData> mFileData;
    FileManagerListener mFolderListener;
    private ListView mListView;

    private class FileData {
        private String item;
        private String path;

        public FileData(String item, String path) {
            this.item = item;
            this.path = path;
        }
    }

    public FileManager(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mData = new ArrayList();
        this.mFileData = null;
        this.mContext = context;
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        View view = layoutInflater.inflate(R.layout.file_folder, this);
        this.mListView = (ListView) view.findViewById(R.id.list);
        getDir(mRoot, this.mListView, false);
    }

    public void setIFolderItemListener(FileManagerListener folderItemListener) {
        this.mFolderListener = folderItemListener;
    }

    public void setDir(String dirPath) {
        getDir(dirPath, this.mListView, false);
    }

    public void setDialog(Dialog d) {
        this.mDialog = d;
    }

    private void setTitle(String dirPath) {
        this.mCurrentPath = dirPath;
        if (this.mDialog != null) {
            this.mDialog.setTitle(this.mCurrentPath);
        }
    }

    private void getDir(String dirPath, ListView v, boolean root) {
        File[] files;
        setTitle(dirPath);
        List<FileData> MetaDirectory = new ArrayList<>();
        List<FileData> MetaFiles = new ArrayList<>();
        File f = new File(dirPath);
        if (!root) {
            files = f.listFiles();
        } else {
            ArrayList<File> rootFiles = new ArrayList<>();
            String[] rootList = AeroActivity.shell.getRootArray("ls -d " + dirPath + "/*", "\n");
            if (rootList != null) {
                for (String a : rootList) {
                    rootFiles.add(new File(a));
                }
            } else {
                rootFiles.add(new File(this.mContext.getText(R.string.folder_empty).toString()));
            }
            files = (File[]) rootFiles.toArray(new File[0]);
        }
        if (!dirPath.equals(mRoot)) {
            MetaDirectory.add(new FileData("../", f.getParent()));
        }
        for (File file : files) {
            if (file.isDirectory()) {
                MetaDirectory.add(new FileData(file.getName() + mRoot, file.getPath()));
            } else {
                MetaFiles.add(new FileData(file.getName(), file.getPath()));
            }
        }
        this.mFileData = new ArrayList();
        Collections.sort(MetaDirectory, new Comparator<FileData>() { // from class: com.aero.control.helpers.FileManager.FileManager.1
            @Override // java.util.Comparator
            public int compare(FileData fileData, FileData fileData2) {
                return fileData.item.compareTo(fileData2.item);
            }
        });
        Collections.sort(MetaFiles, new Comparator<FileData>() { // from class: com.aero.control.helpers.FileManager.FileManager.2
            @Override // java.util.Comparator
            public int compare(FileData fileData, FileData fileData2) {
                return fileData.item.compareTo(fileData2.item);
            }
        });
        this.mFileData.addAll(MetaDirectory);
        this.mFileData.addAll(MetaFiles);
        setItemList();
    }

    public void setItemList() {
        if (this.mData != null) {
            this.mData.clear();
        }
        for (FileData a : this.mFileData) {
            File checkFile = new File(this.mCurrentPath + mRoot + a.item);
            if (checkFile.isDirectory()) {
                this.mData.add(new AeroData(R.drawable.file_folder, a.item));
            } else {
                this.mData.add(new AeroData(R.drawable.file_document, a.item));
            }
        }
        if (this.mAdapter == null) {
            this.mAdapter = new FileAdapter(this.mContext, R.layout.file_row, this.mData);
            this.mListView.setAdapter((ListAdapter) this.mAdapter);
            this.mListView.setOnItemClickListener(this);
            return;
        }
        this.mAdapter.notifyDataSetChanged();
    }

    public void onListItemClick(ListView l, View v, int position, long id) {
        File file = new File(this.mFileData.get(position).path);
        if (file.isDirectory()) {
            if (file.canRead()) {
                getDir(file.toString(), l, false);
                return;
            }
            if (this.mFolderListener != null) {
                this.mFolderListener.OnCannotFileRead(file);
            }
            getDir(file.toString(), l, true);
            return;
        }
        if (this.mFolderListener != null) {
            this.mFolderListener.OnFileClicked(file);
        }
    }

    @Override // android.widget.AdapterView.OnItemClickListener
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        onListItemClick((ListView) arg0, arg0, arg2, arg3);
    }
}
