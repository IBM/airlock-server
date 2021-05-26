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

public class PurchaseOptionsItem extends FeatureItem {
	public class StoreProductId {
		public static final String GOOGLE_PLAY_STORE = "Google Play Store";
		public static final String APPLE_APP_STORE = "Apple App Store";

		String storeType; 
		String productId;

		public StoreProductId(String storeType, String productId) {
			this.storeType = storeType;
			this.productId = productId;
		}
		public StoreProductId() {
			// TODO Auto-generated constructor stub
		}
		public String getStoreType() {
			return storeType;
		}
		public void setStoreType(String storeType) {
			this.storeType = storeType;
		}
		public String getProductId() {
			return productId;
		}
		public void setProductId(String productId) {
			this.productId = productId;
		}

		public JSONObject toJson() throws JSONException {
			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FIELD_STORE_TYPE, storeType);
			res.put(Constants.JSON_FIELD_PRODUCT_ID, productId);
			return res;
		}

		public void fromJson(JSONObject input) throws JSONException {
			storeType = input.getString(Constants.JSON_FIELD_STORE_TYPE);
			productId = input.getString(Constants.JSON_FIELD_PRODUCT_ID);
		}

		public ValidationResults validateStoreProductIdJson(JSONObject input) throws JSONException {
			//storeType - must exist
			if (!input.containsKey(Constants.JSON_FIELD_STORE_TYPE) || input.get(Constants.JSON_FIELD_STORE_TYPE) == null || input.getString(Constants.JSON_FIELD_STORE_TYPE).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_STORE_TYPE), Status.BAD_REQUEST);					
			}
			
			String storeType = input.getString(Constants.JSON_FIELD_STORE_TYPE);
			if (!storeType.equals(APPLE_APP_STORE) && !storeType.equals(GOOGLE_PLAY_STORE)) {
				return new ValidationResults(String.format(Strings.IllegalStoreType, GOOGLE_PLAY_STORE, APPLE_APP_STORE), Status.BAD_REQUEST);
			}

			//productId - must exist and not empty
			if (!input.containsKey(Constants.JSON_FIELD_PRODUCT_ID) || input.get(Constants.JSON_FIELD_PRODUCT_ID) == null || input.getString(Constants.JSON_FIELD_PRODUCT_ID).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_PRODUCT_ID), Status.BAD_REQUEST);					
			}

			return null;
		}

		//Return a string with update details.
		//If nothing was changed - return empty string
		public String updateStoreProductId(JSONObject input) throws JSONException {

			StringBuilder updateDetails = new StringBuilder("");

			//storeType
			String updatedStoreType = input.getString(Constants.JSON_FIELD_STORE_TYPE);
			if (updatedStoreType.equals(storeType)) {
				updateDetails.append("'storeType' changed from " + storeType + " to " + updatedStoreType + "\n");
				storeType = updatedStoreType;					
			}

			//productId
			String updatedProductId = input.getString(Constants.JSON_FIELD_STORE_TYPE);
			if (updatedProductId.equals(productId)) {
				updateDetails.append("'productId' changed from " + productId + " to " + updatedProductId + "\n");
				productId = updatedProductId;					
			}

			return updateDetails.toString();
		}
	}

	private LinkedList<StoreProductId> storeIdsList = null;

	public PurchaseOptionsItem() {
		type = Type.PURCHASE_OPTIONS;
		storeIdsList = new LinkedList<StoreProductId>(); 
	}

	protected BaseAirlockItem newInstance()
	{
		return new PurchaseOptionsItem();
	}

	public LinkedList<StoreProductId> getStoreIdsList() {
		return storeIdsList;
	}

	public void setStoreIdsList(LinkedList<StoreProductId> storeIdsList) {
		this.storeIdsList = storeIdsList;
	}

	protected void clone(BaseAirlockItem other)
	{
		super.clone(other);

		PurchaseOptionsItem po = (PurchaseOptionsItem) other;
		//copyFields(pi);

		try {
			storeIdsList = (po.storeIdsList == null) ? null : (LinkedList<StoreProductId>)po.storeIdsList.clone();
		}
		catch (Exception e) {}
	}

	protected void shallowClone(BaseAirlockItem other)
	{
		super.shallowClone(other);

		PurchaseOptionsItem po = (PurchaseOptionsItem) other;
		//copyFields(pi);
		storeIdsList = po.storeIdsList;
	}

	//return null if valid, ValidationResults otherwise
	//does not change the feature! 
	//for features cannot call super update since some steps are unique to feature even with general fields
	public ValidationResults doValidateFeatureJSON(JSONObject featureObj, ServletContext context, String seasonId, LinkedList<String> addedSubFeatures, 
			LinkedList<String> missingSubFeatures, InternalUserGroups userGroups, HashMap<UUID, Integer> existingFeaturesInUpdate, 
			String parent, HashMap<String, JSONObject> updatedFeaturesMap, UserInfo userInfo,
			ValidationCache tester,
			Map<String, BaseAirlockItem> airlockItemsDB, Environment env,List<OriginalString> copiedStings) {

		if (parent!=null) {
			BaseAirlockItem parentObj = airlockItemsDB.get(parent);
			if (parentObj == null) {
				return new ValidationResults("Parent item " + parent + " not found", Status.BAD_REQUEST);
			}

			if (!parentObj.getType().equals(Type.ENTITLEMENT) && !parentObj.getType().equals(Type.PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP)) {
				return new ValidationResults(Strings.illagePurchaseOptionsParent, Status.BAD_REQUEST);					
			}
		}
		
		ValidationResults res = super.doValidateFeatureJSON(featureObj, context, seasonId, addedSubFeatures, missingSubFeatures, userGroups, existingFeaturesInUpdate, parent, updatedFeaturesMap, userInfo, tester, airlockItemsDB, env, copiedStings);
		if (res !=null)
			return res;

		try {
			if (featureObj.containsKey(Constants.JSON_FEATURE_FIELD_PURCHASE_OPTIONS) && featureObj.get(Constants.JSON_FEATURE_FIELD_PURCHASE_OPTIONS)!=null && !featureObj.getJSONArray(Constants.JSON_FEATURE_FIELD_PURCHASE_OPTIONS).isEmpty()) {
				return new ValidationResults("Illegal parent. A purchase options can only be added under an entitlement or purchase options mutual exclusion group.", Status.BAD_REQUEST);
			}
			
			//storeIdsList - must exist
			if (!featureObj.containsKey(Constants.JSON_FIELD_STORE_PRODUCT_IDS) || featureObj.get(Constants.JSON_FIELD_STORE_PRODUCT_IDS) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_STORE_PRODUCT_IDS), Status.BAD_REQUEST);					
			}

			//validate that is array  - and that no store types duplications 
			JSONArray storeIdsListArr = featureObj.getJSONArray(Constants.JSON_FIELD_STORE_PRODUCT_IDS);
			TreeSet<String> existingStoreTypes = new TreeSet<String>();
			for (int i=0; i<storeIdsListArr.size(); i++) {
				JSONObject storeIdObj = storeIdsListArr.getJSONObject(i);
				StoreProductId tmp = new StoreProductId();
				ValidationResults spiRes = tmp.validateStoreProductIdJson(storeIdObj);
				if (spiRes!=null)
					return spiRes;
				tmp.fromJson(storeIdObj);
				
				if (existingStoreTypes.contains(tmp.getStoreType())) {
					return new ValidationResults(String.format(Strings.duplicateStoreType, tmp.getStoreType()), Status.BAD_REQUEST);
				}
				
				existingStoreTypes.add(tmp.getStoreType());
			}
			
			Action action = Action.ADD;
			if (featureObj.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && featureObj.get(Constants.JSON_FIELD_UNIQUE_ID)!=null) {
				//if JSON contains uniqueId - update an existing feature otherwise create a new feature
				action = Action.UPDATE;
			}
			
			if (action.equals(Action.UPDATE)) {
				EntitlementItem entitlement = getParentEntitlement(airlockItemsDB);
				
				@SuppressWarnings("unchecked")
				Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);
				Season season = seasonsDB.get(seasonId); //not null - after super validation
					
				//validate that emptying the store id list wont leave the parent entitlment without any store product id if it is attached to premium feature
				//LinkedList<StoreProductId> tmpStoreIdsList = JsonArray2StoreIdsList(storeIdsListArr);
				if (storeIdsListArr.isEmpty() && !storeIdsList.isEmpty()) {
					String attachedFeatures = Utilities.featuresAttachedToEntitlement(entitlement, season, env.getBranchId(), context);
					if (attachedFeatures!=null) {
						//the parent entitlement is attached to premium feature - validate that the purchaseOptions updated
						//doesn't leave the entitlement without any storeProductId
						if (!entitlement.hasPurchaseOptionsWithProductId(null, uniqueId, null)) {
							return new ValidationResults(Strings.cannotUpdatePurchaseOptionsLeavesEntitlementWithoutStoreProdId, Status.BAD_REQUEST);
						}
					}			
				}
				
				String stageStr = featureObj.getString(Constants.JSON_FEATURE_FIELD_STAGE);
				Stage stageObj = Utilities.strToStage(stageStr); // i know that exists and valid - after super validation
				
				//if updating from prod to dev and it parent is an entitlement in prod that is attached to feature in prod and
				//moving the purchaseOptions to dev will cause the prod entitlment to lake storeProductId - prevent
				if (stage.equals(Stage.PRODUCTION) && stageObj.equals(Stage.DEVELOPMENT) && entitlement.getStage().equals(Stage.PRODUCTION)) {
					String attachedProductionFeatures = Utilities.productionFeaturesAttachedToEntitlement(entitlement, season, env.getBranchId(), context);
					if (attachedProductionFeatures !=null && !entitlement.hasPurchaseOptionsWithProductId(null, uniqueId, Stage.PRODUCTION)) {
						return new ValidationResults(Strings.cannotUpdatePurchaseOptionsToDevelopment, Status.BAD_REQUEST);
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
				return EntitlementItem.removeOnlyFeatureFields(res);
			}
		}

		res.put(Constants.JSON_FIELD_STORE_PRODUCT_IDS, StoreIdsList2JsonArray());
		
		return EntitlementItem.removeOnlyFeatureFields(res);
	}
	
	private JSONArray StoreIdsList2JsonArray() throws JSONException {
		JSONArray res = new JSONArray();
		for (StoreProductId spi : storeIdsList) {
			res.add(spi.toJson());
		}
		return res;
	}
	
	private String StoreIdsList2String(LinkedList<StoreProductId> storeIdsList) throws JSONException {
		StringBuilder sb = new StringBuilder();
		for (StoreProductId spi : storeIdsList) {
			sb.append(spi.getStoreType() + ", " + spi.getProductId() + "; ");
		}
		return sb.toString();
	}
	
	private LinkedList<StoreProductId> JsonArray2StoreIdsList(JSONArray storeIdsArr) throws JSONException {
		LinkedList<StoreProductId> tmpStoreIdsList = new LinkedList<StoreProductId>();
		for (int i=0; i<storeIdsArr.size(); i++) {
			JSONObject obj = storeIdsArr.getJSONObject(i);
			StoreProductId spi = new StoreProductId();
			spi.fromJson(obj);
			tmpStoreIdsList.add(spi);
		}	
		return tmpStoreIdsList;
	}

	private JSONObject toDeltaJson(JSONObject res, ServletContext context, OutputJSONMode mode, Environment env) throws JSONException {
		@SuppressWarnings("unchecked")
		Map<String, BaseAirlockItem> masterAirlockItemsDB = (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);

		PurchaseOptionsItem itemInMaster = (PurchaseOptionsItem)masterAirlockItemsDB.get(uniqueId.toString());

		if (!storeIdsListAreEqual(itemInMaster.getStoreIdsList(), storeIdsList)) {
			res.put(Constants.JSON_FIELD_STORE_PRODUCT_IDS, StoreIdsList2JsonArray());
		}
		return res;
	}

	private boolean storeIdsListAreEqual(LinkedList<StoreProductId> storeIdsList1, LinkedList<StoreProductId> storeIdsList2) {
		if (storeIdsList1 == null &&  storeIdsList2==null)
			return true;
		
		if (storeIdsList1 == null || storeIdsList2==null)
			return false;
		
		if (storeIdsList1.size() !=  storeIdsList2.size())
			return false;
		
		for (int i=0; i<storeIdsList1.size(); i++) {
			if (!storeIdsList1.get(i).storeType.equals(storeIdsList2.get(i).storeType) || 
				!storeIdsList1.get(i).productId.equals(storeIdsList2.get(i).productId) ) {
				return false;
			}
		}
		
		return true;
	}

	public void fromJSON(JSONObject input, Map<String, BaseAirlockItem> airlockItemsDB, UUID parent, Environment env) throws JSONException {
		super.fromJSON(input, airlockItemsDB, parent, env);

		if (input.containsKey(Constants.JSON_FIELD_STORE_PRODUCT_IDS) && input.get(Constants.JSON_FIELD_STORE_PRODUCT_IDS)!=null)
			storeIdsList = JsonArray2StoreIdsList(input.getJSONArray(Constants.JSON_FIELD_STORE_PRODUCT_IDS));							
	}

	@Override
	public BaseAirlockItem duplicate(String minVersion, UUID newSeasonId, UUID parentId, Map<String, BaseAirlockItem> airlockItemsDB, 
			HashMap<String, String> oldToDuplicatedFeaturesId, ServletContext context, boolean duplicateSubFeatures, boolean createNewId,
			ValidationCache tester) {

		PurchaseOptionsItem res = new PurchaseOptionsItem();
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
		res.setStoreIdsList((LinkedList<StoreProductId>)storeIdsList.clone());

		//no inAppPurchaseItems and purchaseOptionsItems under inAppPurchase
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
		JSONArray updatedStoreIdsList = updatedFeatureData.getJSONArray(Constants.JSON_FIELD_STORE_PRODUCT_IDS);
		
		LinkedList<StoreProductId> tmpStoreIdsList = JsonArray2StoreIdsList(updatedStoreIdsList);
		if (!storeIdsListAreEqual(tmpStoreIdsList, storeIdsList)) 
			return true;

		return false;
	}

	public ValidationResults validateProductionDontChanged(JSONObject updatedFeatureData, Map<String, BaseAirlockItem> airlockItemsDB, Branch branch, ServletContext context, boolean considerProdUnderDevAsDev, Environment env, boolean ignoreUserGroups) throws JSONException {

		ValidationResults superRes = super.validateProductionDontChanged(updatedFeatureData, airlockItemsDB, branch, context, considerProdUnderDevAsDev, env, ignoreUserGroups);

		if (superRes!=null && !superRes.status.equals(Status.OK))
			return superRes;

		//At this stage we can be sure that all mandatory fields exist and legal types exist in the json since validate was previously called.		

		String err = Strings.noPermissionToUpdatePurchaseOptions;

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
			JSONArray updatedStoreIdsList = updatedAirlockdItemData.getJSONArray(Constants.JSON_FIELD_STORE_PRODUCT_IDS);
			LinkedList<StoreProductId> tmpStoreIdsList = JsonArray2StoreIdsList(updatedStoreIdsList);
			if (!storeIdsListAreEqual(tmpStoreIdsList, storeIdsList)) {
				updateDetails.append("'storeProductIds' changed from " + StoreIdsList2String(storeIdsList) + " to " + StoreIdsList2String(tmpStoreIdsList) + "\n");
				storeIdsList  = tmpStoreIdsList;
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

}
