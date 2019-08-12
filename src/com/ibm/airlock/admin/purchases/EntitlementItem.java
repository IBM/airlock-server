package com.ibm.airlock.admin.purchases;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response.Status;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Strings;
import com.ibm.airlock.Constants.Action;
//import com.ibm.airlock.Constants.Action;
import com.ibm.airlock.Constants.BranchStatus;
import com.ibm.airlock.Constants.OutputJSONMode;
import com.ibm.airlock.Constants.Stage;
import com.ibm.airlock.admin.BaseAirlockItem;
import com.ibm.airlock.admin.Branch;
import com.ibm.airlock.admin.ChangeDetails;
import com.ibm.airlock.admin.DataAirlockItem;
import com.ibm.airlock.admin.FeatureItem;
import com.ibm.airlock.admin.InternalUserGroups;
import com.ibm.airlock.admin.Rule;
import com.ibm.airlock.admin.Season;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.ValidationCache;
import com.ibm.airlock.admin.ValidationResults;
import com.ibm.airlock.admin.authentication.UserInfo;
import com.ibm.airlock.admin.translations.OriginalString;
import com.ibm.airlock.engine.Environment;

public class EntitlementItem extends FeatureItem{
	//The  uniqueIds of the included entitlemens. If not empty this indicates that this is a bundle 
	private LinkedList<String> includedPurchases; //c+u

	public EntitlementItem() {
		type = Type.ENTITLEMENT;
		includedPurchases = new LinkedList<String>();
		purchaseOptionsItems = new LinkedList<BaseAirlockItem>();
		entitlementItems = new LinkedList<BaseAirlockItem>();
	}
	
	public LinkedList<String> getIncludedPurchases() {
		return includedPurchases;
	}

	public void setIncludedPurchases(LinkedList<String> includedPurchases) {
		this.includedPurchases = includedPurchases;
	}

	protected BaseAirlockItem newInstance()
	{
		return new EntitlementItem();
	}

	protected void clone(BaseAirlockItem other)
	{
		super.clone(other);

		EntitlementItem pi = (EntitlementItem) other;
		//copyFields(pi);

		try {
			includedPurchases = (pi.includedPurchases == null) ? null : (LinkedList<String>)pi.includedPurchases.clone();
		}
		catch (Exception e) {}
	}

	protected void shallowClone(BaseAirlockItem other)
	{
		super.shallowClone(other);

		EntitlementItem pi = (EntitlementItem) other;
		//copyFields(pi);
		includedPurchases = pi.includedPurchases;
	}

	/*void copyFields(FeatureItem fi)
	{
		defaultIfAirlockSystemIsDown = fi.defaultIfAirlockSystemIsDown;
		defaultConfiguration = fi.defaultConfiguration;
		displayName = fi.displayName;
	}*/

	//return null if valid, ValidationResults otherwise
	//does not change the feature! 
	//for features cannot call super update since some steps are unique to feature even with general fields
	public ValidationResults doValidateFeatureJSON(JSONObject featureObj, ServletContext context, String seasonId, LinkedList<String> addedSubFeatures, 
			LinkedList<String> missingSubFeatures, InternalUserGroups userGroups, HashMap<UUID, Integer> existingFeaturesInUpdate, 
			String parent, HashMap<String, JSONObject> updatedFeaturesMap, UserInfo userInfo,
			ValidationCache tester,
			Map<String, BaseAirlockItem> airlockItemsDB, Environment env, List<OriginalString> copiedStings) {

		@SuppressWarnings("unchecked")
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);
		Season season = seasonsDB.get(seasonId); //not null - after super validation
		
		if (parent!=null) {
			BaseAirlockItem parentObj = airlockItemsDB.get(parent);
			if (parentObj == null) {
				return new ValidationResults("Parent item " + parent + " not found", Status.BAD_REQUEST);
			}

			if (!parentObj.getType().equals(Type.ROOT) && !parentObj.getType().equals(Type.ENTITLEMENT_MUTUAL_EXCLUSION_GROUP) && !parentObj.getType().equals(Type.ENTITLEMENT)) {
				return new ValidationResults(Strings.illegalEntitlementParent, Status.BAD_REQUEST);					
			}
			
			if (parentObj.getType().equals(Type.ROOT) && season.getRoot().getUniqueId().equals(parentObj.getUniqueId())) {
				return new ValidationResults(Strings.cannotAddEntitlementUnderFeaturesRoot, Status.BAD_REQUEST);					
			}
		}

		ValidationResults res = super.doValidateFeatureJSON(featureObj, context, seasonId, addedSubFeatures, missingSubFeatures, userGroups, existingFeaturesInUpdate, parent, updatedFeaturesMap, userInfo, tester, airlockItemsDB, env, copiedStings);
		if (res !=null)
			return res;
		
		try {			
			Action action = Action.ADD;
			String curUniqueId = null;
			if (featureObj.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && featureObj.get(Constants.JSON_FIELD_UNIQUE_ID)!=null) {
				//if JSON contains uniqueId - update an existing feature otherwise create a new feature
				action = Action.UPDATE;
				curUniqueId = featureObj.getString(Constants.JSON_FIELD_UNIQUE_ID);
			}

			
			//includedPurchases - must exist
			if (!featureObj.containsKey(Constants.JSON_FIELD_INCLUDED_ENTITLEMENTS) || featureObj.get(Constants.JSON_FIELD_INCLUDED_ENTITLEMENTS) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_INCLUDED_ENTITLEMENTS), Status.BAD_REQUEST);					
			}

			//validate that is array
			JSONArray includedPurchasesArr = featureObj.getJSONArray(Constants.JSON_FIELD_INCLUDED_ENTITLEMENTS);
			TreeSet<String> existingIds = new TreeSet<String>();
			for (int i=0; i<includedPurchasesArr.size(); i++) {
				String purchaseId = includedPurchasesArr.getString(i);
				if (existingIds.contains(purchaseId)) {
					return new ValidationResults(String.format(Strings.duplicateIncludedEntitlementId, purchaseId), Status.BAD_REQUEST);
				}
				String err = Utilities.validateLegalUUID(purchaseId);
				if (err!=null) {
					return new ValidationResults(Strings.illegalPurchaseUUID + err, Status.BAD_REQUEST);
				}

				BaseAirlockItem pi = airlockItemsDB.get(purchaseId);
				if (pi == null) {
					return new ValidationResults(String.format(Strings.noPurchaseWithGivenId, purchaseId), Status.BAD_REQUEST);
				}

				if (pi.getType()!=Type.ENTITLEMENT) {
					return new ValidationResults(String.format(Strings.notEntitlementId, purchaseId), Status.BAD_REQUEST);
				}
				existingIds.add(purchaseId);
			}
			
			//validate when moving inAppPurchase from prod to dev that no feature in prod is attached to this purcahse
			if (action == Action.UPDATE) {
				String stageStr = featureObj.getString(Constants.JSON_FEATURE_FIELD_STAGE);
				Stage stageObj = Utilities.strToStage(stageStr); // i know that exists and valid - after super validation
				if (stage.equals(Stage.PRODUCTION) && stageObj.equals(Stage.DEVELOPMENT)) {
					String attachedProdFeatures =Utilities.prodFeaturesAttachedToPurchase(this, season, env.getBranchId(), context);
					if (attachedProdFeatures!=null) {
						return new ValidationResults (attachedProdFeatures, Status.BAD_REQUEST);
					}
				}
				
				//in update we should validate included purchases cyclic
				if (!Utilities.compareIgnoreOrder(includedPurchasesArr, includedPurchases)) {
					for (int i=0; i<includedPurchasesArr.size(); i++) {
						String purchaseId = includedPurchasesArr.getString(i);
						if (uniqueId.toString().equals(purchaseId)) {
							return new ValidationResults(Strings.IncludedEntitlementCannotIncludeIteself, Status.BAD_REQUEST);
						}
						
						BaseAirlockItem ai = airlockItemsDB.get(purchaseId);
						String includedInPurchase = isPurchaseIncludedInPurcahse((EntitlementItem)ai, uniqueId.toString(), airlockItemsDB);
						if (includedInPurchase!=null) {
							return new ValidationResults(String.format(Strings.IncludedEntitlementCyclic, includedInPurchase), Status.BAD_REQUEST);
						}
					}
				}
			}
		}
		catch (JSONException jsne) {
			return new ValidationResults(jsne.getMessage(), Status.BAD_REQUEST);
		}
		catch (ClassCastException cce) {
			return new ValidationResults("Illegal feature JSON: " + cce.getMessage(), Status.BAD_REQUEST);
		}

		return null;	
	}

	//return null if not included and cause cyclic - alse return teh feature name in which it is included
	private String isPurchaseIncludedInPurcahse(EntitlementItem purchaseItem, String purcahseIdToLookFor, Map<String, BaseAirlockItem> airlockItemsDB) {
		if (purchaseItem.getIncludedPurchases()!=null) {
			for (int i=0; i<purchaseItem.getIncludedPurchases().size(); ++i) {
				String purchaseId = purchaseItem.getIncludedPurchases().get(i);
				if (purchaseId.equals(purcahseIdToLookFor)) {
					return purchaseItem.getNameSpaceDotName();
				}
				
				BaseAirlockItem ai = airlockItemsDB.get(purchaseId);
				String res = isPurchaseIncludedInPurcahse((EntitlementItem)ai, purcahseIdToLookFor, airlockItemsDB);
				if (res!=null) {
					return res;
				}
			}
		}
		return null;
	}

	public JSONObject toJson(OutputJSONMode mode, ServletContext context, Environment env, UserInfo userInfo) throws JSONException {
		JSONObject res = super.toJson(mode, context, env, userInfo);
		if (res == null) {
			// this can only happen in runtime_production mode when the feature is in development stage
			return null;
		}

		@SuppressWarnings("unchecked")
		Map<String, BaseAirlockItem> masterAirlockItemsDB = (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);

		if (branchStatus.equals(BranchStatus.CHECKED_OUT) && 
				(mode.equals(OutputJSONMode.RUNTIME_DEVELOPMENT) || mode.equals(OutputJSONMode.RUNTIME_PRODUCTION))) {
			if (stage.equals(Stage.DEVELOPMENT) || (((DataAirlockItem)(masterAirlockItemsDB.get(uniqueId.toString()))).getStage().equals(Stage.PRODUCTION))) {
				res = toDeltaJson(res, context, mode, env);
				return removeOnlyFeatureFields(res);
			}
		}

		if (mode.equals(OutputJSONMode.RUNTIME_DEVELOPMENT) || mode.equals(OutputJSONMode.RUNTIME_PRODUCTION) || mode.equals(OutputJSONMode.DEFAULTS)) {
			//in runtime files change the ids to namspace.name
			JSONArray includedPurchasesName  = getIncludedPurchasesByName(env, mode);
			res.put(Constants.JSON_FIELD_INCLUDED_ENTITLEMENTS, includedPurchasesName);
		}
		else if (mode.equals(OutputJSONMode.DISPLAY)){
			JSONArray includedPurchasesName  = getIncludedPurchasesByName(env, mode);
			res.put(Constants.JSON_FIELD_INCLUDED_ENTITLEMENTS_NAMES, includedPurchasesName);
			res.put(Constants.JSON_FIELD_INCLUDED_ENTITLEMENTS, includedPurchases);
		}
		else {
			res.put(Constants.JSON_FIELD_INCLUDED_ENTITLEMENTS, includedPurchases);
		}
		
		return removeOnlyFeatureFields(res);
	}

	private JSONArray getIncludedPurchasesByName(Environment env, OutputJSONMode mode) {
		JSONArray res = new JSONArray();
		for (String includedPurchaseId:includedPurchases) {
			EntitlementItem ai = (EntitlementItem)env.getAirlockItemsDB().get(includedPurchaseId);
			if (mode.equals(OutputJSONMode.RUNTIME_PRODUCTION) && stage.equals(Stage.PRODUCTION) && ai.getStage().equals(Stage.DEVELOPMENT)) {
				continue; //in runtime production - if the included purchase is in development - don't add it to list
			}
			res.add(ai.getNameSpaceDotName());
		}
		return res;
	}

	public static JSONObject removeOnlyFeatureFields(JSONObject input) {
		input.remove(Constants.JSON_FEATURE_FIELD_FEATURES);//no features array
		input.remove(Constants.JSON_FEATURE_FIELD_ORDERING_RULES); //no ordering rules array
		input.remove(Constants.JSON_FIELD_PREMIUM); //no premium field
		input.remove(Constants.JSON_FIELD_ENTITLEMENT); //no inAppPurchase field
		input.remove(Constants.JSON_FEATURE_FIELD_PREMIUM_RULE); //no premiumRule field
		input.remove(Constants.JSON_FIELD_BRANCH_FEATURES_ITEMS); //no inAppPurchase field
		input.remove(Constants.JSON_FIELD_BRANCH_ORDERING_RULE_ITEMS); //no inAppPurchase field
		return input;
	}
	
	private JSONObject toDeltaJson(JSONObject res, ServletContext context, OutputJSONMode mode, Environment env) throws JSONException {
		@SuppressWarnings("unchecked")
		Map<String, BaseAirlockItem> masterItemsItemsDB = (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);

		EntitlementItem itemInMaster = (EntitlementItem)masterItemsItemsDB.get(uniqueId.toString());

		if (!Utilities.compareListsIgnoreOrder(itemInMaster.getIncludedPurchases(), includedPurchases)) {
			//res.put(Constants.JSON_FIELD_INCLUDED_PURCHASES, includedPurchases);
			JSONArray includedPurchasesName  = getIncludedPurchasesByName(env, mode); //delta is always in runtime so names instead of ids
			res.put(Constants.JSON_FIELD_INCLUDED_ENTITLEMENTS, includedPurchasesName);
		}
		return res;
	}

	public void fromJSON(JSONObject input, Map<String, BaseAirlockItem> airlockItemsDB, UUID parent, Environment env) throws JSONException {
		super.fromJSON(input, airlockItemsDB, parent, env);

		if (input.containsKey(Constants.JSON_FIELD_INCLUDED_ENTITLEMENTS) && input.get(Constants.JSON_FIELD_INCLUDED_ENTITLEMENTS)!=null)
			includedPurchases = Utilities.jsonArrToStringsList(input.getJSONArray(Constants.JSON_FIELD_INCLUDED_ENTITLEMENTS));							
	}

	@Override
	public BaseAirlockItem duplicate(String minVersion, UUID newSeasonId, UUID parentId, Map<String, BaseAirlockItem> airlockItemsDB, 
			HashMap<String, String> oldToDuplicatedFeaturesId, ServletContext context, boolean duplicateSubFeatures, boolean createNewId,
			ValidationCache tester) {

		EntitlementItem res = new EntitlementItem();
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

		res.setAdditionalInfo(additionalInfo == null?null:(JSONObject)additionalInfo.clone());
		res.setConfigurationSchema(configurationSchema == null?null:(JSONObject)configurationSchema.clone());
		res.setDefaultConfiguration(defaultConfiguration);
		res.setDisplayName(displayName);
		res.setCreationDate(creationDate);
		res.setCreator(creator);
		res.setDefaultIfAirlockSystemIsDown(defaultIfAirlockSystemIsDown);
		res.setDescription(description);
		res.setEnabled(enabled);
		res.setNoCachedResults(noCachedResults);
		res.setInternalUserGroups(internalUserGroups == null ? null:internalUserGroups.clone());
		res.setLastModified(lastModified);
		res.setName(name);
		res.setNamespace(namespace);
		res.setOwner(owner);		
		res.setRolloutPercentage(rolloutPercentage);
		res.setBranchStatus(branchStatus);
		res.setBranchFeatureParentName(branchFeatureParentName);
		res.setBranchFeaturesItems(Utilities.cloneStringsList(branchFeaturesItems));
		res.setBranchConfigurationRuleItems(Utilities.cloneStringsList(branchConfigurationRuleItems));
		res.setBranchOrderingRuleItems(Utilities.cloneStringsList(branchOrderingRuleItems));
		res.setBranchPurchaseOptionsItems(Utilities.cloneStringsList(branchPurchaseOptionsItems));
		res.setBranchEntitlementItems(Utilities.cloneStringsList(branchEntitlementItems));

		//in new seasons there are no bitmaps

		res.setRolloutPercentageBitmap(rolloutPercentageBitmap); //if bitmap exists it should remain - even in new seasons.
		res.setRule(rule == null?null:Rule.duplicteForNextSeason(rule)); //
		res.setStage(stage);
		res.setIncludedPurchases((LinkedList<String>)includedPurchases.clone());

		//no inAppPurchaseItems under inAppPurchase


		if (duplicateSubFeatures && entitlementItems!=null) {
			for (int i=0;i<entitlementItems.size(); i++) {
				BaseAirlockItem newAirlockItem = entitlementItems.get(i).duplicate(minVersion, newSeasonId, res.getUniqueId(), airlockItemsDB, oldToDuplicatedFeaturesId, context, duplicateSubFeatures, createNewId, tester);
				res.getEntitlementItems().add(newAirlockItem);					
				if (airlockItemsDB!=null)
					airlockItemsDB.put(newAirlockItem.getUniqueId().toString(), newAirlockItem);						
			}
		}
		
		if (duplicateSubFeatures && purchaseOptionsItems!=null) {
			for (int i=0;i<purchaseOptionsItems.size(); i++) {
				BaseAirlockItem newAirlockItem = purchaseOptionsItems.get(i).duplicate(minVersion, newSeasonId, res.getUniqueId(), airlockItemsDB, oldToDuplicatedFeaturesId, context, duplicateSubFeatures, createNewId, tester);
				res.getPurchaseOptionsItems().add(newAirlockItem);					
				if (airlockItemsDB!=null)
					airlockItemsDB.put(newAirlockItem.getUniqueId().toString(), newAirlockItem);						
			}
		}

		if (configurationRuleItems!=null) {
			for (int i=0;i<configurationRuleItems.size(); i++) {
				BaseAirlockItem newAirlockItem = configurationRuleItems.get(i).duplicate(minVersion, newSeasonId, res.getUniqueId(), airlockItemsDB, oldToDuplicatedFeaturesId, context, duplicateSubFeatures, createNewId, tester);
				res.getConfigurationRuleItems().add(newAirlockItem);					
				if (airlockItemsDB!=null)
					airlockItemsDB.put(newAirlockItem.getUniqueId().toString(), newAirlockItem);						
			}
		}

		if (airlockItemsDB!=null)
			airlockItemsDB.put(res.getUniqueId().toString(), res);	

		return res;		
	}	

	boolean isChanged (JSONObject updatedFeatureData, Map<String, BaseAirlockItem> airlockItemsDB) throws JSONException {
		JSONArray updatedIncludedPurchases = updatedFeatureData.getJSONArray(Constants.JSON_FIELD_INCLUDED_ENTITLEMENTS);
		if (!Utilities.compareIgnoreOrder(updatedIncludedPurchases, includedPurchases)) 
			return true;

		return false;
	}

	public ValidationResults validateProductionDontChanged(JSONObject updatedFeatureData, Map<String, BaseAirlockItem> airlockItemsDB, Branch branch, ServletContext context, boolean considerProdUnderDevAsDev, Environment env) throws JSONException {

		ValidationResults superRes = super.validateProductionDontChanged(updatedFeatureData, airlockItemsDB, branch, context, considerProdUnderDevAsDev, env);

		if (superRes!=null && !superRes.status.equals(Status.OK))
			return superRes;

		//At this stage we can be sure that all mandatory fields exist and legal types exist in the json since validate was previously called.		

		String err = Strings.noPermissionToUpdateEntitlemen;

		boolean isProduction = false;
		if (considerProdUnderDevAsDev) {
			isProduction = (isProductionFeature(this, airlockItemsDB));
		}
		else {
			isProduction = (stage == Stage.PRODUCTION);
		}


		if (isProduction) {
			if (isChanged(updatedFeatureData, airlockItemsDB)) {
				return new ValidationResults(err, Status.UNAUTHORIZED);
			}		
		}

		return superRes;
	}	

	public ValidationResults validateMasterFeatureNotChangedFromBranch(JSONObject updatedFeatureData, Map<String, BaseAirlockItem> airlockItemsDB, Environment env) throws JSONException {

		ValidationResults superRes = super.validateMasterFeatureNotChangedFromBranch(updatedFeatureData, airlockItemsDB, env);
		if (superRes!=null)
			return superRes;

		//can update in master or checked out or new features in branch
		if (env.isInMaster() || branchStatus!=BranchStatus.NONE) 
			return null;

		//At this stage we can be sure that all mandatory fields exist and legal types exist in the json since validate was previously called.
		String err = "You cannot update an item that is not checked out. First check out the item. To update a configuration, check out its parent item.";


		if (isChanged(updatedFeatureData, airlockItemsDB))
			return new ValidationResults(err, Status.BAD_REQUEST);				

		return null;
	}	

	//Return a string with update details.
	//If nothing was changed - return empty string
	//for features cannot call super update since some steps are unique to feature even with general fields
	public List<ChangeDetails> updateAirlockItem(JSONObject updatedAirlockdItemData, Map<String, BaseAirlockItem> airlockItemsDB, BaseAirlockItem root, Environment env, Branch branch,Boolean isProdChange, ServletContext context, Map<String, Stage> updatedBranchesMap) throws JSONException {
		Boolean currentlyInProd = this.getStage().toString().equals("PRODUCTION");
		List<ChangeDetails> dataALItemUpdateDetails = super.updateAirlockItem(updatedAirlockdItemData, airlockItemsDB, root, env, branch,isProdChange, context, updatedBranchesMap);

		boolean wasChanged = ((dataALItemUpdateDetails != null) &&  !dataALItemUpdateDetails.isEmpty());
		List<ChangeDetails> changeDetailsList = dataALItemUpdateDetails;
		StringBuilder updateDetails = new StringBuilder("");

		boolean runtimeFieldChnaged = false;
		//in branch - skip master features (branchStatus = null)
		if (branch==null || !branchStatus.equals(BranchStatus.NONE)) {
			JSONArray updatedIncludedPurchases = updatedAirlockdItemData.getJSONArray(Constants.JSON_FIELD_INCLUDED_ENTITLEMENTS);
			if (!Utilities.compareIgnoreOrder(updatedIncludedPurchases, includedPurchases)) {
				updateDetails.append("'includedEntitlements' changed from: \n");
				if (includedPurchases.size() == 0) { 
					updateDetails.append("[] \n");
				}
				else {
					for (int i=0; i<includedPurchases.size(); i++) {
						updateDetails.append(airlockItemsDB.get(includedPurchases.get(i)).getNameSpaceDotName() + "(" + includedPurchases.get(i) + ")\n");
					}
				}
				updateDetails.append("to: \n");
				if (updatedIncludedPurchases.size() == 0) { 
					updateDetails.append("[] \n");
				}
				else {
					for (int i=0; i<updatedIncludedPurchases.size(); i++) {
						updateDetails.append(airlockItemsDB.get(updatedIncludedPurchases.get(i)).getNameSpaceDotName() + "(" + updatedIncludedPurchases.get(i) + ")\n");
					}
				}
			
				includedPurchases  = Utilities.jsonArrToStringsList(updatedIncludedPurchases);
				wasChanged = true;	
				runtimeFieldChnaged = true;
			}		
		}
		
		if(!updateDetails.toString().isEmpty()){
			Boolean isProductionChange = updatedAirlockdItemData.getString(Constants.JSON_FEATURE_FIELD_STAGE).equals("PRODUCTION") || currentlyInProd;
			changeDetailsList.add(new ChangeDetails(updateDetails.toString(),this,isProductionChange));
		}

		if (wasChanged) {
			lastModified = new Date();
		}

		//in master if one of the runtime field was changed we should go over all the branches and if the item is checked out - update runtime since the delta was changed
		if (branch == null && runtimeFieldChnaged) {
			addBranchedToUpdatedBranchesMap(uniqueId.toString(), updatedBranchesMap, context, env);
		}
		return changeDetailsList;

	}

	//updatedPurchaseOptionId - the id of a purcahse options that its storeProductId list becomes empty during update
	public boolean hasPurchaseOptionsWithProductId(UUID deletedPurchaseOptionId, UUID updatedPurchaseOptionId, Stage featureStage) {
		return doHasPurchaseOptionsWithProductId(purchaseOptionsItems, deletedPurchaseOptionId, updatedPurchaseOptionId, featureStage);
	}

	//updatedPurchaseOptionId - the id of a purcahse options that its storeProductId list becomes empty during update
	private boolean doHasPurchaseOptionsWithProductId(LinkedList<BaseAirlockItem> purchaseOptionsItems, UUID deletedPurchaseOptionId, UUID updatedPurchaseOptionId, Stage featureStage) {
		if (purchaseOptionsItems!=null) {
			for (BaseAirlockItem bi : purchaseOptionsItems) {
				if (deletedPurchaseOptionId!=null && bi.getUniqueId().equals(deletedPurchaseOptionId)) {
					continue;
				}
				if (bi.getType().equals(Type.PURCHASE_OPTIONS)) {
					if (updatedPurchaseOptionId == null || !updatedPurchaseOptionId.equals(bi.getUniqueId())) { //if equal the update  - means that its storeIds list is now empty
						if (featureStage == null || featureStage.equals(Stage.DEVELOPMENT) || ((PurchaseOptionsItem)bi).getStage().equals(Stage.PRODUCTION)) { //if feature is production but purchaseOptions is development - don't count it a store ids
							if (((PurchaseOptionsItem)bi).getStoreIdsList()!=null && !((PurchaseOptionsItem)bi).getStoreIdsList().isEmpty()) {
								return true;
							}
						}
					}
				}
				if (bi.getType().equals(Type.PURCHASE_OPTIONS) || bi.getType().equals(Type.PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP)) {
					boolean res = doHasPurchaseOptionsWithProductId(bi.getPurchaseOptionsItems(), deletedPurchaseOptionId, updatedPurchaseOptionId, featureStage);
					if (res)
						return res;
				}
			}
		}
		return false;
	}
}
