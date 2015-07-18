package cn.coderss.activity;

import cn.coderss.R;
import cn.coderss.ui.AddRosterItemDialog;
import cn.coderss.util.CustomSwitchPreference;
import cn.coderss.util.L;
import cn.coderss.util.PreferenceConstants;
import cn.coderss.util.PreferenceUtils;
import cn.coderss.util.T;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

public class SettingActivity extends PreferenceActivity {
	private CustomSwitchPreference avator,offline,music,cl;
	public PreferenceScreen reply,about;
	
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.setting);
		//设置返回bar
		setActionBar();
		
		initView();
	}
	
	
	@SuppressLint("NewApi")
	private void initView() {
		avator=(CustomSwitchPreference)findPreference("avator");
		offline=(CustomSwitchPreference)findPreference("offline");
		music=(CustomSwitchPreference)findPreference("music");
		cl=(CustomSwitchPreference)findPreference("cl");
		reply=(PreferenceScreen)findPreference("reply");
		about=(PreferenceScreen)findPreference("about");
		
		offline.setChecked(PreferenceUtils.getPrefBoolean(
				getApplicationContext(),PreferenceConstants.FOREGROUND, offline.isChecked()));
		music.setChecked(PreferenceUtils.getPrefBoolean(
				getApplicationContext(), PreferenceConstants.SCLIENTNOTIFY, music.isChecked()));
		avator.setChecked(PreferenceUtils.getPrefBoolean(
				getApplicationContext(), PreferenceConstants.SHOW_OFFLINE, avator.isChecked()));
		cl.setChecked(PreferenceUtils.getPrefBoolean(
				getApplicationContext(),PreferenceConstants.AUTO_RECONNECT, cl.isChecked()));
		
		avator.setOnPreferenceChangeListener(new OnPreferenceChangeListener(){

			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
					PreferenceUtils.setPrefBoolean(getApplicationContext(), PreferenceConstants.SHOW_OFFLINE, avator.isChecked());
				return false;
			}
			
		});
		
		offline.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				PreferenceUtils.setPrefBoolean(getApplicationContext(),
						PreferenceConstants.FOREGROUND, offline.isChecked());
				return false;
			}
		});
		
		music.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				PreferenceUtils.setPrefBoolean(
						getApplicationContext(), PreferenceConstants.SCLIENTNOTIFY, music.isChecked());
				return false;
			}
		});
		
		
		cl.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				PreferenceUtils.setPrefBoolean(getApplicationContext(),
						PreferenceConstants.AUTO_RECONNECT, cl.isChecked());
				return false;
			}
		});
		
		reply.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			
			@Override
			public boolean onPreferenceClick(Preference preference) {
				
				return false;
			}
		});
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



	
}
