package tests.restapi;


import java.io.IOException;







import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import tests.com.ibm.qautils.RestClientUtils;

public class StringsRestApi {
	protected static String m_url ;
	
	public void setURL (String url){
		m_url = url;
	}
	
	// getAllStrings GET /translations/seasons/{season-id}/strings	
	public String getAllStrings(String seasonID,String mode, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res =RestClientUtils.sendGet(m_url+"/seasons/" + seasonID + "/strings?mode=" + mode,sessionToken);
		String response = res.message;
		return response;
	}
	
	public String getAllStrings(String seasonID,String mode, String[] idsArray, String sessionToken) throws Exception{
		String ids = idsToString(idsArray);
		RestClientUtils.RestCallResults res =RestClientUtils.sendGet(m_url+"/seasons/" + seasonID + "/strings" +  ids + "&mode=" + mode ,sessionToken);
		String response = res.message;
		return response;
	}

	public String getAllStrings(String seasonID, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res =RestClientUtils.sendGet(m_url+"/seasons/" + seasonID + "/strings",sessionToken);
		String response = res.message;
		return response;
	}
	
	//addString POST /translations/seasons/{season-id}/strings
	public String addString(String seasonID, String str, String sessionToken) throws Exception{	
		RestClientUtils.RestCallResults res =RestClientUtils.sendPost(m_url+"/seasons/" + seasonID + "/strings", str, sessionToken);
		String response = res.message;
		
		String stringId = parseStringId(response);
		return stringId;
	}
	
	public String addString(String seasonID, String str, String mode, String sessionToken) throws Exception{	
		RestClientUtils.RestCallResults res =RestClientUtils.sendPost(m_url+"/seasons/" + seasonID + "/strings?mode=" + mode, str, sessionToken);
		String response = res.message;
		
		String stringId = parseStringId(response);
		return stringId;
	}
	
	//getString GET /translations/seasons/strings/{string-id}
	public String getString(String stringID, String sessionToken) throws Exception{	
		RestClientUtils.RestCallResults res =RestClientUtils.sendGet(m_url+"/seasons/strings/" + stringID, sessionToken);
		String response = res.message;
		return response;
	}
	//getString GET /translations/seasons/strings/{string-id}
	public String getString(String stringID,String mode, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res =RestClientUtils.sendGet(m_url+"/seasons/strings/" + stringID + "?mode=" + mode, sessionToken);
		String response = res.message;
		return response;
	}
	
	//updateString PUT /translations/seasons/strings/{string-id}
	public String updateString(String stringID, String str, String sessionToken) throws IOException{
		RestClientUtils.RestCallResults res =RestClientUtils.sendPut(m_url+"/seasons/strings/" + stringID, str, sessionToken);
		String response = res.message;
		String stringId = parseStringId(response);
		return stringId;
	}
	
	//updateString PUT /translations/seasons/strings/{string-id}
	public String updateString(String stringID, String str, String mode, String sessionToken) throws IOException{
		//http://9.148.48.79:4040/airlock/api/translations/seasons/strings/4d8cbfab-df1b-4b1c-9539-5f9812dea05e?mode=VALIDATE
		RestClientUtils.RestCallResults res =RestClientUtils.sendPut(m_url+"/seasons/strings/" + stringID + "?mode=" + mode, str, sessionToken);
		return res.message;

	}
	
	//deleteString DELETE /translations/seasons/strings/{string-id}
	public int deleteString(String stringID, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendDelete(m_url+"/seasons/strings/" + stringID, sessionToken);
		int response = res.code;
		return response;
	}
	
	//get supported locales (supported languages) /translations/{season-id}/supportedlocales
	public String getSupportedLocales(String seasonID, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res =RestClientUtils.sendGet(m_url + "/" + seasonID + "/supportedlocales", sessionToken);
		return res.message;	//return json as string containing supportedLanguages & seasonId
	}
	
	
	//GET /translations/seasons/{string-id}/stringUsage
	public String getStringUsage(String stringId, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res =RestClientUtils.sendGet(m_url + "/seasons/" + stringId + "/stringusage", sessionToken);
		return res.message;	
		
	}
	
	//GET /translations/seasons/{season-id}/strings/unused
	public String getUnusageStrings(String seasonaId, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res =RestClientUtils.sendGet(m_url + "/seasons/" + seasonaId + "/strings/unused", sessionToken);
		return res.message;	
	}
	
	//PUT /translations/seasons/copystrings/{dest-season-id}
	public String copyStrings(String[] idsArray, String targetSeasonId, String mode, boolean overwrite, String sessionToken) throws IOException{
		String ids = idsToString(idsArray);
		if (sessionToken == null)
			sessionToken = "";
		RestClientUtils.RestCallResults res =RestClientUtils.sendPut(m_url+"/seasons/copystrings/" + targetSeasonId + ids + "&mode=" + mode + "&overwrite=" + overwrite, "", sessionToken);
		return res.message;
	}
	
	//PUT /translations/seasons/{season-id}/importstrings
	public String importStrings(String content, String targetSeasonId, String mode, boolean overwrite, String sessionToken) throws IOException{
		if (sessionToken == null)
			sessionToken = "";
		RestClientUtils.RestCallResults res =RestClientUtils.sendPut(m_url+"/seasons/" + targetSeasonId + "/importstrings?mode=" + mode + "&overwrite=" + overwrite, content, sessionToken);
		return res.message;
	}
	
	private String parseStringId(String response){
		String stringID = "";
		JSONObject json = null;
		try {
			json = new JSONObject(response);
			if (json.containsKey("error")){
				stringID = response;
			} else {
				stringID = (String)json.get("uniqueId");
			}
		} catch (JSONException e) {
			stringID = "Invalid response: " + response;				
		}

		return stringID;
	}
	
	public String idsToString(String[] ids){
		if(ids.length == 0){
			return "";
		}
		StringBuilder idsString = new StringBuilder();
		idsString.append("?ids=");
		for(int i = 0; i<ids.length-1; ++i){
			idsString.append(ids[i]);
			idsString.append("&ids=");
		}
		idsString.append(ids[ids.length-1]);
		return idsString.toString();
	}

	
}
