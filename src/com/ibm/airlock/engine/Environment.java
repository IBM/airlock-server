package com.ibm.airlock.engine;

import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Constants.ServiceState;
import com.ibm.airlock.admin.BaseAirlockItem;
import com.ibm.airlock.admin.ValidationCache;
import com.ibm.airlock.admin.analytics.AirlockAnalytics;

public class Environment extends Properties
{
	private static final long serialVersionUID = 1L;
	public static final String SERVER_VERSION = "serverVersion";
	public static final String PRINT_GOLD = "printGoldFile";
	public static final String DEBUG_MODE = "debugMode";

	Version version = Version.v1;
	String branchId = Constants.MASTER_BRANCH_NAME;
	AirlockAnalytics analytics = null; //keeps the current analytics. If in master - the season's analytics, if in branch - the merged analytics
	Map<String, BaseAirlockItem> airlockItemsDB = null; //keeps the current items map. If in master - the global items map, if in branch - the merged items map
	ArrayList<String> trace = null;
	ValidationCache validationCache =null; //used for orderingRule evaluation during "toJson" call
	private ServiceState serviceState = null;
	private Constants.REQUEST_ITEM_TYPE requestType = Constants.REQUEST_ITEM_TYPE.FEATURES;
	
	public void setServerVersion(String str)
	{
		this.put(SERVER_VERSION, str);
		this.version = Version.find(str);
	}

		public Version getVersion()
	{
		return version;
	}

	public String getBranchId() {
		return branchId;
	}

	public void setBranchId(String branchId) {
		this.branchId = branchId;
	}
	
	public boolean isInMaster() {
		return branchId.equals(Constants.MASTER_BRANCH_NAME);
	}
	
	//Note that if not set - the value is null!
	public AirlockAnalytics getAnalytics() {
		return analytics;
	}

	public void setAnalytics(AirlockAnalytics analytics) {
		this.analytics = analytics;
	}
	
	//Note that if not set - the value is null!
	public Map<String, BaseAirlockItem> getAirlockItemsDB() {
		return airlockItemsDB;
	}

	public void setAirlockItemsDB(Map<String, BaseAirlockItem> airlockItemsDB) {
		this.airlockItemsDB = airlockItemsDB;
	}
	

	public ValidationCache getValidationCache() {
		return validationCache;
	}

	public void setValidationCache(ValidationCache validationCache) {
		this.validationCache = validationCache;
	}

	public void startTrace() { trace = new ArrayList<String>(); }
	public ArrayList<String> endTrace() { ArrayList<String> out = trace; trace = null; return out; }
	public boolean isTraced() { return trace != null; }
	public void addTrace(String msg) { if (trace != null) trace.add(msg); }

	public ServiceState getServiceState() {
		return serviceState;
	}

	public void setServiceState(ServiceState serviceState) {
		this.serviceState = serviceState;
	}

	public Constants.REQUEST_ITEM_TYPE getRequestType() {
		return requestType;
	}

	public void setRequestType(Constants.REQUEST_ITEM_TYPE requestType) {
		this.requestType = requestType;
	}
}
