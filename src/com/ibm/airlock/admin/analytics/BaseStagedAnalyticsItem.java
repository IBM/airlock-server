package com.ibm.airlock.admin.analytics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response.Status;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Strings;
import com.ibm.airlock.Constants.Action;
import com.ibm.airlock.Constants.OutputJSONMode;
import com.ibm.airlock.Constants.Stage;
import com.ibm.airlock.admin.authentication.UserInfo;
import com.ibm.airlock.admin.Rule;
import com.ibm.airlock.admin.Season;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.ValidationCache;
import com.ibm.airlock.admin.ValidationResults;

public abstract class BaseStagedAnalyticsItem extends BaseAnalyticsItem {
	public BaseStagedAnalyticsItem() {
		super();		
	}

	protected Stage stage = null; //c+u
	protected UUID uniqueId = null; //nc + u
	protected String name = null; //c+u
	protected String description = null; //opt in c+u (if missing or null in update don't change)
	protected String displayName = null; //opt in c+u (if missing or null in update don't change)	
	protected Boolean enabled = null; //required in create and update
	protected Date creationDate = null; //nc + u (not changed)
	private String creator = null;	//c+u (creator not changed)
	protected String[] internalUserGroups = null; //opt in creation + in update if missing or null ignore , if empty array then emptying
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public Stage getStage() {
		return stage;
	}
	public void setStage(Stage stage) {
		this.stage = stage;
	}
	public UUID getUniqueId() {
		return uniqueId;
	}
	public void setUniqueId(UUID uniqueId) {
		this.uniqueId = uniqueId;
	}
	public Boolean getEnabled() {
		return enabled;
	}	
	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
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
	public String[] getInternalUserGroups() {
		return internalUserGroups;
	}
	public void setInternalUserGroups(String[] internalUserGroups) {
		this.internalUserGroups = internalUserGroups;
	}
	public String getDisplayName() {
		return displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}


	public void fromJSON(JSONObject input) throws JSONException {
		super.fromJSON(input);
		
		if (input.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && input.get(Constants.JSON_FIELD_UNIQUE_ID) != null) {
			String sStr = input.getString(Constants.JSON_FIELD_UNIQUE_ID);			
			uniqueId = UUID.fromString(sStr);		
		}
		
		if (input.containsKey(Constants.JSON_FEATURE_FIELD_STAGE) && input.get(Constants.JSON_FEATURE_FIELD_STAGE)!=null)
			stage = Utilities.strToStage(input.getString(Constants.JSON_FEATURE_FIELD_STAGE));
		
		if (input.containsKey(Constants.JSON_FIELD_NAME) && input.get(Constants.JSON_FIELD_NAME)!=null) 
			name = input.getString(Constants.JSON_FIELD_NAME);			

		if (input.containsKey(Constants.JSON_FEATURE_FIELD_ENABLED) && input.get(Constants.JSON_FEATURE_FIELD_ENABLED)!=null)
			enabled = input.getBoolean(Constants.JSON_FEATURE_FIELD_ENABLED);
		
		if (input.containsKey(Constants.JSON_FIELD_DESCRIPTION) && input.get(Constants.JSON_FIELD_DESCRIPTION)!=null) 
			description = input.getString(Constants.JSON_FIELD_DESCRIPTION).trim();
		
		if (input.containsKey(Constants.JSON_FIELD_DISPLAY_NAME) && input.get(Constants.JSON_FIELD_DISPLAY_NAME)!=null) 
			displayName = input.getString(Constants.JSON_FIELD_DISPLAY_NAME).trim();
		
		if (input.containsKey(Constants.JSON_FEATURE_FIELD_CREATOR) && input.get(Constants.JSON_FEATURE_FIELD_CREATOR)!=null)
			creator = input.getString(Constants.JSON_FEATURE_FIELD_CREATOR).trim();
		
		if (input.containsKey(Constants.JSON_FEATURE_FIELD_CREATION_DATE) && input.get(Constants.JSON_FEATURE_FIELD_CREATION_DATE)!=null) { 
			long timeInMS = input.getLong(Constants.JSON_FEATURE_FIELD_CREATION_DATE);
			creationDate = new Date(timeInMS);			
		} else {
			creationDate = new Date();
		}
		
		if (input.containsKey(Constants.JSON_FIELD_INTERNAL_USER_GROUPS) && input.get(Constants.JSON_FIELD_INTERNAL_USER_GROUPS)!=null) 
			internalUserGroups = Utilities.jsonArrToStringArr(input.getJSONArray(Constants.JSON_FIELD_INTERNAL_USER_GROUPS));						

	}
	
	public JSONObject toJson (OutputJSONMode mode) throws JSONException {
		JSONObject res = super.toJson(mode);
		if (res == null) {
			// this can only happen in runtime_production mode when the item is in development stage
			return null;
		}
		
		res.put(Constants.JSON_FIELD_UNIQUE_ID, uniqueId==null?null:uniqueId.toString());
		res.put(Constants.JSON_FEATURE_FIELD_STAGE, stage.toString());
		res.put(Constants.JSON_FIELD_NAME, name);
		res.put(Constants.JSON_FEATURE_FIELD_ENABLED, enabled);
		res.put(Constants.JSON_FIELD_INTERNAL_USER_GROUPS, internalUserGroups);
		
		if (mode.equals(OutputJSONMode.ADMIN) || mode.equals(OutputJSONMode.DISPLAY)) {
			res.put(Constants.JSON_FIELD_DESCRIPTION, description);
			res.put(Constants.JSON_FIELD_DISPLAY_NAME, displayName);			
			res.put(Constants.JSON_FEATURE_FIELD_CREATION_DATE, creationDate.getTime()); 			
			res.put(Constants.JSON_FEATURE_FIELD_CREATOR, creator);
		}
		
		return res;
	}
	
	public String updateStagedAnalyticsItem (JSONObject updatedAnalyticsItemJSON, ServletContext context, String objectType) throws JSONException {
		//creator, creationDate should not be updated
		
		String dataALItemUpdateDetails = super.updateAnalyticsItem(updatedAnalyticsItemJSON, context);

		boolean wasChanged = ((dataALItemUpdateDetails != null) &&  !dataALItemUpdateDetails.isEmpty());
		StringBuilder updateDetails = new StringBuilder(dataALItemUpdateDetails);

		//stage		
		Stage updatedStage = Utilities.strToStage(updatedAnalyticsItemJSON.getString(Constants.JSON_FEATURE_FIELD_STAGE));
		if (updatedStage != stage) {
			updateDetails.append(" 'stage' changed from " + stage + " to " + updatedStage + "\n");
			stage = updatedStage;
			wasChanged = true;
		}
		
		//name
		String updatedName = updatedAnalyticsItemJSON.getString(Constants.JSON_FIELD_NAME);
		if (!updatedName.equals(name)) {
			updateDetails.append(" 'name' changed from " + name + " to " + updatedName + "\n");
			name = updatedName;
			wasChanged = true;
		}		

		//enabled
		Boolean updatedEnabled = updatedAnalyticsItemJSON.getBoolean(Constants.JSON_FEATURE_FIELD_ENABLED);
		if (enabled != updatedEnabled) {
			updateDetails.append("'enabled' changed from " + enabled + " to " + updatedEnabled + "\n");
			enabled  = updatedEnabled;
			wasChanged = true;			
		}	

		//optional fields
		if (updatedAnalyticsItemJSON.containsKey(Constants.JSON_FIELD_DESCRIPTION) &&  updatedAnalyticsItemJSON.get(Constants.JSON_FIELD_DESCRIPTION)!=null) {
			//if missing from json or null - ignore
			String updatedDescription = updatedAnalyticsItemJSON.getString(Constants.JSON_FIELD_DESCRIPTION);
			if (description == null || !description.equals(updatedDescription)) {
				updateDetails.append(" 'description' changed from '" + description + "' to '" + updatedDescription + "'\n");
				description  = updatedDescription;
				wasChanged = true;
			}
		}	
		
		if (updatedAnalyticsItemJSON.containsKey(Constants.JSON_FIELD_DISPLAY_NAME) &&  updatedAnalyticsItemJSON.get(Constants.JSON_FIELD_DISPLAY_NAME)!=null) {
			//if missing from json or null - ignore
			String updatedDisplayName = updatedAnalyticsItemJSON.getString(Constants.JSON_FIELD_DISPLAY_NAME);
			if (displayName == null || !displayName.equals(updatedDisplayName)) {
				updateDetails.append(" 'displayName' changed from '" + displayName + "' to '" + updatedDisplayName + "'\n");
				displayName = updatedDisplayName;
				wasChanged = true;
			}
		}	
		
		if (updatedAnalyticsItemJSON.containsKey(Constants.JSON_FIELD_INTERNAL_USER_GROUPS) && updatedAnalyticsItemJSON.get(Constants.JSON_FIELD_INTERNAL_USER_GROUPS)!=null) {
			JSONArray updatedInternalUserGroups = updatedAnalyticsItemJSON.getJSONArray(Constants.JSON_FIELD_INTERNAL_USER_GROUPS);
			if (internalUserGroups == null || !Utilities.stringArrayCompareIgnoreOrder(updatedInternalUserGroups,internalUserGroups)) {
				updateDetails.append("'internalUserGroups' changed from " + Arrays.toString(internalUserGroups) + " to " +  Arrays.toString(Utilities.jsonArrToStringArr(updatedInternalUserGroups)) + "\n");
				internalUserGroups = Utilities.jsonArrToStringArr(updatedInternalUserGroups);
				wasChanged = true;
			}
		}
		if (wasChanged) {
			lastModified = new Date();
		}
		
		return updateDetails.toString();
	}		
	
	public ValidationResults validateStagedAnalyticsItemJSON(JSONObject analyticsItemObj, ServletContext context, UserInfo userInfo) {
		
		try {
			Action action = Action.ADD;
			
			if (analyticsItemObj.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && analyticsItemObj.get(Constants.JSON_FIELD_UNIQUE_ID)!=null) {
				//if JSON contains uniqueId - update an existing feature otherwise create a new feature
				action = Action.UPDATE;
			}
			
			ValidationResults superRes = super.validateBaseAnalyticsItemJSON(analyticsItemObj, context, userInfo, action);
			if (superRes!=null)
				return superRes;
			
			Stage stageObj = null;

			//name
			if (!analyticsItemObj.containsKey(Constants.JSON_FIELD_NAME) || analyticsItemObj.getString(Constants.JSON_FIELD_NAME) == null || analyticsItemObj.getString(Constants.JSON_FIELD_NAME).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_NAME), Status.BAD_REQUEST);
			}
			
			String objName = analyticsItemObj.getString(Constants.JSON_FIELD_NAME);
			String validateNameErr = Utilities.validateName(objName);
			if(validateNameErr!=null) {
				return new ValidationResults(validateNameErr, Status.BAD_REQUEST);
			}

			//stage
			if (!analyticsItemObj.containsKey(Constants.JSON_FEATURE_FIELD_STAGE) || analyticsItemObj.getString(Constants.JSON_FEATURE_FIELD_STAGE) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_STAGE), Status.BAD_REQUEST);					
			}			

			String stageStr = analyticsItemObj.getString(Constants.JSON_FEATURE_FIELD_STAGE);
			stageObj = Utilities.strToStage(stageStr);
			if (stageObj == null) {
				return new ValidationResults("Illegal stage: '" + stageStr + "'", Status.BAD_REQUEST);
			}		
			
			//enabled
			if (!analyticsItemObj.containsKey(Constants.JSON_FEATURE_FIELD_ENABLED) || analyticsItemObj.get(Constants.JSON_FEATURE_FIELD_ENABLED) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_ENABLED), Status.BAD_REQUEST);					
			}

			analyticsItemObj.getBoolean(Constants.JSON_FEATURE_FIELD_ENABLED); //validate that is boolean value
			
			//creator
			if (!analyticsItemObj.containsKey(Constants.JSON_FEATURE_FIELD_CREATOR) || analyticsItemObj.getString(Constants.JSON_FEATURE_FIELD_CREATOR) == null || analyticsItemObj.getString(Constants.JSON_FEATURE_FIELD_CREATOR).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_CREATOR), Status.BAD_REQUEST);
			}	
			
			//internal user groups is validated in the derived classes since here we dont have the product/season
			
			//description and displayName is an optional fields
			if (action == Action.ADD){
				//experiment in production can be added only by Administrator or ProductLead
				if (stageObj.equals(Stage.PRODUCTION) && !validRole(userInfo)) {
					return new ValidationResults("Unable to add the experiment. Only a user with the Administrator or Product Lead role can add experiments and variants in the production stage.", Status.UNAUTHORIZED);					
				}
				
				//creation date => should not appear in branch creation
				if (analyticsItemObj.containsKey(Constants.JSON_FEATURE_FIELD_CREATION_DATE) && analyticsItemObj.get(Constants.JSON_FEATURE_FIELD_CREATION_DATE)!=null) {
					return new ValidationResults("The creationDate field cannot be specified during creation.", Status.BAD_REQUEST);
				}
			}
			else { //UPDATE
				//if experiment is in production or is updated from stage DEVELOPMENT to PRODUCTION
				if (stage.equals(Stage.PRODUCTION) || stageObj.equals(Stage.PRODUCTION)) {						
					//only productLead or Administrator can update experiment in production
					if (!validRole(userInfo)) {
						return new ValidationResults("Unable to update. Only a user with the Administrator or Product Lead role can update an experiment or variant in the production stage.", Status.UNAUTHORIZED);
					}
					
					//name of feature in production cannot be changed
					if (!objName.equals(name)) {
						return new ValidationResults("You cannot change the name of an experiment or variant in the production stage.", Status.UNAUTHORIZED);
					}
				}
								
				//creation date must appear
				if (!analyticsItemObj.containsKey(Constants.JSON_FEATURE_FIELD_CREATION_DATE) || analyticsItemObj.get(Constants.JSON_FEATURE_FIELD_CREATION_DATE)==null) {
					return new ValidationResults("The creationDate field is missing. This field must be specified during update.", Status.BAD_REQUEST);
				}
				
				//verify that legal long
				long creationdateLong = analyticsItemObj.getLong(Constants.JSON_FEATURE_FIELD_CREATION_DATE);
				
				//verify that was not changed
				if (!creationDate.equals(new Date(creationdateLong))) {
					return new ValidationResults("creationDate cannot be changed during update", Status.BAD_REQUEST);
				}
				
				//creator must exist and not be changed
				String creatorStr = analyticsItemObj.getString(Constants.JSON_FEATURE_FIELD_CREATOR);
				if (!creator.equals(creatorStr)) {
					return new ValidationResults(String.format(Strings.fieldCannotBeChangedDuringUpdate, Constants.JSON_FEATURE_FIELD_CREATOR), Status.BAD_REQUEST);
				}
			}
		} catch (JSONException jsne) {
			return new ValidationResults(jsne.getMessage(), Status.BAD_REQUEST);
		}
		return null;
	}
	static boolean validRole(UserInfo userInfo)
	{

		return userInfo == null || userInfo.getRoles().contains(Constants.RoleType.Administrator) || userInfo.getRoles().contains(Constants.RoleType.ProductLead);

	}
	//objectType can be variant or experiment
	public ValidationResults validateProductionDontChanged(JSONObject updatedAnalyticsItemJSON, String objectType) throws JSONException {
		
		Stage updatedStage = Utilities.strToStage(updatedAnalyticsItemJSON.getString(Constants.JSON_FEATURE_FIELD_STAGE));
		if (updatedStage  == Stage.PRODUCTION && stage == Stage.DEVELOPMENT) {
			return new ValidationResults("Unable to update the " + objectType + ". Only a user with the Administrator or Product Lead can change " + objectType + " from the development to the production stage.", Status.UNAUTHORIZED);	
		}			

		String err = "Unable to update the " + objectType + ". Only a user with the Administrator or Product Lead role can change " + objectType + " that is in the production stage.";
		
		if (stage == Stage.PRODUCTION) {
			String updatedName = updatedAnalyticsItemJSON.getString(Constants.JSON_FIELD_NAME);
			if (!updatedName.equals(name)) 
				return new ValidationResults(err, Status.UNAUTHORIZED);
			
			int updatedRolloutPercentage = updatedAnalyticsItemJSON.getInt(Constants.JSON_FEATURE_FIELD_PERCENTAGE);
			if (rolloutPercentage  != updatedRolloutPercentage) 
				return new ValidationResults(err, Status.UNAUTHORIZED);

			JSONObject updatedRuleJSON = updatedAnalyticsItemJSON.getJSONObject(Constants.JSON_FEATURE_FIELD_RULE);
			Rule updatedRule = new Rule();
			updatedRule.fromJSON(updatedRuleJSON);
			if (!rule.equals(updatedRule)) {
				return new ValidationResults(err, Status.UNAUTHORIZED);
			}
			
			Boolean updatedEnabled = updatedAnalyticsItemJSON.getBoolean(Constants.JSON_FEATURE_FIELD_ENABLED);
			if (enabled != updatedEnabled) 
				return new ValidationResults(err, Status.UNAUTHORIZED);			
			
			//optional fields
			if (updatedAnalyticsItemJSON.containsKey(Constants.JSON_FIELD_DESCRIPTION) &&  updatedAnalyticsItemJSON.get(Constants.JSON_FIELD_DESCRIPTION)!=null) {
				//if missing from json or null - ignore
				String updatedDescription = updatedAnalyticsItemJSON.getString(Constants.JSON_FIELD_DESCRIPTION);
				if (description == null || !description.equals(updatedDescription)) {
					return new ValidationResults(err, Status.UNAUTHORIZED);				
				}
			}
			
			if (updatedAnalyticsItemJSON.containsKey(Constants.JSON_FIELD_DISPLAY_NAME) &&  updatedAnalyticsItemJSON.get(Constants.JSON_FIELD_DISPLAY_NAME)!=null) {
				//if missing from json or null - ignore
				String updatedDisplayName = updatedAnalyticsItemJSON.getString(Constants.JSON_FIELD_DISPLAY_NAME);
				if (displayName == null || !displayName.equals(updatedDisplayName)) {
					return new ValidationResults(err, Status.UNAUTHORIZED);				
				}
			}
		}
				
		return null;		
	}	
	
	protected ValidationResults validateRule(JSONObject ruleJson, String minVersion, Stage stage, ArrayList<Season> seasonsInExp, ServletContext context, UserInfo userInfo) throws JSONException {
		Rule tmpRule = new Rule();
		tmpRule.fromJSON(ruleJson);

		ValidationCache tester = new ValidationCache();
		for (Season s:seasonsInExp) {
			ValidationResults res = tmpRule.validateRule(stage, minVersion, s, context, tester, userInfo);

			if (res !=null) {
				res.error = "In version range (" + s.getMinVersion() + "-" + s.getMaxVersion()  +") that is included in the experiment: " + res.error;
				return res;
			}
		}
		
		return null;
	}
	
}
