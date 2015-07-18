package cn.coderss.test;

import cn.coderss.R;
import cn.coderss.activity.LoginActivity;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class TestActivity extends Activity {
	private Button btn;
	private EditText edit;
	private TextView text;
	private Fss_Smack2 smack;
	public static String content;
	public MyHandler nhandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.test);
		
		initView();
	}
	private void initView() {
		btn=(Button)findViewById(R.id.button1);
		edit=(EditText)findViewById(R.id.editText1);
		text=(TextView)findViewById(R.id.textView1);
		
		nhandler=new MyHandler();
		new Thread(){
			public void run() {
				Looper.prepare();
				smack=new Fss_Smack2(getApplicationContext(),nhandler);
				smack.Login("fengss", "xuanxuan");
				smack.getRoster(1);
				smack.getMessage();
				Looper.loop();
			};
		}.start();
		

		btn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent it=new Intent(TestActivity.this,LoginActivity.class);
				startActivity(it);
				/*
				text.append("你所发的信息:"+edit.getText().toString().trim()+"\n");
				smack.sendMessage(edit.getText().toString().trim());
				edit.setText("");
				*/
			}
		});
		
	}
	
	
	public class MyHandler extends Handler{
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if(msg.what==1){
				text.append(content);
			}
			else if (msg.what==2){
				//text.append("你所发的信息:"+edit.getText().toString().trim()+"\n");
			}
			//edit.setText("");
		}
	}
	
}
