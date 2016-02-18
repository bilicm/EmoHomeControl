package packages;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesFile {
	String fForm = null;
	String fLogin = null;
	String fPass = null;
	String fKeepMe = null;
	String fDefUI = null;
	String fApiIp = null;
	String fApiPort = null;
  
	public PropertiesFile(String filename) {

		Properties prop = new Properties();
		InputStream input = null;
	
		try {
	
			input = new FileInputStream(filename);
	
			// load a properties file
			prop.load(input);
	
			// get the property value and print it out
			fForm = prop.getProperty("form");
			fLogin = prop.getProperty("login");
			fPass = prop.getProperty("password");
			fKeepMe = prop.getProperty("keepme");
			fDefUI = prop.getProperty("default_ui");
			fApiIp = prop.getProperty("api_ip");
			fApiPort = prop.getProperty("api_port");
	
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public String getAuthString(){
		String auth;
		auth = "{\"form\": " + fForm + ", \"login\": \"" + fLogin + "\", \"password\": \"" + fPass +"\", \"keepme\": \"" + fKeepMe + "\", \"default_ui\": " + fDefUI + "}";
		
		return auth;
	}
	
	public String getApiLocation(){
		return fApiIp + ":" + fApiPort;
	}
  
}