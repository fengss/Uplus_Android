package cn.coderss.fragment;

import cn.coderss.R;
import cn.coderss.util.L;
import android.annotation.TargetApi;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.os.Bundle;

public class OURS extends Fragment {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		L.i("OURS [ON CREATE]");
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		L.i("OURS CREATE VIEW");
		
		View view=inflater.inflate(R.layout.fragment_ours, null);
		return view;
	}
	
}
