package org.durka.myswat;

import com.unboundid.ldap.sdk.LDAPConnection;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class Directory extends MySwatActivity {
	private String terms;
	private EditText input;
	private ListView output;
	private ArrayAdapter<String> output_adapter;
	
	private static final String CYGNET = "http://cygnet.sccs.swarthmore.edu/";
	private static final String CYGNET_PHOTOS = "photos/";
	private static final String CYGNET_QUERY = "backend.py?terms=";
	
	private class QueryTask extends AsyncTask<String, Void, String[]> {

		@Override
		protected String[] doInBackground(String... queries) {
			if (queries.length == 1)
			{
				String query = queries[0];
			
				// can't do either of these except on campus
				
				//String cygnet = Utils.do_http(CYGNET + CYGNET_QUERY + query);
				
				//LDAPConnection ldap = new LDAPConnection("directory.swarthmore.edu", 389);
				
				return null;
			}
			else
			{
				return null;
			}
		}
		
		@Override
		protected void onPostExecute(String[] results)
		{
			for (int i = 0; i < results.length; ++i)
			{
				Toast.makeText(getApplicationContext(), results[i], Toast.LENGTH_SHORT).show();
			}
		}
	}

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.directory);
        
        terms = getPreferences(Context.MODE_PRIVATE).getString("terms", "");
        
        input = (EditText) findViewById(R.id.terms);
        output = (ListView) findViewById(R.id.results);
        
        output_adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        output.setAdapter(output_adapter);
        
        input.setOnKeyListener(new EditText.OnKeyListener() {

			public boolean onKey(View view, int key, KeyEvent event)
			{
				terms = input.getText().toString();
				
				if ((event.getAction() == KeyEvent.ACTION_DOWN) && (key == KeyEvent.KEYCODE_ENTER))
				{
			          new QueryTask().execute(terms);
			          return true;
			    }
				
				return false;
			}
        	
        });
    }
    
    @Override
    public void onPause()
    {
    	getPreferences(Context.MODE_PRIVATE).edit()
    		.putString("terms", terms)
    		.commit();
    	
    	super.onPause();
    }
}
