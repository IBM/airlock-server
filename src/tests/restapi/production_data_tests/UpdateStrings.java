package tests.restapi.production_data_tests;


import org.apache.wink.json4j.JSONArray;

import org.apache.wink.json4j.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.restapi.AirlockUtils;
import tests.restapi.ProductsRestApi;
import tests.restapi.StringsRestApi;

public class UpdateStrings {
	protected String sourceSeasonID;
	protected String targetSeasonID;
	protected String stringID;
	protected String filePath;
	protected StringsRestApi t;
	protected ProductsRestApi p;
	private AirlockUtils baseUtils;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	private String seasonID;
	private String sourceStage;
	private String targetStage;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "seasonID", "fromStage", "toStage"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, String seasonId,String fromStage, String toStage) throws Exception{
		m_url = url;
		filePath = configPath;
		t = new StringsRestApi();
		t.setURL(translationsUrl);
		p = new ProductsRestApi();
		p.setURL(m_url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = "";
		seasonID = seasonId;
		sourceStage = fromStage;
		targetStage = toStage;
		
	}

//TODO validate all fields

	/*
	@Test
	public void updateInternationalFallback1() throws Exception{
		String str = t.getAllStrings(sourceSeasonID, "BASIC", sessionToken);
		JSONObject temp = new JSONObject(str);
		JSONArray allstr = temp.getJSONArray("strings");
		for (int i=0; i<allstr.size(); i++){
			JSONObject single = allstr.getJSONObject(i);
			if (single.getString("stage").equals("DEVELOPMENT")){
				single.put("stage", "PRODUCTION");
				t.updateString(single.getString("uniqueId"), single.toString(), "ACT", sessionToken);
			}	
		}
	}
	*/
	
	@Test
	public void updateStage() throws Exception{
		String str = t.getAllStrings(seasonID, "BASIC", sessionToken);
		JSONObject temp = new JSONObject(str);
		JSONArray allstr = temp.getJSONArray("strings");
		for (int i=0; i<allstr.size(); i++){
			JSONObject single = allstr.getJSONObject(i);
			if (single.getString("stage").equals(sourceStage)){
				single.put("stage", targetStage);
				t.updateString(single.getString("uniqueId"), single.toString(), "ACT", sessionToken);
			}	
		}
	}
	

}
