package com.ibm.airlock.engine;

import java.util.Map;
import org.apache.wink.json4j.JSONObject;

public abstract class ScriptInvoker
{
	Map<String,String> scriptObjects;
	public enum Result { TRUE, FALSE, ERROR }
	static boolean debugMode = false;

	static public void setDebugMode(boolean b)
	{
		debugMode = b;
	}

	protected final String DISTANCE_FUNC = 

			" function calcDistance(coords1, coords2, isMiles) { \n" +

			"function toRad(x) { return x * Math.PI / 180; };\n" +

			"var lon1 = coords1[0]; var lat1 = coords1[1]; \n" +
			"var lon2 = coords2[0]; var lat2 = coords2[1]; \n" +
			"var x1 = lat2 - lat1; var dLat = toRad(x1);\n" +
			"var x2 = lon2 - lon1; var dLon = toRad(x2);\n" +
			"var a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + \n" +
			" Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * \n" +
			" Math.sin(dLon / 2) * Math.sin(dLon / 2);\n" +
			"var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));\n" +
			"var d = 6371 * c;\n" + // earth in km

			"if (isMiles) d /= 1.60934;\n" +

			"return d; }";

	static public class Output
	{
		public Result result;
		public String error;
		public Output(Result result, String error) {
			this.result = result; this.error = error;
		}
		public Output(Result result) {
			this.result = result; this.error = "";
		}
	}
	static public class InvokerException extends Exception
	{
		private static final long serialVersionUID = 1L;
		InvokerException(String str) {
			super(str);
		}
	}

	// the map contains key/value pairs, where the key is the name of a JavaScript object ("profile", "context", etc)
	// and the value is the JSON string that will construct it.
	public ScriptInvoker(Map<String,String> scriptObjects)
	{
		this.scriptObjects = scriptObjects;
	}

	// construct a JavaScript snippet from the query and the script objects, invoke it and return true/false/error
	public abstract Output evaluate(String query);
	public abstract ScriptInvoker clone();
	public abstract JSONObject evaluateConfiguration(String configString) throws InvokerException;
	public abstract JSONObject evaluateRuleAndConfiguration(String rule, String configString) throws InvokerException;
	public abstract JSONObject evaluateBothRuleAndConfiguration(String rule, String configString) throws InvokerException;
}
