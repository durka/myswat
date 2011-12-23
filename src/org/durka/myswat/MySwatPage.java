package org.durka.myswat;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.net.http.SslError;
import android.widget.Toast;

public class MySwatPage extends Activity {
	private MySwat myswat;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	
	    Bundle joy = getIntent().getExtras();
	    myswat = (MySwat) joy.getSerializable("myswat");
		String name = joy.getString("name");
		String uri = joy.getString("uri");
		
		final Activity activity = this;
		WebView view = new WebView(this);
		setTitle(name);
		view.getSettings().setBuiltInZoomControls(true);
		view.getSettings().setJavaScriptEnabled(true);
		view.setWebViewClient(new WebViewClient() {
			public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
			     Toast.makeText(activity, "Oh no! " + description, Toast.LENGTH_SHORT).show();
			   }
			public void onReceivedSslError (WebView view, SslErrorHandler handler, SslError error) {
				 handler.proceed() ;
			   }
		});
		
		CookieSyncManager.createInstance(this);
		CookieManager monster = CookieManager.getInstance();
		Log.d("myswat", "session ID is " + myswat.getSessionID());
		monster.removeSessionCookie();
		Log.d("myswat", "sleeping for hack");
		SystemClock.sleep(1000);
		Log.d("myswat", "waking up");
		monster.setCookie("https://myswat.swarthmore.edu", "SESSID=" + myswat.getSessionID());
		CookieSyncManager.getInstance().sync();
		
		Log.d("myswat", "loading URI https://myswat.swarthmore.edu" + uri);
		setContentView(view);
	    view.loadUrl("https://myswat.swarthmore.edu" + uri);
	    
		Toast.makeText(this, "Loading " + name + "...", Toast.LENGTH_LONG).show();
	}

}
