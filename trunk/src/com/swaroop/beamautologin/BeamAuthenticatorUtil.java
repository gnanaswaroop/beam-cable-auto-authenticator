package com.swaroop.beamautologin;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class BeamAuthenticatorUtil {

	public static boolean detectConnectivity() throws IOException {

		String sURL = "http://google.com";

		URL uri = new URL(sURL);
		HttpURLConnection huc = (HttpURLConnection) uri.openConnection();
		huc.connect();
		int responseCode = huc.getResponseCode();

		if(responseCode == 200) {
			BufferedReader in = null;  
			in = new BufferedReader(new InputStreamReader(huc.getInputStream()));

			String s = null;
			while((s = in.readLine()) != null) {
				if(s.contains("http://portal.beamtele.com")) {
					log("Detected offline, returning status");
					return false;
				}
			}
		}

		log("Offline detection keywords not present, Assuming to be online");
		return true;
	}

	private static void log(String message) {
		Log.d(BeamCableAutoAuthenticator.TAG_BEAM_CABLE_AUTO_AUTHENTICATOR, message);
	}

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
