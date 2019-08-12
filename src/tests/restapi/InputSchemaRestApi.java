package tests.restapi;


import java.io.IOException;

import tests.com.ibm.qautils.RestClientUtils;

public class InputSchemaRestApi {
	protected static String m_url ;
	
	public void setURL (String url){
		m_url = url;
	}
	

	
	public String getInputSchema(String seasonID, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/products/seasons/" + seasonID + "/inputschema", sessionToken);
		String schema = res.message;
		return schema;

	}

	
	public String updateInputSchema(String seasonID, String schemaJson, String sessionToken) throws IOException{
		RestClientUtils.RestCallResults res =RestClientUtils.sendPut(m_url+"/products/seasons/" + seasonID + "/inputschema", schemaJson, sessionToken);
		String schema = res.message;
		return schema;
	}
	
	public String getInputSample(String seasonID, String stage, String minAppVersion, String sessionToken, String generationMode, Double randomizeNumber) throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/products/seasons/" + seasonID + "/inputsample?stage=" + stage + "&minappversion=" + minAppVersion + "&generationmode=" + generationMode + "&randomize=" + randomizeNumber, sessionToken);
		String sample = res.message;
		return sample;

	}
	public String validateSchema(String seasonID,String schema, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/products/seasons/" + seasonID + "/inputschema/validate",schema, sessionToken);
		return res.message;

	}
	
	public String updateInputSchemaWithForce(String seasonID, String schemaJson, boolean force, String sessionToken) throws IOException{
		RestClientUtils.RestCallResults res =RestClientUtils.sendPut(m_url+"/products/seasons/" + seasonID + "/inputschema?force=" + force, schemaJson, sessionToken);
		String schema = res.message;
		return schema;
	}




}
