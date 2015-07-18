package cn.coderss.adapter;

import java.io.File;
import java.io.FileInputStream;

import com.boyaa.speech.SpeechController;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import cn.coderss.db.ChatProvider;
import cn.coderss.db.ChatProvider.ChatConstants;
import cn.coderss.image_friend.ImageFetcher;
import cn.coderss.util.L;
import cn.coderss.util.PreferenceConstants;
import cn.coderss.util.PreferenceUtils;
import cn.coderss.util.T;
import cn.coderss.util.TimeUtil;
import cn.coderss.R;

public class ChatAdapter extends SimpleCursorAdapter {

	private static final int DELAY_NEWMSG = 2000;
	private Context mContext;
	private LayoutInflater mInflater;
	private SpeechController mSpeechController;
	private ImageFetcher mImageFetcher;

	public ChatAdapter(Context context, Cursor cursor, String[] from) {
		// super(context, android.R.layout.simple_list_item_1, cursor, from,
		// to);
		super(context, 0, cursor, from, null);
		mContext = context;
		mInflater = LayoutInflater.from(context);
		mImageFetcher = new ImageFetcher(context, 240);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Cursor cursor = this.getCursor();
		cursor.moveToPosition(position);
		long dateMilliseconds = cursor.getLong(cursor
				.getColumnIndex(ChatProvider.ChatConstants.DATE));

		int _id = cursor.getInt(cursor
				.getColumnIndex(ChatProvider.ChatConstants._ID));
		String date = TimeUtil.getChatTime(dateMilliseconds);
		String message = cursor.getString(cursor
				.getColumnIndex(ChatProvider.ChatConstants.MESSAGE));
		int come = cursor.getInt(cursor
				.getColumnIndex(ChatProvider.ChatConstants.DIRECTION));// 消息来自
		boolean from_me = (come == ChatConstants.OUTGOING);
		String jid = cursor.getString(cursor
				.getColumnIndex(ChatProvider.ChatConstants.JID));
		int delivery_status = cursor.getInt(cursor
				.getColumnIndex(ChatProvider.ChatConstants.DELIVERY_STATUS));
		ViewHolder viewHolder;
		if (convertView == null
				|| convertView.getTag(R.drawable.ic_launcher + come) == null) {
			if (come == ChatConstants.OUTGOING) {
				convertView = mInflater.inflate(R.layout.chat_item_right,
						parent, false);
			} else {
				convertView = mInflater.inflate(R.layout.chat_item_left, null);
			}
			viewHolder = buildHolder(convertView);
			convertView.setTag(R.drawable.ic_launcher + come, viewHolder);
			convertView
					.setTag(R.string.app_name, R.drawable.ic_launcher + come);
		} else {
			viewHolder = (ViewHolder) convertView.getTag(R.drawable.ic_launcher
					+ come);
		}
		if (!from_me && delivery_status == ChatConstants.DS_NEW) {
			markAsReadDelayed(_id, DELAY_NEWMSG);
		}

		bindViewData(viewHolder, date, from_me, jid, message, delivery_status);
		return convertView;
	}

	private void markAsReadDelayed(final int id, int delay) {
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				markAsRead(id);
			}
		}, delay);
	}

	/**
	 * 标记为已读消息
	 * 
	 * @param id
	 */
	private void markAsRead(int id) {
		Uri rowuri = Uri.parse("content://" + ChatProvider.AUTHORITY + "/"
				+ ChatProvider.TABLE_NAME + "/" + id);
		L.d("markAsRead: " + rowuri);
		ContentValues values = new ContentValues();
		values.put(ChatConstants.DELIVERY_STATUS, ChatConstants.DS_SENT_OR_READ);
		mContext.getContentResolver().update(rowuri, values, null, null);
	}

	private void bindViewData(final ViewHolder holder, String date, boolean from_me,
			String from, String message, int delivery_status) {		
		holder.avatar.setBackgroundResource(R.drawable.login_default_avatar);
		if (from_me
				&& !PreferenceUtils.getPrefBoolean(mContext,
						PreferenceConstants.SHOW_MY_HEAD, true)) {
			holder.avatar.setVisibility(View.GONE);
		}
		//处理将字符转换成表情
		holder.content.setText(message);
		holder.time.setText(date);
		holder.content.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				String content=((TextView)v).getText().toString();
				if(content.length()>6){
					String head_str=content.substring(0, 5);
					if("yuyin".equals(head_str)){
						L.i("开始播放");
						String fileName=content.substring(6, content.length());
						FileInputStream fileInputStream = null;
						L.i("声音文件在：" + fileName);
						try {
							fileInputStream = new FileInputStream(new File(
									fileName));
						} catch (Exception e) {
							e.printStackTrace();
						}
						mSpeechController = SpeechController.getInstance();
						mSpeechController.play(fileInputStream,
								10000, fileName);
						
					}
					
				}
				
			}
		});
		
		int str=message.length();
		if(str>10){
			String foot_str=message.substring(str-4);
			if(".jpg".equals(foot_str)){
				mImageFetcher.loadImage("http://chat.coderss.cn/upload/"+message, holder.image);
				holder.content.setText("图片详情");
			}
			
		}
		

	}

	private ViewHolder buildHolder(View convertView) {
		ViewHolder holder = new ViewHolder();
		holder.content = (TextView) convertView.findViewById(R.id.textView2);
		holder.time = (TextView) convertView.findViewById(R.id.datetime);
		holder.avatar = (ImageView) convertView.findViewById(R.id.icon);
		holder.image=(ImageView)convertView.findViewById(R.id.c_image);
		return holder;
	}

	private static class ViewHolder {
		TextView content;
		TextView time;
		ImageView avatar;
		ImageView image;

	}
	

}
