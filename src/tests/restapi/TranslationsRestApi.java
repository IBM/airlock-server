package tests.restapi;

import java.io.IOException;


import tests.com.ibm.qautils.RestClientUtils;

public class TranslationsRestApi {
	protected static String m_url ;
	protected static String test_url ;

	public void setURL (String url){
		m_url = url;
		test_url = m_url.replace("/api/translations", "/api/test/translations");
		
	}

	//POST /translations/{season-id}/translate/{locale}
	public String addTranslation(String seasonID, String locale, String translation, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res =RestClientUtils.sendPost(m_url+"/" + seasonID + "/translate/" + locale, translation, sessionToken);
		String response = res.message;
		return response;
	}

	//PUT /translations/{season-id}/translate/{locale}
	public String updateTranslation(String seasonID, String locale, String translation, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res =RestClientUtils.sendPut(m_url+"/" + seasonID + "/translate/" + locale, translation, sessionToken);
		String response = res.message;
		return response;
	}

	//GET /translations/{season-id}/translate/{locale}
	public String getTranslation(String seasonID, String locale, String stage, String sessionToken) throws Exception{

		RestClientUtils.RestCallResults res =RestClientUtils.sendGet(m_url+"/" + seasonID + "/translate/" + locale + "?stage=" + stage, sessionToken);
		String response = res.message;
		return response;
	}

	//GET /translations/{season-id}/translate/{locale}
	public String getTranslation(String seasonID, String locale, String sessionToken) throws Exception{

		RestClientUtils.RestCallResults res =RestClientUtils.sendGet(m_url+"/" + seasonID + "/translate/" + locale, sessionToken);
		String response = res.message;
		return response;
	}

	public String stringForTranslation(String seasonID, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res =RestClientUtils.sendGet(m_url+"/seasons/" + seasonID + "/stringsfortranslation/", sessionToken);
		String response = res.message;
		return response;
	}


	//GET /translations/{season-id}/supportedlocales
	public String getSupportedLocales(String seasonID, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res =RestClientUtils.sendGet(m_url+"/" + seasonID + "/supportedlocales", sessionToken);
		String response = res.message;
		return response;
	}

	//POST /translations/{season-id}/supportedlocales/{locale}
	public String addSupportedLocales(String seasonID,String locale, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res =RestClientUtils.sendPost(m_url+"/" + seasonID + "/supportedlocales/"+locale,"", sessionToken);
		String response = res.message;
		return response;
	}

	//POST /translations/{season-id}/supportedlocales/{locale}?source=<sourceLocale>
	public String addSupportedLocaleFromLocale(String seasonID, String locale, String sourceLocale, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res =RestClientUtils.sendPost(m_url+"/" + seasonID + "/supportedlocales/"+locale+"?source=" + sourceLocale,"", sessionToken);
		String response = res.message;
		return response;
	}

		
	//DELETE /translations/{season-id}/supportedlocales/{locale}
	public String removeSupportedLocales(String seasonID,String locale, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res =RestClientUtils.sendDelete(m_url+"/" + seasonID + "/supportedlocales/"+locale, sessionToken);
		String response = res.message;
		return response;
	}
	//DELETE /translations/{season-id}/supportedlocales/{locale}
	public String removeSupportedLocaleLeaveRuntimeFiles(String seasonID,String locale, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res =RestClientUtils.sendDelete(m_url+"/" + seasonID + "/supportedlocales/"+locale+"?removeRuntimeFiles=false", sessionToken);
		String response = res.message;
		return response;
	}
	//PUT /translations/seasons/{string-id}/overridetranslate/{locale}
	public String overrideTranslate(String stringID,String locale,String body, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res =RestClientUtils.sendPut(m_url+"/seasons/" + stringID + "/overridetranslate/"+locale,body, sessionToken);
		String response = res.message;
		return response;
	}
	//PUT /translations/seasons/{string-id}/canceloverride/{locale}
	public String cancelOverride(String stringID,String locale, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res =RestClientUtils.sendPut(m_url+"/seasons/" + stringID + "/canceloverride/"+locale,"", sessionToken);
		String response = res.message;
		return response;
	}

	//GET /translations/seasons/{feature-id}/stringsinuse
	public String stringInUse(String featureID, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res =RestClientUtils.sendGet(m_url+"/seasons/" + featureID + "/stringsinuse", sessionToken);
		String response = res.message;
		return response;
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

	//PUT /translations/seasons/{season-id}/markfortranslation
	public String markForTranslation(String seasonID,String[] idsArray, String sessionToken) throws Exception{
		String ids = idsToString(idsArray);
		RestClientUtils.RestCallResults res =RestClientUtils.sendPut(m_url+"/seasons/" + seasonID + "/markfortranslation"+ids,"", sessionToken);
		String response = res.message;
		return response;
	}
	//PUT /translations/seasons/{season-id}/reviewForTranslation
	public String reviewForTranslation(String seasonID,String[] idsArray, String sessionToken) throws Exception{
		String ids = idsToString(idsArray);
		RestClientUtils.RestCallResults res =RestClientUtils.sendPut(m_url+"/seasons/" + seasonID + "/completereview"+ids,"", sessionToken);
		String response = res.message;
		return response;
	}

	//PUT /translations/seasons/{season-id}/sendtotranslation
	public String sendToTranslation(String seasonID,String[] idsArray, String sessionToken) throws Exception{
		String ids = idsToString(idsArray);
		RestClientUtils.RestCallResults res =RestClientUtils.sendPut(m_url+"/seasons/" + seasonID + "/sendtotranslation"+ids,"", sessionToken);
		String response = res.message;
		return response;
	}

	//GET /translations/seasons/{season-id}/newstringsfortranslation

	public String getNewStringsForTranslation(String seasonID,String[] idsArray, String sessionToken) throws Exception{
		String ids = idsToString(idsArray);
		RestClientUtils.RestCallResults res =RestClientUtils.sendGet(m_url+"/seasons/" + seasonID + "/newstringsfortranslation"+ids, sessionToken);
		String response = res.message;
		return response;
	}

	//GET /translations/seasons/{season-id}/translate/summary
	public String getTranslationSummary(String seasonID,String[] idsArray, String sessionToken) throws Exception{
		String ids = idsToString(idsArray);
		RestClientUtils.RestCallResults res =RestClientUtils.sendGet(m_url+"/seasons/" + seasonID + "/translate/summary"+ids, sessionToken);
		String response = res.message;
		return response;
	}

	//GET /translations/seasons/{season-id}/strings/statuses
	public String getStringStatuses(String seasonID, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res =RestClientUtils.sendGet(m_url+"/seasons/" + seasonID + "/strings/statuses", sessionToken);
		String response = res.message;
		return response;
	}

	//GET /translations/seasons/{season-id}/strings/{status}
	public String getStringsByStatuses(String seasonID, String status, String mode, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res =RestClientUtils.sendGet(m_url+"/seasons/" + seasonID + "/strings/" + status + "?mode=" + mode, sessionToken);
		String response = res.message;
		return response;
	}

	//PUT /translations/seasons/copystrings/{dest-season-id}
	public String copyStrings(String destSeasonID,String[] idsArray,Boolean overwite, String sessionToken) throws Exception{
		String ids = idsToString(idsArray);
		RestClientUtils.RestCallResults res =RestClientUtils.sendPut(m_url+"/seasons/copystrings/" + destSeasonID +ids+"&mode=ACT&overwrite="+overwite,"", sessionToken);
		String response = res.message;
		return response;
	}

	//PUT /translations/seasons/{season-id}/importstrings
	public String importStrings(String seasonID,String strToImport,Boolean overwite, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res =RestClientUtils.sendPut(m_url+"/seasons/" + seasonID +"/importstrings?mode=ACT&overwrite="+overwite,strToImport, sessionToken);
		String response = res.message;
		return response;
	}
	
	
	///translations/seasons/{season-id}/importstringswithformat
	//
	
	//for smartling workflow - automatic translation

	public String traceOn (String sessionToken) throws IOException{
		RestClientUtils.RestCallResults res =RestClientUtils.sendPut(test_url + "?trace=true","", sessionToken);
		return res.message;
	}
	
	public String traceOff (String sessionToken) throws IOException{
		RestClientUtils.RestCallResults res =RestClientUtils.sendPut(test_url + "?trace=false","", sessionToken);
		return res.message;
	}
	
	public String setLogFinegrainOn (String sessionToken) throws IOException{
		RestClientUtils.RestCallResults res =RestClientUtils.sendPut(test_url + "?fine=true","", sessionToken);
		return res.message;
	}
	
	public String setLogFinegrainOff (String sessionToken) throws IOException{
		RestClientUtils.RestCallResults res =RestClientUtils.sendPut(test_url + "?fine=false","", sessionToken);
		return res.message;
	}
	
	public String getLog (String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res =RestClientUtils.sendGet(test_url,sessionToken);
		return res.message;
	}
	
}
