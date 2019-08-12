package com.ibm.airlock.utilities;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import com.ibm.airlock.admin.Utilities;

public class ConvertTranslationsQuick extends ConvertTranslations
{
	static class Pair {
		String in, out;
		Pair (String i, String o) { in = i; out = o; }
	}

	public static void main(String[] args)
	{
		if (args.length != 1)
		{
			System.out.println("usage: ConvertTranslationsQuick propertiesFile");
			return;
		}

		try {
			new ConvertTranslationsQuick().run(args[0]);
			System.out.println("DONE");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	void run(String properties) throws Exception
	{
		Utilities.Props props = new Utilities.Props();
		props.load(properties);

		String inputFolder = props.get("inputFolder");
		String inputMapping = props.get("inputMapping");
		String outputFolder = props.get("outputFolder");
		boolean smartlingFormat = props.get("smartlingFormat").equals("true");
		boolean toUpper = props.get("toUpper").equals("true");

		String mapIds = props.getOptional("mapIds"); // optional filtering and conversion of translation IDs
		Map<String,String[]> ids = loadIds(mapIds);

		File in = new File(inputFolder);
		File out = new File(outputFolder);
		if (!in.isDirectory())
			throw new Exception("missing input folder " + inputFolder);

		if (!out.isDirectory())
			out.mkdir();

		ArrayList<Pair> mapping = getMapping(inputMapping);
		for (Pair pair : mapping)
		{
			File input = new File(in, pair.in);
			File output = new File(out, pair.out);
			doSubfolder(input, output, ids, smartlingFormat, toUpper);
		}
	}

	ArrayList<Pair> getMapping(String path) throws Exception
	{
		ArrayList<Pair> out = new ArrayList<Pair>();
		String data = Utilities.readString(path);
		String lines[] = data.split("\\s*\n\\s*");
		for (String line : lines)
		{
			String[] items = line.split("\\s*,\\s*");
			if (items.length != 2)
				throw new Exception("invalid mapping " + line);
			out.add(new Pair(items[0], items[1]));
		}
		
		return out;
	}
	void doSubfolder(File xml, File out, Map<String,String[]> ids, boolean smartlingFormat, boolean toUpper) throws Exception
	{
		System.out.println("processing " + xml.getName());
		Map<String,String> map = extractXml(xml, ids);
		printMap(out, map, smartlingFormat, toUpper);
		File outFile = new File(out.getAbsolutePath() + ".stats.txt");
		printStats(outFile, map);
	}
}

