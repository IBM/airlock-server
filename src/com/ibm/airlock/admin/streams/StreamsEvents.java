package com.ibm.airlock.admin.streams;

import java.util.Date;
import java.util.UUID;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response.Status;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Constants.Stage;
import com.ibm.airlock.Strings;
import com.ibm.airlock.admin.GenerationException;
import com.ibm.airlock.admin.Season;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.ValidationCache;
import com.ibm.airlock.admin.ValidationResults;
import com.ibm.airlock.engine.ScriptInvoker.InvokerException;
import com.ibm.airlock.engine.StreamsScriptInvoker;
import com.ibm.airlock.engine.VerifyStream;

public class StreamsEvents {
	public static final Logger logger = Logger.getLogger(StreamsEvents.class.getName());
	
	private JSONArray events = new JSONArray(); //array of jsonObjects
	private Date lastModified = null;
	private UUID seasonId = null;
	
	public StreamsEvents(UUID seasonId) {
		this.seasonId = seasonId;
		lastModified = new Date();
	}
	
	public Date getLastModified() {
		return lastModified;
	}

	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}
	
	public JSONArray getEvents() {
		return events;
	}

	public void setEvents(JSONArray events) {
		this.events = events;
	}
	
	public JSONObject toJson() throws JSONException {
		JSONObject res = new JSONObject();
		res.put(Constants.JSON_FIELD_LAST_MODIFIED, lastModified == null?null:lastModified.getTime());
		res.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, seasonId==null?null:seasonId.toString());		
		res.put(Constants.JSON_FIELD_EVENTS, events);
		
		return res;
	}
	
	public void fromJSON (JSONObject input) throws JSONException {		
		if (input.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) && input.get(Constants.JSON_FIELD_LAST_MODIFIED)!=null) { 
			long timeInMS = (Long)input.get(Constants.JSON_FIELD_LAST_MODIFIED);
			lastModified = new Date(timeInMS);
		}  else {
			lastModified = new Date();
		}
				
		events.clear();
		events =  input.getJSONArray(Constants.JSON_FIELD_EVENTS);						
	}
	
	//return null if valid, ValidationResults otherwise
	public ValidationResults validateStreamEventsJSON(JSONObject streamEventsJson) {		
		try {
			//season id
			if (!streamEventsJson.containsKey(Constants.JSON_FEATURE_FIELD_SEASON_ID) || streamEventsJson.get(Constants.JSON_FEATURE_FIELD_SEASON_ID)==null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_SEASON_ID), Status.BAD_REQUEST);
			}
			
			//season id must exists and not be changed
			String seasonIdStr = streamEventsJson.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID);
			if (!seasonIdStr.equals(seasonId.toString())) {
				return new ValidationResults(String.format(Strings.fieldCannotBeChangedDuringUpdate, Constants.JSON_FEATURE_FIELD_SEASON_ID), Status.BAD_REQUEST);
			}
			
			if (!streamEventsJson.containsKey(Constants.JSON_FIELD_EVENTS) || streamEventsJson.get(Constants.JSON_FIELD_EVENTS)==null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_EVENTS), Status.BAD_REQUEST);
			}
						
			JSONArray eventsArr = streamEventsJson.getJSONArray(Constants.JSON_FIELD_EVENTS); //validate that is array of JSONObjects
			//Set<String> existingEventNames = new TreeSet<String>();
			for (int i=0; i<eventsArr.size(); i++) {
				JSONObject streamsEvent = eventsArr.getJSONObject(i); //validate that each item is json object
				if (!streamsEvent.containsKey(Constants.JSON_FIELD_NAME) || streamsEvent.get(Constants.JSON_FIELD_NAME)==null) {
					return new ValidationResults("The 'name' field is missing in one of the streams events.", Status.BAD_REQUEST);
				}
				
				String streamsEventName = streamsEvent.getString(Constants.JSON_FIELD_NAME);
				if (StreamsScriptInvoker.invalidName(streamsEventName)) {					
					return new ValidationResults("Invalid event name: " + streamsEventName, Status.BAD_REQUEST);
				}
				
				if (!streamsEvent.containsKey(Constants.JSON_FIELD_EVENT_DATA) || streamsEvent.get(Constants.JSON_FIELD_EVENT_DATA)==null) {
					return new ValidationResults("The 'eventData' field is missing in one of the streams events.", Status.BAD_REQUEST);
				}
				JSONObject eventData = streamsEvent.getJSONObject(Constants.JSON_FIELD_EVENT_DATA); //validate that is legal json
			}
			
			
			//modification date must appear
			if (!streamEventsJson.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) || streamEventsJson.get(Constants.JSON_FIELD_LAST_MODIFIED)==null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_LAST_MODIFIED), Status.BAD_REQUEST);
			}				
			
			//verify that given modification date is not older that current modification date
			long givenModoficationDate = streamEventsJson.getLong(Constants.JSON_FIELD_LAST_MODIFIED);  //verify that legal long
			Date givenDate = new Date(givenModoficationDate);
			if (givenDate.before(lastModified)) {
				return new ValidationResults("StreamEvents were changed by another user. Get the StreamEvents again and resubmit the request.", Status.CONFLICT);			
			}
								
		} catch (JSONException jsne) {
			return new ValidationResults(jsne.getMessage(), Status.BAD_REQUEST);
		}
		catch (ClassCastException cce) {
			return new ValidationResults("Illegal streamEvents JSON: " + cce.getMessage(), Status.BAD_REQUEST);
		}
		return null;
	}

	public StreamsEvents duplicateForNewSeason(UUID newSeasonId){
		StreamsEvents res = new StreamsEvents(newSeasonId);
		
		try {
			res.setEvents((JSONArray)Utilities.cloneJson(events, true));
		} catch (JSONException e) {
			//should not happen
			logger.severe("Fail duplicate stream events for new season:" + e.getMessage());
		}
		
		return res;
	}

	public JSONObject getEventsFieldsByFilter (String filter, ServletContext context, Season season, Stage stage) throws JSONException, GenerationException, InvokerException {		
		ValidationCache validationCache = new ValidationCache();
		validationCache.setStreamsCache();

		StreamsScriptInvoker streamsScriptInvoker = null;			
		ValidationCache.Info info = validationCache.getInfo(context, season, stage, "");
		streamsScriptInvoker = (StreamsScriptInvoker)info.streamsInvoker;
		JSONArray filteredArr = VerifyStream.evaluateStreamFilter(streamsScriptInvoker, filter, events);
		return VerifyStream.getIntelliSenseData(filteredArr, false);
				
	}

	public JSONObject getEventsFields(ServletContext context, Season season) throws JSONException {
		return VerifyStream.getIntelliSenseData(events, true);
	}
	
	
}
