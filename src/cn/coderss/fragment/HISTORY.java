package cn.coderss.fragment;

import cn.coderss.R;
import cn.coderss.activity.ChatActivity;
import cn.coderss.activity.MainActivity;
import cn.coderss.activity.MainActivity.Roster_Group_Handler;
import cn.coderss.adapter.HistoryAdapter;
import cn.coderss.db.ChatProvider;
import cn.coderss.quickAction.ActionItem;
import cn.coderss.quickAction.QuickAction;
import cn.coderss.quickAction.QuickAction.OnActionItemClickListener;
import cn.coderss.ui.BaseSwipeListViewListener;
import cn.coderss.ui.SwipeListView;
import cn.coderss.util.L;
import cn.coderss.util.T;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.webkit.WebView.FindListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

@SuppressLint("ValidFragment")
public class HISTORY extends Fragment {
	
	public SwipeListView history_listview;
	private Handler mainHandler = new Handler();
	private ContentResolver mContentResolver;
	private ContentObserver mChatObserver = new ChatObserver();
	public View view;
	public HistoryAdapter adapter;
	private MainActivity mainActivity;
	private Context mcontext;
	private String userid;
	private Roster_Group_Handler myManger; 
	
	public HISTORY(MainActivity mainActivity,Roster_Group_Handler handler,Context context) {
		super();
		this.mainActivity = mainActivity;
		this.mcontext=context;
		this.myManger=handler;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		mContentResolver = getActivity().getContentResolver();
	}
	
	private void initView() {
		history_listview=(SwipeListView)view.findViewById(R.id.history_listview);
		adapter=new HistoryAdapter(getActivity());
		history_listview.setAdapter(adapter);
		history_listview.setSwipeMode(0);
		history_listview.setSwipeListViewListener( new BaseSwipeListViewListener() {
		//跳转聊天窗口
		@Override
		public void onClickFrontView(int position,View view) {
			Cursor clickCursor = adapter.getCursor();
			clickCursor.moveToPosition(position);
			String jid = clickCursor.getString(clickCursor
					.getColumnIndex(ChatProvider.ChatConstants.JID));
			userid=jid;
			
			showGroupQuickActionBar(view,1);
		}

		@Override
		public void onClickBackView(int position) {
			history_listview.closeOpenedItems();// 关闭打开的项
		}
		
	});
		
	}
	
	public void updateRoster() {
		adapter.requery();
	}

	private class ChatObserver extends ContentObserver {
		public ChatObserver() {
			super(mainHandler);
		}

		public void onChange(boolean selfChange) {
			updateRoster();
			L.i("liweiping", "selfChange" + selfChange);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		this.view=inflater.inflate(R.layout.history, null);
		initView();
		return view;
	}
	
	
	@Override
	public void onStart() {
		super.onStart();
		
		
	}
	
	@Override
	public void onPause() {
		super.onPause();
		mContentResolver.unregisterContentObserver(mChatObserver);
	}
	
	
	@Override
	public void onResume() {
		super.onResume();
		adapter.requery();
		mContentResolver.registerContentObserver(ChatProvider.CONTENT_URI,
				true, mChatObserver);
	}
	
	
	/**
	 * 会话窗口菜单
	 * 
	 */
	private void showGroupQuickActionBar(View view,int i) {
		QuickAction quickAction = new QuickAction(mcontext, QuickAction.HORIZONTAL);
			//菜单操作
			quickAction.addActionItem(new ActionItem(0,"打开"));
			quickAction.addActionItem(new ActionItem(1,"删除会话"));

		
		quickAction
				.setOnActionItemClickListener(new OnActionItemClickListener() {

					@Override
					public void onItemClick(QuickAction source, int pos,
							int actionId) {
						MainActivity.userJid=userid;
						switch (actionId) {
						case 0:
							//打开会话窗口
							Uri userNameUri = Uri.parse(userid);
							Intent toChatIntent = new Intent(getActivity(), ChatActivity.class);
							toChatIntent.setData(userNameUri);
							toChatIntent.putExtra(ChatActivity.INTENT_EXTRA_USERNAME,
									adapter.getUsername(userid));
							startActivity(toChatIntent);
							break;
						case 1:
							//删除会话窗口
							myManger.sendEmptyMessage(7);
							break;
						default:
							break;
						}
					}
				});
		quickAction.show(view);
		quickAction.setAnimStyle(QuickAction.ANIM_GROW_FROM_CENTER);
	}
}
