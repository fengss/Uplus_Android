package cn.coderss.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import cn.coderss.toweb.friendmessage;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class UploadImage {

	// 要上传的文件路径，理论上可以传输任何文件，实际使用时根据需要处理
	public String srcPath = "/sdcard/testimg.jpg";
	// 服务器上接收文件的处理页面，这里根据需要换成自己的
	public String actionUrl = "http://10.100.1.208/receive_file.php";
	//动作id
	public int action=1;
	//图片资源
	public Bitmap pic; 
	//图片返回保存地址
	public String file;
	//类似朋友圈信息上传
	public friendmessage friendmessage;
	//附加信息
	public String message;
	
	//上传图片线程
	public void UpLoadFile(){
		new Thread(new Runnable(){

			@Override
			public void run() {
				file=uploadFile(actionUrl,pic);
				//上传信息
				if(action==1){
					friendmessage=new friendmessage();
					friendmessage.image=file;
					friendmessage.message=message;
					friendmessage.username=PreferenceConstants.UserName;
					friendmessage.ToFriendMessage();
				}
				
				
			}
			
		},"UpLoadFile").start();
	}
	
	/* 上传文件至Server，uploadUrl：接收文件的处理页面 */
	public static String uploadFile(String uploadUrl,Bitmap pic) {
		String end = "\r\n";
		String twoHyphens = "--";
		String boundary = "******";
		try {
			URL url = new URL(uploadUrl);
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

}
