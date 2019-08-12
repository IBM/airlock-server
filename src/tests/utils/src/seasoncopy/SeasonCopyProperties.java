package tests.utils.src.seasoncopy;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
 
/**
 * @author Crunchify.com
 * 
 */
 
public class SeasonCopyProperties {

	private static InputStream inputStream;
	private static Properties prop;
 
	public static Properties getPropValues(String configPath) throws IOException {
		
 
		try {
			prop = new Properties();
			String propFileName = configPath;
 
			//inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);
			inputStream = new FileInputStream(propFileName);
 
			if (inputStream != null) {
				prop.load(inputStream);
			} else {
				throw new FileNotFoundException("Cannot read property file " + propFileName);
			}
 

		} catch (Exception e) {
			System.out.println("Exception: " + e);
		} finally {
			inputStream.close();
		}
		return prop;
	}
}