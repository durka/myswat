package org.durka.myswat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;

public class Map extends MapActivity {
	
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
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map);
        
        map = (MapView) findViewById(R.id.map);
        map.setBuiltInZoomControls(true);
        
        map.setSatellite(true);
        recenter();
    }

	private void recenter() {
		map.getController().animateTo(new GeoPoint(39905065, -75354005));
        map.getController().zoomToSpan(4000, 4000);
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

}
