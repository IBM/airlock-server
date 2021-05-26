package com.ibm.airlock.admin.streams;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;
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
import com.ibm.airlock.Constants.OutputJSONMode;
import com.ibm.airlock.Constants.Stage;
import com.ibm.airlock.admin.GenerationException;
import com.ibm.airlock.admin.InputSchema;
import com.ibm.airlock.admin.InternalUserGroups;
import com.ibm.airlock.admin.Product;
import com.ibm.airlock.admin.Season;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.ValidationCache;
import com.ibm.airlock.admin.ValidationResults;
import com.ibm.airlock.admin.authentication.UserInfo;
import com.ibm.airlock.engine.ScriptInvoker.InvokerException;
import com.ibm.airlock.engine.StreamsScriptInvoker;
import com.ibm.airlock.engine.VerifyStream;


public class AirlockStream {
	private Stage stage = null; //c+u
	private UUID uniqueId = null; //nc + u
	private String name = null; //c+u
	private String description = null; //opt in c+u (if missing or null in update don't change)
	private String displayName = null; //opt in c+u (if missing or null in update don't change)
	private Boolean enabled = null; //required in create and update
	private Date creationDate = null; //nc + u (not changed)
	private String creator = null;	//c+u (creator not changed)
	private String[] internalUserGroups = null; //opt in creation + in update if missing or null ignore , if empty array then emptying
	private Double rolloutPercentage = null; //c+u
	private Date lastModified = null; // required in update. forbidden in create
	private String minAppVersion = null; //c+u
	private String filter = null; //c+u cannot be empty string
	private String processor = null; //c+u cannot be empty string
	private UUID seasonId = null; //c+u
	private Integer cacheSizeKB = null; //optional, null resets its value, null not written in runtime
	private Integer queueSizeKB = null; //optional, null resets its value, null not written in runtime
	private Integer maxQueuedEvents = null; //optional, null resets its value, null not written in runtime
	private String owner = null; //like desc (not in runtime)
	private String resultsSchema = null; //c+u cannot be empty string (not in runtime)
	
	//history fields
	private Boolean operateOnHistoricalEvents = false; //c+u
	private Long limitByStartDate = null; //c+u null means unlimited
	private Long limitByEndDate = null; //c+u null means unlimited
	private Integer processEventsOfLastNumberOfDays = null; //c+u null is possible when operateOnHistoricalEvents=false or start&end are configured
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public String getDisplayName() {
		return displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	public void setDescription(String description) {
		this.description = description;
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
	public Boolean getEnabled() {
		return enabled;
	}	
	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
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
	public String[] getInternalUserGroups() {
		return internalUserGroups;
	}
	public void setInternalUserGroups(String[] internalUserGroups) {
		this.internalUserGroups = internalUserGroups;
	}
	public Double getRolloutPercentage() {
		return rolloutPercentage;
	}	
	public void setRolloutPercentage(Double rolloutPercentage) {
		this.rolloutPercentage = rolloutPercentage;
	}
	
	public Date getLastModified() {
		return lastModified;
	}
	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}		
	public String getMinAppVersion() {
		return minAppVersion;
	}
	public void setMinAppVersion(String minAppVersion) {
		this.minAppVersion = minAppVersion;
	}		
	public String getFilter() {
		return filter;
	}
	public void setFilter(String filter) {
		this.filter = filter;
	}
	public String getProcessor() {
		return processor;
	}
	public void setProcessor(String processor) {
		this.processor = processor;
	}
	public UUID getSeasonId() {
		return seasonId;
	}
	public void setSeasonId(UUID seasonId) {
		this.seasonId = seasonId;
	}
	public Integer getCacheSizeKB() {
		return cacheSizeKB;
	}
	public void setCacheSizeKB(Integer maxCacheSize) {
		this.cacheSizeKB = maxCacheSize;
	}
	public Integer getMaxQueuedEvents() {
		return maxQueuedEvents;
	}
	public void setMaxQueuedEvents(Integer maxQueuedEvents) {
		this.maxQueuedEvents = maxQueuedEvents;
	}
	public String getOwner() {
		return owner;
	}
	public void setOwner(String owner) {
		this.owner = owner;
	}
	public String getResultsSchema() {
		return resultsSchema;
	}
	public void setResultsSchema(String resultsSchema) {
		this.resultsSchema = resultsSchema;
	}
	public Integer getQueueSizeKB() {
		return queueSizeKB;
	}
	public void setQueueSizeKB(Integer queueSizeKB) {
		this.queueSizeKB = queueSizeKB;
	}	
	
	public Boolean getOperateOnHistoricalEvents() {
		return operateOnHistoricalEvents;
	}
	public void setOperateOnHistoricalEvents(Boolean operateOnHistoricalEvents) {
		this.operateOnHistoricalEvents = operateOnHistoricalEvents;
	}
	public Long getLimitByStartDate() {
		return limitByStartDate;
	}
	public void setLimitByStartDate(Long limitByStartDate) {
		this.limitByStartDate = limitByStartDate;
	}
	public Long getLimitByEndDate() {
		return limitByEndDate;
	}
	public void setLimitByEndDate(Long limitByEndDate) {
		this.limitByEndDate = limitByEndDate;
	}
	public Integer getProcessEventsOfLastNumberOfDays() {
		return processEventsOfLastNumberOfDays;
	}
	public void setProcessEventsOfLastNumberOfDayss(Integer processEventsOfLastNumberOfDays) {
		this.processEventsOfLastNumberOfDays = processEventsOfLastNumberOfDays;
	}
	public void fromJSON(JSONObject input) throws JSONException {
		if (input.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && input.get(Constants.JSON_FIELD_UNIQUE_ID) != null) {
			String sStr = input.getString(Constants.JSON_FIELD_UNIQUE_ID);			
			uniqueId = UUID.fromString(sStr);		
		}
		
		if (input.containsKey(Constants.JSON_FEATURE_FIELD_STAGE) && input.get(Constants.JSON_FEATURE_FIELD_STAGE)!=null)
			stage = Utilities.strToStage(input.getString(Constants.JSON_FEATURE_FIELD_STAGE));
		
		if (input.containsKey(Constants.JSON_FIELD_NAME) && input.get(Constants.JSON_FIELD_NAME)!=null) 
			name = input.getString(Constants.JSON_FIELD_NAME);			

		if (input.containsKey(Constants.JSON_FEATURE_FIELD_ENABLED) && input.get(Constants.JSON_FEATURE_FIELD_ENABLED)!=null)
			enabled = input.getBoolean(Constants.JSON_FEATURE_FIELD_ENABLED);
		
		if (input.containsKey(Constants.JSON_FIELD_DESCRIPTION) && input.get(Constants.JSON_FIELD_DESCRIPTION)!=null) 
			description = input.getString(Constants.JSON_FIELD_DESCRIPTION).trim();
		
		if (input.containsKey(Constants.JSON_FIELD_DISPLAY_NAME) && input.get(Constants.JSON_FIELD_DISPLAY_NAME)!=null) 
			displayName = input.getString(Constants.JSON_FIELD_DISPLAY_NAME).trim();
		
		if (input.containsKey(Constants.JSON_FEATURE_FIELD_OWNER) && input.get(Constants.JSON_FEATURE_FIELD_OWNER)!=null) 
			owner = input.getString(Constants.JSON_FEATURE_FIELD_OWNER).trim();
		
		if (input.containsKey(Constants.JSON_FEATURE_FIELD_CREATOR) && input.get(Constants.JSON_FEATURE_FIELD_CREATOR)!=null)
			creator = input.getString(Constants.JSON_FEATURE_FIELD_CREATOR).trim();
		
		if (input.containsKey(Constants.JSON_FEATURE_FIELD_MIN_APP_VER) && input.get(Constants.JSON_FEATURE_FIELD_MIN_APP_VER)!=null) 
			minAppVersion = input.getString(Constants.JSON_FEATURE_FIELD_MIN_APP_VER);						

		if (input.containsKey(Constants.JSON_FEATURE_FIELD_CREATION_DATE) && input.get(Constants.JSON_FEATURE_FIELD_CREATION_DATE)!=null) { 
			long timeInMS = input.getLong(Constants.JSON_FEATURE_FIELD_CREATION_DATE);
			creationDate = new Date(timeInMS);			
		} else {
			creationDate = new Date();
		}

		if (input.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) && input.get(Constants.JSON_FIELD_LAST_MODIFIED)!=null) { 
			long timeInMS = input.getLong(Constants.JSON_FIELD_LAST_MODIFIED);
			lastModified = new Date(timeInMS);
		}  else {
			lastModified = new Date();
		}

		if (input.containsKey(Constants.JSON_FIELD_INTERNAL_USER_GROUPS) && input.get(Constants.JSON_FIELD_INTERNAL_USER_GROUPS)!=null) 
			internalUserGroups = Utilities.jsonArrToStringArr(input.getJSONArray(Constants.JSON_FIELD_INTERNAL_USER_GROUPS));
		
		if (input.containsKey(Constants.JSON_FEATURE_FIELD_PERCENTAGE) && input.get(Constants.JSON_FEATURE_FIELD_PERCENTAGE)!=null) 
			rolloutPercentage = input.getDouble(Constants.JSON_FEATURE_FIELD_PERCENTAGE);
						
		if (input.containsKey(Constants.JSON_FIELD_FILTER) && input.get(Constants.JSON_FIELD_FILTER)!=null) 
			filter = input.getString(Constants.JSON_FIELD_FILTER);
		
		if (input.containsKey(Constants.JSON_FIELD_PROCESSOR) && input.get(Constants.JSON_FIELD_PROCESSOR)!=null) 
			processor = input.getString(Constants.JSON_FIELD_PROCESSOR);
		
		if (input.containsKey(Constants.JSON_FIELD_RESULTS_SCHEMA) && input.get(Constants.JSON_FIELD_RESULTS_SCHEMA)!=null) 
			resultsSchema = input.getString(Constants.JSON_FIELD_RESULTS_SCHEMA);		
		
		if (input.containsKey(Constants.JSON_FIELD_CACHE_SIZE_KB) && input.get(Constants.JSON_FIELD_CACHE_SIZE_KB)!=null) 
			cacheSizeKB = input.getInt(Constants.JSON_FIELD_CACHE_SIZE_KB);
		
		if (input.containsKey(Constants.JSON_FIELD_MAX_QUEUED_EVENTS) && input.get(Constants.JSON_FIELD_MAX_QUEUED_EVENTS)!=null) 
			maxQueuedEvents = input.getInt(Constants.JSON_FIELD_MAX_QUEUED_EVENTS);
		
		if (input.containsKey(Constants.JSON_FIELD_QUEUE_SIZE_KB) && input.get(Constants.JSON_FIELD_QUEUE_SIZE_KB)!=null) 
			queueSizeKB = input.getInt(Constants.JSON_FIELD_QUEUE_SIZE_KB);
		
		if (input.containsKey(Constants.JSON_FEATURE_FIELD_SEASON_ID) && input.get(Constants.JSON_FEATURE_FIELD_SEASON_ID) != null) {
			String sStr = input.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID);			
			seasonId = UUID.fromString(sStr);			
		}
		
		if (input.containsKey(Constants.JSON_FIELD_OPERATE_ON_HISTORICAL_EVENTS) && input.get(Constants.JSON_FIELD_OPERATE_ON_HISTORICAL_EVENTS)!=null) 
			operateOnHistoricalEvents = input.getBoolean(Constants.JSON_FIELD_OPERATE_ON_HISTORICAL_EVENTS);
		
		//null is legal value
		if (input.containsKey(Constants.JSON_FIELD_LIMIT_BY_START_DATE) && input.get(Constants.JSON_FIELD_LIMIT_BY_START_DATE)!=null) 
			limitByStartDate = input.getLong(Constants.JSON_FIELD_LIMIT_BY_START_DATE);
		
		if (input.containsKey(Constants.JSON_FIELD_LIMIT_BY_END_DATE) && input.get(Constants.JSON_FIELD_LIMIT_BY_END_DATE)!=null) 
			limitByEndDate = input.getLong(Constants.JSON_FIELD_LIMIT_BY_END_DATE);
		
		if (input.containsKey(Constants.JSON_FIELD_PROCESS_EVENTS_OF_LAST_NUMBER_OF_DAYS) && input.get(Constants.JSON_FIELD_PROCESS_EVENTS_OF_LAST_NUMBER_OF_DAYS)!=null) 
			processEventsOfLastNumberOfDays = input.getInt(Constants.JSON_FIELD_PROCESS_EVENTS_OF_LAST_NUMBER_OF_DAYS);		
	}
	
	public JSONObject toJson (OutputJSONMode mode) throws JSONException {
		JSONObject res = new JSONObject();
		
		res.put(Constants.JSON_FIELD_UNIQUE_ID, uniqueId==null?null:uniqueId.toString());		
		res.put(Constants.JSON_FIELD_NAME, name);
		res.put(Constants.JSON_FIELD_FILTER, filter);
		res.put(Constants.JSON_FIELD_PROCESSOR, processor);		
		
		res.put(Constants.JSON_FEATURE_FIELD_ENABLED, enabled);
		res.put(Constants.JSON_FIELD_INTERNAL_USER_GROUPS, internalUserGroups);		
		res.put(Constants.JSON_FEATURE_FIELD_PERCENTAGE, rolloutPercentage);
		res.put(Constants.JSON_FEATURE_FIELD_MIN_APP_VER, minAppVersion);		
		res.put(Constants.JSON_FEATURE_FIELD_STAGE, stage.toString());
		res.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, seasonId==null?null:seasonId.toString());
		
		res.put(Constants.JSON_FIELD_OPERATE_ON_HISTORICAL_EVENTS, operateOnHistoricalEvents);
		res.put(Constants.JSON_FIELD_LIMIT_BY_START_DATE, limitByStartDate);
		res.put(Constants.JSON_FIELD_LIMIT_BY_END_DATE, limitByEndDate);
		res.put(Constants.JSON_FIELD_PROCESS_EVENTS_OF_LAST_NUMBER_OF_DAYS, processEventsOfLastNumberOfDays);
		
		
		if (mode.equals(OutputJSONMode.ADMIN) || mode.equals(OutputJSONMode.DISPLAY)) {
			res.put(Constants.JSON_FIELD_DESCRIPTION, description);
			res.put(Constants.JSON_FIELD_DISPLAY_NAME, displayName);
			res.put(Constants.JSON_FEATURE_FIELD_OWNER, owner);
			res.put(Constants.JSON_FEATURE_FIELD_CREATION_DATE, creationDate.getTime()); 			
			res.put(Constants.JSON_FEATURE_FIELD_CREATOR, creator);
			res.put(Constants.JSON_FIELD_LAST_MODIFIED, lastModified.getTime());			
			res.put(Constants.JSON_FIELD_RESULTS_SCHEMA, resultsSchema);
			res.put(Constants.JSON_FIELD_CACHE_SIZE_KB, cacheSizeKB);
			res.put(Constants.JSON_FIELD_QUEUE_SIZE_KB, queueSizeKB);
			res.put(Constants.JSON_FIELD_MAX_QUEUED_EVENTS, maxQueuedEvents);
		}
		else {
			//in runtime files don't write these field if they are null
			if (cacheSizeKB!=null)
				res.put(Constants.JSON_FIELD_CACHE_SIZE_KB, cacheSizeKB);			
			
			if (queueSizeKB!=null)
				res.put(Constants.JSON_FIELD_QUEUE_SIZE_KB, queueSizeKB);
			
			if (maxQueuedEvents!=null)
				res.put(Constants.JSON_FIELD_MAX_QUEUED_EVENTS, maxQueuedEvents);			
			
		}
		
		return res;
	}
	
	public String updateStraem (JSONObject updatedStreamJSON, Season season) throws JSONException {
		//creator, creationDate should not be updated
		
		boolean wasChanged = false;
		StringBuilder updateDetails = new StringBuilder();
		boolean updateMergeSchema = false;
		
		//stage		
		Stage updatedStage = Utilities.strToStage(updatedStreamJSON.getString(Constants.JSON_FEATURE_FIELD_STAGE));
		if (updatedStage != stage) {
			updateDetails.append(" 'stage' changed from " + stage + " to " + updatedStage + "\n");
			stage = updatedStage;
			updateMergeSchema = true;
			wasChanged = true;
		}
		
		//name
		String updatedName = updatedStreamJSON.getString(Constants.JSON_FIELD_NAME);
		if (!updatedName.equals(name)) {
			updateDetails.append(" 'name' changed from " + name + " to " + updatedName + "\n");
			name = updatedName;
			updateMergeSchema = true;
			wasChanged = true;
		}		

		//enabled
		Boolean updatedEnabled = updatedStreamJSON.getBoolean(Constants.JSON_FEATURE_FIELD_ENABLED);
		if (enabled != updatedEnabled) {
			updateDetails.append("'enabled' changed from " + enabled + " to " + updatedEnabled + "\n");
			enabled  = updatedEnabled;
			wasChanged = true;			
		}	

		//minAppVersion
		String updatedMinAppVersion = updatedStreamJSON.getString(Constants.JSON_FEATURE_FIELD_MIN_APP_VER);
		if (minAppVersion==null || !minAppVersion.equals(updatedMinAppVersion)) {
			updateDetails.append("'minAppVersion' changed from " + minAppVersion + " to " + updatedMinAppVersion + "\n");
			minAppVersion  = updatedMinAppVersion;
			updateMergeSchema = true;
			wasChanged = true;
		}
	
		//rolloutPercentage
		double updatedRolloutPercentage = updatedStreamJSON.getDouble(Constants.JSON_FEATURE_FIELD_PERCENTAGE);
		if (rolloutPercentage  != updatedRolloutPercentage) {
			updateDetails.append(" 'rolloutPercentage' changed from " + rolloutPercentage + " to " + updatedRolloutPercentage + "\n");
			rolloutPercentage = updatedRolloutPercentage;		
			wasChanged = true;
		}
				
		//filter
		String updatedFilter = updatedStreamJSON.getString(Constants.JSON_FIELD_FILTER);
		if (!updatedFilter.equals(filter)) {
			updateDetails.append(" 'filter' changed from " + filter + " to " + updatedFilter + "\n");
			filter = updatedFilter;
			wasChanged = true;
		}
		
		//processor
		String updatedProcessor = updatedStreamJSON.getString(Constants.JSON_FIELD_PROCESSOR);
		if (!updatedProcessor.equals(processor)) {
			updateDetails.append(" 'processor' changed from " + processor + " to " + updatedProcessor + "\n");
			processor = updatedProcessor;
			wasChanged = true;
		}
		
		//resultsSchema
		String updatedResultsSchema = updatedStreamJSON.getString(Constants.JSON_FIELD_RESULTS_SCHEMA);
		if (!updatedResultsSchema.equals(resultsSchema)) {
			updateDetails.append(" 'resultsSchema' changed from " + resultsSchema + " to " + updatedResultsSchema + "\n");
			resultsSchema = updatedResultsSchema;
			updateMergeSchema = true;
			wasChanged = true;
		}
		
		//optional fields
		if (updatedStreamJSON.containsKey(Constants.JSON_FIELD_DESCRIPTION) &&  updatedStreamJSON.get(Constants.JSON_FIELD_DESCRIPTION)!=null) {
			//if missing from json or null - ignore
			String updatedDescription = updatedStreamJSON.getString(Constants.JSON_FIELD_DESCRIPTION);
			if (description == null || !description.equals(updatedDescription)) {
				updateDetails.append(" 'description' changed from '" + description + "' to '" + updatedDescription + "'\n");
				description  = updatedDescription;
				wasChanged = true;
			}
		}	
		
		if (updatedStreamJSON.containsKey(Constants.JSON_FIELD_DISPLAY_NAME) &&  updatedStreamJSON.get(Constants.JSON_FIELD_DISPLAY_NAME)!=null) {
			//if missing from json or null - ignore
			String updatedDisplayName = updatedStreamJSON.getString(Constants.JSON_FIELD_DISPLAY_NAME);
			if (displayName == null || !displayName.equals(updatedDisplayName)) {
				updateDetails.append(" 'displayName' changed from '" + displayName + "' to '" + updatedDisplayName + "'\n");
				displayName  = updatedDisplayName;
				wasChanged = true;
			}
		}	
		
		if (updatedStreamJSON.containsKey(Constants.JSON_FEATURE_FIELD_OWNER) &&  updatedStreamJSON.get(Constants.JSON_FEATURE_FIELD_OWNER)!=null) {
			//if missing from json or null - ignore
			String updatedOwner = updatedStreamJSON.getString(Constants.JSON_FEATURE_FIELD_OWNER);
			if (owner == null || !owner.equals(updatedOwner)) {
				updateDetails.append(" 'owner' changed from '" + owner + "' to '" + updatedOwner + "'\n");
				owner  = updatedOwner;
				wasChanged = true;
			}
		}	
		
		if (updatedStreamJSON.containsKey(Constants.JSON_FIELD_INTERNAL_USER_GROUPS) && updatedStreamJSON.get(Constants.JSON_FIELD_INTERNAL_USER_GROUPS)!=null) {
			JSONArray updatedInternalUserGroups = updatedStreamJSON.getJSONArray(Constants.JSON_FIELD_INTERNAL_USER_GROUPS);
			if (internalUserGroups == null || !Utilities.stringArrayCompareIgnoreOrder(updatedInternalUserGroups,internalUserGroups)) {
				updateDetails.append("'internalUserGroups' changed from " + Arrays.toString(internalUserGroups) + " to " +  Arrays.toString(Utilities.jsonArrToStringArr(updatedInternalUserGroups)) + "\n");
				internalUserGroups = Utilities.jsonArrToStringArr(updatedInternalUserGroups);
				wasChanged = true;
			}
		}
		
		//cacheSizeKB
		if (updatedStreamJSON.containsKey(Constants.JSON_FIELD_CACHE_SIZE_KB)) {
			if (updatedStreamJSON.get(Constants.JSON_FIELD_CACHE_SIZE_KB) == null) {
				if (cacheSizeKB!=null) {
					updateDetails.append(" 'maxCacheSize' changed from " + cacheSizeKB + " to null\n");
					cacheSizeKB = null;
					wasChanged = true;
				}				
			}
			else {
				Integer updatedMaxCacheSize = updatedStreamJSON.getInt(Constants.JSON_FIELD_CACHE_SIZE_KB);
				if (!updatedMaxCacheSize.equals(cacheSizeKB)) {
					updateDetails.append(" 'maxCacheSize' changed from " + cacheSizeKB + " to " + updatedMaxCacheSize + "\n");
					cacheSizeKB = updatedMaxCacheSize;
					wasChanged = true;
				}
			}
		}
		
		//queueSizeKB
		if (updatedStreamJSON.containsKey(Constants.JSON_FIELD_QUEUE_SIZE_KB)) {
			if (updatedStreamJSON.get(Constants.JSON_FIELD_QUEUE_SIZE_KB) == null) {
				if (queueSizeKB!=null) {
					updateDetails.append(" 'queueSizeKB' changed from " + queueSizeKB + " to null\n");
					queueSizeKB = null;
					wasChanged = true;
				}				
			}
			else {
				Integer updatedQueueSizeKB = updatedStreamJSON.getInt(Constants.JSON_FIELD_QUEUE_SIZE_KB);
				if (!updatedQueueSizeKB.equals(queueSizeKB)) {
					updateDetails.append(" 'queueSizeKB' changed from " + queueSizeKB + " to " + updatedQueueSizeKB + "\n");
					queueSizeKB = updatedQueueSizeKB;
					wasChanged = true;
				}
			}
		}

		//maxQueuedEvents
		if (updatedStreamJSON.containsKey(Constants.JSON_FIELD_MAX_QUEUED_EVENTS)) {
			if (updatedStreamJSON.get(Constants.JSON_FIELD_MAX_QUEUED_EVENTS) == null) {
				if (maxQueuedEvents!=null) {
					updateDetails.append(" 'maxQueuedEvents' changed from " + maxQueuedEvents + " to null\n");
					maxQueuedEvents = null;
					wasChanged = true;
				}				
			}
			else {
				Integer updatedMaxQueuedEvents = updatedStreamJSON.getInt(Constants.JSON_FIELD_MAX_QUEUED_EVENTS);
				if (!updatedMaxQueuedEvents.equals(maxQueuedEvents)) {
					updateDetails.append(" 'maxQueuedEvents' changed from " + maxQueuedEvents + " to " + updatedMaxQueuedEvents + "\n");
					maxQueuedEvents = updatedMaxQueuedEvents;
					wasChanged = true;
				}
			}
		}

		//for now history properties are optional until implemented in the console
		if (updatedStreamJSON.containsKey(Constants.JSON_FIELD_OPERATE_ON_HISTORICAL_EVENTS)) {
			//operateOnHistoricalEvents
			boolean updatedPperateOnHistoricalEvents = updatedStreamJSON.getBoolean(Constants.JSON_FIELD_OPERATE_ON_HISTORICAL_EVENTS);
			if (operateOnHistoricalEvents  != updatedPperateOnHistoricalEvents) {
				updateDetails.append(" 'operateOnHistoricalEvents' changed from " + operateOnHistoricalEvents + " to " + updatedPperateOnHistoricalEvents + "\n");
				operateOnHistoricalEvents = updatedPperateOnHistoricalEvents;		
				wasChanged = true;
			}
			
			
			//limitByStartDate - null is a legal value
			if (updatedStreamJSON.containsKey(Constants.JSON_FIELD_LIMIT_BY_START_DATE)) {
				if (updatedStreamJSON.get(Constants.JSON_FIELD_LIMIT_BY_START_DATE) == null) {
					if (limitByStartDate!=null) {
						updateDetails.append(" 'limitByStartDate' changed from " + limitByStartDate + " to null\n");
						limitByStartDate = null;
						wasChanged = true;
					}				
				}
				else {
					Long updatedLimitByStartDate = updatedStreamJSON.getLong(Constants.JSON_FIELD_LIMIT_BY_START_DATE);
					if (!updatedLimitByStartDate.equals(limitByStartDate)) {
						updateDetails.append(" 'limitByStartDate' changed from " + limitByStartDate + " to " + updatedLimitByStartDate + "\n");
						limitByStartDate = updatedLimitByStartDate;
						wasChanged = true;
					}
				}
			}
			
			//limitByEndDate - null is a legal value
			if (updatedStreamJSON.containsKey(Constants.JSON_FIELD_LIMIT_BY_END_DATE)) {
				if (updatedStreamJSON.get(Constants.JSON_FIELD_LIMIT_BY_END_DATE) == null) {
					if (limitByEndDate!=null) {
						updateDetails.append(" 'limitByEndDate' changed from " + limitByEndDate + " to null\n");
						limitByEndDate = null;
						wasChanged = true;
					}				
				}
				else {
					Long updatedLimitByEndDate = updatedStreamJSON.getLong(Constants.JSON_FIELD_LIMIT_BY_END_DATE);
					if (!updatedLimitByEndDate.equals(limitByEndDate)) {
						updateDetails.append(" 'limitByEndDate' changed from " + limitByEndDate + " to " + updatedLimitByEndDate + "\n");
						limitByEndDate = updatedLimitByEndDate;
						wasChanged = true;
					}
				}
			}
			
			//processEventsOfLastNumberOfDays - null is a legal value
			if (updatedStreamJSON.containsKey(Constants.JSON_FIELD_PROCESS_EVENTS_OF_LAST_NUMBER_OF_DAYS)) {
				if (updatedStreamJSON.get(Constants.JSON_FIELD_PROCESS_EVENTS_OF_LAST_NUMBER_OF_DAYS) == null) {
					if (processEventsOfLastNumberOfDays!=null) {
						updateDetails.append(" 'processEventsOfLastNumberOfDays' changed from " + processEventsOfLastNumberOfDays + " to null\n");
						processEventsOfLastNumberOfDays = null;
						wasChanged = true;
					}				
				}
				else {
					Integer updatedProcessEventsOfLastNumberOfDays = updatedStreamJSON.getInt(Constants.JSON_FIELD_PROCESS_EVENTS_OF_LAST_NUMBER_OF_DAYS);
					if (!updatedProcessEventsOfLastNumberOfDays.equals(processEventsOfLastNumberOfDays)) {
						updateDetails.append(" 'processEventsOfLastNumberOfDays' changed from " + processEventsOfLastNumberOfDays + " to " + updatedProcessEventsOfLastNumberOfDays + "\n");
						processEventsOfLastNumberOfDays = updatedProcessEventsOfLastNumberOfDays;
						wasChanged = true;
					}
				}
			}
		}
		
		if (updateMergeSchema) {

			try {
				JSONObject streamsJson = season.getStreams().toJson(OutputJSONMode.ADMIN);
				season.getInputSchema().mergeSchema (streamsJson, false, null);
			} catch (GenerationException e) {
				//ignore. should not happen since we are after validation
			}
		}
		
		if (wasChanged) {
			lastModified = new Date();
		}
		
		return updateDetails.toString();
	}		
	
	public ValidationResults validateStreamJSON(JSONObject streamObj, ServletContext context, UserInfo userInfo, String seasonId, boolean force) {
		@SuppressWarnings("unchecked")
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);
		Season season = seasonsDB.get(seasonId);
		
		try {
			Action action = Action.ADD;
			
			if (streamObj.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && streamObj.get(Constants.JSON_FIELD_UNIQUE_ID)!=null) {
				//if JSON contains uniqueId - update an existing feature otherwise create a new feature
				action = Action.UPDATE;
			}
			
			Stage stageObj = null;

			//name
			if (!streamObj.containsKey(Constants.JSON_FIELD_NAME) || streamObj.getString(Constants.JSON_FIELD_NAME) == null || streamObj.getString(Constants.JSON_FIELD_NAME).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_NAME), Status.BAD_REQUEST);
			}
			
			String objName = streamObj.getString(Constants.JSON_FIELD_NAME);
			String validateNameErr = Utilities.validateName(objName);
			if(validateNameErr!=null) {
				return new ValidationResults(validateNameErr, Status.BAD_REQUEST);
			}

			//validate name uniqueness within season			
			String uid = streamObj.optString(Constants.JSON_FIELD_UNIQUE_ID);
			ValidationResults res = validateStreamNameUniquness (season, objName, uid);
			if (res!=null)
				return res;			
			
			//enabled
			if (!streamObj.containsKey(Constants.JSON_FEATURE_FIELD_ENABLED) || streamObj.get(Constants.JSON_FEATURE_FIELD_ENABLED) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_ENABLED), Status.BAD_REQUEST);					
			}

			boolean newEnabled = streamObj.getBoolean(Constants.JSON_FEATURE_FIELD_ENABLED); //validate that is boolean value
				
			//stage
			if (!streamObj.containsKey(Constants.JSON_FEATURE_FIELD_STAGE) || streamObj.getString(Constants.JSON_FEATURE_FIELD_STAGE) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_STAGE), Status.BAD_REQUEST);					
			}			

			String stageStr = streamObj.getString(Constants.JSON_FEATURE_FIELD_STAGE);
			stageObj = Utilities.strToStage(stageStr);
			if (stageObj == null) {
				return new ValidationResults("Illegal stage: '" + stageStr + "'", Status.BAD_REQUEST);
			}		
						
			//filter cannot be empty if enabled is true
			if (!streamObj.containsKey(Constants.JSON_FIELD_FILTER) || streamObj.getString(Constants.JSON_FIELD_FILTER) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_FILTER), Status.BAD_REQUEST);
			}
			else if (streamObj.getString(Constants.JSON_FIELD_FILTER).isEmpty() && newEnabled==true) {
				return new ValidationResults("The filter field cannot be empty if stream is enabled", Status.BAD_REQUEST);				
			}
			
			String filter = streamObj.getString(Constants.JSON_FIELD_FILTER);
			
			ValidationCache validationCache = new ValidationCache();
			validationCache.setStreamsCache();
			
			JSONArray allEvents = season.getStreamsEvents().getEvents();
			StreamsScriptInvoker streamsScriptInvoker = null;			
			try {
				ValidationCache.Info info = validationCache.getInfo(context, season, stageObj, "");
				streamsScriptInvoker = (StreamsScriptInvoker)info.streamsInvoker;
				//VerifyStream.validateStreamFilter(streamsScriptInvoker, filter);
				// TODO: the same verification is also done below. THe only reason for repeating it here is to pinpoint the error as a filter error
				VerifyStream.validateStreamFilter(streamsScriptInvoker, filter, allEvents);
			} catch (Exception e) {
				return new ValidationResults("Illegal filter: " + e.getMessage(), Status.BAD_REQUEST);
			}
			
			//processor cannot be empty if enabled is true
			if (!streamObj.containsKey(Constants.JSON_FIELD_PROCESSOR) || streamObj.getString(Constants.JSON_FIELD_PROCESSOR) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_PROCESSOR), Status.BAD_REQUEST);
			}
			else if (streamObj.getString(Constants.JSON_FIELD_PROCESSOR).isEmpty() && newEnabled==true) {
				return new ValidationResults("The processor field cannot be empty if stream is enabled", Status.BAD_REQUEST);				
			}

			String processor = streamObj.getString(Constants.JSON_FIELD_PROCESSOR);
			try {
				
				//VerifyStream.evaluateProcessor(streamsScriptInvoker, processor, allEvents);
				// this one is preferable because it prunes the event list before testing the processor
				VerifyStream.evaluateFilterAndProcessor(streamsScriptInvoker, filter, processor, allEvents);
				
			} catch (InvokerException e) {
				return new ValidationResults("Illegal processor: " + e.getMessage(), Status.BAD_REQUEST);
			}
					
			//resultsSchema cannot be empty if enabled is true
			if (!streamObj.containsKey(Constants.JSON_FIELD_RESULTS_SCHEMA) || streamObj.getString(Constants.JSON_FIELD_RESULTS_SCHEMA) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_RESULTS_SCHEMA), Status.BAD_REQUEST);
			}
			else if (streamObj.getString(Constants.JSON_FIELD_RESULTS_SCHEMA).isEmpty() && newEnabled==true) {
				return new ValidationResults("The resultsSchema field cannot be empty if stream is enabled", Status.BAD_REQUEST);				
			}
			
			
			//rolloutPercentage
			if (!streamObj.containsKey(Constants.JSON_FEATURE_FIELD_PERCENTAGE) || streamObj.get(Constants.JSON_FEATURE_FIELD_PERCENTAGE) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_PERCENTAGE), Status.BAD_REQUEST);
			}
			
			Double rp = streamObj.getDouble(Constants.JSON_FEATURE_FIELD_PERCENTAGE); //validate that is legal double
			if (rp<0 || rp>100) {
				return new ValidationResults("Illegal Rollout Percentage value. Should be a double between 0 and 100.", Status.BAD_REQUEST);
			}

			
			//minAppVersion
			if (!streamObj.containsKey(Constants.JSON_FEATURE_FIELD_MIN_APP_VER) || streamObj.get(Constants.JSON_FEATURE_FIELD_MIN_APP_VER) == null || streamObj.getString(Constants.JSON_FEATURE_FIELD_MIN_APP_VER).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_MIN_APP_VER), Status.BAD_REQUEST);					
			}			
						 			
			String updatedMinVer = streamObj.getString(Constants.JSON_FEATURE_FIELD_MIN_APP_VER);
					
			//creator
			if (!streamObj.containsKey(Constants.JSON_FEATURE_FIELD_CREATOR) || streamObj.getString(Constants.JSON_FEATURE_FIELD_CREATOR) == null || streamObj.getString(Constants.JSON_FEATURE_FIELD_CREATOR).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_CREATOR), Status.BAD_REQUEST);
			}	
			
			//season id
			if (!streamObj.containsKey(Constants.JSON_FEATURE_FIELD_SEASON_ID) || streamObj.get(Constants.JSON_FEATURE_FIELD_SEASON_ID)==null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_SEASON_ID), Status.BAD_REQUEST);
			}
			
			//internalUserGroups - optional
			if (streamObj.containsKey(Constants.JSON_FIELD_INTERNAL_USER_GROUPS) && streamObj.get(Constants.JSON_FIELD_INTERNAL_USER_GROUPS) != null) {				
				JSONArray groupsArr = streamObj.getJSONArray(Constants.JSON_FIELD_INTERNAL_USER_GROUPS); //validate that is String array value

				@SuppressWarnings("unchecked")
				Map<String,InternalUserGroups> groupsPerProductMap = (Map<String,InternalUserGroups>) context.getAttribute(Constants.USER_GROUPS_PER_PRODUCT_PARAM_NAME);
				
				@SuppressWarnings("unchecked")
				Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);					

				Product product = productsDB.get(season.getProductId().toString());

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
			
			//cacheSizeKB - optional
			if (streamObj.containsKey(Constants.JSON_FIELD_CACHE_SIZE_KB) && streamObj.get(Constants.JSON_FIELD_CACHE_SIZE_KB) != null) {
				int cs = streamObj.getInt(Constants.JSON_FIELD_CACHE_SIZE_KB);
				if (cs<=0) {
					return new ValidationResults("The cacheSizeKB value is illegal. Should be a positive integer.", Status.BAD_REQUEST);
				}
			}
			
			//queueSizeKB - optional
			if (streamObj.containsKey(Constants.JSON_FIELD_QUEUE_SIZE_KB) && streamObj.get(Constants.JSON_FIELD_QUEUE_SIZE_KB) != null) {
				int qs = streamObj.getInt(Constants.JSON_FIELD_QUEUE_SIZE_KB);
				if (qs<=0) {
					return new ValidationResults("The queueSizeKB value is illegal. Should be a positive integer.", Status.BAD_REQUEST);
				}
			}
			
			//maxQueuedEvents - optional
			if (streamObj.containsKey(Constants.JSON_FIELD_MAX_QUEUED_EVENTS) && streamObj.get(Constants.JSON_FIELD_MAX_QUEUED_EVENTS) != null) {
				int mqe = streamObj.getInt(Constants.JSON_FIELD_MAX_QUEUED_EVENTS);
				if (mqe<=0) { //TODO:
					return new ValidationResults("The maxQueuedEvents value is illegal. Should be a positive integer.", Status.BAD_REQUEST);
				}
			}
			
			//operateOnHistoricalEvents - for now history properties are optional until implemented in theh console
			if (streamObj.containsKey(Constants.JSON_FIELD_OPERATE_ON_HISTORICAL_EVENTS)) {
				if (!streamObj.containsKey(Constants.JSON_FIELD_OPERATE_ON_HISTORICAL_EVENTS) || streamObj.get(Constants.JSON_FIELD_OPERATE_ON_HISTORICAL_EVENTS) == null) {
					return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_OPERATE_ON_HISTORICAL_EVENTS), Status.BAD_REQUEST);
				}
				
				Boolean his = streamObj.getBoolean(Constants.JSON_FIELD_OPERATE_ON_HISTORICAL_EVENTS); //validate that is legal boolean
				if (his) {
					//verify that the streams global enableHistoricalEvents is on
					if (!season.getStreams().isEnableHistoricalEvents()) {
						return new ValidationResults("Cannot turn operation on historical events on. The global setting that enable historical events saving is off.", Status.BAD_REQUEST);
					}	
				}
				
				//limitByStartDate - null is legal value
				if (!streamObj.containsKey(Constants.JSON_FIELD_LIMIT_BY_START_DATE)) {
					return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_LIMIT_BY_START_DATE), Status.BAD_REQUEST);
				}
				
				Long limByStart = null;
				if (streamObj.get(Constants.JSON_FIELD_LIMIT_BY_START_DATE) != null) {
					limByStart = streamObj.getLong(Constants.JSON_FIELD_LIMIT_BY_START_DATE); 
				}
				
				//limitByEndDate - null is legal value
				if (!streamObj.containsKey(Constants.JSON_FIELD_LIMIT_BY_END_DATE)) {
					return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_LIMIT_BY_END_DATE), Status.BAD_REQUEST);
				}
				
				Long limByEnd = null;
				if (streamObj.get(Constants.JSON_FIELD_LIMIT_BY_END_DATE) != null) {
					limByEnd = streamObj.getLong(Constants.JSON_FIELD_LIMIT_BY_END_DATE); 
				}
				
				//processEventsOfLastNumberOfDays - null is legal value
				if (!streamObj.containsKey(Constants.JSON_FIELD_PROCESS_EVENTS_OF_LAST_NUMBER_OF_DAYS)) {
					return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_PROCESS_EVENTS_OF_LAST_NUMBER_OF_DAYS), Status.BAD_REQUEST);
				}
				
				Integer processHisNumDays = null;
				if (streamObj.get(Constants.JSON_FIELD_PROCESS_EVENTS_OF_LAST_NUMBER_OF_DAYS) != null) {
					processHisNumDays = streamObj.getInt(Constants.JSON_FIELD_PROCESS_EVENTS_OF_LAST_NUMBER_OF_DAYS); 
					
					//verify that the streams global keepHistoryOfLastNumberOfDays is greater
					if (season.getStreams().getKeepHistoryOfLastNumberOfDays()!=null && season.getStreams().getKeepHistoryOfLastNumberOfDays() < processHisNumDays) {
						return new ValidationResults("Cannot set " + Constants.JSON_FIELD_PROCESS_EVENTS_OF_LAST_NUMBER_OF_DAYS + " value to " + processHisNumDays + ". It exceeds the global " + Constants.JSON_FIELD_KEEP_HISTORY_OF_LAST_NUM_DAYS, Status.BAD_REQUEST);
					}
				}
				
				if (limByStart!=null && limByEnd!=null && limByStart>limByEnd) {
					return new ValidationResults(Strings.historyRangeStartExceedEnd, Status.BAD_REQUEST);
				}
						
				if ((limByStart!=null || limByEnd!=null) && processHisNumDays!=null) {
					return new ValidationResults(Strings.CannotConfigStreamHistoryRangeAndDaysNumber, Status.BAD_REQUEST);
				}
			}
			
			//description, displayName, owner are optional fields
			if (action == Action.ADD){
				//stream in production can be added only by Administrator or ProductLead
				if (stageObj.equals(Stage.PRODUCTION) && !validRole(userInfo)) {
					return new ValidationResults("Unable to add the stream. Only a user with the Administrator or Product Lead role can add streams in the production stage.", Status.UNAUTHORIZED);					
				}
				
				//creation date => should not appear in branch creation
				if (streamObj.containsKey(Constants.JSON_FEATURE_FIELD_CREATION_DATE) && streamObj.get(Constants.JSON_FEATURE_FIELD_CREATION_DATE)!=null) {
					return new ValidationResults("The creationDate field cannot be specified during creation.", Status.BAD_REQUEST);
				}
				
				//verify that not higher than seasons max version
				//this is checked only in create - in update this is not checked since the seasons min-maxVersion may have been changed. 
				String minAppVersion = streamObj.getString(Constants.JSON_FEATURE_FIELD_MIN_APP_VER);
				String seasonMaxVer = seasonsDB.get(seasonId).getMaxVersion();
				if (seasonMaxVer!=null) {
					if (Season.compare (minAppVersion, seasonMaxVer) >=0) {
						return new ValidationResults("The stream's Minimum App Version must be less than the Maximum Version of the current version range.", Status.BAD_REQUEST);
					}
				}
				
				//modification date => should not appear in stream creation
				if (streamObj.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) && streamObj.get(Constants.JSON_FIELD_LAST_MODIFIED)!=null) {
					return new ValidationResults("The lastModified field cannot be specified during stream creation.", Status.BAD_REQUEST);
				}
				
				String resSchema = streamObj.getString(Constants.JSON_FIELD_RESULTS_SCHEMA);
				if (!resSchema.isEmpty()) {
					JSONObject streamsJson = season.getStreams().toJson(OutputJSONMode.ADMIN);
					JSONArray streamsArr = streamsJson.getJSONArray(Constants.JSON_FIELD_STREAMS);
					streamsArr.add(streamObj);
					try {						
						season.getInputSchema().mergeSchema (streamsJson, true, null);
					} catch (GenerationException e) {
						return new ValidationResults(e.getMessage(), Status.BAD_REQUEST);
					}
				}
			}
			else { //UPDATE
				//if stream is in production or is updated from stage DEVELOPMENT to PRODUCTION
				if (stage.equals(Stage.PRODUCTION) || stageObj.equals(Stage.PRODUCTION)) {						
					//only productLead or Administrator can update stream in production
					if (!validRole(userInfo)) {
						return new ValidationResults("Unable to update. Only a user with the Administrator or Product Lead role can update a stream in the production stage.", Status.UNAUTHORIZED);
					}
					
					//name of feature in production cannot be changed
					if (!objName.equals(name)) {
						return new ValidationResults("You cannot change the name of a stream in the production stage.", Status.UNAUTHORIZED);
					}
				}
								
				//creation date must appear
				if (!streamObj.containsKey(Constants.JSON_FEATURE_FIELD_CREATION_DATE) || streamObj.get(Constants.JSON_FEATURE_FIELD_CREATION_DATE)==null) {
					return new ValidationResults("The creationDate field is missing. This field must be specified during update.", Status.BAD_REQUEST);
				}
				
				//verify that legal long
				long creationdateLong = streamObj.getLong(Constants.JSON_FEATURE_FIELD_CREATION_DATE);
				
				//verify that was not changed
				if (!creationDate.equals(new Date(creationdateLong))) {
					return new ValidationResults("creationDate cannot be changed during update", Status.BAD_REQUEST);
				}
				
				//creator must exist and not be changed
				String creatorStr = streamObj.getString(Constants.JSON_FEATURE_FIELD_CREATOR);
				if (!creator.equals(creatorStr)) {
					return new ValidationResults(String.format(Strings.fieldCannotBeChangedDuringUpdate, Constants.JSON_FEATURE_FIELD_CREATOR), Status.BAD_REQUEST);
				}
				
				//modification date must appear
				if (!streamObj.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) || streamObj.get(Constants.JSON_FIELD_LAST_MODIFIED)==null) {
					return new ValidationResults(Strings.lastModifiedIsMissing, Status.BAD_REQUEST);
				}
				
				//verify that given modification date is not older than current modification date
				long givenModoficationDate = streamObj.getLong(Constants.JSON_FIELD_LAST_MODIFIED);  //verify that legal long
				Date givenDate = new Date(givenModoficationDate);
				if (givenDate.before(lastModified)) {
					return new ValidationResults("The stream was changed by another user. Refresh your browser and try again.", Status.CONFLICT);			
				}
				
				//season id must exists and not be changed
				String seasonIdStr = streamObj.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID);
				if (!seasonIdStr.equals(seasonId)) {
					return new ValidationResults(String.format(Strings.fieldCannotBeChangedDuringUpdate, Constants.JSON_FEATURE_FIELD_SEASON_ID), Status.BAD_REQUEST);
				}
				
				String resSchema = streamObj.getString(Constants.JSON_FIELD_RESULTS_SCHEMA);
				JSONObject updatedMergedSchema = null;
				//if the schema was changed or the stream name was changed or stream moved from prod to dev or when minVersion was changed
				if (!resultsSchema.equals(resSchema) || !name.equals(objName) || 
						(stage.equals(Stage.PRODUCTION) && stageObj.equals(Stage.DEVELOPMENT)) ||
						!minAppVersion.equals(updatedMinVer)) {  
					JSONObject streamsJson = season.getStreams().toJson(OutputJSONMode.ADMIN);
					JSONArray streamsArr = streamsJson.getJSONArray(Constants.JSON_FIELD_STREAMS);
					for (int i=0; i<streamsArr.size(); i++) {
						JSONObject stream = streamsArr.getJSONObject(i);
						if (stream.getString(Constants.JSON_FIELD_UNIQUE_ID).equals(uniqueId.toString())) {
							streamsArr.set(i, streamObj);
							break;
						}
					}
							
					try {						
						updatedMergedSchema = season.getInputSchema().mergeSchema (streamsJson, true, null);
					} catch (GenerationException e) {
						return new ValidationResults(e.getMessage(), Status.BAD_REQUEST);
					}
											
					//go over the season's features and configRules and validate that no rule or configuration uses the missing stream
					try {
						String verFuncRes = InputSchema.validateRulesAndConfigWithNewSchemaOrChangedUtility (season, updatedMergedSchema, context, null, null, null, null, null, null, null, null, null);
						if (verFuncRes != null)
							return new ValidationResults ("Unable to update stream: " + verFuncRes, Status.BAD_REQUEST);
		
					} catch (GenerationException ge) {
						return new ValidationResults ("Failed to generate the data sample: " + ge.getMessage(), Status.BAD_REQUEST);
					} catch (JSONException jsne) {
						return new ValidationResults (jsne.getMessage(), Status.BAD_REQUEST);
					} catch (InterruptedException ie) {
						return new ValidationResults ("Failed to validate branches: " + ie.getMessage(), Status.BAD_REQUEST);
					} catch (ExecutionException ee) {
						return new ValidationResults ("Failed to validate branches: " + ee.getMessage(), Status.BAD_REQUEST);
					}
				}
			}			
		} catch (JSONException jsne) {
			return new ValidationResults(jsne.getMessage(), Status.BAD_REQUEST);
		}
		return null;
	}
	static boolean validRole(UserInfo userInfo)
	{
		return userInfo == null || userInfo.getRoles().contains(Constants.RoleType.Administrator) || userInfo.getRoles().contains(Constants.RoleType.ProductLead);
	}
	private ValidationResults validateStreamNameUniquness(Season season, String newName, String streamId) {
		String replacedNewName = newName.replace(".", " ");
		for (AirlockStream stream:season.getStreams().getStreamsList()) {
			if (streamId != null && streamId.equals(stream.getUniqueId().toString())) {
				continue; //skip the current stream in update				
			}
			
			String replacedName = stream.getName().replace(".", " ");
			if (replacedName.equalsIgnoreCase(replacedNewName)) {
				return new ValidationResults("A stream with the specified name already exists in the current version range. Periods and spaces are considered the same.", Status.BAD_REQUEST);					
			}
		}
	
		return null;
	}
	
	public ValidationResults validateProductionDontChanged(JSONObject updatedAnalyticsItemJSON) throws JSONException {
		
		Stage updatedStage = Utilities.strToStage(updatedAnalyticsItemJSON.getString(Constants.JSON_FEATURE_FIELD_STAGE));
		if (updatedStage  == Stage.PRODUCTION && stage == Stage.DEVELOPMENT) {			
			return new ValidationResults("Unable to update the stream. Only a user with the Administrator or Product Lead can change stream from the development to the production stage.", Status.UNAUTHORIZED);	
		}
		
		if (updatedStage  == Stage.DEVELOPMENT && stage == Stage.PRODUCTION) {			
			return new ValidationResults("Unable to update the stream. Only a user with the Administrator or Product Lead can change stream from the production to the development stage.", Status.UNAUTHORIZED);	
		}

		String err = "Unable to update the stream. Only a user with the Administrator or Product Lead role can change stream that is in the production stage.";
		
		if (stage == Stage.PRODUCTION) {
			String updatedName = updatedAnalyticsItemJSON.getString(Constants.JSON_FIELD_NAME);
			if (!updatedName.equals(name)) 
				return new ValidationResults(err, Status.UNAUTHORIZED);
			
			String updatedFilter = updatedAnalyticsItemJSON.getString(Constants.JSON_FIELD_FILTER);
			if (!updatedFilter.equals(filter)) 
				return new ValidationResults(err, Status.UNAUTHORIZED);
			
			String updatedProcessor = updatedAnalyticsItemJSON.getString(Constants.JSON_FIELD_PROCESSOR);
			if (!updatedProcessor.equals(processor)) 
				return new ValidationResults(err, Status.UNAUTHORIZED);
						
			String updatedResultsSchema = updatedAnalyticsItemJSON.getString(Constants.JSON_FIELD_RESULTS_SCHEMA);
			if (!updatedResultsSchema.equals(resultsSchema)) 
				return new ValidationResults(err, Status.UNAUTHORIZED);
			
			int updatedRolloutPercentage = updatedAnalyticsItemJSON.getInt(Constants.JSON_FEATURE_FIELD_PERCENTAGE);
			if (rolloutPercentage  != updatedRolloutPercentage) 
				return new ValidationResults(err, Status.UNAUTHORIZED);
			
			Boolean updatedEnabled = updatedAnalyticsItemJSON.getBoolean(Constants.JSON_FEATURE_FIELD_ENABLED);
			if (enabled != updatedEnabled) 
				return new ValidationResults(err, Status.UNAUTHORIZED);			
			
			String updatedMinAppVersion = updatedAnalyticsItemJSON.getString(Constants.JSON_FEATURE_FIELD_MIN_APP_VER);
			if (minAppVersion==null || !minAppVersion.equals(updatedMinAppVersion)) {
				return new ValidationResults(err, Status.UNAUTHORIZED);													
			}
			
			Boolean updatedOperateOnHistoricalEvents = updatedAnalyticsItemJSON.getBoolean(Constants.JSON_FIELD_OPERATE_ON_HISTORICAL_EVENTS);
			if (operateOnHistoricalEvents != updatedOperateOnHistoricalEvents) 
				return new ValidationResults(err, Status.UNAUTHORIZED);		
			
			//limitByStartDate - null is legal value
			if (updatedAnalyticsItemJSON.containsKey(Constants.JSON_FIELD_LIMIT_BY_START_DATE)) {
				if (updatedAnalyticsItemJSON.get(Constants.JSON_FIELD_LIMIT_BY_START_DATE) == null) {
					if (limitByStartDate!=null)				
						return new ValidationResults(err, Status.UNAUTHORIZED);
				}
				else {
					Long updatedLimitByStartDate = updatedAnalyticsItemJSON.getLong(Constants.JSON_FIELD_LIMIT_BY_START_DATE);
					if (!updatedLimitByStartDate.equals(limitByStartDate)) 
						return new ValidationResults(err, Status.UNAUTHORIZED);
				}
			}
			
			//limitByEndDate - null is legal value
			if (updatedAnalyticsItemJSON.containsKey(Constants.JSON_FIELD_LIMIT_BY_END_DATE)) {
				if (updatedAnalyticsItemJSON.get(Constants.JSON_FIELD_LIMIT_BY_END_DATE) == null) {
					if (limitByEndDate!=null)				
						return new ValidationResults(err, Status.UNAUTHORIZED);
				}
				else {
					Long updatedLimitByEndDate = updatedAnalyticsItemJSON.getLong(Constants.JSON_FIELD_LIMIT_BY_END_DATE);
					if (!updatedLimitByEndDate.equals(limitByEndDate)) 
						return new ValidationResults(err, Status.UNAUTHORIZED);
				}
			}
			
			//processEventsOfLastNumberOfDays - null is legal value
			if (updatedAnalyticsItemJSON.containsKey(Constants.JSON_FIELD_PROCESS_EVENTS_OF_LAST_NUMBER_OF_DAYS)) {
				if (updatedAnalyticsItemJSON.get(Constants.JSON_FIELD_PROCESS_EVENTS_OF_LAST_NUMBER_OF_DAYS) == null) {
					if (processEventsOfLastNumberOfDays!=null)				
						return new ValidationResults(err, Status.UNAUTHORIZED);
				}
				else {
					Integer updatedProcessEventsOfLastNumberOfDays = updatedAnalyticsItemJSON.getInt(Constants.JSON_FIELD_PROCESS_EVENTS_OF_LAST_NUMBER_OF_DAYS);
					if (!updatedProcessEventsOfLastNumberOfDays.equals(processEventsOfLastNumberOfDays)) 
						return new ValidationResults(err, Status.UNAUTHORIZED);
				}
			}
			
			//optional fields
			//description
			if (updatedAnalyticsItemJSON.containsKey(Constants.JSON_FIELD_DESCRIPTION) &&  updatedAnalyticsItemJSON.get(Constants.JSON_FIELD_DESCRIPTION)!=null) {
				//if missing from json or null - ignore
				String updatedDescription = updatedAnalyticsItemJSON.getString(Constants.JSON_FIELD_DESCRIPTION);
				if (description == null || !description.equals(updatedDescription)) {
					return new ValidationResults(err, Status.UNAUTHORIZED);				
				}
			}		
			
			//displayName
			if (updatedAnalyticsItemJSON.containsKey(Constants.JSON_FIELD_DISPLAY_NAME) &&  updatedAnalyticsItemJSON.get(Constants.JSON_FIELD_DISPLAY_NAME)!=null) {
				//if missing from json or null - ignore
				String updatedDisplayName = updatedAnalyticsItemJSON.getString(Constants.JSON_FIELD_DISPLAY_NAME);
				if (displayName == null || !displayName.equals(updatedDisplayName)) {
					return new ValidationResults(err, Status.UNAUTHORIZED);				
				}
			}		
						
			//cacheSizeKB
			if (updatedAnalyticsItemJSON.containsKey(Constants.JSON_FIELD_CACHE_SIZE_KB)) {
				if (updatedAnalyticsItemJSON.get(Constants.JSON_FIELD_CACHE_SIZE_KB) == null) {
					if (cacheSizeKB!=null)				
						return new ValidationResults(err, Status.UNAUTHORIZED);
				}
				else {
					Integer updatedCacheSizeKB = updatedAnalyticsItemJSON.getInt(Constants.JSON_FIELD_CACHE_SIZE_KB);
					if (!updatedCacheSizeKB.equals(cacheSizeKB)) 
						return new ValidationResults(err, Status.UNAUTHORIZED);
				}
			}
			
			//queueSizeKB
			if (updatedAnalyticsItemJSON.containsKey(Constants.JSON_FIELD_QUEUE_SIZE_KB)) {
				if (updatedAnalyticsItemJSON.get(Constants.JSON_FIELD_QUEUE_SIZE_KB) == null) {
					if (queueSizeKB!=null)				
						return new ValidationResults(err, Status.UNAUTHORIZED);
				}
				else {
					Integer updatedQueueSizeKB = updatedAnalyticsItemJSON.getInt(Constants.JSON_FIELD_QUEUE_SIZE_KB);
					if (!updatedQueueSizeKB.equals(queueSizeKB)) 
						return new ValidationResults(err, Status.UNAUTHORIZED);
				}
			}
			
			//maxQueuedEvents
			if (updatedAnalyticsItemJSON.containsKey(Constants.JSON_FIELD_MAX_QUEUED_EVENTS)) {
				if (updatedAnalyticsItemJSON.get(Constants.JSON_FIELD_MAX_QUEUED_EVENTS) == null) {
					if (maxQueuedEvents != null)
						return new ValidationResults(err, Status.UNAUTHORIZED);
				}
				else {
					Integer updatedMaxQueuedEvents= updatedAnalyticsItemJSON.getInt(Constants.JSON_FIELD_MAX_QUEUED_EVENTS);
					if (!updatedMaxQueuedEvents.equals(maxQueuedEvents)) 
						return new ValidationResults(err, Status.UNAUTHORIZED);
				}
			}
			
			//owner
			if (updatedAnalyticsItemJSON.containsKey(Constants.JSON_FEATURE_FIELD_OWNER) &&  updatedAnalyticsItemJSON.get(Constants.JSON_FEATURE_FIELD_OWNER)!=null) {
				//if missing from json or null - ignore
				String updatedOwner = updatedAnalyticsItemJSON.getString(Constants.JSON_FEATURE_FIELD_OWNER);
				if (owner == null || !owner.equals(updatedOwner)) {
					return new ValidationResults(err, Status.UNAUTHORIZED);				
				}
			}		
		}
				
		return null;		
	}
			
	
	public AirlockStream duplicateForNewSeason(String minVersion, UUID newSeasonId) {
		AirlockStream res = new AirlockStream();
		
		res.setUniqueId(UUID.randomUUID());
		//res.setMinAppVersion(minVersion==null?minAppVersion:minVersion); 		
		res.setMinAppVersion(minAppVersion);
		res.setSeasonId(newSeasonId);
		res.setLastModified(lastModified);
		res.setStage(stage);
		res.setName(name);
		res.setDescription(description);
		res.setDisplayName(displayName);
		res.setEnabled(enabled);
		res.setCreationDate(creationDate);
		res.setCreator(creator);
		res.setInternalUserGroups(internalUserGroups == null ? null:internalUserGroups.clone());
		res.setRolloutPercentage(rolloutPercentage);
		res.setFilter(filter);
		res.setProcessor(processor);
		res.setCacheSizeKB(cacheSizeKB);
		res.setQueueSizeKB(queueSizeKB);
		res.setMaxQueuedEvents(maxQueuedEvents);		
		res.setOwner(owner);
		res.setResultsSchema(resultsSchema);
		res.setOperateOnHistoricalEvents(operateOnHistoricalEvents);
		res.setLimitByEndDate(limitByEndDate);
		res.setLimitByStartDate(limitByStartDate);
		res.setProcessEventsOfLastNumberOfDayss(processEventsOfLastNumberOfDays);
		return res;
	}
	
}
