package cn.coderss.activity;

import cn.coderss.util.L;

import com.baidu.android.bbalbs.common.a.c;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BaiduMapOptions;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.model.LatLng;
import com.baidu.location.GeofenceClient;
import com.baidu.location.LocationClient;

import cn.coderss.toweb.*;
import cn.coderss.baidulbs.RoutePlan;
import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.widget.TextView;

public class FSSAPI extends Application {
	/**
	 * 定位相关
	 */
	public LocationClient mLocationClient;
	public GeofenceClient mGeofenceClient;
	public MyLocationListener mMyLocationListener;
	public TextView mLocationResult,logMsg;
	public TextView trigger,exit;
	public Vibrator mVibrator;
	//to web service
	public user user;
	public userfriend userfriend;
	public chatmessage chatmessage;
	/**
	 * 经度，纬度坐标
	 */
	public float api_x,api_y;

	
	@Override
	public void onCreate() {
		super.onCreate();
		mLocationClient = new LocationClient(this.getApplicationContext());
		mMyLocationListener = new MyLocationListener();
		mLocationClient.registerLocationListener(mMyLocationListener);
		mGeofenceClient = new GeofenceClient(getApplicationContext());
		
		/**
		 * to web service 初始化
		 */
		user=new user(getApplicationContext());
		userfriend=new userfriend(getApplicationContext());
		chatmessage=new chatmessage();
		
		mVibrator =(Vibrator)getApplicationContext().getSystemService(Service.VIBRATOR_SERVICE);
		// 在使用 SDK 各组间之前初始化 context 信息，传入 ApplicationContext
		SDKInitializer.initialize(this);
	}
	
	
	/**
	 * 经纬度计算
	 * @author shenwei
	 *
	 */
	public class MyLocationListener implements BDLocationListener {

		@Override
		public void onReceiveLocation(BDLocation location) {
			//Receive Location 
			api_y=(float) location.getLatitude();//纬度
			api_x=(float) location.getLongitude();//经度
			
			
			
			StringBuffer sb = new StringBuffer(256);
			sb.append("时间点 : ");
			sb.append(location.getTime());
			sb.append("\n代号 : ");
			sb.append(location.getLocType());
			sb.append("\n纬度 : ");
			sb.append(location.getLatitude());
			sb.append("\n经度 : ");
			sb.append(location.getLongitude());
			sb.append("\n半径 : ");
			sb.append(location.getRadius());
			if (location.getLocType() == BDLocation.TypeGpsLocation){
				sb.append("\nspeed : ");
				sb.append(location.getSpeed());
				sb.append("\nsatellite : ");
				sb.append(location.getSatelliteNumber());
				sb.append("\ndirection : ");
				sb.append("\naddr : ");
				sb.append(location.getAddrStr());
				sb.append(location.getDirection());
			} else if (location.getLocType() == BDLocation.TypeNetWorkLocation){
				sb.append("\naddr : ");
				sb.append(location.getAddrStr());
				sb.append("\noperationers : ");
				sb.append(location.getOperators());
			}
			L.i(sb.toString());
			L.i("fss_im lbs:", sb.toString());
		}

		
	}
	
	/**
	 **************** 注册 xmpp服务器用户的时候与webservice同步mysql数据库
	 ****************
	 */ 
	
	/* 注册模块
	 * @param username
	 * @param password
	 */
	public void ToWebRegisterUser(String username,String password){
		user.username=username;
		user.password=password;
		user.ToRegisterUser();
	}
	
	/**
	 * 登陆模块
	 */
	public void ToWebLoginUser(String username,String password){
		user.username=username;
		user.password=password;
		user.ToLoginUser();
	}
	
	/**
	 * gps模块
	 */
	public void ToWebGps(String x,String y){
		user.address_x=x;
		user.address_y=y;
		user.ToGps();
	}
	
	/**
	 * 用户朋友数据mysql同步
	 */
	public void ToMyFriend(String name){
		userfriend.othername=name;
		userfriend.ToMyFriend();
	}
	
	/**
	 * 聊天数据mysql同步
	 */
	public void ToChatMessage(String message,String to){
		chatmessage.message=message;
		chatmessage.othername=to;
		chatmessage.ToChatMessage();
	}
}
