/*
	MyScreen.java
	
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

import net.rim.device.api.command.Command;
import net.rim.device.api.command.CommandHandler;
import net.rim.device.api.command.ReadOnlyCommandMetadata;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.BasicEditField;
import net.rim.device.api.ui.component.StandardTitleBar;
import net.rim.device.api.ui.component.progressindicator.ActivityIndicatorController;
import net.rim.device.api.ui.component.progressindicator.ActivityIndicatorModel;
import net.rim.device.api.ui.component.progressindicator.ActivityIndicatorView;
import net.rim.device.api.ui.container.MainScreen;
import net.rim.device.api.util.StringProvider;


/**
 * A class extending the MainScreen class, which provides default standard
 * behavior for BlackBerry GUI applications.
 */
public final class MyScreen extends MainScreen
{
	private MyApp _app = null;
	
	private BasicEditField _statusField;
	
	/**
	 * Creates a new MyScreen object
	 */
	public MyScreen()
	{
		_app = (MyApp) UiApplication.getUiApplication();
		
		// Set the displayed title of the screen
		StandardTitleBar _titleBar = new StandardTitleBar();
		_titleBar.addSignalIndicator();
		_titleBar.addNotifications();
		_titleBar.addTitle("Green Feed Reader");
		_titleBar.addIcon("img/icon.png");
		setTitleBar(_titleBar);
		
		_statusField = new BasicEditField(Field.READONLY);
		//add(_statusField);
		
		
		MenuItem save = new MenuItem(new StringProvider("Return to Home") , 0x230010, 0); 
		save.setCommand(new Command(new CommandHandler() 
		{
			/**
			 * @see net.rim.device.api.command.CommandHandler#execute(ReadOnlyCommandMetadata, Object)
			 */
			public void execute(ReadOnlyCommandMetadata metadata, Object context) 
			{
				deleteAll();
				_app.showHomeScreen();
			}
		}));
		addMenuItem(save);
	}
	
	
	// Overrides: close() in Screen
	/*public void close()
	{
		int select = Dialog.ask(Dialog.D_OK_CANCEL, "終了してもよろしいですか?", Dialog.NO);
		
		if(select == Dialog.NO)
		{
			return;
		}
		
		super.close();
	}*/ //close()
	
	
	public void showLogField()
	{
		add(_statusField);
	}
		
		
	public void updateStatus(final String message)
	{
		UiApplication.getUiApplication().invokeLater(new Runnable()
		{
			public void run()
			{
				_statusField.setText(_statusField.getText() + "\n" + message);
			}
		});
	} //updateStatus()
}

class MyActivityIndicator
{
	private MainScreen _screen = null;
	private ActivityIndicatorView _activity_indicator_view = null;
	private int count = 0;
	
	public MyActivityIndicator(MainScreen _screen)
	{
		this._screen = _screen;
		
		// アクティビティインジケーターを作成
		_activity_indicator_view = new ActivityIndicatorView(Field.USE_ALL_WIDTH);
		ActivityIndicatorModel _aiModel = new ActivityIndicatorModel();
		ActivityIndicatorController _aiController = new ActivityIndicatorController();
		
		_activity_indicator_view.setController(_aiController);
		_activity_indicator_view.setModel(_aiModel);
		_activity_indicator_view.setMargin(5, 0, 5, 0);
		
		_aiController.setModel(_aiModel);
		_aiController.setView(_activity_indicator_view);
		_aiModel.setController(_aiController);
		_activity_indicator_view.createActivityImageField(Bitmap.getBitmapResource("spinner.png"), 6, Field.FIELD_HCENTER);
		
		count = 0;
	}
	
	
	public void showActivityIndicator()
	{
		count++;
		updateActivityIndicator();
	}
	
	
	public void hideActivityIndicator()
	{
		if(count != 0)
		{
			count--;
		}
		updateActivityIndicator();
	}
	
	
	private void updateActivityIndicator()
	{
		if(count > 0)
		{
			synchronized (UiApplication.getEventLock()) 
			{
				_screen.setStatus(_activity_indicator_view);
			}
		} else {
			synchronized (UiApplication.getEventLock()) 
			{
				_screen.setStatus(null);
			}
		}
	} //updateActivityIndicator()
}//MyActivityIndicator

