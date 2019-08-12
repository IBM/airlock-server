package com.ibm.airlock.engine_dev;

import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Util
{
	static void quoteJSON(String str, Writer w) throws IOException
	{
         if (str == null || str.length() == 0)
         {
        	 w.write("\"\"");
             return;
         }

         w.write('"');

         for (int i = 0; i < str.length(); ++i)
         {
             char c = str.charAt(i);
             switch (c)
             {
             case '\\':
             case '"':
             case '/':
                 w.write('\\');
                 w.write(c);
                 break;

             case '\b': w.write("\\b"); break;
             case '\t': w.write("\\t"); break;
             case '\n': w.write("\\n"); break;
             case '\f': w.write("\\f"); break;
             case '\r': w.write("\\r"); break;

             default:
                 if (c < ' ') {
                	 String t = "000" + Integer.toHexString(c);
                     w.write("\\u" + t.substring(t.length() - 4));
                 } else {
                     w.write(c);
                 }
             }
         }
         w.write('"');
     }
	static void writeKey(String prefix, String key, Writer w) throws IOException
	{
		w.write(prefix);
		w.write("\n");
		quoteJSON(key, w);
	}
	static void writePair(String prefix, String key, String value, boolean quote, Writer w) throws IOException
	{
		writeKey(prefix, key, w);
		w.write(": ");
		if (quote)
			quoteJSON(value, w);
		else
			w.write(value);
	}
	static void writeChildren(String prefix, String key, Rule[] children, Writer w) throws IOException
	{
		writeKey(prefix, key, w);
		w.write(": [\n");

		for (int i = 0; i < children.length; ++i)
		{
			if (children[i] == null)
				throw new RuntimeException("Can't serialize rule - missing children");

			if (i > 0)
				w.write(",");

			children[i].toJSON(w);
		}
		w.write("]");
	}

	static Date string2Date(String str) throws ParseException
	{
		SimpleDateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
		return iso8601.parse(str);
	}
	static String date2String(Date date)
	{
		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
		df.setTimeZone(tz);
		return df.format(date);
	}
}
