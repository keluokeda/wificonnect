package com.ke.wificonnect;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.Logger;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.et_ssid)
    EditText mEtSsid;
    @BindView(R.id.et_password)
    EditText mEtPassword;
    @BindView(R.id.list_view)
    ListView mListView;

    @BindView(R.id.btn_dispose)
    Button mBtnDispose;
    private WifiManager mWifiManager;
    private WifiManager wifiManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);


        mBtnDispose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                diopos(v);
            }
        });

        Logger.addLogAdapter(new AndroidLogAdapter());

        getWifiList();

        mWifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("刷新");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        getWifiList();
        return super.onOptionsItemSelected(item);

    }

    private void getWifiList() {
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);



        final List<ScanResult> list = wifiManager.getScanResults();
        List<String> stringList = new ArrayList<>(list.size());
        for (ScanResult scanResult : list) {
            stringList.add(scanResult.toString());
        }

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1, stringList);
        mListView.setAdapter(arrayAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Logger.d("current wifi ssid = " + mWifiManager.getConnectionInfo().getSSID() + ",current wifi state = " + wifiManager.getWifiState());

                String ssid = list.get(position).SSID;
                mEtSsid.setText(ssid);
            }
        });
    }

    private Disposable mDisposable;

    @OnClick(R.id.btn_connect)
    public void onViewClicked() {
        String wifiName = mEtSsid.getText().toString();
        String password = mEtPassword.getText().toString();

        mDisposable = WifiConnect.connectWifi(wifiName, password, mWifiManager)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        Logger.d("result = " + aBoolean);
                    }
                });
    }


    public void diopos(View view) {
        mDisposable.dispose();
    }
}
