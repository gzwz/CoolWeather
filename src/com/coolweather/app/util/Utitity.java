package com.coolweather.app.util;

import java.util.Iterator;

import android.text.TextUtils;
import android.text.InputFilter.AllCaps;

import com.coolweather.app.db.CoolWeatherDB;
import com.coolweather.app.model.City;
import com.coolweather.app.model.County;
import com.coolweather.app.model.Province;

/*
 * 解析服务器返回的数据
 */

public class Utitity {

	/*
	 * 省级 
	 */
	public synchronized static boolean handleProvincesResponse(CoolWeatherDB coolWeatherDB,
			String response){
		if (!TextUtils.isEmpty(response)) {
			String [] allProvinces = response.split(",");
			if (allProvinces !=null && allProvinces.length>0) {
				for(String p : allProvinces){
					String[] array = p.split("\\|");
					Province province = new Province();
					province.setProvinceCode(array[0]);
					province.setProvinceName(array[1]);
					//将解析出的数据存储到Province表
					coolWeatherDB.saveProvince(province);
					
				}
				return true;
			}
		}
		 return false;
	}
	
	/*
	 * 解析 储存服务器返回的 市 级数据
	 */
	public static boolean handleCitiesResponse(CoolWeatherDB coolWeatherDB,
			String response,int provinceId){
		if (!TextUtils.isEmpty(response)) {
			String[] allCities = response.split(",");
			if (allCities != null && allCities.length > 0) {
				for (String c : allCities) {
					String[] array = c.split("\\|");
					City city = new City();
					city.setCityCode(array[0]);
					city.setCityName(array[1]);
					city.setProvinceId(provinceId);
					coolWeatherDB.saveCity(city);
				}
				return true;
			}
			
		}
		return false;
	}
	
	/*
	 * 解析 储存服务器返回的 县 级数据
	 */
	public static boolean handleCountiesResponse(CoolWeatherDB coolWeatherDB,String response,int cityId){
		if (!TextUtils.isEmpty(response)) {
			String[] allCounties = response.split(",");
			if (allCounties != null && allCounties.length >0) {
				for (String c : allCounties) {
					String [] array= c.split("\\|");
					County county = new County();
					county.setCityId(cityId);
					county.setCountyCode(array[0]);
					county.setCountyName(array[1]);
					coolWeatherDB.saveCounty(county);
				}
			}
		}
		return false;
	}
	
	
	
	
	
}
