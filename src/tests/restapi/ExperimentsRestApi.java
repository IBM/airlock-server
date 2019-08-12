package tests.restapi;

import java.io.IOException;








import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import tests.com.ibm.qautils.RestClientUtils;

public class ExperimentsRestApi {
	protected static String m_url ;
	
	public void setURL (String url){
		m_url = url;
	}

	
	public String getAllExperiments (String productID, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/products/" + productID + "/experiments", sessionToken);
		return res.message;

	}
	
	public String getAllExperiments (String productID, boolean includeindexingdata, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/products/" + productID + "/experiments?includeindexinginfo=" + includeindexingdata, sessionToken);
		return res.message;

	}
	
	
	public String updateExperiments (String productID, String experiment, String sessionToken) throws IOException{

		RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/products/" + productID + "/experiments", experiment, sessionToken);
		return res.message;
	}
	
	public String getExperiment (String experimentID, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/products/experiments/" + experimentID, sessionToken);
		 return res.message;

	}
	
	public String getExperiment (String experimentID, boolean includeindexingdata, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/products/experiments/" + experimentID + "?includeindexinginfo=" + includeindexingdata, sessionToken);
		 return res.message;

	}

	
	public String createExperiment (String productID, String experiment, String sessionToken) throws IOException{

		RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/products/" + productID + "/experiments", experiment, sessionToken);
		return parseId(res.message);
	}
	
	public String updateExperiment(String experimentID, String experiment, String sessionToken) throws IOException{

		RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/products/experiments/" + experimentID, experiment, sessionToken);
		return parseId(res.message);
	}
	
	public int deleteExperiment(String experimentID, String sessionToken) throws Exception{

		RestClientUtils.RestCallResults res = RestClientUtils.sendDelete(m_url+"/products/experiments/" + experimentID, sessionToken);
		return res.code;
	}
	
	public String createVariant (String experimentID, String variant, String sessionToken) throws IOException{

		RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/products/experiments/" + experimentID + "/variants", variant, sessionToken);
		return parseId(res.message);
	}
	
	public String getVariant(String variantID, String sessionToken) throws Exception{

		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/products/experiments/variants/" + variantID,  sessionToken);
		return res.message;
	}
	
	public String updateVariant (String variantID, String variant, String sessionToken) throws IOException{

		RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/products/experiments/variants/" + variantID, variant, sessionToken);
		return parseId(res.message);
	}
	
	public int deleteVariant(String variantID, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendDelete(m_url+"/products/experiments/variants/" + variantID, sessionToken);
		return res.code;
	}
	
	public JSONArray getAllVariants(String experimentID, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/products/experiments/" + experimentID, sessionToken);
		JSONArray variants  = new JSONArray();
		try {
			JSONObject json = new JSONObject( res.message);
			variants = json.getJSONArray("variants");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return variants;

	}
	
	public String getAvailableBranches(String experimentID, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/products/experiments/" + experimentID + "/availablebranches",  sessionToken);
		return res.message;
	}
	
	public String getInputSample(String experimentID, String stage, String minAppVersion, String sessionToken, String generationMode, Double randomizeNumber) throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/products/experiments/" + experimentID + "/inputsample?stage=" + stage + "&minappversion=" + minAppVersion + "&generationmode=" + generationMode + "&randomize=" + randomizeNumber, sessionToken);
		return res.message;
	}
	
	public String getUtilitiesInfo(String experimentID, String stage, String sessionToken) throws Exception{

		RestClientUtils.RestCallResults res = RestClientUtils.sendGet( m_url+"/products/experiments/" + experimentID + "/utilitiesinfo?stage="+stage, sessionToken);
		return res.message;

	}
	
	//GET /analytics/products/experiments/{experiment-id}/indexinginfo
	public String getIndexinginfo(String experimentID, String sessionToken) throws Exception{

		RestClientUtils.RestCallResults res = RestClientUtils.sendGet( m_url+"/products/experiments/" + experimentID + "/indexinginfo", sessionToken);
		return res.message;

	}
	
	
	//PUT /analytics/products/experiments/{experiment-id}/resetdashboard
	public String resetDashboard (String experimentID, String sessionToken) throws IOException{

		RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/products/experiments/" + experimentID + "/resetdashboard", "", sessionToken);
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

}
