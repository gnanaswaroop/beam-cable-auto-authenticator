package com.swaroop.beamautologin;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public class AboutActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);

		TextView homePage = (TextView) findViewById(R.id.project_home_page_field);
		if (homePage != null) {
			homePage.setMovementMethod(LinkMovementMethod.getInstance());
		}

		TextView disclaimer = (TextView) findViewById(R.id.disclaimer);
		if (disclaimer != null) {
			disclaimer.setMovementMethod(LinkMovementMethod.getInstance());
		}
	}

}
