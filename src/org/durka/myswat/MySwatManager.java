package org.durka.myswat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

public class MySwatManager extends Activity {
	private Class<?>[] intents = {
			MySwatMenu.class,
			MapLoading.class,
			Directory.class,
	};
	
    public class ImageAdapter extends BaseAdapter {
    	private Context context;
    	private Integer[] thumbs = {
    			R.drawable.menu,
    			R.drawable.map,
    			R.drawable.addressbook,
    	};

		public ImageAdapter(Context c) {
			context = c;
		}

		public int getCount() {
			return thumbs.length;
		}

		public Object getItem(int position) {
			return null;
		}

		public long getItemId(int position) {
			return 0;
		}

		// create a new ImageView for each item referenced by the Adapter
	    public View getView(int position, View convert_view, ViewGroup parent) {
	        ImageView image;
	        if (convert_view == null) {  // if it's not recycled, initialize some attributes
	            image = new ImageView(context);
	            image.setLayoutParams(new GridView.LayoutParams(115, 115));
	            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
	            image.setPadding(8, 8, 8, 8);
	        } else {
	            image = (ImageView) convert_view;
	        }

	        image.setImageResource(thumbs[position]);
	        return image;
	    }

	}

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manager);
        
        GridView grid = (GridView)findViewById(R.id.grid);
        grid.setAdapter(new ImageAdapter(this));
        
        final Class<?>[] f_intents = intents;
        final Activity activity = this;
        grid.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
            	if (f_intents[position] != null)
            	{
            		activity.startActivity(new Intent(activity, f_intents[position]));
            	}
            }
        });
    }
}
