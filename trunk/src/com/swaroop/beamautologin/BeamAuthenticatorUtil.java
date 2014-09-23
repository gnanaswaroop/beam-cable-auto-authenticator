package com.swaroop.beamautologin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.util.Log;

public class BeamAuthenticatorUtil {

	/**
	 * Detects Connectivity to google.com (assumes it's a website that never goes down)
	 * @return
	 * @throws IOException
	 */
	public static boolean detectConnectivityBoolean() throws IOException {

		String[] loginDetails = detectConnectivityAndFetchLoginDetails();
		if(loginDetails == null || loginDetails.length == 0) {
			return false;
		}
		return true;
	}

	/**
	 * Changed logic to support the new way to verify if the user is logged on and connected to a beam network.
	 * @return
	 * @throws IOException
	 */
	public static String[] detectConnectivityAndFetchLoginDetails() throws IOException {

		long currentTime = System.currentTimeMillis();

		String sURL = "http://portal.beamtele.com/newportal/Ajax.php?function=checkIsLoggedOn&time="+ currentTime;

		URL uri = new URL(sURL);
		HttpURLConnection huc = (HttpURLConnection) uri.openConnection();
		huc.connect();
		int responseCode = huc.getResponseCode();

		if(responseCode == 200) {
			BufferedReader in = null;  
			in = new BufferedReader(new InputStreamReader(huc.getInputStream()));
	
			String s = null;
			while((s = in.readLine()) != null) {
				
				// If the response is 0, then it means the user is not currently logged on. 
				if("0".equals(s)) {
					log("Not Logged in to Beam Network");
					return null;
				} else if(s.contains("at")){
					String[] data = s.split(" at");

					if(data.length >= 2) {
						String successUserName = data[0];
						String ipAddress = data[1];

						// Remove any Single Quotes found
						successUserName = successUserName.replace("'", "").trim();
		
						// Remove the extra characters - &nbsp;-&nbsp;EC
						ipAddress = ipAddress.replace("&nbsp;-&nbsp;EC", "").trim();
						
						log("Username - " + successUserName);
						log("IP Address - " + ipAddress);
						log("Beam Cable Connectivity Verified");

						return new String[] {successUserName, ipAddress};
					} 
				} else {
					log("Response received for the CheckIsLoggedOn request is " + s + " And assuming to be offline");
					return null;
				}
			}
		} 

		log("HTTP Code is not Ok to proceed, assuming to be offline " + responseCode);
		return null;

	}
	
	private static void log(String message) {
		Log.d(BeamCableAutoAuthenticator.TAG_BEAM_CABLE_AUTO_AUTHENTICATOR, message);
	}

	/**
	 * This method takes care of the Login operation into the Beam Network. 
	 * This follows the same pattern as how the Web Browser logs into the Beam Cable Portal. 
	 * @param username
	 * @param password
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	public static String[] login(String username, String password)
			throws MalformedURLException, IOException {

		long currentTime = System.currentTimeMillis();

		String loginURL = "http://portal.beamtele.com/newportal/Ajax.php?function=ExecuteLogin&user=" + username + "&pwd=" + password +"&remember=true&timestamp=" + currentTime;

		URL uri = new URL(loginURL);
		HttpURLConnection huc = (HttpURLConnection) uri.openConnection();
		huc.connect();
		int responseCode = huc.getResponseCode();

		if (responseCode == HttpURLConnection.HTTP_OK) {  
			BufferedReader in = null;  
			in = new BufferedReader(new InputStreamReader(huc.getInputStream()));

			String s = null;
			while((s = in.readLine()) != null) {

				if(s.contains("at")) {
					String[] data = s.split(" at");

					if(data.length >= 2) {
						String successUserName = data[0];
						String ipAddress = data[1];

						log("Username - " + successUserName.trim());
						log("IP Address - " + ipAddress.trim());
						log("Successfully logged-in");

						return data;
					} 
				} else {
					log("Could not parse response " + s + " but successfully logged in");
					return new String[]{};
				}


			}
		} else {
			log("Error in network connection - Are you sure your inline is up? ");
		}

		return null;

	}

	/**
	 * Method logs the user out of the Beam Cable network. 
	 * This URL is specific for Beam Cable ISP 
	 * @return
	 * @throws IOException
	 */
	public static boolean logout() throws IOException {

		String logoutURL = "http://portal.beamtele.com/newportal/Ajax.php?function=userLogout";

		URL uri = new URL(logoutURL);
		HttpURLConnection huc = (HttpURLConnection) uri.openConnection();
		huc.connect();
		int responseCode = huc.getResponseCode();

		if (responseCode == HttpURLConnection.HTTP_OK) {  
			log("Successfully Logged out");
			return true;
		} else {
			log("Failed to Logout - Were you logged in previously?");
			return false;
		}
	}

}
