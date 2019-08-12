package com.ibm.airlock.admin.translations;

import com.ibm.airlock.admin.Season;
import com.ibm.airlock.admin.serialize.AirlockFilesWriter;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ImportExportUtilities {

    static final String L_BRACKET = "[[[";
    static final String R_BRACKET = "]]]";
    static final String PLACEHOLDER_TAG = "xliff:g";
    public static final Logger logger = Logger.getLogger(AirlockFilesWriter.class.getName());
    static TreeMap<String,String> localeMap = new TreeMap<String,String>();


    public boolean doDefaultSubfolder(File subFile, HashMap<String,OriginalString> originalStringMap,boolean keepData, boolean preserveFormat) throws Exception{ return false;}

    public void doLocaleSubfolder(File subFile, HashMap<String,OriginalString> originalStringMap, LinkedList<String> supportedLanguages,boolean keepData, boolean preserveFormat) throws Exception{ }

    public String getFormattedString(String str, boolean preserveFormat) { return str; }
    
    public File exportToZipFile(String seasonId,LinkedList<OriginalString> originalStrings)throws Exception {return null;}

    public static String unzip(Season season, FileInputStream inputStream, boolean overwrite) throws Exception {
        String separator = "/";
        String fileName = "temp"+separator+ "tempExtraction"+separator+season.getUniqueId().toString()+separator +"tempStrings";
        byte[] buf = new byte[1024];
        ZipEntry zipentry;
        ZipInputStream zipinputstream = new ZipInputStream(inputStream);

        zipentry = zipinputstream.getNextEntry();
        while (zipentry != null) {
            if(zipentry.getName() == null){
                throw new Exception("zip file is corrupted");
            }
            logger.info("Extracting file: "+zipentry.getName());
            FileOutputStream fileoutputstream;
            if (zipentry.isDirectory()) {
                zipentry = zipinputstream.getNextEntry();
                continue;
            }
            String[] pathSplit = zipentry.getName().split("/");
            if(pathSplit.length < 2){
                continue;
            }
            String directoryName = pathSplit[pathSplit.length-2];
            File newDirectory = new File(fileName, directoryName);
            if(!newDirectory.exists()) {
                Boolean success = newDirectory.mkdirs();
                if (!success) {
                    throw new Exception("Could not create directory: " + newDirectory.getName());
                }
            }
            File newFile = new File(fileName+separator+directoryName, pathSplit[pathSplit.length-1]);

            if (newFile.exists() && overwrite) {
                Boolean success = newFile.delete();
                if(!success){
                    throw new Exception("Could not overwrite file: "+newFile.getName());
                }
            }
            fileoutputstream = new FileOutputStream(newFile);

            int len;
            while ((len = zipinputstream.read(buf)) > 0) {
                fileoutputstream.write(buf, 0, len);
            }

            fileoutputstream.close();
            zipinputstream.closeEntry();
            zipentry = zipinputstream.getNextEntry();

        }
        zipinputstream.close();
        return fileName;
    }

    public void zip(String zipFilePath,String sourceFolder) throws Exception{
        byte[] buffer = new byte[1024];
        String source = new File(sourceFolder).getName();
        File zipFile = new File(zipFilePath);
        if(zipFile.exists()){
            zipFile.delete();
        }
        FileOutputStream fos = new FileOutputStream(zipFilePath);
        ZipOutputStream zos = new ZipOutputStream(fos);
        logger.info("Zipping folder: "+sourceFolder+ "to zipFile: "+zipFilePath);
        FileInputStream in = null;
        File sourceDirectory = new File(sourceFolder);
        List <String> fileList = new ArrayList<>();
        generateFileList(sourceDirectory,sourceFolder,fileList);
        for (String file: fileList) {
            logger.info("File Added : " + file);
            ZipEntry ze = new ZipEntry(source + File.separator + file);
            zos.putNextEntry(ze);
            try {
                in = new FileInputStream(sourceFolder + File.separator + file);
                int len;
                while ((len = in .read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }
            } finally {
                if(in != null)
                    in.close();
            }
        }
        zos.closeEntry();
        zos.close();
        FileUtils.deleteDirectory(new File(sourceFolder));
        logger.info("Folder successfully compressed");

    }

    public void generateFileList(File node, String sourceFolder,List <String> fileList) {
        // add file only
        if (node.isFile()) {
            fileList.add(generateZipEntry(node.toString(),sourceFolder));
        }

        if (node.isDirectory()) {
            String[] subNote = node.list();
            for (String filename: subNote) {
                generateFileList(new File(node, filename),sourceFolder,fileList);
            }
        }
    }

    private String generateZipEntry(String file, String sourceFolder) {
        return file.substring(sourceFolder.length() + 1, file.length());
    }

    public String removeBackslash(String in)
    {
        in = in.replace("\\'", "'");
        return in;
    }

    public String restoreBackslash(String in)
    {
        in = in.replace("'", "\\'");
        return in;
    }


}
