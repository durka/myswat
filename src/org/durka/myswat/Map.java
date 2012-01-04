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

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.OverlayItem;

public class Map extends MapActivity {
	
	private class SwatOverlay extends ItemizedOverlay<OverlayItem> {
		
		private ArrayList<OverlayItem> overlays = new ArrayList<OverlayItem>();

		public SwatOverlay(Drawable defaultMarker) {
			super(boundCenterBottom(defaultMarker));
		}
		
		public void addOverlay(OverlayItem overlay) {
			overlays.add(overlay);
			populate();
		}

		@Override
		protected OverlayItem createItem(int i) {
			return overlays.get(i);
		}

		@Override
		public int size() {
			return overlays.size();
		}

	}

	private MyLocationOverlay waldo; // current location represented by Where's Waldo? cute huh
	
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
        
        waldo = new MyLocationOverlay(this, map);
        waldo.enableMyLocation();
        waldo.enableCompass();
        map.getOverlays().add(waldo);
    }

	private void recenter() {
		map.getController().animateTo(new GeoPoint(39905065, -75354005));
        map.getController().zoomToSpan(4000, 4000);
	}
	
	private void find_waldo() {
		Location fix = waldo.getLastFix();
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

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		
		if (waldo != null)
		{
			waldo.enableMyLocation();
			waldo.enableCompass();
		}
	}
	
	@Override
	protected void onPause()
	{
		Log.d("Map", "onPause");
		
		waldo.disableCompass();
		waldo.disableMyLocation();
		
		super.onPause();
	}

}
