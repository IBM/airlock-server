package com.ibm.airlock.engine;

import java.util.Map;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.engine.RhinoScriptInvoker;

public class RhinoScriptInvokerV2 extends RhinoScriptInvoker
{
	public RhinoScriptInvokerV2(JSONObject userContext) throws JSONException, InvokerException
	{
		super(userContext);
	}
    public RhinoScriptInvokerV2(Map<String, String> scriptObjects, String functions, boolean validations) throws InvokerException
    {
    	super(scriptObjects, functions, validations);
    }

    @Override
    protected void prepareContextObjects(StringBuilder ruleEngineContextBuffer, Map<String, String> scriptObject)
    {
        for (Map.Entry<String, String> e : scriptObject.entrySet())
        {
        	String key = e.getKey();
        	String value = e.getValue();
        	
        	ruleEngineContextBuffer.append("\nvar ");
        	ruleEngineContextBuffer.append(key);
            ruleEngineContextBuffer.append(" = ");
            ruleEngineContextBuffer.append(value);
            ruleEngineContextBuffer.append(";");
        }
    }
}
