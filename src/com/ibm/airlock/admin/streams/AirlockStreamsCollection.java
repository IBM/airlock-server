package com.ibm.airlock.admin.streams;

import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Strings;
import com.ibm.airlock.Constants.OutputJSONMode;
import com.ibm.airlock.Constants.Stage;
import com.ibm.airlock.admin.GenerationException;
import com.ibm.airlock.admin.InputSchema;
import com.ibm.airlock.admin.Season;
import com.ibm.airlock.admin.ValidationCache;
import com.ibm.airlock.admin.ValidationResults;
import com.ibm.airlock.engine.StreamsScriptInvoker;
import com.ibm.airlock.engine.VerifyStream;
import com.ibm.airlock.engine.ScriptInvoker.InvokerException;
import javax.ws.rs.core.Response.Status;

public class AirlockStreamsCollection {
	public static final Logger logger = Logger.getLogger(AirlockStreamsCollection.class.getName());

	private LinkedList<AirlockStream> streamsList = new LinkedList<AirlockStream>();
	private UUID seasonId = null; 
	private boolean enableHistoricalEvents = false;
	private String filter = "";
	private Integer maxHistorySizeKB = 15*1024; //15M
	private Integer keepHistoryOfLastNumberOfDays = null; //c+u null means unlimited
	private Integer bulkSize = 1; //c+u
	private Integer fileSizeKB = 1*1024; //c+u 1M
	private Date lastModified = null; // required in update. forbidden in create
	private Integer historyBufferSize = null;

	public AirlockStreamsCollection(UUID seasonId) {
		this.seasonId = seasonId;
	}

	public LinkedList<AirlockStream> getStreamsList() {
		return streamsList;
	}

	public void setStreamsList(LinkedList<AirlockStream> streamsList) {
		this.streamsList = streamsList;
	}

	public UUID getSeasonId() {
		return seasonId;
	}

	public void setSeasonId(UUID seasonId) {
		this.seasonId = seasonId;
	}

	public boolean isEnableHistoricalEvents() {
		return enableHistoricalEvents;
	}

	public void setEnableHistoricalEvents(boolean enableHistoricalEvents) {
		this.enableHistoricalEvents = enableHistoricalEvents;
	}

	public String getFilter() {
		return filter;
	}

	public void setFilter(String filter) {
		this.filter = filter;
	}

	public int getMaxHistorySizeKB() {
		return maxHistorySizeKB;
	}

	public void setMaxHistorySizeKB(int maxHistorySizeKB) {
		this.maxHistorySizeKB = maxHistorySizeKB;
	}

	public Integer getKeepHistoryOfLastNumberOfDays() {
		return keepHistoryOfLastNumberOfDays;
	}

	public void setKeepHistoryOfLastNumberOfDays(Integer keepHistoryOfLastNumberOfDays) {
		this.keepHistoryOfLastNumberOfDays = keepHistoryOfLastNumberOfDays;
	}

	public int getBulkSize() {
		return bulkSize;
	}

	public void setBulkSize(int bulkSize) {
		this.bulkSize = bulkSize;
	}

	public int getFileSizeKB() {
		return fileSizeKB;
	}

	public void setFileSizeKB(int fileSizeKB) {
		this.fileSizeKB = fileSizeKB;
	}

	public Date getLastModified() {
		return lastModified;
	}
	
	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}	
	
	public static Logger getLogger() {
		return logger;
	}

	public Integer getHistoryBufferSize() {
		return historyBufferSize;
	}

	public void setHistoryBufferSize(Integer historyBufferSize) {
		this.historyBufferSize = historyBufferSize;
	}

	public JSONObject toJson(OutputJSONMode mode) throws JSONException {
		JSONObject res = new JSONObject();
		
		res.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, seasonId==null?null:seasonId.toString());
		res.put(Constants.JSON_FIELD_FILTER, filter);
		res.put(Constants.JSON_FIELD_MAX_HISTORY_SIZE_KB, maxHistorySizeKB);
		res.put(Constants.JSON_FIELD_ENABLE_HISTORICAL_EVENTS, enableHistoricalEvents);
		res.put(Constants.JSON_FIELD_BULK_SIZE, bulkSize);
		res.put(Constants.JSON_FIELD_FILE_SIZE_KB, fileSizeKB);
		res.put(Constants.JSON_FIELD_KEEP_HISTORY_OF_LAST_NUM_DAYS, keepHistoryOfLastNumberOfDays);
		res.put(Constants.JSON_FIELD_HISTORY_BUFFER_SIZE, historyBufferSize);
		
		if (mode.equals(OutputJSONMode.ADMIN) || mode.equals(OutputJSONMode.DISPLAY)) {	
			res.put(Constants.JSON_FIELD_LAST_MODIFIED, lastModified.getTime());
		}
		
		if (!mode.equals(OutputJSONMode.DISPLAY)) {
			JSONArray streamsArr = new JSONArray();
			for (AirlockStream alStream : streamsList) {
				if (mode != null && alStream.getStage() == Stage.DEVELOPMENT && mode.equals(OutputJSONMode.RUNTIME_PRODUCTION))
					continue; //don't return stream in development when runtime_prod is requested
	
				JSONObject auObj = alStream.toJson(mode);
				streamsArr.add(auObj);				
			}
			res.put(Constants.JSON_FIELD_STREAMS, streamsArr);
		}
		
		return res;
	}

	//called in server init stage - reading the streams from files in s3
	public void fromJSON(JSONObject input, Map<String, AirlockStream> streamsDB) throws JSONException {
		if (input.containsKey(Constants.JSON_FIELD_KEEP_HISTORY_OF_LAST_NUM_DAYS) && input.get(Constants.JSON_FIELD_KEEP_HISTORY_OF_LAST_NUM_DAYS)!=null) {
			keepHistoryOfLastNumberOfDays  = input.getInt(Constants.JSON_FIELD_KEEP_HISTORY_OF_LAST_NUM_DAYS);			
		}
		
		if (input.containsKey(Constants.JSON_FIELD_HISTORY_BUFFER_SIZE) && input.get(Constants.JSON_FIELD_HISTORY_BUFFER_SIZE)!=null) {
			historyBufferSize  = input.getInt(Constants.JSON_FIELD_HISTORY_BUFFER_SIZE);			
		}
		
		if (input.containsKey(Constants.JSON_FIELD_ENABLE_HISTORICAL_EVENTS) && input.get(Constants.JSON_FIELD_ENABLE_HISTORICAL_EVENTS)!=null) {
			enableHistoricalEvents  = input.getBoolean(Constants.JSON_FIELD_ENABLE_HISTORICAL_EVENTS);			
		}
		
		if (input.containsKey(Constants.JSON_FIELD_MAX_HISTORY_SIZE_KB) && input.get(Constants.JSON_FIELD_MAX_HISTORY_SIZE_KB)!=null) {
			maxHistorySizeKB  = input.getInt(Constants.JSON_FIELD_MAX_HISTORY_SIZE_KB);			
		}
		
		if (input.containsKey(Constants.JSON_FIELD_BULK_SIZE) && input.get(Constants.JSON_FIELD_BULK_SIZE)!=null) {
			bulkSize  = input.getInt(Constants.JSON_FIELD_BULK_SIZE);			
		}
		
		if (input.containsKey(Constants.JSON_FIELD_FILE_SIZE_KB) && input.get(Constants.JSON_FIELD_FILE_SIZE_KB)!=null) {
			fileSizeKB  = input.getInt(Constants.JSON_FIELD_FILE_SIZE_KB);			
		}
		
		if (input.containsKey(Constants.JSON_FIELD_FILTER) && input.get(Constants.JSON_FIELD_FILTER)!=null) {
			filter = input.getString(Constants.JSON_FIELD_FILTER);
		}
		
		if (input.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) && input.get(Constants.JSON_FIELD_LAST_MODIFIED)!=null) { 
			long timeInMS = input.getLong(Constants.JSON_FIELD_LAST_MODIFIED);
			lastModified = new Date(timeInMS);
		}  else {
			lastModified = new Date();
		}
		
		JSONArray streamsArr = input.getJSONArray(Constants.JSON_FIELD_STREAMS);
		for (int i=0; i<streamsArr.length(); i++ ) {
			JSONObject alStreamJSON = streamsArr.getJSONObject(i);
			AirlockStream alStream = new AirlockStream();
			alStream.fromJSON(alStreamJSON);
			addAirlockStream(alStream);
			if (streamsDB!=null)
				streamsDB.put(alStream.getUniqueId().toString(), alStream);
		}			
	}

	public void addAirlockStream(AirlockStream alStream) {
		streamsList.add(alStream);		
	}

	//return null if OK, error string on error
	public String removeAirlockStream(AirlockStream streamToRem, Season season, ServletContext context) {
		String alStreamId = streamToRem.getUniqueId().toString(); 
		if (streamsList == null || streamsList.size() == 0) {
			return "Unable to remove stream " + alStreamId + ", " + streamToRem.getName() + " from season " + seasonId.toString() + ": season has no streams.";	
		}

		try {
			//create input schema without the deleted stream
			JSONObject streamsJson = season.getStreams().toJson(OutputJSONMode.ADMIN);
			JSONArray streamsArr = streamsJson.getJSONArray(Constants.JSON_FIELD_STREAMS);
			JSONObject updatedMergedSchema = null;
			for (int i=0; i<streamsArr.size(); i++) {
				JSONObject stream = streamsArr.getJSONObject(i);
				if (stream.getString(Constants.JSON_FIELD_UNIQUE_ID).equals(streamToRem.getUniqueId().toString())) {
					streamsArr.remove(i);
					break;
				}
			}

			try {						
				updatedMergedSchema = season.getInputSchema().mergeSchema (streamsJson, true, null);
			} catch (GenerationException e) {
				return "Unable to remove stream '" + alStreamId + "' from season '" + seasonId.toString() + "': " + e.getMessage();
			}

			//validate that no rule/conf is using the deleted stream's fields
			String verFuncRes = InputSchema.validateRulesAndConfigWithNewSchemaOrChangedUtility (season, updatedMergedSchema, context, null, null, null, null, null, null, null, null, null);
			if (verFuncRes != null)
				return "Unable to remove stream '" + alStreamId + "' from season '" + seasonId.toString() + "': " + verFuncRes;

		} catch (GenerationException ge) {
			return "Failed to generate the data sample: " + ge.getMessage();
		} catch (JSONException jsne) {
			return jsne.getMessage();
		} catch (InterruptedException ie) {
			return "Failed to validate branches: " + ie.getMessage();
		} catch (ExecutionException ee) {
			return "Failed to validate branches: " + ee.getMessage();
		}

		boolean found = false;
		for (int i=0; i< streamsList.size(); i++) {
			if (streamsList.get(i).getUniqueId().toString().equals(alStreamId)) {
				streamsList.remove(i);
				found = true;
			}
		}

		if (found && !streamToRem.getResultsSchema().isEmpty()) {
			try {
				JSONObject streamsJson = season.getStreams().toJson(OutputJSONMode.ADMIN);
				season.getInputSchema().mergeSchema (streamsJson, false, null);
			} catch (GenerationException e) {
				logger.severe(e.getMessage());
				//ignore. should not happen since we are after validation
			} catch (JSONException je) {
				//ignore. should not happen since we are after validation
				logger.severe(je.getMessage());
			}
		}

		if (!found)
			return "Unable to remove stream " + alStreamId + " from season " + seasonId.toString() + ": The specified stream does not exist under this season.";

		return null;
	}


	public AirlockStreamsCollection duplicateForNewSeason (String minVersion, UUID newSeasonId, Map<String, AirlockStream> streamsDB) {
		AirlockStreamsCollection res = new AirlockStreamsCollection(newSeasonId);

		LinkedList<AirlockStream> newStreamsList = new LinkedList<AirlockStream>();
		for (int i=0; i<streamsList.size(); i++) {
			AirlockStream alStream = streamsList.get(i).duplicateForNewSeason(minVersion, newSeasonId);
			newStreamsList.add(alStream);
			streamsDB.put(alStream.getUniqueId().toString(), alStream);
		}

		res.setStreamsList(newStreamsList);
		
		res.setLastModified(lastModified);
		res.setBulkSize(bulkSize);
		res.setEnableHistoricalEvents(enableHistoricalEvents);
		res.setFilter(filter);
		res.setFileSizeKB(fileSizeKB);	
		res.setMaxHistorySizeKB(maxHistorySizeKB);
		res.setKeepHistoryOfLastNumberOfDays(keepHistoryOfLastNumberOfDays);
		res.setHistoryBufferSize(historyBufferSize);
		return res;
	}

	public String updateGlobalStreamsSettings (JSONObject updatedStreamsSettingsJSON, Season season) throws JSONException {
		boolean wasChanged = false;
		StringBuilder updateDetails = new StringBuilder();
		
		//filter
		String updatedFilter = updatedStreamsSettingsJSON.getString(Constants.JSON_FIELD_FILTER);
		if (!updatedFilter.equals(filter)) {
			updateDetails.append(" 'filter' changed from " + filter + " to " + updatedFilter + "\n");
			filter = updatedFilter;
			wasChanged = true;
		}
		
		//enableHistoricalEvents
		Boolean updatedEnableHistoricalEvents = updatedStreamsSettingsJSON.getBoolean(Constants.JSON_FIELD_ENABLE_HISTORICAL_EVENTS);
		if (!updatedEnableHistoricalEvents.equals(enableHistoricalEvents)) {
			updateDetails.append(" 'enableHistoricalEvents' changed from " + enableHistoricalEvents + " to " + updatedEnableHistoricalEvents + "\n");
			enableHistoricalEvents = updatedEnableHistoricalEvents;
			wasChanged = true;
		}
		
		//maxHistorySizeKB
		Integer updatedMaxHistorySizeKB = updatedStreamsSettingsJSON.getInt(Constants.JSON_FIELD_MAX_HISTORY_SIZE_KB);
		if (!updatedMaxHistorySizeKB.equals(maxHistorySizeKB)) {
			updateDetails.append(" 'maxHistorySizeKB' changed from " + maxHistorySizeKB + " to " + updatedMaxHistorySizeKB + "\n");
			maxHistorySizeKB = updatedMaxHistorySizeKB;
			wasChanged = true;
		}
		
		//keepHistoryOfLastNumberOfDays - null is a legal value
		if (updatedStreamsSettingsJSON.containsKey(Constants.JSON_FIELD_KEEP_HISTORY_OF_LAST_NUM_DAYS)) {
			if (updatedStreamsSettingsJSON.get(Constants.JSON_FIELD_KEEP_HISTORY_OF_LAST_NUM_DAYS) == null) {
				if (keepHistoryOfLastNumberOfDays!=null) {
					updateDetails.append(" 'keepHistoryOfLastNumberOfDays' changed from " + keepHistoryOfLastNumberOfDays + " to null\n");
					keepHistoryOfLastNumberOfDays = null;
					wasChanged = true;
				}				
			}
			else {
				Integer updatedKeepHistoryOfLastNumberOfDays = updatedStreamsSettingsJSON.getInt(Constants.JSON_FIELD_KEEP_HISTORY_OF_LAST_NUM_DAYS);
				if (!updatedKeepHistoryOfLastNumberOfDays.equals(keepHistoryOfLastNumberOfDays)) {
					updateDetails.append(" 'keepHistoryOfLastNumberOfDays' changed from " + keepHistoryOfLastNumberOfDays + " to " + updatedKeepHistoryOfLastNumberOfDays + "\n");
					keepHistoryOfLastNumberOfDays = updatedKeepHistoryOfLastNumberOfDays;
					wasChanged = true;
				}
			}
		}
		
		//historyBufferSize - null is a legal value
		if (updatedStreamsSettingsJSON.containsKey(Constants.JSON_FIELD_HISTORY_BUFFER_SIZE)) {
			if (updatedStreamsSettingsJSON.get(Constants.JSON_FIELD_HISTORY_BUFFER_SIZE) == null) {
				if (historyBufferSize!=null) {
					updateDetails.append(" 'historyBufferSize' changed from " + historyBufferSize + " to null\n");
					historyBufferSize = null;
					wasChanged = true;
				}				
			}
			else {
				Integer updatedHistoryBufferSize = updatedStreamsSettingsJSON.getInt(Constants.JSON_FIELD_HISTORY_BUFFER_SIZE);
				if (!updatedHistoryBufferSize.equals(historyBufferSize)) {
					updateDetails.append(" 'historyBufferSize' changed from " + historyBufferSize + " to " + updatedHistoryBufferSize + "\n");
					historyBufferSize = updatedHistoryBufferSize;
					wasChanged = true;
				}
			}
		}
		
		//bulkSize
		Integer updatedBulkSize = updatedStreamsSettingsJSON.getInt(Constants.JSON_FIELD_BULK_SIZE);
		if (!updatedBulkSize.equals(bulkSize)) {
			updateDetails.append(" 'bulkSize' changed from " + bulkSize + " to " + updatedBulkSize + "\n");
			bulkSize = updatedBulkSize;
			wasChanged = true;
		}
		
		//fileSizeKB
		Integer updatedFileSizeKB = updatedStreamsSettingsJSON.getInt(Constants.JSON_FIELD_FILE_SIZE_KB);
		if (!updatedFileSizeKB.equals(fileSizeKB)) {
			updateDetails.append(" 'fileSizeKB' changed from " + fileSizeKB + " to " + updatedFileSizeKB + "\n");
			fileSizeKB = updatedFileSizeKB;
			wasChanged = true;
		}

		if (wasChanged) {
			lastModified = new Date();
		}
		
		return updateDetails.toString();
	}
	
	//always during update
	public ValidationResults validateGlobalStreamsSettingsJSON(JSONObject globalStreamSettingsObj, ServletContext context, String seasonId) {
		@SuppressWarnings("unchecked")
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);
		Season season = seasonsDB.get(seasonId);
		//always update
		try {
			//modification date must appear
			if (!globalStreamSettingsObj.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) || globalStreamSettingsObj.get(Constants.JSON_FIELD_LAST_MODIFIED)==null) {
				return new ValidationResults(Strings.lastModifiedIsMissing, Status.BAD_REQUEST);
			}
			
			//verify that given modification date is not older than current modification date
			long givenModoficationDate = globalStreamSettingsObj.getLong(Constants.JSON_FIELD_LAST_MODIFIED);  //verify that legal long
			Date givenDate = new Date(givenModoficationDate);
			if (givenDate.before(lastModified)) {
				return new ValidationResults("The global streams settings were changed by another user. Refresh your browser and try again.", Status.CONFLICT);			
			}
			
			//verify that JSON does not contain streams array
			if (globalStreamSettingsObj.containsKey(Constants.JSON_FIELD_STREAMS)) {
				return new ValidationResults("The streams list should not be specified while updating the global streams settings.", Status.BAD_REQUEST);
			}
			
			//enableHistoricalEvents
			if (!globalStreamSettingsObj.containsKey(Constants.JSON_FIELD_ENABLE_HISTORICAL_EVENTS) || globalStreamSettingsObj.get(Constants.JSON_FIELD_ENABLE_HISTORICAL_EVENTS) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_ENABLE_HISTORICAL_EVENTS), Status.BAD_REQUEST);					
			}

			Boolean enableHistory = globalStreamSettingsObj.getBoolean(Constants.JSON_FIELD_ENABLE_HISTORICAL_EVENTS); //validate that is boolean value
			if (!enableHistory) {
				//go over the streams and verify that there is no stream that keeps history
				for (AirlockStream s:streamsList) {
					if (s.getOperateOnHistoricalEvents()) {
						return new ValidationResults("Cannot turn down global history processing because stream " + s.getName() +  " operates on historical events.", Status.BAD_REQUEST);
					}
				}
			}
			
			//maxHistorySizeKB
			if (!globalStreamSettingsObj.containsKey(Constants.JSON_FIELD_MAX_HISTORY_SIZE_KB) || globalStreamSettingsObj.get(Constants.JSON_FIELD_MAX_HISTORY_SIZE_KB) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_MAX_HISTORY_SIZE_KB), Status.BAD_REQUEST);					
			}

			int mhs = globalStreamSettingsObj.getInt(Constants.JSON_FIELD_MAX_HISTORY_SIZE_KB);
			if (mhs<=0) {
				return new ValidationResults("The " + Constants.JSON_FIELD_MAX_HISTORY_SIZE_KB + " value is illegal. Should be a positive integer.", Status.BAD_REQUEST);
			}
			
			//bulkSize
			if (!globalStreamSettingsObj.containsKey(Constants.JSON_FIELD_BULK_SIZE) || globalStreamSettingsObj.get(Constants.JSON_FIELD_BULK_SIZE) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_BULK_SIZE), Status.BAD_REQUEST);					
			}

			int bs = globalStreamSettingsObj.getInt(Constants.JSON_FIELD_BULK_SIZE);
			if (bs<=0) {
				return new ValidationResults("The " + Constants.JSON_FIELD_BULK_SIZE + " value is illegal. Should be a positive integer.", Status.BAD_REQUEST);
			}
			
			//fileSizeKB
			if (!globalStreamSettingsObj.containsKey(Constants.JSON_FIELD_FILE_SIZE_KB) || globalStreamSettingsObj.get(Constants.JSON_FIELD_FILE_SIZE_KB) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_FILE_SIZE_KB), Status.BAD_REQUEST);					
			}

			int fs = globalStreamSettingsObj.getInt(Constants.JSON_FIELD_FILE_SIZE_KB);
			if (fs<=0) {
				return new ValidationResults("The " + Constants.JSON_FIELD_FILE_SIZE_KB + " value is illegal. Should be a positive integer.", Status.BAD_REQUEST);
			}
			
			//keepHistoryOfLastNumberOfDays - can be null
			if (!globalStreamSettingsObj.containsKey(Constants.JSON_FIELD_KEEP_HISTORY_OF_LAST_NUM_DAYS)) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_KEEP_HISTORY_OF_LAST_NUM_DAYS), Status.BAD_REQUEST);					
			}
			if (globalStreamSettingsObj.get(Constants.JSON_FIELD_KEEP_HISTORY_OF_LAST_NUM_DAYS)!=null) {
				int khd = globalStreamSettingsObj.getInt(Constants.JSON_FIELD_KEEP_HISTORY_OF_LAST_NUM_DAYS);
				if (khd<=0) {
					return new ValidationResults("The " + Constants.JSON_FIELD_KEEP_HISTORY_OF_LAST_NUM_DAYS + " value is illegal. Should be a positive integer.", Status.BAD_REQUEST);
				}
				//go over the streams and verify that there is no stream with smaller keepHistoryOfLastNumberOfDays
				for (AirlockStream s:streamsList)
				{
					if (s.getProcessEventsOfLastNumberOfDays()!=null && s.getProcessEventsOfLastNumberOfDays()<khd) {
						return new ValidationResults("Cannot set " + Constants.JSON_FIELD_KEEP_HISTORY_OF_LAST_NUM_DAYS + " value to " + khd + " the " + Constants.JSON_FIELD_KEEP_HISTORY_OF_LAST_NUM_DAYS + " of stream " + s.getName() +  " is greater.", Status.BAD_REQUEST);
					}
				}
			}
			
			//historyBufferSize - can be null
			if (!globalStreamSettingsObj.containsKey(Constants.JSON_FIELD_HISTORY_BUFFER_SIZE)) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_HISTORY_BUFFER_SIZE), Status.BAD_REQUEST);					
			}
			if (globalStreamSettingsObj.get(Constants.JSON_FIELD_HISTORY_BUFFER_SIZE)!=null) {
				int hbs = globalStreamSettingsObj.getInt(Constants.JSON_FIELD_HISTORY_BUFFER_SIZE);
				if (hbs<=0) {
					return new ValidationResults("The " + Constants.JSON_FIELD_HISTORY_BUFFER_SIZE + " value is illegal. Should be a positive integer.", Status.BAD_REQUEST);
				}
			}
			
			//filter
			if (!globalStreamSettingsObj.containsKey(Constants.JSON_FIELD_FILTER) || globalStreamSettingsObj.get(Constants.JSON_FIELD_FILTER) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_FILTER), Status.BAD_REQUEST);					
			}
			String filter = globalStreamSettingsObj.getString(Constants.JSON_FIELD_FILTER);
			if (!filter.isEmpty()) { //empty filter is legal
				ValidationCache validationCache = new ValidationCache();
				validationCache.setStreamsCache();
				
				JSONArray allEvents = season.getStreamsEvents().getEvents();
				StreamsScriptInvoker streamsScriptInvoker = null;			
				try {
					ValidationCache.Info info = validationCache.getInfo(context, season, Stage.DEVELOPMENT, "");
					streamsScriptInvoker = (StreamsScriptInvoker)info.streamsInvoker;
					VerifyStream.validateStreamFilter(streamsScriptInvoker, filter, allEvents);
				} catch (Exception e) {
					return new ValidationResults("Illegal filter: " + e.getMessage(), Status.BAD_REQUEST);
				}
			}
		} catch (JSONException jsne) {
			return new ValidationResults(jsne.getMessage(), Status.BAD_REQUEST);
		}
		
		return null;
	}
	
	//return null on success, errorString on error	
	public String validateStreamsWithChangedOrDeletedUtility(ServletContext context, Season season, String removedUtilityId, String newUtilityCode, Stage newUtilStage) {
		String id = (removedUtilityId==null) ? null: removedUtilityId.toString();
		ValidationCache validationCache = new ValidationCache(newUtilityCode, id, newUtilStage); // cache with changed utility
		validationCache.setStreamsCache();

		for (AirlockStream stream:season.getStreams().getStreamsList()) {
			JSONArray allEvents = season.getStreamsEvents().getEvents();
			StreamsScriptInvoker streamsScriptInvoker = null;			

			//validate filter
			try {
				ValidationCache.Info info = validationCache.getInfo(context, season, stream.getStage(), "");
				streamsScriptInvoker = (StreamsScriptInvoker)info.streamsInvoker;
				VerifyStream.validateStreamFilter(streamsScriptInvoker, stream.getFilter(), allEvents);
			} catch (Exception e) {
				return "Illegal filter: " + e.getMessage();
			}

			//validate processor
			try {
				VerifyStream.evaluateFilterAndProcessor(streamsScriptInvoker, stream.getFilter(), stream.getProcessor(), allEvents);
			} catch (InvokerException|JSONException e) {
				return "Illegal processor in stream " + stream.getName() + " : " + e.getMessage();
			}
		}
		return null;		
	}

}
