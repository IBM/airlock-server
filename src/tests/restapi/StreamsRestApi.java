package tests.restapi;

import java.io.IOException;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import tests.com.ibm.qautils.FileUtils;
import tests.com.ibm.qautils.RestClientUtils;

public class StreamsRestApi {
	protected static String m_url ;
	
	public void setURL (String url){
		m_url = url;
	}
	
	//streams
	
	public String getAllStreams (String seasonId, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/products/seasons/" + seasonId + "/streams", sessionToken);
		return res.message;
	}
	
	
	public String createStream (String seasonId, String stream, String sessionToken) throws IOException{

		RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/products/seasons/" + seasonId + "/streams", stream, sessionToken);
		return parseId(res.message);
	}
	
	public String getStream (String streamId, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/products/seasons/streams/" + streamId, sessionToken);
		 return res.message;
	}
	
	public String updateStream(String streamId, String stream, String sessionToken) throws IOException{

		RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/products/seasons/streams/" + streamId, stream, sessionToken);
		return parseId(res.message);
	}
	
	public int deleteStream(String streamId, String sessionToken) throws Exception{

		RestClientUtils.RestCallResults res = RestClientUtils.sendDelete(m_url+"/products/seasons/streams/" + streamId, sessionToken);
		return res.code;
	}
	
	//stream events
	//GET /admin/products/seasons/{season-id}/streams/events
	public String getStreamEvent (String seasonId, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/products/seasons/" + seasonId + "/streams/events", sessionToken);
		 return res.message;
	}
	
	public String updateStreamEvent(String seasonId, String streamEvent, String sessionToken) throws IOException{

		RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/products/seasons/" + seasonId + "/streams/events", streamEvent, sessionToken);
		return res.message;
	}

	
	public String getStreamEventFields (String seasonId, String filter, String sessionToken) throws IOException{

		RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/products/seasons/" + seasonId + "/streams/eventsfields", filter, sessionToken);
		return res.message;
	}
	
	//POST /admin/products/seasons/{season-id}/streams/eventsfieldsbyfilter
	public String getStreamEventsByFilter (String seasonId, String filter, String stage, String sessionToken) throws IOException{

		RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/products/seasons/" + seasonId + "/streams/eventsfieldsbyfilter?stage="+stage, filter, sessionToken);
		return res.message;
	}
	
	private String parseId(String input){
		String idString = "";
		JSONObject response = null;
		try {
			response = new JSONObject(input);
			
			if (response.containsKey("error")){
				idString = input;
			} else {
				idString = (String)response.get("uniqueId");
			}
		} catch (JSONException e) {
			idString = "Invalid response: " + response;
		}

		return idString;
	}
	
	public JSONObject populateStreamEvents(String seasonId, String path, String sessionToken) throws Exception{
		String events = getStreamEvent(seasonId, sessionToken);
		JSONObject json = new JSONObject(events);
		String content = FileUtils.fileToString(path, "UTF-8", false);
		JSONArray eventsList = new JSONObject(content).getJSONArray("events");
		json.put("events", eventsList);
		return json;
		
	}

}
