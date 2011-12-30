package org.durka.myswat;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
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
	private LinkedHashMap<String, String> menu;	// the menu entries <label, href>
	private ArrayAdapter<String> adapter;		// the ListView adapter
	private URI currentURI;						// current URI of the WebView
	private String username, password;			// credentials most recently used to log in
	private int login_attempts;					// we try to avoid getting locked out
	private boolean linking_mode;				// what happens on ListView click?
												//	 true: loadUrl (value is the href)
												//   false: javascript (value is the JS to inject)
	
	private final static int LOGIN_ATTEMPT_LIMIT = 2;
	
	/*
	 * Like Runnable, but has a String parameter
	 * used with ask()
	 */
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

		final Activity activity = this; // final var for inner classes
		
		menu = new LinkedHashMap<String, String>();
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
		ListView list = (ListView)findViewById(R.id.list);
		list.setAdapter(adapter);
		list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			// when a list item is clicked, send the WebView to its href
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				String name = (String)adapter.getItem(position);
				
				WebView web = (WebView)findViewById(R.id.web);
				web.loadUrl("javascript:" + menu.get(name));
			}
		});
		
		// populate the webview
		WebView web = (WebView)findViewById(R.id.web);
		web.getSettings().setBuiltInZoomControls(true);
		web.getSettings().setJavaScriptEnabled(true);
		
		// a cute little Loading dialog to throw up while the WebView is thinking
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Loading...");
		final AlertDialog loading = builder.create();
		
		// override some of the WebView default behavior
		web.setWebViewClient(new WebViewClient() {
			// don't crash on an error
			@Override
			public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
			     Toast.makeText(activity, "Oh no! " + description, Toast.LENGTH_SHORT).show();
			   }
			
			// ignore SSL errors, because Android doesn't trust Swarthmore's
			// FIXME: this is bad! add the real certificate somehow
			@Override
			public void onReceivedSslError (WebView view, SslErrorHandler handler, SslError error) {
				 handler.proceed();
			   }
			
			// when the WebView begins to render a page
			@Override
			public void onPageStarted(WebView view, String address, Bitmap favicon)
			{
				// clear out the ListView
				adapter.clear();
				menu.clear();
				
				// throw up loading dialog
				loading.show();
				Log.d("MySwat", "Loading " + address);
			}
			
			// when the WebView finishes rendering a page
			@Override
			public void onPageFinished(WebView view, String address)
			{
				// kill loading dialog
				loading.hide();
				Log.d("MySwat", "Loaded " + address);
				
				// now let's find out whether we want to process this page
				// FIXME: testing the URI is not complete enough.
				//			since the login page can have any URI, need to look at the source
				try {
					currentURI = new URI(address);
					boolean handled = false;
					if (currentURI.getPath().contains("P_GenMenu"))
					{ // it's a menu! put it in the ListView
						login_attempts = 0; // it isn't the login page
						
						// amazingly, the only way to get the source out is to inject JavaScript
						// we intercept console.log in order to get the data (see setWebChromeClient)
						// previously, the JavaScript sent out the entire source code and it was parsed by
						//		the XOM libraries. This was slow, especially the first time. Now, it takes
						//		advantage of the fact that WebKit already did the parsing and we can pull
						//		the table entries right out of the DOM.
						view.loadUrl("javascript:(function(rows){" +
								"for (var i = 0; i < rows.length; ++i)" + // these are rows of the table holding the menu entries
								"{" +
									"if (rows[i].nodeName == 'TR')" + // is this a row?
									"{" +
										"var tds = rows[i].childNodes;" +
										"for (var j = 0; j < tds.length; ++j)" + // go through the row cells
										"{" +
											"if (tds[j].childNodes.length > 1)" + // is this a non-blank cell?
											"{" + // okay, grab the link info!
												  // we use console.log with a special prefix so that our custom WebChromeClient will catch it
												"console.log('HERPDERP' + tds[j].childNodes[1].innerHTML + 'DERPHERP' + 'window.location=\"' + tds[j].childNodes[1].href + '\"');" +
											"}" +
										"}" +
									"}" +
								"}" +
								"})(document.getElementsByClassName('menuplaintable')[0].childNodes[1].childNodes)"); // feed in the table rows, which are in a <tbody> in the <table class="menuplaintable">
						handled = true;
					}
					else if (currentURI.getPath().contains("P_WWWLogin"))
					{ // it's the login screen! prompt for credentials
						
						// don't get locked out
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
								// the ask() function puts up an AlertDialog with an EditView
								ask("Username:", username, false, new Callee() {
									public void call(String str) {
										username = str;
										ask("Password:", password, true, new Callee() {
											public void call(String str) {
												password = str;
												
												// inject some JavaScript to fill out and submit the login form
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
					else if (currentURI.getPath().contains("P_CrseSchdDetl"))
					{
						if (view.getTitle().equals("Select Term"))
						{
							// it is the Select Term dropdown page
							// let's get the options out
							// (by injecting JavaScript of course)
							view.loadUrl("javascript:(function(form, select){" +
									"for (var i = 0; i < select.options.length; ++i)" + // these are options of the dropdown holding the menu entries
									"{" +
										"console.log('HERPDERP' + select.options[i].text + 'DERPHERP' + 'document.getElementById(\"term_id\").selectedIndex='+i+'; document.forms[1].submit()');" + // submit the form on ListView click
									"}" +
									"})(document.forms[1], document.getElementById('term_id'))"); // feed in the form and the options, which are in a <select id="term_id">
							
							handled = true;
						}
						else if (view.getTitle().equals("Student Detail Schedule"))
						{
							// TODO
							
							handled = false;
						}
					}
					
					// if we processed the page, shrink the WebView and embiggen the ListView
					// otherwise, the other way around
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
		
		// we need to override some of the web chrome to catch console.log
		web.setWebChromeClient(new WebChromeClient() {
			@Override
			public boolean onConsoleMessage(ConsoleMessage cmsg)
			{
				String msg = cmsg.message();
				
				// if the console.log starts with our secret prefix, then it must be a command
				if (msg.startsWith("HERPDERP"))
				{ // this prefix denotes a menu entry
					String[] parts = msg.substring(8).split("DERPHERP");
					adapter.add(parts[0]);
					menu.put(parts[0], parts[1]);
					return true;
				}
				
				return false;
			}
		});
		
		// starting page
		web.loadUrl("https://myswat.swarthmore.edu");
    }
    
    // override the back button to go back in the WebView, if applicable
    // TODO: this seems to need multiple presses sometimes?
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	WebView web = (WebView)findViewById(R.id.web);
        if (keyCode == KeyEvent.KEYCODE_BACK && web.canGoBack())
        {
        	web.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    
    // asynchronously get a string from the user, call callback when it's ready
    // parameters:
    //		title: dialog title
    //		prior: if prior is not "", the dialog is not created and the callback is called immediately as if the user had typed in prior
    //		password: if so, the letters in the dialog are obscured
    //		ok: Callee instance with the callback
    private void ask(String title, String prior, boolean password, final Callee ok)
    {
    	// if prior is not empty, skip the dialog
    	// the point of this is to allow use of an anonymous class for the Callee when it is not known whether ask() will block
    	//		(no code that needs the answer can be placed after ask() returns, because ask() returns immediately but the callback happens later)
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
    	
    	final AlertDialog alert = builder.create(); // build it now so we can call dismiss() in onKey
    	
    	// the enter and tab keys are like clicking OK
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

    	alert.show(); // this does not block!
	}
}

/* Instructions for operating MySwarthmore
	
	1. Request https://myswat.swarthmore.edu/pls/twbkwbis.P_WWWLogin to get the cookies
	2. POST https://myswat.swarthmore.edu/pls/twbkwbis.P_ValLogin to log in
			data: sid=username&PIN=password
	3. The first menu is at https://myswat.swarthmore.edu/pls/twbkwbis.P_GenMenu?name=bmenu.P_MainMnu&msg=WELCOME+
	4. To log out, request https://myswat.swarthmore.edu/pls/twbkwbis.P_Logout with referer set to the menu
*/