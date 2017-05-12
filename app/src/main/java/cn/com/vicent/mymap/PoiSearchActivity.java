package cn.com.vicent.mymap;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AutoCompleteTextView;

import com.amap.api.location.AMapLocation;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.help.Inputtips;
import com.amap.api.services.help.InputtipsQuery;
import com.amap.api.services.help.Tip;
import com.zhy.adapter.recyclerview.CommonAdapter;
import com.zhy.adapter.recyclerview.base.ViewHolder;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class PoiSearchActivity extends AppCompatActivity implements TextWatcher, Inputtips.InputtipsListener, View.OnClickListener {

    private CommonAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private List<Tip> tips = new ArrayList<>();
    private String city = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poi_search);
        getSupportActionBar().hide();
        EventBus.getDefault().register(this);
        AutoCompleteTextView mKeywordText = (AutoCompleteTextView)findViewById(R.id.input_edittext);
        mKeywordText.addTextChangedListener(this);
        findViewById(R.id.rl_tv_map_pick).setOnClickListener(this);
        mRecyclerView = (RecyclerView) findViewById(R.id.ll_rv_inputlist);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = getAdapter();
        mRecyclerView.setAdapter(mAdapter);
    }

    /**
     * 跳转到地图地图选点Activity
     * @param v
     */
    @Override
    public void onClick(View v) {
        startActivity(new Intent(this,PiclocationActivity.class));
    }

    /**
     * 选择了地点，关闭当前Activity
     * @param tip
     */
    @Subscribe(threadMode = ThreadMode.MAIN,sticky = false,priority = 2)
    public void close(Tip tip) {
       finish();
    };

    /**
     * 设置当前城市
     * @param amapLocation
     */
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void setCity(AMapLocation amapLocation) {
        if(amapLocation!=null){
            this.city = amapLocation.getCity();
        }
    };
    /**
     * 文本变化监听事件
     * @param s
     * @param start
     * @param before
    * @param count
     */
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        String newText = s.toString().trim();
        InputtipsQuery inputquery = new InputtipsQuery(newText, city);
        inputquery.setCityLimit(true);
        Inputtips inputTips = new Inputtips(this, inputquery);
        inputTips.setInputtipsListener(this);
        inputTips.requestInputtipsAsyn();
    }

    /**
     * 输入自动提示结果
     * @param list
     * @param i
     */
    @Override
    public void onGetInputtips(List<Tip> list, int i) {
        if(i == AMapException.CODE_AMAP_SUCCESS){
            if(tips.size()>0)
                tips.clear();
            for (Tip tip:list) {
                if(tip.getPoint()!=null){
                    tips.add(tip);
                }
            }
//            tips.addAll(list);
            mAdapter.notifyDataSetChanged();
        }
    }
    private CommonAdapter getAdapter() {
        return new CommonAdapter<Tip>(this,R.layout.item_layout,tips) {

            @Override
            protected void convert(ViewHolder holder, final Tip tip, int position) {

                holder.setText(R.id.poi_field_id,tip.getName());
                holder.setText(R.id.poi_value_id,tip.getDistrict());
                holder.getView(R.id.item_layout).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        EventBus.getDefault().post(tip);
                        finish();
                    }
                });
            }
        };
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }
    @Override
    public void afterTextChanged(Editable s) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
