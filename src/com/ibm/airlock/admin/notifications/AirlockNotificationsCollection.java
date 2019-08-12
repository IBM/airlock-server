package com.ibm.airlock.admin.notifications;

import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response.Status;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Constants.InputSampleGenerationMode;
import com.ibm.airlock.Constants.OutputJSONMode;
import com.ibm.airlock.Constants.Stage;
import com.ibm.airlock.Strings;
import com.ibm.airlock.admin.GenerationException;
import com.ibm.airlock.admin.JsonGenerator;
import com.ibm.airlock.admin.SchemaValidator;
import com.ibm.airlock.admin.Season;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.ValidationCache;
import com.ibm.airlock.admin.ValidationException;
import com.ibm.airlock.admin.ValidationResults;
import com.ibm.airlock.admin.authentication.UserInfo;
import com.ibm.airlock.engine.VerifyRule;

public class AirlockNotificationsCollection {
	public static final Logger logger = Logger.getLogger(AirlockNotificationsCollection.class.getName());

	public class NotificationsLimitation {
		private int maxNotifications = -1; // required in update. -1 means no limitation 
		private int minInterval = -1; // required in update. -1 means no limitation
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

		public void fromJSON(JSONObject input) throws JSONException {
			if (input.containsKey(Constants.JSON_FIELD_MAX_NOTIFICATIONS) && input.get(Constants.JSON_FIELD_MAX_NOTIFICATIONS)!=null) 
				maxNotifications = input.getInt(Constants.JSON_FIELD_MAX_NOTIFICATIONS);

			if (input.containsKey(Constants.JSON_FIELD_MIN_INTERVAL) && input.get(Constants.JSON_FIELD_MIN_INTERVAL)!=null) 
				minInterval = input.getInt(Constants.JSON_FIELD_MIN_INTERVAL);
		}

		public JSONObject toJson() throws JSONException {
			JSONObject res = new JSONObject();

			res.put(Constants.JSON_FIELD_MIN_INTERVAL, minInterval);
			res.put(Constants.JSON_FIELD_MAX_NOTIFICATIONS, maxNotifications);

			return res;
		}

		public ValidationResults validateNotificationsLimitationJSON(JSONObject notificationsLimitaionJSON) {
			try {
				//minInterval
				if (!notificationsLimitaionJSON.containsKey(Constants.JSON_FIELD_MIN_INTERVAL) || notificationsLimitaionJSON.get(Constants.JSON_FIELD_MIN_INTERVAL) == null) {
					return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_MIN_INTERVAL), Status.BAD_REQUEST);
				}

				int mi = notificationsLimitaionJSON.getInt(Constants.JSON_FIELD_MIN_INTERVAL); //validate that is legal int
				if (mi<-1) {
					return new ValidationResults("Illegal minInterval value. Should be an integer greater than -1.", Status.BAD_REQUEST);
				}

				//maxNotifications
				if (!notificationsLimitaionJSON.containsKey(Constants.JSON_FIELD_MAX_NOTIFICATIONS) || notificationsLimitaionJSON.get(Constants.JSON_FIELD_MAX_NOTIFICATIONS) == null) {
					return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_MAX_NOTIFICATIONS), Status.BAD_REQUEST);
				}

				int mn = notificationsLimitaionJSON.getInt(Constants.JSON_FIELD_MAX_NOTIFICATIONS); //validate that is legal int
				if (mn<-1) {
					return new ValidationResults("Illegal maxNotifications value. Should be an integer greater than -1.", Status.BAD_REQUEST);
				}

				if (mi==-1 && mn==-1)
					return new ValidationResults("The maxNotifications and minInterval values cannot both be -1.", Status.BAD_REQUEST);

			} catch (JSONException jsne) {
				return new ValidationResults(jsne.getMessage(), Status.BAD_REQUEST);
			}
			return null;
		}

		public boolean equals (NotificationsLimitation other) {
			return (minInterval == other.minInterval && maxNotifications == other.maxNotifications);
		}
	}
	/*	final static String DEFAULT_NOTIFICATION_SCHEMA = "{" +
			"\"$schema\": \"http://json-schema.org/draft-04/schema#\"," +
		    "\"type\": \"object\"," + 
		    "\"properties\": {"+
		     "   \"title\": {"+
		      "      \"type\": \"string\""+
		       " },"+
		        "\"text\": {"+
		         "   \"type\": \"string\""+
		        "},"+
		        "\"dueDate\": {"+
		         "  \"type\": \"integer\"," +
		          "  \"minimum\": 0" +
		        "}"+
		    "}," +
		    "\"required\": [\"title\", \"text\"]" + 
		"}";
	 */
	private LinkedList<AirlockNotification> notificationsList = new LinkedList<AirlockNotification>();
	private JSONObject configurationSchema = null; //mandatory	
	private UUID seasonId = null;
	private Date lastModified = null; // required in update. protecting the schema from multi user update
	//private int maxNotifications = -1; // required in update. -1 means no limitation 
	//private int minInterval = -1; // required in update. -1 means no limitation
	private LinkedList<NotificationsLimitation> notificationsLimitations = new LinkedList<NotificationsLimitation>(); 


	public LinkedList<AirlockNotification> getNotificationsList() {
		return notificationsList;
	}
	public void setNotificationsList(LinkedList<AirlockNotification> notificationsList) {
		this.notificationsList = notificationsList;
	}
	public JSONObject getConfigurationSchema() {
		return configurationSchema;
	}
	public void setConfigurationSchema(JSONObject configurationSchema) {
		this.configurationSchema = configurationSchema;
	}
	public UUID getSeasonId() {
		return seasonId;
	}
	public void setSeasonId(UUID seasonId) {
		this.seasonId = seasonId;
	} 

	public Date getLastModified() {
		return lastModified;
	}
	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}	

	public LinkedList<NotificationsLimitation> getNotificationsLimitations() {
		return notificationsLimitations;
	}
	public void setNotificationsLimitations(LinkedList<NotificationsLimitation> notificationsLimitations) {
		this.notificationsLimitations = notificationsLimitations;
	}
	public AirlockNotificationsCollection(UUID seasonId, ServletContext context) {
		this.seasonId = seasonId;
		String defaultNotificationSchemaStr = (String)context.getAttribute(Constants.DEFAULT_NOTIFICATION_SCHEMA_PARAM_NAME);
		setLastModified(new Date());
		try {
			this.configurationSchema = new JSONObject(defaultNotificationSchemaStr);
		} catch (JSONException e) {
			//legal json
		}
	}

	public JSONObject toJson(OutputJSONMode mode) throws JSONException {
		JSONObject res = new JSONObject();
		JSONArray notificationsArr = new JSONArray();
		for (AirlockNotification notification : notificationsList) {
			if (mode != null && notification.getStage() == Stage.DEVELOPMENT && mode.equals(OutputJSONMode.RUNTIME_PRODUCTION))
				continue; //don't return stream in development when runtime_prod is requested

			JSONObject notifObj = notification.toJson(mode);
			notificationsArr.add(notifObj);				
		}
		res.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, seasonId==null?null:seasonId.toString());
		res.put(Constants.JSON_FIELD_NOTIFICATIONS, notificationsArr);
		res.put(Constants.JSON_FIELD_LAST_MODIFIED, lastModified.getTime());
		
		JSONArray notificationsLimitationsArr = new JSONArray();
		for (NotificationsLimitation nl:notificationsLimitations) {
			JSONObject notifObj = nl.toJson();
			notificationsLimitationsArr.add(notifObj);	
		}
		res.put(Constants.JSON_FIELD_NOTIFICATIONS_LIMITATIONS, notificationsLimitationsArr);

		if (mode.equals(OutputJSONMode.ADMIN) || mode.equals(OutputJSONMode.DISPLAY)) {
			res.put(Constants.JSON_FEATURE_FIELD_CONFIGURATION_SCHEMA, configurationSchema);
		}

		return res;
	}

	public JSONObject getNotificationSchemaJson() throws JSONException {
		JSONObject res = new JSONObject();

		res.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, seasonId==null?null:seasonId.toString());
		res.put(Constants.JSON_FIELD_LAST_MODIFIED, lastModified.getTime());
		res.put(Constants.JSON_FEATURE_FIELD_CONFIGURATION_SCHEMA, configurationSchema);

		return res;
	}

	//called in server init stage - reading the notifications from files in s3
	public void fromJSON(JSONObject input, Map<String, AirlockNotification> notificationsDB) throws JSONException {
		if (input.containsKey(Constants.JSON_FIELD_NOTIFICATIONS) && input.get(Constants.JSON_FIELD_NOTIFICATIONS)!=null) {
			JSONArray notificationArr = input.getJSONArray(Constants.JSON_FIELD_NOTIFICATIONS);
			for (int i=0; i<notificationArr.length(); i++ ) {
				JSONObject notificationJSON = notificationArr.getJSONObject(i);
				AirlockNotification notification = new AirlockNotification();
				notification.fromJSON(notificationJSON);
				addAirlockNotification(notification);			
				if (notificationsDB!=null)
					notificationsDB.put(notification.getUniqueId().toString(), notification);
			}
		}

		if (input.containsKey(Constants.JSON_FEATURE_FIELD_CONFIGURATION_SCHEMA) && input.get(Constants.JSON_FEATURE_FIELD_CONFIGURATION_SCHEMA)!=null) { 
			configurationSchema = input.getJSONObject(Constants.JSON_FEATURE_FIELD_CONFIGURATION_SCHEMA);					
		}

		if (input.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) && input.get(Constants.JSON_FIELD_LAST_MODIFIED)!=null) { 
			long timeInMS = input.getLong(Constants.JSON_FIELD_LAST_MODIFIED);
			lastModified = new Date(timeInMS);
		}  else {
			lastModified = new Date();
		}
		
		
		if (input.containsKey(Constants.JSON_FIELD_NOTIFICATIONS_LIMITATIONS) && input.get(Constants.JSON_FIELD_NOTIFICATIONS_LIMITATIONS)!=null) {
			JSONArray notificationsLimitationsArr = input.getJSONArray(Constants.JSON_FIELD_NOTIFICATIONS_LIMITATIONS);
			notificationsLimitations.clear();
			for (int i=0; i<notificationsLimitationsArr.length(); i++ ) {
				JSONObject nlJSON = notificationsLimitationsArr.getJSONObject(i);
				NotificationsLimitation nl = new NotificationsLimitation();
				nl.fromJSON(nlJSON);
				notificationsLimitations.add(nl);
			}
		}
	}

	public void addAirlockNotification(AirlockNotification notification) {
		notificationsList.add(notification);		
	}

	//return null if OK, error string on error
	public String removeAirlockNotification(AirlockNotification notificationToRem, Season season, ServletContext context) {
		String notificationId = notificationToRem.getUniqueId().toString(); 
		if (notificationsList == null || notificationsList.size() == 0) {
			return "Unable to remove notification " + notificationId + ", " + notificationToRem.getName() + " from season " + seasonId.toString() + ": season has no notifications.";	
		}

		boolean found = false;
		for (int i=0; i< notificationsList.size(); i++) {
			if (notificationsList.get(i).getUniqueId().toString().equals(notificationId)) {
				notificationsList.remove(i);
				found = true;
			}
		}

		if (!found)
			return "Unable to remove notification " + notificationId + " from season " + seasonId.toString() + ": The specified notification does not exist under this season.";

		return null;
	}

	public AirlockNotificationsCollection duplicateForNewSeason (String minVersion, UUID newSeasonId, Map<String, AirlockNotification> notificationsDB, ServletContext context) {
		AirlockNotificationsCollection res = new AirlockNotificationsCollection(newSeasonId, context);

		LinkedList<AirlockNotification> duplicatedNotificationsList = new LinkedList<AirlockNotification>();
		for (int i=0; i<notificationsList.size(); i++) {
			AirlockNotification notification = notificationsList.get(i).duplicateForNewSeason(minVersion, newSeasonId);
			duplicatedNotificationsList.add(notification);
			notificationsDB.put(notification.getUniqueId().toString(), notification);
		}

		res.setNotificationsList(duplicatedNotificationsList);
		res.setConfigurationSchema(configurationSchema == null?null:(JSONObject)configurationSchema.clone());
		res.setLastModified(lastModified);

		return res;
	}

	public String updateNotifications(JSONObject updatedNotificationsJSON, ServletContext context) throws JSONException {
		boolean wasChanged = false;
		StringBuilder updateDetails = new StringBuilder();

		JSONObject updatedNotificationSchema = updatedNotificationsJSON.getJSONObject(Constants.JSON_FEATURE_FIELD_CONFIGURATION_SCHEMA);
		if (!Utilities.jsonObjsAreEqual(configurationSchema, updatedNotificationSchema)) {
			updateDetails.append("'configurationSchema' changed from " + (configurationSchema==null?"null":configurationSchema.toString()) + " to " + updatedNotificationSchema.toString() + "\n");			
			configurationSchema = updatedNotificationSchema;
			wasChanged = true;

		}		
				
		//notifications
		JSONArray updatedNotificationsArray = updatedNotificationsJSON.getJSONArray(Constants.JSON_FIELD_NOTIFICATIONS); //after validate - i know exists		
		if (orderChanged (updatedNotificationsArray, notificationsList)) {
			@SuppressWarnings("unchecked")
			Map<String, AirlockNotification> notificationsDB = (Map<String, AirlockNotification>)context.getAttribute(Constants.NOTIFICATIONS_DB_PARAM_NAME);

			addAuditMsgForOrderChange(updatedNotificationsArray, updateDetails);
			//create new notifications list - add them one by one and switch with original list
			LinkedList<AirlockNotification> newNotifications = new LinkedList<AirlockNotification>();
			for (int i=0; i<updatedNotificationsArray.size(); i++) {					
				String id  = updatedNotificationsArray.getJSONObject(i).getString(Constants.JSON_FIELD_UNIQUE_ID);										
				AirlockNotification curNotification = notificationsDB.get(id);
				newNotifications.add(curNotification);
			}				
			notificationsList = newNotifications;
			wasChanged = true;
		}	

		//notificationsLimitations
		JSONArray updatedNotificationsLimitationsArray = updatedNotificationsJSON.getJSONArray(Constants.JSON_FIELD_NOTIFICATIONS_LIMITATIONS); //after validate - i know exists		
		if (!areNotificationLimitationListsEqual (updatedNotificationsLimitationsArray, notificationsLimitations)) {		
			addAuditMsgNotificationLimitationChange(updatedNotificationsLimitationsArray, updateDetails);
			//create new notificationsLiitation list - add them one by one and switch with original list
			LinkedList<NotificationsLimitation> newNotificationsLimitations = new LinkedList<NotificationsLimitation>();
			for (int i=0; i<updatedNotificationsLimitationsArray.size(); i++) {		
				NotificationsLimitation nl = new NotificationsLimitation();
				nl.fromJSON(updatedNotificationsLimitationsArray.getJSONObject(i));
				newNotificationsLimitations.add(nl);
			}				
			notificationsLimitations = newNotificationsLimitations;
			wasChanged = true;
		}	
		
		//notification update (can update notification while updating the notifications list)
		int i=0;
		@SuppressWarnings("unchecked")
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);			
		Season season = seasonsDB.get(seasonId.toString());
		for (AirlockNotification notification:notificationsList) {			
			String updateVarDetails = notification.updateNotification(updatedNotificationsArray.getJSONObject(i), season); //i know that the json array and variant list are now at the same order.
			if (!updateVarDetails.isEmpty()) {
				updateDetails.append("In notification " + notification.getName() + ", " + notification.getUniqueId().toString() + ": \n" + updateVarDetails);
				wasChanged = true;
			}
			i++;
		}
		if (wasChanged) {
			lastModified = new Date();
		}

		return updateDetails.toString();
	}

	private void addAuditMsgNotificationLimitationChange(JSONArray updatedNotificationsLimitationsArray,
			StringBuilder updateDetails) throws JSONException {
		updateDetails.append("The notifications limitations of season " + seasonId.toString() + " changed.\n\nBefore:\n[");
		for (int i=0; i<notificationsLimitations.size(); i++) {
			updateDetails.append(notificationsLimitations.get(i).toJson().toString() + "\n");
		}

		updateDetails.append("]\n After:\n[");
		for (int i=0; i<updatedNotificationsLimitationsArray.size(); i++) {
			JSONObject nl = updatedNotificationsLimitationsArray.getJSONObject(i);
			updateDetails.append(nl.toString() + "\n");
		}

		updateDetails.append( "]\n");
		
	}
	private boolean areNotificationLimitationListsEqual(JSONArray updatedNotificationsLimitationsArray,
			LinkedList<NotificationsLimitation> origNotificationsLimitationsList) throws JSONException {
		if (updatedNotificationsLimitationsArray.size() != origNotificationsLimitationsList.size()) {
			return false; 
		}

		for (int i=0; i<origNotificationsLimitationsList.size(); i++) {
			NotificationsLimitation tmp = new NotificationsLimitation();
			tmp.fromJSON(updatedNotificationsLimitationsArray.getJSONObject(i));
			if (!origNotificationsLimitationsList.get(i).equals(tmp)) {
				return false;
			}
		}

		return true;		
	}
	
	private void addAuditMsgForOrderChange(JSONArray updatedNotificationsArray, StringBuilder updateDetails) throws JSONException {
		updateDetails.append("The order of notifications of season " + seasonId.toString() + " changed.\n\nBefore:\n");
		for (int i=0; i<notificationsList.size(); i++) {
			updateDetails.append(notificationsList.get(i).getName() + "(" + notificationsList.get(i).getUniqueId().toString() + ")\n");
		}

		updateDetails.append("\n After:\n");
		for (int i=0; i<updatedNotificationsArray.size(); i++) {
			JSONObject notification = updatedNotificationsArray.getJSONObject(i);
			updateDetails.append(notification.getString(Constants.JSON_FIELD_NAME) + "(" + notification.getString(Constants.JSON_FIELD_UNIQUE_ID) +")\n");
		}

		updateDetails.append( "\n");
	}

	public ValidationResults validateNotificationsJSON(JSONObject notificationsJSON, ServletContext context, Season season, UserInfo userInfo) {
		try {
			@SuppressWarnings("unchecked")
			Map<String, AirlockNotification> notificationsDB = (Map<String, AirlockNotification>)context.getAttribute(Constants.NOTIFICATIONS_DB_PARAM_NAME);

			//schema
			if (!notificationsJSON.containsKey(Constants.JSON_FEATURE_FIELD_CONFIGURATION_SCHEMA) || notificationsJSON.getString(Constants.JSON_FEATURE_FIELD_CONFIGURATION_SCHEMA) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_CONFIGURATION_SCHEMA), Status.BAD_REQUEST);
			}

			JSONObject updatedSchemaJson = notificationsJSON.getJSONObject(Constants.JSON_FEATURE_FIELD_CONFIGURATION_SCHEMA); //validate legal json

			//modification date must appear
			if (!notificationsJSON.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) || notificationsJSON.get(Constants.JSON_FIELD_LAST_MODIFIED)==null) {
				return new ValidationResults("The lastModified field is missing. This field must be specified during notification schema update.", Status.BAD_REQUEST);
			}

			//verify that given modification date is not older that current modification date
			long givenModoficationDate = notificationsJSON.getLong(Constants.JSON_FIELD_LAST_MODIFIED);  //verify that legal long
			Date givenDate = new Date(givenModoficationDate);
			if (givenDate.before(lastModified)) {
				return new ValidationResults("Notifications were changed by another user. Refresh your browser and try again.", Status.CONFLICT);			
			}				

			//notifications
			if (!notificationsJSON.containsKey(Constants.JSON_FIELD_NOTIFICATIONS) || notificationsJSON.get(Constants.JSON_FIELD_NOTIFICATIONS) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_NOTIFICATIONS), Status.BAD_REQUEST);							
			}

			JSONArray notificationsArr = notificationsJSON.getJSONArray(Constants.JSON_FIELD_NOTIFICATIONS);
			for (int i=0; i<notificationsArr.size(); i++) {
				JSONObject notificationJSON = notificationsArr.getJSONObject(i);
				String notificationId = notificationJSON.getString(Constants.JSON_FIELD_UNIQUE_ID);
				if (notificationId == null) {
					return new ValidationResults("Notification id is missing.", Status.BAD_REQUEST);
				}
				AirlockNotification notification = notificationsDB.get(notificationId);
				if (notification == null) {
					return new ValidationResults("Notification does not exist.", Status.BAD_REQUEST);
				}

				ValidationResults vr = notification.validateNotificationJSON(notificationJSON, context, userInfo, seasonId.toString());					
				if (vr!=null && !vr.status.equals(Status.OK))
					return vr;								
			}
			String missingOrAddedNotificationsErr = validateNotificationsArr(notificationsArr);
			if (missingOrAddedNotificationsErr!=null) {
				return new ValidationResults(missingOrAddedNotificationsErr, Status.BAD_REQUEST); 
			}						

			//notifications limitations
			if (!notificationsJSON.containsKey(Constants.JSON_FIELD_NOTIFICATIONS_LIMITATIONS) || notificationsJSON.get(Constants.JSON_FIELD_NOTIFICATIONS_LIMITATIONS) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_NOTIFICATIONS_LIMITATIONS), Status.BAD_REQUEST);							
			}

			JSONArray notificationsLimitationsArr = notificationsJSON.getJSONArray(Constants.JSON_FIELD_NOTIFICATIONS_LIMITATIONS);
			for (int i=0; i<notificationsLimitationsArr.size(); i++) {
				JSONObject nlJSON = notificationsLimitationsArr.getJSONObject(i);				
				NotificationsLimitation nl = new NotificationsLimitation();				
				ValidationResults vr = nl.validateNotificationsLimitationJSON(nlJSON);					
				if (vr!=null && !vr.status.equals(Status.OK))
					return vr;								
			}
			
			if (!Utilities.jsonObjsAreEqual(configurationSchema, updatedSchemaJson)) {
				String ajv = (String)context.getAttribute(Constants.SCHEMA_VALIDATOR_AJV_PARAM_NAME);
				String validator = (String)context.getAttribute(Constants.SCHEMA_VALIDATOR_VALIDATOR_PARAM_NAME);		

				//validate the schema
				try {
					SchemaValidator.validateSchema(validator, null, ajv, updatedSchemaJson.toString());
				} catch (ValidationException ve) {
					return new ValidationResults("The Configuration Schema is not valid: " + ve.getMessage(), Status.BAD_REQUEST);
				}


				//validate that all notifications are aligned with the new schema
				for (AirlockNotification notifiction:notificationsList) {
					try {					
						String notificationConfig = notifiction.getConfiguration();
						String notificationMinAppVer = notifiction.getMinAppVersion();
						Stage notificationStage = notifiction.getStage();
						String notificationRegistrationRuleString = notifiction.getRegistrationRule().getRuleString();						

						ValidationCache tester = new ValidationCache();

						ValidationCache.Info info;
						try {
							info = tester.getInfo(context, season, notificationStage, notificationMinAppVer);
						} catch (GenerationException ge) {
							return new ValidationResults("Failed to generate data sample: " + ge.getMessage(), Status.BAD_REQUEST);		
						}

						JSONObject configJsonObj = null;
						try {
							configJsonObj = VerifyRule.fullConfigurationEvaluation(notificationRegistrationRuleString, notificationConfig, info.minimalInvoker, info.maximalInvoker);

						} catch (ValidationException e) {
							return new ValidationResults("Validation error: " + e.getMessage(), Status.BAD_REQUEST);		
						}

						if (configJsonObj!=null) {
							try {
								boolean relaxed = true;
								SchemaValidator.validation(validator, ajv, updatedSchemaJson.toString(), configJsonObj.toString(), relaxed);
							} catch (ValidationException ve) {
								return new ValidationResults("The '" + notifiction.getName() + "' notification configuration does not match the Configuration Schema: " + ve.getMessage(), Status.BAD_REQUEST);
							}
						}

						boolean relaxed = false;
						SchemaValidator.validation(validator, ajv, updatedSchemaJson.toString(), notifiction.getConfiguration(), relaxed);
					} catch (ValidationException ve) {
						return new ValidationResults("The '" + notifiction.getName() + "' notification configuration does not match the Configuration Schema: " + ve.getMessage(), Status.BAD_REQUEST);
					}
				}
			}
		} catch (JSONException jsne) {
			return new ValidationResults(jsne.getMessage(), Status.BAD_REQUEST);
		}
		return null;
	}

	//if a notification was added or removed - return error string otherwise return null	
	private String validateNotificationsArr(JSONArray notificationsArr) throws JSONException {
		if (notificationsArr.size() > notificationsList.size()) {
			return "A notification cannot be added when the notifications are being updated. Instead, call add notification."; 		
		}

		if (notificationsArr.size() < notificationsList.size()) {
			return "A notification cannot be removed when the notifications are being updated. Instead, call delete notification.";			 	
		}

		//same number of variants - look at the uniqueIds
		for (AirlockNotification notification:notificationsList) {
			boolean found = false;
			for (int i=0; i<notificationsArr.size(); i++) { 
				JSONObject varJSON = notificationsArr.getJSONObject(i); //after validate - i know that is json and containns uniqueId field

				if (notification.getUniqueId().toString().equals(varJSON.getString(Constants.JSON_FIELD_UNIQUE_ID))) {
					found = true;
					break;
				}
			}
			if (!found) {
				return "A notification cannot be added or removed when the notifications are being updated. Instead, call add or delete notification.";				
			}
		}
		return null;
	}

	public String validateProdNotificationNotUsingString(String stringKey, Stage newStage, Season season, ServletContext context, ValidationCache tester) throws JSONException, GenerationException {
		for (AirlockNotification notification:notificationsList) {
			if (newStage == Stage.DEVELOPMENT && notification.getStage() == Stage.PRODUCTION) {

				if (notification.getConfiguration()!=null) {
					try {
						ValidationCache.Info info = tester.getInfo(context, season, notification.getStage(), notification.getMinAppVersion());
						VerifyRule.fullConfigurationEvaluation(notification.getRegistrationRule().getRuleString(), notification.getConfiguration(), info.minimalInvoker, info.maximalInvoker);

					} catch (ValidationException e) {
						return "Unable to update the string '" + stringKey + "'. Either the minimum version of the string is higher than the minimum version of the notification '" + notification.getName() + "' that is using it, or the stage of the string is development while the stage of the notification is production.";		
					}
				}				
			}
		}
		return null;
	}

	public ValidationResults validateProductionDontChanged(JSONObject updatedNotificationsJSON, ServletContext context) throws JSONException {
		//production is change in the following cases:
		//1. Some of the notifications in the season are in production and the maxNotifications is changed
		//2. Some of the notifications in the season are in production and the minInterval is changed
		//3. the order of some notifications in the production stage was changed
		boolean isNotifInProdExists = false;

		@SuppressWarnings("unchecked")
		Map<String, AirlockNotification> notificationsDB = (Map<String, AirlockNotification>)context.getAttribute(Constants.NOTIFICATIONS_DB_PARAM_NAME);


		JSONArray notificationsArr = updatedNotificationsJSON.getJSONArray(Constants.JSON_FIELD_NOTIFICATIONS);
		for (int i=0; i<notificationsArr.size(); i++) {
			JSONObject notificationJSON = notificationsArr.getJSONObject(i);
			String stageStr = notificationJSON.getString(Constants.JSON_FEATURE_FIELD_STAGE); //after validation: it exists and is legal
			if (stageStr.equals(Stage.PRODUCTION.toString())){
				isNotifInProdExists = true;
				break;
			}
			String notificationId = notificationJSON.getString(Constants.JSON_FIELD_UNIQUE_ID); //after validation: it exists and is legal
			AirlockNotification notification = notificationsDB.get(notificationId);
			if (notification.getStage().equals(Stage.PRODUCTION)){
				isNotifInProdExists = true;
				break;
			}			
		}

		if (isNotifInProdExists) {

			String err = "Unable to update the notifications. Only a user with the Administrator or Product Lead role can update the notifications when a notification in production stage exists.";						

			//validate that notifications in production wasn't changed
			for (int i=0; i<notificationsArr.size(); i++) {
				JSONObject notificationJSON = notificationsArr.getJSONObject(i);
				String notificationId = notificationJSON.getString(Constants.JSON_FIELD_UNIQUE_ID); //after validation: it exists and is legal
				AirlockNotification notification = notificationsDB.get(notificationId);
				ValidationResults vr = notification.validateProductionDontChanged(notificationJSON);
				if (vr!=null)
					return vr;
			}

			//validate that the order of notifications in production wasn't changed
			JSONArray updatedNotificationsInProduction = new JSONArray();
			//create updated variants list that are on production
			for (int i=0; i<notificationsArr.size(); i++) {
				JSONObject notificationJSON = notificationsArr.getJSONObject(i);
				if (notificationJSON.containsKey(Constants.JSON_FEATURE_FIELD_STAGE) && notificationJSON.getString(Constants.JSON_FEATURE_FIELD_STAGE).equals(Stage.PRODUCTION.toString())) {
					updatedNotificationsInProduction.add(notificationJSON);
				}
			}

			//create current variants list that are on production
			LinkedList<AirlockNotification> notificationsInProduction = new LinkedList<AirlockNotification>();
			for (int i=0; i<notificationsList.size(); i++) {
				//JSONObject subFeatureJSONObj = updatedSubFeatures.getJSONObject(i);
				if (notificationsList.get(i).getStage().equals(Stage.PRODUCTION)) {
					notificationsInProduction.add(notificationsList.get(i));
				}			
			}


			if (orderChanged (updatedNotificationsInProduction, notificationsInProduction)) {
				return new ValidationResults(err, Status.UNAUTHORIZED);
			}
			
			JSONArray notificationsLimitationsArr = updatedNotificationsJSON.getJSONArray(Constants.JSON_FIELD_NOTIFICATIONS_LIMITATIONS);			
			if (!areNotificationLimitationListsEqual(notificationsLimitationsArr, notificationsLimitations)) {
				return new ValidationResults(err, Status.UNAUTHORIZED);
			}
		}
		return null;
	}

	private boolean orderChanged(JSONArray updatedProdNotifications,  LinkedList<AirlockNotification> origProdNotifications) throws JSONException {
		if (origProdNotifications.size() != updatedProdNotifications.size()) {
			return true; //notifications added hence order changed
		}

		for (int i=0; i<origProdNotifications.size(); i++) {
			if (!origProdNotifications.get(i).getUniqueId().toString().equals(updatedProdNotifications.getJSONObject(i).getString(Constants.JSON_FIELD_UNIQUE_ID))) {
				return true;
			}
		}

		return false;
	}
	
	public JSONObject generateOutputSample(ServletContext context) throws GenerationException, JSONException {		

		String faker = (String)context.getAttribute(Constants.SCHEMA_JSON_FAKER_PARAM_NAME);
		String generator = (String)context.getAttribute(Constants.SCHEMA_JSON_GENERATOR_PARAM_NAME);				
		String prune = (String)context.getAttribute(Constants.SCHEMA_JSON_PRUNE_PARAM_NAME);

		String outputSampleStr = JsonGenerator.generation(generator, faker, prune, configurationSchema.toString(), null, null, InputSampleGenerationMode.MAXIMAL, Constants.DEFAULT_RANDOMIZER); 				
		
		JSONObject outputSampleJson = new JSONObject(outputSampleStr);

		return outputSampleJson;
	}
}



