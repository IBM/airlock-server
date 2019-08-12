package com.ibm.airlock.admin.notifications;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;
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
import com.ibm.airlock.admin.GenerationException;
import com.ibm.airlock.admin.InternalUserGroups;
import com.ibm.airlock.admin.Product;
import com.ibm.airlock.admin.Rule;
import com.ibm.airlock.admin.SchemaValidator;
import com.ibm.airlock.admin.Season;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.ValidationCache;
import com.ibm.airlock.admin.ValidationException;
import com.ibm.airlock.admin.ValidationResults;
import com.ibm.airlock.engine.VerifyRule;
public class AirlockNotification{
	private Stage stage = null; //c+u
	private UUID uniqueId = null; //nc + u
	private String name = null; //c+u
	private String description = null; //opt in c+u (if missing or null in update don't change)
	private String displayName = null; //opt in c+u (if missing or null in update don't change)
	private Boolean enabled = null; //required in create and update
	private Date creationDate = null; //nc + u (not changed)
	private String creator = null;	//c+u (creator not changed)
	private String[] internalUserGroups = null; //opt in creation + in update if missing or null ignore , if empty array then emptying
	private Double rolloutPercentage = null; //c+u
	private Date lastModified = null; // required in update. forbidden in create
	private String minAppVersion = null; //c+u
	private UUID seasonId = null; //c+u
	private String owner = null; //like desc (not in runtime)
	private Rule cancellationRule;//c+u
	private Rule registrationRule;//c+u
	private String configuration = null; //c+u
	private int maxNotifications; //c+u
	private int minInterval; //c+u

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
	public String getDisplayName() {
		return displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
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
	public Double getRolloutPercentage() {
		return rolloutPercentage;
	}
	public void setRolloutPercentage(Double rolloutPercentage) {
		this.rolloutPercentage = rolloutPercentage;
	}
	public Date getLastModified() {
		return lastModified;
	}
	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}
	public String getMinAppVersion() {
		return minAppVersion;
	}
	public void setMinAppVersion(String minAppVersion) {
		this.minAppVersion = minAppVersion;
	}
	public UUID getSeasonId() {
		return seasonId;
	}
	public void setSeasonId(UUID seasonId) {
		this.seasonId = seasonId;
	}
	public String getOwner() {
		return owner;
	}
	public void setOwner(String owner) {
		this.owner = owner;
	}
	public Rule getRegistrationRule() {
		return registrationRule;
	}
	public void setRegistrationRule(Rule registrationRule) {
		this.registrationRule = registrationRule;
	}
	public Rule getCancellationRule() {
		return cancellationRule;
	}
	public void setCancellationRule(Rule cancellationRule) {
		this.cancellationRule = cancellationRule;
	}
	public String getConfiguration() {
		return configuration;
	}
	public void setConfiguration(String configuration) {
		this.configuration = configuration;
	}

	public int getMaxNotifications() {
		return maxNotifications;
	}
	public void setMaxNotifications(int maxNotifications) {
		this.maxNotifications = maxNotifications;
	}
	public int getMinInterval() {
		return minInterval;
	}
	public void setMinInterval(int minInterval) {
		this.minInterval = minInterval;
	}
	public JSONObject toJson(OutputJSONMode outputMode) throws JSONException {

		JSONObject res = new JSONObject();

		res.put(Constants.JSON_FIELD_UNIQUE_ID, uniqueId==null?null:uniqueId.toString());		
		res.put(Constants.JSON_FIELD_NAME, name);
		res.put(Constants.JSON_FEATURE_FIELD_CONFIGURATION, configuration);
		res.put(Constants.JSON_FIELD_CANCELLATION_RULE, cancellationRule.toJson(OutputJSONMode.ADMIN));
		res.put(Constants.JSON_FIELD_REGISTRATION_RULE, registrationRule.toJson(OutputJSONMode.ADMIN));	
		res.put(Constants.JSON_FEATURE_FIELD_ENABLED, enabled);
		res.put(Constants.JSON_FIELD_INTERNAL_USER_GROUPS, internalUserGroups);		
		res.put(Constants.JSON_FEATURE_FIELD_PERCENTAGE, rolloutPercentage);
		res.put(Constants.JSON_FEATURE_FIELD_MIN_APP_VER, minAppVersion);		
		res.put(Constants.JSON_FEATURE_FIELD_STAGE, stage.toString());
		res.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, seasonId==null?null:seasonId.toString());
		res.put(Constants.JSON_FIELD_MIN_INTERVAL, minInterval);
		res.put(Constants.JSON_FIELD_MAX_NOTIFICATIONS, maxNotifications);

		if (outputMode.equals(OutputJSONMode.ADMIN) || outputMode.equals(OutputJSONMode.DISPLAY)) {
			res.put(Constants.JSON_FIELD_DESCRIPTION, description);
			res.put(Constants.JSON_FIELD_DISPLAY_NAME, displayName);
			res.put(Constants.JSON_FEATURE_FIELD_OWNER, owner);
			res.put(Constants.JSON_FEATURE_FIELD_CREATION_DATE, creationDate.getTime()); 			
			res.put(Constants.JSON_FEATURE_FIELD_CREATOR, creator);
			res.put(Constants.JSON_FIELD_LAST_MODIFIED, lastModified.getTime());						
		}				

		return res;
	}

	public void fromJSON(JSONObject input) throws JSONException {
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

		if (input.containsKey(Constants.JSON_FEATURE_FIELD_OWNER) && input.get(Constants.JSON_FEATURE_FIELD_OWNER)!=null) 
			owner = input.getString(Constants.JSON_FEATURE_FIELD_OWNER).trim();

		if (input.containsKey(Constants.JSON_FEATURE_FIELD_CREATOR) && input.get(Constants.JSON_FEATURE_FIELD_CREATOR)!=null)
			creator = input.getString(Constants.JSON_FEATURE_FIELD_CREATOR).trim();

		if (input.containsKey(Constants.JSON_FEATURE_FIELD_MIN_APP_VER) && input.get(Constants.JSON_FEATURE_FIELD_MIN_APP_VER)!=null) 
			minAppVersion = input.getString(Constants.JSON_FEATURE_FIELD_MIN_APP_VER);						

		if (input.containsKey(Constants.JSON_FEATURE_FIELD_CREATION_DATE) && input.get(Constants.JSON_FEATURE_FIELD_CREATION_DATE)!=null) { 
			long timeInMS = input.getLong(Constants.JSON_FEATURE_FIELD_CREATION_DATE);
			creationDate = new Date(timeInMS);			
		} else {
			creationDate = new Date();
		}

		if (input.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) && input.get(Constants.JSON_FIELD_LAST_MODIFIED)!=null) { 
			long timeInMS = input.getLong(Constants.JSON_FIELD_LAST_MODIFIED);
			lastModified = new Date(timeInMS);
		}  else {
			lastModified = new Date();
		}

		if (input.containsKey(Constants.JSON_FIELD_INTERNAL_USER_GROUPS) && input.get(Constants.JSON_FIELD_INTERNAL_USER_GROUPS)!=null) 
			internalUserGroups = Utilities.jsonArrToStringArr(input.getJSONArray(Constants.JSON_FIELD_INTERNAL_USER_GROUPS));

		if (input.containsKey(Constants.JSON_FEATURE_FIELD_PERCENTAGE) && input.get(Constants.JSON_FEATURE_FIELD_PERCENTAGE)!=null) 
			rolloutPercentage = input.getDouble(Constants.JSON_FEATURE_FIELD_PERCENTAGE);

		if (input.containsKey(Constants.JSON_FEATURE_FIELD_SEASON_ID) && input.get(Constants.JSON_FEATURE_FIELD_SEASON_ID) != null) {
			String sStr = input.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID);			
			seasonId = UUID.fromString(sStr);			
		}		

		if (input.containsKey(Constants.JSON_FEATURE_FIELD_CONFIGURATION) && input.get(Constants.JSON_FEATURE_FIELD_CONFIGURATION) != null) {
			configuration = input.getString(Constants.JSON_FEATURE_FIELD_CONFIGURATION);						
		}			

		if (input.containsKey(Constants.JSON_FIELD_CANCELLATION_RULE) && input.get(Constants.JSON_FIELD_CANCELLATION_RULE)!=null) {
			cancellationRule = new Rule();
			cancellationRule.fromJSON(input.getJSONObject(Constants.JSON_FIELD_CANCELLATION_RULE));
		}

		if (input.containsKey(Constants.JSON_FIELD_REGISTRATION_RULE) && input.get(Constants.JSON_FIELD_REGISTRATION_RULE)!=null) {
			registrationRule = new Rule();
			registrationRule.fromJSON(input.getJSONObject(Constants.JSON_FIELD_REGISTRATION_RULE));
		}
		
		if (input.containsKey(Constants.JSON_FIELD_MAX_NOTIFICATIONS) && input.get(Constants.JSON_FIELD_MAX_NOTIFICATIONS)!=null) 
			maxNotifications = input.getInt(Constants.JSON_FIELD_MAX_NOTIFICATIONS);
		
		if (input.containsKey(Constants.JSON_FIELD_MIN_INTERVAL) && input.get(Constants.JSON_FIELD_MIN_INTERVAL)!=null) 
			minInterval = input.getInt(Constants.JSON_FIELD_MIN_INTERVAL);
	}

	public ValidationResults validateNotificationJSON(JSONObject notificationJSON, ServletContext context, UserInfo userInfo, String seasonId) {

		@SuppressWarnings("unchecked")
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);
		Season season = seasonsDB.get(seasonId);
		
		try {
			Action action = Action.ADD;

			if (notificationJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && notificationJSON.get(Constants.JSON_FIELD_UNIQUE_ID)!=null) {
				//if JSON contains uniqueId - update an existing feature otherwise create a new feature
				action = Action.UPDATE;
			}


			Stage stageObj = null;

			//name
			if (!notificationJSON.containsKey(Constants.JSON_FIELD_NAME) || notificationJSON.getString(Constants.JSON_FIELD_NAME) == null || notificationJSON.getString(Constants.JSON_FIELD_NAME).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_NAME), Status.BAD_REQUEST);
			}

			String objName = notificationJSON.getString(Constants.JSON_FIELD_NAME);
			String validateNameErr = Utilities.validateName(objName);
			if(validateNameErr!=null) {
				return new ValidationResults(validateNameErr, Status.BAD_REQUEST);
			}

			//validate name uniqueness within season			
			String uid = notificationJSON.optString(Constants.JSON_FIELD_UNIQUE_ID);
			ValidationResults res = validateNotificationNameUniquness (season, objName, uid);
			if (res!=null)
				return res;			

			//enabled
			if (!notificationJSON.containsKey(Constants.JSON_FEATURE_FIELD_ENABLED) || notificationJSON.get(Constants.JSON_FEATURE_FIELD_ENABLED) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_ENABLED), Status.BAD_REQUEST);					
			}

			notificationJSON.getBoolean(Constants.JSON_FEATURE_FIELD_ENABLED); //validate that is boolean value

			//stage
			if (!notificationJSON.containsKey(Constants.JSON_FEATURE_FIELD_STAGE) || notificationJSON.getString(Constants.JSON_FEATURE_FIELD_STAGE) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_STAGE), Status.BAD_REQUEST);					
			}			

			String stageStr = notificationJSON.getString(Constants.JSON_FEATURE_FIELD_STAGE);
			stageObj = Utilities.strToStage(stageStr);
			if (stageObj == null) {
				return new ValidationResults("Illegal stage: '" + stageStr + "'", Status.BAD_REQUEST);
			}		

			//minAppVersion
			if (!notificationJSON.containsKey(Constants.JSON_FEATURE_FIELD_MIN_APP_VER) || notificationJSON.get(Constants.JSON_FEATURE_FIELD_MIN_APP_VER) == null || notificationJSON.getString(Constants.JSON_FEATURE_FIELD_MIN_APP_VER).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_MIN_APP_VER), Status.BAD_REQUEST);					
			}									 					

			String minAppVer = notificationJSON.getString(Constants.JSON_FEATURE_FIELD_MIN_APP_VER);
			ValidationCache tester = new ValidationCache();									

			//registrationRule 
			if (!notificationJSON.containsKey(Constants.JSON_FIELD_REGISTRATION_RULE) || notificationJSON.getString(Constants.JSON_FIELD_REGISTRATION_RULE) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_REGISTRATION_RULE), Status.BAD_REQUEST);
			}

			notificationJSON.getJSONObject(Constants.JSON_FIELD_REGISTRATION_RULE); //validate legal json
			Rule tmpRegistrationRule = new Rule();
			tmpRegistrationRule.fromJSON(notificationJSON.getJSONObject(Constants.JSON_FIELD_REGISTRATION_RULE));

			res = tmpRegistrationRule.validateRule(stageObj, minAppVer, season, context, tester, userInfo);
			if (res !=null) {
				return res;
			} 	
			
			//configuration - mandatory field
			if (!notificationJSON.containsKey(Constants.JSON_FEATURE_FIELD_CONFIGURATION) || notificationJSON.get(Constants.JSON_FEATURE_FIELD_CONFIGURATION) == null || notificationJSON.getString(Constants.JSON_FEATURE_FIELD_CONFIGURATION).isEmpty()) 
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_CONFIGURATION), Status.BAD_REQUEST);				

			String configurationStr = notificationJSON.getString(Constants.JSON_FEATURE_FIELD_CONFIGURATION); //validate that is json value			

			//validate configuration
			JSONObject configJsonObj = null;
			try {
				ValidationCache.Info info = tester.getInfo(context, season, stageObj, minAppVer);
				configJsonObj = VerifyRule.fullConfigurationEvaluation(tmpRegistrationRule.getRuleString(), configurationStr, info.minimalInvoker, info.maximalInvoker);

				//validate that configuration match the schema
		
				String ajv = (String)context.getAttribute(Constants.SCHEMA_VALIDATOR_AJV_PARAM_NAME);
				String validator = (String)context.getAttribute(Constants.SCHEMA_VALIDATOR_VALIDATOR_PARAM_NAME);				

				boolean relaxed = false;
				SchemaValidator.validation(validator, ajv, season.getNotifications().getConfigurationSchema().toString(), configJsonObj.toString(), relaxed);
			} catch (ValidationException e) {
				return new ValidationResults("Validation error: " + e.getMessage(), Status.BAD_REQUEST);		
			}
			
			//cancellationRule 
			if (!notificationJSON.containsKey(Constants.JSON_FIELD_CANCELLATION_RULE) || notificationJSON.getString(Constants.JSON_FIELD_CANCELLATION_RULE) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_CANCELLATION_RULE), Status.BAD_REQUEST);
			}

			notificationJSON.getJSONObject(Constants.JSON_FIELD_CANCELLATION_RULE); //validate legal json
			Rule tmpRule = new Rule();
			tmpRule.fromJSON(notificationJSON.getJSONObject(Constants.JSON_FIELD_CANCELLATION_RULE));

			res = tmpRule.validateRuleWithAdditionalContext(stageObj, minAppVer, season, context, tester, configJsonObj.toString(), userInfo);
			if (res !=null) {
				return res;
			} 	
			
			//rolloutPercentage
			if (!notificationJSON.containsKey(Constants.JSON_FEATURE_FIELD_PERCENTAGE) || notificationJSON.get(Constants.JSON_FEATURE_FIELD_PERCENTAGE) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_PERCENTAGE), Status.BAD_REQUEST);
			}

			Double rp = notificationJSON.getDouble(Constants.JSON_FEATURE_FIELD_PERCENTAGE); //validate that is legal double
			if (rp<0 || rp>100) {
				return new ValidationResults("Illegal Rollout Percentage value. Should be a double between 0 and 100.", Status.BAD_REQUEST);
			}
			
			//maxNotifications
			if (!notificationJSON.containsKey(Constants.JSON_FIELD_MAX_NOTIFICATIONS) || notificationJSON.get(Constants.JSON_FIELD_MAX_NOTIFICATIONS) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_MAX_NOTIFICATIONS), Status.BAD_REQUEST);
			}

			int mn = notificationJSON.getInt(Constants.JSON_FIELD_MAX_NOTIFICATIONS); //validate that is legal int
			if (mn<-1) {
				return new ValidationResults("Illegal maxNotifications value. Should be an integer greater than -1.", Status.BAD_REQUEST);
			}

			
			//minInterval
			if (!notificationJSON.containsKey(Constants.JSON_FIELD_MIN_INTERVAL) || notificationJSON.get(Constants.JSON_FIELD_MIN_INTERVAL) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_MIN_INTERVAL), Status.BAD_REQUEST);
			}

			int mi = notificationJSON.getInt(Constants.JSON_FIELD_MIN_INTERVAL); //validate that is legal int
			if (mi<-1) {
				return new ValidationResults("Illegal minInterval value. Should be an integer greater than -1.", Status.BAD_REQUEST);
			}

			//creator
			if (!notificationJSON.containsKey(Constants.JSON_FEATURE_FIELD_CREATOR) || notificationJSON.getString(Constants.JSON_FEATURE_FIELD_CREATOR) == null || notificationJSON.getString(Constants.JSON_FEATURE_FIELD_CREATOR).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_CREATOR), Status.BAD_REQUEST);
			}	

			//season id
			if (!notificationJSON.containsKey(Constants.JSON_FEATURE_FIELD_SEASON_ID) || notificationJSON.get(Constants.JSON_FEATURE_FIELD_SEASON_ID)==null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_SEASON_ID), Status.BAD_REQUEST);
			}

			//internalUserGroups - optional
			if (notificationJSON.containsKey(Constants.JSON_FIELD_INTERNAL_USER_GROUPS) && notificationJSON.get(Constants.JSON_FIELD_INTERNAL_USER_GROUPS) != null) {				
				JSONArray groupsArr = notificationJSON.getJSONArray(Constants.JSON_FIELD_INTERNAL_USER_GROUPS); //validate that is String array value

				@SuppressWarnings("unchecked")
				Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);					

				Product product = productsDB.get(season.getProductId().toString());

				@SuppressWarnings("unchecked")
				Map<String,InternalUserGroups> groupsPerProductMap = (Map<String,InternalUserGroups>) context.getAttribute(Constants.USER_GROUPS_PER_PRODUCT_PARAM_NAME);
				InternalUserGroups userGroups = groupsPerProductMap.get(product.getUniqueId().toString());

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

			//description, displayName, owner are optional fields
			if (action == Action.ADD){
				//notification in production can be added only by Administrator or ProductLead
				if (stageObj.equals(Stage.PRODUCTION) && !validRole(userInfo)) {
					return new ValidationResults("Unable to add the notification. Only a user with the Administrator or Product Lead role can add notifications in the production stage.", Status.UNAUTHORIZED);					
				}

				//creation date => should not appear in branch creation
				if (notificationJSON.containsKey(Constants.JSON_FEATURE_FIELD_CREATION_DATE) && notificationJSON.get(Constants.JSON_FEATURE_FIELD_CREATION_DATE)!=null) {
					return new ValidationResults("The creationDate field cannot be specified during creation.", Status.BAD_REQUEST);
				}

				//verify that not higher than seasons max version
				//this is checked only in create - in update this is not checked since the seasons min-maxVersion may have been changed. 
				String minAppVersion = notificationJSON.getString(Constants.JSON_FEATURE_FIELD_MIN_APP_VER);
				String seasonMaxVer = seasonsDB.get(seasonId).getMaxVersion();
				if (seasonMaxVer!=null) {
					if (Season.compare (minAppVersion, seasonMaxVer) >=0) {
						return new ValidationResults("The notification's Minimum App Version must be less than the Maximum Version of the current version range.", Status.BAD_REQUEST);
					}
				}

				//modification date => should not appear in notification creation
				if (notificationJSON.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) && notificationJSON.get(Constants.JSON_FIELD_LAST_MODIFIED)!=null) {
					return new ValidationResults("The lastModified field cannot be specified during notification creation.", Status.BAD_REQUEST);
				}

			}
			else { //UPDATE
				//if notification is in production or is updated from stage DEVELOPMENT to PRODUCTION
				if (stage.equals(Stage.PRODUCTION) || stageObj.equals(Stage.PRODUCTION)) {						
					//only productLead or Administrator can update notification in production
					if (!validRole(userInfo)) {
						return new ValidationResults("Unable to update. Only a user with the Administrator or Product Lead role can update a notification in the production stage.", Status.UNAUTHORIZED);
					}

					//name of feature in production cannot be changed
					if (!objName.equals(name)) {
						return new ValidationResults("You cannot change the name of a notification in the production stage.", Status.UNAUTHORIZED);
					}
				}

				//creation date must appear
				if (!notificationJSON.containsKey(Constants.JSON_FEATURE_FIELD_CREATION_DATE) || notificationJSON.get(Constants.JSON_FEATURE_FIELD_CREATION_DATE)==null) {
					return new ValidationResults("The creationDate field is missing. This field must be specified during update.", Status.BAD_REQUEST);
				}

				//verify that legal long
				long creationdateLong = notificationJSON.getLong(Constants.JSON_FEATURE_FIELD_CREATION_DATE);

				//verify that was not changed
				if (!creationDate.equals(new Date(creationdateLong))) {
					return new ValidationResults("creationDate cannot be changed during update", Status.BAD_REQUEST);
				}

				//creator must exist and not be changed
				String creatorStr = notificationJSON.getString(Constants.JSON_FEATURE_FIELD_CREATOR);
				if (!creator.equals(creatorStr)) {
					return new ValidationResults(String.format(Strings.fieldCannotBeChangedDuringUpdate, Constants.JSON_FEATURE_FIELD_CREATOR), Status.BAD_REQUEST);
				}

				//modification date must appear
				if (!notificationJSON.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) || notificationJSON.get(Constants.JSON_FIELD_LAST_MODIFIED)==null) {
					return new ValidationResults("The lastModified field is missing. This field must be specified during notification update.", Status.BAD_REQUEST);
				}

				//verify that given modification date is not older than current modification date
				long givenModoficationDate = notificationJSON.getLong(Constants.JSON_FIELD_LAST_MODIFIED);  //verify that legal long
				Date givenDate = new Date(givenModoficationDate);
				if (givenDate.before(lastModified)) {
					return new ValidationResults(String.format(Strings.itemChangedByAnotherUser, "notification"), Status.CONFLICT);			
				}

				//season id must exists and not be changed
				String seasonIdStr = notificationJSON.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID);
				if (!seasonIdStr.equals(seasonId)) {
					return new ValidationResults(String.format(Strings.fieldCannotBeChangedDuringUpdate, Constants.JSON_FEATURE_FIELD_SEASON_ID), Status.BAD_REQUEST);
				}								
			}			
		} catch (JSONException jsne) {
			return new ValidationResults(jsne.getMessage(), Status.BAD_REQUEST);
		} catch (GenerationException e) {
			return new ValidationResults("Failed to generate a data sample to validate the notification:" + e.getMessage(), Status.BAD_REQUEST);		
		}
		return null;

	}

	private boolean validRole(UserInfo userInfo) {
		return userInfo == null || userInfo.getRoles().contains(Constants.RoleType.Administrator) || userInfo.getRoles().contains(Constants.RoleType.ProductLead);
	}

	public AirlockNotification duplicateForNewSeason(String minVersion, UUID newSeasonId) {
		AirlockNotification res = new AirlockNotification();
		res.setUniqueId(UUID.randomUUID());
		res.setMinAppVersion(minVersion==null?minAppVersion:minVersion); 				 	
		res.setSeasonId(newSeasonId);
		res.setLastModified(lastModified);
		res.setStage(stage);
		res.setName(name);
		res.setDescription(description);
		res.setDisplayName(displayName);
		res.setEnabled(enabled);
		res.setCreationDate(creationDate);
		res.setCreator(creator);
		res.setInternalUserGroups(internalUserGroups == null ? null:internalUserGroups.clone());
		res.setRolloutPercentage(rolloutPercentage);
		res.setOwner(owner);

		res.setRegistrationRule(registrationRule == null?null:Rule.duplicteForNextSeason(registrationRule));
		res.setCancellationRule(cancellationRule == null?null:Rule.duplicteForNextSeason(cancellationRule));
		res.setConfiguration(configuration);

		return res;
	}

	private ValidationResults validateNotificationNameUniquness(Season season, String newName, String notificationId) {
		String replacedNewName = newName.replace(".", " ");
		for (AirlockNotification notification:season.getNotifications().getNotificationsList()) {
			if (notificationId != null && notificationId.equals(notification.getUniqueId().toString())) {
				continue; //skip the current notification in update				
			}

			String replacedName = notification.getName().replace(".", " ");
			if (replacedName.equalsIgnoreCase(replacedNewName)) {
				return new ValidationResults("A notification with the specified name already exists in the current version range. Periods and spaces are considered the same.", Status.BAD_REQUEST);					
			}
		}

		return null;
	}

	public ValidationResults validateProductionDontChanged(JSONObject updatedNotificationJSON)  throws JSONException {
		Stage updatedStage = Utilities.strToStage(updatedNotificationJSON.getString(Constants.JSON_FEATURE_FIELD_STAGE));
		if (updatedStage  == Stage.PRODUCTION && stage == Stage.DEVELOPMENT) {			
			return new ValidationResults("Unable to update the notification. Only a user with the Administrator or Product Lead can change notification from the development to the production stage.", Status.UNAUTHORIZED);	
		}

		if (updatedStage  == Stage.DEVELOPMENT && stage == Stage.PRODUCTION) {			
			return new ValidationResults("Unable to update the notification. Only a user with the Administrator or Product Lead can change notification from the production to the development stage.", Status.UNAUTHORIZED);	
		}

		String err = "Unable to update the notification. Only a user with the Administrator or Product Lead role can change notification that is in the production stage.";

		if (stage == Stage.PRODUCTION) {
			String updatedName = updatedNotificationJSON.getString(Constants.JSON_FIELD_NAME);
			if (!updatedName.equals(name)) 
				return new ValidationResults(err, Status.UNAUTHORIZED);						

			int updatedRolloutPercentage = updatedNotificationJSON.getInt(Constants.JSON_FEATURE_FIELD_PERCENTAGE);
			if (rolloutPercentage  != updatedRolloutPercentage) 
				return new ValidationResults(err, Status.UNAUTHORIZED);
			
			int updatedMinInterval = updatedNotificationJSON.getInt(Constants.JSON_FIELD_MIN_INTERVAL);
			if (minInterval  != updatedMinInterval) 
				return new ValidationResults(err, Status.UNAUTHORIZED);
			
			int updatedMaxNotifications = updatedNotificationJSON.getInt(Constants.JSON_FIELD_MAX_NOTIFICATIONS);
			if (maxNotifications  != updatedMaxNotifications) 
				return new ValidationResults(err, Status.UNAUTHORIZED);

			Boolean updatedEnabled = updatedNotificationJSON.getBoolean(Constants.JSON_FEATURE_FIELD_ENABLED);
			if (enabled != updatedEnabled) 
				return new ValidationResults(err, Status.UNAUTHORIZED);			

			String updatedMinAppVersion = updatedNotificationJSON.getString(Constants.JSON_FEATURE_FIELD_MIN_APP_VER);
			if (minAppVersion==null || !minAppVersion.equals(updatedMinAppVersion)) {
				return new ValidationResults(err, Status.UNAUTHORIZED);													
			}

			JSONObject updatedCancellationRuleJSON = updatedNotificationJSON.getJSONObject(Constants.JSON_FIELD_CANCELLATION_RULE);
			Rule updatedCancellationRule = new Rule();
			updatedCancellationRule.fromJSON(updatedCancellationRuleJSON);
			if (!cancellationRule.equals(updatedCancellationRule)) {
				return new ValidationResults(err, Status.UNAUTHORIZED);		
			}

			JSONObject updatedRegistrationRuleJSON = updatedNotificationJSON.getJSONObject(Constants.JSON_FIELD_REGISTRATION_RULE);
			Rule updatedRegistrationRule = new Rule();
			updatedRegistrationRule.fromJSON(updatedRegistrationRuleJSON);
			if (!registrationRule.equals(updatedRegistrationRule)) {
				return new ValidationResults(err, Status.UNAUTHORIZED);		
			}			

			String updatedconfiguration = updatedNotificationJSON.getString(Constants.JSON_FEATURE_FIELD_CONFIGURATION);
			if (configuration==null || !configuration.equals(updatedconfiguration)) {
				return new ValidationResults(err, Status.UNAUTHORIZED);													
			}

			//optional fields
			//description
			if (updatedNotificationJSON.containsKey(Constants.JSON_FIELD_DESCRIPTION) &&  updatedNotificationJSON.get(Constants.JSON_FIELD_DESCRIPTION)!=null) {
				//if missing from json or null - ignore
				String updatedDescription = updatedNotificationJSON.getString(Constants.JSON_FIELD_DESCRIPTION);
				if (description == null || !description.equals(updatedDescription)) {
					return new ValidationResults(err, Status.UNAUTHORIZED);				
				}
			}		

			//displayName
			if (updatedNotificationJSON.containsKey(Constants.JSON_FIELD_DISPLAY_NAME) &&  updatedNotificationJSON.get(Constants.JSON_FIELD_DISPLAY_NAME)!=null) {
				//if missing from json or null - ignore
				String updatedDisplayName = updatedNotificationJSON.getString(Constants.JSON_FIELD_DISPLAY_NAME);
				if (displayName == null || !displayName.equals(updatedDisplayName)) {
					return new ValidationResults(err, Status.UNAUTHORIZED);				
				}
			}											

			//owner
			if (updatedNotificationJSON.containsKey(Constants.JSON_FEATURE_FIELD_OWNER) &&  updatedNotificationJSON.get(Constants.JSON_FEATURE_FIELD_OWNER)!=null) {
				//if missing from json or null - ignore
				String updatedOwner = updatedNotificationJSON.getString(Constants.JSON_FEATURE_FIELD_OWNER);
				if (owner == null || !owner.equals(updatedOwner)) {
					return new ValidationResults(err, Status.UNAUTHORIZED);				
				}
			}		
		}

		return null;		
	}
	
	public String updateNotification(JSONObject updatedNotificationJSON, Season season) throws JSONException {
		//creator, creationDate should not be updated

		boolean wasChanged = false;
		StringBuilder updateDetails = new StringBuilder();
		
		//stage		
		Stage updatedStage = Utilities.strToStage(updatedNotificationJSON.getString(Constants.JSON_FEATURE_FIELD_STAGE));
		if (updatedStage != stage) {
			updateDetails.append(" 'stage' changed from " + stage + " to " + updatedStage + "\n");
			stage = updatedStage;
			wasChanged = true;
		}

		//name
		String updatedName = updatedNotificationJSON.getString(Constants.JSON_FIELD_NAME);
		if (!updatedName.equals(name)) {
			updateDetails.append(" 'name' changed from " + name + " to " + updatedName + "\n");
			name = updatedName;
			wasChanged = true;
		}		

		//enabled
		Boolean updatedEnabled = updatedNotificationJSON.getBoolean(Constants.JSON_FEATURE_FIELD_ENABLED);
		if (enabled != updatedEnabled) {
			updateDetails.append("'enabled' changed from " + enabled + " to " + updatedEnabled + "\n");
			enabled  = updatedEnabled;
			wasChanged = true;			
		}	

		//minAppVersion
		String updatedMinAppVersion = updatedNotificationJSON.getString(Constants.JSON_FEATURE_FIELD_MIN_APP_VER);
		if (minAppVersion==null || !minAppVersion.equals(updatedMinAppVersion)) {
			updateDetails.append("'minAppVersion' changed from " + minAppVersion + " to " + updatedMinAppVersion + "\n");
			minAppVersion  = updatedMinAppVersion;			
			wasChanged = true;
		}
		
		//configuration
		String updatedConfiguration = updatedNotificationJSON.getString(Constants.JSON_FEATURE_FIELD_CONFIGURATION);
		if (configuration==null || !configuration.equals(updatedConfiguration)) {
			updateDetails.append("'configuration' changed from " + configuration + " to " + updatedConfiguration + "\n");
			configuration  = updatedConfiguration;			
			wasChanged = true;
		}
		
		//cancellationRule
		JSONObject updatedCancellationRuleJSON = updatedNotificationJSON.getJSONObject(Constants.JSON_FIELD_CANCELLATION_RULE);
		Rule updatedCancellationRule = new Rule();
		updatedCancellationRule.fromJSON(updatedCancellationRuleJSON);
		if (!cancellationRule.equals(updatedCancellationRule)) {
			updateDetails.append("'cancellationRule' changed from \n" + cancellationRule.toJson(OutputJSONMode.ADMIN).toString() + "\nto\n" + updatedCancellationRule.toJson(OutputJSONMode.ADMIN).toString() + "\n");
			cancellationRule = updatedCancellationRule;
			wasChanged = true;			
		}
		
		//registrationRule
		JSONObject updatedRegistrationRuleJSON = updatedNotificationJSON.getJSONObject(Constants.JSON_FIELD_REGISTRATION_RULE);
		Rule updatedRegistrationRule = new Rule();
		updatedRegistrationRule.fromJSON(updatedRegistrationRuleJSON);
		if (!registrationRule.equals(updatedRegistrationRule)) {
			updateDetails.append("'registrationRule' changed from \n" + registrationRule.toJson(OutputJSONMode.ADMIN).toString() + "\nto\n" + updatedRegistrationRule.toJson(OutputJSONMode.ADMIN).toString() + "\n");
			registrationRule = updatedRegistrationRule;
			wasChanged = true;	 		
		}

		//rolloutPercentage
		double updatedRolloutPercentage = updatedNotificationJSON.getDouble(Constants.JSON_FEATURE_FIELD_PERCENTAGE);
		if (rolloutPercentage  != updatedRolloutPercentage) {
			updateDetails.append(" 'rolloutPercentage' changed from " + rolloutPercentage + " to " + updatedRolloutPercentage + "\n");
			rolloutPercentage = updatedRolloutPercentage;		
			wasChanged = true;
		}	
		
		//maxNotifications
		int updatedMaxNotifications = updatedNotificationJSON.getInt(Constants.JSON_FIELD_MAX_NOTIFICATIONS);
		if (maxNotifications  != updatedMaxNotifications) {
			updateDetails.append(" 'maxNotifications' changed from " + maxNotifications + " to " + updatedMaxNotifications + "\n");
			maxNotifications = updatedMaxNotifications;		
			wasChanged = true;
		}	
		
		//minInterval
		int updatedMinInterval = updatedNotificationJSON.getInt(Constants.JSON_FIELD_MIN_INTERVAL);
		if (minInterval  != updatedMinInterval) {
			updateDetails.append(" 'minInterval' changed from " + minInterval + " to " + updatedMinInterval + "\n");
			minInterval = updatedMinInterval;		
			wasChanged = true;
		}	

		//optional fields
		if (updatedNotificationJSON.containsKey(Constants.JSON_FIELD_DESCRIPTION) &&  updatedNotificationJSON.get(Constants.JSON_FIELD_DESCRIPTION)!=null) {
			//if missing from json or null - ignore
			String updatedDescription = updatedNotificationJSON.getString(Constants.JSON_FIELD_DESCRIPTION);
			if (description == null || !description.equals(updatedDescription)) {
				updateDetails.append(" 'description' changed from '" + description + "' to '" + updatedDescription + "'\n");
				description  = updatedDescription;
				wasChanged = true;
			}
		}	

		if (updatedNotificationJSON.containsKey(Constants.JSON_FIELD_DISPLAY_NAME) &&  updatedNotificationJSON.get(Constants.JSON_FIELD_DISPLAY_NAME)!=null) {
			//if missing from json or null - ignore
			String updatedDisplayName = updatedNotificationJSON.getString(Constants.JSON_FIELD_DISPLAY_NAME);
			if (displayName == null || !displayName.equals(updatedDisplayName)) {
				updateDetails.append(" 'displayName' changed from '" + displayName + "' to '" + updatedDisplayName + "'\n");
				displayName  = updatedDisplayName;
				wasChanged = true;
			}
		}	

		if (updatedNotificationJSON.containsKey(Constants.JSON_FEATURE_FIELD_OWNER) &&  updatedNotificationJSON.get(Constants.JSON_FEATURE_FIELD_OWNER)!=null) {
			//if missing from json or null - ignore
			String updatedOwner = updatedNotificationJSON.getString(Constants.JSON_FEATURE_FIELD_OWNER);
			if (owner == null || !owner.equals(updatedOwner)) {
				updateDetails.append(" 'owner' changed from '" + owner + "' to '" + updatedOwner + "'\n");
				owner  = updatedOwner;
				wasChanged = true;
			}
		}	

		if (updatedNotificationJSON.containsKey(Constants.JSON_FIELD_INTERNAL_USER_GROUPS) && updatedNotificationJSON.get(Constants.JSON_FIELD_INTERNAL_USER_GROUPS)!=null) {
			JSONArray updatedInternalUserGroups = updatedNotificationJSON.getJSONArray(Constants.JSON_FIELD_INTERNAL_USER_GROUPS);
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
}
