package com.ibm.airlock.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response.Status;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Constants.Action;
import com.ibm.airlock.Constants.BranchStatus;
import com.ibm.airlock.Constants.OutputJSONMode;
import com.ibm.airlock.Constants.Stage;
import com.ibm.airlock.Strings;
import com.ibm.airlock.admin.authentication.UserInfo;
import com.ibm.airlock.admin.MergeBranch.MergeException;
import com.ibm.airlock.admin.analytics.AnalyticsDataCollection;
import com.ibm.airlock.admin.translations.OriginalString;
import com.ibm.airlock.engine.Environment;
import com.ibm.airlock.engine.VerifyRule;
import com.ibm.airlock.engine.Version;


public class OrderingRuleItem extends ConfigurationRuleItem {
	
	private JSONObject configurtionJson;
	private String configurationStringForEval;

	public String getConfigurationStringForEval() {
		return configurationStringForEval;
	}

	private void setConfigurationStringForEval(String configurationStringForEval) {
		this.configurationStringForEval = configurationStringForEval;
	}

	public JSONObject getConfigurtionJson() {
		return configurtionJson;
	}

	private void setConfigurtionJson(JSONObject configurtionJson) {
		this.configurtionJson = configurtionJson;
	}

	//In ordering rule the configuration is a string that is a legal json. "{\"id1\":\"val1\", \"id2\":\"val2\"}" 
	protected BaseAirlockItem newInstance() {
		return new OrderingRuleItem();
	}	
	
	private static String convertConfigJsonToConfigStringForEval(JSONObject configurationJson) throws JSONException {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		Set<String> ids = configurationJson.keySet();
		boolean first  = true;
		for (String id : ids)
		{
			if (!first) {
				sb.append(",");				
			}
			String val = configurationJson.getString(id);
			sb.append("\"");
			sb.append(id);
			sb.append("\":");
			sb.append(val);
			first = false;
		}
		sb.append("}");
		return sb.toString();
	}

	public OrderingRuleItem() {
		type = Type.ORDERING_RULE;
		orderingRuleItems = new LinkedList<BaseAirlockItem>();
		configurationRuleItems = null;
	}	
	
	public void fromJSON(JSONObject input, Map<String, BaseAirlockItem> airlockItemsDB, UUID parent, Environment env) throws JSONException {
		super.fromJSON(input, airlockItemsDB, parent, env);

		if (input.containsKey(Constants.JSON_FEATURE_FIELD_CONFIGURATION) && input.get(Constants.JSON_FEATURE_FIELD_CONFIGURATION)!=null) {
			configuration = input.getString(Constants.JSON_FEATURE_FIELD_CONFIGURATION);
			
			try {
				configurtionJson = new JSONObject(configuration);
			} catch (JSONException je) {
				//this can be caused by old version of configuration (that is string and not json
				//in this case we should convert the configuration to the new format
				if (env.getServiceState()!=null && env.getServiceState().equals(Constants.ServiceState.INITIALIZING)) {
					configuration = convertConfigurationFromStringToJson(configuration, airlockItemsDB);
					configurtionJson = new JSONObject(configuration);
				}
				else {
					throw je;
				}
			}
			configurationStringForEval = convertConfigJsonToConfigStringForEval(configurtionJson);
		}
	}

	private String convertConfigurationFromStringToJson(String configuration, Map<String, BaseAirlockItem> airlockItemsDB) throws JSONException {
		if (configuration == null)
			return null;
		
		if (configuration.isEmpty()) 
			return new JSONObject().toString();
		
		
		//create list of the sub feature that may be listed in the configuration
		Map<String,Integer> subFeaturePositionMap = new HashMap<String,Integer>();
		BaseAirlockItem parentObj = airlockItemsDB.get(parent.toString());
		if (parentObj.getFeaturesItems()!=null) {
			for (int i=0; i<parentObj.getFeaturesItems().size(); ++i) {
				String subFeatureId = parentObj.getFeaturesItems().get(i).getUniqueId().toString();
				int pos = configuration.indexOf(subFeatureId);
				if (pos != -1) {
					subFeaturePositionMap.put(subFeatureId, pos);
				}
			}
		}
		
		//sort the ids by position
		Set<Entry<String, Integer>> set = subFeaturePositionMap.entrySet();
        List<Entry<String, Integer>> sortedList = new ArrayList<Entry<String, Integer>>(set);
        Collections.sort(sortedList, new Comparator<Map.Entry<String, Integer>>()
        {
            public int compare( Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2 )
            {
                return (o2.getValue()).compareTo( o1.getValue() )*(-1);
            }
        } );
        
        JSONObject res = new JSONObject();
        for (int i=0; i<sortedList.size(); ++i) {
        		String id = sortedList.get(i).getKey();
        		int idPos = sortedList.get(i).getValue();
        		int valStartPos = configuration.indexOf(":", idPos);
        		int valEndPos = 0;
        		
        		if(i < sortedList.size() - 1){
        			int nextIdPos = sortedList.get(i+1).getValue();
        			valEndPos = configuration.substring(0,nextIdPos).lastIndexOf(",");
            }else{
            		valEndPos = configuration.lastIndexOf("}");
            }
            String val = configuration.substring(valStartPos + 1, valEndPos /*- valStartPos - 1*/);
            res.put(id, val);
        }
		logger.info("converting ordering rule configuration from " + configuration + " to " + res.toString());
		return res.toString();
	}

	@Override
	public BaseAirlockItem duplicate(String minVersion, UUID newSeasonId, UUID parentId, Map<String, BaseAirlockItem> airlockItemsDB, 
			HashMap<String, String> oldToDuplicatedFeaturesId, ServletContext context, boolean duplicateSubFeatures, boolean createNewId,
			ValidationCache tester) {
		OrderingRuleItem res = new OrderingRuleItem();

		if (createNewId) {
			res.setUniqueId(UUID.randomUUID());
		} else {
			res.setUniqueId(uniqueId);			
		}
		oldToDuplicatedFeaturesId.put(uniqueId.toString(), res.getUniqueId().toString());

		if (minVersion!=null) {
			res.setMinAppVersion(minVersion);
		} 
		else { 
			res.setMinAppVersion(minAppVersion);
		}

		if (newSeasonId!=null) {
			res.setSeasonId(newSeasonId);
		} 
		else {
			res.setSeasonId(seasonId);
		}

		res.setParent(parentId);
		res.setConfiguration(configuration);
		res.setConfigurationStringForEval(configurationStringForEval);
		try {
			res.setConfigurtionJson(configurtionJson == null?null:(JSONObject)Utilities.cloneJson(configurtionJson, true));
		} catch (JSONException je) {
			//should not occur
			logger.severe("Error while duplicating ordering rule: " + je.getMessage());
		}
		res.setCreationDate(creationDate);
		res.setCreator(creator);
		res.setDescription(description);
		res.setEnabled(enabled);
		res.setNoCachedResults(noCachedResults);
		res.setInternalUserGroups(internalUserGroups == null ? null:internalUserGroups.clone());
		res.setLastModified(lastModified);
		res.setName(name);
		res.setOwner(owner);
		res.setNamespace(namespace);
		res.setRolloutPercentage(rolloutPercentage);
		res.setRolloutPercentageBitmap(rolloutPercentageBitmap); //if bitmap exists it should remain - even in new seasons.
		res.setBranchStatus(branchStatus);
		res.setBranchFeatureParentName(branchFeatureParentName);
		res.setBranchFeaturesItems(Utilities.cloneStringsList(branchFeaturesItems));
		res.setBranchConfigurationRuleItems(Utilities.cloneStringsList(branchConfigurationRuleItems));
		res.setBranchOrderingRuleItems(Utilities.cloneStringsList(branchOrderingRuleItems));
		res.setBranchEntitlementItems(Utilities.cloneStringsList(branchEntitlementItems));
		res.setBranchPurchaseOptionsItems(Utilities.cloneStringsList(branchPurchaseOptionsItems));

		res.setRule(rule == null?null:Rule.duplicteForNextSeason(rule)); 
		res.setStage(stage);

		if (oldToDuplicatedFeaturesId!=null && createNewId) {
			//replace the old subFeatureIds in the configuration with the new ones
			String newConfig = replaceIdsInConfiguration(tester, context, oldToDuplicatedFeaturesId, configuration, rule, seasonId.toString(), stage, minAppVersion);
			
			res.setConfiguration(newConfig);
		}
		else {
			res.setConfiguration(configuration);
				
		}
		
		for (int i=0;i<orderingRuleItems.size(); i++) {
			BaseAirlockItem newAirlockItem = orderingRuleItems.get(i).duplicate(minVersion, newSeasonId, res.getUniqueId(), airlockItemsDB, oldToDuplicatedFeaturesId, context, duplicateSubFeatures, createNewId, tester);
			res.getOrderingRuleItems().add(newAirlockItem);					
			if (airlockItemsDB!=null)
				airlockItemsDB.put(newAirlockItem.getUniqueId().toString(), newAirlockItem);						
		}

		if (airlockItemsDB!=null)
			airlockItemsDB.put(res.getUniqueId().toString(), res);	

		return res;		
	}

	public static String replaceIdsInConfiguration(ValidationCache tester, ServletContext context, 
			Map<String, String> oldToDuplicatedFeaturesId, String configuration, Rule rule, String seasonId, Stage stage, String minAppVersion) {
		String newConfig = configuration;
		String ruleString = rule.getRuleString() == null ? "": rule.getRuleString();
		
		@SuppressWarnings("unchecked")
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

		Season curSeason = seasonsDB.get(seasonId); //look for the info of the prev season since in duplication is the same
		
		ValidationCache.Info info;
		try {
			info = tester.getInfo(context, curSeason, stage, minAppVersion);
			JSONObject configJsonObj = VerifyRule.fullConfigurationEvaluation(ruleString, configuration, info.minimalInvoker, info.maximalInvoker);
			
			if (configJsonObj!=null) {
				Set<Map.Entry<String, Object>> configEntries = configJsonObj.entrySet();
				if (configEntries != null) { 				
					for (Map.Entry<String, Object> entry : configEntries) {
						String oldSubFeatureId = entry.getKey();
						String newSubFeatureId = oldToDuplicatedFeaturesId.get(oldSubFeatureId);
						if (newSubFeatureId!=null) //new feature id will be null when copying ordering rule without its parent
							newConfig = newConfig.replace(oldSubFeatureId, newSubFeatureId);											
					}
				}					
			}	
			
		} catch (JSONException | GenerationException | ValidationException e) {
			logger.severe("Error during ordering rule duplication: " + e.getMessage());
		}
		
		return newConfig;		
	}

	//return null if valid, ValidationResults otherwise
	//does not change the feature! 
	//for features cannot call super update since some steps are unique to feature even with general fields
	public ValidationResults doValidateFeatureJSON(JSONObject featureObj, ServletContext context, String seasonId, LinkedList<String> addedSubFeatures, LinkedList<String> missingSubFeatures, InternalUserGroups userGroups, 
			HashMap<UUID, Integer> existingFeaturesInUpdate, String parent, HashMap<String, JSONObject> updatedFeaturesMap, UserInfo userInfo,
			ValidationCache tester,
			Map<String, BaseAirlockItem> airlockItemsDB, Environment env,List<OriginalString> copiedStrings) {

		//call data item validation and then the ordering rule validation
		ValidationResults superRes = super.doValidateAirlockDataItemJSON(featureObj, context, seasonId, addedSubFeatures, missingSubFeatures, userGroups, existingFeaturesInUpdate, parent, updatedFeaturesMap, userInfo, tester, airlockItemsDB, env,copiedStrings);

		if (superRes!=null)
			return superRes;

		@SuppressWarnings("unchecked")
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

		Season curSeason = seasonsDB.get(seasonId);
		if (curSeason == null) {
			return new ValidationResults("The season of the given ordering rule does not exist.", Status.BAD_REQUEST);
		}

		if (env.getVersion().i < Version.v4_5.i) {
			return new ValidationResults("The current version range does not support ordering rules.", Status.BAD_REQUEST); 

		}

		try {				
			Action action = Action.ADD;
			if (featureObj.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && featureObj.get(Constants.JSON_FIELD_UNIQUE_ID)!=null) {
				//if JSON contains uniqueId - update an existing feature otherwise create a new feature
				action = Action.UPDATE;			
			}

			BaseAirlockItem parentObj = null;
			if (parent!=null) {
				parentObj = airlockItemsDB.get(parent);
				if (parentObj == null) {
					return new ValidationResults("Parent feature " + parent + " not found", Status.BAD_REQUEST);
				}
				
				if (parentObj.getType().equals(Type.ROOT)) {
					return new ValidationResults("Cannot add ordering rule under the root.", Status.BAD_REQUEST);					
				}

				if (parentObj.getType().equals(Type.CONFIGURATION_RULE)) {
					return new ValidationResults("Cannot add ordering rule under configuration rule.", Status.BAD_REQUEST);					
				}

				if (parentObj.getType().equals(Type.CONFIG_MUTUAL_EXCLUSION_GROUP)) {
					return new ValidationResults("Cannot add ordering rule under a configuration mutual exclusion group.", Status.BAD_REQUEST);					
				}
				
				if (parentObj.getType().equals(Type.ENTITLEMENT_MUTUAL_EXCLUSION_GROUP)) {
					return new ValidationResults("Cannot add an ordering rule under an entitlemens mutual exclusion group.", Status.BAD_REQUEST);					
				}
				
				if (parentObj.getType().equals(Type.PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP)) {
					return new ValidationResults("Cannot add an ordering rule under a purchase options mutual exclusion group.", Status.BAD_REQUEST);					
				}
				
				if (parentObj.getType().equals(Type.ENTITLEMENT)) {
					return new ValidationResults("Cannot add an ordering rule under an entitlemen.", Status.BAD_REQUEST);					
				}
				
				if (parentObj.getType().equals(Type.PURCHASE_OPTIONS)) {
					return new ValidationResults("Cannot add an ordering rulep under a purchase options item.", Status.BAD_REQUEST);					
				}
				
			}

			//configuration - mandatory field
			if (!featureObj.containsKey(Constants.JSON_FEATURE_FIELD_CONFIGURATION) || featureObj.get(Constants.JSON_FEATURE_FIELD_CONFIGURATION) == null || featureObj.getString(Constants.JSON_FEATURE_FIELD_CONFIGURATION).isEmpty()) 
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_CONFIGURATION), Status.BAD_REQUEST);				

			String configurationStr = featureObj.getString(Constants.JSON_FEATURE_FIELD_CONFIGURATION);
			String tmpConfigurationStringForEval = "";
			try {
				JSONObject tmpConfigurtionJson = new JSONObject(configurationStr);
				tmpConfigurationStringForEval = convertConfigJsonToConfigStringForEval(tmpConfigurtionJson);
			} catch (JSONException je) {
				return new ValidationResults("Illegal ordering rule configuration: '" + configuration + "'. Should be legal json object.", Status.BAD_REQUEST);
			}
			
			String stageStr = featureObj.getString(Constants.JSON_FEATURE_FIELD_STAGE);
			Stage stage = Utilities.strToStage(stageStr);
			if (stage == null) {
				return new ValidationResults("Illegal ordering rule stage: '" + stageStr + "'", Status.BAD_REQUEST);
			}				

			//minAppVersion
			if (!featureObj.containsKey(Constants.JSON_FEATURE_FIELD_MIN_APP_VER) || featureObj.get(Constants.JSON_FEATURE_FIELD_MIN_APP_VER) == null || featureObj.getString(Constants.JSON_FEATURE_FIELD_MIN_APP_VER).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_MIN_APP_VER), Status.BAD_REQUEST);					
			}			

			String minAppVer = featureObj.getString(Constants.JSON_FEATURE_FIELD_MIN_APP_VER);

			featureObj.getJSONObject(Constants.JSON_FEATURE_FIELD_RULE); //we know that exists since dataAirlockItem validation is done
			Rule tmpRule = new Rule();
			tmpRule.fromJSON(featureObj.getJSONObject(Constants.JSON_FEATURE_FIELD_RULE));

			if (tmpRule.getForce() == true && !validAdmin(userInfo)) {
				return new ValidationResults("Only a user with the Administrator role can create or update an ordering rule without validation.", Status.BAD_REQUEST); 
			}

			JSONObject configJsonObj = null;

			//skip configuration validation if rule.force is true 
			if (!tmpRule.getForce()) {				
				String ruleString = "";
				if (featureObj.containsKey(Constants.JSON_FEATURE_FIELD_RULE) && featureObj.get(Constants.JSON_FEATURE_FIELD_RULE)!=null) {
					JSONObject ruleObj = featureObj.getJSONObject(Constants.JSON_FEATURE_FIELD_RULE);
					if (ruleObj.containsKey(Constants.JSON_RULE_FIELD_RULE_STR) && ruleObj.get(Constants.JSON_RULE_FIELD_RULE_STR)!=null)
						ruleString = ruleObj.getString(Constants.JSON_RULE_FIELD_RULE_STR);				
				}

				try {
					ValidationCache.Info info = tester.getInfo(context, curSeason, stage, minAppVer);
					configJsonObj = VerifyRule.fullConfigurationEvaluation(ruleString, tmpConfigurationStringForEval, info.minimalInvoker, info.maximalInvoker);
				} catch (ValidationException e) {
					return new ValidationResults("Validation error: " + e.getMessage(), Status.BAD_REQUEST);		
				}
			}

			//skip configuration validation rule.force is true 
			if (/*(action == Action.ADD || parent == null) &&*/ !tmpRule.getForce()) {
				BaseAirlockItem parentFeature = getFeatureParent(parent, airlockItemsDB);
				if (configJsonObj!=null && parentFeature!=null) {
					Set<Map.Entry<String, Object>> configEntries = configJsonObj.entrySet();
/*					if ((configEntries == null || configEntries.size() == 0) && (parentFeature.getFeaturesItems().size() > 0)) {
						//configuration cannot be empty if the parent feature has sub features 
						return new ValidationResults("The configuration cannot be empty when the parent has sub features" , Status.BAD_REQUEST);
					}*/	

					//build temp map between feature and its sub features
					HashMap<String , BaseAirlockItem> subFeaturesMap = new  HashMap<String , BaseAirlockItem>();
					for (BaseAirlockItem subFeature:parentFeature.getFeaturesItems()) {
						if (subFeaturesMap.containsKey(subFeature.getUniqueId().toString())) {
							return new ValidationResults("The " + subFeature.getUniqueId().toString() + " cannot be specified more than once in the configuration." , Status.BAD_REQUEST);
						}
						subFeaturesMap.put(subFeature.getUniqueId().toString(), subFeature);
					}
					
					for (Map.Entry<String, Object> entry : configEntries) {
						String key = entry.getKey();
						if (!subFeaturesMap.containsKey(key)) {
							return new ValidationResults(key + " is not an id of a subfeature." , Status.BAD_REQUEST);
						}
						
						BaseAirlockItem subFeature = subFeaturesMap.get(key);
						
						if (entry.getValue() instanceof String) {
							try {
								double d = Double.valueOf(((String)entry.getValue()));
							} catch (NumberFormatException nfe) {
								return new ValidationResults("Illegal weight for subfeature  " + subFeature.getNameSpaceDotName() + ". The weight cannot be evaluated to a decimal number." , Status.BAD_REQUEST);
							}
						} else if (entry.getValue() instanceof Integer){
							try {						
								int intVal = ((Integer)(entry.getValue())).intValue();
								double d = ((double)intVal);
							} catch (ClassCastException cce) {
								return new ValidationResults("Illegal weight for subfeature  " + subFeature.getNameSpaceDotName() + ". The weight cannot be evaluated to a decimal number." , Status.BAD_REQUEST);
							}
						}
						else if (entry.getValue() instanceof Double){
							try {						
								double d = ((Double)(entry.getValue()));
							} catch (ClassCastException cce) {
								return new ValidationResults("Illegal weight for subfeature  " + subFeature.getNameSpaceDotName() + ". The weight cannot be evaluated to a decimal number." , Status.BAD_REQUEST);
							}
						}
						else {
							return new ValidationResults("Illegal weight for subfeature  " + subFeature.getNameSpaceDotName() + ". The weight cannot be evaluated to a decimal number." , Status.BAD_REQUEST);
						}
					}
					
				}			
			}

			if (action == Action.ADD) {
				if (parentObj!=null) {
					AnalyticsDataCollection analyticsData = curSeason.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection();
					
					int updatedProdCount = analyticsData.simulateProdCounterUponOrderingRuleAddition(parentObj, airlockItemsDB);
					if (updatedProdCount > curSeason.getAnalytics().getAnalyticsQuota()) {
						int currentNumOfProdAnaItems = analyticsData.getNumberOfProductionItemsToAnalytics();
						return new ValidationResults("Failed adding ordering rule: The maximum number of items in production to send to analytics was exceeded. The maximum number allowed is " + curSeason.getAnalytics().getAnalyticsQuota() + ". " + currentNumOfProdAnaItems + " were previously selected, and releasing the ordering rule to production increased the number to " + updatedProdCount, Status.BAD_REQUEST);						
					}
				}
			}
			else { //(action == Action.UPDATE) 
				ValidationResults res = validateMasterFeatureNotChangedFromBranch(featureObj, airlockItemsDB, env);
				if (res!=null)
					return res;
			}			
		} catch (JSONException jsne) {
			return new ValidationResults(jsne.getMessage(), Status.BAD_REQUEST);
		}
		catch (ClassCastException cce) {
			return new ValidationResults("Illegal ordering rule JSON: " + cce.getMessage(), Status.BAD_REQUEST);

		} catch (GenerationException e) {
			return new ValidationResults("Failed to generate a data sample to validate the configuration:" + e.getMessage(), Status.BAD_REQUEST);		
		}

		return null;
	}
	
	private BaseAirlockItem getFeatureParent(String parentId, Map<String, BaseAirlockItem> airlockItemsDB) {
		if  (parentId == null) {
			//parent can be null in update feature - in this case take the parent id from the item itself
			parentId = this.parent.toString();			
		}
			
		
		BaseAirlockItem parentItem = airlockItemsDB.get(parentId);
		if (parentItem == null)
			return null;
		
		if (!(parentItem instanceof OrderingRuleItem) && !(parentItem instanceof OrderingRuleMutualExclusionGroupItem))
			return parentItem;
		
		return getFeatureParent(parentItem.getParent().toString(), airlockItemsDB); 		
	}
	
	public JSONObject toJson(OutputJSONMode mode, ServletContext context, Environment env, UserInfo userInfo) throws JSONException {
		if (mode == OutputJSONMode.DEFAULTS) //dont add configuration rule to defaults file
			return null;

		JSONObject res = super.toJson(mode, context, env, userInfo);
		if (res == null) {
			// this can only happen in runtime_production mode when the orderingRule is in development stage
			return null;
		}

		@SuppressWarnings("unchecked")
		Map<String, BaseAirlockItem> masterAirlockItemsDB = (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);

		if (branchStatus.equals(BranchStatus.CHECKED_OUT) && 
				(mode.equals(OutputJSONMode.RUNTIME_DEVELOPMENT) || mode.equals(OutputJSONMode.RUNTIME_PRODUCTION))) {
			if (stage.equals(Stage.DEVELOPMENT) || (((DataAirlockItem)(masterAirlockItemsDB.get(uniqueId.toString()))).getStage().equals(Stage.PRODUCTION))) {
				res = toDeltaJson(res, context, mode, env);
				return res;
			}
			else {
				//in case the item is checked out. Dev in master and prod in branch. Write the whole item (not only delta) and set its 
				//branchStatus to NEW in runtime files
				res.put("branchStatus", "NEW");
			}
		}
				
		if (mode.equals(OutputJSONMode.RUNTIME_DEVELOPMENT) || mode.equals(OutputJSONMode.RUNTIME_PRODUCTION)) {
			String subFeaturesWeightConfig = getSubFeaturesWeightConfigForRunTimeFiles(context, env);
			res.put(Constants.JSON_FEATURE_FIELD_CONFIGURATION, subFeaturesWeightConfig);
			return res;
		}
				
		res.put(Constants.JSON_FEATURE_FIELD_CONFIGURATION, configuration);				

		return res;
	}
	
	private String getSubFeaturesWeightConfigForRunTimeFiles(ServletContext context, Environment env) throws JSONException {
		String subFeaturesWeightConfigForRunTimeFiles = configurationStringForEval;		
		
		try {		
			Map<String, BaseAirlockItem> airlockItemsDB = env.getAirlockItemsDB();
			if (airlockItemsDB == null) {			
				airlockItemsDB = Utilities.getAirlockItemsDB(env.getBranchId(), context);
			}		
												
			if (configurtionJson!=null) {
				Set<Map.Entry<String, Object>> configEntries = configurtionJson.entrySet();
				if (configEntries != null) { 				
					for (Map.Entry<String, Object> entry : configEntries) {
						String subFeatureId = entry.getKey();
						BaseAirlockItem subFeature = airlockItemsDB.get(subFeatureId);
						String namespaceDotName = Branch.getItemBranchName(subFeature); //ns.name or mx.id
						
						subFeaturesWeightConfigForRunTimeFiles = subFeaturesWeightConfigForRunTimeFiles.replace(subFeatureId, namespaceDotName);						
					}
				}
					
			}			
			
		} catch (MergeException e) {
			throw new JSONException("Merge error: " + e.getMessage());
		}
		
		return subFeaturesWeightConfigForRunTimeFiles;
	}

	private JSONObject toDeltaJson(JSONObject res, ServletContext context, OutputJSONMode mode, Environment env) throws JSONException {
		@SuppressWarnings("unchecked")
		Map<String, BaseAirlockItem> airlockItemsDB = (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);

		OrderingRuleItem itemInMaster = (OrderingRuleItem)airlockItemsDB.get(uniqueId.toString());

		if (!Utilities.jsonObjsAreEqual(configurtionJson, itemInMaster.getConfigurtionJson())) {
		//if (!itemInMaster.getConfiguration().equals(configuration)) {
			if (mode.equals(OutputJSONMode.RUNTIME_DEVELOPMENT) || mode.equals(OutputJSONMode.RUNTIME_PRODUCTION)){
				String subFeaturesWeightConfig = getSubFeaturesWeightConfigForRunTimeFiles(context, env);
				res.put(Constants.JSON_FEATURE_FIELD_CONFIGURATION, subFeaturesWeightConfig);
			}
			else {
				res.put(Constants.JSON_FEATURE_FIELD_CONFIGURATION, configuration);
			}
		}		

		return res;		
	}
	//return the ordering rule name in which the feature exists.
	//If does not exists at all - return null. 
	public String containsSubFeaure(String subFeatureId, Season season, ServletContext context, Environment env) throws JSONException, GenerationException, ValidationException {
		String ruleString = rule.getRuleString() == null ? "": rule.getRuleString();
		
		ValidationCache tester = env.getValidationCache();
		if (tester == null) {
			tester = new ValidationCache();
			env.setValidationCache(tester);
		}
		
		JSONObject configJsonObj = null;
		
		ValidationCache.Info info = tester.getInfo(context, season, stage, minAppVersion);
		configJsonObj = VerifyRule.fullConfigurationEvaluation(ruleString, configuration, info.minimalInvoker, info.maximalInvoker);
		
		if (configJsonObj!=null) {
			Set<Map.Entry<String, Object>> configEntries = configJsonObj.entrySet();
			if (configEntries != null) { 				
				for (Map.Entry<String, Object> entry : configEntries) {
					String curSubFeatureId = entry.getKey();
					if (subFeatureId.equals(curSubFeatureId)) {
						return getNameSpaceDotName();
					}
				}
			}			
		}	
		
		if (orderingRuleItems!=null && orderingRuleItems.size()>0) {
			for (int i=0; i<orderingRuleItems.size(); i++) {
				if (orderingRuleItems.get(i) instanceof OrderingRuleItem) {
					String foundInOrderingRule = ((OrderingRuleItem)(orderingRuleItems.get(i))).containsSubFeaure(subFeatureId, season, context, env);
					if (foundInOrderingRule!=null)
						return foundInOrderingRule;
				}
				
				if (orderingRuleItems.get(i) instanceof OrderingRuleMutualExclusionGroupItem) {
					String foundInOrderingRule = ((OrderingRuleMutualExclusionGroupItem)(orderingRuleItems.get(i))).containsSubFeaure(subFeatureId, season, context, env);
					if (foundInOrderingRule!=null)
						return foundInOrderingRule;
				}
			}
			
		}		
		
		return null;		
	}
	
	public static void replaceIdsInSubTree (BaseAirlockItem rootOrderingRule, String oldId, String newId) {
		if (!(rootOrderingRule instanceof OrderingRuleItem) && (rootOrderingRule instanceof OrderingRuleMutualExclusionGroupItem)) {
			return ;
		}
		
		if (rootOrderingRule instanceof OrderingRuleItem) {
			String oldConfig = ((OrderingRuleItem)rootOrderingRule).getConfiguration();
			String newConfig = oldConfig.replace(oldId, newId);
			((OrderingRuleItem) rootOrderingRule).setConfiguration(newConfig);
		}
		
		if (rootOrderingRule.getOrderingRuleItems()!=null) {
			for (int i=0; i<rootOrderingRule.getOrderingRuleItems().size(); i++ ) {
				replaceIdsInSubTree (rootOrderingRule.getOrderingRuleItems().get(i), oldId, newId);
			}
		}
		
	}
	
	public void setConfiguration(String configuration) {
		this.configuration = configuration;
		try {
			JSONObject newConfigurtionJson = new JSONObject(configuration);
			this.configurtionJson = newConfigurtionJson;
			String newConfigurationStringForEval = convertConfigJsonToConfigStringForEval(newConfigurtionJson);
			this.configurationStringForEval = newConfigurationStringForEval;
		}
		catch (JSONException je) {
			//should not occur
			logger.severe("Illegal configuration: " + configuration);
		}
	}
	
	//parent can be either feature of mtx	
	public BaseAirlockItem getParentFeatureOrMTX (Map<String, BaseAirlockItem> airlockItemsDB) {
		//after parent validation - we know that the parent is FEATURE or CONFIG_MUTUAL_EXCLUSION_GROUP
		BaseAirlockItem parentObj = airlockItemsDB.get(parent.toString());

		while (parentObj!=null && parentObj.getType()!=Type.FEATURE && parentObj.getType()!=Type.MUTUAL_EXCLUSION_GROUP) {
			parentObj = airlockItemsDB.get(parentObj.getParent().toString());
		}
		
		return parentObj; //not null because already validated and ordering rule is child of feature or mtx
	}
	
	boolean isChanged (JSONObject updatedFeatureData, Map<String, BaseAirlockItem> airlockItemsDB) throws JSONException {

		if (updatedFeatureData.containsKey(Constants.JSON_FEATURE_FIELD_CONFIGURATION) && updatedFeatureData.get(Constants.JSON_FEATURE_FIELD_CONFIGURATION)!=null) {
			String updatedConfiguration = updatedFeatureData.getString(Constants.JSON_FEATURE_FIELD_CONFIGURATION);
			if (!Utilities.jsonObjsAreEqual(configurtionJson, new JSONObject(updatedConfiguration))) {
				//check on the json and not the string since the ids order may be changed in the string but it is the same json.
				return true;
			}
			//if (!configuration.equals(updatedConfiguration)) {
			//	return true;				
			//}
		}

		return false;		
	}
	
	//Return a string with update details.
	//If nothing was changed - return empty string
	//for features cannot call super update since the cofiguration should be checked using the json and not the string 
	public List<ChangeDetails> updateAirlockItem(JSONObject updatedAirlockdItemData, Map<String, BaseAirlockItem> airlockItemsDB, BaseAirlockItem root, Environment env, Branch branch,Boolean isProdChange, ServletContext context, Map<String, Stage> updatedBranchesMap) throws JSONException {

		Boolean currentlyInProd = this.getStage().toString().equals("PRODUCTION");
		//skip configuration rule update - go straight to dataAirlockItem
		List<ChangeDetails> dataALItemUpdateDetails = super.updateAirlockItemIgnoreConfiguration(updatedAirlockdItemData, airlockItemsDB, root, env, branch,isProdChange, context, updatedBranchesMap);

		boolean wasChanged = ((dataALItemUpdateDetails != null) &&  !dataALItemUpdateDetails.isEmpty());
		List<ChangeDetails> changeDetailsList = dataALItemUpdateDetails;
		StringBuilder updateDetails = new StringBuilder("");

		//in branch - skip master features (branchStatus = null)
		if (branch==null || !branchStatus.equals(BranchStatus.NONE)) {
			if (updatedAirlockdItemData.containsKey(Constants.JSON_FEATURE_FIELD_CONFIGURATION) && updatedAirlockdItemData.get(Constants.JSON_FEATURE_FIELD_CONFIGURATION)!=null) {
				String updatedConfiguration = updatedAirlockdItemData.getString(Constants.JSON_FEATURE_FIELD_CONFIGURATION);
				if (updatedConfiguration!=null && !Utilities.jsonObjsAreEqual(configurtionJson, new JSONObject(updatedConfiguration))) {
					updateDetails.append("'configuration' changed from " + configuration + " to " + updatedConfiguration + "\n");
					setConfiguration(updatedConfiguration);  
					wasChanged = true; 
				}
			}
		}

		if(!updateDetails.toString().isEmpty()){
			Boolean isProductionChange = updatedAirlockdItemData.getString(Constants.JSON_FEATURE_FIELD_STAGE).equals("PRODUCTION") || currentlyInProd;
			changeDetailsList.add(new ChangeDetails(updateDetails.toString(),this,isProductionChange));
		}
		if (wasChanged) {
			lastModified = new Date();
		}

		return changeDetailsList;
	}
	
	protected void clone(BaseAirlockItem other)
	{
		OrderingRuleItem or = (OrderingRuleItem) other;
		super.clone(other);
		
		configuration = or.configuration;
		configurationStringForEval = or.configurationStringForEval;
		try {
			configurtionJson = (or.configurtionJson == null) ? null : (JSONObject) Utilities.cloneJson(or.configurtionJson, true);
		}  catch (JSONException e) {
		}
	}
	
	protected void shallowClone(BaseAirlockItem other)
	{
		OrderingRuleItem or = (OrderingRuleItem) other;
		
		super.shallowClone(other);
		
		configuration = or.configuration;
		configurationStringForEval = or.configurationStringForEval;
		configurtionJson = or.configurtionJson;
	}
}
