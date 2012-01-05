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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.OverlayItem;

public class Map extends MapActivity {
	
	/*
	 * An ItemizedOverlay for Swarthmore locations
	 * 
	 * The extra feature is a "hidden" overlay list that
	 * can be swapped with the real one at any time. It's
	 * like double buffering.
	 */
	private class SwatOverlay extends ItemizedOverlay<OverlayItem> {
		
		private ArrayList<OverlayItem> overlays = new ArrayList<OverlayItem>();
		private ArrayList<OverlayItem> hidden_overlays = new ArrayList<OverlayItem>();
		private Context context;

		public SwatOverlay(Context c, Drawable defaultMarker) {
			super(boundCenterBottom(defaultMarker));
			context = c;
		}
		
		public void addOverlay(OverlayItem overlay) {
			overlays.add(overlay);
			populate();
		}
		
		public void addHiddenOverlay(OverlayItem overlay) {
			hidden_overlays.add(overlay);
		}

		@Override
		protected OverlayItem createItem(int i) {
			return overlays.get(i);
		}

		@Override
		public int size() {
			return overlays.size();
		}
		
		public int hidden_size() {
			return hidden_overlays.size();
		}
		
		public void swap()
		{
			ArrayList<OverlayItem> temp = overlays;
			overlays = hidden_overlays;
			hidden_overlays = temp;
			populate();
		}

		@Override
		protected boolean onTap(int i)
		{
			Toast.makeText(context, overlays.get(i).getSnippet(), Toast.LENGTH_SHORT).show();
			return true;
		}
	}

	private MyLocationOverlay waldo; // current location represented by Where's Waldo? cute huh
	private SwatOverlay campusmap, marauders;
	private boolean gps_on = false;
	private boolean locations_on = false;
	private boolean swatties_on = false;
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.map, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.escape:
				this.finish();
				return true;
			case R.id.map_main:
				recenter();
				return true;
			case R.id.whereami:
				find_waldo();
				return true;
			case R.id.layers:
				final boolean[] on = new boolean[]{gps_on, locations_on, swatties_on};
				AlertDialog.Builder builder = new AlertDialog.Builder(this)
					.setTitle("Layers")
					.setCancelable(true)
					.setMultiChoiceItems(
							new String[]{"GPS", "Locations", "Swatties"},
							new boolean[]{gps_on, locations_on, swatties_on},
							new DialogInterface.OnMultiChoiceClickListener() {
								public void onClick(DialogInterface dialog,
										int which, boolean isChecked)
								{
									on[which] = isChecked;
								}
							})
					.setPositiveButton("Done", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which)
						{
							gps(on[0]);
							locations(on[1]);
							swatties(on[2]);
						}
					});
				builder.show();
				return true;
			case R.id.satellite:
				map.setSatellite(!map.isSatellite());
				item.setTitle(item.getTitle().equals("Satellite") ? "Streets" : "Satellite");
				return true;
			case R.id.googlemaps:
				GeoPoint center = map.getMapCenter();
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:" + (center.getLatitudeE6()/1e6) + "," + (center.getLongitudeE6()/1e6) + "?z=17")));
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	private MapView map;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.d("Map", "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map);
        
        map = (MapView) findViewById(R.id.map);
        map.setBuiltInZoomControls(true);
        
        map.setSatellite(true);
        recenter();
        
        // overlay current location
        waldo = new MyLocationOverlay(this, map);
        map.getOverlays().add(waldo);
        
        // overlay campus map markers
        campusmap = new SwatOverlay(this, getResources().getDrawable(R.drawable.pin));
        map.getOverlays().add(campusmap);
    }
    
    private void gps(boolean on)
    {
    	if (on && !gps_on)
    	{
    		waldo.enableMyLocation();
            waldo.enableCompass();
            gps_on = true;
    	}
    	else if (!on && gps_on)
    	{
    		waldo.disableCompass();
    		waldo.disableMyLocation();
    		gps_on = false;
    	}
    }
    
    private void locations(boolean on)
    {
    	if (on && !locations_on)
    	{
    		if (campusmap.hidden_size() == 0)
    		{
    			Resources res = getResources();
    	        String[] shorts = res.getStringArray(R.array.shorts),
    	        		 names = res.getStringArray(R.array.names),
    	        		 latitudes = res.getStringArray(R.array.latitudes),
    	        		 longitudes = res.getStringArray(R.array.longitudes),
    	        		 descriptions = res.getStringArray(R.array.descriptions);
    	        for (int i = 0; i < shorts.length; ++i)
    	        {
    	        	campusmap.addHiddenOverlay(
    	        			new OverlayItem(
    	        					new GeoPoint(
    	        							(int)(Double.parseDouble(latitudes[i])*1e6),
    	        							(int)(Double.parseDouble(longitudes[i])*1e6)),
    	        					shorts[i],
    	        					names[i]));
    	        }
    		}
    		
    		campusmap.swap();
    		
    		map.invalidate();
    		locations_on = true;
    	}
    	else if (!on && locations_on)
    	{
    		campusmap.swap();
    		map.invalidate();
    		locations_on = false;
    	}
    }
    
    private void swatties(boolean on)
    {
    	if (on)
    	{
    		throw new RuntimeException("privacy");
    	}
    }

	private void recenter() {
		map.getController().animateTo(new GeoPoint(39905065, -75354005));
        map.getController().zoomToSpan(4000, 4000);
	}
	
	private void find_waldo() {
		Location fix = waldo.getLastFix();
		if (fix == null)
		{
			Toast.makeText(this, "No fix or GPS not enabled", Toast.LENGTH_SHORT).show();
		}
		else
		{
			long ago = System.currentTimeMillis() - fix.getTime();
			String description = "This fix is from " + fix.getProvider() + ", ";
			if (ago > 30*60*1000)
			{
				description = null;
			}
			else if (ago > 2*60*1000)
			{
				description += Long.toString(ago/1000/60) + " minutes ago";
			}
			else
			{
				description += Long.toString(ago/1000) + " seconds ago";
			}
			
			if (description != null)
			{
				map.getController().animateTo(waldo.getMyLocation());
				
				Toast.makeText(this, description, Toast.LENGTH_SHORT).show();
			}
			else
			{
				Toast.makeText(this, "No location fix in the last half hour", Toast.LENGTH_SHORT).show();
			}
		}
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		
		if (waldo != null && gps_on)
		{
			gps_on = false;
			gps(true);
		}
	}
	
	@Override
	protected void onPause()
	{
		Log.d("Map", "onPause");
		
		gps(false);
		
		super.onPause();
	}

}
