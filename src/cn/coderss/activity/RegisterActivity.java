package cn.coderss.activity;

import cn.coderss.R;
import cn.coderss.service.FssService;
import cn.coderss.service.IConnectionStatusCallback;
import cn.coderss.util.L;
import cn.coderss.util.PreferenceConstants;
import cn.coderss.util.PreferenceUtils;
import cn.coderss.util.T;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import cn.coderss.toweb.user;

public class RegisterActivity extends Activity implements IConnectionStatusCallback,OnClickListener{
	private FssService mService;
	private String str_account,str_password;
	private EditText account,password;
	private Button register;
	private ProgressDialog dia;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.registermian);
		setActionBar();
		bindFssService();
		//元素初始化
		initView();
	}
	
	private void initView() {
		account=(EditText)findViewById(R.id.register_account);
		password=(EditText)findViewById(R.id.register_password);
		register=(Button)findViewById(R.id.input_register);
		register.setOnClickListener(this);
		
	}

	//服务链接
	ServiceConnection mServiceConnection = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mService.unRegisterConnectionStatusCallback();
			mService=null;
			
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mService=((FssService.FssBinder)service).getService();
			mService.registerConnectionStatusCallback(RegisterActivity.this);			
		}
	};
	
		//服务绑定
		private void bindFssService(){
			Intent mServiceIntent = new Intent(this, FssService.class);
			bindService(mServiceIntent, mServiceConnection,Context.BIND_AUTO_CREATE + Context.BIND_DEBUG_UNBIND);
		}
		
		//服务解除绑定
		private void unbindFssService() {
			try {
				unbindService(mServiceConnection);
			} catch (IllegalArgumentException e) {
				L.e(LoginActivity.class, "服务未绑定");
			}
		}

		@Override
		public void connectionStatusChanged(int connectedState, String reason) {
			if(reason=="success"){
				dia.dismiss();
				T.showLong(getApplicationContext(), "账号注册成功");
			}
			else if(reason=="failed"){
				dia.dismiss();
				T.showLong(getApplicationContext(), "账号注册失败");
			}
		}

		//设置actionBar
		@SuppressLint("NewApi")
		private void setActionBar() {
			ActionBar actionbar = getActionBar();
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
		
		//保存登陆时配置信息
		private void savePreferences() {
			PreferenceUtils.setPrefString(this, PreferenceConstants.ACCOUNT,
					str_account);// 帐号是一直保存的
			PreferenceUtils.setPrefString(this, PreferenceConstants.PASSWORD,
					str_password);//注册成功，密码保存
		}

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.input_register:
				str_account=account.getText().toString().toLowerCase().trim();
				str_password=password.getText().toString().toLowerCase().trim();
				
				dia= ProgressDialog.show(this, "提示", "正在注册中",  
				            false, true); 
				//与webservice同步mysql数据库
				((FSSAPI)getApplication()).ToWebRegisterUser(str_account, str_password);
				
				mService.Register(str_account,str_password);
				break;

			default:
				break;
			}
			
		}
}
