/*
	MyApp.java
	
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

import net.rim.device.api.ui.UiApplication;

/**
 * This class extends the UiApplication class, providing a
 * graphical user interface.
 */
public class MyApp extends UiApplication
{
	private MyScreen _screen = null;
	private FeedlyClient _feedlyclient = null;
	
	
	/**
	 * Entry point for application
	 * @param args Command line arguments (not used)
	 */ 
	public static void main(String[] args)
	{
		// Create a new instance of the application and make the currently
		// running thread the application's event dispatch thread.
		MyApp theApp = new MyApp();
		theApp.enterEventDispatcher();
	}


	/**
	 * Creates a new MyApp object
	 */
	public MyApp()
	{
		_screen = new MyScreen();
		
		// Push a screen onto the UI stack for rendering.
		pushScreen(_screen);
		
		_feedlyclient = new FeedlyClient();
		_feedlyclient.getCurrentState().enter();
		
		//-- テスト用品 -------------------------------------------------------------//
		/*Calendar today = Calendar.getInstance();
		int newMonth = today.get(Calendar.MONTH);
		int newYear = today.get(Calendar.YEAR);
		
		if(newMonth == Calendar.AUGUST && newYear == 2014) {
			UiApplication.getUiApplication().invokeLater(new Runnable()
			{
				public void run()
				{
					Dialog.alert("テストにご協力いただきありがとうございます。このアプリはテスト版のため、使用期限が2014年8月末までとなっております。");
					_feedlyclient.getCurrentState().enter();
				}
			});
		} else {
			UiApplication.getUiApplication().invokeLater(new Runnable()
			{
				public void run()
				{
					Dialog.alert("このアプリはテスト版のため、使用期限が2014年8月末までとなっております。使用期限が切れたためアプリを終了します。");
					_feedlyclient.quitApp();
				}
			});
		}*/
		//--------------------------------------------------------------------------//
	}
	
	
	public void quitApp()
	{
		_feedlyclient = null;
		_screen.close();
	}
	
	
	public void reStartApp()
	{
		_feedlyclient = null;
		_feedlyclient = new FeedlyClient();
		_feedlyclient.getCurrentState().enter();
	}
	
	
	public void showHomeScreen()
	{
		_feedlyclient.showHomeScreen();
	}
	
	
	public void showLogField()
	{
		_screen.showLogField();
	}
	
	
	public void updateStatus(String val)
	{
		_screen.updateStatus(val);
	}
}
