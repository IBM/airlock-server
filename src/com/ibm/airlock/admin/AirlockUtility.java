package com.ibm.airlock.admin;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response.Status;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Strings;
import com.ibm.airlock.Constants.Action;
import com.ibm.airlock.Constants.InputSampleGenerationMode;
import com.ibm.airlock.Constants.RoleType;
import com.ibm.airlock.Constants.Stage;
import com.ibm.airlock.Constants.UtilityType;
import com.ibm.airlock.admin.authentication.UserInfo;
import com.ibm.airlock.engine.Environment;
import com.ibm.airlock.engine.StreamsScriptInvoker;
import com.ibm.airlock.engine.VerifyRule;
import com.ibm.airlock.engine.VerifyStream;

public class AirlockUtility {
	private String utility = null; //required in create and update
	private Stage stage = null; //required in create and update
	private UUID uniqueId = null;//required update. forbidden in create
	private Date lastModified = null; // required in update. forbidden in create
	private UUID seasonId = null; //required in create and update
	private UtilityType type = UtilityType.MAIN_UTILITY; //optional
	protected String name = null; //c+u 
	
	public String getUtility() {
		return utility;
	}

	public void setUtility(String utility) {
		this.utility = utility;
	}

	public Stage getStage() {
		return stage;
	}

	public void setStage(Stage stage) {
		this.stage = stage;
	}

	public UUID getUniqueId() {
		return uniqueId;
	}

	public void setUniqueId(UUID uniqueId) {
		this.uniqueId = uniqueId;
	}

	public Date getLastModified() {
		return lastModified;
	}

	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

	public UtilityType getType() {
		return type;
	}

	public void setType(UtilityType type) {
		this.type = type;
	}

	public UUID getSeasonId() {
		return seasonId;
	}

	public void setSeasonId(UUID seasonId) {
		this.seasonId = seasonId;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public JSONObject toJson() throws JSONException {
		JSONObject res = new JSONObject();
		res.put(Constants.JSON_FIELD_UNIQUE_ID, uniqueId==null?null:uniqueId.toString());
		res.put(Constants.JSON_FIELD_UTILITY, utility);				
		res.put(Constants.JSON_FIELD_LAST_MODIFIED, lastModified.getTime()); //last modified is the time-stamp 	
		//res.put(Constants.JSON_FEATURE_FIELD_MIN_APP_VER, minAppVersion);
		res.put(Constants.JSON_FEATURE_FIELD_STAGE, stage.toString());
		res.put(Constants.JSON_FEATURE_FIELD_TYPE, type.toString());
		res.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, seasonId==null?null:seasonId.toString());
		res.put(Constants.JSON_FIELD_NAME, name);
		
		return res;
	}

	//at this stage we are after validate so all mandatory fields are in and the JSON format is correct.
	public void fromJSON(JSONObject input) throws JSONException {
		utility = input.getString(Constants.JSON_FIELD_UTILITY);

		if (input.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && input.get(Constants.JSON_FIELD_UNIQUE_ID) != null) {
			String sStr = input.getString(Constants.JSON_FIELD_UNIQUE_ID);			
			uniqueId = UUID.fromString(sStr);		
		}

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

		if (input.containsKey(Constants.JSON_FEATURE_FIELD_STAGE) && input.get(Constants.JSON_FEATURE_FIELD_STAGE)!=null)
			stage = Utilities.strToStage(input.getString(Constants.JSON_FEATURE_FIELD_STAGE));		

		if (input.containsKey(Constants.JSON_FEATURE_FIELD_TYPE) && input.get(Constants.JSON_FEATURE_FIELD_TYPE)!=null)
			type = Utilities.strToUtilityType(input.getString(Constants.JSON_FEATURE_FIELD_TYPE));

		if (input.containsKey(Constants.JSON_FIELD_NAME) && input.get(Constants.JSON_FIELD_NAME)!=null) 
			name = input.getString(Constants.JSON_FIELD_NAME);

	}


	public ValidationResults validateUtilityJSON(JSONObject utilityObj, ServletContext context, UserInfo userInfo, Boolean force) {

		@SuppressWarnings("unchecked")
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

		try {
			//seasonId
			if (!utilityObj.containsKey(Constants.JSON_FEATURE_FIELD_SEASON_ID) || utilityObj.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID) == null || utilityObj.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_SEASON_ID), Status.BAD_REQUEST);
			}

			String sId = (String)utilityObj.get(Constants.JSON_FEATURE_FIELD_SEASON_ID);
			Season season = seasonsDB.get(sId);
			if (season == null) {
				return new ValidationResults("The season of the given utility does not exist.", Status.BAD_REQUEST);
			}

			//utility
			if (!utilityObj.containsKey(Constants.JSON_FIELD_UTILITY) || utilityObj.getString(Constants.JSON_FIELD_UTILITY) == null || utilityObj.getString(Constants.JSON_FIELD_UTILITY).isEmpty()) {
				return new ValidationResults("The utility field is missing.", Status.BAD_REQUEST);
			}

			String utilStr = utilityObj.getString(Constants.JSON_FIELD_UTILITY);

			//stage
			if (!utilityObj.containsKey(Constants.JSON_FEATURE_FIELD_STAGE) || utilityObj.getString(Constants.JSON_FEATURE_FIELD_STAGE) == null) {
				return new ValidationResults("The stage field is missing.", Status.BAD_REQUEST);					
			}

			String stageStr = utilityObj.getString(Constants.JSON_FEATURE_FIELD_STAGE);
			Stage stageObj = Utilities.strToStage(stageStr);
			if (stageObj == null) {
				return new ValidationResults("Illegal utility stage: '" + stageStr + "'", Status.BAD_REQUEST);
			}				


			//type - optional, validate if exists
			UtilityType typeObj = null;
			if (utilityObj.containsKey(Constants.JSON_FEATURE_FIELD_TYPE) && utilityObj.get(Constants.JSON_FEATURE_FIELD_TYPE) != null) {
				String typeStr = utilityObj.getString(Constants.JSON_FEATURE_FIELD_TYPE);
				typeObj = Utilities.strToUtilityType(typeStr);
				if (typeObj == null) {
					return new ValidationResults("Illegal utility type: '" + typeStr + "'", Status.BAD_REQUEST);
				}
			}

			//validate that added/updated utility didn't break the javascript context. Run it with "true" rule
			
			if (typeObj.equals(UtilityType.MAIN_UTILITY)) {
				String newJavascriptFuncs = season.getUtilities().generateUtilityCodeSectionForStageAndType(stageObj, utilStr, null, null, UtilityType.MAIN_UTILITY);
				try {
					Environment env = new Environment();
					env.setServerVersion(season.getServerVersion());

					VerifyRule.checkValidity("true", "{context:{}}", newJavascriptFuncs, "{}", false, null, stageObj.toString(), env);
				} catch (ValidationException e) {
					return new ValidationResults("Illegal JavaScript: " + e.getMessage(), Status.BAD_REQUEST);
				}	
			}
			else { 
				ValidationCache validationCache = new ValidationCache(utilStr, null, stageObj); // cache with changed utility
				validationCache.setStreamsCache();
				JSONArray allEvents = season.getStreamsEvents().getEvents();
				StreamsScriptInvoker streamsScriptInvoker = null;			
				try {
					ValidationCache.Info info = validationCache.getInfo(context, season, stageObj, "");
					streamsScriptInvoker = (StreamsScriptInvoker)info.streamsInvoker;
					VerifyStream.validateStreamFilter(streamsScriptInvoker, "true", allEvents);
				} catch (Exception e) {
					return new ValidationResults("Illegal filter: " + e.getMessage(), Status.BAD_REQUEST);
				}
			}
			

			//name
			if (!utilityObj.containsKey(Constants.JSON_FIELD_NAME) || utilityObj.getString(Constants.JSON_FIELD_NAME) == null || utilityObj.getString(Constants.JSON_FIELD_NAME).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_NAME), Status.BAD_REQUEST);
			}

			String nameStr = utilityObj.getString(Constants.JSON_FIELD_NAME);
			String validateNameErr = Utilities.validateName(nameStr);
			if(validateNameErr!=null) {
				return new ValidationResults(validateNameErr, Status.BAD_REQUEST);
			}
			
			ValidationResults res = validateNameUniquness(nameStr, uniqueId, season);
			if (res!=null)
				return res;
			
			
			Action action = Action.ADD;
			if (utilityObj.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && utilityObj.get(Constants.JSON_FIELD_UNIQUE_ID)!=null) {
				//if JSON contains uniqueId - update an existing feature otherwise create a new feature
				action = Action.UPDATE;
			}

			if (action == Action.ADD) {		
												
				
				//modification date => should not appear in creation
				if (utilityObj.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) && utilityObj.get(Constants.JSON_FIELD_LAST_MODIFIED)!=null) {
					return new ValidationResults("The lastModified field cannot be specified during InputSchema creation.", Status.BAD_REQUEST);						
				}

				//utility in production can be added only by Administrator or ProductLead
				if (stageObj.equals(Stage.PRODUCTION) && !validRole(userInfo)) {
					return new ValidationResults("Unable to add the utility. Only a user with the Administrator or Product Lead role can add a utility in the production stage.", Status.UNAUTHORIZED);					
				}
			}
			else { //UPDATE
				//modification date must appear
				if (!utilityObj.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) || utilityObj.get(Constants.JSON_FIELD_LAST_MODIFIED)==null) {
					return new ValidationResults("The lastModified field is missing. This field must be specified during utility update.", Status.BAD_REQUEST);
				}

				//verify that given modification date is not older that current modification date
				long givenModoficationDate = utilityObj.getLong(Constants.JSON_FIELD_LAST_MODIFIED);  //verify that legal long
				Date givenDate = new Date(givenModoficationDate);
				if (givenDate.before(lastModified)) {
					return new ValidationResults("The utility was changed by another user. Refresh your browser and try again.", Status.CONFLICT);			
				}

				//verify that was seasonId not changed
				if (!seasonId.toString().equals(sId)) {
					return new ValidationResults(String.format(Strings.fieldCannotBeChangedDuringUpdate, Constants.JSON_FEATURE_FIELD_SEASON_ID), Status.BAD_REQUEST);
				}

				//if utility is in production or is updated from stage DEVELOPMENT to PRODUCTION
				if (stage.equals(Stage.PRODUCTION) || stageObj.equals(Stage.PRODUCTION)) {						
					//only productLead or Administrator can update utility in production
					if (!validRole(userInfo)) {
						return new ValidationResults("Unable to update the utility. Only a user with the Administrator or Product Lead role can update a utility in the production stage.", Status.UNAUTHORIZED);
					}
				}				
				
				//type - cannot be changed during update
				if (utilityObj.containsKey(Constants.JSON_FEATURE_FIELD_TYPE) && utilityObj.get(Constants.JSON_FEATURE_FIELD_TYPE) != null) {

					if (!typeObj.equals(type)) {
						return new ValidationResults(String.format(Strings.fieldCannotBeChangedDuringUpdate, Constants.JSON_FEATURE_FIELD_TYPE), Status.BAD_REQUEST);
					}
				}

				if (force) //don't make any rule/config validations
					return null;

				if (typeObj.equals(UtilityType.MAIN_UTILITY)) {
					//go over the season's features and configRules and validate that no rule or configuration uses the missing functions
					try {
						String verFuncRes = InputSchema.validateRulesAndConfigWithNewSchemaOrChangedUtility (season, season.getInputSchema().getMergedSchema(), context, uniqueId, utilStr, stageObj, null, null, null, null, null, null);
						if (verFuncRes != null)
							return new ValidationResults ("Unable to update utility: " + verFuncRes, Status.BAD_REQUEST);
	
					} catch (GenerationException ge) {
						return new ValidationResults ("Failed to generate the data sample: " + ge.getMessage(), Status.BAD_REQUEST);
					} catch (JSONException jsne) {
						return new ValidationResults (jsne.getMessage(), Status.BAD_REQUEST);
					} catch (InterruptedException ie) {
						return new ValidationResults(ie.getMessage(), Status.INTERNAL_SERVER_ERROR);
					} catch (ExecutionException ee) {
						return new ValidationResults(ee.getMessage(), Status.INTERNAL_SERVER_ERROR);
					}
				}
				else {
					//go over the season's streams and validate that no stream is using the missing functions
					String verStreamsRes =  season.getStreams().validateStreamsWithChangedOrDeletedUtility(context, season, uniqueId.toString(), utilStr, stageObj);
					if (verStreamsRes != null)
						return new ValidationResults ("Unable to update utility: " + verStreamsRes, Status.BAD_REQUEST);
				}
			}								

		} catch (JSONException jsne) {
			return new ValidationResults(jsne.getMessage(), Status.BAD_REQUEST);
		}
		catch (ClassCastException cce) {
			return new ValidationResults("Illegal utility JSON: " + cce.getMessage(), Status.BAD_REQUEST);
		}
		return null;
	}
	
	private static ValidationResults validateNameUniquness(String nameStr, UUID utilId, Season season) {
		for (AirlockUtility utility:season.getUtilities().getUtilitiesList()) {
			if (utility.getName()!=null && utility.getName().equals(nameStr)) { //name is equal to one of the utilities name 
				if (utilId==null || !utilId.equals(utility.getUniqueId())) { //in update - if is the current utility - skip
					return new ValidationResults("An utility with the specified name already exists.", Status.BAD_REQUEST);
				}
			}
		}
		
		return null;
	}

	static boolean validRole(UserInfo userInfo)
	{
		return userInfo == null || userInfo.getRoles().contains(RoleType.Administrator) || userInfo.getRoles().contains(RoleType.ProductLead);
	}
	public String updateUtility(JSONObject updatedUtilityJSON, Season season) throws JSONException {
		boolean wasChanged = false;
		StringBuilder updateDetails = new StringBuilder();

		//seasonId should not be updated

		//utility
		String updatedUtility = updatedUtilityJSON.getString(Constants.JSON_FIELD_UTILITY);
		if (!updatedUtility.equals(utility)) {
			updateDetails.append("'utility' changed from " + utility + " to " + updatedUtility + ";	");
			utility = updatedUtility;
			wasChanged = true;
		}		

		//name
		String updatedName = updatedUtilityJSON.getString(Constants.JSON_FIELD_NAME);
		if (name==null || !updatedName.equals(name)) {
			updateDetails.append("'name' changed from " + name + " to " + updatedName + "\n");
			name = updatedName;
			wasChanged = true;
		}		
		
		//stage
		Stage updatedStage = Utilities.strToStage(updatedUtilityJSON.getString(Constants.JSON_FEATURE_FIELD_STAGE));
		if (updatedStage != stage) {
			updateDetails.append("'stage' changed from " + stage + " to " + updatedStage + ";	");
			stage = updatedStage;
			wasChanged = true;
		}

		if (wasChanged) {
			lastModified = new Date();
		}

		if (updateDetails.length()!=0) {
			updateDetails.insert(0,"Utility changes: ");
		}
		return updateDetails.toString();


	}


	public static String generateUtilityName(Season season) {
		String newName = "";
		boolean found = false;
		int i=1;
		while (!found) {
			String testName = "utility" + i;
			i++;
			if (validateNameUniquness(testName, null, season)==null) {
				found = true;
				newName = testName;
			}
		}
		
		return newName;		
	}

	public AirlockUtility duplicateForNewSeason (String minVersion, UUID newSeasonId) {
		AirlockUtility res = new AirlockUtility();
		res.setUniqueId(UUID.randomUUID());
		//res.setMinAppVersion(minAppVersion); 
		res.setUtility(utility);
		res.setSeasonId(newSeasonId);
		res.setLastModified(lastModified);
		res.setStage(stage);
		res.setType(type);
		res.setName(name);

		return res;
	}

	//if status is 200 (OK) - the string is a warning that should be added to the output JSON	
	public ValidationResults simulateUtilityForRule(String newUtility, String ruleString, ServletContext context, Season season, Stage stage, String minAppVersion) {				
		//validate that added/updated utility didn't break the javascript context. Run it with "true" rule
		String newJavascriptFuncs = season.getUtilities().generateUtilityCodeSectionForStageAndType(stage, newUtility, null, null, UtilityType.MAIN_UTILITY);

		Environment env = new Environment();
		env.setServerVersion(season.getServerVersion());

		//verify javascript correctness 
		try {
			VerifyRule.checkValidity("true", "{context:{}}", newJavascriptFuncs, "{}", false, minAppVersion, stage.toString(), env);
		} catch (ValidationException e) {
			return new ValidationResults("Illegal JavaScript: " + e.getMessage(), Status.BAD_REQUEST);
		}	

		//check for duplicate functions and add warning
		String duplicateFuntions = null;
		duplicateFuntions = findDuplicateFunctions (newUtility, newJavascriptFuncs);

		//validate rule with new utility
		JSONObject maxInputSample = null;
		JSONObject minInputSample = null;
		try {
			maxInputSample = season.getInputSchema().generateInputSample(stage, minAppVersion, context, InputSampleGenerationMode.MAXIMAL);
			minInputSample = season.getInputSchema().generateInputSample(stage, minAppVersion, context, InputSampleGenerationMode.MINIMAL);							
		} catch (GenerationException ge) {
			return new ValidationResults("Failed to generate data sample: " + ge.getMessage(), Status.BAD_REQUEST);		
		} catch (JSONException e) {
			return new ValidationResults("Failed to generate data sample: " + e.getMessage(), Status.BAD_REQUEST);	
		}

		try {
			VerifyRule.checkValidity(ruleString, maxInputSample.toString(), newJavascriptFuncs, "{}", true, minAppVersion, stage.toString(), env);
		} catch (Exception e) {
			return new ValidationResults("Invalid rule: " + e.getMessage(),  Status.BAD_REQUEST);
		}

		try {
			VerifyRule.checkValidity(ruleString, minInputSample.toString(), newJavascriptFuncs, "{}", false, minAppVersion, stage.toString(), env);
		} catch (Exception e) {
			return new ValidationResults("Invalid rule due to unverified optional fields: " + e.getMessage() /*+ ". To verify an optional field, check that the field and its subelements exist. For examples, see the Airlock Rules help."*/,  Status.BAD_REQUEST);
		}

		if (duplicateFuntions != null)
			return new ValidationResults(duplicateFuntions, Status.OK);

		return null;
	}	

	private String findDuplicateFunctions(String newUtility, String newJavascriptFuncs) {
		String duplicateFunctions = "";
		Map<String, int[]> functionsInNewUtility = VerifyRule.findFunctions(newUtility);
		Map<String, int[]> functionsInAllUtilities = VerifyRule.findFunctions(newJavascriptFuncs);

		//look for duplications in all utilities but if the duplicate function is not from the new utility - ignore it
		Set<String> functionNames = functionsInAllUtilities.keySet();
		for (String functionName:functionNames) {
			if (functionsInAllUtilities.get(functionName)[0]>1) {
				if (functionsInNewUtility.containsKey(functionName)) {
					if (!duplicateFunctions.isEmpty()) {
						duplicateFunctions += ", ";
					}
					duplicateFunctions += functionName;
				}
			}
		}

		if (!duplicateFunctions.isEmpty()) 
			return "duplicate functions: " + duplicateFunctions;

		return null;
	}

	//if status is 200 (OK) - the string is a warning that should be added to the output JSON
	public ValidationResults simulateUtilityForConfiguration(String newUtility, String configString, ServletContext context, Season season, Stage stage, String minAppVersion)
	{
		//validate that added/updated utility didn't break the javascript context. Run it with "true" rule
		//String newJavascriptFuncs = season.getUtilities().generateUtilityCodeSectionForStage(stage, newUtility, null, null);

		ValidationCache tester = new ValidationCache(newUtility, null, null); // a cache that adds the new utility
		ValidationCache.Info info;
		try {
			info = tester.getInfo(context, season, stage, minAppVersion);
		} catch (JSONException | GenerationException e) {
			return new ValidationResults("Failed to generate data sample: " + e.getMessage(), Status.BAD_REQUEST);
		}
		String newJavascriptFuncs = info.javascriptFunctions;

		Environment env = new Environment();
		env.setServerVersion(season.getServerVersion());

		//verify javascript correctness 
		try {
			VerifyRule.checkValidity("true", "{context:{}}", newJavascriptFuncs, "{}", false, minAppVersion, stage.toString(), env);
		} catch (ValidationException e) {
			return new ValidationResults("Illegal JavaScript: " + e.getMessage(), Status.BAD_REQUEST);
		}	

		//check for duplicate functions and add warning
		String duplicateFuntions = null;
		duplicateFuntions = findDuplicateFunctions (newUtility, newJavascriptFuncs);
		
		try {
			//VerifyRule.fullConfigurationEvaluation("true", configString, minInputSample.toString(), maxInputSample.toString(), newJavascriptFuncs, enStrings.toString(), minAppVersion, stage.toString(), env);
			VerifyRule.fullConfigurationEvaluation("true", configString, info.minimalInvoker, info.maximalInvoker);
		} catch (ValidationException e) {
			return new ValidationResults("The configuration is invalid: " + e.getMessage(), Status.BAD_REQUEST);		
		}

		if (duplicateFuntions != null)
			return new ValidationResults(duplicateFuntions, Status.OK);

		return null;
	}

	public Boolean isUsingString(String stringId){
		if(getUtility().contains(stringId)){
			return true;
		}
		return false;
	}

}
