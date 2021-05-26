package com.ibm.airlock.admin;

import java.util.*;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response.Status;

import com.ibm.airlock.admin.translations.OriginalString;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.AnalyticsServices;
import com.ibm.airlock.Constants;
import com.ibm.airlock.Constants.Action;
import com.ibm.airlock.Constants.BranchStatus;
import com.ibm.airlock.Constants.OutputJSONMode;
import com.ibm.airlock.Constants.Platform;
import com.ibm.airlock.Constants.Stage;
import com.ibm.airlock.Strings;
import com.ibm.airlock.admin.authentication.UserInfo;
import com.ibm.airlock.admin.purchases.EntitlementItem;
import com.ibm.airlock.admin.MergeBranch.MergeException;
import com.ibm.airlock.admin.analytics.GlobalDataCollection;
import com.ibm.airlock.engine.Environment;
import com.ibm.airlock.engine.VerifyRule;

public class FeatureItem extends DataAirlockItem {

	
	protected Boolean defaultIfAirlockSystemIsDown = null; //c+u
	protected JSONObject additionalInfo = null; //like desc
	protected JSONObject configurationSchema = null; //like desc
	protected String defaultConfiguration = null; //like desc
	protected String displayName = null; //like desc
	private Boolean premium = false; //like desc
	protected String entitlement = null; //like desc  
	protected Rule premiumRule = null; //like desc. null has a meaning - removing the rule
	
	protected BaseAirlockItem newInstance()
	{
		return new FeatureItem();
	}
	protected void clone(BaseAirlockItem other)
	{
		super.clone(other);

		FeatureItem fi = (FeatureItem) other;
		copyFields(fi);

		try {
			additionalInfo = (fi.additionalInfo == null) ? null : (JSONObject) Utilities.cloneJson(fi.additionalInfo, true);
			configurationSchema = (fi.configurationSchema == null) ? null : (JSONObject) Utilities.cloneJson(fi.configurationSchema, true);
			premiumRule = (fi.premiumRule == null) ? null : new Rule(fi.premiumRule);
		}
		catch (Exception e) {}
	}
	protected void shallowClone(BaseAirlockItem other)
	{
		super.shallowClone(other);

		FeatureItem fi = (FeatureItem) other;
		copyFields(fi);
		additionalInfo = fi.additionalInfo;
		configurationSchema = fi.configurationSchema ;
		premiumRule = fi.premiumRule;
	}
	void copyFields(FeatureItem fi)
	{
		defaultIfAirlockSystemIsDown = fi.defaultIfAirlockSystemIsDown;
		defaultConfiguration = fi.defaultConfiguration;
		displayName = fi.displayName;
		premium = fi.premium;
		entitlement = fi.entitlement;
	}

	public Boolean getDefaultIfAirlockSystemIsDown() {
		return defaultIfAirlockSystemIsDown;
	}
	public void setDefaultIfAirlockSystemIsDown(Boolean defaultIfAirlockSystemIsDown) {
		this.defaultIfAirlockSystemIsDown = defaultIfAirlockSystemIsDown;
	}
	public Boolean getPremium() {
		return premium;
	}
	public void setPremium(Boolean premium) {
		this.premium = premium;
	}
	public String getEntitlement() {
		return entitlement;
	}
	public void setEntitlement(String entitlement) {
		this.entitlement = entitlement;
	}
	
	public JSONObject getAdditionalInfo() {
		return additionalInfo;
	}
	public void setAdditionalInfo(JSONObject additionalInfo) {
		this.additionalInfo = additionalInfo;
	}
	public JSONObject getConfigurationSchema() {
		return configurationSchema;
	}
	public void setConfigurationSchema(JSONObject configurationSchema) {
		this.configurationSchema = configurationSchema;
	}
	public String getDefaultConfiguration() {
		return defaultConfiguration;
	}
	public void setDefaultConfiguration(String defaultConfiguration) {
		this.defaultConfiguration = defaultConfiguration;
	}
	
	public String getDisplayName() {
		return displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	public Rule getPremiumRule() {
		return premiumRule;
	}
	public void setPremiumRule(Rule premiumRule) {
		this.premiumRule = premiumRule;
	}
	public FeatureItem() {
		type = Type.FEATURE;
		featuresItems = new LinkedList<BaseAirlockItem>();
		configurationRuleItems = new LinkedList<BaseAirlockItem>();
		orderingRuleItems = new LinkedList<BaseAirlockItem>();
	}

	private JSONObject toDeltaJson(JSONObject res, ServletContext context, OutputJSONMode mode, Environment env) throws JSONException {
		@SuppressWarnings("unchecked")
		Map<String, BaseAirlockItem> airlockItemsDB = (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);

		FeatureItem itemInMaster = (FeatureItem)airlockItemsDB.get(uniqueId.toString());
		
		if (itemInMaster.getDefaultIfAirlockSystemIsDown()!=defaultIfAirlockSystemIsDown) {
			res.put(Constants.JSON_FEATURE_FIELD_DEF_VAL, defaultIfAirlockSystemIsDown);
		}
		
		if (itemInMaster.getAdditionalInfo()!=null && additionalInfo!=null && !itemInMaster.getAdditionalInfo().equals(additionalInfo)) {
			res.put(Constants.JSON_FEATURE_FIELD_ADDITIONAL_INFO, additionalInfo);
		}
		
		if (itemInMaster.getAdditionalInfo()==null && additionalInfo!=null) {
			res.put(Constants.JSON_FEATURE_FIELD_ADDITIONAL_INFO, additionalInfo);
		}
		
		if (itemInMaster.getDefaultConfiguration()!=null && defaultConfiguration!=null && !itemInMaster.getDefaultConfiguration().equals(defaultConfiguration)) {
			res.put(Constants.JSON_FEATURE_FIELD_DEFAULT_CONFIG, defaultConfiguration);
		}
		
		if (itemInMaster.getDefaultConfiguration()==null && defaultConfiguration!=null) {
			res.put(Constants.JSON_FEATURE_FIELD_DEFAULT_CONFIG, defaultConfiguration);
		}
		
		if (displayName != null && (itemInMaster.getDisplayName() == null || !displayName.equals(itemInMaster.getDisplayName()))) {
			res.put(Constants.JSON_FIELD_DISPLAY_NAME, displayName);
		}
		
		if (type.equals(Type.FEATURE) && entitlement != null && !entitlement.isEmpty() && (itemInMaster.getEntitlement() == null || !entitlement.equals(itemInMaster.getEntitlement()))) {
			//delta json is only for runtime files in which the name should  replace the id
			EntitlementItem purItem = (EntitlementItem)env.getAirlockItemsDB().get(entitlement); 
			res.put(Constants.JSON_FIELD_ENTITLEMENT, purItem.getNameSpaceDotName());
		}
		if (type.equals(Type.FEATURE) &&
				((premiumRule != null && (itemInMaster.getPremiumRule() == null || !premiumRule.equals(itemInMaster.getPremiumRule()))) || (premiumRule == null && itemInMaster.getPremiumRule()!=null))) {
			res.put(Constants.JSON_FEATURE_FIELD_PREMIUM_RULE,  premiumRule == null ? null : premiumRule.toJson(mode));
		}		
		if (type.equals(Type.FEATURE) && itemInMaster.getPremium()!=premium) {
			res.put(Constants.JSON_FIELD_PREMIUM, premium);
		}
		
		if (AnalyticsServices.isAnalyticsSupported(env) && mode != OutputJSONMode.ADMIN && env.getAnalytics()!=null) { //add  analytics data only in display mode (dont write it to S3)
			GlobalDataCollection globaldataCollection = null;
			if (env.getAnalytics() != null) {
				globaldataCollection = env.getAnalytics().getGlobalDataCollection();
			}
			else {
				if (env.isInMaster()) {
					@SuppressWarnings("unchecked")
					Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);
					Season season = seasonsDB.get(seasonId.toString());
					
					globaldataCollection = season.getAnalytics().getGlobalDataCollection();
				} else {
					@SuppressWarnings("unchecked")
					Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);			
					Branch branch = branchesDB.get(env.getBranchId());			
					
					globaldataCollection = branch.getAnalytics().getGlobalDataCollection();
				}
			}
				
			if (globaldataCollection.getAnalyticsDataCollection().getFeaturesPrunedConfigurationAttributesMap().containsKey(uniqueId.toString())) {
				res.put(Constants.JSON_FIELD_ATTRIBUTES_FOR_ANALYTICS, globaldataCollection.getAnalyticsDataCollection().returnAttributeArrayForFeature(uniqueId.toString()));		
			}
		}
				
		return res;		
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
				return res;
			}
			else {
				//in case the item is checked out. Dev in master and prod in branch. Write the whole item (not only delta) and set its 
				//branchStatus to NEW in runtime files
				res.put("branchStatus", "NEW");
			}
		}
		
		res.put(Constants.JSON_FEATURE_FIELD_DEFAULT_CONFIG, defaultConfiguration);	
		
		if (mode.equals(OutputJSONMode.ADMIN) || mode.equals(OutputJSONMode.DISPLAY)) {
			res.put(Constants.JSON_FEATURE_FIELD_CONFIGURATION_SCHEMA, configurationSchema);
			res.put(Constants.JSON_FEATURE_FIELD_DISPLAY_NAME, displayName);
			if(userInfo != null) {
				@SuppressWarnings("unchecked")
				Map<String, ArrayList<String>> followersDB = (Map<String, ArrayList<String>>) context.getAttribute(Constants.FOLLOWERS_FEATURES_DB_PARAM_NAME);
				ArrayList<String> followers = followersDB.get(getUniqueId().toString());
				if (followers != null && followers.size() != 0) {
					JSONArray arrFollowers = new JSONArray();
					for (int i = 0; i < followers.size(); ++i) {
						arrFollowers.add(followers.get(i));
					}
					res.put(Constants.JSON_FEATURE_FIELD_FOLLOWERS, arrFollowers);
					res.put(Constants.JSON_FEATURE_FIELD_IS_FOLLOWING, followers.contains(userInfo.getId()));
				}
			}
		}
		
		if (!mode.equals(OutputJSONMode.DEFAULTS)) {
			res.put(Constants.JSON_FEATURE_FIELD_DEF_VAL, defaultIfAirlockSystemIsDown);
			
			res.put(Constants.JSON_FEATURE_FIELD_ADDITIONAL_INFO, additionalInfo);
			//add  analytics data only in display mode (dont write it to S3) and if analytics is set in env (in add feature it is not set since no analytics available for ne wfeatures)	
			if (AnalyticsServices.isAnalyticsSupported(env) && mode != OutputJSONMode.ADMIN && env.getAnalytics()!=null) { 
				GlobalDataCollection globaldataCollection = env.getAnalytics().getGlobalDataCollection();
					
				if (globaldataCollection.getAnalyticsDataCollection().getFeaturesPrunedConfigurationAttributesMap().containsKey(uniqueId.toString())) {
					res.put(Constants.JSON_FIELD_ATTRIBUTES_FOR_ANALYTICS, globaldataCollection.getAnalyticsDataCollection().returnAttributeArrayForFeature(uniqueId.toString()));		
				}
			}

		}
		else {
			//in defaults file, if stage is DEV - defaultIfAirlockSystemIsDown always false
			res.put(Constants.JSON_FEATURE_FIELD_DEF_VAL, stage.equals(Stage.DEVELOPMENT)? false : defaultIfAirlockSystemIsDown);
		}
		
		//premium fields are always in admin, in runtime only if premium is true and never in defaults
		if (type.equals(Type.FEATURE) && 
				(mode.equals(OutputJSONMode.ADMIN) ||mode.equals(OutputJSONMode.DISPLAY) || 
				((mode.equals(OutputJSONMode.RUNTIME_DEVELOPMENT) || mode.equals(OutputJSONMode.RUNTIME_PRODUCTION)) && premium))) {
			res.put(Constants.JSON_FIELD_PREMIUM, premium);			
			res.put(Constants.JSON_FEATURE_FIELD_PREMIUM_RULE, premiumRule == null ? null : premiumRule.toJson(mode));
			
			if (mode.equals(OutputJSONMode.RUNTIME_DEVELOPMENT) || mode.equals(OutputJSONMode.RUNTIME_PRODUCTION)) {
				EntitlementItem purItem = (EntitlementItem)env.getAirlockItemsDB().get(entitlement);
				res.put(Constants.JSON_FIELD_ENTITLEMENT, purItem.getNameSpaceDotName());
			}
			else if (mode.equals(OutputJSONMode.DISPLAY)) {
				if (premium == true && entitlement!=null && !entitlement.isEmpty()) {
					EntitlementItem purItem = (EntitlementItem)env.getAirlockItemsDB().get(entitlement);
					res.put(Constants.JSON_FIELD_ENTITLEMENT_NAME, purItem.getNameSpaceDotName());
				}
				res.put(Constants.JSON_FIELD_ENTITLEMENT, entitlement);
			}
			else {
				res.put(Constants.JSON_FIELD_ENTITLEMENT, entitlement);
			}
			
		}
			
		return res;
	}

	public void fromJSON(JSONObject input, Map<String, BaseAirlockItem> airlockItemsDB, UUID parent, Environment env) throws JSONException {
		super.fromJSON(input, airlockItemsDB, parent, env);
		
		if (input.containsKey(Constants.JSON_FEATURE_FIELD_ADDITIONAL_INFO) && input.get(Constants.JSON_FEATURE_FIELD_ADDITIONAL_INFO)!=null)
			additionalInfo = input.getJSONObject(Constants.JSON_FEATURE_FIELD_ADDITIONAL_INFO);							

		if (input.containsKey(Constants.JSON_FEATURE_FIELD_CONFIGURATION_SCHEMA) && input.get(Constants.JSON_FEATURE_FIELD_CONFIGURATION_SCHEMA)!=null) 
			configurationSchema = input.getJSONObject(Constants.JSON_FEATURE_FIELD_CONFIGURATION_SCHEMA);					

		if (input.containsKey(Constants.JSON_FEATURE_FIELD_DEFAULT_CONFIG) && input.get(Constants.JSON_FEATURE_FIELD_DEFAULT_CONFIG)!=null) 
			defaultConfiguration = input.getString(Constants.JSON_FEATURE_FIELD_DEFAULT_CONFIG);			
		
		if (input.containsKey(Constants.JSON_FEATURE_FIELD_DEF_VAL) && input.get(Constants.JSON_FEATURE_FIELD_DEF_VAL)!=null) 
			defaultIfAirlockSystemIsDown = input.getBoolean(Constants.JSON_FEATURE_FIELD_DEF_VAL);
		
		if (input.containsKey(Constants.JSON_FEATURE_FIELD_DISPLAY_NAME) && input.get(Constants.JSON_FEATURE_FIELD_DISPLAY_NAME)!=null) 
			displayName = input.getString(Constants.JSON_FEATURE_FIELD_DISPLAY_NAME);
		
		if (input.containsKey(Constants.JSON_FIELD_PREMIUM) && input.get(Constants.JSON_FIELD_PREMIUM)!=null) 
			premium = input.getBoolean(Constants.JSON_FIELD_PREMIUM);
		
		if (input.containsKey(Constants.JSON_FIELD_ENTITLEMENT) && input.get(Constants.JSON_FIELD_ENTITLEMENT)!=null) 
			entitlement = input.getString(Constants.JSON_FIELD_ENTITLEMENT);
		
		if (input.containsKey(Constants.JSON_FEATURE_FIELD_PREMIUM_RULE) ) { //null has a meaning - removing the rule
			if (input.get(Constants.JSON_FEATURE_FIELD_PREMIUM_RULE)==null) {
				premiumRule = null;
			}
			else {
				premiumRule = new Rule();
				premiumRule.fromJSON(input.getJSONObject(Constants.JSON_FEATURE_FIELD_PREMIUM_RULE));
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
		
		if (inBranch && !branchStatus.equals(BranchStatus.NEW)) {
			return; //add only the specific branch features
		}
		
		String nsDotName = getNameSpaceDotName().toLowerCase(); //toLowerCase so if the same feature with different case exists in 2 branches - it will only be listed once
		if (listedFeatures.contains(nsDotName))
			return;
		
		listedFeatures.add(nsDotName);
				
		//add only features
		StringBuilder sb = namespaceStrBuilderMap.get(namespace);
		if (sb == null) {
			//if the exact namespace is not in map look for the same namespace in different case
			//because if there are 2 identical namespace with different case there is one subclass for both with name as the first namespace encountered.
			Set<String> namespaces = namespaceStrBuilderMap.keySet();
			for (String ns:namespaces) {
				if (ns.equalsIgnoreCase(namespace)) {
					sb = namespaceStrBuilderMap.get(ns);
					break;
				}
			}
			if (sb == null) { //namespace not found in the same/different case - add namespace to map
				sb = new StringBuilder();
				namespaceStrBuilderMap.put(namespace, sb);
			}
		}

		String normalizedName = normlizeName (name);

		if (platform.equals(Platform.Android)) {
			sb.append("		public static final String " + normalizedName.toUpperCase() + "=\"" + namespace + "." + name + "\";\n");
		}	
		else if (platform.equals(Platform.c_sharp)) {
			sb.append("			let " + normalizedName.toUpperCase() + "=\"" + namespace + "." + name + "\"\n");
		}
		else {
			//iOs (swift)
			sb.append("		@objc let " + normalizedName.toUpperCase() + "=\"" + namespace + "." + name + "\"\n");
		}		
	}

	private String normlizeName(String name) {
		String normalizedString = name.replace('.', '_');		
		normalizedString= normalizedString.replace(' ', '_');

		return normalizedString;
	}

	@Override
	public BaseAirlockItem duplicate(String minVersion, UUID newSeasonId, UUID parentId, Map<String, BaseAirlockItem> airlockItemsDB, 
			HashMap<String, String> oldToDuplicatedFeaturesId, ServletContext context, boolean duplicateSubFeatures, boolean createNewId,
			ValidationCache tester) {
		
		FeatureItem res = new FeatureItem();
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
		res.setPremium(premium);
		res.setEntitlement(entitlement);
		
		//in new seasons there are no bitmaps	
		res.setRolloutPercentageBitmap(rolloutPercentageBitmap); //if bitmap exists it should remain - even in new seasons.
		res.setRule(rule == null?null:Rule.duplicteForNextSeason(rule)); //
		res.setPremiumRule(premiumRule == null?null:Rule.duplicteForNextSeason(premiumRule)); //
		res.setStage(stage);

		if (duplicateSubFeatures && featuresItems!=null) {
			for (int i=0;i<featuresItems.size(); i++) {
				BaseAirlockItem newAirlockItem = featuresItems.get(i).duplicate(minVersion, newSeasonId, res.getUniqueId(), airlockItemsDB, oldToDuplicatedFeaturesId, context, duplicateSubFeatures, createNewId, tester);
				res.getFeaturesItems().add(newAirlockItem);					
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
			Boolean updatedDefaultIfAirlockSystemIsDown = updatedAirlockdItemData.getBoolean(Constants.JSON_FEATURE_FIELD_DEF_VAL);
			if (defaultIfAirlockSystemIsDown != updatedDefaultIfAirlockSystemIsDown) {
				updateDetails.append("'defaultIfAirlockSystemIsDown' changed from " + defaultIfAirlockSystemIsDown + " to " + updatedDefaultIfAirlockSystemIsDown + "\n");
				defaultIfAirlockSystemIsDown  = updatedDefaultIfAirlockSystemIsDown;
				wasChanged = true;
				runtimeFieldChnaged = true;
			}		
			
			if (updatedAirlockdItemData.containsKey(Constants.JSON_FEATURE_FIELD_ADDITIONAL_INFO) && updatedAirlockdItemData.get(Constants.JSON_FEATURE_FIELD_ADDITIONAL_INFO)!=null) {
				JSONObject updatedAdditionalInfo = updatedAirlockdItemData.getJSONObject(Constants.JSON_FEATURE_FIELD_ADDITIONAL_INFO);
				if (!Utilities.jsonObjsAreEqual(additionalInfo, updatedAdditionalInfo)) {
					updateDetails.append("'additionalInfo' changed from " + (additionalInfo==null?"null":additionalInfo.toString()) + " to " + updatedAdditionalInfo.toString() + "\n");
					additionalInfo  = updatedAdditionalInfo;
					wasChanged = true;
					runtimeFieldChnaged = true;
				}
			}
	
			if (updatedAirlockdItemData.containsKey(Constants.JSON_FEATURE_FIELD_CONFIGURATION_SCHEMA) && updatedAirlockdItemData.get(Constants.JSON_FEATURE_FIELD_CONFIGURATION_SCHEMA)!=null) {
				JSONObject updatedConfigurationSchema = updatedAirlockdItemData.getJSONObject(Constants.JSON_FEATURE_FIELD_CONFIGURATION_SCHEMA);
				if (!Utilities.jsonObjsAreEqual(configurationSchema, updatedConfigurationSchema)) {
					updateDetails.append("'configurationSchema' changed from " + (configurationSchema==null?"null":configurationSchema.toString()) + " to " + updatedConfigurationSchema.toString() + "\n");
					configurationSchema  = updatedConfigurationSchema;
					wasChanged = true; 
				}
			}
	
			if (updatedAirlockdItemData.containsKey(Constants.JSON_FEATURE_FIELD_DEFAULT_CONFIG) && updatedAirlockdItemData.get(Constants.JSON_FEATURE_FIELD_DEFAULT_CONFIG)!=null) {
				String updatedDefConfiguration = updatedAirlockdItemData.getString(Constants.JSON_FEATURE_FIELD_DEFAULT_CONFIG);
				if (updatedDefConfiguration!=null && !updatedDefConfiguration.equals(defaultConfiguration)) {
					updateDetails.append("'defaultConfiguration' changed from " + defaultConfiguration + " to " + updatedDefConfiguration + "\n");
					defaultConfiguration  = updatedDefConfiguration;
					wasChanged = true; 
					runtimeFieldChnaged = true;
				}					
			}
			
			if (updatedAirlockdItemData.containsKey(Constants.JSON_FEATURE_FIELD_DISPLAY_NAME) && updatedAirlockdItemData.get(Constants.JSON_FEATURE_FIELD_DISPLAY_NAME)!=null) {
				String updatedDisplayName = updatedAirlockdItemData.getString(Constants.JSON_FEATURE_FIELD_DISPLAY_NAME);
				if (updatedDisplayName!=null && !updatedDisplayName.equals(displayName)) {
					updateDetails.append("'displayName' changed from " + displayName + " to " + updatedDisplayName + "\n");
					displayName  = updatedDisplayName;
					wasChanged = true; 
				}					
			}
			
			if (updatedAirlockdItemData.containsKey(Constants.JSON_FIELD_PREMIUM) && updatedAirlockdItemData.get(Constants.JSON_FIELD_PREMIUM)!=null) {	
				Boolean updatedPremium = updatedAirlockdItemData.getBoolean(Constants.JSON_FIELD_PREMIUM);
				if (premium != updatedPremium) {
					updateDetails.append("'premium' changed from " + premium + " to " + updatedPremium + "\n");
					premium  = updatedPremium;
					wasChanged = true;
					runtimeFieldChnaged = true;
				}		
			}
			
			if (updatedAirlockdItemData.containsKey(Constants.JSON_FIELD_ENTITLEMENT) && updatedAirlockdItemData.get(Constants.JSON_FIELD_ENTITLEMENT)!=null) {
				String updatedEntitlement = updatedAirlockdItemData.getString(Constants.JSON_FIELD_ENTITLEMENT);
				if (updatedEntitlement!=null && !updatedEntitlement.equals(entitlement)) {
					String tmpCurEntitlement = entitlement;
					if (entitlement!=null && !entitlement.isEmpty()) {
						tmpCurEntitlement = airlockItemsDB.get(entitlement).getNameSpaceDotName() + "(" + entitlement + ")"; 
					}
					String tmpUpdatedEntitlement = updatedEntitlement;
					if (updatedEntitlement!=null && !updatedEntitlement.isEmpty()) {
						tmpUpdatedEntitlement = airlockItemsDB.get(updatedEntitlement).getNameSpaceDotName() + "(" + updatedEntitlement + ")"; 
					}
					updateDetails.append("'entitlement' changed from " + tmpCurEntitlement + " to " + tmpUpdatedEntitlement + "\n");
					entitlement  = updatedEntitlement;
					wasChanged = true; 
					runtimeFieldChnaged = true;
				}					
			}
			
			if (updatedAirlockdItemData.containsKey(Constants.JSON_FEATURE_FIELD_PREMIUM_RULE)) {
				 if (updatedAirlockdItemData.get(Constants.JSON_FEATURE_FIELD_PREMIUM_RULE)==null) {
					 if (premiumRule!=null) {
						 updateDetails.append("'premiumRule' changed from \n" + premiumRule.toJson(OutputJSONMode.ADMIN).toString() + "\nto\n  null \n");
						 premiumRule = null;					
						 wasChanged = true;
						 runtimeFieldChnaged = true;
					 }
				 }
				 else {
					 JSONObject updatedPremiumRuleJSON = updatedAirlockdItemData.getJSONObject(Constants.JSON_FEATURE_FIELD_PREMIUM_RULE);
					 Rule updatedPremiumRule = new Rule();
					 updatedPremiumRule.fromJSON(updatedPremiumRuleJSON);
					 if (premiumRule == null) {
						 updateDetails.append("'premiumRule' changed from \n null \nto\n" + updatedPremiumRule.toJson(OutputJSONMode.ADMIN).toString() + "\n");
						 premiumRule = updatedPremiumRule;					
						 wasChanged = true;
						 runtimeFieldChnaged = true;
					 }
					 else { //non of the premiumRules are null	
						if (!premiumRule.equals(updatedPremiumRule)) {
							updateDetails.append("'premiumRule' changed from \n " + premiumRule.toJson(OutputJSONMode.ADMIN).toString() + " \nto\n" + updatedPremiumRule.toJson(OutputJSONMode.ADMIN).toString() + "\n");
							premiumRule = updatedPremiumRule;					
							wasChanged = true;
							runtimeFieldChnaged = true;
						}
					 }
				 }
			}
		}	

		if(!updateDetails.toString().isEmpty()){
			Boolean isProductionChange = updatedAirlockdItemData.getString(Constants.JSON_FEATURE_FIELD_STAGE).equals("PRODUCTION") || currentlyInProd;
			changeDetailsList.add(new ChangeDetails(updateDetails.toString(),this,isProductionChange));
		}
		
		//in master if one of the runtime field was changed we should go over all the branches and if the item is checked out - update runtime since the delta was changed
		if (branch == null && runtimeFieldChnaged) {
			addBranchedToUpdatedBranchesMap(uniqueId.toString(), updatedBranchesMap, context, env);
		}

		if (wasChanged) {
			lastModified = new Date();
		}

		/*if (updateDetails.length()!=0) {
			updateDetails.insert(0,"Feature changes: ");
		}*/
		return changeDetailsList;

	}


	//return null if valid, ValidationResults otherwise
	//does not change the feature! 
	//for features cannot call super update since some steps are unique to feature even with general fields
	public ValidationResults doValidateFeatureJSON(JSONObject featureObj, ServletContext context, String seasonId, LinkedList<String> addedSubFeatures, 
			LinkedList<String> missingSubFeatures, InternalUserGroups userGroups, HashMap<UUID, Integer> existingFeaturesInUpdate, 
			String parent, HashMap<String, JSONObject> updatedFeaturesMap, UserInfo userInfo,
			ValidationCache tester,
			Map<String, BaseAirlockItem> airlockItemsDB, Environment env,List<OriginalString> copiedStings) {
		
		@SuppressWarnings("unchecked")
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);
		
		Season curSeason = seasonsDB.get(seasonId);
		if (curSeason == null) {
			return new ValidationResults("The season of the given feature does not exist.", Status.BAD_REQUEST);
		}
		
		try {
			Action action = Action.ADD;
			String curUniqueId = null;
			if (featureObj.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && featureObj.get(Constants.JSON_FIELD_UNIQUE_ID)!=null) {
				//if JSON contains uniqueId - update an existing feature otherwise create a new feature
				action = Action.UPDATE;
				curUniqueId = featureObj.getString(Constants.JSON_FIELD_UNIQUE_ID);
			}

			//type
			if (!featureObj.containsKey(Constants.JSON_FEATURE_FIELD_TYPE) || featureObj.getString(Constants.JSON_FEATURE_FIELD_TYPE) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_TYPE), Status.BAD_REQUEST);
			}

			String typeStr = featureObj.getString(Constants.JSON_FEATURE_FIELD_TYPE);
			Type typeObj = strToType(typeStr);
			if (typeObj == null) {
				return new ValidationResults("Illegal type: '" + typeStr + "'", Status.BAD_REQUEST);
			}

			if (parent!=null) {
				BaseAirlockItem parentObj = airlockItemsDB.get(parent);
				if (parentObj == null) {
					return new ValidationResults("Parent feature " + parent + " not found", Status.BAD_REQUEST);
				}

				if (parentObj.getType().equals(Type.CONFIGURATION_RULE)) {
					return new ValidationResults("Cannot add " + typeStr + " under a configuration.", Status.BAD_REQUEST);					
				}

				if (parentObj.getType().equals(Type.CONFIG_MUTUAL_EXCLUSION_GROUP)) {
					return new ValidationResults("Cannot add " + typeStr + " under a configuration mutual exclusion group.", Status.BAD_REQUEST);					
				}	
				
				if (parentObj.getType().equals(Type.ORDERING_RULE)) {
					return new ValidationResults("Cannot add " + typeStr + " under a ordeing rule.", Status.BAD_REQUEST);					
				}

				if (parentObj.getType().equals(Type.ORDERING_RULE_MUTUAL_EXCLUSION_GROUP)) {
					return new ValidationResults("Cannot add " + typeStr + " under a ordeing rule mutual exclusion group.", Status.BAD_REQUEST);					
				}	
				
				if (typeObj.equals(Type.FEATURE)) { //not a derived purchase object
					if (parentObj.getType().equals(Type.ENTITLEMENT_MUTUAL_EXCLUSION_GROUP)) {
						return new ValidationResults(Strings.cannotAddFeatureUnderEntitlementMtx, Status.BAD_REQUEST);					
					}
					
					if (parentObj.getType().equals(Type.PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP)) {
						return new ValidationResults(Strings.cannotAddFeatureUnderPurchaseOptionsMtx, Status.BAD_REQUEST);					
					}
					
					if (parentObj.getType().equals(Type.ENTITLEMENT)) {
						return new ValidationResults(Strings.cannotAddFeatureUnderEntitlement, Status.BAD_REQUEST);					
					}
					
					if (parentObj.getType().equals(Type.PURCHASE_OPTIONS)) {
						return new ValidationResults(Strings.cannotAddFeatureUnderPurchaseOptions, Status.BAD_REQUEST);					
					}
					
					if (parentObj.getType().equals(Type.ROOT) && curSeason.getEntitlementsRoot().getUniqueId().equals(parentObj.getUniqueId())) {
						return new ValidationResults(Strings.cannotAddFeatureUnderEntitlementsRoot, Status.BAD_REQUEST);					
					}
				}
			}

			//defaultIfAirlockSystemIsDown
			if (!featureObj.containsKey(Constants.JSON_FEATURE_FIELD_DEF_VAL) || featureObj.get(Constants.JSON_FEATURE_FIELD_DEF_VAL) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_DEF_VAL), Status.BAD_REQUEST);					
			}

			boolean defaultVal = featureObj.getBoolean(Constants.JSON_FEATURE_FIELD_DEF_VAL); //validate that is boolean value

			//stage
			if (!featureObj.containsKey(Constants.JSON_FEATURE_FIELD_STAGE) || featureObj.getString(Constants.JSON_FEATURE_FIELD_STAGE) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_STAGE), Status.BAD_REQUEST);					
			}

			String stageStr = featureObj.getString(Constants.JSON_FEATURE_FIELD_STAGE);
			Stage stageObj = Utilities.strToStage(stageStr);
			if (stageObj == null) {
				return new ValidationResults("Illegal stage: '" + stageStr + "'", Status.BAD_REQUEST);
			}

			//validate that no more than maxFeaturesOn in PRODUCTION has (defaultIfAirlockSystemIsDown = true)
			if (defaultVal == true) {
				if (stageObj.equals(Stage.PRODUCTION)) {

					BaseAirlockItem parentItem = null;
					if (parent != null) {
						parentItem = airlockItemsDB.get(parent);						
					}
					else {
						//in create feature and in subFeatures the parent is specified. In update (the root of the update)
						//the parent is null so taken from this.
						parentItem = airlockItemsDB.get(this.parent.toString());						
					}

					if (parentItem.getType().equals(Type.MUTUAL_EXCLUSION_GROUP)) {
						if (parentItem.getFeaturesItems()!=null) {
							int featuresInProdDefTrue = 0;						
							for (BaseAirlockItem sibling:parentItem.getFeaturesItems()) {
								if (sibling.getType().equals(Type.FEATURE) && ((FeatureItem)sibling).getStage().equals(Stage.PRODUCTION) &&
										((FeatureItem)sibling).getDefaultIfAirlockSystemIsDown()==true) {
									//in update - skip yourself from the counting since you are already on
									if (curUniqueId == null || !curUniqueId.equals(sibling.getUniqueId().toString())) {
										
										featuresInProdDefTrue++;
									}
								}
							}

							if (featuresInProdDefTrue >= ((MutualExclusionGroupItem)parentItem).getMaxFeaturesOn()) {
								return new ValidationResults("Cannot add the feature to this mutual exclusion group. The feature is in the production stage and the Default setting is On. The value of the Maximal Number of Features On setting was exceeded.", Status.BAD_REQUEST);
							}
						}
					}
				}
			}
			
			//premium - non mandatory field
			Boolean premiumVal = null;
			if (featureObj.containsKey(Constants.JSON_FIELD_PREMIUM) && featureObj.get(Constants.JSON_FIELD_PREMIUM) != null) {
				premiumVal = featureObj.getBoolean(Constants.JSON_FIELD_PREMIUM); //validate that is boolean value
			}

			//entitlement
			String entitlementIdStr = null;
			if (featureObj.containsKey(Constants.JSON_FIELD_ENTITLEMENT) && featureObj.get(Constants.JSON_FIELD_ENTITLEMENT) != null && !featureObj.getString(Constants.JSON_FIELD_ENTITLEMENT).isEmpty()) {
				entitlementIdStr = featureObj.getString(Constants.JSON_FIELD_ENTITLEMENT);
				
				Map<String, BaseAirlockItem> purcahseItemsDB = (Map<String, BaseAirlockItem>)Utilities.getAirlockItemsDB(env.getBranchId(), context);
				
				BaseAirlockItem entitlementObj = purcahseItemsDB.get(entitlementIdStr); 
				if (entitlementObj == null) {
					return new ValidationResults(Strings.noEntitlementItem, Status.BAD_REQUEST);
				}
				if (!entitlementObj.getType().equals(Type.ENTITLEMENT)) {
					return new ValidationResults(Strings.itemIsNotEntitlement, Status.BAD_REQUEST);
				}
				if (!entitlementObj.getSeasonId().equals(curSeason.getUniqueId())) {
					return new ValidationResults(Strings.entitlementInDifferentSeason, Status.BAD_REQUEST);
				}
				if (stageObj.equals(Stage.PRODUCTION) && ((EntitlementItem)entitlementObj).getStage().equals(Stage.DEVELOPMENT)) {
					return new ValidationResults(Strings.featureInProdEntitlementInDev, Status.BAD_REQUEST);
				}
				if (!((EntitlementItem)entitlementObj).hasPurchaseOptionsWithProductId(null, null, stageObj)) {
					return new ValidationResults(Strings.entitlementWithNoStoreProductId, Status.BAD_REQUEST);
				}
			}
			
			if (entitlementIdStr==null && (premiumVal != null && premiumVal)) {
				return new ValidationResults(Strings.premiumFeatureWithoutEntitlement, Status.BAD_REQUEST);
			}
			
			if (entitlementIdStr!=null && (premiumVal == null || !premiumVal)) {
				return new ValidationResults(Strings.entitlementForNonPremiumFeature, Status.BAD_REQUEST);
			}
			
			Rule tmpPremiumRule = null;
			//premiumRule - mandatory if premium == true
			if (featureObj.containsKey(Constants.JSON_FEATURE_FIELD_PREMIUM_RULE) && featureObj.get(Constants.JSON_FEATURE_FIELD_PREMIUM_RULE) != null) {
				featureObj.getJSONObject(Constants.JSON_FEATURE_FIELD_PREMIUM_RULE); //validate legal json
				tmpPremiumRule = new Rule();
				tmpPremiumRule.fromJSON(featureObj.getJSONObject(Constants.JSON_FEATURE_FIELD_PREMIUM_RULE));
				//empty,null or missing ruleString is legal
				
								
				//minAppVersion
				if (!featureObj.containsKey(Constants.JSON_FEATURE_FIELD_MIN_APP_VER) || featureObj.get(Constants.JSON_FEATURE_FIELD_MIN_APP_VER) == null || featureObj.getString(Constants.JSON_FEATURE_FIELD_MIN_APP_VER).isEmpty()) {
					return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_MIN_APP_VER), Status.BAD_REQUEST);					
				}			
				
				String minAppVer = featureObj.getString(Constants.JSON_FEATURE_FIELD_MIN_APP_VER);

				ValidationResults res = tmpPremiumRule.validateRule(stageObj, minAppVer, curSeason, context, tester, userInfo);
				if (res !=null) {
					return res;
				} 			
			}
			
			if (premiumVal!=null && premiumVal && tmpPremiumRule == null) {
				return new ValidationResults(Strings.premiumFeatureWithoutPremiumRule, Status.BAD_REQUEST);
			}
			
			//configurationSchema - non mandatory field
			JSONObject schema = null;
			if (featureObj.containsKey(Constants.JSON_FEATURE_FIELD_CONFIGURATION_SCHEMA) && featureObj.get(Constants.JSON_FEATURE_FIELD_CONFIGURATION_SCHEMA) != null) {
				schema = featureObj.getJSONObject(Constants.JSON_FEATURE_FIELD_CONFIGURATION_SCHEMA); //validate that is json value
			}

			//defaultConfiguration - mandatory field only if schema defined
			String defConfig = null;
			if (schema!=null) {
				if (!featureObj.containsKey(Constants.JSON_FEATURE_FIELD_DEFAULT_CONFIG) || featureObj.get(Constants.JSON_FEATURE_FIELD_DEFAULT_CONFIG) == null || featureObj.getString(Constants.JSON_FEATURE_FIELD_DEFAULT_CONFIG).isEmpty()) {
					return new ValidationResults("The Default Configuration field is required.", Status.BAD_REQUEST);
				}					
			}

			if (featureObj.containsKey(Constants.JSON_FEATURE_FIELD_DEFAULT_CONFIG) && featureObj.get(Constants.JSON_FEATURE_FIELD_DEFAULT_CONFIG) != null) {
				defConfig = featureObj.getString(Constants.JSON_FEATURE_FIELD_DEFAULT_CONFIG);				
			}
			
			featureObj.getJSONObject(Constants.JSON_FEATURE_FIELD_RULE); //we know that exists since dataAirlockItem validation is done
			Rule tmpRule = new Rule();
			tmpRule.fromJSON(featureObj.getJSONObject(Constants.JSON_FEATURE_FIELD_RULE));

			if (tmpRule.getForce() == true && !validAdmin(userInfo)) {
				return new ValidationResults("Only a user with the Administrator role can create or update a feature or configuration without validation.", Status.BAD_REQUEST); 
			}

			//skip default configuration validation if rule.force is true 
			if (defConfig!=null && !tmpRule.getForce()) {
				try {
					new JSONObject(defConfig); //validate that defConfig is a legal json
				} catch (JSONException je) {
					throw new JSONException("illegal Default Configuration JSON: " + je.getMessage() +". Please verify that you are not using functions (for example, translate) or context elements in the default configuration.");
				}
				try {
					VerifyRule.checkDefaultConfiguration(defConfig);
				} catch (ValidationException ve) {
					return new ValidationResults("The Default Configuration is not valid: " + ve.getMessage(), Status.BAD_REQUEST);
				}
			}
			
			//skip schema validation if rule.force is true 
			if (schema!=null && defConfig !=null && !tmpRule.getForce()) {
				String ajv = (String)context.getAttribute(Constants.SCHEMA_VALIDATOR_AJV_PARAM_NAME);
				String validator = (String)context.getAttribute(Constants.SCHEMA_VALIDATOR_VALIDATOR_PARAM_NAME);				
				
				//validate the schema
				try {
					SchemaValidator.validateSchema(validator, null, ajv, schema.toString());
				} catch (ValidationException ve) {
					return new ValidationResults("The Configuration Schema is not valid: " + ve.getMessage(), Status.BAD_REQUEST);
				}
				
				//validate that default configuration match the schema
				try {
					boolean relaxed = false;
					SchemaValidator.validation(validator, ajv, schema.toString(), defConfig.toString(), relaxed);
				} catch (ValidationException ve) {
					return new ValidationResults("The Default Configuration does not match the Configuration Schema: " + ve.getMessage(), Status.BAD_REQUEST);
				}
				
				//go over the configurationRules and check that their configuration matches the schema (partially).
				//in add feature will not be performed since no configRules are specified
				ValidationResults res = validateConfigurationRulesConfigAgainstSchema(featureObj, schema, validator, ajv, curSeason, context, tester);
				if (res!=null)
					return res;
				
			}

			featureObj.getBoolean(Constants.JSON_FEATURE_FIELD_DEF_VAL); //validate that is boolean value

			//additionalInfo - non mandatory field
			if (featureObj.containsKey(Constants.JSON_FEATURE_FIELD_ADDITIONAL_INFO) && featureObj.get(Constants.JSON_FEATURE_FIELD_ADDITIONAL_INFO) != null) {
				featureObj.getJSONObject(Constants.JSON_FEATURE_FIELD_ADDITIONAL_INFO); //validate that is json value								
			}

			//will verify super + subfeatures/configurations
			ValidationResults superRes = super.doValidateFeatureJSON(featureObj, context, seasonId, addedSubFeatures, missingSubFeatures, userGroups, existingFeaturesInUpdate, parent, updatedFeaturesMap, userInfo, tester, airlockItemsDB, env,copiedStings);

			if (superRes!=null)
				return superRes;

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
		catch (MergeException me) {
			return new ValidationResults("Purchases merge error: " + me.getMessage(), Status.BAD_REQUEST);
		}
		return null;
	}
	static boolean validAdmin(UserInfo userInfo)
	{

		return userInfo == null || userInfo.getRoles().contains(Constants.RoleType.Administrator);

	}
	private ValidationResults validateConfigurationRulesConfigAgainstSchema(JSONObject featureObj, JSONObject schema, String validator, String ajv, Season curSeason, ServletContext context,
			ValidationCache tester) throws JSONException {
		if (featureObj.containsKey(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES) && featureObj.get(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES) != null && !featureObj.getJSONArray(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES).isEmpty()) {
			JSONArray configurationRulesArr = featureObj.getJSONArray(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES);
			for (int i=0; i<configurationRulesArr.size(); i++) {
				JSONObject configuleJSON = configurationRulesArr.getJSONObject(i);
				if (configuleJSON.containsKey(Constants.JSON_FEATURE_FIELD_CONFIGURATION) && configuleJSON.get(Constants.JSON_FEATURE_FIELD_CONFIGURATION)!=null) {
					String crConfig = configuleJSON.getString(Constants.JSON_FEATURE_FIELD_CONFIGURATION);
					String minAppVer = configuleJSON.getString(Constants.JSON_FEATURE_FIELD_MIN_APP_VER);

					String stageStr = configuleJSON.getString(Constants.JSON_FEATURE_FIELD_STAGE);
					Stage currStage = "PRODUCTION".equals(stageStr) ? Stage.PRODUCTION : Stage.DEVELOPMENT; // Stage.valueOf(stageStr);

					String ruleString = "";
					if (configuleJSON.containsKey(Constants.JSON_FEATURE_FIELD_RULE)) {
						JSONObject ruleObj = configuleJSON.getJSONObject(Constants.JSON_FEATURE_FIELD_RULE);
						if (ruleObj.containsKey(Constants.JSON_RULE_FIELD_RULE_STR))
							ruleString = ruleObj.getString(Constants.JSON_RULE_FIELD_RULE_STR);						
					}
					
					ValidationCache.Info info;
					try {
						info = tester.getInfo(context, curSeason, currStage, minAppVer);
					} catch (GenerationException ge) {
						return new ValidationResults("Failed to generate data sample: " + ge.getMessage(), Status.BAD_REQUEST);		
					}

					JSONObject configJsonObj = null;
					try {
						configJsonObj = VerifyRule.fullConfigurationEvaluation(ruleString, crConfig, info.minimalInvoker, info.maximalInvoker);

					} catch (ValidationException e) {
						return new ValidationResults("Validation error: " + e.getMessage(), Status.BAD_REQUEST);		
					}

					if (configJsonObj!=null) {
						try {
							boolean relaxed = true;
							SchemaValidator.validation(validator, ajv, schema.toString(), configJsonObj.toString(), relaxed);
						} catch (ValidationException ve) {
							return new ValidationResults("The '" + configuleJSON.getString(Constants.JSON_FIELD_NAME) + "' Configuration does not match the Configuration Schema: " + ve.getMessage(), Status.BAD_REQUEST);
						}
					}
					
					//validate sub configRules
					ValidationResults res = validateConfigurationRulesConfigAgainstSchema(configuleJSON, schema, validator, ajv, curSeason, context, tester);
					if (res != null)
						return res;
				}						
			}
		}	
		
		return null;
		
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
			if (isChanged(updatedFeatureData, airlockItemsDB)) {
				return new ValidationResults(err, Status.UNAUTHORIZED);
			}		
		}

		return superRes;
	}	

	boolean isChanged (JSONObject updatedFeatureData, Map<String, BaseAirlockItem> airlockItemsDB) throws JSONException {
		Boolean updatedDefaultIfAirlockSystemIsDown = updatedFeatureData.getBoolean(Constants.JSON_FEATURE_FIELD_DEF_VAL);
		if (defaultIfAirlockSystemIsDown != updatedDefaultIfAirlockSystemIsDown) 
			return true;

		//optional fields
		if (updatedFeatureData.containsKey(Constants.JSON_FEATURE_FIELD_ADDITIONAL_INFO) && updatedFeatureData.get(Constants.JSON_FEATURE_FIELD_ADDITIONAL_INFO)!=null) {
			JSONObject updatedAdditionalInfo = updatedFeatureData.getJSONObject(Constants.JSON_FEATURE_FIELD_ADDITIONAL_INFO);
			if (!Utilities.jsonObjsAreEqual(additionalInfo, updatedAdditionalInfo)) {
				return true;				
			}
		}

		if (updatedFeatureData.containsKey(Constants.JSON_FEATURE_FIELD_CONFIGURATION_SCHEMA) && updatedFeatureData.get(Constants.JSON_FEATURE_FIELD_CONFIGURATION_SCHEMA)!=null) {
			JSONObject updatedConfigurationSchema = updatedFeatureData.getJSONObject(Constants.JSON_FEATURE_FIELD_CONFIGURATION_SCHEMA);
			if (!Utilities.jsonObjsAreEqual(configurationSchema, updatedConfigurationSchema)) {
				return true;				
			}
		}

		if (updatedFeatureData.containsKey(Constants.JSON_FEATURE_FIELD_DEFAULT_CONFIG) && updatedFeatureData.get(Constants.JSON_FEATURE_FIELD_DEFAULT_CONFIG)!=null) {
			String updatedDefConfiguration = updatedFeatureData.getString(Constants.JSON_FEATURE_FIELD_DEFAULT_CONFIG);
			if (defaultConfiguration == null || !defaultConfiguration.equals(updatedDefConfiguration)) {
				return true;				
			}
		}
		
		if (updatedFeatureData.containsKey(Constants.JSON_FIELD_DISPLAY_NAME) &&  updatedFeatureData.get(Constants.JSON_FIELD_DISPLAY_NAME)!=null) {
			//if missing from json or null - ignore
			String updatedDisplayName = updatedFeatureData.getString(Constants.JSON_FIELD_DISPLAY_NAME);
			if (displayName == null || !displayName.equals(updatedDisplayName)) {
				return true;				
			}
		}
		
		if (updatedFeatureData.containsKey(Constants.JSON_FIELD_PREMIUM) &&  updatedFeatureData.get(Constants.JSON_FIELD_PREMIUM)!=null) {
			Boolean updatedPremium = updatedFeatureData.getBoolean(Constants.JSON_FIELD_PREMIUM);
			if (premium != updatedPremium) 
				return true;
		}
		
		if (updatedFeatureData.containsKey(Constants.JSON_FIELD_ENTITLEMENT) &&  updatedFeatureData.get(Constants.JSON_FIELD_ENTITLEMENT)!=null) {
			//if missing from json or null - ignore
			String updatedEntitlement = updatedFeatureData.getString(Constants.JSON_FIELD_ENTITLEMENT);
			if (entitlement == null || !entitlement.equals(updatedEntitlement)) {
				return true;				
			}
		}

		if (updatedFeatureData.containsKey(Constants.JSON_FEATURE_FIELD_PREMIUM_RULE)) {
			 if (updatedFeatureData.get(Constants.JSON_FEATURE_FIELD_PREMIUM_RULE)==null) {
				 if (premiumRule!=null) {
					 return true;
				 }
			 }
			 else {
				 if (premiumRule == null) {
					 return true;
				 }
				 else { //non of the premiumRules are null
					JSONObject updatedPremiumRuleJSON = updatedFeatureData.getJSONObject(Constants.JSON_FEATURE_FIELD_RULE);
					Rule updatedPremiumRule = new Rule();
					updatedPremiumRule.fromJSON(updatedPremiumRuleJSON);
					if (!premiumRule.equals(updatedPremiumRule)) {
						return true;
					}
				 }
			 }
		}
		return false;
		
	}

}
