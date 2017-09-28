package geotalent.workshoparcgis;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.android.map.FeatureLayer;
import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.LocationDisplayManager;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.android.map.event.OnSingleTapListener;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.map.Feature;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.PictureMarkerSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.tasks.na.NAFeaturesAsFeature;
import com.esri.core.tasks.na.Route;
import com.esri.core.tasks.na.RouteDirection;
import com.esri.core.tasks.na.RouteParameters;
import com.esri.core.tasks.na.RouteResult;
import com.esri.core.tasks.na.RouteTask;
import com.esri.core.tasks.na.StopGraphic;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;

import static android.graphics.Color.*;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private MapView mapView = null;
    private String mapService = "http://services.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Light_Gray_Base/MapServer ";
    final private int REQUEST_CODE_ASK_PERMISSION = 123;
    private GraphicsLayer graphicsLayer = new GraphicsLayer();
    private GraphicsLayer graphicsLayer2 = new GraphicsLayer();
    private Graphic pg;
    private PictureMarkerSymbol pinPic = null;
    private Point pinStart, pinFinish;
    private Button start, finish;
    private Boolean lmain,lroute;
    private TextView btmNav,btmMile;
    private ImageView btmImg;
    private String mapUrl = "http://sampleserver3.arcgisonline.com/ArcGIS/rest/services/Network/USA/NAServer/Route";

    public static Point mLocation = null;
    final SpatialReference wm = SpatialReference.create(102100);
    final SpatialReference egs = SpatialReference.create(4326);

    /**Route Activity**/
    private FloatingActionButton fab;
    private SimpleMarkerSymbol pinSymbol = new SimpleMarkerSymbol(Color.RED, 10, SimpleMarkerSymbol.STYLE.CIRCLE);
    private SimpleMarkerSymbol pinSymbolselect = new SimpleMarkerSymbol(Color.GREEN, 10, SimpleMarkerSymbol.STYLE.CIRCLE);
    private ProgressDialog dialog;
    private RouteResult mResults = null;
    private RouteTask mRouteTask = null;
    private Exception mException = null;
    private Route curRoute = null;
    final Handler mHandler = new Handler();
    final Runnable mUpdateResults = new Runnable() {
        public void run() {
            updateUI();
        }
    };
    private SimpleLineSymbol segmentHider = null;
    private int selectedSegmentID = -1;
    private String routeSummary = null;
    private GraphicsLayer routeLayer = new GraphicsLayer();
    private GraphicsLayer hiddenSegmentsLayer = new GraphicsLayer();
    private BottomSheetBehavior bottomSheetBehavior;
    /**Route Activity**/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        locationPermission();
        lmain = true;
        lroute = false;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.routePin_start:
                pinPic = new PictureMarkerSymbol(getResources().getDrawable(R.drawable.ic_pin_drop_black_24dp));
                mapView.setOnSingleTapListener(new OnSingleTapListener() {
                    @Override
                    public void onSingleTap(float v, float v1) {
                        graphicsLayer.removeAll();
                        pinStart = mapView.toMapPoint(v, v1);
                        pg = new Graphic(pinStart, pinPic);
                        graphicsLayer.addGraphic(pg);
                        mapView.addLayer(graphicsLayer);
                        pinStart = convert(pinStart);
                        start.setText(String.format("Latitude: %.4f, Longitude: %.4f", pinStart.getY(), pinStart.getX()));
                    }
                });
                break;
            case R.id.routePin_finish:
                pinPic = new PictureMarkerSymbol(getResources().getDrawable(R.drawable.ic_pin_drop_red_24dp));
                mapView.setOnSingleTapListener(new OnSingleTapListener() {
                    @Override
                    public void onSingleTap(float v, float v1) {
                        graphicsLayer2.removeAll();
                        pinFinish = mapView.toMapPoint(v, v1);
                        pg = new Graphic(pinFinish, pinPic);
                        graphicsLayer2.addGraphic(pg);
                        mapView.addLayer(graphicsLayer2);
                        pinFinish = convert(pinFinish);
                        finish.setText(String.format("Latitude: %.4f, Longitude: %.4f", pinFinish.getY(), pinFinish.getX()));
                    }
                });
                break;
            case R.id.fab:
                try {
                    mRouteTask = RouteTask.createOnlineRouteTask(mapUrl, null);
                    QueryDirections(pinStart, pinFinish);
                    mapView.setOnSingleTapListener(new OnSingleTapListener() {
                        @Override
                        public void onSingleTap(float x, float y) {
                            int[] indexes = hiddenSegmentsLayer.getGraphicIDs(x, y, 20);
                            hiddenSegmentsLayer.updateGraphic(selectedSegmentID,pinSymbol);
                            if (indexes.length < 1) {
                                return;
                            }
                            selectedSegmentID = indexes[0];
                            Graphic selected = hiddenSegmentsLayer.getGraphic(selectedSegmentID);
                            hiddenSegmentsLayer.updateGraphic(selectedSegmentID,pinSymbolselect);
                            String direction = ((String) selected.getAttributeValue("text"));
                            double length = ((Double) selected.getAttributeValue("length")).doubleValue();
                            btmImg.setImageResource(imgRoute(direction));
                            btmNav.setText(direction);
                            btmMile.setText(String.format("%.2f Miles",length));
                            bottomSheetBehavior.setPeekHeight(200);
                            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                            mapView.setExtent(selected.getGeometry(), 50);
                        }
                    });
                } catch (Exception e) {
                    mException = e;
                }
                break;
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        mapView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.unpause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("map1Active",true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (lmain == true){
            getMenuInflater().inflate(R.menu.menu_main,menu);
        }else if (lroute == true){
            getMenuInflater().inflate(R.menu.menu_route,menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_layer){
            return true;
        }else if(id == R.id.menu_route){ //go to route
            routeActivity();
            switchLayer(2);
            return true;
        }else if(id == R.id.menu_delete){ //back to main
            mainActivity();
            switchLayer(1);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void switchLayer(int layer){
        switch (layer){
            case 1:
                lmain = true;
                lroute = false;
                break;
            case 2:
                lmain = false;
                lroute = true;
                break;
        }
    }

    private void mainActivity() {
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setMap();
        LocationDisplayManager ldm = mapView.getLocationDisplayManager();
        ldm.setLocationListener(new MyLocationListener());
        ldm.start();
        ldm.setAutoPanMode(LocationDisplayManager.AutoPanMode.OFF);
    }


    private class MyLocationListener implements LocationListener {

        public MyLocationListener() {
            super();
        }

        public void onLocationChanged(Location loc) {
            if (loc == null)
                return;
            boolean zoomToMe = (mLocation == null) ? true : false;
            mLocation = new Point(loc.getLongitude(), loc.getLatitude());
            if (zoomToMe) {
                Point p = (Point) GeometryEngine.project(mLocation, egs, wm);
                mapView.zoomToResolution(p, 3.0);
            }
        }

        public void onProviderDisabled(String provider) {
            Toast.makeText(getApplicationContext(), "GPS Disabled",
                    Toast.LENGTH_SHORT).show();
        }

        public void onProviderEnabled(String provider) {
            Toast.makeText(getApplicationContext(), "GPS Enabled",
                    Toast.LENGTH_SHORT).show();
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    }

    private Point convert(Point point) {
        SpatialReference sp = SpatialReference.create(SpatialReference.WKID_WGS84);
        Point aux = (Point) GeometryEngine.project(point, mapView.getSpatialReference(), sp);
        return aux;
    }

    private void setMap() {
        mapView = (MapView) findViewById(R.id.map);
        mapView.addLayer(new ArcGISTiledMapServiceLayer(mapService));
        mapView.enableWrapAround(true);
    }

    private void locationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            int lo = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            if (lo != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION
                }, REQUEST_CODE_ASK_PERMISSION);
                return;
            }
        }
        mainActivity();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermission();
                } else {
                    Toast.makeText(MainActivity.this, "Location Denied", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void routeActivity() {
        setContentView(R.layout.activity_route);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setMap();
        start = (Button) findViewById(R.id.routePin_start);
        start.setOnClickListener(this);
        finish = (Button) findViewById(R.id.routePin_finish);
        finish.setOnClickListener(this);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
        fab.setColorFilter(WHITE);
        fab.setOnClickListener(this);
        View llBottomSheet = findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(llBottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        btmNav = (TextView) findViewById(R.id.btmSheet_nav);
        btmMile = (TextView) findViewById(R.id.btmSheet_mile);
        btmImg = (ImageView) findViewById(R.id.btmSheet_img);
    }

    private void QueryDirections(final Point start, final Point end) {
        dialog = ProgressDialog.show(MainActivity.this, "Routing Sample",
                "Calculating route...", true);
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    RouteParameters rp = mRouteTask.retrieveDefaultRouteTaskParameters();
                    NAFeaturesAsFeature rfaf = new NAFeaturesAsFeature();
                    StopGraphic point1 = new StopGraphic(start);
                    StopGraphic point2 = new StopGraphic(end);
                    rfaf.addFeatures(new Graphic[]{point1, point2});
                    rfaf.setCompressedRequest(true);
                    rp.setStops(rfaf);
                    rp.setOutSpatialReference(wm);
                    mResults = mRouteTask.solve(rp);
                    mHandler.post(mUpdateResults);
                } catch (Exception e) {
                    mException = e;
                    mHandler.post(mUpdateResults);
                }
            }
        };
        t.start();
    }

    void updateUI() {
        dialog.dismiss();
        ArrayList<String> curDirections = new ArrayList<>();
        if (mResults == null) {
            Toast.makeText(MainActivity.this, mException.toString(), Toast.LENGTH_LONG).show();
            return;
        }
        curRoute = mResults.getRoutes().get(0);
        segmentHider = new SimpleLineSymbol(Color.BLUE, 5);
        mapView.addLayer(routeLayer);
        mapView.addLayer(hiddenSegmentsLayer);
        routeLayer.removeAll();
        hiddenSegmentsLayer.removeAll();
        for (RouteDirection rd : curRoute.getRoutingDirections()) {
            HashMap<String, Object> attribs = new HashMap<String, Object>();
            attribs.put("text", rd.getText());
            attribs.put("time", Double.valueOf(rd.getMinutes()));
            attribs.put("length", Double.valueOf(rd.getLength()));
            curDirections.add(String.format("%s%n%.1f minutes (%.1f miles)",
                    rd.getText(), rd.getMinutes(), rd.getLength()));
            Graphic pinGraphic = new Graphic(((Polyline) rd.getGeometry()).getPoint(((Polyline) rd.getGeometry()).getPointCount() - 1), pinSymbol, attribs);
            Graphic routeGraphic = new Graphic(rd.getGeometry(), segmentHider, attribs);
            routeLayer.addGraphic(routeGraphic);
            hiddenSegmentsLayer.addGraphic(pinGraphic);
        }
        selectedSegmentID = -1;
        routeSummary = String.format("%s%n%.1f minutes (%.1f miles)",
                curRoute.getRouteName(), curRoute.getTotalMinutes(),
                curRoute.getTotalMiles());
        mapView.setExtent(curRoute.getEnvelope(), 10);

        curDirections.remove(0);
        curDirections.add(0, "My Location");

        curDirections.remove(curDirections.size() - 1);
        curDirections.add("Destination");
    }

    private int imgRoute(String txt){
        if (txt.contains("Go") || txt.contains("Continue")||txt.contains("straight")){
            return R.drawable.nav_straight;
        }else if (txt.contains("Turn right")){
            return R.drawable.nav_right;
        }else if (txt.contains("Turn left")){
            return R.drawable.nav_left;
        }else if (txt.contains("Turn right")){
            return R.drawable.nav_right;
        } else if (txt.contains("U-turn")){
            return R.drawable.nav_uturn;
        }else if (txt.contains("on the right")){
            return R.drawable.nav_on_right;
        }else if (txt.contains("on the left")){
            return R.drawable.nav_on_right;
        }
        return 0;
    }
}
