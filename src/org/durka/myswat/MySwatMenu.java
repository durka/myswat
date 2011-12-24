package org.durka.myswat;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.http.SslError;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.webkit.ConsoleMessage;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;

import nu.xom.Nodes;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.durka.myswat.MySwat;

public class MySwatMenu extends Activity {
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
		private ListView out;
		
		public MenuTask(ListView list)
		{
			out = list;
		}
		
		protected Boolean doInBackground(String... pages)
		{
			String page = pages[0];
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
				final ArrayAdapter<String> adapter = new ArrayAdapter<String>(out.getContext(), android.R.layout.simple_list_item_1);

				for (String name : menu.keySet())
				{
					adapter.add(name);
				}

				out.setAdapter(adapter);
				
				out.setOnItemClickListener(new AdapterView.OnItemClickListener() {
					public void onItemClick(AdapterView<?> parent, View view, int position, long id)
					{
						String name = (String)adapter.getItem(position);
						String uri = menu.get(name);
						
						if (uri.startsWith("/"))
						{
							uri = "https://myswat.swarthmore.edu" + uri;
						}
						((WebView)findViewById(R.id.web)).loadUrl(uri);
					}
				});
			}
		}
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

		myswat = new MySwat();
		final Activity activity = this;
		
		// populate the webview
		WebView web = (WebView)findViewById(R.id.web);
		web.getSettings().setBuiltInZoomControls(true);
		web.getSettings().setJavaScriptEnabled(true);
		
		web.setWebViewClient(new WebViewClient() {
			public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
			     Toast.makeText(activity, "Oh no! " + description, Toast.LENGTH_SHORT).show();
			   }
			public void onReceivedSslError (WebView view, SslErrorHandler handler, SslError error) {
				 handler.proceed();
			   }
			
			public void onPageFinished(WebView view, String address)
			{
				try {
					URI url = new URI(address);
					if (url.getPath().contains("P_GenMenu"))
					{
						view.loadUrl("javascript:console.log('HERPDERP'+document.getElementsByTagName('html')[0].innerHTML);");
					}
				} catch (URISyntaxException e) {
					// no-op
				}
			}
		});
		
		web.setWebChromeClient(new WebChromeClient() {
			public boolean onConsoleMessage(ConsoleMessage cmsg)
			{
				String msg = cmsg.message();
				
				if (msg.startsWith("HERPDERP"))
				{
					Toast.makeText(activity, "got html", Toast.LENGTH_SHORT).show();
					new MenuTask((ListView)findViewById(R.id.list)).execute(msg.substring(8));
					return true;
				}
				
				return false;
			}
		});
		
		Toast.makeText(this, "Loading MySwat...", Toast.LENGTH_SHORT).show();
		web.loadUrl("https://myswat.swarthmore.edu");
    }
    
    private void ask(String title, boolean password, final Callee ok)
    {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);

    	builder.setTitle(title);

    	// Set an EditText view to get user input 
    	final EditText input = new EditText(this);
    	if (password)
    	{
    		input.setTransformationMethod(new PasswordTransformationMethod());
    	}
    	builder.setView(input);
    	
    	builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int whichButton) {
    			ok.call(input.getText().toString());
    		}
    	});
    	
    	final AlertDialog alert = builder.create();
    	
    	input.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(View v, int keycode, KeyEvent evt) {
				if (evt.getAction() == KeyEvent.ACTION_DOWN && keycode == KeyEvent.KEYCODE_ENTER)
				{
					alert.dismiss();
					ok.call(input.getText().toString());
					return true;
				}
				return false;
			}
    	});

    	alert.show();
	}

	public void onResume()
    {
    	super.onResume();
    	
    	if (menu != null)
    	{
    		//create_adapter();
    	}
    }

	public void onPause()
    {
    	super.onPause();
    	if (myswat != null)
    	{
    		myswat.logout();
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