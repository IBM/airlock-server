package com.ibm.airlock.admin;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response.Status;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Constants.InputSampleGenerationMode;
import com.ibm.airlock.Constants.OutputJSONMode;
import com.ibm.airlock.Constants.REQUEST_ITEM_TYPE;
import com.ibm.airlock.Constants.Stage;
import com.ibm.airlock.Strings;
import com.ibm.airlock.admin.BaseAirlockItem.Type;
import com.ibm.airlock.admin.MergeBranch.MergeException;
import com.ibm.airlock.admin.analytics.AnalyticsDataCollection;
import com.ibm.airlock.admin.analytics.BaseStagedAnalyticsItem;
import com.ibm.airlock.admin.analytics.Experiment;
import com.ibm.airlock.admin.analytics.Variant;
import com.ibm.airlock.admin.notifications.AirlockNotification;
import com.ibm.airlock.admin.streams.AddStreamsToSchema;
import com.ibm.airlock.engine.VerifyRule;

public class InputSchema {
	private JSONObject mergedSchema = null; //required in create and update
	private JSONObject schema = null; //required in create and update	
	private Date lastModified = null; // required in update. forbidden in create
	private UUID seasonId = null; //required in create and update
	private static ExecutorService executor = Executors.newCachedThreadPool();
	public InputSchema() {
		//the fields will be filled later on
	}

	public InputSchema (UUID seasonId) throws JSONException {
		this.seasonId = seasonId;
		//this.mergedSchema = new JSONObject();
		this.schema = new JSONObject();
		this.lastModified = new Date();

		JSONObject properties = new JSONObject();		
		properties.put("context",  new JSONObject());
		schema.put("properties", properties);
		schema.put("type", "object");

		mergedSchema = null;
	}

	public JSONObject getMergedSchema() {
		return mergedSchema != null ? mergedSchema : schema;
	}

	public void setMergedSchema(JSONObject schema) {
		this.mergedSchema = schema;
	}

	public JSONObject getSchema() {
		return schema;
	}

	public void setSchema(JSONObject schema) {
		this.schema = schema;
	}

	public Date getLastModified() {
		return lastModified;
	}

	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

	public UUID getSeasonId() {
		return seasonId;
	}

	public void setSeasonId(UUID seasonId) {
		this.seasonId = seasonId;
	}

	public JSONObject toJson() throws JSONException {
		JSONObject res = new JSONObject();
		res.put(Constants.JSON_FIELD_INPUT_SCHEMA, schema);				
		res.put(Constants.JSON_FIELD_LAST_MODIFIED, lastModified.getTime()); //last modified is the time-stamp 	
		res.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, seasonId==null?null:seasonId.toString());

		return res;
	}

	//at this stage we are after validate so all mandatory fields are in and the json formate is correct.
	public void fromJSON(JSONObject input) throws JSONException {
		schema = input.getJSONObject(Constants.JSON_FIELD_INPUT_SCHEMA);

		if (input.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) && input.get(Constants.JSON_FIELD_LAST_MODIFIED)!=null) { 
			long timeInMS = input.getLong(Constants.JSON_FIELD_LAST_MODIFIED);
			lastModified = new Date(timeInMS);
		}  else {
			lastModified = new Date();
		}	

		if (input.containsKey(Constants.JSON_FEATURE_FIELD_SEASON_ID) && input.get(Constants.JSON_FEATURE_FIELD_SEASON_ID) != null) {
			String sStr = input.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID);			
			seasonId = UUID.fromString(sStr);			
		}

		mergedSchema = null; // needs to be recalculated
	}

	//verification results error will be returned upon general error (illegal schema for example)
	//for rule or configuration that was broken the json structure will be edited.
	//in this case the the verification results will be OK(200) and the error string is actually the 
	//broken rules/config json.
	public ValidationResults validateSchemaOnly(JSONObject schemaJson, ServletContext context, Season season) throws MergeException, InterruptedException, ExecutionException {
		try {
			JSONObject brokenRulesAndConfigJSON = new JSONObject();

			//validate the schema
			String ajv = (String)context.getAttribute(Constants.SCHEMA_VALIDATOR_AJV_PARAM_NAME);
			String validator = (String)context.getAttribute(Constants.SCHEMA_VALIDATOR_VALIDATOR_PARAM_NAME);
			String validateLeaves = (String)context.getAttribute(Constants.SCHEMA_VALIDATE_LEAVES_PARAM_NAME);
			String generator = (String)context.getAttribute(Constants.SCHEMA_JSON_GENERATOR_PARAM_NAME);				
			String prune = (String)context.getAttribute(Constants.SCHEMA_JSON_PRUNE_PARAM_NAME);
			String jsf = (String)context.getAttribute(Constants.SCHEMA_JSON_FAKER_PARAM_NAME);

			try {
				SchemaValidator.extendedValidation(validator, validateLeaves, ajv,
						generator, jsf, prune, Stage.DEVELOPMENT.toString(), "9999999",
						schemaJson.toString());
			} catch (ValidationException ve) {
				return new ValidationResults("The Input Schema is not valid: " + ve.getMessage(), Status.BAD_REQUEST);
			}		

			//validate that each rule and configuration are not broken with the new input schema
			LinkedList<JSONObject> brokenRules = new LinkedList<JSONObject>();
			LinkedList<JSONObject> brokenConfigs = new LinkedList<JSONObject>();
			LinkedList<JSONObject> brokenExperiments = new LinkedList<JSONObject>();
			LinkedList<JSONObject> brokenVariants = new LinkedList<JSONObject>();
			LinkedList<JSONObject> brokenNotificationRules = new LinkedList<JSONObject>();
			LinkedList<JSONObject> brokenNotificationConfigs = new LinkedList<JSONObject>();

			JSONObject updatedMergedSchema = null;
			try {
				JSONObject streamsJson = season.getStreams().toJson(OutputJSONMode.ADMIN);
				updatedMergedSchema = mergeSchema (streamsJson, true, schemaJson);
			} catch (GenerationException e) {
				return new ValidationResults("The Input Schema is not valid, fail merging streams to main schema: " + e.getMessage(), Status.BAD_REQUEST);
			}

			String err = validateRulesAndConfigWithNewSchemaOrChangedUtility(season, updatedMergedSchema, context, null, null, null, brokenRules, brokenConfigs, brokenExperiments, brokenVariants, brokenNotificationRules, brokenNotificationConfigs);

			if (err!=null)
				return new ValidationResults(err, Status.BAD_REQUEST);

			brokenRulesAndConfigJSON.put (Constants.JSON_FIELD_BROKEN_RULES, brokenRules);
			brokenRulesAndConfigJSON.put (Constants.JSON_FIELD_BROKEN_CONFIGS, brokenConfigs);
			brokenRulesAndConfigJSON.put (Constants.JSON_FIELD_BROKEN_EXPERIMENTS, brokenExperiments);
			brokenRulesAndConfigJSON.put (Constants.JSON_FIELD_BROKEN_VARIANTS, brokenVariants);
			brokenRulesAndConfigJSON.put (Constants.JSON_FIELD_BROKEN_NOTIFICATION_RULRES, brokenNotificationRules);
			brokenRulesAndConfigJSON.put (Constants.JSON_FIELD_BROKEN_NOTIFICATION_CONFIGS, brokenNotificationConfigs);

			validateInputFieldsForAnalytics(season, brokenRulesAndConfigJSON, updatedMergedSchema, context);


			return new ValidationResults(brokenRulesAndConfigJSON.write(true), Status.OK);
		} catch (JSONException jse) {
			return new ValidationResults(jse.getMessage(), Status.BAD_REQUEST);
		} catch (GenerationException ge) {
			return new ValidationResults("Failed to generate the data sample: " + ge.getMessage(), Status.BAD_REQUEST);
		}

	}



	//will be used from validate and act. In validate the brokenRulesAndConfigJSON 
	//is taken into account while in act the returned validation results are taken into account 
	private ValidationResults validateInputFieldsForAnalytics(Season season, JSONObject brokenRulesAndConfigJSON, JSONObject schemaJson, ServletContext context) throws GenerationException, JSONException, MergeException {
		LinkedList<String> inputFieldsForAnalyticsList = season.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection().getInputFieldsForAnalytics();
		Map<String,String> newInputFieldsMap = null;
		TreeSet<String> deletedInputFields = new TreeSet<String>();

		String err = null;
		if (inputFieldsForAnalyticsList!=null && inputFieldsForAnalyticsList.size()>0) {

			int updatedNumOfProdInputFields = 0;
			newInputFieldsMap = Utilities.getStageForInputFields(context, schemaJson);
			//validate that existing analytics inputFields are not removed
			for (int k=0; k<inputFieldsForAnalyticsList.size(); k++) {
				if (!newInputFieldsMap.containsKey(inputFieldsForAnalyticsList.get(k))) {
					deletedInputFields.add(inputFieldsForAnalyticsList.get(k));
				}
				else {
					String stageStr = newInputFieldsMap.get(inputFieldsForAnalyticsList.get(k));
					if (stageStr.equals(Stage.PRODUCTION.toString())) {
						updatedNumOfProdInputFields++;
					}

				}
			}					

			int currentNumOfProdAnaItems = season.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection().getNumberOfProductionItemsToAnalytics();
			int currentNumOfProdAnaInputFields = season.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection().getNumberOfProductionInputFieldsToAnalytics();
			int analyticsQuota = season.getAnalytics().getAnalyticsQuota();
			if (updatedNumOfProdInputFields > currentNumOfProdAnaInputFields &&
					currentNumOfProdAnaItems-currentNumOfProdAnaInputFields+updatedNumOfProdInputFields > analyticsQuota) {
				err = "Failed updating input schema: The maximum number of items in production to send to analytics was exceeded. The maximum number allowed is " + analyticsQuota + ". " + currentNumOfProdAnaItems + " were previously selected, and updating the input schema increased the number to " + (currentNumOfProdAnaItems-currentNumOfProdAnaInputFields+updatedNumOfProdInputFields);
				brokenRulesAndConfigJSON.put (Constants.JSON_FIELD_PRON_ANALYTICS_ITEMS_QUOTA_EXCEEDED, err);											
			}


		}

		for (Branch branch:season.getBranches().getBranchesList()) {
			inputFieldsForAnalyticsList = branch.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection().getInputFieldsForAnalytics();
			if (inputFieldsForAnalyticsList!=null && inputFieldsForAnalyticsList.size()>0) {

				if (newInputFieldsMap == null)
					newInputFieldsMap = Utilities.getStageForInputFields(context, schemaJson);

				//validate that existing analytics inputFields are not removed
				for (int k=0; k<inputFieldsForAnalyticsList.size(); k++) {
					if (!newInputFieldsMap.containsKey(inputFieldsForAnalyticsList.get(k))) {
						deletedInputFields.add(inputFieldsForAnalyticsList.get(k));
					}						
				}
			}							
		}

		if (deletedInputFields.size()>0) {
			brokenRulesAndConfigJSON.put (Constants.JSON_FIELD_DELETED_ANALYTICS_INPUT_FIELDS, deletedInputFields);					
		}

		if (err!=null)
			return new ValidationResults(err, Status.BAD_REQUEST);

		List<Experiment> seasonsProdExperiments = season.getExperimentsForSeason(context, true);
		if (seasonsProdExperiments!=null && !seasonsProdExperiments.isEmpty()) {
			if (newInputFieldsMap==null) {
				newInputFieldsMap = Utilities.getStageForInputFields(context, schemaJson);
			}
			for (Experiment exp:seasonsProdExperiments) {
				int updatedProdCount = exp.simulateProdCounterUponNewInputSchema(newInputFieldsMap, context);
				int experimentAnalyticsQuota = exp.getQuota(context);
				if (updatedProdCount > experimentAnalyticsQuota) {
					err = "Failed to update the input schema. The maximum number of items in production to send to analytics for experiment " + exp.getName() + " was exceeded. The maximum number allowed is " + experimentAnalyticsQuota + ". Updating the input schema increased the number to " + updatedProdCount;
					brokenRulesAndConfigJSON.put (Constants.JSON_FIELD_PRON_ANALYTICS_ITEMS_QUOTA_EXCEEDED, err);												
					return new ValidationResults(err, Status.BAD_REQUEST);	
				}
			}

		}

		return null;
	}

	public ValidationResults validateInputSchemaJSON(JSONObject inputSchemaObj, ServletContext context, Boolean force) {

		try {

			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			//schema
			if (!inputSchemaObj.containsKey(Constants.JSON_FIELD_INPUT_SCHEMA) || inputSchemaObj.getString(Constants.JSON_FIELD_INPUT_SCHEMA) == null) {
				return new ValidationResults("The inputSchema field is missing.", Status.BAD_REQUEST);
			}

			JSONObject schemaJson = inputSchemaObj.getJSONObject(Constants.JSON_FIELD_INPUT_SCHEMA); //validate legal json

			//seasonId
			if (!inputSchemaObj.containsKey(Constants.JSON_FEATURE_FIELD_SEASON_ID) || inputSchemaObj.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID) == null || inputSchemaObj.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_SEASON_ID), Status.BAD_REQUEST);
			}

			String sId = (String)inputSchemaObj.get(Constants.JSON_FEATURE_FIELD_SEASON_ID);
			Season season = seasonsDB.get(sId);
			if (season == null) {
				return new ValidationResults("The season of the given Input Schema does not exist.", Status.BAD_REQUEST);
			}


			//cannot add input schema - only update
			//modification date must appear
			if (!inputSchemaObj.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) || inputSchemaObj.get(Constants.JSON_FIELD_LAST_MODIFIED)==null) {
				return new ValidationResults("The lastModified field is missing. This field must be specified during Input Schema update.", Status.BAD_REQUEST);
			}

			//verify that given modification date is not older that current modification date
			long givenModoficationDate = inputSchemaObj.getLong(Constants.JSON_FIELD_LAST_MODIFIED);  //verify that legal long
			Date givenDate = new Date(givenModoficationDate);
			if (givenDate.before(lastModified)) {
				return new ValidationResults("The Input Schema was changed by another user. Refresh your browser and try again.", Status.CONFLICT);			
			}		

			//verify that was seasonId not changed
			if (!seasonId.toString().equals(sId)) {
				return new ValidationResults("seasonId cannot be changed during update", Status.BAD_REQUEST);
			}			

			//validate the schema
			String ajv = (String)context.getAttribute(Constants.SCHEMA_VALIDATOR_AJV_PARAM_NAME);
			String validator = (String)context.getAttribute(Constants.SCHEMA_VALIDATOR_VALIDATOR_PARAM_NAME);
			String validateLeaves = (String)context.getAttribute(Constants.SCHEMA_VALIDATE_LEAVES_PARAM_NAME);
			String generator = (String)context.getAttribute(Constants.SCHEMA_JSON_GENERATOR_PARAM_NAME);				
			String prune = (String)context.getAttribute(Constants.SCHEMA_JSON_PRUNE_PARAM_NAME);
			String jsf = (String)context.getAttribute(Constants.SCHEMA_JSON_FAKER_PARAM_NAME);

			try {
				//SchemaValidator.validateSchema(validator, validateLeaves, ajv, schemaJson.toString());
				SchemaValidator.extendedValidation(validator, validateLeaves, ajv,
						generator, jsf, prune, Stage.DEVELOPMENT.toString(), "9999999",
						schemaJson.toString());
			} catch (ValidationException ve) {
				return new ValidationResults("The Input Schema is not valid: " + ve.getMessage(), Status.BAD_REQUEST);
			}			

			JSONObject updatedMargedSchema = null;
			try {
				JSONObject streamsJson = season.getStreams().toJson(OutputJSONMode.ADMIN);
				updatedMargedSchema = mergeSchema (streamsJson, true, schemaJson);
			} catch (GenerationException e) {
				return new ValidationResults("The Input Schema is not valid, fail merging streams to main schema: " + e.getMessage(), Status.BAD_REQUEST);
			}


			//validate that the quota for prod analytics fields is not exceeded (can happen when field in dev that is 
			//reported in analytics is moved to prod)
			ValidationResults vr = validateInputFieldsForAnalytics(season, new JSONObject(), updatedMargedSchema, context);
			if (vr!=null)
				return vr;


			if (!force) {
				//validate that each rule and configuration are not broken with the new input schema
				String err = validateRulesAndConfigWithNewSchemaOrChangedUtility(season, updatedMargedSchema, context, null, null, null, null, null, null, null, null, null);
				if (err!=null)
					return new ValidationResults(err, Status.BAD_REQUEST);
			}

		} catch (JSONException jsne) {
			return new ValidationResults(jsne.getMessage(), Status.BAD_REQUEST);
		}
		catch (ClassCastException cce) {
			return new ValidationResults("Illegal Input Schema JSON: " + cce.getMessage(), Status.BAD_REQUEST);
		}
		catch (GenerationException ge) {
			return new ValidationResults("Failed to generate the data sample: " + ge.getMessage(), Status.BAD_REQUEST);
		}catch (MergeException e) {
			return new ValidationResults(Strings.mergeException  + e.getMessage(), Status.BAD_REQUEST);
		}
		catch (InterruptedException | ExecutionException e) {
			return new ValidationResults("Failed to run validation on branches: " + e.getMessage(), Status.BAD_REQUEST);
		}
		return null;
	}

	//return null on success, errorString on error
	public static String validateRulesAndConfigWithNewSchemaOrChangedUtility (Season season, JSONObject schemaJson, ServletContext context, 
			UUID removedUtilityId, String newUtilityCode, Stage newUtilStage, 
			LinkedList<JSONObject> brokenRules, LinkedList<JSONObject> brokenConfigs,
			LinkedList<JSONObject> brokenExperiments, LinkedList<JSONObject> brokenVariants,
			LinkedList<JSONObject> brokenNotificationRules, LinkedList<JSONObject> brokenNotificationConfigs) throws JSONException, GenerationException, InterruptedException, ExecutionException
	{
		String id = (removedUtilityId==null) ? null: removedUtilityId.toString();
		ValidationCache tester = new ValidationCache(newUtilityCode, id, newUtilStage); // cache with changed utility
		tester.setSchema(schemaJson); // cache with changed schema
		// cause the tester to load context and utilities
		tester.getInfo(context, season, Stage.DEVELOPMENT, season.getMinVersion());
		tester.getInfo(context, season, Stage.PRODUCTION, season.getMinVersion());
		
		String err = doValidateRulesAndConfigWithNewSchema (season.getRoot(), season,  tester, context, brokenRules, brokenConfigs, Constants.MASTER_BRANCH_NAME);
		if (err != null)
			return err;

		err = doValidateRulesAndConfigWithNewSchema (season.getEntitlementsRoot(), season,  tester, context, brokenRules, brokenConfigs, Constants.MASTER_BRANCH_NAME);
		if (err != null)
			return err;

		//validate experiments
		List<Future<String>> expResults = doValidateExperimentsRulesWithNewSchema (season, tester, 
				context, brokenExperiments, brokenVariants);

		//validate merged branches (simulate branch checkout)
		int numBranches = season.getBranches().getBranchesList().size();
		ArrayList<Future<String>> results = new ArrayList<Future<String>>(numBranches);
		ExecutorService executor = (numBranches > 0)? Executors.newFixedThreadPool(32) : Executors.newCachedThreadPool();		
//		ExecutorService executor = Executors.newFixedThreadPool(numBranches);
		for (Branch b:season.getBranches().getBranchesList()) {
			Future<String> result = executor.submit( new Callable<String>() {
				@Override
				public String call() throws JSONException, GenerationException {
					ValidationCache branchTester = tester.clone();
					try {
						BaseAirlockItem branchRoot = MergeBranch.merge(season.getRoot(), b, Constants.REQUEST_ITEM_TYPE.FEATURES);
						String err = doValidateRulesAndConfigWithNewSchema (branchRoot, season, branchTester, 
								context, brokenRules, brokenConfigs, b.getName());
						if (err != null)
							return err;
					} catch (MergeException e) {
						return "validation failed. merging branch "+b.getName()+" caused error: "+e.getMessage();
					}
					return null;
				}            
			} );
			results.add(result);
		}


		err = doValidateNotificationWithNewSchema (season, tester, 
				context, brokenNotificationRules, brokenNotificationConfigs);

		if (err != null)
			return err;

		//wait for all validations to finish execute
		for (Future<String> result : results) {
			String bErr = result.get();
			if (bErr != null)
				return bErr;

		}
		//wait for all experiments to finish calculate		
		for (Future<String> fRes : expResults) {
			String currRes = fRes.get();
			if (currRes != null) {
				return currRes;
			}
		}

		return err;		
	}


	private static String doValidateItemRulesWithNewSchema(BaseStagedAnalyticsItem item, String minVersion, Season season, 
			ValidationCache tester, ServletContext context, 
			LinkedList<JSONObject> brokenExperiments) throws JSONException, GenerationException
	{
		if (item.getRule()==null || item.getRule().getRuleString()==null)
			return null;

		String itemType = (item instanceof Experiment)?"experiment":"variant";
		Stage itemStage = item.getStage();

		try {
			ValidationCache.Info info = tester.getInfo(context, season, itemStage, minVersion);
			VerifyRule.fullRuleEvaluation(item.getRule().getRuleString(), info.minimalInvoker, info.maximalInvoker);
		}
		catch (Exception e)
		{	
			if (brokenExperiments != null) {
				JSONObject brokenExpObj = new JSONObject();
				brokenExpObj.put(Constants.JSON_FIELD_UNIQUE_ID, item.getUniqueId().toString());
				brokenExpObj.put(Constants.JSON_FIELD_NAME, item.getName());
				brokenExperiments.add(brokenExpObj); //accumulate the broken rules
			}
			else {
				return "Rule of " + itemType + " '" + item.getName() + "' is invalid:" + e.getMessage();
			}
		}
		return null;
	}		

	private static List<Future<String>> doValidateExperimentsRulesWithNewSchema(Season season, //JSONObject schemaJson,
			ValidationCache tester, ServletContext context, 
			LinkedList<JSONObject> brokenExperiments, LinkedList<JSONObject> brokenVariants) throws JSONException, GenerationException {
		List<Experiment> seasonExperiments = season.getExperimentsForSeason(context, false);

		List<Future<String>> toRet = new LinkedList<Future<String>>();
		for (Experiment exp:seasonExperiments) {
			Future<String> expResult = executor.submit(new Callable<String>() {
				
				@Override
				public String call() throws Exception {
					ValidationCache expTester = tester.clone();
					String res = doValidateItemRulesWithNewSchema(exp, exp.getMinVersion(), season, /*schemaJson,*/ expTester, context, /*removedUtilityId, newUtilityCode, newUtilStage,*/ brokenExperiments);
					if (res != null)
						return res;
					for (Variant var:exp.getVariants()) {
						ValidationCache varTester = expTester.clone();
						res = doValidateItemRulesWithNewSchema(var, exp.getMinVersion(), season, /*schemaJson,*/ varTester, context, /*removedUtilityId, newUtilityCode, newUtilStage,*/ brokenVariants);
						if (res != null)
							return res;
						String branchName = var.getBranchName();
						// check if branch is not master
						BaseAirlockItem currRoot = season.getRoot();
						if (branchName!=null && !branchName.equals(Constants.MASTER_BRANCH_NAME)) {
							Branch branch = season.getBranches().getBranchByName(branchName);
							try {
								currRoot = MergeBranch.merge(season.getRoot(), branch, Constants.REQUEST_ITEM_TYPE.FEATURES);
							} catch (MergeException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						LinkedList<JSONObject> brokenConfigs = new LinkedList<JSONObject>();
						LinkedList<JSONObject> brokenRules = new LinkedList<JSONObject>();;
						String err = doValidateRulesAndConfigWithNewSchema (currRoot, season, varTester, 
								context, brokenRules , brokenConfigs , branchName);
						if (err != null)
							return err;
					}
					return res;
				}
				
			});
			toRet.add(expResult);
		}
		
		return toRet;
			
	}

	private static String doValidateNotificationWithNewSchema(Season season, 
			ValidationCache tester, ServletContext context, 
			LinkedList<JSONObject> brokenNotificationRules, LinkedList<JSONObject> brokenNotificationConfigs) throws JSONException, GenerationException {

		List<AirlockNotification> seasonNotifications = season.getNotifications().getNotificationsList();
		for (AirlockNotification notification:seasonNotifications) {
			//cancellation rule
			if (notification.getCancellationRule()!=null && notification.getCancellationRule().getRuleString()!=null) {
				try {
					ValidationCache.Info info = tester.getInfo(context, season, notification.getStage(), notification.getMinAppVersion());
					VerifyRule.fullRuleEvaluation(notification.getCancellationRule().getRuleString(), info.minimalInvoker, info.maximalInvoker);
				}
				catch (Exception e)
				{	
					if (brokenNotificationRules != null) {
						JSONObject brokenNotifRuleObj = new JSONObject();
						brokenNotifRuleObj.put(Constants.JSON_FIELD_UNIQUE_ID, notification.getUniqueId().toString());
						brokenNotifRuleObj.put(Constants.JSON_FIELD_NAME, notification.getName());
						brokenNotificationRules.add(brokenNotifRuleObj); //accumulate the broken rules
					}
					else {
						return "Cancellation rule of notification '" + notification.getName() + "' is invalid: " + e.getMessage();
					}
				}								
			}

			//registrationRule
			if (notification.getRegistrationRule()!=null && notification.getRegistrationRule().getRuleString()!=null) {
				try {
					ValidationCache.Info info = tester.getInfo(context, season, notification.getStage(), notification.getMinAppVersion());
					VerifyRule.fullRuleEvaluation(notification.getRegistrationRule().getRuleString(), info.minimalInvoker, info.maximalInvoker);
				}
				catch (Exception e)
				{	
					if (brokenNotificationRules != null) {
						JSONObject brokenNotifRuleObj = new JSONObject();
						brokenNotifRuleObj.put(Constants.JSON_FIELD_UNIQUE_ID, notification.getUniqueId().toString());
						brokenNotifRuleObj.put(Constants.JSON_FIELD_NAME, notification.getName());
						brokenNotificationRules.add(brokenNotifRuleObj); //accumulate the broken rules
					}
					else {
						return "Registration rule of notification '" + notification.getName() + "' is invalid: " + e.getMessage();
					}
				}								
			}

			//configuration
			String ruleString = "";
			if (notification.getRegistrationRule()!=null && notification.getRegistrationRule().getRuleString()!=null) { 
				ruleString = notification.getRegistrationRule().getRuleString();
			}

			try {
				ValidationCache.Info info = tester.getInfo(context, season, notification.getStage(), notification.getMinAppVersion());
				VerifyRule.fullConfigurationEvaluation(ruleString, notification.getConfiguration(), info.minimalInvoker, info.maximalInvoker);
			}
			catch (Exception e)
			{
				if (brokenNotificationConfigs!= null) {
					JSONObject brokenConfObj = new JSONObject();
					brokenConfObj.put(Constants.JSON_FIELD_UNIQUE_ID, notification.getUniqueId().toString());					
					brokenConfObj.put(Constants.JSON_FIELD_NAME, notification.getName());
					brokenNotificationConfigs.add(brokenConfObj); //accumulate the broken rules
				}
				else {
					return "The configuration of notification '" + notification.getName() + "' is invalid due to unverified optional fields: " + e.getMessage();
				}							
			}

		}

		return null;
	}

	private static String doValidateSingleItemRuleAndConfigWithNewSchema(BaseAirlockItem item, Season season, //JSONObject schemaJson,
			ValidationCache tester,ServletContext context, 
			LinkedList<JSONObject> brokenRules, LinkedList<JSONObject> brokenConfigs, String branchName) throws JSONException, GenerationException {
		if (item.getType().equals(Type.FEATURE)) {
			FeatureItem featureItem = (FeatureItem)item;
			//if rule exist and the feature is in higher minAppVersion than the utility and in the matching stage
			if (featureItem.getRule()!=null && featureItem.getRule().getRuleString()!=null) {
				Stage featureStage = featureItem.getStage();
				String featureMinAppVer = featureItem.getMinAppVersion();		

				try {
					ValidationCache.Info info = tester.getInfo(context, season, featureStage, featureMinAppVer);
					VerifyRule.fullRuleEvaluation(featureItem.getRule().getRuleString(), info.minimalInvoker, info.maximalInvoker);
				}
				catch (Exception e)
				{	
					if (brokenRules != null) {
						JSONObject brokenRuleObj = new JSONObject();
						brokenRuleObj.put(Constants.JSON_FIELD_UNIQUE_ID, featureItem.getUniqueId().toString());
						brokenRuleObj.put(Constants.JSON_FIELD_NAME, featureItem.getNameSpaceDotName());
						brokenRuleObj.put(Constants.JSON_FIELD_BRANCH_NAME, branchName);
						brokenRules.add(brokenRuleObj); //accumulate the broken rules
					}
					else {
						return "Rule of feature '" + featureItem.getNameSpaceDotName() + "' in branch '"+branchName+"' is invalid:" + e.getMessage();
					}
				}
			}
			if (featureItem.getPremiumRule()!=null && featureItem.getPremiumRule().getRuleString()!=null) {
				Stage featureStage = featureItem.getStage();
				String featureMinAppVer = featureItem.getMinAppVersion();		

				try {
					ValidationCache.Info info = tester.getInfo(context, season, featureStage, featureMinAppVer);
					VerifyRule.fullRuleEvaluation(featureItem.getPremiumRule().getRuleString(), info.minimalInvoker, info.maximalInvoker);
				}
				catch (Exception e)
				{	
					if (brokenRules != null) {
						JSONObject brokenRuleObj = new JSONObject();
						brokenRuleObj.put(Constants.JSON_FIELD_UNIQUE_ID, featureItem.getUniqueId().toString());
						brokenRuleObj.put(Constants.JSON_FIELD_NAME, featureItem.getNameSpaceDotName());
						brokenRuleObj.put(Constants.JSON_FIELD_BRANCH_NAME, branchName);
						brokenRules.add(brokenRuleObj); //accumulate the broken rules
					}
					else {
						return "Premium rule of feature '" + featureItem.getNameSpaceDotName() + "' in branch '"+branchName+"' is invalid:" + e.getMessage();
					}
				}
			}
		}
		else if(item.getType().equals(Type.CONFIGURATION_RULE) || item.getType().equals(Type.ORDERING_RULE))
		{
			ConfigurationRuleItem crItem = (ConfigurationRuleItem)item;
			Stage crStage = crItem.getStage();
			String crMinAppVer = crItem.getMinAppVersion();

			String ruleString = "";
			if (crItem.getRule()!=null && crItem.getRule().getRuleString()!=null) { 
				ruleString = crItem.getRule().getRuleString();
			}

			try {
				ValidationCache.Info info = tester.getInfo(context, season, crStage, crMinAppVer);
				String configStr = item.getType().equals(Type.CONFIGURATION_RULE) ? crItem.getConfiguration() : ((OrderingRuleItem)crItem).getConfigurationStringForEval();
				VerifyRule.fullConfigurationEvaluation(ruleString, configStr, info.minimalInvoker, info.maximalInvoker);
			}
			catch (Exception e)
			{
				if (brokenRules != null) {
					JSONObject brokenConfObj = new JSONObject();
					brokenConfObj.put(Constants.JSON_FIELD_UNIQUE_ID, crItem.getUniqueId().toString());
					brokenConfObj.put(Constants.JSON_FIELD_BRANCH_NAME, branchName);
					brokenConfObj.put(Constants.JSON_FIELD_NAME, crItem.getNameSpaceDotName());
					brokenConfigs.add(brokenConfObj); //accumulate the broken rules
				}
				else {
					return "The configuration '" + crItem.getNameSpaceDotName() + "' in branch '" + branchName + "' is invalid: " + e.getMessage();
				}							
			}

			if (!ruleString.isEmpty())
			{
				try {
					ValidationCache.Info info = tester.getInfo(context, season, crStage, crMinAppVer);
					VerifyRule.fullRuleEvaluation(ruleString, info.minimalInvoker, info.maximalInvoker);
				}
				catch (Exception e) {
					if (brokenRules != null) {
						JSONObject brokenRuleObj = new JSONObject();
						brokenRuleObj.put(Constants.JSON_FIELD_UNIQUE_ID, crItem.getUniqueId().toString());
						brokenRuleObj.put(Constants.JSON_FIELD_BRANCH_NAME, branchName);
						brokenRuleObj.put(Constants.JSON_FIELD_NAME, crItem.getNameSpaceDotName());
						brokenRules.add(brokenRuleObj); //accumulate the broken rules
					}
					else {
						return "Rule of configuration '" + crItem.getNameSpaceDotName() + "' in branch '" + branchName + "' is invalid: " + e.getMessage();
					}							
				}
			}
		}
		else if (item.getType().equals(Type.ENTITLEMENT) || item.getType().equals(Type.PURCHASE_OPTIONS)) {
			FeatureItem purchaseItem = (FeatureItem)item;
			//if rule exist and the purchase item is in higher minAppVersion than the utility and in the matching stage
			if (purchaseItem.getRule()!=null && purchaseItem.getRule().getRuleString()!=null) {
				Stage featureStage = purchaseItem.getStage();
				String featureMinAppVer = purchaseItem.getMinAppVersion();		

				try {
					ValidationCache.Info info = tester.getInfo(context, season, featureStage, featureMinAppVer);
					VerifyRule.fullRuleEvaluation(purchaseItem.getRule().getRuleString(), info.minimalInvoker, info.maximalInvoker);
				}
				catch (Exception e)
				{	
					if (brokenRules != null) {
						JSONObject brokenRuleObj = new JSONObject();
						brokenRuleObj.put(Constants.JSON_FIELD_UNIQUE_ID, purchaseItem.getUniqueId().toString());
						brokenRuleObj.put(Constants.JSON_FIELD_NAME, purchaseItem.getNameSpaceDotName());
						brokenRuleObj.put(Constants.JSON_FIELD_BRANCH_NAME, branchName);
						brokenRules.add(brokenRuleObj); //accumulate the broken rules
					}
					else {
						return "Rule of purchase item " + purchaseItem.getType().toString() + " '" + purchaseItem.getNameSpaceDotName() + "' in branch '"+branchName+"' is invalid:" + e.getMessage();
					}
				}
			}
		}


		return null;
	}

	private static String doValidateRulesAndConfigWithNewSchema(BaseAirlockItem root, Season season, //JSONObject schemaJson,
			ValidationCache tester, ServletContext context,
			LinkedList<JSONObject> brokenRules, LinkedList<JSONObject> brokenConfigs, String branchName) throws JSONException, GenerationException {

		if  (root.getType() == Type.FEATURE || root.getType() == Type.CONFIGURATION_RULE || root.getType() == Type.ORDERING_RULE
				|| root.getType() == Type.ENTITLEMENT || root.getType() == Type.PURCHASE_OPTIONS) {		
			String res =doValidateSingleItemRuleAndConfigWithNewSchema(root, season, tester, context, brokenRules, brokenConfigs, branchName);
			if (res!=null) {
				return res;
			}
		}

		if (root.getFeaturesItems() != null) {
			for (int i=0; i<root.getFeaturesItems().size(); i++) {
				String res = doValidateRulesAndConfigWithNewSchema(root.getFeaturesItems().get(i), season, tester, context, brokenRules, brokenConfigs, branchName);
				if (res!=null)
					return res;
			}
		}

		if (root.getConfigurationRuleItems() != null) {
			for (int i=0; i<root.getConfigurationRuleItems().size(); i++) {
				String res = doValidateRulesAndConfigWithNewSchema(root.getConfigurationRuleItems().get(i), season, tester, context, brokenRules, brokenConfigs, branchName);
				if (res!=null)
					return res;
			}
		}	

		if (root.getOrderingRuleItems() != null) {
			for (int i=0; i<root.getOrderingRuleItems().size(); i++) {
				String res = doValidateRulesAndConfigWithNewSchema(root.getOrderingRuleItems().get(i), season, tester, context, brokenRules, brokenConfigs, branchName);
				if (res!=null)
					return res;
			}
		}	

		if (root.getEntitlementItems() != null) {
			for (int i=0; i<root.getEntitlementItems().size(); i++) {
				String res = doValidateRulesAndConfigWithNewSchema(root.getEntitlementItems().get(i), season, tester, context, brokenRules, brokenConfigs, branchName);
				if (res!=null)
					return res;
			}
		}	

		if (root.getPurchaseOptionsItems() != null) {
			for (int i=0; i<root.getPurchaseOptionsItems().size(); i++) {
				String res = doValidateRulesAndConfigWithNewSchema(root.getPurchaseOptionsItems().get(i), season, tester, context, brokenRules, brokenConfigs, branchName);
				if (res!=null)
					return res;
			}
		}	

		
		return null;
	}

	public String updateInputSchema(JSONObject updatedInputSchemaJSON, ServletContext context, Season season) throws JSONException {
		boolean wasChanged = false;
		StringBuilder updateDetails = new StringBuilder();

		//seasonId should not be updated

		//inutSchema
		JSONObject updatedSchema = updatedInputSchemaJSON.getJSONObject(Constants.JSON_FIELD_INPUT_SCHEMA);
		if (!Utilities.jsonObjsAreEqual(getMergedSchema(), updatedSchema)) {
			updateDetails.append("'inputSchema' changed from " + getMergedSchema().toString() + " to " + updatedSchema.toString() + ";	");
			schema  = updatedSchema;
			try {
				JSONObject streamsJson = season.getStreams().toJson(OutputJSONMode.ADMIN);
				mergeSchema (streamsJson, false, null);
			} catch (GenerationException e) {
				//should not happen - after validation
			}
			wasChanged = true; 
		}		

		if (wasChanged) {
			@SuppressWarnings("unchecked")
			Map<String, BaseAirlockItem> airlockItemsDB = (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);

			season.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection().calcNumberOfItemsToAnalytics(context, season.getUniqueId(), airlockItemsDB);
			lastModified = new Date();
		}

		if (updateDetails.length()!=0) {
			updateDetails.insert(0,"Input schema changes: ");
		}
		return updateDetails.toString();
	}

	//remove input fields from analytics if were removed from schema during update
	//return null if analytics didnt changed
	//DEVELOPM if only fields in develop were removed
	//PRODUCTION if at least one field in production was removed
	private Stage doUpdateAnalytics (AnalyticsDataCollection analyticsDataCollection, Season season, ServletContext context, Map<String, String> newInputFieldsMap) throws JSONException  { 
		Stage ret = null;
		LinkedList<String> inputFieldsForAnalyticsList = analyticsDataCollection.getInputFieldsForAnalytics();
		Map<String, Stage> inputFieldsForAnalyticsStageMap = analyticsDataCollection.getInputFieldsForAnalyticsStageMap();
		if (inputFieldsForAnalyticsList!=null && inputFieldsForAnalyticsList.size()>0) {			
			try {
				if (newInputFieldsMap == null)
					newInputFieldsMap = Utilities.getStageForInputFields(context, getMergedSchema());

				Iterator<String> inputFieldsIterator = inputFieldsForAnalyticsList.iterator();
				while (inputFieldsIterator.hasNext()) {
					String inputField = inputFieldsIterator.next();

					//validate that existing analytics inputFields are not removed					
					if (!newInputFieldsMap.containsKey(inputField)) {
						Stage remInputFieldsStage = inputFieldsForAnalyticsStageMap.get(inputField);
						if (remInputFieldsStage.equals(Stage.PRODUCTION)) {
							ret = Stage.PRODUCTION;
						}
						else { //DEVELOPMENT
							if (ret == null)
								ret = Stage.DEVELOPMENT; 
						}
						//inputFieldsForAnalyticsList.remove(inputField);
						inputFieldsIterator.remove();
						inputFieldsForAnalyticsStageMap.remove(inputField);
					}
					else {
						//check if stage changed
						if (!newInputFieldsMap.get(inputField).equals(inputFieldsForAnalyticsStageMap.get(inputField).toString())) {
							ret = Stage.PRODUCTION;
							inputFieldsForAnalyticsStageMap.put(inputField, Stage.valueOf(newInputFieldsMap.get(inputField)));
						}
					}
				}
				/*
				for (int k=0; k<inputFieldsForAnalyticsList.size(); k++) {
					String inputField = inputFieldsForAnalyticsList.get(k);
					if (!newInputFieldsMap.containsKey(inputField)) {
						Stage remInputFieldsStage = inputFieldsForAnalyticsStageMap.get(inputField);
						if (remInputFieldsStage.equals(Stage.PRODUCTION)) {
							ret = Stage.PRODUCTION;
						}
						else { //DEVELOPMENT
							if (ret == null)
								ret = Stage.DEVELOPMENT; 
						}
						inputFieldsForAnalyticsList.remove(inputField);
						inputFieldsForAnalyticsStageMap.remove(inputField);
					}
					else {
						//check if stage changed
						if (!newInputFieldsMap.get(inputField).equals(inputFieldsForAnalyticsStageMap.get(inputField))) {
							ret = Stage.PRODUCTION;
							inputFieldsForAnalyticsStageMap.put(inputField, Stage.valueOf(newInputFieldsMap.get(inputField)));
						}
					}
				}*/
			} catch (GenerationException e) {
				throw new JSONException("GenerationException: " + e.getMessage());
			}
		}

		return ret;
	}

	//remove input fields from analytics if were removed from schema during update
	//return null if analytics didnt changed
	//DEVELOPM if only fields in develop were removed
	//PRODUCTION if at least one field in production was removed	
	//the updatedBranches list is the list of the branches that were updated including master
	public Stage updateAnalytics (Season season, ServletContext context, LinkedList<String> updatedBranches) throws JSONException {
		Stage maxStage = null;
		Map<String, String> newInputFieldsMap = null;

		Stage ret = doUpdateAnalytics(season.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection(), season, context, newInputFieldsMap);
		if (ret!=null) {
			if (ret.equals(Stage.PRODUCTION)) {
				maxStage = Stage.PRODUCTION;
			} 
			else { 
				maxStage = maxStage==null ? Stage.DEVELOPMENT : maxStage;
			}
			updatedBranches.add(Constants.MASTER_BRANCH_NAME);
		}


		for (Branch branch:season.getBranches().getBranchesList()) {
			ret = doUpdateAnalytics(branch.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection(), season, context, newInputFieldsMap);
			if (ret!=null) {
				if (ret.equals(Stage.PRODUCTION)) {
					maxStage = Stage.PRODUCTION;
				} 
				else { 
					maxStage = maxStage==null ? Stage.DEVELOPMENT : maxStage;
				}
				updatedBranches.add(branch.getUniqueId().toString());
			}	
		}

		return maxStage;
	}

	public InputSchema duplicateForNewSeason (UUID newSeasonId) {
		InputSchema res = new InputSchema();
		res.setSchema(schema);
		res.setMergedSchema(mergedSchema);
		res.setLastModified(lastModified);
		res.setSeasonId(newSeasonId);

		return res;
	}

	public JSONObject generateInputSample(Stage stage, String minAppVersion, ServletContext context, InputSampleGenerationMode generationMode) throws GenerationException, JSONException {
		return generateInputSample(stage, minAppVersion, context, generationMode, Constants.DEFAULT_RANDOMIZER);
	}

	public JSONObject generateInputSample(Stage stage, String minAppVersion, ServletContext context, InputSampleGenerationMode generationMode, double randomizer) throws GenerationException, JSONException {		

		String faker = (String)context.getAttribute(Constants.SCHEMA_JSON_FAKER_PARAM_NAME);
		String generator = (String)context.getAttribute(Constants.SCHEMA_JSON_GENERATOR_PARAM_NAME);				
		String prune = (String)context.getAttribute(Constants.SCHEMA_JSON_PRUNE_PARAM_NAME);

		String inputSampleStr = JsonGenerator.generation(generator, faker, prune, getMergedSchema().toString(), stage.toString(), minAppVersion, generationMode, randomizer); 				

		JSONObject inputSampleJson = new JSONObject(inputSampleStr);

		return inputSampleJson;
	}

	public static JSONObject generateInputSampleFromJSON(Stage stage, String minAppVersion, ServletContext context, JSONObject updatedInputSchema, InputSampleGenerationMode generationMode) throws GenerationException, JSONException {		

		String faker = (String)context.getAttribute(Constants.SCHEMA_JSON_FAKER_PARAM_NAME);
		String generator = (String)context.getAttribute(Constants.SCHEMA_JSON_GENERATOR_PARAM_NAME);				
		String prune = (String)context.getAttribute(Constants.SCHEMA_JSON_PRUNE_PARAM_NAME);

		String inputSampleStr = JsonGenerator.generation(generator, faker, prune, updatedInputSchema.toString(), stage.toString(), minAppVersion, generationMode, -1); 				

		JSONObject inputSampleJson = new JSONObject(inputSampleStr);

		return inputSampleJson;
	}


	public JSONObject mergeSchema(JSONObject streamsJson, boolean validationOnly, JSONObject newSchema) throws GenerationException
	{
		try {

			if (validationOnly) 
				streamsJson = (JSONObject)Utilities.cloneJson(streamsJson, true);

			JSONObject tmpSchema = (JSONObject)Utilities.cloneJson(newSchema == null ? schema : newSchema, true);		 				
			AddStreamsToSchema.merge(tmpSchema, streamsJson, false); 

			if (!validationOnly)
				mergedSchema = tmpSchema;

			return tmpSchema;
		}
		catch (Exception e) {
			throw new GenerationException("fail merge streams to main schema: " + e.getMessage());
		}		
	}

}
