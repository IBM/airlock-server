package com.ibm.airlock.admin;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response.Status;

import com.ibm.airlock.admin.translations.OriginalString;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.admin.authentication.UserInfo;
import com.ibm.airlock.engine.Environment;


public class ConfigMutualExclusionGroupItem extends BaseMutualExclusionGroupItem {
	
	protected BaseAirlockItem newInstance()
	{
		return new ConfigMutualExclusionGroupItem();
	}

	public ConfigMutualExclusionGroupItem() {
		type = Type.CONFIG_MUTUAL_EXCLUSION_GROUP;
		configurationRuleItems = new LinkedList<BaseAirlockItem>();
	}

	public BaseAirlockItem duplicate (String minVersion, UUID newSeasonId, UUID parentId, Map<String, BaseAirlockItem> airlockItemsDB, 
			HashMap<String, String> oldToDuplicatedFeaturesId, ServletContext context, boolean duplicateSubFeatures, boolean createNewId,
			ValidationCache tester) {
		ConfigMutualExclusionGroupItem res = new ConfigMutualExclusionGroupItem();
		
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
		res.setBranchEntitlementItems(Utilities.cloneStringsList(branchEntitlementItems));
		res.setBranchPurchaseOptionsItems(Utilities.cloneStringsList(branchPurchaseOptionsItems));
		
		for (int i=0;i<configurationRuleItems.size(); i++) {
			BaseAirlockItem newAirlockItem = configurationRuleItems.get(i).duplicate(minVersion, newSeasonId, res.getUniqueId(), airlockItemsDB, oldToDuplicatedFeaturesId, context, duplicateSubFeatures, createNewId, tester);
			res.getConfigurationRuleItems().add(newAirlockItem);					
			if (airlockItemsDB!=null)
				airlockItemsDB.put(newAirlockItem.getUniqueId().toString(), newAirlockItem);						
		}

		if (airlockItemsDB!=null)
			airlockItemsDB.put(res.getUniqueId().toString(), res);	
		
		return res;
	}	
	
	public ValidationResults doValidateFeatureJSON(JSONObject featureObj, ServletContext context, String seasonId, LinkedList<String> addedSubFeatures, LinkedList<String> missingSubFeatures, InternalUserGroups userGroups, 
			HashMap<UUID, Integer> existingFeaturesInUpdate, String parent, HashMap<String, JSONObject> updatedFeaturesMap, UserInfo userInfo,
			ValidationCache tester,
			Map<String, BaseAirlockItem> airlockItemsDB, Environment env,List<OriginalString> copiedStings) {
		ValidationResults superRes = super.doValidateFeatureJSON(featureObj, context, seasonId, addedSubFeatures, missingSubFeatures, userGroups, existingFeaturesInUpdate, parent, updatedFeaturesMap, userInfo, tester, airlockItemsDB, env,copiedStings);

		
		if (superRes!=null)
			return superRes;

		@SuppressWarnings("unchecked")
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);
		
		Season season = seasonsDB.get(seasonId); //i am sure that not null - already checked
	
		try {
			if (parent!=null) {
				BaseAirlockItem parentObj = airlockItemsDB.get(parent);
				if (parentObj == null) {
					return new ValidationResults("Parent feature " + parent + " not found", Status.BAD_REQUEST);
				}

				if (parentObj.getType().equals(Type.ROOT)) {
					return new ValidationResults("Cannot add a configuration mutual exclusion group under the root.", Status.BAD_REQUEST);					
				}
				
				if (parentObj.getType().equals(Type.ORDERING_RULE)) {
					return new ValidationResults("Cannot add a configuration mutual exclusion group under an ordering rule.", Status.BAD_REQUEST);					
				}
				
				if (parentObj.getType().equals(Type.ORDERING_RULE_MUTUAL_EXCLUSION_GROUP)) {
					return new ValidationResults("Cannot add a configuration mutual exclusion group under an ordering rule mutual exclusion group.", Status.BAD_REQUEST);					
				}
				
				if (parentObj.getType().equals(Type.MUTUAL_EXCLUSION_GROUP)) {
					return new ValidationResults("Cannot add a configuration mutual exclusion group under a mutual exclusion group.", Status.BAD_REQUEST);					
				}
				
				if (parentObj.getType().equals(Type.ENTITLEMENT_MUTUAL_EXCLUSION_GROUP)) {
					return new ValidationResults("Cannot add a configuration mutual exclusion group under an entitlemens mutual exclusion group.", Status.BAD_REQUEST);					
				}
				
				if (parentObj.getType().equals(Type.PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP)) {
					return new ValidationResults("Cannot add a configuration mutual exclusion group under a purchase options mutual exclusion group.", Status.BAD_REQUEST);					
				}
				
				if (season.getServerVersion().equals(Constants.PRE_2_1_SERVER_VERSION)) { 
					
					if (parentObj.getType().equals(Type.CONFIG_MUTUAL_EXCLUSION_GROUP)) {
						return new ValidationResults("In Airlock 2.0 and earlier, you cannot add a configuration mutual exclusion group under another configuration mutual exclusion group.", Status.BAD_REQUEST);
					}
					else if (parentObj.getType().equals(Type.CONFIGURATION_RULE)) {
						return new ValidationResults("In Airlock 2.0 and earlier, you cannot add a configuration mutual exclusion group under a configuration.", Status.BAD_REQUEST);					
					}
				}		
			}
		} catch (ClassCastException cce) {
			return new ValidationResults("Illegal feature JSON: " + cce.getMessage(), Status.BAD_REQUEST);
		}
		return null;
	}

}
