package com.ibm.airlock.admin.serialize;

import java.util.HashSet;
import java.util.Set;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

public abstract class BaseDataSerializer {

    public static final String STORAGE_PUBLIC_PATH = "storagePublicPath";
    public static final String RUNTIME_PUBLIC_FULL_PATH = "runtimePublicFullPath";

    private Set<String> runtimeFileNames = new HashSet<String>();
    
    protected String storagePublicPath;
    protected String runtimePublicFullPath;

    public BaseDataSerializer() {
    		
    }

    public void setRuntimeFileNames(Set<String> runtimeFileNames) {
    		this.runtimeFileNames = runtimeFileNames;
    }
    
    protected boolean isRuntimeFile(String path) {
    		for (String runtimeFileName:runtimeFileNames) {
    			if (path.contains(runtimeFileName)) {
    				return true;
    			}
    		}
    		return false;
    }
    
    public BaseDataSerializer(JSONObject storageParamsObj) throws JSONException {
        this.storagePublicPath = (String)storageParamsObj.get(STORAGE_PUBLIC_PATH);
        this.runtimePublicFullPath = (String)storageParamsObj.get(RUNTIME_PUBLIC_FULL_PATH);
    }

    public String getStoragePublicPath() {
        return storagePublicPath;
    }

    public String getRuntimePublicFullPath() {
        return runtimePublicFullPath;
    }
}
