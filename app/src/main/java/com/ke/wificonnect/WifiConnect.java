package com.ke.wificonnect;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;

import com.orhanobut.logger.Logger;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.schedulers.Schedulers;


public class WifiConnect {
    public static final int TYPE_NO_PASSWORD = 1;
    public static final int TYPE_WEP = 2;
    public static final int TYPE_WPA = 3;
    private static final int DEFAULT_WIFI_TYPE = 3;


    public static Observable<Boolean> openDeviceWifi(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        WifiManager wifiManager = (WifiManager) appContext.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager.isWifiEnabled()) {
            return Observable.just(Boolean.TRUE);
        }
        boolean result = wifiManager.setWifiEnabled(true);
        if (!result) {
            //用户 授权失败
            return Observable.just(Boolean.FALSE);
        }

        return Observable
                .create((ObservableOnSubscribe<Boolean>) e -> {
                    boolean isConnected = false;
                    try {
                        while (!isConnected) {
                            Thread.sleep(200);
                            isConnected = wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED;
                        }
                        e.onNext(Boolean.TRUE);
                    } catch (Exception e1) {
                        //InterruptedException
                        e.onNext(Boolean.FALSE);
                    }
                })
                .subscribeOn(Schedulers.newThread());

    }


    public static Observable<Boolean> connectWifi(@NonNull String wifiName, @NonNull String password, @NonNull WifiManager wifiManager) {

        if (!wifiManager.isWifiEnabled()) {
            Logger.d("wifi 没有打开，请打开wifi再来");
            return Observable.just(Boolean.FALSE);
        }

        String wifiNameSSID = "\"" + wifiName + "\"";
        if (wifiNameSSID.equals(wifiManager.getConnectionInfo().getSSID())) {
            Logger.d("要进行连接的Wi-Fi已经连接");
            return Observable.just(Boolean.TRUE);
        }
        //记录 当前 连接的 netid
        int currentNetId = wifiManager.getConnectionInfo().getNetworkId();

        return Observable
                .create((ObservableOnSubscribe<Boolean>) subscriber -> {
                    final WifiConnect wifiConnect = new WifiConnect(wifiManager);
                    boolean isContainsWifiName = wifiConnect.containsSpecialSSID(wifiName);
                    Logger.d("Wi-Fi 列表 是否 包含 指定 Wi-Fi 名称 =" + isContainsWifiName);
                    if (!isContainsWifiName) {
                        subscriber.onNext(Boolean.FALSE);
                        return;
                    }


                    WifiConfiguration configuration = wifiConnect.createWifConfig(wifiName, password, DEFAULT_WIFI_TYPE);
                    boolean result = wifiConnect.addNetwork(configuration);
                    if (!result) {
                        subscriber.onNext(Boolean.FALSE);
                        return;
                    }

                    int retryTimes = 5;

                    while (retryTimes > 0) {
                        if (wifiNameSSID.equals(wifiManager.getConnectionInfo().getSSID())) {
                            subscriber.onNext(Boolean.TRUE);
                            return;
                        }
                        Thread.sleep(2000);
                        retryTimes--;
                    }

                    Logger.d("打开 Wi-Fi 失败");
                    subscriber.onNext(Boolean.FALSE);


                }).subscribeOn(Schedulers.newThread()).doOnDispose(() -> {
                    boolean result = wifiManager.enableNetwork(currentNetId, true);
                    Logger.d("dispose call ,reconnect wifi = " + result);
                });
    }


    public static boolean isWifiOpen(Context context) {
        return getWifiManager(context).isWifiEnabled();
    }

    public static void openWifi(Context context) {
        if (!isWifiOpen(context)) {
            getWifiManager(context).setWifiEnabled(true);
        }
    }

    private static WifiManager getWifiManager(Context context) {
        return (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }


    // 定义WifiManager对象
    private WifiManager mWifiManager;


    // 构造器
    private WifiConnect(@NonNull WifiManager wifiManager) {
        this.mWifiManager = wifiManager;
    }

    // 打开WIFI
    public void openWifi() {
        if (!mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(true);
        }
    }

    // 关闭WIFI
    public void closeWifi() {
        if (mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(false);
        }
    }

    /**
     * wifi是否打开
     */
    public boolean isWifiOpen() {
        return mWifiManager.isWifiEnabled();
    }

    // 检查当前WIFI状态
    public int checkState() {
        return mWifiManager.getWifiState();
    }


    // 得到配置好的网络 包括 之前保存的
    private List<WifiConfiguration> getConfiguration() {
        return mWifiManager.getConfiguredNetworks();
    }


    /**
     * 开始 扫描 Wi-Fi列表
     */
    private void startScan() {
        mWifiManager.startScan();
    }


    public boolean containsSpecialSSID(String SSID) {
        List<ScanResult> list = mWifiManager.getScanResults();
        for (ScanResult scanResult : list) {
            if (scanResult.SSID.equals(SSID)) {
                return true;
            }
        }
        return false;
    }


    /**
     * 获取当前 连接的Wi-Fi 的名称
     */
    private String getConnectingWifiSSID() {
        return mWifiManager.getConnectionInfo().getSSID();
    }


    // 添加一个网络并连接
    private boolean addNetwork(WifiConfiguration wcg) {
        int wcgID = mWifiManager.addNetwork(wcg);
        return mWifiManager.enableNetwork(wcgID, true);

    }


    // 断开指定ID的网络
    private void disconnectWifi(int netId) {
        mWifiManager.disableNetwork(netId);
        mWifiManager.disconnect();
    }


    private WifiConfiguration createWifConfig(String SSID, String Password, int Type) {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + SSID + "\"";

        WifiConfiguration tempConfig = isContainsTargetWifi(SSID);

        if (tempConfig != null) {
            mWifiManager.removeNetwork(tempConfig.networkId);
        }

        if (Type == TYPE_NO_PASSWORD) //WIFICIPHER_NOPASS
        {
            config.wepKeys[0] = "";
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        }
        if (Type == TYPE_WEP) //WIFICIPHER_WEP
        {
            config.hiddenSSID = true;
            config.wepKeys[0] = "\"" + Password + "\"";
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        }
        if (Type == TYPE_WPA) //WIFICIPHER_WPA
        {
            config.preSharedKey = "\"" + Password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            //config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        }
        return config;
    }

    /**
     * 判断 是否保存 过此wifi信息
     */
    private WifiConfiguration isContainsTargetWifi(String SSID) {
        List<WifiConfiguration> existingConfigs = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (existingConfig.SSID.equals("\"" + SSID + "\"")) {
                return existingConfig;
            }
        }
        return null;
    }


}
