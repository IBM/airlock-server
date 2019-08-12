package tests.restapi;


import java.io.IOException;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;

import tests.com.ibm.qautils.RestClientUtils;

public class ProductsRestApi {
	protected static String m_url ;
	
	public void setURL (String url){
		m_url = url;
	}
	
	public String addProduct(String productJson, String sessionToken) throws IOException{
		String productID = "";
		RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/products", productJson, sessionToken);
		String product = res.message;
		productID = parseProductId(product);
		
		if (productID.contains("error"))
			return productID;
		
		//add QA and DEV user groups
		UserGroupsRestApi ug = new UserGroupsRestApi();
		ug.setURL(m_url);
	
		try {
			String response = ug.getUserGroups(productID, sessionToken);
			JSONObject json = new JSONObject(response);
			JSONArray groups = json.getJSONArray("internalUserGroups");
			groups.put("QA");	
			groups.put("DEV");	
			json.put("internalUserGroups", groups);
			response = ug.setUserGroups(productID, json.toString(), sessionToken);
			if(response.contains("error"))
				return response;
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	
		return productID;
		
	}
	
	public String addProductWithoutAddingUserGroups(String productJson, String sessionToken) throws IOException{
		String productID = "";
		RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/products", productJson, sessionToken);
		String product = res.message;
		productID = parseProductId(product);
		return productID;	
	}
	
	public String addProduct(String productJson, String productID, String sessionToken) throws IOException{
		RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/products/" + productID, productJson, sessionToken);
		String product = res.message;
		productID = parseProductId(product);
		return productID;
		
	}
	

	
	public String getProduct(String productID, String sessionToken){
		String response="";
 		try {
 			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url + "/products/" + productID, sessionToken);
 			response = res.message;
 			//response contains feature json
 			
		} catch (Exception e) {
			Assert.fail("An exception was thrown when trying  to get product. Message: "+e.getMessage()) ;
		}
 		return response;
	}
	

	public String getAllProducts(String sessionToken){
		String response="";
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/products", sessionToken);
			response = res.message;
		 	//response contains feature json
		 			
			} catch (Exception e) {
				System.out.println(e.getLocalizedMessage());
				Assert.fail("An exception was thrown when trying  to get a list of all products. Messaeg: "+e.getLocalizedMessage()) ;
				
			}
		return response;
	}	
			
	public int deleteProduct(String productID, String sessionToken){
		int responseCode = -1;
 		try {
 			RestClientUtils.RestCallResults res = RestClientUtils.sendDelete(m_url+"/products/" + productID, sessionToken);
 			//Assert.assertEquals(responseCode, 200, "Failed to delete a product.");
 			responseCode = res.code;
		} catch (Exception e) {
			Assert.fail("An exception was thrown when trying  to delete a product. Message: "+e.getMessage()) ;
		}
 		return responseCode;
	}
	
	public String updateProduct(String productID, String json){
		String productIDResp="";
 		try {
 			RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/products/" + productID, json);
 			productIDResp = parseProductId(res.message);

 			
		} catch (Exception e) {
			Assert.fail("An exception was thrown when trying  to update a product. Message: "+e.getMessage()) ;
		}
 		return productIDResp;
	}
	
	public void reset() {
		try {
			//RestClientUtils.sendPost(m_url+"/products/reset", null);

		} catch (Exception e) {
			Assert.fail("An exception was thrown when trying  to reset the system. Message: "+e.getMessage()) ;
		}
	}
	
	
	public String addProduct(String productJson) throws IOException{
		String productID = "";
		RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/products", productJson);
		String product = res.message;
		productID = parseProductId(product);
		return productID;
		
	}



	public String getProduct(String productID){
			String response="";
 		try {
 			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url + "/products/" + productID);
 			response = res.message;
 			//response contains feature json
 			
		} catch (Exception e) {
			Assert.fail("An exception was thrown when trying  to get product. Message: "+e.getMessage()) ;
		}
 		return response;
	}
	

	public String getAllProducts(){
		String response="";
		try {
		 	
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/products");
			response = res.message;
		 	//response contains feature json
		 			
			} catch (Exception e) {
				Assert.fail("An exception was thrown when trying  to get a list of all products. Messaeg: "+e.getMessage()) ;
			}
		return response;
	}	
			
	public int deleteProduct(String productID){
		int responseCode = -1;
 		try {
 			RestClientUtils.RestCallResults res = RestClientUtils.sendDelete(m_url+"/products/" + productID);
 			responseCode = res.code;
 			//Assert.assertEquals(responseCode, 200, "Failed to delete a product.");
 			
		} catch (Exception e) {
			Assert.fail("An exception was thrown when trying  to delete a product. Message: "+e.getMessage()) ;
		}
 		return responseCode;
	}
	
	public String updateProduct(String productID, String json, String sessionToken){
		String productIDResp="";
 		try {
 			RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/products/" + productID, json, sessionToken);
 			String response = res.message;
 			productIDResp = parseProductId(response); 			
		} catch (Exception e) {
			Assert.fail("An exception was thrown when trying  to update a product. Message: "+e.getMessage()) ;
		}
 		return productIDResp;
	}
	
	private String parseProductId(String product){
		String productID = "";
		JSONObject response = null;
		try {
			response = new JSONObject(product);
			if (response.containsKey("error")){
				productID = product;
			} else {
				productID = (String)response.get("uniqueId");
			}
		} catch (JSONException e) {
			productID = "Invalid response: " + response;
		}

		return productID;

	}

	public String getProductFollowers(String productID, String sessionToken){
		String response="";
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url + "/products/" + productID+"/follow",sessionToken);
			response = res.message;
			//response list of followers

		} catch (Exception e) {
			Assert.fail("An exception was thrown when trying  to get followers. Message: "+e.getMessage()) ;
		}
		return response;
	}

	public String followProduct(String productID, String sessionToken){
		String response="";
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url + "/products/" + productID+"/follow","",sessionToken);
			response = res.message;
		} catch (Exception e) {
			Assert.fail("An exception was thrown when trying  to follow product. Message: "+e.getMessage()) ;
		}
		return response;
	}

	public int unfollowProduct(String productID, String sessionToken){
		
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendDelete(m_url + "/products/" + productID+"/follow",sessionToken);
			return res.code;
		} catch (Exception e) {
			Assert.fail("An exception was thrown when trying  to unfollow product. Message: "+e.getMessage()) ;
		}
		return -1;
	}


}
