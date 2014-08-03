/*
	FeedlyClient.java
	
	Copyright (C) 2014 Shun ITO <movingentity@gmail.com>
	
	This file is part of Green Feed Reader.

	Green Feed Reader is free software; you can redistribute it and/or
	modify it under the terms of the GNU General Public License
	as published by the Free Software Foundation; either version 2
	of the License, or (at your option) any later version.
	
	Green Feed Reader is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.
	
	You should have received a copy of the GNU General Public License
	along with Green Feed Reader.  If not, see <http://www.gnu.org/licenses/>.
*/

package mypackage;

import java.io.IOException;
import java.util.Hashtable;

import org.json.me.JSONException;
import org.json.me.JSONObject;

import net.rim.device.api.io.transport.ConnectionFactory;
import net.rim.device.api.system.CodeModuleManager;
import net.rim.device.api.system.CodeSigningKey;
import net.rim.device.api.system.ControlledAccess;
import net.rim.device.api.system.PersistentObject;
import net.rim.device.api.system.PersistentStore;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.container.MainScreen;


public class FeedlyClient
{
	//-- 以下は大切な値のため公開しません -------------------------------------------//
	
	private final static long PERSISTENT_OBJECT_KEY = ;
	private final static String client_id = "";
	private final static String client_secret = "";
	private final static boolean sandbox = false;
	private final static String network_secret = "";
	
	//--------------------------------------------------------------------------//
	
	private MyApp _app = null;
	private FeedlyAPI _feedlyapi = null;
	private Network _network = null;
	
	private State_Base _currentState = null;
	private State_Home _homeState = null;
	
	private PersistentObject _persist;
	private CodeSigningKey codeSigningKey = null;
	
	private ConnectionFactory _connectionFactory = null;
	
	private final static String FEEDLY_REDIRECT_URI = "https://localhost";
	
	// The new sandbox client id and client secret are posted once a month on the
	// https://groups.google.com/forum/#!forum/feedly-cloud
	//private final static String client_id = "xxx";
	//private final static String client_secret = "xxx";
	//private final static boolean sandbox = true;

	private String id = "";
	private String access_token = "";
	private String refresh_token = "";
	private int expires_in = 0; //sec
	private long update = 0; //sec
	
	
	public FeedlyClient()
	{
		_app = (MyApp) UiApplication.getUiApplication();
		
		_feedlyapi = new FeedlyAPI(this, client_id, client_secret, sandbox);
		
		_network = new Network(this);
		_connectionFactory = _network.selectTransport();
		
		//
		//　デバイスに保存された設定の読み込み
		//
		// Get the code signing key associated with "ACME"
		codeSigningKey = CodeSigningKey.get(CodeModuleManager.getModuleHandle("GreenFeedReader"), "ACME" );
		
		//PersistentStore.destroyPersistentObject(PERSISTENT_OBJECT_KEY);
		//return;
		
		_persist = PersistentStore.getPersistentObject(PERSISTENT_OBJECT_KEY);
				
		// 初回起動時はハッシュテーブル形式の設定項目を作成。
		synchronized (_persist)
		{
			if(_persist.getContents(codeSigningKey) == null)
			{
				Hashtable _tmp_ht = new Hashtable();
				_tmp_ht.put("id", "");
				_tmp_ht.put("access_token", "");
				_tmp_ht.put("refresh_token", "");
				_tmp_ht.put("expires_in", new Integer(0));
				_tmp_ht.put("update", new Long(0));
				
				_persist.setContents(new ControlledAccess(_tmp_ht, codeSigningKey));
				PersistentObject.commit(_persist);
			}
		}
		
		// 展開
		Hashtable _tmp_ht = (Hashtable) _persist.getContents(codeSigningKey);
		this.id = (String) _tmp_ht.get("id");
		this.access_token = (String) _tmp_ht.get("access_token");
		this.refresh_token = (String) _tmp_ht.get("refresh_token");
		this.expires_in = ((Integer) _tmp_ht.get("expires_in")).intValue();
		this.update = ((Long)_tmp_ht.get("update")).longValue();
		
		
		//
		// 初期ステイトを設定
		//
		// Homeステイトは保持する。
		_homeState = new State_Home(this);
		_currentState = _homeState;
	}
	
	
	public void addIntegerToPersistentDB(String key, int value)
	{
		synchronized (_persist)
		{
			Hashtable _tmp_ht = (Hashtable) _persist.getContents(codeSigningKey);
			_tmp_ht.put(key, new Integer(value));
			_persist.setContents(new ControlledAccess(_tmp_ht, codeSigningKey));
			PersistentObject.commit(_persist);
		}
	}
	
	
	public void addLongToPersistentDB(String key, long value)
	{
		synchronized (_persist)
		{
			Hashtable _tmp_ht = (Hashtable) _persist.getContents(codeSigningKey);
			_tmp_ht.put(key, new Long(value));
			_persist.setContents(new ControlledAccess(_tmp_ht, codeSigningKey));
			PersistentObject.commit(_persist);
		}
	}
	
	
	public void addStringToPersistentDB(String key, String value)
	{
		synchronized (_persist)
		{
			Hashtable _tmp_ht = (Hashtable) _persist.getContents(codeSigningKey);
			_tmp_ht.put(key, value);
			_persist.setContents(new ControlledAccess(_tmp_ht, codeSigningKey));
			PersistentObject.commit(_persist);
		}
	}
	
	
	public void changeState(State_Base state)
	{
		// 現在のステートクラスのexit()を実行
		_currentState.exit();

		// 新しいステートクラスを、現在のステートクラスとして設定
		_currentState = state;

		// 現在のステートクラス（新ステートクラス）のenter()を実行
		_currentState.enter();
	}
	
	
	public void changeStateToHomeState()
	{
		changeState(_homeState);
	}
	
	
	public void forceLogoutFeedly()
	{
		// AccessTokenを空にする
		this.access_token = "";
		addStringToPersistentDB("access_token", this.access_token);
		
		// RefreshTokenを空にする。
		this.refresh_token = "";
		addStringToPersistentDB("refresh_token", this.refresh_token);
		
		// 取得したExpiresInの値を保存する
		this.expires_in = -1;
		addIntegerToPersistentDB("expires_in", this.expires_in);
		
		// Update
		this.update = System.currentTimeMillis();
		addLongToPersistentDB("update", this.update);
		
		// Homeステイトへ移動
		//changeState(new State_Home(this));
		
		// 現在のステイトを終了して、アプリをリスタート
		_currentState.exit();
		_app.reStartApp();
	}
	
	
	public String getAccessToken()
	{
		return this.access_token;
	}
	
	
	public ConnectionFactory getConnectionFactory()
	{
		return _connectionFactory;
	}
	
	
	public int getExpiresIn()
	{
		return this.expires_in;
	}
	
	
	public String getID()
	{
		return this.id;
	}
	
	
	public String getRefreshToken()
	{
		return this.refresh_token;
	}
	
	
	public FeedlyAPI getFeedlyAPI()
	{
		return this._feedlyapi;
	}
	
	
	public State_Base getCurrentState()
	{
		return _currentState;
	}
	
	
	public String getNetworkSecret()
	{
		return network_secret;
	}
	
	
	public String getRedirectURI()
	{
		return FEEDLY_REDIRECT_URI;
	}
	
	
	public long getUpdate()
	{
		return this.update;
	}
	
	
	public boolean isLogin()
	{
		// リフレッシュトークンが存在する場合はログイン中とみなす。
		if(refresh_token.equals("")) {
			return false;
		} else {
			return true;
		}
	}
	
	
	public void logoutFeedly() throws Exception
	{
		// API
		JSONObject retval = _feedlyapi.revokeRefreshToken();
		
		// AccessTokenを空にする
		access_token = "";
		addStringToPersistentDB("access_token", access_token);
		
		// RefreshTokenを空にする。
		refresh_token = "";
		addStringToPersistentDB("refresh_token", refresh_token);
		
		// 取得したExpiresInの値を保存する
		try {
			expires_in = retval.getInt("expires_in");
		} catch (JSONException e) {
			expires_in = -1;
		}
		addIntegerToPersistentDB("expires_in", expires_in);
		
		// Update
		update = System.currentTimeMillis();
		addLongToPersistentDB("update", update);
		
		// 現在のステイトを終了して、アプリをリスタート
		_currentState.exit();
		_app.reStartApp();
	} //logoutFeedly()
	
	
	public void setAccessToken(String accesstoken)
	{
		this.access_token = accesstoken;
	}
	
	
	public void setExpiresIn (int expires_in)
	{
		this.expires_in = expires_in;
	}
	
	
	public void setID(String id)
	{
		this.id = id;
	}
	
	
	public void setRefreshToken(String refresh_token)
	{
		this.refresh_token = refresh_token;
	}
	
	public void setUpdate(long update)
	{
		this.update = update;
	}
	
	
	public void quitApp()
	{
		_currentState.exit();
		_app.quitApp();
	}
	
	
	public void reStartApp()
	{
		_currentState.exit();
		_app.reStartApp();
	}
	
	
	private boolean isAccessTokenExpired()
	{
		long passed = (System.currentTimeMillis() - this.update) / 1000;
		
		//int remain = this.expires_in - (int) passed;
		//updateStatus("ExpiersIn:" + this.expires_in + " PASSED:" + passed + " Remain:" + remain);
		
		if(this.expires_in < (int) passed) {
			return true;
		} else {
			return false;
		}
	}
	
	
	private void refreshAccessToken() throws IOException, Exception
	{
		JSONObject retval = _feedlyapi.refreshAccessToken();
		
		// example
		// "id": "c805fcbf-3acf-4302-a97e-d82f9d7c897f",
		// "plan": "standard",
		// "access_token": "AQAAEg_icFaeyekDi7gKtCL9O1jh...",
		// "expires_in": 3920,
		// "token_type": "Bearer"
		
		// IDが合致しない場合は強制ログアウト
		if( !this.id.equals(retval.getString("id")) )
		{
			forceLogoutFeedly();
			throw new Exception("IDが一致しません");
		}
		
		// AccessToken
		this.access_token = retval.getString("access_token");
		addStringToPersistentDB("access_token", this.access_token);
		
		// ExpiresIn
		this.expires_in = retval.getInt("expires_in");
		addIntegerToPersistentDB("expires_in", this.expires_in);
		
		// Update
		this.update = System.currentTimeMillis();
		addLongToPersistentDB("update", this.update);
		
		/*UiApplication.getUiApplication().invokeLater(new Runnable()
		{
			public void run()
			{
				Dialog.alert("AccessTokenを更新しました。");
			}
		});*/
	}
	
	
	//
	//-- NetWork --------------------------------------------------------------//
	//
	public String doDelete(String url) throws IOException, Exception
	{
		synchronized(_network)
		{
			// AccessTokenの有効期限が切れている場合は更新する
			if(isAccessTokenExpired())
			{
				// AccessTokenを更新
				refreshAccessToken();
			}
			return _network.delete(_connectionFactory, url, access_token);
		}
	}
	
	
	public String doGet(String url) throws IOException, Exception
	{
		synchronized(_network)
		{
			// AccessTokenの有効期限が切れている場合は更新する
			if(isAccessTokenExpired())
			{
				// AccessTokenを更新
				refreshAccessToken();
			}
			return _network.get(_connectionFactory, url, access_token);
		}
	}
	
	
	/*public Bitmap doGetWebBitmap(String url) throws IOException, Exception
	{	
		synchronized(_network)
		{
			return _network.getWebBitmap(_connectionFactory, url);
		}
	}*/
	
	
	public String doPost(String url, JSONObject body, boolean check) throws IOException, Exception
	{
		synchronized(_network)
		{
			// AccessTokenの有効期限チェックが有効で、期限が切れている場合は更新する
			if(check && isAccessTokenExpired())
			{
				// AccessTokenを更新
				refreshAccessToken();
			}
			//return _network.post(url, body, access_token);
			return _network.post(_connectionFactory ,url, body.toString().getBytes("UTF-8"), access_token);
		}
	}
	
	/*public String doPostJSONArray(String url, JSONArray body, boolean check) throws IOException, Exception
	{
		synchronized(_network)
		{
			// AccessTokenの有効期限チェックが有効で、期限が切れている場合は更新する
			if(check && isAccessTokenExpired())
			{
				// AccessTokenを更新
				refreshAccessToken();
			}
			//return _network.post(url, body, access_token);
			return _network.post(_connectionFactory, url, body.toString().getBytes("UTF-8"), access_token);
		}
	}*/
	
	
	public String doPut(String url, JSONObject body, boolean check) throws IOException, Exception
	{
		synchronized(_network)
		{
			// AccessTokenの有効期限チェックが有効で、期限が切れている場合は更新する
			if(check && isAccessTokenExpired())
			{
				// AccessTokenを更新
				refreshAccessToken();
			}
			//return _network.post(url, body, access_token);
			return _network.put(_connectionFactory ,url, body.toString().getBytes("UTF-8"), access_token);
		}
	}
	
	
	
	public void popScreen(MainScreen screen)
	{
		synchronized (UiApplication.getEventLock())
		{
			_app.popScreen(screen);
		}
	}
	
	
	public void pushScreen(MainScreen screen)
	{
		synchronized (UiApplication.getEventLock())
		{
			_app.pushScreen(screen);
		}
	}
	
	
	public void showHomeScreen()
	{
		_homeState.pushHomeScreen();
	}
	
	
	public void showLogField()
	{
		_app.showLogField();
	}
	
	
	public void updateStatus(final String message)
	{
		_app.updateStatus(message);
	} //updateStatus()
	
}