package cn.coderss.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 时间格式
 * ***/
public class CurrentTime {
	//"yyyy-MM-dd hh:mm:ss"
	public static String getCurrentTime(String format){
		SimpleDateFormat dateForamt= new SimpleDateFormat(format);
		Date curDate = new Date(System.currentTimeMillis());
		String time = dateForamt.format(curDate);
		return time;
	}
	
}
