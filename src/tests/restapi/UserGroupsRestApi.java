package tests.restapi;

import tests.com.ibm.qautils.RestClientUtils;

public class UserGroupsRestApi {
	protected static String m_url ;
	
	//url is http://localhost:9090/airlock/api/admin/products/{product-id}/usergroups
	public void setURL (String url){
		m_url = url + "/products/";
	}
	
	public String getUserGroups(String productId, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url + productId +"/usergroups" , sessionToken);
		return res.message;

	}
	
	public String getUserGroupsUsage(String productId, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url + productId +"/usergroups/usage" , sessionToken);
		return res.message;

	}
	
	public String setUserGroups(String productId, String groups, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url + productId +"/usergroups" , groups, sessionToken);
		return res.message;

	}
}
