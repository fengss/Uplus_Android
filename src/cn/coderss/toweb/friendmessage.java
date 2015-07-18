package cn.coderss.toweb;

import cn.coderss.util.L;
import cn.coderss.util.Net;
import cn.coderss.util.TimeUtil;

public class friendmessage {
	public String message,time,image,username;
	public Net net;
	
	
	//To friend message
	public void ToFriendMessage(){
		new Thread((new Runnable() {
			
			@Override
			public void run() {
				//消息时间
				time=TimeUtil.getTime(System.currentTimeMillis());
				toFriendMessage(message, time);
				
			}
		}),"ToFriendMessage").start();
	}
	
	public boolean toFriendMessage(final String message,final String time){
		L.i("url:http://uplus.coderss.cn/index.php?m=FriendMessage&a=Message&time="+time+"&userid="+user.id+"&image="+image+"&message="+message);
		String result=net.Post("http://uplus.coderss.cn/index.php/FriendMessage/Message", "time="+time+"&userid="+user.id+"&image="+image+"&message="+message);
		L.i("开始执行webservice同步,message:"+message+",userid:"+user.id+",time:"+time+",image:"+image);
		return true;
	} 
}
