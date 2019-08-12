package com.ibm.airlock;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.ibm.airlock.admin.serialize.DataSerializer;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.admin.BaseAirlockItem;
import com.ibm.airlock.admin.Season;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.ValidationResults;
import com.ibm.airlock.admin.authentication.UserInfo;
import com.ibm.airlock.admin.translations.BackgroundTranslator;



@Path ("/test")
//@Api(value = "/test", description = "test utilities API")
public class TestServices {

	public static final Logger logger = Logger.getLogger(TestServices.class.getName());


	@Context
	private ServletContext context;

	//TODO: this function is not valid any more since season has now 2 input folders - runtime and internal
	/*
	 * input is:
	    {
	 		"path": "/airlockdev/irit/SEASON_FROM_V2/seasons/d87caa6f-88d1-4a40-9f9b-164cc94232aa/f9432f71-8f26-4062-a85e-381ee0b36a9d",
	 		"productId": "d87caa6f-88d1-4a40-9f9b-164cc94232aa",
	 	    "productName": "testProd",
 	 		"seasonId": "f9432f71-8f26-4062-a85e-381ee0b36a9d",
 	        "minVersion":"0.1"
	 	}
	 */
	@POST
	@Path ("/import")	
	/*@ApiOperation(value = "import season to server", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	*/
	public Response importSeason(String inputParameters,						  
						 @HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {


		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("importSeason request: inputParameters =" + inputParameters);
		}

		UserInfo userInfo = UserInfo.validate("TestServices.importSeason", context, assertion, null);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);
		
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);

		ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock) context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
		readWriteLock.writeLock().lock();
		try {			
			ValidationResults vr = validateInputParameters(inputParameters, ds);
			if (vr!=null) {
				logger.severe(vr.error);
				return Response.status(vr.status).entity(Utilities.errorMsgToErrorJSON(vr.error)).build();
			}
			
			JSONObject inputParametersJSON = new JSONObject(inputParameters);	//will not fail - we are aftre validate
			String path = inputParametersJSON.getString(Constants.JSON_FIELD_PATH);
			String prodId = inputParametersJSON.getString(Constants.JSON_FIELD_PRODUCT_ID);
			String prodName = inputParametersJSON.getString(Constants.JSON_FIELD_PRODUCT_NAME);			
			String seasonId = inputParametersJSON.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID);
			String minVersion = inputParametersJSON.getString(Constants.JSON_SEASON_FIELD_MIN_VER);

			vr = createProduct(prodId, prodName, context, userInfo, assertion);
			if (vr!=null) {
				logger.severe(vr.error);
				return Response.status(vr.status).entity(Utilities.errorMsgToErrorJSON(vr.error)).build();
			}
			
			vr = createEmptySeason(prodId, seasonId, minVersion, context, userInfo);
			if (vr!=null) {
				logger.severe(vr.error);
				return Response.status(vr.status).entity(Utilities.errorMsgToErrorJSON(vr.error)).build();
			}
			
			vr = loadDataToSeason(path, context, seasonId);
			if (vr!=null) {
				logger.severe(vr.error);
				return Response.status(vr.status).entity(Utilities.errorMsgToErrorJSON(vr.error)).build();
			}
			
			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, seasonId);
			res.put(Constants.JSON_FIELD_PRODUCT_ID, prodId);
			res.put(Constants.JSON_FIELD_PRODUCT_NAME, prodName);
			
			return (Response.ok(res.toString())).build();
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}
	
	private ValidationResults loadDataToSeason(String path, ServletContext context, String seasonId) {
		@SuppressWarnings("unchecked")			
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);
		@SuppressWarnings("unchecked")
		Map<String, BaseAirlockItem> airlockItemsDB = (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		String separator = ds.getSeparator();
		try {
			Season season = seasonsDB.get(seasonId);
			
			String destSeasonFolder = Constants.SEASONS_FOLDER_NAME+separator+season.getProductId()+separator+seasonId;
			String srcSeasonFolder = path;
			
			ds.deleteFolderContent(destSeasonFolder);
			ds.fullPathCopyFolder(srcSeasonFolder, destSeasonFolder);
			
			//features
			String featuresPath = destSeasonFolder + separator + Constants.AIRLOCK_FEATURES_FILE_NAME;
			JSONObject featuresJSON = ds.readDataToJSON(featuresPath);			
			Utilities.initFromSeasonFeaturesJSON(featuresJSON, seasonsDB, airlockItemsDB, ds, true, Constants.REQUEST_ITEM_TYPE.FEATURES);
			
			//strings			
			String originalStringsPath = destSeasonFolder + separator + Constants.TRANSLATIONS_FOLDER_NAME + separator + Constants.ORIGINAL_STRINGS_FILE_NAME;
			JSONObject stringsJSON = ds.readDataToJSON(originalStringsPath);			
			Utilities.initFromSeasonOriginalStringsJSON(stringsJSON, context);

			// streams
			JSONObject streamsJSON = null;
			String streamsPath = destSeasonFolder + separator + Constants.AIRLOCK_STREAMS_FILE_NAME;
			if (ds.isFileExists(streamsPath)) {
				streamsJSON = ds.readDataToJSON(streamsPath);			
			}
			//inputSchema
			String inputSchemaPath = destSeasonFolder + separator + Constants.AIRLOCK_INPUT_SCHEMA_FILE_NAME;
			if (ds.isFileExists(inputSchemaPath)) {
				JSONObject schemaJSON = ds.readDataToJSON(inputSchemaPath);			
				Utilities.initFromSeasonInputschemaJSON(schemaJSON, streamsJSON, context);
			}
			
			//utilities
			String utilitiesPath = destSeasonFolder + separator + Constants.AIRLOCK_UTILITIES_FILE_NAME;
			if (ds.isFileExists(utilitiesPath)) {
				season.getUtilities().getUtilitiesList().clear();
				JSONObject utilsJSON = ds.readDataToJSON(utilitiesPath);			
				Utilities.initFromSeasonUtilitiesJSON(utilsJSON, context);			
			}
			
			//analytics
			String analyticsPath = destSeasonFolder + separator + Constants.AIRLOCK_ANALYTICS_FILE_NAME;
			if (ds.isFileExists(analyticsPath)) {				
				JSONObject analyticsJSON = ds.readDataToJSON(analyticsPath);			
				Utilities.initFromSeasonAnalyticsJSON(analyticsJSON, context);
			}						
									
			//followers
			String followersPath = destSeasonFolder + separator + Constants.FOLLOWERS_FEATURES_FILE_NAME;
			if (ds.isFileExists(followersPath)) {
				JSONObject followersJSON = ds.readDataToJSON(followersPath);			
				Utilities.initFeatureFollowersJSON(followersJSON, context, seasonsDB);
			}			
		} catch (JSONException je) {
			return new ValidationResults(je.getMessage(), Status.BAD_REQUEST);
		} catch (IOException ioe) {
			return new ValidationResults(ioe.getMessage(), Status.BAD_REQUEST);
		}
		return null;
	}

	private ValidationResults createEmptySeason(String prodId, String seasonId, String minVersion, ServletContext context, UserInfo userInfo) {
		try {
			JSONObject seasonJson = new JSONObject();
			
			seasonJson.put(Constants.JSON_SEASON_FIELD_MIN_VER, minVersion);
			
			ProductServices.doAddSeason(prodId, seasonJson.toString(), userInfo, UUID.fromString(seasonId), context);
		} catch( JSONException je) {
			return new ValidationResults(je.getMessage(), Status.BAD_REQUEST);
		}
		
		return null;
	}

	private ValidationResults createProduct(String prodId, String prodName, ServletContext context, UserInfo userInfo, String assertion) {
		try {
			JSONObject prodJson = new JSONObject();
			prodJson.put(Constants.JSON_FIELD_NAME, prodName);
			prodJson.put(Constants.JSON_PRODUCT_FIELD_CODE_IDENTIFIER, prodName);
			
			ProductServices.doAddProduct(prodJson.toString(), userInfo, UUID.fromString(prodId), context, assertion);
		} catch( JSONException je) {
			return new ValidationResults(je.getMessage(), Status.BAD_REQUEST);
		}
		
		return null;
	}

	private ValidationResults validateInputParameters(String inputParameters, DataSerializer ds) {
		String separator = ds.getSeparator();
		try {
			JSONObject inputParametersJSON = null;
			try {
				inputParametersJSON = new JSONObject(inputParameters);			
			} catch (JSONException je) {
				String errMsg = Strings.illegalInputJSON + je.getMessage();
				return new  ValidationResults(errMsg, Status.BAD_REQUEST);
			}
			
			//path
			if (!inputParametersJSON.containsKey(Constants.JSON_FIELD_PATH) || inputParametersJSON.get(Constants.JSON_FIELD_PATH) == null) {
				return new ValidationResults("The path field is missing.", Status.BAD_REQUEST);
			}
			
			//path
			if (!inputParametersJSON.containsKey(Constants.JSON_SEASON_FIELD_MIN_VER) || inputParametersJSON.get(Constants.JSON_SEASON_FIELD_MIN_VER) == null) {
				return new ValidationResults("The minVersion field is missing.", Status.BAD_REQUEST);
			}
			
			//validate path existence
			String pathStr = inputParametersJSON.getString(Constants.JSON_FIELD_PATH);
		
			//validate that the airlockFeatures.json file exists in the given forder
			String featuresPath = pathStr + separator + Constants.AIRLOCK_FEATURES_FILE_NAME;
			if (!ds.fullPathIsFileExists(featuresPath)) {
				String errMsg = Strings.pathMissingFile + featuresPath;
				return new  ValidationResults(errMsg, Status.BAD_REQUEST);
			}
				
			//productName
			if (!inputParametersJSON.containsKey(Constants.JSON_FIELD_PRODUCT_NAME) || inputParametersJSON.get(Constants.JSON_FIELD_PRODUCT_NAME) == null) {
				return new ValidationResults("The productName field is missing.", Status.BAD_REQUEST);
			}
			
			//productId
			if (!inputParametersJSON.containsKey(Constants.JSON_FIELD_PRODUCT_ID) || inputParametersJSON.get(Constants.JSON_FIELD_PRODUCT_ID) == null) {
				return new ValidationResults("The productId field is missing.", Status.BAD_REQUEST);
			}
			
			String productIdStr = inputParametersJSON.getString(Constants.JSON_FIELD_PRODUCT_ID);
			
			//verify that is legal GUID
			String err = Utilities.validateLegalUUID(productIdStr);
			if (err!=null) {
				String errMsg = Strings.illegalProductUUID + err;
				return new ValidationResults(errMsg, Status.BAD_REQUEST);			
			}
			
			//verify that the specified GUID is not in use
			err = Utilities.verifyGUIDIsNotInUse(context, productIdStr);
			if (err!=null) {
				return new ValidationResults(err, Status.BAD_REQUEST);				
			}
								
			//seasonId
			if (!inputParametersJSON.containsKey(Constants.JSON_FEATURE_FIELD_SEASON_ID) || inputParametersJSON.get(Constants.JSON_FEATURE_FIELD_SEASON_ID) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_SEASON_ID), Status.BAD_REQUEST);
			}
			
			String seasonIdStr = inputParametersJSON.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID);
			
			//verify that is legal GUID
			err = Utilities.validateLegalUUID(seasonIdStr);
			if (err!=null) {
				String errMsg = Strings.illegalSeasonUUID + err;
				return new ValidationResults(errMsg, Status.BAD_REQUEST);			
			}
			
			//verify that the specified GUID is not in use
			err = Utilities.verifyGUIDIsNotInUse(context, seasonIdStr);
			if (err!=null) {
				return new ValidationResults(err, Status.BAD_REQUEST);				
			}
			
			//verify that given season id is identical to teh season id in the airlockFeatures file
			JSONObject featuresJSON = ds.fullPathReadDataToJSON(featuresPath);
			String seasonId = featuresJSON.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID);
				
			if (!seasonId.equals(seasonIdStr)) {
				String errMsg = Strings.airlockFeatureSeasonWithDifferentId;
				return new ValidationResults(errMsg, Status.BAD_REQUEST);
			}
			
		} catch( JSONException je) {
			return new ValidationResults(je.getMessage(), Status.BAD_REQUEST);
		} catch( IOException ioe) {
			return new ValidationResults(ioe.getMessage(), Status.BAD_REQUEST);
		}
		
		return null;
	}
	@PUT
	@Path ("/TestMails")
	public Response setTestMails(@QueryParam("start")Boolean start,
								 @HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {

		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("setTestMails  "+"\n\n");
		}

		UserInfo userInfo = UserInfo.validate("TestServices.setTestMails", context, assertion, null);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		if(start == null || start == false) {
			context.setAttribute(Constants.IS_TEST_MODE,false);
		}
		else {
			context.setAttribute(Constants.IS_TEST_MODE,true);
		}
		JSONObject res = new JSONObject();
		res.put(Constants.IS_TEST_MODE, context.getAttribute(Constants.IS_TEST_MODE));
		return (Response.ok(res.toString())).build();
	}

	@GET
	@Path ("/TestMails")
	public Response getTestMails(@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		return testMailAction(false,assertion);
	}

	@DELETE
	@Path ("/TestMails")
	public Response deleteTestMails(@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		return testMailAction(true,assertion);
	}

	private Response testMailAction(Boolean delete,String assertion){
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("deleteTestMails  "+"\n\n");
		}

		UserInfo userInfo = UserInfo.validate("TestServices.setTestMails", context, assertion, null);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
		readWriteLock.writeLock().lock();
		try {
			DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
			String separator = ds.getSeparator();
			String emailFile = Constants.TESTS_PATH +separator+Constants.EMAILS_FILE_NAME;
			if (ds.isFileExists( emailFile)){
				JSONObject res = new JSONObject();
				if(delete) {
					ds.deleteData(emailFile);
				}
				else {
					res = ds.readDataToJSON(emailFile);
				}
				return (Response.ok(res.toString())).build();
			}
			else{
				String errMsg = Strings.nonExistingFile;
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();

			}
		}catch (Exception e){
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
		}
		finally {
			readWriteLock.writeLock().unlock();
		}
	}

	@PUT
	@Path ("/translations")
	public Response setTestTranslations(@QueryParam("trace")Boolean trace,@QueryParam("fine")Boolean fine,
						 @HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException
	{
		if (logger.isLoggable(Level.FINEST))
			logger.finest("setTestTranslations  "+"\n\n");

		UserInfo userInfo = UserInfo.validate("TestServices.setTestTranslations", context, assertion, null);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		BackgroundTranslator smartling = (BackgroundTranslator) context.getAttribute(Constants.BACKGROUND_TRANSLATOR);
		if (smartling == null)
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(Strings.translationNotRunning)).build();

		if (trace == null && fine == null)
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON("missing trace/fine parameters")).build();

		if (trace != null)
			smartling.setTrace(trace);
		if (fine != null)
			smartling.setFine(fine);

		return Response.ok().build();
	}
	@GET
	@Path ("/translations")
	public Response getTestTranslations(@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException
	{
		if (logger.isLoggable(Level.FINEST))
			logger.finest("getTestTranslations  "+"\n\n");

		UserInfo userInfo = UserInfo.validate("TestServices.getTestTranslations", context, assertion, null);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		BackgroundTranslator smartling = (BackgroundTranslator) context.getAttribute(Constants.BACKGROUND_TRANSLATOR);
		if (smartling == null)
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(Strings.translationNotRunning)).build();

		JSONArray trace = new JSONArray(smartling.getTrace());
		JSONObject res = new JSONObject();
		res.put("trace", trace);
		return (Response.ok(res.toString())).build();
	}
	@PUT
	@Path ("/translations/wakeup")
	public Response wakeupTranslations(@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException
	{
		if (logger.isLoggable(Level.FINEST))
			logger.finest("wakeupTranslations  "+"\n\n");

		UserInfo userInfo = UserInfo.validate("TestServices.wakeupTranslations", context, assertion, null);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		BackgroundTranslator smartling = (BackgroundTranslator) context.getAttribute(Constants.BACKGROUND_TRANSLATOR);
		if (smartling == null)
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(Strings.translationNotRunning)).build();

		smartling.wakeup();
		return Response.ok().build();
	}
	Response sendError(Status status, String err)
	{
		return Response.status(status).entity(Utilities.errorMsgToErrorJSON(err)).build();
	}
	Response sendInfoError(Status status, UserInfo info)
	{
		return Response.status(status).entity(info.getErrorJson()).build();
	}
}
