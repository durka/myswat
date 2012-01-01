package org.durka.myswat;

import android.content.Context;
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
			          Toast.makeText(Directory.this, terms, Toast.LENGTH_SHORT).show();
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
