package com.ibm.airlock;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponses;
import com.wordnik.swagger.annotations.ApiResponse;
import com.ibm.airlock.Constants.AirlockCapability;
import com.ibm.airlock.Constants.GetAnalyticsOutputMode;
import com.ibm.airlock.Constants.InputSampleGenerationMode;
import com.ibm.airlock.Constants.OutputJSONMode;
import com.ibm.airlock.Constants.RoleType;
import com.ibm.airlock.Constants.Stage;
import com.ibm.airlock.admin.BaseAirlockItem;
import com.ibm.airlock.admin.Branch;
import com.ibm.airlock.admin.ConfigurationRuleItem;
import com.ibm.airlock.admin.Season;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.Utilities.ProductErrorPair;
import com.ibm.airlock.admin.ValidationResults;
import com.ibm.airlock.admin.BaseAirlockItem.Type;
import com.ibm.airlock.admin.MergeBranch.MergeException;
import com.ibm.airlock.admin.DataAirlockItem;
import com.ibm.airlock.admin.FeatureItem;
import com.ibm.airlock.admin.GenerationException;
import com.ibm.airlock.admin.Product;
import com.ibm.airlock.admin.analytics.AirlockAnalytics;
import com.ibm.airlock.admin.analytics.AnalyticsDataCollection;
import com.ibm.airlock.admin.analytics.AnalyticsUtilities;
import com.ibm.airlock.admin.analytics.Experiment;
import com.ibm.airlock.admin.analytics.Variant;
import com.ibm.airlock.admin.authentication.UserInfo;
import com.ibm.airlock.admin.operations.AirlockChange;
import com.ibm.airlock.admin.operations.AirlockChangeContent;
import com.ibm.airlock.admin.operations.Webhooks;
import com.ibm.airlock.admin.serialize.AirlockFilesWriter;
import com.ibm.airlock.admin.serialize.AuditLogWriter;
import com.ibm.airlock.engine.Environment;
import com.ibm.airlock.engine.Version;

@Path ("/analytics")
@Api(value = "/analytics", description = "Analytics management API")
public class AnalyticsServices {

	public static final Logger logger = Logger.getLogger(AnalyticsServices.class.getName());


	@Context
	private ServletContext context;

	@GET
	@Path("/globalDataCollection/{season-id}/branches/{branch-id}")
	@ApiOperation(value = "Returns the global data collection of the given season", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),			
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getGlobalDataCollection(@PathParam("season-id")String season_id,
			@PathParam("branch-id")String branch_id,
			@ApiParam(value="BASIC, VERBOSE or DISPLAY")@QueryParam("mode") String mode,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getGlobalDataCollection request");
		}

		String err = Utilities.validateLegalUUID(season_id);
		if (err!=null) {
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalSeasonUUID + err);
		}

		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, branch_id, season_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;

		// use userInfo for more stringent checks. null if authorization is off
		UserInfo userInfo = UserInfo.validate("AnalyticsServices.getGlobalDataCollection", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		GetAnalyticsOutputMode outputMode = GetAnalyticsOutputMode.BASIC; //if mode not specified - return basic
		if (mode != null) {
			outputMode = Utilities.strToGetAnalyticsOutputMode(mode);
			if (outputMode==null) {
				return sendAndLogError(Status.BAD_REQUEST, String.format(Strings.illegalMode,mode) + Constants.GetAnalyticsOutputMode.returnValues());	
			}
		}

		//capability verification
		AirlockCapability[] requiredCapabilities = getRequiredCapabilities(branch_id); //ANALYTICS + maybe BRANCHES
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, requiredCapabilities);
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();			
		readWriteLock.readLock().lock(); 
		try {
			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);			

			Season season = seasonsDB.get(season_id);

			JSONObject res = new JSONObject();
			if (season == null) {
				return sendAndLogError(Status.NOT_FOUND, Strings.seasonNotFound);
			}

			//verify branchId
			ValidationResults validationRes = Utilities.validateBranchId(context, branch_id, season);
			if (validationRes!=null) {
				return sendAndLogError(validationRes.status, validationRes.error);
			}
			//boolean inMaster = branch_id.equals(Constants.MASTER_BRANCH_NAME);

			//validate version. Only post 2.1 seasons support analytics
			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());
			env.setBranchId(branch_id);
			validationRes = validateAnalyticsSupport(env, season);
			if (validationRes!=null) 
				return sendAndLogError(validationRes.status, validationRes.error);

			Map<String, BaseAirlockItem> airlockItemsDB = Utilities.getAirlockItemsDB (branch_id, context);			
			AirlockAnalytics airlockAnalytics = Utilities.getAirlockAnalytics(season, branch_id, context, airlockItemsDB, env);

			if (outputMode.equals(GetAnalyticsOutputMode.DISPLAY)) {
				res = airlockAnalytics.getGlobalDataCollection().toDisplayJson(context, season, env, airlockItemsDB);
			}else { //basic or verbose
				res = airlockAnalytics.getGlobalDataCollection().toJson(outputMode.equals(GetAnalyticsOutputMode.VERBOSE), context, season, true, airlockItemsDB, env);
			}

			return (Response.ok()).entity(res.toString()).build();
		} catch (MergeException e) {
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, Strings.mergeException  + e.getMessage());
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error getting global data collection for season " + season_id + " branch " + branch_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error getting analytics: " + e.getMessage());
		}  
		finally {
			readWriteLock.readLock().unlock();
		}
	}		

	@PUT
	@Path("/globalDataCollection/{season-id}/branches/{branch-id}")
	@ApiOperation(value = "Updates global data collection of the given season", response = String.class)
	@Produces(value="text/plain")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 404, message = "Season not found"),						
			@ApiResponse(code = 500, message = "Internal error") })
	public Response updateGlobalDataCollection(@PathParam("season-id")String season_id,
			@PathParam("branch-id")String branch_id,
			String globalDataCollection, 
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {

		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("updateGlobalDataCollection request, season_id = " + season_id + ", branch_id = " + branch_id + ", globalDataCollection = " +  globalDataCollection);
		}
		AirlockChange change = new AirlockChange();
		String err = Utilities.validateLegalUUID(season_id);
		if (err!=null) {
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalSeasonUUID + err);
		}

		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, branch_id, season_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);

		//check user authorization
		UserInfo userInfo = UserInfo.validate("AnalyticsServices.updateGlobalDataCollection", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification
		AirlockCapability[] requiredCapabilities = getRequiredCapabilities(branch_id); //ANALYTICS + maybe BRANCHES
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, requiredCapabilities);
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();		
		readWriteLock.writeLock().lock();
		try {
			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			Season season = seasonsDB.get(season_id);
			change.setSeason(season);

			if (season == null) {
				String errMsg = Strings.seasonNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();				
			}			

			//verify branchId
			ValidationResults validationRes = Utilities.validateBranchId(context, branch_id, season);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			//validate version. Only post 2.1 seasons support analytics
			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());
			env.setBranchId(branch_id);

			validationRes = validateAnalyticsSupport(env, season);
			if (validationRes!=null) 
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(validationRes.error)).build();	

			Map<String, BaseAirlockItem> airlockItemsDB = Utilities.getAirlockItemsDB (branch_id, context);

			env.setAirlockItemsDB(airlockItemsDB);

			JSONObject updatedGlobalDataCollectionJSON = null;
			try {
				updatedGlobalDataCollectionJSON = new JSONObject(globalDataCollection);
			} catch (JSONException je) {
				String errMsg = Strings.illegalInputJSON + je.getMessage();
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			//if not set - set the seasonId to be the id path param
			if (!updatedGlobalDataCollectionJSON.containsKey(Constants.JSON_FEATURE_FIELD_SEASON_ID) || updatedGlobalDataCollectionJSON.get(Constants.JSON_FEATURE_FIELD_SEASON_ID) == null) {
				updatedGlobalDataCollectionJSON.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, season_id);
			}
			else {
				//verify that season-id in path is identical to seasonId in request pay-load  
				if (!updatedGlobalDataCollectionJSON.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID).equals(season_id)) {
					String errMsg = Strings.collectionSeasonWithDifferentId;
					logger.severe(errMsg);
					return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();					
				}
			}

			AirlockAnalytics mergedAirlockAnalytics = Utilities.getAirlockAnalytics(season, branch_id, context, airlockItemsDB, env);			
			//GlobalDataCollection globalDataCollectionToUpdate = airlockAnalytics.getGlobalDataCollection(); //this is the merged analytics


			boolean productionChangeAllowed = validRole(userInfo);

			validationRes = mergedAirlockAnalytics.getGlobalDataCollection().validateAnalyticsItemJSON(updatedGlobalDataCollectionJSON, context, userInfo, env, airlockItemsDB, productionChangeAllowed);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			boolean productionChanged = (mergedAirlockAnalytics.getGlobalDataCollection().validateProductionDontChanged(updatedGlobalDataCollectionJSON, airlockItemsDB, context) != null);

			if (productionChanged) {
				//only productLead or Administrator can add feature in production to analytics
				if (!validRole(userInfo)) { 
					String errMsg = Strings.prodAnalyticsUpdateError;
					logger.severe(errMsg);
					return Response.status(Status.UNAUTHORIZED).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
				}								
			}
			LinkedList<String> beforeList = new LinkedList<>(season.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection().getFeaturesOnOff());

			//retrieve the real analytics object to perform the action on - the merged analytics is just a copy. 
			Branch branch = null;
			AirlockAnalytics airlockAnalytics = null;
			if (!env.isInMaster()) {
				@SuppressWarnings("unchecked")
				Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);				
				branch = branchesDB.get(branch_id);		//i know branch exists - after validate						
				airlockAnalytics = branch.getAnalytics();
				env.setAnalytics(mergedAirlockAnalytics); //this is the merged analytics - for comparison 
			}
			else {
				airlockAnalytics = season.getAnalytics();
			}
			change.setBranch(branch);
			//finally - actually update the globalDataCollection			
			String updateDetails = airlockAnalytics.getGlobalDataCollection().updateGlobalDataCollection(updatedGlobalDataCollectionJSON, context, env, airlockItemsDB);

			LinkedList<String> afterList = season.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection().getFeaturesOnOff();
			sendMailsForAnalytics(airlockItemsDB,beforeList,afterList,userInfo,env);


			if (!updateDetails.isEmpty()) { //if some fields were changed				
				try {
					change.getFiles().addAll(writeAnalyticsChangeToS3(env, season, productionChanged, branch, context));
					Webhooks.get(context).notifyChanges(change, context);
					/*if (env.isInMaster()) {
						AirlockFilesWriter.writeAnalytics(season, context, Constants.MASTER_BRANCH_NAME, env);	
						AirlockFilesWriter.doWriteFeatures (season, OutputJSONMode.RUNTIME_DEVELOPMENT, context); //write the dev runtime files
						if (productionChanged)
							AirlockFilesWriter.doWriteFeatures (season, OutputJSONMode.RUNTIME_PRODUCTION, context); //write the prod runtime files
					}
					else {
						AirlockFilesWriter.writeAnalytics(season, context, branch_id, env);				
						AirlockFilesWriter.writeBranchAndMasterRuntimeFiles(season, branch, context, env, productionChanged);
					}*/									
				} catch (IOException e) {
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
				}

				AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
				auditLogWriter.log("Update global data collection for season: " + season_id + ", branch " + branch_id + ",   " + updateDetails, userInfo); 								
			}

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Updated global data collection of season " + season_id + ", branch " + branch_id + ". Updated details: " + updateDetails);
			}

			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, season_id);
			res.put(Constants.JSON_FIELD_LAST_MODIFIED, airlockAnalytics.getGlobalDataCollection().getLastModified().getTime());
			return (Response.ok(res.toString())).build();
		} catch (MergeException e) {
			err = Strings.mergeException  + e.getMessage();
			logger.severe(err);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(err)).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error updating global data for analytics for season " + season_id  + " branch " + branch_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error updating global data for analytics: " + e.getMessage());
		}  finally {
			readWriteLock.writeLock().unlock();
		}
	}

	static boolean validRole(UserInfo userInfo)
	{
		return userInfo == null || userInfo.getRoles().contains(RoleType.Administrator) || userInfo.getRoles().contains(RoleType.ProductLead);
	}

	@Deprecated
	@POST
	@Path ("/globalDataCollection/branches/{branch-id}/feature/{feature-id}")
	@ApiOperation(value = "DEPRECTED - Send the specified feature or configuration rule to analytics", response = String.class) 
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Feature not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response addFeatureToAnalytics(@PathParam("feature-id")String feature_id,
			@PathParam("branch-id")String branch_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		return doAddItemToAnalytics(feature_id, branch_id, assertion, "AnalyticsServices.addFeatureToAnalytics");
	}
	
	@POST
	@Path ("/globalDataCollection/branches/{branch-id}/items/{item-id}")
	@ApiOperation(value = "Send the specified airlock item to analytics", response = String.class) 
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Airlock item not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response addItemToAnalytics(@PathParam("item-id")String item_id,
			@PathParam("branch-id")String branch_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		return doAddItemToAnalytics(item_id, branch_id, assertion, "AnalyticsServices.addItemToAnalytics");
	}

	private Response doAddItemToAnalytics(String item_id, String branch_id, String assertion, String funcName) {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest(funcName + " request: item_id: " + item_id + " branch_id = " + branch_id);
		}
		AirlockChange change = new AirlockChange();

		String err = Utilities.validateLegalUUID(item_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalFeatureUUID + err);

		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductForBranchOrFeature(context, branch_id, item_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);

		//check user authorization
		UserInfo userInfo = UserInfo.validate(funcName, context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);			

		//capability verification
		AirlockCapability[] requiredCapabilities = getRequiredCapabilities(branch_id); //ANALYTICS + maybe BRANCHES		
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, requiredCapabilities);
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.writeLock().lock();
		try {

			//verify branchId
			ValidationResults validationRes = Utilities.validateBranchId(context, branch_id, null);
			if (validationRes!=null) {
				return sendAndLogError(validationRes.status, validationRes.error);
			}

			Map<String, BaseAirlockItem> airlockItemsDB = Utilities.getAirlockItemsDB (branch_id, context);

			@SuppressWarnings("unchecked")			
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			Environment env = new Environment();

			BaseAirlockItem featureToAnalytics = airlockItemsDB.get(item_id);
			if (featureToAnalytics == null) {
				//airlockItemsDB = Utilities.getPurchaseItemsDB (branch_id, context);
				//featureToAnalytics = airlockItemsDB.get(feature_id);
				//if (featureToAnalytics == null) {
					return sendAndLogError(Status.NOT_FOUND, Strings.AirlockItemNotFound);
				//}
				//env.setRequestType(REQUEST_TYPE.PURCHASES);
			}		

			UUID seasonId = featureToAnalytics.getSeasonId();
			Season season = seasonsDB.get(seasonId.toString());
			if (season == null) {
				return sendAndLogError(Status.NOT_FOUND, Strings.seasonNotFound);	
			}
			change.setSeason(season);

			//validate version. Only post 2.1 seasons support analytics
			env.setServerVersion(season.getServerVersion());
			env.setBranchId(branch_id);
			env.setAirlockItemsDB(airlockItemsDB);

			validationRes = validateAnalyticsSupport(env, season);
			if (validationRes!=null) 
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(validationRes.error)).build();					

			if (BaseAirlockItem.isProductionFeature(featureToAnalytics, airlockItemsDB)) {
				//only productLead or Administrator can add feature in production to analytics
				if (!validRole(userInfo)) {
					return sendAndLogError(Status.UNAUTHORIZED, Strings.prodFeatureAnalyticsError);
				}								
			}

			AirlockAnalytics airlockAnalytics = Utilities.getAirlockAnalytics(season, branch_id, context, airlockItemsDB, env);

			validationRes = airlockAnalytics.getGlobalDataCollection().ValidateFeatureOnOffAddition(featureToAnalytics, env, season, context);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			//retrieve the real analytics objcet to perform the action on - the merged analytics is jsut a copy. 
			Branch branch = null;
			if (!env.isInMaster()) {
				@SuppressWarnings("unchecked")
				Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);				
				branch = branchesDB.get(branch_id);		//i know branch exists - after validate						
				airlockAnalytics = branch.getAnalytics(); 
			}
			else {
				airlockAnalytics = season.getAnalytics();
			}
			change.setBranch(branch);

			airlockAnalytics.getGlobalDataCollection().addFeatureOnOff(featureToAnalytics, airlockItemsDB);					

			if (featureToAnalytics instanceof DataAirlockItem) {
				sendMailForAnalytics(airlockItemsDB, (DataAirlockItem) featureToAnalytics, item_id, userInfo,"was",env);
			}
			try {
				boolean isProductionFeature = BaseAirlockItem.isProductionFeature(featureToAnalytics, airlockItemsDB);
				change.getFiles().addAll(writeAnalyticsChangeToS3(env, season, isProductionFeature, branch, context));
				Webhooks.get(context).notifyChanges(change, context);

			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}

			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Update global data collection for season: " + seasonId + ",   add item " + item_id + ", " + featureToAnalytics.getNameSpaceDotName(), userInfo); 								

			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, seasonId.toString());
			res.put(Constants.JSON_FIELD_LAST_MODIFIED, season.getAnalytics().getGlobalDataCollection().getLastModified().getTime());
			return (Response.ok(res.toString())).build();

		} catch (MergeException e) {
			err = Strings.mergeException  + e.getMessage();
			logger.severe(err);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(err)).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error adding feature " + item_id + ", branch " + branch_id + " to analytics: ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error adding item to analytics: " + e.getMessage());
		} 
		finally {
			readWriteLock.writeLock().unlock();
		}	
	}

	@Deprecated
	@DELETE
	@Path ("/globalDataCollection/branches/{branch-id}/feature/{feature-id}")
	@ApiOperation(value = "DEPRECATED - Stop sending the specified feature or configuration rule to analytics", response = String.class) 
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Feature not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response removeFeatureFromAnalytics(@PathParam("feature-id")String feature_id,	
			@PathParam("branch-id")String branch_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		
		return doRemoveItemFromAnalytics(feature_id, branch_id, assertion, "AnalyticsServices.removeFeatureFromAnalytics");
	}
	
	@DELETE
	@Path ("/globalDataCollection/branches/{branch-id}/items/{item-id}")
	@ApiOperation(value = "Stop sending the specified airlock item to analytics", response = String.class) 
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Airlock item not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response removeItemFromAnalytics(@PathParam("item-id")String item_id,	
			@PathParam("branch-id")String branch_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		
		return doRemoveItemFromAnalytics(item_id, branch_id, assertion, "AnalyticsServices.removeItemFromAnalytics");
	}


	private Response doRemoveItemFromAnalytics(String item_id, String branch_id, String assertion, String funcName) {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest(funcName + " request: feature_id: " + item_id + ", branch_id = " + branch_id);
		}
		AirlockChange change = new AirlockChange();
		String err = Utilities.validateLegalUUID(item_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalFeatureUUID + err);


		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductForBranchOrFeature(context, branch_id, item_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);

		//check user authorization
		UserInfo userInfo = UserInfo.validate(funcName, context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification
		AirlockCapability[] requiredCapabilities = getRequiredCapabilities(branch_id); //ANALYTICS + maybe BRANCHES		
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, requiredCapabilities);
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.writeLock().lock();
		try {


			@SuppressWarnings("unchecked")			
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);


			//verify branchId
			ValidationResults validationRes = Utilities.validateBranchId(context, branch_id, null);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			Map<String, BaseAirlockItem> airlockItemsDB = Utilities.getAirlockItemsDB(branch_id, context);

			BaseAirlockItem featureToAnalytics = airlockItemsDB.get(item_id);
			if (featureToAnalytics == null) {
				String errMsg = Strings.AirlockItemNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();			
			}			

			UUID seasonId = featureToAnalytics.getSeasonId();
			Season season = seasonsDB.get(seasonId.toString());
			change.setSeason(season);
			if (season == null) {
				String errMsg = Strings.seasonNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();	
			}

			//validate version. Only post 2.1 seasons support analytics
			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());
			env.setBranchId(branch_id);
			env.setAirlockItemsDB(airlockItemsDB);

			validationRes = validateAnalyticsSupport(env, season);
			if (validationRes!=null) 
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(validationRes.error)).build();	

			if (BaseAirlockItem.isProductionFeature(featureToAnalytics, airlockItemsDB)) {
				//only productLead or Administrator can remove feature in production from analytics
				if (!validRole(userInfo)) { 
					String errMsg = Strings.prodFeatureStopAnalyticsError;
					logger.severe(errMsg);
					return Response.status(Status.UNAUTHORIZED).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
				}								
			}

			AirlockAnalytics airlockAnalytics = Utilities.getAirlockAnalytics(season, branch_id, context, airlockItemsDB, env);

			validationRes = airlockAnalytics.getGlobalDataCollection().validateFeatureOnOffRemoval(featureToAnalytics, env, season);			
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			//retrieve the real analytics object to perform the action on - the merged analytics is just a copy. 
			Branch branch = null;
			if (!env.isInMaster()) {
				@SuppressWarnings("unchecked")
				Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);				
				branch = branchesDB.get(branch_id);		//i know branch exists - after validate						
				airlockAnalytics = branch.getAnalytics(); 
			}
			else {
				airlockAnalytics = season.getAnalytics();
			}
			change.setBranch(branch);

			airlockAnalytics.getGlobalDataCollection().removeFeatureOnOff(featureToAnalytics, airlockItemsDB);

			if (featureToAnalytics instanceof DataAirlockItem) {
				sendMailForAnalytics(airlockItemsDB, (DataAirlockItem) featureToAnalytics, item_id, userInfo,"is no longer being",env);
			}

			try {
				boolean isProductionFeature = BaseAirlockItem.isProductionFeature(featureToAnalytics, airlockItemsDB);
				change.getFiles().addAll(writeAnalyticsChangeToS3(env, season, isProductionFeature, branch, context));
				Webhooks.get(context).notifyChanges(change, context);
			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}

			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Update global data collection for season: " + seasonId + ", branch: " + branch_id + ",   remove airlock item " + item_id + ", " + featureToAnalytics.getNameSpaceDotName(), userInfo); 								

			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, seasonId.toString());
			res.put(Constants.JSON_FIELD_LAST_MODIFIED, season.getAnalytics().getGlobalDataCollection().getLastModified().getTime());
			return (Response.ok(res.toString())).build();
		} catch (MergeException e) {
			err = Strings.mergeException  + e.getMessage();
			logger.severe(err);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(err)).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error removing airlock item " + item_id + " branch " + branch_id + " from analytics : ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error rremoving airlock item from analytics: " + e.getMessage());
		}  finally {
			readWriteLock.writeLock().unlock();
		}
	}

	@Deprecated
	@PUT
	@Path ("/globalDataCollection/branches/{branch-id}/feature/{feature-id}/attributes")
	@ApiOperation(value = "DEPRECATED - Update features attributes for analytics", response = String.class) 
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response updateFeatureAttributesForAnalytics(@PathParam("feature-id")String feature_id,
			@PathParam("branch-id")String branch_id,
			String featureAttributes,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		
		return doUpdateItemAttributesForAnalytics (feature_id, branch_id, featureAttributes, assertion, "AnalyticsServices.updateFeatureAttributesForAnalytics");
	}
	
	@PUT
	@Path ("/globalDataCollection/branches/{branch-id}/items/{item-id}/attributes")
	@ApiOperation(value = "Update airlock item's attributes for analytics", response = String.class) 
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Airlock item not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response updateItemAttributesForAnalytics(@PathParam("item-id")String item_id,
			@PathParam("branch-id")String branch_id,
			String featureAttributes,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		
		return doUpdateItemAttributesForAnalytics (item_id, branch_id, featureAttributes, assertion, "AnalyticsServices.updateItemAttributesForAnalytics");
	}



	private Response doUpdateItemAttributesForAnalytics(String feature_id, String branch_id, String featureAttributes,
			String assertion, String funcName) {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest(funcName + " request: feature_id: " + feature_id + ",branch_id: " + branch_id + ", featureAttributes = " + featureAttributes);
		}
		AirlockChange change = new AirlockChange();
		String err = Utilities.validateLegalUUID(feature_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalFeatureUUID + err);

		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductForBranchOrFeature(context, branch_id, feature_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);

		//check user authorization
		UserInfo userInfo = UserInfo.validate(funcName, context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//validate that is a legal JSON Array
		JSONArray attributesJSONArray = null;
		try {
			attributesJSONArray = new JSONArray(featureAttributes);
		} catch (JSONException je) {
			String errMsg = Strings.illegalFeatureAttibuteJSON + je.getMessage();
			logger.severe(errMsg);
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();				
		}

		//capability verification
		AirlockCapability[] requiredCapabilities = getRequiredCapabilities(branch_id); //ANALYTICS + maybe BRANCHES		
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, requiredCapabilities);
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.writeLock().lock();
		try {
			@SuppressWarnings("unchecked")			
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);			

			//verify branchId
			ValidationResults validationRes = Utilities.validateBranchId(context, branch_id, null);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}
			//			boolean inMaster = branch_id.equals(Constants.MASTER_BRANCH_NAME);

			Map<String, BaseAirlockItem> airlockItemsDB = Utilities.getAirlockItemsDB (branch_id, context);

			BaseAirlockItem feature = airlockItemsDB.get(feature_id);
			if (feature == null) {
				return sendAndLogError(Status.NOT_FOUND, Strings.AirlockItemNotFound);			
			}

			if (!(feature instanceof FeatureItem)) {
				return sendAndLogError(Status.BAD_REQUEST, Strings.onlyFeaturesInAppPurchasesAndPurcahseOptions);			
			}

			String seasonId = feature.getSeasonId().toString();

			Season season = seasonsDB.get(seasonId);
			change.setSeason(season);
			if (season == null) {
				return sendAndLogError(Status.NOT_FOUND, Strings.seasonNotFound);	
			}

			//validate version. Only post 2.1 seasons support analytics
			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());
			env.setBranchId(branch_id);
			env.setAirlockItemsDB(airlockItemsDB);

			validationRes = validateAnalyticsSupport(env, season);
			if (validationRes!=null) 
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(validationRes.error)).build();	

			if (BaseAirlockItem.isProductionFeature(feature, airlockItemsDB)) {
				//only productLead or Administrator can update attributes for feature in production
				if (!validRole(userInfo)) { 
					String errMsg = Strings.prodFeatureUpdateAnalyticsError;
					logger.severe(errMsg);
					return Response.status(Status.UNAUTHORIZED).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
				}								
			}

			AirlockAnalytics airlockAnalytics = Utilities.getAirlockAnalytics(season, branch_id, context, airlockItemsDB, env);
			//needed for both validate and update to created here
			// build the att map from the merged features tree:
			BaseAirlockItem rootOfMergedTree = null;
			if (BaseAirlockItem.isPurchaseItem(feature.getType())) {
				rootOfMergedTree = airlockItemsDB.get(season.getEntitlementsRoot().getUniqueId().toString());
			}
			else {
				rootOfMergedTree = airlockItemsDB.get(season.getRoot().getUniqueId().toString());
			}
			Map<String, TreeSet<String>> featureAttributesMap = AnalyticsDataCollection.getFeatureAttributesMap (rootOfMergedTree.toJson(OutputJSONMode.ADMIN, context, env), null, season, context); 			
			validationRes = airlockAnalytics.getGlobalDataCollection().validateFeatureAttributsUpdate((FeatureItem)feature, attributesJSONArray, context, season, airlockItemsDB, featureAttributesMap, env, false); //check counters as well
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			//retrieve the real analytics object to perform the action on - the merged analytics is just a copy. 
			Branch branch = null;
			if (!env.isInMaster()) {
				@SuppressWarnings("unchecked")
				Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);				
				branch = branchesDB.get(branch_id);		//i know branch exists - after validate						
				airlockAnalytics = branch.getAnalytics(); 
			}
			else {
				airlockAnalytics = season.getAnalytics();
			}
			change.setBranch(branch);

			airlockAnalytics.getGlobalDataCollection().updateFeatureAttributs ((FeatureItem)feature, attributesJSONArray, context, season, airlockItemsDB, featureAttributesMap, env);

			try {
				boolean productionChange = ((FeatureItem)feature).getStage().equals(Stage.PRODUCTION);
				change.getFiles().addAll(writeAnalyticsChangeToS3(env, season, productionChange, branch, context));
				Webhooks.get(context).notifyChanges(change, context);
				/*				if (env.isInMaster()) {
					AirlockFilesWriter.writeAnalytics(season, context, Constants.MASTER_BRANCH_NAME, env);	
					AirlockFilesWriter.doWriteFeatures (season, OutputJSONMode.RUNTIME_DEVELOPMENT, context); //write the dev runtime files
					if (productionChange)
						AirlockFilesWriter.doWriteFeatures (season, OutputJSONMode.RUNTIME_PRODUCTION, context); //write the prod runtime files
				} 
				else {
					AirlockFilesWriter.writeAnalytics(season, context, branch_id, env);				
					AirlockFilesWriter.writeBranchAndMasterRuntimeFiles(season, branch, context, env, productionChange);
				}*/				
			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}


			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Update global data collection for season: attributes for analytics of feature: " + feature_id + ", " + feature.getNameSpaceDotName() + " updated to " + featureAttributes , userInfo); 								

			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, seasonId);
			res.put(Constants.JSON_FIELD_LAST_MODIFIED, season.getAnalytics().getGlobalDataCollection().getLastModified().getTime());
			return (Response.ok(res.toString())).build();

		} catch (MergeException e) {
			err = Strings.mergeException  + e.getMessage();
			logger.severe(err);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(err)).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error updating feature " + feature_id + " branch " + branch_id + " attributes for analytics: " , e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error updating feature attributes for analytics: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}

	}

	//the input is jsonArray of the input fields
	@PUT
	@Path ("/globalDataCollection/{season-id}/branches/{branch-id}/inputfields")
	@ApiOperation(value = "Update input field for analytics", response = String.class) 
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response updateInputFieldsForAnalytics(@PathParam("season-id")String season_id,
			@PathParam("branch-id")String branch_id,
			String inputFields,			
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("updateInputFieldsForAnalytics request: season_id: " + season_id + ", branch_id = " + branch_id);
		}

		String err = Utilities.validateLegalUUID(season_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalSeasonUUID + err);

		AirlockChange change = new AirlockChange();

		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, branch_id, season_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);

		//check user authorization
		UserInfo userInfo = UserInfo.validate("AnalyticsServices.updateInputFieldsForAnalytics", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification
		AirlockCapability[] requiredCapabilities = getRequiredCapabilities(branch_id); //ANALYTICS + maybe BRANCHES
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, requiredCapabilities);
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();				

		//validate that is a legal JSON Array
		JSONArray inputFieldsJSONArray = null;
		try {
			inputFieldsJSONArray = new JSONArray(inputFields);
		} catch (JSONException je) {
			String errMsg = Strings.illegalInputFieldJSONArray + je.getMessage();
			logger.severe(errMsg);
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();				
		}

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.writeLock().lock();
		try {

			@SuppressWarnings("unchecked")			
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			Season season = seasonsDB.get(season_id);
			change.setSeason(season);
			if (season == null) {
				String errMsg = Strings.seasonNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();	
			}

			//verify branchId
			ValidationResults validationRes = Utilities.validateBranchId(context, branch_id, season);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}
			//			boolean inMaster = branch_id.equals(Constants.MASTER_BRANCH_NAME);

			//validate version. Only post 2.1 seasons support analytics
			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());
			env.setBranchId(branch_id);

			validationRes = validateAnalyticsSupport(env, season);
			if (validationRes!=null) 
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(validationRes.error)).build();	

			boolean productionChangeAllowed = validRole(userInfo);

			Map<String, BaseAirlockItem> airlockItemsDB = Utilities.getAirlockItemsDB (branch_id, context);
			AirlockAnalytics airlockAnalytics = Utilities.getAirlockAnalytics(season, branch_id, context, airlockItemsDB, env);

			env.setAirlockItemsDB(airlockItemsDB);

			validationRes = airlockAnalytics.getGlobalDataCollection().validateInputFieldsUpdate(inputFieldsJSONArray, context, season, productionChangeAllowed, env, false);
			boolean productionChange = false;
			//upon production change the validationRes are not null but with OK status
			if (validationRes!=null) {
				if (validationRes.status == Status.OK) {
					productionChange = true;
				}
				else {
					String errMsg = validationRes.error;
					logger.severe(errMsg);
					return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
				}
			}

			//retrieve the real analytics object to perform the action on - the merged analytics is just a copy. 
			Branch branch = null;
			if (!env.isInMaster()) {
				@SuppressWarnings("unchecked")
				Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);				
				branch = branchesDB.get(branch_id);		//i know branch exists - after validate						
				airlockAnalytics = branch.getAnalytics(); 
			}
			else {
				airlockAnalytics = season.getAnalytics();
			}
			change.setBranch(branch);

			airlockAnalytics.getGlobalDataCollection().updateInputFields(inputFieldsJSONArray, context, season, productionChangeAllowed, env);					

			try {
				change.getFiles().addAll(writeAnalyticsChangeToS3(env, season, productionChange, branch, context));
				Webhooks.get(context).notifyChanges(change, context);
			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}

			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Update global data collection for season: input fields for analytics updated to " + inputFields, userInfo); 								

			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, season_id);
			res.put(Constants.JSON_FIELD_LAST_MODIFIED, season.getAnalytics().getGlobalDataCollection().getLastModified().getTime());
			return (Response.ok(res.toString())).build();

		} 
		catch (MergeException e) {
			err = Strings.mergeException  + e.getMessage();
			logger.severe(err);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(err)).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error updating input fields for analytics for season " + season_id  + " branch " + branch_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error updating input fields for analytics: " + e.getMessage());
		} 
		finally {
			readWriteLock.writeLock().unlock();
		}
	}


	private ValidationResults validateAnalyticsSupport (Environment env, Season season) {
		if (isAnalyticsSupported(env))  //only post 2.1 seasons support analytics
			return null;

		String errMsg = Strings.analyticsNotSupported;
		logger.severe(errMsg);
		return new ValidationResults(errMsg, Status.BAD_REQUEST);
	}

	private ValidationResults validateExperimentsSupport (Environment env, Season season) {
		if (isExperimentsSupported(env))  //only post 2.5 seasons support experiments (3.0 and up)
			return null;

		String errMsg = Strings.experimentNotSupported;
		logger.severe(errMsg);
		return new ValidationResults(errMsg, Status.BAD_REQUEST);
	}	

	public static boolean isAnalyticsSupported (Environment env) {
		Version version = env.getVersion();
		return version.i >= Version.v2_5.i;  //only post 2.1 seasons support analytics			
	}

	public static boolean isExperimentsSupported (Environment env) {
		Version version = env.getVersion();
		return version.i >= Version.v3_0.i;  //only post 2.5 seasons support experiments (3.0 and up)	
	}

	private void sendMailsForAnalytics(Map<String, BaseAirlockItem> airlockItemsDB,LinkedList<String> before, LinkedList<String> after, UserInfo userInfo,Environment env){
		// added
		for(int i = 0; i<after.size();++i){
			String itemId = after.get(i);
			if(!before.contains(itemId)){
				BaseAirlockItem featureToAnalytics = airlockItemsDB.get(itemId);
				if(featureToAnalytics instanceof DataAirlockItem){
					sendMailForAnalytics(airlockItemsDB,(DataAirlockItem) featureToAnalytics,itemId,userInfo,"sent to",env);
				}
			}
		}
		//removed
		for(int j = 0; j<before.size();++j){
			String itemId = before.get(j);
			if(!after.contains(itemId)){
				BaseAirlockItem featureToAnalytics = airlockItemsDB.get(itemId);
				if(featureToAnalytics instanceof DataAirlockItem){
					sendMailForAnalytics(airlockItemsDB,(DataAirlockItem) featureToAnalytics,itemId,userInfo,"removed from",env);
				}
			}
		}
	}

	private void sendMailForAnalytics(Map<String, BaseAirlockItem> airlockItemsDB, DataAirlockItem featureToAnalytics, String featureId, UserInfo userInfo,String addOrRemoved, Environment env) {
		FeatureItem featureToNotify = null;
		Map<String, ArrayList<String>> followersFeaturesDB = (Map<String, ArrayList<String>>) context.getAttribute(Constants.FOLLOWERS_FEATURES_DB_PARAM_NAME);
		Boolean isProduction = featureToAnalytics.getStage().toString().equals("PRODUCTION");
		ArrayList<String> followers = new ArrayList<>();
		if (featureToAnalytics instanceof FeatureItem) {
			featureToNotify = (FeatureItem) featureToAnalytics;
			followers = followersFeaturesDB.get(featureToAnalytics.getUniqueId().toString());
		} else if (featureToAnalytics instanceof ConfigurationRuleItem) { //config rule or ordering rule
			BaseAirlockItem configParent = airlockItemsDB.get(featureToAnalytics.getParent().toString());
			while (configParent.getType() != Type.FEATURE && configParent.getType() != Type.ROOT) {
				configParent = airlockItemsDB.get(configParent.getParent().toString());
			}
			if (configParent instanceof FeatureItem) {
				featureToNotify = (FeatureItem) configParent;
				followers = followersFeaturesDB.get(configParent.getUniqueId().toString());
			}
		}
		if (featureToNotify!=null) {
			String details = "The " + featureToAnalytics.getObjTypeStrByType() + " " + featureToAnalytics.getNameSpaceDotName() + " with ID " + featureId + " "+ addOrRemoved+ " sent to analytics \n";
			Utilities.sendEmailForDataItem(context, featureToNotify, followers, details, null, null, isProduction, userInfo,env);
		}
	}

	@GET
	@Path("/{season-id}/quota")
	@ApiOperation(value = "Returns the analytics quota", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),			
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getAnalyticsQuota(@PathParam("season-id")String season_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getAnalyticsQuota request");
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

		// use userInfo for more stringent checks. null if authorization is off
		UserInfo userInfo = UserInfo.validate("AnalyticsServices.getAnalyticsQuota", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.ANALYTICS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();		
		readWriteLock.readLock().lock(); 
		try {
			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);			

			Season season = seasonsDB.get(season_id);

			if (season == null) {
				String errMsg = Strings.seasonNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();				
			}

			//validate version. Only post 2.1 seasons support analytics
			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion()); 			
			ValidationResults validationRes = validateAnalyticsSupport(env, season);
			if (validationRes!=null) 
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(validationRes.error)).build();	

			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, season_id);			
			res.put(Constants.JSON_FIELD_ANALYTICS_QUOTA, season.getAnalytics().getAnalyticsQuota());

			return (Response.ok()).entity(res.toString()).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error getting analytics quota for season " + season_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error getting analytics quota: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}

	}


	@PUT
	@Path("/{season-id}/quota/{quota}")
	@ApiOperation(value = "Update the analytics quota", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),			
			@ApiResponse(code = 500, message = "Internal error") })
	public Response setAnalyticsQuota(@PathParam("season-id")String season_id, @PathParam("quota")Integer quota,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("setAnalyticsQuota request, quota = " + quota);
		}
		AirlockChange change = new AirlockChange();
		String err = Utilities.validateLegalUUID(season_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalSeasonUUID + err);

		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, season_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);

		// use userInfo for more stringent checks. null if authorization is off
		UserInfo userInfo = UserInfo.validate("AnalyticsServices.setAnalyticsQuota", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.ANALYTICS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();		
		readWriteLock.writeLock().lock();
		try {

			@SuppressWarnings("unchecked")			
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			@SuppressWarnings("unchecked")
			Map<String, BaseAirlockItem> airlockItemsDB = (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);

			Season season = seasonsDB.get(season_id);
			change.setSeason(season);
			if (season == null) {
				String errMsg = Strings.seasonNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();	
			}

			//validate version. Only post 2.1 seasons support analytics
			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());
			env.setAirlockItemsDB(airlockItemsDB);

			ValidationResults validationRes = validateAnalyticsSupport(env, season);
			if (validationRes!=null) 
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(validationRes.error)).build();	


			if (quota == null || quota<0) {
				String errMsg = Strings.illegalQuota;
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			validationRes = season.validateNewQuota(quota, context);
			if (validationRes!=null) 
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(validationRes.error)).build();


			season.getAnalytics().setAnalyticsQuota(quota);
			try {
				change.getFiles().addAll(AirlockFilesWriter.writeAnalytics(season, context, Constants.MASTER_BRANCH_NAME, env, Stage.PRODUCTION));
				Webhooks.get(context).notifyChanges(change, context);
			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}

			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Set analytics quota to " + quota, userInfo); 								

			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, season_id);
			res.put(Constants.JSON_FIELD_ANALYTICS_QUOTA, quota);
			return (Response.ok(res.toString())).build();

		} catch (MergeException e) {
			err = Strings.mergeException  + e.getMessage();
			logger.severe(err);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(err)).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error setting analytics quota for season " + season_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error setting analytics quota: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}


	@POST 
	@Path ("/products/{product-id}/experiments") 
	@ApiOperation(value = "Creates experiment within the specified product", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Product not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response addExperiment(@PathParam("product-id")String product_id, String newExperiment,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("addExperiment request: product_id = " + product_id + ", newExperiment = " + newExperiment);
		}

		AirlockChange change = new AirlockChange();

		String err = Utilities.validateLegalUUID(product_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalProductUUID + err);

		//find relevant product
		ProductErrorPair prodErrPair = Utilities.getProduct(context, product_id);
		if (prodErrPair.error != null) {			
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		Product currentProduct = prodErrPair.product;
		change.setProduct(currentProduct);
		//check user authorization
		UserInfo userInfo = UserInfo.validate("AnalyticsServices.addExperiment", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.EXPERIMENTS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.writeLock().lock();
		try {	
			@SuppressWarnings("unchecked")
			Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);


			//Validate season existence
			Product product = productsDB.get(product_id);

			if (product == null) {
				String errMsg = Strings.productNotFound;
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();				
			}			

			//validate that is a legal JSON
			JSONObject newExperimentJSON = null;
			try {
				newExperimentJSON = new JSONObject(newExperiment);
			} catch (JSONException je) {
				String errMsg = Strings.illegalInputJSON + je.getMessage();
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();				
			}

			//verify that JSON does not contain uniqueId field
			if (newExperimentJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && newExperimentJSON.get(Constants.JSON_FIELD_UNIQUE_ID) !=null) {
				String errMsg = Strings.experimentWithId;
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();							
			}

			//verify that JSON does not contain different product-id then the path parameter
			if (newExperimentJSON.containsKey(Constants.JSON_FIELD_PRODUCT_ID) && newExperimentJSON.get(Constants.JSON_FIELD_PRODUCT_ID) !=null) {
				if (!product_id.equals(newExperimentJSON.getString(Constants.JSON_FIELD_PRODUCT_ID))) {
					String errMsg = Strings.experimentProductWithDifferentId;
					logger.severe(errMsg);
					return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();									
				}
			}
			else {		
				newExperimentJSON.put(Constants.JSON_FIELD_PRODUCT_ID, product_id);
			}

			Experiment newExperimentObj = new Experiment(product.getUniqueId());

			ValidationResults validationRes = newExperimentObj.validateExperimentJSON(newExperimentJSON, context, userInfo);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			newExperimentObj.fromJSON(newExperimentJSON, context, false);
			newExperimentObj.addExperimentRange(newExperimentObj.getCreationDate());
			newExperimentObj.setUniqueId(UUID.randomUUID());

			product.addExperiment(newExperimentObj);

			ValidationResults pushingExpToAnalyticsSrvRes = null;
			try {
				//Date now = new Date();
				//pushingExpToAnalyticsSrvRes = AnalyticsUtilities.updateAnalyticsServer(context, null, null, now, null, null, null, null);
				pushingExpToAnalyticsSrvRes = AnalyticsUtilities.addExperimentToAnalyticsServer(context, newExperimentObj);
			} catch (Exception e) {
				pushingExpToAnalyticsSrvRes = new ValidationResults(Strings.failAddingExperiment + e.getMessage(), Status.INTERNAL_SERVER_ERROR);
			} 

			if (pushingExpToAnalyticsSrvRes!=null) {
				product.removeExperiment(newExperimentObj.getUniqueId());				
				logger.severe(pushingExpToAnalyticsSrvRes.error);
				return Response.status(pushingExpToAnalyticsSrvRes.status).entity(Utilities.errorMsgToErrorJSON(pushingExpToAnalyticsSrvRes.error)).build();
			}

			try {
				//AirlockFilesWriter.writeProducts(productsDB, context);
				change.getFiles().addAll(AirlockFilesWriter.writeExperiments(product, context, newExperimentObj.getStage()));

				Map<String, BaseAirlockItem> airlockItemsDB = Utilities.getAirlockItemsDB(Constants.MASTER_BRANCH_NAME, context);
				for (Season s:product.getSeasonsWithinRange(newExperimentObj.getMinVersion(), newExperimentObj.getMaxVersion())) {
					Environment env = new Environment();
					env.setAirlockItemsDB(airlockItemsDB);
					env.setServerVersion(s.getServerVersion());
					env.setBranchId(Constants.MASTER_BRANCH_NAME);
					//write the new experiment in the runtime files
					change.getFiles().addAll(AirlockFilesWriter.doWriteFeatures(s, OutputJSONMode.RUNTIME_DEVELOPMENT, context, newExperimentObj.getStage(), env));
					if (newExperimentObj.getStage().equals(Stage.PRODUCTION))
						change.getFiles().addAll(AirlockFilesWriter.doWriteFeatures(s, OutputJSONMode.RUNTIME_PRODUCTION, context, newExperimentObj.getStage(), env));
				} 
				Webhooks.get(context).notifyChanges(change, context);
			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}

			@SuppressWarnings("unchecked")
			Map<String, Experiment> experimentsDB = (Map<String, Experiment>)context.getAttribute(Constants.EXPERIMENTS_DB_PARAM_NAME);

			experimentsDB.put(newExperimentObj.getUniqueId().toString(), newExperimentObj);



			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Create new experiment: " + newExperimentObj.getName() + ", " + newExperimentObj.getUniqueId() + ":" + newExperimentObj.toJson(OutputJSONMode.ADMIN, context, false, false).toString(), userInfo); 

			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FIELD_UNIQUE_ID, newExperimentObj.getUniqueId().toString());
			res.put(Constants.JSON_FIELD_NAME, newExperimentObj.getName());
			res.put(Constants.JSON_FIELD_LAST_MODIFIED, newExperimentObj.getLastModified().getTime());

			logger.info("Experiment added to product '"+  product_id + "': "+ res.toString());

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Added experiment: " + newExperimentObj.toJson(OutputJSONMode.ADMIN));
			}
			Utilities.sendEmailForExperiment(context,newExperimentObj,null,product_id,"created",null,newExperimentObj.getStage().equals(Stage.PRODUCTION),userInfo);
			return (Response.ok(res.toString())).build();
		} catch (MergeException e) {
			err = Strings.mergeException  + e.getMessage();
			logger.severe(err);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(err)).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error adding experiment to product " + product_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error adding item: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	@GET
	@Path ("/products/experiments/{experiment-id}")	 
	@ApiOperation(value = "Returns the specified experiment", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Feature not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getExperiment(@PathParam("experiment-id")String experiment_id,
			@QueryParam("includeindexinginfo")Boolean includeIndexingInfo,			
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getExperiment request");
		}

		String err = Utilities.validateLegalUUID(experiment_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalExperimentUUID + err);

		//find relevant product
		ProductErrorPair prodErrPair = Utilities.getProductOfExperiment(context, experiment_id);
		if (prodErrPair.error != null) {			
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		Product currentProduct = prodErrPair.product;

		//check user authorization
		UserInfo userInfo = UserInfo.validate("AnalyticsServices.getExperiment", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		if (includeIndexingInfo == null)
			includeIndexingInfo = false; 	

		//if (includeIndexingInfo && !isAnalyticsServerConfigured()) {
		//	return sendError(Status.BAD_REQUEST, Strings.analyticsServerNotConfigured);
		//}

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.EXPERIMENTS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();		
		readWriteLock.readLock().lock();
		try {			

			@SuppressWarnings("unchecked")
			Map<String, Experiment> experimentsDB = (Map<String, Experiment>)context.getAttribute(Constants.EXPERIMENTS_DB_PARAM_NAME);

			Experiment exp = experimentsDB.get(experiment_id);

			if (exp == null) {
				String errMsg = Strings.experimentNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			//no need to validate experiments support since the experiment exists so it is supported in its season  

			JSONObject expJSON = exp.toJson(OutputJSONMode.ADMIN, context, false, false);
			if (includeIndexingInfo) {
				ValidationResults res = null;
				try { 
					res = AnalyticsUtilities.getExperimentFromAnalyticsServer(context, experiment_id);
					JSONObject indexingDataJson = null;

					if (res ==null) {
						//means analytics server does not exists
						expJSON.put(Constants.JSON_FIELD_INDEXING_INFO, indexingDataJson);
					}
					else {
						if (res.status!=Status.OK && res.status!=Status.NOT_FOUND) {
							logger.severe(res.error);
							return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(res.error)).build();						
						}

						//if experiment is not found in the analytics server its indexing data is null
						if (res.status==Status.OK) {
							indexingDataJson = new JSONObject(res.error);						
						}

						expJSON.put(Constants.JSON_FIELD_INDEXING_INFO, indexingDataJson);
					}
				} catch (Exception e) {
					err = Strings.failGetExperimentFromAnalyticsServer + e.getMessage();
					logger.severe(err);
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(err)).build();					
				}
			}
			return Response.ok(expJSON.toString()).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error getting experiment " + experiment_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error getting experiment: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}


	@PUT
	@Path ("/products/experiments/{experiment-id}/resetdashboard")	
	@ApiOperation(value = "Reset the specified experiment's dashboard", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Feature not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response resetExperimentDashboard(@PathParam("experiment-id")String experiment_id,					
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("resetExperimentDashboard request");
		}

		String err = Utilities.validateLegalUUID(experiment_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalExperimentUUID + err);

		if (!isAnalyticsServerConfigured()) {
			return sendAndLogError(Status.BAD_REQUEST, Strings.analyticsServerNotConfigured);
		}

		//find relevant product
		ProductErrorPair prodErrPair = Utilities.getProductOfExperiment(context, experiment_id);
		if (prodErrPair.error != null) {			
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		Product currentProduct = prodErrPair.product;

		//check user authorization
		UserInfo userInfo = UserInfo.validate("AnalyticsServices.resetExperimentDashboard", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.EXPERIMENTS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();	
		readWriteLock.writeLock().lock();
		try {			

			@SuppressWarnings("unchecked")
			Map<String, Experiment> experimentsDB = (Map<String, Experiment>)context.getAttribute(Constants.EXPERIMENTS_DB_PARAM_NAME);

			Experiment exp = experimentsDB.get(experiment_id);

			if (exp == null) {
				String errMsg = Strings.experimentNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			if (exp.getStage().equals(Stage.PRODUCTION) && !validRole(userInfo)) {
				err = "Unable to reset the expeimrnt's dashboard. Only a user with the Administrator or Product Lead role can reset dashboard for experiemnt in the production stage.";
				logger.severe(err);
				return Response.status(Status.UNAUTHORIZED).entity(Utilities.errorMsgToErrorJSON(err)).build();
			}

			//no need to validate experiments support since the experiment exists so it is supported in its season  
			try {
				ValidationResults res = AnalyticsUtilities.resetDashboardInAnalyticsServer(context, experiment_id);

				if (res!=null && res.status!=Status.OK) {
					logger.severe(res.error);
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(res.error)).build();						
				}				
			} catch (Exception e) {
				err = Strings.failResetExperimentDashboardInAnalyticsServer + e.getMessage();
				logger.severe(err);
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(err)).build();

			} 		

			return Response.ok().build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error resetting experiment " + experiment_id + " dashboard : ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error resetting experiment dashboard: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	@GET
	@Path ("/products/experiments/{experiment-id}/inputsample")	
	@ApiOperation(value = "Returns the specified experiment", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Feature not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getExperimentInputSample(@PathParam("experiment-id")String experiment_id,
			@ApiParam(value="DEVELOPMENT or PRODUCTION")@QueryParam("stage")String stage , 
			@QueryParam("minappversion")String minAppVersion,
			@ApiParam(value="MINIMAL, MAXIMAL or PARTIAL")@QueryParam("generationmode")String generationMode, 
			@ApiParam(value="determines the values that are returned. 0 = fully randomized.")@QueryParam("randomize")Double randomize, 			
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getExperimentInputSample request: experiment_id = " + experiment_id + ", stage = " + stage + ", minappversion = " + minAppVersion + ", generationmode = " + generationMode);
		}

		String err = Utilities.validateLegalUUID(experiment_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalExperimentUUID + err);

		//find relevant product
		ProductErrorPair prodErrPair = Utilities.getProductOfExperiment(context, experiment_id);
		if (prodErrPair.error != null) {			
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		Product currentProduct = prodErrPair.product;

		//check user authorization
		UserInfo userInfo = UserInfo.validate("AnalyticsServices.getExperimentInputSample", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.EXPERIMENTS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		

		if (stage==null) {
			String errMsg = Strings.stageMissing;
			logger.severe(errMsg);						
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
		}				

		Stage stageObj = Utilities.strToStage(stage);

		if (stageObj==null) {			
			String errMsg =  String.format(Strings.illegalStage,stage)  + Constants.Stage.returnValues();
			logger.severe(errMsg);						
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
		}

		if (generationMode==null) {
			String errMsg =  Strings.generationModeMissing;
			logger.severe(errMsg);						
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
		}

		InputSampleGenerationMode generationModeObj = Utilities.strToInputSampleGenerationMode(generationMode);

		if (generationModeObj==null) {
			String errMsg = String.format(Strings.illegalGenerationMode,generationMode) + Constants.InputSampleGenerationMode.returnValues();
			logger.severe(errMsg);						
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
		}

		if (minAppVersion==null) {
			String errMsg =   Strings.minAppMissing;
			logger.severe(errMsg);						
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
		}

		if (randomize==null) {
			randomize = Constants.DEFAULT_RANDOMIZER;		
		}

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();	
		readWriteLock.readLock().lock();
		try {			

			@SuppressWarnings("unchecked")
			Map<String, Experiment> experimentsDB = (Map<String, Experiment>)context.getAttribute(Constants.EXPERIMENTS_DB_PARAM_NAME);

			Experiment exp = experimentsDB.get(experiment_id);

			if (exp == null) {
				String errMsg = Strings.experimentNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			//no need to validate experiments support since the experiment exists so it is supported in its season  

			try {
				JSONObject res = exp.generateInputSample(stageObj, minAppVersion, context, generationModeObj, randomize);				
				return Response.ok(res.toString()).build();
			} catch (GenerationException e) {
				String errMsg = Strings.failedGeneratingSample + e.getMessage();
				logger.severe(errMsg);						
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}
			catch (JSONException je) {
				String errMsg = Strings.invalidJsonGeneratingSample + je.getMessage();
				logger.severe(errMsg);						
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error getting experiment " + experiment_id + " input sample: ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error getting experiment input sample: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}


	//return the max quota of all seasons in experiment
	@GET
	@Path ("/products/experiments/{experiment-id}/quota")	
	@ApiOperation(value = "Returns the experiment analytics quota", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),			
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getExperimentAnalyticsQuota(@PathParam("experiment-id")String experiment_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getExperimentAnalyticsQuota request");
		}

		String err = Utilities.validateLegalUUID(experiment_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalExperimentUUID + err);

		//find relevant product
		ProductErrorPair prodErrPair = Utilities.getProductOfExperiment(context, experiment_id);
		if (prodErrPair.error != null) {			
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		Product currentProduct = prodErrPair.product;

		// use userInfo for more stringent checks. null if authorization is off
		UserInfo userInfo = UserInfo.validate("AnalyticsServices.getExperimentAnalyticsQuota", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.EXPERIMENTS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();	
		readWriteLock.readLock().lock(); 
		try {
			@SuppressWarnings("unchecked")
			Map<String, Experiment> experimentsDB = (Map<String, Experiment>)context.getAttribute(Constants.EXPERIMENTS_DB_PARAM_NAME);

			Experiment exp = experimentsDB.get(experiment_id);

			if (exp == null) {
				String errMsg = Strings.experimentNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			//no need to validate experiments support since the experiment exists so it is supported in its season						

			int quota = exp.getQuota(context);

			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FIELD_EXPERIMENT_ID, experiment_id);			
			res.put(Constants.JSON_FIELD_ANALYTICS_QUOTA, quota);

			return (Response.ok()).entity(res.toString()).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error getting experiment " + experiment_id + " quota: ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error getting experiment quota: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}

	}


	@GET
	@Path ("/products/experiments/{experiment-id}/utilitiesinfo")
	@ApiOperation(value = "Returns the utilities names and parameters for the specified experiment and stage.", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Feature not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getExperimentUtilitiesInfo(@PathParam("experiment-id")String experiment_id,
			@ApiParam(value="DEVELOPMENT or PRODUCTION")@QueryParam("stage")String stage , 
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getExperimentUtilitiesInfo request: experiment_id = " + experiment_id + ", stage = " + stage);
		}

		String err = Utilities.validateLegalUUID(experiment_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalExperimentUUID + err);

		//find relevant product
		ProductErrorPair prodErrPair = Utilities.getProductOfExperiment(context, experiment_id);
		if (prodErrPair.error != null) {			
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		Product currentProduct = prodErrPair.product;

		//check user authorization
		UserInfo userInfo = UserInfo.validate("AnalyticsServices.getExperimentUtilitiesInfo", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.EXPERIMENTS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		

		if (stage==null) {
			String errMsg = Strings.stageMissing;
			logger.severe(errMsg);						
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
		}				

		Stage stageObj = Utilities.strToStage(stage);

		if (stageObj==null) {			
			String errMsg =  String.format(Strings.illegalStage,stage)  + Constants.Stage.returnValues();
			logger.severe(errMsg);						
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
		}

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();	
		readWriteLock.readLock().lock();
		try {			

			@SuppressWarnings("unchecked")
			Map<String, Experiment> experimentsDB = (Map<String, Experiment>)context.getAttribute(Constants.EXPERIMENTS_DB_PARAM_NAME);

			Experiment exp = experimentsDB.get(experiment_id);

			if (exp == null) {
				String errMsg = Strings.experimentNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			//no need to validate experiments support since the experiment exists so it is supported in its season  

			JSONObject res = exp.getUtilitiesInfo(stageObj, context);				
			return Response.ok(res.toString()).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error getting experiment " + experiment_id + " utilities info: ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error getting experiment utilities information: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}


	@GET
	@Path ("/products/{product-id}/experiments")
	@ApiOperation(value = "Returns the experiments for the specified product ", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Product not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getExperiments(@PathParam("product-id")String product_id,
			@QueryParam("includeindexinginfo")Boolean includeIndexingInfo,						
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getExperiments request");
		}

		String err = Utilities.validateLegalUUID(product_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalProductUUID + err);

		//find relevant product
		ProductErrorPair prodErrPair = Utilities.getProduct(context, product_id);
		if (prodErrPair.error != null) {			
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		Product currentProduct = prodErrPair.product;

		//check user authorization
		UserInfo userInfo = UserInfo.validate("AnalyticsServices.getExperiments", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		if (includeIndexingInfo == null)
			includeIndexingInfo = false; 	

		//if (includeIndexingInfo && !isAnalyticsServerConfigured()) {
		//	return sendError(Status.BAD_REQUEST, Strings.analyticsServerNotConfigured);
		//}

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.EXPERIMENTS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.readLock().lock();
		try {

			@SuppressWarnings("unchecked")
			Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);			

			Product product = productsDB.get(product_id);

			if (product == null) {
				String errMsg = Strings.productNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();				
			}

			JSONObject experimentsJson = product.getExperimentsJson(OutputJSONMode.ADMIN, context);

			if (includeIndexingInfo) {
				ValidationResults res = null;
				try { 
					res = AnalyticsUtilities.getExperimentsFromAnalyticsServer(context);

					if (res!=null) { //res is null when no analytics server is configured
						if (res.status!=Status.OK && res.status!=Status.NOT_FOUND) {
							logger.severe(res.error);
							return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(res.error)).build();						
						}

						JSONObject indexingDataJson = null;
						if (res.status==Status.OK) {
							indexingDataJson = new JSONObject(res.error);						
						}

						experimentsJson = AnalyticsUtilities.mergeIndexingDataIntoExperimentsList(experimentsJson, indexingDataJson);									
					}
				} catch (Exception e) {
					err = Strings.failGetExperimentFromAnalyticsServer + e.getMessage();
					logger.severe(err);
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(err)).build();					
				}
			}

			return (Response.ok()).entity(experimentsJson.toString()).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error getting experiments for product " + product_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error getting experiments: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	@PUT
	@Path ("/products/experiments/{experiment-id}")	
	@ApiOperation(value = "Updates the specified experiment", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Experiment not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response updateExperiment(@PathParam("experiment-id")String experiment_id, String experiment,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("updateExperiment request: experiment_id =" + experiment_id +", experiment = " + experiment);
		}

		AirlockChange change = new AirlockChange();

		String err = Utilities.validateLegalUUID(experiment_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalExperimentUUID + err);

		//find relevant product
		ProductErrorPair prodErrPair = Utilities.getProductOfExperiment(context, experiment_id);
		if (prodErrPair.error != null) {			
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		Product currentProduct = prodErrPair.product;
		change.setProduct(currentProduct);

		//check user authorization
		UserInfo userInfo = UserInfo.validate("AnalyticsServices.updateExperiment", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.EXPERIMENTS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();	
		readWriteLock.writeLock().lock();
		try {

			@SuppressWarnings("unchecked")
			Map<String, Experiment> experimentsDB = (Map<String, Experiment>)context.getAttribute(Constants.EXPERIMENTS_DB_PARAM_NAME);

			@SuppressWarnings("unchecked")			
			Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);

			Experiment experimentToUpdate = experimentsDB.get(experiment_id);
			if (experimentToUpdate == null) {
				String errMsg = Strings.experimentNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();			
			}			

			Product product = productsDB.get(experimentToUpdate.getProductId().toString());
			if (product == null) {
				String errMsg = Strings.productNotFound;
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();				
			}

			//no need to validate experiments support since the experiment exists so it is supported in its season  			

			JSONObject updatedExperimentJSON = null;
			try {
				updatedExperimentJSON = new JSONObject(experiment);
			} catch (JSONException je) {
				String errMsg = Strings.illegalInputJSON + je.getMessage();
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}						

			//if not set - set the uniqueId to be the id path param
			if (!updatedExperimentJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) || updatedExperimentJSON.get(Constants.JSON_FIELD_UNIQUE_ID) == null) {
				updatedExperimentJSON.put(Constants.JSON_FIELD_UNIQUE_ID, experiment_id);
			}
			else {
				//verify that experiment-id in path is identical to uniqueId in request pay-load  
				if (!updatedExperimentJSON.getString(Constants.JSON_FIELD_UNIQUE_ID).equals(experiment_id)) {
					String errMsg = Strings.experimentWithDifferentId;
					logger.severe(errMsg);
					return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();					
				}
			}

			ValidationResults validationRes = experimentToUpdate.validateExperimentJSON(updatedExperimentJSON, context, userInfo);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			//only productLead or Administrator can update experiment in production
			if (!validRole(userInfo)) {
				ValidationResults validateProdDontChangeRes = experimentToUpdate.validateProductionDontChanged(updatedExperimentJSON);

				if (validateProdDontChangeRes!=null) {
					String errMsg = validateProdDontChangeRes.error;
					logger.severe(errMsg);
					return Response.status(validateProdDontChangeRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
				}					
			} 

			Stage prevStage = experimentToUpdate.getStage();
			String prevMinVersion = experimentToUpdate.getMinVersion();
			String prevMaxVersion = experimentToUpdate.getMaxVersion();

			boolean publishToAnalyticsSvrRequired = experimentToUpdate.isPublishToAnalyticsSvrRequired(updatedExperimentJSON, context);
			Date now = new Date();
			if (publishToAnalyticsSvrRequired) {
				ValidationResults pushingExpToAnalyticsSrvRes = null;
				try {
					//pushingExpToAnalyticsSrvRes = AnalyticsUtilities.updateAnalyticsServer(context, null, updatedExperimentJSON, now, null, null, null, null);				
					pushingExpToAnalyticsSrvRes = AnalyticsUtilities.updateExperimentInAnalyticsServer(context, experimentToUpdate, updatedExperimentJSON, now, null, null, null, null);
				} catch (Exception e) {
					pushingExpToAnalyticsSrvRes = new ValidationResults(Strings.failUpdatingExperiment + e.getMessage(), Status.INTERNAL_SERVER_ERROR);
				} 

				if (pushingExpToAnalyticsSrvRes!=null) {
					logger.severe(pushingExpToAnalyticsSrvRes.error);
					return Response.status(pushingExpToAnalyticsSrvRes.status).entity(Utilities.errorMsgToErrorJSON(pushingExpToAnalyticsSrvRes.error)).build();
				}
			}

			//finally - actually update the experiment.
			String updateDetails = experimentToUpdate.updateExperiment(updatedExperimentJSON, context, now);

			if (!updateDetails.isEmpty()) { //if some fields were changed	
				try {
					//ProductServices.writeAnalytics(season, context);
					//if the experiment is in prod, or changed to/from production - we'll call it production change
					Stage changeStage = (prevStage == Stage.DEVELOPMENT && experimentToUpdate.getStage() == Stage.DEVELOPMENT)? Stage.DEVELOPMENT : Stage.PRODUCTION;
					//AirlockFilesWriter.writeProducts(productsDB, context);
					change.getFiles().addAll(AirlockFilesWriter.writeExperiments(product, context, changeStage));

					ArrayList<Season> seasonsInUpdatedExpRange = product.getSeasonsWithinRange(experimentToUpdate.getMinVersion(), experimentToUpdate.getMaxVersion());
					Map<String, BaseAirlockItem> airlockItemsDB = Utilities.getAirlockItemsDB(Constants.MASTER_BRANCH_NAME, context);
					
					for (Season season:seasonsInUpdatedExpRange) {
						Environment env = new Environment();
						env.setAirlockItemsDB(airlockItemsDB);
						env.setServerVersion(season.getServerVersion());
						env.setBranchId(Constants.MASTER_BRANCH_NAME);
						//write the updated experiment in the runtime files
						change.getFiles().addAll(AirlockFilesWriter.doWriteFeatures(season, OutputJSONMode.RUNTIME_DEVELOPMENT, context, changeStage, env));
						if (experimentToUpdate.getStage().equals(Stage.PRODUCTION) || prevStage.equals(Stage.PRODUCTION))
							change.getFiles().addAll(AirlockFilesWriter.doWriteFeatures(season, OutputJSONMode.RUNTIME_PRODUCTION, context, changeStage, env));
					}

					//the min/max version may be updated - update seasons that are no longer in the exp as well
					ArrayList<Season> seasonsInPrevExpRange = product.getSeasonsWithinRange(prevMinVersion, prevMaxVersion);
					
					for (Season season:seasonsInPrevExpRange) {
						boolean alreadyWritten = false;
						for (Season s:seasonsInUpdatedExpRange) {
							if (s.getUniqueId().equals(season.getUniqueId())) {
								alreadyWritten = true;
								break;
							}
						}
						if (!alreadyWritten) {
							Environment env = new Environment();
							env.setAirlockItemsDB(airlockItemsDB);
							env.setServerVersion(season.getServerVersion());
							env.setBranchId(Constants.MASTER_BRANCH_NAME);
							
							//write the updated experiment in the runtime files
							change.getFiles().addAll(AirlockFilesWriter.doWriteFeatures(season, OutputJSONMode.RUNTIME_DEVELOPMENT, context, changeStage, env));
							if (experimentToUpdate.getStage().equals(Stage.PRODUCTION) || prevStage.equals(Stage.PRODUCTION))
								change.getFiles().addAll(AirlockFilesWriter.doWriteFeatures(season, OutputJSONMode.RUNTIME_PRODUCTION, context, changeStage, env));
						}
					}
					Webhooks.get(context).notifyChanges(change, context);

				} catch (IOException e) {
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
				}

				AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
				auditLogWriter.log("Update experiment: " + experimentToUpdate.getName() + ", " + experiment_id + ":   " + updateDetails, userInfo);

			}

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Updated experiment: " + experimentToUpdate.toJson(OutputJSONMode.ADMIN, context, false, false) + "\n updatd details: " + updateDetails);
			}
			Utilities.sendEmailForExperiment(context,experimentToUpdate,null,product.getUniqueId().toString(),"updated",updateDetails,experimentToUpdate.getStage().equals(Stage.PRODUCTION) || prevStage.equals(Stage.PRODUCTION),userInfo);

			return (Response.ok(experimentToUpdate.toJson(OutputJSONMode.ADMIN).toString())).build();
		} catch (MergeException e) {
			err = Strings.mergeException  + e.getMessage();
			logger.severe(err);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(err)).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error updating experiment " + experiment_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error updating experiment: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}	

	@PUT
	@Path ("/products/{product-id}/experiments")
	@ApiOperation(value = "Updates the specified product's experiments", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Product not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response updateProductExperiments(@PathParam("product-id")String product_id, String experiments,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("updateProductExperiments request: experiment_id =" + product_id +", experiments = " + experiments);
		}
		AirlockChange change = new AirlockChange();

		String err = Utilities.validateLegalUUID(product_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalProductUUID + err);


		//find relevant product
		ProductErrorPair prodErrPair = Utilities.getProduct(context, product_id);
		if (prodErrPair.error != null) {			
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		Product currentProduct = prodErrPair.product;
		change.setProduct(currentProduct);

		//check user authorization
		UserInfo userInfo = UserInfo.validate("AnalyticsServices.updateProductExperiments", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.EXPERIMENTS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.writeLock().lock();
		try {
			@SuppressWarnings("unchecked")			
			Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);

			Product product = productsDB.get(product_id);
			if (product == null) {
				String errMsg = Strings.productNotFound;
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();				
			}

			JSONObject updatedExperimentsJSON = null;
			try {
				updatedExperimentsJSON = new JSONObject(experiments);
			} catch (JSONException je) {
				String errMsg = Strings.illegalInputJSON + je.getMessage();
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}						

			ValidationResults validationRes = product.getExperimentsMutualExclusionGroup().validateExpMutualExclusionGroupJSON(updatedExperimentsJSON, context, userInfo);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			ValidationResults validateProdDontChangeRes = product.getExperimentsMutualExclusionGroup().validateProductionDontChanged(updatedExperimentsJSON);

			boolean productionChanged = (validateProdDontChangeRes!=null);
			Stage changeStage = productionChanged ? Stage.PRODUCTION : Stage.DEVELOPMENT;
			//only productLead or Administrator can update experiment in production

			if (!validRole(userInfo)) {				
				if (validateProdDontChangeRes!=null) {
					String errMsg = validateProdDontChangeRes.error;
					logger.severe(errMsg);
					return Response.status(validateProdDontChangeRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
				}					
			} 

			Date now = new Date();
			//finally - actually update the experiments list.
			String updateDetails = product.getExperimentsMutualExclusionGroup().updateExpMutualExclusionGroup(updatedExperimentsJSON, context, now);

			if (!updateDetails.isEmpty()) { //if some fields were changed		
				try {
					//ProductServices.writeAnalytics(season, context);
					//AirlockFilesWriter.writeProducts(productsDB, context);
					change.setProduct(product);
					change.getFiles().addAll(AirlockFilesWriter.writeExperiments(product, context, changeStage));

					// If the order of the experiments or the maxExpOn was updated and exp in prod was effected - the prod
					//runtime of all seasons in prod will be rewritten (as well as the dev runtimes)
					Map<String, BaseAirlockItem> airlockItemsDB = Utilities.getAirlockItemsDB(Constants.MASTER_BRANCH_NAME, context);
					
					for (Season season:product.getSeasons()) {
						Environment env = new Environment();
						env.setAirlockItemsDB(airlockItemsDB);
						env.setServerVersion(season.getServerVersion());
						env.setBranchId(Constants.MASTER_BRANCH_NAME);
						
						//write the updated experiment in the runtime files
						change.getFiles().addAll(AirlockFilesWriter.doWriteFeatures(season, OutputJSONMode.RUNTIME_DEVELOPMENT, context, changeStage, env));
						if (productionChanged)
							change.getFiles().addAll(AirlockFilesWriter.doWriteFeatures(season, OutputJSONMode.RUNTIME_PRODUCTION, context, changeStage, env));
					}
					Webhooks.get(context).notifyChanges(change, context);
				} catch (IOException e) {
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
				}

				AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
				auditLogWriter.log("Update experiment of product: " + product.getName() + ", " + product_id + ":   " + updateDetails, userInfo);

			}

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Updated experiment of product: " + product.getName() + ", " + product_id + ": \n updatd details: " + updateDetails);
			}

			if(productionChanged){
				Utilities.sendEmailForAllExperiments(context,product_id,updateDetails,userInfo);
			}
			return (Response.ok(product.getExperimentsMutualExclusionGroup().toJson(OutputJSONMode.ADMIN, context, false).toString())).build();
		} catch (MergeException e) {
			err = Strings.mergeException  + e.getMessage();
			logger.severe(err);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(err)).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error updating product " + product_id + " experiments: ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error updating experiments: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}	

	@DELETE
	@Path ("/products/experiments/{experiment-id}")	
	@ApiOperation(value = "Deletes the specified experiment")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Experiment not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response deleteExperiment(@PathParam("experiment-id")String experiment_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException  {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("deleteExperiment request: experiment_id =" + experiment_id);
		}
		AirlockChange change = new AirlockChange();
		String err = Utilities.validateLegalUUID(experiment_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalExperimentUUID + err);

		//find relevant product
		ProductErrorPair prodErrPair = Utilities.getProductOfExperiment(context, experiment_id);
		if (prodErrPair.error != null) {			
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		Product currentProduct = prodErrPair.product;
		change.setProduct(currentProduct);
		//check user authorization		
		UserInfo userInfo = UserInfo.validate("AnalyticsServices.deleteExperiment", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.EXPERIMENTS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();	
		readWriteLock.writeLock().lock();
		try {		

			@SuppressWarnings("unchecked")
			Map<String, Experiment> experimentsDB = (Map<String, Experiment>)context.getAttribute(Constants.EXPERIMENTS_DB_PARAM_NAME);

			@SuppressWarnings("unchecked")			
			Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);

			@SuppressWarnings("unchecked")
			Map<String, Variant> variantsDB = (Map<String, Variant>)context.getAttribute(Constants.VARIANTS_DB_PARAM_NAME);

			Experiment experimentToDelete = experimentsDB.get(experiment_id);
			if (experimentToDelete == null) {
				String errMsg = Strings.experimentNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();			
			}			

			//no need to validate experiments support since the experiment exists so it is supported in its season  

			if (experimentToDelete.getStage() == Stage.PRODUCTION) {
				String errMsg = Strings.expInProd;
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			Product product = productsDB.get(experimentToDelete.getProductId().toString());
			if (product == null) {
				String errMsg = Strings.productNotFound;
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();				
			}

			//publish experiment deletion to analytics server
			ValidationResults pushingExpToAnalyticsSrvRes = null;
			try {
				//pushingExpToAnalyticsSrvRes = AnalyticsUtilities.updateAnalyticsServer(context, experimentToDelete.getUniqueId(), null, now, null, null, null, null);				
				pushingExpToAnalyticsSrvRes = AnalyticsUtilities.deleteExperimentFromAnalyticsServer(context, experiment_id);
			} catch (Exception e) {
				pushingExpToAnalyticsSrvRes = new ValidationResults(Strings.failDeletingExperiment + e.getMessage(), Status.INTERNAL_SERVER_ERROR);
			} 

			if (pushingExpToAnalyticsSrvRes!=null) {
				logger.severe(pushingExpToAnalyticsSrvRes.error);
				return Response.status(pushingExpToAnalyticsSrvRes.status).entity(Utilities.errorMsgToErrorJSON(pushingExpToAnalyticsSrvRes.error)).build();
			}

			//actually removing the experiment
			String errorString = product.removeExperiment(experimentToDelete.getUniqueId());
			if (errorString!=null) {
				//should not happen - will be returned if experiment is not in product
				logger.severe(errorString);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errorString)).build();
			}

			experimentsDB.remove(experiment_id);

			//remove all the experiment's variants (branches are not removed)
			for (Variant variant:experimentToDelete.getVariants()) {
				variantsDB.remove(variant.getUniqueId().toString());
			}

			try {
				//AirlockFilesWriter.writeProducts(productsDB, context);
				change.setProduct(product);
				change.getFiles().addAll(AirlockFilesWriter.writeExperiments(product, context, Stage.DEVELOPMENT));
				
				Map<String, BaseAirlockItem> airlockItemsDB = Utilities.getAirlockItemsDB(Constants.MASTER_BRANCH_NAME, context);
				
				for (Season season:product.getSeasonsWithinRange(experimentToDelete.getMinVersion(), experimentToDelete.getMaxVersion())) {
					Environment env = new Environment();
					env.setAirlockItemsDB(airlockItemsDB);
					env.setServerVersion(season.getServerVersion());
					env.setBranchId(Constants.MASTER_BRANCH_NAME);
					
					//update the runtime files
					change.getFiles().addAll(AirlockFilesWriter.doWriteFeatures(season, OutputJSONMode.RUNTIME_DEVELOPMENT, context, Stage.DEVELOPMENT, env));
					if (experimentToDelete.getStage().equals(Stage.PRODUCTION))
						change.getFiles().addAll(AirlockFilesWriter.doWriteFeatures(season, OutputJSONMode.RUNTIME_PRODUCTION, context, Stage.DEVELOPMENT, env));

				}
				Webhooks.get(context).notifyChanges(change, context);
			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}

			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Delete experiment: name:" + experimentToDelete.getName() + ", uniqueId: " + experiment_id, userInfo);

			logger.info("Experiment " + experimentToDelete.getName() + ", uniqueId:" + experiment_id + " was deleted");
			Utilities.sendEmailForExperiment(context,experimentToDelete,null,product.getUniqueId().toString(),"created",null,experimentToDelete.getStage().equals(Stage.PRODUCTION),userInfo);

			return (Response.ok()).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error deleting experiment " + experiment_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error deleting experiment: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}	


	@POST 
	@Path ("/products/experiments/{experiment-id}/variants")
	@ApiOperation(value = "Creates variant within the specified experiment", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Experiment not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response addVariant(@PathParam("experiment-id")String experiment_id, String newVariant,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("addVariant request: experiment_id = " + experiment_id + ", newVariant = " + newVariant);
		}
		AirlockChange change = new AirlockChange();
		String err = Utilities.validateLegalUUID(experiment_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalExperimentUUID + err);

		//find relevant product
		ProductErrorPair prodErrPair = Utilities.getProductOfExperiment(context, experiment_id);
		if (prodErrPair.error != null) {			
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		Product currentProduct = prodErrPair.product;
		change.setProduct(currentProduct);
		//check user authorization
		UserInfo userInfo = UserInfo.validate("AnalyticsServices.addVariant", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.EXPERIMENTS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();	
		readWriteLock.writeLock().lock();
		try {				
			@SuppressWarnings("unchecked")
			Map<String, Experiment> experimentsDB = (Map<String, Experiment>)context.getAttribute(Constants.EXPERIMENTS_DB_PARAM_NAME);

			//Validate experiment existence
			Experiment experiment = experimentsDB.get(experiment_id);

			if (experiment == null) {
				String errMsg = Strings.experimentNotFound;
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();				
			}	

			@SuppressWarnings("unchecked")
			Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);


			//Validate season existence
			Product product = productsDB.get(experiment.getProductId().toString());

			if (product == null) {
				String errMsg = Strings.productNotFound;
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();				
			}		

			//no need to validate experiments support since the experiment exists so it is supported in its season  							

			//validate that is a legal JSON
			JSONObject newVariantJSON = null;
			try {
				newVariantJSON = new JSONObject(newVariant);
			} catch (JSONException je) {
				String errMsg = Strings.illegalInputJSON + je.getMessage();
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();				
			}

			//verify that JSON does not contain uniqueId field
			if (newVariantJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && newVariantJSON.get(Constants.JSON_FIELD_UNIQUE_ID) !=null) {
				String errMsg = Strings.variantWithId;
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();							
			}


			//verify that JSON does not contain different experiment-id then the path parameter
			if (newVariantJSON.containsKey(Constants.JSON_FIELD_EXPERIMENT_ID) && newVariantJSON.get(Constants.JSON_FIELD_EXPERIMENT_ID) !=null) {
				if (!experiment_id.equals(newVariantJSON.getString(Constants.JSON_FIELD_EXPERIMENT_ID))) {
					String errMsg = Strings.variantExperimentWithDifferentId;
					logger.severe(errMsg);
					return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();									
				}
			}
			else {		
				newVariantJSON.put(Constants.JSON_FIELD_EXPERIMENT_ID, experiment_id);
			}

			Variant newVariantObj = new Variant(experiment.getUniqueId());

			ValidationResults validationRes = newVariantObj.validateVariantJSON(newVariantJSON, context, userInfo, experiment.getMinVersion(), experiment.getMaxVersion(), true);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			newVariantObj.fromJSON(newVariantJSON, context);
			newVariantObj.setUniqueId(UUID.randomUUID());

			experiment.addVariant(newVariantObj);

			ValidationResults pushingExpToAnalyticsSrvRes = null;
			try {
				Date now = new Date();
				//pushingExpToAnalyticsSrvRes = AnalyticsUtilities.updateAnalyticsServer(context, null, null, now, null, null, null, null);
				pushingExpToAnalyticsSrvRes = AnalyticsUtilities.updateExperimentInAnalyticsServer(context, experiment, null, now, null, null, null, null);
			} catch (Exception e) {
				pushingExpToAnalyticsSrvRes = new ValidationResults(Strings.failUpdatingExperiment + e.getMessage(), Status.INTERNAL_SERVER_ERROR);
			} 

			if (pushingExpToAnalyticsSrvRes!=null) {
				experiment.removeVariant(newVariantObj.getUniqueId());				
				logger.severe(pushingExpToAnalyticsSrvRes.error);
				return Response.status(pushingExpToAnalyticsSrvRes.status).entity(Utilities.errorMsgToErrorJSON(pushingExpToAnalyticsSrvRes.error)).build();
			}			

			try {
				//AirlockFilesWriter.writeProducts(productsDB, context);
				change.setProduct(product);
				change.getFiles().addAll(AirlockFilesWriter.writeExperiments(product, context, newVariantObj.getStage()));				
				Map<String, BaseAirlockItem> airlockItemsDB = Utilities.getAirlockItemsDB(Constants.MASTER_BRANCH_NAME, context);
				
				for (Season season:product.getSeasonsWithinRange(experiment.getMinVersion(), experiment.getMaxVersion())) {
					Environment env = new Environment();
					env.setAirlockItemsDB(airlockItemsDB);
					env.setServerVersion(season.getServerVersion());
					env.setBranchId(Constants.MASTER_BRANCH_NAME);
					
					//write the new variant in the runtime files
					change.getFiles().addAll(AirlockFilesWriter.doWriteFeatures(season, OutputJSONMode.RUNTIME_DEVELOPMENT, context, Stage.DEVELOPMENT, env));
					if (newVariantObj.getStage().equals(Stage.PRODUCTION))
						change.getFiles().addAll(AirlockFilesWriter.doWriteFeatures(season, OutputJSONMode.RUNTIME_PRODUCTION, context, newVariantObj.getStage(), env));
				}
				Webhooks.get(context).notifyChanges(change, context);
			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}

			@SuppressWarnings("unchecked")
			Map<String, Variant> variantsDB = (Map<String, Variant>)context.getAttribute(Constants.VARIANTS_DB_PARAM_NAME);
			variantsDB.put(newVariantObj.getUniqueId().toString(), newVariantObj);

			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Create new variant: " + newVariantObj.getName() + ", " + newVariantObj.getUniqueId() + ":" + newVariantObj.toJson(OutputJSONMode.ADMIN, context).toString() + " to experiment: " + experiment.getName() + ", " + experiment_id, userInfo); 

			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FIELD_UNIQUE_ID, newVariantObj.getUniqueId().toString());
			res.put(Constants.JSON_FIELD_NAME, newVariantObj.getName());
			res.put(Constants.JSON_FIELD_LAST_MODIFIED, newVariantObj.getLastModified().getTime());

			logger.info("Variant added to experiment '"+  experiment_id + "': "+ res.toString());

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Added variant: " + newVariantObj.toJson(OutputJSONMode.ADMIN, context));
			}
			String details = " Variant "+newVariantObj.getName()+" added to experiment "+ experiment.getName()+ " with branch "+ newVariantObj.getBranchName();
			Utilities.sendEmailForVariant(context,newVariantObj,null,product.getUniqueId().toString(),details,newVariantObj.getStage().equals(Stage.PRODUCTION),userInfo);
			return (Response.ok(res.toString())).build();
		} catch (MergeException e) {
			err = Strings.mergeException  + e.getMessage();
			logger.severe(err);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(err)).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error adding variant to experiment " + experiment_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error adding variant: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	@GET
	@Path ("/products/experiments/variants/{variant-id}") 
	@ApiOperation(value = "Returns the specified experiment", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Feature not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getVariant(@PathParam("variant-id")String variant_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getVariant request");
		}

		String err = Utilities.validateLegalUUID(variant_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalVariantUUID + err);

		//find relevant product
		ProductErrorPair prodErrPair = Utilities.getProductOfVariant(context, variant_id);
		if (prodErrPair.error != null) {			
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		Product currentProduct = prodErrPair.product;

		//check user authorization
		UserInfo userInfo = UserInfo.validate("AnalyticsServices.getVariant", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.EXPERIMENTS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();	
		readWriteLock.readLock().lock();
		try {			

			@SuppressWarnings("unchecked")
			Map<String, Variant> variantsDB = (Map<String, Variant>)context.getAttribute(Constants.VARIANTS_DB_PARAM_NAME);

			Variant variant = variantsDB.get(variant_id);

			if (variant == null) {
				String errMsg = Strings.variantNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			//no need to validate experiments support since the experiment and variant exists so it is supported in its season  

			return Response.ok(variant.toJson(OutputJSONMode.ADMIN, context).toString()).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error getting variant " + variant_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error getting variant: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	@DELETE
	@Path ("/products/experiments/variants/{variant-id}")
	@ApiOperation(value = "Deletes the specified variant")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Variant not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response deleteVariant(@PathParam("variant-id")String variant_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException  {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("deleteVariant request: variant_id =" + variant_id);
		}
		AirlockChange change = new AirlockChange();

		String err = Utilities.validateLegalUUID(variant_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalVariantUUID + err);

		//find relevant product
		ProductErrorPair prodErrPair = Utilities.getProductOfVariant(context, variant_id);
		if (prodErrPair.error != null) {			
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		Product currentProduct = prodErrPair.product;
		change.setProduct(currentProduct);

		//check user authorization
		UserInfo userInfo = UserInfo.validate("AnalyticsServices.deleteVariant", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.EXPERIMENTS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.writeLock().lock();
		try {		

			@SuppressWarnings("unchecked")
			Map<String, Variant> variantsDB = (Map<String, Variant>)context.getAttribute(Constants.VARIANTS_DB_PARAM_NAME);

			@SuppressWarnings("unchecked")
			Map<String, Experiment> experimentsDB = (Map<String, Experiment>)context.getAttribute(Constants.EXPERIMENTS_DB_PARAM_NAME);

			@SuppressWarnings("unchecked")			
			Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);

			Variant variantToDelete = variantsDB.get(variant_id);
			if (variantToDelete == null) {
				String errMsg = Strings.variantNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();			
			}			

			//no need to validate experiments support since the experiment exists so it is supported in its season  

			if (variantToDelete.getStage() == Stage.PRODUCTION) {
				String errMsg = Strings.variantInProd;
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			Experiment experiment = experimentsDB.get(variantToDelete.getExperimentId().toString());
			if (experiment == null) {
				String errMsg = Strings.experimentNotFound;
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();				
			}

			Product product = productsDB.get(experiment.getProductId().toString());
			if (product == null) {
				String errMsg = Strings.productNotFound;
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();				
			}

			//publish experiment deletion to analytics server
			ValidationResults pushingExpToAnalyticsSrvRes = null;
			Date now = new Date();
			try {
				//pushingExpToAnalyticsSrvRes = AnalyticsUtilities.updateAnalyticsServer(context, variantToDelete.getUniqueId(), null, now, variantToDelete.getUniqueId(), null,  null, null);				
				pushingExpToAnalyticsSrvRes = AnalyticsUtilities.updateExperimentInAnalyticsServer(context, experiment, null, now, variantToDelete.getUniqueId(), null, null, null);
			} catch (Exception e) {
				pushingExpToAnalyticsSrvRes = new ValidationResults(Strings.failUpdatingExperiment+ e.getMessage(), Status.INTERNAL_SERVER_ERROR);
			} 

			if (pushingExpToAnalyticsSrvRes!=null) {
				logger.severe(pushingExpToAnalyticsSrvRes.error);
				return Response.status(pushingExpToAnalyticsSrvRes.status).entity(Utilities.errorMsgToErrorJSON(pushingExpToAnalyticsSrvRes.error)).build();
			}

			String errorString = experiment.removeVariant(variantToDelete.getUniqueId());
			if (errorString!=null) {
				logger.severe(errorString);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errorString)).build();
			}

			variantsDB.remove(variant_id);
			experiment.removeVariantFromControlGroup(variantToDelete.getName());

			try {
				//AirlockFilesWriter.writeProducts(productsDB, context);
				change.setProduct(product);
				change.getFiles().addAll(AirlockFilesWriter.writeExperiments(product, context, variantToDelete.getStage()));

				Map<String, BaseAirlockItem> airlockItemsDB = Utilities.getAirlockItemsDB(Constants.MASTER_BRANCH_NAME, context);
				
				for (Season season:product.getSeasonsWithinRange(experiment.getMinVersion(), experiment.getMaxVersion())) {
					Environment env = new Environment();
					env.setAirlockItemsDB(airlockItemsDB);
					env.setServerVersion(season.getServerVersion());
					env.setBranchId(Constants.MASTER_BRANCH_NAME);
					
					//update the runtime files
					change.getFiles().addAll(AirlockFilesWriter.doWriteFeatures(season, OutputJSONMode.RUNTIME_DEVELOPMENT, context, Stage.DEVELOPMENT, env));
					if (variantToDelete.getStage().equals(Stage.PRODUCTION))
						change.getFiles().addAll(AirlockFilesWriter.doWriteFeatures(season, OutputJSONMode.RUNTIME_PRODUCTION, context, variantToDelete.getStage(), env));
				}
				Webhooks.get(context).notifyChanges(change, context);
			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}

			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Delete variant: name:" + variantToDelete.getName() + ", uniqueId: " + variant_id, userInfo);

			logger.info("Variant " + variantToDelete.getName() + ", uniqueId:" + variant_id + " was deleted");
			//TO DO: useless without variant followers as you can't remove in prod
			String details = " Variant "+variantToDelete.getName()+" removed from experiment "+ experiment.getName()+ " with branch "+ variantToDelete.getBranchName();
			Utilities.sendEmailForVariant(context,variantToDelete,null,product.getUniqueId().toString(),details,variantToDelete.getStage().equals(Stage.PRODUCTION),userInfo);

			return (Response.ok()).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error deleting variant " + variant_id + " : ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error deleting variant: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}	

	@PUT
	@Path ("/products/experiments/variants/{variant-id}")
	@ApiOperation(value = "Updates the specified variant", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Variant not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response updateVariant(@PathParam("variant-id")String variant_id, String variant,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("updateVariant request: variant_id =" + variant_id +", variant = " + variant);
		}
		AirlockChange change = new AirlockChange();
		String err = Utilities.validateLegalUUID(variant_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalVariantUUID + err);

		//find relevant product
		ProductErrorPair prodErrPair = Utilities.getProductOfVariant(context, variant_id);
		if (prodErrPair.error != null) {			
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		Product currentProduct = prodErrPair.product;
		change.setProduct(currentProduct);

		//check user authorization
		UserInfo userInfo = UserInfo.validate("AnalyticsServices.updateVariant", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.EXPERIMENTS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.writeLock().lock();
		try {

			@SuppressWarnings("unchecked")
			Map<String, Experiment> experimentsDB = (Map<String, Experiment>)context.getAttribute(Constants.EXPERIMENTS_DB_PARAM_NAME);

			@SuppressWarnings("unchecked")			
			Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);

			@SuppressWarnings("unchecked")
			Map<String, Variant> variantsDB = (Map<String, Variant>)context.getAttribute(Constants.VARIANTS_DB_PARAM_NAME);

			Variant variantToUpdate = variantsDB.get(variant_id);
			if (variantToUpdate == null) {
				String errMsg = Strings.variantNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();			
			}			

			Experiment experiment = experimentsDB.get(variantToUpdate.getExperimentId().toString());
			if (experiment == null) {
				String errMsg = Strings.experimentNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();			
			}	

			Product product = productsDB.get(experiment.getProductId().toString());
			if (product == null) {
				String errMsg = Strings.productNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();			
			}	

			//no need to validate experiments support since the experiment exists so it is supported in its season  			

			JSONObject updatedVariantJSON = null;
			try {
				updatedVariantJSON = new JSONObject(variant);
			} catch (JSONException je) {
				String errMsg = Strings.illegalInputJSON + je.getMessage();
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}						

			//if not set - set the uniqueId to be the id path param
			if (!updatedVariantJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) || updatedVariantJSON.get(Constants.JSON_FIELD_UNIQUE_ID) == null) {
				updatedVariantJSON.put(Constants.JSON_FIELD_UNIQUE_ID, variant_id);
			}
			else {
				//verify that variant-id in path is identical to uniqueId in request pay-load  
				if (!updatedVariantJSON.getString(Constants.JSON_FIELD_UNIQUE_ID).equals(variant_id)) {
					String errMsg = Strings.variantWithDifferentId;
					logger.severe(errMsg);
					return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();					
				}
			}

			ValidationResults validationRes = variantToUpdate.validateVariantJSON(updatedVariantJSON, context, userInfo, experiment.getMinVersion(), experiment.getMaxVersion(), true);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}


			//only productLead or Administrator can update experiment in production

			if (!validRole(userInfo)) {
				ValidationResults validateProdDontChangeRes = variantToUpdate.validateProductionDontChanged(updatedVariantJSON);

				if (validateProdDontChangeRes!=null) {
					String errMsg = validateProdDontChangeRes.error;
					logger.severe(errMsg);
					return Response.status(validateProdDontChangeRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
				}					
			} 

			Stage prevStage = variantToUpdate.getStage();

			boolean publishToAnalyticsSvrRequired = variantToUpdate.isPublishToAnalyticsSvrRequired(updatedVariantJSON);
			Date now = new Date();
			if (publishToAnalyticsSvrRequired) {
				ValidationResults pushingExpToAnalyticsSrvRes = null;
				try {
					pushingExpToAnalyticsSrvRes = AnalyticsUtilities.updateExperimentInAnalyticsServer(context, experiment, null, now, variantToUpdate.getUniqueId(), updatedVariantJSON, null, null);
				} catch (Exception e) {
					pushingExpToAnalyticsSrvRes = new ValidationResults(Strings.failUpdatingExperiment + e.getMessage(), Status.INTERNAL_SERVER_ERROR);
				} 

				if (pushingExpToAnalyticsSrvRes!=null) {
					logger.severe(pushingExpToAnalyticsSrvRes.error);
					return Response.status(pushingExpToAnalyticsSrvRes.status).entity(Utilities.errorMsgToErrorJSON(pushingExpToAnalyticsSrvRes.error)).build();
				}
			}

			//finally - actually update the variant.
			String updateDetails = variantToUpdate.updateVariant(updatedVariantJSON, context);
			Stage changeStage = (prevStage == Stage.DEVELOPMENT && variantToUpdate.getStage() == Stage.DEVELOPMENT)?
					Stage.DEVELOPMENT : Stage.PRODUCTION;
			if (!updateDetails.isEmpty()) { //if some fields were changed		
				try {
					change.setProduct(product);
					//AirlockFilesWriter.writeProducts(productsDB, context);
					change.getFiles().addAll(AirlockFilesWriter.writeExperiments(product, context, changeStage));

					Map<String, BaseAirlockItem> airlockItemsDB = Utilities.getAirlockItemsDB(Constants.MASTER_BRANCH_NAME, context);
					
					for (Season season:product.getSeasonsWithinRange(experiment.getMinVersion(), experiment.getMaxVersion())) {
						Environment env = new Environment();
						env.setAirlockItemsDB(airlockItemsDB);
						env.setServerVersion(season.getServerVersion());
						env.setBranchId(Constants.MASTER_BRANCH_NAME);
						
						//write the updated experiment in the runtime files
						change.getFiles().addAll(AirlockFilesWriter.doWriteFeatures(season, OutputJSONMode.RUNTIME_DEVELOPMENT, context, Stage.DEVELOPMENT, env));
						if (variantToUpdate.getStage().equals(Stage.PRODUCTION) || prevStage.equals(Stage.PRODUCTION))
							change.getFiles().addAll(AirlockFilesWriter.doWriteFeatures(season, OutputJSONMode.RUNTIME_PRODUCTION, context, Stage.PRODUCTION, env));					
					}
					Webhooks.get(context).notifyChanges(change, context);
				} catch (IOException e) {
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
				}

				AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
				auditLogWriter.log("Update variant: " + variantToUpdate.getName() + ", " + variant_id + ":   \n" + updateDetails, userInfo);
			}

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Updated variant: " + variantToUpdate.toJson(OutputJSONMode.ADMIN, context) + "\n update details: " + updateDetails);
			}
			Utilities.sendEmailForVariant(context,variantToUpdate,null,product.getUniqueId().toString(),updateDetails,variantToUpdate.getStage().equals(Stage.PRODUCTION),userInfo);

			return (Response.ok(variantToUpdate.toJson(OutputJSONMode.ADMIN, context).toString())).build();
		} catch (MergeException e) {
			err = Strings.mergeException  + e.getMessage();
			logger.severe(err);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(err)).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error updating variant " + variant_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error updating variant: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}	

	@GET
	@Path ("/products/experiments/{experiment-id}/availablebranches")	
	@ApiOperation(value = "Returns the available branches of the specified experiment", response = String.class) 
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Feature not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getExperimentAvailableBranches(@PathParam("experiment-id")String experiment_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getExperiment request");
		}

		String err = Utilities.validateLegalUUID(experiment_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalExperimentUUID + err);

		//find relevant product
		ProductErrorPair prodErrPair = Utilities.getProductOfExperiment(context, experiment_id);
		if (prodErrPair.error != null) {			
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		Product currentProduct = prodErrPair.product;

		//check user authorization
		UserInfo userInfo = UserInfo.validate("AnalyticsServices.getExperiment", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.EXPERIMENTS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();	
		readWriteLock.readLock().lock();
		try {			

			@SuppressWarnings("unchecked")
			Map<String, Experiment> experimentsDB = (Map<String, Experiment>)context.getAttribute(Constants.EXPERIMENTS_DB_PARAM_NAME);

			Experiment exp = experimentsDB.get(experiment_id);

			if (exp == null) {
				String errMsg = Strings.experimentNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			//no need to validate experiments support since the experiment exists so it is supported in its season  
			JSONObject res = exp.getAvailbaleBranches(context);			

			return Response.ok(res.toString()).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error getting experiment " + experiment_id + " available branches: ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error getting experiment available branches: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	//merge analytics of: for each season the master + all branches analytics
	@GET
	@Path ("/globalDataCollection/experiments/{experiment-id}")
	@ApiOperation(value = "Returns the analytics of the given experiment", response = String.class) 
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Experiment not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getExperimentGlobalDataCollection(@PathParam("experiment-id")String experiment_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getExperimentGlobalDataCollection request");
		}

		String err = Utilities.validateLegalUUID(experiment_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalExperimentUUID + err);

		//find relevant product
		ProductErrorPair prodErrPair = Utilities.getProductOfExperiment(context, experiment_id);
		if (prodErrPair.error != null) {			
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		Product currentProduct = prodErrPair.product;

		//check user authorization
		UserInfo userInfo = UserInfo.validate("AnalyticsServices.getExperimentGlobalDataCollection", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.EXPERIMENTS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();	
		readWriteLock.readLock().lock();
		try {			

			@SuppressWarnings("unchecked")
			Map<String, Experiment> experimentsDB = (Map<String, Experiment>)context.getAttribute(Constants.EXPERIMENTS_DB_PARAM_NAME);

			Experiment exp = experimentsDB.get(experiment_id);
			if (exp == null) {
				String errMsg = Strings.experimentNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}



			JSONObject res = exp.getExperimentAnalyticsJson(context); 

			return (Response.ok()).entity(res.toString()).build();	

		} catch (MergeException e) {
			err = Strings.mergeException  + e.getMessage();
			logger.severe(err);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(err)).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error getting experiment " + experiment_id + " analytics: ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error getting experiment analytics: " + e.getMessage());
		}  finally {
			readWriteLock.readLock().unlock();
		}
	}

	private LinkedList<AirlockChangeContent> writeAnalyticsChangeToS3(Environment env, Season season, boolean isProductionChange, Branch branch, ServletContext contxet) throws IOException, JSONException, MergeException{
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>(); 
		if (env.isInMaster()) {
			changesArr.addAll(AirlockFilesWriter.writeAnalytics(season, context, Constants.MASTER_BRANCH_NAME, env, (isProductionChange)? Stage.PRODUCTION : Stage.DEVELOPMENT));	
			changesArr.addAll(AirlockFilesWriter.doWriteFeatures (season, OutputJSONMode.RUNTIME_DEVELOPMENT, context,Stage.DEVELOPMENT, env)); //write the dev runtime files
			if (isProductionChange)
				changesArr.addAll(AirlockFilesWriter.doWriteFeatures (season, OutputJSONMode.RUNTIME_PRODUCTION, context, Stage.PRODUCTION, env)); //write the prod runtime files
		}
		else {
			changesArr.addAll(AirlockFilesWriter.writeAnalytics(season, context, env.getBranchId(), env, (isProductionChange)? Stage.PRODUCTION : Stage.DEVELOPMENT));			
			changesArr.addAll(AirlockFilesWriter.writeBranchAndMasterRuntimeFiles(season, branch, context, env, isProductionChange));
		}

		//if the season is in experiment update runtime files of all other seasons in experiment since exp analytics was changed 

		if (branch!=null) { //if the analytics change was in branch it is counted as prod change only if the branch is attached to variant in production
			Stage varianStage = ProductServices.isBranchInExp(branch, season, context); //return null if not in exp. If in exp return the max variant stage
			if (varianStage!=null) {			
				isProductionChange = varianStage.equals(Stage.PRODUCTION) && isProductionChange;
			}
		}

		changesArr.addAll(writeAllSeasonPariticipatingInGivenSeasonsExperiments(season, isProductionChange, contxet));
		return changesArr;
	}

	public static LinkedList<AirlockChangeContent> writeAllSeasonPariticipatingInGivenSeasonsExperiments(Season season, boolean isProductionChange, ServletContext context) throws JSONException, IOException, MergeException {

		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();

		TreeSet<UUID> writtenSeasonsIDs = new TreeSet<UUID>();
		writtenSeasonsIDs.add(season.getUniqueId());
		List<Experiment> experimentsForSeason = season.getExperimentsForSeason(context, false);
		Map<String, BaseAirlockItem> airlockItemsDB = Utilities.getAirlockItemsDB(Constants.MASTER_BRANCH_NAME, context);
		
		for (Experiment exp:experimentsForSeason) {
			Product prod = productsDB.get(exp.getProductId().toString());
			List<Season> expSeasons = prod.getSeasonsWithinRange(exp.getMinVersion(), exp.getMaxVersion());
			for (Season s:expSeasons) {
				if (writtenSeasonsIDs.contains(s.getUniqueId()))
					continue;
				
				Environment env = new Environment();
				env.setAirlockItemsDB(airlockItemsDB);
				env.setServerVersion(season.getServerVersion());
				env.setBranchId(Constants.MASTER_BRANCH_NAME);
				
				//write the new variant in the runtime files
				changesArr.addAll(AirlockFilesWriter.doWriteFeatures(s, OutputJSONMode.RUNTIME_DEVELOPMENT, context, Stage.DEVELOPMENT, env));
				if (isProductionChange)
					changesArr.addAll(AirlockFilesWriter.doWriteFeatures(s, OutputJSONMode.RUNTIME_PRODUCTION, context, Stage.PRODUCTION, env));

				writtenSeasonsIDs.add(s.getUniqueId());
			}			

		}
		return changesArr;
	}

	@GET
	@Path ("/products/experiments/{experiment-id}/indexinginfo")	
	@ApiOperation(value = "Returns the specified experiment", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Feature not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getExperimentIndexingInfo(@PathParam("experiment-id")String experiment_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getExperiment request");
		}

		String err = Utilities.validateLegalUUID(experiment_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalExperimentUUID + err);

		if (!isAnalyticsServerConfigured()) {
			return sendAndLogError(Status.BAD_REQUEST, Strings.analyticsServerNotConfigured);
		}

		//find relevant product
		ProductErrorPair prodErrPair = Utilities.getProductOfExperiment(context, experiment_id);
		if (prodErrPair.error != null) {			
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		Product currentProduct = prodErrPair.product;

		//check user authorization
		UserInfo userInfo = UserInfo.validate("AnalyticsServices.getExperimentIndexingInfo", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.EXPERIMENTS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();	
		readWriteLock.readLock().lock();
		try {			

			@SuppressWarnings("unchecked")
			Map<String, Experiment> experimentsDB = (Map<String, Experiment>)context.getAttribute(Constants.EXPERIMENTS_DB_PARAM_NAME);

			Experiment exp = experimentsDB.get(experiment_id);

			if (exp == null) {
				String errMsg = Strings.experimentNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			//no need to validate experiments support since the experiment exists so it is supported in its season			

			ValidationResults res = null;
			try { 
				res = AnalyticsUtilities.getExperimentFromAnalyticsServer(context, experiment_id);
				if (res.status!=Status.OK) {
					logger.severe(res.error);
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(res.error)).build();						
				}
			} catch (Exception e) {
				err = Strings.failGetExperimentFromAnalyticsServer + e.getMessage();
				logger.severe(err);
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(err)).build();

			} 			

			return Response.ok(res.error).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error getting experiment " + experiment_id + " indexing info: ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error getting experiment indexing information: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	Response sendAndLogError(Status status, String err) {
		logger.severe(err);
		return Response.status(status).entity(Utilities.errorMsgToErrorJSON(err)).build();
	}

	Response sendInfoError(Status status, UserInfo info){
		return Response.status(status).entity(info.getErrorJson()).build();
	}

	private AirlockCapability[] getRequiredCapabilities(String branch_id) {
		if (branch_id.equals(Constants.MASTER_BRANCH_NAME)) {
			return new AirlockCapability[]{AirlockCapability.ANALYTICS};
		}
		else {
			return new AirlockCapability[]{AirlockCapability.ANALYTICS, AirlockCapability.BRANCHES};
		}
	}

	private boolean isAnalyticsServerConfigured() {
		String analyticsServerUrl = (String)context.getAttribute(Constants.ANALYTICS_SERVER_URL_PARAM_NAME);
		return (analyticsServerUrl!=null && !analyticsServerUrl.isEmpty());
	}
}
