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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

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
		
		public static final int BOTTOM = 0;
		public static final int CENTER = 1;

		public SwatOverlay(Context c, Drawable defaultMarker, int bound) {
			super(bound == BOTTOM ? boundCenterBottom(defaultMarker) : boundCenter(defaultMarker));
			context = c;
			populate();
		}
		
		public void addOverlay(OverlayItem overlay) {
			overlays.add(overlay);
			populate();
		}
		
		public void addHiddenOverlay(OverlayItem overlay) {
			hidden_overlays.add(overlay);
		}
		
		public void clearOverlays() {
			overlays.clear();
			populate();
		}
		
		public void clearHiddenOverlays() {
			hidden_overlays.clear();
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
			ArrayList<OverlayItem> temp = new ArrayList<OverlayItem>(overlays);
			overlays = new ArrayList<OverlayItem>(hidden_overlays);
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
	private SwatOverlay campusmap, swatties;
	private Handler handle = new Handler();
	private Runnable swatties_runnable;
	private boolean gps_on = false;
	private boolean locations_on = false;
	private boolean swatties_on = false;
	private String marauder_id = "", marauder_name = "";
	
	private static final String MARAUDER_URL = "http://www.sccs.swarthmore.edu/users/12/aburka1/marauder/map.php";
	
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
							// TODO change Locations to Places (and in the prefs), switch up the order
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
			case R.id.marauderclear:
				getPreferences(Context.MODE_PRIVATE).edit()
					.remove("marauder_id")
					.remove("marauder_name")
					.commit();
				marauder_id = "";
				marauder_name = "";
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
        waldo = new MyLocationOverlay(this, map) {
        	@Override
        	public void onLocationChanged(Location location)
        	{
        		super.onLocationChanged(location);
        		
        		if (swatties_on) send_location(location);
        	}
        };
        map.getOverlays().add(waldo);
        
        // overlay campus map markers
        campusmap = new SwatOverlay(this, getResources().getDrawable(R.drawable.pin), SwatOverlay.BOTTOM);
        map.getOverlays().add(campusmap);
        
        // overlay swatties
        swatties = new SwatOverlay(this, getResources().getDrawable(R.drawable.footprint), SwatOverlay.CENTER);
        map.getOverlays().add(swatties);
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
    	if (on && !swatties_on)
    	{
    		if (marauder_name.equals(""))
    		{
    			waldo.disableMyLocation();
    			Utils.ask(this, "Enter your full name", "", false,
    					new Utils.Callee() {
		    				public void call(String str)
		    				{
		    					marauder_name = str;
		    					waldo.enableMyLocation();
		    					swatties(true);
		    				}
    					},
    					new Runnable () {
    						public void run()
    						{
    							Toast.makeText(Map.this, "Turn GPS off to show Swatties without sharing location", Toast.LENGTH_LONG).show();
    							waldo.enableMyLocation();
    						}
    					});
    			return;
    		}
    		
    		if (swatties_runnable == null)
    		{
    			swatties_runnable = new Runnable() {
					public void run()
					{
						try {
							List<String[]> people = new CSVReader(new StringReader(Utils.do_http(MARAUDER_URL))).readAll();
							swatties.swap();
							swatties.clearHiddenOverlays();
							for (String[] person : people)
							{
								swatties.addHiddenOverlay(
										new OverlayItem(
												new GeoPoint(
														Integer.parseInt(person[2]),
														Integer.parseInt(person[3])),
												person[1],
												person[1]));
							}
							swatties.swap();
						} catch (IOException e) {
							// it's a StringReader, it can't fail
							// I hate checked exceptions
							e.printStackTrace();
						}
						
						if (waldo.getLastFix() != null)
						{
							send_location(waldo.getLastFix());
						}
						
						handle.postDelayed(this, 5000);
					}
    			};
    		}
    		
    		handle.postDelayed(swatties_runnable, 100);
    		
    		swatties.swap();
    		map.invalidate();
    		swatties_on = true;
    	}
    	else if (!on && swatties_on)
    	{
    		if (swatties_runnable != null)
    		{
    			handle.removeCallbacks(swatties_runnable);
    		}
    		swatties.swap();
    		map.invalidate();
    		swatties_on = false;
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
			Toast.makeText(this, "Can't show location: no fix or GPS not enabled", Toast.LENGTH_SHORT).show();
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
	
	private void send_location(final Location fix)
	{
		if (fix == null)
		{
			Toast.makeText(this, "Can't share location: no fix or GPS not enabled", Toast.LENGTH_SHORT).show();
		}
		else
		{
			StringWriter writer = new StringWriter();
			new CSVWriter(writer, CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER)
				.writeNext(new String[]{
					marauder_name,
					Integer.toString((int)(fix.getLatitude()*1e6)),
					Integer.toString((int)(fix.getLongitude()*1e6)),
					Long.toString(System.currentTimeMillis()/1000)
			});
			String encoded_location = null;
			try {
				encoded_location = URLEncoder.encode(writer.toString().trim(), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			
			if (!marauder_id.equals(""))
			{
				Utils.do_http(MARAUDER_URL + "?i=" + marauder_id + "&l=" + encoded_location);
			}
			else
			{
				marauder_id = Utils.do_http(MARAUDER_URL + "?l=" + encoded_location).trim();
				Log.d("Map", "got marauder id: " + marauder_id);
			}
		}
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	
	@Override
	protected boolean isLocationDisplayed() {
		return gps_on;
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		
		SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
		marauder_id = prefs.getString("marauder_id", "");
		marauder_name = prefs.getString("marauder_name", "");
		Log.d("Map", "Loading marauder credentials " + marauder_id + "=" + marauder_name);
		
		if (waldo != null && gps_on)
		{
			gps_on = false;
			gps(true);
		}
		if (swatties_on)
		{
			swatties_on = false;
			swatties(true);
		}
	}
	
	@Override
	protected void onPause()
	{
		Log.d("Map", "onPause");
		
		if (gps_on)
		{
			gps(false);
			gps_on = true;
		}
		if (swatties_on)
		{
			swatties(false);
			swatties_on = true;
		}
		
		Log.d("Map", "Saving marauder credentials " + marauder_id + "=" + marauder_name);
		SharedPreferences.Editor edit = getPreferences(Context.MODE_PRIVATE).edit();
		if (!marauder_id.equals("")) edit.putString("marauder_id", marauder_id);
		if (!marauder_name.equals("")) edit.putString("marauder_name", marauder_name);
		edit.commit();
		
		super.onPause();
	}

}
