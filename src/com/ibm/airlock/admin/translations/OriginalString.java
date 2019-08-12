package com.ibm.airlock.admin.translations;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response.Status;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Constants.Stage;
import com.ibm.airlock.Constants.StringStatus;
import com.ibm.airlock.Constants.StringsOutputMode;
import com.ibm.airlock.Constants.TranslationStatus;
import com.ibm.airlock.Strings;
import com.ibm.airlock.admin.authentication.UserInfo;
import com.ibm.airlock.TranslationServices;
import com.ibm.airlock.admin.BaseAirlockItem;
import com.ibm.airlock.admin.Branch;
import com.ibm.airlock.admin.GenerationException;
import com.ibm.airlock.admin.Season;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.ValidationCache;
import com.ibm.airlock.admin.ValidationResults;
import com.ibm.airlock.admin.translations.OriginalString.StringTranslations.LocaleTranslationData;
import com.ibm.airlock.engine.Environment;
import com.ibm.airlock.Constants.Action;
import com.ibm.airlock.Constants.IssueStatus;

/*
  Original strings file structure:
  The key is the id

 {
	"seasonId": 1479797461540,
	"strings": [{
		"key": "hello.world",
		"value": "Hello World!",
		"lastModified": 1479797461540,		
		"internationalFallback": "Hello",
		"minAppVersion":"0.1",
		"stage":"DEVELOPMENT",
		"translations":{
			"en":{
				"translatedValue":"Hi",
				"shadowValue":"hi!"
			},
			"fr":{
				"translatedValue":"Bonjour",
				"shadowValue":"Bonjour!"
			}		
		}
	}, {
		"key": "app.title",
		"value": "The Weather Company",
		"lastModified": 1479797461540,		
		"internationalFallback": "Weather",
		"minAppVersion":"0.1",
		"stage":"DEVELOPMENT",
		"translations":{
			"en":{
				"translatedValue":"The Weather Comp.",
				"shadowValue":"Weather"
			},
			"fr":{
				"translatedValue":"Temps",
				"shadowValue":null
			}		
		}
	}]
}


 strings__enDEVELOPMENT.json:
 {
	"mostRecentTranslation": 1479797461540,
	"strings": {
		"hello.world": "hi!",
		"app.title": "Temps"
	}
}

strings__enPRODUCTION.json:
 {
	"mostRecentTranslation": 1479797461540,
	"strings": {
		"hello.world": "Hi",
		"app.title": "The Weather Comp."
	}
}
 */

public class OriginalString {

	//----------------------------------------------------
	public class StringTranslations {
		//----------------------------------------------------
		public class LocaleTranslationData {
			private String translatedValue = null; //must exists both in add and update - can be null
			private String shadowValue = null; //must exists both in add and update - can be null
			//private String issue = null; //optional both in add and update - can be null
			//private String issueId = null; //optional both in add and update - can be null
			private IssueStatus issueStatus = IssueStatus.NO_ISSUES;
			private Constants.TranslationStatus translationStatus = null; //must exists both in add and update
			private String newTranslationAvailable = null; //optional both in add and update - can be null
			
			public LocaleTranslationData(String translatedValue, String shadowValue, TranslationStatus status, IssueStatus issueStatus, String newTranslationAvailable) {
				this.translatedValue = translatedValue;
				this.shadowValue = shadowValue;
				this.translationStatus = status;
				//this.issue = issue;
				this.issueStatus = (issueStatus == null) ? IssueStatus.NO_ISSUES : issueStatus;
				this.newTranslationAvailable = newTranslationAvailable;
			}
			
			public LocaleTranslationData() {
			}
			
			public String getTranslatedValue() {
				return translatedValue;
			}
			public void setTranslatedValue(String translatedValue) {
				this.translatedValue = translatedValue;
			}
			public String getShadowValue() {
				return shadowValue;
			}
			public void setShadowValue(String shadowValue) {
				this.shadowValue = shadowValue;
			}	

			public void setOverrideValue(String overrideVal) {
				this.translatedValue = overrideVal;
				this.translationStatus = TranslationStatus.OVERRIDE;
			}

			public Constants.TranslationStatus getTranslationStatus() {
				return translationStatus;
			}

			public void setTranslationStatus(Constants.TranslationStatus translationStatus) {
				this.translationStatus = translationStatus;
			}

			public String getNewTranslationAvailable() {
				return newTranslationAvailable;
			}

			public void setNewTranslationAvailable(String newTranslationAvailable) {
				this.newTranslationAvailable = newTranslationAvailable;
			}

			public IssueStatus getIssueStatus() {
				return issueStatus;
			}
			public void setIssueStatus(IssueStatus status) {
				issueStatus = status == null ? IssueStatus.NO_ISSUES : status;
			}
			public void setIssueStatus(String status) {
				if (status == null)
					setIssueStatus(IssueStatus.NO_ISSUES);
				else
					setIssueStatus(IssueStatus.valueOf(status));
			}

			public LocaleTranslationData duplicate() {
				LocaleTranslationData res = new LocaleTranslationData();
				//res.setIssue(issue);
				res.setIssueStatus(issueStatus);
				res.setNewTranslationAvailable(newTranslationAvailable);
				res.setShadowValue(shadowValue);
				res.setTranslatedValue(translatedValue);
				res.setTranslationStatus(translationStatus);
				return res;
			}
			
			public void fromJSON (JSONObject input, Environment env) throws JSONException {		
				if (input.containsKey(Constants.JSON_FIELD_TRANSLATED_VALUE) && input.get(Constants.JSON_FIELD_TRANSLATED_VALUE)!=null) 
					translatedValue = input.getString(Constants.JSON_FIELD_TRANSLATED_VALUE);

				if (input.containsKey(Constants.JSON_FIELD_SHADOW_VALUE) && input.get(Constants.JSON_FIELD_SHADOW_VALUE)!=null) 
					shadowValue = input.getString(Constants.JSON_FIELD_SHADOW_VALUE);
				
				//if (input.containsKey(Constants.JSON_FIELD_OVERRIDE_VALUE) && input.get(Constants.JSON_FIELD_OVERRIDE_VALUE)!=null) 
				//	overrideValue = input.getString(Constants.JSON_FIELD_OVERRIDE_VALUE);
				
				if (TranslationServices.isTranslationStatusesSupport(env)) {
					if (input.containsKey(Constants.JSON_FIELD_TRANSLATION_STATUS) && input.get(Constants.JSON_FIELD_TRANSLATION_STATUS)!=null)
						translationStatus = Utilities.strToTranslationStatus(input.getString(Constants.JSON_FIELD_TRANSLATION_STATUS));
	
					//if (input.containsKey(Constants.JSON_FIELD_ISSUE) && input.get(Constants.JSON_FIELD_ISSUE)!=null) 
					//	issue = input.getString(Constants.JSON_FIELD_ISSUE);
					//if (input.containsKey(Constants.JSON_FIELD_ISSUE_ID) && input.get(Constants.JSON_FIELD_ISSUE_ID)!=null) 
					//	issueId = input.getString(Constants.JSON_FIELD_ISSUE_ID);
					String issuestatus = input.optString(Constants.JSON_FIELD_ISSUE_STATUS);
					setIssueStatus(issuestatus);

					if (input.containsKey(Constants.JSON_FIELD_NEW_TRANSLATION_AVAILABLE) && input.get(Constants.JSON_FIELD_NEW_TRANSLATION_AVAILABLE)!=null) 
						newTranslationAvailable = input.getString(Constants.JSON_FIELD_NEW_TRANSLATION_AVAILABLE);
				} else {
					translationStatus = TranslationStatus.NONE;
				}
			}
			
			public JSONObject toJson(Environment env) throws JSONException {
				JSONObject res = new JSONObject();
				
				res.put(Constants.JSON_FIELD_TRANSLATED_VALUE, translatedValue);				
				res.put(Constants.JSON_FIELD_SHADOW_VALUE, shadowValue);				
				//res.put(Constants.JSON_FIELD_OVERRIDE_VALUE, overrideValue);
				if (TranslationServices.isTranslationStatusesSupport(env)) {
					res.put(Constants.JSON_FIELD_TRANSLATION_STATUS, translationStatus == null?null:translationStatus.toString());
					res.put(Constants.JSON_FIELD_ISSUE_STATUS, issueStatus.toString());
					//res.put(Constants.JSON_FIELD_ISSUE_ID, issueId);
					//res.put(Constants.JSON_FIELD_ISSUE, issue);
					res.put(Constants.JSON_FIELD_NEW_TRANSLATION_AVAILABLE, newTranslationAvailable);
				}					
				return res;
			}
			
			public ValidationResults validateLocaleTranslationData(JSONObject localeTranslationDataObj, ServletContext context) {
				try {					
					//translatedValue
					if (!localeTranslationDataObj.containsKey(Constants.JSON_FIELD_TRANSLATED_VALUE)) {
						return new ValidationResults("The translatedValue field is missing.", Status.BAD_REQUEST);
					}
					
					if (localeTranslationDataObj.get(Constants.JSON_FIELD_TRANSLATED_VALUE)!=null)
						localeTranslationDataObj.getString(Constants.JSON_FIELD_TRANSLATED_VALUE); //validate that is string

					
					//shadowValue
					if (!localeTranslationDataObj.containsKey(Constants.JSON_FIELD_SHADOW_VALUE)) {
						return new ValidationResults("The shadowValue field is missing.", Status.BAD_REQUEST);
					}

					if (localeTranslationDataObj.get(Constants.JSON_FIELD_SHADOW_VALUE)!=null)
						localeTranslationDataObj.getString(Constants.JSON_FIELD_SHADOW_VALUE); //validate that is string
/*
					//overrideValue - optional
					if (localeTranslationDataObj.containsKey(Constants.JSON_FIELD_OVERRIDE_VALUE) && localeTranslationDataObj.get(Constants.JSON_FIELD_OVERRIDE_VALUE)!=null)
						localeTranslationDataObj.getString(Constants.JSON_FIELD_OVERRIDE_VALUE); //validate that is string
*/
					//translationStatus
					TranslationStatus translationStatusTmp = null;
					if (!localeTranslationDataObj.containsKey(Constants.JSON_FIELD_TRANSLATION_STATUS) || localeTranslationDataObj.getString(Constants.JSON_FIELD_TRANSLATION_STATUS) == null) {
						return new ValidationResults("The translationStatus field is missing.", Status.BAD_REQUEST);					
					}

					//issue
					//if (localeTranslationDataObj.containsKey(Constants.JSON_FIELD_ISSUE) && localeTranslationDataObj.get(Constants.JSON_FIELD_ISSUE)!=null)
					//	localeTranslationDataObj.getString(Constants.JSON_FIELD_ISSUE); //validate that is string

					//newTranslationAvailable
					if (localeTranslationDataObj.containsKey(Constants.JSON_FIELD_NEW_TRANSLATION_AVAILABLE) && localeTranslationDataObj.get(Constants.JSON_FIELD_NEW_TRANSLATION_AVAILABLE)!=null)
						localeTranslationDataObj.getString(Constants.JSON_FIELD_NEW_TRANSLATION_AVAILABLE); //validate that is string
					
					
					//issueId
					if (localeTranslationDataObj.containsKey(Constants.JSON_FIELD_ISSUE_STATUS) && localeTranslationDataObj.get(Constants.JSON_FIELD_ISSUE_STATUS)!=null)
						localeTranslationDataObj.getString(Constants.JSON_FIELD_ISSUE_STATUS); //validate that is string
					
					
					String translationStatusStr = localeTranslationDataObj.getString(Constants.JSON_FIELD_TRANSLATION_STATUS);
					translationStatusTmp = Utilities.strToTranslationStatus(translationStatusStr);
					if (translationStatusTmp == null) {
						return new ValidationResults("Illegal translationStatus: '" + translationStatusStr + "'", Status.BAD_REQUEST);
					}				

				} catch (JSONException jsne) {
					return new ValidationResults(jsne.getMessage(), Status.BAD_REQUEST);
				}
				catch (ClassCastException cce) {
					return new ValidationResults("Illegal LocaleTranslationData JSON: " + cce.getMessage(), Status.BAD_REQUEST);
				}
				return null;
			}

		}

		//----------------------------------------------------
		public void clear() {
			stringTranslationsMap.clear();
		}
		
		//map between locale and translation json 
		HashMap <String, LocaleTranslationData> stringTranslationsMap = new HashMap<String, LocaleTranslationData>();			
		
		public StringTranslations duplicate() {
			StringTranslations res = new StringTranslations();
			Set<String> locales = getStringTranslationsMap().keySet();
			for (String locale:locales) {
				LocaleTranslationData DuplicatedTransData = getStringTranslationsMap().get(locale).duplicate();
				res.stringTranslationsMap.put(locale, DuplicatedTransData);
			}
			return res;
		}
		
		public HashMap<String, LocaleTranslationData> getStringTranslationsMap() {
			return stringTranslationsMap;
		}

		public void setStringTranslationsMap(HashMap<String, LocaleTranslationData> stringTranslationsMap) {
			this.stringTranslationsMap = stringTranslationsMap;
		}
		
		public void addLocaleTranslation (String locale, LocaleTranslationData translationData) {
			stringTranslationsMap.put (locale, translationData);
		}
		
		public void removeLocaleTranslation (String locale) {
			stringTranslationsMap.remove (locale);
		}
				
		public LocaleTranslationData getTranslationDataPerLocale (String locale) {
			return stringTranslationsMap.get(locale);
		}
		
		//TODO: should call validate to be sure that in the correct format
		public void fromJSON (JSONObject input, Environment env) throws JSONException {		
			@SuppressWarnings("unchecked")
			Set<String> locales = input.keySet();
			for (String locale:locales) {
				LocaleTranslationData localeTranslationData = new LocaleTranslationData();
				localeTranslationData.fromJSON(input.getJSONObject(locale), env);
				addLocaleTranslation(locale, localeTranslationData);
			}			
		}
		
		public JSONObject toJson(Environment env) throws JSONException {			
			JSONObject res = new JSONObject();
			
			Set<String> locales = stringTranslationsMap.keySet();
			for (String locale:locales) {
				res.put(locale, stringTranslationsMap.get(locale).toJson(env));
			}
			
			return res;
		}
		
		public ValidationResults validateStringTranslations(JSONObject stringTranslationsObj, ServletContext context) {
			
			try {
				@SuppressWarnings("unchecked")
				Set<String> locales = stringTranslationsObj.keySet();
				for (String locale:locales) {
					LocaleTranslationData localeTranslationData = new LocaleTranslationData();
					localeTranslationData.validateLocaleTranslationData(stringTranslationsObj.getJSONObject(locale), context);					
				}					
			} catch (JSONException jsne) {
				return new ValidationResults(jsne.getMessage(), Status.BAD_REQUEST);
			}
			catch (ClassCastException cce) {
				return new ValidationResults("Illegal translations JSON: " + cce.getMessage(), Status.BAD_REQUEST);
			}
			return null;
		}

		public void setAllTranslationStatuses(TranslationStatus newTranslationStatus) {
			Set<String> locales = stringTranslationsMap.keySet();
			for (String locale:locales) {
				stringTranslationsMap.get(locale).setTranslationStatus(newTranslationStatus);
			}			
		}

	}
	//----------------------------------------------------
	private String value = null; //required in create and update
	private String key = null; //required in create and update
	private Date lastModified = null; //mandatory in update forbidden in create
	private Date lastSourceModification = null; //mandatory in update forbidden in create
	private String owner = null; //optional in create and update
	private String internationalFallback = null;  //optional in create and update
	protected Stage stage = null; //required in create and update		
	//protected String minAppVersion = null; //required in create and update
	private UUID seasonId = null; //required in create and update
	private UUID uniqueId = null;//required update. forbidden in create
	private StringTranslations stringTranslations = new StringTranslations();  //mandatory in update forbidden in create
	private String translatorId = ""; //forbidden in create optional in update but when is set - cannot be changed by update 
	private StringStatus status = null; //forbidden in create optional in update but when is set - cannot be changed by update 
	protected String creator = null;	//c+u (creator not changed)
	protected String description = null; //opt in c+u (if missing or null in update don't change)
	
	private Integer maxStringSize = null; //optional 

	private String variant = "";
	private String translationInstruction = null;
	private String smartlingCreationProcess = null;

	//private Issues issueIds = null;  //optional both in add and update - can be null
	private IssueStatus issueStatus = IssueStatus.NO_ISSUES; 

	public OriginalString (String value) {
		this.value = value;
		this.lastModified = new Date();
		this.lastSourceModification = new Date();
	}

	public OriginalString (String key, String value) {
		this.key = key;
		this.value = value;
		this.internationalFallback = value;
		this.lastModified = new Date();
		this.lastSourceModification = new Date();
		this.stage = Stage.DEVELOPMENT;
		this.status = StringStatus.NEW_STRING;
	}

	public OriginalString(){}

	public UUID getUniqueId() {
		return uniqueId;
	}

	public void setUniqueId(UUID uniqueId) {
		this.uniqueId = uniqueId;
	}

	public String getValue() {
		return value;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	public Date getLastModified() {
		return lastModified;
	}
	
	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}
	
	public Date getLastSourceModification() {
		return lastSourceModification;
	}
	
	public void setLastSourceModification(Date lastSourceModification) {
		this.lastSourceModification = lastSourceModification;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getInternationalFallback() {
		return internationalFallback;
	}

	public void setInternationalFallback(String internationalFallback) {
		this.internationalFallback = internationalFallback;
	}

	public Stage getStage() {
		return stage;
	}

	public void setStage(Stage stage) {
		this.stage = stage;
	}
	
	public UUID getSeasonId() {
		return seasonId;
	}

	public void setSeasonId(UUID seasonId) {
		this.seasonId = seasonId;
	}

	public StringTranslations getStringTranslations() {
		return stringTranslations;
	}

	public void setStringTranslations(StringTranslations stringTranslations) {
		this.stringTranslations = stringTranslations;
	}

	public String getTranslatorId() { // the smartling hash
		return translatorId;
	}

	public void setTranslatorId(String translatorId) {
		this.translatorId = translatorId;
	}

	public StringStatus getStatus() {
		return status;
	}

	public void setStatus(StringStatus status) {
		this.status = status;
	}

	public Integer getMaxStringSize() {
		return maxStringSize;
	}

	public void setMaxStringSize(Integer maxStringSize) {
		this.maxStringSize = maxStringSize;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getCreator() {
		return creator;
	}
	public void setCreator(String creator) {
		this.creator = creator;
	}
	public String getVariant() {
		return this.variant;
	}
	public void setVariant(String variant) {
		this.variant = variant;
	}
	public String getTranslationInstruction() {
		return this.translationInstruction;
	}
	public void setTranslationInstruction(String instruction) {
		this.translationInstruction = instruction;
	}
	public String getSmartlingCreationProcess() {
		return smartlingCreationProcess;
	}
	public void setSmartlingCreationProcess(String processId) {
		this.smartlingCreationProcess = processId;
	}
	public IssueStatus getIssueStatus() {
		return issueStatus;
	}
	public void setIssueStatus(IssueStatus status) {
		issueStatus = (status == null) ? IssueStatus.NO_ISSUES : status;
	}
	public void setIssueStatus(String status) {
		if (status == null)
			setIssueStatus(IssueStatus.NO_ISSUES);
		else
			setIssueStatus(IssueStatus.valueOf(status));
	}

	public JSONObject toJson(StringsOutputMode outputMode, Season season) throws JSONException {
		JSONObject res = new JSONObject();
		res.put(Constants.JSON_FIELD_UNIQUE_ID, uniqueId==null?null:uniqueId.toString());
		res.put(Constants.JSON_FIELD_LAST_MODIFIED, lastModified.getTime());						
		res.put(Constants.JSON_FIELD_VALUE, value);
		res.put(Constants.JSON_FIELD_KEY, key);
		res.put(Constants.JSON_FEATURE_FIELD_STAGE, stage.toString());		
		res.put(Constants.JSON_FEATURE_FIELD_OWNER, owner);
		res.put(Constants.JSON_FIELD_INTERNATIONAL_FALLBACK, internationalFallback);
		res.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, seasonId==null?null:seasonId.toString());
		res.put(Constants.JSON_FIELD_ISSUE_STATUS, issueStatus.toString());
		if(creator != null) {
			res.put(Constants.JSON_FEATURE_FIELD_CREATOR, creator);
		}else {
			res.put(Constants.JSON_FEATURE_FIELD_CREATOR, Constants.JSON_FEATURE_FIELD_LEGACY_CREATOR);
		}
		res.put(Constants.JSON_FIELD_DESCRIPTION, description);
		

		Environment env = new Environment();
		env.setServerVersion(season.getServerVersion()); 	
		
		if (TranslationServices.isTranslationStatusesSupport(env)) {
			res.put(Constants.JSON_FIELD_LAST_SOURCE_MODIFICATION, lastSourceModification == null ? null : lastSourceModification.getTime());			
			res.put(Constants.JSON_FIELD_TRANSLATOR_ID, translatorId);
			res.put(Constants.JSON_FIELD_STATUS, status==null?null:status.toString());
			res.put(Constants.JSON_FIELD_STRINGS_VARIANT, variant);
			res.put(Constants.JSON_FIELD_SMARTLING_PROCESS, smartlingCreationProcess);
			res.put(Constants.JSON_FIELD_TRANSLATION_INSTRUCTION, translationInstruction);
			res.put(Constants.JSON_FIELD_MAX_STRING_SIZE, maxStringSize);
		}
	
		if (outputMode.equals(StringsOutputMode.INCLUDE_TRANSLATIONS))
			res.put(Constants.JSON_FIELD_TRANSLATIONS, stringTranslations.toJson(env));
		return res;
	}

	public void fromJSON (JSONObject input, Season season) throws JSONException {		
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

		if (input.containsKey(Constants.JSON_FIELD_VALUE) && input.get(Constants.JSON_FIELD_VALUE)!=null) 
			value = input.getString(Constants.JSON_FIELD_VALUE);

		if (input.containsKey(Constants.JSON_FIELD_KEY) && input.get(Constants.JSON_FIELD_KEY)!=null) 
			key = input.getString(Constants.JSON_FIELD_KEY);

		if (input.containsKey(Constants.JSON_FEATURE_FIELD_OWNER) && input.get(Constants.JSON_FEATURE_FIELD_OWNER)!=null) 
			owner = input.getString(Constants.JSON_FEATURE_FIELD_OWNER);
		
		if (input.containsKey(Constants.JSON_FEATURE_FIELD_CREATOR) && input.get(Constants.JSON_FEATURE_FIELD_CREATOR)!=null)
			creator = input.getString(Constants.JSON_FEATURE_FIELD_CREATOR).trim();

		if (input.containsKey(Constants.JSON_FIELD_INTERNATIONAL_FALLBACK) && input.get(Constants.JSON_FIELD_INTERNATIONAL_FALLBACK)!=null) 
			internationalFallback = input.getString(Constants.JSON_FIELD_INTERNATIONAL_FALLBACK);

		if (input.containsKey(Constants.JSON_FEATURE_FIELD_STAGE) && input.get(Constants.JSON_FEATURE_FIELD_STAGE)!=null)
			stage = Utilities.strToStage(input.getString(Constants.JSON_FEATURE_FIELD_STAGE));

		if (input.containsKey(Constants.JSON_FIELD_DESCRIPTION) && input.get(Constants.JSON_FIELD_DESCRIPTION)!=null) 
			description = input.getString(Constants.JSON_FIELD_DESCRIPTION).trim();
				
		if (input.containsKey(Constants.JSON_FEATURE_FIELD_SEASON_ID) && input.get(Constants.JSON_FEATURE_FIELD_SEASON_ID) != null) {
			String sStr = input.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID);			
			seasonId = UUID.fromString(sStr);			
		}
		String issue = input.optString(Constants.JSON_FIELD_ISSUE_STATUS);
		setIssueStatus(issue);

		Environment env = new Environment();
		env.setServerVersion(season.getServerVersion()); 	
		
		
		if (input.containsKey(Constants.JSON_FIELD_TRANSLATIONS) && input.get(Constants.JSON_FIELD_TRANSLATIONS) != null) {
			JSONObject stringTranslationsObj =  input.getJSONObject(Constants.JSON_FIELD_TRANSLATIONS);
			stringTranslations.fromJSON(stringTranslationsObj, env);			
		}		
		
		if (TranslationServices.isTranslationStatusesSupport(env)) {
			translatorId = input.optString(Constants.JSON_FIELD_TRANSLATOR_ID);
			if (translatorId == null)
				translatorId = input.optString("smartlingId"); // legacy name

			if (input.containsKey(Constants.JSON_FIELD_STATUS) && input.get(Constants.JSON_FIELD_STATUS)!=null) { 
				status = Utilities.strToStringStatus(input.getString(Constants.JSON_FIELD_STATUS));
			}
			else {
				status = StringStatus.NEW_STRING; //this is new string
			}
			
			if (input.containsKey(Constants.JSON_FIELD_LAST_SOURCE_MODIFICATION) && input.get(Constants.JSON_FIELD_LAST_SOURCE_MODIFICATION)!=null) { 
				long timeInMS = input.getLong(Constants.JSON_FIELD_LAST_SOURCE_MODIFICATION);
				lastSourceModification= new Date(timeInMS);
			}  else {
				lastSourceModification = new Date();
			}
			
			if (input.containsKey(Constants.JSON_FIELD_MAX_STRING_SIZE) && input.get(Constants.JSON_FIELD_MAX_STRING_SIZE)!=null) { 
				maxStringSize = input.getInt(Constants.JSON_FIELD_MAX_STRING_SIZE);				
			}  

			// disabled for now: there must always be a variant. If missing, generate a unique one
			//variant = input.optString(Constants.JSON_FIELD_STRINGS_VARIANT, Constants.NEW_VARIANT);
			//if (variant.isEmpty() || variant.equals(Constants.NEW_VARIANT))
			//	variant = generateVariant(key);
			variant = input.optString(Constants.JSON_FIELD_STRINGS_VARIANT);
			if (variant == null)
				variant = input.optString(Constants.JSON_FIELD_STRINGS_OLD_VARIANT); // legacy name
	
			smartlingCreationProcess = input.optString(Constants.JSON_FIELD_SMARTLING_PROCESS, null);
			translationInstruction = input.optString(Constants.JSON_FIELD_TRANSLATION_INSTRUCTION, null);			
		}
		else {
			status = StringStatus.NONE;
		}
	}


	public ValidationResults validateOriginalStringJSONForImport(JSONObject origStringObj, ServletContext context, UserInfo userInfo) {

		try {

			//seasonId
			if (!origStringObj.containsKey(Constants.JSON_FEATURE_FIELD_SEASON_ID) || origStringObj.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID) == null || origStringObj.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_SEASON_ID), Status.BAD_REQUEST);
			}

			//key
			if (!origStringObj.containsKey(Constants.JSON_FIELD_KEY) || origStringObj.getString(Constants.JSON_FIELD_KEY) == null || origStringObj.getString(Constants.JSON_FIELD_KEY).isEmpty()) {
				return new ValidationResults("The key field is missing.", Status.BAD_REQUEST);
			}


			//stage
			if (!origStringObj.containsKey(Constants.JSON_FEATURE_FIELD_STAGE) || origStringObj.getString(Constants.JSON_FEATURE_FIELD_STAGE) == null) {
				return new ValidationResults("The stage field is missing.", Status.BAD_REQUEST);
			}

			String stageStr = origStringObj.getString(Constants.JSON_FEATURE_FIELD_STAGE);
			Stage stageObj = Utilities.strToStage(stageStr);
			if (stageObj == null) {
				return new ValidationResults("Illegal stage: '" + stageStr + "'", Status.BAD_REQUEST);
			}
			
			//creator
			if (!origStringObj.containsKey(Constants.JSON_FEATURE_FIELD_CREATOR) || origStringObj.getString(Constants.JSON_FEATURE_FIELD_CREATOR) == null || origStringObj.getString(Constants.JSON_FEATURE_FIELD_CREATOR).isEmpty()) {
				return new ValidationResults("The creator field is missing.", Status.BAD_REQUEST);
			}

			//value
			if (!origStringObj.containsKey(Constants.JSON_FIELD_VALUE) || origStringObj.getString(Constants.JSON_FIELD_VALUE) == null || origStringObj.getString(Constants.JSON_FIELD_VALUE).isEmpty()) {
				return new ValidationResults("The value field is missing.", Status.BAD_REQUEST);
			}

			if (origStringObj.containsKey(Constants.JSON_FIELD_MAX_STRING_SIZE) && origStringObj.get(Constants.JSON_FIELD_MAX_STRING_SIZE)!=null) {
				Integer mss = origStringObj.getInt(Constants.JSON_FIELD_MAX_STRING_SIZE);	//validate legal integer
				if (mss<=0) {
					return new ValidationResults("The maximum translation length should be a positive integer.", Status.BAD_REQUEST);
				}
			}

			//internationalFallback are optional strings hence not being checked

				//status => can exists and not be changed
				if (origStringObj.containsKey(Constants.JSON_FIELD_STATUS) && origStringObj.get(Constants.JSON_FIELD_STATUS)!=null) {
					String statusStr = origStringObj.getString(Constants.JSON_FIELD_STATUS);
					StringStatus statusObj = Utilities.strToStringStatus(statusStr);
					if (statusObj == null) {
						return new ValidationResults("Illegal status: '" + statusStr + "'", Status.BAD_REQUEST);
					}
				}

		} catch (JSONException jsne) {
			return new ValidationResults(jsne.getMessage(), Status.BAD_REQUEST);
		}
		catch (ClassCastException cce) {
			return new ValidationResults("Illegal OriginalString JSON: " + cce.getMessage(), Status.BAD_REQUEST);
		}
		return null;
	}
	public ValidationResults validateOriginalStringJSON(JSONObject origStringObj, ServletContext context, UserInfo userInfo) {

		@SuppressWarnings("unchecked")
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

		try {

			Action action = Action.ADD;
			if (origStringObj.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && origStringObj.get(Constants.JSON_FIELD_UNIQUE_ID)!=null) {
				//if JSON contains uniqueId - update an existing feature otherwise create a new feature
				action = Action.UPDATE;
			}
			
			
			//seasonId
			if (!origStringObj.containsKey(Constants.JSON_FEATURE_FIELD_SEASON_ID) || origStringObj.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID) == null || origStringObj.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_SEASON_ID), Status.BAD_REQUEST);
			}

			String sId = (String)origStringObj.get(Constants.JSON_FEATURE_FIELD_SEASON_ID);
			Season season = seasonsDB.get(sId);
			if (season == null) {
				return new ValidationResults("The season of the given string does not exist.", Status.BAD_REQUEST);
			}
			
			//key
			if (!origStringObj.containsKey(Constants.JSON_FIELD_KEY) || origStringObj.getString(Constants.JSON_FIELD_KEY) == null || origStringObj.getString(Constants.JSON_FIELD_KEY).isEmpty()) {
				return new ValidationResults("The key field is missing.", Status.BAD_REQUEST);
			}			
			
			//validate key uniqueness
			String newKey = origStringObj.getString(Constants.JSON_FIELD_KEY);			
			for (int i=0; i<season.getOriginalStrings().length(); i++) {
				OriginalString os = season.getOriginalStrings().get(i);
				if (uniqueId == null || !uniqueId.equals(os.getUniqueId())) { //in update - skip yourself
					if (os.getKey().equals(newKey)) {
						return new ValidationResults("A string with the specified string ID already exists.", Status.BAD_REQUEST);
					}
				}
			}
			
			//creator
			if (!origStringObj.containsKey(Constants.JSON_FEATURE_FIELD_CREATOR) || origStringObj.getString(Constants.JSON_FEATURE_FIELD_CREATOR) == null || origStringObj.getString(Constants.JSON_FEATURE_FIELD_CREATOR).isEmpty()) {
				return new ValidationResults("The creator field is missing.", Status.BAD_REQUEST);
			}
			
			//stage
			if (!origStringObj.containsKey(Constants.JSON_FEATURE_FIELD_STAGE) || origStringObj.getString(Constants.JSON_FEATURE_FIELD_STAGE) == null) {
				return new ValidationResults("The stage field is missing.", Status.BAD_REQUEST);					
			}
			
			String stageStr = origStringObj.getString(Constants.JSON_FEATURE_FIELD_STAGE);
			Stage stageObj = Utilities.strToStage(stageStr);
			if (stageObj == null) {
				return new ValidationResults("Illegal stage: '" + stageStr + "'", Status.BAD_REQUEST);
			}	


			//value
			if (!origStringObj.containsKey(Constants.JSON_FIELD_VALUE) || origStringObj.getString(Constants.JSON_FIELD_VALUE) == null || origStringObj.getString(Constants.JSON_FIELD_VALUE).isEmpty()) {
				return new ValidationResults("The value field is missing.", Status.BAD_REQUEST);
			}
			
			//cannot add or update string with its translations			
			if (origStringObj.containsKey(Constants.JSON_FIELD_TRANSLATIONS) && origStringObj.get(Constants.JSON_FIELD_TRANSLATIONS)!=null) {
				JSONObject stringTranslationsObj = origStringObj.getJSONObject(Constants.JSON_FIELD_TRANSLATIONS);
				if (!stringTranslationsObj.isEmpty())
					return new ValidationResults("The translations field cannot be specified during string creation or update.", Status.BAD_REQUEST);
				//stringTranslations.validateStringTranslations(stringTranslationsObj, context);				
			}
			
			if (origStringObj.containsKey(Constants.JSON_FIELD_MAX_STRING_SIZE) && origStringObj.get(Constants.JSON_FIELD_MAX_STRING_SIZE)!=null) { 
				Integer mss = origStringObj.getInt(Constants.JSON_FIELD_MAX_STRING_SIZE);	//validate legal integer
				if (mss<=0) {
					return new ValidationResults("The maximum translation length should be a positive integer.", Status.BAD_REQUEST);
				}
			} 
			
			//internationalFallback are optional strings hence not being checked 

			if (action == Action.ADD) {
				//modification date => should not appear in creation
				if (origStringObj.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) && origStringObj.get(Constants.JSON_FIELD_LAST_MODIFIED)!=null) {
					return new ValidationResults("The lastModified field cannot be specified during string creation.", Status.BAD_REQUEST);						
				}
				
				//source modification date => should not appear in creation
				if (origStringObj.containsKey(Constants.JSON_FIELD_LAST_SOURCE_MODIFICATION) && origStringObj.get(Constants.JSON_FIELD_LAST_SOURCE_MODIFICATION)!=null) {
					return new ValidationResults("The lastSourceModification field cannot be specified during string creation.", Status.BAD_REQUEST);						
				}
			
				//translatorId => should not appear in creation
				if (origStringObj.containsKey(Constants.JSON_FIELD_TRANSLATOR_ID) && origStringObj.get(Constants.JSON_FIELD_TRANSLATOR_ID)!=null) {
					return new ValidationResults("The translatorId field cannot be specified during string creation.", Status.BAD_REQUEST);						
				}

				//status => should not appear in creation
				if (origStringObj.containsKey(Constants.JSON_FIELD_STATUS) && origStringObj.get(Constants.JSON_FIELD_STATUS)!=null) {
					return new ValidationResults("The status field cannot be specified during string creation.", Status.BAD_REQUEST);						
				}

				//string in production can be added only by Administrator or ProductLead
				if (stageObj.equals(Stage.PRODUCTION) && !validRole(userInfo)) {
					return new ValidationResults("Unable to add the string. Only a user with the Administrator, Product Lead or Translation Specialist role can add string in the production stage.", Status.UNAUTHORIZED);					
				}
			}
			else { //update
				
				//if string is in production or is updated from stage DEVELOPMENT to PRODUCTION
				if (stage.equals(Stage.PRODUCTION) || stageObj.equals(Stage.PRODUCTION)) {						
					//only productLead or Administrator can update string in production
					if (!validRole(userInfo)) {
						return new ValidationResults("Unable to update the string. Only a user with the Administrator, Product Lead or Translation Specialist role can update string in the production stage.", Status.UNAUTHORIZED);
					}
				}
				
				//creator must exist and not be changed
				String creatorStr = origStringObj.getString(Constants.JSON_FEATURE_FIELD_CREATOR);
				if (creator!=null && !creator.equals(creatorStr)) {
					return new ValidationResults(String.format(Strings.fieldCannotBeChangedDuringUpdate, Constants.JSON_FEATURE_FIELD_CREATOR), Status.BAD_REQUEST);
				}
				//modification date must appear
				if (!origStringObj.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) || origStringObj.get(Constants.JSON_FIELD_LAST_MODIFIED)==null) {
					return new ValidationResults("The lastModified field is missing. This field must be specified during string update.", Status.BAD_REQUEST);
				}

				//verify that given modification date is not older than current modification date
				long givenModoficationDate = origStringObj.getLong(Constants.JSON_FIELD_LAST_MODIFIED);  //verify that legal long
				Date givenDate = new Date(givenModoficationDate);
				if (givenDate.before(lastModified)) {
					return new ValidationResults("The string was changed by another user. Refresh your browser and try again.", Status.CONFLICT);			
				}								
				

				//key must exist and not be changed
				String keyStr = origStringObj.getString(Constants.JSON_FIELD_KEY);
				if (!key.equals(keyStr)) {
					return new ValidationResults("The String ID is not editable.", Status.BAD_REQUEST);
				}
			
				if (!value.equals(origStringObj.getString(Constants.JSON_FIELD_VALUE)) && (stageObj.equals(Stage.PRODUCTION) || stage.equals(Stage.PRODUCTION)) &&
						!validRole(userInfo)) {
					return new ValidationResults("Only a user with the Administrator, Product Lead or Translation Specialist role can change the value of strings in production.", Status.BAD_REQUEST);
				}					
								
				Environment env = new Environment();
				env.setServerVersion(season.getServerVersion()); 			
				
				if (TranslationServices.isTranslationStatusesSupport(env)) {
					//source modification date must appear
					if (!origStringObj.containsKey(Constants.JSON_FIELD_LAST_SOURCE_MODIFICATION) || origStringObj.get(Constants.JSON_FIELD_LAST_SOURCE_MODIFICATION)==null) {
						return new ValidationResults("The lastSourceModification field is missing. This field must be specified during string update.", Status.BAD_REQUEST);
					}
	
					//verify that given source modification date is not older than current source modification date
					long givenSourceModoficationDate = origStringObj.getLong(Constants.JSON_FIELD_LAST_SOURCE_MODIFICATION);  //verify that legal long
					Date givenSourceDate = new Date(givenSourceModoficationDate);
					if (givenSourceDate.before(lastSourceModification)) {
						return new ValidationResults("The string source was changed by another user. Refresh your browser and try again.", Status.CONFLICT);			
					}
					
					//translatorId => can exists and not be changed
					if (origStringObj.containsKey(Constants.JSON_FIELD_TRANSLATOR_ID) && origStringObj.get(Constants.JSON_FIELD_TRANSLATOR_ID)!=null) {
						String translatorIdStr = origStringObj.getString(Constants.JSON_FIELD_TRANSLATOR_ID);
						if (!translatorId.equals(translatorIdStr)) {
							return new ValidationResults(String.format(Strings.fieldCannotBeChangedDuringUpdate, Constants.JSON_FIELD_TRANSLATOR_ID), Status.BAD_REQUEST);
						}						
					}
				}
				//status => can exists and not be changed
				if (origStringObj.containsKey(Constants.JSON_FIELD_STATUS) && origStringObj.get(Constants.JSON_FIELD_STATUS)!=null) {
					String statusStr = origStringObj.getString(Constants.JSON_FIELD_STATUS);
					StringStatus statusObj = Utilities.strToStringStatus(statusStr);
					if (statusObj == null) {
						return new ValidationResults("Illegal status: '" + statusStr + "'", Status.BAD_REQUEST);
					}
					if (!status.equals(statusObj)) {
						return new ValidationResults(String.format(Strings.fieldCannotBeChangedDuringUpdate, Constants.JSON_FIELD_STATUS), Status.BAD_REQUEST);
					}						
				}

				if (stageObj == Stage.DEVELOPMENT && stage == Stage.PRODUCTION) {
					//if the stage changed from prod to dev- verify that configurations in prod is not using it - check that is not using the string.
					
					ValidationCache tester = new ValidationCache(keyStr, null);  // a cache without the deleted string
					//go over the season's configRules and validate that no configuration uses the updated string
					try {
						String verRes = season.getOriginalStrings().validateProdConfigNotUsingString(keyStr, stageObj, season.getRoot(), context, tester);
						if (verRes != null)
							return new ValidationResults(verRes, Status.BAD_REQUEST); 

						for (Branch branch:season.getBranches().getBranchesList()) {
							for (BaseAirlockItem subTreeRoot: branch.getBranchFeatures())
							verRes = season.getOriginalStrings().validateProdConfigNotUsingString(keyStr, stageObj, subTreeRoot, context, tester);
							if (verRes != null)
								return new ValidationResults(verRes, Status.BAD_REQUEST); 
	
						}
					} catch (JSONException e) {
						return new ValidationResults("Unable to verify string update: " + e.getMessage(), Status.BAD_REQUEST);
					} catch (GenerationException e) {
						return new ValidationResults("Unable to verify string update: " + e.getMessage(), Status.BAD_REQUEST);
					} 
					
					
					try {
						String verRes = season.getNotifications().validateProdNotificationNotUsingString(keyStr, stageObj, season, context, tester);
						if (verRes != null)
							return new ValidationResults(verRes, Status.BAD_REQUEST);
					} catch (JSONException e) {
						return new ValidationResults("Unable to verify string update: " + e.getMessage(), Status.BAD_REQUEST);
					} catch (GenerationException e) {
						return new ValidationResults("Unable to verify string update: " + e.getMessage(), Status.BAD_REQUEST);
					} 
				}
		
			}
			//modification date - verify long if exists
			if (origStringObj.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) && origStringObj.get(Constants.JSON_FIELD_LAST_MODIFIED)!=null) {
				origStringObj.getLong(Constants.JSON_FIELD_LAST_MODIFIED);						
			}
		} catch (JSONException jsne) {
			return new ValidationResults(jsne.getMessage(), Status.BAD_REQUEST);
		}
		catch (ClassCastException cce) {
			return new ValidationResults("Illegal OriginalString JSON: " + cce.getMessage(), Status.BAD_REQUEST);
		}
		return null;
	}
	static boolean validRole(UserInfo userInfo)
	{

		return userInfo == null || userInfo.getRoles().contains(Constants.RoleType.Administrator)
			|| userInfo.getRoles().contains(Constants.RoleType.ProductLead)
			|| userInfo.getRoles().contains(Constants.RoleType.TranslationSpecialist);

	}
	public String updateOriginalString(JSONObject updatedOrigStrJSON) throws JSONException {
		boolean wasChanged = false;
		StringBuilder updateDetails = new StringBuilder();

		//creator, seasonId and key should not be updated
		//value
		String updatedValue = updatedOrigStrJSON.getString(Constants.JSON_FIELD_VALUE);
		if (!updatedValue.equals(value)) {
			updateDetails.append("'value' changed from " + value + " to " + updatedValue + ";	");
			value = updatedValue;
			wasChanged = true;
			lastSourceModification = new Date(); //the source was updated
			status = StringStatus.NEW_STRING;	
			stringTranslations.setAllTranslationStatuses(TranslationStatus.NOT_TRANSLATED);
		}		

		//stage
		Stage updatedStage = Utilities.strToStage(updatedOrigStrJSON.getString(Constants.JSON_FEATURE_FIELD_STAGE));
		if (updatedStage != stage) {
			updateDetails.append("'stage' changed from " + stage + " to " + updatedStage + ";	");
			stage = updatedStage;
			wasChanged = true;
		}

		//if value was changed for string in development (in prod it is forbidden) - remove the translations
		if (!updatedValue.equals(value) && stage.equals(Stage.DEVELOPMENT) )
			stringTranslations.clear();
		
		//optional fields
		//owner
		if (updatedOrigStrJSON.containsKey(Constants.JSON_FEATURE_FIELD_OWNER) &&  updatedOrigStrJSON.get(Constants.JSON_FEATURE_FIELD_OWNER)!=null) {
			//if missing from json or null - ignore
			String updatedOwner= updatedOrigStrJSON.getString(Constants.JSON_FEATURE_FIELD_OWNER);
			if (owner == null || !owner.equals(updatedOwner)) {
				updateDetails.append("'owner' changed from " + owner + " to " + updatedOwner + ";	");
				owner  = updatedOwner;
				wasChanged = true;
			}
		}		
		
		//optional fields
		if (updatedOrigStrJSON.containsKey(Constants.JSON_FIELD_DESCRIPTION) &&  updatedOrigStrJSON.get(Constants.JSON_FIELD_DESCRIPTION)!=null) {
			//if missing from json or null - ignore
			String updatedDescription = updatedOrigStrJSON.getString(Constants.JSON_FIELD_DESCRIPTION);
			if (description == null || !description.equals(updatedDescription)) {
				updateDetails.append(" 'description' changed from '" + description + "' to '" + updatedDescription + "'\n");
				description  = updatedDescription;
				wasChanged = true;
			}
		}	
		
		//internationalFallback
		if (updatedOrigStrJSON.containsKey(Constants.JSON_FIELD_INTERNATIONAL_FALLBACK) &&  updatedOrigStrJSON.get(Constants.JSON_FIELD_INTERNATIONAL_FALLBACK)!=null) {
			String updatedInterFB= updatedOrigStrJSON.getString(Constants.JSON_FIELD_INTERNATIONAL_FALLBACK);
			if (internationalFallback == null || !internationalFallback.equals(updatedInterFB)) {
				updateDetails.append("'internationalFallback' changed from " + internationalFallback + " to " + updatedInterFB + ";	");
				internationalFallback  = updatedInterFB;
				wasChanged = true;
			}
		}
		else {
			//if does not exist in json - ignore. If exist and equals null - delete
			if (updatedOrigStrJSON.containsKey(Constants.JSON_FIELD_INTERNATIONAL_FALLBACK) && updatedOrigStrJSON.get(Constants.JSON_FIELD_INTERNATIONAL_FALLBACK)==null) {
				if (internationalFallback!=null) {
					updateDetails.append("'internationalFallback' changed from " + internationalFallback + " to null;	");
					internationalFallback  = null;
					wasChanged = true;
				}
			}
		}
		
		//translationInstruction
		if (updatedOrigStrJSON.containsKey(Constants.JSON_FIELD_TRANSLATION_INSTRUCTION) &&  updatedOrigStrJSON.get(Constants.JSON_FIELD_TRANSLATION_INSTRUCTION)!=null) {			
			String updatedTranslationInstruction= updatedOrigStrJSON.getString(Constants.JSON_FIELD_TRANSLATION_INSTRUCTION);
			if (translationInstruction == null || !translationInstruction.equals(updatedTranslationInstruction)) {
				updateDetails.append("'translationInstruction' changed from " + translationInstruction + " to " + updatedTranslationInstruction + ";	");
				translationInstruction  = updatedTranslationInstruction;
				wasChanged = true;
			}
		}
		else {
			//if does not exist in json - ignore. If exist and equals null - delete
			if (updatedOrigStrJSON.containsKey(Constants.JSON_FIELD_TRANSLATION_INSTRUCTION) && updatedOrigStrJSON.get(Constants.JSON_FIELD_TRANSLATION_INSTRUCTION)==null) {
				if (translationInstruction!=null) {
					updateDetails.append("'translationInstruction' changed from " + translationInstruction + " to null;	");
					translationInstruction  = null;
					wasChanged = true;
				}
			}
		}
		
		//maxStringSize
		if (updatedOrigStrJSON.containsKey(Constants.JSON_FIELD_MAX_STRING_SIZE) &&  updatedOrigStrJSON.get(Constants.JSON_FIELD_MAX_STRING_SIZE)!=null) {			
			Integer updatedMaxStringSize = updatedOrigStrJSON.getInt(Constants.JSON_FIELD_MAX_STRING_SIZE);
			if (maxStringSize == null || !maxStringSize.equals(updatedMaxStringSize)) {
				updateDetails.append("'maxStringSize' changed from " + maxStringSize + " to " + updatedMaxStringSize + ";	");
				maxStringSize  = updatedMaxStringSize;
				wasChanged = true;
			}
		}
		else {
			//if does not exist in json - ignore. If exist and equals null - delete
			if (updatedOrigStrJSON.containsKey(Constants.JSON_FIELD_MAX_STRING_SIZE) && updatedOrigStrJSON.get(Constants.JSON_FIELD_MAX_STRING_SIZE)==null) {
				if (maxStringSize!=null) {
					updateDetails.append("'maxStringSize' changed from " + maxStringSize + " to null;	");
					maxStringSize  = null;
					wasChanged = true;

				}
			}
		}
		

		// disabled for now: there must always be a variant. If missing, generate a unique one
		//String updatedVariant = updatedOrigStrJSON.optString(Constants.JSON_FIELD_STRINGS_VARIANT, Constants.NEW_VARIANT);
		//if (updatedVariant.isEmpty() || updatedVariant.equals(Constants.NEW_VARIANT))
		//	updatedVariant = generateVariant(key);
		String updatedVariant = updatedOrigStrJSON.optString(Constants.JSON_FIELD_STRINGS_VARIANT);
		if (updatedVariant == null)
			updatedVariant = updatedOrigStrJSON.optString(Constants.JSON_FIELD_STRINGS_OLD_VARIANT);

		if (variant == null || !variant.equals(updatedVariant))
		{
			updateDetails.append("'variant' changed from " + variant + " to " + updatedVariant + ";	"); // original variant may be null
			variant = updatedVariant;
			wasChanged = true;
// disabled for now (not sent to automatic translation)
//			status = StringStatus.NEW_STRING; // maybe use another status
//			stringTranslations.setAllTranslationStatuses(TranslationStatus.NOT_TRANSLATED);
		}

		if (wasChanged) {
			lastModified = new Date();
		}

		if (updateDetails.length()!=0) {
			updateDetails.insert(0,"Original string changes: ");
		}
		return updateDetails.toString();
	}

	public void addStringTranslationForLocale(String locale, String value, LinkedList<String> supportedLanguages) {
		LocaleTranslationData existingTransData = stringTranslations.getTranslationDataPerLocale(locale);
		
		if (existingTransData != null && existingTransData.getTranslationStatus() == TranslationStatus.OVERRIDE)
			return; //dont update/add translation to a string that has an override translation value 
		
		//String issue = null;
		//String issueId = null;
		IssueStatus issueStatus = null;
		String newTranslationAvailable = null;
		if (existingTransData!=null) {
			//issue = existingTransData.getIssue();
			issueStatus = existingTransData.getIssueStatus();
			newTranslationAvailable = existingTransData.getNewTranslationAvailable();
		}
			
		stringTranslations.addLocaleTranslation(locale, stringTranslations.new LocaleTranslationData(value, null, TranslationStatus.TRANSLATED, issueStatus, newTranslationAvailable));
		
		moveStringStatusToTransCompletedIfNeeded(supportedLanguages);
		/*boolean allTranslated = true;
		for (String lang:supportedLanguages) {
			if (lang.equals(Constants.DEFAULT_LANGUAGE))
				continue;
			
			LocaleTranslationData transData = stringTranslations.getTranslationDataPerLocale(lang);
			if (transData == null || transData.getTranslationStatus() == null || 
					(!transData.getTranslationStatus().equals(TranslationStatus.OVERRIDE) && !transData.getTranslationStatus().equals(TranslationStatus.TRANSLATED))) {
				allTranslated = false;
				break;
			}
		}
		
		if (allTranslated) {
			this.status = StringStatus.TRANSLATION_COMPLETE;
		}*/
	}
	
	public void moveStringStatusToTransCompletedIfNeeded(LinkedList<String> supportedLanguages) {
		boolean allTranslated = true;
		for (String lang:supportedLanguages) {
			if (lang.equals(Constants.DEFAULT_LANGUAGE))
				continue;
			
			LocaleTranslationData transData = stringTranslations.getTranslationDataPerLocale(lang);
			if (transData == null || transData.getTranslationStatus() == null || 
					(!transData.getTranslationStatus().equals(TranslationStatus.OVERRIDE) && !transData.getTranslationStatus().equals(TranslationStatus.TRANSLATED))) {
				allTranslated = false;
				break;
			}
		}
		
		if (allTranslated) {
			this.status = StringStatus.TRANSLATION_COMPLETE;
		}
	}
	
	public OriginalString duplicateForNewSeason (String minVersion, UUID newSeasonId, String origServerVersion, LinkedList<String> supportedLanguages) {
		OriginalString res = new OriginalString();	
		res.setUniqueId(UUID.randomUUID());
		res.setSeasonId(newSeasonId);
		res.setLastModified(lastModified);
		res.setLastSourceModification(lastSourceModification);
		res.setStage(stage);
		res.setKey(key);
		res.setValue(value);
		res.setInternationalFallback(internationalFallback);
		res.setStringTranslations(stringTranslations.duplicate());
		res.setTranslatorId(translatorId);
		res.setStatus(status);
		res.setTranslationInstruction(translationInstruction);
		res.setVariant(variant);
		res.setSmartlingCreationProcess(smartlingCreationProcess);
		res.setMaxStringSize(maxStringSize);
		res.setCreator(creator);
		res.setOwner(owner);
		res.setDescription(description);
		
		Environment origSeasonEnv = new Environment();
		origSeasonEnv.setServerVersion(origServerVersion); 			
		
		if (!TranslationServices.isTranslationStatusesSupport(origSeasonEnv)) { //if pre 2.5 season - should update the statuses in the new season			
			HashMap<String, LocaleTranslationData> stringTranslationsMap =  res.getStringTranslations().getStringTranslationsMap();
			Set<String> locales = stringTranslationsMap.keySet();
			for (String locale:locales) {
				stringTranslationsMap.get(locale).setTranslationStatus(TranslationStatus.TRANSLATED);
			}
			
			if (locales.size() == 0) {
				res.setStatus(StringStatus.NEW_STRING);
			}
			else if (locales.size() == supportedLanguages.size()) {
				res.setStatus(StringStatus.TRANSLATION_COMPLETE);
			}
			else {
				res.setStatus(StringStatus.IN_TRANSLATION);
			}
		}
			
		
		
		return res;
	}

	public void overrideTranslation(String locale, String overrideValue, LinkedList<String> supportedLanguages) {
		LocaleTranslationData transData = getStringTranslations().getTranslationDataPerLocale(locale);
		if (transData == null)
		{
			transData = getStringTranslations().new LocaleTranslationData();
			transData.setOverrideValue(overrideValue);
			getStringTranslations().addLocaleTranslation(locale, transData);
		}
		else // save any existing translation in the alternate field
		{
			transData.setNewTranslationAvailable(transData.getTranslatedValue());
			transData.setOverrideValue(overrideValue);
		}

		moveStringStatusToTransCompletedIfNeeded(supportedLanguages);
		
	}
	
	public void cancelOverride(String locale) {
		LocaleTranslationData transData = getStringTranslations().getTranslationDataPerLocale(locale);
		if (transData == null)
			return;

		String newTran = transData.getNewTranslationAvailable();
		if (newTran != null)
		{
			transData.setTranslatedValue(newTran);
			transData.setNewTranslationAvailable(null);
			transData.setTranslationStatus(TranslationStatus.TRANSLATED);
		}
		else
		{
			getStringTranslations().removeLocaleTranslation(locale);
			if (status == StringStatus.TRANSLATION_COMPLETE)
				status = StringStatus.IN_TRANSLATION;
		}
	}

	public static String generateVariant(String key)
	{
		String uuid = UUID.randomUUID().toString();
		if (key == null)
			return uuid;
		else
			return key + "_" + uuid;
	}
}
