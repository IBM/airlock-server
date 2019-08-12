package com.ibm.airlock;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.ibm.airlock.admin.translations.OriginalString;
import com.ibm.airlock.admin.translations.OriginalStrings;
import com.ibm.airlock.utilities.FeatureFilter;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponses;

import com.ibm.airlock.utilities.Pair;

import com.wordnik.swagger.annotations.ApiResponse;
import com.ibm.airlock.admin.BaseAirlockItem.Type;
import com.ibm.airlock.admin.MergeBranch.MergeException;
import com.ibm.airlock.admin.Utilities.ProductErrorPair;
import com.ibm.airlock.admin.authentication.UserInfo;
import com.ibm.airlock.admin.operations.AirlockChange;
import com.ibm.airlock.admin.operations.AirlockChangeContent;
import com.ibm.airlock.admin.operations.Webhooks;
import com.ibm.airlock.admin.serialize.AirlockFilesWriter;
import com.ibm.airlock.admin.serialize.AuditLogWriter;
import com.ibm.airlock.engine.Environment;
import com.ibm.airlock.Constants.ActionType;
import com.ibm.airlock.Constants.AirlockCapability;
import com.ibm.airlock.Constants.BranchStatus;
import com.ibm.airlock.Constants.OutputJSONMode;
import com.ibm.airlock.Constants.REQUEST_ITEM_TYPE;
import com.ibm.airlock.Constants.Stage;
import com.ibm.airlock.admin.*;

import org.apache.wink.json4j.JSONArray;


@Path ("/admin/features")
@Api(value = "/features", description = "Feature discovery API")
public class FeatureServices {
	public static final Logger logger = Logger.getLogger(FeatureServices.class.getName());

	@Context
	private ServletContext context;

	@GET
	@ApiOperation(value = "Returns all features grouped by product and season from master", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getFeatures(@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		//long startTime = System.currentTimeMillis();

		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getFeatures request");
		}

		// use userInfo for more stringent checks. null if authorization is off
		UserInfo userInfo = UserInfo.validate("FeatureServices.getFeatures", context, assertion, null);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
		readWriteLock.readLock().lock();
		try {
			@SuppressWarnings("unchecked")
			Map<String, BaseAirlockItem> airlockItemsDB = (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);
			
			@SuppressWarnings("unchecked")
			Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);
			Set<String> products = productsDB.keySet();

			JSONObject res = new JSONObject();
			JSONArray productsArr = new JSONArray();

			for (String prodID:products) {
				JSONObject prodJsonObj = new JSONObject();
				prodJsonObj.put(Constants.JSON_FIELD_UNIQUE_ID, prodID);

				Product prod = productsDB.get(prodID);

				//if user does not have permission to call getProduct for a specific product it means that he is not viewer in this product
				//return only products in which the user is viewer
				UserInfo prodUserInfo = UserInfo.validate("ProductServices.getProduct", context, assertion, prod);
				if (prodUserInfo != null && prodUserInfo.getErrorJson() != null)
					continue;
				
				prodJsonObj.put(Constants.JSON_FIELD_NAME, prod.getName());

				JSONArray seasonsArr = new JSONArray();

				LinkedList<Season> seasons = prod.getSeasons();
				for (int i=0; i<seasons.size(); i++) {
					JSONObject seasonJsonObj = new JSONObject();
					Season season = seasons.get(i);
					seasonJsonObj.put(Constants.JSON_FIELD_UNIQUE_ID, season.getUniqueId().toString());
					seasonJsonObj.put(Constants.JSON_SEASON_FIELD_MAX_VER, season.getMaxVersion());
					seasonJsonObj.put(Constants.JSON_SEASON_FIELD_MIN_VER, season.getMinVersion());
					//seasonJsonObj.put(Constants.JSON_FIELD_NAME, season.getName());

					JSONObject rootJSON = null;
					try {
						RootItem root = season.getRoot();

						if (root != null) {
							Environment env = new Environment();
							env.setServerVersion(season.getServerVersion());
							env.setAirlockItemsDB(airlockItemsDB);
							rootJSON  = root.toJson(OutputJSONMode.DISPLAY, context, env,userInfo);
						}
					}
					catch (Exception e)
					{}

					if (rootJSON == null)
						logger.severe(Strings.skyppingEmptySeason + season.getUniqueId().toString());
					else
					{
						seasonJsonObj.put(Constants.JSON_FIELD_ROOT, rootJSON);
						seasonsArr.add(seasonJsonObj);
					}
				}

				prodJsonObj.put(Constants.JSON_PRODUCT_FIELD_SEASONS, seasonsArr);
				productsArr.add(prodJsonObj);
			}

			res.put(Constants.JSON_FIELD_PRODUCTS, productsArr);
			//long stopTime = System.currentTimeMillis();
			//long elapsedTime = stopTime - startTime;

			//System.out.println("******* FeatureServices.getFeatures time = " + elapsedTime);

			return Response.ok(res.toString()).build();
		} finally {
			readWriteLock.readLock().unlock();
			//long stopTime = System.currentTimeMillis();
			// long elapsedTime = stopTime - startTime;
			// System.out.println("******* in finally, FeatureServices.getFeatures time = " + elapsedTime);
		}
	}


	@PUT
	@Path ("/copy/branches/{source-branch-id}/{feature-id}/branches/{destination-branch-id}/{new-parent-id}")
	@ApiOperation(value = "Copy the specified feature to the specified parent.", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Feature or parent not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response copy(@PathParam("source-branch-id")String sourceBranchId,			 
						 @PathParam("feature-id")String featureId,
						 @PathParam("destination-branch-id")String destinationBranchId,						 
						 @PathParam("new-parent-id")String newParentId,
						 @ApiParam(value="VALIDATE or ACT")@QueryParam("mode")String mode,
						 @QueryParam("namesuffix")String nameSuffix,
						 @QueryParam("minappversion")String minAppVersion,
						 @HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		
		long startTime = System.currentTimeMillis();

		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("copy request: feature_id =" + featureId +", new-parent-id = " + newParentId+", mode = " + mode+", nameSuffix = " + nameSuffix+", minAppVersion = " + minAppVersion);
		}
		AirlockChange change = new AirlockChange();
		String err = Utilities.validateLegalUUID(featureId);
		if (err!=null) 
			return sendError(Status.BAD_REQUEST, Strings.illegalFeatureUUID + err);

		err = Utilities.validateLegalUUID(newParentId);
		if (err!=null) 
			return sendError(Status.BAD_REQUEST, Strings.illegalNewParentUUID + err);

		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductForBranchOrFeature(context, destinationBranchId, newParentId);
		if (productErrorPair.error != null) {
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);
		//check user authorization
		UserInfo userInfo = UserInfo.validate("FeatureServices.copy", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

				
		if (mode == null || mode.isEmpty()) {
			return sendError(Status.BAD_REQUEST, Strings.modeMissing);
		}
		ActionType actionTypeObj = Utilities.strToActionType(mode, ActionType.ACT);

		if (actionTypeObj==null) {
			return sendError(Status.BAD_REQUEST, String.format(Strings.illegalMode,mode)  + Constants.ActionType.returnValues());
		}

		//nameSuffix, overrideIds and minAppVersion are optional parameters
		if (nameSuffix == null)
			nameSuffix="";

		@SuppressWarnings("unchecked")
		Map<String,InternalUserGroups> groupsPerProductMap = (Map<String,InternalUserGroups>) context.getAttribute(Constants.USER_GROUPS_PER_PRODUCT_PARAM_NAME);
		InternalUserGroups userGroups = groupsPerProductMap.get(currentProduct.getUniqueId().toString());

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.FEATURES});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();	
						
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.writeLock().lock();
		try {
			@SuppressWarnings("unchecked")
			Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);
			
			
			//verify sourceBranchId
			ValidationResults validationRes = Utilities.validateBranchId(context, sourceBranchId, null);
			if (validationRes!=null) {
				return sendError(validationRes.status, validationRes.error);
			}
			
			//verify destinationBranchId
			validationRes = Utilities.validateBranchId(context, destinationBranchId, null);
			if (validationRes!=null) {
				return sendError(validationRes.status, validationRes.error);
			}
			
			Map<String, BaseAirlockItem> sourceAirlockItemsDB = Utilities.getAirlockItemsDB(sourceBranchId, context);			
			Map<String, BaseAirlockItem> destinationAirlockItemsDB = Utilities.getAirlockItemsDB(destinationBranchId, context);
			
			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			BaseAirlockItem featureToCopy = sourceAirlockItemsDB.get(featureId);
			if (featureToCopy == null) {
				return sendError(Status.NOT_FOUND, Strings.AirlockItemNotFound);
			}

			if (featureToCopy.getType() == Type.ROOT) {
				return sendError(Status.BAD_REQUEST, Strings.copyFeatureRoot);
			}
			
			if (featureToCopy.getType() == Type.ORDERING_RULE || featureToCopy.getType() == Type.ORDERING_RULE_MUTUAL_EXCLUSION_GROUP) {
				return sendError(Status.BAD_REQUEST, Strings.copyOrderingRule);
			}

			BaseAirlockItem newParentObj = destinationAirlockItemsDB.get(newParentId);
			if (newParentObj == null) {
				return sendError(Status.NOT_FOUND, Strings.parentNotFound);
			}
			
			if (!destinationBranchId.equals(Constants.MASTER_BRANCH_NAME) && newParentObj.getBranchStatus().equals(BranchStatus.NONE) &&
					!newParentObj.getType().equals(BaseAirlockItem.Type.ROOT)) {
				//in the console you can only copy features while in the API you can copy CR and MTX as well.
				//the error message is for the console therefore referring only to features
				return sendError(Status.BAD_REQUEST, Strings.pastedNotCheckout);
			}
			
			Season parentsSeason = seasonsDB.get(newParentObj.getSeasonId().toString());
			change.setSeason(parentsSeason);
			
			Environment env = new Environment();
			env.setServerVersion(parentsSeason.getServerVersion());
			env.setAnalytics(parentsSeason.getAnalytics());
			env.setBranchId(destinationBranchId);
			env.setAirlockItemsDB(destinationAirlockItemsDB);

			//return direct violation first
			if(actionTypeObj == ActionType.VALIDATE){
				Season sourceSeason = seasonsDB.get(featureToCopy.getSeasonId().toString());
				try{
					JSONObject res = Utilities.getDirectAssetsViolations(sourceSeason,parentsSeason,featureToCopy,context);
					if (res != null) {
						return Response.status(Status.BAD_REQUEST).entity(res.toString()).build();
					}
				}catch (Exception e){
					err = Strings.missingAssetException  + e.getMessage();
					logger.severe(err);
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(err)).build();
				}
			}
			//TODO: change that when we check strings utilities and context separatly!!!!
			List<OriginalString> copiedStrings = new ArrayList<>();
			JSONArray stringsInConflict = new JSONArray();
			JSONArray addedStrings = new JSONArray();
			copiedStrings = getStringInUseByConfig(context, featureToCopy,true);
			if(copiedStrings.size() != 0) {// don't check if there are no strings
				List<OriginalString> newStrings = new ArrayList<>();
				List<TranslationServices.ConflictingStrings> conflictingStrings = new ArrayList<>();
				List<OriginalString> nonConflictingStrings = new ArrayList<>();
				TranslationServices.checkConflicts(copiedStrings,parentsSeason,conflictingStrings,newStrings,nonConflictingStrings,false, false, null);
				if(conflictingStrings.size() != 0) {
					for(int i = 0; i<conflictingStrings.size();++i){
						TranslationServices.ConflictingStrings conflictingString = conflictingStrings.get(i);
						JSONObject conflict = new JSONObject();
						conflict.put("key",conflictingString.getKey());
						conflict.put("sourceValue", conflictingString.getSource().getValue());
						conflict.put("destValue",conflictingString.getDest().getValue());
						stringsInConflict.add(conflict);
					}
				}
				if(newStrings.size() != 0){
					for(int i = 0; i<newStrings.size();++i){
						addedStrings.add(newStrings.get(i).getKey());
					}
				}
			}
			try {
				JSONObject res = Utilities.getNameAndMinAppVerAndAssetsViolation(sourceAirlockItemsDB, destinationAirlockItemsDB, featureId, newParentId, nameSuffix, minAppVersion, seasonsDB, parentsSeason,copiedStrings, context, destinationBranchId);
				if (res != null) {
					return Response.status(Status.BAD_REQUEST).entity(res.toString()).build();
				}
			} catch (JSONException je) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(je.getMessage())).build();
			}


			boolean destinationIsMaster = destinationBranchId.equals(Constants.MASTER_BRANCH_NAME);
			Map<String, BaseAirlockItem> airlockItemsMapCopy = null;
			try {
				if (destinationIsMaster) {
					airlockItemsMapCopy = Utilities.getAirlockItemsMapCopy (context, parentsSeason.getUniqueId().toString());
				}
				else {
					//in branch the destinationItemsdb is a copy in the first place -it is a copy build during features tree merge. 
					airlockItemsMapCopy = destinationAirlockItemsDB;
				}
	 		} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}

			Branch destinationBranchCopy = null;			
			Branch destinationBranch = null;			
			if (!destinationIsMaster) {				
				destinationBranch = branchesDB.get(destinationBranchId);
				
				destinationBranchCopy = new Branch(destinationBranch.getSeasonId());
				destinationBranchCopy.clone(destinationBranch, context);				
			}
			change.setBranch(destinationBranch);
			
			//add copied features one by one to the airlockItemsMapCopy
			HashMap<String, String> oldToNewIDsMap = new HashMap<String, String>();
			
			REQUEST_ITEM_TYPE itemType = BaseAirlockItem.isOnlyPurchaseItem(featureToCopy.getType()) ? REQUEST_ITEM_TYPE.ENTITLEMENTS:REQUEST_ITEM_TYPE.FEATURES;
			ValidationResults copyValidationResults = Utilities.copy(airlockItemsMapCopy, featureToCopy, newParentId, nameSuffix, minAppVersion, 
					userGroups, userInfo, context, oldToNewIDsMap, env,copiedStrings, destinationBranchCopy, sourceAirlockItemsDB, 
					parentsSeason, itemType);
			
			if (copyValidationResults!=null) {
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(copyValidationResults.error)).build();
			}

			RootItem rootFeature = null;
			RootItem rootPurchases = null;
			Branch destBranch = null;
			if (actionTypeObj == ActionType.ACT) {
				copyStrings(copiedStrings, parentsSeason, actionTypeObj, userInfo, featureId, oldToNewIDsMap, airlockItemsMapCopy, env);
							
				if (destinationIsMaster) {
					//add/override copied features one by one to the airlockItemsDB				
					Set<String> feauruesIds = airlockItemsMapCopy.keySet();
					for (String fId:feauruesIds) {					
						destinationAirlockItemsDB.put(fId, airlockItemsMapCopy.get(fId));					
					}

					context.setAttribute(Constants.FEATURES_DB_PARAM_NAME, destinationAirlockItemsDB);
					updateSeasonsRootToNewAirlockObjects(destinationAirlockItemsDB, context);
				}
				else {
					//replace the old destination branch with the copy of the destination branch on which we perform the copy action
					branchesDB.put(destinationBranchCopy.getUniqueId().toString(), destinationBranchCopy);
					parentsSeason.getBranches().replaceBranch(destinationBranchCopy);
				}
	
				if (featureToCopy instanceof DataAirlockItem) {
					sendMailForCopy(destinationAirlockItemsDB, newParentObj, (DataAirlockItem) featureToCopy, featureId, newParentId, userInfo, env);
				}

				try { 
					boolean writeProductionFeatures = false; //always changing copied features stage to development
					if (destinationIsMaster) {
						if (itemType.equals(REQUEST_ITEM_TYPE.ENTITLEMENTS)) {
							Pair<String, LinkedList<AirlockChangeContent>> pWriteRes = 
									AirlockFilesWriter.writePurchases (parentsSeason, writeProductionFeatures, context, false, env);
							change.getFiles().addAll(pWriteRes.getValue());	
						}
						else {
							Pair<String, LinkedList<AirlockChangeContent>> writeRes = AirlockFilesWriter.writeFeatures (parentsSeason, writeProductionFeatures, context, env); 
							err = writeRes.getKey();
							change.getFiles().addAll(writeRes.getValue());	
						}
					} 
					else {
						ValidationResults res = Utilities.validateBranchStructure(destinationBranchId, parentsSeason, context);
						if (res!=null) {
							String errMsg = res.error;
							logger.severe(errMsg);
							return Response.status(res.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
						}	
						
						destBranch = branchesDB.get(destinationBranchId);
						if (itemType.equals(REQUEST_ITEM_TYPE.ENTITLEMENTS)) {
							change.getFiles().addAll(AirlockFilesWriter.writeBranchPurchases(destBranch, parentsSeason, context, env, Stage.DEVELOPMENT));											
						}
						else {
							change.getFiles().addAll(AirlockFilesWriter.writeBranchFeatures(destBranch, parentsSeason, context, env, Stage.DEVELOPMENT));												
						}
						change.getFiles().addAll(AirlockFilesWriter.writeBranchRuntime(destBranch, parentsSeason, context, env, writeProductionFeatures));
						change.getFiles().addAll(AirlockFilesWriter.doWriteConstantsFiles(parentsSeason, context, Stage.DEVELOPMENT));
						
					}
				} catch (IOException e) {
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
				}

				if (destinationIsMaster) {
					rootFeature = (RootItem)airlockItemsMapCopy.get(parentsSeason.getRoot().getUniqueId().toString());
					rootPurchases = (RootItem)airlockItemsMapCopy.get(parentsSeason.getEntitlementsRoot().getUniqueId().toString());
				}
				else {
					Map<String, BaseAirlockItem> newDestinationAirlockItemsDB = Utilities.getAirlockItemsDBForBranchCopy(destinationBranchCopy, context);
				
					rootFeature = (RootItem)newDestinationAirlockItemsDB.get(parentsSeason.getRoot().getUniqueId().toString());
					rootPurchases = (RootItem)newDestinationAirlockItemsDB.get(parentsSeason.getEntitlementsRoot().getUniqueId().toString());
				}
			}
			else { //VALIDATE
				if (destinationIsMaster) {
					rootFeature = Utilities.findItemsRoot(airlockItemsMapCopy.get(newParentId), airlockItemsMapCopy);
					rootPurchases = Utilities.findItemsRoot(airlockItemsMapCopy.get(newParentId), airlockItemsMapCopy);
				}
				else {
					destBranch = branchesDB.get(destinationBranchId);
					Map<String, BaseAirlockItem> newDestinationAirlockItemsDB = Utilities.getAirlockItemsDBForBranchCopy(destinationBranchCopy, context);				
					rootFeature = Utilities.findItemsRoot(newDestinationAirlockItemsDB.get(newParentId), newDestinationAirlockItemsDB);
					rootPurchases = Utilities.findItemsRoot(newDestinationAirlockItemsDB.get(newParentId), newDestinationAirlockItemsDB);
				}
			}

			JSONObject res = new JSONObject();

			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			String srcBranchName = Constants.MASTER_BRANCH_NAME;
			if (!sourceBranchId.equals(Constants.MASTER_BRANCH_NAME)) {
				Branch srcBranch = branchesDB.get(sourceBranchId);
				srcBranchName = srcBranch.getName() + ", " + sourceBranchId;
			}
			
			String destBranchName = Constants.MASTER_BRANCH_NAME;
			if (!destinationBranchId.equals(Constants.MASTER_BRANCH_NAME)) {
				destBranchName = destBranch.getName() + ", " + destinationBranchId;
			}
			
			auditLogWriter.log("Copy item: " + featureToCopy.toString() +": " + featureToCopy.getNameSpaceDotName() + ", " + featureToCopy.getUniqueId().toString() + " from branch: " + srcBranchName + ": new parent = " + newParentObj.getNameSpaceDotName() + ", " + newParentId + ", branch " + destBranchName, userInfo);

			res.put(Constants.JSON_FIELD_UPDATED_SEASONS_FEATURES, rootFeature.toJson(OutputJSONMode.DISPLAY, context, env));
			res.put(Constants.JSON_FIELD_UPDATED_SEASONS_ENTITLEMENTS, rootPurchases.toJson(OutputJSONMode.DISPLAY, context, env));
			res.put(Constants.JSON_FIELD_NEW_SUBTREE_ID, oldToNewIDsMap.get(featureId));
			res.put(Constants.JSON_FIELD_STRING_CONFLICTS,stringsInConflict);
			res.put(Constants.JSON_FIELD_STRING_ADDED,addedStrings);
			
			long stopTime = System.currentTimeMillis();
		    long elapsedTime = stopTime - startTime;
		    		    
		    logger.info("******* FeatureServices.copy time = " + elapsedTime);
		    Webhooks.get(context).notifyChanges(change, context);
			return (Response.ok(res.toString())).build();
		} catch (MergeException e) {
			err = Strings.mergeException  + e.getMessage();
			logger.severe(err);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(err)).build();		
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	private void copyStrings(List<OriginalString>copiedStrings, Season parentsSeason, ActionType actionTypeObj, UserInfo userInfo,String featureId, HashMap<String, String> oldToNewIDsMap ,Map<String, BaseAirlockItem> airlockItemsMapCopy,Environment env)throws JSONException{
		if(copiedStrings.size() != 0) {//do nothing if there are no strings
			/*Pair<Response, LinkedList<AirlockChangeContent>> copyResult = */TranslationServices.doCopyStrings(context,copiedStrings, parentsSeason, parentsSeason.getUniqueId().toString(), actionTypeObj, userInfo,false,false,false, false, null);
		}
	}

	public static List<OriginalString> getStringInUseByConfig(ServletContext context, BaseAirlockItem feature,Boolean recursive)throws JSONException{
		@SuppressWarnings("unchecked")
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);
		Season copiedSeason = seasonsDB.get(feature.getSeasonId().toString());
		List<OriginalString> copiedStrings = new ArrayList<>();
		Set<String> stringsInUseByConfigList = new HashSet<String>();
		Set<String> stringsInUseByUtilList = new HashSet<String>();
		feature.doGetStringsInUseByItem(copiedSeason.getOriginalStrings().getAllStringKeys(), feature, stringsInUseByConfigList, stringsInUseByUtilList, copiedSeason, recursive, true);
		
		OriginalStrings seasonStrings = copiedSeason.getOriginalStrings();
		Iterator<String> iter = stringsInUseByConfigList.iterator();
		while (iter.hasNext()){
			String currId = iter.next();
			OriginalString copiedString = seasonStrings.getOriginalStringByKey(currId);
			copiedStrings.add(copiedString);
		}
		iter = stringsInUseByUtilList.iterator();
		while (iter.hasNext()){
			String currId = iter.next();
			OriginalString copiedString = seasonStrings.getOriginalStringByKey(currId);
			copiedStrings.add(copiedString);
		}
		return copiedStrings;
	}

	@PUT
	//@Path ("/import/{new-parent-id}")
	@Path ("/import/branches/{destination-branch-id}/{new-parent-id}")
	@ApiOperation(value = "Import the given features to the specified parent.", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Feature or parent not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response importFeatures(String importedFeatures, 
						 @PathParam("destination-branch-id")String destinationBranchId,
						 @PathParam("new-parent-id")String newParentId,
						 @ApiParam(value="VALIDATE or ACT")@QueryParam("mode")String mode,
						 @QueryParam("namesuffix")String nameSuffix,
						 @QueryParam("minappversion")String minAppVersion,
						 @QueryParam("overrideids")Boolean overrideIds,  //by default true
						 @HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {


		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("importFeatures request: importedFeatures =" + importedFeatures + "\n\n, new-parent-id = " + newParentId + ", mode = " + mode + ", nameSuffix = " + nameSuffix + ", minAppVersion = " + minAppVersion);
		}

		String err = Utilities.validateLegalUUID(newParentId);
		if (err!=null) 
			return sendError(Status.BAD_REQUEST, Strings.illegalNewParentUUID + err);
		
		AirlockChange change = new AirlockChange();
		
		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductForBranchOrFeature(context, destinationBranchId, newParentId);
		if (productErrorPair.error != null) {
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);
		//check user authorization		
		UserInfo userInfo = UserInfo.validate("FeatureServices.importFeatures", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);
		
		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, 
											new AirlockCapability[]{AirlockCapability.FEATURES, AirlockCapability.EXPORT_IMPORT});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		
		
		if (mode == null || mode.isEmpty()) {
			String errMsg = Strings.modeMissing;
			logger.severe(errMsg);
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
		}
		ActionType actionTypeObj = Utilities.strToActionType(mode, ActionType.ACT);

		if (actionTypeObj == null) {
			return sendError(Status.BAD_REQUEST, String.format(Strings.illegalMode,mode) + Constants.ActionType.returnValues());
		}

		//nameSuffix, overrideIds and minAppVersion are optional parameters
		if (nameSuffix == null)
			nameSuffix = "";

		if (overrideIds == null)
			overrideIds = true;

		@SuppressWarnings("unchecked")
		Map<String,InternalUserGroups> groupsPerProductMap = (Map<String,InternalUserGroups>) context.getAttribute(Constants.USER_GROUPS_PER_PRODUCT_PARAM_NAME);
		InternalUserGroups userGroups = groupsPerProductMap.get(currentProduct.getUniqueId().toString());
				
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.writeLock().lock();
		try {
			@SuppressWarnings("unchecked")
			Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);
			
			//verify destinationBranchId
			ValidationResults validationRes = Utilities.validateBranchId(context, destinationBranchId, null);
			if (validationRes!=null) {
				return sendError(validationRes.status, validationRes.error);
			}
						
			Map<String, BaseAirlockItem> destinationAirlockItemsDB = Utilities.getAirlockItemsDB(destinationBranchId, context);
		
			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>) context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			BaseAirlockItem newParentObj = destinationAirlockItemsDB.get(newParentId);
			if (newParentObj == null) {
				return sendError(Status.NOT_FOUND, Strings.parentNotFound);
			}

			if (!destinationBranchId.equals(Constants.MASTER_BRANCH_NAME) && newParentObj.getBranchStatus().equals(BranchStatus.NONE) &&
					!newParentObj.getType().equals(BaseAirlockItem.Type.ROOT)) {
				return sendError(Status.NOT_FOUND, Strings.importNotCheckout);
			}
			
			JSONObject importedFeaturesJSON = null;
			try {
				importedFeaturesJSON = new JSONObject(importedFeatures);
			} catch (JSONException je) {
				return sendError(Status.BAD_REQUEST, Strings.illegalInputJSON + je.getMessage());
			}

			String typeStr = null;
			try {
				typeStr = importedFeaturesJSON.getString(Constants.JSON_FEATURE_FIELD_TYPE);
			} catch (JSONException je) {
				return sendError(Status.NOT_FOUND, String.format(Strings.keyMissing,Constants.JSON_FEATURE_FIELD_TYPE));
			}
			Type typeObj = BaseAirlockItem.strToType(typeStr);
			if (typeObj == null)
				throw new JSONException(Strings.illegalType + typeStr);

			if (typeObj == Type.ROOT) {
				return sendError(Status.NOT_FOUND, Strings.importFeatureRoot);
			}
			Season parentsSeason = seasonsDB.get(newParentObj.getSeasonId().toString());
			change.setSeason(parentsSeason);

			//return direct violation first
			if(actionTypeObj == ActionType.VALIDATE){
				try{
					JSONObject res = Utilities.getDirectAssetsViolationsJSON(parentsSeason,importedFeaturesJSON,context);
					if (res != null) {
						return Response.status(Status.BAD_REQUEST).entity(res.toString()).build();
					}
				}catch (Exception e){
					return sendError(Status.INTERNAL_SERVER_ERROR, Strings.missingAssetException  + e.getMessage());
				}
			}
			Boolean copyStrings = importedFeaturesJSON.containsKey("strings");
			String featureId = importedFeaturesJSON.getString(Constants.JSON_FIELD_UNIQUE_ID);
			List<OriginalString> copiedStrings = new ArrayList<>();
			JSONArray stringsInConflict = new JSONArray();
			JSONArray addedStrings = new JSONArray();
			if (copyStrings){
				JSONArray stringsArray = importedFeaturesJSON.getJSONArray("strings");
				try {
					for (int i = 0; i < stringsArray.size(); ++i) {
						OriginalString oriString = new OriginalString();
						JSONObject newStringJson = stringsArray.getJSONObject(i);
						validationRes = oriString.validateOriginalStringJSONForImport(newStringJson, context, userInfo);
						if (validationRes!=null) {
							String errMsg = validationRes.error;
							logger.severe(errMsg);
							return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
						}
						oriString.fromJSON(newStringJson,parentsSeason);
						copiedStrings.add(oriString);
					}
				}
				catch (JSONException e) {
					return sendError(Status.NOT_FOUND, Strings.illegalStringJSON);
				}
				if(copiedStrings.size() != 0) {// don't check if there are no strings
					List<OriginalString> newStrings = new ArrayList<>();
					List<TranslationServices.ConflictingStrings> conflictingStrings = new ArrayList<>();
					List<OriginalString> nonConflictingStrings = new ArrayList<>();
					TranslationServices.checkConflicts(copiedStrings,parentsSeason,conflictingStrings,newStrings,nonConflictingStrings,false, false, null);
					if(conflictingStrings.size() != 0) {
						for(int i = 0; i<conflictingStrings.size();++i){
							TranslationServices.ConflictingStrings conflictingString = conflictingStrings.get(i);
							JSONObject conflict = new JSONObject();
							conflict.put("key",conflictingString.getKey());
							conflict.put("sourceValue", conflictingString.getSource().getValue());
							conflict.put("destValue",conflictingString.getDest().getValue());
							stringsInConflict.add(conflict);
						}
					}
					if(newStrings.size() != 0){
						for(int i = 0; i<newStrings.size();++i){
							addedStrings.add(newStrings.get(i).getKey());
						}
					}
				}
			}
			try {
				JSONObject res = Utilities.getNameAndMinAppVersionViolationFromJSON(destinationAirlockItemsDB, importedFeaturesJSON, newParentId, nameSuffix, minAppVersion, seasonsDB, overrideIds,copiedStrings, destinationBranchId, context);
				if (res != null) {
					return Response.status(Status.BAD_REQUEST).entity(res.toString()).build();
				}
			} catch (JSONException je) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON("Input is not a llegal JSON: " + je.getMessage())).build();
			}

			boolean destinationIsMaster = destinationBranchId.equals(Constants.MASTER_BRANCH_NAME);
			Map<String, BaseAirlockItem> airlockItemsMapCopy = null;
			try {
				if (destinationIsMaster) {
					airlockItemsMapCopy = Utilities.getAirlockItemsMapCopy (context, parentsSeason.getUniqueId().toString());
				}
				else {
					//in branch the destinationItemsdb is a copy in the first place -it is a copy build during features tree merge. 
					airlockItemsMapCopy = destinationAirlockItemsDB;
				}
	 		} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}
			
			
			String importedRootId = importedFeaturesJSON.getString(Constants.JSON_FIELD_UNIQUE_ID);

			//add copied features one by one to the airlockItemsMapCopy
			HashMap<String, String> oldToNewIDsMap = new HashMap<String, String>();
			Environment env = new Environment();
			env.setServerVersion(parentsSeason.getServerVersion());
			env.setAnalytics(parentsSeason.getAnalytics());
			env.setBranchId(destinationBranchId);
			env.setAirlockItemsDB(destinationAirlockItemsDB);

			Branch destinationBranchCopy = null;			
			Branch destinationBranch = null;			
			if (!destinationIsMaster) {				
				destinationBranch = branchesDB.get(destinationBranchId);
				
				destinationBranchCopy = new Branch(destinationBranch.getSeasonId());
				destinationBranchCopy.clone(destinationBranch, context);				
			}
			change.setBranch(destinationBranch);


			REQUEST_ITEM_TYPE itemType = BaseAirlockItem.isOnlyPurchaseItem(typeObj) ? REQUEST_ITEM_TYPE.ENTITLEMENTS:REQUEST_ITEM_TYPE.FEATURES;

			ValidationResults copyValidationResults = Utilities.copyFromJSON(airlockItemsMapCopy, importedFeaturesJSON, newParentId, 
					nameSuffix, minAppVersion, userGroups, userInfo, context, overrideIds, oldToNewIDsMap, env,copiedStrings, 
					destinationBranchCopy, destinationAirlockItemsDB, itemType);
			
			if (copyValidationResults != null) {
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(copyValidationResults.error)).build();
			}

			RootItem rootFeature = null;
			RootItem rootPurchases = null;
			
			if (actionTypeObj == ActionType.ACT) {
				if(copyStrings) {
					copyStrings(copiedStrings, parentsSeason, actionTypeObj, userInfo, featureId, oldToNewIDsMap, airlockItemsMapCopy, env);
				}
				
				if (destinationIsMaster) {
					//add/override copied features one by one to the airlockItemsDB				
					Set<String> feauruesIds = airlockItemsMapCopy.keySet();
					for (String fId:feauruesIds) {					
						destinationAirlockItemsDB.put(fId, airlockItemsMapCopy.get(fId));					
					}

					context.setAttribute(Constants.FEATURES_DB_PARAM_NAME, destinationAirlockItemsDB);
					updateSeasonsRootToNewAirlockObjects(destinationAirlockItemsDB, context);
				}
				else {
					//replace the old destination branch with the copy of the destination branch on which we perform the copy action
					branchesDB.put(destinationBranchCopy.getUniqueId().toString(), destinationBranchCopy);
					parentsSeason.getBranches().replaceBranch(destinationBranchCopy);
				}

				if (newParentObj instanceof FeatureItem) {
					@SuppressWarnings("unchecked")					
					Map<String, ArrayList<String>> followersFeaturesDB = (Map<String, ArrayList<String>>) context.getAttribute(Constants.FOLLOWERS_FEATURES_DB_PARAM_NAME);
					Boolean isProduction = ((DataAirlockItem) newParentObj).getStage().toString().equals("PRODUCTION");
					String details = "The json " + importedFeaturesJSON + " was imported to the feature " + newParentObj.getNameSpaceDotName() + " with ID " + newParentId+"\n";
					Utilities.sendEmailForDataItem(context, (FeatureItem) newParentObj, followersFeaturesDB.get(newParentId), details, null, null, isProduction, userInfo,env);
				}
				try {
					boolean writeProductionFeatures = false; //always changing copied features stage to development
					// BaseAirlockItem.isProductionFeature(featureToCopy);
					if (destinationIsMaster) {
						
						if (itemType.equals(REQUEST_ITEM_TYPE.ENTITLEMENTS)) {
							Pair<String, LinkedList<AirlockChangeContent>> pWriteRes = 
									AirlockFilesWriter.writePurchases (parentsSeason, writeProductionFeatures, context, false, env);
							change.getFiles().addAll(pWriteRes.getValue());	
						}
						else {
							Pair<String, LinkedList<AirlockChangeContent>> writeRes = 
									AirlockFilesWriter.writeFeatures (parentsSeason, writeProductionFeatures, context, env);
							change.getFiles().addAll(writeRes.getValue());	
						}
					} 
					else {
						ValidationResults res = Utilities.validateBranchStructure(destinationBranchId, parentsSeason, context);
						if (res!=null) {
							return sendError(res.status, res.error);
						}	
						
						Branch branch = branchesDB.get(destinationBranchId);
						change.setBranch(branch);
						change.getFiles().addAll(AirlockFilesWriter.writeBranchAndMasterRuntimeFiles(parentsSeason, branch, context, env, writeProductionFeatures));
						change.getFiles().addAll(AirlockFilesWriter.doWriteConstantsFiles(parentsSeason, context, Stage.DEVELOPMENT));
						
						if (itemType.equals(REQUEST_ITEM_TYPE.ENTITLEMENTS)) {
							change.getFiles().addAll(AirlockFilesWriter.writeBranchPurchases(branch, parentsSeason, context, env, Stage.DEVELOPMENT));										
						}
						else {
							change.getFiles().addAll(AirlockFilesWriter.writeBranchFeatures(branch, parentsSeason, context, env, Stage.DEVELOPMENT));										
						}
					}
				} catch (IOException e) {
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
				}
				
				//rootFeature = parentsSeason.getRoot();
				if (destinationIsMaster) {
					rootFeature = parentsSeason.getRoot();
					rootPurchases = parentsSeason.getEntitlementsRoot();
				}
				else {
					Map<String, BaseAirlockItem> newDestinationAirlockItemsDB = Utilities.getAirlockItemsDBForBranchCopy(destinationBranchCopy, context);
				
					rootFeature = (RootItem)newDestinationAirlockItemsDB.get(parentsSeason.getRoot().getUniqueId().toString());
					rootPurchases = (RootItem)newDestinationAirlockItemsDB.get(parentsSeason.getEntitlementsRoot().getUniqueId().toString());
				}

			} else { //VALIDATE
				if (destinationIsMaster) {
					rootFeature = Utilities.findItemsRoot(airlockItemsMapCopy.get(newParentId), airlockItemsMapCopy);
					rootPurchases = Utilities.findItemsRoot(airlockItemsMapCopy.get(newParentId), airlockItemsMapCopy);
				}
				else {
					Map<String, BaseAirlockItem> newDestinationAirlockItemsDB = Utilities.getAirlockItemsDBForBranchCopy(destinationBranchCopy, context);				
					rootFeature = Utilities.findItemsRoot(newDestinationAirlockItemsDB.get(newParentId), newDestinationAirlockItemsDB);
					rootPurchases = Utilities.findItemsRoot(newDestinationAirlockItemsDB.get(newParentId), newDestinationAirlockItemsDB);
				}
			}

			String destBranchName = Constants.MASTER_BRANCH_NAME;
			if (!destinationBranchId.equals(Constants.MASTER_BRANCH_NAME)) {
				destBranchName = destinationBranch.getName() + ", " + destinationBranchId;
			}
			AuditLogWriter auditLogWriter = (AuditLogWriter) context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Import sub-tree under parent = " + newParentObj.getNameSpaceDotName() + ", " + newParentId + ", in branch " + destBranchName, userInfo);
			
			Webhooks.get(context).notifyChanges(change, context);
			
			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FIELD_UPDATED_SEASONS_FEATURES, rootFeature.toJson(OutputJSONMode.DISPLAY, context, env));
			res.put(Constants.JSON_FIELD_UPDATED_SEASONS_ENTITLEMENTS, rootPurchases.toJson(OutputJSONMode.DISPLAY, context, env));
			res.put(Constants.JSON_FIELD_NEW_SUBTREE_ID, oldToNewIDsMap.get(importedRootId));
			res.put(Constants.JSON_FIELD_STRING_CONFLICTS,stringsInConflict);
			res.put(Constants.JSON_FIELD_STRING_ADDED,addedStrings);

			return (Response.ok(res.toString())).build();
		} catch (MergeException e) {
			return sendError(Status.INTERNAL_SERVER_ERROR, Strings.mergeException  + e.getMessage());		
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	@GET
	@Path ("/find/{season-id}/{branch-id}")
	@ApiOperation(value = "Find features by given criteria", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Feature or parent not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response find(@PathParam("season-id")String seasonId,
						 @PathParam("branch-id")String branchId,
						 @ApiParam(value="mandatory") @QueryParam("pattern")String pattern,
						 @ApiParam(value="when not specified search in all areas", allowMultiple=true) @QueryParam("searchareas")Set<String> searchAreas,
						 @ApiParam(value="when not specified default search options will be used", allowMultiple=true) @QueryParam("options")Set<String> options,
						 @HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {


		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("find request: seasonId =" + seasonId + "\n, pattern = " + pattern + ", searchAreas = " + searchAreas + ", options = " + options);
		}

		String err = Utilities.validateLegalUUID(seasonId);
		if (err!=null) 
			return sendError(Status.BAD_REQUEST, Strings.illegalSeasonUUID + err);

		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, seasonId);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;

		//check user authorization
		UserInfo userInfo = UserInfo.validate("FeatureServices.find", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		FeatureFilter featureFilter;
		try {
			featureFilter = FeatureFilter.build(pattern, searchAreas, options);
		} catch (IllegalArgumentException e) {
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
		}
		
		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.FEATURES});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		
		
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();

		readWriteLock.readLock().lock();
		try {

			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			Season season = seasonsDB.get(seasonId);

			if (season == null) {
				return sendError(Status.NOT_FOUND, Strings.seasonNotFound);
			}

			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, seasonId);
			res.put(Constants.JSON_FIELD_BRANCH_ID, branchId);

			String rootId = season.getRoot().getUniqueId().toString();
			Map<String, BaseAirlockItem> airlockItemsDB = Utilities.getAirlockItemsDB (branchId, context);
			BaseAirlockItem rootElem = airlockItemsDB.get(rootId);
			JSONArray foundIds = new JSONArray();
			rootElem.find(featureFilter,foundIds);
			res.put("foundIds",foundIds);


			return (Response.ok()).entity(res.toString()).build();
		} catch (MergeBranch.MergeException e) {
			return sendError(Status.INTERNAL_SERVER_ERROR, Strings.mergeException  +e.getMessage());
		}finally {
			readWriteLock.readLock().unlock();
		}
	}

	private void updateSeasonsRootToNewAirlockObjects(Map<String, BaseAirlockItem> airlockItemsDB, ServletContext context) {
		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);

		Set<String> productIds = productsDB.keySet();
		for (String prodId:productIds) {
			Product prod = productsDB.get(prodId);
			for (int i=0; i<prod.getSeasons().size(); i++) {
				Season season = prod.getSeasons().get(i);
				
				String rootId  = season.getRoot().getUniqueId().toString();
				BaseAirlockItem newRoot = airlockItemsDB.get(rootId);
				season.setRoot((RootItem)newRoot);
				
				String purchasesRootId  = season.getEntitlementsRoot().getUniqueId().toString();
				BaseAirlockItem newPurchasesRoot = airlockItemsDB.get(purchasesRootId);
				season.setEntitlementsRoot((RootItem)newPurchasesRoot);
			}
		}
	}


	private void sendMailForCopy(Map<String,BaseAirlockItem> airlockItemsDB ,BaseAirlockItem newParentObj ,DataAirlockItem featureToCopy, String featureId,String newParentId, UserInfo userInfo,Environment env){
		FeatureItem featureToNotify = null;
		@SuppressWarnings("unchecked")
		Map<String, ArrayList<String>> followersFeaturesDB = (Map<String, ArrayList<String>>)context.getAttribute(Constants.FOLLOWERS_FEATURES_DB_PARAM_NAME);
		Boolean isProduction = false;//featureToCopy.getStage().toString().equals("PRODUCTION");
		ArrayList<String> emailFollowers = new ArrayList<>();
		if (featureToCopy.getType() == Type.FEATURE ) {
			featureToNotify = (FeatureItem) featureToCopy;
			ArrayList<String> followers = followersFeaturesDB.get(featureId);
			if(newParentObj.getType() == Type.FEATURE){
				ArrayList<String> newParentFollowers = followersFeaturesDB.get(newParentObj.getUniqueId().toString())  ;
				if(followers != null) {
					emailFollowers.addAll(followers);
				}
				if(newParentFollowers != null) {
					emailFollowers.addAll(newParentFollowers);
				}
			}
		} else if (featureToCopy.getType() == Type.CONFIGURATION_RULE || featureToCopy.getType() == Type.ORDERING_RULE) {
			BaseAirlockItem configParent = newParentObj;
			while (configParent.getType() != Type.FEATURE && configParent.getType() != Type.ROOT) {
				configParent = airlockItemsDB.get(configParent.getParent().toString());
			}
			if (configParent instanceof FeatureItem) {
				featureToNotify = (FeatureItem)configParent;
				emailFollowers = followersFeaturesDB.get(configParent.getUniqueId().toString());
			}
		}
		
		if (featureToNotify!=null) {
			String details = "The " + featureToCopy.getObjTypeStrByType() + " " + featureToCopy.getNameSpaceDotName() + " with ID " + featureToCopy.getUniqueId()+" was copied to " +newParentObj.getObjTypeStrByType()+" with ID "+ newParentId +"\n";
			Utilities.sendEmailForDataItem(context, featureToNotify, emailFollowers, details, null, null, isProduction, userInfo,env);
		}
	}
	Response sendError(Status status, String err)
	{
		logger.severe(err);
		return Response.status(status).entity(Utilities.errorMsgToErrorJSON(err)).build();
	}
	
	Response sendInfoError(Status status, UserInfo info)
	{
		return Response.status(status).entity(info.getErrorJson()).build();
	}
}
