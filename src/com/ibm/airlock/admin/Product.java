package com.ibm.airlock.admin;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.ibm.airlock.admin.airlytics.JobStatus;
import com.ibm.airlock.admin.cohorts.AirlockCohorts;
import com.ibm.airlock.admin.cohorts.CohortItem;
import com.ibm.airlock.admin.dataimport.AirlyticsDataImport;
import com.ibm.airlock.admin.dataimport.DataImportItem;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.AnalyticsServices;
import com.ibm.airlock.Constants;
import com.ibm.airlock.Constants.Action;
import com.ibm.airlock.Constants.AirlockCapability;
import com.ibm.airlock.Constants.OutputJSONMode;
import com.ibm.airlock.Constants.Stage;
import com.ibm.airlock.Strings;
import com.ibm.airlock.admin.MergeBranch.MergeException;
import com.ibm.airlock.admin.airlytics.entities.AirlyticsEntities;
import com.ibm.airlock.admin.airlytics.entities.Attribute;
import com.ibm.airlock.admin.airlytics.entities.AttributeType;
import com.ibm.airlock.admin.airlytics.entities.Entity;
import com.ibm.airlock.admin.analytics.Experiment;
import com.ibm.airlock.admin.analytics.ExperimentsMutualExclusionGroup;
import com.ibm.airlock.admin.analytics.Variant;
import com.ibm.airlock.admin.authentication.UserInfo;
import com.ibm.airlock.admin.operations.AirlockCapabilities;
import com.ibm.airlock.admin.operations.UserRoleSets;
import com.ibm.airlock.admin.operations.UserRoleSets.UserRoleSet;
import com.ibm.airlock.engine.Environment;


public class Product {
	public static final Logger logger = Logger.getLogger(Product.class.getName());
	
	
	private String name = null;
	private String description = null;
	private UUID uniqueId = null;
	private String codeIdentifier = null;
	private Date lastModified = null; 
	private LinkedList<Season> seasons = new LinkedList<Season>();
	private ExperimentsMutualExclusionGroup  experimentsMutualExclusionGroup = null;
	private AirlockCohorts cohorts = null;
	private AirlyticsEntities entities = null;

	private AirlyticsDataImport dataImports = null;
	private String smartlingProjectId  = null; //optional
	private ReentrantReadWriteLock productLock = new ReentrantReadWriteLock();
	private ReentrantReadWriteLock cohortsProductLock = new ReentrantReadWriteLock();
	private ReentrantReadWriteLock dataImportProductLock = new ReentrantReadWriteLock();
	private Set<AirlockCapability> capabilities = null;//new TreeSet<AirlockCapability>();//optional
	private UserRoleSets productUsers = new UserRoleSets();
	private boolean cohortsWriteNeeded = false;
	
	public ReentrantReadWriteLock getProductLock() {
		return productLock;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public UUID getUniqueId() {
		return uniqueId;
	}
	public void setUniqueId(UUID uniqueId) {
		this.uniqueId = uniqueId;
		experimentsMutualExclusionGroup = new ExperimentsMutualExclusionGroup(uniqueId);
		cohorts = new AirlockCohorts(uniqueId);
		entities = new AirlyticsEntities(uniqueId);
		dataImports = new AirlyticsDataImport(uniqueId);
	}
	public String getCodeIdentifier() {
		return codeIdentifier;
	}
	public void setCodeIdentifier(String codeIdentifier) {
		this.codeIdentifier = codeIdentifier;
	}	
	public Date getLastModified() {
		return lastModified;
	}
	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}
	public LinkedList<Season> getSeasons() {
		return seasons;
	}
	public void setSeasons(LinkedList<Season> seasons) {
		this.seasons = seasons;
	}
	public String getSmartlingProjectId() {
		return smartlingProjectId;
	}
	public void setSmartlingProjectId(String smartlingProjectId) {
		this.smartlingProjectId = smartlingProjectId;
	}
	public ExperimentsMutualExclusionGroup getExperimentsMutualExclusionGroup() {
		return experimentsMutualExclusionGroup;
	}
	public void setExperimentsMutualExclusionGroup(ExperimentsMutualExclusionGroup experimentsMutualExclusionGroup) {
		this.experimentsMutualExclusionGroup = experimentsMutualExclusionGroup;
	}	
	public void addSeason(Season newSeason) {
		seasons.add(newSeason);
	}
	public Set<AirlockCapability> getCapabilities() {
		return capabilities;
	}
	public void setCapabilities(Set<AirlockCapability> capabilities) {
		this.capabilities = capabilities;
	}
	public UserRoleSets getProductUsers() {
		return productUsers;
	}

	public void setProductUsers(UserRoleSets productUsers) {
		this.productUsers = productUsers;
	}

	public void removeSeason(Season seasonToDel, ServletContext context) throws JSONException, IOException, MergeException {
		UUID remSeasonId = seasonToDel.getUniqueId();
		
		if (seasons == null || seasons.size() == 0) {
			logger.warning("Unable to remove version range (season) " + remSeasonId.toString() + " from product " + uniqueId.toString() + ": no version range was found.");
			return;
		}
		
		boolean found = false;
		for (int i=0; i< seasons.size(); i++) {
			if (seasons.get(i).getUniqueId() == remSeasonId) {
				found = true;
				seasons.remove(i);
				break;
			}
		}

		if (!found) {
			logger.warning("Unable to remove version range (season) " + remSeasonId.toString() + " from product " + uniqueId.toString() + ": the specified version range was not found for this product.");
		}
		else {
			//if the season is reporting to analytics and is part of experiment - update other seasons runtime files that are in the experiment
			if (seasonToDel.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection().getFeaturesOnOff().size()>0 || 
				seasonToDel.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection().getInputFieldsForAnalytics().size()>0	||
				seasonToDel.getAnalytics().getGlobalDataCollection().getAnalyticsDataCollection().getFeaturesConfigurationAttributesList().size()>0) {
				AnalyticsServices.writeAllSeasonPariticipatingInGivenSeasonsExperiments(seasonToDel, true, context);
			}
		}		
					
	}
		
	public void addExperiment(Experiment experiment) {
		//after validate - name is unique
		this.experimentsMutualExclusionGroup.addExperiment(experiment);		
	}
	
	//return null if OK, error staring on error
	public String removeExperiment(UUID experimentId) {
		return this.experimentsMutualExclusionGroup.removeExperiment(experimentId);			
	}

	public void addCohort(CohortItem cohort) {
		this.cohorts.addCohort(cohort);
	}

	public String removeCohort(UUID cohortId) {
		return this.cohorts.removeCohort(cohortId);
	}

	public String removeEntity(UUID entityId) {
		return this.entities.removeEntity(entityId);
	}

	public void addEntity(Entity entity) {
		this.entities.addEntity(entity);
	}

	public JSONObject toJson(boolean withSeasons, boolean verbose, boolean withExperiments) throws JSONException {
		return toJson(withSeasons,null,null,verbose, withExperiments);
	}

	public JSONObject toJsonForRuntime() throws JSONException {
		JSONObject res = new JSONObject();
		res.put(Constants.JSON_FIELD_NAME, name);
		res.put(Constants.JSON_PRODUCT_FIELD_CODE_IDENTIFIER, codeIdentifier);		
		res.put(Constants.JSON_FIELD_UNIQUE_ID, uniqueId==null?null:uniqueId.toString());
		
		JSONArray seasonsArr = new JSONArray();
		for (int i=0; i<seasons.size(); i++) {
			seasonsArr.add(seasons.get(i).toJson(false));
		}		
		res.put(Constants.JSON_PRODUCT_FIELD_SEASONS, seasonsArr);
		return res;
	}
	
	public JSONObject toJson(boolean withSeasons,ServletContext context, UserInfo userInfo, boolean verbose, boolean withExperiments) throws JSONException {
		JSONObject res = new JSONObject();
		res.put(Constants.JSON_FIELD_NAME, name);
		res.put(Constants.JSON_FIELD_UNIQUE_ID, uniqueId==null?null:uniqueId.toString());
		res.put(Constants.JSON_FIELD_DESCRIPTION, description);
		res.put(Constants.JSON_FIELD_SMARTLING_PROJECT_ID, smartlingProjectId);		
		res.put(Constants.JSON_PRODUCT_FIELD_CODE_IDENTIFIER, codeIdentifier);
		res.put(Constants.JSON_FIELD_LAST_MODIFIED, lastModified.getTime());
		res.put(Constants.JSON_FIELD_CAPABILITIES, Utilities.capabilitieslistToJsonArray(capabilities));
		if (withSeasons) {
			JSONArray seasonsArr = new JSONArray();
			for (int i=0; i<seasons.size(); i++) {
				seasonsArr.add(seasons.get(i).toJson(verbose));
			}		
			res.put(Constants.JSON_PRODUCT_FIELD_SEASONS, seasonsArr);
		}
		if(userInfo != null) {
			Map<String, ArrayList<String>> followersDB = (Map<String, ArrayList<String>>) context.getAttribute(Constants.FOLLOWERS_PRODUCTS_DB_PARAM_NAME);
			ArrayList<String> followers = followersDB.get(getUniqueId().toString());
			if (followers != null && followers.size() != 0) {
				JSONArray arrFollowers = new JSONArray();
				for (int i = 0; i < followers.size(); ++i) {
					arrFollowers.add(followers.get(i));
				}
				res.put(Constants.JSON_PRODUCT_FIELD_FOLLOWERS, arrFollowers);
				res.put(Constants.JSON_PRODUCT_FIELD_IS_FOLLOWING, followers.contains(userInfo.getId())); 				
			}
		}
		
		if (withExperiments && experimentsMutualExclusionGroup!=null)
			res.put(Constants.JSON_FIELD_EXPERIMENTS, experimentsMutualExclusionGroup.toJson(OutputJSONMode.ADMIN, context, false));
		
		return res;
	}
	
	//if seasonsDB is not null => add seasons to seasonsDB
	public void fromJSON (JSONObject input, Map<String, Season> seasonsDB, ServletContext context) throws JSONException {
		name = (String)input.get(Constants.JSON_FIELD_NAME);
		
		if (input.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && input.get(Constants.JSON_FIELD_UNIQUE_ID) != null) {
			String sStr = (String)input.get(Constants.JSON_FIELD_UNIQUE_ID);			
			uniqueId = UUID.fromString(sStr);
		}
		
		if (input.containsKey(Constants.JSON_FIELD_DESCRIPTION)) 
			description = (String)input.get(Constants.JSON_FIELD_DESCRIPTION);							
		
		if (input.containsKey(Constants.JSON_FIELD_SMARTLING_PROJECT_ID)) 
			smartlingProjectId = (String)input.get(Constants.JSON_FIELD_SMARTLING_PROJECT_ID);							
		
		if (input.containsKey(Constants.JSON_PRODUCT_FIELD_CODE_IDENTIFIER)) 
			codeIdentifier = (String)input.get(Constants.JSON_PRODUCT_FIELD_CODE_IDENTIFIER);
		
		if (input.containsKey(Constants.JSON_PRODUCT_FIELD_SEASONS)) {
			JSONArray seasonsArr =input.getJSONArray(Constants.JSON_PRODUCT_FIELD_SEASONS);
			if (seasonsArr != null && seasonsArr.size()>0) {
				for (int i=0; i<seasonsArr.size(); i++) {
					JSONObject seasonJSONObj = seasonsArr.getJSONObject(i);
					Season s = new Season(false); //dont init root node. Should be read from other json.
					s.fromJSON(seasonJSONObj);
					s.generateEmptyInputSchema(); //will be probably read later from another json
					s.generateEmptyAnalytics(null); //will be probably read later from another json
					s.generateEmptyStreams(); //will be probably read later from another json
					s.generateEmptyNotifications(context); //will be probably read later from another json
					seasons.add(s);			
					if (seasonsDB!=null) {
						seasonsDB.put(s.getUniqueId().toString(), s);
					}
				}
			}						
		}	
		
		if (input.containsKey(Constants.JSON_FIELD_EXPERIMENTS) && input.get(Constants.JSON_FIELD_EXPERIMENTS)!=null) {
			JSONObject experimentsMEGJson = input.getJSONObject(Constants.JSON_FIELD_EXPERIMENTS); //after validation - i know it is json Object
			experimentsMutualExclusionGroup = new ExperimentsMutualExclusionGroup(uniqueId);
			experimentsMutualExclusionGroup.fromJSON(experimentsMEGJson, context);						
		}

					
		if (input.containsKey(Constants.JSON_FIELD_CAPABILITIES) && input.get(Constants.JSON_FIELD_CAPABILITIES)!=null) {
			capabilities = new TreeSet<AirlockCapability>();
			JSONArray capabilitiesArr = input.getJSONArray(Constants.JSON_FIELD_CAPABILITIES);
			for (int i=0; i<capabilitiesArr.size(); i++) {
				String capabilityStr = capabilitiesArr.getString(i);
				AirlockCapability capability = Utilities.strToAirlockCapability(capabilityStr);
				if (capability == null)
					throw new JSONException("invalid capability " + capability);
				capabilities.add(capability);
			}
		}
		else { //if the capabilities are not given and the member is  null - take the capabilities from the global (tenant) capabilities
			if (capabilities == null) {
				AirlockCapabilities airlockCapabilities = (AirlockCapabilities)context.getAttribute(Constants.CAPABILITIES_PARAM_NAME);
				capabilities = Utilities.cloneCapabilitiesSet(airlockCapabilities.getCapabilities());
			}
		}
		
		if (input.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) && input.get(Constants.JSON_FIELD_LAST_MODIFIED)!=null) { 
			long timeInMS = (Long)input.get(Constants.JSON_FIELD_LAST_MODIFIED);
			lastModified = new Date(timeInMS);
		}  else {
			lastModified = new Date();
		}
	}
	

	public JSONObject getExperimentsJson(OutputJSONMode mode, ServletContext context) throws JSONException {
		return experimentsMutualExclusionGroup.toJson(mode, context, false);
	}

	
	
	//return the experiments that this season is part of
	public ArrayList<Experiment> getExprimentsPerSeason (Season season){
		ArrayList<Experiment> res = new ArrayList<Experiment>();
		for (Experiment exp:experimentsMutualExclusionGroup.getExperiments()) {
			if (isSeasonInRange(season, exp.getMinVersion(), exp.getMaxVersion())) {				
				res.add(exp);
			}
		}
		
		return res;
	}
	
	public JSONArray getBranchesArrayByStageForSeason(OutputJSONMode outputMode, ServletContext context, Season season) throws JSONException {
		//only for RUNTIME_DEVELOPMENT and RUNTIME_PRODUCTION modes
		//dont return branches that are connected to disabled variant/experiment
		
		JSONArray res = new JSONArray();
		
		Environment env = new Environment();
		env.setServerVersion(season.getServerVersion()); 
		
		TreeSet<UUID> writtenBranchesIds = new TreeSet<UUID>();
		for (Experiment exp:experimentsMutualExclusionGroup.getExperiments()) {
			if (exp.getEnabled() == false) {
				continue; //skip disabled experiments
			}
			if (isSeasonInRange(season, exp.getMinVersion(), exp.getMaxVersion())) {
				if (outputMode == OutputJSONMode.RUNTIME_PRODUCTION) {
					if (exp.getStage() == Stage.DEVELOPMENT) 
						continue;
				}
				for (Variant var:exp.getVariants()) {
					if (var.getBranchName().equals(Constants.MASTER_BRANCH_NAME))
						continue; //master is a valid branch name but no need to write it in barcnhes section
					
					if (var.getEnabled() == false) {
						continue; //skip disabled variants
					}
					if (outputMode == OutputJSONMode.RUNTIME_PRODUCTION) {
						if (var.getStage() == Stage.DEVELOPMENT) 
							continue;
					}
					
					Branch branch = season.getBranches().getBranchByName(var.getBranchName());
					if (writtenBranchesIds.contains(branch.getUniqueId()))
							continue; //if 2 variant uses the same branch - dont write it twice
					res.add(branch.toJson(outputMode, context, env, true, true, true)); // return analytics on branches in general runtimes files
					writtenBranchesIds.add(branch.getUniqueId());
					
				}				
			}
		}
		return res;
	}
	
	public boolean containSubItemInProductionStage() {
		for (int i=0; i<seasons.size(); i++) {
			if (seasons.get(i).containSubItemInProductionStage()) 
				return true;
		}
		return false;
	}
	
	public boolean containStreamsInProductionStage() {
		for (int i=0; i<seasons.size(); i++) {
			if (seasons.get(i).containStreamsInProductionStage()) 
				return true;
		}
		return false;
	}
	
	public boolean containNotificationsInProductionStage() {
		for (int i=0; i<seasons.size(); i++) {
			if (seasons.get(i).containNotificationsInProductionStage()) 
				return true;
		}
		return false;
	}
	
	public void productDeletionCleanup(ServletContext context, UserInfo userInfo) {
		removeProdExperimentsAndVariantsFromDB(context);
		removeProdSeasonsAssetsDBs(context, userInfo);
		removeProdUserRolesFromBD(context);
		removeProdEntitiesFromDB(context);
		
		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);
		productsDB.remove(uniqueId.toString());
	}
	
	private void removeProdUserRolesFromBD(ServletContext context) {
		@SuppressWarnings("unchecked")
		Map<String, UserRoleSet> usersDB = (Map<String, UserRoleSet>)context.getAttribute(Constants.AIRLOCK_USERS_DB_PARAM_NAME);
	
		LinkedHashMap<String, UserRoleSet> prodUserRoleSets = productUsers.getAirlockUsers();
		for (Map.Entry<String, UserRoleSet> entry : prodUserRoleSets.entrySet()) {
			UserRoleSet urs = entry.getValue();
			usersDB.remove(urs.getUniqueId().toString());
		}
	}

	private void removeProdExperimentsAndVariantsFromDB(ServletContext context) {
		@SuppressWarnings("unchecked")
		Map<String, Variant> variantsDB = (Map<String, Variant>)context.getAttribute(Constants.VARIANTS_DB_PARAM_NAME);
		@SuppressWarnings("unchecked")
		Map<String, Experiment> experimentsDB = (Map<String, Experiment>)context.getAttribute(Constants.EXPERIMENTS_DB_PARAM_NAME);

		if (experimentsMutualExclusionGroup!=null) {
			for (Experiment exp:experimentsMutualExclusionGroup.getExperiments()) {
				for (Variant var:exp.getVariants()) {
					variantsDB.remove(var.getUniqueId().toString());
				}
				experimentsDB.remove(exp.getUniqueId().toString());
			}
		}
	}
	
	private void removeProdEntitiesFromDB(ServletContext context) {
		@SuppressWarnings("unchecked")
		Map<String, Entity> entitiesDB = (Map<String, Entity>)context.getAttribute(Constants.ENTITIES_DB_PARAM_NAME);
		@SuppressWarnings("unchecked")
		Map<String, Attribute> attributesDB = (Map<String, Attribute>)context.getAttribute(Constants.ATTRIBUTES_DB_PARAM_NAME);
		@SuppressWarnings("unchecked")
		Map<String, AttributeType> attributeTypesDB = (Map<String, AttributeType>)context.getAttribute(Constants.ATTRIBUTE_TYPES_DB_PARAM_NAME);

		if (entities!=null) {
			for (Entity entity:entities.getEntities()) {
				for (Attribute attribute:entity.getAttributes()) {
					attributesDB.remove(attribute.getUniqueId().toString());
				}
				for (AttributeType attributeType:entity.getAttributeTypes()) {
					attributeTypesDB.remove(attributeType.getUniqueId().toString());
				}
				entitiesDB.remove(entity.getUniqueId().toString());
			}
		}
	}
	
	//remove the product, seasons, features and branches from DBs	
	private void removeProdSeasonsAssetsDBs(ServletContext context, UserInfo userInfo) {	
		for (int i=0; i<seasons.size(); i++) {
			seasons.get(i).removeSeasonAssetsFromDBs(context, userInfo);
		}		
	}
	
	public ValidationResults validateProductJSON(JSONObject productObj, Map<String, Product> productsDB, ServletContext context) {
		Action action = Action.ADD;
		try {
			if (productObj.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && productObj.get(Constants.JSON_FIELD_UNIQUE_ID)!=null) {
				//if JSON contains uniqueId - update an existing product otherwise create a new product
				action = Action.UPDATE;
			}						
			
			//name
			if (!productObj.containsKey(Constants.JSON_FIELD_NAME) || productObj.get(Constants.JSON_FIELD_NAME) == null || productObj.getString(Constants.JSON_FIELD_NAME).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_NAME), Status.BAD_REQUEST);
			}
							
			String validateNameErr = Utilities.validateName(productObj.getString(Constants.JSON_FIELD_NAME));
			if(validateNameErr!=null) {
				return new ValidationResults(validateNameErr, Status.BAD_REQUEST);
			}
			
			//code identifier
			if (!productObj.containsKey(Constants.JSON_PRODUCT_FIELD_CODE_IDENTIFIER) || productObj.get(Constants.JSON_PRODUCT_FIELD_CODE_IDENTIFIER) == null || productObj.getString(Constants.JSON_PRODUCT_FIELD_CODE_IDENTIFIER).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_PRODUCT_FIELD_CODE_IDENTIFIER), Status.BAD_REQUEST);
			}
			
			String validateCodeIdErr = validateCodeIdentifier(productObj.getString(Constants.JSON_PRODUCT_FIELD_CODE_IDENTIFIER));
			if(validateCodeIdErr!=null) {
				return new ValidationResults(validateCodeIdErr, Status.BAD_REQUEST);
			}
			
			//validate name and codeIdentifier uniqueness
			String updatedName = productObj.getString(Constants.JSON_FIELD_NAME);
			String updatedCodeIdentifier = productObj.getString(Constants.JSON_PRODUCT_FIELD_CODE_IDENTIFIER);		
			Set<String> prodIds = productsDB.keySet();
			for (String prodId:prodIds) {
				Product prod = productsDB.get(prodId);
				if (uniqueId==null || !prodId.equals(uniqueId.toString())) {  //in update - skip yourself
					if (prod.getName().equalsIgnoreCase(updatedName)) {
						return new ValidationResults("A product with the specified name already exists.", Status.BAD_REQUEST);
					}
					
					if (prod.getCodeIdentifier().equals(updatedCodeIdentifier)) {
						return new ValidationResults("A product with the specified codeIdentifier already exists.", Status.BAD_REQUEST);
					}
				}
			}
			
			//seasons should not appear in product add/update
			if (productObj.containsKey(Constants.JSON_PRODUCT_FIELD_SEASONS) && productObj.get(Constants.JSON_PRODUCT_FIELD_SEASONS)!=null) {
				return new ValidationResults("Product seasons should not be specified during product creation and update. Seasons must be created and updated one by one.", Status.BAD_REQUEST);
			}
			
			//capabilities - optional field
			if (productObj.containsKey(Constants.JSON_FIELD_CAPABILITIES) && productObj.get(Constants.JSON_FIELD_CAPABILITIES) != null) {				
				JSONArray capabilitiesArr = productObj.getJSONArray(Constants.JSON_FIELD_CAPABILITIES);
				AirlockCapabilities airlockCapabilities = (AirlockCapabilities)context.getAttribute(Constants.CAPABILITIES_PARAM_NAME);
				ValidationResults res = AirlockCapabilities.validateCapabilitiesList(capabilitiesArr, airlockCapabilities.getCapabilities());
				if (res!=null)
					return res;
				
				//if reducing RUNTIME_ENCRYPTION encryption and some season is configured to encrypt runtime files - throw an error
				boolean orgCapabilitiesRuntimeEncryptionIncluded = capabilities.contains(AirlockCapability.RUNTIME_ENCRYPTION);
				boolean newCapabilitiesRuntimeEncryptionIncluded = new TreeSet<String>(capabilitiesArr).contains(AirlockCapability.RUNTIME_ENCRYPTION.toString());
				if (orgCapabilitiesRuntimeEncryptionIncluded && !newCapabilitiesRuntimeEncryptionIncluded && containSeasonWithRuntimeEncryption()) {
					return new ValidationResults(Strings.failReduceRuntimeEncryptionCapabilityFromProduct, Status.BAD_REQUEST);
				}
			}				
			
			if (action == Action.ADD) {
				//modification date => should not appear in feature creation
				if (productObj.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) && productObj.get(Constants.JSON_FIELD_LAST_MODIFIED)!=null) {
					return new ValidationResults("The lastModified field cannot be specified during product creation.", Status.BAD_REQUEST);
				}
			}
			else { //update
				//modification date must appear
				if (!productObj.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) || productObj.get(Constants.JSON_FIELD_LAST_MODIFIED)==null) {
					return new ValidationResults("The lastModified field is missing. This field must be specified during product update.", Status.BAD_REQUEST);
				}				
				
				//verify that given modification date is not older that current modification date
				long givenModoficationDate = productObj.getLong(Constants.JSON_FIELD_LAST_MODIFIED);  //verify that legal long
				Date givenDate = new Date(givenModoficationDate);
				if (givenDate.before(lastModified)) {
					return new ValidationResults("The product was changed by another user. Refresh your browser and try again.", Status.CONFLICT);			
				}
			}
			
		} catch (JSONException jsne) {
			return new ValidationResults(jsne.getMessage(), Status.BAD_REQUEST);
		}
		catch (ClassCastException cce) {
			return new ValidationResults("Illegal product JSON: " + cce.getMessage(), Status.BAD_REQUEST);
		}
		
		return null;
		
	}
	
	//Return a string with update details.
	//If nothing was changed - return empty string 
	public String updateProduct(JSONObject updatedProductData) throws JSONException {

		StringBuilder updateDetails = new StringBuilder();
		boolean wasChanged = false;
		String updatedName = updatedProductData.getString(Constants.JSON_FIELD_NAME);
		if (!updatedName.equals(name)) {
			updateDetails.append("'name' changed from " + name + " to " + updatedName + ";	");
			name = updatedName;
			wasChanged = true;			
		}
		
		String updatedCodeIdentifier = updatedProductData.getString(Constants.JSON_PRODUCT_FIELD_CODE_IDENTIFIER);
		if (!updatedCodeIdentifier.equals(codeIdentifier)) {
			updateDetails.append("'codeIdentifier' changed from " + codeIdentifier + " to " + updatedCodeIdentifier + ";	");
			codeIdentifier = updatedCodeIdentifier;
			wasChanged = true;
		}
		
		//optional fields
		if (updatedProductData.containsKey(Constants.JSON_FIELD_DESCRIPTION) &&  updatedProductData.get(Constants.JSON_FIELD_DESCRIPTION)!=null) {
			//if missing from json or null - ignore
			String updatedDescription = updatedProductData.getString(Constants.JSON_FIELD_DESCRIPTION);
			if (description == null || !description.equals(updatedDescription)) {
				updateDetails.append("'description' changed from '" + description + "' to '" + updatedDescription + "';	");
				description  = updatedDescription;
				wasChanged = true;
			}
		}
		
		if (updatedProductData.containsKey(Constants.JSON_FIELD_SMARTLING_PROJECT_ID) &&  updatedProductData.get(Constants.JSON_FIELD_SMARTLING_PROJECT_ID)!=null) {
			//if missing from json or null - ignore
			String updatedSmartlingProjectId  = updatedProductData.getString(Constants.JSON_FIELD_SMARTLING_PROJECT_ID);
			if (smartlingProjectId  == null || !smartlingProjectId .equals(updatedSmartlingProjectId)) {
				updateDetails.append("'smartlingProjectId ' changed from '" + smartlingProjectId  + "' to '" + updatedSmartlingProjectId + "';	");
				smartlingProjectId   = updatedSmartlingProjectId;
				wasChanged = true;
			}
		}
		
		if (updatedProductData.containsKey(Constants.JSON_FIELD_CAPABILITIES) && updatedProductData.get(Constants.JSON_FIELD_CAPABILITIES) != null) {
			JSONArray updatedCapabilities = updatedProductData.getJSONArray(Constants.JSON_FIELD_CAPABILITIES);
			String updateCapabilitiesDetails = AirlockCapabilities.updateCapabilities(updatedCapabilities, capabilities);
			
			if (!updateCapabilitiesDetails.isEmpty()) {
				wasChanged = true;
				updateDetails.append(updateCapabilitiesDetails);
				
			}
		}
		
		if (wasChanged) {
			lastModified = new Date();
		}

		if (updateDetails.length()!=0) {
			updateDetails.insert(0,"Product changes: ");
		}
		return updateDetails.toString();
	}
		
	
	//if valid return null, else return the error in output String
	private String validateCodeIdentifier (String codeIdentifier) {
		if (codeIdentifier == null || codeIdentifier.isEmpty())
			return "The codeIdentifier field cannot be null or empty.";
		
		if (!Character.isLetter(codeIdentifier.charAt(0))) {
			return "The codeIdentifier field must start with a letter.";
		}
		
		for (int i=1; i<codeIdentifier.length(); i++) {
			Character c = codeIdentifier.charAt(i);
			if (!Utilities.isEnglishLetter(c) && !Character.isDigit(c) && !c.equals(' ') && !c.equals('.')) {
				return "The codeIdentifier field can contain English letters, digits, spaces, and periods only.";						
			}			
		}
		
		return null;		
	}

	public ArrayList<Season> getSeasonsWithinRange(String minVersion, String maxVersion) {
		ArrayList<Season> res = new ArrayList<Season>();
		for (Season s:seasons) {
			if (isSeasonInRange(s, minVersion, maxVersion)) {
				res.add(s);
			}			
		}
		return res;
	}
	
	public static boolean isSeasonInRange(Season season, String minVersion, String maxVersion) {
		if (season.getMaxVersion() == null || Season.compare(minVersion, season.getMaxVersion()) < 0) {
			//the season's maxVer is higher than the given minVersion
			if (maxVersion == null || Season.compare(season.getMinVersion(), maxVersion) < 0) {
				//the season's minVersion is smaller than the given maxVerion
				return true;			
			}
		}		
		
		return false;
	}
	
	public boolean containExperimentsInProductionStage() {
		if (experimentsMutualExclusionGroup!=null) {
			for (Experiment exp:experimentsMutualExclusionGroup.getExperiments()) {
				if (exp.getStage().equals(Stage.PRODUCTION))
					return true;
			}
		}
		return false;
	}
	
	public boolean isPublishToAnalyticsSvrRequired(JSONObject updatedProductJSON) throws JSONException {
		if (experimentsMutualExclusionGroup.getExperiments().size() == 0)
			return false;
		
		//name
		String updatedName = updatedProductJSON.getString(Constants.JSON_FIELD_NAME);
		if (!updatedName.equals(name)) 
			return true;
		
		return false;
	}
	
	public boolean allSeasonsSupportRuntimeInternalSeparation () {
		for (Season s:seasons) {
			if (!s.isRuntimeInternalSeparationSupported()) {
				return false;
			}
		}
		return true;
	}
	
	public boolean containSeasonWithRuntimeEncryption() {
		for (int i=0; i< seasons.size(); i++) {
			if (seasons.get(i).getRuntimeEncryption()) {
				return true;
			}
		}	
		return false;
	}

	public AirlockCohorts getCohorts() {
		return cohorts;
	}

	public void setCohorts(AirlockCohorts cohorts) {
		this.cohorts = cohorts;
	}

	public AirlyticsEntities getEntities() {
		return entities;
	}

	public void setEntities(AirlyticsEntities entities) {
		this.entities = entities;
	}

	public String updateAirlockCohorts(JSONObject newCohortsDataJSON) throws JSONException {
		return this.cohorts.updateFromJSON(newCohortsDataJSON);
	}



	public AirlyticsDataImport getDataImports() {
		return dataImports;
	}

	public void setDataImports(AirlyticsDataImport dataImports) {
		this.dataImports = dataImports;
	}

	public String updateDataInports(JSONObject newDataImportJSON) throws JSONException {
		return this.dataImports.updateFromJSON(newDataImportJSON);
	}

	public void addDataImport(DataImportItem job) {
		this.dataImports.addJob(job);
	}

	public String removeDataImport(UUID jobId) {
		return this.dataImports.removeJob(jobId);
	}

	public boolean pruneJobs() {
		//if we don't have data import on it, return false
		ValidationResults capabilityValidationRes = Utilities.validateCapability (this, new Constants.AirlockCapability[]{Constants.AirlockCapability.DATA_IMPORT});
		if (capabilityValidationRes!=null)
			return false;
		//if pruneThreshold is not set, return false
		AirlyticsDataImport dataImportData = this.getDataImports();
		if (dataImportData.getPruneThreshold() == null || dataImportData.getPruneThreshold() <= 0) {
			return false;
		}
		Long threshold = dataImportData.getPruneThreshold();
		Date now = new Date();
		boolean didPrune = false;
		List<UUID> foundIds = new ArrayList<>();
		for (DataImportItem item :dataImportData.getJobs()) {
			if (item.getStatus() != JobStatus.RUNNING) {
				Date lastModified = item.getLastModified() != null ? item.getCreationDate() : item.getLastModified();
				long elapsedTime = now.getTime()-lastModified.getTime();
				double elapsedTimeMinutes = (elapsedTime/1000.0)/60.0;
				if (elapsedTimeMinutes > threshold) {
					logger.info("PRUNNING data import job with id "+item.getUniqueId()+", "+item.getName());
					foundIds.add(item.getUniqueId());
					didPrune = true;
				}
			}
		}

		for (UUID uId : foundIds) {
			this.removeDataImport(uId);
		}
		return didPrune;
	}

	public ReentrantReadWriteLock getCohortsProductLock() {
		return cohortsProductLock;
	}

	public ReentrantReadWriteLock getDataImportProductLock() {
		return dataImportProductLock;
	}

	public boolean isCohortsWriteNeeded() {
		return cohortsWriteNeeded;
	}

	public void setCohortsWriteNeeded(boolean cohortsWriteNeeded) {
		this.cohortsWriteNeeded = cohortsWriteNeeded;
	}
}
