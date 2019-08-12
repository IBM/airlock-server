package com.ibm.airlock.admin;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response.Status;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.admin.authentication.UserInfo;
import com.ibm.airlock.admin.translations.OriginalString;
import com.ibm.airlock.engine.Environment;
import com.ibm.airlock.engine.Version;

public class OrderingRuleMutualExclusionGroupItem extends BaseMutualExclusionGroupItem {

	protected BaseAirlockItem newInstance()
	{
		return new OrderingRuleMutualExclusionGroupItem();
	}

	public OrderingRuleMutualExclusionGroupItem() {
		type = Type.ORDERING_RULE_MUTUAL_EXCLUSION_GROUP;
		orderingRuleItems = new LinkedList<BaseAirlockItem>();
	}
	
	public BaseAirlockItem duplicate (String minVersion, UUID newSeasonId, UUID parentId, Map<String, BaseAirlockItem> airlockItemsDB, 
			HashMap<String, String> oldToDuplicatedFeaturesId, ServletContext context, boolean duplicateSubFeatures, boolean createNewId,
			ValidationCache tester) {
		OrderingRuleMutualExclusionGroupItem res = new OrderingRuleMutualExclusionGroupItem();

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
		
				
		/*if (duplicateSubFeatures) {
			for (int i=0;i<orderingRuleItems.size(); i++) {
				BaseAirlockItem newAirlockItem = orderingRuleItems.get(i).duplicate(minVersion, newSeasonId, res.getUniqueId(), airlockItemsDB, oldToDuplicatedFeaturesId, context, duplicateSubFeatures, createNewId, tester);
				res.getOrderingRuleItems().add(newAirlockItem);					
				if (airlockItemsDB!=null)
					airlockItemsDB.put(newAirlockItem.getUniqueId().toString(), newAirlockItem);						
			}
		}*/
		
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
	
	public ValidationResults doValidateFeatureJSON(JSONObject featureObj, ServletContext context, String seasonId, LinkedList<String> addedSubFeatures, LinkedList<String> missingSubFeatures, InternalUserGroups userGroups, 
			HashMap<UUID, Integer> existingFeaturesInUpdate, String parent, HashMap<String, JSONObject> updatedFeaturesMap, UserInfo userInfo,
			ValidationCache tester,
			Map<String, BaseAirlockItem> airlockItemsDB, Environment env,List<OriginalString> copiedStings) {
		ValidationResults superRes = super.doValidateFeatureJSON(featureObj, context, seasonId, addedSubFeatures, missingSubFeatures, userGroups, existingFeaturesInUpdate, parent, updatedFeaturesMap, userInfo, tester, airlockItemsDB, env,copiedStings);
		
		if (superRes!=null)
			return superRes;
	
		if (env.getVersion().i < Version.v4_5.i) {
			return new ValidationResults("The current version range does not support ordering rules.", Status.BAD_REQUEST); 

		}
		
		try {
			if (parent!=null) {
				BaseAirlockItem parentObj = airlockItemsDB.get(parent);
				if (parentObj == null) {
					return new ValidationResults("Parent feature " + parent + " not found", Status.BAD_REQUEST);
				}

				if (parentObj.getType().equals(Type.ROOT)) {
					return new ValidationResults("Cannot add an ordering rule mutual exclusion group under the root.", Status.BAD_REQUEST);					
				}
				
				if (parentObj.getType().equals(Type.CONFIGURATION_RULE)) {
					return new ValidationResults("Cannot add an ordering rule mutual exclusion group under a configuration.", Status.BAD_REQUEST);					
				}
				
				if (parentObj.getType().equals(Type.CONFIG_MUTUAL_EXCLUSION_GROUP)) {
					return new ValidationResults("Cannot add an ordering rule mutual exclusion group under a configuration mutual exclusion group.", Status.BAD_REQUEST);					
				}								
				
				if (parentObj.getType().equals(Type.ENTITLEMENT_MUTUAL_EXCLUSION_GROUP)) {
					return new ValidationResults("Cannot add an ordering rule mutual exclusion group under an entitlemens mutual exclusion group.", Status.BAD_REQUEST);					
				}
				
				if (parentObj.getType().equals(Type.PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP)) {
					return new ValidationResults("Cannot add an ordering rule mutual exclusion group under a purchase options mutual exclusion group.", Status.BAD_REQUEST);					
				}
				
				if (parentObj.getType().equals(Type.ENTITLEMENT)) {
					return new ValidationResults("Cannot add an ordering rule mutual exclusion group under an entitlemen.", Status.BAD_REQUEST);					
				}
				
				if (parentObj.getType().equals(Type.PURCHASE_OPTIONS)) {
					return new ValidationResults("Cannot add an ordering rule mutual exclusion group under a purchase options.", Status.BAD_REQUEST);					
				}
			}
		} catch (ClassCastException cce) {
			return new ValidationResults("Illegal ordering rule mutual exclusion JSON: " + cce.getMessage(), Status.BAD_REQUEST);
		}
		return null;
	}
	
	//return the ordering rule name in which the feature exists.
	//If does not exists at all - return null. 
	public String containsSubFeaure(String subFeatureId, Season season, ServletContext context, Environment env) throws JSONException, GenerationException, ValidationException {						
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
}
