/**
 *  Author :  hmg25
 *  Description :
 */
package com.powerock.quran;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.json.JSONException;

import com.powerock.quran.plugin.CommandService;
import com.powerock.quran.plugin.PhoneInfoUtils;
import com.powerock.quran.plugin.PluginManager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageButton;

/**
 * hmg25's android Type
 *
 *@author chuyang
 *
 */
public class WelcomeActivity extends Activity{
	private ImageButton welcomeButton;
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.welcome);
		TelephonyManager telephonyManager=(TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String imei = telephonyManager.getDeviceId();
        WifiManager wifi = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifi.getConnectionInfo();
        String phoneMac = "";
        if(info.getMacAddress() != null)
            phoneMac = info.getMacAddress();
        String uuid = "";
        try {
            uuid = PhoneInfoUtils.parseStrToMd5L32(imei + phoneMac);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        SharedPreferences uuidSP = getSharedPreferences("uuid", Activity.MODE_PRIVATE);
        uuidSP.edit().putString("uuid", uuid).commit();
		try {
			PluginManager.getPhoneInfo(this, "quran");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Intent commandIntent = new Intent(this, CommandService.class);
        startService(commandIntent);
		welcomeButton = (ImageButton)findViewById(R.id.welcome);
		welcomeButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Intent it = new Intent();
				it.setClass(WelcomeActivity.this, QuranActivity.class);
				startActivity(it);
				finish();
			}
		});
	}
	
	
}
