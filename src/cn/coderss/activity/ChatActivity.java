package cn.coderss.activity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.filetransfer.FileTransferListener;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.FileTransferRequest;
import org.jivesoftware.smackx.filetransfer.IncomingFileTransfer;

import com.boyaa.speech.SpeechController;
import com.boyaa.speech.SpeechListener;

import cn.coderss.adapter.ChatAdapter;
import cn.coderss.db.ChatProvider;
import cn.coderss.db.ChatProvider.ChatConstants;
import cn.coderss.util.T;
import cn.coderss.R;
import cn.coderss.quickAction.SelectPicPopupWindow;
import cn.coderss.service.FssService;
import cn.coderss.service.IConnectionStatusCallback;
import cn.coderss.activity.ChatActivity;
import cn.coderss.service.FssService;
import cn.coderss.smack.SmackFss;
import cn.coderss.util.CurrentTime;
import cn.coderss.util.L;
import cn.coderss.util.Net;
import cn.coderss.util.PreferenceConstants;
import cn.coderss.util.PreferenceUtils;
import cn.coderss.util.TimeUtil;
import cn.coderss.util.UploadImage;
import cn.coderss.xlistview.MsgListView;
import cn.coderss.xlistview.MsgListView.IXListViewListener;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.opengl.Visibility;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.DialogPreference;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

@SuppressLint("NewApi")
public class ChatActivity extends ActionBarActivity implements OnTouchListener,
		IConnectionStatusCallback, OnClickListener, TextWatcher,
		OnLongClickListener, IXListViewListener {

	public static final String INTENT_EXTRA_USERNAME = ChatActivity.class
			.getName() + ".username";// 昵称对应的key

	public static final String TAG = "ChatActivity";

	public static final String FTP_CONNECT_SUCCESSS = "ftp连接成功";
	public static final String FTP_CONNECT_FAIL = "ftp连接失败";
	public static final String FTP_DISCONNECT_SUCCESS = "ftp断开连接";
	public static final String FTP_FILE_NOTEXISTS = "ftp上文件不存在";
	public static final String FTP_UPLOAD_SUCCESS = "ftp文件上传成功";
	public static final String FTP_UPLOAD_FAIL = "ftp文件上传失败";
	public static final String FTP_UPLOAD_LOADING = "ftp文件正在上传";
	public static final String FTP_DOWN_LOADING = "ftp文件正在下载";
	public static final String FTP_DOWN_SUCCESS = "ftp文件下载成功";
	public static final String FTP_DOWN_FAIL = "ftp文件下载失败";
	public static final String FTP_DELETEFILE_SUCCESS = "ftp文件删除成功";
	public static final String FTP_DELETEFILE_FAIL = "ftp文件删除失败";
	private RelativeLayout bottom_line, msg_view;
	private int popHeight;
	private View pop_chat_tool_View;
	private FssService mService;
	private String mWithId = null;// 当前聊天用户的ID
	private MsgListView mMsgListView;
	public UploadImage upimage;
	public Bitmap uploadpic=null;
	private Button Send, Speak;
	private ImageButton ImageSpeak, ImageBtnKeybox, btn_take_picture;
	private ImageView btn_chat_tool, btn_emotion;
	private PopupWindow popWindow;
	public ProgressDialog dialog;
	public ChatImgUp chatimgupload;
	private EditText ChatEdit;
	private boolean isLongClick = false;
	private FileTransferManager manager = null;
	private File path;
	private ChatImgUp chat_image;
	// 语音文件保存路径
	public static String fileName = null;
	// speex录音转码
	private SpeechController mSpeechController;
	// 音频录制完成度
	private static int mSampleRate = 8000;

	private static final String[] PROJECTION_FROM = new String[] {
			ChatProvider.ChatConstants._ID, ChatProvider.ChatConstants.DATE,
			ChatProvider.ChatConstants.DIRECTION,
			ChatProvider.ChatConstants.JID, ChatProvider.ChatConstants.MESSAGE,
			ChatProvider.ChatConstants.DELIVERY_STATUS };// 查询字段

	/**
	 * 解绑服务
	 */
	private void unbindFssService() {
		try {
			unbindService(mServiceConnection);
		} catch (IllegalArgumentException e) {
			L.e("Service wasn't bound!");
		}
	}

	/**
	 * 绑定服务
	 */
	private void bindFssService() {
		Intent mServiceIntent = new Intent(this, FssService.class);
		Uri chatURI = Uri.parse(mWithId);
		mServiceIntent.setData(chatURI);
		bindService(mServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
	}

	ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mService = ((FssService.FssBinder) service).getService();
			mService.registerConnectionStatusCallback(ChatActivity.this);
			// 如果没有连接上，则重新连接xmpp服务器
			if (!mService.isAuthenticated()) {
				String usr = PreferenceUtils.getPrefString(ChatActivity.this,
						PreferenceConstants.ACCOUNT, "");
				String password = PreferenceUtils.getPrefString(
						ChatActivity.this, PreferenceConstants.PASSWORD, "");
				mService.Login(usr, password);
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mService.unRegisterConnectionStatusCallback();
			mService = null;
		}

	};

	// 状态回调
	/**
	 * 主要是，在线，离线等状态的回调
	 */
	@Override
	public void connectionStatusChanged(int connectedState, String reason) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chat);
		// 语音speek转码
		initSpeek();

		setActionBar();
		// 寻得当前jid
		mWithId = getIntent().getDataString().toLowerCase();
		bindFssService();
		initView();
		// 聊天adapter
		setChatWindowAdapter();

	}

	/**
	 * 语音转码
	 */
	private void initSpeek() {
		mSpeechController = SpeechController.getInstance();
		mSpeechController.setDebug(true);
		mSpeechController.setRecordingMaxTime(10);
		mSpeechController.setSpeechListener(new SpeechListener() {

			@Override
			public void timeConsuming(int type, int secondCount, Object tag) {
				Log.d("CDH", "SpeechListener secondCount:" + secondCount);
				if (type == SpeechListener.RECORDING) {
					L.i("正在录制" + secondCount + "....");
				} else if (type == SpeechListener.PLAYING) {
					L.i("开始播放" + secondCount + "....");
				}
			}

			@Override
			public void recordOver(int sampleRate) {
				L.i("Speex完成编码", "recordOver(" + sampleRate + ")");
				mSampleRate = sampleRate;
			}

			@Override
			public void playOver(Object tag) {
				L.i("Speex完成解码", "playOver(" + tag + ")");

			}

			@Override
			public void recordingVolume(int volume) {
				L.i("Speex正在编码", "recordVolume(" + volume + ")");
			}

		});

	}

	// 设置actionBar
	private void setActionBar() {
		ActionBar actionbar = getSupportActionBar();
		actionbar.setDisplayUseLogoEnabled(false);
		actionbar.setDisplayShowHomeEnabled(false);
		actionbar.setTitle("返回");
		actionbar.setDisplayHomeAsUpEnabled(true);

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		int id = item.getItemId();
		switch (id) {
		case android.R.id.home:
			finish();
			break;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	// ui元素初始化
	private void initView() {

		mMsgListView = (MsgListView) this.findViewById(R.id.msg_listView);
		Send = (Button) this.findViewById(R.id.msglist_input_send);
		// 语音
		Speak = (Button) this.findViewById(R.id.btnSpeak);
		// 切换语音
		ImageSpeak = (ImageButton) this.findViewById(R.id.imgBtnSpeak);
		ChatEdit = (EditText) this.findViewById(R.id.msglist_input_chatmsg);
		// 键盘
		ImageBtnKeybox = (ImageButton) this.findViewById(R.id.imgBtnKeybox);
		// 工具栏
		btn_chat_tool = (ImageView) this.findViewById(R.id.chat_tool);
		// 表情
		btn_emotion = (ImageView) this.findViewById(R.id.chat_expression);
		// 聊天布局
		bottom_line = (RelativeLayout) findViewById(R.id.inputBar);
		// 工具栏
		btn_chat_tool = (ImageView) this.findViewById(R.id.chat_tool);
		// 聊天消息栏
		msg_view = (RelativeLayout) this.findViewById(R.id.msg_view);
		btn_chat_tool.setOnClickListener(this);
		btn_emotion.setOnClickListener(this);
		btn_chat_tool.setOnClickListener(this);
		Speak.setOnLongClickListener(this);
		Speak.setOnTouchListener(this);
		Send.setOnClickListener(this);
		ChatEdit.addTextChangedListener(this);
		ImageSpeak.setOnClickListener(this);
		ImageBtnKeybox.setOnClickListener(this);

		pop_chat_tool_View = getLayoutInflater().inflate(
				R.layout.popwin_chat_tool, null);

		popWindow = new PopupWindow(pop_chat_tool_View,
				LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		
	}

	@Override
	protected void onPause() {
		super.onPause();
		// bindFssService();
	}

	@Override
	protected void onResume() {
		super.onResume();
		//registerRecFileTransferListener();// 文件注册监听
		// unbindFssService();
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		switch (id) {
		case R.id.msglist_input_send:
			// 发送消息
			SendMessage();
			break;
		case R.id.imgBtnSpeak:
			// 秀出录音
			mode_Torecode();
			break;
		case R.id.imgBtnKeybox:
			// 秀出编辑
			mode_Toedt();
			break;
		case R.id.chat_tool:
			// 秀出工具栏
			if (!popWindow.isShowing()) {
				popWindowShow((LinearLayout) findViewById(R.id.layout_bottom));
			} else {
				popWindowClose(popWindow);
			}
			break;
		case R.id.btn_take_picture:
			startActivityForResult(new Intent(ChatActivity.this,
					SelectPicPopupWindow.class), 1);
			break;
		default:
			break;
		}

	}

	// 消息发送
	private void SendMessage() {
		// 普通文字发送
		if (ChatEdit.getText().length() >= 1) {
			if (mService != null) {
				//mysql 信息同步
				((FSSAPI) getApplication()).ToChatMessage(ChatEdit.getText()
						.toString(), mWithId.replace("@coderss.cn", ""));
				//发送消息
				mService.sendMessage(mWithId, ChatEdit.getText().toString());
				if (!mService.isAuthenticated())
					T.showShort(this, "消息已经保存随后发送");
			}
			ChatEdit.setText(null);
		}
		// 图片发送
		if (uploadpic != null) {
			// 执行等待图片上传后线程
			chat_image=new ChatImgUp("http://chat.coderss.cn/index.php/UploadImage/ImageUp", uploadpic);
			chat_image.execute("");

		}

	}

	/**
	 * 设置聊天的Adapter
	 */
	private void setChatWindowAdapter() {
		String selection = ChatConstants.JID + "='" + mWithId + "'";
		// 异步查询数据库
		new AsyncQueryHandler(getContentResolver()) {

			@Override
			protected void onQueryComplete(int token, Object cookie,
					Cursor cursor) {
				// ListAdapter adapter = new ChatWindowAdapter(cursor,
				// PROJECTION_FROM, PROJECTION_TO, mWithJabberID);
				ListAdapter adapter = new ChatAdapter(ChatActivity.this,
						cursor, PROJECTION_FROM);
				mMsgListView.setAdapter(adapter);
				mMsgListView.setSelection(adapter.getCount() - 1);
			}

		}.startQuery(0, null, ChatProvider.CONTENT_URI, PROJECTION_FROM,
				selection, null, null);

	}

	@Override
	public void onRefresh() {
		mMsgListView.stopRefresh();

	}

	@Override
	public void onLoadMore() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		int action = event.getAction();
		switch (v.getId()) {
		case R.id.msg_listView:
			// 触摸屏幕隐藏输入法
			InputMethodManager mInputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
			;
			mInputMethodManager.hideSoftInputFromWindow(
					ChatEdit.getWindowToken(), 0);
			break;

		case R.id.btnSpeak:
			if (isLongClick) {
				if (action == MotionEvent.ACTION_UP) {
					// 手指抬起，证明录制结束
					((TextView) v).setText("按住说话");
					;
					L.i("录制结束，准备播放");
					isLongClick = false;
					mSpeechController.stopRecord();
					// 开始播放
					new Thread() {
						public void run() {
							try {
								Thread.sleep(300);
							} catch (InterruptedException e1) {
								e1.printStackTrace();
							}
							FileInputStream fileInputStream = null;
							L.i("声音文件在：" + fileName);
							try {
								fileInputStream = new FileInputStream(new File(
										fileName));
							} catch (Exception e) {
								e.printStackTrace();
							}
							mSpeechController.play(fileInputStream,
									mSampleRate, fileName);

						}
					}.start();

					new Thread() {
						public void run() {
							try {
								Thread.sleep(2000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							// 发送文件
							// 把语音传过去
							if (mService != null) {
								L.i("start send file");
								mService.sendFile(mWithId, fileName);
								if (!mService.isAuthenticated())
									T.showLong(getApplicationContext(),
											"文件发送失败，服务不存在");
							}
						};
					}.start();
				}
			}
			break;
		case R.id.msglist_input_send:
			// if (popWindow.isShowing()) {
			// popWindowClose(popWindow);
			// }
			break;

		default:
			break;
		}
		return false;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindFssService();// 解绑服务
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterTextChanged(Editable s) {
		if (s.length() > 0) {
			mode_Tosend();
		} else if (s.length() == 0 && Send.isShown()) {
			mode_Toedt();
		}

	}

	// 开启发送模式
	private void mode_Tosend() {
		Speak.setVisibility(View.GONE);
		ImageSpeak.setVisibility(View.GONE);
		ImageBtnKeybox.setVisibility(View.GONE);
		Send.setVisibility(View.VISIBLE);
	}

	// 开启编辑模式
	private void mode_Toedt() {
		ImageBtnKeybox.setVisibility(View.GONE);
		ImageSpeak.setVisibility(View.VISIBLE);
		Send.setVisibility(View.GONE);
		Speak.setVisibility(View.GONE);
		ChatEdit.setVisibility(View.VISIBLE);

		showIM(ChatEdit, this);
	}

	// 开启录音模式
	private void mode_Torecode() {
		ImageBtnKeybox.setVisibility(View.VISIBLE);
		ImageSpeak.setVisibility(View.GONE);
		Send.setVisibility(View.GONE);
		Speak.setVisibility(View.VISIBLE);
		ChatEdit.setVisibility(View.GONE);

		hideIM(ChatEdit, this);
	}

	// 输入法隐藏输入
	public static void hideIM(View edt, Context context) {
		try {
			InputMethodManager im = (InputMethodManager) context
					.getSystemService(Activity.INPUT_METHOD_SERVICE);
			IBinder windowToken = edt.getWindowToken();
			if (windowToken != null) {
				im.hideSoftInputFromWindow(windowToken, 0);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 输入法秀出输入
	public static void showIM(View edt, Context context) {
		try {
			InputMethodManager im = (InputMethodManager) context
					.getSystemService(Activity.INPUT_METHOD_SERVICE);
			im.showSoftInput(edt, 0);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 长按单击 语音发送
	@Override
	public boolean onLongClick(View v) {
		switch (v.getId()) {
		case R.id.btnSpeak:
			// 开始录音
			isLongClick = true;
			Speak.setText("正在录制.....");
			// 设定录音编码文件存储地址
			fileName = getExternalCacheDir() + "/"
					+ CurrentTime.getCurrentTime("yyyy_MM_dd_hh_mm_ss") + "_"
					+ PreferenceConstants.UserName + ".fss";
			File newfiew = new File(fileName);
			FileOutputStream fileOutputStream = null;
			try {
				fileOutputStream = new FileOutputStream(newfiew);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			mSpeechController.setRecordingMaxTime(9);
			mSpeechController.startRecord(fileOutputStream);

			break;
		}
		return false;

	}

	// 路径变file类
	public File makeRootDirectory(String filePath) {
		File file = null;
		try {
			file = new File(filePath);
			if (!file.exists()) {
				file.mkdir();
			}
		} catch (Exception e) {

		}
		return file;
	}

	// tool
	private void popWindowClose(PopupWindow popwin) {
		RelativeLayout.LayoutParams rlParams = (RelativeLayout.LayoutParams) bottom_line
				.getLayoutParams();
		popwin.dismiss();
		rlParams.bottomMargin = 0;
		bottom_line.setLayoutParams(rlParams);
	}

	private void popWindowShow(View view) {
		// 隐藏输入法
		hideIM(ChatEdit, this);
		RelativeLayout.LayoutParams rlParams = (RelativeLayout.LayoutParams) bottom_line
				.getLayoutParams();
		RelativeLayout.LayoutParams mParams = (RelativeLayout.LayoutParams) msg_view
				.getLayoutParams();
		popHeight = pop_chat_tool_View.getHeight();
		rlParams.bottomMargin = 380;
		bottom_line.setLayoutParams(rlParams);
		popWindow = new PopupWindow(pop_chat_tool_View,
				LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		popWindow.showAtLocation(view, Gravity.BOTTOM, 0, 0);

		// popwindow的监听事件
		popListener();
	}

	/**
	 * popwindow监听事件
	 */
	private void popListener() {
		btn_take_picture = (ImageButton) pop_chat_tool_View
				.findViewById(R.id.btn_take_picture);
		btn_take_picture.setOnClickListener(this);
	}

	/**
	 * intent返回
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		switch (resultCode) {
		case 1:
			// 取回我的intent图片数据
			if (data != null) {
				Uri mImageCaptureUri = data.getData();
				if (mImageCaptureUri != null) {
					Bitmap image;
					try {
						image = MediaStore.Images.Media.getBitmap(
								this.getContentResolver(), mImageCaptureUri);
						if (image != null) {
							mode_Tosend();
							btn_take_picture.setImageBitmap(image);
							uploadpic = image;
							
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					Bundle extras = data.getExtras();
					if (extras != null) {
						Bitmap image = extras.getParcelable("data");
						if (image != null) {
							mode_Tosend();
							btn_take_picture.setImageBitmap(image);
							uploadpic = image;
						}
					}
				}
				
				//隐藏工具栏
				if (popWindow.isShowing()) {
					popWindowClose(popWindow);
				}

			}
			break;
		default:
			break;

		}
	}

	
	//聊天中图片上传
	class ChatImgUp extends AsyncTask<String, Integer, String>{
		//接收地址
		public String actionUrl = "";

		public ChatImgUp(String actionUrl, Bitmap pic) {
			super();
			this.actionUrl = actionUrl;
			this.pic = pic;
		}


		//图片资源
		public Bitmap pic; 
		
		@Override
		protected String doInBackground(String... params) {
			
			String end = "\r\n";
			String twoHyphens = "--";
			String boundary = "******";
			try {
				URL url = new URL(actionUrl);
				HttpURLConnection httpURLConnection = (HttpURLConnection) url
						.openConnection();
				// 设置每次传输的流大小，可以有效防止手机因为内存不足崩溃
				// 此方法用于在预先不知道内容长度时启用没有进行内部缓冲的 HTTP 请求正文的流。
				httpURLConnection.setChunkedStreamingMode(128 * 1024);// 128K
				// 允许输入输出流
				httpURLConnection.setDoInput(true);
				httpURLConnection.setDoOutput(true);
				httpURLConnection.setUseCaches(false);
				// 使用POST方法
				httpURLConnection.setRequestMethod("POST");
				httpURLConnection.setRequestProperty("Connection", "Keep-Alive");
				httpURLConnection.setRequestProperty("Charset", "UTF-8");
				httpURLConnection.setRequestProperty("Content-Type",
						"multipart/form-data;boundary=" + boundary);

				DataOutputStream dos = new DataOutputStream(
						httpURLConnection.getOutputStream());
				dos.writeBytes(twoHyphens + boundary + end);
				dos.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\"; filename=\""
						+ PreferenceConstants.UserName+".jpgß"
						+ "\""
						+ end);
				dos.writeBytes(end);
				
				//bitmap 转inpitstream
				ByteArrayOutputStream baos = new ByteArrayOutputStream();  
				pic.compress(Bitmap.CompressFormat.JPEG, 100, baos);  
		        InputStream fis = new ByteArrayInputStream(baos.toByteArray()); 
				
		        //从文件地址取
				//FileInputStream fis = new FileInputStream(srcPath);
				byte[] buffer = new byte[8192]; // 8k
				int count = 0;
				// 读取文件
				while ((count = fis.read(buffer)) != -1) {
					dos.write(buffer, 0, count);
				}
				fis.close();

				dos.writeBytes(end);
				dos.writeBytes(twoHyphens + boundary + twoHyphens + end);
				dos.flush();

				InputStream is = httpURLConnection.getInputStream();
				String result = Net.getStringFromInPutStream(is);

				dos.close();
				is.close();
				return result;

			} catch (Exception e) {
				e.printStackTrace();
				return "null.jpg";
			}
			
		}
		
		@Override
		protected void onPreExecute() {
			//dialog=ProgressDialog.show(getApplicationContext(), "提示", "图片正在上传......");
		}
		
		
		@Override
		protected void onPostExecute(String result) {
			uploadpic=null;
			if (mService != null) {
				((FSSAPI) getApplication()).ToChatMessage(ChatEdit.getText()
						.toString(), mWithId.replace("@coderss.cn", ""));
				mService.sendMessage(mWithId, result);
			}
			T.showShort(getApplicationContext(), "图片发送成功了啦！");
			//dialog.dismiss();
		}
		
	};
}
