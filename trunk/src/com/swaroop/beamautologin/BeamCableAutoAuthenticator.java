package com.swaroop.beamautologin;

import java.io.IOException;
import java.net.MalformedURLException;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

/**
 * Beam Cable Auto Authenticator - Unofficial
 * Please note that this application does not transmit your credentials
 * The application only stores the credentials offline and is only privately accessible to this application.
 * @author swaroop
 *
 */

public class BeamCableAutoAuthenticator extends Activity {

	// Constants
	public static final String TAG_BEAM_CABLE_AUTO_AUTHENTICATOR = "BeamCableAutoAuthenticator";
	private static final String SHOULD_SAVE_CREDENTIALS_SHAREDPREF_KEY = "shouldSaveCredentials";
	private static final String USERNAME_SHAREDPREF_KEY = "username";
	private static final String PASSWORD_SHAREDPREF_KEY = "password";
	private static final String BLANK_DEFAULT_VALUE = "";

	// These variables will be used throughout the application flow. 
	private static String username = "";
	private static String password = "";
	private static boolean shouldStoreCredentials = true;

	private static String LOGOUT_INTENT_COMMAND = "logout";
	private static String LOGIN_INTENT_COMMAND = "login";

	public enum NETWORK_OPERATION {LOGIN, LOGOUT, DETECT_CONNECTIVITY};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_beam_cable_auto_authenticator);

		privateAction();
	}

	public void privateAction() {

		// Fetch the previously stored credentials 
		getCredentialsFromSharedPreferences();
		
		// Attach the events for Login/Logout buttons
		attachEvents();
		
		// Check if command received from Intent, if yes then don't proceed below. 
		if(verifyIfLaunchedViaURLScheme()) {
			return;
		}


		// Start the login process if the credentials are already stored. Else wait for the user to enter them.
		new NetworkAsyncTask().execute(new NETWORK_OPERATION[] {NETWORK_OPERATION.LOGIN});
	}

	private boolean verifyIfLaunchedViaURLScheme() {
		Uri data = getIntent().getData();

		if(data == null) {
			return false;
		}

		String commandReceived = data.getHost();
		if(LOGIN_INTENT_COMMAND.equalsIgnoreCase(commandReceived)) {
			new NetworkAsyncTask().execute(new NETWORK_OPERATION[] {NETWORK_OPERATION.LOGIN});
			return true;
		} else if(LOGOUT_INTENT_COMMAND.equalsIgnoreCase(commandReceived)) {
			new NetworkAsyncTask().execute(new NETWORK_OPERATION[] {NETWORK_OPERATION.LOGOUT});
			return true;
		} 

		// If no command is received, then ignore the input command and display the activity		
		return false;
	}

	/**
	 * Attach Events for the Login, Logout buttons. 
	 */
	private void attachEvents() {
		Button loginButton = (Button) findViewById(R.id.loginManually);
		loginButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				new NetworkAsyncTask().execute(new NETWORK_OPERATION[] {NETWORK_OPERATION.LOGIN});
			}
		});

		Button logoutButton = (Button) findViewById(R.id.logoutManually);
		logoutButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				new NetworkAsyncTask().execute(new NETWORK_OPERATION[] {NETWORK_OPERATION.LOGOUT});
			}
		});


	}

	/**
	 * Detect Internet connectivity
	 */
	private void detectConnectivityInternal() {

		// Wait for a while before checking Connectivity
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// Supress the wait. 
		}

		TextView connectivityStatusField = (TextView) findViewById(R.id.connectivityStatusField);
		setTextToTextView(connectivityStatusField, R.string.status_check_in_progress_label);

		try {
			boolean isConnected = BeamAuthenticatorUtil.detectConnectivity();
			if(isConnected) {
				setTextToTextView(connectivityStatusField, R.string.connected_label);
			} else {
				setTextToTextView(connectivityStatusField, R.string.not_connected_label);
			}

		} catch(Exception ex) {
			Log.e(TAG_BEAM_CABLE_AUTO_AUTHENTICATOR, "Error while determining network connectivity", ex);
			displayToast("Misc Error: " + ex.getMessage());
			setTextToTextView(connectivityStatusField, R.string.error_label);
		}
	}

	/*
	 * Do the Connectivity Determination in an Async thread 
	 * as Android wouldn't allow N/w requests on the main UI thread. 
	 */
	private class NetworkAsyncTask
	extends AsyncTask<NETWORK_OPERATION, Void, Void> {

		@Override
		protected Void doInBackground(NETWORK_OPERATION... params) {

			try {
				switch(params[0]) {
				case LOGIN: 
					loginInternal();
					detectConnectivityInternal();
					break;
				case LOGOUT:
					logoutInternal();
					detectConnectivityInternal();
					break;
				case DETECT_CONNECTIVITY:
					detectConnectivityInternal();
					break;
				default:
					displayToast(R.string.network_operation_not_supported_error);
				}
			} catch(Exception ex) {
				displayToast("Misc Error: " + ex.getMessage());
				Log.e(TAG_BEAM_CABLE_AUTO_AUTHENTICATOR, "Error while determining network connectivity", ex);
			}

			return null;		
		}
	}


	/**
	 * Utility method checks if the input string is blank or null. 
	 * @param str
	 * @return
	 */
	private boolean isNullOrBlank(String str) {
		return (str == null || str.trim().equals(BLANK_DEFAULT_VALUE));
	}

	/**
	 * Fetches data from the UI field and populates the member variables which are used by
	 * by all the other methods. 
	 * @return
	 */
	private  boolean fetchDetailsFromUI() {
		username = getLoginFieldReference().getText().toString();
		password = getPasswordFieldReference().getText().toString();

		if(isNullOrBlank(username)) {
			displayToast(R.string.username_empty_error);
			return false;
		}

		if(isNullOrBlank(password)) {
			displayToast(R.string.password_empty_error);
			return false;
		}

		shouldStoreCredentials = getSaveCredentialsToggleButton().isChecked();

		log("Credentials found on the UI.. Good job");
		return true;
	}

	/**
	 * Stores the username, password into Shared Preference if the "Remember Credentials is ON"
	 */
	private void storeCredentialsIntoSharedPreferences() {

		fetchDetailsFromUI();

		log("Should Save Credentials is " + shouldStoreCredentials);

		// Keep the Preference private. 
		if(!shouldStoreCredentials) {
			log("Should not save credentials, skipping the store process");

			clearSavedCredentials();
			return;
		}

		SharedPreferences sharedpreferences = getSharedPreferences("BeamCableCredentials", Context.MODE_PRIVATE);
		Editor editor = sharedpreferences.edit();

		// If the details have to be stored proceed
		if(!isNullOrBlank(username)) {
			editor.putString(USERNAME_SHAREDPREF_KEY, username);
			log("Username stored into SharedPreferences");
		}

		if(!isNullOrBlank(password)) {
			editor.putString(PASSWORD_SHAREDPREF_KEY, password);
			log("Password stored into SharedPreferences");
		}

		editor.putBoolean(SHOULD_SAVE_CREDENTIALS_SHAREDPREF_KEY, shouldStoreCredentials);

		editor.commit();

	}

	/**
	 * Fetches any stored Shared preferences
	 */
	private void getCredentialsFromSharedPreferences() {

		SharedPreferences sharedpreferences = getSharedPreferences("BeamCableCredentials", Context.MODE_PRIVATE);

		String userNameFromPreferences = sharedpreferences.getString(USERNAME_SHAREDPREF_KEY, BLANK_DEFAULT_VALUE);
		String passwordFromPreferences = sharedpreferences.getString(PASSWORD_SHAREDPREF_KEY, BLANK_DEFAULT_VALUE);

		if(!isNullOrBlank(userNameFromPreferences)) {
			username = userNameFromPreferences;
			getLoginFieldReference().setText(userNameFromPreferences);
			log("Username field populated from SharedPreference");
		} else {
			log("Username field blanked as no value found in SharedPreferences");
		}

		if(!isNullOrBlank(passwordFromPreferences)) {
			password = passwordFromPreferences;
			getPasswordFieldReference().setText(passwordFromPreferences);
			log("Password field populated from SharedPreference");
		} else {
			log("Password field blanked as no value found in SharedPreferences");
		}

		// If there is no previous setting then set it to Save (true).
		shouldStoreCredentials = sharedpreferences.getBoolean(SHOULD_SAVE_CREDENTIALS_SHAREDPREF_KEY, true);
		getSaveCredentialsToggleButton().setChecked(shouldStoreCredentials);

	}

	/**
	 * Returns the Field Reference from the UI 
	 * @return
	 */

	private EditText getLoginFieldReference() {
		return (EditText) findViewById(R.id.loginField);
	}

	/**
	 * Returns the Field Reference from the UI 
	 * @return
	 */
	private EditText getPasswordFieldReference() {
		return (EditText) findViewById(R.id.passwordField);
	}

	/**
	 * Fetches the UI reference for the Save Credentials Toggle field. 
	 * @return
	 */
	private ToggleButton getSaveCredentialsToggleButton() {
		return (ToggleButton) findViewById(R.id.saveCredentialsToggleField);
	}

	/**
	 * Sets a Message Resource String to the UI.
	 * @param field
	 * @param message
	 */
	private void setTextToTextView(final TextView field, final int message) {

		runOnUiThread(new Thread("Set Text to Field " + field + " to " + message) {
			public void run() {
				field.setText(message);
			}
		});
	}

	/**
	 * Setting a Message String to the UI.
	 * @param field
	 * @param message
	 */
	private void setTextToTextView(final TextView field, final String message) {

		runOnUiThread(new Thread("Set Text to Field " + field + " to " + message) {
			public void run() {
				field.setText(message);
			}
		});
	}

	/**
	 * Display a Toast with an error
	 * @param errorCode
	 */
	private void displayToast(final int errorCode) {

		runOnUiThread(new Thread("Display Toast") {
			public void run() {
				Toast.makeText(getApplicationContext(), errorCode, Toast.LENGTH_LONG)
				.show();
			}
		});
	}

	private void displayToast(final String message) {
		runOnUiThread(new Thread("Display Toast") {
			public void run() {
				Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG)
				.show();

				displayNotification(message);
			}
		});

	}

	@SuppressLint("NewApi")
	private void displayNotification(final String message) {

		Intent intent = new Intent(this, BeamCableAutoAuthenticator.class);
		PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);

		Notification n  = new Notification.Builder(this)
		.setContentTitle(getResources().getString(R.string.app_name))
		.setContentText(message)
		.setSmallIcon(R.drawable.ic_launcher)
		.setContentIntent(pIntent)
		.setAutoCancel(true)
		.build();


		NotificationManager notificationManager = 
				(NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		notificationManager.notify(0, n); 
	}

	private void log(String message) {
		Log.d(TAG_BEAM_CABLE_AUTO_AUTHENTICATOR, message);
	}


	/** 
	 * Takes care of calling the Beam Authenticator Util and fetches the logged-in username and IP address to be displayed on the UI. 
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	private void loginInternal() throws MalformedURLException, IOException {
		log("Logging IN Start");
		String[] loginData = BeamAuthenticatorUtil.login(username, password);
		if(loginData != null && loginData.length == 2) {
			log("Successfully Logged in with User - " + loginData[0] + " And at IP Address : " + loginData[1]);

			String loggedInUserValue = loginData[0];
			loggedInUserValue = loggedInUserValue != null ? loggedInUserValue.trim() : loggedInUserValue;

			TextView loggedInUser = (TextView) findViewById(R.id.loggedInUserField);
			setTextToTextView(loggedInUser, loggedInUserValue);

			String ipAddressValue = loginData[1];
			ipAddressValue = ipAddressValue != null ? ipAddressValue.trim() : ipAddressValue;

			TextView ipAddress = (TextView) findViewById(R.id.ipAddressField);
			setTextToTextView(ipAddress, ipAddressValue);

			displayNotification("Login Success - IP Address : " + ipAddressValue);

		} else {
			emptyRuntimeDetails();
		}
		storeCredentialsIntoSharedPreferences();
		log("Logging IN End");
	}

	private void logoutInternal() throws IOException {
		log("Logging Out Start");
		BeamAuthenticatorUtil.logout();
		emptyRuntimeDetails();
		log("Logging Out end");
	}

	private void emptyRuntimeDetails() {
		TextView loggedInUser = (TextView) findViewById(R.id.loggedInUserField);
		setTextToTextView(loggedInUser, R.string.not_determined_label);

		TextView ipAddress = (TextView) findViewById(R.id.ipAddressField);
		setTextToTextView(ipAddress, R.string.not_determined_label);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.layout.menu, menu);
		return true;
	}

	private void clearSavedCredentials() {

		SharedPreferences sharedpreferences = getSharedPreferences("BeamCableCredentials", Context.MODE_PRIVATE);
		Editor editor = sharedpreferences.edit();

		editor.remove(USERNAME_SHAREDPREF_KEY);
		editor.remove(PASSWORD_SHAREDPREF_KEY);
		editor.commit();

		log("Clear saved credentials from SharedPreferences");

		displayToast(R.string.cleared_saved_credentials);
	}

	private void displayAbout() {
		Intent aboutPage = new Intent(this, AboutActivity.class);
		startActivity(aboutPage);
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{

		switch (item.getItemId())
		{
		case R.id.menu_clear_credentials:
			clearSavedCredentials();
			return true;
		case R.id.menu_about:
			displayAbout();
			return true;
		default:
			log("Menu Option not recognized.. Something wrong");
		}

		return false;
	}	


}
