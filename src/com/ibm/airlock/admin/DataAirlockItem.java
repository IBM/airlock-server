package com.ibm.airlock.admin;

import java.util.*;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response.Status;

import com.ibm.airlock.Strings;
import com.ibm.airlock.admin.translations.OriginalString;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Constants.Action;
import com.ibm.airlock.Constants.BranchStatus;
import com.ibm.airlock.Constants.OutputJSONMode;
import com.ibm.airlock.Constants.RoleType;
import com.ibm.airlock.Constants.Stage;
import com.ibm.airlock.admin.authentication.UserInfo;
import com.ibm.airlock.admin.MergeBranch.MergeException;
import com.ibm.airlock.admin.analytics.AnalyticsDataCollection;
import com.ibm.airlock.admin.analytics.Experiment;
import com.ibm.airlock.engine.Environment;
import com.ibm.airlock.engine.Percentile;
import com.ibm.airlock.engine.VerifyRule;
import com.ibm.airlock.engine.Version;
import com.ibm.airlock.engine.Percentile.PercentileException;

public abstract class DataAirlockItem extends BaseAirlockItem{
	protected String name = null; //c+u
	protected String namespace = null; //c+u
	protected String description = null; //opt in c+u (if missing or null in update don't change)
	protected Date creationDate = null; //nc + u (not changed)
	protected String creator = null;	//c+u (creator not changed)
	protected Double rolloutPercentage = null; //c+u
	protected String rolloutPercentageBitmap = null; //create should not in update must get it
	protected Stage stage = null; //c+u
	protected String owner = null; //like desc
	protected String minAppVersion = null; //c+u
	protected Rule rule = null; //c+u
	protected Boolean enabled = null; //c+u
	protected Boolean noCachedResults = false; //like desc
	protected String[] internalUserGroups = null; //opt in creation + in update if missing or null ignore , if empty array then emptying

	protected void clone(BaseAirlockItem other)
	{
		super.clone(other);
		DataAirlockItem dai = (DataAirlockItem) other;
		copyFields(dai);
		internalUserGroups = (dai.internalUserGroups == null) ? null : dai.internalUserGroups.clone();
		rule = (dai.rule == null) ? null : new Rule(dai.rule);
	}
	protected void shallowClone(BaseAirlockItem other)
	{
		super.shallowClone(other);
		DataAirlockItem dai = (DataAirlockItem) other;
		copyFields(dai);
		internalUserGroups = dai.internalUserGroups;
		rule = dai.rule;
	}
	void copyFields(DataAirlockItem dai)
	{
		name = dai.name;
		namespace = dai.namespace;
		description = dai.description;
		creationDate = dai.creationDate;
		creator = dai.creator;
		rolloutPercentage = dai.rolloutPercentage;
		rolloutPercentageBitmap = dai.rolloutPercentageBitmap;
		stage = dai.stage;
		owner = dai.owner;
		minAppVersion = dai.minAppVersion;
		enabled = dai.enabled;
		noCachedResults = dai.noCachedResults;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getNamespace() {
		return namespace;
	}
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public Date getCreationDate() {
		return creationDate;
	}
	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}
	public String getCreator() {
		return creator;
	}
	public void setCreator(String creator) {
		this.creator = creator;
	}
	public Double getRolloutPercentage() {
		return rolloutPercentage;
	}
	public void setRolloutPercentage(Double rolloutPercentage) {
		this.rolloutPercentage = rolloutPercentage;
	}
	public String getRolloutPercentageBitmap() {
		return rolloutPercentageBitmap;
	}
	public void setRolloutPercentageBitmap(String rolloutPercentageBitmap) {
		this.rolloutPercentageBitmap = rolloutPercentageBitmap;
	}
	public Stage getStage() {
		return stage;
	}
	public void setStage(Stage stage) {
		this.stage = stage;
	}
	public String getMinAppVersion() {
		return minAppVersion;
	}
	public void setMinAppVersion(String minAppVersion) {
		this.minAppVersion = minAppVersion;
	}
	public Rule getRule() {
		return rule;
	}
	public void setRule(Rule rule) {
		this.rule = rule;
	}
	public Boolean getEnabled() {
		return enabled;
	}
	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}
	public Boolean getNoCachedResults() {
		return noCachedResults;
	}
	public void setNoCachedResults(Boolean noCachedResults) {
		this.noCachedResults = noCachedResults;
	}
	public String[] getInternalUserGroups() {
		return internalUserGroups;
	}
	public void setInternalUserGroups(String[] internalUserGroups) {
		this.internalUserGroups = internalUserGroups;
	}
	public String getOwner() {
		return owner;
	}
	public void setOwner(String owner) {
		this.owner = owner;
	}

	public JSONObject toJson(OutputJSONMode mode, ServletContext context, Environment env, UserInfo userInfo) throws JSONException {
		JSONObject res = super.toJson(mode, context, env, userInfo);
		if (res == null) {
			// this can only happen in runtime_production mode when the item is in development stage
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
		}
		
		res.put(Constants.JSON_FIELD_NAME, name);
		res.put(Constants.JSON_FEATURE_FIELD_NAMESPACE, namespace);
		res.put(Constants.JSON_FEATURE_FIELD_NO_CACHED_RES, noCachedResults);
		res.put(Constants.JSON_FEATURE_FIELD_PERCENTAGE, rolloutPercentage);
		
		if (!mode.equals(OutputJSONMode.DEFAULTS)) {
			res.put(Constants.JSON_FEATURE_FIELD_ENABLED, enabled);			
			res.put(Constants.JSON_FEATURE_FIELD_MIN_APP_VER, minAppVersion);		
			res.put(Constants.JSON_FEATURE_FIELD_STAGE, stage.toString());		
			res.put(Constants.JSON_FEATURE_FIELD_RULE, rule.toJson(mode));
			res.put(Constants.JSON_FIELD_INTERNAL_USER_GROUPS, internalUserGroups);			
			
			if (rolloutPercentageBitmap!=null) //if bitmap exists return as is
				res.put(Constants.JSON_FEATURE_FIELD_PERCENTAGE_BITMAP, rolloutPercentageBitmap);
		}


		if (mode == OutputJSONMode.ADMIN || mode == OutputJSONMode.DISPLAY) {
			res.put(Constants.JSON_FEATURE_FIELD_CREATION_DATE, creationDate.getTime()); 			
			res.put(Constants.JSON_FIELD_DESCRIPTION, description);
			res.put(Constants.JSON_FEATURE_FIELD_CREATOR, creator);
			res.put(Constants.JSON_FEATURE_FIELD_OWNER, owner);				
		}		
		
					
		return res;
	}

	private JSONObject toDeltaJson(JSONObject res, ServletContext context, OutputJSONMode mode) throws JSONException {
		@SuppressWarnings("unchecked")
		Map<String, BaseAirlockItem> airlockItemsDB = (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);

		DataAirlockItem itemInMaster = (DataAirlockItem)airlockItemsDB.get(uniqueId.toString());
		
		res.put(Constants.JSON_FIELD_NAME, name);
		res.put(Constants.JSON_FEATURE_FIELD_NAMESPACE, namespace);
		
		if (itemInMaster.getEnabled()!=enabled) {
			res.put(Constants.JSON_FEATURE_FIELD_ENABLED, enabled);
		}
		
		if (itemInMaster.getNoCachedResults()!=noCachedResults) {
			res.put(Constants.JSON_FEATURE_FIELD_NO_CACHED_RES, noCachedResults);
		}
		
		if (itemInMaster.getMinAppVersion()!=minAppVersion) {	
			res.put(Constants.JSON_FEATURE_FIELD_MIN_APP_VER, minAppVersion);
		}
		
		if (itemInMaster.getStage()!=stage) {
			res.put(Constants.JSON_FEATURE_FIELD_STAGE, stage.toString());
		}
		
		if (!itemInMaster.getRule().equals(rule)) {
			res.put(Constants.JSON_FEATURE_FIELD_RULE, rule.toJson(mode));
		}
		
		if (!Arrays.equals(itemInMaster.getInternalUserGroups(), internalUserGroups)) {
			res.put(Constants.JSON_FIELD_INTERNAL_USER_GROUPS, internalUserGroups);
		}
		
		if (itemInMaster.getRolloutPercentage()!=rolloutPercentage) {
			res.put(Constants.JSON_FEATURE_FIELD_PERCENTAGE, rolloutPercentage);
		}
		
		if (description != null && (itemInMaster.getDescription() == null || !description.equals(itemInMaster.getDescription()))) {
			res.put(Constants.JSON_FIELD_DESCRIPTION, description);
		}
				
		if (rolloutPercentageBitmap!=null && itemInMaster.getRolloutPercentageBitmap()!=null && 
				rolloutPercentageBitmap.equals(itemInMaster.getRolloutPercentageBitmap())) {
			res.put(Constants.JSON_FEATURE_FIELD_PERCENTAGE_BITMAP, rolloutPercentageBitmap);
		}		
		
		return res;
	}

	public void fromJSON(JSONObject input, Map<String, BaseAirlockItem> airlockItemsDB, UUID parent, Environment env) throws JSONException {
		super.fromJSON(input, airlockItemsDB, parent, env);

		name = input.getString(Constants.JSON_FIELD_NAME);			

		if (input.containsKey(Constants.JSON_FIELD_DESCRIPTION) && input.get(Constants.JSON_FIELD_DESCRIPTION)!=null) 
			description = input.getString(Constants.JSON_FIELD_DESCRIPTION).trim();

		if (input.containsKey(Constants.JSON_FEATURE_FIELD_NAMESPACE) && input.get(Constants.JSON_FEATURE_FIELD_NAMESPACE)!=null) 
			namespace = input.getString(Constants.JSON_FEATURE_FIELD_NAMESPACE);

		if (input.containsKey(Constants.JSON_FEATURE_FIELD_CREATOR) && input.get(Constants.JSON_FEATURE_FIELD_CREATOR)!=null)
			creator = input.getString(Constants.JSON_FEATURE_FIELD_CREATOR).trim();

		if (input.containsKey(Constants.JSON_FEATURE_FIELD_ENABLED) && input.get(Constants.JSON_FEATURE_FIELD_ENABLED)!=null)
			enabled = input.getBoolean(Constants.JSON_FEATURE_FIELD_ENABLED);
		
		if (input.containsKey(Constants.JSON_FEATURE_FIELD_NO_CACHED_RES) && input.get(Constants.JSON_FEATURE_FIELD_NO_CACHED_RES)!=null)
			noCachedResults = input.getBoolean(Constants.JSON_FEATURE_FIELD_NO_CACHED_RES);
		
		if (input.containsKey(Constants.JSON_FEATURE_FIELD_OWNER)  && input.get(Constants.JSON_FEATURE_FIELD_OWNER)!=null)
			owner = input.getString(Constants.JSON_FEATURE_FIELD_OWNER).trim();

		if (input.containsKey(Constants.JSON_FEATURE_FIELD_CREATION_DATE) && input.get(Constants.JSON_FEATURE_FIELD_CREATION_DATE)!=null) { 
			long timeInMS = input.getLong(Constants.JSON_FEATURE_FIELD_CREATION_DATE);
			creationDate = new Date(timeInMS);			
		} else {
			creationDate = new Date();
		}	

		if (input.containsKey(Constants.JSON_FEATURE_FIELD_RULE) && input.get(Constants.JSON_FEATURE_FIELD_RULE)!=null) {
			rule = new Rule();
			rule.fromJSON(input.getJSONObject(Constants.JSON_FEATURE_FIELD_RULE));
		}
		
		if (input.containsKey(Constants.JSON_FEATURE_FIELD_STAGE) && input.get(Constants.JSON_FEATURE_FIELD_STAGE)!=null)
			stage = Utilities.strToStage(input.getString(Constants.JSON_FEATURE_FIELD_STAGE));

		if (input.containsKey(Constants.JSON_FIELD_INTERNAL_USER_GROUPS) && input.get(Constants.JSON_FIELD_INTERNAL_USER_GROUPS)!=null) 
			internalUserGroups = Utilities.jsonArrToStringArr(input.getJSONArray(Constants.JSON_FIELD_INTERNAL_USER_GROUPS));						

		if (input.containsKey(Constants.JSON_FEATURE_FIELD_MIN_APP_VER) && input.get(Constants.JSON_FEATURE_FIELD_MIN_APP_VER)!=null) 
			minAppVersion = input.getString(Constants.JSON_FEATURE_FIELD_MIN_APP_VER);						

		
		if (input.containsKey(Constants.JSON_FEATURE_FIELD_PERCENTAGE) && input.get(Constants.JSON_FEATURE_FIELD_PERCENTAGE)!=null) {
			rolloutPercentage = input.getDouble(Constants.JSON_FEATURE_FIELD_PERCENTAGE);

			
			if (isLegacyPercentage(env)) { //only pre 2.5 seasons have bitmaps and the percentage is int
				Integer intPercenatage = rolloutPercentage.intValue();
				if (input.containsKey(Constants.JSON_FEATURE_FIELD_PERCENTAGE_BITMAP) && input.get(Constants.JSON_FEATURE_FIELD_PERCENTAGE_BITMAP)!=null) {
					//not new feature - loading from existing jsons, use existing bitmap
					rolloutPercentageBitmap = input.getString(Constants.JSON_FEATURE_FIELD_PERCENTAGE_BITMAP);
				}
				else {
					try {
						//new feature - create bitmap from specified percentage 
						BaseAirlockItem parentFeature = airlockItemsDB.get(parent.toString());
						String parentBitmap = getParentPercentageBitmap(parentFeature, airlockItemsDB);
						Percentile percentile = null;
						if (parentBitmap == null)
							percentile = new Percentile(intPercenatage);
						else 
							percentile = new Percentile(new Percentile(parentBitmap), intPercenatage);
		
						//System.out.println("percentile = " + Utilities.intArrayToString(percentile.getContent(), null));
						rolloutPercentageBitmap = percentile.toString();
					} catch (PercentileException pe) {
						throw new JSONException("PercentileException: " + pe.getMessage());
					}
					
				}
			}
			else {
				if (input.containsKey(Constants.JSON_FEATURE_FIELD_PERCENTAGE_BITMAP) && input.get(Constants.JSON_FEATURE_FIELD_PERCENTAGE_BITMAP)!=null) {
					//even in new seasons - if bitmap exists leave it as is
					rolloutPercentageBitmap = input.getString(Constants.JSON_FEATURE_FIELD_PERCENTAGE_BITMAP);
				}
			}
		}
	}

	private String getParentPercentageBitmap(BaseAirlockItem parentFeature, Map<String, BaseAirlockItem> airlockItemsDB) {

		if (parentFeature == null) 
			return null;

		while (parentFeature.getType().equals(Type.MUTUAL_EXCLUSION_GROUP) || parentFeature.getType().equals(Type.CONFIG_MUTUAL_EXCLUSION_GROUP)) {
			parentFeature = airlockItemsDB.get(parentFeature.getParent().toString());
			if (parentFeature==null) 
				return null;
		}

		if (parentFeature.getType().equals(Type.ROOT)) {
			return null;
		}

		if (parentFeature instanceof DataAirlockItem) {
			return ((DataAirlockItem)parentFeature).getRolloutPercentageBitmap();
		}

		return null;
	}		

	//Return a string with update details.
	//If nothing was changed - return empty string
	//for features cannot call super update since some steps are unique to feature even with general fields
	public List<ChangeDetails> updateAirlockItem(JSONObject updatedAirlockdItemData, Map<String, BaseAirlockItem> airlockItemsDB, BaseAirlockItem root, Environment env, Branch branch,Boolean isProdChange, ServletContext context, Map<String, Stage> updatedBranchesMap) throws JSONException {
		boolean wasChanged = false;
		Boolean currentlyInProd = this.getStage().toString().equals("PRODUCTION");
		List<ChangeDetails> changeDetailsList = new ArrayList<>();
		StringBuilder updateDetails = new StringBuilder("");

		//String objTypeStr = getObjTypeStrByType();

		//Creator, creation date, seasonId and type should not be updated

		//At this stage we can be sure that all mandatory fields exist and legal types exist in the json since validate was previously called.		

		//in branch - skip master features (branchStatus = null)
		if (branch==null || !branchStatus.equals(BranchStatus.NONE)) {
			String origNamespaceDotName = getNameSpaceDotName();
			boolean nameChanged = false;
			boolean namespaceCanged = false;
			boolean runtimeFieldChnaged = false;
			String updatedNamespace = updatedAirlockdItemData.getString(Constants.JSON_FEATURE_FIELD_NAMESPACE);
		
			
			String updatedName = updatedAirlockdItemData.getString(Constants.JSON_FIELD_NAME);
			if (!updatedName.equals(name)) {
				updateDetails.append("'name' changed from " + name + " to " + updatedName + "\n");
				name = updatedName;
				wasChanged = true;
				nameChanged = true;
				updateFeatureNameInBranches(uniqueId.toString(), name, updatedBranchesMap, context, env);
			}		
	
			if (!namespace.equals(updatedNamespace)) {
				updateDetails.append("'namespace' changed from " + namespace + " to " + updatedNamespace + "\n");
				namespace = updatedNamespace;
				wasChanged = true;	
				namespaceCanged=true;
				updateFeatureNamespaceInBranches(uniqueId.toString(), namespace, updatedBranchesMap, context, env);
			}					
			
			if (nameChanged || namespaceCanged) {
				updateBranchFeatureNamePointing(origNamespaceDotName, getNameSpaceDotName(), env, context, updatedBranchesMap);
				runtimeFieldChnaged = true;
			}
			
			Stage updatedStage = Utilities.strToStage(updatedAirlockdItemData.getString(Constants.JSON_FEATURE_FIELD_STAGE));
			if (updatedStage != stage) {
				updateDetails.append("'stage' changed from " + stage + " to " + updatedStage + "\n");
				stage = updatedStage;
				wasChanged = true;	
				runtimeFieldChnaged = true;
			}
							
			String updatedMinAppVersion = updatedAirlockdItemData.getString(Constants.JSON_FEATURE_FIELD_MIN_APP_VER);
			if (minAppVersion==null || !minAppVersion.equals(updatedMinAppVersion)) {
				updateDetails.append("'minAppVersion' changed from " + minAppVersion + " to " + updatedMinAppVersion + "\n");
				minAppVersion  = updatedMinAppVersion;
				wasChanged = true;
				runtimeFieldChnaged = true;
			}
											
					
	
			Boolean updatedEnabled = updatedAirlockdItemData.getBoolean(Constants.JSON_FEATURE_FIELD_ENABLED);
			if (enabled != updatedEnabled) {
				updateDetails.append("'enabled' changed from " + enabled + " to " + updatedEnabled + "\n");
				enabled  = updatedEnabled;
				wasChanged = true;
				runtimeFieldChnaged = true;
			}	
					
			double updatedRolloutPercentage = updatedAirlockdItemData.getDouble(Constants.JSON_FEATURE_FIELD_PERCENTAGE);
			if (rolloutPercentage  != updatedRolloutPercentage) {
				updateDetails.append("'rolloutPercentage' changed from " + rolloutPercentage + " to " + updatedRolloutPercentage + "\n");
				rolloutPercentage = updatedRolloutPercentage;
	
				if (isLegacyPercentage(env)) //only pre 2.5 seasons update the bitmaps - else leave as is
					updateRolloutPercentageBitmap(airlockItemsDB);				
				wasChanged = true;
				runtimeFieldChnaged = true;
			}
	
			JSONObject updatedRuleJSON = updatedAirlockdItemData.getJSONObject(Constants.JSON_FEATURE_FIELD_RULE);
			Rule updatedRule = new Rule();
			updatedRule.fromJSON(updatedRuleJSON);
			if (!rule.equals(updatedRule)) {
				updateDetails.append("'rule' changed from \n" + rule.toJson(OutputJSONMode.ADMIN).toString() + "\nto\n" + updatedRule.toJson(OutputJSONMode.ADMIN).toString() + "\n");
				rule = updatedRule;
				wasChanged = true;	
				runtimeFieldChnaged = true;
			}
	
			//optional fields
			if (updatedAirlockdItemData.containsKey(Constants.JSON_FIELD_DESCRIPTION) &&  updatedAirlockdItemData.get(Constants.JSON_FIELD_DESCRIPTION)!=null) {
				//if missing from json or null - ignore
				String updatedDescription = updatedAirlockdItemData.getString(Constants.JSON_FIELD_DESCRIPTION);
				if (description == null || !description.equals(updatedDescription)) {
					updateDetails.append("'description' changed from '" + description + "' to '" + updatedDescription + "'\n");
					description  = updatedDescription;
					wasChanged = true;
				}
			}	
			
			if (updatedAirlockdItemData.containsKey(Constants.JSON_FEATURE_FIELD_NO_CACHED_RES) &&  updatedAirlockdItemData.get(Constants.JSON_FEATURE_FIELD_NO_CACHED_RES)!=null) {
				//if missing from json or null - ignore
				Boolean updatedNoCachedResults = updatedAirlockdItemData.getBoolean(Constants.JSON_FEATURE_FIELD_NO_CACHED_RES);
				if (noCachedResults != updatedNoCachedResults) {
					updateDetails.append("'noChachedResults' changed from " + noCachedResults + " to " + updatedNoCachedResults + "\n");
					noCachedResults  = updatedNoCachedResults;
					wasChanged = true;
					runtimeFieldChnaged = true;
				}
			}	
	
			if (updatedAirlockdItemData.containsKey(Constants.JSON_FEATURE_FIELD_OWNER) &&  updatedAirlockdItemData.get(Constants.JSON_FEATURE_FIELD_OWNER)!=null) {
				//if missing from json or null - ignore
				String updatedOwner= updatedAirlockdItemData.getString(Constants.JSON_FEATURE_FIELD_OWNER);
				if (owner == null || !owner.equals(updatedOwner)) {
					updateDetails.append("'owner' changed from " + owner + " to " + updatedOwner + "\n");
					owner  = updatedOwner;
					wasChanged = true;
				}
			}
			
			if (updatedAirlockdItemData.containsKey(Constants.JSON_FIELD_INTERNAL_USER_GROUPS) && updatedAirlockdItemData.get(Constants.JSON_FIELD_INTERNAL_USER_GROUPS)!=null) {
				JSONArray updatedInternalUserGroups = updatedAirlockdItemData.getJSONArray(Constants.JSON_FIELD_INTERNAL_USER_GROUPS);
				if (internalUserGroups == null || !Utilities.stringArrayCompareIgnoreOrder(updatedInternalUserGroups,internalUserGroups)) {
					updateDetails.append("'internalUserGroups' changed from " + Arrays.toString(internalUserGroups) + " to " +  Arrays.toString(Utilities.jsonArrToStringArr(updatedInternalUserGroups)) + "\n");
					internalUserGroups = Utilities.jsonArrToStringArr(updatedInternalUserGroups);
					wasChanged = true;
					runtimeFieldChnaged = true;
				}
			}
			
			//in master if one of the runtime field was changed we should go over all the branches and if the item is checked out - update runtime since the delta was changed
			if (branch == null && runtimeFieldChnaged) {
				addBranchedToUpdatedBranchesMap(uniqueId.toString(), updatedBranchesMap, context, env);
			}
				
		}
	
		//sub features (at this stage i know none was removed.) Relevant for both FEATURE and MUTUAL_EXCLUSION_GROUP
		//for each sub feature - check if was added if so: remove from prv parent and set modif there. add to current list
		if (featuresItems!=null && env.getRequestType().equals(Constants.REQUEST_ITEM_TYPE.FEATURES)) {
			JSONArray updatedSubFeatures = updatedAirlockdItemData.getJSONArray(Constants.JSON_FEATURE_FIELD_FEATURES);
		
			featuresItems = updateAirlockItemPerList (updatedSubFeatures, airlockItemsDB.get(uniqueId.toString()).getFeaturesItems(), featuresItems, airlockItemsDB, root, changeDetailsList, env, branch, Type.FEATURE,isProdChange, context, updatedBranchesMap);
		}
		
		if (configurationRuleItems!=null) {
			JSONArray updatedSubConfiges = updatedAirlockdItemData.getJSONArray(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES);
		
			configurationRuleItems = updateAirlockItemPerList (updatedSubConfiges, airlockItemsDB.get(uniqueId.toString()).getConfigurationRuleItems(), configurationRuleItems, airlockItemsDB, root, changeDetailsList, env, branch, Type.CONFIGURATION_RULE, isProdChange, context, updatedBranchesMap);
		}
		
		if (orderingRuleItems!=null && updatedAirlockdItemData.containsKey(Constants.JSON_FEATURE_FIELD_ORDERING_RULES) && updatedAirlockdItemData.get(Constants.JSON_FEATURE_FIELD_ORDERING_RULES) != null) {
			JSONArray updatedSubOrderingRules = updatedAirlockdItemData.getJSONArray(Constants.JSON_FEATURE_FIELD_ORDERING_RULES);
		
			orderingRuleItems = updateAirlockItemPerList (updatedSubOrderingRules, airlockItemsDB.get(uniqueId.toString()).getOrderingRuleItems(), orderingRuleItems, airlockItemsDB, root, changeDetailsList, env, branch, Type.ORDERING_RULE, isProdChange, context, updatedBranchesMap);
		}
		
		if (entitlementItems!=null && updatedAirlockdItemData.containsKey(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS) && updatedAirlockdItemData.get(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS) != null) {
			JSONArray updatedSubInAppPurchases = updatedAirlockdItemData.getJSONArray(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS);
		
			entitlementItems = updateAirlockItemPerList (updatedSubInAppPurchases, airlockItemsDB.get(uniqueId.toString()).getEntitlementItems(), entitlementItems, airlockItemsDB, root, changeDetailsList, env, branch, Type.ENTITLEMENT, isProdChange, context, updatedBranchesMap);
		}
		
		if (purchaseOptionsItems!=null && updatedAirlockdItemData.containsKey(Constants.JSON_FEATURE_FIELD_PURCHASE_OPTIONS) && updatedAirlockdItemData.get(Constants.JSON_FEATURE_FIELD_PURCHASE_OPTIONS) != null) {
			JSONArray updatedSubPurchaseOptions = updatedAirlockdItemData.getJSONArray(Constants.JSON_FEATURE_FIELD_PURCHASE_OPTIONS);
		
			purchaseOptionsItems = updateAirlockItemPerList (updatedSubPurchaseOptions, airlockItemsDB.get(uniqueId.toString()).getOrderingRuleItems(), purchaseOptionsItems, airlockItemsDB, root, changeDetailsList, env, branch, Type.PURCHASE_OPTIONS, isProdChange, context, updatedBranchesMap);
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
	
	private void updateFeatureNamespaceInBranches(String featureId, String newNamespace,
			Map<String, Stage> updatedBranchesMap, ServletContext context, Environment env) {
		if (env.isInMaster()) {
			//go over all branches and update feature pointing by name
			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			Season season = seasonsDB.get(seasonId.toString());
			for (Branch branch:season.getBranches().getBranchesList()) {
				if (branch.getBranchAirlockItemsBD().containsKey(featureId)) {
					BaseAirlockItem featureInBranch = branch.getBranchAirlockItemsBD().get(featureId);
					if (featureInBranch instanceof DataAirlockItem) { //should always be true
						((DataAirlockItem)featureInBranch).setNamespace(newNamespace);
					
						Stage itemStage = ((DataAirlockItem)featureInBranch).getStage();
					
						if (updatedBranchesMap.containsKey(branch.getUniqueId().toString())) {
							if (updatedBranchesMap.get(branch.getUniqueId().toString()).equals(Stage.DEVELOPMENT)) {
								updatedBranchesMap.put(branch.getUniqueId().toString(), itemStage);
							}
						}
						else {
							updatedBranchesMap.put(branch.getUniqueId().toString(), itemStage);
						}
					}
				}
			}
		}
		else {
			@SuppressWarnings("unchecked")
			Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);
			
			Branch branch = branchesDB.get(env.getBranchId());
	
			if (branch.getBranchAirlockItemsBD().containsKey(featureId)) {
				BaseAirlockItem featureInBranch = branch.getBranchAirlockItemsBD().get(featureId);
				if (featureInBranch instanceof DataAirlockItem) { //should always be true
					((DataAirlockItem)featureInBranch).setNamespace(newNamespace);
				
					Stage itemStage = ((DataAirlockItem)featureInBranch).getStage();
				
					if (updatedBranchesMap.containsKey(branch.getUniqueId().toString())) {
						if (updatedBranchesMap.get(branch.getUniqueId().toString()).equals(Stage.DEVELOPMENT)) {
							updatedBranchesMap.put(branch.getUniqueId().toString(), itemStage);
						}
					}
					else {
						updatedBranchesMap.put(branch.getUniqueId().toString(), itemStage);
					}
				}
			}			
		}				
	}

	private void updateFeatureNameInBranches(String featureId, String newName, Map<String, Stage> updatedBranchesMap, ServletContext context, Environment env) {
		if (env.isInMaster()) {
			//go over all branches and update feature pointing by name
			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			Season season = seasonsDB.get(seasonId.toString());
			for (Branch branch:season.getBranches().getBranchesList()) {
				if (branch.getBranchAirlockItemsBD().containsKey(featureId)) {
					BaseAirlockItem featureInBranch = branch.getBranchAirlockItemsBD().get(featureId);
					if (featureInBranch instanceof DataAirlockItem) { //should always be true
						((DataAirlockItem)featureInBranch).setName(newName);
					
						Stage itemStage = ((DataAirlockItem)featureInBranch).getStage();
					
						if (updatedBranchesMap.containsKey(branch.getUniqueId().toString())) {
							if (updatedBranchesMap.get(branch.getUniqueId().toString()).equals(Stage.DEVELOPMENT)) {
								updatedBranchesMap.put(branch.getUniqueId().toString(), itemStage);
							}
						}
						else {
							updatedBranchesMap.put(branch.getUniqueId().toString(), itemStage);
						}
					}
				}
			}
		}
		else {
			@SuppressWarnings("unchecked")
			Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);
			
			Branch branch = branchesDB.get(env.getBranchId());
	
			if (branch.getBranchAirlockItemsBD().containsKey(featureId)) {
				BaseAirlockItem featureInBranch = branch.getBranchAirlockItemsBD().get(featureId);
				if (featureInBranch instanceof DataAirlockItem) { //should always be true
					((DataAirlockItem)featureInBranch).setName(newName);
				
					Stage itemStage = ((DataAirlockItem)featureInBranch).getStage();
				
					if (updatedBranchesMap.containsKey(branch.getUniqueId().toString())) {
						if (updatedBranchesMap.get(branch.getUniqueId().toString()).equals(Stage.DEVELOPMENT)) {
							updatedBranchesMap.put(branch.getUniqueId().toString(), itemStage);
						}
					}
					else {
						updatedBranchesMap.put(branch.getUniqueId().toString(), itemStage);
					}
				}
			}			
		}
		
	}
	
	private void updateBranchFeatureNamePointing(String origNamespaceDotName, String newNameSpaceDotName, Environment env, ServletContext context, Map<String, Stage> updatedBranchesMap) {
		Stage maxStage = null;
		if (env.isInMaster()) {
			//go over all branches and update feature pointing by name
			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			Season season = seasonsDB.get(seasonId.toString());
			for (Branch branch:season.getBranches().getBranchesList()) {
				maxStage = branch.updateFeatureNamePointing(origNamespaceDotName, newNameSpaceDotName);
				if (maxStage!=null) {
					if (updatedBranchesMap.containsKey(branch.getUniqueId().toString())) {
						if (updatedBranchesMap.get(branch.getUniqueId().toString()).equals(Stage.DEVELOPMENT)) {
							updatedBranchesMap.put(branch.getUniqueId().toString(), maxStage);
						}
					}
					else {
						updatedBranchesMap.put(branch.getUniqueId().toString(), maxStage);
					}
				}
			}
		}
		else {
			@SuppressWarnings("unchecked")
			Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);
			
			Branch branch = branchesDB.get(env.getBranchId());
	
			maxStage = branch.updateFeatureNamePointing(origNamespaceDotName, newNameSpaceDotName);
			if (maxStage!=null) {
				if (updatedBranchesMap.containsKey(branch.getUniqueId().toString())) {
					if (updatedBranchesMap.get(branch.getUniqueId().toString()).equals(Stage.DEVELOPMENT)) {
						updatedBranchesMap.put(branch.getUniqueId().toString(), maxStage);
					}
				}
				else {
					updatedBranchesMap.put(branch.getUniqueId().toString(), maxStage);
				}
			}
		}
		
	}
	
	//done when feature percentage changed or when parent changed (feature moved from parent to parent)
	private void updateRolloutPercentageBitmap(Map<String, BaseAirlockItem> airlockItemsDB) throws JSONException {
		try {
			//use the original feature bit map and update it according to the new percentage
			Percentile percentile = new Percentile(rolloutPercentageBitmap); 
			BaseAirlockItem parentFeature = airlockItemsDB.get(parent.toString());
			String parentBitmap = getParentPercentageBitmap(parentFeature, airlockItemsDB);
	
			Integer intPercenatage = rolloutPercentage.intValue(); //only pre 2.5 seasons have bitmaps and the percentage is int
			
			if (parentBitmap == null)
				percentile.changePercentage(intPercenatage);
			else
				//take into account parent bitmap
				percentile.changePercentage(new Percentile(parentBitmap), intPercenatage);
	
			//System.out.println("percentile = " + Utilities.intArrayToString(percentile.getContent(), null));
			rolloutPercentageBitmap = percentile.toString();
			if (featuresItems!=null)
				updateSubFeaturesPercentageBitmap (rolloutPercentageBitmap, featuresItems);		
			
			if (configurationRuleItems!=null)
				updateSubFeaturesPercentageBitmap (rolloutPercentageBitmap, configurationRuleItems);
		} catch (PercentileException pe) {
			throw new JSONException("PercentileException: " + pe.getMessage());
		}
	}


	//update subFeatuers bitmap upon parent percentage change
	private void updateSubFeaturesPercentageBitmap(String parentRolloutPercentageBitmap, LinkedList<BaseAirlockItem> subAirlockItems) throws JSONException
	{
		try {
			if (subAirlockItems == null) 
				return;
	
			for (BaseAirlockItem item:subAirlockItems)
			{
				String parentBitmap = parentRolloutPercentageBitmap;
				if (item.type == Type.FEATURE || item.type == Type.CONFIGURATION_RULE)
				{
					// features and configurations calculate new bitmap
					DataAirlockItem dalItem = (DataAirlockItem)item;
					Percentile percentile = new Percentile(dalItem.getRolloutPercentageBitmap());
					//System.out.println("son before percentile = " + Utilities.intArrayToString(percentile.getContent(), null));
					//can convert percentage to int since in pre 2.5 seasons the percentage is int
					percentile.changePercentage(new Percentile(parentRolloutPercentageBitmap), dalItem.getRolloutPercentage().intValue());
					//System.out.println("son after percentile = " + Utilities.intArrayToString(percentile.getContent(), null));
					dalItem.setRolloutPercentageBitmap(percentile.toString());
					parentBitmap = dalItem.getRolloutPercentageBitmap();
				}
	
				// arrays pass the parent's bitmap down
				updateSubFeaturesPercentageBitmap(parentBitmap, item.getConfigurationRuleItems());
				updateSubFeaturesPercentageBitmap(parentBitmap, item.getFeaturesItems());
			}
		} catch (PercentileException pe) {
			throw new JSONException("PercentileException: " + pe.getMessage());
		}
	}

	//return null if valid, ValidationResults otherwise
	//does not change the feature! 
	//for features cannot call super update since some steps are unique to feature even with general fields
	public ValidationResults doValidateFeatureJSON(JSONObject featureObj, ServletContext context, String seasonId, LinkedList<String> addedSubFeatures, LinkedList<String> missingSubFeatures, InternalUserGroups userGroups, 
			HashMap<UUID, Integer> existingFeaturesInUpdate, String parent, HashMap<String, JSONObject> updatedFeaturesMap, UserInfo userInfo,
			ValidationCache tester,
			Map<String, BaseAirlockItem> airlockItemsDB, Environment env,List<OriginalString> copiedStings) {
		@SuppressWarnings("unchecked")
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

		Action action = Action.ADD;

		//description, owner, internalUserGroups, minAppVersion and additionalInfo, configurationSchema, defaultConfiguration are optional 
		try {
			//type
			if (!featureObj.containsKey(Constants.JSON_FEATURE_FIELD_TYPE) || featureObj.getString(Constants.JSON_FEATURE_FIELD_TYPE) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_TYPE), Status.BAD_REQUEST);
			}

			String typeStr = featureObj.getString(Constants.JSON_FEATURE_FIELD_TYPE);
			Type typeObj = strToType(typeStr);
			if (typeObj == null) {
				return new ValidationResults("Illegal type: '" + typeStr + "'", Status.BAD_REQUEST);
			}

			if (featureObj.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && featureObj.get(Constants.JSON_FIELD_UNIQUE_ID)!=null) {
				//if JSON contains uniqueId - update an existing feature otherwise create a new feature
				action = Action.UPDATE;
			}
						
			Stage stageObj = null;

			//name
			if (!featureObj.containsKey(Constants.JSON_FIELD_NAME) || featureObj.getString(Constants.JSON_FIELD_NAME) == null || featureObj.getString(Constants.JSON_FIELD_NAME).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_NAME), Status.BAD_REQUEST);
			}

			String validateNameErr = Utilities.validateName(featureObj.getString(Constants.JSON_FIELD_NAME));
			if(validateNameErr!=null) {
				return new ValidationResults(validateNameErr, Status.BAD_REQUEST);
			}

			//namespace
			if (!featureObj.containsKey(Constants.JSON_FEATURE_FIELD_NAMESPACE) || featureObj.getString(Constants.JSON_FEATURE_FIELD_NAMESPACE) == null || featureObj.getString(Constants.JSON_FEATURE_FIELD_NAMESPACE).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_NAMESPACE), Status.BAD_REQUEST);
			}

			String validateNamespaceErr = validateNamespace(featureObj.getString(Constants.JSON_FEATURE_FIELD_NAMESPACE));
			if(validateNamespaceErr!=null) {
				return new ValidationResults(validateNamespaceErr, Status.BAD_REQUEST);
			}
		
			Season curSeason = seasonsDB.get(seasonId);
			if (curSeason == null) {
				return new ValidationResults("The season of the given " + getObjTypeStrByType() + " does not exist.", Status.BAD_REQUEST);
			}

			//verify name+namespace uniqueness within the season (in master). 
			//The name uniqueness is between features and entitlements items as well
			String newName = featureObj.getString(Constants.JSON_FIELD_NAME);
			String newNamespace = featureObj.getString(Constants.JSON_FEATURE_FIELD_NAMESPACE);
		
			BaseAirlockItem root = null;
			root = airlockItemsDB.get(seasonsDB.get(seasonId).getRoot().getUniqueId().toString());
			
			ValidationResults res = verifyNamespaceNameUniqueness(newNamespace, newName, root, uniqueId, typeObj);
			if (res !=null) {
				return res;
			}
			else {
				root = airlockItemsDB.get(seasonsDB.get(seasonId).getEntitlementsRoot().getUniqueId().toString());
				res = verifyNamespaceNameUniqueness(newNamespace, newName, root, uniqueId, typeObj);
				if (res !=null) {
					return res;
				}
			}

			//when name/namespace changed
			if (name == null || !name.equals(newName) || !namespace.equals(newNamespace)) {
				if (env.isInMaster()) { //in master 
					//verify name+namespace uniqueness within the season's branches
					res = verifyNamespaceNameUniquenessInBranches(newNamespace, newName, curSeason);
					if (res !=null) {
						return res;
					}
				}
				else {
					//in branch - validate that name does not collide with a config rule that was added to master after the checkout
					//            The name uniqueness is between features and entitlements items as well
					@SuppressWarnings("unchecked")
					Map<String, BaseAirlockItem> masterItemsDB = (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);
					BaseAirlockItem rootInMaster = masterItemsDB.get(seasonsDB.get(seasonId).getRoot().getUniqueId().toString());
					res = verifyNamespaceNameUniqueness(newNamespace, newName, rootInMaster, uniqueId, typeObj);
					if (res !=null) {
						return res;
					}	
					else {
						rootInMaster = masterItemsDB.get(seasonsDB.get(seasonId).getEntitlementsRoot().getUniqueId().toString());
						res = verifyNamespaceNameUniqueness(newNamespace, newName, rootInMaster, uniqueId, typeObj);
						if (res !=null) {
							return res;
						}	
					}
				}
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
			
			//creator
			if (!featureObj.containsKey(Constants.JSON_FEATURE_FIELD_CREATOR) || featureObj.getString(Constants.JSON_FEATURE_FIELD_CREATOR) == null || featureObj.getString(Constants.JSON_FEATURE_FIELD_CREATOR).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_CREATOR), Status.BAD_REQUEST);
			}			

			//stage
			if (!featureObj.containsKey(Constants.JSON_FEATURE_FIELD_STAGE) || featureObj.getString(Constants.JSON_FEATURE_FIELD_STAGE) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_STAGE), Status.BAD_REQUEST);					
			}

			String stageStr = featureObj.getString(Constants.JSON_FEATURE_FIELD_STAGE);
			stageObj = Utilities.strToStage(stageStr);
			if (stageObj == null) {
				return new ValidationResults("Illegal stage: '" + stageStr + "'", Status.BAD_REQUEST);
			}				

			//minAppVersion
			if (!featureObj.containsKey(Constants.JSON_FEATURE_FIELD_MIN_APP_VER) || featureObj.get(Constants.JSON_FEATURE_FIELD_MIN_APP_VER) == null || featureObj.getString(Constants.JSON_FEATURE_FIELD_MIN_APP_VER).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_MIN_APP_VER), Status.BAD_REQUEST);					
			}			
			
			String minAppVer = featureObj.getString(Constants.JSON_FEATURE_FIELD_MIN_APP_VER);

			//rule
			if (!featureObj.containsKey(Constants.JSON_FEATURE_FIELD_RULE) || featureObj.getString(Constants.JSON_FEATURE_FIELD_RULE) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_RULE), Status.BAD_REQUEST);
			}

			featureObj.getJSONObject(Constants.JSON_FEATURE_FIELD_RULE); //validate legal json
			Rule tmpRule = new Rule();
			tmpRule.fromJSON(featureObj.getJSONObject(Constants.JSON_FEATURE_FIELD_RULE));
			//empty,null or missing ruleString is legal
			
			
			res = tmpRule.validateRule(stageObj, minAppVer, curSeason, context, tester, userInfo);
			if (res !=null) {
				return res;
			} 			
			
			//season id
			if (!featureObj.containsKey(Constants.JSON_FEATURE_FIELD_SEASON_ID) || featureObj.get(Constants.JSON_FEATURE_FIELD_SEASON_ID)==null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_SEASON_ID), Status.BAD_REQUEST);
			}

			//enabled
			if (!featureObj.containsKey(Constants.JSON_FEATURE_FIELD_ENABLED) || featureObj.get(Constants.JSON_FEATURE_FIELD_ENABLED) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_ENABLED), Status.BAD_REQUEST);					
			}

			featureObj.getBoolean(Constants.JSON_FEATURE_FIELD_ENABLED); //validate that is boolean value		
			
			//internalUserGroups - optional
			if (featureObj.containsKey(Constants.JSON_FIELD_INTERNAL_USER_GROUPS) && featureObj.get(Constants.JSON_FIELD_INTERNAL_USER_GROUPS) != null) {				
				JSONArray groupsArr = featureObj.getJSONArray(Constants.JSON_FIELD_INTERNAL_USER_GROUPS); //validate that is String array value

				//validate that specified groups actually exist
				for (int k=0; k<groupsArr.length(); k++) {
					if (!userGroups.getGroupsMap().containsKey(groupsArr.get(k))) {
						return new ValidationResults("The internalUserGroups value '" + groupsArr.get(k) + "' does not exist.", Status.BAD_REQUEST);
					}
				}
			}

			//noChachedResults
			if (featureObj.containsKey(Constants.JSON_FEATURE_FIELD_NO_CACHED_RES) && featureObj.get(Constants.JSON_FEATURE_FIELD_NO_CACHED_RES) != null) {
				featureObj.getBoolean(Constants.JSON_FEATURE_FIELD_NO_CACHED_RES); //validate that is boolean value					
			}
			
			//rolloutPercentage
			if (!featureObj.containsKey(Constants.JSON_FEATURE_FIELD_PERCENTAGE) && featureObj.get(Constants.JSON_FEATURE_FIELD_PERCENTAGE) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_PERCENTAGE), Status.BAD_REQUEST);					
			}
			double p = featureObj.getDouble(Constants.JSON_FEATURE_FIELD_PERCENTAGE); //validate that is Double value
			if (isLegacyPercentage(env)) {
				//in pre 2.5 seasons the percentage is int 0-100				
				if ((p % 1) != 0 || p<0 || p>100) {
					return new ValidationResults("rolloutPercentage should be an integer between 0-100.", Status.BAD_REQUEST);
				}				
			}
			else {
				ValidationResults vr = Utilities.validatePercentage(p);
				if (vr!=null)
					return vr;
			}

			if (action == Action.ADD) {		
				//cannot create root feature
				if (typeObj == Type.ROOT) {
					return new ValidationResults("Cannot create ROOT feature.", Status.BAD_REQUEST);
				}
				
				//feature in production can be added only by Administrator or ProductLead
				if (stageObj.equals(Stage.PRODUCTION) && !validRole(userInfo)) {
					return new ValidationResults("Unable to add the " + getObjTypeStrByType() + ". Only a user with the Administrator or Product Lead role can add an item in the production stage.", Status.UNAUTHORIZED);					
				}

				//modification date => should not appear in feature creation
				if (featureObj.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) && featureObj.get(Constants.JSON_FIELD_LAST_MODIFIED)!=null) {
					return new ValidationResults("The lastModified field cannot be specified during " + getObjTypeStrByType() + " creation.", Status.BAD_REQUEST);
				}

				//sub features are not allowed in add feature (adding only one by one)
				if (featureObj.containsKey(Constants.JSON_FEATURE_FIELD_FEATURES) && featureObj.get(Constants.JSON_FEATURE_FIELD_FEATURES) != null && !featureObj.getJSONArray(Constants.JSON_FEATURE_FIELD_FEATURES).isEmpty()) {
					return new ValidationResults(Strings.featureWithSubfeatures, Status.BAD_REQUEST);
				}				
				
				//sub features are not allowed in add feature (adding only one by one)
				if (featureObj.containsKey(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES) && featureObj.get(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES) != null && !featureObj.getJSONArray(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES).isEmpty()) {
					return new ValidationResults("Cannot add a " + getObjTypeStrByType() + " with configurations. Add the " + getObjTypeStrByType() + " and its sub-configurations one by one.", Status.BAD_REQUEST);
				}	

				//creation date => should not appear in feature creation
				if (featureObj.containsKey(Constants.JSON_FEATURE_FIELD_CREATION_DATE) && featureObj.get(Constants.JSON_FEATURE_FIELD_CREATION_DATE)!=null) {
					return new ValidationResults("The creationDate field cannot be specified during " + getObjTypeStrByType() + " creation.", Status.BAD_REQUEST);
				}

				//rolloutPercentageBitmap field should not be in create feature
				if (featureObj.containsKey(Constants.JSON_FEATURE_FIELD_PERCENTAGE_BITMAP) && featureObj.get(Constants.JSON_FEATURE_FIELD_PERCENTAGE_BITMAP) != null) {
					return new ValidationResults("The rolloutPercentageBitmap field cannot be specified during " + getObjTypeStrByType() + " creation.", Status.BAD_REQUEST);
				}		
				
				//verify that not higher than seasons max version
				//this is checked only in create - in update this is not checked since the seasons min-maxVersion may have been changed. 
				String minAppVersion = featureObj.getString(Constants.JSON_FEATURE_FIELD_MIN_APP_VER);
				String seasonMaxVer = seasonsDB.get(seasonId).getMaxVersion();
				if (seasonMaxVer!=null) {
					if (Season.compare (minAppVersion, seasonMaxVer) >=0) {
						return new ValidationResults("The " + getObjTypeStrByType() + "'s Minimum App Version must be less than the Maximum Version of the current version range.", Status.BAD_REQUEST);
					}
				}
				
				//branchStatus => should not appear in feature creation
				if (featureObj.containsKey(Constants.JSON_FIELD_BRANCH_STATUS) && featureObj.get(Constants.JSON_FIELD_BRANCH_STATUS)!=null) {
					return new ValidationResults("The branchStatus field cannot be specified during feature creation.", Status.BAD_REQUEST);
				}
			
				//branchFeaturesItems => should not appear in feature creation
				if (featureObj.containsKey(Constants.JSON_FIELD_BRANCH_FEATURES_ITEMS) && featureObj.get(Constants.JSON_FIELD_BRANCH_FEATURES_ITEMS)!=null) {
					return new ValidationResults("The branchFeaturesItems field cannot be specified during feature creation.", Status.BAD_REQUEST);
				}
				
				//branchConfigurationRuleItems => should not appear in feature creation
				if (featureObj.containsKey(Constants.JSON_FIELD_BRANCH_CONFIGURATION_RULE_ITEMS) && featureObj.get(Constants.JSON_FIELD_BRANCH_CONFIGURATION_RULE_ITEMS)!=null) {
					return new ValidationResults("The branchConfigurationRuleItems field cannot be specified during feature creation.", Status.BAD_REQUEST);
				}
			}
			else { //update
				String featureId = featureObj.getString(Constants.JSON_FIELD_UNIQUE_ID);

				//modification date must appear
				if (!featureObj.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) || featureObj.get(Constants.JSON_FIELD_LAST_MODIFIED)==null) {
					return new ValidationResults("The lastModified field is missing. This field must be specified during " + getObjTypeStrByType() + " update.", Status.BAD_REQUEST);
				}				

				//verify that given modification date is not older that current modification date
				long givenModoficationDate = featureObj.getLong(Constants.JSON_FIELD_LAST_MODIFIED);  //verify that legal long
				Date givenDate = new Date(givenModoficationDate);
				if (givenDate.before(lastModified)) {
					return new ValidationResults("The " + getObjTypeStrByType() + " was changed by another user. Refresh your browser and try again.", Status.CONFLICT);			
				}

				//branchStatus should not be changed in feature creation
				if (featureObj.containsKey(Constants.JSON_FIELD_BRANCH_STATUS) && featureObj.get(Constants.JSON_FIELD_BRANCH_STATUS)!=null) {
					String branchStatusStr = featureObj.getString(Constants.JSON_FIELD_BRANCH_STATUS);
					BranchStatus branchStatusObj = strToBranchStatus(branchStatusStr); //already validate 
					if (!branchStatus.equals(branchStatusObj))
						return new ValidationResults("The branchStatus field cannot be updated during feature update.", Status.BAD_REQUEST);
				}

				//branchFeaturesItems should not be changed in feature update
				if (featureObj.containsKey(Constants.JSON_FIELD_BRANCH_FEATURES_ITEMS) && featureObj.get(Constants.JSON_FIELD_BRANCH_FEATURES_ITEMS)!=null) {
					JSONArray branchFeaturesItemsArr = featureObj.getJSONArray(Constants.JSON_FIELD_BRANCH_FEATURES_ITEMS);
					if (!Utilities.compareStringsArrayToStringsList(branchFeaturesItemsArr, branchFeaturesItems))
						return new ValidationResults("The branchFeaturesItems field cannot be updated during feature update.", Status.BAD_REQUEST);
				}
				
				//branchConfigurationRuleItems should not be changed in feature update
				if (featureObj.containsKey(Constants.JSON_FIELD_BRANCH_CONFIGURATION_RULE_ITEMS) && featureObj.get(Constants.JSON_FIELD_BRANCH_CONFIGURATION_RULE_ITEMS)!=null) {
					JSONArray branchConfigurationRuleItemsArr = featureObj.getJSONArray(Constants.JSON_FIELD_BRANCH_CONFIGURATION_RULE_ITEMS);
					if (!Utilities.compareStringsArrayToStringsList(branchConfigurationRuleItemsArr, branchConfigurationRuleItems))
						return new ValidationResults("The branchConfigurationRuleItems field cannot be updated during feature update.", Status.BAD_REQUEST);
				}
				
				if (existingFeaturesInUpdate.containsKey(uniqueId)) {
					return new ValidationResults("A " + getObjTypeStrByType() + " with id " + uniqueId.toString() + " appears more than once in the input data.", Status.BAD_REQUEST);
				}

				existingFeaturesInUpdate.put (uniqueId, 1);
				
				//in branch dont allow name/namespace change of a checked out feature
				if (!env.isInMaster() && (!name.equals(newName) || !namespace.equals(newNamespace))) {
					if (branchStatus.equals(BranchStatus.CHECKED_OUT)) {
						return new ValidationResults("You cannot change the name or namespace of an item that is checked out from the master.", Status.BAD_REQUEST);
					}
				}

				//map of the features in the update - this way we will be able to take the updated feature for stage validation and not the original
				//this is good for the case you update feature and its subFeature from dev to prod.
				updatedFeaturesMap.put(uniqueId.toString(), featureObj);

				if (parent!=null && !parent.equals(this.parent.toString()) && (type.equals(Type.FEATURE) || type.equals(Type.MUTUAL_EXCLUSION_GROUP))) {						
					//if parent was changed during update and it previous parent has an ordering rule that refers to the moved item - return error
					res = validatePrevParentOrderingRule(airlockItemsDB, tester, curSeason, context);
					if (res!=null)
						return res;
				}
				
				if (featuresItems !=null && env.getRequestType().equals(Constants.REQUEST_ITEM_TYPE.FEATURES)) {
				
					//sub features are mandatory - if none exist expect empty list
					if (!featureObj.containsKey(Constants.JSON_FEATURE_FIELD_FEATURES) || featureObj.get(Constants.JSON_FEATURE_FIELD_FEATURES) == null) {
						return new ValidationResults("The features field is missing. This field must be specified during feature update.", Status.BAD_REQUEST);
					}

					JSONArray updatedSubFeatures = featureObj.getJSONArray(Constants.JSON_FEATURE_FIELD_FEATURES); //validate that is array value

					res = doValidateSubItems(featureObj, updatedSubFeatures, featuresItems, Constants.JSON_FEATURE_FIELD_FEATURES, context, seasonId, addedSubFeatures, missingSubFeatures, userGroups, existingFeaturesInUpdate, parent, updatedFeaturesMap, userInfo, tester, airlockItemsDB, env,copiedStings);
					if (res!=null)
						return res;
				}

				if (configurationRuleItems !=null) {
					
					//sub features are mandatory - if none exist expect empty list
					if (!featureObj.containsKey(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES) || featureObj.get(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES) == null) {
						return new ValidationResults("The configurationRules field is missing. This field must be specified during update.", Status.BAD_REQUEST);
					}

					JSONArray updatedSubConfigRules= featureObj.getJSONArray(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES); //validate that is array value

					res = doValidateSubItems(featureObj, updatedSubConfigRules, configurationRuleItems, Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES, context, seasonId, addedSubFeatures, missingSubFeatures, userGroups, existingFeaturesInUpdate, parent, updatedFeaturesMap, userInfo, tester, airlockItemsDB, env,copiedStings);
					if (res!=null)
						return res;
				}
				
				if (orderingRuleItems !=null) {					
					//sub ordering rules are optional - skip if does not exist
					if (featureObj.containsKey(Constants.JSON_FEATURE_FIELD_ORDERING_RULES) && featureObj.get(Constants.JSON_FEATURE_FIELD_ORDERING_RULES) != null) {
						JSONArray updatedSubOrderingRules= featureObj.getJSONArray(Constants.JSON_FEATURE_FIELD_ORDERING_RULES); //validate that is array value
	
						res = doValidateSubItems(featureObj, updatedSubOrderingRules, orderingRuleItems, Constants.JSON_FEATURE_FIELD_ORDERING_RULES, context, seasonId, addedSubFeatures, missingSubFeatures, userGroups, existingFeaturesInUpdate, parent, updatedFeaturesMap, userInfo, tester, airlockItemsDB, env,copiedStings);
						if (res!=null)
							return res;					
					}
				}
				
				if (entitlementItems !=null) {					
					//sub ordering rules are optional - skip if does not exist
					if (featureObj.containsKey(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS) && featureObj.get(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS) != null) {
						JSONArray updatedSubInAppPurchases= featureObj.getJSONArray(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS); //validate that is array value
	
						res = doValidateSubItems(featureObj, updatedSubInAppPurchases, entitlementItems, Constants.JSON_FEATURE_FIELD_ENTITLEMENTS, context, seasonId, addedSubFeatures, missingSubFeatures, userGroups, existingFeaturesInUpdate, parent, updatedFeaturesMap, userInfo, tester, airlockItemsDB, env,copiedStings);
						if (res!=null)
							return res;					
					}
				}
				
				if (purchaseOptionsItems !=null) {					
					//sub ordering rules are optional - skip if does not exist
					if (featureObj.containsKey(Constants.JSON_FEATURE_FIELD_PURCHASE_OPTIONS) && featureObj.get(Constants.JSON_FEATURE_FIELD_PURCHASE_OPTIONS) != null) {
						JSONArray updatedSubPurchaseOptionsItems = featureObj.getJSONArray(Constants.JSON_FEATURE_FIELD_PURCHASE_OPTIONS); //validate that is array value
	
						res = doValidateSubItems(featureObj, updatedSubPurchaseOptionsItems, purchaseOptionsItems, Constants.JSON_FEATURE_FIELD_PURCHASE_OPTIONS, context, seasonId, addedSubFeatures, missingSubFeatures, userGroups, existingFeaturesInUpdate, parent, updatedFeaturesMap, userInfo, tester, airlockItemsDB, env,copiedStings);
						if (res!=null)
							return res;					
					}
				}

				//type cannot be changed in update
				if (type != typeObj) {
					return new ValidationResults("The " + getObjTypeStrByType() + " type cannot be changed during update.", Status.BAD_REQUEST);
				}				

				//season id must exists and not be changed
				String seasonIdStr = featureObj.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID);
				if (!seasonIdStr.equals(seasonId)) {
					return new ValidationResults(String.format(Strings.fieldCannotBeChangedDuringUpdate, Constants.JSON_FEATURE_FIELD_SEASON_ID), Status.BAD_REQUEST);
				}

				//creator must exist and not be changed
				String creatorStr = featureObj.getString(Constants.JSON_FEATURE_FIELD_CREATOR);
				if (!creator.equals(creatorStr)) {
					return new ValidationResults(String.format(Strings.fieldCannotBeChangedDuringUpdate, Constants.JSON_FEATURE_FIELD_CREATOR), Status.BAD_REQUEST);
				}

				if (isLegacyPercentage(env)) { //only pre 2.5 seasons have bitmaps and the percentage is int
					//rolloutPercentageBitmap must exist and not be changed
					String percentageBitmapStr = featureObj.getString(Constants.JSON_FEATURE_FIELD_PERCENTAGE_BITMAP);
					if (percentageBitmapStr == null) {
						return new ValidationResults("The rolloutPercentageBitmap field is missing. This field must be specified during " + getObjTypeStrByType() + " update.", Status.BAD_REQUEST);
					}
	
					if (!rolloutPercentageBitmap.equals(percentageBitmapStr)) {
						return new ValidationResults(String.format(Strings.fieldCannotBeChangedDuringUpdate, Constants.JSON_FEATURE_FIELD_PERCENTAGE_BITMAP), Status.BAD_REQUEST);
					}
				}

				//creation date must appear
				if (!featureObj.containsKey(Constants.JSON_FEATURE_FIELD_CREATION_DATE) || featureObj.get(Constants.JSON_FEATURE_FIELD_CREATION_DATE)==null) {
					return new ValidationResults("The creationDate field is missing. This field must be specified during " + getObjTypeStrByType() + " update.", Status.BAD_REQUEST);
				}

				//verify that legal long
				long creationdateLong = featureObj.getLong(Constants.JSON_FEATURE_FIELD_CREATION_DATE);
				//verify that was not changed
				if (!creationDate.equals(new Date(creationdateLong))) {
					return new ValidationResults("creationDate cannot be changed during update", Status.BAD_REQUEST);
				}
 
				//rolloutPercentageBitmap must exist in update if already exists in feature
				if (rolloutPercentageBitmap != null && (!featureObj.containsKey(Constants.JSON_FEATURE_FIELD_PERCENTAGE_BITMAP) ||  featureObj.get(Constants.JSON_FEATURE_FIELD_PERCENTAGE_BITMAP) == null)) {
					return new ValidationResults("The rolloutPercentageBitmap field is missing. This field must be specified during " + getObjTypeStrByType() + " update.", Status.BAD_REQUEST);
				}
				
				//in PRODUCTION name and namespace must not change
				if(stageObj == Stage.PRODUCTION) {
					
					String nameStr = featureObj.getString(Constants.JSON_FIELD_NAME);
					if (!name.equals(nameStr)) {
						return new ValidationResults("The name of a " + getObjTypeStrByType() + " in the production stage cannot be changed.", Status.BAD_REQUEST);
					}
					
					String namespaceStr = featureObj.getString(Constants.JSON_FEATURE_FIELD_NAMESPACE);
					if (!namespace.equals(namespaceStr)) {
						return new ValidationResults("The namespace of a " + getObjTypeStrByType() + " in the production stage cannot be changed.", Status.BAD_REQUEST);
					}
				}
				
				//in update: if feature is reported in analytics and was cahnged from dev to prod, verify that prodItemsToAnalytics 
				//does not exceed quota
				if(stage == Stage.DEVELOPMENT && stageObj == Stage.PRODUCTION) {
					curSeason.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection();
					AnalyticsDataCollection analyticsData = curSeason.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection();
					if (analyticsData.isFeatureExistInAnalytics(featureId)) {
						boolean orderingRulesExist = orderingRuleItems!=null&&orderingRuleItems.size()>0;
						int updatedProdCount = analyticsData.simulateProdCounterUponFeatureMoveToProd(featureId, orderingRulesExist);
						if (updatedProdCount > curSeason.getAnalytics().getAnalyticsQuota()) {
							int currentNumOfProdAnaItems = analyticsData.getNumberOfProductionItemsToAnalytics();
							return new ValidationResults("Failed updating Feature: The maximum number of items in production to send to analytics was exceeded. The maximum number allowed is " + curSeason.getAnalytics().getAnalyticsQuota() + ". " + currentNumOfProdAnaItems + " were previously selected, and releasing the feature to production increased the number to " + updatedProdCount, Status.BAD_REQUEST);						
						}
					}
					
					List<Experiment> seasonsProdExperiments = curSeason.getExperimentsForSeason(context, true);
					if (seasonsProdExperiments!=null && !seasonsProdExperiments.isEmpty()) {
						String namespaceDotname = newNamespace+"."+newName;
						for (Experiment exp:seasonsProdExperiments) {
							int updatedProdCount = exp.simulateProdCounterUponFeatureMoveToProd(namespaceDotname, context);
							int experimentAnalyticsQuota = exp.getQuota(context);
							if (updatedProdCount > experimentAnalyticsQuota) {
								return new ValidationResults("Failed to update Feature. The maximum number of items in production to send to analytics for experiment " + exp.getName() + " was exceeded. The maximum number allowed is " + experimentAnalyticsQuota + ". Releasing the feature to production increased the number to " + updatedProdCount, Status.BAD_REQUEST);						
							}
						}					
					}
				}
				//in update 
			}
		} catch (JSONException jsne) {
			return new ValidationResults(jsne.getMessage(), Status.BAD_REQUEST);
		}
		catch (ClassCastException cce) {
			return new ValidationResults("Illegal feature JSON: " + cce.getMessage(), Status.BAD_REQUEST);
		}
		catch (MergeException e) {			
			return new ValidationResults(Strings.mergeException + e.getMessage(), Status.INTERNAL_SERVER_ERROR);
		}
		catch (ValidationException ve) {
			return new ValidationResults(ve.getMessage(), Status.BAD_REQUEST);
		}
		catch (GenerationException ge) {
			return new ValidationResults(ge.getMessage(), Status.BAD_REQUEST);
		}
		return null;
	}

	private ValidationResults verifyNamespaceNameUniquenessInBranches(String newNamespace, String newName, Season curSeason) {
		newName = newName.replace(".", " "); //both dot and space are mapped to underscore in java constant file so we cannot allow same name with only dot and spaces switch  				
		String newNameAndNamespace = newNamespace+"."+newName;

		for (Branch b:curSeason.getBranches().getBranchesList()) {
			Set<String> branchesFeatures = b.getBranchAirlockItemsBD().keySet();
			for (String branchFeatureId:branchesFeatures) {				
				BaseAirlockItem branchAI = b.getBranchAirlockItemsBD().get(branchFeatureId);
				if (branchAI.getBranchStatus().equals(BranchStatus.NEW) && branchAI instanceof DataAirlockItem) {
					String nsDotName = ((DataAirlockItem)(branchAI)).getNameSpaceDotName();
					if (newNameAndNamespace.equals(nsDotName)) {
						return new ValidationResults("A feature with the specified namespace and name already exists in the current version range in branch '" + b.getName() + "'. Periods and spaces are considered the same.", Status.BAD_REQUEST);
					}
				}
			}
		}
		return null;
	}

	private ValidationResults doValidateSubItems (JSONObject featureObj, JSONArray updatedSubFeatures, LinkedList<BaseAirlockItem> curSubFeatures, String updatedSubFeaturesType, ServletContext context, String seasonId, LinkedList<String> addedSubFeatures, LinkedList<String> missingSubFeatures, InternalUserGroups userGroups, 
			HashMap<UUID, Integer> existingFeaturesInUpdate, String parent, HashMap<String, JSONObject> updatedFeaturesMap, UserInfo userInfo,
			ValidationCache tester,
			Map<String, BaseAirlockItem> airlockItemsDB, Environment env,List<OriginalString> copiedStings) throws JSONException {

		for (int i=0; i<updatedSubFeatures.size(); i++) {
			JSONObject updatedSubFeature = updatedSubFeatures.getJSONObject(i);

			//verify that none of the given sub features is new (uniqueId is missing)
			if (!updatedSubFeature.containsKey(Constants.JSON_FIELD_UNIQUE_ID) || updatedSubFeature.get(Constants.JSON_FIELD_UNIQUE_ID)==null) {
				return new ValidationResults("Cannot add new features during feature update.", Status.BAD_REQUEST);
			}

			//verify that sub feature is in an allowed type according to parents type
			if (!updatedSubFeature.containsKey(Constants.JSON_FEATURE_FIELD_TYPE) || updatedSubFeature.get(Constants.JSON_FEATURE_FIELD_TYPE)==null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_TYPE), Status.BAD_REQUEST);					
			}
			String typeStr = updatedSubFeature.getString(Constants.JSON_FEATURE_FIELD_TYPE);
			Type updatedSubFeatureType = strToType(typeStr);
			if (updatedSubFeatureType!=null) {
				if (updatedSubFeaturesType.equals(Constants.JSON_FEATURE_FIELD_FEATURES) &&
						(updatedSubFeatureType.equals(Type.CONFIG_MUTUAL_EXCLUSION_GROUP) || updatedSubFeatureType.equals(Type.CONFIGURATION_RULE)) ) {
					return new ValidationResults("A configuration item cannot reside in features array.", Status.BAD_REQUEST);	
				}
				
				if (updatedSubFeaturesType.equals(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES) &&
						(updatedSubFeatureType.equals(Type.MUTUAL_EXCLUSION_GROUP) || updatedSubFeatureType.equals(Type.FEATURE)) ) {
					return new ValidationResults("A feature item cannot reside in configurations array.", Status.BAD_REQUEST);	
				}								
			}			
			
			String updatedSubFeatureId = updatedSubFeature.getString(Constants.JSON_FIELD_UNIQUE_ID);
			if (isNewSubAirlockItem(updatedSubFeatureId, null)) { 
				//verify that added sub features are of the same season as the parent
				if (!seasonId.toString().equals(featureObj.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID))) {
					return new ValidationResults("A subfeature is not in the current season.", Status.BAD_REQUEST);
				}

				//verify that added sub feature is not with that same id as the parent.
				if (uniqueId.toString().equals(updatedSubFeatureId)) {
					return new ValidationResults("A subfeature cannot be the same as its parent.", Status.BAD_REQUEST);
				}
				//cannot move an item under an unchecked out item (NONE). The root is an exception - a new feature can be added/moved
				//under the root.
				if (!env.isInMaster() && branchStatus.equals(BranchStatus.NONE)) {
					return new ValidationResults ("You cannot move an item under an item that is not checked out. First check out the parent item.", Status.BAD_REQUEST);
				}
				
				addedSubFeatures.add (updatedSubFeatureId);
			}
		}

		//verify that no original sub features are missing from the updated sub features list.
		//accumulate in missing features in list and verify that not added somewhere else in the tree later on.
		listMissingSubAirlockItems (updatedSubFeatures, curSubFeatures, missingSubFeatures);

		if (!env.isInMaster() && branchStatus.equals(BranchStatus.NONE) && orderChangedIgnoreMissing(updatedSubFeatures, curSubFeatures)) {
			return new ValidationResults("You cannot change the order of sub-items under an item that is not checked out. First check out the parent item. To update a configuration, check out its parent feature.", Status.BAD_REQUEST);
		}
		
		//validate sub features JSON
		for (int i=0; i<updatedSubFeatures.size(); i++) {
			JSONObject updatedSubFeature = updatedSubFeatures.getJSONObject(i);
			BaseAirlockItem featureToUpdate = airlockItemsDB.get(updatedSubFeature.get(Constants.JSON_FIELD_UNIQUE_ID));
			if (featureToUpdate == null) {
				return new ValidationResults("The subfeature '" + updatedSubFeature.get(Constants.JSON_FIELD_UNIQUE_ID) + "' does not exist.", Status.BAD_REQUEST);
			}
			ValidationResults validationRes = featureToUpdate.doValidateFeatureJSON(updatedSubFeature, context, seasonId, addedSubFeatures, missingSubFeatures, userGroups, existingFeaturesInUpdate, this.uniqueId.toString(), updatedFeaturesMap, userInfo, tester, airlockItemsDB, env,copiedStings);
			if (validationRes!=null)
				return validationRes;
		}

		return null;
	}
	
	//if valid return null, else return the error in output String
	private String validateNamespace (String namespace) {
		if (namespace == null || namespace.isEmpty())
			return "The namespace field cannot be null or empty.";

		if (!Character.isLetter(namespace.charAt(0))) {
			return "The namespace field must start with a letter.";
		}

		for (int i=1; i<namespace.length(); i++) {
			Character c = namespace.charAt(i);
			if (!Utilities.isEnglishLetter(c) && !Character.isDigit(c)) {
				return "The namespace field can contain English letters and digits only.";
			}
		}

		if (namespace.equalsIgnoreCase(Constants.RESERVED_NAMESPACE)) {
			return "The '" + namespace + "' namespace is reserved for internal use.";
		}
		return null;
	}

	public static ValidationResults verifyNamespaceNameUniqueness(String newNamespace, String newName, BaseAirlockItem root, UUID uniqueIdToSkip, Type objType) {
		newName = newName.replace(".", " "); //both dot and space are mapped to underscore in java constant file so we cannot allow same name with only dot and spaces switch  				
		String newNameAndNamespace = newNamespace+"."+newName;

		if ((root instanceof DataAirlockItem) && 
				(uniqueIdToSkip == null || !uniqueIdToSkip.toString().equals(root.getUniqueId().toString()))) { //in update - skip yourself
			String replacedName = ((DataAirlockItem)root).getName().replace(".", " ");
			if (newNameAndNamespace.equalsIgnoreCase(((DataAirlockItem)root).getNamespace()+"."+replacedName)){
				if (objType.equals(Type.FEATURE) && newNamespace.equals(Constants.RESERVED_ENTITLEMENTS_NAMESPACE)) {
					return new ValidationResults(String.format(Strings.notUniqueNameReservedEntitlementNamespace, Constants.RESERVED_ENTITLEMENTS_NAMESPACE), Status.BAD_REQUEST);
				}
				else {
					return new ValidationResults(Strings.notUniqueName, Status.BAD_REQUEST);
				}
			}
		}

		if (root.getFeaturesItems()!=null) {
			for (int i=0; i<root.getFeaturesItems().size(); i++) {
				ValidationResults res = verifyNamespaceNameUniqueness (newNamespace, newName, root.getFeaturesItems().get(i), uniqueIdToSkip, objType);
				if (res !=null)
					return res;
			}		
		}

		if (root.getConfigurationRuleItems()!=null) {
			for (int i=0; i<root.getConfigurationRuleItems().size(); i++) {
				ValidationResults res = verifyNamespaceNameUniqueness (newNamespace, newName, root.getConfigurationRuleItems().get(i), uniqueIdToSkip, objType);
				if (res !=null)
					return res;
			}		
		}
		
		if (root.getOrderingRuleItems()!=null) {
			for (int i=0; i<root.getOrderingRuleItems().size(); i++) {
				ValidationResults res = verifyNamespaceNameUniqueness (newNamespace, newName, root.getOrderingRuleItems().get(i), uniqueIdToSkip, objType);
				if (res !=null)
					return res;
			}		
		}
		
		if (root.getEntitlementItems()!=null) {
			for (int i=0; i<root.getEntitlementItems().size(); i++) {
				ValidationResults res = verifyNamespaceNameUniqueness (newNamespace, newName, root.getEntitlementItems().get(i), uniqueIdToSkip, objType);
				if (res !=null)
					return res;
			}		
		}
		
		if (root.getPurchaseOptionsItems()!=null) {
			for (int i=0; i<root.getPurchaseOptionsItems().size(); i++) {
				ValidationResults res = verifyNamespaceNameUniqueness (newNamespace, newName, root.getPurchaseOptionsItems().get(i), uniqueIdToSkip, objType);
				if (res !=null)
					return res;
			}		
		}

		return null;
	}
	static boolean validRole(UserInfo userInfo)
	{
		return userInfo == null || userInfo.getRoles().contains(RoleType.Administrator) || userInfo.getRoles().contains(RoleType.ProductLead);
	}
	protected boolean parentInDevStage(Map<String, BaseAirlockItem> airlockItemsDB, HashMap<String, JSONObject> updatedFeaturesMap, String parent) throws JSONException {
		BaseAirlockItem parentFeature = airlockItemsDB.get(parent); //the original parent 
		JSONObject updatedParent = updatedFeaturesMap.get(parent); //the updated parent if exists
		String typeStr = "";
		String stageStr = "";
		if (updatedParent != null) {
			typeStr = updatedParent.getString(Constants.JSON_FEATURE_FIELD_TYPE);
			if (typeStr.equals(Type.FEATURE.toString()) || typeStr.equals(Type.CONFIGURATION_RULE.toString()))
				stageStr = updatedParent.getString(Constants.JSON_FEATURE_FIELD_STAGE);
		}
		else {					
			if (parentFeature!=null) {
				typeStr = parentFeature.getType().toString();
				if (typeStr.equals(Type.FEATURE.toString()) || typeStr.equals(Type.CONFIGURATION_RULE.toString()))
					stageStr = ((DataAirlockItem)parentFeature).getStage().toString();
			}
		}

		//parent featue in null if parent not found in the updated and in the orig features DB.
		while (parentFeature!=null && !typeStr.equals(Type.ROOT.toString())) {
			if (typeStr.equals(Type.FEATURE.toString()) || typeStr.equals(Type.CONFIGURATION_RULE.toString())) {
				if (stageStr.equals(Stage.DEVELOPMENT.toString())) {
					return true;
				}								
			}
			String nextParentStr = parentFeature.getParent().toString(); 
			parentFeature = airlockItemsDB.get(nextParentStr);
			updatedParent = updatedFeaturesMap.get(nextParentStr); //the updated parent if exists
			if (updatedParent != null) {
				typeStr = updatedParent.getString(Constants.JSON_FEATURE_FIELD_TYPE);
				if (typeStr.equals(Type.FEATURE.toString()) || typeStr.equals(Type.CONFIGURATION_RULE.toString()))
					stageStr = updatedParent.getString(Constants.JSON_FEATURE_FIELD_STAGE);
			}
			else {					
				if (parentFeature!=null) {
					typeStr = parentFeature.getType().toString();
					if (typeStr.equals(Type.FEATURE.toString()) || typeStr.equals(Type.CONFIGURATION_RULE.toString()))
						stageStr = ((DataAirlockItem)parentFeature).getStage().toString();
				}
			}
		}
		return false;
	}

	//relevant only to update since in create the feature has no sub-features.
	private boolean updatedChildInProdStage(JSONArray updatedSubFeatures) {
		try {
			for (int i=0; i<updatedSubFeatures.size(); i++) {
				JSONObject subFeatureJSONObj = updatedSubFeatures.getJSONObject(i);
				if (subFeatureJSONObj.containsKey(Constants.JSON_FEATURE_FIELD_STAGE) && subFeatureJSONObj.get(Constants.JSON_FEATURE_FIELD_STAGE).equals(Stage.PRODUCTION.toString())) {
					return true;
				}
				if (subFeatureJSONObj.containsKey(Constants.JSON_FEATURE_FIELD_FEATURES) && subFeatureJSONObj.get(Constants.JSON_FEATURE_FIELD_FEATURES)!=null) {

					JSONArray subFeatures = subFeatureJSONObj.getJSONArray(Constants.JSON_FEATURE_FIELD_FEATURES);
					boolean found = updatedChildInProdStage (subFeatures);
					if (found)
						return true;

				}
			}		
		} catch (JSONException je) {
			//ignore - will be caught later on in validate. 
		}	
		return false;
	}

	public boolean containSubItemInProductionStage() {
		if (stage.equals(Stage.PRODUCTION)) 		
			return true;

		if (featuresItems != null) {
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
	
	private boolean isChanged(JSONObject updatedFeatureData, Map<String, BaseAirlockItem> airlockItemsDB, boolean allowStageChange) throws JSONException {
		Stage updatedStage = Utilities.strToStage(updatedFeatureData.getString(Constants.JSON_FEATURE_FIELD_STAGE));		
		if (!allowStageChange && !updatedStage.equals(stage))
			return true;

		String updatedName = updatedFeatureData.getString(Constants.JSON_FIELD_NAME);
		if (!updatedName.equals(name)) 
			return true;

		String updatedNamespace = updatedFeatureData.getString(Constants.JSON_FEATURE_FIELD_NAMESPACE);
		if (!namespace.equals(updatedNamespace)) 
			return true;

		Boolean updatedEnabled = updatedFeatureData.getBoolean(Constants.JSON_FEATURE_FIELD_ENABLED);
		if (enabled != updatedEnabled) 
			return true;		

		int updatedRolloutPercentage = updatedFeatureData.getInt(Constants.JSON_FEATURE_FIELD_PERCENTAGE);
		if (rolloutPercentage  != updatedRolloutPercentage) 
			return true;

		JSONObject updatedRuleJSON = updatedFeatureData.getJSONObject(Constants.JSON_FEATURE_FIELD_RULE);
		Rule updatedRule = new Rule();
		updatedRule.fromJSON(updatedRuleJSON);
		if (!rule.equals(updatedRule)) {
			return true;
		}
		
		String updatedMinAppVersion = updatedFeatureData.getString(Constants.JSON_FEATURE_FIELD_MIN_APP_VER);
		if (minAppVersion==null || !minAppVersion.equals(updatedMinAppVersion)) {
			return true;							
		}	

		//optional fields
		if (updatedFeatureData.containsKey(Constants.JSON_FIELD_DESCRIPTION) &&  updatedFeatureData.get(Constants.JSON_FIELD_DESCRIPTION)!=null) {
			//if missing from json or null - ignore
			String updatedDescription = updatedFeatureData.getString(Constants.JSON_FIELD_DESCRIPTION);
			if (description == null || !description.equals(updatedDescription)) {
				return true;				
			}
		}
		
		if (updatedFeatureData.containsKey(Constants.JSON_FEATURE_FIELD_OWNER) &&  updatedFeatureData.get(Constants.JSON_FEATURE_FIELD_OWNER)!=null) {
			//if missing from json or null - ignore
			String updatedOwner= updatedFeatureData.getString(Constants.JSON_FEATURE_FIELD_OWNER);
			if (owner == null || !owner.equals(updatedOwner)) {
				return true;				
			}
		}
								

		if (updatedFeatureData.containsKey(Constants.JSON_FIELD_INTERNAL_USER_GROUPS) && updatedFeatureData.get(Constants.JSON_FIELD_INTERNAL_USER_GROUPS)!=null) {
			JSONArray updatedInternalUserGroups = updatedFeatureData.getJSONArray(Constants.JSON_FIELD_INTERNAL_USER_GROUPS);
			if (!Utilities.stringArrayCompareIgnoreOrder(updatedInternalUserGroups,internalUserGroups)) {
				return true;				
			}
		}	
		
		if (updatedFeatureData.containsKey(Constants.JSON_FEATURE_FIELD_NO_CACHED_RES) && updatedFeatureData.get(Constants.JSON_FEATURE_FIELD_NO_CACHED_RES)!=null) {				
			Boolean updatedNoCachedResults = updatedFeatureData.getBoolean(Constants.JSON_FEATURE_FIELD_NO_CACHED_RES);
			if (noCachedResults != updatedNoCachedResults) 
				return true;			
		}
			
		return false;		
	}
	
	public ValidationResults validateMasterFeatureNotChangedFromBranch(JSONObject updatedFeatureData, Map<String, BaseAirlockItem> airlockItemsDB, Environment env) throws JSONException {
		//can update in master or checked out or new features in branch
		if (env.isInMaster() || branchStatus!=BranchStatus.NONE) 
			return null;
		
		//Creator, creation date, seasonId and type should not be updated

		//At this stage we can be sure that all mandatory fields exist and legal types exist in the json since validate was previously called.
		
		String err = "You cannot update an item that is not checked out. First check out the item. To update a configuration, check out its parent feature.";

		if (isChanged(updatedFeatureData, airlockItemsDB, false))
			return new ValidationResults(err, Status.BAD_REQUEST);				
		
		return null;
	}	

	
	//If user moved from prod to dev in standAlone branch or branch that is not participating in production experiment:
	//return validationResults with error code 200 (this change is allowed but the production runtime file should be written)
	
	//considerProdUnderDev: for prod runtime file - we should consider prod under dev as dev
	//                      for user permissions - we should consider prod under dev as prod 
	public ValidationResults validateProductionDontChanged(JSONObject updatedFeatureData, Map<String, BaseAirlockItem> airlockItemsDB, Branch branch, ServletContext context, boolean considerProdUnderDevAsDev, Environment env) throws JSONException {
		//Creator, creation date, seasonId and type should not be updated

		//At this stage we can be sure that all mandatory fields exist and legal types exist in the json since validate was previously called.		

		Stage updatedStage = Utilities.strToStage(updatedFeatureData.getString(Constants.JSON_FEATURE_FIELD_STAGE));

		boolean stageChanged = (updatedStage == Stage.PRODUCTION && stage == Stage.DEVELOPMENT);
		if (considerProdUnderDevAsDev && parent!=null) {
			BaseAirlockItem parentItem = airlockItemsDB.get(parent.toString());
			if (!parentItem.getType().equals(Type.ROOT)) {
				if (parentItem instanceof DataAirlockItem) {
					stageChanged = (stageChanged && isProductionFeature(parentItem, airlockItemsDB));
				}
				else {
					//mtx + config mtx + orderingRule mtx (if one of their cildren change stae + none of their ancesstors are dev they are production)
					stageChanged = (stageChanged && !hasDevelopmentAncesstor(parentItem, airlockItemsDB));
				}
			}
		}

		if (stageChanged) {
			return new ValidationResults("Unable to update the feature. Only a user with the Administrator or Product Lead can change a subfeature from the development to the production stage.", Status.UNAUTHORIZED);	
		}	
		
		ValidationResults res = null;
		//in standAlone branch or branch that is not participating in production experiment: every one can move 
		//feature from prod to dev
		
		boolean consideredProd;
		if (considerProdUnderDevAsDev) {
			consideredProd = isProductionFeature(this, airlockItemsDB);
			stageChanged = (updatedStage == Stage.DEVELOPMENT && consideredProd);
		}
		else {
			consideredProd = stage == Stage.PRODUCTION;
			stageChanged = (updatedStage == Stage.DEVELOPMENT && consideredProd);
		}
		
		if (stageChanged) {
			if (branch!=null && branch.isPartOfExperimentInProduction(context)==null) {
				res = new ValidationResults("", Status.OK);
			}
			else {
				return new ValidationResults("Unable to update the feature. Only a user with the Administrator or Product Lead can change a subfeature from the production to the development stage.", Status.UNAUTHORIZED);
			}
		}	

		String err = "Unable to update the " + getObjTypeStrByType() + ". Only a user with the Administrator or Product Lead role can change a subitem that is in the production stage.";		
		
		if (consideredProd) {
			if (isChanged(updatedFeatureData, airlockItemsDB, true)) {
				return new ValidationResults(err, Status.UNAUTHORIZED);
			}
		}
		
		ValidationResults superRes = super.validateProductionDontChanged(updatedFeatureData, airlockItemsDB, branch, context, considerProdUnderDevAsDev, env);
		if (superRes!=null)
			return superRes;
				
		return res;
	}	

	private boolean isLegacyPercentage (Environment env) {
		Version version = env.getVersion();
		return version.i < Version.v2_5.i; //only pre 2.5 seasons have percentage bitmaps
	}

	public Boolean isStringUsed(String stringId,Set<String> stringIds){
		if (this.getType() == Type.CONFIGURATION_RULE) {
			ConfigurationRuleItem crItem = (ConfigurationRuleItem)this;
			Set<String> foundIds = VerifyRule.findAllTranslationIds(stringIds, crItem.getConfiguration(), false);
			if(foundIds.contains(stringId)){
				return true;
			}
		}
		return false;
	}
	public JSONObject getStringsInUseByItem(Set<String> stringIds, Season season) throws JSONException {
		Set<String> stringsInUseByConfigList = new HashSet<String>();
		Set<String> stringsInUseByUtilList = new HashSet<String>();
		doGetStringsInUseByItem(stringIds, this, stringsInUseByConfigList, stringsInUseByUtilList, season, false, true);
		
		JSONObject res = new JSONObject();
		JSONArray strByConfArray = new JSONArray();
		for (String key:stringsInUseByConfigList) {
			JSONObject strObj = new JSONObject();
			strObj.put(Constants.JSON_FIELD_KEY, key);
			strObj.put(Constants.JSON_FIELD_ID, season.getOriginalStrings().getOriginalStringByKey(key).getUniqueId().toString());
			strByConfArray.put(strObj);			
		}
		res.put (Constants.JSON_FIELD_STRINGS_IN_USE_BY_CONFIG, strByConfArray);
		
		JSONArray strByUtilArray = new JSONArray();
		for (String key:stringsInUseByUtilList) {
			JSONObject strObj = new JSONObject();
			strObj.put(Constants.JSON_FIELD_KEY, key);
			strObj.put(Constants.JSON_FIELD_ID, season.getOriginalStrings().getOriginalStringByKey(key).getUniqueId().toString());
			strByUtilArray.put(strObj);			
		}
		res.put (Constants.JSON_FIELD_STRINGS_IN_USE_BY_UTIL, strByUtilArray);
		
		return res;
	}
		
	public boolean isAnalyticsChanged(JSONObject updatedFeatureData, Season season, ServletContext context, Environment env) throws JSONException {
		String updatedStage = updatedFeatureData.getString(Constants.JSON_FEATURE_FIELD_STAGE);
		
		//if the stage was changed 
		if (!updatedStage.equals(stage.toString())) {
			//if the item is reported to analytics
			if (env.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection().getFeaturesOnOffMap().containsKey(uniqueId.toString())) 
				return true;
				
			//if the item is feature that some of its attributes are reported to analytics
			if ((type.equals(Type.FEATURE) || type.equals(Type.PURCHASE_OPTIONS) || type.equals(Type.ENTITLEMENT))
					&& env.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection().getFeaturesConfigurationAttributesMap().containsKey(uniqueId.toString()))
				return true;
		}
			
		boolean  analyticsChanged = super.isAnalyticsChanged(updatedFeatureData, season, context, env);
		if (analyticsChanged) 
			return true;
		
		return false;
	}

}
