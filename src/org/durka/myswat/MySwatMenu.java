package org.durka.myswat;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
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
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.Toast;

public class MySwatMenu extends Activity {
	private LinkedHashMap<String, String> menu;
	private ArrayAdapter<String> adapter;
	private URI currentURI;
	private String username, password;
	private int login_attempts;
	
	private final static int LOGIN_ATTEMPT_LIMIT = 2;
	
	private abstract class Callee
	{
		public abstract void call(String str);
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        username = "";
        password = "";
        login_attempts = 0;

		final Activity activity = this;
		
		menu = new LinkedHashMap<String, String>();
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
		ListView list = (ListView)findViewById(R.id.list);
		list.setAdapter(adapter);
		list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				String name = (String)adapter.getItem(position);
				String uri = menu.get(name);
				
				if (uri.startsWith("/"))
				{
					uri = currentURI.getScheme() + "://" + currentURI.getHost() + "/" + uri;
				}
				((WebView)findViewById(R.id.web)).loadUrl(uri);
			}
		});
		
		// populate the webview
		WebView web = (WebView)findViewById(R.id.web);
		web.getSettings().setBuiltInZoomControls(true);
		web.getSettings().setJavaScriptEnabled(true);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Loading...");
		final AlertDialog loading = builder.create();
		
		web.setWebViewClient(new WebViewClient() {
			public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
			     Toast.makeText(activity, "Oh no! " + description, Toast.LENGTH_SHORT).show();
			   }
			public void onReceivedSslError (WebView view, SslErrorHandler handler, SslError error) {
				 handler.proceed();
			   }
			
			public void onPageStarted(WebView view, String address, Bitmap favicon)
			{
				adapter.clear();
				menu.clear();
				
				loading.show();
				Log.d("MySwat", "Loading " + address);
			}
			
			public void onPageFinished(WebView view, String address)
			{
				loading.hide();
				Log.d("MySwat", "Loaded " + address);
				
				try {
					currentURI = new URI(address);
					boolean handled = false;
					if (currentURI.getPath().contains("P_GenMenu"))
					{
						login_attempts = 0;
						
						view.loadUrl("javascript:(function(rows){" +
								"for (var i = 0; i < rows.length; ++i)" +
								"{" +
									"if (rows[i].nodeName == 'TR')" +
									"{" +
										"var tds = rows[i].childNodes;" +
										"for (var j = 0; j < tds.length; ++j)" +
										"{" +
											"if (tds[j].childNodes.length > 1)" +
											"{" +
												"console.log('HERPDERP' + tds[j].childNodes[1].innerHTML + 'DERPHERP' + tds[j].childNodes[1].href);" +
											"}" +
										"}" +
									"}" +
								"}" +
								"})(document.getElementsByClassName('menuplaintable')[0].childNodes[1].childNodes)");
						handled = true;
					}
					else if (currentURI.getPath().contains("P_WWWLogin"))
					{
						++login_attempts;
						Log.d("MySwat", "Login attempt #" + Integer.toString(login_attempts));
						if (login_attempts >= LOGIN_ATTEMPT_LIMIT)
						{
							Toast.makeText(activity, "ERROR: Too many login failures.", Toast.LENGTH_SHORT).show();
						}
						else
						{
							if (username.equals(""))
							{
								ask("Username:", username, false, new Callee() {
									public void call(String str) {
										username = str;
										ask("Password:", password, true, new Callee() {
											public void call(String str) {
												password = str;
												
												((WebView)findViewById(R.id.web)).loadUrl("javascript:" +
														"document.getElementsByName('sid')[0].value = '" + username + "';" +
														"document.getElementsByName('PIN')[0].value = '" + password + "';" +
														"document.forms[0].submit();");
											}
										});
									}
								});
							}
							
							handled = true;
						}
					}
					
					View web = findViewById(R.id.web),
						 list = findViewById(R.id.list);
					LinearLayout.LayoutParams wparam = (LayoutParams)web.getLayoutParams(),
											  lparam = (LayoutParams)list.getLayoutParams();
					if (handled)
					{
						wparam.weight = 0.2f;
						lparam.weight = 0.8f;
					}
					else
					{
						wparam.weight = 0.8f;
						lparam.weight = 0.2f;
					}
					web.setLayoutParams(wparam);
					list.setLayoutParams(lparam);
					
				} catch (URISyntaxException e) {
					// this never happens
				}
			}
		});
		
		web.setWebChromeClient(new WebChromeClient() {
			public boolean onConsoleMessage(ConsoleMessage cmsg)
			{
				String msg = cmsg.message();
				
				if (msg.startsWith("HERPDERP"))
				{
					String[] parts = msg.substring(8).split("DERPHERP");
					adapter.add(parts[0]);
					menu.put(parts[0], parts[1]);
					return true;
				}
				
				return false;
			}
		});
		
		web.loadUrl("https://myswat.swarthmore.edu");
    }
    
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	WebView web = (WebView)findViewById(R.id.web);
        if (keyCode == KeyEvent.KEYCODE_BACK && web.canGoBack())
        {
        	web.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    
    private void ask(String title, String prior, boolean password, final Callee ok)
    {
    	if (prior != "")
    	{
    		ok.call(prior);
    		return;
    	}
    	
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);

    	builder.setTitle(title);

    	// Set an EditText view to get user input 
    	final EditText input = new EditText(this);
    	if (password)
    	{
    		input.setTransformationMethod(new PasswordTransformationMethod());
    	}
    	builder.setView(input);
    	
    	builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int whichButton) {
    			ok.call(input.getText().toString());
    		}
    	});
    	
    	final AlertDialog alert = builder.create();
    	
    	input.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(View v, int keycode, KeyEvent evt) {
				if (evt.getAction() == KeyEvent.ACTION_DOWN
						&& (keycode == KeyEvent.KEYCODE_ENTER || keycode == KeyEvent.KEYCODE_TAB))
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
}

/* Instructions for operating MySwarthmore
	
	1. Request https://myswat.swarthmore.edu/pls/twbkwbis.P_WWWLogin to get the cookies
	2. POST https://myswat.swarthmore.edu/pls/twbkwbis.P_ValLogin to log in
			data: sid=username&PIN=password
	3. The first menu is at https://myswat.swarthmore.edu/pls/twbkwbis.P_GenMenu?name=bmenu.P_MainMnu&msg=WELCOME+
	4. To log out, request https://myswat.swarthmore.edu/pls/twbkwbis.P_Logout with referer set to the menu
*/