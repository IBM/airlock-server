package com.ibm.airlock.admin;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Constants.Action;
import com.ibm.airlock.Constants.AirlockCapability;
import com.ibm.airlock.Constants.OutputJSONMode;
import com.ibm.airlock.Constants.Stage;
import com.ibm.airlock.Constants.UtilityType;
import com.ibm.airlock.ProductServices;
import com.ibm.airlock.Strings;
import com.ibm.airlock.admin.translations.OriginalString;
import com.ibm.airlock.admin.translations.OriginalStrings;
import com.ibm.airlock.engine.Environment;
import com.ibm.airlock.engine.VerifyRule;
import com.ibm.airlock.engine.Version;


import com.ibm.airlock.admin.BaseAirlockItem.Type;
import com.ibm.airlock.admin.MergeBranch.MergeException;
import com.ibm.airlock.admin.analytics.AirlockAnalytics;
import com.ibm.airlock.admin.analytics.Experiment;
import com.ibm.airlock.admin.analytics.Variant;
import com.ibm.airlock.admin.authentication.UserInfo;
import com.ibm.airlock.admin.notifications.AirlockNotification;
import com.ibm.airlock.admin.notifications.AirlockNotificationsCollection;
import com.ibm.airlock.admin.operations.AirlockChangeContent;
import com.ibm.airlock.admin.purchases.EntitlementItem;
import com.ibm.airlock.admin.serialize.AirlockFilesWriter;
import com.ibm.airlock.admin.streams.AirlockStream;
import com.ibm.airlock.admin.streams.AirlockStreamsCollection;
import com.ibm.airlock.admin.streams.StreamsEvents;

public class Season {
	public static final Logger logger = Logger.getLogger(Season.class.getName());

	public class Utilities {
		//list of the utilities
		LinkedList<AirlockUtility> utilitiesList = new LinkedList<AirlockUtility>();

		public LinkedList<AirlockUtility> getUtilitiesList() {
			return utilitiesList;
		}
		
		public JSONObject toJson(Stage stage) throws JSONException {
			JSONObject res = new JSONObject();
			JSONArray utilitiesArr = new JSONArray();
			for (AirlockUtility alUtil : utilitiesList) {
				if (stage != null && alUtil.getStage() == Stage.DEVELOPMENT && stage == Stage.PRODUCTION)
					continue; //dont return utils in dev when prod is requested
								
				JSONObject auObj = alUtil.toJson();
				utilitiesArr.add(auObj);				
			}
			res.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, uniqueId==null?null:uniqueId.toString());
			res.put(Constants.JSON_FIELD_UTILITIES, utilitiesArr);

			return res;
		}

		//called in server init stage - reading the utilities from files in s3
		public void fromJSON(JSONArray input, Map<String, AirlockUtility> utilitiesDB) throws JSONException {
			for (int i=0; i<input.length(); i++ ) {
				JSONObject alUtilityJSON = input.getJSONObject(i);
				AirlockUtility alUtility = new AirlockUtility();
				alUtility.fromJSON(alUtilityJSON);
				addAirlockUtility(alUtility);
				if (utilitiesDB!=null)
					utilitiesDB.put(alUtility.getUniqueId().toString(), alUtility);
			}			
		}

		public void addAirlockUtility(AirlockUtility alUtility) {
			utilitiesList.add(alUtility);
		}


		//return null if OK, error staring on error
		public String removeAirlockUtility(AirlockUtility alToRem, Season season, ServletContext context) {
			String alUtilityId = alToRem.getUniqueId().toString(); 
			if (utilitiesList == null || utilitiesList.size() == 0) {
				return "Unable to remove utility " + alUtilityId + " from season " + uniqueId.toString() + ": season has no utilities.";				
			}


			//validate other utilities (validate that this utility is not used by other utilities)
			//String remainingFunctions = generateUtilityCodeSectionFromMinAppVer(alToRem.getMinAppVersion(), null, alToRem.getUniqueId().toString());
			String remainingFunctions = generateUtilityCodeExceptSpecified (alToRem.getUniqueId());
			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion());
			try {
				//this only verify syntax not removal of a function that is in use by other function
				VerifyRule.checkValidity("true", "{context:{}}", remainingFunctions, "{}", false, null, alToRem.getStage().toString(), env);
			} catch (ValidationException e) {
				return "Unable to remove utility '" + alUtilityId + "' from season '" + uniqueId.toString() + "': " + e.getMessage();
			}	

			if(alToRem.getType().equals(UtilityType.MAIN_UTILITY)) {
				//go over the season's features and configRules and validate that no rule or configuration uses the missing functions
				try {
					String verFuncRes = InputSchema.validateRulesAndConfigWithNewSchemaOrChangedUtility (season, season.getInputSchema().getMergedSchema(), context, alToRem.getUniqueId(), null, null, null, null, null, null, null, null);
					if (verFuncRes != null)
						return "Unable to remove utility '" + alUtilityId + "' from season '" + uniqueId.toString() + "': " + verFuncRes;
	
				} catch (GenerationException ge) {
					return "Failed to generate the data sample: " + ge.getMessage();
				} catch (JSONException jsne) {
					return jsne.getMessage();
				} catch (InterruptedException ie) {
					return ie.getMessage(); 
				} catch (ExecutionException ee) {
					return ee.getMessage();
				}			
			}	
			else { //STREAMS_UTILITY
				//go over the season's streams and validate that no stream is using the missing functions
				String verStreamsRes =  season.getStreams().validateStreamsWithChangedOrDeletedUtility(context, season, alUtilityId, null, null);
				if (verStreamsRes != null)
					return "Unable to remove streams utility '" + alUtilityId + "' from season '" + uniqueId.toString() + "': " + verStreamsRes;

			}
			for (int i=0; i< utilitiesList.size(); i++) {
				if (utilitiesList.get(i).getUniqueId().toString().equals(alUtilityId)) {
					utilitiesList.remove(i);
					return null;
				}
			}

			return "Unable to remove utility " + alUtilityId + " from season " + uniqueId.toString() + ": The specified utility does not exist under this season.";
		}

		//this function look for features in higher minAppVersion than the updated/removed utility minAppVersion.
		//This way we can find out if a function in use was removed from the updated/removed utility
		public String validateMissingFuncsInHigherItems(Set<String> removedFunctions, BaseAirlockItem root, Stage utilStage, String utilMinAppVersion) {
			if (root.getFeaturesItems()!=null) {
				for (int i=0; i<root.getFeaturesItems().size(); i++) {
					if  (root.getFeaturesItems().get(i).getType() == Type.FEATURE) {
						FeatureItem featureItem = (FeatureItem)root.getFeaturesItems().get(i);
						//if rule exist and the feature is in higher minAppVersion than the utility and in the matching stage
						if (featureItem.getRule()!=null && featureItem.getRule().getRuleString()!=null && compare(utilMinAppVersion, featureItem.getMinAppVersion()) <= 0 &&
								(utilStage == Stage.PRODUCTION || featureItem.getStage() == Stage.DEVELOPMENT)) {

							Set<String> missingFunctions = VerifyRule.findFunctionsInRule(removedFunctions, featureItem.getRule().getRuleString());
							if (missingFunctions!=null && missingFunctions.size()>0) {
								return reportMissingFunctionsInFeature(featureItem, missingFunctions);
							}
						}
					}

					String res = validateMissingFuncsInHigherItems(removedFunctions, root.getFeaturesItems().get(i), utilStage, utilMinAppVersion);
					if (res!=null)
						return res;
				}
			}
			if (root.getConfigurationRuleItems() != null) {
				for (int i=0; i<root.getConfigurationRuleItems().size(); i++) {
					if (root.getConfigurationRuleItems().get(i).getType() == Type.CONFIGURATION_RULE) {
						ConfigurationRuleItem crItem = (ConfigurationRuleItem)root.getConfigurationRuleItems().get(i);

						if (compare(utilMinAppVersion, crItem.getMinAppVersion()) <= 0 &&
								(utilStage == Stage.PRODUCTION || crItem.getStage() == Stage.DEVELOPMENT)) {

							if (crItem.getRule()!=null && crItem.getRule().getRuleString()!=null) {
								Set<String> missingFunctions = VerifyRule.findFunctionsInRule(removedFunctions, crItem.getRule().getRuleString());
								if (missingFunctions!=null && missingFunctions.size()>0) {
									return reportMissingFunctionsInConfigRule(crItem, missingFunctions, "rule");
								}
							}

							if (crItem.getConfiguration()!=null && !crItem.getConfiguration().isEmpty() ) {
								Set<String> missingFunctions = VerifyRule.findFunctionsInRule(removedFunctions, crItem.getConfiguration());
								if (missingFunctions!=null && missingFunctions.size()>0) {
									return reportMissingFunctionsInConfigRule(crItem, missingFunctions, "configuration");
								}
							}
						}						

					}
					String res = validateMissingFuncsInHigherItems(removedFunctions, root.getConfigurationRuleItems().get(i), utilStage, utilMinAppVersion);
					if (res!=null)
						return res;
				}
			}

			if (root.getEntitlementItems()!=null) {
				for (int i=0; i<root.getEntitlementItems().size(); i++) {
					if  (root.getEntitlementItems().get(i).getType() == Type.ENTITLEMENT) {
						FeatureItem entitlementItem = (FeatureItem)root.getEntitlementItems().get(i);
						//if rule exist and the entitlement is in higher minAppVersion than the utility and in the matching stage
						if (entitlementItem.getRule()!=null && entitlementItem.getRule().getRuleString()!=null && compare(utilMinAppVersion, entitlementItem.getMinAppVersion()) <= 0 &&
								(utilStage == Stage.PRODUCTION || entitlementItem.getStage() == Stage.DEVELOPMENT)) {

							Set<String> missingFunctions = VerifyRule.findFunctionsInRule(removedFunctions, entitlementItem.getRule().getRuleString());
							if (missingFunctions!=null && missingFunctions.size()>0) {
								return reportMissingFunctionsInFeature(entitlementItem, missingFunctions);
							}
						}
					}

					String res = validateMissingFuncsInHigherItems(removedFunctions, root.getEntitlementItems().get(i), utilStage, utilMinAppVersion);
					if (res!=null)
						return res;
				}
			}

			if (root.getPurchaseOptionsItems()!=null) {
				for (int i=0; i<root.getPurchaseOptionsItems().size(); i++) {
					if  (root.getPurchaseOptionsItems().get(i).getType() == Type.PURCHASE_OPTIONS) {
						FeatureItem poItem = (FeatureItem)root.getPurchaseOptionsItems().get(i);
						//if rule exist and the purchase options is in higher minAppVersion than the utility and in the matching stage
						if (poItem.getRule()!=null && poItem.getRule().getRuleString()!=null && compare(utilMinAppVersion, poItem.getMinAppVersion()) <= 0 &&
								(utilStage == Stage.PRODUCTION || poItem.getStage() == Stage.DEVELOPMENT)) {

							Set<String> missingFunctions = VerifyRule.findFunctionsInRule(removedFunctions, poItem.getRule().getRuleString());
							if (missingFunctions!=null && missingFunctions.size()>0) {
								return reportMissingFunctionsInFeature(poItem, missingFunctions);
							}
						}
					}

					String res = validateMissingFuncsInHigherItems(removedFunctions, root.getPurchaseOptionsItems().get(i), utilStage, utilMinAppVersion);
					if (res!=null)
						return res;
				}
			}

			return null;
		}


		//this function look for features in lower minAppVersion than the updated utility minAppVersion.
		//This way we can find out if the utility was updated to lower minAppVersion but there are lower features that uses it
		public String validateMissingFuncsInLowerItems(Set<String> removedFunctions, BaseAirlockItem root, Stage utilStage, String utilMinAppVersion) {
			if (root.getFeaturesItems()!=null) {
				for (int i=0; i<root.getFeaturesItems().size(); i++) {
					if  (root.getFeaturesItems().get(i).getType() == Type.FEATURE) {
						FeatureItem featureItem = (FeatureItem)root.getFeaturesItems().get(i);
						//if rule exist and the feature is in higher minAppVersion than the utility and in the matching stage
						if (featureItem.getRule()!=null && featureItem.getRule().getRuleString()!=null && compare(utilMinAppVersion, featureItem.getMinAppVersion()) > 0 &&
								(utilStage == Stage.PRODUCTION || featureItem.getStage() == Stage.DEVELOPMENT)) {

							Set<String> missingFunctions = VerifyRule.findFunctionsInRule(removedFunctions, featureItem.getRule().getRuleString());
							if (missingFunctions!=null && missingFunctions.size()>0) {
								return reportMissingFunctionsInFeature(featureItem, missingFunctions);
							}
						}
					}

					String res = validateMissingFuncsInLowerItems(removedFunctions, root.getFeaturesItems().get(i), utilStage, utilMinAppVersion);
					if (res!=null)
						return res;
				}
			}
			if (root.getConfigurationRuleItems() != null) {
				for (int i=0; i<root.getConfigurationRuleItems().size(); i++) {
					if (root.getConfigurationRuleItems().get(i).getType() == Type.CONFIGURATION_RULE) {
						ConfigurationRuleItem crItem = (ConfigurationRuleItem)root.getConfigurationRuleItems().get(i);

						if (compare(utilMinAppVersion, crItem.getMinAppVersion()) > 0 &&
								(utilStage == Stage.PRODUCTION || crItem.getStage() == Stage.DEVELOPMENT)) {

							if (crItem.getRule()!=null && crItem.getRule().getRuleString()!=null) {
								Set<String> missingFunctions = VerifyRule.findFunctionsInRule(removedFunctions, crItem.getRule().getRuleString());
								if (missingFunctions!=null && missingFunctions.size()>0) {
									return reportMissingFunctionsInConfigRule(crItem, missingFunctions, "rule");
								}
							}

							if (crItem.getConfiguration()!=null && !crItem.getConfiguration().isEmpty() ) {
								Set<String> missingFunctions = VerifyRule.findFunctionsInRule(removedFunctions, crItem.getConfiguration());
								if (missingFunctions!=null && missingFunctions.size()>0) {
									return reportMissingFunctionsInConfigRule(crItem, missingFunctions, "configuration");
								}
							}
						}						

					}
					String res = validateMissingFuncsInLowerItems(removedFunctions, root.getConfigurationRuleItems().get(i), utilStage, utilMinAppVersion);
					if (res!=null)
						return res;
				}
			}
			
			if (root.getEntitlementItems()!=null) {
				for (int i=0; i<root.getEntitlementItems().size(); i++) {
					if  (root.getEntitlementItems().get(i).getType() == Type.ENTITLEMENT) {
						FeatureItem entitlementItem = (FeatureItem)root.getEntitlementItems().get(i);
						//if rule exist and the entitlement is in higher minAppVersion than the utility and in the matching stage
						if (entitlementItem.getRule()!=null && entitlementItem.getRule().getRuleString()!=null && compare(utilMinAppVersion, entitlementItem.getMinAppVersion()) > 0 &&
								(utilStage == Stage.PRODUCTION || entitlementItem.getStage() == Stage.DEVELOPMENT)) {

							Set<String> missingFunctions = VerifyRule.findFunctionsInRule(removedFunctions, entitlementItem.getRule().getRuleString());
							if (missingFunctions!=null && missingFunctions.size()>0) {
								return reportMissingFunctionsInFeature(entitlementItem, missingFunctions);
							}
						}
					}

					String res = validateMissingFuncsInLowerItems(removedFunctions, root.getEntitlementItems().get(i), utilStage, utilMinAppVersion);
					if (res!=null)
						return res;
				}
			}
			
			if (root.getPurchaseOptionsItems()!=null) {
				for (int i=0; i<root.getPurchaseOptionsItems().size(); i++) {
					if  (root.getPurchaseOptionsItems().get(i).getType() == Type.PURCHASE_OPTIONS) {
						FeatureItem poItem = (FeatureItem)root.getPurchaseOptionsItems().get(i);
						//if rule exist and the entitlement is in higher minAppVersion than the utility and in the matching stage
						if (poItem.getRule()!=null && poItem.getRule().getRuleString()!=null && compare(utilMinAppVersion, poItem.getMinAppVersion()) > 0 &&
								(utilStage == Stage.PRODUCTION || poItem.getStage() == Stage.DEVELOPMENT)) {

							Set<String> missingFunctions = VerifyRule.findFunctionsInRule(removedFunctions, poItem.getRule().getRuleString());
							if (missingFunctions!=null && missingFunctions.size()>0) {
								return reportMissingFunctionsInFeature(poItem, missingFunctions);
							}
						}
					}

					String res = validateMissingFuncsInLowerItems(removedFunctions, root.getPurchaseOptionsItems().get(i), utilStage, utilMinAppVersion);
					if (res!=null)
						return res;
				}
			}

			return null;
		}


		private String reportMissingFunctionsInFeature(FeatureItem featureItem, Set<String> missingFunctions) {
			StringBuilder sb = new StringBuilder();
			sb.append("The rule: '");
			sb.append(featureItem.getRule().getRuleString());
			sb.append("' of the feature '");
			sb.append(featureItem.getName());
			sb.append("' is using the following functions that were either removed or the function's minimum version is now higher than the feature's minimum version: ");
			for (String func:missingFunctions) {
				sb.append(func);
				sb.append(", ");
			}

			return sb.toString();				
		}

		private String reportMissingFunctionsInConfigRule(ConfigurationRuleItem crItem, Set<String> missingFunctions, String ruleOrConfig) {
			StringBuilder sb = new StringBuilder();
			sb.append("The '" + ruleOrConfig + "': ");
			sb.append(crItem.getRule().getRuleString());
			sb.append(" of the configuration ");
			sb.append(crItem.getName());
			sb.append(" is using the following functions that were removed or the function's minimum version is now higher than the configuration's minimum version:  ");
			for (String func:missingFunctions) {
				sb.append(func);
				sb.append(", ");
			}

			return sb.toString();				
		}

		public LinkedList<AirlockUtility> duplicateForNewSeason (String minVersion, UUID newSeasonId, Map<String, AirlockUtility> utilitiesDB) {
			LinkedList<AirlockUtility> res = new LinkedList<AirlockUtility>();

			for (int i=0; i<utilitiesList.size(); i++) {
				AirlockUtility alUtil = utilitiesList.get(i).duplicateForNewSeason(minVersion, newSeasonId);
				res.add(alUtil);
				utilitiesDB.put(alUtil.getUniqueId().toString(), alUtil);
			}

			return res;
		}

		//if stage == null return all
		//if minAppVersion == null return all
		//return all utilities up-to this version (including)
		public String generateUtilityCodeSectionForStageAndType(Stage stage, String aditionalUtility, String ommitUtilityId, Stage additionalUtilStage, UtilityType utilType) {
			StringBuilder sb = new StringBuilder(); 
			for (int i=0; i<utilitiesList.size(); i++) {

				if (!utilType.equals(utilitiesList.get(i).getType()))
					continue; 
				
				if (stage != null && utilitiesList.get(i).getStage() == Stage.DEVELOPMENT && stage == Stage.PRODUCTION) 
					continue; //dont use utils in dev when prod is requested
				
				if (ommitUtilityId!=null && ommitUtilityId.equals(utilitiesList.get(i).getUniqueId().toString()) )
					continue;

				sb.append(utilitiesList.get(i).getUtility());
				sb.append("\n");
			}

			
			if (aditionalUtility!=null) {
				if (stage != null && additionalUtilStage!=null && additionalUtilStage == Stage.DEVELOPMENT && stage == Stage.PRODUCTION) 
					 return sb.toString(); //dont use utils in dev when prod is requested

				sb.append(aditionalUtility);
				sb.append("\n");
			}

			return sb.toString();
		}
		
		public String generateUtilityCodeExceptSpecified(UUID utilityId) {
			StringBuilder sb = new StringBuilder(); 
			for (int i=0; i<utilitiesList.size(); i++) {

				if (utilitiesList.get(i).getUniqueId().equals(utilityId)) {
					continue;
				}

				sb.append(utilitiesList.get(i).getUtility());
				sb.append("\n");
			}

			return sb.toString();
		}

		public List<String> utilitiesIdsUsingString(String stringId) {
			List<String> utilitiesIds = new ArrayList<>();
			for (int i=0; i<utilitiesList.size(); i++) {
				AirlockUtility currUtil = utilitiesList.get(i);
				if (currUtil.isUsingString(stringId)) {
					utilitiesIds.add(currUtil.getUniqueId().toString());
				}
			}
			return utilitiesIds;
		}

		public void addInitialUtility(ServletContext context) {
			@SuppressWarnings("unchecked")
			Map<String, AirlockUtility> utilitiesDB = (Map<String, AirlockUtility>)context.getAttribute(Constants.UTILITIES_DB_PARAM_NAME);

			String javascriptUtils = (String)context.getAttribute(Constants.JAVASCRIPT_UTILITIES_PARAM_NAME);
			AirlockUtility alUtil = new AirlockUtility();
			alUtil.setUtility(javascriptUtils);
			alUtil.setSeasonId(uniqueId);
			alUtil.setName(Constants.BASIC_UTILITY_NAME);
			alUtil.setStage(Stage.PRODUCTION);
			alUtil.setUniqueId(UUID.randomUUID());
			alUtil.setLastModified(new Date());

			addAirlockUtility(alUtil);
			utilitiesDB.put(alUtil.getUniqueId().toString(), alUtil);

		}
		
		public JSONObject getUtilitiesInfo(Stage stage, UtilityType type) throws JSONException {
			JSONObject res = new JSONObject();
			
			String utilStr = generateUtilityCodeSectionForStageAndType(stage, null, null, null, type);
			
			TreeMap<String, String[]> functions = VerifyRule.findFunctionSignatures(utilStr);
			
			Set<String> functionNames = functions.keySet();
			for (String func:functionNames) {
				res.put(func, functions.get(func));
			}
			
			return res;
		}

		public String addNames(Season season) {
			StringBuilder sb = new StringBuilder();
			for (AirlockUtility util:utilitiesList) {
				if (util.getName() == null) {
					String newName = AirlockUtility.generateUtilityName(season);
					util.setName(newName);
					util.setLastModified(new Date());
					sb.append("Utility " + util.getUniqueId().toString() +" name changed to: " + newName + "\n");
				}
			}			
			
			return sb.toString();			
		}
	}


	

	private UUID uniqueId = null;	
	private UUID productId = null;
	private String minVersion = null;
	private String maxVersion = null;
	private Date lastModified = null; 	
	private InputSchema inputSchema = null;
	private Utilities utilities = new Utilities();
	private OriginalStrings originalStrings = null;
	private String productionChangedFilePath = null;
	private boolean isOldRuntimeFileExists = false; //if AirlockRuntime.json file exist which means it is an old season.	
	private String serverVersion = null;
	private AirlockAnalytics analytics = null;
	private List<String> followersIds= new ArrayList<>();
	private boolean purged = false;
	private BranchesCollection branches = null;
	private AirlockStreamsCollection streams = null;
	private StreamsEvents streamsEvents = null;
	private AirlockNotificationsCollection notifications = null;
	private String encryptionKey = null;
	private boolean runtimeEncryption = false;

	private RootItem root = new RootItem(); //holding and managing the season's features list
	
	private RootItem entitlementsRoot = new RootItem(); //holding and managing the season's purchases list

	public Season(boolean initRoot) {
		if (initRoot) {
			root.setLastModified(new Date());
			root.setUniqueId(UUID.randomUUID());
		}		
		originalStrings = new OriginalStrings(this);
		branches = new BranchesCollection(this);
		
		//entitlementsRoot should always be initialized for seasons with prev versions
		entitlementsRoot.setLastModified(new Date());
		entitlementsRoot.setUniqueId(UUID.randomUUID());
	}
	
	public static String generateEncryptionKey() {
		return RandomStringUtils.randomAlphanumeric(16).toUpperCase(); // 128 bit key
	}

	public void resetEncryptionKey() {
		this.encryptionKey = generateEncryptionKey();
	}
	
	public Season(ServletContext context) {
		originalStrings = new OriginalStrings(this);
		setAnalytics(new AirlockAnalytics(uniqueId));		
	}

	public void generateEmptyInputSchema() throws JSONException {
		inputSchema = new InputSchema(uniqueId);
	}
	
	public void generateEmptyStreams() {
		streams = new AirlockStreamsCollection(uniqueId);
		streams.setLastModified(new Date());
		streamsEvents = new StreamsEvents(uniqueId);
	}
	
	public void generateEmptyNotifications(ServletContext context) {
		notifications = new AirlockNotificationsCollection(uniqueId, context);		
	}
	
	public void generateEmptyAnalytics(ServletContext context) {
		analytics = new AirlockAnalytics(uniqueId);
	}

	public UUID getUniqueId() {
		return uniqueId;
	}
	public void setUniqueId(UUID uniqueId) {
		this.uniqueId = uniqueId;
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
	public Date getLastModified() {
		return lastModified;
	}
	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}
	public InputSchema getInputSchema() {
		return this.inputSchema;
	}
	public void setInputSchema(InputSchema inputSchemas) {
		this.inputSchema = inputSchemas;
	}
	public AirlockStreamsCollection getStreams() {
		return streams;
	}
	public void setStreams(AirlockStreamsCollection streams) {
		this.streams = streams;
	}
	public boolean getRuntimeEncryption() {
		return runtimeEncryption;
	}
	public void setRuntimeEncryption (boolean runtimeEncryption) {
		this.runtimeEncryption = runtimeEncryption;
	}
	// allow Smartling background thread to check for deleted seasons
	public boolean isPurged() {
		return purged;
	}
	public void purge() {
		purged = true;
		originalStrings = new OriginalStrings();
	}

	public RootItem getRoot() {
		return root;
	}

	public RootItem getEntitlementsRoot() {
		return entitlementsRoot;
	}
	public void setRoot(RootItem root) {
		this.root = root;
	}
	public void setEntitlementsRoot(RootItem entitlementsRoot) {
		this.entitlementsRoot = entitlementsRoot;
	}
	public Utilities getUtilities() {
		return utilities;
	}

	public void setUtilities(Utilities utilities) {
		this.utilities = utilities;
	}

	public OriginalStrings getOriginalStrings() {
		return originalStrings;
	}

	public void setOriginalStrings(OriginalStrings originalStrings) {
		this.originalStrings = originalStrings;
	}
	
	public AirlockAnalytics getAnalytics() {
		return analytics;
	}

	public void setAnalytics(AirlockAnalytics analytics) {
		this.analytics = analytics;
	}

	public List<String> getFollowersId() {
		return followersIds;
	}

	public void addFollowersIds(String newId) {
		if (!followersIds.contains(newId)){
			followersIds.add(newId);
		}
	}
	public void removeFollowersId(String newId) {
		if (followersIds.contains(newId)) {
			followersIds.remove(newId);
		}
	}
	
	public BranchesCollection getBranches() {
		return branches;
	}

	public void setBranches(BranchesCollection branches) {
		this.branches = branches;
	}
	
	public StreamsEvents getStreamsEvents() {
		return streamsEvents;
	}

	public void setStreamsEvents(StreamsEvents streamsEvents) {
		this.streamsEvents = streamsEvents;
	}

	public AirlockNotificationsCollection getNotifications() {
		return notifications;
	}

	public void setNotifications(AirlockNotificationsCollection notifications) {
		this.notifications = notifications;
	}

	public String getEncryptionKey() {
		return encryptionKey;
	}

	public void setEncryptionKey(String encryptionKey) {
		this.encryptionKey = encryptionKey;
	}
	
	public String getProductionChangedFilePath(String separator) {
		if (productionChangedFilePath == null)			
			productionChangedFilePath = Constants.SEASONS_FOLDER_NAME+separator+productId.toString()+separator+uniqueId.toString()+separator+Constants.PRODUCTION_CHANGED_FILE_NAME;

		return productionChangedFilePath;		
	}

	public JSONObject toJson(boolean verbose) throws JSONException {
		JSONObject res = new JSONObject();
		res.put(Constants.JSON_FIELD_UNIQUE_ID, uniqueId==null?null:uniqueId.toString());
		res.put(Constants.JSON_FIELD_PRODUCT_ID, productId==null?null:productId.toString()); 
		res.put(Constants.JSON_SEASON_FIELD_MIN_VER, minVersion);
		res.put(Constants.JSON_SEASON_FIELD_MAX_VER, maxVersion);	
		res.put(Constants.JSON_FIELD_RUNTIME_ENCRYPTION, runtimeEncryption);	
		res.put(Constants.JSON_FIELD_LAST_MODIFIED, lastModified.getTime());
		if (verbose) {
			res.put(Constants.JSON_FIELD_SERVER_VERSION, serverVersion);
			
			JSONArray supportedPlatforms = new JSONArray();
			supportedPlatforms.add(Constants.Platform.Android.toString());
			supportedPlatforms.add(Constants.Platform.iOS.toString());
			Environment env = new Environment();
			env.setServerVersion(serverVersion);
			if(ProductServices.isCSharpConstantsSupported(env)) {
				supportedPlatforms.add(Constants.Platform.c_sharp.toString());
			}
			res.put(Constants.JSON_FIELD_PLATRORMS, supportedPlatforms);		
		}
		return res;
	}

	public void fromJSON (JSONObject input) throws JSONException {
		if (input.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && input.get(Constants.JSON_FIELD_UNIQUE_ID) != null) {
			String sStr = input.getString(Constants.JSON_FIELD_UNIQUE_ID);			
			uniqueId = UUID.fromString(sStr);		
		}

		if (input.containsKey(Constants.JSON_FIELD_PRODUCT_ID) && input.get(Constants.JSON_FIELD_PRODUCT_ID) != null) {
			String pStr = input.getString(Constants.JSON_FIELD_PRODUCT_ID);			
			productId = UUID.fromString(pStr);		
		}

		if (input.containsKey(Constants.JSON_SEASON_FIELD_MIN_VER) && input.get(Constants.JSON_SEASON_FIELD_MIN_VER) != null) 
			minVersion = input.getString(Constants.JSON_SEASON_FIELD_MIN_VER);

		if (input.containsKey(Constants.JSON_SEASON_FIELD_MAX_VER) && input.get(Constants.JSON_SEASON_FIELD_MAX_VER) != null) 
			maxVersion = input.getString(Constants.JSON_SEASON_FIELD_MAX_VER);

		if (input.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) && input.get(Constants.JSON_FIELD_LAST_MODIFIED)!=null) { 
			long timeInMS = (Long)input.get(Constants.JSON_FIELD_LAST_MODIFIED);
			lastModified = new Date(timeInMS);
		}  else {
			lastModified = new Date();
		}
		
		if (input.containsKey(Constants.JSON_FIELD_RUNTIME_ENCRYPTION) && input.get(Constants.JSON_FIELD_RUNTIME_ENCRYPTION) != null) 
			runtimeEncryption = input.getBoolean(Constants.JSON_FIELD_RUNTIME_ENCRYPTION);
	}

	public void duplicateFeatures (Season src, Map<String, BaseAirlockItem> featursDB, HashMap<String, String> oldToDuplicatedFeaturesId, ServletContext context) {
		LinkedList<BaseAirlockItem> srcFeaturesList = src.getRoot().getFeaturesItems();
		ValidationCache tester = new ValidationCache();
		for (int i=0; i<srcFeaturesList.size(); i++) {
			BaseAirlockItem f = srcFeaturesList.get(i).duplicate(minVersion, uniqueId, root.getUniqueId(), featursDB, oldToDuplicatedFeaturesId, context, true, true, tester);
			root.getFeaturesItems().add(f);
		}

	}
	public void duplicatePurchases (Season src, Map<String, BaseAirlockItem> purchasesDB, HashMap<String, String> oldToDuplicatedFeaturesId, ServletContext context) {
		LinkedList<BaseAirlockItem> srcPurchasesList = src.getEntitlementsRoot().getEntitlementItems();
		ValidationCache tester = new ValidationCache();
		for (int i=0; i<srcPurchasesList.size(); i++) {
			BaseAirlockItem iap = srcPurchasesList.get(i).duplicate(minVersion, uniqueId, entitlementsRoot.getUniqueId(), purchasesDB, oldToDuplicatedFeaturesId, context, true, true, tester);
			entitlementsRoot.getEntitlementItems().add(iap);
		}
	}
	
	public boolean containSubItemInProductionStage() {
		for (int i=0; i<root.getFeaturesItems().size(); i++) {
			if (root.getFeaturesItems().get(i).containSubItemInProductionStage()) 
				return true;
		}
		
		for (int i=0; i<entitlementsRoot.getEntitlementItems().size(); i++) {
			if (entitlementsRoot.getEntitlementItems().get(i).containSubItemInProductionStage()) 
				return true;
		}
		
		return false;		
	}
	public boolean containStreamsInProductionStage() {
		for (int i=0; i<streams.getStreamsList().size(); i++) {
			if (streams.getStreamsList().get(i).getStage().equals(Stage.PRODUCTION)) 
				return true;
		}
		return false;		
	}
	public boolean containNotificationsInProductionStage() {
		for (int i=0; i<notifications.getNotificationsList().size(); i++) {
			if (notifications.getNotificationsList().get(i).getStage().equals(Stage.PRODUCTION)) 
				return true;
		}
		return false;		
	}

	//remove the season's features, branches, utilities and strings from DBs. Than remove the season itself from DB
	public void removeSeasonAssetsFromDBs(ServletContext context, UserInfo userInfo) {
		@SuppressWarnings("unchecked")
		Map<String, BaseAirlockItem> airlockItemsDB = (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);
		@SuppressWarnings("unchecked")
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);		
		@SuppressWarnings("unchecked")
		Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);
		@SuppressWarnings("unchecked")
		Map<String, AirlockUtility> utilitiesDB = (Map<String, AirlockUtility>)context.getAttribute(Constants.UTILITIES_DB_PARAM_NAME);
		@SuppressWarnings("unchecked")
		Map<String, OriginalString> stringsDB = (Map<String, OriginalString>)context.getAttribute(Constants.ORIG_STRINGS_DB_PARAM_NAME);
		@SuppressWarnings("unchecked")
		Map<String, AirlockStream> streamsDB = (Map<String, AirlockStream>)context.getAttribute(Constants.STREAMS_DB_PARAM_NAME);
		@SuppressWarnings("unchecked")
		Map<String, AirlockNotification> notificationsDB = (Map<String, AirlockNotification>)context.getAttribute(Constants.NOTIFICATIONS_DB_PARAM_NAME);
		
		for (int i=0; i<root.getFeaturesItems().size(); i++) {
			root.getFeaturesItems().get(i).removeFromAirlockItemsDB(airlockItemsDB,context,userInfo);
		}
		
		LinkedList<Branch> branches = getBranches().getBranchesList();
		for (Branch b:branches) {
			branchesDB.remove(b.getUniqueId().toString());
		}
		
		LinkedList<AirlockUtility> utils = getUtilities().getUtilitiesList();
		for (AirlockUtility util: utils) {
			utilitiesDB.remove(util.getUniqueId().toString());
		}
		
		for (AirlockStream stream:streams.getStreamsList()) {
			streamsDB.remove(stream.getUniqueId().toString());
		}
		
		for (AirlockNotification notification:notifications.getNotificationsList()) {
			notificationsDB.remove(notification.getUniqueId().toString());
		}
		
		LinkedList<OriginalString> origStrings = getOriginalStrings().getOrigStringsList();
		for (OriginalString origStr:origStrings) {
			stringsDB.remove(origStr.getUniqueId().toString());
		}
		
		seasonsDB.remove(uniqueId.toString());
		purge(); // mark the season as deleted for the Smartling background thread
	}

	public ValidationResults validateSeasonJSON(JSONObject seasonObj, ServletContext context) {
		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);

		Action action = Action.ADD;
		try {
			if (seasonObj.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && seasonObj.get(Constants.JSON_FIELD_UNIQUE_ID)!=null) {
				//if JSON contains uniqueId - update an existing product otherwise create a new product
				action = Action.UPDATE;
			}

			//product id
			if (!seasonObj.containsKey(Constants.JSON_FIELD_PRODUCT_ID) || seasonObj.getString(Constants.JSON_FIELD_PRODUCT_ID) == null || seasonObj.getString(Constants.JSON_FIELD_PRODUCT_ID).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_PRODUCT_ID), Status.BAD_REQUEST);
			}
			Product prod = productsDB.get(seasonObj.getString(Constants.JSON_FIELD_PRODUCT_ID));
			if (prod == null) { //product not found
				return new ValidationResults("The product of the specified season does not exist.", Status.BAD_REQUEST);							
			}	

			//min version
			if (!seasonObj.containsKey(Constants.JSON_SEASON_FIELD_MIN_VER) || seasonObj.getString(Constants.JSON_SEASON_FIELD_MIN_VER) == null || seasonObj.getString(Constants.JSON_SEASON_FIELD_MIN_VER).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_SEASON_FIELD_MIN_VER), Status.BAD_REQUEST);
			}

			//features should not appear in season add/update
			if (seasonObj.containsKey(Constants.JSON_FEATURE_FIELD_FEATURES) && seasonObj.get(Constants.JSON_FEATURE_FIELD_FEATURES)!=null) {
				return new ValidationResults("Season features should not be specified during season creation and update. They must be added one by one.", Status.BAD_REQUEST);
			}

			//runtimeEncryption
			if (!seasonObj.containsKey(Constants.JSON_FIELD_RUNTIME_ENCRYPTION) || seasonObj.get(Constants.JSON_FIELD_RUNTIME_ENCRYPTION) == null || seasonObj.getString(Constants.JSON_SEASON_FIELD_MIN_VER).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_RUNTIME_ENCRYPTION), Status.BAD_REQUEST);
			}
			
			boolean tmpRuntimeEncryption = seasonObj.getBoolean(Constants.JSON_FIELD_RUNTIME_ENCRYPTION); //validate that is boolean value
			
			//validate that server supports encryption
			if (tmpRuntimeEncryption && !prod.getCapabilities().contains(AirlockCapability.RUNTIME_ENCRYPTION)) {
				return new ValidationResults(Strings.illegalRuntimeEncryption, Status.BAD_REQUEST);
			}
			
			//validate that server version support encryption 
			if (tmpRuntimeEncryption && serverVersion != null) { //not new season and runtime encryption is true
				Environment env = new Environment();
				env.setServerVersion(serverVersion);
 				if (!ProductServices.isEncryptionSupported(env)) {
 					return new ValidationResults(Strings.encryptionNotSupported, Status.BAD_REQUEST);
 				}
			}
			
			if (action == Action.ADD) {
				//max version should not be included in add
				if (seasonObj.containsKey(Constants.JSON_SEASON_FIELD_MAX_VER) && seasonObj.get(Constants.JSON_SEASON_FIELD_MAX_VER)!=null)
					return new ValidationResults("The max version field cannot be specified during season creation.", Status.BAD_REQUEST);

				//modification date => should not appear in feature creation
				if (seasonObj.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) && seasonObj.get(Constants.JSON_FIELD_LAST_MODIFIED)!=null) {
					return new ValidationResults("The last modified field cannot be specified during season creation.", Status.BAD_REQUEST);
				}

				//in season creation, if there is a previous season, validate that minVersion is higher than previous season's minVersion
				if (prod.getSeasons().size()>0) {
					Season lastSeason = prod.getSeasons().getLast();								
					if (compare(lastSeason.getMinVersion(), seasonObj.getString(Constants.JSON_SEASON_FIELD_MIN_VER)) >= 0) {
						return new ValidationResults("The minimum version must be higher than a previous version range's minimum version.", Status.BAD_REQUEST);
					}
				}
			}
			else { //update
				//modification date must appear
				if (!seasonObj.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) || seasonObj.get(Constants.JSON_FIELD_LAST_MODIFIED)==null) {
					return new ValidationResults(Strings.lastModifiedIsMissing, Status.BAD_REQUEST);
				}				

				//verify that given modification date is not older that current modification date
				long givenModoficationDate = seasonObj.getLong(Constants.JSON_FIELD_LAST_MODIFIED);  //verify that legal long
				Date givenDate = new Date(givenModoficationDate);
				if (givenDate.before(lastModified)) {
					return new ValidationResults(String.format(Strings.itemChangedByAnotherUser, "version range"), Status.CONFLICT);			
				}

				//product id cannot be changed
				String updatedProductId = seasonObj.getString(Constants.JSON_FIELD_PRODUCT_ID);
				if (!updatedProductId.equals(productId.toString())) {
					return new ValidationResults(String.format(Strings.fieldCannotBeChangedDuringUpdate, Constants.JSON_FIELD_PRODUCT_ID), Status.BAD_REQUEST);
				}

				int seasonPos = getSeasonPosition (prod, this.uniqueId.toString());

				if (seasonPos == -1) {
					return new ValidationResults(Strings.seasonNotInProduct, Status.BAD_REQUEST);
				}

				String minVer = seasonObj.getString(Constants.JSON_SEASON_FIELD_MIN_VER);

				//if maxVersion is given - validate				
				if (seasonObj.containsKey(Constants.JSON_SEASON_FIELD_MAX_VER) && seasonObj.get(Constants.JSON_SEASON_FIELD_MAX_VER)!=null && !seasonObj.getString(Constants.JSON_SEASON_FIELD_MAX_VER).isEmpty()) {
					String maxVer = seasonObj.getString(Constants.JSON_SEASON_FIELD_MAX_VER);					

					if (seasonPos == prod.getSeasons().size()-1) {
						//in last season maxVersion should be null or empty
						return new ValidationResults("The maximum version must not be specified for the latest version range.", Status.BAD_REQUEST);
					}
					else {
						//not last season 
						Season nextSeason = prod.getSeasons().get(seasonPos+1);
						if (nextSeason.getMaxVersion()!=null) {
							//verify that max version is not higher than next's season max version
							if (compare(nextSeason.getMaxVersion(), maxVer) < 0) {
								return new ValidationResults("The maximum version must be less than next version range's maximum version.", Status.BAD_REQUEST);
							}				
						}
					}

					if (compare(minVer, maxVer) >= 0) {
						return new ValidationResults("maxVersion must not be less than minVersion.", Status.BAD_REQUEST);
					}
				}

				//if there is a previous season, validate that minVersion is higher than previous season's minVersion
				if (seasonPos>0) {
					Season prevSeason = prod.getSeasons().get(seasonPos-1);								
					if (compare(prevSeason.getMinVersion(), minVer) >= 0) {
						return new ValidationResults("minVersion must be higher than a previous version range's (season's) minVersion.", Status.BAD_REQUEST);
					}
				}
			}

		} catch (JSONException jsne) {
			return new ValidationResults(jsne.getMessage(), Status.BAD_REQUEST);
		}
		catch (ClassCastException cce) {
			return new ValidationResults("Illegal version range JSON: " + cce.getMessage(), Status.BAD_REQUEST);
		}

		return null;

	}

	//returns the season position in the product seasons array
	public int getSeasonPosition(Product prod, String seasonId) {
		for (int i=0; i<prod.getSeasons().size(); i++) {
			if (seasonId.equals(prod.getSeasons().get(i).getUniqueId().toString())) {
				return i;
			}
		}
		return -1; //should never get here! (season not in product's seasons list)
	}

	//Return a string with update details.
	//If nothing was changed - return empty string 
	public String updateSeason(JSONObject updatedSeasonData, Map<String, Product> productsDB) throws JSONException {

		StringBuilder updateDetails = new StringBuilder();
		boolean wasChanged = false;		

		Product prod = productsDB.get(productId.toString());
		if (prod == null) { //product not found
			//cannot be here since we are after validation
			return updateDetails.toString();							
		}	

		int seasonPos = getSeasonPosition (prod, this.uniqueId.toString());

		if (seasonPos == -1) {
			//cannot be here since we are after validation
			return updateDetails.toString(); 
		}

		String updatedMinVer = updatedSeasonData.getString(Constants.JSON_SEASON_FIELD_MIN_VER);
		if (!updatedMinVer.equals(minVersion)) {
			updateDetails.append("'minVersion' changed from " + minVersion + " to " + updatedMinVer + ";	");
			minVersion = updatedMinVer;			
			if (seasonPos>0) {
				//updating minVersion of a non first season changes the maxVersion of its previous season
				Season prevSeason = prod.getSeasons().get(seasonPos-1);
				String prevMaxVesrion = prevSeason.getMaxVersion();
				prevSeason.setMaxVersion(updatedMinVer);
				updateDetails.append("previous season " + prevSeason.getUniqueId().toString() + " 'maxVersion' changed from " + prevMaxVesrion + " to " + updatedMinVer + ";	");				
			}
			wasChanged = true;
		}
		
		boolean updatedRuntimeEncryption = updatedSeasonData.getBoolean(Constants.JSON_FIELD_RUNTIME_ENCRYPTION);
		if (updatedRuntimeEncryption!=runtimeEncryption) {
			updateDetails.append("'runtimeEncryption' changed from " + runtimeEncryption + " to " + updatedRuntimeEncryption + ";	");
			runtimeEncryption = updatedRuntimeEncryption;			
			wasChanged = true;
		}

		//optional field
		if (updatedSeasonData.containsKey(Constants.JSON_SEASON_FIELD_MAX_VER) && updatedSeasonData.get(Constants.JSON_SEASON_FIELD_MAX_VER)!=null && !updatedSeasonData.getString(Constants.JSON_SEASON_FIELD_MAX_VER).isEmpty()) {
			String updatedMaxVer = updatedSeasonData.getString(Constants.JSON_SEASON_FIELD_MAX_VER);
			if (!updatedMaxVer.equals(maxVersion)) {
				updateDetails.append("'maxVersion' changed from " + maxVersion + " to " + updatedMaxVer + ";	");
				maxVersion = updatedMaxVer;
				if (seasonPos<prod.getSeasons().size()-1) {
					//updating maxVersion of a non last season changes the minVersion of its next season
					Season nextSeason = prod.getSeasons().get(seasonPos+1);
					String nextMinVesrion = nextSeason.getMinVersion();
					nextSeason.setMinVersion(updatedMaxVer);
					updateDetails.append("next season " + nextSeason.getUniqueId().toString() + " 'minVersion' changed from " + nextMinVesrion + " to " + updatedMaxVer + ";	");				
				}
				wasChanged = true;
			}
		}

		if (wasChanged) {
			lastModified = new Date();
		}

		if (updateDetails.length()!=0) {
			updateDetails.insert(0,"Season changes: ");
		}
		return updateDetails.toString();		
	}

	//return res < 0 if s1<s2
	//return 0 if s1===s2
	//return res > 0 if s1>s2 
	public static int compare(String s1, String s2) {
		if ((s1 == null || s1.equals(""))&& (s2 == null || s2.equals("")))
			return 0;
		if (s1 == null || s1.equals(""))
			return 1;
		if (s2 == null || s2.equals(""))
			return -1;
		String[] s1Array = s1.split("\\.");
		String[] s2Array = s2.split("\\.");

		int numPartsToCompare = Math.min(s1Array.length, s2Array.length);
		for (int i=0; i<numPartsToCompare; i++) {
			// try to compare numeric
			try {
				int s1Val = Integer.parseInt(s1Array[i]);
				int s2Val = Integer.parseInt(s2Array[i]);
				if (s1Val != s2Val) {
					return s1Val - s2Val;
				}
			}
			// compare Strings
			catch (NumberFormatException e1) {
				String s1Val = s1Array[i];
				String s2Val = s2Array[i];
				if (s1Val.compareTo(s2Val) != 0) {
					return s1Val.compareTo(s2Val);
				}
			}
		}
		if (s1Array.length > s2Array.length) {
			// check for the longer array, that all the nodes are negligible (0 or empty string)
			for (int j = numPartsToCompare; j < s1Array.length; j++) {
				if (!s1Array[j].equals("") && !s1Array[j].equals("0")) {
					return 1;
				}
			}
		}
		else if (s2Array.length > s1Array.length) {
			// check for the longer array, that all the nodes are negligible (0 or empty string)
			for (int k = numPartsToCompare; k < s2Array.length; k++) {
				if (!s2Array[k].equals("") && !s2Array[k].equals("0")) {
					return -1;
				}
			}
		}
		return 0;
	}

	public void duplicateInputSchema(Season lastSeason) {
		inputSchema = lastSeason.getInputSchema().duplicateForNewSeason(uniqueId);		
	}
	
	public void duplicateAnalytics(Season lastSeason, HashMap<String, String> oldToDuplicatedFeaturesId, ServletContext context) throws JSONException {
		analytics = lastSeason.getAnalytics().duplicateForNewSeason(uniqueId, oldToDuplicatedFeaturesId, context, lastSeason.getUniqueId());		
	}

	public void duplicateBranchesCollection(Season lastSeason, HashMap<String, String> oldToDuplicatedFeaturesId, ServletContext context) throws JSONException {
		branches = lastSeason.getBranches().duplicateForNewSeason(this, oldToDuplicatedFeaturesId, context);
	}
	
	public void duplicateUtilities(Season lastSeason, Map<String, AirlockUtility> utilitiesDB, ServletContext context) {
		utilities.utilitiesList = lastSeason.getUtilities().duplicateForNewSeason(minVersion, uniqueId, utilitiesDB);
		if (utilities.utilitiesList.size() == 0) {
			//the previous season does not contain any utility - probably from previous version.
			//in this case add the initial utilities to the new season.
			utilities.addInitialUtility(context);
		}						
	}
	
	public void duplicateStreams(Season lastSeason, Map<String, AirlockStream> streamsDB, ServletContext context) {
		generateEmptyStreams();
		streams = lastSeason.getStreams().duplicateForNewSeason(minVersion, uniqueId, streamsDB);
		streamsEvents = lastSeason.getStreamsEvents().duplicateForNewSeason(uniqueId);
	}
	
	public void duplicateNotifications(Season lastSeason, Map<String, AirlockNotification> notificationsDB, ServletContext context) {
		notifications = lastSeason.getNotifications().duplicateForNewSeason(minVersion, uniqueId, notificationsDB, context);		
	}

	public void duplicateOriginalStrings(Season lastSeason, Map<String, OriginalString> originalStringsDB) {
		originalStrings = lastSeason.getOriginalStrings().duplicateForNewSeason(minVersion, originalStringsDB, this, lastSeason.getServerVersion());		
	}

	public void generateInitialUtilities(ServletContext context) {
		utilities.addInitialUtility(context);
	}

	public boolean isOldRuntimeFileExists() {
		return isOldRuntimeFileExists;
	}	

	public void setOldRuntimeFileExists(boolean isOldRuntimeFileExists) {
		this.isOldRuntimeFileExists = isOldRuntimeFileExists;
	}

	public String getServerVersion() {
		return serverVersion;
	}

	public void setServerVersion(String serverVersion) {
		this.serverVersion = serverVersion;
	}
	
	public LinkedList<AirlockChangeContent> upgrade(String fromVersion, ServletContext context, UserInfo userInfo) throws JSONException, IOException {
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		if (fromVersion.equals("V2")) {
			//upgrade the Translations: OriginalStrings structure changed 
			changesArr.addAll(originalStrings.upgrade("V2", context, true)); //update original string file but leave the localTranslation files as is (without dev+prod separation) for prev sdk
		}
		//removed since not needed any more
		/*
		if (fromVersion.equals("V2.5") || fromVersion.equals("V2")) {
			changesArr.addAll(originalStrings.upgrade("V2.5", context, true)); //update original string file but leave the localTranslation files as is (without dev+prod separation) for prev sdk
			serverVersion = Constants.CURRENT_SERVER_VERSION;
			
			changesArr.addAll(AirlockFilesWriter.doWriteFeatures(this, OutputJSONMode.RUNTIME_DEVELOPMENT, context, Stage.DEVELOPMENT, env));
			changesArr.addAll(AirlockFilesWriter.doWriteFeatures(this, OutputJSONMode.RUNTIME_PRODUCTION, context, Stage.PRODUCTION, env));
			changesArr.addAll(AirlockFilesWriter.writeServerInfo(this, context));
		}
		
		if (fromVersion.equals("V3.0")) { //pre 3.0 seasons did not have experiemnts 
			
			//update experiments - add range
			boolean writeExperimentNeeded = false;
			List<Experiment> experiments = getExperimentsForSeason(context, false);
			
			for (Experiment exp:experiments) {
				if (exp.getRangesList().size() == 0 && exp.getEnabled() == true) {
					ExperimentRange newExperimentRange = new ExperimentRange(exp.getCreationDate(), null);
					ArrayList<ExperimentRange> expRanges = new ArrayList<ExperimentRange>();
					expRanges.add(newExperimentRange);
					exp.setRangesList(expRanges);
					writeExperimentNeeded = true;
				}				
			}			
			
			if (writeExperimentNeeded) {
				@SuppressWarnings("unchecked")
				Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);
				
				Product prod = productsDB.get(productId.toString());
				changesArr.addAll(AirlockFilesWriter.writeExperiments(prod, context, Stage.PRODUCTION));
			}
			
			//write streams files - empty for now
			changesArr.addAll(AirlockFilesWriter.writeSeasonStreams(this, true, context));
			changesArr.addAll(AirlockFilesWriter.writeSeasonStreamsEvents(this, context));
			changesArr.addAll(AirlockFilesWriter.writeSeasonUtilities(this, true, context, UtilityType.STREAMS_UTILITY));
			
			serverVersion = Constants.CURRENT_SERVER_VERSION;
			changesArr.addAll(AirlockFilesWriter.writeServerInfo(this, context));	
			changesArr.addAll(AirlockFilesWriter.doWriteFeatures(this, OutputJSONMode.RUNTIME_DEVELOPMENT, context, Stage.DEVELOPMENT, env));
			changesArr.addAll(AirlockFilesWriter.doWriteFeatures(this, OutputJSONMode.RUNTIME_PRODUCTION, context, Stage.PRODUCTION, env));
		}
		
		if (fromVersion.equals("V4.0")) {
			
			serverVersion = Constants.CURRENT_SERVER_VERSION;
			changesArr.addAll(AirlockFilesWriter.doWriteFeatures(this, OutputJSONMode.RUNTIME_DEVELOPMENT, context, Stage.DEVELOPMENT, env));
			changesArr.addAll(AirlockFilesWriter.doWriteFeatures(this, OutputJSONMode.RUNTIME_PRODUCTION, context, Stage.PRODUCTION, env));
			changesArr.addAll(AirlockFilesWriter.writeServerInfo(this, context));
			
		}
		if (fromVersion.equals("V4.1")) {
			
			serverVersion = Constants.CURRENT_SERVER_VERSION;
			changesArr.addAll(AirlockFilesWriter.doWriteFeatures(this, OutputJSONMode.RUNTIME_DEVELOPMENT, context, Stage.DEVELOPMENT, env));
			changesArr.addAll(AirlockFilesWriter.doWriteFeatures(this, OutputJSONMode.RUNTIME_PRODUCTION, context, Stage.PRODUCTION, env));
			changesArr.addAll(AirlockFilesWriter.writeServerInfo(this, context));
			
			//write notifications files - empty for now
			changesArr.addAll(AirlockFilesWriter.writeSeasonNotifications(this, true, context, false));
			
			//add name to utilities
			Pair<String,LinkedList<AirlockChangeContent>> writeRes = ProductServices.doUpgradeSeasonUtilities(this, context); 
			String upgradeDetails = writeRes.getKey();
			if (upgradeDetails.length()>0) {
				AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
				auditLogWriter.log("Upgrade season utilities for season: " + uniqueId.toString() + " :\n" + upgradeDetails, userInfo); 
			}
			changesArr.addAll(writeRes.getValue());
		}*/
		return changesArr;
				
	}

	public boolean containBranchInUseByExperiment(ServletContext context) {
		for (Branch b:branches.getBranchesList()) {
			if (b.isPartOfExperiment(context)!=null) {
				return true;
			}
		}
		return false;
	}

	public List<Experiment> getExperimentsForSeason(ServletContext context, boolean onlyProduction) {
		
		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);

		Product prod = productsDB.get(productId.toString());
		List<Experiment> allExperiments = prod.getExperimentsMutualExclusionGroup().getExperiments();
		List<Experiment> seasonExperiments = new LinkedList<Experiment>();
		for(Experiment exp:allExperiments) {
			if (onlyProduction && exp.getStage().equals(Stage.DEVELOPMENT))
				continue;
			
			if (Product.isSeasonInRange(this, exp.getMinVersion(), exp.getMaxVersion())) {
				seasonExperiments.add(exp);
			}
		}
		
		return seasonExperiments;
	}

	//if the quota is decreased, validate that no experiment in prod is oven quota.
	public ValidationResults validateNewQuota(Integer newQuota, ServletContext context) throws MergeException {
		if (newQuota >= analytics.getAnalyticsQuota()) {
			//increasing the quota wont cause any problems 
			return null;
		}
		List<Experiment> productionExperiments = getExperimentsForSeason(context, true);
		for (Experiment exp: productionExperiments) {
			int origExpQuota = exp.getQuota(context);
			int newExpQuota = exp.getQuotaReplaceSeasonQuota(context, uniqueId, newQuota);
			if (origExpQuota>newExpQuota) {
				int expProdCounter = exp.getAnalyticsProductionCounter(context, null, null, null);
				if (expProdCounter>newExpQuota) {
					String err = "Failed to update season quota. The seaosn is included in an experiment in production and reducing the quota caused experiment " + exp.getName() + " to exceed quota.";
					logger.severe(err);
					return new ValidationResults(err, Status.BAD_REQUEST);
				}
			}
		}
				
		return null;
	}

	//return null if nothing is missing
	public String isMissingBranchUsedByExperiment(ServletContext context) {
		List<Experiment> experiments = getExperimentsForSeason(context, false);
		for (Experiment exp:experiments) {
			for (Variant var:exp.getVariants()) {
				if (!var.getBranchName().equals(Constants.MASTER_BRANCH_NAME) && branches.getBranchByName(var.getBranchName()) == null)
					return "The version range is in the range of the experiment " +  exp.getName() +", and the version range is missing the branch " + var.getBranchName() +" that is used by the experiment.";
			}
		}
		return null;
	}
	
	public Set<AirlockCapability> getSeasonCapabilities(ServletContext context) {
		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);
		
		Product prod = productsDB.get(productId.toString());
		Set<AirlockCapability> productCapabilities = com.ibm.airlock.admin.Utilities.cloneCapabilitiesSet(prod.getCapabilities());
		Version seasonVersion = Version.find(serverVersion);
		if (seasonVersion.i < Version.v2_5.i) {
			productCapabilities.remove(AirlockCapability.ANALYTICS); //only post 2.1 seasons support analytics	 (2.5 and up)
		}
		
		if(seasonVersion.i < Version.v3_0.i) {
			productCapabilities.remove(AirlockCapability.BRANCHES); //only post 2.5 seasons support branches (3.0 and up)	
		}
	
	    if (seasonVersion.i < Version.v3_0.i)  {
	    		productCapabilities.remove(AirlockCapability.EXPERIMENTS); //only post 2.5 seasons support experiments (3.0 and up)
	    }
	
	    if (seasonVersion.i < Version.v4_0.i)  {
	    	productCapabilities.remove(AirlockCapability.STREAMS); //only post 3_5 seasons support streams (4.0 and up)	
	    }
	    
	    if (seasonVersion.i < Version.v4_5.i)  {
	    	productCapabilities.remove(AirlockCapability.NOTIFICATIONS); //only post 4_0 seasons support notifications (4.5 and up)	
	    }
	    
	    if (seasonVersion.i < Version.v2_5.i) {
	    		productCapabilities.remove(AirlockCapability.TRANSLATIONS); //only post 2_1 seasons support notifications (2.5 and up)	
	    }
	    
	    if (seasonVersion.i < Version.v5_0.i)  {
	    		productCapabilities.remove(AirlockCapability.RUNTIME_ENCRYPTION); //only post 5_0 seasons support runtime encryption (4.5 and up)	
	    }
	    
	    if (seasonVersion.i < Version.v5_5.i)  {
    			productCapabilities.remove(AirlockCapability.ENTITLEMENTS); //only post 5_5 seasons support entitlements
	    }
	    
		return productCapabilities;
	}
	
	
	public boolean isRuntimeInternalSeparationSupported () {
		Version version = Version.find(serverVersion);
		return version.i >= Version.v5_0.i;  //only seasons from version 5.0 and up supports runtime/internal folder seperation
	}

	public void rewriteRuntimeFileWithUpdatedEncryptionMode(boolean orgRuntimeEncryption, boolean newRuntimeEncryption, ServletContext context, Product prod, Environment env) throws IOException, JSONException {
		if (newRuntimeEncryption == orgRuntimeEncryption)
			return;
		
		AirlockFilesWriter.doWriteFeatures (this, OutputJSONMode.RUNTIME_DEVELOPMENT, context, Stage.DEVELOPMENT, env);
		AirlockFilesWriter.doWriteFeatures (this, OutputJSONMode.RUNTIME_PRODUCTION, context, Stage.PRODUCTION, env);
		
		AirlockFilesWriter.writeSeasonUtilities(this, true, context, UtilityType.MAIN_UTILITY);
		AirlockFilesWriter.writeSeasonUtilities(this, true, context, UtilityType.STREAMS_UTILITY);
		
		AirlockFilesWriter.writeSeasonStreams(this, true, context);
		AirlockFilesWriter.writeSeasonNotifications(this, true, context, false);
		
		AirlockFilesWriter.writeSeasonBranches(this, context, env, Stage.PRODUCTION);
		AirlockFilesWriter.writeUserGroupsRuntimeForSeason(context, prod, this);
		
		AirlockFilesWriter.writeProductRuntimeForSeason(context, prod, this, Stage.PRODUCTION);
		
		AirlockFilesWriter.writeAllLocalesStringsFiles(this, context, true, true);
		
		//branches
	    LinkedList<Branch> branches = getBranches().getBranchesList();
	    for (Branch branch:branches) {
	    		AirlockFilesWriter.writeBranchRuntime(branch, this, context, env, true);
	    }
	}

	public void replaceNewPurchaseIdsInPremiumFeatures(HashMap<String, String> oldToDuplicatedFeaturesId) {
		doReplacePrucahseIdInFeature(root, oldToDuplicatedFeaturesId, null);	
	}

	public static void doReplacePrucahseIdInFeature(BaseAirlockItem airlockItem, HashMap<String, String> oldToDuplicatedFeaturesIdMaster, HashMap<String, String> oldToDuplicatedFeaturesIdBranch) {
		if (airlockItem.type.equals(Type.FEATURE)) {
			FeatureItem fi = (FeatureItem)airlockItem;
			if (fi.getEntitlement()!=null && !fi.getEntitlement().isEmpty()) {
				String newPurchaseId = null;
				if (oldToDuplicatedFeaturesIdBranch!=null) { //first look for the old id in the branch map
					newPurchaseId = oldToDuplicatedFeaturesIdBranch.get(fi.getEntitlement());
				}
				if (newPurchaseId == null) {
					newPurchaseId = oldToDuplicatedFeaturesIdMaster.get(fi.getEntitlement());
				}
				fi.setEntitlement(newPurchaseId);
			}
		}
		
		if (airlockItem.getFeaturesItems()!=null) {
			for (BaseAirlockItem ai:airlockItem.getFeaturesItems()) {
				doReplacePrucahseIdInFeature(ai, oldToDuplicatedFeaturesIdMaster, oldToDuplicatedFeaturesIdBranch);
			}
		}	
	}

	public void replaceNewPurchaseIdsInBundles(HashMap<String, String> oldToDuplicatedFeaturesId) {
		doReplacePrucahseIdInBundle(entitlementsRoot, oldToDuplicatedFeaturesId, null);
	}
	
	public static void doReplacePrucahseIdInBundle(BaseAirlockItem airlockItem, HashMap<String, String> oldToDuplicatedFeaturesIdMaster, HashMap<String, String> oldToDuplicatedFeaturesIdBranch) {
		if (airlockItem.type.equals(Type.ENTITLEMENT)) {
			EntitlementItem iap = (EntitlementItem)airlockItem;
			if (iap.getIncludedPurchases()!=null && !iap.getIncludedPurchases().isEmpty()) {
				for (int i=0; i<iap.getIncludedPurchases().size(); i++) {
					String newPurchaseId = null;
					if (oldToDuplicatedFeaturesIdBranch!=null) { //first look for the old id in the branch map
						newPurchaseId = oldToDuplicatedFeaturesIdBranch.get(iap.getIncludedPurchases().get(i));
					}
					if (newPurchaseId == null) {
						newPurchaseId = oldToDuplicatedFeaturesIdMaster.get(iap.getIncludedPurchases().get(i));
					}
					iap.getIncludedPurchases().set(i, newPurchaseId);
				}
			}
		}
		
		if (airlockItem.getEntitlementItems()!=null) {
			for (BaseAirlockItem ai:airlockItem.getEntitlementItems()) {
				doReplacePrucahseIdInBundle(ai, oldToDuplicatedFeaturesIdMaster, oldToDuplicatedFeaturesIdBranch);
			}
		}	
	}

}
