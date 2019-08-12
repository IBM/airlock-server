package com.ibm.airlock;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import com.amazonaws.regions.Regions;
import com.ibm.airlock.admin.Product;
import com.ibm.airlock.admin.AirlockServers;
import com.ibm.airlock.admin.AirlockServers.AirlockServer;
import com.ibm.airlock.admin.AirlockUtility;
import com.ibm.airlock.admin.BaseAirlockItem;
import com.ibm.airlock.admin.Branch;
import com.ibm.airlock.admin.InitializationThread;
import com.ibm.airlock.admin.InternalUserGroups;
import com.ibm.airlock.admin.Season;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.analytics.Experiment;
import com.ibm.airlock.admin.analytics.Variant;
import com.ibm.airlock.admin.authentication.AzurAD;
import com.ibm.airlock.admin.authentication.BlueId;
import com.ibm.airlock.admin.authentication.JwtData;
import com.ibm.airlock.admin.authentication.Okta;
import com.ibm.airlock.admin.authentication.Providers;
import com.ibm.airlock.admin.authentication.UserRoles;
import com.ibm.airlock.admin.notifications.AirlockNotification;
import com.ibm.airlock.admin.operations.AirlockAPIKeys;
import com.ibm.airlock.admin.operations.AirlockCapabilities;
//import com.ibm.airlock.admin.operations.AirlockUsers;
import com.ibm.airlock.admin.operations.UserRoleSets;
import com.ibm.airlock.admin.operations.UserRoleSets.UserRoleSet;
import com.ibm.airlock.admin.operations.Webhooks;
import com.ibm.airlock.admin.operations.Roles;
import com.ibm.airlock.admin.serialize.*;
import com.ibm.airlock.admin.streams.AirlockStream;
import com.ibm.airlock.admin.translations.OriginalString;
import com.ibm.airlock.admin.translations.BackgroundTranslator;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;




@WebListener
public class AirLockContextListener implements ServletContextListener {

	public static final Logger logger = Logger.getLogger(AirLockContextListener.class.getName());

	public void contextInitialized(ServletContextEvent servletContextEvent)
	{
		ServletContext context = servletContextEvent.getServletContext();

		context.setAttribute(Constants.FEATURES_DB_PARAM_NAME,  new ConcurrentHashMap<String, BaseAirlockItem>());
		//context.setAttribute(Constants.PURCHASES_DB_PARAM_NAME, new ConcurrentHashMap<String, BaseAirlockItem>());	
		context.setAttribute(Constants.PRODUCTS_DB_PARAM_NAME, new ConcurrentHashMap<String, Product>());		
		context.setAttribute(Constants.FOLLOWERS_PRODUCTS_DB_PARAM_NAME, new ConcurrentHashMap<String, ArrayList<String>>());
		context.setAttribute(Constants.FOLLOWERS_FEATURES_DB_PARAM_NAME, new ConcurrentHashMap<String, ArrayList<String>>());
		context.setAttribute(Constants.SEASONS_DB_PARAM_NAME, new ConcurrentHashMap<String, Season>());
		context.setAttribute(Constants.UTILITIES_DB_PARAM_NAME, new ConcurrentHashMap<String, AirlockUtility>());
		context.setAttribute(Constants.STREAMS_DB_PARAM_NAME, new ConcurrentHashMap<String, AirlockStream>());
		context.setAttribute(Constants.NOTIFICATIONS_DB_PARAM_NAME, new ConcurrentHashMap<String, AirlockNotification>());
		context.setAttribute(Constants.ORIG_STRINGS_DB_PARAM_NAME, new ConcurrentHashMap<String, OriginalString>());
		context.setAttribute(Constants.EXPERIMENTS_DB_PARAM_NAME, new ConcurrentHashMap<String, Experiment>());
		context.setAttribute(Constants.BRANCHES_DB_PARAM_NAME, new ConcurrentHashMap<String, Branch>());
		context.setAttribute(Constants.VARIANTS_DB_PARAM_NAME, new ConcurrentHashMap<String, Variant>());
		context.setAttribute(Constants.ROLES_PARAM_NAME, new Roles());
		context.setAttribute(Constants.AIRLOCK_SERVERS_PARAM_NAME, new AirlockServers());
		context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.INITIALIZING);
		context.setAttribute(Constants.GLOBAL_LOCK_PARAM_NAME, new ReentrantReadWriteLock());
		context.setAttribute(Constants.API_KEYS_PARAM_NAME, new AirlockAPIKeys());
		
		context.setAttribute(Constants.WEBHOOKS_PARAM_NAME, new Webhooks());
		context.setAttribute(Constants.CAPABILITIES_PARAM_NAME, new AirlockCapabilities());
		context.setAttribute(Constants.USER_GROUPS_PER_PRODUCT_PARAM_NAME, new ConcurrentHashMap<String,InternalUserGroups>());
		context.setAttribute(Constants.USER_ROLES_PER_PRODUCT_PARAM_NAME, new ConcurrentHashMap<String,UserRoles>());
		
		//Airlock users db (contains both global and per product users)
		context.setAttribute(Constants.AIRLOCK_USERS_DB_PARAM_NAME, new ConcurrentHashMap<String, UserRoleSet>());
		
		//global users collection
		context.setAttribute(Constants.AIRLOCK_GLOBAL_USERS_PARAM_NAME, new UserRoleSets());
		
		
		//logsFolderPath
		String logsFolderPath = getEnv(Constants.ENV_LOGS_FOLDER_PATH);	
		
		if (logsFolderPath == null || logsFolderPath.isEmpty()) {
			String err = "Failed initializing the Airlock service. " +  Constants.ENV_LOGS_FOLDER_PATH + " environment parameter is missing.";
			logger.severe(err);
			throw new RuntimeException(err);
		}
		
		logger.info(Constants.ENV_LOGS_FOLDER_PATH + " = " + logsFolderPath);
		context.setAttribute(Constants.ENV_LOGS_FOLDER_PATH, logsFolderPath);
				
		try {
			context.setAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME, new AuditLogWriter(logsFolderPath));
		} catch (IOException e) {
			logger.severe("Fail creating audit log: " + e.getMessage());
			throw new RuntimeException(e);
		}		

		DataSerializer dataSerializer = null;
		String envStorgeParams = getEnv(Constants.ENV_STORAGE_PARAMS);
		logger.info(Constants.ENV_STORAGE_PARAMS + " - " + envStorgeParams);
		if (envStorgeParams == null || envStorgeParams.isEmpty()) {
			String err = "Failed initializing the Airlock service. " + Constants.ENV_STORAGE_PARAMS + " environment parameter is missing.";
			logger.severe(err);
			throw new RuntimeException(err);
		}
		dataSerializer = buildDataSerializerFromStorageParams(envStorgeParams);


		context.setAttribute(Constants.DATA_SERIALIZER_PARAM_NAME, dataSerializer);

		String runtimeFullPath = dataSerializer.getRuntimePublicFullPath();
		logger.info("runtimeFullPath = " + runtimeFullPath);
		if (runtimeFullPath == null || runtimeFullPath.isEmpty()) {
			String err = "Failed initializing the Airlock service. runtimeFullPath is missing.";
			logger.severe(err);
			throw new RuntimeException(err);
		}
		
		context.setAttribute(Constants.RUNTIME_PUBLIC_FULL_PATH_PARAM_NAME, runtimeFullPath);

		String storagePublicPath = dataSerializer.getStoragePublicPath();
		logger.info("storagePublicPath = " + storagePublicPath);
		
		if (storagePublicPath == null || storagePublicPath.isEmpty()) {
			String err = "Failed initializing the Airlock service. storagePublicPath is missing.";
			logger.severe(err);
			throw new RuntimeException(err);
		}
		
		context.setAttribute(Constants.STORAGE_PUBLIC_PATH_PARAM_NAME, storagePublicPath);

		String serverName = getEnv(Constants.ENV_SERVER_NAME);
		if (serverName == null || serverName.isEmpty()) {
			String err = "Failed initializing the Airlock service. " + Constants.ENV_SERVER_NAME + " environment parameter is missing.";
			logger.severe(err);
			throw new RuntimeException(err);
		}

		String analyticsServerUrl = getEnv(Constants.ENV_ANALYTICS_SERVER_URL);	
		/*
		 //TODO: for now allow missing parameter - in this case - no publishing to analytics server
		if (analyticsServerUrl == null || analyticsServerUrl.isEmpty()) {
			String err = "Failed initializing the Airlock service. " +  Constants.ENV_ANALYTICS_SERVER_URL + " environment parameter is missing.";
			logger.severe(err);
			throw new RuntimeException(err);
		}*/
		
		logger.info(Constants.ANALYTICS_SERVER_URL_PARAM_NAME + " = " + analyticsServerUrl);		
		context.setAttribute(Constants.ANALYTICS_SERVER_URL_PARAM_NAME, analyticsServerUrl);
		
		//airlockChangesMailAddress
		String airlockChangesMailAddress = getEnv(Constants.ENV_AIRLOCK_CHANGES_MAIL_ADDRESS);	
		
		if (airlockChangesMailAddress == null || airlockChangesMailAddress.isEmpty()) {
			String err = "Failed initializing the Airlock service. " +  Constants.ENV_AIRLOCK_CHANGES_MAIL_ADDRESS + " environment parameter is missing.";
			logger.severe(err);
			throw new RuntimeException(err);
		}
		
		logger.info(Constants.AIRLOCK_CHANGES_MAIL_ADDRESS_PARAM_NAME + " = " + airlockChangesMailAddress);		
		context.setAttribute(Constants.AIRLOCK_CHANGES_MAIL_ADDRESS_PARAM_NAME, airlockChangesMailAddress);		
		
		//emailProviderTypeStr
		String emailProviderTypeStr = getEnv(Constants.ENV_EMAIL_PROVIDER_TYPE);	
		
		if (emailProviderTypeStr == null || emailProviderTypeStr.isEmpty()) {
			String err = "Failed initializing the Airlock service. " +  Constants.ENV_EMAIL_PROVIDER_TYPE + " environment parameter is missing.";
			logger.severe(err);
			throw new RuntimeException(err);
		}
		Constants.EmailProviderType emailProviderType = Utilities.valueOf(Constants.EmailProviderType.class, emailProviderTypeStr);
		if (emailProviderType == null) { 
			String err = "Failed initializing the Airlock service. Illegal value '" + emailProviderTypeStr + "' for " + Constants.ENV_EMAIL_PROVIDER_TYPE + " environment parameter.";
			logger.severe(err);
			throw new RuntimeException(err);
		}
		logger.info(Constants.ENV_EMAIL_PROVIDER_TYPE + " = " + emailProviderTypeStr);		
		context.setAttribute(Constants.ENV_EMAIL_PROVIDER_TYPE, emailProviderType);
		
		if (emailProviderType.equals(Constants.EmailProviderType.SEND_GRID)) {
			//sendGridApiKey
			String sendGridApiKey = getEnv(Constants.ENV_SEND_GRID_API_KEY);	
			
			if (sendGridApiKey == null || sendGridApiKey.isEmpty()) {
				String err = "Failed initializing the Airlock service. " +  Constants.ENV_SEND_GRID_API_KEY + " environment parameter is missing.";
				logger.severe(err);
				throw new RuntimeException(err);
			}
			
			logger.info(Constants.ENV_SEND_GRID_API_KEY + " = " + sendGridApiKey);		
			context.setAttribute(Constants.ENV_SEND_GRID_API_KEY, sendGridApiKey);		
		}
		
		String serverDisplayName = getEnv(Constants.ENV_SERVER_DISPLAY_NAME);
		if (serverDisplayName == null || serverDisplayName.isEmpty())
			serverDisplayName = Constants.SERVER_DEFAULT_DISPLAY_NAME; 
		
		context.setAttribute(Constants.AIRLOCK_SERVER_DISPLAY_NAME_PARAM_NAME, serverDisplayName);
		
		logger.info(Constants.AIRLOCK_SERVER_DISPLAY_NAME_PARAM_NAME + " = " + serverDisplayName);
		
		//skip authentication
		String skipAuthStr = getEnv(Constants.ENV_SKIP_AUTHENTICATION);	
		Boolean skipAuth = new Boolean(skipAuthStr);

		logger.info(Constants.ENV_SKIP_AUTHENTICATION + " = " + skipAuth);
		context.setAttribute(Constants.SKIP_AUTHENTICATION_PARAM_NAME, skipAuth);

	/*	//runtime encryption
		String runtimeEncryptionStr = getEnv(Constants.ENV_RUNTIME_ENCRYPTION);	
		if (runtimeEncryptionStr == null || runtimeEncryptionStr.isEmpty()) {
			String err = "Failed initializing the Airlock service. " +  Constants.ENV_RUNTIME_ENCRYPTION + " environment parameter is missing.";
			logger.severe(err);
			throw new RuntimeException(err);
		}
		Boolean runtimeEncryption = new Boolean(runtimeEncryptionStr);

		logger.info(Constants.ENV_RUNTIME_ENCRYPTION + " = " + runtimeEncryption);
		context.setAttribute(Constants.ENV_RUNTIME_ENCRYPTION, runtimeEncryption);
*/
		String jwtExpirationMinStr = getEnv(Constants.ENV_JWT_EXPIRATION_MIN);
		Integer jwtExpirationMin = null;
		if (!jwtExpirationMinStr.isEmpty()) {
			jwtExpirationMin = new Integer(jwtExpirationMinStr);
			JwtData.setExpirationMin(jwtExpirationMin);
		}

		
		logger.info(Constants.ENV_JWT_EXPIRATION_MIN + " = " + (jwtExpirationMin == null ? "not specified" : jwtExpirationMin));
		context.setAttribute(Constants.JWT_EXPIRATION_MIN_PARAM_NAME, jwtExpirationMin);

		context.setAttribute(Constants.IS_TEST_MODE, false);

		String consoleUrl = getEnv(Constants.ENV_CONSOLE_URL);
		logger.info(Constants.ENV_CONSOLE_URL + " = " + consoleUrl);
		context.setAttribute(Constants.CONSOLE_URL, consoleUrl);

		String sesEndpoint = getEnv(Constants.ENV_SES_ENDPOINT);
		if(sesEndpoint.equals("")){ // default is prod endpoint
			sesEndpoint = "us-east-1";
		}
		else{
			try{
				Regions.fromName(sesEndpoint);
			}catch (Exception e){
				String err = "Failed initializing the Airlock service. " +  Constants.ENV_SES_ENDPOINT + " environment parameter is not a valid region.";
				logger.severe(err);
				throw new RuntimeException(err);
			}
		}
		logger.info(Constants.ENV_SES_ENDPOINT + " = " + sesEndpoint);
		context.setAttribute(Constants.SES_ENDPOINT, sesEndpoint);

		//Init data for schema validation and store in context
		String ajv = getResource(Constants.SCHEMA_VALIDATOR_SCRIPTS_FOLDER_NAME, "ajv.min.js");
		context.setAttribute(Constants.SCHEMA_VALIDATOR_AJV_PARAM_NAME, ajv);

		String validator = getResource(Constants.SCHEMA_VALIDATOR_SCRIPTS_FOLDER_NAME, "validator.js");
		context.setAttribute(Constants.SCHEMA_VALIDATOR_VALIDATOR_PARAM_NAME, validator);
		
		String faker = getResource(Constants.SCHEMA_VALIDATOR_SCRIPTS_FOLDER_NAME, "json-schema-faker.min.js");
		context.setAttribute(Constants.SCHEMA_JSON_FAKER_PARAM_NAME, faker);
		
		String generator = getResource(Constants.SCHEMA_VALIDATOR_SCRIPTS_FOLDER_NAME, "generator.js");
		context.setAttribute(Constants.SCHEMA_JSON_GENERATOR_PARAM_NAME, generator);
		
		String prune = getResource(Constants.SCHEMA_VALIDATOR_SCRIPTS_FOLDER_NAME, "prune.js");
		context.setAttribute(Constants.SCHEMA_JSON_PRUNE_PARAM_NAME, prune);
		
		String validateLeaves = getResource(Constants.SCHEMA_VALIDATOR_SCRIPTS_FOLDER_NAME, "validateLeaves.js");
		context.setAttribute(Constants.SCHEMA_VALIDATE_LEAVES_PARAM_NAME, validateLeaves);

		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
			
		//init roles from S3 file
		try {
			logger.info ("Initalizing roles from: " + Constants.ROLES_FILE_NAME);
			JSONObject rolesJSON = ds.readDataToJSON(Constants.ROLES_FILE_NAME);			
			Utilities.initFromRolesJSON (rolesJSON, context);
			logger.info ("Roles initialization done.");
		} catch (IOException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			String errMsg = String.format(Strings.failedInitializationReadingFile,Constants.ROLES_FILE_NAME) + e.getMessage();
			logger.severe(errMsg);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			throw new RuntimeException(errMsg);			
		} catch (JSONException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			String errMsg = String.format(Strings.failedInitializationInvalidJson,Constants.ROLES_FILE_NAME) + e.getMessage();
			logger.severe(errMsg);		
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			throw new RuntimeException(errMsg);								
		}
		
		//init webhooks from S3 file
		try {
			logger.info ("Initalizing webhooks from: " + Constants.WEBHOOKS_FILE_NAME);
			JSONObject webhooksJSON = ds.readDataToJSON(Constants.WEBHOOKS_FILE_NAME);			
			Utilities.initFromWebhooksJSON (webhooksJSON, context);
			logger.info ("Webhooks initialization done.");
		} catch (IOException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			String errMsg = String.format(Strings.failedInitializationReadingFile,Constants.WEBHOOKS_FILE_NAME) + e.getMessage();
			logger.severe(errMsg);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			throw new RuntimeException(errMsg);			
		} catch (JSONException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			String errMsg = String.format(Strings.failedInitializationInvalidJson,Constants.WEBHOOKS_FILE_NAME) + e.getMessage();
			logger.severe(errMsg);		
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			throw new RuntimeException(errMsg);								
		}	

		//init airlock users from S3 file
		String globalAirlocUsersFilePath = Constants.OPERATIONS_FOLDER_NAME + Constants.AIRLOCK_USERS_FILE_NAME; 
		try {
			logger.info ("Initalizing airlockUsers from: " + globalAirlocUsersFilePath);
			JSONObject airlockUsersJSON = ds.readDataToJSON(globalAirlocUsersFilePath);
			Utilities.initFromAirlockUsersJSON (airlockUsersJSON, context, null);
			logger.info ("AirlockUsers initialization done.");
		} catch (IOException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			String errMsg = String.format(Strings.failedInitializationReadingFile,globalAirlocUsersFilePath) + e.getMessage();
			logger.severe(errMsg);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			throw new RuntimeException(errMsg);			
		} catch (JSONException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			String errMsg = String.format(Strings.failedInitializationInvalidJson,globalAirlocUsersFilePath) + e.getMessage();
			logger.severe(errMsg);		
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			throw new RuntimeException(errMsg);					
		}	

		//init airlockServers from S3 file
		try {
			logger.info ("Initalizing airlock servers from: " + Constants.AIRLOCK_SERVERS_FILE_NAME);
			if (ds.isFileExists(Constants.AIRLOCK_SERVERS_FILE_NAME)) {
				JSONObject alServersJSON = ds.readDataToJSON(Constants.AIRLOCK_SERVERS_FILE_NAME);			
				Utilities.initFromAirlockServersJSON(alServersJSON, context);
			} else {
				AirlockServers alServers = (AirlockServers)context.getAttribute(Constants.AIRLOCK_SERVERS_PARAM_NAME);		
				AirlockServer alServer = alServers.new AirlockServer();
				
				alServer.setCdnOverride(runtimeFullPath);
				alServer.setUrl(storagePublicPath + ds.getPathPrefix());
				alServer.setDisplayName(serverDisplayName);
				
				alServers.getServers().add(alServer);
				alServers.setLastModified(new Date());
				alServers.setDefaultServer(serverDisplayName);
				
				ds.writeData(Constants.AIRLOCK_SERVERS_FILE_NAME, alServers.toJson(true).write(true));
			}
			logger.info ("Airlock servers initialization done.");
		} catch (IOException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			String errMsg = String.format(Strings.failedInitializationReadingFile,Constants.AIRLOCK_SERVERS_FILE_NAME) + e.getMessage();
			logger.severe(errMsg);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			throw new RuntimeException(errMsg);			
		} catch (JSONException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			String errMsg = String.format(Strings.failedInitializationInvalidJson,Constants.AIRLOCK_SERVERS_FILE_NAME) + e.getMessage();
			logger.severe(errMsg);		
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			throw new RuntimeException(errMsg);								
		}
		//load javascriptUtilities
		try {
			logger.info ("Initalizing javascriptUtilities from: " + Constants.JAVASCRIPT_UTILITIES_FILE_NAME);
			String javascriptUtilitiesStr = ds.readDataToString(Constants.JAVASCRIPT_UTILITIES_FILE_NAME);
			context.setAttribute(Constants.JAVASCRIPT_UTILITIES_PARAM_NAME, javascriptUtilitiesStr);			
			logger.info ("javascriptUtilities initialization done.");
		} catch (IOException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			String errMsg = String.format(Strings.failedInitializationReadingFile,Constants.JAVASCRIPT_UTILITIES_FILE_NAME) + e.getMessage();
			logger.severe(errMsg);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			throw new RuntimeException(errMsg);			
		}			
		
		//load defaultNotificationSchema
		try {
			logger.info ("Initalizing defaultNotificationSchema from: " + Constants.DEFAULT_NOTIFICATION_SCHEMA_FILE_NAME);
			String defaultNotificationSchemaStr = ds.readDataToString(Constants.DEFAULT_NOTIFICATION_SCHEMA_FILE_NAME);
			context.setAttribute(Constants.DEFAULT_NOTIFICATION_SCHEMA_PARAM_NAME, defaultNotificationSchemaStr);			
			logger.info ("defaultNotificationSchemaStr initialization done.");
		} catch (IOException e) {
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			String errMsg = String.format(Strings.failedInitializationReadingFile,Constants.DEFAULT_NOTIFICATION_SCHEMA_FILE_NAME) + e.getMessage();
			logger.severe(errMsg);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			throw new RuntimeException(errMsg);			
		}	
		//calculate users roles even is the server is not authenticated
		reloadUserRoles(context);
		
		if (!skipAuth)
		{
			String authenticationProviderTypeStr = getEnv(Constants.ENV_AUTHENTICATION_PROVIDER_TYPE);	
			
			if (authenticationProviderTypeStr == null || authenticationProviderTypeStr.isEmpty()) {
				String err = "Failed initializing the Airlock service. " +  Constants.ENV_AUTHENTICATION_PROVIDER_TYPE + " environment parameter is missing.";
				logger.severe(err);
				throw new RuntimeException(err);
			}
			Providers.Type authProviderType = Utilities.valueOf(Providers.Type.class, authenticationProviderTypeStr);
			if (authProviderType == null) { 
				String err = "Failed initializing the Airlock service. Illegal value '" + authenticationProviderTypeStr + "' for " + Constants.ENV_AUTHENTICATION_PROVIDER_TYPE + " environment parameter.";
				logger.severe(err);
				throw new RuntimeException(err);
			}
			logger.info(Constants.ENV_AUTHENTICATION_PROVIDER_TYPE + " = " + authenticationProviderTypeStr);		
			context.setAttribute(Constants.ENV_AUTHENTICATION_PROVIDER_TYPE, authProviderType);
			
			Providers providers = new Providers();
			
			if (authProviderType.equals(Providers.Type.OKTA)) {
				// get OKTA_APPLICATION - a list of accepted OKTA applications separated by ;
				// if environment variable is empty allow access to all known applications.
				String oktaApplications = getEnv(Constants.ENV_OKTA_APPLICATIONS);
				logger.info(Constants.ENV_OKTA_APPLICATIONS + " = " + oktaApplications);
	
				TreeSet<String> supportedApps = new TreeSet<String>();
				for (String item : oktaApplications.split(";"))
				{
					item = item.trim();
					if (!item.isEmpty())
						supportedApps.add(item);
				}
	
				try {
					TreeMap<String,String> config = getSamlConfig();
					for (Map.Entry<String,String> ent : config.entrySet())
					{
						String appName = ent.getKey();
						if (supportedApps.isEmpty() || supportedApps.contains(appName))
							providers.addProvider(appName, new Okta(appName, ent.getValue()));
					}
	
					supportedApps.removeAll(config.keySet());
					if (!supportedApps.isEmpty())
						throw new Exception("invalid values in " + Constants.ENV_OKTA_APPLICATIONS + ": " + supportedApps.toString());
				}
				catch (Exception e)
				{
					String err = "Failed to initialize the Airlock service. Unable to initialize the authentication configuration: " + e.getMessage();
					logger.severe(err);
					throw new RuntimeException(err);	        	
				}
			}
			
			else if (authProviderType.equals(Providers.Type.BLUEID)) {
				try {
					TreeMap<String,String> keys = getBlueIDKeyConfig();
					for (Map.Entry<String,String> ent : keys.entrySet())
					{
						String appKey = ent.getKey();
						providers.addProvider(appKey, new BlueId(appKey, ent.getValue()));
					}
				}
				catch (Exception e) // for the time being SSO is optional
				{
					String err = "Public keys are not configured in the server. SSO authentication is turned off: " + e.getMessage();
					logger.severe(err);
				}
			}
			else if (authProviderType.equals(Providers.Type.AZURE)) {
				String azureClientId = getEnv(Constants.ENV_AZURE_CLIENT_ID);	
				if (azureClientId == null || azureClientId.isEmpty()) {
					String err = "Failed initializing the Airlock service. " +  Constants.ENV_AZURE_CLIENT_ID + " environment parameter is missing.";
					logger.severe(err);
					throw new RuntimeException(err);
				}
				logger.info(Constants.ENV_AZURE_CLIENT_ID + " = " + azureClientId);		
				
				String azureClientSecret = getEnv(Constants.ENV_AZURE_CLIENT_SECRET);	
				if (azureClientSecret == null || azureClientSecret.isEmpty()) {
					String err = "Failed initializing the Airlock service. " +  Constants.ENV_AZURE_CLIENT_SECRET + " environment parameter is missing.";
					logger.severe(err);
					throw new RuntimeException(err);
				}
				logger.info(Constants.ENV_AZURE_CLIENT_SECRET + " = " + azureClientSecret);		
				
				String azureTenant = getEnv(Constants.ENV_AZURE_TENANT);	
				if (azureTenant == null || azureTenant.isEmpty()) {
					String err = "Failed initializing the Airlock service. " +  Constants.ENV_AZURE_TENANT + " environment parameter is missing.";
					logger.severe(err);
					throw new RuntimeException(err);
				}
				logger.info(Constants.ENV_AZURE_TENANT + " = " + azureTenant);		
				
				try {
					TreeMap<String,String> keys = getAzureADKeyConfig();
					for (Map.Entry<String,String> ent : keys.entrySet())
					{
						String appKey = ent.getKey();
						providers.addProvider(appKey, new AzurAD(appKey, ent.getValue(), azureClientId, azureClientSecret, azureTenant));
					}
				}
				catch (Exception e) // for the time being SSO is optional
				{
					String err = "Public keys are not configured in the server. SSO authentication is turned off: " + e.getMessage();
					logger.severe(err);
				}
			}
			context.setAttribute(Constants.PROVIDERS, providers);
		}

		InitializationThread initThread = new InitializationThread(context); 
		initThread.start();

		logger.info("The Airlock service initialized successfully.");
	}


	private DataSerializer buildDataSerializerFromStorageParams(String envStorgeParams) {
		DataSerializer ds = null;
		try {
			JSONObject storageParamsObj = new JSONObject(envStorgeParams);
			if (storageParamsObj.get(DataSerializer.STORAGE_TYPE).equals(DataSerializer.STORAGE_TYPE_S3)) {
				ds = new S3DataSerializer(storageParamsObj);
			}
			else if (storageParamsObj.get(DataSerializer.STORAGE_TYPE).equals(DataSerializer.STORAGE_TYPE_FS)) {
				ds = new FSDataSerializer(storageParamsObj);
			}
			else if (storageParamsObj.get(DataSerializer.STORAGE_TYPE).equals(DataSerializer.STORAGE_TYPE_AZURE)) {
				ds = new AzureDataSerializer(storageParamsObj);
			}
			else if (storageParamsObj.get(DataSerializer.STORAGE_TYPE).equals(DataSerializer.STORAGE_TYPE_BLUEMIX)) {
				ds = new BluemixDataSerializer(storageParamsObj);
			}
			else if (storageParamsObj.get(DataSerializer.STORAGE_TYPE).equals(DataSerializer.STORAGE_TYPE_FS)) {
				ds = new FSDataSerializer(storageParamsObj);
			}
			else {
				String err = "Failed initializing the Airlock service. " + storageParamsObj.get(DataSerializer.STORAGE_TYPE) + " is unknown storage type";
				logger.severe(err);
				throw new RuntimeException(err);
			}
					
			ds.setRuntimeFileNames(AirlockFilesWriter.getRuntimeFileNames());
			return ds;
		}
		catch (Exception e) {
			String err = "Failed initializing the Airlock service. Error when initialize DataSerializer from storage params: " + e.getMessage();
			logger.severe(err);
			throw new RuntimeException(err);
		}
	}

	public void contextDestroyed(ServletContextEvent servletContextEvent) {
		try {
			ServletContext context = servletContextEvent.getServletContext();
			BackgroundTranslator smartling = (BackgroundTranslator) context.getAttribute(Constants.BACKGROUND_TRANSLATOR);
			if (smartling != null)
				smartling.stop();
		}
		catch (Throwable t) {
			logger.warning("Failed to stop the background translator thread: " + t.getMessage());
		}

		try {
			com.amazonaws.http.IdleConnectionReaper.shutdown();
		} catch (Throwable t) {
			logger.warning("Failed stopping Amazon thread IdleConnectionReaper: " + t.getMessage());
		}
	}

	public static String getEnv(String key)
	{
		String value = System.getenv(key);
		if (value == null) {
			value = System.getProperty(key);
			if (value == null) {
				value = "";
			}
		}
		return value;
	}

	public static void reloadUserRoles(ServletContext context)
	{
		Roles roles = (Roles) context.getAttribute(Constants.ROLES_PARAM_NAME);
		UserRoleSets users = (UserRoleSets) context.getAttribute(Constants.AIRLOCK_GLOBAL_USERS_PARAM_NAME);
		if (roles == null || users == null)
			throw new RuntimeException("airlock users/roles missing from context");

		UserRoles filter = new UserRoles(roles, users);
		context.setAttribute(Constants.USER_ROLES, filter);
	}

	public static void reloadUserRolesPerProduct(ServletContext context)
	{
		Roles roles = (Roles) context.getAttribute(Constants.ROLES_PARAM_NAME);

		@SuppressWarnings("unchecked")
		Map<String,UserRoles> rolesPerProductMap = (Map<String,UserRoles>) context.getAttribute(Constants.USER_ROLES_PER_PRODUCT_PARAM_NAME);
		if (roles == null)
			throw new RuntimeException("airlock users per product/roles missing from context");

		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);		

		Set<String> productIds = productsDB.keySet();
		for (String prodId:productIds) {
			Product prod = productsDB.get(prodId);
			UserRoles filter = new UserRoles(roles, prod.getProductUsers());
			rolesPerProductMap.put(prodId, filter);
		}
	}
	
	TreeMap<String,String> getSamlConfig()
	{
		logger.info("Context: " + this.getClass().getClassLoader().getResource("/").toString());
		TreeMap<String,String> out = new TreeMap<String,String>();

		String content = getResource("saml", Constants.AUTHENTICATION_SAML_FILES); // throws Runtime exception if missing
		String[] files = content.split("\n");

		for (String file : files)
		{
			file = file.trim();
			if (file.isEmpty())
				continue;

			content = getResource("saml", file);

			// remove ".xml" suffix
			int index = file.lastIndexOf(".");
			if (index > 0)
				file = file.substring(0, index);
			out.put(file, content);
		}

		if (out.isEmpty())
			throw new RuntimeException("no SAML configuration files found in " + Constants.AUTHENTICATION_SAML_FILES);

		return out;
	}
	TreeMap<String,String> getBlueIDKeyConfig()
	{
		TreeMap<String,String> out = new TreeMap<String,String>();
		String content = getResource("blueIDKeys", Constants.AUTHENTICATION_KEY_FILES); // throws Runtime exception if missing
		String[] files = content.split("\n");

		for (String file : files)
		{
			file = file.trim();
			if (file.isEmpty())
				continue;

			String pemFile = getResource("blueIDKeys", file);
			out.put(file, pemFile);
		}

		if (out.isEmpty())
			throw new RuntimeException("no key files found in " + Constants.AUTHENTICATION_KEY_FILES);

		return out;
	}

	TreeMap<String,String> getAzureADKeyConfig()
	{
		TreeMap<String,String> out = new TreeMap<String,String>();
		String content = getResource("azureADKeys", Constants.AUTHENTICATION_KEY_FILES); // throws Runtime exception if missing
		String[] files = content.split("\n");

		for (String file : files)
		{
			file = file.trim();
			if (file.isEmpty())
				continue;

			String pemFile = getResource("azureADKeys", file);
			out.put(file, pemFile);
		}

		if (out.isEmpty())
			throw new RuntimeException("no key files found in " + Constants.AUTHENTICATION_KEY_FILES);

		return out;
	}

	public String getResource(String folder, String fileName)
	{
		// load a configuration file from a relative path, to make it work in eclipse too.
		// point to WEB-INF/<folder>/<filename> from the class loader location which is WEB-INF/classes
		try {
			InputStream stream = this.getClass().getClassLoader().getResourceAsStream("../" + folder + "/" + fileName);
			if (stream == null)
				throw new Exception("");

			String content = Utilities.streamToString(stream);
			stream.close();
			return content;
		}
		catch (Exception e)
		{
			String err = "Failed to initialize the Airlock service. Can't find file " + fileName;
			logger.severe(err);
			throw new RuntimeException(err);
		}
	}
}
