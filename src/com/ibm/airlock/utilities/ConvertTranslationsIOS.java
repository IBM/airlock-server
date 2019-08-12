package com.ibm.airlock.utilities;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.airlock.admin.Utilities;

public class ConvertTranslationsIOS extends ConvertTranslations
{
	static final String SUFFIX = ".lproj";
	static final String STRING_FILE = "Localizable.strings";

	public static void main(String[] args)
	{
		if (args.length != 1)
		{
			System.out.println("usage: ConvertTranslationsIOS propertiesFile");
			return;
		}

		try {
			new ConvertTranslationsIOS().run(args[0]);
			System.out.println("DONE");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	void doSubfolder(File subFile, File out, Map<String,String[]> ids, boolean smartlingFormat, boolean toUpper) throws Exception
	{
		File file = null;
		String folder = subFile.getName();

		if (subFile.isDirectory() && folder.endsWith(SUFFIX))
			file = findFile(subFile);

		if (file == null)
			System.out.println("skipping folder " + folder);
		else
		{
			String localeName = folder.substring(0, folder.length() - SUFFIX.length());
			System.out.println("processing " + localeName);
			Map<String,String> map = extractFile(file, ids);
			printMap(out, map, localeName, smartlingFormat, toUpper);
			printStats(out, map, localeName);
		}
	}
	File findFile(File subFile)
	{
		File[] sons = subFile.listFiles();
		if (sons == null)
			return null;

		for (int i = 0; i < sons.length; ++i)
		{
			if (sons[i].getName().equals(STRING_FILE) && sons[i].isFile())
				return sons[i];
		}
		return null;
	}
	Map<String,String> extractFile(File file,  Map<String,String[]> ids) throws Exception
	{
		Map<String,String> out = new LinkedHashMap<String,String>(); // to preserve order
		
		String str = Utilities.readString(file.getPath());
		str = Utilities.removeComments(str, true);

		String[] keyVal = new String[2];
		String lines[] = str.split("\\s*\n\\s*");

		for (int i = 0; i < lines.length; ++i)
		{
			String line = lines[i].trim();
			if (line.isEmpty())
				continue;

			if (line.charAt(line.length()-1) == ';') 
				line = line.substring(0, line.length()-1);

			if (parseLine(line, keyVal) == false)
				System.out.println("skipping bad line: " + line);
			else
			{
				String[] translateId = getAlternativeIds(keyVal[0], ids);
				if (translateId == null)
					continue;

				String val = fixPlaceholders(keyVal[1]);
				val = removeBackslash(val); // TODO TEMPORARY!!

				 for (int j = 0; j < translateId.length; ++j)
					 out.put(translateId[j], val);
			}
		}
		return out;
	}
	boolean parseLine(String line, String[] keyVal)
	{
		int loc = line.indexOf("=");
		if (loc < 3)
			return false;

		keyVal[0] = unquote(line.substring(0, loc).trim());
		keyVal[1] = unquote(line.substring(loc+1).trim());
		return keyVal[0] != null && keyVal[1] != null;
	}
	String unquote(String in)
	{
		if (in.length() < 2)
			return null;

		int last = in.length() - 1;
		if (in.charAt(0) != '"' || in.charAt(last) != '"')
			return null;

		StringBuilder b = new StringBuilder();
		for (int i = 1; i < last; ++i)
		{
			char c = in.charAt(i);
			if (c == '\\')
				c = in.charAt(++i);
			b.append(c);
		}
		return b.toString();
	}
	String fixPlaceholders(String in)
	{
		String out = in;
		String pattern = "\\%(\\d+)\\$\\@";
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(in);

		while (m.find())
		{
			String old = m.group(0);
			String number = m.group(1);
			String newOne = L_BRACKET + number + R_BRACKET;
			out = out.replace(old, newOne);
		}
		return out;
	}
}

