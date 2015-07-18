package cn.coderss.service;

import cn.coderss.activity.MainActivity;

public interface IConnectionStatusCallback {
	public void connectionStatusChanged(int connectedState, String reason);

}
