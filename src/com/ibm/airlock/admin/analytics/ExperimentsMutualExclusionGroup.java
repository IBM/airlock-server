package com.ibm.airlock.admin.analytics;

import java.util.Date;
import java.util.Map;
import java.util.LinkedList;
import java.util.UUID;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Response.Status;

import com.ibm.airlock.admin.MergeBranch.MergeException;
import com.ibm.airlock.admin.Product;
import com.ibm.airlock.admin.Season;
import com.ibm.airlock.admin.ValidationResults;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import com.ibm.airlock.Constants;
import com.ibm.airlock.Strings;
import com.ibm.airlock.Constants.OutputJSONMode;
import com.ibm.airlock.Constants.Stage;
import com.ibm.airlock.admin.authentication.UserInfo;

public class ExperimentsMutualExclusionGroup {

	private Integer maxExperimentsOn = 1;  //optional field
	private LinkedList<Experiment> experiments = new LinkedList<Experiment>();
	private UUID productId = null; //required in create and update
	private Date lastModified = new Date(); 

	public ExperimentsMutualExclusionGroup(UUID productId) {
		this.productId = productId;
	}
	public Integer getMaxExperimentsOn() {
		return maxExperimentsOn;
	}
	public void setMaxExperimentsOn(Integer maxExperimentsOn) {
		this.maxExperimentsOn = maxExperimentsOn;
	}
	public LinkedList<Experiment> getExperiments() {
		return experiments;
	}
	public void setExperiments(LinkedList<Experiment> experiments) {
		this.experiments = experiments;
	}
	public UUID getProductId() {
		return productId;
	}
	public void setProductId(UUID productId) {
		this.productId = productId;
	}
	public Date getLastModified() {
		return lastModified;
	}
	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

	public void fromJSON(JSONObject input, ServletContext context) throws JSONException {

		if (input.containsKey(Constants.JSON_FIELD_MAX_EXPERIMENTS_ON) && input.get(Constants.JSON_FIELD_MAX_EXPERIMENTS_ON)!=null) {
			maxExperimentsOn = input.getInt(Constants.JSON_FIELD_MAX_EXPERIMENTS_ON);			
		}

		if (input.containsKey(Constants.JSON_FIELD_PRODUCT_ID) && input.get(Constants.JSON_FIELD_PRODUCT_ID) != null) {
			String sStr = input.getString(Constants.JSON_FIELD_PRODUCT_ID);			
			productId = UUID.fromString(sStr);			
		}	

		if (input.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) && input.get(Constants.JSON_FIELD_LAST_MODIFIED)!=null) { 
			long timeInMS = (Long)input.get(Constants.JSON_FIELD_LAST_MODIFIED);
			lastModified = new Date(timeInMS);
		}  else {
			lastModified = new Date();
		}

		if (input.containsKey(Constants.JSON_FIELD_EXPERIMENTS) && input.get(Constants.JSON_FIELD_EXPERIMENTS) != null) {
			experiments.clear();

			@SuppressWarnings("unchecked")
			Map<String, Experiment> experimentsDB = (Map<String, Experiment>)context.getAttribute(Constants.EXPERIMENTS_DB_PARAM_NAME);

			if (input.containsKey(Constants.JSON_FIELD_EXPERIMENTS) && input.get(Constants.JSON_FIELD_EXPERIMENTS)!=null) {
				JSONArray experimentsArr = input.getJSONArray(Constants.JSON_FIELD_EXPERIMENTS); //after validation - i know it is json array
				for (int i=0; i<experimentsArr.size(); i++) {
					JSONObject expJsonObj = experimentsArr.getJSONObject(i); //after validation - i know it is json object
					Experiment exp = new Experiment(productId);
					exp.fromJSON(expJsonObj, context, true);
					experiments.add(exp);
					experimentsDB.put(exp.getUniqueId().toString(), exp); //this function is only called when server initialized - hence added to experiments db
				}			
			}	
		}
	}

	public JSONObject toJson(OutputJSONMode mode, ServletContext context, boolean skipDisabledVariant) throws JSONException {
		JSONObject res = new JSONObject();
		res.put(Constants.JSON_FIELD_MAX_EXPERIMENTS_ON, maxExperimentsOn);
		
		if (mode == OutputJSONMode.ADMIN || mode == OutputJSONMode.DISPLAY) {
			res.put(Constants.JSON_FIELD_PRODUCT_ID, productId==null?null:productId.toString());			
			res.put(Constants.JSON_FIELD_LAST_MODIFIED, lastModified.getTime());
		}

		JSONArray experimentsArr = new JSONArray();
		for (Experiment exp:experiments) {
			experimentsArr.add (exp.toJson(OutputJSONMode.ADMIN, context, skipDisabledVariant, false));
		}
		res.put(Constants.JSON_FIELD_EXPERIMENTS, experimentsArr);

		return res;
	}

	public ValidationResults validateExpMutualExclusionGroupJSON(JSONObject megObj, ServletContext context, UserInfo userInfo) throws MergeException {
		//Can only be in update

		try {
			//modification date must appear
			if (!megObj.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) || megObj.get(Constants.JSON_FIELD_LAST_MODIFIED)==null) {
				return new ValidationResults(Strings.lastModifiedIsMissing, Status.BAD_REQUEST);
			}

			//maxFeaturesOn
			if (megObj.containsKey(Constants.JSON_FIELD_MAX_EXPERIMENTS_ON) && megObj.get(Constants.JSON_FIELD_MAX_EXPERIMENTS_ON) != null) {
				int meo = megObj.getInt(Constants.JSON_FIELD_MAX_EXPERIMENTS_ON); //validate that is Integer value
				if (meo<=0) {
					return new ValidationResults("The Maximal Number of Experiments On should be an integer greater than 1.", Status.BAD_REQUEST);
				}
			}

			//product id must exists and not be changed
			String productIdStr = megObj.getString(Constants.JSON_FIELD_PRODUCT_ID);
			if (!productIdStr.equals(productId.toString())) {
				return new ValidationResults(String.format(Strings.fieldCannotBeChangedDuringUpdate, Constants.JSON_FIELD_PRODUCT_ID), Status.BAD_REQUEST);
			}

			JSONArray experimentsArr = megObj.getJSONArray(Constants.JSON_FIELD_EXPERIMENTS);
			for (int i=0; i<experimentsArr.size(); i++) {
				JSONObject expJSONObj = experimentsArr.getJSONObject(i);
				Experiment exp = new Experiment(productId);
				exp.validateExperimentJSON(expJSONObj, context, userInfo);								
			}
			//validate that no experimant was added to list or removed from list 
			String err = validateExperimentsList(experimentsArr);
			if (err!=null)
				return new ValidationResults(err, Status.BAD_REQUEST);

		} catch (JSONException jsne) {
			return new ValidationResults(jsne.getMessage(), Status.BAD_REQUEST);
		}
		catch (ClassCastException cce) {
			return new ValidationResults("Illegal JSON: " + cce.getMessage(), Status.BAD_REQUEST);
		} 

		return null;
	}	

	//validate that no variant was added to list or removed from list
	//return error message is variant was add or removed from list, null otherwise
	private String validateExperimentsList(JSONArray experimentsArr) throws JSONException {
		if (experimentsArr.size() > experiments.size()) {
			return "An experiment cannot be added to the experiment's list when the list is being updated. Instead, call add experiment.";						 
		}

		if (experimentsArr.size() < experiments.size()) {
			return "An experiment cannot be removed from the experiment's list when the list is being updated. Instead, call delete experiment.";			 	
		}

		//same number of variants - look at the uniqueIds
		for (Experiment exp:experiments) {
			boolean found = false;
			for (int i=0; i<experimentsArr.size(); i++) { 
				JSONObject expJSON = experimentsArr.getJSONObject(i); //after validate - i know that is json and containns uniqueId field

				if (exp.getUniqueId().toString().equals(expJSON.getString(Constants.JSON_FIELD_UNIQUE_ID))) {
					found = true;
					break;
				}
			}
			if (!found) {
				return "An experiment cannot be added or removed from the experiment's list when the list is being updated. Instead, call add or delete experiment.";
			}
		}
		return null;
	}

	public void addExperiment(Experiment experiment) {
		experiments.add(experiment);
		lastModified = new Date();
	}
	
	public String removeExperiment(UUID experimentId) {
		//TODO: remove all variants as well
		for (int i=0; i< experiments.size(); i++) {
			if (experiments.get(i).getUniqueId().equals(experimentId)) {
				experiments.remove(i);
				lastModified = new Date();
				return null;
			}
		}
		
		return "Unable to remove experiment " + experimentId.toString() + ": The specified experiment does not exist in this season.";
	}
	
	public String updateExpMutualExclusionGroup (JSONObject updatedExpMEGJSON, ServletContext context, Date now) throws JSONException {
		
		StringBuilder updateDetails = new StringBuilder();
		boolean wasChanged = false;
			
		//optional field
		if (updatedExpMEGJSON.containsKey(Constants.JSON_FIELD_MAX_EXPERIMENTS_ON) &&  updatedExpMEGJSON.get(Constants.JSON_FIELD_MAX_EXPERIMENTS_ON)!=null) {
			int updatedMaxExperimentsOn = updatedExpMEGJSON.getInt(Constants.JSON_FIELD_MAX_EXPERIMENTS_ON);
			if (maxExperimentsOn  != updatedMaxExperimentsOn) {
				updateDetails.append("'maxExperimentsOn' changed from " + maxExperimentsOn + " to " + updatedMaxExperimentsOn + "\n");
				maxExperimentsOn = updatedMaxExperimentsOn;		
				wasChanged = true;
			}
		}
		
		@SuppressWarnings("unchecked")
		Map<String, Experiment> experimentsDB = (Map<String, Experiment>)context.getAttribute(Constants.EXPERIMENTS_DB_PARAM_NAME);
		
		//variants order	
		JSONArray updatedExperimentsArray = updatedExpMEGJSON.getJSONArray(Constants.JSON_FIELD_EXPERIMENTS); //after validate - i know exists		
		if (orderChanged (updatedExperimentsArray, experiments)) {
			addAuditMsgForOrderChange(updatedExperimentsArray, updateDetails, context);
			//create new features list - add them one by one and switch with original list
			LinkedList<Experiment> newExperiments = new LinkedList<Experiment>();
			for (int i=0; i<updatedExperimentsArray.size(); i++) {					
				String id  = updatedExperimentsArray.getJSONObject(i).getString(Constants.JSON_FIELD_UNIQUE_ID);										
				Experiment curVariant = experimentsDB.get(id);
				newExperiments.add(curVariant);
			}				
			experiments = newExperiments;
			wasChanged = true;
		}	
		
		//experiment update (can update experiment while updating experiments list)
		int i=0;
		for (Experiment exp:experiments) {
			String updateExpDetails = exp.updateExperiment(updatedExperimentsArray.getJSONObject(i), context, now); //i know that the json array and variant list are now at the same order.
			if (!updateExpDetails.isEmpty()) {
				updateDetails.append("In experiment " + exp.name + ", " + exp.getUniqueId().toString() + ": \n" + updateExpDetails);
				wasChanged = true;
			}
			i++;
		}
		
		if (wasChanged) {
			lastModified = now;
		}
		
		return updateDetails.toString();
	}
	
	protected boolean orderChanged(JSONArray updatedExperiments,  LinkedList<Experiment> origExperiments) throws JSONException {
		if (origExperiments.size() != updatedExperiments.size()) {
			return true; //experiments added hence order changed
		}

		for (int i=0; i<origExperiments.size(); i++) {
			if (!origExperiments.get(i).getUniqueId().toString().equals(updatedExperiments.getJSONObject(i).getString(Constants.JSON_FIELD_UNIQUE_ID))) {
				return true;
			}
		}

		return false;
	}
	
	private void addAuditMsgForOrderChange(JSONArray updatedExperimentsArray, StringBuilder updateDetails, ServletContext context) throws JSONException {
		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);
		
		Product prod = productsDB.get(productId.toString());
		
		updateDetails.append("The order of experiments under product " + prod.getName()+"("+ productId.toString() + ") changed.\n\nBefore:\n");
		for (int i=0; i<experiments.size(); i++) {
			updateDetails.append(experiments.get(i).getName() + "(" + experiments.get(i).getUniqueId().toString() + ")\n");
		}

		updateDetails.append("\n After:\n");
		for (int i=0; i<updatedExperimentsArray.size(); i++) {
			JSONObject variant = updatedExperimentsArray.getJSONObject(i);
			updateDetails.append(variant.getString(Constants.JSON_FIELD_NAME) + "(" + variant.getString(Constants.JSON_FIELD_UNIQUE_ID) +")\n");
		}

		updateDetails.append("\n");
	}
	
	public ValidationResults validateProductionDontChanged(JSONObject updatedExperimentsJSON) throws JSONException {
		//validate that the order of experiments that are on production was not changed:
		
		String err = "Unable to update the experiments list. Only a user with the Administrator or Product Lead role can change an experiments that are in the production stage.";			
				
		JSONArray updatedExperimentsArray = updatedExperimentsJSON.getJSONArray(Constants.JSON_FIELD_EXPERIMENTS); //after validate - i know exists
		
		JSONArray updatedExperimentsInProduction = new JSONArray();
		//create updated variants list that are on production
		for (int i=0; i<updatedExperimentsArray.size(); i++) {
			JSONObject experimentJSONObj = updatedExperimentsArray.getJSONObject(i);
			if (experimentJSONObj.containsKey(Constants.JSON_FEATURE_FIELD_STAGE) && experimentJSONObj.getString(Constants.JSON_FEATURE_FIELD_STAGE).equals(Stage.PRODUCTION.toString())) {
				updatedExperimentsInProduction.add(experimentJSONObj);
			}
		}
		
		//create current experiments list that are on production
		LinkedList<Experiment> experimentsInProduction = new LinkedList<Experiment>();
		for (int i=0; i<experiments.size(); i++) {
			//JSONObject subFeatureJSONObj = updatedSubFeatures.getJSONObject(i);
			if (experiments.get(i).getStage().equals(Stage.PRODUCTION)) {
				experimentsInProduction.add(experiments.get(i));
			}			
		}
					
		
		if (orderChanged (updatedExperimentsInProduction, experimentsInProduction)) {
			return new ValidationResults(err, Status.UNAUTHORIZED);
		}
		return null;
	}
	
	public JSONObject getExperimentsArrayByStageForSeason(OutputJSONMode outputMode, ServletContext context, Season season, boolean skipDisabled) throws JSONException {
		//only for RUNTIME_DEVELOPMENT and RUNTIME_PRODUCTION modes
		//if requested - skip disabled experiments and variant
		
		JSONObject res = new JSONObject();
		res.put(Constants.JSON_FIELD_MAX_EXPERIMENTS_ON, maxExperimentsOn);
		
		JSONArray experimentsArr = new JSONArray();
		for (Experiment exp:experiments) {
			
			if (!Product.isSeasonInRange(season, exp.getMinVersion(), exp.getMaxVersion()))
					continue;
			
			if (skipDisabled && (exp.getEnabled() == false))
				continue; //skip disabled experiment
			
			if (outputMode == OutputJSONMode.RUNTIME_PRODUCTION) {
				if (exp.getStage() == Stage.DEVELOPMENT) 
					continue;
			}
			
			
			experimentsArr.add (exp.toJson(outputMode, context, skipDisabled, true));
		}
		res.put(Constants.JSON_FIELD_EXPERIMENTS, experimentsArr);

		return res;				
	}
	
}
