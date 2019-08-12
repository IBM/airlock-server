package com.ibm.airlock.admin.analytics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response.Status;

import com.ibm.airlock.Strings;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.AnalyticsServices;
import com.ibm.airlock.Constants;
import com.ibm.airlock.Constants.Action;
import com.ibm.airlock.Constants.InputSampleGenerationMode;
import com.ibm.airlock.Constants.OutputJSONMode;
import com.ibm.airlock.Constants.RoleType;
import com.ibm.airlock.Constants.Stage;
import com.ibm.airlock.Constants.UtilityType;
import com.ibm.airlock.admin.authentication.UserInfo;
import com.ibm.airlock.admin.BaseAirlockItem;
import com.ibm.airlock.admin.Branch;
import com.ibm.airlock.admin.ConfigurationRuleItem;
import com.ibm.airlock.admin.FeatureItem;
import com.ibm.airlock.admin.GenerationException;
import com.ibm.airlock.admin.InternalUserGroups;
import com.ibm.airlock.admin.JsonGenerator;
import com.ibm.airlock.admin.OrderingRuleItem;
import com.ibm.airlock.admin.Product;
import com.ibm.airlock.admin.Season;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.ValidationResults;
import com.ibm.airlock.admin.MergeBranch.MergeException;
import com.ibm.airlock.admin.analytics.AnalyticsDataCollection.FeatureAttributesPair;
import com.ibm.airlock.engine.Environment;
import com.ibm.airlock.engine.FeatureAttributes;

public class Experiment extends BaseStagedAnalyticsItem {

	private LinkedList<Variant> variants = new LinkedList<Variant>();
	private UUID productId = null; //required in create and update
	private String minVersion = null; //required in create and update
	private String maxVersion = null; //optional
	private String hypothesis = null; //optional
	private String measurements = null; //optional
	private String owner = null; //optional
	private LinkedList<String> controlGroupVariants = new LinkedList<String>(); //optional(the control variants name)
	private ArrayList<ExperimentRange> rangesList = new ArrayList<ExperimentRange>(); //forbidden in create, mandatory in update if some ranges already exists
	private Boolean indexExperiment = false; 
	
	public Boolean getIndexExperiment() {
		return indexExperiment;
	}

	public void setIndexExperiment(Boolean indexExperiment) {
		this.indexExperiment = indexExperiment;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public LinkedList<Variant> getVariants() {
		return variants;
	}

	public void setVariants(LinkedList<Variant> variants) {
		this.variants = variants;
	}
	
	public UUID getProductId() {
		return productId;
	}

	public void setProductId(UUID productId) {
		this.productId = productId;
	}
	
	public String getMinVersion() {
		return minVersion;
	}
	
	public void setMinVersion(String minVersion) {
		this.minVersion = minVersion;
	}
	
	public String getMaxVersion() {
		return maxVersion;
	}
	
	public void setMaxVersion(String maxVersion) {
		this.maxVersion = maxVersion;
	}
	
		
	public String getHypothesis() {
		return hypothesis;
	}

	public void setHypothesis(String hypothesis) {
		this.hypothesis = hypothesis;
	}

	public String getMeasurements() {
		return measurements;
	}

	public void setMeasurements(String measurments) {
		this.measurements = measurments;
	}
	
	public LinkedList<String> getControlGroupVariants() {
		return controlGroupVariants;
	}

	public void setControlGroupVariants(LinkedList<String> controlGroupsVariants) {
		this.controlGroupVariants = controlGroupsVariants;
	}
	
	public ArrayList<ExperimentRange> getRangesList() {
		return rangesList;
	}
	
	public void setRangesList(ArrayList<ExperimentRange> rangesList) {
		this.rangesList = rangesList;
	}
	

	public Experiment(UUID productId) {
		super();		
		this.productId = productId;
	}			

	public ValidationResults validateExperimentJSON(JSONObject experimentJSON, ServletContext context, UserInfo userInfo) throws MergeException {
		ValidationResults vr = super.validateStagedAnalyticsItemJSON(experimentJSON, context, userInfo);
		if (vr!=null)
			return vr;
		
		try {
			//productId
			if (!experimentJSON.containsKey(Constants.JSON_FIELD_PRODUCT_ID) || experimentJSON.getString(Constants.JSON_FIELD_PRODUCT_ID) == null || experimentJSON.getString(Constants.JSON_FIELD_PRODUCT_ID).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_PRODUCT_ID), Status.BAD_REQUEST);
			}
	
			@SuppressWarnings("unchecked")
			Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);
			
			String pId = (String)experimentJSON.get(Constants.JSON_FIELD_PRODUCT_ID);
			Product product = productsDB.get(pId);
			if (product == null) {
				return new ValidationResults("The product of the given experiment does not exist.", Status.BAD_REQUEST);
			}
			
			//cannot add or update experiment with its indexingInfo
			if (experimentJSON.containsKey(Constants.JSON_FIELD_INDEXING_INFO) && experimentJSON.get(Constants.JSON_FIELD_INDEXING_INFO)!=null) {
				return new ValidationResults("The indexingInfo field cannot be specified during experiment creation or update.", Status.BAD_REQUEST);							
			}
			
			//min version
			if (!experimentJSON.containsKey(Constants.JSON_SEASON_FIELD_MIN_VER) || experimentJSON.getString(Constants.JSON_SEASON_FIELD_MIN_VER) == null || experimentJSON.getString(Constants.JSON_SEASON_FIELD_MIN_VER).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_SEASON_FIELD_MIN_VER), Status.BAD_REQUEST);
			}
			
			String minVer = experimentJSON.getString(Constants.JSON_SEASON_FIELD_MIN_VER);					
			String maxVer = null;					
						
			
			//if maxVersion is given - validate				
			if (experimentJSON.containsKey(Constants.JSON_SEASON_FIELD_MAX_VER) && experimentJSON.get(Constants.JSON_SEASON_FIELD_MAX_VER)!=null && !experimentJSON.getString(Constants.JSON_SEASON_FIELD_MAX_VER).isEmpty()) {
				maxVer = experimentJSON.getString(Constants.JSON_SEASON_FIELD_MAX_VER);					
				
				if (Season.compare(minVer, maxVer) > 0) {
					return new ValidationResults("maxVersion must not be less than minVersion.", Status.BAD_REQUEST);
				}
			}
			
			//validate name uniqueness within all products
			String newName = experimentJSON.getString(Constants.JSON_FIELD_NAME);
			for (Experiment exp:product.getExperimentsMutualExclusionGroup().getExperiments()) {
				if (uniqueId == null || !uniqueId.equals(exp.getUniqueId())) { //in update - skip yourself
					if (exp.getName().equals(newName)) {
						return new ValidationResults("An experiment with the specified name already exists. Experiment names must be unique across all products.", Status.BAD_REQUEST);
					}
				}
			}			
			ArrayList<Season> seasonsWithinExpRange = product.getSeasonsWithinRange(minVer, maxVer);
			
			//verify that minVersion is in season that supports analytics
			for (Season s:seasonsWithinExpRange) {
				Environment env = new Environment();
				env.setServerVersion(s.getServerVersion()); 			

				if (!AnalyticsServices.isExperimentsSupported(env)) {
					String errMsg = Strings.expNotSupportedInSeason;
					return new ValidationResults(errMsg, Status.BAD_REQUEST);
				}
			}

			//internalUserGroups - optional
			if (experimentJSON.containsKey(Constants.JSON_FIELD_INTERNAL_USER_GROUPS) && experimentJSON.get(Constants.JSON_FIELD_INTERNAL_USER_GROUPS) != null) {				
				JSONArray groupsArr = experimentJSON.getJSONArray(Constants.JSON_FIELD_INTERNAL_USER_GROUPS); //validate that is String array value

				@SuppressWarnings("unchecked")
				Map<String,InternalUserGroups> groupsPerProductMap = (Map<String,InternalUserGroups>) context.getAttribute(Constants.USER_GROUPS_PER_PRODUCT_PARAM_NAME);
				InternalUserGroups userGroups = groupsPerProductMap.get(product.getUniqueId().toString());

				//validate that specified groups actually exist
				for (int k=0; k<groupsArr.length(); k++) {
					if (!userGroups.getGroupsMap().containsKey(groupsArr.get(k))) {
						return new ValidationResults("The internalUserGroups value '" + groupsArr.get(k) + "' does not exist.", Status.BAD_REQUEST);
					}
				}
				
				//verify that there are no duplications in the user groups
				for(int j = 0; j < groupsArr.length(); j++){
				    for(int k = j+1; k < groupsArr.length(); k++){
				        if (groupsArr.get(j).equals(groupsArr.get(k))){
				        	return new ValidationResults("The internalUserGroups value '" + groupsArr.get(k) + "' appears more than once in the internalUserGroups list.", Status.BAD_REQUEST);
				        }
				    }
				}
			}
			
			String stageStr = experimentJSON.getString(Constants.JSON_FEATURE_FIELD_STAGE);
			Stage stageObj = Utilities.strToStage(stageStr); //I know it is fine - was validated in base
						
			//validate rule (basic rule validation is done in the BaseAnalyticsItem, now only validate rule context/str/util ...)
			ValidationResults res = validateRule (experimentJSON.getJSONObject(Constants.JSON_FEATURE_FIELD_RULE), minVer, stageObj, seasonsWithinExpRange, context, userInfo);
			if (res !=null)
				return res;			

			Action action = Action.ADD;
			
			if (experimentJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && experimentJSON.get(Constants.JSON_FIELD_UNIQUE_ID)!=null) {
				//if JSON contains uniqueId - update an existing feature otherwise create a new feature
				action = Action.UPDATE;
			}
			
			boolean indexExp = false;
			if (experimentJSON.containsKey(Constants.JSON_FIELD_INDEX_EXPERIMENT) && experimentJSON.get(Constants.JSON_FIELD_INDEX_EXPERIMENT)!=null) {
				indexExp = experimentJSON.getBoolean(Constants.JSON_FIELD_INDEX_EXPERIMENT); //validate legal boolean				
			}
			else {
				indexExp = indexExperiment;
			}						
			
			//validate controlGroupVariants
			res = validateControlGroupVariants(experimentJSON);
			if (res!=null)
				return res;
			
			boolean enabledVar = experimentJSON.getBoolean(Constants.JSON_FEATURE_FIELD_ENABLED);
			
			if (action == Action.ADD) {
				//variants are not allowed in add experiment (adding only one by one)
				if (experimentJSON.containsKey(Constants.JSON_FIELD_VARIANTS) && experimentJSON.get(Constants.JSON_FIELD_VARIANTS) != null && !experimentJSON.getJSONArray(Constants.JSON_FIELD_VARIANTS).isEmpty()) {
					return new ValidationResults("Cannot add an experiment with variants. Add the experiment and its variants one by one.", Status.BAD_REQUEST);
				}			
				
				//ranges is forbidden in create
				if (experimentJSON.containsKey(Constants.JSON_FIELD_RANGES) && experimentJSON.get(Constants.JSON_FIELD_RANGES)!=null ) {
					return new ValidationResults("The ranges field cannot be specified during experiment creation.", Status.BAD_REQUEST);
				}
				
				if (indexExp) {
					//index experiment cannot be true when no ranges exists
					return new ValidationResults("The indexExperiment cannot be true during experiment creation.", Status.BAD_REQUEST);
				}
				
				if (enabledVar) {
					//experiment cannot enabled when no variants exist
					return new ValidationResults("An experiment without variants cannot be enabled. Add variants and try again.", Status.BAD_REQUEST);
					
				}
			}
			else {
				//product id must exists and not be changed
				String productIdStr = experimentJSON.getString(Constants.JSON_FIELD_PRODUCT_ID);
				if (!productIdStr.equals(productId.toString())) {
					return new ValidationResults(String.format(Strings.fieldCannotBeChangedDuringUpdate, Constants.JSON_FIELD_PRODUCT_ID), Status.BAD_REQUEST);
				}
				
				//when moving exp from prod to dev verify that no variant of this exp is in prod
				boolean moveExpFromProdToDev  = stageObj.equals(Stage.DEVELOPMENT) && stage.equals(Stage.PRODUCTION);
				
				
				if (!experimentJSON.containsKey(Constants.JSON_FIELD_VARIANTS) || experimentJSON.get(Constants.JSON_FIELD_VARIANTS) == null) {
					return new ValidationResults("The variants field is missing. This field must be specified during update.", Status.BAD_REQUEST);
				}

				//ranges field is mandatory if already exists in the experiemnt
				if (!experimentJSON.containsKey(Constants.JSON_FIELD_RANGES) || experimentJSON.get(Constants.JSON_FIELD_RANGES)==null ) {
					if (rangesList.size()>0) {
						return new ValidationResults("The ranges field must be specified in update experiment if ranges already exist.", Status.BAD_REQUEST);
					}
				}
					
				boolean noRangeExist=false;
				//validate ranges - only admin can update experiment ranges
				if (experimentJSON.containsKey(Constants.JSON_FIELD_RANGES) && experimentJSON.get(Constants.JSON_FIELD_RANGES)!=null ) {
					JSONArray expRangesArr = experimentJSON.getJSONArray(Constants.JSON_FIELD_RANGES);
					res = validateRanges (expRangesArr, name, userInfo);
					if (res != null)
						return res;
					if(expRangesArr.size() == 0) {
						noRangeExist = true;
					}
					else {
						noRangeExist = rangesList.size()==0;
					}
				}
				
				if (noRangeExist && enabled == false && enabledVar == true) { //when no ranges exists but the exp is turned on from disabled to enabled => range will be created in a sec
					noRangeExist = false;
				}
				
				if (indexExp && noRangeExist) {
					//index experiment cannot be true when no ranges exists
					return new ValidationResults("The indexExperiment cannot be true when no indexing range exist.", Status.BAD_REQUEST);
				}
				
				JSONArray variantsArr = experimentJSON.getJSONArray(Constants.JSON_FIELD_VARIANTS);
				@SuppressWarnings("unchecked")
				Map<String, Variant> variantsDB = (Map<String, Variant>)context.getAttribute(Constants.VARIANTS_DB_PARAM_NAME);
				
				boolean expeimrtQuotaValidationNeeded = false;
				for (int i=0; i<variantsArr.size(); i++) {
					JSONObject varJSONObj = variantsArr.getJSONObject(i);
					String variantId = varJSONObj.getString(Constants.JSON_FIELD_UNIQUE_ID);
					if (variantId == null) {
						return new ValidationResults("Variant id is missing.", Status.BAD_REQUEST);
					}
					Variant variant = variantsDB.get(variantId);
					if (variant == null) {
						return new ValidationResults("Variant does not exist.", Status.BAD_REQUEST);
					}
					
					vr = variant.validateVariantJSON(varJSONObj, context, userInfo, minVer, maxVer, false);					
					if (vr!=null && !vr.status.equals(Status.OK))
						return vr;
					
					if (vr!=null && vr.status.equals(Status.OK))
						expeimrtQuotaValidationNeeded = true;
					
					String variantStage = varJSONObj.getString(Constants.JSON_FEATURE_FIELD_STAGE);
					//when moving exp from prod to dev verify that no variant of this exp is in prod
					if (moveExpFromProdToDev && variantStage.equals(Stage.PRODUCTION.toString())) {
						return new ValidationResults("An experiment in the DEVELOPMENT stage cannot include a variant in the PRODUCTION stage.", Status.BAD_REQUEST);
					}
				}
				//validate that no variant was added to list or removed from list 
				String err = validateVraiantsList(variantsArr);
				if (err!=null)
					return new ValidationResults(err, Status.BAD_REQUEST);
				
				if (expeimrtQuotaValidationNeeded || 
						(stageObj.equals(Stage.PRODUCTION) && isExperimentRangeChanged(minVersion, maxVersion, minVer, maxVer))) {
					//if one of the variants changed and need quota validation or the range of experiment in production changed
					Experiment tmpExp = new Experiment(productId);
					tmpExp.fromJSON(experimentJSON);
					int updatedProdCount = tmpExp.getAnalyticsProductionCounter(context, null, null, null);
					int experimentAnalyticsQuota = tmpExp.getQuota(context);
					if(updatedProdCount>experimentAnalyticsQuota) {
						return new ValidationResults("Failed to update the experiment. The maximum number of items in production to send to analytics for experiment " + name + " was exceeded. The maximum number allowed is " + experimentAnalyticsQuota + ". The update increased the number to " + updatedProdCount, Status.BAD_REQUEST);		
					}
					
				}
				
				if (enabledVar && (variantsArr==null || variantsArr.size() == 0)) {
					//experiment cannot enabled when no variants exist
					return new ValidationResults("An experiment without variants cannot be enabled. Add variants and try again.", Status.BAD_REQUEST);					
				}
			}
			
		} catch (JSONException jsne) {
			return new ValidationResults(jsne.getMessage(), Status.BAD_REQUEST);
		}

		return null;
	}
	
	private ValidationResults validateRanges(JSONArray expRangesArr, String expName, UserInfo userInfo) throws JSONException {
		ValidationResults res = null;
		Long prevRangeEnd = 0L;
		boolean prevEndIsNull = false;
		for (int i=0; i<expRangesArr.size(); i++) {
			if (prevEndIsNull) {
				return new ValidationResults("Cannot add a range after a range that is open (end=null).", Status.BAD_REQUEST);
			}
			JSONObject rangeJson = expRangesArr.getJSONObject(i);
			res = ExperimentRange.validateRangeJson(rangeJson);
			if (res!=null)
				return res;

			ExperimentRange tmp = new ExperimentRange();
			tmp.fromJSON(rangeJson);

			if (prevRangeEnd > 0 &&  prevRangeEnd > tmp.start.getTime()) {
				return new ValidationResults("Ranges of experiment " + expName + " are overlapping.", Status.BAD_REQUEST);
			}
			if (tmp.end!=null)
				prevRangeEnd = tmp.end.getTime();
			else
				prevEndIsNull = true;
		}
		
		if (!areRangesListEqual(expRangesArr) && !validAdmin(userInfo)) {
			return new ValidationResults("Only an Administrator can update experiment ranges.", Status.UNAUTHORIZED);
		}
		return res;
	}
	static boolean validAdmin(UserInfo userInfo)
	{
		return userInfo == null || userInfo.getRoles().contains(RoleType.Administrator);
	}
	public boolean areRangesListEqual(JSONArray newRangesList) throws JSONException {
		if (newRangesList == null && rangesList == null)
			return true;

		if (newRangesList == null || rangesList == null)
			return false;

		if (newRangesList.size()!=rangesList.size())
			return false;

		for (int i=0;i<newRangesList.size(); i++) {
			JSONObject rangeJson = newRangesList.getJSONObject(i);
			
			ExperimentRange newRange = new ExperimentRange();
			newRange.fromJSON(rangeJson);
						
			ExperimentRange existingRange = rangesList.get(i);

			if (!newRange.equals(existingRange))
				return false;
		}
		return true;
	}
	
	private ValidationResults validateControlGroupVariants(JSONObject experimentJSON) throws JSONException {

		if (experimentJSON.containsKey(Constants.JSON_FIELD_CONTROL_GROUP_VARIANTS) && experimentJSON.get(Constants.JSON_FIELD_CONTROL_GROUP_VARIANTS) != null) {
			JSONArray controlGroupVariantsArr = experimentJSON.getJSONArray(Constants.JSON_FIELD_CONTROL_GROUP_VARIANTS);
			
			TreeSet<String> existingControlGroupVariants = new TreeSet<String>();
			for (int i=0; i<controlGroupVariantsArr.size(); i++) {
				String ctlvarName = controlGroupVariantsArr.getString(i);
				if (existingControlGroupVariants.contains(ctlvarName)) {
					return new ValidationResults("controlGroupVariant: '"+ ctlvarName + "' appears more than once in the controlGroupVariants list." , Status.BAD_REQUEST);					
				}
				
				boolean found = false;
				for (Variant var:variants) {
					if (var.getName().equals(ctlvarName)){
						found = true;
						break;
					}
				}
				
				if (!found) {
					return new ValidationResults("controlGroupVariant: '" + ctlvarName + "' does not exist in the experiment." , Status.BAD_REQUEST);
				}
				
				existingControlGroupVariants.add(ctlvarName);
				
			}
		}
		
		return null;
	}

	//maxVersion can be null minVersion cannot be null 
	private boolean isExperimentRangeChanged(String origMinVersion, String origMaxVersion, String newMinVersion, String newMaxVersion) {
		if (!origMinVersion.equals(newMinVersion))
			return true;
		
		if (origMaxVersion == null && newMaxVersion != null)
			return true;
		
		if (origMaxVersion != null && newMaxVersion == null)
			return true;
		
		if (origMaxVersion == null && newMaxVersion == null)
			return false;
		
		return !origMaxVersion.equals(newMaxVersion);
	}
	
	//validate that no variant was added to list or removed from list
	//return error message is variant was add or removed from list, null otherwise
	private String validateVraiantsList(JSONArray variantsArr) throws JSONException {
		if (variantsArr.size() > variants.size()) {
			return "A variant cannot be added to an experiment when the experiment is being updated. Instead, call add variant."; 		
		}
		
		if (variantsArr.size() < variants.size()) {
			return "A variant cannot be removed from an experiment when the experiment is being updated. Instead, call delete variant.";			 	
		}

		//same number of variants - look at the uniqueIds
		for (Variant var:variants) {
			boolean found = false;
			for (int i=0; i<variantsArr.size(); i++) { 
				JSONObject varJSON = variantsArr.getJSONObject(i); //after validate - i know that is json and containns uniqueId field
			
				if (var.getUniqueId().toString().equals(varJSON.getString(Constants.JSON_FIELD_UNIQUE_ID))) {
					found = true;
					break;
				}
			}
			if (!found) {
				return "A variant cannot be added or removed from an experiment when the experiment is being updated. Instead, call add or delete variant.";				
			}
		}
		return null;
	}

	public JSONObject toJson(OutputJSONMode outputMode, ServletContext context, boolean skipDisabledVariant, boolean addAnalytics) throws JSONException {
		JSONObject res = super.toJson(outputMode);
		if (res == null) {
			// this can only happen in runtime_production mode when the feature is in development stage
			return null;
		}
		
		res.put(Constants.JSON_FIELD_PRODUCT_ID, productId==null?null:productId.toString());
		res.put(Constants.JSON_SEASON_FIELD_MIN_VER, minVersion);
		res.put(Constants.JSON_SEASON_FIELD_MAX_VER, maxVersion);
		
		
		JSONArray variantsArray = new JSONArray();
		for (Variant variant:variants) {
			if (skipDisabledVariant && variant.getEnabled() == false)
				continue; //skip disabled variants
			
			if (outputMode == OutputJSONMode.RUNTIME_PRODUCTION) {
				if (variant.getStage() == Stage.DEVELOPMENT) 
					continue;
			}
			
			variantsArray.add(variant.toJson(outputMode, context));
		}
		
		JSONArray rangesArr = getRangesJsonArray();
		res.put(Constants.JSON_FIELD_RANGES, rangesArr);
		
		res.put(Constants.JSON_FIELD_CONTROL_GROUP_VARIANTS, controlGroupVariants);
		res.put(Constants.JSON_FIELD_VARIANTS, variantsArray);
		res.put(Constants.JSON_FEATURE_FIELD_OWNER, owner);
		res.put(Constants.JSON_FIELD_INDEX_EXPERIMENT, indexExperiment);
				
		if (outputMode.equals(OutputJSONMode.ADMIN) || outputMode.equals(OutputJSONMode.DISPLAY)) {
			res.put(Constants.JSON_FIELD_HYPOTHESIS, hypothesis);
			res.put(Constants.JSON_FIELD_MEASUREMENTS, measurements);
		}
		
		if (addAnalytics) {
			@SuppressWarnings("unchecked")
			Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);
			Product product = productsDB.get(productId.toString());
			
			ArrayList<Season> seasons = product.getSeasonsWithinRange(minVersion, maxVersion);
			
			@SuppressWarnings("unchecked")
			Map<String, BaseAirlockItem> mastersAirlockItemsDB = (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);

			//map between the input field and the maximal stage in all masters/branches
			HashMap<String, Stage> inputFieldsMaxStageMap = new HashMap<String, Stage>();
			
			//map between the feature on/off namespace.name and the maximal stage in all masters/branches
			HashMap<String, Stage> featuresOnOffMaxStageMap = new HashMap<String, Stage>();
			
			//map between the feature namespace.name that reports attributes and a pair of the maximal stage in all masters/branches and its pruned attributes 
			HashMap<String, StageAttributesPair> featuresAttributesStageMap = new HashMap<String, StageAttributesPair>();
			
			analyticsCounters counters = new analyticsCounters();
			
			HashMap<String, Stage> branchesStageInExperment = getBranchesStageInExperment();
			
			for (Season season:seasons) {
				//add the season's master analytics (master branch stage is the sam stage as the experiment's)
				addAnalytics(season.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection(), mastersAirlockItemsDB, inputFieldsMaxStageMap, featuresOnOffMaxStageMap, featuresAttributesStageMap, counters, null, stage, null, null, null, season);
				for (Branch branch:season.getBranches().getBranchesList()) {
					if (branchesStageInExperment.containsKey(branch.getName())) {
						Stage branchStageInExp = branchesStageInExperment.get(branch.getName());
					
						Map<String, BaseAirlockItem> branchAirlockItemsDB;
						try {
							branchAirlockItemsDB = Utilities.getAirlockItemsDB (branch.getUniqueId().toString(), context);
						} catch (MergeException e) {
							throw new JSONException(Strings.mergeException  + e.getMessage());
						} 
						addAnalytics(branch.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection(), branchAirlockItemsDB, inputFieldsMaxStageMap, featuresOnOffMaxStageMap, featuresAttributesStageMap, counters, branch, branchStageInExp, null, null, null, season);						
					}
				}
			}
			JSONObject experimentAnalytics = new JSONObject();
			JSONArray inputFields = new JSONArray();
			Set<String> inputFiledNames = inputFieldsMaxStageMap.keySet();
			if (outputMode.equals(OutputJSONMode.RUNTIME_PRODUCTION)) {
				for (String inputField:inputFiledNames) {
					Stage maxStage = inputFieldsMaxStageMap.get(inputField);
					if (maxStage.equals(Stage.PRODUCTION))
						inputFields.add(inputField);
				}
			}
			else {
				inputFields.addAll(inputFiledNames);
			}
			
			JSONArray featuresOnOff = new JSONArray();
			Set<String> featureOnOffNames = featuresOnOffMaxStageMap.keySet();
			if (outputMode.equals(OutputJSONMode.RUNTIME_PRODUCTION)) {
				for (String featureName:featureOnOffNames) {
					Stage maxStage = featuresOnOffMaxStageMap.get(featureName);
					if (maxStage.equals(Stage.PRODUCTION))
						featuresOnOff.add(featureName);
				}
			}
			else {
				featuresOnOff.addAll(featureOnOffNames);
			}
			
			JSONArray featuresAttribute = new JSONArray();
			Set<String> faetureAttributesNames = featuresAttributesStageMap.keySet();
			for (String featureAttName:faetureAttributesNames) {
				if (outputMode.equals(OutputJSONMode.RUNTIME_PRODUCTION) && featuresAttributesStageMap.get(featureAttName).stage.equals(Stage.DEVELOPMENT))
					continue;
				
				JSONObject featureAttJSON = new JSONObject();
				featureAttJSON.put(Constants.JSON_FIELD_NAME, featureAttName);
				featureAttJSON.put(Constants.JSON_FIELD_ATTRIBUTES, featuresAttributesStageMap.get(featureAttName).attributes);
				featuresAttribute.add(featureAttJSON);
			}
			
			experimentAnalytics.put (Constants.JSON_FIELD_INPUT_FIELDS_FOR_ANALYTICS, inputFields);
			experimentAnalytics.put (Constants.JSON_FIELD_FEATURES_CONFIGS_FOR_ANALYTICS, featuresOnOff);
			experimentAnalytics.put (Constants.JSON_FIELD_FEATURES_ATTRIBUTES_FOR_ANALYTICS, featuresAttribute);
			res.put(Constants.JSON_FIELD_ANALYTICS, experimentAnalytics);
		}
		return res;
	}
	
	public void fromJSON(JSONObject input, ServletContext context, boolean addToVariantsDB) throws JSONException {
		super.fromJSON(input);
				
		if (input.containsKey(Constants.JSON_FIELD_PRODUCT_ID) && input.get(Constants.JSON_FIELD_PRODUCT_ID) != null) {
			String sStr = input.getString(Constants.JSON_FIELD_PRODUCT_ID);			
			productId = UUID.fromString(sStr);			
		}			
		
		if (input.containsKey(Constants.JSON_SEASON_FIELD_MIN_VER) && input.get(Constants.JSON_SEASON_FIELD_MIN_VER) != null) 
			minVersion = input.getString(Constants.JSON_SEASON_FIELD_MIN_VER);

		if (input.containsKey(Constants.JSON_SEASON_FIELD_MAX_VER) && input.get(Constants.JSON_SEASON_FIELD_MAX_VER) != null) 
			maxVersion = input.getString(Constants.JSON_SEASON_FIELD_MAX_VER);

		if (input.containsKey(Constants.JSON_FIELD_HYPOTHESIS) && input.get(Constants.JSON_FIELD_HYPOTHESIS)!=null) 
			hypothesis = input.getString(Constants.JSON_FIELD_HYPOTHESIS).trim();
		
		if (input.containsKey(Constants.JSON_FIELD_MEASUREMENTS) && input.get(Constants.JSON_FIELD_MEASUREMENTS)!=null) 
			measurements = input.getString(Constants.JSON_FIELD_MEASUREMENTS).trim();
		
		if (input.containsKey(Constants.JSON_FEATURE_FIELD_OWNER) && input.get(Constants.JSON_FEATURE_FIELD_OWNER)!=null) 
			owner = input.getString(Constants.JSON_FEATURE_FIELD_OWNER).trim();
		
		if (input.containsKey(Constants.JSON_FIELD_INDEX_EXPERIMENT) && input.get(Constants.JSON_FIELD_INDEX_EXPERIMENT)!=null) 
			indexExperiment = input.getBoolean(Constants.JSON_FIELD_INDEX_EXPERIMENT);
		
		rangesList.clear();
		if (input.containsKey(Constants.JSON_FIELD_RANGES) && input.get(Constants.JSON_FIELD_RANGES)!=null) {
			JSONArray expRangesArr = input.getJSONArray(Constants.JSON_FIELD_RANGES);
			setRangesListFromArray(expRangesArr);						
		}
		
		if (input.containsKey(Constants.JSON_FIELD_CONTROL_GROUP_VARIANTS) && input.get(Constants.JSON_FIELD_CONTROL_GROUP_VARIANTS) != null) {
			controlGroupVariants.clear();
			JSONArray controlGroupVariantsArr = input.getJSONArray(Constants.JSON_FIELD_CONTROL_GROUP_VARIANTS);
			controlGroupVariants.addAll(controlGroupVariantsArr);
		}
					
		if (input.containsKey(Constants.JSON_FIELD_VARIANTS) && input.get(Constants.JSON_FIELD_VARIANTS) != null) {
			variants.clear();
			
			@SuppressWarnings("unchecked")
			Map<String, Variant> variantsDB = (Map<String, Variant>)context.getAttribute(Constants.VARIANTS_DB_PARAM_NAME);
			
			JSONArray variantArr = input.getJSONArray(Constants.JSON_FIELD_VARIANTS);			
			for (int i=0; i<variantArr.size(); i++) {
				JSONObject varJSONObj = variantArr.getJSONObject(i);
				Variant varint = new Variant(this.uniqueId);
				varint.fromJSON(varJSONObj, context);
				variants.add(varint);

				if (addToVariantsDB) 
					variantsDB.put(varint.getUniqueId().toString(), varint);
			}
		}				
	}

	private void setRangesListFromArray(JSONArray expRangesArr) throws JSONException {
		rangesList.clear();
		for (int i=0; i<expRangesArr.size(); i++) {
			JSONObject rangeJson = expRangesArr.getJSONObject(i);
			ExperimentRange range = new ExperimentRange();
			range.fromJSON(rangeJson);
			rangesList.add(range);
		}
		
	}

	public JSONArray getRangesJsonArray() throws JSONException {
		JSONArray rangesArr = new JSONArray();
		for (ExperimentRange range:rangesList) {
			JSONObject rangeJson = range.toJson();
			rangesArr.add(rangeJson);
		}
		return rangesArr;
	}
	
	public void addVariant(Variant newVariantObj) {
		// at this stage i know the name is unique since i m after validate
		variants.add(newVariantObj);
		lastModified = new Date();
	}
	
	//return null if OK, error staring on error
	public String removeVariant(UUID variantId) {
		if (enabled && variants.size() == 1) {
			return "The variant cannot be removed. An experiment without variants cannot be enabled. First disable the experiment, then remove the variant.";
		}
		
		for (int i=0; i< variants.size(); i++) {
			if (variants.get(i).getUniqueId().equals(variantId)) {
				variants.remove(i);
				return null;
			}
		}

		lastModified = new Date();
		
		return "Unable to remove variant " + variantId.toString() + ": The specified variant does not exist in this experiment.";		
	}	
	
	public ValidationResults validateProductionDontChanged(JSONObject updatedExperimentJSON) throws JSONException {
		ValidationResults res = super.validateProductionDontChanged(updatedExperimentJSON, "vaexperimentriant");
		if (res!=null)
			return res;
		
		String err = "Unable to update the experiment. Only a user with the Administrator or Product Lead role can change an experiment that is in the production stage.";			
				
		if (stage == Stage.PRODUCTION) {
			
			String updatedMinVer = updatedExperimentJSON.getString(Constants.JSON_SEASON_FIELD_MIN_VER);
			if (!updatedMinVer.equals(minVersion)) 
				return new ValidationResults(err, Status.UNAUTHORIZED);
			
			
			String updatedMaxVer = updatedExperimentJSON.getString(Constants.JSON_SEASON_FIELD_MAX_VER);
			if (!updatedMaxVer.equals(maxVersion)) 
				return new ValidationResults(err, Status.UNAUTHORIZED);			
			
			if (updatedExperimentJSON.containsKey(Constants.JSON_FIELD_HYPOTHESIS) &&  updatedExperimentJSON.get(Constants.JSON_FIELD_HYPOTHESIS)!=null) {
				//if missing from json or null - ignore
				String updatedHypothesis = updatedExperimentJSON.getString(Constants.JSON_FIELD_HYPOTHESIS);
				if (hypothesis == null || !hypothesis.equals(updatedHypothesis)) {
					return new ValidationResults(err, Status.UNAUTHORIZED);				
				}
			}	
			
			if (updatedExperimentJSON.containsKey(Constants.JSON_FEATURE_FIELD_OWNER) &&  updatedExperimentJSON.get(Constants.JSON_FEATURE_FIELD_OWNER)!=null) {
				//if missing from json or null - ignore
				String updatedOwner = updatedExperimentJSON.getString(Constants.JSON_FEATURE_FIELD_OWNER);
				if (owner == null || !owner.equals(updatedOwner)) {
					return new ValidationResults(err, Status.UNAUTHORIZED);				
				}
			}	
			
			if (updatedExperimentJSON.containsKey(Constants.JSON_FIELD_INDEX_EXPERIMENT) &&  updatedExperimentJSON.get(Constants.JSON_FIELD_INDEX_EXPERIMENT)!=null) {
				//if missing from json or null - ignore
				Boolean updatedIndexExp = updatedExperimentJSON.getBoolean(Constants.JSON_FIELD_INDEX_EXPERIMENT);
				if (!indexExperiment.equals(updatedIndexExp)) {
					return new ValidationResults(err, Status.UNAUTHORIZED);				
				}
			}	
			
			if (updatedExperimentJSON.containsKey(Constants.JSON_FIELD_MEASUREMENTS) &&  updatedExperimentJSON.get(Constants.JSON_FIELD_MEASUREMENTS)!=null) {
				//if missing from json or null - ignore
				String updatedMeasurments = updatedExperimentJSON.getString(Constants.JSON_FIELD_MEASUREMENTS);
				if (measurements == null || !measurements.equals(updatedMeasurments)) {
					return new ValidationResults(err, Status.UNAUTHORIZED);				
				}
			}	
			
			if (updatedExperimentJSON.containsKey(Constants.JSON_FIELD_INDEX_EXPERIMENT) &&  updatedExperimentJSON.get(Constants.JSON_FIELD_INDEX_EXPERIMENT)!=null) {
				//if missing from json or null - ignore
				Boolean updatedIndexExp = updatedExperimentJSON.getBoolean(Constants.JSON_FIELD_INDEX_EXPERIMENT);
				if (!indexExperiment.equals(updatedIndexExp)) {
					return new ValidationResults(err, Status.UNAUTHORIZED);				
				}
			}	
			
			if (updatedExperimentJSON.containsKey(Constants.JSON_FIELD_CONTROL_GROUP_VARIANTS) &&  updatedExperimentJSON.get(Constants.JSON_FIELD_CONTROL_GROUP_VARIANTS)!=null) {
				//if missing from json or null - ignore
				JSONArray controlGroupVariantsArr = updatedExperimentJSON.getJSONArray(Constants.JSON_FIELD_CONTROL_GROUP_VARIANTS);
				if (!Utilities.compareIgnoreOrder(controlGroupVariantsArr, controlGroupVariants)) {
					return new ValidationResults(err, Status.UNAUTHORIZED);				
				}
			}	
			
			//validate that the order of variants that are on production was not changed:
			JSONArray updatedVariantsArray = updatedExperimentJSON.getJSONArray(Constants.JSON_FIELD_VARIANTS); //after validate - i know exists
			
			JSONArray updatedVariantsInProduction = new JSONArray();
			//create updated variants list that are on production
			for (int i=0; i<updatedVariantsArray.size(); i++) {
				JSONObject variantJSONObj = updatedVariantsArray.getJSONObject(i);
				if (variantJSONObj.containsKey(Constants.JSON_FEATURE_FIELD_STAGE) && variantJSONObj.getString(Constants.JSON_FEATURE_FIELD_STAGE).equals(Stage.PRODUCTION.toString())) {
					updatedVariantsInProduction.add(variantJSONObj);
				}
			}
			
			//create current variants list that are on production
			LinkedList<Variant> variantsInProduction = new LinkedList<Variant>();
			for (int i=0; i<variants.size(); i++) {
				//JSONObject subFeatureJSONObj = updatedSubFeatures.getJSONObject(i);
				if (variants.get(i).getStage().equals(Stage.PRODUCTION)) {
					variantsInProduction.add(variants.get(i));
				}			
			}
						
			
			if (orderChanged (updatedVariantsInProduction, variantsInProduction)) {
				return new ValidationResults(err, Status.UNAUTHORIZED);
			}
		}
		
		return null;		
	}
	
	private boolean orderChanged(JSONArray updatedProdVariants,  LinkedList<Variant> origProdVariants) throws JSONException {
		if (origProdVariants.size() != updatedProdVariants.size()) {
			return true; //variants added hence order changed
		}

		for (int i=0; i<origProdVariants.size(); i++) {
			if (!origProdVariants.get(i).getUniqueId().toString().equals(updatedProdVariants.getJSONObject(i).getString(Constants.JSON_FIELD_UNIQUE_ID))) {
				return true;
			}
		}

		return false;
	}
	
	public String updateExperiment (JSONObject updatedExperimentJSON, ServletContext context, Date now) throws JSONException {
		boolean prevEnabled = enabled;
		Stage prevStage = stage;
		
		String expUpdateDetails = super.updateStagedAnalyticsItem(updatedExperimentJSON, context, "experiment");

		boolean wasChanged = ((expUpdateDetails != null) &&  !expUpdateDetails.isEmpty());
		StringBuilder updateDetails = new StringBuilder(expUpdateDetails);
		
		String updatedMinVer = updatedExperimentJSON.getString(Constants.JSON_SEASON_FIELD_MIN_VER);
		if (!updatedMinVer.equals(minVersion)) {
			updateDetails.append("'minVersion' changed from " + minVersion + " to " + updatedMinVer + ";	");
			minVersion = updatedMinVer;			
			
			wasChanged = true;
		}
		
		//optional fields (only admin can update ranges - checked in validate)
		if (updatedExperimentJSON.containsKey(Constants.JSON_FIELD_RANGES) &&  updatedExperimentJSON.get(Constants.JSON_FIELD_RANGES)!=null) {
			JSONArray updatedExpRangesArr = updatedExperimentJSON.getJSONArray(Constants.JSON_FIELD_RANGES);
			if (!areRangesListEqual(updatedExpRangesArr)) {
				updateDetails.append("'ranges' changed from " + getRangesJsonArray().toString() + " to " + updatedExpRangesArr.toString() + "\n");
				setRangesListFromArray(updatedExpRangesArr);
				wasChanged = true;
			}
		}
		
		if (updatedExperimentJSON.containsKey(Constants.JSON_SEASON_FIELD_MAX_VER) && updatedExperimentJSON.get(Constants.JSON_SEASON_FIELD_MAX_VER)!=null) {
			String updatedMaxVer = updatedExperimentJSON.getString(Constants.JSON_SEASON_FIELD_MAX_VER);
			if (!updatedMaxVer.equals(maxVersion)) {
				updatedMaxVer = (updatedMaxVer.isEmpty()?null:updatedMaxVer);
				updateDetails.append("'maxVersion' changed from " + maxVersion + " to " + updatedMaxVer + ";	");
				maxVersion = updatedMaxVer;
				
				wasChanged = true;
			}
		}
		
		if (updatedExperimentJSON.containsKey(Constants.JSON_FIELD_HYPOTHESIS) &&  updatedExperimentJSON.get(Constants.JSON_FIELD_HYPOTHESIS)!=null) {
			//if missing from json or null - ignore
			String updatedHypothesis = updatedExperimentJSON.getString(Constants.JSON_FIELD_HYPOTHESIS);
			if (hypothesis == null || !hypothesis.equals(updatedHypothesis)) {
				updateDetails.append(" 'hypothesis' changed from '" + hypothesis + "' to '" + updatedHypothesis + "'\n");
				hypothesis  = updatedHypothesis;
				wasChanged = true;
			}
		}	
		
		if (updatedExperimentJSON.containsKey(Constants.JSON_FEATURE_FIELD_OWNER) &&  updatedExperimentJSON.get(Constants.JSON_FEATURE_FIELD_OWNER)!=null) {
			//if missing from json or null - ignore
			String updatedOwner = updatedExperimentJSON.getString(Constants.JSON_FEATURE_FIELD_OWNER);
			if (owner == null || !owner.equals(updatedOwner)) {
				updateDetails.append(" 'owner' changed from '" + owner + "' to '" + updatedOwner + "'\n");
				owner  = updatedOwner;
				wasChanged = true;
			}
		}
		
		if (updatedExperimentJSON.containsKey(Constants.JSON_FIELD_INDEX_EXPERIMENT) &&  updatedExperimentJSON.get(Constants.JSON_FIELD_INDEX_EXPERIMENT)!=null) {
			//if missing from json or null - ignore
			Boolean updatedIndexExp = updatedExperimentJSON.getBoolean(Constants.JSON_FIELD_INDEX_EXPERIMENT);
			if (!indexExperiment.equals(updatedIndexExp)) {
				updateDetails.append(" 'indexExperiment' changed from '" + indexExperiment + "' to '" + updatedIndexExp + "'\n");
				indexExperiment  = updatedIndexExp;
				wasChanged = true;
			}
		}
		
		if (updatedExperimentJSON.containsKey(Constants.JSON_FIELD_MEASUREMENTS) &&  updatedExperimentJSON.get(Constants.JSON_FIELD_MEASUREMENTS)!=null) {
			//if missing from json or null - ignore
			String updatedMeasurments = updatedExperimentJSON.getString(Constants.JSON_FIELD_MEASUREMENTS);
			if (measurements == null || !measurements.equals(updatedMeasurments)) {
				updateDetails.append(" 'measurments' changed from '" + measurements + "' to '" + updatedMeasurments + "'\n");
				measurements  = updatedMeasurments;
				wasChanged = true;
			}
		}	

		if (updatedExperimentJSON.containsKey(Constants.JSON_FIELD_CONTROL_GROUP_VARIANTS) && updatedExperimentJSON.get(Constants.JSON_FIELD_CONTROL_GROUP_VARIANTS) != null) {
			JSONArray updatedControlGroupVariants = updatedExperimentJSON.getJSONArray(Constants.JSON_FIELD_CONTROL_GROUP_VARIANTS);
			if (!Utilities.compareIgnoreOrder(updatedControlGroupVariants, controlGroupVariants)) {
				updateDetails.append(" 'controlGroupVariants' changed from '" + Utilities.StringsListToString(controlGroupVariants) + "' to '" + Arrays.toString(Utilities.jsonArrToStringArr(updatedControlGroupVariants)) + "'\n");
				controlGroupVariants.clear();
				controlGroupVariants.addAll(updatedControlGroupVariants);				
				wasChanged = true;
			}
			
		}
		
		@SuppressWarnings("unchecked")
		Map<String, Variant> variantsDB = (Map<String, Variant>)context.getAttribute(Constants.VARIANTS_DB_PARAM_NAME);
		
		//variants order	
		JSONArray updatedVariantsArray = updatedExperimentJSON.getJSONArray(Constants.JSON_FIELD_VARIANTS); //after validate - i know exists		
		if (orderChanged (updatedVariantsArray, variants)) {
			addAuditMsgForOrderChange(updatedVariantsArray, updateDetails);
			//create new features list - add them one by one and switch with original list
			LinkedList<Variant> newVriants = new LinkedList<Variant>();
			for (int i=0; i<updatedVariantsArray.size(); i++) {					
				String id  = updatedVariantsArray.getJSONObject(i).getString(Constants.JSON_FIELD_UNIQUE_ID);										
				Variant curVariant = variantsDB.get(id);
				newVriants.add(curVariant);
			}				
			variants = newVriants;
			wasChanged = true;
		}	
		
		//variant update (can update variant while updating exp)
		int i=0;
		for (Variant var:variants) {
			String updateVarDetails = var.updateVariant(updatedVariantsArray.getJSONObject(i), context); //i know that the json array and variant list are now at the same order.
			if (!updateVarDetails.isEmpty()) {
				updateDetails.append("In variant" + var.name + ", " + var.getUniqueId().toString() + ": \n" + updateVarDetails);
				wasChanged = true;
			}
			i++;
		}
		
		//close current range
		if (enabled == false && prevEnabled == true) {
			closeExistingExperimentRange(now);
		}
		
		//create new range
		if (enabled == true && prevEnabled == false) {
			addExperimentRange(now);
		}
		
		//moving enabled experiment to prod - close previous range and open a new one 			
		if (stage.equals(Stage.PRODUCTION) && prevStage.equals(Stage.DEVELOPMENT) && enabled == true && prevEnabled == true) {
			closeExistingExperimentRange(now);
			addExperimentRange(now);
			//deletePerviousRangesAndCreateNewRange(now);
		}
		
		if (wasChanged) {
			lastModified = now;
		}
		
		return updateDetails.toString();
	}
	
	private void addAuditMsgForOrderChange(JSONArray updatedVariantsArray, StringBuilder updateDetails) throws JSONException {
		
		updateDetails.append("The order of variants under experiment "  + name +"("+ uniqueId.toString() + ") changed.\n\nBefore:\n");
		for (int i=0; i<variants.size(); i++) {
			updateDetails.append(variants.get(i).getName() + "(" + variants.get(i).getUniqueId().toString() + ")\n");
		}

		updateDetails.append("\n After:\n");
		for (int i=0; i<updatedVariantsArray.size(); i++) {
			JSONObject variant = updatedVariantsArray.getJSONObject(i);
			updateDetails.append(variant.getString(Constants.JSON_FIELD_NAME) + "(" + variant.getString(Constants.JSON_FIELD_UNIQUE_ID) +")\n");
		}

		updateDetails.append( "\n");
	}

	public JSONObject getAvailbaleBranches(ServletContext context) throws JSONException {
		JSONObject res = new JSONObject();
		
		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);					
			
		//map between branch name and the number of seasons (that are in the exp) it is in.
		HashMap<String, Integer> branchesCounter = new HashMap<String, Integer>();
		int numberOfSeasonsInRange = 0;
		
		Product prod = productsDB.get(productId.toString());
		for (Season season:prod.getSeasons()) {			
			if (prod.isSeasonInRange(season, minVersion, maxVersion)) {
				numberOfSeasonsInRange++;
				for (Branch b : season.getBranches().getBranchesList()) {
					if (branchesCounter.containsKey(b.getName())) {
						branchesCounter.put(b.getName(), branchesCounter.get(b.getName()) + 1);
					}
					else {
						branchesCounter.put(b.getName(), 1);
					}
				}
			}
		}
		
		ArrayList<String> availableBranches = new ArrayList<String>(); 
		ArrayList<String> partiallyAvailableBranches = new ArrayList<String>();
		
		Set<String> branchesNames = branchesCounter.keySet();
		for(String branchName:branchesNames) {
			if (branchesCounter.get(branchName) == numberOfSeasonsInRange) {
				availableBranches.add(branchName);
			}
			else {
				partiallyAvailableBranches.add(branchName);
			}
		}
						
		if (numberOfSeasonsInRange>0) {
			availableBranches.add(Constants.MASTER_BRANCH_NAME);
		}

		res.put(Constants.JSON_FIELD_AVALIABLE_IN_ALL_SEASONS, availableBranches);
		res.put(Constants.JSON_FIELD_AVALIABLE_IN_SOME_SEASONS, partiallyAvailableBranches);
		
		return res;
	}
		
	//return intersect of the input sample of seasons that participate in the experiment
	public JSONObject generateInputSample(Stage stage, String minAppVersion, ServletContext context, InputSampleGenerationMode generationMode, double randomizer) throws GenerationException, JSONException {		

		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);					
				
		String faker = (String)context.getAttribute(Constants.SCHEMA_JSON_FAKER_PARAM_NAME);
		String generator = (String)context.getAttribute(Constants.SCHEMA_JSON_GENERATOR_PARAM_NAME);				
		String prune = (String)context.getAttribute(Constants.SCHEMA_JSON_PRUNE_PARAM_NAME);

		Product product = productsDB.get(productId.toString());
		ArrayList<Season> seasonsWithinExpRange = product.getSeasonsWithinRange(minVersion, maxVersion);
		ArrayList<JSONObject> inputSamples = new ArrayList<JSONObject>();
		for (Season s:seasonsWithinExpRange) {
			String inputSampleStr = JsonGenerator.generation(generator, faker, prune, s.getInputSchema().getMergedSchema().toString(), stage.toString(), minAppVersion, generationMode, randomizer); 						
			JSONObject inputSampleJson = new JSONObject(inputSampleStr);
			inputSamples.add(inputSampleJson);
		}

		JSONObject res =  FeatureAttributes.intersectSamples(inputSamples);
		
		if (res == null) { //can happen if no season exists
			res = new JSONObject();
		}
		
		return res;
	}
	
	public JSONObject getUtilitiesInfo(Stage stage, ServletContext context) throws JSONException {
	
		JSONObject res = new JSONObject();
	
		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);					
		
		Product product = productsDB.get(productId.toString());
		ArrayList<Season> seasonsWithinExpRange = product.getSeasonsWithinRange(minVersion, maxVersion);
		
		if (seasonsWithinExpRange.size()>0) {			
			Season season = seasonsWithinExpRange.get(0);
			res = season.getUtilities().getUtilitiesInfo(stage, UtilityType.MAIN_UTILITY);				
		}
		
		return res;
		
	}

	public AirlockAnalytics getAnalyticsUnion(ServletContext context, ArrayList<Season> seasonsInExp) throws MergeException {
		if (seasonsInExp == null || seasonsInExp.size()==0) { 
			AirlockAnalytics emptyAnalytics = new AirlockAnalytics(null);
			return emptyAnalytics;
		}
		
		try {
			Season baseSeason = seasonsInExp.get(0);
			//this is the base analytics (it is a copy of the first season's master analytics)
			AirlockAnalytics mergedAirlockAnalytics = baseSeason.getAnalytics().duplicateForNewSeason(baseSeason.getUniqueId(), null, context, baseSeason.getUniqueId());	
			
			return mergedAirlockAnalytics;
			
		} catch (JSONException je) {
			throw new MergeException("Analytics merge error: " + je.getMessage());
		}
		
		
		
		
	}

	//The maximum quota of all seasons in experiment range
	public int getQuota(ServletContext context) {
		return doGetQuota(context, null, null);				
	}
	
	//The maximum quota of all seasons in experiment range
	public int doGetQuota(ServletContext context, UUID replacedSeasonId, Integer newQuota) {
		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);
		Product product = productsDB.get(productId.toString());
		if (product == null) {
			return Constants.DEFAULT_ANALYTICS_QUOTA; //should not happen
		}
		
		int quota = Constants.DEFAULT_ANALYTICS_QUOTA;
		
		ArrayList<Season> seasons = product.getSeasonsWithinRange(minVersion, maxVersion);
		if (seasons!=null && seasons.size()>0) {
			for (int i=0; i<seasons.size(); i++) {
				int seasonAnaQuota = seasons.get(i).getAnalytics().getAnalyticsQuota();
				if (seasons.get(i).getUniqueId().equals(replacedSeasonId)) {
					seasonAnaQuota = newQuota;
				}
				if (i==0) {
					quota = seasonAnaQuota;
				}
				else {
					if (quota<seasons.get(i).getAnalytics().getAnalyticsQuota()) {
						quota = seasonAnaQuota;
					}
				}
			}		
		}
		
		return quota;	
	}

	private class StageAttributesPair {
		Stage stage;
		TreeSet<String> attributes = new TreeSet<String>();
	}
	
	private class analyticsCounters {
		int developmentCounter = 0;
		int productionCounter = 0;		
	}
	
	//does not include MASTER
	HashMap<String, Stage> getBranchesStageInExperment() {
		HashMap<String, Stage> branchesInExp = new HashMap<String, Stage>();
		for (Variant var:variants) {
			branchesInExp.put(var.getBranchName(), var.getStage());
		}
		return branchesInExp;
	}
	
	//does not include MASTER
	Stage getSingleBranchStageInExperment(String branchName) {
		for (Variant var:variants) {
			if (var.getBranchName().equals(branchName))
				return var.getStage();
		}
		return null;
	}
	
	public JSONObject getExperimentAnalyticsJson(ServletContext context) throws MergeException, JSONException {
		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);
		Product product = productsDB.get(productId.toString());
		
		ArrayList<Season> seasons = product.getSeasonsWithinRange(minVersion, maxVersion);
		if (seasons==null || seasons.size()==0) {
			//return empty analytics data
			Environment env = new Environment();
							
			AirlockAnalytics emptyAnalytics = new AirlockAnalytics(null);
			return emptyAnalytics.getGlobalDataCollection().toDisplayJson(context, null, env, null);			
		}	
		else {
			@SuppressWarnings("unchecked")
			Map<String, BaseAirlockItem> mastersAirlockItemsDB = (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);

			//map between the input field and the maximal stage in all masters/branches
			HashMap<String, Stage> inputFieldsMaxStageMap = new HashMap<String, Stage>();
			
			//map between the feature on/off namespace.name and the maximal stage in all masters/branches
			HashMap<String, Stage> featuresOnOffMaxStageMap = new HashMap<String, Stage>();
			
			//map between the feature namespace.name that reports attributes and a pair of the maximal stage in all masters/branches and its pruned attributes 
			HashMap<String, StageAttributesPair> featuresAttributesStageMap = new HashMap<String, StageAttributesPair>();
			
			analyticsCounters counters = new analyticsCounters();
			
			HashMap<String, Stage> branchesStageInExperment = getBranchesStageInExperment();

			//map between feature name to a list of branches it is found in
			HashMap<String, TreeSet<String>> featuresBranches = new HashMap<String, TreeSet<String>>();
			
			//map between configurationRule and a map between parent and all branches in which this is the parent
			HashMap<String, HashMap<String, ArrayList<String>>> configurationRulesParentInBranches = new HashMap<String, HashMap<String, ArrayList<String>>>();
			
			
			//map between orderingRule and a map between parent and all branches in which this is the parent
			HashMap<String, HashMap<String, ArrayList<String>>> orderingRulesParentInBranches = new HashMap<String, HashMap<String, ArrayList<String>>>();

			int numberOfBranchesInExp = 0;
			
			for (Season season:seasons) {
				//add the season's master analytics (master branch stage is the sam stage as the experiment's)
				addAnalytics(season.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection(), mastersAirlockItemsDB, inputFieldsMaxStageMap, featuresOnOffMaxStageMap, featuresAttributesStageMap, counters, null, stage, featuresBranches, configurationRulesParentInBranches, orderingRulesParentInBranches, season);
				numberOfBranchesInExp++;
				for (Branch branch:season.getBranches().getBranchesList()) {
					if (branchesStageInExperment.containsKey(branch.getName())) {
						Stage branchStageInExp = branchesStageInExperment.get(branch.getName());
					
						Map<String, BaseAirlockItem> branchAirlockItemsDB = Utilities.getAirlockItemsDB (branch.getUniqueId().toString(), context); 
						addAnalytics(branch.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection(), branchAirlockItemsDB, inputFieldsMaxStageMap, featuresOnOffMaxStageMap, featuresAttributesStageMap, counters, branch, branchStageInExp, featuresBranches, configurationRulesParentInBranches, orderingRulesParentInBranches, season);
						numberOfBranchesInExp++;
					}
				}
			}
		
			JSONObject res = buildExperimentAnalyticsJson(inputFieldsMaxStageMap, featuresOnOffMaxStageMap, featuresAttributesStageMap, counters, featuresBranches, configurationRulesParentInBranches, orderingRulesParentInBranches, numberOfBranchesInExp);
			return res;
		}
	

}

	private JSONObject buildExperimentAnalyticsJson(HashMap<String, Stage> inputFieldsMaxStageMap,
			HashMap<String, Stage> featuresOnOffMaxStageMap,
			HashMap<String, StageAttributesPair> featuresAttributesStageMap, analyticsCounters counters,
			HashMap<String, TreeSet<String>> featuresBranches, 
			HashMap<String, HashMap<String, ArrayList<String>>> configurationRulesParentInBranchs,
			HashMap<String, HashMap<String, ArrayList<String>>> orderingRulesParentInBranches,
			int numberOfBranchesInExp) throws JSONException {
		JSONObject res = new  JSONObject();
		
		//input fields
		JSONArray inputFieldsArray = new JSONArray();
		Set<String> inputFields = inputFieldsMaxStageMap.keySet();
		for (String inputField:inputFields) {
			JSONObject inputFieldJSON = new JSONObject();
			inputFieldJSON.put(Constants.JSON_FIELD_NAME, inputField);			
			inputFieldsArray.add(inputFieldJSON);
		}
		res.put(Constants.JSON_FIELD_INPUT_FIELDS_FOR_ANALYTICS, inputFieldsArray);
		
		JSONArray analyticsByFeatureNamesArr = new JSONArray(); 
		//map between feature namespace.name and its data
		HashMap <String, JSONObject> featuresDataMap = new HashMap<String, JSONObject>();
		//first iterate all the features  (not configurationRules) in featuresOnOff
		Set<String> featuresOnOffNames = featuresOnOffMaxStageMap.keySet();
		for (String featuresOnOffName : featuresOnOffNames) {
			if (!featuresBranches.containsKey(featuresOnOffName)) {
				continue; //all features are kept in the featuresBranches map, if it is not there it is configuration rule. 
			}
			JSONObject featureData = new JSONObject();
			featureData.put(Constants.JSON_FIELD_NAME, featuresOnOffName);
			featureData.put(Constants.JSON_FIELD_SEND_TO_ANALYTICS, true);
			if (featuresBranches.get(featuresOnOffName).size() < numberOfBranchesInExp) {				
				featureData.put(Constants.JSON_FIELD_BRANCHES, buildBranchesArray(featuresBranches.get(featuresOnOffName)));
			}
			if (featuresAttributesStageMap.containsKey(featuresOnOffName)) {
				featureData.put(Constants.JSON_FIELD_ATTRIBUTES, featuresAttributesStageMap.get(featuresOnOffName).attributes);
			}	
			analyticsByFeatureNamesArr.add(featureData);
			featuresDataMap.put(featuresOnOffName, featureData);
		}
			
		//second iteration - look for feature that has attribute but is not sent to analytics
		Set<String> featuresAttsNames = featuresAttributesStageMap.keySet();
		for (String featuresAttName : featuresAttsNames) {
			if (!featuresDataMap.containsKey(featuresAttName)) {
				JSONObject featureData = new JSONObject();
				featureData.put(Constants.JSON_FIELD_NAME, featuresAttName);
				featureData.put(Constants.JSON_FIELD_SEND_TO_ANALYTICS, false);
				if (featuresBranches.get(featuresAttName).size() < numberOfBranchesInExp) {
					featureData.put(Constants.JSON_FIELD_BRANCHES, buildBranchesArray(featuresBranches.get(featuresAttName)));
				}
				featureData.put(Constants.JSON_FIELD_ATTRIBUTES, featuresAttributesStageMap.get(featuresAttName).attributes);
				analyticsByFeatureNamesArr.add(featureData);
				featuresDataMap.put(featuresAttName, featureData);
			}
		}

		
		//third iterate all the configurationRules (not features) in featuresOnOff
		Set<String> configRulesNames = configurationRulesParentInBranchs.keySet();
		for (String configRuleName:configRulesNames) {
			HashMap<String, ArrayList<String>> crParentBranches = configurationRulesParentInBranchs.get(configRuleName);
			Set<String> parentNames = crParentBranches.keySet();
			for (String parentName:parentNames) {
				if (!featuresDataMap.containsKey(parentName)) {
					JSONObject featureData = new JSONObject();

					featureData.put(Constants.JSON_FIELD_NAME, parentName);
					featureData.put(Constants.JSON_FIELD_SEND_TO_ANALYTICS, false);
					JSONArray configRules = new JSONArray();
					JSONObject configRuleJSON = new JSONObject();
					configRuleJSON.put(Constants.JSON_FIELD_NAME, configRuleName);
					if (crParentBranches.get(parentName).size()<numberOfBranchesInExp) {
						configRuleJSON.put(Constants.JSON_FIELD_BRANCHES, buildBranchesArray(crParentBranches.get(parentName)));
					}
					configRules.add(configRuleJSON);
					featureData.put(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES, configRules);
					analyticsByFeatureNamesArr.add(featureData);
					featuresDataMap.put(parentName, featureData);
				}
				else {
					//add configRule to an existing feature data	
					JSONObject featureData = featuresDataMap.get(parentName);
					if (featureData.containsKey(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES)) {
						JSONArray configRules  = featureData.getJSONArray(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES);
						JSONObject configRuleJSON = new JSONObject();
						configRuleJSON.put(Constants.JSON_FIELD_NAME, configRuleName);
						if (crParentBranches.get(parentName).size()<numberOfBranchesInExp) {
							configRuleJSON.put(Constants.JSON_FIELD_BRANCHES, buildBranchesArray(crParentBranches.get(parentName)));
						}
						configRules.add(configRuleJSON);
						featureData.put(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES, configRules);
					}
					else {
						JSONArray configRules = new JSONArray();
						JSONObject configRuleJSON = new JSONObject();
						configRuleJSON.put(Constants.JSON_FIELD_NAME, configRuleName);
						if (crParentBranches.get(parentName).size()<numberOfBranchesInExp) {
							configRuleJSON.put(Constants.JSON_FIELD_BRANCHES, buildBranchesArray(crParentBranches.get(parentName)));
						}
						
						configRules.add(configRuleJSON);
						featureData.put(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES, configRules);
					}
				}
			}
 		}
		
		//forth iterate all the orderingRules (not features) in featuresOnOff
		Set<String> orderingRulesNames = orderingRulesParentInBranches.keySet();
		for (String orderingRuleName:orderingRulesNames) {
			HashMap<String, ArrayList<String>> orParentBranches = orderingRulesParentInBranches.get(orderingRuleName);
			Set<String> parentNames = orParentBranches.keySet();
			for (String parentName:parentNames) {
				if (!featuresDataMap.containsKey(parentName)) {
					JSONObject featureData = new JSONObject();

					featureData.put(Constants.JSON_FIELD_NAME, parentName);
					featureData.put(Constants.JSON_FIELD_SEND_TO_ANALYTICS, false);
					JSONArray orderingRules = new JSONArray();
					JSONObject orderingRuleJSON = new JSONObject();
					orderingRuleJSON.put(Constants.JSON_FIELD_NAME, orderingRuleName);
					if (orParentBranches.get(parentName).size()<numberOfBranchesInExp) {
						orderingRuleJSON.put(Constants.JSON_FIELD_BRANCHES, buildBranchesArray(orParentBranches.get(parentName)));
					}
					orderingRules.add(orderingRuleJSON);
					featureData.put(Constants.JSON_FEATURE_FIELD_ORDERING_RULES, orderingRules);
					analyticsByFeatureNamesArr.add(featureData);
					featuresDataMap.put(parentName, featureData);
				}
				else {
					//add configRule to an existing feature data	
					JSONObject featureData = featuresDataMap.get(parentName);
					if (featureData.containsKey(Constants.JSON_FEATURE_FIELD_ORDERING_RULES)) {
						JSONArray orderingRules  = featureData.getJSONArray(Constants.JSON_FEATURE_FIELD_ORDERING_RULES);
						JSONObject orderingRuleJSON = new JSONObject();
						orderingRuleJSON.put(Constants.JSON_FIELD_NAME, orderingRuleName);
						if (orParentBranches.get(parentName).size()<numberOfBranchesInExp) {
							orderingRuleJSON.put(Constants.JSON_FIELD_BRANCHES, buildBranchesArray(orParentBranches.get(parentName)));
						}
						orderingRules.add(orderingRuleJSON);
						featureData.put(Constants.JSON_FEATURE_FIELD_ORDERING_RULES, orderingRules);
					}
					else {
						JSONArray orderingRules = new JSONArray();
						JSONObject orderingRuleJSON = new JSONObject();
						orderingRuleJSON.put(Constants.JSON_FIELD_NAME, orderingRuleName);
						if (orParentBranches.get(parentName).size()<numberOfBranchesInExp) {
							orderingRuleJSON.put(Constants.JSON_FIELD_BRANCHES, buildBranchesArray(orParentBranches.get(parentName)));
						}
						
						orderingRules.add(orderingRuleJSON);
						featureData.put(Constants.JSON_FEATURE_FIELD_ORDERING_RULES, orderingRules);
					}
				}
			}
 		}

		res.put(Constants.JSON_FIELD_ANALYTICS_BY_FEATURE_NAMES, analyticsByFeatureNamesArr);
		res.put(Constants.JSON_FIELD_DEVELOPMENT_ANALYTICS_COUNT, counters.developmentCounter);
		res.put(Constants.JSON_FIELD_PRODUCTION_ANALYTICS_COUNT, counters.productionCounter);
		return res;
	}


	private JSONArray buildBranchesArray(Collection<String> branches) throws JSONException {
		JSONArray res = new JSONArray();
		for (String seasonBranch: branches) {
			JSONObject branchObj = new JSONObject();
			String[] branchSearonPair = seasonBranch.split(";");
			branchObj.put(Constants.JSON_FIELD_SEASON, branchSearonPair[0]);
			branchObj.put(Constants.JSON_FIELD_BRANCH, branchSearonPair[1]);			
			res.add(branchObj);
		}
		
		return res;
	}

	private void addAnalytics(AnalyticsDataCollection analyticsDataCollection,
			Map<String, BaseAirlockItem> airlockItemsDB, HashMap<String, Stage> inputFieldsMaxStageMap,
			HashMap<String, Stage> featuresOnOffMaxStageMap,
			HashMap<String, StageAttributesPair> featuresAttributesStageMap, analyticsCounters counters, Branch branch,
			Stage branchStage, HashMap<String, TreeSet<String>> featuresBranches, HashMap<String, 
			HashMap<String, ArrayList<String>>> ConfigurationRulesParentInBranchs, 
			HashMap<String, HashMap<String, ArrayList<String>>> orderingRulesParentInBranches,
			Season season) {
		
		Boolean enforceDevelopment = false;
		if (branchStage!=null && branchStage.equals(Stage.DEVELOPMENT))
			enforceDevelopment=true;
		
		String branchName = (branch == null?Constants.MASTER_BRANCH_NAME:branch.getName());
		String seasonBranchName = null;
		if (season.getMaxVersion() == null) {
			seasonBranchName = season.getMinVersion()  + " and up;" + branchName;
		} else {
			seasonBranchName = season.getMinVersion()  + " to " + season.getMaxVersion() + ";" + branchName;
		}		
		
		//add features on/off
		for (String featureOnOffToAdd : analyticsDataCollection.getFeaturesOnOff()) {
			BaseAirlockItem item = airlockItemsDB.get(featureOnOffToAdd);
			String itemNamespaceDotName  = item.getNameSpaceDotName();
			
			if (ConfigurationRulesParentInBranchs!=null && featuresBranches!=null && orderingRulesParentInBranches!=null) {
				addItemToBranchesStructurMaps(item, ConfigurationRulesParentInBranchs, orderingRulesParentInBranches, featuresBranches, airlockItemsDB, seasonBranchName, itemNamespaceDotName);
			}
			
			boolean featureContainingOrderingRules = false;
			if (item instanceof FeatureItem) {
				FeatureItem fi = (FeatureItem)item;
				if (fi.getOrderingRuleItems()!=null && fi.getOrderingRuleItems().size()>0) {
					featureContainingOrderingRules = true; //if feature is reported to analytics and has ordering rules another event is sent (the order of its sub features)
				}							
			}
			if (!featuresOnOffMaxStageMap.containsKey(itemNamespaceDotName)) {
				boolean isProduction = BaseAirlockItem.isProductionFeature(item, airlockItemsDB) && !enforceDevelopment;
				featuresOnOffMaxStageMap.put(itemNamespaceDotName, isProduction?Stage.PRODUCTION:Stage.DEVELOPMENT);
				if (isProduction) {
					counters.productionCounter++;
					if (featureContainingOrderingRules)
						counters.productionCounter++;
				}
				counters.developmentCounter++;		
				if (featureContainingOrderingRules)
					counters.developmentCounter++;
			}
			else { //already listed - check if stage changed from dev to prod
				if (featuresOnOffMaxStageMap.get(itemNamespaceDotName).equals(Stage.DEVELOPMENT)) { //if equals production no need to calc and change, it is the max already
					boolean isProduction = BaseAirlockItem.isProductionFeature(item, airlockItemsDB) && !enforceDevelopment;
					//change from dev to prod
					if (isProduction) {
						featuresOnOffMaxStageMap.put(itemNamespaceDotName, Stage.PRODUCTION);
						counters.productionCounter++;
						if (featureContainingOrderingRules)
							counters.productionCounter++; 
					}
				}
			}			
		}
		
		//add input fields
		for (String inputFieldfToAdd : analyticsDataCollection.getInputFieldsForAnalytics()) {
			if (!inputFieldsMaxStageMap.containsKey(inputFieldfToAdd)) {
				boolean isProduction = analyticsDataCollection.getInputFieldsForAnalyticsStageMap().get(inputFieldfToAdd)!=null &&
									   analyticsDataCollection.getInputFieldsForAnalyticsStageMap().get(inputFieldfToAdd).equals(Stage.PRODUCTION)  && 
									   !enforceDevelopment;
				inputFieldsMaxStageMap.put(inputFieldfToAdd, isProduction?Stage.PRODUCTION:Stage.DEVELOPMENT);
				if (isProduction) {
					counters.productionCounter++;
				}
				counters.developmentCounter++;				
			}
			else { //already listed - check if stage changed from dev to prod
				if (inputFieldsMaxStageMap.get(inputFieldfToAdd).equals(Stage.DEVELOPMENT)) { //if equals production no need to calc and change, it is the max already
					boolean isProduction = analyticsDataCollection.getInputFieldsForAnalyticsStageMap().get(inputFieldfToAdd).equals(Stage.PRODUCTION)&& !enforceDevelopment;
					
					//change from dev to prod
					if (isProduction) {
						inputFieldsMaxStageMap.put(inputFieldfToAdd, Stage.PRODUCTION);						
						counters.productionCounter++;
					}	
				}
			}			
		}
		
		//add feature attributes
		LinkedList<FeatureAttributesPair> featureAttributesListToAdd = analyticsDataCollection.getFeaturesConfigurationAttributesList();
		if (featureAttributesListToAdd!=null && featureAttributesListToAdd.size()>0) {
			for (FeatureAttributesPair featureAttsToAdd:featureAttributesListToAdd) {
				BaseAirlockItem feature = airlockItemsDB.get(featureAttsToAdd.id);
				String featureNamespaceDotName  = feature.getNameSpaceDotName();
				
				if (ConfigurationRulesParentInBranchs!=null && featuresBranches!=null && orderingRulesParentInBranches!=null) {
					addItemToBranchesStructurMaps(feature, ConfigurationRulesParentInBranchs, orderingRulesParentInBranches, featuresBranches, airlockItemsDB, seasonBranchName, featureNamespaceDotName);
				}
				
				if (!featuresAttributesStageMap.containsKey(featureNamespaceDotName)) {
					boolean isProduction = BaseAirlockItem.isProductionFeature(feature, airlockItemsDB) && !enforceDevelopment;
					StageAttributesPair stageAttsPair = new StageAttributesPair();
					stageAttsPair.stage = isProduction?Stage.PRODUCTION:Stage.DEVELOPMENT;
					stageAttsPair.attributes.addAll(analyticsDataCollection.getFeaturesPrunedConfigurationAttributesMap().get(featureAttsToAdd.id));
					featuresAttributesStageMap.put(featureNamespaceDotName, stageAttsPair);
					if (isProduction) {
						counters.productionCounter=counters.productionCounter+stageAttsPair.attributes.size();
					}
					counters.developmentCounter=counters.developmentCounter+stageAttsPair.attributes.size();				
				}
				else {
					StageAttributesPair stageAttsPair =  featuresAttributesStageMap.get(featureNamespaceDotName);
					int prevNumOfAtt = stageAttsPair.attributes.size();
					stageAttsPair.attributes.addAll(analyticsDataCollection.getFeaturesPrunedConfigurationAttributesMap().get(featureAttsToAdd.id));
					int currentNumOfAtt = stageAttsPair.attributes.size();
										
					Stage prevSatge = stageAttsPair.stage;
					if (prevSatge.equals(Stage.DEVELOPMENT)) {
						boolean isProduction = BaseAirlockItem.isProductionFeature(feature, airlockItemsDB) && !enforceDevelopment;
						stageAttsPair.stage = isProduction?Stage.PRODUCTION:Stage.DEVELOPMENT;	
					}
					
					int numOfAddedAttributes = currentNumOfAtt-prevNumOfAtt;
					counters.developmentCounter = counters.developmentCounter + numOfAddedAttributes;
					
					if (stageAttsPair.stage.equals(Stage.PRODUCTION) && !enforceDevelopment) { 
						if (prevSatge.equals(Stage.DEVELOPMENT)) { //if changed from dev to prod add all att count
							counters.productionCounter = counters.productionCounter + currentNumOfAtt;						
						}
						else {
							//was and is prod  - only add delta
							counters.productionCounter = counters.productionCounter + numOfAddedAttributes;
						}
					}
				}
			}
		}		
	}

	//build map that contains feature location in different branches for experiment analytics display
	private void addItemToBranchesStructurMaps(BaseAirlockItem item,
			HashMap<String, HashMap<String, ArrayList<String>>> configurationRulesParentInBranch,
			HashMap<String, HashMap<String, ArrayList<String>>> orderingRulesParentInBranches,
			HashMap<String, TreeSet<String>> featuresBranches, Map<String, BaseAirlockItem> airlockItemsDB, String branchName, String itemNamespaceDotName) {
		if (item instanceof FeatureItem) {
			if (!featuresBranches.containsKey(itemNamespaceDotName)) {
				TreeSet<String> featureBranchesList = new TreeSet<String> ();
				featureBranchesList.add(branchName);
				featuresBranches.put(itemNamespaceDotName, featureBranchesList);
			}
			else { 
				TreeSet<String> featureBranchesList = featuresBranches.get(itemNamespaceDotName);
				featureBranchesList.add(branchName);
			}
		}
		else if (item instanceof OrderingRuleItem) {
			//parent can be either feature of mtx
			BaseAirlockItem parentFeature = ((OrderingRuleItem)item).getParentFeatureOrMTX(airlockItemsDB);
			
			if (!orderingRulesParentInBranches.containsKey(itemNamespaceDotName)) {
				HashMap<String, ArrayList<String>> orBranchesParentsMap = new HashMap<String, ArrayList<String>>();
				ArrayList<String> parentBranches = new ArrayList<String>();
				parentBranches.add(branchName);				
				orBranchesParentsMap.put(Branch.getItemBranchName(parentFeature), parentBranches);
				orderingRulesParentInBranches.put(itemNamespaceDotName, orBranchesParentsMap);					
			}
			else {
				HashMap<String, ArrayList<String>> orBranchesParentsMap = orderingRulesParentInBranches.get(itemNamespaceDotName);
				String parentName = Branch.getItemBranchName(parentFeature);
				if (orBranchesParentsMap.containsKey(parentName)) {
					orBranchesParentsMap.get(parentName).add(branchName);
				}
				else {
					ArrayList<String> parentBranches = new ArrayList<String>();
					parentBranches.add(branchName);
					orBranchesParentsMap.put(parentName, parentBranches);
				}
			}
		}
		else if (item instanceof ConfigurationRuleItem) {
			FeatureItem parentFeature = ((ConfigurationRuleItem)item).getParentFeature(airlockItemsDB);
			
			if (!configurationRulesParentInBranch.containsKey(itemNamespaceDotName)) {
				HashMap<String, ArrayList<String>> crBranchesParentsMap = new HashMap<String, ArrayList<String>>();
				ArrayList<String> parentBranches = new ArrayList<String>();
				parentBranches.add(branchName);
				crBranchesParentsMap.put(parentFeature.getNameSpaceDotName(), parentBranches);
				configurationRulesParentInBranch.put(itemNamespaceDotName, crBranchesParentsMap);					
			}
			else {
				HashMap<String, ArrayList<String>> crBranchesParentsMap = configurationRulesParentInBranch.get(itemNamespaceDotName);
				String parentName = parentFeature.getNameSpaceDotName();
				if (crBranchesParentsMap.containsKey(parentName)) {
					crBranchesParentsMap.get(parentFeature.getNameSpaceDotName()).add(branchName);
				}
				else {
					ArrayList<String> parentBranches = new ArrayList<String>();
					parentBranches.add(branchName);
					crBranchesParentsMap.put(parentName, parentBranches);
				}
			}
		}
		
	}

	public int simulateProdCounterUponFeatureMoveToProd(String featureNamespaceDotName, ServletContext context) throws MergeException {
		if (stage.equals(Stage.DEVELOPMENT))
			return 0;
		
		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);
		Product product = productsDB.get(productId.toString());
		
		ArrayList<Season> seasons = product.getSeasonsWithinRange(minVersion, maxVersion);
		if (seasons==null || seasons.size()==0) {
			return 0;			
		}	
		@SuppressWarnings("unchecked")
		Map<String, BaseAirlockItem> mastersAirlockItemsDB = (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);

		//map between the input field and the maximal stage in all masters/branches
		HashMap<String, Stage> inputFieldsMaxStageMap = new HashMap<String, Stage>();
		
		//map between the feature on/off namespace.name and the maximal stage in all masters/branches
		HashMap<String, Stage> featuresOnOffMaxStageMap = new HashMap<String, Stage>();
		
		//map between the feature namespace.name that reports attributes and a pair of the maximal stage in all masters/branches and its pruned attributes 
		HashMap<String, StageAttributesPair> featuresAttributesStageMap = new HashMap<String, StageAttributesPair>();
		
		analyticsCounters counters = new analyticsCounters();
		
		HashMap<String, Stage> branchesStageInExperment = getBranchesStageInExperment();

		//map between feature name to a list of branches it is found in
		HashMap<String, TreeSet<String>> featuresBranches = new HashMap<String, TreeSet<String>>();
		
		//map between configurationRule and a map between parent and all branches in which this is the parent
		HashMap<String, HashMap<String, ArrayList<String>>> configurationRulesParentInBranches = new HashMap<String, HashMap<String, ArrayList<String>>>();

		//map between configurationRule and a map between parent and all branches in which this is the parent
		HashMap<String, HashMap<String, ArrayList<String>>> orderingRulesParentInBranches = new HashMap<String, HashMap<String, ArrayList<String>>>();
	
		for (Season season:seasons) {
			//add the season's master analytics (master branch stage is the sam stage as the experiment's)
			addAnalytics(season.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection(), mastersAirlockItemsDB, inputFieldsMaxStageMap, featuresOnOffMaxStageMap, featuresAttributesStageMap, counters, null, stage, featuresBranches, configurationRulesParentInBranches, orderingRulesParentInBranches, season);
			for (Branch branch:season.getBranches().getBranchesList()) {
				if (!isBranchAssociatedToVariantInProd(branch))
					continue;
				if (branchesStageInExperment.containsKey(branch.getName())) {
					Stage branchStageInExp = branchesStageInExperment.get(branch.getName());
				
					Map<String, BaseAirlockItem> branchAirlockItemsDB = Utilities.getAirlockItemsDB (branch.getUniqueId().toString(), context); 
					addAnalytics(branch.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection(), branchAirlockItemsDB, inputFieldsMaxStageMap, featuresOnOffMaxStageMap, featuresAttributesStageMap, counters, branch, branchStageInExp, featuresBranches, configurationRulesParentInBranches, orderingRulesParentInBranches, season);
				}
			}
		}
		
		if (featuresOnOffMaxStageMap.containsKey(featureNamespaceDotName) && featuresOnOffMaxStageMap.get(featureNamespaceDotName).equals(Stage.DEVELOPMENT)) {
			counters.productionCounter++;
		}

		if (featuresAttributesStageMap.containsKey(featureNamespaceDotName) && featuresAttributesStageMap.get(featureNamespaceDotName).stage.equals(Stage.DEVELOPMENT)) {
			counters.productionCounter = counters.productionCounter + featuresAttributesStageMap.get(featureNamespaceDotName).attributes.size();
		}
		
		return counters.productionCounter;
	}

	//if replaced season exists - use the replacedAnalytics instead of the season analytics
	//if replaced branch exists - use the replacedAnalytics instead of the branch analytics
	public int getAnalyticsProductionCounter(ServletContext context, String replacedSeasonId, String replacedBranchId, AnalyticsDataCollection replacedAnalytics) throws MergeException {
		if (stage.equals(Stage.DEVELOPMENT))
			return 0;
		
		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);
		Product product = productsDB.get(productId.toString());
		
		ArrayList<Season> seasons = product.getSeasonsWithinRange(minVersion, maxVersion);
		if (seasons==null || seasons.size()==0) {
			return 0;			
		}	
		@SuppressWarnings("unchecked")
		Map<String, BaseAirlockItem> mastersAirlockItemsDB = (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);

		//map between the input field and the maximal stage in all masters/branches
		HashMap<String, Stage> inputFieldsMaxStageMap = new HashMap<String, Stage>();
		
		//map between the feature on/off namespace.name and the maximal stage in all masters/branches
		HashMap<String, Stage> featuresOnOffMaxStageMap = new HashMap<String, Stage>();
		
		//map between the feature namespace.name that reports attributes and a pair of the maximal stage in all masters/branches and its pruned attributes 
		HashMap<String, StageAttributesPair> featuresAttributesStageMap = new HashMap<String, StageAttributesPair>();
		
		analyticsCounters counters = new analyticsCounters();
		
		HashMap<String, Stage> branchesStageInExperment = getBranchesStageInExperment();

		//map between feature name to a list of branches it is found in
		HashMap<String, TreeSet<String>> featuresBranches = new HashMap<String, TreeSet<String>>();
		
		//map between configurationRule and a map between parent and all branches in which this is the parent
		HashMap<String, HashMap<String, ArrayList<String>>> configurationRulesParentInBranches = new HashMap<String, HashMap<String, ArrayList<String>>>();
		
		//map between configurationRule and a map between parent and all branches in which this is the parent
		HashMap<String, HashMap<String, ArrayList<String>>> orderingRulesParentInBranches = new HashMap<String, HashMap<String, ArrayList<String>>>();
		
		for (Season season:seasons) {
			//add the season's master analytics (master branch stage is the same stage as the experiment's)
			AnalyticsDataCollection anaDataCollection = null;
			if (season.getUniqueId().equals(replacedSeasonId)) {
				anaDataCollection = replacedAnalytics;
			}
			else {
				anaDataCollection = season.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection();
			}
			addAnalytics(anaDataCollection, mastersAirlockItemsDB, inputFieldsMaxStageMap, featuresOnOffMaxStageMap, featuresAttributesStageMap, counters, null, stage, featuresBranches, configurationRulesParentInBranches, orderingRulesParentInBranches, season);
			for (Branch branch:season.getBranches().getBranchesList()) {
				if (!isBranchAssociatedToVariantInProd(branch))
					continue;
				if (branchesStageInExperment.containsKey(branch.getName())) {
					Stage branchStageInExp = branchesStageInExperment.get(branch.getName());
				
					Map<String, BaseAirlockItem> branchAirlockItemsDB = Utilities.getAirlockItemsDB (branch.getUniqueId().toString(), context);
					
					AnalyticsDataCollection branchAnaDataCollection = null;
					if (season.getUniqueId().equals(replacedSeasonId)) {
						branchAnaDataCollection = replacedAnalytics;
					}
					else {
						branchAnaDataCollection = branch.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection();
					}
					
					addAnalytics(branchAnaDataCollection, branchAirlockItemsDB, inputFieldsMaxStageMap, featuresOnOffMaxStageMap, featuresAttributesStageMap, counters, branch, branchStageInExp, featuresBranches, configurationRulesParentInBranches, orderingRulesParentInBranches, season);
				}
			}
		}
				
		return counters.productionCounter;
	}
	
	
	public int simulateProdCounterUponNewInputSchema(Map<String,String> newInputFieldsMap, ServletContext context) throws MergeException {
		if (stage.equals(Stage.DEVELOPMENT))
			return 0;
		
		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);
		Product product = productsDB.get(productId.toString());
		
		ArrayList<Season> seasons = product.getSeasonsWithinRange(minVersion, maxVersion);
		if (seasons==null || seasons.size()==0) {
			return 0;			
		}	
		@SuppressWarnings("unchecked")
		Map<String, BaseAirlockItem> mastersAirlockItemsDB = (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);

		//map between the input field and the maximal stage in all masters/branches
		HashMap<String, Stage> inputFieldsMaxStageMap = new HashMap<String, Stage>();
		
		//map between the feature on/off namespace.name and the maximal stage in all masters/branches
		HashMap<String, Stage> featuresOnOffMaxStageMap = new HashMap<String, Stage>();
		
		//map between the feature namespace.name that reports attributes and a pair of the maximal stage in all masters/branches and its pruned attributes 
		HashMap<String, StageAttributesPair> featuresAttributesStageMap = new HashMap<String, StageAttributesPair>();
		
		analyticsCounters counters = new analyticsCounters();
		
		HashMap<String, Stage> branchesStageInExperment = getBranchesStageInExperment();

		//map between feature name to a list of branches it is found in
		HashMap<String, TreeSet<String>> featuresBranches = new HashMap<String, TreeSet<String>>();
		
		//map between configurationRule and a map between parent and all barcnhes in which this is the parent
		HashMap<String, HashMap<String, ArrayList<String>>> configurationRulesParentInBranches = new HashMap<String, HashMap<String, ArrayList<String>>>();
		
		//map between configurationRule and a map between parent and all branches in which this is the parent
		HashMap<String, HashMap<String, ArrayList<String>>> orderingRulesParentInBranches = new HashMap<String, HashMap<String, ArrayList<String>>>();
		
		for (Season season:seasons) {
			//add the season's master analytics (master branch stage is the sam stage as the experiment's)
			addAnalytics(season.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection(), mastersAirlockItemsDB, inputFieldsMaxStageMap, featuresOnOffMaxStageMap, featuresAttributesStageMap, counters, null, stage, featuresBranches, configurationRulesParentInBranches, orderingRulesParentInBranches, season);
			for (Branch branch:season.getBranches().getBranchesList()) {
				if (!isBranchAssociatedToVariantInProd(branch))
					continue;
				if (branchesStageInExperment.containsKey(branch.getName())) {
					Stage branchStageInExp = branchesStageInExperment.get(branch.getName());
				
					Map<String, BaseAirlockItem> branchAirlockItemsDB = Utilities.getAirlockItemsDB (branch.getUniqueId().toString(), context); 
					addAnalytics(branch.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection(), branchAirlockItemsDB, inputFieldsMaxStageMap, featuresOnOffMaxStageMap, featuresAttributesStageMap, counters, branch, branchStageInExp, featuresBranches, configurationRulesParentInBranches, orderingRulesParentInBranches, season);
				}
			}
		}
		
		int deletedInputFieldsCount = 0;
		int movedToProdInputFieldsCount = 0;
		int movedToDevInputFieldsCount = 0;
		
		Set<String> inputFieldsInExpAnalytics = inputFieldsMaxStageMap.keySet(); 
		for (String inputField:inputFieldsInExpAnalytics) {
			if (!newInputFieldsMap.containsKey(inputField)) {
				deletedInputFieldsCount++;
			} else {
				if (newInputFieldsMap.get(inputField).equals(Stage.DEVELOPMENT) && inputFieldsMaxStageMap.get(inputField).equals(Stage.PRODUCTION)) {
					movedToDevInputFieldsCount++;
				}
				else if (newInputFieldsMap.get(inputField).equals(Stage.PRODUCTION.toString()) && inputFieldsMaxStageMap.get(inputField).equals(Stage.DEVELOPMENT)) {
					movedToProdInputFieldsCount++;									
				}
			}				
		}
		
		counters.productionCounter = counters.productionCounter - deletedInputFieldsCount - movedToDevInputFieldsCount + movedToProdInputFieldsCount;
		return counters.productionCounter;
	}
	
	public int simulateProdCounterUponUpdateInputFields(Map<String,String> inputFieldsMap, JSONArray inputFieldToAnalyticsArray, ServletContext context) throws MergeException {
		if (stage.equals(Stage.DEVELOPMENT))
			return 0;
		
		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);
		Product product = productsDB.get(productId.toString());
		
		ArrayList<Season> seasons = product.getSeasonsWithinRange(minVersion, maxVersion);
		if (seasons==null || seasons.size()==0) {
			return 0;			
		}	
		@SuppressWarnings("unchecked")
		Map<String, BaseAirlockItem> mastersAirlockItemsDB = (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);

		//map between the input field and the maximal stage in all masters/branches
		HashMap<String, Stage> inputFieldsMaxStageMap = new HashMap<String, Stage>();
		
		//map between the feature on/off namespace.name and the maximal stage in all masters/branches
		HashMap<String, Stage> featuresOnOffMaxStageMap = new HashMap<String, Stage>();
		
		//map between the feature namespace.name that reports attributes and a pair of the maximal stage in all masters/branches and its pruned attributes 
		HashMap<String, StageAttributesPair> featuresAttributesStageMap = new HashMap<String, StageAttributesPair>();
		
		analyticsCounters counters = new analyticsCounters();
		
		HashMap<String, Stage> branchesStageInExperment = getBranchesStageInExperment();

		//map between feature name to a list of branches it is found in
		HashMap<String, TreeSet<String>> featuresBranches = new HashMap<String, TreeSet<String>>();
		
		//map between configurationRule and a map between parent and all barcnhes in which this is the parent
		HashMap<String, HashMap<String, ArrayList<String>>> configurationRulesParentInBranches = new HashMap<String, HashMap<String, ArrayList<String>>>();
		
		//map between configurationRule and a map between parent and all branches in which this is the parent
		HashMap<String, HashMap<String, ArrayList<String>>> orderingRulesParentInBranches = new HashMap<String, HashMap<String, ArrayList<String>>>();
		
		for (Season season:seasons) {
			//add the season's master analytics (master branch stage is the sam stage as the experiment's)
			addAnalytics(season.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection(), mastersAirlockItemsDB, inputFieldsMaxStageMap, featuresOnOffMaxStageMap, featuresAttributesStageMap, counters, null, stage, featuresBranches, configurationRulesParentInBranches, orderingRulesParentInBranches, season);
			for (Branch branch:season.getBranches().getBranchesList()) {
				if (!isBranchAssociatedToVariantInProd(branch))
					continue;
				if (branchesStageInExperment.containsKey(branch.getName())) {
					Stage branchStageInExp = branchesStageInExperment.get(branch.getName());
				
					Map<String, BaseAirlockItem> branchAirlockItemsDB = Utilities.getAirlockItemsDB (branch.getUniqueId().toString(), context); 
					addAnalytics(branch.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection(), branchAirlockItemsDB, inputFieldsMaxStageMap, featuresOnOffMaxStageMap, featuresAttributesStageMap, counters, branch, branchStageInExp, featuresBranches, configurationRulesParentInBranches, orderingRulesParentInBranches, season);
				}
			}
		}
		
		for (int i=0; i<inputFieldToAnalyticsArray.size(); i++) {
			if (!inputFieldsMaxStageMap.containsKey(inputFieldToAnalyticsArray.get(i))) {
				//exists in array not in map - added
				String fieldStageStr = inputFieldsMap.get(inputFieldToAnalyticsArray.get(i));
				if (fieldStageStr.equals(Stage.PRODUCTION.toString())) {
					counters.productionCounter++;
				}
			}
			else {
				if (inputFieldsMaxStageMap.containsKey(inputFieldToAnalyticsArray.get(i))) {
					//exists in array and in map - if stage moved from dev to prod - increase prod counter
					Stage currentExpMaxStage = inputFieldsMaxStageMap.get(inputFieldToAnalyticsArray.get(i));
					String fieldStageStr = inputFieldsMap.get(inputFieldToAnalyticsArray.get(i));
					if (currentExpMaxStage.equals(Stage.DEVELOPMENT) && fieldStageStr.equals(Stage.PRODUCTION.toString())) {
						counters.productionCounter++;
					}
				}
			}
		}
		
		Set<String> existingInputFields = inputFieldsMaxStageMap.keySet();
		TreeSet<String> updatedList = new TreeSet<String>(inputFieldToAnalyticsArray);
		for (String existingInputField:existingInputFields) {
			if (!updatedList.contains(existingInputField)) {
				if (inputFieldsMaxStageMap.get(existingInputField).equals(Stage.PRODUCTION)) {
					//a field in production stage was removed from analytics
					counters.productionCounter--;
				}
			}
		}
		return counters.productionCounter;
	}
	
	
	public int simulateProdCounterUponUpdateAttributesListForAnalytics(FeatureItem feature, JSONArray prunedAttArray, ServletContext context) throws MergeException {
		if (stage.equals(Stage.DEVELOPMENT))
			return 0;
				
		
		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);
		Product product = productsDB.get(productId.toString());
		
		ArrayList<Season> seasons = product.getSeasonsWithinRange(minVersion, maxVersion);
		if (seasons==null || seasons.size()==0) {
			return 0;			
		}	
		@SuppressWarnings("unchecked")
		Map<String, BaseAirlockItem> mastersAirlockItemsDB = (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);

		//map between the input field and the maximal stage in all masters/branches
		HashMap<String, Stage> inputFieldsMaxStageMap = new HashMap<String, Stage>();
		
		//map between the feature on/off namespace.name and the maximal stage in all masters/branches
		HashMap<String, Stage> featuresOnOffMaxStageMap = new HashMap<String, Stage>();
		
		//map between the feature namespace.name that reports attributes and a pair of the maximal stage in all masters/branches and its pruned attributes 
		HashMap<String, StageAttributesPair> featuresAttributesStageMap = new HashMap<String, StageAttributesPair>();
		
		analyticsCounters counters = new analyticsCounters();
		
		HashMap<String, Stage> branchesStageInExperment = getBranchesStageInExperment();

		//map between feature name to a list of branches it is found in
		HashMap<String, TreeSet<String>> featuresBranches = new HashMap<String, TreeSet<String>>();
		
		//map between configurationRule and a map between parent and all barcnhes in which this is the parent
		HashMap<String, HashMap<String, ArrayList<String>>> configurationRulesParentInBranches = new HashMap<String, HashMap<String, ArrayList<String>>>();
		
		//map between configurationRule and a map between parent and all branches in which this is the parent
		HashMap<String, HashMap<String, ArrayList<String>>> orderingRulesParentInBranches = new HashMap<String, HashMap<String, ArrayList<String>>>();

		for (Season season:seasons) {
			//add the season's master analytics (master branch stage is the sam stage as the experiment's)
			addAnalytics(season.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection(), mastersAirlockItemsDB, inputFieldsMaxStageMap, featuresOnOffMaxStageMap, featuresAttributesStageMap, counters, null, stage, featuresBranches, configurationRulesParentInBranches, orderingRulesParentInBranches, season);
			for (Branch branch:season.getBranches().getBranchesList()) {
				if (!isBranchAssociatedToVariantInProd(branch))
					continue;
				if (branchesStageInExperment.containsKey(branch.getName())) {
					Stage branchStageInExp = branchesStageInExperment.get(branch.getName());
				
					Map<String, BaseAirlockItem> branchAirlockItemsDB = Utilities.getAirlockItemsDB (branch.getUniqueId().toString(), context); 
					addAnalytics(branch.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection(), branchAirlockItemsDB, inputFieldsMaxStageMap, featuresOnOffMaxStageMap, featuresAttributesStageMap, counters, branch, branchStageInExp, featuresBranches, configurationRulesParentInBranches, orderingRulesParentInBranches, season);
				}
			}
		}
		
		if (feature.getStage().equals(Stage.DEVELOPMENT)) {
			return counters.productionCounter;
		}
		
		String featureNamespaceDotName = feature.getNameSpaceDotName();
		
		if (!featuresAttributesStageMap.containsKey(featureNamespaceDotName)) {
			return  counters.productionCounter+prunedAttArray.size();
		}
		
		StageAttributesPair stageAttsPair = featuresAttributesStageMap.get(featureNamespaceDotName);
		
		for (int i=0; i<prunedAttArray.size(); i++) {
			if (!stageAttsPair.attributes.contains(prunedAttArray.get(i))) {
				//exists in array not in map - added
				counters.productionCounter++;				
			}
			else {
				//exists in array and in map - if stage moved from dev to prod - increase prod counter
				if (stageAttsPair.equals(Stage.DEVELOPMENT)) {
					counters.productionCounter++;
				}
				
			}
		}
		
		TreeSet<String> updatedAttributesList = new TreeSet<>(prunedAttArray);
		for (String att:stageAttsPair.attributes) {
			if (!updatedAttributesList.contains(att)) {
				if (stageAttsPair.stage.equals(Stage.PRODUCTION)) {
					//an attribute in production stage was removed from analytics
					counters.productionCounter--;
				}
			}
		}
		return counters.productionCounter;
	}

	private boolean isBranchAssociatedToVariantInProd(Branch branch) {
		for (Variant var:variants) {
			if (var.getBranchName().equals(branch.getName())) {
				return (var.getStage().equals(Stage.PRODUCTION)?true:false);
			}
		}
		return false;
	}
	
	//if replaced season exists - use the replacedAnalytics instead of the season analytics
	//if replaced branch exists - use the replacedAnalytics instead of the branch analytics
	public int getAnalyticsProductionCounterUponVariantUpdate(ServletContext context, UUID variantId, String updatedBranchName, Stage newStage, String newBranch ) throws MergeException {
		if (stage.equals(Stage.DEVELOPMENT))
			return 0;
		
		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);
		Product product = productsDB.get(productId.toString());
		
		ArrayList<Season> seasons = product.getSeasonsWithinRange(minVersion, maxVersion);
		if (seasons==null || seasons.size()==0) {
			return 0;			
		}	
		@SuppressWarnings("unchecked")
		Map<String, BaseAirlockItem> mastersAirlockItemsDB = (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);

		//map between the input field and the maximal stage in all masters/branches
		HashMap<String, Stage> inputFieldsMaxStageMap = new HashMap<String, Stage>();
		
		//map between the feature on/off namespace.name and the maximal stage in all masters/branches
		HashMap<String, Stage> featuresOnOffMaxStageMap = new HashMap<String, Stage>();
		
		//map between the feature namespace.name that reports attributes and a pair of the maximal stage in all masters/branches and its pruned attributes 
		HashMap<String, StageAttributesPair> featuresAttributesStageMap = new HashMap<String, StageAttributesPair>();
		
		analyticsCounters counters = new analyticsCounters();
		
		HashMap<String, Stage> branchesStageInExperment = getBranchesStageInExperment();

		//map between feature name to a list of branches it is found in
		HashMap<String, TreeSet<String>> featuresBranches = new HashMap<String, TreeSet<String>>();
		
		//map between configurationRule and a map between parent and all branches in which this is the parent
		HashMap<String, HashMap<String, ArrayList<String>>> configurationRulesParentInBranches = new HashMap<String, HashMap<String, ArrayList<String>>>();
		
		//map between configurationRule and a map between parent and all branches in which this is the parent
		HashMap<String, HashMap<String, ArrayList<String>>> orderingRulesParentInBranches = new HashMap<String, HashMap<String, ArrayList<String>>>();

		
		for (Season season:seasons) {
			//add the season's master analytics (master branch stage is the same stage as the experiment's)
			AnalyticsDataCollection anaDataCollection = season.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection();
			addAnalytics(anaDataCollection, mastersAirlockItemsDB, inputFieldsMaxStageMap, featuresOnOffMaxStageMap, featuresAttributesStageMap, counters, null, stage, featuresBranches, configurationRulesParentInBranches, orderingRulesParentInBranches, season);
			for (Branch branch:season.getBranches().getBranchesList()) {
				boolean isBranchAssociatedToVariantInProd = false;
				for (Variant var:variants) {
					if (updatedBranchName!=null && var.getUniqueId().equals(variantId)) {
						if (updatedBranchName.equals(branch.getName())) {
							isBranchAssociatedToVariantInProd = newStage.equals(Stage.PRODUCTION)?true:false;
						}
					}
					else if (var.getBranchName().equals(branch.getName())) {
						isBranchAssociatedToVariantInProd = var.getStage().equals(Stage.PRODUCTION)?true:false;
					}
				}				
				
				if (!isBranchAssociatedToVariantInProd)
					continue;
				
				
				Stage branchStageInExp = Stage.PRODUCTION;// branchesStageInExperment.get(branch.getName());
			
				Map<String, BaseAirlockItem> branchAirlockItemsDB = Utilities.getAirlockItemsDB (branch.getUniqueId().toString(), context);

				AnalyticsDataCollection branchAnaDataCollection = branch.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection();
				
				addAnalytics(branchAnaDataCollection, branchAirlockItemsDB, inputFieldsMaxStageMap, featuresOnOffMaxStageMap, featuresAttributesStageMap, counters, branch, branchStageInExp, featuresBranches, configurationRulesParentInBranches, orderingRulesParentInBranches, season);
			
			}
			if (newBranch!=null && newStage.equals(Stage.PRODUCTION)) {
				Branch newBranchObj = season.getBranches().getBranchByName(newBranch);
				Map<String, BaseAirlockItem> branchAirlockItemsDB = Utilities.getAirlockItemsDB (newBranchObj.getUniqueId().toString(), context);

				AnalyticsDataCollection branchAnaDataCollection = newBranchObj.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection();
				
				addAnalytics(branchAnaDataCollection, branchAirlockItemsDB, inputFieldsMaxStageMap, featuresOnOffMaxStageMap, featuresAttributesStageMap, counters, newBranchObj, Stage.PRODUCTION, featuresBranches, configurationRulesParentInBranches, orderingRulesParentInBranches, season);				
			}
		}
				
		return counters.productionCounter;
	}

	public int getQuotaReplaceSeasonQuota(ServletContext context, UUID replacedSeasonId, Integer newQuota) {
		return doGetQuota(context, replacedSeasonId, newQuota);
	}

	public boolean isPublishToAnalyticsSvrRequired(JSONObject updatedExperimentJSON, ServletContext context) throws JSONException {
		//stage
		String updatedStage = updatedExperimentJSON.getString(Constants.JSON_FEATURE_FIELD_STAGE);
		if (!updatedStage.equals(stage.toString())) 
			return true;
		
		//name
		String updatedName = updatedExperimentJSON.getString(Constants.JSON_FIELD_NAME);
		if (!updatedName.equals(name)) 
			return true;				
		
		//enable
		Boolean updatedEnabled = updatedExperimentJSON.getBoolean(Constants.JSON_FEATURE_FIELD_ENABLED);
		if (updatedEnabled!=enabled) 
			return true;
				
		//optional fields
		
		//hypothesis
		if (updatedExperimentJSON.containsKey(Constants.JSON_FIELD_HYPOTHESIS) &&  updatedExperimentJSON.get(Constants.JSON_FIELD_HYPOTHESIS)!=null) {			
			Object updatedHypothesis = updatedExperimentJSON.get(Constants.JSON_FIELD_HYPOTHESIS);		
			if (hypothesis == null) {
				return true;
			}
			else if (!hypothesis.equals(updatedHypothesis)) {
				return true;
			}
		}

		//description
		if (updatedExperimentJSON.containsKey(Constants.JSON_FIELD_DESCRIPTION) &&  updatedExperimentJSON.get(Constants.JSON_FIELD_DESCRIPTION)!=null) {			
			Object updatedDesc = updatedExperimentJSON.get(Constants.JSON_FIELD_DESCRIPTION);		
			if (description == null) {
				return true;
			}
			else if (!description.equals(updatedDesc)) {
				return true;
			}
		}

		//displayName
		if (updatedExperimentJSON.containsKey(Constants.JSON_FIELD_DISPLAY_NAME) &&  updatedExperimentJSON.get(Constants.JSON_FIELD_DISPLAY_NAME)!=null) {			
			Object updatedDispName = updatedExperimentJSON.get(Constants.JSON_FIELD_DISPLAY_NAME);		
			if (displayName == null) {
				return true;
			}
			else if (!displayName.equals(updatedDispName)) {
				return true;
			}
		}

		//ranges
		if (updatedExperimentJSON.containsKey(Constants.JSON_FIELD_RANGES) &&  updatedExperimentJSON.get(Constants.JSON_FIELD_RANGES)!=null) {
			JSONArray updatedExpRangesArr = updatedExperimentJSON.getJSONArray(Constants.JSON_FIELD_RANGES);
			if (!areRangesListEqual(updatedExpRangesArr)) {
				return true;
			}
		}
				
		//owner
		if (updatedExperimentJSON.containsKey(Constants.JSON_FEATURE_FIELD_OWNER) &&  updatedExperimentJSON.get(Constants.JSON_FEATURE_FIELD_OWNER)!=null) {			
			Object updatedOwner = updatedExperimentJSON.get(Constants.JSON_FEATURE_FIELD_OWNER);
			if (owner== null) {
				return true;
			}
			else if (!owner.equals(updatedOwner)) {
				return true;
			}
		}
		
		//indexExperiment
		if (updatedExperimentJSON.containsKey(Constants.JSON_FIELD_INDEX_EXPERIMENT) &&  updatedExperimentJSON.get(Constants.JSON_FIELD_INDEX_EXPERIMENT)!=null) {			
			Boolean updatedIndexExp = updatedExperimentJSON.getBoolean(Constants.JSON_FIELD_INDEX_EXPERIMENT);
			if (!indexExperiment.equals(updatedIndexExp)) {
				return true;
			}
		}
		
		//controlGroupVariants
		if (updatedExperimentJSON.containsKey(Constants.JSON_FIELD_CONTROL_GROUP_VARIANTS) &&  updatedExperimentJSON.get(Constants.JSON_FIELD_CONTROL_GROUP_VARIANTS)!=null) {
			//if missing from json or null - ignore
			JSONArray controlGroupVariantsArr = updatedExperimentJSON.getJSONArray(Constants.JSON_FIELD_CONTROL_GROUP_VARIANTS);
			if (!Utilities.compareIgnoreOrder(controlGroupVariantsArr, controlGroupVariants)) {
				return true;		
			}
		}	
		
		//variant changed
		if (updatedExperimentJSON.containsKey(Constants.JSON_FIELD_VARIANTS)) {
			@SuppressWarnings("unchecked")
			Map<String, Variant> variantsDB = (Map<String, Variant>)context.getAttribute(Constants.VARIANTS_DB_PARAM_NAME);
		
			JSONArray updatedVariantsArray = updatedExperimentJSON.getJSONArray(Constants.JSON_FIELD_VARIANTS); //after validate - i know exists
		
			for (int i=0; i<updatedVariantsArray.size(); i++) {
				JSONObject variantJSONObj = updatedVariantsArray.getJSONObject(i);

				String varId = variantJSONObj.getString(Constants.JSON_FIELD_UNIQUE_ID);
				Variant var = variantsDB.get(varId);
				if (var.isPublishToAnalyticsSvrRequired(variantJSONObj))
					return true;			
			}
		}
		
		
		return false;
	}

	//add first range to new experiment
	public void addExperimentRange(Date startDate) {
		if (enabled == true) {
			ExperimentRange expRange = new ExperimentRange(startDate, null);
			rangesList.add(expRange);
		}		
	}
	
	//close last range when enabled updated from true to false
	public void closeExistingExperimentRange(Date endDate) {
		if (rangesList.size()>0) {
			ExperimentRange lastRange = rangesList.get(rangesList.size()-1);
			lastRange.end = endDate;
		}
	}
	
	//When moving experiment from dev to prod: delete previous ranges and create new range starting now 
	public void deletePerviousRangesAndCreateNewRange(Date now) {
		rangesList.clear();
		addExperimentRange(now);
	}
	
	public void removeVariantFromControlGroup(String varName) {
		for (int i=0; i<controlGroupVariants.size(); i++) {
			if (controlGroupVariants.get(i).equals(varName)) {
				controlGroupVariants.remove(i);
				break;
			}		
		}
	}
}
