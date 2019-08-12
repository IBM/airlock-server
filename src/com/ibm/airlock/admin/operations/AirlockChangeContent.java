package com.ibm.airlock.admin.operations;

import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants.Stage;

public class AirlockChangeContent {
	private JSONObject json;
	private String data;
	private boolean isAdmin;
	private boolean isRuntime;
	private Stage minStage;
	private String fileName;
	
	public AirlockChangeContent(JSONObject json, String fileName, boolean isAdmin, boolean isRuntime, Stage minStage) {
		this.setJson(json);
		this.setAdmin(isAdmin);
		this.setRuntime(isRuntime);
		this.setFileName(fileName);
		this.setMinStage(minStage);
	}

	public static AirlockChangeContent getAdminChange(JSONObject json, String fileName, Stage minStage) {
		return new AirlockChangeContent(json,fileName, true, false, minStage);
	}
	
	public static AirlockChangeContent getAdminChange(String data, String fileName, Stage minStage) {
		AirlockChangeContent changeContent =  new AirlockChangeContent(null,fileName, true, false, minStage);
		changeContent.setData(data);
		return changeContent;
		
	}
	
	public static AirlockChangeContent getRuntimeChange(JSONObject json, String fileName, Stage minStage) {
		return new AirlockChangeContent(json,fileName, false, true, minStage);
	}
	
	public JSONObject getJson() {
		return json;
	}

	public void setJson(JSONObject json) {
		this.json = json;
	}

	public boolean isAdmin() {
		return isAdmin;
	}

	public void setAdmin(boolean isAdmin) {
		this.isAdmin = isAdmin;
	}

	public boolean isRuntime() {
		return isRuntime;
	}

	public void setRuntime(boolean isRuntime) {
		this.isRuntime = isRuntime;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public Stage getMinStage() {
		return minStage;
	}

	public void setMinStage(Stage minStage) {
		this.minStage = minStage;
	}
}