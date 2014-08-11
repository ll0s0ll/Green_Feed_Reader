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
import java.util.Hashtable;
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
	
	private JSONArray subscriptions = null;
	private Hashtable subscriptions_index = null;
	private Hashtable categories = null;

	private boolean showfeeds;
	
	public State_Home(FeedlyClient feedlyclient)
	{
		this._feedlyclient = feedlyclient;
		this._feedlyapi = feedlyclient.getFeedlyAPI();
		
		this.showfeeds = true;
		
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
		if(subscriptions != null) { return; }
		
		
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
						
						subscriptions = _feedlyapi.getUserSubscriptions();
						
						subscriptions_index = makeIndex();
						
						categories = doCategorize(subscriptions);
						
						//
						// エントリーをスクリーンに追加。
						//
						for(Enumeration e = categories.keys(); e.hasMoreElements();)
						{
							String category_name = (String) e.nextElement();
							
							// Uncategorizedカテゴリは飛ばして、後で追加する。
							if(category_name.equals("uncategorized")) { continue; }
							
							Category tmp_category = (Category)categories.get(category_name);
							tmp_category.makeIndex();
							tmp_category.doAddCategoryRichList();
							tmp_category.doAddCategoryRow();
						}
						
						// 購読中のストリームがない場合はリターン
						if(subscriptions.length() == 0) { return; };
							
						// Uncategorizedカテゴリのエントリーを追加する。
						Category tmp_uncategorized = (Category)categories.get("uncategorized");
						tmp_uncategorized.makeIndex();
						tmp_uncategorized.doAddCategoryRichList();
						tmp_uncategorized.doAddCategoryRow();
						
						//
						// unread、updatedを更新。
						//
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
	
	/*
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
	*/
	
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
				
				subscriptions = null;
				subscriptions_index = null;
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
						_screen.showActivityIndicator();
						
						if(showfeeds) {
							// 各カテゴリごとにフィードを削除する
							for(Enumeration e = categories.keys(); e.hasMoreElements();)
							{
								String category_name = (String) e.nextElement();
								
								// Globalカテゴリは飛ばす
								if(category_name.equals("Global")) { continue; }
								
								((Category)categories.get(category_name)).doDeleteAllFeedsFromRichList();
							}
							
							// フラグ更新
							showfeeds = false;
							
						} else {
							// 各カテゴリごとにフィードを追加する
							for(Enumeration e = categories.keys(); e.hasMoreElements();)
							{
								String category_name = (String) e.nextElement();
								
								// Globalカテゴリは飛ばす
								if(category_name.equals("Global")) { continue; }
								
								((Category)categories.get(category_name)).doAddCategoryRow();
							}
							
							// unread、updatedを更新
							refreshUnreadCounts();
							
							// フラグ更新
							showfeeds = true;
						}
						
						_screen.deleteActivityIndicator();
						
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
			//_feedlyclient.updateStatus(unreadlist.toString());
			
			// idをキーにして、countとupdatedを含んだハッシュテーブルを含む、ハッシュテーブルを作る。
			Hashtable unreadlist_hash = new Hashtable();
			for(int i=0; i<unreadlist.length(); i++)
			{
				Hashtable tmp = new Hashtable();
				tmp.put("count", new Integer(unreadlist.getJSONObject(i).getInt("count")));
				tmp.put("updated", new Long(unreadlist.getJSONObject(i).getLong("updated")));
				
				unreadlist_hash.put(unreadlist.getJSONObject(i).get("id"), tmp);
			}
			
			// それぞれのカテゴリごとにリフレッシュメソッドを実行
			for(Enumeration e = categories.keys(); e.hasMoreElements();)
			{
				String category_name = (String) e.nextElement();
				((Category)categories.get(category_name)).refreshUnreadAndUpdated(unreadlist_hash);
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
	
	
	private Hashtable doCategorize(JSONArray subscriptions) throws JSONException
	{
		if(subscriptions == null) { return new Hashtable(); }
		
		Hashtable out = new Hashtable();
		
		
		// あらかじめGlobalカテゴリーを追加。
		out.put("Global", new Global("Global"));
		
		
		// 購読中のストリームがない場合は、Globalのみ追加してリターン
		if(subscriptions.length() == 0) { return out; }
		
		
		// あらかじめUncategorizedカテゴリーを追加。
		Category _category = new Category("Uncategorized");
		_category.addStramID("user/" + _feedlyclient.getID() + "/category/global.uncategorized");
		out.put("uncategorized", _category);
		
		for(int i=0; i<subscriptions.length(); i++)
		{
			JSONObject feed = subscriptions.getJSONObject(i);
			JSONArray categorys = feed.getJSONArray("categories");
			String feedid = feed.getString("id");
			
			// カテゴリの指定がない場合は、Uncategorizedカテゴリーへ登録する。
			if(categorys.length() == 0)
			{
				((Category)out.get("uncategorized")).addID(feedid);
			}
			
			// カテゴリ指定を解析
			for(int j=0; j<categorys.length(); j++)
			{
				JSONObject category = categorys.getJSONObject(j);
				String category_name = category.getString("label");
				String streamid = category.getString("id");
				
				// 該当するカテゴリがない場合は新規作成する。
				if(out.containsKey(category_name)) {
					((Category)out.get(category_name)).addID(feedid);
					((Category)out.get(category_name)).addStramID(streamid);
				} else {
					out.put(category_name, new Category(category_name));
					((Category)out.get(category_name)).addID(feedid);
					((Category)out.get(category_name)).addStramID(streamid);
				}
			}
		}
		
		return out;
	} //doCategorize()
	
	
	private Hashtable makeIndex() throws JSONException
	{
		if(subscriptions == null){ return new Hashtable(); };
		
		// idをキーにしたインデックスを作成。
		Hashtable out = new Hashtable();
		
		for(int i=0; i<subscriptions.length(); i++)
		{
			JSONObject feed = subscriptions.getJSONObject(i);
			out.put(feed.getString("id"), new Integer(i));
		}
		
		return out;
	}
	
	
	private class Category
	{
		private Vector ids = null;
		private Hashtable ids_index = null;
		private String category_name = "";
		private String streamID = "";
		private RichList _list = null;
		
		public Category(String category_name)
		{
			this.category_name = category_name;
			
			ids = new Vector();
		}
		
		
		public void addID(String id)
		{
			ids.addElement(id);
		}
		
		
		public void addStramID(String streamid)
		{
			this.streamID = streamid;
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
		}
		
		
		public void doAddCategoryRow()
		{
			synchronized (UiApplication.getEventLock()) 
			{
				//_list = _screen.addCategory();
				//_list.setCommand(showStreamScreenCMD());
				//_screen.addCategoryHeader(_list, this.category_name);
			}
			
			
			for(Enumeration e = ids.elements(); e.hasMoreElements();)
			{
				String id = (String) e.nextElement();
				
				JSONObject feed = null;
				try {
					feed = subscriptions.getJSONObject(((Integer)subscriptions_index.get(id)).intValue());
				} catch (JSONException e1) {
					continue;
				}
				
				String title = "";
				try {
					title = feed.getString("title");
				} catch (JSONException e1) {
					title = "Untitled";
				}
				
				int unread = 0;
				
				String update = "";
				try {
					update = _feedlyapi.getTime(feed.getLong("updated"));
				} catch (JSONException e1) {
					update = "Unknown";
				}
					
				synchronized (UiApplication.getEventLock()) 
				{
					_screen.addCategoryRow(_list, title, unread, update);
				}
			}
		} //doAddCategoryRow()
		
		
		public void makeIndex()
		{
			// keyはfeedのidで、valueはfeedのidがthis.idsに格納された位置でハッシュテーブルを作る
			// feedのidをキーにして調べ物ができるようにするため。
			//
			// ex.
			// this.ids[1] = "feed/http://feeds.feedburner.com/design-milk";
			// out.put("feed/http://feeds.feedburner.com/design-milk", new Integer(1))
			// ex.
			// this.ids[3] = "feed/http://5secondrule.typepad.com/my_weblog/atom.xml";
			// out.put("feed/http://5secondrule.typepad.com/my_weblog/atom.xml", new Integer(3))
			
			ids_index = new Hashtable();
			
			for(int i=0; i<this.ids.size(); i++)
			{
				ids_index.put(ids.elementAt(i), new Integer(i));
			}
		} //makeIndex()
		
		
		public void refreshUnreadAndUpdated(Hashtable source)// throws Exception
		{
			for(Enumeration e = source.keys(); e.hasMoreElements();)
			{
				String id_souce = (String) e.nextElement();
				Hashtable feed = (Hashtable) source.get(id_souce);
				
				
				int count = ((Integer)feed.get("count")).intValue();
				long updated_long =  ((Long)feed.get("updated")).longValue();
				String updated_string = _feedlyapi.getTime(updated_long);
				
				// フィードの未読数を更新
				if(ids_index.containsKey(id_souce))
				{
					// rowIndexはヘッダ補正のため+1する。
					int rowIndex = ((Integer)ids_index.get(id_souce)).intValue() + 1;
					_screen.refreshUnreadAndUpdated(_list, rowIndex, count, updated_string);
					source.remove(id_souce);
				} else if(id_souce.equals(streamID)) {
					// カテゴリの未読件数を更新
					_screen.refreshCategoryHeaderUnread(_list, category_name, count);
					source.remove(id_souce);
				}
			}
		} //refreshUnreadAndUpdated()
		
		
		private Command showStreamScreenCMD()
		{
			Command out = new Command(new CommandHandler() 
			{
				public void execute(ReadOnlyCommandMetadata metadata, Object context) 
				{
					try {
						
						// フォーカス位置を取得
						int focusrow = _list.getFocusRow();
						
						// フォーカス位置が0の場合は、ヘッダにフォーカスした場合はなので、カテゴリを表示させれる。
						if(focusrow == 0)
						{
							_feedlyclient.changeState(new State_Stream(_feedlyclient, streamID, category_name, false));
							return;
						}
						
						// フォーカスしたフィードのストリームIDを取得（ヘッダ補正のため-1する。）
						String streamId = (String) ids.elementAt(focusrow-1);
						
						// フィードのタイトルを取得
						int indexATsubscriptions = ((Integer)subscriptions_index.get(streamId)).intValue();
						String title = subscriptions.getJSONObject(indexATsubscriptions).getString("title");
						
						// ステイトをチェンジ
						_feedlyclient.changeState(new State_Stream(_feedlyclient, streamId, title, false));
						
					} catch (JSONException e) {
						return;
					}
				} //execute()
			});
			return out;
		} //showStreamScreenCMD()
	}//Category
	
	
	private class Global extends Category
	{
		public Global(String category_name)
		{
			super(category_name);
		}
		
		
		public void doAddCategoryRichList()
		{
			synchronized (UiApplication.getEventLock()) 
			{
				super._list = _screen.addCategory();
				super._list.setCommand(showStreamScreenCMD());
				_screen.addCategoryHeader(super._list, super.category_name);
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
		
		
		public void refreshUnreadAndUpdated(Hashtable source)// throws Exception
		{
			for(Enumeration e = source.keys(); e.hasMoreElements();)
			{
				String id_souce = (String) e.nextElement();
				Hashtable feed = (Hashtable) source.get(id_souce);
				
				
				int count = ((Integer)feed.get("count")).intValue();
				long updated_long =  ((Long)feed.get("updated")).longValue();
				String updated_string = _feedlyapi.getTime(updated_long);
				
				
				if(id_souce.equals("user/" + _feedlyclient.getID() + "/category/global.all")) {
					_screen.refreshUnreadAndUpdated(super._list, 1, count, updated_string);
					source.remove(id_souce);
				}/* else if(id_souce.equals("user/" + _feedlyclient.getID() + "/tag/global.saved")) {
					_screen.refreshUnreadAndUpdated(super._list, 2, count, updated_string);
					source.remove(id_souce);
				}*/
			}
		} //refreshUnreadAndUpdated()
		
		
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
	
}