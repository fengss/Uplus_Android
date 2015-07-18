package cn.coderss.activity;

import org.jivesoftware.smack.ConnectionConfiguration;

import cn.coderss.R;
import cn.coderss.activity.LoginActivity;
import cn.coderss.service.FssService;
import cn.coderss.service.IConnectionStatusCallback;
import cn.coderss.util.PreferenceConstants;
import cn.coderss.util.PreferenceUtils;
import cn.coderss.util.T;
import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import cn.coderss.util.*;

public class LoginActivity extends Activity implements
		IConnectionStatusCallback, OnClickListener {

	private FssService mService;
	public static final String LOGIN_ACTION = "cn.coderss.action.LOGIN";
	private String str_account;
	private String str_password;
	private EditText edit_account, edit_password;
	private Button login, Register;
	private CheckBox Jizhu, Yinshen;
	private Dialog mLoginDialog;
	private static final int LOGIN_OUT_TIME = 0;
	private ConnectionOutTimeProcess mLoginOutTimeProcess;

	ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mService.unRegisterConnectionStatusCallback();
			mService = null;

		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mService = ((FssService.FssBinder) service).getService();
			mService.registerConnectionStatusCallback(LoginActivity.this);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// 绑定服务
		bindFssService();
		startService(new Intent(LoginActivity.this, FssService.class));
		setContentView(R.layout.loginmain);
		// 寻找ui组件
		initView();

	};

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// 解除服务绑定
		unbindFssService();
	}

	private void initView() {
		edit_account = (EditText) findViewById(R.id.input_account);
		edit_password = (EditText) findViewById(R.id.input_password);
		Jizhu = (CheckBox) findViewById(R.id.input_jizhu);
		Yinshen = (CheckBox) findViewById(R.id.input_yinshen);
		login = (Button) findViewById(R.id.login);
		mLoginDialog = DialogUtil.getLoginDialog(this);
		Register = (Button) findViewById(R.id.register);

		Register.setOnClickListener(this);
	}

	/**
	 * 服务回调给activity函数
	 */
	@Override
	public void connectionStatusChanged(int connectedState, String reason) {
		if (reason == "success") {
			if (mLoginDialog != null && mLoginDialog.isShowing())
				mLoginDialog.dismiss();
		}
		if (mLoginOutTimeProcess != null && mLoginOutTimeProcess.running) {
			mLoginOutTimeProcess.stop();
			mLoginOutTimeProcess = null;
		}
		if (connectedState == FssService.CONNECTED) {
			savePreferences();
			startActivity(new Intent(this, MainActivity.class));
			finish();
		} else if (connectedState == FssService.DISCONNECTED){
			T.showLong(LoginActivity.this, " 登陆失败 " + reason);
		}

	}

	// 保存登陆时配置信息
	private void savePreferences() {
		PreferenceUtils.setPrefString(this, PreferenceConstants.ACCOUNT,
				str_account);// 帐号是一直保存的
		// 是否记住密码
		if (Jizhu.isChecked()) {
			PreferenceUtils.setPrefString(this, PreferenceConstants.PASSWORD,
					str_password);
		} else {
			PreferenceUtils.setPrefString(this, PreferenceConstants.PASSWORD,
					"");
		}
		// 是否隐身
		if (Yinshen.isChecked())
			PreferenceUtils.setPrefString(this,
					PreferenceConstants.STATUS_MODE, PreferenceConstants.XA);
		else
			PreferenceUtils.setPrefString(this,
					PreferenceConstants.STATUS_MODE,
					PreferenceConstants.AVAILABLE);
	}

	// 登陆操作
	public void login(View view) {
		// 账号密码数据
		str_account = edit_account.getText().toString().trim();
		str_password = edit_password.getText().toString().trim();

		// 与服务器mysql数据同步
		((FSSAPI) getApplication()).ToWebLoginUser(str_account, str_password);
		
		//保存静态类变量
		PreferenceConstants.UserName=str_account;
		PreferenceConstants.PassWord=str_password;
		
		// 设定我们的服务器名称
		PreferenceUtils.setPrefString(this, PreferenceConstants.Server,
				"www.coderss.cn");
		if (mLoginOutTimeProcess != null && !mLoginOutTimeProcess.running)
			mLoginOutTimeProcess.start();
		if (TextUtils.isEmpty(str_account)) {
			T.showShort(this, "账号为空，请重新填写账号");
		}
		if (TextUtils.isEmpty(str_password)) {
			T.showShort(this, "密码为空，请重新填写密码");
		}
		if (mLoginDialog != null && !mLoginDialog.isShowing())
			mLoginDialog.show();
		if (mService != null) {
			// 开始登陆
			mService.Login(str_account, str_password);
		}

	}

	// 服务绑定
	private void bindFssService() {
		Intent mServiceIntent = new Intent(this, FssService.class);
		mServiceIntent.setAction(LOGIN_ACTION);
		bindService(mServiceIntent, mServiceConnection,
				Context.BIND_AUTO_CREATE + Context.BIND_DEBUG_UNBIND);
	}

	// 服务解除绑定
	private void unbindFssService() {
		try {
			unbindService(mServiceConnection);
		} catch (IllegalArgumentException e) {
			L.e(LoginActivity.class, "服务未绑定");
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.register:
			// 注册操作
			startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
			break;

		default:
			break;
		}

	}

	// 登录超时处理线程
	class ConnectionOutTimeProcess implements Runnable {
		public boolean running = false;
		private long startTime = 0L;
		private Thread thread = null;

		ConnectionOutTimeProcess() {
		}

		public void run() {
			while (true) {
				if (!this.running)
					return;
				if (System.currentTimeMillis() - this.startTime > 20 * 1000L) {
					mHandler.sendEmptyMessage(LOGIN_OUT_TIME);
				}
				try {
					Thread.sleep(10L);
				} catch (Exception localException) {
				}
			}
		}

		public void start() {
			try {
				this.thread = new Thread(this);
				this.running = true;
				this.startTime = System.currentTimeMillis();
				this.thread.start();
			} finally {
			}
		}

		public void stop() {
			try {
				this.running = false;
				this.thread = null;
				this.startTime = 0L;
			} finally {
			}
		}
	}

	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case LOGIN_OUT_TIME:
				if (mLoginOutTimeProcess != null
						&& mLoginOutTimeProcess.running)
					mLoginOutTimeProcess.stop();
				if (mLoginDialog != null && mLoginDialog.isShowing())
					mLoginDialog.dismiss();
				T.showShort(LoginActivity.this, "登陆超时");
				break;

			default:
				break;
			}
		}

	};

}
