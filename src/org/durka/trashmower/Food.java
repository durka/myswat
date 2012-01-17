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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.SortedMap;

import com.google.android.maps.GeoPoint;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

public class Food extends Activity {
	
	private abstract class FoodEstablishment
	{
		public GeoPoint location;
		public boolean open;
		public String menu;
		
		public abstract boolean update();
	}
	
	private class FoodELA extends BaseExpandableListAdapter
	{
		private Context context;
		private ArrayList<String> labels;
		private ArrayList<FoodEstablishment> foods;
		private LinkedHashMap<String, FoodEstablishment> map;
		
		public FoodELA(Context c, LinkedHashMap<String, FoodEstablishment> m)
		{
			context = c;
			map = m;
			reload();
		}
		
		public void reload()
		{
			labels = new ArrayList<String>();
			foods = new ArrayList<FoodEstablishment>();

			for (Entry<String, FoodEstablishment> entry : map.entrySet())
			{
				labels.add(entry.getKey());
				foods.add(entry.getValue());
			}
		}

		public Object getChild(int groupPosition, int childPosition) {
			return foods.get(groupPosition);
		}

		public Object getGroup(int groupPosition) {
			return labels.get(groupPosition);
		}

		public int getChildrenCount(int groupPosition) {
			return 1;
		}

		public int getGroupCount() {
			return labels.size();
		}

		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		public boolean hasStableIds() {
			return true;
		}

		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}
		
		private TextView getGenericView() {
	        // Layout parameters for the ExpandableListView
	        AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
	            ViewGroup.LayoutParams.FILL_PARENT, 64);

	        TextView textView = new TextView(context);
	        textView.setLayoutParams(lp);
	        // Center the text vertically
	        textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
	        // Set the text starting position
	        textView.setPadding(36, 0, 0, 0);
	        return textView;
	    }
		
		public View getChildView(int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {
			TextView tv = getGenericView();
			tv.setText(((FoodEstablishment) getChild(groupPosition, childPosition)).menu);
			return tv;
		}

		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {
			TextView tv = getGenericView();
			tv.setText(getGroup(groupPosition).toString());
			return tv;
		}

	}
	
	private ExpandableListView list;
	private FoodELA adapter;
	private LinkedHashMap<String, FoodEstablishment> establishments;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.food);
        
        list = (ExpandableListView) findViewById(R.id.food_list);
        establishments = new LinkedHashMap<String, FoodEstablishment>();
        adapter = new FoodELA(this, establishments);
        list.setAdapter(adapter);
        
        // populate the establishments
        establishments.put("Sharples", new FoodEstablishment() {
			@Override
			public boolean update()
			{
				open = true;
				menu = "some food and stuff";
				return true;
			}
        });
        adapter.reload();
    }
    
    @Override
    protected void onResume()
    {
    	super.onResume();
    	
    	for (FoodEstablishment fe : establishments.values())
    	{
    		fe.update();
    	}
    	adapter.reload();
    }

}
