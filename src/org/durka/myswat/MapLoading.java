package org.durka.myswat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class MapLoading extends Activity {
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.maploading);
        
        new Handler().post(new Runnable() {
        	public void run()
        	{
        		startActivity(new Intent(MapLoading.this, Map.class));
        	}
        });
    }

}
