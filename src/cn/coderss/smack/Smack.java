package cn.coderss.smack;


public interface Smack {
	public boolean login(String account, String password) throws Exception;

	public boolean logout();

	public boolean isAuthenticated();

	public void addRosterItem(String user, String alias, String group)
			throws Exception;

	public void removeRosterItem(String user) throws Exception;

	public void renameRosterItem(String user, String newName)
			throws Exception;

	public void moveRosterItemToGroup(String user, String group)
			throws Exception;

	public void renameRosterGroup(String group, String newGroup);

	public void requestAuthorizationForRosterItem(String user);

	public void addRosterGroup(String group);

	public void setStatusFromConfig();

	public void sendMessage(String user, String message);
	
	public void sendFile(String user,String filepath);

	public void sendServerPing();

	public String getNameForJID(String jid);
}
