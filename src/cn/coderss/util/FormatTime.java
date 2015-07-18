package cn.coderss.util;

public class FormatTime {

	/**************************************��ʽ��ʱ�䣺���롪��>mm��ss�ķ���******************************************************/
	public static String formatTime(int msec){
		int minute = (msec / 1000) / 60;
		int second = (msec / 1000) % 60;
		String minuteString;
		String secondString;
		if(minute < 10){
			minuteString = "0" + minute;
		} else{
			minuteString = "" + minute;
		}
		if (second < 10){
			secondString = "0" + second;
		} else {
			secondString = "" + second;
		}
		return minuteString + ":" + secondString;
	}
}
