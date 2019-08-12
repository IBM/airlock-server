package com.ibm.airlock.admin.analytics;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response.Status;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Constants.Action;
import com.ibm.airlock.Constants.OutputJSONMode;
import com.ibm.airlock.Constants.Stage;
import com.ibm.airlock.Strings;
import com.ibm.airlock.admin.authentication.UserInfo;
import com.ibm.airlock.admin.MergeBranch.MergeException;
import com.ibm.airlock.admin.InternalUserGroups;
import com.ibm.airlock.admin.Product;
import com.ibm.airlock.admin.Season;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.ValidationResults;

public class Variant extends BaseStagedAnalyticsItem {

	private final static String RESERVED_NAME = "DEFAULT";
	private UUID experimentId = null; //c+u
	private String branchName = null; //c+u
	
	public UUID getExperimentId() {
		return experimentId;
	}

	public void setExperimentId(UUID experimentId) {
		this.experimentId = experimentId;
	}
	
	public String getBranchName() {
		return branchName;
	}

	public void setBranchName(String branchName) {
		this.branchName = branchName;
	}

	public Variant(UUID experimentId) {
		super();
		this.experimentId = experimentId;		
	}	

	//return null is no errors 
	//return status 200 (OK) if need experiment analytics quota validation (branch changed while in prod or moved from dev to prod)
	//return status other than 200 upon error 
	public ValidationResults validateVariantJSON(JSONObject variantJSON, ServletContext context, UserInfo userInfo, String expMinVersion, String expMaxVersion, boolean validateExpeimentQuota) throws MergeException {			
		try {
			ValidationResults superRes = super.validateStagedAnalyticsItemJSON(variantJSON, context, userInfo);
			if (superRes!=null)
				return superRes;
			
			@SuppressWarnings("unchecked")
			Map<String, Experiment> experimentsDB = (Map<String, Experiment>)context.getAttribute(Constants.EXPERIMENTS_DB_PARAM_NAME);
				
			//experimentId
			if (!variantJSON.containsKey(Constants.JSON_FIELD_EXPERIMENT_ID) || variantJSON.getString(Constants.JSON_FIELD_EXPERIMENT_ID) == null || variantJSON.getString(Constants.JSON_FIELD_EXPERIMENT_ID).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_EXPERIMENT_ID), Status.BAD_REQUEST);
			}
	
			String expId = (String)variantJSON.get(Constants.JSON_FIELD_EXPERIMENT_ID);
			Experiment experiment = experimentsDB.get(expId);
			
			if (experiment == null) {
				return new ValidationResults("The variant does not exist in the given experiment.", Status.BAD_REQUEST);
			} 
			
			@SuppressWarnings("unchecked")
			Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);					
			
			Product prod = productsDB.get(experiment.getProductId().toString());
			
			if (prod == null) {
				return new ValidationResults("The product of the given variant's experiment does not exist.", Status.BAD_REQUEST);
			}
			
			//branchName
			if (!variantJSON.containsKey(Constants.JSON_FIELD_BRANCH_NAME) || variantJSON.get(Constants.JSON_FIELD_BRANCH_NAME) == null || variantJSON.getString(Constants.JSON_FIELD_BRANCH_NAME).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_BRANCH_NAME), Status.BAD_REQUEST);
			}
	
			//validate variant name uniqueness within the experiment			
			String newName = variantJSON.getString(Constants.JSON_FIELD_NAME);			
			for (int i=0; i<experiment.getVariants().size(); i++) {
				Variant var = experiment.getVariants().get(i);
				if (uniqueId == null || !uniqueId.equals(var.getUniqueId())) { //in update - skip yourself
					if (var.getName().equals(newName)) {
						return new ValidationResults("A variant with the specified name already exists in the experiment.", Status.BAD_REQUEST);
					}
				}
			}
			
			String branchName = variantJSON.getString(Constants.JSON_FIELD_BRANCH_NAME);
			//validate the such branch exists in all season participating in the experiment
			ArrayList<Season> seasonsInExp = prod.getSeasonsWithinRange(expMinVersion, expMaxVersion);
			
			if (seasonsInExp.size()==0) {
				return new ValidationResults("No seasons is included in the experiment. Therefore you cannot choose '" + branchName + "' for variant. ", Status.BAD_REQUEST);
			}
			
			//internalUserGroups - optional
			if (variantJSON.containsKey(Constants.JSON_FIELD_INTERNAL_USER_GROUPS) && variantJSON.get(Constants.JSON_FIELD_INTERNAL_USER_GROUPS) != null) {				
				JSONArray groupsArr = variantJSON.getJSONArray(Constants.JSON_FIELD_INTERNAL_USER_GROUPS); //validate that is String array value

				@SuppressWarnings("unchecked")
				Map<String,InternalUserGroups> groupsPerProductMap = (Map<String,InternalUserGroups>) context.getAttribute(Constants.USER_GROUPS_PER_PRODUCT_PARAM_NAME);
				InternalUserGroups userGroups = groupsPerProductMap.get(prod.getUniqueId().toString());

				//validate that specified groups actually exist
				for (int k=0; k<groupsArr.length(); k++) {
					if (!userGroups.getGroupsMap().containsKey(groupsArr.get(k))) {
						return new ValidationResults("The internalUserGroups value '" + groupsArr.get(k) + "' does not exist.", Status.BAD_REQUEST);
					}
				}
				
				//verify that there are no duplications in the user groups
				for(int j = 0; j < groupsArr.length(); j++){
				    for(int k = j+1; k < groupsArr.length(); k++){
				        if (groupsArr.get(j).equals(groupsArr.get(k))){
				        	return new ValidationResults("The internalUserGroups value '" + groupsArr.get(k) + "' appears more than once in the internalUserGroups list.", Status.BAD_REQUEST);
				        }
				    }
				}
			}

			if (!branchName.equals(Constants.MASTER_BRANCH_NAME)) {				
				for (Season s:seasonsInExp) {
					if (s.getBranches().getBranchByName(branchName) == null) {
						return new ValidationResults("Some version ranges that are included in the experiment do not contain '"+ branchName + "' branch.", Status.BAD_REQUEST);
					}					
				}
			}
						
			String stageStr = variantJSON.getString(Constants.JSON_FEATURE_FIELD_STAGE);
			Stage stageObj = Utilities.strToStage(stageStr); //I know it is fine - was validated in base
			
			if (stageObj.equals(Stage.PRODUCTION) && experiment.getStage().equals(Stage.DEVELOPMENT)) {
				return new ValidationResults("An experiment in the DEVELOPMENT stage cannot include a variant in the PRODUCTION stage.", Status.BAD_REQUEST);
			}
						
			//validate rule (basic rule validation is done in the BaseAnalyticsItem, now only validate rule context/str/util ...)
			ValidationResults res = validateRule (variantJSON.getJSONObject(Constants.JSON_FEATURE_FIELD_RULE), expMinVersion, stageObj, seasonsInExp, context, userInfo);
			if (res !=null)
				return res;
			
			//variantName cannot be "DEFAULT"
			if (newName.equalsIgnoreCase(RESERVED_NAME)) {
				return new ValidationResults(RESERVED_NAME + " is a reserved name. Enter a different name for the variant.", Status.BAD_REQUEST);
			}
			
			Action action = Action.ADD;
					
			
			if (variantJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && variantJSON.get(Constants.JSON_FIELD_UNIQUE_ID)!=null) {
				//if JSON contains uniqueId - update an existing feature otherwise create a new feature

				action = Action.UPDATE;				

			}
			
			if (action == Action.ADD) {
				if (stageObj.equals(Stage.PRODUCTION)) {
					if (!validateExpeimentQuota) {					
						return new ValidationResults("Variant added in the production stage", Status.OK); //need to validate exp analytics quota 
					}
					else {
						int updatedProdCount = experiment.getAnalyticsProductionCounterUponVariantUpdate(context, uniqueId, null, stageObj, branchName);						
							
						int experimentAnalyticsQuota = experiment.getQuota(context);
											
						if(updatedProdCount>experimentAnalyticsQuota) {
							return new ValidationResults("Failed to update variant. The maximum number of items in production to send to analytics for experiment " + name + " was exceeded. The maximum number allowed is " + experimentAnalyticsQuota + ". The update increased the number to " + updatedProdCount, Status.BAD_REQUEST);		
						}
					}
				}
			}
			else {
				//experiment id must exists and not be changed
				String expIdStr = variantJSON.getString(Constants.JSON_FIELD_EXPERIMENT_ID);
				if (!expIdStr.equals(experimentId.toString())) {
					return new ValidationResults(String.format(Strings.fieldCannotBeChangedDuringUpdate, Constants.JSON_FIELD_EXPERIMENT_ID), Status.BAD_REQUEST);
				}
				
				if (!newName.equals(name)) {
					return new ValidationResults(String.format(Strings.fieldCannotBeChangedDuringUpdate, "Variant name"), Status.BAD_REQUEST);
				}
				
				if ((stage.equals(Stage.DEVELOPMENT) && stageObj.equals(Stage.PRODUCTION)) ||
						stageObj.equals(Stage.PRODUCTION) && !branchName.equals(this.branchName)) {
					
					if (!validateExpeimentQuota) {					
						return new ValidationResults("Variant updated from dev to prod or variant in prod changed branch.", Status.OK); //need to validate exp analytics quota 
					}
					else {
						int updatedProdCount = experiment.getAnalyticsProductionCounterUponVariantUpdate(context, uniqueId, branchName, stageObj, null);
						
						int experimentAnalyticsQuota = experiment.getQuota(context);
						if(updatedProdCount>experimentAnalyticsQuota) {
							return new ValidationResults("Failed to update variant. The maximum number of items in production to send to analytics for experiment " + name + " was exceeded. The maximum number allowed is " + experimentAnalyticsQuota + ". The update increased the number to " + updatedProdCount, Status.BAD_REQUEST);		
						}
					}
				}
			 }
			
			
		} catch (JSONException jsne) {
			return new ValidationResults(jsne.getMessage(), Status.BAD_REQUEST);
		}
		return null;		
	}	
	
	public JSONObject toJson (OutputJSONMode mode, ServletContext context) throws JSONException {
		JSONObject res = super.toJson(mode);
		if (res == null) {
			// this can only happen in runtime_production mode when the item is in development stage
			return null;
		}
		
		res.put(Constants.JSON_FIELD_EXPERIMENT_ID, experimentId==null?null:experimentId.toString());				
		res.put(Constants.JSON_FIELD_BRANCH_NAME, branchName);
		
		if (mode.equals(OutputJSONMode.RUNTIME_DEVELOPMENT) || mode.equals(OutputJSONMode.RUNTIME_PRODUCTION)) {
			@SuppressWarnings("unchecked")
			Map<String, Experiment> experimentsDB = (Map<String, Experiment>)context.getAttribute(Constants.EXPERIMENTS_DB_PARAM_NAME);
			
			Experiment exp = experimentsDB.get(experimentId.toString());
			res.put(Constants.JSON_FIELD_EXPERIMENT_NAME, exp.getName());
		}
		return res;
	}

	public ValidationResults validateProductionDontChanged(JSONObject updatedVariantJSON) throws JSONException {
		ValidationResults res = super.validateProductionDontChanged(updatedVariantJSON, "variant");
		if (res!=null)
			return res;
		
		String err = "Unable to update the variant. Only a user with the Administrator or Product Lead role can change an variant that is in the production stage.";
		
		if (stage == Stage.PRODUCTION) {
			String updatedBranchName = updatedVariantJSON.getString(Constants.JSON_FIELD_BRANCH_NAME);
			if (!updatedBranchName.equals(branchName)) 
				return new ValidationResults(err, Status.UNAUTHORIZED);
		}		
		
		return null;
	}

	public String updateVariant(JSONObject updatedVariantJSON, ServletContext context) throws JSONException {
		String expUpdateDetails = super.updateStagedAnalyticsItem(updatedVariantJSON, context, "variant");

		boolean wasChanged = ((expUpdateDetails != null) &&  !expUpdateDetails.isEmpty());
		StringBuilder updateDetails = new StringBuilder(expUpdateDetails);

		if (updatedVariantJSON.containsKey(Constants.JSON_FIELD_BRANCH_NAME) && updatedVariantJSON.get(Constants.JSON_FIELD_BRANCH_NAME)!=null) {
			String updatedBranchName = updatedVariantJSON.getString(Constants.JSON_FIELD_BRANCH_NAME);
			if (!updatedBranchName.equals(branchName)) {
				updateDetails.append(" 'branchName' changed from " + branchName + " to " +  updatedBranchName + "\n");
				branchName = updatedBranchName;
				wasChanged = true;
			}
		}
		
		if (wasChanged) {
			lastModified = new Date();
		}
			
		return updateDetails.toString();
	}
	
	public void fromJSON(JSONObject input, ServletContext context) throws JSONException {
		super.fromJSON(input);
		
		if (input.containsKey(Constants.JSON_FIELD_EXPERIMENT_ID) && input.get(Constants.JSON_FIELD_EXPERIMENT_ID) != null) {
			String expIdStr = input.getString(Constants.JSON_FIELD_EXPERIMENT_ID);			
			experimentId = UUID.fromString(expIdStr);		
		}
		
		if (input.containsKey(Constants.JSON_FIELD_BRANCH_NAME) && input.get(Constants.JSON_FIELD_BRANCH_NAME) != null) {
			branchName = input.getString(Constants.JSON_FIELD_BRANCH_NAME);						
		}
	}

	public boolean isPublishToAnalyticsSvrRequired(JSONObject updatedVariantJSON) throws JSONException {
		//name
		String updatedName = updatedVariantJSON.getString(Constants.JSON_FIELD_NAME);
		if (!updatedName.equals(name)) 
			return true;
		
		//optional fields
		
		//description
		if (updatedVariantJSON.containsKey(Constants.JSON_FIELD_DESCRIPTION) &&  updatedVariantJSON.get(Constants.JSON_FIELD_DESCRIPTION)!=null) {			
			Object updatedDescription = updatedVariantJSON.get(Constants.JSON_FIELD_DESCRIPTION);		
			if (description == null) {
				return true;
			}
			else if (!description.equals(updatedDescription)) {
				return true;
			}
		}
		
		//displayName
		if (updatedVariantJSON.containsKey(Constants.JSON_FIELD_DISPLAY_NAME) &&  updatedVariantJSON.get(Constants.JSON_FIELD_DISPLAY_NAME)!=null) {			
			Object updatedDispName = updatedVariantJSON.get(Constants.JSON_FIELD_DISPLAY_NAME);		
			if (displayName == null) {
				return true;
			}
			else if (!displayName.equals(updatedDispName)) {
				return true;
			}
		}

		return false;
	}
}
