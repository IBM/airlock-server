package com.ibm.airlock.admin.translations;

import com.ibm.airlock.admin.Utilities;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImportExportUtilitiesIOS extends ImportExportUtilities {

    static final String SUFFIX = ".lproj";
    static final String STRING_FILE = "Localizable.strings";

    static Pattern pattern = Pattern.compile("\\%(\\d+)\\$\\@");
    static Pattern pattern2 = Pattern.compile("\\%[sSdfb]");

    static // default mapping
    {
        localeMap.put("no", "nb");
        //localeMap.put("nb", "no");
        localeMap.put("hi-IN", "hi");
        localeMap.put("pt-PT", "pt");
        localeMap.put("pt", "pt-PT");
        localeMap.put("pt-BR", "pt_BR");
        localeMap.put("pt_BR", "pt-BR");
        localeMap.put("zh-Hans", "zh_Hans");
        localeMap.put("zh_Hans", "zh-Hans");
        localeMap.put("zh-Hant", "zh_Hant");
        localeMap.put("zh_Hant", "zh-Hant");
        localeMap.put("en-GB", "en_GB");
        localeMap.put("en_GB", "en-GB");
        localeMap.put("en-AU", "en_AU");
        localeMap.put("en_AU", "en-AU");
        localeMap.put("fr-CA", "fr_CA");
        localeMap.put("fr_CA", "fr-CA");
    }

    public boolean doDefaultSubfolder(File inputFolder, HashMap<String,OriginalString> originalStringMap, boolean keepData, boolean preserveFormat) throws Exception{
        logger.info("processing default" );
        File subFile = null;
        for (File f : inputFolder.listFiles()) {
            if (f.isDirectory() && f.getName().endsWith("Base.lproj")) {
                subFile = f;
                break;
            }
        }
        if(subFile == null){
            return false;
        }
        File file = findFile(subFile);
        if(file == null){
            throw new Exception("missing default values Localizable.strings ");
        }
        Map<String,String> map = extractFile(file, preserveFormat);
        //TODO arrays?
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            OriginalString importedString = new OriginalString(key, value);
            originalStringMap.put(key,importedString);
        }
        return true;
    }
    public void doLocaleSubfolder(File subFile,HashMap<String,OriginalString> originalStringMap, LinkedList<String> supportedLanguages,boolean keepData, boolean preserveFormat) throws Exception {
        File file = null;
        String folder = subFile.getName();

        if (subFile.isDirectory() && folder.endsWith(SUFFIX))
            file = findFile(subFile);

        if (file == null)
            logger.info("skipping folder " + folder);
        else {
            String localeName = folder.substring(0, folder.length() - SUFFIX.length());
            String mappedLocaleName = mapLocale(localeName);
            if(mappedLocaleName == null){
                logger.info("unsuported locale " + localeName);
                return;
            }
            logger.info("processing " + localeName);

            Map<String, String> map = extractFile(file, preserveFormat);
            for (Map.Entry<String, String> entry : map.entrySet()) {
                if(originalStringMap.containsKey(entry.getKey())) {
                    originalStringMap.get(entry.getKey()).addStringTranslationForLocale(mappedLocaleName, entry.getValue(), supportedLanguages);
                    if(mappedLocaleName.equals("nb")){ // special treatment for norvegian
                        originalStringMap.get(entry.getKey()).addStringTranslationForLocale("no", entry.getValue(), supportedLanguages);
                    }
                }
            }
        }
    }

    public File exportToZipFile(String seasonId,LinkedList<OriginalString> originalStrings) throws Exception{
        HashMap<String,FileWriter> localeFileMap = new HashMap<>();
        String separator = "/";
        String fileName ="temp"+separator+"export"+separator+seasonId;
        String zipName = "temp"+separator+"export"+separator+"exportedStrings.zip";
        File newFile = new File(fileName);
        if(!newFile.exists())
            newFile.mkdirs();
        newFile = new File(fileName,"Base.lproj");
        if(!newFile.exists())
            newFile.mkdirs();
        File stringsFile = new File(fileName,"Base.lproj/Localizable.strings");
        FileWriter defaultDoc = new FileWriter(stringsFile);

      for(int i = 0 ; i<originalStrings.size();++i) {
            OriginalString oriString = originalStrings.get(i);
            String value = removePlaceHolders(oriString.getValue());
            defaultDoc.write("\""+oriString.getKey()+"\"=\""+value+"\";\n");
            //translation
            OriginalString.StringTranslations stringTranslations = oriString.getStringTranslations();
            Iterator<Map.Entry<String,OriginalString.StringTranslations.LocaleTranslationData>> iter = stringTranslations.stringTranslationsMap.entrySet().iterator();
            while (iter.hasNext()){
                Map.Entry<String,OriginalString.StringTranslations.LocaleTranslationData> entry = iter.next();
                FileWriter localeDoc = getFileForLocale(mapLocale(entry.getKey()),localeFileMap,fileName);
                String localeValue = removePlaceHolders(entry.getValue().getTranslatedValue());
                localeDoc.write("\""+oriString.getKey()+"\"=\""+localeValue+"\";\n");
            }
        }
        // close the files
       defaultDoc.close();
        //translations
        Iterator<Map.Entry<String,FileWriter>> iter2 = localeFileMap.entrySet().iterator();
        while (iter2.hasNext()){
            Map.Entry<String,FileWriter> entry = iter2.next();
            entry.getValue().close();
        }
        zip(zipName,fileName);
        return new File(zipName);
    }

    public String removePlaceHolders(String in) {
        return in.replace(L_BRACKET,"%").replace(R_BRACKET,"$@");
    }

    public FileWriter getFileForLocale(String locale, HashMap<String,FileWriter> mapLocaleFile,String fileName) throws IOException {
        if(mapLocaleFile.containsKey(locale)){
            return  mapLocaleFile.get(locale);
        }
        else {
            File localeDirectory = new File (fileName,locale+".lproj");
            if(!localeDirectory.exists())
                localeDirectory.mkdirs();
            File localeFile = new File(localeDirectory,"Localizable.strings");
            FileWriter localeWritter = new FileWriter(localeFile);
            mapLocaleFile.put(locale,localeWritter);
            return localeWritter;
        }
    }
    public String mapLocale(String localeName){
        if(localeName.equals("Base")){
            return null;
        }
        if(localeMap.containsKey(localeName)){
            return localeMap.get(localeName);
        }
        return localeName;
    }

    File findFile(File subFile) {
        File[] sons = subFile.listFiles();
        if (sons == null)
            return null;

        for (int i = 0; i < sons.length; ++i) {
            if (sons[i].getName().equals(STRING_FILE) && sons[i].isFile())
                return sons[i];
        }
        return null;
    }
    
    public String getFormattedString(String str, boolean preserveFormat) {
    	String text = str;
    	if (!preserveFormat && str!=null) {
        	text = fixPlaceholders(text);
            text = fixNumberPlacehoder(text);
        }
    	return text;
    }

    Map<String, String> extractFile(File file, boolean preserveFormat) throws Exception {
        Map<String, String> out = new LinkedHashMap<String, String>(); // to preserve order

        String str = Utilities.readString(file.getPath());
        str = Utilities.removeComments(str, true);

        String[] keyVal = new String[2];
        String lines[] = str.split("\\s*\n\\s*");

        for (int i = 0; i < lines.length; ++i) {
            String line = lines[i].trim();
            if (line.isEmpty())
                continue;


            if (line.charAt(line.length() - 1) == ';') // TODO several strings on same line?
                line = line.substring(0, line.length() - 1);

            if (parseLine(line, keyVal) == false)
                logger.info("skipping bad line: " + line);
            else {
            	String val = keyVal[1];
            	if (!preserveFormat) {
            		val = fixPlaceholders(keyVal[1]);
                    val = fixNumberPlacehoder(val);
                   // val = removeBackslash(val);
            	}
                out.put(keyVal[0], val);
            }
        }
        return out;
    }

    boolean parseLine(String line, String[] keyVal) {
        int loc = line.indexOf("=");
        if (loc < 3)
            return false;

        keyVal[0] = unquote(line.substring(0, loc).trim());
        keyVal[1] = unquote(line.substring(loc + 1).trim());
        return keyVal[0] != null && keyVal[1] != null;
    }

    String unquote(String in) {
        if (in.length() < 2)
            return null;

        int last = in.length() - 1;
        if (in.charAt(0) != '"' || in.charAt(last) != '"')
            return null;

        StringBuilder b = new StringBuilder();
        for (int i = 1; i < last; ++i) {
            char c = in.charAt(i);
            if (c == '\\')
                c = in.charAt(++i);
            b.append(c);
        }
        return b.toString();
    }

    String fixPlaceholders(String in) {
        String out = in;
        Matcher m = pattern.matcher(in);

        while (m.find()) {
            String old = m.group(0);
            String number = m.group(1);
            String newOne = L_BRACKET + number + R_BRACKET;
            out = out.replace(old, newOne);
        }
        return out;
    }

    public String fixNumberPlacehoder(String text) {
        String[] splitText = text.split(pattern2.pattern());
        if (splitText.length == 1)
            return text;
        StringBuilder sb = new StringBuilder();
        int count = 1;
        for (int i = 0; i < splitText.length - 1; ++i) {
            sb.append(splitText[i] + L_BRACKET + count + R_BRACKET);
            count++;
        }
        sb.append(splitText[splitText.length - 1]);
        return sb.toString();
    }
}
