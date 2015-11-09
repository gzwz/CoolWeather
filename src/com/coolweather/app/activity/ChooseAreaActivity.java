package com.coolweather.app.activity;

import java.util.ArrayList;
import java.util.List;

import com.coolweather.app.R;
import com.coolweather.app.db.CoolWeatherDB;
import com.coolweather.app.model.City;
import com.coolweather.app.model.County;
import com.coolweather.app.model.Province;
import com.coolweather.app.util.HttpCallbackListener;
import com.coolweather.app.util.HttpUtil;
import com.coolweather.app.util.Utitity;

import android.R.anim;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ChooseAreaActivity extends Activity {
	public static final int LEVEL_PROVINCE = 0;
	public static final int LEVEL_CITY = 1;
	public static final int LEVEL_COUNTY = 2;
	
	private ProgressDialog progressDialog;
	private TextView titleText;
	private ListView listView;
	private ArrayAdapter<String> adapter;
	private CoolWeatherDB coolWeatherDB;
	private List<String> dataList = new ArrayList<String>();
	/*
	 * 是否从WeatherActivity 中跳转过来
	 */
	private boolean isFromWeatherActivity;
	/*
	 * 省
	 */
	private List<Province> provinceList;
	/*
	 * 市
	 */
	private List<City> cityList;
	/*
	 * 县
	 */
	private List<County> countyList;
	/*
	 * 选中的省份
	 */
	private Province selectedProvince;
	/*
	 * 选中的城市
	 */
	private City selectedCity;
	/*
	 * 选中的级别
	 */
	private int currentLevel;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		isFromWeatherActivity = getIntent().getBooleanExtra("from_weather_activity", false);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		//已经选择了城市且不是冲WeatherActivity跳转过来，才会字节跳转到WeatherActivity
		
		if (prefs.getBoolean("city_selected", false) && !isFromWeatherActivity) {
			Intent intent = new Intent(this,WeatherActivity.class);
			startActivity(intent);
			finish();
			return;
		}
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.choose_area);
		listView = (ListView)findViewById(R.id.list_view);
		titleText = (TextView)findViewById(R.id.title_text);
		adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,dataList);
		listView.setAdapter(adapter);
		coolWeatherDB = CoolWeatherDB.getInstance(this);
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int index, long id) {
				// TODO Auto-generated method stub
				if (currentLevel == LEVEL_PROVINCE) {
					selectedProvince = provinceList.get(index);
					queryCities();
				}else if (currentLevel == LEVEL_CITY) {
					selectedCity = cityList.get(index);
					queryCounties();
				}else if (currentLevel == LEVEL_COUNTY) {
					String countyCode = countyList.get(index).getCountyCode();
					Intent intent = new Intent(ChooseAreaActivity.this,WeatherActivity.class);
					intent.putExtra("county_code", countyCode);
					startActivity(intent);
					finish();
				}
			}
		});
		queryProvinces();
	}
	
	/*
	 * 查询全国所有的省，优先从数据库查询，如果没有，再到服务器上去查询
	 */
	private void queryProvinces(){
		provinceList = coolWeatherDB.loadProvinces();
		if (provinceList.size() > 0 ) {
			dataList.clear();
			for (Province province : provinceList) {
				dataList.add(province.getProvinceName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText("中国");
			currentLevel = LEVEL_PROVINCE;
		}else {
			queryFromServer(null, "province");
		}
	}
	
	/*
	 * 查询选中的省内的所有城市，优先从数据库查询，若没有，再到服务器去查询
	 */
	private void queryCities(){
		cityList = coolWeatherDB.loadCities(selectedProvince.getId());
		if (cityList.size() > 0 ) {
			dataList.clear();
			for (City city : cityList) {
				dataList.add(city.getCityName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedProvince.getProvinceName());
			currentLevel = LEVEL_CITY;
		}else {
			queryFromServer(selectedProvince.getProvinceCode(), "city");
		}
		
	}
	
	/*
	 * 查询选中的城市内的所有县份，优先数据库，再次服务器
	 */
	private void queryCounties(){
		countyList = coolWeatherDB.loadCounties(selectedCity.getId());
		if (countyList.size() >0) {
			dataList.clear();
			for (County county : countyList) {
				dataList.add(county.getCountyName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedCity.getCityName());
			currentLevel = LEVEL_COUNTY;
		}else {
			queryFromServer(selectedCity.getCityCode(), "county");
		}
		
	}
	
	/*
	 * 根据传入代号和类型从服务器上查询省市县数据
	 */
	private void queryFromServer(final String code, final String type){
		String address;
		if (!TextUtils.isEmpty(code)) {
			address = "http://www.weather.com.cn/data/list3/city"+code+".xml";
			
		}else {
			address = "http://www.weather.com.cn/data/list3/city.xml";
		}
		showProgressDialog();
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
			
			@Override
			public void onFinish(String response) {
				// TODO Auto-generated method stub
				boolean result = false;
				if ("province".equals(type)) {
					result = Utitity.handleProvincesResponse(coolWeatherDB, response);
					
				}else if ("city".equals(type)) {
					result = Utitity.handleCitiesResponse(coolWeatherDB, response, selectedProvince.getId());
					
				}else if ("county".equals(type)) {
					result = Utitity.handleCountiesResponse(coolWeatherDB, response, selectedCity.getId());
				}
				if (result) {
					//通过runONUiThread()方法返回到主线程处理逻辑
					runOnUiThread(new Runnable() {
						public void run() {
							closeProgressDialog();
							if ("province".equals(type)) {
								queryProvinces();
							}else if ("city".equals(type)) {
								queryCities();
							}else if ("county".equals(type)) {
								queryCounties();
							}
						}
					});
				}
			}
			
			@Override
			public void onError(Exception e) {
				// 通过runonUiThread（）方法返回到主线程处理逻辑
				runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						// TODO Auto-generated method stub
					closeProgressDialog();
					Toast.makeText(ChooseAreaActivity.this, "加载失败！", Toast.LENGTH_SHORT).show();
					}
				});
			}
		});
	}
	
	private void showProgressDialog(){
		if (progressDialog == null) {
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("正在加载...");
			progressDialog.setCanceledOnTouchOutside(false);
		}
		progressDialog.show();
	}
	private void closeProgressDialog(){
		if (progressDialog != null) {
			progressDialog.dismiss();
		}
	}
	
	/*
	 * 捕获Back按键，
	 */
	public void onBackPressed(){
		if (currentLevel == LEVEL_COUNTY) {
			queryCities();
		}else if (currentLevel == LEVEL_CITY) {
			queryProvinces();
		}else {
			if (isFromWeatherActivity) {
				Intent intent = new Intent(this,WeatherActivity.class);
				startActivity(intent);
			}
			finish();
		}
	}
	
}









