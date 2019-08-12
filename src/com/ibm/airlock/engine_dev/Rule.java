package com.ibm.airlock.engine_dev;

import java.io.IOException;
import java.io.Writer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

//import com.mongodb.BasicDBObject;

public abstract class Rule
{
	enum NodeType { AND, OR, NOT, OPERATION, STRING, BOOL, INT, FLOAT, DATE, FIELD, SHORT_FIELD, COUNT, JAVASCRIPT}
	NodeType nodeType;
	Result.Type resultType;

	final static String NODE_TYPE = "nodeType";
	final static String RESULT_TYPE = "resultType";
	final static String OPERATION = "operation";
	final static String CHILDREN = "children";
	final static String VALUE = "value";

	public Rule(NodeType n, Result.Type r) { nodeType = n; resultType = r; }

	public NodeType getNodeType() { return nodeType; }
	public Result.Type getResultType() { return resultType; }


	public abstract Result evaluate(RuleEngine.UserProfile profile);
	public abstract void toText(Writer w) throws IOException;

	public void toJSON(Writer w) throws IOException
	{
		Util.writePair("{", NODE_TYPE, nodeType.toString(), true, w);
		Util.writePair(",", RESULT_TYPE, resultType.toString(), true, w);
	}

	void verifyBoolean(Rule[] children) throws EngineException
	{
		for (Rule rule : children)
		{
			if (rule.getResultType() != Result.Type.BOOL)
				throw new EngineException("Child node must be boolean");
		}
	}
	//-----------------------------------------------

	static public String printNodeType(NodeType t)
	{
		return t.toString().toLowerCase();
	}

	static Rule jsonLoader(JSONObject obj) throws JSONException, EngineException
	{
		String s1 = obj.getString(NODE_TYPE);
		String s2 = obj.getString(RESULT_TYPE);
		Result.Type resultType = Result.Type.valueOf(s2);

		NodeType nodeType = NodeType.valueOf(s1);

		switch (nodeType)
		{
		case AND:		return new Rule.And(obj);
		case OR:		return new Rule.Or(obj);
		case NOT:		return new Rule.Not(obj);
		case OPERATION:	return new Rule.Operation(obj);
		case STRING:	return new Rule.Str(obj);
		case BOOL:		return new Rule.Bool(obj);
		case INT:		return new Rule.Int(obj);
		case FLOAT:		return new Rule.Flot(obj);
		case DATE:		return new Rule.Dater(obj);
		case FIELD:		return new Rule.Field(obj, resultType);
		case COUNT:		return new Rule.FieldCounter(obj, resultType);
		case SHORT_FIELD: return new Rule.ShortField(obj, resultType);
		case JAVASCRIPT:  return new Rule.JavaScript(obj);
		//case UNPARSED:   return Rule.Unparsed.runtimeParse(obj);

		default:		throw new JSONException("unknown node type");
		}
	}
	static Rule[] jsonArrayLoader(JSONArray arr) throws JSONException, EngineException
	{
		Rule[] out = new Rule[ arr.length() ];
		for (int i = 0; i < arr.length(); ++i)
		{
			JSONObject obj = arr.getJSONObject(i);
			out[i] = jsonLoader(obj);
		}
		return out;
	}

	//-----------------------------------------------
	public static class Not extends Rule
	{
		Rule[] children;

		public Not(Rule r) throws EngineException
		{
			super(NodeType.NOT, Result.Type.BOOL);

			children = new Rule[1];
			children[0] = r;
			verifyBoolean(children);
		}
		Not(JSONObject obj) throws JSONException, EngineException
		{
			super(NodeType.NOT, Result.Type.BOOL);
			JSONArray arr = obj.getJSONArray(CHILDREN);
			children = jsonArrayLoader(arr);
			verifyBoolean(children);
		}

		public Result evaluate(RuleEngine.UserProfile profile)
		{
			Result r = children[0].evaluate(profile);
			if (r.getType() == Result.Type.ERROR)
				return r;

			return new Result.Bool(! r.getBool());
		}

		public void toJSON(Writer w) throws IOException
		{
			super.toJSON(w);
			Util.writeChildren(",", CHILDREN, children, w);
			w.write("}\n");
		}

		public void toText(Writer w) throws IOException
		{
			w.write("not (");
			children[0].toText(w);
			w.write(")");
		}
	}
	//-----------------------------------------------
	public static class And extends Rule
	{
		Rule[] children;

		And(Rule[] sons) throws EngineException
		{
			super(NodeType.AND, Result.Type.BOOL);
			children = sons;
			if (children.length < 2)
				throw new EngineException("child nodes missing in AND");
			verifyBoolean(children);
		}
		And(Rule son1, Rule son2) throws EngineException
		{
			super(NodeType.AND, Result.Type.BOOL);
			children = new Rule[2];
			children[0] = son1;
			children[1] = son2;
			verifyBoolean(children);
		}
		And(JSONObject obj) throws JSONException, EngineException
		{
			super(NodeType.AND, Result.Type.BOOL);
			JSONArray arr = obj.getJSONArray(CHILDREN);
			children = jsonArrayLoader(arr);
			verifyBoolean(children);
		}

		public Result evaluate(RuleEngine.UserProfile profile)
		{
			for (Rule child : children)
			{
				Result r = child.evaluate(profile);
				if (r.getType() == Result.Type.ERROR || r.getBool() == false)
					return r;
			}
			return new Result.Bool(true);
		}

		public void toJSON(Writer w) throws IOException
		{
			super.toJSON(w);
			Util.writeChildren(",", CHILDREN, children, w);
			w.write("}\n");
		}

		public void toText(Writer w) throws IOException
		{
			for (int i = 0; i < children.length; ++i)
			{
				if (i > 0)
					w.write(" and ");
				w.write("(");
				children[i].toText(w);
				w.write(")");
			}
		}
	}
	//-----------------------------------------------
	public static class Or extends Rule
	{
		Rule[] children;

		public Or(Rule[] sons) throws EngineException
		{
			super(NodeType.OR, Result.Type.BOOL);
			children = sons;
			if (children.length < 2)
				throw new EngineException("child nodes missing in AND");
			verifyBoolean(children);
		}
		public Or(Rule son1, Rule son2) throws EngineException
		{
			super(NodeType.OR, Result.Type.BOOL);
			children = new Rule[2];
			children[0] = son1;
			children[1] = son2;
			verifyBoolean(children);
		}
		Or(JSONObject obj) throws JSONException, EngineException
		{
			super(NodeType.OR, Result.Type.BOOL);
			JSONArray arr = obj.getJSONArray(CHILDREN);
			children = jsonArrayLoader(arr);
			verifyBoolean(children);
		}

		public Result evaluate(RuleEngine.UserProfile profile)
		{
			for (Rule child : children)
			{
				Result r = child.evaluate(profile);
				if (r.getBool() == true) // if Result.Error was returned, try other children
					return r;
			}

			return new Result.Bool(false);
		}

		public void toJSON(Writer w) throws IOException
		{
			super.toJSON(w);
			Util.writeChildren(",", CHILDREN, children, w);
			w.write("}\n");
		}

		public void toText(Writer w) throws IOException
		{
			for (int i = 0; i < children.length; ++i)
			{
				if (i > 0)
					w.write(" or ");
				w.write("(");
				children[i].toText(w);
				w.write(")");
			}
		}
	}
	//-----------------------------------------------
	public static class Operation extends Rule
	{
		enum Type { EQ, NE, LT, GT, GE, LE, CONTAINS }
		static String[] printed = { "=", "<>", "<", ">", ">=", "<=", "contains" };

		Type type;
		Rule[] children;

		public Operation(Rule operand1, Type operation, Rule operand2) throws EngineException
		{
			super(NodeType.OPERATION, Result.Type.BOOL);
			type = operation;
			children = new Rule[2];
			children[0] = operand1;
			children[1] = operand2;
			compareChildTypes();
		}
		Operation(JSONObject obj) throws JSONException, EngineException
		{
			super(NodeType.OPERATION, Result.Type.BOOL);

			String str = obj.getString(OPERATION);
			type = Type.valueOf(str);

			JSONArray arr = obj.getJSONArray(CHILDREN);
			children = jsonArrayLoader(arr);
		}

		void compareChildTypes() throws EngineException
		{
			Result.Type r0 = children[0].getResultType();
			Result.Type r1 = children[1].getResultType();

			// promote integer to float if necessary
			if (r0 == Result.Type.INT && r1 == Result.Type.FLOAT)
				children[0] = intToFloat(children[0]);
			else if (r1 == Result.Type.INT && r0 == Result.Type.FLOAT)
				children[1] = intToFloat(children[1]);

			// infer the type of an unknown field from its sibling
			else if (r0 == Result.Type.UNKNOWN || r1 == Result.Type.UNKNOWN)
			{
				if (r0 == r1)
					throw new EngineException("unqualified $path can't be used on both sides of an operation (use explicit path type, e.g. path_int or path_string)");

				if (r0 == Result.Type.UNKNOWN)
					children[0].resultType = r1;
				else
					children[1].resultType = r0;
			}

			if (children[0].getResultType() != children[1].getResultType())
				throw new EngineException("Children of OPERATION node have mismatched types");
		}
		Rule intToFloat(Rule rule) throws EngineException // promote integer to float
		{
			if (rule instanceof Rule.Int) // integer constant
				return new Rule.Flot(((Rule.Int)rule).integer);
			else if (rule instanceof Rule.Field) // both FieldCounter and Field
			{
				rule.resultType = Result.Type.FLOAT;
				return rule;
			}
			else
				throw new EngineException("Unexpected Rule type");
		}

		public Result evaluate(RuleEngine.UserProfile profile)
		{
			Result r0 = children[0].evaluate(profile);
			if (r0.getType() == Result.Type.ERROR)
				return new Result.Bool(false); // TODO we assume the error is due to a missing field, but returning false is not always appropriate
				//return r0;

			Result r1 = children[1].evaluate(profile);
			if (r1.getType() == Result.Type.ERROR)
				return new Result.Bool(false);
				//return r1;

			boolean b;
			switch (children[0].getResultType())
			{
			case STRING: b = compare(r0.getString(), r1.getString()); break;
			case BOOL:   b = compare(r0.getBool(), r1.getBool()); break;
			case INT:    b = compare(r0.getInt(), r1.getInt()); break;
			case FLOAT:  b = compare(r0.getFloat(), r1.getFloat()); break;
			case DATE:   b = compare(r0.getDate(), r1.getDate()); break;
			default:     b = false; // not possible  
			}
			return new Result.Bool(b);
		}
		boolean compare(String s1, String s2)
		{
			switch (type)
			{
			case EQ: return s1.equals(s2);
			case NE: return ! s1.equals(s2);
			case LT: return s1.compareTo(s2) < 0;
			case GT: return s1.compareTo(s2) > 0;
			case GE: return s1.compareTo(s2) >= 0;
			case LE: return s1.compareTo(s2) <= 0;
			case CONTAINS: return s1.contains(s2);
			default: return false;
			}
		}
		boolean compare(boolean s1, boolean s2)
		{
			return compare(s1 ? 1 : 0, s2 ? 1 : 0);
		}
		boolean compare(int s1, int s2)
		{
			switch (type)
			{
			case EQ: return s1 == s2;
			case NE: return s1 != s2;
			case LT: return s1 < s2;
			case GT: return s1 > s2;
			case GE: return s1 >= s2;
			case LE: return s1 <= s2;
			default: return false;
			}
		}
		boolean compare(float s1, float s2)
		{
			final double DELTA = 0.000000001;
			switch (type)
			{
			case EQ:
			{
				double diff = (s1 > s2) ? s1 - s2 : s2 - s1;
				return (diff < DELTA);
			}
			case NE:
			{
				double diff = (s1 > s2) ? s1 - s2 : s2 - s1;
				return (diff >= DELTA);
			}

			case LT: return s1 < s2;
			case GT: return s1 > s2;
			case GE: return s1 >= s2;
			case LE: return s1 <= s2;
			default: return false;
			}
		}
		boolean compare(Date s1, Date s2)
		{
			int diff = s1.compareTo(s2);
			switch (type)
			{
			case EQ: return diff == 0;
			case NE: return diff != 0;
			case LT: return diff < 0;
			case GT: return diff > 0;
			case GE: return diff >= 0;
			case LE: return diff <= 0;
			default: return false;
			}
		}

		public void toJSON(Writer w) throws IOException
		{
			super.toJSON(w);
			Util.writePair(",", OPERATION, type.toString(), true, w);
			Util.writeChildren(",", CHILDREN, children, w);
			w.write("}\n");
		}

		public void toText(Writer w) throws IOException
		{
			children[0].toText(w);
			w.write(" ");
			w.write(printType(type));
			w.write(" ");
			children[1].toText(w);
		}
		//----------------------------------------------
		public static String printType(Type t)
		{
			return printed[ t.ordinal() ];
		}
		public Type parseType(String str) throws EngineException
		{
			for (int i = 0; i < printed.length; ++i)
			{
				if (printed[i].equals(str))
					return Type.values()[i];
			}
			throw new EngineException("Invalid operation");
		}
	}

	//-----------------------------------------------
	public static class Int extends Rule
	{
		int integer;
		public Int(int i)
		{
			super(NodeType.INT, Result.Type.INT);
			integer = i;
		}

		Int(JSONObject obj) throws JSONException
		{
			super(NodeType.INT, Result.Type.INT);
			integer = obj.getInt(VALUE);
		}

		public Result evaluate(RuleEngine.UserProfile profile)
		{
			return new Result.Int(integer);
		}

		public void toJSON(Writer w) throws IOException
		{
			super.toJSON(w);
			Util.writePair(",", VALUE, Integer.toString(integer), false, w);
			w.write("\n}\n");
		}

		public void toText(Writer w) throws IOException
		{
			w.write(Integer.toString(integer));
		}
	}
	//-----------------------------------------------
	public static class Flot extends Rule
	{
		float flot;
		public Flot(float f)
		{
			super(NodeType.FLOAT, Result.Type.FLOAT);
			flot = f;
		}

		Flot(JSONObject obj) throws JSONException
		{
			super(NodeType.FLOAT, Result.Type.FLOAT);
			String str = obj.getString(VALUE);
			flot = Float.parseFloat(str);
		}

		public Result evaluate(RuleEngine.UserProfile profile)
		{
			return new Result.Flot(flot);
		}

		public void toJSON(Writer w) throws IOException
		{
			super.toJSON(w);
			Util.writePair(",", VALUE, Float.toString(flot), false, w);
			w.write("\n}\n");
		}

		public void toText(Writer w) throws IOException
		{
			w.write(Float.toString(flot));
		}
	}
	//-----------------------------------------------
	public static class Str extends Rule
	{
		String str;
		public Str(String s)
		{
			super(NodeType.STRING, Result.Type.STRING);
			str = s;
		}
		Str(JSONObject obj) throws JSONException
		{
			super(NodeType.STRING, Result.Type.STRING);
			str = obj.getString(VALUE);
		}

		public Result evaluate(RuleEngine.UserProfile profile)
		{
			return new Result.Str(str);
		}

		public void toJSON(Writer w) throws IOException
		{
			super.toJSON(w);
			Util.writePair(",", VALUE, str, true, w);
			w.write("\n}\n");
		}

		public void toText(Writer w) throws IOException
		{
			Util.quoteJSON(str, w);
		}

	}
	//-----------------------------------------------
	public static class Bool extends Rule
	{
		boolean bool;
		public Bool(boolean b)
		{
			super(NodeType.BOOL, Result.Type.BOOL);
			bool = b;
		}
		Bool(JSONObject obj) throws JSONException
		{
			super(NodeType.BOOL, Result.Type.BOOL);
			bool = obj.getBoolean(VALUE);
		}

		public Result evaluate(RuleEngine.UserProfile profile)
		{
			return new Result.Bool(bool);
		}

		public void toJSON(Writer w) throws IOException
		{
			super.toJSON(w);
			Util.writePair(",", VALUE, Boolean.toString(bool), false, w);
			w.write("\n}\n");
		}

		public void toText(Writer w) throws IOException
		{
			w.write(Boolean.toString(bool));
		}
	}
	//-----------------------------------------------
	public static class Dater extends Rule
	{
		final static String DATE_PREFIX = "date";
		Date date;
		public Dater(Date d)
		{
			super(NodeType.DATE, Result.Type.DATE);
			date = d;
		}

		Dater(JSONObject obj) throws JSONException
		{
			super(NodeType.DATE, Result.Type.DATE);
			String str = obj.getString(VALUE);
			try {
				date = Util.string2Date(str);
			}
			catch (ParseException e) {
				throw new JSONException(e.toString());
			}
		}

		public Result evaluate(RuleEngine.UserProfile profile)
		{
			return new Result.Dater(date);
		}

		public void toJSON(Writer w) throws IOException
		{
			super.toJSON(w);
			String iso = Util.date2String(date);
			Util.writePair(",", VALUE, iso, true, w);
			w.write("\n}\n");
		}

		public void toText(Writer w) throws IOException
		{
			w.write(DATE_PREFIX + ":"); // add prefix to designate a date string
			String iso = Util.date2String(date);
			Util.quoteJSON(iso, w);
		}
	}
	//-----------------------------------------------
	public static class Field extends Rule
	{
		final static String PATH_PREFIX = "path_";

		String path;

		static class PathElement {
			String key = null;
			Integer ordinal = null;
		}
		ArrayList<PathElement> elements;

		public Field(String s, Result.Type t) throws EngineException
		{
			super(NodeType.FIELD, t);
			path = s;
			parsePath();
		}

		Field(JSONObject obj, Result.Type t) throws JSONException, EngineException
		{
			super(NodeType.FIELD, t);
			path = obj.getString(VALUE);
			parsePath();
		}

		Object traverse(JSONObject document) throws Exception
		{
			Object current = document;
			for (PathElement el : elements)
			{
				if (el.key != null)
				{
					if (current instanceof JSONObject)
						current = ((JSONObject) current).opt(el.key);
					else
						throw new Exception("");
				}
				else
				{
					if (current instanceof JSONArray)
						current = ((JSONArray) current).get(el.ordinal);
					else
						throw new Exception("");
				}
				
				if (current == null)
					throw new Exception("");
			}
			return current;
		}

		public Result evaluate(RuleEngine.UserProfile profile)
		{
			JSONObject document = profile.json;
			if (document == null)
				return new Result.Error("missing document");

			try {

				Object current = traverse(document);
				String str = current.toString();

				switch (resultType)
				{
				case STRING: return new Result.Str(str);
				case BOOL:   return new Result.Bool(Boolean.parseBoolean(str));
				case INT:    return new Result.Int(Integer.parseInt(str));
				case FLOAT:  return new Result.Flot(Float.parseFloat(str));
				case DATE:   return new Result.Dater(Util.string2Date(str));
				default:     throw new Exception("unknown result type");
				}
			}
			catch (Exception e)
			{
				return new Result.Error("path not extracted in document:" + path);
			}
		}

		// path follows MongoDb syntax a.b.c where each element is either a key in a map or an index in an array
		void parsePath() throws EngineException
		{
			elements = new ArrayList<PathElement>();
			String[] items = path.split("\\.");
			for (String item : items)
			{
				item = item.trim();
				if (item.isEmpty())
					continue;
				
				if (Character.isDigit(item.charAt(0)))
				{
					try {
						PathElement e = new PathElement();
						e.ordinal = Integer.parseInt(item);
						elements.add(e);
					}
					catch (Exception e)
					{
						throw new EngineException("Invalid field item number " + path);
					}
				}
				else
				{
					PathElement e = new PathElement();
					e.key = item;
					elements.add(e);
				}
			}
			if (elements.size() == 0)
				throw new EngineException("Invalid field path " + path);
		}

		public void toJSON(Writer w) throws IOException
		{
			super.toJSON(w);
			Util.writePair(",", VALUE, path, true, w);
			w.write("\n}\n");
		}

		public void toText(Writer w) throws IOException
		{
			// add prefix to designate field path and field type
			w.write(PATH_PREFIX);
			w.write(Result.printType(resultType));
			w.write(":");
			Util.quoteJSON(path, w);
		}
	}
	//-----------------------------------------------
	public static class FieldCounter extends Field
	{
		public FieldCounter(String s) throws EngineException {
			super(s, Result.Type.INT);
			nodeType = NodeType.COUNT;
		}
		FieldCounter(JSONObject obj, Result.Type t) throws JSONException, EngineException
		{
			super(obj, t);
			nodeType = NodeType.COUNT;
		}
		@Override
		public Result evaluate(RuleEngine.UserProfile profile)
		{
			JSONObject document = profile.json;
			if (document == null)
				return new Result.Error("missing document");
			
			try {

				Object current = traverse(document);

				int count;
				if (current instanceof JSONObject)
				{
					count = ((JSONObject) current).length();
				}
				else if (current instanceof JSONArray)
				{
					count = ((JSONArray) current).length();
				}
				else count = 1;

				if (resultType == Result.Type.FLOAT)
					return new Result.Flot(count);
				else
					return new Result.Int(count);
			}
			catch (Exception e)
			{
				return new Result.Error("path not extracted in document:" + path);
			}
		}

		public void toText(Writer w) throws IOException
		{
			w.write(PATH_PREFIX);
			w.write(nodeType.toString().toLowerCase());
			w.write(":");
			Util.quoteJSON(path, w);
		}
	}
	//-----------------------------------------------
	public static class ShortField extends Field
	{
		static final String TEXTUAL_NAME = "$";

		public ShortField(String s) throws EngineException {
			super(s, Result.Type.UNKNOWN); // actual result type will be inferred from sibling
			nodeType = NodeType.SHORT_FIELD;
		}
		ShortField(JSONObject obj, Result.Type t) throws JSONException, EngineException
		{
			super(obj, t);
			nodeType = NodeType.SHORT_FIELD;
		}

		public void toText(Writer w) throws IOException
		{
			w.write(TEXTUAL_NAME);
			w.write(path); // short fields do not need quoting
		}
	}

/*
	//-------------------------------------------------------------------------------
	// a convenience class for saving a native rule to JSON in unparsed form.
	// it only exists while writing to JSON.
	//-------------------------------------------------------------------------------
	public static class Unparsed extends Rule
	{
		String unparsed;

		Unparsed(String trigger) {
			super(NodeType.UNPARSED, Result.Type.BOOL);
			unparsed = trigger;
		}
		static Rule runtimeParse(JSONObject obj) throws JSONException, EngineException
		{
			String trigger = obj.getString(VALUE);
			return RuleParser.parseRule(trigger);
		}
		public void toJSON(Writer w) throws IOException
		{
			super.toJSON(w);
			Util.writePair(",", VALUE, unparsed, true, w);
			w.write("\n}\n");
		}
		public void toText(Writer w) throws IOException	{
			throw new RuntimeException("Unparsed rule can't be saved");
		}
		public Result evaluate(UserProfile profile) {
			throw new RuntimeException("Unparsed rule can't be evaluated");
		}
	}
*/

	//-----------------------------------------------------------------------------------
	// this class evaluates a JavaScript rule that inspects a bound object called $
	//-----------------------------------------------------------------------------------
	public static class JavaScript extends Rule
	{
		// JavaScript engine is thread-safe
		static ScriptEngineManager manager = new ScriptEngineManager();
		static ScriptEngine engine = manager.getEngineByName("JavaScript");

		String rule;

		public JavaScript(String rule) throws EngineException
		{
			super(NodeType.JAVASCRIPT, Result.Type.BOOL);
			this.rule = rule;
			validate(rule);
		}

		JavaScript(JSONObject obj) throws JSONException, EngineException
		{
			super(NodeType.JAVASCRIPT, Result.Type.BOOL);
			rule = obj.getString(VALUE);
			validate(rule);
		}
		public void toJSON(Writer w) throws IOException
		{
			super.toJSON(w);
			Util.writePair(",", VALUE, rule, true, w);
			w.write("\n}\n");
		}

		public void toText(Writer w) throws IOException {
			w.write(rule);
		}

		public Result evaluate(RuleEngine.UserProfile profile)
		{
			if (profile.binding == null)
				return new Result.Error("missing document");

			try {
				 Object obj = runScript(rule, profile.binding);
				if (obj instanceof Boolean)
					return new Result.Bool((Boolean)obj);
				else
					return new Result.Error("javascript does not evaluate to true or false: " + rule);
				}
				catch (ScriptException e) {
					return new Result.Error("failed to evaluate javascript: " + rule);
				}
		}

		// convert a JSON profile string to a JavaScript object called '$' inside a binding
		static Bindings createUserObject(String jsonProfile) throws JSONException 
		{
			Bindings binding = new SimpleBindings();
			binding.put("_jsonString", jsonProfile);
			
			try {
				engine.eval("var $ = JSON.parse(_jsonString);", binding);
			}
			catch (ScriptException e) {
				throw new JSONException(e.toString());
			}
			return binding;
		}

		// run a JavaScript rule against the user profile
		static Object runScript(String rule, Bindings binding) throws ScriptException
		{
			// the rule should refer to a profile object called '$' that exists in the binding
			engine.eval("var _result = " + rule + " ;", binding);
			return binding.get("_result");
		}

		static void validate(String rule) throws EngineException
		{
		}
	}

}
