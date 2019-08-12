package com.ibm.airlock.admin.translations;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import com.ibm.airlock.admin.serialize.DataSerializer;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Constants.Stage;
import com.ibm.airlock.Constants.StringStatus;
import com.ibm.airlock.Constants.StringsOutputMode;
import com.ibm.airlock.Constants.TranslationStatus;
import com.ibm.airlock.admin.BaseAirlockItem;
import com.ibm.airlock.admin.ConfigurationRuleItem;
import com.ibm.airlock.admin.GenerationException;
import com.ibm.airlock.admin.Season;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.ValidationCache;
import com.ibm.airlock.admin.ValidationException;
import com.ibm.airlock.admin.notifications.AirlockNotification;
import com.ibm.airlock.admin.operations.AirlockChangeContent;
import com.ibm.airlock.admin.serialize.AirlockFilesWriter;
import com.ibm.airlock.admin.translations.OriginalString.StringTranslations.LocaleTranslationData;
import com.ibm.airlock.admin.translations.TranslatedStrings.TranslatedString;
import com.ibm.airlock.admin.BaseAirlockItem.Type;
import com.ibm.airlock.admin.Branch;
import com.ibm.airlock.engine.VerifyRule;
import com.ibm.airlock.utilities.ConvertTranslations;
import com.ibm.airlock.utilities.ConvertTranslations.SmartlingData;

public class OriginalStrings {

	public static final Logger logger = Logger.getLogger(OriginalStrings.class.getName());

	//--------------------------------------------------------------------------
	// quick access to all strings that have the same value
	public class StringXref
	{
		HashMap<String,String> byId = new HashMap<String,String>(); // id to original value
		HashMap<String,HashSet<OriginalString>> byValue = new HashMap<String,HashSet<OriginalString>>(); // value to original strings

		public StringXref()
		{
			if (origStringsList != null)
				for (OriginalString orig : origStringsList)
					addItem(orig);
		}
		public void clear()
		{
			byId.clear();
			byValue.clear();
		}
		public void addOrUpdateItem(OriginalString orig)
		{
			deleteItem(orig); // OK if missing
			addItem(orig);
		}
		public void addItem(OriginalString orig)
		{
			String id = orig.getUniqueId().toString();
			String value = noNull(orig.getValue());
			byId.put(id, value);

			HashSet<OriginalString> list = byValue.get(value);
			if (list == null)
			{
				list = new HashSet<OriginalString>();
				byValue.put(value, list);
			}
			list.add(orig);
		}
		public void deleteItem(OriginalString orig)
		{
			String id = orig.getUniqueId().toString();
			String value = byId.get(id);
			if (value == null)
				return;

			byId.remove(id);
			HashSet<OriginalString> list = byValue.get(value);
			removeIdFromSet(list, id);
		}
		void removeIdFromSet(Set<OriginalString> list, String id)
		{
			if (list != null)
			{
				OriginalString found = null;
				for (OriginalString item : list)
				{
					if (id.equals(item.getUniqueId().toString()))
					{
						found = item;
						break;
					}
				}
				if (found != null)
					list.remove(found);
			}
		}

		// return all strings that have this value
		public Set<OriginalString> findSimilar(String value)
		{
			return byValue.get(noNull(value));
		}

		// return all strings that have the value except for the original string
		public Set<OriginalString> findSimilar(OriginalString orig)
		{
			Set<OriginalString> out = byValue.get(noNull(orig.getValue()));
			if (out != null)
			{
				out = new HashSet<OriginalString>(out);
				removeIdFromSet(out, orig.getUniqueId().toString());
			}
			return out;
		}
		// new string mode -- just compare values and return one list
		public JSONObject addSimilarToJson(String value) throws JSONException
		{
			JSONObject json = new JSONObject();
			addToJson(json, findSimilar(value));
			return json;
		}
		// update string mode -- show two lists (for identical + different variants)
		public JSONObject addSimilarToJson(OriginalString orig) throws JSONException
		{
			JSONObject json = new JSONObject();
			Set<OriginalString> set = findSimilar(orig);
			String originalVariant = noNull(orig.getVariant());
			addToJson(json, set, originalVariant, true);
			addToJson(json, set, originalVariant, false);
			return json;
		}
		void addToJson(JSONObject json, Set<OriginalString> set) throws JSONException
		{
			if (set == null || set.isEmpty())
				return;

			JSONArray array = new JSONArray();
			for (OriginalString orig : set)
				array.add(toObj(orig));

			json.put(Constants.JSON_FIELD_SAME_STRINGS, array);
		}
		void addToJson(JSONObject json, Set<OriginalString> set, String originalVariant, boolean showSameVariant) throws JSONException
		{
			if (set == null || set.isEmpty())
				return;

			JSONArray array = new JSONArray();
			for (OriginalString orig : set)
			{
				String variant = noNull(orig.getVariant());
				boolean same = originalVariant.equals(variant);
				if (same == showSameVariant)
					array.add(toObj(orig));
			}

			if (!array.isEmpty())
			{
				String field = showSameVariant ? Constants.JSON_FIELD_SAME_STRINGS_AND_VARIANTS : Constants.JSON_FIELD_SAME_STRINGS_OTHER_VARIANTS;
				json.put(field, array);
			}
		}
		JSONObject toObj(OriginalString orig) throws JSONException
		{
			JSONObject obj = new JSONObject();
			obj.put(Constants.JSON_FIELD_KEY, orig.getKey());
			obj.put(Constants.JSON_FIELD_VALUE, orig.getValue());
			obj.put(Constants.JSON_FIELD_STRINGS_VARIANT, orig.getVariant());
			obj.put(Constants.JSON_FIELD_STRING_ID, orig.getUniqueId().toString());
			return obj;
		}
		String noNull(String in) { return in == null ? "" : in; }
	}
	//----------------------------------------------------------------------------------------
	//list of the strings
	private LinkedList<OriginalString> origStringsList = new LinkedList<OriginalString>();
	//map between key and original string
	private HashMap<String, OriginalString> origStringsMap = new HashMap<String, OriginalString>(); //for O{1} access
	private StringXref stringXref = null;
	private Season season = null;
	private boolean isOldEnStringsFileExists = false;
	private boolean isUpgradedPre21Season = false; //relevant only in old seasons (isOldEnStringsFileExists=true) 
 
	private LinkedList<String> supportedLanguages = new LinkedList<String>(); //for keeping order

	public OriginalStrings() {
		supportedLanguages.add(Constants.DEFAULT_LANGUAGE);
		stringXref = new StringXref();
	}

	public OriginalStrings(Season season) {
		this.season = season;			
		supportedLanguages.add(Constants.DEFAULT_LANGUAGE);
		stringXref = new StringXref();
	}		

	public boolean isUpgradedPre21Season() {
		return isUpgradedPre21Season;
	}

	public void setUpgradedPre21Season(boolean isUpgradedPre21Season) {
		this.isUpgradedPre21Season = isUpgradedPre21Season;
	}

	public boolean isOldEnStringsFileExists() {
		return isOldEnStringsFileExists;
	}

	public void setOldEnStringsFileExists(boolean isOldEnStringsFileExists) {
		this.isOldEnStringsFileExists = isOldEnStringsFileExists;
	}

	public LinkedList<String> getSupportedLanguages() {
		return supportedLanguages;
	}

	public void setSupportedLanguages(LinkedList<String> supportedLanguages) {
		this.supportedLanguages = supportedLanguages;
	}

	public boolean isLanguageSupported(String lang) {
		for (int i=0; i<supportedLanguages.size(); i++) {
			if (supportedLanguages.get(i).equals(lang)){
				return true;
			}				
		}
		return false;
	}

	public Set<String> getAllStringKeys() {
		return origStringsMap.keySet();
	}
	
	//return error string if this language already listed else return null
	//if sourceLocale is not null. Copy the translations from the source locale. 
	public void addSupportedLanguage(String newLocale, String sourceLocale) throws IllegalArgumentException {
		if (isLanguageSupported(newLocale)) {
			throw new IllegalArgumentException("The " + newLocale + " language is already supported.");
		}
		supportedLanguages.add(newLocale);	
		if (sourceLocale!=null) {
			for (int i=0; i<origStringsList.size(); i++) {
				LocaleTranslationData srcLocaleTranslationData = origStringsList.get(i).getStringTranslations().getTranslationDataPerLocale(sourceLocale);
				if (srcLocaleTranslationData!=null) {
					LocaleTranslationData localeTranslationData = srcLocaleTranslationData.duplicate();
					origStringsList.get(i).getStringTranslations().addLocaleTranslation(newLocale, localeTranslationData);
				}
			}
		}
	}
	
	//return error string if this language does not listed else return null
	public Stage removeSupportedLanguage(String supportedLanguage) throws IllegalArgumentException {
		Stage changeStage = Stage.DEVELOPMENT;
		if (!isLanguageSupported(supportedLanguage)) {
			throw new IllegalArgumentException("The " + supportedLanguage + " language is does not supported.");
		}

		supportedLanguages.remove(supportedLanguage);
		
		//go over all the strings and remove the locale's translation
		for (OriginalString origStr:origStringsList) {
			HashMap<String, LocaleTranslationData> strTranlationsMap = origStr.getStringTranslations().getStringTranslationsMap();
			strTranlationsMap.remove(supportedLanguage);
			
			if (origStr.getStatus().equals(StringStatus.IN_TRANSLATION)) {
				//move string status to translation_complete if after removing the locale - all locales are translated
				origStr.moveStringStatusToTransCompletedIfNeeded(supportedLanguages);
			}
			if (origStr.getStage() == Stage.PRODUCTION)
				changeStage = Stage.PRODUCTION;
		}
		return changeStage;			
	}

	public StringXref getStringXref() {
		return stringXref;
	}
	public LinkedList<OriginalString> getOrigStringsList() {
		return this.origStringsList;
	}

	public void setOrigStringsList(LinkedList<OriginalString> origStringsList ) {
		this.origStringsList = origStringsList;
		for (OriginalString origstr:this.origStringsList) {
			origStringsMap.put(origstr.getKey(), origstr);
		}
		stringXref = new StringXref();
	}

	public JSONObject toJson(StringsOutputMode outputMode) throws JSONException {
		JSONObject res = new JSONObject();
		JSONArray origStringsArr = new JSONArray();
		for (OriginalString origStr : origStringsList) {
			JSONObject osObj = origStr.toJson(outputMode, season);
			origStringsArr.add(osObj);				
		}
		res.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, season.getUniqueId()==null?null:season.getUniqueId().toString());
		res.put(Constants.JSON_FIELD_STRINGS, origStringsArr);
		res.put(Constants.JSON_FIELD_SUPPORTED_LANGUAGES, supportedLanguages);

		return res;
	}

	public JSONObject getStringsByStatusJSON(StringsOutputMode outputMode, StringStatus status) throws JSONException {
		JSONObject res = new JSONObject();
		JSONArray origStringsArr = new JSONArray();
		for (OriginalString origStr : origStringsList) {
			if (origStr.getStatus().equals(status)) {
				JSONObject osObj = origStr.toJson(outputMode, season);
				origStringsArr.add(osObj);
			}
		}
		res.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, season.getUniqueId()==null?null:season.getUniqueId().toString());
		res.put(Constants.JSON_FIELD_STRINGS, origStringsArr);
		res.put(Constants.JSON_FIELD_STATUS, status.toString());

		return res;
	}

	
	//called in server init stage - reading the schemas from files in s3
	public void fromJSON(JSONObject input, Map<String, OriginalString> originalStringsDB) throws JSONException {
		//for now no validation is done - error will cause JSONError.
		JSONArray origStrsSArr = input.getJSONArray(Constants.JSON_FIELD_STRINGS);

		for (int i=0; i<origStrsSArr.length(); i++ ) {
			JSONObject origStrJSON = origStrsSArr.getJSONObject(i);
			OriginalString origStr = new OriginalString();
			origStr.fromJSON(origStrJSON, season);
			addOriginalString(origStr);
			if (originalStringsDB!=null)
				originalStringsDB.put(origStr.getUniqueId().toString(), origStr);
		}

		if (input.containsKey(Constants.JSON_FIELD_SUPPORTED_LANGUAGES) && input.get(Constants.JSON_FIELD_SUPPORTED_LANGUAGES)!=null) {
			supportedLanguages.clear();

			JSONArray supLangsJSONArr = (input.getJSONArray(Constants.JSON_FIELD_SUPPORTED_LANGUAGES));
			if (supLangsJSONArr != null && supLangsJSONArr.size()>0) {
				for (int i=0; i<supLangsJSONArr.size(); i++) {
					supportedLanguages.add(supLangsJSONArr.getString(i));										
					//groupsMap.put(groupsJSONArr.getString(i), 0);
				}
			}
		}
	}

	public void addOriginalString(OriginalString origStr) {
		origStringsList.add(origStr);
		origStringsMap.put(origStr.getKey(), origStr);
		stringXref.addOrUpdateItem(origStr);
	}

	//return null if does not exits
	public OriginalString getOriginalStringByKey(String key) {
		/*for (OriginalString origStr:origStringsList) {
				if (origStr.getKey().equals(key))
					return origStr;
			}
			return null;*/
		return origStringsMap.get(key);
	}

	//return null if OK, error staring on error
	public String removeOriginalString(OriginalString stringToDel, ServletContext context) {
		if (origStringsList == null || origStringsList.size() == 0) {
			return "Unable to remove original string " + stringToDel.getUniqueId().toString() + " from season " + season.getUniqueId().toString() + ": season has no original strings.";				
		}

		//go over the season's configRules and validate that no rule or configuration uses the missing string
		ValidationCache tester = new ValidationCache(stringToDel.getKey(), null);
		String verFuncRes;
		try {
			verFuncRes = validateDeletedStringIsNotInUse(tester, stringToDel, season.getRoot(), context, Constants.MASTER_BRANCH_NAME);
			if (verFuncRes != null)
				return verFuncRes;
			
			verFuncRes = validateDeletedStringIsNotInUse(tester, stringToDel, season.getEntitlementsRoot(), context, Constants.MASTER_BRANCH_NAME);
			if (verFuncRes != null)
				return verFuncRes;
			
			for (Branch b:season.getBranches().getBranchesList()) {
				for (BaseAirlockItem branchSubTreeRoot:b.getBranchFeatures()) {
					verFuncRes = validateDeletedStringIsNotInUse(tester, stringToDel, branchSubTreeRoot, context, b.getName());
					if (verFuncRes != null)
						return verFuncRes;
				}
			}
			
			verFuncRes = validateDeletedStringIsNotInUseInNotifications(tester, stringToDel, context, Constants.MASTER_BRANCH_NAME);
			if (verFuncRes != null)
				return verFuncRes;
		} catch (JSONException e) {			 
			return "Unable to verify string deletion:" + e.getMessage();
		} catch (GenerationException e) {
			return "Unable to verify string deletion:" + e.getMessage();
		}

		for (int i=0; i< origStringsList.size(); i++) {
			if (origStringsList.get(i).getUniqueId().equals(stringToDel.getUniqueId())) {
				origStringsList.remove(i);
				origStringsMap.remove(stringToDel.getKey());
				stringXref.deleteItem(stringToDel);
				return null;
			}
		}

		return "Unable to remove original string " + stringToDel.getUniqueId().toString() + " from season " + season.getUniqueId().toString() + ": The specified original string does not exist under this season.";
	}

	private String validateDeletedStringIsNotInUse(ValidationCache tester, OriginalString stringToDel, BaseAirlockItem root, ServletContext context, String branchName) throws JSONException, GenerationException
	{
		//String are only used in configuration field in ConfigurationRule items
		//take into account the string stage and minAppVersion
		if (root.getFeaturesItems()!=null) {
			for (int i=0; i<root.getFeaturesItems().size(); i++) {								
				String res = validateDeletedStringIsNotInUse(tester, stringToDel, root.getFeaturesItems().get(i), context, branchName);
				if (res!=null)
					return res;
			}
		}
		if (root.getEntitlementItems()!=null) {
			for (int i=0; i<root.getEntitlementItems().size(); i++) {								
				String res = validateDeletedStringIsNotInUse(tester, stringToDel, root.getEntitlementItems().get(i), context, branchName);
				if (res!=null)
					return res;
			}
		}
		if (root.getPurchaseOptionsItems()!=null) {
			for (int i=0; i<root.getPurchaseOptionsItems().size(); i++) {								
				String res = validateDeletedStringIsNotInUse(tester, stringToDel, root.getPurchaseOptionsItems().get(i), context, branchName);
				if (res!=null)
					return res;
			}
		}
		if (root.getConfigurationRuleItems() != null) {
			for (int i=0; i<root.getConfigurationRuleItems().size(); i++) {
				if (root.getConfigurationRuleItems().get(i).getType() == Type.CONFIGURATION_RULE) {
					ConfigurationRuleItem crItem = (ConfigurationRuleItem)root.getConfigurationRuleItems().get(i);

					if (stringToDel.getStage() == Stage.PRODUCTION || crItem.getStage() == Stage.DEVELOPMENT) {

						if (crItem.getConfiguration()!=null && crItem.getConfiguration()!=null) {														
							ValidationCache.Info info = tester.getInfo(context, season, crItem.getStage(), crItem.getMinAppVersion());
							try {
								VerifyRule.fullConfigurationEvaluation(crItem.getRule().getRuleString(), crItem.getConfiguration(), info.minimalInvoker, info.maximalInvoker);
							} catch (ValidationException e) {
								return "Cannot delete string key '" + stringToDel.getKey() + "'. The configuration '" + crItem.getNameSpaceDotName() + "' in branch '" + branchName + "' is invalid:" + e.getMessage();		
							}
						}						
					}
				}
				String res = validateDeletedStringIsNotInUse(tester, stringToDel, root.getConfigurationRuleItems().get(i), context, branchName);
				if (res!=null)
					return res;
			}
		}

		return null;

	}
	
	private String validateDeletedStringIsNotInUseInNotifications(ValidationCache tester, OriginalString stringToDel, ServletContext context, String branchName) throws JSONException, GenerationException
	{
		for (AirlockNotification notification:season.getNotifications().getNotificationsList()) {
			if (notification.getConfiguration()!=null && notification.getConfiguration()!=null) {														
				ValidationCache.Info info = tester.getInfo(context, season, notification.getStage(), notification.getMinAppVersion());
				try {
					VerifyRule.fullConfigurationEvaluation(notification.getRegistrationRule().getRuleString(), notification.getConfiguration(), info.minimalInvoker, info.maximalInvoker);
				} catch (ValidationException e) {
					return "Cannot delete string key '" + stringToDel.getKey() + "'. The notification '" + notification.getName() + "' is invalid:" + e.getMessage();		
				}
			}		
		}
		
		return null;
	}

	public String validateProdConfigNotUsingString(String stringKey, Stage newStage, BaseAirlockItem root, ServletContext context,
			ValidationCache tester
			) throws JSONException, GenerationException {
		
		if (root.getType().equals(Type.CONFIGURATION_RULE)) {
			ConfigurationRuleItem crItem = (ConfigurationRuleItem)root;
			//if config in prod is using the string whom we are updating to dev
			if (newStage == Stage.DEVELOPMENT && crItem.getStage() == Stage.PRODUCTION) {

				if (crItem.getConfiguration()!=null) {
					try {
						ValidationCache.Info info = tester.getInfo(context, season, crItem.getStage(), crItem.getMinAppVersion());
						VerifyRule.fullConfigurationEvaluation(crItem.getRule().getRuleString(), crItem.getConfiguration(), info.minimalInvoker, info.maximalInvoker);

					} catch (ValidationException e) {
						return "Unable to update the string '" + stringKey + "'. Either the minimum version of the string is higher than the minimum version of the configuration '" + crItem.getNameSpaceDotName() + "' that is using it, or the stage of the string is development while the stage of the configuration is production.";		
					}
				}				
			}
		}
		
		//String are only used in configuration field in ConfigurationRule items
		//take into account the string stage and minAppVersion
		if (root.getFeaturesItems()!=null) {
			for (int i=0; i<root.getFeaturesItems().size(); i++) {								
				//String res = validateProdConfigNotUsingString(stringKey, newStage, root.getFeaturesItems().get(i), context, maximalInputSamplesMap, minimalInputSamplesMap, enStringsMap, javascriptUtilitiesMap);
				String res = validateProdConfigNotUsingString(stringKey, newStage, root.getFeaturesItems().get(i), context, tester);
				if (res!=null)
					return res;
			}
		}
		if (root.getConfigurationRuleItems() != null) {
			for (int i=0; i<root.getConfigurationRuleItems().size(); i++) {
				//String res = validateProdConfigNotUsingString(stringKey, newStage, root.getConfigurationRuleItems().get(i), context, maximalInputSamplesMap, minimalInputSamplesMap, enStringsMap, javascriptUtilitiesMap);
				String res = validateProdConfigNotUsingString(stringKey, newStage, root.getConfigurationRuleItems().get(i), context, tester);
				if (res!=null)
					return res;
			}
		}

		return null;

	}

	public int length() {
		return origStringsList.size();
	}	

	public OriginalStrings duplicateForNewSeason (String minVersion, Map<String, OriginalString> originalStringsDB, Season newSeason, String origServerVersion) {
		OriginalStrings res = new OriginalStrings(newSeason);
		LinkedList<OriginalString> strList = new LinkedList<OriginalString>();
		for (int i=0; i<origStringsList.size(); i++) {
			OriginalString origStr = origStringsList.get(i).duplicateForNewSeason(minVersion, newSeason.getUniqueId(), origServerVersion, supportedLanguages);
			strList.add(origStr);
			originalStringsDB.put(origStr.getUniqueId().toString(), origStr);
		}

		res.setOrigStringsList(strList);
		//res.setSupportedLanguages(supportedLanguages);
		res.setSupportedLanguages(Utilities.cloneStringsList(supportedLanguages));		

		return res;
	}


	public OriginalString get(int index) {
		return origStringsList.get(index);
	}

	//if stage == null return all
	//if minAppVersion == null return all
	public JSONObject toEnStringsJSON(Stage stage) throws JSONException {
		JSONObject res = new JSONObject();
		res.put(Constants.JSON_FIELD_MOST_RECENT_TRANSLATION, season.getLastModified().getTime());
		JSONObject stringsJSON = new JSONObject();
		for (int i=0;i<origStringsList.size(); i++) {
			if (stage != null && origStringsList.get(i).getStage() == Stage.DEVELOPMENT && stage == Stage.PRODUCTION) 
				continue; //don't use string in dev when prod is requested
			
			stringsJSON.put (origStringsList.get(i).getKey(), origStringsList.get(i).getValue());
		}

		res.put(Constants.JSON_FIELD_STRINGS, stringsJSON);
		return res;
	}

	//if stage == null return all
	public JSONObject toLocaleStringsJSON(String locale, Stage stage) throws JSONException {
		JSONObject res = new JSONObject();
		res.put(Constants.JSON_FIELD_MOST_RECENT_TRANSLATION, season.getLastModified().getTime());
		JSONObject stringsJSON = new JSONObject();
		for (int i=0;i<origStringsList.size(); i++) {
			if (stage != null && origStringsList.get(i).getStage() == Stage.DEVELOPMENT && stage == Stage.PRODUCTION) 
				continue; //don't use string in dev when prod is requested

			String value = null;
			LocaleTranslationData localeTranslationData = origStringsList.get(i).getStringTranslations().getTranslationDataPerLocale(locale);
			if (localeTranslationData!=null && localeTranslationData.getTranslatedValue()!=null) {				
				value = localeTranslationData.getTranslatedValue();				
			}
			else if (origStringsList.get(i).getInternationalFallback()!=null) {
				value = origStringsList.get(i).getInternationalFallback();	 					 
			}

			if (value != null) {				
				stringsJSON.put (origStringsList.get(i).getKey(), value);
			}
		}

		res.put(Constants.JSON_FIELD_STRINGS, stringsJSON);
		return res;
	}

	public String toTranslationFormatForNewStrings(List<String> ids) {
		StringWriter stringWriter = new StringWriter();
		PrintWriter writer = new PrintWriter(stringWriter);
		

		//create list for smartling
		ArrayList<SmartlingData> smarlingItems = new ArrayList<SmartlingData>();
		for (int i=0;i<origStringsList.size(); i++) {
			if (ids==null || ids.size() == 0 || ids.contains(origStringsList.get(i).getUniqueId().toString())) { //if all strings are needed or this string is in the list
				if (origStringsList.get(i).getStatus().equals(StringStatus.REVIEWED_FOR_TRANSLATION)) {
					smarlingItems.add(new SmartlingData(origStringsList.get(i).getKey(), origStringsList.get(i).getValue(), origStringsList.get(i).getTranslationInstruction(), origStringsList.get(i).getVariant()));
				}
			}
		}
		
		ConvertTranslations.printSmartling(writer, smarlingItems, false);

		return stringWriter.toString();
	}
	
	public String toStringsTranslationFormat() {
		StringWriter stringWriter = new StringWriter();
		PrintWriter writer = new PrintWriter(stringWriter);
		
		//create list for smartling
		ArrayList<SmartlingData> smarlingItems = new ArrayList<SmartlingData>();
		for (int i=0;i<origStringsList.size(); i++) {
			smarlingItems.add(new SmartlingData(origStringsList.get(i).getKey(), origStringsList.get(i).getValue(), origStringsList.get(i).getTranslationInstruction(), origStringsList.get(i).getVariant()));
		}
		
		ConvertTranslations.printSmartling(writer, smarlingItems, false);

		return stringWriter.toString();
	}
	
	//return true if production string added false otherwise
	public boolean addLocaleTranslations(TranslatedStrings translationStringsObj, String locale) {
		HashMap<String, TranslatedString> translatedStringsMap = translationStringsObj.getTranslatedStringsMap();
		Set<String> keys = translatedStringsMap.keySet();
		boolean containsProductionStrings = false;

		for (String key:keys) {
			OriginalString origStr = origStringsMap.get(key);
			if (origStr == null)
				continue; //the string was deleted from original string - dont add its translation
			//StringTranslations strTranslations = origStr.getStringTranslations();
			//strTranslations.addLocaleTranslation(locale, strTranslations.new LocaleTranslationData(translatedStringsMap.get(key).getTranslation(), null));
			origStr.addStringTranslationForLocale(locale, translatedStringsMap.get(key).getTranslation(), supportedLanguages);
			if (origStr.getStage() == Stage.PRODUCTION)
				containsProductionStrings = true;
		}

		return containsProductionStrings;
	}
	
	
	public LinkedList<AirlockChangeContent> upgrade(String fromVersion, ServletContext context, boolean updateOnlyOrigStr) throws IOException, JSONException {
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		String separator = ds.getSeparator();
		LinkedList<AirlockChangeContent> changesArr = new LinkedList<AirlockChangeContent>();
		if (fromVersion.equals("V2")) {
			isUpgradedPre21Season = true;
			Stage changeStage = Stage.DEVELOPMENT;
			List<String> locales = TranslationUtilities.getLocalesByStringsFiles(season.getProductId().toString(), season.getUniqueId().toString(), ds, logger);
			for (String locale:locales) {
				if (locale.equals(Constants.DEFAULT_LANGUAGE))
					continue; //skip the strings__en.json file
				
				JSONObject localeStringsObj = TranslationUtilities.readLocaleStringsFile(locale, season, context, logger, null);
				JSONObject localeStrings = localeStringsObj.getJSONObject(Constants.JSON_FIELD_STRINGS);
				@SuppressWarnings("unchecked")
				Set<String> stringKeys = localeStrings.keySet();
				for (String strKey: stringKeys) {
					OriginalString origStr = origStringsMap.get(strKey);
					if (origStr == null)
						continue; //this is because teh string was deleted but not removed from the locale strings file
					
					if (!localeStrings.getString(strKey).equals(origStr.getInternationalFallback()))
						origStr.addStringTranslationForLocale(locale, localeStrings.getString(strKey), supportedLanguages); //add only if string was actually translated
					
					if (origStr.getStage() == Stage.PRODUCTION)
						changeStage = Stage.PRODUCTION;
					
				}
				
				try {
					addSupportedLanguage(locale, null);
				} catch (IllegalArgumentException iae){
					//skip this error, just dont add the string
				}
			}
			stringXref = new StringXref(); // just in case
			changesArr.addAll(AirlockFilesWriter.writeOriginalStrings(season, context, changeStage));
			
			if (!updateOnlyOrigStr) {
				//remove old local translation files
				for (String locale:locales) {
					String localeStringsFilePath = Constants.SEASONS_FOLDER_NAME+separator+season.getProductId().toString()+
							separator+season.getUniqueId().toString()+separator+Constants.TRANSLATIONS_FOLDER_NAME + 
							separator + Constants.STRINGS_FILE_NAME_PREFIX + locale + Constants.STRINGS_FILE_NAME_EXTENSION;
					ds.deleteData(localeStringsFilePath, true);
				}
				changesArr.addAll(AirlockFilesWriter.writeEnStringsFiles(season, context, true));
				changesArr.addAll(AirlockFilesWriter.writeAllLocalesStringsFiles(season, context, true, false));
			}
		}	
		else if (fromVersion.equals("V2.5")) {
			// always save it to make sure the JSON is written with translatorId, transationVariant 
			changesArr.addAll(AirlockFilesWriter.writeOriginalStrings(season, context, Stage.PRODUCTION));
		}
		return changesArr;
	}
	
	//TODO: If no value specified? use international fallback? override? if exists val+override?
	public JSONObject getLocaleTranslationStatus (String locale, List<String> stringsIds) throws JSONException {
		JSONArray translationsStatusesArr = new JSONArray();		
		for (OriginalString origStr: origStringsList) {
			if (stringsIds == null || stringsIds.size() == 0 || stringsIds.contains(origStr.getUniqueId().toString())) {
				JSONObject strStatus = new JSONObject();
				strStatus.put(Constants.JSON_FIELD_UNIQUE_ID, origStr.getUniqueId().toString());
				strStatus.put(Constants.JSON_FIELD_KEY, origStr.getKey());
				
				LocaleTranslationData transData = origStr.getStringTranslations().getTranslationDataPerLocale(locale);
				if (transData == null) {
					strStatus.put(Constants.JSON_FIELD_STATUS, TranslationStatus.NOT_TRANSLATED.toString()); 					
				}
				else {
					strStatus.put(Constants.JSON_FIELD_STATUS, transData.getTranslationStatus()==null?null:transData.getTranslationStatus().toString());					
				}
				
				if (transData == null || transData.getTranslatedValue()==null) {
					if (origStr.getInternationalFallback()!=null) {
						strStatus.put(Constants.JSON_FIELD_VALUE, origStr.getInternationalFallback());
					}
				} else {
					strStatus.put(Constants.JSON_FIELD_VALUE, transData.getTranslatedValue());
				}
				
				translationsStatusesArr.add(strStatus);
			}
		}
	
		JSONObject res = new JSONObject();
		res.put(Constants.JSON_FIELD_STRING_STATUSES, translationsStatusesArr);
		
		return res;
	}

	public JSONObject getStringsStatus(List<String> stringIds) throws JSONException {
		JSONArray stringsStatusesArr = new JSONArray();
		LinkedList<OriginalString> stringsList = season.getOriginalStrings().getOrigStringsList();
		for (OriginalString origStr: stringsList) {
			if (stringIds == null || stringIds.size() == 0 || stringIds.contains(origStr.getUniqueId().toString())) {
				JSONObject strStatus = new JSONObject();
				strStatus.put(Constants.JSON_FIELD_UNIQUE_ID, origStr.getUniqueId().toString());
				strStatus.put(Constants.JSON_FIELD_KEY, origStr.getKey());
				strStatus.put(Constants.JSON_FIELD_STATUS, origStr.getStatus()==null?null:origStr.getStatus().toString());
				
				stringsStatusesArr.add(strStatus);
			}
		}

		JSONObject res = new JSONObject();
		res.put(Constants.JSON_FIELD_STRING_STATUSES, stringsStatusesArr);
		
		return res;
	}

	private JSONObject getOrigStringStatusSummary (OriginalString origStr, List<String> locales, Boolean showTranslations) throws JSONException {
		JSONObject localeStrData = new JSONObject();
		localeStrData.put(Constants.JSON_FIELD_UNIQUE_ID, origStr.getUniqueId().toString());
		localeStrData.put(Constants.JSON_FIELD_KEY, origStr.getKey());
		localeStrData.put(Constants.JSON_FIELD_VALUE, origStr.getValue());
		localeStrData.put(Constants.JSON_FIELD_STATUS, origStr.getStatus().toString());
		localeStrData.put(Constants.JSON_FIELD_INTERNATIONAL_FALLBACK, origStr.getInternationalFallback());
		localeStrData.put(Constants.JSON_FEATURE_FIELD_STAGE, origStr.getStage().toString());
		localeStrData.put(Constants.JSON_FIELD_MAX_STRING_SIZE, origStr.getMaxStringSize());
		localeStrData.put(Constants.JSON_FIELD_TRANSLATION_INSTRUCTION, origStr.getTranslationInstruction());
		localeStrData.put(Constants.JSON_FIELD_LAST_MODIFIED, origStr.getLastModified().getTime());
		localeStrData.put(Constants.JSON_FEATURE_FIELD_CREATOR, origStr.getCreator());

		JSONArray translationStatusArr = new JSONArray();
		if (locales != null && locales.size() > 0) {
			for (String locale:locales) {
				if (locale.equals(Constants.DEFAULT_LANGUAGE))
					continue;
				translationStatusArr.add(getOrigStringTranslationStatusSummary(origStr, locale, showTranslations));
			}
		}
		else {
			for (String locale:supportedLanguages) {
				if (locale.equals(Constants.DEFAULT_LANGUAGE))
					continue;
				translationStatusArr.add(getOrigStringTranslationStatusSummary(origStr, locale, showTranslations));
			}
		}
			
		localeStrData.put(Constants.JSON_FIELD_TRANSLATIONS, translationStatusArr);
		
		return localeStrData;
	}
	
	//TODO: what about internationalFallback?
	private JSONObject getOrigStringTranslationStatusSummary (OriginalString origStr, String locale, Boolean showTranslations) throws JSONException {
		JSONObject res = new JSONObject();
		if (origStr.getStringTranslations()!=null && origStr.getStringTranslations().getTranslationDataPerLocale(locale)!=null &&
				origStr.getStringTranslations().getTranslationDataPerLocale(locale).getTranslationStatus()!=null) {
			res.put(Constants.JSON_FIELD_TRANSLATION_STATUS, origStr.getStringTranslations().getTranslationDataPerLocale(locale).getTranslationStatus().toString());
			res.put(Constants.JSON_FIELD_ISSUE_STATUS, origStr.getStringTranslations().getTranslationDataPerLocale(locale).getIssueStatus().toString());

			//res.put(Constants.JSON_FIELD_ISSUE_ID, origStr.getStringTranslations().getTranslationDataPerLocale(locale).getIssueId());
			if (showTranslations)
				res.put(Constants.JSON_FIELD_TRANSLATION, origStr.getStringTranslations().getTranslationDataPerLocale(locale).getTranslatedValue());
		}
		else {
			if (origStr.getStatus().equals(Constants.StringStatus.NEW_STRING) || origStr.getStatus().equals(Constants.StringStatus.READY_FOR_TRANSLATION) || origStr.getStatus().equals(Constants.StringStatus.REVIEWED_FOR_TRANSLATION) ) {
				res.put(Constants.JSON_FIELD_TRANSLATION_STATUS, TranslationStatus.NOT_TRANSLATED.toString());
			} 
			else { 
				res.put(Constants.JSON_FIELD_TRANSLATION_STATUS, TranslationStatus.IN_TRANSLATION.toString());
			}
		}
		res.put(Constants.JSON_FIELD_lOCALE, locale);
		return res;
	}
	
	public JSONObject getTranslationStatusSummary(List<String> ids, List<String> locales, Boolean showTranslations, Map<String, OriginalString> originalStringsDB) throws JSONException {
		JSONObject res = new JSONObject();
		JSONArray stringSummaryArr = new JSONArray();
		
		if (ids == null || ids.size() == 0) {
			for (OriginalString origStr:origStringsList) 
				stringSummaryArr.add(getOrigStringStatusSummary(origStr, locales, showTranslations));
		}
		else {
			for (String id:ids) {
				OriginalString origStr = originalStringsDB.get(id); //i know that exists since after validation
				stringSummaryArr.add(getOrigStringStatusSummary(origStr, locales, showTranslations));
			}
		}
		res.put(Constants.JSON_FIELD_TRANSLATION_SUMMARY, stringSummaryArr);
		return res;
	}
}
