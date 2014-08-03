/*
	State_Auth.java
	
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

import org.json.me.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.rim.device.api.browser.field2.BrowserField;
import net.rim.device.api.browser.field2.BrowserFieldListener;
import net.rim.device.api.io.transport.ConnectionFactory;
import net.rim.device.api.script.ScriptEngine;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.Dialog;


public class State_Auth extends BrowserFieldListener  implements State_Base
{
	private FeedlyClient _feedlyclient = null;
	private FeedlyAPI _feedlyapi = null;
	
	private Screen_Auth _authscreen = null;
	
	
	public State_Auth(FeedlyClient _feedlyclient)
	{
		this._feedlyclient = _feedlyclient;
		this._feedlyapi = _feedlyclient.getFeedlyAPI();
	}
	
	
	
	//
	//-- Implement State__Base----------------------------------------------------//
	//
	public void enter()
	{
		//updateStatus("enter()");
		
		// 画面を表示
		String url = _feedlyapi.get_code_url(_feedlyclient.getRedirectURI());
		_authscreen = new Screen_Auth(this);
		_feedlyclient.pushScreen(_authscreen);
		
		// 有効な通信経路があるか確認する
		if(Network.isCoverageSufficient())
		{
			// Feedlyの認証画面をリクエスト
			_authscreen.requestContent(url);
			
		} else {
			UiApplication.getUiApplication().invokeLater(new Runnable()
			{
				public void run()
				{
					//Dialog.alert("A communication error has occurred. Please make sure your device is connected to Internet.");
					int select = Dialog.ask("A communication error has occurred. Please make sure your device is connected to Internet.", new Object[]{"Try again","Quit"}, 0);
					
					switch(select)
					{
						case -1:
							_feedlyclient.quitApp();
							break;
						
						case 0:
							_feedlyclient.reStartApp();
							break;
							
						case 1:
							_feedlyclient.quitApp();
							break;
					}
				} //run()
			});
		}
	} //enter()
	
	
	public void exit()
	{
		//updateStatus("exit()");
		
		if(_authscreen != null)
		{
			_feedlyclient.popScreen(_authscreen);
		}
		
		_authscreen = null;
	}
	
	
	public void close()
	{
		UiApplication.getUiApplication().invokeLater(new Runnable()
		{
			public void run()
			{
				int select = Dialog.ask("Authentication is not complete.", new Object[]{"Continue","Quit"}, 0);
				
				switch(select)
				{
					case -1:
						//_feedlyclient.quitApp();
						// PASS
						break;
					
					case 0:
						//_feedlyclient.reStartApp();
						// PASS
						break;
						
					case 1:
						_feedlyclient.quitApp();
						break;
				}
			} //run()
		});
	}
	
	
	public ConnectionFactory getConnectionFactory()
	{
		return _feedlyclient.getConnectionFactory();
	}
	
	/*
	public Command changeStateToHomeCMD()
	{
		Command out = new Command(new CommandHandler() 
		{
			public void execute(ReadOnlyCommandMetadata metadata, Object context) 
			{
				_feedlyclient.changeState(new State_Home(_feedlyclient));
			} //execute()
		});
		return out;
	} //changeStateToHomeCMD()
	*/
	
	
	public void updateStatus(final String message)
	{
		_feedlyclient.updateStatus("State_Auth::" + message);
	} //updateStatus()
	
	
	private void getAccessToken(final String code)
	{
		new Thread()
		{
			public void run()
			{
				try {
					//
					JSONObject returnedjson = _feedlyapi.getAccessToken(_feedlyclient.getRedirectURI(), code);
					
					if(returnedjson != null)
					{
						//
						// 取得した値を設定ファイルに保存
						//
						// ID
						_feedlyclient.setID(returnedjson.getString("id"));
						_feedlyclient.addStringToPersistentDB("id", _feedlyclient.getID());
						
						// AccessToken
						_feedlyclient.setAccessToken(returnedjson.getString("access_token"));
						_feedlyclient.addStringToPersistentDB("access_token", _feedlyclient.getAccessToken());
						
						// RefreshToken
						_feedlyclient.setRefreshToken(returnedjson.getString("refresh_token"));
						_feedlyclient.addStringToPersistentDB("refresh_token", _feedlyclient.getRefreshToken());
						//updateStatus("refresh_token:" + returnedjson.getString("refresh_token"));
						
						// ExpiresIn
						_feedlyclient.setExpiresIn(returnedjson.getInt("expires_in"));
						_feedlyclient.addIntegerToPersistentDB("expires_in", _feedlyclient.getExpiresIn());
						//updateStatus("expires_in:" + returnedjson.getInt("expires_in"));
						
						// Update
						_feedlyclient.setUpdate(System.currentTimeMillis());
						_feedlyclient.addLongToPersistentDB("update", _feedlyclient.getUpdate());
						//updateStatus("update:" + _feedlyclient.getUpdate());
						
						//
						// Homeステイトを移動
						//
						_feedlyclient.changeStateToHomeState();
					}
				} catch (Exception e) {
					// エラーをロギング
					updateStatus("getAccessToken() " + e.toString());
					
					UiApplication.getUiApplication().invokeLater(new Runnable()
					{
						public void run()
						{
							//Dialog.alert("A communication error has occurred. Please make sure your device is connected to Internet.");
							int select = Dialog.ask("A communication error has occurred. Please make sure your device is connected to Internet.", new Object[]{"Try again","Quit"}, 0);
							
							switch(select)
							{
								case -1:
									_feedlyclient.quitApp();
									break;
								
								case 0:
									_feedlyclient.reStartApp();
									break;
									
								case 1:
									_feedlyclient.quitApp();
									break;
							}
						} //run()
					});
				}
			} //run()
		}.start(); //Thread()
	} //getAccessToken
	
	
	
	//
	//-- Implement BrowserFieldListener ------------------------------------------//
	//
	// Override BrowserFieldListener
	public void documentCreated(BrowserField browserField, ScriptEngine scriptEngine, Document document)
			throws Exception
	{
		super.documentCreated(browserField, scriptEngine, document);
		//updateStatus("documentCreated");
		//_authscreen.showActivityIndicator();
		
		
		// 表示するURLを取得
		String url = document.getDocumentURI();
		
		// [Handling the response]
		// An error response:
		// https://your.redirect.uri/feedlyCallback?error=access_denied&state=state.passed.in
		// A code response
		// https://your.redirect.uri/feedlyCallback?code=AQAA7rJ7InAiOjEsImEiOiJmZWVk…&state=state.passed.in
		if(url.startsWith("https://localhost" + "/?code=")) {
			// 認証成功
			_authscreen.showCompliteMessage();
			
			// Get code
			int start = url.indexOf("code=") + "code=".length();
			int end = 	url.indexOf("&state=");
			String code = url.substring(start, end);
			
			getAccessToken(code);
			
		} else if(document.getDocumentURI().startsWith("https://localhost" + "/?error=")) {
			// 認証失敗
			
			// アクティビティインジケーターを削除
			_authscreen.deleteActivityIndicator();
			
			// 余計なメッセージが表示されるので、BrowserFieldを削除。
			_authscreen.deleteBrowserField();
			
			UiApplication.getUiApplication().invokeLater(new Runnable()
			{
				public void run()
				{
					//Dialog.alert("A communication error has occurred. Please make sure your device is connected to Internet.");
					int select = Dialog.ask("Authentication failed.", new Object[]{"Try again","Quit"}, 0);
					
					switch(select)
					{
						case -1:
							_feedlyclient.quitApp();
							break;
						
						case 0:
							_feedlyclient.reStartApp();
							break;
							
						case 1:
							_feedlyclient.quitApp();
							break;
					}
				} //run()
			});
		}
	} //documentCreated()
	
	
	// Override BrowserFieldListener
	public void documentLoaded(BrowserField browserField, Document document) throws Exception
	{
		// the document has loaded, do something ...
		super.documentLoaded(browserField, document);
		//updateStatus("documentLoaded");
		
		// アクティビティインジケーターを削除
		_authscreen.deleteActivityIndicator();
		
		//
		// 通信エラーが発生した場合は、エラーメッセージが帰ってくる。
		// それを判別してエラーダイアログを出す。
		//
		Element element = document.getDocumentElement();
		//updateStatus("NodeName:" + element.getNodeName());
		//updateStatus("NodeType:" + element.getNodeType());
		//updateStatus("NodelocalName:" + element.getLocalName());
		
		NodeList _nodeList = element.getElementsByTagName("body");
		//updateStatus("NodeLocalName:" + _nodeList.item(0).getLocalName());
		//updateStatus("NodeValue:" + _nodeList.item(0).getNodeValue());
		//updateStatus("StringValue:" + _nodeList.item(0).getTextContent());
		
		String contents = _nodeList.item(0).getTextContent();

		// 固定フレーズが含まれている場合は、通信エラーが発生したとみなし、ダイアログを出す。
		if(contents.indexOf("Could not select proper Transport Descriptor") != -1)
		{
			// 余計なメッセージが表示されるので、BrowserFieldを削除。
			_authscreen.deleteBrowserField();
			
			UiApplication.getUiApplication().invokeLater(new Runnable()
			{
				public void run()
				{
					//Dialog.alert("A communication error has occurred. Please make sure your device is connected to Internet.");
					int select = Dialog.ask("A communication error has occurred. Please make sure your device is connected to Internet.", new Object[]{"Try again","Quit"}, 0);
					
					switch(select)
					{
						case -1:
							_feedlyclient.quitApp();
							break;
						
						case 0:
							_feedlyclient.reStartApp();
							break;
							
						case 1:
							_feedlyclient.quitApp();
							break;
					}
				} //run()
			});
		}
	} //documentLoaded()
}