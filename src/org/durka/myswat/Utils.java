package org.durka.myswat;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.method.PasswordTransformationMethod;
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
}
