package com.ibm.airlock.engine;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import com.ibm.airlock.Constants;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.ValidationException;
import com.ibm.airlock.engine.ScriptInvoker.InvokerException;

public class VerifyRule
{
	//------------------------
	public static class FunctionNames {
		public Set<String> oldFunctions, newFunctions;
		public String toString() { return "old: " + oldFunctions + ", new: " + newFunctions; }
	}
	//------------------------

	// combine context/functions/translations into a single JSON object for the script invoker
	public static JSONObject createContextObject(String sampleUserContext, String javascriptFunctions, String translations) throws JSONException
	{
		JSONObject userContext = new JSONObject(sampleUserContext);
		userContext.put(Constants.JSON_FIELD_JAVASCRIPT_UTILITIES, javascriptFunctions);
		userContext.put(Constants.JSON_FIELD_TRANSLATIONS, new JSONObject(translations));
		return userContext;
	}
	public static ScriptInvoker makeInvoker(String sampleUserContext, String javascriptFunctions, String enTranslations, boolean extraValidation, Environment env) throws JSONException, InvokerException
	{
		boolean legacy = (env.getVersion().i < Version.v2_5.i);
				
		JSONObject userContext = createContextObject(sampleUserContext, javascriptFunctions, enTranslations);
		if (extraValidation)
		{
			userContext.put(Constants.JSON_FIELD_VALIDATION_MODE, new JSONObject()); // this freezes the context & checks translation parameters
			if (legacy)
				userContext.put("_validate_translation_", new JSONObject());
		}

		return legacy ? new RhinoScriptInvokerV2(userContext) : new RhinoScriptInvoker(userContext);
	}
	// invoker with additional initialized variables
	public static ScriptInvoker makeInvoker(String sampleUserContext, String extraContext, String javascriptFunctions, String enTranslations, boolean extraValidation) throws JSONException, InvokerException
	{
		JSONObject userContext = createContextObject(sampleUserContext, javascriptFunctions, enTranslations);
		JSONObject extra = new JSONObject(extraContext);

		@SuppressWarnings("unchecked")
		Set<String> keys = extra.keySet();
		for (String key :  keys)
		{
			// verify input contains only JSONObjects
			JSONObject object = RhinoScriptInvoker.getJSONObject(extra, key);
			userContext.put(key, object);
		}

		if (extraValidation)
			userContext.put(Constants.JSON_FIELD_VALIDATION_MODE, new JSONObject());
	
		return new RhinoScriptInvoker(userContext);
	}
	static String error(String minVersion, String stage)
	{
		if (minVersion == null && stage == null)
			return "Validation error: ";
		else if (minVersion == null)
			return "Validation error in stage " + stage + ": ";
		else
			return "Validation error in minVersion " + minVersion + ", stage " +  stage + ": ";
	}

	// given JS rule and context/functions/translations, evaluate it to check its validity
	public static void checkValidity(String newRule, String sampleUserContext, String javascriptFunctions, String enTranslations,
			boolean maximalValidation, String minVersion, String stage, Environment env) throws ValidationException
	{
		if (newRule == null || newRule.isEmpty())
			return;

		try {

			ScriptInvoker invoker = makeInvoker(sampleUserContext, javascriptFunctions, enTranslations, true, env);
			ScriptInvoker.Output output = invoker.evaluate(newRule);
			if (output.result == ScriptInvoker.Result.ERROR)
				throw new Exception("Rule error: " + output.error);

			// verifyFields can fail when you protect a field which is not in the schema (if a && a.b && a.b.c )
			// We call it anyway, since we assume all input fields are present in the schema.
			// (optional schema fields are OK -  we run the validation on a maximal sample)
			if (maximalValidation)  // && output.result == ScriptInvoker.Result.TRUE)
				verifyFields(invoker, newRule);
		}
		catch (Exception e)
		{
			throw ValidationException.factory(error(minVersion, stage), e.toString());
		}
	}

	// verify rule using both minimal and maximal context.
	public static void fullRuleEvaluation(String rule, ScriptInvoker minimalInvoker, ScriptInvoker maximalInvoker) throws ValidationException
	{
		if (rule == null || rule.isEmpty())
			return;

		try {
			ScriptInvoker.Output output = maximalInvoker.evaluate(rule);
			if (output.result == ScriptInvoker.Result.ERROR)
				throw new Exception(output.error);

			verifyFields(maximalInvoker, rule);
		}
		catch (Exception e)
		{
			throw ValidationException.factory("Invalid rule: ", e.toString());
		}

		try {
			ScriptInvoker.Output output = minimalInvoker.evaluate(rule);
			if (output.result == ScriptInvoker.Result.ERROR)
				throw new Exception(output.error);
		}
		catch (Exception e)
		{
			throw ValidationException.factory("Invalid rule due to unverified optional fields: ", e.toString());
		}
	}

	// validate rule with additional initialization of variables
	public static void validateRuleWithAdditionalContext(String newRule,
			String minimalContext, String maximalContext, String extraContext, 
			String javascriptFunctions, String enTranslations) throws ValidationException
	{
		if (newRule == null || newRule.isEmpty())
			return;

		try {
			ScriptInvoker invoker = makeInvoker(maximalContext, extraContext, javascriptFunctions, enTranslations, true);
			ScriptInvoker.Output output = invoker.evaluate(newRule);
			if (output.result == ScriptInvoker.Result.ERROR)
				throw new Exception("Rule error: " + output.error);

			verifyFields(invoker, newRule);

			invoker = makeInvoker(minimalContext, extraContext, javascriptFunctions, enTranslations, true);
			output = invoker.evaluate(newRule);
			if (output.result == ScriptInvoker.Result.ERROR)
				throw new Exception("Rule error: " + output.error);
		}
		catch (Exception e)
		{
			throw new ValidationException(e.toString());
		}
	}

	/*
	// given JS configuration trigger+content and context/functions/translations, evaluate it to check its validity
	public static JSONObject checkConfigurationWithTrigger(String triggerString, String configString, String sampleUserContext, String javascriptFunctions, String enTranslations,
			boolean maximalValidation, String minVersion, String stage, Environment env) throws ValidationException
	{

		try {
			if (configString == null || configString.isEmpty())
				throw new Exception("missing configuration string");

			ScriptInvoker invoker = makeInvoker(sampleUserContext, javascriptFunctions, enTranslations, true, env);

			// evaluate rule and configuration together. This allows the configuration to act on variables introduced in the rule
			JSONObject out = invoker.evaluateRuleAndConfiguration(triggerString, configString); // null if not triggered

			if (out != null && maximalValidation)
				verifyFields(invoker, configString);

			return out;
		}
		catch (Exception e)
		{
			throw ValidationException.factory(error(minVersion, stage), e.getMessage());
		}
	}
	// given JS configuration trigger+content and maximalContext/functions/translations, check them both
	// maximal context is used, so it's safe to check the configuration even when the rule returns false.
	// variables and functions introduced in the rule will be recognized in the configuration.
	public static JSONObject checkBothConfigurationAndTrigger(String triggerString, String configString, String maximalContext, String javascriptFunctions, String enTranslations,
			boolean maximalValidation, String minVersion, String stage, Environment env) throws ValidationException
	{

		try {
			if (configString == null || configString.isEmpty())
				throw new Exception("missing configuration string");

			ScriptInvoker invoker = makeInvoker(maximalContext, javascriptFunctions, enTranslations, true, env);
			JSONObject out = invoker.evaluateBothRuleAndConfiguration(triggerString, configString);

			if (maximalValidation)
				verifyFields(invoker, configString);

			return out;
		}
		catch (Exception e)
		{
			throw ValidationException.factory(error(minVersion, stage), e.getMessage());
		}
	}*/

	// perform all validation tests on a ConfigurationRule/Configuration pair. requires both minimal & maximal samples
	public static JSONObject fullConfigurationEvaluation(String triggerString, String configString, String minimalContext, String maximalContext, String javascriptFunctions, String enTranslations,
			String minVersion, String stage, Environment env) throws ValidationException
	{

		JSONObject out = null;
		try {
			if (configString == null || configString.isEmpty())
				throw new Exception("missing configuration string");

			ScriptInvoker invoker = makeInvoker(maximalContext, javascriptFunctions, enTranslations, true, env);
			// evaluate rule and then configuration. This allows the configuration to act on variables introduced in the rule.
			out = invoker.evaluateBothRuleAndConfiguration(triggerString, configString);

			if (triggerString != null && !triggerString.isEmpty())
				verifyFields(invoker, triggerString);
			verifyFields(invoker, configString);
		}
		catch (Exception e)
		{
			throw ValidationException.factory(error(minVersion, stage), e.getMessage());
		}

		try {
	
			ScriptInvoker invoker = makeInvoker(minimalContext, javascriptFunctions, enTranslations, true, env);

			// evaluate configuration if rule succeeds on a minimal context. This makes sure optional context is protected.
			invoker.evaluateRuleAndConfiguration(triggerString, configString); // null if not triggered
		}
		catch (Exception e)
		{
			// translation errors have been caught in the first stage, so don't use ValidationException.factory()
			throw new ValidationException(error(minVersion, stage) + ": optional fields and subfields may need to be checked for existence in the rule: " + e.getMessage());
		}
		return out;
	}

	public static JSONObject fullConfigurationEvaluation(String triggerString, String configString,
			ScriptInvoker minimalInvoker, ScriptInvoker maximalInvoker) throws ValidationException
	{
		JSONObject out = null;
		try {
			if (configString == null || configString.isEmpty())
				throw new Exception("missing configuration string");
	
			// evaluate rule and then configuration. This allows the configuration to act on variables introduced in the rule.
			out = maximalInvoker.evaluateBothRuleAndConfiguration(triggerString, configString);
	
			if (triggerString != null && !triggerString.isEmpty())
				verifyFields(maximalInvoker, triggerString);
			verifyFields(maximalInvoker, configString);
		}
		catch (Exception e)
		{
			throw ValidationException.factory("Validation: ", e.getMessage());
		}

		try {
	
			// evaluate configuration if rule succeeds on a minimal context. This makes sure optional context is protected.
			minimalInvoker.evaluateRuleAndConfiguration(triggerString, configString); // null if not triggered
		}
		catch (Exception e)
		{
			// translation errors have been caught in the first stage, so don't use ValidationException.factory()
			throw new ValidationException("Validation: optional fields and subfields may need to be checked for existence in the rule: " + e.getMessage());
		}
		return out;
	}
	/*
	// given JS configuration and context/functions/translations, evaluate it to check its validity
	public static JSONObject checkConfiguration(String configString, String sampleUserContext, String javascriptFunctions, String enTranslations,
			boolean maximalValidation, String minVersion, String stage, Environment env) throws ValidationException
	{
		try {
			if (configString == null || configString.isEmpty())
				throw new Exception("missing configuration string");

			ScriptInvoker invoker = makeInvoker(sampleUserContext, javascriptFunctions, enTranslations, true, env);
			JSONObject out = invoker.evaluateConfiguration(configString);

			if (maximalValidation)
				verifyFields(invoker, configString);
			return out;
		}
		catch (Exception e)
		{
			throw new ValidationException(error(minVersion, stage) + e.toString());
		}
	}
*/
	// context/translations/functions are not allowed inside default configurations
	public static JSONObject checkDefaultConfiguration(String config) throws ValidationException
	{
		try {
			// default configuration is not evaluated but parsed as JSON
			// strict conversion: all strings must be quoted (including identifiers), and comments are not allowed.
			return new JSONObject(config, true);
		}
		catch (Exception e) {
			throw new ValidationException("The default configuration is not valid. Unlike output configurations in configuration rules, the default configuration must be a static JSON that does not contain context variables or JavaScript functions.\n" + config);
		}
	}

	static ArrayList<String> findFields(String newRule){
		newRule = Utilities.removeComments(newRule, false);
		// extract full field names from JavaScript rule with a regEx
		final String pattern = "(?:^|[^\\w\\d\\.])(context.[\\w\\d\\.]*)";
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(newRule);

		ArrayList<String> matches = new ArrayList<String>();
		while (m.find())
		{
			matches.add(m.group(1));
		}
		return matches;
	}

	// JavaScript will not throw errors on rules with invalid leaf names, so do a special check
	static void verifyFields(ScriptInvoker invoker, String newRule) throws ValidationException
	{
		ArrayList<String> matches = findFields(newRule);
		//System.out.println(matches);
		for (String match : matches)
		{
			//System.out.println("'" + match + "'");
			String test = match + " === undefined";
			ScriptInvoker.Output output = invoker.evaluate(test);
			if (output.result == ScriptInvoker.Result.TRUE)
				throw new ValidationException("Undefined field: " + match);
			if (output.result == ScriptInvoker.Result.ERROR)
				throw new ValidationException("Invalid field: " + match);
		}
	}

	public static JSONArray findMissingFields(ScriptInvoker invoker, String newRule) throws ValidationException
	{
		ArrayList<String> matches = findFields(newRule);
		JSONArray missingFields = new JSONArray();
		//System.out.println(matches);
		for (String match : matches)
		{
			//System.out.println("'" + match + "'");
			String test = match + " === undefined";
			ScriptInvoker.Output output = invoker.evaluate(test);
			if (output.result == ScriptInvoker.Result.TRUE)
				missingFields.add("Undefined field: " + match);
			if (output.result == ScriptInvoker.Result.ERROR)
				missingFields.add("Invalid field: " + match);
		}
		return missingFields;
	}

	// given old and new versions of JavaScript functions, extract added/deleted function names
	public static FunctionNames findChangedFunctions(String oldtext, String newtext)
	{
		Map<String, int[]> oldFuncs = findFunctions(oldtext);
		Map<String, int[]> newFuncs = findFunctions(newtext);
		FunctionNames out = new FunctionNames();
		out.oldFunctions = getDiff(oldFuncs, newFuncs);
		out.newFunctions = getDiff(newFuncs, oldFuncs);
		return out;
	}
/*
	public static FunctionNames findFunctionsInRule(FunctionNames functions, String rule)
	{
		FunctionNames out = new FunctionNames();
		out.oldFunctions = findFunctionSet(functions.oldFunctions, rule);
		out.newFunctions = findFunctionSet(functions.newFunctions, rule);
		return out;
	}
*/
	// find if a rule or configuration string uses any of the given function names
	public static Set<String> findFunctionsInRule(Set<String> set, String rule)
	{
		rule = Utilities.removeComments(rule, false);
		Set<String> out = new TreeSet<String>();
		for (String func : set)
		{
			String pattern = "(^|[^\\w\\d\\.])" + escapeMetaChars(func) + "\\s*\\(";

			Pattern p = Pattern.compile(pattern);
			Matcher m = p.matcher(rule);
			if (m.find())
				out.add(func);
		}
		return out;
	}

	// extract function names with a regular expression. overridden functions are counted.
	public static Map<String, int[]> findFunctions(String text)
	{
		text = Utilities.removeComments(text, false);
		final String pattern = "(?:^|[^\\w\\d\\.])function\\s+(\\w[\\w\\d]*)\\s*\\(";  
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(text);

		TreeMap<String, int[]> matches = new TreeMap<String, int[]>();
		while (m.find())
		{
			String key = m.group(1);
			int[] val = matches.get(key);
			if (val == null)
				val = new int[1];

			++val[0];
			matches.put(key, val);
		}
		return matches;
	}

	// get set difference
	static Set<String> getDiff(Map<String, int[]> s1, Map<String, int[]> s2)
	{
		Set<String> out = new TreeSet<String>(s1.keySet());
		out.removeAll(s2.keySet());
		return out;
	}

	// used on configurations.
	public static boolean findTranslationId(String id, String config)
	{
		config = Utilities.removeComments(config, true);
		final String pattern = "(?:^|[^\\w\\d\\.])translate\\s*\\(\\W*" + escapeMetaChars(id) + "\\W+";
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(config);
		return m.find();
	}

	// used on configurations and utilities. adds quote marks to each id and does a simple search.
	// useEscape=true if the text comes from a configuration.
	public static Set<String> findAllTranslationIds(Set<String> ids, String text, boolean useEscape)
	{
		text = Utilities.removeComments(text, true);
		Set<String> out = new TreeSet<String>();
		for (String id : ids)
		{
			String quoted = "\"";
			quoted += useEscape ? escapeMetaChars(id) : id;
			quoted += "\"";
			if (text.contains(quoted))
				out.add(id);
		}
		return out;
	}
	static String escapeMetaChars(String in)
	{
		StringBuilder b = new StringBuilder();

		for (int i = 0; i < in.length(); ++i)
		{
			char ch = in.charAt(i);
			switch (ch)
			{
			case '.' :
			// add other cases here
				b.append("\\");

			// fall through
			default: b.append(ch);
			}
		}
		return b.toString();
	}

	// extract function names and parameters with a regular expression
	public static TreeMap<String, String[]> findFunctionSignatures(String text)
	{
		text = Utilities.removeComments(text, false);
		final String pattern = "(?:^|[^\\w\\d\\.])function\\s+(\\w[\\w\\d]*)\\s*\\(([^\\)]*)\\)";  
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(text);

		TreeMap<String,String[]> out = new TreeMap<String,String[]>();
		while (m.find())
		{
			String func = m.group(1);
			String parms = m.group(2).trim();
			String[] parmList = (parms.isEmpty()) ? new String[0] :  parms.split("\\s*,\\s*");
			out.put(func, parmList);
		}
		return out;
	}

	//---------------------------------------------------------------------
	//test
	public static void main(String[] args)
	{

		try {
/*
			String oldFuncs = Utilities.readString("C:/client/v/functions1.txt");
			String newFuncs = Utilities.readString("C:/client/v/functions2.txt");
			String rule = Utilities.readString("C:/client/v/rule.txt");
			
			FunctionNames fn = findChangedFunctions(oldFuncs, newFuncs);
			System.out.println(fn);
			
			Set<String> a = findFunctionsInRule(fn.oldFunctions, rule);
			System.out.println(a);
			
			Set<String> b = findFunctionsInRule(fn.newFunctions, rule);
			System.out.println(b);
*/

/*
			String translations = "{}";
			String functions = "function deepFreeze() {}";
			checkValidity(" (a && a.b && a.b.c) !== undefined", "{ 'a' : { 'a' : 5 } }", functions, translations);
			System.out.println("ok");
*/

			String functions = Utilities.readString("C:/client/v/functions.txt");
			String maxcontext = Utilities.readString("C:/client/v21/maxsample2.json");
			//String mincontext = Utilities.readString("C:/client/amitai/minimalSample.json");
			String translations = Utilities.readString("C:/client/v/translations.json");

			//String config = Utilities.readString("C:/client/v/checkConfig.txt");
			//"{'test': translate('id1', context.name) }"

			//String rule = " if(context.testData && context.testData.dsx && context.testData.dsx.teaserTitle){context.testData.dsx.teaserTitle == \"hi\"}else{false}";
			//String rule = " context.testData.dsx = \"aaa\"; true;";
			String rule = Utilities.readString("C:/client/v21/rule.txt");
			String minVer = "1.1";
			String stage = "DEVELOPMENT";

			Environment env = new Environment();
			env.setServerVersion("2.1");

		//	maxcontext = " { \"context\" : {\"testData\" : {\"dsx\" : {}}}} ";
			checkValidity(rule, maxcontext, functions, translations, true, minVer, stage, env);
			//checkValidity(rule, mincontext, functions, translations, false, minVer, stage, env);
			//JSONObject json = checkConfiguration(config, context, functions, translations, true, minVer, stage);
			//System.out.println(json.write(true));
/*
			TreeMap<String, String[]> map = findFunctionSignatures(functions);
			for (Map.Entry<String, String[]> item : map.entrySet())
			{
				System.out.print(item.getKey());
				System.out.print(": ");
				System.out.println(Arrays.asList(item.getValue()));
			}
*/
		}
		catch (Exception e) {
			e.printStackTrace();
		}

	}

}
