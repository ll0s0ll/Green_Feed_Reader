/*
	Screen_Home.java
	
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
import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.Ui;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.XYEdges;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.component.StandardTitleBar;
import net.rim.device.api.ui.component.table.RichList;
import net.rim.device.api.ui.component.table.TableController;
import net.rim.device.api.ui.container.MainScreen;
import net.rim.device.api.ui.container.VerticalFieldManager;
import net.rim.device.api.ui.decor.BackgroundFactory;
import net.rim.device.api.ui.decor.Border;
import net.rim.device.api.ui.decor.BorderFactory;
import net.rim.device.api.ui.menu.SubMenu;
import net.rim.device.api.util.StringProvider;


public class Screen_Home extends MainScreen
{
	private State_Home _state = null;
	
	private VerticalFieldManager _mainVFM = null;
	private MyActivityIndicator _activity_indicator = null;
	
	
	public Screen_Home(State_Home state)
	{
		this._state = state;
		
		//
		// タイトルバーを作成
		//
		StandardTitleBar _titleBar = new StandardTitleBar();
		_titleBar.addSignalIndicator();
		_titleBar.addNotifications();
		_titleBar.addTitle("Home");
		_titleBar.addIcon("img/icon.png");
		setTitleBar(_titleBar);
		
		//
		// アクティビティインジケーターを作成
		//
		_activity_indicator = new MyActivityIndicator(this);
		
		//
		// メインマネージャーを作成。
		//
		_mainVFM = new VerticalFieldManager(Manager.NO_VERTICAL_SCROLL)
		{
			protected boolean keyChar(char ch, int status, int time)
			{
				switch(ch)
				{
					// Feedを表示するかしないかを切り替え
					case 'f':
						_state.CMD_toggleShowAndHideFeeds().execute("");
						break;
					
					// 未読数表示を更新
					case 'r':
						_state.CMD_refresh().execute("");
						break;
				}
				
				return super.keyChar(ch, status, time);
			} //keyChar()
		};
		add(_mainVFM);
		
		//Manager mainManager = getMainManager();
	}
	
	
	protected void makeMenu( Menu menu, int instance )
	{
		/*
		MenuItem all = new MenuItem(new StringProvider("ALL") , 0x130010, 0);
		all.setCommand(_state.CMD_changeStateToAll());
		menu.add(all);
		
		MenuItem saved = new MenuItem(new StringProvider("Saved") , 0x130011, 0);
		saved.setCommand(_state.CMD_changeStateToSaved());
		menu.add(saved);
		*/
		
		MenuItem refresh = new MenuItem(new StringProvider("Refresh") , 0x230010, 0);
		refresh.setCommand(_state.CMD_refresh());
		menu.add(refresh);
		
		MenuItem toggleShowAndHideFeeds = new MenuItem(new StringProvider("Toggle Show/Hide Feeds") , 0x230011, 0);
		toggleShowAndHideFeeds.setCommand(_state.CMD_toggleShowAndHideFeeds());
		menu.add(toggleShowAndHideFeeds);
		
		MenuItem reload = new MenuItem(new StringProvider("Reload") , 0x230012, 0);
		reload.setCommand(_state.CMD_reload());
		menu.add(reload);
		
		MenuItem logout = new MenuItem(new StringProvider("Logout") , 0x230013, 0);
		logout.setCommand(_state.CMD_logout());
		menu.add(logout);
		
		super.makeMenu(menu, instance);
	}; //makeMenu()
	
	
	protected void onUiEngineAttached(boolean attached)
	{
		super.onUiEngineAttached(attached);
		if(attached)
		{	
			//
			// 未読数表示を更新
			//
			
			// 有効な通信経路がない場合はリターン
			if(!Network.isCoverageSufficient()) { return; }
				
			// 更新されるべきエントリーがない場合はリターン
			if(_mainVFM.getFieldCount() == 0) {  return; }
			
			new Thread()
			{
				public void run()
				{
					_state.refreshUnreadCounts();
				}
			}.start();
		}
	} //onUiEngineAttached()
	
	
	protected boolean openProductionBackdoor(int backdoorCode)
	{
		// Use a Backdoor Sequence
		// http://www.blackberryforums.com.au/forums/blackberry-java-development-environment/211-use-backdoor-sequence.html
			
		switch( backdoorCode )
		{
			// BACKDOOR - converts four chars to an int via bit shifting and a bitwise OR
			/*
			case ( 'A' << 24 ) | ( 'B' << 16 ) | ( 'C' << 8 ) | 'D': 
			UiApplication.getUiApplication().invokeLater (new Runnable() {
				public void run() 
				{
					Dialog.inform("Backdoor sequence received");
				}
			}); 
			return true; // handled
			 */
			// ログを表示
			case ( 'L' << 24 ) | ( 'G' << 16 ) | ( 'L' << 8 ) | 'G':
				_state.showLog();
				return true;
		} //switch
		return super.openProductionBackdoor(backdoorCode);
	} //openProductionBackdoor()
	
	
	public RichList addCategory()
	{
		RichList _richList = new RichList(_mainVFM, false, 1, 0);
		_richList.setFocusPolicy(TableController.ROW_FOCUS);
		_richList.getView().setDataTemplateFocus(BackgroundFactory.createSolidBackground(Color.LIGHTBLUE));
		
		return _richList;
	}
	
	
	public void addCategoryHeader(RichList _richList, String feedtitle)
	{
		VerticalFieldManager _vfm = new VerticalFieldManager();
		
		LabelField field = new LabelField("> " + feedtitle, Field.NON_FOCUSABLE | LabelField.ELLIPSIS | LabelField.USE_ALL_WIDTH);
		field.setMargin(10, 15, 10, 15);
		field.setBorder(BorderFactory.createSimpleBorder(new XYEdges(0, 0, 1, 0), new XYEdges(Color.WHITE, Color.WHITE, Color.GRAY, Color.WHITE), Border.STYLE_SOLID));
		_vfm.add(field);
		
		Object[] rowObjects = new Object[]{_vfm};
		
		_richList.add(rowObjects);
	}
	
	
	public void addCategoryRow(RichList _richList, String title, int unread, String update)
	{
		VerticalFieldManager _vfm = new VerticalFieldManager();
		
		LabelField field = new LabelField(title, Field.NON_FOCUSABLE | LabelField.ELLIPSIS);
		field.setMargin(10, 20, 0, 20);
		_vfm.add(field);
		
		// GlobalのSave項目の場合は、Unread&Updatedフィールドは空。
		if(title.equals("Saved For Later"))
		{
			LabelField field2 = new LabelField("", Field.NON_FOCUSABLE);
			field2.setMargin(0, 20, 10, 20);
			field2.setFont(Font.getDefault().derive(Font.PLAIN, (Font.getDefault().getHeight(Ui.UNITS_pt)-1), Ui.UNITS_pt));
			_vfm.add(field2);
			
			Object[] rowObjects = new Object[]{_vfm};
			_richList.add(rowObjects);
			return;
		}
			
			
		LabelField field2 = new LabelField("Unread: " + unread + " / " + "Updated: " + update, Field.NON_FOCUSABLE)
		{
			protected void paint(Graphics g)
			{
				String tmp = this.getText();
				String count = tmp.substring(tmp.indexOf(':')+1, tmp.indexOf('/'));
				//_state.updateStatus(count.trim());
				Integer count_integer  = Integer.valueOf(count.trim());
				
				if(count_integer.intValue() > 0) {
					g.setColor(Color.GREEN);
				} else {
					g.setColor(Color.GRAY);
				}
				super.paint(g);
			}
		};
		field2.setMargin(0, 20, 10, 20);
		field2.setFont(Font.getDefault().derive(Font.PLAIN, (Font.getDefault().getHeight(Ui.UNITS_pt)-1), Ui.UNITS_pt));
		_vfm.add(field2);
		
		Object[] rowObjects = new Object[]{_vfm};
		
		_richList.add(rowObjects);
	} //addCategoryRow()
	
	
	public void close()
	{
		_state.close();
		//super.close();
	}
	
	
	public void deleteActivityIndicator()
	{
		_activity_indicator.hideActivityIndicator();
	}
	
	
	public void deleteAllCategories()
	{
		_mainVFM.deleteAll();
	}
	
	
	public void showActivityIndicator()
	{
		_activity_indicator.showActivityIndicator();
	}
	
	
	public void refreshCategoryHeaderUnread(RichList _richList, String feedtitle, int unread)
	{
		Object[] obj = _richList.get(0);
		VerticalFieldManager _inputed_VFM = (VerticalFieldManager) obj[0];
		LabelField unread_field = (LabelField) _inputed_VFM.getField(0);
		
		synchronized (UiApplication.getEventLock()) 
		{
			if(unread == 0) {
				unread_field.setText("> " + feedtitle);
			} else {
				unread_field.setText("> " + unread + " " + feedtitle);
			}
		}
	} //refreshCategoryHeaderUnread()
	
	
	public void refreshUnreadAndUpdated(RichList _richList, int rowIndex, int unread, String update)
	{
		// 存在しないRowインデックスの場合はリターン
		if(rowIndex > _richList.getModel().getNumberOfRows()-1) { return; };
		
		Object[] obj = _richList.get(rowIndex);
		VerticalFieldManager _inputed_VFM = (VerticalFieldManager) obj[0];
		LabelField unread_field = (LabelField) _inputed_VFM.getField(1);
		
		synchronized (UiApplication.getEventLock()) 
		{
			unread_field.setText("Unread: " + unread + " / " + "Updated: " + update);
		}
	} //refreshUnreadAndUpdated()
}