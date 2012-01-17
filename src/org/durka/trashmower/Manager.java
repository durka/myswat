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
import android.widget.TextView;

import org.durka.trashmower.R;

public class Manager extends Activity {
	private Class<?>[] intents = {
			MySwat.class,
			MapLoading.class,
			Directory.class,
			Food.class,
			Preferences.class,
	};
	
    public class ImageAdapter extends BaseAdapter {
    	private Context context;
    	private Integer[] thumbs = {
    			R.drawable.menu,
    			R.drawable.map,
    			R.drawable.addressbook,
    			R.drawable.food,
    			R.drawable.settings,
    	};
    	private String[] labels = {
    			"MySwat",
    			"Map",
    			"Directory",
    			"Food",
    			"Settings",
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
	    public View getView(int position, View convert_view, ViewGroup parent)
	    {
	        if (convert_view == null) {  // if it's not recycled, initialize some attributes
	        	convert_view = getLayoutInflater().inflate(R.layout.manager_item, null);
	        	TextView txt = (TextView) convert_view.findViewById(R.id.manager_item_label);
	        	ImageView img = (ImageView) convert_view.findViewById(R.id.manager_item_image);
	        	
	            txt.setText(labels[position]);
	            img.setImageResource(thumbs[position]);
	        }

	        return convert_view;
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
