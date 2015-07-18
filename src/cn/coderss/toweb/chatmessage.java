package cn.coderss.toweb;

import cn.coderss.util.L;
import cn.coderss.util.Net;
import cn.coderss.util.TimeUtil;

public class chatmessage {
	public String message,time,othername;
	public Net net;
	
	
	//To chat message
	public void ToChatMessage(){
		new Thread((new Runnable() {
			
			@Override
			public void run() {
				//消息时间
				time=TimeUtil.getTime(System.currentTimeMillis());
				toChatMessage(message, time);
				
			}
		}),"ToChatMessage").start();
	}
	
	public boolean toChatMessage(final String message,final String time){
		String result=net.Post("http://uplus.coderss.cn/index.php/ChatMessage/Message", "time="+time+"&from="+user.id+"&to="+othername+"&message="+message);
		L.i("开始执行webservice同步,message:"+message+",from:"+user.id+",to:"+othername+",time:"+time);
		return true;
	} 
}
