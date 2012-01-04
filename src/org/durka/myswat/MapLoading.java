package org.durka.myswat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

public class MapLoading extends Activity {
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.d("MapLoading", "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.maploading);
        
        new Handler().post(new Runnable() {
        	public void run()
        	{
        		startActivity(new Intent(MapLoading.this, Map.class));
        	}
        });
    }

    @Override
    public void onPause()
    {
    	Log.d("MapLoading", "onPause");
    	super.onPause();
    }
}
