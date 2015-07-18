package cn.coderss.fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import cn.coderss.ui.CustomDialog;
import cn.coderss.quickAction.ActionItem;
import cn.coderss.quickAction.QuickAction;
import cn.coderss.quickAction.QuickAction.OnActionItemClickListener;
import cn.coderss.service.FssService;
import cn.coderss.ui.AddRosterItemDialog;
import cn.coderss.R;
import cn.coderss.activity.ChatActivity;
import cn.coderss.activity.MainActivity;
import cn.coderss.adapter.RosterAdapter;
import cn.coderss.db.RosterProvider;
import cn.coderss.db.RosterProvider.RosterConstants;
import cn.coderss.iphonetreeview.IphoneTreeView;
import cn.coderss.pulltorefresh.RefreshableView;
import cn.coderss.pulltorefresh.RefreshableView.PullToRefreshListener;
import cn.coderss.util.L;
import cn.coderss.util.PreferenceConstants;
import cn.coderss.util.PreferenceUtils;
import cn.coderss.util.T;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.DialogInterface.OnClickListener;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.Layout;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AbsListView.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import cn.coderss.activity.MainActivity.Roster_Group_Handler;

@SuppressLint("ValidFragment")
public class QQ extends Fragment {
	
	public LayoutInflater layoutInflater;
	public RefreshableView refreshableView;
	public Context mcontext;
	public static Context s_mcontext;
	public View view;
	private MainActivity mainActivity;
	private IphoneTreeView mIphoneTreeView;
	private RosterAdapter mRosterAdapter;
	private PopupWindow pop;// popupwindow  
	private ListView lvPopupList;// popupwindow中的ListView  
	private int NUM_OF_VISIBLE_LIST_ROWS = 3;// 指定popupwindow中Item的数量 
	private MyHandler mHandler=new MyHandler();
	public PopupWindow mPopupWindow;
	private int GroupId, ChildId;
	private Roster_Group_Handler myManger;
	private Handler mainHandler = new Handler();
	private ContentObserver mRosterObserver = new RosterObserver();
	//联系人和组查询
	private static final String[] GROUPS_QUERY = new String[] {
		RosterConstants._ID, RosterConstants.GROUP, };
	private static final String[] ROSTER_QUERY = new String[] {
		RosterConstants._ID, RosterConstants.JID, RosterConstants.ALIAS,
		RosterConstants.STATUS_MODE, RosterConstants.STATUS_MESSAGE, };
	
	@SuppressLint("ValidFragment")
	public QQ(Context context,Roster_Group_Handler handler,MainActivity m) {
		super();		
		this.mcontext=context;
		this.myManger=handler;
		this.mainActivity=m;
		this.s_mcontext=mcontext;
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		L.i("QQ [ON CREATE]");
		
		
	}
	
	//寻找ui元素
	private void initView() {
		refreshableView = (RefreshableView) view.findViewById(R.id.refreshable_view);  
		mIphoneTreeView = (IphoneTreeView) view.findViewById(R.id.iphone_tree_view);
		mIphoneTreeView.setHeaderView(getActivity().getLayoutInflater().inflate(
				R.layout.contact_list_group, mIphoneTreeView, false));
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		L.i("QQ CREATE VIEW");
		view=inflater.inflate(R.layout.fragment_qq, null);
		
		//找ui
		initView();
		
		//数据初始化
		initData();
		
		//事件初始化
		initC();
		return view;
	}

	private void initC() {
		refreshableView.setOnRefreshListener(new PullToRefreshListener() {  
            @Override  
            public void onRefresh() {   
            	mHandler.sendEmptyMessage(1);
                L.i("QQ ADAPTER REQUERY");
                refreshableView.finishRefreshing();  
            }  
        }, 0);  
		
		mIphoneTreeView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View mview,
					int position, long id) {
				TextView t1;
				if(mview.findViewById(R.id.group_name)!=null){
					t1=(TextView)mview.findViewById(R.id.group_name);
					GroupId=(int) mview.getTag(R.id.p01);
					ChildId=(int) mview.getTag(R.id.c02);
					//父操作
					//mHandler.sendEmptyMessage(2);
					showGroupQuickActionBar(t1,1);
				}
				else{
					t1=(TextView)mview.findViewById(R.id.contact_list_item_state);	
					GroupId=(int) mview.getTag(R.id.p02);
					ChildId=(int) mview.getTag(R.id.c03);
					//子操作
					//mHandler.sendEmptyMessage(2);
					showGroupQuickActionBar(t1,2);
				}
				
				L.i("roster tag:"+t1.getText().toString());
				
				return false;
			}
		});
		
		mIphoneTreeView.setOnChildClickListener(new OnChildClickListener(){

			@Override
			public boolean onChildClick(ExpandableListView parent, View v,
					int groupPosition, int childPosition, long id) {
				
				String userJid=mRosterAdapter.getChild(groupPosition,
							childPosition).getJid();
				String userName = mRosterAdapter.getChild(groupPosition,
							childPosition).getAlias();
				Intent chatIntent = new Intent(getActivity(), ChatActivity.class);
				Uri userNameUri = Uri.parse(userJid);
				chatIntent.setData(userNameUri);
				chatIntent.putExtra(ChatActivity.INTENT_EXTRA_USERNAME, userName);
				L.i("enter chatactivity with userJid:"+userJid+",userName:"+userName);
				startActivity(chatIntent);
				
				return false;
			}
			
		});
		
	}

	@Override
	public void onResume() {
		super.onResume();
		mainActivity.getContentResolver().registerContentObserver(
				RosterProvider.CONTENT_URI, true, mRosterObserver);
		L.i("QQ [ON RESUME]");
		
		
	}
	
	
	@Override
	public void onPause() {
		super.onPause();
		L.i("QQ [ON PAUSE]");
		mainActivity.getContentResolver().unregisterContentObserver(mRosterObserver);
		mRosterAdapter.requery();
	}
	
	private void initData() {
		mRosterAdapter = new RosterAdapter(getActivity().getApplicationContext(), mIphoneTreeView);
		mIphoneTreeView.setAdapter(mRosterAdapter);
		mRosterAdapter.requery();
	}
	
	
	//handler 更新
	class MyHandler extends Handler{
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case 1:
				try {
					Thread.sleep(1500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				mRosterAdapter.requery();
				break;
			case 2:
				//设置操作
				initPopuptWindow(view.findViewById(R.id.group_name)); 			
				break;

			default:
				break;
			}
		}
	}; 
	
	
	/**
	 * 弹出框
	 * @param v
	 */
	 public void initPopuptWindow(View v) { 	
		View popupWindow = (ViewGroup)LayoutInflater.from(mcontext).inflate(R.layout.list_item_popupwindow, null);
		mPopupWindow = new PopupWindow(popupWindow, 400, 300,true);
	    mPopupWindow.setAnimationStyle(R.style.PopupAnimation);
	    mPopupWindow.showAtLocation(v, Gravity.CENTER, 0, 0);   
	    mPopupWindow.update();
	    mPopupWindow.setFocusable(true); // 设置PopupWindow可获得焦点
	    mPopupWindow.setTouchable(true); // 设置PopupWindow可触摸
	    mPopupWindow.setOutsideTouchable(true); // 设置非PopupWindow区域可触摸
	    ColorDrawable dw = new ColorDrawable(-00000);
	    mPopupWindow.setBackgroundDrawable(dw);
	    
	    TextView t=(TextView) popupWindow.findViewById(R.id.tv_list_item);
	    t.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				mPopupWindow.dismiss();
				
			}
		});
	    
	    
	    }
	 
	 
	 
	 /**
	  * 获取所有组
	  */
	 public static List<String> getRosterGroups() {
			// we want all, online and offline
			List<String> list = new ArrayList<String>();
			Cursor cursor = s_mcontext.getContentResolver().query(RosterProvider.GROUPS_URI,GROUPS_QUERY, null, null, RosterConstants.GROUP);
			int idx = cursor.getColumnIndex(RosterConstants.GROUP);
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				list.add(cursor.getString(idx));
				cursor.moveToNext();
			}
			cursor.close();
			return list;
	}
	 
	
	/**
	 * 获取所有联系人
	 * @return
	 */
	public static List<String[]> getRosterContacts() {
			// we want all, online and offline
			List<String[]> list = new ArrayList<String[]>();
			Cursor cursor = s_mcontext.getContentResolver().query(RosterProvider.CONTENT_URI,
					ROSTER_QUERY, null, null, RosterConstants.ALIAS);
			int JIDIdx = cursor.getColumnIndex(RosterConstants.JID);
			int aliasIdx = cursor.getColumnIndex(RosterConstants.ALIAS);
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				String jid = cursor.getString(JIDIdx);
				String alias = cursor.getString(aliasIdx);
				if ((alias == null) || (alias.length() == 0))
					alias = jid;
				list.add(new String[] { jid, alias });
				cursor.moveToNext();
			}
			cursor.close();
			return list;
	}

	
	/**
	 * 好友或组
	 * 设置弹窗
	 * @param view
	 */
	private void showGroupQuickActionBar(View view,int i) {
		QuickAction quickAction = new QuickAction(mcontext, QuickAction.HORIZONTAL);
		if(i==1){
			//父操作
			quickAction.addActionItem(new ActionItem(0,"添加好友"));
			quickAction.addActionItem(new ActionItem(2,"删除分组"));
		}
		else if(i==2){
			//子操作
			quickAction.addActionItem(new ActionItem(1,"备注好友"));
			quickAction.addActionItem(new ActionItem(3,"删除好友"));
			quickAction.addActionItem(new ActionItem(4,"移动好友"));
		}
		
		quickAction
				.setOnActionItemClickListener(new OnActionItemClickListener() {

					@Override
					public void onItemClick(QuickAction source, int pos,
							int actionId) {
						String userJid =null;
						String userName = null;
						String groupName=null;
						if(ChildId!=-1){
							userJid=mRosterAdapter.getChild(GroupId, ChildId).getJid();
							userName=mRosterAdapter.getChild(GroupId, ChildId).getAlias();
							groupName = mRosterAdapter.getGroup(GroupId).getGroupName();
							MainActivity.userJid=userJid;
							MainActivity.userName=userName;
							MainActivity.GroupName=groupName;
						}
						else{
							groupName = mRosterAdapter.getGroup(GroupId).getGroupName();
							MainActivity.GroupName=groupName;
						}
						
						
						switch (actionId) {
						case 0:
							//添加联系人
							myManger.sendEmptyMessage(1);
							break;
						case 1:
							//重命名联系人
							myManger.sendEmptyMessage(2);
							break;
						case 2:
							//删除分组
							if (TextUtils.isEmpty(groupName)) {// 系统默认分组不允许重命名
								T.showShort(mainActivity,"不许空名分组");
								return;
							}
							T.showShort(mainActivity, "删除分组，需要将组下成员移除即可");
							break;
						case 3:
							//删除联系人
							myManger.sendEmptyMessage(3);
							break;
						case 4:
							//移动联系人
							myManger.sendEmptyMessage(5);
							break;
						default:
							break;
						}
					}
				});
		quickAction.show(view);
		quickAction.setAnimStyle(QuickAction.ANIM_GROW_FROM_CENTER);
	}
	
	//数据库联系人监听
	private class RosterObserver extends ContentObserver {
		public RosterObserver() {
			super(mainHandler);
		}

		public void onChange(boolean selfChange) {
			L.d(MainActivity.class, "RosterObserver.onChange: " + selfChange);
			if (mRosterAdapter != null)
				mainHandler.postDelayed(new Runnable() {
					public void run() {
						updateRoster();
					}
				}, 100);
		}
	}

	
	public void updateRoster() {
		mRosterAdapter.requery();
	}
}
