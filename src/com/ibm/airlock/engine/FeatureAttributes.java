package com.ibm.airlock.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import com.ibm.airlock.Constants;
import com.ibm.airlock.admin.BaseAirlockItem;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.engine.ScriptInvoker.InvokerException;
import com.ibm.airlock.admin.BaseAirlockItem.Type;

// get a flat list of all possible attribute names for each feature

// arraySpec contains array specifications: which items of an array should be displayed.
// if arraySpec == null, show all items of all arrays
// if initialized, but a specification for current array is not found, show the first item of the array.
// else, show specific items for the array. Example format: { "some.path.to.array" : "1,3,5-7,10" }

public class FeatureAttributes
{
	// split the attribute or constraint path into names and array elements using . and [
	// the ] is kept in the output as a marker to indicate array parts.
	static final String SPLITTER = "[\\[\\.]";

	//------------------------------------------------------------------------------------
	class ScanFeatures extends Visit
	{
		boolean useIds;
		TreeMap<String, TreeSet<String>> featureMap;
		TreeMap<String,String> arraySpec;

		ScanFeatures(boolean useIds, TreeMap<String,String> arraySpec)
		{
			visiting = FEATURES;
			featureMap = new TreeMap<String, TreeSet<String>>();
			this.useIds = useIds;
			this.arraySpec = arraySpec;
		}

		// visit one feature
		protected Object visit(JSONObject obj, Object state) throws Exception
		{
			BaseAirlockItem.Type type = getNodeType(obj);
			if (type == Type.FEATURE || type == Type.PURCHASE_OPTIONS || type == Type.ENTITLEMENT)
			{
				String featureName = useIds ? getId(obj) : getName(obj);

				// start with attributes of default configuration
				String str = obj.optString(Constants.JSON_FEATURE_FIELD_DEFAULT_CONFIG, "{}");
				JSONObject defConfig = new JSONObject(str);
				ScanAttributes scanAttr = new ScanAttributes(defConfig, arraySpec);

				// add attributes of the configuration tree
				scanAttr.run(obj, null);

				// put them under feature name
				featureMap.put(featureName, scanAttr.attrMap);
			}
			return null;
		}
	}
	//------------------------------------------------------------------------------------
	class ScanAttributes extends Visit
	{
		TreeSet<String> attrMap;
		TreeMap<String,String> arraySpec;

		ScanAttributes(JSONObject defaultAttr, TreeMap<String,String> arraySpec) throws JSONException
		{
			visiting = CONFIGURATIONS;
			this.arraySpec = arraySpec;

			attrMap = new TreeSet<String>();
			Utilities.flatten(defaultAttr, null, attrMap, arraySpec);
		}

		// visit one configuration
		protected Object visit(JSONObject obj, Object state) throws Exception
		{
			BaseAirlockItem.Type type = getNodeType(obj);
			if (type == Type.CONFIGURATION_RULE)
			{
				// configuration must be evaluated to produce a legal JSON
				String configString = obj.getString(Constants.JSON_FEATURE_FIELD_CONFIGURATION);
				//JSONObject config = invoker.evaluateConfiguration(configString);
				
				String ruleString = "";
				if (obj.containsKey(Constants.JSON_FEATURE_FIELD_RULE)) {
					JSONObject ruleObj = obj.getJSONObject(Constants.JSON_FEATURE_FIELD_RULE);
					if (ruleObj.containsKey(Constants.JSON_RULE_FIELD_RULE_STR) && ruleObj.get(Constants.JSON_RULE_FIELD_RULE_STR)!=null)
						ruleString = ruleObj.getString(Constants.JSON_RULE_FIELD_RULE_STR);						
				}
				JSONObject config = invoker.evaluateRuleAndConfiguration(ruleString, configString);
				// flatten all property names into a set
				Utilities.flatten(config, null, attrMap, arraySpec);
			}
			return null;
		}
	}

	//------------------------------------------------------------------------------------

	ScriptInvoker invoker;

	public FeatureAttributes(String maxUserSample, String javascriptFunctions, String enTranslations, Environment env) throws JSONException, InvokerException
	{
		invoker = VerifyRule.makeInvoker(maxUserSample, javascriptFunctions, enTranslations, false, env);
	}

	public Map<String, TreeSet<String>> getAttributes(JSONObject features, JSONObject entitlements, boolean useIds, TreeMap<String,String> arraySpec) throws Exception
	{
		ScanFeatures scan = new ScanFeatures(useIds, arraySpec);
		if (features!=null) {
			scan.run(features, null);
		}
		if (entitlements!=null) {
			scan.run(entitlements, null);
		}
		return scan.featureMap;
	}

	static BaseAirlockItem.Type getNodeType(JSONObject obj) throws JSONException
	{
		String str = obj.getString(Constants.JSON_FEATURE_FIELD_TYPE);
		return Utilities.valueOf(BaseAirlockItem.Type.class, str); // null on error
	}
	static String getName(JSONObject obj) throws JSONException
	{
		BaseAirlockItem.Type type = getNodeType(obj);
		if (type == BaseAirlockItem.Type.ROOT)
			return "root";

		if (type == BaseAirlockItem.Type.CONFIG_MUTUAL_EXCLUSION_GROUP || type == BaseAirlockItem.Type.MUTUAL_EXCLUSION_GROUP)
			return "mx." +  obj.optString(Constants.JSON_FIELD_UNIQUE_ID, "<unknown>"); 

		String name = obj.optString(Constants.JSON_FIELD_NAME, "<unknown>");
		String namespace = obj.optString(Constants.JSON_FEATURE_FIELD_NAMESPACE, "<unknown>"); 
		return namespace + "." + name;
	}
	static String getId(JSONObject obj) throws JSONException
	{
		return obj.getString(Constants.JSON_FIELD_UNIQUE_ID);
	}
	static String getRuleString(JSONObject json)
	{
		json = json.optJSONObject(Constants.JSON_FEATURE_FIELD_RULE);
		return  (json == null) ? "" : json.optString(Constants.JSON_RULE_FIELD_RULE_STR);
	}

	//-------------------------------------------------------------
	// filter and return all attributes that match the constraint.
	// constraint example:  "aa.bb[0-2,4].cc"  (accept items 0,2,3,4 from the array aa.bb[] )
	public static TreeSet<String> pruneAttributes(TreeSet<String> attributes, String allConstraints) throws Exception
	{
		Constraint[] constraints = parseConstraint(allConstraints);

		TreeSet<String> out = new TreeSet<String>();
		for (String path : attributes)
		{
			if (isAccepted(path, constraints, false))
				out.add(path);
		}
		return out;
	}

	//-------------------------------------------------------------
	// if one of the attributes matches all the non-array constraints, return an enumeration
	// of the constraint, that contains all the array indices that appear in it, one by one.
	public static TreeSet<String> enumerateConstraint(TreeSet<String> attributes, String allConstraints, boolean checkForExistence) throws Exception
	{
		Constraint[] constraints = parseConstraint(allConstraints);
		TreeSet<String> out = new TreeSet<String>();

		if (checkForExistence)
		{
			boolean accepted = false;
			for (String path : attributes)
			{
				if (isAccepted(path, constraints, true))
					{ accepted = true; break; }
			}
			if (!accepted)
				return out;
		}

		enumerateConstraint(constraints, 0, "", out);
		return out;
	}
	static void enumerateConstraint(Constraint[] constraints, int index, String path, TreeSet<String> out)
	{
		if (index == constraints.length)
		{
			out.add(path);
			return;
		}

		Constraint c = constraints[index];
		++index;

		if (c.selection == null)
		{
			String newPath = path.isEmpty() ? c.key : path + "." + c.key;
			enumerateConstraint(constraints, index, newPath, out);
		}
		else
		{
			for (int ix : c.selection) // [] (select all) is not allowed anymore
			{
				String newPath = path + "[" + ix + "]";
				enumerateConstraint(constraints, index, newPath, out);
			}
		}
	}
	public static Constraint[] parseConstraint(String arrayConstraint) throws Exception
	{
		// split the constraint into names and array elements using . and [  (the ] is kept as a differentiator)
		ArrayList<Constraint> constraints = new ArrayList<Constraint>();
		String[] parts = arrayConstraint.split(SPLITTER);
		for (String part : parts)
		{
			constraints.add(new Constraint(part));
		}

		return constraints.toArray(new Constraint[constraints.size()]);
	}
	static class Constraint
	{
		String key = "";
		TreeSet<Integer> selection = null;

		Constraint(String str) throws Exception
		{
			if (str.isEmpty())
				throw new Exception("Invalid constraint");

			int last = str.length() - 1;
			if (str.charAt(last) == ']') // an array element selector
			{
				str = str.substring(0, last);
				selection = str.isEmpty() ? new TreeSet<Integer>() : parseElementSelector(str);
			}
			else
				key = str;
		}
	}

	static TreeSet<Integer> parseElementSelector(String spec) throws Exception
	{
		TreeSet<Integer> selection = new TreeSet<Integer>();
		for (String item : spec.split("\\s*,\\s*"))
		{
			String[] spair = item.split("\\s*-\\s*");
			int[] pair = parsePair(spair);
			if (pair == null)
				throw new Exception("Invalid constraint");
	
			for (int i = pair[0]; i <= pair[1]; ++i)
				selection.add(i);
		}
		return selection;
	}
	static int[] parsePair(String[] pair)
	{
		if (pair.length != 1 && pair.length != 2)
			return null;

		try {
			int[] out = new int[2];
			for (int i = 0; i < pair.length; ++i)
			{
				out[i] = Integer.parseInt(pair[i]);
				if (out[i] < 0)
					return null;
			}

			if (pair.length == 1)
				out[1] = out[0];
			return out;
		}
		catch (Exception e)
		{
			return null;
		}
	}

	public static boolean isAccepted(String path, Constraint[] constraints, boolean acceptAllIndices)
	{
		String[] parts = path.split(SPLITTER);
		for (int index = 0; index < constraints.length; ++index)
		{
			if (index == parts.length)
				return false; // path is shorter than the constraint

			String part = parts[index];
			Constraint constraint = constraints[index];

			if (part.isEmpty())
				return false; // ill-formed path. Throw exception?

			int last = part.length() - 1;
			if (part.charAt(last) == ']')
			{
				if (constraint.selection == null)
					return false; // input has array, constraint does not

				if (acceptAllIndices)
					continue;

				int arrayPart = -1;
				try {
					arrayPart = Integer.parseInt(part.substring(0, last));
				}
				catch (Exception e)
				{}

				if (arrayPart < 0)
					return false; // ill-formed path. Throw exception?

				if (!constraint.selection.isEmpty()) // an empty selection means all indexes are allowed
					if (!constraint.selection.contains(arrayPart))
						return false; // remove this array index from the output
			}
			else
			{
				if (!part.equals(constraint.key))
					return false; // key constraint is not matched to input
			}
		}
		return true;
	}

	//---------------------------------------------------------------------

	public static void validateAttributePath(String path) throws Exception
	{
		String err = doValidate(path);
		if (err != null)
			throw new Exception("validation error for string " + path + " : " + err);
	}
	static String doValidate(String path)
	{
		if (path == null || path.isEmpty())
			return "empty string";

		if (!okChar(path.charAt(0), 0))
			return "starts with invalid character";

		char lastChar = path.charAt(path.length()-1);
		if (lastChar == '.' || lastChar == '[')
			return "ends with invalid character"; // other endings checked below

		int nleft = 0;
		int nright = 0;
		for (char c : path.toCharArray())
		{
			boolean special = false;
			if ( c == '[') { ++nleft; special = true; }
			else if ( c == ']') { ++nright; special = true; }
			else if (c == '.') special = true;

			int diff = nleft - nright;
			if (diff != 0 && diff != 1)
				return "contains embedded brackets";

			if (special || Character.isAlphabetic(c) || Character.isDigit(c))
				continue;

			return "contains invalid characters or spaces";
		}

		if (nleft != nright)
			return "contains unbalanced brackets";

		if (path.contains("..") || path.contains(".[") || path.contains(".]"))
			return "contains invalid sequences of .[]"; // this is also caught as empty tokens but we want to be explicit

		// split by [ and . , leaving ] as a marker
		String[] tokens = path.split(SPLITTER);
		if (tokens.length == 0)
			return "contains no tokens";

		for (String token : tokens)
		{
			if (token.isEmpty())
				return "contains empty tokens";

			int last = token.length() - 1;
			if (token.charAt(last) == ']')
			{
				if (last == 0)
					return "contains empty tokens";

				for (int i = 0; i < last; ++i)
					if (!Character.isDigit(token.charAt(i)))
						return "contains invalid array index [" + token;
			}
			else
			{
				for (int i = 0; i < token.length(); ++i)
				{
					if (!okChar(token.charAt(i), i))
						return "contains invalid key " + token;
				}
			}
		}
		return null;
	}
	static boolean okChar(char c, int index)
	{
		if (index > 0 && Character.isDigit(c))
			return true;

		return (c == '$' || c == '_' || Character.isAlphabetic(c));
	}
	
	// merge attribute paths by replacing each occurrence of array index [nnn] by [0]
	public static TreeSet<String> mergePaths(TreeSet<String> paths) throws Exception
	{
		TreeSet<String> out = new  TreeSet<String>();
		for (String str : paths)
		{
			String newStr = str.replaceAll("\\[\\d+\\]", "[0]");
			out.add(newStr);
		}
		return out;
	}
	
	//-------------------------------------------------------------
	// if one of the attributes matches all the non-array constraints, return an enumeration
	// of the constraint, that contains all the array indices that appear in it, one by one.
	public static JSONObject getWhitelistData(JSONObject context, TreeSet<String> whitlelist) throws Exception
	{
		JSONObject out = new JSONObject();
		for (String key : whitlelist)
		{
			Constraint[] constraints = parseConstraint(key);
			Object obj = locateConstraint(context, constraints);
			if (obj != null)
				out.put(key, obj.toString());
		}
		return out;
	}
	static Object locateConstraint(Object obj, Constraint[] constraints) throws Exception
	{
		for (int i = 0; i < constraints.length; ++i)
		{
			Constraint c = constraints[i];
			if (c.selection != null)
			{
				if (c.selection.isEmpty() || !(obj instanceof JSONArray))
					return null;

				JSONArray array = (JSONArray) obj;
				obj = array.opt(c.selection.first());
				if (obj == null)
					return null;
			}
			else
			{
				if (!(obj instanceof JSONObject))
					return null;
				JSONObject map = (JSONObject) obj;
				obj = map.opt(c.key);
				if (obj == null)
					return null;
			}
		}
		return obj;
	}
	//---------------------------------------------------------------------
	public enum Jtype { MAP, ARRAY, STRING, NUMBER, BOOLEAN, NULL };
	public Map<String, TreeSet<Jtype>> getFeatureAttributeTypes(JSONObject feature) throws Exception
	{
		BaseAirlockItem.Type type = getNodeType(feature);
		if (type != Type.FEATURE && type != Type.ENTITLEMENT && type != Type.PURCHASE_OPTIONS)
			throw new Exception("JSONObject is not a feature node");

		TreeMap<String,TreeSet<Jtype>> out = new TreeMap<String,TreeSet<Jtype>>();

		String str = feature.optString(Constants.JSON_FEATURE_FIELD_DEFAULT_CONFIG, "{}");
		JSONObject defConfig = new JSONObject(str);
		flattenAttr(defConfig, null, out);

		ScanAttributeTypes scanner = new ScanAttributeTypes(out);
		scanner.run(feature, null);

		//TreeMap<String, String> formatted = new TreeMap<String, String>();
		for (Map.Entry<String,TreeSet<Jtype>> ent : out.entrySet())
		{
			//String key = ent.getKey();
			TreeSet<Jtype> types = ent.getValue();

			if (types.size() > 1 && types.contains(Jtype.NULL))
				types.remove(Jtype.NULL);
			
			//formatted.put(key, types.toString());
		}

		return out;
	}
	static void flattenAttr(Object obj, String parent, TreeMap<String,TreeSet<Jtype>> out) throws JSONException
	{
		Jtype type = getType(obj);
		addType(out, parent, type);

		if (type == Jtype.MAP)
		{
			String prefix = (parent == null) ? "" : parent + ".";
			JSONObject attr = (JSONObject) obj;
			@SuppressWarnings("unchecked")
			Set<String> keys = attr.keySet();
			for (String str : keys)
			{
				flattenAttr(attr.get(str), prefix + str, out);
			}
		}
		else if (type == Jtype.ARRAY)
		{
			String prefix = (parent == null) ? "[" : parent + "[";
			JSONArray arr = (JSONArray) obj;
			for (int i = 0; i < arr.length(); ++i)
			{
				flattenAttr(arr.get(i), prefix + i + "]", out);
			}
		}
	}
	static Jtype getType(Object obj)
	{
		if (obj instanceof JSONArray)
			return Jtype.ARRAY;
		if (obj instanceof JSONObject)
			return Jtype.MAP;
		if (obj instanceof String)
			return Jtype.STRING;
		if (obj instanceof Boolean)
			return Jtype.BOOLEAN;
		if (obj instanceof Integer || obj instanceof Long || obj instanceof Double || obj instanceof Short) // Float converts to Double
			return Jtype.NUMBER;
		return Jtype.NULL;
		
	}
	static void addType(TreeMap<String,TreeSet<Jtype>> out, String path, Jtype type)
	{
		if (path == null)
			return; // top level ignored
		
		TreeSet<Jtype> types = out.get(path);
		if (types == null)
		{
			types = new TreeSet<Jtype>();
			out.put(path, types);
		}
		types.add(type);
	}
	class ScanAttributeTypes extends Visit
	{
		TreeMap<String,TreeSet<Jtype>> out;

		ScanAttributeTypes(TreeMap<String,TreeSet<Jtype>> out) throws JSONException
		{
			visiting = CONFIGURATIONS; // FEATURES | CONFIGURATIONS; (called for a single feature and its configurations, not for features below the top feature)
			this.out = out;
		}
		protected Object visit(JSONObject obj, Object state) throws Exception
		{
			BaseAirlockItem.Type type = getNodeType(obj);
			if (type == Type.FEATURE || type == Type.ENTITLEMENT || type == Type.PURCHASE_OPTIONS)
			{
				String ruleString = getRuleString(obj);
				invoker.evaluate(ruleString); //evaluate the rule so if variable is declared in the features rule, the configuration attribute will be able to use it
			}
			else if (type == Type.CONFIGURATION_RULE)
			{
				// configuration must be evaluated to produce a legal JSON
				String ruleString = getRuleString(obj);
				String configString = obj.getString(Constants.JSON_FEATURE_FIELD_CONFIGURATION);
				JSONObject config = invoker.evaluateBothRuleAndConfiguration(ruleString, configString);
				// flatten all property types into a set
				flattenAttr(config, null, out);
			}
			return null;
		}
	}
	//---------------------------------------------------------------------
	public static JSONObject intersectSamples(List<JSONObject> samples) throws JSONException
	{
		if (samples == null || samples.isEmpty())
			return null;

		if (samples.size() == 1)
			return samples.get(0);

		Object out = Utilities.cloneJson(samples.get(0), true);

		for (int i = 1; i < samples.size(); ++i)
		{
			intersectJson(out, samples.get(i));
		}
		return (JSONObject) out;
	}
	static void intersectJson(Object one, Object two)
	{
		if (one instanceof JSONObject)
			intersectMap((JSONObject) one, (JSONObject) two); // earlier we made sure the one/two types match
		else if (one instanceof JSONArray)
			intersectArray((JSONArray) one, (JSONArray) two);
	}
	static void intersectMap(JSONObject one, JSONObject two)
	{
		@SuppressWarnings("unchecked")
		Set<String> keys = new TreeSet<String>(one.keySet()); // make fresh copy since original is modified

		for (String key : keys)
		{
			if (equalNodeTypes(one, two, key))
				intersectJson(one.opt(key), two.opt(key));
			else
				one.remove(key);
		}
	}
	static void intersectArray(JSONArray one, JSONArray two)
	{
		int len1 = one.size();
		int len2 = two.size();

		// shorten the array to the smaller size
		if (len1 > len2)
		{
			for (int i = len1 - 1; i >= len2; --i)
				one.remove(i);
		}

		boolean mismatch = false;
		for (int i = 0; i < one.size(); ++i)
		{
			if (!equalNodeTypes(one.opt(i), two.opt(i)))
			{
				mismatch = true;
				break;
			}
		}

		if (mismatch)
		{
			one.clear();
		}
		else for (int i = 0; i < one.size(); ++i)
		{
			intersectJson(one.opt(i), two.opt(i));
		}
	}
	static boolean equalNodeTypes(JSONObject one, JSONObject two, String key)
	{
		if (!two.containsKey(key))
			return false;

		return equalNodeTypes(one.opt(key), two.opt(key));
	}

	static boolean equalNodeTypes(Object o1, Object o2)
	{
		Jtype t1 = getType(o1);
		Jtype t2 = getType(o2);

		if (t1 == t2)
			return true;

		// accept situations where one of the nodes is null and the other is a basic type
		if (t1 == Jtype.NULL && t2 != Jtype.MAP && t2 != Jtype.ARRAY)
			return true;
		if (t2 == Jtype.NULL && t1 != Jtype.MAP && t1 != Jtype.ARRAY)
			return true;

		return false;
	}

	//---------------------------------------------------------------------

	//test
	public static void main(String[] args)
	{
		try {
			/*
			TreeSet<Jtype> types = new TreeSet<Jtype>();
			types.add(Jtype.MAP);
			types.add(Jtype.ARRAY);
			types.add(Jtype.NUM);
			String zzz = types.toString();
			*/
	/*
			TreeSet<String> in = new TreeSet<String>();
			in.add("a.b[0].c");
			String constraint = "a.b[0,4-6].c";
			TreeSet<String> out = enumerateConstraint(in, constraint, true);
			System.out.println(out);
			*/
			JSONObject j1 = Utilities.readJson("C:/client/viki/samples/ProfileV2.json");
			//String cont = j1.write(true);
			//Utilities.writeString(cont, "C:/client/viki/samples/ProfileV2.json");
			JSONObject j2 = Utilities.readJson("C:/client/viki/samples/ProfileV2.2.json");
			//cont = j1.write(true);
			//Utilities.writeString(cont, "C:/client/viki/samples/ProfileV2.2.json");
			
			ArrayList<JSONObject> arr = new ArrayList<JSONObject>();
			arr.add(j1);
			arr.add(j2);
			JSONObject out = intersectSamples(arr);
			String content = out.write(true);
			Utilities.writeString(content, "C:/client/viki/samples/ProfileV2.out.json");

/*			
			String functions = Utilities.readString("C:/client/v/functions.txt");
			String context = Utilities.readString("C:/client/v/context.json");
			String translations = Utilities.readString("C:/client/v/translations.json");
			JSONObject config = Utilities.readJson("C:/client/v/AirlockFeatures.json").getJSONObject("root");

			Environment env = new Environment();
			env.setServerVersion("2");
			FeatureAttributes fa = new FeatureAttributes(context, functions, translations, env);
			Map<String, TreeSet<String>> map = fa.getAttributes(config, false, null);
			System.out.println(map);
			*/
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
