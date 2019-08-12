package tests.restapi;

import tests.com.ibm.qautils.RestClientUtils;


public class TestServicesRestApi {
	protected static String m_url ;
	
	public void setURL (String url){
		m_url = url;
	}



	public String setTestMails(String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/TestMails","",sessionToken);
		String response = res.message;
		return response;
	}

	public String getTestMails(String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/TestMails",sessionToken);
		String response = res.message;
		return response;
	}

	public String deleteTestMails(String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendDelete(m_url+"/TestMails",sessionToken);
		String response = res.message;
		return response;
	}

	public String setTestTranslation(String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/translations","",sessionToken);
		String response = res.message;
		return response;
	}

	public String getTestTranslation(String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/translations",sessionToken);
		String response = res.message;
		return response;
	}

	public String wakeUpTranslation(String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/translations/wakeup","",sessionToken);
		String response = res.message;
		return response;
	}

	public String importSeason(String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/import","",sessionToken);
		String response = res.message;
		return response;
	}

}
