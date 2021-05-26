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
import com.ibm.airlock.engine.VerifyRule;

public class ConfigurationRuleItem extends DataAirlockItem {

	protected String configuration = null; //c+u

	protected BaseAirlockItem newInstance()
	{
		return new ConfigurationRuleItem();
	}
	protected void clone(BaseAirlockItem other)
	{
		super.clone(other);
		configuration = ((ConfigurationRuleItem) other).configuration;
	}
	protected void shallowClone(BaseAirlockItem other)
	{
		super.shallowClone(other);
		configuration = ((ConfigurationRuleItem) other).configuration;
	}
	public ConfigurationRuleItem() {
		type = Type.CONFIGURATION_RULE;
		configurationRuleItems = new LinkedList<BaseAirlockItem>();
	}	

	public String getConfiguration() {
		return configuration;
	}

	public void setConfiguration(String configuration) {
		this.configuration = configuration;
	}

	public JSONObject toJson(OutputJSONMode mode, ServletContext context, Environment env, UserInfo userInfo) throws JSONException {
		if (mode == OutputJSONMode.DEFAULTS) //dont add configuration rule to defaults file
			return null;

		JSONObject res = super.toJson(mode, context, env, userInfo);
		if (res == null) {
			// this can only happen in runtime_production mode when the cnfigRule is in development stage
			return null;
		}

		@SuppressWarnings("unchecked")
		Map<String, BaseAirlockItem> masterAirlockItemsDB = (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);

		if (branchStatus.equals(BranchStatus.CHECKED_OUT) && 
				(mode.equals(OutputJSONMode.RUNTIME_DEVELOPMENT) || mode.equals(OutputJSONMode.RUNTIME_PRODUCTION))) {
			if (stage.equals(Stage.DEVELOPMENT) || (((DataAirlockItem)(masterAirlockItemsDB.get(uniqueId.toString()))).getStage().equals(Stage.PRODUCTION))) {
				res = toDeltaJson(res, context, mode);
				return res;
			}
			else {
				//in case the item is checked out. Dev in master and prod in branch. Write the whole item (not only delta) and set its 
				//branchStatus to NEW in runtime files
				res.put("branchStatus", "NEW");
			}
		}

		res.put(Constants.JSON_FEATURE_FIELD_CONFIGURATION, configuration);				

		return res;
	}

	private JSONObject toDeltaJson(JSONObject res, ServletContext context, OutputJSONMode mode) throws JSONException {
		@SuppressWarnings("unchecked")
		Map<String, BaseAirlockItem> airlockItemsDB = (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);

		ConfigurationRuleItem itemInMaster = (ConfigurationRuleItem)airlockItemsDB.get(uniqueId.toString());

		if (!itemInMaster.getConfiguration().equals(configuration)) {
			res.put(Constants.JSON_FEATURE_FIELD_CONFIGURATION, configuration);
		}		

		return res;		
	}

	public void fromJSON(JSONObject input, Map<String, BaseAirlockItem> airlockItemsDB, UUID parent, Environment env) throws JSONException {
		super.fromJSON(input, airlockItemsDB, parent, env);

		if (input.containsKey(Constants.JSON_FEATURE_FIELD_CONFIGURATION) && input.get(Constants.JSON_FEATURE_FIELD_CONFIGURATION)!=null)
			configuration = input.getString(Constants.JSON_FEATURE_FIELD_CONFIGURATION);
	}

	@Override
	public BaseAirlockItem duplicate(String minVersion, UUID newSeasonId, UUID parentId, Map<String, BaseAirlockItem> airlockItemsDB, 
			HashMap<String, String> oldToDuplicatedFeaturesId, ServletContext context, boolean duplicateSubFeatures, boolean createNewId,
			ValidationCache tester) {
		ConfigurationRuleItem res = new ConfigurationRuleItem();

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

		//in new seasons there are no bitmaps

		res.setRule(rule == null?null:Rule.duplicteForNextSeason(rule)); 
		res.setStage(stage);

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

	//Return a string with update details.
	//If nothing was changed - return empty string
	//for features cannot call super update since some steps are unique to feature even with general fields
	public List<ChangeDetails> updateAirlockItem(JSONObject updatedAirlockdItemData, Map<String, BaseAirlockItem> airlockItemsDB, BaseAirlockItem root, Environment env, Branch branch,Boolean isProdChange, ServletContext context, Map<String, Stage> updatedBranchesMap) throws JSONException {

		Boolean currentlyInProd = this.getStage().toString().equals("PRODUCTION");
		List<ChangeDetails> dataALItemUpdateDetails = super.updateAirlockItem(updatedAirlockdItemData, airlockItemsDB, root, env, branch,isProdChange, context, updatedBranchesMap);

		boolean wasChanged = ((dataALItemUpdateDetails != null) &&  !dataALItemUpdateDetails.isEmpty());
		List<ChangeDetails> changeDetailsList = dataALItemUpdateDetails;
		StringBuilder updateDetails = new StringBuilder("");

		//in branch - skip master features (branchStatus = null)
		if (branch==null || !branchStatus.equals(BranchStatus.NONE)) {
			if (updatedAirlockdItemData.containsKey(Constants.JSON_FEATURE_FIELD_CONFIGURATION) && updatedAirlockdItemData.get(Constants.JSON_FEATURE_FIELD_CONFIGURATION)!=null) {
				String updatedConfiguration = updatedAirlockdItemData.getString(Constants.JSON_FEATURE_FIELD_CONFIGURATION);
				if (updatedConfiguration!=null && !updatedConfiguration.equals(configuration)) {
					updateDetails.append("'configuration' changed from " + configuration + " to " + updatedConfiguration + "\n");
					configuration  = updatedConfiguration;
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
	//Return a string with update details.
	//If nothing was changed - return empty string
	//for features cannot call super update since some steps are unique to feature even with general fields
	public List<ChangeDetails> updateAirlockItemIgnoreConfiguration(JSONObject updatedAirlockdItemData, Map<String, BaseAirlockItem> airlockItemsDB, BaseAirlockItem root, Environment env, Branch branch,Boolean isProdChange, ServletContext context, Map<String, Stage> updatedBranchesMap) throws JSONException {

		List<ChangeDetails> dataALItemUpdateDetails = super.updateAirlockItem(updatedAirlockdItemData, airlockItemsDB, root, env, branch,isProdChange, context, updatedBranchesMap);

		boolean wasChanged = ((dataALItemUpdateDetails != null) &&  !dataALItemUpdateDetails.isEmpty());
		List<ChangeDetails> changeDetailsList = dataALItemUpdateDetails;

		if (wasChanged) {
			lastModified = new Date();
		}

		return changeDetailsList;
	}

	//return null if valid, ValidationResults otherwise
	//does not change the feature! 
	//for features cannot call super update since some steps are unique to feature even with general fields
	public ValidationResults doValidateFeatureJSON(JSONObject featureObj, ServletContext context, String seasonId, LinkedList<String> addedSubFeatures, LinkedList<String> missingSubFeatures, InternalUserGroups userGroups, 
			HashMap<UUID, Integer> existingFeaturesInUpdate, String parent, HashMap<String, JSONObject> updatedFeaturesMap, UserInfo userInfo,
			ValidationCache tester,
			Map<String, BaseAirlockItem> airlockItemsDB, Environment env,List<OriginalString> copiedStrings) {

		ValidationResults superRes = super.doValidateFeatureJSON(featureObj, context, seasonId, addedSubFeatures, missingSubFeatures, userGroups, existingFeaturesInUpdate, parent, updatedFeaturesMap, userInfo, tester, airlockItemsDB, env,copiedStrings);

		if (superRes!=null)
			return superRes;

		@SuppressWarnings("unchecked")
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

		Season curSeason = seasonsDB.get(seasonId);
		if (curSeason == null) {
			return new ValidationResults("The season of the given configuration does not exist.", Status.BAD_REQUEST);
		}

		
		if (parent!=null) {
			BaseAirlockItem parentObj = airlockItemsDB.get(parent);
			if (parentObj == null) {
				return new ValidationResults("Parent feature " + parent + " not found", Status.BAD_REQUEST);
			}			
			if (curSeason.getServerVersion().equals(Constants.PRE_2_1_SERVER_VERSION)) { 

				if (parentObj.getType().equals(Type.CONFIGURATION_RULE)) {
					return new ValidationResults("In Airlock 2.0 and earlier, you cannot add a configuration under another configuration.", Status.BAD_REQUEST);
				}
			}							
		}
		
		try {	

			Action action = Action.ADD;
			if (featureObj.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && featureObj.get(Constants.JSON_FIELD_UNIQUE_ID)!=null) {
				//if JSON contains uniqueId - update an existing feature otherwise create a new feature
				action = Action.UPDATE;			
			}

			if (parent!=null) {
				BaseAirlockItem parentObj = airlockItemsDB.get(parent);
				if (parentObj == null) {
					return new ValidationResults("Parent feature " + parent + " not found", Status.BAD_REQUEST);
				}

				if (parentObj.getType().equals(Type.ROOT)) {
					return new ValidationResults("Cannot add configuration rule under the root.", Status.BAD_REQUEST);					
				}

				if (parentObj.getType().equals(Type.MUTUAL_EXCLUSION_GROUP)) {
					return new ValidationResults("Cannot add configuration rule under mutual exclusion group.", Status.BAD_REQUEST);					
				}
				
				if (parentObj.getType().equals(Type.ORDERING_RULE)) {
					return new ValidationResults("Cannot add configuration rule under ordering rule.", Status.BAD_REQUEST);					
				}

				if (parentObj.getType().equals(Type.ORDERING_RULE_MUTUAL_EXCLUSION_GROUP)) {
					return new ValidationResults("Cannot add configuration rule under ordering rule mutual exclusion group.", Status.BAD_REQUEST);					
				}
				
				if (parentObj.getType().equals(Type.ENTITLEMENT_MUTUAL_EXCLUSION_GROUP)) {
					return new ValidationResults("Cannot add configuration rule under an entitlemens mutual exclusion group.", Status.BAD_REQUEST);					
				}
				
				if (parentObj.getType().equals(Type.PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP)) {
					return new ValidationResults("Cannot add configuration rule under a purchase options mutual exclusion group.", Status.BAD_REQUEST);					
				}				
			}

			//configuration - mandatory field
			if (!featureObj.containsKey(Constants.JSON_FEATURE_FIELD_CONFIGURATION) || featureObj.get(Constants.JSON_FEATURE_FIELD_CONFIGURATION) == null || featureObj.getString(Constants.JSON_FEATURE_FIELD_CONFIGURATION).isEmpty()) 
				return new ValidationResults("The configuration field is missing.", Status.BAD_REQUEST);				

			String configurationStr = featureObj.getString(Constants.JSON_FEATURE_FIELD_CONFIGURATION); //validate that is json value

			String stageStr = featureObj.getString(Constants.JSON_FEATURE_FIELD_STAGE);
			Stage stage = Utilities.strToStage(stageStr);
			if (stage == null) {
				return new ValidationResults("Illegal configuration stage: '" + stageStr + "'", Status.BAD_REQUEST);
			}				

			//minAppVersion
			if (!featureObj.containsKey(Constants.JSON_FEATURE_FIELD_MIN_APP_VER) || featureObj.get(Constants.JSON_FEATURE_FIELD_MIN_APP_VER) == null || featureObj.getString(Constants.JSON_FEATURE_FIELD_MIN_APP_VER).isEmpty()) {
				return new ValidationResults("The minAppVersion field is missing.", Status.BAD_REQUEST);					
			}			

			String minAppVer = featureObj.getString(Constants.JSON_FEATURE_FIELD_MIN_APP_VER);

			featureObj.getJSONObject(Constants.JSON_FEATURE_FIELD_RULE); //we know that exists since dataAirlockItem validation is done
			Rule tmpRule = new Rule();
			tmpRule.fromJSON(featureObj.getJSONObject(Constants.JSON_FEATURE_FIELD_RULE));

			if (tmpRule.getForce() == true && !validAdmin(userInfo)) {
				return new ValidationResults("Only a user with the Administrator role can create or update a feature or configuration without validation.", Status.BAD_REQUEST); 
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
					configJsonObj = VerifyRule.fullConfigurationEvaluation(ruleString, configurationStr, info.minimalInvoker, info.maximalInvoker);

				} catch (ValidationException e) {
					return new ValidationResults("Validation error: " + e.getMessage(), Status.BAD_REQUEST);		
				}
			}

			//skip configuration validation against schema if rule.force is true 
			if ((action == Action.ADD || parent == null) && !tmpRule.getForce()) { //only in configRule addition or update of configRule alone 
				//validate that default configuration match the feature schema
				JSONObject parentSchema = getParentFeatureSchema (parent, airlockItemsDB);
				if (parentSchema!=null && configJsonObj != null) {
					try {
						String ajv = (String)context.getAttribute(Constants.SCHEMA_VALIDATOR_AJV_PARAM_NAME);
						String validator = (String)context.getAttribute(Constants.SCHEMA_VALIDATOR_VALIDATOR_PARAM_NAME);				

						boolean relaxed = true;
						SchemaValidator.validation(validator, ajv, parentSchema.toString(), configJsonObj.toString(), relaxed);
					} catch (ValidationException ve) {
						return new ValidationResults("The configuration does not match the Configuration Schema: " + ve.getMessage(), Status.BAD_REQUEST);
					}
				}
			}

			if (action == Action.UPDATE) {
				ValidationResults res = validateMasterFeatureNotChangedFromBranch(featureObj, airlockItemsDB, env);
				if (res!=null)
					return res;
			}			
		} catch (JSONException jsne) {
			return new ValidationResults(jsne.getMessage(), Status.BAD_REQUEST);
		}
		catch (ClassCastException cce) {
			return new ValidationResults("Illegal feature JSON: " + cce.getMessage(), Status.BAD_REQUEST);

		} catch (GenerationException e) {
			return new ValidationResults("Failed to generate a data sample to validate the configuration:" + e.getMessage(), Status.BAD_REQUEST);		
		}

		return null;
	}
	static boolean validAdmin(UserInfo userInfo)
	{

		return userInfo == null || userInfo.getRoles().contains(Constants.RoleType.Administrator);

	}

	//considerProdUnderDev: for prod runtime file - we should consider prod under dev as dev
	//                      for user permissions - we should consider prod under dev as prod 


	public ValidationResults validateProductionDontChanged(JSONObject updatedFeatureData, Map<String, BaseAirlockItem> airlockItemsDB, Branch branch, ServletContext context, boolean considerProdUnderDevAsDev, Environment env, boolean ignoreUserGroups) throws JSONException {

		ValidationResults superRes = super.validateProductionDontChanged(updatedFeatureData, airlockItemsDB, branch, context, considerProdUnderDevAsDev, env, ignoreUserGroups);

		if (superRes!=null && !superRes.status.equals(Status.OK))
			return superRes;

		//At this stage we can be sure that all mandatory fields exist and legal types exist in the json since validate was previously called.		

		String err = "Unable to update the feature. Only a user with the Administrator or Product Lead role can change a subitem that is in the production stage.";

		boolean isProduction = false;
		if (considerProdUnderDevAsDev) {
			isProduction = (isProductionFeature(this, airlockItemsDB));
		}
		else {
			isProduction = (stage == Stage.PRODUCTION);
		}

		if (isProduction) {
			if (isChanged(updatedFeatureData, airlockItemsDB))
				return new ValidationResults(err, Status.BAD_REQUEST);						

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

		//Creator, creation date, seasonId and type should not be updated

		//At this stage we can be sure that all mandatory fields exist and legal types exist in the json since validate was previously called.

		String err = "You cannot update an item that is not checked out. First check out the item. To update a configuration, check out its parent feature.";

		if (isChanged(updatedFeatureData, airlockItemsDB))
			return new ValidationResults(err, Status.BAD_REQUEST);				

		return null;
	}	

	boolean isChanged (JSONObject updatedFeatureData, Map<String, BaseAirlockItem> airlockItemsDB) throws JSONException {

		if (updatedFeatureData.containsKey(Constants.JSON_FEATURE_FIELD_CONFIGURATION) && updatedFeatureData.get(Constants.JSON_FEATURE_FIELD_CONFIGURATION)!=null) {
			String updatedConfiguration = updatedFeatureData.getString(Constants.JSON_FEATURE_FIELD_CONFIGURATION);
			if (!configuration.equals(updatedConfiguration)) {
				return true;				
			}
		}

		return false;		
	}


	//return null if not found
	private JSONObject getParentFeatureSchema(String parent, Map<String, BaseAirlockItem> airlockItemsDB) {
		JSONObject schema = null;
		if (parent == null) {
			//in update - go to the features parent
			parent = this.parent.toString();
		}


		//after parent validation - we know that the parent is FEATURE or CONFIG_MUTUAL_EXCLUSION_GROUP
		BaseAirlockItem parentObj = airlockItemsDB.get(parent);

		while (parentObj!=null && !(parentObj instanceof FeatureItem)) {
			parentObj = airlockItemsDB.get(parentObj.getParent().toString());
		}

		if (parentObj instanceof FeatureItem) {
			return ((FeatureItem)parentObj).getConfigurationSchema();
		}
		
		return schema;
	}

	public FeatureItem getParentFeature (Map<String, BaseAirlockItem> airlockItemsDB) {
		//after parent validation - we know that the parent is FEATURE or CONFIG_MUTUAL_EXCLUSION_GROUP
		BaseAirlockItem parentObj = airlockItemsDB.get(parent.toString());

		while (parentObj!=null && parentObj.getType()!=Type.FEATURE && parentObj.getType()!=Type.ENTITLEMENT && parentObj.getType()!=Type.PURCHASE_OPTIONS) {
			parentObj = airlockItemsDB.get(parentObj.getParent().toString());
		}

		if (parentObj instanceof FeatureItem)
			return ((FeatureItem)parentObj);

		return null; //should not happen since already validated and config is child of feature
	}
	
	protected ValidationResults doValidateAirlockDataItemJSON(JSONObject featureObj, ServletContext context, String seasonId, LinkedList<String> addedSubFeatures, LinkedList<String> missingSubFeatures, InternalUserGroups userGroups, 
			HashMap<UUID, Integer> existingFeaturesInUpdate, String parent, HashMap<String, JSONObject> updatedFeaturesMap, UserInfo userInfo,
			ValidationCache tester,
			Map<String, BaseAirlockItem> airlockItemsDB, Environment env,List<OriginalString> copiedStrings) {

		return super.doValidateFeatureJSON(featureObj, context, seasonId, addedSubFeatures, missingSubFeatures, userGroups, existingFeaturesInUpdate, parent, updatedFeaturesMap, userInfo, tester, airlockItemsDB, env,copiedStrings);
	}
}
