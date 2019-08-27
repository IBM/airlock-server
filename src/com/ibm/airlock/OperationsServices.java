package com.ibm.airlock;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
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

import com.ibm.airlock.admin.serialize.AirlockFilesWriter;
import com.ibm.airlock.admin.serialize.AuditLogWriter;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponses;

import com.ibm.airlock.utilities.Pair;

import com.wordnik.swagger.annotations.ApiResponse;
import com.ibm.airlock.Constants.APIKeyOutputMode;
import com.ibm.airlock.Constants.AirlockCapability;
import com.ibm.airlock.Constants.RoleType;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.ValidationResults;
import com.ibm.airlock.admin.operations.Webhook;
import com.ibm.airlock.admin.Utilities.ProductErrorPair;
import com.ibm.airlock.admin.authentication.UserInfo;
import com.ibm.airlock.admin.authentication.UserRoles;
import com.ibm.airlock.admin.operations.AirlockAPIKey;
import com.ibm.airlock.admin.operations.AirlockAPIKeys;
import com.ibm.airlock.admin.operations.AirlockCapabilities;
import com.ibm.airlock.admin.operations.AirlockChange;
import com.ibm.airlock.admin.operations.AirlockChangeContent;
import com.ibm.airlock.admin.operations.UserRoleSets;
import com.ibm.airlock.admin.operations.Roles;
import com.ibm.airlock.admin.operations.UserRoleSets.UserRoleSet;
import com.ibm.airlock.admin.operations.Webhooks;
import com.ibm.airlock.admin.Product;
import com.ibm.airlock.admin.Season;

@Path ("/ops")
@Api(value = "/Operations", description = "Operations management API")
public class OperationsServices {

	public static final Logger logger = Logger.getLogger(OperationsServices.class.getName());

	@Context
	private ServletContext context;

	@GET
	@Path("/roles")
	@ApiOperation(value = "Returns all roles", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getRoles(@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getRoles request");
		}

		// use userInfo for more stringent checks. null if authorization is off
		UserInfo userInfo = UserInfo.validate("OperationsServices.getRoles", context, assertion, null);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
		readWriteLock.readLock().lock(); 
		try {
			Roles roles = (Roles)context.getAttribute(Constants.ROLES_PARAM_NAME);		
	
			JSONObject res = roles.toJson();
			
			return (Response.ok(res.toString())).build();	
		} finally {
			readWriteLock.readLock().unlock();
		}
	}		

	@GET
	@Path("/userrolesets")
	@ApiOperation(value = "Returns glabal  user roles sets", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getUserRoleSets(@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getAirlockUsers request");
		}

		UserInfo userInfo = UserInfo.validate("OperationsServices.getUserRoleSets", context, assertion, null);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
		readWriteLock.readLock().lock(); 

		try {				
			UserRoleSets globalUsers = (UserRoleSets)context.getAttribute(Constants.AIRLOCK_GLOBAL_USERS_PARAM_NAME); 					
			JSONObject res = globalUsers.toJSON();				
			return (Response.ok(res.toString())).build();	
		} finally {
			readWriteLock.readLock().unlock();
		}
	}
	
	@GET
	@Path("/userrolesets/{roleset-id}")
	@ApiOperation(value = "Returns the specified user role set.", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Airlock user not found"),			
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getUserRoleSet(@PathParam("roleset-id")String user_id, 
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getUserRoleSet request");
		}

		String err = Utilities.validateLegalUUID(user_id);
		if (err!=null) 
			return logReply(Status.BAD_REQUEST, Strings.illegalUserUUID + err);

		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfAirlockUser(context, user_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		
		//check user authorization
		UserInfo userInfo = null;
		ReentrantReadWriteLock readWriteLock = null;
		if (currentProduct == null) { //global User
			userInfo = UserInfo.validate("OperationsServices.getUserRoleSet", context, assertion, null);
			readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
			
		}
		else { //product user
			userInfo = UserInfo.validate("OperationsServices.getUserRoleSet", context, assertion, currentProduct);
			readWriteLock = currentProduct.getProductLock();			
		}
		
		if (userInfo != null && userInfo.getErrorJson() != null) {
			return sendInfoError(Status.UNAUTHORIZED, userInfo);
		}
		
		readWriteLock.readLock().lock();
		try {			
			@SuppressWarnings("unchecked")
			Map<String, UserRoleSet> usersDB = (Map<String, UserRoleSet>)context.getAttribute(Constants.AIRLOCK_USERS_DB_PARAM_NAME);
			UserRoleSet user = usersDB.get(user_id);

			if (user == null) {
				logReply(Status.NOT_FOUND, Strings.airlockUserNotFound);
			}

			return Response.ok(user.toJSON().toString()).build();					
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	@DELETE
	@Path("/userrolesets/{roleset-id}")
	@ApiOperation(value = "Delete the specified user role set.", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Airlock user not found"),			
			@ApiResponse(code = 500, message = "Internal error") })
	public Response deleteUserRoleSet(@PathParam("roleset-id")String user_id, 
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("deleteUserRoleSet request");
		}

		String err = Utilities.validateLegalUUID(user_id);
		if (err!=null) 
			return logReply(Status.BAD_REQUEST, Strings.illegalUserUUID + err);
		AirlockChange change = new AirlockChange();
		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfAirlockUser(context, user_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);
		//check user authorization
		UserInfo userInfo = null;
		ReentrantReadWriteLock readWriteLock = null;
		if (currentProduct == null) { //global airlock user
			userInfo = UserInfo.validate("OperationsServices.deleteUserRoleSet", context, assertion, null);
			readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);	
		}
		else { //product user			
			userInfo = UserInfo.validate("OperationsServices.deleteUserRoleSet", context, assertion, currentProduct);
			readWriteLock = currentProduct.getProductLock();			
		}
		
		if (userInfo != null && userInfo.getErrorJson() != null) {
			return sendInfoError(Status.UNAUTHORIZED, userInfo);
		}
		
		readWriteLock.writeLock().lock();
		try {			
			@SuppressWarnings("unchecked")
			Map<String, UserRoleSet> usersDB = (Map<String, UserRoleSet>)context.getAttribute(Constants.AIRLOCK_USERS_DB_PARAM_NAME);
			UserRoleSet userToDel = usersDB.get(user_id);

			if (userToDel == null) {
				logReply(Status.NOT_FOUND, Strings.airlockUserNotFound);
			}

			UserRoleSets airlockUsers = null;
			if (currentProduct == null) { //global user
				airlockUsers = (UserRoleSets)context.getAttribute(Constants.AIRLOCK_GLOBAL_USERS_PARAM_NAME);
				String userProducts = Utilities.listProductsInWhichUserHasPermissions(context, userToDel.getUserIdentifier());
				if (!userProducts.isEmpty()) {
					return logReply(Status.BAD_REQUEST, String.format(Strings.userInProducts, userProducts));
				}
			
			}
			else { //product user				
				airlockUsers = currentProduct.getProductUsers();
			}
			
			airlockUsers.removeUser(userToDel);			
			usersDB.remove(user_id);
			
			String updatedKeysDetails = null;
			//update relevant userRoles object 
			if (currentProduct !=null) {
				@SuppressWarnings("unchecked")
				Map<String,UserRoles> rolesPerProductMap = (Map<String,UserRoles>) context.getAttribute(Constants.USER_ROLES_PER_PRODUCT_PARAM_NAME);
				
				UserRoles productUserRoles = rolesPerProductMap.get(currentProduct.getUniqueId().toString());
				productUserRoles.removeUser(userToDel, airlockUsers);
			}
			else { //global user
				UserRoles userRoles = (UserRoles)context.getAttribute(Constants.USER_ROLES);
				userRoles.removeUser(userToDel, airlockUsers);
			}
			
			//update keys with deleted roleSets
			AirlockAPIKeys apiKeys = (AirlockAPIKeys)context.getAttribute(Constants.API_KEYS_PARAM_NAME);
			updatedKeysDetails = apiKeys.updateApiKeysWithNewUsersRoles(context, currentProduct, userToDel.getUserIdentifier());
			
			try {
				if (currentProduct == null) { //global User
					change.getFiles().addAll(AirlockFilesWriter.writeGlobalAirlockUsers(airlockUsers.toJSON(), context));
					if (updatedKeysDetails!=null) {
						change.getFiles().addAll(AirlockFilesWriter.writeAirlockApiKeys(context));
					}
				}
				else { //product user
					change.getFiles().addAll(AirlockFilesWriter.writeProductUsers(currentProduct, context));
				}
				
			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}
			
			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Delete user role set: uniqueId: " + user_id + ", identifier: " + userToDel.getUserIdentifier() + ", from product: " +  userToDel.getProductId(), userInfo); 

			if (updatedKeysDetails!=null && !updatedKeysDetails.isEmpty()) {
				auditLogWriter.log("Updated api keys: " + updatedKeysDetails, userInfo); 
			}
			Webhooks.get(context).notifyChanges(change, context);
			logger.info("User role set " + userToDel.getUserIdentifier() + ", " + user_id + " was deleted");
					
			return (Response.ok()).build();
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	@PUT
	@Path("/userrolesets/{roleset-id}")
	@ApiOperation(value = "Update the specified user role set.", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "User role set not found"),			
			@ApiResponse(code = 500, message = "Internal error") })
	public Response updateUserRoleSet(@PathParam("roleset-id")String user_id, String updatedUser,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("updateUserRoleSet request");
		}

		String err = Utilities.validateLegalUUID(user_id);
		if (err!=null) 
			return logReply(Status.BAD_REQUEST, Strings.illegalUserUUID + err);
		AirlockChange change = new AirlockChange();
		//validate that is a legal JSON
		JSONObject updatedUserJSON = null;
		try {
			updatedUserJSON = new JSONObject(updatedUser);  
		} catch (JSONException je) {
			return logReply(Status.BAD_REQUEST, Strings.illegalInputJSON + je.getMessage());
		}
		
		//find relevant product
		ProductErrorPair productErrorPair = Utilities.getProductOfAirlockUser(context, user_id);
		if (productErrorPair.error != null) {
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);
		
		//check user authorization
		UserInfo userInfo = null;
		ReentrantReadWriteLock readWriteLock = null;
		if (currentProduct == null) { //global User role set 
			userInfo = UserInfo.validate("OperationsServices.updateUserRoleSet", context, assertion, null);
			readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);	
		}
		else { //product user			
			userInfo = UserInfo.validate("OperationsServices.updateUserRoleSet", context, assertion, currentProduct);
			readWriteLock = currentProduct.getProductLock();			
		}
		
		if (userInfo != null && userInfo.getErrorJson() != null) {
			return sendInfoError(Status.UNAUTHORIZED, userInfo);
		}
		
		readWriteLock.writeLock().lock();
		try {			
			@SuppressWarnings("unchecked")
			Map<String, UserRoleSet> usersDB = (Map<String, UserRoleSet>)context.getAttribute(Constants.AIRLOCK_USERS_DB_PARAM_NAME);
			UserRoleSet userToUpdate = usersDB.get(user_id);

			if (userToUpdate == null) {
				return logReply(Status.NOT_FOUND, Strings.airlockUserNotFound);
			}

			//if not set - set the uniqueId to be the id path param
			if (!updatedUserJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) || updatedUserJSON.get(Constants.JSON_FIELD_UNIQUE_ID) == null) {
				updatedUserJSON.put(Constants.JSON_FIELD_UNIQUE_ID, user_id);
			}
			else {
				//verify that roleset-id in path is identical to uniqueId in request pay-load  
				if (!updatedUserJSON.getString(Constants.JSON_FIELD_UNIQUE_ID).equals(user_id)) {
					return logReply(Status.BAD_REQUEST, Strings.userWithDifferentId);					
				}
			}			

			ValidationResults validationRes = userToUpdate.validateAirlockUser(updatedUserJSON, context);
			if (validationRes!=null) {
				return logReply(validationRes.status, validationRes.error);
			}
			updatedUserJSON = userToUpdate.setRolesListByHigherPermission(updatedUserJSON);
			
			//finally - actually update the user.
			String updateDetails = userToUpdate.updateAirlockUser(updatedUserJSON);

			String updatedKeysDetails = null;
			if (!updateDetails.isEmpty()) { //if some fields were changed

				if (updateDetails.contains("roles") || updateDetails.contains("isGroupRepresentation") ) {
					//update relevant userRoles object 
					if (currentProduct !=null) {
						@SuppressWarnings("unchecked")
						Map<String,UserRoles> rolesPerProductMap = (Map<String,UserRoles>) context.getAttribute(Constants.USER_ROLES_PER_PRODUCT_PARAM_NAME);
						
						UserRoles productUserRoles = rolesPerProductMap.get(currentProduct.getUniqueId().toString());
						productUserRoles.resetUsersRoles(currentProduct.getProductUsers());
					}
					else {
						UserRoleSets globalUsers = (UserRoleSets) context.getAttribute(Constants.AIRLOCK_GLOBAL_USERS_PARAM_NAME);
						UserRoles userRoles = (UserRoles)context.getAttribute(Constants.USER_ROLES);
						userRoles.resetUsersRoles(globalUsers);
						
					}
					
					//update roles in api keys
					AirlockAPIKeys apiKeys = (AirlockAPIKeys)context.getAttribute(Constants.API_KEYS_PARAM_NAME);
					updatedKeysDetails = apiKeys.updateApiKeysWithNewUsersRoles(context, currentProduct, userToUpdate.getUserIdentifier());
				}
							
				try {
					if (currentProduct == null) { //global User
						UserRoleSets airlockUsers = (UserRoleSets)context.getAttribute(Constants.AIRLOCK_GLOBAL_USERS_PARAM_NAME);
						change.getFiles().addAll(AirlockFilesWriter.writeGlobalAirlockUsers(airlockUsers.toJSON(), context));
						
						if (updatedKeysDetails!=null) {
							change.getFiles().addAll(AirlockFilesWriter.writeAirlockApiKeys(context));
						}
					}
					else { //product user
						change.getFiles().addAll(AirlockFilesWriter.writeProductUsers(currentProduct, context));
					}
					
					AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
					auditLogWriter.log("Update User role set: " + user_id + ",   " + updateDetails, userInfo); 								

					if (updatedKeysDetails!=null && !updatedKeysDetails.isEmpty()) {
						auditLogWriter.log("Updated api keys: " + updatedKeysDetails, userInfo); 
					}
					Webhooks.get(context).notifyChanges(change, context);
				} catch (IOException e) {
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
				}
			}
			
			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Updated user role set : " + userToUpdate.toJSON() + "\n updatd details: " + updateDetails);
			}

			JSONObject res = userToUpdate.toJSON();
			return (Response.ok(res.toString())).build();

		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	@GET
	@Path("/seasons/{season-id}/capabilities")
	@ApiOperation(value = "Returns all capabilities for the specified season", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getSeasonCapabilities(@PathParam("season-id")String season_id, 
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getSeasonCapabilities request, season-id " + season_id);
		}

		String err = Utilities.validateLegalUUID(season_id);
		if (err!=null) 
			return logReply(Status.BAD_REQUEST, Strings.illegalSeasonUUID + err);


		//find relevant product
		ProductErrorPair prodErrPair = Utilities.getProductOfBranchOrSeason(context, null, season_id);
		if (prodErrPair.error != null) {			
			return Response.status(Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		Product currentProduct = prodErrPair.product;
		
		//check user authorization			
		UserInfo userInfo = UserInfo.validate("OperationsServices.getSeasonCapabilities", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.readLock().lock(); 
		try {
			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);		

			Season season = seasonsDB.get(season_id);

			if (season == null)
				return logReply(Status.NOT_FOUND, Strings.seasonNotFound);
			
			Set<AirlockCapability> seasonCapabilities = season.getSeasonCapabilities(context);
			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FIELD_UNIQUE_ID, season_id);
			res.put(Constants.JSON_FIELD_CAPABILITIES, Utilities.capabilitieslistToJsonArray(seasonCapabilities));
			
			return (Response.ok(res.toString())).build();	
		} finally {
			readWriteLock.readLock().unlock();
		}
	}	
	
	@POST
	@Path("/airlockuser/roles")
	@ApiOperation(value = "Returns all roles for the specified user", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getAirlockRolesForUser(String userJson, @HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException
	{
		if (logger.isLoggable(Level.FINEST))
			logger.finest("getAirlockRolesForUser request");

		UserInfo userInfo = UserInfo.validate("OperationsServices.getAirlockRolesForUser", context, assertion, null);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		/*if (userInfo == null)
			return Response.status(Status.BAD_REQUEST).entity("server is not using authentication").build(); // we need a securifyFilter with roles to process this request
		if (userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);
*/
		JSONObject json;
		try {
			json = new JSONObject(userJson);  
		} catch (JSONException je) {
			return logReply(Status.BAD_REQUEST, Strings.illegalInputJSON + je.getMessage());
		}

		if (!json.containsKey(Constants.JSON_FIELD_USER) || json.get(Constants.JSON_FIELD_USER) == null || json.getString(Constants.JSON_FIELD_USER).isEmpty() ) {	
			return logReply(Status.BAD_REQUEST, "missing 'user' in input json");
			
		}
		ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
		readWriteLock.readLock().lock(); 
		try {
			UserRoles ur = UserRoles.get(context);
			String user = json.getString(Constants.JSON_FIELD_USER); // usually an email address. 
			Set<RoleType> roles = ur.getUserRoles(user);

			JSONObject res = new JSONObject();
			res.put(Constants.ROLES_PARAM_NAME, Utilities.roleTypeslistToJsonArray(roles));

			return (Response.ok(res.toString())).build();	
		}
		catch (Exception e) {
			return logReply(Status.BAD_REQUEST, e.getMessage());
		}
		finally {
			readWriteLock.readLock().unlock();
		}
	}	
	
	///
	@POST
	@Path("/userrolesets/user")
	@ApiOperation(value = "Returns all role sets for the specified user", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getAllRoleSetsForUser(String userJson, @HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException
	{
		if (logger.isLoggable(Level.FINEST))
			logger.finest("getAllRoleSetsForUser request");

		UserInfo userInfo = UserInfo.validate("OperationsServices.getAllRoleSetsForUser", context, assertion, null);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		JSONObject json;
		try {
			json = new JSONObject(userJson);  
		} catch (JSONException je) {
			return logReply(Status.BAD_REQUEST, Strings.illegalInputJSON + je.getMessage());
		}

		if (!json.containsKey(Constants.JSON_FIELD_IDENTIFIER) || json.get(Constants.JSON_FIELD_IDENTIFIER) == null  || json.getString(Constants.JSON_FIELD_IDENTIFIER).isEmpty() )
			return logReply(Status.BAD_REQUEST, "missing 'identifier' in input json");

		ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
		readWriteLock.readLock().lock(); 
		try {
			String userIdentifier = json.getString(Constants.JSON_FIELD_IDENTIFIER); // usually an email address. 
			
			JSONObject res = Utilities.buildRoleSetsListForUser(context, userIdentifier);
		
			return (Response.ok(res.toString())).build();	
		}
		catch (Exception e) {
			return logReply(Status.BAD_REQUEST, e.getMessage());
		}
		finally {
			readWriteLock.readLock().unlock();
		}
	}			

	///
	
	@POST
	@Path("/products/{product-id}/airlockuser/roles")
	@ApiOperation(value = "Returns all roles for the specified user and product", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getProductAirlockRolesForUser(@PathParam("product-id")String product_id, 
			String userJson, @HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getProductAirlockRolesForUser request, product-id " + product_id);
		}

		String err = Utilities.validateLegalUUID(product_id);
		if (err!=null) 
			return logReply(Status.BAD_REQUEST, Strings.illegalProductUUID + err);

		//find relevant product
		ProductErrorPair prodErrPair = Utilities.getProduct(context, product_id);
		if (prodErrPair.error != null) {			
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		Product currentProduct = prodErrPair.product;
		
		//check user authorization			
		UserInfo userInfo = UserInfo.validate("OperationsServices.getProductAirlockRolesForUser", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		JSONObject json;
		try {
			json = new JSONObject(userJson);  
		} catch (JSONException je) {
			return logReply(Status.BAD_REQUEST, Strings.illegalInputJSON + je.getMessage());
		}

		if (!json.containsKey(Constants.JSON_FIELD_USER) || json.get(Constants.JSON_FIELD_USER) == null || json.getString(Constants.JSON_FIELD_USER).isEmpty() ) {
			return logReply(Status.BAD_REQUEST, "missing 'user' in input json");
		}

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.readLock().lock(); 		

		try {
			@SuppressWarnings("unchecked")
			Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);		

			Product prod = productsDB.get(product_id);

			if (prod == null) {
				return logReply(Status.NOT_FOUND, Strings.productNotFound);
			}
			
			@SuppressWarnings("unchecked")
			Map<String,UserRoles> userRolesPerProductMap = (Map<String,UserRoles>) context.getAttribute(Constants.USER_ROLES_PER_PRODUCT_PARAM_NAME);
			
			UserRoles userRoles = userRolesPerProductMap.get(product_id);
			if (userRoles == null) {
				//if the system is not configured with authentication - use the airlock users to find the user's roles
				if ((Boolean)context.getAttribute(Constants.SKIP_AUTHENTICATION_PARAM_NAME)) {
					Roles roles = (Roles) context.getAttribute(Constants.ROLES_PARAM_NAME);
					userRoles = new UserRoles(roles, prod.getProductUsers());					
				}
				else {
					userRoles = (UserRoles)context.getAttribute(Constants.USER_ROLES);
				}
			}
			String user = json.getString(Constants.JSON_FIELD_USER); // usually an email address. 
			
			Set<RoleType> roles = userRoles.getUserRoles(user);

			JSONObject res = new JSONObject();
			res.put(Constants.ROLES_PARAM_NAME, Utilities.roleTypeslistToJsonArray(roles));

			return (Response.ok(res.toString())).build();	
		}
		catch (Exception e) {
			return logReply(Status.BAD_REQUEST, e.getMessage());
		}
		finally {
			readWriteLock.readLock().unlock();
		}
	}			

	@PUT
	@Path("/roles")
	@ApiOperation(value = "Updates all roles", response = String.class)
	@Produces(value="text/plain")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response setRoles(String roles, @HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {

		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("setRoles request, roles = " + roles);
		}
		AirlockChange change = new AirlockChange();
		UserInfo userInfo = UserInfo.validate("OperationsServices.setRoles", context, assertion, null);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
		readWriteLock.writeLock().lock();
		try {
			Roles rolesObj = (Roles)context.getAttribute(Constants.ROLES_PARAM_NAME);		
			
			//validate that is a legal JSON
			JSONObject rolesJSON = null;
			try {
				rolesJSON = new JSONObject(roles);
			} catch (JSONException je) {
				return Response.status(Status.BAD_REQUEST).entity(Strings.illegalInputJSON + je.getMessage()).build();
			}

			ValidationResults validationRes = rolesObj.validateRolesJSON(rolesJSON);
			if (validationRes!=null)
				return Response.status(validationRes.status).entity(validationRes.error).build();

			rolesObj.fromJSON(rolesJSON);
			rolesObj.setLastModified(new Date());
			AirLockContextListener.reloadUserRoles(context);
	
			//writing updated roles to S3
			try {
				change.getFiles().addAll(AirlockFilesWriter.writeRoles(rolesJSON, context));				
			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
			}
			Webhooks.get(context).notifyChanges(change, context);

			return Response.ok().build();
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	@GET
	@Path("/healthcheck")
	@Produces(value="text/plain")
	@ApiOperation(value = "Checks that the service is working properly.", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response healthCheck() {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("healthCheck request");
		}

		return (Response.ok()).build();
	}	
	
	@GET
	@Path("/about")
	@ApiOperation(value = "About", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response about(@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("about request");
		}

		// use userInfo for more stringent checks. null if authorization is off
		UserInfo userInfo = UserInfo.validate("OperationsServices.about", context, assertion, null);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
		readWriteLock.readLock().lock(); 
		try {
			InputStream input = context.getResourceAsStream("/META-INF/MANIFEST.MF");
			Manifest manifest = new Manifest(input);
	        Attributes attributes = manifest.getMainAttributes();
	        String buildNum = attributes.getValue(Constants.MANIFEST_ATT_BUILD_NUM);
	        String buildDate = attributes.getValue(Constants.MANIFEST_ATT_BUILD_DATE);
	        String prodName = attributes.getValue(Constants.MANIFEST_ATT_PRODUCT_NAME);
				      
			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FIELD_BUILD_NUM, buildNum);
			res.put(Constants.JSON_FIELD_BUILD_DATE, buildDate);
			res.put(Constants.JSON_FIELD_PRODUCT, prodName);			
			
			return (Response.ok(res.toString())).build();	
		} catch (IOException ioe) {
			return logReply(Status.INTERNAL_SERVER_ERROR, Strings.failedReadingWar + ioe.getMessage());			
		} finally {
			readWriteLock.readLock().unlock();
		}
	}		
	
	@POST
	@Path("/airlockkeys")
	@ApiOperation(value = "Generate key for the specified roles", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response generateAirlockKey (String apiKey,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("generateAirlockKey request");
		}
		
		if (context.getAttribute(Constants.USER_ROLES) == null) {
			// authentication not configured on this server
			return logReply(Status.BAD_REQUEST, Strings.authNotConfigured);		
		}

		UserInfo userInfo = UserInfo.validate("OperationsServices.generateAirlockKey", context, assertion, null);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		UserRoles userRoles= UserRoles.get(context);
		if (userRoles == null)
			return Response.status(Status.BAD_REQUEST).entity(Strings.systemIsNotAuthenticated).build();
		
		//validate capabilities
		AirlockCapabilities capabilities = (AirlockCapabilities)context.getAttribute(Constants.CAPABILITIES_PARAM_NAME);
		if (!capabilities.getCapabilities().contains(AirlockCapability.API_KEY_MANAGEMENT)) {
			return logReply(Status.BAD_REQUEST, String.format(Strings.capabilityNotEnabled, AirlockCapability.API_KEY_MANAGEMENT.toString()));			
		}
				
		//validate that is a legal JSON
		JSONObject apiKeyJSON = null;
		try {
			apiKeyJSON = new JSONObject(apiKey);  
		} catch (JSONException je) {
			return logReply(Status.BAD_REQUEST, Strings.illegalInputJSON + je.getMessage());				
		}

		//verify that JSON does not contain uniqueId field
		if (apiKeyJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && apiKeyJSON.get(Constants.JSON_FIELD_UNIQUE_ID) !=null) {
			return logReply(Status.BAD_REQUEST, Strings.apiKeyWithId);							
		}
		AirlockChange change = new AirlockChange();
		ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
		readWriteLock.readLock().lock(); 
		try {

			AirlockAPIKeys apiKeys = (AirlockAPIKeys)context.getAttribute(Constants.API_KEYS_PARAM_NAME);

			AirlockAPIKey apiKeyObj = new AirlockAPIKey();
			ValidationResults validationRes = apiKeyObj.validateAPIKeyJSON(apiKeyJSON, apiKeys, userInfo, userRoles, context);
			if (validationRes!=null) {
				return logReply(Status.BAD_REQUEST, validationRes.error);
			}
			
			apiKeyJSON = apiKeyObj.setRolesListByHigherPermission(apiKeyJSON);

			// generate password
			String password = Utilities.generateAirlockKeyPassword();

			apiKeyObj.fromJSON(apiKeyJSON, null);
			apiKeyObj.setUniqueId(UUID.randomUUID());
			apiKeyObj.setPassword(password);
			apiKeyObj.setOwner(userInfo.getId());

			apiKeys.addAPIKey(apiKeyObj);			

			//writing updated servers to S3
			try {
				change.getFiles().addAll(AirlockFilesWriter.writeAirlockApiKeys(context));				
			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
			}

			JSONObject res = apiKeyObj.toJSON(APIKeyOutputMode.FULL);
			
			//this is the only place where password is returned. From now on the key json wont contain the password and in the server only its hash is saved
			res.put(Constants.JSON_FIELD_AIRLOCK_KEY_PASSWORD, password); 
			
			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Create new API key: " + apiKeyObj.toJSON(APIKeyOutputMode.WITHOUT_PASSWORD).toString(), userInfo); 

			logger.info("Airlock key added: "+ res.toString());

			Webhooks.get(context).notifyChanges(change, context);
			return (Response.ok(res.toString())).build();
		}catch (Throwable e) {
			return logReply(Status.INTERNAL_SERVER_ERROR, "Error generating api key: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}
	
	@GET
	@Path ("/airlockkeys")
	@ApiOperation(value = "Returns the Airlock API keys", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Season not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getAirlockAPIKeys(@ApiParam(value="Optional. If does not exist - return all API keys")@QueryParam("owner") @DefaultValue("") String owner,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getAirlockAPIKeys request. owner = " + owner);
		}

		if (context.getAttribute(Constants.USER_ROLES) == null) {
			// authentication not configured on this server
			return logReply(Status.BAD_REQUEST, Strings.authNotConfigured);		
		}
		
		UserInfo userInfo = UserInfo.validate("OperationsServices.getAirlockAPIKeys", context, assertion, null);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		
		//validate capabilities
		AirlockCapabilities capabilities = (AirlockCapabilities)context.getAttribute(Constants.CAPABILITIES_PARAM_NAME);
		if (!capabilities.getCapabilities().contains(AirlockCapability.API_KEY_MANAGEMENT)) {
			return logReply(Status.BAD_REQUEST, String.format(Strings.capabilityNotEnabled, AirlockCapability.API_KEY_MANAGEMENT.toString()));			
		}
								
		ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
		readWriteLock.readLock().lock();
		try {
			AirlockAPIKeys apiKeys = (AirlockAPIKeys)context.getAttribute(Constants.API_KEYS_PARAM_NAME);
						
			JSONObject res;
			if (owner==null || owner.isEmpty()) {
				res = apiKeys.toJSON(APIKeyOutputMode.WITHOUT_PASSWORD);
			}
			else {
				res = apiKeys.toJSONForOwner(APIKeyOutputMode.WITHOUT_PASSWORD, owner);
			}

			return (Response.ok()).entity(res.toString()).build();
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	

	@DELETE
	@Path ("/airlockkeys/{key-id}")
	@ApiOperation(value = "Deletes the Airlock API key")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Api key not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response deleteAirlockAPIKey(@PathParam("key-id")String key_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException  {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("deleteAirlockAPIKey request: key_id =" + key_id);
		}

		if (context.getAttribute(Constants.USER_ROLES) == null) {
			// authentication not configured on this server
			return logReply(Status.BAD_REQUEST, Strings.authNotConfigured);		
		}
		AirlockChange change = new AirlockChange();
		
		UserInfo userInfo = UserInfo.validate("OperationsServices.deleteAirlockAPIKey", context, assertion, null);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		String err = Utilities.validateLegalUUID(key_id);
		if (err!=null) {
			return logReply(Status.BAD_REQUEST, Strings.illegalKeyUUID + err);				
		}

		ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME); 		
		readWriteLock.writeLock().lock();
		try {		
			AirlockAPIKeys apiKeys = (AirlockAPIKeys)context.getAttribute(Constants.API_KEYS_PARAM_NAME);
			
			AirlockAPIKey apiKey = apiKeys.getAPIKeyById(key_id);	
			if (apiKey == null) {
				return logReply(Status.NOT_FOUND, Strings.apiKeyNotFound);
			}
				
			//only administrator or the key's owner can delete the key
			if (!userInfo.getRoles().contains(Constants.RoleType.Administrator) && !userInfo.getId().equals(apiKey.getOwner())) {
				return logReply(Status.BAD_REQUEST, Strings.OnlyAdminAndOwnerCanDeleteKey);
			}
			
			//key cannot delete itself
			String JwtApiKey =  userInfo.getApiKey(); //can be null if the jwt was not created from api key
			if (JwtApiKey!=null && apiKey.getKey().equals(JwtApiKey)) {
				return logReply(Status.BAD_REQUEST, Strings.KeyCannotDeleteItself);
			}

			apiKeys.removeAPIKey(apiKey);
			
			try {
				//writing updated api keys list to S3
				change.getFiles().addAll(AirlockFilesWriter.writeAirlockApiKeys(context));
			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}

			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Delete API key: uniqueId: " + key_id + ", key: " + apiKey.getKey(), userInfo); 

			logger.info("Airlock API key: uniqueId: " + key_id + ", key: " + apiKey.getKey() + " was deleted");
			Webhooks.get(context).notifyChanges(change, context);
			return (Response.ok()).build();
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}	
	
	@GET
	@Path ("/airlockkeys/{key-id}")
	@ApiOperation(value = "Returns the specified Airlock API key", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "API key not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getAirlockAPIKey(@PathParam("key-id")String key_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getAirlockAPIKey request");
		}
		
		if (context.getAttribute(Constants.USER_ROLES) == null) {
			// authentication not configured on this server
			return logReply(Status.BAD_REQUEST, Strings.authNotConfigured);		
		}
		
		UserInfo userInfo = UserInfo.validate("OperationsServices.getAirlockAPIKey", context, assertion, null);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		String err = Utilities.validateLegalUUID(key_id);
		if (err!=null) {
			return logReply(Status.BAD_REQUEST, Strings.illegalKeyUUID);				
		}

		//validate capabilities
		AirlockCapabilities capabilities = (AirlockCapabilities)context.getAttribute(Constants.CAPABILITIES_PARAM_NAME);
		if (!capabilities.getCapabilities().contains(AirlockCapability.API_KEY_MANAGEMENT)) {
			return logReply(Status.BAD_REQUEST, String.format(Strings.capabilityNotEnabled, AirlockCapability.API_KEY_MANAGEMENT.toString()));			
		}
				
		ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
		readWriteLock.readLock().lock();
		try {			
			AirlockAPIKeys apiKeys = (AirlockAPIKeys)context.getAttribute(Constants.API_KEYS_PARAM_NAME);			
			AirlockAPIKey apiKey = apiKeys.getAPIKeyById(key_id);

			if (apiKey == null) {
				return logReply(Status.NOT_FOUND, Strings.apiKeyNotFound);
			}			

			return Response.ok(apiKey.toJSON(APIKeyOutputMode.WITHOUT_PASSWORD).toString()).build();					
		} finally {
			readWriteLock.readLock().unlock();
		}
	}
	
	@PUT
	@Path ("/airlockkeys/{key-id}")
	@ApiOperation(value = "Updates the specified Airlock API key", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "API key not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response updateAirlockAPIKey(@PathParam("key-id")String key_id, String apiKey,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("updateAirlockAPIKey request: key_id =" + key_id + ", apiKey = " + apiKey);
		}

		AirlockChange change = new AirlockChange();
		if (context.getAttribute(Constants.USER_ROLES) == null) {
			return logReply(Status.BAD_REQUEST, Strings.authNotConfigured);		
		}
		
		UserInfo userInfo = UserInfo.validate("OperationsServices.updateAirlockAPIKey", context, assertion, null);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		String err = Utilities.validateLegalUUID(key_id);
		if (err!=null) {
			return logReply(Status.BAD_REQUEST, Strings.illegalKeyUUID + err);				
		}			
		
		//validate capabilities
		AirlockCapabilities capabilities = (AirlockCapabilities)context.getAttribute(Constants.CAPABILITIES_PARAM_NAME);
		if (!capabilities.getCapabilities().contains(AirlockCapability.API_KEY_MANAGEMENT)) {
			return logReply(Status.BAD_REQUEST, String.format(Strings.capabilityNotEnabled, AirlockCapability.API_KEY_MANAGEMENT.toString()));			
		}
									
		ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
		readWriteLock.writeLock().lock();
		try {

			AirlockAPIKeys apiKeys = (AirlockAPIKeys)context.getAttribute(Constants.API_KEYS_PARAM_NAME);			
			AirlockAPIKey apiKeyToUpdate = apiKeys.getAPIKeyById(key_id);

			if (apiKeyToUpdate == null) {
				return logReply(Status.NOT_FOUND, Strings.apiKeyNotFound);
			}			

			//only administrator or the key's owner can update the key
			if (!userInfo.getRoles().contains(Constants.RoleType.Administrator) && !userInfo.getId().equals(apiKeyToUpdate.getOwner())) {
				return logReply(Status.BAD_REQUEST, Strings.OnlyAdminAndOwnerCanUpdateKey);
			}

			JSONObject updatedApiKeyJSON = null;
			try {
				updatedApiKeyJSON = new JSONObject(apiKey);
			} catch (JSONException je) {
				return logReply(Status.BAD_REQUEST, Strings.illegalInputJSON + je.getMessage());
			}						

			//if not set - set the uniqueId to be the id path param
			if (!updatedApiKeyJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) || updatedApiKeyJSON.get(Constants.JSON_FIELD_UNIQUE_ID) == null) {
				updatedApiKeyJSON.put(Constants.JSON_FIELD_UNIQUE_ID, key_id);
			}
			else {
				//verify that stream-id in path is identical to uniqueId in request pay-load  
				if (!updatedApiKeyJSON.getString(Constants.JSON_FIELD_UNIQUE_ID).equals(key_id)) {
					return logReply(Status.BAD_REQUEST, Strings.apiKeyWithDifferentId);					
				}
			}			
			UserRoles userRoles= UserRoles.get(context);
			if (userRoles == null)
				return Response.status(Status.BAD_REQUEST).entity(Strings.systemIsNotAuthenticated).build();

			ValidationResults validationRes = apiKeyToUpdate.validateAPIKeyJSON(updatedApiKeyJSON, apiKeys, userInfo, userRoles, context);
			if (validationRes!=null) {
				return logReply(validationRes.status, validationRes.error);
			}

			updatedApiKeyJSON = apiKeyToUpdate.setRolesListByHigherPermission(updatedApiKeyJSON);

			//finally - actually update the stream.
			String updateDetails = apiKeyToUpdate.updateAPIKeyJSON(updatedApiKeyJSON);

			if (!updateDetails.isEmpty()) { //if some fields were changed

				try {
					change.getFiles().addAll(AirlockFilesWriter.writeAirlockApiKeys(context));
				} catch (IOException e) {
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
				}

				AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
				auditLogWriter.log("Update Airlock API key: " + key_id + ", " + apiKeyToUpdate.getKey() + ": " + updateDetails, userInfo);
				Webhooks.get(context).notifyChanges(change, context);
				if (logger.isLoggable(Level.FINE)) {
					logger.fine("Updated stream: " + apiKeyToUpdate.toJSON(APIKeyOutputMode.WITHOUT_PASSWORD) + "\n updatd details: " + updateDetails);
				}
			}			

			JSONObject res = apiKeyToUpdate.toJSON(APIKeyOutputMode.WITHOUT_PASSWORD);
			return (Response.ok(res.toString())).build();
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}
	
	@GET
	@Path ("/capabilities")
	@ApiOperation(value = "Returns Airlock capabilities", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getAirlockCapabilities(@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {

		// use userInfo for more stringent checks. null if authorization is off
		UserInfo userInfo = UserInfo.validate("OperationsServices.getAirlockCapabilities", context, assertion, null);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return Response.status(Status.UNAUTHORIZED).entity(userInfo.getErrorJson()).build();

		ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
		readWriteLock.readLock().lock();
		try {
			AirlockCapabilities capabilities = (AirlockCapabilities)context.getAttribute(Constants.CAPABILITIES_PARAM_NAME);		
	
			JSONObject res = capabilities.toJSON();
			
			return (Response.ok(res.toString())).build();	
		} finally {
			readWriteLock.readLock().unlock();
		}
	}		

	@PUT
	@Path ("/capabilities")
	@ApiOperation(value = "Updates Airlock capabilities", response = String.class)
	@Produces(value="text/plain")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response setAirlockCapabilities(String capabilities, @HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {

		UserInfo userInfo = UserInfo.validate("OperationsServices.setAirlockCapabilities", context, assertion, null);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return Response.status(Status.UNAUTHORIZED).entity(userInfo.getErrorJson()).build();
		AirlockChange change = new AirlockChange();
		ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
		readWriteLock.writeLock().lock();
		try {
			AirlockCapabilities airlockCapabilities = (AirlockCapabilities)context.getAttribute(Constants.CAPABILITIES_PARAM_NAME);		
			
			//validate that is a legal JSON
			JSONObject capabilitiesJSON = null;
			try {
				capabilitiesJSON = new JSONObject(capabilities);
			} catch (JSONException je) {
				return Response.status(Status.BAD_REQUEST).entity(Strings.illegalInputJSON + je.getMessage()).build();
			}
			
			ValidationResults validationRes = airlockCapabilities.validateCapabilitiesJSON(capabilitiesJSON, context);
			if (validationRes!=null)
				return logReply(validationRes.status, validationRes.error);
				//return Response.status(validationRes.status).entity(validationRes.error).build();
			
			Set<AirlockCapability> prevCapabilities = Utilities.cloneCapabilitiesSet(airlockCapabilities.getCapabilities());
			String updateDetails = airlockCapabilities.updateCapabilitiesJSON(capabilitiesJSON);
			if (!updateDetails.isEmpty()) {
				 String productsUpdateDetails = airlockCapabilities.updateProductWithReducdedCapabilities(prevCapabilities, context);
				 boolean someProductUpdated = productsUpdateDetails!=null;
				 
				//writing updated capabilities to S3
				try {
					Pair<String, LinkedList<AirlockChangeContent>> writeResult =AirlockFilesWriter.writeAirlockCapabilities(airlockCapabilities.toJSON(), context); 
					String err = writeResult.getKey();
					if (err!=null) {
						return Response.status(Status.INTERNAL_SERVER_ERROR).entity(err).build();
					}
					change.setFiles(writeResult.getValue());
					
					if (someProductUpdated) {
						@SuppressWarnings("unchecked")
						Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);

						change.getFiles().addAll(AirlockFilesWriter.writeProducts(productsDB, context, null)); //no need to update product runtime in seasons
						updateDetails = updateDetails + productsUpdateDetails;
					}
				} catch (IOException e) {
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
				}
				
				
				AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
				auditLogWriter.log("Update Airlock capabilities: " + updateDetails, userInfo);
				Webhooks.get(context).notifyChanges(change, context);
			}
			return Response.ok().build();
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}	

	@POST
	@Path ("/products/{product-id}/userrolesets")
	@ApiOperation(value = "Create an user role set for the specified product", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Product not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response addUserRoleSetToProduct(@PathParam("product-id")String product_id, 
			String newUser,			
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("addUserRoleSetToProduct request: product_id = " + product_id + ", newUser = " + newUser);
		}

		String err = Utilities.validateLegalUUID(product_id);
		if (err!=null) 
			return logReply(Status.BAD_REQUEST, Strings.illegalProductUUID + err);

		AirlockChange change = new AirlockChange();
		//find relevant product
		ProductErrorPair prodErrPair = Utilities.getProduct(context, product_id);
		if (prodErrPair.error != null) {			
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		Product currentProduct = prodErrPair.product;
		change.setProduct(currentProduct);
		//check user authorization			
		UserInfo userInfo = UserInfo.validate("OperationsServices.addUserRoleSetToProduct", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);
						
		//validate that is a legal JSON
		JSONObject newUserJSON = null;
		try {
			newUserJSON = new JSONObject(newUser);  
		} catch (JSONException je) {
			return logReply(Status.BAD_REQUEST, Strings.illegalInputJSON + je.getMessage());
		}

		//verify that JSON does not contain uniqueId field
		if (newUserJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && newUserJSON.get(Constants.JSON_FIELD_UNIQUE_ID) !=null) {
			return logReply(Status.BAD_REQUEST, Strings.airlockUserWithId);							
		}

		//verify that JSON does not contain different product-id then the path parameter
		if (newUserJSON.containsKey(Constants.JSON_FIELD_PRODUCT_ID) && newUserJSON.get(Constants.JSON_FIELD_PRODUCT_ID) !=null) {
			if (!product_id.equals(newUserJSON.getString(Constants.JSON_FIELD_PRODUCT_ID))) {
				return logReply(Status.BAD_REQUEST, Strings.userProductWithDifferentId);									
			}
		}
		else if (newUserJSON.containsKey(Constants.JSON_FIELD_PRODUCT_ID) && newUserJSON.get(Constants.JSON_FIELD_PRODUCT_ID) == null) { 
			return logReply(Status.BAD_REQUEST, Strings.productUserDoesNotContainNullProductId);
		}
		else {		
			newUserJSON.put(Constants.JSON_FIELD_PRODUCT_ID, product_id);
		}

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();	
		readWriteLock.writeLock().lock();
		try {	
			@SuppressWarnings("unchecked")
			Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);					

			@SuppressWarnings("unchecked")
			Map<String, UserRoleSet> usersDB = (Map<String, UserRoleSet>)context.getAttribute(Constants.AIRLOCK_USERS_DB_PARAM_NAME);

			//Validate product existence
			Product product = productsDB.get(product_id);

			if (product == null) {
				return logReply(Status.NOT_FOUND, Strings.productNotFound);
			}			

			UserRoleSet newUserObj = product.getProductUsers().new UserRoleSet();

			ValidationResults validationRes = newUserObj.validateAirlockUser(newUserJSON, context);
			if (validationRes!=null) {
				return logReply(validationRes.status, validationRes.error);
			}

			newUserJSON = newUserObj.setRolesListByHigherPermission(newUserJSON);
			newUserObj.fromJSON(newUserJSON);
			newUserObj.setUniqueId(UUID.randomUUID());

			//verify that user is global user (cannot ad user to product if he is not global user)
			UserRoles userRoles = UserRoles.get(context);
			Set<RoleType> globalUserRoles = userRoles.getUserRoles(newUserObj.getUserIdentifier());
			if (globalUserRoles == null || globalUserRoles.size() == 0) {
				return logReply(Status.BAD_REQUEST, String.format(Strings.notGlobalUser, newUserObj.getUserIdentifier()));
			}
			
			product.getProductUsers().addUser(newUserObj);			

			@SuppressWarnings("unchecked")
			Map<String,UserRoles> rolesPerProductMap = (Map<String,UserRoles>) context.getAttribute(Constants.USER_ROLES_PER_PRODUCT_PARAM_NAME);
			
			UserRoles productUserRoles = rolesPerProductMap.get(product_id);
			productUserRoles.addUser(newUserObj);
			
			try {
				change.getFiles().addAll(AirlockFilesWriter.writeProductUsers(product, context));				
			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}

			usersDB.put(newUserObj.getUniqueId().toString(), newUserObj);

			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Create new user role set for product " + product_id + ": " + newUserObj.toJSON().toString(), userInfo); 

			JSONObject res = newUserObj.toJSON();
			
			logger.info("Airlock user added to product '"+  product_id + "': "+ res.toString());

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Added Airlock user: " + newUserObj.toJSON());
			}
			Webhooks.get(context).notifyChanges(change, context);
			return (Response.ok(res.toString())).build();
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	@POST
	@Path ("/userrolesets")
	@ApiOperation(value = "Craete a global user role set.", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response addUserRoleSet(String newUser,			
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("addUserRoleSet request, newUser = " + newUser);
		}
		
		UserInfo userInfo = UserInfo.validate("OperationsServices.addUserRoleSet", context, assertion, null);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		//validate that is a legal JSON
		JSONObject newUserJSON = null;
		try {
			newUserJSON = new JSONObject(newUser);  
		} catch (JSONException je) {
			return logReply(Status.BAD_REQUEST, Strings.illegalInputJSON + je.getMessage());
		}

		//verify that JSON does not contain uniqueId field
		if (newUserJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && newUserJSON.get(Constants.JSON_FIELD_UNIQUE_ID) !=null) {
			return logReply(Status.BAD_REQUEST, Strings.airlockUserWithId);							
		}

		//verify that JSON does not contain product
		if (newUserJSON.containsKey(Constants.JSON_FIELD_PRODUCT_ID) && newUserJSON.get(Constants.JSON_FIELD_PRODUCT_ID) !=null) {
			return logReply(Status.BAD_REQUEST, Strings.globalUserDoesNotContainProductId);									
		}
		
		String prod_id = null;
		newUserJSON.put(Constants.JSON_FIELD_PRODUCT_ID, prod_id);
		
		ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
		readWriteLock.writeLock().lock();
		try {	
			@SuppressWarnings("unchecked")
			Map<String, UserRoleSet> usersDB = (Map<String, UserRoleSet>)context.getAttribute(Constants.AIRLOCK_USERS_DB_PARAM_NAME);
			UserRoleSets globalUsers = (UserRoleSets)context.getAttribute(Constants.AIRLOCK_GLOBAL_USERS_PARAM_NAME); 					
			

			UserRoleSet newUserObj = globalUsers.new UserRoleSet();

			ValidationResults validationRes = newUserObj.validateAirlockUser(newUserJSON, context);
			if (validationRes!=null) {
				return logReply(validationRes.status, validationRes.error);
			}

			newUserJSON = newUserObj.setRolesListByHigherPermission(newUserJSON);
			newUserObj.fromJSON(newUserJSON);
			newUserObj.setUniqueId(UUID.randomUUID());

			globalUsers.addUser(newUserObj);			

			UserRoles userRoles = (UserRoles)context.getAttribute(Constants.USER_ROLES);
			userRoles.addUser(newUserObj);
			AirlockChange change = new AirlockChange();
			try {
				change.getFiles().addAll(AirlockFilesWriter.writeGlobalAirlockUsers(globalUsers.toJSON(), context)); 
			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}

			usersDB.put(newUserObj.getUniqueId().toString(), newUserObj);

			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Create new global user role set: " + newUserObj.toJSON().toString(), userInfo); 

			JSONObject res = newUserObj.toJSON();
			logger.info("Global Airlock user added: "+ res.toString());

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Added global airlock user: " + newUserObj.toJSON());
			}
			Webhooks.get(context).notifyChanges(change, context);
			
			
			return (Response.ok(res.toString())).build();
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	@GET
	@Path("/products/{product-id}/userrolesets")
	@ApiOperation(value = "Returns all Airlock users for the specified product", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Product not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getProductUserRoleSets(@PathParam("product-id")String product_id, 
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getProductUserRoleSets request, product-id " + product_id);
		}

		String err = Utilities.validateLegalUUID(product_id);
		if (err!=null) 
			return logReply(Status.BAD_REQUEST, Strings.illegalProductUUID + err);


		//find relevant product
		ProductErrorPair prodErrPair = Utilities.getProduct(context, product_id);
		if (prodErrPair.error != null) {			
			return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		Product currentProduct = prodErrPair.product;
		
		//check user authorization			
		UserInfo userInfo = UserInfo.validate("OperationsServices.getProductUserRoleSets", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		
		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.readLock().lock(); 
		try {
			@SuppressWarnings("unchecked")
			Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);		

			Product prod = productsDB.get(product_id);

			if (prod == null) {
				return logReply(Status.NOT_FOUND, Strings.productNotFound);
			}

			UserRoleSets productUsers = prod.getProductUsers();
			
			JSONObject res = productUsers.toJSON();
			
			return (Response.ok(res.toString())).build();	
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	@POST
	@Path ("/webhooks")
	@ApiOperation(value = "Craete new webhook.", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response addWebhook(String newWebhook,
								   @HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("addWebhook request, newWebhook = " + newWebhook);
		}
		AirlockChange change = new AirlockChange();
		UserInfo userInfo = UserInfo.validate("OperationsServices.addWebhook", context, assertion, null);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);
		
		//validate that is a legal JSON
		JSONObject newWebhookJSON = null;
		try {
			newWebhookJSON = new JSONObject(newWebhook);  
		} catch (JSONException je) {
			return logReply(Status.BAD_REQUEST, Strings.illegalInputJSON + je.getMessage());
		}
		
		//verify that JSON does not contain uniqueId field
		if (newWebhookJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && newWebhookJSON.get(Constants.JSON_FIELD_UNIQUE_ID) !=null) {
			return logReply(Status.BAD_REQUEST, Strings.webhookWithId);							
		}
		
		//create the webhook
		ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
		readWriteLock.writeLock().lock();
		try {	
			Webhooks webhooks = (Webhooks)context.getAttribute(Constants.WEBHOOKS_PARAM_NAME);
			
			Webhook hook = new Webhook();

			ValidationResults validationRes = webhooks.validateWebhookJSON(newWebhookJSON, hook, context);
			if (validationRes!=null) {
				return logReply(validationRes.status, validationRes.error);
			}

			try {
				hook.fromJSON(newWebhookJSON);
			} catch (JSONException je) {
				String errMsg = Strings.illegalInputJSON + je.getMessage();
				return logReply(Status.BAD_REQUEST, errMsg);
			}

			hook.setUniqueId(UUID.randomUUID());

			webhooks.addWebhook(hook);
			
			try {
				change.getFiles().addAll(AirlockFilesWriter.writeWebhooks(context));				
			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}

			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Create new webhook: " + hook.toJson().toString(), userInfo); 

			JSONObject res = hook.toJson();
			logger.info("Webhook added: "+ res.toString());

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Added webhook: " + hook.toJson());
			}
			Webhooks.get(context).notifyChanges(change, context);
			return (Response.ok(res.toString())).build();
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}
	
	@PUT
	@Path ("/webhooks/{webhook-id}")
	@ApiOperation(value = "Update webhook.", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 404, message = "Webhook not found"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response updateWebhook(@PathParam("webhook-id")String webhook_id, String updatedWebhook,
								   @HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("update request: webhook_id =" + webhook_id);
		}
		UserInfo userInfo = UserInfo.validate("OperationsServices.updateWebhook", context, assertion, null);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);
		String err = Utilities.validateLegalUUID(webhook_id);
		if (err!=null)
			return logReply(Status.BAD_REQUEST, Strings.illegalNotificationUUID + err);
		
		AirlockChange change = new AirlockChange();
		
		ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
		readWriteLock.writeLock().lock();
		try {
			Webhooks webhooks = (Webhooks)context.getAttribute(Constants.WEBHOOKS_PARAM_NAME);
			Webhook hookToUpdate = webhooks.get(webhook_id);
			
			if (hookToUpdate == null) {
				return logReply(Status.NOT_FOUND, Strings.webhookNotFound);
			}
			
			//validate that is a legal JSON
			JSONObject updateWebhookJSON = null;
			try {
				updateWebhookJSON = new JSONObject(updatedWebhook);  
			} catch (JSONException je) {
				return logReply(Status.BAD_REQUEST, Strings.illegalInputJSON + je.getMessage());
			}
			//if not set - set the uniqueId to be the id path param
			if (!updateWebhookJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) || updateWebhookJSON.get(Constants.JSON_FIELD_UNIQUE_ID) == null) {
				updateWebhookJSON.put(Constants.JSON_FIELD_UNIQUE_ID, webhook_id);
			}
			else {
				//verify that stream-id in path is identical to uniqueId in request pay-load  
				if (!updateWebhookJSON.getString(Constants.JSON_FIELD_UNIQUE_ID).equals(webhook_id)) {
					return logReply(Status.BAD_REQUEST, Strings.webhookWithDifferentId);
				}
			}
			ValidationResults validationRes = webhooks.validateWebhookJSON(updateWebhookJSON, hookToUpdate, context);
			if (validationRes!=null) {
				return logReply(validationRes.status, validationRes.error);
			}
			hookToUpdate.updateWebhook(updateWebhookJSON);
						
			try {
				change.getFiles().addAll(AirlockFilesWriter.writeWebhooks(context));
				
			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}
			
			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Update webhook: uniqueId: " + webhook_id + ", name: " + hookToUpdate.getName(), userInfo); 
			logger.info("Webhook " + hookToUpdate.getName() + ", " + webhook_id + " was updated");
			webhooks.notifyChanges(change, context);
			return (Response.ok()).build();
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}
	/*
	@PUT
	@Path ("/resetelasticsearch")	
	@ApiOperation(value = "Reset the elastic search", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response resetElasticSearch(@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("resetElasticSearch request");
		}
		
		//check user authorization
		UserInfo userInfo = UserInfo.validate("OperationsServices.resetElasticSearch", context, assertion, null);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return Response.status(Status.UNAUTHORIZED).entity(userInfo.getErrorJson()).build();

		ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
		readWriteLock.writeLock().lock();
		try {			
			Utilities.resetElasticSearch(context);	
			
			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Reset elastic search.", userInfo);
			return Response.ok().build();
		} catch (Throwable e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}*/

	
	@DELETE
	@Path ("/webhooks/{webhook-id}")
	@ApiOperation(value = "Deletes the specified webhook", response = String.class)	
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class),
			@ApiResponse(code = 404, message = "Webhook not found"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response deleteWebhook(@PathParam("webhook-id")String webhook_id, @HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("deleteWebhook request: webhook_id =" + webhook_id);
		}
		UserInfo userInfo = UserInfo.validate("OperationsServices.deleteWebhook", context, assertion, null);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);
		AirlockChange change = new AirlockChange();
		ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
		readWriteLock.writeLock().lock();
		try {
			Webhooks webhooks = (Webhooks)context.getAttribute(Constants.WEBHOOKS_PARAM_NAME);
			Webhook hookToDel = webhooks.get(webhook_id);
			
			if (hookToDel == null) {
				return logReply(Status.NOT_FOUND, Strings.webhookNotFound);
			}
			
			webhooks.removeWebhook(hookToDel);
						
			try {
				change.getFiles().addAll(AirlockFilesWriter.writeWebhooks(context));
				
			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}
			
			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Delete webhook: uniqueId: " + webhook_id + ", name: " + hookToDel.getName(), userInfo); 
			logger.info("Webhook " + hookToDel.getName() + ", " + webhook_id + " was deleted");
			Webhooks.get(context).notifyChanges(change, context);
			return (Response.ok()).build();
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}
	
	@GET
	@Path("/webhooks")
	@ApiOperation(value = "Returns all webhooks for this server", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Product not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getWebhooks( 
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getWebhooks request");
		}
		UserInfo userInfo = UserInfo.validate("OperationsServices.getWebhooks", context, assertion, null);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
		readWriteLock.readLock().lock(); 

		try {				
			Webhooks webhooks = (Webhooks)context.getAttribute(Constants.WEBHOOKS_PARAM_NAME);
			JSONObject res = webhooks.toJSON();				
			return (Response.ok(res.toString())).build();	
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	Response logReply(Status status, String errMsg)
	{
		logger.severe(errMsg);
		return Response.status(status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();		
	}
	Response sendInfoError(Status status, UserInfo info)
	{
		return Response.status(status).entity(info.getErrorJson()).build();
	}
}
