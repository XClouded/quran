package com.powerock.quran;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.powerock.quran.plugin.Constants;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

public class QuranActivity extends Activity {
	/** Called when the activity is first created. */
	private static PageWidget mPageWidget;
	static Bitmap mCurPageBitmap;
	static Bitmap mNextPageBitmap;
	static Canvas mCurPageCanvas;
	static Canvas mNextPageCanvas;
	static BookPageFactory pagefactory;
	private int pageWidth;
	private int pageHeight;	
	private AlertDialog dialog = null;

	@SuppressLint("WrongCall")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		DisplayMetrics metric = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metric);
		pageWidth = metric.widthPixels;
		pageHeight = metric.heightPixels;
		mPageWidget = new PageWidget(this, pageWidth, pageHeight);
		setContentView(mPageWidget);
		initBook();
		mCurPageBitmap = Bitmap.createBitmap(pageWidth, pageHeight, Bitmap.Config.ARGB_8888);
		mNextPageBitmap = Bitmap
				.createBitmap(pageWidth, pageHeight, Bitmap.Config.ARGB_8888);

		mCurPageCanvas = new Canvas(mCurPageBitmap);
		mNextPageCanvas = new Canvas(mNextPageBitmap);
		pagefactory = new BookPageFactory(this, pageWidth, pageHeight);

		pagefactory.setBgBitmap(Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
				this.getResources(), R.drawable.bg), pageWidth, pageHeight, true));

		try {
			//pagefactory.openbook("/sdcard/test.txt");
			pagefactory.openbook(Constants.FILE_PATH);
			pagefactory.onDraw(mCurPageCanvas);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			Toast.makeText(this, "电子书不存在,请将《test.txt》放在SD卡根目录下",
					Toast.LENGTH_SHORT).show();
		}
		SharedPreferences sp =getSharedPreferences("page_number", Activity.MODE_PRIVATE);
		long pageNumber = sp.getLong("number", 1);
		if(pageNumber == 1){
			GetNumberTask gnt = new GetNumberTask();
			gnt.execute();
		}
		SharedPreferences spLast =getSharedPreferences("last_number", Activity.MODE_PRIVATE);
    	int lastPageNumber = spLast.getInt("number", 1);
    	try {
			pagefactory.redirectToPage(lastPageNumber);
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		mPageWidget.setBitmaps(mCurPageBitmap, mCurPageBitmap);

		mPageWidget.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent e) {
				// TODO Auto-generated method stub
				
				boolean ret=false;
				if (v == mPageWidget) {
					if (e.getAction() == MotionEvent.ACTION_DOWN) {
						mPageWidget.abortAnimation();
						mPageWidget.calcCornerXY(e.getX(), e.getY());
						pagefactory.onDraw(mCurPageCanvas);
						if (mPageWidget.DragToRight()) {
							try {
								pagefactory.prePage();
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}						
							if(pagefactory.isfirstPage())return false;
							pagefactory.onDraw(mNextPageCanvas);
						} else {
							try {
								pagefactory.nextPage();
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
							if(pagefactory.islastPage())return false;
							pagefactory.onDraw(mNextPageCanvas);
						}
						mPageWidget.setBitmaps(mCurPageBitmap, mNextPageBitmap);
					}
                 
					ret = mPageWidget.doTouchEvent(e);
					return ret;
				}
				return false;
			}

		});
		/*
		mPageWidget.setOnLongClickListener(new OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View arg0) {
				
				return true;
			}
		});*/
	}
	
	@Override
	 public boolean onCreateOptionsMenu(Menu menu) {
	  // TODO Auto-generated method stub
		super.onCreateOptionsMenu(menu);
		menu.add(0,0,0,"跳页");
		menu.add(0,1,0,"退出");
		return true;
	 }
	
	@Override
	 public boolean onOptionsItemSelected(MenuItem item) {
	  // TODO Auto-generated method stub
		super.onOptionsItemSelected(item);
		  switch(item.getItemId())
		  {
		  case 0:
			  jumpTo();
		      break;
		  case 1:
			  SharedPreferences sp =getSharedPreferences("last_number", Activity.MODE_PRIVATE);
			  sp.edit().putInt("number", pagefactory.getCurrentNumber()).commit();
			  finish();
		      break;
		  }
		  return true;
	 }
	
	
	
	private void jumpTo(){
		final EditText numberEdit = new EditText(QuranActivity.this);
		SharedPreferences sp =getSharedPreferences("page_number", Activity.MODE_PRIVATE);
		final long pageNumber = sp.getLong("number", 1);
		dialog = new AlertDialog.Builder(QuranActivity.this)
		.setTitle("请输入页码")  
		.setIcon(android.R.drawable.ic_dialog_info)  
		.setView(numberEdit)  
		.setNegativeButton("确定", new OnClickListener() {	
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				// TODO Auto-generated method stub
				dialog.dismiss();
				if(!isNumeric(numberEdit.getText().toString()) ||
						Integer.parseInt(numberEdit.getText().toString()) < 1 ||
						Integer.parseInt(numberEdit.getText().toString()) > pageNumber ){
					Toast.makeText(QuranActivity.this, "请输入正确页码!", Toast.LENGTH_SHORT).show();
				}else{
					try {
						pagefactory.redirectToPage(Integer.parseInt(numberEdit.getText().toString()));
					} catch (NumberFormatException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		})  
		.setPositiveButton("取消", new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				// TODO Auto-generated method stub
				dialog.dismiss();
			}
		} )  
		.show();
	}
	
	class GetNumberTask extends AsyncTask<String, String, String>{

		/* (non-Javadoc)
		 * @see android.os.AsyncTask#doInBackground(java.lang.Object[])
		 */
		@Override
		protected String doInBackground(String... arg0) {
			// TODO Auto-generated method stub
			mPageWidget.setEnabled(false);
			long number = 0;
			try {
				number = pagefactory.getPageNumber();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			SharedPreferences sp = QuranActivity.this.getSharedPreferences("page_number", Activity.MODE_PRIVATE);
			sp.edit().putLong("number", number).commit();
			
			return null;
		}
		
		@Override  
	    protected void onPostExecute(String result) {  
			mPageWidget.setEnabled(true); 
	    }  
	}
	
	@SuppressLint("WrongCall")
	public static void refresh(){
		//MotionEvent e = MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 300, 0, 0);
		//mPageWidget.abortAnimation();
		//mPageWidget.calcCornerXY(e.getX(), e.getY());
		pagefactory.onDraw(mCurPageCanvas);
		pagefactory.onDraw(mNextPageCanvas);
		//mPageWidget.setBitmaps(mCurPageBitmap, mNextPageBitmap);
		//mPageWidget.doTouchEvent(e);
		mPageWidget.postInvalidate();
		
	}
	
	public static void setWidgetEnableTrue(){
		mPageWidget.postInvalidate();
		mPageWidget.setEnabled(true); 
	}
	
	public static void setWidgetEnableFalse(){
		mPageWidget.postInvalidate();
		mPageWidget.setEnabled(false); 
	}
	
	public static boolean isNumeric(String str){  
		if(str.equals(""))
			return false;
		  for (int i = str.length();--i>=0;){    
		   if (!Character.isDigit(str.charAt(i))){  
		    return false;  
		   }  
		  }  
		  return true;  
		}  
	
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		 
        if (keyCode == KeyEvent.KEYCODE_BACK
                 && event.getRepeatCount() == 0) {
        	SharedPreferences sp =getSharedPreferences("last_number", Activity.MODE_PRIVATE);
        	sp.edit().putInt("number", pagefactory.getCurrentNumber()).commit();
        	finish();
             return true;
         }
         return super.onKeyDown(keyCode, event);
     }
	
	
	private void initBook(){
		
		File dir = new File(Constants.APP_DIR);
		Log.d("exit", dir.exists() + "");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        if (!(new File(Constants.FILE_PATH).exists())) {
            InputStream is = null;
            is = getResources().openRawResource(R.raw.quran);
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(Constants.FILE_PATH);
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            byte[] buffer = new byte[8192];
            int count = 0;
            try {
                while ((count = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, count);
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            try {
                fos.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            try {
                is.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        
	}
}