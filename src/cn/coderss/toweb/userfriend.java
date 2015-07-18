package cn.coderss.toweb;

import cn.coderss.util.L;
import cn.coderss.util.Net;
import cn.coderss.util.TimeUtil;
import android.content.Context;

public class userfriend {
	public String othername,time;
	public int userid;
	public Context mcontext;
	private Net net;
	
	public userfriend(Context context) {
		super();
		this.mcontext=context;
	}
	
	public userfriend() {
		super();
		// TODO Auto-generated constructor stub
	}

	//To my friend
	public void ToMyFriend(){
		new Thread((new Runnable() {
			
			@Override
			public void run() {
				//注册时间
				time=TimeUtil.getTime(System.currentTimeMillis());
				toMyFriend(othername,time);
			}
		}),"ToLoginUser").start();
	}
	
	public boolean toMyFriend(final String othername,final String time){
		String result=net.Post("http://chat.coderss.cn/index.php/UserFriend/Friend", "time="+time+"&userid="+user.id+"&othername="+othername);
		L.i("开始执行webservice同步,othername:"+othername+",userid:"+user.id+",time:"+time);
		return true;
	} 
	
}
