package com.ibm.airlock.admin.analytics;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.Map;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import javax.ws.rs.core.Response.Status;

import javax.servlet.ServletContext;

import com.ibm.airlock.Strings;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Constants.Stage;
import com.ibm.airlock.admin.Product;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.ValidationResults;
import com.ibm.airlock.admin.authentication.JwtData;

public class AnalyticsUtilities {
	
	public static final Logger logger = Logger.getLogger(AnalyticsUtilities.class.getName());

	public static final int CONNECTION_TIMEOUT_MS = 120000; //2 minutes
	
	//if the return code is OK (200) the result is null, otherwise the results contains the error code and error message 
	public static ValidationResults getExperimentFromAnalyticsServer(ServletContext context, String experimentId) throws Exception {

		String analyticsServerUrl = (String)context.getAttribute(Constants.ANALYTICS_SERVER_URL_PARAM_NAME);
		if (analyticsServerUrl==null || analyticsServerUrl.isEmpty())
			return null;

		URL url = new URL(analyticsServerUrl+"/"+experimentId);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setReadTimeout(CONNECTION_TIMEOUT_MS);
		con.setConnectTimeout(CONNECTION_TIMEOUT_MS);
		
		con.setRequestMethod("GET");

		//add assertion as request header		
		String sessionToken = JwtData.createAnalyticsJWT();
		con.setRequestProperty ("sessionToken", sessionToken);


		// Send get request
		int responseCode = con.getResponseCode();

		if (responseCode == 404) {
			return new ValidationResults("{}", Status.OK); //if the experiment does not exist in the analytics server return empty json
		}
		
		if ((responseCode >= 200 && responseCode <= 299) || con.getErrorStream() == null)			
			return new ValidationResults(Utilities.streamToString(con.getInputStream()), Status.OK);

		String erroMsgJsonStr = Utilities.streamToString(con.getErrorStream());
		JSONObject erroMsgJson = new JSONObject(erroMsgJsonStr);
		return new ValidationResults(Strings.failGetExperimentFromAnalyticsServer + erroMsgJson.getString("error"), Status.fromStatusCode(responseCode)/*Status.INTERNAL_SERVER_ERROR*/);

	}

	//if the return code is OK (200) the result is null, otherwise the results contains the error code and error message 
	public static ValidationResults addExperimentToAnalyticsServer(ServletContext context, Experiment addedExperiment) throws Exception {

		String analyticsServerUrl = (String)context.getAttribute(Constants.ANALYTICS_SERVER_URL_PARAM_NAME);
		if (analyticsServerUrl==null || analyticsServerUrl.isEmpty())
			return null;

		//if no ranges exist - don't publish to analytics server 
		if (addedExperiment.getRangesList() == null || addedExperiment.getRangesList().size() == 0)
			return null;

		URL url = new URL(analyticsServerUrl+"/"+addedExperiment.getUniqueId().toString());
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setReadTimeout(CONNECTION_TIMEOUT_MS);
		con.setConnectTimeout(CONNECTION_TIMEOUT_MS);
		
		con.setRequestMethod("POST");

		//add assertion as request header		
		String sessionToken = JwtData.createAnalyticsJWT();
		con.setRequestProperty ("sessionToken", sessionToken);

		// Send post request
		con.setDoOutput(true);
		String data = getExperimentJSONForAnalyticsServer(addedExperiment, context);
		con.getOutputStream().write(data.getBytes("UTF-8"));

		int code = con.getResponseCode();
		if (code ==  Status.OK.getStatusCode()) {
			logger.info("publish experiment creation to analytics servr: " + url);
			return null;
		}

		String erroMsgJsonStr = Utilities.streamToString(con.getErrorStream());
		JSONObject erroMsgJson = new JSONObject(erroMsgJsonStr);
		return new ValidationResults(Strings.failAddingExperiment + erroMsgJson.getString("error"), Status.INTERNAL_SERVER_ERROR);
	}

	//if the return code is OK (200) the result is null, otherwise the results contains the error code and error message 
	public static ValidationResults deleteExperimentFromAnalyticsServer(ServletContext context, String experimentId) throws Exception {

		String analyticsServerUrl = (String)context.getAttribute(Constants.ANALYTICS_SERVER_URL_PARAM_NAME);
		if (analyticsServerUrl==null || analyticsServerUrl.isEmpty())
			return null;

		URL url = new URL(analyticsServerUrl+"/"+experimentId);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setReadTimeout(CONNECTION_TIMEOUT_MS);
		con.setConnectTimeout(CONNECTION_TIMEOUT_MS);
		
		con.setRequestMethod("DELETE");

		//add assertion as request header		
		String sessionToken = JwtData.createAnalyticsJWT();
		con.setRequestProperty ("sessionToken", sessionToken);

		int code = con.getResponseCode();
		if (code ==  Status.OK.getStatusCode()) {
			logger.info("publish experiment deletion to analytics servr: " + url);		
			return null;
		}
		
		if (code == Status.NOT_FOUND.getStatusCode()) {
			logger.info("experiment " + experimentId + " was not found in Analytics service hence was not removed from analytics service");
			return null;
		}
		
		String erroMsgJsonStr = Utilities.streamToString(con.getErrorStream());
		JSONObject erroMsgJson = new JSONObject(erroMsgJsonStr);
		return new ValidationResults(Strings.failDeletingExperiment + erroMsgJson.getString("error"), Status.INTERNAL_SERVER_ERROR);
	}


	//if the return code is OK (200) the result is null, otherwise the results contains the error code and error message 
	public static ValidationResults updateExperimentInAnalyticsServer(ServletContext context, Experiment updatedExperiment, 
			JSONObject updatedExpJSON, Date now, UUID updatedVriantID, JSONObject updatedVariantJSON,
			UUID updatedProductID, String newProductName) throws Exception {

		String analyticsServerUrl = (String)context.getAttribute(Constants.ANALYTICS_SERVER_URL_PARAM_NAME);
		if (analyticsServerUrl==null || analyticsServerUrl.isEmpty())
			return null;

		String data = getUpdatedExperimentJSONForAnalyticsServer(updatedExperiment, context, updatedExpJSON, now, updatedVriantID, updatedVariantJSON, updatedProductID, newProductName);
		if (data == null)
			return null; //no ranges

		URL url = new URL(analyticsServerUrl+"/"+updatedExperiment.getUniqueId().toString());
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setReadTimeout(CONNECTION_TIMEOUT_MS);
		con.setConnectTimeout(CONNECTION_TIMEOUT_MS);
		
		boolean inUpdate = true;
		if ((updatedExperiment.getRangesList() == null || updatedExperiment.getRangesList().size() == 0) && updatedExpJSON!=null &&
				updatedExpJSON.getBoolean(Constants.JSON_FEATURE_FIELD_ENABLED)) { 
			con.setRequestMethod("POST"); //if it is the first range and the experiment was now updated from disabled to enabled call addExperimenyt since wasn't published to analytics before
			inUpdate = false;
		}
		else {
			con.setRequestMethod("PUT");			
		}

		//add assertion as request header		
		String sessionToken = JwtData.createAnalyticsJWT();
		con.setRequestProperty ("sessionToken", sessionToken);

		// Send post request
		con.setDoOutput(true);

		con.getOutputStream().write(data.getBytes("UTF-8"));

		int code = con.getResponseCode();
		if (code ==  Status.OK.getStatusCode()) {
			logger.info("publish experiment update to analytics servr: " + url);
			return null;
		}

		String errorString = Utilities.streamToString(con.getErrorStream());
		
		if (inUpdate && code == Status.NOT_FOUND.getStatusCode()) {
			//can be pre 4.0 experiments that wasn't published to analytics yet
			HttpURLConnection secondCon = (HttpURLConnection) url.openConnection();
			secondCon.setRequestMethod("POST"); //create the experiemnt in analytics
			secondCon.setRequestProperty ("sessionToken", sessionToken);
			secondCon.setDoOutput(true);

			secondCon.getOutputStream().write(data.getBytes("UTF-8"));

			code = secondCon.getResponseCode();
			if (code ==  Status.OK.getStatusCode()) {
				logger.info("publish experiment update to analytics servr: " + url);
				return null;
			}
			
			errorString = Utilities.streamToString(secondCon.getErrorStream());
		}
		
		String erroMsgJsonStr = errorString;
		JSONObject erroMsgJson = new JSONObject(erroMsgJsonStr);
		return new ValidationResults(Strings.failUpdatingExperiment + erroMsgJson.getString("error"), Status.INTERNAL_SERVER_ERROR);
	}

	private static String getUpdatedExperimentJSONForAnalyticsServer(Experiment updatedExperiment,
			ServletContext context, JSONObject updatedExpJSON, Date now, UUID updatedVriantID,
			JSONObject updatedVariantJSON, UUID updatedProductID, String newProductName) throws JSONException {
		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);			

		@SuppressWarnings("unchecked")
		Map<String, Experiment> experimentsDB = (Map<String, Experiment>)context.getAttribute(Constants.EXPERIMENTS_DB_PARAM_NAME);

		Product prod = productsDB.get(updatedExperiment.getProductId().toString());

		String prodName = prod.getName();
		if (newProductName!=null && prod.getUniqueId().equals(updatedProductID))
			prodName = newProductName;

		Experiment exp = updatedExperiment;

		if (updatedExpJSON!=null) {
			if (updatedExpJSON.getString(Constants.JSON_FIELD_UNIQUE_ID).equals(exp.getUniqueId().toString())) {
				Experiment tmpExp = new Experiment(exp.getProductId());
				tmpExp.fromJSON(updatedExpJSON, context, false);

				Experiment origExp = experimentsDB.get(tmpExp.getUniqueId().toString());
				if (tmpExp.enabled == false && origExp.enabled == true) {
					tmpExp.closeExistingExperimentRange(now);
				}

				if (tmpExp.enabled == true && origExp.enabled == false) {
					tmpExp.addExperimentRange(now);
				}

				//moving enabled experiment to prod - close previous range and open a new one 
				if (tmpExp.stage.equals(Stage.PRODUCTION) && origExp.stage.equals(Stage.DEVELOPMENT) && tmpExp.enabled == true && origExp.enabled == true) {
					tmpExp.closeExistingExperimentRange(now);
					tmpExp.addExperimentRange(now);
					//tmpExp.deletePerviousRangesAndCreateNewRange(now);
				}

				exp=tmpExp;
			}
		}

		JSONObject expObj = new JSONObject();

		//ranges
		JSONArray rangesArr = exp.getRangesJsonArray();
		if (rangesArr == null || rangesArr.length()==0) {
			return null;  
		}

		expObj.put(Constants.JSON_FIELD_RANGES, rangesArr);


		//name
		expObj.put(Constants.JSON_FIELD_NAME, exp.getName());

		//id
		expObj.put(Constants.JSON_FIELD_EXPERIMENT_ID, exp.getUniqueId().toString());

		//productName
		expObj.put(Constants.JSON_FIELD_PRODUCT, prodName);

		//productId
		expObj.put(Constants.JSON_FIELD_PRODUCT_ID, prod.getUniqueId().toString());

		//hypothesis
		expObj.put(Constants.JSON_FIELD_HYPOTHESIS, exp.getHypothesis());

		//hypothesis
		expObj.put(Constants.JSON_FIELD_DISPLAY_NAME, exp.getDisplayName());

		//owner
		String expOwner = exp.getOwner();
		if (expOwner == null)
			expOwner = exp.getCreator();

		expObj.put(Constants.JSON_FEATURE_FIELD_OWNER, expOwner);

		//indexExperiment
		expObj.put(Constants.JSON_FIELD_INDEX_EXPERIMENT, exp.getIndexExperiment());

		//Stage
		expObj.put(Constants.JSON_FEATURE_FIELD_STAGE, exp.getStage().toString());
		
		//variants
		List<String> variants = new LinkedList<String>();
		List<String> variantsDescriptions = new LinkedList<String>();
		List<String> variantsDisplayNames = new LinkedList<String>();

		//controlGroupVariants
		expObj.put(Constants.JSON_FIELD_CONTROL_GROUP_VARIANTS, exp.getControlGroupVariants());

		for (Variant var:exp.getVariants()) {
			if (updatedVriantID!=null && updatedVriantID.equals(var.getUniqueId())) {
				if (updatedVariantJSON == null) {		
					//if variant is deleted remove it from the control group list if exists there
					if (exp.getControlGroupVariants().contains(var.getName())) {
						JSONArray cgArr = expObj.getJSONArray(Constants.JSON_FIELD_CONTROL_GROUP_VARIANTS);						
						for (int i=0; i<cgArr.size(); i++) {
							if (cgArr.getString(i).equals(var.getName())) {
								cgArr.remove(i);
								break;
							}								
						}						
					}
					continue; //this variant is deleted					
				}
				else {
					variants.add(updatedVariantJSON.getString(Constants.JSON_FIELD_NAME));
					String updatedVariantDesc = "";
					if (updatedVariantJSON.containsKey(Constants.JSON_FIELD_DESCRIPTION) && updatedVariantJSON.get(Constants.JSON_FIELD_DESCRIPTION)!=null) {
						updatedVariantDesc = updatedVariantJSON.getString(Constants.JSON_FIELD_DESCRIPTION);
					}
					variantsDescriptions.add(updatedVariantDesc);

					String updatedVariantDisplayName = null;
					if (updatedVariantJSON.containsKey(Constants.JSON_FIELD_DISPLAY_NAME) && updatedVariantJSON.get(Constants.JSON_FIELD_DISPLAY_NAME)!=null) {
						updatedVariantDisplayName = updatedVariantJSON.getString(Constants.JSON_FIELD_DISPLAY_NAME);
					}
					variantsDisplayNames.add(updatedVariantDisplayName);
				}
			}
			else {
				variants.add(var.getName());
				variantsDescriptions.add(var.getDescription() == null ? "" : var.getDescription());
				variantsDisplayNames.add(var.getDisplayName());
			}
		}
				
		expObj.put(Constants.JSON_FIELD_VARIANTS, variants);
		expObj.put(Constants.JSON_FIELD_VARIANTS_DESCRIPTIONS, variantsDescriptions);				
		expObj.put(Constants.JSON_FIELD_VARIANTS_DISPLAY_NAMES, variantsDisplayNames);

		//description
		expObj.put(Constants.JSON_FIELD_DESCRIPTION, exp.getDescription());

		return expObj.toString();
	}

	private static String getExperimentJSONForAnalyticsServer (Experiment exp, ServletContext context) throws JSONException {

		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);			

		Product prod = productsDB.get(exp.getProductId().toString());

		JSONObject expObj = new JSONObject();

		//ranges
		JSONArray rangesArr = exp.getRangesJsonArray();
		expObj.put(Constants.JSON_FIELD_RANGES, rangesArr);


		//name
		expObj.put(Constants.JSON_FIELD_NAME, exp.getName());

		//id
		expObj.put(Constants.JSON_FIELD_EXPERIMENT_ID, exp.getUniqueId().toString());

		//productName
		expObj.put(Constants.JSON_FIELD_PRODUCT, prod.getName());

		//productId
		expObj.put(Constants.JSON_FIELD_PRODUCT_ID, prod.getUniqueId().toString());

		//hypothesis
		expObj.put(Constants.JSON_FIELD_HYPOTHESIS, exp.getHypothesis());

		//hypothesis
		expObj.put(Constants.JSON_FIELD_DISPLAY_NAME, exp.getDisplayName());

		//owner
		String expOwner = exp.getOwner();
		if (expOwner == null)
			expOwner = exp.getCreator();

		expObj.put(Constants.JSON_FEATURE_FIELD_OWNER, expOwner);

		//indexExperiment
		expObj.put(Constants.JSON_FIELD_INDEX_EXPERIMENT, exp.getIndexExperiment());

		//Stage
		expObj.put(Constants.JSON_FEATURE_FIELD_STAGE, exp.getStage().toString());

		//controlGroupVariants
		expObj.put(Constants.JSON_FIELD_CONTROL_GROUP_VARIANTS, exp.getControlGroupVariants());

		//variants
		List<String> variants = new LinkedList<String>();
		List<String> variantsDescriptions = new LinkedList<String>();
		List<String> variantsDisplayNames = new LinkedList<String>();

		expObj.put(Constants.JSON_FIELD_VARIANTS, variants);
		expObj.put(Constants.JSON_FIELD_VARIANTS_DESCRIPTIONS, variantsDescriptions);				
		expObj.put(Constants.JSON_FIELD_VARIANTS_DISPLAY_NAMES, variantsDisplayNames);

		//description
		expObj.put(Constants.JSON_FIELD_DESCRIPTION, exp.getDescription());

		return expObj.toString();
	}

	public static ValidationResults updateDeletedProductToAnalytics(ServletContext context, Product prodToDel) throws Exception {
		for (Experiment exp:prodToDel.getExperimentsMutualExclusionGroup().getExperiments()) {
			deleteExperimentFromAnalyticsServer(context, exp.getUniqueId().toString());
		}
		return null;
	}

	public static ValidationResults updateNewProductNameToAnalytics(ServletContext context, Product prod, String newProdName) throws Exception {
		for (Experiment exp:prod.getExperimentsMutualExclusionGroup().getExperiments()) {
			Date now = new Date();
			updateExperimentInAnalyticsServer(context, exp, null, now, null, null, prod.getUniqueId(), newProdName);
		}
		return null;		
	}



	//if the return code is OK (200) the result is null, otherwise the results contains the error code and error message 
	public static ValidationResults getExperimentsFromAnalyticsServer(ServletContext context) throws Exception {

		String analyticsServerUrl = (String)context.getAttribute(Constants.ANALYTICS_SERVER_URL_PARAM_NAME);
		if (analyticsServerUrl==null || analyticsServerUrl.isEmpty())
			return null;

		URL url = new URL(analyticsServerUrl);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setReadTimeout(CONNECTION_TIMEOUT_MS);
		con.setConnectTimeout(CONNECTION_TIMEOUT_MS);
		
		con.setRequestMethod("GET");

		//add assertion as request header		
		String sessionToken = JwtData.createAnalyticsJWT();
		con.setRequestProperty ("sessionToken", sessionToken);


		// Send get request
		int responseCode = con.getResponseCode();

		if ((responseCode >= 200 && responseCode <= 299) || con.getErrorStream() == null)			
			return new ValidationResults(Utilities.streamToString(con.getInputStream()), Status.OK);

		String erroMsgJsonStr = Utilities.streamToString(con.getErrorStream());
		JSONObject erroMsgJson = new JSONObject(erroMsgJsonStr);
		return new ValidationResults(Strings.failGetExperimentsFromAnalyticsServer + erroMsgJson.getString("error"), Status.fromStatusCode(responseCode)/*Status.INTERNAL_SERVER_ERROR*/);
	}

	public static JSONObject mergeIndexingDataIntoExperimentsList(JSONObject experimentsJson, JSONObject indexingDataJson) throws JSONException {
		JSONArray exerimentsArr = experimentsJson.getJSONArray(Constants.JSON_FIELD_EXPERIMENTS);
		JSONArray indexingDataArr = indexingDataJson.getJSONArray(Constants.JSON_FIELD_EXPERIMENTS);
		
		for (int i=0; i<exerimentsArr.size(); i++) {
			JSONObject expIndexingData = new JSONObject();
			JSONObject expJson = exerimentsArr.getJSONObject(i);
			String expId = expJson.getString(Constants.JSON_FIELD_UNIQUE_ID);
			
			for (int j=0; j<indexingDataArr.size(); j++) {
				JSONObject expIndexingDataJson = indexingDataArr.getJSONObject(j);
				String indexingDataExpId = expIndexingDataJson.getString(Constants.JSON_FIELD_EXPERIMENT_ID);
				if (expId.equals(indexingDataExpId)) {
					expJson.put (Constants.JSON_FIELD_INDEXING_INFO, expIndexingDataJson);
					expIndexingData = expIndexingDataJson;
					break;
				}								
			}
			
			expJson.put (Constants.JSON_FIELD_INDEXING_INFO, expIndexingData);
		}
		return experimentsJson;
	}

	//if the return code is OK (200) the result is null, otherwise the results contains the error code and error message 
	public static ValidationResults resetDashboardInAnalyticsServer(ServletContext context, String experimentId) throws Exception {

		String analyticsServerUrl = (String)context.getAttribute(Constants.ANALYTICS_SERVER_URL_PARAM_NAME);
		if (analyticsServerUrl==null || analyticsServerUrl.isEmpty())
			return null;

		URL url = new URL(analyticsServerUrl+"/"+experimentId+"/resetdashboard");
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setReadTimeout(CONNECTION_TIMEOUT_MS);
		con.setConnectTimeout(CONNECTION_TIMEOUT_MS);
		
		con.setRequestMethod("PUT");

		//add assertion as request header		
		String sessionToken = JwtData.createAnalyticsJWT();
		con.setRequestProperty ("sessionToken", sessionToken);

		// Send get request
		int responseCode = con.getResponseCode();

		if ((responseCode >= 200 && responseCode <= 299) || con.getErrorStream() == null)			
			return null;

		String erroMsgJsonStr = Utilities.streamToString(con.getErrorStream());
		JSONObject erroMsgJson = new JSONObject(erroMsgJsonStr);
		return new ValidationResults(Strings.failResetExperimentDashboardInAnalyticsServer + erroMsgJson.getString("error"), Status.fromStatusCode(responseCode)/*Status.INTERNAL_SERVER_ERROR*/);

	}

}
