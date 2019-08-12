package tests.restapi;

import java.io.IOException;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import tests.com.ibm.qautils.RestClientUtils;

public class BranchesRestApi {
	protected static String m_url ;
	public static String MASTER = "MASTER";

	
	public void setURL (String url){
		m_url = url;
	}

	
	//GET /admin/products/seasons/{season-id}/branches Returns the branches for the specified season
	public String getAllBranches(String seasonID, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/products/seasons/" + seasonID + "/branches", sessionToken);
		return res.message;

	}
	
	//POST /admin/products/seasons/{season-id}/{source-branch-id}/branches Creates a branch within the specified season
	public String createBranch(String seasonID, String branch, String sourceBranch, String sessionToken) throws IOException{
		RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/products/seasons/" + seasonID + "/" + sourceBranch + "/branches", branch, sessionToken);
		return parseId(res.message);

	}
	
	//GET /admin/products/seasons/branches/{branch-id} Returns the specified branch
	public String getBranch(String branchID, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/products/seasons/branches/" + branchID , sessionToken);
		return res.message;

	}
	
	//return purchases as well
	//GET /admin/products/seasons/branches/{branch-id} Returns the specified branch
	public String getBranchWithFeatures(String branchID, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/products/seasons/branches/" + branchID + "?mode=INCLUDE_FEATURES", sessionToken);
		return res.message;

	}
	
	//DELETE /admin/products/seasons/branches/{branch-id} Deletes the specified branch
	public int deleteBranch(String branchID, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendDelete(m_url+"/products/seasons/branches/" + branchID , sessionToken);
		return res.code;
	}
	
	//PUT /admin/products/seasons/branches/{branch-id} Updates the specified branch 
	public String updateBranch(String branchID, String branch, String sessionToken) throws IOException{
		RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/products/seasons/branches/" + branchID , branch, sessionToken);
		return parseId(res.message);

	}
	
	//CHECKOUT FEATURE FROM BRANCH (can be purchase item as well)	
	public String checkoutFeature(String branchID, String featureID, String sessionToken) throws IOException{
		//PUT /admin/products/seasons/branches/{branch-id}/checkout/{feature-id}

		RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/products/seasons/branches/" + branchID + "/checkout/" + featureID, "", sessionToken);
 		return parseId(res.message);
	}
	
	//uncheckout feature from branch (can be purchase item as well)

	public String cancelCheckoutFeature(String branchID, String featureID, String sessionToken) throws IOException{
		//put /admin/products/seasons/branches/{branch-id}/cancelcheckout/{feature-id}

		RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/products/seasons/branches/" + branchID + "/cancelcheckout/" + featureID, "", sessionToken);
 		return res.message;
	}
	
	//uncheckout feature from branch (can be purchase item as well)

	public String cancelCheckoutFeatureWithSubFeatures(String branchID, String featureID, String sessionToken) throws IOException{
		//put /admin/products/seasons/branches/{branch-id}/cancelcheckout/{feature-id}

		RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/products/seasons/branches/" + branchID + "/cancelcheckout/" + featureID + "?mode=INCLUDE_SUB_FEATURES", "", sessionToken);
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
