package cn.coderss.test;

import java.util.Collection;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;

import cn.coderss.R;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;


public class Fss_Smack {
	private static final String TAG = "Fss_Smack";
	//连接参数
	private static ConnectionConfiguration CONFIG=new ConnectionConfiguration("www.coderss.cn",5222);;
	//连接对象
	private static XMPPConnection CON=new XMPPConnection(CONFIG);;
	//状态配置信息
	private static Presence mPresence;
	//联系人
	private static Roster roster;
	//聊天包裹ID
	private static String PacketId=new String("1");
	//Chat类接收包
	private static ChatManager chat;
	//Chat类发送包
	private static ChatManager Tochat;
	//消息发送包
	private static Message newMessage;
	//handler
	private static MyHandler mhandler;
	//获得发来的content内容
	private static String content="";
	//context
	private static Context mcontext;
	
	/*
	 public static void main(String[] args) {
		Fss_Smack s=new Fss_Smack();
		s.Login("fengss", "xuanxuan");
		s.getRoster(1);
		s.getMessage();
	
	}
	*/
	
	public Fss_Smack(Context context) {
		try {
			CON.connect();
		} catch (XMPPException e) {
			e.printStackTrace();
			System.out.println("连接出错");
		}
		MyHandler mhandler=new MyHandler();
		mhandler.sendEmptyMessage(1);
		this.mcontext=context;
	}
	
	public Fss_Smack() {
		try {
			CON.connect();
		} catch (XMPPException e) {
			e.printStackTrace();
			System.out.println("连接出错");
		}
	}

	/**	
	 * 
	 * 创建用户
	 * @param username
	 * @param password
	 */
	public static void createUser(String username,String password){
		try {
			AccountManager manager=CON.getAccountManager();
			manager.createAccount(username, password);
		} catch (XMPPException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * 断开重新连接服务器
	 */
	public static void Connection(){
		if(!CON.isConnected()||!CON.isAuthenticated()){
			try {
				CON.connect();
			} catch (XMPPException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	/**
	 * 用户登陆
	 * @param username
	 * @param password
	 */
	public static void Login(String username,String password){
		try {
			CON.login(username, password);
			
		} catch (XMPPException e) {
			System.out.println("登陆出错");
			e.printStackTrace();
		}
		
	}
	
	/**
	 * 发送用户的配置信息
	 * @param Type
	 */
	public static void sendConfig(String Type){
		/**
	  	available: 表示处于在线状态
		unavailable: 表示处于离线状态
		subscribe: 表示发出添加好友的申请
		unsubscribe: 表示发出删除好友的申请
		unsubscribed: 表示拒绝添加对方为好友
		error: 表示presence信息报中包含了一个错误消息。
		*/
		switch (Type) {
		case "available":
			mPresence=new Presence(Presence.Type.available);
			break;
		case "unavailable":
			mPresence=new Presence(Presence.Type.unavailable);
			break;
		case "subscribe":
			mPresence=new Presence(Presence.Type.subscribe);
			break;
		case "unsubscribe":
			mPresence=new Presence(Presence.Type.unsubscribe);
			break;
		case "unsubscribed":
			mPresence=new Presence(Presence.Type.unsubscribed);
			break;
		default:
			break;
		}
	}

	/**
	 * 获取联系人
	 * 以及他们的信息
	 * 1:获取状态
	 * 2:获取姓名
	 * @param Type
	 */
	public static void getRoster(int Type){
		roster=CON.getRoster();
		//获取联系人列表的集合
		Collection<RosterEntry> coll=roster.getEntries();
		for (RosterEntry rosterentry:coll) {
			System.out.println("====联系人状态:===="+rosterentry.getUser());
			switch (Type) {
			case 1:
				System.out.println("====联系人状态:===="+rosterentry.getStatus());
				break;
			case 2:
				System.out.println("====联系人姓名:===="+rosterentry.getName());
				break;

			default:
				break;
			}
		}
	}
	
	/**
	 * 添加联系人
	 * @param username 用户名
	 * @param nickname 昵称
	 * @param groupname 组名 字符串数组
	 */
	public static void addFriend(String username,String nickname,String[] groupname){
		roster=CON.getRoster();
		try {
			roster.createEntry(username+"@coderss.cn", nickname, groupname);
		} catch (XMPPException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * 删除联系人
	 * @param username 用户名
	 */
	public static void deleteFriend(String username){
		roster=CON.getRoster();
		try {
			roster.removeEntry(roster.getEntry(username));
		} catch (XMPPException e) {
			e.printStackTrace();
		}
		
	}
	
	public void getMessage(){
		new MyThread().start();
	}
	
	class MyThread extends Thread{
		@Override
		public synchronized void run() {
			int i=0;
			while(i<10000){
				
				CON.getChatManager().addChatListener(new ChatManagerListener() {
					
					@Override
					public void  chatCreated(Chat mchat, boolean createLocally) {
						//检测消息是否本地发出
						if(!createLocally){
							mchat.addMessageListener(new MessageListener() {						
								@Override
								public void processMessage(Chat mChat, Message mMessage) {
									//避免消息重复
									if(!PacketId.equals(mMessage.getPacketID())&&mMessage.getBody()!=null){
										PacketId=mMessage.getPacketID();
										System.out.println("接受到的信息是:"+mMessage.getBody());
										new MyHandler().sendEmptyMessage(1);
										Fss_Smack.content="接受到的信息是:"+mMessage.getBody()+"\n";
									}
								}
							});
						}
					}
				});
				try {
					Thread.sleep(1000);
					i++;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		
		}
	};
	
	/**
	 * 接收和发送聊天信息
	 * @param content 聊天内容
	 */
	public static void sendMessage(String content){
		newMessage=new Message();
		newMessage.setBody(content);
		
		Tochat=CON.getChatManager();
		Tochat.createChat("newuser@coderss.cn", new MessageListener() {
			
			@Override
			public void processMessage(Chat mchat, Message mMessage) {
				try {
					mchat.sendMessage(newMessage);
				} catch (XMPPException e) {
					e.printStackTrace();
				}
			}
		});	
	}

	
	class MyHandler extends Handler{
		@Override
		public void handleMessage(android.os.Message msg) {
			if(msg.what==1){
				System.out.println("进入handler");
			}
			System.out.println(content+" test");
			LayoutInflater li=LayoutInflater.from(mcontext);
			View view=li.inflate(R.layout.test, null);
			TextView t1=(TextView) view.findViewById(R.id.textView1);
			t1.setText(content);
			super.handleMessage(msg);
		}
	}
}
