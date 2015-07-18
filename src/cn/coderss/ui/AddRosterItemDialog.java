package cn.coderss.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import cn.coderss.R;
import cn.coderss.activity.MainActivity;
import cn.coderss.fragment.QQ;
import cn.coderss.service.FssService;
import cn.coderss.util.PreferenceConstants;
import cn.coderss.util.PreferenceUtils;

public class AddRosterItemDialog extends AlertDialog implements
		DialogInterface.OnClickListener{

	private QQ qq;
	private MainActivity mMainActivity;
	private FssService mService;
	private Context mcontext;
	private Button okButton;
	private EditText userInputField;
	private EditText aliasInputField;
	private GroupNameView mGroupNameView;
	private String username;

	public AddRosterItemDialog(MainActivity mainActivity,
			FssService service,String name) {
		super(mainActivity);
		mService = service;
		mMainActivity=mainActivity;
		username=name;
		
		
		setTitle("添加好友");

		LayoutInflater inflater = (LayoutInflater) mainActivity
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View group = inflater.inflate(R.layout.addrosteritemdialog, null, false);
		setView(group);

		userInputField = (EditText)group.findViewById(R.id.AddContact_EditTextField);
		aliasInputField = (EditText)group.findViewById(R.id.AddContactAlias_EditTextField);

		mGroupNameView = (GroupNameView)group.findViewById(R.id.AddRosterItem_GroupName);
		mGroupNameView.setGroupList(QQ.getRosterGroups());

		userInputField.setText(username);
		setButton(BUTTON_POSITIVE, mainActivity.getString(android.R.string.ok), this);
		setButton(BUTTON_NEGATIVE, mainActivity.getString(android.R.string.cancel),
				(DialogInterface.OnClickListener)null);

	}
	public AddRosterItemDialog(MainActivity mainActivity,
			FssService service,String name, String jid) {
		this(mainActivity, service,name);
		userInputField.setText(name);
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		okButton = getButton(BUTTON_POSITIVE);
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		mService.addRosterItem(userInputField.getText()
				.toString()+"@coderss.cn", aliasInputField.getText().toString(),
				mGroupNameView.getGroupName());
	}



}
