/*
	Screen_Auth.java
	
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

import mypackage.MyActivityIndicator;

import net.rim.device.api.browser.field2.BrowserField;
import net.rim.device.api.browser.field2.BrowserFieldConfig;
import net.rim.device.api.browser.field2.BrowserFieldRequest;
import net.rim.device.api.browser.field2.ProtocolController;
import net.rim.device.api.io.transport.ConnectionFactory;
import net.rim.device.api.system.Display;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.component.StandardTitleBar;
import net.rim.device.api.ui.container.MainScreen;


public class Screen_Auth extends MainScreen
{
	private State_Auth _state = null;
	private MyActivityIndicator _activity_indicator = null;
	
	private BrowserField _browserField;
	
	
	public Screen_Auth(State_Auth state)
	{
		this._state = state;
	
		//---- タイトルバーを作成
		StandardTitleBar _titleBar = new StandardTitleBar();
		_titleBar.addSignalIndicator();
		_titleBar.addNotifications();
		_titleBar.addTitle("Login");
		_titleBar.addIcon("img/icon.png");
		setTitleBar(_titleBar);
		
		
		_browserField = new BrowserField();
		
		ConnectionFactory _connectionFactory = _state.getConnectionFactory();
		_browserField.getConfig().setProperty(BrowserFieldConfig.CONNECTION_FACTORY, _connectionFactory);
		
		
		// ナビゲーションモードをポインタに設定
		_browserField.getConfig().setProperty(BrowserFieldConfig.NAVIGATION_MODE, BrowserFieldConfig.NAVIGATION_MODE_POINTER);
		
		// オートフォーカスを解除
		_browserField.getConfig().setProperty(BrowserFieldConfig.DISABLE_AUTO_FOCUS, Boolean.TRUE);
		
		// オートフォーカスを解除
		_browserField.getConfig().setProperty(BrowserFieldConfig.VIEWPORT_WIDTH, new Integer(Display.getWidth()));

		// How to Implement a Web Cache for Your BrowserField... - BlackBerry Support Community Forums
		// http://supportforums.blackberry.com/t5/Java-Development/How-to-Implement-a-Web-Cache-for-Your-BrowserField2-Application/ta-p/817911
		_browserField.getConfig().setProperty(BrowserFieldConfig.CONTROLLER, new ProtocolController(_browserField)
		{
			public void handleNavigationRequest(BrowserFieldRequest request) throws Exception
			{
				showActivityIndicator();
				super.handleNavigationRequest(request);
			}
		});
		
		// リスナ追加
		_browserField.addListener(_state);
		
		// 
		add(_browserField);
		
		// アクティビティインジケーターを作成
		_activity_indicator = new MyActivityIndicator(this);
	}
	
	
	// Overrides: close() in Screen
	public void close()
	{
		// 認証しないでスクリーンを閉じる場合は、確認ダイアログを出し、アプリを終了する。
		_state.close();
		
	} //close()
		
		
	public void deleteActivityIndicator()
	{
		_activity_indicator.hideActivityIndicator();
	}
	
	
	public void deleteBrowserField()
	{
		delete(_browserField);
	}
	
	public void requestContent(String url)
	{
		_browserField.requestContent(url);
	}
	
	
	public void showActivityIndicator()
	{
		_activity_indicator.showActivityIndicator();
	}
	
	
	public void showCompliteMessage()
	{
		delete(_browserField);
		add(new LabelField("Authentication is complete.\nPlease wait..."));
	}
}

