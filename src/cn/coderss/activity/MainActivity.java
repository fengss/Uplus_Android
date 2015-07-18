package cn.coderss.activity;

import java.util.ArrayList;
import java.util.List;

import cn.coderss.R;
import cn.coderss.baidulbs.RoutePlan;
import cn.coderss.db.ChatProvider;
import cn.coderss.db.RosterProvider;
import cn.coderss.fragment.*;
import cn.coderss.quickAction.ActionItem;
import cn.coderss.quickAction.QuickAction;
import cn.coderss.quickAction.QuickAction.OnActionItemClickListener;
import cn.coderss.service.FssService;
import cn.coderss.service.IConnectionStatusCallback;
import cn.coderss.ui.AddRosterItemDialog;
import cn.coderss.ui.CustomDialog;
import cn.coderss.ui.GroupNameView;
import cn.coderss.util.L;
import cn.coderss.util.PreferenceConstants;
import cn.coderss.util.PreferenceUtils;
import cn.coderss.util.T;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBar.TabListener;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.DownloadManager.Request;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Build;

public class MainActivity extends ActionBarActivity implements
		IConnectionStatusCallback, OnQueryTextListener,
		android.view.View.OnClickListener {

	private FssService mService;
	private ContentObserver mRosterObserver = new RosterObserver();
	private Handler publicHandler = new Handler();
	public ViewPager vp;
	public SearchView find;
	public ViewPagerdapter myadapter;
	// 常量
	public static final int TAB_WECHAT = 0;
	public static final int TAB_QQ = 1;
	public static final int TAB_FRIEND = 2;
	// fragment
	public OURS OURS = new OURS();
	public QQ QQ;
	public HISTORY HISTORY;
	// event x坐标
	public float RawX;
	public static String userJid, userName, GroupName;
	// menu
	public Button map_find, friend, chat;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		startService(new Intent(MainActivity.this, FssService.class));
		setContentView(R.layout.activity_main);

		initView();
	}

	@SuppressLint("NewApi")
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		find = (SearchView) menu.findItem(R.id.action_find).getActionView();
		find.setSubmitButtonEnabled(true);
		find.setIconifiedByDefault(true);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		switch (id) {
		case R.id.action_find:
			// 搜索好友操作
			find.setOnQueryTextListener(this);
			break;
		case R.id.action_settings:
			// 设置操作
			startActivity(new Intent(MainActivity.this, SettingActivity.class));
			break;
		case R.id.address:
			// 地图 寻友
			startActivity(new Intent(MainActivity.this, RoutePlan.class));
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	// 初始化ui以及fragment
	private void initView() {
		QQ = new QQ(getApplicationContext(), new Roster_Group_Handler(),
				MainActivity.this);
		HISTORY = new HISTORY(MainActivity.this, new Roster_Group_Handler(),
				getApplicationContext());

		// 总导航
		map_find = (Button) findViewById(R.id.menu_find);
		friend = (Button) findViewById(R.id.menu_friend);
		chat = (Button) findViewById(R.id.menu_chat);
		map_find.setOnClickListener(this);
		friend.setOnClickListener(this);
		chat.setOnClickListener(this);

		// getSupportActionBar().hide();
		List<Fragment> list = new ArrayList<Fragment>();
		FragmentManager fm = getSupportFragmentManager();
		list.add(HISTORY);
		list.add(QQ);
		list.add(OURS);
		vp = (ViewPager) findViewById(R.id.Main_vp);
		myadapter = new ViewPagerdapter(fm, list);
		vp.setAdapter(myadapter);
	}

	// 服务链接
	ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mService = ((FssService.FssBinder) service).getService();
			mService.registerConnectionStatusCallback(MainActivity.this);
			// 判断是否链接，如果无，进行登陆链接
			if (!mService.isAuthenticated()) {
				String usr = PreferenceUtils.getPrefString(MainActivity.this,
						PreferenceConstants.ACCOUNT, "");
				String password = PreferenceUtils.getPrefString(
						MainActivity.this, PreferenceConstants.PASSWORD, "");
				mService.Login(usr, password);
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mService.unRegisterConnectionStatusCallback();
			mService = null;
		}

	};

	@Override
	protected void onResume() {
		super.onResume();
		// 监听联系人数据库变动
		getContentResolver().registerContentObserver(
				RosterProvider.CONTENT_URI, true, mRosterObserver);
		bindFssService();
	};

	@Override
	protected void onPause() {
		super.onPause();
		// 去除联系人数据库变动监听
		getContentResolver().unregisterContentObserver(mRosterObserver);
		unbindFssService();
		L.i("MainActivity ON PAUSE");
	}

	// 解除服务
	private void unbindFssService() {
		try {
			unbindService(mServiceConnection);
			L.i(LoginActivity.class, "[SERVICE] Unbind");
		} catch (IllegalArgumentException e) {
			L.e(LoginActivity.class, "Service wasn't bound!");
		}
	}

	// 绑定服务
	private void bindFssService() {
		L.i(MainActivity.class, "[SERVICE] Onbind");
		boolean bind = bindService(new Intent(MainActivity.this,
				FssService.class), mServiceConnection, Context.BIND_AUTO_CREATE
				+ Context.BIND_DEBUG_UNBIND);
	}

	/**
	 * 连续按两次返回键就退出
	 */
	private long firstTime;

	@Override
	public void onBackPressed() {
		if (System.currentTimeMillis() - firstTime < 3000) {
			finish();
		} else {
			firstTime = System.currentTimeMillis();
			T.showShort(this, "请再按一次");
		}
	}

	@SuppressLint("NewApi")
	@Override
	public void connectionStatusChanged(int connectedState, String reason) {
		// 判断链接状态
		switch (connectedState) {
		case FssService.CONNECTED:
			// 链接中
			getActionBar().setTitle(PreferenceConstants.UserName + " 在线");
			break;
		case FssService.CONNECTING:
			// 正在链接
			getActionBar().setTitle("正在连接");
			startActivity(new Intent(MainActivity.this,LoginActivity.class));
			break;
		case FssService.DISCONNECTED:
			// 断开链接
			getActionBar().setTitle("断开链接");
			startActivity(new Intent(MainActivity.this,LoginActivity.class));
			break;

		default:
			break;
		}

	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			RawX = event.getX();

			break;
		case MotionEvent.ACTION_UP:

			break;
		default:
			break;
		}

		if (QQ.mPopupWindow != null && QQ.mPopupWindow.isShowing()) {
			QQ.mPopupWindow.dismiss();
			QQ.mPopupWindow = null;
		}
		return false;
	}

	// 判断服务链接有没有丢失
	private boolean isConnected() {
		return mService != null && mService.isAuthenticated();
	}

	// 联系人数据库变动监听
	private class RosterObserver extends ContentObserver {
		public RosterObserver() {
			super(publicHandler);
		}

		public void onChange(boolean selfChange) {
			L.d(MainActivity.class, "数据库收到改变通知信息...RosterObserver.onChange: "
					+ selfChange);
			/**
			 * 处理联系人adapter
			 */
			// if (mRosterAdapter != null)
			publicHandler.postDelayed(new Runnable() {
				public void run() {
					// updateRoster();
				}
			}, 100);
		}
	}

	/**
	 * VIEWPAGER 画面切换
	 */
	class ViewPagerdapter extends FragmentPagerAdapter {
		private List<Fragment> list;

		public ViewPagerdapter(FragmentManager fm, List<Fragment> list) {
			super(fm);
			this.list = list;
		}

		@Override
		public int getCount() {
			return list.size();
		}

		@Override
		public int getItemPosition(Object object) {
			return list.indexOf(object);
		}

		@Override
		public Fragment getItem(int arg0) {
			return list.get(arg0);
		}

	}

	// 搜索栏好友搜索
	@Override
	public boolean onQueryTextChange(String arg0) {
		return false;
	}

	@Override
	public boolean onQueryTextSubmit(String name) {
		new AddRosterItemDialog(MainActivity.this, mService, name).show();
		return false;
	}

	/**
	 * 信息内容编辑框
	 * 
	 * @param titleId
	 * @param message
	 * @param text
	 * @param ok
	 */
	private void editTextDialog(String title, CharSequence message,
			String text, final EditOk ok) {
		LayoutInflater inflater = LayoutInflater.from(this);
		View layout = inflater.inflate(R.layout.edittext_dialog, null);

		TextView messageView = (TextView) layout.findViewById(R.id.text);
		messageView.setText(message);
		final EditText input = (EditText) layout.findViewById(R.id.editText);
		input.setTransformationMethod(android.text.method.SingleLineTransformationMethod
				.getInstance());
		input.setText(text);
		new CustomDialog.Builder(MainActivity.this)
				.setTitle(title)
				.setView(layout)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								String newName = input.getText().toString();
								if (newName.length() != 0)
									ok.ok(newName);
							}
						})
				.setNegativeButton(android.R.string.cancel,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {

							}
						}).create().show();
	}

	public abstract class EditOk {
		abstract public void ok(String result);
	}

	// 重命名分组
	void renameRosterGroupDialog() {
		editTextDialog("备注分组", "分组名重新备注", GroupName, new EditOk() {
			public void ok(String result) {
				if (mService != null)
					mService.renameRosterGroup(GroupName, result);
			}
		});
	}

	// 重命名联系人
	void renameRosterItemDialog(final String JID, final String userName) {
		editTextDialog("备注联系人", JID, userName, new EditOk() {
			public void ok(String result) {
				if (mService != null)
					mService.renameRosterItem(JID, result);
			}
		});
	}

	// 删除联系人组
	void renameRosterGroupDialog(final String groupName) {
		editTextDialog("删除联系人组", groupName, groupName, new EditOk() {
			public void ok(String result) {
				if (mService != null)
					mService.renameRosterGroup(groupName, result);
			}
		});
	}

	// 移动联系人
	void moveRosterItemToGroupDialog(final String jabberID) {
		LayoutInflater inflater = LayoutInflater.from(this);
		View group = inflater
				.inflate(R.layout.moverosterentrytogroupview, null);
		final GroupNameView gv = (GroupNameView) group
				.findViewById(R.id.moverosterentrytogroupview_gv);
		gv.setGroupList(QQ.getRosterGroups());
		new CustomDialog.Builder(this)
				.setTitle("移动联系人")
				.setView(group)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								L.d("new group: " + gv.getGroupName());
								if (isConnected())
									mService.moveRosterItemToGroup(jabberID,
											gv.getGroupName());
							}
						})
				.setNegativeButton(android.R.string.cancel,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {

							}
						}).create().show();
	}

	// 删除会话窗口
	void removeChatItemDialog(final String JID) {
		new CustomDialog.Builder(MainActivity.this)
				.setTitle("删除联系人会话")
				.setMessage("是否要真的删除此联系人的会话呢？")
				.setPositiveButton(android.R.string.yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								// 删除联系人
								getApplicationContext().getContentResolver()
										.delete(ChatProvider.CONTENT_URI,
												ChatProvider.ChatConstants.JID
														+ " = ?",
												new String[] { JID });
							}
						})
				.setNegativeButton(android.R.string.no,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {

							}
						}).create().show();
	}

	// 删除联系人
	void removeRosterItemDialog(final String JID, final String userName) {
		new CustomDialog.Builder(MainActivity.this)
				.setTitle("删除联系人")
				.setMessage("是否要真的删除联系人呢？")
				.setPositiveButton(android.R.string.yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								mService.removeRosterItem(JID);
							}
						})
				.setNegativeButton(android.R.string.no,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {

							}
						}).create().show();
	}

	// 负责联系人和联系人组管理
	public class Roster_Group_Handler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case 1:
				// 添加联系人
				new AddRosterItemDialog(MainActivity.this, mService, " ")
						.show();// 添加联系人
				break;
			case 2:
				// 重名联系人
				renameRosterItemDialog(userJid, userName);
				break;
			case 3:
				// 删除联系人
				removeRosterItemDialog(userJid, userName);
				break;
			case 4:
				// 删除联系人组
				renameRosterGroupDialog(GroupName);
				break;
			case 5:
				// 移动联系人
				moveRosterItemToGroupDialog(userJid);
				break;
			case 6:
				// 重备注分组
				renameRosterGroupDialog();
				break;
			case 7:
				// 删除联系人窗口
				removeChatItemDialog(userJid);
				break;
			default:
				break;
			}
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.menu_chat:
			vp.setCurrentItem(1);
			break;

		case R.id.menu_find:
			startActivity(new Intent(MainActivity.this, RoutePlan.class));
			break;

		case R.id.menu_friend:
			startActivity(new Intent(MainActivity.this, FriendActivity.class));
			break;
		default:
			break;
		}

	};

	// 秀出窗口
	public void showReLoginView() {
		// 离线后的重新登录
		final ProgressDialog dia = new ProgressDialog(
				getApplicationContext());
		dia.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		dia.setCancelable(false);
		dia.setCanceledOnTouchOutside(false);
		dia.setIcon(R.drawable.ic_launcher);
		dia.setTitle("账号离线");
		dia.setMessage("账号正在重链.....");
		dia.setButton("确定",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(
							DialogInterface dialog,
							int which) {
						mService.Login(PreferenceConstants.UserName, PreferenceConstants.PassWord);
					}
				});
		dia.setButton("取消",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(
							DialogInterface dialog,
							int which) {
						dia.dismiss();
					}
				});

	}

}
