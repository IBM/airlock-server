package com.ibm.airlock.admin.analytics;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletContext;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Constants.OutputJSONMode;
import com.ibm.airlock.admin.BaseAirlockItem;
import com.ibm.airlock.admin.Season;
import com.ibm.airlock.engine.Environment;

public class AirlockAnalytics {
	
	
	private GlobalDataCollection globalDataCollection; //required in update
	private UUID seasonId = null; //required in create and update	
	private int analyticsQuota = Constants.DEFAULT_ANALYTICS_QUOTA;
	
	public AirlockAnalytics(UUID seasonId) {
		this.seasonId = seasonId;
		globalDataCollection = new GlobalDataCollection(seasonId, this);		
	}
	
	public GlobalDataCollection getGlobalDataCollection() {
		return globalDataCollection;
	}
	
	public void setGlobalDataCollection(GlobalDataCollection globalDataCollection) {
		this.globalDataCollection = globalDataCollection;
	}
		
	public UUID getSeasonId() {
		return seasonId;
	}

	public void setSeasonId(UUID seasonId) {
		this.seasonId = seasonId;
	}

	public int getAnalyticsQuota() {
		return analyticsQuota;
	}

	public void setAnalyticsQuota(int analyticsQuota) {
		this.analyticsQuota = analyticsQuota;
	}

	public void fromJSON(UUID seasonId, JSONObject input, ServletContext context, Environment env, Map<String, BaseAirlockItem> airlockItemsDB ) throws JSONException {		
		if (input.containsKey(Constants.JSON_FEATURE_FIELD_SEASON_ID) && input.get(Constants.JSON_FEATURE_FIELD_SEASON_ID) != null) {
			String sStr = input.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID);			
			seasonId = UUID.fromString(sStr);			
		}
	
		
		if (input.containsKey(Constants.JSON_FIELD_GLOBAL_DATA_COLLECTION) && input.get(Constants.JSON_FIELD_GLOBAL_DATA_COLLECTION)!=null) {
			globalDataCollection = new GlobalDataCollection(seasonId, this);
			globalDataCollection.fromJSON(input.getJSONObject(Constants.JSON_FIELD_GLOBAL_DATA_COLLECTION), context, env, airlockItemsDB);
		}				
		
		if (input.containsKey(Constants.JSON_FIELD_ANALYTICS_QUOTA) && input.get(Constants.JSON_FIELD_ANALYTICS_QUOTA) != null) {
			analyticsQuota = input.getInt(Constants.JSON_FIELD_ANALYTICS_QUOTA);								
		}
	}
		 
	public JSONObject toJson(OutputJSONMode mode, ServletContext context, Season season, Map<String, BaseAirlockItem> airlockItemsDB, Environment env) throws JSONException {
		JSONObject res = new JSONObject();
		
		res.put(Constants.JSON_FIELD_GLOBAL_DATA_COLLECTION, globalDataCollection.toJson(false, context, season, false, airlockItemsDB, env));
		res.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, seasonId==null?null:seasonId.toString());
		res.put(Constants.JSON_FIELD_ANALYTICS_QUOTA, analyticsQuota);
				
		return res;
	}
		
	public AirlockAnalytics duplicateForNewSeason (UUID newSeasonId, HashMap<String, String> oldToDuplicatedFeaturesId, ServletContext context, UUID oldSeasonId) throws JSONException {
		AirlockAnalytics res = new AirlockAnalytics(newSeasonId);
		
		res.setSeasonId(newSeasonId);
		res.setAnalyticsQuota(analyticsQuota);

		res.setGlobalDataCollection(globalDataCollection.duplicateForNewSeason(newSeasonId, oldToDuplicatedFeaturesId, context, oldSeasonId, res));
		
		return res;
	}

}
