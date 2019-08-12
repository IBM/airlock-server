package com.ibm.airlock.admin;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

import javax.ws.rs.core.Response.Status;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Strings;

public class AirlockServers {
	public class AirlockServer{
		String displayName;
		String url;
		String cdnOverride; //optional 
		
		
		public String getDisplayName() {
			return displayName;
		}

		public void setDisplayName(String displayName) {
			this.displayName = displayName;
		}

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public String getCdnOverride() {
			return cdnOverride;
		}

		public void setCdnOverride(String cdnOverride) {
			this.cdnOverride = cdnOverride;
		}

		public void fromJSON (JSONObject input) throws JSONException {
			//after validate - i know that all fields appears, not empty and the right object
		
			displayName = (String)input.get(Constants.JSON_FIELD_DISPLAY_NAME);
			url = (String)input.get(Constants.JSON_FIELD_URL);
			if (input.containsKey(Constants.JSON_FIELD_CDN_OVERRIDE) && input.get(Constants.JSON_FIELD_CDN_OVERRIDE)!=null)
				cdnOverride = (String)input.get(Constants.JSON_FIELD_CDN_OVERRIDE);
		}
		
		public JSONObject toJSON() throws JSONException {
			JSONObject res = new JSONObject();
			
			res.put(Constants.JSON_FIELD_DISPLAY_NAME, displayName);
			res.put(Constants.JSON_FIELD_URL, url);
			res.put(Constants.JSON_FIELD_CDN_OVERRIDE, cdnOverride);
			
			return res;			
		}
		
		//return null if valid, ValidationResults otherwise
		public ValidationResults validateAirlockServerJSON(JSONObject alServerJSON) throws JSONException {
			//displayName
			if (!alServerJSON.containsKey(Constants.JSON_FIELD_DISPLAY_NAME) || alServerJSON.get(Constants.JSON_FIELD_DISPLAY_NAME) == null || alServerJSON.getString(Constants.JSON_FIELD_DISPLAY_NAME).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_DISPLAY_NAME), Status.BAD_REQUEST);
			}
			
			//url
			if (!alServerJSON.containsKey(Constants.JSON_FIELD_URL) || alServerJSON.get(Constants.JSON_FIELD_URL) == null || alServerJSON.getString(Constants.JSON_FIELD_URL).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_URL), Status.BAD_REQUEST);
			}
			
			//cdnOverride is optional 
			return null;
		}
		
	}
	
	private Date lastModified = null;
	private LinkedList<AirlockServer> servers = new LinkedList<AirlockServer>(); 
	private String defaultServer = null;
	
	public Date getLastModified() {
		return lastModified;
	}

	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

	
	public LinkedList<AirlockServer> getServers() {
		return servers;
	}

	public void setServers(LinkedList<AirlockServer> servers) {
		this.servers = servers;
	}

	public String getDefaultServer() {
		return defaultServer;
	}

	public void setDefaultServer(String defaultServer) {
		this.defaultServer = defaultServer;
	}

	public JSONObject toJson(boolean withLastModify) throws JSONException {
		JSONObject res = new JSONObject();
		if (withLastModify)
			res.put(Constants.JSON_FIELD_LAST_MODIFIED, lastModified.getTime());
		JSONArray serversArr = new JSONArray();
		for (int i=0; i<servers.size(); i++) {
			serversArr.add(servers.get(i).toJSON());			
		}
		
		res.put(Constants.JSON_FIELD_SERVERS, serversArr);
		res.put(Constants.JSON_FIELD_DEFAULT_SERVER, defaultServer);
		return res;
	}
	
	public void fromJSON (JSONObject input) throws JSONException {		
		if (input.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) && input.get(Constants.JSON_FIELD_LAST_MODIFIED)!=null) {
			long timeInMS = (Long)input.get(Constants.JSON_FIELD_LAST_MODIFIED);
			lastModified = new Date(timeInMS);
		}  else {
			lastModified = new Date();
		}
				
		defaultServer = (String)input.get(Constants.JSON_FIELD_DEFAULT_SERVER);
				
		servers.clear();
		if (input.containsKey(Constants.JSON_FIELD_SERVERS) && input.get(Constants.JSON_FIELD_SERVERS)!=null) {
			JSONArray serversJSONArr = input.getJSONArray(Constants.JSON_FIELD_SERVERS);
			if (serversJSONArr != null && serversJSONArr.size()>0) {
				for (int i=0; i<serversJSONArr.size(); i++) {
					AirlockServer alServer = new AirlockServer();
					alServer.fromJSON(serversJSONArr.getJSONObject(i));
					servers.add(alServer);															
				}
			}
		}								
	}
	
	//return null if valid, ValidationResults otherwise
	public ValidationResults validateAirlockServersJSON(JSONObject alServersJson) {		
		try {
			if (!alServersJson.containsKey(Constants.JSON_FIELD_SERVERS) || alServersJson.get(Constants.JSON_FIELD_SERVERS)==null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_SERVERS), Status.BAD_REQUEST);
			}
			
			JSONArray serversArr = alServersJson.getJSONArray(Constants.JSON_FIELD_SERVERS); //validate that is array value
			
			//modification date must appear
			if (!alServersJson.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) || alServersJson.get(Constants.JSON_FIELD_LAST_MODIFIED)==null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_LAST_MODIFIED), Status.BAD_REQUEST);
			}				
			
			//verify that given modification date is not older that current modification date
			long givenModoficationDate = alServersJson.getLong(Constants.JSON_FIELD_LAST_MODIFIED);  //verify that legal long
			Date givenDate = new Date(givenModoficationDate);
			if (givenDate.before(lastModified)) {
				return new ValidationResults("The Airlock servers JSON was changed by another user. Refresh your browser and try again.", Status.CONFLICT);			
			}
		
			//defaultServer
			if (!alServersJson.containsKey(Constants.JSON_FIELD_DEFAULT_SERVER) || alServersJson.get(Constants.JSON_FIELD_DEFAULT_SERVER) == null || alServersJson.getString(Constants.JSON_FIELD_DEFAULT_SERVER).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_DEFAULT_SERVER), Status.BAD_REQUEST);
			}
			
			//verify that there are no duplications in the user groups
			HashMap<String, Integer> tmpServersMap = new HashMap<String, Integer>(); 
			for(int j = 0; j < serversArr.length(); j++){
			    JSONObject serverJSON = serversArr.getJSONObject(j);
			    AirlockServer alServer = new AirlockServer();
			    ValidationResults res = alServer.validateAirlockServerJSON(serverJSON);
			    if (res != null)
			    	return res;
			    String serverDisplayName = serverJSON.getString(Constants.JSON_FIELD_DISPLAY_NAME);
			    if (tmpServersMap.containsKey(serverDisplayName)) {
			    	return new ValidationResults("Airlock server '" + serverDisplayName + "' apears more than once in the airlock servers JSON.", Status.BAD_REQUEST);
			    }
			    tmpServersMap.put(serverJSON.getString(Constants.JSON_FIELD_DISPLAY_NAME), 1);
			}
			
			if (tmpServersMap.get(alServersJson.getString(Constants.JSON_FIELD_DEFAULT_SERVER)) == null) {
				return new ValidationResults("The defaultServer was not found in the list of servers.", Status.BAD_REQUEST);
			}
		} catch (JSONException jsne) {
			return new ValidationResults(jsne.getMessage(), Status.BAD_REQUEST);
		}
		catch (ClassCastException cce) {
			return new ValidationResults("Illegal Airlock servers JSON: " + cce.getMessage(), Status.BAD_REQUEST);
		}
		
		return null;
	}
	
	

}
