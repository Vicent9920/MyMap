package cn.com.vicent.mymap;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.navi.AMapNavi;
import com.amap.api.navi.AMapNaviListener;
import com.amap.api.navi.model.AMapLaneInfo;
import com.amap.api.navi.model.AMapNaviCameraInfo;
import com.amap.api.navi.model.AMapNaviCross;
import com.amap.api.navi.model.AMapNaviInfo;
import com.amap.api.navi.model.AMapNaviLocation;
import com.amap.api.navi.model.AMapNaviPath;
import com.amap.api.navi.model.AMapNaviTrafficFacilityInfo;
import com.amap.api.navi.model.AMapServiceAreaInfo;
import com.amap.api.navi.model.AimLessModeCongestionInfo;
import com.amap.api.navi.model.AimLessModeStat;
import com.amap.api.navi.model.NaviInfo;
import com.amap.api.navi.model.NaviLatLng;
import com.amap.api.navi.view.RouteOverLay;
import com.amap.api.services.core.LatLonPoint;
import com.autonavi.tbt.TrafficFacilityInfo;
import com.zhy.adapter.recyclerview.CommonAdapter;
import com.zhy.adapter.recyclerview.base.ViewHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NavigationActivity extends AppCompatActivity implements View.OnClickListener, LocationSource, AMapLocationListener, AMapNaviListener {
    private static final String TAG = "NavigationActivity";
    private TabLayout mTabLayout;
    private TextView tvStart,tvEnd;
    private TextView tvNavi;
    private RelativeLayout oneWay;
    private TextView tvTime,tvLength;
    private int navigationType = 0;
    private AMap amap;
    private MapView mapview;
    private OnLocationChangedListener mListener;
    private AMapLocationClient mlocationClient;
    private AMapLocationClientOption mLocationOption;
    private RecyclerView mRecyclerView;
    private CommonAdapter mAdapter;
    /**************************************************导航相关************************************** ********************/
    private AMapNavi mAMapNavi;
    /**
     * 起点坐标集合[由于需要确定方向，建议设置多个起点]
     */
    private List<NaviLatLng> startList = new ArrayList<NaviLatLng>();
    /**
     * 途径点坐标集合
     */
    private List<NaviLatLng> wayList = new ArrayList<NaviLatLng>();
    /**
     * 终点坐标集合［建议就一个终点］
     */
    private List<NaviLatLng> endList = new ArrayList<NaviLatLng>();
    /**
     * 保存当前算好的路线
     */
    private SparseArray<RouteOverLay> routeOverlays = new SparseArray<RouteOverLay>();
    private List<AMapNaviPath> ways = new ArrayList<>();
    private boolean calculateSuccess;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar!=null)actionBar.hide();
        initView();
        mapview.onCreate(savedInstanceState);// 此方法必须重写
        initMap();

    }

    private void initView() {
        mapview = (MapView) findViewById(R.id.navi_view);
        mRecyclerView = (RecyclerView) findViewById(R.id.rl_rlv_ways);
        mRecyclerView.setLayoutManager(new GridLayoutManager(this,3));
        mAdapter = getAdapter();
        mRecyclerView.setAdapter(mAdapter);
        oneWay = (RelativeLayout) findViewById(R.id.ll_rl_1way);
        tvTime = (TextView) findViewById(R.id.rl_tv_time);
        tvLength = (TextView) findViewById(R.id.rl_tv_length);
        mTabLayout = (TabLayout) findViewById(R.id.tabs);
        //tab的字体选择器,默认灰色,选择时白色
        mTabLayout.setTabTextColors(Color.BLACK, Color.WHITE);
        //设置tab的下划线颜色,默认是粉红色
        mTabLayout.setSelectedTabIndicatorColor(Color.WHITE);
        mTabLayout.addTab(mTabLayout.newTab().setText("驾车"));
        mTabLayout.addTab(mTabLayout.newTab().setText("步行"));
        mTabLayout.addTab(mTabLayout.newTab().setText("骑车"));
        tvStart = (TextView) findViewById(R.id.rl_tv_start);
        tvEnd = (TextView) findViewById(R.id.rl_tv_end);
        tvNavi = (TextView) findViewById(R.id.rl_tv_navistart);
        tvNavi.setOnClickListener(this);
        tvEnd.setOnClickListener(this);
        //添加页卡标题

        mTabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String tabName = tab.getText().toString();
                if(tabName.equals("驾车")){
                    navigationType = 0;
                }else if(tabName.equals("步行")){
                    navigationType = 1;
                }else{
                    navigationType = 2;
                }
                clearRoute();
                planRoute();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    /**
     * 初始化AMap对象
     */
    private void initMap() {
        if (amap == null) {
            amap = mapview.getMap();
            //设置显示定位按钮 并且可以点击
            UiSettings settings = amap.getUiSettings();
            amap.setLocationSource(this);//设置了定位的监听,这里要实现LocationSource接口
            // 是否显示定位按钮
            settings.setMyLocationButtonEnabled(true);
            amap.setMyLocationEnabled(true);//显示定位层并且可以触发定位,默认是flase
            mAMapNavi = AMapNavi.getInstance(getApplicationContext());
            mAMapNavi.addAMapNaviListener(this);
            amap.moveCamera(CameraUpdateFactory.zoomTo(15));
        }
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.rl_tv_end:
                Intent intent = new Intent(this,PiclocationActivity.class);
                startActivityForResult(intent,0);
                break;
            case R.id.rl_tv_navistart:
                if(startList.size()==0){
                    Snackbar.make(tvEnd,"未获取到当前位置，不能导航",Snackbar.LENGTH_SHORT).show();
                }else if(endList.size()==0){
                    Snackbar.make(tvEnd,"未获取到终点，不能导航",Snackbar.LENGTH_SHORT).show();
                }else{
                    if (!calculateSuccess) {
                        Snackbar.make(tvEnd,"请先计算路线",Snackbar.LENGTH_SHORT).show();
                        return;
                    }else{
                        Intent activity = new Intent(this, GPSNaviActivity.class);
                        activity.putExtra("start",startList.get(0));
                        activity.putExtra("end",endList.get(0));
                        startActivity(activity);
//                        Intent gpsintent = new Intent(getApplicationContext(), RouteNaviActivity.class);
//                        gpsintent.putExtra("gps", true);
//                        startActivity(gpsintent);
                        finish();
                    }
                }
        }
    }

    /**
     * 绘制路线
     * @param routeId
     * @param path
     */
    private void drawRoutes(int routeId, AMapNaviPath path) {
        calculateSuccess = true;
        amap.moveCamera(CameraUpdateFactory.changeTilt(0));
        RouteOverLay routeOverLay = new RouteOverLay(amap, path, this);
        routeOverLay.setTrafficLine(false);
        routeOverLay.addToMap();
        routeOverlays.put(routeId, routeOverLay);

    }

    /**
     * 获取终点信息
     * @param requestCode
     * @param resultCode
     * @param intent
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if(resultCode==RESULT_OK){
            tvEnd.setText("到     "+intent.getStringExtra("address"));
            LatLonPoint endLp = intent.getParcelableExtra("value");
            endList.clear();
            endList.add(new NaviLatLng(endLp.getLatitude(),endLp.getLongitude()));
        }
    }






    /**
     * 方法必须重写
     */
    protected void onResume() {
        super.onResume();
        mapview.onResume();
        clearRoute();
        planRoute();
    }
    /**
     * 清除当前地图上算好的路线
     */
    private void clearRoute() {
        for (int i = 0; i < routeOverlays.size(); i++) {
            RouteOverLay routeOverlay = routeOverlays.valueAt(i);
            routeOverlay.removeFromMap();
        }
        routeOverlays.clear();
    }
    /**
     * 路线规划
     */
    private void planRoute() {
        if(startList.size()>0 && endList.size()>0){
            if(navigationType == 0){//驾车
                int strategy=0;
                try {
                    strategy = mAMapNavi.strategyConvert(true, false, false, true, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mAMapNavi.calculateDriveRoute(startList, endList, wayList, strategy);
            }else if(navigationType == 1){//步行
                mAMapNavi.calculateWalkRoute(startList.get(0), endList.get(0));
            }else{//骑行
                mAMapNavi.calculateRideRoute(startList.get(0), endList.get(0));
            }
        }
    }


    @Override
    public void onCalculateRouteFailure(int i) {
        calculateSuccess = false;
        Snackbar.make(tvEnd,"计算路线失败",Snackbar.LENGTH_SHORT).show();
    }


    /**
     * 多条路径计算结果回调
     * @param ints
     */
    @Override
    public void onCalculateMultipleRoutesSuccess(int[] ints) {
//清空上次计算的路径列表。
        routeOverlays.clear();
        ways.clear();
        HashMap<Integer, AMapNaviPath> paths = mAMapNavi.getNaviPaths();
        for (int i = 0; i < ints.length; i++) {
            AMapNaviPath path = paths.get(ints[i]);
            if (path != null) {
                if(i == 0){
                    drawRoutes(-1, path);
                    ways.add(path);
                }else{
                    ways.add(path);
                }

            }
        }
        if(ways.size()>0){
            mAdapter.notifyDataSetChanged();
            mRecyclerView.setVisibility(View.VISIBLE);
            oneWay.setVisibility(View.GONE);
            tvNavi.setText("开始导航");
        }else if(ways.size()==1){
            mRecyclerView.setVisibility(View.GONE);
            oneWay.setVisibility(View.VISIBLE);
            tvTime.setText(getTime(ways.get(0).getAllTime()));
            tvLength.setText(getLength(ways.get(0).getAllLength()));
            tvNavi.setText("开始导航");
        }else{
            mRecyclerView.setVisibility(View.GONE);
            tvNavi.setText("准备导航");
        }

    }
    /**
     * 单条路线计算结果回调
     *  多条路线与单条路线不是根据结果来标识，而是根据计算方法的参数来标识
     */
    @Override
    public void onCalculateRouteSuccess() {
        /**
         * 清空上次计算的路径列表。
         */
        routeOverlays.clear();
        ways.clear();
        AMapNaviPath path = mAMapNavi.getNaviPath();
        /**
         * 单路径不需要进行路径选择，直接传入－1即可
         */
        drawRoutes(-1, path);
        mRecyclerView.setVisibility(View.GONE);
        oneWay.setVisibility(View.VISIBLE);
        tvTime.setText(getTime(path.getAllTime()));
        tvLength.setText(getLength(path.getAllLength()));
        tvNavi.setText("开始导航");
    }



    private CommonAdapter getAdapter() {

        return new CommonAdapter<AMapNaviPath>(this, R.layout.item_recycleview_naviways, ways)
        {

            @Override
            protected void convert(ViewHolder holder, final AMapNaviPath aMapNaviPath, int position) {
//                holder.setText(R.id.ll_tv_labels,aMapNaviPath.getLabels());
                holder.setText(R.id.ll_tv_time,getTime(aMapNaviPath.getAllTime()));
                holder.setText(R.id.ll_tv_length,getLength(aMapNaviPath.getAllLength()));
                holder.getView(R.id.ll_itemview).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clearRoute();
                        drawRoutes(-1, aMapNaviPath);
                    }
                });
            }
        };
    }

    /**
     * 计算路程
     * @param allLength
     * @return
     */
    private String getLength(int allLength) {
        if(allLength>1000){
            int remainder = allLength%1000;
            String m = remainder>0 ? remainder+"米":"";
            return allLength/1000+"公里"+m;
        }else{
            return allLength+"米";
        }
    }
    /**
     * 计算时间
     * @param allTime
     * @return
     */
    private String getTime(int allTime) {
        if(allTime>3600){//1小时以上
            int minute = allTime%3600;
            String min = minute/60!=0?minute/60+"分钟":"";
            return allTime/3600+"小时"+min;
        }else{
            int minute = allTime%3600;
            return minute/60+"分钟";
        }
    }
    /**
     * 定位地点
     * @param amapLocation
     */
    @Override
    public void onLocationChanged(AMapLocation amapLocation) {
        if (mListener != null&&amapLocation != null) {
            if (amapLocation != null
                    &&amapLocation.getErrorCode() == 0) {
                startList.add(new NaviLatLng(amapLocation.getLatitude(),amapLocation.getLongitude()));
                mListener.onLocationChanged(amapLocation);// 显示系统小蓝点
            } else {
                String errText = "定位失败," + amapLocation.getErrorCode()+ ": " + amapLocation.getErrorInfo();
                Log.e("AmapErr",errText);
            }
        }
    }
    /**
     * 激活定位
     * @param listener
     */
    @Override
    public void activate(OnLocationChangedListener listener) {
        mListener = listener;
        if (mlocationClient == null) {
            //初始化定位
            mlocationClient = new AMapLocationClient(this);
            //初始化定位参数
            mLocationOption = new AMapLocationClientOption();
            //设置定位回调监听
            mlocationClient.setLocationListener(this);
            //设置为高精度定位模式
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
//            mLocationOption.setOnceLocation(true);
            //设置定位参数
            mlocationClient.setLocationOption(mLocationOption);
            // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
            // 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
            // 在定位结束后，在合适的生命周期调用onDestroy()方法
            // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
            mlocationClient.startLocation();//启动定位
        }
    }
    /**
     * 注销定位
     */
    @Override
    public void deactivate() {
        mListener = null;
        if (mlocationClient != null) {
            mlocationClient.stopLocation();
            mlocationClient.onDestroy();
        }
        mlocationClient = null;
    }
    /**
     * 方法必须重写
     */
    @Override
    protected void onPause() {
        super.onPause();
        mapview.onPause();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapview.onSaveInstanceState(outState);
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapview.onDestroy();
        if(null != mlocationClient){
            mlocationClient.onDestroy();
        }
    }
    /**
     * ************************************************** 在算路页面，以下接口全不需要处理，在以后的版本中SDK会进行优化***********************************************************************************************
     **/

    @Override
    public void onReCalculateRouteForYaw() {

    }
    @Override
    public void onInitNaviSuccess() {

    }
    @Override
    public void onGetNavigationText(int i, String s) {

    }

    @Override
    public void onInitNaviFailure() {

    }



    @Override
    public void onStartNavi(int i) {

    }

    @Override
    public void onTrafficStatusUpdate() {

    }

    @Override
    public void onLocationChange(AMapNaviLocation aMapNaviLocation) {

    }



    @Override
    public void onEndEmulatorNavi() {

    }

    @Override
    public void onArriveDestination() {

    }



    @Override
    public void onReCalculateRouteForTrafficJam() {

    }

    @Override
    public void onArrivedWayPoint(int i) {

    }

    @Override
    public void onGpsOpenStatus(boolean b) {

    }

    @Override
    public void onNaviInfoUpdate(NaviInfo naviInfo) {

    }

    @Override
    public void onNaviInfoUpdated(AMapNaviInfo aMapNaviInfo) {

    }

    @Override
    public void updateCameraInfo(AMapNaviCameraInfo[] aMapNaviCameraInfos) {

    }

    @Override
    public void onServiceAreaUpdate(AMapServiceAreaInfo[] aMapServiceAreaInfos) {

    }


    @Override
    public void showCross(AMapNaviCross aMapNaviCross) {

    }

    @Override
    public void hideCross() {

    }

    @Override
    public void showLaneInfo(AMapLaneInfo[] aMapLaneInfos, byte[] bytes, byte[] bytes1) {

    }

    @Override
    public void hideLaneInfo() {

    }



    @Override
    public void notifyParallelRoad(int i) {

    }

    @Override
    public void OnUpdateTrafficFacility(AMapNaviTrafficFacilityInfo aMapNaviTrafficFacilityInfo) {

    }

    @Override
    public void OnUpdateTrafficFacility(AMapNaviTrafficFacilityInfo[] aMapNaviTrafficFacilityInfos) {

    }

    @Override
    public void OnUpdateTrafficFacility(TrafficFacilityInfo trafficFacilityInfo) {

    }

    @Override
    public void updateAimlessModeStatistics(AimLessModeStat aimLessModeStat) {

    }

    @Override
    public void updateAimlessModeCongestionInfo(AimLessModeCongestionInfo aimLessModeCongestionInfo) {

    }

    @Override
    public void onPlayRing(int i) {

    }


}
