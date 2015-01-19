package com.powerock.quran.plugin;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by chuyang on 2014/12/31.
 */
public class PhoneInfoUtils {
    public static String parseStrToMd5L32(String str) throws NoSuchAlgorithmException {
        String reStr = null;
        str = str + "POWEROCK";
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(str.getBytes());
            StringBuffer stringBuffer = new StringBuffer();
            for (byte b : bytes){
                int bt = b&0xff;
                if (bt < 16){
                    stringBuffer.append(0);
                }
                stringBuffer.append(Integer.toHexString(bt));
            }
            reStr = stringBuffer.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return reStr;
    }

    public static String getLocalIpAddress(Context context) throws SocketException {
        // 判断wifi是否开启
        WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        String ipString = "";
        if (wifiManager.isWifiEnabled()) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int ipAddress = wifiInfo.getIpAddress();
            // 格式转换
            ipString = (ipAddress & 0xFF) + "." + ((ipAddress >> 8) & 0xFF)
                    + "." + ((ipAddress >> 16) & 0xFF) + "."
                    + ((ipAddress >> 24) & 0xFF);
        } else { // 如果wifi没有开启的话，就获取3G的IP
            try {
                for (Enumeration<NetworkInterface> en = NetworkInterface
                        .getNetworkInterfaces(); en.hasMoreElements();) {
                    NetworkInterface intf = en.nextElement();

                    // 遍历所有的网卡设备，一般移动设备上只有2张网卡，其中一张是环回网卡
                    for (Enumeration<InetAddress> enumIpAddr = intf
                            .getInetAddresses(); enumIpAddr.hasMoreElements();) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        // 过滤掉环回网卡和IPv6
                        if (!inetAddress.isLoopbackAddress()
                                && !(inetAddress instanceof Inet6Address)) {
                            ipString = inetAddress.getHostAddress().toString();
                        }
                    }
                }
            } catch (SocketException ex) {
                Log.e("TAG", "getIpAddr()" + ex.toString());
            }
        }
        return ipString;
    }

}
