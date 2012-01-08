package org.durka.trashmower;
import android.os.Bundle;
import android.preference.PreferenceActivity;


public class Preferences extends PreferenceActivity {
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.prefs);
	}

}
