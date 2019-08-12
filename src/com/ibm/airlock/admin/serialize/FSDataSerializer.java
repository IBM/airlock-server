package com.ibm.airlock.admin.serialize;

import org.apache.commons.io.FileUtils;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;

public class FSDataSerializer extends BaseDataSerializer implements DataSerializer {

    private static final String FS_PATH_PREFIX = "fsPathPrefix";

    private String pathPrefix;
    private S3DataSerializer publicDataSerializer;

    public FSDataSerializer(JSONObject storageParamsObj) throws JSONException {
        super(storageParamsObj);
        this.pathPrefix = (String)storageParamsObj.get(FS_PATH_PREFIX);
        this.publicDataSerializer = new S3DataSerializer(storageParamsObj);
    }

    @Override
    public String getSeparator() {
        return File.separator;
    }

    public void copyFolder(String fromPath, String toPath) throws IOException {
        copyFolder(fromPath, toPath, false);
    }

    @Override
    public void copyFolder(String fromPath, String toPath, boolean publish) throws IOException {
        FileUtils.copyDirectory(new File(pathPrefix + File.separator + fromPath),
                new File(pathPrefix + File.separator + toPath));

        if (publish) {
            copyPublishedFolder(fromPath, toPath);
        }

    }

    private void copyPublishedFolder(String fromPath, String toPath) throws IOException {
        publicDataSerializer.copyFolder(fromPath, toPath, false);
    }

    @Override
    public void fullPathCopyFolder(String fullFromPath, String relativeToPath) throws IOException {
        FileUtils.copyDirectory(new File(fullFromPath),
                new File(pathPrefix + File.separator + relativeToPath));
    }

    public void deleteData(String path) throws IOException {
        deleteData(path, false);
    }

    @Override
    public void deleteData(String path, boolean published) throws IOException {
        FileUtils.forceDelete(new File(pathPrefix + File.separator + path));
        if (published) {
            deletePublicData(path);
        }
    }

    private void deletePublicData(String path) throws IOException {
        publicDataSerializer.deleteData(path);
    }

    public void deleteFolderContent(String path) throws IOException {
        deleteFolderContent(path, false);
    }

    @Override
    public void deleteFolderContent(String path, boolean published) throws IOException {
        FileUtils.deleteDirectory(new File(pathPrefix + File.separator + path));
        if (published) {
            deletePublishedFolderContent(path);
        }
    }

    private void deletePublishedFolderContent(String path) throws IOException {
        publicDataSerializer.deleteFolderContent(path, false);
    }

    @Override
    public boolean isFileExists(String path) {
        return new File(pathPrefix + File.separator + path).exists();
    }

    @Override
    public boolean fullPathIsFileExists(String fullPath) {
        return new File(fullPath).exists();
    }

    @Override
    public Collection<String> listFileNames(String path) throws IOException {
        Collection<File> listFiles = FileUtils.listFiles(new File(pathPrefix + File.separator + path), null, true);
        Collection<String> listNames = new ArrayList<String>(listFiles.size());
        for (File file : listFiles) {
            listNames.add(file.getName());
        }

        return listNames;
    }

    @Override
    public JSONObject readDataToJSON(String path) throws IOException, JSONException {

        InputStream is = FileUtils.openInputStream(new File(pathPrefix + File.separator + path));
        JSONObject dataObj = new JSONObject(is);
        is.close();
        return dataObj;
    }

    @Override
    public JSONObject fullPathReadDataToJSON(String fullPath) throws IOException, JSONException {
        InputStream is = FileUtils.openInputStream(new File(fullPath));
        JSONObject dataObj = new JSONObject(is);
        is.close();
        return dataObj;
    }

    @Override
    public String readDataToString(String path) throws IOException {
        return FileUtils.readFileToString(new File(pathPrefix + File.separator + path), Charset.forName("UTF-8"));
    }
    
    @Override
    public byte[] readDataToByteArray(String path) throws IOException {
        return FileUtils.readFileToByteArray(new File(pathPrefix + File.separator + path));
    }

    private void publishData(String path, String data) throws IOException {
        publicDataSerializer.writeData(path, data, false);
    }

    @Override
    public void writeData(String path, String data) throws IOException {
        writeData(path, data, false);
    }

    public void writeData(String path, String data, boolean publish) throws IOException {
        FileUtils.write(new File(pathPrefix + File.separator + path), data, Charset.forName("UTF-8"));
        if (publish) {
            publishData(path, data);
        }
    }

    @Override
    public String getPathPrefix() {
        return pathPrefix;
    }

    public String getPublicPathPrefix() {
        return publicDataSerializer.getPublicPathPrefix();
    }

	@Override
	public void writeInputStreamData(String path, InputStream data, int length) throws IOException {
		FileUtils.copyInputStreamToFile(data, new File(pathPrefix + File.separator + path));
	}

	@Override
	public void writeDataToRuntimeFolder(String path, String data) throws IOException {
		//for now not implemented since needed only in s3 serializer
	}

}
