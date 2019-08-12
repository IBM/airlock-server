package com.ibm.airlock.admin.streams;

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
import com.ibm.airlock.Constants.OutputJSONMode;
import com.ibm.airlock.Constants.Stage;
import com.ibm.airlock.admin.GenerationException;
import com.ibm.airlock.admin.InputSchema;
import com.ibm.airlock.admin.Season;
import com.ibm.airlock.admin.ValidationCache;
import com.ibm.airlock.engine.StreamsScriptInvoker;
import com.ibm.airlock.engine.VerifyStream;
import com.ibm.airlock.engine.ScriptInvoker.InvokerException;

public class AirlockStreamsCollection {
	public static final Logger logger = Logger.getLogger(AirlockStreamsCollection.class.getName());

	private LinkedList<AirlockStream> streamsList = new LinkedList<AirlockStream>();
	private UUID seasonId = null; 

	public AirlockStreamsCollection(UUID seasonId) {
		this.seasonId = seasonId;
	}

	public LinkedList<AirlockStream> getStreamsList() {
		return streamsList;
	}

	public void setStreamsList(LinkedList<AirlockStream> streamsList) {
		this.streamsList = streamsList;
	}

	public JSONObject toJson(OutputJSONMode mode) throws JSONException {
		JSONObject res = new JSONObject();
		JSONArray streamsArr = new JSONArray();
		for (AirlockStream alStream : streamsList) {
			if (mode != null && alStream.getStage() == Stage.DEVELOPMENT && mode.equals(OutputJSONMode.RUNTIME_PRODUCTION))
				continue; //don't return stream in development when runtime_prod is requested

			JSONObject auObj = alStream.toJson(mode);
			streamsArr.add(auObj);				
		}
		res.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, seasonId==null?null:seasonId.toString());
		res.put(Constants.JSON_FIELD_STREAMS, streamsArr);

		return res;
	}

	//called in server init stage - reading the streams from files in s3
	public void fromJSON(JSONArray input, Map<String, AirlockStream> streamsDB) throws JSONException {
		for (int i=0; i<input.length(); i++ ) {
			JSONObject alStreamJSON = input.getJSONObject(i);
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


	public LinkedList<AirlockStream> duplicateForNewSeason (String minVersion, UUID newSeasonId, Map<String, AirlockStream> streamsDB) {
		LinkedList<AirlockStream> res = new LinkedList<AirlockStream>();

		for (int i=0; i<streamsList.size(); i++) {
			AirlockStream alStream = streamsList.get(i).duplicateForNewSeason(minVersion, newSeasonId);
			res.add(alStream);
			streamsDB.put(alStream.getUniqueId().toString(), alStream);
		}

		return res;
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
				return "Illegal processor: " + e.getMessage();
			}
		}
		return null;		
	}

}
