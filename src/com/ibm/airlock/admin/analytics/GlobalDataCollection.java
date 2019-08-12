package com.ibm.airlock.admin.analytics;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response.Status;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Constants.Action;
import com.ibm.airlock.Constants.OutputJSONMode;
import com.ibm.airlock.Constants.Stage;
import com.ibm.airlock.Strings;
import com.ibm.airlock.admin.authentication.UserInfo;
import com.ibm.airlock.admin.BaseAirlockItem;
import com.ibm.airlock.admin.FeatureItem;
import com.ibm.airlock.admin.MergeBranch.MergeException;
import com.ibm.airlock.admin.Rule;
import com.ibm.airlock.admin.Season;
import com.ibm.airlock.admin.ValidationResults;
import com.ibm.airlock.engine.Environment;

public class GlobalDataCollection extends BaseAnalyticsItem {
	public static final Logger logger = Logger.getLogger(GlobalDataCollection.class.getName());
		
	private AnalyticsDataCollection analyticsDataCollection; //required in update
	private AirlockAnalytics airlockAnalytics = null;
	private UUID seasonId = null; //required in create and update
	
	public GlobalDataCollection(UUID seasonId, AirlockAnalytics airlockAnalytics) {
		super ();
		this.airlockAnalytics = airlockAnalytics;
		analyticsDataCollection = new AnalyticsDataCollection(this.airlockAnalytics);		
		this.seasonId = seasonId;	
	}

	public AnalyticsDataCollection getAnalyticsDataCollection() {
		return analyticsDataCollection;
	}
	
	public void setAnalyticsDataCollection(AnalyticsDataCollection globalDataCollection) {
		this.analyticsDataCollection = globalDataCollection;
	}
	
	public UUID getSeasonId() {
		return seasonId;
	}

	public void setSeasonId(UUID seasonId) {
		this.seasonId = seasonId;
	}
	
	public void fromJSON(JSONObject input, ServletContext context, Environment env, Map<String, BaseAirlockItem> airlockItemsDB ) throws JSONException {
		super.fromJSON(input);
			
		if (input.containsKey(Constants.JSON_FEATURE_FIELD_SEASON_ID) && input.get(Constants.JSON_FEATURE_FIELD_SEASON_ID) != null) {
			String sStr = input.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID);			
			seasonId = UUID.fromString(sStr);			
		}
		
		if (input.containsKey(Constants.JSON_FIELD_ANALYTICS_DATA_COLLECTION) && input.get(Constants.JSON_FIELD_ANALYTICS_DATA_COLLECTION)!=null) {
			analyticsDataCollection = new AnalyticsDataCollection(airlockAnalytics);
			analyticsDataCollection.fromJSON(input.getJSONObject(Constants.JSON_FIELD_ANALYTICS_DATA_COLLECTION), context, seasonId, env, airlockItemsDB);
		}		
	} 
	
	
	public JSONObject toJson(boolean verbose, ServletContext context, Season season, Boolean includeWarnings, Map<String, BaseAirlockItem> airlockItemsDB, Environment env) throws JSONException {
		JSONObject res = super.toJson(OutputJSONMode.ADMIN);
		if (res == null) {
			// this can only happen in runtime_production mode when the feature is in development stage
			return null;
		}
		
		res.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, seasonId==null?null:seasonId.toString());		
		res.put(Constants.JSON_FIELD_ANALYTICS_DATA_COLLECTION, analyticsDataCollection.toJson(verbose, context, season, includeWarnings, airlockItemsDB, env));
		
		return res;
	}
	
	public ValidationResults validateAnalyticsItemJSON(JSONObject globalDataCollectionObj, ServletContext context, UserInfo userInfo, Environment env, Map<String, BaseAirlockItem> airlockItemsDB, boolean productionChangeAllowed ) throws MergeException {
		@SuppressWarnings("unchecked")
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

		try {
			//will verify super + subfeatures/configurations
			ValidationResults superRes = super.validateBaseAnalyticsItemJSON(globalDataCollectionObj, context, userInfo, Action.UPDATE); //always update since created during season craetion

			if (superRes!=null)
				return superRes;			


			//analyticsDataCollection
			if (!globalDataCollectionObj.containsKey(Constants.JSON_FIELD_ANALYTICS_DATA_COLLECTION) || globalDataCollectionObj.get(Constants.JSON_FIELD_ANALYTICS_DATA_COLLECTION)==null) {
				return new ValidationResults("The analyticsDataCollection field is missing. This field must be specified during global data collection update.", Status.BAD_REQUEST);
			}				
			
			//seasonId
			if (!globalDataCollectionObj.containsKey(Constants.JSON_FEATURE_FIELD_SEASON_ID) || globalDataCollectionObj.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID) == null || globalDataCollectionObj.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_SEASON_ID), Status.BAD_REQUEST);
			}
	
			String sId = (String)globalDataCollectionObj.get(Constants.JSON_FEATURE_FIELD_SEASON_ID);
			Season season = seasonsDB.get(sId);
			if (season == null) {
				return new ValidationResults("The version range of the given analytics item does not exist.", Status.BAD_REQUEST);
			}
			
			JSONObject anaDataColl = globalDataCollectionObj.getJSONObject(Constants.JSON_FIELD_ANALYTICS_DATA_COLLECTION); //validate legal json
			ValidationResults res = this.analyticsDataCollection.validateDataColJSON(anaDataColl, context, userInfo, season, env, airlockItemsDB,  productionChangeAllowed);
			if (res!=null)
				return res;

		} catch (JSONException jsne) {
			return new ValidationResults(jsne.getMessage(), Status.BAD_REQUEST);
		}
		return null;
	}
	
	public String updateGlobalDataCollection(JSONObject updatedGlobalDataCollectionJSON, ServletContext context, Environment env, Map<String, BaseAirlockItem> airlockItemsDB) throws JSONException {
		
		String dataALItemUpdateDetails = super.updateAnalyticsItem(updatedGlobalDataCollectionJSON, context);

		boolean wasChanged = ((dataALItemUpdateDetails != null) &&  !dataALItemUpdateDetails.isEmpty());
		StringBuilder updateDetails = new StringBuilder(dataALItemUpdateDetails);

		//analyticsDataCollection
		JSONObject updatedAnalyticsDataCollection = updatedGlobalDataCollectionJSON.getJSONObject(Constants.JSON_FIELD_ANALYTICS_DATA_COLLECTION);
		String updateStr = analyticsDataCollection.updateDataCollection(updatedAnalyticsDataCollection, context, seasonId, env, airlockItemsDB);
		if (!updateStr.isEmpty()) {
			wasChanged = true;
			updateDetails.append("'analyticsDataCollection' changed: \n" + updateStr);
		}
		
		if (wasChanged) {
			lastModified = new Date();
		}
		
		return updateDetails.toString();
	}
	
	//called after ValidateFeatureOnOffAddition - i know it is legal

	public void addFeatureOnOff(BaseAirlockItem feature, Map<String, BaseAirlockItem> airlockItemsDB) {

		analyticsDataCollection.addFeatureOnOff(feature, airlockItemsDB);
		
		lastModified = new Date();	
	}
	
	//called after ValidateFeatureOnOffAddition - i know it is legal
	public void addInputField(String inputField, Stage stage) {
		analyticsDataCollection.addInputField(inputField, stage);
		
		lastModified = new Date();	
	}
	
	public ValidationResults ValidateFeatureOnOffAddition(BaseAirlockItem feature, Environment env, Season season, ServletContext context) throws MergeException {
		ValidationResults vr = analyticsDataCollection.ValidateFeatureOnOffAddition(feature, env, season, context);
		if (vr!=null)
			return vr;
				
		return null;
	}
	

	public void removeFeatureOnOff(BaseAirlockItem feature, Map<String, BaseAirlockItem> airlockItemsDB) {

		analyticsDataCollection.removeFeatureOnOff(feature, airlockItemsDB);		
		lastModified = new Date();		
	}
	
	public ValidationResults validateFeatureOnOffRemoval(BaseAirlockItem feature, Environment env, Season season) {
		ValidationResults vr = analyticsDataCollection.validateFeatureOnOffRemoval(feature, env, season);
		if (vr!=null)
			return vr;
				
		return null;
	}

	public GlobalDataCollection duplicateForNewSeason(UUID newSeasonId, HashMap<String, String> oldToDuplicatedFeaturesId, ServletContext context, UUID oldSeasonId, AirlockAnalytics newAirlockAnalytics) throws JSONException {
		GlobalDataCollection res = new GlobalDataCollection(newSeasonId, newAirlockAnalytics);
		res.setRolloutPercentage(rolloutPercentage);
		res.setRule(rule == null?null:Rule.duplicteForNextSeason(rule));
		res.setLastModified(lastModified);
		res.setSeasonId(newSeasonId);
		
		res.setAnalyticsDataCollection(analyticsDataCollection.duplicateForNewSeason(oldToDuplicatedFeaturesId, context, oldSeasonId, newAirlockAnalytics));

		return res;
	}
	
	public ValidationResults validateProductionDontChanged(JSONObject updatedGlobalDataCollectionJSON, Map<String, BaseAirlockItem> airlockItemsDB, ServletContext context) throws JSONException {
		JSONObject updatedAnalyticsDataCollection = updatedGlobalDataCollectionJSON.getJSONObject(Constants.JSON_FIELD_ANALYTICS_DATA_COLLECTION);
		return analyticsDataCollection.validateProductionDontChanged(updatedAnalyticsDataCollection, airlockItemsDB, seasonId, context);		
	}

	public JSONObject toDisplayJson(ServletContext context, Season season, Environment env, Map<String, BaseAirlockItem> airlockItemsDB) throws JSONException {
		JSONObject res = super.toJson(OutputJSONMode.DISPLAY);
		if (res == null) {
			// this can only happen in runtime_production mode when the feature is in development stage
			return null;
		}
		
		res.put(Constants.JSON_FIELD_ANALYTICS_DATA_COLLECTION, analyticsDataCollection.toDisplayJson(context, season, env, airlockItemsDB));
		
		return res;
	}

	public void updateInputFields(JSONArray inputFieldsJSONArray, ServletContext context, Season season, boolean productionChangeAllowed, Environment env) throws JSONException {
		analyticsDataCollection.updateInputFields(inputFieldsJSONArray, context, season, productionChangeAllowed, env);
		lastModified = new Date();		
	}	
	
	public ValidationResults validateInputFieldsUpdate(JSONArray inputFieldsJSONArray, ServletContext context, Season season, boolean productionChangeAllowed, Environment env, boolean skipCountersChecking) throws MergeException {
		ValidationResults vr = analyticsDataCollection.validateInputFieldsUpdate(inputFieldsJSONArray, context, season, productionChangeAllowed, env, skipCountersChecking);
		if (vr!=null)
			return vr;
		
		return null;
	}	
	
	public void updateFeatureAttributs(FeatureItem feature, JSONArray attributesJSONArray, ServletContext context, Season season, Map<String, BaseAirlockItem> airlockItemsDB, Map<String, TreeSet<String>> featureAttributesMap, Environment env) throws JSONException {
		analyticsDataCollection.updateFeatureAttributs(feature, attributesJSONArray, context, season, airlockItemsDB, featureAttributesMap, env);		
		lastModified = new Date();		
	}	
	
	public ValidationResults validateFeatureAttributsUpdate(FeatureItem feature, JSONArray attributesJSONArray, ServletContext context, Season season, Map<String, BaseAirlockItem> airlockItemsDB, Map<String, TreeSet<String>> featureAttributesMap, Environment env, boolean skipCountersChecking) throws MergeException {
		ValidationResults vr = analyticsDataCollection.validateFeatureAttributsUpdate(feature, attributesJSONArray, context, season, airlockItemsDB, featureAttributesMap, env, skipCountersChecking);
		if (vr!=null)
			return vr;		
		
		return null;
	}	
		
}
