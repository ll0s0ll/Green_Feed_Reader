/*
	Screen_Entry.java
	
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

import java.io.UnsupportedEncodingException;

import net.rim.blackberry.api.browser.Browser;
import net.rim.blackberry.api.browser.BrowserSession;
import net.rim.blackberry.api.sendmenu.SendCommandMenu;
import net.rim.device.api.browser.field2.BrowserField;
import net.rim.device.api.browser.field2.BrowserFieldConfig;
import net.rim.device.api.browser.field2.BrowserFieldNavigationRequestHandler;
import net.rim.device.api.browser.field2.BrowserFieldRequest;
import net.rim.device.api.browser.field2.ProtocolController;
import net.rim.device.api.command.Command;
import net.rim.device.api.command.CommandHandler;
import net.rim.device.api.command.ReadOnlyCommandMetadata;
import net.rim.device.api.system.Display;
import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Keypad;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.Ui;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.component.SeparatorField;
import net.rim.device.api.ui.component.StandardTitleBar;
import net.rim.device.api.ui.container.MainScreen;
import net.rim.device.api.ui.container.VerticalFieldManager;
import net.rim.device.api.util.StringProvider;


public class Screen_Entry extends MainScreen
{
	private State_Stream _state = null;
	private MyActivityIndicator _activity_indicator = null;
	
	private VerticalFieldManager _mainVFM = null;
	private BrowserField _browserField;
	private StandardTitleBar _titleBar = null;
	
	private int index = 0;
	private String url = "";
	
	public Screen_Entry(final State_Stream _state)
	{
		this._state = _state;
		
		//
		// タイトルバーを作成
		//
		_titleBar = new StandardTitleBar();
		_titleBar.addSignalIndicator();
		_titleBar.addNotifications();
		_titleBar.addIcon("img/icon.png");
		setTitleBar(_titleBar);
		
		//
		// メニューアイテムを作成
		//
		/*MenuItem show_instapaper_mobilizer_ver = new MenuItem(new StringProvider("View Website via Instapaper Mobilizer") , 0x220010, 0);
		show_instapaper_mobilizer_ver.setCommand(new Command(new CommandHandler() 
		{
			public void execute(ReadOnlyCommandMetadata metadata, Object context) 
			{
				String insta_url = "http://mobilizer.instapaper.com/m?u=" + url;
				//_browserField.requestContent(insta_url);
				Browser.getDefaultSession().displayPage(insta_url);
			}
		}));
		addMenuItem(show_instapaper_mobilizer_ver);*/
		
		MenuItem toggle_unsaved_saved = new MenuItem(new StringProvider("Toggle Unsaved/Saved") , 0x230012, 0);
		toggle_unsaved_saved.setCommand(_state.CMD_toggleUnsavedAndSaved());
		addMenuItem(toggle_unsaved_saved);
		
		MenuItem _visitWebsite = new MenuItem(new StringProvider("Visit Website") , 0x230013, 0); 
		_visitWebsite.setCommand(new Command(new CommandHandler() 
		{
			public void execute(ReadOnlyCommandMetadata metadata, Object context) 
			{
				Browser.getDefaultSession().displayPage(url);
			}
		}));
		addMenuItem(_visitWebsite);
		
		
		//
		// メインマネージャー作成。
		//
		_mainVFM = new VerticalFieldManager()
		{
			protected boolean keyChar(char ch, int status, int time)
			{
				switch(ch)
				{
					case Keypad.KEY_SPACE:
						// オリジナルコンテンツをデフォルトブラウザで表示
						Browser.getDefaultSession().displayPage(url);
						break;
						
					case 'b':
						// 最下部へスクロール。
						getMainManager().setVerticalScroll(_mainVFM.getContentHeight()-_mainVFM.getVisibleHeight(), false);
						break;
						
					case 'n':
						// 次のエントリーを表示
						_state.CMD_displayNextEntry().execute("");
						break;
					
					case 'p':
						// 前のエントリーを表示
						_state.CMD_displayPrevEntry().execute("");
						break;
						
					case 's':
						// Savedタグを付けたり消したり。
						_state.CMD_toggleUnsavedAndSaved(index).execute("");
						break;
						
					case 't':
						// 最上部へスクロール。
						getMainManager().setVerticalScroll(Manager.TOPMOST, false);
						break;
				}
				
				return super.keyChar(ch, status, time);
			} //keyChar()
		};
		add(_mainVFM);
		
		
		//
		// ダミーのヘッダを作成。
		//
		_mainVFM.add(new VerticalFieldManager());
		
		
		//
		// ブラウザフィールドを作成。
		//
		// BrowserField Sample Code - Using the BrowserFieldC... - BlackBerry Support Community Forums
		// http://supportforums.blackberry.com/t5/Java-Development/BrowserField-Sample-Code-Using-the-BrowserFieldConfig-class/ta-p/495716		
		_browserField = new BrowserField();
		
		// Use the default (full-featured) browser to view external http content
		// See..
		// BrowserField - open links in actual browser? - BlackBerry Support Community Forums
		// http://supportforums.blackberry.com/t5/Java-Development/BrowserField-open-links-in-actual-browser/m-p/468721
		ProtocolController hybridController = new ProtocolController(_browserField);
		hybridController.setNavigationRequestHandler("http", new BrowserFieldNavigationRequestHandler() {
			public void handleNavigation(BrowserFieldRequest request)
			{
				BrowserSession browser = Browser.getDefaultSession();
				browser.displayPage(request.getURL());
				
				/*String requestURL = request.getURL();
				if(requestURL.startsWith("http://mobilizer.instapaper.com/m?u="))
				{
					_browserField.requestContent(request);
					return;
				}*/
			}
		});
		hybridController.setNavigationRequestHandler("https", new BrowserFieldNavigationRequestHandler() {
			public void handleNavigation(BrowserFieldRequest request)
			{
				BrowserSession browser = Browser.getDefaultSession();
				browser.displayPage(request.getURL());
			}
		});
		_browserField.getConfig().setProperty(BrowserFieldConfig.CONTROLLER, hybridController);
		
		//
		_browserField.getConfig().setProperty(BrowserFieldConfig.CONNECTION_FACTORY, _state.getConnectionFactory());
		
		// ナビゲーションモードをポインタに設定
		_browserField.getConfig().setProperty(BrowserFieldConfig.NAVIGATION_MODE, BrowserFieldConfig.NAVIGATION_MODE_POINTER);
		
		// オートフォーカスを解除
		_browserField.getConfig().setProperty(BrowserFieldConfig.DISABLE_AUTO_FOCUS, Boolean.TRUE);
		
		// ビューポートを設定
		_browserField.getConfig().setProperty(BrowserFieldConfig.VIEWPORT_WIDTH, new Integer(Display.getWidth()));
		
		_mainVFM.add(_browserField);
		//add(_browserField);
		
		// アクティビティインジケーターを作成
		_activity_indicator = new MyActivityIndicator(this);
	}
	
	
	protected void makeMenu(Menu menu, int instance)
	{
		super.makeMenu(menu, instance);
		
		// Sendメニューを作成
		try {
			String text = _state.makeTextForSendMenu(index);
			SendCommandMenu _sendCommandMenu = _state.makeSendCommandMenu(text);
			if(_sendCommandMenu != null)
			{
				menu.add(_sendCommandMenu);
			}
		} catch (Exception e) {
			//PASS
		}
		
		// Next、Prevメニューアイテムを追加
		if(_state.isFirstEntry(index) && !_state.isOnlyOneEntry(index)) {
			
			// 前のエントリーはない。次のエントリーはある。
			menu.add(makeNextEntryMenuItem());
			
		} else if(_state.isLastEntry(index) && !_state.isOnlyOneEntry(index)){
			
			// 次のエントリーはない。前のエントリーはある。
			menu.add(makePrevEntryMenuItem());
			
		} else if(_state.isOnlyOneEntry(index)) {
			// PASS
		} else {
			
			// 次のエントリーも前のエントリーもある。
			menu.add(makeNextEntryMenuItem());
			menu.add(makePrevEntryMenuItem());
		}
	} //makeMenu()
	
	
	public void close()
	{
		_mainVFM.deleteAll();
		
		if(_browserField != null) { _browserField = null; }
		
		_state.cleanEntryScreen();
		super.close();
	}
	
	
	public void displayContent(int index, String title, String origin_title, String published, String url, String html)
	{
		// このエントリーのインデックスを保存
		this.index = index;
		
		// 全文表示リンクを保存
		this.url = url;
		
		// スクリーンタイトルを更新
		synchronized (UiApplication.getEventLock())
		{
			_titleBar.addTitle(title);
		}
		
		
		// 新しいヘッダを作成して、古いヘッダと入れ替える
		synchronized (UiApplication.getEventLock())
		{
			VerticalFieldManager _oldHeader = (VerticalFieldManager)_mainVFM.getField(0);
			VerticalFieldManager _newHeader = makeHeader(index, title, origin_title, published);
			_mainVFM.replace(_oldHeader, _newHeader);
		}
		
		// ブラウザフィールドにHTMLを読み込み
		// BrowserField Encoding problem - BlackBerry Support Community Forums
		// http://supportforums.blackberry.com/t5/Java-Development/BrowserField-Encoding-problem/td-p/1428779
		try {
			_browserField.displayContent(html.getBytes("utf-8"), "text/html; charset=utf-8", "http://localhost/");
		} catch (UnsupportedEncodingException e) {
			// PASS
		}
		
		// 最上部へスクロールさせておく。
		getMainManager().setVerticalScroll(Manager.TOPMOST, false);
		
	} //displayContent()
	
	
	public int getIndexOfEntry()
	{
		return index;
	}
	
	
	public void hideActivityIndicator()
	{
		_activity_indicator.hideActivityIndicator();
	}
	
	
	public void showActivityIndicator()
	{
		_activity_indicator.showActivityIndicator();
	}
	
	
	public void updateSavedAndUpdatedField(int rowIndex)
	{
		// 表示中のリッチリストより目的のフィールドを取得。
		//Object[] obj = _richList.get(rowIndex);
		VerticalFieldManager _vfm = (VerticalFieldManager)_mainVFM.getField(0);
		LabelField _field = (LabelField)_vfm.getField(2);
		
		// 現在表示中のテキストを取得。'Saved / 'が含まれている場合は削除する。
		String org_str = _field.getText();
		if(org_str.startsWith("Saved / "))
		{
			org_str = org_str.substring(org_str.lastIndexOf('/')+2, org_str.length());
		}
		
		// Savedフラグに応じたテキストを適応する。
		if(_state.isSavedEntry(rowIndex)) {
			_field.setText("Saved / " + org_str);
		} else {
			_field.setText(org_str);
		}
	} //updateSavedAndUpdatedField()
	
	
	private VerticalFieldManager makeHeader(final int index, String title, String feed_title, String update)
	{
		VerticalFieldManager _vfm = new VerticalFieldManager(Manager.NO_VERTICAL_SCROLL);
		
		// タイトル
		LabelField _field0 = new LabelField(title, Field.NON_FOCUSABLE)
		{
			protected void paint(Graphics g)
			{
				//if(_state.isUnreadEntry(index)) {
					g.setColor(Color.BLACK);
				//} else {
					//g.setColor(Color.GRAY);
				//}
				
				super.paint(g);
			}
		};
		_field0.setFont(Font.getDefault().derive(Font.BOLD, (Font.getDefault().getHeight(Ui.UNITS_px)+1), Ui.UNITS_px));
		_field0.setMargin(10, 10, 0, 10);
		_vfm.add(_field0);
		
		
		// フィード名
		LabelField _field1 = new LabelField(feed_title, Field.NON_FOCUSABLE | LabelField.ELLIPSIS)
		{
			protected void paint(Graphics g)
			{
				g.setColor(Color.GRAY);
				super.paint(g);
			}
		};
		_field1.setFont(Font.getDefault().derive(Font.PLAIN, (Font.getDefault().getHeight(Ui.UNITS_px)-5), Ui.UNITS_px));
		_field1.setMargin(5, 10, 5, 10);
		_vfm.add(_field1);
		
		
		// パブリッシュからの経過時間
		if(_state.isSavedEntry(index))
		{ update = "Saved / " + update; }
		
		LabelField _field2 = new LabelField(update, Field.NON_FOCUSABLE | LabelField.ELLIPSIS | Field.USE_ALL_WIDTH)
		{
			protected void paint(Graphics g)
			{
				if(_state.isSavedEntry(index)) {
					g.setColor(Color.GREEN);
				} else {
					g.setColor(Color.GRAY);
				}
				super.paint(g);
			}
		};
		_field2.setFont(Font.getDefault().derive(Font.PLAIN, (Font.getDefault().getHeight(Ui.UNITS_px)-5), Ui.UNITS_px));
		_field2.setMargin(0, 10, 10, 10);
		_vfm.add(_field2);
		
		// 区切り線
		SeparatorField _sep = new SeparatorField();
		_sep.setMargin(0, 5, 0, 5);
		_vfm.add(_sep);
		
		return _vfm;
	} //makeHeader()
	
	
	private MenuItem makeNextEntryMenuItem()
	{
		MenuItem out = new MenuItem(new StringProvider("Next") , 0x230010, 0);
		out.setCommand(_state.CMD_displayNextEntry());
		return out;
	}
	
	
	private MenuItem makePrevEntryMenuItem()
	{
		MenuItem out = new MenuItem(new StringProvider("Prev") , 0x230011, 0); 
		out.setCommand(_state.CMD_displayPrevEntry());
		return out;
	}
}