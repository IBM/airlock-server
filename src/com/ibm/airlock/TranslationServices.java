package com.ibm.airlock;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.ibm.airlock.admin.*;
import com.ibm.airlock.admin.Utilities.ProductErrorPair;
import com.ibm.airlock.admin.serialize.DataSerializer;
import com.ibm.airlock.admin.translations.*;

import org.apache.commons.io.FileUtils;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants.ActionType;
import com.ibm.airlock.Constants.AirlockCapability;
import com.ibm.airlock.Constants.IssueStatus;
import com.ibm.airlock.Constants.RoleType;
import com.ibm.airlock.Constants.Stage;
import com.ibm.airlock.Constants.StringStatus;
import com.ibm.airlock.Constants.StringsOutputMode;
import com.ibm.airlock.Constants.TranslationStatus;
import com.ibm.airlock.Constants.UtilityType;
import com.ibm.airlock.admin.authentication.UserInfo;
import com.ibm.airlock.admin.operations.AirlockChange;
import com.ibm.airlock.admin.operations.AirlockChangeContent;
import com.ibm.airlock.admin.operations.Webhooks;
import com.ibm.airlock.admin.serialize.AirlockFilesWriter;
import com.ibm.airlock.admin.serialize.AuditLogWriter;
import com.ibm.airlock.admin.translations.OriginalString.StringTranslations.LocaleTranslationData;
import com.ibm.airlock.admin.translations.SmartlingClient.IssueSubType;
import com.ibm.airlock.admin.translations.SmartlingClient.IssueType;
import com.ibm.airlock.engine.Environment;
import com.ibm.airlock.engine.VerifyRule;
import com.ibm.airlock.engine.Version;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import com.ibm.airlock.utilities.Pair;

@Path ("/translations")
@Api(value = "/Translations", description = "Translations management API")
public class TranslationServices {

	public static final Logger logger = Logger.getLogger(TranslationServices.class.getName());

	public static class ConflictingStrings{
		private OriginalString source;
		private OriginalString dest;
		private String key;
		private JSONArray conflictingFields;


		public ConflictingStrings(OriginalString _source, OriginalString _dest, String stringKey,JSONArray _conflictingFields){
			source= _source;
			dest = _dest;
			key = stringKey;
			conflictingFields = _conflictingFields;
		}

		public OriginalString getSource(){
			return source;
		}
		public OriginalString getDest(){
			return dest;
		}

		public JSONObject toJson() throws JSONException{
			JSONObject json = new JSONObject();
			json.put("source",source);
			json.put("dest",dest);
			json.put("key",key);
			return json;
		}

		public String getKey(){
			return key;
		}
		public JSONArray getConflictingFields(){
			return conflictingFields;
			/*StringBuilder fields = new StringBuilder();
			for(int i = 0; i< conflictingFields.size();++i){
				fields.append(conflictingFields.get(i));
				fields.append(", ");
			}
			String fieldsString = fields.toString();
			return fieldsString.substring(0,fieldsString.length()-2);*/
		}
	}

	@Context
	private ServletContext context;		

	@GET
	@Path ("/{season-id}/translate/{locale}")
	@ApiOperation(value = "Returns translated strings for the specified season and locale", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getTranslations(@PathParam("season-id")String season_id, 
			@PathParam("locale")String locale, 
			@ApiParam(value="DEVELOPMENT or PRODUCTION")@QueryParam("stage")String stage,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getTranslations request");
		}

		String err = Utilities.validateLegalUUID(season_id);
		if (err!=null)
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalSeasonUUID + err);
		
		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, season_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		
		//check user authorization
		UserInfo userInfo = UserInfo.validate("TranslationServices.getTranslations", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);
		
		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.TRANSLATIONS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		

		Stage stageObj = null;
		if (stage!=null && !stage.isEmpty()) {
			stageObj = Utilities.strToStage(stage);

			if (stageObj==null)
				return sendAndLogError(String.format(Strings.illegalStage,stage)  + Constants.Stage.returnValues());
		}
					
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();	
		readWriteLock.readLock().lock();
		try {
			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			//Validate season existence
			Season season = seasonsDB.get(season_id);

			if (season == null)
				return sendAndLogError(Strings.seasonNotFound);

			//validate that pre 2.1 season is upgraded			
			ValidationResults upgradedRes = validatePre21SeasonsIsUpgraded(season);
			if (upgradedRes!=null)
				return sendAndLogError(upgradedRes.status, upgradedRes.error);

			if (season.getOriginalStrings().isOldEnStringsFileExists()) {
				if (stage!=null)
					return sendAndLogError(Strings.stageNotNeeded);

			} else { //2.1 and higher server versions
				if (stage==null || stage.isEmpty()) {
					return sendAndLogError(Strings.stageMissing);
				}
			}

			//validate that locale is one of the supported languages
			boolean localeExists = season.getOriginalStrings().isLanguageSupported(locale);
			if (!localeExists) {
				return sendAndLogError(Strings.translationDoesNotExist);
			}		

			JSONObject res;
			try {
				res = TranslationUtilities.readLocaleStringsFile(locale, season, context, logger, stageObj);
			} catch (IOException e) {
				return sendAndLogError(Status.INTERNAL_SERVER_ERROR, e.getMessage());	
			}

			return (Response.ok(res.toString())).build();
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	@PUT
	@Path ("/{season-id}/translate/{locale}")
	@ApiOperation(value = "Updates translated strings for the specified season and locale.", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "locale not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response updateTranslations(@PathParam("season-id")String season_id, @PathParam("locale")String locale, String tarnslations,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("updateTranslations request: season_id = " + season_id + ", locale = " + locale + ", tarnslations = " + tarnslations);
		}
		AirlockChange change = new AirlockChange();
		String err = Utilities.validateLegalUUID(season_id);
		if (err!=null) {
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalSeasonUUID + err);
		}
		
		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, season_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);
		
		//check user authorization
		UserInfo userInfo = UserInfo.validate("TranslationServices.updateTranslations", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);		

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.TRANSLATIONS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();	
			
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.writeLock().lock();
		try {						
			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			//Validate season existence
			Season season = seasonsDB.get(season_id);
			change.setProduct(currentProduct);
			if (season == null) {
				return sendAndLogError(Strings.seasonNotFound);
			}

			//validate that pre 2.1 season is upgraded			
			ValidationResults upgradedRes = validatePre21SeasonsIsUpgraded(season);
			if (upgradedRes!=null)
				return Response.status(upgradedRes.status).entity(Utilities.errorMsgToErrorJSON(upgradedRes.error)).build();

			//validate that is a legal JSON
			JSONObject translationsJSON = null;
			try {
				translationsJSON = new JSONObject(tarnslations);
			} catch (JSONException je) {
				return sendAndLogError(Strings.illegalInputJSON + je.getMessage());
			}

			//validate that locale one of the supported languages
			boolean localeExists = season.getOriginalStrings().isLanguageSupported(locale);
			if (!localeExists) {
				return sendAndLogError(Strings.translationDoesNotExist);
			}

			if (locale.equals(Constants.DEFAULT_LANGUAGE)) {  //English files are not translated
				return sendAndLogError(Strings.englishNotNeeded);
			}

			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion()); 			
					
			TranslatedStrings translationStringsObj = new TranslatedStrings();
			ValidationResults validationRes = translationStringsObj.validateTranslatedStringsJSON(translationsJSON, season.getOriginalStrings(), env);
			if (validationRes!=null) {
				return sendAndLogError(validationRes.status, validationRes.error);
			}

			translationStringsObj.fromJSON(translationsJSON);


			boolean productionStringChanged = season.getOriginalStrings().addLocaleTranslations(translationStringsObj, locale);
			Stage changeStage = productionStringChanged? Stage.PRODUCTION : Stage.DEVELOPMENT;
			try {
				change.getFiles().addAll(AirlockFilesWriter.writeOriginalStrings(season, context, changeStage));
				change.getFiles().addAll(AirlockFilesWriter.writeLocaleStringsFiles(season, locale, context, productionStringChanged));
				Webhooks.get(context).notifyChanges(change, context);
			} catch (IOException e) {
				return sendAndLogError(Status.INTERNAL_SERVER_ERROR, e.getMessage());
			}

			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Translations updated for locale '" + locale + "': \n" + tarnslations, userInfo);

			return (Response.ok()).build();			
		} finally {
			readWriteLock.writeLock().unlock();
		}	
	}
	
	@POST
	@Path ("/{season-id}/translate/{locale}")
	@ApiOperation(value = "Adds translated strings for the specified season and locale.", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response addTranslations(@PathParam("season-id")String season_id, @PathParam("locale")String locale, String tarnslations,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("addTranslations request: season_id = " + season_id + ", locale = " + locale + ", tarnslations = " + tarnslations);
		}
		AirlockChange change = new AirlockChange();
		String err = Utilities.validateLegalUUID(season_id);
		if (err!=null) {
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalSeasonUUID + err);
		}
		
		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, season_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);
		
		//check user authorization
		UserInfo userInfo = UserInfo.validate("TranslationServices.addTranslations", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.TRANSLATIONS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		
		
		ReentrantReadWriteLock readWriteLock = productErrorPair.product.getProductLock();
		readWriteLock.writeLock().lock();
		try {						
			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			//Validate season existence
			Season season = seasonsDB.get(season_id);
			change.setSeason(season);
			
			if (season == null) {
				return sendAndLogError(Status.NOT_FOUND, Strings.seasonNotFound);				
			}

			//validate that pre 2.1 season is upgraded			
			ValidationResults upgradedRes = validatePre21SeasonsIsUpgraded(season);
			if (upgradedRes!=null)
				return Response.status(upgradedRes.status).entity(Utilities.errorMsgToErrorJSON(upgradedRes.error)).build();

			//validate that is a legal JSON
			JSONObject translationsJSON = null;
			try {
				translationsJSON = new JSONObject(tarnslations);
			} catch (JSONException je) {
				String errMsg = Strings.illegalInputJSON + je.getMessage();
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();				
			}

			//validate that locale is not one of the supported languages yet
			boolean localeExists = season.getOriginalStrings().isLanguageSupported(locale);			
			if (localeExists) {
				return sendAndLogError(Status.BAD_REQUEST, Strings.translationAlreadyExist);			
			}

			if (locale.equals(Constants.DEFAULT_LANGUAGE)) {  //English files are not translated
				return sendAndLogError(Status.BAD_REQUEST, Strings.englishNotNeeded);			
			}

			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion()); 			
		
			TranslatedStrings translationStringsObj = new TranslatedStrings();
			ValidationResults validationRes = translationStringsObj.validateTranslatedStringsJSON(translationsJSON, season.getOriginalStrings(), env);
			if (validationRes!=null) {
				return sendAndLogError(Status.BAD_REQUEST, validationRes.error);
			}

			translationStringsObj.fromJSON(translationsJSON);

			boolean productionChanged = season.getOriginalStrings().addLocaleTranslations(translationStringsObj, locale);

			season.getOriginalStrings().addSupportedLanguage(locale, null);
			Stage changeStage = productionChanged? Stage.PRODUCTION : Stage.DEVELOPMENT;
			try {
				change.getFiles().addAll(AirlockFilesWriter.writeOriginalStrings(season, context, changeStage));				
				change.getFiles().addAll(AirlockFilesWriter.writeLocaleStringsFiles(season, locale, context, true)); //always write both development and production for new locale

				//generate defaults file since supported languages list was updated
				change.getFiles().addAll(AirlockFilesWriter.doWriteDefaultsFile(season, context, changeStage));
				Webhooks.get(context).notifyChanges(change, context);
			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}


			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Translations added for locale '" + locale + "': \n" + tarnslations, userInfo);

			return (Response.ok()).build();
		} finally {
			readWriteLock.writeLock().unlock();
		}	
	}

	@POST
	@Path ("/seasons/{season-id}/strings")
	@ApiOperation(value = "Creates string within the specified season", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response addString(@PathParam("season-id")String season_id, String newString,
			@ApiParam(value="VALIDATE or ACT")@QueryParam("mode")String mode,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("addString request: season_id = " + season_id + ", newString = " + newString);
		}
		AirlockChange change = new AirlockChange();
		
		String err = Utilities.validateLegalUUID(season_id);
		if (err!=null) {
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalSeasonUUID + err);
		}
		
		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, season_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);
		
		//check user authorization
		UserInfo userInfo = UserInfo.validate("TranslationServices.addString", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.TRANSLATIONS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		
				
		boolean validation = "VALIDATE".equals(mode);
		Pair<Response, LinkedList<AirlockChangeContent>> writeRes = doAddString(context,season_id,newString,validation,false,userInfo);
		change.getFiles().addAll(writeRes.getValue());
		Response r = writeRes.getKey();
		if (validation || r.getStatus() != Response.Status.OK.getStatusCode()){
			return r;
		}
		else{
			List<String> newStringIds = new ArrayList<>();
			String newStringId = r.getEntity().toString();
			newStringIds.add(newStringId);
			Webhooks.get(context).notifyChanges(change, context);
			return writeAddedStrings(context,season_id,newStringIds,userInfo);
		}

	}

	public static Pair<Response, LinkedList<AirlockChangeContent>> doAddString(ServletContext context, String season_id,String newString, boolean validation,boolean bulk, UserInfo userInfo) throws JSONException {
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		String err = Utilities.validateLegalUUID(season_id);
		if (err != null) {
			String errMsg = Strings.illegalSeasonUUID + err;
			logger.severe(errMsg);
			return createResponseChangesPair(Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build(), changesArr);
		}

		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, season_id);
		if (productErrorPair.error != null) {
			return createResponseChangesPair(Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build(), changesArr);
		}
		ReentrantReadWriteLock readWriteLock = productErrorPair.product.getProductLock();
		readWriteLock.writeLock().lock();
		try {
			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>) context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			@SuppressWarnings("unchecked")
			Map<String, OriginalString> originalStringsDB = (Map<String, OriginalString>) context.getAttribute(Constants.ORIG_STRINGS_DB_PARAM_NAME);

			//Validate season existence
			Season season = seasonsDB.get(season_id);

			if (season == null) {
				String errMsg = Strings.seasonNotFound;
				logger.severe(errMsg);
				return createResponseChangesPair(Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build(), changesArr);
			}

			//validate that pre 2.1 season is upgraded
			ValidationResults upgradedRes = validatePre21SeasonsIsUpgraded(season);
			if (upgradedRes != null)
				return createResponseChangesPair(Response.status(upgradedRes.status).entity(Utilities.errorMsgToErrorJSON(upgradedRes.error)).build(), changesArr);

			//validate that is a legal JSON
			JSONObject newStringJSON = null;
			try {
				newStringJSON = new JSONObject(newString);
			} catch (JSONException je) {
				String errMsg = Strings.illegalInputJSON + je.getMessage();
				logger.severe(errMsg);
				return createResponseChangesPair(Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build(), changesArr);
			}

			//verify that JSON does not contain uniqueId field
			if (newStringJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && newStringJSON.get(Constants.JSON_FIELD_UNIQUE_ID) != null) {
				String errMsg = Strings.stringWithId;
				logger.severe(errMsg);
				return createResponseChangesPair(Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build(), changesArr);
			}


			//verify that JSON does not contain different season-id then the path parameter
			if (newStringJSON.containsKey(Constants.JSON_FEATURE_FIELD_SEASON_ID) && newStringJSON.get(Constants.JSON_FEATURE_FIELD_SEASON_ID) != null) {
				if (!season_id.equals(newStringJSON.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID))) {
					String errMsg = Strings.stringSeasonWithDifferentId;
					logger.severe(errMsg);
					return createResponseChangesPair(Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build(), changesArr);
				}
			} else {
				newStringJSON.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, season_id);
			}

			OriginalString newStringObj = new OriginalString();

			ValidationResults validationRes = newStringObj.validateOriginalStringJSON(newStringJSON, context, userInfo);
			if (validationRes != null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return createResponseChangesPair(Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build(), changesArr);
			}

			newStringObj.fromJSON(newStringJSON, season);
			newStringObj.setUniqueId(UUID.randomUUID());

			if (validation)
			{
				// return similar strings
				JSONObject json = season.getOriginalStrings().getStringXref().addSimilarToJson(newStringObj.getValue());
				return createResponseChangesPair((Response.ok(json.toString())).build(), changesArr);
			}

			season.getOriginalStrings().addOriginalString(newStringObj);
			if(!bulk){
				Pair<Response, LinkedList<AirlockChangeContent>> writeRes = writeStrings(season,context,newStringObj.getStage() == Stage.PRODUCTION,newStringObj.getInternationalFallback() != null);
				changesArr.addAll(writeRes.getValue());
				Response r = writeRes.getKey(); 
				if(r != null){
					return writeRes;
				}
			}
			originalStringsDB.put(newStringObj.getUniqueId().toString(), newStringObj);
			return createResponseChangesPair((Response.ok(newStringObj.getUniqueId().toString())).build(), changesArr);
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	public static Pair<Response, LinkedList<AirlockChangeContent>> writeStrings(Season season, ServletContext context,boolean writeProduction,boolean writelocales){
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		try {
			Stage changeStage = writeProduction? Stage.PRODUCTION : Stage.DEVELOPMENT;
			changesArr.addAll(AirlockFilesWriter.writeOriginalStrings(season, context, changeStage));
			changesArr.addAll(AirlockFilesWriter.writeEnStringsFiles(season, context, writeProduction));
			if (writelocales) {
				changesArr.addAll(AirlockFilesWriter.writeAllLocalesStringsFiles(season, context, writeProduction, false));
			}
		} catch (Exception e) {
			return createResponseChangesPair(Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build(), changesArr);
		}
		return createResponseChangesPair(null, changesArr);
	}
	
	private static Pair<Response, LinkedList<AirlockChangeContent>> createResponseChangesPair(Response response, LinkedList<AirlockChangeContent> changesArr) {
		Pair<Response, LinkedList<AirlockChangeContent>> toRet = new  Pair<Response, LinkedList<AirlockChangeContent>>(response, changesArr);
		return toRet;
	}
	public static Response writeAddedStrings(ServletContext context,String season_id, List<String> stringIdList,UserInfo userInfo)throws JSONException{
		ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock) context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
		readWriteLock.writeLock().lock();
		try{
			JSONObject res = new JSONObject();
			JSONArray resArray = new JSONArray();

			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>) context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			@SuppressWarnings("unchecked")
			Map<String, OriginalString> originalStringsDB = (Map<String, OriginalString>) context.getAttribute(Constants.ORIG_STRINGS_DB_PARAM_NAME);

			//Validate season existence
			Season season = seasonsDB.get(season_id);

			for(int i = 0;i<stringIdList.size();++i) {


				OriginalString newStringObj = originalStringsDB.get(stringIdList.get(i));
				AuditLogWriter auditLogWriter = (AuditLogWriter) context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
				auditLogWriter.log("Create new string: " + newStringObj.getKey() + ", " + newStringObj.getUniqueId() + ":" + newStringObj.toJson(StringsOutputMode.BASIC, season).toString(), userInfo);

				JSONObject newString = new JSONObject();
				newString.put(Constants.JSON_FIELD_UNIQUE_ID, newStringObj.getUniqueId().toString());
				newString.put(Constants.JSON_FIELD_KEY, newStringObj.getKey());
				newString.put(Constants.JSON_FIELD_LAST_MODIFIED, newStringObj.getLastModified().getTime());
				resArray.add(newString);

				logger.info("String added to season '" + season_id + "': " + res.toString());

				if (logger.isLoggable(Level.FINE)) {
					logger.fine("Added string: " + newStringObj.toJson(StringsOutputMode.BASIC, season));
				}
			}
			if (resArray.size() == 1) {
				res = resArray.getJSONObject(0);
			} else {
				res.put("newstrings", resArray);
			}
			return (Response.ok(res.toString())).build();
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	

	@GET
	@Path ("/seasons/{season-id}/strings")
	@ApiOperation(value = "Returns the strings for the specified season ", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getStrings(@PathParam("season-id")String season_id,
							   @QueryParam("ids")List<String> ids,
			@ApiParam(value="BASIC or INCLUDE_TRANSLATIONS")@QueryParam("mode")String outputMode,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getStrings request");
		}

		String err = Utilities.validateLegalUUID(season_id);
		if (err!=null) {
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalSeasonUUID + err);
		}
		
		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, season_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		
		//check user authorization
		UserInfo userInfo = UserInfo.validate("TranslationServices.getStrings", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		
		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.TRANSLATIONS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		
		
		if(ids == null){
			ids = new ArrayList<>();
		}
		for(int i = 0;i<ids.size();++i){
			String stringId = ids.get(i);
			err = Utilities.validateLegalUUID(stringId);
			if (err!=null) {
				return sendAndLogError(Status.BAD_REQUEST, Strings.illegalStringUUID + err);
			}
		}

		StringsOutputMode outputModeObj = StringsOutputMode.BASIC; //if not specified basic is the defaults
		if (outputMode!=null && !outputMode.isEmpty()) {
			outputModeObj = Utilities.strToStringsOutputMode(outputMode);

			if (outputModeObj==null) {
				return sendAndLogError(Status.BAD_REQUEST, String.format(Strings.illegalOutputMode,outputMode)  + Constants.StringsOutputMode.returnValues());
			}
		}

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.readLock().lock();
		try {

			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			Season season = seasonsDB.get(season_id);

			if (season == null) {
				return sendAndLogError(Status.NOT_FOUND, Strings.seasonNotFound);
			}

			//validate that pre 2.1 season is upgraded
			ValidationResults upgradedRes = validatePre21SeasonsIsUpgraded(season);
			if (upgradedRes!=null)
				return Response.status(upgradedRes.status).entity(Utilities.errorMsgToErrorJSON(upgradedRes.error)).build();

			JSONObject res = new JSONObject();
			if(ids.size() == 0) {
				res = season.getOriginalStrings().toJson(outputModeObj);
			}
			else {
				@SuppressWarnings("unchecked")
				Map<String, OriginalString> originalStringsDB = (Map<String, OriginalString>)context.getAttribute(Constants.ORIG_STRINGS_DB_PARAM_NAME);

				JSONArray stringsArray = new JSONArray();
				for (int i = 0; i<ids.size();++i) {
					OriginalString originalString = originalStringsDB.get(ids.get(i));
					String currSeasonId = originalString.getSeasonId().toString();

					if (!season_id.equals(currSeasonId)) {
						return sendAndLogError(Status.NOT_FOUND, Strings.stringNotInSeason+ ids.get(i));
					}
					stringsArray.add(originalString.toJson(outputModeObj,season));
				}
				res.put(Constants.JSON_FIELD_STRINGS,stringsArray);
			}
			return (Response.ok()).entity(res.toString()).build();
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	@GET
	@Path ("/seasons/{season-id}/stringstoformat")
	@Produces(value = "application/zip")
	@ApiOperation(value = "Returns the strings for the specified season in zip", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getStringsInFormat(@PathParam("season-id")String season_id,
							   @QueryParam("ids")List<String> ids,
							   @ApiParam(value="BASIC or INCLUDE_TRANSLATIONS")
							   @QueryParam("mode")String outputMode,
							   @ApiParam(value="IOS or ANDROID")							   
							   @QueryParam("format")String format,
							   @HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getStringsInFormat request");
		}
		
		String err = Utilities.validateLegalUUID(season_id);
		if (err!=null) {
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalSeasonUUID + err);
		}

		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, season_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		
		//check user authorization
		UserInfo userInfo = UserInfo.validate("TranslationServices.getStringsInFormat", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.TRANSLATIONS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		
				
		if(ids == null){
			ids = new ArrayList<>();
		}
		for(int i = 0;i<ids.size();++i){
			String stringId = ids.get(i);
			err = Utilities.validateLegalUUID(stringId);
			if (err!=null) {
				return sendAndLogError(Status.BAD_REQUEST, Strings.illegalStringUUID + err);
			}
		}

		StringsOutputMode outputModeObj = StringsOutputMode.BASIC; //if not specified basic is the defaults
		if (outputMode!=null && !outputMode.isEmpty()) {
			outputModeObj = Utilities.strToStringsOutputMode(outputMode);

			if (outputModeObj==null) {
				return sendAndLogError(Status.BAD_REQUEST, String.format(Strings.illegalOutputMode,outputMode)  + Constants.StringsOutputMode.returnValues());
			}
		}

		Constants.InputFormat inputFormat = Utilities.strToInputFormat(format);
		if (inputFormat==null) {
			return sendAndLogError(Status.BAD_REQUEST, String.format(Strings.illegalFormat,format)  + Constants.InputFormat.returnValues());
		}

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.readLock().lock();
		File zipFile = null;
		try {

			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			Season season = seasonsDB.get(season_id);

			if (season == null) {
				return sendAndLogError(Status.NOT_FOUND, Strings.seasonNotFound);
			}

			//validate that pre 2.1 season is upgraded
			ValidationResults upgradedRes = validatePre21SeasonsIsUpgraded(season);
			if (upgradedRes!=null)
				return Response.status(upgradedRes.status).entity(Utilities.errorMsgToErrorJSON(upgradedRes.error)).build();

			LinkedList<OriginalString> originalStrings = new LinkedList<>();
			if(ids.size() == 0) {
				originalStrings = season.getOriginalStrings().getOrigStringsList();
			}
			else {
				@SuppressWarnings("unchecked")
				Map<String, OriginalString> originalStringsDB = (Map<String, OriginalString>)context.getAttribute(Constants.ORIG_STRINGS_DB_PARAM_NAME);

				for (int i = 0; i<ids.size();++i) {
					OriginalString originalString = originalStringsDB.get(ids.get(i));
					String currSeasonId = originalString.getSeasonId().toString();

					if (!season_id.equals(currSeasonId)) {
						return sendAndLogError(Status.NOT_FOUND, Strings.stringNotInSeason+ ids.get(i));
					}
					originalStrings.add(originalString);
				}
			}
			ImportExportUtilities ieUtil;
			if (format.equals(Constants.InputFormat.ANDROID.toString())) {
				ieUtil = new ImportExportUtilitiesAndroid();
			}else {
				ieUtil = new ImportExportUtilitiesIOS();
			}
			zipFile = ieUtil.exportToZipFile(season_id,originalStrings);
			return (Response.ok()).entity(zipFile).build();
		}
		catch (Exception e){
			return sendAndLogError(Status.BAD_REQUEST, Strings.failedExport + e.getMessage());
		}finally {
			readWriteLock.readLock().unlock();
		}
	}
	
	@GET
	@Path ("/seasons/{season-id}/strings/{status}")
	@ApiOperation(value = "Returns the strings for the specified season and status", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getStringsByStatus(@PathParam("season-id")String season_id, 
			@ApiParam(value="NEW_STRING or READY_FOR_TRANSLATION or IN_TRANSLATION or TRANSLATION_COMPLETE")@PathParam("status")String status,
			@ApiParam(value="BASIC or INCLUDE_TRANSLATIONS")@QueryParam("mode")String outputMode,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {		
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getStringsByStatus request");
		}

		String err = Utilities.validateLegalUUID(season_id);
		if (err!=null) {
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalSeasonUUID + err);
		}

		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, season_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;

		//check user authorization
		UserInfo userInfo = UserInfo.validate("TranslationServices.getStringsByStatus", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.TRANSLATIONS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		
		
		StringsOutputMode outputModeObj = StringsOutputMode.BASIC; //if not specified basic is the defaults
		if (outputMode!=null && !outputMode.isEmpty()) {
			outputModeObj = Utilities.strToStringsOutputMode(outputMode);

			if (outputModeObj==null) {
				return sendAndLogError(Status.BAD_REQUEST, String.format(Strings.illegalOutputMode,outputMode)  + Constants.StringsOutputMode.returnValues());
			}
		}

		if (status == null || status.isEmpty()) {
			return sendAndLogError(Status.BAD_REQUEST,  Strings.statusMissing);	
		}
		
		StringStatus strStatus = Utilities.strToStringStatus(status);
		if (strStatus == null) {
			return sendAndLogError(Status.BAD_REQUEST, String.format(Strings.illegalStatus,status)  + Constants.StringStatus.returnValues());
		}
		
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.readLock().lock();
		try {

			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);			

			Season season = seasonsDB.get(season_id);

			if (season == null) {
				return sendAndLogError(Status.NOT_FOUND, Strings.seasonNotFound);				
			}

			//validate that pre 2.1 season is upgraded			
			ValidationResults upgradedRes = validatePre21SeasonsIsUpgraded(season);
			if (upgradedRes!=null)
				return Response.status(upgradedRes.status).entity(Utilities.errorMsgToErrorJSON(upgradedRes.error)).build();

			JSONObject res = season.getOriginalStrings().getStringsByStatusJSON(outputModeObj, strStatus);

			return (Response.ok()).entity(res.toString()).build();
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	@DELETE
	@Path ("/seasons/strings/{string-id}")
	@ApiOperation(value = "Deletes the specified string")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "String not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response deleteString(@PathParam("string-id")String string_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException  {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("deleteString request: string_id =" + string_id);
		}
		AirlockChange change = new AirlockChange();
		String err = Utilities.validateLegalUUID(string_id);
		if (err!=null) {
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalStringUUID + err);
		}
		
		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfString(context, string_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);
		
		//check user authorization
		UserInfo userInfo = UserInfo.validate("TranslationServices.deleteString", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		long start = new Date().getTime(); // XXX TEMPORARY

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.TRANSLATIONS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();	
				
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.writeLock().lock();
		try {		

			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			@SuppressWarnings("unchecked")
			Map<String, OriginalString> originalStringsDB = (Map<String, OriginalString>)context.getAttribute(Constants.ORIG_STRINGS_DB_PARAM_NAME);

			if (!originalStringsDB.containsKey(string_id)) {
				return sendAndLogError(Status.NOT_FOUND, Strings.stringNotFound);
			}

			OriginalString stringToDel = originalStringsDB.get(string_id);			

			Season season = seasonsDB.get(stringToDel.getSeasonId().toString()); 
			change.setSeason(season);
			if (season == null) {
				return sendAndLogError(Status.BAD_REQUEST, String.format(Strings.nonExistingSeason,stringToDel.getSeasonId().toString()));				
			}

			//validate that pre 2.1 season is upgraded			
			ValidationResults upgradedRes = validatePre21SeasonsIsUpgraded(season);
			if (upgradedRes!=null)
				return Response.status(upgradedRes.status).entity(Utilities.errorMsgToErrorJSON(upgradedRes.error)).build();

			if (stringToDel.getStage() == Stage.PRODUCTION) {
				return sendAndLogError(Status.BAD_REQUEST, Strings.stringInProd);
			}

			long startRemove = new Date().getTime(); // XXX TEMPORARY
			
			String errorString = season.getOriginalStrings().removeOriginalString(stringToDel, context);
			if (errorString!=null) {
				return sendAndLogError(Status.BAD_REQUEST, errorString);
			}
			long endRemove = new Date().getTime() - startRemove;
			logger.info(" removeOriginalString (with validation) took " + endRemove + " ms");

			originalStringsDB.remove(string_id);

			//writing updated strings list to S3
			try {
				change.getFiles().addAll(AirlockFilesWriter.writeOriginalStrings(season, context, stringToDel.getStage()));				
				change.getFiles().addAll(AirlockFilesWriter.writeEnStringsFiles(season, context, stringToDel.getStage() == Stage.PRODUCTION));
			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}
			try {
				//deleteStringFromLanguageFiles(season, stringToDel.getKey(), context, stringToDel.getStage() == Stage.PRODUCTION);
				change.getFiles().addAll(AirlockFilesWriter.writeAllLocalesStringsFiles(season, context, stringToDel.getStage() == Stage.PRODUCTION, false));
				Webhooks.get(context).notifyChanges(change, context);

			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}
			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Delete string: key: " + stringToDel.getKey() + ", uniqueId: " + string_id, userInfo); 

			logger.info("String " + stringToDel.getKey() + ", uniqueId:" + string_id + " was deleted");

			long diff = new Date().getTime() - start;
			logger.info(" deleteString (with write to S3) took " + diff + " ms");

			return (Response.ok()).build();
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}	

	@GET
	@Path ("/seasons/strings/{string-id}")
	@ApiOperation(value = "Returns the specified string", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "String not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getString(@PathParam("string-id")String string_id,
			@ApiParam(value="BASIC or INCLUDE_TRANSLATIONS")@QueryParam("mode")String outputMode,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getString request");
		}

		String err = Utilities.validateLegalUUID(string_id);
		if (err!=null) {
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalStringUUID + err);
		}
		
		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfString(context, string_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		
		//check user authorization
		UserInfo userInfo = UserInfo.validate("TranslationServices.getString", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);
		
		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.TRANSLATIONS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();	
				
		StringsOutputMode outputModeObj = StringsOutputMode.BASIC; //if not specified basic is the defaults
		if (outputMode!=null && !outputMode.isEmpty()) {
			outputModeObj = Utilities.strToStringsOutputMode(outputMode);

			if (outputModeObj==null) {
				return sendAndLogError(Status.BAD_REQUEST, String.format(Strings.illegalOutputMode,outputMode)  + Constants.StringsOutputMode.returnValues());
			}
		}
		
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.readLock().lock();
		try {			

			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			@SuppressWarnings("unchecked")
			Map<String, OriginalString> originalStringsDB = (Map<String, OriginalString>)context.getAttribute(Constants.ORIG_STRINGS_DB_PARAM_NAME);

			OriginalString origstr = originalStringsDB.get(string_id);

			if (origstr == null) {
				return sendAndLogError(Status.NOT_FOUND, Strings.stringNotFound);
			}

			Season season = seasonsDB.get(origstr.getSeasonId().toString());
			if (season == null) {
				return sendAndLogError(Status.NOT_FOUND, Strings.seasonNotFound);				
			}

			//validate that pre 2.1 season is upgraded			
			ValidationResults upgradedRes = validatePre21SeasonsIsUpgraded(season);
			if (upgradedRes!=null)
				return Response.status(upgradedRes.status).entity(Utilities.errorMsgToErrorJSON(upgradedRes.error)).build();

			return Response.ok(origstr.toJson(outputModeObj, season).toString()).build();					
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	@PUT
	@Path ("/seasons/strings/{string-id}")
	@ApiOperation(value = "Updates the specified string", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "String not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response updateString(@PathParam("string-id")String string_id, String string,
			@ApiParam(value="VALIDATE or ACT")@QueryParam("mode")String mode,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("updateString request: string_id =" + string_id +", string = " + string);
		}
		AirlockChange change = new AirlockChange();
		String err = Utilities.validateLegalUUID(string_id);
		if (err!=null) {
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalStringUUID + err);
		}

		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfString(context, string_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);
		
		//check user authorization		
		UserInfo userInfo = UserInfo.validate("TranslationServices.updateString", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.TRANSLATIONS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();	
				
		long start = new Date().getTime(); // XXX TEMPORARY		
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.writeLock().lock();
		try {

			@SuppressWarnings("unchecked")
			Map<String, OriginalString> originalStringsDB = (Map<String, OriginalString>)context.getAttribute(Constants.ORIG_STRINGS_DB_PARAM_NAME);

			@SuppressWarnings("unchecked")			
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			OriginalString origStrToUpdate = originalStringsDB.get(string_id);
			if (origStrToUpdate == null) {
				return sendAndLogError(Status.NOT_FOUND, Strings.stringNotFound);			
			}			

			Season season = seasonsDB.get(origStrToUpdate.getSeasonId().toString()); //after validate we know the season exists
			if (season == null) {
				return sendAndLogError(Status.NOT_FOUND, Strings.seasonNotFound);				
			}
			change.setSeason(season);

			//validate that pre 2.1 season is upgraded			
			ValidationResults upgradedRes = validatePre21SeasonsIsUpgraded(season);
			if (upgradedRes!=null)
				return Response.status(upgradedRes.status).entity(Utilities.errorMsgToErrorJSON(upgradedRes.error)).build();

			JSONObject updatedStringJSON = null;
			try {
				updatedStringJSON = new JSONObject(string);
			} catch (JSONException je) {
				return sendAndLogError(Status.BAD_REQUEST, Strings.illegalInputJSON + je.getMessage());
			}						

			//if not set - set the uniqueId to be the id path param
			if (!updatedStringJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) || updatedStringJSON.get(Constants.JSON_FIELD_UNIQUE_ID) == null) {
				updatedStringJSON.put(Constants.JSON_FIELD_UNIQUE_ID, string_id);
			}
			else {
				//verify that string-id in path is identical to uniqueId in request pay-load  
				if (!updatedStringJSON.getString(Constants.JSON_FIELD_UNIQUE_ID).equals(string_id)) {
					return sendAndLogError(Status.BAD_REQUEST, Strings.stringWithDifferentId);					
				}
			}
			
			ValidationResults validationRes = origStrToUpdate.validateOriginalStringJSON(updatedStringJSON, context, userInfo);
			if (validationRes!=null) {
				return sendAndLogError(validationRes.status, validationRes.error);
			}

			if ("VALIDATE".equals(mode))
			{
				OriginalString newString = new OriginalString();
				newString.fromJSON (updatedStringJSON, season);

				// get similar strings broken down by variant
				JSONObject json = season.getOriginalStrings().getStringXref().addSimilarToJson(newString);
				return (Response.ok(json.toString())).build();
			}

			String origInternationalFallback = origStrToUpdate.getInternationalFallback();
			Stage origStage = origStrToUpdate.getStage();
			String origValue = origStrToUpdate.getValue();			
			Stage changeStage = origStage == Stage.DEVELOPMENT && origStrToUpdate.getStage() == Stage.DEVELOPMENT ? Stage.DEVELOPMENT : Stage.PRODUCTION;
			//finally - actually update the string.
			String updateDetails = origStrToUpdate.updateOriginalString(updatedStringJSON);

			String newInternationalFallback = origStrToUpdate.getInternationalFallback();
			if (!updateDetails.isEmpty()) { //if some fields were changed		
				try {
					change.getFiles().addAll(AirlockFilesWriter.writeOriginalStrings(season, context, changeStage));
					if (!origValue.equals(origStrToUpdate.getValue()) || origStage != origStrToUpdate.getStage()) {
						//update English strings only if the value or the stage were changed
						change.getFiles().addAll(AirlockFilesWriter.writeEnStringsFiles(season, context, origStage == Stage.PRODUCTION || origStrToUpdate.getStage() == Stage.PRODUCTION));
					}

				} catch (IOException e) {
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
				}

				AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
				auditLogWriter.log("Update string: " + origStrToUpdate.getKey() + ", " + string_id + ":   " + updateDetails, userInfo);

				try {
					if (!areInternationalFallbacksEquals(origInternationalFallback, newInternationalFallback) ||
							(origStage != origStrToUpdate.getStage())) {
						//update locale strings only if the intenationalFallback or the stage were changed
						change.getFiles().addAll(AirlockFilesWriter.writeAllLocalesStringsFiles(season, context, origStage == Stage.PRODUCTION || origStrToUpdate.getStage() == Stage.PRODUCTION, false));
					}
				} catch (IOException e) {
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
				}
				Webhooks.get(context).notifyChanges(change, context);
			}

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Updated string: " + origStrToUpdate.toJson(StringsOutputMode.BASIC, season) + "\n updatd details: " + updateDetails);
			}
			
			JSONObject obj = origStrToUpdate.toJson(StringsOutputMode.INCLUDE_TRANSLATIONS, season);

			return (Response.ok(obj.toString())).build();
		} finally {
			readWriteLock.writeLock().unlock();
			long diff = new Date().getTime() - start;
			logger.info(" string update took " + diff + " ms");
		}
	}	

	@POST
	@Path ("/{season-id}/supportedlocales/{locale}") 
	@ApiOperation(value = "Add supported locale for the specified season", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response addSupportedLocale(@PathParam("season-id")String season_id, @PathParam("locale")String locale,
			@ApiParam(value="Optional. The source locale.")@QueryParam("source")String source,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("addSupportedLocale request, locale = " + locale);
		}
		AirlockChange change = new AirlockChange();
		String err = Utilities.validateLegalUUID(season_id);
		if (err!=null) {
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalSeasonUUID + err);
		}
		
		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, season_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);
		
		//check user authorization
		UserInfo userInfo = UserInfo.validate("TranslationServices.addSupportedLocale", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.TRANSLATIONS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		
		
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.writeLock().lock();
		try {
			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			//Validate season existence
			Season season = seasonsDB.get(season_id);
			change.setSeason(season);
			if (season == null) {
				return sendAndLogError(Status.NOT_FOUND, Strings.seasonNotFound);				
			}

			//validate that pre 2.1 season is upgraded			
			ValidationResults upgradedRes = validatePre21SeasonsIsUpgraded(season);
			if (upgradedRes!=null)
				return Response.status(upgradedRes.status).entity(Utilities.errorMsgToErrorJSON(upgradedRes.error)).build();

			//validate that locale is not one of the supported languages yet
			if (season.getOriginalStrings().isLanguageSupported(locale)) {
				return sendAndLogError(Status.BAD_REQUEST, Strings.translationAlreadyExist);			
			}
			
			//validate that the source locale (if specified) exists in the season
			if (source!=null && !season.getOriginalStrings().isLanguageSupported(source)) {
				return sendAndLogError(Status.BAD_REQUEST, Strings.translationDoesNotExist);
			}

			if (locale.equals(Constants.DEFAULT_LANGUAGE)) {  //English files are not translated
				return sendAndLogError(Status.BAD_REQUEST, Strings.englishNotNeeded);			
			}

			season.getOriginalStrings().addSupportedLanguage(locale, source);
			//move strings status from translation_complete to in_translation since there is one locale (the new one)
			//that is not translated
			Stage changeStage = updateStringFromStatusToStatus(StringStatus.TRANSLATION_COMPLETE, StringStatus.IN_TRANSLATION, season.getOriginalStrings().getOrigStringsList());
			try {
				change.getFiles().addAll(AirlockFilesWriter.writeOriginalStrings(season, context, changeStage));				
				change.getFiles().addAll(AirlockFilesWriter.writeLocaleStringsFiles(season, locale, context, true/*productionStringAdded*/)); //always write both development and production for new locale

				//generate defaults file since supported languages list was updated
				change.getFiles().addAll(AirlockFilesWriter.doWriteDefaultsFile(season, context, changeStage));
				Webhooks.get(context).notifyChanges(change, context);
			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}

			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Loacle '" + locale + "' added.\n", userInfo);

			return (Response.ok()).build();
		}catch (Throwable e) {
			logger.log(Level.SEVERE, "Error adding supported locale: ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error adding supported locale: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}
	
	@DELETE
	@Path ("/{season-id}/supportedlocales/{locale}") 
	@ApiOperation(value = "Remove supported locale for the specified season", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response removeSupportedLocale(@PathParam("season-id")String season_id, @PathParam("locale")String locale,
			@ApiParam(value="optional")@QueryParam("removeRuntimeFiles") Boolean removeRuntimeFiles,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("removeSupportedLocale request, locale = " + locale);
		}
		AirlockChange change = new AirlockChange();
		String err = Utilities.validateLegalUUID(season_id);
		if (err!=null) {
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalSeasonUUID + err);
		}
		
		if (removeRuntimeFiles == null) {
			removeRuntimeFiles = true;
		}
		
		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, season_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);
		
		//check user authorization
		UserInfo userInfo = UserInfo.validate("TranslationServices.removeSupportedLocale", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);
		
		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.TRANSLATIONS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		
		
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();		
		readWriteLock.writeLock().lock();
		try {
			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
			
			//Validate season existence
			Season season = seasonsDB.get(season_id);
			change.setSeason(season);
			if (season == null) {
				return sendAndLogError(Status.NOT_FOUND, Strings.seasonNotFound);				
			}

			//validate that pre 2.1 season is upgraded			
			ValidationResults upgradedRes = validatePre21SeasonsIsUpgraded(season);
			if (upgradedRes!=null)
				return Response.status(upgradedRes.status).entity(Utilities.errorMsgToErrorJSON(upgradedRes.error)).build();

			//validate that locale is one of the supported languages yet
			boolean localeExists = season.getOriginalStrings().isLanguageSupported(locale);			
			if (!localeExists) {
				return sendAndLogError(Status.BAD_REQUEST, Strings.translationDoesNotExist);			
			}

			if (locale.equals(Constants.DEFAULT_LANGUAGE)) {  //English files are not translated
				return sendAndLogError(Status.BAD_REQUEST, Constants.DEFAULT_LANGUAGE + Strings.cannotRemove);			
			}

			Stage changeStage = season.getOriginalStrings().removeSupportedLanguage(locale);
						
			try {
				if (removeRuntimeFiles) {
					change.getFiles().addAll(removeLocaleFiles(season, context, locale));
				}
				change.getFiles().addAll(AirlockFilesWriter.writeOriginalStrings(season, context, changeStage));				
				change.getFiles().addAll(Utilities.touchProductionChangedFile(context, ds, season));
				
				//generate defaults file since supported languages list was updated
				change.getFiles().addAll(AirlockFilesWriter.doWriteDefaultsFile(season, context, changeStage));
				Webhooks.get(context).notifyChanges(change, context);
			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}

			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Loacle '" + locale + "' removed.\n", userInfo);
			return (Response.ok()).build();
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}
	
	private LinkedList<AirlockChangeContent> removeLocaleFiles(Season season, ServletContext context, String locale) throws IOException {
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		String error = null;
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		String separator = ds.getSeparator();
		String devLocaleStringsFilePath = Constants.SEASONS_FOLDER_NAME+separator+season.getProductId().toString()+
				separator+season.getUniqueId().toString()+separator+Constants.TRANSLATIONS_FOLDER_NAME + 
				separator + Constants.STRINGS_FILE_NAME_PREFIX + locale + Stage.DEVELOPMENT.toString() + Constants.STRINGS_FILE_NAME_EXTENSION;

		String prodLocaleStringsFilePath = Constants.SEASONS_FOLDER_NAME+separator+season.getProductId().toString()+
				separator+season.getUniqueId().toString()+separator+Constants.TRANSLATIONS_FOLDER_NAME + 
				separator + Constants.STRINGS_FILE_NAME_PREFIX + locale + Stage.PRODUCTION.toString() + Constants.STRINGS_FILE_NAME_EXTENSION;

	
	
		try {			
			ds.deleteData(devLocaleStringsFilePath, true);
			changesArr.add(AirlockChangeContent.getAdminChange(Constants.FILE_DELETED, devLocaleStringsFilePath, Stage.DEVELOPMENT));
			ds.deleteData(prodLocaleStringsFilePath, true);
			changesArr.add(AirlockChangeContent.getAdminChange(Constants.FILE_DELETED, prodLocaleStringsFilePath, Stage.PRODUCTION));
			return changesArr;
		} catch (IOException ioe) {
			//failed writing 3 times to s3. Set server state to ERROR.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			error = Strings.failedDeletingString + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			throw new IOException(error);			
		}		
		
	}


	//
	@GET
	@Path ("/{season-id}/supportedlocales") 
	@ApiOperation(value = "Returns the supported locales for the specified season", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getSupportedLocales(@PathParam("season-id")String season_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getSupportedLocales request");
		}

		String err = Utilities.validateLegalUUID(season_id);
		if (err!=null) {
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalSeasonUUID + err);
		}

		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, season_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;

		//check user authorization
		UserInfo userInfo = UserInfo.validate("TranslationServices.getSupportedLocales", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.TRANSLATIONS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		
		
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.readLock().lock();
		try {
			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			//Validate season existence
			Season season = seasonsDB.get(season_id);

			if (season == null) {
				return sendAndLogError(Status.NOT_FOUND, Strings.seasonNotFound);				
			}

			//validate that pre 2.1 season is upgraded			
			ValidationResults upgradedRes = validatePre21SeasonsIsUpgraded(season);
			if (upgradedRes!=null)
				return Response.status(upgradedRes.status).entity(Utilities.errorMsgToErrorJSON(upgradedRes.error)).build();

			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, season_id);			
			res.put(Constants.JSON_FIELD_SUPPORTED_LANGUAGES,  season.getOriginalStrings().getSupportedLanguages());				

			return (Response.ok(res.toString())).build();

		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	private boolean areInternationalFallbacksEquals(String origInternationalFallback, String newInternationalFallback) {
		if (newInternationalFallback == null && origInternationalFallback == null)
			return true;

		if (origInternationalFallback==null && newInternationalFallback!=null)
			return false;

		if (origInternationalFallback!=null && newInternationalFallback==null) 
			return false;

		if (!origInternationalFallback.equals(newInternationalFallback)) 
			return false;

		return true;
	}
	
	@GET
	@Path ("/seasons/{item-id}/stringsinuse")
	@ApiOperation(value = "Returns the strings used in the specified item ", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Feature not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getStringsUsedInFeature(@PathParam("item-id")String item_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getStringsUsedInFeature request");
		}

		String err = Utilities.validateLegalUUID(item_id);
		if (err!=null) {
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalFeatureUUID + err);
		}
		
		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductForBranchOrFeature(context, Constants.MASTER_BRANCH_NAME, item_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		
		//check user authorization
		UserInfo userInfo = UserInfo.validate("TranslationServices.getStringsUsedInFeature", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.TRANSLATIONS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();	
						
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.readLock().lock();
		try {

			@SuppressWarnings("unchecked")
			Map<String, BaseAirlockItem> airlockItemsDB = (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);

			BaseAirlockItem alItem = airlockItemsDB.get(item_id);

			if (alItem == null) {
				return sendAndLogError(Status.NOT_FOUND, Strings.AirlockItemNotFound);				
			}

			if (!(alItem instanceof DataAirlockItem)) {
				return sendAndLogError(Status.NOT_FOUND, Strings.onlyFeatureConfigUseStrings);		
			}	
			
			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			//Validate season existence
			Season season = seasonsDB.get(alItem.getSeasonId().toString());

			if (season == null) {
				return sendAndLogError(Status.NOT_FOUND, Strings.seasonNotFound);				
			}
			
			JSONObject res = ((DataAirlockItem)alItem).getStringsInUseByItem(season.getOriginalStrings().getAllStringKeys(), season);

			return (Response.ok()).entity(res.toString()).build();
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	@GET
	@Path ("/seasons/{string-id}/stringusage")
	@ApiOperation(value = "Returns the configuration/utilities using the specified string ", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Feature not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getStringUsage(@PathParam("string-id")String string_id,
											@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getStringsUsedInFeature request");
		}

		String err = Utilities.validateLegalUUID(string_id);
		if (err!=null) {
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalStringUUID + err);
		}
		
		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfString(context, string_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		
		//check user authorization
		UserInfo userInfo = UserInfo.validate("TranslationServices.getStringUsage", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		
		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.TRANSLATIONS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();	
				
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.readLock().lock();
		try {

			@SuppressWarnings("unchecked")
			Map<String, OriginalString> originalStringsDB = (Map<String, OriginalString>)context.getAttribute(Constants.ORIG_STRINGS_DB_PARAM_NAME);

			if (!originalStringsDB.containsKey(string_id)) {
				return sendAndLogError(Status.NOT_FOUND, Strings.stringNotFound);
			}

			OriginalString oriString = originalStringsDB.get(string_id);
			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);
			String seasonId = oriString.getSeasonId().toString();
			Season season = seasonsDB.get(seasonId);
			JSONObject res = new JSONObject();
			JSONArray configIds = new JSONArray();
			//JSONArray featuresIds = new JSONArray();
			JSONArray utilitiesIds = new JSONArray();

			Set<String> stringKeySet = new HashSet<>();
			String oriKey = oriString.getKey();
			stringKeySet.add(oriKey);
			List<Branch> allBranches = season.getBranches().getBranchesList();
			for(int i = 0; i<allBranches.size();++i) {
				String branchId = allBranches.get(i).getUniqueId().toString();
				String branchName =  allBranches.get(i).getName();
				configIds.addAll(getConfigsUsingString(branchId,branchName,oriKey,stringKeySet,seasonId));
			}
			configIds.addAll(getConfigsUsingString(Constants.MASTER_BRANCH_NAME,Constants.MASTER_BRANCH_NAME,oriKey,stringKeySet,seasonId));
			Season.Utilities utils = season.getUtilities();
			List<String> utilsIds = utils.utilitiesIdsUsingString(oriKey);
			for(int i = 0; i<utilsIds.size();++i){
				utilitiesIds.add(utilsIds.get(i));
			}

			res.put("UsedByConfigurations",configIds);
			//res.put("UsedByFeatures",featuresIds);
			res.put("UsedByUtilities",utilitiesIds);
			return (Response.ok()).entity(res.toString()).build();
		} catch (MergeBranch.MergeException e) {
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	@GET
	@Path ("/seasons/{season-id}/strings/unused")
	@ApiOperation(value = "Returns unused strings", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Feature not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getUnusedStrings(@PathParam("season-id")String season_id,
											@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getUnusedStrings request");
		}

		String err = Utilities.validateLegalUUID(season_id);
		if (err!=null) {
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalSeasonUUID + err);
		}
		
		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, season_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		
		//check user authorization
		UserInfo userInfo = UserInfo.validate("TranslationServices.getStrings", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.TRANSLATIONS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		
				
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.readLock().lock();
		try {
			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			Season season = seasonsDB.get(season_id);

			if (season == null) {
				return sendAndLogError(Status.NOT_FOUND, Strings.seasonNotFound);
			}
			
			
			Set<String> stringsInUseByConfigList = new HashSet<String>();
			Set<String> stringsInUseByUtilList = new HashSet<String>();
			
			season.getRoot().doGetStringsInUseByItem(season.getOriginalStrings().getAllStringKeys(), season.getRoot(), stringsInUseByConfigList, stringsInUseByUtilList, season, true, false);
			String javascriptFunctions = season.getUtilities().generateUtilityCodeSectionForStageAndType(Stage.DEVELOPMENT, null, null, null, UtilityType.MAIN_UTILITY);
			stringsInUseByUtilList.addAll(VerifyRule.findAllTranslationIds(season.getOriginalStrings().getAllStringKeys(), javascriptFunctions, false));
			javascriptFunctions = season.getUtilities().generateUtilityCodeSectionForStageAndType(Stage.DEVELOPMENT, null, null, null, UtilityType.STREAMS_UTILITY);
			stringsInUseByUtilList.addAll(VerifyRule.findAllTranslationIds(season.getOriginalStrings().getAllStringKeys(), javascriptFunctions, false));
			
			for (Branch branch : season.getBranches().getBranchesList()) {
				Map<String, BaseAirlockItem> branchAirlockItemsBD =  branch.getBranchAirlockItemsBD();
				for (Map.Entry<String, BaseAirlockItem> entry : branchAirlockItemsBD.entrySet()) {
					BaseAirlockItem item = entry.getValue();
					if (item instanceof ConfigurationRuleItem) {
						item.doGetStringsInUseByItem(season.getOriginalStrings().getAllStringKeys(), item, stringsInUseByConfigList, stringsInUseByUtilList, season, false, false);
					}
				}
			}
			Set<String> stringsInUse = new HashSet<String>();
			stringsInUse.addAll(stringsInUseByConfigList);
			stringsInUse.addAll(stringsInUseByUtilList);
			
			JSONArray unusedStrings = new JSONArray();
			for (OriginalString origStr : season.getOriginalStrings().getOrigStringsList()) {
				String key = origStr.getKey();
				if (!stringsInUse.contains(key)) {
					JSONObject unusedStringObj = new JSONObject();
					UUID stringId = origStr.getUniqueId();
					unusedStringObj.put(Constants.JSON_FIELD_UNIQUE_ID, origStr.getUniqueId().toString());
					unusedStringObj.put(Constants.JSON_FIELD_KEY, key);
					unusedStrings.add(unusedStringObj);
				}
			}
			
			JSONObject res = new JSONObject();
			
			res.put("unusedStrings",unusedStrings);
			return (Response.ok()).entity(res.toString()).build();
		} finally {
			readWriteLock.readLock().unlock();
		}
	}
	
	//TODO: if no fall back declared dont use the value .
	//deletion of internationalFallback will couse deletion of this string from all locales if wasn't translated? 
	//how do i know that is wasn't translated? if equals internationalFallback?
	//handle in string addition/deletion and update
	//handle in add/update translations as well

	private JSONArray getConfigsUsingString(String branchId,String branchName, String oriKey,Set<String> stringKeySet,String seasonId) throws MergeBranch.MergeException,JSONException{
		JSONArray configIds = new JSONArray();
		Map<String, BaseAirlockItem> airlockItemsDB = Utilities.getAirlockItemsDB(branchId, context);
		Collection<BaseAirlockItem> items = airlockItemsDB.values();
		Iterator<BaseAirlockItem> iterator = items.iterator();
		while (iterator.hasNext()) {
			BaseAirlockItem item = iterator.next();
			if (item.getType().equals(BaseAirlockItem.Type.CONFIGURATION_RULE) && item.getSeasonId().toString().equals(seasonId) && (branchName == Constants.MASTER_BRANCH_NAME || item.getBranchStatus() != Constants.BranchStatus.NONE )) {
				if (((DataAirlockItem) item).isStringUsed(oriKey, stringKeySet)) {
					JSONObject config = new JSONObject();
					config.put("configID", item.getUniqueId().toString());
					config.put("configName", item.getNameSpaceDotName());
					FeatureItem feature = ((ConfigurationRuleItem) item).getParentFeature(airlockItemsDB);
					config.put("featureID", feature.getUniqueId().toString());
					config.put("featureName", feature.getNameSpaceDotName());
					config.put("branchName", branchName);
					configIds.add(config);

				}
			}
		}
		return configIds;
	}

	private static ValidationResults validatePre21SeasonsIsUpgraded(Season season) {
		if (season.getOriginalStrings().isOldEnStringsFileExists() && !season.getOriginalStrings().isUpgradedPre21Season()) {
			String errMsg = Strings.mustUpgradeSeason;
			logger.severe(errMsg);
			return new ValidationResults(errMsg, Status.BAD_REQUEST);					
		}

		return null;
	}


	//getNewStringsForTranslation - no locale
	//@Path ("/seasons/{season-id}/newstringsfortranslation")
	@GET
	@Path ("/seasons/{season-id}/newstringsfortranslation")
	@ApiOperation(value = "Returns the strings that are reviewed for translation in the formate required for translation", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getNewStringsForTranslation(@PathParam("season-id")String season_id,
			@ApiParam(value="When not specified, all strings that are reviewed for translation are retrieved")@QueryParam("ids")List<String> ids, 
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getNewStringsForTranslation request");
		}

		String err = Utilities.validateLegalUUID(season_id);
		if (err!=null) {
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalSeasonUUID + err);
		}
	
		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, season_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
	
		//check user authorization
		UserInfo userInfo = UserInfo.validate("TranslationServices.getNewStringsForTranslation", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

	
		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.TRANSLATIONS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		
		
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		
		readWriteLock.readLock().lock();
		try {

			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);			

			@SuppressWarnings("unchecked")
			Map<String, OriginalString> originalStringsDB = (Map<String, OriginalString>)context.getAttribute(Constants.ORIG_STRINGS_DB_PARAM_NAME);

			Season season = seasonsDB.get(season_id);

			//validate season
			if (season == null) {
				return sendAndLogError(Status.NOT_FOUND, Strings.seasonNotFound);				
			}

			//validate version. Only post 2.1 seasons support translation statuses
			ValidationResults validationRes = validateTranslationStatusesSupport(season);
			if (validationRes!=null) 
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(validationRes.error)).build();	

			//for now only REVIEWED_FOR_TRANSLATION strings are sent to translation
			ValidationResults vr = validateStrings (ids, Constants.StringStatus.REVIEWED_FOR_TRANSLATION, originalStringsDB, "sendToTranslation", userInfo);
			if (vr!=null) {
				logger.severe(vr.error);			
				return Response.status(vr.status).entity(Utilities.errorMsgToErrorJSON(vr.error)).build();
			}

			String stringsForTranslations = season.getOriginalStrings().toTranslationFormatForNewStrings(ids);			
			return (Response.ok()).entity(stringsForTranslations).build();
		} finally {
			readWriteLock.readLock().unlock();
		}
	}
	
	@PUT
	@Path ("/seasons/{string-id}/overridetranslate/{locale}")
	@ApiOperation(value = "Override the translation of the given string in the specified locale.", response = String.class) 
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "String not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response overrideTranslation(@PathParam("string-id")String string_id, 
			@PathParam("locale")String locale, 			
			String overrideValue,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("overrideTranslation request: string_id =" + string_id + ", locale =" + locale + ", overrideValue = " + overrideValue);
		}
		AirlockChange change = new AirlockChange();
		String err = Utilities.validateLegalUUID(string_id);
		if (err!=null) {
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalStringUUID + err);
		}

		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfString(context, string_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);
		//check user authorization
		UserInfo userInfo = UserInfo.validate("TranslationServices.overrideTranslation", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.TRANSLATIONS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();	
				
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.readLock().lock();
		try {

			@SuppressWarnings("unchecked")
			Map<String, OriginalString> originalStringsDB = (Map<String, OriginalString>)context.getAttribute(Constants.ORIG_STRINGS_DB_PARAM_NAME);

			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);			

			OriginalString origStrToUpdate = originalStringsDB.get(string_id);
			if (origStrToUpdate == null) {
				return sendAndLogError(Status.NOT_FOUND, Strings.stringNotFound);			
			}			

			Season season = seasonsDB.get(origStrToUpdate.getSeasonId().toString());
			change.setSeason(season);
			
			//validate version. Only post 2.1 seasons support translation statuses
			ValidationResults validationRes = validateTranslationStatusesSupport(season);
			if (validationRes!=null) 
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(validationRes.error)).build();	


			//validate locale			
			boolean localeExists = season.getOriginalStrings().isLanguageSupported(locale);
			if (!localeExists) {
				return sendAndLogError(Status.BAD_REQUEST, Strings.localNotSupported);			
			}

			if (locale.equals(Constants.DEFAULT_LANGUAGE)) {  //English files are not translated
				return sendAndLogError(Status.BAD_REQUEST, Strings.englishNotNeeded);			
			}	

			if (productionUnauthorized(origStrToUpdate, userInfo)) {
				return sendAndLogError(Status.BAD_REQUEST, Strings.prodOverrideError);					
			} 		 
			origStrToUpdate.overrideTranslation (locale, overrideValue, season.getOriginalStrings().getSupportedLanguages());

			//need to write the original strings file since some statuses may be changed
			try {
				change.getFiles().addAll(AirlockFilesWriter.writeOriginalStrings(season, context, origStrToUpdate.getStage()));
				change.getFiles().addAll(AirlockFilesWriter.writeLocaleStringsFiles(season, locale, context, origStrToUpdate.getStage().equals(Stage.PRODUCTION)));
				Webhooks.get(context).notifyChanges(change, context);
			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}

			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Override translation for string "  + origStrToUpdate.getKey() + ", " + origStrToUpdate.getUniqueId().toString() + ", for locale " + locale + ": " + overrideValue  , userInfo);
			
			return (Response.ok()).entity("").build();
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	@PUT
	@Path ("/seasons/{string-id}/canceloverride/{locale}")
	@ApiOperation(value = "Cancel the translation override of the given string in the specified locale.", response = String.class) 
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "String not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response cancelOverride(@PathParam("string-id")String string_id, 
			@PathParam("locale")String locale, 						
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("cancelOverride request: string_id =" + string_id + ", locale =" + locale );
		}

		String err = Utilities.validateLegalUUID(string_id);
		if (err!=null) {
			return sendAndLogError(Strings.illegalStringUUID + err);
		}				
		AirlockChange change = new AirlockChange();

		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfString(context, string_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);
		
		//check user authorization
		UserInfo userInfo = UserInfo.validate("TranslationServices.cancelOverride", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);


		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.TRANSLATIONS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();	
				
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.readLock().lock();
		try {

			@SuppressWarnings("unchecked")
			Map<String, OriginalString> originalStringsDB = (Map<String, OriginalString>)context.getAttribute(Constants.ORIG_STRINGS_DB_PARAM_NAME);

			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);			

			OriginalString origStrToUpdate = originalStringsDB.get(string_id);
			if (origStrToUpdate == null) {
				return sendAndLogError(Status.NOT_FOUND, Strings.stringNotFound);
			}			

			Season season = seasonsDB.get(origStrToUpdate.getSeasonId().toString());
			change.setSeason(season);
			
			//validate version. Only post 2.1 seasons support translation statuses
			ValidationResults validationRes = validateTranslationStatusesSupport(season);
			if (validationRes!=null) 
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(validationRes.error)).build();	

			//validate locale			
			boolean localeExists = season.getOriginalStrings().isLanguageSupported(locale);
			if (!localeExists) {
				return sendAndLogError(Status.BAD_REQUEST, Strings.localNotSupported);			
			}

			if (locale.equals(Constants.DEFAULT_LANGUAGE)) {  //English files are not translated
				return sendAndLogError(Strings.englishNotNeeded);
			}	

			if (productionUnauthorized(origStrToUpdate, userInfo)) {
				return sendAndLogError(Strings.prodCancelOverrideError);
			} 

			LocaleTranslationData transData = origStrToUpdate.getStringTranslations().getTranslationDataPerLocale(locale);
			if (transData == null ||  !transData.getTranslationStatus().equals(TranslationStatus.OVERRIDE) ) {
				return sendAndLogError(Strings.nonExistingOverride);
			}

			origStrToUpdate.cancelOverride (locale);

			//need to write the original strings file since some statuses may be changed
			try {
				change.getFiles().addAll(AirlockFilesWriter.writeOriginalStrings(season, context, origStrToUpdate.getStage()));
				change.getFiles().addAll(AirlockFilesWriter.writeLocaleStringsFiles(season, locale, context, origStrToUpdate.getStage().equals(Stage.PRODUCTION)));
				Webhooks.get(context).notifyChanges(change, context);
			} catch (IOException e) {
				return sendAndLogError(Status.INTERNAL_SERVER_ERROR, e.getMessage());	
			}

			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Override canceled for string "  + origStrToUpdate.getKey() + ", " + origStrToUpdate.getUniqueId().toString() + ", for locale " + locale , userInfo);
			
			return (Response.ok()).entity("").build();
		} finally {
			readWriteLock.readLock().unlock();
		}
	}
	@PUT
	@Path ("/seasons/{string-id}/replacetranslation/{locale}")
	@ApiOperation(value = "Accept a pending translation to replace current translation of the string in the specified locale.", response = String.class) 
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "String not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response replaceTranslation(@PathParam("string-id")String string_id, 
			@PathParam("locale")String locale, 			
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("replacetranslation request: string_id =" + string_id + ", locale =" + locale);
		}
		AirlockChange change = new AirlockChange();
		String err = Utilities.validateLegalUUID(string_id);
		if (err!=null)
			return sendAndLogError(Strings.illegalStringUUID + err);

		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfString(context, string_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);
		
		//check user authorization
		UserInfo userInfo = UserInfo.validate("TranslationServices.replaceTranslation", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.TRANSLATIONS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();	
				
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.readLock().lock();
		try {

			@SuppressWarnings("unchecked")
			Map<String, OriginalString> originalStringsDB = (Map<String, OriginalString>)context.getAttribute(Constants.ORIG_STRINGS_DB_PARAM_NAME);

			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);			

			OriginalString origStrToUpdate = originalStringsDB.get(string_id);
			if (origStrToUpdate == null) {
				return sendAndLogError(Status.NOT_FOUND, Strings.stringNotFound);			
			}			

			Season season = seasonsDB.get(origStrToUpdate.getSeasonId().toString());
			change.setSeason(season);
			
			//validate version. Only post 2.1 seasons support translation statuses
			ValidationResults validationRes = validateTranslationStatusesSupport(season);
			if (validationRes!=null) 
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(validationRes.error)).build();	

			//validate locale			
			boolean localeExists = season.getOriginalStrings().isLanguageSupported(locale);
			if (!localeExists)
				return sendAndLogError(Strings.localNotSupported);

			if (locale.equals(Constants.DEFAULT_LANGUAGE))
				return sendAndLogError(Strings.englishNotNeeded);

			if (productionUnauthorized(origStrToUpdate, userInfo))
				return sendAndLogError(Strings.prodReplaceTranslationError);

			LocaleTranslationData transData = origStrToUpdate.getStringTranslations().getTranslationDataPerLocale(locale);
			String newString = (transData == null) ? null : transData.getNewTranslationAvailable();
			if (newString == null || newString.isEmpty())
				return sendAndLogError(Strings.noPendingTranslation + locale);

			if (transData.getTranslationStatus() == TranslationStatus.OVERRIDE)
				return sendAndLogError(String.format(Strings.haveOverride,locale));

			transData.setTranslatedValue(newString);
			transData.setNewTranslationAvailable(null);

			try {
				change.getFiles().addAll(AirlockFilesWriter.writeOriginalStrings(season, context, origStrToUpdate.getStage()));
				change.getFiles().addAll(AirlockFilesWriter.writeLocaleStringsFiles(season, locale, context, origStrToUpdate.getStage().equals(Stage.PRODUCTION)));
				Webhooks.get(context).notifyChanges(change, context);
			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}

			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Replace translation for string "  + origStrToUpdate.getKey() + ", " + origStrToUpdate.getUniqueId().toString() + ", for locale " + locale + ": " + newString  , userInfo);

			return (Response.ok()).entity("").build();
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	@GET
	@Path ("/seasons/{season-id}/strings/statuses")
	@ApiOperation(value = "Returns the strings for the specified season ", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getStringStatuses(@PathParam("season-id")String season_id, 
			@ApiParam(value="When not specified, the status of all strings is returned")@QueryParam("ids")List<String> ids, 		
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getStringStatuses request");
		}

		String err = Utilities.validateLegalUUID(season_id);
		if (err!=null) {
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalSeasonUUID + err);
		}
		
		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, season_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		
		//check user authorization
		UserInfo userInfo = UserInfo.validate("TranslationServices.getStringStatuses", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.TRANSLATIONS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		
		
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();	
		readWriteLock.readLock().lock();
		try {

			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);			

			Season season = seasonsDB.get(season_id);

			if (season == null) {
				return sendAndLogError(Status.NOT_FOUND, Strings.seasonNotFound);				
			}

			//validate version. Only post 2.1 seasons support translation statuses
			ValidationResults validationRes = validateTranslationStatusesSupport(season);
			if (validationRes!=null) 
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(validationRes.error)).build();	

			//validate string ids
			if (ids!=null && ids.size()>0) {
				@SuppressWarnings("unchecked")
				Map<String, OriginalString> originalStringsDB = (Map<String, OriginalString>)context.getAttribute(Constants.ORIG_STRINGS_DB_PARAM_NAME);

				for (String strId:ids) {
					err = Utilities.validateLegalUUID(strId);
					if (err!=null) {
						return sendAndLogError(Status.BAD_REQUEST, Strings.illegalStringUUID + err);				
					}

					OriginalString origStr = originalStringsDB.get(strId);
					if (origStr == null) {
						return sendAndLogError(Status.NOT_FOUND, Strings.stringNotFound);
					}					
				}	
			}									

			JSONObject res = season.getOriginalStrings().getStringsStatus(ids);

			return (Response.ok()).entity(res.toString()).build();
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	@PUT
	@Path ("/seasons/{season-id}/markfortranslation")
	@ApiOperation(value = "Mark the specified strings for translation (ids in url or in body)", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response markForTranslation(@PathParam("season-id")String season_id,
			@QueryParam("ids")List<String> ids,
			String body,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		AirlockChange change = new AirlockChange();
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("markForTranslation request.");
		}

		String err = Utilities.validateLegalUUID(season_id);
		if (err!=null) {
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalSeasonUUID + err);
		}

		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, season_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);
		
		//check user authorization
		UserInfo userInfo = UserInfo.validate("TranslationServices.markForTranslation", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.TRANSLATIONS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		
		
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.writeLock().lock();
		try {

			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);			

			@SuppressWarnings("unchecked")
			Map<String, OriginalString> originalStringsDB = (Map<String, OriginalString>)context.getAttribute(Constants.ORIG_STRINGS_DB_PARAM_NAME);

			Season season = seasonsDB.get(season_id);
			change.setSeason(season);
			
			//validate season
			if (season == null) {
				return sendAndLogError(Status.NOT_FOUND, Strings.seasonNotFound);				
			}

			//validate version. Only post 2.1 seasons support translation statuses
			ValidationResults validationRes = validateTranslationStatusesSupport(season);
			if (validationRes!=null) 
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(validationRes.error)).build();	
					
			try {
				ids = resolveIds(ids, body);
			}
			catch (Exception e)
			{
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();										
			}

			ValidationResults vr = validateStrings (ids, Constants.StringStatus.NEW_STRING, originalStringsDB, "markForTranslation", userInfo);
			if (vr!=null) {
				return sendAndLogError(vr.status, vr.error);
			}

			Stage changeStage = updateStringStatus(ids, Constants.StringStatus.READY_FOR_TRANSLATION, originalStringsDB);

			//need to write the original strings file since the status was changed
			try {
				change.getFiles().addAll(AirlockFilesWriter.writeOriginalStrings(season, context, changeStage));
				Webhooks.get(context).notifyChanges(change, context);
			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}

			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Mark the strings: "  + Utilities.StringsListToString(ids) + " for translation." , userInfo);
			
			return (Response.ok("")).build();
		} finally {
			readWriteLock.writeLock().unlock();
		}		
	}

	List<String> resolveIds(List<String> ids, String body) throws Exception
	{
		if (ids != null && !ids.isEmpty())
			return ids;

		if (body == null || body.isEmpty())
			throw new Exception (Strings.idsMissing);

		JSONObject json;
		try { json = new JSONObject(body); }
		catch (Exception e) { throw new Exception(Strings.illegalInputJSON); }
		
		JSONArray array;
		try { array = json.getJSONArray(Constants.JSON_FIELD_STRING_IDS); }
		catch (Exception e) { throw new Exception(Strings.missingKeyJSON + Constants.JSON_FIELD_STRING_IDS); }
		if (array.isEmpty())
			throw new Exception(Strings.illegalInputFieldJSONArray + Constants.JSON_FIELD_STRING_IDS);
		
		List<String> out = new ArrayList<String>();
		for (int i = 0; i < array.size(); ++i)
			out.add(array.getString(i));

		return out;
	}

	@PUT
	@Path ("/seasons/{season-id}/sendtotranslation")
	@ApiOperation(value = "Send the specified strings to translation (ids in url or in body)", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response sendToTranslation(@PathParam("season-id")String season_id,
			@QueryParam("ids")List<String> ids,
			String body,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		AirlockChange change = new AirlockChange();
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("sendToTranslation request.");
		}

		String err = Utilities.validateLegalUUID(season_id);
		if (err!=null) {
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalSeasonUUID + err);
		}
		
		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, season_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);
		
		//check user authorization
		UserInfo userInfo = UserInfo.validate("TranslationServices.sendToTranslation", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);
		
		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.TRANSLATIONS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		
		
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.writeLock().lock();
		try {

			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);			

			@SuppressWarnings("unchecked")
			Map<String, OriginalString> originalStringsDB = (Map<String, OriginalString>)context.getAttribute(Constants.ORIG_STRINGS_DB_PARAM_NAME);

			Season season = seasonsDB.get(season_id);
			change.setSeason(season);
			
			//validate season
			if (season == null) {
				return sendAndLogError(Status.NOT_FOUND, Strings.seasonNotFound);				
			}
			
			//validate version. Only post 2.1 seasons support translation statuses
			ValidationResults validationRes = validateTranslationStatusesSupport(season);
			if (validationRes!=null) 
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(validationRes.error)).build();	

			try {
				ids = resolveIds(ids, body);
			}
			catch (Exception e)
			{
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();										
			}

			ValidationResults vr = validateStrings (ids, Constants.StringStatus.REVIEWED_FOR_TRANSLATION, originalStringsDB, "sendToTranslation", userInfo);
			if (vr!=null) {
				return sendAndLogError(vr.status, vr.error);
			}

			Stage changeStage = updateStringStatus(ids, Constants.StringStatus.IN_TRANSLATION, originalStringsDB);

			//need to write the original strings file since the status was changed
			try {
				change.getFiles().addAll(AirlockFilesWriter.writeOriginalStrings(season, context, changeStage));
				Webhooks.get(context).notifyChanges(change, context);
			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}
			
			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Send the strings: "  + Utilities.StringsListToString(ids) + " to translation." , userInfo);

			return (Response.ok("")).build();
		} finally {
			readWriteLock.writeLock().unlock();
		}		
	}			

	@PUT
	@Path ("/seasons/{season-id}/completereview")
	@ApiOperation(value = "Mark the specified strings as reviewed for translation (ids in url or in body)", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response completeReview(@PathParam("season-id")String season_id,
			@QueryParam("ids")List<String> ids,
			String body,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		AirlockChange change = new AirlockChange();
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("completeReview request.");
		}

		String err = Utilities.validateLegalUUID(season_id);
		if (err!=null) {
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalSeasonUUID + err);
		}
		
		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, season_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);
		
		//check user authorization
		UserInfo userInfo = UserInfo.validate("TranslationServices.completeReview", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);					

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.TRANSLATIONS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		
		
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.writeLock().lock();
		try {

			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);			

			@SuppressWarnings("unchecked")
			Map<String, OriginalString> originalStringsDB = (Map<String, OriginalString>)context.getAttribute(Constants.ORIG_STRINGS_DB_PARAM_NAME);

			Season season = seasonsDB.get(season_id);
			change.setSeason(season);
			
			//validate season
			if (season == null) {
				return sendAndLogError(Status.NOT_FOUND, Strings.seasonNotFound);				
			}
			
			//validate version. Only post 2.1 seasons support translation statuses
			ValidationResults validationRes = validateTranslationStatusesSupport(season);
			if (validationRes!=null) 
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(validationRes.error)).build();	

			try {
				ids = resolveIds(ids, body);
			}
			catch (Exception e)
			{
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();										
			}

			ValidationResults vr = validateStrings (ids, Constants.StringStatus.READY_FOR_TRANSLATION, originalStringsDB, "reviewedForTranslation", userInfo);
			if (vr!=null) {
				return sendAndLogError(vr.status, vr.error);
			}

			Stage changeStage = updateStringStatus(ids, Constants.StringStatus.REVIEWED_FOR_TRANSLATION, originalStringsDB);

			//need to write the original strings file since the status was changed
			try {
				change.getFiles().addAll(AirlockFilesWriter.writeOriginalStrings(season, context, changeStage));
				Webhooks.get(context).notifyChanges(change, context);
			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}

			return (Response.ok("")).build();
		} finally {
			readWriteLock.writeLock().unlock();
		}		
	}			
	@PUT
	@Path ("/seasons/copystrings/{dest-season-id}")
	@ApiOperation(value = "Copy the list of strings from season-id to dest-season-id", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response copyStrings(@QueryParam("ids")List<String> ids,
								      @PathParam("dest-season-id") String destSeasonId,
								      @ApiParam(value="VALIDATE or ACT")
								 	  @QueryParam("mode")String mode,
									  @QueryParam("overwrite")Boolean override,
									  @HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {

		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("copystrings request.");
		}
		AirlockChange change = new AirlockChange();
		String err = Utilities.validateLegalUUID(destSeasonId);
		if (err!=null) {
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalDestinationUUID + err);
		}

		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, destSeasonId);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);
		
		//check user authorization
		UserInfo userInfo = UserInfo.validate("TranslationServices.copyStrings", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);
		
		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.TRANSLATIONS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		
		
		if(override == null){
			override = false;
		}

		if (ids == null || ids.size() == 0) {
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON("The 'ids' parameter is missing.")).build();
		}

		//if not specified - mode = ACT (actually copy the strings)
		ActionType actionTypeObj  = Utilities.strToActionType(mode, ActionType.ACT);
		
		if (actionTypeObj==null) {
			return sendAndLogError(Status.BAD_REQUEST, String.format(Strings.illegalMode,mode)  + Constants.ActionType.returnValues());
		}

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.writeLock().lock();
		try {

			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			@SuppressWarnings("unchecked")
			Map<String, OriginalString> originalStringsDB = (Map<String, OriginalString>)context.getAttribute(Constants.ORIG_STRINGS_DB_PARAM_NAME);

			Season destSeason = seasonsDB.get(destSeasonId);
			change.setSeason(destSeason);
			
			//validate season
			if (destSeason == null) {
				return sendAndLogError(Status.NOT_FOUND, Strings.destinationNotFound);
			}			

			ValidationResults vr = validateStrings (ids, null, originalStringsDB, "",true, userInfo);
			if (vr!=null) {
				return sendAndLogError(vr.status, vr.error);
			}

			String sourceSeasonId = originalStringsDB.get(ids.get(0)).getSeasonId().toString();

			if(sourceSeasonId.equals(destSeasonId)) {//cannot copy within the same season
				return sendAndLogError(Status.BAD_REQUEST, Strings.cannotCopyStringWithinTheSameSeason);
			}

			List<OriginalString> originalStringList = new ArrayList<>();
			for(int i = 0; i<ids.size();++i) {
				String currId = ids.get(i);
				OriginalString copiedString = originalStringsDB.get(currId);
				originalStringList.add(copiedString);
			}
			Pair<Response, LinkedList<AirlockChangeContent>> writeRes = doCopyStrings(context,originalStringList,destSeason,destSeasonId,actionTypeObj,userInfo,true,override,false, false, null);
			change.getFiles().addAll(writeRes.getValue());
			Webhooks.get(context).notifyChanges(change, context);
			return writeRes.getKey();

		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	@PUT
	@Path ("/seasons/{season-id}/importstrings")
	@ApiOperation(value = "Import strings from Json to dest-season-id", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response importStrings(String importedStrings,
								@PathParam("season-id")String destSeasonId,
								@ApiParam(value="VALIDATE or ACT")@QueryParam("mode")String mode,
								@QueryParam("overwrite")Boolean override,
								@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {

		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("importstrings request.");
		}
		AirlockChange change = new AirlockChange();
		String err = Utilities.validateLegalUUID(destSeasonId);
		if (err!=null) {
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalDestinationUUID + err);
		}
		
		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, destSeasonId);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);
		
		//check user authorization
		UserInfo userInfo = UserInfo.validate("TranslationServices.importStrings", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		
		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.TRANSLATIONS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();					

		if(override == null){
			override = false;
		}

		//if not specified - mode = ACT (actually import the strings)
		ActionType actionTypeObj  = Utilities.strToActionType(mode, ActionType.ACT);
		
		if (actionTypeObj==null) {
			return sendAndLogError(Status.BAD_REQUEST, String.format(Strings.illegalMode,mode)  + Constants.ActionType.returnValues());
		}

		JSONArray importedStringsJSONArray = null;
		try {
			JSONObject importedJSON = new JSONObject(importedStrings);
			importedStringsJSONArray = importedJSON.getJSONArray("strings");
		} catch (JSONException je) {
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalInputJSON + je.getMessage());
		}
		
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.writeLock().lock();
		try {

			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>) context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			Season destSeason = seasonsDB.get(destSeasonId);
			change.setSeason(destSeason);
			
			//validate season
			if (destSeason == null) {
				return sendAndLogError(Status.NOT_FOUND, Strings.destinationNotFound);
			}
			List<OriginalString> originalStringList = new ArrayList<>();
			try {
				for (int i = 0; i < importedStringsJSONArray.size(); ++i) {
					OriginalString copiedString = new OriginalString();
					JSONObject newStringJson = importedStringsJSONArray.getJSONObject(i);
					ValidationResults validationRes = copiedString.validateOriginalStringJSONForImport(newStringJson, context, userInfo);
					if (validationRes!=null) {
						return sendAndLogError(validationRes.status, validationRes.error);
					}
					copiedString.fromJSON(newStringJson, destSeason);
					originalStringList.add(copiedString);
				}
			}
			catch (JSONException e) {
				return sendAndLogError(Status.NOT_FOUND, Strings.illegalStringJSON);
			}
			Pair<Response, LinkedList<AirlockChangeContent>> writeRes = doCopyStrings(context,originalStringList,destSeason, destSeasonId, actionTypeObj, userInfo,true,override,false, false, null);
			change.getFiles().addAll(writeRes.getValue());
			Webhooks.get(context).notifyChanges(change, context);
			return writeRes.getKey();
		}
		catch (Throwable e) {
			logger.log(Level.SEVERE, "Error importing strings: ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error importing strings: " + e.getMessage());
		}
		finally {
			readWriteLock.writeLock().unlock();
		}
	}

	@PUT
	@Path ("/seasons/{season-id}/importstringswithformat")
	@ApiOperation(value = "Import strings from zip to dest-season-id", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response importStringsWithFormat(File zipFile,
								  @PathParam("season-id")String destSeasonId,
								  @ApiParam(value="VALIDATE or ACT")
								  @QueryParam("mode")String mode,
								  @ApiParam(value="IOS or ANDROID")							   
								  @QueryParam("format")String format,
								  @QueryParam("overwrite")Boolean override,
								  @QueryParam("keepData")Boolean keepData,
								  @QueryParam("preserveFormat")boolean preserveFormat,
								  @HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {

		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("importstringswithFormat request.");
		}
		
		AirlockChange change = new AirlockChange();
		String err = Utilities.validateLegalUUID(destSeasonId);
		if (err!=null) {
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalDestinationUUID + err);
		}

		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, destSeasonId);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);
		//check user authorization
		UserInfo userInfo = UserInfo.validate("TranslationServices.importStringsWithFormat", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.TRANSLATIONS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		
		
		if(override == null){
			override = false;
		}
		if(keepData == null){
			keepData = false;
		}

		//if not specified - mode = ACT (actually import the strings)
		ActionType actionTypeObj  = Utilities.strToActionType(mode, ActionType.ACT);
		
		if (actionTypeObj==null) {
			return sendAndLogError(Status.BAD_REQUEST, String.format(Strings.illegalMode,mode)  + Constants.ActionType.returnValues());
		}

		Constants.InputFormat inputFormat = Utilities.strToInputFormat(format);
		if (inputFormat==null) {
			return sendAndLogError(Status.BAD_REQUEST, String.format(Strings.illegalFormat,format)  + Constants.InputFormat.returnValues());
		}

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.writeLock().lock();
		try {

			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>) context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);
			Season destSeason = seasonsDB.get(destSeasonId);
			change.setSeason(destSeason);
			//validate season
			if (destSeason == null) {
				return sendAndLogError(Status.NOT_FOUND, Strings.destinationNotFound);
			}
			HashMap<String, OriginalString> originalStringMap = new HashMap<>();
			File in = null;
			ImportExportUtilities ieUtil;
			try {
				FileInputStream fos = new FileInputStream(zipFile);
				String inputFolder = ImportExportUtilities.unzip(destSeason, fos, true);
				in = new File(inputFolder);
				if (!in.isDirectory())
					throw new Exception("missing input folder " + inputFolder);
				if (format.equals(Constants.InputFormat.ANDROID.toString())) {
					ieUtil = new ImportExportUtilitiesAndroid();
				}else {
					ieUtil = new ImportExportUtilitiesIOS();
				}
				Boolean hasDefault = ieUtil.doDefaultSubfolder(in, originalStringMap,keepData, preserveFormat);
				if (!hasDefault) {
					throw new Exception("missing default values folder");
				}
				for (File f : in.listFiles()) {
					ieUtil.doLocaleSubfolder(f, originalStringMap, destSeason.getOriginalStrings().getSupportedLanguages(),keepData, preserveFormat);
				}
			}
			catch (Exception e) {
				return sendAndLogError(Status.BAD_REQUEST, Strings.failedImport +e.getMessage());
			}
			finally {
				if(in != null) {
					try {
						FileUtils.deleteDirectory(in.getParentFile());
					} catch (IOException e) {
					}
				}
			}
		
			Pair<Response, LinkedList<AirlockChangeContent>> writeRes = doCopyStrings(context,new ArrayList<>(originalStringMap.values()),destSeason, destSeasonId, actionTypeObj, userInfo,true,override, true, preserveFormat, ieUtil);
			change.getFiles().addAll(writeRes.getValue());
			Webhooks.get(context).notifyChanges(change, context);
			return writeRes.getKey();
		}
		finally {
			readWriteLock.writeLock().unlock();
		}
	}

	public static Pair<Response, LinkedList<AirlockChangeContent>> doCopyStrings(ServletContext context , List<OriginalString> originalStrings, Season destSeason, String destSeasonId, Constants.ActionType mode, UserInfo userInfo, Boolean extendedConflict, Boolean override,Boolean fromZip, boolean preserveFormat, ImportExportUtilities ieUtil) throws JSONException{
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		List<OriginalString> newStrings = new ArrayList<>();
		List<ConflictingStrings> conflictingStrings = new ArrayList<>();
		List<OriginalString> nonConflictingStrings = new ArrayList<>();
		int maxStringsInJson = 100;
		checkConflicts(originalStrings,destSeason,conflictingStrings,newStrings,nonConflictingStrings,extendedConflict, preserveFormat, ieUtil);

		JSONObject res = new JSONObject();
		JSONArray addedStringsJson = new JSONArray();

		if (mode == Constants.ActionType.ACT) {
			//add the new one
			List<OriginalString> addedStrings;
			try {
				addedStrings = addCopiedString(context, newStrings, destSeasonId, destSeason, userInfo);
			} catch (Exception e) {
				String errMsg = Strings.JSONException;
				logger.severe(errMsg);
				return createResponseChangesPair(Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build(), changesArr);
			}
			for (int i = 0; i < addedStrings.size(); ++i) {
				addedStringsJson.add(addedStrings.get(i).toJson(StringsOutputMode.BASIC, destSeason));
			}

		}
		else {
			for (int i = 0; i < newStrings.size(); ++i) {
				addedStringsJson.add(newStrings.get(i).toJson(StringsOutputMode.BASIC, destSeason));
			}
		}
		//ignore the equal one
		if(override) {
			JSONArray stringsOverride = new JSONArray();
			boolean productionOverride = false;
			for (int i = 0; i < conflictingStrings.size(); ++i) {
				ConflictingStrings conflictingString = conflictingStrings.get(i);
				if(conflictingString.getDest().getStage().toString().equals("PRODUCTION")){
					productionOverride = true;
					if(conflictingString.getSource().getStage().toString().equals("DEVELOPMENT")){
						if(fromZip ){ // zip default to dev. Changing to prod
							conflictingString.getSource().setStage(Stage.PRODUCTION);
						}
						else {//can't override form prod to dev
							String errMsg = Strings.cannotOverrideProdDev;
							logger.severe(errMsg);
							return createResponseChangesPair(Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build(), changesArr);
						}
					}
				}
			}
			//only prod lead and admin can override prod strings
			if (productionOverride && !priviledgedUser(userInfo)) {
				String errMsg = Strings.prodCopyOverrideError;
				logger.severe(errMsg);
				return createResponseChangesPair(Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build(), changesArr);
			}
			boolean writeStrings = false;
			boolean writeEnglish = false;
			boolean writeProduction = false;
			List<String> modifiedLocales = new ArrayList<>();
			try {
				for (int i = 0; i < conflictingStrings.size(); ++i) {
					writeStrings = true;
					ConflictingStrings conflictingString = conflictingStrings.get(i);
					OriginalString sourceString = conflictingString.getSource();
					OriginalString destString = conflictingString.getDest();
					//no need to update stage
					//update value;
					if(destString.getStage().equals(Stage.PRODUCTION)){
						writeProduction = true;
					}
					if(!destString.getValue().equals(sourceString.getValue())) {
						writeEnglish = true;
						destString.setValue(sourceString.getValue());
					}
					//update international fallback
					destString.setInternationalFallback(sourceString.getInternationalFallback());
					//update translations

					copyTranslation(sourceString, destString, destSeason,modifiedLocales);
					/*if(destString.getStatus() != StringStatus.TRANSLATION_COMPLETE && destString.getStatus() != StringStatus.IN_TRANSLATION ){
						destString.setStatus(destString.getStatus());
					}*/
					stringsOverride.add(destString.toJson(StringsOutputMode.BASIC, destSeason));
				}
				if (writeStrings)
					changesArr.addAll(AirlockFilesWriter.writeOriginalStrings(destSeason, context, writeProduction? Stage.PRODUCTION : Stage.DEVELOPMENT));
				if (writeEnglish)
					changesArr.addAll(AirlockFilesWriter.writeEnStringsFiles(destSeason, context, writeProduction));
				for (String destinationLocale : modifiedLocales) {
					changesArr.addAll(AirlockFilesWriter.writeLocaleStringsFiles(destSeason, destinationLocale, context, writeProduction));
				}
			} catch (IOException e) {
				return createResponseChangesPair(Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build(), changesArr);
			}
			int overrideSize = stringsOverride.size();
			int addedSize = addedStringsJson.size();
			int nonConflictSize = nonConflictingStrings.size();
			if (overrideSize != 0 || addedSize != 0 || nonConflictSize != 0) {
				res.put(Constants.JSON_FIELD_STRING_OVERRIDE_SIZE, overrideSize);
				if(overrideSize > maxStringsInJson)
					res.put(Constants.JSON_FIELD_STRING_OVERRIDE, stringsOverride.subList(0,maxStringsInJson));
				else
					res.put(Constants.JSON_FIELD_STRING_OVERRIDE, stringsOverride);

				res.put(Constants.JSON_FIELD_STRING_ADDED_SIZE, addedSize);
				if(addedSize > maxStringsInJson)
					res.put(Constants.JSON_FIELD_STRING_ADDED, addedStringsJson.subList(0,maxStringsInJson));
				else
					res.put(Constants.JSON_FIELD_STRING_ADDED, addedStringsJson);

				res.put(Constants.JSON_FIELD_STRING_NON_CONFLICT_SIZE, nonConflictSize);
				if(nonConflictSize > maxStringsInJson)
					res.put(Constants.JSON_FIELD_STRING_NON_CONFLICT, nonConflictingStrings.subList(0,maxStringsInJson));
				else
					res.put(Constants.JSON_FIELD_STRING_NON_CONFLICT, nonConflictingStrings);
			}
		}
		else {
			//report the conflicting one
			JSONArray stringsInConflict = new JSONArray();
			for (int i = 0; i < conflictingStrings.size(); ++i) {
				ConflictingStrings conflictingString = conflictingStrings.get(i);
				JSONObject conflict = new JSONObject();
				conflict.put("key", conflictingString.getKey());
				String sourceValue = conflictingString.getSource().getValue();
				String destValue = conflictingString.getDest().getValue();
				if (!sourceValue.equals(destValue)) {
					conflict.put("sourceValue", sourceValue);
					conflict.put("destValue", destValue);
				}
				conflict.put("stage",conflictingString.getDest().getStage().toString());
				conflict.put("conflictingFields", conflictingString.getConflictingFields());
				stringsInConflict.add(conflict);
			}
			int conflictSize = stringsInConflict.size();
			int addedSize = addedStringsJson.size();
			int nonConflictSize = nonConflictingStrings.size();

			if (conflictSize != 0 || addedSize != 0 || nonConflictSize != 0) {
				res.put(Constants.JSON_FIELD_STRING_CONFLICTS_SIZE, conflictSize);
				if(conflictSize > maxStringsInJson)
					res.put(Constants.JSON_FIELD_STRING_CONFLICTS, stringsInConflict.subList(0,maxStringsInJson));
				else
					res.put(Constants.JSON_FIELD_STRING_CONFLICTS, stringsInConflict);

				res.put(Constants.JSON_FIELD_STRING_ADDED_SIZE, addedSize);
				if(addedSize > maxStringsInJson)
					res.put(Constants.JSON_FIELD_STRING_ADDED, addedStringsJson.subList(0,maxStringsInJson));
				else
					res.put(Constants.JSON_FIELD_STRING_ADDED, addedStringsJson);

				res.put(Constants.JSON_FIELD_STRING_NON_CONFLICT_SIZE, nonConflictSize);
				if(nonConflictSize > 100)
					res.put(Constants.JSON_FIELD_STRING_NON_CONFLICT, nonConflictingStrings.subList(0,100));
				else
					res.put(Constants.JSON_FIELD_STRING_NON_CONFLICT, nonConflictingStrings);
			}
		}
		return createResponseChangesPair((Response.ok(res.toString())).build(), changesArr);

	}

	public static List<OriginalString> addCopiedString(ServletContext context,List<OriginalString> newStrings, String destSeasonId,Season destSeason, UserInfo userInfo)throws JSONException,IOException{
		@SuppressWarnings("unchecked")
		Map<String, OriginalString> originalStringsDB = (Map<String, OriginalString>)context.getAttribute(Constants.ORIG_STRINGS_DB_PARAM_NAME);
		List<OriginalString> addedStrings = new ArrayList<>();
		List<String> addedStringsIds = new ArrayList<>();
		List<String> addedLocales = new ArrayList<>();
		AirlockChange change = new AirlockChange();
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, destSeasonId);
		change.setProduct(productErrorPair.product);
		change.setSeason(destSeason);
		
		for(int i =0; i<newStrings.size();++i) {
			OriginalString copyString = newStrings.get(i);
			JSONObject copyJson = copyString.toJson(StringsOutputMode.INCLUDE_TRANSLATIONS, destSeason);
			//remove unexpected fields
			copyJson.remove("uniqueId");
			copyJson.put("seasonId", destSeasonId);
			copyJson.put("stage","DEVELOPMENT");
			copyJson.remove("lastModified");
			copyJson.remove("lastSourceModification");
			copyJson.remove("translatorId");
			copyJson.remove("status");
			copyJson.remove("translations");
			Pair<Response, LinkedList<AirlockChangeContent>> writeRes = doAddString(context, destSeasonId, copyJson.toString(), false,true, userInfo); 
			Response r = writeRes.getKey();
			if(r.getStatus() != Status.OK.getStatusCode()){
				continue;
			}
			change.getFiles().addAll(writeRes.getValue());
			String newStringId = r.getEntity().toString();
			addedStringsIds.add(newStringId);
			OriginalString newString = originalStringsDB.get(newStringId);
			if(copyString.getTranslatorId() != null) {
				newString.setTranslatorId(new String(copyString.getTranslatorId()));
			}
			else {
				newString.setTranslatorId(null);
			}
			//deal with translation
			copyTranslation(copyString, newString, destSeason,addedLocales);
			if(newString.getStatus() != StringStatus.TRANSLATION_COMPLETE && newString.getStatus() != StringStatus.IN_TRANSLATION ){
				if(copyString.getStatus() == StringStatus.TRANSLATION_COMPLETE){
					newString.setStatus(StringStatus.IN_TRANSLATION);
				}
				else {
					newString.setStatus(copyString.getStatus());
				}
			}
			addedStrings.add(newString);
		}
		change.getFiles().addAll(writeStrings(destSeason,context,false,true).getValue());
		writeAddedStrings(context,destSeasonId,addedStringsIds,userInfo);
		for (String destinationLocale : addedLocales) {
				change.getFiles().addAll(AirlockFilesWriter.writeLocaleStringsFiles(destSeason, destinationLocale, context, false));
		}
		Webhooks.get(context).notifyChanges(change, context);
		return addedStrings;
	}


	/*//TODO ASK ALWAYS SUFFIX OR JUST CONFLICT
	public static String checkConflictsWithSuffix(String nameSuffix, List<OriginalString> originalStrings,Season destSeason,List<ConflictingStrings> conflictingStringsWithSuffix,List<OriginalString> newStrings,List<OriginalString> newStringsWithSuffix)throws JSONException {
		List<ConflictingStrings> conflictingStrings = new ArrayList<>();
		checkConflicts(originalStrings, destSeason, conflictingStrings, newStrings);
		if (conflictingStrings.size() != 0) {
			//check conflicts with suffix
			List conflictStringsList = new ArrayList();
			for (int i = 0; i < conflictingStrings.size(); ++i) {
				String newKey = conflictingStrings.get(i).getSource().getKey() + nameSuffix;
				OriginalString stringWithSuffix = new OriginalString();
				stringWithSuffix.fromJSON(conflictingStrings.get(i).getSource().toJson(StringsOutputMode.INCLUDE_TRANSLATIONS, destSeason), destSeason);
				stringWithSuffix.setKey(newKey);
				conflictStringsList.add(stringWithSuffix);
			}
			checkConflicts(conflictStringsList, destSeason, conflictingStringsWithSuffix, newStringsWithSuffix);
			//error
			if (conflictingStringsWithSuffix.size() != 0) {
				StringBuilder conflictingIds = new StringBuilder();
				for (int i = 0; i < conflictingStringsWithSuffix.size(); ++i) {
					String key = conflictingStringsWithSuffix.get(i).getSource().getKey();
					conflictingIds.append(key.substring(0, key.length() - nameSuffix.length()));
					conflictingIds.append(", ");
				}
				return "The following keys are in conflicts: " + conflictingIds.substring(0, conflictingIds.length() - 2);
			}
		}
		return null;
	}*/


	public static void checkConflicts( List<OriginalString> copiedStringList,Season destSeason,List<ConflictingStrings> conflictingStrings,List<OriginalString> newStrings,List<OriginalString> nonConflictStrings,Boolean extendedConflict, boolean preserveFormat, ImportExportUtilities ieUtil){
		//@SuppressWarnings("unchecked")
		OriginalStrings destStrings =  destSeason.getOriginalStrings();
		for(int i = 0; i<copiedStringList.size();++i) {
			OriginalString copiedString = copiedStringList.get(i);
			String key = copiedString.getKey();
			OriginalString destString = destStrings.getOriginalStringByKey(key);
			if(destString == null){
				newStrings.add(copiedString);
			}
			else if(extendedConflict){
				JSONArray conflictingFields = findConflictingKeys(copiedString,destString,destSeason, preserveFormat, ieUtil);
				if(conflictingFields.size() !=0) {
					ConflictingStrings conflict = new ConflictingStrings(copiedString, destString, key, conflictingFields);
					conflictingStrings.add(conflict);
				}
				else {
					nonConflictStrings.add(copiedString);
				}
			}
			else if (!copiedString.getValue().equals(destString.getValue())) {
				JSONArray conflictingFields = new JSONArray();
				JSONObject conflictingField = new JSONObject();
				try {
					conflictingField.put("fieldName", "value");
					conflictingField.put("sourceField", copiedString.getValue());
					conflictingField.put("destField", destString.getValue());
				}catch (JSONException e){

				}
				conflictingFields.add(conflictingField);
				ConflictingStrings conflict = new ConflictingStrings(copiedString, destString, key,conflictingFields);
				conflictingStrings.add(conflict);
			}
			else {
				nonConflictStrings.add(copiedString);
			}
		}
	}

	public static JSONArray findConflictingKeys(OriginalString copiedString,OriginalString destString,Season destSeason, boolean preserveFormat, ImportExportUtilities ieUtil){
		JSONArray conflictingKeys= new JSONArray();
		try {
			String altValue = (ieUtil!=null)?ieUtil.getFormattedString(copiedString.getValue(), false) : copiedString.getValue();
			String altDestValue = (ieUtil!=null)?ieUtil.getFormattedString(destString.getValue(), false) : destString.getValue();
		if(!(copiedString.getValue().equals(destString.getValue()) ||
				altValue.equals(altDestValue))) {
			JSONObject conflictingField = new JSONObject();
			conflictingField.put("fieldName", "value");
			conflictingField.put("sourceField", copiedString.getValue());
			conflictingField.put("destField", destString.getValue());
			conflictingKeys.add(conflictingField);
		}
		//stage is not conflict anymore
		/*if(!copiedString.getStage().equals(destString.getStage())){
			JSONObject conflictingField = new JSONObject();
			conflictingField.put("fieldName", "stage");
			conflictingField.put("sourceField", copiedString.getStage().toString());
			conflictingField.put("destField", destString.getStage().toString());
			conflictingKeys.add(conflictingField);
		}*/
		String altFallback = (ieUtil!=null)?ieUtil.getFormattedString(copiedString.getInternationalFallback(), false) : copiedString.getInternationalFallback();
		String altDestFallback = (ieUtil!=null)?ieUtil.getFormattedString(destString.getInternationalFallback(), false) : destString.getInternationalFallback();
		if(copiedString.getInternationalFallback() != null && 
				!copiedString.getInternationalFallback().equals(destString.getInternationalFallback()) &&
				!altFallback.equals(altDestFallback)){
			JSONObject conflictingField = new JSONObject();
			conflictingField.put("fieldName", "international fallback");
			conflictingField.put("sourceField", copiedString.getInternationalFallback());
			conflictingField.put("destField", destString.getInternationalFallback());
			conflictingKeys.add(conflictingField);
		}
		//TO DO: uncomment when auto translate is back
		/*if(!copiedString.getVariant().equals(destString.getVariant())) {
			JSONObject conflictingField = new JSONObject();
			conflictingField.put("fieldName", "variant");
			conflictingKeys.add(conflictingField);
		}*/
		List<String> conflictingLocales = isTranslationConflict(copiedString,destString,destSeason);
		if(conflictingLocales.size() != 0) {
			JSONObject conflictingField = new JSONObject();
			conflictingField.put("fieldName", "translations");
			conflictingField.put("sourceField", conflictingLocales);
			conflictingKeys.add(conflictingField);
		}
		}catch (JSONException e){

		}
		return conflictingKeys;
	}
	private static String iosAndroidLocaleMapping(String locale){
		switch (locale){
			case "zh_CN":
				return "zh_Hans";
			case "zh_Hans":
				return "zh_CN";
			case "zh_TW":
				return "zh_Hant";
			case "zh_Hant":
				return "zh_TW";
			case "iw":
				return "he";
			case "he":
				return "iw";
			case "in":
				return "id";
			case "id":
				return "in";
			default:
				return locale;

		}
	}

	private static List<String> isTranslationConflict(OriginalString copiedString,OriginalString destString,Season destSeason){
		List<String> conflictingLocales = new ArrayList<>();
		Set<String> copiedLocales = copiedString.getStringTranslations().getStringTranslationsMap().keySet();
		LinkedList<String> destLocales=  destSeason.getOriginalStrings().getSupportedLanguages();
		Iterator<String> iterator = copiedLocales.iterator();
		while(iterator.hasNext()){
			String locale = iterator.next();
			String mappedLocale = iosAndroidLocaleMapping(locale);
			String destinationLocale = null;
			if(destLocales.contains(locale)){
				destinationLocale = locale;
			}
			if(destLocales.contains(mappedLocale)){
				destinationLocale = mappedLocale;
			}
			if(destinationLocale != null){
				String copiedLocaleValue = null;
				if(copiedString.getStringTranslations().getStringTranslationsMap().containsKey(locale)) {
					copiedLocaleValue = copiedString.getStringTranslations().getStringTranslationsMap().get(locale).getTranslatedValue();
				}
				String destLocalValue = null;
				if(destString.getStringTranslations().getStringTranslationsMap().containsKey(destinationLocale)) {
					destLocalValue = destString.getStringTranslations().getStringTranslationsMap().get(destinationLocale).getTranslatedValue();
				}
				if(copiedLocaleValue != null && destLocalValue == null){
					conflictingLocales.add(destinationLocale);
				}
				if(copiedLocaleValue != null && destLocalValue != null) {
					if (!copiedLocaleValue.equals(destLocalValue)) {
						conflictingLocales.add(destinationLocale);
					}
				}
			}
		}
		return conflictingLocales;
	}


	private static void copyTranslation(OriginalString copiedString,OriginalString newString,Season destSeason,List<String> addedLocales) throws IOException{
		Set<String> copiedLocales = copiedString.getStringTranslations().getStringTranslationsMap().keySet();
		OriginalString.StringTranslations translations = copiedString.getStringTranslations();
		LinkedList<String> destLocales= destSeason.getOriginalStrings().getSupportedLanguages();
		Iterator<String> iterator = copiedLocales.iterator();
		while(iterator.hasNext()){
			String locale = iterator.next();
			String mappedLocale = iosAndroidLocaleMapping(locale);
			String destinationLocale = null;
			if(destLocales.contains(locale)){
				destinationLocale = locale;
			}
			if(destLocales.contains(mappedLocale)){
				destinationLocale = mappedLocale;
			}
			if(destinationLocale != null){
				OriginalString.StringTranslations.LocaleTranslationData sourceData = translations.getStringTranslationsMap().get(locale);
				if(sourceData != null) {
					String translatedValue = new String(sourceData.getTranslatedValue());
					if(newString.getStringTranslations().getStringTranslationsMap().get(destinationLocale) != null) {
						newString.getStringTranslations().getStringTranslationsMap().get(destinationLocale).setTranslationStatus(TranslationStatus.NOT_TRANSLATED);
					}
					if(sourceData.getTranslationStatus() != TranslationStatus.OVERRIDE){
						newString.setStatus(StringStatus.IN_TRANSLATION);
					}
					newString.addStringTranslationForLocale(destinationLocale, translatedValue, destLocales);
					newString.getStringTranslations().getStringTranslationsMap().get(destinationLocale).setTranslationStatus(sourceData.getTranslationStatus());
					newString.getStringTranslations().getStringTranslationsMap().get(destinationLocale).setNewTranslationAvailable(sourceData.getNewTranslationAvailable());
					addedLocales.add(destinationLocale);
				}
			}
		}
	}
	
	private ValidationResults validateStrings(List<String> ids, StringStatus stringStatus, Map<String, OriginalString> originalStringsDB, String requstedAction, UserInfo userInfo){
		return validateStrings(ids,stringStatus,originalStringsDB,requstedAction,false,userInfo);
	}
	//if stringStatus is null - dont check the status
	private ValidationResults validateStrings(List<String> ids, StringStatus stringStatus, Map<String, OriginalString> originalStringsDB, String requstedAction,Boolean skipProduction, UserInfo userInfo) {
		//validate string ids, existance and status
		for (String strId:ids) {
			String err = Utilities.validateLegalUUID(strId);
			if (err!=null) {
				return new ValidationResults(Strings.illegalStringUUID + err, Status.BAD_REQUEST);
			}

			OriginalString origStr = originalStringsDB.get(strId);
			if (origStr == null) {
				return new ValidationResults(Strings.stringNotFound, Status.BAD_REQUEST);
			}

			//if given status is null - dont check on string status
			if (stringStatus!= null && !origStr.getStatus().equals(stringStatus)) {
				String errMsg = String.format(Strings.stringNotInStatus,strId,origStr.getKey(),stringStatus.toString(),requstedAction);
				return new ValidationResults(errMsg, Status.BAD_REQUEST);
			}
			
			if (!skipProduction &&productionUnauthorized(origStr, userInfo)) {
				String errMsg = Strings.prodActionStringsError;
				return new ValidationResults(errMsg, Status.BAD_REQUEST);	
			} 
		}

		return null;
	}


	private Stage updateStringStatus(List<String> ids, StringStatus stringStatus, Map<String, OriginalString> originalStringsDB) {
		//validate string ids, existance and status
		Stage changeStage = Stage.DEVELOPMENT;
		for (String strId:ids) {
			OriginalString origStr = originalStringsDB.get(strId); //after validation - i know the string exists
			origStr.setStatus(stringStatus);
			if (origStr.getStage() == Stage.PRODUCTION) {
				changeStage = Stage.PRODUCTION;
			}
		}
		return changeStage;
	}

	
	private Stage updateStringFromStatusToStatus(StringStatus fromStatus, StringStatus toStatus, LinkedList<OriginalString> originalStringsList) {
		Stage toRet = Stage.DEVELOPMENT;
		//validate string ids, existance and status
		for (OriginalString origStr:originalStringsList) {
			if (origStr.getStatus() == fromStatus)
				origStr.setStatus(toStatus);
			if (origStr.getStage() == Stage.PRODUCTION)
				toRet = Stage.PRODUCTION;
		}
		return toRet;
	}


	@GET
	@Path ("/seasons/{season-id}/translate/summary")
	@ApiOperation(value = "Return the status, key, value, and ID for each specified string, and the status and translation for each specified locale", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getTranslationSummary(@PathParam("season-id")String season_id,
			@ApiParam(value="When not specified, all strings are returned", allowMultiple=true)@QueryParam("ids")List<String> ids, 
			@ApiParam(value="When not specified, all locales are returned", allowMultiple=true)@QueryParam("locales")List<String> locales, 
			@ApiParam(value="When not specified, do not return translations")@QueryParam("showtranslations")Boolean showTranslations, 
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getTranslationSummary request");
		}

		String err = Utilities.validateLegalUUID(season_id);
		if (err!=null) {
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalSeasonUUID + err);
		}

		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, season_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;

		//check user authorization
		UserInfo userInfo = UserInfo.validate("TranslationServices.getTranslationSummary", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.TRANSLATIONS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		
		
		if (showTranslations == null)
			showTranslations = false;
		
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.readLock().lock();
		try {

			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);			

			@SuppressWarnings("unchecked")
			Map<String, OriginalString> originalStringsDB = (Map<String, OriginalString>)context.getAttribute(Constants.ORIG_STRINGS_DB_PARAM_NAME);

			Season season = seasonsDB.get(season_id);

			if (season == null) {
				return sendAndLogError(Status.NOT_FOUND, Strings.seasonNotFound);				
			}

			//validate version. Only post 2.1 seasons support translation statuses
			ValidationResults validationRes = validateTranslationStatusesSupport(season);
			if (validationRes!=null) 
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(validationRes.error)).build();	

			//validate ids
			if (ids != null) {
				ValidationResults vr = validateStrings(ids, null, originalStringsDB, null, null);
				if (vr!=null) {
					return sendAndLogError(vr.status, vr.error);
				}
			}

			//validate locales
			if (locales!=null) {
				ValidationResults vr = validateLocales(locales, season);
				if (vr!=null) {
					return sendAndLogError(vr.status, vr.error);
				}
			}
				
			JSONObject res = season.getOriginalStrings().getTranslationStatusSummary(ids, locales, showTranslations, originalStringsDB);

			return (Response.ok()).entity(res.toString()).build();
		} finally {
			readWriteLock.readLock().unlock();
		}
	}


	private ValidationResults validateLocales(List<String> locales, Season season) {
		for (String locale:locales) {
			if (!season.getOriginalStrings().isLanguageSupported(locale)) {		
				return new ValidationResults(Strings.translationDoesNotExist, Status.BAD_REQUEST);
			}
		}
		return null;
	}

	@GET
	@Path ("/seasons/{season-id}/stringsfortranslation")
	@ApiOperation(value = "Returns the strings in translation format for the specified season ", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getStringsForTranslation(@PathParam("season-id")String season_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getStrings request");
		}
		
		String err = Utilities.validateLegalUUID(season_id);
		if (err!=null) {
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalSeasonUUID + err);
		}

		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, season_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;

		//check user authorization
		UserInfo userInfo = UserInfo.validate("TranslationServices.getStringsForTranslation", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);
		
		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.TRANSLATIONS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		
		
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.readLock().lock();
		try {
						
			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);			
	
			Season season = seasonsDB.get(season_id);
	
			if (season == null) {
				return sendAndLogError(Status.NOT_FOUND, Strings.seasonNotFound);				
			}
	
			//validate that pre 2.1 season is upgraded			
			ValidationResults upgradedRes = validatePre21SeasonsIsUpgraded(season);
			if (upgradedRes!=null)
				return Response.status(upgradedRes.status).entity(Utilities.errorMsgToErrorJSON(upgradedRes.error)).build();

			String stringsForTranslations = season.getOriginalStrings().toStringsTranslationFormat();
			
			
			return (Response.ok()).entity(stringsForTranslations).build();
		} finally {
			readWriteLock.readLock().unlock();
		}
	}	
		
	private ValidationResults validateTranslationStatusesSupport (Season season) {
		
		//validate that pre 2.1 season is upgraded			
		ValidationResults upgradedRes = validatePre21SeasonsIsUpgraded(season);
		if (upgradedRes!=null)
			return upgradedRes;						

		Environment env = new Environment();
		env.setServerVersion(season.getServerVersion()); 			
		
		if (isTranslationStatusesSupport(env))  //only post 2.1 seasons stranslation statuses
			return null;
		
		String errMsg =Strings.translationStatusNotSupported;
		logger.severe(errMsg);
		return new ValidationResults(errMsg, Status.BAD_REQUEST);
	}

	public static boolean isTranslationStatusesSupport (Environment env) {
		Version version = env.getVersion();
		return version.i >= Version.v2_5.i;  //only post 2.1 seasons support translation statuses			
	}
	static boolean productionUnauthorized(OriginalString string, UserInfo userInfo)
	{
		return string.getStage().equals(Stage.PRODUCTION) && !priviledgedUser(userInfo);
	}
	static boolean priviledgedUser(UserInfo userInfo)
	{
		return userInfo == null || userInfo.getRoles().contains(RoleType.ProductLead) || 
		   userInfo.getRoles().contains(RoleType.Administrator) || userInfo.getRoles().contains(RoleType.TranslationSpecialist);
	}

	@GET
	@Path ("/seasons/{season-id}/smartlingstatus")
	@ApiOperation(value = "compares season locles to smartling locales", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getSmartlingStatus(@PathParam("season-id")String season_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException
	{
		if (logger.isLoggable(Level.FINEST))
			logger.finest("getSmartlingStatus request");

		String err = Utilities.validateLegalUUID(season_id);
		if (err!=null) {
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalSeasonUUID + err);
		}
		
		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, season_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		
		//check user authorization
		UserInfo userInfo = UserInfo.validate("TranslationServices.getSmartlingStatus", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		BackgroundTranslator smartling = (BackgroundTranslator) context.getAttribute(Constants.BACKGROUND_TRANSLATOR);
		if (smartling == null)
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(Strings.translationNotRunning)).build();

		String smartlingProject;
		Set<String> seasonLocales;

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.TRANSLATIONS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		
		
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.readLock().lock();

		try {
			smartlingProject = getSmartlingProject(season_id);
			seasonLocales = getSeasonLocales(season_id);
		}
		catch (Exception e) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(e.toString())).build();
		}
		finally {
			readWriteLock.readLock().unlock();
		}

		seasonLocales.remove(Constants.DEFAULT_LANGUAGE);
		Set<String> smartlingLocales;

		try {
			smartlingLocales = smartling.getSmartlingLocales(smartlingProject);
		}
		catch (Exception e) {
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON("Can't get Smartling locales: " + e.toString())).build();
		}

		Set<String> mappedLocales = smartling.seasonLocales2SmartlingLocales(seasonLocales);

		JSONObject out = new JSONObject();
		out.put("smartlingLocales", new JSONArray(smartlingLocales));
		out.put("seasonLocales", new JSONArray(seasonLocales));
		out.put("onlyInSmartling", getDiff(smartlingLocales, mappedLocales));
		out.put("onlyInSeason", getDiff(mappedLocales, smartlingLocales));

		return (Response.ok()).entity(out.toString()).build();
	}

	Set<String> getDiff(Set<String> one, Set<String> two)
	{
		Set<String> out = new TreeSet<String>(one);
		out.removeAll(two);
		return out;
	}

	@SuppressWarnings("unchecked")
	String getSmartlingProject(String season_id) throws Exception
	{
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

		Season season = seasonsDB.get(season_id);
		if (season == null)
			throw new Exception(Strings.seasonNotFound);

		String productId = season.getProductId().toString();
		Product product = productsDB.get(productId);
		String project = (product == null) ? null : product.getSmartlingProjectId();
		if (project == null || project.isEmpty())
			throw new Exception(Strings.smartlingNotConfigured + season_id);

		return project;
	}
	Set<String> getSeasonLocales(String season_id) throws Exception
	{
		@SuppressWarnings("unchecked")
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);
		Season season = seasonsDB.get(season_id);
		if (season == null)
			throw new Exception(Strings.seasonNotFound);

		return new TreeSet<String>(season.getOriginalStrings().getSupportedLanguages());
	}

	@GET
	@Path ("/seasons/{season-id}/issue/{issue-id}")
	@ApiOperation(value = "Returns the issue for the specified issue id ", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getIssue(@PathParam("season-id")String season_id,
			@PathParam("issue-id")String issue_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException
	{
		if (logger.isLoggable(Level.FINEST))
			logger.finest("getIssue request");

		String err = Utilities.validateLegalUUID(season_id);
		if (err!=null) {
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalSeasonUUID + err);
		}
		
		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, season_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		
		//check user authorization
		UserInfo userInfo = UserInfo.validate("TranslationServices.getIssue", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		BackgroundTranslator smartling = (BackgroundTranslator) context.getAttribute(Constants.BACKGROUND_TRANSLATOR);
		if (smartling == null)
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(Strings.translationNotRunning)).build();

		String smartlingProject;
		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.TRANSLATIONS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		
		
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.readLock().lock();

		try {
			smartlingProject = getSmartlingProject(season_id);
		}
		catch (Exception e) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(e.toString())).build();
		}
		finally {
			readWriteLock.readLock().unlock();
		}

		try {
			JSONObject json = smartling.getIssue(smartlingProject, issue_id);
			return (Response.ok()).entity(json.toString()).build();
		}
		catch (Exception e)
		{
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(e.toString())).build();
		}
	}
	
	@GET
	@Path ("/seasons/strings/{string-id}/locale/{locale}/issues")
	@ApiOperation(value = "Returns the issues for the specified string and locale", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getStringIssues(@PathParam("string-id")String string_id,
			@PathParam("locale")String locale,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException
	{
		if (logger.isLoggable(Level.FINEST))
			logger.finest("getStringIssues request");

		String err = Utilities.validateLegalUUID(string_id);
		if (err!=null)
			return sendAndLogError(Strings.illegalStringUUID + err);

		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfString(context, string_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;

		//check user authorization
		UserInfo userInfo = UserInfo.validate("TranslationServices.getStringIssues", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		BackgroundTranslator smartling = (BackgroundTranslator) context.getAttribute(Constants.BACKGROUND_TRANSLATOR);
		if (smartling == null)
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(Strings.translationNotRunning)).build();

		Season season;
		OriginalString originalString;
		String seasonId, smartlingProject, smartlingHash, localeId;

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.TRANSLATIONS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();	
				
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.readLock().lock();
		try {

			@SuppressWarnings("unchecked")
			Map<String, OriginalString> originalStringsDB = (Map<String, OriginalString>)context.getAttribute(Constants.ORIG_STRINGS_DB_PARAM_NAME);

			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);			

			originalString = originalStringsDB.get(string_id);
			if (originalString == null)
				return sendAndLogError(Strings.stringNotFound);

			smartlingHash = originalString.getTranslatorId();
			if (smartlingHash == null || smartlingHash.isEmpty())
				return sendAndLogError(Strings.stringNotSubmitted);

			seasonId = originalString.getSeasonId().toString();
			season = seasonsDB.get(seasonId);

			//validate version. Only post 2.1 seasons support translation statuses
			ValidationResults validationRes = validateTranslationStatusesSupport(season);
			if (validationRes != null) 
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(validationRes.error)).build();	

			//validate locale
			boolean localeExists = season.getOriginalStrings().isLanguageSupported(locale);
			if (!localeExists)
				return sendAndLogError(Strings.localNotSupported);

			// TODO: pull SOURCE issues from Smartling in the background & not just push TRANSLATION issues to Smartling.
			if (locale.equals(Constants.DEFAULT_LANGUAGE))
				localeId = null; // source issues must not specify a locale
			else
			{
				localeId = SmartlingLocales.get(locale);
				if (localeId == null)
					return sendAndLogError(String.format(Strings.localeNotMapped,locale));
			}
			//Do we need this?
			//if (productionUnauthorized(originalString, userInfo))
			//	return sendError("Unable to create issue. Only a user with the Administrator or Product Lead role can create issues for strings in the production stage.");

			try {
				smartlingProject = getSmartlingProject(seasonId);
			}
			catch (Exception e) {
				return sendAndLogError(e.toString());
			}
		}
		finally
		{
			readWriteLock.readLock().unlock();
		}

		try {
			JSONObject json = smartling.getStringIssues(smartlingProject, smartlingHash, locale);
			return (Response.ok()).entity(json.toString()).build();
		}
		catch (Exception e)
		{
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(e.toString())).build();
		}
	}
	
	@PUT
	@Path ("/seasons/strings/{string-id}/locale/{locale}/updateissuestatus")
	@ApiOperation(value = "Returns the status on issues of the specified string and locale", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response updateIssueStatus(@PathParam("string-id")String string_id,
			@PathParam("locale")String locale,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException
	{
		if (logger.isLoggable(Level.FINEST))
			logger.finest("getIssueStatus request");
		AirlockChange change = new AirlockChange();
		String err = Utilities.validateLegalUUID(string_id);
		if (err!=null)
			return sendAndLogError(Strings.illegalStringUUID + err);

		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfString(context, string_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);
		
		//check user authorization
		UserInfo userInfo = UserInfo.validate("TranslationServices.updateIssueStatus", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		BackgroundTranslator smartling = (BackgroundTranslator) context.getAttribute(Constants.BACKGROUND_TRANSLATOR);
		if (smartling == null)
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(Strings.translationNotRunning)).build();

		Season season;
		OriginalString originalString;
		String seasonId, smartlingProject, smartlingHash, localeId;

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.TRANSLATIONS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();	
				
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.readLock().lock();
		try {

			@SuppressWarnings("unchecked")
			Map<String, OriginalString> originalStringsDB = (Map<String, OriginalString>)context.getAttribute(Constants.ORIG_STRINGS_DB_PARAM_NAME);

			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);			

			originalString = originalStringsDB.get(string_id);
			if (originalString == null)
				return sendAndLogError(Strings.stringNotFound);

			smartlingHash = originalString.getTranslatorId();
			if (smartlingHash == null || smartlingHash.isEmpty())
				return sendAndLogError(Strings.stringNotSubmitted);

			seasonId = originalString.getSeasonId().toString();
			season = seasonsDB.get(seasonId);
			change.setSeason(season);
			
			//validate version. Only post 2.1 seasons support translation statuses
			ValidationResults validationRes = validateTranslationStatusesSupport(season);
			if (validationRes != null) 
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(validationRes.error)).build();	

			//validate locale
			boolean localeExists = season.getOriginalStrings().isLanguageSupported(locale);
			if (!localeExists)
				return sendAndLogError(Strings.localNotSupported);

			// TODO: pull SOURCE issues from Smartling in the background & not just push TRANSLATION issues to Smartling.
			if (locale.equals(Constants.DEFAULT_LANGUAGE))
				localeId = null; // source issues must not specify a locale
			else
			{
				localeId = SmartlingLocales.get(locale);
				if (localeId == null)
					return sendAndLogError(String.format(Strings.localeNotMapped,locale));
			}
			//Do we need this?
			//if (productionUnauthorized(originalString, userInfo))
			//	return sendError("Unable to create issue. Only a user with the Administrator or Product Lead role can create issues for strings in the production stage.");

			try {
				smartlingProject = getSmartlingProject(seasonId);
			}
			catch (Exception e) {
				return sendAndLogError(e.toString());
			}
		}
		finally
		{
			readWriteLock.readLock().unlock();
		}

		IssueStatus status;
		try {
			String str = smartling.getIssueStatus(smartlingProject, smartlingHash, locale);
			status = IssueStatus.valueOf(str);
		}
		catch (Exception e)
		{
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(e.toString())).build();
		}
		readWriteLock.writeLock().lock();
		try {
			if (!season.isPurged())
			{
				if (localeId == null) // a SOURCE issue
					originalString.setIssueStatus(status);
				else
					originalString.getStringTranslations().getTranslationDataPerLocale(locale).setIssueStatus(status);

				change.getFiles().addAll(AirlockFilesWriter.writeOriginalStrings(season, context, originalString.getStage()));
				Webhooks.get(context).notifyChanges(change, context);
			}
		}
		catch (Exception e)
		{
			return sendAndLogError(Strings.cannotUpdateIssue + e.toString());
		}
		finally {
			readWriteLock.writeLock().unlock();
		}

		JSONObject json = new JSONObject();
		json.put("status", status.toString());
		return (Response.ok()).entity(json.toString()).build();
	}


	@POST
	//@Path ("/seasons/{season-id}/issue")
	@Path ("/seasons/{string-id}/issues/{locale}") 
	@ApiOperation(value = "Create a new translation issue.", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 500, message = "Internal error") })
	
	public Response createIssue(@PathParam("string-id")String string_id, @PathParam("locale")String locale, 
			String issue_body,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException
	{
		if (logger.isLoggable(Level.FINEST))
			logger.finest("createIssue request");
		AirlockChange change = new AirlockChange();
		String err = Utilities.validateLegalUUID(string_id);
		if (err != null)
			return sendAndLogError(Strings.illegalStringUUID + err);

		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfString(context, string_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);
		
		//check user authorization
		UserInfo userInfo = UserInfo.validate("TranslationServices.createIssue", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		BackgroundTranslator smartling = (BackgroundTranslator) context.getAttribute(Constants.BACKGROUND_TRANSLATOR);
		if (smartling == null)
			return sendAndLogError(Strings.translationNotRunning);

		
		Season season;
		OriginalString originalString;
		String seasonId, smartlingProject, smartlingHash, localeId;

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.TRANSLATIONS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();	
				
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.readLock().lock();
		try {

			@SuppressWarnings("unchecked")
			Map<String, OriginalString> originalStringsDB = (Map<String, OriginalString>)context.getAttribute(Constants.ORIG_STRINGS_DB_PARAM_NAME);

			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);			

			originalString = originalStringsDB.get(string_id);
			if (originalString == null)
				return sendAndLogError(Strings.stringNotFound);

			smartlingHash = originalString.getTranslatorId();
			if (smartlingHash == null || smartlingHash.isEmpty())
				return sendAndLogError(Strings.stringNotSubmitted);

			seasonId = originalString.getSeasonId().toString();
			season = seasonsDB.get(seasonId);
			change.setSeason(season);
			
			//validate version. Only post 2.1 seasons support translation statuses
			ValidationResults validationRes = validateTranslationStatusesSupport(season);
			if (validationRes != null) 
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(validationRes.error)).build();	

			//validate locale
			boolean localeExists = season.getOriginalStrings().isLanguageSupported(locale);
			if (!localeExists)
				return sendAndLogError(Strings.localNotSupported);

			// TODO: pull SOURCE issues from Smartling in the background & not just push TRANSLATION issues to Smartling.
			if (locale.equals(Constants.DEFAULT_LANGUAGE))
				localeId = null; // source issues must not specify a locale
			else
			{
				localeId = SmartlingLocales.get(locale);
				if (localeId == null)
					return sendAndLogError(String.format(Strings.localeNotMapped,locale));
			}
			//Do we need this?
			//if (productionUnauthorized(originalString, userInfo))
			//	return sendError("Unable to create issue. Only a user with the Administrator or Product Lead role can create issues for strings in the production stage.");

			try {
				smartlingProject = getSmartlingProject(seasonId);
			}
			catch (Exception e) {
				return sendAndLogError(e.toString());
			}
		}
		finally
		{
			readWriteLock.readLock().unlock();
		}

		String issueId = null;
		try {
			JSONObject issue = createIssueJSON(issue_body, smartlingHash, localeId);
			issueId = smartling.createIssue(smartlingProject, issue);
		}
		catch (Exception e)
		{
			return sendAndLogError(e.toString());
		}

		readWriteLock.writeLock().lock();
		try {
			if (!season.isPurged())
			{
				if (localeId == null) // a SOURCE issue
					originalString.setIssueStatus(IssueStatus.HAS_OPEN_ISSUES);
				else
					originalString.getStringTranslations().getTranslationDataPerLocale(locale).setIssueStatus(IssueStatus.HAS_OPEN_ISSUES);

				change.getFiles().addAll(AirlockFilesWriter.writeOriginalStrings(season, context, originalString.getStage()));
				Webhooks.get(context).notifyChanges(change, context);
			}
		}
		catch (Exception e)
		{
			return sendAndLogError(Strings.cannotAddIssue + e.toString());
		}
		finally {
			readWriteLock.writeLock().unlock();
		}

		JSONObject json = new JSONObject();
		json.put(Constants.JSON_FIELD_UNIQUE_ID, issueId);
		return (Response.ok()).entity(json.toString()).build();
	}

	@PUT
	@Path ("/seasons/{season-id}/issue/{issue-id}")
	@ApiOperation(value = "Update a translation issue.", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 500, message = "Internal error") })
	
	public Response updateIssue(@PathParam("season-id")String season_id,
			@PathParam("issue-id")String issue_id,
			String body,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException
	{
		if (logger.isLoggable(Level.FINEST))
			logger.finest("updateIssue request");
		String err = Utilities.validateLegalUUID(season_id);
		if (err!=null) {
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalSeasonUUID + err);
		}
		
		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, season_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		
		//check user authorization
		UserInfo userInfo = UserInfo.validate("TranslationServices.updateIssue", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		BackgroundTranslator smartling = (BackgroundTranslator) context.getAttribute(Constants.BACKGROUND_TRANSLATOR);
		if (smartling == null)
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(Strings.translationNotRunning)).build();

		String smartlingProject;

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.TRANSLATIONS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		
		
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.readLock().lock();

		try {
			smartlingProject = getSmartlingProject(season_id);
		}
		catch (Exception e) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(e.toString())).build();
		}
		finally {
			readWriteLock.readLock().unlock();
		}

		try {
			smartling.updateIssue(smartlingProject, issue_id, body);
			// TODO: we may need to update the IssueStatus, but it's not certain all issues on this string are closed.
			// perhaps the console need to put a non-commital "MODIFIED" status until the status is refreshed in the background
			return (Response.ok()).build();
		}
		catch (Exception e)
		{
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(e.toString())).build();
		}
	}
	
	JSONObject createIssueJSON(String body, String hash, String localeId) throws Exception
	{
		JSONObject obj;
		try {
			obj = new JSONObject(body);
		}
		catch (Exception e) {
			throw new Exception("Body is not a valid JSON");
		}

		checkString(obj, "issueText");
		String issueSubTypeCode = checkString(obj, "issueSubTypeCode");
		String issueTypeCode = checkString(obj, "issueTypeCode");

		IssueType issueType = IssueType.valueOf(issueTypeCode);
		if (issueType == null)
			throw new Exception (Strings.illegalIssueType + issueType);
		
		IssueSubType subtype = IssueSubType.valueOf(issueSubTypeCode);
		if (subtype == null)
			throw new Exception (Strings.illegalIssueSubType + issueSubTypeCode);

		boolean sourceIssue = (issueType == IssueType.SOURCE);
		boolean sourceSubIssue = (subtype == IssueSubType.CLARIFICATION || subtype == IssueSubType.MISSPELLING);
		if (sourceIssue != sourceSubIssue)
			throw new Exception (Strings.illegalIssueTypeAndSubtype+ issueTypeCode + "/" + issueSubTypeCode);

		if (issueType == IssueType.SOURCE && localeId != null)
			throw new Exception (Strings.issueWithLocal );

		JSONObject string = new JSONObject();
		string.put("hashcode", hash);
		if (localeId != null)
			string.put("localeId", localeId);
		obj.put("string", string);
		return obj;
	}
	String checkString(JSONObject obj, String key) throws Exception
	{
		String val = obj.optString(key);
		if (val == null || val.isEmpty())
			throw new Exception(Strings.missingKeyJSON + key);
		return val;
	}
	Response sendAndLogError(String error)
	{
		return sendAndLogError(Status.BAD_REQUEST, error);
	}
	Response sendAndLogError(Status status, String error)
	{
		logger.severe(error);
		return Response.status(status).entity(Utilities.errorMsgToErrorJSON(error)).build();
	}
	Response sendInfoError(Status status, UserInfo info)
	{
		return Response.status(status).entity(info.getErrorJson()).build();
	}
}
