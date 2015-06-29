/*
	State_Home.java
	
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

import java.util.Enumeration;
import java.util.Vector;

import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;

import net.rim.device.api.command.Command;
import net.rim.device.api.command.CommandHandler;
import net.rim.device.api.command.ReadOnlyCommandMetadata;
import net.rim.device.api.ui.TransitionContext;
import net.rim.device.api.ui.Ui;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.UiEngineInstance;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.table.RichList;




public class State_Home implements State_Base
{
	private FeedlyClient _feedlyclient = null;
	private FeedlyAPI _feedlyapi = null;
	
	private Screen_Home _screen = null;
	
	private Vector categories = null;
	
	public State_Home(FeedlyClient feedlyclient)
	{
		this._feedlyclient = feedlyclient;
		this._feedlyapi = feedlyclient.getFeedlyAPI();
		
		//
		// スクリーントランジションを設定
		//
		// FADE IN
		TransitionContext transIN = new TransitionContext(TransitionContext.TRANSITION_FADE);
		transIN.setIntAttribute(TransitionContext.ATTR_DURATION, 100);
		
		// SET
		UiEngineInstance engine = Ui.getUiEngineInstance();
		engine.setTransition(null, _screen, UiEngineInstance.TRIGGER_PUSH, transIN);
		
		// FADE OUT
		TransitionContext transOUT = new TransitionContext(TransitionContext.TRANSITION_FADE);
		transOUT.setIntAttribute(TransitionContext.ATTR_DURATION, 100);
		transOUT.setIntAttribute(TransitionContext.ATTR_KIND, TransitionContext.KIND_OUT);
		
		// SET
		engine.setTransition(_screen, null, UiEngineInstance.TRIGGER_POP, transOUT);
	}
	
	
	public void enter()
	{
		//updateStatus("enter()");
		
		//updateStatus("[AT] " + _feedlyclient.getAccessToken());
		//updateStatus("[RT] " + _feedlyclient.getRefreshToken());
		//updateStatus("[EI] " + _feedlyclient.getExpiresIn());
		//updateStatus("[UD] " + _feedlyclient.getUpdate());
		
		
		// Feedlyにログインしていない場合は、Authステイトへ変更
		if(!_feedlyclient.isLogin())
		{
			_feedlyclient.changeState(new State_Auth(_feedlyclient));
			return;
		}
		
		// Homeスクリーンを表示
		if(_screen == null)
		{
			_screen = new Screen_Home(this);
		}
		_feedlyclient.pushScreen(_screen);
		
		
		//
		// 購読中のフィードを取得
		//
		// すでに取得している場合は、新たに取得しない。
		if(categories != null) { return; }
		
		
		// 有効な通信経路があるか確認
		if(Network.isCoverageSufficient())
		{	
			new Thread()
			{
				public void run()
				{
					try {
						// アクティビティインジケーターを表示
						_screen.showActivityIndicator();
						
						// 購読中のフィードを取得
						JSONArray subscriptions = _feedlyapi.getUserSubscriptions();
						
						// フィードをカテゴリ分けする
						categories = doCategorize(subscriptions);
						
						// フィードをスクリーンに追加。
						for(Enumeration e = categories.elements(); e.hasMoreElements();)
						{
							Category tmp_category = (Category)e.nextElement();
							tmp_category.doAddCategoryRichList();
						}
						
						_screen.setFocusToStartPos();
						
						// unread、updatedを更新。
						refreshUnreadCounts();
						
					} catch (Exception e) {
						// エラーをロギング
						updateStatus("enter() " + e.toString());
						
						// 再試行しても解決しない場合は、アプリ強制終了
						UiApplication.getUiApplication().invokeLater(new Runnable()
						{
							public void run()
							{
								//Dialog.alert("An unexpected error occurred.");
								//_feedlyclient.quitApp();
								int select = Dialog.ask("An unexpected error occurred.", new Object[]{"Try again", "Quit", "Show error log"}, 0);
								
								switch(select)
								{
									case Dialog.CANCEL:
										_feedlyclient.quitApp();
										break;
										
									case 0:
										_feedlyclient.forceLogoutFeedly();
										break;
										
									case 1:
										_feedlyclient.quitApp();
										break;
										
									case 2:
										showLog();
										break;
								}
							}
						});
						
					} finally {
						// アクティビティインジケーターを削除
						_screen.deleteActivityIndicator();
					}
				} //run()
			}.start(); //Thread()
			
		// 有効な通信経路がない場合
		} else {
			UiApplication.getUiApplication().invokeLater(new Runnable()
			{
				public void run()
				{
					int select = Dialog.ask("A communication error has occurred. Please make sure your device is connected to Internet.", new Object[]{"Try again","Quit"}, 0);
					
					switch(select)
					{
						case Dialog.CANCEL:
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
		} //if()
	} //enter()
	
	
	public void exit()
	{
		//updateStatus("exit()");
		
		if(_screen != null)
		{
			_feedlyclient.popScreen(_screen);
		}
	}
	
	
	public void pushHomeScreen()
	{
		// デバグ用
		_feedlyclient.pushScreen(_screen);
	}
	
	
	public void close()
	{
		if(categories != null)
		{
			categories.removeAllElements();
			categories = null;
		}
		
		_feedlyclient.quitApp();
	}
	
	
	public void showLog()
	{
		UiApplication.getUiApplication().invokeLater(new Runnable()
		{
			public void run()
			{
				_feedlyclient.popScreen(_screen);
				_feedlyclient.showLogField();
			}
		});
	}
	
	
	public Command CMD_changeStateToAll()
	{
		Command out = new Command(new CommandHandler() 
		{
			public void execute(ReadOnlyCommandMetadata metadata, Object context) 
			{
				_feedlyclient.changeState(new State_Stream(_feedlyclient, "user/" + _feedlyclient.getID() + "/category/global.all", "ALL", true));
			} //execute()
		});
		return out;
	} //CMD_changeStateToAll()
	
	
	public Command CMD_changeStateToSaved()
	{
		Command out = new Command(new CommandHandler() 
		{
			public void execute(ReadOnlyCommandMetadata metadata, Object context) 
			{
				_feedlyclient.changeState(new State_Stream(_feedlyclient, "user/" + _feedlyclient.getID() + "/tag/global.saved", "Saved", false));
			} //execute()
		});
		return out;
	} //CMD_changeStateToSaved()
	
	
	public Command CMD_logout()
	{
		Command out = new Command(new CommandHandler() 
		{
			public void execute(ReadOnlyCommandMetadata metadata, Object context) 
			{
				// 有効な通信経路がない場合はリターン
				if(!Network.isCoverageSufficient())
				{
					Dialog.alert("A communication error has occurred. Please make sure your device is connected to Internet.");
					return;
				}
				
				// ダイアログを出して実行してよいか確認する。
				int select = Dialog.ask(Dialog.D_OK_CANCEL, "Do you really want to log out?", Dialog.NO);
				if(select == Dialog.NO) { return; }
				
				// ログアウト
				new Thread()
				{
					public void run()
					{
						try {
							// アクティビティインジケーターを表示
							_screen.showActivityIndicator();
							
							// ログアウト
							_feedlyclient.logoutFeedly();
							
						} catch (final Exception e) {
							// エラーをロギング
							updateStatus("CMD_logout() " + e.toString());
							
							// 失敗したら強制的にログアウト
							_feedlyclient.forceLogoutFeedly();
							
							// 失敗しましたダイアログ表示
							/*UiApplication.getUiApplication().invokeLater(new Runnable()
							{
								public void run()
								{
									Dialog.alert("An unexpected error occurred while logging out.");
								}
							});*/
							
						} finally {
							// アクティビティインジケーターを削除
							_screen.deleteActivityIndicator();
						}
					} //run()
				}.start(); //Thread()
			} //execute()
		});
		return out;
	} //CMD_logout()
	
	
	public Command CMD_refresh()
	{
		Command out = new Command(new CommandHandler() 
		{
			public void execute(ReadOnlyCommandMetadata metadata, Object context)
			{
				// 有効な通信経路がない場合はリターン
				if(!Network.isCoverageSufficient())
				{
					Dialog.alert("A communication error has occurred. Please make sure your device is connected to Internet.");
					return;
				}
				
				new Thread()
				{
					public void run()
					{
						_screen.showActivityIndicator();
						refreshUnreadCounts();
						_screen.deleteActivityIndicator();
						
					} //run()
				}.start(); //Thread()
			} //execute()
		});
		return out;
	} //CMD_refresh()
	
	
	public Command CMD_reload()
	{
		Command out = new Command(new CommandHandler() 
		{
			public void execute(ReadOnlyCommandMetadata metadata, Object context)
			{
				// 有効な通信経路がない場合はリターン
				if(!Network.isCoverageSufficient())
				{
					Dialog.alert("A communication error has occurred. Please make sure your device is connected to Internet.");
					return;
				}
				
				categories.removeAllElements();
				categories = null;
				
				_screen.deleteAllCategories();
				
				_feedlyclient.changeStateToHomeState();
				
			} //execute()
		});
		return out;
	} //CMD_reload()
	
	
	public Command CMD_toggleShowAndHideFeeds()
	{
		Command out = new Command(new CommandHandler() 
		{
			public void execute(ReadOnlyCommandMetadata metadata, Object context)
			{
				new Thread()
				{
					public void run()
					{
						synchronized (Lock.lock)
						{
							// フォーカスしているカテゴリのインデックスを取得
							int index = _screen.getRowNumberWithFocus();
							
							// index == 0 はGlobalなので処理しない。
							if(index == 0) { return; }
							
							_screen.showActivityIndicator();
							
							// フォーカスしているカテゴリを取得
							Category _tmp = (Category)categories.elementAt(index);
							
							// リストの開閉を実行
							_tmp.toggleShowAndHideFeeds();
							
							// リストが開いている場合は、unread、updatedを更新
							if(!_tmp.isCollapsed())
							{
								refreshUnreadCounts();
							}
							
							_screen.deleteActivityIndicator();
						}
					} //run()
				}.start(); //Thread()
			} //execute()
		});
		return out;
	} //CMD_toggleShowAndHideFeeds()
	
	
	public void refreshUnreadCounts()
	{
		try {
			// アクティビティインジケーターを表示
			//_screen.showActivityIndicator();
			
			JSONArray unreadlist = _feedlyapi.getListOfUnreadCounts().getJSONArray("unreadcounts");
			
			for(int i=0; i<unreadlist.length(); i++)
			{
				JSONObject source = unreadlist.getJSONObject(i);
				String id = source.getString("id");
				int count = source.getInt("count");
				String updated =  _feedlyapi.getTime(source.getLong("updated"));
				
				// それぞれのカテゴリごとにリフレッシュメソッドを実行
				for(Enumeration e = categories.elements(); e.hasMoreElements();)
				{
					Category category = (Category)e.nextElement();
					category.refreshUnreadAndUpdated(id, count, updated);
				}
			}
		} catch (final Exception e) {
			// エラーをロギング
			updateStatus("refreshUnreadCounts() " + e.toString());
			
			// 失敗しましたダイアログ表示
			UiApplication.getUiApplication().invokeLater(new Runnable()
			{
				public void run()
				{
					Dialog.alert("An unexpected error occurred while refreshing status.");
				}
			});
		} finally {
			//_screen.deleteActivityIndicator();
		}
	} //getListOfUnreadCounts()
	
	
	public void updateStatus(final String message)
	{
		_feedlyclient.updateStatus("State_Home::" + message);
	} //updateStatus()
	
	
	private Vector doCategorize(JSONArray subscriptions) throws JSONException
	{
		Vector out = new Vector();
		
		// あらかじめGlobalカテゴリーを追加。
		out.addElement(new Global("Global"));
		
		// 取得した購読中フィード一覧に異常がある場合は、Globalのみ追加してリターン
		if(subscriptions == null) { return out; }
		
		// 購読中のフィードがない場合は、Globalのみ追加してリターン
		if(subscriptions.length() == 0) { return out; }
		
		// あらかじめUncategorizedカテゴリーを作成
		String streamid_uncategorized = "user/" + _feedlyclient.getID() + "/category/global.uncategorized";
		Category _uncategorized = new Category("Uncategorized", streamid_uncategorized, true);
		
		// 購読中のフィードごとにカテゴリを振り分け
		for(int i=0; i<subscriptions.length(); i++)
		{
			// フィードの情報を取得
			JSONObject feed_jsonObject = subscriptions.getJSONObject(i);
			Feed _feed = new Feed(feed_jsonObject);
			
			// フィードのカテゴリ情報を取得
			JSONArray categorys = feed_jsonObject.getJSONArray("categories");
			
			// カテゴリの指定がない場合は、Uncategorizedカテゴリーへ登録する。
			if(categorys.length() == 0)
			{
				_uncategorized.addFeed(_feed);
				continue;
			}
			
			// フィードのカテゴリ指定を解析
			label: for(int j=0; j<categorys.length(); j++)
			{
				// フィードのカテゴリ情報を取得
				JSONObject category = categorys.getJSONObject(j);
				String category_name = category.getString("label");
				String stream_id = category.getString("id");
				
				// 既存のカテゴリを調査
				for(Enumeration e = out.elements(); e.hasMoreElements();)
				{
					// 既存のカテゴリ情報を取得
					Category _ctgry = (Category)e.nextElement();
					
					// 合致するカテゴリがある場合は、それに追加。
					if(_ctgry.getCategoryName().equals(category_name))
					{
						_ctgry.addFeed(_feed);
						break label;
					}
				}
				
				// 合致するカテゴリがない場合は、カテゴリを新規作成する。
				Category _new_category = new Category(category_name, stream_id, true);
				_new_category.addFeed(_feed);
				out.addElement(_new_category);
			}
		} //for
		
		// 最後にUncategorizedカテゴリーを追加（1つ以上フィードが追加されている場合のみ）
		if(_uncategorized.getNumOfFeeds() != 0)
		{
			out.addElement(_uncategorized);
		}
		
		return out;
	} //doCategorize()
	
	
	private class Category
	{
		private Vector ids = null;
		private String category_name = "";
		private String streamID = "";
		private RichList _list = null;
		private boolean isCollapsed;
		
		public Category(String category_name, String streamID, boolean isCollapsed)
		{
			this.category_name = category_name;
			this.streamID = streamID;
			this.isCollapsed = isCollapsed;
			
			// フィード格納用のベクターを作成
			ids = new Vector();
		}
		
		
		public void addFeed(Feed feed)
		{
			ids.addElement(feed);
		}
		
		
		public void doDeleteAllFeedsFromRichList()
		{
			// リッチリストに追加されている要素数を取得
			int num_of_rows = _list.getModel().getNumberOfRows();
			
			// カテゴリ名の分を減らす
			num_of_rows--;
			
			// 削除
			synchronized (UiApplication.getEventLock()) 
			{
				for(int i=0; i<num_of_rows; i++)
				{
					// 1行目はカテゴリ名なので、毎回2行目を削除する。
					_list.remove(1);
				}
			}
		}
		
		
		public void doAddCategoryRichList()
		{
			synchronized (UiApplication.getEventLock()) 
			{
				_list = _screen.addCategory();
				_list.setCommand(showStreamScreenCMD());
				_screen.addCategoryHeader(_list, this.category_name);
			}
			
			if(!isCollapsed)
			{
				doAddCategoryRow();
			}
		}
		
		
		public void doAddCategoryRow()
		{
			for(Enumeration e = ids.elements(); e.hasMoreElements();)
			{
				Feed feed = (Feed) e.nextElement();
				
				// とりあえず未読にしておく。あとで更新する。
				int unread = 0;
				
				synchronized (UiApplication.getEventLock()) 
				{
					_screen.addCategoryRow(_list, feed.getTitle(), unread, feed.getUpdate());
				}
			}
		} //doAddCategoryRow()
		
		
		public String getCategoryName()
		{
			return category_name;
		}
		
		
		public int getNumOfFeeds()
		{
			return ids.size();
		}
		
		
		public boolean isCollapsed()
		{
			return isCollapsed;
		}
		
		
		public void refreshUnreadAndUpdated(String id, int num_unread, String updated)
		{
			// IDが合致する場合、カテゴリの情報を更新
			if(id.equals(streamID))
			{
				_screen.refreshCategoryHeaderUnread(_list, category_name, num_unread);
				return;
			}
			
			// リストが閉じている場合はフィードの情報は更新しない
			if(isCollapsed) { return; }
			
			// IDが合致する場合、Feedの情報を更新
			for(int i=0; i<ids.size(); i++)
			{
				Feed feed = (Feed)ids.elementAt(i);
				
				if(id.equals(feed.getId()))
				{
					// rowIndexはヘッダ補正のため+1する。
					int rowIndex = i+1;
					
					_screen.refreshUnreadAndUpdated(_list, rowIndex, num_unread, updated);
					
					return;
				}
			}
		} //refreshUnreadAndUpdated()
		
		
		public void toggleShowAndHideFeeds()
		{
			if(isCollapsed) {
				// フィードを追加する。
				doAddCategoryRow();
				isCollapsed = false;
			} else {
				// リストに追加されているフィードを削除する
				doDeleteAllFeedsFromRichList();
				isCollapsed = true;
			}
		} //toggleShowAndHideFeeds()
		
		
		private Command showStreamScreenCMD()
		{
			Command out = new Command(new CommandHandler() 
			{
				public void execute(ReadOnlyCommandMetadata metadata, Object context) 
				{
					// フォーカス位置を取得
					int focusrow = _list.getFocusRow();
					
					// フォーカス位置が0の場合は、ヘッダにフォーカスした場合はなので、カテゴリを表示させれる。
					if(focusrow == 0)
					{
						_feedlyclient.changeState(new State_Stream(_feedlyclient, streamID, category_name, false));
						return;
					}
					
					// フォーカスしたフィードを取得（indexはヘッダ補正のため-1する。）
					Feed _feed = (Feed)ids.elementAt(focusrow-1);
					
					// ステイトをチェンジ
					_feedlyclient.changeState(new State_Stream(_feedlyclient, _feed.getId(), _feed.getTitle(), false));
					
				} //execute()
			});
			return out;
		} //showStreamScreenCMD()
	}//Category
	
	
	class Feed
	{
		private String id = "";
		private String title = "";
		private String update = "";
		
		
		public Feed(JSONObject source)
		{
			try {
				this.id = source.getString("id");
			} catch (JSONException e) {
				// PASS
			}
			
			// 
			try {
				this.title = source.getString("title");
			} catch (JSONException e) {
				this.title = "Untitled";
			}
			
			// 
			try {
				this.update = _feedlyapi.getTime(source.getLong("updated"));
			} catch (JSONException e) {
				this.update = "Unknown";
			}
		}
		
		
		public String getId()
		{
			return id;
		}
		
		public String getTitle()
		{
			return title;
		}
		
		public String getUpdate()
		{
			return update;
		}
	}
	
	
	private class Global extends Category
	{
		public Global(String category_name)
		{
			super(category_name, "dummy", false);
		}
		
		
		public void doAddCategoryRichList()
		{
			synchronized (UiApplication.getEventLock()) 
			{
				super._list = _screen.addCategory();
				super._list.setCommand(showStreamScreenCMD());
				_screen.addCategoryHeader(super._list, super.category_name);
			}
			
			if(!super.isCollapsed)
			{
				doAddCategoryRow();
			}
		}
		
		
		public void doAddCategoryRow()
		{
			//
			// ALL
			//
			String title_all = "ALL";
			int unread_all = 0;
			String updated_all = "Unknown";
			synchronized (UiApplication.getEventLock()) 
			{
				_screen.addCategoryRow(super._list, title_all, unread_all, updated_all);
			}
			
			//
			// Saved
			//
			String title_saved = "Saved For Later";
			int unread_saved = 0;
			String updated_saved = "Unknown";
			synchronized (UiApplication.getEventLock()) 
			{
				_screen.addCategoryRow(super._list, title_saved, unread_saved, updated_saved);
			}
			
		} //doAddCategoryRow()
		
		
		public void refreshUnreadAndUpdated(String id, int num_unread, String updated)
		{
			String streamId_all = "user/" + _feedlyclient.getID() + "/category/global.all";
			
			if(id.equals(streamId_all))
			{
				_screen.refreshUnreadAndUpdated(super._list, 1, num_unread, updated);
			}
			
			//String streamId_saved = "user/" + _feedlyclient.getID() + "/tag/global.saved";
			//_screen.refreshUnreadAndUpdated(super._list, 2, count, updated_string);
			
		} //refreshUnreadAndUpdated()
		
		
		public void toggleShowAndHideFeeds()
		{
			// PASS
		} //toggleShowAndHideFeeds()
		
		
		private Command showStreamScreenCMD()
		{
			final RichList _list = super._list;
			
			Command out = new Command(new CommandHandler() 
			{
				public void execute(ReadOnlyCommandMetadata metadata, Object context) 
				{
					// フォーカス位置を取得
					int focusrow = _list.getFocusRow();
					
					// フォーカス位置が0の場合は、ALLストリームを表示する。
					if(focusrow == 1) {
						_feedlyclient.changeState(new State_Stream(_feedlyclient, "user/" + _feedlyclient.getID() + "/category/global.all", "ALL", true));
					} else if(focusrow == 2) {
						// フォーカス位置が1の場合は、Savedストリームを表示する。
						_feedlyclient.changeState(new State_Stream(_feedlyclient, "user/" + _feedlyclient.getID() + "/tag/global.saved", "Saved", false));
					}
				} //execute()
			});
			return out;
		}//showStreamScreenCMD()
	}//Global
	
	
	private static class Lock
	{
		static Object lock = new Object();
	}
	
}
