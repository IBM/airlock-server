package com.ibm.airlock.admin.serialize;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import com.ibm.airlock.Strings;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.AnalyticsServices;
import com.ibm.airlock.Constants;
import com.ibm.airlock.ProductServices;
import com.ibm.airlock.Constants.APIKeyOutputMode;
import com.ibm.airlock.Constants.OutputJSONMode;
import com.ibm.airlock.Constants.Platform;
import com.ibm.airlock.Constants.Stage;
import com.ibm.airlock.Constants.StringsOutputMode;
import com.ibm.airlock.Constants.UtilityType;
import com.ibm.airlock.admin.BaseAirlockItem;
import com.ibm.airlock.admin.Branch;
import com.ibm.airlock.admin.BranchesCollection;
import com.ibm.airlock.admin.InputSchema;
import com.ibm.airlock.admin.InternalUserGroups;
import com.ibm.airlock.admin.Product;
import com.ibm.airlock.admin.RootItem;
import com.ibm.airlock.admin.Season;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.MergeBranch.MergeException;
import com.ibm.airlock.admin.analytics.AirlockAnalytics;
import com.ibm.airlock.admin.notifications.AirlockNotificationsCollection;
import com.ibm.airlock.admin.operations.AirlockAPIKeys;
import com.ibm.airlock.admin.operations.AirlockChangeContent;
import com.ibm.airlock.admin.operations.UserRoleSets;
import com.ibm.airlock.admin.operations.Webhooks;
import com.ibm.airlock.admin.streams.AirlockStreamsCollection;
import com.ibm.airlock.admin.streams.StreamsEvents;
import com.ibm.airlock.admin.translations.OriginalStrings;
import com.ibm.airlock.engine.Environment;
import com.ibm.airlock.utilities.Pair;

public class AirlockFilesWriter {
	public static final Logger logger = Logger.getLogger(AirlockFilesWriter.class.getName());

	//return error string upon error. null upon success
	public static AirlockAnalytics getAnalytics(Season season, String branchId, ServletContext context) {
		AirlockAnalytics airlockAnalytics = null;
		if (branchId.equals(Constants.MASTER_BRANCH_NAME)) {
			airlockAnalytics = season.getAnalytics();
		}
		else {
			@SuppressWarnings("unchecked")
			Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);
			Branch branch = branchesDB.get(branchId);
			airlockAnalytics = branch.getAnalytics();
		}
		return airlockAnalytics;
	}
	public static List<AirlockChangeContent> writeAnalytics (Season season, ServletContext context, String branchId, Environment env, Stage stage) throws IOException {
 		JSONObject output = new JSONObject();
		String error = null;
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		String separator = ds.getSeparator();
		String fileName = null;
		AirlockAnalytics airlockAnalytics = null;
		if (branchId.equals(Constants.MASTER_BRANCH_NAME)) {
			fileName = Constants.SEASONS_FOLDER_NAME + separator + season.getProductId().toString() +
					separator + season.getUniqueId().toString() + separator +
					Constants.AIRLOCK_ANALYTICS_FILE_NAME;

			airlockAnalytics = season.getAnalytics();
		}
		else {
			fileName = Constants.SEASONS_FOLDER_NAME + separator + season.getProductId().toString() +
					separator + season.getUniqueId().toString() + separator +
					Constants.AIRLOCK_BRANCHES_FOLDER_NAME + separator+branchId +
					separator + Constants.AIRLOCK_BRANCH_ANALYTICS_FILE_NAME;

			@SuppressWarnings("unchecked")
			Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);
			Branch branch = branchesDB.get(branchId);			
			airlockAnalytics = branch.getAnalytics(); 
		}

		Map<String, BaseAirlockItem> airlockItemsDB;
		try {
			airlockItemsDB = Utilities.getAirlockItemsDB(branchId, context);
			output = airlockAnalytics.toJson(OutputJSONMode.ADMIN, context, season, airlockItemsDB, env);
		} catch (JSONException je) {
			//should never happen - creating JSON object from memory should always succeed but just in-case ...
			//reload from s3 and return error
			logger.severe("Failed writing the analytics to S3. Illegal JSON format: " + je.getMessage());
			try {
				error = "Failed writing the analytics to S3 due to illegal JSON format. Reloading previous data from S3.";
				logger.info(error);
				JSONObject seasonAnalytics  = ds.readDataToJSON(fileName);
				Utilities.initFromSeasonAnalyticsJSON(seasonAnalytics, context);		
			} catch (IOException e) {
				String errMsg = String.format(Strings.failedReadingFile,fileName) + e.getMessage();
				error = error + "\n" + errMsg;
				logger.severe(errMsg);
				logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
				throw new IOException(errMsg);
			} catch (JSONException e) {
				String errMsg = fileName + " file is not in a legal JSON format: " + e.getMessage();
				error = error + "\n" + errMsg;
				logger.severe(errMsg);			
				logger.severe(Strings.changeAirlockSerevrStateTo + "IO_ERROR.");
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
				throw new IOException(errMsg);
			} 
			//roll back succeeded. Throwing an exception to inform the user but the server state is still RUNNING. (not a S3 problem) 
			logger.severe(error);			
			throw new IOException(error);
		} catch (MergeException e) {
			String errMsg = Strings.mergeException + e.getMessage();
			error = error + "\n" + errMsg;
			logger.severe(errMsg);
			throw new IOException(errMsg);		
		} 	
		
		//validate the data consistency of the object that is going to be written to s3
		try { 
			AirlockAnalytics tmp = new AirlockAnalytics(season.getUniqueId());
			tmp.fromJSON(season.getUniqueId(), output, context, env, airlockItemsDB);
		} catch (JSONException e) {
			String errMsg = "Failed writing the analytics to S3. Illegal JSON format during data validation: " + e.getMessage();
			error = error + "\n" + errMsg;
			logger.severe(errMsg);			
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			throw new IOException(errMsg);
		}	

		try {			
			ds.writeData(fileName, output.write(true));
			LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
			changesArr.add(AirlockChangeContent.getAdminChange(output, fileName, stage));
			return changesArr;
		} catch (IOException ioe) {
			//failed writing 3 times to s3. Set server state to ERROR.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = "Failed writing analytics to S3: " + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			throw new IOException(error);			
		}
		catch (JSONException je) {
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = "Failed writing analytics to S3: " + je.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			throw new IOException(error);			
		}
	}

	public static LinkedList<AirlockChangeContent> writeBranchRuntime (Branch branch, Season season, ServletContext context, Environment env, boolean writeProduction) throws IOException {
		String error = null;
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		String separator = ds.getSeparator();
		//to be used if writeRuntime = true
		JSONObject branchRuntimeDevOutput = new JSONObject();		
		JSONObject branchRuntimeProdOutput = new JSONObject();
		String branchRuntimeDevFileName = Constants.SEASONS_FOLDER_NAME+separator+season.getProductId().toString()+
				separator+season.getUniqueId().toString()+separator+Constants.AIRLOCK_BRANCHES_FOLDER_NAME+ 
				separator+branch.getUniqueId().toString()+separator+Constants.AIRLOCK_RUNTIME_BRANCH_DEVELOPMENT_FILE_NAME;							

		String branchRuntimeProdFileName = Constants.SEASONS_FOLDER_NAME+separator+season.getProductId().toString()+
				separator+season.getUniqueId().toString()+separator+Constants.AIRLOCK_BRANCHES_FOLDER_NAME+ 
				separator+branch.getUniqueId().toString()+separator+Constants.AIRLOCK_RUNTIME_BRANCH_PRODUCTION_FILE_NAME;							

		try {
			branchRuntimeDevOutput = branch.toJson(OutputJSONMode.RUNTIME_DEVELOPMENT, context, env, true, true, true);
			if (writeProduction) //no need to touch the prod file - if this branch is in exp - the writing of the global runtime will touch, if stand alone branch - no need to touch the file
				branchRuntimeProdOutput = branch.toJson(OutputJSONMode.RUNTIME_PRODUCTION, context, env, true, true, true);
		} catch (JSONException je) {
			//should never happen - creating JSON object from memory should always succeed but just in-case ...
			//reload from s3 and return error
			logger.severe("Failed writing the branch " + branch.getName() + " to S3. Illegal JSON format: " + je.getMessage());			
			throw new IOException(error);
		} 

		try {			
			//ds.writeData(branchRuntimeDevFileName, branchRuntimeDevOutput.write(true), true);
			writeData(context, ds, branchRuntimeDevFileName, branchRuntimeDevOutput.write(true), season, true);
			changesArr.add(AirlockChangeContent.getRuntimeChange(branchRuntimeDevOutput, branchRuntimeDevFileName, Stage.DEVELOPMENT));
			if (writeProduction) {
				//ds.writeData(branchRuntimeProdFileName, branchRuntimeProdOutput.write(true), true);
				writeData(context, ds, branchRuntimeProdFileName, branchRuntimeProdOutput.write(true), season, true);
				changesArr.add(AirlockChangeContent.getRuntimeChange(branchRuntimeProdOutput, branchRuntimeProdFileName, Stage.PRODUCTION));
			}
				
			
			return changesArr;
		} catch (IOException ioe) {
			//failed writing 3 times to s3. Set server state to ERROR.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = "Failed writing the branch " + branch.getName() + " to S3: " + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			throw new IOException(error);			
		}
		catch (JSONException je) {
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = "Failed writing the branch " + branch.getName() + " to S3: " + je.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			throw new IOException(error);			
		}				
	}

	public static LinkedList<AirlockChangeContent> writeBranchFeatures (Branch branch, Season season, ServletContext context, Environment env, Stage stage) throws IOException {
		String error = null;
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		String separator = ds.getSeparator();
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		JSONObject branchAdminOutput = new JSONObject();		
		String branchAdminFileName = Constants.SEASONS_FOLDER_NAME+separator+season.getProductId().toString()+
				separator+season.getUniqueId().toString()+separator+Constants.AIRLOCK_BRANCHES_FOLDER_NAME+ 
				separator+branch.getUniqueId().toString()+separator+Constants.AIRLOCK_BRANCH_FEATURES_FILE_NAME;							

		try {
			branchAdminOutput = branch.toJson(OutputJSONMode.ADMIN, context, env, true, true, false);
		} catch (JSONException je) {
			//should never happen - creating JSON object from memory should always succeed but just in-case ...
			//reload from s3 and return error
			logger.severe("Failed writing the branch " + branch.getName() + " to S3. Illegal JSON format: " + je.getMessage());			
			throw new IOException(error);
		}

		//validate the data consistency of the object that is going to be written to s3
		try { 
			Branch tmp = new Branch(season.getUniqueId());
			tmp.fromJSON(branchAdminOutput, env, season, context);
		} catch (JSONException e) {
			String errMsg = "Failed writing the branch to S3. Illegal JSON format during data validation: " + e.getMessage();
			error = error + "\n" + errMsg;
			logger.severe(errMsg);			
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			throw new IOException(errMsg);
		}	
		
		
		try {			
			ds.writeData(branchAdminFileName, branchAdminOutput.write(true));
			changesArr.add(AirlockChangeContent.getAdminChange(branchAdminOutput, branchAdminFileName, stage));
			return changesArr;
		} catch (IOException ioe) {
			//failed writing 3 times to s3. Set server state to ERROR.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = "Failed writing the branch " + branch.getName() + " to S3: " + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			throw new IOException(error);			
		}
		catch (JSONException je) {
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = "Failed writing the branch " + branch.getName() + " to S3: " + je.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			throw new IOException(error);			
		}				
	}

	//return error string upon error. null upon success
	public static Pair<String,LinkedList<AirlockChangeContent>> writeFeatures (Season season, boolean writeProduction, ServletContext context, Environment env) throws IOException {
		String error = null;
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		Stage stage = writeProduction? Stage.PRODUCTION : Stage.DEVELOPMENT;
		try {
			//runtime version 
			changesArr.addAll(doWriteFeatures (season, OutputJSONMode.RUNTIME_DEVELOPMENT, context, stage, env));

			//defaults version
			changesArr.addAll(doWriteDefaultsFile(season, context, stage));

			//airlock constants java and swift file
			changesArr.addAll(doWriteConstantsFiles(season, context, stage));
			
			if (writeProduction)
				changesArr.addAll(doWriteFeatures (season, OutputJSONMode.RUNTIME_PRODUCTION, context, stage, env));

			//admin version
			changesArr.addAll(doWriteFeatures (season, OutputJSONMode.ADMIN, context, stage, env)); //this is the last one to be updated so if writing to other file will fail we will be able to revert

		} catch  (JSONException je) {
			//should never happen - creating JSON object from memory should always succeed but just in-case ...
			//reload from s3 and return error
			DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
			String separator = ds.getSeparator();
			String seasonFilePath = Constants.SEASONS_FOLDER_NAME + separator + season.getProductId().toString() + separator + season.getUniqueId().toString();
			logger.severe("Failed writing the features to S3. Illegal JSON format: " + je.getMessage());
			try {
				error = "Failed building the feature's JSON object. Reloading previous data from S3.";
				logger.info(error);				
				JSONObject seasonJSON  = ds.readDataToJSON(seasonFilePath);
				
				Utilities.initFromSeasonFeaturesJSON(seasonJSON, (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME), 
						(Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME), ds, true, Constants.REQUEST_ITEM_TYPE.FEATURES);
			} catch (IOException e) {
				String errMsg = String.format(Strings.failedReadingFile,seasonFilePath) + e.getMessage();
				error = error + "\n" + errMsg;
				logger.severe(errMsg);	
				logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
				throw new IOException(errMsg);
			} catch (JSONException e) {
				String errMsg = seasonFilePath + " file is not in a legal JSON format: " + e.getMessage();
				error = error + "\n" + errMsg;
				logger.severe(errMsg);
				logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
				throw new IOException(errMsg);
			}	
			//roll back succeeded. Throwing an exception to inform the user but the server state is still RUNNING. (not a S3 problem) 
			logger.severe(error);			
			throw new IOException(error);

		} catch (IOException ioe) {
			//failed writing 3 times to s3. Set server state to ERROR.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = "Failed writing the features to S3: " + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");			
			throw new IOException(error);		
		}

		return new Pair<>(error,changesArr);
	}

	public static LinkedList<AirlockChangeContent> doWriteFeatures(Season season, OutputJSONMode mode, ServletContext context, Stage stage, Environment env) throws JSONException, IOException{
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		String separator = ds.getSeparator();
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);

		String season_id = season.getUniqueId().toString();
		String product_id = season.getProductId().toString();


		boolean oldSeason = false;
		String oldRuntimeFile = null;

		JSONObject res = new JSONObject();
		boolean isAdmin = true;

		if (mode == OutputJSONMode.RUNTIME_DEVELOPMENT || mode == OutputJSONMode.RUNTIME_PRODUCTION) {
			isAdmin = false;
			//if oldRuntimeFile exists this means that we are in an old season therefore only development content is written to 
			//the oldRuntimeFile (AirlockRuntime.json). In old seasons production and development files are not written
			oldRuntimeFile = Constants.SEASONS_FOLDER_NAME+separator+product_id+separator+season_id+separator+Constants.AIRLOCK_RUNTIME_FILE_NAME;

			if (season.isOldRuntimeFileExists()) {
				oldSeason = true;
				if (mode == OutputJSONMode.RUNTIME_PRODUCTION) {
					//in old season production runtime is not written at all
					return changesArr;
				}
			}	

			String runtimePublicFullPath = (String)context.getAttribute(Constants.RUNTIME_PUBLIC_FULL_PATH_PARAM_NAME);
			String storagePublicPath = (String)context.getAttribute(Constants.STORAGE_PUBLIC_PATH_PARAM_NAME);
			Product prod = productsDB.get(product_id);
			
			res.put(Constants.JSON_FIELD_STORAGE_PUBLIC_PATH, runtimePublicFullPath/*Constants.S3_PATH + ds.getPathPrefix()*/);
			res.put(Constants.JSON_FIELD_DEV_STORAGE_PUBLIC_PATH, storagePublicPath + ds.getPublicPathPrefix());
			res.put(Constants.JSON_FIELD_PRODUCT_NAME, prod.getName());
			res.put(Constants.JSON_FIELD_DEFAULT_LANGUAGE, Constants.DEFAULT_LANGUAGE); 
			LinkedList<String> supLangandStrings = season.getOriginalStrings().getSupportedLanguages();
			res.put(Constants.JSON_FIELD_SUPPORTED_LANGUAGES, supLangandStrings); 

			
		/*	Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());
			env.setBranchId(Constants.MASTER_BRANCH_NAME);*/

			if (AnalyticsServices.isAnalyticsSupported(env)) {
				res.put (Constants.JSON_FIELD_INPUT_FIELDS_FOR_ANALYTICS, season.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection().getInputFieldsForAnalyticsPerStage(mode == OutputJSONMode.RUNTIME_DEVELOPMENT?Stage.DEVELOPMENT:Stage.PRODUCTION, context, season));		
			}

			if (AnalyticsServices.isExperimentsSupported(env)) {
				JSONObject experimentsObj = prod.getExperimentsMutualExclusionGroup().getExperimentsArrayByStageForSeason(mode, context, season, true); //skip disabled
				res.put(Constants.JSON_FIELD_EXPERIMENTS, experimentsObj);

				JSONArray branchesArr = prod.getBranchesArrayByStageForSeason(mode, context, season);
				res.put(Constants.JSON_FIELD_BRANCHES, branchesArr);
			}
		}


		//TODO: for now server versions and defaults verions are the same 2.1=>V2.1 2.5=>V2.5.
		//      when they wont be the same we will need to have a map between them
		res.put(Constants.JSON_FIELD_VERSION, Utilities.getSDKVersionPerSerevrVersion(season.getServerVersion())); 
		res.put(Constants.JSON_FIELD_SERVER_VERSION, "V"+season.getServerVersion()); 

		res.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, season_id);
		res.put(Constants.JSON_SEASON_FIELD_MAX_VER, season.getMaxVersion());
		res.put(Constants.JSON_SEASON_FIELD_MIN_VER, season.getMinVersion());
		res.put(Constants.JSON_FIELD_PRODUCT_ID, product_id); 

		//Environment env = new Environment();
		//env.setServerVersion(season.getServerVersion());
		env.setAnalytics(season.getAnalytics());
		//env.setBranchId(Constants.MASTER_BRANCH_NAME);

		JSONObject featuresTree = season.getRoot().toJson(mode, context, env);
		featuresTree.remove(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS); //remove features field from root
		res.put(Constants.JSON_FIELD_ROOT, featuresTree);
		
		//in runtime and in defaults - add purchases
		if (ProductServices.isPurchasesSupported(env) && mode!=OutputJSONMode.ADMIN) {
			JSONObject purchasesTree = season.getEntitlementsRoot().toJson(mode, context, env);
			purchasesTree.remove(Constants.JSON_FEATURE_FIELD_FEATURES); //remove features field from root 
			res.put(Constants.JSON_FIELD_ENTITLEMENTS_ROOT, purchasesTree);	
		}

		String fileName = "";
		boolean publish = false;

		if (mode == OutputJSONMode.RUNTIME_DEVELOPMENT) {
			fileName = Constants.SEASONS_FOLDER_NAME+separator+product_id+separator+season_id+separator+Constants.AIRLOCK_RUNTIME_DEVELOPMENT_FILE_NAME;
			if (oldSeason) {
				fileName = oldRuntimeFile; //in old season dev data is written to old filename ("AirlockRuntime.json")
			}
			publish = true;
		}
		else if (mode == OutputJSONMode.RUNTIME_PRODUCTION) {
			fileName = Constants.SEASONS_FOLDER_NAME+separator+product_id+separator+season_id+separator+Constants.AIRLOCK_RUNTIME_PRODUCTION_FILE_NAME;
			changesArr.addAll(Utilities.touchProductionChangedFile(context, ds, season));
			publish = true;
		}
		else { //admin
			fileName = Constants.SEASONS_FOLDER_NAME+separator+product_id+separator+season_id+separator+Constants.AIRLOCK_FEATURES_FILE_NAME;
		}

		if (mode.equals(OutputJSONMode.ADMIN)) {
			//validate the data consistency of the object that is going to be written to s3
			try { 
				RootItem rootFeature = new RootItem();										
				rootFeature.fromJSON(res, null, null, env);				
			} catch (JSONException e) {
				String errMsg = "Failed writing the features to S3. Illegal JSON format during data validation: " + e.getMessage();
				logger.severe(errMsg);			
				logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
				throw new IOException(errMsg);
			}
		}
		
		//ds.writeData(fileName, res.write(true), publish);
		if (isAdmin) {
			changesArr.add(AirlockChangeContent.getAdminChange(res, fileName, stage));
		} else {
			changesArr.add(AirlockChangeContent.getRuntimeChange(res, fileName, stage));
		}
		writeData(context, ds, fileName, res.write(true), season, publish);
		return changesArr;
	}
	
	public static LinkedList<AirlockChangeContent> doWriteFeatureFollowers(Season season, ServletContext context, Stage stage) throws JSONException, IOException {
		try{
			DataSerializer ds = (DataSerializer) context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
			String separator = ds.getSeparator();
			LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
			
			String season_id = season.getUniqueId().toString();
			String product_id = season.getProductId().toString();

			@SuppressWarnings("unchecked")			
			Map<String, ArrayList<String>> followersDB = (Map<String, ArrayList<String>>) context.getAttribute(Constants.FOLLOWERS_FEATURES_DB_PARAM_NAME);

			List<String> featuresFollowers = season.getFollowersId();
			JSONObject res = new JSONObject();
			JSONArray allFollowersArray = new JSONArray();
			for(int i=0; i<featuresFollowers.size();++i){
				String id = featuresFollowers.get(i);
				List<String> followers = followersDB.get(id);
				if(followers != null && followers.size() != 0){
					JSONObject currFeatureFollowers = new JSONObject();
					currFeatureFollowers.put("uniqueID",id);
					currFeatureFollowers.put("followers",followersDB.get(id));
					allFollowersArray.add(currFeatureFollowers);
				}
			}
			res.put(Constants.JSON_FIELD_FOLLOWERS,allFollowersArray);
			res.put(Constants.JSON_FEATURE_FIELD_SEASON_ID,season_id);

			String fileName = Constants.SEASONS_FOLDER_NAME + separator + product_id + separator + season_id + separator + Constants.FOLLOWERS_FEATURES_FILE_NAME;
			ds.writeData(fileName, res.write(true));
			changesArr.add(AirlockChangeContent.getAdminChange(res, fileName, stage));
			return changesArr;
		}catch (IOException ioe) {
			//failed writing roles 3 times to s3.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			String error = "Failed writing the feature followers to S3: " + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + " S3_IO_ERROR.");
			throw new IOException(error);
		}
		catch (JSONException je) {
			//failed writing roles 3 times to s3.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			String error = "Failed writing the feature followers to S3: " + je.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			throw new IOException(error);
		}
	}

	public static LinkedList<AirlockChangeContent> doWriteConstantsFiles(Season season, ServletContext context, Stage stage) throws JSONException, IOException {
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		//airlock constants java file
		changesArr.addAll(doWriteConstantsFile(season, Platform.Android, context, stage));

		//airlock constants swift file
		changesArr.addAll(doWriteConstantsFile(season, Platform.iOS, context, stage));
		
		Environment env = new Environment();
		env.setServerVersion(season.getServerVersion());
		
		if (ProductServices.isCSharpConstantsSupported(env)) {
			//airlock constants c# file
			changesArr.addAll(doWriteConstantsFile(season, Platform.c_sharp, context, stage));			
		}
		return changesArr;
	}
	
	private static LinkedList<AirlockChangeContent> doWriteConstantsFile(Season season, Constants.Platform platform, ServletContext context, Stage stage) throws JSONException, IOException {
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		String separator = ds.getSeparator();
		RootItem root = season.getRoot();
		RootItem entitlementsRoot = season.getEntitlementsRoot();

		String season_id = season.getUniqueId().toString();
		String product_id = season.getProductId().toString();

		StringBuilder javaFileContent = new StringBuilder();
		HashMap<String, StringBuilder> namespaceStrBuilderMap = new HashMap<String, StringBuilder>(); //for each namespace a separate sb since for each namespace separate class
		if (platform.equals(Platform.Android)) {
			//javaFileContent.append("package com.weather.airlock; \n\n");
			javaFileContent.append("/**\n");
			javaFileContent.append("* Automatically generated file by Airlock server. DO NOT MODIFY\n");
			javaFileContent.append("*/\n");
			javaFileContent.append("public class AirlockConstants { \n");
		}
		else if (platform.equals(Platform.iOS)){
			//iOS
			javaFileContent.append("import Foundation\n\n");
			javaFileContent.append("/**\n");
			javaFileContent.append("* Automatically generated file by Airlock server. DO NOT MODIFY\n");
			javaFileContent.append("*/\n");
			javaFileContent.append("@objc public class AirlockConstants : NSObject {\n");
		}
		else if (platform.equals(Platform.c_sharp)){
			//C#
			javaFileContent.append("using System;\n\n");
			javaFileContent.append("/**\n");
			javaFileContent.append("* Automatically generated file by Airlock server. DO NOT MODIFY\n");
			javaFileContent.append("*/\n");
			javaFileContent.append("namespace Airlock {\n");			;
			javaFileContent.append("	public class AirlockConstants {\n");
		}
		
		//list of all the found features (namespace.name) so if a feature with the same name exists in 2 different branches it will
		//only be listed once
		TreeSet<String> foundFeatures = new TreeSet<String>();
		if (root.getFeaturesItems() != null && root.getFeaturesItems().size()>0) {
			for (int i=0; i<root.getFeaturesItems().size(); i++) {
				BaseAirlockItem f = root.getFeaturesItems().get(i);
				f.toConstantsFile(namespaceStrBuilderMap, platform, false, foundFeatures);				
			}				
		}
		
		if (entitlementsRoot.getEntitlementItems() != null && entitlementsRoot.getEntitlementItems().size()>0) {
			for (int i=0; i<entitlementsRoot.getEntitlementItems().size(); i++) {
				BaseAirlockItem f = entitlementsRoot.getEntitlementItems().get(i);
				f.toConstantsFile(namespaceStrBuilderMap, platform, false, foundFeatures);				
			}				
		}
		
		for (Branch branch : season.getBranches().getBranchesList()) {
			for (BaseAirlockItem branchSubTreeRoot:branch.getBranchFeatures()) {
				branchSubTreeRoot.toConstantsFile(namespaceStrBuilderMap, platform, true, foundFeatures);	
			}
			
			for (BaseAirlockItem branchSubTreeRoot:branch.getBranchPurchases()) {
				branchSubTreeRoot.toConstantsFile(namespaceStrBuilderMap, platform, true, foundFeatures);	
			}
		}

		Set<String> namespaces = namespaceStrBuilderMap.keySet();
		for (String ns:namespaces) {
			if (platform.equals(Platform.Android)) {
				javaFileContent.append("	public class " + ns + " { \n");
			}
			else if (platform.equals(Platform.iOS)) {
				//iOS
				javaFileContent.append("	@objc static let " + ns + " = " + ns + "_impl()\n\n");
				javaFileContent.append("	@objc class "+ns+"_impl : NSObject {\n");
			}
			else if (platform.equals(Platform.c_sharp)) {
				//C#
				javaFileContent.append("		public class " + ns + "{\n");
			}
			javaFileContent.append(namespaceStrBuilderMap.get(ns));
			if (platform.equals(Platform.c_sharp)) {
				javaFileContent.append("	");
			}
			javaFileContent.append("	}\n");
		}
		if (platform.equals(Platform.c_sharp)) {
			javaFileContent.append("	}\n");
		}
		javaFileContent.append("}\n");

		String fileName  = "";
		if (platform.equals(Platform.Android)) {
			fileName = Constants.SEASONS_FOLDER_NAME+separator+product_id+separator+season_id+separator+Constants.AIRLOCK_JAVA_FILE_NAME;
		}
		else if (platform.equals(Platform.iOS)) {  
			//iOS
			fileName = Constants.SEASONS_FOLDER_NAME+separator+product_id+separator+season_id+separator+Constants.AIRLOCK_SWIFT_FILE_NAME;
		}
		else if (platform.equals(Platform.c_sharp)) {  
			//C#
			fileName = Constants.SEASONS_FOLDER_NAME+separator+product_id+separator+season_id+separator+Constants.AIRLOCK_C_SHARP_FILE_NAME;
		}
		ds.writeData(fileName, javaFileContent.toString());
		changesArr.add(AirlockChangeContent.getAdminChange(javaFileContent.toString(), fileName, stage));
		return changesArr;
	}

	//cannot use doWriteFeaturs since not hierarchical	
	public static LinkedList<AirlockChangeContent> doWriteDefaultsFile(Season season, ServletContext context, Stage stage) throws JSONException, IOException {
		//RootItem root = season.getRoot();
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		String season_id = season.getUniqueId().toString();
		String product_id = season.getProductId().toString();
		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);		

		Product prod = productsDB.get(product_id);
		if (prod == null) {
			//should not happen - at this point season has a product but just in case...
			String err = "Product " + product_id + " does not exist.";
			logger.severe(err);
			throw new JSONException(err);
		}

		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		String separator = ds.getSeparator();
		String runtimePublicFullPath = (String)context.getAttribute(Constants.RUNTIME_PUBLIC_FULL_PATH_PARAM_NAME);
		String storagePublicPath = (String)context.getAttribute(Constants.STORAGE_PUBLIC_PATH_PARAM_NAME);

		JSONObject res = new JSONObject();
		//map between server version and default file version 2.1=>V2.1 2.5=>V2.5. 3.0=>2.5
		res.put(Constants.JSON_FIELD_VERSION, Utilities.getSDKVersionPerSerevrVersion(season.getServerVersion())); 

		res.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, season_id);
		res.put(Constants.JSON_FIELD_STORAGE_PUBLIC_PATH, runtimePublicFullPath/*Constants.S3_PATH + ds.getPathPrefix()*/);
		res.put(Constants.JSON_FIELD_DEV_STORAGE_PUBLIC_PATH, storagePublicPath + ds.getPublicPathPrefix());
		res.put(Constants.JSON_FIELD_PRODUCT_ID, product_id); 
		res.put(Constants.JSON_FIELD_PRODUCT_NAME, prod.getName());
		res.put(Constants.JSON_FIELD_DEFAULT_LANGUAGE, Constants.DEFAULT_LANGUAGE); 
		LinkedList<String> supLangandStrings = season.getOriginalStrings().getSupportedLanguages();
		res.put(Constants.JSON_FIELD_SUPPORTED_LANGUAGES, supLangandStrings); 

		if (season.isOldRuntimeFileExists()) {
			String javascriptUtility = season.getUtilities().generateUtilityCodeSectionForStageAndType(null, null, null, null, UtilityType.MAIN_UTILITY);
			res.put(Constants.JSON_FIELD_JAVASCRIPT_UTILITIES, javascriptUtility/*ds.readDataToString(Constants.JAVASCRIPT_UTILITIES_FILE_NAME)*/); 
		}

		@SuppressWarnings("unchecked")
		Map<String, BaseAirlockItem> airlockItemsDB = (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);
		
		Environment env = new Environment();
		env.setServerVersion(season.getServerVersion());
		env.setAirlockItemsDB(airlockItemsDB);

		res.put(Constants.JSON_FIELD_ROOT, season.getRoot().toJson(OutputJSONMode.DEFAULTS, context, env));		
		res.put(Constants.JSON_FIELD_ENTITLEMENTS_ROOT, season.getEntitlementsRoot().toJson(OutputJSONMode.DEFAULTS, context, env));		

		String fileName = Constants.SEASONS_FOLDER_NAME+separator+product_id+separator+season_id+separator+Constants.AIRLOCK_DEFAULTS_FILE_NAME;
		ds.writeData(fileName, res.write(true), true/*.toString()*/);
		changesArr.add(AirlockChangeContent.getAdminChange(res, fileName, stage));
		return changesArr;
	}

	//return error string upon error. null upon success
	public static LinkedList<AirlockChangeContent> writeInputSchema (Season season, ServletContext context, Stage stage) throws IOException {
		JSONObject output = new JSONObject();
		String error = null;
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		String separator = ds.getSeparator();
		String base = Constants.SEASONS_FOLDER_NAME+separator+season.getProductId().toString()+separator+season.getUniqueId().toString()+separator;
		String fileName = base + Constants.AIRLOCK_INPUT_SCHEMA_FILE_NAME;
		String streamsPath = base + Constants.AIRLOCK_STREAMS_FILE_NAME;
		String errorFile = fileName;

		try {
			output = season.getInputSchema().toJson();
		} catch (JSONException je) {
			//should never happen - creating JSON object from memory should always succeed but just in-case ...
			//reload from s3 and return error
			logger.severe("Failed writing the InputSchema to S3. Illegal JSON format: " + je.getMessage());
			try {
				error = "Failed writing the InputSchema to S3 due to illegal JSON format. Reloading previous data from S3.";
				logger.info(error);
				JSONObject inputSchemaJSON  = ds.readDataToJSON(fileName);

				JSONObject streamsJSON = null;
				if (ds.isFileExists(streamsPath)) {
					errorFile = streamsPath; // throw the correct file name on error
					streamsJSON = ds.readDataToJSON(streamsPath);			
				}

				Utilities.initFromSeasonInputschemaJSON(inputSchemaJSON, streamsJSON, context);		
			} catch (IOException e) {
				String errMsg = String.format(Strings.failedReadingFile,errorFile) + e.getMessage();
				error = error + "\n" + errMsg;
				logger.severe(errMsg);
				logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
				throw new IOException(errMsg);
			} catch (JSONException e) {
				String errMsg = errorFile + " file is not in a legal JSON format: " + e.getMessage();
				error = error + "\n" + errMsg;
				logger.severe(errMsg);			
				logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
				throw new IOException(errMsg);
			}
			//roll back succeeded. Throwing an exception to inform the user but the server state is still RUNNING. (not a S3 problem) 
			logger.severe(error);			
			throw new IOException(error);
		}				

		//validate the data consistency of the object that is going to be written to s3
		try { 
			InputSchema tmp = new InputSchema(season.getUniqueId());
			tmp.fromJSON(output);
		} catch (JSONException e) {
			String errMsg = "Failed writing the input schema to S3. Illegal JSON format during data validation: " + e.getMessage();
			error = error + "\n" + errMsg;
			logger.severe(errMsg);			
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			throw new IOException(errMsg);
		}
		
		try {			
			ds.writeData(fileName, output.write(true));
			changesArr.add(AirlockChangeContent.getAdminChange(output, fileName, stage));
			return changesArr;
		} catch (IOException ioe) {
			//failed writing 3 times to s3. Set server state to ERROR.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = "Failed writing InputSchema to S3: " + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			throw new IOException(error);			
		}
		catch (JSONException je) {
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = "Failed writing InputSchema to S3: " + je.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			throw new IOException(error);			
		}
	}

	/*
	//return error string upon error. null upon success
	public static void doWriteFollowersProducts(ServletContext context) throws IOException {
		@SuppressWarnings("unchecked")
		Map<String, ArrayList<String>> followersDB = (Map<String, ArrayList<String>>) context.getAttribute(Constants.FOLLOWERS_PRODUCTS_DB_PARAM_NAME);

		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		
		JSONObject res = new JSONObject();
		JSONArray allFollowersArray = new JSONArray();
		try {
			Iterator iterator = followersDB.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry pair = (Map.Entry)iterator.next();
				JSONObject currFeatureFollowers = new JSONObject();
				currFeatureFollowers.put("uniqueID", pair.getKey().toString());
				JSONArray arrFollowers = new JSONArray();
				List<String> followers = (List<String>) pair.getValue();
				if(followers.size() == 0){
					continue;
				}
				for(int i = 0;i<followers.size();++i){
					arrFollowers.add(followers.get(i));
				}
				currFeatureFollowers.put("followers",arrFollowers);
				allFollowersArray.add(currFeatureFollowers);
			}
			res.put("allFollowers",allFollowersArray);

			ds.writeData(Constants.FOLLOWERS_PRODUCTS_FILE_NAME, res.write(true));
		} catch (IOException ioe) {
			//failed writing roles 3 times to s3.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			String error = Strings.failedWritingProduct + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			throw new IOException(error);
		}
		catch (JSONException je) {
			//failed writing roles 3 times to s3.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			String error = Strings.failedWritingProduct + je.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			throw new IOException(error);
		}
	}
*/
	
	//return error string upon error. null upon success
	public static LinkedList<AirlockChangeContent> doWriteFollowersProducts(ServletContext context) throws IOException {
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		@SuppressWarnings("unchecked")
		Map<String, ArrayList<String>> followersDB = (Map<String, ArrayList<String>>) context.getAttribute(Constants.FOLLOWERS_PRODUCTS_DB_PARAM_NAME);

		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		JSONObject res = new JSONObject();
		JSONArray allFollowersArray = new JSONArray();
		try {
			for (Map.Entry<String,ArrayList<String>> pair : followersDB.entrySet())
			{
				List<String> followers = pair.getValue();
				if(followers.size() == 0){
					continue;
				}
				JSONArray arrFollowers = new JSONArray(followers);

				JSONObject currFeatureFollowers = new JSONObject();
				currFeatureFollowers.put("uniqueID", pair.getKey());
				currFeatureFollowers.put("followers",arrFollowers);

				allFollowersArray.add(currFeatureFollowers);
			}
			res.put("allFollowers",allFollowersArray);

			ds.writeData(Constants.FOLLOWERS_PRODUCTS_FILE_NAME, res.write(true));
			changesArr.add(AirlockChangeContent.getAdminChange(res, Constants.FOLLOWERS_PRODUCTS_FILE_NAME, Stage.PRODUCTION));
			return changesArr;
		} catch (IOException ioe) {
			//failed writing roles 3 times to s3.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			String error = Strings.failedWritingProduct + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			throw new IOException(error);
		}
		catch (JSONException je) {
			//failed writing roles 3 times to s3.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			String error = Strings.failedWritingProduct + je.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			throw new IOException(error);
		}
	}

	//return error string upon error. null upon success
	//if product is not null - write the product runtime file is its seasons
	public static LinkedList<AirlockChangeContent> writeProducts(Map<String, Product> productsDB, ServletContext context, Product product) throws IOException {
		String error = null;
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		String seperator = ds.getSeparator();
		
		JSONObject productsRes = new JSONObject();
		JSONObject productRes = null;
		JSONObject onlyProductsWithOldSeasonsRes = new JSONObject(); //res containing only the 
		boolean productsWithOldSeasonsExist = false;
		//building products json
		try {
			JSONArray products = new JSONArray();

			Set<String> ids = productsDB.keySet();
			for (String id:ids) {
				Product p = productsDB.get(id);
				products.add(p.toJson(true, false, false)); //with seasons, without experiments						
			}
			productsRes.put(Constants.JSON_FIELD_PRODUCTS, products);
			
			if (product!=null) {
				productRes = product.toJsonForRuntime();
			}
			
			if (product == null || !product.allSeasonsSupportRuntimeInternalSeparation()) {
				JSONArray productsWithOldSeasons = new JSONArray();

				//in this case - old seasons exists in some products, we should write products.json file on the root for old sdks
				for (String id:ids) {
					Product p = productsDB.get(id);
					if(p.allSeasonsSupportRuntimeInternalSeparation())
						continue;
					productsWithOldSeasons.add(p.toJson(true, false, false)); //with seasons, without experiments
					productsWithOldSeasonsExist = true;
				}
				onlyProductsWithOldSeasonsRes.put(Constants.JSON_FIELD_PRODUCTS, productsWithOldSeasons);
			}
		} catch (JSONException je) {
			//should never happen - creating JSON object from memory should always succeed but just in-case ...
			//reload from s3 and return error
			logger.severe("Failed writing the products to S3. Illegal JSON format: " + je.getMessage());
			try {
				error = "Failed writing the products to S3 due to illegal JSON format. Reloading previous data from S3.";
				logger.info(error);
				JSONObject productsJSON  = ds.readDataToJSON(Constants.PRODUCTS_FILE_NAME);
				Utilities.initFromProductsJSON(productsJSON, context);

			} catch (IOException e) {
				String errMsg = String.format(Strings.failedReadingFile,Constants.PRODUCTS_FILE_NAME) + e.getMessage();
				error = error + "\n" + errMsg;
				logger.severe(errMsg);
				logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
				throw new IOException(errMsg);
			} catch (JSONException e) {
				String errMsg = Constants.PRODUCTS_FILE_NAME + " file is not in a legal JSON format: " + e.getMessage();
				error = error + "\n" + errMsg;
				logger.severe(errMsg);			
				logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
				throw new IOException(errMsg);
			}
			//roll back succeeded. Throwing an exception to inform the user but the server state is still RUNNING. (not a S3 problem) 
			logger.severe(error);			
			throw new IOException(error);
		}

		//validate the data consistency of the object that is going to be written to s3
		try {
			Utilities.doInitProductsListFromJson(productsRes, null, null, context);			
		} catch (JSONException e) {
			String errMsg = "Failed writing the products to S3. Illegal JSON format during data validation: " + e.getMessage();
			error = error + "\n" + errMsg;
			logger.severe(errMsg);			
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			throw new IOException(errMsg);
		}	
		//writing the created products json to s3
		try {			
			ds.writeData(Constants.PRODUCTS_FILE_NAME, productsRes.write(true));
			if (productsWithOldSeasonsExist) {
				//old seasons exists that do not support internal/runtime separation - write the products.json file to
				//the root of the runtime folder for prev sdks
				ds.writeDataToRuntimeFolder(Constants.PRODUCTS_FILE_NAME, onlyProductsWithOldSeasonsRes.write(true));
			}
			changesArr.add(AirlockChangeContent.getAdminChange(productsRes, Constants.PRODUCTS_FILE_NAME, Stage.PRODUCTION));
			if (productRes!=null) {
				for (Season s:product.getSeasons()) {
					String productRTFilePath= Constants.SEASONS_FOLDER_NAME + seperator + product.getUniqueId().toString() + seperator + 
							s.getUniqueId().toString() + seperator + Constants.PRODUCT_RUNTIME_FILE_NAME;
									
					writeData(context, ds, productRTFilePath, productRes.write(true), s, true);
					changesArr.add(AirlockChangeContent.getAdminChange(productRes, productRTFilePath, Stage.PRODUCTION));
				}
			}
			return changesArr;
		} catch (IOException ioe) {
			//failed writing 3 times to s3. Set server state to ERROR.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = "Failed writing the products to S3: " + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			throw new IOException(error);			
		}
		catch (JSONException je) {
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			error = "Failed writing the products to S3: " + je.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			throw new IOException(error);			
		}
	}

	public static LinkedList<AirlockChangeContent> writeProductRuntimeForSeason(ServletContext context, Product product, Season season, Stage stage) throws IOException {
		String error = null;
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		String seperator = ds.getSeparator();
		
		JSONObject productRes = null;

		//building products json
		try {
			productRes = product.toJsonForRuntime();			
		} catch (JSONException je) {
			//should never happen - creating JSON object from memory should always succeed but just in-case ...
			//reload from s3 and return error
			logger.severe("Failed writing the product runtime to S3. Illegal JSON format: " + je.getMessage());
			String errMsg = Constants.PRODUCTS_FILE_NAME + " file is not in a legal JSON format: " + je.getMessage();
			error = error + "\n" + errMsg;
			logger.severe(errMsg);			
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);				
			throw new IOException(errMsg);
		}

		
		//writing the created products json to s3
		try {			
			String productRTFilePath= Constants.SEASONS_FOLDER_NAME + seperator + product.getUniqueId().toString() + seperator + 
					season.getUniqueId().toString() + seperator + Constants.PRODUCT_RUNTIME_FILE_NAME;
								
			writeData(context, ds, productRTFilePath, productRes.write(true), season, true);
			changesArr.add(AirlockChangeContent.getRuntimeChange(productRes, productRTFilePath, stage));
			return changesArr;
		} catch (IOException ioe) {
			//failed writing 3 times to s3. Set server state to ERROR.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = "Failed writing the product runtime to S3: " + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			throw new IOException(error);			
		}
		catch (JSONException je) {
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			error = "Failed writing the product runtime to S3: " + je.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			throw new IOException(error);			
		}
	}

	//return error string upon error. null upon success
	public static LinkedList<AirlockChangeContent> writeSeasonBranches (Season season, ServletContext context, Environment env, Stage stage) throws IOException {
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		JSONObject branchesOutput = new JSONObject();
		String error = null;
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		String separator = ds.getSeparator();
		String branchesFileName = Constants.SEASONS_FOLDER_NAME+separator+season.getProductId().toString()+separator+season.getUniqueId().toString()+separator+Constants.AIRLOCK_BRANCHES_FILE_NAME;

		try {
			branchesOutput = season.getBranches().toJson(OutputJSONMode.ADMIN, context, env, false, false, false);			
		} catch (JSONException je) {
			//should never happen - creating JSON object from memory should always succeed but just in-case ...
			//reload from s3 and return error
			logger.severe("Failed writing the season's branches to S3. Illegal JSON format: " + je.getMessage());
			try {
				error = "Failed writing the season's branches to S3 due to illegal JSON format. Reloading previous data from S3.";
				logger.info(error);
				JSONObject branchesJSON  = ds.readDataToJSON(branchesFileName);
				Utilities.initFromSeasonBranchesJSON(branchesJSON, context, ds);		
			} catch (IOException e) {
				String errMsg = String.format(Strings.failedReadingFile,branchesFileName) + e.getMessage();
				error = error + "\n" + errMsg;
				logger.severe(errMsg);
				logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
				throw new IOException(errMsg);
			} catch (JSONException e) {
				String errMsg = branchesFileName + " file is not in a legal JSON format: " + e.getMessage();
				error = error + "\n" + errMsg;
				logger.severe(errMsg);			
				logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
				throw new IOException(errMsg);
			}
			//roll back succeeded. Throwing an exception to inform the user but the server state is still RUNNING. (not a S3 problem) 
			logger.severe(error);			
			throw new IOException(error);
		}  	

		//validate the data consistency of the object that is going to be written to s3
		try { 
			BranchesCollection tmp = new BranchesCollection(season);
			tmp.fromJSON(branchesOutput.getJSONArray(Constants.JSON_FIELD_BRANCHES), null, env, context);
		} catch (JSONException e) {
			String errMsg = "Failed writing the analytics to S3. Illegal JSON format during data validation: " + e.getMessage();
			error = error + "\n" + errMsg;
			logger.severe(errMsg);			
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			throw new IOException(errMsg);
		}	

		if (!season.isRuntimeInternalSeparationSupported()) {
			//For season older than 5.0, internal/runtime separation is not supported - write the airlock branches to runtime folder with its
			//original name
			try {			
				ds.writeDataToRuntimeFolder(branchesFileName, branchesOutput.write(true));	
			} catch (IOException ioe) {
				//failed writing 3 times to s3. Set server state to ERROR.
				//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
				error = "Failed writing branches to S3: " + ioe.getMessage();
				logger.severe(error);
				logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
				throw new IOException(error);			
			}
			catch (JSONException je) {
				//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
				error = "Failed writing branches to S3: " + je.getMessage();
				logger.severe(error);
				logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
				throw new IOException(error);			
			}
		}

		try {			
			ds.writeData(branchesFileName, branchesOutput.write(true));	
			changesArr.add(AirlockChangeContent.getAdminChange(branchesOutput, branchesFileName, stage));
			//write encrypted runtime file for the sdk to be able to access the branches list
			String branchesRuntimeFileName = Constants.SEASONS_FOLDER_NAME+separator+season.getProductId().toString()+separator+season.getUniqueId().toString()+separator+Constants.AIRLOCK_RUNTIME_BRANCHES_FILE_NAME;
			writeData(context, ds, branchesRuntimeFileName, branchesOutput.write(true), season, true);
			changesArr.add(AirlockChangeContent.getRuntimeChange(branchesOutput, branchesRuntimeFileName, stage));
			return changesArr;
		} catch (IOException ioe) {
			//failed writing 3 times to s3. Set server state to ERROR.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = "Failed writing branches to S3: " + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			throw new IOException(error);			
		}
		catch (JSONException je) {
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = "Failed writing branches to S3: " + je.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			throw new IOException(error);			
		}	
		
			
	}

	public static LinkedList<AirlockChangeContent> writeSeasonUtilities (Season season, boolean writeProduction, ServletContext context, UtilityType type) throws IOException {
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		JSONObject utilsOutput = new JSONObject();
		String error = null;
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		String separator = ds.getSeparator();
		String utilitiesFileName = Constants.SEASONS_FOLDER_NAME+separator+season.getProductId().toString()+separator+season.getUniqueId().toString()+separator+Constants.AIRLOCK_UTILITIES_FILE_NAME;


		try {
			utilsOutput = season.getUtilities().toJson(null);			
		} catch (JSONException je) {
			//should never happen - creating JSON object from memory should always succeed but just in-case ...
			//reload from s3 and return error
			logger.severe("Failed writing the season's utilities to S3. Illegal JSON format: " + je.getMessage());
			try {
				error = "Failed writing the season's utilities to S3 due to illegal JSON format. Reloading previous data from S3.";
				logger.info(error);
				JSONObject utilitiessJSON  = ds.readDataToJSON(utilitiesFileName);
				Utilities.initFromSeasonUtilitiesJSON(utilitiessJSON, context);		
			} catch (IOException e) {
				String errMsg = String.format(Strings.failedReadingFile,utilitiesFileName) + e.getMessage();
				error = error + "\n" + errMsg;
				logger.severe(errMsg);
				logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
				throw new IOException(errMsg);
			} catch (JSONException e) {
				String errMsg = utilitiesFileName + " file is not in a legal JSON format: " + e.getMessage();
				error = error + "\n" + errMsg;
				logger.severe(errMsg);			
				logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
				throw new IOException(errMsg);
			}
			//roll back succeeded. Throwing an exception to inform the user but the server state is still RUNNING. (not a S3 problem) 
			logger.severe(error);			
			throw new IOException(error);
		}				

		//validate the data consistency of the object that is going to be written to s3
		try { 
			Season.Utilities tmp = season.new Utilities();
			JSONArray utilitiesSArr = utilsOutput.getJSONArray(Constants.JSON_FIELD_UTILITIES);			
			tmp.fromJSON(utilitiesSArr, null);			
		} catch (JSONException e) {
			String errMsg = "Failed writing the utilities to S3. Illegal JSON format during data validation: " + e.getMessage();
			error = error + "\n" + errMsg;
			logger.severe(errMsg);			
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			throw new IOException(errMsg);
		}	

		try {			
			ds.writeData(utilitiesFileName, utilsOutput.write(true));
			changesArr.add(AirlockChangeContent.getAdminChange(utilsOutput, utilitiesFileName, Stage.PRODUCTION));
		} catch (IOException ioe) {
			//failed writing 3 times to s3. Set server state to ERROR.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = "Failed writing utilities to S3: " + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			throw new IOException(error);			
		}
		catch (JSONException je) {
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = "Failed writing utilities to S3: " + je.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			throw new IOException(error);			
		}

		if (season.isOldRuntimeFileExists()) 
			return changesArr; //in old season don't write AirlockUtilitiesDEVELOPMENT.json and AirlockUtilitiesPRODUCTION.json

		//write development (dev+prod) utilities 
		try {
			String devUtilitiesFileName = type.equals(UtilityType.MAIN_UTILITY) ? Constants.AIRLOCK_UTILITIES_DEVELOPMENT_FILE_NAME : Constants.AIRLOCK_STREAMS_UTILITIES_DEVELOPMENT_FILE_NAME;
			String devUtilitiesFilePath = Constants.SEASONS_FOLDER_NAME+separator+season.getProductId().toString()+separator+season.getUniqueId().toString()+separator+devUtilitiesFileName;		
			String devUtilsSection = season.getUtilities().generateUtilityCodeSectionForStageAndType(null, null, null, null, type);

			//ds.writeData(devUtilitiesFilePath, devUtilsSection, true);
			writeData(context, ds, devUtilitiesFilePath, devUtilsSection, season, true);
			changesArr.add(AirlockChangeContent.getAdminChange(devUtilsSection, devUtilitiesFilePath, Stage.DEVELOPMENT));
		} catch (IOException ioe) {
			//failed writing 3 times to s3. Set server state to ERROR.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = "Failed writing utilities to S3: " + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			throw new IOException(error);			
		}


		if (writeProduction) {
			//write production utilities 
			try {
				String prodUtilitiesFileName = type.equals(UtilityType.MAIN_UTILITY) ? Constants.AIRLOCK_UTILITIES_PRODUCTION_FILE_NAME : Constants.AIRLOCK_STREAMS_UTILITIES_PRODUCTION_FILE_NAME;
				String prodUtilitiesFilePath = Constants.SEASONS_FOLDER_NAME+separator+season.getProductId().toString()+separator+season.getUniqueId().toString()+separator+prodUtilitiesFileName;		

				String prodUtilsSection = season.getUtilities().generateUtilityCodeSectionForStageAndType(Stage.PRODUCTION, null, null, null, type);

				changesArr.addAll(Utilities.touchProductionChangedFile(context, ds, season));
				//ds.writeData(prodUtilitiesFilePath, prodUtilsSection, true);
				writeData(context, ds, prodUtilitiesFilePath, prodUtilsSection, season, true);
				changesArr.add(AirlockChangeContent.getAdminChange(prodUtilsSection, prodUtilitiesFilePath, Stage.PRODUCTION));
			} catch (IOException ioe) {
				//failed writing 3 times to s3. Set server state to ERROR.
				//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
				error = "Failed writing utilities to S3: " + ioe.getMessage();
				logger.severe(error);
				logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
				throw new IOException(error);			
			}		
		}
		return changesArr;
	}

	//return error string upon error. null upon success
	public static LinkedList<AirlockChangeContent> writeServerInfo(Season season, ServletContext context) throws IOException {
		String error = null;
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		String separator = ds.getSeparator();
		JSONObject serverInfoObj = new JSONObject();

		//building server info json
		try {
			serverInfoObj = new JSONObject();
			serverInfoObj.put(Constants.JSON_FIELD_SERVER_VERSION, season.getServerVersion());				
		} catch (JSONException je) {
			//should never happen - creating JSON object from memory should always succeed but just in-case ...
			error = "Failed writing the server info to S3. Illegal JSON format: " + je.getMessage();				
			logger.severe(error);			
			throw new IOException(error);
		}

		//writing the created products json to s3
		try {		
			String filename = Constants.SEASONS_FOLDER_NAME+separator+season.getProductId().toString()+separator+season.getUniqueId().toString()+separator+Constants.AIRLOCK_SERVER_INFO_FILE_NAME;
			ds.writeData(filename, serverInfoObj.write(true));
			changesArr.add(AirlockChangeContent.getAdminChange(serverInfoObj, filename, Stage.PRODUCTION));
			return changesArr;
		} catch (IOException ioe) {
			//failed writing 3 times to s3. Set server state to ERROR.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = "Failed writing the server info to S3: " + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			throw new IOException(error);			
		}
		catch (JSONException je) {
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			error = "Failed writing the server info to S3: " + je.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			throw new IOException(error);			
		}
	}
	
	//return error string upon error. null upon success
	public static LinkedList<AirlockChangeContent> writeEncryptionKey(Season season, ServletContext context) throws IOException {
		String error = null;
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		String separator = ds.getSeparator();
		JSONObject encryptionKeyObj = new JSONObject();

		//building server info json
		try {
			encryptionKeyObj = new JSONObject();
			encryptionKeyObj.put(Constants.JSON_FIELD_ENCRYPTION_KEY, season.getEncryptionKey());
			encryptionKeyObj.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, season.getUniqueId().toString());
		} catch (JSONException je) {
			//should never happen - creating JSON object from memory should always succeed but just in-case ...
			error = "Failed writing the encryption key to S3. Illegal JSON format: " + je.getMessage();				
			logger.severe(error);			
			throw new IOException(error);
		}

		//writing the created products json to s3
		try {		
			String filename = Constants.SEASONS_FOLDER_NAME+separator+season.getProductId().toString()+separator+season.getUniqueId().toString()+separator+Constants.AIRLOCK_ENCRYTION_KEY_FILE_NAME;
			ds.writeData(filename, encryptionKeyObj.write(true));
			changesArr.add(AirlockChangeContent.getAdminChange(encryptionKeyObj, filename, Stage.PRODUCTION));
			return changesArr;
		} catch (IOException ioe) {
			//failed writing 3 times to s3. Set server state to ERROR.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = "Failed writing the encryption key to S3: " + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			throw new IOException(error);			
		}
		catch (JSONException je) {
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			error = "Failed writing the encryption key to S3: " + je.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			throw new IOException(error);			
		}
	}


	//Strings
	public static LinkedList<AirlockChangeContent> writeAllLocalesStringsFiles(Season season, ServletContext context, boolean writeProduction, boolean includeDefaultLanguage) throws IOException {
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		LinkedList<String> supportedLangs = season.getOriginalStrings().getSupportedLanguages();
		long start = new Date().getTime(); // XXX TEMPORARY
		for (int i=0; i<supportedLangs.size(); i++) {
			String supportedLang = supportedLangs.get(i);
			if (!includeDefaultLanguage && supportedLang.equals(Constants.DEFAULT_LANGUAGE)) //skip English file since it is updated during original string write
				continue;
			
			changesArr.addAll(writeLocaleStringsFiles(season, supportedLang, context, writeProduction));
		}
		long diff = new Date().getTime() - start;
		logger.info(" write ALL LocalesStringsFiles took " + diff + " ms");
		return changesArr;
	}

	public static LinkedList<AirlockChangeContent> writeLocaleStringsFiles(Season season, String locale, ServletContext context, boolean writeProduction) throws IOException {
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		String separator = ds.getSeparator();
		long start = new Date().getTime(); // XXX TEMPORARY
		try {
			JSONObject devLocaleStrings = season.getOriginalStrings().toLocaleStringsJSON(locale, null);

			if (season.getOriginalStrings().isOldEnStringsFileExists()) {
				//if oldLocaleStringsFile (without DEVELOPMENT or PRODUCTION) exists this means that we are in an old season therefore only development content is written to 
				//the oldLocaleStringsFile (strings__<locale>.json). In old seasons production and development files are not written
				String oldLocaleStringsFile = Constants.SEASONS_FOLDER_NAME+separator+season.getProductId().toString()+
						separator+season.getUniqueId().toString()+separator+Constants.TRANSLATIONS_FOLDER_NAME + 
						separator + Constants.STRINGS_FILE_NAME_PREFIX + locale + Constants.STRINGS_FILE_NAME_EXTENSION;

				//ds.writeData(oldLocaleStringsFile, devLocaleStrings.write(true), true);
				writeData(context, ds, oldLocaleStringsFile, devLocaleStrings.write(true), season, true);
				changesArr.add(AirlockChangeContent.getAdminChange(devLocaleStrings, oldLocaleStringsFile, Stage.DEVELOPMENT));
				return changesArr;
			}

			String devLocaleStringsFilePath = Constants.SEASONS_FOLDER_NAME+separator+season.getProductId().toString()+
					separator+season.getUniqueId().toString()+separator+Constants.TRANSLATIONS_FOLDER_NAME + 
					separator + Constants.STRINGS_FILE_NAME_PREFIX + locale + Stage.DEVELOPMENT.toString() + Constants.STRINGS_FILE_NAME_EXTENSION;

			//ds.writeData(devLocaleStringsFilePath, devLocaleStrings.write(true), true);
			writeData(context, ds, devLocaleStringsFilePath, devLocaleStrings.write(true), season, true);
			changesArr.add(AirlockChangeContent.getAdminChange(devLocaleStrings, devLocaleStringsFilePath, Stage.DEVELOPMENT));
			if (writeProduction) {
				JSONObject prodLocaleStrings = season.getOriginalStrings().toLocaleStringsJSON(locale, Stage.PRODUCTION);


				String prodEnglishStringsFilePath = Constants.SEASONS_FOLDER_NAME+separator+season.getProductId().toString()+
						separator+season.getUniqueId().toString()+separator+Constants.TRANSLATIONS_FOLDER_NAME +
						separator + Constants.STRINGS_FILE_NAME_PREFIX + locale + Stage.PRODUCTION.toString() + Constants.STRINGS_FILE_NAME_EXTENSION;
				changesArr.addAll(Utilities.touchProductionChangedFile(context, ds, season));

				//ds.writeData(prodEnglishStringsFilePath, prodLocaleStrings.write(true), true);
				writeData(context, ds, prodEnglishStringsFilePath, prodLocaleStrings.write(true), season, true);
				changesArr.add(AirlockChangeContent.getAdminChange(prodLocaleStrings, prodEnglishStringsFilePath, Stage.PRODUCTION));
			}
		} catch (IOException ioe) {
			//failed writing 3 times to s3. Set server state to ERROR.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			String error = "Failed writing " + locale + " strings file to S3: " + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			throw new IOException(error);			
		}
		catch (JSONException je) {
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			String error = "Failed writing " + locale + " strings file to S3: " + je.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			throw new IOException(error);			
		}
		finally 
		{
			long diff = new Date().getTime() - start;
			logger.info(" writeLocaleStringsFiles took " + diff + " ms");
		}
		return changesArr;
	}


	public static LinkedList<AirlockChangeContent> writeOriginalStrings (Season season, ServletContext context, Stage stage) throws IOException {
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		JSONObject output = new JSONObject();
		String error = null;
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		String separator = ds.getSeparator();
		String fileName = Constants.SEASONS_FOLDER_NAME+separator+season.getProductId().toString()+
				separator+season.getUniqueId().toString()+separator+Constants.TRANSLATIONS_FOLDER_NAME + 
				separator + Constants.ORIGINAL_STRINGS_FILE_NAME;


		try {
			output = season.getOriginalStrings().toJson(StringsOutputMode.INCLUDE_TRANSLATIONS);
		} catch (JSONException je) {
			//should never happen - creating JSON object from memory should always succeed but just in-case ...
			//reload from s3 and return error
			logger.severe("Failed writing the season's original strings to S3. Illegal JSON format: " + je.getMessage());
			try {
				error = "Failed writing the season's original strings to S3 due to illegal JSON format. Reloading previous data from S3.";
				logger.info(error);
				JSONObject utilitiessJSON  = ds.readDataToJSON(fileName);
				Utilities.initFromSeasonOriginalStringsJSON(utilitiessJSON, context);		
			} catch (IOException e) {
				String errMsg =String.format(Strings.failedReadingFile,fileName) + e.getMessage();
				error = error + "\n" + errMsg;
				logger.severe(errMsg);
				logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
				throw new IOException(errMsg);
			} catch (JSONException e) {
				String errMsg = fileName + " file is not in a legal JSON format: " + e.getMessage();
				error = error + "\n" + errMsg;
				logger.severe(errMsg);			
				logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
				throw new IOException(errMsg);
			}
			//roll back succeeded. Throwing an exception to inform the user but the server state is still RUNNING. (not a S3 problem) 
			logger.severe(error);			
			throw new IOException(error);
		}				

		long start = new Date().getTime(); // XXX TEMPORARY

		//validate the data consistency of the object that is going to be written to s3
		try { 
			OriginalStrings tmp = new OriginalStrings(season);
			tmp.fromJSON(output, null);
		} catch (JSONException e) {
			String errMsg = "Failed writing the original strings to S3. Illegal JSON format during data validation: " + e.getMessage();
			error = error + "\n" + errMsg;
			logger.severe(errMsg);			
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			throw new IOException(errMsg);
		}	
		
		try {			
			ds.writeData(fileName, output.write(true));
			changesArr.add(AirlockChangeContent.getAdminChange(output, fileName, stage));
		} catch (IOException ioe) {
			//failed writing 3 times to s3. Set server state to ERROR.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = "Failed writing original strings to S3: " + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			throw new IOException(error);			
		}
		catch (JSONException je) {
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = "Failed writing original strings to S3: " + je.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			throw new IOException(error);			
		}
		finally {
			long diff = new Date().getTime() - start;
			logger.info(" writeOriginalStrings took " + diff + " ms");
		}
		return changesArr;
	}

	//write the strings__enPRODUCTION.json (if needed) and the strings__enDEVELOPMENT.json 	
	public static LinkedList<AirlockChangeContent> writeEnStringsFiles(Season season, ServletContext context, boolean writeProduction)  throws IOException, JSONException { 
		String error = null;
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		long start = new Date().getTime(); // XXX TEMPORARY
		try {
			changesArr.addAll(doWriteEnStringsFileForSeason (season, context, writeProduction));
		} catch (IOException ioe) {
			//failed writing 3 times to s3. Set server state to ERROR.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = "Failed writing english strings file to S3: " + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			throw new IOException(error);			
		}
		catch (JSONException je) {
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = "Failed writing english strings file to S3: " + je.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			throw new IOException(error);			
		}
		finally {
			long diff = new Date().getTime() - start;
			logger.info(" writeEnStringsFiles took " + diff + " ms");
		}
		return changesArr;
	}

	private static LinkedList<AirlockChangeContent> doWriteEnStringsFileForSeason(Season season, ServletContext context, boolean writeProduction)  throws IOException, JSONException {
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		String separator = ds.getSeparator();
		JSONObject devEnglishStrings = season.getOriginalStrings().toEnStringsJSON(null);

		String devEnglishStringsFilePath = Constants.SEASONS_FOLDER_NAME+separator+season.getProductId().toString()+
				separator+season.getUniqueId().toString()+separator+Constants.TRANSLATIONS_FOLDER_NAME + 
				separator + Constants.STRINGS_FILE_NAME_PREFIX + Constants.DEFAULT_LANGUAGE + Stage.DEVELOPMENT.toString() + Constants.STRINGS_FILE_NAME_EXTENSION;



		//if strings__en.json file exist which means it is an old season - set its content as the strings__enDEVELOPMENT.json content		
		if (season.getOriginalStrings().isOldEnStringsFileExists()) {
			String oldEnglishStringsFilePath = Constants.SEASONS_FOLDER_NAME+separator+season.getProductId().toString()+
					separator+season.getUniqueId().toString()+separator+Constants.TRANSLATIONS_FOLDER_NAME + 
					separator + Constants.STRINGS_FILE_NAME_PREFIX + Constants.DEFAULT_LANGUAGE + Constants.STRINGS_FILE_NAME_EXTENSION;

			//ds.writeData(oldEnglishStringsFilePath, devEnglishStrings.write(true), true);
			writeData(context, ds, oldEnglishStringsFilePath, devEnglishStrings.write(true), season, true);
			changesArr.add(AirlockChangeContent.getAdminChange(devEnglishStrings, oldEnglishStringsFilePath, Stage.DEVELOPMENT));
			return changesArr; //if in old season write only strings__en.json
		}		

		//ds.writeData(devEnglishStringsFilePath, devEnglishStrings.write(true), true);
		writeData(context, ds, devEnglishStringsFilePath, devEnglishStrings.write(true), season, true);
		changesArr.add(AirlockChangeContent.getAdminChange(devEnglishStrings, devEnglishStringsFilePath, Stage.DEVELOPMENT));

		if (writeProduction) {
			JSONObject prodEnglishStrings = season.getOriginalStrings().toEnStringsJSON(Stage.PRODUCTION);

			String prodEnglishStringsFilePath = Constants.SEASONS_FOLDER_NAME+separator+season.getProductId().toString()+
					separator+season.getUniqueId().toString()+separator+Constants.TRANSLATIONS_FOLDER_NAME + 
					separator + Constants.STRINGS_FILE_NAME_PREFIX + Constants.DEFAULT_LANGUAGE + Stage.PRODUCTION.toString() + Constants.STRINGS_FILE_NAME_EXTENSION;

			changesArr.addAll(Utilities.touchProductionChangedFile(context, ds, season));

			//ds.writeData(prodEnglishStringsFilePath, prodEnglishStrings.write(true), true);
			writeData(context, ds, prodEnglishStringsFilePath, prodEnglishStrings.write(true), season, true);
			changesArr.add(AirlockChangeContent.getAdminChange(prodEnglishStrings, prodEnglishStringsFilePath, Stage.PRODUCTION));
		}
		return changesArr;
	}
	
	public static void writeData(ServletContext context, DataSerializer ds, String filePath, String data, Season season, boolean publish) throws IOException {
		Environment env = new Environment();
		env.setServerVersion(season.getServerVersion());
		//boolean isProductRuntimeEncrypted = Utilities.isProductSupportRuntimeEncryption(context, season);
		boolean isSeasonRuntimeEncrypted = season.getRuntimeEncryption();
		
		if (isSeasonRuntimeEncrypted && publish && ProductServices.isEncryptionSupported(env)) { //published files of season from version 5_0 should be encrypted
			try {				
				byte[] key = Encryption.fromString(season.getEncryptionKey());
			    Encryption e = new Encryption(key);
			       
				byte[] compressed = Compression.compress(data);
			    byte[] encrypted = e.encrypt(compressed);
				ds.writeInputStreamData(filePath, new ByteArrayInputStream(encrypted), encrypted.length);
			} catch (GeneralSecurityException e) {
				throw new IOException("GeneralSecurityException: " + e.getMessage());
			}		    
		}
		else {
			ds.writeData(filePath, data, publish);
		}
	}
	
	//write the branch runtime files + write the master runtime files if the branch is part of experiment.
	//Assuming branch is not null.
	public static LinkedList<AirlockChangeContent> writeBranchAndMasterRuntimeFiles(Season season, Branch branch, ServletContext context, Environment env, boolean writeProduction) throws IOException, JSONException {
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		changesArr.addAll(AirlockFilesWriter.writeBranchRuntime(branch, season, context, env, writeProduction));
		Stage varianStage = ProductServices.isBranchInExp(branch, season, context); //return null if not in exp. If in exp return the max variant stage
		if (varianStage!=null) {			
			changesArr.addAll(AirlockFilesWriter.doWriteFeatures(season, OutputJSONMode.RUNTIME_DEVELOPMENT, context, Stage.DEVELOPMENT, env));
			if (varianStage.equals(Stage.PRODUCTION) && writeProduction)
				changesArr.addAll(AirlockFilesWriter.doWriteFeatures(season, OutputJSONMode.RUNTIME_PRODUCTION, context, Stage.PRODUCTION, env));			
		}
		return changesArr;
	}
	
	
	public static LinkedList<AirlockChangeContent> writeExperiments (Product product, ServletContext context, Stage stage) throws IOException {
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		String error = null;
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		String separator = ds.getSeparator();
		String productExperimentsFileName = Constants.SEASONS_FOLDER_NAME + 
				separator + product.getUniqueId().toString() + separator
				+Constants.AIRLOCK_EXPERIMENTS_FILE_NAME;							

		JSONObject experimentsJson = null;
		try {
			experimentsJson = product.getExperimentsMutualExclusionGroup().toJson(OutputJSONMode.ADMIN, context, false);
		} catch (JSONException je) {
			//should never happen - creating JSON object from memory should always succeed but just in-case ...
			//reload from s3 and return error
			logger.severe("Failed writing product " +  product.getName() + ", " + product.getUniqueId().toString() + " experiments  to S3. Illegal JSON format: " + je.getMessage());			
			throw new IOException(error);
		}				

		try {			
			ds.writeData(productExperimentsFileName, experimentsJson.write(true));
			changesArr.add(AirlockChangeContent.getAdminChange(experimentsJson, productExperimentsFileName, stage));
		} catch (IOException ioe) {
			//failed writing 3 times to s3. Set server state to ERROR.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = "Failed writing product " +  product.getName() + ", " + product.getUniqueId().toString() + " experiments to S3: " + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			throw new IOException(error);			
		}
		catch (JSONException je) {
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = "Failed writing product " +  product.getName() + ", " + product.getUniqueId().toString() + " experiments to S3: " + je.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			throw new IOException(error);			
		}
		return changesArr;
	}

	public static LinkedList<AirlockChangeContent> writeSeasonStreams (Season season, boolean writeProduction, ServletContext context) throws IOException {
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		JSONObject streamsOutput = new JSONObject();
		String error = null;
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		String separator = ds.getSeparator();
		String streamsFileName = Constants.SEASONS_FOLDER_NAME+separator+season.getProductId().toString()+separator+season.getUniqueId().toString()+separator+Constants.AIRLOCK_STREAMS_FILE_NAME;

		try {
			streamsOutput = season.getStreams().toJson(OutputJSONMode.ADMIN);			
		} catch (JSONException je) {
			//should never happen - creating JSON object from memory should always succeed but just in-case ...
			//reload from s3 and return error
			logger.severe("Failed writing the season's streams to S3. Illegal JSON format: " + je.getMessage());
			try {
				error = "Failed writing the season's streams to S3 due to illegal JSON format. Reloading previous data from S3.";
				logger.info(error);
				JSONObject streamsJSON  = ds.readDataToJSON(streamsFileName);
				Utilities.initFromSeasonStreamsJSON(streamsJSON, context);		
			} catch (IOException e) {
				String errMsg = String.format(Strings.failedReadingFile,streamsFileName) + e.getMessage();
				error = error + "\n" + errMsg;
				logger.severe(errMsg);
				logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
				throw new IOException(errMsg);
			} catch (JSONException e) {
				String errMsg = streamsFileName + " file is not in a legal JSON format: " + e.getMessage();
				error = error + "\n" + errMsg;
				logger.severe(errMsg);			
				logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
				throw new IOException(errMsg);
			}
			//roll back succeeded. Throwing an exception to inform the user but the server state is still RUNNING. (not a S3 problem) 
			logger.severe(error);			
			throw new IOException(error);
		}				

		//validate the data consistency of the object that is going to be written to s3
		try { 
			AirlockStreamsCollection tmp = new AirlockStreamsCollection(season.getUniqueId());
			tmp.fromJSON(streamsOutput.getJSONArray(Constants.JSON_FIELD_STREAMS), null);
		} catch (JSONException e) {
			String errMsg = "Failed writing the streams to S3. Illegal JSON format during data validation: " + e.getMessage();
			error = error + "\n" + errMsg;
			logger.severe(errMsg);			
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			throw new IOException(errMsg);
		}	
		
		try {			
			ds.writeData(streamsFileName, streamsOutput.write(true));
			changesArr.add(AirlockChangeContent.getAdminChange(streamsOutput, streamsFileName, Stage.PRODUCTION));
		} catch (IOException ioe) {
			//failed writing 3 times to s3. Set server state to ERROR.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = "Failed writing streams to S3: " + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			throw new IOException(error);			
		}
		catch (JSONException je) {
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = "Failed writing streams to S3: " + je.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			throw new IOException(error);			
		}
		
		//write development (dev+prod) streams 
		try {	
			String devStreamsFileName = Constants.SEASONS_FOLDER_NAME+separator+season.getProductId().toString()+separator+season.getUniqueId().toString()+separator+Constants.AIRLOCK_STREAMS_DEVELOPMENT_FILE_NAME;		
						
			streamsOutput = season.getStreams().toJson(OutputJSONMode.RUNTIME_DEVELOPMENT);
			//ds.writeData(devStreamsFileName, streamsOutput.write(true), true);
			writeData(context, ds, devStreamsFileName, streamsOutput.write(true), season, true);
			changesArr.add(AirlockChangeContent.getRuntimeChange(streamsOutput, devStreamsFileName, Stage.DEVELOPMENT));
		} catch (IOException ioe) {
			//failed writing 3 times to s3. Set server state to ERROR.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = "Failed writing runtime development streams to S3: " + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			throw new IOException(error);			
		}
		catch (JSONException je) {
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = "Failed writing runtime development streams to S3: " + je.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			throw new IOException(error);			
		}
		
		if (writeProduction) {
			//write production streams 
			try {
				String prodStreamsFileName = Constants.SEASONS_FOLDER_NAME+separator+season.getProductId().toString()+separator+season.getUniqueId().toString()+separator+Constants.AIRLOCK_STREAMS_PRODUCTION_FILE_NAME;		
				streamsOutput = season.getStreams().toJson(OutputJSONMode.RUNTIME_PRODUCTION);
				
				changesArr.addAll(Utilities.touchProductionChangedFile(context, ds, season));
				//ds.writeData(prodStreamsFileName, streamsOutput.write(true), true);
				writeData(context, ds, prodStreamsFileName, streamsOutput.write(true), season, true);
				changesArr.add(AirlockChangeContent.getRuntimeChange(streamsOutput, prodStreamsFileName, Stage.PRODUCTION));
			} catch (IOException ioe) {
				//failed writing 3 times to s3. Set server state to ERROR.
				//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
				error = "Failed writing runtime production streams to S3: " + ioe.getMessage();
				logger.severe(error);
				logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
				throw new IOException(error);			
			} catch (JSONException je) {
				//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
				error = "Failed writing runtime production streams to S3: " + je.getMessage();
				logger.severe(error);
				logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
				throw new IOException(error);			
			}	
		}
		return changesArr;
	}
	
	public static LinkedList<AirlockChangeContent> writeSeasonStreamsEvents(Season season, ServletContext context) throws IOException {			
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		JSONObject output = new JSONObject();
		String error = null;
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		String separator = ds.getSeparator();
		String fileName = Constants.SEASONS_FOLDER_NAME+separator+season.getProductId().toString()+
				separator+season.getUniqueId().toString()+separator + Constants.AIRLOCK_STREAMS_EVENTS_FILE_NAME;

		try {
			output = season.getStreamsEvents().toJson();
		} catch (JSONException je) {
			//should never happen - creating JSON object from memory should always succeed but just in-case ...
			//reload from s3 and return error
			logger.severe("Failed writing the season's streams events to S3. Illegal JSON format: " + je.getMessage());
			try {
				error = "Failed writing the season's streams events to S3 due to illegal JSON format. Reloading previous data from S3.";
				logger.info(error);
				JSONObject streamsJSON  = ds.readDataToJSON(fileName);
				Utilities.initFromSeasonOriginalStringsJSON(streamsJSON, context);		
			} catch (IOException e) {
				String errMsg =String.format(Strings.failedReadingFile,fileName) + e.getMessage();
				error = error + "\n" + errMsg;
				logger.severe(errMsg);
				logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
				throw new IOException(errMsg);
			} catch (JSONException e) {
				String errMsg = fileName + " file is not in a legal JSON format: " + e.getMessage();
				error = error + "\n" + errMsg;
				logger.severe(errMsg);			
				logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
				throw new IOException(errMsg);
			}
			//roll back succeeded. Throwing an exception to inform the user but the server state is still RUNNING. (not a S3 problem) 
			logger.severe(error);			
			throw new IOException(error);
		}				

		//validate the data consistency of the object that is going to be written to s3
		try { 
			StreamsEvents tmp  = new StreamsEvents(season.getUniqueId());
			tmp.fromJSON(output);
		} catch (JSONException e) {
			String errMsg = "Failed writing the stream events to S3. Illegal JSON format during data validation: " + e.getMessage();
			error = error + "\n" + errMsg;
			logger.severe(errMsg);			
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			throw new IOException(errMsg);
		}	
		
		try {			
			ds.writeData(fileName, output.write(true));
			changesArr.add(AirlockChangeContent.getAdminChange(output, fileName, Stage.PRODUCTION));
		} catch (IOException ioe) {
			//failed writing 3 times to s3. Set server state to ERROR.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = "Failed writing streams events to S3: " + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			throw new IOException(error);			
		}
		catch (JSONException je) {
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = "Failed writing streams events to S3: " + je.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			throw new IOException(error);			
		}
		return changesArr;
	}


	//return error string upon error. null upon success
	public static Pair<String,LinkedList<AirlockChangeContent>> writeUserGroups(InternalUserGroups internalUserGroups, ServletContext context, Product product) throws IOException {
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);

		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);

		String error = null;
		String seperator = ds.getSeparator();
		
		JSONObject userGroupsJson = null;
		try {
			userGroupsJson = internalUserGroups.toJson();
		} catch (JSONException je) {
			//should never happen - creating JSON object from memory should always succeed 
			error = "Failed writing product " +  product.getName() + ", " + product.getUniqueId().toString() + " user groups  to S3. Illegal JSON format: " + je.getMessage();
			logger.severe(error);			
			throw new IOException(error);
		}
		
		String productId = product.getUniqueId().toString();
		String filePath = Constants.SEASONS_FOLDER_NAME+ seperator + productId + seperator + Constants.USER_GROUPS_FILE_NAME;
		try {			
			ds.writeData(filePath, userGroupsJson.write(true));
			changesArr.add(AirlockChangeContent.getAdminChange(userGroupsJson, filePath, Stage.DEVELOPMENT));
		} catch (IOException ioe) {
			//failed writing 3 times to s3. reload from s3 and return error			
			try {
				error = "Failed writing userGroups of product " + productId + " to s3. Discarding change by reloading previous data from s3";
				logger.info(error);
				JSONObject usrGroupsJSON  = ds.readDataToJSON(filePath);
				Utilities.initFromUserGroupsJson (usrGroupsJSON, context, productId);
			} catch (IOException e) {
				String errMsg = String.format(Strings.failedReadingFile,Constants.USER_GROUPS_FILE_NAME) + e.getMessage();
				error = error + "\n" + errMsg;
				logger.severe(errMsg);	
				logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
				throw new IOException(errMsg);
			} catch (JSONException e) {
				String errMsg = Constants.USER_GROUPS_FILE_NAME + " file is not a legal json: " + e.getMessage();
				error = error + "\n" + errMsg;
				logger.severe(errMsg);		
				logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
				throw new IOException(errMsg);
			}	
		} catch (JSONException e) {
			String errMsg = Constants.USER_GROUPS_FILE_NAME + " file is not a legal json: " + e.getMessage();
			error = error + "\n" + errMsg;
			logger.severe(errMsg);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);			
			throw new IOException(errMsg);
		}	
		
		//write the encrypted files for each season in the product
		for (Season s:product.getSeasons()) {
			try {
				String seasonId = s.getUniqueId().toString();
				String seasonUserGroupsRuntimePath = Constants.SEASONS_FOLDER_NAME + seperator + productId + seperator + 
						seasonId + seperator + Constants.USER_GROUPS_RUNTIME_FILE_NAME;
				
				writeData(context, ds, seasonUserGroupsRuntimePath, userGroupsJson.write(true), s, true);
				changesArr.add(AirlockChangeContent.getRuntimeChange(userGroupsJson, seasonUserGroupsRuntimePath, Stage.DEVELOPMENT));
			} catch (JSONException e) {
				String errMsg = Constants.USER_GROUPS_FILE_NAME + " file is not a legal json: " + e.getMessage();
				error = error + "\n" + errMsg;
				logger.severe(errMsg);
				logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);			
				throw new IOException(errMsg);
			}
		}

		//for product that has old seasons that do not support runtime/internal folder separation - write the users groups of all 
		//products that has old seasons in the runtime folder root
		if (!product.allSeasonsSupportRuntimeInternalSeparation()) {
			@SuppressWarnings("unchecked")
			Map<String,InternalUserGroups> groupsPerProductMap = (Map<String,InternalUserGroups>) context.getAttribute(Constants.USER_GROUPS_PER_PRODUCT_PARAM_NAME);
			
			//for all products that contain old season - merge their user groups and write them to the runtime root
			TreeSet<String> mergedUserGroupsSet = new TreeSet<String>();
			Set<String> ids = productsDB.keySet();
			for (String id:ids) {
				Product p = productsDB.get(id);
				if (!p.allSeasonsSupportRuntimeInternalSeparation()) {
					InternalUserGroups oldProdGroupsList = groupsPerProductMap.get(id);
					if (oldProdGroupsList == null) //this can happen the first time the server starts and some products still dont have userGroups
						continue;
					mergedUserGroupsSet.addAll(oldProdGroupsList.getGroups());
				}					
			}
			InternalUserGroups mergedUserGroups = new InternalUserGroups();
			mergedUserGroups.setGroups(mergedUserGroupsSet);
			try {			
				ds.writeDataToRuntimeFolder(Constants.USER_GROUPS_FILE_NAME, mergedUserGroups.toJson().write(true));
			}
			catch (IOException e) {
				//failed writing 3 times to s3. Set server state to ERROR.
				//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
				error = "Failed writing user groups to runtime root in S3: " + e.getMessage();
				logger.severe(error);
				logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
				throw new IOException(error);
			}
			catch (JSONException e) {
				String errMsg = "merged " + Constants.USER_GROUPS_FILE_NAME + " file is not a legal json: " + e.getMessage();
				error = error + "\n" + errMsg;
				logger.severe(errMsg);
				logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);			
				throw new IOException(errMsg);
			}
		}
		
		return new Pair<String, LinkedList<AirlockChangeContent>>(error, changesArr);
	}
	
	//return error string upon error. null upon success
	public static Pair<String, LinkedList<AirlockChangeContent>> writeAirlockCapabilities(JSONObject capabilities, ServletContext context) throws IOException {
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		String error = null;
		
		try {			
			ds.writeData(Constants.AIRLOCK_CAPABILITIES_FILE_NAME, capabilities.write(true));
			changesArr.add(AirlockChangeContent.getAdminChange(capabilities, Constants.AIRLOCK_CAPABILITIES_FILE_NAME, Stage.PRODUCTION));
		} catch (IOException ioe) {
			//failed writing 3 times to s3. reload from s3 and return error			
			try {
				error = "Failed writing capabilities to s3. Discarding change by reloading previous data from s3";
				logger.info(error);
				JSONObject capabilitiesJSON  = ds.readDataToJSON(Constants.AIRLOCK_CAPABILITIES_FILE_NAME);
				Utilities.initFromCapabilitiesJSON(capabilitiesJSON, context);
			} catch (IOException e) {
				String errMsg = String.format(Strings.failedReadingFile,Constants.AIRLOCK_CAPABILITIES_FILE_NAME) + e.getMessage();
				error = error + "\n" + errMsg;
				logger.severe(errMsg);	
				logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
				throw new IOException(errMsg);
			} catch (JSONException e) {
				String errMsg = Constants.AIRLOCK_CAPABILITIES_FILE_NAME + " file is not a legal json: " + e.getMessage();
				error = error + "\n" + errMsg;
				logger.severe(errMsg);		
				logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
				throw new IOException(errMsg);
			}	
		} catch (JSONException e) {
			String errMsg = Constants.AIRLOCK_CAPABILITIES_FILE_NAME + " file is not a legal json: " + e.getMessage();
			error = error + "\n" + errMsg;
			logger.severe(errMsg);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);			
			throw new IOException(errMsg);
		}	

		return new Pair<String, LinkedList<AirlockChangeContent>>(error, changesArr);
	}
		
	//return error string upon error. null upon success
	public static LinkedList<AirlockChangeContent> writeAirlockApiKeys(ServletContext context) throws IOException {
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		AirlockAPIKeys apiKeys = (AirlockAPIKeys)context.getAttribute(Constants.API_KEYS_PARAM_NAME);		
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);

		JSONObject apiKeysJSON;
		JSONObject apiKeysPasswordsJSON;
		
		String error;
		
		try {
			apiKeysJSON = apiKeys.toJSON(APIKeyOutputMode.WITHOUT_PASSWORD);
			apiKeysPasswordsJSON = apiKeys.toJSON(APIKeyOutputMode.ONLY_PASSWORD);
		} catch (JSONException je) {
			//should never happen - creating JSON object from memory should always succeed but just in-case ...
			error = "Failed writing the Airlock api keys to S3 due to illegal JSON format: " +  je.getMessage() + "\n" + Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.";
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			logger.severe(error);			
			throw new IOException(error);
		}				
		
		try {			
			ds.writeData(Constants.AIRLOCK_API_KEYS_FILE_NAME, apiKeysJSON.write(true));
			changesArr.add(AirlockChangeContent.getAdminChange(apiKeysJSON, Constants.AIRLOCK_API_KEYS_FILE_NAME, Stage.PRODUCTION));
			/*if (ds instanceof S3DataSerializer) { //only s3 serialization supports encryption	
				((S3DataSerializer)ds).putEncryptedFile(apiKeysPasswordsJSON.write(true), Constants.AIRLOCK_API_KEYS_PASSWORDS_FILE_NAME);
			}
			else {*/
				ds.writeData(Constants.AIRLOCK_API_KEYS_PASSWORDS_FILE_NAME, apiKeysPasswordsJSON.write(true));
			//}
		} catch (JSONException je) {
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = "Failed writing Airlock api keys to S3: " + je.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			throw new IOException(error);			
		} catch (Exception ioe) {
			//failed writing 3 times to s3. Set server state to ERROR.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = "Failed writing Airlock api keys to S3: " + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			throw new IOException(error);			
		}
		return changesArr;
	}
	
	//return error string upon error. null upon success
	public static LinkedList<AirlockChangeContent> writeWebhooks(ServletContext context) throws IOException {
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		Webhooks webhooks = (Webhooks)context.getAttribute(Constants.WEBHOOKS_PARAM_NAME);		
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);

		JSONObject webhooksJSON;

		String error;

		try {
			webhooksJSON = webhooks.toJSON();
		} catch (JSONException je) {
			//should never happen - creating JSON object from memory should always succeed but just in-case ...
			error = "Failed writing the Airlock api keys to S3 due to illegal JSON format: " +  je.getMessage() + "\n" + Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.";
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			logger.severe(error);			
			throw new IOException(error);
		}				

		try {			
			ds.writeData(Constants.WEBHOOKS_FILE_NAME, webhooksJSON.write(true));
			changesArr.add(AirlockChangeContent.getAdminChange(webhooksJSON, Constants.WEBHOOKS_FILE_NAME, Stage.PRODUCTION));
		} catch (JSONException je) {
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = "Failed writing webhooks to S3: " + je.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			throw new IOException(error);			
		} catch (Exception ioe) {
			//failed writing 3 times to s3. Set server state to ERROR.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = "Failed writing webhooks to S3: " + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			throw new IOException(error);			
		}	
		return changesArr;
	}
		
	public static LinkedList<AirlockChangeContent> writeSeasonNotifications (Season season, boolean writeProduction, ServletContext context, boolean onlyAdmin) throws IOException {
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		JSONObject notificationsOutput = new JSONObject();
		String error = null;
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		String separator = ds.getSeparator();
		String notificationsFileName = Constants.SEASONS_FOLDER_NAME+separator+season.getProductId().toString()+separator+season.getUniqueId().toString()+separator+Constants.AIRLOCK_NOTIFICATIONS_FILE_NAME;

		try {
			notificationsOutput = season.getNotifications().toJson(OutputJSONMode.ADMIN);			
		} catch (JSONException je) {
			//should never happen - creating JSON object from memory should always succeed but just in-case ...
			//reload from s3 and return error
			logger.severe("Failed writing the season's notifications to S3. Illegal JSON format: " + je.getMessage());
			try {
				error = "Failed writing the season's notifications to S3 due to illegal JSON format. Reloading previous data from S3.";
				logger.info(error);
				JSONObject notificationsJSON  = ds.readDataToJSON(notificationsFileName);
				Utilities.initFromSeasonNotificationsJSON(notificationsJSON, context);		
			} catch (IOException e) {
				String errMsg = String.format(Strings.failedReadingFile, notificationsFileName) + e.getMessage();
				error = error + "\n" + errMsg;
				logger.severe(errMsg);
				logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
				throw new IOException(errMsg);
			} catch (JSONException e) {
				String errMsg = notificationsFileName + " file is not in a legal JSON format: " + e.getMessage();
				error = error + "\n" + errMsg;
				logger.severe(errMsg);			
				logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
				throw new IOException(errMsg);
			}
			//roll back succeeded. Throwing an exception to inform the user but the server state is still RUNNING. (not a S3 problem) 
			logger.severe(error);			
			throw new IOException(error);
		}				

		//validate the data consistency of the object that is going to be written to s3
		try { 
			AirlockNotificationsCollection tmp = new AirlockNotificationsCollection(season.getUniqueId(), context);
			tmp.fromJSON(notificationsOutput, null);
		} catch (JSONException e) {
			String errMsg = "Failed writing the notifications to S3. Illegal JSON format during data validation: " + e.getMessage();
			error = error + "\n" + errMsg;
			logger.severe(errMsg);			
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			throw new IOException(errMsg);
		}	
		
		try {			
			ds.writeData(notificationsFileName, notificationsOutput.write(true));
			changesArr.add(AirlockChangeContent.getAdminChange(notificationsOutput, notificationsFileName, Stage.PRODUCTION));
		} catch (IOException ioe) {
			//failed writing 3 times to s3. Set server state to ERROR.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = "Failed writing notifications to S3: " + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			throw new IOException(error);			
		}
		catch (JSONException je) {
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = "Failed writing notifications to S3: " + je.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			throw new IOException(error);			
		}
		
		if (onlyAdmin)
			return changesArr;
		
		//write development (dev+prod) notifications 
		try {	
			String devNotificationsFileName = Constants.SEASONS_FOLDER_NAME+separator+season.getProductId().toString()+separator+season.getUniqueId().toString()+separator+Constants.AIRLOCK_NOTIFICATIONS_DEVELOPMENT_FILE_NAME;		
						
			notificationsOutput = season.getNotifications().toJson(OutputJSONMode.RUNTIME_DEVELOPMENT);
			//ds.writeData(devNotificationsFileName, notificationsOutput.write(true), true);
			writeData(context, ds, devNotificationsFileName, notificationsOutput.write(true), season, true);
			changesArr.add(AirlockChangeContent.getRuntimeChange(notificationsOutput, devNotificationsFileName, Stage.DEVELOPMENT));
		} catch (IOException ioe) {
			//failed writing 3 times to s3. Set server state to ERROR.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = "Failed writing runtime development notifications to S3: " + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			throw new IOException(error);			
		}
		catch (JSONException je) {
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = "Failed writing runtime development notifications to S3: " + je.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			throw new IOException(error);			
		}
		
		if (writeProduction) {
			//write production utilities 
			try {
				String prodNotificationsFileName = Constants.SEASONS_FOLDER_NAME+separator+season.getProductId().toString()+separator+season.getUniqueId().toString()+separator+Constants.AIRLOCK_NOTIFICATIONS_PRODUCTION_FILE_NAME;		
				notificationsOutput = season.getNotifications().toJson(OutputJSONMode.RUNTIME_PRODUCTION);
				
				changesArr.addAll(Utilities.touchProductionChangedFile(context, ds, season));
				//ds.writeData(prodNotificationsFileName, notificationsOutput.write(true), true);
				writeData(context, ds, prodNotificationsFileName, notificationsOutput.write(true), season, true);
				changesArr.add(AirlockChangeContent.getRuntimeChange(notificationsOutput, prodNotificationsFileName, Stage.PRODUCTION));
			} catch (IOException ioe) {
				//failed writing 3 times to s3. Set server state to ERROR.
				//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
				error = "Failed writing runtime production notifications to S3: " + ioe.getMessage();
				logger.severe(error);
				logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
				throw new IOException(error);			
			} catch (JSONException je) {
				//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
				error = "Failed writing runtime production notifications to S3: " + je.getMessage();
				logger.severe(error);
				logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
				throw new IOException(error);			
			}	
		}
		return changesArr;
	}
/*
	public static void writeProductUsers(Product product, AirlockUsers productUsers, ServletContext context) throws IOException {
		String error = null;
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		String separator = ds.getSeparator();
		String productExperimentsFileName = Constants.SEASONS_FOLDER_NAME + 
				separator + product.getUniqueId().toString() + separator
				+Constants.AIRLOCK_USERS_FILE_NAME;							

		JSONObject airlockUsersJson = null;
		try {
			airlockUsersJson = productUsers.toJson();
		} catch (JSONException je) {
			//should never happen - creating JSON object from memory should always succeed
			error = "Failed writing product " +  product.getName() + ", " + product.getUniqueId().toString() + " users  to S3. Illegal JSON format: " + je.getMessage();
			logger.severe(error);			
			throw new IOException(error);
		}				

		try {			
			ds.writeData(productExperimentsFileName, airlockUsersJson.write(true));
		} catch (IOException ioe) {
			//failed writing 3 times to s3. Set server state to ERROR.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = "Failed writing product " +  product.getName() + ", " + product.getUniqueId().toString() + " users to S3: " + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			throw new IOException(error);			
		}
		catch (JSONException je) {
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = "Failed writing product " +  product.getName() + ", " + product.getUniqueId().toString() + " users to S3: " + je.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			throw new IOException(error);			
		}						
	}
	*/
	public static LinkedList<AirlockChangeContent> writeGlobalAirlockUsers(JSONObject airlockUsers, ServletContext context) throws IOException {			
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		String globalAirlocUsersFilePath = Constants.OPERATIONS_FOLDER_NAME + Constants.AIRLOCK_USERS_FILE_NAME; 
		
		try {			
			ds.writeData(globalAirlocUsersFilePath, airlockUsers.write(true));
			changesArr.add(AirlockChangeContent.getAdminChange(airlockUsers, globalAirlocUsersFilePath, Stage.PRODUCTION));
		} catch (IOException ioe) {
			//failed writing 3 times to s3.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);			
			String error = Strings.failedWritingUsers + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");			
			throw new IOException(error);			
		}
		catch (JSONException je) {
			//failed writing 3 times to s3.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);			
			String error = Strings.failedWritingUsers + je.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");			
			throw new IOException(error);			
		}
		return changesArr;
	}
	
	//return error string upon error. null upon success
	public static LinkedList<AirlockChangeContent> writeAirlockServers(JSONObject alServers, ServletContext context) throws IOException {
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		
		try {			
			ds.writeData(Constants.AIRLOCK_SERVERS_FILE_NAME, alServers.write(true));
			changesArr.add(AirlockChangeContent.getAdminChange(alServers, Constants.AIRLOCK_SERVERS_FILE_NAME, Stage.PRODUCTION));
		} catch (IOException ioe) {
			//failed writing roles 3 times to s3.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);			
			String error = Strings.failedWritingServer + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");			
			throw new IOException(error);
		}
		catch (JSONException je) {
			//failed writing roles 3 times to s3.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);			
			String error = Strings.failedWritingServer + je.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");			
			throw new IOException(error);
		}
		return changesArr;
	}	
	
	//return error string upon error. null upon success
	public static LinkedList<AirlockChangeContent> writeRoles(JSONObject roles, ServletContext context) throws IOException {			
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		try {			
			ds.writeData(Constants.ROLES_FILE_NAME, roles.write(true));
			changesArr.add(AirlockChangeContent.getAdminChange(roles, Constants.ROLES_FILE_NAME, Stage.PRODUCTION));
		} catch (IOException ioe) {
			//failed writing roles 3 times to s3.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);			
			String error = Strings.failedWritingRole + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");			
			throw new IOException(error);
		}
		catch (JSONException je) {
			//failed writing roles 3 times to s3.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);			
			String error = Strings.failedWritingRole + je.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");			
			throw new IOException(error);
		}
		return changesArr;
	}
	
	private static void addNewEncryptedFileToMap(String filePath, DataSerializer ds, HashMap<String, byte[]> newContentMap, Encryption prevKeyEncryptor, Encryption newKeyEncryptor) throws IOException, GeneralSecurityException {
		byte[] dataBytesPrevKey = ds.readDataToByteArray(filePath);
		byte[] decryptedData = null;
		if (prevKeyEncryptor != null) {
			decryptedData = prevKeyEncryptor.decrypt(dataBytesPrevKey);
		}
		else {
			//the prev runtime is not encrypted
			decryptedData = dataBytesPrevKey;
		}
		
		byte[] encryptedData = null;
		if (newKeyEncryptor!=null) {
			encryptedData = newKeyEncryptor.encrypt(decryptedData);
		}
		else {
			//the new runtime should not be encrypted
			encryptedData = decryptedData;
		}
	    newContentMap.put(filePath, encryptedData);
	}

	public static LinkedList<AirlockChangeContent> writeAllRuntimeFilesWithNewEncryption(Season season, ServletContext context, String prevKey, String newKey) throws IOException {
		//read all runtime files as byte arrays, decrypt them with old key and then encrypt them with new key.
		//keep all path and new content in map and only after all re-encrypted, write. try to implement transaction as much as possible
		
		HashMap<String, byte[]> newContentMap = new HashMap<String, byte[]>();
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		
		String separator = ds.getSeparator();
		
		String season_id = season.getUniqueId().toString();
		String product_id = season.getProductId().toString();

		String seasonFolder = Constants.SEASONS_FOLDER_NAME+separator+product_id+separator+season_id+separator;
		
		Encryption prevKeyEncryptor = null;
		if (prevKey != null) {
			byte[] prevKeyBytes = Encryption.fromString(prevKey);
			prevKeyEncryptor = new Encryption(prevKeyBytes);					
		}
		
		Encryption newKeyEncryptor = null;
		if (newKey != null) {
			byte[] newKeyBytes = Encryption.fromString(newKey);
			 newKeyEncryptor = new Encryption(newKeyBytes);
		}
		
		try {
			//dev features runtime 
			String devAirlockFeaturesPath = seasonFolder + Constants.AIRLOCK_RUNTIME_DEVELOPMENT_FILE_NAME;
			addNewEncryptedFileToMap(devAirlockFeaturesPath, ds, newContentMap, prevKeyEncryptor, newKeyEncryptor);
			
		    //prod features runtime 			
		    String prodAirlockFeaturesPath = seasonFolder + Constants.AIRLOCK_RUNTIME_PRODUCTION_FILE_NAME;
		    addNewEncryptedFileToMap(prodAirlockFeaturesPath, ds, newContentMap, prevKeyEncryptor, newKeyEncryptor);
			
		    //dev utilities runtime 			
		    String devAirlockUtilitiesPath = seasonFolder + Constants.AIRLOCK_UTILITIES_DEVELOPMENT_FILE_NAME;
		    addNewEncryptedFileToMap(devAirlockUtilitiesPath, ds, newContentMap, prevKeyEncryptor, newKeyEncryptor);
			
		    //prod utilities runtime 			
		    String prodAirlockUtilitiesPath = seasonFolder + Constants.AIRLOCK_UTILITIES_PRODUCTION_FILE_NAME;
		    addNewEncryptedFileToMap(prodAirlockUtilitiesPath, ds, newContentMap, prevKeyEncryptor, newKeyEncryptor);
			
		    //dev streams utilities runtime 			
		    String devAirlockStreamsUtilitiesPath = seasonFolder + Constants.AIRLOCK_STREAMS_UTILITIES_DEVELOPMENT_FILE_NAME;
		    addNewEncryptedFileToMap(devAirlockStreamsUtilitiesPath, ds, newContentMap, prevKeyEncryptor, newKeyEncryptor);
			
		    //prod streams utilities runtime 			
		    String prodAirlockStreamsUtilitiesPath = seasonFolder + Constants.AIRLOCK_STREAMS_UTILITIES_PRODUCTION_FILE_NAME;
		    addNewEncryptedFileToMap(prodAirlockStreamsUtilitiesPath, ds, newContentMap, prevKeyEncryptor, newKeyEncryptor);
			
		    //dev streams runtime 			
		    String devAirlockStreamsPath = seasonFolder + Constants.AIRLOCK_STREAMS_DEVELOPMENT_FILE_NAME;
		    addNewEncryptedFileToMap(devAirlockStreamsPath, ds, newContentMap, prevKeyEncryptor, newKeyEncryptor);
			
		    //prod streams runtime 			
		    String prodAirlockStreamsPath = seasonFolder + Constants.AIRLOCK_STREAMS_PRODUCTION_FILE_NAME;
		    addNewEncryptedFileToMap(prodAirlockStreamsPath, ds, newContentMap, prevKeyEncryptor, newKeyEncryptor);
			
		    //dev notifications runtime 			
		    String devAirlockNotificationsPath = seasonFolder + Constants.AIRLOCK_NOTIFICATIONS_DEVELOPMENT_FILE_NAME;
		    addNewEncryptedFileToMap(devAirlockNotificationsPath, ds, newContentMap, prevKeyEncryptor, newKeyEncryptor);
			
		    //prod notifications runtime 			
		    String prodAirlockNotificationsPath = seasonFolder + Constants.AIRLOCK_NOTIFICATIONS_PRODUCTION_FILE_NAME;
		    addNewEncryptedFileToMap(prodAirlockNotificationsPath, ds, newContentMap, prevKeyEncryptor, newKeyEncryptor);
			
		    //runtime branches
		    String airlockRuntimeBranchesPath = seasonFolder + Constants.AIRLOCK_RUNTIME_BRANCHES_FILE_NAME;
		    addNewEncryptedFileToMap(airlockRuntimeBranchesPath, ds, newContentMap, prevKeyEncryptor, newKeyEncryptor);
			
		    //runtime user groups
		    String airlockRuntimeUserGroupsPath = seasonFolder + Constants.USER_GROUPS_RUNTIME_FILE_NAME;
		    addNewEncryptedFileToMap(airlockRuntimeUserGroupsPath, ds, newContentMap, prevKeyEncryptor, newKeyEncryptor);
			
		    //runtime product
		    String airlockRuntimeProductPath = seasonFolder + Constants.PRODUCT_RUNTIME_FILE_NAME;
		    addNewEncryptedFileToMap(airlockRuntimeProductPath, ds, newContentMap, prevKeyEncryptor, newKeyEncryptor);
			
		    //branches
		    LinkedList<Branch> branches = season.getBranches().getBranchesList();
		    for (Branch branch:branches) {
		    		//dev
		    		String branchRuntimeDevFileName = seasonFolder+Constants.AIRLOCK_BRANCHES_FOLDER_NAME+ 
		    				separator + branch.getUniqueId().toString()+separator+Constants.AIRLOCK_RUNTIME_BRANCH_DEVELOPMENT_FILE_NAME;	
		    		addNewEncryptedFileToMap(branchRuntimeDevFileName, ds, newContentMap, prevKeyEncryptor, newKeyEncryptor);
				
			    //prod
		    		String branchRuntimeProdFileName = seasonFolder+Constants.AIRLOCK_BRANCHES_FOLDER_NAME+ 
		    				separator + branch.getUniqueId().toString()+separator+Constants.AIRLOCK_RUNTIME_BRANCH_PRODUCTION_FILE_NAME;	
		    		addNewEncryptedFileToMap(branchRuntimeProdFileName, ds, newContentMap, prevKeyEncryptor, newKeyEncryptor);
				
		    }
		
			
		    //translations files
		    LinkedList<String> supportedLangs = season.getOriginalStrings().getSupportedLanguages();
		    for (int i=0; i<supportedLangs.size(); i++) {
		    		String locale = supportedLangs.get(i);
		    		//dev
		    		String devLocaleStringsFilePath = seasonFolder+Constants.TRANSLATIONS_FOLDER_NAME + 
						separator + Constants.STRINGS_FILE_NAME_PREFIX + locale + Stage.DEVELOPMENT.toString() + Constants.STRINGS_FILE_NAME_EXTENSION;
		    		addNewEncryptedFileToMap(devLocaleStringsFilePath, ds, newContentMap, prevKeyEncryptor, newKeyEncryptor);
				
			    String prodLocaleStringsFilePath = seasonFolder+Constants.TRANSLATIONS_FOLDER_NAME + 
						separator + Constants.STRINGS_FILE_NAME_PREFIX + locale + Stage.PRODUCTION.toString() + Constants.STRINGS_FILE_NAME_EXTENSION;
			    addNewEncryptedFileToMap(prodLocaleStringsFilePath, ds, newContentMap, prevKeyEncryptor, newKeyEncryptor);
				
		    }
			
		   		    
		}
		catch (GeneralSecurityException gse) {
			throw new IOException("GeneralSecurityException: " + gse.getMessage()); 
		}
		
		//write the newly encrypted runtime content.
		Set<String> runtimeFilePathes = newContentMap.keySet();
		for (String runtimeFilePath : runtimeFilePathes) {
			byte[] newData = newContentMap.get(runtimeFilePath);  			
			ds.writeInputStreamData(runtimeFilePath, new ByteArrayInputStream(newData), newData.length);
		}
		
		return Utilities.touchProductionChangedFile(context, ds, season);
	}

	public static Pair<String, LinkedList<AirlockChangeContent>> writeUserGroupsRuntimeForSeason(ServletContext context, Product product, Season season) throws IOException {
		@SuppressWarnings("unchecked")
		Map<String,InternalUserGroups> groupsPerProductMap = (Map<String,InternalUserGroups>) context.getAttribute(Constants.USER_GROUPS_PER_PRODUCT_PARAM_NAME);
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		String error = null;
		String seperator = ds.getSeparator();
		String productId = product.getUniqueId().toString();
		InternalUserGroups internalUserGroups = groupsPerProductMap.get(product.getUniqueId().toString());

		JSONObject userGroupsJson = null;
		try {
			userGroupsJson = internalUserGroups.toJson();
		} catch (JSONException je) {
			//should never happen - creating JSON object from memory should always succeed 
			error = "Failed writing product " +  product.getName() + ", " + productId + " user groups  to S3. Illegal JSON format: " + je.getMessage();
			logger.severe(error);			
			throw new IOException(error);
		}
		
		String seasonId = season.getUniqueId().toString();
		
		String seasonUserGroupsRuntimePath = Constants.SEASONS_FOLDER_NAME + seperator + productId + seperator + 
				seasonId + seperator + Constants.USER_GROUPS_RUNTIME_FILE_NAME;
		
		try {
			writeData(context, ds, seasonUserGroupsRuntimePath, userGroupsJson.write(true), season, true);
			changesArr.add(AirlockChangeContent.getRuntimeChange(userGroupsJson, seasonUserGroupsRuntimePath, Stage.DEVELOPMENT));
		} catch (JSONException e) {
			String errMsg = Constants.USER_GROUPS_RUNTIME_FILE_NAME + " file is not a legal json: " + e.getMessage();
			error = error + "\n" + errMsg;
			logger.severe(errMsg);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);			
			throw new IOException(errMsg);
		}
		
		return new Pair<String, LinkedList<AirlockChangeContent>>(error, changesArr);
	}



	public static Set<String> getRuntimeFileNames() {
		Set<String> runtimeFileNames = new HashSet<String>();
		runtimeFileNames.add(Constants.AIRLOCK_RUNTIME_BRANCH_DEVELOPMENT_FILE_NAME);
		runtimeFileNames.add(Constants.AIRLOCK_RUNTIME_BRANCH_PRODUCTION_FILE_NAME);
		runtimeFileNames.add(Constants.AIRLOCK_RUNTIME_PRODUCTION_FILE_NAME);
		runtimeFileNames.add(Constants.AIRLOCK_RUNTIME_DEVELOPMENT_FILE_NAME);
		runtimeFileNames.add(Constants.AIRLOCK_UTILITIES_DEVELOPMENT_FILE_NAME);
		runtimeFileNames.add(Constants.AIRLOCK_UTILITIES_PRODUCTION_FILE_NAME);
		runtimeFileNames.add(Constants.AIRLOCK_STREAMS_UTILITIES_DEVELOPMENT_FILE_NAME);
		runtimeFileNames.add(Constants.AIRLOCK_STREAMS_UTILITIES_PRODUCTION_FILE_NAME);
		runtimeFileNames.add(Constants.AIRLOCK_RUNTIME_BRANCHES_FILE_NAME);
		runtimeFileNames.add(Constants.PRODUCT_RUNTIME_FILE_NAME);
		runtimeFileNames.add(Constants.USER_GROUPS_RUNTIME_FILE_NAME);
		runtimeFileNames.add(Constants.STRINGS_FILE_NAME_PREFIX); //all translations files
		runtimeFileNames.add(Constants.AIRLOCK_NOTIFICATIONS_DEVELOPMENT_FILE_NAME);
		runtimeFileNames.add(Constants.AIRLOCK_NOTIFICATIONS_PRODUCTION_FILE_NAME);
		runtimeFileNames.add(Constants.AIRLOCK_STREAMS_DEVELOPMENT_FILE_NAME);
		runtimeFileNames.add(Constants.AIRLOCK_STREAMS_PRODUCTION_FILE_NAME);	
		runtimeFileNames.add(Constants.PRODUCTION_CHANGED_FILE_NAME);
		
		return runtimeFileNames;
	}

	public static LinkedList<AirlockChangeContent> writeProductUsers(Product product, ServletContext context) throws IOException {
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		JSONObject usersOutput = new JSONObject();
		String error = null;
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		String separator = ds.getSeparator();
		String productUsersFileName = Constants.SEASONS_FOLDER_NAME+separator+product.getUniqueId().toString()+separator+Constants.AIRLOCK_USERS_FILE_NAME;

		try {
			usersOutput = product.getProductUsers().toJSON();			
		} catch (JSONException je) {
			//should never happen - creating JSON object from memory should always succeed but just in-case ...
			//reload from s3 and return error
			logger.severe("Failed writing the product's users to S3. Illegal JSON format: " + je.getMessage());
			try {
				error = "Failed writing the product's users to S3 due to illegal JSON format. Reloading previous data from S3.";
				logger.info(error);
				JSONObject usersJSON  = ds.readDataToJSON(productUsersFileName);
				Utilities.initFromSeasonNotificationsJSON(usersJSON, context);		
			} catch (IOException e) {
				String errMsg = String.format(Strings.failedReadingFile, productUsersFileName) + e.getMessage();
				error = error + "\n" + errMsg;
				logger.severe(errMsg);
				logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
				throw new IOException(errMsg);
			} catch (JSONException e) {
				String errMsg = productUsersFileName + " file is not in a legal JSON format: " + e.getMessage();
				error = error + "\n" + errMsg;
				logger.severe(errMsg);			
				logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
				throw new IOException(errMsg);
			}
			//roll back succeeded. Throwing an exception to inform the user but the server state is still RUNNING. (not a S3 problem) 
			logger.severe(error);			
			throw new IOException(error);
		}				

		//validate the data consistency of the object that is going to be written to s3
		try { 
			UserRoleSets tmp = new UserRoleSets();
			tmp.fromJSON(usersOutput, null);
		} catch (JSONException e) {
			String errMsg = "Failed writing the notifications to S3. Illegal JSON format during data validation: " + e.getMessage();
			error = error + "\n" + errMsg;
			logger.severe(errMsg);			
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			throw new IOException(errMsg);
		}	
		
		try {			
			ds.writeData(productUsersFileName, usersOutput.write(true));
			changesArr.add(AirlockChangeContent.getAdminChange(usersOutput, productUsersFileName, Stage.PRODUCTION));
		} catch (IOException ioe) {
			//failed writing 3 times to s3. Set server state to ERROR.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = "Failed writing notifications to S3: " + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			throw new IOException(error);			
		}
		catch (JSONException je) {
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = "Failed writing notifications to S3: " + je.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			throw new IOException(error);			
		}
		
		return changesArr;
		
	}

	//return error string upon error. null upon success
	public static Pair<String,LinkedList<AirlockChangeContent>> writePurchases (Season season, boolean writeProduction, ServletContext context, boolean onlyPurchases, Environment env) throws IOException {
		String error = null;
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		Stage stage = writeProduction? Stage.PRODUCTION : Stage.DEVELOPMENT;
		try {
			if (!onlyPurchases) {
				//runtime version - includes purchases section
				changesArr.addAll(doWriteFeatures (season, OutputJSONMode.RUNTIME_DEVELOPMENT, context, stage, env));
	
				//defaults version - includes purchases section
				changesArr.addAll(doWriteDefaultsFile(season, context, stage));
	
				//airlock constants java and swift file - includes purchases section
				changesArr.addAll(doWriteConstantsFiles(season, context, stage));
				
				if (writeProduction) //includes purchases section
					changesArr.addAll(doWriteFeatures (season, OutputJSONMode.RUNTIME_PRODUCTION, context, stage, env));
			}
			//admin version - separate admin file for purchases
			changesArr.addAll(doWritePurchases (season, context, stage)); //this is the last one to be updated so if writing to other file will fail we will be able to revert
		} catch  (JSONException je) {
			//should never happen - creating JSON object from memory should always succeed but just in-case ...
			//reload from s3 and return error
			DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
			String separator = ds.getSeparator();
			String seasonFilePath = Constants.SEASONS_FOLDER_NAME + separator + season.getProductId().toString() + separator + season.getUniqueId().toString();
			logger.severe("Failed writing the features to S3. Illegal JSON format: " + je.getMessage());
			try {
				error = "Failed building the feature's JSON object. Reloading previous data from S3.";
				logger.info(error);				
				JSONObject seasonJSON  = ds.readDataToJSON(seasonFilePath);
				
				Utilities.initFromSeasonFeaturesJSON(seasonJSON, (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME), 
						(Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME), ds, true, Constants.REQUEST_ITEM_TYPE.ENTITLEMENTS);
			} catch (IOException e) {
				String errMsg = String.format(Strings.failedReadingFile,seasonFilePath) + e.getMessage();
				error = error + "\n" + errMsg;
				logger.severe(errMsg);	
				logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
				throw new IOException(errMsg);
			} catch (JSONException e) {
				String errMsg = seasonFilePath + " file is not in a legal JSON format: " + e.getMessage();
				error = error + "\n" + errMsg;
				logger.severe(errMsg);
				logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
				throw new IOException(errMsg);
			}	
			//roll back succeeded. Throwing an exception to inform the user but the server state is still RUNNING. (not a S3 problem) 
			logger.severe(error);			
			throw new IOException(error);

		} catch (IOException ioe) {
			//failed writing 3 times to s3. Set server state to ERROR.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = "Failed writing the features to S3: " + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");			
			throw new IOException(error);		
		}

		return new Pair<>(error,changesArr);
	}
	
	//Only admin mode since runtime/default/constants are written to the exisiting files
	public static LinkedList<AirlockChangeContent> doWritePurchases(Season season, ServletContext context, Stage stage) throws JSONException, IOException{
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		String separator = ds.getSeparator();
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		
		String season_id = season.getUniqueId().toString();
		String product_id = season.getProductId().toString();

		JSONObject res = new JSONObject();
		
		res.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, season_id);
		res.put(Constants.JSON_FIELD_PRODUCT_ID, product_id); 

		Environment env = new Environment();
		env.setServerVersion(season.getServerVersion());
		env.setAnalytics(season.getAnalytics());
		env.setBranchId(Constants.MASTER_BRANCH_NAME);

		res.put(Constants.JSON_FIELD_ROOT, season.getEntitlementsRoot().toJson(OutputJSONMode.ADMIN, context, env));

		String fileName = "";
		boolean publish = false;

		fileName = Constants.SEASONS_FOLDER_NAME+separator+product_id+separator+season_id+separator+Constants.AIRLOCK_ENTITLEMENTS_FILE_NAME;
	
		//validate the data consistency of the object that is going to be written to s3
		try { 
			RootItem root = new RootItem();										
			root.fromJSON(res, null, null, env);				
		} catch (JSONException e) {
			String errMsg = "Failed writing the purchases to S3. Illegal JSON format during data validation: " + e.getMessage();
			logger.severe(errMsg);			
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			throw new IOException(errMsg);
		}

		changesArr.add(AirlockChangeContent.getAdminChange(res, fileName, stage));
	
		writeData(context, ds, fileName, res.write(true), season, publish);
		return changesArr;
	}
	
	
	public static LinkedList<AirlockChangeContent> writeBranchPurchases (Branch branch, Season season, ServletContext context, Environment env, Stage stage) throws IOException {
		String error = null;
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		String separator = ds.getSeparator();
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		JSONObject branchPurchasesAdminOutput = new JSONObject();		
		String branchAdminFileName = Constants.SEASONS_FOLDER_NAME+separator+season.getProductId().toString()+
				separator+season.getUniqueId().toString()+separator+Constants.AIRLOCK_BRANCHES_FOLDER_NAME+ 
				separator+branch.getUniqueId().toString()+separator+Constants.AIRLOCK_BRANCH_ENTITLEMENTS_FILE_NAME;							

		try {
			branchPurchasesAdminOutput = branch.toJson(OutputJSONMode.ADMIN, context, env, false, true, true); //return purchases dont return features
		} catch (JSONException je) {
			//should never happen - creating JSON object from memory should always succeed but just in-case ...
			//reload from s3 and return error
			logger.severe("Failed writing the branch " + branch.getName() + " to S3. Illegal JSON format: " + je.getMessage());			
			throw new IOException(error);
		}

		//validate the data consistency of the object that is going to be written to s3
		try { 
			Branch tmp = new Branch(season.getUniqueId());
			tmp.fromJSON(branchPurchasesAdminOutput, env, season, context);
		} catch (JSONException e) {
			String errMsg = "Failed writing the branch to S3. Illegal purchases JSON format during data validation: " + e.getMessage();
			error = error + "\n" + errMsg;
			logger.severe(errMsg);			
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			throw new IOException(errMsg);
		}	
		
		
		try {			
			ds.writeData(branchAdminFileName, branchPurchasesAdminOutput.write(true));
			changesArr.add(AirlockChangeContent.getAdminChange(branchPurchasesAdminOutput, branchAdminFileName, stage));
			return changesArr;
		} catch (IOException ioe) {
			//failed writing 3 times to s3. Set server state to ERROR.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = "Failed writing the branch purchases " + branch.getName() + " to S3: " + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			throw new IOException(error);			
		}
		catch (JSONException je) {
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = "Failed writing the branch purchases" + branch.getName() + " to S3: " + je.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			throw new IOException(error);			
		}				
	}

}
