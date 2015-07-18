package cn.coderss.toweb;

import org.apache.http.conn.params.ConnConnectionPNames;

import android.R.integer;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.sax.StartElementListener;
import android.text.format.Formatter;
import cn.coderss.util.*;

public class user {
	private Net net;
	private Context mcontext;
	public static int id;
	public String username = "test", password = "test", login_time,
			register_time, avator, address_x, address_y, ip;

	public user(Context context) {
		super();
		this.mcontext = context;
	}

	// To register user
	public void ToRegisterUser() {
		new Thread((new Runnable() {
			public void run() {
				// 取出ip地址
				WifiManager wifiManager = (WifiManager) mcontext.getSystemService(mcontext.WIFI_SERVICE);
				WifiInfo info = wifiManager.getConnectionInfo();
				ip = Formatter.formatIpAddress(info.getIpAddress());
				// 注册时间
				register_time = TimeUtil.getTime(System.currentTimeMillis());
				toRegisterUser(username, password, register_time, ip);
			}
		}), "ToRegisterUser").start();
	}

	public boolean toRegisterUser(final String username, final String password,
			final String register_time, final String register_ip) {
		L.i("开始执行webservice同步,username" + username + ",password:" + password
				+ ",register_time:" + register_time + ",ip:" + ip);
		String result = net.Post(
				"http://uplus.coderss.cn/index.php/User/toUser", "username="
						+ username + "&password=" + password + "&time="
						+ register_time + "&ip=" + register_ip);
		user.id = Integer.parseInt(result);
		return true;
	}

	// To login user
	public void ToLoginUser() {
		new Thread((new Runnable() {

			@Override
			public void run() {
				// 注册时间
				login_time = TimeUtil.getTime(System.currentTimeMillis());
				toLoginUser(username, password, login_time);
			}
		}), "ToLoginUser").start();
	}

	public boolean toLoginUser(final String username,final String password,final String login_time){
		String result=net.Post("http://uplus.coderss.cn/index.php/User/LoginUser", "time="+login_time+"&id="+user.id+"&username="+username+"&password="+password);
		L.i("开始执行webservice同步,login_time:"+login_time);
		if(result==""){
			result="10000";
		}
		user.id=Integer.parseInt(result);
		return true;
	}

	// To Address Gps
	public void ToGps() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				// 同步gps
				toGps(address_x, address_y);

			}
		}, "ToGps").start();
	}

	public boolean toGps(final String x, final String y) {
		String result = net.Post(
				"http://uplus.coderss.cn/index.php/User/ToGps", "x=" + x
						+ "&y=" + y + "&id=" + user.id);
		L.i("开始执行webservice同步,x:" + x + ",y:" + y);
		return true;
	}

}
