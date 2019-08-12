package tests.restapi;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Properties;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;

import tests.com.ibm.qautils.RestClientUtils;

public class UtilitiesRestApi {
	protected static String m_url ;
	public static String MAIN_UTILITY = "MAIN_UTILITY";
	public static String STREAM_UTILITY = "STREAMS_UTILITY";
	
	public void setURL (String url){
		m_url = url;
	}
	


	
	public String getAllUtilites(String seasonID, String sessionToken, String stage){
		
		String utilites = "";
		try {
			String reqUrl = m_url+"/products/seasons/" + seasonID + "/utilities";
			if (stage!=null) {
				reqUrl+="?";
				if (stage!=null)
					reqUrl=reqUrl+"stage="+stage;
				
			}

			
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(reqUrl, sessionToken);
			utilites = res.message;
			if (res.code!=200) {
				Assert.fail("Error when trying  to get all utilities. Message: "+ res.message) ;
			}
		} catch (Exception e) {
			Assert.fail("An exception was thrown when trying  to get all utilities. Message: "+ e.getLocalizedMessage()) ;
		}
		return utilites;
	}
	/*
	public String addUtility(String seasonID, String utilityPropStr, String sessionToken) throws IOException{
		Properties utilProps = new Properties();
		utilProps.load(new StringReader(utilityPropStr));
		return addUtility(seasonID, utilProps, sessionToken);		
	}*/
	
	public String addUtility(String seasonID, JSONObject utilityJson, String sessionToken) throws IOException, JSONException{
		Properties utilProps = new Properties();
		//utilProps.put ("minAppVersion", utilityJson.getString("minAppVersion"));
		String name = "";
		if (utilityJson.containsKey("name"))
			name = utilityJson.getString("name")+RandomStringUtils.randomAlphabetic(4);
		else
			name = RandomStringUtils.randomAlphabetic(4);
		
		utilProps.put ("stage", utilityJson.getString("stage"));
		utilProps.put ("name", name);
		String utility = utilityJson.getString("utility");
		utilProps.put ("utility", StringEscapeUtils.unescapeJava(utility));
		utilProps.put ("lastModified", utilityJson.getString("lastModified"));
		return addUtility(seasonID, utilProps, sessionToken);		
	}
	
	public String addUtility(String seasonID, Properties utilityProps, String sessionToken) throws IOException{
		//String minAppVersion = utilityProps.getProperty("minAppVersion");
		String stage = utilityProps.getProperty("stage");
		String utility = utilityProps.getProperty("utility");
		String name = "";
		if (utilityProps.containsKey("name"))
			name = utilityProps.getProperty("name")+RandomStringUtils.randomAlphabetic(4);
		else
			name = RandomStringUtils.randomAlphabetic(4);
		
		String utilityId = "";
		try {
			//RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/products/seasons/" + seasonID + "/utilities?stage="+stage+"&minappversion="+minAppVersion, utility, sessionToken);
			RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/products/seasons/" + seasonID + "/utilities?stage="+stage + "&name=" + URLEncoder.encode(name, "UTF-8"), utility, sessionToken);
			String response = res.message;
			utilityId = parseUtilityId(response);
			
		} catch (IOException e) {
			Assert.fail("An exception was thrown when trying  to add a utility. Message: "+e.getLocalizedMessage()) ;
		}
		return utilityId;
	}
	
	public String addUtilityNoNameChange(String seasonID, Properties utilityProps, String sessionToken) throws IOException{
		//String minAppVersion = utilityProps.getProperty("minAppVersion");
		String stage = utilityProps.getProperty("stage");
		String utility = utilityProps.getProperty("utility");
		String name = "";
		if (utilityProps.containsKey("name"))
			name = utilityProps.getProperty("name");
		else
			name = RandomStringUtils.randomAlphabetic(4);
		
		String utilityId = "";
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/products/seasons/" + seasonID + "/utilities?stage="+stage + "&name=" + URLEncoder.encode(name, "UTF-8"), utility, sessionToken);
			String response = res.message;
			utilityId = parseUtilityId(response);
			
		} catch (IOException e) {
			Assert.fail("An exception was thrown when trying  to add a utility. Message: "+e.getLocalizedMessage()) ;
		}
		return utilityId;
	}
	
	public String addUtility(String seasonID, Properties utilityProps, String type, String sessionToken) throws IOException{
		//String minAppVersion = utilityProps.getProperty("minAppVersion");
		String stage = utilityProps.getProperty("stage");
		String utility = utilityProps.getProperty("utility"); 
		String name = "";
		if (utilityProps.containsKey("name"))
			name = utilityProps.getProperty("name")+RandomStringUtils.randomAlphabetic(4);
		else
			name = RandomStringUtils.randomAlphabetic(4);
		
		String utilityId = "";
		try {
			//RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/products/seasons/" + seasonID + "/utilities?stage="+stage+"&minappversion="+minAppVersion, utility, sessionToken);
			RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/products/seasons/" + seasonID + "/utilities?stage="+stage + "&type=" + type + "&name=" + URLEncoder.encode(name, "UTF-8"), utility, sessionToken);
			String response = res.message;
			utilityId = parseUtilityId(response);
			
		} catch (IOException e) {
			Assert.fail("An exception was thrown when trying  to add a utility. Message: "+e.getLocalizedMessage()) ;
		}
		return utilityId;
	}

	public String addUtilityWithForce(String seasonID, Properties utilityProps, boolean force, String sessionToken) throws IOException{
		//String minAppVersion = utilityProps.getProperty("minAppVersion");
		String stage = utilityProps.getProperty("stage");
		String utility = utilityProps.getProperty("utility"); 
		String name = "";
		if (utilityProps.containsKey("name"))
			name = utilityProps.getProperty("name")+RandomStringUtils.randomAlphabetic(4);
		else
			name = RandomStringUtils.randomAlphabetic(4);
		
		String utilityId = "";
		try {
			//RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/products/seasons/" + seasonID + "/utilities?stage="+stage+"&minappversion="+minAppVersion, utility, sessionToken);
			RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/products/seasons/" + seasonID + "/utilities?stage="+stage + "&force=" + force + "&name=" + URLEncoder.encode(name, "UTF-8"), utility, sessionToken);
			String response = res.message;
			utilityId = parseUtilityId(response);
			
		} catch (IOException e) {
			Assert.fail("An exception was thrown when trying  to add a utility. Message: "+e.getLocalizedMessage()) ;
		}
		return utilityId;
	}
	
	public String getUtility(String utilityID, String sessionToken){
		
		String utility = "";
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/products/seasons/utilities/" + utilityID, sessionToken);
			utility = res.message; 
		} catch (Exception e) {
			Assert.fail("An exception was thrown when trying  to get a utility. Message: "+e.getLocalizedMessage()) ;		
		}
		return utility;
	}	
	
	/*public String getUtilitiesInfo(String seasonID, String sessionToken, String stage, String minAppVersion){
		
		String utility = "";
		
		String reqUrl = m_url+"/products/seasons/" + seasonID + "/utilitiesinfo";
		if (stage!=null || minAppVersion!=null) {
			reqUrl+="?";
			if (stage!=null)
				reqUrl=reqUrl+"stage="+stage;
			
			if (stage!=null && minAppVersion!=null) {
				reqUrl+="&";
			}
			
			if (minAppVersion!=null)
				reqUrl=reqUrl+"minAppVerion="+minAppVersion;
		}
		
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(reqUrl, sessionToken);
			utility = res.message; 
		} catch (Exception e) {
			Assert.fail("An exception was thrown when trying  to get a utilitiesInfo. Message: "+e.getLocalizedMessage()) ;		
		}
		return utility;
	}	*/
	
	public String getUtilitiesInfo(String seasonID, String sessionToken, String stage){
		
		String utility = "";
		
		String reqUrl = m_url+"/products/seasons/" + seasonID + "/utilitiesinfo";
		if (stage!=null) {
			reqUrl+="?";

			reqUrl=reqUrl+"stage="+stage;

		}
		
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(reqUrl, sessionToken);
			utility = res.message; 
		} catch (Exception e) {
			Assert.fail("An exception was thrown when trying  to get a utilitiesInfo. Message: "+e.getLocalizedMessage()) ;		
		}
		return utility;
	}
	
	public String getUtilitiesInfo(String seasonID, String sessionToken, String stage, String type){
		
		String utility = "";
		
		String reqUrl = m_url+"/products/seasons/" + seasonID + "/utilitiesinfo";
		if (stage!=null) {
			reqUrl+="?";

			reqUrl=reqUrl+"stage="+stage;

		}
		
		if (type != null)
			reqUrl = reqUrl + "&type=" + type;
		
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(reqUrl, sessionToken);
			utility = res.message; 
		} catch (Exception e) {
			Assert.fail("An exception was thrown when trying  to get a utilitiesInfo. Message: "+e.getLocalizedMessage()) ;		
		}
		return utility;
	}

	/*
	public String updateUtility(String utilityID, String newContent, String sessionToken) throws Exception
	{
		String utilityId = "";
		try {
			String str = getUtility(utilityID, sessionToken);
			JSONObject utilityJson = new JSONObject(str);
			utilityJson.put("utility", newContent);
			utilityId = updateUtility(utilityID, utilityJson, sessionToken);

		} catch (Exception e) {
			Assert.fail("An exception was thrown when trying  to update a utility. Message: "+e.getLocalizedMessage()) ;
		}
		return utilityId;
	}
	*/

	public String updateUtility(String utilityID, JSONObject utilityJson, String sessionToken) throws JSONException{

			//String minAppVersion = utilityJson.getString("minAppVersion");
			String stage = utilityJson.getString("stage");
			String utility = utilityJson.getString("utility");
			String name = "";
			if (utilityJson.containsKey("name"))
				name = utilityJson.getString("name");
			else 
				name = RandomStringUtils.randomAlphabetic(4);
			
			utility = StringEscapeUtils.unescapeJava(utility);

			String lastModified = utilityJson.getString("lastModified");
			
		String utilityId = "";
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/products/seasons/utilities/" + utilityID + "?stage=" + stage /*+ "&minappversion=" + minAppVersion */+ "&lastmodified=" + lastModified + "&name="+URLEncoder.encode(name, "UTF-8"), utility, sessionToken);
			String response = res.message;
			utilityId = parseUtilityId(response);
		} catch (IOException e) {
			Assert.fail("An exception was thrown when trying  to update a utility. Message: "+e.getLocalizedMessage()) ;
		}
		
		return utilityId;
	}
	
	public int deleteUtility(String utilityID, String sessionToken) {
		int responseCode = -1;
 		try {
 			RestClientUtils.RestCallResults res = RestClientUtils.sendDelete(m_url+"/products/seasons/utilities/" + utilityID, sessionToken);
 			responseCode = res.code;
		} catch (Exception e) {
			Assert.fail("An exception was thrown when trying  to delete a utility. Message: "+e.getMessage()) ;
		}
 		return responseCode;
		
	}

	public String simulateUtility(String seasonID,String body,String stage, String minAppVersion, String simulationtype, String sessionToken) {
		String responseMessage = "";
		try {
			//RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/products/seasons/" + seasonID + "/utilities?stage="+stage+"&minappversion="+minAppVersion+"&simulationtype="+simulationtype, body, sessionToken);
			RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/products/seasons/" + seasonID +"/utilities/simulate?stage="+stage+"&minappversion="+minAppVersion+"&simulationtype="+simulationtype,body, sessionToken);
			responseMessage = res.message;
		} catch (Exception e) {
			Assert.fail("An exception was thrown when trying  to simulate an utility. Message: "+e.getMessage()) ;
		}
		return responseMessage;

	}
	
	public String updateUtilityWithForce(String utilityID, JSONObject utilityJson, boolean force, String sessionToken) throws JSONException{

		//String minAppVersion = utilityJson.getString("minAppVersion");
		String stage = utilityJson.getString("stage");
		String utility = utilityJson.getString("utility");
		utility = StringEscapeUtils.unescapeJava(utility);
		String name = "";
		if (utilityJson.containsKey("name"))
			name = utilityJson.getString("name");
		else 
			name = RandomStringUtils.randomAlphabetic(4);

		String lastModified = utilityJson.getString("lastModified");
		
	String utilityId = "";
	try {
		RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/products/seasons/utilities/" + utilityID + "?stage=" + stage /*+ "&minappversion=" + minAppVersion */+ "&lastmodified=" + lastModified + "&force=" + force + "&name="+URLEncoder.encode(name, "UTF-8"), utility, sessionToken);
		String response = res.message;
		utilityId = parseUtilityId(response);
	} catch (IOException e) {
		Assert.fail("An exception was thrown when trying  to update a utility. Message: "+e.getLocalizedMessage()) ;
	}
	
	return utilityId;
}
	
	
	private String parseUtilityId(String utility){
		String utilityID = "";
		JSONObject response = null;
		try {
			response = new JSONObject(utility);
			if (response.containsKey("error")){
				utilityID = utility;
			} else {
				utilityID = (String)response.get("uniqueId");
			}
		} catch (JSONException e) {
			utilityID = "Invalid response: " + response;		
		}

		return utilityID;

	}


}
