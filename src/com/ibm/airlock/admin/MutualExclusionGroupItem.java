package com.ibm.airlock.admin;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response.Status;

import com.ibm.airlock.admin.translations.OriginalString;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Constants.Action;
import com.ibm.airlock.Constants.Stage;
import com.ibm.airlock.Strings;
import com.ibm.airlock.admin.BaseAirlockItem.Type;
import com.ibm.airlock.admin.authentication.UserInfo;
import com.ibm.airlock.engine.Environment;

public class MutualExclusionGroupItem extends BaseMutualExclusionGroupItem {

	protected BaseAirlockItem newInstance()
	{
		return new MutualExclusionGroupItem();
	}

	public MutualExclusionGroupItem() {
		type = Type.MUTUAL_EXCLUSION_GROUP;
		featuresItems = new LinkedList<BaseAirlockItem>();
		orderingRuleItems = new LinkedList<BaseAirlockItem>();
	}
	
	public BaseAirlockItem duplicate (String minVersion, UUID newSeasonId, UUID parentId, Map<String, BaseAirlockItem> airlockItemsDB, 
			HashMap<String, String> oldToDuplicatedFeaturesId, ServletContext context, boolean duplicateSubFeatures, boolean createNewId,
			ValidationCache tester) {
		MutualExclusionGroupItem res = new MutualExclusionGroupItem();
		//res.lastModified = lastModified;
		if (createNewId) {
			res.setUniqueId(UUID.randomUUID());
		} else {
			res.setUniqueId(uniqueId);			
		}
		oldToDuplicatedFeaturesId.put(uniqueId.toString(), res.getUniqueId().toString());
		
		if (newSeasonId!=null) {
			res.setSeasonId(newSeasonId);
		} 
		else {
			res.setSeasonId(seasonId);
		}
		
		res.setParent(parentId);
		res.setMaxFeaturesOn(maxFeaturesOn);
		res.setLastModified(lastModified);
		res.setBranchStatus(branchStatus);
		res.setBranchFeatureParentName(branchFeatureParentName);
		res.setBranchFeaturesItems(Utilities.cloneStringsList(branchFeaturesItems));
		res.setBranchConfigurationRuleItems(Utilities.cloneStringsList(branchConfigurationRuleItems));
		res.setBranchOrderingRuleItems(Utilities.cloneStringsList(branchOrderingRuleItems));
		res.setBranchEntitlementItems(Utilities.cloneStringsList(branchEntitlementItems));
		res.setBranchPurchaseOptionsItems(Utilities.cloneStringsList(branchPurchaseOptionsItems));
				
		if (duplicateSubFeatures) {
			for (int i=0;i<featuresItems.size(); i++) {
				BaseAirlockItem newAirlockItem = featuresItems.get(i).duplicate(minVersion, newSeasonId, res.getUniqueId(), airlockItemsDB, oldToDuplicatedFeaturesId, context, duplicateSubFeatures, createNewId, tester);
				res.getFeaturesItems().add(newAirlockItem);					
				if (airlockItemsDB!=null)
					airlockItemsDB.put(newAirlockItem.getUniqueId().toString(), newAirlockItem);						
			}
						
		}

		if (orderingRuleItems!=null) {
			for (int i=0;i<orderingRuleItems.size(); i++) {
				BaseAirlockItem newAirlockItem = orderingRuleItems.get(i).duplicate(minVersion, newSeasonId, res.getUniqueId(), airlockItemsDB, oldToDuplicatedFeaturesId, context, duplicateSubFeatures, createNewId, tester);
				res.getOrderingRuleItems().add(newAirlockItem);					
				if (airlockItemsDB!=null)
					airlockItemsDB.put(newAirlockItem.getUniqueId().toString(), newAirlockItem);						
			}
		}
		
		if (airlockItemsDB!=null)
			airlockItemsDB.put(res.getUniqueId().toString(), res);	
		
		return res;
	}	
	
	public ValidationResults doValidateFeatureJSON(JSONObject featureObj, ServletContext context, String seasonId, LinkedList<String> addedSubFeatures, LinkedList<String> missingSubFeatures, InternalUserGroups userGroups, 
			HashMap<UUID, Integer> existingFeaturesInUpdate, 
			String parent, HashMap<String, JSONObject> updatedFeaturesMap, UserInfo userInfo,
			ValidationCache tester,
			Map<String, BaseAirlockItem> airlockItemsDB, Environment env,List<OriginalString> copiedStings) {
		ValidationResults superRes = super.doValidateFeatureJSON(featureObj, context, seasonId, addedSubFeatures, missingSubFeatures, userGroups, existingFeaturesInUpdate, parent, updatedFeaturesMap, userInfo, tester, airlockItemsDB, env,copiedStings);

		if (superRes!=null)
			return superRes;

		@SuppressWarnings("unchecked")
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);
		
		Season season = seasonsDB.get(seasonId); //i am sure that not null - already checked
		
		Action action = Action.ADD;
		try {
			if (parent!=null) {
				BaseAirlockItem parentObj = airlockItemsDB.get(parent);
				if (parentObj == null) {
					return new ValidationResults("Parent feature " + parent + " not found", Status.BAD_REQUEST);
				}

				if (parentObj.getType().equals(Type.CONFIGURATION_RULE)) {
					return new ValidationResults("Cannot add a mutual exclusion group under a configuration.", Status.BAD_REQUEST);					
				}
				
				if (parentObj.getType().equals(Type.CONFIG_MUTUAL_EXCLUSION_GROUP)) {
					return new ValidationResults("Cannot add a mutual exclusion group under a configuration mutual exclusion group.", Status.BAD_REQUEST);					
				}
				
				if (parentObj.getType().equals(Type.ORDERING_RULE)) {
					return new ValidationResults("Cannot add a mutual exclusion group under an ordering rule.", Status.BAD_REQUEST);					
				}
				
				if (parentObj.getType().equals(Type.ORDERING_RULE_MUTUAL_EXCLUSION_GROUP)) {
					return new ValidationResults("Cannot add a mutual exclusion group under an ordering rule mutual exclusion group.", Status.BAD_REQUEST);					
				}
				
				if (parentObj.getType().equals(Type.ENTITLEMENT_MUTUAL_EXCLUSION_GROUP)) {
					return new ValidationResults("Cannot add a mutual exclusion group under an entitlements mutual exclusion group.", Status.BAD_REQUEST);					
				}
				
				if (parentObj.getType().equals(Type.PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP)) {
					return new ValidationResults("Cannot add a mutual exclusion group under a purchase options mutual exclusion group.", Status.BAD_REQUEST);					
				}
				
				if (parentObj.getType().equals(Type.ENTITLEMENT)) {
					return new ValidationResults("Cannot add a mutual exclusion group under an entitlement item.", Status.BAD_REQUEST);					
				}
				
				if (parentObj.getType().equals(Type.PURCHASE_OPTIONS)) {
					return new ValidationResults("Cannot add a mutual exclusion group under a purchase options item.", Status.BAD_REQUEST);					
				}
				
				if (parentObj.getType().equals(Type.ROOT) && season.getEntitlementsRoot().getUniqueId().equals(parentObj.getUniqueId())) {
					return new ValidationResults(Strings.cannotAddFeatureMtxUnderEntitlementsRoot, Status.BAD_REQUEST);					
				}
				
				if (season.getServerVersion().equals(Constants.PRE_2_1_SERVER_VERSION)) { 					
					if (parentObj.getType().equals(Type.MUTUAL_EXCLUSION_GROUP)) {
						return new ValidationResults("In a version range created in Airlock V2.0 and earlier, you cannot add a mutual exclusion group under another mutual exclusion group.", Status.BAD_REQUEST);
					}
				}					
			}
			
			if (featureObj.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && featureObj.get(Constants.JSON_FIELD_UNIQUE_ID)!=null) {
				//if JSON contains uniqueId - update an existing feature otherwise create a new feature
				action = Action.UPDATE;
			}
			
			
			if (action == Action.UPDATE) {			
				//validate that no more that maxFeaturesOn features are in PRODUCTION and defaultIfAirlockSystemIsDown = true
				//sub features are mandatory - if none exist expect empty list
				if (!featureObj.containsKey(Constants.JSON_FEATURE_FIELD_FEATURES) || featureObj.get(Constants.JSON_FEATURE_FIELD_FEATURES) == null) {
					return new ValidationResults("The features field is missing. This field must be specified during feature update.", Status.BAD_REQUEST);
				}

				int updatedMaxFeaturesOn=1;
				if (featureObj.containsKey(Constants.JSON_FEATURE_FIELD_MAX_FEATURES_ON) && featureObj.get(Constants.JSON_FEATURE_FIELD_MAX_FEATURES_ON) != null) {
					updatedMaxFeaturesOn = featureObj.getInt(Constants.JSON_FEATURE_FIELD_MAX_FEATURES_ON); 
					if (updatedMaxFeaturesOn<=0) { 
						return new ValidationResults("maxFeaturesOn should be an integer greater than 1.", Status.BAD_REQUEST);
					}
				}
				
				int numOfProdDefTrue = 0;
				JSONArray updatedSubFeatures = featureObj.getJSONArray(Constants.JSON_FEATURE_FIELD_FEATURES); //validate that is array value
				for (int i=0; i<updatedSubFeatures.size(); i++) {
					JSONObject updatedSubFeature = updatedSubFeatures.getJSONObject(i);					
					
					
					//need to validate each field since doValidate of subFeature wasn't called yet 
					//type - look only for features
					if (!updatedSubFeature.containsKey(Constants.JSON_FEATURE_FIELD_TYPE) || updatedSubFeature.getString(Constants.JSON_FEATURE_FIELD_TYPE) == null) {
						return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_TYPE), Status.BAD_REQUEST);
					}

					String typeStr = updatedSubFeature.getString(Constants.JSON_FEATURE_FIELD_TYPE);
					Type typeObj = strToType(typeStr);
					if (typeObj == null) {
						return new ValidationResults("Illegal feature type: '" + typeStr + "'", Status.BAD_REQUEST);
					}
					
					if (typeObj==Type.FEATURE) {
					
						//defaultIfAirlockSystemIsDown
						if (!updatedSubFeature.containsKey(Constants.JSON_FEATURE_FIELD_DEF_VAL) || updatedSubFeature.get(Constants.JSON_FEATURE_FIELD_DEF_VAL) == null) {
							return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_DEF_VAL), Status.BAD_REQUEST);					
						}
	
						boolean defaultVal = updatedSubFeature.getBoolean(Constants.JSON_FEATURE_FIELD_DEF_VAL); //validate that is boolean value
						
						if (!updatedSubFeature.containsKey(Constants.JSON_FEATURE_FIELD_STAGE) || updatedSubFeature.getString(Constants.JSON_FEATURE_FIELD_STAGE) == null) {
							return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_STAGE), Status.BAD_REQUEST);					
						}
	
						String stageStr = updatedSubFeature.getString(Constants.JSON_FEATURE_FIELD_STAGE);
						Stage stage = Utilities.strToStage(stageStr);
						if (stage == null) {
							return new ValidationResults("Illegal feature stage: '" + stageStr + "'", Status.BAD_REQUEST);
						}				

						if (defaultVal == true && stage==Stage.PRODUCTION) {
							numOfProdDefTrue++;
							if (numOfProdDefTrue > updatedMaxFeaturesOn) {
								return new ValidationResults("Unable to update the feature. The feature is in the Production stage and the Default setting is On. The value of the Maximal Number of Features On setting was exceeded.", Status.BAD_REQUEST);
							}
						}
					}
				}
			}
		} catch (JSONException jsne) {
			return new ValidationResults(jsne.getMessage(), Status.BAD_REQUEST);		
		} catch (ClassCastException cce) {
			return new ValidationResults("Illegal feature JSON: " + cce.getMessage(), Status.BAD_REQUEST);
		}
		return null;
	}
			
}
