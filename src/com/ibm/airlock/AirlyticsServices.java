package com.ibm.airlock;

import com.ibm.airlock.Constants.OutputJSONMode;
import com.ibm.airlock.admin.Product;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.ValidationResults;
import com.ibm.airlock.admin.airlytics.entities.Attribute;
import com.ibm.airlock.admin.airlytics.entities.AttributeType;
import com.ibm.airlock.admin.airlytics.entities.AttributeType.AttributesPermission;
import com.ibm.airlock.admin.airlytics.entities.EntitiesUtilities;
import com.ibm.airlock.admin.airlytics.entities.Entity;
import com.ibm.airlock.admin.authentication.UserInfo;
import com.ibm.airlock.admin.cohorts.AirCohortsCallResult;
import com.ibm.airlock.admin.cohorts.CohortItem;
import com.ibm.airlock.admin.cohorts.CohortsUtilities;
import com.ibm.airlock.admin.cohorts.QueryValidationItem;
import com.ibm.airlock.admin.dataimport.DataImportCallResult;
import com.ibm.airlock.admin.dataimport.DataImportItem;
import com.ibm.airlock.admin.dataimport.DataImportUtilities;
import com.ibm.airlock.admin.operations.AirlockCapabilities;
import com.ibm.airlock.admin.operations.AirlockChange;
import com.ibm.airlock.admin.operations.Webhooks;
import com.ibm.airlock.admin.serialize.AirlockFilesWriter;
import com.ibm.airlock.admin.serialize.AuditLogWriter;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.ibm.airlock.Constants.COHORT_EXPORT_TYPE_LOCALYTICS;

@Path("/admin/airlytics")
@Api(value = "/airlytics", description = "Airlytics management API")
public class AirlyticsServices {
	public static final Logger logger = Logger.getLogger(AirlyticsServices.class.getName());

	@Context
	private ServletContext context;

	@GET
	@Path("/products/{product-id}/cohorts/meta/users/columns")
	@ApiOperation(value = "Get users DB table columns", response = String.class)
	@ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Feature not found"),
			@ApiResponse(code = 500, message = "Internal error")})
	public Response getCohortsColumnNames(@PathParam("product-id") String product_id, @HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getCohortsColumnNames request");
		}

		String err = Utilities.validateLegalUUID(product_id);
		if (err!=null)
			return sendAndLogError(Response.Status.BAD_REQUEST, Strings.illegalProductUUID + err);

		//find relevant product
		Utilities.ProductErrorPair prodErrPair = Utilities.getProduct(context, product_id);
		if (prodErrPair.error != null) {
			return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		Product currentProduct = prodErrPair.product;

		UserInfo userInfo = UserInfo.validate("AirlyticsServices.getCohorts", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Response.Status.UNAUTHORIZED, userInfo);

		try {
			return CohortsUtilities.getColumnsNames(context, product_id, assertion);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Error getting cohorts column names :", e);
			return sendAndLogError(Response.Status.INTERNAL_SERVER_ERROR, "Error getting cohorts column names: " + e.getMessage());
		}
	}


	@GET
	@Path("/products/{product-id}/cohorts")
	@ApiOperation(value = "Get cohorts for product", response = String.class)
	@ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Feature not found"),
			@ApiResponse(code = 500, message = "Internal error")})
	public Response getCohorts(@PathParam("product-id") String product_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getCohorts request");
		}

		String err = Utilities.validateLegalUUID(product_id);
		if (err!=null)
			return sendAndLogError(Response.Status.BAD_REQUEST, Strings.illegalProductUUID + err);

		//find relevant product
		Utilities.ProductErrorPair prodErrPair = Utilities.getProduct(context, product_id);
		if (prodErrPair.error != null) {
			return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		Product currentProduct = prodErrPair.product;

		UserInfo userInfo = UserInfo.validate("AirlyticsServices.getCohorts", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Response.Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new Constants.AirlockCapability[]{Constants.AirlockCapability.COHORTS});
		if (capabilityValidationRes!=null)
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();

		ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
		readWriteLock.readLock().lock();
		try {
			@SuppressWarnings("unchecked")
			JSONObject cohortsJson = currentProduct.getCohorts().toJson();

			return (Response.ok(cohortsJson.toString())).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error getting cohorts :", e);
			return sendAndLogError(Response.Status.INTERNAL_SERVER_ERROR, "Error getting cohorts: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	@GET
	@Path("/cohorts/{cohort-id}")
	@ApiOperation(value = "Get cohort", response = String.class)
	@ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Feature not found"),
			@ApiResponse(code = 500, message = "Internal error")})
	public Response getCohort(@PathParam("cohort-id") String cohort_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getCohort request");
		}

		//find relevant product
		Utilities.ProductErrorPair prodErrPair = Utilities.getProductOfCohort(context, cohort_id);
		if (prodErrPair.error != null) {
			return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		Product currentProduct = prodErrPair.product;

		UserInfo userInfo = UserInfo.validate("AirlyticsServices.getCohort", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Response.Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new Constants.AirlockCapability[]{Constants.AirlockCapability.COHORTS});
		if (capabilityValidationRes!=null)
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();

		ReentrantReadWriteLock readWriteLock = currentProduct.getCohortsProductLock();
		printLockStats(readWriteLock, "getCohort");
		readWriteLock.readLock().lock();
		try {
			@SuppressWarnings("unchecked")
			Map<String, CohortItem> cohortsDB = (Map<String, CohortItem>)context.getAttribute(Constants.COHORTS_DB_PARAM_NAME);

			CohortItem cohort = cohortsDB.get(cohort_id);

			if (cohort == null) {
				String errMsg = Strings.cohortNotFound;
				logger.severe(errMsg);
				return Response.status(Response.Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			JSONObject cohortJSON = cohort.toJson();
			return (Response.ok(cohortJSON.toString())).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error getting cohort :", e);
			return sendAndLogError(Response.Status.INTERNAL_SERVER_ERROR, "Error getting cohort: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	@PUT
	@Path("/cohorts/{cohort-id}")
	@ApiOperation(value = "Update cohort", response = String.class)
	@ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Feature not found"),
			@ApiResponse(code = 500, message = "Internal error")})
	public Response updateCohort(@PathParam("cohort-id") String cohort_id, String cohort,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("updateCohort request for id:"+cohort_id);
		}

		AirlockChange change = new AirlockChange();

		String err = Utilities.validateLegalUUID(cohort_id);
		if (err!=null)
			return sendAndLogError(Response.Status.BAD_REQUEST, Strings.illegalCohortUUID + err);

		//find relevant product
		Utilities.ProductErrorPair prodErrPair = Utilities.getProductOfCohort(context, cohort_id);
		if (prodErrPair.error != null) {
			return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		Product currentProduct = prodErrPair.product;
		change.setProduct(currentProduct);

		UserInfo userInfo = UserInfo.validate("AirlyticsServices.updateCohort", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Response.Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new Constants.AirlockCapability[]{Constants.AirlockCapability.COHORTS});
		if (capabilityValidationRes!=null)
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();

		ReentrantReadWriteLock readWriteLock = currentProduct.getCohortsProductLock();
		printLockStats(readWriteLock, "updateCohort");
		readWriteLock.writeLock().lock();
		try {
			@SuppressWarnings("unchecked")
			Map<String, CohortItem> cohortsDB = (Map<String, CohortItem>)context.getAttribute(Constants.COHORTS_DB_PARAM_NAME);

			@SuppressWarnings("unchecked")
			Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);

			CohortItem cohortToUpdate = cohortsDB.get(cohort_id);
			if (cohortToUpdate == null) {
				String errMsg = Strings.cohortNotFound;
				logger.severe(errMsg);
				return Response.status(Response.Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			JSONObject updatedCohortJSON = null;
			try {
				updatedCohortJSON = new JSONObject(cohort);
			} catch (JSONException je) {
				String errMsg = Strings.illegalInputJSON + je.getMessage();
				logger.severe(errMsg);
				return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			if (!updatedCohortJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) || updatedCohortJSON.get(Constants.JSON_FIELD_UNIQUE_ID) == null) {
				updatedCohortJSON.put(Constants.JSON_FIELD_UNIQUE_ID, cohort_id);
			}
			else {
				//verify that cohort-id in path is identical to uniqueId in request pay-load
				if (!updatedCohortJSON.getString(Constants.JSON_FIELD_UNIQUE_ID).equals(cohort_id)) {
					String errMsg = Strings.cohortWithDifferentId;
					logger.severe(errMsg);
					return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
				}
			}

			ValidationResults validationRes = cohortToUpdate.validateCohortJSON(updatedCohortJSON, context, userInfo);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			//finally - actually update the experiment.
			Date now = new Date();
			String updateDetails = cohortToUpdate.updateCohort(updatedCohortJSON, context, now);

			if (!updateDetails.isEmpty()) {
				change.getFiles().addAll(AirlockFilesWriter.writeCohorts(currentProduct, context));
				Webhooks.get(context).notifyChanges(change, context);

				AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
				auditLogWriter.log("Update cohort: " + cohort_id + ",   " + updateDetails, userInfo);
			}

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Updated cohort: " + cohortToUpdate.toJson() + "\n updatd details: " + updateDetails);
			}
			return (Response.ok(cohortToUpdate.toJson().toString())).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error updating cohort :", e);
			return sendAndLogError(Response.Status.INTERNAL_SERVER_ERROR, "Error updating cohort: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	@PUT
	@Path("/cohorts/{cohort-id}/execute")
	@ApiOperation(value = "Export cohort", response = String.class)
	@ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Feature not found"),
			@ApiResponse(code = 500, message = "Internal error")})
	public Response exportCohort(@PathParam("cohort-id") String cohort_id, String cohort,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("updateCohort request for id:"+cohort_id);
		}

		AirlockChange change = new AirlockChange();

		String err = Utilities.validateLegalUUID(cohort_id);
		if (err!=null)
			return sendAndLogError(Response.Status.BAD_REQUEST, Strings.illegalCohortUUID + err);

		//find relevant product
		Utilities.ProductErrorPair prodErrPair = Utilities.getProductOfCohort(context, cohort_id);
		if (prodErrPair.error != null) {
			return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		Product currentProduct = prodErrPair.product;
		change.setProduct(currentProduct);

		UserInfo userInfo = UserInfo.validate("AirlyticsServices.updateCohort", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Response.Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new Constants.AirlockCapability[]{Constants.AirlockCapability.COHORTS});
		if (capabilityValidationRes!=null)
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();

		ReentrantReadWriteLock readWriteLock = currentProduct.getCohortsProductLock();
		printLockStats(readWriteLock, "exportCohort");
		try {

			@SuppressWarnings("unchecked")
			Map<String, CohortItem> cohortsDB = (Map<String, CohortItem>)context.getAttribute(Constants.COHORTS_DB_PARAM_NAME);

			CohortItem cohortToUpdate = cohortsDB.get(cohort_id);
			if (cohortToUpdate == null) {
				String errMsg = Strings.cohortNotFound;
				logger.severe(errMsg);
				return Response.status(Response.Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			//finally - actually update the experiment.
			Date now = new Date();
			ValidationResults vr = cohortToUpdate.checkExport();
			if (vr != null) {
				String errMsg = vr.error;
				logger.severe(errMsg);
				return Response.status(Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}
			AirCohortsCallResult result = CohortsUtilities.executeCohort(context,cohortToUpdate, currentProduct.getUniqueId().toString(), assertion);
			ValidationResults validationRes = result.getValidationResults();
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}
			readWriteLock.readLock().lock();
			try {
				CohortItem updatedItem = cohortsDB.get(cohort_id);
				if (logger.isLoggable(Level.FINE)) {
					logger.fine("Updated cohort: " + updatedItem.toJson());
				}
				return (Response.ok(updatedItem.toJson().toString())).build();
			} finally {
				readWriteLock.readLock().unlock();
			}

		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error updating cohort :", e);
			return sendAndLogError(Response.Status.INTERNAL_SERVER_ERROR, "Error updating cohort: " + e.getMessage());
		}
	}

	private void printLockStats(ReentrantReadWriteLock readWriteLock, String prefix) {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest(prefix+" lock stats. WriteHoldCount:"+readWriteLock.getWriteHoldCount()+". getReadHoldCount:"+readWriteLock.getReadHoldCount()+
					". getReadLockCount:"+readWriteLock.getReadLockCount()+". isWriteLocked:"+readWriteLock.isWriteLocked()+". getQueueLength:"+readWriteLock.getQueueLength()+
					". hasQueuedThreads:"+readWriteLock.hasQueuedThreads());
		}

	}

	@PUT
	@Path("/cohorts/{cohort-id}/rename")
	@ApiOperation(value = "Export cohort", response = String.class)
	@ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Feature not found"),
			@ApiResponse(code = 500, message = "Internal error")})
	public Response renameCohort(@PathParam("cohort-id") String cohort_id, String renameObj,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("updateCohort request for id:"+cohort_id);
		}

		AirlockChange change = new AirlockChange();

		String err = Utilities.validateLegalUUID(cohort_id);
		if (err!=null)
			return sendAndLogError(Response.Status.BAD_REQUEST, Strings.illegalCohortUUID + err);

		//find relevant product
		Utilities.ProductErrorPair prodErrPair = Utilities.getProductOfCohort(context, cohort_id);
		if (prodErrPair.error != null) {
			return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		Product currentProduct = prodErrPair.product;
		change.setProduct(currentProduct);

		UserInfo userInfo = UserInfo.validate("AirlyticsServices.updateCohort", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Response.Status.UNAUTHORIZED, userInfo);
		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new Constants.AirlockCapability[]{Constants.AirlockCapability.COHORTS});
		if (capabilityValidationRes!=null)
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();

		ReentrantReadWriteLock readWriteLock = currentProduct.getCohortsProductLock();
		printLockStats(readWriteLock, "renameCohort");
		try {

			@SuppressWarnings("unchecked")
			Map<String, CohortItem> cohortsDB = (Map<String, CohortItem>)context.getAttribute(Constants.COHORTS_DB_PARAM_NAME);

			CohortItem cohortToUpdate = cohortsDB.get(cohort_id);
			if (cohortToUpdate == null) {
				String errMsg = Strings.cohortNotFound;
				logger.severe(errMsg);
				return Response.status(Response.Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}
			JSONObject renameJSON = new JSONObject(renameObj);
			String exportKey = renameJSON.getString("exportKey");
			String newExportName = renameJSON.getString("newExportName");
			String oldExportName = renameJSON.getString("oldExportName");
			if (exportKey == null || exportKey.isEmpty()) {
				return sendAndLogError(Response.Status.BAD_REQUEST, "Missing request field 'exportKey'");
			}
			if (newExportName == null || newExportName.isEmpty()) {
				return sendAndLogError(Response.Status.BAD_REQUEST, "Missing request field 'newExportName'");
			}
			if (oldExportName == null || oldExportName.isEmpty()) {
				return sendAndLogError(Response.Status.BAD_REQUEST, "Missing request field 'oldExportName'");
			}
			if (oldExportName.equals(newExportName)) {
				return sendAndLogError(Response.Status.BAD_REQUEST, "The new exportName is equal to the old one");
			}
			if (!cohortToUpdate.getExports().containsKey(exportKey)) {
				return sendAndLogError(Response.Status.BAD_REQUEST, "invalid exportKey '"+exportKey+"'");
			}

			//finally - actually update the cohort.
			AirCohortsCallResult result = CohortsUtilities.renameCohortExport(context, cohortToUpdate,exportKey, oldExportName, newExportName, currentProduct.getUniqueId().toString(), assertion);
			ValidationResults validationRes = result.getValidationResults();
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}
			readWriteLock.writeLock().lock();
			try {
				CohortItem updatedItem = cohortsDB.get(cohort_id);
				Date now = new Date();
				String updateDetails = updatedItem.updateExportName(exportKey, newExportName, now);
				if (logger.isLoggable(Level.FINE)) {
					logger.fine("Updated cohort: " + updatedItem.toJson());
				}
				if (!updateDetails.isEmpty()) {
					change.getFiles().addAll(AirlockFilesWriter.writeCohorts(currentProduct, context));
					Webhooks.get(context).notifyChanges(change, context);
				}
				return (Response.ok(updatedItem.toJson().toString())).build();
			} finally {
				readWriteLock.writeLock().unlock();
			}

		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error updating cohort :", e);
			return sendAndLogError(Response.Status.INTERNAL_SERVER_ERROR, "Error updating cohort: " + e.getMessage());
		}
	}


	@DELETE
	@Path("/cohorts/{cohort-id}/export/{export-key}")
	@ApiOperation(value = "Delete export from cohort", response = String.class)
	@ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Not found"),
			@ApiResponse(code = 500, message = "Internal error")})
	public Response deleteExport(@PathParam("cohort-id") String cohort_id, @PathParam("export-key") String export_key,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		AirlockChange change = new AirlockChange();
		String err = Utilities.validateLegalUUID(cohort_id);
		if (err!=null)
			return sendAndLogError(Response.Status.BAD_REQUEST, Strings.illegalExperimentUUID + err);

		//find relevant product
		Utilities.ProductErrorPair prodErrPair = Utilities.getProductOfCohort(context, cohort_id);
		if (prodErrPair.error != null) {
			return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		Product currentProduct = prodErrPair.product;
		change.setProduct(currentProduct);
		//check user authorization
		UserInfo userInfo = UserInfo.validate("AirlyticsServices.updateCohort", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Response.Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new Constants.AirlockCapability[]{Constants.AirlockCapability.COHORTS});
		if (capabilityValidationRes!=null)
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();

		ReentrantReadWriteLock readWriteLock = currentProduct.getCohortsProductLock();
		printLockStats(readWriteLock, "deleteExport");
		try {
			@SuppressWarnings("unchecked")
			Map<String, CohortItem> cohortsDB = (Map<String, CohortItem>)context.getAttribute(Constants.COHORTS_DB_PARAM_NAME);

			@SuppressWarnings("unchecked")
			Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);

			CohortItem cohortToUpdate = cohortsDB.get(cohort_id);
			if (cohortToUpdate == null) {
				String errMsg = Strings.cohortNotFound;
				logger.severe(errMsg);
				return Response.status(Response.Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			//remove from aircohorts
			AirCohortsCallResult aircohortsResult = CohortsUtilities.deleteCohort(context, cohortToUpdate, export_key, currentProduct.getUniqueId().toString(), assertion);
			if (aircohortsResult.getValidationResults() != null) {
				ValidationResults validationRes = aircohortsResult.getValidationResults();
				return Response.status(validationRes.status).entity("failed to delete cohort in airCohorts:"+validationRes.error).build();
			}
			readWriteLock.writeLock().lock();
			try {
				//re-get cohort
				cohortToUpdate = cohortsDB.get(cohort_id);
				//actually update the cohort
				Date now = new Date();
				String updateDetails = cohortToUpdate.deleteExport(export_key, context, now);

				if (!updateDetails.isEmpty()) {
					change.getFiles().addAll(AirlockFilesWriter.writeCohorts(currentProduct, context));
					Webhooks.get(context).notifyChanges(change, context);
				}
				if (logger.isLoggable(Level.FINE)) {
					logger.fine("Updated cohort: " + cohortToUpdate.toJson() + "\n updatd details: " + updateDetails);
				}
				return (Response.ok(cohortToUpdate.toJson().toString())).build();
			} finally {
				readWriteLock.writeLock().unlock();
			}


		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error deleting export_key "+export_key+" from cohort " + cohort_id + ": ", e);
			return sendAndLogError(Response.Status.INTERNAL_SERVER_ERROR, "Error deleting export key from cohort: " + e.getMessage());
		}
	}
	@DELETE
	@Path("/cohorts/{cohort-id}")
	@ApiOperation(value = "Delete cohort", response = String.class)
	@ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Not found"),
			@ApiResponse(code = 500, message = "Internal error")})
	public Response deleteCohort(@PathParam("cohort-id") String cohort_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		AirlockChange change = new AirlockChange();
		String err = Utilities.validateLegalUUID(cohort_id);
		if (err!=null)
			return sendAndLogError(Response.Status.BAD_REQUEST, Strings.illegalExperimentUUID + err);

		//find relevant product
		Utilities.ProductErrorPair prodErrPair = Utilities.getProductOfCohort(context, cohort_id);
		if (prodErrPair.error != null) {
			return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		Product currentProduct = prodErrPair.product;
		change.setProduct(currentProduct);
		//check user authorization
		UserInfo userInfo = UserInfo.validate("AirlyticsServices.deleteCohort", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Response.Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new Constants.AirlockCapability[]{Constants.AirlockCapability.COHORTS});
		if (capabilityValidationRes!=null)
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();

		ReentrantReadWriteLock readWriteLock = currentProduct.getCohortsProductLock();
		printLockStats(readWriteLock, "deleteCohort");
		try {
			@SuppressWarnings("unchecked")
			Map<String, CohortItem> cohortsDB = (Map<String, CohortItem>)context.getAttribute(Constants.COHORTS_DB_PARAM_NAME);

			@SuppressWarnings("unchecked")
			Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);

			CohortItem cohortToDelete = cohortsDB.get(cohort_id);
			if (cohortToDelete == null) {
				String errMsg = Strings.cohortNotFound;
				logger.severe(errMsg);
				return Response.status(Response.Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			//remove from aircohorts
			AirCohortsCallResult aircohortsResult = CohortsUtilities.deleteCohort(context, cohortToDelete, null, currentProduct.getUniqueId().toString(), assertion);
			if (aircohortsResult.getValidationResults() != null) {
				ValidationResults validationRes = aircohortsResult.getValidationResults();
				return Response.status(validationRes.status).entity("failed to delete cohort in airCohorts:"+validationRes.error).build();
			}
			readWriteLock.writeLock().lock();
			try {
				//actually removing the cohort
				String errorString = currentProduct.removeCohort(cohortToDelete.getUniqueId());
				if (errorString!=null) {
					//should not happen - will be returned if cohort is not in product
					logger.severe(errorString);
					return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errorString)).build();
				}

				cohortsDB.remove(cohort_id);
				try {
					change.setProduct(currentProduct);
					change.getFiles().addAll(AirlockFilesWriter.writeCohorts(currentProduct, context));
					Webhooks.get(context).notifyChanges(change, context);
				} catch (IOException e) {
					return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
				}

				AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
				auditLogWriter.log("Delete cohort: uniqueId: " + cohort_id, userInfo); 

				return (Response.ok()).build();
			} finally {
				readWriteLock.writeLock().unlock();
			}

		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error deleting cohort " + cohort_id + ": ", e);
			return sendAndLogError(Response.Status.INTERNAL_SERVER_ERROR, "Error deleting cohort: " + e.getMessage());
		}
	}
	@PUT
	@Path("/cohorts/{cohort-id}/status")
	@ApiOperation(value = "Update cohort status", response = String.class)
	@ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Feature not found"),
			@ApiResponse(code = 500, message = "Internal error")})
	public Response updateCohortStatus(@PathParam("cohort-id") String cohort_id, String status,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("updateCohortStatus request for id:"+cohort_id);
		}

		logger.info("status for update ("+cohort_id+"):"+status);
		AirlockChange change = new AirlockChange();

		String err = Utilities.validateLegalUUID(cohort_id);
		if (err!=null)
			return sendAndLogError(Response.Status.BAD_REQUEST, Strings.illegalCohortUUID + err);

		//find relevant product
		Utilities.ProductErrorPair prodErrPair = Utilities.getProductOfCohort(context, cohort_id);
		if (prodErrPair.error != null) {
			return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		Product currentProduct = prodErrPair.product;
		change.setProduct(currentProduct);

		UserInfo userInfo = UserInfo.validate("AirlyticsServices.updateCohort", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Response.Status.UNAUTHORIZED, userInfo);
		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new Constants.AirlockCapability[]{Constants.AirlockCapability.COHORTS});
		if (capabilityValidationRes!=null)
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();

		ReentrantReadWriteLock readWriteLock = currentProduct.getCohortsProductLock();
		printLockStats(readWriteLock, "updateCohortStatus");
		readWriteLock.writeLock().lock();
		try {
			@SuppressWarnings("unchecked")
			Map<String, CohortItem> cohortsDB = (Map<String, CohortItem>)context.getAttribute(Constants.COHORTS_DB_PARAM_NAME);

			CohortItem cohortToUpdate = cohortsDB.get(cohort_id);
			if (cohortToUpdate == null) {
				String errMsg = Strings.cohortNotFound;
				logger.severe(errMsg);
				return Response.status(Response.Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			JSONObject updatedCohortJSON = null;
			try {
				updatedCohortJSON = new JSONObject(status);
			} catch (JSONException je) {
				String errMsg = Strings.illegalInputJSON + je.getMessage();
				logger.severe(errMsg);
				return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}
			if (!updatedCohortJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) || updatedCohortJSON.get(Constants.JSON_FIELD_UNIQUE_ID) == null) {
				updatedCohortJSON.put(Constants.JSON_FIELD_UNIQUE_ID, cohort_id);
			}
			else {
				//verify that cohort-id in path is identical to uniqueId in request pay-load
				if (!updatedCohortJSON.getString(Constants.JSON_FIELD_UNIQUE_ID).equals(cohort_id)) {
					String errMsg = Strings.cohortWithDifferentId;
					logger.severe(errMsg);
					return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
				}
			}

			ValidationResults validationRes = cohortToUpdate.validateCohortStatusJSON(updatedCohortJSON, context);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			//finally - actually update the status.
			Date now = new Date();
			String updateDetails = cohortToUpdate.updateCohortStatus(updatedCohortJSON, context, now);

			if (!updateDetails.isEmpty()) {
				currentProduct.setCohortsWriteNeeded(true);

				AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
				auditLogWriter.log("Update cohort status: " + cohort_id + ",   " + updateDetails, userInfo);

			}
			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Updated cohort: " + cohortToUpdate.toJson() + "\n updatd details: " + updateDetails);
			}
			logger.info("Updated cohort: " + cohortToUpdate.toJson() + "\n updatd details: " + updateDetails);
			return (Response.ok(cohortToUpdate.toJson().toString())).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error updating cohort :", e);
			return sendAndLogError(Response.Status.INTERNAL_SERVER_ERROR, "Error updating cohort: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}
	@PUT
	@Path("/products/{product-id}/cohorts")
	@ApiOperation(value = "Update cohorts data", response = String.class)
	@ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Not found"),
			@ApiResponse(code = 500, message = "Internal error")})
	public Response updateCohortsData(@PathParam("product-id") String product_id,
			String cohortsData,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("updateCohortsData request for product:"+product_id);
		}

		AirlockChange change = new AirlockChange();

		String err = Utilities.validateLegalUUID(product_id);
		if (err!=null)
			return sendAndLogError(Response.Status.BAD_REQUEST, Strings.illegalProductUUID + err);

		//find relevant product
		Utilities.ProductErrorPair productErrorPair = Utilities.getProduct(context, product_id);
		if (productErrorPair.error != null) {
			return Response.status(Response.Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);

		//check user authorization
		UserInfo userInfo = UserInfo.validate("AirlyticsServices.updateCohortsData", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Response.Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new Constants.AirlockCapability[]{Constants.AirlockCapability.COHORTS});
		if (capabilityValidationRes!=null)
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();

		//validate that is a legal JSON
		JSONObject newCohortsDataJSON = null;
		try {
			newCohortsDataJSON = new JSONObject(cohortsData);
		} catch (JSONException je) {
			String errMsg = Strings.illegalInputJSON + je.getMessage();
			logger.severe(errMsg);
			return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
		}

		ReentrantReadWriteLock readWriteLock = currentProduct.getCohortsProductLock();
		printLockStats(readWriteLock, "updateCohortsData");
		readWriteLock.writeLock().lock();
		try {
			String updateDetails = currentProduct.updateAirlockCohorts(newCohortsDataJSON);
			if (!updateDetails.isEmpty()) {
				change.getFiles().addAll(AirlockFilesWriter.writeCohorts(currentProduct, context));
				Webhooks.get(context).notifyChanges(change, context);

				AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
				auditLogWriter.log("Update cohort data for product: " + product_id + ",   " + updateDetails, userInfo);
			}
			return (Response.ok(updateDetails.toString())).build();
		} catch (JSONException | IOException e) {
			logger.log(Level.SEVERE, "Error updating airlock cohorts action on product-id" + product_id + ": ", e);
			return sendAndLogError(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
		}
		finally {
			readWriteLock.writeLock().unlock();
		}
	}
	@POST
	@Path("/products/{product-id}/cohorts")
	@ApiOperation(value = "Create cohort", response = String.class)
	@ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Feature not found"),
			@ApiResponse(code = 500, message = "Internal error")})
	public Response createCohort(@PathParam("product-id") String product_id,
			String newCohort,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("createCohort request for product:"+product_id);
		}
		logger.info("createCohort request for product:"+product_id);

		AirlockChange change = new AirlockChange();

		String err = Utilities.validateLegalUUID(product_id);
		if (err!=null)
			return sendAndLogError(Response.Status.BAD_REQUEST, Strings.illegalProductUUID + err);

		//find relevant product
		Utilities.ProductErrorPair productErrorPair = Utilities.getProduct(context, product_id);
		if (productErrorPair.error != null) {
			return Response.status(Response.Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);

		//check user authorization
		UserInfo userInfo = UserInfo.validate("AirlyticsServices.createCohort", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Response.Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new Constants.AirlockCapability[]{Constants.AirlockCapability.COHORTS});
		if (capabilityValidationRes!=null)
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();

		//validate that is a legal JSON
		JSONObject newCohortJSON = null;
		try {
			newCohortJSON = new JSONObject(newCohort);
		} catch (JSONException je) {
			String errMsg = Strings.illegalInputJSON + je.getMessage();
			logger.severe(errMsg);
			return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
		}

		//verify that JSON does not contain uniqueId field
		if (newCohortJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && newCohortJSON.get(Constants.JSON_FIELD_UNIQUE_ID) !=null) {
			String errMsg = Strings.audienceWithId;
			logger.severe(errMsg);
			return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
		}

		//verify that JSON does not contain different product-id then the path parameter
		if (newCohortJSON.containsKey(Constants.JSON_FIELD_PRODUCT_ID) && newCohortJSON.get(Constants.JSON_FIELD_PRODUCT_ID) !=null) {
			if (!product_id.equals(newCohortJSON.getString(Constants.JSON_FIELD_PRODUCT_ID))) {
				String errMsg = Strings.audienceProductWithDifferentId;
				logger.severe(errMsg);
				return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}
		}
		else {
			newCohortJSON.put(Constants.JSON_FIELD_PRODUCT_ID, product_id);
		}

		ReentrantReadWriteLock readWriteLock = currentProduct.getCohortsProductLock();
		printLockStats(readWriteLock, "createCohort");
		readWriteLock.writeLock().lock();
		try {
			CohortItem newCohortObj = new CohortItem(currentProduct.getUniqueId());

			@SuppressWarnings("unchecked")
			Map<String, CohortItem> cohortsDB = (Map<String, CohortItem>)context.getAttribute(Constants.COHORTS_DB_PARAM_NAME);
			ValidationResults validationRes = newCohortObj.validateCohortJSON(newCohortJSON, context, userInfo);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			newCohortObj.fromJSON(newCohortJSON);
			newCohortObj.setUniqueId(UUID.randomUUID());

			if (newCohortObj.getExports().containsKey(COHORT_EXPORT_TYPE_LOCALYTICS)){
				String errMsg = "Localytics export type is no longer supported";
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			currentProduct.addCohort(newCohortObj);
			cohortsDB.put(newCohortObj.getUniqueId().toString(), newCohortObj);

			change.getFiles().addAll(AirlockFilesWriter.writeCohorts(currentProduct, context));
			Webhooks.get(context).notifyChanges(change, context);


			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Create new cohort: " + newCohortObj.getName() + ", " + newCohortObj.getUniqueId() + ":" + newCohortObj.toJson().toString(), userInfo);
			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FIELD_UNIQUE_ID, newCohortObj.getUniqueId().toString());
			res.put(Constants.JSON_FIELD_NAME, newCohortObj.getName());
			res.put(Constants.JSON_FIELD_LAST_MODIFIED, newCohortObj.getLastModified().getTime());

			logger.info("Cohort added to product '"+  product_id + "': "+ res.toString());

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Added cohort: " + newCohortObj.toJson());
			}
			return (Response.ok(res.toString())).build();
		} catch (IOException e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	@POST
	@Path("/products/{product-id}/cohorts/validate")
	@ApiOperation(value = "Validate cohort query", response = String.class)
	@ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Feature not found"),
			@ApiResponse(code = 500, message = "Internal error")})
	public Response validateCohort(@PathParam("product-id") String product_id,
								   String queryValidation,
								   @HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("createCohort request for product:"+product_id);
		}


		String err = Utilities.validateLegalUUID(product_id);
		if (err!=null)
			return sendAndLogError(Response.Status.BAD_REQUEST, Strings.illegalProductUUID + err);

		//find relevant product
		Utilities.ProductErrorPair productErrorPair = Utilities.getProduct(context, product_id);
		if (productErrorPair.error != null) {
			return Response.status(Response.Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;

		//check user authorization
		UserInfo userInfo = UserInfo.validate("AirlyticsServices.validateCohort", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Response.Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new Constants.AirlockCapability[]{Constants.AirlockCapability.COHORTS});
		if (capabilityValidationRes!=null)
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();

		//validate that is a legal JSON
		JSONObject newValidationJSON = null;
		try {
			newValidationJSON = new JSONObject(queryValidation);
		} catch (JSONException je) {
			String errMsg = Strings.illegalInputJSON + je.getMessage();
			logger.severe(errMsg);
			return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
		}

		//verify that JSON does not contain different product-id then the path parameter
		if (newValidationJSON.containsKey(Constants.JSON_FIELD_PRODUCT_ID) && newValidationJSON.get(Constants.JSON_FIELD_PRODUCT_ID) !=null) {
			if (!product_id.equals(newValidationJSON.getString(Constants.JSON_FIELD_PRODUCT_ID))) {
				String errMsg = Strings.audienceProductWithDifferentId;
				logger.severe(errMsg);
				return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}
		}
		else {
			newValidationJSON.put(Constants.JSON_FIELD_PRODUCT_ID, product_id);
		}

		ReentrantReadWriteLock readWriteLock = currentProduct.getCohortsProductLock();
		printLockStats(readWriteLock, "validateCohort");
		readWriteLock.readLock().lock();
		try {
			QueryValidationItem neQueryObj = new QueryValidationItem(currentProduct.getUniqueId());

			@SuppressWarnings("unchecked")
			ValidationResults validationRes = neQueryObj.validateItemJSON(newValidationJSON);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			neQueryObj.fromJSON(newValidationJSON);
			AirCohortsCallResult result = CohortsUtilities.validateQuery(context, neQueryObj, false, assertion);
			ValidationResults validationResults = result.getValidationResults();
			if (validationResults != null) {
				String errMsg = validationResults.error;
				logger.severe(errMsg);
				return Response.status(validationResults.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			JSONObject res = result.getItem().toJson();
			if (logger.isLoggable(Level.FINE)) {
				logger.fine("validated query: " + neQueryObj.toJson());
			}
			return (Response.ok(res.toString())).build();
		} catch (Exception e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	@POST
	@Path("/products/{product-id}/cohorts/validateQuery")
	@ApiOperation(value = "Validate cohort query", response = String.class)
	@ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Feature not found"),
			@ApiResponse(code = 500, message = "Internal error")})
	public Response validateCohortBeforeSave(@PathParam("product-id") String product_id,
			String queryValidation,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("createCohort request for product:"+product_id);
		}


		String err = Utilities.validateLegalUUID(product_id);
		if (err!=null)
			return sendAndLogError(Response.Status.BAD_REQUEST, Strings.illegalProductUUID + err);

		//find relevant product
		Utilities.ProductErrorPair productErrorPair = Utilities.getProduct(context, product_id);
		if (productErrorPair.error != null) {
			return Response.status(Response.Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;

		//check user authorization
		UserInfo userInfo = UserInfo.validate("AirlyticsServices.validateCohort", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Response.Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new Constants.AirlockCapability[]{Constants.AirlockCapability.COHORTS});
		if (capabilityValidationRes!=null)
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();

		//validate that is a legal JSON
		JSONObject newValidationJSON = null;
		try {
			newValidationJSON = new JSONObject(queryValidation);
		} catch (JSONException je) {
			String errMsg = Strings.illegalInputJSON + je.getMessage();
			logger.severe(errMsg);
			return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
		}

		//verify that JSON does not contain different product-id then the path parameter
		if (newValidationJSON.containsKey(Constants.JSON_FIELD_PRODUCT_ID) && newValidationJSON.get(Constants.JSON_FIELD_PRODUCT_ID) !=null) {
			if (!product_id.equals(newValidationJSON.getString(Constants.JSON_FIELD_PRODUCT_ID))) {
				String errMsg = Strings.audienceProductWithDifferentId;
				logger.severe(errMsg);
				return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}
		}
		else {
			newValidationJSON.put(Constants.JSON_FIELD_PRODUCT_ID, product_id);
		}

		ReentrantReadWriteLock readWriteLock = currentProduct.getCohortsProductLock();
		printLockStats(readWriteLock, "validateCohortBeforeSave");
		readWriteLock.readLock().lock();
		try {
			QueryValidationItem neQueryObj = new QueryValidationItem(currentProduct.getUniqueId());

			@SuppressWarnings("unchecked")
			ValidationResults validationRes = neQueryObj.validateItemJSON(newValidationJSON);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			neQueryObj.fromJSON(newValidationJSON);
			AirCohortsCallResult result = CohortsUtilities.validateQuery(context, neQueryObj, true, assertion);
			ValidationResults validationResults = result.getValidationResults();
			if (validationResults != null) {
				String errMsg = validationResults.error;
				logger.severe(errMsg);
				return Response.status(validationResults.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			JSONObject res = result.getItem().toJson();
			if (logger.isLoggable(Level.FINE)) {
				logger.fine("validated query: " + neQueryObj.toJson());
			}
			return (Response.ok(res.toString())).build();
		} catch (Exception e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	@GET
	@Path("/products/{product-id}/entities")
	@ApiOperation(value = "Returns the entities list for the specified product", response = String.class)
	@ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 500, message = "Internal error")})
	public Response getEntities(@PathParam("product-id") String product_id, @HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getEntities request");
		}

		String err = Utilities.validateLegalUUID(product_id);
		if (err!=null)
			return sendAndLogError(Response.Status.BAD_REQUEST, Strings.illegalProductUUID + err);

		//find relevant product
		Utilities.ProductErrorPair prodErrPair = Utilities.getProduct(context, product_id);
		if (prodErrPair.error != null) {
			return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		Product currentProduct = prodErrPair.product;

		UserInfo userInfo = UserInfo.validate("AirlyticsServices.getEntities", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Response.Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new Constants.AirlockCapability[]{Constants.AirlockCapability.ENTITIES});
		if (capabilityValidationRes!=null)
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();

		ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
		readWriteLock.readLock().lock();
		try {
			JSONObject entitiesJson = currentProduct.getEntities().toJSON(true, userInfo, context, OutputJSONMode.DISPLAY);

			return (Response.ok(entitiesJson.toString())).build();
		} catch (Throwable e) {
			return sendAndLogError(Response.Status.INTERNAL_SERVER_ERROR, "Error getting entities: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	@GET
	@Path("/products/entities/{entity-id}")
	@ApiOperation(value = "Returns the specified entity", response = String.class)
	@ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Entity not found"),
			@ApiResponse(code = 500, message = "Internal error")})
	public Response getEntity(@PathParam("entity-id") String entity_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getEntity request");
		}

		String err = Utilities.validateLegalUUID(entity_id);
		if (err!=null)
			return sendAndLogError(Response.Status.BAD_REQUEST, Strings.illegalEntityUUID + err);


		//find relevant product
		Utilities.ProductErrorPair prodErrPair = Utilities.getProductOfEntity(context, entity_id);
		if (prodErrPair.error != null) {
			return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		Product currentProduct = prodErrPair.product;

		UserInfo userInfo = UserInfo.validate("AirlyticsServices.getEntity", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Response.Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new Constants.AirlockCapability[]{Constants.AirlockCapability.ENTITIES});
		if (capabilityValidationRes!=null)
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.readLock().lock();
		try {
			@SuppressWarnings("unchecked")
			Map<String, Entity> entitiesDB = (Map<String, Entity>)context.getAttribute(Constants.ENTITIES_DB_PARAM_NAME);

			Entity entity = entitiesDB.get(entity_id);

			if (entity == null) {
				return sendAndLogError(Response.Status.NOT_FOUND, Strings.entityNotFound);
			}

			JSONObject entityJSON = entity.toJson(userInfo, context, OutputJSONMode.DISPLAY);
			return (Response.ok(entityJSON.toString())).build();
		} catch (Throwable e) {
			return sendAndLogError(Response.Status.INTERNAL_SERVER_ERROR, "Error getting entity: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	@PUT
	@Path("/products/entities/{entity-id}")
	@ApiOperation(value = "Updates the specified entity", response = String.class)
	@ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Entity not found"),
			@ApiResponse(code = 500, message = "Internal error")})
	public Response updateEntity(@PathParam("entity-id") String entity_id, String entity,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("updateEntity request for id:"+entity_id);
		}

		AirlockChange change = new AirlockChange();

		String err = Utilities.validateLegalUUID(entity_id);
		if (err!=null) {
			return sendAndLogError(Response.Status.BAD_REQUEST, Strings.illegalCohortUUID + err);
		}
		//find relevant product
		Utilities.ProductErrorPair prodErrPair = Utilities.getProductOfEntity(context, entity_id);
		if (prodErrPair.error != null) {
			return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		Product currentProduct = prodErrPair.product;
		change.setProduct(currentProduct);

		UserInfo userInfo = UserInfo.validate("AirlyticsServices.updateEntity", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null) {
			return sendInfoError(Response.Status.UNAUTHORIZED, userInfo);
		}
		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new Constants.AirlockCapability[]{Constants.AirlockCapability.ENTITIES});
		if (capabilityValidationRes!=null)
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.writeLock().lock();
		try {
			@SuppressWarnings("unchecked")
			Map<String, Entity> entitiessDB = (Map<String, Entity>)context.getAttribute(Constants.ENTITIES_DB_PARAM_NAME);

			Entity entityToUpdate = entitiessDB.get(entity_id);
			if (entityToUpdate == null) {
				return sendAndLogError(Response.Status.NOT_FOUND, Strings.entityNotFound);
			}

			JSONObject updatedEntityJSON = null;
			try {
				updatedEntityJSON = new JSONObject(entity);
			} catch (JSONException je) {
				return sendAndLogError(Response.Status.BAD_REQUEST, Strings.illegalInputJSON + je.getMessage());
			}

			if (!updatedEntityJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) || updatedEntityJSON.get(Constants.JSON_FIELD_UNIQUE_ID) == null) {
				updatedEntityJSON.put(Constants.JSON_FIELD_UNIQUE_ID, entity_id);
			}
			else {
				//verify that product-id in path is identical to uniqueId in request pay-load
				if (!updatedEntityJSON.getString(Constants.JSON_FIELD_UNIQUE_ID).equals(entity_id)) {
					return sendAndLogError(Response.Status.BAD_REQUEST,  Strings.entityWithDifferentId);
				}
			}

			ValidationResults validationRes = entityToUpdate.validateJSON(updatedEntityJSON, context);
			if (validationRes!=null) {
				return sendAndLogError(validationRes.status, validationRes.error);
			}

			//finally - actually update the experiment.
			String updateDetails = entityToUpdate.updateEntity(updatedEntityJSON);

			if (!updateDetails.isEmpty()) {
				change.getFiles().addAll(AirlockFilesWriter.writeProductEntities(currentProduct, context, userInfo));
				Webhooks.get(context).notifyChanges(change, context);

				AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
				auditLogWriter.log("Update entity: " + entity_id + ",   " + updateDetails, userInfo);
			}

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Updated entity: " + entityToUpdate.toJson(userInfo, context, OutputJSONMode.ADMIN) + "\n updatd details: " + updateDetails);
			}
			return (Response.ok(entityToUpdate.toJson().toString())).build();
		} catch (Throwable e) {
			return sendAndLogError(Response.Status.INTERNAL_SERVER_ERROR, "Error updating entity: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	//TODO: delete  entity deletes it from the db as well? (createEntity doesnt creates te table ....)
	@DELETE
	@Path("/products/entities/{entity-id}")
	@ApiOperation(value = "Deletes the specified entity", response = String.class)
	@ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Entity not found"),
			@ApiResponse(code = 500, message = "Internal error")})
	public Response deleteEntity(@PathParam("entity-id") String entity_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("deleteEntity request");
		}
		AirlockChange change = new AirlockChange();
		String err = Utilities.validateLegalUUID(entity_id);
		if (err!=null)
			return sendAndLogError(Response.Status.BAD_REQUEST, Strings.illegalEntityUUID + err);

		//find relevant product
		Utilities.ProductErrorPair prodErrPair = Utilities.getProductOfEntity(context, entity_id);
		if (prodErrPair.error != null) {
			return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}

		Product currentProduct = prodErrPair.product;
		change.setProduct(currentProduct);
		//check user authorization
		UserInfo userInfo = UserInfo.validate("AirlyticsServices.deleteEntity", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Response.Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new Constants.AirlockCapability[]{Constants.AirlockCapability.ENTITIES});
		if (capabilityValidationRes!=null)
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.writeLock().lock();
		try {
			@SuppressWarnings("unchecked")
			Map<String, Entity> entitiesDB = (Map<String, Entity>)context.getAttribute(Constants.ENTITIES_DB_PARAM_NAME);

			Entity entityToDelete = entitiesDB.get(entity_id);
			if (entityToDelete == null) {
				return sendAndLogError(Response.Status.NOT_FOUND, Strings.entityNotFound);
			}

			String errorString = entityToDelete.deleteEntity(context, currentProduct);
			if (errorString!=null) {
				return sendAndLogError(Response.Status.BAD_REQUEST, errorString);
			}

			try {
				change.setProduct(currentProduct);
				change.getFiles().addAll(AirlockFilesWriter.writeProductEntities(currentProduct, context, userInfo));
				Webhooks.get(context).notifyChanges(change, context);
			} catch (IOException e) {
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}

			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Delete entity: uniqueId: " + entity_id, userInfo); 

			return (Response.ok()).build();
		} catch (Throwable e) {
			return sendAndLogError(Response.Status.INTERNAL_SERVER_ERROR, "Error deleting entity: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		} 
	}

	@POST
	@Path("/products/{product-id}/entities")
	@ApiOperation(value = "Creates a new entity for the specified product", response = String.class)
	@ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Product not found"),
			@ApiResponse(code = 500, message = "Internal error")})
	public Response createEntity(@PathParam("product-id") String product_id, String newEntity,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("createEntity request for product:"+product_id);
		}

		AirlockChange change = new AirlockChange();

		String err = Utilities.validateLegalUUID(product_id);
		if (err!=null)
			return sendAndLogError(Response.Status.BAD_REQUEST, Strings.illegalProductUUID + err);

		//find relevant product
		Utilities.ProductErrorPair productErrorPair = Utilities.getProduct(context, product_id);
		if (productErrorPair.error != null) {
			return Response.status(Response.Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);

		//check user authorization
		UserInfo userInfo = UserInfo.validate("AirlyticsServices.createEntity", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Response.Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new Constants.AirlockCapability[]{Constants.AirlockCapability.ENTITIES});
		if (capabilityValidationRes!=null)
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.writeLock().lock();
		try {
			//validate that is a legal JSON
			JSONObject newEntityJSON = null;
			try {
				newEntityJSON = new JSONObject(newEntity);
			} catch (JSONException je) {
				return sendAndLogError(Response.Status.BAD_REQUEST, Strings.illegalInputJSON + je.getMessage());
			}

			//verify that JSON does not contain uniqueId field
			if (newEntityJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && newEntityJSON.get(Constants.JSON_FIELD_UNIQUE_ID) !=null) {
				return sendAndLogError (Response.Status.BAD_REQUEST, Strings.entityWithId);
			}

			//verify that JSON does not contain different product-id than the path parameter
			if (newEntityJSON.containsKey(Constants.JSON_FIELD_PRODUCT_ID) && newEntityJSON.get(Constants.JSON_FIELD_PRODUCT_ID) !=null) {
				if (!product_id.equals(newEntityJSON.getString(Constants.JSON_FIELD_PRODUCT_ID))) {
					return sendAndLogError (Response.Status.BAD_REQUEST, Strings.entityProductWithDifferentId);
				}
			}
			else {
				newEntityJSON.put(Constants.JSON_FIELD_PRODUCT_ID, product_id);
			}

			Entity newEntityObj = new Entity(currentProduct.getUniqueId());

			ValidationResults validationRes = newEntityObj.validateJSON(newEntityJSON, context);
			if (validationRes!=null) {
				return sendAndLogError(validationRes.status, validationRes.error);
			}

			newEntityObj.fromJSON(newEntityJSON, null, null);
			newEntityObj.setUniqueId(UUID.randomUUID());

			@SuppressWarnings("unchecked")
			Map<String, Entity> entitiesDB = (Map<String, Entity>)context.getAttribute(Constants.ENTITIES_DB_PARAM_NAME);				

			entitiesDB.put(newEntityObj.getUniqueId().toString(), newEntityObj);
			currentProduct.addEntity(newEntityObj);

			try {
				change.getFiles().addAll(AirlockFilesWriter.writeProductEntities(currentProduct, context, userInfo));
				Webhooks.get(context).notifyChanges(change, context);
			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}

			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Create new entity: " + newEntityObj.toJson(userInfo, context, OutputJSONMode.ADMIN).toString(), userInfo); 

			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FIELD_UNIQUE_ID, newEntityObj.getUniqueId().toString());
			res.put(Constants.JSON_FIELD_NAME, newEntityObj.getName());
			res.put(Constants.JSON_FIELD_LAST_MODIFIED, newEntityObj.getLastModified().getTime());

			logger.info("Entity added to product '"+  product_id + "': "+ res.toString());

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Added entity: " + newEntityObj.toJson());
			}

			return (Response.ok(res.toString())).build();
		} catch (Throwable e) {
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error adding entity: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	@GET
	@Path("/products/entities/{entity-id}/attributes")
	@ApiOperation(value = "Returns the attributes list for the specified entity", response = String.class)
	@ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Entity not found"),
			@ApiResponse(code = 500, message = "Internal error")})
	public Response getAttributes(@PathParam("entity-id") String entity_id, @HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getAttributes request");
		}

		String err = Utilities.validateLegalUUID(entity_id);
		if (err!=null)
			return sendAndLogError(Response.Status.BAD_REQUEST, Strings.illegalProductUUID + err);

		//find relevant product
		Utilities.ProductErrorPair prodErrPair = Utilities.getProductOfEntity(context, entity_id);
		if (prodErrPair.error != null) {
			return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		Product currentProduct = prodErrPair.product;

		UserInfo userInfo = UserInfo.validate("AirlyticsServices.getAttributes", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Response.Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new Constants.AirlockCapability[]{Constants.AirlockCapability.ENTITIES});
		if (capabilityValidationRes!=null)
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();

		ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
		readWriteLock.readLock().lock();
		try {
			@SuppressWarnings("unchecked")
			Map<String, Entity> entitiesDB = (Map<String, Entity>)context.getAttribute(Constants.ENTITIES_DB_PARAM_NAME);
			Entity entity = entitiesDB.get(entity_id);
			if (entity == null) {
				return sendAndLogError(Response.Status.NOT_FOUND, Strings.entityNotFound);
			}
			JSONObject attributesJson = entity.attributesToJson(userInfo, context, OutputJSONMode.DISPLAY);

			return (Response.ok(attributesJson.toString())).build();
		} catch (Throwable e) {
			return sendAndLogError(Response.Status.INTERNAL_SERVER_ERROR, "Error getting attributes: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	@GET
	@Path("/products/entities/{entity-id}/attributetypes")
	@ApiOperation(value = "Returns the attribute types list for the specified entity", response = String.class)
	@ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Entity not found"),
			@ApiResponse(code = 500, message = "Internal error")})
	public Response getAttributeTypes(@PathParam("entity-id") String entity_id, @HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getAttributeTypes request");
		}

		String err = Utilities.validateLegalUUID(entity_id);
		if (err!=null)
			return sendAndLogError(Response.Status.BAD_REQUEST, Strings.illegalProductUUID + err);

		//find relevant product
		Utilities.ProductErrorPair prodErrPair = Utilities.getProductOfEntity(context, entity_id);
		if (prodErrPair.error != null) {
			return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		Product currentProduct = prodErrPair.product;

		UserInfo userInfo = UserInfo.validate("AirlyticsServices.getAttributeTypes", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Response.Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new Constants.AirlockCapability[]{Constants.AirlockCapability.ENTITIES});
		if (capabilityValidationRes!=null)
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();

		ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
		readWriteLock.readLock().lock();
		try {
			@SuppressWarnings("unchecked")
			Map<String, Entity> entitiesDB = (Map<String, Entity>)context.getAttribute(Constants.ENTITIES_DB_PARAM_NAME);
			Entity entity = entitiesDB.get(entity_id);
			if (entity == null) {
				return sendAndLogError(Response.Status.NOT_FOUND, Strings.entityNotFound);
			}
			JSONObject attributeTypesJson = entity.attributeTypesToJson();

			return (Response.ok(attributeTypesJson.toString())).build();
		} catch (Throwable e) {
			return sendAndLogError(Response.Status.INTERNAL_SERVER_ERROR, "Error getting attribute types: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	@GET
	@Path("/products/entities/attributes/{attribute-id}")
	@ApiOperation(value = "Returns the specified attribute", response = String.class)
	@ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Attribute not found"),
			@ApiResponse(code = 500, message = "Internal error")})
	public Response getAttribute(@PathParam("attribute-id") String attribute_id, @HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getAttribute request");
		}

		String err = Utilities.validateLegalUUID(attribute_id);
		if (err!=null)
			return sendAndLogError(Response.Status.BAD_REQUEST, Strings.illegalAttributeUUID + err);

		//find relevant product
		Utilities.ProductErrorPair prodErrPair = Utilities.getProductOfEntityAttribute(context, attribute_id);
		if (prodErrPair.error != null) {
			return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		Product currentProduct = prodErrPair.product;

		UserInfo userInfo = UserInfo.validate("AirlyticsServices.getAttribute", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Response.Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new Constants.AirlockCapability[]{Constants.AirlockCapability.ENTITIES});
		if (capabilityValidationRes!=null)
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.readLock().lock();
		try {
			@SuppressWarnings("unchecked")
			Map<String, Attribute> attributesDB = (Map<String, Attribute>)context.getAttribute(Constants.ATTRIBUTES_DB_PARAM_NAME);

			Attribute attribute = attributesDB.get(attribute_id);

			if (attribute == null) {
				return sendAndLogError(Response.Status.NOT_FOUND, Strings.attributeNotFound);
			}
			
			ValidationResults validateAccess = attribute.validateAttributeAccess(userInfo, AttributesPermission.READ_ONLY, context);
			if (validateAccess != null) {
				return sendAndLogError(validateAccess.status, validateAccess.error);
			}
			
			JSONObject attributeJSON = attribute.toJson();
			return (Response.ok(attributeJSON.toString())).build();
		} catch (Throwable e) {
			return sendAndLogError(Response.Status.INTERNAL_SERVER_ERROR, "Error getting attribute: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	@POST
	@Path("/products/entities/{entity-id}/attributes")
	@ApiOperation(value = "Creates an attribute for the specified entity", response = String.class)
	@ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Entity not found"),
			@ApiResponse(code = 500, message = "Internal error")})
	public Response createAttribute(@PathParam("entity-id") String entity_id, String newAttribute, 
			@QueryParam("ignoreexistence")Boolean ignoreExistence, @HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("createAttribute request for entity: "+ entity_id);
		}

		AirlockChange change = new AirlockChange();

		String err = Utilities.validateLegalUUID(entity_id);
		if (err!=null)
			return sendAndLogError(Response.Status.BAD_REQUEST, Strings.illegalProductUUID + err);

		//find relevant product
		Utilities.ProductErrorPair productErrorPair = Utilities.getProductOfEntity(context, entity_id);
		if (productErrorPair.error != null) {
			return Response.status(Response.Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);

		//check user authorization
		UserInfo userInfo = UserInfo.validate("AirlyticsServices.createAttribute", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Response.Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new Constants.AirlockCapability[]{Constants.AirlockCapability.ENTITIES});
		if (capabilityValidationRes!=null)
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.writeLock().lock();
		try {
			//validate that is a legal JSON
			JSONObject newAttributeJSON = null;
			try {
				newAttributeJSON = new JSONObject(newAttribute);
			} catch (JSONException je) {
				return sendAndLogError(Response.Status.BAD_REQUEST, Strings.illegalInputJSON + je.getMessage());
			}

			if (ignoreExistence == null)
				ignoreExistence = false; //ignoreExistence: create the attribute even when this column already exists in the database or in Athena

			
			//verify that JSON does not contain uniqueId field
			if (newAttributeJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && newAttributeJSON.get(Constants.JSON_FIELD_UNIQUE_ID) !=null) {
				return sendAndLogError (Response.Status.BAD_REQUEST, Strings.attributeWithId);
			}

			//verify that JSON does not contain different entity-id than the path parameter
			if (newAttributeJSON.containsKey(Constants.JSON_FIELD_ENTITY_ID) && newAttributeJSON.get(Constants.JSON_FIELD_ENTITY_ID) !=null) {
				if (!entity_id.equals(newAttributeJSON.getString(Constants.JSON_FIELD_ENTITY_ID))) {
					return sendAndLogError (Response.Status.BAD_REQUEST, Strings.attributeEntityWithDifferentId);
				}
			}
			else {
				newAttributeJSON.put(Constants.JSON_FIELD_ENTITY_ID, entity_id);
			}

			Attribute newAttributeObj = new Attribute();

			ValidationResults validationRes = newAttributeObj.validateJSON(newAttributeJSON, context, ignoreExistence, userInfo);
			if (validationRes!=null) {
				return sendAndLogError(validationRes.status, validationRes.error);
			}

			newAttributeObj.fromJSON(newAttributeJSON);
			newAttributeObj.setUniqueId(UUID.randomUUID());

			@SuppressWarnings("unchecked")
			Map<String, Entity> entitiesDB = (Map<String, Entity>)context.getAttribute(Constants.ENTITIES_DB_PARAM_NAME);				

			@SuppressWarnings("unchecked")
			Map<String, Attribute> attributesDB = (Map<String, Attribute>)context.getAttribute(Constants.ATTRIBUTES_DB_PARAM_NAME);				

			Entity entity = entitiesDB.get(entity_id);

			//add attribute to the table in the db
			newAttributeObj.addToDatabase(context, ignoreExistence);
			
			//add attribute to the table in the athena
			newAttributeObj.addToAthena(context, ignoreExistence);
			
			attributesDB.put(newAttributeObj.getUniqueId().toString(), newAttributeObj);
			entity.addAttribute(newAttributeObj);

			try {
				change.getFiles().addAll(AirlockFilesWriter.writeProductEntities(currentProduct, context, userInfo));
				Webhooks.get(context).notifyChanges(change, context);
			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}

			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Create new attribute: " + newAttributeObj.toJson().toString(), userInfo); 

			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FIELD_UNIQUE_ID, newAttributeObj.getUniqueId().toString());
			res.put(Constants.JSON_FIELD_NAME, newAttributeObj.getName());
			res.put(Constants.JSON_FIELD_LAST_MODIFIED, newAttributeObj.getLastModified().getTime());

			logger.info("Attribute added to entity '"+  entity_id + "': "+ res.toString());

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Added attribute: " + newAttributeObj.toJson());
			}

			return (Response.ok(res.toString())).build();
		} catch (Throwable e) {
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error adding attribute: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	@PUT
	@Path("/products/entities/attributes/{attribute-id}")
	@ApiOperation(value = "Updates the specified attribute", response = String.class)
	@ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Attribute not found"),
			@ApiResponse(code = 500, message = "Internal error")})
	public Response updateAttribute(@PathParam("attribute-id") String attribute_id, String attribute, 
			@QueryParam("ignoreexistence")Boolean ignoreExistence, @HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("updateAttribute request for id:"+attribute_id);
		}

		AirlockChange change = new AirlockChange();

		String err = Utilities.validateLegalUUID(attribute_id);
		if (err!=null) {
			return sendAndLogError(Response.Status.BAD_REQUEST, Strings.illegalCohortUUID + err);
		}
		//find relevant product
		Utilities.ProductErrorPair prodErrPair = Utilities.getProductOfEntityAttribute(context, attribute_id);
		if (prodErrPair.error != null) {
			return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		Product currentProduct = prodErrPair.product;
		change.setProduct(currentProduct);

		UserInfo userInfo = UserInfo.validate("AirlyticsServices.updateAttribute", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null) {
			return sendInfoError(Response.Status.UNAUTHORIZED, userInfo);
		}
		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new Constants.AirlockCapability[]{Constants.AirlockCapability.ENTITIES});
		if (capabilityValidationRes!=null)
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.writeLock().lock();
		try {
			@SuppressWarnings("unchecked")
			Map<String, Attribute> attributesDB = (Map<String, Attribute>)context.getAttribute(Constants.ATTRIBUTES_DB_PARAM_NAME);

			Attribute attributeToUpdate = attributesDB.get(attribute_id);
			if (attributeToUpdate == null) {
				return sendAndLogError(Response.Status.NOT_FOUND, Strings.attributeNotFound);
			}

			JSONObject updatedAttributeJSON = null;
			try {
				updatedAttributeJSON = new JSONObject(attribute);
			} catch (JSONException je) {
				return sendAndLogError(Response.Status.BAD_REQUEST, Strings.illegalInputJSON + je.getMessage());
			}
			
			if (ignoreExistence == null)
				ignoreExistence = false; //ignoreExistence: create the attribute even when this column already exists in the database or in Athena

			if (!updatedAttributeJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) || updatedAttributeJSON.get(Constants.JSON_FIELD_UNIQUE_ID) == null) {
				updatedAttributeJSON.put(Constants.JSON_FIELD_UNIQUE_ID, attribute_id);
			}
			else {
				//verify that attribute-id in path is identical to uniqueId in request pay-load
				if (!updatedAttributeJSON.getString(Constants.JSON_FIELD_UNIQUE_ID).equals(attribute_id)) {
					return sendAndLogError(Response.Status.BAD_REQUEST,  Strings.attributeWithDifferentId);
				}
			}

			ValidationResults validationRes = attributeToUpdate.validateJSON(updatedAttributeJSON, context, ignoreExistence, userInfo);
			if (validationRes!=null) {
				return sendAndLogError(validationRes.status, validationRes.error);
			}

			//finally - actually update the experiment.
			String updateDetails = attributeToUpdate.updateAttribute(updatedAttributeJSON, context, ignoreExistence);

			if (!updateDetails.isEmpty()) {
				change.getFiles().addAll(AirlockFilesWriter.writeProductEntities(currentProduct, context, userInfo));
				Webhooks.get(context).notifyChanges(change, context);

				AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
				auditLogWriter.log("Update attribute: " + attribute_id + ",   " + updateDetails, userInfo);
			}
			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Updated attribute: " + attributeToUpdate.toJson() + "\n updatd details: " + updateDetails);
			}
			return (Response.ok(attributeToUpdate.toJson().toString())).build();
		} catch (Throwable e) {
			return sendAndLogError(Response.Status.INTERNAL_SERVER_ERROR, "Error updating attribute: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	@DELETE
	@Path("/products/entities/attributes/{attribute-id}")
	@ApiOperation(value = "Deletes the specified attribute", response = String.class)
	@ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Attribute not found"),
			@ApiResponse(code = 500, message = "Internal error")})
	public Response deleteAttribute(@PathParam("attribute-id") String attribute_id, @HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("deleteAttribute request");
		}
		AirlockChange change = new AirlockChange();
		String err = Utilities.validateLegalUUID(attribute_id);
		if (err!=null)
			return sendAndLogError(Response.Status.BAD_REQUEST, Strings.illegalAttributeUUID + err);

		//find relevant product
		Utilities.ProductErrorPair prodErrPair = Utilities.getProductOfEntityAttribute(context, attribute_id);
		if (prodErrPair.error != null) {
			return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}

		Product currentProduct = prodErrPair.product;
		change.setProduct(currentProduct);
		//check user authorization
		UserInfo userInfo = UserInfo.validate("AirlyticsServices.deleteAttribute", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Response.Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new Constants.AirlockCapability[]{Constants.AirlockCapability.ENTITIES});
		if (capabilityValidationRes!=null)
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.writeLock().lock();
		try {
			@SuppressWarnings("unchecked")
			Map<String, Attribute> attributesDB = (Map<String, Attribute>)context.getAttribute(Constants.ATTRIBUTES_DB_PARAM_NAME);

			Attribute attributeToDelete = attributesDB.get(attribute_id);
			if (attributeToDelete == null) {
				return sendAndLogError(Response.Status.NOT_FOUND, Strings.attributeNotFound);
			}

			if (!attributeToDelete.getDeprecated()) {
				return sendAndLogError(Response.Status.NOT_FOUND, Strings.CannotDeleteNonDeprecatedAtt);
			}
			
			ValidationResults validateAccess = attributeToDelete.validateAttributeAccess(userInfo, AttributesPermission.READ_WRITE_DELETE, context);
			if (validateAccess != null) {
				return sendAndLogError(validateAccess.status, validateAccess.error);
			}
			
			@SuppressWarnings("unchecked")
			Map<String, Entity> entitiesDB = (Map<String, Entity>)context.getAttribute(Constants.ENTITIES_DB_PARAM_NAME);

			Entity entity = entitiesDB.get(attributeToDelete.getEntityId().toString());
			if (entity == null) {
				return sendAndLogError(Response.Status.NOT_FOUND, Strings.entityNotFound);
			}
			
			attributeToDelete.removeFromDb(context);
			attributeToDelete.removeFromAthena(context);
			
			String errorString = entity.removeAttribute(attributeToDelete, currentProduct);
			if (errorString!=null) {
				return sendAndLogError(Response.Status.BAD_REQUEST, errorString);
			}
			attributesDB.remove(attribute_id);

			try {
				change.setProduct(currentProduct);
				change.getFiles().addAll(AirlockFilesWriter.writeProductEntities(currentProduct, context, userInfo));
				Webhooks.get(context).notifyChanges(change, context);
			} catch (IOException e) {
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}

			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Delete attribute: uniqueId: " + attribute_id, userInfo); 

			return (Response.ok()).build();
		} catch (Throwable e) {
			return sendAndLogError(Response.Status.INTERNAL_SERVER_ERROR, "Error deleting attribute: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		} 
	}

	@GET
	@Path("/products/entities/attributetypes/{attributetype-id}")
	@ApiOperation(value = "Returns the specified attribute type", response = String.class)
	@ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "AttributeType not found"),
			@ApiResponse(code = 500, message = "Internal error")})
	public Response getAttributeType(@PathParam("attributetype-id") String attributetype_id, @HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getAttributeType request");
		}

		String err = Utilities.validateLegalUUID(attributetype_id);
		if (err!=null)
			return sendAndLogError(Response.Status.BAD_REQUEST, Strings.illegalAttributeTypeUUID + err);

		//find relevant product
		Utilities.ProductErrorPair prodErrPair = Utilities.getProductOfEntityAttributeType(context, attributetype_id);
		if (prodErrPair.error != null) {
			return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		Product currentProduct = prodErrPair.product;

		UserInfo userInfo = UserInfo.validate("AirlyticsServices.getAttributeType", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Response.Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new Constants.AirlockCapability[]{Constants.AirlockCapability.ENTITIES});
		if (capabilityValidationRes!=null)
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.readLock().lock();
		try {
			@SuppressWarnings("unchecked")
			Map<String, AttributeType> attributeTypesDB = (Map<String, AttributeType>)context.getAttribute(Constants.ATTRIBUTE_TYPES_DB_PARAM_NAME);

			AttributeType attributeType = attributeTypesDB.get(attributetype_id);

			if (attributeType == null) {
				return sendAndLogError(Response.Status.NOT_FOUND, Strings.attributeTypeNotFound);
			}

			JSONObject attributeTypeJSON = attributeType.toJson();
			return (Response.ok(attributeTypeJSON.toString())).build();
		} catch (Throwable e) {
			return sendAndLogError(Response.Status.INTERNAL_SERVER_ERROR, "Error getting attributeType: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	@POST
	@Path("/products/entities/{entity-id}/attributetypes")
	@ApiOperation(value = "Creates an attribute type for the specified entity", response = String.class)
	@ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Entity not found"),
			@ApiResponse(code = 500, message = "Internal error")})
	public Response createAttributeType(@PathParam("entity-id") String entity_id, String newAttributeType, 
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("createAttributeType request for entity: "+ entity_id);
		}

		AirlockChange change = new AirlockChange();

		String err = Utilities.validateLegalUUID(entity_id);
		if (err!=null)
			return sendAndLogError(Response.Status.BAD_REQUEST, Strings.illegalProductUUID + err);

		//find relevant product
		Utilities.ProductErrorPair productErrorPair = Utilities.getProductOfEntity(context, entity_id);
		if (productErrorPair.error != null) {
			return Response.status(Response.Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);

		//check user authorization
		UserInfo userInfo = UserInfo.validate("AirlyticsServices.createAttributeType", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Response.Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new Constants.AirlockCapability[]{Constants.AirlockCapability.ENTITIES});
		if (capabilityValidationRes!=null)
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.writeLock().lock();
		try {
			//validate that is a legal JSON
			JSONObject newAttributeTypeJSON = null;
			try {
				newAttributeTypeJSON = new JSONObject(newAttributeType);
			} catch (JSONException je) {
				return sendAndLogError(Response.Status.BAD_REQUEST, Strings.illegalInputJSON + je.getMessage());
			}

			//verify that JSON does not contain uniqueId field
			if (newAttributeTypeJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && newAttributeTypeJSON.get(Constants.JSON_FIELD_UNIQUE_ID) !=null) {
				return sendAndLogError (Response.Status.BAD_REQUEST, Strings.attributeTypeWithId);
			}

			//verify that JSON does not contain different entity-id than the path parameter
			if (newAttributeTypeJSON.containsKey(Constants.JSON_FIELD_ENTITY_ID) && newAttributeTypeJSON.get(Constants.JSON_FIELD_ENTITY_ID) !=null) {
				if (!entity_id.equals(newAttributeTypeJSON.getString(Constants.JSON_FIELD_ENTITY_ID))) {
					return sendAndLogError (Response.Status.BAD_REQUEST, Strings.attributeTypeEntityWithDifferentId);
				}
			}
			else {
				newAttributeTypeJSON.put(Constants.JSON_FIELD_ENTITY_ID, entity_id);
			}

			AttributeType newAttributeTypeObj = new AttributeType();

			ValidationResults validationRes = newAttributeTypeObj.validateJSON(newAttributeTypeJSON, context);
			if (validationRes!=null) {
				return sendAndLogError(validationRes.status, validationRes.error);
			}

			newAttributeTypeObj.fromJSON(newAttributeTypeJSON);
			newAttributeTypeObj.setUniqueId(UUID.randomUUID());

			@SuppressWarnings("unchecked")
			Map<String, Entity> entitiesDB = (Map<String, Entity>)context.getAttribute(Constants.ENTITIES_DB_PARAM_NAME);				

			@SuppressWarnings("unchecked")
			Map<String, AttributeType> attributeTypesDB = (Map<String, AttributeType>)context.getAttribute(Constants.ATTRIBUTE_TYPES_DB_PARAM_NAME);				

			Entity entity = entitiesDB.get(entity_id);

			attributeTypesDB.put(newAttributeTypeObj.getUniqueId().toString(), newAttributeTypeObj);
			entity.addAttributeType(newAttributeTypeObj);

			try {
				change.getFiles().addAll(AirlockFilesWriter.writeProductEntities(currentProduct, context, userInfo));
				Webhooks.get(context).notifyChanges(change, context);
			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}

			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Create new attribute type: " + newAttributeTypeObj.toJson().toString(), userInfo); 

			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FIELD_UNIQUE_ID, newAttributeTypeObj.getUniqueId().toString());
			res.put(Constants.JSON_FIELD_NAME, newAttributeTypeObj.getName());
			res.put(Constants.JSON_FIELD_LAST_MODIFIED, newAttributeTypeObj.getLastModified().getTime());

			logger.info("Attribute type added to entity '"+  entity_id + "': "+ res.toString());

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Added attribute type: " + newAttributeTypeObj.toJson());
			}

			return (Response.ok(res.toString())).build();
		} catch (Throwable e) {
			return sendAndLogError(Status.INTERNAL_SERVER_ERROR, "Error adding attribute type: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}

	}

	@PUT
	@Path("/products/entities/attributetypes/{attributetype-id}")
	@ApiOperation(value = "Updates the specified attribute type", response = String.class)
	@ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Attribute type not found"),
			@ApiResponse(code = 500, message = "Internal error")})
	public Response updateAttributeType(@PathParam("attributetype-id") String attributetype_id, String attributeType, 
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("updateAttributeType request for id:"+attributetype_id);
		}

		AirlockChange change = new AirlockChange();

		String err = Utilities.validateLegalUUID(attributetype_id);
		if (err!=null) {
			return sendAndLogError(Response.Status.BAD_REQUEST, Strings.illegalCohortUUID + err);
		}
		//find relevant product
		Utilities.ProductErrorPair prodErrPair = Utilities.getProductOfEntityAttributeType(context, attributetype_id);
		if (prodErrPair.error != null) {
			return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		Product currentProduct = prodErrPair.product;
		change.setProduct(currentProduct);

		UserInfo userInfo = UserInfo.validate("AirlyticsServices.updateAttributeType", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null) {
			return sendInfoError(Response.Status.UNAUTHORIZED, userInfo);
		}
		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new Constants.AirlockCapability[]{Constants.AirlockCapability.ENTITIES});
		if (capabilityValidationRes!=null)
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.writeLock().lock();
		try {
			@SuppressWarnings("unchecked")
			Map<String, AttributeType> attributeTypesDB = (Map<String, AttributeType>)context.getAttribute(Constants.ATTRIBUTE_TYPES_DB_PARAM_NAME);

			AttributeType attributeTypeToUpdate = attributeTypesDB.get(attributetype_id);
			if (attributeTypeToUpdate == null) {
				return sendAndLogError(Response.Status.NOT_FOUND, Strings.attributeTypeNotFound);
			}

			JSONObject updatedAttributeTypeJSON = null;
			try {
				updatedAttributeTypeJSON = new JSONObject(attributeType);
			} catch (JSONException je) {
				return sendAndLogError(Response.Status.BAD_REQUEST, Strings.illegalInputJSON + je.getMessage());
			}

			if (!updatedAttributeTypeJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) || updatedAttributeTypeJSON.get(Constants.JSON_FIELD_UNIQUE_ID) == null) {
				updatedAttributeTypeJSON.put(Constants.JSON_FIELD_UNIQUE_ID, attributetype_id);
			}
			else {
				//verify that attributetype-id in path is identical to uniqueId in request pay-load
				if (!updatedAttributeTypeJSON.getString(Constants.JSON_FIELD_UNIQUE_ID).equals(attributetype_id)) {
					return sendAndLogError(Response.Status.BAD_REQUEST,  Strings.attributeTypeWithDifferentId);
				}
			}

			ValidationResults validationRes = attributeTypeToUpdate.validateJSON(updatedAttributeTypeJSON, context);
			if (validationRes!=null) {
				return sendAndLogError(validationRes.status, validationRes.error);
			}

			//finally - actually update the experiment.
			String updateDetails = attributeTypeToUpdate.updateAttributeType(updatedAttributeTypeJSON);

			if (!updateDetails.isEmpty()) {
				change.getFiles().addAll(AirlockFilesWriter.writeProductEntities(currentProduct, context, userInfo));
				Webhooks.get(context).notifyChanges(change, context);

				AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
				auditLogWriter.log("Update attribute type: " + attributetype_id + ",   " + updateDetails, userInfo);
			}
			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Updated attribute type: " + attributeTypeToUpdate.toJson() + "\n updatd details: " + updateDetails);
			}
			return (Response.ok(attributeTypeToUpdate.toJson().toString())).build();
		} catch (Throwable e) {
			return sendAndLogError(Response.Status.INTERNAL_SERVER_ERROR, "Error updating attribute type: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	@DELETE
	@Path("/products/entities/attributetypes/{attributetype-id}")
	@ApiOperation(value = "Deletes the specified attribute type", response = String.class)
	@ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Attribute type not found"),
			@ApiResponse(code = 500, message = "Internal error")})
	public Response deleteAttributeType(@PathParam("attributetype-id") String attributetype_id, @HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("deleteAttributeType request");
		}
		AirlockChange change = new AirlockChange();
		String err = Utilities.validateLegalUUID(attributetype_id);
		if (err!=null)
			return sendAndLogError(Response.Status.BAD_REQUEST, Strings.illegalAttributeTypeUUID + err);

		//find relevant product
		Utilities.ProductErrorPair prodErrPair = Utilities.getProductOfEntityAttributeType(context, attributetype_id);
		if (prodErrPair.error != null) {
			return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}

		Product currentProduct = prodErrPair.product;
		change.setProduct(currentProduct);
		//check user authorization
		UserInfo userInfo = UserInfo.validate("AirlyticsServices.deleteAttributeType", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Response.Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new Constants.AirlockCapability[]{Constants.AirlockCapability.ENTITIES});
		if (capabilityValidationRes!=null)
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();

		ReentrantReadWriteLock readWriteLock = currentProduct.getProductLock();
		readWriteLock.writeLock().lock();
		try {
			@SuppressWarnings("unchecked")
			Map<String, AttributeType> attributeTypesDB = (Map<String, AttributeType>)context.getAttribute(Constants.ATTRIBUTE_TYPES_DB_PARAM_NAME);

			AttributeType attributeTypeToDelete = attributeTypesDB.get(attributetype_id);
			if (attributeTypeToDelete == null) {
				return sendAndLogError(Response.Status.NOT_FOUND, Strings.attributeTypeNotFound);
			}

			@SuppressWarnings("unchecked")
			Map<String, Entity> entitiesDB = (Map<String, Entity>)context.getAttribute(Constants.ENTITIES_DB_PARAM_NAME);

			Entity entity = entitiesDB.get(attributeTypeToDelete.getEntityId().toString());
			if (entity == null) {
				return sendAndLogError(Response.Status.NOT_FOUND, Strings.entityNotFound);
			}

			String errorString = entity.removeAttributeType(attributeTypeToDelete.getUniqueId());
			if (errorString!=null) {
				return sendAndLogError(Response.Status.BAD_REQUEST, errorString);
			}
			attributeTypesDB.remove(attributetype_id);

			try {
				change.setProduct(currentProduct);
				change.getFiles().addAll(AirlockFilesWriter.writeProductEntities(currentProduct, context, userInfo));
				Webhooks.get(context).notifyChanges(change, context);
			} catch (IOException e) {
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
			}

			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Delete attribute type: uniqueId: " + attributetype_id, userInfo); 

			return (Response.ok()).build();
		} catch (Throwable e) {
			return sendAndLogError(Response.Status.INTERNAL_SERVER_ERROR, "Error deleting attribute type: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		} 
	}

	Response sendAndLogError(Response.Status status, String err)
	{
		logger.severe(err);
		return Response.status(status).entity(Utilities.errorMsgToErrorJSON(err)).build();
	}

	@GET
	@Path("/products/{product-id}/dataimport/meta/users/tables")
	@ApiOperation(value = "Get users DB user features table names", response = String.class)
	@ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Feature not found"),
			@ApiResponse(code = 500, message = "Internal error")})
	public Response getDataImportTables(@PathParam("product-id") String product_id, @HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getDataImportTables request");
		}

		String err = Utilities.validateLegalUUID(product_id);
		if (err!=null)
			return sendAndLogError(Response.Status.BAD_REQUEST, Strings.illegalProductUUID + err);

		//find relevant product
		Utilities.ProductErrorPair prodErrPair = Utilities.getProduct(context, product_id);
		if (prodErrPair.error != null) {
			return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		Product currentProduct = prodErrPair.product;

		UserInfo userInfo = UserInfo.validate("AirlyticsServices.getDataImportJobs", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Response.Status.UNAUTHORIZED, userInfo);

		try {
			return DataImportUtilities.getDataImportTables(context,currentProduct.getUniqueId().toString(), assertion);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Error getting data import table names :", e);
			return sendAndLogError(Response.Status.INTERNAL_SERVER_ERROR, "Error getting data import table names: " + e.getMessage());
		}
	}

	@GET
	@Path("/products/{product-id}/dataimport")
	@ApiOperation(value = "Get data import jobs for product", response = String.class)
	@ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Feature not found"),
			@ApiResponse(code = 500, message = "Internal error")})
	public Response getDataImportJobs(@PathParam("product-id") String product_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getDataImportJobs request");
		}

		String err = Utilities.validateLegalUUID(product_id);
		if (err!=null)
			return sendAndLogError(Response.Status.BAD_REQUEST, Strings.illegalProductUUID + err);

		//find relevant product
		Utilities.ProductErrorPair prodErrPair = Utilities.getProduct(context, product_id);
		if (prodErrPair.error != null) {
			return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		Product currentProduct = prodErrPair.product;

		UserInfo userInfo = UserInfo.validate("AirlyticsServices.getDataImportJobs", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Response.Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new Constants.AirlockCapability[]{Constants.AirlockCapability.DATA_IMPORT});
		if (capabilityValidationRes!=null)
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();

		ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
		readWriteLock.readLock().lock();
		try {
			@SuppressWarnings("unchecked")
			JSONObject importJobsJson = currentProduct.getDataImports().toJson();

			return (Response.ok(importJobsJson.toString())).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error getting data imports :", e);
			return sendAndLogError(Response.Status.INTERNAL_SERVER_ERROR, "Error getting cohorts: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	@POST
	@Path("/products/{product-id}/dataimport")
	@ApiOperation(value = "Create data import job", response = String.class)
	@ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Feature not found"),
			@ApiResponse(code = 500, message = "Internal error")})
	public Response createDataImportJob(@PathParam("product-id") String product_id,
			String newDataImport,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("createDataImportJob request for product:"+product_id);
		}

		AirlockChange change = new AirlockChange();

		String err = Utilities.validateLegalUUID(product_id);
		if (err!=null)
			return sendAndLogError(Response.Status.BAD_REQUEST, Strings.illegalProductUUID + err);

		//find relevant product
		Utilities.ProductErrorPair productErrorPair = Utilities.getProduct(context, product_id);
		if (productErrorPair.error != null) {
			return Response.status(Response.Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);

		//check user authorization
		UserInfo userInfo = UserInfo.validate("AirlyticsServices.createDataImportJob", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Response.Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new Constants.AirlockCapability[]{Constants.AirlockCapability.DATA_IMPORT});
		if (capabilityValidationRes!=null)
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();

		//validate that is a legal JSON
		JSONObject newJobJSON = null;
		try {
			newJobJSON = new JSONObject(newDataImport);
		} catch (JSONException je) {
			String errMsg = Strings.illegalInputJSON + je.getMessage();
			logger.severe(errMsg);
			return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
		}

		//verify that JSON does not contain uniqueId field
		if (newJobJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && newJobJSON.get(Constants.JSON_FIELD_UNIQUE_ID) !=null) {
			String errMsg = Strings.dataImportWithId;
			logger.severe(errMsg);
			return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
		}

		//verify that JSON does not contain different product-id then the path parameter
		if (newJobJSON.containsKey(Constants.JSON_FIELD_PRODUCT_ID) && newJobJSON.get(Constants.JSON_FIELD_PRODUCT_ID) !=null) {
			if (!product_id.equals(newJobJSON.getString(Constants.JSON_FIELD_PRODUCT_ID))) {
				String errMsg = Strings.dataImportProductWithDifferentId;
				logger.severe(errMsg);
				return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}
		}
		else {
			newJobJSON.put(Constants.JSON_FIELD_PRODUCT_ID, product_id);
		}

		DataImportItem newJobObj = new DataImportItem(currentProduct.getUniqueId());

		ReentrantReadWriteLock readWriteLock = currentProduct.getDataImportProductLock();
		readWriteLock.writeLock().lock();

		try {
			@SuppressWarnings("unchecked")
			Map<String, DataImportItem> jobsDB = (Map<String, DataImportItem>)context.getAttribute(Constants.DATA_IMPORT_DB_PARAM_NAME);
			ValidationResults validationRes = newJobObj.validateDataImportJSON(newJobJSON);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			newJobObj.fromJSON(newJobJSON);
			newJobObj.setUniqueId(UUID.randomUUID());


			currentProduct.addDataImport(newJobObj);
			jobsDB.put(newJobObj.getUniqueId().toString(), newJobObj);

			change.getFiles().addAll(AirlockFilesWriter.writeDataImportJobs(currentProduct, context));
			Webhooks.get(context).notifyChanges(change, context);



			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Create new data import job: " + newJobObj.getS3File() + ", " + newJobObj.getUniqueId() + ":" + newJobObj.toJson().toString(), userInfo);
			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FIELD_UNIQUE_ID, newJobObj.getUniqueId().toString());
			res.put(Constants.JSON_FIELD_DATA_IMPORT_S3_FILE, newJobObj.getS3File());
			res.put(Constants.JSON_FIELD_LAST_MODIFIED, newJobObj.getLastModified().getTime());

			logger.info("Data import job added to product '"+  product_id + "': "+ res.toString());

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Added data import job: " + newJobObj.toJson());
			}
		} catch (IOException e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
		} finally {
			readWriteLock.writeLock().unlock();
		}

		//import data in data-import server
		try {
			DataImportCallResult result = DataImportUtilities.importData(context, newJobObj, assertion);
			ValidationResults validation = result.getValidationResults();
			if (validation!=null) {
				String errMsg = validation.error;
				logger.severe(errMsg);
				return Response.status(validation.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			} else {
				//                DataImportResult response = result.getItem();
				//                String updateDetails = newJobObj.updateDataImportStatus(response.toJson(), new Date());
				//                if (updateDetails != null && !updateDetails.isEmpty()) {
				//                    change.getFiles().addAll(AirlockFilesWriter.writeDataImportJobs(currentProduct, context));
				//                    Webhooks.get(context).notifyChanges(change, context);
				//                }
				return this.getDataImportJob(newJobObj.getUniqueId().toString(), assertion);
			}
		} catch (IOException e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
		}

	}

	@PUT
	@Path("/products/{product-id}/dataimport")
	@ApiOperation(value = "Update data import data", response = String.class)
	@ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Not found"),
			@ApiResponse(code = 500, message = "Internal error")})
	public Response updateDataImportData(@PathParam("product-id") String product_id,
			String dataImportData,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("updateDataImportData request for product:"+product_id);
		}

		AirlockChange change = new AirlockChange();

		String err = Utilities.validateLegalUUID(product_id);
		if (err!=null)
			return sendAndLogError(Response.Status.BAD_REQUEST, Strings.illegalProductUUID + err);

		//find relevant product
		Utilities.ProductErrorPair productErrorPair = Utilities.getProduct(context, product_id);
		if (productErrorPair.error != null) {
			return Response.status(Response.Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(productErrorPair.error)).build();
		}
		Product currentProduct = productErrorPair.product;
		change.setProduct(currentProduct);

		//check user authorization
		UserInfo userInfo = UserInfo.validate("AirlyticsServices.updateDataImportData", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Response.Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new Constants.AirlockCapability[]{Constants.AirlockCapability.DATA_IMPORT});
		if (capabilityValidationRes!=null)
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();

		//validate that is a legal JSON
		JSONObject newJobsDataJSON = null;
		try {
			newJobsDataJSON = new JSONObject(dataImportData);
		} catch (JSONException je) {
			String errMsg = Strings.illegalInputJSON + je.getMessage();
			logger.severe(errMsg);
			return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
		}

		ReentrantReadWriteLock readWriteLock = currentProduct.getDataImportProductLock();

		readWriteLock.writeLock().lock();
		try {
			String updateDetails = currentProduct.updateDataInports(newJobsDataJSON);
			if (!updateDetails.isEmpty()) {
				change.getFiles().addAll(AirlockFilesWriter.writeDataImportJobs(currentProduct, context));
				Webhooks.get(context).notifyChanges(change, context);

				AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
				auditLogWriter.log("Update data import data for product: " + product_id + ",   " + updateDetails, userInfo);
			}
			return (Response.ok(updateDetails.toString())).build();
		} catch (JSONException | IOException e) {
			logger.log(Level.SEVERE, "Error updating airlytics data-imports data action on product-id" + product_id + ": ", e);
			return sendAndLogError(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
		}
		finally {
			readWriteLock.writeLock().unlock();
		}
	}

	@PUT
	@Path("/products/{product-id}/dataimport/{job-id}/status")
	@ApiOperation(value = "Update data import status", response = String.class)
	@ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Feature not found"),
			@ApiResponse(code = 500, message = "Internal error")})
	public Response updateDataImportJobStatus(@PathParam("product-id") String product_id ,@PathParam("job-id") String job_id, String status,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("updateDataImportJobStatus request for id:"+job_id);
		}

		AirlockChange change = new AirlockChange();

		String err = Utilities.validateLegalUUID(job_id);
		if (err!=null)
			return sendAndLogError(Response.Status.BAD_REQUEST, Strings.illegalCohortUUID + err);

		//find relevant product
		Utilities.ProductErrorPair prodErrPair = Utilities.getProductOfDataImport(context, job_id);
		if (prodErrPair.error != null) {
			return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		Product currentProduct = prodErrPair.product;
		if (product_id == null || product_id.isEmpty()) {
			return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON("product_id is missing")).build();
		}
		if (!currentProduct.getUniqueId().toString().equals(product_id)) {
			return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON("job_id is from a different product than product_id")).build();
		}
		change.setProduct(currentProduct);

		UserInfo userInfo = UserInfo.validate("AirlyticsServices.updateDataImportJob", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Response.Status.UNAUTHORIZED, userInfo);
		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new Constants.AirlockCapability[]{Constants.AirlockCapability.DATA_IMPORT});
		if (capabilityValidationRes!=null)
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();

		ReentrantReadWriteLock readWriteLock = currentProduct.getDataImportProductLock();
		readWriteLock.writeLock().lock();

		try {

			@SuppressWarnings("unchecked")
			Map<String, DataImportItem> jobsDB = (Map<String, DataImportItem>)context.getAttribute(Constants.DATA_IMPORT_DB_PARAM_NAME);

			DataImportItem jobToUpdate = jobsDB.get(job_id);
			if (jobToUpdate == null) {
				String errMsg = Strings.dataImportNotFound;
				logger.severe(errMsg);
				return Response.status(Response.Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			JSONObject updatedJobJSON = null;
			try {
				updatedJobJSON = new JSONObject(status);
			} catch (JSONException je) {
				String errMsg = Strings.illegalInputJSON + je.getMessage();
				logger.severe(errMsg);
				return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			if (!updatedJobJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) || updatedJobJSON.get(Constants.JSON_FIELD_UNIQUE_ID) == null) {
				updatedJobJSON.put(Constants.JSON_FIELD_UNIQUE_ID, job_id);
			}
			else {
				//verify that job-id in path is identical to uniqueId in request pay-load
				if (!updatedJobJSON.getString(Constants.JSON_FIELD_UNIQUE_ID).equals(job_id)) {
					String errMsg = Strings.dataImportWithDifferentId;
					logger.severe(errMsg);
					return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
				}
			}

			ValidationResults validationRes = jobToUpdate.validateDataImportStatusJSON(updatedJobJSON, context);
			if (validationRes!=null) {
				String errMsg = validationRes.error;
				logger.severe(errMsg);
				return Response.status(validationRes.status).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			//finally - actually update the data import item.
			Date now = new Date();
			String updateDetails = jobToUpdate.updateDataImportStatus(updatedJobJSON, now);

			if (!updateDetails.isEmpty()) {
				change.getFiles().addAll(AirlockFilesWriter.writeDataImportJobs(currentProduct, context));
				Webhooks.get(context).notifyChanges(change, context);

				AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
				auditLogWriter.log("Update data import job status for product: " + product_id + ",   " + updateDetails, userInfo);
			}
			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Updated data import status: " + jobToUpdate.toJson() + "\n updatd details: " + updateDetails);
			}
			return (Response.ok(jobToUpdate.toJson().toString())).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error updating data import status :", e);
			return sendAndLogError(Response.Status.INTERNAL_SERVER_ERROR, "Error updating data import status: " + e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	@DELETE
	@Path("/dataimport/{job-id}")
	@ApiOperation(value = "Delete data import item", response = String.class)
	@ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Not found"),
			@ApiResponse(code = 500, message = "Internal error")})
	public Response deleteDataImportItem(@PathParam("job-id") String job_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) throws JSONException {
		AirlockChange change = new AirlockChange();
		String err = Utilities.validateLegalUUID(job_id);
		if (err!=null)
			return sendAndLogError(Response.Status.BAD_REQUEST, Strings.illegalDataImportUUID + err);

		//find relevant product
		Utilities.ProductErrorPair prodErrPair = Utilities.getProductOfDataImport(context, job_id);
		if (prodErrPair.error != null) {
			return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		Product currentProduct = prodErrPair.product;
		change.setProduct(currentProduct);
		//check user authorization
		UserInfo userInfo = UserInfo.validate("AirlyticsServices.deleteDataImportItem", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Response.Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new Constants.AirlockCapability[]{Constants.AirlockCapability.DATA_IMPORT});
		if (capabilityValidationRes!=null)
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();

		ReentrantReadWriteLock readWriteLock = currentProduct.getDataImportProductLock();

		try {
			@SuppressWarnings("unchecked")
			Map<String, DataImportItem> jobsDB = (Map<String, DataImportItem>)context.getAttribute(Constants.DATA_IMPORT_DB_PARAM_NAME);

			DataImportItem jobToDelete = jobsDB.get(job_id);
			if (jobToDelete == null) {
				String errMsg = Strings.dataImportNotFound;
				logger.severe(errMsg);
				return Response.status(Response.Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			//remove from dataImportService
			//            AirCohortsCallResult aircohortsResult = CohortsUtilities.deleteCohort(context, jobToDelete, null, assertion);
			//            if (aircohortsResult.getValidationResults() != null) {
			//                ValidationResults validationRes = aircohortsResult.getValidationResults();
			//                return Response.status(validationRes.status).entity("failed to delete cohort in airCohorts:"+validationRes.error).build();
			//            }
			try {
				readWriteLock.writeLock().lock();
				//actually removing the job
				String errorString = currentProduct.removeDataImport(jobToDelete.getUniqueId());
				if (errorString!=null) {
					//should not happen - will be returned if cohort is not in product
					logger.severe(errorString);
					return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(errorString)).build();
				}

				jobsDB.remove(job_id);
				try {
					change.setProduct(currentProduct);
					change.getFiles().addAll(AirlockFilesWriter.writeDataImportJobs(currentProduct, context));
					Webhooks.get(context).notifyChanges(change, context);
				} catch (IOException e) {
					return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Utilities.errorMsgToErrorJSON(e.getMessage())).build();
				}
				return (Response.ok()).build();
			} finally {
				readWriteLock.writeLock().unlock();
			}

		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error deleting data import item " + job_id + ": ", e);
			return sendAndLogError(Response.Status.INTERNAL_SERVER_ERROR, "Error deleting data import item: " + e.getMessage());
		}
	}

	@GET
	@Path("/dataimport/{job-id}")
	@ApiOperation(value = "Get cohort", response = String.class)
	@ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 404, message = "Feature not found"),
			@ApiResponse(code = 500, message = "Internal error")})
	public Response getDataImportJob(@PathParam("job-id") String job_id,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getDataImportJob request");
		}

		//find relevant product
		Utilities.ProductErrorPair prodErrPair = Utilities.getProductOfDataImport(context, job_id);
		if (prodErrPair.error != null) {
			return Response.status(Response.Status.BAD_REQUEST).entity(Utilities.errorMsgToErrorJSON(prodErrPair.error)).build();
		}
		Product currentProduct = prodErrPair.product;

		UserInfo userInfo = UserInfo.validate("AirlyticsServices.getDataImportJob", context, assertion, currentProduct);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Response.Status.UNAUTHORIZED, userInfo);

		//capability verification
		ValidationResults capabilityValidationRes = Utilities.validateCapability (currentProduct, new Constants.AirlockCapability[]{Constants.AirlockCapability.DATA_IMPORT});
		if (capabilityValidationRes!=null)
			return Response.status(capabilityValidationRes.status).entity(Utilities.errorMsgToErrorJSON(capabilityValidationRes.error)).build();

		ReentrantReadWriteLock readWriteLock = currentProduct.getDataImportProductLock();
		readWriteLock.readLock().lock();
		try {
			@SuppressWarnings("unchecked")
			Map<String, DataImportItem> jobsDB = (Map<String, DataImportItem>)context.getAttribute(Constants.DATA_IMPORT_DB_PARAM_NAME);

			DataImportItem item = jobsDB.get(job_id);

			if (item == null) {
				String errMsg = Strings.dataImportNotFound;
				logger.severe(errMsg);
				return Response.status(Response.Status.NOT_FOUND).entity(Utilities.errorMsgToErrorJSON(errMsg)).build();
			}

			JSONObject dataImportJSON = item.toJson();
			return (Response.ok(dataImportJSON.toString())).build();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error getting data import item :", e);
			return sendAndLogError(Response.Status.INTERNAL_SERVER_ERROR, "Error getting data import item: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	@GET
	@Path("/dbschemas")
	//@ApiOperation(value = "Returns the available schemas list", response = String.class)
	@ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 500, message = "Internal error")})
	public Response getDBSchemas(@HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getDBSchemas request");
		}

		UserInfo userInfo = UserInfo.validate("AirlyticsServices.getDBSchemas", context, assertion, null);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Response.Status.UNAUTHORIZED, userInfo);

		//capability verification
		AirlockCapabilities airlockCapabilities = (AirlockCapabilities)context.getAttribute(Constants.CAPABILITIES_PARAM_NAME);
		if (!airlockCapabilities.contains(Constants.AirlockCapability.ENTITIES)) {
			return sendAndLogError(Response.Status.BAD_REQUEST, String.format(Strings.capabilityNotEnabled, Constants.AirlockCapability.ENTITIES.toString()));
		}
		
		ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
		readWriteLock.readLock().lock();
		try {
			JSONObject schemas = EntitiesUtilities.schemasListToJSONObject(context);
			return (Response.ok(schemas.toString())).build();
		} catch (Throwable e) {
			return sendAndLogError(Response.Status.INTERNAL_SERVER_ERROR, "Error getting schemas: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}
	
	@GET
	@Path("/dbschemas/{schema-name}/tables")
	//@ApiOperation(value = "Returns the available tables list for the specified schema.", response = String.class)
	@ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 500, message = "Internal error")})
	public Response getDBTables(@PathParam("schema-name") String schema_name, @HeaderParam(Constants.AUTHENTICATION_HEADER) String assertion) {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("getDBTables request");
		}

		UserInfo userInfo = UserInfo.validate("AirlyticsServices.getDBTables", context, assertion, null);
		if (userInfo != null && userInfo.getErrorJson() != null)
			return sendInfoError(Response.Status.UNAUTHORIZED, userInfo);

		//capability verification
		AirlockCapabilities airlockCapabilities = (AirlockCapabilities)context.getAttribute(Constants.CAPABILITIES_PARAM_NAME);
		if (!airlockCapabilities.contains(Constants.AirlockCapability.ENTITIES)) {
			return sendAndLogError(Response.Status.BAD_REQUEST, String.format(Strings.capabilityNotEnabled, Constants.AirlockCapability.ENTITIES.toString()));
		}
		
		ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
		readWriteLock.readLock().lock();
		try {
			ValidationResults schemaExistanceRes = EntitiesUtilities.validateSchemaExistance(context, schema_name);
			if (schemaExistanceRes!=null) 
				return sendAndLogError(schemaExistanceRes.status, schemaExistanceRes.error);
			
			JSONObject tables = EntitiesUtilities.tablesListToJSONObject(context, schema_name);
			return (Response.ok(tables.toString())).build();
		} catch (Throwable e) {
			return sendAndLogError(Response.Status.INTERNAL_SERVER_ERROR, "Error getting tables: " + e.getMessage());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	
	Response sendError(Response.Status status, String err)
	{
		return Response.status(status).entity(Utilities.errorMsgToErrorJSON(err)).build();
	}
	Response sendInfoError(Response.Status status, UserInfo info)
	{
		return Response.status(status).entity(info.getErrorJson()).build();
	}
}

