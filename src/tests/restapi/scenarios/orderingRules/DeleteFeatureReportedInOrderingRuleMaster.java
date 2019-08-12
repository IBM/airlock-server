package tests.restapi.scenarios.orderingRules;

import java.io.IOException;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;

public class DeleteFeatureReportedInOrderingRuleMaster {
	protected String seasonID;
	protected String filePath;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	private AirlockUtils baseUtils;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	private String featureID1;
	private String featureID2;
	private String featureID3;
	private String featureID4;
	private String featureID5;
	private String orderingRuleMtxID1;
	private String orderingRuleMtxID2;
	private String orderingRuleID1;
	private String orderingRuleID2;
	private String mtxID;

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(m_url);
		p = new ProductsRestApi();
		p.setURL(m_url);

		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);

	}


	//F1 -> OR{F2, F3}, F3; F2 -> F4;F5;OR{F4}
	@Test (description = "Add features and  ordering rules")
	public void addComponents() throws JSONException, IOException{
		//add features
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("name", RandomStringUtils.randomAlphabetic(5)+"XXXF1");
		featureID1 = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Can't add feature  " + featureID1);

		json.put("name", RandomStringUtils.randomAlphabetic(5)+"XXXF2");
		featureID2 = f.addFeature(seasonID, json.toString(), featureID1, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Can't add feature  " + featureID2);

		json.put("name", RandomStringUtils.randomAlphabetic(5)+"XXXF3");
		featureID3 = f.addFeature(seasonID, json.toString(), featureID1, sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "Can't add feature  " + featureID3);

		json.put("name", RandomStringUtils.randomAlphabetic(5)+"XXXF4");
		featureID4 = f.addFeature(seasonID, json.toString(), featureID2, sessionToken);
		Assert.assertFalse(featureID4.contains("error"), "Can't add feature  " + featureID4);

		json.put("name", RandomStringUtils.randomAlphabetic(5)+"XXXF5");
		featureID5 = f.addFeature(seasonID, json.toString(), featureID2, sessionToken);
		Assert.assertFalse(featureID5.contains("error"), "Can't add feature  " + featureID5);


		//add ordering rules
		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", "OR."+RandomStringUtils.randomAlphabetic(5)+"XXXOR1");

		JSONObject configJson = new JSONObject();
		configJson.put(featureID2, "1.0");
		configJson.put(featureID3, "2.0");

		jsonOR.put("configuration", configJson.toString());

		orderingRuleID1 = f.addFeature(seasonID, jsonOR.toString(), featureID1, sessionToken);
		Assert.assertFalse(orderingRuleID1.contains("error"), "Can't add orderingRule  " + orderingRuleID1);

		jsonOR.put("name", "OR."+RandomStringUtils.randomAlphabetic(5)+"XXXOR2");
		configJson = new JSONObject();
		configJson.put(featureID4, "1.0");		
		jsonOR.put("configuration", configJson.toString());
		orderingRuleID2 = f.addFeature(seasonID, jsonOR.toString(), featureID2, sessionToken);
		Assert.assertFalse(orderingRuleID2.contains("error"), "Can't add orderingRule  " + orderingRuleID2);

	}

	@Test(dependsOnMethods = "addComponents", description = "try to delete feature reported in ordering rule")
	public void deleteFeatureReportedInOrderingRule () throws Exception {
		int respCode = f.deleteFeature(featureID2, sessionToken);
		Assert.assertNotEquals(respCode, 200, "Feature reported in orderingRule was deleted");
	}

	@Test(dependsOnMethods = "deleteFeatureReportedInOrderingRule", description = "remove feature from oredering rule and delete it")
	public void removeFeatureFromORAndDeleteIt () throws Exception {
		JSONObject configJson = new JSONObject();
		configJson.put(featureID3, "2.0");

		String orderingRule = f.getFeature(orderingRuleID1, sessionToken);
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("configuration", configJson.toString());
		String response = f.updateFeature(seasonID, orderingRuleID1, jsonOR.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update orderingRule  " + response);

		int respCode = f.deleteFeature(featureID2, sessionToken);
		Assert.assertTrue(respCode==200, "Feature reported in orderingRule was not deleted");
	}

	@Test(dependsOnMethods = "removeFeatureFromORAndDeleteIt", description = "remove feature with ordering rule")
	public void removeFeatureWithOrderingRule () throws Exception {
		int respCode = f.deleteFeature(featureID1, sessionToken);
		Assert.assertTrue(respCode==200, "Feature with orderingRule was not deleted");
	} 

	//F1 -> OR{MTX, F3}, F3; MTX -> MTX,F4;F5;OR{F4, MTX}
	@Test (dependsOnMethods ="removeFeatureWithOrderingRule",description = "Add features, mtx and  ordering rules")
	public void reAddComponents() throws JSONException, IOException{
		//add features
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("name", RandomStringUtils.randomAlphabetic(5)+"XXXF11");
		featureID1 = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Can't add feature  " + featureID1);

		String mtxStr = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		mtxID = f.addFeature(seasonID, mtxStr, featureID1, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Can't add mtx  " + featureID2);

		json.put("name", RandomStringUtils.randomAlphabetic(5)+"XXXF33");
		featureID3 = f.addFeature(seasonID, json.toString(), featureID1, sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "Can't add feature  " + featureID3);

		json.put("name", RandomStringUtils.randomAlphabetic(5)+"XXXF44");
		featureID4 = f.addFeature(seasonID, json.toString(), mtxID, sessionToken);
		Assert.assertFalse(featureID4.contains("error"), "Can't add feature  " + featureID4);

		json.put("name", RandomStringUtils.randomAlphabetic(5)+"XXXF55");
		featureID5 = f.addFeature(seasonID, json.toString(), mtxID, sessionToken);
		Assert.assertFalse(featureID5.contains("error"), "Can't add feature  " + featureID5);


		//add ordering rules
		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", "OR."+RandomStringUtils.randomAlphabetic(5)+"XXXOR11");

		JSONObject configJson = new JSONObject();
		configJson.put(mtxID, "1.0");
		configJson.put(featureID3, "2.0");

		jsonOR.put("configuration", configJson.toString());

		orderingRuleID1 = f.addFeature(seasonID, jsonOR.toString(), featureID1, sessionToken);
		Assert.assertFalse(orderingRuleID1.contains("error"), "Can't add orderingRule  " + orderingRuleID1);

		jsonOR.put("name", "OR."+RandomStringUtils.randomAlphabetic(5)+"XXXOR22");
		configJson = new JSONObject();
		configJson.put(featureID4, "1.0");		
		jsonOR.put("configuration", configJson.toString());
		orderingRuleID2 = f.addFeature(seasonID, jsonOR.toString(), mtxID, sessionToken);
		Assert.assertFalse(orderingRuleID2.contains("error"), "Can't add orderingRule  " + orderingRuleID2);

	}

	@Test(dependsOnMethods = "reAddComponents", description = "try to delete mtx reported in ordering rule")
	public void deleteMTXReportedInOrderingRule () throws Exception {
		int respCode = f.deleteFeature(mtxID, sessionToken);
		Assert.assertNotEquals(respCode, 200, "mtx reported in orderingRule was deleted");
	}

	@Test(dependsOnMethods = "deleteMTXReportedInOrderingRule", description = "remove mtx from oredering rule and delete it")
	public void removeMTXFromORAndDeleteIt () throws Exception {
		JSONObject configJson = new JSONObject();
		configJson.put(featureID3, "2.0");

		String orderingRule = f.getFeature(orderingRuleID1, sessionToken);
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("configuration", configJson.toString());
		String response = f.updateFeature(seasonID, orderingRuleID1, jsonOR.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update orderingRule  " + response);

		int respCode = f.deleteFeature(mtxID, sessionToken);
		Assert.assertTrue(respCode==200, "mtx reported in orderingRule was not deleted");
	}


	@Test(dependsOnMethods = "removeMTXFromORAndDeleteIt", description = "remove feature with ordering rule")
	public void removeFeatureWithOrderingRule2 () throws Exception {
		int respCode = f.deleteFeature(featureID1, sessionToken);
		Assert.assertTrue(respCode==200, "Feature with orderingRule was not deleted");
	}

	//F1 -> OR_MTX->OR{MTX, F3}, F3; MTX -> MTX,F4;F5;MTX_OR->OR{F4, MTX}
	@Test (dependsOnMethods ="removeFeatureWithOrderingRule2",description = "Add features, mtx, ordering rules and  mtx ordering rules")
	public void reAddComponents2() throws JSONException, IOException{
		//add features
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("name", RandomStringUtils.randomAlphabetic(5)+"XXXF11");
		featureID1 = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Can't add feature  " + featureID1);

		String mtxStr = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		mtxID = f.addFeature(seasonID, mtxStr, featureID1, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Can't add mtx  " + featureID2);

		json.put("name", RandomStringUtils.randomAlphabetic(5)+"XXXF33");
		featureID3 = f.addFeature(seasonID, json.toString(), featureID1, sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "Can't add feature  " + featureID3);

		json.put("name", RandomStringUtils.randomAlphabetic(5)+"XXXF44");
		featureID4 = f.addFeature(seasonID, json.toString(), mtxID, sessionToken);
		Assert.assertFalse(featureID4.contains("error"), "Can't add feature  " + featureID4);

		json.put("name", RandomStringUtils.randomAlphabetic(5)+"XXXF55");
		featureID5 = f.addFeature(seasonID, json.toString(), mtxID, sessionToken);
		Assert.assertFalse(featureID5.contains("error"), "Can't add feature  " + featureID5);


		//add ordering rules
		String orderingRuleMtx = FileUtils.fileToString(filePath + "orderingRule/mtxOrderingRule.txt", "UTF-8", false);
		orderingRuleMtxID1 = f.addFeature(seasonID, orderingRuleMtx, featureID1, sessionToken);
		Assert.assertFalse(orderingRuleMtxID1.contains("error"), "Can't add ordering rule mtx  " + orderingRuleMtxID1);
		
		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", "OR."+RandomStringUtils.randomAlphabetic(5)+"XXXOR11");

		JSONObject configJson = new JSONObject();
		configJson.put(mtxID, "1.0");
		configJson.put(featureID3, "2.0");

		jsonOR.put("configuration", configJson.toString());

		orderingRuleID1 = f.addFeature(seasonID, jsonOR.toString(), orderingRuleMtxID1, sessionToken);
		Assert.assertFalse(orderingRuleID1.contains("error"), "Can't add orderingRule  " + orderingRuleID1);

		
		orderingRuleMtxID2 = f.addFeature(seasonID, orderingRuleMtx, mtxID, sessionToken);
		Assert.assertFalse(orderingRuleMtxID2.contains("error"), "Can't add ordering rule mtx  " + orderingRuleMtxID2);
		
		jsonOR.put("name", "OR."+RandomStringUtils.randomAlphabetic(5)+"XXXOR22");
		configJson = new JSONObject();
		configJson.put(featureID4, "1.0");		
		jsonOR.put("configuration", configJson.toString());
		orderingRuleID2 = f.addFeature(seasonID, jsonOR.toString(), orderingRuleMtxID2, sessionToken);
		Assert.assertFalse(orderingRuleID2.contains("error"), "Can't add orderingRule  " + orderingRuleID2);

	}

	@Test(dependsOnMethods = "reAddComponents2", description = "try to delete mtx reported in ordering rule")
	public void deleteMTXReportedInOrderingRule2 () throws Exception {
		int respCode = f.deleteFeature(mtxID, sessionToken);
		Assert.assertNotEquals(respCode, 200, "mtx reported in orderingRule was deleted");
	}

	@Test(dependsOnMethods = "deleteMTXReportedInOrderingRule2", description = "remove mtx from oredering rule and delete it")
	public void removeMTXFromORAndDeleteIt2 () throws Exception {
		JSONObject configJson = new JSONObject();
		configJson.put(featureID3, "2.0");

		String orderingRule = f.getFeature(orderingRuleID1, sessionToken);
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("configuration", configJson.toString());
		String response = f.updateFeature(seasonID, orderingRuleID1, jsonOR.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update orderingRule  " + response);

		int respCode = f.deleteFeature(mtxID, sessionToken);
		Assert.assertTrue(respCode==200, "mtx reported in orderingRule was not deleted");
	}


	@Test(dependsOnMethods = "removeMTXFromORAndDeleteIt2", description = "remove feature with ordering rule")
	public void removeFeatureWithOrderingRule3 () throws Exception {
		int respCode = f.deleteFeature(featureID1, sessionToken);
		Assert.assertTrue(respCode==200, "Feature with orderingRule was not deleted");
	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
