package com.ibm.airlock.utilities;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.engine.Environment;
import com.ibm.airlock.engine.ScriptInvoker;
import com.ibm.airlock.engine.VerifyRule;
import tests.com.ibm.qautils.RestClientUtils;

public class InsertPmml
{
	static class Parms extends InsertOrdering.Config
	{
		String minAppVersion, functionPath;
		String pmml, rulePath, configurationPath, fieldMapPath;

		public Parms(String propertiesPath) throws Exception
		{
			super(propertiesPath);

			minAppVersion = properties.get("minAppVersion");
			pmml = properties.get("pmml");
			rulePath = properties.get("rulePath");
			configurationPath = properties.get("configurationPath");
			fieldMapPath = properties.get("fieldMapPath", null);
			functionPath = properties.get("functionPath", null);
		}
	}

	//---------------------------------------------------------------------
	static class Case
	{
		String predicate;
		String score;
		ArrayList<Case> children = new ArrayList<Case>();
	}
	//---------------------------------------------------------------------
	static class Regression
	{
		double intercept;
		LinkedHashMap<String,Double> coefficients = new LinkedHashMap<String,Double>(); // maintain original order for readability
	}
	//---------------------------------------------------------------------
	static class JsPath
	{
		String jsObject, pathInObject;
		public String toString() { return jsObject + "." + pathInObject; }
	}
	//---------------------------------------------------------------------
	Parms parms;
	Document doc;
	PrintWriter rule;
	PrintWriter config;
	ArrayList<String> inputVar = new ArrayList<String>();
	String outputVar;
	String functions;

	JSONObject fieldMapping = new JSONObject();
	TreeSet<String> models = new TreeSet<String>(Arrays.asList("TreeModel", "RegressionModel"));
	TreeSet<String> predicates = new TreeSet<String>(Arrays.asList("True", "False", "Predicate", "SimplePredicate", "CompoundPredicate"));
	HashMap<String,String> safeVariables = new HashMap<String,String>();
	TreeMap<String,String> tinyMap = new TreeMap<String,String>();

	static DecimalFormat df = new DecimalFormat("#.####");
	static {
		df.setRoundingMode(RoundingMode.CEILING);
	}

	public static void main(String[] args)
	{
		if (args.length != 1)
		{
			System.out.println("usage: InsertPmml configurationFile");
			return;
		}

		try {
			InsertPmml ip = new InsertPmml(args[0]);
			ip.generateJS();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public InsertPmml(String confPath) throws Exception
	{
		parms = new Parms(confPath);
		functions = (parms.functionPath != null) ? Utilities.readString(parms.functionPath) : getServerFunctions();
		rule = new PrintWriter(parms.rulePath, "UTF-8"); 
		config = new PrintWriter(parms.configurationPath, "UTF-8");

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();

		// fieldMapping is an optional JSONObject that converts PMML fields to Airlock fields
		// if it does not exist, convert every _ to . (not a safe assumption)
		if (parms.fieldMapPath == null)
		{
			String content = Utilities.readString(parms.pmml);
			content = content.replace("_", ".");
			InputStream stream = new ByteArrayInputStream(content.getBytes("UTF-8"));
			doc = db.parse(stream);
		}
		else
		{
			doc = db.parse(parms.pmml); 
			JSONObject json = Utilities.readJson(parms.fieldMapPath);
			JSONObject map = json.optJSONObject("map");
			JSONArray list = json.optJSONArray("list");

			if (map != null)
				fieldMapping = map;

			if (list != null)
			{
				for (int i = 0; i < list.size(); ++i)
					fieldMapping.put("field_" + i, list.getString(i));
			}
		}

		NodeList nd = doc.getElementsByTagName("MiningField");
		for (int i = 0; i < nd.getLength(); ++i)
		{
			Element e = (Element) nd.item(i);
			String name = e.getAttribute("name");
			String usage = e.getAttribute("usageType");
			if (name.isEmpty())
				throw new Exception("MiningField without a 'name' attribute");

			name = mapField(name);
			if (usage.equals("predicted") || usage.equals("target") )
				outputVar = name;
			else
			{
				inputVar.add(name);
			}
		}

		optionalReplaceOutputVar();
		if (outputVar == null)
			throw new Exception("'predicted/target' MiningField not found");
	}
	void optionalReplaceOutputVar()
	{
		NodeList nd = doc.getElementsByTagName("OutputField");
		if (nd != null && nd.getLength() > 0)
		{
			Element e = (Element) nd.item(0);
			String name = e.getAttribute("name");
			if (name != null && !name.isEmpty())
				outputVar = name;
		}
	}
	public void generateJS() throws Exception
	{
		Element root = doc.getDocumentElement();
		ArrayList<Node> nodes = getDirectChildren(root, models);
		if (nodes.size() != 1)
			throw new Exception("expected one models tag, found " + nodes.size());

		Node tree = nodes.get(0);
		String model = tree.getNodeName();

		makeSafeVars();
		makeTinyVars();

		if (model.equals("TreeModel"))
		{
			Node top = getFirstItem(tree, "Node");
			Case items = parseDecisionTree(top);
			printRule(items);
			printConfiguration();
		}
		else if (model.equals("RegressionModel"))
		{
			Node top = getFirstItem(tree, "RegressionTable");
			Regression regression = parseRegressionNode(top);
			printRegressionRule(regression);
			printConfiguration();
		}
		else
			throw new Exception("unknown model " + model);
	}

	Case parseDecisionTree(Node node) throws Exception
	{
		Case item = new Case();
		item.predicate = getPredicate(node);

		ArrayList<Node> children = getDirectChildren(node, "Node");
		if (children.isEmpty())
			item.score = ((Element) node).getAttribute("score");
		else for (Node child : children)
		{
			item.children.add(parseDecisionTree(child));
		}
		return item;
	}

	String getPredicate(Node node) throws Exception
	{
		// modelName="logistic regression" functionName="classification" normalizationMethod="logit"
		ArrayList<Node> children = getDirectChildren(node, predicates);
		if (children.size() != 1)
			throw new Exception("expected one Predicate tag, found " + children.size());
//			return "True";

		return interpretPerdicate((Element) children.get(0));
	}

	String interpretPerdicate(Element child) throws Exception
	{
		String name = child.getNodeName();

		if (name.equals("True"))
			return "true";
		if (name.equals("False"))
			return "false";
		if (name.equals("Predicate") || name.equals("SimplePredicate"))
			return simplePredicate(child);
		if (name.equals("CompoundPredicate"))
			return compoundPredicate(child);

		throw new Exception("unknown tag " + name);
	}

	String simplePredicate(Element e) throws Exception
	{
		String field = e.getAttribute("field");
		String operator = e.getAttribute("operator");
		String value = e.getAttribute("value");
		if (field.isEmpty() || operator.isEmpty() || value.isEmpty())
			throw new Exception("missing 'field/operator/value' in Predicate tag");

		field = mapField(field); // convert the PMML variable to an airlock context variable
		field = getTiny(field); // get a short and safe version

		return field + " " + convertOperator(operator) + " " + safeString(value);
	}

	enum OP { equal, notEqual, lessThan , lessOrEqual ,  greaterThan , greaterOrEqual  };
	final String[] OPstring = { "==", "!=", "<", "<=", ">", ">="};
	String convertOperator(String name) throws Exception
	{
		OP op = Enum.valueOf(OP.class, name);
		return OPstring[ op.ordinal() ];
	}

	String compoundPredicate(Element e) throws Exception
	{
		String str = e.getAttribute("booleanOperator");
		String booleanOperator = convertCompound(str);

		ArrayList<Node> children = getDirectChildren(e, predicates);
		if (children.size() < 2)
			throw new Exception("missing children of CompoundPredicate tag");

		StringBuilder sb = new StringBuilder();
		for (Node n : children)
		{
			String item = interpretPerdicate((Element)n);

			if (sb.length() > 0)
				sb.append(booleanOperator);
			sb.append(" (");
			sb.append(item);
			sb.append(") ");
			
		}
		return sb.toString();
	}
	enum COMPOUND { or, and, xor, cascade };
	final String[] COMPOUNDstring = { " || ", " && ", "", ""};
	String convertCompound(String name) throws Exception
	{
		COMPOUND op = Enum.valueOf(COMPOUND.class, name);
		String val = COMPOUNDstring[ op.ordinal() ];
		if (val.isEmpty())
			throw new Exception(name + " compound predicate is not implemented yet");
		return val;
	}
	
	//----------------------------------------
	void printRule(Case item) throws Exception
	{
		// addMissingFields();
		// protectMissingFields(); // add customized isAvailable() function
		// makeSafeVars();

//		rule.print("if (isAvailable()) {  "); // do all requested optional variables appear in runtime context?
		rule.print("var decisionTree = ");
		printCase(item, "");
		rule.println(";");

		rule.println("var calculation_output = decisionTree.evaluate();");
		rule.println("calculation_output.match;"); // if match = true, the weight is in calculation_output.result

//		rule.println(" }\n else { false; } "); // requested optional variables are missing

		rule.close();
	}
	//----------------------------------------
	void protectMissingFields() throws Exception
	{
		// with maximal context, all variables should be found
		ArrayList<JsPath> vars = getVars();
		ScriptInvoker maximal = getInvoker(false);

		ArrayList<JsPath> checked = checkMissing(maximal, vars);
		if (checked.size() > 0)
			System.out.println("Warning: unknown context variables: " + checked);
//			throw new Exception("unknown context variables: " + checked); // remove the exception, allow unknown streams to be accepted

		// with minimal context, optional variables are missing but should be checked at runtime 
		ScriptInvoker minimal = getInvoker(true);
		checked = checkMissing(minimal, vars);
		rule.print("function isAvailable() {\n");

		for (JsPath item : checked)
		{
			rule.print("if (foundInContext("  + item.jsObject + " , " + JSstring(item.pathInObject) + ") == false) return false;\n");
		}
		rule.print("return true; }\n\n");
	}

	void addMissingFields() throws Exception
	{
		ArrayList<JsPath> vars = getVars();
		for (JsPath pp : vars)
		{
			rule.println("addMissingContext(" + pp.jsObject + " , " + JSstring(pp.pathInObject) + ");\n");
		}
	}

	void makeSafeVars() throws Exception
	{
		// with maximal context, all variables should be found
		ArrayList<JsPath> vars = getVars();
		ScriptInvoker maximal = getInvoker(false);

		ArrayList<JsPath> checked = checkMissing(maximal, vars);
		if (checked.size() > 0)
			System.out.println("Warning: unknown context variables: " + checked);
//			throw new Exception("unknown context variables: " + checked); // remove the exception, allow unknown streams to be accepted

		// with minimal context, optional variables are missing but should be checked at runtime 
		ScriptInvoker minimal = getInvoker(true);
		checked = checkMissing(minimal, vars);
		for (JsPath pp : checked)
		{
			// if a variable needs protection, enclose it in a safe() function
			// the space before the comma " , " is a hack to prevent verifyFields() from failing.
			// (it uses an imperfect RegExp that would extract the context variable with a comma appended to it)
			String safe = "safe(" + pp.jsObject + " , " + JSstring(pp.pathInObject) + ")";
			safeVariables.put(pp.toString(), safe);
		}
	}

	void makeTinyVars() throws Exception
	{
		rule.print("var tiny = {};\n");

		for (String var : inputVar)
		{
			String safeName = safeVariables.get(var);
			if (safeName == null)
				safeName = var; // variable is already safe, no need to use the safe() wrapper

			String shortName = "tiny.a" + tinyMap.size();
			rule.print(shortName + " = " + safeName + ";\n");

			tinyMap.put(var, shortName);
		}
		rule.print("\n");
	}

	String getTiny(String in) throws Exception
	{
		String out = tinyMap.get(in);
		if (out == null)
			throw new Exception("unexpected input variable " + in);
		return out;
	}

	void printCase(Case item, String pad) throws Exception
	{
		rule.print(pad + "new Case(" + item.predicate + ", ");
		pad = pad + "   ";

		if (item.children.isEmpty())
		{
			rule.print(safeString(item.score));
		}
		else if (item.children.size() == 1)
		{
			printCase(item.children.get(0), pad);
		}
		else
		{
			rule.println("Array(");
			int last = item.children.size() - 1;
			for (int i = 0; i <= last; ++i)
			{
				printCase(item.children.get(i), pad);
				if (i != last)
					rule.println(",");	
			}
			rule.print(")");
		}
		rule.print(")");
	}
	void printConfiguration() throws Exception
	{
	// NOTE: the final configuration is this: config.println("{ " + JSstring(outputVar) + " : calculation_output.result }");
	// however, the API expects a UUID instead of outputVar. So we put out a temporary configuration instead
		config.println(outputVar);
		config.println("calculation_output.result");
		config.close();
	}
	//----------------------------------------
	Regression parseRegressionNode(Node node) throws Exception
	{
		Regression regression = new Regression();
		Element e = (Element) node;
		String intercept = e.getAttribute("intercept");
		String targetCategory = e.getAttribute("targetCategory"); // TODO. for now assume the first category is ON
		if (intercept.isEmpty() || targetCategory.isEmpty())
			throw new Exception("missing intercept/targetCategory attributes");

		regression.intercept = Double.parseDouble(intercept);
		ArrayList<Node> children = getDirectChildren(e, "NumericPredictor");
		if (children.isEmpty())
			throw new Exception("missing NumericPredictor tags");
		
		for (Node child : children)
		{
			e = (Element) child;
			String field = e.getAttribute("name");
			String coefficient = e.getAttribute("coefficient");
			if (field.isEmpty() || coefficient.isEmpty())
				throw new Exception("missing name/coefficient attributes");

			field = mapField(field); // convert the PMML variable to an airlock context variable
			field = getTiny(field); // get a short and safe version
			double d =  Double.parseDouble(coefficient);
			regression.coefficients.put(field, d);
		}
		return regression;
	}
	void printRegressionRule(Regression regression) throws Exception
	{
		// addMissingFields();
		// protectMissingFields(); // add customized isAvailable() function
		// makeSafeVars();

		//y = intercept + Sumi (coefficienti * independent variablei )
		//p = 1/(1+exp(-y))
		
		//rule.print("if (isAvailable()) {  "); // do all requested optional variables appear in runtime context?

		rule.println("var regressionSum = " + regression.intercept + ";");
		for (Map.Entry<String,Double> ent : regression.coefficients.entrySet())
		{
			rule.println("regressionSum += " + JSstring(ent.getKey()) + " * " + ent.getValue() + ";");
		}
		rule.println("var regressionProbablity = 1. / (1. + Math.exp(- regressionSum ));");

		rule.println("var calculation_output = { 'match' : true, 'result' : regressionProbablity } ;" );
		rule.println("calculation_output.match;"); // for now always true

		//rule.println(" } else { false; } "); // requested optional variables are missing
		rule.close();
	}
	//----------------------------------------
	// utilities
	ArrayList<JsPath> getVars() throws Exception
	{
		ArrayList<JsPath> out = new ArrayList<JsPath>();
		for (String str : inputVar)
		{
			JsPath pp = new JsPath();
			pp.jsObject = "";
			pp.pathInObject = "";
			int pos = str.indexOf(".");
			if (pos > 0)
			{
				pp.jsObject = str.substring(0, pos);
				pp.pathInObject = str.substring(pos+1);
			}
			if (pp.jsObject.isEmpty())
				throw new Exception("missing top object in " + str);
			if (pp.pathInObject.isEmpty())
				throw new Exception("missing path in object " + str);
			out.add(pp);
		}
		return out;
	}
	Node getFirstItem(Node node, String tag) throws Exception
	{
		for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling())
		{
			if (child.getNodeName().equals(tag))
				return child;
		}
		throw new Exception("tag not found: " + tag);
	}
	ArrayList<Node> getDirectChildren(Node node, TreeSet<String> tags) throws Exception
	{
		ArrayList<Node> out = new ArrayList<Node>();
		for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling())
		{
			if (tags.contains(child.getNodeName()))
				out.add(child);
		}
		return out;
	}
	ArrayList<Node> getDirectChildren(Node node, String tag)
	{
		ArrayList<Node> out = new ArrayList<Node>();
		for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling())
		{
			if (child.getNodeName().equals(tag))
				out.add(child);
		}
		return out;
	}

	String safeString(String in)
	{
		if (in == null)
			return "null";

		// currently just check for booleans and numbers, otherwise assume string type
		if (in.equals("true") || in.equals("false"))
			return in;

		if (isNumeric(in))
		{
			Double d = Double.parseDouble(in);
			return df.format(d); // truncate to 5 positions
		}

		return JSstring(in);
	}
	boolean isNumeric(String str)  
	{  
		try {  
			Double.parseDouble(str);  
			return true;  
		}  
		catch (NumberFormatException e) {  
			return false;  
		}  
	}
	String JSstring(String value)
	{
        StringBuilder sb = new StringBuilder();
        sb.append('"');

        char[] chars = value.toCharArray();
        for (int i = 0; i < chars.length; ++i)
        {
            char c = chars[i];
            switch (c) {
                case  '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case    0: sb.append("\\0"); break;
                case '\b': sb.append("\\b"); break;
                case '\t': sb.append("\\t"); break;
                case '\n': sb.append("\\n"); break;
                case '\f': sb.append("\\f"); break;
                case '\r': sb.append("\\r"); break;
                case '/': sb.append("\\/"); break;
                default:
                    if ((c >= 32) && (c <= 126))
                        sb.append(c);
                    else {
                        sb.append("\\u");
                        sb.append(rightAlignedZero(Integer.toHexString(c),4));
                    }
            }
        }
        sb.append('"');
        return sb.toString();
	}
	String rightAlignedZero(String s, int len)
	{
        if (len == s.length())
        	return s;

        StringBuffer sb = new StringBuffer(s);
        while (sb.length() < len) {
            sb.insert(0, '0');
        }
        return sb.toString();
    }

	String mapField(String in) throws Exception
	{
		String out = fieldMapping.optString(in);
		if (out != null && !out.isEmpty())
			return out;
		return in;
	}


	String getSample(boolean minimal) throws Exception
	{
		String pp = "stage=" + parms.stage + "&minappversion=" + parms.minAppVersion + "&randomize=0.8&generationmode=" + (minimal ? "MINIMAL" : "MAXIMAL");
		if (parms.sessionToken != null)
			pp += "&sessionToken=" + parms.sessionToken;

		String call = parms.url + "products/seasons/" + parms.seasonId + "/inputsample?" + pp;
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(call);
		JSONObject obj = InsertOrdering.parseError(res.message, "get sample");
		return obj.toString();
	}


/* JavaScript functions
   ----------------------------
  	function foundInContext(obj, pathInsideObj) {

		if (typeof(obj) != "object")
			throw "top level object is missing";

		var arr = pathInsideObj.split(/[\.\[\]\"\']/);

		var filtered = [];
		for (var i = 0; i < arr.length; ++i)
		{
			var str = ("" + arr[i]).trim();
			if (str.length > 0)
				filtered.push(str);
		}

		for (var i = 0; i < filtered.length; ++i)
		{
			var part = filtered[i];
			if (!obj.hasOwnProperty(part)) 
				return false;
			obj = obj[part];
	    }
		return true;
	};
	 ----------------------------
	function addMissingContext(obj, pathInsideObj) {

		if (typeof(obj) != "object")
			throw "top level object is missing";
	
		// the path does not contain the top level
		var arr = pathInsideObj.split(/[\.\[\]\"\']/);
	
		var filtered = [];
		for (var i = 0; i < arr.length; ++i)
		{
			var str = ("" + arr[i]).trim();
			if (str.length > 0)
				filtered.push(str);
		}
	
		var last = filtered.length - 1;
		for (var i = 0; i <= last; ++i)
		{
			var part = filtered[i];
			if (!obj.hasOwnProperty(part)) {
				obj[part] = (i == last) ? 0 : {} ;
			}
			obj = obj[part];
	    }
    };
    ----------------------------
    // return last item in path, or a value of zero if any part of the path is missing
	function safe(obj, pathInsideObj) {

		var arr = pathInsideObj.split(/[\.\[\]\"\']/);

		var filtered = [];
		for (var i = 0; i < arr.length; ++i)
		{
			var str = ("" + arr[i]).trim();
			if (str.length > 0)
				filtered.push(str);
		}
		
		for (var i = 0; i < filtered.length; ++i)
		{
			var part = filtered[i];
			if (!obj.hasOwnProperty(part)) 
				return 0. ;

			obj = obj[part];
	    }
		return obj;
	};
*/

	ScriptInvoker getInvoker(boolean minimal) throws Exception
	{
		String sample = getSample(minimal);
		Environment env = new Environment();
		env.setServerVersion("1"); // no freezing

		return VerifyRule.makeInvoker(sample, functions, "{}", false, env);
	}
	ArrayList<JsPath> checkMissing(ScriptInvoker invoker, ArrayList<JsPath> in)
	{
		ArrayList<JsPath> out = new ArrayList<JsPath>();
		for (JsPath item : in)
		{
			if (checkVarExists(invoker, item) == false)
				out.add(item);
		}
		return out;
	}
	boolean checkVarExists(ScriptInvoker invoker, JsPath item)
	{
		try {
			String test = "foundInContext("  + item.jsObject + " , " + JSstring(item.pathInObject) + ")";
			ScriptInvoker.Output output = invoker.evaluate(test);
			return (output.result == ScriptInvoker.Result.TRUE);
		}
		catch (Exception e)
		{
			return false;
		}
	}
	String getServerFunctions() throws Exception
	{
		String url = parms.url + "products/seasons/" + parms.seasonId + "/utilities";
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(url, parms.sessionToken);
		JSONObject utilities =  InsertOrdering.parseError(res.message, "utilities");

		boolean production = parms.stage.equals("PRODUCTION");
		StringBuilder sb = new StringBuilder();

		JSONArray utilsArr = utilities.getJSONArray("utilities");
		for (int i = 0; i < utilsArr.length(); ++i)
		{
			JSONObject utility = utilsArr.getJSONObject(i);

			String stage = utility.getString("stage");
			 // production mode does not look at development functions
			if (production && !stage.equals("PRODUCTION"))
				continue;

			String functions = utility.getString("utility");
			sb.append(functions);
			sb.append("\n");
		}
		return sb.toString();
	}
}
