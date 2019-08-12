package com.ibm.airlock.admin;

import java.util.*;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response.Status;

import com.ibm.airlock.Strings;
import com.ibm.airlock.admin.translations.OriginalString;
import com.ibm.airlock.engine.VerifyRule;
import com.ibm.airlock.utilities.FeatureFilter;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.AnalyticsServices;
import com.ibm.airlock.Constants;
import com.ibm.airlock.Constants.Action;
import com.ibm.airlock.Constants.BranchStatus;
import com.ibm.airlock.Constants.OutputJSONMode;
import com.ibm.airlock.Constants.Platform;
import com.ibm.airlock.Constants.REQUEST_ITEM_TYPE;
import com.ibm.airlock.Constants.Stage;
import com.ibm.airlock.Constants.UtilityType;
import com.ibm.airlock.admin.authentication.UserInfo;
import com.ibm.airlock.admin.purchases.EntitlementItem;
import com.ibm.airlock.admin.purchases.EntitlementMutualExclusionGroupItem;
import com.ibm.airlock.admin.purchases.PurchaseOptionsItem;
import com.ibm.airlock.admin.purchases.PurchaseOptionsMutualExclusionGroupItem;
import com.ibm.airlock.admin.BaseAirlockItem.Type;
import com.ibm.airlock.admin.MergeBranch.MergeException;
import com.ibm.airlock.admin.analytics.GlobalDataCollection;
import com.ibm.airlock.engine.Environment;

public abstract class BaseAirlockItem {

	public static final Logger logger = Logger.getLogger(BaseAirlockItem.class.getName());

	public enum Type {
		FEATURE,
		CONFIGURATION_RULE,
		MUTUAL_EXCLUSION_GROUP,
		CONFIG_MUTUAL_EXCLUSION_GROUP,
		ROOT,
		ORDERING_RULE,
		ORDERING_RULE_MUTUAL_EXCLUSION_GROUP,
		ENTITLEMENT,
		PURCHASE_OPTIONS,
		ENTITLEMENT_MUTUAL_EXCLUSION_GROUP,
		PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP
	}

	protected Type type = null;	//c+u (not changed)
	protected UUID uniqueId = null; //nc + u
	protected LinkedList<BaseAirlockItem> featuresItems = null; //in create: not in map/null/empty array, update: required (in map not null.)
	protected LinkedList<BaseAirlockItem> configurationRuleItems = null; //in create: not in map/null/empty array, update: required (in map not null.)
	protected LinkedList<BaseAirlockItem> orderingRuleItems = null; //in create: not in map/null/empty array, update: required (in map not null.)
	protected LinkedList<BaseAirlockItem> entitlementItems = null; //in create: not in map/null/empty array, update: required (in map not null.)
	protected LinkedList<BaseAirlockItem> purchaseOptionsItems = null; //in create: not in map/null/empty array, update: required (in map not null.)
	
	protected Date lastModified = null; // nc + u
	protected UUID seasonId = null; //c+u
	protected UUID parent = null;
	
	//fields that are used only for features in branches
	protected String branchFeatureParentName = null;
	protected BranchStatus branchStatus = BranchStatus.NONE; //optional field, cannot be updated during update, should not be specified during creation
	protected LinkedList<String> branchFeaturesItems = null; //optional field - forbidden in create, if exists in update should not be changed
	protected LinkedList<String> branchConfigurationRuleItems = null; //optional field - forbidden in create, if exists in update should not be changed
	protected LinkedList<String> branchOrderingRuleItems = null; //optional field - forbidden in create, if exists in update should not be changed
	protected LinkedList<String> branchEntitlementItems = null; //optional field - forbidden in create, if exists in update should not be changed
	protected LinkedList<String> branchPurchaseOptionsItems = null; //optional field - forbidden in create, if exists in update should not be changed

	LinkedList<BaseAirlockItem> getItemList(Type type) throws Exception
	{
		switch (type)
		{
		case FEATURE : return featuresItems;
		case CONFIGURATION_RULE : return configurationRuleItems;
		case ORDERING_RULE : return orderingRuleItems;
		case ENTITLEMENT : return entitlementItems;
		case PURCHASE_OPTIONS : return purchaseOptionsItems;
		default: throw new Exception("invalid type");
		}
	}
	public void setItemList(Type type, LinkedList<BaseAirlockItem> list) throws Exception
	{
		switch (type)
		{
		case FEATURE : featuresItems = list; return;
		case CONFIGURATION_RULE : configurationRuleItems = list; return;
		case ORDERING_RULE : orderingRuleItems = list; return;
		case ENTITLEMENT : entitlementItems = list; return;
		case PURCHASE_OPTIONS : purchaseOptionsItems = list; return;
		default: throw new Exception("invalid type");
		}
	}
	LinkedList<String> getBranchList(Type type) throws Exception
	{
		switch (type)
		{
		case FEATURE : return branchFeaturesItems;
		case CONFIGURATION_RULE : return branchConfigurationRuleItems;
		case ORDERING_RULE : return branchOrderingRuleItems;
		case ENTITLEMENT : return branchEntitlementItems;
		case PURCHASE_OPTIONS : return branchPurchaseOptionsItems;
		default: throw new Exception("invalid type");
		}
	}
	public static BaseAirlockItem getClone(BaseAirlockItem other)
	{
		BaseAirlockItem item = other.newInstance();
		item.clone(other);
		return item;
	}
	public static LinkedList<BaseAirlockItem> getClone(LinkedList<BaseAirlockItem> other)
	{
		if (other == null)
			return null;

		LinkedList<BaseAirlockItem> out = new LinkedList<BaseAirlockItem>();
		for (BaseAirlockItem item : other)
		{
			out.add(getClone(item));
		}
		return out;
	}
	public static BaseAirlockItem getShallowClone(BaseAirlockItem other)
	{
		BaseAirlockItem item = other.newInstance();
		item.shallowClone(other);
		return item;
	}
	protected abstract BaseAirlockItem newInstance(); // derived classes will implement

	protected void clone(BaseAirlockItem other)
	{
		copyFields(other);

		featuresItems = getClone(other.featuresItems);
		configurationRuleItems = getClone(other.configurationRuleItems);
		orderingRuleItems = getClone(other.orderingRuleItems);
		entitlementItems = getClone(other.entitlementItems);
		purchaseOptionsItems = getClone(other.purchaseOptionsItems);
		
		branchFeaturesItems = other.branchFeaturesItems == null ? null : new LinkedList<String>(other.branchFeaturesItems);
		branchConfigurationRuleItems = other.branchConfigurationRuleItems == null ? null : new LinkedList<String>(other.branchConfigurationRuleItems);
		branchOrderingRuleItems = other.branchOrderingRuleItems == null ? null : new LinkedList<String>(other.branchOrderingRuleItems);
		branchEntitlementItems = other.branchEntitlementItems == null ? null : new LinkedList<String>(other.branchEntitlementItems);
		branchPurchaseOptionsItems = other.branchPurchaseOptionsItems == null ? null : new LinkedList<String>(other.branchPurchaseOptionsItems);
	}
	protected void shallowClone(BaseAirlockItem other)
	{
		copyFields(other);
		featuresItems = other.featuresItems;
		configurationRuleItems = other.configurationRuleItems;
		orderingRuleItems = other.orderingRuleItems;
		entitlementItems = other.entitlementItems;
		purchaseOptionsItems = other.purchaseOptionsItems;
		
		branchFeaturesItems = other.branchFeaturesItems;
		branchConfigurationRuleItems = other.branchConfigurationRuleItems;
		branchOrderingRuleItems = other.branchOrderingRuleItems;
		branchEntitlementItems = other.branchEntitlementItems;
		branchPurchaseOptionsItems = other.branchPurchaseOptionsItems;
	}
	void copyFields(BaseAirlockItem other)
	{
		type = other.type;
		uniqueId = other.uniqueId;
		lastModified = other.lastModified;
		seasonId = other.seasonId;
		parent = other.parent;
		branchFeatureParentName = other.branchFeatureParentName;
		branchStatus = other.branchStatus;
	}
	public Type getType() {
		return type;
	}
	/*public void setType(Type type) {
		this.type = type;
	}*/
	public UUID getUniqueId() {
		return uniqueId;
	}
	public void setUniqueId(UUID uniqueId) {
		this.uniqueId = uniqueId;
	}
	public LinkedList<BaseAirlockItem> getFeaturesItems() {
		return featuresItems;
	}
	public LinkedList<BaseAirlockItem> getConfigurationRuleItems() {
		return configurationRuleItems;
	}
	public LinkedList<BaseAirlockItem> getOrderingRuleItems() {
		return orderingRuleItems;
	}
	public LinkedList<BaseAirlockItem> getEntitlementItems() {
		return entitlementItems;
	}
	public LinkedList<BaseAirlockItem> getPurchaseOptionsItems() {
		return purchaseOptionsItems;
	}

	
	public Date getLastModified() {
		return lastModified;
	}
	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

	public UUID getSeasonId() {
		return seasonId;
	}
	public void setSeasonId(UUID seasonId) {
		this.seasonId = seasonId;
	}
	public UUID getParent() {
		return parent;
	}
	public void setParent(UUID parent) {
		this.parent = parent;
	}
	//return null upon success, error string upon failure
	public String addAirlockItem(BaseAirlockItem airlockItem) {
		logger.info("adding feature " + airlockItem.getUniqueId().toString() +  " to parent " + uniqueId.toString());

		if (airlockItem.getType().equals(Type.FEATURE) || airlockItem.getType().equals(Type.MUTUAL_EXCLUSION_GROUP)) {
			if (featuresItems == null) {
				return "Failed to add Feature. Parent does not contain the specified feature type.";
			}
			//logger.info("current number of features = " + featuresItems.size());		
			boolean suc = featuresItems.add(airlockItem);
			if (!suc)
				logger.info("Failed to add feature to the list.");

			//logger.info("after addition number of features = " + featuresItems.size());

		}

		if (airlockItem.getType().equals(Type.CONFIG_MUTUAL_EXCLUSION_GROUP) || airlockItem.getType().equals(Type.CONFIGURATION_RULE)) {

			if (configurationRuleItems == null) {
				return "Failed to add configuration. Parent does not contain the specified feature type.";
			}
			//logger.info("current number of configuration rule = " + configurationRuleItems.size());
			boolean suc = configurationRuleItems.add(airlockItem);
			if (!suc)
				logger.info("Failed to add configuration rule to the list.");

			//logger.info("after addition number of configuration rule = " + configurationRuleItems.size());

		}
		
		if (airlockItem.getType().equals(Type.ORDERING_RULE_MUTUAL_EXCLUSION_GROUP) || airlockItem.getType().equals(Type.ORDERING_RULE)) {

			if (orderingRuleItems == null) {
				return "Failed to add ordering rule. Parent does not contain the specified feature type.";
			}
			
			boolean suc = orderingRuleItems.add(airlockItem);
			if (!suc)
				logger.info("Failed to add ordering rule to the list.");
		}
		
		if (airlockItem.getType().equals(Type.ENTITLEMENT) || airlockItem.getType().equals(Type.ENTITLEMENT_MUTUAL_EXCLUSION_GROUP)) {

			if (entitlementItems == null) {
				return "Failed to add in app purchase. Parent does not contain the specified type.";
			}
			
			boolean suc = entitlementItems.add(airlockItem);
			if (!suc)
				logger.info("Failed to add in app purchase to the list.");
		}
		
		if (airlockItem.getType().equals(Type.PURCHASE_OPTIONS) || airlockItem.getType().equals(Type.PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP)) {

			if (purchaseOptionsItems == null) {
				return "Failed to add purchase options. Parent does not contain the specified type.";
			}
			
			boolean suc = purchaseOptionsItems.add(airlockItem);
			if (!suc)
				logger.info("Failed to add purchase options to the list.");
		}
		return null;
	}

	public boolean containSubItemInProductionStage() {
		if (featuresItems!=null) {
			for (int i=0;i<featuresItems.size(); i++) {
				if (featuresItems.get(i).containSubItemInProductionStage())
					return true;
			}		
		}

		if (configurationRuleItems!=null) {
			for (int i=0;i<configurationRuleItems.size(); i++) {
				if (configurationRuleItems.get(i).containSubItemInProductionStage())
					return true;
			}		
		}
		
		if (orderingRuleItems!=null) {
			for (int i=0;i<orderingRuleItems.size(); i++) {
				if (orderingRuleItems.get(i).containSubItemInProductionStage())
					return true;
			}		
		}
		
		if (entitlementItems!=null) {
			for (int i=0;i<entitlementItems.size(); i++) {
				if (entitlementItems.get(i).containSubItemInProductionStage())
					return true;
			}		
		}
		
		if (purchaseOptionsItems!=null) {
			for (int i=0;i<purchaseOptionsItems.size(); i++) {
				if (purchaseOptionsItems.get(i).containSubItemInProductionStage())
					return true;
			}		
		}

		return false;
	}

	//return list with all the feture's subFeatures IDs
	public LinkedList<UUID> getSubFeaturesId() {
		LinkedList<UUID> subFeaturesIds = new LinkedList<UUID>();
		doGetSubFeaturesId(subFeaturesIds);
		return subFeaturesIds;
	}

	public String getBranchFeatureParentName() {
		return branchFeatureParentName;
	}
	public void setBranchFeatureParentName(String branchFeatureParentName) {
		this.branchFeatureParentName = branchFeatureParentName;
	}
	public BranchStatus getBranchStatus() {
		return branchStatus;
	}
	public void setBranchStatus(BranchStatus branchStatus) {
		this.branchStatus = branchStatus;
	}
	public LinkedList<String> getBranchFeaturesItems() {
		return branchFeaturesItems;
	}
	public void setBranchFeaturesItems(LinkedList<String> branchFeaturesItems) {
		this.branchFeaturesItems = branchFeaturesItems;
	}
	public LinkedList<String> getBranchConfigurationRuleItems() {
		return branchConfigurationRuleItems;
	}
	public void setBranchConfigurationRuleItems(LinkedList<String> branchConfigurationRuleItems) {
		this.branchConfigurationRuleItems = branchConfigurationRuleItems;
	}
	public LinkedList<String> getBranchOrderingRuleItems() {
		return branchOrderingRuleItems;
	}
	public void setBranchOrderingRuleItems(LinkedList<String> branchOrderingRuleItems) {
		this.branchOrderingRuleItems = branchOrderingRuleItems;
	}
	public LinkedList<String> getBranchEntitlementItems() {
		return branchEntitlementItems;
	}
	public void setBranchEntitlementItems(LinkedList<String> branchEntitlementItems) {
		this.branchEntitlementItems = branchEntitlementItems;
	}
	public LinkedList<String> getBranchPurchaseOptionsItems() {
		return branchPurchaseOptionsItems;
	}
	public void setBranchPurchaseOptionsItems(LinkedList<String> branchPurchaseOptionsItems) {
		this.branchPurchaseOptionsItems = branchPurchaseOptionsItems;
	}
	
	public void doGetSubFeaturesId(LinkedList<UUID> subFeaturesIds) {
		if (featuresItems!=null) {
			for (int i=0;i<featuresItems.size(); i++) {
				subFeaturesIds.add(featuresItems.get(i).getUniqueId());
				featuresItems.get(i).doGetSubFeaturesId(subFeaturesIds);
			}		
		}

		if (configurationRuleItems!=null) {
			for (int i=0;i<configurationRuleItems.size(); i++) {
				subFeaturesIds.add(configurationRuleItems.get(i).getUniqueId());
				configurationRuleItems.get(i).doGetSubFeaturesId(subFeaturesIds);
			}		
		}
		
		if (orderingRuleItems!=null) {
			for (int i=0;i<orderingRuleItems.size(); i++) {
				subFeaturesIds.add(orderingRuleItems.get(i).getUniqueId());
				orderingRuleItems.get(i).doGetSubFeaturesId(subFeaturesIds);
			}		
		}
		
		if (entitlementItems!=null) {
			for (int i=0;i<entitlementItems.size(); i++) {
				subFeaturesIds.add(entitlementItems.get(i).getUniqueId());
				entitlementItems.get(i).doGetSubFeaturesId(subFeaturesIds);
			}		
		}
		
		if (purchaseOptionsItems!=null) {
			for (int i=0;i<purchaseOptionsItems.size(); i++) {
				subFeaturesIds.add(purchaseOptionsItems.get(i).getUniqueId());
				purchaseOptionsItems.get(i).doGetSubFeaturesId(subFeaturesIds);
			}		
		}
	}

	public void removeAirlockItem(UUID remItemId) {

		if ((featuresItems == null || featuresItems.size() == 0) && (configurationRuleItems == null || configurationRuleItems.size() == 0) 
				&& (orderingRuleItems == null || orderingRuleItems.size() == 0) && (entitlementItems == null || entitlementItems.size() == 0)
				&& (purchaseOptionsItems == null || purchaseOptionsItems.size() == 0)) {
			logger.warning("Unable to remove feature " + remItemId.toString() + " from parent " + uniqueId.toString() + ": parent has no subfeatures.");
			return;
		}

		if (featuresItems!=null) {
			for (int i=0; i< featuresItems.size(); i++) {
				if (featuresItems.get(i).getUniqueId().equals(remItemId)) {
					featuresItems.remove(i);
					return;
				}
			}
		}

		if (configurationRuleItems!=null) {
			for (int i=0; i< configurationRuleItems.size(); i++) {
				if (configurationRuleItems.get(i).getUniqueId().equals(remItemId)) {
					configurationRuleItems.remove(i);
					return;
				}
			}
		}
		
		if (orderingRuleItems!=null) {
			for (int i=0; i< orderingRuleItems.size(); i++) {
				if (orderingRuleItems.get(i).getUniqueId().equals(remItemId)) {
					orderingRuleItems.remove(i);
					return;
				}
			}
		}
		
		if (entitlementItems!=null) {
			for (int i=0; i< entitlementItems.size(); i++) {
				if (entitlementItems.get(i).getUniqueId().equals(remItemId)) {
					entitlementItems.remove(i);
					return;
				}
			}
		}
		
		if (purchaseOptionsItems!=null) {
			for (int i=0; i< purchaseOptionsItems.size(); i++) {
				if (purchaseOptionsItems.get(i).getUniqueId().equals(remItemId)) {
					purchaseOptionsItems.remove(i);
					return;
				}
			}
		}

		//logger.warning("Unable to remove feature " + remItemId.toString() + " from parent " + uniqueId.toString() + ": The specified subfeature does not exist under this parent.");
	}

	public ValidationResults validateFeatureJSON(JSONObject featureObj, ServletContext context, String seasonId, InternalUserGroups userGroups,
			String parent, UserInfo userInfo, Map<String, BaseAirlockItem> airlockItemsDB, Environment env) {
		return validateFeatureJSON(featureObj,context,seasonId,userGroups,parent,userInfo,airlockItemsDB,env,new ArrayList<OriginalString>());
	}

	public ValidationResults validateFeatureJSON(JSONObject featureObj, ServletContext context, String seasonId, InternalUserGroups userGroups,
			String parent, UserInfo userInfo, Map<String, BaseAirlockItem> airlockItemsDB, Environment env, List<OriginalString> copiedStrings) {
		LinkedList<String> addedSubFeatures = new LinkedList<String>();
		LinkedList<String> missingSubFeatures = new LinkedList<String>();

		//check that each featureId appears only once in the update tree
		HashMap<UUID, Integer> existingFeaturesInUpdate = new HashMap<UUID, Integer>();  

		HashMap<String, JSONObject> updatedFeatures = new HashMap<String, JSONObject>();

		ValidationCache tester = new ValidationCache(null, copiedStrings); // add copied strings to cache

		ValidationResults res = doValidateFeatureJSON (featureObj, context, seasonId, addedSubFeatures, missingSubFeatures, userGroups, existingFeaturesInUpdate, parent, updatedFeatures, userInfo, tester, airlockItemsDB, env,copiedStrings);
		if (res != null)
			return res;

		//validate that all missing subFeatures were added somewhere else in the given tree.
		//This is used for the case that subFeature were moved within the given feature tree.
		for (String missingFeatureId:missingSubFeatures) {
			boolean found = false;
			for (int i=0; i<addedSubFeatures.size(); i++) {
				if (missingFeatureId.equals(addedSubFeatures.get(i))) {
					found = true;
					break;
				}
			}
			if (!found) {
				return new ValidationResults(missingFeatureId.toString() + " subitem is missing. If you want to remove the subitem, call delete.", Status.BAD_REQUEST);
			}
		}

		return null;

	}
	//considerProdUnderDev: for prod runtime file - we should consider prod under dev as dev
	//                      for user permissions - we should consider prod under dev as prod 

	private ValidationResults validateProductionDontChangedForList(LinkedList<BaseAirlockItem> currentSubItemslist, JSONArray updatedSubItemsArray, Map<String, BaseAirlockItem> airlockItemsDB, Branch branch, ServletContext context, boolean considerProdUnderDevAsDev, Environment env) throws JSONException {
		String err = "Unable to update the feature. Only a user with the Administrator or Product Lead role can change a subitem that is in the production stage.";
		ValidationResults res = null;

		//validate that the order of sub-features that are on production was not changed:
		JSONArray updatedSubFeaturesInProduction = new JSONArray();
		//create update sub-features list that are on production
		for (int i=0; i<updatedSubItemsArray.size(); i++) {
			JSONObject subFeatureJSONObj = updatedSubItemsArray.getJSONObject(i);
			if (subFeatureJSONObj.containsKey(Constants.JSON_FEATURE_FIELD_STAGE) && subFeatureJSONObj.getString(Constants.JSON_FEATURE_FIELD_STAGE).equals(Stage.PRODUCTION.toString())) {
				updatedSubFeaturesInProduction.add(subFeatureJSONObj);
			}
		}

		//create current sub-features list that are on production
		LinkedList<BaseAirlockItem> featuresOnProduction = new LinkedList<BaseAirlockItem>();
		for (int i=0; i<currentSubItemslist.size(); i++) {
			//JSONObject subFeatureJSONObj = updatedSubFeatures.getJSONObject(i);
			if (currentSubItemslist.get(i) instanceof DataAirlockItem) {
				DataAirlockItem dai = (DataAirlockItem)currentSubItemslist.get(i);
				if (dai.getStage().equals(Stage.PRODUCTION)) {
					featuresOnProduction.add(dai);
				}
			}
		}

		boolean prodOrderChanged = orderChanged (updatedSubFeaturesInProduction, featuresOnProduction, false);
		if (prodOrderChanged && considerProdUnderDevAsDev) {
			//when prod under dev is considered dev check if the new parent or the old parent of moved feature is in production.
			prodOrderChanged = prodOrderChanged && (BaseAirlockItem.isProductionFeature(this, airlockItemsDB) ||
					prodRemovedFromProdParent(updatedSubFeaturesInProduction, featuresOnProduction, false, airlockItemsDB));
		}
		
		if (prodOrderChanged) { 
			if (branch==null || branch.isPartOfExperimentInProduction(context)!=null || orderChanged(updatedSubItemsArray, currentSubItemslist, true)) {
				//if in branch that is not part of exp in production and the order of features is not changed - this is not an production change
				//since in such branch you can move a feature and all of its sub items from prod to dev so the stage changed but the order remains
				return new ValidationResults(err, Status.UNAUTHORIZED);
			}
		}

		for (int i=0; i<updatedSubItemsArray.size(); i++) {
			JSONObject subFeatureJSONObj = updatedSubItemsArray.getJSONObject(i);
			String subFeatureId = subFeatureJSONObj.getString(Constants.JSON_FIELD_UNIQUE_ID);
			BaseAirlockItem feature = airlockItemsDB.get(subFeatureId); //i know that exists since validate succeeded
			//for each sub-feature call to validateProductionDontChanged
			res = feature.validateProductionDontChanged(subFeatureJSONObj, airlockItemsDB, branch, context, considerProdUnderDevAsDev, env);
			if (res!=null)
				return res;
		}				

		return null;				
	}

	//considerProdUnderDev: for prod runtime file - we should consider prod under dev as dev
	//                      for user permissions - we should consider prod under dev as prod 
	public ValidationResults validateProductionDontChanged(JSONObject updatedFeatureData, Map<String, BaseAirlockItem> airlockItemsDB, Branch branch, ServletContext context, boolean considerProdUnderDevAsDev, Environment env) throws JSONException {

		if (featuresItems!=null && env.getRequestType().equals(REQUEST_ITEM_TYPE.FEATURES)) {
			ValidationResults res = validateProductionDontChangedForList (featuresItems, updatedFeatureData.getJSONArray(Constants.JSON_FEATURE_FIELD_FEATURES), airlockItemsDB, branch, context, considerProdUnderDevAsDev, env);
			if (res != null) 
				return res;

		}

		if (configurationRuleItems!=null) {
			ValidationResults res = validateProductionDontChangedForList (configurationRuleItems, updatedFeatureData.getJSONArray(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES), airlockItemsDB, branch, context, considerProdUnderDevAsDev, env);
			if (res != null) 
				return res;

		}
		
		if (orderingRuleItems!=null && updatedFeatureData.containsKey(Constants.JSON_FEATURE_FIELD_ORDERING_RULES) && updatedFeatureData.get(Constants.JSON_FEATURE_FIELD_ORDERING_RULES) != null) {
			ValidationResults res = validateProductionDontChangedForList (orderingRuleItems, updatedFeatureData.getJSONArray(Constants.JSON_FEATURE_FIELD_ORDERING_RULES), airlockItemsDB, branch, context, considerProdUnderDevAsDev, env);
			if (res != null) 
				return res;

		}

		if (entitlementItems!=null && updatedFeatureData.containsKey(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS) && updatedFeatureData.get(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS) != null) {
			ValidationResults res = validateProductionDontChangedForList (entitlementItems, updatedFeatureData.getJSONArray(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS), airlockItemsDB, branch, context, considerProdUnderDevAsDev, env);
			if (res != null) 
				return res;

		}

		if (purchaseOptionsItems!=null && updatedFeatureData.containsKey(Constants.JSON_FEATURE_FIELD_PURCHASE_OPTIONS) && updatedFeatureData.get(Constants.JSON_FEATURE_FIELD_PURCHASE_OPTIONS) != null) {
			ValidationResults res = validateProductionDontChangedForList (purchaseOptionsItems, updatedFeatureData.getJSONArray(Constants.JSON_FEATURE_FIELD_PURCHASE_OPTIONS), airlockItemsDB, branch, context, considerProdUnderDevAsDev, env);
			if (res != null) 
				return res;
		}

		return null;		
	}	
	//true if the stage of a feature that is reported to analytics was changed
	public boolean isAnalyticsChanged (JSONObject updatedFeatureData, Season season, ServletContext context, Environment env) throws JSONException {
		if (featuresItems!=null && env.getRequestType().equals(REQUEST_ITEM_TYPE.FEATURES)) {
			boolean  analyticsChanged = isAnalyticsChangedForList (featuresItems, updatedFeatureData.getJSONArray(Constants.JSON_FEATURE_FIELD_FEATURES), season, context, env);
			if (analyticsChanged) 
				return true;

		}

		if (configurationRuleItems!=null) {
			boolean  analyticsChanged = isAnalyticsChangedForList (configurationRuleItems, updatedFeatureData.getJSONArray(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES), season, context, env);
			if (analyticsChanged) 
				return true;
		}

		if (orderingRuleItems!=null && updatedFeatureData.containsKey(Constants.JSON_FEATURE_FIELD_ORDERING_RULES) && updatedFeatureData.get(Constants.JSON_FEATURE_FIELD_ORDERING_RULES) != null) {
			boolean  analyticsChanged = isAnalyticsChangedForList (orderingRuleItems, updatedFeatureData.getJSONArray(Constants.JSON_FEATURE_FIELD_ORDERING_RULES), season, context, env);
			if (analyticsChanged) 
				return true;
		}
		
		if (entitlementItems!=null && updatedFeatureData.containsKey(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS) && updatedFeatureData.get(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS) != null) {
			boolean  analyticsChanged = isAnalyticsChangedForList (entitlementItems, updatedFeatureData.getJSONArray(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS), season, context, env);
			if (analyticsChanged) 
				return true;
		}
		
		if (purchaseOptionsItems!=null && updatedFeatureData.containsKey(Constants.JSON_FEATURE_FIELD_PURCHASE_OPTIONS) && updatedFeatureData.get(Constants.JSON_FEATURE_FIELD_PURCHASE_OPTIONS) != null) {
			boolean  analyticsChanged = isAnalyticsChangedForList (purchaseOptionsItems, updatedFeatureData.getJSONArray(Constants.JSON_FEATURE_FIELD_PURCHASE_OPTIONS), season, context, env);
			if (analyticsChanged) 
				return true;
		}

		
		return false;
	}

	//return null if valid, ValidationResults otherwise
	//does not change the feature! 
	protected ValidationResults doValidateFeatureJSON(JSONObject featureObj, ServletContext context, String seasonId, LinkedList<String> addedSubFeatures, LinkedList<String> missingSubFeatures, InternalUserGroups userGroups, HashMap<UUID, Integer> existingFeaturesInUpdate, String parent, HashMap<String, JSONObject> updatedFeaturesMap, UserInfo userInfo,
			ValidationCache tester,
			Map<String, BaseAirlockItem> airlockItemsDB, Environment env,List<OriginalString> copiedStings) {
		Action action = Action.ADD;

		//description, owner, internalUserGroups, minAppVersion and additionalInfo, configurationSchema, configuration are optional 
		try {
			//type
			if (!featureObj.containsKey(Constants.JSON_FEATURE_FIELD_TYPE) || featureObj.getString(Constants.JSON_FEATURE_FIELD_TYPE) == null) {
				return new ValidationResults("The type field is missing.", Status.BAD_REQUEST);
			}

			String typeStr = featureObj.getString(Constants.JSON_FEATURE_FIELD_TYPE);
			Type typeObj = strToType(typeStr);
			if (typeObj == null) {
				return new ValidationResults("Illegal feature type: '" + typeStr + "'", Status.BAD_REQUEST);
			}

			//season id
			if (!featureObj.containsKey(Constants.JSON_FEATURE_FIELD_SEASON_ID) || featureObj.get(Constants.JSON_FEATURE_FIELD_SEASON_ID)==null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_SEASON_ID), Status.BAD_REQUEST);
			}

			if (featureObj.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && featureObj.get(Constants.JSON_FIELD_UNIQUE_ID)!=null) {
				//if JSON contains uniqueId - update an existing feature otherwise create a new feature
				action = Action.UPDATE;
			}

			//branchStatus - optional field
			if (featureObj.containsKey(Constants.JSON_FIELD_BRANCH_STATUS) && featureObj.get(Constants.JSON_FIELD_BRANCH_STATUS)!=null) {				
				String branchStatusStr = featureObj.getString(Constants.JSON_FIELD_BRANCH_STATUS);
				BranchStatus branchStatusObj = strToBranchStatus(branchStatusStr);
				if (branchStatusObj == null) {
					return new ValidationResults("Illegal branch status: '" + branchStatusStr + "'", Status.BAD_REQUEST);
				}
			}

			//branchFeaturesItems - optional field
			if (featureObj.containsKey(Constants.JSON_FIELD_BRANCH_FEATURES_ITEMS) && featureObj.get(Constants.JSON_FIELD_BRANCH_FEATURES_ITEMS)!=null) {				
				featureObj.getJSONArray(Constants.JSON_FIELD_BRANCH_FEATURES_ITEMS); //validate that is strings array				
			}

			//branchConfigurationRuleItems - optional field
			if (featureObj.containsKey(Constants.JSON_FIELD_BRANCH_CONFIGURATION_RULE_ITEMS) && featureObj.get(Constants.JSON_FIELD_BRANCH_CONFIGURATION_RULE_ITEMS)!=null) {				
				featureObj.getJSONArray(Constants.JSON_FIELD_BRANCH_CONFIGURATION_RULE_ITEMS); //validate that is strings array				
			}
			
			//branchOrderingRuleItems - optional field
			if (featureObj.containsKey(Constants.JSON_FIELD_BRANCH_ORDERING_RULE_ITEMS) && featureObj.get(Constants.JSON_FIELD_BRANCH_ORDERING_RULE_ITEMS)!=null) {				
				featureObj.getJSONArray(Constants.JSON_FIELD_BRANCH_ORDERING_RULE_ITEMS); //validate that is strings array				
			}
			
			//branchEntitlementItems - optional field
			if (featureObj.containsKey(Constants.JSON_FIELD_BRANCH_ENTITLEMENT_ITEMS) && featureObj.get(Constants.JSON_FIELD_BRANCH_ENTITLEMENT_ITEMS)!=null) {				
				featureObj.getJSONArray(Constants.JSON_FIELD_BRANCH_ENTITLEMENT_ITEMS); //validate that is strings array				
			}
			
			//branchPurchaseOptionsItems - optional field
			if (featureObj.containsKey(Constants.JSON_FIELD_BRANCH_PURCHASE_OPTIONS_ITEMS) && featureObj.get(Constants.JSON_FIELD_BRANCH_PURCHASE_OPTIONS_ITEMS)!=null) {				
				featureObj.getJSONArray(Constants.JSON_FIELD_BRANCH_PURCHASE_OPTIONS_ITEMS); //validate that is strings array				
			}

			if (action == Action.ADD) {		
				//cannot create root feature
				if (typeObj == Type.ROOT) {
					return new ValidationResults("Cannot create ROOT feature.", Status.BAD_REQUEST);
				}

				//modification date => should not appear in feature creation
				if (featureObj.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) && featureObj.get(Constants.JSON_FIELD_LAST_MODIFIED)!=null) {
					return new ValidationResults("The lastModified field cannot be specified during creation.", Status.BAD_REQUEST);
				}

				//branchStatus => should not appear in feature creation
				if (featureObj.containsKey(Constants.JSON_FIELD_BRANCH_STATUS) && featureObj.get(Constants.JSON_FIELD_BRANCH_STATUS)!=null) {
					return new ValidationResults("The branchStatus field cannot be specified during creation.", Status.BAD_REQUEST);
				}

				//sub features are not allowed in add feature (adding only one by one)
				if (featureObj.containsKey(Constants.JSON_FEATURE_FIELD_FEATURES) && featureObj.get(Constants.JSON_FEATURE_FIELD_FEATURES) != null && !featureObj.getJSONArray(Constants.JSON_FEATURE_FIELD_FEATURES).isEmpty()) {
					return new ValidationResults(Strings.featureWithSubfeatures, Status.BAD_REQUEST);
				}

				//sub configuration rules are not allowed in add feature (adding only one by one)
				if (featureObj.containsKey(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES) && featureObj.get(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES) != null && !featureObj.getJSONArray(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES).isEmpty()) {
					return new ValidationResults(Strings.featureWithConfigurations, Status.BAD_REQUEST);
				}

				//branchFeaturesItems => should not appear in feature creation
				if (featureObj.containsKey(Constants.JSON_FIELD_BRANCH_FEATURES_ITEMS) && featureObj.get(Constants.JSON_FIELD_BRANCH_FEATURES_ITEMS)!=null) {
					return new ValidationResults("The branchFeaturesItems field cannot be specified during creation.", Status.BAD_REQUEST);
				}

				//branchConfigurationRuleItems => should not appear in feature creation
				if (featureObj.containsKey(Constants.JSON_FIELD_BRANCH_CONFIGURATION_RULE_ITEMS) && featureObj.get(Constants.JSON_FIELD_BRANCH_CONFIGURATION_RULE_ITEMS)!=null) {
					return new ValidationResults("The branchConfigurationRuleItems field cannot be specified during creation.", Status.BAD_REQUEST);
				}
				
				//branchOrderingRuleItems => should not appear in feature creation
				if (featureObj.containsKey(Constants.JSON_FIELD_BRANCH_ORDERING_RULE_ITEMS) && featureObj.get(Constants.JSON_FIELD_BRANCH_ORDERING_RULE_ITEMS)!=null) {
					return new ValidationResults("The branchOrderingRuleItems field cannot be specified during creation.", Status.BAD_REQUEST);
				}
				
				//branchEntitlementItems => should not appear in feature creation
				if (featureObj.containsKey(Constants.JSON_FIELD_BRANCH_ENTITLEMENT_ITEMS) && featureObj.get(Constants.JSON_FIELD_BRANCH_ENTITLEMENT_ITEMS)!=null) {
					return new ValidationResults("The branchEntitlementItems field cannot be specified during creation.", Status.BAD_REQUEST);
				}
				
				//branchPurchaseOptionsItems => should not appear in feature creation
				if (featureObj.containsKey(Constants.JSON_FIELD_BRANCH_PURCHASE_OPTIONS_ITEMS) && featureObj.get(Constants.JSON_FIELD_BRANCH_PURCHASE_OPTIONS_ITEMS)!=null) {
					return new ValidationResults("The branchPurchaseOptionsItems field cannot be specified during creation.", Status.BAD_REQUEST);
				}
			}
			else {//update
				//modification date must appear
				if (!featureObj.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) || featureObj.get(Constants.JSON_FIELD_LAST_MODIFIED)==null) {
					return new ValidationResults("The lastModified field is missing. This field must be specified during feature update.", Status.BAD_REQUEST);
				}				

				//branchStatus should not be changed in feature update
				if (featureObj.containsKey(Constants.JSON_FIELD_BRANCH_STATUS) && featureObj.get(Constants.JSON_FIELD_BRANCH_STATUS)!=null) {
					String branchStatusStr = featureObj.getString(Constants.JSON_FIELD_BRANCH_STATUS);
					BranchStatus branchStatusObj = strToBranchStatus(branchStatusStr); //already validate 
					if (!branchStatus.equals(branchStatusObj))
						return new ValidationResults("The branchStatus field cannot be updated during update.", Status.BAD_REQUEST);
				}

				//branchFeaturesItems should not be changed in feature update
				if (featureObj.containsKey(Constants.JSON_FIELD_BRANCH_FEATURES_ITEMS) && featureObj.get(Constants.JSON_FIELD_BRANCH_FEATURES_ITEMS)!=null) {
					JSONArray branchFeaturesItemsArr = featureObj.getJSONArray(Constants.JSON_FIELD_BRANCH_FEATURES_ITEMS);
					if (!Utilities.compareStringsArrayToStringsList(branchFeaturesItemsArr, branchFeaturesItems))
						return new ValidationResults("The branchFeaturesItems field cannot be updated during update.", Status.BAD_REQUEST);
				}

				//branchConfigurationRuleItems should not be changed in feature update
				if (featureObj.containsKey(Constants.JSON_FIELD_BRANCH_CONFIGURATION_RULE_ITEMS) && featureObj.get(Constants.JSON_FIELD_BRANCH_CONFIGURATION_RULE_ITEMS)!=null) {
					JSONArray branchConfigurationRuleItemsArr = featureObj.getJSONArray(Constants.JSON_FIELD_BRANCH_CONFIGURATION_RULE_ITEMS);
					if (!Utilities.compareStringsArrayToStringsList(branchConfigurationRuleItemsArr, branchConfigurationRuleItems))
						return new ValidationResults("The branchConfigurationRuleItems field cannot be updated during update.", Status.BAD_REQUEST);
				}

				//branchOrderingRuleItems should not be changed in feature update
				if (featureObj.containsKey(Constants.JSON_FIELD_BRANCH_ORDERING_RULE_ITEMS) && featureObj.get(Constants.JSON_FIELD_BRANCH_ORDERING_RULE_ITEMS)!=null) {
					JSONArray branchOrderingRuleItemsArr = featureObj.getJSONArray(Constants.JSON_FIELD_BRANCH_ORDERING_RULE_ITEMS);
					if (!Utilities.compareStringsArrayToStringsList(branchOrderingRuleItemsArr, branchOrderingRuleItems))
						return new ValidationResults("The branchOrderingRuleItems field cannot be updated during update.", Status.BAD_REQUEST);
				}
				
				//branchEntitlementItems should not be changed in feature update
				if (featureObj.containsKey(Constants.JSON_FIELD_BRANCH_ENTITLEMENT_ITEMS) && featureObj.get(Constants.JSON_FIELD_BRANCH_ENTITLEMENT_ITEMS)!=null) {
					JSONArray branchInAppPurchaseItemsArr = featureObj.getJSONArray(Constants.JSON_FIELD_BRANCH_ENTITLEMENT_ITEMS);
					if (!Utilities.compareStringsArrayToStringsList(branchInAppPurchaseItemsArr, branchEntitlementItems))
						return new ValidationResults("The branchEntitlementItems field cannot be updated during update.", Status.BAD_REQUEST);
				}
				
				//branchPurchaseOptionsItems should not be changed in feature update
				if (featureObj.containsKey(Constants.JSON_FIELD_BRANCH_PURCHASE_OPTIONS_ITEMS) && featureObj.get(Constants.JSON_FIELD_BRANCH_PURCHASE_OPTIONS_ITEMS)!=null) {
					JSONArray branchPurchaseOptionstemsArr = featureObj.getJSONArray(Constants.JSON_FIELD_BRANCH_PURCHASE_OPTIONS_ITEMS);
					if (!Utilities.compareStringsArrayToStringsList(branchPurchaseOptionstemsArr, branchPurchaseOptionsItems))
						return new ValidationResults("The branchPurchaseOptionsItems field cannot be updated during update.", Status.BAD_REQUEST);
				}
				
				//verify that given modification date is not older than current modification date
				long givenModoficationDate = featureObj.getLong(Constants.JSON_FIELD_LAST_MODIFIED);  //verify that legal long
				Date givenDate = new Date(givenModoficationDate);
				if (givenDate.before(lastModified)) {
					return new ValidationResults("The feature was changed by another user. Refresh your browser and try again.", Status.CONFLICT);			
				}

				if (featuresItems != null) {
					//sub features are mandatory - if none exist expect empty list
					if (!featureObj.containsKey(Constants.JSON_FEATURE_FIELD_FEATURES) || featureObj.get(Constants.JSON_FEATURE_FIELD_FEATURES) == null) {
						return new ValidationResults("The features field is missing. This field must be specified during update.", Status.BAD_REQUEST);
					}
					JSONArray updatedSubFeatures = featureObj.getJSONArray(Constants.JSON_FEATURE_FIELD_FEATURES); //validate that is array value

					ValidationResults res = doValidateSubItems(featureObj, updatedSubFeatures, featuresItems, Constants.JSON_FEATURE_FIELD_FEATURES, context, seasonId, addedSubFeatures, missingSubFeatures, userGroups, existingFeaturesInUpdate, parent, updatedFeaturesMap, userInfo, tester, airlockItemsDB, env);
					if (res != null)
						return res;
				}

				if (configurationRuleItems != null) {
					//sub features are mandatory - if none exist expect empty list
					if (!featureObj.containsKey(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES) || featureObj.get(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES) == null) {
						return new ValidationResults("The configurations field is missing. This field must be specified during update.", Status.BAD_REQUEST);
					}
					JSONArray updatedSubConfigRules = featureObj.getJSONArray(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES); //validate that is array value

					ValidationResults res = doValidateSubItems(featureObj, updatedSubConfigRules, configurationRuleItems, Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES, context, seasonId, addedSubFeatures, missingSubFeatures, userGroups, existingFeaturesInUpdate, parent, updatedFeaturesMap, userInfo, tester, airlockItemsDB, env);
					if (res != null)
						return res;
				}
				
				if (orderingRuleItems != null) { //ordering rules is not mandatory
					if (featureObj.containsKey(Constants.JSON_FEATURE_FIELD_ORDERING_RULES) && featureObj.get(Constants.JSON_FEATURE_FIELD_ORDERING_RULES)!=null) { 
						JSONArray updatedSubOrderingRules = featureObj.getJSONArray(Constants.JSON_FEATURE_FIELD_ORDERING_RULES); //validate that is array value
	
						ValidationResults res = doValidateSubItems(featureObj, updatedSubOrderingRules, orderingRuleItems, Constants.JSON_FIELD_BRANCH_ORDERING_RULE_ITEMS, context, seasonId, addedSubFeatures, missingSubFeatures, userGroups, existingFeaturesInUpdate, parent, updatedFeaturesMap, userInfo, tester, airlockItemsDB, env);
						if (res != null)
							return res;
					}
				}

				if (entitlementItems != null) { //in app purchases is not mandatory
					if (featureObj.containsKey(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS) && featureObj.get(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS)!=null) { 
						JSONArray updatedSubEntitlements = featureObj.getJSONArray(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS); //validate that is array value
	
						ValidationResults res = doValidateSubItems(featureObj, updatedSubEntitlements, entitlementItems, Constants.JSON_FIELD_BRANCH_ENTITLEMENT_ITEMS, context, seasonId, addedSubFeatures, missingSubFeatures, userGroups, existingFeaturesInUpdate, parent, updatedFeaturesMap, userInfo, tester, airlockItemsDB, env);
						if (res != null)
							return res;
					}
				}
				
				if (purchaseOptionsItems != null) { //purchase options is not mandatory
					if (featureObj.containsKey(Constants.JSON_FEATURE_FIELD_PURCHASE_OPTIONS) && featureObj.get(Constants.JSON_FEATURE_FIELD_PURCHASE_OPTIONS)!=null) { 
						JSONArray updatedSubPurchaseOptions = featureObj.getJSONArray(Constants.JSON_FEATURE_FIELD_PURCHASE_OPTIONS); //validate that is array value
	
						ValidationResults res = doValidateSubItems(featureObj, updatedSubPurchaseOptions, purchaseOptionsItems, Constants.JSON_FIELD_BRANCH_PURCHASE_OPTIONS_ITEMS, context, seasonId, addedSubFeatures, missingSubFeatures, userGroups, existingFeaturesInUpdate, parent, updatedFeaturesMap, userInfo, tester, airlockItemsDB, env);
						if (res != null)
							return res;
					}
				}


				//type cannot be changed in update
				if (type != typeObj) {
					return new ValidationResults(String.format(Strings.fieldCannotBeChangedDuringUpdate, Constants.JSON_FEATURE_FIELD_TYPE), Status.BAD_REQUEST);
				}	

				//season id must exists and not be changed
				String seasonIdStr = featureObj.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID);
				if (!seasonIdStr.equals(seasonId)) {
					return new ValidationResults(String.format(Strings.fieldCannotBeChangedDuringUpdate, Constants.JSON_FEATURE_FIELD_SEASON_ID), Status.BAD_REQUEST);
				}
				
				if (parent!=null && !parent.equals(this.parent.toString()) && (type.equals(Type.FEATURE) || type.equals(Type.MUTUAL_EXCLUSION_GROUP))) {
					@SuppressWarnings("unchecked")			
					Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);
					
					Season season = seasonsDB.get(seasonId);
					//if parent was changed during update and it previous parent has an ordering rule that refers to the moved item - return error
					ValidationResults res = validatePrevParentOrderingRule(airlockItemsDB, tester, season, context);
					if (res!=null)
						return res;
				}
			}

		} catch (JSONException jsne) {
			return new ValidationResults(jsne.getMessage(), Status.BAD_REQUEST);
		}
		catch (ClassCastException cce) {
			return new ValidationResults("Illegal feature JSON: " + cce.getMessage(), Status.BAD_REQUEST);
		}
		catch (ValidationException ve) {
			return new ValidationResults(ve.getMessage(), Status.BAD_REQUEST);
		}
		catch (GenerationException ge) {
			return new ValidationResults(ge.getMessage(), Status.BAD_REQUEST);
		}
		return null;
	}

	protected ValidationResults validatePrevParentOrderingRule(Map<String, BaseAirlockItem> airlockItemsDB, ValidationCache tester, Season season, ServletContext context) throws JSONException, GenerationException, ValidationException {
		BaseAirlockItem prevParent = airlockItemsDB.get(this.parent.toString());
		Environment env = new Environment();
		env.setValidationCache(tester);
		if (prevParent.getOrderingRuleItems()!=null) {
			for (int i=0; i< prevParent.getOrderingRuleItems().size(); i++) {
				BaseAirlockItem orderingItem = prevParent.getOrderingRuleItems().get(i);
				String foundInOrderingRule = null;
				if (orderingItem instanceof OrderingRuleItem) {
					foundInOrderingRule = ((OrderingRuleItem)orderingItem).containsSubFeaure(uniqueId.toString(), season, context, env);
				}
				if (orderingItem instanceof OrderingRuleMutualExclusionGroupItem) {
					foundInOrderingRule = ((OrderingRuleMutualExclusionGroupItem)orderingItem).containsSubFeaure(uniqueId.toString(), season, context, env);
				}
				
				if (foundInOrderingRule!=null) {
					return new ValidationResults("The feature " + getNameSpaceDotName() + " cannot be moved. It is referenced in the " + foundInOrderingRule + " ordering rule.", Status.BAD_REQUEST);
				}
				
			}
		}
		return null;
	}
	
	private ValidationResults doValidateSubItems (JSONObject featureObj, JSONArray updatedSubFeatures, LinkedList<BaseAirlockItem> curSubFeatures, String updatedSubFeaturesType, ServletContext context, String seasonId, LinkedList<String> addedSubFeatures, LinkedList<String> missingSubFeatures, InternalUserGroups userGroups, HashMap<UUID, Integer> existingFeaturesInUpdate, String parent, HashMap<String, JSONObject> updatedFeaturesMap, UserInfo userInfo,
			ValidationCache tester,
			Map<String, BaseAirlockItem> airlockItemsDB, Environment env) throws JSONException {
		if (existingFeaturesInUpdate.containsKey(uniqueId)) {
			return new ValidationResults("A feature with id " + uniqueId.toString() + " appears more than once in the input data.", Status.BAD_REQUEST);
		}

		for (int i=0; i<updatedSubFeatures.size(); i++) {
			JSONObject updatedSubFeature = updatedSubFeatures.getJSONObject(i);

			//verify that none of the given sub features is new (uniqueId is missing)
			if (!updatedSubFeature.containsKey(Constants.JSON_FIELD_UNIQUE_ID) || updatedSubFeature.get(Constants.JSON_FIELD_UNIQUE_ID)==null) {
				return new ValidationResults("Cannot add new features during feature update.", Status.BAD_REQUEST);
			}

			//verify that sub feature is in an allowed type according to parents type
			if (!updatedSubFeature.containsKey(Constants.JSON_FEATURE_FIELD_TYPE) || updatedSubFeature.get(Constants.JSON_FEATURE_FIELD_TYPE)==null) {
				return new ValidationResults("The type field is missing.", Status.BAD_REQUEST);					
			}
			String typeStr = updatedSubFeature.getString(Constants.JSON_FEATURE_FIELD_TYPE);
			Type updatedSubFeatureType = strToType(typeStr);
			if (updatedSubFeatureType!=null) {
				if (updatedSubFeaturesType.equals(Constants.JSON_FEATURE_FIELD_FEATURES) &&
						(updatedSubFeatureType.equals(Type.CONFIG_MUTUAL_EXCLUSION_GROUP) || updatedSubFeatureType.equals(Type.CONFIGURATION_RULE)) ) {
					return new ValidationResults("A configuration item cannot reside in the features array.", Status.BAD_REQUEST);	
				}

				if (updatedSubFeaturesType.equals(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES) &&
						(updatedSubFeatureType.equals(Type.MUTUAL_EXCLUSION_GROUP) || updatedSubFeatureType.equals(Type.FEATURE)) ) {
					return new ValidationResults("A feature item cannot reside in the configurations array.", Status.BAD_REQUEST);	
				}								
			}	

			String updatedSubFeatureId = updatedSubFeature.getString(Constants.JSON_FIELD_UNIQUE_ID);

			if (isNewSubAirlockItem(updatedSubFeatureId, null)) { 
				//verify that added sub features are of the same season as the parent
				if (!seasonId.toString().equals(featureObj.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID))) {
					return new ValidationResults("One of the subfeatures is not in the current season.", Status.BAD_REQUEST);
				}

				//verify that added sub feature is not with that same id as the parent.
				if (uniqueId.toString().equals(updatedSubFeatureId)) {
					return new ValidationResults("A subfeature cannot be the same as its parent.", Status.BAD_REQUEST);
				}
				
				//cannot move an item under an unchecked out item (NONE). The root is an exception - a new feature can be added/moved
				//under the root.
				if (!env.isInMaster() && branchStatus.equals(BranchStatus.NONE) && !type.equals(Type.ROOT)) {

					return new ValidationResults ("You cannot move an item under an item that is not checked out. First,check out the parent item.", Status.BAD_REQUEST);

				}

				addedSubFeatures.add (updatedSubFeatureId);
			}
		}

		//verify that no original sub features are missing from the updated sub features list.
		//accumulate in missing features in list and verify that not added somewhere else in the tree later on.
		listMissingSubAirlockItems (updatedSubFeatures, curSubFeatures, missingSubFeatures);

		//the root is an exception - feature can be added to it even when it is not checked out - the order is ignored
		if (!env.isInMaster() && branchStatus.equals(BranchStatus.NONE) &&orderChangedIgnoreMissing(updatedSubFeatures, curSubFeatures) && !type.equals(Type.ROOT)) {

			return new ValidationResults("You cannot change the order of sub-items under an item that is not checked out. First, check out the parent item.", Status.BAD_REQUEST);

		}

		//validate sub features JSON
		for (int i=0; i<updatedSubFeatures.size(); i++) {
			JSONObject updatedSubFeature = updatedSubFeatures.getJSONObject(i);
			BaseAirlockItem featureToUpdate = airlockItemsDB.get(updatedSubFeature.get(Constants.JSON_FIELD_UNIQUE_ID));
			if (featureToUpdate == null) {
				return new ValidationResults("Subfeature " + updatedSubFeature.get(Constants.JSON_FIELD_UNIQUE_ID) + " does not exist.", Status.BAD_REQUEST);
			}
			ValidationResults validationRes = featureToUpdate.doValidateFeatureJSON(updatedSubFeature, context, seasonId, addedSubFeatures, missingSubFeatures, userGroups, existingFeaturesInUpdate, this.uniqueId.toString(), updatedFeaturesMap, userInfo, tester, airlockItemsDB, env,new ArrayList<OriginalString>());
			if (validationRes!=null)
				return validationRes;
		}

		return null;
	}

	public void fromJSON (JSONObject input, Map<String, BaseAirlockItem> airlockItemsDB, UUID parent, Environment env) throws JSONException {
		if (input.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && input.get(Constants.JSON_FIELD_UNIQUE_ID) != null) {
			String sStr = input.getString(Constants.JSON_FIELD_UNIQUE_ID);			
			uniqueId = UUID.fromString(sStr);		
		}	

		this.parent = parent;

		if (input.containsKey(Constants.JSON_FEATURE_FIELD_TYPE))
			type = strToType(input.getString(Constants.JSON_FEATURE_FIELD_TYPE));

		if (input.containsKey(Constants.JSON_FEATURE_FIELD_SEASON_ID) && input.get(Constants.JSON_FEATURE_FIELD_SEASON_ID) != null) {
			String sStr = input.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID);			
			seasonId = UUID.fromString(sStr);			
		}

		if (input.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) && input.get(Constants.JSON_FIELD_LAST_MODIFIED)!=null) { 
			long timeInMS = input.getLong(Constants.JSON_FIELD_LAST_MODIFIED);
			lastModified = new Date(timeInMS);
		}  else {
			lastModified = new Date();
		}

		if (input.containsKey(Constants.JSON_FIELD_BRANCH_FEATURE_PARENT_NAME) && input.get(Constants.JSON_FIELD_BRANCH_FEATURE_PARENT_NAME)!=null)  
			branchFeatureParentName = input.getString(Constants.JSON_FIELD_BRANCH_FEATURE_PARENT_NAME);			

		if (input.containsKey(Constants.JSON_FIELD_BRANCH_FEATURES_ITEMS) && input.get(Constants.JSON_FIELD_BRANCH_FEATURES_ITEMS)!=null)  
			branchFeaturesItems = Utilities.stringJsonArrayToStringsList(input.getJSONArray(Constants.JSON_FIELD_BRANCH_FEATURES_ITEMS));			

		if (input.containsKey(Constants.JSON_FIELD_BRANCH_CONFIGURATION_RULE_ITEMS) && input.get(Constants.JSON_FIELD_BRANCH_CONFIGURATION_RULE_ITEMS)!=null)  
			branchConfigurationRuleItems = Utilities.stringJsonArrayToStringsList(input.getJSONArray(Constants.JSON_FIELD_BRANCH_CONFIGURATION_RULE_ITEMS));			

		if (input.containsKey(Constants.JSON_FIELD_BRANCH_ORDERING_RULE_ITEMS) && input.get(Constants.JSON_FIELD_BRANCH_ORDERING_RULE_ITEMS)!=null)  
			branchOrderingRuleItems = Utilities.stringJsonArrayToStringsList(input.getJSONArray(Constants.JSON_FIELD_BRANCH_ORDERING_RULE_ITEMS));			

		if (input.containsKey(Constants.JSON_FIELD_BRANCH_ENTITLEMENT_ITEMS) && input.get(Constants.JSON_FIELD_BRANCH_ENTITLEMENT_ITEMS)!=null)  
			branchEntitlementItems = Utilities.stringJsonArrayToStringsList(input.getJSONArray(Constants.JSON_FIELD_BRANCH_ENTITLEMENT_ITEMS));			

		if (input.containsKey(Constants.JSON_FIELD_BRANCH_PURCHASE_OPTIONS_ITEMS) && input.get(Constants.JSON_FIELD_BRANCH_PURCHASE_OPTIONS_ITEMS)!=null)  
			branchPurchaseOptionsItems = Utilities.stringJsonArrayToStringsList(input.getJSONArray(Constants.JSON_FIELD_BRANCH_PURCHASE_OPTIONS_ITEMS));			

		if (input.containsKey(Constants.JSON_FIELD_BRANCH_STATUS) && input.get(Constants.JSON_FIELD_BRANCH_STATUS)!=null)  
			branchStatus = Utilities.strToBranchStatus(input.getString(Constants.JSON_FIELD_BRANCH_STATUS));			

		if (input.containsKey(Constants.JSON_FEATURE_FIELD_FEATURES) && input.get(Constants.JSON_FEATURE_FIELD_FEATURES)!=null) {
			JSONArray featuresJSONArr = (input.getJSONArray(Constants.JSON_FEATURE_FIELD_FEATURES));
			if (featuresJSONArr != null && featuresJSONArr.size()>0) {
				for (int i=0; i<featuresJSONArr.size(); i++) {
					JSONObject featureJSONObj = featuresJSONArr.getJSONObject(i);
					BaseAirlockItem alItem = getAirlockItemByType(featureJSONObj);
					if (alItem==null) {
						String errMsg = Strings.typeNotFound;
						logger.severe(errMsg);
						throw new JSONException(errMsg);
					}
					alItem.fromJSON(featureJSONObj, airlockItemsDB, uniqueId, env);					
					featuresItems.add(alItem);					
					if (airlockItemsDB!=null)
						airlockItemsDB.put(alItem.getUniqueId().toString(), alItem);
				}
			}
		}

		if (input.containsKey(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES) && input.get(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES)!=null) {
			JSONArray configsJSONArr = (input.getJSONArray(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES));
			if (configsJSONArr != null && configsJSONArr.size()>0) {
				for (int i=0; i<configsJSONArr.size(); i++) {
					JSONObject configJSONObj =  configsJSONArr.getJSONObject(i);
					BaseAirlockItem alItem = getAirlockItemByType(configJSONObj);
					if (alItem==null) {
						String errMsg = Strings.typeNotFound;
						logger.severe(errMsg);
						throw new JSONException(errMsg);
					}
					alItem.fromJSON(configJSONObj, airlockItemsDB, uniqueId, env);					
					configurationRuleItems.add(alItem);					
					if (airlockItemsDB!=null)
						airlockItemsDB.put(alItem.getUniqueId().toString(), alItem);
				}
			}
		}
		
		//this is used only for ordering rule conversion of configuration from string to json. This will be added to the map later on as well
		if (airlockItemsDB!=null && this.getUniqueId()!=null)
			airlockItemsDB.put(this.getUniqueId().toString(), this);
		
		if (input.containsKey(Constants.JSON_FEATURE_FIELD_ORDERING_RULES) && input.get(Constants.JSON_FEATURE_FIELD_ORDERING_RULES)!=null) {
			JSONArray orderingRulesJSONArr = (input.getJSONArray(Constants.JSON_FEATURE_FIELD_ORDERING_RULES));
			if (orderingRulesJSONArr != null && orderingRulesJSONArr.size()>0) {
				for (int i=0; i<orderingRulesJSONArr.size(); i++) {
					JSONObject configJSONObj =  orderingRulesJSONArr.getJSONObject(i);
					BaseAirlockItem alItem = getAirlockItemByType(configJSONObj);
					if (alItem==null) {
						String errMsg = Strings.typeNotFound;
						logger.severe(errMsg);
						throw new JSONException(errMsg);
					}
					alItem.fromJSON(configJSONObj, airlockItemsDB, uniqueId, env);					
					orderingRuleItems.add(alItem);					
					if (airlockItemsDB!=null)
						airlockItemsDB.put(alItem.getUniqueId().toString(), alItem);
				}
			}
		}
		
		if (input.containsKey(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS) && input.get(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS)!=null) {
			JSONArray entitlementsJSONArr = (input.getJSONArray(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS));
			if (entitlementsJSONArr != null && entitlementsJSONArr.size()>0) {
				for (int i=0; i<entitlementsJSONArr.size(); i++) {
					JSONObject entitlementJSONObj =  entitlementsJSONArr.getJSONObject(i);
					BaseAirlockItem alItem = getAirlockItemByType(entitlementJSONObj);
					if (alItem==null) {
						String errMsg = Strings.typeNotFound;
						logger.severe(errMsg);
						throw new JSONException(errMsg);
					}
					alItem.fromJSON(entitlementJSONObj, airlockItemsDB, uniqueId, env);					
					entitlementItems.add(alItem);					
					if (airlockItemsDB!=null)
						airlockItemsDB.put(alItem.getUniqueId().toString(), alItem);
				}
			}
		}
		
		if (input.containsKey(Constants.JSON_FEATURE_FIELD_PURCHASE_OPTIONS) && input.get(Constants.JSON_FEATURE_FIELD_PURCHASE_OPTIONS)!=null) {
			JSONArray purchaseOptionsJSONArr = (input.getJSONArray(Constants.JSON_FEATURE_FIELD_PURCHASE_OPTIONS));
			if (purchaseOptionsJSONArr != null && purchaseOptionsJSONArr.size()>0) {
				for (int i=0; i<purchaseOptionsJSONArr.size(); i++) {
					JSONObject purchaseOptionsJSONObj =  purchaseOptionsJSONArr.getJSONObject(i);
					BaseAirlockItem alItem = getAirlockItemByType(purchaseOptionsJSONObj);
					if (alItem==null) {
						String errMsg = Strings.typeNotFound;
						logger.severe(errMsg);
						throw new JSONException(errMsg);
					}
					alItem.fromJSON(purchaseOptionsJSONObj, airlockItemsDB, uniqueId, env);					
					purchaseOptionsItems.add(alItem);					
					if (airlockItemsDB!=null)
						airlockItemsDB.put(alItem.getUniqueId().toString(), alItem);
				}
			}
		}
	}

	public void toConstantsFile(HashMap<String, StringBuilder> namespaceStrBuilderMap, Platform platform, boolean inBranch, TreeSet<String> listedFeatures) {
		if (featuresItems!=null) {						
			for (int i=0; i<featuresItems.size(); i++) {
				featuresItems.get(i).toConstantsFile(namespaceStrBuilderMap, platform, inBranch, listedFeatures);				
			}
		}
		
		if (entitlementItems!=null) {						
			for (int i=0; i<entitlementItems.size(); i++) {
				entitlementItems.get(i).toConstantsFile(namespaceStrBuilderMap, platform, inBranch, listedFeatures);				
			}
		}
		
		if (purchaseOptionsItems!=null) {						
			for (int i=0; i<purchaseOptionsItems.size(); i++) {
				purchaseOptionsItems.get(i).toConstantsFile(namespaceStrBuilderMap, platform, inBranch, listedFeatures);				
			}
		}
	}

	public JSONObject toJson(OutputJSONMode mode, ServletContext context, Environment env) throws JSONException {
		return  toJson(mode,context,env,null);
	}

	public JSONObject toJson(OutputJSONMode mode, ServletContext context, Environment env,UserInfo userInfo) throws JSONException {
		return  toJson(mode,context,env,userInfo, FeatureFilter.NONE);
	}


	//the relevant analytics and airlockItemsDB are set as the environment properties
	public JSONObject toJson(OutputJSONMode mode, ServletContext context, Environment env,UserInfo userInfo, FeatureFilter featureFilter) throws JSONException {
		if (mode == OutputJSONMode.RUNTIME_PRODUCTION) {
			if (type == Type.FEATURE || type == Type.CONFIGURATION_RULE || type == Type.ORDERING_RULE || type == Type.ENTITLEMENT || type == Type.PURCHASE_OPTIONS) {
				DataAirlockItem dataAlItem = (DataAirlockItem) this;
				if (dataAlItem.getStage() == Stage.DEVELOPMENT)
					return null;
			}
		}


		JSONObject res = new JSONObject();

		res.put(Constants.JSON_FIELD_UNIQUE_ID, uniqueId==null?null:uniqueId.toString());

		res.put(Constants.JSON_FEATURE_FIELD_TYPE, type.toString());

		if (branchFeatureParentName!=null)
			res.put(Constants.JSON_FIELD_BRANCH_FEATURE_PARENT_NAME, branchFeatureParentName);

		if (branchFeaturesItems!=null) {
			if (featuresItems!=null && mode.equals(OutputJSONMode.RUNTIME_PRODUCTION) && 
					branchStatus.equals(BranchStatus.CHECKED_OUT) && isProductionFeature(this, env.getAirlockItemsDB())) {
				//in runtime production - if a checked-out feature is in production and has sub features in dev they wont be in the tree and 
				//shouldn't be in the branch features items list
				List<String> withoutDevChildren = Utilities.cloneStringsList(branchFeaturesItems);
				for (BaseAirlockItem subFeature:featuresItems) {
					if (!isProductionFeature(subFeature, env.getAirlockItemsDB())) {
						removeSubFeatureFromList(withoutDevChildren, Branch.getItemBranchName(subFeature));
					}
					
				}
				res.put(Constants.JSON_FIELD_BRANCH_FEATURES_ITEMS, withoutDevChildren);
			}
			else {
				res.put(Constants.JSON_FIELD_BRANCH_FEATURES_ITEMS, branchFeaturesItems);
			}
		}

		if (branchConfigurationRuleItems!=null) {
			if (configurationRuleItems!=null && mode.equals(OutputJSONMode.RUNTIME_PRODUCTION) &&
					branchStatus.equals(BranchStatus.CHECKED_OUT) && isProductionFeature(this, env.getAirlockItemsDB())) {
				//in runtime production - if a chceked-out feature is in production and has sub features in dev they wont be in the tree and 
				//shouldn't be in the branch faetures items list
				List<String> withoutDevChildren = Utilities.cloneStringsList(branchConfigurationRuleItems);
				for (BaseAirlockItem subConfig:configurationRuleItems) {
					if (!isProductionFeature(subConfig, env.getAirlockItemsDB())) {
						removeSubFeatureFromList(withoutDevChildren, Branch.getItemBranchName(subConfig));
					}
					
				}
				res.put(Constants.JSON_FIELD_BRANCH_CONFIGURATION_RULE_ITEMS, withoutDevChildren);
			}
			else {
				res.put(Constants.JSON_FIELD_BRANCH_CONFIGURATION_RULE_ITEMS, branchConfigurationRuleItems);
			}
		}
		
		if (branchOrderingRuleItems!=null) {
			if (orderingRuleItems!=null && mode.equals(OutputJSONMode.RUNTIME_PRODUCTION) &&
					branchStatus.equals(BranchStatus.CHECKED_OUT) && isProductionFeature(this, env.getAirlockItemsDB())) {
				//in runtime production - if a chceked-out feature is in production and has sub features in dev they wont be in the tree and 
				//shouldn't be in the branch faetures items list
				List<String> withoutDevChildren = Utilities.cloneStringsList(branchOrderingRuleItems);
				for (BaseAirlockItem subConfig:orderingRuleItems) {
					if (!isProductionFeature(subConfig, env.getAirlockItemsDB())) {
						removeSubFeatureFromList(withoutDevChildren, Branch.getItemBranchName(subConfig));
					}
					
				}
				res.put(Constants.JSON_FIELD_BRANCH_ORDERING_RULE_ITEMS, withoutDevChildren);
			}
			else {
				res.put(Constants.JSON_FIELD_BRANCH_ORDERING_RULE_ITEMS, branchOrderingRuleItems);
			}
		}
		
		if (branchEntitlementItems!=null) {
			if (entitlementItems!=null && mode.equals(OutputJSONMode.RUNTIME_PRODUCTION) &&
					branchStatus.equals(BranchStatus.CHECKED_OUT) && isProductionFeature(this, env.getAirlockItemsDB())) {
				//in runtime production - if a chceked-out feature is in production and has sub features in dev they wont be in the tree and 
				//shouldn't be in the branch faetures items list
				List<String> withoutDevChildren = Utilities.cloneStringsList(branchEntitlementItems);
				for (BaseAirlockItem subConfig:entitlementItems) {
					if (!isProductionFeature(subConfig, env.getAirlockItemsDB())) {
						removeSubFeatureFromList(withoutDevChildren, Branch.getItemBranchName(subConfig));
					}
					
				}
				res.put(Constants.JSON_FIELD_BRANCH_ENTITLEMENT_ITEMS, withoutDevChildren);
			}
			else {
				res.put(Constants.JSON_FIELD_BRANCH_ENTITLEMENT_ITEMS, branchEntitlementItems);
			}
		}
		
		if (branchPurchaseOptionsItems!=null) {
			if (purchaseOptionsItems!=null && mode.equals(OutputJSONMode.RUNTIME_PRODUCTION) &&
					branchStatus.equals(BranchStatus.CHECKED_OUT) && isProductionFeature(this, env.getAirlockItemsDB())) {
				//in runtime production - if a chceked-out feature is in production and has sub features in dev they wont be in the tree and 
				//shouldn't be in the branch faetures items list
				List<String> withoutDevChildren = Utilities.cloneStringsList(branchEntitlementItems);
				for (BaseAirlockItem subConfig:purchaseOptionsItems) {
					if (!isProductionFeature(subConfig, env.getAirlockItemsDB())) {
						removeSubFeatureFromList(withoutDevChildren, Branch.getItemBranchName(subConfig));
					}
					
				}
				res.put(Constants.JSON_FIELD_BRANCH_PURCHASE_OPTIONS_ITEMS, withoutDevChildren);
			}
			else {
				res.put(Constants.JSON_FIELD_BRANCH_PURCHASE_OPTIONS_ITEMS, branchPurchaseOptionsItems);
			}
		}

		if (branchStatus!=null && !mode.equals(OutputJSONMode.DEFAULTS))
			res.put(Constants.JSON_FIELD_BRANCH_STATUS, branchStatus.toString());


		JSONArray featuresJSONArr = new JSONArray();
		if (featuresItems!=null) {
			for (int i = 0; i < featuresItems.size(); i++) {
				if (featureFilter.isConditionSatisfied(featuresItems.get(i))) {
					JSONObject alItemJSONObj = featuresItems.get(i).toJson(mode, context, env, userInfo);
					if (alItemJSONObj == null)
						continue;
					featuresJSONArr.add(alItemJSONObj);
				}
			}
			res.put(Constants.JSON_FEATURE_FIELD_FEATURES, featuresJSONArr);
		}

		JSONArray configJSONArr = new JSONArray();
		if (configurationRuleItems!=null && mode != OutputJSONMode.DEFAULTS) { //dont ping configRules in defaults mode
			for (int i=0; i<configurationRuleItems.size(); i++) {
				JSONObject alItemJSONObj = configurationRuleItems.get(i).toJson(mode, context, env, userInfo);
				if (alItemJSONObj == null)
					continue;
				configJSONArr.add(alItemJSONObj);
			}
			res.put(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES, configJSONArr);			
		}
		
		JSONArray orderingRulesJSONArr = new JSONArray();
		if (orderingRuleItems!=null && mode != OutputJSONMode.DEFAULTS) { //dont ping configRules in defaults mode
			for (int i=0; i<orderingRuleItems.size(); i++) {
				JSONObject alItemJSONObj = orderingRuleItems.get(i).toJson(mode, context, env, userInfo);
				if (alItemJSONObj == null)
					continue;
				orderingRulesJSONArr.add(alItemJSONObj);
			}
			res.put(Constants.JSON_FEATURE_FIELD_ORDERING_RULES, orderingRulesJSONArr);			
		}

		JSONArray entitlementsJSONArr = new JSONArray();
		if (entitlementItems!=null) { 
			for (int i=0; i<entitlementItems.size(); i++) {
				JSONObject alItemJSONObj = entitlementItems.get(i).toJson(mode, context, env, userInfo);
				if (alItemJSONObj == null)
					continue;
				entitlementsJSONArr.add(alItemJSONObj);
			}
			res.put(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS, entitlementsJSONArr);			
		}
		
		JSONArray purchaseOptionsJSONArr = new JSONArray();
		if (purchaseOptionsItems!=null) {
			for (int i=0; i<purchaseOptionsItems.size(); i++) {
				JSONObject alItemJSONObj = purchaseOptionsItems.get(i).toJson(mode, context, env, userInfo);
				if (alItemJSONObj == null)
					continue;
				purchaseOptionsJSONArr.add(alItemJSONObj);
			}
			res.put(Constants.JSON_FEATURE_FIELD_PURCHASE_OPTIONS, purchaseOptionsJSONArr);			
		}

		if ( mode == OutputJSONMode.RUNTIME_PRODUCTION) {
			if (type == Type.CONFIG_MUTUAL_EXCLUSION_GROUP || type == Type.MUTUAL_EXCLUSION_GROUP || type == Type.ORDERING_RULE_MUTUAL_EXCLUSION_GROUP) {
				if (featuresJSONArr.size() == 0 && configJSONArr.size() == 0 && orderingRulesJSONArr.size() == 0) {
					return null; //in runtime_production mode don't add mutual exclusion group that dont have sub items in production
				}
			}
		}

		if (mode == OutputJSONMode.ADMIN || mode == OutputJSONMode.DISPLAY) { 
			res.put(Constants.JSON_FIELD_LAST_MODIFIED, lastModified.getTime());
			res.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, seasonId==null?null:seasonId.toString());
		}

		if(mode != OutputJSONMode.DEFAULTS) {
			if (AnalyticsServices.isAnalyticsSupported(env) && mode != OutputJSONMode.ADMIN && env.getAnalytics()!=null) { //add analytics data only on display mode (dont write it to S3)
				//Season season = seasonsDB.get(seasonId.toString());
				GlobalDataCollection globaldataCollection = env.getAnalytics().getGlobalDataCollection();
				if (globaldataCollection.getAnalyticsDataCollection().getFeaturesOnOffMap().containsKey(uniqueId.toString())) {
					res.put(Constants.JSON_FIELD_SEND_TO_ANALYTICS, true);				
				}
			}
		}

		return res;		
	}

	private void removeSubFeatureFromList(List<String> withoutDevChildrenBranchSubItemsList, String itemBranchName) {
		for (int i=0; i<withoutDevChildrenBranchSubItemsList.size(); i++) {
			if (withoutDevChildrenBranchSubItemsList.get(i).equals(itemBranchName)) {
				withoutDevChildrenBranchSubItemsList.remove(i);
				return;
			}
		}		
	}
	
	//List type should be feature or configRule. This way we know what branch child list should be updated
	public LinkedList<BaseAirlockItem> updateAirlockItemPerList(JSONArray updatedSubFeatures, LinkedList<BaseAirlockItem> mergedSubFeatures, LinkedList<BaseAirlockItem> currentSubFeatures, 
			Map<String, BaseAirlockItem> airlockItemsDB, BaseAirlockItem root,List<ChangeDetails> updateDetails,Environment env, 
			Branch branch, Type listType, Boolean isProdChange, ServletContext context, Map<String, Stage> updatedBranchesMap) throws JSONException {
		boolean wasChanged = false;

		REQUEST_ITEM_TYPE reqType = REQUEST_ITEM_TYPE.FEATURES;
		if (listType.equals(Type.ENTITLEMENT) || listType.equals(Type.PURCHASE_OPTIONS)) {
			reqType = REQUEST_ITEM_TYPE.ENTITLEMENTS;
		}
		
		LinkedList<BaseAirlockItem> resultSubFeatures = new LinkedList<BaseAirlockItem>();
		//for each sub feature - check if was added if so: remove from prv parent and set modif there. add to current list
		for (int i=0; i<updatedSubFeatures.size(); i++) {
			JSONObject subFeatureJSONObj = updatedSubFeatures.getJSONObject(i);
			String subFeatureId = subFeatureJSONObj.getString(Constants.JSON_FIELD_UNIQUE_ID);			

			BaseAirlockItem feature = airlockItemsDB.get(subFeatureId); //i know that exists since validate succeeded
			//parent id should be taken from feature in the tree even in branches because  in branch can be null
			//but in tree is correct
			UUID previousParentId = feature.getParent(); 

			if (branch!=null && branch.getBranchAirlockItemsBD().containsKey(subFeatureId)) {
				//in branch: take the feature to update from branch items db since the itemsDb we are using is a clone of the features in 
				//the branch and no the features themselves. 
				feature = branch.getBranchAirlockItemsBD().get(subFeatureId);				
			}

			//for each sub feature call for update
			updateDetails.addAll (feature.updateAirlockItem(subFeatureJSONObj, airlockItemsDB, root,env, branch,isProdChange, context, updatedBranchesMap));

			String subItemBranchName=null;
			if (!env.isInMaster()) {
				subItemBranchName = Branch.getItemBranchName(feature);
			}

			//verify if was added
			if (isNewSubAirlockItem(subFeatureId, subItemBranchName)) {				
				if (previousParentId == null || airlockItemsDB.get(previousParentId.toString()).getType().equals(Type.ROOT)) { //previous parent was root
					if (!env.isInMaster()) {
						//in master - if none feature moved under none root - it is added to a list of subFeatures for further merge
						if (feature.getBranchStatus().equals(BranchStatus.NONE)) {
							//if root is none in branch, this feature will be removed from list, if root not none - the function will do nothing							
							branch.removeNonCheckedOutFeatureFromNonCheckedOutRoot(this, root, reqType);
						}
						else {
							BaseAirlockItem branchMovedFeature = airlockItemsDB.get(subFeatureId);
							if (!branchMovedFeature.getBranchStatus().equals(BranchStatus.NONE)) { //c_o or new moved from root
								branch.removeNewAndCheckedOutFeatureFromNonCheckedOutRoot(branchMovedFeature, root);
							}
						}
					}
				}

				BaseAirlockItem prevParent = airlockItemsDB.get(previousParentId.toString());
				if (prevParent != null) {						
					if (!env.isInMaster()) {
						if (!prevParent.getBranchStatus().equals(BranchStatus.NONE)) {						
							//On new/checkedOut feature in branch should do the action on the real instance of the feature
							//as well.
							BaseAirlockItem prevBranchParent = branch.getBranchAirlockItemsBD().get(previousParentId.toString());
							Branch.removeFromBranchChildrenList (prevBranchParent, feature);
							prevBranchParent.removeAirlockItem(UUID.fromString(subFeatureId));
							prevBranchParent.setLastModified(new Date());
						}
						
						//if the moved item was in the branch subTrees root - remove from there since is moved to another subTree
						branch.removeFeaturesSubTreeFromBranch(feature.getUniqueId(), env.getRequestType());

						//if the current parent is the root - which means a feature was added to root: If the status is non
						//add it to the noneRootNonechekedoutChildren list
						if (type.equals(Type.ROOT) && branchStatus.equals(BranchStatus.NONE)) {
							if(feature.getBranchStatus().equals(BranchStatus.NONE)) {
								branch.addNonCheckedOutFeatureUnderNonCheckedOutRoot(feature, this, reqType);
							}
							else {
								//the feature is a subtree root in the branch since checkedOut or new child of unchecked out root 
								branch.addSubTreeRoot(feature);								
							}
						}
						
						//if none configurationRule is moved under checked out or new item in branch- check it out
						if (!branchStatus.equals(BranchStatus.NONE) && feature.getBranchStatus().equals(BranchStatus.NONE) &&
								(feature instanceof ConfigurationRuleItem || feature instanceof ConfigMutualExclusionGroupItem)) {
							ValidationCache tester = new ValidationCache();
							branch.checkoutConfig(uniqueId, feature, context, tester);
						}
						
						//if none orderingRule is moved under checked out or new item in branch- check it out
						if (!branchStatus.equals(BranchStatus.NONE) && feature.getBranchStatus().equals(BranchStatus.NONE) &&
								(feature instanceof OrderingRuleItem || feature instanceof OrderingRuleMutualExclusionGroupItem)) {
							ValidationCache tester = new ValidationCache();
							branch.checkoutConfig(uniqueId, feature, context, tester);
						}
					} 

					prevParent.removeAirlockItem(UUID.fromString(subFeatureId));
					prevParent.setLastModified(new Date());	
				}
				
				ChangeDetails change;
				if(this instanceof DataAirlockItem) {
					change = new ChangeDetails("Feature " + subFeatureId + " moved from parent feature " + previousParentId.toString() + " to parent feature " + uniqueId.toString() + ";	", this, isProdChange);
				}
				else {
					change = new ChangeDetails("Feature " + subFeatureId + " moved from parent feature " + previousParentId.toString() + " to parent feature " + uniqueId.toString() + ";	", this);
				}
				updateDetails.add(change);
				feature.setParent(uniqueId);
				wasChanged = true;
			}						
		}

		//for now order matters for both m_e_g and regular feature 
		//was changed = true if new feature added
		if (wasChanged || orderChanged (updatedSubFeatures, mergedSubFeatures, branch!=null)) {
			addAuditMsgForOrderChange(updatedSubFeatures, mergedSubFeatures, updateDetails,isProdChange);
			//create new features list - add them one by one and switch with original list
			LinkedList<BaseAirlockItem> newfeatures = new LinkedList<BaseAirlockItem>();
			for (int i=0; i<updatedSubFeatures.size(); i++) {					
				String id  = updatedSubFeatures.getJSONObject(i).getString(Constants.JSON_FIELD_UNIQUE_ID);										
				BaseAirlockItem curFeature = airlockItemsDB.get(id);
				if (branch!=null && branch.getBranchAirlockItemsBD().containsKey(id)) {
					//in branch: take the feature to update from branch items db since the itemsDb we are using is a clone of the features in 
					//the branch and no the features themselves. 
					curFeature = branch.getBranchAirlockItemsBD().get(id);				
				}
				curFeature.setParent(uniqueId);
				newfeatures.add(curFeature);
				if (!branchStatus.equals(BranchStatus.NONE)) {
					//if the item is in the branch - set the sub item's parent to null since it is not the root of subTree in the branch 
					curFeature.setBranchFeatureParentName(null);
				}
			}			
			
			resultSubFeatures = newfeatures;
			wasChanged = true;
			if (!env.isInMaster()) {
				//in branch - children order changed: set relevant branchChildrenList 
				if (listType.equals(Type.FEATURE)) {
					if (branchFeaturesItems == null) {
						branchFeaturesItems=new LinkedList<String>();
					}
					else {					
						branchFeaturesItems.clear();
					}

					for (int i=0; i<resultSubFeatures.size(); i++) {
						branchFeaturesItems.add(Branch.getItemBranchName(resultSubFeatures.get(i)));
					}
				}
				else if (listType.equals(Type.CONFIGURATION_RULE)){
					//CONFIGURATION_RULE
					if (branchConfigurationRuleItems == null) {
						branchConfigurationRuleItems=new LinkedList<String>();
					}
					else {					
						branchConfigurationRuleItems.clear();
					}

					for (int i=0; i<resultSubFeatures.size(); i++) {
						branchConfigurationRuleItems.add(Branch.getItemBranchName(resultSubFeatures.get(i)));
					}
				}
				else if (listType.equals(Type.ORDERING_RULE)){
					//ORDERING_RULE
					if (branchOrderingRuleItems == null) {
						branchOrderingRuleItems=new LinkedList<String>();
					}
					else {					
						branchOrderingRuleItems.clear();
					}

					for (int i=0; i<resultSubFeatures.size(); i++) {
						branchOrderingRuleItems.add(Branch.getItemBranchName(resultSubFeatures.get(i)));
					}
				} 
				else if (listType.equals(Type.ENTITLEMENT)){
					//ENTITLEMENT
					if (branchEntitlementItems == null) {
						branchEntitlementItems=new LinkedList<String>();
					}
					else {					
						branchEntitlementItems.clear();
					}

					for (int i=0; i<resultSubFeatures.size(); i++) {
						branchEntitlementItems.add(Branch.getItemBranchName(resultSubFeatures.get(i)));
					}
					
				}
				else if (listType.equals(Type.PURCHASE_OPTIONS)){
					//PURCHASE_OPTIONS
					if (branchPurchaseOptionsItems == null) {
						branchPurchaseOptionsItems=new LinkedList<String>();
					}
					else {					
						branchPurchaseOptionsItems.clear();
					}

					for (int i=0; i<resultSubFeatures.size(); i++) {
						branchPurchaseOptionsItems.add(Branch.getItemBranchName(resultSubFeatures.get(i)));
					}
				}

				//in branch - for new/checkedOut feature - keep only the new/checkedOut subFeatues in the list
				if (!branchStatus.equals(BranchStatus.NONE)) {
					LinkedList<BaseAirlockItem> branchSubFeatures = new LinkedList<BaseAirlockItem>();
					for (int i=0; i<resultSubFeatures.size(); i++) {
						if (!resultSubFeatures.get(i).getBranchStatus().equals(BranchStatus.NONE)) {
							branchSubFeatures.add(resultSubFeatures.get(i));
						}
					}
					resultSubFeatures = branchSubFeatures;
				}
			}
		}		

		if (wasChanged) {
			lastModified = new Date();
			return resultSubFeatures;
		}
		else {
			return currentSubFeatures;
		}

	}

	//If is in master: branch=null
	//Return a string with update details.
	//If nothing was changed - return empty list	
	public List<ChangeDetails> updateAirlockItem(JSONObject updatedAirlockItemData, Map<String, BaseAirlockItem> airlockItemsDB, BaseAirlockItem root,Environment env, Branch branch,Boolean isProdChange, ServletContext context, Map<String, Stage> updatedBranchesMap) throws JSONException {
		List<ChangeDetails> updateDetails = new ArrayList<>();

		if (featuresItems!=null) {
			JSONArray updatedSubFeatures = updatedAirlockItemData.getJSONArray(Constants.JSON_FEATURE_FIELD_FEATURES);

			featuresItems = updateAirlockItemPerList (updatedSubFeatures, airlockItemsDB.get(uniqueId.toString()).getFeaturesItems(), featuresItems, airlockItemsDB, root, updateDetails, env, branch,Type.FEATURE,isProdChange, context, updatedBranchesMap);
		}

		if (configurationRuleItems!=null) {
			JSONArray updatedSubConfiges = updatedAirlockItemData.getJSONArray(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES);

			configurationRuleItems = updateAirlockItemPerList (updatedSubConfiges, airlockItemsDB.get(uniqueId.toString()).getConfigurationRuleItems(), configurationRuleItems,  airlockItemsDB, root, updateDetails, env, branch, Type.CONFIGURATION_RULE, isProdChange, context, updatedBranchesMap);
		}

		if (orderingRuleItems!=null && updatedAirlockItemData.containsKey(Constants.JSON_FEATURE_FIELD_ORDERING_RULES) && updatedAirlockItemData.get(Constants.JSON_FEATURE_FIELD_ORDERING_RULES) != null) {
			JSONArray updatedSubOrderingRules = updatedAirlockItemData.getJSONArray(Constants.JSON_FEATURE_FIELD_ORDERING_RULES);

			orderingRuleItems = updateAirlockItemPerList (updatedSubOrderingRules, airlockItemsDB.get(uniqueId.toString()).getOrderingRuleItems(), orderingRuleItems,  airlockItemsDB, root, updateDetails, env, branch, Type.ORDERING_RULE, isProdChange, context, updatedBranchesMap);
		}

		if (entitlementItems!=null && updatedAirlockItemData.containsKey(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS) && updatedAirlockItemData.get(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS) != null) {
			JSONArray updatedSubEntitlements = updatedAirlockItemData.getJSONArray(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS);

			entitlementItems = updateAirlockItemPerList (updatedSubEntitlements, airlockItemsDB.get(uniqueId.toString()).getEntitlementItems(), entitlementItems,  airlockItemsDB, root, updateDetails, env, branch, Type.ENTITLEMENT, isProdChange, context, updatedBranchesMap);
		}

		if (purchaseOptionsItems!=null && updatedAirlockItemData.containsKey(Constants.JSON_FEATURE_FIELD_PURCHASE_OPTIONS) && updatedAirlockItemData.get(Constants.JSON_FEATURE_FIELD_PURCHASE_OPTIONS) != null) {
			JSONArray updatedSubPurchaseOptions = updatedAirlockItemData.getJSONArray(Constants.JSON_FEATURE_FIELD_PURCHASE_OPTIONS);

			purchaseOptionsItems = updateAirlockItemPerList (updatedSubPurchaseOptions, airlockItemsDB.get(uniqueId.toString()).getPurchaseOptionsItems(), purchaseOptionsItems,  airlockItemsDB, root, updateDetails, env, branch, Type.PURCHASE_OPTIONS, isProdChange, context, updatedBranchesMap);
		}


		return updateDetails;
	}

	protected void addAuditMsgForOrderChange(JSONArray updatedSubFeatures, LinkedList<BaseAirlockItem> curSubFeatures, List<ChangeDetails> updateDetails,Boolean isProdChange) throws JSONException {
		StringBuilder details = new StringBuilder();
		String featureType = this.getObjTypeStrByType();
		String featureName = "";
		if(this instanceof DataAirlockItem){
			featureName = getNameSpaceDotName();
		}
		details.append("The order of sub-items under "+featureType + " "+featureName+"(" + uniqueId.toString() + ") changed.\n\nBefore:\n");
		ArrayList<DataAirlockItem> subFeaturesToNotify = new ArrayList<>();
		for (int i=0; i<curSubFeatures.size(); i++) {
			details.append(curSubFeatures.get(i).getNameSpaceDotName() + "(" + curSubFeatures.get(i).getUniqueId().toString() + ")\n");
		}
		details.append("\n After:\n");
		for (int i=0; i<updatedSubFeatures.size(); i++) {

			JSONObject subFeature = updatedSubFeatures.getJSONObject(i);
			details.append(getNameDotNamespaceFromJson(subFeature) + "(" + subFeature.getString(Constants.JSON_FIELD_UNIQUE_ID) +")\n");
			// if Feature or config rule
			if (subFeature.containsKey(Constants.JSON_FEATURE_FIELD_TYPE) && subFeature.get(Constants.JSON_FEATURE_FIELD_TYPE)!=null
					&& (subFeature.getString(Constants.JSON_FEATURE_FIELD_TYPE).equals("FEATURE")) || (subFeature.getString(Constants.JSON_FEATURE_FIELD_TYPE).equals("CONFIGURATION"))) {
				BaseAirlockItem item=null;
				int previousPosition = -1;
				String id = subFeature.getString(Constants.JSON_FIELD_UNIQUE_ID);
				for (int j=0; j<curSubFeatures.size(); j++) {
					if(curSubFeatures.get(j).getUniqueId().toString().equals(id)){
						item = curSubFeatures.get(j);
						previousPosition = j;
						break;
					}
				}
				if(previousPosition != i && item instanceof DataAirlockItem) {
					subFeaturesToNotify.add((DataAirlockItem) item);
					//updateDetails.add(new ChangeDetails("This element was reordered from position " + (previousPosition + 1) + " to position " + (i + 1), item, ((DataAirlockItem) item).getStage().toString().equals("PRODUCTION")));
				}
			}
		}

		details.append("\n");
		if(this instanceof DataAirlockItem) {
			ChangeDetails reoderDetails = new ChangeDetails(details.toString(), this, isProdChange);
			reoderDetails.setSubfeatures(subFeaturesToNotify);
			updateDetails.add(reoderDetails);

		}
		else {
			updateDetails.add(new ChangeDetails(details.toString(), this));
		}
	}

	private static String getNameDotNamespaceFromJson(JSONObject updatedSubFeature) throws JSONException {
		String res = "";
		if (updatedSubFeature.containsKey(Constants.JSON_FEATURE_FIELD_NAMESPACE) && updatedSubFeature.get(Constants.JSON_FEATURE_FIELD_NAMESPACE)!=null) {
			res = updatedSubFeature.get(Constants.JSON_FEATURE_FIELD_NAMESPACE) + ".";
		}

		if (updatedSubFeature.containsKey(Constants.JSON_FIELD_NAME) && updatedSubFeature.get(Constants.JSON_FIELD_NAME)!=null) {
			res += updatedSubFeature.get(Constants.JSON_FIELD_NAME);
		}

		if (res.isEmpty()) {
			if (updatedSubFeature.containsKey(Constants.JSON_FEATURE_FIELD_TYPE) && updatedSubFeature.get(Constants.JSON_FEATURE_FIELD_TYPE)!=null) {
				res += updatedSubFeature.get(Constants.JSON_FEATURE_FIELD_TYPE);
			}
		}

		return res;
	}

	protected boolean prodRemovedFromProdParent(JSONArray updatedSubFeatures,  LinkedList<BaseAirlockItem> origAirlockItems, boolean inBranch, Map<String, BaseAirlockItem> airlockItemsDB) throws JSONException {
		JSONArray newUpdatedSubFeatures = new JSONArray();
		for (int i=0;i<updatedSubFeatures.size(); i++) {
			if (!inBranch || branchStatus.equals(BranchStatus.NONE) || !updatedSubFeatures.getJSONObject(i).getString(Constants.JSON_FIELD_BRANCH_STATUS).equals(BranchStatus.NONE.toString())) {
				//in branch remove the items with status none from updatedSubFeatures list
				newUpdatedSubFeatures.add(updatedSubFeatures.getJSONObject(i));
			}
		}

		if (origAirlockItems.size() < newUpdatedSubFeatures.size()) {
			for (int i=0; i<newUpdatedSubFeatures.size(); i++) {
				String updatedItemUniqueId = newUpdatedSubFeatures.getJSONObject(i).getString(Constants.JSON_FIELD_UNIQUE_ID);
				boolean isNew = true;
				for (int j=0;j<origAirlockItems.size(); j++) {
					if (origAirlockItems.get(j).getUniqueId().toString().equals(updatedItemUniqueId)) {
						isNew = false;
						break;
					}
				}
				if (isNew) {
					BaseAirlockItem item = airlockItemsDB.get(updatedItemUniqueId);
					if (item.getParent()!=null) {
						if (item.getBranchFeatureParentName()!=null && item.getBranchFeatureParentName().equals(Constants.ROOT_FEATURE)) {
							return true; //if prod feature moved from root - it is considered as if its parent was prod => prod change
						}
						
						BaseAirlockItem parentItem = airlockItemsDB.get(item.getParent().toString());
					
						if (BaseAirlockItem.isProductionFeature(parentItem, airlockItemsDB))
							return true;
					}
					else {
						return true; //if prod feature moved from root - it is considered as if its parent was prod => prod change
					}
				}
			}
		}

		return false;
	}


	protected boolean orderChanged(JSONArray updatedSubFeatures,  LinkedList<BaseAirlockItem> origAirlockItems, boolean inBranch) throws JSONException {
		JSONArray newUpdatedSubFeatures = new JSONArray();
		for (int i=0;i<updatedSubFeatures.size(); i++) {
			if (!inBranch || branchStatus.equals(BranchStatus.NONE) || !updatedSubFeatures.getJSONObject(i).getString(Constants.JSON_FIELD_BRANCH_STATUS).equals(BranchStatus.NONE.toString())) {
				//in branch remove the items with status none from updatedSubFeatures list
				newUpdatedSubFeatures.add(updatedSubFeatures.getJSONObject(i));
			}
		}

		if (origAirlockItems.size() != newUpdatedSubFeatures.size()) {
			return true; //features added hence order changed
		}

		for (int i=0; i<origAirlockItems.size(); i++) {
			if (!origAirlockItems.get(i).getUniqueId().toString().equals(newUpdatedSubFeatures.getJSONObject(i).getString(Constants.JSON_FIELD_UNIQUE_ID))) {
				return true;
			}
		}

		return false;
	}

	protected boolean orderChangedIgnoreMissing(JSONArray updatedSubFeatures, LinkedList<BaseAirlockItem> origAirlockItems) throws JSONException {
		if (origAirlockItems.size() < updatedSubFeatures.size()) {
			return true; //features added hence order changed
		}

		if (origAirlockItems.size() == updatedSubFeatures.size()) { //sizes are equal - the order shoudl be the same
			for (int i=0; i<origAirlockItems.size(); i++) { 
				if (!origAirlockItems.get(i).getUniqueId().toString().equals(updatedSubFeatures.getJSONObject(i).getString(Constants.JSON_FIELD_UNIQUE_ID))) {
					return true;
				}
			}
		}
		else {
			List<String> before = new LinkedList<String>();
			for (int i=0; i<origAirlockItems.size(); i++) { 
				before.add(origAirlockItems.get(i).getUniqueId().toString());
			}
			List<String> after = new LinkedList<String>();
			for (int i=0; i<updatedSubFeatures.size(); i++) {
				after.add(updatedSubFeatures.getJSONObject(i).getString(Constants.JSON_FIELD_UNIQUE_ID));
			}

			TreeSet<String> set1= new TreeSet<String>(before);
			TreeSet<String> set2= new TreeSet<String>(after);
			for(String str: set1) {
				if(!set2.contains(str)) {
					before.remove(str);
				}
			}

			return !before.equals(after);			
		}

		return false;
	}

	public abstract BaseAirlockItem duplicate (String minVersion, UUID newSeasonId, UUID parentId, 
			Map<String, BaseAirlockItem> airlockItemsDB, HashMap<String, String> oldToDuplicatedFeaturesId, ServletContext context, 
			boolean duplicateSubFeatures, boolean createNewId, ValidationCache tester);

	public static boolean isPurchaseItem(Type type) {
		if (type.equals(Type.PURCHASE_OPTIONS) || type.equals(Type.PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP) || 
				type.equals(Type.ENTITLEMENT) || type.equals(Type.ENTITLEMENT_MUTUAL_EXCLUSION_GROUP) ||
				type.equals(Type.CONFIGURATION_RULE) || type.equals(Type.CONFIG_MUTUAL_EXCLUSION_GROUP)) {
			return true;
		}
		return false;
	}
	
	public static boolean isOnlyPurchaseItem(Type type) {
		if (type.equals(Type.PURCHASE_OPTIONS) || type.equals(Type.PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP) || 
				type.equals(Type.ENTITLEMENT) || type.equals(Type.ENTITLEMENT_MUTUAL_EXCLUSION_GROUP)) {
			return true;
		}
		return false;
	}
	
	//if no type exists return null
	public static BaseAirlockItem getAirlockItemByType (JSONObject input) throws JSONException {
		Type type = null;
		if (input.containsKey(Constants.JSON_FEATURE_FIELD_TYPE) && input.get(Constants.JSON_FEATURE_FIELD_TYPE)!=null)
			type = strToType(input.getString(Constants.JSON_FEATURE_FIELD_TYPE));

		if (type==null)
			return null;

		switch (type) {
			case FEATURE:
				return new FeatureItem();
	
			case MUTUAL_EXCLUSION_GROUP:
				return new MutualExclusionGroupItem();
	
			case ROOT:
				return new RootItem();
	
			case CONFIGURATION_RULE:
				return new ConfigurationRuleItem();
	
			case CONFIG_MUTUAL_EXCLUSION_GROUP:
				return new ConfigMutualExclusionGroupItem();
	
			case ORDERING_RULE:
				return new OrderingRuleItem();
				
			case ORDERING_RULE_MUTUAL_EXCLUSION_GROUP:
				return new OrderingRuleMutualExclusionGroupItem();
				
			case ENTITLEMENT:
				return new EntitlementItem();
				
			case ENTITLEMENT_MUTUAL_EXCLUSION_GROUP:
				return new EntitlementMutualExclusionGroupItem();
				
			case PURCHASE_OPTIONS:
				return new PurchaseOptionsItem();
				
			case PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP:
				return new PurchaseOptionsMutualExclusionGroupItem();
				
			default: return null;
		}
	}

	protected boolean isNewSubAirlockItem(String subItemId, String subItemBranchName) {
		if (featuresItems!=null) {
			for (int i=0; i<featuresItems.size(); i++) {
				if (subItemId.equals(featuresItems.get(i).getUniqueId().toString())) {
					return false;
				}				
			}
		}

		if (configurationRuleItems!=null) {
			for (int i=0; i<configurationRuleItems.size(); i++) {
				if (subItemId.equals(configurationRuleItems.get(i).getUniqueId().toString())) {
					return false;
				}
			}
		}
		
		if (orderingRuleItems!=null) {
			for (int i=0; i<orderingRuleItems.size(); i++) {
				if (subItemId.equals(orderingRuleItems.get(i).getUniqueId().toString())) {
					return false;
				}
			}
		}
		
		if (entitlementItems!=null) {
			for (int i=0; i<entitlementItems.size(); i++) {
				if (subItemId.equals(entitlementItems.get(i).getUniqueId().toString())) {
					return false;
				}
			}
		}
		
		if (purchaseOptionsItems!=null) {
			for (int i=0; i<purchaseOptionsItems.size(); i++) {
				if (subItemId.equals(purchaseOptionsItems.get(i).getUniqueId().toString())) {
					return false;
				}
			}
		}

		if (subItemBranchName!=null) { //in branch the subItem don't have to be directly in the list - it can be just listed in the branch subItems list. (if none)
			if (branchFeaturesItems!=null) {
				for (int i=0; i<branchFeaturesItems.size(); i++) {
					if (subItemBranchName.equals(branchFeaturesItems.get(i))) {
						return false;
					}				
				}
			}

			if (branchConfigurationRuleItems!=null) {
				for (int i=0; i<branchConfigurationRuleItems.size(); i++) {
					if (subItemBranchName.equals(branchConfigurationRuleItems.get(i))) {
						return false;
					}
				}
			}
			
			if (branchOrderingRuleItems!=null) {
				for (int i=0; i<branchOrderingRuleItems.size(); i++) {
					if (subItemBranchName.equals(branchOrderingRuleItems.get(i))) {
						return false;
					}
				}
			}
			
			if (branchEntitlementItems!=null) {
				for (int i=0; i<branchEntitlementItems.size(); i++) {
					if (subItemBranchName.equals(branchEntitlementItems.get(i))) {
						return false;
					}
				}
			}
			
			if (branchPurchaseOptionsItems!=null) {
				for (int i=0; i<branchPurchaseOptionsItems.size(); i++) {
					if (subItemBranchName.equals(branchPurchaseOptionsItems)) {
						return false;
					}
				}
			}
		}
		return true;
	}

	//return null if no such type
	public static Type strToType(String typeStr) {
		if (typeStr == null) 
			return null;

		if(typeStr.equalsIgnoreCase(Type.FEATURE.toString())) 
			return Type.FEATURE;

		if(typeStr.equalsIgnoreCase(Type.ROOT.toString())) 
			return Type.ROOT;

		if(typeStr.equalsIgnoreCase(Type.MUTUAL_EXCLUSION_GROUP.toString())) 
			return Type.MUTUAL_EXCLUSION_GROUP;

		if(typeStr.equalsIgnoreCase(Type.CONFIG_MUTUAL_EXCLUSION_GROUP.toString())) 
			return Type.CONFIG_MUTUAL_EXCLUSION_GROUP;

		if(typeStr.equalsIgnoreCase(Type.CONFIGURATION_RULE.toString())) 
			return Type.CONFIGURATION_RULE;

		if(typeStr.equalsIgnoreCase(Type.ORDERING_RULE.toString())) 
			return Type.ORDERING_RULE;
		
		if(typeStr.equalsIgnoreCase(Type.ORDERING_RULE_MUTUAL_EXCLUSION_GROUP.toString())) 
			return Type.ORDERING_RULE_MUTUAL_EXCLUSION_GROUP;
		
		if(typeStr.equalsIgnoreCase(Type.ENTITLEMENT.toString())) 
			return Type.ENTITLEMENT;
		
		if(typeStr.equalsIgnoreCase(Type.ENTITLEMENT_MUTUAL_EXCLUSION_GROUP.toString())) 
			return Type.ENTITLEMENT_MUTUAL_EXCLUSION_GROUP;
		
		if(typeStr.equalsIgnoreCase(Type.PURCHASE_OPTIONS.toString())) 
			return Type.PURCHASE_OPTIONS;
		
		if(typeStr.equalsIgnoreCase(Type.PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP.toString())) 
			return Type.PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP;
		
		return null; 
	}

	//return null if no such type
	public static BranchStatus strToBranchStatus(String branchStatusStr) {
		if (branchStatusStr == null) 
			return null;

		if(branchStatusStr.equalsIgnoreCase(BranchStatus.NONE.toString())) 
			return BranchStatus.NONE;

		if(branchStatusStr.equalsIgnoreCase(BranchStatus.CHECKED_OUT.toString())) 
			return BranchStatus.CHECKED_OUT;

		if(branchStatusStr.equalsIgnoreCase(BranchStatus.NEW.toString())) 
			return BranchStatus.NEW;

		return null; 
	}

	protected void listMissingSubAirlockItems (JSONArray updatedSubFeatures,  LinkedList<BaseAirlockItem> curList, LinkedList<String> missingSubFeatures) throws JSONException {
		if (curList !=null) {
			if (curList.size() == 0)
				return; //if original features list is empty => none can be missing

			for (int i=0; i<curList.size(); i++) {
				String existingIdStr = curList.get(i).getUniqueId().toString();
				boolean found = false;
				for (int j=0;j<updatedSubFeatures.size();j++) {
					//i am sure that the UniqueId field exists in the updated JSON because i've already verified that no now feature added.
					String updatedIdStr = (updatedSubFeatures.getJSONObject(j)).getString(Constants.JSON_FIELD_UNIQUE_ID);
					if (existingIdStr.equals(updatedIdStr)) {
						found = true;
						break;
					}
				}
				if (!found)
					missingSubFeatures.add(existingIdStr); //no feature with the existing id exists in the updated list
			}
		}

	}

	public void removeFromAirlockItemsDB(Map<String, BaseAirlockItem> airlockItemsDB,ServletContext context, UserInfo userInfo) {
		if (featuresItems!=null) {
			for (int i=0; i<featuresItems.size(); i++) {
				featuresItems.get(i).removeFromAirlockItemsDB(airlockItemsDB,context,userInfo);
			}
		}

		if (configurationRuleItems!=null) {
			for (int i=0; i<configurationRuleItems.size(); i++) {
				configurationRuleItems.get(i).removeFromAirlockItemsDB(airlockItemsDB,context,userInfo);
			}
		}
		
		if (orderingRuleItems!=null) {
			for (int i=0; i<orderingRuleItems.size(); i++) {
				orderingRuleItems.get(i).removeFromAirlockItemsDB(airlockItemsDB,context,userInfo);
			}
		}
		
		if (entitlementItems!=null) {
			for (int i=0; i<entitlementItems.size(); i++) {
				entitlementItems.get(i).removeFromAirlockItemsDB(airlockItemsDB,context,userInfo);
			}
		}
		
		if (purchaseOptionsItems!=null) {
			for (int i=0; i<purchaseOptionsItems.size(); i++) {
				purchaseOptionsItems.get(i).removeFromAirlockItemsDB(airlockItemsDB,context,userInfo);
			}
		}
		
		if(this instanceof DataAirlockItem) {
			FeatureItem featureToNotify = null;
			Boolean isProduction = ((DataAirlockItem) this).getStage().toString().equals("PRODUCTION");
			ArrayList<String> followers = null;
			@SuppressWarnings("unchecked")
			Map<String, ArrayList<String>> followersFeaturesDB = (Map<String, ArrayList<String>>)context.getAttribute(Constants.FOLLOWERS_FEATURES_DB_PARAM_NAME);
			if(this.getType() == Type.FEATURE || this.getType() == Type.ENTITLEMENT || this.getType() == Type.PURCHASE_OPTIONS){
				featureToNotify = (FeatureItem) this;
				ArrayList<String> followersList = followersFeaturesDB.get(featureToNotify.getUniqueId().toString());
				followers = followersList;
			}
			else if(this.getType() == Type.CONFIGURATION_RULE) {
				BaseAirlockItem configParent = airlockItemsDB.get(this.getParent().toString());
				//while (configParent.getType() != Type.FEATURE) {
				while (!(configParent instanceof FeatureItem)) { //FeatureItem, entitlementItem, purchaseOperationItem
					configParent = airlockItemsDB.get(configParent.getParent().toString());
				}
				featureToNotify = (FeatureItem)configParent;
				ArrayList<String> followersList = followersFeaturesDB.get(featureToNotify.getUniqueId().toString());
				followers = followersList;

			}
			else if(this.getType() == Type.ORDERING_RULE) {
				BaseAirlockItem orderingRuleParent = airlockItemsDB.get(this.getParent().toString());
				while (orderingRuleParent.getType() != Type.FEATURE && orderingRuleParent.getType() != Type.ROOT ) {
					orderingRuleParent = airlockItemsDB.get(orderingRuleParent.getParent().toString());
				}
				if (orderingRuleParent.getType() == Type.FEATURE) {
					featureToNotify = (FeatureItem)orderingRuleParent;
					ArrayList<String> followersList = followersFeaturesDB.get(featureToNotify.getUniqueId().toString());
					followers = followersList;
				}
			}
			if (featureToNotify!=null) {
				String details = "The " + this.getObjTypeStrByType() + " " + ((DataAirlockItem) this).getNameSpaceDotName() + " was deleted \n";
				Utilities.sendEmailForDataItem(context,featureToNotify,followers,details,null,null,isProduction,userInfo,null );
			}
		}
		airlockItemsDB.remove(uniqueId.toString());
	}

	public void doGetStringsInUseByItem(Set<String> stringIds, BaseAirlockItem alItem, Set<String> stringsInUseByConfigList, Set<String> stringsInUseByUtilList, Season season,Boolean recursive, boolean includeUtilities) {
		if (alItem.getType() == Type.CONFIGURATION_RULE) {
			ConfigurationRuleItem crItem = (ConfigurationRuleItem)alItem;
			stringsInUseByConfigList.addAll(VerifyRule.findAllTranslationIds(stringIds, crItem.getConfiguration(), false));
			if (includeUtilities) {
				String javascriptFunctions = season.getUtilities().generateUtilityCodeSectionForStageAndType(crItem.getStage(), null, null, null, UtilityType.MAIN_UTILITY);
				stringsInUseByUtilList.addAll(VerifyRule.findAllTranslationIds(stringIds, javascriptFunctions, false));
			}
		}

		if (alItem.getConfigurationRuleItems()!=null) {
			for (int i=0; i<alItem.getConfigurationRuleItems().size(); i++) {
				doGetStringsInUseByItem(stringIds, alItem.getConfigurationRuleItems().get(i), stringsInUseByConfigList, stringsInUseByUtilList, season, recursive, includeUtilities);
			}
		}
		if(recursive && alItem.getFeaturesItems() != null){
			for (int i=0; i<alItem.getFeaturesItems().size(); i++) {
				doGetStringsInUseByItem(stringIds, alItem.getFeaturesItems().get(i), stringsInUseByConfigList, stringsInUseByUtilList, season, recursive, includeUtilities);
			}
		}
		if(recursive && alItem.getEntitlementItems() != null){
			for (int i=0; i<alItem.getEntitlementItems().size(); i++) {
				doGetStringsInUseByItem(stringIds, alItem.getEntitlementItems().get(i), stringsInUseByConfigList, stringsInUseByUtilList, season, recursive, includeUtilities);
			}
		}
		if(recursive && alItem.getPurchaseOptionsItems() != null){
			for (int i=0; i<alItem.getPurchaseOptionsItems().size(); i++) {
				doGetStringsInUseByItem(stringIds, alItem.getPurchaseOptionsItems().get(i), stringsInUseByConfigList, stringsInUseByUtilList, season, recursive, includeUtilities);
			}
		}
	}

	public JSONArray find(FeatureFilter filter, JSONArray resArray){
		if (filter.isConditionSatisfied(this)) {
			resArray.add(uniqueId.toString());
		}

		if (featuresItems!=null) {
			for (int i=0; i<featuresItems.size(); i++) {
				featuresItems.get(i).find(filter, resArray);
			}
		}
		if (configurationRuleItems!=null) {
			for (int i=0; i<configurationRuleItems.size(); i++) {
				configurationRuleItems.get(i).find(filter, resArray);
			}
		}
		if (orderingRuleItems!=null) {
			for (int i=0; i<orderingRuleItems.size(); i++) {
				orderingRuleItems.get(i).find(filter, resArray);
			}
		}
		if (entitlementItems!=null) {
			for (int i=0; i<entitlementItems.size(); i++) {
				entitlementItems.get(i).find(filter, resArray);
			}
		}
		if (purchaseOptionsItems!=null) {
			for (int i=0; i<purchaseOptionsItems.size(); i++) {
				purchaseOptionsItems.get(i).find(filter, resArray);
			}
		}
		return resArray;
	}

	public String getNameSpaceDotName() {
		if (type == Type.FEATURE || type == Type.CONFIGURATION_RULE || type == Type.ORDERING_RULE || type == Type.ENTITLEMENT || type == Type.PURCHASE_OPTIONS) {
			return ((DataAirlockItem)this).namespace+"."+((DataAirlockItem)this).name;
		}
		return type.toString();
	}

	public String getObjTypeStrByType() {
		switch (type){
		case CONFIG_MUTUAL_EXCLUSION_GROUP:
			return "mutual exclusion config";
		case ORDERING_RULE_MUTUAL_EXCLUSION_GROUP:
			return "ordering rule mutual exclusion";
		case CONFIGURATION_RULE:
			return "configuration";
		case FEATURE:
			return "feature";
		case MUTUAL_EXCLUSION_GROUP:
			return "mutual exclusion";
		case ROOT:
			return "root";
		case ORDERING_RULE:
			return "ordering rule";
		case PURCHASE_OPTIONS:
			return "purchase options";
		case ENTITLEMENT:
			return "entitlement";
		case PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP:
			return "purchase options mutual exclusion";
		case ENTITLEMENT_MUTUAL_EXCLUSION_GROUP:
			return "entitlement mutual exclusion";
		default:
			return "";
		}
	}
	
	public static boolean hasDevelopmentAncesstor(BaseAirlockItem airlockItem, Map<String, BaseAirlockItem> airlockItemsDB) {
		UUID parentId = airlockItem.getParent();
		while (parentId!=null) {
			BaseAirlockItem parentItem = airlockItemsDB.get(parentId.toString());
			if (parentItem instanceof DataAirlockItem && ((DataAirlockItem)parentItem).getStage().equals(Stage.DEVELOPMENT)) {
				return true; //if one of the feature's ancestors is in development - it is counted as a development feature.
			}
			parentId = parentItem.getParent();
		}
		
		return false; 
	}
	
	//check if the feature is in production or one of its sub features are in production
	public static boolean isProductionFeature(BaseAirlockItem airlockItem, Map<String, BaseAirlockItem> airlockItemsDB) {
		/*UUID parentId = airlockItem.getParent();
		while (parentId!=null) {
			BaseAirlockItem parentItem = airlockItemsDB.get(parentId.toString());
			if (parentItem instanceof DataAirlockItem && ((DataAirlockItem)parentItem).getStage().equals(Stage.DEVELOPMENT)) {
				return false; //if one of the feature's ancestors is in development - it is counted as a development feature.
			}
			parentId = parentItem.getParent();
		}*/
		if (hasDevelopmentAncesstor(airlockItem, airlockItemsDB)) {
			return false;
		}
		
		if (airlockItem instanceof DataAirlockItem) {
			if (((DataAirlockItem)airlockItem).getStage().equals(Stage.PRODUCTION)) {
				return true;
			}
		}
		else {
			if (airlockItem.containSubItemInProductionStage()) {
				return true;
			}
		}

		return false;
	}
	private boolean isAnalyticsChangedForList(LinkedList<BaseAirlockItem> currentSubItemslist, JSONArray updatedSubItemsArray, Season season, ServletContext context, Environment env) throws JSONException {
		
		for (int i=0; i<updatedSubItemsArray.size(); i++) {
			JSONObject subFeatureJSONObj = updatedSubItemsArray.getJSONObject(i);
			String subFeatureId = subFeatureJSONObj.getString(Constants.JSON_FIELD_UNIQUE_ID);
			BaseAirlockItem feature = env.getAirlockItemsDB().get(subFeatureId); //i know that exists since validate succeeded
			//for each sub-feature call to validateProductionDontChanged
			boolean  analyticsChanged = feature.isAnalyticsChanged(subFeatureJSONObj, season, context, env);
			if (analyticsChanged) 
				return true;
		}				

		return false;				
	}
	//return the ordering rule name in which the feature exists.
	//If does not exists at all - return null. 
	public String appearsInOrderingRule(Season season, ServletContext context, Environment env, boolean deletedSubItem) throws MergeException {		
		if (!(this instanceof FeatureItem) && !(this instanceof MutualExclusionGroupItem)) {
			return null; //only mtx or feature can appear in ordering rule
		}
		
		if (!deletedSubItem) { //this is the root feature of the deleted tree - check that doesn't appear in his parent's ordering rule
			Map<String, BaseAirlockItem> airlockItemsDB = env.getAirlockItemsDB();
			BaseAirlockItem parentObject = airlockItemsDB.get(parent.toString());
			if ((parentObject instanceof FeatureItem || parentObject instanceof MutualExclusionGroupItem) && 
					parentObject.getOrderingRuleItems()!=null && parentObject.getOrderingRuleItems().size()>0) {
				for (int i=0;i<parentObject.getOrderingRuleItems().size(); i++) {
					
					try {
						BaseAirlockItem orderingRule = parentObject.getOrderingRuleItems().get(i);
						String foundInOrderingRule = isDeletedFeatureFoundInOrderingRuleTree(uniqueId.toString(), orderingRule, null, season, context, env);
						if (foundInOrderingRule!=null)
							return foundInOrderingRule;
					} catch (JSONException | GenerationException | ValidationException e) {					
						logger.severe("Error in appearsInOrderingRule:" + e.getMessage());
					}
					
				}
			}
		}
		
		if (env.getBranchId().equals(Constants.MASTER_BRANCH_NAME)) {
			//validate that the deleted feature does not appear in an ordering rule in the branch
			for (Branch branch:season.getBranches().getBranchesList()) {
				Map<String, BaseAirlockItem> branchAirlockItemsDB = Utilities.getAirlockItemsDB(branch.getUniqueId().toString(), context);
				BaseAirlockItem currentFeatureInBranch = branchAirlockItemsDB.get(uniqueId.toString());
				if (currentFeatureInBranch==null) {
					continue; //if the feature is not in the branch - for example if was added to master after its parent was checked out.
				}
				if (currentFeatureInBranch.getBranchStatus().equals(BranchStatus.CHECKED_OUT)) {
					continue; //if the feature is checked out it can be deleted in master since will be recreated as NEW in branch
				}
				BaseAirlockItem branchParentObject = branchAirlockItemsDB.get(currentFeatureInBranch.getParent().toString());
				if ((branchParentObject instanceof FeatureItem || branchParentObject instanceof MutualExclusionGroupItem) && 
						branchParentObject.getOrderingRuleItems()!=null && branchParentObject.getOrderingRuleItems().size()>0) {
					for (int i=0;i<branchParentObject.getOrderingRuleItems().size(); i++) {
						try {
							BaseAirlockItem orderingRule = branchParentObject.getOrderingRuleItems().get(i);
							String foundInOrderingRule = isDeletedFeatureFoundInOrderingRuleTree(uniqueId.toString(), orderingRule, branch, season, context, env);
							if (foundInOrderingRule!=null)
								return foundInOrderingRule;
						} catch (JSONException | GenerationException | ValidationException e) {					
							logger.severe("Error in appearsInOrderingRule in barnch: " + e.getMessage());
						}							
					}
				}
			}
			
			//if the deleted feature is in the master we should check that one of its deleted subItems does not appear in a checked out ordering rule
			//in one of the branches (in master the ordering rule of sub item must be deleted as well since is part of the deleted tree)
			if (featuresItems!=null) {
				for (int i=0;i<featuresItems.size(); i++) {
					String appersInBranchOrderingRule = featuresItems.get(i).appearsInOrderingRule(season, context, env, true);
					if (appersInBranchOrderingRule!=null) {
						return appersInBranchOrderingRule;
					}
				}		
			}
			
		}
				
		return null;
	
	}
	
	//return the ordering rule name in which the feature exists.
	//If does not exists at all - return null.
	//branch is null if in master
	private String isDeletedFeatureFoundInOrderingRuleTree(String deletedFeatureId, BaseAirlockItem orderingRuleTreeRoot, Branch branch, Season season, ServletContext context, Environment env) throws JSONException, GenerationException, ValidationException {
		if (!(orderingRuleTreeRoot instanceof OrderingRuleItem) &&!(orderingRuleTreeRoot instanceof OrderingRuleMutualExclusionGroupItem)) 
		return null;
		
		if (orderingRuleTreeRoot instanceof OrderingRuleItem) {
			if (branch ==null || !orderingRuleTreeRoot.getBranchStatus().equals(BranchStatus.NONE)) { //if in master or in feature that is new/checked_out in branch
				String foundInOrderingRule = ((OrderingRuleItem)(orderingRuleTreeRoot)).containsSubFeaure(uniqueId.toString(), season, context, env);
				if (foundInOrderingRule!=null) {
					if (branch == null) {
						return foundInOrderingRule;
					}
					else {
						return foundInOrderingRule + " in branch " + branch.getName();
					}
				}
			}
		}
		
		if (orderingRuleTreeRoot.getOrderingRuleItems()!=null) {
			for (int i=0; i<orderingRuleTreeRoot.getOrderingRuleItems().size(); i++) {
				String foundInOrderingRule = isDeletedFeatureFoundInOrderingRuleTree(deletedFeatureId, orderingRuleTreeRoot.getOrderingRuleItems().get(i), branch, season, context, env);
				if (foundInOrderingRule!=null)
					return foundInOrderingRule;
			}
		}
		
		return null;
	}
	
	protected void addBranchedToUpdatedBranchesMap(String featureId, Map<String, Stage> updatedBranchesMap, ServletContext context, Environment env) {
		if (env.isInMaster()) {
			//go over all branches and update feature pointing by name
			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			Season season = seasonsDB.get(seasonId.toString());
			for (Branch branch:season.getBranches().getBranchesList()) {
				if (branch.getBranchAirlockItemsBD().containsKey(featureId)) {
					BaseAirlockItem featureInBranch = branch.getBranchAirlockItemsBD().get(featureId);
					if (featureInBranch.getBranchStatus().equals(BranchStatus.CHECKED_OUT)) {
						//if the updated is mtx - write both runtimes else - according to item stage
						Stage itemStage = featureInBranch instanceof DataAirlockItem ? ((DataAirlockItem)featureInBranch).getStage() : Stage.PRODUCTION;
						if (itemStage.equals(Stage.PRODUCTION)) {
							updatedBranchesMap.put(branch.getUniqueId().toString(), itemStage);
						}
						else {							
							if (!updatedBranchesMap.containsKey(branch.getUniqueId().toString())) {
								updatedBranchesMap.put(branch.getUniqueId().toString(), itemStage);								
							}									
						}						
					}
				}
			}
		}		
	}

	public boolean hasNewSubItems() {
		boolean hasNewSubItems = false;
		if (configurationRuleItems!=null) {
			for (int i=0; i<configurationRuleItems.size(); i++) {
				if (configurationRuleItems.get(i).getBranchStatus().equals(BranchStatus.NEW))
					return true;
			}
		}
		
		if (orderingRuleItems!=null) {
			for (int i=0; i<orderingRuleItems.size(); i++) {
				if (orderingRuleItems.get(i).getBranchStatus().equals(BranchStatus.NEW))
					return true;
			}
		}
		
		if (entitlementItems!=null) {
			for (int i=0; i<entitlementItems.size(); i++) {
				if (entitlementItems.get(i).getBranchStatus().equals(BranchStatus.NEW))
					return true;
			}
		}
		
		if (purchaseOptionsItems!=null) {
			for (int i=0; i<purchaseOptionsItems.size(); i++) {
				if (purchaseOptionsItems.get(i).getBranchStatus().equals(BranchStatus.NEW))
					return true;
			}
		}
		
		if (featuresItems!=null) {
			for (int i=0; i<featuresItems.size(); i++) {
				if (featuresItems.get(i).getBranchStatus().equals(BranchStatus.NEW))
					return true;
			}
		}
		return hasNewSubItems;
	}
	
	//The item's root should be the seasons's feature items root 
	public boolean inFeaturesTree(Season season, Map<String, BaseAirlockItem> airlockItemsDB) {
		if (type.equals(Type.ROOT)) {
			if (uniqueId.equals(season.getRoot().getUniqueId())) {
				return true;
			}
			else {
				return false;
			}
		}
		else {
			return airlockItemsDB.get(parent.toString()).inFeaturesTree(season, airlockItemsDB);
		}	
	}
	
	//The item's root should be the seasons's purchases items root 
	public boolean inPurchasesTree(Season season, Map<String, BaseAirlockItem> airlockItemsDB) {
		if (type.equals(Type.ROOT)) {
			if (uniqueId.equals(season.getEntitlementsRoot().getUniqueId())) {
				return true;
			}
			else {
				return false;
			}
		}
		else {
			return airlockItemsDB.get(parent.toString()).inPurchasesTree(season, airlockItemsDB);
		}	
	}
	
	//this function relevant only to PURCHASE_OPTIONS and PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP.
	//else return null
	public EntitlementItem getParentEntitlement (Map<String, BaseAirlockItem> airlockItemsDB) {
		if (!type.equals(Type.PURCHASE_OPTIONS) && !type.equals(Type.PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP)) {
			return null;
		}
		
		//after parent validation - we know that the parent is ENTITLEMENT or PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP
		BaseAirlockItem parentObj = airlockItemsDB.get(parent.toString());

		while (parentObj!=null && parentObj.getType()!=Type.ENTITLEMENT) {
			parentObj = airlockItemsDB.get(parentObj.getParent().toString());
		}

		if (parentObj.getType()==Type.ENTITLEMENT)
			return ((EntitlementItem)parentObj);

		return null; //should not happen since already validated and purchaseOptions is child of entitlement
	}

}
