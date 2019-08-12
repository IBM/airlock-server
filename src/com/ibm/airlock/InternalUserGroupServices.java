package com.ibm.airlock;

import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponses;

import com.ibm.airlock.utilities.Pair;

import com.wordnik.swagger.annotations.ApiResponse;

import com.ibm.airlock.admin.InternalUserGroups;
import com.ibm.airlock.admin.Product;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.ValidationResults;
import com.ibm.airlock.admin.Utilities.ProductErrorPair;
import com.ibm.airlock.admin.authentication.UserInfo;
import com.ibm.airlock.admin.operations.AirlockChange;
import com.ibm.airlock.admin.operations.AirlockChangeContent;
import com.ibm.airlock.admin.operations.Webhooks;
import com.ibm.airlock.admin.serialize.AirlockFilesWriter;
import com.ibm.airlock.admin.serialize.AuditLogWriter;


@Path ("/admin/products/{product-id}/usergroups")
@Api(value = "/internalUserGroups", description = "Internal user groups management API")
public class InternalUserGroupServices {

	public static final Logger logger = Logger.getLogger(InternalUserGroupServices.class.getName());

	@Context
	private ServletContext context;

	
	
	@GET
	//@Path("/{product-id}/usergroups")
	@ApiOperation(value = "Returns all internal user groups of the specified product", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Product not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getInternalUserGroups(@PathParam("product-id")String product_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {

		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getInternalUserGroups request, product-id " + product_id);
		}

		//find relevant product
		ProductErrorPair prodErrPair = Utilities.getProduct(context, product_id);
		if (prodErrPair.error != null) {	
			return logReply(Status.BAD_REQUEST, prodErrPair.error);
		}
		Product currentProduct = prodErrPair.product;
		
		//check user authorization			
		UserInfo userInfo = UserInfo.validate("InternalUserGroupServices.getInternalUserGroups", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
		readWriteLock.readLock().lock();
		try {
			
			@SuppressWarnings("unchecked")
			Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);		

			Product prod = productsDB.get(product_id);

			if (prod == null) {
				return logReply(Status.NOT_FOUND, Strings.productNotFound);
			}

			@SuppressWarnings("unchecked")
			Map<String,InternalUserGroups> groupsPerProductMap = (Map<String,InternalUserGroups>) context.getAttribute(Constants.USER_GROUPS_PER_PRODUCT_PARAM_NAME);
			InternalUserGroups groupsList = groupsPerProductMap.get(product_id);
			
			JSONObject res = groupsList.toJson();
			
			return (Response.ok(res.toString())).build();	
		} finally {
			readWriteLock.readLock().unlock();
		}
	}		

	@PUT
	//@Path("/{product-id}/usergroups")
	@ApiOperation(value = "Updates all internal user groups", response = String.class)
	@Produces(value="text/plain")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 400, message = "Bad request"),
			@ApiResponse(code = 404, message = "Product not found"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response setInternalUserGroups(@PathParam("product-id")String product_id, String internalUserGroups, @HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {

		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("setInternalUserGroups request, product-id " + product_id + ", internalUserGroups = " + internalUserGroups);
		}
		AirlockChange change = new AirlockChange();
		//find relevant product
		ProductErrorPair prodErrPair = Utilities.getProduct(context, product_id);
		if (prodErrPair.error != null) {			
			return logReply(Status.BAD_REQUEST, prodErrPair.error);
		}
		Product currentProduct = prodErrPair.product;
		change.setProduct(currentProduct);
		
		//check user authorization			
		UserInfo userInfo = UserInfo.validate("InternalUserGroupServices.setInternalUserGroups", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
		readWriteLock.writeLock().lock();
		try {
			//InternalUserGroups groupsList = (InternalUserGroups)context.getAttribute(Constants.USER_GROUPS_PARAM_NAME);		
			@SuppressWarnings("unchecked")
			Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);		

			Product prod = productsDB.get(product_id);

			if (prod == null) {
				return logReply(Status.NOT_FOUND, Strings.productNotFound);
			}

			@SuppressWarnings("unchecked")
			Map<String,InternalUserGroups> groupsPerProductMap = (Map<String,InternalUserGroups>) context.getAttribute(Constants.USER_GROUPS_PER_PRODUCT_PARAM_NAME);
			
			//validate that is a legal JSON
			JSONObject userGroupsJSON = null;
			try {
				userGroupsJSON = new JSONObject(internalUserGroups);
			} catch (JSONException je) {
				return logReply(Status.BAD_REQUEST, Strings.illegalInputJSON + je.getMessage());
			}
			
			InternalUserGroups groupsList = groupsPerProductMap.get(product_id);

			
			ValidationResults validationRes = groupsList.validateInternalUserGroupsJSON(userGroupsJSON, product_id, context);
			if (validationRes!=null)
				return logReply(validationRes.status, validationRes.error);
				
			String prevGroupsList = groupsList.getGroupsAsArrayString();
			groupsList.fromJSON(userGroupsJSON);
			groupsList.setLastModified(new Date());
			
			//writing updated user groups to S3
			try {
				Pair<String, LinkedList<AirlockChangeContent>> writeRes = AirlockFilesWriter.writeUserGroups(groupsList, context, prod); 
				String err = writeRes.getKey();
				if (err!=null) {
					return logReply(Status.INTERNAL_SERVER_ERROR, err);
				}
				change.getFiles().addAll(writeRes.getValue());
			} catch (IOException e) {
				return logReply(Status.INTERNAL_SERVER_ERROR, e.getMessage());
				
			}
			Webhooks.get(context).notifyChanges(change, context);
			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("InternalUserGroups updated from:\n   " + prevGroupsList + "\nto:\n   " + groupsList.getGroupsAsArrayString(), userInfo); 
					
			return Response.ok().build();
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	@GET
	@Path("/usage")
	@ApiOperation(value = "Returns all user groups usage of the specified product.", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Product not found"),
			@ApiResponse(code = 500, message = "Internal error") })
	public Response getInternalUserGroupsUsage(@PathParam("product-id")String product_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {

		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getInternalUserGroups request, product-id " + product_id);
		}

		//find relevant product
		ProductErrorPair prodErrPair = Utilities.getProduct(context, product_id);
		if (prodErrPair.error != null) {	
			return logReply(Status.BAD_REQUEST, prodErrPair.error);
		}
		Product currentProduct = prodErrPair.product;
		
		//check user authorization			
		UserInfo userInfo = UserInfo.validate("InternalUserGroupServices.getInternalUserGroupsUsage", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Status.UNAUTHORIZED, userInfo);

		ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
		readWriteLock.readLock().lock();
		try {
			
			@SuppressWarnings("unchecked")
			Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);		

			Product prod = productsDB.get(product_id);

			if (prod == null) {
				return logReply(Status.NOT_FOUND, Strings.productNotFound);
			}

			@SuppressWarnings("unchecked")
			Map<String,InternalUserGroups> groupsPerProductMap = (Map<String,InternalUserGroups>) context.getAttribute(Constants.USER_GROUPS_PER_PRODUCT_PARAM_NAME);
			InternalUserGroups groupsList = groupsPerProductMap.get(product_id);
			
			JSONObject res = groupsList.toUsageJson(prod, context);
			
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
