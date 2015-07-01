/*
	Screen_Stream.java
	
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

import net.rim.blackberry.api.sendmenu.SendCommandMenu;
import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.Ui;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.ButtonField;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.component.SeparatorField;
import net.rim.device.api.ui.component.StandardTitleBar;
import net.rim.device.api.ui.component.table.RichList;
import net.rim.device.api.ui.component.table.TableController;
import net.rim.device.api.ui.container.MainScreen;
import net.rim.device.api.ui.container.VerticalFieldManager;
import net.rim.device.api.ui.decor.BackgroundFactory;
import net.rim.device.api.util.StringProvider;


public class Screen_Stream extends MainScreen
{
	private State_Stream _state = null;
	private MyActivityIndicator _activity_indicator = null;
	
	private StandardTitleBar _titleBar = null;
	
	private VerticalFieldManager _mainVFM = null;
	private RichList _richList = null;
	
	private ButtonField _buttonField_markAllAsRead = null;
	
	public Screen_Stream(State_Stream state)
	{
		this._state = state;
		
		// タイトルバーを作成
		_titleBar = new StandardTitleBar();
		_titleBar.addSignalIndicator();
		_titleBar.addNotifications();
		_titleBar.addIcon("img/icon.png");
		setTitleBar(_titleBar);
		
		//
		// メニューアイテムを作成
		//
		// ステイト変更コマンド
		/*
		MenuItem all = new MenuItem(new StringProvider("Home") , 0x130010, 0);
		all.setCommand(_state.changeStateToHomeCMD());
		addMenuItem(all);
		
		MenuItem saved = new MenuItem(new StringProvider("Saved") , 0x130011, 0);
		saved.setCommand(_state.changeStateToHomeCMD());
		addMenuItem(saved);
		*/
		// ユーティリティ
		MenuItem refresh = new MenuItem(new StringProvider("Refresh") , 0x230010, 0); 
		refresh.setCommand(_state.CMD_refresh());
		addMenuItem(refresh);
		
		MenuItem toggle_unread_read = new MenuItem(new StringProvider("Toggle Unread/Read") , 0x230011, 0);
		toggle_unread_read.setCommand(_state.CMD_toggleUnreadAndRead());
		addMenuItem(toggle_unread_read);
		
		MenuItem toggle_unsaved_saved = new MenuItem(new StringProvider("Toggle Unsaved/Saved") , 0x230012, 0);
		toggle_unsaved_saved.setCommand(_state.CMD_toggleUnsavedAndSaved());
		addMenuItem(toggle_unsaved_saved);
		
		MenuItem make_all_entries_as_read = new MenuItem(new StringProvider("Make all entries as Read") , 0x230013, 0);
		make_all_entries_as_read.setCommand(_state.CMD_makeAllEntriesAsRead());
		addMenuItem(make_all_entries_as_read);
		
		MenuItem unreadonly = new MenuItem(new StringProvider("Toggle Show/Hide Read") , 0x230014, 0);
		unreadonly.setCommand(_state.CMD_toggleShowAndHideRead());
		addMenuItem(unreadonly);
		
		
		// 
		_mainVFM = new VerticalFieldManager(Manager.NO_VERTICAL_SCROLL)
		{
			protected boolean keyChar(char ch, int status, int time)
			{
				switch(ch)
				{
					case 'a':
					{
						// すべて既読にする
						_state.CMD_makeAllEntriesAsRead().execute("");
						break;
					}
			
					//case  'b':
						// 一番最後のエントリーにフォーカスしたい。
						// いろいろ試したが、方法わからず。
						//break;
					
					case  'm':
						// Unread/Readをトグル
						_state.CMD_toggleUnreadAndRead().execute("");
						break;
					
					case  'r':
						// 未読数表示を更新
						_state.CMD_refresh().execute("");
						break;
						
					case  's':
						// Savedタグを付けたり消したり。
						_state.CMD_toggleUnsavedAndSaved().execute("");
						break;
						
					//case  't':
						// 一番はじめのエントリーにフォーカスしたい。
						// いろいろ試したが、方法わからず。
						//break;
				}
				
				return super.keyChar(ch, status, time);
			} //keyChar()
		};
		add(_mainVFM);
		
		
		// リッチリストを使う。
		_richList = new RichList(_mainVFM, false, 1, 0);
		_richList.setCommand(_state.CMD_clickedTableRow());
		_richList.setFocusPolicy(TableController.ROW_FOCUS);
		_richList.getView().setDataTemplateFocus(BackgroundFactory.createSolidBackground(Color.LIGHTBLUE));
		_richList.getView().setFocusListener(_state);
		
		// アクティビティインジケーターを作成
		_activity_indicator = new MyActivityIndicator(this);
		
		// すべて既読ボタンを作成。
		_buttonField_markAllAsRead = new ButtonField("Mark all as read", Field.FIELD_HCENTER | ButtonField.NEVER_DIRTY | ButtonField.CONSUME_CLICK);
		_buttonField_markAllAsRead.setCommand(_state.CMD_makeAllEntriesAsRead());
		_buttonField_markAllAsRead.setMargin(20, 0, 20, 0);
		
	} //StreamScreen()
	
	
	protected void makeMenu(Menu menu, int instance)
	{
		super.makeMenu(menu, instance);
		
		// Sendメニューを作成
		try {
			String text = _state.makeTextForSendMenu(getRowNumberWithFocus());
			SendCommandMenu _sendCommandMenu = _state.makeSendCommandMenu(text);
			if(_sendCommandMenu != null)
			{
				menu.add(_sendCommandMenu);
			}
		} catch (Exception e) {
			//PASS
		}
		
		// 必要な場合は'Get more entries'メニューを作成
		if(_state.isAvailableMoreEntries())
		{
			MenuItem _menuItem = new MenuItem(new StringProvider("Get more entries") , 0x230009, 0);
			_menuItem.setCommand(_state.CMD_getMoreEntries());
			menu.add(_menuItem);
		}
	} //makeMenu()
	
	
	public void addMarkAllAsReadButton()
	{
		try {
			_mainVFM.add(_buttonField_markAllAsRead);
		} catch (IllegalStateException e) {
			// DO NOTHING（もう追加されていれば、それで良い）
		}
	} //addMarkAllAsReadButtonToMainVFM()
	
	
	public void addRowToRichList(final int index, String title, String feed_title, String update)
	{
		VerticalFieldManager _vfm = new VerticalFieldManager(Manager.NO_VERTICAL_SCROLL);

		// タイトル
		LabelField _field0 = new LabelField(title, Field.NON_FOCUSABLE | LabelField.ELLIPSIS)
		{
			protected void paint(Graphics g)
			{
				if(_state.isUnreadEntry(index)) {
					g.setColor(Color.BLACK);
				} else {
					g.setColor(Color.GRAY);
				}
				
				super.paint(g);
			}
		};
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
		_field1.setFont(Font.getDefault().derive(Font.PLAIN, (Font.getDefault().getHeight(Ui.UNITS_pt) - 1), Ui.UNITS_pt));
		_field1.setMargin(0, 10, 0, 10);
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
		_field2.setFont(Font.getDefault().derive(Font.PLAIN, (Font.getDefault().getHeight(Ui.UNITS_pt) - 1), Ui.UNITS_pt));
		_field2.setMargin(0, 10, 10, 10);
		_vfm.add(_field2);
		
		// 区切り線
		SeparatorField _sep = new SeparatorField();
		_sep.setMargin(0, 5, 0, 5);
		_vfm.add(_sep);
		
		Object[] rowObjects = new Object[]{_vfm};
		
		_richList.add(rowObjects);
	} //addRowToRichList()
	
	
	public void close()
	{
		_mainVFM.deleteAll();
		
		// スクリーンを閉じず、Homeステイトへ移動する。
		_state.CMD_changeStateToHome().execute("");
	} //close()
	
	
	public void deleteActivityIndicator()
	{
		_activity_indicator.hideActivityIndicator();
	}
	
	
	public int getRowNumberWithFocus()
	{
		return _richList.getFocusRow();
	}
	
	
	public void refreshRichList()
	{
		_richList.getView().invalidate();
	}
	
	
	public void removeAllEntriesFromRichList()
	{
		int total_num_of_rows = _richList.getModel().getNumberOfRows();
		if(total_num_of_rows != 0)
		{
			_richList.getModel().removeRowRangeAt(0, total_num_of_rows);
		}
		
		// すべて既読ボタンも取り除く
		removeMarkAllAsReadButton();
	}
	
	
	public void removeMarkAllAsReadButton()
	{
		try {
			synchronized (UiApplication.getEventLock()) 
			{
				_mainVFM.delete(_buttonField_markAllAsRead);
			}
		} catch (IllegalArgumentException e) {
			// 存在しなければ、それでよい。
		}
	}
	
	
	public void setScreenTitle(String title)
	{
		synchronized (UiApplication.getEventLock()) 
		{
			_titleBar.addTitle(title);
		}
	}
	
	
	public void showActivityIndicator()
	{
		_activity_indicator.showActivityIndicator();
	}
	
	
	public void updateSavedAndUpdatedField(int rowIndex)
	{
		// 表示中のリッチリストより目的のフィールドを取得。
		Object[] obj = _richList.get(rowIndex);
		VerticalFieldManager _vfm = (VerticalFieldManager)obj[0];
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
}

