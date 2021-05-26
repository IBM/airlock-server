package com.ibm.airlock.admin;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.HashMap;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response.Status;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Strings;
import com.ibm.airlock.admin.BaseAirlockItem.Type;
import com.ibm.airlock.admin.analytics.Experiment;
import com.ibm.airlock.admin.analytics.Variant;
import com.ibm.airlock.admin.purchases.EntitlementItem;
import com.ibm.airlock.admin.purchases.PurchaseOptionsItem;

public class InternalUserGroups {
	private Date lastModified = null;
	private TreeSet<String> groups = new TreeSet<String>(); //for keeping order
	private HashMap<String, Integer> groupsMap = new HashMap<String, Integer>(); //to enable o(1) access for feature creation/update validation
	
	public InternalUserGroups() {
		lastModified = new Date();
	}
	
	public Date getLastModified() {
		return lastModified;
	}

	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

	public TreeSet<String> getGroups() {
		return groups;
	}

	public String getGroupsAsArrayString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		boolean groupExists = false;
		for (String group:groups) {
			groupExists=true;
			sb.append(group);
			sb.append(",");
		}
		
		if (groupExists)
			sb.deleteCharAt(sb.length()-1);
		sb.append("]");
		
		return sb.toString();
	}
	
	public void setGroups(TreeSet<String> groups) {
		this.groups = groups;
	}

	public HashMap<String, Integer> getGroupsMap() {
		return groupsMap;
	}

	public void setGroupsMap(HashMap<String, Integer> groupsMap) {
		this.groupsMap = groupsMap;
	}
	
	public JSONObject toJson() throws JSONException {
		JSONObject res = new JSONObject();
		res.put(Constants.JSON_FIELD_LAST_MODIFIED, lastModified.getTime());
		res.put(Constants.JSON_FIELD_INTERNAL_USER_GROUPS, groups);
		
		return res;
	}
	
	public void fromJSON (JSONObject input) throws JSONException {		
		if (input.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) && input.get(Constants.JSON_FIELD_LAST_MODIFIED)!=null) { 
			long timeInMS = (Long)input.get(Constants.JSON_FIELD_LAST_MODIFIED);
			lastModified = new Date(timeInMS);
		}  else {
			lastModified = new Date();
		}
				
		groups.clear();
		if (input.containsKey(Constants.JSON_FIELD_INTERNAL_USER_GROUPS) && input.get(Constants.JSON_FIELD_INTERNAL_USER_GROUPS)!=null) {
			JSONArray groupsJSONArr = input.getJSONArray(Constants.JSON_FIELD_INTERNAL_USER_GROUPS);
			if (groupsJSONArr != null && groupsJSONArr.size()>0) {
				for (int i=0; i<groupsJSONArr.size(); i++) {
					groups.add(groupsJSONArr.getString(i));										
					groupsMap.put(groupsJSONArr.getString(i), 0);
				}
			}
		}								
	}
	
	//return null if valid, ValidationResults otherwise
	public ValidationResults validateInternalUserGroupsJSON(JSONObject groupsJSON, String productId, ServletContext context) {		
		try {
			//groups must appear
			if (!groupsJSON.containsKey(Constants.JSON_FIELD_INTERNAL_USER_GROUPS) || groupsJSON.get(Constants.JSON_FIELD_INTERNAL_USER_GROUPS)==null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_INTERNAL_USER_GROUPS), Status.BAD_REQUEST);
			}
			
			JSONArray userGroupsArr = groupsJSON.getJSONArray(Constants.JSON_FIELD_INTERNAL_USER_GROUPS); //validate that is String array value
						
			//modification date must appear
			if (!groupsJSON.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) || groupsJSON.get(Constants.JSON_FIELD_LAST_MODIFIED)==null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_LAST_MODIFIED), Status.BAD_REQUEST);
			}				
			
			//verify that given modification date is not older that current modification date
			long givenModoficationDate = groupsJSON.getLong(Constants.JSON_FIELD_LAST_MODIFIED);  //verify that legal long
			Date givenDate = new Date(givenModoficationDate);
			if (givenDate.before(lastModified)) {
				return new ValidationResults("User groups were changed by another user. Refresh your browser and try again.", Status.CONFLICT);			
			}
		
			//verify that there are no duplications in the user groups
			for(int j = 0; j < userGroupsArr.length(); j++){
			    for(int k = j+1; k < userGroupsArr.length(); k++){
			        if (userGroupsArr.get(j).equals(userGroupsArr.get(k))){
			        	return new ValidationResults("The internalUserGroups value '" + userGroupsArr.get(k) + "' appears more than once in the internalUserGroups list.", Status.BAD_REQUEST);
			        }
			    }
			}
			
			//identify deleted userGroups and verify that they are not in use
			LinkedList<String> removedUserGroups = new LinkedList<String>();
			for (String existingUserGroup:groups) {
				boolean found = false;
				for(int k = 0; k < userGroupsArr.length(); k++){
					if (userGroupsArr.getString(k).equals(existingUserGroup) ) {
						found = true;
						break;						
					}					
				}	
				if (!found)
					removedUserGroups.add(existingUserGroup);
				
			}
			
			if (removedUserGroups.isEmpty())
				return null;
			
			//for each season, for each feature and config and ordering rule - verify that not using one of the deleted user groups
			@SuppressWarnings("unchecked")
			Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);		
	
			Product prod = productsDB.get(productId);

			for (Experiment exp:prod.getExperimentsMutualExclusionGroup().getExperiments()) {
				String res = deletedUserGroupAreInUse(exp.getInternalUserGroups(), removedUserGroups, exp.getName());
				if (res!=null)
					return new ValidationResults(res, Status.BAD_REQUEST);
				for (Variant var:exp.getVariants()) {
					res = deletedUserGroupAreInUse(var.getInternalUserGroups(), removedUserGroups, exp.getName() + ", " + var.getName());
					if (res!=null)
						return new ValidationResults(res, Status.BAD_REQUEST);
				}
			}
			 
			for (Season season:prod.getSeasons()) {
				String res = validateDeletedUserGroupIsNotInUse(removedUserGroups, season.getRoot(), context);
				if (res != null) {
					return new ValidationResults(res, Status.BAD_REQUEST);
				}
				
				for (Branch b:season.getBranches().getBranchesList()) {
					for (BaseAirlockItem branchSubTreeRoot:b.getBranchFeatures()) {
						res = validateDeletedUserGroupIsNotInUse(removedUserGroups, branchSubTreeRoot, context);
						if (res != null) {
							return new ValidationResults(res, Status.BAD_REQUEST);
						}
					}
				}
			}			
		} catch (JSONException jsne) {
			return new ValidationResults(jsne.getMessage(), Status.BAD_REQUEST);
		}
		catch (ClassCastException cce) {
			return new ValidationResults("Illegal internalUserGroups JSON: " + cce.getMessage(), Status.BAD_REQUEST);
		}
		
		return null;
	}

	private String validateDeletedUserGroupIsNotInUseByExperiment(LinkedList<String> removedUserGroups, Product prod, ServletContext context) {
		for (Experiment exp : prod.getExperimentsMutualExclusionGroup().getExperiments()) {
			String res = deletedUserGroupAreInUse(exp.getInternalUserGroups(), removedUserGroups, exp.getName());
			if (res!=null)
				return res;
			
			for (Variant var: exp.getVariants()) {
				res = deletedUserGroupAreInUse(var.getInternalUserGroups(), removedUserGroups, var.getName());
				if (res!=null)
					return res;
			}
		}

		return null;
	}

	private String validateDeletedUserGroupIsNotInUse(LinkedList<String> removedUserGroups, BaseAirlockItem root, ServletContext context) {
		//check on the root
		if (root instanceof DataAirlockItem) {
			DataAirlockItem item = (DataAirlockItem)root;
			String[] featureUserGroup = item.getInternalUserGroups();
			String res = deletedUserGroupAreInUse(featureUserGroup, removedUserGroups, item.getNameSpaceDotName());
			if (res!=null)
				return res;
		}
		
		//check on subItems
		if (root.getFeaturesItems()!=null) {
			for (int i=0; i<root.getFeaturesItems().size(); i++) {	
				if (root.getFeaturesItems().get(i).getType() == Type.FEATURE) {
					FeatureItem featureItem = (FeatureItem)root.getFeaturesItems().get(i);
					String[] featureUserGroup = featureItem.getInternalUserGroups();
					String res = deletedUserGroupAreInUse(featureUserGroup, removedUserGroups, featureItem.getNameSpaceDotName());
					if (res!=null)
						return res;
				}
				String res = validateDeletedUserGroupIsNotInUse(removedUserGroups, root.getFeaturesItems().get(i), context);
				if (res!=null)
					return res;
			}
		}
		if (root.getConfigurationRuleItems() != null) {
			for (int i=0; i<root.getConfigurationRuleItems().size(); i++) {
				if (root.getConfigurationRuleItems().get(i).getType() == Type.CONFIGURATION_RULE) {
					ConfigurationRuleItem crItem = (ConfigurationRuleItem)root.getConfigurationRuleItems().get(i);

					String[] featureUserGroup = crItem.getInternalUserGroups();
					String res = deletedUserGroupAreInUse(featureUserGroup, removedUserGroups, crItem.getNameSpaceDotName());
					if (res!=null)
						return res;	
				}
				String res = validateDeletedUserGroupIsNotInUse(removedUserGroups, root.getConfigurationRuleItems().get(i), context);
				if (res!=null)
					return res;
			}
		}
		if (root.getOrderingRuleItems() != null) {
			for (int i=0; i<root.getOrderingRuleItems().size(); i++) {
				if (root.getOrderingRuleItems().get(i).getType() == Type.ORDERING_RULE) {
					OrderingRuleItem orItem = (OrderingRuleItem)root.getOrderingRuleItems().get(i);

					String[] featureUserGroup = orItem.getInternalUserGroups();
					String res = deletedUserGroupAreInUse(featureUserGroup, removedUserGroups, orItem.getNameSpaceDotName());
					if (res!=null)
						return res;	
				}
				String res = validateDeletedUserGroupIsNotInUse(removedUserGroups, root.getOrderingRuleItems().get(i), context);
				if (res!=null)
					return res;
			}
		}
		if (root.getEntitlementItems() != null) {
			for (int i=0; i<root.getEntitlementItems().size(); i++) {
				if (root.getEntitlementItems().get(i).getType() == Type.ENTITLEMENT) {
					EntitlementItem ipItem = (EntitlementItem)root.getEntitlementItems().get(i);

					String[] featureUserGroup = ipItem.getInternalUserGroups();
					String res = deletedUserGroupAreInUse(featureUserGroup, removedUserGroups, ipItem.getNameSpaceDotName());
					if (res!=null)
						return res;	
				}
				String res = validateDeletedUserGroupIsNotInUse(removedUserGroups, root.getEntitlementItems().get(i), context);
				if (res!=null)
					return res;
			}
		}
		if (root.getPurchaseOptionsItems() != null) {
			for (int i=0; i<root.getPurchaseOptionsItems().size(); i++) {
				if (root.getPurchaseOptionsItems().get(i).getType() == Type.PURCHASE_OPTIONS) {
					PurchaseOptionsItem poItem = (PurchaseOptionsItem)root.getPurchaseOptionsItems().get(i);

					String[] featureUserGroup = poItem.getInternalUserGroups();
					String res = deletedUserGroupAreInUse(featureUserGroup, removedUserGroups, poItem.getNameSpaceDotName());
					if (res!=null)
						return res;	
				}
				String res = validateDeletedUserGroupIsNotInUse(removedUserGroups, root.getPurchaseOptionsItems().get(i), context);
				if (res!=null)
					return res;
			}
		}

		return null;
	}

	private String deletedUserGroupAreInUse(String[] usedUserGroup, LinkedList<String> removedUserGroups, String itemName) {
		if (usedUserGroup == null)
			return null;
		
		for (int i=0; i<usedUserGroup.length; i++) {
			for (int j=0; j<removedUserGroups.size(); j++)
				if (usedUserGroup[i].equals(removedUserGroups.get(j))) {
					return "The '" + usedUserGroup[i] + "' user group is in use by '"+ itemName+ "' so it cannot be deleted.";
				}
		}
		return null;
	}

	public JSONObject toUsageJson(Product prod, ServletContext context) throws JSONException {
		
		//map between season and map between userGroup and the ids of dataItems using it
		TreeMap<String,TreeMap <String, ArrayList<String>>> usageInSeasonInMaster = new  TreeMap<String, TreeMap <String, ArrayList<String>>>();
		//map between season and map between branch id and map userGroup and the ids of dataItems using it
		TreeMap<String, TreeMap<String, TreeMap <String, ArrayList<String>>>> usageInSeasonInBranches = new TreeMap<String, TreeMap<String, TreeMap <String, ArrayList<String>>>>();
		
		//build maps from user groups to items using it
		for (Season season:prod.getSeasons()) {
			//map between userGroup and the ids of dataItems using it
			TreeMap <String, ArrayList<String>> usageInMaster = new  TreeMap <String, ArrayList<String>>();
			setUserGroupUsageMap(season.getRoot(), usageInMaster);
			
			//map between branch id and map userGroup and the ids of dataItems using it
			TreeMap<String, TreeMap <String, ArrayList<String>>> usageInBranches = new TreeMap<String, TreeMap <String, ArrayList<String>>>();
			
			for (Branch b:season.getBranches().getBranchesList()) {
				//map between userGroup and the ids of dataItems using it
				TreeMap <String, ArrayList<String>> usageInBranch = new  TreeMap <String, ArrayList<String>>();
				
				for (BaseAirlockItem branchSubTreeRoot:b.getBranchFeatures()) {
					setUserGroupUsageMap(branchSubTreeRoot, usageInBranch);
					usageInBranches.put(b.getUniqueId().toString(), usageInBranch);
				}
			}
			
			usageInSeasonInMaster.put(season.getUniqueId().toString(), usageInMaster);
			usageInSeasonInBranches.put(season.getUniqueId().toString(), usageInBranches);
		}
		
		//map between userGroup and the ids of experiments using it
		TreeMap <String, ArrayList<String>> usageInExperiments = new  TreeMap <String, ArrayList<String>>();
		
		//map between userGroup and the ids of variants using it
		TreeMap <String, ArrayList<String>> usageInVariants = new  TreeMap <String, ArrayList<String>>();
				
		for (Experiment exp:prod.getExperimentsMutualExclusionGroup().getExperiments()) {
			addExperimentToUserGroups(exp, usageInExperiments);
			
			for (Variant var:exp.getVariants()) {
				addVariantToUserGroups(var, usageInVariants);
			}
		}
		
		
		@SuppressWarnings("unchecked")
		Map<String, Experiment> experimentsDB = (Map<String, Experiment>)context.getAttribute(Constants.EXPERIMENTS_DB_PARAM_NAME);

		@SuppressWarnings("unchecked")
		Map<String, Variant> variantsDB = (Map<String, Variant>)context.getAttribute(Constants.VARIANTS_DB_PARAM_NAME);

		@SuppressWarnings("unchecked")
		Map<String, BaseAirlockItem> itemsDB =  (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);
		
		//building the result json
		JSONObject res = new JSONObject();
		res.put(Constants.JSON_FIELD_PRODUCT_ID, prod.getUniqueId().toString());
		JSONArray userGroupsArray = new JSONArray();
		
		for (String userGroup:groups) {
			JSONObject userGroupObj = new JSONObject();
			userGroupObj.put(Constants.JSON_FIELD_INTERNAL_USER_GROUP, userGroup);
			
			//experiments
			JSONArray experimentsArray = new JSONArray();
			HashMap<String, JSONObject> experimentsIdsToJsonObjectMap= new HashMap<String, JSONObject>();
		
			ArrayList<String> experimentsUsingUserGroup = usageInExperiments.get(userGroup);
			if (experimentsUsingUserGroup!=null) {
				for (String expId:experimentsUsingUserGroup) {
					JSONObject expObj = new JSONObject();
					expObj.put(Constants.JSON_FIELD_UNIQUE_ID, expId);
					Experiment exp = experimentsDB.get(expId);
					expObj.put(Constants.JSON_FIELD_NAME, exp.getName());
					expObj.put(Constants.JSON_FIELD_DISPLAY_NAME, exp.getDisplayName());
					expObj.put(Constants.JSON_FIELD_IN_USE, true);
					expObj.put(Constants.JSON_FIELD_VARIANTS, new JSONArray());
					experimentsArray.add(expObj);
					experimentsIdsToJsonObjectMap.put(expId, expObj);
				}
			}
			
			//variants
			ArrayList<String> variantsUsingUserGroup = usageInVariants.get(userGroup);
			if (variantsUsingUserGroup!=null) {
					
				for (String varId:variantsUsingUserGroup) {
					JSONObject varObj = new JSONObject();
					varObj.put(Constants.JSON_FIELD_UNIQUE_ID, varId);
					Variant var = variantsDB.get(varId);
					varObj.put(Constants.JSON_FIELD_NAME, var.getName());
					varObj.put(Constants.JSON_FIELD_DISPLAY_NAME, var.getDisplayName());
					
					//find experiment object
					String expId = var.getExperimentId().toString();
					JSONObject expObj = experimentsIdsToJsonObjectMap.get(expId);
					if (expObj == null) {
						expObj = new JSONObject();
						expObj.put(Constants.JSON_FIELD_UNIQUE_ID, expId);
						Experiment exp = experimentsDB.get(expId);
						expObj.put(Constants.JSON_FIELD_NAME, exp.getName());
						expObj.put(Constants.JSON_FIELD_DISPLAY_NAME, exp.getDisplayName());
						expObj.put(Constants.JSON_FIELD_IN_USE, false);
						expObj.put(Constants.JSON_FIELD_VARIANTS, new JSONArray());
						experimentsArray.add(expObj);
						experimentsIdsToJsonObjectMap.put(expId, expObj);
					}
					expObj.getJSONArray(Constants.JSON_FIELD_VARIANTS).add(varObj);
				}
			}
			userGroupObj.put(Constants.JSON_FIELD_EXPERIMENTS, experimentsArray);
			
			//seasons
			JSONArray seasonsArray = new JSONArray();
			for (Season season:prod.getSeasons()) {
				JSONObject seasonObj = new JSONObject();
				seasonObj.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, season.getUniqueId().toString());
				seasonObj.put(Constants.JSON_SEASON_FIELD_MAX_VER, season.getMaxVersion());
				seasonObj.put(Constants.JSON_SEASON_FIELD_MIN_VER, season.getMinVersion());
				
				TreeMap <String, ArrayList<String>> usageInMaster = usageInSeasonInMaster.get(season.getUniqueId().toString());
				JSONArray masterItemsArray = new JSONArray();
				
				if (usageInMaster!=null) {
					//master items
					ArrayList<String> itemsinMasterUsingUserGroup = usageInMaster.get(userGroup);
					
					if (itemsinMasterUsingUserGroup!=null) {
						for (String itemId:itemsinMasterUsingUserGroup) {
							JSONObject itemObj = new JSONObject();
							itemObj.put(Constants.JSON_FIELD_UNIQUE_ID, itemId);
							DataAirlockItem item = (DataAirlockItem)itemsDB.get(itemId);
							itemObj.put(Constants.JSON_FIELD_NAME, item.getName());
							itemObj.put(Constants.JSON_FEATURE_FIELD_NAMESPACE, item.getNamespace());
							itemObj.put(Constants.JSON_FEATURE_FIELD_TYPE, item.getType().toString());
							masterItemsArray.add(itemObj);
						}
					}
				}
				seasonObj.put(Constants.JSON_FEATURE_FIELD_FEATURES, masterItemsArray);
				
				//branches
				JSONArray branchesArr = new JSONArray();
				TreeMap <String, TreeMap <String, ArrayList<String>>> usageInBranches = usageInSeasonInBranches.get(season.getUniqueId().toString());
				for (Branch branch:season.getBranches().getBranchesList()) {
					JSONObject branchObj = new JSONObject();
					String branchId = branch.getUniqueId().toString();
					TreeMap <String, ArrayList<String>> usageInBranch = usageInBranches.get(branchId);
					JSONArray branchItemsArray = new JSONArray();
					if (usageInBranch!=null) {
						//branch items
						ArrayList<String> itemsInBranchUsingUserGroup = usageInBranch.get(userGroup);						
						if (itemsInBranchUsingUserGroup!=null) {
							for (String itemId:itemsInBranchUsingUserGroup) {
								JSONObject itemObj = new JSONObject();
								itemObj.put(Constants.JSON_FIELD_UNIQUE_ID, itemId);
								DataAirlockItem item = (DataAirlockItem)branch.getBranchAirlockItemsBD().get(itemId);
								itemObj.put(Constants.JSON_FIELD_NAME, item.getName());
								itemObj.put(Constants.JSON_FEATURE_FIELD_NAMESPACE, item.getNamespace());
								itemObj.put(Constants.JSON_FEATURE_FIELD_TYPE, item.getType().toString());
								branchItemsArray.add(itemObj);
							}
						}
					}
					if (branchItemsArray.size() > 0) { //add branch only if is contains features using the userGroup
						branchObj.put(Constants.JSON_FIELD_UNIQUE_ID, branchId);
						branchObj.put(Constants.JSON_FIELD_NAME, branch.getName());
						branchObj.put(Constants.JSON_FEATURE_FIELD_FEATURES, branchItemsArray);
						branchesArr.add(branchObj);
					}
				}
				
				seasonObj.put(Constants.JSON_FIELD_BRANCHES, branchesArr);
				
				seasonsArray.add(seasonObj);
			}
			
			userGroupObj.put(Constants.JSON_PRODUCT_FIELD_SEASONS, seasonsArray);
			
			userGroupsArray.add(userGroupObj);			
		}
		
		res.put(Constants.JSON_FIELD_INTERNAL_USER_GROUPS, userGroupsArray);
		return res;
	}

	private void setUserGroupUsageMap(BaseAirlockItem root, TreeMap<String, ArrayList<String>> usageMap) {
		//check on the root
		if (root instanceof DataAirlockItem) {
			DataAirlockItem item = (DataAirlockItem)root;
			addDataItemToUserGroups(item, usageMap);
		}
		
		//check on subItems
		if (root.getFeaturesItems()!=null) {
			for (int i=0; i<root.getFeaturesItems().size(); i++) {	
				setUserGroupUsageMap(root.getFeaturesItems().get(i), usageMap);
			}
		}
		if (root.getConfigurationRuleItems() != null) {
			for (int i=0; i<root.getConfigurationRuleItems().size(); i++) {
				setUserGroupUsageMap(root.getConfigurationRuleItems().get(i), usageMap);
			}
		}
		if (root.getOrderingRuleItems() != null) {
			for (int i=0; i<root.getOrderingRuleItems().size(); i++) {
				setUserGroupUsageMap(root.getOrderingRuleItems().get(i), usageMap);
			}
		}
		if (root.getEntitlementItems() != null) {
			for (int i=0; i<root.getEntitlementItems().size(); i++) {
				setUserGroupUsageMap(root.getEntitlementItems().get(i), usageMap);
			}
		}
		if (root.getPurchaseOptionsItems() != null) {
			for (int i=0; i<root.getPurchaseOptionsItems().size(); i++) {
				setUserGroupUsageMap(root.getPurchaseOptionsItems().get(i), usageMap);
			}
		}
	}

	private void addDataItemToUserGroups(DataAirlockItem item, TreeMap<String, ArrayList<String>> usageMap) {
		String[] featureUserGroup = item.getInternalUserGroups();
		if (featureUserGroup == null)
			return;
		for (String ug:featureUserGroup) {
			ArrayList<String> ugItems = usageMap.get(ug);
			if(ugItems == null) {
				ugItems = new ArrayList<String>();
				usageMap.put(ug,  ugItems);
			}
			ugItems.add(item.getUniqueId().toString());
		}
	}
	
	private void addExperimentToUserGroups(Experiment experiment, TreeMap<String, ArrayList<String>> usageMap) {
		String[] featureUserGroup = experiment.getInternalUserGroups();
		if (featureUserGroup == null)
			return;
		for (String ug:featureUserGroup) {
			ArrayList<String> ugExperiments = usageMap.get(ug);
			if(ugExperiments == null) {
				ugExperiments = new ArrayList<String>();
				usageMap.put(ug,  ugExperiments);
			}
			ugExperiments.add(experiment.getUniqueId().toString());
		}
	}
	
	private void addVariantToUserGroups(Variant variant, TreeMap<String, ArrayList<String>> usageMap) {
		String[] featureUserGroup = variant.getInternalUserGroups();
		if (featureUserGroup == null)
			return;
		for (String ug:featureUserGroup) {
			ArrayList<String> ugVariants = usageMap.get(ug);
			if(ugVariants == null) {
				ugVariants = new ArrayList<String>();
				usageMap.put(ug,  ugVariants);
			}
			ugVariants.add(variant.getUniqueId().toString());
		}
	}

}
