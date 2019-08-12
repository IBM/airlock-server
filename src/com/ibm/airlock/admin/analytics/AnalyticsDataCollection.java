package com.ibm.airlock.admin.analytics;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response.Status;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Constants.AttributeType;
import com.ibm.airlock.Constants.UtilityType;
import com.ibm.airlock.Strings;
import com.ibm.airlock.Constants.BranchStatus;
import com.ibm.airlock.Constants.InputSampleGenerationMode;
import com.ibm.airlock.Constants.OutputJSONMode;
import com.ibm.airlock.Constants.Stage;
import com.ibm.airlock.admin.authentication.UserInfo;
import com.ibm.airlock.admin.BaseAirlockItem;
import com.ibm.airlock.admin.Branch;
import com.ibm.airlock.admin.BaseAirlockItem.Type;
import com.ibm.airlock.admin.ConfigurationRuleItem;
import com.ibm.airlock.admin.DataAirlockItem;
import com.ibm.airlock.admin.FeatureItem;
import com.ibm.airlock.admin.GenerationException;
import com.ibm.airlock.admin.MergeBranch.MergeException;
import com.ibm.airlock.admin.MutualExclusionGroupItem;
import com.ibm.airlock.admin.OrderingRuleItem;
import com.ibm.airlock.admin.Season;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.ValidationResults;
import com.ibm.airlock.admin.analytics.AnalyticsDataCollection.FeatureAttributesPair.AttributeTypePair;
import com.ibm.airlock.engine.Environment;
import com.ibm.airlock.engine.FeatureAttributes;
import com.ibm.airlock.engine.FeatureAttributes.Jtype;


/*
 Regular:
 {
 	"inputFieldsToAnalytics": [
 		"context.device.locale",
 		"context.device.osVersion"
 	],
 	"featuresOnOff": [
 		"bd39d284-7a51-4e37-bc79-f44099add4a7"
 	],
 	"featuresAttributesToAnalytics": [{
 		"attributes": [{
 			"name": "color",
 			"type": "REGULAR"
 		}, {
 			"name": "size",
 			"type": "CUSTOM"
 		}],
 		"id": "bd39d284-7a51-4e37-bc79-f44099add4a7"
 	}]
 }

 Verbose:
 {
    "inputFieldsToAnalytics": [
      "context.device.locale",
      "context.device.osVersion"
    ],
    "featuresOnOff": [
      {
        "name": "ns1.F3",       
        "id": "bd39d284-7a51-4e37-bc79-f44099add4a7"
      }
    ],
    "featuresAttributesToAnalytics": [
      {
        "name": "ns1.F3",
       	 "attributes": [{
 			"name": "color",
 			"type": "REGULAR"
 		 }, {
 			"name": "size",
 			"type": "custom"
 		 }]
        "id": "bd39d284-7a51-4e37-bc79-f44099add4a7"
      }
    ]
  }

 */


public class AnalyticsDataCollection {
	public static final Logger logger = Logger.getLogger(AnalyticsDataCollection.class.getName());

	public class FeatureAttributesPair {

		public class AttributeTypePair {
			String name;
			AttributeType type;

			public AttributeTypePair(String name, AttributeType type) {
				this.name = name;
				this.type = type;
			}

			public AttributeTypePair() {}


			public JSONObject toJson() throws JSONException {
				JSONObject res = new JSONObject();
				res.put(Constants.JSON_FIELD_NAME, name);
				res.put(Constants.JSON_FEATURE_FIELD_TYPE, type.toString());
				return res;
			}

			public void fromJson(JSONObject attTypePairJSON) throws JSONException {
				name = attTypePairJSON.getString(Constants.JSON_FIELD_NAME);
				type = Utilities.strToAttributeType(attTypePairJSON.getString(Constants.JSON_FEATURE_FIELD_TYPE));				
			}

			public ValidationResults ValidateAttTypePairJSON( JSONObject attTypePairJSON) throws JSONException {
				try {
					//name
					if (!attTypePairJSON.containsKey(Constants.JSON_FIELD_NAME) || attTypePairJSON.get(Constants.JSON_FIELD_NAME) == null) {
						return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_NAME), Status.BAD_REQUEST);
					}
					String attName = attTypePairJSON.getString(Constants.JSON_FIELD_NAME); //validate that is string

					//type
					if (!attTypePairJSON.containsKey(Constants.JSON_FEATURE_FIELD_TYPE) || attTypePairJSON.get(Constants.JSON_FEATURE_FIELD_TYPE) == null) {
						return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_TYPE), Status.BAD_REQUEST);
					}

					String typeStr = attTypePairJSON.getString(Constants.JSON_FEATURE_FIELD_TYPE); //validate that is string
					AttributeType tmpType = Utilities.strToAttributeType(typeStr);
					if (tmpType == null) {
						return new ValidationResults(Strings.illegalType + typeStr, Status.BAD_REQUEST);
					}

					if (tmpType == AttributeType.ARRAY){
						if (!attName.contains("[") || attName.contains("[]")) {
							return new ValidationResults("Illegal attribute " + attName + ". For ARRAY attributes, you must include brackets [] and they cannot be empty.", Status.BAD_REQUEST); 
						}
					}
					if (tmpType == AttributeType.CUSTOM){
						if (attName.contains("[]")) {
							return new ValidationResults("Illegal attribute '" + attName + "'. For CUSTOM attributes, empty brackets [] are not allowed.", Status.BAD_REQUEST); 
						}
					}
				} catch (JSONException jsne) {
					return new ValidationResults(jsne.getMessage(), Status.BAD_REQUEST);
				}

				return null;
			}

			public AttributeTypePair duplicateForNewSeason() {
				AttributeTypePair res = new AttributeTypePair();
				res.name = name;
				res.type = type;
				return res;
			}
		}

		public String id;
		public LinkedList<AttributeTypePair> attributes;

		public FeatureAttributesPair(String id) {
			this.id = id;
			this.attributes = new LinkedList<AttributeTypePair>();			
		}

		public JSONObject toJson() throws JSONException {
			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FIELD_ID, id);
			JSONArray attsArray = new JSONArray();
			for (AttributeTypePair attType:attributes) {
				JSONObject attObj = attType.toJson();
				attsArray.add(attObj);
			}
			res.put(Constants.JSON_FIELD_ATTRIBUTES, attsArray);
			return res;
		}

		public void fromJson(JSONObject featureAttPairJSON) throws JSONException {
			id = featureAttPairJSON.getString(Constants.JSON_FIELD_ID);			

			attributes.clear();

			JSONArray attsSArr = featureAttPairJSON.getJSONArray(Constants.JSON_FIELD_ATTRIBUTES);

			for (int i=0; i<attsSArr.length(); i++ ) {
				JSONObject attTypeJSON = attsSArr.getJSONObject(i);
				AttributeTypePair attType = new AttributeTypePair();
				attType.fromJson(attTypeJSON);
				attributes.add(attType);				
			}			
		}

		public ValidationResults validateFeatureAttributesPair(JSONObject featureAttPairJSON) throws JSONException {
			try {
				if (!featureAttPairJSON.containsKey(Constants.JSON_FIELD_ID) || featureAttPairJSON.get(Constants.JSON_FIELD_ID) == null) {
					return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_ID), Status.BAD_REQUEST);
				}

				String idStr = featureAttPairJSON.getString(Constants.JSON_FIELD_ID);
				String err= Utilities.validateLegalUUID(idStr);
				if (err != null) {
					return new ValidationResults("Illegal id field: Illegal GUID" + idStr + ": " + err, Status.BAD_REQUEST);
				}

				if (!featureAttPairJSON.containsKey(Constants.JSON_FIELD_ATTRIBUTES) || featureAttPairJSON.get(Constants.JSON_FIELD_ATTRIBUTES) == null) {
					return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_ATTRIBUTES), Status.BAD_REQUEST);
				}

				JSONArray attsSArr = featureAttPairJSON.getJSONArray(Constants.JSON_FIELD_ATTRIBUTES);
				TreeSet<String> attributeNames = new TreeSet<String>();
				for (int i=0; i<attsSArr.length(); i++ ) {
					JSONObject attTypeJSON = attsSArr.getJSONObject(i);
					AttributeTypePair attType = new AttributeTypePair();
					ValidationResults vr = attType.ValidateAttTypePairJSON(attTypeJSON);
					if (vr != null)
						return vr;	

					attType.fromJson(attTypeJSON);
					if (attributeNames.contains(attType.name)) {
						return new ValidationResults("The attributes " + attType.name + " appears more than once.", Status.BAD_REQUEST);
					}

					attributeNames.add(attType.name);

				}
			} catch (JSONException jsne) {
				return new ValidationResults("Illegal JSON: " + jsne.getMessage(), Status.BAD_REQUEST);
			}
			return null;
		}

		public FeatureAttributesPair duplicateForNewSeason(String newId) {
			FeatureAttributesPair res = new FeatureAttributesPair(newId);
			res.id = newId;
			for (int i=0; i<attributes.size(); i++) {
				res.attributes.add(attributes.get(i).duplicateForNewSeason());
			}

			return res;
		}

		public ValidationResults validateAttributesArr(String featureId, JSONArray attributesArr, Map<String, TreeSet<String>> featureAttributesMap, 
				Map<String, BaseAirlockItem> airlockItemsDB) throws JSONException {
			BaseAirlockItem feature = airlockItemsDB.get(featureId);
			TreeSet<String> featureAtts = new TreeSet<String>();

			for (int j=0; j<attributesArr.size(); j++) {

				String attName = attributesArr.getJSONObject(j).getString(Constants.JSON_FIELD_NAME); //i know that exists since after validation
				String attType = attributesArr.getJSONObject(j).getString(Constants.JSON_FEATURE_FIELD_TYPE); //i know that exists and valid since after validation


				if (attType.equals(AttributeType.REGULAR.toString())) {
					if (!isFeatureContainsAttribute(featureId, attName, featuresConfigurationAttributesMap) && (!featureAttributesMap.containsKey(featureId) || !featureAttributesMap.get(featureId).contains(attName) ) ) {
						return new ValidationResults("Feature " + featureId + ", " + feature.getNameSpaceDotName() + " does not contain the '" + attName + "' configuration attribute.", Status.BAD_REQUEST);							
					}

				}
				else if (attType.equals(AttributeType.CUSTOM.toString())) {
					try {						
						FeatureAttributes.validateAttributePath(attName); //throws exception if illegal
					} catch (Exception e) {
						return new ValidationResults(e.getMessage(), Status.BAD_REQUEST);															
					}
				} else {

					try {
						//if att already exists in feature attributes list - ignore missing or wrong type (probably att updated or removed)
						if (!isFeatureContainsAttribute(featureId, attName, featuresConfigurationAttributesMap)) {
							TreeSet<String> arrValues = FeatureAttributes.enumerateConstraint (featureAttributesMap.get(featureId), attName, true);
							if (arrValues == null || arrValues.size() == 0) {							
								return new ValidationResults(attName + " does not exist for feature " + featureId, Status.BAD_REQUEST); 
							}


						}
					} catch (Exception e) {
						return new ValidationResults("Illegal attribute '" + attName + "': " +e.getMessage(), Status.BAD_REQUEST);	
					}
				}

				featureAtts.add(attName);
			}
			return null;
		}

		

	}

	private LinkedList<String> featuresOnOff; 	// required in update and create. 
	private LinkedList<String> inputFieldsForAnalytics; // required in update and create.
	private LinkedList<FeatureAttributesPair> featuresConfigurationAttributesList; // required in update and create.
	private int numberOfDevelopmentItemsToAnalytics = 0;
	private int numberOfProductionItemsToAnalytics = 0;
	private int numberOfProductionInputFieldsToAnalytics = 0;
	private AirlockAnalytics airlockAnalytics = null;

	//maps for O(1) access
	private HashMap<String, Boolean> featuresOnOffMap; 	
	private HashMap<String, LinkedList<AttributeTypePair>> featuresConfigurationAttributesMap;

	private HashMap<String, JSONArray> featuresPrunedConfigurationAttributesMap;

	//map between the input field and its stage
	private HashMap<String, Stage> inputFieldsForAnalyticsStageMap;

	public AnalyticsDataCollection(AirlockAnalytics airlockAnalytics) {
		featuresOnOff = new LinkedList<String>(); 
		inputFieldsForAnalytics = new LinkedList<String>(); 
		featuresConfigurationAttributesList = new LinkedList<FeatureAttributesPair>();
		featuresOnOffMap = new HashMap<String, Boolean>();
		featuresConfigurationAttributesMap = new HashMap<String, LinkedList<AttributeTypePair>>();
		featuresPrunedConfigurationAttributesMap = new HashMap<String, JSONArray>(); //to be used in runtime
		inputFieldsForAnalyticsStageMap = new  HashMap<String, Stage>();
		this.airlockAnalytics = airlockAnalytics;
	}

	public LinkedList<String> getFeaturesOnOff() {
		return featuresOnOff;
	}

	public void setFeaturesOnOff(LinkedList<String> featuresToReportOnOff) {
		this.featuresOnOff = featuresToReportOnOff;
	}

	public LinkedList<String> getInputFieldsForAnalytics() {
		return inputFieldsForAnalytics;
	}

	public LinkedList<String> getInputFieldsForAnalyticsPerStage(Stage stage, ServletContext context, Season season) throws JSONException {
		if (stage == Stage.DEVELOPMENT) {
			return inputFieldsForAnalytics;
		}

		//PRODUCTION
		LinkedList<String> prodInputFieldsForAnalytics = new LinkedList<String>();
		if (inputFieldsForAnalytics.size()>0) {
			Map<String, String> inputFieldsMap;
			try {
				inputFieldsMap = Utilities.getStageForInputFields(context, season.getInputSchema().getMergedSchema());
			} catch (GenerationException e) {
				throw new JSONException(e.getMessage());
			}
			for (String inputField : inputFieldsForAnalytics) {
				if (inputFieldsMap.get(inputField) != null && inputFieldsMap.get(inputField).equals(Stage.PRODUCTION.toString()))
					prodInputFieldsForAnalytics.add(inputField);
			}
		}

		return prodInputFieldsForAnalytics;
	}

	public void setInputFieldsForAnalytics(LinkedList<String> inputFieldsForAnalytics) {
		this.inputFieldsForAnalytics = inputFieldsForAnalytics;
	}

	public LinkedList<FeatureAttributesPair> getFeaturesConfigurationAttributesList() {
		return featuresConfigurationAttributesList;
	}

	public void setFeaturesConfigurationAttributesList(LinkedList<FeatureAttributesPair> featuresConfigurationAttributesList) {
		this.featuresConfigurationAttributesList = featuresConfigurationAttributesList;
	}

	public HashMap<String, Boolean> getFeaturesOnOffMap() {
		return featuresOnOffMap;
	}

	public HashMap<String, LinkedList<AttributeTypePair>> getFeaturesConfigurationAttributesMap() {
		return featuresConfigurationAttributesMap;
	}

	public HashMap<String, JSONArray> getFeaturesPrunedConfigurationAttributesMap() {
		return featuresPrunedConfigurationAttributesMap;
	}

	//return empty array is not found
	public JSONArray returnAttributeArrayForFeature(String featureId) {
		//JSONArray res = new JSONArray();
		if (featuresPrunedConfigurationAttributesMap.containsKey(featureId)) {
			return featuresPrunedConfigurationAttributesMap.get(featureId);
		}
		return new JSONArray(); 
	}

	public HashMap<String, Stage> getInputFieldsForAnalyticsStageMap() {
		return inputFieldsForAnalyticsStageMap;
	}

	private void setInputFieldsForAnalyticsFromJson(JSONArray inputFieldsArr, ServletContext context, UUID seasonId, Environment env) throws JSONException {
		//in branch - keep only the delta

		@SuppressWarnings("unchecked")			
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

		Season season = seasonsDB.get(seasonId.toString());

		if (!env.isInMaster()) {

			//In branch add only Delta
			LinkedList<String> allInputFields = Utilities.jsonArrToStringsList(inputFieldsArr);
			TreeSet<String> currentBranchInputFieldsSet = Utilities.stringsListToTreeSet(inputFieldsForAnalytics);
			inputFieldsForAnalytics.clear();
			for (String inputField:allInputFields) {
				if (!season.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection().getInputFieldsForAnalyticsStageMap().containsKey(inputField) ||
						currentBranchInputFieldsSet.contains(inputField)) {
					//add the input field only if was there before or if not in master. (can be duplicate as in master only if was in branch before master so
					//wont be deleted from branch if removed from master) 
					inputFieldsForAnalytics.add(inputField);
				}
			}
		}
		else {
			inputFieldsForAnalytics = Utilities.jsonArrToStringsList(inputFieldsArr);
		}

		if (inputFieldsForAnalytics.size()>0) {
			Map<String, String> newInputFieldsMap;
			try {
				newInputFieldsMap = Utilities.getStageForInputFields(context, season.getInputSchema().getMergedSchema());
			} catch (GenerationException e) {
				throw new JSONException("GenerationException: " + e.getMessage());
			}
	
			inputFieldsForAnalyticsStageMap.clear();
			for (int i=0; i<inputFieldsForAnalytics.size(); i++) {	
				if (newInputFieldsMap.get(inputFieldsForAnalytics.get(i)) == null) {
					//should not happen since field was validated
					continue;
				}
	
				inputFieldsForAnalyticsStageMap.put(inputFieldsForAnalytics.get(i), Stage.valueOf(newInputFieldsMap.get(inputFieldsForAnalytics.get(i))));
			}
		}
	}

	private void setFeaturesOnOffFromJson(JSONObject input, ServletContext context, UUID seasonId, Environment env) throws JSONException {
		
		@SuppressWarnings("unchecked")			
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

		Season season = seasonsDB.get(seasonId.toString());

		
		featuresOnOff.clear();
		featuresOnOffMap.clear();

//		featuresOnOff = Utilities.jsonArrToStringsList(input.getJSONArray(Constants.JSON_FIELD_FEATURES_CONFIGS_FOR_ANALYTICS));
		if (!env.isInMaster()) {
			//In branch add only Delta
			LinkedList<String> allFeaturesOnOff = Utilities.jsonArrToStringsList(input.getJSONArray(Constants.JSON_FIELD_FEATURES_CONFIGS_FOR_ANALYTICS));
			TreeSet<String> currentBranchFeaturesOnOff = Utilities.stringsListToTreeSet(featuresOnOff);
			
			for (String fOnOff:allFeaturesOnOff) {
				if (!season.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection().getFeaturesOnOffMap().containsKey(fOnOff) ||
						currentBranchFeaturesOnOff.contains(fOnOff)) {

					//add the feature only if was in branch before or if not in master. (can be duplicate as in master only if was in branch before master so

					//wont be deleted from branch if removed from master)
					featuresOnOff.add(fOnOff);
				}
			}
		}
		else {
			featuresOnOff = Utilities.jsonArrToStringsList(input.getJSONArray(Constants.JSON_FIELD_FEATURES_CONFIGS_FOR_ANALYTICS));
		}


		for (String featureOnOff : featuresOnOff) {
			featuresOnOffMap.put (featureOnOff, true);
		}
	}

	private void setFeaturesConfigAttsFromjson(JSONObject input, ServletContext context, UUID seasonId, Environment env) throws JSONException {

		@SuppressWarnings("unchecked")			
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

		Season season = seasonsDB.get(seasonId.toString());
		JSONArray featureConfigAttArr= input.getJSONArray(Constants.JSON_FIELD_FEATURES_ATTRIBUTES_FOR_ANALYTICS);
		
		if (env.isInMaster()) {
			featuresConfigurationAttributesList.clear();
			featuresConfigurationAttributesMap.clear();
			featuresPrunedConfigurationAttributesMap.clear();
		} else {
			//remove only features + atts that exists only in branch. keep the feature + atts that were declared in the 
			//master as well as in branch, so if they will be removed from master - they wont be removed from branch.
			for (int i=0; i<featuresConfigurationAttributesList.size(); i++) {
				FeatureAttributesPair faPair = featuresConfigurationAttributesList.get(i);
				if (!season.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection().getFeaturesConfigurationAttributesMap().containsKey(faPair.id)) {
					featuresConfigurationAttributesMap.remove(faPair.id);
					featuresPrunedConfigurationAttributesMap.remove(faPair.id);
					featuresConfigurationAttributesList.remove(i);
				}
			}		
		}

		if (featureConfigAttArr!=null && featureConfigAttArr.size()>0) {
			Map<String, TreeSet<String>> featureAttributesMap = getFeatureAttributesMap (season.getRoot().toJson(OutputJSONMode.ADMIN, context, env), season.getEntitlementsRoot().toJson(OutputJSONMode.ADMIN, context, env), season, context);

			for (int i=0; i<featureConfigAttArr.size(); i++) {
				JSONObject featureConfigAttObj = featureConfigAttArr.getJSONObject(i);
				setSingleFeatureAttributesFromJson(featureConfigAttObj, featureAttributesMap, season, env);		
			}
		}
	}

	private boolean isAttributeInList(String attributeName, LinkedList<AttributeTypePair> attrbutesList) {
		for (AttributeTypePair masterAttTypePair : attrbutesList) {
			if (masterAttTypePair.name.equals(attributeName)) {
				return true;			
			}
		}
		return false;
	}
	
	private void setSingleFeatureAttributesFromJson(JSONObject featureConfigAttObj, Map<String, TreeSet<String>> featureAttributesMap, Season season, Environment env) throws JSONException {
		String featureId = featureConfigAttObj.getString(Constants.JSON_FIELD_ID);
		FeatureAttributesPair featureAtts = new FeatureAttributesPair(featureId);
		featureAtts.fromJson(featureConfigAttObj);
		
		LinkedList<AttributeTypePair> currentFeatureAttrbutes = new LinkedList<AttributeTypePair>();
		if (featuresConfigurationAttributesMap.containsKey(featureId)) {
			removeFeatureFromFeaturesConfigAttList(featureId);
			currentFeatureAttrbutes = featuresConfigurationAttributesMap.get(featureId);
		}

		if (!env.isInMaster()) {
			
			//add only attributes that are not reported in master (only delta)
			LinkedList<AttributeTypePair> masterFeatureAttsPairList = season.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection().getFeaturesConfigurationAttributesMap().get(featureAtts.id);
			if (masterFeatureAttsPairList!=null) {
				LinkedList<AttributeTypePair> branchDeltaAttsList = new LinkedList<AttributeTypePair>();
				for(AttributeTypePair attTypePair:featureAtts.attributes) {
					boolean foundInMaster = isAttributeInList(attTypePair.name, masterFeatureAttsPairList);
					/*for (AttributeTypePair masterAttTypePair : masterFeatureAttsPairList) {
						if (masterAttTypePair.name.equals(attTypePair.name)) {
							foundInMaster = true;
							break;
						}
					}*/
						
					if (!foundInMaster || (foundInMaster && isAttributeInList(attTypePair.name, currentFeatureAttrbutes))) {
						branchDeltaAttsList.add(attTypePair);				
					}
				}
				featureAtts.attributes = branchDeltaAttsList;
			}
		}
		if (featureAtts.attributes!=null && featureAtts.attributes.size()>0) {
			featuresConfigurationAttributesList.add(featureAtts);
			featuresConfigurationAttributesMap.put (featureAtts.id, featureAtts.attributes);
			updateFeaturesPrunedAttributes (featureAtts.id, featureAtts.attributes, featureAttributesMap);	
		}
	}

	public void addDeltaFeatureAttsPair (FeatureAttributesPair featureAtts, Map<String, TreeSet<String>> featureAttributesMap, Season season, Environment env, boolean isProductionFeatureInBranch, boolean isProductionFeatureInMaster) {
		int addedAttribures = 0;
		TreeSet<String> existingAttributes = new TreeSet<String>();
		if (!featuresConfigurationAttributesMap.containsKey(featureAtts.id)) {
			//this feature attributes are not reported at all - all the feature attributes as is
			featuresConfigurationAttributesList.add(featureAtts);
			featuresConfigurationAttributesMap.put (featureAtts.id, featureAtts.attributes);
			updateFeaturesPrunedAttributes (featureAtts.id, featureAtts.attributes, featureAttributesMap);
			addedAttribures = featuresPrunedConfigurationAttributesMap.get(featureAtts.id).size();
			
			numberOfDevelopmentItemsToAnalytics = numberOfDevelopmentItemsToAnalytics + addedAttribures;

			if (isProductionFeatureInBranch)
				numberOfProductionItemsToAnalytics = numberOfProductionItemsToAnalytics + addedAttribures;
			
			return;
		}
		else {
			//some or all of the attributes are reported - add only the delta's			
			LinkedList<AttributeTypePair> newAttsList = new LinkedList<AttributeTypePair>();			
			for (AttributeTypePair existingAttTypePair : featuresConfigurationAttributesMap.get(featureAtts.id)) {
				newAttsList.add(existingAttTypePair);
				existingAttributes.add(existingAttTypePair.name);
			}
			
			for(AttributeTypePair attTypePair:featureAtts.attributes) {
				if (!existingAttributes.contains(attTypePair.name)) {
					newAttsList.add(attTypePair);
					addedAttribures++;
				}
			}
			
			FeatureAttributesPair mergedFeatureattributes = new FeatureAttributesPair(featureAtts.id);
			mergedFeatureattributes.attributes = newAttsList;
			
			//replace attributes list with the existing one.
			for (int i=0; i<featuresConfigurationAttributesList.size(); i++) {
				if (featuresConfigurationAttributesList.get(i).id.equals(featureAtts.id)) {
					featuresConfigurationAttributesList.get(i).attributes = newAttsList;					
				}
			}
			
			featuresConfigurationAttributesMap.put (mergedFeatureattributes.id, mergedFeatureattributes.attributes);
			updateFeaturesPrunedAttributes (mergedFeatureattributes.id, mergedFeatureattributes.attributes, featureAttributesMap);
		}		

		if (isProductionFeatureInBranch!=isProductionFeatureInMaster) {
			//stage moved in branch
			if (isProductionFeatureInMaster) {
				//change to dev in branch - remove existing att from prod counter
				numberOfProductionItemsToAnalytics = numberOfProductionItemsToAnalytics-existingAttributes.size();
				numberOfDevelopmentItemsToAnalytics = numberOfDevelopmentItemsToAnalytics + addedAttribures;
			}
			else {
				//change to prod in branch - add existing att + addedattributes to prod counter
				numberOfProductionItemsToAnalytics = numberOfProductionItemsToAnalytics + existingAttributes.size() + addedAttribures;
				numberOfDevelopmentItemsToAnalytics = numberOfDevelopmentItemsToAnalytics + addedAttribures;
			}
		}
		else {
		
			numberOfDevelopmentItemsToAnalytics = numberOfDevelopmentItemsToAnalytics + addedAttribures;

			if (isProductionFeatureInBranch)
				numberOfProductionItemsToAnalytics = numberOfProductionItemsToAnalytics + addedAttribures;
		}
		
		

	}

	public int getNumberOfDevelopmentItemsToAnalytics() {
		return numberOfDevelopmentItemsToAnalytics;
	}

	public void setNumberOfDevelopmentItemsToAnalytics(int numberOfDevelopmentItemsToAnalytics) {
		this.numberOfDevelopmentItemsToAnalytics = numberOfDevelopmentItemsToAnalytics;
	}

	public int getNumberOfProductionItemsToAnalytics() {
		return numberOfProductionItemsToAnalytics;
	}

	public void setNumberOfProductionItemsToAnalytics(int numberOfProductionItemsToAnalytics) {
		this.numberOfProductionItemsToAnalytics = numberOfProductionItemsToAnalytics;
	}

	public int getNumberOfProductionInputFieldsToAnalytics() {
		return numberOfProductionInputFieldsToAnalytics;
	}

	public void setNumberOfProductionInputFieldsToAnalytics(int numberOfProductionInputFields) {
		this.numberOfProductionInputFieldsToAnalytics = numberOfProductionInputFields;
	}

	private JSONArray calcFeaturesPrunedAttributes (String featureId, LinkedList<AttributeTypePair> attsList,  Map<String, TreeSet<String>> featureAttributesMap) {
		JSONArray prunedAttArray = new JSONArray();
		for (int j=0; j<attsList.size(); j++) {
			if (attsList.get(j).type.equals(AttributeType.ARRAY)) {
				try {
					//prune the array even if the att does not exists in the feature any more
					TreeSet<String> arrValues = FeatureAttributes.enumerateConstraint (featureAttributesMap.get(featureId), attsList.get(j).name, false); 
					if (arrValues != null && arrValues.size()>0) {
						for (String val:arrValues) {
							prunedAttArray.add(val);
						}
					}					
				} catch (Exception e) {
					//ignore
				}
			}
			else {
				prunedAttArray.add(attsList.get(j).name);
			}
		}
		return prunedAttArray;
	}

	private void updateFeaturesPrunedAttributes (String featureId, LinkedList<AttributeTypePair> attsList,  Map<String, TreeSet<String>> featureAttributesMap) {
		JSONArray prunedAttArray = calcFeaturesPrunedAttributes (featureId, attsList, featureAttributesMap);

		featuresPrunedConfigurationAttributesMap.put(featureId, prunedAttArray);	
	}

	public void fromJSON(JSONObject input, ServletContext context, UUID seasonId, Environment env, Map<String, BaseAirlockItem> airlockItemsDB ) throws JSONException {
		//input fields for analytics	
		setInputFieldsForAnalyticsFromJson(input.getJSONArray(Constants.JSON_FIELD_INPUT_FIELDS_FOR_ANALYTICS), context, seasonId, env);

		//features on off
		setFeaturesOnOffFromJson(input, context, seasonId, env);

		//config attributes
		setFeaturesConfigAttsFromjson(input, context, seasonId, env);

		//calculate the number of items to report to analytics 
		calcNumberOfItemsToAnalytics(context, seasonId, airlockItemsDB);
	}

	public JSONObject toJson(boolean verbose, ServletContext context, Season season, Boolean includeWarning, Map<String, BaseAirlockItem> airlockItemsDB, Environment env) throws JSONException {

		JSONObject res = new JSONObject();

		res.put(Constants.JSON_FIELD_INPUT_FIELDS_FOR_ANALYTICS, inputFieldsForAnalytics);

		if (verbose) {
			JSONArray featuresOnOffArr = new JSONArray();
			for (String featureId: featuresOnOff) {
				JSONObject fOnOff = new JSONObject();
				fOnOff.put(Constants.JSON_FIELD_ID, featureId);
				fOnOff.put(Constants.JSON_FIELD_NAME, airlockItemsDB.get(featureId).getNameSpaceDotName());
				featuresOnOffArr.add(fOnOff);				
			}
			res.put(Constants.JSON_FIELD_FEATURES_CONFIGS_FOR_ANALYTICS, featuresOnOffArr);
		} else {
			res.put(Constants.JSON_FIELD_FEATURES_CONFIGS_FOR_ANALYTICS, featuresOnOff);
		}


		JSONArray featuresConfigurationAttributesArr = null;
		if (includeWarning) {			
			Map<String, TreeSet<String>> featureAttributesMap = null;
			if (featuresConfigurationAttributesMap.size()>0) {
				// build the att map from the merged features tree:
				BaseAirlockItem featuresRootOfMergedTree = airlockItemsDB.get(season.getRoot().getUniqueId().toString());
				BaseAirlockItem entitlementsRootOfMergedTree = airlockItemsDB.get(season.getEntitlementsRoot().getUniqueId().toString());
				featureAttributesMap = getFeatureAttributesMap (featuresRootOfMergedTree.toJson(OutputJSONMode.ADMIN, context, env), entitlementsRootOfMergedTree.toJson(OutputJSONMode.ADMIN, context, env), season, context);
			}

			FeatureAttributes featureAttributesObj = null;

			try {
				String javascriptUtilities = season.getUtilities().generateUtilityCodeSectionForStageAndType(Stage.DEVELOPMENT, null, null, null, UtilityType.MAIN_UTILITY);
				JSONObject maxInputSample = season.getInputSchema().generateInputSample(Stage.DEVELOPMENT, "999999", context, InputSampleGenerationMode.MAXIMAL);
				JSONObject enStrings = season.getOriginalStrings().toEnStringsJSON (Stage.DEVELOPMENT).getJSONObject(Constants.JSON_FIELD_STRINGS);				
				featureAttributesObj = new FeatureAttributes (maxInputSample.toString(), javascriptUtilities, enStrings.toString(), env);
			} catch (Exception e) {
				throw new JSONException("Failed to generate attribute types map: " + e.getMessage());			
			}	

			featuresConfigurationAttributesArr = featuresConfigurationAttributesListToJson(verbose, airlockItemsDB, featureAttributesMap, context, env, featureAttributesObj, season);
		}
		else {
			featuresConfigurationAttributesArr = featuresConfigurationAttributesListToJson(verbose, airlockItemsDB, null, null, env, null, season);
		}

		res.put(Constants.JSON_FIELD_FEATURES_ATTRIBUTES_FOR_ANALYTICS, featuresConfigurationAttributesArr);


		res.put(Constants.JSON_FIELD_FEATURES_ATTRIBUTES_FOR_ANALYTICS, featuresConfigurationAttributesArr);

		return res;
	}
	private ValidationResults validateInputFieldsForAnalytics (JSONArray inputFieldsArr, ServletContext context, Season season) throws GenerationException, JSONException {
		if (inputFieldsArr!=null && inputFieldsArr.size()>0) {				
			Map<String,String> inputFieldsMap = Utilities.getStageForInputFields(context, season.getInputSchema().getMergedSchema());
			//validate that specified inputFields actually exist and only once
			TreeSet<String> inputFields = new TreeSet<String>();
			for (int k=0; k<inputFieldsArr.length(); k++) {
				if (!inputFieldsMap.containsKey(inputFieldsArr.getString(k))) {
					return new ValidationResults("The input field '" + inputFieldsArr.get(k) + "' does not exist in the input schema.", Status.BAD_REQUEST);
				}
				if (inputFields.contains(inputFieldsArr.get(k))) {
					return new ValidationResults("The field " + inputFieldsArr.get(k) + " exists more than once in the inputFields list.", Status.BAD_REQUEST);
				}
				inputFields.add(inputFieldsArr.getString(k));
			}
		}

		return null;
	}

	public ValidationResults validateDataColJSON(JSONObject dataColObj, ServletContext context, UserInfo userInfo, Season season, Environment env, 
			Map<String, BaseAirlockItem> airlockItemsDB, boolean productionChangeAllowed ) throws MergeException {
		//there is no create mode. This object is created when season is created. only update is available

		try {		

			if (!dataColObj.containsKey(Constants.JSON_FIELD_FEATURES_CONFIGS_FOR_ANALYTICS) || dataColObj.get(Constants.JSON_FIELD_FEATURES_CONFIGS_FOR_ANALYTICS) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_FEATURES_CONFIGS_FOR_ANALYTICS), Status.BAD_REQUEST); 
			}

			JSONArray featuresOnOffArr = dataColObj.getJSONArray(Constants.JSON_FIELD_FEATURES_CONFIGS_FOR_ANALYTICS); //validate that is String array value

			//validate that specified features/config actually exist and only once, and not in master when updating branch
			TreeSet<String> featureOnOffIds = new TreeSet<String>();
			for (int k=0; k<featuresOnOffArr.length(); k++) {
				if (!airlockItemsDB.containsKey(featuresOnOffArr.get(k))) {
					return new ValidationResults("The feature or configuration '" + featuresOnOffArr.get(k) + "' does not exist.", Status.BAD_REQUEST); 
				}
				if (featureOnOffIds.contains(featuresOnOffArr.get(k))) {
					return new ValidationResults("The item " + featuresOnOffArr.get(k) + " exists more than once in the featuresAndConfigurationsForAnalytics list.", Status.BAD_REQUEST);
				}
				
				if (!env.isInMaster()) {
					BaseAirlockItem alItemToAna = airlockItemsDB.get(featuresOnOffArr.get(k)); //not null - checked above
 					if (alItemToAna.getBranchStatus().equals(BranchStatus.NONE) && 
 							!season.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection().getFeaturesOnOffMap().containsKey(featuresOnOffArr.get(k))) {
 						//If feature is not checked out or new you cannot add it to analyze from branch
						return new ValidationResults("Feature " + alItemToAna.getUniqueId().toString() + " is not checked out. First check out the feature.", Status.BAD_REQUEST);
					}
				}
								
				featureOnOffIds.add(featuresOnOffArr.getString(k));					
			}
			
			if (!env.isInMaster()) {
				//validate that no feature from master was removed in branch (only if seen by branch ie not added to master after checkout)
				LinkedList<String> masterFeaturesOnOff = season.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection().getFeaturesOnOff();
				for (String masterfeatureOnOff:masterFeaturesOnOff) {
					if (!featureOnOffIds.contains(masterfeatureOnOff) && airlockItemsDB.containsKey(masterfeatureOnOff)) {
						return new ValidationResults("Feature " + masterfeatureOnOff + " is reported to analytics in the master. You cannot stop reporting to analytics the branch.", Status.BAD_REQUEST);						
					}
				}
			}

			if (!dataColObj.containsKey(Constants.JSON_FIELD_INPUT_FIELDS_FOR_ANALYTICS) || dataColObj.get(Constants.JSON_FIELD_INPUT_FIELDS_FOR_ANALYTICS) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_INPUT_FIELDS_FOR_ANALYTICS), Status.BAD_REQUEST); 				
			}

			JSONArray inputFieldsArr = dataColObj.getJSONArray(Constants.JSON_FIELD_INPUT_FIELDS_FOR_ANALYTICS); //validate that is String array value
			//ValidationResults vr = validateInputFieldsForAnalytics(inputFieldsArr, context, season);
			ValidationResults vr = validateInputFieldsUpdate(inputFieldsArr, context, season, productionChangeAllowed, env, true); //skip counter check - will be done later
			if (vr!=null && !vr.status.equals(Status.OK))
				return vr;			

			if (!dataColObj.containsKey(Constants.JSON_FIELD_FEATURES_ATTRIBUTES_FOR_ANALYTICS) || dataColObj.get(Constants.JSON_FIELD_FEATURES_ATTRIBUTES_FOR_ANALYTICS) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_FEATURES_ATTRIBUTES_FOR_ANALYTICS), Status.BAD_REQUEST); 				
			}

			JSONArray configAttsToAnalyticsArr = dataColObj.getJSONArray(Constants.JSON_FIELD_FEATURES_ATTRIBUTES_FOR_ANALYTICS); //validate that is JSON object value
			if (configAttsToAnalyticsArr==null || configAttsToAnalyticsArr.size()==0) {
				if (!env.isInMaster()) {
					//validate that no feature Attributes from master was removed in branch
					LinkedList<FeatureAttributesPair> masterFeaturesAttList = season.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection().getFeaturesConfigurationAttributesList();
					if (masterFeaturesAttList.size() > 0) {
						//Cannot remove attributes that are reported to analytics in the master in branch						
						return new ValidationResults("You must report all attributes that are reported in the master, in addition to the attributes that you want to report in the branch.", Status.BAD_REQUEST);
					}
					
				}
			}
			else {
				//if (configAttsToAnalyticsArr.size() > 0) {
				BaseAirlockItem featuresRoot = airlockItemsDB.get(season.getRoot().getUniqueId().toString());
				BaseAirlockItem entitlementsRoot = airlockItemsDB.get(season.getEntitlementsRoot().getUniqueId().toString());
				Map<String, TreeSet<String>> featureAttributesMap = getFeatureAttributesMap (featuresRoot.toJson(OutputJSONMode.ADMIN, context, env), entitlementsRoot.toJson(OutputJSONMode.ADMIN, context, env), season, context);
				

				TreeSet<String> featureIds = new TreeSet<String>();
				for (int i=0; i<configAttsToAnalyticsArr.size(); i++) {
					JSONObject featureConfigAttObj = configAttsToAnalyticsArr.getJSONObject(i);

					if (!featureConfigAttObj.containsKey(Constants.JSON_FIELD_ID) || featureConfigAttObj.get(Constants.JSON_FIELD_ID) == null) {
						return new ValidationResults("The id field of one of the configAttributesToAnalytics items is missing.", Status.BAD_REQUEST); 					
					}

					String featureId = featureConfigAttObj.getString(Constants.JSON_FIELD_ID);				
					FeatureAttributesPair tmp = new FeatureAttributesPair(featureId);
					vr = tmp.validateFeatureAttributesPair(featureConfigAttObj);
					if (vr != null)
						return vr;

					if (!airlockItemsDB.containsKey(featureId)) {
						return new ValidationResults("The feature '" + featureId + "' does not exist.", Status.BAD_REQUEST); 
					}

					BaseAirlockItem feature = airlockItemsDB.get(featureId);
					if (!(feature instanceof FeatureItem)) {
						return new ValidationResults("The item '" + featureId + "' is not a feature. Only features can contain attributes.", Status.BAD_REQUEST);
					}
					
					if (featureIds.contains(featureId)) {
						return new ValidationResults("The feature '" + featureId + "' apprears more than once in the featuresAttributesForAnalytics array.", Status.BAD_REQUEST); 
					}

					featureIds.add(featureId);

					if (!featureConfigAttObj.containsKey(Constants.JSON_FIELD_ATTRIBUTES) || featureConfigAttObj.get(Constants.JSON_FIELD_ATTRIBUTES) == null) {
						return new ValidationResults("The attributes field of one of the configurationAttributesToAnalytics items is missing.", Status.BAD_REQUEST); 					
					}

					JSONArray attributesArr = featureConfigAttObj.getJSONArray(Constants.JSON_FIELD_ATTRIBUTES);

					if (attributesArr.size() == 0) {
						return new ValidationResults("The attributes array cannot be empty.", Status.BAD_REQUEST);
					}

					//vr = tmp.validateAttributesArr(featureId, attributesArr, featureAttributesMap, airlockItemsDB);
					vr = validateFeatureAttributsUpdate((FeatureItem)feature, attributesArr, context, season, airlockItemsDB, featureAttributesMap, env, true); //skip counter checking - will be done later						
					if (vr != null)
						return vr;
				}
				if (!env.isInMaster()) {
					//validate that no feature Attributes from master was removed in branch
					Set<String> masterFeaturesReportAtt = season.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection().getFeaturesConfigurationAttributesMap().keySet();
					for (String masterfeatureAtt:masterFeaturesReportAtt) {
						if (!featureIds.contains(masterfeatureAtt) && airlockItemsDB.containsKey(masterfeatureAtt)) {
							//Cannot remove feature that reports attributes to analytics from master in branch						
							return new ValidationResults("You must report all attributes that are reported in the master.", Status.BAD_REQUEST);
						}
					}
				}
			}

			AnalyticsDataCollection temp = new AnalyticsDataCollection(airlockAnalytics);
			temp.fromJSON(dataColObj, context, season.getUniqueId(), env, airlockItemsDB);

			if (temp.numberOfProductionItemsToAnalytics > season.getAnalytics().getAnalyticsQuota()) {
				return new ValidationResults("The maximum number of items in production to send to analytics was exceeded. The maximum number allowed is " + season.getAnalytics().getAnalyticsQuota() + ". " + numberOfProductionItemsToAnalytics + " were previously selected, and you attempted to select " + temp.numberOfProductionItemsToAnalytics, Status.BAD_REQUEST);
			}
			
			List<Experiment> seasonsProdExperiments = season.getExperimentsForSeason(context, true);
			if (seasonsProdExperiments!=null && !seasonsProdExperiments.isEmpty()) {
				for (Experiment exp:seasonsProdExperiments) {
					String seasonToReplace = null;
					String branchToReplace = null;
					AnalyticsDataCollection AnalyticsDataCollectionToReplace = temp;
					if (env.isInMaster()) {
						seasonToReplace = season.getUniqueId().toString();						
					}
					else {
						branchToReplace = env.getBranchId();						
					}
					int updatedProdCount = exp.getAnalyticsProductionCounter(context, seasonToReplace, branchToReplace, AnalyticsDataCollectionToReplace);
					int experimentAnalyticsQuota = exp.getQuota(context);
					if (updatedProdCount > experimentAnalyticsQuota) {
						return new ValidationResults("Failed to update analytics: The maximum number of items in production to send to analytics for experiment " + exp.getName() + " was exceeded. The maximum number allowed is " + experimentAnalyticsQuota + ". The update increased the number to " + updatedProdCount, Status.BAD_REQUEST);						
					}
				}	
			}
		

		} catch (JSONException je) {
			return new ValidationResults(je.getMessage(), Status.BAD_REQUEST);
		} 
		
		return null;
	}

	public String updateDataCollection(JSONObject updatedDataColJSON, ServletContext context, UUID seasonId, Environment env, 
			Map<String, BaseAirlockItem> airlockItemsDB) throws JSONException {
		StringBuilder updateDetails = new StringBuilder();

		LinkedList<String> origInputFieldsForAnalytics = null;
		LinkedList<String> origFeaturesOnOff = null;
		LinkedList<FeatureAttributesPair> origFeaturesConfigurationAttributesList = null;
		HashMap<String,LinkedList<AttributeTypePair>> origFeaturesConfigurationAttributesMap = null;
		if (env.isInMaster()) {
			origInputFieldsForAnalytics = inputFieldsForAnalytics;
			origFeaturesOnOff = featuresOnOff;
			origFeaturesConfigurationAttributesList = featuresConfigurationAttributesList;
			origFeaturesConfigurationAttributesMap = featuresConfigurationAttributesMap;
		}
		else {
			//in branch compare the given list to the merged list
			origInputFieldsForAnalytics = env.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection().getInputFieldsForAnalytics();
			origFeaturesOnOff = env.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection().getFeaturesOnOff();
			origFeaturesConfigurationAttributesList = env.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection().getFeaturesConfigurationAttributesList();
			origFeaturesConfigurationAttributesMap = env.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection().getFeaturesConfigurationAttributesMap();
		}
		
		
		JSONArray updatedInputFields = updatedDataColJSON.getJSONArray(Constants.JSON_FIELD_INPUT_FIELDS_FOR_ANALYTICS);
		if (!Utilities.compareIgnoreOrder(updatedInputFields, origInputFieldsForAnalytics)) {
			updateDetails.append("'inputFieldsForAnalytics' changed from " + Utilities.StringsListToString(origInputFieldsForAnalytics) + " to " +  Arrays.toString(Utilities.jsonArrToStringArr(updatedInputFields)) + "\n");
			setInputFieldsForAnalyticsFromJson(updatedInputFields, context, seasonId, env);
		}

		
		JSONArray updatedFeaturesOnOff = updatedDataColJSON.getJSONArray(Constants.JSON_FIELD_FEATURES_CONFIGS_FOR_ANALYTICS);
		if (!Utilities.compareIgnoreOrder(updatedFeaturesOnOff, origFeaturesOnOff)) {
			updateDetails.append("'featuresAndConfigurationsForAnalytics' changed from " + Utilities.StringsListToString(origFeaturesOnOff) + " to " +  Arrays.toString(Utilities.jsonArrToStringArr(updatedFeaturesOnOff)) + "\n");
			setFeaturesOnOffFromJson(updatedDataColJSON, context, seasonId, env);
		}


		JSONArray updatedFeatureConfigAttArr= updatedDataColJSON.getJSONArray(Constants.JSON_FIELD_FEATURES_ATTRIBUTES_FOR_ANALYTICS);
		if (!compareConfigAttsIgnoreOrder(updatedFeatureConfigAttArr, origFeaturesConfigurationAttributesList, origFeaturesConfigurationAttributesMap)) {

			@SuppressWarnings("unchecked")			
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			Season season = seasonsDB.get(seasonId);
			JSONArray prevConfigAtts = featuresConfigurationAttributesListToJson(false, airlockItemsDB, null, null, env, null, season);
			setFeaturesConfigAttsFromjson(updatedDataColJSON, context, seasonId, env);

			JSONArray newConfigAtts = featuresConfigurationAttributesListToJson(false, airlockItemsDB, null, null, env, null, season);
			updateDetails.append("'featuresAttributesForAnalytics' changed from \n" + prevConfigAtts.write(true) + "\n to \n" +  newConfigAtts.write(true) + "\n");
		}

		//calculate the number of items to report to analytics 
		calcNumberOfItemsToAnalytics(context, seasonId, airlockItemsDB);

		return updateDetails.toString();		
	}


	private boolean compareConfigAttsIgnoreOrder(JSONArray updatedFeatureConfigAttArr, LinkedList<FeatureAttributesPair> featuresConfigAttList,  
			HashMap<String, LinkedList<AttributeTypePair>> featuresConfigAttMap) throws JSONException {
		if (updatedFeatureConfigAttArr == null && featuresConfigAttList == null)
			return true;

		if (updatedFeatureConfigAttArr == null || featuresConfigAttList == null)
			return false;

		if (updatedFeatureConfigAttArr.size() != featuresConfigAttList.size())
			return false;

		//same amount of features
		for (int i=0; i<updatedFeatureConfigAttArr.size(); i++) {
			JSONObject featureConfigAttObj = updatedFeatureConfigAttArr.getJSONObject(i);
			String featureId = featureConfigAttObj.getString(Constants.JSON_FIELD_ID);

			if (!featuresConfigAttMap.containsKey(featureId))
				return false;

			LinkedList<AttributeTypePair> currentAttributes = featuresConfigAttMap.get(featureId);
			FeatureAttributesPair updatedFeatureAttsPair = new FeatureAttributesPair(featureId);
			updatedFeatureAttsPair.fromJson(featureConfigAttObj);
			if (!compareAttributeTypeLists (updatedFeatureAttsPair.attributes, currentAttributes)) {
				return false;
			}
		}

		return true;
	}

	private boolean compareAttributeTypeLists(LinkedList<AttributeTypePair> updatedAttributes, LinkedList<AttributeTypePair> currentAttributes) {
		if (updatedAttributes == null && currentAttributes == null)
			return true;

		if (updatedAttributes == null || currentAttributes == null)
			return false;

		if (updatedAttributes.size() != currentAttributes.size())
			return false;

		for (int i=0; i<currentAttributes.size(); i++) {
			AttributeTypePair curAttType =  currentAttributes.get(i);
			boolean found = false;
			for (int j=0; j<updatedAttributes.size(); j++) {
				AttributeTypePair updatedAttType =  updatedAttributes.get(j);
				if (curAttType.name.equals(updatedAttType.name)) {
					if (curAttType.type.equals(updatedAttType.type)) {
						found = true;
						break;
					}
					else {
						//same name diff type
						return false;
					}
				}
			}
			if (!found)
				return false;
		}
		return true;
	}

	private JSONArray featuresConfigurationAttributesListToJson(boolean verbose, Map<String, BaseAirlockItem> airlockItemsDB,
			Map<String, TreeSet<String>> featureAttributesMap, ServletContext context, Environment env, 
			FeatureAttributes featureAttributesObj, Season season) throws JSONException {
		JSONArray res = new JSONArray();				

		for (int i=0; i<featuresConfigurationAttributesList.size(); i++){
			FeatureAttributesPair fa = featuresConfigurationAttributesList.get(i);
			JSONObject faObj = new JSONObject();
			faObj.put(Constants.JSON_FIELD_ID, fa.id);
								
			faObj.put(Constants.JSON_FIELD_ATTRIBUTES, attributesListToJSONArray(fa.attributes, fa.id, featureAttributesMap, context, env, featureAttributesObj, season, false, airlockItemsDB));
			if (verbose) {
				faObj.put(Constants.JSON_FIELD_NAME, airlockItemsDB.get(fa.id).getNameSpaceDotName());
			}
			res.add(faObj);
		}	

		return res;
	}

	//branch will be given in display mode when analytics is branch analytics
	private JSONArray attributesListToJSONArray(LinkedList<AttributeTypePair> attsList, String featureId, 
			Map<String, TreeSet<String>> featureAttributesMap, ServletContext context, Environment env, 
			FeatureAttributes featureAttributesObj, Season season, boolean addSourceBranchField, Map<String, BaseAirlockItem> airlockItemsDB) throws JSONException {
		JSONArray arr = new JSONArray();

		Map<String, TreeSet<Jtype>> attributesTypes = null;

		for (int i=0; i<attsList.size(); i++) {
			JSONObject attJSON = attsList.get(i).toJson();

			if (addSourceBranchField) {
				if (env.isInMaster()) {
					attJSON.put(Constants.JSON_FIELD_BRANCH_NAME, Constants.MASTER_BRANCH_NAME);
				}
				else {
					HashMap<String,LinkedList<AnalyticsDataCollection.FeatureAttributesPair.AttributeTypePair>> masterFeatureAttributesMap = season.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection().getFeaturesConfigurationAttributesMap();
					if (isFeatureContainsAttribute(featureId, attJSON.getString(Constants.JSON_FIELD_NAME), masterFeatureAttributesMap)) {
						attJSON.put(Constants.JSON_FIELD_BRANCH_NAME, Constants.MASTER_BRANCH_NAME);
					}									
					else {
						@SuppressWarnings("unchecked")
						Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);
	
						Branch branch = branchesDB.get(env.getBranchId());
						attJSON.put(Constants.JSON_FIELD_BRANCH_NAME, branch.getName());
					}
				}
			}
			
			//add attribute warning messages
			if (featureAttributesMap!=null && featureAttributesObj!=null && !attsList.get(i).type.equals(AttributeType.CUSTOM)) {
				TreeSet<String> featureExistingAttributes = featureAttributesMap.get(featureId);
				try {
					TreeSet<String> values = FeatureAttributes.enumerateConstraint (featureExistingAttributes, attsList.get(i).name, true);
					if (values == null || values.size() == 0) {
						if (env.isInMaster() || !attJSON.containsKey(Constants.JSON_FIELD_BRANCH_NAME) || !attJSON.get(Constants.JSON_FIELD_BRANCH_NAME).equals(Constants.MASTER_BRANCH_NAME)) {
							//in branch when the attribute is from the master dont add the warning
							attJSON.put(Constants.JSON_FIELD_WARNING, attsList.get(i).name + " does not exist for feature " + featureId);
						}
					}
					else {						
						try {
							if (attributesTypes == null) {
								BaseAirlockItem feature = airlockItemsDB.get(featureId);										
								attributesTypes = featureAttributesObj.getFeatureAttributeTypes(feature.toJson(OutputJSONMode.ADMIN, context, env));
							}
						} catch (Exception e) {
							logger.severe("Fail initializing attributes type map"); 
							return arr;
						}	

						TreeSet<Jtype> attPossibleTypes = attributesTypes.get(attsList.get(i).name);

						if (attsList.get(i).type.equals(AttributeType.REGULAR)) {
							if (attPossibleTypes.size() == 1 && attPossibleTypes.contains(Jtype.ARRAY)) {
								//if the only possible type is array - return error
								attJSON.put(Constants.JSON_FIELD_WARNING, attsList.get(i).name + " is of type ARRAY."); 						
							}
						} else if (attsList.get(i).type.equals(AttributeType.ARRAY)) {
							if (!attPossibleTypes.contains(Jtype.ARRAY)) {
								//if the only possible type is array - return error
								attJSON.put(Constants.JSON_FIELD_WARNING, attsList.get(i).name + " is not of type ARRAY."); 						
							}
						}
					}
				} catch (Exception e) {
					//ignore 
				}
			}
			arr.add(attJSON);			
		}
		return arr;
	}

	public AnalyticsDataCollection duplicateForNewSeason(HashMap<String, String> oldToDuplicatedFeaturesId, ServletContext context, UUID oldSeasonId, AirlockAnalytics newAirlockAnalytics) throws JSONException {
		AnalyticsDataCollection res = new AnalyticsDataCollection(newAirlockAnalytics);

		//features on off
		for (int i=0; i<featuresOnOff.size(); i++) {
			if (oldToDuplicatedFeaturesId == null) {
				//this can be when duplicating in the same season for analytics merge with branch - in this case the ids remain the same 
				res.getFeaturesOnOff().add(featuresOnOff.get(i));
				res.featuresOnOffMap.put(featuresOnOff.get(i), true);
			}
			else {
				res.getFeaturesOnOff().add(oldToDuplicatedFeaturesId.get(featuresOnOff.get(i)));
				res.featuresOnOffMap.put(oldToDuplicatedFeaturesId.get(featuresOnOff.get(i)), true);
			}
		}

		//input fields
		@SuppressWarnings("unchecked")			
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

		Season season = seasonsDB.get(oldSeasonId.toString()); //take the inputschema from the previous season since they are identical and the new season is not in seasonsDB yet
		Map<String, String> newInputFieldsMap;
		try {
			newInputFieldsMap = Utilities.getStageForInputFields(context, season.getInputSchema().getMergedSchema());			
		} catch (GenerationException e) {
			throw new JSONException("GenerationException: " + e.getMessage());
		}

		for (int i=0; i<inputFieldsForAnalytics.size(); i++) {
			res.getInputFieldsForAnalytics().add(inputFieldsForAnalytics.get(i));
			res.getInputFieldsForAnalyticsStageMap().put(inputFieldsForAnalytics.get(i), Stage.valueOf(newInputFieldsMap.get(inputFieldsForAnalytics.get(i))));
		}

		//features configuration attributes
		for (int i=0; i<featuresConfigurationAttributesList.size(); i++) {

			String newId = null;
			if (oldToDuplicatedFeaturesId == null) {
				//this can be when duplicating in the same season for analytics merge with branch - in this case the ids remain the same 
				newId = featuresConfigurationAttributesList.get(i).id;
			}
			else {
				newId = oldToDuplicatedFeaturesId.get(featuresConfigurationAttributesList.get(i).id);
			}
			FeatureAttributesPair dupfa = featuresConfigurationAttributesList.get(i).duplicateForNewSeason(newId); //new FeatureAttributesPair(newId, featuresConfigurationAttributesList.get(i).attributes);
			res.getFeaturesConfigurationAttributesList().add(dupfa);
			res.getFeaturesConfigurationAttributesMap().put (newId, featuresConfigurationAttributesList.get(i).attributes);
			res.getFeaturesPrunedConfigurationAttributesMap().put (newId, duplicateJSONArray(featuresPrunedConfigurationAttributesMap.get(featuresConfigurationAttributesList.get(i).id)));
		}

		res.setNumberOfDevelopmentItemsToAnalytics(numberOfDevelopmentItemsToAnalytics);
		res.setNumberOfProductionItemsToAnalytics(numberOfProductionItemsToAnalytics);
		res.setNumberOfProductionInputFieldsToAnalytics(numberOfProductionInputFieldsToAnalytics);

		return res;
	}

	private JSONArray duplicateJSONArray(JSONArray src) {
		if (src == null)
			return null;

		JSONArray res = new JSONArray();
		for (int i=0; i<src.size(); i++)
			res.add(src.get(i));	
		return res;
	}

	public ValidationResults validateProductionDontChanged(JSONObject updatedDataColJSON, Map<String, BaseAirlockItem> airlockItemsDB, UUID seasonId, ServletContext context) {
		try {
			//Validate that production features are not added/deleted from featuresOnOff list		
			JSONArray updatedFeaturesOnOff = updatedDataColJSON.getJSONArray(Constants.JSON_FIELD_FEATURES_CONFIGS_FOR_ANALYTICS);
			if (!Utilities.compareIgnoreOrder(updatedFeaturesOnOff, featuresOnOff)) {
				//the featuresOnOff list was changed look for added/removed prod features
				for (int i=0; i<updatedFeaturesOnOff.size(); i++) {
					String updatedFeatureOnOff = updatedFeaturesOnOff.getString(i);
					if (!featuresOnOffMap.containsKey(updatedFeatureOnOff)) {
						//new feature - check if in the production stage
						BaseAirlockItem ai = airlockItemsDB.get(updatedFeatureOnOff);
						if (ai instanceof DataAirlockItem) {
							if (((DataAirlockItem)ai).getStage().equals(Stage.PRODUCTION)) {
								return new ValidationResults("Feature " + updatedFeatureOnOff + " is in the production stage.", Status.BAD_REQUEST);
							}
						}
						else {
							if (ai.containSubItemInProductionStage()) {
								return new ValidationResults("Feature " + updatedFeatureOnOff + " has sub-items in the production stage.", Status.BAD_REQUEST);
							}
						}
					}
				}

				for (int i=0; i<featuresOnOff.size(); i++) {
					String existingFeatureOnOff = featuresOnOff.get(i);
					if (!updatedFeaturesOnOff.contains(existingFeatureOnOff)) {
						//removed feature - check if in the production stage					
						BaseAirlockItem ai = airlockItemsDB.get(existingFeatureOnOff);
						if (ai instanceof DataAirlockItem) {
							if (((DataAirlockItem)ai).getStage().equals(Stage.PRODUCTION)) {
								return new ValidationResults("Feature " + existingFeatureOnOff + " is in the production stage.", Status.BAD_REQUEST);
							}
						}	
					}
				}
			}	


			//validate the production context fields are not added deleted from inputFields list					
			JSONArray updatedInputFields = updatedDataColJSON.getJSONArray(Constants.JSON_FIELD_INPUT_FIELDS_FOR_ANALYTICS);

			ValidationResults vr = validateInputFieldsProductionDontChanged  (updatedInputFields , context, seasonId);
			if (vr != null) 
				return vr;

			//Validate that production features are not added/deleted/AttsChanged from featuresConfigAtts list
			JSONArray updatedFeatureConfigAttArr = updatedDataColJSON.getJSONArray(Constants.JSON_FIELD_FEATURES_ATTRIBUTES_FOR_ANALYTICS);
			if (!compareConfigAttsIgnoreOrder(updatedFeatureConfigAttArr, featuresConfigurationAttributesList, featuresConfigurationAttributesMap)) {			
				//the featuresConfigurationAttributes list was changed look for added/removed/changed prod features
				for (int i=0; i<updatedFeatureConfigAttArr.size(); i++) {
					JSONObject featureConfigAttObj = updatedFeatureConfigAttArr.getJSONObject(i);
					String featureId = featureConfigAttObj.getString(Constants.JSON_FIELD_ID);
					if (!featuresConfigurationAttributesMap.containsKey(featureId)) {
						//new feature - check if in the production stage
						BaseAirlockItem ai = airlockItemsDB.get(featureId);
						if (ai instanceof DataAirlockItem) {
							if (((DataAirlockItem)ai).getStage().equals(Stage.PRODUCTION)) {
								return new ValidationResults("Feature " + featureId + " is in the production stage.", Status.BAD_REQUEST);
							}
						}					
					}
				}

				for (int i=0; i<featuresConfigurationAttributesList.size(); i++) {
					FeatureAttributesPair existingFeatureConfigAtt = featuresConfigurationAttributesList.get(i);
					if (!featureConfigAttsArrContainFeature(updatedFeatureConfigAttArr, existingFeatureConfigAtt.id)) {						
						//removed feature - check if in the production stage					
						BaseAirlockItem ai = airlockItemsDB.get(existingFeatureConfigAtt.id);
						if (ai instanceof DataAirlockItem) {
							if (((DataAirlockItem)ai).getStage().equals(Stage.PRODUCTION)) {
								return new ValidationResults("feature " + existingFeatureConfigAtt + " is in the production stage.", Status.BAD_REQUEST);
							}
						}	
					}
				}

				//no production features added or removed from featuresConfigurationAttributesList. look at the attributes lists
				for (int i=0; i<updatedFeatureConfigAttArr.size(); i++) {
					JSONObject featureConfigAttObj = updatedFeatureConfigAttArr.getJSONObject(i);
					String featureId = featureConfigAttObj.getString(Constants.JSON_FIELD_ID);

					LinkedList<AttributeTypePair> currentAttributes = featuresConfigurationAttributesMap.get(featureId);
					FeatureAttributesPair updatedFeatureAttsPair = new FeatureAttributesPair(featureId);
					updatedFeatureAttsPair.fromJson(featureConfigAttObj);
					if (!compareAttributeTypeLists (updatedFeatureAttsPair.attributes, currentAttributes)) {
						BaseAirlockItem ai = airlockItemsDB.get(featureId);
						if (ai instanceof DataAirlockItem) {
							if (((DataAirlockItem)ai).getStage().equals(Stage.PRODUCTION)) {
								return new ValidationResults("feature " + featureId + " is in the production stage.", Status.BAD_REQUEST);
							}
						}							
					}
				}
			}	
		} catch (JSONException je) {
			//should not happen since we are after validate
			logger.warning("Error during AnalyticsDataCollection.validateProductionDontChanged: " + je.getMessage());

		}
		return null;
	}

	private ValidationResults validateInputFieldsProductionDontChanged(JSONArray updatedInputFields, ServletContext context, UUID seasonId) throws JSONException {
		if (!Utilities.compareIgnoreOrder(updatedInputFields, inputFieldsForAnalytics)) {
			@SuppressWarnings("unchecked")			
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			Season season = seasonsDB.get(seasonId.toString());
			Map<String, String> newInputFieldsMap;
			try {
				newInputFieldsMap = Utilities.getStageForInputFields(context, season.getInputSchema().getMergedSchema());
			} catch (GenerationException e) {
				throw new JSONException("GenerationException: " + e.getMessage());
			}

			//the input fields list was changed look for added/removed prod fields
			for (int i=0; i<updatedInputFields.size(); i++) {
				String updatedInputField = updatedInputFields.getString(i);
				if (!inputFieldsForAnalytics.contains(updatedInputField)) {
					//new input field
					if (newInputFieldsMap.get(updatedInputField).equals(Stage.PRODUCTION.toString())) {
						return new ValidationResults("Input field '" + updatedInputField + "' is in the production stage.", Status.BAD_REQUEST);
					}
				}
			}

			for (int i=0; i<inputFieldsForAnalytics.size(); i++) {
				String existingInputField = inputFieldsForAnalytics.get(i);
				if (!updatedInputFields.contains(existingInputField)) {
					//removed input field
					if (newInputFieldsMap.get(existingInputField)!=null && newInputFieldsMap.get(existingInputField).equals(Stage.PRODUCTION.toString())) {
						return new ValidationResults("Input field '" + existingInputField + "' is in the production stage.", Status.BAD_REQUEST);
					}						
				}
			}
		}

		return null;
	}

	private boolean featureConfigAttsArrContainFeature(JSONArray featureConfigAttArr, String id) throws JSONException {
		for (int i=0; i<featureConfigAttArr.size(); i++) {
			JSONObject featureConfigAttObj = featureConfigAttArr.getJSONObject(i);
			String featureId = featureConfigAttObj.getString(Constants.JSON_FIELD_ID);

			if (featureId.equals(id))
				return true;
		}		
		return false;
	}


	//return Stage if the features existed in one of the lists, null otherwise 
	public Stage removeDeletedFeatureFromAnalytics(BaseAirlockItem deletedFeature, ServletContext context, Season season, 
			Map<String, BaseAirlockItem> airlockItemsDB) throws JSONException {

		//remove the feature and all of its sub features from analytics  
		LinkedList<UUID> subFeaturesIds =  deletedFeature.getSubFeaturesId();	

		Stage changeStage = null;

		//boolean featureInAnalytics = false;
		Stage stg = removeDeletedFeature(deletedFeature, airlockItemsDB);
		if (stg!=null)
			changeStage = stg;

		for (UUID sfId:subFeaturesIds) {
			stg = removeDeletedFeature(airlockItemsDB.get(sfId.toString()), airlockItemsDB);
			if (stg != null && changeStage!=Stage.PRODUCTION) {
				changeStage = stg;
			}
		}
		if (changeStage!=null)
			calcNumberOfItemsToAnalytics(context, season.getUniqueId(), airlockItemsDB);

		return changeStage;
	}

	//create attributes map from the given features map
	public static Map<String, TreeSet<String>> getFeatureAttributesMap (JSONObject features, JSONObject entitlements, Season season, ServletContext context) throws JSONException {
		Map<String, TreeSet<String>> featureAttributesMap = null;
		//get the maximal input data				
		try {
			String javascriptUtilities = season.getUtilities().generateUtilityCodeSectionForStageAndType(Stage.DEVELOPMENT, null, null, null, UtilityType.MAIN_UTILITY);
			JSONObject maxInputSample = season.getInputSchema().generateInputSample(Stage.DEVELOPMENT, "999999", context, InputSampleGenerationMode.MAXIMAL);
			JSONObject enStrings = season.getOriginalStrings().toEnStringsJSON (Stage.DEVELOPMENT).getJSONObject(Constants.JSON_FIELD_STRINGS);
			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());
			FeatureAttributes featureAttributesObj = new FeatureAttributes (maxInputSample.toString(), javascriptUtilities, enStrings.toString(), env);
			//TreeMap<String,String> arraySpec = new TreeMap<String,String>();//this way only the array in pos 0 is returned
			featureAttributesMap = featureAttributesObj.getAttributes(features, entitlements, true, null);
			return featureAttributesMap;
		} catch (Exception e) {
			throw new JSONException("Failed to generate data sample: " + e.getMessage());	
		}			
	}

	//create attributes map from the given features map
	public static JSONObject getFeatureAttributeTypeJSON (JSONObject features, JSONObject entitlements, Season season, ServletContext context, String featureId) throws JSONException {
		JSONObject res = new JSONObject();
		try {
			Map<String, TreeSet<String>> featureAttributesMap = getFeatureAttributesMap (features, entitlements, season, context);
			TreeSet<String> featureAttributes = featureAttributesMap.get(featureId);
			TreeSet<String> uniquefeatureAttributes = new TreeSet<String>();
			for (String attribute:featureAttributes) {
				if (attribute.contains("[")) {
					String fixedAtt = attribute;	
					int startBracketPos = fixedAtt.indexOf("[");
					while (startBracketPos!=-1) {
						//this is an array - remove the number between the square brackets and leave only []
						int endBracketPos = fixedAtt.indexOf("]", startBracketPos);

						if (startBracketPos+1 == endBracketPos) {
							//empty bracket - leave as is
							//uniquefeatureAttributes.add(attribute);
						} else {
							//there is a number in the bracket - remove it
							String tmpFixedAtt = fixedAtt.substring(0, startBracketPos+1);
							tmpFixedAtt += fixedAtt.substring(endBracketPos);
							//uniquefeatureAttributes.add(fixedAtt);
							fixedAtt = tmpFixedAtt;
						}
						startBracketPos = fixedAtt.indexOf("[", endBracketPos);						
					}
					uniquefeatureAttributes.add(fixedAtt);
				} else {
					uniquefeatureAttributes.add(attribute);
				}
			}

			JSONArray attTypeArr = new JSONArray();
			for (String attribute : uniquefeatureAttributes) {
				JSONObject attObj = new JSONObject();
				attObj.put(Constants.JSON_FIELD_NAME, attribute);
				attObj.put(Constants.JSON_FEATURE_FIELD_TYPE, attribute.contains("[")?AttributeType.ARRAY.toString():AttributeType.REGULAR.toString());
				attTypeArr.add(attObj);
			}

			res.put(Constants.JSON_FIELD_ATTRIBUTES, attTypeArr);

		} catch (Exception e) {
			throw new JSONException("Failed to generate data sample: " + e.getMessage());	
		}			

		return res;
	}


	//return Stage if the features existed in one of the lists null otherwise.
	//The Stage is the stage of the change 
	private Stage removeDeletedFeature(BaseAirlockItem deletedFeature, Map<String, BaseAirlockItem> airlockItemsDB) throws JSONException {

		//boolean found = false;
		Stage changeStage = null;

		String deletedFeatureId = deletedFeature.getUniqueId().toString();

		//look for this feature in the featuresOnOff
		if (featuresOnOffMap.containsKey(deletedFeatureId)) {
			featuresOnOff.remove(deletedFeatureId);
			featuresOnOffMap.remove(deletedFeatureId);
			if (changeStage!=Stage.PRODUCTION) {
				changeStage = BaseAirlockItem.isProductionFeature(deletedFeature, airlockItemsDB)? Stage.PRODUCTION:Stage.DEVELOPMENT;				
			}
		}

		//look for this feature in the featuresConfigurationAttributes		
		if (featuresConfigurationAttributesMap.containsKey(deletedFeatureId)) {			
			removeFeatureFromFeaturesConfigAttList(deletedFeatureId);
			featuresConfigurationAttributesMap.remove(deletedFeatureId);
			featuresPrunedConfigurationAttributesMap.remove(deletedFeatureId);
			if (changeStage!=Stage.PRODUCTION) {
				changeStage = BaseAirlockItem.isProductionFeature(deletedFeature, airlockItemsDB)? Stage.PRODUCTION:Stage.DEVELOPMENT;
				/*if (deletedFeature instanceof DataAirlockItem) {				
					changeStage = ((DataAirlockItem)deletedFeature).getStage();
				}*/

			}
		}

		return changeStage;
	}

	private void removeFeatureFromFeaturesConfigAttList (String featureId) {
		for (int i=0; i<featuresConfigurationAttributesList.size(); i++) {
			if (featuresConfigurationAttributesList.get(i).id.equals(featureId)) {
				featuresConfigurationAttributesList.remove(i);
				break;
			}
		}
	}

	public void removeFeaturAttributesList(BaseAirlockItem feature, Map<String, BaseAirlockItem> airlockItemsDB) {

		
		String featureId = feature.getUniqueId().toString();		

		if (featuresConfigurationAttributesMap.containsKey(featureId)) {			
			removeFeatureFromFeaturesConfigAttList(featureId);
			featuresConfigurationAttributesMap.remove(featureId);
			int numOfAtts = featuresPrunedConfigurationAttributesMap.get(featureId).length();
			 
			featuresPrunedConfigurationAttributesMap.remove(featureId);
		
			numberOfDevelopmentItemsToAnalytics=numberOfDevelopmentItemsToAnalytics-numOfAtts;
			if (BaseAirlockItem.isProductionFeature(feature, airlockItemsDB))
				numberOfProductionItemsToAnalytics=numberOfProductionItemsToAnalytics-numOfAtts;
		}
		

	}
	
	public boolean isFeatureExistInAnalytics(String featureId) {
		if (featuresOnOffMap.containsKey(featureId)) {
			return true;
		}

		if (featuresConfigurationAttributesMap.containsKey(featureId)) {
			return true;
		}

		return false;		
	}

	//return null if the feature and its sub features are not reported in the analytics
	//If the feature or/and its sub features are reported in analytics - the output is an informative message  
	public String deletedFeatureInUseByAnalytics(BaseAirlockItem featureToDel, Map<String, BaseAirlockItem> airlockItemsDB, 
			ServletContext context, Season season, UserInfo userInfo, Environment env) throws JSONException {
		boolean delFeatureReportedInAnalytics = isFeatureExistInAnalytics(featureToDel.getUniqueId().toString());
		if (delFeatureReportedInAnalytics) {
			return "The deleted feature is reported in analytics.";		
		}

		//check for all deleted subFeatures if they are in use by analytics
		LinkedList<UUID> subFeaturesIds =  featureToDel.getSubFeaturesId();				
		LinkedList<String> delSubFeaturesInAnalyticsNames =  new LinkedList<String>();
		for (UUID sfId:subFeaturesIds) {
			if (isFeatureExistInAnalytics(sfId.toString())) {
				delSubFeaturesInAnalyticsNames.add(airlockItemsDB.get(sfId.toString()).getNameSpaceDotName());
			}
		}

		if (delSubFeaturesInAnalyticsNames.size()>0) {
			return "The following sunFeatures are reported in analytics: " + Utilities.StringsListToString(delSubFeaturesInAnalyticsNames);			
		}

		//check if the deleted feature or one of its sub features consists of a configuration attribute that is in use by the analytics

		//build copy of the features map and delete the "featureToDel" from it 
		Map<String, BaseAirlockItem> airlockItemsMapCopy = null;
		try {
			if (env.isInMaster())
				//airlockItemsMapCopy = Utilities.getPurchaseItemsMapCopyForSeason(context, season.getUniqueId().toString());
				airlockItemsMapCopy = Utilities.getAirlockItemsMapCopy (context, season.getUniqueId().toString());
			else
				//airlockItemsMapCopy = Utilities.getPurchaseItemsDB(env.getBranchId(), context);
				airlockItemsMapCopy = Utilities.getAirlockItemsDB(env.getBranchId(), context);

		} catch (Exception e) {
			throw new JSONException("INTERNAL_SERVER_ERROR:" + e.getMessage());
		}

		BaseAirlockItem copyOfFeatureToDel = airlockItemsMapCopy.get(featureToDel.getUniqueId().toString());
		BaseAirlockItem root = Utilities.findItemsRoot(airlockItemsMapCopy.get(copyOfFeatureToDel.getUniqueId().toString()), airlockItemsMapCopy);

		if (copyOfFeatureToDel.getParent() == null) {
			root.removeAirlockItem(copyOfFeatureToDel.getUniqueId());
		}
		else {
			BaseAirlockItem parent = airlockItemsMapCopy.get(copyOfFeatureToDel.getParent().toString());
			parent.removeAirlockItem(copyOfFeatureToDel.getUniqueId());
			parent.setLastModified(new Date());
		}

		//remove deleted feature and sub features from features map
		copyOfFeatureToDel.removeFromAirlockItemsDB(airlockItemsMapCopy,context,userInfo);

		//		Environment env = new Environment();
		//		env.setServerVersion(season.getServerVersion()); 

		//create attributes map from the new features map
		Map<String, TreeSet<String>> featureAttributesMap = getFeatureAttributesMap (root.toJson(OutputJSONMode.ADMIN, context, env), null, season, context);

		LinkedList<String> missingConfigAttributes =  new LinkedList<String>();
		for (FeatureAttributesPair featureAttPair:featuresConfigurationAttributesList) {
			String faetureId = featureAttPair.id;
			LinkedList<AttributeTypePair> attributes = featureAttPair.attributes;

			TreeSet<String> featuresAttribures = featureAttributesMap.get(faetureId);
			if (attributes!=null) {
				for (int i=0; i<attributes.size(); i++) {
					if (attributes.get(i).type.equals(AttributeType.CUSTOM))
						continue;

					String att = attributes.get(i).name;
					if (!featuresAttribures.contains(att)) {
						missingConfigAttributes.add(att);
					}
				}				
			}			
		}

		if (missingConfigAttributes.size()>0) {
			return "The following configuration attributes are reported in analytics: " + Utilities.StringsListToString(missingConfigAttributes);
		}

		return null;
	}


	//return null if the updated feature removes an attribute that is reported in analytics - the output is an informative message  
	public String updatedFeatureInUseByAnalytics(String updatedFeatureId, JSONObject updatedFeatureJSON, Map<String, BaseAirlockItem> airlockItemsDB, 
			ServletContext context, Season season, Environment env,Boolean isProdChange) throws JSONException, MergeException {

		//build copy of the features map and update the given feature in it 
		Map<String, BaseAirlockItem> airlockItemsMapCopy = null;
		if (env.isInMaster()) {
			try {
				airlockItemsMapCopy = Utilities.getAirlockItemsMapCopyForSeason(context, season.getUniqueId().toString());
			} catch (IOException e) {
				throw new JSONException("INTERNAL_SERVER_ERROR:" + e.getMessage());
			}
		}
		else {
			airlockItemsMapCopy = Utilities.getAirlockItemsDB(env.getBranchId(), context);
		}

		BaseAirlockItem root = Utilities.findItemsRoot(airlockItemsMapCopy.get(updatedFeatureId), airlockItemsMapCopy);		
		BaseAirlockItem copyOfFeatureToUpdate = airlockItemsMapCopy.get(updatedFeatureId);

		Branch branch = null;
		if (!env.isInMaster()) {
			@SuppressWarnings("unchecked")
			Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);

			branch = branchesDB.get(env.getBranchId());
		}

		Map<String, Stage> updatedBranchesMap = new HashMap<String, Stage> (); //not is use here 
		copyOfFeatureToUpdate.updateAirlockItem(updatedFeatureJSON, airlockItemsMapCopy, root, env, branch,isProdChange, context, updatedBranchesMap);

		//create attributes map from the new features map
		Map<String, TreeSet<String>> featureAttributesMap = getFeatureAttributesMap (root.toJson(OutputJSONMode.ADMIN, context, env), null, season, context);

		LinkedList<String> missingConfigAttributes =  new LinkedList<String>();
		for (FeatureAttributesPair featureAttPair:featuresConfigurationAttributesList) {
			String faetureId = featureAttPair.id;
			LinkedList<AttributeTypePair> attributes = featureAttPair.attributes;

			TreeSet<String> featuresAttribures = featureAttributesMap.get(faetureId);
			if (attributes!=null) {
				for (int i=0; i<attributes.size(); i++) {
					if (attributes.get(i).type.equals(AttributeType.CUSTOM))
						continue;

					String att = attributes.get(i).name;
					if (!featuresAttribures.contains(att)) {
						missingConfigAttributes.add(att);
					}
				}				
			}			
		}

		if (missingConfigAttributes.size()>0) {
			return "The following configuration attributes are reported in analytics: " + Utilities.StringsListToString(missingConfigAttributes);
		}

		return null;
	}

	public JSONObject toDisplayJson(ServletContext context, Season season, Environment env, Map<String, BaseAirlockItem> airlockItemsDB) throws JSONException {
		
		Branch branch = null;
		if (!env.isInMaster()) {
			@SuppressWarnings("unchecked")
			Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);
			branch = branchesDB.get(env.getBranchId());
		}
		
		JSONObject res = new JSONObject();
		
		//input fields
		JSONArray inputFieldsArray = new JSONArray();
		for (String inputField:inputFieldsForAnalytics) {
			JSONObject inputFieldJSON = new JSONObject();
			inputFieldJSON.put(Constants.JSON_FIELD_NAME, inputField);
			if (env.isInMaster()) {
				inputFieldJSON.put(Constants.JSON_FIELD_BRANCH_NAME, Constants.MASTER_BRANCH_NAME);
			}
			else {
				if (season.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection().getInputFieldsForAnalyticsStageMap().containsKey(inputField)) {
					inputFieldJSON.put(Constants.JSON_FIELD_BRANCH_NAME, Constants.MASTER_BRANCH_NAME);
				}
				else {
					inputFieldJSON.put(Constants.JSON_FIELD_BRANCH_NAME, branch.getName());
				}
			}
			inputFieldsArray.add(inputFieldJSON);
		}
		
		res.put(Constants.JSON_FIELD_INPUT_FIELDS_FOR_ANALYTICS, inputFieldsArray);

		Map<String, TreeSet<String>> featureAttributesMap = null;
		if (featuresConfigurationAttributesMap.size()>0) {
			// build the att map from the merged features tree. 
			BaseAirlockItem featuresRootOfMergedTree = airlockItemsDB.get(season.getRoot().getUniqueId().toString());
			BaseAirlockItem entitlementsRootOfMergedTree = airlockItemsDB.get(season.getEntitlementsRoot().getUniqueId().toString());
			featureAttributesMap = getFeatureAttributesMap (featuresRootOfMergedTree.toJson(OutputJSONMode.ADMIN, context, env), entitlementsRootOfMergedTree.toJson(OutputJSONMode.ADMIN, context, env), season, context);
		}

		FeatureAttributes featureAttributesObj = null;
		if ((featuresConfigurationAttributesList!=null && featuresConfigurationAttributesList.size()>0) || 
				(featuresOnOff!=null && featuresOnOff.size()>0)) {
			try {
				String javascriptUtilities = season.getUtilities().generateUtilityCodeSectionForStageAndType(Stage.DEVELOPMENT, null, null, null, UtilityType.MAIN_UTILITY);
				JSONObject maxInputSample = season.getInputSchema().generateInputSample(Stage.DEVELOPMENT, "999999", context, InputSampleGenerationMode.MAXIMAL);
				JSONObject enStrings = season.getOriginalStrings().toEnStringsJSON (Stage.DEVELOPMENT).getJSONObject(Constants.JSON_FIELD_STRINGS);
				env.setServerVersion(season.getServerVersion());
	
				featureAttributesObj = new FeatureAttributes (maxInputSample.toString(), javascriptUtilities, enStrings.toString(), env);
			} catch (Exception e) {
				throw new JSONException("Failed to generate attribute types map: " + e.getMessage());			
			}
		}
				
		
		JSONArray analyticsByFeatureNamesArr = new JSONArray(); 
		//map between feature id and its data
		HashMap <String, JSONObject> featuresDataMap = new HashMap<String, JSONObject>();
		//first iterate all the features  (not configurationRules) in featuresOnOff
		for (int i=0; i<featuresOnOff.size(); i++) {			
			BaseAirlockItem alItem = airlockItemsDB.get(featuresOnOff.get(i));
			if (alItem.getType().equals(Type.FEATURE) || alItem.getType().equals(Type.ENTITLEMENT) || alItem.getType().equals(Type.PURCHASE_OPTIONS)) {
				JSONObject featureData = new JSONObject();
				FeatureItem feature = (FeatureItem) alItem;
				String featureId = feature.getUniqueId().toString();
				featureData.put(Constants.JSON_FIELD_ID, featureId);
				featureData.put(Constants.JSON_FIELD_NAME, feature.getNameSpaceDotName());
				featureData.put(Constants.JSON_FIELD_SEND_TO_ANALYTICS, true);
				if (env.isInMaster()) {
					featureData.put(Constants.JSON_FIELD_BRANCH_NAME, Constants.MASTER_BRANCH_NAME);
				}
				else {
					if (season.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection().getFeaturesOnOffMap().containsKey(featureId)) {
						featureData.put(Constants.JSON_FIELD_BRANCH_NAME, Constants.MASTER_BRANCH_NAME);
					}
					else {
						featureData.put(Constants.JSON_FIELD_BRANCH_NAME, branch.getName());
					}
				}
				
				if (featuresConfigurationAttributesMap.containsKey(featuresOnOff.get(i))) {
					featureData.put(Constants.JSON_FIELD_ATTRIBUTES, attributesListToJSONArray(featuresConfigurationAttributesMap.get(featuresOnOff.get(i)), featuresOnOff.get(i), featureAttributesMap, context, env, featureAttributesObj, season, true, airlockItemsDB));
				}

				analyticsByFeatureNamesArr.add(featureData);
				featuresDataMap.put(featuresOnOff.get(i), featureData);
			}
		}

		//second iterate all the features in featuresConfigurationAttributesList
		for (int i=0; i<featuresConfigurationAttributesList.size(); i++) {			
			String id = featuresConfigurationAttributesList.get(i).id;
			if (!featuresDataMap.containsKey(id)) { //add only if wasnt already listed in featuresOnOff
				BaseAirlockItem alItem = airlockItemsDB.get(id);

				JSONObject featureData = new JSONObject();
				FeatureItem feature = (FeatureItem) alItem; //will succeed since this is only features list
				featureData.put(Constants.JSON_FIELD_ID, feature.getUniqueId().toString());
				featureData.put(Constants.JSON_FIELD_NAME, feature.getNameSpaceDotName());
				featureData.put(Constants.JSON_FIELD_SEND_TO_ANALYTICS, false);
				if (featuresConfigurationAttributesMap.containsKey(id)) {
					featureData.put(Constants.JSON_FIELD_ATTRIBUTES, attributesListToJSONArray(featuresConfigurationAttributesMap.get(id), id, featureAttributesMap, context, env, featureAttributesObj, season, true, airlockItemsDB));
				}

				analyticsByFeatureNamesArr.add(featureData);
				featuresDataMap.put(id, featureData);				
			}
		}

		//third iterate all the configurationRules and ordering rules (not features) in featuresOnOff
		for (int j=0; j<featuresOnOff.size(); j++) {			
			BaseAirlockItem alItem = airlockItemsDB.get(featuresOnOff.get(j));
			if (alItem.getType().equals(Type.CONFIGURATION_RULE)) {
				FeatureItem feature = ((ConfigurationRuleItem)alItem).getParentFeature(airlockItemsDB);
				if (!featuresDataMap.containsKey(feature.getUniqueId().toString())) {
					JSONObject featureData = new JSONObject();

					featureData.put(Constants.JSON_FIELD_ID, feature.getUniqueId().toString());
					featureData.put(Constants.JSON_FIELD_NAME, feature.getNameSpaceDotName());
					featureData.put(Constants.JSON_FIELD_SEND_TO_ANALYTICS, false);
					JSONArray configRules = new JSONArray();
					JSONObject configRuleJSON = getConfigRuleJsonForDisplay(alItem, season, branch);					
					configRules.add(configRuleJSON);
					featureData.put(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES, configRules);
					analyticsByFeatureNamesArr.add(featureData);
					featuresDataMap.put(feature.getUniqueId().toString(), featureData);
				}
				else {
					//add configRule to an existing feature data	
					JSONObject featureData = featuresDataMap.get(feature.getUniqueId().toString());
					if (featureData.containsKey(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES)) {
						JSONArray configRules  = featureData.getJSONArray(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES);
						JSONObject configRuleJSON = getConfigRuleJsonForDisplay(alItem, season, branch);
						configRules.add(configRuleJSON);
						featureData.put(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES, configRules);
					}
					else {
						JSONArray configRules = new JSONArray();
						JSONObject configRuleJSON = getConfigRuleJsonForDisplay(alItem, season, branch);
						configRules.add(configRuleJSON);
						featureData.put(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES, configRules);
					}
				}
			}
			else if (alItem.getType().equals(Type.ORDERING_RULE)) {
				BaseAirlockItem parent = ((OrderingRuleItem)alItem).getParentFeatureOrMTX(airlockItemsDB);
				if (!featuresDataMap.containsKey(parent.getUniqueId().toString())) {
					JSONObject featureData = new JSONObject();

					featureData.put(Constants.JSON_FIELD_ID, parent.getUniqueId().toString());
					featureData.put(Constants.JSON_FIELD_NAME, Branch.getItemBranchName(parent));
					featureData.put(Constants.JSON_FIELD_SEND_TO_ANALYTICS, false);
					JSONArray orderingRules = new JSONArray();
					JSONObject orderingRuleJSON = getConfigRuleJsonForDisplay(alItem, season, branch);					
					orderingRules.add(orderingRuleJSON);
					featureData.put(Constants.JSON_FEATURE_FIELD_ORDERING_RULES, orderingRules);
					analyticsByFeatureNamesArr.add(featureData);
					featuresDataMap.put(parent.getUniqueId().toString(), featureData);
				}
				else {
					//add configRule to an existing feature data	
					JSONObject featureData = featuresDataMap.get(parent.getUniqueId().toString());
					if (featureData.containsKey(Constants.JSON_FEATURE_FIELD_ORDERING_RULES)) {
						JSONArray orderingRules  = featureData.getJSONArray(Constants.JSON_FEATURE_FIELD_ORDERING_RULES);
						JSONObject orderingRuleJSON = getConfigRuleJsonForDisplay(alItem, season, branch);
						orderingRules.add(orderingRuleJSON);
						featureData.put(Constants.JSON_FEATURE_FIELD_ORDERING_RULES, orderingRules);
					}
					else {
						JSONArray orderingRules = new JSONArray();
						JSONObject orderingRuleJSON = getConfigRuleJsonForDisplay(alItem, season, branch);
						orderingRules.add(orderingRuleJSON);
						featureData.put(Constants.JSON_FEATURE_FIELD_ORDERING_RULES, orderingRules);
					}
				}
			}
		}


		res.put(Constants.JSON_FIELD_ANALYTICS_BY_FEATURE_NAMES, analyticsByFeatureNamesArr);
		res.put(Constants.JSON_FIELD_DEVELOPMENT_ANALYTICS_COUNT, numberOfDevelopmentItemsToAnalytics);
		res.put(Constants.JSON_FIELD_PRODUCTION_ANALYTICS_COUNT, numberOfProductionItemsToAnalytics);
		return res;
	}

	private JSONObject getConfigRuleJsonForDisplay(BaseAirlockItem alItem, Season season, Branch branch) throws JSONException {
		JSONObject configRuleJSON = new JSONObject();
		configRuleJSON.put(Constants.JSON_FIELD_NAME, ((ConfigurationRuleItem)alItem).getNameSpaceDotName());
								
		if (season.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection().getFeaturesOnOffMap().containsKey(alItem.getUniqueId().toString())) {
			configRuleJSON.put(Constants.JSON_FIELD_BRANCH_NAME, Constants.MASTER_BRANCH_NAME);
		}
		else {
			configRuleJSON.put(Constants.JSON_FIELD_BRANCH_NAME, branch.getName());
		}
		return configRuleJSON;
	}
	
	public ValidationResults ValidateFeatureOnOffAddition(BaseAirlockItem feature, Environment env, Season season, ServletContext context) throws MergeException {
		//at this stage we know the feature exists

		String featureId = feature.getUniqueId().toString();
		//check for duplication
		if (featuresOnOffMap.containsKey(featureId)) {
			return new ValidationResults("Feature " + featureId + " was already sent to analytics.", Status.BAD_REQUEST);
		}

		if (!env.isInMaster()) {
			if (feature.getBranchStatus().equals(BranchStatus.NONE)) {
				return new ValidationResults("Feature " + featureId + " is not checked out. First check out the feature.", Status.BAD_REQUEST);
			}
		}
		
		Map<String, BaseAirlockItem> airlockItemsDB = env.getAirlockItemsDB();
		if (airlockItemsDB == null) {			
			airlockItemsDB = (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);
		}
		
		boolean isProdFeature = BaseAirlockItem.isProductionFeature(feature, airlockItemsDB); 

		if (isProdFeature) {
			if ((feature instanceof FeatureItem || feature instanceof MutualExclusionGroupItem) &&
					feature.getOrderingRuleItems()!=null && !feature.getOrderingRuleItems().isEmpty()) {
				if (numberOfProductionItemsToAnalytics + 2  > season.getAnalytics().getAnalyticsQuota()) {
					return new ValidationResults("The maximum number of items in production to send to analytics was exceeded. The maximum number allowed is " + season.getAnalytics().getAnalyticsQuota() + ". ", Status.BAD_REQUEST);					
				}
			}
			else if (numberOfProductionItemsToAnalytics + 1  > season.getAnalytics().getAnalyticsQuota()) {
				return new ValidationResults("The maximum number of items in production to send to analytics was exceeded. The maximum number allowed is " + season.getAnalytics().getAnalyticsQuota() + ". ", Status.BAD_REQUEST);
			}

		}		

		if (isProdFeature) {
			List<Experiment> seasonsProdExperiments = season.getExperimentsForSeason(context, true);
			if (seasonsProdExperiments!=null && !seasonsProdExperiments.isEmpty()) {
				for (Experiment exp:seasonsProdExperiments) {
					if (!env.isInMaster()) {
						@SuppressWarnings("unchecked")
						Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);
						Branch branch = branchesDB.get(env.getBranchId());	
						Stage branchStageInExp = exp.getSingleBranchStageInExperment(branch.getName());
						if (branchStageInExp==null || branchStageInExp.equals(Stage.DEVELOPMENT))
							continue; //dont calculate production analytics items if branch is attached to variant in dev or if feature is in branch that is not part of the experiment
					}
					int updatedProdCount = exp.getAnalyticsProductionCounter(context, null, null, null) + 1;
					int experimentAnalyticsQuota = exp.getQuota(context);
					if (updatedProdCount > experimentAnalyticsQuota) {
						return new ValidationResults("Failed to send item to analytics. The maximum number of items in production to send to analytics for experiment " + exp.getName() + " was exceeded. The maximum number allowed is " + experimentAnalyticsQuota + ". Sending the item to analytics increased the number to " + updatedProdCount, Status.BAD_REQUEST);						
					}
				}	
			}
		}

		return null;
	}

	//called after ValidateFeatureOnOffAddition - i know it is legal

	public void addFeatureOnOff(BaseAirlockItem feature, Map<String, BaseAirlockItem> airlockItemsDB) {
		//at this stage we know the feature exists
		String featureId = feature.getUniqueId().toString();

		featuresOnOff.add(featureId);
		featuresOnOffMap.put(featureId, true);		

		boolean isProdFeature = BaseAirlockItem.isProductionFeature(feature, airlockItemsDB); 

		boolean isFeatureWithOrderingRules = isFeatureWithOrderingRules(feature);
		

		numberOfDevelopmentItemsToAnalytics++;
		if (isFeatureWithOrderingRules)
			numberOfDevelopmentItemsToAnalytics++;
		
		if (isProdFeature) {
			numberOfProductionItemsToAnalytics++;
			if (isFeatureWithOrderingRules)
				numberOfProductionItemsToAnalytics++;
		}
	}

	public void removeFeatureOnOff(BaseAirlockItem feature, Map<String, BaseAirlockItem> airlockItemsDB) {
		//at this stage we know the feature exists

		String featureId = feature.getUniqueId().toString();		

		featuresOnOff.remove(featureId);
		featuresOnOffMap.remove(featureId);
		
		boolean isFeatureWithOrderingRules = isFeatureWithOrderingRules(feature);

		numberOfDevelopmentItemsToAnalytics--;

		if (isFeatureWithOrderingRules)
			numberOfDevelopmentItemsToAnalytics--;
		
		if (BaseAirlockItem.isProductionFeature(feature, airlockItemsDB)) {

			numberOfProductionItemsToAnalytics--;
			if (isFeatureWithOrderingRules) {
				numberOfProductionItemsToAnalytics--;
			}
		}
	}


	public ValidationResults validateFeatureOnOffRemoval(BaseAirlockItem feature, Environment env, Season season) {
		//at this stage we know the feature exists

		String featureId = feature.getUniqueId().toString();
		//check for duplication
		if (!featuresOnOffMap.containsKey(featureId)) {
			return new ValidationResults("Feature " + featureId + " was not sent to analytics.", Status.BAD_REQUEST);
		}

		if (!env.isInMaster()) {
			if (feature.getBranchStatus().equals(BranchStatus.NONE)) {

				return new ValidationResults("Feature " + featureId + " is not checked out. First check out the feature.", Status.BAD_REQUEST);

			}

			//if the feature is checked out but is reported to analytics in the master - refuse (user cannot remove analytics data 
			//from master in branch)
			if (season.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection().getFeaturesOnOffMap().containsKey(featureId)) {
				return new ValidationResults("The status of the item is being sent to analytics from the master branch. To stop sending item status to analytics, first go to the master and stop sending to analytics. Then, return to the branch and stop sending to analytics.", Status.BAD_REQUEST);
			}


		}

		return null;
	}

	public ValidationResults validateInputFieldsUpdate(JSONArray inputFieldsJSONArray, ServletContext context, Season season, boolean productionChangeAllowed, Environment env, Boolean skipCountersChecking) throws MergeException {
		ValidationResults vr  = null;
		try {
			vr = validateInputFieldsForAnalytics(inputFieldsJSONArray, context, season);
			if (vr!=null)
				return vr;

			//in branch - cannot delete inputField that is reported in analytics
			if (!env.isInMaster()) {
				LinkedList<String> masterInputFields = season.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection().getInputFieldsForAnalytics();
				for (String masterInputField:masterInputFields) {
					boolean found = false;
					if (inputFieldsJSONArray!=null) { 
						for (int k=0; k<inputFieldsJSONArray.length(); k++) {  
							if (masterInputField.equals(inputFieldsJSONArray.get(k))) {
								found = true;
								break;
							}
						}
					}
					if (!found) {
						return new ValidationResults("You must report all input fields that are reported in the master, in addition to the input fields that you want to report in the branch.", Status.BAD_REQUEST);
					}
				}
			}

			//vr will be null if no production change, not null if there was a production change
			vr = validateInputFieldsProductionDontChanged  (inputFieldsJSONArray , context, season.getUniqueId());
			if (vr!=null) {
				if (productionChangeAllowed) {				
					vr = new ValidationResults("", Status.OK); //mark as prod changed
				}
				else {
					//error - prod change is fobidden
					String err = "Unable to update context fields for analytics. Only a user with the Administrator or Product Lead role can update context fields in the production stage for analytics.";;
					return new ValidationResults(err, Status.UNAUTHORIZED); //mark as prod changed
				}
			}

			if (!skipCountersChecking) {
				//validate prod count + count prod fields
				if (vr!=null) { //this means production change occurs			
	
					int origProdFieldsCount = 0;
					int newProdFieldsCount = 0;
	
					//TODO: recreating the inputFields map - should be created once for prodcount and prod change check
					Map<String, String> newInputFieldsMap;
					try {
						newInputFieldsMap = Utilities.getStageForInputFields(context, season.getInputSchema().getMergedSchema());
					} catch (GenerationException e) {
						throw new JSONException("GenerationException: " + e.getMessage());
					}
	
					for (int i=0; i<inputFieldsJSONArray.size(); i++) {
						String updatedInputField = inputFieldsJSONArray.getString(i);
						if (newInputFieldsMap.get(updatedInputField).equals(Stage.PRODUCTION.toString())) {
							newProdFieldsCount++;
						}
					}
	
					for (int i=0; i<inputFieldsForAnalytics.size(); i++) {
						String updatedInputField = inputFieldsForAnalytics.get(i);
						if (newInputFieldsMap.get(updatedInputField).equals(Stage.PRODUCTION.toString())) {
							origProdFieldsCount++;
						}
					}
	
					if (numberOfProductionItemsToAnalytics - origProdFieldsCount + newProdFieldsCount > season.getAnalytics().getAnalyticsQuota()) {
						return new ValidationResults("The maximum number of items in production to send to analytics was exceeded. The maximum number allowed is " + season.getAnalytics().getAnalyticsQuota() + ". " + numberOfProductionItemsToAnalytics + " were previously selected, " +  origProdFieldsCount + " of them were context fields. You attempted to select " + newProdFieldsCount + " context fields.", Status.BAD_REQUEST);					
					}
				
					List<Experiment> seasonsProdExperiments = season.getExperimentsForSeason(context, true);
					if (seasonsProdExperiments!=null && !seasonsProdExperiments.isEmpty()) {
						for (Experiment exp:seasonsProdExperiments) {
							int updatedProdCount = exp.simulateProdCounterUponUpdateInputFields(newInputFieldsMap, inputFieldsJSONArray, context);;
							int experimentAnalyticsQuota = exp.getQuota(context);
							if (updatedProdCount > experimentAnalyticsQuota) {
								return new ValidationResults("Failed to send input fields to analytics. The maximum number of items in production to send to analytics for experiment " + exp.getName() + " was exceeded. The maximum number allowed is " + experimentAnalyticsQuota + ". Sending context fields to analytics increased the number to " + updatedProdCount, Status.BAD_REQUEST);						
							}
						}	
					}					
				}
			}
			
		} catch (GenerationException ge) {
			return new ValidationResults(ge.getMessage(), Status.BAD_REQUEST);
		} catch (JSONException je) {
			return new ValidationResults(je.getMessage(), Status.BAD_REQUEST);
		}

		return vr;
	}	

	public void updateInputFields(JSONArray inputFieldsJSONArray, ServletContext context, Season season, boolean productionChangeAllowed, Environment env) throws JSONException {		
		int currentNumberOfInputFields = inputFieldsForAnalytics.size();
		int updatedNumberOfInputFields = inputFieldsJSONArray.size();

		int origProdFieldsCount = 0;
		int newProdFieldsCount = 0;
		
		Map<String, String> newInputFieldsMap;
		try {
			newInputFieldsMap = Utilities.getStageForInputFields(context, season.getInputSchema().getMergedSchema());
		} catch (GenerationException e) {
			throw new JSONException("GenerationException: " + e.getMessage());
		}

		for (int i=0; i<inputFieldsJSONArray.size(); i++) {
			String updatedInputField = inputFieldsJSONArray.getString(i);
			if (newInputFieldsMap.get(updatedInputField).equals(Stage.PRODUCTION.toString())) {
					newProdFieldsCount++;
			}
		}
		
		for (int i=0; i<inputFieldsForAnalytics.size(); i++) {
			String updatedInputField = inputFieldsForAnalytics.get(i);
			if (newInputFieldsMap.get(updatedInputField).equals(Stage.PRODUCTION.toString())) {
					origProdFieldsCount++;
			}
		}
		
		
		numberOfProductionItemsToAnalytics = numberOfProductionItemsToAnalytics - origProdFieldsCount + newProdFieldsCount;
		numberOfProductionInputFieldsToAnalytics = newProdFieldsCount;
		
		setInputFieldsForAnalyticsFromJson(inputFieldsJSONArray, context, season.getUniqueId(), env);

		numberOfDevelopmentItemsToAnalytics = numberOfDevelopmentItemsToAnalytics - currentNumberOfInputFields + updatedNumberOfInputFields;				
	}	

	public void updateFeatureAttributs(FeatureItem feaute, JSONArray attributesJSONArray, ServletContext context, Season season, 
			Map<String, BaseAirlockItem> airlockItemsDB, Map<String, TreeSet<String>> featureAttributesMap, Environment env) throws JSONException {

		JSONObject featureConfigAttObj = new JSONObject();

		featureConfigAttObj.put(Constants.JSON_FIELD_ID, feaute.getUniqueId().toString());
		featureConfigAttObj.put(Constants.JSON_FIELD_ATTRIBUTES, attributesJSONArray);
		String featureId = featureConfigAttObj.getString(Constants.JSON_FIELD_ID);				

		if (attributesJSONArray.size() == 0) { //after validation - i know that no master feature's attributes were removed
			//remove the feature from list				
			removeFeatureFromFeaturesConfigAttList(featureId);
			featuresConfigurationAttributesMap.remove(featureId);
			featuresPrunedConfigurationAttributesMap.remove(featureId);
			calcNumberOfItemsToAnalytics(context, season.getUniqueId(), airlockItemsDB);
			return;
		}

		int currentNumberOfFeatureAttributes=0;
		if (featuresPrunedConfigurationAttributesMap.get(featureId) != null) {
			currentNumberOfFeatureAttributes = featuresPrunedConfigurationAttributesMap.get(featureId).size();
		}

		FeatureAttributesPair featureAtts = new FeatureAttributesPair(featureId);
		featureAtts.fromJson(featureConfigAttObj);
		int updatedNumberOfFeatureAttributes = calcFeaturesPrunedAttributes (featureId, featureAtts.attributes, featureAttributesMap).size();

		boolean isProdChange = BaseAirlockItem.isProductionFeature(feaute, airlockItemsDB);

		setSingleFeatureAttributesFromJson(featureConfigAttObj, featureAttributesMap, season, env);

		numberOfDevelopmentItemsToAnalytics = numberOfDevelopmentItemsToAnalytics - currentNumberOfFeatureAttributes + updatedNumberOfFeatureAttributes;

		if (isProdChange)
			numberOfProductionItemsToAnalytics = numberOfProductionItemsToAnalytics - currentNumberOfFeatureAttributes + updatedNumberOfFeatureAttributes;


	}


	public ValidationResults validateFeatureAttributsUpdate(FeatureItem feature, JSONArray attributesJSONArray, ServletContext context,
			Season season, Map<String, BaseAirlockItem> airlockItemsDB, Map<String, TreeSet<String>> featureAttributesMap, Environment env, boolean skipCountersChecking) throws MergeException {
		try {			

			String featureId = feature.getUniqueId().toString();				
			
			
			if (!env.isInMaster()) {
				if (feature.getBranchStatus().equals(BranchStatus.NONE)) {
					AnalyticsDataCollection masterDataCollection = season.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection();
					if (!masterDataCollection.getFeaturesConfigurationAttributesMap().containsKey(feature.getUniqueId().toString())  
							 || (!compareAttributeTypeArrayToList(attributesJSONArray, masterDataCollection.getFeaturesConfigurationAttributesMap().get(featureId)))) {
						//If feature is not checked out or new you cannot add it or its attributes to analyze from branch
						return new ValidationResults("Feature " + feature.getUniqueId().toString() + " is not checked out. First check out the feature.", Status.BAD_REQUEST);
					}
				}
			}
			
			JSONObject featureConfigAttObj = new JSONObject();

			featureConfigAttObj.put(Constants.JSON_FIELD_ID, feature.getUniqueId().toString());
			featureConfigAttObj.put(Constants.JSON_FIELD_ATTRIBUTES, attributesJSONArray);
			
			if (attributesJSONArray.size() == 0) {
				if (!env.isInMaster()) {
					if (season.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection().getFeaturesConfigurationAttributesList().size()!=0) {
						return new ValidationResults("You must report all attributes that are reported in the master, in addition to the attributes that you want to report in the branch.", Status.BAD_REQUEST);
					}
				}
				//TODO: validate that no master features were removed				
				return null;				
			}

			FeatureAttributesPair tmp = new FeatureAttributesPair(featureId);
			ValidationResults vr = tmp.validateFeatureAttributesPair(featureConfigAttObj);
			if (vr != null)
				return vr;

			vr = tmp.validateAttributesArr(featureId, attributesJSONArray, featureAttributesMap, airlockItemsDB);
			if (vr != null)
				return vr;

			
			//in branch - cannot delete feature attributes that is reported in analytics
			if (!env.isInMaster()) {
				HashMap<String, LinkedList<AttributeTypePair>> masterFeatureAttributesMap = season.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection().getFeaturesConfigurationAttributesMap();
				LinkedList<AttributeTypePair> masterFeatureAttributes = masterFeatureAttributesMap.get(featureId);
				if (masterFeatureAttributes!=null) {
					for (AttributeTypePair masterAttribute:masterFeatureAttributes) {
						boolean found = false;						
						for (int k=0; k<attributesJSONArray.length(); k++) { 
							JSONObject branchAttribute =  attributesJSONArray.getJSONObject(k); //i know that is jsonObj since after validate
							if (masterAttribute.name.equals(branchAttribute.getString(Constants.JSON_FIELD_NAME))) {
								found = true;
								break;
							}
						}
						if (!found) {
							return new ValidationResults("You must report all attributes that are reported in the master, in addition to the attributes that you want to report in the branch.", Status.BAD_REQUEST);
						}
					}
				}
			}
			if(!skipCountersChecking) {
				int currentNumberOfFeatureAttributes=0;
				if (featuresPrunedConfigurationAttributesMap.get(featureId) != null) {
					currentNumberOfFeatureAttributes = featuresPrunedConfigurationAttributesMap.get(featureId).size();
				}
	
				FeatureAttributesPair featureAtts = new FeatureAttributesPair(featureId);
				featureAtts.fromJson(featureConfigAttObj);
				JSONArray prunedAttArray = calcFeaturesPrunedAttributes (featureId, featureAtts.attributes, featureAttributesMap);
				int updatedNumberOfFeatureAttributes = prunedAttArray.size();
	
				boolean isProdChange = BaseAirlockItem.isProductionFeature(feature, airlockItemsDB);
				if (isProdChange&& numberOfProductionItemsToAnalytics - currentNumberOfFeatureAttributes + updatedNumberOfFeatureAttributes > season.getAnalytics().getAnalyticsQuota()) {
					return new ValidationResults("The maximum number of items in production to send to analytics was exceeded. The maximum number allowed is " + season.getAnalytics().getAnalyticsQuota() + ". " + numberOfProductionItemsToAnalytics + " were previously selected, and you attempted to select " +  (numberOfProductionItemsToAnalytics - currentNumberOfFeatureAttributes + updatedNumberOfFeatureAttributes), Status.BAD_REQUEST);
				}
				
				if (isProdChange) {
					List<Experiment> seasonsProdExperiments = season.getExperimentsForSeason(context, true);
					if (seasonsProdExperiments!=null && !seasonsProdExperiments.isEmpty()) {
						for (Experiment exp:seasonsProdExperiments) {
							if (!env.isInMaster()) {
								@SuppressWarnings("unchecked")
								Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);
								Branch branch = branchesDB.get(env.getBranchId());	
								Stage branchStageInExp = exp.getSingleBranchStageInExperment(branch.getName());
								if (branchStageInExp==null || branchStageInExp.equals(Stage.DEVELOPMENT))
									continue; //dont calculate production analytics items if branch is attached to variant in dev or if feature is in branch that is not part of the experiment
							}

							int updatedProdCount = exp.simulateProdCounterUponUpdateAttributesListForAnalytics(feature, prunedAttArray, context);;
							int experimentAnalyticsQuota = exp.getQuota(context);
							if (updatedProdCount > experimentAnalyticsQuota) {
								return new ValidationResults("Fail to report attributes to analytics. The maximum number of items in production to send to analytics for experiment " + exp.getName() + " was exceeded. The maximum number allowed is " + experimentAnalyticsQuota + ". Reporting attributes increased the number to " + updatedProdCount, Status.BAD_REQUEST);						
							}
						}	
					}	
				}
			}	
		}  catch (JSONException je) {
			return new ValidationResults(je.getMessage(), Status.BAD_REQUEST);
		}

		return null;
	}


	private boolean compareAttributeTypeArrayToList(JSONArray attributesJSONArray, LinkedList<AttributeTypePair> attributeJsonList) throws JSONException {
		if (attributesJSONArray == null && attributeJsonList == null)
			return true;

		if (attributesJSONArray == null || attributeJsonList == null)
			return false;

		if (attributesJSONArray.size() != attributeJsonList.size())
			return false;

		for (int i=0; i<attributeJsonList.size(); i++) {
			AttributeTypePair curAttType =  attributeJsonList.get(i);
			boolean found = false;
			for (int j=0; j<attributesJSONArray.size(); j++) {
				JSONObject updatedAttType =  attributesJSONArray.getJSONObject(j);
				String updatedName = updatedAttType.getString(Constants.JSON_FIELD_NAME); 
				String updatedType = updatedAttType.getString(Constants.JSON_FEATURE_FIELD_TYPE);
				if (curAttType.name.equals(updatedName)) {
					if (curAttType.type.toString().equals(updatedType)) {
						found = true;
						break;
					}
					else {
						//same name diff type
						return false;
					}
				}
			}
			if (!found)
				return false;
		}
		return true;
	}

	private boolean isFeatureWithOrderingRules(BaseAirlockItem item) {
		if (item instanceof FeatureItem || item instanceof MutualExclusionGroupItem) {			
			if (item.getOrderingRuleItems()!=null && item.getOrderingRuleItems().size()>0) {
				return true;
			}
		}
		return false;
	}
	
	public void calcNumberOfItemsToAnalytics(ServletContext context, UUID seasonId, Map<String, BaseAirlockItem> airlockItemsDB) throws JSONException {
		//Development items
		int featuresOnOffSize = 0;
		for (String featureId:featuresOnOff) {
			featuresOnOffSize++;
			BaseAirlockItem ai = airlockItemsDB.get(featureId);
			if (isFeatureWithOrderingRules(ai)) {
				featuresOnOffSize++; //if feature is reported to analytics and has ordering rules another event is sent (the order of its sub features)
			}
		}
		
		numberOfDevelopmentItemsToAnalytics = 0;
		numberOfDevelopmentItemsToAnalytics = featuresOnOffSize + inputFieldsForAnalytics.size();
		Set<String> featuresAtts = featuresPrunedConfigurationAttributesMap.keySet();
		for (String featureId: featuresAtts) {
			numberOfDevelopmentItemsToAnalytics += featuresPrunedConfigurationAttributesMap.get(featureId).size();
		}

		numberOfProductionItemsToAnalytics = 0;
		numberOfProductionInputFieldsToAnalytics = 0;
		for (String featureId:featuresOnOff) {
			BaseAirlockItem f = airlockItemsDB.get(featureId);
 
			if (BaseAirlockItem.isProductionFeature(f, airlockItemsDB)) {

				numberOfProductionItemsToAnalytics++;
				if (isFeatureWithOrderingRules(f)) {
					numberOfProductionItemsToAnalytics++; //if feature is reported to analytics and has ordering rules another event is sent (the order of its sub features)					
				}
			}
		}

		for (FeatureAttributesPair featureAtts:featuresConfigurationAttributesList) {
			BaseAirlockItem f = airlockItemsDB.get(featureAtts.id);
			if (BaseAirlockItem.isProductionFeature(f, airlockItemsDB))
				numberOfProductionItemsToAnalytics += featuresPrunedConfigurationAttributesMap.get(featureAtts.id).size();				
		}

		if (inputFieldsForAnalytics!=null && inputFieldsForAnalytics.size()>0) {
			@SuppressWarnings("unchecked")			
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			Season season = seasonsDB.get(seasonId.toString());
			Map<String, String> inputFieldsMap;
			try {
				inputFieldsMap = Utilities.getStageForInputFields(context, season.getInputSchema().getMergedSchema());
			} catch (GenerationException e) {
				throw new JSONException("GenerationException: " + e.getMessage());
			}

			for (String inputField:inputFieldsForAnalytics) {
				if (inputFieldsMap.get(inputField)!=null && inputFieldsMap.get(inputField).equals(Stage.PRODUCTION.toString())) {
					numberOfProductionItemsToAnalytics++;
					numberOfProductionInputFieldsToAnalytics++;
				}			
			}
		}

	}


	//for a feature that is reported in analytics if would be updated from dev to prod - what would the prod count be.
	public int simulateProdCounterUponFeatureMoveToProd(String featureId, boolean orderingRulesExist) throws JSONException {
		int prodCount = numberOfProductionItemsToAnalytics; 

		if (featuresOnOffMap.containsKey(featureId)) {
			prodCount++;
			if (orderingRulesExist)
				prodCount++;
		}

		if (featuresPrunedConfigurationAttributesMap.containsKey(featureId.toString())) {
			prodCount = prodCount + featuresPrunedConfigurationAttributesMap.get(featureId).size();
		}

		return prodCount;
	}
	
	//for a feature that is reported in analytics if would be updated from dev to prod - what would the prod count be.
	public int simulateProdCounterUponOrderingRuleAddition(BaseAirlockItem parent, Map<String, BaseAirlockItem> airlockItemsDB) throws JSONException {
		int prodCount = numberOfProductionItemsToAnalytics; 

		if (featuresOnOffMap.containsKey(parent.getUniqueId().toString())) {			
			if (parent instanceof FeatureItem) {
				FeatureItem fi = (FeatureItem) parent;
				if (fi.getOrderingRuleItems()==null || fi.getOrderingRuleItems().size() == 0) { //the added ordering rule is the new one					
					if (BaseAirlockItem.isProductionFeature(fi, airlockItemsDB)) {
						prodCount++;
					}
				}
			}
		}		

		return prodCount;
	}

	public void addInputField(String inputField, Stage stage) {
		inputFieldsForAnalytics.add(inputField);
		inputFieldsForAnalyticsStageMap.put(inputField, stage);

		numberOfDevelopmentItemsToAnalytics++;
		if (stage.equals(Stage.PRODUCTION)) {
			numberOfProductionItemsToAnalytics++;
			numberOfProductionInputFieldsToAnalytics++;
		}
	}
	
	private static boolean isFeatureContainsAttribute(String featureId, String attName,  HashMap<String, LinkedList<AttributeTypePair>> featuresAttributesMap) {
		LinkedList<AttributeTypePair> attList = featuresAttributesMap.get(featureId);
		if (attList == null || attList.size() == 0)
			return false;

		for (AttributeTypePair existingAtt:attList) {
			if (attName.equals(existingAtt.name))
				return true;
		}

		return false;
	}
		
}
