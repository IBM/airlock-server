package tests.restapi.upgradeV2V2_5;

import org.apache.wink.json4j.JSONException;

import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.RestClientUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.SeasonsRestApi;


public class CopyS3 {
	protected String seasonID;
	protected String productID;
	protected String filePath;
	protected SeasonsRestApi s;
	protected FeaturesRestApi f;
	protected String sessionToken = "";
	protected String m_url;
	protected String m_fromVersion;
	private JSONObject body = new JSONObject();

	
	@BeforeClass
	@Parameters({"testUrl", "path", "productName", "configPath", "productId",  "seasonId", "sessionToken", "minVersion"})
	public void init(String testUrl, String path, String productName, String configPath, String productId, String seasonId, @Optional String sToken, String minVersion) throws JSONException{
		m_url = testUrl;
		if (sToken != null)
			sessionToken = sToken;
		
		//body.put("path", "vicky/SEASON_FROM_V2/seasons/bc479db5-ff58-4138-b5e4-a8400a1f78d5/7a688737-8cc4-4a46-a261-0517bf63a203");
		body.put("path", "DEV4/seasons/914227f7-83bc-4a0f-a428-8241c920d45c/6cf064ec-0288-413b-b7f5-c7eddf3b5a6f");
		
		body.put("productName", "MTXWithMaximalMaxFeaturesOn");
		body.put("productId", "914227f7-83bc-4a0f-a428-8241c920d45c");
		body.put("seasonId", "6cf064ec-0288-413b-b7f5-c7eddf3b5a6f");
		body.put("minVersion", "8.0");
		
	 	//android_productId=bc479db5-ff58-4138-b5e4-a8400a1f78d5
		//season=7a688737-8cc4-4a46-a261-0517bf63a203
	}
	

	
	@Test (description = "")
	public void copy() throws Exception{

		//http://localhost:8080/airlock/api/test/import
		RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url, body.toString(), sessionToken);
		Assert.assertTrue(res.code==200, "Product was not copied");;
		
	}
	

}
