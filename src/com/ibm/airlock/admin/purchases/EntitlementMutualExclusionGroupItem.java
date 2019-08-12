package com.ibm.airlock.admin.purchases;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response.Status;

import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Strings;
import com.ibm.airlock.admin.BaseAirlockItem;
import com.ibm.airlock.admin.BaseMutualExclusionGroupItem;
import com.ibm.airlock.admin.InternalUserGroups;
import com.ibm.airlock.admin.Season;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.ValidationCache;
import com.ibm.airlock.admin.ValidationResults;
import com.ibm.airlock.admin.BaseAirlockItem.Type;
import com.ibm.airlock.admin.authentication.UserInfo;
import com.ibm.airlock.admin.translations.OriginalString;
import com.ibm.airlock.engine.Environment;
import com.ibm.airlock.engine.Version;

public class EntitlementMutualExclusionGroupItem extends BaseMutualExclusionGroupItem {

	protected BaseAirlockItem newInstance()
	{
		return new EntitlementMutualExclusionGroupItem();
	}

	public EntitlementMutualExclusionGroupItem() {
		type = Type.ENTITLEMENT_MUTUAL_EXCLUSION_GROUP;
		entitlementItems = new LinkedList<BaseAirlockItem>();
	}
	
	public BaseAirlockItem duplicate (String minVersion, UUID newSeasonId, UUID parentId, Map<String, BaseAirlockItem> airlockItemsDB, 
			HashMap<String, String> oldToDuplicatedFeaturesId, ServletContext context, boolean duplicateSubFeatures, boolean createNewId,
			ValidationCache tester) {
		EntitlementMutualExclusionGroupItem res = new EntitlementMutualExclusionGroupItem();

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
			for (int i=0;i<entitlementItems.size(); i++) {
				BaseAirlockItem newAirlockItem = entitlementItems.get(i).duplicate(minVersion, newSeasonId, res.getUniqueId(), airlockItemsDB, oldToDuplicatedFeaturesId, context, duplicateSubFeatures, createNewId, tester);
				res.getEntitlementItems().add(newAirlockItem);					
				if (airlockItemsDB!=null)
					airlockItemsDB.put(newAirlockItem.getUniqueId().toString(), newAirlockItem);						
			}
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
		
		@SuppressWarnings("unchecked")
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);
		Season season = seasonsDB.get(seasonId); //not null - after super validation
		
		if (superRes!=null)
			return superRes;
	
		if (env.getVersion().i < Version.v5_5.i) {
			return new ValidationResults(Strings.entitlementNotSupportedInVersion, Status.BAD_REQUEST); 
		}
		
		try {
			if (parent!=null) {
				BaseAirlockItem parentObj = airlockItemsDB.get(parent);
				if (parentObj == null) {
					return new ValidationResults("Parent feature " + parent + " not found", Status.BAD_REQUEST);
				}

				if (!parentObj.getType().equals(Type.ROOT) && !parentObj.getType().equals(Type.ENTITLEMENT) && !parentObj.getType().equals(Type.ENTITLEMENT_MUTUAL_EXCLUSION_GROUP)) {
					return new ValidationResults(Strings.illegalEntitlementMTXParent, Status.BAD_REQUEST);					
				}				
				
				if (parentObj.getType().equals(Type.ROOT) && season.getRoot().getUniqueId().equals(parentObj.getUniqueId())) {
					return new ValidationResults(Strings.cannotAddEntitlementMtxUnderFeaturesRoot, Status.BAD_REQUEST);					
				}
			}
		} catch (ClassCastException cce) {
			return new ValidationResults(Strings.illegalEntitlemenJson + cce.getMessage(), Status.BAD_REQUEST);
		}
		return null;
	}	
}
