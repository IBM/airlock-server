package com.ibm.airlock.admin.serialize;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Set;

public interface DataSerializer {

    public static final String STORAGE_TYPE_S3 = "S3";
    public static final String STORAGE_TYPE_FS = "FS";
    public static final String STORAGE_TYPE_AZURE = "AZURE";
    public static final String STORAGE_TYPE_BLUEMIX = "BLUEMIX";
    public static final String STORAGE_TYPE = "storageType";

    public String getSeparator();
    public void copyFolder (String fromPath, String toPath) throws IOException;
    public void copyFolder (String fromPath, String toPath, boolean publish) throws IOException;
    public void fullPathCopyFolder (String fullFromPath, String relativeToPath) throws IOException;
    public void deleteData(String path) throws IOException;
    public void deleteData(String path, boolean published) throws IOException;
    public void deleteFolderContent(String path) throws IOException;
    public void deleteFolderContent(String path, boolean published) throws IOException;
    public boolean isFileExists(String path);
    public boolean fullPathIsFileExists(String fullPath);
    public Collection<String> listFileNames(String path) throws IOException;
    public JSONObject readDataToJSON(String path)  throws IOException, JSONException;
    public JSONObject fullPathReadDataToJSON(String fullPath)  throws IOException, JSONException;
    public String readDataToString(String path)  throws IOException;
    public byte[] readDataToByteArray(String path)  throws IOException;
    public void writeData(String path, String data) throws IOException;
    public void writeData(String path, String data, boolean publish) throws IOException;
    public void writeInputStreamData(String path, InputStream data, int length) throws IOException;
    public String getPathPrefix();
    public String getPublicPathPrefix();
    public String getStoragePublicPath();
    public String getRuntimePublicFullPath();
    public void setRuntimeFileNames(Set<String> runtimeFileNames);
    public void writeDataToRuntimeFolder(String path, String data) throws IOException; //for seasons that do not support internal/runtime folders separation - some internal files must be written to the runtime folder. for example "AirlockBranches.json"
}
