package com.ibm.airlock.admin;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import com.ibm.airlock.Strings;
import com.ibm.airlock.admin.serialize.AirlockFilesWriter;
import com.ibm.airlock.admin.serialize.DataSerializer;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.AirLockContextListener;
import com.ibm.airlock.Constants;
import com.ibm.airlock.Constants.OutputJSONMode;
import com.ibm.airlock.ProductServices;
import com.ibm.airlock.admin.analytics.ExperimentsMutualExclusionGroup;
import com.ibm.airlock.admin.operations.AirlockCapabilities;
import com.ibm.airlock.admin.serialize.S3DataSerializer;

import com.ibm.airlock.admin.serialize.TranslatorLogWriter;
import com.ibm.airlock.admin.translations.SmartlingClient;
import com.ibm.airlock.admin.translations.SmartlingLocales;
import com.ibm.airlock.admin.translations.BackgroundTranslator;
import com.ibm.airlock.admin.translations.TranslationUtilities;
import com.ibm.airlock.engine.Environment;

public class InitializationThread extends Thread {
	public static final Logger logger = Logger.getLogger(InitializationThread.class.getName());
	
	ServletContext context;
	DataSerializer ds;
	
	public InitializationThread (ServletContext context) {
		super("Airlock-initialization-thread");
		this.context = context;
	}
	
	public void run() {
		@SuppressWarnings("unchecked")
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

		ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);

		//init capabilities from S3 file
		try {
			logger.info ("Initalizing Airlock capabilities from: " + Constants.AIRLOCK_CAPABILITIES_FILE_NAME);						
			JSONObject capabilitiesJSON  = ds.readDataToJSON(Constants.AIRLOCK_CAPABILITIES_FILE_NAME);
			Utilities.initFromCapabilitiesJSON (capabilitiesJSON, context);
			logger.info ("Airlock capabilities initialization done.");
			logger.info("Server capabilities: \n" + ((AirlockCapabilities)context.getAttribute(Constants.CAPABILITIES_PARAM_NAME)).getCapabilities().toString());
		} catch (IOException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			String error = String.format(Strings.failedInitializationReadingFile,Constants.AIRLOCK_CAPABILITIES_FILE_NAME) + e.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			throw new RuntimeException(error);							
		} catch (JSONException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			String errMsg = String.format(Strings.failedInitializationInvalidJson,Constants.AIRLOCK_CAPABILITIES_FILE_NAME) + e.getMessage();
			logger.severe(errMsg);		
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			throw new RuntimeException(errMsg);						
		}					
				
		//init products from S3 file		
		try {
			logger.info ("Initalizing products from: " + Constants.PRODUCTS_FILE_NAME);			
			JSONObject productsJSON = ds.readDataToJSON(Constants.PRODUCTS_FILE_NAME);
			Utilities.initFromProductsJSON (productsJSON, context);			
			logger.info ("Products initialization done.");
		} catch (IOException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			String error = String.format(Strings.failedInitializationReadingFile,Constants.PRODUCTS_FILE_NAME) + e.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			throw new RuntimeException(error);
		} catch (JSONException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			String errMsg = String.format(Strings.failedInitializationInvalidJson,Constants.PRODUCTS_FILE_NAME) + e.getMessage();
			logger.severe(errMsg);		
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			throw new RuntimeException(errMsg);
		}


		//init API keys from S3 file
		JSONObject keysPasswordsJSON = null;
		JSONObject keysJSON = null;
		
		try {
			logger.info ("Initalizing Airlock API keys from: " + Constants.AIRLOCK_API_KEYS_FILE_NAME);			
			keysJSON = ds.readDataToJSON(Constants.AIRLOCK_API_KEYS_FILE_NAME);			
		} catch (IOException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			String error = String.format(Strings.failedInitializationReadingFile, Constants.AIRLOCK_API_KEYS_FILE_NAME) + e.getMessage();
			logger.severe(error);
			logger.severe("Changing Airlock service state to S3_IO_ERROR.");
			throw new RuntimeException(error);
		} catch (JSONException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			String errMsg = String.format(Strings.failedInitializationInvalidJson, Constants.AIRLOCK_API_KEYS_FILE_NAME) + e.getMessage();
			logger.severe(errMsg);		
			logger.severe("Changing Airlock service state to S3_DATA_CONSISTENCY_ERROR.");
			throw new RuntimeException(errMsg);
		}
		
		String passwordsFileName = Constants.AIRLOCK_API_KEYS_PASSWORDS_FILE_NAME;
		
		try {				
			logger.info ("Initalizing Airlock API keys passwords from: " + passwordsFileName);
			if (ds instanceof S3DataSerializer) { //only s3 serialization supports encryption					
				keysPasswordsJSON = new JSONObject(((S3DataSerializer)ds).getEncryptedFileShortName(passwordsFileName));
			}
			else {
				keysPasswordsJSON = ds.readDataToJSON(passwordsFileName);
			}
		} catch (IOException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			String error = String.format(Strings.failedInitializationReadingFile, passwordsFileName) + e.getMessage();
			logger.severe(error);
			logger.severe("Changing Airlock service state to S3_IO_ERROR.");
			throw new RuntimeException(error);
		} catch (JSONException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			String errMsg = String.format(Strings.failedInitializationInvalidJson, passwordsFileName) + e.getMessage();
			logger.severe(errMsg);		
			logger.severe("Changing Airlock service state to S3_DATA_CONSISTENCY_ERROR.");
			throw new RuntimeException(errMsg);
		} catch (Exception e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			String error = String.format(Strings.failedInitializationReadingFile, passwordsFileName) + e.getMessage();
			logger.severe(error);
			logger.severe("Changing Airlock service state to S3_IO_ERROR.");
			throw new RuntimeException(error);
		}
		
		try {			
			Utilities.initAirlockKeysPasswords (keysJSON, keysPasswordsJSON, context);			
			logger.info ("Airlock API keys initialization done.");
		} catch (JSONException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			String errMsg = String.format(Strings.failedInitializationInvalidJson, Constants.AIRLOCK_API_KEYS_FILE_NAME) + e.getMessage();
			logger.severe(errMsg);		
			logger.severe("Changing Airlock service state to S3_DATA_CONSISTENCY_ERROR.");
			throw new RuntimeException(errMsg);
		}
	
		String seasonId = "";
		try {	
			logger.info ("Initalizing season features from: " + Constants.SEASONS_FOLDER_NAME);
			LinkedList<JSONObject> seasonFeaturesList = getSeasonFilesJSONs(Constants.AIRLOCK_FEATURES_FILE_NAME, true, ds);
			for (JSONObject seasonFeatures: seasonFeaturesList) {
				seasonId = seasonFeatures.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID);
				@SuppressWarnings("unchecked")
				Map<String, BaseAirlockItem> map = (Map<String, BaseAirlockItem>) context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);
				seasonId = Utilities.initFromSeasonFeaturesJSON (seasonFeatures, seasonsDB, map, ds, true, Constants.REQUEST_ITEM_TYPE.FEATURES);
				logger.info ("season features'" + seasonId + "' initialization done.");
				
				try {					
					JSONObject stringsJSON = TranslationUtilities.readOriginalStringsFile(seasonsDB.get(seasonId), context, logger);
					seasonId = Utilities.initFromSeasonOriginalStringsJSON(stringsJSON, context);
					logger.info ("season strings '" + seasonId + "' initialization done.");				
				} catch (JSONException je) {
					logger.severe("Failed reading the translation file for season: " + seasonId + ":" + je.getMessage());
					throw je;
				} catch (IOException ioe) {
					logger.severe("Failed reading the translation file for season: " + seasonId + ":" + ioe.getMessage());
					throw ioe;
				}
			}
			logger.info ("Seasons initialization done.");
		} catch (IOException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			String errMsg = Strings.failedInitializationSeason + seasonId + String.format(Strings.unableToReadFile,Constants.SEASONS_FOLDER_NAME) + e.getMessage();
			logger.severe(errMsg);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");			
			throw new RuntimeException(errMsg);						
		} catch (JSONException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			String errMsg = Strings.failedInitializationSeason + seasonId + Strings.failedInitializationSeasonUnexpectedFormat + e.getMessage();
			logger.severe(errMsg);		
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			throw new RuntimeException(errMsg);						
		}	
		
		try {	
			logger.info ("Initalizing season purchase items from: " + Constants.SEASONS_FOLDER_NAME);
			LinkedList<JSONObject> seasonPurchasesList = getSeasonFilesJSONs(Constants.AIRLOCK_ENTITLEMENTS_FILE_NAME, false, ds); //pre 5.5 seasons dont have purchases
			for (JSONObject seasonPurchases: seasonPurchasesList) {
				seasonId = seasonPurchases.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID);
				@SuppressWarnings("unchecked")
				Map<String, BaseAirlockItem> map = (Map<String, BaseAirlockItem>) context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);
				seasonId = Utilities.initFromSeasonFeaturesJSON (seasonPurchases, seasonsDB, map, ds, true, Constants.REQUEST_ITEM_TYPE.ENTITLEMENTS);
				logger.info ("season purchase items '" + seasonId + "' initialization done.");
			}
			
			//for each season - if it's version is pre 5.5 - add its entitlements root to the airlock items db
			Set<String> seasonsIds = seasonsDB.keySet();
			@SuppressWarnings("unchecked")
			Map<String, BaseAirlockItem> airlockItemsDB = (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);
			
			for(String sId:seasonsIds) {
				Season s = seasonsDB.get(sId);
				Environment env = new Environment();
				env.setServerVersion(s.getServerVersion());

				if (!ProductServices.isPurchasesSupported(env)) {
					airlockItemsDB.put(s.getEntitlementsRoot().getUniqueId().toString(), s.getEntitlementsRoot());
				}
			}
			
			logger.info ("Seasons purchase items initialization done.");	
		} catch (IOException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			String errMsg = Strings.failedInitializationSeason + seasonId + String.format(Strings.unableToReadFile, Constants.AIRLOCK_ENTITLEMENTS_FILE_NAME) + e.getMessage();
			logger.severe(errMsg);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");			
			throw new RuntimeException(errMsg);						
		} catch (JSONException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			String errMsg = Strings.failedInitializationSeason + seasonId + Strings.failedInitializationSeasonPurchasesUnexpectedFormat + e.getMessage();
			logger.severe(errMsg);		
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			throw new RuntimeException(errMsg);						
		}	
		
		try {				
			//not mandatory since can exist for some seasons and for others not			
			logger.info ("Initalizing season encryption key from: " + Constants.SEASONS_FOLDER_NAME);
			LinkedList<JSONObject> seasonEncryptionKeysList = getSeasonFilesJSONs(Constants.AIRLOCK_ENCRYTION_KEY_FILE_NAME, false, ds);

			for (JSONObject seasonEncryptionKey: seasonEncryptionKeysList) {
				try {
					seasonId = seasonEncryptionKey.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID);
					seasonId = Utilities.initFromEncryptionKeyJSON(seasonEncryptionKey, context);
					logger.info ("season encryption key '" + seasonId + "' initialization done.");	
				} catch (IllegalArgumentException iax) {
					//this error is thrown when the season does not exists in the products file
					//This can happend if the season was deleted but some files (in this case the AirlockInputShema.json) was not deleted 
					//from S3. in this case - report error and continue with next seasons
					logger.warning ("encryption key for season '" + seasonEncryptionKey.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID) + "' exists but the season was already deleted:" + iax.getMessage());
				}
			}
			logger.info ("Seasons encryption keys initialization done.");		
		} catch (IOException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			String errMsg = Strings.failedInitializationSeason + seasonId + String.format(Strings.unableToReadFile,Constants.AIRLOCK_ENCRYTION_KEY_FILE_NAME) + e.getMessage();
			logger.severe(errMsg);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			throw new RuntimeException(errMsg);						
		} catch (JSONException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			String errMsg = Strings.failedInitializationSeason + seasonId + String.format(Strings.failedInitializationFileUnexpectedFormat,Constants.AIRLOCK_ENCRYTION_KEY_FILE_NAME) + e.getMessage();
			logger.severe(errMsg);		
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			throw new RuntimeException(errMsg);						
		}
		
		try {	
			logger.info ("Initalizing season input schema from: " + Constants.SEASONS_FOLDER_NAME);
			LinkedList<JSONObject> seasonInputSchemasList = getSeasonFilesJSONs(Constants.AIRLOCK_INPUT_SCHEMA_FILE_NAME, true, ds);

			for (JSONObject seasonInputSchema: seasonInputSchemasList) {
				try {
					seasonId = seasonInputSchema.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID);
					// note: we don't merge the stream schemas here - they are added a little later with mergeSchema().
					// the main schema is incomplete and should not be used by analytics until then.
					seasonId = Utilities.initFromSeasonInputschemaJSON(seasonInputSchema, null, context);
					logger.info ("season input schema '" + seasonId + "' initialization done.");	
				} catch (IllegalArgumentException iax) {
					//this error is thrown when the season does not exists in the products file
					//This can happend if the season was deleted but some files (in this case the AirlockInputShema.json) was not deleted 
					//from S3. in this case - report error and continue with next seasons
					logger.warning ("Input schema for season '" + seasonInputSchema.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID) + "' exists but the season was already deleted:" + iax.getMessage());
				}
			}
			logger.info ("Seasons input schema initialization done.");		
		} catch (IOException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			String errMsg = Strings.failedInitializationSeason + seasonId + String.format(Strings.unableToReadFile,Constants.AIRLOCK_INPUT_SCHEMA_FILE_NAME) + e.getMessage();
			logger.severe(errMsg);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			throw new RuntimeException(errMsg);						
		} catch (JSONException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			String errMsg = Strings.failedInitializationSeason + seasonId + String.format(Strings.failedInitializationFileUnexpectedFormat,Constants.AIRLOCK_INPUT_SCHEMA_FILE_NAME) + e.getMessage();
			logger.severe(errMsg);		
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			throw new RuntimeException(errMsg);						
		}
		
		try {	
			logger.info ("Initalizing season utilities from: " + Constants.SEASONS_FOLDER_NAME);
			LinkedList<JSONObject> seasonUtilitiesList = getSeasonFilesJSONs(Constants.AIRLOCK_UTILITIES_FILE_NAME, true, ds);
			for (JSONObject alUtil: seasonUtilitiesList) {
				try { 
					seasonId = alUtil.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID);
					seasonId = Utilities.initFromSeasonUtilitiesJSON(alUtil, context);
					logger.info ("season utilities '" + seasonId + "' initialization done.");	
				} catch (IllegalArgumentException iax) {
					//this error is thrown when the season does not exists in the products file
					//This can happend if the season was deleted but some files (in this case the AirlockUtilities.json) was not deleted 
					//from S3. in this case - report error and continue with next seasons
					logger.warning ("Utilities for season '" + alUtil.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID) + "'  exist but the season was already deleted:" + iax.getMessage());
				}						
			}
			logger.info ("Seasons utilities initialization done.");
		} catch (IOException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			String errMsg = Strings.failedInitializationSeason + seasonId + String.format(Strings.unableToReadFile,Constants.AIRLOCK_UTILITIES_FILE_NAME) + e.getMessage();
			logger.severe(errMsg);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			throw new RuntimeException(errMsg);						
		} catch (JSONException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			String errMsg = Strings.failedInitializationSeason + seasonId + String.format(Strings.failedInitializationFileUnexpectedFormat,Constants.AIRLOCK_UTILITIES_FILE_NAME) + e.getMessage();
			logger.severe(errMsg);		
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			throw new RuntimeException(errMsg);						
		}
				
		try {	
			logger.info ("Initalizing season streams from: " + Constants.SEASONS_FOLDER_NAME);
			LinkedList<JSONObject> seasonStreamsList = getSeasonFilesJSONs(Constants.AIRLOCK_STREAMS_FILE_NAME, false, ds);
			for (JSONObject seasonStreams: seasonStreamsList) {
				try { 
					seasonId = seasonStreams.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID);
					seasonId = Utilities.initFromSeasonStreamsJSON(seasonStreams, context);
					logger.info ("season streams '" + seasonId + "' initialization done.");
					Season season = seasonsDB.get(seasonId);
					
					JSONObject streamsJson = season.getStreams().toJson(OutputJSONMode.ADMIN);
					season.getInputSchema().mergeSchema(streamsJson, false, null);					
				} catch (IllegalArgumentException iax) {
					//this error is thrown when the season does not exists in the products file
					//This can happend if the season was deleted but some files (in this case the AirlockUtilities.json) was not deleted 
					//from S3. in this case - report error and continue with next seasons
					logger.warning ("Streams for season '" + seasonStreams.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID) + "'  exist but the season was already deleted:" + iax.getMessage());
				} catch (GenerationException e) {
					context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
					String errMsg = "Failed to merge the streams results schema into the main schema: " + e.getMessage();
					logger.severe(errMsg);		
					logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
					throw new RuntimeException(errMsg);
				}
			}
			logger.info ("Seasons streams initialization done.");
		} catch (IOException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			String errMsg = Strings.failedInitializationSeason + seasonId + String.format(Strings.unableToReadFile,Constants.AIRLOCK_STREAMS_FILE_NAME) + e.getMessage();
			logger.severe(errMsg);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			throw new RuntimeException(errMsg);						
		} catch (JSONException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			String errMsg = Strings.failedInitializationSeason + seasonId + String.format(Strings.failedInitializationFileUnexpectedFormat,Constants.AIRLOCK_STREAMS_FILE_NAME) + e.getMessage();
			logger.severe(errMsg);		
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			throw new RuntimeException(errMsg);						
		}
		
		try {	
			logger.info ("Initalizing season streams events from: " + Constants.SEASONS_FOLDER_NAME);
			LinkedList<JSONObject> seasonStreamsEventsList = getSeasonFilesJSONs(Constants.AIRLOCK_STREAMS_EVENTS_FILE_NAME, false, ds);
			for (JSONObject streamsEvents: seasonStreamsEventsList) {
				try { 
					seasonId = streamsEvents.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID);
					seasonId = Utilities.initFromSeasonStreamsEventsJSON(streamsEvents, context);
					logger.info ("season streams events '" + seasonId + "' initialization done.");
					Season season = seasonsDB.get(seasonId);
					
					JSONObject streamsJson = season.getStreams().toJson(OutputJSONMode.ADMIN);
					season.getInputSchema().mergeSchema(streamsJson, false, null);					
				} catch (IllegalArgumentException iax) {
					//this error is thrown when the season does not exists in the products file
					//This can happend if the season was deleted but some files (in this case the AirlockUtilities.json) was not deleted 
					//from S3. in this case - report error and continue with next seasons
					logger.warning ("Streams events for season '" + streamsEvents.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID) + "'  exist but the season was already deleted:" + iax.getMessage());
				} catch (GenerationException e) {
					context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
					String errMsg = "Failed to merge the streams results schema into the main schema: " + e.getMessage();
					logger.severe(errMsg);		
					logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
					throw new RuntimeException(errMsg);
				}
			}
			logger.info ("Seasons streams events initialization done.");
		} catch (IOException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			String errMsg = Strings.failedInitializationSeason + seasonId + String.format(Strings.unableToReadFile,Constants.AIRLOCK_STREAMS_FILE_NAME) + e.getMessage();
			logger.severe(errMsg);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			throw new RuntimeException(errMsg);						
		} catch (JSONException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			String errMsg = Strings.failedInitializationSeason + seasonId + String.format(Strings.failedInitializationFileUnexpectedFormat,Constants.AIRLOCK_STREAMS_EVENTS_FILE_NAME) + e.getMessage();
			logger.severe(errMsg);		
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			throw new RuntimeException(errMsg);						
		}

		try {	
			logger.info ("Initalizing season notifications from: " + Constants.SEASONS_FOLDER_NAME);
			LinkedList<JSONObject> seasonNotificationsList = getSeasonFilesJSONs(Constants.AIRLOCK_NOTIFICATIONS_FILE_NAME, false, ds);
			for (JSONObject seasonNotifications: seasonNotificationsList) {
				try { 
					seasonId = seasonNotifications.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID);
					seasonId = Utilities.initFromSeasonNotificationsJSON(seasonNotifications, context);
					logger.info ("season notifications '" + seasonId + "' initialization done.");					
				} catch (IllegalArgumentException iax) {
					//this error is thrown when the season does not exists in the products file
					//This can happen if the season was deleted but some files (in this case the AirlockNotifications.json) was not deleted 
					//from S3. in this case - report error and continue with next seasons
					logger.warning ("Notifications for season '" + seasonId + "'  exist but the season was already deleted:" + iax.getMessage());
				}
			}
			logger.info ("Seasons notifications initialization done.");
		} catch (IOException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			String errMsg = Strings.failedInitializationSeason + seasonId + String.format(Strings.unableToReadFile,Constants.AIRLOCK_NOTIFICATIONS_FILE_NAME) + e.getMessage();
			logger.severe(errMsg);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			throw new RuntimeException(errMsg);						
		} catch (JSONException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			String errMsg = Strings.failedInitializationSeason + seasonId + String.format(Strings.failedInitializationFileUnexpectedFormat,Constants.AIRLOCK_STREAMS_FILE_NAME) + e.getMessage();
			logger.severe(errMsg);		
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			throw new RuntimeException(errMsg);						
		}
		
		try {	
			logger.info ("Initalizing season analytics from: " + Constants.SEASONS_FOLDER_NAME);
			//LinkedList<JSONObject> seasonAnalyticssList = ds.listFilesData(Constants.SEASONS_FOLDER_NAME, Constants.AIRLOCK_ANALYTICS_FILE_NAME);
			LinkedList<JSONObject> seasonAnalyticsList = getSeasonFilesJSONs(Constants.AIRLOCK_ANALYTICS_FILE_NAME, false, ds);
			for (JSONObject seasonAnalytics: seasonAnalyticsList) {
				try {
					seasonId = seasonAnalytics.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID);
					
					seasonId = Utilities.initFromSeasonAnalyticsJSON(seasonAnalytics, context);
					logger.info ("season analytics '" + seasonId + "' initialization done.");	
				} catch (IllegalArgumentException iax) {
					//this error is thrown when the season does not exists in the products file
					//This can happen if the season was deleted but some files (in this case the AirlockInputShema.json) was not deleted 
					//from S3. in this case - report error and continue with next seasons
					logger.warning ("Analytics for season '" + seasonAnalytics.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID) + "' exists but the season was already deleted:" + iax.getMessage());
				}
			}
			logger.info ("Seasons analytics initialization done.");		
		} catch (IOException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			String errMsg = Strings.failedInitializationSeason + seasonId + String.format(Strings.unableToReadFile,Constants.AIRLOCK_ANALYTICS_FILE_NAME) + e.getMessage();
			logger.severe(errMsg);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			throw new RuntimeException(errMsg);						
		} catch (JSONException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			String errMsg = Strings.failedInitializationSeason + seasonId + String.format(Strings.failedInitializationFileUnexpectedFormat,Constants.AIRLOCK_ANALYTICS_FILE_NAME) + e.getMessage();
			logger.severe(errMsg);		
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			throw new RuntimeException(errMsg);						
		}

		//init products followers from S3 file
		try {
			logger.info ("Initalizing products followers from: " + Constants.FOLLOWERS_PRODUCTS_FILE_NAME);
			if (ds.isFileExists(Constants.FOLLOWERS_PRODUCTS_FILE_NAME)) {
				JSONObject followersJson = ds.readDataToJSON(Constants.FOLLOWERS_PRODUCTS_FILE_NAME);
				Utilities.initProductFollowersJSON (followersJson, context);
			} else {
				String emptyFollowers = "{\"allFollowers\":[]}";
				ds.writeData(Constants.FOLLOWERS_PRODUCTS_FILE_NAME, emptyFollowers);
			}
			logger.info ("Products followers initialization done.");
		} catch (IOException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			String error = String.format(Strings.failedInitializationReadingFile,Constants.FOLLOWERS_PRODUCTS_FILE_NAME) + e.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			throw new RuntimeException(error);
		} catch (JSONException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			String errMsg = String.format(Strings.failedInitializationInvalidJson,Constants.FOLLOWERS_PRODUCTS_FILE_NAME) + e.getMessage();
			logger.severe(errMsg);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			throw new RuntimeException(errMsg);
		}
		
		try {
			logger.info ("Initalizing features followers from: " + Constants.FOLLOWERS_FEATURES_FILE_NAME);
			//LinkedList<JSONObject> seasonFeaturesFollowersList = ds.listFilesData(Constants.SEASONS_FOLDER_NAME, Constants.FOLLOWERS_FEATURES_FILE_NAME);
			LinkedList<JSONObject> seasonFeaturesFollowersList = getSeasonFilesJSONs(Constants.FOLLOWERS_FEATURES_FILE_NAME, false, ds);
			for (JSONObject seasonFeaturesFollowers: seasonFeaturesFollowersList) {
				Utilities.initFeatureFollowersJSON(seasonFeaturesFollowers, context, seasonsDB);
			}
			logger.info("Features followers initialization done.");

		} catch (IOException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			String errMsg = String.format(Strings.failedInitializationReadingFile,Constants.FOLLOWERS_FEATURES_FILE_NAME) + e.getMessage();
			logger.severe(errMsg);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");			throw new RuntimeException(errMsg);
		} catch (JSONException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			String errMsg = Strings.failedInitializationFollowersUnexpectedFormat + e.getMessage();
			logger.severe(errMsg);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			throw new RuntimeException(errMsg);
		}

		try {	
			logger.info ("Initalizing season branches from: " + Constants.SEASONS_FOLDER_NAME);
			//LinkedList<JSONObject> seasonAnalyticssList = ds.listFilesData(Constants.SEASONS_FOLDER_NAME, Constants.AIRLOCK_ANALYTICS_FILE_NAME);
			LinkedList<JSONObject> seasonBranchesList = getSeasonFilesJSONs(Constants.AIRLOCK_BRANCHES_FILE_NAME, false, ds);
			for (JSONObject seasonBranch: seasonBranchesList) {
				try {
					seasonId = seasonBranch.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID);
					seasonId = Utilities.initFromSeasonBranchesJSON(seasonBranch, context, ds);
					logger.info ("season branches '" + seasonId + "' initialization done.");	
				} catch (IllegalArgumentException iax) {
					//this error is thrown when the season does not exists in the products file
					//This can happen if the season was deleted but some files (in this case the AirlockInputShema.json) was not deleted 
					//from S3. in this case - report error and continue with next seasons
					logger.warning ("Branches for season '" + seasonBranch.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID) + "' exists but the season was already deleted:" + iax.getMessage());
				}
			}
			logger.info ("Seasons branches initialization done.");		
		} catch (IOException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			String errMsg = Strings.failedInitializationSeason + seasonId + String.format(Strings.unableToReadFile,Constants.AIRLOCK_ANALYTICS_FILE_NAME) + e.getMessage();
			logger.severe(errMsg);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			throw new RuntimeException(errMsg);						
		} catch (JSONException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			String errMsg = Strings.failedInitializationSeason + seasonId + String.format(Strings.failedInitializationFileUnexpectedFormat,Constants.AIRLOCK_ANALYTICS_FILE_NAME) + e.getMessage();
			logger.severe(errMsg);		
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			throw new RuntimeException(errMsg);						
		}
		
		try {
			initializeProductsExperiments(context);
		} catch (IOException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			String errMsg = Strings.failedInitialization + e.getMessage();
			logger.severe(errMsg);			
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			throw new RuntimeException(errMsg);						
		} catch (JSONException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			String errMsg =Strings.failedInitialization + e.getMessage();
			logger.severe(errMsg);		
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			throw new RuntimeException(errMsg);						
		}

		try {
			initializeProductsAirlockUsersAutherization(context);
			//if (!(Boolean)context.getAttribute(Constants.SKIP_AUTHENTICATION_PARAM_NAME)) {
			//load users roles even if the server is not authenticated
			AirLockContextListener.reloadUserRolesPerProduct(context);
			//}
		} catch (IOException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			String errMsg = Strings.failedInitialization + e.getMessage();
			logger.severe(errMsg);			
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			throw new RuntimeException(errMsg);						
		} catch (JSONException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			String errMsg =Strings.failedInitialization + e.getMessage();
			logger.severe(errMsg);		
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			throw new RuntimeException(errMsg);						
		} catch (Exception e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			String err = "Failed to initialize the Airlock service. Unable to initialize the authentication configuration: " + e.getMessage();
			logger.severe(err);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			throw new RuntimeException(err);	        	
		}
		
		try {
			initializeProductsUserGroups(context);
			if (!(Boolean)context.getAttribute(Constants.SKIP_AUTHENTICATION_PARAM_NAME)) {
				AirLockContextListener.reloadUserRolesPerProduct(context);
			}
		} catch (IOException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			String errMsg = Strings.failedInitialization + e.getMessage();
			logger.severe(errMsg);			
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			throw new RuntimeException(errMsg);						
		} catch (JSONException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			String errMsg =Strings.failedInitialization + e.getMessage();
			logger.severe(errMsg);		
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			throw new RuntimeException(errMsg);						
		} catch (Exception e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			String err = "Failed to initialize the Airlock service. Unable to initialize the authentication configuration: " + e.getMessage();
			logger.severe(err);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			throw new RuntimeException(err);	        	
		}
		initializeSmartling((String)(context.getAttribute(Constants.ENV_LOGS_FOLDER_PATH)));
		
		logger.info (Strings.changeAirlockSerevrStateTo + "RUNNING.");
		context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.RUNNING);
	}

	private void initializeProductsExperiments(ServletContext context) throws IOException, JSONException {
		
		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);		
		Set<String> products = productsDB.keySet();
		for (String prodId:products) {
			Product prod = productsDB.get(prodId);

			String separator = ds.getSeparator();
			try {
				String filePath = Constants.SEASONS_FOLDER_NAME+separator + prod.getUniqueId().toString() + 
								separator+Constants.AIRLOCK_EXPERIMENTS_FILE_NAME;
				
				prod.setExperimentsMutualExclusionGroup(new ExperimentsMutualExclusionGroup(prod.getUniqueId()));				
				
				if (!ds.isFileExists(filePath)) {
					System.out.println("filePath = " + filePath + " does not exist and is not mandatory - skipping");
					continue;
				}
						
				System.out.println("filePath = " + filePath);
						
				JSONObject json = ds.readDataToJSON(filePath);
				
				prod.getExperimentsMutualExclusionGroup().fromJSON(json, context);
			} catch (IOException ioe) {
				throw new IOException("IOException while reading the experiments file of product " + prod.getUniqueId().toString() + ", '" + prod.getName() + "': " + ioe.getMessage());
			} catch (JSONException je) {
				throw new IOException("JSONException while reading the experiments file of product " + prod.getUniqueId().toString() + ", '" + prod.getName() + "': " + je.getMessage());
			}
		}		
	}

	private void initializeProductsAirlockUsersAutherization(ServletContext context) throws IOException, JSONException {
		
		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);		
		Set<String> products = productsDB.keySet();
		for (String prodId:products) {
			Product prod = productsDB.get(prodId);

			String separator = ds.getSeparator();
			try {
				String filePath = Constants.SEASONS_FOLDER_NAME+separator + prod.getUniqueId().toString() + 
								separator+Constants.AIRLOCK_USERS_FILE_NAME;
				
				if (!ds.isFileExists(filePath)) {
					System.out.println("filePath = " + filePath + " does not exist and is not mandatory - skipping");
					continue;
				}
						
				System.out.println("filePath = " + filePath);
						
				JSONObject airlockUsersJSON = ds.readDataToJSON(filePath);
				
				Utilities.initFromAirlockUsersJSON (airlockUsersJSON, context, prod);
				
			} catch (IOException ioe) {
				throw new IOException("IOException while reading the Airlock users file of product " + prod.getUniqueId().toString() + ", '" + prod.getName() + "': " + ioe.getMessage());
			} catch (JSONException je) {
				throw new IOException("JSONException while reading the Airlock users file of product " + prod.getUniqueId().toString() + ", '" + prod.getName() + "': " + je.getMessage());
			}
		}		
	}

	private void initializeProductsUserGroups(ServletContext context) throws IOException, JSONException {
		
		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);		
		Set<String> products = productsDB.keySet();
		JSONObject globalUserGroupsJSON = null; //exists since pre 5_0 server has global and not per product user groups
		
		for (String prodId:products) {
			Product prod = productsDB.get(prodId);

			String separator = ds.getSeparator();
			try {
				String filePath = Constants.SEASONS_FOLDER_NAME+separator + prod.getUniqueId().toString() + 
								separator+Constants.USER_GROUPS_FILE_NAME;
				System.out.println("filePath = " + filePath);
				if (!ds.isFileExists(filePath)) {
					if (globalUserGroupsJSON == null) {
						//try looking for global internal user groups
						String globalInternalUserGroupsfilePath = Constants.USER_GROUPS_FILE_NAME;
						if (!ds.isFileExists(globalInternalUserGroupsfilePath)) {
							throw new IOException("Internal user groups file of product " + prod.getUniqueId().toString() + ", '" + prod.getName() + "': does not exist and global internal groups does not exist as well." );
						}
						globalUserGroupsJSON = ds.readDataToJSON(globalInternalUserGroupsfilePath);						
					}
					InternalUserGroups userGroups = Utilities.initFromUserGroupsJson(globalUserGroupsJSON, context, prod.getUniqueId().toString());
					AirlockFilesWriter.writeUserGroups(userGroups, context, prod);
					System.out.println("filePath = " + filePath + " does not exist and is not mandatory - copying user groups from root");
					continue;
				}
				else {							
					JSONObject userGroupsJSON = ds.readDataToJSON(filePath);
					Utilities.initFromUserGroupsJson(userGroupsJSON, context, prod.getUniqueId().toString());
				}
			} catch (IOException ioe) {
				throw new IOException("IOException while reading the internal user groups file of product " + prod.getUniqueId().toString() + ", '" + prod.getName() + "': " + ioe.getMessage());
			} catch (JSONException je) {
				throw new IOException("JSONException while reading the internal user groups file of product " + prod.getUniqueId().toString() + ", '" + prod.getName() + "': " + je.getMessage());
			}
		}		
	}

	private LinkedList<JSONObject> getSeasonFilesJSONs(String airlockFileName, boolean mustExists, DataSerializer ds ) throws IOException, JSONException {
		String separator = ds.getSeparator();
		LinkedList<JSONObject> res = new LinkedList<JSONObject>();
		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);		
		Set<String> products = productsDB.keySet();
		for (String prodId:products) {
			Product prod = productsDB.get(prodId);			
			for (Season season:prod.getSeasons()) {
				String filePath = Constants.SEASONS_FOLDER_NAME+separator+season.getProductId().toString()+separator+season.getUniqueId().toString()+separator+airlockFileName;
				if (!mustExists && !ds.isFileExists(filePath)) {
					System.out.println("filePath = " + filePath + " does not exist and is not mandatory - skipping");				
					continue;
				}
				
				System.out.println("filePath = " + filePath);
				
				JSONObject json = ds.readDataToJSON(filePath);
				res.add(json);
							
			}
		}
		return res;		
	}
	
	enum TranslatorMode { OFF, SMARTLING }; // add new 3rd party translators as needed
	private void initializeSmartling(String logsFolderPath)
	{
		String str = AirLockContextListener.getEnv(Constants.ENV_TRANSLATOR_MODE);
		TranslatorMode mode = str.isEmpty() ? null : TranslatorMode.valueOf(str);
		if (mode == null)
			mode = TranslatorMode.OFF;

		logger.info ("Third Party Translator mode: " + mode);
		if (mode == TranslatorMode.OFF)
			return;

		try {
			if (ds.isFileExists(Constants.SMARTLING_LOCALE_FILE_OVERRIDE))
			{
				String content = ds.readDataToString(Constants.SMARTLING_LOCALE_FILE_OVERRIDE);
				SmartlingLocales.updateMap(content);
			}
		}
		catch (Exception e)
		{
			logger.warning (Constants.SMARTLING_LOCALE_FILE_OVERRIDE + " could not be loaded. Using default smartling locale mapping.\n" + e.toString());
		}


		SmartlingClient client = getSmartlingClient();
		if (client == null)
			return;

		TranslatorLogWriter smartlingLogger = null;
		try {
			smartlingLogger = new TranslatorLogWriter(logsFolderPath);
		}
		catch (Exception e)
		{
			logger.warning ("background translator logger could not be initialized. Automatic translation is disabled: " + e.toString());
			return;
		}

		//smartlingLogger.setFine(mode == SmartlingMode.DEBUG);
		client.setLogger(smartlingLogger); // logs http calls in debug mode

		// ping to verify the connection
		try {
			JSONObject locales = client.getProjectLocales(null);
			smartlingLogger.log("background translator server information: " + locales.write());
		}
		catch (Exception e)
		{
			logger.warning ("background translator initialization error. Automatic translation is disabled: " + e.toString());
			return;
		}

		BackgroundTranslator smartling = new BackgroundTranslator(client, context, smartlingLogger);
		context.setAttribute(Constants.BACKGROUND_TRANSLATOR, smartling);
		smartling.start();
	}

	/*old code
	SmartlingClient oldSmartlingClient()
	{
		String smartlingUser = AirLockContextListener.getEnv(Constants.SMARTLING_USER);
		String smartlingSecret = AirLockContextListener.getEnv(Constants.SMARTLING_SECRET);
		String smartlingProject = AirLockContextListener.getEnv(Constants.SMARTLING_PROJECT);


		if (smartlingUser.isEmpty() || smartlingSecret.isEmpty() || smartlingProject.isEmpty())
		{
			logger.info ("Smartling configuration is missing in environment. Automatic translation is disabled.");
			return null;
		}

		return new SmartlingClient(smartlingUser, smartlingSecret, smartlingProject);
	}*/

	// new code
	SmartlingClient getSmartlingClient()
	{
		String smartlingFile = AirLockContextListener.getEnv(Constants.ENV_SMARTLING_CONFIG_FILE);
		if (smartlingFile.isEmpty())
			smartlingFile = Constants.SMARTLING_CONFIG_FILE_DEFAULT;

		String config;
		try {
			config = ds.readDataToString(smartlingFile);
			// config = S3Encryptor.getFile(smartlingFile);
		}
		catch (Exception e)
		{
			logger.warning (smartlingFile + " could not be read. Automatic translation is disabled.\n" + e.getMessage());
			return null;
		}

		String smartlingUser, smartlingSecret, smartlingProject;
		try {
			JSONObject json = new JSONObject(config);
			smartlingUser = json.getString("smartlingUser");
			smartlingSecret = json.getString("smartlingSecret");
			smartlingProject = json.getString("smartlingProject");
		}
		catch (Exception e)
		{
			logger.warning (smartlingFile + " is not in expected format. Automatic translation is disabled.\n" + e.getMessage());
			return null;
		}

		return new SmartlingClient(smartlingUser, smartlingSecret, smartlingProject);
	}
}
