package com.ibm.airlock.admin;

import java.util.*;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response.Status;

import com.ibm.airlock.admin.translations.OriginalString;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Constants.Action;
import com.ibm.airlock.Constants.BranchStatus;
import com.ibm.airlock.Constants.OutputJSONMode;
import com.ibm.airlock.Constants.Stage;
import com.ibm.airlock.admin.authentication.UserInfo;
import com.ibm.airlock.engine.Environment;

public abstract class BaseMutualExclusionGroupItem extends BaseAirlockItem {
	protected Integer maxFeaturesOn = 1; //relevant only to mutual_exclusion_group.

	protected void clone(BaseAirlockItem other)
	{
		super.clone(other);
		maxFeaturesOn = ((BaseMutualExclusionGroupItem)other).maxFeaturesOn;
	}
	protected void shallowClone(BaseAirlockItem other)
	{
		super.shallowClone(other);
		maxFeaturesOn = ((BaseMutualExclusionGroupItem)other).maxFeaturesOn;
	}
	public Integer getMaxFeaturesOn() {
		return maxFeaturesOn;
	}
	
	public void setMaxFeaturesOn(Integer maxFeaturesOn) {
		this.maxFeaturesOn = maxFeaturesOn;
	}
		
	public void fromJSON(JSONObject input, Map<String, BaseAirlockItem> airlockItemsDB, UUID parent, Environment env) throws JSONException {
		super.fromJSON(input, airlockItemsDB, parent, env);
		
		if (input.containsKey(Constants.JSON_FEATURE_FIELD_MAX_FEATURES_ON) && input.get(Constants.JSON_FEATURE_FIELD_MAX_FEATURES_ON)!=null) {
			maxFeaturesOn = input.getInt(Constants.JSON_FEATURE_FIELD_MAX_FEATURES_ON);
		}
	}

	private JSONObject toDeltaJson(JSONObject res, ServletContext context, OutputJSONMode mode) throws JSONException {
		@SuppressWarnings("unchecked")
		Map<String, BaseAirlockItem> airlockItemsDB = (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);

		BaseMutualExclusionGroupItem itemInMaster = (BaseMutualExclusionGroupItem)airlockItemsDB.get(uniqueId.toString());
		
		
		if (itemInMaster.getMaxFeaturesOn()!=maxFeaturesOn) {
			res.put(Constants.JSON_FEATURE_FIELD_MAX_FEATURES_ON, maxFeaturesOn);
		}
		
		return res;
	}
	
	public JSONObject toJson(OutputJSONMode mode, ServletContext context, Environment env, UserInfo userInfo) throws JSONException {
		JSONObject res = super.toJson(mode, context, env, userInfo);
		if (res == null) {
			// this can only happen in runtime_production mode when the MUTUAL_EXCLUSION_GROUP has no sub item in production
			return null;
		}
		/*
		if (branchStatus.equals(BranchStatus.CHECKED_OUT) && 
				(mode.equals(OutputJSONMode.RUNTIME_DEVELOPMENT) || mode.equals(OutputJSONMode.RUNTIME_PRODUCTION))) {
			res = toDeltaJson(res, context, mode);
			return res;
		}*/
		
		res.put(Constants.JSON_FEATURE_FIELD_MAX_FEATURES_ON, maxFeaturesOn);
		return res;
	}
	
	//Return a string with update details.
	//If nothing was changed - return empty string 
	public List<ChangeDetails> updateAirlockItem(JSONObject updatedAirlockdItemData, Map<String, BaseAirlockItem> airlockItemsDB, BaseAirlockItem root,Environment env, Branch branch,Boolean isProdChange, ServletContext context, Map<String, Stage> updatedBranchesMap) throws JSONException {
		List<ChangeDetails> baseALItemUpdateDetails = super.updateAirlockItem(updatedAirlockdItemData, airlockItemsDB, root, env, branch,isProdChange, context, updatedBranchesMap);
		List<ChangeDetails> changeDetailsList = baseALItemUpdateDetails;
		StringBuilder updateDetails = new StringBuilder("");
		boolean wasChanged = ((baseALItemUpdateDetails != null) &&  !baseALItemUpdateDetails.isEmpty());

		//in branch - skip master features (branchStatus = null)
		if (branch==null || !branchStatus.equals(BranchStatus.NONE)) {
			//optional field
			if (updatedAirlockdItemData.containsKey(Constants.JSON_FEATURE_FIELD_MAX_FEATURES_ON) &&  updatedAirlockdItemData.get(Constants.JSON_FEATURE_FIELD_MAX_FEATURES_ON)!=null) {
				int updatedMaxFeaturesOn = updatedAirlockdItemData.getInt(Constants.JSON_FEATURE_FIELD_MAX_FEATURES_ON);
				if (maxFeaturesOn  != updatedMaxFeaturesOn) {
					updateDetails.append("'maxFeaturesOn' changed from " + maxFeaturesOn + " to " + updatedMaxFeaturesOn + "\n");
					maxFeaturesOn = updatedMaxFeaturesOn;		
					wasChanged = true;
				}
			}
		}
		
		if(!updateDetails.toString().isEmpty()){
			changeDetailsList.add(new ChangeDetails(updateDetails.toString(),this));
		}
		if (wasChanged) {
			//in master if one of the runtime field was changed we should go over all the branches and if the item is checked out - update runtime since the delta was changed
			if (branch == null) {
				addBranchedToUpdatedBranchesMap(uniqueId.toString(), updatedBranchesMap, context, env);
			}
			lastModified = new Date();
		}

		return changeDetailsList;
	}

	public ValidationResults doValidateFeatureJSON(JSONObject featureObj, ServletContext context, String seasonId, LinkedList<String> addedSubFeatures, LinkedList<String> missingSubFeatures, InternalUserGroups userGroups, HashMap<UUID, Integer> existingFeaturesInUpdate, String parent, HashMap<String, JSONObject> updatedFeaturesMap, UserInfo userInfo,
			ValidationCache tester,
			Map<String, BaseAirlockItem> airlockItemsDB, Environment env,List<OriginalString> copiedStings) {
		ValidationResults superRes = super.doValidateFeatureJSON(featureObj, context, seasonId, addedSubFeatures, missingSubFeatures, userGroups, existingFeaturesInUpdate, parent, updatedFeaturesMap, userInfo, tester, airlockItemsDB, env,copiedStings);
		
		if (superRes!=null)
			return superRes;
		try {
			//maxFeaturesOn
			if (featureObj.containsKey(Constants.JSON_FEATURE_FIELD_MAX_FEATURES_ON) && featureObj.get(Constants.JSON_FEATURE_FIELD_MAX_FEATURES_ON) != null) {
				int mfo = featureObj.getInt(Constants.JSON_FEATURE_FIELD_MAX_FEATURES_ON); //validate that is Integer value
				if (mfo<=0) {
					return new ValidationResults("The Maximal Number of Features On should be an integer greater than 1.", Status.BAD_REQUEST);
				}
			}
			
			//in update - validate that master feature (NONE status) wasnt changed
			Action action = Action.ADD;
			if (featureObj.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && featureObj.get(Constants.JSON_FIELD_UNIQUE_ID)!=null) {
				//if JSON contains uniqueId - update an existing feature otherwise create a new feature
				action = Action.UPDATE;			
			}
			
			if (action.equals(Action.UPDATE)) {
				ValidationResults res = validateMasterFeatureNotChangedFromBranch(featureObj, airlockItemsDB, env);
				if (res!=null)
					return res;
			}
		} catch (JSONException jsne) {
			return new ValidationResults(jsne.getMessage(), Status.BAD_REQUEST);
		}
		catch (ClassCastException cce) {
			return new ValidationResults("Illegal feature JSON: " + cce.getMessage(), Status.BAD_REQUEST);
		}
		return null;
	}
		
	
	//considerProdUnderDev: for prod runtime file - we should consider prod under dev as dev
	//                      for user permissions - we should consider prod under dev as prod 

	public ValidationResults validateProductionDontChanged(JSONObject updatedFeatureData, Map<String, BaseAirlockItem> airlockItemsDB, Branch branch, ServletContext context, boolean considerProdUnderDevAsDev, Environment env) throws JSONException {
		//At this stage we can be sure that all mandatory fields exist and legal types exist in the json since validate was previously called.		
		String err = "Unable to update the MUTUAL_EXCLUSION_GROUP. Only a user with the Administrator or Product Lead role can change a subitem that is in the production stage.";			
		
		if (isProductionFeature(this, airlockItemsDB) &&isChanged(updatedFeatureData, airlockItemsDB))
			return new ValidationResults(err, Status.BAD_REQUEST);
	
		ValidationResults superRes = super.validateProductionDontChanged(updatedFeatureData, airlockItemsDB, branch, context, considerProdUnderDevAsDev, env);
		if (superRes!=null)
			return superRes;
		
		return null;
	}
	
	public boolean isAnalyticsChanged(JSONObject updatedFeatureData, Season season, ServletContext context, Environment env) throws JSONException {		
		boolean  analyticsChanged = super.isAnalyticsChanged(updatedFeatureData, season, context, env);
		if (analyticsChanged) 
			return true;
		
		return false;
	}
	public ValidationResults validateMasterFeatureNotChangedFromBranch(JSONObject updatedFeatureData, Map<String, BaseAirlockItem> airlockItemsDB, Environment env) throws JSONException {
		
		/*ValidationResults superRes = super.validateMasterFeatureNotChangedFromBranch(updatedFeatureData, airlockItemsDB);
		if (superRes!=null)
			return superRes;
			*/	
		
		//can update in master or checked out or new features in branch
		if (env.isInMaster() || branchStatus!=BranchStatus.NONE) 
			return null;

		//Creator, creation date, seasonId and type should not be updated

		//At this stage we can be sure that all mandatory fields exist and legal types exist in the json since validate was previously called.
		
		String err = "You cannot update an item that is not checked out. First check out the item. To update a configuration, check out its parent feature.";

		if (isChanged(updatedFeatureData, airlockItemsDB))
			return new ValidationResults(err, Status.BAD_REQUEST);				
		
		return null;
	}	
	
	private boolean isChanged (JSONObject updatedFeatureData, Map<String, BaseAirlockItem> airlockItemsDB) throws JSONException {
		
		int updatedMaxFeaturesOn = updatedFeatureData.getInt(Constants.JSON_FEATURE_FIELD_MAX_FEATURES_ON);
		if (maxFeaturesOn!=updatedMaxFeaturesOn)
				return true;		
						
		return false;		
	}
}
