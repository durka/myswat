/*
 * Copyright 2012 Alex Burka
 * 
 * This file is part of Trashmower.
 * Trashmower is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Trashmower is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Trashmower.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.durka.trashmower;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import org.durka.trashmower.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MySwat extends Activity {
	
	public static final String ROOT = "https://myswat.swarthmore.edu";
	public static final String MAIN = "/pls/twbkwbis.P_GenMenu?name=bmenu.P_MainMnu&msg=WELCOME+";
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.myswat, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.myswat_main:
				((WebView) findViewById(R.id.web)).loadUrl(ROOT + MAIN);
				return true;
			case R.id.escape:
				this.finish();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	private LinkedHashMap<String, String> menu;	// the menu entries <label, href>
	private ArrayAdapter<String> adapter;		// the ListView adapter
	private URI currentURI;						// current URI of the WebView
	private int login_attempts;					// we try to avoid getting locked out
	private boolean login_page;
	private AlertDialog loading;
	
	private final static int LOGIN_ATTEMPT_LIMIT = 2;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu);
        
        login_attempts = 0;
        login_page = false;

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
				Log.d("MySwat", menu.get(name));
				web.loadUrl("javascript:" + menu.get(name));
			}
		});
		
		// populate the webview
		WebView web = (WebView)findViewById(R.id.web);
		web.getSettings().setBuiltInZoomControls(true);
		web.getSettings().setJavaScriptEnabled(true);
		
		// a cute little Loading dialog to throw up while the WebView is thinking
		final TextView percent = new TextView(this);
		percent.setGravity(Gravity.CENTER);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Loading...");
		builder.setView(percent);
		loading = builder.create();
		
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
				
				WebView web = (WebView) findViewById(R.id.web);
				ListView list = (ListView) findViewById(R.id.list);
				
				// kill loading dialog
				loading.hide();
				Log.d("MySwat", "Loaded " + address);
				
				if (login_page)
				{
					login_page = false;
					web.clearHistory();
				}
				
				// now let's find out whether we want to process this page
				// FIXME: testing the URI is not complete enough.
				//			since the login page can have any URI, need to look at the source
				try {
					currentURI = new URI(address);
					boolean handled = false;
					if (currentURI.getPath().contains("P_WWWLogin") || (view.getTitle() != null && view.getTitle().equals("User Login")))
					{ // it's the login screen! prompt for credentials
						
						// don't get locked out
						++login_attempts;
						login_page = true;
						Log.d("MySwat", "Login attempt #" + Integer.toString(login_attempts));
						if (login_attempts >= LOGIN_ATTEMPT_LIMIT)
						{
							Toast.makeText(activity, "ERROR: Too many login failures.", Toast.LENGTH_SHORT).show();
						}
						else
						{
							final SharedPreferences prefs = activity.getPreferences(Context.MODE_PRIVATE);
							final SharedPreferences.Editor e = prefs.edit();
							
							// the ask() function puts up an AlertDialog with an EditView
							// TODO: activity for user management
							Utils.ask(activity, "Username:", prefs.getString("username", ""), false, new Utils.Callee() {
								public void call(String str) {
									final String username = str;
									e.putString("username", username);
									
									Utils.ask(activity, "Password:", prefs.getString("password", ""), true, new Utils.Callee() {
										public void call(String str) {
											final String password = str;
											if (!password.equals(prefs.getString("password", "")))
											{
												e.putString("password", password);
												Utils.yesno(activity, "Remember password?",
														new Runnable() {
															public void run()
															{
																e.commit();
															}
														},
														new Runnable() {
															public void run()
															{
																// not saving the password
															}
														});
											}
											
											// inject some JavaScript to fill out and submit the login form
											((WebView)findViewById(R.id.web)).loadUrl("javascript:" +
													"document.getElementsByName('sid')[0].value = '" + username + "';" +
													"document.getElementsByName('PIN')[0].value = '" + password + "';" +
													"document.loginform.submit();");
										}
									});
								}
							});
							
							handled = true;
						}
					}
					else if (currentURI.getPath().contains("P_GenMenu") || (view.getTitle() != null && view.getTitle().equals("Registration")))
					{ // it's a menu! put it in the ListView
						
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
												"console.log('HERPDERP' + " +
													"tds[j].childNodes[1].innerText + " +
															"'DERPHERP' + " +
													"'window.location=\"' + tds[j].childNodes[1].href + '\"'" +
												");" +
											"}" +
										"}" +
									"}" +
								"}" +
								"})(document.getElementsByClassName('menuplaintable')[0].childNodes[1].childNodes)"); // feed in the table rows, which are in a <tbody> in the <table class="menuplaintable">
						handled = true;
					}
					else if (view.getTitle() != null && view.getTitle().equals("Select Term"))
					{
						// it is the Select Term dropdown page
						// let's get the options out
						// (by injecting JavaScript of course)
						view.loadUrl("javascript:(function(form_num, select_id){" +
								"var form = document.forms[form_num], select = document.getElementById(select_id);" +
								"for (var i = 0; i < select.options.length; ++i)" + // these are options of the dropdown holding the menu entries
								"{" +
									"console.log('HERPDERP' + " +
										"select.options[i].text + " +
												"'DERPHERP' + " +
										"'document.getElementById(\"' + select_id + '\").selectedIndex='+i+';" +
										" document.forms[' + form_num + '].submit()'" + // submit the form on ListView click
									");" +
								"}" +
								"})(1, 'term_id')"); // feed in the form and the options, which are in a <select id="term_id">
						
						handled = true;
					}
					else if (view.getTitle() != null && view.getTitle().equals("Student Detail Schedule"))
					{
						// it is the detailed schedule table
						// let's parse the classes out
						view.loadUrl("javascript:(function(tables){" +
								"var classes = [];" +
								"for (var i = 0; i < tables.length; ++i)" + // there is a pair of tables for each class
								"{" +
									"var klass = tables[i].caption.innerText.split(/ ?- ?/);" +
									"var name = klass[0];" +
									"if (i+1 <= tables.length && tables[i+1].summary.substr(0, 10) == 'This table')" +
									"{" +
										"if (tables[i+1].rows[1].cells[5].innerText == 'Lab')" +
										"{" +
											"classes[classes.length-1].time += ', lab ' + tables[i+1].rows[1].cells[2].innerText + ' ' + tables[i+1].rows[1].cells[1].innerText.split(\" - \")[0];" +
											"classes[classes.length-1].place += ', lab ' + tables[i+1].rows[1].cells[3].innerText;" +
										"}" +
										"else if (tables[i+1].rows[1].cells[5].innerText == 'Problem session')" +
										"{" +
											"classes[classes.length-1].time += ', prob sess ' + tables[i+1].rows[1].cells[2].innerText + ' ' + tables[i+1].rows[1].cells[1].innerText.split(\" - \")[0];" +
											"classes[classes.length-1].place += ', prob sess ' + tables[i+1].rows[1].cells[3].innerText;" +
										"}" +
										"else" +
										"{" +
											"classes[classes.length] = {" +
												"'name': name," +
												"'section': klass[1] + ' (' + klass[2] + ')'," +
												"'prof': tables[i+1].rows[1].cells[6].innerText," +
												"'time': tables[i+1].rows[1].cells[2].innerText + ' ' + tables[i+1].rows[1].cells[1].innerText.split(\" - \")[0]," +
												"'place': tables[i+1].rows[1].cells[3].innerText," +
												"'credit': Math.round(tables[i].rows[5].cells[1].innerText*10)/10 + ' credit(s), ' + tables[i].rows[4].cells[1].innerText," +
											"};" +
										"}" +
										"++i;" +
									"}" +
									"else" +
									"{" +
										"classes[classes.length] = {" +
											"'name': name," +
											"'section': klass[1] + ' (' + klass[2] + ')'," +
											"'prof': ''," +
											"'time': ''," +
											"'place': ''," +
											"'credit': Math.round(tables[i].rows[5].cells[1].innerText*10)/10 + ' credit(s), ' + tables[i].rows[4].cells[1].innerText" +
										"};" +
									"}" +
								"}" +
								"for (var i = 0; i < classes.length; ++i)" +
								"{" +
									"console.log('HERPDERP' + " +
										"classes[i].name + " +
												"'DERPHERP' + " +
										"'alert(\"' + [classes[i].section, classes[i].prof," +
													 " classes[i].time," +
													 " classes[i].place.replace(/Science Center/g, 'Sci')," +
													 " classes[i].credit].join('\\\\n')" +
											"+ '\")'" +
									");" +
								"}" +
								"})(document.getElementsByClassName('datadisplaytable'))");
						
						handled = true;
					}
					else if (view.getTitle() != null && view.getTitle().equals("Unoffical Grade Report")) // [sic]
					{
						// Grades at a Glance!
						
						view.loadUrl("javascript:(function(rows){" +
								"var semesters = [];" +
								"var cursem = null;" +
								"for (var i = 0; i < rows.length; ++i)" + // a row may be a semester colspan, a header row or a data row
								"{" +
									"if (rows[i].cells.length == 1)" +
									"{" + // it's a semester colspan
										"if (cursem != null) semesters[semesters.length] = cursem;" +
										"var text = rows[i].cells[0].innerText.split(/\\s+/);" +
										"cursem = {" +
											"'term': text[1] + ' ' + text[2]," +
											"'credits': parseInt(text[text.length-1])," +
											"'grades': []," +
										"};" +
									"}" +
									"else if (rows[i].onmouseover != null)" +
									"{" + // data rows have mouseover events
										"function T(j) { return rows[i].cells[j].innerText; }" +
										"cursem.grades[cursem.grades.length] = {" +
											"'name': T(1)," +
											"'detail': T(0)," +
											"'prof': T(5)," +
											"'credits': T(2)," +
											"'grade': T(3)," +
										"};" +
									"}" +
								"}" +
								"if (cursem != null) semesters[semesters.length] = cursem;" +
								"for (var i = 0; i < semesters.length; ++i)" +
								"{" +
									"var s = [];" +
									"for (var j = 0; j < semesters[i].grades.length; ++j)" +
									"{" +
										"s[s.length] = [semesters[i].grades[j].grade + ' in ' + semesters[i].grades[j].name, semesters[i].grades[j].detail, semesters[i].grades[j].prof, semesters[i].grades[j].credits + ' credit(s)'].filter(function (a) { return a != '-'; }).join('\\\\n\\\\t\\\\t');" +
									"}" +
									"console.log('HERPDERP' + " +
										"semesters[i].term + ' (' + semesters[i].credits + ' credits)' + " +
												"'DERPHERP' + " +
										"'alert(\"' + s.join('\\\\n') + '\")'" +
									");" +
								"}" +
								"})(document.getElementsByClassName('default2')[0].rows)");
						
						handled = true;
					}
					
					// if we processed the page, shrink the WebView and embiggen the ListView
					// otherwise, the other way around
					LinearLayout.LayoutParams wparam = (LayoutParams)web.getLayoutParams(),
											  lparam = (LayoutParams)list.getLayoutParams();
					if (handled)
					{
						wparam.weight = 0.0f;
						lparam.weight = 1.0f;
					}
					else
					{
						wparam.weight = 1.0f;
						lparam.weight = 0.0f;
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
			
			@Override
			public boolean onJsAlert(WebView view, String url, String msg, JsResult result)
			{
				AlertDialog.Builder builder = new AlertDialog.Builder(activity);
				
				TextView alert = new TextView(getApplicationContext());
				alert.setText(msg);
				alert.setTextSize(12);
				alert.setMovementMethod(new ScrollingMovementMethod());
				
				builder.setTitle(view.getTitle());
				builder.setView(alert);
				builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int button) {
						// nothing
					}
				});
				
				builder.show();
				
				result.confirm();
				return true; // we handled it
			}
			
			@Override
			public void onReceivedTitle(WebView view, String title)
			{
				activity.setTitle(getResources().getString(R.string.myswat_activity) + " (" + title + ")");
			}
			
			@Override
			public void onProgressChanged(WebView view, int progress)
			{
				percent.setText(Integer.toString(progress) + "%");
			}
		});
		
		// starting page
		web.loadUrl(getPreferences(Context.MODE_PRIVATE).getString("page", ROOT));
    }
    
    @Override
    public void onPause()
    {
    	// persist whatever page the user was on
    	// TODO: add a button so you can get out without going back to Main Menu
    	getPreferences(Context.MODE_PRIVATE).edit()
    		.putString("page",
    				   ((WebView) findViewById(R.id.web)).getUrl())
    		.commit();
    	
    	loading.dismiss();
    	super.onPause();
    }
    
    // override the back button to go back in the WebView, if applicable
    // TODO: this seems to need multiple presses sometimes?
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	WebView web = (WebView)findViewById(R.id.web);
        if (keyCode == KeyEvent.KEYCODE_BACK && web.canGoBack())
        {
        	web.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}

/* Instructions for operating MySwarthmore
	
	1. Request https://myswat.swarthmore.edu/pls/twbkwbis.P_WWWLogin to get the cookies
	2. POST https://myswat.swarthmore.edu/pls/twbkwbis.P_ValLogin to log in
			data: sid=username&PIN=password
	3. The first menu is at https://myswat.swarthmore.edu/pls/twbkwbis.P_GenMenu?name=bmenu.P_MainMnu&msg=WELCOME+
	4. To log out, request https://myswat.swarthmore.edu/pls/twbkwbis.P_Logout with referer set to the menu
*/