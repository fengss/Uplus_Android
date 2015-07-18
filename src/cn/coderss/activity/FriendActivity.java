package cn.coderss.activity;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import cn.coderss.R;
import cn.coderss.xlistview.XListView;
import cn.coderss.xlistview.XListView.IXListViewListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import cn.coderss.model_friend.FriendInfo;
import cn.coderss.quickAction.SelectPicPopupWindow;
import cn.coderss.toweb.friendmessage;
import cn.coderss.util.L;
import cn.coderss.util.NetUtil;
import cn.coderss.util.T;
import cn.coderss.util.UploadImage;
import cn.coderss.image_friend.ScaleImageView;
import cn.coderss.image_friend.ImageFetcher;

public class FriendActivity extends FragmentActivity implements IXListViewListener,OnClickListener {
    private ImageFetcher mImageFetcher;
    private XListView mAdapterView = null;
    private StaggeredAdapter mAdapter = null;
    private int currentPage = 0;
    private Button friend_photo,friend_send;
    private EditText friend_message;
    public ImageView friend_pic;
    ContentTask task = new ContentTask(this, 2);
    public Bitmap uploadpic;
    public UploadImage upimage;
    public friendmessage friendmessage;

    private class ContentTask extends AsyncTask<String, Integer, List<FriendInfo>> {

        private Context mContext;
        private int mType = 1;

        public ContentTask(Context context, int type) {
            super();
            mContext = context;
            mType = type;
        }

        @Override
        protected List<FriendInfo> doInBackground(String... params) {
            try {
                return parseNewsJSON(params[0]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<FriendInfo> result) {
            if (mType == 1) {

                mAdapter.addItemTop(result);
                mAdapter.notifyDataSetChanged();
                mAdapterView.stopRefresh();

            } else if (mType == 2) {
                mAdapterView.stopLoadMore();
                mAdapter.addItemLast(result);
                mAdapter.notifyDataSetChanged();
            }

        }

        @Override
        protected void onPreExecute() {
        }

        public List<FriendInfo> parseNewsJSON(String url) throws IOException {
            List<FriendInfo> duitangs = new ArrayList<FriendInfo>();
            String json = "";
            if (NetUtil.checkConnection(mContext)) {
                try {
                    json = NetUtil.getStringFromUrl(url);

                } catch (IOException e) {
                    Log.e("IOException is : ", e.toString());
                    e.printStackTrace();
                    return duitangs;
                }
            }
            Log.d("FriendActiivty", "json:" + json);

            try {
                if (null != json) {
                    JSONObject newsObject = new JSONObject(json);
                    JSONObject jsonObject = newsObject.getJSONObject("data");
                    JSONArray blogsJson = jsonObject.getJSONArray("friendmessage");

                    for (int i = 0; i < blogsJson.length(); i++) {
                        JSONObject newsInfoLeftObject = blogsJson.getJSONObject(i);
                        FriendInfo newsInfo1 = new FriendInfo();
                        newsInfo1.setAlbid(newsInfoLeftObject.isNull("userid") ? "" : newsInfoLeftObject.getString("userid"));
                        newsInfo1.setIsrc(newsInfoLeftObject.isNull("image") ? "" : newsInfoLeftObject.getString("image"));
                        newsInfo1.setMsg(newsInfoLeftObject.isNull("message") ? "" : newsInfoLeftObject.getString("message"));
                        newsInfo1.setHeight(newsInfoLeftObject.getInt("iht"));
                        duitangs.add(newsInfo1);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return duitangs;
        }
    }

    /**
     * 添加内容
     * 
     * @param pageindex
     * @param type
     *            1为下拉刷新 2为加载更多
     */
    private void AddItemToContainer(int pageindex, int type) {
        if (task.getStatus() != Status.RUNNING) {
            String url = "http://uplus.coderss.cn/index.php/FriendMessage/getMessage";
            ContentTask task = new ContentTask(this, type);
            task.execute(url);

        }
    }

    public class StaggeredAdapter extends BaseAdapter {
        private Context mContext;
        private LinkedList<FriendInfo> mInfos;
        private XListView mListView;

        public StaggeredAdapter(Context context, XListView xListView) {
            mContext = context;
            mInfos = new LinkedList<FriendInfo>();
            mListView = xListView;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder holder;
            FriendInfo FriendInfo = mInfos.get(position);

            if (convertView == null) {
                LayoutInflater layoutInflator = LayoutInflater.from(parent.getContext());
                convertView = layoutInflator.inflate(R.layout.friend_infos_list, null);
                holder = new ViewHolder();
                holder.imageView = (ScaleImageView) convertView.findViewById(R.id.xlistview_news_pic);
                holder.contentView = (TextView) convertView.findViewById(R.id.xlistview_news_title);
                convertView.setTag(holder);
            }
            
            L.i("msg:"+FriendInfo.getMsg()+",image:"+FriendInfo.getIsrc());
            holder = (ViewHolder) convertView.getTag();
            holder.imageView.setImageWidth(FriendInfo.getWidth());
            holder.imageView.setImageHeight(FriendInfo.getHeight()-100);
            holder.contentView.setText(FriendInfo.getMsg());
            mImageFetcher.loadImage(FriendInfo.getIsrc(), holder.imageView);
            return convertView;
        }

        class ViewHolder {
            ScaleImageView imageView;
            TextView contentView;
            TextView timeView;
        }

        @Override
        public int getCount() {
            return mInfos.size();
        }

        @Override
        public Object getItem(int arg0) {
            return mInfos.get(arg0);
        }

        @Override
        public long getItemId(int arg0) {
            return 0;
        }

        public void addItemLast(List<FriendInfo> datas) {
            mInfos.addAll(datas);
        }

        public void addItemTop(List<FriendInfo> datas) {
            for (FriendInfo info : datas) {
                mInfos.addFirst(info);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.friend);
        setActionBar();
        initView();
    }
    

    private void initView() {
    	mAdapterView = (XListView) findViewById(R.id.xlistview_list);
        mAdapterView.setPullLoadEnable(true);
        mAdapterView.setXListViewListener(this);
        mAdapter = new StaggeredAdapter(this, mAdapterView);
        mImageFetcher = new ImageFetcher(this, 240);
        mImageFetcher.setLoadingImage(R.drawable.empty_photo);
        
        friend_photo=(Button)findViewById(R.id.friend_photo);
        friend_send=(Button)findViewById(R.id.friend_send);
        friend_pic=(ImageView)findViewById(R.id.friend_pic);
        friend_message=(EditText)findViewById(R.id.friend_msg);
        
        friend_photo.setOnClickListener(this);
        friend_send.setOnClickListener(this);
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
  		

    @Override
    protected void onResume() {
        super.onResume();
        mImageFetcher.setExitTasksEarly(false);
        mAdapterView.setAdapter(mAdapter);
        AddItemToContainer(currentPage, 2);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    @Override
    public void onRefresh() {
        AddItemToContainer(++currentPage, 1);

    }

    @Override
    public void onLoadMore() {
        AddItemToContainer(++currentPage, 2);

    }

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.friend_photo:
			startActivityForResult(new Intent(FriendActivity.this,
					SelectPicPopupWindow.class), 1);
			break;
		case R.id.friend_send:
			T.showShort(getApplicationContext(), "发送功能，要等我数据整理好即可发布");
			String message=friend_message.getText().toString().trim();
			if(message==""){
				T.showLong(getApplicationContext(), "请填写信息后再发布！");
				return;
			}
			upimage=new UploadImage();
			upimage.actionUrl="http://uplus.coderss.cn/index.php/UploadImage/Upload";
			upimage.pic=uploadpic;
			upimage.message=message;
			upimage.action=1;
			upimage.UpLoadFile();
			
			friend_message.setText("");
			
			T.showShort(getApplicationContext(), "正在发送消息......");
			break;
		default:
			break;
		}
		
	}
	
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		switch (resultCode) {
		case 1:
			//取回我的intent图片数据
			if (data != null) {
				Uri mImageCaptureUri = data.getData();
				if (mImageCaptureUri != null) {
					Bitmap image;
					try {
						image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), mImageCaptureUri);
						if (image != null) {
							friend_pic.setImageBitmap(image);
						}
						uploadpic=image;
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					Bundle extras = data.getExtras();
					if (extras != null) {
						Bitmap image = extras.getParcelable("data");
						if (image != null) {
							friend_pic.setImageBitmap(image);
						}
						uploadpic=image;
					}
				}

			}
			break;
		default:
			break;

		}
	}
	
	
	
}
