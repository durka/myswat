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
	
	/* BEGIN delegate to MySwatActivity */
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.common_menu, menu);
		menu.add("Recenter");
		menu.add("Google Maps");
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (item.getTitle().equals("Escape"))
		{
			this.finish();
			return true;
		}
		else if (item.getTitle().equals("Recenter"))
		{
			recenter();
			return true;
		}
		else if (item.getTitle().equals("Google Maps"))
		{
			GeoPoint center = map.getMapCenter();
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:" + (center.getLatitudeE6()/1e6) + "," + (center.getLongitudeE6()/1e6) + "?z=17")));
			return true;
		}
		
		
		return super.onOptionsItemSelected(item);
	}
	/* END delegate to MySwatActivity */
	
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
