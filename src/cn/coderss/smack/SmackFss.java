package cn.coderss.smack;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smackx.packet.DelayInfo;
import org.jivesoftware.smackx.packet.DelayInformation;
import org.jivesoftware.smackx.ping.PingManager;
import org.jivesoftware.smackx.ping.packet.Ping;
import org.jivesoftware.smackx.ping.provider.PingProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.carbons.Carbon;
import org.jivesoftware.smackx.carbons.CarbonManager;
import org.jivesoftware.smackx.filetransfer.FileTransfer;
import org.jivesoftware.smackx.filetransfer.FileTransferListener;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.FileTransferNegotiator;
import org.jivesoftware.smackx.filetransfer.FileTransferRequest;
import org.jivesoftware.smackx.filetransfer.IncomingFileTransfer;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;
import org.jivesoftware.smackx.forward.Forwarded;
import org.jivesoftware.smackx.provider.DelayInfoProvider;
import org.jivesoftware.smackx.provider.DiscoverInfoProvider;
import org.jivesoftware.smackx.receipts.DeliveryReceipt;
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager;
import org.jivesoftware.smackx.receipts.DeliveryReceiptRequest;

import com.boyaa.speech.SpeechController;

import cn.coderss.util.FTP;
import cn.coderss.util.FTP.DownLoadProgressListener;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import cn.coderss.R;
import cn.coderss.activity.ChatActivity;
import cn.coderss.activity.FSSAPI;
import cn.coderss.activity.MainActivity;
import cn.coderss.db.ChatProvider;
import cn.coderss.db.ChatProvider.ChatConstants;
import cn.coderss.db.RosterProvider;
import cn.coderss.db.RosterProvider.RosterConstants;
import cn.coderss.service.FssService;
import cn.coderss.toweb.userfriend;
import cn.coderss.util.CurrentTime;
import cn.coderss.util.FTP.UploadProgressListener;
import cn.coderss.util.L;
import cn.coderss.util.PreferenceConstants;
import cn.coderss.util.PreferenceUtils;
import cn.coderss.util.StatusMode;

public class SmackFss implements Smack {
	public static final String XMPP_IDENTITY_NAME = "fss_Im";
	public static final String XMPP_IDENTITY_TYPE = "phone";
	private static final int PACKET_TIMEOUT = 30000;
	private static FileTransferManager fileTransferManager;
	final static private String[] SEND_OFFLINE_PROJECTION = new String[] {
			ChatConstants._ID, ChatConstants.JID, ChatConstants.MESSAGE,
			ChatConstants.DATE, ChatConstants.PACKET_ID };
	final static private String SEND_OFFLINE_SELECTION = ChatConstants.DIRECTION
			+ " = "
			+ ChatConstants.OUTGOING
			+ " AND "
			+ ChatConstants.DELIVERY_STATUS + " = " + ChatConstants.DS_NEW;

	// 当用户启动该应用时，即启动本应用关健服务
	// 并与界面Activity完成绑定，同时完成xmpp的参数配置
	// 我这里是放在类的静态块里面完成的：
	static {
		registerSmackProviders();
	}

	static void registerSmackProviders() {

		ProviderManager pm = ProviderManager.getInstance();
		// add IQ handling
		pm.addIQProvider("query", "http://jabber.org/protocol/disco#info",
				new DiscoverInfoProvider());
		// add delayed delivery notifications
		pm.addExtensionProvider("delay", "urn:xmpp:delay",
				new DelayInfoProvider());
		pm.addExtensionProvider("x", "jabber:x:delay", new DelayInfoProvider());
		// add carbons and forwarding
		pm.addExtensionProvider("forwarded", Forwarded.NAMESPACE,
				new Forwarded.Provider());
		pm.addExtensionProvider("sent", Carbon.NAMESPACE, new Carbon.Provider());
		pm.addExtensionProvider("received", Carbon.NAMESPACE,
				new Carbon.Provider());
		// add delivery receipts
		pm.addExtensionProvider(DeliveryReceipt.ELEMENT,
				DeliveryReceipt.NAMESPACE, new DeliveryReceipt.Provider());
		pm.addExtensionProvider(DeliveryReceiptRequest.ELEMENT,
				DeliveryReceipt.NAMESPACE,
				new DeliveryReceiptRequest.Provider());
		// add XMPP Ping (XEP-0199)
		pm.addIQProvider("ping", "urn:xmpp:ping", new PingProvider());

		ServiceDiscoveryManager.setIdentityName(XMPP_IDENTITY_NAME);
		ServiceDiscoveryManager.setIdentityType(XMPP_IDENTITY_TYPE);
	}

	private ConnectionConfiguration mXMPPConfig;
	public static XMPPConnection mXMPPConnection;
	private FssService mService;
	private Roster mRoster;
	private FileTransferManager manager = null;
	private final ContentResolver mContentResolver;
	private RosterListener mRosterListener;
	private PacketListener mPacketListener;
	private PacketListener mSendFailureListener;
	private PacketListener mPongListener;
	private File path;
	// ping-pong服务器消息 心跳包
	private String mPingID;
	private long mPingTimestamp;
	private static OutgoingFileTransfer sendTransfer;
	private PendingIntent mPingAlarmPendIntent;
	private PendingIntent mPongTimeoutAlarmPendIntent;
	private static final String PING_ALARM = "cn.coderss.PING_ALARM";
	private static final String PONG_TIMEOUT_ALARM = "cn.coderss.PONG_TIMEOUT_ALARM";
	private Intent mPingAlarmIntent = new Intent(PING_ALARM);
	private Intent mPongTimeoutAlarmIntent = new Intent(PONG_TIMEOUT_ALARM);
	private PongTimeoutAlarmReceiver mPongTimeoutAlarmReceiver = new PongTimeoutAlarmReceiver();
	private BroadcastReceiver mPingAlarmReceiver = new PingAlarmReceiver();

	// ping-pong服务器消息 心跳包
	/**
	 * 分配smackfss对象内存空间时 将配置信息全都设定初始值
	 * 
	 * @param service
	 */
	public SmackFss(FssService service) {
		String customServer = PreferenceUtils.getPrefString(service,
				PreferenceConstants.CUSTOM_SERVER, "");
		int port = PreferenceUtils.getPrefInt(service,
				PreferenceConstants.PORT, PreferenceConstants.DEFAULT_PORT_INT);
		String server = PreferenceUtils.getPrefString(service,
				PreferenceConstants.Server, PreferenceConstants.GMAIL_SERVER);
		boolean smackdebug = PreferenceUtils.getPrefBoolean(service,
				PreferenceConstants.SMACKDEBUG, false);
		boolean requireSsl = PreferenceUtils.getPrefBoolean(service,
				PreferenceConstants.REQUIRE_TLS, false);
		if (customServer.length() > 0
				|| port != PreferenceConstants.DEFAULT_PORT_INT)
			this.mXMPPConfig = new ConnectionConfiguration(customServer, port,
					server);
		else
			this.mXMPPConfig = new ConnectionConfiguration(server); // use SRV

		this.mXMPPConfig.setReconnectionAllowed(false);
		this.mXMPPConfig.setSendPresence(false);
		this.mXMPPConfig.setCompressionEnabled(false); // disable for now
		this.mXMPPConfig.setDebuggerEnabled(smackdebug);
		if (requireSsl)
			this.mXMPPConfig
					.setSecurityMode(ConnectionConfiguration.SecurityMode.required);

		this.mXMPPConnection = new XMPPConnection(mXMPPConfig);
		this.mService = service;
		mContentResolver = service.getContentResolver();
	}

	/*
	 * 当用户输入账号密码时，在服务中开启新线程启动连接服务器， 传入参数信息(服务器、账号密码等)实现登录，
	 * 同时会将登陆成功与否信息通过回调函数通知界面 cn.coderss.smack.Smack#login(java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public boolean login(String account, String password) throws Exception {
		try {
			if (mXMPPConnection.isConnected()) {
				try {
					mXMPPConnection.disconnect();
				} catch (Exception e) {
					L.d("通讯链接失败 " + e);
				}
			}
			SmackConfiguration.setPacketReplyTimeout(PACKET_TIMEOUT);
			SmackConfiguration.setKeepAliveInterval(-1);
			SmackConfiguration.setDefaultPingInterval(0);
			registerRosterListener();// 监听联系人动态变化
			mXMPPConnection.connect();
			if (!mXMPPConnection.isConnected()) {
				throw new Exception("SMACK connect failed without exception!");
			}
			mXMPPConnection.addConnectionListener(new ConnectionListener() {
				public void connectionClosedOnError(Exception e) {
					mService.postConnectionFailed(e.getMessage());
				}

				public void connectionClosed() {
				}

				public void reconnectingIn(int seconds) {
				}

				public void reconnectionFailed(Exception e) {
				}

				public void reconnectionSuccessful() {
				}
			});
			initServiceDiscovery();// 与服务器交互消息监听,发送消息需要回执，判断是否发送成功
			// SMACK auto-logins if we were authenticated before
			if (!mXMPPConnection.isAuthenticated()) {
				String ressource = PreferenceUtils.getPrefString(mService,
						PreferenceConstants.RESSOURCE, "Fss");
				L.i("正在开始登陆操作");
				mXMPPConnection.login(account, password, ressource);
			}
			setStatusFromConfig();// 更新在线状态

		} catch (XMPPException e) {
			throw new Exception(e.getLocalizedMessage(),
					e.getWrappedThrowable());
		} catch (Exception e) {
			// actually we just care for IllegalState or NullPointer or XMPPEx.
			L.e(SmackFss.class, "login(): " + Log.getStackTraceString(e));
			throw new Exception(e.getLocalizedMessage(), e.getCause());
		}
		registerAllListener();// 注册监听其他的事件，比如新消息
		return mXMPPConnection.isAuthenticated();
	}

	/**
	 * 注册联系人 事件体
	 * 
	 */
	public boolean Register(String account, String password) throws Exception {
		try {
			if (mXMPPConnection.isConnected()) {
				try {
					mXMPPConnection.disconnect();
				} catch (Exception e) {
					L.d("通讯链接失败 " + e);
				}
			}
			SmackConfiguration.setPacketReplyTimeout(PACKET_TIMEOUT);
			SmackConfiguration.setKeepAliveInterval(-1);
			SmackConfiguration.setDefaultPingInterval(0);
			registerRosterListener();// 监听联系人动态变化
			mXMPPConnection.connect();
			if (!mXMPPConnection.isConnected()) {
				throw new Exception("SMACK connect failed without exception!");
			}
			mXMPPConnection.addConnectionListener(new ConnectionListener() {
				public void connectionClosedOnError(Exception e) {
					mService.postConnectionFailed(e.getMessage());
				}

				public void connectionClosed() {
				}

				public void reconnectingIn(int seconds) {
				}

				public void reconnectionFailed(Exception e) {
				}

				public void reconnectionSuccessful() {
				}
			});
			initServiceDiscovery();// 与服务器交互消息监听,发送消息需要回执，判断是否发送成功
			// SMACK auto-logins if we were authenticated before
			if (!mXMPPConnection.isAuthenticated()) {
				String ressource = PreferenceUtils.getPrefString(mService,
						PreferenceConstants.RESSOURCE, "Fss");
				L.i("正在开始注册操作");
				org.jivesoftware.smack.AccountManager mAccountManager = mXMPPConnection
						.getAccountManager();
				mAccountManager.createAccount(account, password);
			}
			setStatusFromConfig();// 更新在线状态
			return true;
		} catch (XMPPException e) {
			throw new Exception(e.getLocalizedMessage(),
					e.getWrappedThrowable());

		} catch (Exception e) {
			// actually we just care for IllegalState or NullPointer or XMPPEx.
			L.e(SmackFss.class, "login(): " + Log.getStackTraceString(e));
			throw new Exception(e.getLocalizedMessage(), e.getCause());

		}
	}

	/**
	 * 比较关健，登陆成功后， 我们就必须要监听服务器的各种消息状态变化， 以及要维持自身的一个稳定性，即保持长连接和掉线自动重连。
	 * 
	 */
	private void registerAllListener() {
		// actually, authenticated must be true now, or an exception must have
		// been thrown.
		if (isAuthenticated()) {
			registerMessageListener();// 注册新消息监听
			registerMessageSendFailureListener();// 注册消息发送失败监听
			registerPongListener();// 注册服务器回应ping消息监听
			sendOfflineMessages();// 发送离线消息
			initFileTransport();// 注册文件接收初始化
			if (mService == null) {
				mXMPPConnection.disconnect();
				return;
			}
			// we need to "ping" the service to let it know we are actually
			// connected, even when no roster entries will come in
			mService.rosterChanged();
		}
	}

	/**
	 * 新消息到来处理 注册消息监听，也跟联系人动态监听是一样的处理方式， 将消息的动态变化同步到消息数据库Chat.db，并未直接通知界面，
	 * 界面也是通过监听数据库变化来作出动态变化的
	 */
	/************ start 新消息处理 ********************/
	private void registerMessageListener() {
		// do not register multiple packet listeners
		if (mPacketListener != null)
			mXMPPConnection.removePacketListener(mPacketListener);

		PacketTypeFilter filter = new PacketTypeFilter(Message.class);

		mPacketListener = new PacketListener() {
			public void processPacket(Packet packet) {
				try {
					if (packet instanceof Message) {// 如果是消息类型
						Message msg = (Message) packet;
						String chatMessage = msg.getBody();

						// try to extract a carbon
						Carbon cc = CarbonManager.getCarbon(msg);
						if (cc != null
								&& cc.getDirection() == Carbon.Direction.received) {
							// 收到的消息
							L.d("carbon: " + cc.toXML());
							msg = (Message) cc.getForwarded()
									.getForwardedPacket();
							chatMessage = msg.getBody();
							// fall through
						} else if (cc != null
								&& cc.getDirection() == Carbon.Direction.sent) {
							// 如果是自己发送的消息，则添加到数据库后直接返回
							L.d("carbon: " + cc.toXML());
							msg = (Message) cc.getForwarded()
									.getForwardedPacket();
							chatMessage = msg.getBody();
							if (chatMessage == null)
								return;
							String fromJID = getJabberID(msg.getTo());

							addChatMessageToDB(ChatConstants.OUTGOING, fromJID,
									chatMessage, ChatConstants.DS_SENT_OR_READ,
									System.currentTimeMillis(),
									msg.getPacketID());
							// always return after adding
							return;// 记得要返回
						}

						if (chatMessage == null) {
							// 如果消息为空，直接返回了
							return;
						}

						if (msg.getType() == Message.Type.error) {
							// 错误的消息类型
							chatMessage = "<Error> " + chatMessage;
						}

						// 判断是否是语音文件，是开始开线程ftp协议下载语音文件，不是跳过
						if (chatMessage.length() > 6) {
							String head_str = chatMessage.substring(0, 5);
							if ("yuyin".equals(head_str)) {
								L.i("开始播放");
								final String fileName = chatMessage.substring(
										6, chatMessage.length());
								new Thread(new Runnable() {
									@Override
									public void run() {

										// 下载
										try {

											// 单文件下载
											new FTP()
													.downloadSingleFile(
															"/fss_music/"
																	+ fileName,
															getExternalCacheDir(),
															fileName,
															new DownLoadProgressListener() {

																@Override
																public void onDownLoadProgress(
																		String currentStep,
																		long downProcess,
																		File file) {
																	Log.d(ChatActivity.TAG,
																			currentStep);
																	if (currentStep
																			.equals(ChatActivity.FTP_DOWN_SUCCESS)) {
																		Log.d(ChatActivity.TAG,
																				"-----xiazai--successful");
																	} else if (currentStep
																			.equals(ChatActivity.FTP_DOWN_LOADING)) {
																		Log.d(ChatActivity.TAG,
																				"-----xiazai---"
																						+ downProcess
																						+ "%");
																	}
																}

															});

										} catch (Exception e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}

									}

									private String getExternalCacheDir() {
										// TODO Auto-generated method stub
										return null;
									}
								}).start();

							}
						}

						long ts;// 消息时间戳
						DelayInfo timestamp = (DelayInfo) msg.getExtension(
								"delay", "urn:xmpp:delay");
						if (timestamp == null)
							timestamp = (DelayInfo) msg.getExtension("x",
									"jabber:x:delay");
						if (timestamp != null)
							ts = timestamp.getStamp().getTime();
						else
							ts = System.currentTimeMillis();
						// 消息来自对象
						String fromJID = getJabberID(msg.getFrom());

						addChatMessageToDB(ChatConstants.INCOMING, fromJID,
								chatMessage, ChatConstants.DS_NEW, ts,
								msg.getPacketID());
						// 存入数据库，并标记为新消息DS_NEW
						mService.newMessage(fromJID, chatMessage);
						// 通知service，处理是否需要显示通知栏，
					}
				} catch (Exception e) {
					// SMACK silently discards exceptions dropped from
					// processPacket :(
					L.e("failed to process packet:");
					e.printStackTrace();
				}
			}
		};
		// 这是最关健的了，少了这句，前面的都是无效
		mXMPPConnection.addPacketListener(mPacketListener, filter);
	}

	private void addChatMessageToDB(int direction, String JID, String message,
			int delivery_status, long ts, String packetID) {
		ContentValues values = new ContentValues();

		values.put(ChatConstants.DIRECTION, direction);
		values.put(ChatConstants.JID, JID);
		values.put(ChatConstants.MESSAGE, message);
		values.put(ChatConstants.DELIVERY_STATUS, delivery_status);
		values.put(ChatConstants.DATE, ts);
		values.put(ChatConstants.PACKET_ID, packetID);

		mContentResolver.insert(ChatProvider.CONTENT_URI, values);
	}

	/************ end 新消息处理 ********************/

	/***************** start 处理消息发送失败状态 ***********************/
	private void registerMessageSendFailureListener() {
		// do not register multiple packet listeners
		if (mSendFailureListener != null)
			mXMPPConnection
					.removePacketSendFailureListener(mSendFailureListener);

		PacketTypeFilter filter = new PacketTypeFilter(Message.class);

		mSendFailureListener = new PacketListener() {
			public void processPacket(Packet packet) {
				try {
					if (packet instanceof Message) {
						Message msg = (Message) packet;
						String chatMessage = msg.getBody();

						Log.d("SmackableImp",
								"message "
										+ chatMessage
										+ " could not be sent (ID:"
										+ (msg.getPacketID() == null ? "null"
												: msg.getPacketID()) + ")");
						changeMessageDeliveryStatus(msg.getPacketID(),
								ChatConstants.DS_NEW);
					}
				} catch (Exception e) {
					// SMACK silently discards exceptions dropped from
					// processPacket :(
					L.e("failed to process packet:");
					e.printStackTrace();
				}
			}
		};

		mXMPPConnection.addPacketSendFailureListener(mSendFailureListener,
				filter);
	}

	public void changeMessageDeliveryStatus(String packetID, int new_status) {
		ContentValues cv = new ContentValues();
		cv.put(ChatConstants.DELIVERY_STATUS, new_status);
		Uri rowuri = Uri.parse("content://" + ChatProvider.AUTHORITY + "/"
				+ ChatProvider.TABLE_NAME);
		mContentResolver.update(rowuri, cv, ChatConstants.PACKET_ID
				+ " = ? AND " + ChatConstants.DIRECTION + " = "
				+ ChatConstants.OUTGOING, new String[] { packetID });
	}

	/***************** end 处理消息发送失败状态 ***********************/

	/**
	 * 启动保持长连接任务。我这里与服务器保持长连接，其实是通过每隔一段时间(本应用是15分钟)去ping一次服务器，
	 * 服务器收到此ping消息，会对应的回复一个pong消息，完成一次ping-pong的过程，
	 * 我们暂且叫它为心跳。此ping-pong过程有一个唯一的id，用来区分每一次的ping-pong记录。
	 * 为了保证应用在系统休眠时也能启动ping的任务，我们使用了闹钟服务，而不是定时器， Android中的定时器AlarmManager
	 * 。具体操作是： 从连上服务器完成登录15分钟后，闹钟响起，开始给服务器发送一条ping消息(随机生成一唯一ID)，
	 * 同时启动超时闹钟(本应用是30+3秒)，如果服务器在30+3秒内回复了一条pong消息(与之前发送的ping消息ID相同)，
	 * 代表与服务器任然保持连接，则取消超时闹钟，完成一次ping-pong过程。如果在30+3秒内服务器未响应，
	 * 或者回复的pong消息与之前发送的ping消息ID不一致，则认为与服务器已经断开。 此时，将此消息反馈给界面，同时启动重连任务。实现长连接。
	 */
	/***************** start 处理ping服务器消息 ***********************/
	private void registerPongListener() {
		// reset ping expectation on new connection
		// 初始化ping的id
		mPingID = null;

		if (mPongListener != null)
			// 先移除之前监听对象
			mXMPPConnection.removePacketListener(mPongListener);

		mPongListener = new PacketListener() {

			@Override
			public void processPacket(Packet packet) {
				if (packet == null)
					return;

				// 如果服务器返回的消息为ping服务器时的消息，说明没有掉线
				if (packet.getPacketID().equals(mPingID)) {
					L.i(String.format(
							"Ping: server latency %1.3fs",
							(System.currentTimeMillis() - mPingTimestamp) / 1000.));
					mPingID = null;
					((AlarmManager) mService
							.getSystemService(Context.ALARM_SERVICE))
							.cancel(mPongTimeoutAlarmPendIntent);// 取消超时闹钟
				}
			}

		};

		// 正式开始监听
		mXMPPConnection.addPacketListener(mPongListener, new PacketTypeFilter(
				IQ.class));
		// 定时ping服务器，以此来确定是否掉线
		mPingAlarmPendIntent = PendingIntent.getBroadcast(
				mService.getApplicationContext(), 0, mPingAlarmIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);
		// 超时闹钟
		mPongTimeoutAlarmPendIntent = PendingIntent.getBroadcast(
				mService.getApplicationContext(), 0, mPongTimeoutAlarmIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);
		// 注册定时ping服务器广播接收者
		mService.registerReceiver(mPingAlarmReceiver, new IntentFilter(
				PING_ALARM));
		// 注册连接超时广播接收者
		mService.registerReceiver(mPongTimeoutAlarmReceiver, new IntentFilter(
				PONG_TIMEOUT_ALARM));
		// 15分钟ping一次服务器
		((AlarmManager) mService.getSystemService(Context.ALARM_SERVICE))
				.setInexactRepeating(AlarmManager.RTC_WAKEUP,
						System.currentTimeMillis()
								+ AlarmManager.INTERVAL_FIFTEEN_MINUTES,
						AlarmManager.INTERVAL_FIFTEEN_MINUTES,
						mPingAlarmPendIntent);
	}

	/**
	 * BroadcastReceiver to trigger reconnect on pong timeout.
	 */
	private class PongTimeoutAlarmReceiver extends BroadcastReceiver {
		public void onReceive(Context ctx, Intent i) {
			L.d("Ping: timeout for " + mPingID);
			mService.postConnectionFailed(FssService.PONG_TIMEOUT);
			logout();// 超时就断开连接
		}
	}

	/**
	 * BroadcastReceiver to trigger sending pings to the server
	 */
	private class PingAlarmReceiver extends BroadcastReceiver {
		public void onReceive(Context ctx, Intent i) {
			if (mXMPPConnection.isAuthenticated()) {
				sendServerPing();// 收到ping服务器的闹钟，即ping一下服务器
			} else
				L.d("Ping: alarm received, but not connected to server.");
		}
	}

	/***************** end 处理ping服务器消息 ***********************/

	/***************** start 发送离线消息 ***********************/
	public void sendOfflineMessages() {
		Cursor cursor = mContentResolver.query(ChatProvider.CONTENT_URI,
				SEND_OFFLINE_PROJECTION, SEND_OFFLINE_SELECTION, null, null);
		final int _ID_COL = cursor.getColumnIndexOrThrow(ChatConstants._ID);
		final int JID_COL = cursor.getColumnIndexOrThrow(ChatConstants.JID);
		final int MSG_COL = cursor.getColumnIndexOrThrow(ChatConstants.MESSAGE);
		final int TS_COL = cursor.getColumnIndexOrThrow(ChatConstants.DATE);
		final int PACKETID_COL = cursor
				.getColumnIndexOrThrow(ChatConstants.PACKET_ID);
		ContentValues mark_sent = new ContentValues();
		mark_sent.put(ChatConstants.DELIVERY_STATUS,
				ChatConstants.DS_SENT_OR_READ);
		while (cursor.moveToNext()) {
			int _id = cursor.getInt(_ID_COL);
			String toJID = cursor.getString(JID_COL);
			String message = cursor.getString(MSG_COL);
			String packetID = cursor.getString(PACKETID_COL);
			long ts = cursor.getLong(TS_COL);
			L.d("sendOfflineMessages: " + toJID + " > " + message);
			final Message newMessage = new Message(toJID, Message.Type.chat);
			newMessage.setBody(message);
			DelayInformation delay = new DelayInformation(new Date(ts));
			newMessage.addExtension(delay);
			newMessage.addExtension(new DelayInfo(delay));
			newMessage.addExtension(new DeliveryReceiptRequest());
			if ((packetID != null) && (packetID.length() > 0)) {
				newMessage.setPacketID(packetID);
			} else {
				packetID = newMessage.getPacketID();
				mark_sent.put(ChatConstants.PACKET_ID, packetID);
			}
			Uri rowuri = Uri.parse("content://" + ChatProvider.AUTHORITY + "/"
					+ ChatProvider.TABLE_NAME + "/" + _id);
			mContentResolver.update(rowuri, mark_sent, null, null);
			mXMPPConnection.sendPacket(newMessage); // must be after marking
													// delivered, otherwise it
													// may override the
													// SendFailListener
		}
		cursor.close();
	}

	public static void sendOfflineMessage(ContentResolver cr, String toJID,
			String message) {
		ContentValues values = new ContentValues();
		values.put(ChatConstants.DIRECTION, ChatConstants.OUTGOING);
		values.put(ChatConstants.JID, toJID);
		values.put(ChatConstants.MESSAGE, message);
		values.put(ChatConstants.DELIVERY_STATUS, ChatConstants.DS_NEW);
		values.put(ChatConstants.DATE, System.currentTimeMillis());

		cr.insert(ChatProvider.CONTENT_URI, values);
	}

	/***************** end 发送离线消息 ***********************/

	/**
	 * 这个我主要用来发送文件 用户和文件地址
	 */
	/***************** start 发送文件 ***********************/
	@Override
	public void sendFile(final String toUser, final String filepath) {
		FileTransferManager sendFilemanager = null;
		if (isAuthenticated()) {

			final Message newMessage = new Message(toUser, Message.Type.chat);
			newMessage.setBody("yuyin:" + filepath);
			newMessage.addExtension(new DeliveryReceiptRequest());

			// 判断是否在线
			addChatMessageToDB(ChatConstants.OUTGOING, toUser, "yuyin:"
					+ filepath, ChatConstants.DS_SENT_OR_READ,
					System.currentTimeMillis(),
					String.valueOf(System.currentTimeMillis()));

			// 发送开始线程
			new Thread(new Runnable() {
				@Override
				public void run() {

					// 上传
					File file = new File(filepath);
					try {

						// 单文件上传
						new FTP().uploadSingleFile(file, "/fss_music/",
								new UploadProgressListener() {

									@Override
									public void onUploadProgress(
											String currentStep,
											long uploadSize, File file) {
										// TODO Auto-generated method stub
										Log.d(ChatActivity.TAG, currentStep);
										if (currentStep
												.equals(ChatActivity.FTP_UPLOAD_SUCCESS)) {
											Log.d(ChatActivity.TAG,
													"-----shanchuan--successful");
										} else if (currentStep
												.equals(ChatActivity.FTP_UPLOAD_LOADING)) {
											long fize = file.length();
											float num = (float) uploadSize
													/ (float) fize;
											int result = (int) (num * 100);
											Log.d(ChatActivity.TAG,
													"-----shangchuan---"
															+ result + "%");
										}
									}
								});
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			}).start();

			mXMPPConnection.sendPacket(newMessage);

		} else {
			// send offline -> store to DB
			addChatMessageToDB(ChatConstants.OUTGOING, toUser, "yuyin:"
					+ filepath, ChatConstants.DS_NEW,
					System.currentTimeMillis(),
					String.valueOf(System.currentTimeMillis()));
		}

	}

	/***************** end 发送文件 ***********************/

	/**
	 * 注册联系人动态变化监听：第一次登陆时要同步本地数据库与服务器数据库的联系人，
	 * 同时处理连接过程中联系人动态变化，比如说好友切换在线状态、有人申请加好友等。
	 * 我这里没有将动态变化直接通知到界面线程，而是直接更新联系人数据库Roster.db，
	 * 因为：在界面线程监听了联系人数据库的动态变化，这就是ContentProvider的好处
	 */
	/******************************* start 联系人数据库事件处理 **********************************/
	private void registerRosterListener() {
		mRoster = mXMPPConnection.getRoster();
		mRosterListener = new RosterListener() {
			private boolean isFristRoter;

			@Override
			public void presenceChanged(Presence presence) {
				// 联系人状态改变，比如在线或离开、隐身之类
				L.i("联系人状态改变(" + presence.getFrom() + "): " + presence);
				String jabberID = getJabberID(presence.getFrom());
				RosterEntry rosterEntry = mRoster.getEntry(jabberID);
				updateRosterEntryInDB(rosterEntry);// 更新联系人数据库
				mService.rosterChanged();// 回调通知服务，主要是用来判断一下是否掉线
			}

			@Override
			public void entriesUpdated(Collection<String> entries) {
				// 更新数据库，第一次登陆
				// TODO Auto-generated method stub
				L.i("好友更新操作 entriesUpdated(" + entries + ")");
				for (String entry : entries) {
					RosterEntry rosterEntry = mRoster.getEntry(entry);
					updateRosterEntryInDB(rosterEntry);
				}
				// 回调通知服务，主要是用来判断一下是否掉线
				mService.rosterChanged();
			}

			@Override
			public void entriesDeleted(Collection<String> entries) {
				// 有好友删除时，
				L.i("好友删除操作 entriesDeleted(" + entries + ")");
				for (String entry : entries) {
					deleteRosterEntryFromDB(entry);
				}
				// 回调通知服务，主要是用来判断一下是否掉线
				mService.rosterChanged();
			}

			@Override
			public void entriesAdded(Collection<String> entries) {
				// 有人添加好友时，我这里没有弹出对话框确认，直接添加到数据库
				L.i("好友添加操作 entriesAdded(" + entries + ")");
				ContentValues[] cvs = new ContentValues[entries.size()];
				int i = 0;
				// 同步webservice 联系人数据
				String othername = null;
				int count = 0;
				for (String entry : entries) {
					RosterEntry rosterEntry = mRoster.getEntry(entry);
					cvs[i++] = getContentValuesForRosterEntry(rosterEntry);
					if (count == 0) {
						count++;
						othername = entry.replace("@coderss.cn", "");
					} else {
						othername = entry.replace("@coderss.cn", "") + ","
								+ othername;
					}

				}

				userfriend u = new userfriend();
				u.othername = othername;
				u.ToMyFriend();

				mContentResolver.bulkInsert(RosterProvider.CONTENT_URI, cvs);
				if (isFristRoter) {
					isFristRoter = false;
					// 回调通知服务，主要是用来判断一下是否掉线
					mService.rosterChanged();
				}
			}
		};
		mRoster.addRosterListener(mRosterListener);
	}

	private String getJabberID(String from) {
		String[] res = from.split("/");
		return res[0].toLowerCase();
	}

	private void updateRosterEntryInDB(final RosterEntry entry) {
		final ContentValues values = getContentValuesForRosterEntry(entry);

		if (mContentResolver.update(RosterProvider.CONTENT_URI, values,
				RosterConstants.JID + " = ?", new String[] { entry.getUser() }) == 0)
			addRosterEntryToDB(entry);
	}

	private void addRosterEntryToDB(final RosterEntry entry) {
		ContentValues values = getContentValuesForRosterEntry(entry);
		Uri uri = mContentResolver.insert(RosterProvider.CONTENT_URI, values);
		L.i("addRosterEntryToDB: Inserted " + uri);
	}

	private void deleteRosterEntryFromDB(final String jabberID) {
		int count = mContentResolver.delete(RosterProvider.CONTENT_URI,
				RosterConstants.JID + " = ?", new String[] { jabberID });
		L.i("deleteRosterEntryFromDB: Deleted " + count + " entries");
	}

	private ContentValues getContentValuesForRosterEntry(final RosterEntry entry) {
		final ContentValues values = new ContentValues();

		values.put(RosterConstants.JID, entry.getUser());
		values.put(RosterConstants.ALIAS, getName(entry));

		Presence presence = mRoster.getPresence(entry.getUser());
		values.put(RosterConstants.STATUS_MODE, getStatusInt(presence));
		values.put(RosterConstants.STATUS_MESSAGE, presence.getStatus());
		values.put(RosterConstants.GROUP, getGroup(entry.getGroups()));

		return values;
	}

	private String getGroup(Collection<RosterGroup> groups) {
		for (RosterGroup group : groups) {
			return group.getName();
		}
		return "";
	}

	private String getName(RosterEntry rosterEntry) {
		String name = rosterEntry.getName();
		if (name != null && name.length() > 0) {
			return name;
		}
		name = StringUtils.parseName(rosterEntry.getUser());
		if (name.length() > 0) {
			return name;
		}
		return rosterEntry.getUser();
	}

	private StatusMode getStatus(Presence presence) {
		if (presence.getType() == Presence.Type.available) {
			if (presence.getMode() != null) {
				return StatusMode.valueOf(presence.getMode().name());
			}
			return StatusMode.available;
		}
		return StatusMode.offline;
	}

	private int getStatusInt(final Presence presence) {
		return getStatus(presence).ordinal();
	}

	public void setStatusFromConfig() {
		boolean messageCarbons = PreferenceUtils.getPrefBoolean(mService,
				PreferenceConstants.MESSAGE_CARBONS, true);
		String statusMode = PreferenceUtils.getPrefString(mService,
				PreferenceConstants.STATUS_MODE, PreferenceConstants.AVAILABLE);
		String statusMessage = PreferenceUtils.getPrefString(mService,
				PreferenceConstants.STATUS_MESSAGE,
				mService.getString(R.string.status_online));
		int priority = PreferenceUtils.getPrefInt(mService,
				PreferenceConstants.PRIORITY, 0);
		if (messageCarbons)
			CarbonManager.getInstanceFor(mXMPPConnection).sendCarbonsEnabled(
					true);

		Presence presence = new Presence(Presence.Type.available);
		Mode mode = Mode.valueOf(statusMode);
		presence.setMode(mode);
		presence.setStatus(statusMessage);
		presence.setPriority(priority);
		mXMPPConnection.sendPacket(presence);
	}

	/******************************* end 联系人数据库事件处理 **********************************/

	/**
	 * 与服务器交互消息监听,发送消息需要回执，判断是否发送成功
	 */
	private void initServiceDiscovery() {
		// register connection features
		ServiceDiscoveryManager sdm = ServiceDiscoveryManager
				.getInstanceFor(mXMPPConnection);
		if (sdm == null)
			sdm = new ServiceDiscoveryManager(mXMPPConnection);

		sdm.addFeature("http://jabber.org/protocol/disco#info");

		// reference PingManager, set ping flood protection to 10s
		PingManager.getInstanceFor(mXMPPConnection).setPingMinimumInterval(
				10 * 1000);
		// reference DeliveryReceiptManager, add listener

		DeliveryReceiptManager dm = DeliveryReceiptManager
				.getInstanceFor(mXMPPConnection);
		dm.enableAutoReceipts();
		dm.registerReceiptReceivedListener(new org.jivesoftware.smackx.receipts.DeliveryReceiptManager.ReceiptReceivedListener() {

			@Override
			public void onReceiptReceived(String fromJid, String toJid,
					String receiptId) {
				L.d(SmackFss.class, "got delivery receipt for " + receiptId);
				changeMessageDeliveryStatus(receiptId, ChatConstants.DS_ACKED);
			}
		});
	}

	@Override
	public boolean isAuthenticated() {
		if (mXMPPConnection != null) {
			return (mXMPPConnection.isConnected() && mXMPPConnection
					.isAuthenticated());
		}
		return false;
	}

	@Override
	public void addRosterItem(String user, String alias, String group)
			throws Exception {
		// TODO Auto-generated method stub
		addRosterEntry(user, alias, group);
	}

	private void addRosterEntry(String user, String alias, String group)
			throws Exception {
		mRoster = mXMPPConnection.getRoster();
		try {
			mRoster.createEntry(user, alias, new String[] { group });
		} catch (XMPPException e) {
			throw new Exception(e.getLocalizedMessage());
		}
	}

	@Override
	public void removeRosterItem(String user) throws Exception {
		// TODO Auto-generated method stub
		L.d("removeRosterItem(" + user + ")");

		removeRosterEntry(user);
		mService.rosterChanged();
	}

	private void removeRosterEntry(String user) throws Exception {
		mRoster = mXMPPConnection.getRoster();
		try {
			RosterEntry rosterEntry = mRoster.getEntry(user);

			if (rosterEntry != null) {
				mRoster.removeEntry(rosterEntry);
			}
		} catch (XMPPException e) {
			throw new Exception(e.getLocalizedMessage());
		}
	}

	@Override
	public void renameRosterItem(String user, String newName) throws Exception {
		// TODO Auto-generated method stub
		mRoster = mXMPPConnection.getRoster();
		RosterEntry rosterEntry = mRoster.getEntry(user);

		if (!(newName.length() > 0) || (rosterEntry == null)) {
			throw new Exception("JabberID to rename is invalid!");
		}
		rosterEntry.setName(newName);
	}

	@Override
	public void moveRosterItemToGroup(String user, String group)
			throws Exception {
		// TODO Auto-generated method stub
		tryToMoveRosterEntryToGroup(user, group);
	}

	private void tryToMoveRosterEntryToGroup(String userName, String groupName)
			throws Exception {

		mRoster = mXMPPConnection.getRoster();
		RosterGroup rosterGroup = getRosterGroup(groupName);
		RosterEntry rosterEntry = mRoster.getEntry(userName);

		removeRosterEntryFromGroups(rosterEntry);

		if (groupName.length() == 0)
			return;
		else {
			try {
				rosterGroup.addEntry(rosterEntry);
			} catch (XMPPException e) {
				throw new Exception(e.getLocalizedMessage());
			}
		}
	}

	private void removeRosterEntryFromGroups(RosterEntry rosterEntry)
			throws Exception {
		Collection<RosterGroup> oldGroups = rosterEntry.getGroups();

		for (RosterGroup group : oldGroups) {
			tryToRemoveUserFromGroup(group, rosterEntry);
		}
	}

	private void tryToRemoveUserFromGroup(RosterGroup group,
			RosterEntry rosterEntry) throws Exception {
		try {
			group.removeEntry(rosterEntry);
		} catch (XMPPException e) {
			throw new Exception(e.getLocalizedMessage());
		}
	}

	private RosterGroup getRosterGroup(String groupName) {
		RosterGroup rosterGroup = mRoster.getGroup(groupName);

		// create group if unknown
		if ((groupName.length() > 0) && rosterGroup == null) {
			rosterGroup = mRoster.createGroup(groupName);
		}
		return rosterGroup;

	}

	@Override
	public void renameRosterGroup(String group, String newGroup) {
		// TODO Auto-generated method stub
		L.i("oldgroup=" + group + ", newgroup=" + newGroup);
		mRoster = mXMPPConnection.getRoster();
		RosterGroup groupToRename = mRoster.getGroup(group);
		if (groupToRename == null) {
			return;
		}
		groupToRename.setName(newGroup);
	}

	@Override
	public void requestAuthorizationForRosterItem(String user) {
		// TODO Auto-generated method stub
		Presence response = new Presence(Presence.Type.subscribe);
		response.setTo(user);
		mXMPPConnection.sendPacket(response);
	}

	@Override
	public void addRosterGroup(String group) {
		// TODO Auto-generated method stub
		mRoster = mXMPPConnection.getRoster();
		mRoster.createGroup(group);
	}

	@Override
	public void sendMessage(String toJID, String message) {
		// TODO Auto-generated method stub
		final Message newMessage = new Message(toJID, Message.Type.chat);
		newMessage.setBody(message);
		newMessage.addExtension(new DeliveryReceiptRequest());
		if (isAuthenticated()) {
			addChatMessageToDB(ChatConstants.OUTGOING, toJID, message,
					ChatConstants.DS_SENT_OR_READ, System.currentTimeMillis(),
					newMessage.getPacketID());
			mXMPPConnection.sendPacket(newMessage);
		} else {
			// send offline -> store to DB
			addChatMessageToDB(ChatConstants.OUTGOING, toJID, message,
					ChatConstants.DS_NEW, System.currentTimeMillis(),
					newMessage.getPacketID());
		}
	}

	@Override
	public void sendServerPing() {
		if (mPingID != null) {// 此时说明上一次ping服务器还未回应，直接返回，直到连接超时
			L.d("Ping: requested, but still waiting for " + mPingID);
			return; // a ping is still on its way
		}
		Ping ping = new Ping();
		ping.setType(Type.GET);
		ping.setTo(PreferenceUtils.getPrefString(mService,
				PreferenceConstants.Server, PreferenceConstants.GMAIL_SERVER));
		mPingID = ping.getPacketID();// 此id其实是随机生成，但是唯一的
		mPingTimestamp = System.currentTimeMillis();
		L.d("Ping: sending ping " + mPingID);
		mXMPPConnection.sendPacket(ping);// 发送ping消息

		// register ping timeout handler: PACKET_TIMEOUT(30s) + 3s
		((AlarmManager) mService.getSystemService(Context.ALARM_SERVICE)).set(
				AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
						+ PACKET_TIMEOUT + 3000, mPongTimeoutAlarmPendIntent);
	}// 此时需要启动超时判断的闹钟了，时间间隔为30+3秒

	@Override
	public String getNameForJID(String jid) {
		if (null != this.mRoster.getEntry(jid)
				&& null != this.mRoster.getEntry(jid).getName()
				&& this.mRoster.getEntry(jid).getName().length() > 0) {
			return this.mRoster.getEntry(jid).getName();
		} else {
			return jid;
		}
	}

	@Override
	public boolean logout() {
		L.d("unRegisterCallback()");
		// remove callbacks _before_ tossing old connection
		try {
			mXMPPConnection.getRoster().removeRosterListener(mRosterListener);
			mXMPPConnection.removePacketListener(mPacketListener);
			mXMPPConnection
					.removePacketSendFailureListener(mSendFailureListener);
			mXMPPConnection.removePacketListener(mPongListener);
			((AlarmManager) mService.getSystemService(Context.ALARM_SERVICE))
					.cancel(mPingAlarmPendIntent);
			((AlarmManager) mService.getSystemService(Context.ALARM_SERVICE))
					.cancel(mPongTimeoutAlarmPendIntent);
			mService.unregisterReceiver(mPingAlarmReceiver);
			mService.unregisterReceiver(mPongTimeoutAlarmReceiver);
		} catch (Exception e) {
			// ignore it!
			return false;
		}
		if (mXMPPConnection.isConnected()) {
			// work around SMACK's #%&%# blocking disconnect()
			new Thread() {
				public void run() {
					L.d("shutDown thread started");
					mXMPPConnection.disconnect();
					L.d("shutDown thread finished");
				}
			}.start();
		}
		setStatusOffline();
		this.mService = null;
		return true;
	}

	private void setStatusOffline() {
		ContentValues values = new ContentValues();
		values.put(RosterConstants.STATUS_MODE, StatusMode.offline.ordinal());
		mContentResolver.update(RosterProvider.CONTENT_URI, values, null, null);
	}

	/**
	 * 注册接收文件
	 */

	// 初始化文件接受
	public boolean initFileTransport() {
		if (mXMPPConnection == null) {
			return false;
		} else if (fileTransferManager != null) {
			return true;
		} else {
			// fileTransferManager = new
			// FileTransferManager(connection);//网上的放在这里，这回导致Asmack崩溃
			ServiceDiscoveryManager sdManager = ServiceDiscoveryManager
					.getInstanceFor(mXMPPConnection);
			if (sdManager == null) {
				sdManager = new ServiceDiscoveryManager(mXMPPConnection);
			}
			sdManager.addFeature("http://jabber.org/protocol/disco#info");
			sdManager.addFeature("jabber:iq:privacy");
			FileTransferNegotiator.setServiceEnabled(mXMPPConnection, true);
			fileTransferManager = new FileTransferManager(mXMPPConnection);// 在0.8.1.1版本（之后应该也是如此），正确的应该是放在这里。

			return true;
		}
	}

}
