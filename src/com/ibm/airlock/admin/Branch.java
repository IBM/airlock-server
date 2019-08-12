package com.ibm.airlock.admin;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response.Status;

import com.ibm.airlock.Strings;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.AnalyticsServices;
import com.ibm.airlock.Constants;
import com.ibm.airlock.Constants.Action;
import com.ibm.airlock.Constants.BranchStatus;
import com.ibm.airlock.Constants.OutputJSONMode;
import com.ibm.airlock.Constants.REQUEST_ITEM_TYPE;
import com.ibm.airlock.Constants.Stage;
import com.ibm.airlock.admin.authentication.UserInfo;
import com.ibm.airlock.admin.purchases.EntitlementItem;
import com.ibm.airlock.admin.purchases.EntitlementMutualExclusionGroupItem;
import com.ibm.airlock.admin.purchases.PurchaseOptionsItem;
import com.ibm.airlock.admin.purchases.PurchaseOptionsMutualExclusionGroupItem;
import com.ibm.airlock.admin.BaseAirlockItem.Type;
import com.ibm.airlock.admin.MergeBranch.MergeException;
import com.ibm.airlock.admin.analytics.AirlockAnalytics;
import com.ibm.airlock.admin.analytics.AnalyticsDataCollection;
import com.ibm.airlock.admin.analytics.AnalyticsDataCollection.FeatureAttributesPair;
import com.ibm.airlock.admin.analytics.Experiment;
import com.ibm.airlock.admin.analytics.Variant;
import com.ibm.airlock.engine.Environment;

public class Branch {

	public static final Logger logger = Logger.getLogger(Branch.class.getName());


	private String name = null; //c+u
	private UUID uniqueId = null; //nc + u
	private LinkedList<BaseAirlockItem> branchFeatures = new  LinkedList<BaseAirlockItem>(); //nc
	private String description = null; //opt in c+u (if missing or null in update don't change)
	private UUID seasonId = null; //c+u
	private Date lastModified = null; // required in update. forbidden in create
	private Date creationDate = null; //nc + u (not changed)
	private String creator = null;	//c+u (creator not changed)
	private Map<String, BaseAirlockItem> branchAirlockItemsBD  = new ConcurrentHashMap<String, BaseAirlockItem>(); //the added and checked-out features
	private AirlockAnalytics analytics = null;
	//A root item in status none. It is used when the root is not checked out but some none children was moved to it
	//In the tree merge this is used only for getting the children list of the root
	private RootItem unCheckedoutRoot = null;
	private RootItem unCheckedoutPurchasesRoot = null;

	private LinkedList<BaseAirlockItem> branchPurchases = new  LinkedList<BaseAirlockItem>(); //nc 

	public Branch(UUID seasonId) {
		this.seasonId = seasonId;
		analytics = new AirlockAnalytics(seasonId);
	}

	public Date getLastModified() {
		return lastModified;
	}
	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public UUID getUniqueId() {
		return uniqueId;
	}
	public void setUniqueId(UUID uniqueId) {
		this.uniqueId = uniqueId;
	}	
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public UUID getSeasonId() {
		return seasonId;
	}
	public void setSeasonId(UUID seasonId) {
		this.seasonId = seasonId;
	}

	public Date getCreationDate() {
		return creationDate;
	}
	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}
	public String getCreator() {
		return creator;
	}
	public void setCreator(String creator) {
		this.creator = creator;
	}
	public LinkedList<BaseAirlockItem> getBranchPurchases() {
		return branchPurchases;
	}

	public void setBranchPurchases(LinkedList<BaseAirlockItem> branchPurchases) {
		this.branchPurchases = branchPurchases;
	}

	public LinkedList<BaseAirlockItem> getBranchFeatures() {
		return branchFeatures;
	}
	public void setBranchFeatures(LinkedList<BaseAirlockItem> branchFeatures) {
		this.branchFeatures = branchFeatures;
	}
	public BaseAirlockItem getBranchFeatureById(String itemId) {
		return branchAirlockItemsBD.get(itemId);
	}

	public AirlockAnalytics getAnalytics() {
		return analytics;
	}

	public void setAnalytics(AirlockAnalytics analytics) {
		this.analytics = analytics;
	}

	public RootItem getUnCheckedoutRoot() {
		return unCheckedoutRoot;
	}

	public void setUnCheckedoutRoot(RootItem unCheckedoutRoot) {
		this.unCheckedoutRoot = unCheckedoutRoot;
	}

	public RootItem getUnCheckedoutPurchasesRoot() {
		return unCheckedoutPurchasesRoot;
	}

	public void setUnCheckedoutPurchasesRoot(RootItem unCheckedoutPurchasesRoot) {
		this.unCheckedoutPurchasesRoot = unCheckedoutPurchasesRoot;
	}

	public DataAirlockItem getBranchFeatureByName(String branchFeatureName) {
		Set<String> alItemIds = branchAirlockItemsBD.keySet();
		for (String alItemId:alItemIds) {
			BaseAirlockItem alItem = branchAirlockItemsBD.get(alItemId);
			if (alItem instanceof DataAirlockItem) {
				DataAirlockItem dataAlItem = (DataAirlockItem)alItem;
				if (getItemBranchName(dataAlItem).equals(branchFeatureName)) {
					return dataAlItem;
				}
			}

		}

		return null;
	}

	public Map<String, BaseAirlockItem> getBranchAirlockItemsBD() {
		return branchAirlockItemsBD;
	}

	public JSONObject toJson(OutputJSONMode outputMode, ServletContext context, Environment env, boolean returnFeatures, boolean returnAnalyticsOnFeatures, boolean returnPurcahses) throws JSONException{
		JSONObject res = new JSONObject();

		res.put(Constants.JSON_FIELD_UNIQUE_ID, uniqueId==null?null:uniqueId.toString());
		res.put(Constants.JSON_FIELD_NAME, name);

		try {
			Map<String, BaseAirlockItem> branchAirlockItemsDB = Utilities.getAirlockItemsDB(uniqueId.toString(), context);

			if (returnFeatures) {
				JSONArray branchFeaturesArray = new JSONArray();

				for (BaseAirlockItem alItem:branchFeatures) {

					if (returnAnalyticsOnFeatures)
						env.setAnalytics(analytics);
					else 
						env.setAnalytics(null);

					env.setAirlockItemsDB(branchAirlockItemsDB);
					JSONObject alItemJson = alItem.toJson(outputMode, context, env);
					if (alItemJson!=null) //can be null if feature is in dev and mode is runtime_production
						branchFeaturesArray.add(alItemJson/*alItem.toJson(outputMode, context, env)*/);
				}
				res.put(Constants.JSON_FEATURE_FIELD_FEATURES, branchFeaturesArray);

			}

			if (returnPurcahses) {
				//return purchases as well
				JSONArray branchPurchasesArray = new JSONArray();

				for (BaseAirlockItem alItem:branchPurchases) {

					if (returnAnalyticsOnFeatures)
						env.setAnalytics(analytics);
					else 
						env.setAnalytics(null);

					env.setAirlockItemsDB(branchAirlockItemsDB);
					JSONObject alItemJson = alItem.toJson(outputMode, context, env);
					if (alItemJson!=null) //can be null if purchaseItem is in dev and mode is runtime_production
						branchPurchasesArray.add(alItemJson);
				}
				res.put(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS, branchPurchasesArray);
			}
		}
		catch (MergeException me) {
			String err = Strings.mergeException  + me.getMessage();
			logger.severe(err);
			throw new JSONException(err);			
		}


		//no dev/prod separation in this case but using existing modes so checking on both
		if (outputMode!=OutputJSONMode.RUNTIME_DEVELOPMENT && outputMode!=OutputJSONMode.RUNTIME_PRODUCTION) {
			res.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, seasonId==null?null:seasonId.toString());						

			if (outputMode.equals(OutputJSONMode.ADMIN) || outputMode.equals(OutputJSONMode.DISPLAY)) {
				res.put(Constants.JSON_FIELD_DESCRIPTION, description);
				res.put(Constants.JSON_FIELD_LAST_MODIFIED, lastModified.getTime());
				res.put(Constants.JSON_FEATURE_FIELD_CREATION_DATE, creationDate.getTime()); 			
				res.put(Constants.JSON_FEATURE_FIELD_CREATOR, creator);
			}
		}

		if (returnAnalyticsOnFeatures && AnalyticsServices.isAnalyticsSupported(env) &&
				(outputMode==OutputJSONMode.RUNTIME_DEVELOPMENT || outputMode==OutputJSONMode.RUNTIME_PRODUCTION)) {
			//in runtime files - write the inputFieldsForAnalitics

			@SuppressWarnings("unchecked")			
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);
			Season season = seasonsDB.get(seasonId.toString());

			res.put (Constants.JSON_FIELD_INPUT_FIELDS_FOR_ANALYTICS, analytics.getGlobalDataCollection().getAnalyticsDataCollection().getInputFieldsForAnalyticsPerStage(outputMode == OutputJSONMode.RUNTIME_DEVELOPMENT?Stage.DEVELOPMENT:Stage.PRODUCTION, context, season));		
		}


		return res;
	}

	public void fromJSON (JSONObject input, Environment env, Season season, ServletContext context) throws JSONException {
		if (input.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && input.get(Constants.JSON_FIELD_UNIQUE_ID) != null) {
			String sStr = input.getString(Constants.JSON_FIELD_UNIQUE_ID);			
			uniqueId = UUID.fromString(sStr);		
		}	

		if (input.containsKey(Constants.JSON_FEATURE_FIELD_SEASON_ID) && input.get(Constants.JSON_FEATURE_FIELD_SEASON_ID) != null) {
			String sStr = input.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID);			
			seasonId = UUID.fromString(sStr);			
		}

		if (input.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) && input.get(Constants.JSON_FIELD_LAST_MODIFIED)!=null) { 
			long timeInMS = input.getLong(Constants.JSON_FIELD_LAST_MODIFIED);
			lastModified = new Date(timeInMS);
		}  else {
			lastModified = new Date();
		}

		if (input.containsKey(Constants.JSON_FIELD_NAME) && input.get(Constants.JSON_FIELD_NAME)!=null) 
			name = input.getString(Constants.JSON_FIELD_NAME);			

		if (input.containsKey(Constants.JSON_FIELD_DESCRIPTION) && input.get(Constants.JSON_FIELD_DESCRIPTION)!=null) 
			description = input.getString(Constants.JSON_FIELD_DESCRIPTION).trim();

		if (input.containsKey(Constants.JSON_FEATURE_FIELD_CREATOR) && input.get(Constants.JSON_FEATURE_FIELD_CREATOR)!=null)
			creator = input.getString(Constants.JSON_FEATURE_FIELD_CREATOR).trim();

		if (input.containsKey(Constants.JSON_FEATURE_FIELD_CREATION_DATE) && input.get(Constants.JSON_FEATURE_FIELD_CREATION_DATE)!=null) { 
			long timeInMS = input.getLong(Constants.JSON_FEATURE_FIELD_CREATION_DATE);
			creationDate = new Date(timeInMS);			
		} else {
			creationDate = new Date();
		}

		if (input.containsKey(Constants.JSON_FEATURE_FIELD_FEATURES) && input.get(Constants.JSON_FEATURE_FIELD_FEATURES)!=null) { 			
			JSONArray branchFeaturesArray = input.getJSONArray(Constants.JSON_FEATURE_FIELD_FEATURES);
			for (int i=0; i<branchFeaturesArray.size(); i++) {
				JSONObject branchFeatureJSON = branchFeaturesArray.getJSONObject(i);

				BaseAirlockItem alItem = BaseAirlockItem.getAirlockItemByType(branchFeatureJSON);
				if (alItem==null) {
					String errMsg = Strings.typeNotFound;
					logger.severe(errMsg);
					throw new JSONException(errMsg);
				}

				UUID parentId = null; 
				alItem.fromJSON(branchFeatureJSON, branchAirlockItemsBD, parentId, env);	
				if (alItem.getParent() == null && alItem.getBranchFeatureParentName()!=null) { //branchFeatureParentName is null if the feature is the root
					//if root of a subtree is under the root - set the parent field according to the branchFeatureParentName field
					if (alItem.getBranchFeatureParentName().equals(Constants.ROOT_FEATURE)) {
						alItem.setParent(season.getRoot().getUniqueId());
					}
					else {
						try {
							BaseAirlockItem parentItem = Utilities.getFeatureByName(alItem.getBranchFeatureParentName(), Utilities.getAirlockItemsDB(Constants.MASTER_BRANCH_NAME, context));
							if (parentItem!=null)
								alItem.setParent(parentItem.getUniqueId());
						} catch (MergeException e) {
							//should not be thrown since we are using the master's db and not a branch db
						}
					}
				}

				branchAirlockItemsBD.put(alItem.getUniqueId().toString(), alItem);
				branchFeatures.add(alItem);
				if (alItem.getType().equals(Type.ROOT) && alItem.getBranchStatus().equals(BranchStatus.NONE)) {
					unCheckedoutRoot = (RootItem)alItem;
				}
			}
		}

		fromEntitlementsJSON(input, env, season, context);
	}

	public void fromEntitlementsJSON (JSONObject input, Environment env, Season season, ServletContext context) throws JSONException {
		if (input.containsKey(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS) && input.get(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS)!=null) { 			
			JSONArray branchPurchasesArray = input.getJSONArray(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS);
			for (int i=0; i<branchPurchasesArray.size(); i++) {
				JSONObject branchFeatureJSON = branchPurchasesArray.getJSONObject(i);

				BaseAirlockItem alItem = BaseAirlockItem.getAirlockItemByType(branchFeatureJSON);
				if (alItem==null) {
					String errMsg = Strings.typeNotFound;
					logger.severe(errMsg);
					throw new JSONException(errMsg);
				}

				UUID parentId = null; 
				alItem.fromJSON(branchFeatureJSON, branchAirlockItemsBD, parentId, env);	
				if (alItem.getParent() == null && alItem.getBranchFeatureParentName()!=null) { //branchFeatureParentName is null if the feature is the root
					//if root of a subtree is under the root - set the parent field according to the branchFeatureParentName field
					if (alItem.getBranchFeatureParentName().equals(Constants.ROOT_FEATURE)) {
						alItem.setParent(season.getRoot().getUniqueId());
					}
					else {
						try {
							BaseAirlockItem parentItem = Utilities.getFeatureByName(alItem.getBranchFeatureParentName(), Utilities.getAirlockItemsDB(Constants.MASTER_BRANCH_NAME, context));
							if (parentItem!=null)
								alItem.setParent(parentItem.getUniqueId());
						} catch (MergeException e) {
							//should not be thrown since we are using the master's db and not a branch db
						}
					}
				}

				branchAirlockItemsBD.put(alItem.getUniqueId().toString(), alItem);
				branchPurchases.add(alItem);
				if (alItem.getType().equals(Type.ROOT) && alItem.getBranchStatus().equals(BranchStatus.NONE)) {
					unCheckedoutPurchasesRoot = (RootItem)alItem;
				}
			}
		}
	}
	
	public ValidationResults validateBranchJSON(JSONObject branchJSON, ServletContext context, UserInfo userInfo) {
		try {
			//seasonId
			if (!branchJSON.containsKey(Constants.JSON_FEATURE_FIELD_SEASON_ID) || branchJSON.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID) == null || branchJSON.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_SEASON_ID), Status.BAD_REQUEST);
			}

			@SuppressWarnings("unchecked")
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			String sId = branchJSON.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID);
			Season season = seasonsDB.get(sId);
			if (season == null) {
				return new ValidationResults("The version range of the given branch does not exist.", Status.BAD_REQUEST);
			}

			//name
			if (!branchJSON.containsKey(Constants.JSON_FIELD_NAME) || branchJSON.getString(Constants.JSON_FIELD_NAME) == null || branchJSON.getString(Constants.JSON_FIELD_NAME).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_NAME), Status.BAD_REQUEST);
			}

			String validateNameErr = Utilities.validateName(branchJSON.getString(Constants.JSON_FIELD_NAME));
			if(validateNameErr!=null) {
				return new ValidationResults(validateNameErr, Status.BAD_REQUEST);
			}

			//creator
			if (!branchJSON.containsKey(Constants.JSON_FEATURE_FIELD_CREATOR) || branchJSON.getString(Constants.JSON_FEATURE_FIELD_CREATOR) == null || branchJSON.getString(Constants.JSON_FEATURE_FIELD_CREATOR).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_CREATOR), Status.BAD_REQUEST);
			}			

			//validate name uniqueness within the season
			String newName = branchJSON.getString(Constants.JSON_FIELD_NAME);
			for (int i=0; i<season.getBranches().getBranchesList().size(); i++) {
				Branch branch = season.getBranches().getBranchesList().get(i);
				if (uniqueId == null || !uniqueId.equals(branch.getUniqueId())) { //in update - skip yourself
					if (branch.getName().equals(newName)) {
						return new ValidationResults("A branch with the specified name already exists.", Status.BAD_REQUEST);
					}
				}
			} 

			Action action = Action.ADD;

			if (branchJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && branchJSON.get(Constants.JSON_FIELD_UNIQUE_ID)!=null) {
				//if JSON contains uniqueId - update an existing feature otherwise create a new feature
				action = Action.UPDATE;
			}						

			if (action == Action.ADD) {
				//modification date => should not appear in branch creation
				if (branchJSON.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) && branchJSON.get(Constants.JSON_FIELD_LAST_MODIFIED)!=null) {
					return new ValidationResults("The lastModified field cannot be specified during branch creation.", Status.BAD_REQUEST);
				}	

				if (branchJSON.containsKey(Constants.JSON_FIELD_ROOT) && branchJSON.get(Constants.JSON_FIELD_ROOT)!=null) {
					return new ValidationResults("The root field cannot be specified during branch creation.", Status.BAD_REQUEST);
				}

				//creation date => should not appear in branch creation
				if (branchJSON.containsKey(Constants.JSON_FEATURE_FIELD_CREATION_DATE) && branchJSON.get(Constants.JSON_FEATURE_FIELD_CREATION_DATE)!=null) {
					return new ValidationResults("The creationDate field cannot be specified during branch creation.", Status.BAD_REQUEST);
				}

				//features => should not appear in branch creation
				if (branchJSON.containsKey(Constants.JSON_FEATURE_FIELD_FEATURES) && branchJSON.get(Constants.JSON_FEATURE_FIELD_FEATURES)!=null) {
					return new ValidationResults("The features field cannot be specified during branch creation.", Status.BAD_REQUEST);
				}	
			}
			else { //update
				//modification date must appear in update
				if (!branchJSON.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) || branchJSON.get(Constants.JSON_FIELD_LAST_MODIFIED)==null) {
					return new ValidationResults("The lastModified field is missing. This field must be specified during branch update.", Status.BAD_REQUEST);
				}				

				//verify that given modification date is not older that current modification date
				long givenModoficationDate = branchJSON.getLong(Constants.JSON_FIELD_LAST_MODIFIED);  //verify that legal long
				Date givenDate = new Date(givenModoficationDate);
				if (givenDate.before(lastModified)) {
					return new ValidationResults("The branch was changed by another user. Refresh your browser and try again.", Status.CONFLICT);			
				}

				//creation date must appear
				if (!branchJSON.containsKey(Constants.JSON_FEATURE_FIELD_CREATION_DATE) || branchJSON.get(Constants.JSON_FEATURE_FIELD_CREATION_DATE)==null) {
					return new ValidationResults("The creationDate field is missing. This field branchJSON be specified during branch update.", Status.BAD_REQUEST);
				}

				//verify that legal long
				long creationdateLong = branchJSON.getLong(Constants.JSON_FEATURE_FIELD_CREATION_DATE);

				//verify that was not changed
				if (!creationDate.equals(new Date(creationdateLong))) {
					return new ValidationResults("creationDate cannot be changed during update", Status.BAD_REQUEST);
				}

				//creator must exist and not be changed
				String creatorStr = branchJSON.getString(Constants.JSON_FEATURE_FIELD_CREATOR);
				if (!creator.equals(creatorStr)) {
					return new ValidationResults(String.format(Strings.fieldCannotBeChangedDuringUpdate, Constants.JSON_FEATURE_FIELD_CREATOR), Status.BAD_REQUEST);
				}

				//verify that the name of branch that is used by experiment wasnt changed
				if (!newName.equals(name)) {
					String partOfExpData = isPartOfExperiment(context);
					if (partOfExpData!=null){
						return new ValidationResults("You cannot change a branch name that is in use by an experiment. The branch is being used by the experiment '" + partOfExpData + "'.", Status.BAD_REQUEST);
					}					
				}

				//season id must exists and not be changed
				if (!sId.equals(seasonId.toString())) {
					return new ValidationResults(String.format(Strings.fieldCannotBeChangedDuringUpdate, Constants.JSON_FEATURE_FIELD_SEASON_ID), Status.BAD_REQUEST);
				}

				//features => should not appear in branch update
				if (branchJSON.containsKey(Constants.JSON_FEATURE_FIELD_FEATURES) && branchJSON.get(Constants.JSON_FEATURE_FIELD_FEATURES)!=null) {
					return new ValidationResults("The features field cannot be specified during branch update.", Status.BAD_REQUEST);
				}	
			}
		} catch (JSONException jsne) {
			return new ValidationResults(jsne.getMessage(), Status.BAD_REQUEST);
		}

		return null;
	}


	//return null if branch is not part of experiment
	//return string containing the experiment and variant names that the branch is part of. 
	public String isPartOfExperiment(ServletContext context) {
		return doGetIsPartOfExperiment(context, false);		
	}

	//return null if branch is not part of experiment in production
	//return string containing the experiment and variant names that the branch is part of.
	//in production if experiment and variant are in production
	public String isPartOfExperimentInProduction(ServletContext context) {
		return doGetIsPartOfExperiment(context, true);		
	}

	//return null if branch is not part of experiment (in production)
	//return string containing the experiment and variant names that the branch is part of.
	private String doGetIsPartOfExperiment(ServletContext context, boolean onlyProductionExperimets) {
		@SuppressWarnings("unchecked")
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);
		Season season = seasonsDB.get(seasonId.toString()); 

		if (season == null) 
			return null; //should not happen

		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);					

		Product prod = productsDB.get(season.getProductId().toString());

		if (prod == null) 
			return null; //should not happen

		for (int i=0; i<prod.getExperimentsMutualExclusionGroup().getExperiments().size(); i++) {
			Experiment exp = prod.getExperimentsMutualExclusionGroup().getExperiments().get(i);
			if (onlyProductionExperimets && exp.getStage().equals(Stage.DEVELOPMENT)) //skip experiment in development
				continue;

			if (Product.isSeasonInRange(season, exp.getMinVersion(), exp.getMaxVersion())) {
				for (int j=0; j<exp.getVariants().size(); j++) {
					Variant var = exp.getVariants().get(j);
					if (onlyProductionExperimets && var.getStage().equals(Stage.DEVELOPMENT)) //skip variant in development
						continue;

					if (var.getBranchName().equals(name)) {
						return "experiment: " + exp.getName() + " variant: " + var.getName();
					}
				}
			}
		}

		return null;		
	}

	//return a list of experiment ids of the experiments that this branch is part of
	public LinkedList<String> getExperimentsOfBranch(ServletContext context) {
		@SuppressWarnings("unchecked")
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);
		Season season = seasonsDB.get(seasonId.toString()); 

		if (season == null) 
			return null; //should not happen

		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);					

		Product prod = productsDB.get(season.getProductId().toString());

		LinkedList<String> res = new LinkedList<String>();

		if (prod == null) 
			return res; //should not happen

		for (int i=0; i<prod.getExperimentsMutualExclusionGroup().getExperiments().size(); i++) {
			Experiment exp = prod.getExperimentsMutualExclusionGroup().getExperiments().get(i);						
			if (Product.isSeasonInRange(season, exp.getMinVersion(), exp.getMaxVersion())) {
				for (int j=0; j<exp.getVariants().size(); j++) {
					Variant var = exp.getVariants().get(j);
					if (var.getBranchName().equals(name)) {
						res.add(exp.getUniqueId().toString());
					}
				}
			}
		}

		return res;		
	}

	public String updateBranch(JSONObject updatedBranchJSON) throws JSONException {
		//seasonId, creator, creationDate should not be updated
		boolean wasChanged = false;
		StringBuilder updateDetails = new StringBuilder();

		//name
		String updatedName = updatedBranchJSON.getString(Constants.JSON_FIELD_NAME);
		if (!updatedName.equals(name)) {
			updateDetails.append(" 'name' changed from " + name + " to " + updatedName + "\n");
			name = updatedName;
			wasChanged = true;
		}	

		//optional fields
		if (updatedBranchJSON.containsKey(Constants.JSON_FIELD_DESCRIPTION) &&  updatedBranchJSON.get(Constants.JSON_FIELD_DESCRIPTION)!=null) {
			//if missing from json or null - ignore
			String updatedDescription = updatedBranchJSON.getString(Constants.JSON_FIELD_DESCRIPTION);
			if (description == null || !description.equals(updatedDescription)) {
				updateDetails.append(" 'description' changed from '" + description + "' to '" + updatedDescription + "'\n");
				description  = updatedDescription;
				wasChanged = true;
			}
		}	

		if (wasChanged) {
			lastModified = new Date();
		}

		return updateDetails.toString();			
	}

	//checkout the feature (with configurations without ancestors). 
	public BaseAirlockItem duplicateAndCheckoutToBranch(BaseAirlockItem featureToDuplicate, ServletContext context, Season season, REQUEST_ITEM_TYPE itemType) throws MergeException {

		HashMap<String, String> oldToDuplicatedFeaturesId = new HashMap<String, String>();
		ValidationCache tester = new ValidationCache();
		BaseAirlockItem duplicatedCheckedOutItem = featureToDuplicate.duplicate(null, null, featureToDuplicate.getParent(), branchAirlockItemsBD, oldToDuplicatedFeaturesId, context, false, true, tester);
		duplicatedCheckedOutItem.setBranchStatus(BranchStatus.NEW);

		//add the root of the checkout to the features list of the branch
		if (!isInBranchFeaturesListAsRoot(duplicatedCheckedOutItem, itemType)) {
			if (itemType.equals(REQUEST_ITEM_TYPE.FEATURES)) {
				branchFeatures.add(duplicatedCheckedOutItem);
			}
			else {//REQUEST_TYPE.PURCHASES
				branchPurchases.add(duplicatedCheckedOutItem);
			}
		}

		@SuppressWarnings("unchecked")
		Map<String, BaseAirlockItem> airlockItemsDB = (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);
		BaseAirlockItem parentInMaster = airlockItemsDB.get(featureToDuplicate.getParent().toString());
		duplicatedCheckedOutItem.setBranchFeatureParentName(getItemBranchName(parentInMaster));

		LinkedList<BaseAirlockItem> subTreesPointingToDeleted = new LinkedList<BaseAirlockItem>();
		LinkedList<BaseAirlockItem> branchItems = itemType.equals(REQUEST_ITEM_TYPE.FEATURES) ? branchFeatures : branchPurchases;
		for (BaseAirlockItem subTreeRoot:branchItems) {
			if (subTreeRoot.getBranchFeatureParentName()!=null && subTreeRoot.getBranchFeatureParentName().equals(getItemBranchName(duplicatedCheckedOutItem))) {
				subTreesPointingToDeleted.add(subTreeRoot);
			}
		}

		for (BaseAirlockItem subTreeRoot:subTreesPointingToDeleted) {
			duplicatedCheckedOutItem.addAirlockItem(subTreeRoot);

			if (subTreeRoot.getType().equals(Type.FEATURE) || subTreeRoot.getType().equals(Type.MUTUAL_EXCLUSION_GROUP)) { 
				if (duplicatedCheckedOutItem.getBranchFeaturesItems() == null) { 
					duplicatedCheckedOutItem.setBranchFeaturesItems(new LinkedList<String>());
				}
				duplicatedCheckedOutItem.getBranchFeaturesItems().add(getItemBranchName(subTreeRoot));
			}

			if (subTreeRoot.getType().equals(Type.CONFIG_MUTUAL_EXCLUSION_GROUP) || subTreeRoot.getType().equals(Type.CONFIGURATION_RULE)) {
				if (duplicatedCheckedOutItem.getBranchConfigurationRuleItems() == null) { 
					duplicatedCheckedOutItem.setBranchConfigurationRuleItems(new LinkedList<String>());
				}
				duplicatedCheckedOutItem.getBranchConfigurationRuleItems().add(getItemBranchName(subTreeRoot));
			}

			if (subTreeRoot.getType().equals(Type.ORDERING_RULE) || subTreeRoot.getType().equals(Type.ORDERING_RULE_MUTUAL_EXCLUSION_GROUP)) {
				if (duplicatedCheckedOutItem.getBranchOrderingRuleItems() == null) { 
					duplicatedCheckedOutItem.setBranchOrderingRuleItems(new LinkedList<String>());
				}
				duplicatedCheckedOutItem.getBranchOrderingRuleItems().add(getItemBranchName(subTreeRoot));
			}

			if (subTreeRoot.getType().equals(Type.ENTITLEMENT) || subTreeRoot.getType().equals(Type.ENTITLEMENT_MUTUAL_EXCLUSION_GROUP)) {
				if (duplicatedCheckedOutItem.getBranchEntitlementItems() == null) { 
					duplicatedCheckedOutItem.setBranchEntitlementItems(new LinkedList<String>());
				}
				duplicatedCheckedOutItem.getBranchEntitlementItems().add(getItemBranchName(subTreeRoot));
			}

			if (subTreeRoot.getType().equals(Type.PURCHASE_OPTIONS) || subTreeRoot.getType().equals(Type.PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP)) {
				if (duplicatedCheckedOutItem.getBranchPurchaseOptionsItems() == null) { 
					duplicatedCheckedOutItem.setBranchPurchaseOptionsItems(new LinkedList<String>());
				}
				duplicatedCheckedOutItem.getBranchPurchaseOptionsItems().add(getItemBranchName(subTreeRoot));
			}

			subTreeRoot.setParent(duplicatedCheckedOutItem.getUniqueId());
			subTreeRoot.setBranchFeatureParentName(null);

			removeFeaturesSubTreeFromBranch(subTreeRoot.getUniqueId(), itemType);
		}


		//if this feature exists in master analytics - add it to branch analytics
		AnalyticsDataCollection masterAnalyticsData = season.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection();
		addCheckedOutAndConfigsToAnalytics (duplicatedCheckedOutItem, masterAnalyticsData, airlockItemsDB);

		return duplicatedCheckedOutItem;
	}

	public BaseAirlockItem checkoutFeature(BaseAirlockItem feature, ServletContext context, REQUEST_ITEM_TYPE itemType) throws MergeException {
		//at this stage i know the feature and branch season is the same
		@SuppressWarnings("unchecked")			
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

		HashMap<String, String> oldToDuplicatedFeaturesId = new HashMap<String, String>();

		Map<String, BaseAirlockItem> branchAirlockItemsDB = Utilities.getAirlockItemsDB (uniqueId.toString(), context);
		BaseAirlockItem featureInBranch = branchAirlockItemsDB.get(feature.getUniqueId().toString());

		if (featureInBranch == null) {
			//the feature is in the master but not visible in branch (for example: if its parent was checked out before it was added to master) 
			return null;
		}

		LinkedList<String> ancestorsIds = getFeatureAncestors (featureInBranch, branchAirlockItemsDB);

		Season season = seasonsDB.get(seasonId.toString());

		LinkedList<BaseAirlockItem> duplicatedFeatures = new LinkedList<BaseAirlockItem>(); 
		BaseAirlockItem checkedOutItem = null;

		if (featureInBranch instanceof RootItem) {
			removeUnCheckedoutRoot(itemType); //if there is an unchecked out root - remove it from list
			ValidationCache tester = new ValidationCache();
			checkedOutItem = featureInBranch.duplicate(null, null, null, branchAirlockItemsBD, oldToDuplicatedFeaturesId, context, false, false, tester);
			duplicatedFeatures.add(checkedOutItem);
			setCheckedOutBranchStatus(checkedOutItem, branchAirlockItemsDB);						

			//add the root of the checkout to the features list of the branch
			if (!isInBranchFeaturesListAsRoot(checkedOutItem, itemType)) {
				if (itemType.equals(REQUEST_ITEM_TYPE.FEATURES)) {
					branchFeatures.add(checkedOutItem);
				}
				else { //REQUEST_ITEM_TYPE.PURCHASES
					branchPurchases.add(checkedOutItem);
				}
			}			
		}
		else {		
			//the checked out feature is not the root
			boolean firstItem = true;
			boolean prevIsMTX = false;

			ValidationCache tester = new ValidationCache();

			//the order of the ancestors list is from the child to the parent (the feature under root is the last) 
			//therefore we should first add the last one so we will have parentIds			
			for (int i = ancestorsIds.size()-1; i>=0; i--) {
				if (prevIsMTX) { //if parent is mtx - the feature was already added to the tree as one of the mtx child - skip it.
					BaseAirlockItem mtxChild = branchAirlockItemsDB.get(ancestorsIds.get(i)); 
					while ((mtxChild.getType().equals(Type.MUTUAL_EXCLUSION_GROUP) || mtxChild.getType().equals(Type.PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP) || mtxChild.getType().equals(Type.ENTITLEMENT_MUTUAL_EXCLUSION_GROUP)) 
							&& i>0)
						mtxChild = branchAirlockItemsDB.get(ancestorsIds.get(i--));
					checkedOutItem = mtxChild;
					prevIsMTX = false;
					continue;
				}

				BaseAirlockItem alItem = branchAirlockItemsDB.get(ancestorsIds.get(i));

				if (branchAirlockItemsBD.get(alItem.getUniqueId().toString())!=null &&
						branchAirlockItemsBD.get(alItem.getUniqueId().toString()).getBranchStatus() != BranchStatus.NONE) {
					checkedOutItem = branchAirlockItemsBD.get(alItem.getUniqueId().toString());
					firstItem = false;
					continue;	
				}

				//duplicate the airlock item and its configurations
				UUID parentId = null;
				if (firstItem) {
					parentId = itemType.equals(REQUEST_ITEM_TYPE.FEATURES) ? season.getRoot().getUniqueId() : season.getEntitlementsRoot().getUniqueId();
				}
				else {
					parentId = checkedOutItem.getUniqueId();
				}

				checkedOutItem = alItem.duplicate(null, null, parentId, branchAirlockItemsBD, oldToDuplicatedFeaturesId, context, false, false, tester);
				duplicatedFeatures.add(checkedOutItem);

				setCheckedOutBranchStatus(checkedOutItem, branchAirlockItemsDB);

				if (!firstItem) {
					//add the feature to its parent's features list
					branchAirlockItemsBD.get(checkedOutItem.getParent().toString()).addAirlockItem(checkedOutItem);
				}

				//if ((alItem.getType().equals(Type.MUTUAL_EXCLUSION_GROUP) || alItem.getType().equals(Type.ENTITLEMENT_MUTUAL_EXCLUSION_GROUP) || alItem.getType().equals(Type.PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP)) 
				//		&& i>0) {
				if (alItem.getType().equals(Type.MUTUAL_EXCLUSION_GROUP) && i>0) {
					//if the checked out is mtx but is not the feature we asked for.
					//When asking to checkout an mtx - its children are not checked out. (only if the mtx is on the ancestors path)
					checkoutMTXchildren((MutualExclusionGroupItem)alItem, (MutualExclusionGroupItem)checkedOutItem, oldToDuplicatedFeaturesId, context, duplicatedFeatures, branchAirlockItemsDB, tester, itemType);
					prevIsMTX = true;
				}
				else if (alItem.getType().equals(Type.ENTITLEMENT_MUTUAL_EXCLUSION_GROUP) && i>0) {
					//if the checked out is mtx but is not the feature we asked for.
					//When asking to checkout an mtx - its children are not checked out. (only if the mtx is on the ancestors path)
					checkoutInAppPurcahseMTXchildren((EntitlementMutualExclusionGroupItem)alItem, (EntitlementMutualExclusionGroupItem)checkedOutItem, oldToDuplicatedFeaturesId, context, duplicatedFeatures, branchAirlockItemsDB, tester, itemType);
					prevIsMTX = true;
				}
				else if (alItem.getType().equals(Type.PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP) && i>0) {
					//if the checked out is mtx but is not the feature we asked for.
					//When asking to checkout an mtx - its children are not checked out. (only if the mtx is on the ancestors path)
					checkoutPurcahseOptionsMTXchildren((PurchaseOptionsMutualExclusionGroupItem)alItem, (PurchaseOptionsMutualExclusionGroupItem)checkedOutItem, oldToDuplicatedFeaturesId, context, duplicatedFeatures, branchAirlockItemsDB, tester, itemType);
					prevIsMTX = true;
				}
				else {
					prevIsMTX = false;
				}

				firstItem=false;
			}

			//the first one added is the checked root		
			//BaseAirlockItem checkOutRoot = getBranchFeatureByName(getItemBranchName(airlockItemsDB.get(ancestorsIds.get(ancestorsIds.size()-1))));				
			BaseAirlockItem checkOutRoot = branchAirlockItemsBD.get(ancestorsIds.get(ancestorsIds.size()-1));
			checkOutRoot.setBranchFeatureParentName(Type.ROOT.toString());
			
			//add the root of the checkout to the features list of the branch
			if (!isInBranchFeaturesListAsRoot(checkOutRoot, itemType)) {
				addCheckoutRootAsSubTree(checkOutRoot,itemType, season, branchAirlockItemsDB);
			}
		}		

		//go over the duplicated feature (the checked out features) and see if some subTreeRoot is pointing to one of them.
		//if so remove it from the subTrees list and add it as a child to the duplicated feature

		//create list to prevent ConcurrentModificationException (iterating and changing branchFeatures concurrently)
		LinkedList<BaseAirlockItem> subTreeRootItems = new LinkedList<BaseAirlockItem>();
		if (itemType.equals(REQUEST_ITEM_TYPE.FEATURES)) {			
			for (BaseAirlockItem subTreeRoot:branchFeatures) {
				subTreeRootItems.add(subTreeRoot);
			}
		} 
		else { //REQUEST_ITEM_TYPE.PURCHASES
			for (BaseAirlockItem subTreeRoot:branchPurchases) {
				subTreeRootItems.add(subTreeRoot);
			}
		}

		for (BaseAirlockItem dupFeature:duplicatedFeatures) {
			for (BaseAirlockItem subTreeRoot:subTreeRootItems) {
				if (subTreeRoot.getBranchFeatureParentName()!=null && subTreeRoot.getBranchFeatureParentName().equals(getItemBranchName(dupFeature))) {
					dupFeature.addAirlockItem(subTreeRoot);
					subTreeRoot.setParent(dupFeature.getUniqueId());
					subTreeRoot.setBranchFeatureParentName(null);
					removeFeaturesSubTreeFromBranch(subTreeRoot.getUniqueId(), itemType);
				}
			}
		}

		//if this feature exists in master analytics - add it to branch analytics
		AnalyticsDataCollection masterAnalyticsData = season.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection();
		addCheckedOutAndConfigsToAnalytics (checkedOutItem, masterAnalyticsData, branchAirlockItemsDB);

		return checkedOutItem;		
	}

	private void addCheckoutRootAsSubTree(BaseAirlockItem checkOutRoot, REQUEST_ITEM_TYPE itemType, Season season, Map<String, BaseAirlockItem> branchAirlockItemsDB) {
		if (itemType.equals(REQUEST_ITEM_TYPE.FEATURES)) {
			//if the root is checked out - add the checkedout sub-item as the root's child in the branch
			RootItem rootInBranch = (RootItem)branchAirlockItemsDB.get(season.getRoot().getUniqueId().toString());
			if (rootInBranch.getBranchStatus().equals(BranchStatus.CHECKED_OUT)) {
				rootInBranch = (RootItem)branchFeatures.get(0); //if the root is checked out - it is the only feature in the subTrees list
				boolean found = false;
				for (int f=0; f<rootInBranch.getFeaturesItems().size(); f++) {
					if(rootInBranch.getFeaturesItems().get(f).getUniqueId().equals(checkOutRoot.getUniqueId())) {
						rootInBranch.getFeaturesItems().set(f, checkOutRoot);
						found = true;
						break;
					}
				}
				if (!found) {
					rootInBranch.getFeaturesItems().add(checkOutRoot);
				}
			}
			else {
				checkOutRoot.setParent(season.getRoot().getUniqueId());
				branchFeatures.add(checkOutRoot);
			}
		}
		else {//REQUEST_ITEM_TYPE.PURCHASES
			//if the root is checked out - add the checkedout sub-item as the root's child in the branch
			RootItem rootInBranch = (RootItem)branchAirlockItemsDB.get(season.getEntitlementsRoot().getUniqueId().toString());
			if (rootInBranch.getBranchStatus().equals(BranchStatus.CHECKED_OUT)) {
				rootInBranch = (RootItem)branchPurchases.get(0); //if the root is checked out - it is the only feature in the subTrees list
				boolean found = false;
				for (int f=0; f<rootInBranch.getEntitlementItems().size(); f++) {
					if(rootInBranch.getEntitlementItems().get(f).getUniqueId().equals(checkOutRoot.getUniqueId())) {
						rootInBranch.getEntitlementItems().set(f, checkOutRoot);
						found = true;
						break;
					}
				}
				if (!found) {
					rootInBranch.getEntitlementItems().add(checkOutRoot);
				}
			}
			else {
				checkOutRoot.setParent(season.getEntitlementsRoot().getUniqueId());
				branchPurchases.add(checkOutRoot);
			}
		}
		
	}

	//if this feature or one of its config items exists in master analytics - add it to branch analytics
	private void addCheckedOutAndConfigsToAnalytics(BaseAirlockItem checkedOutItem, AnalyticsDataCollection masterAnalyticsData, Map<String, BaseAirlockItem> airlockItemsDB) {
		String featureId = checkedOutItem.getUniqueId().toString();
		if (masterAnalyticsData.getFeaturesOnOffMap().containsKey(featureId)) {
			analytics.getGlobalDataCollection().getAnalyticsDataCollection().addFeatureOnOff(checkedOutItem, airlockItemsDB);
		}

		if (checkedOutItem instanceof FeatureItem) {
			if (masterAnalyticsData.getFeaturesConfigurationAttributesMap().containsKey(featureId)) {
				AnalyticsDataCollection.FeatureAttributesPair branchFeatureAtt = analytics.getGlobalDataCollection().getAnalyticsDataCollection().new FeatureAttributesPair(featureId);
				branchFeatureAtt.attributes = Utilities.cloneAttributesList(masterAnalyticsData.getFeaturesConfigurationAttributesMap().get(featureId));
				AnalyticsDataCollection brachAnalyticsData = analytics.getGlobalDataCollection().getAnalyticsDataCollection();
				brachAnalyticsData.getFeaturesConfigurationAttributesList().add(branchFeatureAtt);
				brachAnalyticsData.getFeaturesConfigurationAttributesMap().put(featureId, branchFeatureAtt.attributes);
				brachAnalyticsData.getFeaturesPrunedConfigurationAttributesMap().put(featureId, masterAnalyticsData.getFeaturesPrunedConfigurationAttributesMap().get(featureId));

				int numberofPrunedFeatures = brachAnalyticsData.getFeaturesPrunedConfigurationAttributesMap().get(featureId).size();
				brachAnalyticsData.setNumberOfDevelopmentItemsToAnalytics(brachAnalyticsData.getNumberOfDevelopmentItemsToAnalytics() + numberofPrunedFeatures);

				if (BaseAirlockItem.isProductionFeature(checkedOutItem, airlockItemsDB)) {
					brachAnalyticsData.setNumberOfProductionItemsToAnalytics(brachAnalyticsData.getNumberOfProductionItemsToAnalytics() + numberofPrunedFeatures);				
				}		
			}
		}

		if (checkedOutItem.getConfigurationRuleItems()!=null) {
			for (BaseAirlockItem subConfigItem:checkedOutItem.getConfigurationRuleItems()) {
				addCheckedOutAndConfigsToAnalytics(subConfigItem, masterAnalyticsData, airlockItemsDB);
			}
		}

		if (checkedOutItem.getOrderingRuleItems()!=null) {
			for (BaseAirlockItem subConfigItem:checkedOutItem.getOrderingRuleItems()) {
				addCheckedOutAndConfigsToAnalytics(subConfigItem, masterAnalyticsData, airlockItemsDB);
			}
		}
	}

	public void cancelCheckout(BaseAirlockItem feature, ServletContext context, boolean removeSubFeaturesAsWell, Map<String, BaseAirlockItem> branchAirlockItemsDB, REQUEST_ITEM_TYPE itemType) {

		if (removeSubFeaturesAsWell) {
			//remove feature and its configurations and subFeatures from branch
			removeFeatureAndSubItemsFromBranch(feature);
		}
		else {
			//remove feature and its configurations from branch
			removeFeatureAndConfigsFromBranch(feature);
		}

		LinkedList<BaseAirlockItem> branchSubTreeRoots = itemType.equals(REQUEST_ITEM_TYPE.FEATURES) ? branchFeatures : branchPurchases;
		for (BaseAirlockItem rootItem:branchSubTreeRoots) {
			//look for the unchecked feature in the branch features trees and remove it from there
			if (featureInSubTree(rootItem, feature)) {
				removeFeaturefromSubTree(rootItem, feature, removeSubFeaturesAsWell, false, itemType);
				break; //a checked out feature can be only in one subTree
			}
		}

		//if this feature exists in branch analytics - remove it from there
		removeUncheckedOutFeatureFromAnalytics(feature, branchAirlockItemsDB);
		AnalyticsDataCollection brachAnalyticsData = analytics.getGlobalDataCollection().getAnalyticsDataCollection();
		String featureId = feature.getUniqueId().toString();		
		if (brachAnalyticsData.getFeaturesOnOffMap().containsKey(featureId)) {
			analytics.getGlobalDataCollection().getAnalyticsDataCollection().removeFeatureOnOff(feature, branchAirlockItemsDB);
		}

		if (brachAnalyticsData.getFeaturesConfigurationAttributesMap().containsKey(featureId)) {
			for (int i=0; i<brachAnalyticsData.getFeaturesConfigurationAttributesList().size(); i++) {
				if (brachAnalyticsData.getFeaturesConfigurationAttributesList().get(i).id.equals(featureId)) {
					brachAnalyticsData.getFeaturesConfigurationAttributesList().remove(i);
					break;
				}
			}
			brachAnalyticsData.getFeaturesConfigurationAttributesMap().remove(featureId);
			int numberofPrunedFeatures = brachAnalyticsData.getFeaturesPrunedConfigurationAttributesMap().get(featureId).size();

			brachAnalyticsData.getFeaturesPrunedConfigurationAttributesMap().remove(featureId);			
			brachAnalyticsData.setNumberOfDevelopmentItemsToAnalytics(brachAnalyticsData.getNumberOfDevelopmentItemsToAnalytics() - numberofPrunedFeatures);

			if (BaseAirlockItem.isProductionFeature(feature, branchAirlockItemsDB)) {
				brachAnalyticsData.setNumberOfProductionItemsToAnalytics(brachAnalyticsData.getNumberOfProductionItemsToAnalytics() - numberofPrunedFeatures);				
			}		
		}
	}

	private void removeUncheckedOutFeatureFromAnalytics(BaseAirlockItem feature, Map<String, BaseAirlockItem> branchAirlockItemsDB) {
		AnalyticsDataCollection brachAnalyticsData = analytics.getGlobalDataCollection().getAnalyticsDataCollection();
		String featureId = feature.getUniqueId().toString();		
		if (brachAnalyticsData.getFeaturesOnOffMap().containsKey(featureId)) {
			analytics.getGlobalDataCollection().getAnalyticsDataCollection().removeFeatureOnOff(feature, branchAirlockItemsDB);
		}

		if (feature instanceof FeatureItem) {
			if (brachAnalyticsData.getFeaturesConfigurationAttributesMap().containsKey(featureId)) {
				for (int i=0; i<brachAnalyticsData.getFeaturesConfigurationAttributesList().size(); i++) {
					if (brachAnalyticsData.getFeaturesConfigurationAttributesList().get(i).id.equals(featureId)) {
						brachAnalyticsData.getFeaturesConfigurationAttributesList().remove(i);
						break;
					}
				}
				brachAnalyticsData.getFeaturesConfigurationAttributesMap().remove(featureId);
				int numberofPrunedFeatures = brachAnalyticsData.getFeaturesPrunedConfigurationAttributesMap().get(featureId).size();

				brachAnalyticsData.getFeaturesPrunedConfigurationAttributesMap().remove(featureId);			
				brachAnalyticsData.setNumberOfDevelopmentItemsToAnalytics(brachAnalyticsData.getNumberOfDevelopmentItemsToAnalytics() - numberofPrunedFeatures);

				if (BaseAirlockItem.isProductionFeature(feature, branchAirlockItemsDB)) {
					brachAnalyticsData.setNumberOfProductionItemsToAnalytics(brachAnalyticsData.getNumberOfProductionItemsToAnalytics() - numberofPrunedFeatures);				
				}		
			}
		}

		if (feature.getConfigurationRuleItems()!=null) {
			for (BaseAirlockItem subConfig:feature.getConfigurationRuleItems()) {
				removeUncheckedOutFeatureFromAnalytics(subConfig, branchAirlockItemsDB);
			}
		}

		if (feature.getOrderingRuleItems()!=null) {
			for (BaseAirlockItem subConfig:feature.getOrderingRuleItems()) {
				removeUncheckedOutFeatureFromAnalytics(subConfig, branchAirlockItemsDB);
			}
		}

	}

	//removeFromBranchChildrenList - if uncheckOut: leave the feature in the childlist
	//                               if delete: remove the feature from the childList
	private void removeFeaturefromSubTree(BaseAirlockItem rootItem, BaseAirlockItem removedFeature, boolean removeNewSubFeaturesAsWell, boolean removeFromBranchChildrenList, REQUEST_ITEM_TYPE itemType) {
		//if has added/checkedOut children - each one of them becomes root that pointed to the unchecked feature

		if(itemType.equals(REQUEST_ITEM_TYPE.FEATURES)) {
			if (removedFeature.getFeaturesItems()!=null && !removeNewSubFeaturesAsWell) {
				for (BaseAirlockItem subFeature:removedFeature.getFeaturesItems()) {
					if (!subFeature.getBranchStatus().equals(BranchStatus.NONE)) {
						subFeature.setBranchFeatureParentName(getItemBranchName(removedFeature));
						subFeature.setParent(removedFeature.getUniqueId());
						branchFeatures.addLast(subFeature);
					}
				}
			}
		}
		else {
			if (removedFeature.getEntitlementItems()!=null && !removeNewSubFeaturesAsWell) {
				for (BaseAirlockItem subFeature:removedFeature.getEntitlementItems()) {
					if (!subFeature.getBranchStatus().equals(BranchStatus.NONE)) {
						subFeature.setBranchFeatureParentName(getItemBranchName(removedFeature));
						subFeature.setParent(removedFeature.getUniqueId());
						branchPurchases.addLast(subFeature);	
					}
				}
			}

			if (removedFeature.getPurchaseOptionsItems()!=null && !removeNewSubFeaturesAsWell) {
				for (BaseAirlockItem subFeature:removedFeature.getPurchaseOptionsItems()) {
					if (!subFeature.getBranchStatus().equals(BranchStatus.NONE)) {
						subFeature.setBranchFeatureParentName(getItemBranchName(removedFeature));
						subFeature.setParent(removedFeature.getUniqueId());
						branchPurchases.addLast(subFeature);	
					}
				}
			}
		}


		if (removedFeature.getUniqueId().equals(rootItem.getUniqueId())) {
			//the unchecked feature is in the root - remove it from list
			removeFeaturesSubTreeFromBranch(removedFeature.getUniqueId(), itemType);			
		}
		else {
			//remove the feature from its parent's subfeatures list
			BaseAirlockItem parent = branchAirlockItemsBD.get(removedFeature.getParent().toString());
			parent.removeAirlockItem(removedFeature.getUniqueId());
			if (removeFromBranchChildrenList)
				removeFromBranchChildrenList (parent, removedFeature);
		}
	}

	public static void removeFromBranchChildrenList(BaseAirlockItem parent, BaseAirlockItem removedItem) {
		String removedItemBranchName = getItemBranchName(removedItem);
		if (removedItem instanceof EntitlementItem || removedItem instanceof EntitlementMutualExclusionGroupItem) {			
			if (parent.getBranchEntitlementItems()==null)
				return;

			for (int i=0; i<parent.getBranchEntitlementItems().size(); i++) {
				if (parent.getBranchEntitlementItems().get(i).equals(removedItemBranchName)) {
					parent.getBranchEntitlementItems().remove(i);								
					break;
				}
			}	
		}
		else if (removedItem instanceof PurchaseOptionsItem || removedItem instanceof PurchaseOptionsMutualExclusionGroupItem) {			
			if (parent.getBranchPurchaseOptionsItems()==null)
				return;

			for (int i=0; i<parent.getBranchPurchaseOptionsItems().size(); i++) {
				if (parent.getBranchPurchaseOptionsItems().get(i).equals(removedItemBranchName)) {
					parent.getBranchPurchaseOptionsItems().remove(i);								
					break;
				}
			}	
 		}
		//feature should be after entitlement and purcahseOptions since both of them are also instance of feature 
		else if (removedItem instanceof MutualExclusionGroupItem || removedItem instanceof FeatureItem) {
			if (parent.getBranchFeaturesItems()==null)
				return;

			for (int i=0; i<parent.getBranchFeaturesItems().size(); i++) {
				if (parent.getBranchFeaturesItems().get(i).equals(removedItemBranchName)) {
					parent.getBranchFeaturesItems().remove(i);								
					break;
				}
			}		
		}
		else if (removedItem instanceof OrderingRuleItem || removedItem instanceof OrderingRuleMutualExclusionGroupItem) {			
			if (parent.getBranchOrderingRuleItems()==null)
				return;

			for (int i=0; i<parent.getBranchOrderingRuleItems().size(); i++) {
				if (parent.getBranchOrderingRuleItems().get(i).equals(removedItemBranchName)) {
					parent.getBranchOrderingRuleItems().remove(i);								
					break;
				}
			}	
		}
		else if (removedItem instanceof ConfigMutualExclusionGroupItem || removedItem instanceof ConfigurationRuleItem) {			
			if (parent.getBranchConfigurationRuleItems()==null)
				return;

			for (int i=0; i<parent.getBranchConfigurationRuleItems().size(); i++) {
				if (parent.getBranchConfigurationRuleItems().get(i).equals(removedItemBranchName)) {
					parent.getBranchConfigurationRuleItems().remove(i);								
					break;
				}
			}	
		}
	}

	//for feature/configRule return namespace.name
	//for root return ROOT
	//for MTX return mtx.uniqueId
	//for configMTX return cmtx.uniqueId
	public static String getItemBranchName(BaseAirlockItem item) {
		if (item instanceof BaseMutualExclusionGroupItem)
			return "mx." + item.getUniqueId().toString();

		return item.getNameSpaceDotName();
	}

	public void removeFeaturesSubTreeFromBranch(UUID rootId, REQUEST_ITEM_TYPE itemType) {
		if (itemType.equals(REQUEST_ITEM_TYPE.FEATURES)) {
			for (int i=0; i<branchFeatures.size(); i++) {
				BaseAirlockItem root = branchFeatures.get(i);
				if (root.getUniqueId().equals(rootId)) {
					branchFeatures.remove(i);
				}
			}
		}
		else { //REQUEST_ITEM_TYPE.PURCHASES
			for (int i=0; i<branchPurchases.size(); i++) {
				BaseAirlockItem root = branchPurchases.get(i);
				if (root.getUniqueId().equals(rootId)) {
					branchPurchases.remove(i);
				}
			}
		}
	}
	/*
	public void removePurchasesSubTreeFromBranch(UUID rootId) {
		for (int i=0; i<branchPurchases.size(); i++) {
			BaseAirlockItem root = branchPurchases.get(i);
			if (root.getUniqueId().equals(rootId)) {
				branchPurchases.remove(i);
			}
		}
	}*/

	private boolean featureInSubTree(BaseAirlockItem rootItem, BaseAirlockItem feature) {
		if (feature.getUniqueId().equals(rootItem.getUniqueId())) {
			return true;
		}
		if(rootItem.getFeaturesItems()!=null) {
			for (BaseAirlockItem alItem:rootItem.getFeaturesItems()) {
				if (featureInSubTree(alItem, feature))
					return true; 
			}
		}
		if(rootItem.getConfigurationRuleItems()!=null) {
			for (BaseAirlockItem alItem:rootItem.getConfigurationRuleItems()) {
				if (featureInSubTree(alItem, feature))
					return true; 
			}
		}
		if(rootItem.getOrderingRuleItems()!=null) {
			for (BaseAirlockItem alItem:rootItem.getOrderingRuleItems()) {
				if (featureInSubTree(alItem, feature))
					return true; 
			}
		}
		if(rootItem.getEntitlementItems()!=null) {
			for (BaseAirlockItem alItem:rootItem.getEntitlementItems()) {
				if (featureInSubTree(alItem, feature))
					return true; 
			}
		}
		if(rootItem.getPurchaseOptionsItems()!=null) {
			for (BaseAirlockItem alItem:rootItem.getPurchaseOptionsItems()) {
				if (featureInSubTree(alItem, feature))
					return true; 
			}
		}
		return false;
	}

	//remove feature and its configurations from branch	
	private void removeFeatureAndConfigsFromBranch(BaseAirlockItem item) {		
		removeItemAndItsSubItemsFromBranch(item, true, false);
	}

	//remove feature and its configurations and subFeatures from branch	
	private void removeFeatureAndSubItemsFromBranch(BaseAirlockItem item) {		
		removeItemAndItsSubItemsFromBranch(item, true, true);
	}


	//remove feature and its configurations from branch	
	private void removeItemAndItsSubItemsFromBranch(BaseAirlockItem item, boolean removeSubConfigs, boolean removeSubFeatures) {
		branchAirlockItemsBD.remove(item.getUniqueId().toString());
		if (removeSubConfigs && item.getConfigurationRuleItems()!=null) {
			for (BaseAirlockItem bai:item.getConfigurationRuleItems()) {
				removeItemAndItsSubItemsFromBranch(bai, removeSubConfigs, removeSubFeatures);
			}
		}	

		if (removeSubConfigs && item.getOrderingRuleItems()!=null) {
			for (BaseAirlockItem bai:item.getOrderingRuleItems()) {
				removeItemAndItsSubItemsFromBranch(bai, removeSubConfigs, removeSubFeatures);
			}
		}	

		if (removeSubFeatures && item.getFeaturesItems()!=null) {
			for (BaseAirlockItem bai:item.getFeaturesItems()) {
				removeItemAndItsSubItemsFromBranch(bai, removeSubConfigs, removeSubFeatures);
			}
		}

		if (removeSubFeatures && item.getPurchaseOptionsItems()!=null) {
			for (BaseAirlockItem bai:item.getPurchaseOptionsItems()) {
				removeItemAndItsSubItemsFromBranch(bai, removeSubConfigs, removeSubFeatures);
			}
		}

		if (removeSubFeatures && item.getEntitlementItems()!=null) {
			for (BaseAirlockItem bai:item.getEntitlementItems()) {
				removeItemAndItsSubItemsFromBranch(bai, removeSubConfigs, removeSubFeatures);
			}
		}
	}

	//set the feature and its configurations status to checked_out
	private void setCheckedOutBranchStatus(BaseAirlockItem checkedOutItem, Map<String, BaseAirlockItem> branchAirlockItemsDB) {
		checkedOutItem.setBranchStatus(BranchStatus.CHECKED_OUT);
		//taking the child order as in the branch at the checkout time. (if a child was moved to other parent in branch dont include it back at checkout)
		BaseAirlockItem itemInBranch = branchAirlockItemsDB.get(checkedOutItem.getUniqueId().toString());
		LinkedList<String> branchFeatureItemsList = new LinkedList<>();
		LinkedList<String> branchConfigItemsList = new LinkedList<>();
		LinkedList<String> orderingRuleItemsList = new LinkedList<>();
		LinkedList<String> inAppPurchaseItemsList = new LinkedList<>();
		LinkedList<String> purchaseOptionsItemsList = new LinkedList<>();

		//checking on both features and config rule since i know only one of them is populated
		if (itemInBranch.getFeaturesItems()!=null) { 
			for (BaseAirlockItem item:itemInBranch.getFeaturesItems()) {
				branchFeatureItemsList.add(getItemBranchName(item));
			}
		}

		if (itemInBranch.getConfigurationRuleItems()!=null) {
			for (BaseAirlockItem item:itemInBranch.getConfigurationRuleItems()) {
				branchConfigItemsList.add(getItemBranchName(item));
			}
		}

		if (itemInBranch.getOrderingRuleItems()!=null) {
			for (BaseAirlockItem item:itemInBranch.getOrderingRuleItems()) {
				orderingRuleItemsList.add(getItemBranchName(item));
			}
		}

		if (itemInBranch.getEntitlementItems()!=null) {
			for (BaseAirlockItem item:itemInBranch.getEntitlementItems()) {
				inAppPurchaseItemsList.add(getItemBranchName(item));
			}
		}

		if (itemInBranch.getPurchaseOptionsItems()!=null) {
			for (BaseAirlockItem item:itemInBranch.getPurchaseOptionsItems()) {
				purchaseOptionsItemsList.add(getItemBranchName(item));
			}
		}

		checkedOutItem.setBranchFeaturesItems(branchFeatureItemsList);
		checkedOutItem.setBranchConfigurationRuleItems(branchConfigItemsList);
		checkedOutItem.setBranchOrderingRuleItems(orderingRuleItemsList);
		checkedOutItem.setBranchEntitlementItems(inAppPurchaseItemsList);
		checkedOutItem.setBranchPurchaseOptionsItems(purchaseOptionsItemsList);

		if (checkedOutItem.getConfigurationRuleItems()!=null) {
			for (BaseAirlockItem configItem:checkedOutItem.getConfigurationRuleItems()) {
				setCheckedOutBranchStatus (configItem, branchAirlockItemsDB);
			}
		}	

		if (checkedOutItem.getOrderingRuleItems()!=null) {
			for (BaseAirlockItem orderingRuleItem:checkedOutItem.getOrderingRuleItems()) {
				setCheckedOutBranchStatus (orderingRuleItem, branchAirlockItemsDB);
			}
		}	
	}

	private boolean isInBranchFeaturesListAsRoot(BaseAirlockItem checkOutRoot, REQUEST_ITEM_TYPE itemType) {
		if (itemType.equals(REQUEST_ITEM_TYPE.FEATURES)) {
			for (BaseAirlockItem subTreeRoot : branchFeatures) {
				if (subTreeRoot.getUniqueId().equals(checkOutRoot.getUniqueId()))
					return true;
			}
		}
		else {
			for (BaseAirlockItem subTreeRoot : branchPurchases) {
				if (subTreeRoot.getUniqueId().equals(checkOutRoot.getUniqueId()))
					return true;
			}
		}
		return false;
	}
	/*
	private boolean isInBranchPurchasesListAsRoot(BaseAirlockItem checkOutRoot) {
		for (BaseAirlockItem subTreeRoot : branchPurchases) {
			if (subTreeRoot.getUniqueId().equals(checkOutRoot.getUniqueId()))
				return true;
		}
		return false;
	}
	 */
	private void checkoutMTXchildren(MutualExclusionGroupItem mastreMTX, MutualExclusionGroupItem branchMTX, HashMap<String, String> oldToDuplicatedFeaturesId, ServletContext context, LinkedList<BaseAirlockItem> duplicatedFeatures, 
			Map<String, BaseAirlockItem> branchAirlockItemsDB, ValidationCache tester, REQUEST_ITEM_TYPE itemType) {

		if (branchMTX.getOrderingRuleItems()!=null) {
			LinkedList<String> orderingRuleItemsList = new LinkedList<>();

			//set branch ordering rules
			for (BaseAirlockItem orderingRuleItem:branchMTX.getOrderingRuleItems()) {
				orderingRuleItemsList.add(getItemBranchName(orderingRuleItem));											
			}

			branchMTX.setBranchOrderingRuleItems(orderingRuleItemsList);

			//call to set checked out status to all sub odering rules
			for (BaseAirlockItem orderingRuleItem:branchMTX.getOrderingRuleItems()) {
				setCheckedOutBranchStatus (orderingRuleItem, branchAirlockItemsDB);			
			}
		}	


		for (BaseAirlockItem mtxChild : mastreMTX.getFeaturesItems()) {	
			///
			//if the child of the mtx is already checked out use it
			if (branchAirlockItemsBD.get(mtxChild.getUniqueId().toString())!=null &&
					branchAirlockItemsBD.get(mtxChild.getUniqueId().toString()).getBranchStatus() != BranchStatus.NONE) {
				BaseAirlockItem checkedOutItem = branchAirlockItemsBD.get(mtxChild.getUniqueId().toString());
				//if it is root of subTree in the branch and its parent is the mtx => move it under the mtx tree
				if (isInBranchFeaturesListAsRoot(checkedOutItem, itemType) && checkedOutItem.getBranchFeatureParentName().equals(getItemBranchName(branchMTX))) {
					branchMTX.addAirlockItem(checkedOutItem);
					removeFeaturesSubTreeFromBranch(checkedOutItem.getUniqueId(), itemType);
				}

				//if the sub item is checked out but moved to another feature in the branch - dont add it to the checked out mtx
				continue;	
			}

			///
			BaseAirlockItem checkedOutItem = mtxChild.duplicate(null, null, branchMTX.getUniqueId(), branchAirlockItemsBD, oldToDuplicatedFeaturesId, context, false, false, tester);
			duplicatedFeatures.add(checkedOutItem);			
			setCheckedOutBranchStatus(checkedOutItem, branchAirlockItemsDB);
			branchMTX.addAirlockItem(checkedOutItem);

			if (checkedOutItem.getType().equals(Type.MUTUAL_EXCLUSION_GROUP)) {
				checkoutMTXchildren((MutualExclusionGroupItem)mtxChild, (MutualExclusionGroupItem)checkedOutItem, oldToDuplicatedFeaturesId, context, duplicatedFeatures, branchAirlockItemsDB, tester, itemType);
			}
		}
	}

	private void checkoutInAppPurcahseMTXchildren(EntitlementMutualExclusionGroupItem mastreMTX, EntitlementMutualExclusionGroupItem branchMTX, HashMap<String, String> oldToDuplicatedFeaturesId, ServletContext context, LinkedList<BaseAirlockItem> duplicatedFeatures, 
			Map<String, BaseAirlockItem> branchAirlockItemsDB, ValidationCache tester, REQUEST_ITEM_TYPE itemType) {

		for (BaseAirlockItem mtxChild : mastreMTX.getEntitlementItems()) {	
			//if the child of the mtx is already checked out use it
			if (branchAirlockItemsBD.get(mtxChild.getUniqueId().toString())!=null &&
					branchAirlockItemsBD.get(mtxChild.getUniqueId().toString()).getBranchStatus() != BranchStatus.NONE) {
				BaseAirlockItem checkedOutItem = branchAirlockItemsBD.get(mtxChild.getUniqueId().toString());
				//if it is root of subTree in the branch and its parent is the mtx => move it under the mtx tree
				if (isInBranchFeaturesListAsRoot(checkedOutItem, itemType) && checkedOutItem.getBranchFeatureParentName().equals(getItemBranchName(branchMTX))) {
					branchMTX.addAirlockItem(checkedOutItem);
					removeFeaturesSubTreeFromBranch(checkedOutItem.getUniqueId(), itemType);
				}

				//if the sub item is checked out but moved to another feature in the branch - dont add it to the checked out mtx
				continue;	
			}
			BaseAirlockItem checkedOutItem = mtxChild.duplicate(null, null, branchMTX.getUniqueId(), branchAirlockItemsBD, oldToDuplicatedFeaturesId, context, false, false, tester);
			duplicatedFeatures.add(checkedOutItem);			
			setCheckedOutBranchStatus(checkedOutItem, branchAirlockItemsDB);
			branchMTX.addAirlockItem(checkedOutItem);

			if (checkedOutItem.getType().equals(Type.ENTITLEMENT_MUTUAL_EXCLUSION_GROUP)) {
				checkoutInAppPurcahseMTXchildren((EntitlementMutualExclusionGroupItem)mtxChild, (EntitlementMutualExclusionGroupItem)checkedOutItem, oldToDuplicatedFeaturesId, context, duplicatedFeatures, branchAirlockItemsDB, tester, itemType);
			}
		}
	}

	private void checkoutPurcahseOptionsMTXchildren(PurchaseOptionsMutualExclusionGroupItem mastreMTX, PurchaseOptionsMutualExclusionGroupItem branchMTX, HashMap<String, String> oldToDuplicatedFeaturesId, ServletContext context, LinkedList<BaseAirlockItem> duplicatedFeatures, 
			Map<String, BaseAirlockItem> branchAirlockItemsDB, ValidationCache tester, REQUEST_ITEM_TYPE itemType) {

		for (BaseAirlockItem mtxChild : mastreMTX.getPurchaseOptionsItems()) {	
			//if the child of the mtx is already checked out use it
			if (branchAirlockItemsBD.get(mtxChild.getUniqueId().toString())!=null &&
					branchAirlockItemsBD.get(mtxChild.getUniqueId().toString()).getBranchStatus() != BranchStatus.NONE) {
				BaseAirlockItem checkedOutItem = branchAirlockItemsBD.get(mtxChild.getUniqueId().toString());
				//if it is root of subTree in the branch and its parent is the mtx => move it under the mtx tree
				if (isInBranchFeaturesListAsRoot(checkedOutItem, itemType) && checkedOutItem.getBranchFeatureParentName().equals(getItemBranchName(branchMTX))) {
					branchMTX.addAirlockItem(checkedOutItem);
					removeFeaturesSubTreeFromBranch(checkedOutItem.getUniqueId(), itemType);
				}

				//if the sub item is checked out but moved to another feature in the branch - dont add it to the checked out mtx
				continue;	
			}
			BaseAirlockItem checkedOutItem = mtxChild.duplicate(null, null, branchMTX.getUniqueId(), branchAirlockItemsBD, oldToDuplicatedFeaturesId, context, false, false, tester);
			duplicatedFeatures.add(checkedOutItem);			
			setCheckedOutBranchStatus(checkedOutItem, branchAirlockItemsDB);
			branchMTX.addAirlockItem(checkedOutItem);

			if (checkedOutItem.getType().equals(Type.ENTITLEMENT_MUTUAL_EXCLUSION_GROUP)) {
				checkoutPurcahseOptionsMTXchildren((PurchaseOptionsMutualExclusionGroupItem)mtxChild, (PurchaseOptionsMutualExclusionGroupItem)checkedOutItem, oldToDuplicatedFeaturesId, context, duplicatedFeatures, branchAirlockItemsDB, tester, itemType);
			}
		}
	}

	//the order of the list is from the chile to the parent (the feature under root is the last) 
	private LinkedList<String> getFeatureAncestors(BaseAirlockItem alItem, Map<String, BaseAirlockItem> airlockItemsDB) {
		LinkedList<String> ancestorsIds = new LinkedList<String>();
		while (!alItem.getType().equals(Type.ROOT)) {
			ancestorsIds.add(alItem.getUniqueId().toString());			
			alItem = airlockItemsDB.get(alItem.getParent().toString());
		}
		return ancestorsIds;
	}

	public static JSONObject getDummyMasterBranchObject(Season season) throws JSONException {
		JSONObject res = new JSONObject();

		res.put(Constants.JSON_FIELD_UNIQUE_ID, Constants.MASTER_BRANCH_NAME);
		res.put(Constants.JSON_FIELD_NAME, Constants.MASTER_BRANCH_NAME);


		//no dev/prod separation in this case but using existing modes so checking on both
		res.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, season.getUniqueId()==null?null:season.getUniqueId().toString());						

		res.put(Constants.JSON_FIELD_DESCRIPTION, Constants.MASTER_BRANCH_NAME);
		res.put(Constants.JSON_FIELD_LAST_MODIFIED, season.getLastModified().getTime()); //Elik: are you checing on these fields
		res.put(Constants.JSON_FEATURE_FIELD_CREATION_DATE, season.getLastModified().getTime()); 			
		res.put(Constants.JSON_FEATURE_FIELD_CREATOR, Constants.MASTER_BRANCH_NAME);

		return res;
	}

	public Branch duplicateForNewSeason (Season newSeason, HashMap<String, String> oldToDuplicatedFeaturesId, ServletContext context) throws JSONException {
		Branch res = new Branch(newSeason.getUniqueId());
		res.setName(name);
		res.setCreator(creator);
		res.setCreationDate(creationDate);
		res.setLastModified(lastModified);
		res.setSeasonId(newSeason.getUniqueId());
		res.setUniqueId(UUID.randomUUID());

		HashMap<String, String> branchOldToDuplicatedFeaturesId = new HashMap<String, String>();

		ValidationCache tester = new ValidationCache();
		for (BaseAirlockItem subTreeRoot:branchFeatures) {
			BaseAirlockItem newSubTreeRoot = subTreeRoot.duplicate(newSeason.getMinVersion(), newSeason.getUniqueId(), null, res.branchAirlockItemsBD, branchOldToDuplicatedFeaturesId, context, true, true, tester);
			res.branchAirlockItemsBD.put(newSubTreeRoot.getUniqueId().toString(), newSubTreeRoot);
			res.branchFeatures.add(newSubTreeRoot);

			if (newSubTreeRoot.getType().equals(Type.ROOT) && newSubTreeRoot.getBranchStatus().equals(BranchStatus.NONE)) {
				unCheckedoutRoot = (RootItem)newSubTreeRoot;
			}
		}

		for (BaseAirlockItem subTreeRoot:branchPurchases) {
			BaseAirlockItem newSubTreeRoot = subTreeRoot.duplicate(newSeason.getMinVersion(), newSeason.getUniqueId(), null, res.branchAirlockItemsBD, branchOldToDuplicatedFeaturesId, context, true, true, tester);
			res.branchAirlockItemsBD.put(newSubTreeRoot.getUniqueId().toString(), newSubTreeRoot);
			res.branchPurchases.add(newSubTreeRoot);

			if (newSubTreeRoot.getType().equals(Type.ROOT) && newSubTreeRoot.getBranchStatus().equals(BranchStatus.NONE)) {
				unCheckedoutRoot = (RootItem)newSubTreeRoot;
			}
		}

		//update the uniqueIds of checkedOut features to the uniqueId of the original copy in master in new season.
		HashMap<String, String> branchChangedFeaturesId = new HashMap<String, String>();
		Set<String> branchOldFeatureIds = branchOldToDuplicatedFeaturesId.keySet();
		for (String branchOldFeatureId : branchOldFeatureIds) {
			String  branchNewfeatureId = branchOldToDuplicatedFeaturesId.get(branchOldFeatureId);
			String  masterNewfeatureId = oldToDuplicatedFeaturesId.get(branchOldFeatureId);

			BaseAirlockItem branchItem = res.branchAirlockItemsBD.get(branchNewfeatureId);
			if (branchItem.getBranchStatus().equals(BranchStatus.CHECKED_OUT)) {
				UUID newId = UUID.fromString(masterNewfeatureId);
				branchItem.setUniqueId(newId);
				res.branchAirlockItemsBD.remove(branchNewfeatureId);
				res.branchAirlockItemsBD.put(masterNewfeatureId, branchItem);

				branchChangedFeaturesId.put(branchNewfeatureId, masterNewfeatureId);
				branchOldToDuplicatedFeaturesId.put (branchOldFeatureId, masterNewfeatureId); //update the map for analytics duplication

				if (branchItem.getFeaturesItems()!=null) {
					for (BaseAirlockItem subItem:branchItem.getFeaturesItems()) {
						subItem.setParent(newId);
					}
				}
				if (branchItem.getConfigurationRuleItems()!=null) {
					for (BaseAirlockItem subItem:branchItem.getConfigurationRuleItems()) {
						subItem.setParent(newId);
					}
				}
				if (branchItem.getOrderingRuleItems()!=null) {
					for (BaseAirlockItem subItem:branchItem.getOrderingRuleItems()) {
						subItem.setParent(newId);
					}
				}
				if (branchItem.getEntitlementItems()!=null) {
					for (BaseAirlockItem subItem:branchItem.getEntitlementItems()) {
						subItem.setParent(newId);
					}
				}
				if (branchItem.getPurchaseOptionsItems()!=null) {
					for (BaseAirlockItem subItem:branchItem.getPurchaseOptionsItems()) {
						subItem.setParent(newId);
					}
				}
			}			
		}

		for (BaseAirlockItem subTreeRoot:res.branchFeatures) {
			if (subTreeRoot.getBranchFeatureParentName().startsWith("mx.")) {
				//it is pointing to a mutualExclusion item. Since the mtx id was changed we should update it in the 
				//subTree root as well.
				String prevMtxId = subTreeRoot.getBranchFeatureParentName().substring(3);
				String newMtxId = oldToDuplicatedFeaturesId.get(prevMtxId);
				subTreeRoot.setBranchFeatureParentName("mx." + newMtxId);
			}
		}
		
		for (BaseAirlockItem subTreeRoot:res.branchPurchases) {
			if (subTreeRoot.getBranchFeatureParentName().startsWith("mx.")) {
				//it is pointing to a mutualExclusion item. Since the mtx id was changed we should update it in the 
				//subTree root as well.
				String prevMtxId = subTreeRoot.getBranchFeatureParentName().substring(3);
				String newMtxId = oldToDuplicatedFeaturesId.get(prevMtxId);
				subTreeRoot.setBranchFeatureParentName("mx." + newMtxId);
			}
		}

		Set<String> branchFeatureIds = res.branchAirlockItemsBD.keySet();
		for (String branchFeatureId:branchFeatureIds) {
			BaseAirlockItem branchFeature = res.branchAirlockItemsBD.get(branchFeatureId);
			if (branchFeature.getBranchFeaturesItems()!=null) {
				for (int i=0; i<branchFeature.getBranchFeaturesItems().size(); i++) {
					String subFeatureId = branchFeature.getBranchFeaturesItems().get(i); 
					if (subFeatureId.startsWith("mx.")) {
						//it is pointing to a mutualExclusion item. Since the mtx id was changed we should update it in the 
						//subTree root as well.
						String prevMtxId = subFeatureId.substring(3);
						String newMtxId = oldToDuplicatedFeaturesId.get(prevMtxId);
						if (newMtxId == null)
							newMtxId = branchOldToDuplicatedFeaturesId.get(prevMtxId); //this means that the mtx was new therefore exists only in branch ids map
						subFeatureId = "mx." + newMtxId;
						branchFeature.getBranchFeaturesItems().set(i, subFeatureId);
					}

				}
			}

			if (branchFeature instanceof OrderingRuleItem) {
				//update the sub features ids of the features that are in the master and are not checked out
				String newConfig = OrderingRuleItem.replaceIdsInConfiguration(tester, context, oldToDuplicatedFeaturesId, 
						((OrderingRuleItem)branchFeature).getConfiguration(), ((OrderingRuleItem)branchFeature).getRule(), 
						((OrderingRuleItem)branchFeature).getSeasonId().toString(), ((OrderingRuleItem)branchFeature).getStage(), 
						((OrderingRuleItem)branchFeature).getMinAppVersion());

				//if a checked out feature is in the config - its id was changed in the feature duplication but was changed again to be the master's id
				newConfig = OrderingRuleItem.replaceIdsInConfiguration(tester, context, branchChangedFeaturesId, 
						newConfig, ((OrderingRuleItem)branchFeature).getRule(), 
						((OrderingRuleItem)branchFeature).getSeasonId().toString(), ((OrderingRuleItem)branchFeature).getStage(), 
						((OrderingRuleItem)branchFeature).getMinAppVersion());

				((OrderingRuleItem)branchFeature).setConfiguration(newConfig);
			}

			if (branchFeature.getBranchConfigurationRuleItems()!=null) {
				for (int i=0; i<branchFeature.getBranchConfigurationRuleItems().size(); i++) {
					String subFeatureId = branchFeature.getBranchConfigurationRuleItems().get(i); 
					if (subFeatureId.startsWith("mx.")) {
						//it is pointing to a configMutualExclusion item. Since the mtx id was changed we should update it in the 
						//subTree root as well.
						String prevMtxId = subFeatureId.substring(3);
						String newMtxId = oldToDuplicatedFeaturesId.get(prevMtxId);
						if (newMtxId == null)
							newMtxId = branchOldToDuplicatedFeaturesId.get(prevMtxId); //this means that the mtx was new therefore exists only in branch ids map

						subFeatureId = "mx." + newMtxId;
						branchFeature.getBranchConfigurationRuleItems().set(i, subFeatureId);
					}

				}
			}

			if (branchFeature.getBranchOrderingRuleItems()!=null) {
				for (int i=0; i<branchFeature.getBranchOrderingRuleItems().size(); i++) {
					String subFeatureId = branchFeature.getBranchOrderingRuleItems().get(i); 
					if (subFeatureId.startsWith("mx.")) {
						//it is pointing to a OrderingRuleMutualExclusion item. Since the mtx id was changed we should update it in the 
						//subTree root as well.
						String prevMtxId = subFeatureId.substring(3);
						String newMtxId = oldToDuplicatedFeaturesId.get(prevMtxId);
						if (newMtxId == null)
							newMtxId = branchOldToDuplicatedFeaturesId.get(prevMtxId); //this means that the mtx was new therefore exists only in branch ids map

						subFeatureId = "mx." + newMtxId;
						branchFeature.getBranchOrderingRuleItems().set(i, subFeatureId);
					}

				}
			}
			
			if (branchFeature.getBranchEntitlementItems()!=null) {
				for (int i=0; i<branchFeature.getBranchEntitlementItems().size(); i++) {
					String subFeatureId = branchFeature.getBranchEntitlementItems().get(i); 
					if (subFeatureId.startsWith("mx.")) {
						//it is pointing to an inAppPurchaseMutualExclusion item. Since the mtx id was changed we should update it in the 
						//subTree root as well.
						String prevMtxId = subFeatureId.substring(3);
						String newMtxId = oldToDuplicatedFeaturesId.get(prevMtxId);
						if (newMtxId == null)
							newMtxId = branchOldToDuplicatedFeaturesId.get(prevMtxId); //this means that the mtx was new therefore exists only in branch ids map

						subFeatureId = "mx." + newMtxId;
						branchFeature.getBranchEntitlementItems().set(i, subFeatureId);
					}
				}
			}
			
			if (branchFeature.getBranchPurchaseOptionsItems()!=null) {
				for (int i=0; i<branchFeature.getBranchPurchaseOptionsItems().size(); i++) {
					String subFeatureId = branchFeature.getBranchPurchaseOptionsItems().get(i); 
					if (subFeatureId.startsWith("mx.")) {
						//it is pointing to an purchaseOptionsMutualExclusion item. Since the mtx id was changed we should update it in the 
						//subTree root as well.
						String prevMtxId = subFeatureId.substring(3);
						String newMtxId = oldToDuplicatedFeaturesId.get(prevMtxId);
						if (newMtxId == null)
							newMtxId = branchOldToDuplicatedFeaturesId.get(prevMtxId); //this means that the mtx was new therefore exists only in branch ids map

						subFeatureId = "mx." + newMtxId;
						branchFeature.getBranchPurchaseOptionsItems().set(i, subFeatureId);
					}
				}
			}
		}

		//replacing the inAppPurchses in a bundles and in premium features
		for (BaseAirlockItem subTreeRoot:res.getBranchFeatures()) {
			Season.doReplacePrucahseIdInFeature(subTreeRoot,oldToDuplicatedFeaturesId,  branchOldToDuplicatedFeaturesId);
		}
		
		for (BaseAirlockItem subTreeRoot:res.getBranchPurchases()) {
			Season.doReplacePrucahseIdInBundle(subTreeRoot, oldToDuplicatedFeaturesId, branchOldToDuplicatedFeaturesId);
		}
		
		res.setAnalytics(analytics.duplicateForNewSeason(newSeason.getUniqueId(), branchOldToDuplicatedFeaturesId, context, seasonId));

		return res;
	}
	//return null upon success, error string upon failure	
	public String addPurchase(BaseAirlockItem newPurcahse, BaseAirlockItem parentPurcahse) {
		return doAddItem(newPurcahse, parentPurcahse, REQUEST_ITEM_TYPE.ENTITLEMENTS);
	}

	//return null upon success, error string upon failure	
	public String addFeature(BaseAirlockItem newFeature, BaseAirlockItem parentFeature) {
		return doAddItem(newFeature, parentFeature, REQUEST_ITEM_TYPE.FEATURES);
	}

	//return null upon success, error string upon failure	
	public String addAirlockItem(BaseAirlockItem newAirlockItem, BaseAirlockItem parentItem, REQUEST_ITEM_TYPE itemType) {
		return doAddItem(newAirlockItem, parentItem, itemType);
	}
	
	//add purchase or feature return null upon success, error string upon failure	
	public String doAddItem(BaseAirlockItem newItem, BaseAirlockItem parentItem, REQUEST_ITEM_TYPE itemType) {
		//cannot add an item under an unchecked out item (NONE). Root is an exceptional - a user can add a a feature under the root even if not checked out
		if (!(parentItem instanceof RootItem) && parentItem.getBranchStatus().equals(BranchStatus.NONE)) {
			return "You cannot add the " + newItem.getObjTypeStrByType() + " under an item that is not checked out. First check out the parent item. To add a configuration, check out its parent feature.";			
		}

		newItem.setBranchStatus(BranchStatus.NEW);
		if (!parentItem.getBranchStatus().equals(BranchStatus.NONE)) { 
			BaseAirlockItem parentInBranch = branchAirlockItemsBD.get(parentItem.getUniqueId().toString());
			String err = parentInBranch.addAirlockItem(newItem);
			if (err!=null) {
				return err;
			}
			if (newItem instanceof MutualExclusionGroupItem || newItem.getType().equals(Type.FEATURE)) {
				if (parentInBranch.getBranchFeaturesItems() == null) { 
					parentInBranch.setBranchFeaturesItems(new LinkedList<String>());
				}
				parentInBranch.getBranchFeaturesItems().add(getItemBranchName(newItem));
			}
			else if (newItem instanceof OrderingRuleMutualExclusionGroupItem || newItem instanceof OrderingRuleItem) {
				//newFeature is ordering rule or orderingRuleMutualExclusion
				if (parentInBranch.getBranchOrderingRuleItems() == null) { 
					parentInBranch.setBranchOrderingRuleItems(new LinkedList<String>());
				}
				parentInBranch.getBranchOrderingRuleItems().add(getItemBranchName(newItem));
			}
			else if (newItem instanceof ConfigMutualExclusionGroupItem || newItem instanceof ConfigurationRuleItem) {
				//newFeature is configRule or configMutualExclusion
				if (parentInBranch.getBranchConfigurationRuleItems() == null) { 
					parentInBranch.setBranchConfigurationRuleItems(new LinkedList<String>());
				}
				parentInBranch.getBranchConfigurationRuleItems().add(getItemBranchName(newItem));
			}
			else if (newItem instanceof EntitlementMutualExclusionGroupItem || newItem instanceof EntitlementItem) {
				//newFeature is inAppPurcahse or inAppPurcahseMutualExclusion
				if (parentInBranch.getBranchEntitlementItems() == null) { 
					parentInBranch.setBranchEntitlementItems(new LinkedList<String>());
				}
				parentInBranch.getBranchEntitlementItems().add(getItemBranchName(newItem));
			}
			else if (newItem instanceof PurchaseOptionsMutualExclusionGroupItem || newItem instanceof PurchaseOptionsItem) {
				//newFeature is PurcahseOptions or PurcahseOptionsMutualExclusion
				if (parentInBranch.getBranchPurchaseOptionsItems() == null) { 
					parentInBranch.setBranchPurchaseOptionsItems(new LinkedList<String>());
				}
				parentInBranch.getBranchPurchaseOptionsItems().add(getItemBranchName(newItem));
			}

			parentItem.setLastModified(new Date());
		}
		else {
			newItem.setBranchFeatureParentName(getItemBranchName(parentItem)); //added to root - this is the only option that the parent is in none status
			if (itemType.equals(REQUEST_ITEM_TYPE.FEATURES) ) {
				branchFeatures.add(newItem);
			} 
			else { //PURCHASES
				branchPurchases.add(newItem);
			}
		}

		branchAirlockItemsBD.put(newItem.getUniqueId().toString(), newItem);
		return null;
	}

	//return null upon success, error string upon failure		
	public String deleteFeature(BaseAirlockItem featureToDel, REQUEST_ITEM_TYPE itemType) {
		if (featureToDel.getBranchStatus().equals(BranchStatus.CHECKED_OUT)) {
			return "You cannot delete an item that is checked out from the master. Instead, disable the item.";
		}

		if (featureToDel.getBranchStatus().equals(BranchStatus.NONE)) {
			return "You cannot delete an item that is not checked out. Instead, you can check out the item and disable it.";
		}

		//NEW feature - created in branch
		BaseAirlockItem branchBaseAI = branchAirlockItemsBD.get(featureToDel.getUniqueId().toString());
		if (hasCheckedOutSubItem(branchBaseAI, itemType)) {

			return "You cannot delete the item because one of its sub-items is checked out from the master."; 

		}

		//at this stage i know that the feature can be deleted
		removeItemAndItsSubItemsFromBranch(branchBaseAI, true, true);
		LinkedList<BaseAirlockItem> branchSubTrees = itemType.equals(REQUEST_ITEM_TYPE.FEATURES)?branchFeatures:branchPurchases;
		for (BaseAirlockItem rootItem:branchSubTrees) {
			//look for the unchecked feature in the branch features trees and remove it from there
			if (featureInSubTree(rootItem, branchBaseAI)) {
				removeFeaturefromSubTree(rootItem, branchBaseAI, true, true, itemType); //delete also new sub features 
				break; //a checked out feature can be only in one subTree
			}
		}

		return null;
	}

	private boolean hasCheckedOutSubItem(BaseAirlockItem branchFeature, REQUEST_ITEM_TYPE itemType) {
		if (itemType.equals(REQUEST_ITEM_TYPE.FEATURES)) {
			if (branchFeature.getFeaturesItems()!=null) {
				for (BaseAirlockItem subItem : branchFeature.getFeaturesItems()) {
					if (subItem.getBranchStatus().equals(BranchStatus.CHECKED_OUT)) {
						return true;
					}
					boolean foundInSubTree = hasCheckedOutSubItem(subItem, itemType);
					if (foundInSubTree)
						return true;
				}
			}
		}
		else { //purchases
			if (branchFeature.getEntitlementItems()!=null) {				
				for (BaseAirlockItem subItem : branchFeature.getEntitlementItems()) {
					if (subItem.getBranchStatus().equals(BranchStatus.CHECKED_OUT)) {
						return true;
					}
					boolean foundInSubTree = hasCheckedOutSubItem(subItem, itemType);
					if (foundInSubTree)
						return true;
				}
			}
			
			if  (branchFeature.getPurchaseOptionsItems() !=null) {
				for (BaseAirlockItem subItem : branchFeature.getPurchaseOptionsItems()) {
					if (subItem.getBranchStatus().equals(BranchStatus.CHECKED_OUT)) {
						return true;
					}
					boolean foundInSubTree = hasCheckedOutSubItem(subItem, itemType);
					if (foundInSubTree)
						return true;
				}
			}
		}
		
		return false;
	}

	//return void if the updated feature is not pointed in the branch, other wise return the max stage of a feature
	//pointing to the updated feature
	public Stage updateFeatureNamePointing(String origNamespaceDotName, String newNameSpaceDotName) {
		Stage maxStage = null;
		for (BaseAirlockItem subTreeRoot:branchFeatures) {
			if (subTreeRoot.getBranchFeatureParentName().equals(origNamespaceDotName)) {
				subTreeRoot.setBranchFeatureParentName(newNameSpaceDotName);
				maxStage=calculateMaxStage (maxStage, subTreeRoot); 
			}
		}
		
		for (BaseAirlockItem subTreeRoot:branchPurchases) {
			if (subTreeRoot.getBranchFeatureParentName().equals(origNamespaceDotName)) {
				subTreeRoot.setBranchFeatureParentName(newNameSpaceDotName);
				maxStage=calculateMaxStage (maxStage, subTreeRoot); 
			}
		}

		Set<String> branchFeatureIds = branchAirlockItemsBD.keySet();
		for (String branchFeatureId:branchFeatureIds) {
			BaseAirlockItem branchFeature = branchAirlockItemsBD.get(branchFeatureId);
			if (branchFeature.getBranchFeaturesItems()!=null) {
				for (int i=0; i<branchFeature.getBranchFeaturesItems().size(); i++) {
					String subFeatureId = branchFeature.getBranchFeaturesItems().get(i); 
					if (subFeatureId.equals(origNamespaceDotName)) {
						branchFeature.getBranchFeaturesItems().set(i, newNameSpaceDotName);
						maxStage=calculateMaxStage (maxStage, branchFeature);
						break;
					}
				}
			}

			if (branchFeature.getBranchConfigurationRuleItems()!=null) {
				for (int i=0; i<branchFeature.getBranchConfigurationRuleItems().size(); i++) {
					String subCRId = branchFeature.getBranchConfigurationRuleItems().get(i); 
					if (subCRId.equals(origNamespaceDotName)) {
						branchFeature.getBranchConfigurationRuleItems().set(i, newNameSpaceDotName);
						maxStage=calculateMaxStage (maxStage, branchFeature);
						break;
					}
				}
			}	

			if (branchFeature.getBranchOrderingRuleItems()!=null) {
				for (int i=0; i<branchFeature.getBranchOrderingRuleItems().size(); i++) {
					String subCRId = branchFeature.getBranchOrderingRuleItems().get(i); 
					if (subCRId.equals(origNamespaceDotName)) {
						branchFeature.getBranchOrderingRuleItems().set(i, newNameSpaceDotName);
						maxStage=calculateMaxStage (maxStage, branchFeature);
						break;
					}
				}
			}
			
			if (branchFeature.getBranchPurchaseOptionsItems()!=null) {
				for (int i=0; i<branchFeature.getBranchPurchaseOptionsItems().size(); i++) {
					String subCRId = branchFeature.getBranchPurchaseOptionsItems().get(i); 
					if (subCRId.equals(origNamespaceDotName)) {
						branchFeature.getBranchPurchaseOptionsItems().set(i, newNameSpaceDotName);
						maxStage=calculateMaxStage (maxStage, branchFeature);
						break;
					}
				}
			}
			
			if (branchFeature.getBranchEntitlementItems()!=null) {
				for (int i=0; i<branchFeature.getBranchEntitlementItems().size(); i++) {
					String subCRId = branchFeature.getBranchEntitlementItems().get(i); 
					if (subCRId.equals(origNamespaceDotName)) {
						branchFeature.getBranchEntitlementItems().set(i, newNameSpaceDotName);
						maxStage=calculateMaxStage (maxStage, branchFeature);
						break;
					}
				}
			}
		}

		return maxStage;		
	}

	private Stage calculateMaxStage(Stage givenStage, BaseAirlockItem item) {
		Stage itemStage = null;
		if (item instanceof DataAirlockItem) {
			itemStage = ((DataAirlockItem)item).getStage();			
		}
		else {
			itemStage = item.containSubItemInProductionStage()?Stage.PRODUCTION:Stage.DEVELOPMENT;
		}

		if (givenStage == null) {
			return itemStage;
		}

		if (givenStage.equals(Stage.PRODUCTION)) {
			return givenStage;
		}

		return itemStage;
	}

	//return true if a feature or one of its configuration rules are reported in analytics 
	public boolean isFeatureInAnalytics(BaseAirlockItem feature) {
		return isItemInReportedInAnalytics(feature);				
	}

	private boolean isItemInReportedInAnalytics(BaseAirlockItem item) {
		String itemId = item.getUniqueId().toString(); 
		if (analytics.getGlobalDataCollection().getAnalyticsDataCollection().getFeaturesOnOffMap().containsKey(itemId)) { 
			return true;
		}

		if (item instanceof FeatureItem) {
			if (analytics.getGlobalDataCollection().getAnalyticsDataCollection().getFeaturesConfigurationAttributesMap().containsKey(itemId)) { 
				return true;
			}
		}

		if (item.getConfigurationRuleItems()!=null) {
			for (BaseAirlockItem subItem:item.getConfigurationRuleItems()) {
				if (isItemInReportedInAnalytics(subItem))
					return true;
			}
		}

		if (item.getOrderingRuleItems()!=null) {
			for (BaseAirlockItem subItem:item.getOrderingRuleItems()) {
				if (isItemInReportedInAnalytics(subItem))
					return true;
			}
		}
		if (item.getEntitlementItems()!=null) {
			for (BaseAirlockItem subItem:item.getEntitlementItems()) {
				if (isItemInReportedInAnalytics(subItem))
					return true;
			}
		}
		if (item.getPurchaseOptionsItems()!=null) {
			for (BaseAirlockItem subItem:item.getPurchaseOptionsItems()) {
				if (isItemInReportedInAnalytics(subItem))
					return true;
			}
		}

		return false;
	}

	public void replaceFeatureIdInAnalytics(String oldId, String newId) {
		AnalyticsDataCollection  branchAnalytics = analytics.getGlobalDataCollection().getAnalyticsDataCollection();
		if (branchAnalytics.getFeaturesOnOffMap().containsKey(oldId)) {
			branchAnalytics.getFeaturesOnOffMap().put(newId, branchAnalytics.getFeaturesOnOffMap().get(oldId));
			branchAnalytics.getFeaturesOnOffMap().remove(oldId);
			branchAnalytics.getFeaturesOnOff().remove(oldId);
			branchAnalytics.getFeaturesOnOff().add(newId);
			//no need to update counters - besides id all stay the same
		}

		if (branchAnalytics.getFeaturesConfigurationAttributesMap().containsKey(oldId)) {
			branchAnalytics.getFeaturesConfigurationAttributesMap().put(newId, branchAnalytics.getFeaturesConfigurationAttributesMap().get(oldId));
			branchAnalytics.getFeaturesConfigurationAttributesMap().remove(oldId);
			branchAnalytics.getFeaturesPrunedConfigurationAttributesMap().put(newId, branchAnalytics.getFeaturesPrunedConfigurationAttributesMap().get(oldId));
			branchAnalytics.getFeaturesPrunedConfigurationAttributesMap().remove(oldId);

			for (int i=0; i<branchAnalytics.getFeaturesConfigurationAttributesList().size(); i++) {
				if (branchAnalytics.getFeaturesConfigurationAttributesList().get(i).id.equals(oldId)) {
					FeatureAttributesPair faetureAtt = branchAnalytics.getFeaturesConfigurationAttributesList().get(i);
					branchAnalytics.getFeaturesConfigurationAttributesList().remove(i);
					faetureAtt.id = newId;
					branchAnalytics.getFeaturesConfigurationAttributesList().add(i, faetureAtt);
					break;
				}
			}

			//no need to update counters - besides id all stay the same
		}

	}

	public void addNonCheckedOutFeatureUnderNonCheckedOutRoot(BaseAirlockItem movedItem, BaseAirlockItem rootNode, REQUEST_ITEM_TYPE reqType) {
		if (reqType.equals(REQUEST_ITEM_TYPE.FEATURES)) { 
			if (unCheckedoutRoot == null) {
				unCheckedoutRoot = new RootItem();
				unCheckedoutRoot.setBranchStatus(BranchStatus.NONE);
				unCheckedoutRoot.setUniqueId(rootNode.getUniqueId());
				unCheckedoutRoot.setLastModified(rootNode.getLastModified());
				unCheckedoutRoot.setSeasonId(rootNode.getSeasonId());
				LinkedList<String> rootMovedtSubFeaturesItems = new LinkedList<String>();
				rootMovedtSubFeaturesItems.add(getItemBranchName(movedItem));
				unCheckedoutRoot.setBranchFeaturesItems(rootMovedtSubFeaturesItems);
				branchFeatures.add(unCheckedoutRoot);
			}
			else {
				unCheckedoutRoot.getBranchFeaturesItems().add(getItemBranchName(movedItem));
			}
		}
		else {
			//PURCHASES
			if (unCheckedoutPurchasesRoot == null) {
				unCheckedoutPurchasesRoot = new RootItem();
				unCheckedoutPurchasesRoot.setBranchStatus(BranchStatus.NONE);
				unCheckedoutPurchasesRoot.setUniqueId(rootNode.getUniqueId());
				unCheckedoutPurchasesRoot.setLastModified(rootNode.getLastModified());
				unCheckedoutPurchasesRoot.setSeasonId(rootNode.getSeasonId());
				LinkedList<String> rootMovedtSubPurchasesItems = new LinkedList<String>();
				rootMovedtSubPurchasesItems.add(getItemBranchName(movedItem));
				unCheckedoutPurchasesRoot.setBranchEntitlementItems(rootMovedtSubPurchasesItems);
				branchPurchases.add(unCheckedoutPurchasesRoot);
			}
			else {
				unCheckedoutPurchasesRoot.getBranchEntitlementItems().add(getItemBranchName(movedItem));
			}
		}
	}

	public void removeNonCheckedOutFeatureFromNonCheckedOutRoot(BaseAirlockItem movedItem, BaseAirlockItem rootNode, REQUEST_ITEM_TYPE reqType) {
		if (reqType.equals(REQUEST_ITEM_TYPE.FEATURES)) {
			if (unCheckedoutRoot != null) {
				String movedItemName = getItemBranchName(movedItem);
				for (int i=0; i<unCheckedoutRoot.getBranchFeaturesItems().size(); i++) {
					if (unCheckedoutRoot.getBranchFeaturesItems().get(i).equals(movedItemName)) {
						unCheckedoutRoot.getBranchFeaturesItems().remove(i);
						break;
					}
				}						
	
				if (unCheckedoutRoot.getBranchFeaturesItems().isEmpty()) {
					removeUnCheckedoutRoot(reqType);								
				}
			}
		}
		else {
			//PURCHASES
			if (unCheckedoutPurchasesRoot != null) {
				String movedItemName = getItemBranchName(movedItem);
				for (int i=0; i<unCheckedoutPurchasesRoot.getBranchEntitlementItems().size(); i++) {
					if (unCheckedoutPurchasesRoot.getBranchEntitlementItems().get(i).equals(movedItemName)) {
						unCheckedoutPurchasesRoot.getBranchEntitlementItems().remove(i);
						break;
					}
				}						
	
				if (unCheckedoutPurchasesRoot.getBranchEntitlementItems().isEmpty()) {
					removeUnCheckedoutRoot(reqType);								
				}
			}
		}
	}

	public void removeNewAndCheckedOutFeatureFromNonCheckedOutRoot(BaseAirlockItem movedItem, BaseAirlockItem rootNode) {
		//if (unCheckedoutRoot != null) {
		//the root is no longer its parent - it was moved under other feature so parent name is not needed
		movedItem.setBranchFeatureParentName(null);

		//since the new/checkedout feature was under unchecked out root - it was a subTree root and should be removed from subtree list
		for (int i=0; i<branchFeatures.size(); i++) {
			BaseAirlockItem subTreeRoot = branchFeatures.get(i);
			if (subTreeRoot.getUniqueId().equals(movedItem.getUniqueId())) {
				branchFeatures.remove(i);
				break;
			}
		}
		//	}		
	}

	private void removeUnCheckedoutRoot(REQUEST_ITEM_TYPE reqType) {
		if (reqType.equals(REQUEST_ITEM_TYPE.FEATURES)) {
			if (unCheckedoutRoot!=null) {
				for (int i=0; i<branchFeatures.size(); i++) {
					BaseAirlockItem subTreeRoot = branchFeatures.get(i);
					if (subTreeRoot.getUniqueId().equals(unCheckedoutRoot.getUniqueId())) {
						branchFeatures.remove(i);
						break;
					}
				}
	
				unCheckedoutRoot = null;	
			}
		}
		else {
			//PURCHASES
			if (unCheckedoutPurchasesRoot!=null) {
				for (int i=0; i<branchPurchases.size(); i++) {
					BaseAirlockItem subTreeRoot = branchPurchases.get(i);
					if (subTreeRoot.getUniqueId().equals(unCheckedoutPurchasesRoot.getUniqueId())) {
						branchPurchases.remove(i);
						break;
					}
				}
	
				unCheckedoutPurchasesRoot = null;	
			}
		}
	}

	public boolean featureIsParentOfBranchSubTree(BaseAirlockItem item, REQUEST_ITEM_TYPE reqType) {
		String itemBranchName = getItemBranchName(item);
		
		LinkedList<BaseAirlockItem> branchItems = reqType.equals(REQUEST_ITEM_TYPE.FEATURES)?branchFeatures:branchPurchases;
				
		for (BaseAirlockItem branchSubTreeRoot:branchItems) {
			if (itemBranchName.equals(branchSubTreeRoot.getBranchFeatureParentName())) {
				return true;
			}
		}
		return false;
	}

	public void duplicationFeaturesAndAnalyticsFromOther(Branch srcBranch, Season season, ServletContext context) throws JSONException {
		branchAirlockItemsBD.clear();
		branchFeatures.clear();

		HashMap<String, String> branchOldToDuplicatedFeaturesId = new HashMap<String, String>();

		ValidationCache tester = new ValidationCache();
		for (BaseAirlockItem subTreeRoot:srcBranch.branchFeatures) {
			//duplicate without creating new ids and with the same minAppVersion
			BaseAirlockItem newSubTreeRoot = subTreeRoot.duplicate(null, season.getUniqueId(), null, branchAirlockItemsBD, branchOldToDuplicatedFeaturesId, context, true, false, tester);
			branchAirlockItemsBD.put(newSubTreeRoot.getUniqueId().toString(), newSubTreeRoot);
			branchFeatures.add(newSubTreeRoot);

			if (newSubTreeRoot.getType().equals(Type.ROOT) && newSubTreeRoot.getBranchStatus().equals(BranchStatus.NONE)) {
				unCheckedoutRoot = (RootItem)newSubTreeRoot;
			}
		}

		for (BaseAirlockItem subTreeRoot:srcBranch.branchPurchases) {
			//duplicate without creating new ids and with the same minAppVersion
			BaseAirlockItem newSubTreeRoot = subTreeRoot.duplicate(null, season.getUniqueId(), null, branchAirlockItemsBD, branchOldToDuplicatedFeaturesId, context, true, false, tester);
			branchAirlockItemsBD.put(newSubTreeRoot.getUniqueId().toString(), newSubTreeRoot);
			branchPurchases.add(newSubTreeRoot);

			if (newSubTreeRoot.getType().equals(Type.ROOT) && newSubTreeRoot.getBranchStatus().equals(BranchStatus.NONE)) {
				unCheckedoutPurchasesRoot = (RootItem)newSubTreeRoot;
			}
		}

		//create list of all the NEW features and entitlements in branch
		Set<String> branchItemIds = branchAirlockItemsBD.keySet();
		ArrayList<String> newItemsIds = new ArrayList<>();
		for (String branchItemId : branchItemIds) {
			if (branchAirlockItemsBD.get(branchItemId).getBranchStatus().equals(BranchStatus.NEW)) {
				BaseAirlockItem item = branchAirlockItemsBD.get(branchItemId);
				newItemsIds.add(item.getUniqueId().toString());
			}
		}

		//replace ids of new features and entitlements + if it is mtx and some feature in branch is pointing to it - change the pointer (since the id is part of the pointer)
		for (String newBranchItemId : newItemsIds) {
			BaseAirlockItem newItem = branchAirlockItemsBD.get(newBranchItemId);
			branchAirlockItemsBD.remove(newBranchItemId);
			String prevId = newItem.getUniqueId().toString();
			newItem.setUniqueId(UUID.randomUUID());
			String newId = newItem.getUniqueId().toString();
			branchOldToDuplicatedFeaturesId.put(prevId, newId);
			branchAirlockItemsBD.put(newId, newItem);
			if (newItem instanceof BaseMutualExclusionGroupItem) {
				String prevBranchName = "mx." + prevId;
				Set<String> curBranchFeatureIds = branchAirlockItemsBD.keySet();
				for (String curBranchFeatureId:curBranchFeatureIds) {
					BaseAirlockItem curItem = branchAirlockItemsBD.get(curBranchFeatureId);
					if (curItem.getBranchFeaturesItems()!=null) {
						for (int i=0; i<curItem.getBranchFeaturesItems().size(); i++) {
							String subFeatureId = curItem.getBranchFeaturesItems().get(i); 
							if (subFeatureId.equals(prevBranchName)) {
								//it is pointing to a mutualExclusion item. Since the mtx id was changed we should update it in the 
								//subTree root as well.									
								subFeatureId = "mx." + newId;
								curItem.getBranchFeaturesItems().set(i, subFeatureId);
							}
						}
					}

					if (curItem.getBranchConfigurationRuleItems()!=null) {
						for (int i=0; i<curItem.getBranchConfigurationRuleItems().size(); i++) {
							String subFeatureId = curItem.getBranchConfigurationRuleItems().get(i); 
							if (subFeatureId.equals(prevBranchName)) {
								//it is pointing to a mutualExclusion item. Since the mtx id was changed we should update it in the 
								//subTree root as well.									
								subFeatureId = "mx." + newId;
								curItem.getBranchConfigurationRuleItems().set(i, subFeatureId);
							}
						}
					}

					if (curItem.getBranchOrderingRuleItems()!=null) {
						for (int i=0; i<curItem.getBranchOrderingRuleItems().size(); i++) {
							String subFeatureId = curItem.getBranchOrderingRuleItems().get(i); 
							if (subFeatureId.equals(prevBranchName)) {
								//it is pointing to a OrderingRuleMutualExclusion item. Since the mtx id was changed we should update it in the 
								//subTree root as well.									
								subFeatureId = "mx." + newId;
								curItem.getBranchOrderingRuleItems().set(i, subFeatureId);
							}
						}
					}
					
					if (curItem.getBranchEntitlementItems()!=null) {
						for (int i=0; i<curItem.getBranchEntitlementItems().size(); i++) {
							String subFeatureId = curItem.getBranchEntitlementItems().get(i); 
							if (subFeatureId.equals(prevBranchName)) {
								//it is pointing to a entitlementsMutualExclusion item. Since the mtx id was changed we should update it in the 
								//subTree root as well.									
								subFeatureId = "mx." + newId;
								curItem.getBranchEntitlementItems().set(i, subFeatureId);
							}
						}
					}
					
					if (curItem.getBranchPurchaseOptionsItems()!=null) {
						for (int i=0; i<curItem.getBranchPurchaseOptionsItems().size(); i++) {
							String subFeatureId = curItem.getBranchPurchaseOptionsItems().get(i); 
							if (subFeatureId.equals(prevBranchName)) {
								//it is pointing to a purchase options MutualExclusion item. Since the mtx id was changed we should update it in the 
								//subTree root as well.									
								subFeatureId = "mx." + newId;
								curItem.getBranchPurchaseOptionsItems().set(i, subFeatureId);
							}
						}
					}

				}
			}			
		}
		
		for (String branchItemId:branchItemIds) {
			BaseAirlockItem branchItem = branchAirlockItemsBD.get(branchItemId);			
			if (branchItem instanceof OrderingRuleItem) {
				//update the sub features ids of the features that are in the master and are not checked out
				String newConfig = OrderingRuleItem.replaceIdsInConfiguration(tester, context, branchOldToDuplicatedFeaturesId, 
						((OrderingRuleItem)branchItem).getConfiguration(), ((OrderingRuleItem)branchItem).getRule(), 
						((OrderingRuleItem)branchItem).getSeasonId().toString(), ((OrderingRuleItem)branchItem).getStage(), 
						((OrderingRuleItem)branchItem).getMinAppVersion());

				((OrderingRuleItem)branchItem).setConfiguration(newConfig);
			}
			
			if (branchItem instanceof FeatureItem) {
				//update the sub entitlement id of the premium features that are in the master and are not checked out
				FeatureItem fi = (FeatureItem)branchItem;
				if (fi.getEntitlement()!=null && !fi.getEntitlement().isEmpty()) {
					String oldSubEntitlementId = fi.getEntitlement();
					String newSubEntitlementId = branchOldToDuplicatedFeaturesId.get(oldSubEntitlementId);
					fi.setEntitlement(newSubEntitlementId);
				}
			}
		}
		
		setAnalytics(srcBranch.getAnalytics().duplicateForNewSeason(season.getUniqueId(), branchOldToDuplicatedFeaturesId, context, seasonId));
	}

	public void clone(Branch other, ServletContext context ) {
		analytics = other.analytics; //NOTE the analytics is not duplicated! It is the same object
		creationDate = other.creationDate;
		creator = other.creator;
		description = other.description;
		name = other.name;
		seasonId = other.seasonId;
		lastModified = other.lastModified;
		uniqueId = other.uniqueId;

		ValidationCache tester = new ValidationCache();
		for (BaseAirlockItem subTreeRoot:other.branchFeatures) {
			HashMap<String, String> branchOldToDuplicatedFeaturesId = new HashMap<String, String> ();
			BaseAirlockItem newSubTreeRoot = subTreeRoot.duplicate(null, seasonId, null, branchAirlockItemsBD, branchOldToDuplicatedFeaturesId, context, true, false, tester);
			branchAirlockItemsBD.put(newSubTreeRoot.getUniqueId().toString(), newSubTreeRoot);
			branchFeatures.add(newSubTreeRoot);

			if (newSubTreeRoot.getType().equals(Type.ROOT) && newSubTreeRoot.getBranchStatus().equals(BranchStatus.NONE)) {
				unCheckedoutRoot = (RootItem)newSubTreeRoot;
			}
		}
		
		for (BaseAirlockItem subTreeRoot:other.branchPurchases) {
			HashMap<String, String> branchOldToDuplicatedFeaturesId = new HashMap<String, String> ();
			BaseAirlockItem newSubTreeRoot = subTreeRoot.duplicate(null, seasonId, null, branchAirlockItemsBD, branchOldToDuplicatedFeaturesId, context, true, false, tester);
			branchAirlockItemsBD.put(newSubTreeRoot.getUniqueId().toString(), newSubTreeRoot);
			branchPurchases.add(newSubTreeRoot);

			if (newSubTreeRoot.getType().equals(Type.ROOT) && newSubTreeRoot.getBranchStatus().equals(BranchStatus.NONE)) {
				unCheckedoutPurchasesRoot = (RootItem)newSubTreeRoot;
			}
		}
	}

	public void addSubTreeRoot(BaseAirlockItem feature) {
		feature.setBranchFeatureParentName(Type.ROOT.toString());
		branchFeatures.add(feature);		
	}

	//only dupliacte the config subTree and add to DB - will be added to relevant feature later
	public void checkoutConfig(UUID parentId, BaseAirlockItem config, ServletContext context, ValidationCache tester) {
		HashMap<String, String> oldToDuplicatedFeaturesId = new HashMap<String, String>();

		BaseAirlockItem checkedOutConfig = config.duplicate(null, null, parentId, branchAirlockItemsBD, oldToDuplicatedFeaturesId, context, false, false, tester);
		setCheckedOutBranchStatus(checkedOutConfig, branchAirlockItemsBD);		
	}

	//only dupliacte the ordering rule subTree and add to DB - will be added to relevant feature later
	public void checkoutOrderingRule(UUID parentId, BaseAirlockItem orderingRule, ServletContext context, ValidationCache tester) {
		HashMap<String, String> oldToDuplicatedFeaturesId = new HashMap<String, String>();

		BaseAirlockItem checkedOutConfig = orderingRule.duplicate(null, null, parentId, branchAirlockItemsBD, oldToDuplicatedFeaturesId, context, false, false, tester);
		setCheckedOutBranchStatus(checkedOutConfig, branchAirlockItemsBD);		
	}

	public boolean newBranchFeaturesIncludes(BaseAirlockItem feature) {
		return areNewBranchFeaturesIncludes(feature);		
	}

	private boolean areNewBranchFeaturesIncludes(BaseAirlockItem item) {
		if (item.getBranchStatus().equals(BranchStatus.NEW)) {
			return true;
		}

		if (item.getConfigurationRuleItems()!=null) {
			for (BaseAirlockItem bai:item.getConfigurationRuleItems()) {
				boolean isNew = areNewBranchFeaturesIncludes(bai);
				if (isNew)
					return true;
			}
		}	

		if (item.getFeaturesItems()!=null) {
			for (BaseAirlockItem bai:item.getFeaturesItems()) {
				boolean isNew = areNewBranchFeaturesIncludes(bai);
				if (isNew)
					return true;
			}
		}	

		if (item.getOrderingRuleItems()!=null) {
			for (BaseAirlockItem bai:item.getOrderingRuleItems()) {
				boolean isNew = areNewBranchFeaturesIncludes(bai);
				if (isNew)
					return true;
			}
		}

		if (item.getEntitlementItems()!=null) {
			for (BaseAirlockItem bai:item.getEntitlementItems()) {
				boolean isNew = areNewBranchFeaturesIncludes(bai);
				if (isNew)
					return true;
			}
		}

		if (item.getPurchaseOptionsItems()!=null) {
			for (BaseAirlockItem bai:item.getPurchaseOptionsItems()) {
				boolean isNew = areNewBranchFeaturesIncludes(bai);
				if (isNew)
					return true;
			}
		}

		return false;			
	}

}
