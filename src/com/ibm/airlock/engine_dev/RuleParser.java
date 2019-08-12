package com.ibm.airlock.engine_dev;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

public class RuleParser
{
	static HashMap<String,Token> keywords = new HashMap<String,Token>();

	static {
		// regular tokens
		keywords.put("and", new Token("and", Token.Type.AND));
		keywords.put("or", new Token("or", Token.Type.OR));
		keywords.put("not", new Token("not", Token.Type.NOT));
		keywords.put("date:", new Token("date:", Token.Type.DATE_PREFIX));
		keywords.put("true", new Token("true", Token.Type.BOOL));
		keywords.put("false", new Token("false", Token.Type.BOOL));

		// operation tokens
		keywords.put("=", new Token("=", Rule.Operation.Type.EQ));
		keywords.put("<>", new Token("<>", Rule.Operation.Type.NE));
		keywords.put("<=", new Token("<=", Rule.Operation.Type.LE));
		keywords.put(">=", new Token(">=", Rule.Operation.Type.GE));
		keywords.put(">", new Token(">", Rule.Operation.Type.GT));
		keywords.put("<", new Token("<", Rule.Operation.Type.LT));
		keywords.put("contains", new Token("contains", Rule.Operation.Type.CONTAINS));

		// path tokens
		keywords.put("path_string:", new Token("path_string:", Result.Type.STRING));
		keywords.put("path_date:", new Token("path_date:", Result.Type.DATE));
		keywords.put("path_bool:", new Token("path_bool:", Result.Type.BOOL));
		keywords.put("path_int:", new Token("path_int:", Result.Type.INT));
		keywords.put("path_float:", new Token("path_float:", Result.Type.FLOAT));
		keywords.put("path_count:", new Token("path_count:", Result.Type.COUNT));
	}
	static Token l_parend = new Token("(", Token.Type.L_PAREND);
	static Token r_parend = new Token(")", Token.Type.R_PAREND);

	static class Token
	{
		public enum Type { KEYWORD, STRING, INT, FLOAT, BOOL, DATE, DATE_PREFIX, OPERATION, PATH, L_PAREND, R_PAREND, AND, OR, NOT }
		Type type;
		String string;

		Result.Type resultType;
		Rule.Operation.Type operationType;

		// regular token
		Token(String s, Type t)
			{ string = s; type = t; }
		// operation
		Token(String s, Rule.Operation.Type ot)
			{ string = s; type = Type.OPERATION; operationType = ot; }
		// path with result type
		Token(String s, Result.Type rt )
			{ string = s; type = Type.PATH; resultType = rt; }
	}

	static class Lines
	{
		List<String> lines;
		int index;

		Lines(List<String> lines)
		{
			this.lines = lines;
			index = 0;
		}
		String nextLine()
		{
			if (index >= lines.size())
				return null;

			return lines.get(index++);
		}
	}

	//-----------------------------------------------------------------
	static public Rule parseRule(String rule) throws EngineException
	{
		ArrayList<Token> tokens = tokenize(rule);
		ArrayList<Token> processedTokens = findKeywords(tokens);
		ArrayList<Token> reversedTokens = reversePolishNotation(processedTokens);
		return tokensToRule(reversedTokens);
	}
	static ArrayList<Token> tokenize(String rule) throws EngineException
	{
		ArrayList<Token> out = new ArrayList<Token>();

		for (int i = 0; i < rule.length(); )
		{
			char c = rule.charAt(i);
			switch (c)
			{
			case '(' : out.add(l_parend); ++i; break;
			case ')' : out.add(r_parend); ++i; break;

			case '"' : i = parseJsonString(rule, i, out); break;
			case '$' : i = parseShortPath(rule, i, out); break;

			default:
				if (Character.isWhitespace(c))
					++i;
				else if (c == '-' || Character.isDigit(c))
					i = parseNumber(rule, i, out);
				else if (c == '-' || Character.isDigit(c))
					i = parseNumber(rule, i, out);
				else
					i = parseKeyword(rule, i, out);
			}
		}
		return out;
	}

	static int parseJsonString(String rule, int from, ArrayList<Token>out) throws EngineException
	{
		StringBuilder b = new StringBuilder();

		for (int i = from + 1; i < rule.length(); ++i)
		{
			char c = rule.charAt(i);
			switch (c)
			{
            case '\\':
            {
            	++i;
            	char next = (i == rule.length()) ? 0 : rule.charAt(i);
   
            	switch (next)
            	{
            	case '\\': b.append('\\'); break;
            	case '"': b.append('"'); break;
            	case '/': b.append('/'); break;
            	case 'b': b.append('\b'); break;
            	case 't': b.append('\t'); break;
            	case 'n': b.append('\n'); break;
            	case 'f': b.append('\f'); break;
            	case 'r': b.append('\r'); break;
            	case 'u': b.append(decodeHex(rule, i)); i += 4; break;
            	default:
            		throw new EngineException("unexpected \\ escape in rule " + rule);
            	}
            }
            break;
 
            case '"':
            	out.add(new Token(b.toString(), Token.Type.STRING));
            	return ++i;
 
            default: b.append(c); break;
			}
		}
		throw new EngineException("unbalanced quotes in rule " + rule);
	}
	static char decodeHex(String rule, int i) throws EngineException
	{
		try {
			if (i + 4 >= rule.length())
				throw new Exception("");
			String num = rule.substring(i+1, i+5);
			return (char) Integer.parseInt(num, 16);
		}
		catch (Exception e)
		{
			throw new EngineException("invalid \\u sequence in rule " + rule);
		}
	}

	static int parseKeyword(String rule, int from, ArrayList<Token>out)
	{
		StringBuilder b = new StringBuilder();
		int i = buildString(rule, from, b);
		out.add(new Token(b.toString(), Token.Type.KEYWORD));
		return i;
	}
	static int parseShortPath(String rule, int from, ArrayList<Token>out)
	{
		StringBuilder b = new StringBuilder();
		int i = buildString(rule, from+1, b);
		out.add(new Token(b.toString(), Result.Type.UNKNOWN));
		return i;
	}
	static int buildString(String rule, int from, StringBuilder b)
	{
		int i;
		for (i = from; i < rule.length(); ++i)
		{
			char c = rule.charAt(i);
			if (c == '(' || c == ')' || c == '"' || Character.isWhitespace(c))
				break;

			b.append(c);
		}
		return i;
	}

	static int parseNumber(String rule, int from, ArrayList<Token>out) throws EngineException
	{
		StringBuilder b = new StringBuilder();
		boolean negative = false;
		boolean floating = false;
		boolean digits = false;

		int i;
		for (i = from; i < rule.length(); ++i)
		{
			char c = rule.charAt(i);
			if (c == '-' || c == '.' || Character.isDigit(c))
			{
				b.append(c);
				switch (c)
				{
				case '-':
					if (negative || floating || digits)
						throw new EngineException("invalid number " + b.toString());
					negative = true;
					break;
				case '.':
					if (floating || !digits) // allow 0.2 but not .2
						throw new EngineException("invalid number " + b.toString());
					floating = true;
					break;
				default:
					digits = true;
				}
			}
			else
				break;
		}
		if (!digits)
			throw new EngineException("invalid number " + b.toString());

		out.add(new Token(b.toString(), floating ? Token.Type.FLOAT: Token.Type.INT));
		return i;
	}

	static ArrayList<Token> findKeywords(ArrayList<Token> tokens) throws EngineException
	{
		ArrayList<Token> out = new ArrayList<Token>();

		for (int i = 0; i < tokens.size(); ++i)
		{
			Token t = tokens.get(i);
			if (t.type != Token.Type.KEYWORD)
				out.add(t);
			else
			{
				Token special = keywords.get(t.string);
				if (special == null)
					throw new EngineException("unknown token: " + t.string);
				
				if (special.type == Token.Type.DATE_PREFIX || special.type == Token.Type.PATH)
				{
					++i;
					Token next = (i == tokens.size()) ? null : tokens.get(i);

					if (next == null || next.type != Token.Type.STRING)
						throw new EngineException("out-of-context token found: " + t.string);
	
					if (special.type == Token.Type.DATE_PREFIX)
					{
						// consolidate 2 tokens from date:"..." to one date token
						out.add(new Token(next.string, Token.Type.DATE));
					}
					else
					{
						// consolidate 2 tokens from path_<type>:"..." to one path token
						out.add(new Token(next.string, special.resultType));
					}
				}
				else
				{
					out.add(special);
				}
			}
		}
		return out;
	}

	static ArrayList<Token> reversePolishNotation(ArrayList<Token> tokens) throws EngineException
	{
		if (tokens.size() == 0)
			throw new EngineException("empty rule");

		ArrayList<Token> out = new ArrayList<Token>();
		Stack<Token> stack = new Stack<Token>();

		for (int i = 0; i < tokens.size(); ++i)
		{
			Token t = tokens.get(i);
	
			if (t.type == Token.Type.R_PAREND) // clear stack up to last L_PAREND and remove the ()
			{
				while (true)
				{
					if (stack.isEmpty())
						throw new EngineException("unbalanced paerentheses");

					Token inStack = stack.pop();
					if (inStack.type == Token.Type.L_PAREND)
						break;

					out.add(inStack);
				}
				continue;
			}

			if (t.type == Token.Type.L_PAREND || stack.isEmpty())
			{
				stack.push(t);
				continue;
			}

			int currTokPriority = tokenPriority(t);
			int stackPriority = stack.isEmpty() ? Integer.MAX_VALUE : tokenPriority(stack.peek());

			// push low priority items out of the stack
			while (currTokPriority > stackPriority)
			{
				Token inStack = stack.pop();
				out.add(inStack);
				stackPriority = stack.isEmpty() ? Integer.MAX_VALUE : tokenPriority(stack.peek());
				
			}

			stack.push(t);
		}

		// finish up
		while (!stack.isEmpty())
		{
			Token inStack = stack.pop();
			if (inStack.type == Token.Type.L_PAREND)
				throw new EngineException("unbalanced paerentheses");
			out.add(inStack);
		}
		return out;
	}
	static int tokenPriority(Token t)
	{
		switch (t.type)
		{
		case L_PAREND:
		case R_PAREND:
			return 4;
		case AND:
		case OR:
		case NOT:
			return 3;
		case OPERATION:
			return 2;
		default:
			return 1; // priority of leaf tokens
		}
	}

	static Rule tokensToRule(ArrayList<Token> tokens) throws EngineException
	{
		if (tokens.size() == 0)
			throw new EngineException("empty rule");

		int last = tokens.size() - 1;
		Token root = tokens.get(last);

		switch (root.type)
		{
		case AND:
		case OR:
		case NOT:
		case OPERATION:
		case BOOL:
			break;

		case PATH:
			if (root.resultType == Result.Type.BOOL)
				break; // otherwise fall through and throw exception

		default:
			throw new EngineException("rule does not return true or false");
		}

		int[] index = new int[1];
		index[0] = last;

		Rule rule = makeNode(tokens, index);
		if (index[0] != -1)
			throw new EngineException("rule syntax error: too many operands; insufficient operations");

		return rule;
	}

	// scan polish notation list backwards and generate a node tree
	static Rule makeNode(ArrayList<Token> tokens, int[] index) throws EngineException
	{
		if (index[0] < 0)
			throw new EngineException("rule syntax error: too many operations; insufficient operands");

		Token t = tokens.get(index[0]);
		--index[0];

		Rule son1, son2;
		switch(t.type)
		{
		case NOT:
			son1 = makeNode(tokens, index);
			return new Rule.Not(son1); // checks that son is boolean

		case AND:
			son2 = makeNode(tokens, index); // backward scan, children are in reverse order
			son1 = makeNode(tokens, index);
			return new Rule.And(son1, son2); // checks that sons are boolean

		case OR:
			son2 = makeNode(tokens, index); // children are in reverse order
			son1 = makeNode(tokens, index);
			return new Rule.Or(son1, son2); // checks that sons are boolean

		case OPERATION:
			son2 = makeNode(tokens, index); // children are in reverse order
			son1 = makeNode(tokens, index);
			return new Rule.Operation(son1, t.operationType, son2);  // checks that sons are compatible

		case PATH:
			switch (t.resultType)
			{
			case UNKNOWN:	return new Rule.ShortField(t.string);
			case COUNT:	return new Rule.FieldCounter(t.string);
			default:		return new Rule.Field(t.string, t.resultType);
			}

		case STRING:
			return new Rule.Str(t.string);

		case INT:
			return new Rule.Int(Integer.parseInt(t.string));

		case FLOAT:
			return new Rule.Flot(Float.parseFloat(t.string));

		case BOOL:
			return new Rule.Bool(Boolean.parseBoolean(t.string));

		case DATE:
			try {
				return new Rule.Dater(Util.string2Date(t.string));
			} catch (ParseException e) {
				throw new EngineException("Invalid date string: " + t.string);
			}

		default:
			throw new EngineException("unexpected token found: " + t.string);
		}
	}

	//---------------------------------------------------------------

}
