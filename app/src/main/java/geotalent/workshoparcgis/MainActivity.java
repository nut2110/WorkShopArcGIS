package geotalent.workshoparcgis;

import android.Manifest;
import android.animation.Animator;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.location.Location;
import android.location.LocationListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.LocationDisplayManager;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.android.map.event.OnLongPressListener;
import com.esri.android.map.event.OnSingleTapListener;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.LinearUnit;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.Unit;
import com.esri.core.map.Graphic;
import com.esri.core.renderer.ClassBreaksRenderer;
import com.esri.core.symbol.PictureMarkerSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.tasks.na.CostAttribute;
import com.esri.core.tasks.na.NAFeaturesAsFeature;
import com.esri.core.tasks.na.NetworkDescription;
import com.esri.core.tasks.na.Route;
import com.esri.core.tasks.na.RouteParameters;
import com.esri.core.tasks.na.RouteResult;
import com.esri.core.tasks.na.RouteTask;
import com.esri.core.tasks.na.StopGraphic;

import java.util.List;

import static android.graphics.Color.*;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    MapView mapView = null;
    String mapService = "http://services.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Light_Gray_Base/MapServer ";
    final private int REQUEST_CODE_ASK_PERMISSION = 123;
    GraphicsLayer graphicsLayer = new GraphicsLayer();
    GraphicsLayer graphicsLayer2 = new GraphicsLayer();
    Graphic pg;
    PictureMarkerSymbol pinPic = null;
    Point pinStart, pinFinish;
    Button start, finish;
    ImageButton rounteDelete, rountingBtn, layerBtn;

    public static Point mLocation = null;
    final SpatialReference wm = SpatialReference.create(102100);
    final SpatialReference egs = SpatialReference.create(4326);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        locationPermission();
    }

    /*@Override
    protected void onPause() {
        super.onPause();
        mapView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.unpause();
    }*/

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.main_route:
                routeActivity();
                break;
            case R.id.route_delete:
                mainActivity();
                break;
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
                RouteTask mRouteTask;
                RouteResult mResults;
                RouteParameters rp;
                Route curRoute = null;
                GraphicsLayer routeLayer = new GraphicsLayer();
                mapView.addLayer(routeLayer);
                try {
                    mRouteTask = RouteTask
                            .createOnlineRouteTask(
                                    "http://sampleserver3.arcgisonline.com/ArcGIS/rest/services/Network/USA/NAServer/Route ",
                                    null);
                    rp = mRouteTask.retrieveDefaultRouteTaskParameters();
                    mResults = mRouteTask.solve(rp);
                    curRoute = mResults.getRoutes().get(0);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                SimpleLineSymbol routeSymbol = new SimpleLineSymbol(Color.BLUE, 3);
                PictureMarkerSymbol destinationSymbol = new PictureMarkerSymbol(
                        mapView.getContext(), getResources().getDrawable(
                        R.drawable.ic_my_location_black_24dp));
                Graphic routeGraphic = new Graphic(curRoute.getRouteGraphic()
                        .getGeometry(), routeSymbol);
                Graphic endGraphic = new Graphic(
                        ((Polyline) routeGraphic.getGeometry()).getPoint(((Polyline) routeGraphic
                                .getGeometry()).getPointCount() - 1), destinationSymbol);
                routeLayer.addGraphics(new Graphic[]{routeGraphic, endGraphic});
                mapView.addLayer(routeLayer);
                break;
        }
    }

    private void mainActivity() {
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setMap();
        rountingBtn = (ImageButton) findViewById(R.id.main_route);
        rountingBtn.setOnClickListener(this);
        layerBtn = (ImageButton) findViewById(R.id.main_layer);
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

    private void routeActivity() {
        setContentView(R.layout.activity_route);
        rounteDelete = (ImageButton) findViewById(R.id.route_delete);
        rounteDelete.setOnClickListener(this);
        setMap();
        start = (Button) findViewById(R.id.routePin_start);
        start.setOnClickListener(this);
        finish = (Button) findViewById(R.id.routePin_finish);
        finish.setOnClickListener(this);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
        fab.setColorFilter(WHITE);
        fab.setOnClickListener(this);
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

}
