package com.ibm.airlock.engine_dev;

import java.util.Date;

public class Result
{
	public enum Type { ERROR, STRING, BOOL, INT, FLOAT, DATE, UNKNOWN, COUNT }
	Type type;

	public Result(Type t) { type = t; }
	public Type getType() { return type; }
	public void setType(Type t) { type = t; }

	// not declared abstract - each derived class replaces just one of these
	public String getError() { return null; }
	public String getString() { return null; }
	public boolean getBool() { return false; }
	public int getInt() { return 0; }
	public float getFloat() { return 0; }
	public Date getDate() { return null; }

	static public String printType(Type t)
	{
		return t.toString().toLowerCase();
	}
	//------------------------------------------------------------
	public static class Error extends Result
	{
		String str;
		public Error(String s)
		{
			super(Type.ERROR);
			str = s;
		}
		public String getError() { return str; }
	}
	public static class Str extends Result
	{
		String str;
		public Str(String s)
		{
			super(Type.STRING);
			str = s;
		}
		public String getString() { return str; }
	}
	public static class Bool extends Result
	{
		boolean bool;
		public Bool(boolean b)
		{
			super(Type.BOOL);
			bool = b;
		}
		public boolean getBool() { return bool; }
	}
	public static class Int extends Result
	{
		int integer;
		public Int(int i)
		{
			super(Type.INT);
			integer = i;
		}
		public int getInt() { return integer; }
	}
	public static class Flot extends Result
	{
		float flot;
		public Flot(float f)
		{
			super(Type.FLOAT);
			flot = f;
		}
		public float getFloat() { return flot; }
	}
	public static class Dater extends Result
	{
		Date date;
		public Dater(Date d)
		{
			super(Type.DATE);
			date = d;
		}
		public Date getDate() { return date; }
	}
}
