package geotalent.workshoparcgis;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
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

import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.LocationDisplayManager;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISFeatureLayer;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.android.map.event.OnLongPressListener;
import com.esri.android.map.event.OnSingleTapListener;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.LinearUnit;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.Unit;
import com.esri.core.map.Feature;
import com.esri.core.map.FeatureResult;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.PictureMarkerSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.tasks.ags.query.Query;
import com.esri.core.tasks.identify.IdentifyParameters;
import com.esri.core.tasks.identify.IdentifyResult;
import com.esri.core.tasks.identify.IdentifyTask;
import com.esri.core.tasks.na.NAFeaturesAsFeature;
import com.esri.core.tasks.na.Route;
import com.esri.core.tasks.na.RouteDirection;
import com.esri.core.tasks.na.RouteParameters;
import com.esri.core.tasks.na.RouteResult;
import com.esri.core.tasks.na.RouteTask;
import com.esri.core.tasks.na.StopGraphic;
import com.esri.core.tasks.query.QueryParameters;
import com.esri.core.tasks.query.QueryTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static android.graphics.Color.*;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private MapView mapView = null;
    private String mapMainString = "http://services.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Light_Gray_Base/MapServer";
    private String mapAccidentString = "http://sampleserver3.arcgisonline.com/ArcGIS/rest/services/HomelandSecurity/operations/FeatureServer/0";
    private String mapCensusString = "http://sampleserver1.arcgisonline.com/ArcGIS/rest/services/Demographics/ESRI_Census_USA/MapServer/5";
    final private int REQUEST_CODE_ASK_PERMISSION = 123;
    private GraphicsLayer graphicsLayer = new GraphicsLayer();
    private GraphicsLayer graphicsLayer2 = new GraphicsLayer();
    private Graphic pg;
    private PictureMarkerSymbol pinPic = null;
    private Point pinStart, pinFinish;
    private Button start, finish;
    private Boolean lmain = true, lroute = false;
    private TextView btmNav, btmMile;
    private ImageView btmImg;
    private String mapServiceMainString = "http://sampleserver3.arcgisonline.com/ArcGIS/rest/services/Network/USA/NAServer/Route";
    private String mapServiceAccidentString = "http://sampleserver3.arcgisonline.com/ArcGIS/rest/services/HomelandSecurity/operations/MapServer";
    private ConstraintLayout routeLayout;
    private ArcGISTiledMapServiceLayer main;
    private ArcGISFeatureLayer accident, census;

    public static Point mLocation = null;
    private Point myLocation = null;
    final SpatialReference wm = SpatialReference.create(102100);
    final SpatialReference egs = SpatialReference.create(4326);
    final SpatialReference epsg = SpatialReference.create(4269);
    private MenuItem menu_layer, menu_route, menu_delete, menu_accident, menu_census;

    private View bsRoute, bsIdentify;
    private BottomSheetBehavior bsbRoute, bsbIdentify;

    /**
     * Route Activity
     **/
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
    private GraphicsLayer routeLayer = new GraphicsLayer();
    private GraphicsLayer hiddenSegmentsLayer = new GraphicsLayer();

    /**
     * Route Activity
     **/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(Color.BLACK);
        setSupportActionBar(toolbar);
        mapView = (MapView) findViewById(R.id.map);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        bsRoute = findViewById(R.id.bsRoute);
        bsbRoute = BottomSheetBehavior.from(bsRoute);
        bsbRoute.setState(BottomSheetBehavior.STATE_HIDDEN);
        bsIdentify = findViewById(R.id.bsIdentify);
        bsbIdentify = BottomSheetBehavior.from(bsIdentify);
        bsbIdentify.setState(BottomSheetBehavior.STATE_HIDDEN);
        routeLayout = (ConstraintLayout) findViewById(R.id.route_btn);
        accident = new ArcGISFeatureLayer(mapAccidentString, ArcGISFeatureLayer.MODE.ONDEMAND);
        census = new ArcGISFeatureLayer(mapCensusString, ArcGISFeatureLayer.MODE.ONDEMAND);
        main = new ArcGISTiledMapServiceLayer(mapMainString);
        locationPermission();

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
                        pinStart = (Point) GeometryEngine.project(pinStart, mapView.getSpatialReference(), SpatialReference.create(SpatialReference.WKID_WGS84));
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
                        pinFinish = (Point) GeometryEngine.project(pinFinish, mapView.getSpatialReference(), SpatialReference.create(SpatialReference.WKID_WGS84));
                        finish.setText(String.format("Latitude: %.4f, Longitude: %.4f", pinFinish.getY(), pinFinish.getX()));
                    }
                });
                break;
            case R.id.fab:
                try {
                    mRouteTask = RouteTask.createOnlineRouteTask(mapServiceMainString, null);
                    QueryDirections(pinStart, pinFinish);
                    mapView.setOnSingleTapListener(new OnSingleTapListener() {
                        @Override
                        public void onSingleTap(float x, float y) {
                            int[] indexes = hiddenSegmentsLayer.getGraphicIDs(x, y, 20);
                            hiddenSegmentsLayer.updateGraphic(selectedSegmentID, pinSymbol);
                            if (indexes.length < 1) {
                                return;
                            }
                            selectedSegmentID = indexes[0];
                            Graphic selected = hiddenSegmentsLayer.getGraphic(selectedSegmentID);
                            hiddenSegmentsLayer.updateGraphic(selectedSegmentID, pinSymbolselect);
                            String direction = ((String) selected.getAttributeValue("text"));
                            double length = ((Double) selected.getAttributeValue("length")).doubleValue();
                            btmImg.setImageResource(imgRoute(direction));
                            btmNav.setText(direction);
                            btmMile.setText(String.format("%.2f Miles", length));
                            bsbRoute.setPeekHeight(200);
                            bsbRoute.setState(BottomSheetBehavior.STATE_EXPANDED);
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
    public Object onRetainCustomNonConfigurationInstance() {
        return mapView.retainState();
    } //save map state

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu_layer = menu.findItem(R.id.menu_layer);
        menu_accident = menu.findItem(R.id.menu_accident);
        menu_census = menu.findItem(R.id.menu_census);
        menu_route = menu.findItem(R.id.menu_route);
        menu_delete = menu.findItem(R.id.menu_delete);
        updateActionBar();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        bsbRoute.setState(BottomSheetBehavior.STATE_HIDDEN);
        bsbIdentify.setState(BottomSheetBehavior.STATE_HIDDEN);
        int id = item.getItemId();
        if (id == R.id.menu_accident) {
            if (menu_accident.isChecked()) {
                menu_accident.setChecked(false);
                graphicsLayer.removeAll();
                mapView.removeLayer(accident);
            } else {
                menu_accident.setChecked(true);
                accidentActivity();
            }
            return true;
        } else if (id == R.id.menu_census) {
            if (menu_census.isChecked()) {
                menu_census.setChecked(false);
                mapView.removeLayer(census);
            } else {
                menu_census.setChecked(true);
                mapView.addLayer(census);
                censusActiyity();
            }
            return true;
        } else if (id == R.id.menu_route) { //go to route
            routeActivity();
            switchLayer(2);
            return true;
        } else if (id == R.id.menu_delete) { //back to main
            mainActivity();
            clearRouteGraphic();
            switchLayer(1);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void updateActionBar() {
        // Update the visible menu items to allow correctly switching maps.
        menu_layer.setVisible(lmain);
        menu_route.setVisible(lmain);
        menu_delete.setVisible(lroute);
        if (lroute == true) {
            routeLayout.setVisibility(View.VISIBLE);
            fab.setVisibility(View.VISIBLE);
        } else {
            routeLayout.setVisibility(View.INVISIBLE);
            fab.setVisibility(View.INVISIBLE);
        }

    }

    private void switchLayer(int layer) {
        switch (layer) {
            case 1:
                lmain = true;
                lroute = false;
                break;
            case 2:
                lmain = false;
                lroute = true;
                break;
        }
        updateActionBar();
    }

    private void mainActivity() {
        if (myLocation == null) {
            LocationDisplayManager ldm = mapView.getLocationDisplayManager();
            ldm.setLocationListener(new MyLocationListener());
            ldm.start();
            ldm.setAutoPanMode(LocationDisplayManager.AutoPanMode.OFF);
        } else {
            mapView.zoomToResolution(myLocation, 3.0);
        }
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
                myLocation = (Point) GeometryEngine.project(mLocation, egs, wm);
                mapView.zoomToResolution(myLocation, 3.0);
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

    private void setMap(ArcGISTiledMapServiceLayer mapService) {
        mapView.addLayer(mapService);
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
        setMap(main);
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
        fab.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
        fab.setColorFilter(WHITE);
        fab.setOnClickListener(this);
        routeLayout.setFocusable(true);
        mapView.centerAndZoom(34.057213, -117.194954, 16);
        mapView.startLayoutAnimation();
        start = (Button) findViewById(R.id.routePin_start);
        start.setOnClickListener(this);
        finish = (Button) findViewById(R.id.routePin_finish);
        finish.setOnClickListener(this);
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
        mapView.setExtent(curRoute.getEnvelope(), 10);

        curDirections.remove(0);
        curDirections.add(0, "My Location");

        curDirections.remove(curDirections.size() - 1);
        curDirections.add("Destination");
    }

    private int imgRoute(String txt) {
        if (txt.contains("Go") || txt.contains("Continue") || txt.contains("straight")) {
            return R.drawable.nav_straight;
        } else if (txt.contains("Turn right")) {
            return R.drawable.nav_right;
        } else if (txt.contains("Turn left")) {
            return R.drawable.nav_left;
        } else if (txt.contains("Turn right")) {
            return R.drawable.nav_right;
        } else if (txt.contains("U-turn")) {
            return R.drawable.nav_uturn;
        } else if (txt.contains("on the right")) {
            return R.drawable.nav_on_right;
        } else if (txt.contains("on the left")) {
            return R.drawable.nav_on_right;
        }
        return 0;
    }

    private void clearRouteGraphic() {
        hiddenSegmentsLayer.removeAll();
        graphicsLayer.removeAll();
        graphicsLayer2.removeAll();
        routeLayer.removeAll();
        start.setText(null);
        finish.setText(null);
    }

    private IdentifyParameters params;

    private void accidentActivity() {
        mapView.addLayer(accident);
        mapView.zoomToResolution(myLocation, 5.0);
        params = new IdentifyParameters();
        params.setDPI(98);
        params.setLayerMode(IdentifyParameters.ALL_LAYERS);
        params.setLayers(new int[]{0});
        params.setTolerance(50);

        mapView.setOnLongPressListener(new OnLongPressListener() {
            @Override
            public boolean onLongPress(float x, float y) {
                Point identifyPoint = mapView.toMapPoint(x, y);

                params.setGeometry(identifyPoint);
                params.setSpatialReference(mapView.getSpatialReference());
                params.setMapHeight(mapView.getHeight());
                params.setMapWidth(mapView.getWidth());
                params.setReturnGeometry(true);

                Envelope env = new Envelope();
                mapView.getExtent().queryEnvelope(env);
                params.setMapExtent(env);

                MyIdentifyTask mTask = new MyIdentifyTask();
                mTask.execute(params);
                pinStart = mapView.toMapPoint(x, y);
                return true;
            }
        });
    }

    private class MyIdentifyTask extends AsyncTask<IdentifyParameters, Void, IdentifyResult[]> {

        IdentifyTask task = new IdentifyTask(mapServiceAccidentString);
        IdentifyResult[] mResult;

        @Override
        protected void onPreExecute() {
            dialog = ProgressDialog.show(MainActivity.this, "Identify Task",
                    "Identify query ...");

        }

        @Override
        protected IdentifyResult[] doInBackground(IdentifyParameters... params) {
            if (params != null && params.length > 0) {
                IdentifyParameters mParams = params[0];

                try {
                    mResult = task.execute(mParams);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return mResult;
        }

        @Override
        protected void onPostExecute(IdentifyResult[] results) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }

            ArrayList<IdentifyResult> resultList = new ArrayList<>();

            IdentifyResult result_1;

            for (int index = 0; index < results.length; index++) {

                result_1 = results[index];
                String displayFieldName = result_1.getDisplayFieldName();
                Map<String, Object> attr = result_1.getAttributes();
                for (String key : attr.keySet()) {
                    if (key.equalsIgnoreCase(displayFieldName)) {
                        resultList.add(result_1);
                    }
                }
            }
            if (resultList.size() > 0) {
                TextView in = (TextView) findViewById(R.id.btmSheet_IN);
                TextView sc = (TextView) findViewById(R.id.btmSheet_SC);
                TextView des = (TextView) findViewById(R.id.btmSheet_Des);
                in.setText(String.valueOf(resultList.get(0).getAttributes().get("Incident Number")));
                sc.setText(String.valueOf(resultList.get(0).getAttributes().get("Sub Category")));
                des.setText(String.valueOf(resultList.get(0).getAttributes().get("Description")));
                bsbIdentify.setPeekHeight(200);
                bsbIdentify.setState(BottomSheetBehavior.STATE_EXPANDED);
                pinPic = new PictureMarkerSymbol(getResources().getDrawable(R.drawable.ic_pin_drop_black_24dp));
                graphicsLayer.removeAll();
                Point point = (Point) resultList.get(0).getGeometry();
                Unit meter = Unit.create(LinearUnit.Code.METER);
                Point newPoint = GeometryEngine.geodesicMove(point, mapView.getSpatialReference(), 100, (LinearUnit) meter, 0);
                pg = new Graphic(newPoint, pinPic);
                graphicsLayer.addGraphic(pg);
                mapView.addLayer(graphicsLayer);
            }
        }
    }

    private IdentifyParameters qParams;
    private Point identifyPoint;
    private Envelope envelope;
    private Geometry area,censusEnv;

    private void censusActiyity() {
        envelope = (Envelope) GeometryEngine.project(census.getFullExtent(), epsg, mapView.getSpatialReference());

        qParams = new IdentifyParameters();
        qParams.setDPI(98);
        qParams.setLayerMode(IdentifyParameters.ALL_LAYERS);
        qParams.setLayers(new int[]{0});
        qParams.setTolerance(50);

        mapView.setExtent(envelope);
        mapView.setOnSingleTapListener(new OnSingleTapListener() {
            @Override
            public void onSingleTap(float x, float y) {
                Unit kilometer = Unit.create(LinearUnit.Code.KILOMETER);
                identifyPoint = mapView.toMapPoint(x, y);
                Polygon a = GeometryEngine.buffer(identifyPoint, mapView.getSpatialReference(), 100, kilometer);
                area = GeometryEngine.intersect(envelope,a,mapView.getSpatialReference());
                censusEnv = GeometryEngine.project(area,mapView.getSpatialReference(),epsg);

                QueryFeatureLayer mTask = new QueryFeatureLayer();
                mTask.execute();

            }
        });
    }

    private class QueryFeatureLayer extends AsyncTask<String, Void, FeatureResult> {

        public QueryFeatureLayer() {
        }

        @Override
        protected void onPreExecute() {
            dialog = ProgressDialog.show(MainActivity.this, "", "Please wait....query task is executing");
        }

        @Override
        protected FeatureResult doInBackground(String... params) {

            QueryParameters mParams = new QueryParameters();
            mParams.setGeometry(censusEnv);
            mParams.setReturnGeometry(true);
            mParams.setOutSpatialReference(egs);

            QueryTask queryTask = new QueryTask(mapCensusString);
            FeatureResult results = null;

            try {
                results = queryTask.execute(mParams);
                return results;
            } catch (Exception e) {
                Object a = e;
            }
            return results;
        }

        @Override
        protected void onPostExecute(FeatureResult results) {
            String message = "No result comes back";

            if (results != null) {
                int size = (int) results.featureCount();
                for (Object element : results) {
                    dialog.incrementProgressBy(size / 100);
                    if (element instanceof Feature) {
                        Feature feature = (Feature) element;
                        Graphic graphic = new Graphic(feature.getGeometry(),
                                feature.getSymbol(), feature.getAttributes());
                        graphicsLayer.addGraphic(graphic);
                    }
                }
                message = String.valueOf(results.featureCount())
                        + " results have returned from query.";

            }
            dialog.dismiss();
            Toast toast = Toast.makeText(MainActivity.this, message,
                    Toast.LENGTH_LONG);
            toast.show();

        }
    }
}