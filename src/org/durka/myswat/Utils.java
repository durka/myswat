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

package org.durka.myswat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.EditText;

public class Utils {
	
	/*
	 * Like Runnable, but has a String parameter
	 * used with ask()
	 */
	public static abstract class Callee
	{
		public abstract void call(String str);
	}
	
    // asynchronously get a string from the user, call callback when it's ready
    // parameters:
    //		title: dialog title
    //		prior: if prior is not "", the dialog is not created and the callback is called immediately as if the user had typed in prior
    //		password: if so, the letters in the dialog are obscured
    //		ok: Callee instance with the callback
    public static void ask(Context c, String title, String prior, boolean password, final Callee ok)
    {
    	// if prior is not empty, skip the dialog
    	// the point of this is to allow use of an anonymous class for the Callee when it is not known whether ask() will block
    	//		(no code that needs the answer can be placed after ask() returns, because ask() returns immediately but the callback happens later)
    	if (prior != "")
    	{
    		ok.call(prior);
    		return;
    	}
    	
    	AlertDialog.Builder builder = new AlertDialog.Builder(c);

    	builder.setTitle(title);

    	// Set an EditText view to get user input 
    	final EditText input = new EditText(c);
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
    
    public static void yesno(Context c, String question, final Runnable yes, final Runnable no)
    {
    	AlertDialog.Builder builder = new AlertDialog.Builder(c);
    	
    	builder.setMessage(question);
    	
    	builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int whichButton) {
    			yes.run();
    		}
    	});
    	
    	builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int whichButton) {
    			no.run();
    		}
    	});
    	
    	builder.show();
    }

    public static String do_http(String uri) { return do_http(uri, "GET", null); }
    public static String do_http(String uri, String method) { return do_http(uri, method, null); }
    public static String do_http(String uri, String method, String referer)
	{
    	Log.d("MySwat", method + " " + uri);
		try
		{
			HttpUriRequest request = null;
			if (method == "GET")
			{
				request = new HttpGet(uri);
			}
			else if (method == "POST")
			{
				request = new HttpPost(uri);
			}

			if (referer != null)
			{
				request.addHeader("Referer", referer);
			}

			BufferedReader in = new BufferedReader(
									new InputStreamReader(
											new DefaultHttpClient()
												.execute(request)
													.getEntity()
														.getContent()));
			StringBuffer sb = new StringBuffer("");
			String line = "", NL = System.getProperty("line.separator");
            while ((line = in.readLine()) != null)
            {
                sb.append(line + NL);
            }
            in.close();
            
            return sb.toString();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return "E: " + e.getMessage();
		}
	}
}
