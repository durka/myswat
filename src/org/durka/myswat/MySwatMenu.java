package org.durka.myswat;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.LinkedHashMap;

import nu.xom.Nodes;

import org.durka.myswat.MySwat;

public class MySwatMenu extends ListActivity {
	private MySwat myswat;
	private LinkedHashMap<String, String> menu;
	
	private abstract class Callee
	{
		public abstract void call(String str);
	}
	
	private class MenuTask extends AsyncTask<String, Void, Boolean>
	{
		public LinkedHashMap<String, String> menu;
		public String title;
		private Activity parent;
		
		public MenuTask(Activity p)
		{
			parent = p;
		}
		
		protected Boolean doInBackground(String... uris)
		{
			String page = myswat.get_page(uris[0]);
			menu = myswat.parse_menu(page);
			Nodes stuff = myswat.xpath(page, "//html:title");
			if (stuff.size() > 0)
			{
				title = myswat.xpath(page, "//html:title").get(0).getChild(0).toXML();
				return true;
			}
			
			return false;
		}
		
		protected void onPostExecute(Boolean success)
		{
			if (success)
			{
				parent.setTitle(title);
				((MySwatMenu) parent).create_adapter(menu);
			}
		}
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Bundle joy = getIntent().getExtras();
    	if (joy != null)
    	{
    		myswat = (MySwat) joy.getSerializable("myswat");
    		String name = joy.getString("name");
    		String uri = joy.getString("uri");
    		
    		Toast.makeText(this, "Loading " + name + "...", Toast.LENGTH_LONG).show();
    		new MenuTask(this).execute(uri);
    	}
    	else
    	{
    		final MySwatMenu that = this;
    		
    		ask("Username:", new Callee() {
				public void call(String str) {
					final String username = str;
					ask("Password:", new Callee() {
						public void call(String str) {
							String password = str;
							myswat = new MySwat(username, password);
							
							Toast.makeText(that, "Logging in...", Toast.LENGTH_LONG).show();
				    		new MenuTask(that).execute(myswat.MAIN_MENU);
						}
					});
				}
    		});
    	}
    }
    
    private void ask(String title, final Callee ok)
    {
    	AlertDialog.Builder alert = new AlertDialog.Builder(this);

    	alert.setTitle(title);

    	// Set an EditText view to get user input 
    	final EditText input = new EditText(this);
    	alert.setView(input);

    	alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int whichButton) {
    			ok.call(input.getText().toString());
    		}
    	});

    	alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int whichButton) {
    			// Canceled.
    	  	}
    	});

    	alert.show();
	}

	public void onResume()
    {
    	super.onResume();
    	
    	if (menu != null)
    	{
    		create_adapter();
    	}
    }
    
    private void create_adapter(LinkedHashMap<String, String> m)
    {
    	menu = m;
    	create_adapter();
    }
    
    private void create_adapter()
    {
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
		
		for (String name : menu.keySet())
		{
			adapter.add(name);
		}
		
		setListAdapter(adapter);
	}

	public void onPause()
    {
    	super.onPause();
    	myswat.logout();
    }
	
	@Override
	protected void onListItemClick(ListView list, View view, int position, long id)
	{
		super.onListItemClick(list, view, position, id);
		
		String name = (String)getListAdapter().getItem(position);
		String uri = menu.get(name);
		
		if (uri.contains("P_GenMenu"))
		{
			Intent intent = new Intent(this, MySwatMenu.class);
			Bundle joy = new Bundle();
			joy.putSerializable("myswat", myswat);
			joy.putString("name", name);
			joy.putString("uri", uri);
			intent.putExtras(joy);
			
			startActivity(intent);
		}
		else
		{
			Intent intent = new Intent(this, MySwatPage.class);
			Bundle joy = new Bundle();
			joy.putSerializable("myswat", myswat);
			joy.putString("name", name);
			joy.putString("uri", uri);
			intent.putExtras(joy);
			
			startActivity(intent);
		}
	}
}

/* Instructions for operating MySwarthmore
	
	1. Request https://myswat.swarthmore.edu/pls/twbkwbis.P_WWWLogin to get the cookies
	2. POST https://myswat.swarthmore.edu/pls/twbkwbis.P_ValLogin to log in
			data: sid=username&PIN=password
	3. The first menu is at https://myswat.swarthmore.edu/pls/twbkwbis.P_GenMenu?name=bmenu.P_MainMnu&msg=WELCOME+
	4. To log out, request https://myswat.swarthmore.edu/pls/twbkwbis.P_Logout with referer set to the menu
*/