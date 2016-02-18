package packages;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.*;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;


/** Simple example of JNA interface mapping and usage. */
public class EmoHomeControl 
{      
	static HttpPost postChange;
	static Timer timer = new Timer();
    static boolean timeout = false;
    static private String auth = null;
    static private String apiLoc = null;
	
    public static void main(String[] args) 
    {
    	Pointer eEvent			= Edk.INSTANCE.EE_EmoEngineEventCreate();
    	Pointer eState			= Edk.INSTANCE.EE_EmoStateCreate();
    	IntByReference userID 	= null;
    	short composerPort		= 1726;
    	short enginePort		= 3008;
    	int option 				= 1;
    	int state  				= 0;
		int actionState			= 0;
    	
    	userID = new IntByReference(0);
    	    	

		//Connect to EmoEngine
		if (Edk.INSTANCE.EE_EngineRemoteConnect("127.0.0.1", enginePort, "Emotiv Systems-5") != EdkErrorCode.EDK_OK.ToInt()) {
			System.out.println("Emotiv Engine start up failed.");
			return;
		}
		System.out.println("Connected to EmoEngine on [127.0.0.1]");
    	
    	auth = httpCookie();
    	
    	PropertiesFile prop = new PropertiesFile("config.properties");
    	
    	apiLoc = prop.getApiLocation();
    	
		while (true) 
		{
			state = Edk.INSTANCE.EE_EngineGetNextEvent(eEvent);
			
			// New event needs to be handled
			if (state == EdkErrorCode.EDK_OK.ToInt()) {

				int eventType = Edk.INSTANCE.EE_EmoEngineEventGetType(eEvent);
				Edk.INSTANCE.EE_EmoEngineEventGetUserId(eEvent, userID);

				// Log the EmoState if it has been updated
				if (eventType == Edk.EE_Event_t.EE_EmoStateUpdated.ToInt()) {

					Edk.INSTANCE.EE_EmoEngineEventGetEmoState(eEvent, eState);
					float timestamp = EmoState.INSTANCE.ES_GetTimeFromStart(eState);
					System.out.println(timestamp + " : New EmoState from user " + userID.getValue());
					
					System.out.print("WirelessSignalStatus: ");
					System.out.println(EmoState.INSTANCE.ES_GetWirelessSignalStatus(eState));

					//Logging Cognitive Actions 
					System.out.print("CognitivGetCurrentAction: ");
					System.out.println(EmoState.INSTANCE.ES_CognitivGetCurrentAction(eState));
					System.out.print("CurrentActionPower: ");
					System.out.println(EmoState.INSTANCE.ES_CognitivGetCurrentActionPower(eState));
					
					//Check for pushing action at a power over 5.0 and timeout false
					actionState = EmoState.INSTANCE.ES_CognitivGetCurrentAction(eState);
					if (((actionState == 2) || (actionState ==4)) && (EmoState.INSTANCE.ES_CognitivGetCurrentActionPower(eState) > 0.5) && (timeout == false)) {
						System.out.println("Over");
						try {
							restPost(actionState);
							timerStart(true);
						} catch (ClientProtocolException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					
				}
				
			}
			else if (state != EdkErrorCode.EDK_NO_EVENT.ToInt()) {
				System.out.println("Internal error in Emotiv Engine!");
				break;
			}
		}
    	
    	Edk.INSTANCE.EE_EngineDisconnect();
    	System.out.println("Disconnected!");
    }
    
    public static void restPost(int actionState) throws ClientProtocolException, IOException {
    	
    	  switch (actionState) {
    	  case 4:
  			{
  				//Set light value to off
  				try {
					URL url = new URL("http://" + apiLoc + "/ZAutomation/api/v1/devices/ZWayVDev_zway_9-0-37/command/off");
					URLConnection conn = url.openConnection();

					conn.setRequestProperty("Cookie", auth);

					conn.connect();
				  
					DataInputStream response = new DataInputStream(conn.getInputStream());
				  
					System.out.print(response);
				} catch (Exception e) {
					auth = httpCookie();
					e.printStackTrace();
				}
  			}
  				break;
    	  case 2:
			{
				//Set Light value to on
  				try {
					URL url = new URL("http://" + apiLoc + "/ZAutomation/api/v1/devices/ZWayVDev_zway_9-0-37/command/on");
					URLConnection conn = url.openConnection();

					conn.setRequestProperty("Cookie", auth);

					conn.connect();
				  
					DataInputStream response = new DataInputStream(conn.getInputStream());
				  
					System.out.print(response);
				} catch (Exception e) {
					//Try to request cookie in case of failed attempt
					auth = httpCookie();
					e.printStackTrace();
				}
			}
				break;
  		  } 
    }
	
	public static String httpCookie(){
    	
    	HttpClient httpClient = HttpClientBuilder.create().build();
    	String cookie = null;
    	
    	PropertiesFile properties = new PropertiesFile("config.properties");
    	
    	String authString = properties.getAuthString();
    	String apiLocation = properties.getApiLocation();
    	System.out.print(authString + apiLocation);
    	
    	try {
            HttpPost request = new HttpPost("http://" + apiLocation + "/ZAutomation/api/v1/login");
            StringEntity params =new StringEntity(authString);
            request.addHeader("content-type", "application/json");
            request.setEntity(params);
            HttpResponse response = httpClient.execute(request);
            Header[] headers = response.getAllHeaders();
        	for (Header header : headers) {
        		System.out.println("Key : " + header.getName() + " ,Value : " + header.getValue());
        	}

        	cookie = response.getFirstHeader("Set-Cookie").getValue();
        	
        	System.out.print(cookie);
        	
        }catch (Exception ex) {
        	System.out.println("Error while trying to authenticate with service.");
        }
		return cookie;
    }
    
    public static void timerStart(boolean startValue) {
    	if (startValue == true) {
    		timeout = true;
    		//Wait 10 seconds for head set
    		timer.schedule(new TimerTask() {
    			  @Override
    			  public void run() {
    			    timeout = false;
    			    System.out.println("Timeout");
    			  }
    			}, 3000);
    	}
    }

}
