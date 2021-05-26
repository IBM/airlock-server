package com.ibm.airlock;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
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

import com.ibm.airlock.admin.serialize.DataSerializer;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import com.ibm.airlock.utilities.Pair;
import com.ibm.airlock.Constants.ActionType;
import com.ibm.airlock.Constants.AirlockCapability;
import com.ibm.airlock.Constants.BranchStatus;
import com.ibm.airlock.Constants.BranchesOutputMode;
import com.ibm.airlock.Constants.CancelCheckoutMode;
import com.ibm.airlock.Constants.InputSampleGenerationMode;
import com.ibm.airlock.Constants.OutputJSONMode;
import com.ibm.airlock.Constants.Platform;
import com.ibm.airlock.Constants.REQUEST_ITEM_TYPE;
import com.ibm.airlock.Constants.RoleType;
import com.ibm.airlock.Constants.SimulationType;
import com.ibm.airlock.Constants.Stage;
import com.ibm.airlock.Constants.UtilityType;
import com.ibm.airlock.admin.*;
import com.ibm.airlock.admin.BaseAirlockItem.Type;
import com.ibm.airlock.admin.MergeBranch.MergeException;
import com.ibm.airlock.admin.Utilities.ProductErrorPair;
import com.ibm.airlock.admin.analytics.AirlockAnalytics;
import com.ibm.airlock.admin.analytics.AnalyticsDataCollection;
import com.ibm.airlock.admin.analytics.AnalyticsUtilities;
import com.ibm.airlock.admin.analytics.Experiment;
import com.ibm.airlock.admin.analytics.GlobalDataCollection;
import com.ibm.airlock.admin.analytics.Variant;
import com.ibm.airlock.admin.authentication.JwtData;
import com.ibm.airlock.admin.authentication.UserInfo;
import com.ibm.airlock.admin.authentication.UserRoles;
import com.ibm.airlock.admin.notifications.AirlockNotification;
import com.ibm.airlock.admin.operations.UserRoleSets;
import com.ibm.airlock.admin.operations.Webhooks;
import com.ibm.airlock.admin.purchases.EntitlementItem;
import com.ibm.airlock.admin.purchases.PurchaseOptionsItem;
import com.ibm.airlock.admin.operations.AirlockChange;
import com.ibm.airlock.admin.operations.AirlockChangeContent;
import com.ibm.airlock.admin.operations.AirlockAPIKey;
import com.ibm.airlock.admin.operations.AirlockAPIKeys;
import com.ibm.airlock.admin.operations.Roles;
import com.ibm.airlock.admin.operations.UserRoleSets.UserRoleSet;
import com.ibm.airlock.engine.Environment;
import com.ibm.airlock.engine.Version;
import com.ibm.airlock.admin.serialize.AirlockFilesWriter;
import com.ibm.airlock.admin.serialize.AuditLogWriter;
import com.ibm.airlock.admin.streams.AirlockStream;
import com.ibm.airlock.admin.translations.OriginalString;

@Path ("/admin/products")
@Api(value = "/products", description = "Airlock management API")
public class ProductServices{
	public static final Logger logger = Logger.getLogger(ProductServices.class.getName());

	@Context
	private ServletContext context;		

	@POST
	@Path ("/{product-id}")
	@ApiOperation(value = "Creates a product with the specified id.", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 500, message = "Internal error") })

	public Response addProductWithId(@PathParam("product-id")String product_id, 
			String newProduct, 
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("addProductWithId request: " + newProduct);
		}
		AirlockChange change = new AirlockChange();
		// use userInfo for more stringent checks. null if authorization is off
		UserInfo userInfo = UserInfo.validate("ProductServices.addProductWithId", context, assertion, null);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//verify that is legal GUID
		String err = Utilities.validateLegalUUID(product_id);
		if (err!=null) {
			return logReply(Status.BAD_REQUEST, Strings.illegalProductUUID + err);			
		}

		//verify that the specified GUID is not in use
		err = Utilities.verifyGUIDIsNotInUse(context, product_id);
		if (err!=null) {
			return logReply(Status.BAD_REQUEST, err);			
		}

		Pair<ValidationResults, LinkedList<AirlockChangeContent>> writeRes = doAddProduct (newProduct, userInfo, UUID.fromString(product_id), context, assertion, false);
		ValidationResults validationRes =  writeRes.getKey();

		if (validationRes.status!=null) {	
			//failure
			return logReply(validationRes.status, validationRes.error);
		}
		else {
			change.getFiles().addAll(writeRes.getValue());
			Webhooks.get(context).notifyChanges(change, context);
			//success - in this case the error is actually the results
			return (Response.ok(validationRes.error)).build();	
		}
	}
	@POST
	@ApiOperation(value = "Creates a product", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response addProduct(String newProduct, 
			@QueryParam("addGlobalAdministrators")Boolean addGlobalAdministrators,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {

		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("addProduct request: " + newProduct);
		}
		AirlockChange change = new AirlockChange();

		if (addGlobalAdministrators == null) 
			addGlobalAdministrators = false; //default value for addGlobalAdministrators is false 
		
		// use userInfo for more stringent checks. null if authorization is off
		UserInfo userInfo = UserInfo.validate("ProductServices.addProduct", context, assertion, null);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		Pair<ValidationResults, LinkedList<AirlockChangeContent>> writeRes = doAddProduct (newProduct, userInfo, null, context, assertion, addGlobalAdministrators);
		ValidationResults validationRes =  writeRes.getKey();
		if (validationRes.status!=null) {
			//failure
			return logReply(validationRes.status, validationRes.error);
		}
		else {
			//success - in this case the error is actually the results
			change.getFiles().addAll(writeRes.getValue());
			Webhooks.get(context).notifyChanges(change, context);
			return (Response.ok(validationRes.error)).build();	
		}							
	}

	private static Pair<ValidationResults, LinkedList<AirlockChangeContent>> createRetPair(ValidationResults key, LinkedList<AirlockChangeContent> value) {
		return new Pair<ValidationResults, LinkedList<AirlockChangeContent>>(key, value);
	}
	//validation results is returned with error code upon failure and with null error code upon success
	public static Pair<ValidationResults, LinkedList<AirlockChangeContent>> doAddProduct(String newProduct, UserInfo userInfo, UUID specifiedUniqueId, ServletContext context, String assertion, Boolean addGlobalAdministrators) throws JSONException {
		ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		readWriteLock.writeLock().lock();
		try {
			@SuppressWarnings("unchecked")
			Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);					

			@SuppressWarnings("unchecked")
			Map<String, UserRoleSet> usersDB = (Map<String, UserRoleSet>)context.getAttribute(Constants.AIRLOCK_USERS_DB_PARAM_NAME);
			
			JSONObject newProductJSON = null;
			try {
				newProductJSON = new JSONObject(newProduct);
			} catch (JSONException je) {
				String errMsg = Strings.illegalInputJSON + je.getMessage();
				return createRetPair(new ValidationResults (errMsg, Status.BAD_REQUEST), changesArr);
			}

			//verify that JSON does not contain uniqueId field
			if (newProductJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && newProductJSON.get(Constants.JSON_FIELD_UNIQUE_ID) !=null) {
				String errMsg = Strings.productWithId;
				return createRetPair(new ValidationResults (errMsg, Status.BAD_REQUEST), changesArr);
			}

			Product prod = new Product();

			ValidationResults validationRes = prod.validateProductJSON(newProductJSON, productsDB, context);
			if (validationRes!=null) 
				return createRetPair(validationRes, changesArr);

			try {
				prod.fromJSON(newProductJSON, null, context);
			} catch (JSONException je) {
				String errMsg = Strings.illegalInputJSON + je.getMessage();
				return createRetPair(new ValidationResults (errMsg, Status.BAD_REQUEST), changesArr);
			}

			if (specifiedUniqueId == null)
				prod.setUniqueId(UUID.randomUUID());
			else 
				prod.setUniqueId(specifiedUniqueId);

			productsDB.put(prod.getUniqueId().toString(), prod);
			
			//product users authorization
			Roles roles = (Roles) context.getAttribute(Constants.ROLES_PARAM_NAME);			
			UserRoleSets prodUsers = new UserRoleSets();
			if (addGlobalAdministrators) {
				UserRoleSets airlockUsers = (UserRoleSets)context.getAttribute(Constants.AIRLOCK_GLOBAL_USERS_PARAM_NAME);
				prodUsers = airlockUsers.cloneByRole(usersDB, prod.getUniqueId(), userInfo, RoleType.Administrator);
			}
			
			//add the product creator as admin to the  product
			if (userInfo!=null) {
				prodUsers.addUserRole(usersDB, userInfo.getId(), prod.getUniqueId(), RoleType.Administrator);
			}
			prod.setProductUsers(prodUsers);
			
			@SuppressWarnings("unchecked")
			Map<String,UserRoles> rolesPerProductMap = (Map<String,UserRoles>) context.getAttribute(Constants.USER_ROLES_PER_PRODUCT_PARAM_NAME);			
			
			UserRoles filter = new UserRoles(roles, prodUsers);
			rolesPerProductMap.put(prod.getUniqueId().toString(), filter);
			InternalUserGroups prodGroups = new InternalUserGroups();
			@SuppressWarnings("unchecked")
			Map<String,InternalUserGroups> groupsPerProductMap = (Map<String,InternalUserGroups>) context.getAttribute(Constants.USER_GROUPS_PER_PRODUCT_PARAM_NAME);
			groupsPerProductMap.put(prod.getUniqueId().toString(), prodGroups);
			
			//if product was created using an  Api Key - add the product to the key products 
			AirlockAPIKey apiKey = null;
			try {
				apiKey = JwtData.getApiKeyFromJWT(assertion, context);
				if (apiKey != null) {
					//the product was created using an api key - the key should be added an administrator roles for this product
					apiKey.addProductAdminRole(prod);
				}
			} catch (GeneralSecurityException e) {
				return createRetPair(new ValidationResults (e.getMessage(), Status.INTERNAL_SERVER_ERROR), changesArr);
			}
			
			//writing updated products list to S3
			try {
				changesArr.addAll(AirlockFilesWriter.writeProducts(productsDB, context, prod));
				changesArr.addAll(AirlockFilesWriter.writeProductUsers(prod, context));
				changesArr.addAll(AirlockFilesWriter.writeExperiments(prod, context, Stage.PRODUCTION));
				LinkedList<AirlockChangeContent> resChanges = AirlockFilesWriter.writeUserGroups(prodGroups, context, prod).getValue();
				if (resChanges != null) {
					changesArr.addAll(resChanges);
				}
				if (apiKey!=null) {
					changesArr.addAll(AirlockFilesWriter.writeAirlockApiKeys(context));
				}
			} catch (IOException e) {
				return createRetPair(new ValidationResults (e.getMessage(), Status.INTERNAL_SERVER_ERROR), changesArr);
			}			

			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Create new product: " + prod.toJson(false, false, false).toString(), userInfo); 

			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FIELD_UNIQUE_ID, prod.getUniqueId().toString());
			res.put(Constants.JSON_FIELD_NAME, prod.getName());
			res.put(Constants.JSON_FIELD_LAST_MODIFIED, prod.getLastModified().getTime());
			logger.info("Product added: " + res.toString());

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Added product: " + prod.toJson(false, false, false));
			}

			return createRetPair(new ValidationResults(res.toString(), null), changesArr); //if not error status - the message should be returned from request
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error adding product: ", e);
			return createRetPair(new ValidationResults ("Error adding product: " + e.getMessage(), Status.INTERNAL_SERVER_ERROR), changesArr);
		} finally {
			readWriteLock.writeLock().unlock();
		}

	}


	@PUT
	@Path ("/{product-id}")
	@ApiOperation(value = "Updates the specified product", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response updateProduct(@PathParam("product-id")String product_id, 
			String product, 
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {

		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("updateProduct request: product_id =" + product_id +", product = " + product);
		}
		AirlockChange change = new AirlockChange();
		ProductErrorPair prodErrPair = Utilities.getProduct(context, product_id);
		if (prodErrPair.error != null) {			
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		
		UserInfo userInfo = UserInfo.validate("ProductServices.updateProduct", context, assertion, prodErrPair.product);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		String err = Utilities.validateLegalUUID(product_id);
		if (err!=null) {
			return logReply(Status.BAD_REQUEST, Strings.illegalProductUUID + err);				
		}


		ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
		readWriteLock.writeLock().lock();
		try {
			@SuppressWarnings("unchecked")
			Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);		

			Product productToUpdate = productsDB.get(product_id);
			if (productToUpdate == null) {
				return logReply(Status.NOT_FOUND, Strings.productNotFound);			
			}
			change.setProduct(productToUpdate);

			JSONObject updatedProductJSON = null;
			try {
				updatedProductJSON = new JSONObject(product);
			} catch (JSONException je) {
				return logReply(Status.BAD_REQUEST, Strings.illegalInputJSON + je.getMessage());
			}

			//if not set - set the uniqueId to be the id path param
			if (!updatedProductJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) || updatedProductJSON.get(Constants.JSON_FIELD_UNIQUE_ID) == null) {
				updatedProductJSON.put(Constants.JSON_FIELD_UNIQUE_ID, product_id);
			}
			else {
				//verify that product-id in path is identical to uniqueId in request pay-load  
				if (!updatedProductJSON.getString(Constants.JSON_FIELD_UNIQUE_ID).equals(product_id)) {
					return logReply(Status.BAD_REQUEST, Strings.productWithDifferentId);					
				}
			}

			ValidationResults validationRes = productToUpdate.validateProductJSON(updatedProductJSON, productsDB, context);
			if (validationRes!=null) {
				return logReply(validationRes.status, validationRes.error);
			}

			boolean publishToAnalyticsSvrRequired = productToUpdate.isPublishToAnalyticsSvrRequired(updatedProductJSON);
			if (publishToAnalyticsSvrRequired) {
				ValidationResults pushingExpToAnalyticsSrvRes = null;
				try {
					//pushingExpToAnalyticsSrvRes = AnalyticsUtilities.updateAnalyticsServer(context, null, null, now, null, null, productToUpdate.getUniqueId(), updatedProductJSON.getString(Constants.JSON_FIELD_NAME));
					pushingExpToAnalyticsSrvRes = AnalyticsUtilities.updateNewProductNameToAnalytics(context, productToUpdate, updatedProductJSON.getString(Constants.JSON_FIELD_NAME));
				} catch (Exception e) {
					pushingExpToAnalyticsSrvRes = new ValidationResults(Strings.failUpdatingExperiment + e.getMessage(), Status.INTERNAL_SERVER_ERROR);
				} 

				if (pushingExpToAnalyticsSrvRes!=null) {
					return logReply(pushingExpToAnalyticsSrvRes.status, pushingExpToAnalyticsSrvRes.error);
				}
			}

			//finally - actually update the product.
			String updateDetails = productToUpdate.updateProduct(updatedProductJSON);

			if (!updateDetails.isEmpty()) { //if some fields were changed
				//writing updated products list to S3
				change.setProduct(productToUpdate);
				try {
					change.getFiles().addAll(AirlockFilesWriter.writeProducts(productsDB, context, productToUpdate));
					Webhooks.get(context).notifyChanges(change, context);
				} catch (IOException e) {
					return logReply(Status.INTERNAL_SERVER_ERROR, e.getMessage());
				}

				AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
				auditLogWriter.log("Update product: " + productToUpdate.getName() +", "+ product_id + ",   " + updateDetails, userInfo);			
			}

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Updated product: " + productToUpdate.toJson(false, false, true) + "\n updatd details: " + updateDetails);
			}

			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FIELD_UNIQUE_ID, productToUpdate.getUniqueId().toString());
			res.put(Constants.JSON_FIELD_NAME, productToUpdate.getName());
			res.put(Constants.JSON_FIELD_LAST_MODIFIED, productToUpdate.getLastModified().getTime());
			return (Response.ok(res.toString())).build();		
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error updating product: " + product_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error updating product: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	@DELETE
	@Path ("/{product-id}")
	@ApiOperation(value = "Deletes the specified product", response = String.class)	
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class),
			@ApiResponse(code = 404, message = "Product not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response deleteProduct(@PathParam("product-id")String product_id, @HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("deleteProduct request: product_id =" + product_id);
		}
		AirlockChange change = new AirlockChange();
		
		ProductErrorPair prodErrPair = Utilities.getProduct(context, product_id);
		if (prodErrPair.error != null) {			
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}

		UserInfo userInfo = UserInfo.validate("ProductServices.deleteProduct", context, assertion, prodErrPair.product);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		String err = Utilities.validateLegalUUID(product_id);
		if (err!=null) {
			return logReply(Status.BAD_REQUEST, Strings.illegalProductUUID + err);
		}

		ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
		readWriteLock.writeLock().lock();
		try {			

			@SuppressWarnings("unchecked")
			Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);

			if (!productsDB.containsKey(product_id)) {
				return logReply(Status.NOT_FOUND, Strings.productNotFound);		
			}

			Product prodToDel = productsDB.get(product_id);
			change.setProduct(prodToDel);
			if (prodToDel.containSubItemInProductionStage()) {
				return logReply(Status.BAD_REQUEST, Strings.productWithFeatureProd);
			}

			if (prodToDel.containExperimentsInProductionStage()) {
				return logReply(Status.BAD_REQUEST, Strings.productWithExpProd);
			}
			
			if (prodToDel.containStreamsInProductionStage()) {
				return logReply(Status.BAD_REQUEST, Strings.productWithExpProd);
			}
			
			if (prodToDel.containNotificationsInProductionStage()) {
				return logReply(Status.BAD_REQUEST, Strings.productWithNotificationProd);
			}

			if (prodToDel.getExperimentsMutualExclusionGroup().getExperiments().size()>0) {
				//publish experiments deletion to analytics server
				ValidationResults pushingExpToAnalyticsSrvRes = null;
				//Date now = new Date();
				try {
					//pushingExpToAnalyticsSrvRes = AnalyticsUtilities.updateAnalyticsServer(context, null, null, now, null, null, prodToDel.getUniqueId(), null);
					pushingExpToAnalyticsSrvRes = AnalyticsUtilities.updateDeletedProductToAnalytics(context, prodToDel);
				} catch (Exception e) {
					pushingExpToAnalyticsSrvRes = new ValidationResults(Strings.failDeletingExperiment + e.getMessage(), Status.INTERNAL_SERVER_ERROR);
				} 

				if (pushingExpToAnalyticsSrvRes!=null) {
					return logReply(pushingExpToAnalyticsSrvRes.status, pushingExpToAnalyticsSrvRes.error);
				}
			}

			//remove the seasons, features and branches from DBs
			prodToDel.productDeletionCleanup(context, userInfo);

			//remove deleted product roles from api keys
			AirlockAPIKeys apiKeys = (AirlockAPIKeys)context.getAttribute(Constants.API_KEYS_PARAM_NAME);
			boolean keysUpdated = apiKeys.removeProductRolesFromKeys(prodToDel);
			
			try {
				//removing product folder from S3 
				change.getFiles().addAll(removeProductFolder(prodToDel));

				//writing updated products list to S3
				change.getFiles().addAll(AirlockFilesWriter.writeProducts(productsDB, context, null));
				if (keysUpdated) {
					change.getFiles().addAll(AirlockFilesWriter.writeAirlockApiKeys(context));
				}
				Webhooks.get(context).notifyChanges(change, context);
			} catch (IOException e) {
				return logReply(Status.INTERNAL_SERVER_ERROR, e.getMessage());
			}
			Utilities.sendEmailForProduct(context,userInfo,prodToDel);

			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Delete product: uniqueId:" + product_id + ", name:" + prodToDel.getName(), userInfo); 

			logger.info("Product " + product_id + " was deleted");

			return (Response.ok()).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error deleting product " + product_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error deleting product: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}


	private LinkedList<AirlockChangeContent> removeProductFolder(Product product) throws IOException {
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		String separator = ds.getSeparator();
		try {
			ds.deleteFolderContent(Constants.SEASONS_FOLDER_NAME+separator+product.getUniqueId().toString());
			AirlockChangeContent changeContent = AirlockChangeContent.getAdminChange(Constants.FOLDER_DELETED, Constants.SEASONS_FOLDER_NAME+separator+product.getUniqueId().toString(), Stage.PRODUCTION);
			LinkedList<AirlockChangeContent> toRet = new LinkedList<AirlockChangeContent>();
			toRet.add(changeContent);
			return toRet;
		} catch (IOException ioe){
			//failed deleting 3 times to s3. Set server state to ERROR.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			String error = Strings.failedDeletingProducts + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			throw new IOException(error);				
		} 			
	}

	private LinkedList<AirlockChangeContent> removeSeasonFiles(Season season) throws IOException {
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		String separator = ds.getSeparator();
		try {
			String seasonId = season.getUniqueId().toString();
			String productId = season.getProductId().toString();

			String translationsFolderName = Constants.SEASONS_FOLDER_NAME+separator+productId+separator+seasonId+separator+Constants.TRANSLATIONS_FOLDER_NAME;
			ds.deleteFolderContent(translationsFolderName);
			AirlockChangeContent changeContent = AirlockChangeContent.getAdminChange(Constants.FOLDER_DELETED, translationsFolderName, Stage.PRODUCTION);
			LinkedList<AirlockChangeContent> toRet = new LinkedList<AirlockChangeContent>();
			toRet.add(changeContent);
			
			String seasonFolderName = Constants.SEASONS_FOLDER_NAME+separator+productId+separator+seasonId;
			ds.deleteFolderContent(seasonFolderName, true); //delete runtime files
			 toRet.add(AirlockChangeContent.getAdminChange(Constants.FOLDER_DELETED, seasonFolderName, Stage.PRODUCTION));
			return toRet;
		} catch (IOException ioe){
			//failed deleting 3 times to s3. Set server state to ERROR.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			String error = Strings.failedDeletingSeasons + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			throw new IOException(error);					
		} 
	}


	@GET
	@Path ("/{product-id}")
	@ApiOperation(value = "Returns the specified product", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Product not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getProduct(@PathParam("product-id")String product_id, @HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {

		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getProduct request: product_id =" + product_id);
		}

		ProductErrorPair prodErrPair = Utilities.getProduct(context, product_id);
		if (prodErrPair.error != null) {			
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		
		UserInfo userInfo = UserInfo.validate("ProductServices.getProduct", context, assertion, prodErrPair.product);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		String err = Utilities.validateLegalUUID(product_id);
		if (err!=null) {
			String errMsg = Strings.illegalProductUUID + err;
			logger.severe(errMsg);
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
		}

		ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
		readWriteLock.readLock().lock();
		try {			

			@SuppressWarnings("unchecked")
			Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);		

			Product prod = productsDB.get(product_id);

			if (prod == null) {
				String errMsg = Strings.productNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			return (Response.ok(prod.toJson(true, context, userInfo, true, true).toString())).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error getting product: " + product_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error getting product: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	@GET
	@ApiOperation(value = "Returns the product list", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getProducts(@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getProducts request");
		}

		UserInfo userInfo = UserInfo.validate("ProductServices.getProducts", context, assertion, null);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
		readWriteLock.readLock().lock();
		try {
			@SuppressWarnings("unchecked")
			Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);		

			JSONArray products = new JSONArray();

			Set<String> ids = productsDB.keySet();
			for (String id:ids) {
				Product p = productsDB.get(id);
				//if user does not have permission to call getProduct for a specific product it means that he is not viewer in this product
				//return only products in which the user is viewer
				UserInfo prodUserInfo = UserInfo.validate("ProductServices.getProduct", context, assertion, p);
				if (prodUserInfo != null && prodUserInfo.getErrorJson() != null)
					continue;
				
				products.add(p.toJson(false, context, userInfo, false, true)); //without seasons, with experiments 
			}

			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FIELD_PRODUCTS, products);

			return (Response.ok(res.toString())).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error getting products: " + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error getting products " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}	

	@GET 
	@Path("/seasons")	
	@ApiOperation(value = "Returns all seasons grouped by product", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 500, message = "Internal error") })

	public Response getSeasons(@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {

		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getSeasons request");
		}

		UserInfo userInfo = UserInfo.validate("ProductServices.getSeasons", context, assertion, null);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
		readWriteLock.readLock().lock();
		try {
			@SuppressWarnings("unchecked")
			Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);		

			JSONArray products = new JSONArray();

			Set<String> ids = productsDB.keySet();
			for (String id:ids) {
				Product p = productsDB.get(id);
				
				//if user does not have permission to call getProduct for a specific product it means that he is not viewer in this product
				//return only products in which the user is viewer
				UserInfo prodUserInfo = UserInfo.validate("ProductServices.getProduct", context, assertion, p);
				if (prodUserInfo != null && prodUserInfo.getErrorJson() != null)
					continue;
				
				
				products.add(p.toJson(true, context, userInfo, true, true)); //with seasons, with experiments
			}

			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FIELD_PRODUCTS, products); 

			return (Response.ok(res.toString())).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error getting seasons :", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error getting seasons: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	@POST 
	@Path("/{product-id}/seasons")
	@ApiOperation(value = "Creates a new season for the specified product", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Product not found"),
			@ApiResponse(code = 500, message = "Internal error") })	
	public Response addSeason(@PathParam ("product-id") String product_id, String newSeason,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("addSeason request: product_id =" + product_id +", newSeason = " + newSeason);
		}
		AirlockChange change = new AirlockChange();
		ProductErrorPair prodErrPair = Utilities.getProduct(context, product_id);
		if (prodErrPair.error != null) {			
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		change.setProduct(prodErrPair.product);
		
		UserInfo userInfo = UserInfo.validate("ProductServices.addSeason", context, assertion, prodErrPair.product);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		Pair<ValidationResults, LinkedList<AirlockChangeContent>> writeRes = doAddSeason (product_id, newSeason, userInfo, null, context);
		ValidationResults validationRes =  writeRes.getKey();
		if (validationRes.status!=null) {		
			//failure
			return logReply(validationRes.status, validationRes.error);
		}
		else {
			//success - in this case the error is actually the results
			change.getFiles().addAll(writeRes.getValue());
			Webhooks.get(context).notifyChanges(change, context);
			return (Response.ok(validationRes.error)).build();	
		}		
	}

	@POST 
	@Path("/{product-id}/seasons/{season-id}")
	@ApiOperation(value = "Creates a new season with the specified id for the specified product.", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Product not found"),
			@ApiResponse(code = 500, message = "Internal error") })	
	public Response addSeasonWithId(@PathParam ("product-id") String product_id, @PathParam("season-id")String season_id, String newSeason,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("addSeasonWithId request: product_id =" + product_id + ", season_id =" + season_id + ", newSeason = " + newSeason);
		}
		AirlockChange change = new AirlockChange();

		ProductErrorPair prodErrPair = Utilities.getProduct(context, product_id);
		if (prodErrPair.error != null) {			
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		change.setProduct(prodErrPair.product);
		
		UserInfo userInfo = UserInfo.validate("ProductServices.addSeasonWithId", context, assertion, prodErrPair.product);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//verify that is legal GUID
		String err = Utilities.validateLegalUUID(season_id);
		if (err!=null) 	
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalSeasonUUID + err);

		//verify that the specified GUID is not in use
		err = Utilities.verifyGUIDIsNotInUse(context, season_id);
		if (err!=null) {
			logger.severe(err);
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(err)).build();				
		}

		Pair<ValidationResults, LinkedList<AirlockChangeContent>> writeRes = doAddSeason (product_id, newSeason, userInfo, UUID.fromString(season_id), context); 
		ValidationResults validationRes = writeRes.getKey();
		if (validationRes.status!=null) {		
			return logReply(validationRes.status, validationRes.error);
		}
		else {
			//success - in this case the error is actually the results
			change.getFiles().addAll(writeRes.getValue());
			Webhooks.get(context).notifyChanges(change, context);
			return (Response.ok(validationRes.error)).build();	
		}		
	}

	//validation results is returned with error code upon failure and with null error code upon success	
	public static Pair<ValidationResults, LinkedList<AirlockChangeContent>> doAddSeason(String product_id, String newSeason, UserInfo userInfo, UUID specifiedUniqueId, ServletContext context) throws JSONException {
		String err = Utilities.validateLegalUUID(product_id);
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		if (err!=null) {
			String errMsg = Strings.illegalProductUUID + err;
			return createRetPair(new ValidationResults (errMsg, Status.BAD_REQUEST), changesArr);			
		}

		ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
		readWriteLock.writeLock().lock();
		try {			

			@SuppressWarnings("unchecked")
			Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);
			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);
			@SuppressWarnings("unchecked")
			Map<String, BaseAirlockItem> airlockItemsDB = (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);
			
			Product prod = productsDB.get(product_id);
			if (prod == null) { //product not found
				String errMsg = Strings.productNotFound;
				logger.severe(errMsg);
				return createRetPair(new ValidationResults (errMsg, Status.NOT_FOUND), changesArr);												
			}			

			Season newSeasonObj = new Season(true); //init root node

			JSONObject newSeasonJSON = null;
			try {
				newSeasonJSON = new JSONObject(newSeason);			
			} catch (JSONException je) {
				String errMsg = Strings.illegalInputJSON + je.getMessage();
				return createRetPair(new ValidationResults (errMsg, Status.BAD_REQUEST), changesArr);	
			}

			//verify that JSON does not contain uniqueId field
			if (newSeasonJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && newSeasonJSON.get(Constants.JSON_FIELD_UNIQUE_ID) !=null) {
				String errMsg = Strings.seasonWithId ;
				return createRetPair(new ValidationResults (errMsg, Status.BAD_REQUEST), changesArr);	
			}

			//verify that JSON does not contain different product-id then the path parameter
			if (newSeasonJSON.containsKey(Constants.JSON_FIELD_PRODUCT_ID) && newSeasonJSON.get(Constants.JSON_FIELD_PRODUCT_ID) !=null) {
				if (!product_id.equals(newSeasonJSON.getString(Constants.JSON_FIELD_PRODUCT_ID))) {
					String errMsg = Strings.seasonProductWithDifferentId;
					return createRetPair(new ValidationResults (errMsg, Status.BAD_REQUEST), changesArr);	
				}
			}
			else {		
				newSeasonJSON.put(Constants.JSON_FIELD_PRODUCT_ID, product_id);
			}

			ValidationResults validationRes = newSeasonObj.validateSeasonJSON(newSeasonJSON, context);
			if (validationRes!=null) 
				return createRetPair(validationRes, changesArr);

			newSeasonObj.fromJSON(newSeasonJSON);						

			UUID newSeaosnId = null;
			if (specifiedUniqueId == null)
				newSeaosnId = UUID.randomUUID();
			else 
				newSeaosnId = specifiedUniqueId;

			newSeasonObj.setUniqueId(newSeaosnId);
			newSeasonObj.getRoot().setSeasonId(newSeaosnId);
			newSeasonObj.getEntitlementsRoot().setSeasonId(newSeaosnId);
			
			newSeasonObj.setServerVersion(Constants.CURRENT_SERVER_VERSION);

			//should be added to db before duplication since the season is required in duplication
			seasonsDB.put(newSeasonObj.getUniqueId().toString(), newSeasonObj);			

			//if not first season - close last season and duplicate features, input schemas and input schema definitions from last to new season
			Season lastSeason = null;
			try {
				if (prod.getSeasons().size()>0) {
					lastSeason = prod.getSeasons().getLast();
					lastSeason.setMaxVersion(newSeasonObj.getMinVersion());	

					HashMap<String, String> oldToDuplicatedFeaturesId = new  HashMap<String, String>();
					newSeasonObj.duplicateFeatures(lastSeason, airlockItemsDB, oldToDuplicatedFeaturesId, context);
					newSeasonObj.duplicatePurchases(lastSeason, airlockItemsDB, oldToDuplicatedFeaturesId, context);
					
					@SuppressWarnings("unchecked")
					Map<String, AirlockUtility> utilitiesDB = (Map<String, AirlockUtility>)context.getAttribute(Constants.UTILITIES_DB_PARAM_NAME);

					@SuppressWarnings("unchecked")
					Map<String, AirlockStream> streamsDB = (Map<String, AirlockStream>)context.getAttribute(Constants.STREAMS_DB_PARAM_NAME);
					
					@SuppressWarnings("unchecked")
					Map<String, AirlockNotification> notificationsDB = (Map<String, AirlockNotification>)context.getAttribute(Constants.NOTIFICATIONS_DB_PARAM_NAME);

					@SuppressWarnings("unchecked")
					Map<String, OriginalString> originalStringsDB = (Map<String, OriginalString>)context.getAttribute(Constants.ORIG_STRINGS_DB_PARAM_NAME);

					newSeasonObj.duplicateInputSchema(lastSeason);
					newSeasonObj.duplicateUtilities(lastSeason, utilitiesDB, context);
					newSeasonObj.duplicateStreams(lastSeason, streamsDB, context);
					newSeasonObj.duplicateNotifications(lastSeason, notificationsDB, context);
					newSeasonObj.duplicateOriginalStrings(lastSeason, originalStringsDB);
					newSeasonObj.duplicateAnalytics(lastSeason, oldToDuplicatedFeaturesId, context);
					newSeasonObj.duplicateBranchesCollection(lastSeason, oldToDuplicatedFeaturesId, context);	
					if (lastSeason.getEncryptionKey() != null) {
						//new season uses the same encryption key as the last season
						newSeasonObj.setEncryptionKey(lastSeason.getEncryptionKey());
					}
					else {
						newSeasonObj.setEncryptionKey(Season.generateEncryptionKey());
					}

					newSeasonObj.replaceNewPurchaseIdsInPremiumFeatures(oldToDuplicatedFeaturesId);
					newSeasonObj.replaceNewPurchaseIdsInBundles(oldToDuplicatedFeaturesId);
				} else {
					newSeasonObj.generateEmptyInputSchema();				
					newSeasonObj.generateInitialUtilities(context);
					newSeasonObj.generateEmptyAnalytics(context);
					newSeasonObj.generateEmptyStreams();
					newSeasonObj.generateEmptyNotifications(context);	
					newSeasonObj.setEncryptionKey(Season.generateEncryptionKey());
				}
			} catch (Exception e) {
				//clean the db if season wasn't added after all
				seasonsDB.remove(newSeasonObj.getUniqueId().toString());
				throw e;
			}

			String missingBranchError = newSeasonObj.isMissingBranchUsedByExperiment(context);
			if (missingBranchError!=null) {
				logger.severe(missingBranchError);
				return createRetPair(new ValidationResults (missingBranchError, Status.BAD_REQUEST), changesArr);
			}

			prod.addSeason(newSeasonObj);
			
			//add new root node to the featuresDB
			airlockItemsDB.put(newSeasonObj.getRoot().getUniqueId().toString(), newSeasonObj.getRoot());
		
			//add new purchases root node to the featuresDB
			airlockItemsDB.put(newSeasonObj.getEntitlementsRoot().getUniqueId().toString(), newSeasonObj.getEntitlementsRoot());

			//writing updated products list to S3
			try {
				
				changesArr.addAll(AirlockFilesWriter.writeProducts(productsDB, context, prod));	
				changesArr.addAll(AirlockFilesWriter.writeServerInfo(newSeasonObj, context));
				changesArr.addAll(AirlockFilesWriter.writeEncryptionKey(newSeasonObj, context));
				changesArr.addAll(AirlockFilesWriter.writeUserGroupsRuntimeForSeason(context, prod, newSeasonObj).getValue());
				changesArr.addAll(AirlockFilesWriter.writeProductRuntimeForSeason(context, prod, newSeasonObj, Stage.PRODUCTION));
				if (prod.getSeasons().size()>1) {
					if (lastSeason.getRuntimeEncryption()!=newSeasonObj.getRuntimeEncryption()) {
						//write translation files with new encryption mode
						changesArr.addAll(AirlockFilesWriter.writeAllLocalesStringsFiles(newSeasonObj, context, true, true));
					}
					else {
						//copy translation file from previous season
						changesArr.addAll(copyTranslationsFolderFromLastSeason(lastSeason, newSeasonObj, context));	
					}
				}
			} catch (IOException e) {
				return createRetPair(new ValidationResults (e.getMessage(), Status.INTERNAL_SERVER_ERROR), changesArr);
			}						

			Environment env = new Environment();
			env.setServerVersion(newSeasonObj.getServerVersion());
			env.setAirlockItemsDB(airlockItemsDB);

			//writing updated features and input schemas list to S3
			try {
				Pair<String,LinkedList<AirlockChangeContent>> writeRes = AirlockFilesWriter.writeFeatures (newSeasonObj, true, context, env);
				err = writeRes.getKey();
				if (err!=null) {
					return createRetPair(new ValidationResults (err, Status.INTERNAL_SERVER_ERROR), changesArr);
				}
				changesArr.addAll(AirlockFilesWriter.writeInputSchema(newSeasonObj, context, Stage.PRODUCTION));
				changesArr.addAll(AirlockFilesWriter.writeAnalytics(newSeasonObj, context, Constants.MASTER_BRANCH_NAME, env, Stage.PRODUCTION));
				changesArr.addAll(AirlockFilesWriter.writeSeasonUtilities(newSeasonObj, true, context, UtilityType.MAIN_UTILITY));
				changesArr.addAll(AirlockFilesWriter.writeSeasonUtilities(newSeasonObj, true, context, UtilityType.STREAMS_UTILITY));
				changesArr.addAll(AirlockFilesWriter.writeOriginalStrings(newSeasonObj, context, Stage.PRODUCTION));	
				if (prod.getSeasons().size() == 1) {
					//if this is the first season => generate the English strings files a s well 
					changesArr.addAll(AirlockFilesWriter.writeEnStringsFiles(newSeasonObj, context, true));
				}
				changesArr.addAll(AirlockFilesWriter.doWriteDefaultsFile(newSeasonObj, context, Stage.PRODUCTION));
				changesArr.addAll(AirlockFilesWriter.writeSeasonBranches(newSeasonObj, context, env, Stage.PRODUCTION));
				for (Branch branch:newSeasonObj.getBranches().getBranchesList()) {
					changesArr.addAll(AirlockFilesWriter.writeBranchFeatures(branch, newSeasonObj, context, env, Stage.PRODUCTION));
					changesArr.addAll(AirlockFilesWriter.writeBranchPurchases(branch, newSeasonObj, context, env, Stage.PRODUCTION));
					changesArr.addAll(AirlockFilesWriter.writeAnalytics(newSeasonObj, context, branch.getUniqueId().toString(), env, Stage.PRODUCTION));
					env.setAirlockItemsDB(Utilities.getAirlockItemsDB(branch.getUniqueId().toString(), context));
					changesArr.addAll(AirlockFilesWriter.writeBranchRuntime(branch, newSeasonObj, context, env, true));
					env.setAirlockItemsDB(airlockItemsDB);
				}
				changesArr.addAll(AirlockFilesWriter.writeSeasonStreams(newSeasonObj, true, context));
				changesArr.addAll(AirlockFilesWriter.writeSeasonStreamsEvents(newSeasonObj, context));	
				changesArr.addAll(AirlockFilesWriter.writeSeasonNotifications(newSeasonObj, true, context, false));
				changesArr.addAll(AirlockFilesWriter.doWritePurchases(newSeasonObj, context, Stage.PRODUCTION));
			} catch (IOException e) {
				return createRetPair(new ValidationResults (e.getMessage(), Status.INTERNAL_SERVER_ERROR), changesArr);
			}

			//writing last season if exist (max version changed)
			if (lastSeason != null) {
				try {
					Pair<String,LinkedList<AirlockChangeContent>> writeRes = AirlockFilesWriter.writeFeatures (lastSeason, true, context, env); 
					err = writeRes.getKey();
					if (err!=null) {
						return createRetPair(new ValidationResults (err, Status.INTERNAL_SERVER_ERROR), changesArr);
					}
					changesArr.addAll(writeRes.getValue());
					//writeInputSchemas(lastSeason);					

				} catch (IOException e) {
					return createRetPair(new ValidationResults (e.getMessage(), Status.INTERNAL_SERVER_ERROR), changesArr);
				}
			}

			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Create new season to propduct " + prod.getName() +": " + newSeasonObj.toJson(true).toString(), userInfo);

			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FIELD_UNIQUE_ID, newSeasonObj.getUniqueId().toString());
			res.put(Constants.JSON_FIELD_LAST_MODIFIED, newSeasonObj.getLastModified().getTime());

			logger.info("Season added to product '" + product_id + "': "+ res.toString());

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Added season: " + newSeasonObj.toJson(true));
			}
			Utilities.sendEmailForSeason(context, userInfo, newSeasonObj, "created");
			return createRetPair(new ValidationResults(res.toString(), null), changesArr); //if not error status - the message should be returned from request			
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error adding season: ", e);
			return createRetPair(new ValidationResults ("Error adding season: " + e.getMessage(), Status.INTERNAL_SERVER_ERROR), changesArr);
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}
	private static LinkedList<AirlockChangeContent> copyTranslationsFolderFromLastSeason(Season lastSeason, Season newSeason, ServletContext context) throws IOException, JSONException {
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		String separator = ds.getSeparator(); 
		String srcPath = Constants.SEASONS_FOLDER_NAME+separator+lastSeason.getProductId().toString()+
				separator+lastSeason.getUniqueId().toString()+separator+Constants.TRANSLATIONS_FOLDER_NAME;

		String destinationPath = Constants.SEASONS_FOLDER_NAME+separator+newSeason.getProductId().toString()+
				separator+newSeason.getUniqueId().toString()+separator+Constants.TRANSLATIONS_FOLDER_NAME;
		
		ds.copyFolder(srcPath, destinationPath, true);

		String oldEnStringsFile = destinationPath + separator + Constants.STRINGS_FILE_NAME_PREFIX + Constants.DEFAULT_LANGUAGE + Constants.STRINGS_FILE_NAME_EXTENSION;
		if (ds.isFileExists(oldEnStringsFile)) {
			newSeason.getOriginalStrings().upgrade("V2", context, false); 
		}
		return changesArr;
	}	

	@PUT
	@Path ("/seasons/{season-id}")
	@ApiOperation(value = "Updates the specified season", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response updateSeason(@PathParam("season-id")String season_id, String season,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("updateSeason request: season_id =" + season_id +", season = " + season);
		}
		AirlockChange change = new AirlockChange();
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, season_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);
		
		UserInfo userInfo = UserInfo.validate("ProductServices.updateSeason", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		String err = Utilities.validateLegalUUID(season_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalSeasonUUID + err);

		ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
		readWriteLock.writeLock().lock();
		try {
			@SuppressWarnings("unchecked")
			Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);
			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			Season seasonToUpdate = seasonsDB.get(season_id);
			if (seasonToUpdate == null) {
				return logReply(Status.NOT_FOUND, Strings.seasonNotFound);				
			}
			change.setSeason(seasonToUpdate);

			JSONObject updatedSeasonJSON = null;
			try {
				updatedSeasonJSON = new JSONObject(season);
			} catch (JSONException je) {
				String errMsg = Strings.illegalInputJSON + je.getMessage();
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			//if not set - set the uniqueId to be the id path param
			if (!updatedSeasonJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) || updatedSeasonJSON.get(Constants.JSON_FIELD_UNIQUE_ID) == null) {
				updatedSeasonJSON.put(Constants.JSON_FIELD_UNIQUE_ID, season_id);
			}
			else {
				//verify that season-id in path is identical to uniqueId in request pay-load  
				if (!updatedSeasonJSON.getString(Constants.JSON_FIELD_UNIQUE_ID).equals(season_id)) {
					String errMsg = Strings.seasonWithDifferentId;
					logger.severe(errMsg);
					return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();					
				}
			}

			ValidationResults validationRes = seasonToUpdate.validateSeasonJSON(updatedSeasonJSON, context);
			if (validationRes!=null) {
				return logReply(validationRes.status, validationRes.error);
			}

			boolean orgRuntimeEncryption = seasonToUpdate.getRuntimeEncryption();
			String updateDetails = seasonToUpdate.updateSeason(updatedSeasonJSON, productsDB);
			boolean newRuntimeEncryption = seasonToUpdate.getRuntimeEncryption();
			
			if (!updateDetails.isEmpty()) { //if some fields were changed
				Product prod = productsDB.get(seasonToUpdate.getProductId().toString());
				int seasonPos = seasonToUpdate.getSeasonPosition(prod, season_id);
				Season prevSeason = null;
				Season nextSeason = null;

				if (seasonPos != 0) {
					prevSeason = prod.getSeasons().get(seasonPos-1);
				}

				if (seasonPos < prod.getSeasons().size()-1) {
					nextSeason = prod.getSeasons().get(seasonPos+1);
				}

				//writing updated products list to S3
				try {
					change.getFiles().addAll(AirlockFilesWriter.writeProducts(productsDB, context, prod));
				} catch (IOException e) {
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
				}

				Map<String, BaseAirlockItem> airlockItemsDB = Utilities.getAirlockItemsDB(Constants.MASTER_BRANCH_NAME, context);
				
				try {
					Environment env = new Environment();
					env.setAirlockItemsDB(airlockItemsDB);
					env.setServerVersion(seasonToUpdate.getServerVersion());
					env.setBranchId(Constants.MASTER_BRANCH_NAME);
					
					Pair<String,LinkedList<AirlockChangeContent>> writeRes = AirlockFilesWriter.writeFeatures (seasonToUpdate, true, context, env); 
					err = writeRes.getKey();					
					if (err!=null) {
						return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(err)).build();
					}
					change.getFiles().addAll(writeRes.getValue());

					//writing prevSeason since its max version may changed
					if (prevSeason!=null) {	
						Environment prevEnv = new Environment();
						env.setAirlockItemsDB(airlockItemsDB);
						env.setServerVersion(prevSeason.getServerVersion());
						env.setBranchId(Constants.MASTER_BRANCH_NAME);
						
						env.setAirlockItemsDB(airlockItemsDB);
						env.setServerVersion(prevSeason.getServerVersion());
						
						writeRes = AirlockFilesWriter.writeFeatures (prevSeason, true, context, prevEnv); 
						err = writeRes.getKey();
						if (err!=null) {
							return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(err)).build();
						}
						change.getFiles().addAll(writeRes.getValue());
						//writeInputSchemas(prevSeason); //write inputSchemas of the prev season since have min/max version fields						
					}

					//writing nextSeason since its min version may changed
					if (nextSeason!=null) {
						Environment nextEnv = new Environment();
						env.setAirlockItemsDB(airlockItemsDB);
						env.setServerVersion(nextSeason.getServerVersion());
						env.setBranchId(Constants.MASTER_BRANCH_NAME);
						
						writeRes = AirlockFilesWriter.writeFeatures (nextSeason, true, context, nextEnv);
						err = writeRes.getKey();
						if (err!=null) {
							return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(err)).build();
						}
						change.getFiles().addAll(writeRes.getValue());
						//writeInputSchemas(nextSeason); //write inputSchemas of the next season since have min/max version fields						
					}

					if (orgRuntimeEncryption!=newRuntimeEncryption) {
						seasonToUpdate.rewriteRuntimeFileWithUpdatedEncryptionMode(orgRuntimeEncryption, newRuntimeEncryption, context, prod, env);
					}
					Webhooks.get(context).notifyChanges(change, context);
				} catch (IOException e) {
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
				}

				AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
				auditLogWriter.log("Update season: " + season_id + ",   " + updateDetails, userInfo);
				Utilities.sendEmailForSeason(context,userInfo,seasonToUpdate,"updated");
			}

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Updated season: " + seasonToUpdate.toJson(true) + "\n updatd details: " + updateDetails);
			}

			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FIELD_UNIQUE_ID, seasonToUpdate.getUniqueId().toString());
			res.put(Constants.JSON_FIELD_LAST_MODIFIED, seasonToUpdate.getLastModified().getTime());

			return Response.ok(res.toString()).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error updating season: " + season_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error updating season: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	@DELETE
	@Path("/seasons/{season-id}")
	@ApiOperation(value = "Deletes the specified season")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response deleteSeason(@PathParam("season-id")String season_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("deleteSeason request: season_id =" + season_id);
		}
		AirlockChange change = new AirlockChange();
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, season_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);
		
		UserInfo userInfo = UserInfo.validate("ProductServices.deleteSeason", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		String err = Utilities.validateLegalUUID(season_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalSeasonUUID + err);

		ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
		readWriteLock.writeLock().lock();
		try {			

			@SuppressWarnings("unchecked")
			Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);
			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			if (!seasonsDB.containsKey(season_id)) {
				String errMsg = Strings.seasonNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			Season seasonToDel = seasonsDB.get(season_id);
			change.setSeason(seasonToDel);

			if (seasonToDel.containSubItemInProductionStage()) {
				String errMsg = Strings.seasonWithFeatureProd;
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			if (seasonToDel.containStreamsInProductionStage()) {
				String errMsg = Strings.seasonWithStreamProd;
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}
			
			if (seasonToDel.containNotificationsInProductionStage()) {
				String errMsg = Strings.seasonWithNotificationProd;
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			UUID productID = seasonToDel.getProductId();
			Product prod = productsDB.get(productID.toString());

			//if last season and contain branch that is in use in experiment - prevent from deleting
			if (prod.getSeasons().size() == 1 && seasonToDel.containBranchInUseByExperiment(context)) {
				String errMsg = Strings.seasonWithExpProd;
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			//if last season and not the only season in the prod - set the maxVersion of the prev season to null
			Season prevSeason = null;
			if (seasonToDel.getMaxVersion() == null && prod.getSeasons().size() > 1) {
				prevSeason = prod.getSeasons().get(prod.getSeasons().size()-2);
				prevSeason.setMaxVersion(null);
			}


			seasonToDel.removeSeasonAssetsFromDBs(context, userInfo);

			
			try {
				//the other seasons that are participate in the same experiments as the deleted season will be written as well is this season 
				//reports to analytics
				if (prod != null) {
					prod.removeSeason(seasonToDel, context);
				}
				
				//removing season files from S3
				change.getFiles().addAll(removeSeasonFiles(seasonToDel));

				//writing updated products list to S3
				change.getFiles().addAll(AirlockFilesWriter.writeProducts(productsDB, context, prod));

				Map<String, BaseAirlockItem> airlockItemsDB = Utilities.getAirlockItemsDB(Constants.MASTER_BRANCH_NAME, context);
				
				//writing previous season if its maxVesrion was updated to null
				if (prevSeason !=null) {
					Environment env = new Environment();
					env.setAirlockItemsDB(airlockItemsDB);
					env.setServerVersion(prevSeason.getServerVersion());
					env.setBranchId(Constants.MASTER_BRANCH_NAME);
					
					change.getFiles().addAll(AirlockFilesWriter.writeFeatures(prevSeason, true, context, env).getValue());
				}
				
				Webhooks.get(context).notifyChanges(change, context);

			} catch (Exception e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}

			AuditLogWriter auditLogWriter = (AuditLogWriter) context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Delete season: uniqueId:" + season_id, userInfo);

			Utilities.sendEmailForSeason(context, userInfo, seasonToDel, "deleted");

			logger.info("Season " + season_id + " was deleted");

			return (Response.ok()).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error deleting season " + season_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error deleting season: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	@GET
	@Path ("/seasons/{season-id}/version")
	@ApiOperation(value = "Returns the server version of the server that created the specified season", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getServerVersion(@PathParam("season-id")String season_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {				
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getServerVersion request");
		}

		String err = Utilities.validateLegalUUID(season_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalSeasonUUID + err);

		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, season_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		
		UserInfo userInfo = UserInfo.validate("ProductServices.getServerVersion", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification		
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.FEATURES});
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

			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, season_id);
			res.put(Constants.JSON_FIELD_SERVER_VERSION, season.getServerVersion());

			return (Response.ok()).entity(res.toString()).build();
		} finally {
			readWriteLock.readLock().unlock();		
		}
	}

	@GET
	@Path ("/seasons/{season-id}/encryptionkey")
	@ApiOperation(value = "Returns the encryption key of the specified season", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getEncryptionKey(@PathParam("season-id")String season_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {				
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getServerVersion request");
		}

		String err = Utilities.validateLegalUUID(season_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalSeasonUUID + err);

		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, season_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		
		UserInfo userInfo = UserInfo.validate("ProductServices.getEncryptionKey", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification		
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.RUNTIME_ENCRYPTION});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();				
		
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.readLock().lock();
		try {

			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);			

			Season season = seasonsDB.get(season_id);

			if (season == null) 
				return logReply(Status.NOT_FOUND, Strings.seasonNotFound);				

			if (!season.getRuntimeEncryption()) 
				return logReply(Status.BAD_REQUEST, Strings.seasonNotConfiguredForEncryption);
			
			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion()); 			
			ValidationResults validationRes = validateEncryptionSupport(env);
			if (validationRes!=null) 
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(validationRes.error)).build();	

			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, season_id);
			res.put(Constants.JSON_FIELD_ENCRYPTION_KEY, season.getEncryptionKey());

			return (Response.ok()).entity(res.toString()).build();
		} finally {
			readWriteLock.readLock().unlock();		
		}
	}

	@PUT
	@Path ("/seasons/{season-id}/encryptionkey")
	@ApiOperation(value = "Reset the encryption key of the specified season", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response resetEncryptionKey(@PathParam("season-id")String season_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {				
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getServerVersion request");
		}
		AirlockChange change = new AirlockChange();
		
		String err = Utilities.validateLegalUUID(season_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalSeasonUUID + err);

		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, season_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);
		
		UserInfo userInfo = UserInfo.validate("ProductServices.resetEncryptionKey", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification		
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.RUNTIME_ENCRYPTION});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();				
		
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.readLock().lock();
		try {

			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);			

			Season season = seasonsDB.get(season_id);
			change.setSeason(season);
			
			if (season == null) 
				return logReply(Status.NOT_FOUND, Strings.seasonNotFound);				
			
			if (!season.getRuntimeEncryption()) 
				return logReply(Status.BAD_REQUEST, Strings.seasonNotConfiguredForEncryption);
			
			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion()); 			
			ValidationResults validationRes = validateEncryptionSupport(env);
			if (validationRes!=null) 
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(validationRes.error)).build();	

			String prevEncryptionKey = season.getEncryptionKey();
			season.resetEncryptionKey();
			
			try {
				change.getFiles().addAll(AirlockFilesWriter.writeAllRuntimeFilesWithNewEncryption(season, context, prevEncryptionKey, season.getEncryptionKey()));
				change.getFiles().addAll(AirlockFilesWriter.writeEncryptionKey(season, context));
				Webhooks.get(context).notifyChanges(change, context);
			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}
			
			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, season_id);
			res.put(Constants.JSON_FIELD_ENCRYPTION_KEY, season.getEncryptionKey());

			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Reset encryption key of season: " + season.getUniqueId().toString() + " from: " + prevEncryptionKey + " to: " + season.getEncryptionKey()); 

			return (Response.ok()).entity(res.toString()).build();
		} finally {
			readWriteLock.readLock().unlock();		
		}
	}

	public static ValidationResults validateEncryptionSupport (Environment env) {
		if (isEncryptionSupported(env))  //only post 2.1 seasons support analytics
			return null;

		String errMsg = Strings.encryptionNotSupported;
		logger.severe(errMsg);
		return new ValidationResults(errMsg, Status.BAD_REQUEST);
	}

	@GET
	@Path ("/seasons/{season-id}/branches/{branch-id}/features")
	@ApiOperation(value = "Returns the feature list for the specified season ", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getFeatures(@PathParam("season-id")String season_id,
			@PathParam("branch-id")String branch_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {		
		//long startTime = System.currentTimeMillis();

		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getFeatures request");
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
		UserInfo userInfo = UserInfo.validate("ProductServices.getFeatures", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification
		AirlockCapability[] requiredCapabilities = getRequiredFeaturesCapabilities(branch_id); //FEATURES + maybe BRANCHES
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, requiredCapabilities);
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

			//verify branchId
			ValidationResults validationRes = Utilities.validateBranchId(context, branch_id, season);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}			

			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, season_id);
		
			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion()); 
			env.setBranchId(branch_id);

			try {
				JSONObject rootJSON = doGetFeatures(season, OutputJSONMode.DISPLAY, context, env, userInfo);
				res.put(Constants.JSON_FIELD_ROOT, rootJSON);
			} catch (MergeException e) {
				err = Strings.mergeException  +e.getMessage();
				logger.severe(err);
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(err)).build();
			}

			return (Response.ok()).entity(res.toString()).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error getting features of season: " + season_id + " branch " + branch_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error getting features: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
			//long stopTime = System.currentTimeMillis();
			//long elapsedTime = stopTime - startTime;
			//System.out.println("^^^^^^^^^^^^ in finally, ProductServices.getFeatures time = " + elapsedTime);
		}
	}	


	private JSONObject doGetFeatures(Season season, OutputJSONMode outputMode, ServletContext context, Environment env, UserInfo userInfo) throws MergeException, JSONException {
		String branchId = env.getBranchId();
		if (branchId.equals(Constants.MASTER_BRANCH_NAME)) {
			RootItem root = season.getRoot();	
			env.setAnalytics(season.getAnalytics());

			@SuppressWarnings("unchecked")
			Map<String, BaseAirlockItem> airlockItemsDB = (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);

			env.setAirlockItemsDB(airlockItemsDB);
			return root.toJson(outputMode, context, env, userInfo);
		}
		else { //branch
			//at this stage i know the branch exists - after validate
			@SuppressWarnings("unchecked")
			Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);

			Branch branch = branchesDB.get(branchId);
			
			BaseAirlockItem branchPurchasesRoot = MergeBranch.merge(season.getEntitlementsRoot(), branch, Constants.REQUEST_ITEM_TYPE.ENTITLEMENTS); 
			Map<String, BaseAirlockItem> airlockPurchasesItemsDB = MergeBranch.getItemMap(branchPurchasesRoot, true);
			
			BaseAirlockItem branchRoot = MergeBranch.merge(season.getRoot(), branch, Constants.REQUEST_ITEM_TYPE.FEATURES); 
			Map<String, BaseAirlockItem> airlockFeaturesItemsDB = MergeBranch.getItemMap(branchRoot, true);
			
			Map<String, BaseAirlockItem> uniteAirlockItemsDB = new HashMap<String, BaseAirlockItem>();
			uniteAirlockItemsDB.putAll(airlockPurchasesItemsDB);
			uniteAirlockItemsDB.putAll(airlockFeaturesItemsDB);
			
			env.setAnalytics(Utilities.getAirlockAnalytics(season, env.getBranchId(), context, uniteAirlockItemsDB, env));
			env.setAirlockItemsDB(uniteAirlockItemsDB);
			
			return branchRoot.toJson(outputMode, context, env, userInfo);
		}		
	}

	@GET
	@Path ("/seasons/{season-id}/inputschema")
	@ApiOperation(value = "Returns the input schema for the specified season ", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getInputSchema(@PathParam("season-id")String season_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getInputSchema request");
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
		UserInfo userInfo = UserInfo.validate("ProductServices.getInputSchema", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);		

		//capability verification		
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.FEATURES});
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

			JSONObject res = season.getInputSchema().toJson();

			return (Response.ok()).entity(res.toString()).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error getting input schema of season: " + season_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error getting input schema: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	@GET
	@Path ("/seasons/{season-id}/utilitiesinfo")
	@ApiOperation(value = "Returns the utilities names and parameters for the specified season, type and stage.", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getUtilitiesInfo(@PathParam("season-id")String season_id,
			@ApiParam(value="DEVELOPMENT or PRODUCTION")@QueryParam("stage")String stage,
			@ApiParam(value="MAIN_UTILITY or STREAMS_UTILITY")@QueryParam("type")String type,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {		
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getUtilitiesInfo request: season_id = " + season_id + ", stage = " + stage);
		}

		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, season_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		
		//check user authorization		
		UserInfo userInfo = UserInfo.validate("ProductServices.getUtilitiesInfo", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification		
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.FEATURES});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();
		
		String err = Utilities.validateLegalUUID(season_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalSeasonUUID + err);

		if (stage==null) {
			String errMsg = Strings.stageMissing;
			logger.severe(errMsg);						
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
		}

		Stage stageObj = Utilities.strToStage(stage);

		if (stageObj==null) {
			String errMsg =  String.format(Strings.illegalStage,stage) + Constants.Stage.returnValues();
			logger.severe(errMsg);						
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
		}

		UtilityType typeObj = null;
		if (type==null) {
			typeObj = UtilityType.MAIN_UTILITY;
		}
		else {		
			typeObj = Utilities.strToUtilityType(type);

			if (typeObj==null) {
				String errMsg =  String.format(Strings.illegalTypeLegalValuesAre, type) + Constants.UtilityType.returnValues();
				logger.severe(errMsg);						
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}
		}
		
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

			JSONObject res = season.getUtilities().getUtilitiesInfo(stageObj, typeObj);
			return (Response.ok()).entity(res.toString()).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error getting utilities info of season: " + season_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error getting utilities info: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	@GET
	@Path ("/seasons/{season-id}/utilities")
	@ApiOperation(value = "Returns the utilities for the specified season, stage and minAppVersion", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getUtilities(@PathParam("season-id")String season_id, 
			@ApiParam(value="DEVELOPMENT or PRODUCTION")@QueryParam("stage")String stage, 
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getUtilities request");
		}

		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, season_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		
		//check user authorization		
		UserInfo userInfo = UserInfo.validate("ProductServices.getUtilities", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		String err = Utilities.validateLegalUUID(season_id);
		if (err!=null) 	
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalSeasonUUID + err);

		//capability verification		
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.FEATURES});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();
		
		Stage stageObj=null;
		if (stage!=null) {
			stageObj = Utilities.strToStage(stage);
			if (stageObj==null) {
				String errMsg =  String.format(Strings.illegalStage,stage) + Constants.Stage.returnValues();
				logger.severe(errMsg);						
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}
		}
		
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

			JSONObject res = season.getUtilities().toJson(stageObj);

			return (Response.ok()).entity(res.toString()).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error getting utilities of season: " + season_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error getting utilities: " + e.getMessage());

		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	@GET
	@Path ("/seasons/{season-id}/streams")
	@ApiOperation(value = "Returns the streams for the specified season", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getStreams(@PathParam("season-id")String season_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getStreams request");
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
		UserInfo userInfo = UserInfo.validate("ProductServices.getStreams", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);		
		
		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.STREAMS});
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

			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());

			ValidationResults validationRes = validateStreamsSupport(env);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			JSONObject res = season.getStreams().toJson(OutputJSONMode.ADMIN);

			return (Response.ok()).entity(res.toString()).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error getting streams of season: " + season_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error getting streams: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	@GET
	@Path ("/seasons/{season-id}/constants")
	@ApiOperation(value = "Returns the constants java file content for the specified season ", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getConstants(@PathParam("season-id")String season_id, 
			@ApiParam(value="iOS, Android or c_sharp")@QueryParam("platform") String platform,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getConstants request");
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
		UserInfo userInfo = UserInfo.validate("ProductServices.getConstants", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		if (platform==null) {
			String errMsg = Strings.plateformMissing;
			logger.severe(errMsg);
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();				
		}		

		Platform p = Utilities.strToPlatform(platform);
		if (p==null) {
			String errMsg = Strings.platformNotFound + platform;
			logger.severe(errMsg);
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
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

			Season season = seasonsDB.get(season_id);

			if (season == null) {
				String errMsg = Strings.seasonNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());
			
			if (p.equals(Platform.c_sharp) && !isCSharpConstantsSupported(env)) {
				return sendAndLogError(Status.BAD_REQUEST, Strings.cSharpConstantsNotSupported);
			}
			
			String res = null;;
			try {
				res = readConstantsFile (season, p);
			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}


			return (Response.ok()).entity(res).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error getting constants of season: " + season_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error getting constants: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}	

	private String readConstantsFile(Season season, Platform p) throws IOException {
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		String separator = ds.getSeparator();
		String fileName = "";
		if (p.equals(Platform.Android)) {
			fileName = Constants.SEASONS_FOLDER_NAME+separator+season.getProductId().toString()+separator+season.getUniqueId().toString()+separator+Constants.AIRLOCK_JAVA_FILE_NAME;
		} 
		else if (p.equals(Platform.iOS)) {
			//iOs 
			fileName = Constants.SEASONS_FOLDER_NAME+separator+season.getProductId().toString()+separator+season.getUniqueId().toString()+separator+Constants.AIRLOCK_SWIFT_FILE_NAME;
		}
		else if (p.equals(Platform.c_sharp)) {
			//C# 
			fileName = Constants.SEASONS_FOLDER_NAME+separator+season.getProductId().toString()+separator+season.getUniqueId().toString()+separator+Constants.AIRLOCK_C_SHARP_FILE_NAME;
		}
		
		try {
			return ds.readDataToString(fileName);
		} catch (IOException ioe) {
			//failed reading 3 times to s3. Set server state to ERROR.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			String error =  String.format(Strings.failedReadingFile,"the constants") + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			throw new IOException(error);	
		} 
	}

	@GET
	@Path ("/seasons/{season-id}/defaults")
	@ApiOperation(value = "Returns the defaults for the specified season ", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getDefaults(@PathParam("season-id")String season_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getDefaults request");
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
		UserInfo userInfo = UserInfo.validate("ProductServices.getDefaults", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);		

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.FEATURES});
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

			JSONObject res = null;			
			try {
				res = readDefaultsFile (season);
			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}


			return (Response.ok()).entity(res.toString()).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error getting defaults of season: " + season_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error getting defaults: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}	


	@GET
	@Path ("/seasons/{season-id}/documentlinks")
	@ApiOperation(value = "Returns the document links for the specified season ", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getDocumentLinks(@PathParam("season-id")String season_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getDocumentLinks request");
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
		UserInfo userInfo = UserInfo.validate("ProductServices.getDocumentLinks", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification		
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.FEATURES});
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

			DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
			String separator = ds.getSeparator();
			//String runtimeFullPath = (String)context.getAttribute(Constants.RUNTIME_PUBLIC_FULL_PATH_PARAM_NAME);
			String storagePublicPath = (String)context.getAttribute(Constants.STORAGE_PUBLIC_PATH_PARAM_NAME);



			String seasonFolderPath =  storagePublicPath + ds.getPathPrefix() + Constants.SEASONS_FOLDER_NAME + separator + season.getProductId().toString() + separator + season_id + separator;

			String defaultfFileLinks =  seasonFolderPath + Constants.AIRLOCK_DEFAULTS_FILE_NAME;
			JSONObject res = new JSONObject();

			res.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, season_id);
			res.put(Constants.JSON_FIELD_DEFAULTS_FILE, defaultfFileLinks);

			JSONArray platforms = new JSONArray();

			//Android
			JSONObject andriodPlatform = new JSONObject();
			andriodPlatform.put(Constants.JSON_FIELD_PLATFORM, Platform.Android.toString());
			JSONArray androidLinks = new JSONArray();
			JSONObject androidConstsLink = new JSONObject();
			androidConstsLink.put(Constants.JSON_FIELD_LINK, seasonFolderPath + Constants.AIRLOCK_JAVA_FILE_NAME);
			androidConstsLink.put(Constants.JSON_FIELD_DISPLAY_NAME, "Constants for Java/Android");
			androidLinks.add(androidConstsLink);
			andriodPlatform.put(Constants.JSON_FIELD_LINKS, androidLinks);
			platforms.add(andriodPlatform);

			//iOS
			JSONObject iosPlatform = new JSONObject();
			iosPlatform.put(Constants.JSON_FIELD_PLATFORM, Platform.iOS.toString());
			JSONArray iosLinks = new JSONArray();
			JSONObject iosConstsLink = new JSONObject();
			iosConstsLink.put(Constants.JSON_FIELD_LINK, seasonFolderPath + Constants.AIRLOCK_SWIFT_FILE_NAME);
			iosConstsLink.put(Constants.JSON_FIELD_DISPLAY_NAME, "Constants for iOS");
			iosLinks.add(iosConstsLink);
			iosPlatform.put(Constants.JSON_FIELD_LINKS, iosLinks);
			platforms.add(iosPlatform);

			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());
			
			if (ProductServices.isCSharpConstantsSupported(env)) {
				//C#
				JSONObject cSharpPlatform = new JSONObject();
				cSharpPlatform.put(Constants.JSON_FIELD_PLATFORM, Platform.c_sharp.toString());
				JSONArray cSharpLinks = new JSONArray();
				JSONObject cSharpConstsLink = new JSONObject();
				cSharpConstsLink.put(Constants.JSON_FIELD_LINK, seasonFolderPath + Constants.AIRLOCK_C_SHARP_FILE_NAME);
				cSharpConstsLink.put(Constants.JSON_FIELD_DISPLAY_NAME, "Constants for C#");
				cSharpLinks.add(cSharpConstsLink);
				cSharpPlatform.put(Constants.JSON_FIELD_LINKS, cSharpLinks);
				platforms.add(cSharpPlatform);
			}
			res.put(Constants.JSON_FIELD_PLATFORMS, platforms);

			return (Response.ok()).entity(res.toString()).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error getting document links of season: " + season_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error getting documents links: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}	

	private JSONObject readDefaultsFile(Season season) throws IOException, JSONException  {
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		String separator = ds.getSeparator();
		String fileName = Constants.SEASONS_FOLDER_NAME+separator+season.getProductId().toString()+separator+season.getUniqueId().toString()+separator+Constants.AIRLOCK_DEFAULTS_FILE_NAME;

		try {
			return ds.readDataToJSON(fileName);
		} catch (IOException ioe) {
			//failed reading 3 times to s3. Set server state to ERROR.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			String error = String.format(Strings.failedReadingFile,"the defaults") + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			throw new IOException(error);	
		} catch (JSONException e) {
			//JSON Format of defaults file on S3 is wrong. write it again from memory and return it
			try {
				AirlockFilesWriter.doWriteDefaultsFile(season, context, Stage.PRODUCTION);
				return ds.readDataToJSON(fileName);
			} catch (IOException ioe) {
				//failed writing 3 times to s3. Set server state to ERROR.
				//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
				String error = String.format(Strings.failedReadingFile,"the defaults") + ioe.getMessage();
				logger.severe(error);
				logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
				throw new IOException(error);	
			} catch (JSONException jse) {
				//should never happen
				throw jse;
			}			
		}
	}


	@GET
	@Path ("/seasons/branches/{branch-id}/features/{feature-id}")
	@ApiOperation(value = "Returns the specified feature", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Feature not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getFeature(@PathParam("feature-id")String feature_id,
			@PathParam("branch-id")String branch_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion,
			@QueryParam("includeStrings")Boolean includeStrings) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getFeature request");
		}

		String err = Utilities.validateLegalUUID(feature_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalFeatureUUID + err);
		
		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductForBranchOrFeature(context, branch_id, feature_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		
		//check user authorization		
		UserInfo userInfo = UserInfo.validate("ProductServices.getFeature", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);		

		if(includeStrings == null){
			includeStrings = false;
		}

		//capability verification
		AirlockCapability[] requiredCapabilities = getRequiredFeaturesCapabilities(branch_id); //FEATURES + maybe BRANCHES		
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, requiredCapabilities);
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();
		
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.readLock().lock();
		try {			

			//verify branchId
			ValidationResults validationRes = Utilities.validateBranchId(context, branch_id, null);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			Map<String, BaseAirlockItem> airlockItemsDB = Utilities.getAirlockItemsDB (branch_id, context);

			BaseAirlockItem f = airlockItemsDB.get(feature_id);

			if (f == null) {
				String errMsg = Strings.AirlockItemNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			Season season = seasonsDB.get(f.getSeasonId().toString());
			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());
			env.setBranchId(branch_id);

			if (env.isInMaster()) {
				env.setAnalytics(season.getAnalytics());				
			}
			else { //branch					
				env.setAnalytics(Utilities.getAirlockAnalytics(season, env.getBranchId(), context, airlockItemsDB, env));				
			}		
			
			env.setAirlockItemsDB(airlockItemsDB);

			JSONObject featureJson = f.toJson(OutputJSONMode.DISPLAY, context, env,userInfo);
			if(includeStrings && f instanceof DataAirlockItem){
				JSONArray stringArray = new JSONArray();
				List<OriginalString> copiedStrings = FeatureServices.getStringInUseByAirlockItem(context, f,false);
				for (int i = 0; i<copiedStrings.size();++i){
					OriginalString currString = copiedStrings.get(i);
					stringArray.add(currString.toJson(Constants.StringsOutputMode.INCLUDE_TRANSLATIONS,seasonsDB.get(currString.getSeasonId().toString())));
				}
				featureJson.put("strings",stringArray);
			}
			return Response.ok(featureJson.toString()).build();
		} catch (MergeException e) {
			err = Strings.mergeException  + e.getMessage();
			logger.severe(err);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(err)).build();		
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error getting feature: " + feature_id + " from branch " + branch_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error getting feature: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}


	private AirlockCapability[] getRequiredFeaturesCapabilities(String branch_id) {
		if (branch_id.equals(Constants.MASTER_BRANCH_NAME)) {
			return new AirlockCapability[]{AirlockCapability.FEATURES};
		}
		else {
			return new AirlockCapability[]{AirlockCapability.FEATURES, AirlockCapability.BRANCHES};
		}
	}
	
	@GET
	@Path ("/seasons/utilities/{utility-id}")
	@ApiOperation(value = "Returns the specified utility", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Utility not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getUtility(@PathParam("utility-id")String utility_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getUtility request");
		}

		String err = Utilities.validateLegalUUID(utility_id);
		if (err!=null) {
			String errMsg = Strings.illegalUtilityUUID + err;
			logger.severe(errMsg);
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();				
		}
		
		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfUtility(context, utility_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		
		//check user authorization		
		UserInfo userInfo = UserInfo.validate("ProductServices.getUtility", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);		
	
		//capability verification		
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.FEATURES});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();
		
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.readLock().lock();
		try {			

			@SuppressWarnings("unchecked")
			Map<String, AirlockUtility> utilitiesDB = (Map<String, AirlockUtility>)context.getAttribute(Constants.UTILITIES_DB_PARAM_NAME);

			AirlockUtility is = utilitiesDB.get(utility_id);

			if (is == null) {
				String errMsg = Strings.utilityNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}
			return Response.ok(is.toJson().toString()).build();					
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error getting utility of season: " + utility_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error getting utility: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	@GET
	@Path ("/seasons/streams/{stream-id}")
	@ApiOperation(value = "Returns the specified stream", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Stream not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getStream(@PathParam("stream-id")String stream_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getStream request");
		}

		String err = Utilities.validateLegalUUID(stream_id);
		if (err!=null) {
			String errMsg = Strings.illegalStreamUUID + err;
			logger.severe(errMsg);
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();				
		}

		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfStream(context, stream_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		
		UserInfo userInfo = UserInfo.validate("ProductServices.getStream", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);
		
		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.STREAMS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();	
		
		
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.readLock().lock();
		try {			

			@SuppressWarnings("unchecked")
			Map<String, AirlockStream> streamsDB = (Map<String, AirlockStream>)context.getAttribute(Constants.STREAMS_DB_PARAM_NAME);

			AirlockStream stream = streamsDB.get(stream_id);

			if (stream == null) {
				String errMsg = Strings.streamNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);			

			Season season = seasonsDB.get(stream.getSeasonId().toString());

			if (season == null) {
				String errMsg = Strings.seasonNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();				
			}

			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());

			ValidationResults validationRes = validateStreamsSupport(env);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			return Response.ok(stream.toJson(OutputJSONMode.ADMIN).toString()).build();					
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error gettingstream: " + stream_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error getting stream: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}


	@POST
	@Path ("/seasons/{season-id}/utilities")
	@ApiOperation(value = "Creates an utility or streams utility within the specified season", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response addUtility(@PathParam("season-id")String season_id, String newUtility,
			@ApiParam(value="DEVELOPMENT or PRODUCTION")@QueryParam("stage") String stage, 
			@QueryParam("force")Boolean force,
			@ApiParam(value="MAIN_UTILITY or STREAMS_UTILITY")@QueryParam("type")String type,
			@QueryParam("name")String name,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("addUtility request: season_id = " + season_id + ", stage = " + stage + ", newUtility = " + newUtility);
		}

		String err = Utilities.validateLegalUUID(season_id);
		if (err!=null) 	
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalSeasonUUID + err);
			
		AirlockChange change = new AirlockChange();
		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, season_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);
		
		//check user authorization		
		UserInfo userInfo = UserInfo.validate("ProductServices.addUtility", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		if (stage == null || stage.isEmpty()) {
			String errMsg = Strings.stageMissing;
			logger.severe(errMsg);
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();			
		}	

		//capability verification		
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.FEATURES});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();
		
		//missing type means type=MAIN_UTILITY		
		if (type == null) {
			type = UtilityType.MAIN_UTILITY.toString();
		}

		if (name == null || name.isEmpty()) {
			String errMsg = Strings.nameMissing;
			logger.severe(errMsg);
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();			
		}				

		if (force == null)
			force = false; //force means update the input schema without rule/config/utility validations

		if (force == true && !validAdmin(userInfo)) {
			String errMsg = Strings.utilityNoValidationError;
			logger.severe(errMsg);
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();			
		}

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();		
		readWriteLock.writeLock().lock();
		try {	
			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			@SuppressWarnings("unchecked")
			Map<String, AirlockUtility> utilitiesDB = (Map<String, AirlockUtility>)context.getAttribute(Constants.UTILITIES_DB_PARAM_NAME);

			//Validate season existence
			Season season = seasonsDB.get(season_id);
			change.setSeason(season);
			
			if (season == null) {
				String errMsg = Strings.seasonNotFound;
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();				
			}			


			//build utility json
			JSONObject newUtilityJSON = new JSONObject();
			newUtilityJSON.put(Constants.JSON_FEATURE_FIELD_STAGE, stage);
			newUtilityJSON.put(Constants.JSON_FIELD_UTILITY, newUtility);
			newUtilityJSON.put(Constants.JSON_FEATURE_FIELD_TYPE, type);
			newUtilityJSON.put(Constants.JSON_FIELD_NAME, name);

			//verify that JSON does not contain uniqueId field
			if (newUtilityJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && newUtilityJSON.get(Constants.JSON_FIELD_UNIQUE_ID) !=null) {
				String errMsg = Strings.utilityWithId;
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();							
			}

			//verify that JSON does not contain different season-id then the path parameter
			if (newUtilityJSON.containsKey(Constants.JSON_FEATURE_FIELD_SEASON_ID) && newUtilityJSON.get(Constants.JSON_FEATURE_FIELD_SEASON_ID) !=null) {
				if (!season_id.equals(newUtilityJSON.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID))) {
					String errMsg = Strings.utilitySeasonWithDifferentId;
					logger.severe(errMsg);
					return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();									
				}
			}
			else {		
				newUtilityJSON.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, season_id);
			}

			AirlockUtility newUtilityObj = new AirlockUtility();

			ValidationResults validationRes = newUtilityObj.validateUtilityJSON(newUtilityJSON, context, userInfo, force);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			newUtilityObj.fromJSON(newUtilityJSON);
			newUtilityObj.setUniqueId(UUID.randomUUID());

			season.getUtilities().addAirlockUtility(newUtilityObj);

			try {
				change.getFiles().addAll(AirlockFilesWriter.writeSeasonUtilities(season, newUtilityObj.getStage() == Stage.PRODUCTION, context, newUtilityObj.getType()));
				change.getFiles().addAll(AirlockFilesWriter.doWriteDefaultsFile(season, context, newUtilityObj.getStage()));
				Webhooks.get(context).notifyChanges(change, context);
			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}

			utilitiesDB.put(newUtilityObj.getUniqueId().toString(), newUtilityObj);

			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Create new utilitiy: " + newUtilityObj.toJson().toString(), userInfo); 

			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FIELD_UNIQUE_ID, newUtilityObj.getUniqueId().toString());
			res.put(Constants.JSON_FIELD_LAST_MODIFIED, newUtilityObj.getLastModified().getTime());

			logger.info("Utility added to season '"+  season_id + "': "+ res.toString());

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Added utility: " + newUtilityObj.toJson());
			}

			return (Response.ok(res.toString())).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error adding utility: ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error adding utility: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	@POST
	@Path ("/seasons/{season-id}/branches/{branch-id}/features")
	@ApiOperation(value = "Creates a feature within the specified season", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season or parent feature not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response addFeature(@PathParam("season-id")String season_id,
			@PathParam("branch-id")String branch_id,
			@QueryParam("parent") String parent, String newFeature,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("addFeature request: season_id = " + season_id + ", parent = " + parent + ", branch_id = " + branch_id + ", newFeature = " + newFeature);
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
		//check user authorization		
		UserInfo userInfo = UserInfo.validate("ProductServices.addFeature", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);						

		//capability verification
		AirlockCapability[] requiredCapabilities = getRequiredFeaturesCapabilities(branch_id); //FEATURES + maybe BRANCHES
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, requiredCapabilities);
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		
		
		if (parent==null) {
			String errMsg = Strings.parentMissing;
			logger.severe(errMsg);						
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
		}
		
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.writeLock().lock();
		try {				

			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			@SuppressWarnings("unchecked")
			Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);

			@SuppressWarnings("unchecked")
			Map<String,InternalUserGroups> groupsPerProductMap = (Map<String,InternalUserGroups>) context.getAttribute(Constants.USER_GROUPS_PER_PRODUCT_PARAM_NAME);
			InternalUserGroups userGroups = groupsPerProductMap.get(currentProduct.getUniqueId().toString());

			//String minimalProfile = (String)context.getAttribute(Constants.MINIMAL_USER_PROFILE);

			//Validate season existence
			Season season = seasonsDB.get(season_id);
			change.setSeason(season);
			if (season == null) {
				return sendAndLogError(Status.NOT_FOUND, Strings.seasonNotFound);				
			}

			//verify branchId
			ValidationResults validationRes = Utilities.validateBranchId(context, branch_id, season);
			if (validationRes!=null) {
				return sendAndLogError(validationRes.status, validationRes.error);
			}
			boolean inMaster = branch_id.equals(Constants.MASTER_BRANCH_NAME);

			Map<String, BaseAirlockItem> airlockItemsDB = Utilities.getAirlockItemsDB (branch_id, context);			

			if (parent.equals(Constants.ROOT_FEATURE)) {
				parent = season.getRoot().getUniqueId().toString(); //the season's root node id
			}
			else {
				//Validate parent existence + in the given season 
				err = Utilities.validateLegalUUID(parent);
				if (err!=null) {
					return sendAndLogError(Status.BAD_REQUEST, Strings.illegalParentUUID + err);
				}

				BaseAirlockItem parentFeature = airlockItemsDB.get(parent);
				if (parentFeature == null) {
					return sendAndLogError(Status.NOT_FOUND, Strings.parentNotFound);
				}

				if (!season_id.equals(parentFeature.getSeasonId().toString())) {
					return sendAndLogError(Status.BAD_REQUEST, Strings.parentNotFoundInSeason);
				}	
				
				//validate that parent is a node of the features tree
				if (!parentFeature.inFeaturesTree(season, airlockItemsDB)) {
					return sendAndLogError(Status.BAD_REQUEST, Strings.parentInPurchasesTree);
				}
			}

			//validate that is a legal JSON
			JSONObject newFeatureJSON = null;
			try {
				newFeatureJSON = new JSONObject(newFeature);  
			} catch (JSONException je) {
				return sendAndLogError(Status.BAD_REQUEST, Strings.illegalInputJSON + je.getMessage());				
			}

			//verify that JSON does not contain uniqueId field
			if (newFeatureJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && newFeatureJSON.get(Constants.JSON_FIELD_UNIQUE_ID) !=null) {
				return sendAndLogError(Status.BAD_REQUEST, Strings.featureWithId);							
			}

			//verify that JSON does not contain different season-id then the path parameter
			if (newFeatureJSON.containsKey(Constants.JSON_FEATURE_FIELD_SEASON_ID) && newFeatureJSON.get(Constants.JSON_FEATURE_FIELD_SEASON_ID) !=null) {
				if (!season_id.equals(newFeatureJSON.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID))) {
					return sendAndLogError(Status.BAD_REQUEST, Strings.featureSeasonWithDifferentId);									
				}
			}
			else {		
				newFeatureJSON.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, season_id);
			}

			//2 validations that are done for baseAirlockItem as well but should be performed here - before all other validations			
			validationRes = preliminaryFeatureJsonValidation(newFeatureJSON);
			if (validationRes!=null) {
				return sendAndLogError(validationRes.status, validationRes.error);
			}

			BaseAirlockItem newAirlockObj = BaseAirlockItem.getAirlockItemByType(newFeatureJSON);
			if (newAirlockObj == null) {
				return sendAndLogError(Status.BAD_REQUEST, Strings.typeNotFound);
			}

			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());
			env.setBranchId(branch_id);
			env.setAirlockItemsDB(airlockItemsDB);

			validationRes = newAirlockObj.validateFeatureJSON(newFeatureJSON, context, season_id, userGroups, parent, userInfo, airlockItemsDB, env);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			newAirlockObj.fromJSON(newFeatureJSON, null, UUID.fromString(parent), env);
			newAirlockObj.setUniqueId(UUID.randomUUID());

			BaseAirlockItem parentFeature = airlockItemsDB.get(newAirlockObj.getParent().toString());
			if (inMaster) {
				err = parentFeature.addAirlockItem(newAirlockObj);
				if (err!=null) {
					logger.severe(err);
					return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(err)).build();
				}
				parentFeature.setLastModified(new Date());

				airlockItemsDB.put(newAirlockObj.getUniqueId().toString(), newAirlockObj);
				if (newAirlockObj instanceof DataAirlockItem) {
					sendMailForAdd(context, airlockItemsDB,parentFeature, (DataAirlockItem) newAirlockObj,userInfo,env);
				}
			}
			else {
				//add feature to branch.				
				Branch branch = branchesDB.get(branch_id);
				change.setBranch(branch);
				err = branch.addFeature (newAirlockObj, parentFeature);
				if (err!=null) {
					logger.severe(err);
					return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(err)).build();
				}

			}

			//when adding new ordering rule if its parent is reported to analytics - analytics counters may change
			if (newAirlockObj instanceof OrderingRuleItem) {
				season.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection().calcNumberOfItemsToAnalytics(context, season.getUniqueId(), airlockItemsDB);
			}
			
			//writing updated features list to S3
			try {
				if(!inMaster) {
					ValidationResults res = Utilities.validateBranchStructure(branch_id, season, context);
					if (res!=null) {
						String errMsg = res.error;
						logger.severe(errMsg);
						return Response.status(res.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
					}					
				}


				boolean writeProductionFeatures = false;
				Stage changeStage = Stage.DEVELOPMENT;
				if ((newAirlockObj instanceof FeatureItem || newAirlockObj instanceof ConfigurationRuleItem) && BaseAirlockItem.isProductionFeature(newAirlockObj, airlockItemsDB)) {
					writeProductionFeatures = true;
					changeStage = Stage.PRODUCTION;
				}

				if (inMaster) {
					Pair<String,LinkedList<AirlockChangeContent>> writeRes = AirlockFilesWriter.writeFeatures (season, writeProductionFeatures, context, env); 
					err = writeRes.getKey();
					change.getFiles().addAll(writeRes.getValue());
				} 
				else {
					Branch branch = branchesDB.get(branch_id);
					change.setBranch(branch);
					change.getFiles().addAll(AirlockFilesWriter.writeBranchFeatures(branch, season, context, env, changeStage));										
					change.getFiles().addAll(AirlockFilesWriter.writeBranchAndMasterRuntimeFiles(season, branch, context, env, writeProductionFeatures));
					change.getFiles().addAll(AirlockFilesWriter.doWriteConstantsFiles(season, context, changeStage));
				}
				Webhooks.get(context).notifyChanges(change, context);
				
				if (err!=null) {
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(err)).build();					
				}

			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}

			BaseAirlockItem parentObj = airlockItemsDB.get(parent);
			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Create new feature: " + newAirlockObj.getNameSpaceDotName() + ", " + newAirlockObj.getUniqueId().toString() + " in branch :"+ branch_id + ": parent = " + parentObj.getNameSpaceDotName() + ", " + parent + ": "+ newAirlockObj.toJson(OutputJSONMode.DISPLAY, context, env).toString(), userInfo); 

			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FIELD_UNIQUE_ID, newAirlockObj.getUniqueId().toString());
			res.put(Constants.JSON_FIELD_LAST_MODIFIED, newAirlockObj.getLastModified().getTime());

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Added feature: " + newAirlockObj.toJson(OutputJSONMode.ADMIN, context, env));
			}

			return (Response.ok(res.toString())).build();
		} catch (MergeException e) {
			err = Strings.mergeException  + e.getMessage();
			logger.severe(err);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(err)).build();		
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error adding item: ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error adding item: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}	

	//A few validations that are done for baseAirlockItem as well but should be performed here - before all other validations
	private static ValidationResults preliminaryFeatureJsonValidation(JSONObject newFeatureJSON) {
		//sub features are not allowed in add feature (adding only one by one)
		try {
			if (!newFeatureJSON.containsKey(Constants.JSON_FEATURE_FIELD_TYPE) || newFeatureJSON.get(Constants.JSON_FEATURE_FIELD_TYPE)==null) {
				return new ValidationResults(Strings.typeMissing, Status.BAD_REQUEST);
			}
			String typeStr = newFeatureJSON.getString(Constants.JSON_FEATURE_FIELD_TYPE);
			Type typeObj = Utilities.valueOf(BaseAirlockItem.Type.class, typeStr);
			if (typeObj == null) {
				return new ValidationResults("Illegal type: '" + typeStr + "'", Status.BAD_REQUEST);
			}
			
			//validate that only 'feature' types are passed (not purchase items)
			if (BaseAirlockItem.isOnlyPurchaseItem(typeObj)) {
				return new ValidationResults(String.format(Strings.typeNotSupportedInFeaturesFunction, typeStr), Status.BAD_REQUEST);
			}
			
			if (newFeatureJSON.containsKey(Constants.JSON_FEATURE_FIELD_FEATURES) && newFeatureJSON.get(Constants.JSON_FEATURE_FIELD_FEATURES) != null && !newFeatureJSON.getJSONArray(Constants.JSON_FEATURE_FIELD_FEATURES).isEmpty()) {
				return new ValidationResults(Strings.featureWithSubfeatures, Status.BAD_REQUEST);
			}

			//sub configuration rules are not allowed in add feature (adding only one by one)
			if (newFeatureJSON.containsKey(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES) && newFeatureJSON.get(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES) != null && !newFeatureJSON.getJSONArray(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES).isEmpty()) {
				return new ValidationResults(Strings.featureWithConfigurations, Status.BAD_REQUEST);
			}
			
			//sub ordering rules are not allowed in add feature (adding only one by one)
			if (newFeatureJSON.containsKey(Constants.JSON_FEATURE_FIELD_ORDERING_RULES) && newFeatureJSON.get(Constants.JSON_FEATURE_FIELD_ORDERING_RULES) != null && !newFeatureJSON.getJSONArray(Constants.JSON_FEATURE_FIELD_ORDERING_RULES).isEmpty()) {
				return new ValidationResults(Strings.featureWithOrderingRules, Status.BAD_REQUEST);
			}
			
			//sub purchaseOptions are not allowed in add feature 
			if (newFeatureJSON.containsKey(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS) && newFeatureJSON.get(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS) != null && !newFeatureJSON.getJSONArray(Constants.JSON_FEATURE_FIELD_ORDERING_RULES).isEmpty()) {
				return new ValidationResults(Strings.featureWithPurchaseOptions, Status.BAD_REQUEST);
			}
			
			//sub entitlemen are not allowed in add feature 
			if (newFeatureJSON.containsKey(Constants.JSON_FEATURE_FIELD_PURCHASE_OPTIONS) && newFeatureJSON.get(Constants.JSON_FEATURE_FIELD_PURCHASE_OPTIONS) != null && !newFeatureJSON.getJSONArray(Constants.JSON_FEATURE_FIELD_ORDERING_RULES).isEmpty()) {
				return new ValidationResults(Strings.featureWithInAppPurchases, Status.BAD_REQUEST);
			}
		} catch (JSONException je) {
			return new ValidationResults(je.getMessage(), Status.BAD_REQUEST);
		}

		return null;
	}

	@DELETE
	@Path ("/seasons/branches/{branch-id}/features/{feature-id}")
	@ApiOperation(value = "Deletes the specified feature")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Feature not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response deleteFeature(@PathParam("feature-id")String feature_id,
			@PathParam("branch-id")String branch_id,
			@ApiParam(value="VALIDATE or ACT")@QueryParam("mode")String mode,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("ProductServices.deleteFeature request: feature_id =" + feature_id + ", branch_id = " + branch_id + ", mode = " + mode);
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
		UserInfo userInfo = UserInfo.validate("ProductServices.deleteFeature", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//if not specified - mode = ACT (actually delete the feature item)
		ActionType actionTypeObj  = Utilities.strToActionType(mode, ActionType.ACT);
		
		if (actionTypeObj==null) {
			return sendAndLogError(Status.BAD_REQUEST, String.format(Strings.illegalMode,mode)  + Constants.ActionType.returnValues());
		}

		//capability verification
		AirlockCapability[] requiredCapabilities = getRequiredFeaturesCapabilities(branch_id); //FEATURES + maybe BRANCHES		
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, requiredCapabilities);
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();
		
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		
		readWriteLock.writeLock().lock();
		try {		
			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);
			@SuppressWarnings("unchecked")
			Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);


			//verify branchId
			ValidationResults validationRes = Utilities.validateBranchId(context, branch_id, null);
			if (validationRes!=null) 
				return sendAndLogError(validationRes.status, validationRes.error);
				
			Map<String, BaseAirlockItem> airlockItemsDB = Utilities.getAirlockItemsDB (branch_id, context);			

			if (!airlockItemsDB.containsKey(feature_id)) 
				return sendAndLogError(Status.NOT_FOUND, Strings.AirlockItemNotFound);

			BaseAirlockItem featureToDel = airlockItemsDB.get(feature_id);
			if (featureToDel.getType() == Type.ROOT) 
				return sendAndLogError(Status.BAD_REQUEST, Strings.deleteFeatureRoot);				

			if (featureToDel.containSubItemInProductionStage()) {
				logger.severe(String.format(Strings.featureWithSubfeatureProdWithId, feature_id));
				return sendAndLogError(Status.BAD_REQUEST, Strings.featureWithSubfeatureProd);
			}

			Season season = seasonsDB.get(featureToDel.getSeasonId().toString()); 	
			if (season == null) 
				sendAndLogError(Status.BAD_REQUEST, String.format(Strings.nonExistingSeason,featureToDel.getSeasonId().toString()));				

			change.setSeason(season);

			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());
			env.setBranchId(branch_id);			
			env.setAirlockItemsDB(airlockItemsDB);
			String orderingRule = featureToDel.appearsInOrderingRule(season, context, env, false);
			if (orderingRule!=null) 
				return sendAndLogError(Status.BAD_REQUEST, String.format(Strings.featureAppearsInOrderingRule, orderingRule));			
			
			
			boolean inMaster = branch_id.equals(Constants.MASTER_BRANCH_NAME);
			try {
				if (actionTypeObj == ActionType.VALIDATE) {
					JSONObject res = new JSONObject();
					String deletedFeatureInUseByAnalytics = null;
					if (inMaster) {
						deletedFeatureInUseByAnalytics = season.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection().deletedFeatureInUseByAnalytics(featureToDel, airlockItemsDB, context, season,userInfo, env);
					}
					else {
						Branch branch = branchesDB.get(branch_id);						
						deletedFeatureInUseByAnalytics =  branch.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection().deletedFeatureInUseByAnalytics(featureToDel, airlockItemsDB, context, season,userInfo, env);
					}

					if (deletedFeatureInUseByAnalytics!=null) {
						res.put (Constants.JSON_FIELD_WARNING, deletedFeatureInUseByAnalytics);					
					}

					return (Response.ok(res.toString())).build();				
				}

				TreeSet<String> changedBranches = new TreeSet<String>();				
				//TreeSet<String> changedBranches = null;
				AirlockAnalytics analytics = null;
				if (inMaster) {
					
					//map between branch and its merged items db			
					Map<String, Map<String, BaseAirlockItem>> branchAirlockItemsDBsMap = new HashMap<String, Map<String, BaseAirlockItem>>();
					
					//if this feature is checked out in a branch - duplicate it in branch and set it in BranchStatus.NEW
					Utilities.duplicateDeletedItemWhenCheckedOutInBranches(featureToDel, season, context, changedBranches, branchAirlockItemsDBsMap, Constants.REQUEST_ITEM_TYPE.FEATURES);

					//If the deleted feature is subFeature of some feature in branch - delete it from its branchFeatureItems/branchConfigItems/branchOrderingRulesItems lists
					Utilities.removeDeletedfromBranchSubItemLists(featureToDel, season, changedBranches);					
					
					if (featureToDel.getParent() == null) {
						BaseAirlockItem root = season.getRoot();
						root.removeAirlockItem(featureToDel.getUniqueId());
					}
					else {
						BaseAirlockItem parent = airlockItemsDB.get(featureToDel.getParent().toString());
						parent.removeAirlockItem(featureToDel.getUniqueId());
						parent.setLastModified(new Date());
					}
					
					analytics = season.getAnalytics();
				} else {
					Branch branch = branchesDB.get(branch_id);
					change.setBranch(branch);
					err = branch.deleteFeature(featureToDel, REQUEST_ITEM_TYPE.FEATURES);
					if (err!=null) 
						return sendAndLogError(Status.BAD_REQUEST, err);

					analytics = branch.getAnalytics();
				}

				Stage featureInAnalyticsChangeStage = Stage.DEVELOPMENT;
				try {
					//remove the feature and all of its sub features from analytics  
					featureInAnalyticsChangeStage = analytics.getGlobalDataCollection().getAnalyticsDataCollection().removeDeletedFeatureFromAnalytics(featureToDel, context, season, airlockItemsDB);
				} catch (Exception e) {
					err = Strings.failUpdateAnalytics + e.getMessage();
					logger.severe(err);
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(err)).build();
				}

				//writing updated features list to S3
				try {
					if(!inMaster) {
						ValidationResults res = Utilities.validateBranchStructure(branch_id, season, context);
						if (res!=null) 
							return sendAndLogError(res.status, res.error);	
					}
					else {
						if (changedBranches!=null && changedBranches.size()>0) {
							//when deleting feature from master - that is checked out in some branches ... 							
							for (String branchId:changedBranches) {
								ValidationResults res = Utilities.validateBranchStructure(branchId, season, context);
								if (res!=null) 
									return sendAndLogError(res.status, res.error);	
							}
						}
					}

					boolean writeProductionFeatures = false;
					if (((featureToDel instanceof FeatureItem || featureToDel instanceof ConfigurationRuleItem) && ((DataAirlockItem)featureToDel).getStage() == Stage.PRODUCTION) ||
							(featureInAnalyticsChangeStage!=null && featureInAnalyticsChangeStage == Stage.PRODUCTION))
						writeProductionFeatures = true; //should not happen - cannot delete feature in production
					Stage changeStage = writeProductionFeatures? Stage.PRODUCTION : Stage.DEVELOPMENT;
					
					if (inMaster) {
						Pair<String,LinkedList<AirlockChangeContent>> writeRes = AirlockFilesWriter.writeFeatures (season, writeProductionFeatures, context, env); 
						err = writeRes.getKey();
						if (err!=null) {
							return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(err)).build();
						}
						change.getFiles().addAll(writeRes.getValue());

						if (changedBranches!=null && changedBranches.size()>0) {
							//when deleting feature from master - that is checked out in some branches ... 
							change.getFiles().addAll(AirlockFilesWriter.writeSeasonBranches(season, context, env, changeStage));
							for (String branchId:changedBranches) {
								Branch branch = branchesDB.get(branchId);
								change.setBranch(branch);
								change.getFiles().addAll(AirlockFilesWriter.writeBranchFeatures(branch, season, context, env, changeStage));												
								change.getFiles().addAll(AirlockFilesWriter.writeBranchRuntime(branch, season, context, env, writeProductionFeatures));
								change.getFiles().addAll(AirlockFilesWriter.writeAnalytics(season, context, branch.getUniqueId().toString(), env, changeStage)); //writing analytics since if this feature was in analytics - the id was updated								
							}
						}

						//remove deleted feature and sub features from features map
						featureToDel.removeFromAirlockItemsDB(airlockItemsDB, context, userInfo);

						if (featureInAnalyticsChangeStage!=null) {
							season.getAnalytics().getGlobalDataCollection().setLastModified(new Date());
							change.getFiles().addAll(AirlockFilesWriter.writeAnalytics(season, context, Constants.MASTER_BRANCH_NAME,env, changeStage));
						}
					} 
					else {
						Branch branch = branchesDB.get(branch_id);
						change.setBranch(branch);
						change.getFiles().addAll(AirlockFilesWriter.writeSeasonBranches(season, context, env, changeStage));						
						change.getFiles().addAll(AirlockFilesWriter.writeBranchAndMasterRuntimeFiles(season, branch, context, env, writeProductionFeatures));
						change.getFiles().addAll(AirlockFilesWriter.doWriteConstantsFiles(season, context, changeStage));
						if (featureInAnalyticsChangeStage!=null) {
							branch.getAnalytics().getGlobalDataCollection().setLastModified(new Date());
							change.getFiles().addAll(AirlockFilesWriter.writeAnalytics(season, context, branch_id, env, changeStage));
						}							
					}

					if (featureInAnalyticsChangeStage!=null) {
						changedBranches.add(branch_id);							
						
						//write all the runtime files of the other seasons that are participating in an experiment that its analytics was changed
						change.getFiles().addAll(updateOtherSeasonsRuntimeFiles(changedBranches, season, branchesDB, writeProductionFeatures, context));
					}
					Webhooks.get(context).notifyChanges(change, context);

				} catch (IOException e) {
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
				}
			} catch (JSONException je){
				return sendAndLogError(Status.BAD_REQUEST, je.getMessage());	
			}

			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Delete feature: " + featureToDel.getNameSpaceDotName() + ", " + featureToDel.getUniqueId().toString() + " from branch :" + branch_id, userInfo); 

			logger.info("Feature " + feature_id + " was deleted");

			return (Response.ok()).build();
		} catch (MergeException e) {
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, Strings.mergeException  + e.getMessage());		
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error deleting item " + feature_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error deleting item" + feature_id + ": " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}


	@DELETE
	@Path ("/seasons/utilities/{utility-id}")
	@ApiOperation(value = "Deletes the specified utility or streams utility")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Utility not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response deleteUtility(@PathParam("utility-id")String utility_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException  {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("deleteUtility request: utility_id =" + utility_id);
		}
		AirlockChange change = new AirlockChange();
		String err = Utilities.validateLegalUUID(utility_id);
		if (err!=null) 
			return logReply(Status.BAD_REQUEST, Strings.illegalUtilityUUID + err);		

		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfUtility(context, utility_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);
		
		UserInfo userInfo = UserInfo.validate("ProductServices.deleteUtility", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);
		
		//capability verification		
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.FEATURES});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();
		
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.writeLock().lock();
		try {		

			@SuppressWarnings("unchecked")
			Map<String, AirlockUtility> utilitiesDB = (Map<String, AirlockUtility>)context.getAttribute(Constants.UTILITIES_DB_PARAM_NAME);
			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			if (!utilitiesDB.containsKey(utility_id)) {
				String errMsg = Strings.utilityNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			AirlockUtility utilToDel = utilitiesDB.get(utility_id);			

			Season season = seasonsDB.get(utilToDel.getSeasonId().toString()); 
			change.setSeason(season);
			if (season == null) {
				String errMsg = String.format(Strings.nonExistingSeason,utilToDel.getSeasonId().toString());
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();				
			}

			if (utilToDel.getStage() == Stage.PRODUCTION) {
				String errMsg = Strings.utilityProd;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			String errorString = season.getUtilities().removeAirlockUtility(utilToDel, season, context);
			if (errorString!=null) {
				logger.severe(errorString);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errorString)).build();
			}

			utilitiesDB.remove(utility_id);

			//writing updated utilities list to S3
			try {
				change.getFiles().addAll(AirlockFilesWriter.writeSeasonUtilities(season, utilToDel.getStage() == Stage.PRODUCTION, context, utilToDel.getType()));
				if (utilToDel.getType().equals(UtilityType.MAIN_UTILITY))
					change.getFiles().addAll(AirlockFilesWriter.doWriteDefaultsFile(season, context, utilToDel.getStage()));
				Webhooks.get(context).notifyChanges(change, context);
			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}

			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Delete utility: uniqueId: " + utility_id, userInfo); 

			logger.info("Utility " + utility_id + " was deleted");

			return (Response.ok()).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error deleting utility " + utility_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error deleting utility: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}	

	@DELETE
	@Path ("/seasons/streams/{stream-id}")
	@ApiOperation(value = "Deletes the specified stream")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Stream not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response deleteStream(@PathParam("stream-id")String stream_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException  {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("deleteStream request: stream_id =" + stream_id);
		}
		AirlockChange change = new AirlockChange();
		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfStream(context, stream_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);
		
		//check user authorization		
		UserInfo userInfo = UserInfo.validate("ProductServices.deleteStream", context, assertion,currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		String err = Utilities.validateLegalUUID(stream_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalStreamUUID + err);
		
		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.STREAMS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();	
				
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();		
		readWriteLock.writeLock().lock();
		try {		

			@SuppressWarnings("unchecked")
			Map<String, AirlockStream> streamsDB = (Map<String, AirlockStream>)context.getAttribute(Constants.STREAMS_DB_PARAM_NAME);
			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);
			@SuppressWarnings("unchecked")
			Map<String, BaseAirlockItem> airlockItemsDB = (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);
			

			if (!streamsDB.containsKey(stream_id)) {
				String errMsg = Strings.streamNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			AirlockStream streamToDel = streamsDB.get(stream_id);			

			Season season = seasonsDB.get(streamToDel.getSeasonId().toString()); 
			change.setSeason(season);
			
			if (season == null) {
				String errMsg = String.format(Strings.nonExistingSeason,streamToDel.getSeasonId().toString());
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();				
			}

			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());
			env.setAirlockItemsDB(airlockItemsDB);
			
			ValidationResults validationRes = validateStreamsSupport(env);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			if (streamToDel.getStage() == Stage.PRODUCTION) {
				String errMsg = Strings.streamProd;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			String errorString = season.getStreams().removeAirlockStream(streamToDel, season, context);
			if (errorString!=null) {
				logger.severe(errorString);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errorString)).build();
			}

			streamsDB.remove(stream_id);

			//remove input fields from analytics if were removed from schema during update			
			LinkedList<String> updatedBranches = new LinkedList<String>();
			Stage analyticsUpdateStage = season.getInputSchema().updateAnalytics (season, context, updatedBranches);


			try {
				//writing updated streams list to S3
				change.getFiles().addAll(AirlockFilesWriter.writeSeasonStreams(season, streamToDel.getStage() == Stage.PRODUCTION, context));

				//writing updated runtime+analytics files if some of the stream's fields were reported to analytics
				if (analyticsUpdateStage!=null) {
					@SuppressWarnings("unchecked")
					Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);

					boolean masterRuntimeFilesWereWritten = false;
					boolean masterRuntimeFilesShouldBeWritten = false;
					for (String updatedBranch:updatedBranches) {
						//fields were removed - analytics and runtime files should be written
						if (updatedBranch.equals(Constants.MASTER_BRANCH_NAME)) {
							change.getFiles().addAll(AirlockFilesWriter.writeAnalytics(season, context, Constants.MASTER_BRANCH_NAME, env, streamToDel.getStage()));	
							season.getAnalytics().getGlobalDataCollection().setLastModified(new Date());

							change.getFiles().addAll(AirlockFilesWriter.doWriteFeatures (season, OutputJSONMode.RUNTIME_DEVELOPMENT, context, Stage.DEVELOPMENT, env)); //write the dev runtime files

							if (analyticsUpdateStage.equals(Stage.PRODUCTION)) {
								change.getFiles().addAll(AirlockFilesWriter.doWriteFeatures (season, OutputJSONMode.RUNTIME_PRODUCTION, context, Stage.PRODUCTION, env)); //write the prod runtime files
							}
							masterRuntimeFilesWereWritten = true;
						}
						else {								
							change.getFiles().addAll(AirlockFilesWriter.writeAnalytics(season, context, updatedBranch, env, streamToDel.getStage()));
							Branch branch = branchesDB.get(updatedBranch);
							branch.getAnalytics().getGlobalDataCollection().setLastModified(new Date());

							change.getFiles().addAll(AirlockFilesWriter.writeBranchRuntime(branch, season, context, env, analyticsUpdateStage.equals(Stage.PRODUCTION)));
							if (branch.isPartOfExperiment(context)!=null)
								masterRuntimeFilesShouldBeWritten=true;									
						}							
					}
					if (masterRuntimeFilesShouldBeWritten && !masterRuntimeFilesWereWritten) {
						change.getFiles().addAll(AirlockFilesWriter.doWriteFeatures (season, OutputJSONMode.RUNTIME_DEVELOPMENT, context, Stage.DEVELOPMENT, env)); //write the dev runtime files

						if (analyticsUpdateStage.equals(Stage.PRODUCTION)) {
							change.getFiles().addAll(AirlockFilesWriter.doWriteFeatures (season, OutputJSONMode.RUNTIME_PRODUCTION, context,Stage.PRODUCTION, env)); //write the prod runtime files
						}
					}
					Webhooks.get(context).notifyChanges(change, context);
				}
			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}

			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Delete stream: uniqueId: " + stream_id, userInfo); 

			logger.info("Stream " + streamToDel.getName() + ", " + stream_id + " was deleted");

			return (Response.ok()).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error deleting stream " + stream_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error deleting stream: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}	

	@PUT
	@Path ("/seasons/branches/{branch-id}/features/{feature-id}")
	@ApiOperation(value = "Updates the specified feature", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Feature not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response updateFeature(@PathParam("feature-id")String feature_id,
			@PathParam("branch-id")String branch_id,
			String feature,
			@ApiParam(value="VALIDATE or ACT")@QueryParam("mode")String mode,			
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("updateFeature request: feature_id =" + feature_id + ", branch_id = " + branch_id + ", mode = " + mode +", feature = " + feature);
		}
		AirlockChange change = new AirlockChange();
		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductForBranchOrFeature(context, branch_id, feature_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);
		
		//check user authorization		
		UserInfo userInfo = UserInfo.validate("ProductServices.updateFeature", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);
		
		//Temporary for profiling
		logger.info("updateFeature request: feature_id =" + feature_id + ", branch_id = " + branch_id + ", mode = " + mode +", feature = " + feature);

		String err = Utilities.validateLegalUUID(feature_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalFeatureUUID + err);

		//if not specified - mode = ACT (actually update the feature item)
		ActionType actionTypeObj  = Utilities.strToActionType(mode, ActionType.ACT);
		
		if (actionTypeObj==null) {
			return sendAndLogError(Status.BAD_REQUEST, String.format(Strings.illegalMode,mode)  + Constants.ActionType.returnValues());
		}
				
		long validated, written, total;
		validated = written = total = 0;
		long start = new Date().getTime(); // XXX TEMPORARY

		//capability verification
		AirlockCapability[] requiredCapabilities = getRequiredFeaturesCapabilities(branch_id); //FEATURES + maybe BRANCHES		
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, requiredCapabilities);
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();
		
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();			
		readWriteLock.writeLock().lock();
		try {				
			@SuppressWarnings("unchecked")			
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);
			@SuppressWarnings("unchecked")
			Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);

			@SuppressWarnings("unchecked")
			Map<String,InternalUserGroups> groupsPerProductMap = (Map<String,InternalUserGroups>) context.getAttribute(Constants.USER_GROUPS_PER_PRODUCT_PARAM_NAME);
			InternalUserGroups userGroups = groupsPerProductMap.get(currentProduct.getUniqueId().toString());

			//verify branchId
			ValidationResults validationRes = Utilities.validateBranchId(context, branch_id, null);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			Map<String, BaseAirlockItem> airlockItemsDB = Utilities.getAirlockItemsDB(branch_id, context);

			BaseAirlockItem featureToUpdate = airlockItemsDB.get(feature_id);
			if (featureToUpdate == null) {
				String errMsg = Strings.AirlockItemNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();			
			}			

			JSONObject updatedFeatureJSON = null;
			try {
				updatedFeatureJSON = new JSONObject(feature);
			} catch (JSONException je) {
				String errMsg = Strings.illegalInputJSON + je.getMessage();
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			//if not set - set the uniqueId to be the id path param
			if (!updatedFeatureJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) || updatedFeatureJSON.get(Constants.JSON_FIELD_UNIQUE_ID) == null) {
				updatedFeatureJSON.put(Constants.JSON_FIELD_UNIQUE_ID, feature_id);
			}
			else {
				//verify that feature-id in path is identical to uniqueId in request pay-load  
				if (!updatedFeatureJSON.getString(Constants.JSON_FIELD_UNIQUE_ID).equals(feature_id)) {
					String errMsg = Strings.featureWithDifferentId;
					logger.severe(errMsg);
					return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();					
				}
			}

			Season season = seasonsDB.get(featureToUpdate.getSeasonId().toString()); 		
			if (season == null) {
				String errMsg = String.format(Strings.nonExistingSeason,featureToUpdate.getSeasonId().toString());
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();				
			}
			change.setSeason(season);

			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());
			env.setBranchId(branch_id);		

			validationRes = featureToUpdate.validateFeatureJSON(updatedFeatureJSON, context, featureToUpdate.getSeasonId().toString(), userGroups, null, userInfo, airlockItemsDB, env);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			boolean inMaster = branch_id.equals(Constants.MASTER_BRANCH_NAME);						
			Branch curBranch = null;
			if (!inMaster) {
				curBranch = branchesDB.get(branch_id);
			}
			change.setBranch(curBranch);

			//If feature is root or mutual exclusion group, verify that one of its sub-features is not in production and 
			//is changed if you are not permitted (i.e you are not admin or productLead).
			//consider prod under dev as prod
			ValidationResults validateProdDontChangeRes = featureToUpdate.validateProductionDontChanged(updatedFeatureJSON, airlockItemsDB, curBranch, context, false, env, false);
			Boolean isProdChange = (validateProdDontChangeRes != null);
			//only productLead or Administrator can update feature in production
			if (!validRole(userInfo)) {
				//the status is ok on standAlone branch when moving from dev to prod
				if (validateProdDontChangeRes!=null && !validateProdDontChangeRes.status.equals(Status.OK)) {
					String errMsg = validateProdDontChangeRes.error;
					logger.severe(errMsg);
					return Response.status(validateProdDontChangeRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
				}					
			} 
			AirlockAnalytics mergedAnalytics = Utilities.getAirlockAnalytics(season, branch_id, context, airlockItemsDB, env);
			env.setAnalytics(mergedAnalytics);
			env.setAirlockItemsDB(airlockItemsDB);
			boolean isAnalyticsChanged = featureToUpdate.isAnalyticsChanged(updatedFeatureJSON, season, context, env);

			if (actionTypeObj == ActionType.VALIDATE) {
				GlobalDataCollection dataColl = null;
				if (env.isInMaster()) {
					dataColl = season.getAnalytics().getGlobalDataCollection();
				}
				else {
					dataColl = curBranch.getAnalytics().getGlobalDataCollection();
				}
				JSONObject res = new JSONObject();

				String deletedFeatureInUseByAnalytics = dataColl.getAnalyticsDataCollection().updatedFeatureInUseByAnalytics(feature_id, updatedFeatureJSON, airlockItemsDB, context, season, env,isProdChange);
				if (deletedFeatureInUseByAnalytics!=null) {
					res.put (Constants.JSON_FIELD_WARNING, deletedFeatureInUseByAnalytics);					
				}

				return (Response.ok(res.toString())).build();				
			}

			//If feature is root or mutual exclusion group, verify that one of its sub-features is not in production and 
			//is changed if you are not permitted (i.e you are not admin or productLead).
			//consider prod under dev as dev
			ValidationResults validateProdDontChangeForRuntimeFilesRes = featureToUpdate.validateProductionDontChanged(updatedFeatureJSON, airlockItemsDB, curBranch, context, true, env, true);
			Boolean writeProduction = (validateProdDontChangeForRuntimeFilesRes != null);
			
			//Branch curBranch = null;
			if (!inMaster) {
				if (curBranch.getBranchAirlockItemsBD().containsKey(feature_id)) { 
					//in branch: take the feature to update from branch items db since the itemsDb we are using is a clone of the features in 
					//the branch and no the features themselves. 
					featureToUpdate = curBranch.getBranchAirlockItemsBD().get(feature_id);
				}
				env.setAnalytics(mergedAnalytics);
			}
			else {
				env.setAnalytics(season.getAnalytics());
			}
			
			validated = new Date().getTime();
			//finally - actually update the feature.
			//remove deleted configuration attributes (if there are any) from analytics
			Map<String, Stage> updatedBranchesMap = new HashMap<String, Stage> (); //map that keeps the branches that were changed and the stage that need to be written
			List<ChangeDetails> updateDetails = featureToUpdate.updateAirlockItem(updatedFeatureJSON, airlockItemsDB, airlockItemsDB.get(season.getRoot().getUniqueId().toString()), env, curBranch,isProdChange, context, updatedBranchesMap);

			updateDetails = Utilities.sanitizeChangesList(updateDetails);
			if (!updateDetails.isEmpty()) { //if some fields were changed
				try {
					//boolean productionChange = (validateProdDontChangeRes!=null);
					Set<String> updatedBranches = updatedBranchesMap.keySet();
					if (inMaster) {
						Pair<String,LinkedList<AirlockChangeContent>> writeRes = AirlockFilesWriter.writeFeatures (season, writeProduction, context, env); //if prod changed - write runtimeProd file 
						err = writeRes.getKey();
						if (err!=null) {
							return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(err)).build();
						}
						change.getFiles().addAll(writeRes.getValue());

						//go over all branches that were changed (for example by feature name change or if the delta of a checked out feature was changed)

						for (String updatedBranchId:updatedBranches) {
							//go over all updated branch and validate its structure
							Branch branch = branchesDB.get(updatedBranchId);							
							ValidationResults res = Utilities.validateBranchStructure(branch.getUniqueId().toString(), season, context);
							if (res!=null) {
								String errMsg = res.error;
								logger.severe(errMsg);
								return Response.status(res.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
							}												
						}

						for (String updatedBranchId:updatedBranches) {
							Branch branch = branchesDB.get(updatedBranchId);							
							change.getFiles().addAll(AirlockFilesWriter.writeBranchFeatures(branch, season, context, env, updatedBranchesMap.get(updatedBranchId)));																	
							change.getFiles().addAll(AirlockFilesWriter.writeBranchRuntime(branch, season, context, env, updatedBranchesMap.get(updatedBranchId).equals(Stage.PRODUCTION)));
						}

						//if (isProdChange) { //not only on prod change because with ordering rules even moving ordering rule from parent to parent can change analytics counters 
							//in case of production change (including dev to prod and vice versa)
							//recalculate analytics counters
							season.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection().calcNumberOfItemsToAnalytics(context, season.getUniqueId(), airlockItemsDB);												
						//}
					}
					else {

						ValidationResults res = Utilities.validateBranchStructure(branch_id, season, context);
						if (res!=null) {
							String errMsg = res.error;
							logger.severe(errMsg);
							return Response.status(res.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
						}											

						Branch branch = branchesDB.get(branch_id);
						
						if (updatedBranchesMap.containsKey(branch_id)) {
							writeProduction = writeProduction || updatedBranchesMap.get(branch_id).equals(Stage.PRODUCTION);
						}
						
						change.getFiles().addAll(AirlockFilesWriter.writeBranchFeatures(branch, season, context, env,
								(writeProduction? Stage.PRODUCTION : Stage.DEVELOPMENT)));										

						
						change.getFiles().addAll(AirlockFilesWriter.writeBranchAndMasterRuntimeFiles(season, branch, context, env, writeProduction));

						//no need to update branch analytics counters since it is never accessed directly - only after analytics merge
					}

					if (isAnalyticsChanged) {
						HashSet<String> updatedBranchesHS = new HashSet<String>(updatedBranches);
						updatedBranchesHS.add(branch_id);
						updatedBranches = updatedBranchesHS;
						
						//write all the runtime files of the other seasons that are participating in an experiment that its analytics was changed
						change.getFiles().addAll(updateOtherSeasonsRuntimeFiles(updatedBranches, season, branchesDB, writeProduction, context));
					}
					Webhooks.get(context).notifyChanges(change, context);
				} catch (IOException e) {
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
				}
				written = new Date().getTime();
				AuditLogWriter auditLogWriter = (AuditLogWriter) context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
				String featureType = featureToUpdate.getObjTypeStrByType();

				String changesMessage = "Update to "+featureType+": " + featureToUpdate.getNameSpaceDotName() + ", " + featureToUpdate.getUniqueId().toString() + " , branch = " + branch_id + ", produced the following changes:\n";
				StringBuilder listUpdateDetails = new StringBuilder(changesMessage);
				for (int i = 0; i < updateDetails.size(); ++i) {
					listUpdateDetails.append(updateDetails.get(i).changeToString());
				}
				auditLogWriter.log(changesMessage + listUpdateDetails.toString(), userInfo);
				String rootCause = "A "+featureType+" was updated: " + featureToUpdate.getNameSpaceDotName() + ", " + featureToUpdate.getUniqueId().toString();
				Utilities.sendEmails(context, rootCause, featureToUpdate.getUniqueId(), updateDetails, userInfo,airlockItemsDB,env);
			}

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Updated feature: " + featureToUpdate.toJson(OutputJSONMode.DISPLAY, context, env) + ". update details: \n" + updateDetails);
			}

			JSONObject res = new JSONObject();
			env.setAirlockItemsDB(airlockItemsDB);
			if (inMaster) {
				env.setAnalytics(season.getAnalytics());
				res = featureToUpdate.toJson(OutputJSONMode.DISPLAY, context, env,userInfo);				
			} else {
				//after update: the branch tree should be recalculated
				airlockItemsDB = Utilities.getAirlockItemsDB (branch_id, context);			
				BaseAirlockItem f = airlockItemsDB.get(feature_id);
				env.setAnalytics(Utilities.getAirlockAnalytics(season, env.getBranchId(), context, airlockItemsDB, env));
				res = f.toJson(OutputJSONMode.DISPLAY, context, env,userInfo);
			}


			return (Response.ok(res.toString())).build();
		} catch (MergeException e) {
			err = Strings.mergeException  + e.getMessage();
			logger.severe(err);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(err)).build();					
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error updating feature " + feature_id  + " in branch " + branch_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error updating feature: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
			total = new Date().getTime();
			logger.info(String.format("feature update: validation %d ms, writing %d ms, total %d ms", validated - start, written - validated, total - start)); 								
		}
	}

	@PUT
	@Path ("/seasons/{season-id}/inputschema")
	@ApiOperation(value = "Updates the input schema of the specified season.", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response updateInputSchema(@PathParam("season-id")String season_id, String inputSchema, @QueryParam("force")Boolean force,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {

		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("updateInputSchema request: season_id =" + season_id +", input schema = " + inputSchema);
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
		
		//check user authorization		
		UserInfo userInfo = UserInfo.validate("ProductServices.updateInputSchema", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);
		
		//capability verification		
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.FEATURES});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();
		

		if (force == null)
			force = false; //force means update the input schema without rule/config validations

		if (force == true && !validAdmin(userInfo)) {
			String errMsg = Strings.inputSchemaNoValidationError;
			logger.severe(errMsg);
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();			
		}

		long validated, written, total;
		validated = written = total = 0;
		long start = new Date().getTime(); // XXX TEMPORARY
		
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

			JSONObject updatedInputSchemaJSON = null;
			try {
				updatedInputSchemaJSON = new JSONObject(inputSchema);
			} catch (JSONException je) {
				String errMsg = Strings.illegalInputJSON + je.getMessage();
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			//if not set - set the seasonId to be the id path param
			if (!updatedInputSchemaJSON.containsKey(Constants.JSON_FEATURE_FIELD_SEASON_ID) || updatedInputSchemaJSON.get(Constants.JSON_FEATURE_FIELD_SEASON_ID) == null) {
				updatedInputSchemaJSON.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, season_id);
			}
			else {
				//verify that season-id in path is identical to seasonId in request pay-load  
				if (!updatedInputSchemaJSON.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID).equals(season_id)) {
					String errMsg = Strings.inputSchemaSeasonWithDifferentId;
					logger.severe(errMsg);
					return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();					
				}
			}

			InputSchema inputSchemaToUpdate = season.getInputSchema();
			ValidationResults validationRes = inputSchemaToUpdate.validateInputSchemaJSON(updatedInputSchemaJSON, context, force);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			validated = new Date().getTime();
			//finally - actually update the feature.
			String updateDetails = inputSchemaToUpdate.updateInputSchema(updatedInputSchemaJSON, context, season);

			LinkedList<String> updatedBranches = new LinkedList<String>();
			//remove input fields from analytics if were removed from schema during update			
			Stage analyticsUpdateStage = inputSchemaToUpdate.updateAnalytics (season, context, updatedBranches);


			if (!updateDetails.isEmpty()) { //if some fields were changed		
				try {
					change.getFiles().addAll(AirlockFilesWriter.writeInputSchema(season, context, analyticsUpdateStage));	

					Environment env = new Environment();
					env.setServerVersion(season.getServerVersion());
					env.setAirlockItemsDB(airlockItemsDB);
					
					if (analyticsUpdateStage!=null) {
						@SuppressWarnings("unchecked")
						Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);

						boolean masterRuntimeFilesWereWritten = false;
						boolean masterRuntimeFilesShouldBeWritten = false;
						boolean prodMasterRuntimeFilesShouldBeWritten = false;
						for (String updatedBranch:updatedBranches) {
							//fields were removed - analytics and runtime files should be written
							if (updatedBranch.equals(Constants.MASTER_BRANCH_NAME)) {
								AirlockFilesWriter.writeAnalytics(season, context, Constants.MASTER_BRANCH_NAME, env, analyticsUpdateStage);	
								season.getAnalytics().getGlobalDataCollection().setLastModified(new Date());

								change.getFiles().addAll(AirlockFilesWriter.doWriteFeatures (season, OutputJSONMode.RUNTIME_DEVELOPMENT, context, Stage.DEVELOPMENT, env)); //write the dev runtime files

								if (analyticsUpdateStage.equals(Stage.PRODUCTION)) {
									change.getFiles().addAll(AirlockFilesWriter.doWriteFeatures (season, OutputJSONMode.RUNTIME_PRODUCTION, context, Stage.PRODUCTION, env)); //write the prod runtime files
								}
								masterRuntimeFilesWereWritten = true;
							}
							else {								
								AirlockFilesWriter.writeAnalytics(season, context, updatedBranch, env, analyticsUpdateStage);
								Branch branch = branchesDB.get(updatedBranch);
								branch.getAnalytics().getGlobalDataCollection().setLastModified(new Date());

								change.getFiles().addAll(AirlockFilesWriter.writeBranchRuntime(branch, season, context, env, analyticsUpdateStage.equals(Stage.PRODUCTION)));
								if (branch.isPartOfExperiment(context)!=null) {
									masterRuntimeFilesShouldBeWritten=true;
									prodMasterRuntimeFilesShouldBeWritten = (branch.isPartOfExperimentInProduction(context)!=null);
								}
							}							
						}
						if (masterRuntimeFilesShouldBeWritten && !masterRuntimeFilesWereWritten) {
							change.getFiles().addAll(AirlockFilesWriter.doWriteFeatures (season, OutputJSONMode.RUNTIME_DEVELOPMENT, context, Stage.DEVELOPMENT, env)); //write the dev runtime files

							if (analyticsUpdateStage.equals(Stage.PRODUCTION) && prodMasterRuntimeFilesShouldBeWritten) {
								change.getFiles().addAll(AirlockFilesWriter.doWriteFeatures (season, OutputJSONMode.RUNTIME_PRODUCTION, context, Stage.PRODUCTION, env)); //write the prod runtime files
							}
						}
						
						//write all the runtime files of the other seasons that are participating in an experiment that its analytics was changed
						change.getFiles().addAll(updateOtherSeasonsRuntimeFiles(updatedBranches, season, branchesDB, analyticsUpdateStage.equals(Stage.PRODUCTION), context));
						
					}
					Webhooks.get(context).notifyChanges(change, context);

				} catch (IOException e) {
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
				}

				AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
				auditLogWriter.log("Update input schema for season: " + season_id + ",   " + updateDetails, userInfo); 								
			}
			written = new Date().getTime();
			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Updated input schema: " + inputSchemaToUpdate.toJson() + "\n updatd details: " + updateDetails);
			}

			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, season_id);
			res.put(Constants.JSON_FIELD_LAST_MODIFIED, inputSchemaToUpdate.getLastModified().getTime());
			return (Response.ok(res.toString())).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error updating input schema of season: " + season_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error updating input schema: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
			total = new Date().getTime();
			logger.info(String.format("input schema update: validation %d ms, writing %d ms, total %d ms", validated - start, written - validated, total - start)); 								
		}
	}

	public static LinkedList<AirlockChangeContent> updateOtherSeasonsRuntimeFiles(Collection<String> updatedBranches, Season season, Map<String, Branch> branchesDB, boolean isProductionChange, ServletContext context) throws JSONException, IOException, MergeException {
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		Set<String>  updatedSeasonIds = new HashSet<String>();
		@SuppressWarnings("unchecked")
		Map<String, Experiment> experimentsDB = (Map<String, Experiment>)context.getAttribute(Constants.EXPERIMENTS_DB_PARAM_NAME);
		
		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);

		@SuppressWarnings("unchecked")			
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

		for (String updatedBranchId:updatedBranches) {
			Branch updatedBranch = branchesDB.get(updatedBranchId);
			if (updatedBranchId.equals(Constants.MASTER_BRANCH_NAME)) {
				//master is participating in all experiments. If there is an experiment for this season. all seaosn in exp shoudl be written
				List<Experiment> expsForSeason = season.getExperimentsForSeason(context, false);
				for (Experiment exp:expsForSeason) {
					Product prod = productsDB.get(exp.getProductId().toString());
					List<Season> expSeasons = prod.getSeasonsWithinRange(exp.getMinVersion(), exp.getMaxVersion());
					for (Season s:expSeasons) {
						if (s.getUniqueId().equals(season.getUniqueId())) //the seasons runtime fiels were already written
							continue;
						
						updatedSeasonIds.add(s.getUniqueId().toString());					
					}	
				}
			}
			else {
				LinkedList<String> branchExperiments = updatedBranch.getExperimentsOfBranch(context);	
						
				for (String expId:branchExperiments) {
					Experiment exp = experimentsDB.get(expId);
					Product prod = productsDB.get(exp.getProductId().toString());
					List<Season> expSeasons = prod.getSeasonsWithinRange(exp.getMinVersion(), exp.getMaxVersion());
					for (Season s:expSeasons) {
						if (s.getUniqueId().equals(season.getUniqueId())) //the seasons runtime fiels were already written
							continue;
						
						updatedSeasonIds.add(s.getUniqueId().toString());					
					}								
				}
			}
		}
		
		Map<String, BaseAirlockItem> airlockItemsDB = Utilities.getAirlockItemsDB(Constants.MASTER_BRANCH_NAME, context);
		for (String sId:updatedSeasonIds) {
			Season s = seasonsDB.get(sId);
			Environment env = new Environment();
			env.setAirlockItemsDB(airlockItemsDB);
			env.setServerVersion(s.getServerVersion());
			env.setBranchId(Constants.MASTER_BRANCH_NAME);
			
			changesArr.addAll(AirlockFilesWriter.doWriteFeatures(s, OutputJSONMode.RUNTIME_DEVELOPMENT, context, Stage.DEVELOPMENT, env));
			if (isProductionChange)
				changesArr.addAll(AirlockFilesWriter.doWriteFeatures(s, OutputJSONMode.RUNTIME_PRODUCTION, context, Stage.PRODUCTION, env));
		}
		return changesArr;
	}
	
	
	@PUT
	@Path ("/seasons/utilities/{utility-id}")
	@ApiOperation(value = "Updates the specified utility or streams utility", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Utility not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response updateUtility(@PathParam("utility-id")String utility_id, String utility,
			@ApiParam(value="DEVELOPMENT or PRODUCTION")@QueryParam("stage") String stage, 
			@QueryParam("name")String name,
			@QueryParam("lastmodified") Long lastModified, 
			@QueryParam("force")Boolean force,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("updateUtility request: utility_id =" + utility_id + ", stage = " + stage + ", lastmodified = " + lastModified + ", utility = " + utility);
		}

		String err = Utilities.validateLegalUUID(utility_id);
		if (err!=null)
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalUtilityUUID + err);
		AirlockChange change = new AirlockChange();
		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfUtility(context, utility_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);
		
		//check user authorization		
		UserInfo userInfo = UserInfo.validate("ProductServices.updateUtility", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);
			
		//capability verification		
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.FEATURES});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();
		
		if (stage == null || stage.isEmpty()) {
			String errMsg = Strings.stageMissing;
			logger.severe(errMsg);
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();			
		}	

		if (name == null || name.isEmpty()) {
			String errMsg = Strings.nameMissing;
			logger.severe(errMsg);
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();			
		}	
		
		if (lastModified == null) {
			String errMsg = Strings.lastModifiedMissing;
			logger.severe(errMsg);
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();			
		}	

		if (force == null)
			force = false; //force means update the input schema without rule/config/utility validations

		if (force == true && !validAdmin(userInfo)) {
			String errMsg = Strings.utilityUpdateNoValidationError;
			logger.severe(errMsg);
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();			
		}

		logger.info("starting update utility");
		long total, validated, written;
		total = validated = written = 0;
		long start = new Date().getTime(); // XXX TEMPORARY

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.writeLock().lock();
		try {

			@SuppressWarnings("unchecked")
			Map<String, AirlockUtility> utilitiesDB = (Map<String, AirlockUtility>)context.getAttribute(Constants.UTILITIES_DB_PARAM_NAME);

			@SuppressWarnings("unchecked")			
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			AirlockUtility alUtilityToUpdate = utilitiesDB.get(utility_id);
			if (alUtilityToUpdate == null) {
				String errMsg = Strings.utilityNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();			
			}			

			//build utility json
			JSONObject updatedUtilityJSON = new JSONObject();
			updatedUtilityJSON.put(Constants.JSON_FEATURE_FIELD_STAGE, stage);
			updatedUtilityJSON.put(Constants.JSON_FIELD_UTILITY, utility);
			updatedUtilityJSON.put(Constants.JSON_FIELD_UNIQUE_ID, utility_id);
			updatedUtilityJSON.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, alUtilityToUpdate.getSeasonId().toString());
			updatedUtilityJSON.put(Constants.JSON_FEATURE_FIELD_TYPE, alUtilityToUpdate.getType().toString());
			updatedUtilityJSON.put(Constants.JSON_FIELD_LAST_MODIFIED, lastModified);
			updatedUtilityJSON.put(Constants.JSON_FIELD_NAME, name);

			Stage prevStage = alUtilityToUpdate.getStage();

			ValidationResults validationRes = alUtilityToUpdate.validateUtilityJSON(updatedUtilityJSON, context, userInfo, force);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}
			Season season = seasonsDB.get(alUtilityToUpdate.getSeasonId().toString()); //after validate we know the season exists
			change.setSeason(season);
			
			validated = new Date().getTime();
			//finally - actually update the utility.
			String updateDetails = alUtilityToUpdate.updateUtility(updatedUtilityJSON, season);
			Stage changeStage = (prevStage == Stage.PRODUCTION || stage.equals(Stage.PRODUCTION.toString()))? Stage.PRODUCTION : Stage.DEVELOPMENT;
			if (!updateDetails.isEmpty()) { //if some fields were changed		
				try {
					change.getFiles().addAll(AirlockFilesWriter.writeSeasonUtilities(season, prevStage == Stage.PRODUCTION || stage.equals(Stage.PRODUCTION.toString()), context, alUtilityToUpdate.getType()));

					if (alUtilityToUpdate.getType().equals(UtilityType.MAIN_UTILITY)) {  
						change.getFiles().addAll(AirlockFilesWriter.doWriteDefaultsFile(season, context, changeStage));
					}
					Webhooks.get(context).notifyChanges(change, context);
				} catch (IOException e) {
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
				}

				AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
				auditLogWriter.log("Update utility: " + utility_id + ",   " + updateDetails, userInfo); 								
			}
			written = new Date().getTime();
			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Updated utility: " + alUtilityToUpdate.toJson() + "\n updatd details: " + updateDetails);
			}

			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FIELD_UNIQUE_ID, utility_id);
			res.put(Constants.JSON_FIELD_LAST_MODIFIED, alUtilityToUpdate.getLastModified().getTime());
			return (Response.ok(res.toString())).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error updating utility: " + utility_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error updating utility: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
			total = new Date().getTime();
			logger.info(String.format("utility update: validation %d ms, writing %d ms, total %d ms", validated - start, written - validated, total - start)); 								
		}
	}	

	@PUT
	@Path ("/seasons/streams/{stream-id}")
	@ApiOperation(value = "Updates the specified stream", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Stream not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response updateStream(@PathParam("stream-id")String stream_id, String stream,
			@QueryParam("force")Boolean force,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("updateStream request: stream_id =" + stream_id + ", force = " + force +", stream = " + stream);
		}
		AirlockChange change = new AirlockChange();
		String err = Utilities.validateLegalUUID(stream_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalStreamUUID + err);

		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfStream(context, stream_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);
		
		//check user authorization		
		UserInfo userInfo = UserInfo.validate("ProductServices.updateStream", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.STREAMS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();	
										
		if (force == null)
			force = false; //force means update the input schema without rule/config/utility validations

		if (force == true && !validAdmin(userInfo)) {
			String errMsg = Strings.streamUpdateNoValidationError;
			logger.severe(errMsg);
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();			
		}

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.writeLock().lock();
		try {

			@SuppressWarnings("unchecked")
			Map<String, AirlockStream> streamsDB = (Map<String, AirlockStream>)context.getAttribute(Constants.STREAMS_DB_PARAM_NAME);
			@SuppressWarnings("unchecked")			
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);
			@SuppressWarnings("unchecked")
			Map<String, BaseAirlockItem> airlockItemsDB = (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);
			
			AirlockStream alStreamToUpdate = streamsDB.get(stream_id);
			if (alStreamToUpdate == null) {
				String errMsg = Strings.streamNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();			
			}			

			JSONObject updatedStreamJSON = null;
			try {
				updatedStreamJSON = new JSONObject(stream);
			} catch (JSONException je) {
				String errMsg = Strings.illegalInputJSON + je.getMessage();
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}						

			//if not set - set the uniqueId to be the id path param
			if (!updatedStreamJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) || updatedStreamJSON.get(Constants.JSON_FIELD_UNIQUE_ID) == null) {
				updatedStreamJSON.put(Constants.JSON_FIELD_UNIQUE_ID, stream_id);
			}
			else {
				//verify that stream-id in path is identical to uniqueId in request pay-load  
				if (!updatedStreamJSON.getString(Constants.JSON_FIELD_UNIQUE_ID).equals(stream_id)) {
					String errMsg = Strings.streamWithDifferentId;
					logger.severe(errMsg);
					return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();					
				}
			}			

			ValidationResults validationRes = alStreamToUpdate.validateStreamJSON(updatedStreamJSON, context, userInfo, alStreamToUpdate.getSeasonId().toString(), force);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			Season season = seasonsDB.get(alStreamToUpdate.getSeasonId().toString()); //after validate we know the season exists
			change.setSeason(season);

			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());
			env.setAirlockItemsDB(airlockItemsDB);

			validationRes = validateStreamsSupport(env);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			ValidationResults validateProdDontChangeRes = alStreamToUpdate.validateProductionDontChanged(updatedStreamJSON);
			Boolean isProdChange = (validateProdDontChangeRes != null);
			//only productLead or Administrator can update feature in production
			if (!validRole(userInfo)) {
				if (validateProdDontChangeRes!=null && !validateProdDontChangeRes.status.equals(Status.OK)) {
					String errMsg = validateProdDontChangeRes.error;
					logger.severe(errMsg);
					return Response.status(validateProdDontChangeRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
				}					
			}

			Stage prevStage = alStreamToUpdate.getStage();
			String prevResSchema = alStreamToUpdate.getResultsSchema();

			//finally - actually update the stream.
			String updateDetails = alStreamToUpdate.updateStraem(updatedStreamJSON, season);
			Stage changeStage = (prevStage == Stage.DEVELOPMENT && alStreamToUpdate.getStage() == Stage.DEVELOPMENT)?
							Stage.DEVELOPMENT : Stage.PRODUCTION;
			if (!updateDetails.isEmpty()) { //if some fields were changed

				try {
					change.getFiles().addAll(AirlockFilesWriter.writeSeasonStreams(season, isProdChange, context));					


					//if stage or results schema were updated - validate that no input field should be removed from analytics
					if (!prevStage.equals(alStreamToUpdate.getStage()) || !prevResSchema.equals(alStreamToUpdate.getResultsSchema())) {
						//remove input fields from analytics if were removed from schema during update			
						LinkedList<String> updatedBranches = new LinkedList<String>();
						Stage analyticsUpdateStage = season.getInputSchema().updateAnalytics (season, context, updatedBranches);
						//writing updated runtime+analytics files if some of the stream's fields were reported to analytics
						if (analyticsUpdateStage!=null) {
							@SuppressWarnings("unchecked")
							Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);

							boolean masterRuntimeFilesWereWritten = false;
							boolean masterRuntimeFilesShouldBeWritten = false;
							for (String updatedBranch:updatedBranches) {
								//fields were removed - analytics and runtime files should be written
								if (updatedBranch.equals(Constants.MASTER_BRANCH_NAME)) {
									change.getFiles().addAll(AirlockFilesWriter.writeAnalytics(season, context, Constants.MASTER_BRANCH_NAME, env, changeStage));	
									season.getAnalytics().getGlobalDataCollection().setLastModified(new Date());

									change.getFiles().addAll(AirlockFilesWriter.doWriteFeatures (season, OutputJSONMode.RUNTIME_DEVELOPMENT, context, Stage.DEVELOPMENT, env)); //write the dev runtime files

									if (analyticsUpdateStage.equals(Stage.PRODUCTION)) {
										change.getFiles().addAll(AirlockFilesWriter.doWriteFeatures (season, OutputJSONMode.RUNTIME_PRODUCTION, context, Stage.PRODUCTION, env)); //write the prod runtime files
									}
									masterRuntimeFilesWereWritten = true;
								}
								else {								
									change.getFiles().addAll(AirlockFilesWriter.writeAnalytics(season, context, updatedBranch, env, changeStage));
									Branch branch = branchesDB.get(updatedBranch);
									branch.getAnalytics().getGlobalDataCollection().setLastModified(new Date());

									change.getFiles().addAll(AirlockFilesWriter.writeBranchRuntime(branch, season, context, env, analyticsUpdateStage.equals(Stage.PRODUCTION)));
									if (branch.isPartOfExperiment(context)!=null)
										masterRuntimeFilesShouldBeWritten=true;									
								}							
							}
							if (masterRuntimeFilesShouldBeWritten && !masterRuntimeFilesWereWritten) {
								change.getFiles().addAll(AirlockFilesWriter.doWriteFeatures (season, OutputJSONMode.RUNTIME_DEVELOPMENT, context, Stage.DEVELOPMENT, env)); //write the dev runtime files

								if (analyticsUpdateStage.equals(Stage.PRODUCTION)) {
									change.getFiles().addAll(AirlockFilesWriter.doWriteFeatures (season, OutputJSONMode.RUNTIME_PRODUCTION, context, Stage.PRODUCTION, env)); //write the prod runtime files
								}
							}
						}
					}
					Webhooks.get(context).notifyChanges(change, context);
				} catch (IOException e) {
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
				}

				AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
				auditLogWriter.log("Update stream: " + stream_id + ",   " + updateDetails, userInfo); 								
			}

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Updated stream: " + alStreamToUpdate.toJson(OutputJSONMode.ADMIN) + "\n updatd details: " + updateDetails);
			}

			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FIELD_UNIQUE_ID, stream_id);
			res.put(Constants.JSON_FIELD_LAST_MODIFIED, alStreamToUpdate.getLastModified().getTime());
			return (Response.ok(res.toString())).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error updating streams: " + stream_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error updating streams: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}	

	@GET
	@Path ("/seasons/{season-id}/inputsample")
	@ApiOperation(value = "Returns the input sample for the specified season, stage, minAppVersion, generationMode and randomize", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getInputSample(@PathParam("season-id")String season_id, 
			@ApiParam(value="DEVELOPMENT or PRODUCTION")@QueryParam("stage")String stage , 
			@QueryParam("minappversion")String minAppVersion,
			@ApiParam(value="MINIMAL, MAXIMAL or PARTIAL")@QueryParam("generationmode")String generationMode, 
			@ApiParam(value="determines the values that are returned. 0 = fully randomized.")@QueryParam("randomize")Double randomize, 
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getInputSample request: season_id = " + season_id + ", stage = " + stage + ", minappversion = " + minAppVersion + ", generationmode = " + generationMode);
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
		UserInfo userInfo = UserInfo.validate("ProductServices.getInputSample", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification		
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.FEATURES});
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
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);			

			Season season = seasonsDB.get(season_id);

			if (season == null) {
				String errMsg = Strings.seasonNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();				
			}

			JSONObject res = null;
			try {
				res = season.getInputSchema().generateInputSample (stageObj, minAppVersion, context, generationModeObj, randomize);
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

			return (Response.ok()).entity(res.toString()).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error getting input sample of season: " + season_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error getting input sample: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	@PUT
	@Path ("/seasons/upgrade/{season-id}/{from-version}")
	@ApiOperation(value = "Upgrade the specified season from the specified version V2.5 or V2 .", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response upgradeSeason(@PathParam("season-id")String season_id, @PathParam("from-version")String fromVersion,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("upgradeSeason request: season_id =" + season_id +", from version = " + fromVersion);
		}
		AirlockChange change = new AirlockChange();
		
		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, season_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);
		
		UserInfo userInfo = UserInfo.validate("ProductServices.upgradeSeason", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		String err = Utilities.validateLegalUUID(season_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalSeasonUUID + err);

		ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
		readWriteLock.writeLock().lock();
		try {
			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			Season seasonToUpgrade = seasonsDB.get(season_id);
			change.setSeason(seasonToUpgrade);
			if (seasonToUpgrade == null) {
				String errMsg = Strings.seasonNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();				
			}

			try {
				change.getFiles().addAll(doUpgradeSeason (seasonToUpgrade, fromVersion, userInfo));
				Webhooks.get(context).notifyChanges(change, context);
			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();					
			}

			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FIELD_UNIQUE_ID, seasonToUpgrade.getUniqueId().toString());
			res.put(Constants.JSON_FIELD_LAST_MODIFIED, seasonToUpgrade.getLastModified().getTime());
			res.put(Constants.JSON_FIELD_VERSION, fromVersion);

			return Response.ok(res.toString()).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error upgrading season: " + season_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error upgrading season: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}
	
	
	@PUT
	@Path ("/seasons/{season-id}/upgrade/utilities")
	@ApiOperation(value = "Upgrade the specified season utilities.", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response upgradeUtilities(@PathParam("season-id")String season_id, 
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("upgradeUtilities request: season_id =" + season_id);
		}
		AirlockChange change = new AirlockChange();
		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, season_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		
		//check user authorization		
		UserInfo userInfo = UserInfo.validate("ProductServices.upgradeUtilities", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		String err = Utilities.validateLegalUUID(season_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalSeasonUUID + err);

		ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
		readWriteLock.writeLock().lock();
		try {
			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			Season seasonToUpgrade = seasonsDB.get(season_id);
			change.setSeason(seasonToUpgrade);
			if (seasonToUpgrade == null) {
				String errMsg = Strings.seasonNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();				
			}

			try {
				Pair<String, LinkedList<AirlockChangeContent>> writeRes = doUpgradeSeasonUtilities(seasonToUpgrade, context); 
				String upgradeDetails = writeRes.getKey();
				if (upgradeDetails.length()>0) {
					AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
					auditLogWriter.log("Upgrade season utilities for season: " + season_id + " :\n" + upgradeDetails, userInfo); 
				}
				change.getFiles().addAll(writeRes.getValue());
				Webhooks.get(context).notifyChanges(change, context);
			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();					
			}

			
			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FIELD_UNIQUE_ID, seasonToUpgrade.getUniqueId().toString());
			res.put(Constants.JSON_FIELD_LAST_MODIFIED, seasonToUpgrade.getLastModified().getTime());
			
			return Response.ok(res.toString()).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error updating utilities of season: " + season_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error updating utilities: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}
	
	private LinkedList<AirlockChangeContent> doUpgradeSeason(Season seasonToUpgrade, String fromVersion, UserInfo userInfo) throws IOException {
		try {
			return seasonToUpgrade.upgrade(fromVersion, context, userInfo);
		} catch (IOException ioe) {		
			String errMsg = String.format(Strings.failedUpgradingSeason,seasonToUpgrade.getUniqueId().toString()) + ioe.getMessage();
			logger.severe(errMsg);	
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			throw new IOException(errMsg);				
		} catch (JSONException e) {
			String errMsg = String.format(Strings.failedUpgradingSeason,seasonToUpgrade.getUniqueId().toString()) + e.getMessage();
			logger.severe(errMsg);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			throw new IOException(errMsg);
		}		
	}
	
	public static Pair<String,LinkedList<AirlockChangeContent>> doUpgradeSeasonUtilities(Season seasonToUpgrade, ServletContext context) throws IOException {
		try {
			LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
			String res = seasonToUpgrade.getUtilities().addNames(seasonToUpgrade);

			if (res.length()>0) {
				changesArr.addAll(AirlockFilesWriter.writeSeasonUtilities(seasonToUpgrade, false, context, UtilityType.MAIN_UTILITY));
				changesArr.addAll(AirlockFilesWriter.writeSeasonUtilities(seasonToUpgrade, false, context, UtilityType.STREAMS_UTILITY));
			}
			return new Pair<String, LinkedList<AirlockChangeContent>>(res, changesArr);
		} catch (IOException ioe) {		
			String errMsg = String.format(Strings.failedUpgradingSeasonUtilities,seasonToUpgrade.getUniqueId().toString()) + ioe.getMessage();
			logger.severe(errMsg);	
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			throw new IOException(errMsg);				
		} 
	}

	@POST
	@Path ("/seasons/{season-id}/inputschema/validate")
	@ApiOperation(value = "Validate the input schema of the specified season.", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response validateInputSchema(@PathParam("season-id")String season_id, String inputSchema,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException, MergeException {

		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("validateInputSchema request: season_id =" + season_id +", input schema = " + inputSchema);
		}
		long startTime = System.currentTimeMillis();
		logger.info("in validateInputSchema");
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
		UserInfo userInfo = UserInfo.validate("ProductServices.validateInputSchema", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);
		
		//capability verification		
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.FEATURES});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();
				
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.writeLock().lock();
		try {

			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);			

			Season season = seasonsDB.get(season_id);

			if (season == null) {
				String errMsg = Strings.seasonNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();				
			}										

			JSONObject inputSchemaJSON = null;
			try {
				inputSchemaJSON = new JSONObject(inputSchema); //the input is only the input schema (not including season_id and lastModified 
			} catch (JSONException je) {
				String errMsg = Strings.illegalInputJSON + je.getMessage();
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			InputSchema inputSchemaToValidate = season.getInputSchema();
			ValidationResults validationRes = inputSchemaToValidate.validateSchemaOnly(inputSchemaJSON, context, season);
			if (validationRes.status!=Status.OK) { //general error in schema
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}
			else {										
				//in status = OK the error string is the result JSON 
				return (Response.ok(validationRes.error)).build();
			}
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error validating input schema of season " + season_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error validating input schema: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
			long stopTime = System.currentTimeMillis();
 			long elapsedTime = stopTime - startTime;
 			System.out.println("%%%%%%%%% in finally, ProductServices.validateInputSchema time = " + elapsedTime);
		}
	}


	@POST
	@Path ("/seasons/{season-id}/utilities/simulate")
	@ApiOperation(value = "Simulates the running of a utility using the given rule or configurtion", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response simulateUtility(@PathParam("season-id")String season_id, String newUtilityAndRule,
			@ApiParam(value="DEVELOPMENT or PRODUCTION")@QueryParam("stage") String stage,
			@QueryParam("minappversion") String minAppVersion,
			@ApiParam(value="RULE or CONFIGURATION")@QueryParam("simulationtype") String simulationType,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("simulateUtility request: season_id = " + season_id + ", stage = " + stage + ", simulationtype = " + simulationType + ", newUtilityAndRule = " + newUtilityAndRule);
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
		UserInfo userInfo = UserInfo.validate("ProductServices.simulateUtility", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		if (minAppVersion==null) {
			String errMsg =   Strings.minAppMissing;
			logger.severe(errMsg);						
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
		}

		if (stage == null || stage.isEmpty()) {
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

		if (simulationType == null || simulationType.isEmpty()) {
			String errMsg =Strings.simulationTypeMissing;
			logger.severe(errMsg);
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();			
		}		
		SimulationType simulationTypeObj = Utilities.strToSimulationType(simulationType);

		if (simulationTypeObj==null) {
			String errMsg = String.format(Strings.illegalSimulationType,simulationType)  + Constants.SimulationType.returnValues();
			logger.severe(errMsg);						
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
		}

		if (!newUtilityAndRule.contains(Constants.SIMULATION_UTIL_RULE_SEPARATOR)) {
			String errMsg = Strings.missingSeparator + simulationType;
			logger.severe(errMsg);						
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build(); 			
		}

		int separatorPos = newUtilityAndRule.indexOf(Constants.SIMULATION_UTIL_RULE_SEPARATOR); 
		String newUtility =  newUtilityAndRule.substring(0,separatorPos);
		String ruleOrConfig = newUtilityAndRule.substring(separatorPos + Constants.SIMULATION_UTIL_RULE_SEPARATOR.length());

		
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.writeLock().lock();
		try {	
			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			//Validate season existence
			Season season = seasonsDB.get(season_id);

			if (season == null) {
				String errMsg = Strings.seasonNotFound;
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();				
			}														

			AirlockUtility newUtilityObj = new AirlockUtility();

			ValidationResults validationRes = null;

			if (simulationTypeObj==SimulationType.RULE)
				validationRes = newUtilityObj.simulateUtilityForRule(newUtility, ruleOrConfig, context, season, stageObj, minAppVersion);
			else //SimulationType.CONFIGURTION
				validationRes = newUtilityObj.simulateUtilityForConfiguration(newUtility, ruleOrConfig, context, season, stageObj, minAppVersion);

			JSONObject res = new JSONObject();

			if (validationRes!=null) {
				if (validationRes.status == Status.OK) {
					//when the validation results are returned with status ok - it contains the warning about duplicate functions
					res.put(Constants.JSON_FIELD_WARNING, validationRes.error);
				} else {
					String errMsg = validationRes.error;
					logger.severe(errMsg);
					return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
				}
			}



			return (Response.ok(res.toString())).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error simulate utility: ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error simulate utility: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}


	@GET
	@Deprecated
	@Path ("/seasons/branches/{branch-id}/features/{feature-id}/attributes")
	@ApiOperation(value = "DEPRECATED - Returns the specified feature's configuration attributes", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Feature not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getFeatureAttributes(@PathParam("feature-id")String feature_id,
			@PathParam("branch-id")String branch_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		return doGetItemAttributes (feature_id, branch_id, assertion, "ProductServices.getFeatureAttributes");
	}
	
	@GET
	@Path ("/seasons/branches/{branch-id}/items/{item-id}/attributes")
	@ApiOperation(value = "Returns the specified airlock item's configuration attributes", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Feature not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getItemAttributes(@PathParam("item-id")String item_id,
			@PathParam("branch-id")String branch_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		return doGetItemAttributes (item_id, branch_id, assertion, "ProductServices.getItemAttributes");
	}

	private Response doGetItemAttributes(String feature_id, String branch_id, String assertion, String funcName) {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest(funcName + " request");
		}

		String err = Utilities.validateLegalUUID(feature_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalFeatureUUID + err);

		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductForBranchOrFeature(context, branch_id, feature_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;

		//check user authorization		
		UserInfo userInfo = UserInfo.validate(funcName, context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification
		AirlockCapability[] requiredCapabilities = getRequiredFeaturesCapabilities(branch_id); //FEATURES + maybe BRANCHES		
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, requiredCapabilities);
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();
		
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.readLock().lock();
		try {			
			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			Map<String, BaseAirlockItem> airlockItemsDB = Utilities.getAirlockItemsDB (branch_id, context);

			BaseAirlockItem f = airlockItemsDB.get(feature_id);

			if (f == null) {
				return logReply(Status.NOT_FOUND, Strings.AirlockItemNotFound);
			}

			if (!f.getType().equals(Type.FEATURE) && !f.getType().equals(Type.ENTITLEMENT) && !f.getType().equals(Type.PURCHASE_OPTIONS)) {
				return logReply(Status.BAD_REQUEST, f.getType().toString() + Strings.attributesNotFound);
			}
			Season season = seasonsDB.get(f.getSeasonId().toString());

			//verify branchId
			ValidationResults validationRes = Utilities.validateBranchId(context, branch_id, null);
			if (validationRes!=null) {
				return logReply(validationRes.status, validationRes.error);
			}

			JSONObject res = new JSONObject();
			try {
				Environment env = new Environment();
				env.setServerVersion(season.getServerVersion());
				env.setBranchId(branch_id);

				try {
					JSONObject featuresTree = doGetFeatures(season, OutputJSONMode.DISPLAY, context, env, userInfo);
					JSONObject entitlementsTree = doGetPurchases(season, OutputJSONMode.DISPLAY, context, env, userInfo);
					res = AnalyticsDataCollection.getFeatureAttributeTypeJSON(featuresTree, entitlementsTree, season, context, feature_id);
				} catch (MergeException e) {
					err = Strings.mergeException  +e.getMessage();
					logger.severe(err);
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(err)).build();
				}
			} catch (JSONException je){
				logger.severe(je.getMessage());
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(je.getMessage())).build();
			}

			return Response.ok(res.toString()).build();					
		} catch (MergeException e) {
			return logReply(Status.INTERNAL_SERVER_ERROR, Strings.mergeException  + e.getMessage());		
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error getting attributes of item: " + feature_id + " from branch " + branch_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error getting item's attributes: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}
	
	public static void sendMailForAdd(ServletContext context, Map<String,BaseAirlockItem> airlockItemsDB ,BaseAirlockItem parentFeature ,DataAirlockItem newAirlockObj, UserInfo userInfo,Environment env) {
		FeatureItem featureToNotify = null;

		@SuppressWarnings("unchecked")
		Map<String, ArrayList<String>> followersFeaturesDB = (Map<String, ArrayList<String>>)context.getAttribute(Constants.FOLLOWERS_FEATURES_DB_PARAM_NAME);

		Boolean isProduction = newAirlockObj.getStage().toString().equals("PRODUCTION");
		ArrayList<String> followers = new ArrayList<>();

		if (newAirlockObj.getType() == Type.FEATURE || newAirlockObj.getType() == Type.ENTITLEMENT || newAirlockObj.getType() == Type.PURCHASE_OPTIONS) {
			featureToNotify = (FeatureItem) newAirlockObj;
			if(parentFeature.getType() == Type.FEATURE || parentFeature.getType() == Type.ENTITLEMENT || parentFeature.getType() == Type.PURCHASE_OPTIONS){
				followers = followersFeaturesDB.get(parentFeature.getUniqueId().toString());
			}
		} else if (newAirlockObj.getType() == Type.CONFIGURATION_RULE) {
			BaseAirlockItem configParent = parentFeature;
			//while (configParent.getType() != Type.FEATURE) {
			while (!(configParent instanceof FeatureItem)) { //FeatureItem, inAppPurchaseItem, purchaseOperationItem 
				configParent = airlockItemsDB.get(configParent.getParent().toString());
			}
			featureToNotify = (FeatureItem)configParent;
			followers = followersFeaturesDB.get(configParent.getUniqueId().toString());
		} else if (newAirlockObj.getType() == Type.ORDERING_RULE) {
			BaseAirlockItem orderingRuleParent = parentFeature;
			while (orderingRuleParent.getType() != Type.FEATURE && orderingRuleParent.getType() != Type.ROOT) {
				orderingRuleParent = airlockItemsDB.get(orderingRuleParent.getParent().toString());
			}
			if (orderingRuleParent.getType() == Type.FEATURE) {  
				featureToNotify = (FeatureItem)orderingRuleParent;
				followers = followersFeaturesDB.get(orderingRuleParent.getUniqueId().toString());
			}
		}
		
		if (featureToNotify!=null) {
			String details = "The " + newAirlockObj.getObjTypeStrByType() + " " + newAirlockObj.getNameSpaceDotName() + " was created with ID " + newAirlockObj.getUniqueId()+ "\n";
			Utilities.sendEmailForDataItem(context, featureToNotify, followers, details, null, null, isProduction, userInfo,env);
		}
	}

	@GET
	@Path("/{product-id}/follow")
	@ApiOperation(value = "Followers of this product", response = String.class)
	@ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Feature not found"),
			@ApiResponse(code = 500, message = "Internal error")})
	public Response getProductFollowers(@PathParam("product-id") String feature_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		return followAction(false,feature_id,assertion,FollowingAction.GET_FOLLOWING,"ProductServices.getProductFollowers");
	}

	@POST
	@Path("/{product-id}/follow")
	@ApiOperation(value = "Follow the product", response = String.class)
	@ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Feature not found"),
			@ApiResponse(code = 500, message = "Internal error")})
	public Response followProduct(@PathParam("product-id") String feature_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		return followAction(false,feature_id,assertion,FollowingAction.FOLLOW,"ProductServices.followProduct");
	}
	
	@DELETE
	@Path("/{product-id}/follow")
	@ApiOperation(value = "Unfollow the product", response = String.class)
	@ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Feature not found"),
			@ApiResponse(code = 500, message = "Internal error")})
	public Response unfollowProduct(@PathParam("product-id") String feature_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		return followAction(false,feature_id,assertion,FollowingAction.UNFOLLOW,"ProductServices.unfollowProduct");
	}

	@GET
	@Path("/seasons/features/{feature-id}/follow")
	@ApiOperation(value = "Followers of this feature", response = String.class)
	@ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Feature not found"),
			@ApiResponse(code = 500, message = "Internal error")})
	public Response getFeatureFollowers(@PathParam("feature-id") String feature_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		return followAction(true,feature_id,assertion,FollowingAction.GET_FOLLOWING,"ProductServices.getFeatureFollowers");
	}

	@POST
	@Path("/seasons/features/{feature-id}/follow")
	@ApiOperation(value = "Follow the feature", response = String.class)
	@ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Feature not found"),
			@ApiResponse(code = 500, message = "Internal error")})
	public Response followFeature(@PathParam("feature-id") String feature_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		return followAction(true,feature_id,assertion,FollowingAction.FOLLOW,"ProductServices.followFeature");
	}
	@DELETE
	@Path("/seasons/features/{feature-id}/follow")
	@ApiOperation(value = "Unfollow the feature", response = String.class)
	@ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Feature not found"),
			@ApiResponse(code = 500, message = "Internal error")})
	public Response unfollowFeature(@PathParam("feature-id") String feature_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		return followAction(true,feature_id,assertion,FollowingAction.UNFOLLOW,"ProductServices.unfollowFeature");
	}

	private enum FollowingAction {
		FOLLOW,
		UNFOLLOW,
		GET_FOLLOWING
	}
	@SuppressWarnings("unchecked")
	private Response followAction(Boolean isFeature,String id,
			String assertion, FollowingAction action,String actionName) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			if(isFeature) {
				logger.finest("follow feature request");
			}else {
				logger.finest("follow product request");
			}
		}
		AirlockChange change = new AirlockChange();
		//find relevant product
		Product currentProduct;
		if (!isFeature) {
			ProductErrorPair prodErrPair = Utilities.getProduct(context, id);
			if (prodErrPair.error != null) {			
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
			}
			currentProduct = prodErrPair.product;
		}else {
			ProductErrorPair productErrorPair = Utilities.getProductForBranchOrFeature(context, Constants.MASTER_BRANCH_NAME, id);
			if (productErrorPair.error != null) {
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
			}
			currentProduct = productErrorPair.product;	
		}
		change.setProduct(currentProduct);
		
		//check user authorization		
		UserInfo userInfo = UserInfo.validate(actionName, context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		if(userInfo == null){
			return logReply(Status.BAD_REQUEST, Strings.userNotLoggedIn);
		}
		String err = Utilities.validateLegalUUID(id);
		if (err != null) {
			String prefix = isFeature? Strings.illegalFeatureUUID: Strings.illegalProductUUID;
			return logReply(Status.BAD_REQUEST, prefix + err);
		}

		ReentrantReadWriteLock readWriteLock;
		if (!isFeature) {
			readWriteLock = (ReentrantReadWriteLock) context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
		} else {
			//capability verification		
			ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.FEATURES});
			if (capabilityValidationRes!=null) 
				return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();
			
			readWriteLock = currentProduct.getProductLock();
		}
		
		readWriteLock.readLock().lock();
		try {
			Map<String, ArrayList<String>> followersDB;
			Map<String, BaseAirlockItem> airlockItemsDB = (Map<String, BaseAirlockItem>) context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);
			Map<String, Season> seasonsDB = (Map<String, Season>) context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);
			Map<String, Product> productDB = (Map<String, Product>) context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);

			BaseAirlockItem f = airlockItemsDB.get(id);
			Season s = null;
			if(isFeature) {
				followersDB = (Map<String, ArrayList<String>>) context.getAttribute(Constants.FOLLOWERS_FEATURES_DB_PARAM_NAME);
				if (f == null) {
					return logReply(Status.NOT_FOUND, Strings.AirlockItemNotFound);
				}
				if (!f.getType().equals(Type.FEATURE) && !f.getType().equals(Type.ENTITLEMENT) && !f.getType().equals(Type.PURCHASE_OPTIONS)) {
					return logReply(Status.BAD_REQUEST, f.getType().toString() + Strings.cannotBeFollowed);
				}
			}
			else{
				followersDB = (Map<String, ArrayList<String>>) context.getAttribute(Constants.FOLLOWERS_PRODUCTS_DB_PARAM_NAME);
				Product p = productDB.get(id);
				if (p == null) {
					return logReply(Status.NOT_FOUND, Strings.productNotFound);
				}
			}

			if (userInfo != null) {
				AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
				ArrayList<String> followers = followersDB.get(id);
				if(action.equals(FollowingAction.GET_FOLLOWING)){
					JSONObject res = new JSONObject();
					res.put("uniqueID",id.toString());
					JSONArray arrFollowers = new JSONArray();
					if(followers == null){
						res.put(Constants.JSON_PRODUCT_FIELD_IS_FOLLOWING, false);
					}
					else {
						res.put(Constants.JSON_PRODUCT_FIELD_IS_FOLLOWING, followers.contains(userInfo.getId()));
						for (int i = 0; i < followers.size(); ++i) {
							arrFollowers.add(followers.get(i));
						}
					}
					res.put(Constants.JSON_PRODUCT_FIELD_FOLLOWERS,arrFollowers);
					return Response.ok(res.toString()).build();
				}
				if (action.equals(FollowingAction.FOLLOW)) {
					if(followers == null){
						ArrayList<String> newFollowers = new ArrayList<>();
						followersDB.put(id,newFollowers);
						followers = newFollowers;
					}
					if(!followers.contains(userInfo.getId())){
						followers.add(userInfo.getId());
						if(isFeature) {
							s = seasonsDB.get(f.getSeasonId().toString());
							s.addFollowersIds(id);
						}
						auditLogWriter.log("User " + userInfo.getId() +" was added to followers of "+id, userInfo);
					}
					else {
						return logReply(Status.BAD_REQUEST, String.format(Strings.allreadyFollowed, isFeature?"Item":"Product"));
					}
				}
				else if (action.equals(FollowingAction.UNFOLLOW)) {
					if(followers == null){
						return Response.ok(true).build();
					}
					followers.remove(userInfo.getId());
					if(followers.size() == 0){
						followersDB.remove(id);
						if(isFeature){
							s = seasonsDB.get(f.getSeasonId().toString());
							s.removeFollowersId(id);
						}
						auditLogWriter.log("User " + userInfo.getId() +" was removed from followers of "+id, userInfo);
					}
				}
				followersDB.put(id, followers);
				if(isFeature) {
					s = seasonsDB.get(f.getSeasonId().toString());
					change.setSeason(s);
					change.getFiles().addAll(AirlockFilesWriter.doWriteFeatureFollowers(s,context, BaseAirlockItem.isProductionFeature(f, airlockItemsDB)? Stage.PRODUCTION : Stage.DEVELOPMENT));
				}
				else {
					change.getFiles().addAll(AirlockFilesWriter.doWriteFollowersProducts(context));
				}
				Webhooks.get(context).notifyChanges(change, context);
			}
			return Response.ok().build();
		} catch (IOException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error following action on id" + id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	//    BRANCHES
	//----------------

	@POST
	@Path ("/seasons/{season-id}/{source-branch-id}/branches")
	@ApiOperation(value = "Creates a branch within the specified season", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season does not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response addBranch(@PathParam("season-id")String season_id, 
			@PathParam("source-branch-id")String source_branch_id,
			String newBranch,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("addBranch request: season_id = " + season_id + ", newBranch = " + newBranch + ", source_branch_id = " + source_branch_id);
		}
		AirlockChange change = new AirlockChange();
		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, season_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);
		//check user authorization		
		UserInfo userInfo = UserInfo.validate("ProductServices.addBranch", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		String err = Utilities.validateLegalUUID(season_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalSeasonUUID + err);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.BRANCHES});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		
		
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.writeLock().lock();
		try {						
			@SuppressWarnings("unchecked")
			Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);

			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			@SuppressWarnings("unchecked")
			Map<String, BaseAirlockItem> airlockItemsDB = (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);
			
			//Validate season existence
			Season season = seasonsDB.get(season_id);
			change.setSeason(season);
			if (season == null) {
				return logReply(Status.NOT_FOUND, Strings.seasonNotFound);				
			}

			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());
			env.setAirlockItemsDB(airlockItemsDB);

			ValidationResults validationRes = validateBranchesSupport(env);
			if (validationRes!=null) {
				return logReply(validationRes.status, validationRes.error);
			}

			//verify branchId
			validationRes = Utilities.validateBranchId(context, source_branch_id, season);
			if (validationRes!=null) {
				return logReply(validationRes.status, validationRes.error);
			}
			boolean fromMaster = source_branch_id.equals(Constants.MASTER_BRANCH_NAME);

			//validate that is a legal JSON
			JSONObject newBranchJSON = null;
			try {
				newBranchJSON = new JSONObject(newBranch);  
			} catch (JSONException je) {
				return logReply(Status.BAD_REQUEST, Strings.illegalInputJSON + je.getMessage());				
			}						

			//verify that JSON does not contain uniqueId field
			if (newBranchJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && newBranchJSON.get(Constants.JSON_FIELD_UNIQUE_ID) !=null) {
				return logReply(Status.BAD_REQUEST, Strings.branchWithId);							
			}

			//verify that JSON does not contain different season-id then the path parameter
			if (newBranchJSON.containsKey(Constants.JSON_FEATURE_FIELD_SEASON_ID) && newBranchJSON.get(Constants.JSON_FEATURE_FIELD_SEASON_ID) !=null) {
				if (!season_id.equals(newBranchJSON.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID))) {
					return logReply(Status.BAD_REQUEST, Strings.branchSeasonWithDifferentId);									
				}
			}
			else {		
				newBranchJSON.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, season_id);
			}

			Branch newBranchObj = new Branch(season.getUniqueId());
			validationRes = newBranchObj.validateBranchJSON(newBranchJSON, context, userInfo);
			if (validationRes!=null) {
				return logReply(validationRes.status, validationRes.error);
			}

			newBranchObj.fromJSON(newBranchJSON, /*airlockItemsDB,*/ env, season, context);
			if (!fromMaster) {
				Branch srcBranch = branchesDB.get(source_branch_id);								
				newBranchObj.duplicationFeaturesAndAnalyticsFromOther(srcBranch, season, context);
			}
			newBranchObj.setUniqueId(UUID.randomUUID());

			branchesDB.put(newBranchObj.getUniqueId().toString(), newBranchObj);
			season.getBranches().addBranch(newBranchObj);

			//writing updated branches list to S3
			try {				
				// all of the changes are development changes
				change.getFiles().addAll(AirlockFilesWriter.writeSeasonBranches (season, context, env, Stage.DEVELOPMENT));	
				change.getFiles().addAll(AirlockFilesWriter.writeBranchFeatures(newBranchObj, season, context, env, Stage.DEVELOPMENT));
				change.getFiles().addAll(AirlockFilesWriter.writeBranchPurchases(newBranchObj, season, context, env, Stage.DEVELOPMENT));				
				change.getFiles().addAll(AirlockFilesWriter.writeAnalytics(season, context, newBranchObj.getUniqueId().toString(), env, Stage.DEVELOPMENT));
				change.getFiles().addAll(AirlockFilesWriter.writeBranchRuntime(newBranchObj, season, context, env, true));
				Webhooks.get(context).notifyChanges(change, context);
			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}

			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Create new branch: " + newBranchObj.getName() + ", " + newBranchObj.getUniqueId().toString() + " from branch " + source_branch_id + ": "+ newBranchObj.toJson(OutputJSONMode.DISPLAY, context, env, true, true, true).toString(), userInfo); 

			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FIELD_UNIQUE_ID, newBranchObj.getUniqueId().toString());
			res.put(Constants.JSON_FIELD_NAME, newBranchObj.getName());
			res.put(Constants.JSON_FIELD_LAST_MODIFIED, newBranchObj.getLastModified().getTime());

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Added branch: " + newBranchObj.toJson(OutputJSONMode.ADMIN, context, env, true, true, true));
			}

			return (Response.ok(res.toString())).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error adding branch: ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error adding branch: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}


	private ValidationResults validateBranchesSupport (Environment env) {
		if (isBranchesSupported(env))  //only post 2.5 seasons support branches (3.0 and up)
			return null;

		String errMsg = Strings.branchNotSupported;
		logger.severe(errMsg);
		return new ValidationResults(errMsg, Status.BAD_REQUEST);
	}

	public static boolean isBranchesSupported (Environment env) {
		Version version = env.getVersion();
		return version.i >= Version.v3_0.i;  //only post 2.5 seasons support branches (3.0 and up)	
	}

	private ValidationResults validateStreamsSupport (Environment env) {
		if (isStreamsSupported(env))  //only post 3_0 seasons support streams (4.0 and up)	
			return null;

		String errMsg = Strings.streamsNotSupported;
		logger.severe(errMsg);
		return new ValidationResults(errMsg, Status.BAD_REQUEST);
	}
	
	private ValidationResults validateNotificationsSupport (Environment env) {
		if (isNotificationsSupported(env))  //only post 3_0 seasons support streams (4.0 and up)	
			return null;

		String errMsg = Strings.notificationsNotSupported;
		logger.severe(errMsg);
		return new ValidationResults(errMsg, Status.BAD_REQUEST);
	}

	public static boolean isNotificationsSupported (Environment env) {
		Version version = env.getVersion();
		return version.i >= Version.v4_5.i;  //only post 4_0 seasons support notifications (4.5 and up)	
	}
	
	public static boolean isStreamsSupported (Environment env) {
		Version version = env.getVersion();
		return version.i >= Version.v4_0.i;  //only post 3_0 seasons support streams (4.0 and up)	
	}

	public static boolean isEncryptionSupported (Environment env) {
		Version version = env.getVersion();
		return version.i >= Version.v5_0.i;  //only seasons from version 5.0 and up supports encryption	
	}

	public static boolean isCSharpConstantsSupported (Environment env) {
		Version version = env.getVersion();
		return version.i >= Version.v5_0.i;  //only seasons from version 5.0 and up supports c# constants file	
	}

	private static LinkedList<AirlockChangeContent> deleteBranchFolder(String branch_id, Season season, ServletContext context) throws IOException {
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		String separator = ds.getSeparator();

		String branchRuntimeFolder = Constants.SEASONS_FOLDER_NAME+separator+season.getProductId().toString()+
				separator+season.getUniqueId().toString()+separator+Constants.AIRLOCK_BRANCHES_FOLDER_NAME+
				separator+branch_id;

		try {
			ds.deleteFolderContent(branchRuntimeFolder);
			AirlockChangeContent changeContent = AirlockChangeContent.getAdminChange(Constants.FOLDER_DELETED, branchRuntimeFolder, Stage.DEVELOPMENT);
			LinkedList<AirlockChangeContent> toRet = new LinkedList<AirlockChangeContent>();
			toRet.add(changeContent);
			return toRet;
		} catch (IOException ioe){
			//failed deleting 3 times to s3. Set server state to ERROR.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			String error = String.format(Strings.failedDeletingBranch,branch_id) + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			throw new IOException(error);				
		} 	
	}

	@GET
	@Path ("/seasons/{season-id}/branches")
	@ApiOperation(value = "Returns the branches for the specified season", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getBranches(@PathParam("season-id")String season_id,
			@ApiParam(value="BASIC or INCLUDE_FEATURES")@QueryParam("mode")String outputMode,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getBranches request");
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
		UserInfo userInfo = UserInfo.validate("ProductServices.getBranches", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);
		
		BranchesOutputMode outputModeObj = BranchesOutputMode.BASIC; //if not specified basic is the defaults
		if (outputMode!=null && !outputMode.isEmpty()) {
			outputModeObj = Utilities.strToBranchesOutputMode(outputMode);

			if (outputModeObj==null) {
				String errMsg = String.format(Strings.illegalOutputMode,outputMode)  + Constants.BranchesOutputMode.returnValues();
				logger.severe(errMsg);						
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}
		}

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.BRANCHES});
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

			//no branches support is needed - if not supported, only the dummy master branch is returned

			/*

			ValidationResults validationRes = validateBranchesSupport(env);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}*/

			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());

			JSONObject res = season.getBranches().toJson(OutputJSONMode.ADMIN, context, env, outputModeObj.equals(BranchesOutputMode.INCLUDE_FEATURES), true, outputModeObj.equals(BranchesOutputMode.INCLUDE_FEATURES));

			return (Response.ok()).entity(res.toString()).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error getting branches of season " + season_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error getting branches: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	@GET
	@Path ("/seasons/branches/{branch-id}")
	@ApiOperation(value = "Returns the specified branch", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Branch not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getBranch(@PathParam("branch-id")String branch_id,
			@ApiParam(value="BASIC or INCLUDE_FEATURES")@QueryParam("mode")String outputMode,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getBranch request");
		}

		String err = Utilities.validateLegalUUID(branch_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalBranchUUID + err);	

		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, branch_id, null);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		
		//check user authorization		
		UserInfo userInfo = UserInfo.validate("ProductServices.getBranch", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		BranchesOutputMode outputModeObj = BranchesOutputMode.BASIC; //if not specified basic is the defaults
		if (outputMode!=null && !outputMode.isEmpty()) {
			outputModeObj = Utilities.strToBranchesOutputMode(outputMode);

			if (outputModeObj==null) {
				String errMsg = String.format(Strings.illegalOutputMode,outputMode)  + Constants.BranchesOutputMode.returnValues();
				logger.severe(errMsg);						
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}
		}

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.BRANCHES});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		
		
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.readLock().lock();
		try {			

			@SuppressWarnings("unchecked")
			Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);

			Branch branch = branchesDB.get(branch_id);

			if (branch == null) {
				String errMsg = Strings.branchNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			Season season = seasonsDB.get(branch.getSeasonId().toString());
			if (season == null) {
				String errMsg = Strings.seasonNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());

			return Response.ok(branch.toJson(OutputJSONMode.ADMIN, context, env, outputModeObj.equals(BranchesOutputMode.INCLUDE_FEATURES), true, outputModeObj.equals(BranchesOutputMode.INCLUDE_FEATURES)).toString()).build();					
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error getting branch " + branch_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error getting branch: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	@DELETE
	@Path ("/seasons/branches/{branch-id}")
	@ApiOperation(value = "Deletes the specified branch")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Branch not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response deleteBranch(@PathParam("branch-id")String branch_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException  {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("deleteBranch request: branch_id =" + branch_id);
		}
		AirlockChange change = new AirlockChange();
		String err = Utilities.validateLegalUUID(branch_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalBranchUUID + err);

		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, branch_id, null);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);
		
		//check user authorization		
		UserInfo userInfo = UserInfo.validate("ProductServices.deleteBranch", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);		
		
		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.BRANCHES});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		
		
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.writeLock().lock();
		try {		

			@SuppressWarnings("unchecked")
			Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);
			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			if (!branchesDB.containsKey(branch_id)) {
				String errMsg = Strings.branchNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			Branch branchToDel = branchesDB.get(branch_id);			

			Season season = seasonsDB.get(branchToDel.getSeasonId().toString()); 
			change.setSeason(season);
			change.setBranch(branchToDel);
			
			if (season == null) {
				String errMsg = String.format(Strings.nonExistingSeason,branchToDel.getSeasonId().toString());
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();				
			}

			//check if branch is part of experiment
			String partOfExpData = branchToDel.isPartOfExperiment(context);
			if (partOfExpData != null) {
				String errMsg = Strings.branchInUse + partOfExpData;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			season.getBranches().removeBranch(branchToDel.getName());

			branchesDB.remove(branch_id);
			
			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());

			//writing updated branches list to S3
			try {
				change.getFiles().addAll(AirlockFilesWriter.writeSeasonBranches(season, context, env, Stage.DEVELOPMENT));
				change.getFiles().addAll(deleteBranchFolder(branch_id, season, context));
				Webhooks.get(context).notifyChanges(change, context);
			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}

			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Delete branch: uniqueId: " + branch_id + ", name: " + branchToDel.getName(), userInfo); 

			logger.info("Branch " + branch_id + " was deleted");

			return (Response.ok()).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error deleting branch " + branch_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error deleting branch: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	@PUT
	@Path ("/seasons/branches/{branch-id}")
	@ApiOperation(value = "Updates the specified branch", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Branch not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response updateBranch(@PathParam("branch-id")String branch_id, String branch,			
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("updateBranch request: branch_id =" + branch_id + ", branch = " + branch);
		}
		AirlockChange change = new AirlockChange();
		String err = Utilities.validateLegalUUID(branch_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalBranchUUID + err);							

		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, branch_id, null);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);
		
		//check user authorization
		UserInfo userInfo = UserInfo.validate("ProductServices.updateBranch", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.BRANCHES});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		
		
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.writeLock().lock();
		try {

			@SuppressWarnings("unchecked")
			Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);

			@SuppressWarnings("unchecked")			
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			Branch branchToUpdate = branchesDB.get(branch_id);
			if (branchToUpdate == null) {
				String errMsg = Strings.branchNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();			
			}			
			change.setBranch(branchToUpdate);
			
			//check if branch is part of experiment
			boolean isPartOfExp = false;
			String partOfExpData = branchToUpdate.isPartOfExperiment(context);
			if (partOfExpData != null) {
				isPartOfExp = true;
			}
			//build utility json
			JSONObject updatedBranchJSON = null;
			try {
				updatedBranchJSON = new JSONObject(branch);
			} catch (JSONException je) {
				String errMsg = Strings.illegalInputJSON + je.getMessage();
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			//if not set - set the uniqueId to be the id path param
			if (!updatedBranchJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) || updatedBranchJSON.get(Constants.JSON_FIELD_UNIQUE_ID) == null) {
				updatedBranchJSON.put(Constants.JSON_FIELD_UNIQUE_ID, branch_id);
			}
			else {
				//verify that branch-id in path is identical to uniqueId in request pay-load  
				if (!updatedBranchJSON.getString(Constants.JSON_FIELD_UNIQUE_ID).equals(branch_id)) {
					String errMsg = Strings.branchWithId;
					logger.severe(errMsg);
					return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();					
				}
			}

			Season season = seasonsDB.get(branchToUpdate.getSeasonId().toString()); 		
			if (season == null) {
				String errMsg =String.format(Strings.nonExistingSeason,branchToUpdate.getSeasonId().toString());
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();				
			}
			change.setSeason(season);

			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());

			ValidationResults validationRes = branchToUpdate.validateBranchJSON(updatedBranchJSON, context, userInfo);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			//finally - actually update the utility.
			String updateDetails = branchToUpdate.updateBranch(updatedBranchJSON);

			if (!updateDetails.isEmpty()) { //if some fields were changed		
				try {
					change.getFiles().addAll(AirlockFilesWriter.writeSeasonBranches(season, context, env, isPartOfExp? Stage.PRODUCTION : Stage.DEVELOPMENT)); //branch name was changed - should write the runtime file as well
					change.getFiles().addAll(AirlockFilesWriter.writeBranchFeatures(branchToUpdate, season, context, env, isPartOfExp? Stage.PRODUCTION : Stage.DEVELOPMENT));									
					change.getFiles().addAll(AirlockFilesWriter.writeBranchRuntime(branchToUpdate, season, context, env, true));
					Webhooks.get(context).notifyChanges(change, context);
				} catch (IOException e) {
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
				}

				AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
				auditLogWriter.log("Update branch: " + branch_id + ",   " + updateDetails, userInfo); 								
			}

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Updated branch: " + branchToUpdate.toJson(OutputJSONMode.ADMIN, context, env, false, false, false) + "\n updatd details: " + updateDetails);
			}

			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FIELD_UNIQUE_ID, branch_id);
			res.put(Constants.JSON_FIELD_LAST_MODIFIED, branchToUpdate.getLastModified().getTime());
			return (Response.ok(res.toString())).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error updating branch: " + branch_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error updating branch: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}	


	@PUT
	@Path ("/seasons/branches/{branch-id}/checkout/{item-id}") 
	@ApiOperation(value = "Checkout the specified feature to the specified branch", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Branch not found"),
			@ApiResponse(code = 404, message = "Feature not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response checkout(@PathParam("branch-id")String branch_id, 
			@PathParam("item-id")String item_id,	
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("checkout request: branch_id =" + branch_id + ", feature_id = " + item_id);
		}
		AirlockChange change = new AirlockChange();

		String err = Utilities.validateLegalUUID(branch_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalBranchUUID + err);

		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, branch_id, null);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);
		
		//check user authorization
		UserInfo userInfo = UserInfo.validate("ProductServices.checkout", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		err = Utilities.validateLegalUUID(item_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalFeatureUUID + err);

		//capability verification
		AirlockCapability[] requiredCapabilities = getRequiredFeaturesCapabilities(branch_id); //FEATURES + maybe BRANCHES
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, requiredCapabilities);
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		
	
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.writeLock().lock();
		try {

			@SuppressWarnings("unchecked")
			Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);

			@SuppressWarnings("unchecked")
			Map<String, BaseAirlockItem> airlockItemsDB = (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);

			@SuppressWarnings("unchecked")			
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			Branch branch = branchesDB.get(branch_id);
			if (branch == null) {
				return sendAndLogError(Status.NOT_FOUND, Strings.branchNotFound);			
			}
			change.setBranch(branch);

			BaseAirlockItem item = airlockItemsDB.get(item_id); 
			if (item == null) {
				return sendAndLogError(Status.NOT_FOUND, Strings.AirlockItemNotFound);			
			}	

			Season season = seasonsDB.get(branch.getSeasonId().toString());
			if (season == null) {
				return sendAndLogError(Status.NOT_FOUND, Strings.seasonNotFound);			
			}		
			
			if (!branch.getSeasonId().equals(item.getSeasonId())) {
				return sendAndLogError(Status.NOT_FOUND, Strings.branchFeatureWithDifferentSeason);	
			}
			
			if (!item.getType().equals(Type.FEATURE) && !item.getType().equals(Type.MUTUAL_EXCLUSION_GROUP) && !item.getType().equals(Type.ROOT) &&
					!item.getType().equals(Type.ENTITLEMENT) && !item.getType().equals(Type.ENTITLEMENT_MUTUAL_EXCLUSION_GROUP) && 
					!item.getType().equals(Type.PURCHASE_OPTIONS) && !item.getType().equals(Type.PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP)) {
				return sendAndLogError(Status.BAD_REQUEST, Strings.typeCheckoutError);				
			}

			REQUEST_ITEM_TYPE itemType = REQUEST_ITEM_TYPE.FEATURES;
			if (item.getType().equals(Type.ENTITLEMENT) || item.getType().equals(Type.ENTITLEMENT_MUTUAL_EXCLUSION_GROUP) ||  
					item.getType().equals(Type.PURCHASE_OPTIONS) || item.getType().equals(Type.PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP) ||
					(item.getType().equals(Type.ROOT) && item.getUniqueId().equals(season.getEntitlementsRoot().getUniqueId())) ) {
				itemType = REQUEST_ITEM_TYPE.ENTITLEMENTS;
			}
			
			change.setSeason(season);

			BaseAirlockItem faetureInBranch = branch.getBranchFeatureById(item.getUniqueId().toString());
			
			//If feature if root and its status is none  - it is not checked out, will be removed from list later on
			if (faetureInBranch!=null && !(faetureInBranch.getType().equals(Type.ROOT) && faetureInBranch.getBranchStatus().equals(BranchStatus.NONE))) {
				return sendAndLogError(Status.BAD_REQUEST, Strings.alreadyCheckout);	
			}

			boolean isProduction = BaseAirlockItem.isProductionFeature(item, airlockItemsDB);
			Stage changeStage = isProduction? Stage.PRODUCTION : Stage.DEVELOPMENT;
			
			//feature in production can be checked out only by Administrator or ProductLead if the branch is participating in prod exp
			if (isProduction && !validRole(userInfo) &&
					branch.isPartOfExperimentInProduction(context)!=null) {
				return sendAndLogError(Status.UNAUTHORIZED, Strings.prodCheckoutError);	
			}

			BaseAirlockItem checkedOutFeature = branch.checkoutFeature(item, context, itemType);

			if (checkedOutFeature == null) {
				//the feature is in the master but not visible in branch (for example: if its parent was checked out before it was added to master) 
				return logReply(Status.BAD_REQUEST, Strings.featureNotVisibleInBranch);
			}

			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());
			env.setBranchId(branch_id);
			env.setAirlockItemsDB(airlockItemsDB);
			try {
				ValidationResults res = Utilities.validateBranchStructure(branch_id, season, context);
				if (res!=null) {
					return sendAndLogError(res.status, res.error);
				}

				if (itemType.equals(REQUEST_ITEM_TYPE.FEATURES)) {
					change.getFiles().addAll(AirlockFilesWriter.writeBranchFeatures(branch, season, context, env, changeStage));
				}
				else { //REQUEST_ITEM_TYPE.PURCHASES
					change.getFiles().addAll(AirlockFilesWriter.writeBranchPurchases(branch, season, context, env, changeStage));
				}

				boolean writeProduction = isProduction;				

				change.getFiles().addAll(AirlockFilesWriter.writeBranchAndMasterRuntimeFiles(season, branch, context, env, writeProduction));

				//if feature was reported to analytics in master it is now reported to analytics in branch
				if (branch.isFeatureInAnalytics(checkedOutFeature)) {
					change.getFiles().addAll(AirlockFilesWriter.writeAnalytics(season, context, branch_id, env, changeStage));
				}
				Webhooks.get(context).notifyChanges(change, context);

			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();				
			}

			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Checkout airlock item: " + item.getType().toString() + ", " + item_id + ", " + item.getNameSpaceDotName() + " to branch: " + branch_id + ", " + branch.getName(), userInfo); 								

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Checkout airlock item: " + item.getType().toString() + ", " + item_id + ", " + item.getNameSpaceDotName() + " to branch: " + branch_id + ", " + branch.getName());
			}

			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FIELD_UNIQUE_ID, checkedOutFeature.getUniqueId().toString());			
			return (Response.ok(res.toString())).build();
		} catch (MergeException e) {
			err = Strings.mergeException  +e.getMessage();
			logger.severe(err);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(err)).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error checkout item " + item_id + " to branch "+ branch_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error checkout item: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	@PUT
	@Path ("/seasons/branches/{branch-id}/cancelcheckout/{feature-id}") 
	@ApiOperation(value = "Checkout the specified feature to the specified branch", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Branch not found"),
			@ApiResponse(code = 404, message = "Feature not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response cancelCheckout(@PathParam("branch-id")String branch_id, 
			@PathParam("feature-id")String feature_id,	
			@ApiParam(value="STAND_ALONE or INCLUDE_SUB_FEATURES")@QueryParam("mode")String mode,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("cancelCheckout request: branch_id =" + branch_id + ", feature_id = " + feature_id);
		}
		AirlockChange change = new AirlockChange();
		String err = Utilities.validateLegalUUID(branch_id);
		if (err!=null) 	
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalBranchUUID + err);

		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, branch_id, null);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);
		
		//check user authorization
		UserInfo userInfo = UserInfo.validate("ProductServices.cancelCheckout", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);		

		err = Utilities.validateLegalUUID(feature_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalFeatureUUID + err);

		CancelCheckoutMode cancelModeodeObj = CancelCheckoutMode.STAND_ALONE; //if not specified stand-alone is the defaults
		if (mode!=null && !mode.isEmpty()) {
			cancelModeodeObj = Utilities.strToCancelCheckoutMode(mode);

			if (cancelModeodeObj==null) {
				return sendAndLogError(Status.BAD_REQUEST, String.format(Strings.illegalMode,mode)  + Constants.CancelCheckoutMode.returnValues());
			}
		}

		//capability verification
		AirlockCapability[] requiredCapabilities = getRequiredFeaturesCapabilities(branch_id); //FEATURES + maybe BRANCHES
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, requiredCapabilities);
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		
		
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.writeLock().lock();
		try {

			@SuppressWarnings("unchecked")
			Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);

			@SuppressWarnings("unchecked")
			Map<String, BaseAirlockItem> airlockItemsDB = (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);

			@SuppressWarnings("unchecked")			
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			Branch branch = branchesDB.get(branch_id);
			if (branch == null) {
				return sendAndLogError(Status.NOT_FOUND, Strings.branchNotFound);			
			}
			change.setBranch(branch);

			if (airlockItemsDB.get(feature_id) == null) {
				return sendAndLogError(Status.NOT_FOUND, Strings.AirlockItemNotFound);			
			}	

			BaseAirlockItem featureInBranch = branch.getBranchFeatureById(feature_id);
			if (featureInBranch == null || featureInBranch.getBranchStatus().equals(BranchStatus.NONE)) {
				return sendAndLogError(Status.NOT_FOUND, Strings.featureNotInBranch);	
			}

			Map<String, BaseAirlockItem> branchAirlockItemsDB = Utilities.getAirlockItemsDB(branch_id, context);
			
			boolean isProduction = false;
			if (featureInBranch instanceof DataAirlockItem) {
				if (((DataAirlockItem)featureInBranch).getStage().equals(Stage.PRODUCTION)) {
					isProduction = true;
				}
			}
			else {
				if (BaseAirlockItem.isProductionFeature(featureInBranch, branchAirlockItemsDB/*airlockItemsDB*/)) {
					isProduction =  true;
				}
			}
			
			//feature in production can be un-checked out only by Administrator or ProductLead if the branch is participating in prod exp
			if (isProduction && !validRole(userInfo) && branch.isPartOfExperimentInProduction(context)!=null) {
				String errMsg = Strings.prodCancelCheckoutError;

				logger.severe(errMsg);
				return Response.status(Status.UNAUTHORIZED).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();	
			}

			if (!branch.getSeasonId().equals(featureInBranch.getSeasonId())) {
				return sendAndLogError(Status.BAD_REQUEST, Strings.branchFeatureWithDifferentSeason);	
			}

			//if feature has NEW children in branch - it cannot be unchecked out
			if (featureInBranch.hasNewSubItems()) {
				return logReply(Status.BAD_REQUEST, Strings.cancelCheckoutErrorNewSubItemsExist);
			}

			Season season = seasonsDB.get(branch.getSeasonId().toString());
			if (season == null) {
				return sendAndLogError(Status.NOT_FOUND, Strings.seasonNotFound);			
			}	

			if (!featureInBranch.getType().equals(Type.FEATURE) && !featureInBranch.getType().equals(Type.MUTUAL_EXCLUSION_GROUP) && !featureInBranch.getType().equals(Type.ROOT) &&
					!featureInBranch.getType().equals(Type.PURCHASE_OPTIONS) && !featureInBranch.getType().equals(Type.PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP) && !featureInBranch.getType().equals(Type.ENTITLEMENT) &&
					!featureInBranch.getType().equals(Type.ENTITLEMENT_MUTUAL_EXCLUSION_GROUP)) {
				return sendAndLogError(Status.BAD_REQUEST, Strings.typeCancelCheckoutError);				
			}
			
			REQUEST_ITEM_TYPE itemType = REQUEST_ITEM_TYPE.FEATURES;
			if (featureInBranch.getType().equals(Type.ENTITLEMENT) || featureInBranch.getType().equals(Type.ENTITLEMENT_MUTUAL_EXCLUSION_GROUP) ||  
					featureInBranch.getType().equals(Type.PURCHASE_OPTIONS) || featureInBranch.getType().equals(Type.PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP) ||
					(featureInBranch.getType().equals(Type.ROOT) && featureInBranch.getUniqueId().equals(season.getEntitlementsRoot().getUniqueId())) ) {
				itemType = REQUEST_ITEM_TYPE.ENTITLEMENTS;
			}

			change.setSeason(season);

			boolean includeSubFeatures = cancelModeodeObj.equals(CancelCheckoutMode.INCLUDE_SUB_FEATURES);
			if (includeSubFeatures && branch.newBranchFeaturesIncludes(featureInBranch)) {
				return sendAndLogError(Status.BAD_REQUEST, Strings.cannotRemoveFromBranch);
			}

			boolean featureInAnalytics = branch.isFeatureInAnalytics(featureInBranch);
			branch.cancelCheckout(featureInBranch, context, includeSubFeatures, branchAirlockItemsDB, itemType);

			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());
			env.setAirlockItemsDB(branchAirlockItemsDB);

			try {
				ValidationResults res = Utilities.validateBranchStructure(branch_id, season, context);
				if (res!=null) {
					return sendAndLogError(res.status, res.error);
				}

				boolean writeProduction = BaseAirlockItem.isProductionFeature(featureInBranch, branchAirlockItemsDB);
				
				if (itemType.equals(REQUEST_ITEM_TYPE.FEATURES)) {
					change.getFiles().addAll(AirlockFilesWriter.writeBranchFeatures(branch, season, context, env, writeProduction? Stage.PRODUCTION : Stage.DEVELOPMENT));
				}
				else { //REQUEST_ITEM_TYPE.PURCHASES
					change.getFiles().addAll(AirlockFilesWriter.writeBranchPurchases(branch, season, context, env, writeProduction? Stage.PRODUCTION : Stage.DEVELOPMENT));					
				}
				
				change.getFiles().addAll(AirlockFilesWriter.writeBranchAndMasterRuntimeFiles(season, branch, context, env, writeProduction));
				//if feature was reported to analytics in master it is now reported to analytics in branch
				if (featureInAnalytics) {
					change.getFiles().addAll(AirlockFilesWriter.writeAnalytics(season, context, branch_id, env, writeProduction? Stage.PRODUCTION : Stage.DEVELOPMENT));
				}
				Webhooks.get(context).notifyChanges(change, context);
			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();				
			}

			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Cancel feature checkout feature: " + feature_id + ", " + featureInBranch.getNameSpaceDotName() + " from branch: " + branch_id + ", " + branch.getName(), userInfo); 								

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Cancel feature checkout: " + feature_id + ", " + featureInBranch.getNameSpaceDotName() + " from branch: " + branch_id + ", " + branch.getName());
			}

			JSONObject res = new JSONObject();
			//res.put(Constants.JSON_FIELD_UNIQUE_ID, checkedOutFeature.getUniqueId().toString());			
			return (Response.ok(res.toString())).build();
		} catch (MergeException e) {
			err = Strings.mergeException  + e.getMessage();
			logger.severe(err);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(err)).build();							
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error cancel checkout of item " + feature_id + " in branch "+ branch_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error cancel checkout: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	public static Stage isBranchInExp(Branch branch, Season season, ServletContext context) {
		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);					

		Product prod = productsDB.get(season.getProductId().toString());
		ArrayList<Experiment> seasonExperiments = prod.getExprimentsPerSeason(season);

		Stage res = null;
		for (Experiment exp:seasonExperiments) {
			for (Variant var:exp.getVariants()) {
				if (var.getBranchName().equals(branch.getName())) {
					if (res == null)
						res = var.getStage();
					else
						res = (res==Stage.PRODUCTION)?res:var.getStage();
				}
			}
		}

		return res;		
	}	
	
	static boolean validRole(UserInfo userInfo)
	{
		return userInfo == null || userInfo.getRoles().contains(RoleType.Administrator) || userInfo.getRoles().contains(RoleType.ProductLead);
	}

	@GET
	@Path ("/seasons/{season-id}/allfeatures")
	@ApiOperation(value = "Returns the feature list for the specified season ", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getAllFeatures(@PathParam("season-id")String season_id,			 
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {		

		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getAllFeatures request");
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
		UserInfo userInfo = UserInfo.validate("ProductServices.getAllFeatures", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.FEATURES});
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

			JSONObject res = Utilities.getAllFeaturesList(season);

			return (Response.ok()).entity(res.toString()).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error getting features of season " + season_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error getting features: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
			//long stopTime = System.currentTimeMillis();
			//long elapsedTime = stopTime - startTime;
			//System.out.println("^^^^^^^^^^^^ in finally, ProductServices.getFeatures time = " + elapsedTime);
		}
	}	


	@POST
	@Path ("/seasons/{season-id}/streams")
	@ApiOperation(value = "Creates a stream within the specified season", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response addStream(@PathParam("season-id")String season_id, 
			String newStream,
			@QueryParam("force")Boolean force,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("addStream request: season_id = " + season_id + ", newStream = " + newStream);
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
		
		//check user authorization
		UserInfo userInfo = UserInfo.validate("ProductServices.addStream", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);
		
		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.STREAMS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();	
				
		
		if (force == null)
			force = false; //force means update the input schema without rule/config/utility validations

		if (force == true && !validAdmin(userInfo)) {
			String errMsg = Strings.streamNoValidationError;
			logger.severe(errMsg);
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();			
		}

		//validate that is a legal JSON
		JSONObject newStreamJSON = null;
		try {
			newStreamJSON = new JSONObject(newStream);  
		} catch (JSONException je) {
			String errMsg = Strings.illegalInputJSON + je.getMessage();
			logger.severe(errMsg);
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();				
		}

		//verify that JSON does not contain uniqueId field
		if (newStreamJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && newStreamJSON.get(Constants.JSON_FIELD_UNIQUE_ID) !=null) {
			String errMsg = Strings.streamWithId;
			logger.severe(errMsg);
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();							
		}

		//verify that JSON does not contain different season-id then the path parameter
		if (newStreamJSON.containsKey(Constants.JSON_FEATURE_FIELD_SEASON_ID) && newStreamJSON.get(Constants.JSON_FEATURE_FIELD_SEASON_ID) !=null) {
			if (!season_id.equals(newStreamJSON.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID))) {
				String errMsg = Strings.streamSeasonWithDifferentId;
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();									
			}
		}
		else {		
			newStreamJSON.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, season_id);
		}
		
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		
		readWriteLock.writeLock().lock();
		try {	
			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			@SuppressWarnings("unchecked")
			Map<String, AirlockStream> streamsDB = (Map<String, AirlockStream>)context.getAttribute(Constants.STREAMS_DB_PARAM_NAME);

			//Validate season existence
			Season season = seasonsDB.get(season_id);
			change.setSeason(season);
			if (season == null) {
				String errMsg = Strings.seasonNotFound;
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();				
			}			

			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());

			ValidationResults validationRes = validateStreamsSupport(env);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			AirlockStream newStreamObj = new AirlockStream();

			validationRes = newStreamObj.validateStreamJSON(newStreamJSON, context, userInfo, season_id, force);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			newStreamObj.fromJSON(newStreamJSON);
			newStreamObj.setUniqueId(UUID.randomUUID());

			season.getStreams().addAirlockStream(newStreamObj);

			try {
				JSONObject streamsJson = season.getStreams().toJson(OutputJSONMode.ADMIN);
				season.getInputSchema().mergeSchema (streamsJson, false, null);
			} catch (GenerationException e) {
				//ignore. should not happen since we are after validation
			}

			try {
				change.getFiles().addAll(AirlockFilesWriter.writeSeasonStreams(season, newStreamObj.getStage() == Stage.PRODUCTION, context));
				change.getFiles().addAll(AirlockFilesWriter.doWriteDefaultsFile(season, context, newStreamObj.getStage()));
				Webhooks.get(context).notifyChanges(change, context);
			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}

			streamsDB.put(newStreamObj.getUniqueId().toString(), newStreamObj);

			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Create new stream: " + newStreamObj.toJson(OutputJSONMode.ADMIN).toString(), userInfo); 

			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FIELD_UNIQUE_ID, newStreamObj.getUniqueId().toString());
			res.put(Constants.JSON_FIELD_LAST_MODIFIED, newStreamObj.getLastModified().getTime());

			logger.info("Stream added to season '"+  season_id + "': "+ res.toString());

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Added stream: " + newStreamObj.toJson(OutputJSONMode.ADMIN));
			}

			return (Response.ok(res.toString())).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error adding stream: ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error adding stream: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	@POST
	@Path ("/seasons/{season-id}/streams/eventsfieldsbyfilter")
	@ApiOperation(value = "Returns the fields relevants to the events in the given filter.", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getEventsFieldsByFilter(@PathParam("season-id")String season_id, String filter,
			@ApiParam(value="DEVELOPMENT or PRODUCTION")@QueryParam("stage")String stage, 
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException, MergeException {

		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getEventsFieldsByFilter request: season_id =" + season_id +", filter = " + filter + ", stage = " + stage);
		}

		String err = Utilities.validateLegalUUID(season_id);
		if (err!=null) {
			return logReply(Status.BAD_REQUEST, Strings.illegalSeasonUUID + err);
		}

		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, season_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		
		//check user authorization
		UserInfo userInfo = UserInfo.validate("ProductServices.getEventsFieldsByFilter", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);
		
		if (stage==null) {
			return logReply(Status.BAD_REQUEST, Strings.stageMissing);
		}				

		Stage stageObj = Utilities.strToStage(stage);

		if (stageObj==null) {
			return logReply(Status.BAD_REQUEST, String.format(Strings.illegalStage,stage) + Constants.Stage.returnValues());
		}

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.STREAMS});
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
				return logReply(Status.BAD_REQUEST, Strings.seasonNotFound);
			}			

			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());

			ValidationResults validationRes = validateStreamsSupport(env);
			if (validationRes!=null) {
				return logReply(validationRes.status, validationRes.error);
			}

			JSONObject res;
			try {
				res = season.getStreamsEvents().getEventsFieldsByFilter(filter, context, season, stageObj);
			} catch (Exception e) {
				return logReply(Status.INTERNAL_SERVER_ERROR, Strings.failGetEventsFields + e.getMessage());
			}

			return (Response.ok(res.toString())).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error getting events fields by filter of season: " + season_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error getting events fields by filter: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();

		}
	}

	@POST
	@Path ("/seasons/{season-id}/streams/eventsfields")
	@ApiOperation(value = "Returns the events fields for the specified season", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getEventsFields(@PathParam("season-id")String season_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException, MergeException {

		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getEventsFields request: season_id =" + season_id);
		}

		String err = Utilities.validateLegalUUID(season_id);
		if (err!=null) 
			return logReply(Status.BAD_REQUEST, Strings.illegalSeasonUUID + err);		
		
		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, season_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		
		//check user authorization
		UserInfo userInfo = UserInfo.validate("ProductServices.getEventsFields", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);		

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.STREAMS});
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
				return logReply(Status.BAD_REQUEST, Strings.seasonNotFound);
			}			

			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());

			ValidationResults validationRes = validateStreamsSupport(env);
			if (validationRes!=null) {
				return logReply(validationRes.status, validationRes.error);
			}

			JSONObject res;
			try {
				res = season.getStreamsEvents().getEventsFields(context, season);
			} catch (Exception e) {
				return logReply(Status.INTERNAL_SERVER_ERROR, Strings.failGetEventsFields + e.getMessage());
			}

			return (Response.ok(res.toString())).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error getting events fields of season: " + season_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error getting events fields: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();

		}
	}

	@GET
	@Path("/seasons/{season-id}/streams/events")
	@ApiOperation(value = "Returns all streams events for the specified season", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getStreamsEvents(@PathParam("season-id")String season_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getStreamsEvents request: season_id = " + season_id);
		}

		String err = Utilities.validateLegalUUID(season_id);
		if (err!=null) 
			return logReply(Status.BAD_REQUEST, Strings.illegalSeasonUUID + err);

		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, season_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		
		//check user authorization
		UserInfo userInfo = UserInfo.validate("ProductServices.getStreamsEvents", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);			

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.STREAMS});
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
				return logReply(Status.BAD_REQUEST, Strings.seasonNotFound);
			}			

			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());

			ValidationResults validationRes = validateStreamsSupport(env);
			if (validationRes!=null) {
				return logReply(validationRes.status, validationRes.error);
			}

			JSONObject res = season.getStreamsEvents().toJson();
			return (Response.ok()).entity(res.toString()).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error getting streams events of season: " + season_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error getting streams events: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}			

	@PUT
	@Path("/seasons/{season-id}/streams/events")
	@ApiOperation(value = "Updates streams events for the specified season", response = String.class)
	@Produces(value="text/plain")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response setStreamsEvents(@PathParam("season-id")String season_id, String events, 
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("setStreamsEvents request: season_id = " + season_id + ", events = " + events);
		}
		AirlockChange change = new AirlockChange();
		String err = Utilities.validateLegalUUID(season_id);
		if (err!=null) 
			return logReply(Status.BAD_REQUEST, Strings.illegalSeasonUUID + err);
		
		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, season_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);
		
		//check user authorization
		UserInfo userInfo = UserInfo.validate("ProductServices.setStreamsEvents", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);		

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.STREAMS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();	
				
		//validate that is a legal JSON
		JSONObject newEventsJSON = null;
		try {
			newEventsJSON = new JSONObject(events);  
		} catch (JSONException je) {
			return logReply(Status.BAD_REQUEST, Strings.illegalSeasonUUID + Strings.illegalInputJSON + je.getMessage());				
		}

		//verify that JSON does not contain different season-id then the path parameter
		if (newEventsJSON.containsKey(Constants.JSON_FEATURE_FIELD_SEASON_ID) && newEventsJSON.get(Constants.JSON_FEATURE_FIELD_SEASON_ID) !=null) {
			if (!season_id.equals(newEventsJSON.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID))) {
				return logReply(Status.BAD_REQUEST, Strings.streamSeasonWithDifferentId);
			}
		}
		else {		
			newEventsJSON.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, season_id);
		}
		
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();				
		readWriteLock.writeLock().lock();
		try {	
			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			//Validate season existence
			Season season = seasonsDB.get(season_id);
			change.setSeason(season);
			
			if (season == null) {
				return logReply(Status.BAD_REQUEST, Strings.seasonNotFound);
			}			

			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());

			ValidationResults validationRes = validateStreamsSupport(env);
			if (validationRes!=null) {
				return logReply(validationRes.status, validationRes.error);
			}

			validationRes = season.getStreamsEvents().validateStreamEventsJSON(newEventsJSON);
			if (validationRes!=null) {
				return logReply(validationRes.status, validationRes.error);
			}

			season.getStreamsEvents().fromJSON(newEventsJSON);
			season.getStreamsEvents().setLastModified(new Date());

			try {
				change.getFiles().addAll(AirlockFilesWriter.writeSeasonStreamsEvents(season, context));
				Webhooks.get(context).notifyChanges(change, context);
			} catch (IOException e) {
				return logReply(Status.INTERNAL_SERVER_ERROR, e.getMessage());
			}

			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Stream events for season : " + season_id + " were updated: "  + newEventsJSON.toString(), userInfo); 

			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, season_id);
			res.put(Constants.JSON_FIELD_LAST_MODIFIED, season.getStreamsEvents().getLastModified().getTime());

			logger.info("Stream events updated for season '"+  season_id);

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Stream events updated for season " + season_id + ": " + newEventsJSON.toString());
			}

			return (Response.ok(res.toString())).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error setting streams events of season: " + season_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error setting streams events: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}
	static boolean validAdmin(UserInfo userInfo)
	{
		return userInfo == null || userInfo.getRoles().contains(RoleType.Administrator);
	}
	Response logReply(Status status, String errMsg)
	{
		logger.severe(errMsg);
		return Response.status(status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();		
	}
	
	@POST
	@Path ("/seasons/{season-id}/notifications")
	@ApiOperation(value = "Creates a notification within the specified season", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response addNotification(@PathParam("season-id")String season_id, 
			String newNotification,			
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("addNotification request: season_id = " + season_id + ", newNotification = " + newNotification);
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
		
		//check user authorization
		UserInfo userInfo = UserInfo.validate("ProductServices.addNotification", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);
		
		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.NOTIFICATIONS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();	
							
		//validate that is a legal JSON
		JSONObject newNotificationJSON = null;
		try {
			newNotificationJSON = new JSONObject(newNotification);  
		} catch (JSONException je) {
			String errMsg = Strings.illegalInputJSON + je.getMessage();
			logger.severe(errMsg);
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();				
		}

		//verify that JSON does not contain uniqueId field
		if (newNotificationJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && newNotificationJSON.get(Constants.JSON_FIELD_UNIQUE_ID) !=null) {
			String errMsg = Strings.notificationWithId;
			logger.severe(errMsg);
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();							
		}

		//verify that JSON does not contain different season-id then the path parameter
		if (newNotificationJSON.containsKey(Constants.JSON_FEATURE_FIELD_SEASON_ID) && newNotificationJSON.get(Constants.JSON_FEATURE_FIELD_SEASON_ID) !=null) {
			if (!season_id.equals(newNotificationJSON.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID))) {
				String errMsg = Strings.notificationSeasonWithDifferentId;
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();									
			}
		}
		else {		
			newNotificationJSON.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, season_id);
		}

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();	
		readWriteLock.writeLock().lock();
		try {	
			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			@SuppressWarnings("unchecked")
			Map<String, AirlockNotification> notificationsDB = (Map<String, AirlockNotification>)context.getAttribute(Constants.NOTIFICATIONS_DB_PARAM_NAME);

			//Validate season existence
			Season season = seasonsDB.get(season_id);
			change.setSeason(season);
			if (season == null) {
				String errMsg = Strings.seasonNotFound;
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();				
			}			

			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());

			ValidationResults validationRes = validateNotificationsSupport(env);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			AirlockNotification newNotificationObj = new AirlockNotification();

			validationRes = newNotificationObj.validateNotificationJSON(newNotificationJSON, context, userInfo, season_id);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			newNotificationObj.fromJSON(newNotificationJSON);
			newNotificationObj.setUniqueId(UUID.randomUUID());

			season.getNotifications().addAirlockNotification(newNotificationObj);			

			try {
				change.getFiles().addAll(AirlockFilesWriter.writeSeasonNotifications(season, newNotificationObj.getStage() == Stage.PRODUCTION, context, false));
				Webhooks.get(context).notifyChanges(change, context);
			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}

			notificationsDB.put(newNotificationObj.getUniqueId().toString(), newNotificationObj);

			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Create new notification: " + newNotificationObj.toJson(OutputJSONMode.ADMIN).toString(), userInfo); 

			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FIELD_UNIQUE_ID, newNotificationObj.getUniqueId().toString());
			res.put(Constants.JSON_FIELD_LAST_MODIFIED, newNotificationObj.getLastModified().getTime());

			logger.info("Notification added to season '"+  season_id + "': "+ res.toString());

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Added notification: " + newNotificationObj.toJson(OutputJSONMode.ADMIN));
			}

			return (Response.ok(res.toString())).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error adding notification: ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error adding notification: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}
	
	@GET
	@Path ("/seasons/{season-id}/notifications")
	@ApiOperation(value = "Returns the notifications for the specified season", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getNotifications(@PathParam("season-id")String season_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getNotifications request");
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
		UserInfo userInfo = UserInfo.validate("ProductServices.getNotifications", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);
		
		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.NOTIFICATIONS});
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

			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());

			ValidationResults validationRes = validateNotificationsSupport(env);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			JSONObject res = season.getNotifications().toJson(OutputJSONMode.ADMIN);

			return (Response.ok()).entity(res.toString()).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error getting notifications of season: " + season_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error getting notifications: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}
	
	@PUT
	@Path ("/seasons/{season-id}/notifications")
	@ApiOperation(value = "Update the notifications for the specified season", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response updateNotifications(@PathParam("season-id")String season_id, String notifications,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getNotifications request");
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
		//check user authorization
		UserInfo userInfo = UserInfo.validate("ProductServices.updateNotifications", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);
		
		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.NOTIFICATIONS});
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
				return sendAndLogError(Status.NOT_FOUND, Strings.seasonNotFound);							
			}

			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());

			ValidationResults validationRes = validateNotificationsSupport(env);
			if (validationRes!=null) {
				return sendAndLogError(validationRes.status, validationRes.error);				
			}

			JSONObject updatedNotificationsJSON = null;
			try {
				updatedNotificationsJSON = new JSONObject(notifications);
			} catch (JSONException je) {
				String errMsg = Strings.illegalInputJSON + je.getMessage();
				return sendAndLogError(Status.BAD_REQUEST, errMsg);				
			}		
			
			//if not set - set the seasonId to be the id path param
			if (!updatedNotificationsJSON.containsKey(Constants.JSON_FEATURE_FIELD_SEASON_ID) || updatedNotificationsJSON.get(Constants.JSON_FEATURE_FIELD_SEASON_ID) == null) {
				updatedNotificationsJSON.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, season_id);
			}
			else {
				//verify that season-id in path is identical to seasonId in request pay-load  
				if (!updatedNotificationsJSON.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID).equals(season_id)) {
					String errMsg = Strings.notificationSeasonWithDifferentId;
					logger.severe(errMsg);
					return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();					
				}
			}
			
			
			validationRes = season.getNotifications().validateNotificationsJSON(updatedNotificationsJSON, context, season, userInfo);
			if (validationRes!=null) {
				return sendAndLogError(validationRes.status, validationRes.error);
			}
			
			ValidationResults validateProdDontChangeRes = season.getNotifications().validateProductionDontChanged(updatedNotificationsJSON, context);
			
			//only productLead or Administrator can update order of notifications in production, season schema, season maxNotifications or season minInterval  for season that has notifications in production 
			if (!validRole(userInfo)) {			
				if (validateProdDontChangeRes!=null) {
					return sendAndLogError(validateProdDontChangeRes.status, validateProdDontChangeRes.error);
				}					
			} 
			
			//finally - actually update the experiment.
			String updateDetails = season.getNotifications().updateNotifications(updatedNotificationsJSON, context);

			if (!updateDetails.isEmpty()) {
				boolean writeProductionRuntime = (validateProdDontChangeRes!=null);
				
				try {
					change.getFiles().addAll(AirlockFilesWriter.writeSeasonNotifications(season, writeProductionRuntime, context, false));
					Webhooks.get(context).notifyChanges(change, context);
				} catch (IOException e) {
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
				}
				
				AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
				auditLogWriter.log("Update notifications for season: " + season_id + ",   " + updateDetails, userInfo); 				
			}	
			
			JSONObject res = season.getNotifications().toJson(OutputJSONMode.ADMIN);
			return (Response.ok()).entity(res.toString()).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error updating notifications of season: " + season_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error updating notifications: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}
	
	@GET
	@Path ("/seasons/notifications/{notification-id}")
	@ApiOperation(value = "Returns the specified notification", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Notification not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getNotification(@PathParam("notification-id")String notification_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getNotification request");
		}

		String err = Utilities.validateLegalUUID(notification_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalNotificationUUID + err);

		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfNotification(context, notification_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		
		//check user authorization
		UserInfo userInfo = UserInfo.validate("ProductServices.getNotification", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.NOTIFICATIONS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();	
		
		
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.readLock().lock();
		try {			

			@SuppressWarnings("unchecked")
			Map<String, AirlockNotification> notificationsDB = (Map<String, AirlockNotification>)context.getAttribute(Constants.NOTIFICATIONS_DB_PARAM_NAME);

			AirlockNotification notification = notificationsDB.get(notification_id);

			if (notification == null) {
				String errMsg = Strings.notificationNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);			

			Season season = seasonsDB.get(notification.getSeasonId().toString());

			if (season == null) {
				String errMsg = Strings.seasonNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();				
			}

			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());

			ValidationResults validationRes = validateNotificationsSupport(env);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			return Response.ok(notification.toJson(OutputJSONMode.ADMIN).toString()).build();					
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error getting notification: " + notification_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error getting notification: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}
	
	@DELETE
	@Path ("/seasons/notifications/{notification-id}")
	@ApiOperation(value = "Deletes the specified notification")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Notification not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response deleteNotification(@PathParam("notification-id")String notification_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException  {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("deleteNotification request: notification_id =" + notification_id);
		}
		AirlockChange change = new AirlockChange();
		String err = Utilities.validateLegalUUID(notification_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalNotificationUUID + err);
		
		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfNotification(context, notification_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);
		
		//check user authorization
		UserInfo userInfo = UserInfo.validate("ProductServices.deleteNotification", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);
		
		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.NOTIFICATIONS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();	
				
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();		
		readWriteLock.writeLock().lock();
		try {		

			@SuppressWarnings("unchecked")
			Map<String, AirlockNotification> notificationsDB = (Map<String, AirlockNotification>)context.getAttribute(Constants.NOTIFICATIONS_DB_PARAM_NAME);
			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);
			

			if (!notificationsDB.containsKey(notification_id)) {
				String errMsg = Strings.notificationNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			AirlockNotification notificationToDel = notificationsDB.get(notification_id);			

			Season season = seasonsDB.get(notificationToDel.getSeasonId().toString()); 
			change.setSeason(season);
			if (season == null) {
				String errMsg = String.format(Strings.nonExistingSeason,notificationToDel.getSeasonId().toString());
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();				
			}

			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());			
			
			ValidationResults validationRes = validateNotificationsSupport(env);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			if (notificationToDel.getStage() == Stage.PRODUCTION) {
				String errMsg = Strings.notificationProd;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			String errorString = season.getNotifications().removeAirlockNotification(notificationToDel, season, context);
			if (errorString!=null) {
				logger.severe(errorString);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errorString)).build();
			}

			notificationsDB.remove(notification_id);

			try {
				//writing updated notifications list to S3
				change.getFiles().addAll(AirlockFilesWriter.writeSeasonNotifications(season, notificationToDel.getStage() == Stage.PRODUCTION, context, false));
				Webhooks.get(context).notifyChanges(change, context);
			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}

			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Delete notification: uniqueId: " + notification_id, userInfo); 

			logger.info("Notification " + notificationToDel.getName() + ", " + notification_id + " was deleted");

			return (Response.ok()).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error deleting notification " + notification_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error deleting notification: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}	
	
	@PUT
	@Path ("/seasons/notifications/{notification-id}")
	@ApiOperation(value = "Updates the specified notification", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Stream not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response updateNotification(@PathParam("notification-id")String notification_id, String notification,			
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("updateNotification request: notification_id =" + notification_id + ", notification = " + notification);
		}
		AirlockChange change = new AirlockChange();
		String err = Utilities.validateLegalUUID(notification_id);
		if (err!=null)
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalNotificationUUID + err);
		
		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfNotification(context, notification_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);
		
		//check user authorization
		UserInfo userInfo = UserInfo.validate("ProductServices.updateNotification", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);								

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.NOTIFICATIONS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();	
				
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.writeLock().lock();
		try {

			@SuppressWarnings("unchecked")
			Map<String, AirlockNotification> notificationsDB = (Map<String, AirlockNotification>)context.getAttribute(Constants.NOTIFICATIONS_DB_PARAM_NAME);
			@SuppressWarnings("unchecked")			
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);
			
			AirlockNotification alNotificationToUpdate = notificationsDB.get(notification_id);
			if (alNotificationToUpdate == null) {
				String errMsg = Strings.notificationNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();			
			}			

			JSONObject updatedNotificationJSON = null;
			try {
				updatedNotificationJSON = new JSONObject(notification);
			} catch (JSONException je) {
				String errMsg = Strings.illegalInputJSON + je.getMessage();
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}						

			//if not set - set the uniqueId to be the id path param
			if (!updatedNotificationJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) || updatedNotificationJSON.get(Constants.JSON_FIELD_UNIQUE_ID) == null) {
				updatedNotificationJSON.put(Constants.JSON_FIELD_UNIQUE_ID, notification_id);
			}
			else {
				//verify that stream-id in path is identical to uniqueId in request pay-load  
				if (!updatedNotificationJSON.getString(Constants.JSON_FIELD_UNIQUE_ID).equals(notification_id)) {
					String errMsg = Strings.notificationWithDifferentId;
					logger.severe(errMsg);
					return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();					
				}
			}			

			ValidationResults validationRes = alNotificationToUpdate.validateNotificationJSON(updatedNotificationJSON, context, userInfo, alNotificationToUpdate.getSeasonId().toString());
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			Season season = seasonsDB.get(alNotificationToUpdate.getSeasonId().toString()); //after validate we know the season exists
			change.setSeason(season);
			
			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());			

			validationRes = validateNotificationsSupport(env);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			ValidationResults validateProdDontChangeRes = alNotificationToUpdate.validateProductionDontChanged(updatedNotificationJSON);
			Boolean isProdChange = (validateProdDontChangeRes != null);
			//only productLead or Administrator can update feature in production
			if (!validRole(userInfo)) {
				if (validateProdDontChangeRes!=null && !validateProdDontChangeRes.status.equals(Status.OK)) {
					String errMsg = validateProdDontChangeRes.error;
					logger.severe(errMsg);
					return Response.status(validateProdDontChangeRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
				}					
			}

			//finally - actually update the notification.
			String updateDetails = alNotificationToUpdate.updateNotification(updatedNotificationJSON, season);

			if (!updateDetails.isEmpty()) { //if some fields were changed

				try {
					change.getFiles().addAll(AirlockFilesWriter.writeSeasonNotifications(season, isProdChange, context, false));
					Webhooks.get(context).notifyChanges(change, context);
				} catch (IOException e) {
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
				}

				AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
				auditLogWriter.log("Update notification: " + notification_id + ",   " + updateDetails, userInfo); 								
			}

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Updated notification: " + alNotificationToUpdate.toJson(OutputJSONMode.ADMIN) + "\n updatd details: " + updateDetails);
			}

			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FIELD_UNIQUE_ID, notification_id);
			res.put(Constants.JSON_FIELD_LAST_MODIFIED, alNotificationToUpdate.getLastModified().getTime());
			return (Response.ok(res.toString())).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error updating notification: " + notification_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error updating notification: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}	
	
/*	@GET
	@Path ("/seasons/{season-id}/notificationschema")
	@ApiOperation(value = "Returns the notification schema for the specified season", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getNotificationSchema(@PathParam("season-id")String season_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getNotificationSchema request");
		}

		UserInfo userInfo = UserInfo.validate("ProductServices.getNotificationSchema", context, assertion);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		String err = Utilities.validateLegalUUID(season_id);
		if (err!=null) {
			String errMsg = Strings.illegalSeasonUUID + err;
			logger.severe(errMsg);
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();				
		}

		//capability verification
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, season_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.NOTIFICATIONS});
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

			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());

			ValidationResults validationRes = validateNotificationsSupport(env);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			JSONObject res = season.getNotifications().getNotificationSchemaJson();

			return (Response.ok()).entity(res.toString()).build();
		} finally {
			readWriteLock.readLock().unlock();
		}
	}*/
	/*
	@PUT
	@Path ("/seasons/{season-id}/notificationschema")
	@ApiOperation(value = "Returns the notification schema for the specified season", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response updateNotificationSchema(@PathParam("season-id")String season_id,
			String notificationSchema,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("updateNotificationSchema request. notificationSchema = " + notificationSchema);
		}

		UserInfo userInfo = UserInfo.validate("ProductServices.updateNotificationSchema", context, assertion);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		String err = Utilities.validateLegalUUID(season_id);
		if (err!=null) {
			String errMsg = Strings.illegalSeasonUUID + err;
			logger.severe(errMsg);
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();				
		}

		//capability verification
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, season_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.NOTIFICATIONS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();	
				
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();	
		readWriteLock.writeLock().lock();
		try {

			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);			

			Season season = seasonsDB.get(season_id);

			if (season == null) {
				String errMsg = Strings.seasonNotFound;
				logger.severe(errMsg);
				return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();				
			}

			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());

			ValidationResults validationRes = validateNotificationsSupport(env);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			JSONObject updatedNotificationSchemaJSON = null;
			try {
				updatedNotificationSchemaJSON = new JSONObject(notificationSchema);
			} catch (JSONException je) {
				String errMsg = Strings.illegalInputJSON + je.getMessage();
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}	
			
			//if not set - set the seasonId to be the id path param
			if (!updatedNotificationSchemaJSON.containsKey(Constants.JSON_FEATURE_FIELD_SEASON_ID) || updatedNotificationSchemaJSON.get(Constants.JSON_FEATURE_FIELD_SEASON_ID) == null) {
				updatedNotificationSchemaJSON.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, season_id);
			}
			else {
				//verify that season-id in path is identical to seasonId in request pay-load  
				if (!updatedNotificationSchemaJSON.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID).equals(season_id)) {
					String errMsg = Strings.inputSchemaSeasonWithDifferentId;
					logger.severe(errMsg);
					return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();					
				}
			}
			
			AirlockNotificationsCollection notificationsCollection = season.getNotifications();
			validationRes = notificationsCollection.validateNotificationsJSON(updatedNotificationSchemaJSON, context, season);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			String updateDetails = season.getNotifications().updateConfigurationSchema(updatedNotificationSchemaJSON);
			if (!updateDetails.isEmpty()) {
				try {
					AirlockFilesWriter.writeSeasonNotifications(season, false, context, true);
				} catch (IOException e) {
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
				}
			}

			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, season_id);
			res.put(Constants.JSON_FIELD_LAST_MODIFIED, season.getNotifications().getLastModified().getTime());
			return (Response.ok(res.toString())).build();			
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}*/
	@GET
	@Path ("/seasons/{season-id}/notifications/outputsample")
	@ApiOperation(value = "Returns the notifications output sample for the specified season", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getNotificationsOutputSample(@PathParam("season-id")String season_id, 
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getNotificationsOutputSample request: season_id = " + season_id);
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
		UserInfo userInfo = UserInfo.validate("ProductServices.getNotificationsOutputSample", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification		
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.NOTIFICATIONS});
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

			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());

			ValidationResults validationRes = validateNotificationsSupport(env);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			JSONObject res = null;
			try {
				res = season.getNotifications().generateOutputSample(context);
			} catch (GenerationException e) {
				String errMsg = Strings.failedGeneratingNotificationsOutputSample + e.getMessage();
				logger.severe(errMsg);						
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}
			catch (JSONException je) {
				String errMsg = Strings.invalidJsonGeneratingNotificationsOutputSample + je.getMessage();
				logger.severe(errMsg);						
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			return (Response.ok()).entity(res.toString()).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error getting notification output sample of season: " + season_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error getting notification output sample: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	@GET
	@Path ("/seasons/{season-id}/runtimedefaults")
	@Produces(value = "application/zip")
	@ApiOperation(value = "Returns the runtime defaults as zip file for the specified season ", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getRuntimeDefaults(@PathParam("season-id")String season_id, @PathParam("locales")String locales,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getRuntimeDefaults request. season_id = " + season_id);
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
		UserInfo userInfo = UserInfo.validate("ProductServices.getRuntimeDefaults", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);		

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.FEATURES});
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

			File zipFile;			
			try {
				List<String> localesList = (locales==null || locales.isEmpty())? null : Arrays.asList(locales.split("\\s*,\\s*"));
				localesList = (localesList!=null && localesList.isEmpty())? null : localesList;
				zipFile = Utilities.generateRuntimeDefaultsZip (season, context, localesList);
			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}


			return (Response.ok()).entity(zipFile).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error getting defaults of season: " + season_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error getting defaults: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}	
	Response sendAndLogError(Status status, String err)
	{
		logger.severe(err);
		return Response.status(status).entity(Utilities.errorMsgToErrorJSON(err)).build();
	}
	Response sendError(Status status, String err)
	{
		return Response.status(status).entity(Utilities.errorMsgToErrorJSON(err)).build();
	}
	Response sendInfoError(Status status, UserInfo info)
	{
		return Response.status(status).entity(info.getErrorJson()).build();
	}
	
	//*******************
	//   PURCHAES API
	//*******************
	
	
	@POST
	@Path ("/seasons/{season-id}/branches/{branch-id}/entitlements")
	@ApiOperation(value = "Creates an entitlement item  within the specified season and branch", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season or parent item not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response addEntitlement(@PathParam("season-id")String season_id,
			@PathParam("branch-id")String branch_id,
			@QueryParam("parent") String parent, String newPurchaseItem,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("addEntitlement request: season_id = " + season_id + ", parent = " + parent + ", branch_id = " + branch_id + ", newPurchaseItem = " + newPurchaseItem);
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

		//check user authorization		
		UserInfo userInfo = UserInfo.validate("ProductServices.addEntitlement", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);						

		//capability verification
		AirlockCapability[] requiredCapabilities = getRequiredPurchasesCapabilities(branch_id); //IN_APP_PURCHASES + maybe BRANCHES
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, requiredCapabilities);
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		

		if (parent==null) {
			return sendAndLogError(Status.BAD_REQUEST, Strings.parentMissing);
		}

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.writeLock().lock();
		try {				

			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			@SuppressWarnings("unchecked")
			Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);

			@SuppressWarnings("unchecked")
			Map<String,InternalUserGroups> groupsPerProductMap = (Map<String,InternalUserGroups>) context.getAttribute(Constants.USER_GROUPS_PER_PRODUCT_PARAM_NAME);
			InternalUserGroups userGroups = groupsPerProductMap.get(currentProduct.getUniqueId().toString());

			//Validate season existence
			Season season = seasonsDB.get(season_id);
			change.setSeason(season);
			if (season == null) {
				return sendAndLogError(Status.NOT_FOUND, Strings.seasonNotFound);				
			}

			//validate that the season is from version that supports entitlements
			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());

			ValidationResults validationRes = validatePurchasesSupport(env);
			if (validationRes!=null) {
				return sendAndLogError(validationRes.status, validationRes.error);
			}

			//verify branchId
			validationRes = Utilities.validateBranchId(context, branch_id, season);
			if (validationRes!=null) {
				return sendAndLogError(validationRes.status, validationRes.error);
			}
			boolean inMaster = branch_id.equals(Constants.MASTER_BRANCH_NAME);

			Map<String, BaseAirlockItem> airlockItemsDB = Utilities.getAirlockItemsDB (branch_id, context);			

			if (parent.equals(Constants.ROOT_FEATURE)) {
				parent = season.getEntitlementsRoot().getUniqueId().toString(); //the season's purchases root node id
			}
			else {
				//Validate parent existence + in the given season 
				err = Utilities.validateLegalUUID(parent);
				if (err!=null) {
					return sendAndLogError(Status.BAD_REQUEST, Strings.illegalParentUUID + err);
				}

				BaseAirlockItem parentItem = airlockItemsDB.get(parent);

				if (parentItem == null) {
					return sendAndLogError(Status.NOT_FOUND, Strings.parentNotFound);
				}

				if (!season_id.equals(parentItem.getSeasonId().toString())) {
					return sendAndLogError(Status.BAD_REQUEST, Strings.parentNotFoundInSeason);
				}		
				
				//validate that parent is a node of the purchases tree
				if (!parentItem.inPurchasesTree(season, airlockItemsDB)) {
					return sendAndLogError(Status.BAD_REQUEST, Strings.parentInFeaturesTree);
				}
			}

			//validate that is a legal JSON
			JSONObject newPurchaseItemJSON = null;
			try {
				newPurchaseItemJSON = new JSONObject(newPurchaseItem);  
			} catch (JSONException je) {
				return sendAndLogError(Status.BAD_REQUEST, Strings.illegalInputJSON + je.getMessage());				
			}

			//verify that JSON does not contain uniqueId field
			if (newPurchaseItemJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && newPurchaseItemJSON.get(Constants.JSON_FIELD_UNIQUE_ID) !=null) {
				return sendAndLogError(Status.BAD_REQUEST, Strings.featureWithId);							
			}

			//verify that JSON does not contain different season-id then the path parameter
			if (newPurchaseItemJSON.containsKey(Constants.JSON_FEATURE_FIELD_SEASON_ID) && newPurchaseItemJSON.get(Constants.JSON_FEATURE_FIELD_SEASON_ID) !=null) {
				if (!season_id.equals(newPurchaseItemJSON.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID))) {
					return sendAndLogError(Status.BAD_REQUEST, Strings.featureSeasonWithDifferentId);									
				}
			}
			else {		
				newPurchaseItemJSON.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, season_id);
			}

			//2 validations that are done for baseAirlockItem as well but should be performed here - before all other validations			
			validationRes = preliminaryPurcahseItemJsonValidation(newPurchaseItemJSON);
			if (validationRes!=null) {
				return sendAndLogError(validationRes.status, validationRes.error);
			}

			BaseAirlockItem newAirlockObj = BaseAirlockItem.getAirlockItemByType(newPurchaseItemJSON);
			if (newAirlockObj == null) {
				return sendAndLogError(Status.BAD_REQUEST, Strings.typeNotFound);
			}

			env.setBranchId(branch_id);
			env.setRequestType(Constants.REQUEST_ITEM_TYPE.ENTITLEMENTS);
			env.setAirlockItemsDB(airlockItemsDB);

			validationRes = newAirlockObj.validateFeatureJSON(newPurchaseItemJSON, context, season_id, userGroups, parent, userInfo, airlockItemsDB, env);
			if (validationRes!=null) {
				return sendAndLogError(validationRes.status, validationRes.error);
			}

			newAirlockObj.fromJSON(newPurchaseItemJSON, null, UUID.fromString(parent), env);
			newAirlockObj.setUniqueId(UUID.randomUUID());

			BaseAirlockItem parentObj = airlockItemsDB.get(newAirlockObj.getParent().toString());
			if (inMaster) {
				err = parentObj.addAirlockItem(newAirlockObj);
				if (err!=null) {
					return sendAndLogError(Status.BAD_REQUEST, err);
				}
				parentObj.setLastModified(new Date());

				airlockItemsDB.put(newAirlockObj.getUniqueId().toString(), newAirlockObj);
				if (newAirlockObj instanceof DataAirlockItem) {
					ProductServices.sendMailForAdd (context, airlockItemsDB, parentObj, (DataAirlockItem) newAirlockObj, userInfo, env);
				}
			}
			else {
				//add purchase to branch.				
				Branch branch = branchesDB.get(branch_id);
				change.setBranch(branch);
				err = branch.addPurchase(newAirlockObj, parentObj);
				if (err!=null) {
					return sendAndLogError(Status.BAD_REQUEST, err);
				}
			}

			//writing updated purchases list to S3
			try {
				if(!inMaster) {
					ValidationResults res = Utilities.validateBranchStructure(branch_id, season, context);
					if (res!=null) {
						return sendAndLogError(res.status, res.error);
					}					
				}

				boolean writeProductionPurchases = false;
				Stage changeStage = Stage.DEVELOPMENT;
				if ((newAirlockObj instanceof PurchaseOptionsItem || newAirlockObj instanceof EntitlementItem) && BaseAirlockItem.isProductionFeature(newAirlockObj, airlockItemsDB)) {
					writeProductionPurchases = true;
					changeStage = Stage.PRODUCTION;
				}

				if (inMaster) {
					Pair<String,LinkedList<AirlockChangeContent>> writeRes = AirlockFilesWriter.writePurchases(season, writeProductionPurchases, context, false, env); 
					err = writeRes.getKey();
					change.getFiles().addAll(writeRes.getValue());
				} 
				else {
					Branch branch = branchesDB.get(branch_id);
					change.setBranch(branch);
					change.getFiles().addAll(AirlockFilesWriter.writeBranchPurchases(branch, season, context, env, changeStage));										
					change.getFiles().addAll(AirlockFilesWriter.writeBranchAndMasterRuntimeFiles(season, branch, context, env, writeProductionPurchases));
					change.getFiles().addAll(AirlockFilesWriter.doWriteConstantsFiles(season, context, changeStage));
				}
				Webhooks.get(context).notifyChanges(change, context);

				if (err!=null) {
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(err)).build();					
				}

			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}

			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Create new purchase item: " + newAirlockObj.getNameSpaceDotName() + ", " + newAirlockObj.getUniqueId().toString() + " in branch :"+ branch_id + ": parent = " + parentObj.getNameSpaceDotName() + ", " + parent + ": "+ newAirlockObj.toJson(OutputJSONMode.DISPLAY, context, env).toString(), userInfo); 

			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FIELD_UNIQUE_ID, newAirlockObj.getUniqueId().toString());
			res.put(Constants.JSON_FIELD_LAST_MODIFIED, newAirlockObj.getLastModified().getTime());

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Added purchase item: " + newAirlockObj.toJson(OutputJSONMode.ADMIN, context, env));
			}

			return (Response.ok(res.toString())).build();
		} catch (MergeException e) {
			err = Strings.mergeException  + e.getMessage();
			logger.severe(err);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(err)).build();		
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error adding item: ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error adding item: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}	


	@GET
	@Path ("/seasons/{season-id}/branches/{branch-id}/entitlements")
	@ApiOperation(value = "Returns the entitlements list for the specified season ", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getEntitlements(@PathParam("season-id")String season_id,
			@PathParam("branch-id")String branch_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {		
		//long startTime = System.currentTimeMillis();

		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getEntitlements request");
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
		UserInfo userInfo = UserInfo.validate("ProductServices.getEntitlements", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null) {
			return sendInfoError(Status.UNAUTHORIZED, userInfo);
		}

		//capability verification
		AirlockCapability[] requiredCapabilities = getRequiredPurchasesCapabilities(branch_id); //IN_APP_PURCHASES + maybe BRANCHES
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, requiredCapabilities);
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

			//verify branchId
			ValidationResults validationRes = Utilities.validateBranchId(context, branch_id, season);
			if (validationRes!=null) {
				return sendAndLogError(validationRes.status, validationRes.error);
			}			

			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, season_id);

			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());
			
			//validate that the season is from version that supports entitlements
			validationRes = validatePurchasesSupport(env);
			if (validationRes!=null) {
				return sendAndLogError(validationRes.status, validationRes.error);
			}

			env.setBranchId(branch_id);
			env.setRequestType(Constants.REQUEST_ITEM_TYPE.ENTITLEMENTS);

			try {
				JSONObject rootJSON = doGetPurchases(season, OutputJSONMode.DISPLAY, context, env, userInfo);
				res.put(Constants.JSON_FIELD_ENTITLEMENTS_ROOT, rootJSON);
			} catch (MergeException e) {
				return sendAndLogError(Status.INTERNAL_SERVER_ERROR, Strings.mergeException  +e.getMessage());
			}

			return (Response.ok()).entity(res.toString()).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error getting entitlements of season: " + season_id + " branch " + branch_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error getting entitlements item: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}	

	@GET
	@Path ("/seasons/branches/{branch-id}/entitlements/{entitlement-id}")
	@ApiOperation(value = "Returns the specified entitlement item", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Entitlement item not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getEntitlement(@PathParam("entitlement-id")String entitlement_id,
			@PathParam("branch-id")String branch_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion,
			@QueryParam("includeStrings")Boolean includeStrings) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getEntitlement request");
		}

		String err = Utilities.validateLegalUUID(entitlement_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalPurchaseUUID + err);

		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductForBranchOrFeature(context, branch_id, entitlement_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;

		//check user authorization		
		UserInfo userInfo = UserInfo.validate("ProductServices.getEntitlement", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);		

		if(includeStrings == null){
			includeStrings = false;
		}

		//capability verification
		AirlockCapability[] requiredCapabilities = getRequiredPurchasesCapabilities(branch_id); //IN_APP_PURCHASES + maybe BRANCHES		
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, requiredCapabilities);
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.readLock().lock();
		try {			

			//verify branchId
			ValidationResults validationRes = Utilities.validateBranchId(context, branch_id, null);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			Map<String, BaseAirlockItem> purchaseItemsDB = Utilities.getAirlockItemsDB(branch_id, context);

			BaseAirlockItem pi = purchaseItemsDB.get(entitlement_id);

			if (pi == null) {
				return sendAndLogError(Status.NOT_FOUND, Strings.purchaseItemNotFound);
			}

			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			Season season = seasonsDB.get(pi.getSeasonId().toString());
			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());
			env.setBranchId(branch_id);
			env.setRequestType(Constants.REQUEST_ITEM_TYPE.ENTITLEMENTS);
			
			//validate that the season is from version that supports entitlements
			validationRes = validatePurchasesSupport(env);
			if (validationRes!=null) {
				return sendAndLogError(validationRes.status, validationRes.error);
			}

			if (env.isInMaster()) {
				env.setAnalytics(season.getAnalytics());				
			}
			else { //branch					
				env.setAnalytics(Utilities.getAirlockAnalytics(season, env.getBranchId(), context, purchaseItemsDB, env));				
			}		

			env.setAirlockItemsDB(purchaseItemsDB);

			JSONObject purchaseItemJson = pi.toJson(OutputJSONMode.DISPLAY, context, env,userInfo);
			if(includeStrings && pi instanceof DataAirlockItem){
				JSONArray stringArray = new JSONArray();
				List<OriginalString> copiedStrings = FeatureServices.getStringInUseByAirlockItem(context, pi, false);
				for (int i = 0; i<copiedStrings.size();++i){
					OriginalString currString = copiedStrings.get(i);
					stringArray.add(currString.toJson(Constants.StringsOutputMode.INCLUDE_TRANSLATIONS,seasonsDB.get(currString.getSeasonId().toString())));
				}
				purchaseItemJson.put("strings",stringArray);
			}
			return Response.ok(purchaseItemJson.toString()).build();
		} catch (MergeException e) {
			err = Strings.mergeException  + e.getMessage();
			logger.severe(err);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(err)).build();		
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error getting entitlement item: " + entitlement_id + " from branch " + branch_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error getting purchase item: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	@PUT
	@Path ("/seasons/branches/{branch-id}/entitlements/{entitlement-id}")
	@ApiOperation(value = "Updates the specified entitlement item", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Entitlement item not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response updateEntitlement(@PathParam("entitlement-id")String entitlement_id,
			@PathParam("branch-id")String branch_id,
			String purchaseItem,
			@ApiParam(value="VALIDATE or ACT")@QueryParam("mode")String mode,			
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("updateEntitlement request: entitlement_id =" + entitlement_id + ", branch_id = " + branch_id + ", mode = " + mode +", purchaseItem = " + purchaseItem);
		}
		AirlockChange change = new AirlockChange();
		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductForBranchOrFeature(context, branch_id, entitlement_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);

		//check user authorization		
		UserInfo userInfo = UserInfo.validate("ProductServices.updateEntitlement", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//Temporary for profiling
		logger.info("updatePurchase request: purchase_id =" + entitlement_id + ", branch_id = " + branch_id + ", mode = " + mode +", purchaseItem = " + purchaseItem);

		String err = Utilities.validateLegalUUID(entitlement_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalFeatureUUID + err);

		//if not specified - mode = ACT (actually update the purchase item)
		ActionType actionTypeObj  = Utilities.strToActionType(mode, ActionType.ACT);
		
		if (actionTypeObj==null) {
			return sendAndLogError(Status.BAD_REQUEST, String.format(Strings.illegalMode,mode)  + Constants.ActionType.returnValues());
		}

		long validated, written, total;
		validated = written = total = 0;
		long start = new Date().getTime(); // XXX TEMPORARY

		//capability verification
		AirlockCapability[] requiredCapabilities = getRequiredPurchasesCapabilities(branch_id); //IN_APP_PURCHASES + maybe BRANCHES		
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, requiredCapabilities);
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();			
		readWriteLock.writeLock().lock();
		try {				
			@SuppressWarnings("unchecked")			
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);
			@SuppressWarnings("unchecked")
			Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);

			@SuppressWarnings("unchecked")
			Map<String,InternalUserGroups> groupsPerProductMap = (Map<String,InternalUserGroups>) context.getAttribute(Constants.USER_GROUPS_PER_PRODUCT_PARAM_NAME);
			InternalUserGroups userGroups = groupsPerProductMap.get(currentProduct.getUniqueId().toString());

			//verify branchId
			ValidationResults validationRes = Utilities.validateBranchId(context, branch_id, null);
			if (validationRes!=null) {
				return sendAndLogError(validationRes.status, validationRes.error);
			}

			Map<String, BaseAirlockItem> airlockItemsDB = Utilities.getAirlockItemsDB(branch_id, context);

			BaseAirlockItem purchaseItemToUpdate = airlockItemsDB.get(entitlement_id);
			if (purchaseItemToUpdate == null) {
				return sendAndLogError(Status.NOT_FOUND, Strings.purchaseItemNotFound);			
			}			

			JSONObject updatedPurchaseItemJSON = null;
			try {
				updatedPurchaseItemJSON = new JSONObject(purchaseItem);
			} catch (JSONException je) {
				return sendAndLogError(Status.BAD_REQUEST, Strings.illegalInputJSON + je.getMessage());
			}

			//if not set - set the uniqueId to be the id path parameter
			if (!updatedPurchaseItemJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) || updatedPurchaseItemJSON.get(Constants.JSON_FIELD_UNIQUE_ID) == null) {
				updatedPurchaseItemJSON.put(Constants.JSON_FIELD_UNIQUE_ID, purchaseItem);
			}
			else {
				//verify that purchase-id in path is identical to uniqueId in request pay-load  
				if (!updatedPurchaseItemJSON.getString(Constants.JSON_FIELD_UNIQUE_ID).equals(entitlement_id)) {
					return sendAndLogError(Status.BAD_REQUEST, Strings.featureWithDifferentId);					
				}
			}

			Season season = seasonsDB.get(purchaseItemToUpdate.getSeasonId().toString()); 		
			if (season == null) {
				return sendAndLogError(Status.BAD_REQUEST, String.format(Strings.nonExistingSeason,purchaseItemToUpdate.getSeasonId().toString()));				
			}
			change.setSeason(season);
			
			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());
			env.setBranchId(branch_id);		
			env.setRequestType(Constants.REQUEST_ITEM_TYPE.ENTITLEMENTS);
			env.setAirlockItemsDB(airlockItemsDB);

			//validate that the season is from version that supports entitlements
			validationRes = validatePurchasesSupport(env);
			if (validationRes!=null) {
				return sendAndLogError(validationRes.status, validationRes.error);
			}
	
			validationRes = purchaseItemToUpdate.validateFeatureJSON(updatedPurchaseItemJSON, context, purchaseItemToUpdate.getSeasonId().toString(), userGroups, null, userInfo, airlockItemsDB, env);
			if (validationRes!=null) {
				return sendAndLogError(validationRes.status, validationRes.error);
			}

			boolean inMaster = branch_id.equals(Constants.MASTER_BRANCH_NAME);						
			Branch curBranch = null;
			if (!inMaster) {
				curBranch = branchesDB.get(branch_id);
			}
			change.setBranch(curBranch);

			//If purchaseItem is root or mutual exclusion group, verify that one of its sub-features is not in production and 
			//is changed if you are not permitted (i.e you are not admin or productLead).
			//consider prod under dev as prod
			ValidationResults validateProdDontChangeRes = purchaseItemToUpdate.validateProductionDontChanged(updatedPurchaseItemJSON, airlockItemsDB, curBranch, context, false, env, false);
			Boolean isProdChange = (validateProdDontChangeRes != null);
			//only productLead or Administrator can update purchase in production
			if (!ProductServices.validRole(userInfo)) {
				//the status is ok on standAlone branch when moving from dev to prod
				if (validateProdDontChangeRes!=null && !validateProdDontChangeRes.status.equals(Status.OK)) {
					return sendAndLogError(validateProdDontChangeRes.status, validateProdDontChangeRes.error);
				}					
			} 
			
			AirlockAnalytics mergedAnalytics = Utilities.getAirlockAnalytics(season, branch_id, context, airlockItemsDB, env);
			env.setAnalytics(mergedAnalytics);
			env.setAirlockItemsDB(airlockItemsDB);
			boolean isAnalyticsChanged = purchaseItemToUpdate.isAnalyticsChanged(updatedPurchaseItemJSON, season, context, env);

			if (actionTypeObj == ActionType.VALIDATE) {
				GlobalDataCollection dataColl = null;
				if (env.isInMaster()) {
					dataColl = season.getAnalytics().getGlobalDataCollection();
				}
				else {
					dataColl = curBranch.getAnalytics().getGlobalDataCollection();
				}
				JSONObject res = new JSONObject();

				String deletedPurchaseItemInUseByAnalytics = dataColl.getAnalyticsDataCollection().updatedFeatureInUseByAnalytics(entitlement_id, updatedPurchaseItemJSON, airlockItemsDB, context, season, env,isProdChange);
				if (deletedPurchaseItemInUseByAnalytics!=null) {
					res.put (Constants.JSON_FIELD_WARNING, deletedPurchaseItemInUseByAnalytics);					
				}
				return (Response.ok(res.toString())).build();				
			}

			//Branch curBranch = null;
			if (!inMaster) {
				if (curBranch.getBranchAirlockItemsBD().containsKey(entitlement_id)) { 
					//in branch: take the purchaseItem to update from branch items db since the itemsDb we are using is a clone of the purchases in 
					//the branch and no the purchases themselves. 
					purchaseItemToUpdate = curBranch.getBranchAirlockItemsBD().get(entitlement_id);
				}
				env.setAnalytics(mergedAnalytics);
			}
			else {
				env.setAnalytics(season.getAnalytics());
			}

			//If purchase item is root or mutual exclusion group, verify that one of its sub-items is not in production and 
			//is changed if you are not permitted (i.e you are not admin or productLead).
			//consider prod under dev as dev
			ValidationResults validateProdDontChangeForRuntimeFilesRes = purchaseItemToUpdate.validateProductionDontChanged(updatedPurchaseItemJSON, airlockItemsDB, curBranch, context, true, env, true);
			Boolean writeProduction = (validateProdDontChangeForRuntimeFilesRes != null);

			validated = new Date().getTime();
			//finally - actually update the purchase item.
			//remove deleted configuration attributes (if there are any) from analytics
			Map<String, Stage> updatedBranchesMap = new HashMap<String, Stage> (); //map that keeps the branches that were changed and the stage that need to be written
			List<ChangeDetails> updateDetails = purchaseItemToUpdate.updateAirlockItem(updatedPurchaseItemJSON, airlockItemsDB, airlockItemsDB.get(season.getRoot().getUniqueId().toString()), env, curBranch,isProdChange, context, updatedBranchesMap);

			updateDetails = Utilities.sanitizeChangesList(updateDetails);
			if (!updateDetails.isEmpty()) { //if some fields were changed
				try {
					//boolean productionChange = (validateProdDontChangeRes!=null);
					Set<String> updatedBranches = updatedBranchesMap.keySet();
					if (inMaster) {
						Pair<String,LinkedList<AirlockChangeContent>> writeRes = AirlockFilesWriter.writePurchases (season, writeProduction, context, false, env); //if prod changed - write runtimeProd file 
						err = writeRes.getKey();
						if (err!=null) {
							return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(err)).build();
						}
						change.getFiles().addAll(writeRes.getValue());

						//go over all branches that were changed (for example by purchaseItem name change or if the delta of a checked out purchaseItem was changed)

						for (String updatedBranchId:updatedBranches) {
							//go over all updated branch and validate its structure
							Branch branch = branchesDB.get(updatedBranchId);							
							ValidationResults res = Utilities.validateBranchStructure(branch.getUniqueId().toString(), season, context);
							if (res!=null) {
								return sendAndLogError(res.status, res.error);
							}												
						}

						for (String updatedBranchId:updatedBranches) {
							Branch branch = branchesDB.get(updatedBranchId);							

							change.getFiles().addAll(AirlockFilesWriter.writeBranchPurchases(branch, season, context, env, updatedBranchesMap.get(updatedBranchId)));																	
							change.getFiles().addAll(AirlockFilesWriter.writeBranchAndMasterRuntimeFiles(season, branch, context, env, updatedBranchesMap.get(updatedBranchId).equals(Stage.PRODUCTION)));
						}

						//recalculate analytics counters
						season.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection().calcNumberOfItemsToAnalytics(context, season.getUniqueId(), airlockItemsDB);												
					}
					else {
						ValidationResults res = Utilities.validateBranchStructure(branch_id, season, context);
						if (res!=null) {
							return sendAndLogError(res.status, res.error);
						}											

						Branch branch = branchesDB.get(branch_id);

						if (updatedBranchesMap.containsKey(branch_id)) {
							writeProduction = writeProduction || updatedBranchesMap.get(branch_id).equals(Stage.PRODUCTION);
						}

						change.getFiles().addAll(AirlockFilesWriter.writeBranchPurchases(branch, season, context, env,
								(writeProduction? Stage.PRODUCTION : Stage.DEVELOPMENT)));										


						change.getFiles().addAll(AirlockFilesWriter.writeBranchAndMasterRuntimeFiles(season, branch, context, env, writeProduction));

						//no need to update branch analytics counters since it is never accessed directly - only after analytics merge
					}

					if (isAnalyticsChanged) {
						HashSet<String> updatedBranchesHS = new HashSet<String>(updatedBranches);
						updatedBranchesHS.add(branch_id);
						updatedBranches = updatedBranchesHS;

						//write all the runtime files of the other seasons that are participating in an experiment that its analytics was changed
						change.getFiles().addAll(ProductServices.updateOtherSeasonsRuntimeFiles(updatedBranches, season, branchesDB, writeProduction, context));
					}
					Webhooks.get(context).notifyChanges(change, context);
				} catch (IOException e) {
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
				}
				written = new Date().getTime();
				AuditLogWriter auditLogWriter = (AuditLogWriter) context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
				String purchaseItemType = purchaseItemToUpdate.getObjTypeStrByType();

				String changesMessage = "Update to "+purchaseItemType+": " + purchaseItemToUpdate.getNameSpaceDotName() + ", " + purchaseItemToUpdate.getUniqueId().toString() + " , branch = " + branch_id + ", produced the following changes:\n";
				StringBuilder listUpdateDetails = new StringBuilder(changesMessage);
				for (int i = 0; i < updateDetails.size(); ++i) {
					listUpdateDetails.append(updateDetails.get(i).changeToString());
				}
				auditLogWriter.log(changesMessage + listUpdateDetails.toString(), userInfo);
				String rootCause = "A "+purchaseItemType+" was updated: " + purchaseItemToUpdate.getNameSpaceDotName() + ", " + purchaseItemToUpdate.getUniqueId().toString();
				Utilities.sendEmails(context, rootCause, purchaseItemToUpdate.getUniqueId(), updateDetails, userInfo,airlockItemsDB,env);
			}

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Updated entitlement Item: " + purchaseItemToUpdate.toJson(OutputJSONMode.DISPLAY, context, env) + ". update details: \n" + updateDetails);
			}

			JSONObject res = new JSONObject();
			env.setAirlockItemsDB(airlockItemsDB);
			if (inMaster) {
				env.setAnalytics(season.getAnalytics());
				res = purchaseItemToUpdate.toJson(OutputJSONMode.DISPLAY, context, env,userInfo);				
			} else {
				//after update: the branch tree should be recalculated
				airlockItemsDB = Utilities.getAirlockItemsDB (branch_id, context);			
				BaseAirlockItem f = airlockItemsDB.get(entitlement_id);
				env.setAnalytics(Utilities.getAirlockAnalytics(season, env.getBranchId(), context, airlockItemsDB, env));
				res = f.toJson(OutputJSONMode.DISPLAY, context, env,userInfo);
			}


			return (Response.ok(res.toString())).build();
		} catch (MergeException e) {
			err = Strings.mergeException  + e.getMessage();
			logger.severe(err);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(err)).build();					
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error updating purchase item " + entitlement_id  + " in branch " + branch_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error updating purchase item: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
			total = new Date().getTime();
			logger.info(String.format("purchase item update: validation %d ms, writing %d ms, total %d ms", validated - start, written - validated, total - start)); 								
		}
	}
	
	@DELETE
	@Path ("/seasons/branches/{branch-id}/entitlements/{entitlement-id}")
	@ApiOperation(value = "Deletes the specified entitlement item")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Purchase item not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response deleteEntitlement(@PathParam("entitlement-id")String entitlement_id,
			@PathParam("branch-id")String branch_id,
			@ApiParam(value="VALIDATE or ACT")@QueryParam("mode")String mode,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("ProductServices.deleteEntitlement request: entitlement_id =" + entitlement_id + ", branch_id = " + branch_id + ", mode = " + mode);
		}
		AirlockChange change = new AirlockChange();
		String err = Utilities.validateLegalUUID(entitlement_id);
		if (err!=null) 
			return sendAndLogError(Status.BAD_REQUEST, Strings.illegalFeatureUUID + err);
		
		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductForBranchOrFeature(context, branch_id, entitlement_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);
		
		//check user authorization		
		UserInfo userInfo = UserInfo.validate("ProductServices.deleteEntitlement", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//if not specified - mode = ACT (actually delete the purchase item)
		ActionType actionTypeObj  = Utilities.strToActionType(mode, ActionType.ACT);
		
		if (actionTypeObj==null) {
			return sendAndLogError(Status.BAD_REQUEST, String.format(Strings.illegalMode,mode)  + Constants.ActionType.returnValues());
		}
		
		//capability verification
		AirlockCapability[] requiredCapabilities = getRequiredPurchasesCapabilities(branch_id); //IN_APP_PURCHASES + maybe BRANCHES		
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, requiredCapabilities);
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();
		
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		
		readWriteLock.writeLock().lock();
		try {		
			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);
			@SuppressWarnings("unchecked")
			Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);

			//verify branchId
			ValidationResults validationRes = Utilities.validateBranchId(context, branch_id, null);
			if (validationRes!=null) 
				return sendAndLogError(validationRes.status, validationRes.error);
				
			Map<String, BaseAirlockItem> airlockItemsDB = Utilities.getAirlockItemsDB (branch_id, context);			

			if (!airlockItemsDB.containsKey(entitlement_id)) 
				return sendAndLogError(Status.NOT_FOUND, Strings.purchaseItemNotFound);

			BaseAirlockItem purchaseItemToDel = airlockItemsDB.get(entitlement_id);
			if (purchaseItemToDel.getType() == Type.ROOT) 
				return sendAndLogError(Status.BAD_REQUEST, Strings.deletePurchaseRoot);				

			Season season = seasonsDB.get(purchaseItemToDel.getSeasonId().toString()); 	
			if (season == null) 
				sendAndLogError(Status.BAD_REQUEST, String.format(Strings.nonExistingSeason,purchaseItemToDel.getSeasonId().toString()));
			
			//validate that the season is from version that supports entitlements
			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());

			validationRes = validatePurchasesSupport(env);
			if (validationRes!=null) {
				return sendAndLogError(validationRes.status, validationRes.error);
			}

			change.setSeason(season);

			if (purchaseItemToDel.containSubItemInProductionStage()) {
				return sendAndLogError(Status.BAD_REQUEST, Strings.featureWithSubfeatureProd);
			}
					
			String attachedFeatures = Utilities.featuresAttachedToEntitlement(purchaseItemToDel, season, branch_id, context);
			if (attachedFeatures!=null) {
				return sendAndLogError(Status.BAD_REQUEST, attachedFeatures);
			}
			
			String includedInBundles = Utilities.deletedPurchaseIsIncludedInOtherPurcahse(purchaseItemToDel, season, branch_id, context);
			if (includedInBundles!=null) {
				return sendAndLogError(Status.BAD_REQUEST, includedInBundles);
			}
			
			String entitlementWithoutStoreId = Utilities.deletedPurchaseOptionsCauseEntitlementWithoutStoreId(purchaseItemToDel, season, branch_id, context, airlockItemsDB);
			if (entitlementWithoutStoreId!=null) {
				return sendAndLogError(Status.BAD_REQUEST, entitlementWithoutStoreId);
			}
		
			env.setBranchId(branch_id);			
			env.setAirlockItemsDB(airlockItemsDB);
			env.setRequestType(Constants.REQUEST_ITEM_TYPE.ENTITLEMENTS);

			boolean inMaster = branch_id.equals(Constants.MASTER_BRANCH_NAME);
			try {
				if (actionTypeObj == ActionType.VALIDATE) {
					JSONObject res = new JSONObject();
					String deletedPurchaseInUseByAnalytics = null;
					if (inMaster) {
						deletedPurchaseInUseByAnalytics = season.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection().deletedFeatureInUseByAnalytics(purchaseItemToDel, airlockItemsDB, context, season,userInfo, env);
					}
					else {
						Branch branch = branchesDB.get(branch_id);						
						deletedPurchaseInUseByAnalytics =  branch.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection().deletedFeatureInUseByAnalytics(purchaseItemToDel, airlockItemsDB, context, season,userInfo, env);
					}

					if (deletedPurchaseInUseByAnalytics!=null) {
						res.put (Constants.JSON_FIELD_WARNING, deletedPurchaseInUseByAnalytics);					
					}

					return (Response.ok(res.toString())).build();				
				}

				TreeSet<String> changedBranches = new TreeSet<String>();				
				AirlockAnalytics analytics = null;
				if (inMaster) {
					//map between branch and its merged items db			
					Map<String, Map<String, BaseAirlockItem>> branchAirlockItemsDBsMap = new HashMap<String, Map<String, BaseAirlockItem>>();
					
					//if this purchase item is checked out in a branch - duplicate it in branch and set it in BranchStatus.NEW
					Utilities.duplicateDeletedItemWhenCheckedOutInBranches(purchaseItemToDel, season, context, changedBranches, branchAirlockItemsDBsMap, Constants.REQUEST_ITEM_TYPE.ENTITLEMENTS);

					//If the deleted purchase item is subItems of some purcahse in branch - delete it from its branchFeatureItems/branchConfigItems/branchOrderingRulesItems lists
					Utilities.removeDeletedfromBranchSubItemLists(purchaseItemToDel, season, changedBranches);					
					
					if (purchaseItemToDel.getParent() == null) {
						BaseAirlockItem root = season.getRoot();
						root.removeAirlockItem(purchaseItemToDel.getUniqueId());
					}
					else {
						BaseAirlockItem parent = airlockItemsDB.get(purchaseItemToDel.getParent().toString());
						parent.removeAirlockItem(purchaseItemToDel.getUniqueId());
						parent.setLastModified(new Date());
					}
					
					analytics = season.getAnalytics();
				} else {
					Branch branch = branchesDB.get(branch_id);
					change.setBranch(branch);
					err = branch.deleteFeature(purchaseItemToDel, REQUEST_ITEM_TYPE.ENTITLEMENTS);
					if (err!=null) 
						return sendAndLogError(Status.BAD_REQUEST, err);

					analytics = branch.getAnalytics();
				}

				Stage featureInAnalyticsChangeStage = Stage.DEVELOPMENT;
				try {
					//remove the purchase item and all of its sub items from analytics  
					featureInAnalyticsChangeStage = analytics.getGlobalDataCollection().getAnalyticsDataCollection().removeDeletedFeatureFromAnalytics(purchaseItemToDel, context, season, airlockItemsDB);
				} catch (Exception e) {
					err = Strings.failUpdateAnalytics + e.getMessage();
					logger.severe(err);
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(err)).build();
				}

				//writing updated purchases list to S3
				try {
					if(!inMaster) {
						ValidationResults res = Utilities.validateBranchStructure(branch_id, season, context);
						if (res!=null) 
							return sendAndLogError(res.status, res.error);	
					}
					else {
						if (changedBranches!=null && changedBranches.size()>0) {
							//when deleting purcahse from master - that is checked out in some branches ... 							
							for (String branchId:changedBranches) {
								ValidationResults res = Utilities.validateBranchStructure(branchId, season, context);
								if (res!=null) 
									return sendAndLogError(res.status, res.error);	
							}
						}
					}

					boolean writeProductionPurchases = false;
					if (((purchaseItemToDel instanceof FeatureItem || purchaseItemToDel instanceof ConfigurationRuleItem) && ((DataAirlockItem)purchaseItemToDel).getStage() == Stage.PRODUCTION) ||
							(featureInAnalyticsChangeStage!=null && featureInAnalyticsChangeStage == Stage.PRODUCTION))
						writeProductionPurchases = true; //should not happen - cannot delete purchase item in production
					Stage changeStage = writeProductionPurchases? Stage.PRODUCTION : Stage.DEVELOPMENT;
					
					if (inMaster) {
						Pair<String,LinkedList<AirlockChangeContent>> writeRes = AirlockFilesWriter.writePurchases (season, writeProductionPurchases, context, false, env); 
						err = writeRes.getKey();
						if (err!=null) {
							return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(err)).build();
						}
						change.getFiles().addAll(writeRes.getValue());

						if (changedBranches!=null && changedBranches.size()>0) {
							//when deleting purcahse from master - that is checked out in some branches ... 
							change.getFiles().addAll(AirlockFilesWriter.writeSeasonBranches(season, context, env, changeStage));
							for (String branchId:changedBranches) {
								Branch branch = branchesDB.get(branchId);
								change.setBranch(branch);
								change.getFiles().addAll(AirlockFilesWriter.writeBranchPurchases(branch, season, context, env, changeStage));												
								change.getFiles().addAll(AirlockFilesWriter.writeBranchRuntime(branch, season, context, env, writeProductionPurchases));
								change.getFiles().addAll(AirlockFilesWriter.writeAnalytics(season, context, branch.getUniqueId().toString(), env, changeStage)); //writing analytics since if this purchase item was in analytics - the id was updated								
							}
						}

						//remove deleted purchase item and sub items from purchases  map
						purchaseItemToDel.removeFromAirlockItemsDB(airlockItemsDB, context, userInfo);

						if (featureInAnalyticsChangeStage!=null) {
							season.getAnalytics().getGlobalDataCollection().setLastModified(new Date());
							change.getFiles().addAll(AirlockFilesWriter.writeAnalytics(season, context, Constants.MASTER_BRANCH_NAME,env, changeStage));
						}
					} 
					else {
						Branch branch = branchesDB.get(branch_id);
						change.setBranch(branch);
						change.getFiles().addAll(AirlockFilesWriter.writeBranchPurchases(branch, season, context, env, changeStage));						
						change.getFiles().addAll(AirlockFilesWriter.writeBranchAndMasterRuntimeFiles(season, branch, context, env, writeProductionPurchases));
						change.getFiles().addAll(AirlockFilesWriter.doWriteConstantsFiles(season, context, changeStage));
						if (featureInAnalyticsChangeStage!=null) {
							branch.getAnalytics().getGlobalDataCollection().setLastModified(new Date());
							change.getFiles().addAll(AirlockFilesWriter.writeAnalytics(season, context, branch_id, env, changeStage));
						}							
					}

					if (featureInAnalyticsChangeStage!=null) {
						changedBranches.add(branch_id);							
						
						//write all the runtime files of the other seasons that are participating in an experiment that its analytics was changed
						change.getFiles().addAll(ProductServices.updateOtherSeasonsRuntimeFiles(changedBranches, season, branchesDB, writeProductionPurchases, context));
					}
					Webhooks.get(context).notifyChanges(change, context);

				} catch (IOException e) {
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
				}
			} catch (JSONException je){
				return sendAndLogError(Status.BAD_REQUEST, je.getMessage());	
			}

			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Delete entitlement item: " + purchaseItemToDel.getNameSpaceDotName() + ", " + purchaseItemToDel.getUniqueId().toString() + " from branch :" + branch_id, userInfo); 

			logger.info("Entitlement item " + entitlement_id + " was deleted");

			return (Response.ok()).build();
		} catch (MergeException e) {
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, Strings.mergeException  + e.getMessage());		
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error deleting item " + entitlement_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error deleting item" + entitlement_id + ": " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	private AirlockCapability[] getRequiredPurchasesCapabilities(String branch_id) {
		if (branch_id.equals(Constants.MASTER_BRANCH_NAME)) {
			return new AirlockCapability[]{AirlockCapability.ENTITLEMENTS};
		}
		else {
			return new AirlockCapability[]{AirlockCapability.ENTITLEMENTS, AirlockCapability.BRANCHES};
		}
	}

	//A few validations that are done for baseAirlockItem as well but should be performed here - before all other validations
	private static ValidationResults preliminaryPurcahseItemJsonValidation(JSONObject newFeatureJSON) {
		//sub purchase items are not allowed in add purchase item (adding only one by one)
		try {
			if (!newFeatureJSON.containsKey(Constants.JSON_FEATURE_FIELD_TYPE) || newFeatureJSON.get(Constants.JSON_FEATURE_FIELD_TYPE)==null) {
				return new ValidationResults(Strings.typeMissing, Status.BAD_REQUEST);
			}
			String typeStr = newFeatureJSON.getString(Constants.JSON_FEATURE_FIELD_TYPE);
			Type typeObj = Utilities.valueOf(BaseAirlockItem.Type.class, typeStr);
			if (typeObj == null) {
				return new ValidationResults("Illegal type: '" + typeStr + "'", Status.BAD_REQUEST);
			}

			//validate that only 'purchase' types are passed (not purchase items)
			if (!BaseAirlockItem.isPurchaseItem(typeObj)) {
				return new ValidationResults(String.format(Strings.typeNotSupportedInPurchasesFunction, typeStr), Status.BAD_REQUEST);
			}

			//sub purchase items are not allowed in add purchase item
			if (newFeatureJSON.containsKey(Constants.JSON_FEATURE_FIELD_FEATURES) && newFeatureJSON.get(Constants.JSON_FEATURE_FIELD_FEATURES) != null && !newFeatureJSON.getJSONArray(Constants.JSON_FEATURE_FIELD_FEATURES).isEmpty()) {
				return new ValidationResults(Strings.purchaseItemWithSubfeatures, Status.BAD_REQUEST);
			}

			//sub configuration rules are not allowed in add purchase item
			if (newFeatureJSON.containsKey(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES) && newFeatureJSON.get(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES) != null && !newFeatureJSON.getJSONArray(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES).isEmpty()) {
				return new ValidationResults(Strings.purchaseItemWithConfigurations, Status.BAD_REQUEST);
			}

			//sub ordering rules are not allowed in add purchase item 
			if (newFeatureJSON.containsKey(Constants.JSON_FEATURE_FIELD_ORDERING_RULES) && newFeatureJSON.get(Constants.JSON_FEATURE_FIELD_ORDERING_RULES) != null && !newFeatureJSON.getJSONArray(Constants.JSON_FEATURE_FIELD_ORDERING_RULES).isEmpty()) {
				return new ValidationResults(Strings.purchaseItemWithOrderingRules, Status.BAD_REQUEST);
			}

			//sub purchaseOptions are not allowed in add purchase item  (adding only one by one)
			if (newFeatureJSON.containsKey(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS) && newFeatureJSON.get(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS) != null && !newFeatureJSON.getJSONArray(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS).isEmpty()) {
				return new ValidationResults(Strings.purchaseItemWithPurchaseOptions, Status.BAD_REQUEST);
			}

			//sub entitlemens are not allowed in add purchase item  (adding only one by one)
			if (newFeatureJSON.containsKey(Constants.JSON_FEATURE_FIELD_PURCHASE_OPTIONS) && newFeatureJSON.get(Constants.JSON_FEATURE_FIELD_PURCHASE_OPTIONS) != null && !newFeatureJSON.getJSONArray(Constants.JSON_FEATURE_FIELD_PURCHASE_OPTIONS).isEmpty()) {
				return new ValidationResults(Strings.purchaseItemWithEntitlements, Status.BAD_REQUEST);
			}
		} catch (JSONException je) {
			return new ValidationResults(je.getMessage(), Status.BAD_REQUEST);
		}

		return null;
	}

	private JSONObject doGetPurchases(Season season, OutputJSONMode outputMode, ServletContext context, Environment env, UserInfo userInfo) throws MergeException, JSONException {
		String branchId = env.getBranchId();
		if (branchId.equals(Constants.MASTER_BRANCH_NAME)) {
			RootItem root = season.getEntitlementsRoot();	
			env.setAnalytics(season.getAnalytics());

			@SuppressWarnings("unchecked")
			Map<String, BaseAirlockItem> purchaseItemsDB = (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);

			env.setAirlockItemsDB(purchaseItemsDB);
			return root.toJson(outputMode, context, env, userInfo);
		}
		else { //branch
			//at this stage i know the branch exists - after validate
			@SuppressWarnings("unchecked")
			Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);

			Branch branch = branchesDB.get(branchId);			
			
			BaseAirlockItem branchPurchasesRoot = MergeBranch.merge(season.getEntitlementsRoot(), branch, Constants.REQUEST_ITEM_TYPE.ENTITLEMENTS); 
			Map<String, BaseAirlockItem> airlockPurchasesItemsDB = MergeBranch.getItemMap(branchPurchasesRoot, true);
			
			BaseAirlockItem branchRoot = MergeBranch.merge(season.getRoot(), branch, Constants.REQUEST_ITEM_TYPE.FEATURES); 
			Map<String, BaseAirlockItem> airlockFeaturesItemsDB = MergeBranch.getItemMap(branchRoot, true);
			
			Map<String, BaseAirlockItem> uniteAirlockItemsDB = new HashMap<String, BaseAirlockItem>();
			uniteAirlockItemsDB.putAll(airlockPurchasesItemsDB);
			uniteAirlockItemsDB.putAll(airlockFeaturesItemsDB);
			
			env.setAnalytics(Utilities.getAirlockAnalytics(season, env.getBranchId(), context, uniteAirlockItemsDB, env));
			env.setAirlockItemsDB(uniteAirlockItemsDB);
			
			return branchPurchasesRoot.toJson(outputMode, context, env, userInfo);
		}		
	}

	@PUT
	@Path("/seasons/{season-id}/streams")
	@ApiOperation(value = "Updates global streams settings for the specified season", response = String.class)
	@Produces(value="text/plain")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response updateGlobalStreamsSettings(@PathParam("season-id")String season_id, String globalStreamsSettings, 
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("updateGlobalStreamsSettings request: season_id = " + season_id + ", globalStreamsSettings = " + globalStreamsSettings);
		}
		AirlockChange change = new AirlockChange();
		String err = Utilities.validateLegalUUID(season_id);
		if (err!=null) 
			return logReply(Status.BAD_REQUEST, Strings.illegalSeasonUUID + err);
		
		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfBranchOrSeason(context, null, season_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);
		
		//check user authorization
		UserInfo userInfo = UserInfo.validate("ProductServices.updateGlobalStreamsSettings", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);		

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.STREAMS});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();	
				
		//validate that is a legal JSON
		JSONObject newGlobalStremSettingsJSON = null;
		try {
			newGlobalStremSettingsJSON = new JSONObject(globalStreamsSettings);  
		} catch (JSONException je) {
			return logReply(Status.BAD_REQUEST, Strings.illegalSeasonUUID + Strings.illegalInputJSON + je.getMessage());				
		}

		//verify that JSON does not contain different season-id then the path parameter
		if (newGlobalStremSettingsJSON.containsKey(Constants.JSON_FEATURE_FIELD_SEASON_ID) && newGlobalStremSettingsJSON.get(Constants.JSON_FEATURE_FIELD_SEASON_ID) !=null) {
			if (!season_id.equals(newGlobalStremSettingsJSON.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID))) {
				return logReply(Status.BAD_REQUEST, Strings.streamSeasonWithDifferentId);
			}
		}
		else {		
			newGlobalStremSettingsJSON.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, season_id);
		}
		
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();				
		readWriteLock.writeLock().lock();
		try {	
			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			//Validate season existence
			Season season = seasonsDB.get(season_id);
			change.setSeason(season);
			
			if (season == null) {
				return logReply(Status.BAD_REQUEST, Strings.seasonNotFound);
			}			

			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());

			ValidationResults validationRes = validateStreamsSupport(env);
			if (validationRes!=null) {
				return logReply(validationRes.status, validationRes.error);
			}

			validationRes = season.getStreams().validateGlobalStreamsSettingsJSON (newGlobalStremSettingsJSON, context, season_id);
			if (validationRes!=null) {
				return logReply(validationRes.status, validationRes.error);
			}

			//finally - actually update the global streams settings.
			String updateDetails = season.getStreams().updateGlobalStreamsSettings(newGlobalStremSettingsJSON, season);
			
			if (!updateDetails.isEmpty()) { //if some fields were changed

				try {
					change.getFiles().addAll(AirlockFilesWriter.writeSeasonStreams(season, true, context));	//production change				
					Webhooks.get(context).notifyChanges(change, context);
				} catch (IOException e) {
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
				}

				AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
				auditLogWriter.log("Update global streams settings for season: " + season_id + ",   " + updateDetails, userInfo);
			}
			
			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Update global streams settings for season: " + season_id + ",   " + updateDetails);
			}

			JSONObject res = season.getStreams().toJson(OutputJSONMode.DISPLAY);
			return (Response.ok()).entity(res.toString(true)).build();
			//return (Response.ok(res.toString(true))).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error setting streams events of season: " + season_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error setting streams events: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}
	
	@GET
	@Path ("/seasons/{season-id}/branchesusage")
	@ApiOperation(value = "Returns the branches usages details for the specified season", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getBranchesUsage(@PathParam("season-id")String season_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getBranchesUsage request");
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
		UserInfo userInfo = UserInfo.validate("ProductServices.getBranches", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new AirlockCapability[]{AirlockCapability.BRANCHES});
		if (capabilityValidationRes!=null) 
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();		
		
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.readLock().lock();
		try {

			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);			
			Season season = seasonsDB.get(season_id);
			if (season == null) {
				return logReply(Status.NOT_FOUND, Strings.seasonNotFound);					
			}

			JSONObject res = season.getBranches().toBranchesUsageJson(context);
			return (Response.ok()).entity(res.toString()).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error getting branches usage of season " + season_id + ": ", e);
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error getting branches usage: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}
	
	private ValidationResults validatePurchasesSupport (Environment env) {
		if (isPurchasesSupported(env))  //only seasons from 5.5 and up supports purchases
			return null;

		String errMsg = Strings.entitlementsNotSupported;
		logger.severe(errMsg);
		return new ValidationResults(errMsg, Status.BAD_REQUEST);
	}

	
	public static boolean isPurchasesSupported (Environment env) {
		Version version = env.getVersion();
		return version.i >= Version.v5_5.i;  //only seasons from version 5.5 and up supports purcahses
	}

}
