/*
	State_Stream.java
	
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

import java.util.Vector;

import net.rim.blackberry.api.sendmenu.SendCommand;
import net.rim.blackberry.api.sendmenu.SendCommandContextKeys;
import net.rim.blackberry.api.sendmenu.SendCommandMenu;
import net.rim.blackberry.api.sendmenu.SendCommandRepository;
import net.rim.device.api.command.Command;
import net.rim.device.api.command.CommandHandler;
import net.rim.device.api.command.ReadOnlyCommandMetadata;
import net.rim.device.api.io.transport.ConnectionFactory;
import net.rim.device.api.system.Display;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FocusChangeListener;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Ui;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.util.StringProvider;

import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;


public class State_Stream implements State_Base, FocusChangeListener
{
	private FeedlyClient _feedlyclient = null;
	private FeedlyAPI _feedlyapi = null;
	
	private Screen_Stream _screen = null;
	private Screen_Entry _entryScreen = null;
	
	private Vector entries = null;
	private String continuation = "";
	private String streamId = "";
	private String screen_title = "";
	private boolean unread_only;
	
	
	public State_Stream(FeedlyClient _feedlyclient, String streamId, String screen_title, boolean unread_only)
	{
		this._feedlyclient = _feedlyclient;
		this._feedlyapi = _feedlyclient.getFeedlyAPI();
		
		this.streamId = streamId;
		this.screen_title = screen_title;
		this.unread_only = unread_only;
	}
	
	
	//
	//-- Implement State_Base ----------------------------------------------------------//
	//
	public void enter()
	{
		//updateStatus("enter()");
		
		// スクリーンを作成
		_screen = new Screen_Stream(this);
		
		// スクリーンタイトルを設定
		if(unread_only) {
			_screen.setScreenTitle("(Unread) " + screen_title);
		} else {
			_screen.setScreenTitle(screen_title);
		}
		
		// スクリーンを表示
		_feedlyclient.pushScreen(_screen);
		
		
		if(Network.isCoverageSufficient())
		{
			//
			// エントリーを取得して、スクリーンに表示する。
			//
			new Thread()
			{
				public void run()
				{
					try {
						// アクティビティインジケーターを表示
						_screen.showActivityIndicator();
						
						// ストリームを取得。
						JSONObject stream_jsonO = getStream("");
						
						// ストリームのcontinuationを保存
						continuation = getContinuationId(stream_jsonO);
						
						// ストリームのアイテムを取得
						JSONArray items_jsonA = stream_jsonO.getJSONArray("items");
						
						// エントリーを取得。 すでにものが詰まっている場合は削除する。
						if(entries != null){ entries = null; }
						entries = new Vector();
						for(int i=0; i<items_jsonA.length(); i++)
						{
							entries.addElement(new Entry(items_jsonA.getJSONObject(i), _feedlyapi));
						}
						
						// 新しいエントリーをテーブルに追加する。
						addRowToRichList(entries, 0);
						
						// アクティビティインジケーターを削除
						_screen.deleteActivityIndicator();
						
					} catch (final Exception e) {
						// エラーをロギング
						updateStatus("enter() " + e.toString());
						
						// 再試行しても解決しない場合は、Homeステイトに戻る
						UiApplication.getUiApplication().invokeLater(new Runnable()
						{
							public void run()
							{
								
								Dialog.alert("An unexpected error occurred.");
								_feedlyclient.changeStateToHomeState();
							}
						});
						
					} finally {
						// アクティビティインジケーターを削除
						_screen.deleteActivityIndicator();
					}
				} //run()
			}.start(); //Thread()
			
		} else {
			UiApplication.getUiApplication().invokeLater(new Runnable()
			{
				public void run()
				{
					Dialog.alert("A communication error has occurred. Please make sure your device is connected to Internet.");
					_feedlyclient.changeStateToHomeState();
				}
			});
		} //if()
	} //enter()
	
	
	public void exit()
	{
		if(_screen != null)
		{
			_feedlyclient.popScreen(_screen);
			_screen.deleteAll();
			_screen = null;
		}
		
		if(entries != null)
		{
			entries.removeAllElements();
			entries = null;
		}
	}
	
	
	//
	//-- Implement FocusChangeListener ----------------------------------------------------------//
	//
	synchronized public void focusChanged(final Field field, int eventType)
	{
		new Thread()
		{
			public void run()
			{
				synchronized (Lock.lock)
				{
					int index = _screen.getRowNumberWithFocus();
					
					// 未読込のエントリーがある場合は読み込む。
					if(index == entries.size()-1 && isAvailableMoreEntries())
					{
						getMoreEntries();
					}
				} //synchronized
			} //run()
		}.start(); //Thread()
	} //focusChanged()
	
	
	public void cleanEntryScreen()
	{
		_entryScreen.deleteAll();
		_entryScreen = null;
	}
	
	
	public boolean isAvailableOneOrMoreEntries()
	{
		if(entries.size() == 0) {
			return false;
		} else {
			return true;
		}
	}
	
	
	public boolean isAvailableMoreEntries()
	{
		if(continuation.equals("")) {
			return false;
		} else {
			return true;
		}
	}
	
	
	public boolean isFirstEntry(int index)
	{
		if(index == 0) {
			return true;
		} else {
			return false;
		}
	}
	
	
	public boolean isLastEntry(int index)
	{
		if(index == entries.size()-1  && !isAvailableMoreEntries()) {
			return true;
		} else {
			return false;
		}
	}
	
	
	public boolean isOnlyOneEntry(int index)
	{
		if(index == 0 && entries.size() == 1) {
			return true;
		} else {
			return false;
		}
	}
	
	
	public boolean isUnreadEntry(int rowindex)
	{
		Entry _entry = (Entry)entries.elementAt(rowindex);
		return _entry.isUnread();
	}
	
	
	public boolean isSavedEntry(int rowindex)
	{
		Entry _entry = (Entry)entries.elementAt(rowindex);
		return _entry.isSaved();
	}
	
	
	public ConnectionFactory getConnectionFactory()
	{
		return _feedlyclient.getConnectionFactory();
	}
	
	
	public SendCommandMenu makeSendCommandMenu(String text) throws JSONException
	{
		JSONObject context = new JSONObject();
		context.put(SendCommandContextKeys.TEXT, text);
		//context.put(SendCommandContextKeys.SUBJECT, "Selected text");
		
		SendCommandRepository repository = SendCommandRepository.getInstance();
		SendCommand[] sendCommands = repository.get(SendCommand.TYPE_TEXT, context, true);
		
		return new SendCommandMenu(sendCommands, new StringProvider("Send"), 0x130010, 0);
	} //makeSendCommandMenu()
	
	
	public String makeTextForSendMenu(int num_of_row)
	{
		//int num_of_row = _screen.getRowNumberWithFocus();
		Entry _entry = (Entry)entries.elementAt(num_of_row);
		
		String out = "";
		// エントリーのタイトル
		out += _entry.getTitle();
		
		// フィードのタイトル
		out +=  " / " + _entry.getOriginTitle();
		
		// エントリーのURL
		if(!_entry.getAlternateHref().equals("")) {
			out += " " + _entry.getAlternateHref();
		} else if(!_entry. getOriginId().equals("")) {
			out += " " + _entry.getOriginId();
		}
		
		return out;
	}
	
	
	public void updateStatus(final String message)
	{
		_feedlyclient.updateStatus("State_Stream::" + message);
	} //updateStatus()
	
	
	private void addRowToRichList(Vector entries, int start_index)
	{
		// エントリー数が0の場合はダイアログを出してリターン
		if(entries.size() == 0)
		{
			UiApplication.getUiApplication().invokeLater(new Runnable()
			{
				public void run()
				{
					Dialog.alert("No entries found.");
				}
			});
			return;
		}
		
		for(int i=0; i<entries.size(); i++)
		{
			Entry _entry = (Entry)entries.elementAt(i);
			String title = _entry.getTitle();
			String origin_title = _entry.getOriginTitle();
			String published = _entry.getPublished();
			
			synchronized (UiApplication.getEventLock()) 
			{
				_screen.addRowToRichList(i+start_index, title, origin_title, published);
			}
		}
		
		// すべて既読ボタン追加
		if(!isAvailableMoreEntries())
		{
			synchronized (UiApplication.getEventLock()) 
			{
				_screen.addMarkAllAsReadButton();
			}
		}
		
	} //addRowToRichList()
	
	
	private void displayNextEntry()
	{
		new Thread()
		{
			public void run()
			{
				int index = _entryScreen.getIndexOfEntry();
				
				// インデックスを進める。
				if(index == entries.size()-1 && isAvailableMoreEntries()) {
					
					// 未読込のエントリーがある場合は読み込む。
					_entryScreen.showActivityIndicator();
					getMoreEntries();
					_entryScreen.hideActivityIndicator();
					index++;
					
				} else if(index == entries.size()-1) {
					
					// これ以上エントリーがない場合は、ダイアログを出して、リターン。
					UiApplication.getUiApplication().invokeLater(new Runnable()
					{
						public void run()
						{
							Dialog.alert("No more entries found.");
							
						}
					});
					return;
				} else {
					index++;
				}
				
				try {
					// エントリー内容を取得
					Entry _entry = (Entry)entries.elementAt(index);
					
					// 
					String title = _entry.getTitle();
					String origin_title = _entry.getOriginTitle();
					String published = _entry.getPublished();
					
					// 全文表示リンク
					String url = "";
					if(!_entry.getAlternateHref().equals("")) {
						url = _entry.getAlternateHref();
					} else if(!_entry. getOriginId().equals("")) {
						url = _entry.getOriginId();
					}
					
					// 表示させるHTMLを作成。
					String html = makeHTMLOfEntry(_entry);
					
					// 表示を更新
					_entryScreen.displayContent(index, title, origin_title, published, url, html);
					
					// 未読エントリーなら、Feedlyに既読コマンドを送信、対象エントリーを既読表示に変更
					if(_entry.isUnread())
					{
						makeEntryAsRead(index);
					}
					
				} catch (final Exception e) {
					// エラーをロギング
					updateStatus("displayNextEntry() " + e.toString());
					
					// 失敗しましたダイアログ表示
					UiApplication.getUiApplication().invokeLater(new Runnable()
					{
						public void run()
						{
							
							Dialog.alert("An unexpected error occurred while displaying entry.");
						}
					});
				}
			} //run()
		}.start(); //Thread()
	} //displayNextEntry()
	
	
	private void displayPrevEntry()
	{
		new Thread()
		{
			public void run()
			{
				int index = _entryScreen.getIndexOfEntry();
				
				// インデックスを進める。
				if(isFirstEntry(index)) {
					
					// これ以上エントリーがない場合はリターン。
					UiApplication.getUiApplication().invokeLater(new Runnable()
					{
						public void run()
						{
							Dialog.alert("No more entries found.");
							
						}
					});
					return;
					
				} else {
					index--;
				}
				
				try {
					// エントリー内容を取得
					Entry _entry = (Entry)entries.elementAt(index);
					
					// 
					String title = _entry.getTitle();
					String origin_title = _entry.getOriginTitle();
					String published = _entry.getPublished();
					
					// 全文表示リンク
					String url = "";
					if(!_entry.getAlternateHref().equals("")) {
						url = _entry.getAlternateHref();
					} else if(!_entry. getOriginId().equals("")) {
						url = _entry.getOriginId();
					}
					
					// 表示させるHTMLを作成。
					String html = makeHTMLOfEntry(_entry);
					
					// 表示を更新
					_entryScreen.displayContent(index, title, origin_title, published, url, html);
					
					// 未読エントリーなら、Feedlyに既読コマンドを送信、対象エントリーを既読表示に変更
					if(_entry.isUnread())
					{
						makeEntryAsRead(index);
					}
					
				} catch (final Exception e) {
					
					// エラーをロギング
					updateStatus("displayPrevEntry() " + e.toString());
					
					// 失敗しましたダイアログ表示
					UiApplication.getUiApplication().invokeLater(new Runnable()
					{
						public void run()
						{
							Dialog.alert("An unexpected error occurred while displaying entry.");
						}
					});
				}
			} //run()
		}.start(); //Thread()
	} //displayPrevEntry()
	
	
	private String getContinuationId(JSONObject _stream)
	{
		String out = "";
		try {
			out = _stream.getString("continuation");
		} catch (JSONException e) {
			out = "";
		}
		return out;
	}
	
	
	private void getMoreEntries()
	{
		// アクティビティインジケーターを表示
		_screen.showActivityIndicator();
		
		// エントリーがない場合はリターン
		if(!isAvailableMoreEntries())
		{
			// アクティビティインジケーターを削除
			_screen.deleteActivityIndicator();
			
			// 失敗しましたダイアログ表示
			UiApplication.getUiApplication().invokeLater(new Runnable()
			{
				public void run()
				{
					Dialog.alert("No more entries found.");
				}
			});
			return;
		}
		
		// 有効な通信経路がない場合はリターン
		if(!Network.isCoverageSufficient())
		{
			// アクティビティインジケーターを削除
			_screen.deleteActivityIndicator();
			
			UiApplication.getUiApplication().invokeLater(new Runnable()
			{
				public void run()
				{
					Dialog.alert("A communication error has occurred. Please make sure your device is connected to Internet.");
				}
			});
			return;
		} //if()
		
		// 追加のエントリーを取得
		try {
			// 既存エントリー数を取得
			int start_index = entries.size();
			
			// 追加のエントリーを取得
			JSONObject stream_jsonO = getStream(continuation);
			
			// ストリームのcontinuationを保存
			continuation = getContinuationId(stream_jsonO);
			
			// ストリームのアイテムを取得
			JSONArray extent_stream = stream_jsonO.getJSONArray("items");
			
			// 追加エントリーを追加。
			Vector extent_entries = new Vector();
			for(int i=0; i<extent_stream.length(); i++)
			{
				Entry _entry = new Entry(extent_stream.getJSONObject(i), _feedlyapi);
				extent_entries.addElement(_entry);
				entries.addElement(_entry);
			}
			
			// 新しいエントリーをテーブルに追加する。
			addRowToRichList(extent_entries, start_index);
			
		} catch (final Exception e) {
			
			// エラーをロギング
			updateStatus("getMoreEntries() " + e.toString());
			
			// 失敗しましたダイアログ表示
			UiApplication.getUiApplication().invokeLater(new Runnable()
			{
				public void run()
				{
					
					Dialog.alert("An unexpected error occurred while getting more entries.");
				}
			});
		} finally {
			// アクティビティインジケーターを削除
			_screen.deleteActivityIndicator();
		}
	} //getMoreEntries()
	
	
	private JSONObject getStream(String continuation) throws Exception
	{
		//
		// オプションを設定
		//
		StringBuffer option = new StringBuffer("&");
		
		// 未読のみ
		if(unread_only) {
			option.append("unreadOnly=true");
		} else {
			option.append("unreadOnly=false");
		}
		
		// 取得エントリー数
		//option.append("&count=25");
		
		// コンティニューション
		if(!continuation.equals("")) {
			option.append("&continuation=" + continuation);
		}
		
		//
		// ストリームを取得してリターン
		//
		return _feedlyapi.getTheContentOfaStream(streamId + option.toString());
	} //getStream()
	
	
	private void makeAllEntriesAsRead()
	{
		// 有効な通信経路がない場合はリターン
		if(!Network.isCoverageSufficient())
		{
			// アクティビティインジケーターを削除
			_screen.deleteActivityIndicator();
			
			UiApplication.getUiApplication().invokeLater(new Runnable()
			{
				public void run()
				{
					Dialog.alert("A communication error has occurred. Please make sure your device is connected to Internet.");
				}
			});
			return;
		} //if()
				
		// ダイアログを出して実行してよいか確認する。
		int select = Dialog.ask(Dialog.D_OK_CANCEL, "Do you really want to make all entries as read?", Dialog.NO);
		if(select == Dialog.NO) { return; }
		
		new Thread()
		{
			public void run()
			{
				Vector ids = new Vector();
				
				// 対象のエントリー数を取得
				int num_items = entries.size();
				
				try {
					// アクティビティインジケーターを表示
					_screen.showActivityIndicator();
					
					// エントリーごとに処理する
					for(int i=0; i<num_items; i++)
					{
						Entry _entry = (Entry)entries.elementAt(i);
						
						// すでに既読の場合はスキップ
						if(!_entry.isUnread()) { continue; }
						
						// エントリーを既読にする
						_entry.makeAsRead();
							
						// APIに投げるエントリーidをまとめる。
						ids.addElement(_entry.getId());
					}
					
					// 未読にするエントリーがない場合はリターン
					if(ids.size() == 0) { return; }
					
					// 表示を更新する。
					_screen.refreshRichList();
					
					// JSONデータをPOST
					_feedlyapi.markOneOrMultipleArticlesAsRead(ids);

				} catch (final Exception e) {
					
					// 失敗したらエントリーを未読に戻す。
					for(int i=0; i<num_items; i++)
					{
						Entry _entry = (Entry)entries.elementAt(i);
						_entry.makeAsUnread();
					}
					
					// 表示も戻す。
					_screen.refreshRichList();
					
					// エラーをロギング
					updateStatus("makeAllEntriesAsRead() " + e.toString());
					
					// 失敗しましたダイアログ表示
					UiApplication.getUiApplication().invokeLater(new Runnable()
					{
						public void run()
						{
							Dialog.alert("An unexpected error occurred while making entries as Read");
						}
					});
					
				} finally {
					// アクティビティインジケーターを削除
					_screen.deleteActivityIndicator();
				}
			} //run()
		}.start(); //Thread()
	} // makeAllEntriesAsRead()
	
	
	private void makeEntryAsRead(final int row_number)
	{
		// 有効な通信経路がない場合はリターン
		if(!Network.isCoverageSufficient())
		{
			// アクティビティインジケーターを削除
			_screen.deleteActivityIndicator();
			
			UiApplication.getUiApplication().invokeLater(new Runnable()
			{
				public void run()
				{
					Dialog.alert("A communication error has occurred. Please make sure your device is connected to Internet.");
				}
			});
			return;
		} //if()
				
		
		new Thread()
		{
			public void run()
			{
				try {
					// アクティビティインジケーターを表示
					//_screen.showActivityIndicator();
					
					// unread項目をアップデート
					Entry _entry = (Entry)entries.elementAt(row_number);
					_entry.makeAsRead();
					
					
					// 表示を更新
					_screen.refreshRichList();
					
					// savedにするエントリーのIDを取得
					String entryId = _entry.getId();
					
					// APIに渡すエントリーIDを準備
					Vector entryIds = new Vector();
					entryIds.addElement(entryId);
					
					// Feedly APIを叩く。
					_feedlyapi.markOneOrMultipleArticlesAsRead(entryIds);
					
				} catch (final Exception e) {
					
					// 失敗したらunread項目を戻す。
					Entry _entry = (Entry)entries.elementAt(row_number);
					_entry.makeAsUnread();
					
					// 表示も戻す。
					_screen.refreshRichList();
					
					// エラーをロギング
					updateStatus("makeEntryAsRead() " + e.toString());
					
					// 失敗しましたダイアログ表示
					UiApplication.getUiApplication().invokeLater(new Runnable()
					{
						public void run()
						{
							Dialog.alert("An unexpected error occurred while making entry as Read");
						}
					});
					
				} finally {
					// アクティビティインジケーターを削除
					//_screen.deleteActivityIndicator();
				}
			} //run()
		}.start(); //Thread()
	} //makeEntryAsRead()
	
	
	private String makeHTMLOfEntry(Entry _entry)
	{
		//
		// 表示するHTMLを作成
		//
		// STYLE
		String style = "";
		style += "body{font-size: " + (Font.getDefault().getHeight(Ui.UNITS_px)-2) + "px;}";
		style += "dt{margin-left: 0; font-size: " + (Font.getDefault().getHeight(Ui.UNITS_px)-1 ) + "px; font-weight: bold;}";
		style += "dd{margin-left: 0; font-size: " + (Font.getDefault().getHeight(Ui.UNITS_px)-5) + "px; color: #808080;}";
		//style += ".visual{margin-left:auto; margin-right:auto;}";
		style += ".visual{text-align:center;}";
		style += ".button{text-align:center; margin-bottom:20;}";
		
		
		// BODY
		String body = "";
		/*
		// Title
		body += "<dl><dt>" + source.get("title") + "</dt>";
		
		// Feed title
		body += "<dd>" + source.get("origin_title") + "</dd>";
		
		// Published
		if(((Boolean)source.get("saved")).booleanValue()) {
			body += "<dd style=\"color:#008000;\">Saved / " + source.get("published") + "</dd>";
		} else {
			body += "<dd>" + source.get("published") + "</dd>";
		}
		
		body += "</dl>";
		
		// 区切り
		body += "<hr />";
		*/

		// Visual
		String visual_url = _entry.getVisualUrl();
		int visual_width = _entry.getVisualWidth();
		
		if(!visual_url.equals(""))
		{
			// 画像の横幅が画面幅を超えている場合は、画像幅を調整する
			if(visual_width > (Display.getWidth()-20)) {
				int width = Display.getWidth() - 20;
				body += "<p class=\"visual\"><img src=\"" + visual_url + "\" width=\"" + width + "\"></p>";
			} else {
				body += "<p class=\"visual\"><img src=\"" + visual_url + "\"></p>";
			}
		}
		
		// Content
		body += "<p>" + _entry.getContent() + "</p>";
		
		// 全文表示リンク
		String alternate_href = _entry.getAlternateHref();
		String originId = _entry.getOriginId();
		if(!alternate_href.equals("")) {
			body += "<hr />";
			body += "<p class=\"button\"><input type=\"button\" value=\"Visit Website\" onClick=\"location.href=\'" + alternate_href + "'\"></p>";
		} else if(alternate_href.equals("") && !originId.equals("")) {
			body += "<hr />";
			body += "<p class=\"button\"><input type=\"button\" value=\"Visit Website\" onClick=\"location.href=\'" + originId + "'\"></p>";
		}
		
		// 合体
		String out = "<html><head><style type=\"text/css\">" + style + "</style></head><body>" + body + "</body>" + "</html>";
		
		return out;
	} //makeHTMLFromHashtable()
	
	
	private void refresh()
	{
		// 有効な通信経路がない場合はリターン
		if(!Network.isCoverageSufficient())
		{
			// アクティビティインジケーターを削除
			_screen.deleteActivityIndicator();
			
			UiApplication.getUiApplication().invokeLater(new Runnable()
			{
				public void run()
				{
					Dialog.alert("A communication error has occurred. Please make sure your device is connected to Internet.");
				}
			});
			return;
		} //if()
				
		try {
			// アクティビティインジケーターを表示
			_screen.showActivityIndicator();
			
			// スクリーンタイトルを更新
			if(unread_only) {
				_screen.setScreenTitle("(Unread) " + screen_title);
			} else {
				_screen.setScreenTitle(screen_title);
			}
			
			// ストリームを取得。
			JSONObject stream_jsonO = getStream("");
			
			// ストリームのcontinuationを保存
			continuation = getContinuationId(stream_jsonO);
			
			// ストリームのアイテムを取得
			JSONArray items_jsonA = stream_jsonO.getJSONArray("items");
			
			// エントリーを取得。 すでにものが詰まっている場合は削除する。
			if(entries != null){ entries = null; }
			entries = new Vector();
			for(int i=0; i<items_jsonA.length(); i++)
			{
				entries.addElement(new Entry(items_jsonA.getJSONObject(i), _feedlyapi));
			}
			
			// すべてのエントリーを削除
			_screen.removeAllEntriesFromRichList();
			
			// 新しいエントリーをテーブルに追加する。
			addRowToRichList(entries, 0);
			
		} catch (final Exception e) {
			
			// エラーをロギング
			updateStatus("refresh() " + e.toString());
			
			// 失敗しましたダイアログ表示
			UiApplication.getUiApplication().invokeLater(new Runnable()
			{
				public void run()
				{
					Dialog.alert("An unexpected error occurred while getting entries");
				}
			});
		} finally {
			// アクティビティインジケーターを削除
			_screen.deleteActivityIndicator();
		}
	} //refresh()
	
	
	private void showEntryScreen(int row_number)
	{
		try {
			// スクリーンを表示
			if(_entryScreen != null) { _entryScreen = null; }
			
			// BrowserFieldがバグるので、毎回新規作成。
			_entryScreen = new Screen_Entry(this);
			
			_feedlyclient.pushScreen(_entryScreen);
			
			// クリックされたエントリーを取得
			Entry _entry = (Entry)entries.elementAt(row_number);
			
			// 
			String title = _entry.getTitle();
			String origin_title = _entry.getOriginTitle();
			String published = _entry.getPublished();
			
			// 全文表示リンク
			String url = "";
			if(!_entry.getAlternateHref().equals("")) {
				url = _entry.getAlternateHref();
			} else if(!_entry. getOriginId().equals("")) {
				url = _entry.getOriginId();
			}
			
			// 表示させるHTMLを作成。
			String html = makeHTMLOfEntry(_entry);
			
			_entryScreen.displayContent(row_number, title, origin_title, published, url, html);
			
		} catch (final Exception e) {
			
			// エラーをロギング
			updateStatus("showEntryScreen() " + e.toString());
			
			// 失敗しましたダイアログ表示
			UiApplication.getUiApplication().invokeLater(new Runnable()
			{
				public void run()
				{
					Dialog.alert("An unexpected error occurred while displaying entry.");
				}
			});
		}
	} //showEntryScreen()
	
	
	private void toggleShowAndHideRead()
	{
		// 現在のunread_only値からフラグを更新
		if(unread_only) {
			unread_only = false;
		} else {
			unread_only = true;
		}
		
		// 表示内容を更新する。
		new Thread()
		{
			public void run()
			{
				refresh();
			} //run()
		}.start(); //Thread()
	} //toggleShowAndHideRead()
	
	
	private void toggleUnreadAndRead(final int index)
	{
		// 有効な通信経路がない場合はリターン
		if(!Network.isCoverageSufficient())
		{
			// アクティビティインジケーターを削除
			_screen.deleteActivityIndicator();
			
			UiApplication.getUiApplication().invokeLater(new Runnable()
			{
				public void run()
				{
					Dialog.alert("A communication error has occurred. Please make sure your device is connected to Internet.");
				}
			});
			return;
		} //if()
				
		new Thread()
		{
			public void run()
			{
				boolean desired_unread_status = false;
				boolean current_unread_status = false;
				
				try {
					// アクティビティインジケーターを表示
					_screen.showActivityIndicator();
					
					// エントリーのunreadの値から目的を設定
					if(isUnreadEntry(index)) {
						desired_unread_status = false;
						current_unread_status = true;
					} else {
						desired_unread_status = true;
						current_unread_status = false;
					}
					
					// unread項目をアップデート
					Entry _entry = (Entry)entries.elementAt(index);
					if(desired_unread_status) {
						_entry.makeAsUnread();
					} else {
						_entry.makeAsRead();
					}
					
					// 表示を更新
					_screen.refreshRichList();
					
					// savedにするエントリーのIDを取得
					String entryId = _entry.getId();
					
					// APIに渡すエントリーIDを準備
					Vector entryIds = new Vector();
					entryIds.addElement(entryId);
					
					// Feedly APIを叩く。
					if(current_unread_status) {
						_feedlyapi.markOneOrMultipleArticlesAsRead(entryIds);
					} else {
						_feedlyapi.keepOneOrMultipleArticlesAsUnread(entryIds);
					}
					
				} catch (final Exception e) {
					
					// 失敗したらunread項目を戻す。
					Entry _entry = (Entry)entries.elementAt(index);
					if(desired_unread_status) {
						_entry.makeAsUnread();
					} else {
						_entry.makeAsRead();
					}
					
					// 表示も戻す。
					_screen.refreshRichList();
					
					// エラーをロギング
					updateStatus("toggleUnreadAndRead()" + e.toString());
					
					// 失敗しましたダイアログ表示
					UiApplication.getUiApplication().invokeLater(new Runnable()
					{
						public void run()
						{
							Dialog.alert("An unexpected error occurred while making entry as Unread/Read");
						}
					});
					
				} finally {
					// アクティビティインジケーターを削除
					_screen.deleteActivityIndicator();
				}
			} //run()
		}.start(); //Thread()
	} //tagEntries()
	
	
	private void toggleUnsavedAndSaved(final int index)
	{
		// 有効な通信経路がない場合はリターン
		if(!Network.isCoverageSufficient())
		{
			// アクティビティインジケーターを削除
			_screen.deleteActivityIndicator();
			
			UiApplication.getUiApplication().invokeLater(new Runnable()
			{
				public void run()
				{
					Dialog.alert("A communication error has occurred. Please make sure your device is connected to Internet.");
				}
			});
			return;
		} //if()
				
		new Thread()
		{
			public void run()
			{
				boolean desired_savedtag_status = false;
				boolean current_savedtag_status = false;
				
				try {
					// アクティビティインジケーターを表示
					_screen.showActivityIndicator();
					
					// エントリーのSave状況から目的を設定
					if(isSavedEntry(index)) {
						desired_savedtag_status = false;
						current_savedtag_status = true;
					} else {
						desired_savedtag_status = true;
						current_savedtag_status = false;
					}
					
					// saved項目をアップデート
					Entry _entry = (Entry)entries.elementAt(index);
					if(desired_savedtag_status) {
						_entry.makeAsSaved();
					} else {
						_entry.makeAsUnsaved();
					}
					
					// 表示を更新
					_screen.updateSavedAndUpdatedField(index);
					
					// エントリースクリーンが表示されている場合は、そちらも更新。
					if(_entryScreen != null && _entryScreen.isVisible())
					{
						_entryScreen.updateSavedAndUpdatedField(index);
					}
					
					//
					String tagId = "user/" + _feedlyclient.getID() + "/tag/global.saved";
					
					// savedにするエントリーのIDを取得
					String entryId = _entry.getId();
					
					// Feedly APIを叩く。
					if(current_savedtag_status) {
						Vector entryIds = new Vector();
						entryIds.addElement(entryId);
						_feedlyapi.untagMultipleEntries(tagId, entryIds);
					} else {
						_feedlyapi.tagEntry(tagId, entryId);
					}
					
				} catch (final Exception e) {
					
					// 失敗したらsaved項目を戻す。
					Entry _entry = (Entry)entries.elementAt(index);
					if(desired_savedtag_status) {
						_entry.makeAsSaved();
					} else {
						_entry.makeAsUnsaved();
					}
					
					// 表示も戻す。
					_screen.updateSavedAndUpdatedField(index);
					
					// エントリースクリーンが表示されている場合は、そちらも戻す。
					if(_entryScreen != null && _entryScreen.isVisible())
					{
						_entryScreen.updateSavedAndUpdatedField(index);
					}
					
					// エラーをロギング
					updateStatus("toggleUnsavedAndSaved() " + e.toString());
					
					// 失敗しましたダイアログ表示
					UiApplication.getUiApplication().invokeLater(new Runnable()
					{
						public void run()
						{
							Dialog.alert("An unexpected error occurred while tagging entry.");
						}
					});
					
				} finally {
					// アクティビティインジケーターを削除
					_screen.deleteActivityIndicator();
				}
			} //run()
		}.start(); //Thread()
	} //tagEntries()
	
	
	public Command CMD_changeStateToHome()
	{
		Command out = new Command(new CommandHandler() 
		{
			public void execute(ReadOnlyCommandMetadata metadata, Object context) 
			{
				_feedlyclient.changeStateToHomeState();
			}
		});
		return out;
	} //changeStateToHomeCMD()
	
	
	public Command CMD_clickedTableRow()
	{
		Command out = new Command(new CommandHandler()
		{
			public void execute(ReadOnlyCommandMetadata metadata, Object context)
			{
				new Thread()
				{
					public void run()
					{
						// フォーカスされたRowのインデックスを取得
						final int row_number = _screen.getRowNumberWithFocus();
						
						// EntryScreenを表示
						showEntryScreen(row_number);
						
						// 未読エントリーなら、Feedlyに既読コマンドを送信、対象エントリーを既読表示に変更
						if(isUnreadEntry(row_number))
						{
							makeEntryAsRead(row_number);
						}
						
					} //run()
				}.start(); //Thread()
			}
		});
		return out;
	} //clickedTableRow()
	
	
	public Command CMD_displayNextEntry()
	{
		Command out = new Command(new CommandHandler()
		{
			public void execute(ReadOnlyCommandMetadata metadata, Object context)
			{
				displayNextEntry();
			}
		});
		return out;
	} //CMD_displayNextEntry()
	
	
	public Command CMD_displayPrevEntry()
	{
		Command out = new Command(new CommandHandler()
		{
			public void execute(ReadOnlyCommandMetadata metadata, Object context)
			{
				displayPrevEntry();
			}
		});
		return out;
	} //CMD_displayPrevEntry()
	
	
	public Command CMD_getMoreEntries()
	{
		Command out = new Command(new CommandHandler()
		{
			public void execute(ReadOnlyCommandMetadata metadata, Object context)
			{
				new Thread()
				{
					public void run()
					{
						getMoreEntries();
					} //run()
				}.start(); //Thread()
			}
		});
		return out;
	} //getMoreEntriesCMD()
	
	
	public Command CMD_makeAllEntriesAsRead()
	{
		Command out = new Command(new CommandHandler()
		{
			public void execute(ReadOnlyCommandMetadata metadata, Object context)
			{
				if(!isAvailableOneOrMoreEntries())
				{
					UiApplication.getUiApplication().invokeLater(new Runnable()
					{
						public void run()
						{
							Dialog.alert("No entries found.");
						}
					});
					return;
				}
				
				makeAllEntriesAsRead();
			}
		});
		return out;
	} //makeEntriesAsReadCMD()
	
	
	public Command CMD_refresh()
	{
		Command out = new Command(new CommandHandler() 
		{
			public void execute(ReadOnlyCommandMetadata metadata, Object context) 
			{
				new Thread()
				{
					public void run()
					{
						refresh();
					} //run()
				}.start(); //Thread()
			} //execute()
		});
		return out;
	} //refreshCMD()
	
	
	public Command CMD_toggleUnreadAndRead()
	{
		Command out = new Command(new CommandHandler() 
		{
			public void execute(ReadOnlyCommandMetadata metadata, Object context) 
			{
				if(!isAvailableOneOrMoreEntries())
				{
					UiApplication.getUiApplication().invokeLater(new Runnable()
					{
						public void run()
						{
							Dialog.alert("No entries found.");
						}
					});
					return;
				}
				
				toggleUnreadAndRead( _screen.getRowNumberWithFocus() );
			}
		});
		return out;
	}
	
	
	public Command CMD_toggleUnsavedAndSaved()
	{
		Command out = new Command(new CommandHandler()
		{
			public void execute(ReadOnlyCommandMetadata metadata, Object context)
			{
				if(!isAvailableOneOrMoreEntries())
				{
					UiApplication.getUiApplication().invokeLater(new Runnable()
					{
						public void run()
						{
							Dialog.alert("No entries found.");
						}
					});
					return;
				}
				
				toggleUnsavedAndSaved( _screen.getRowNumberWithFocus() );
			}
		});
		return out;
	} //CMD_toggleSavedTag()
	
	
	public Command CMD_toggleUnsavedAndSaved(final int row_number)
	{
		Command out = new Command(new CommandHandler()
		{
			public void execute(ReadOnlyCommandMetadata metadata, Object context)
			{
				toggleUnsavedAndSaved( row_number );
			}
		});
		return out;
	} //CMD_toggleSavedTag()
	
	
	public Command CMD_toggleShowAndHideRead()
	{
		Command out = new Command(new CommandHandler() 
		{
			public void execute(ReadOnlyCommandMetadata metadata, Object context)
			{
				toggleShowAndHideRead();
			}
		});
		return out;
	} //toggleUnreadOnlyCMD()
	
	
	
	private static class Lock
	{
		static Object lock = new Object();
	}
	
	
	private class Entry
	{
		private String id = "";
		private String title = "";
		private String published = "";
		private boolean unread = false;
		private String origin_title = "";
		private boolean saved = false;
		
		private String originId = "";
		private String alternate_href = "";
		private String visual_url = "";
		private int visual_width = 0;
		//private int visual_height = 0;
		private String content = "";
		
		public Entry(JSONObject source, FeedlyAPI _feedlyapi)
		{
			// id [string]
			// the unique, immutable ID for this particular article.
			try {
				this.id = source.getString("id");
			} catch (JSONException e) {
				this.id = "";
			}
			
			// title [Optional][string]
			// the article’s title. This string does not contain any HTML markup.
			try {
				this.title = source.getString("title");
			} catch (JSONException e) {
				this.title = "";
			}
			
			// published [timestamp]
			// the timestamp, in ms, 
			// when this article was published, as reported by the RSS feed (often inaccurate).
			try {
				this.published = _feedlyapi.getTime(source.getLong("published"));
			} catch (JSONException e) {
				this.published = "";
			}
			
			// unread [boolean]
			// was this entry read by the user? 
			// If an Authorization header is not provided, this will always return false.
			// If an Authorization header is provided,
			// it will reflect if the user has read this entry or not.
			try {
				if(source.getBoolean("unread")) {
					this.unread = true;
				} else {
					this.unread = false;
				}
			} catch (JSONException e) {
				this.unread = false;
			}
			
			// origin [Optional][origin object]
			// the feed from which this article was crawled.
			// If present, “streamId” will contain the feed id,
			// “title” will contain the feed title, and “htmlUrl” will contain the feed’s website.
			try {
				JSONObject origin = source.getJSONObject("origin");
				try {
					this.origin_title = origin.getString("title");
				} catch (JSONException e) {
					this.origin_title = "";
				}
			} catch (JSONException e) {
				this.origin_title = "";
			}
			
			// tags [Optional] [tag object array]
			// A list of tag objects (“id” and “label”) that the user added to this entry.
			try {
				JSONArray tags = source.getJSONArray("tags");
				
				// saved項目は、デフォルトでfalseを設定。
				this.saved = false;
				
				for(int j=0; j<tags.length(); j++)
				{
					JSONObject tag = tags.getJSONObject(j);
					
					// Savedタグをチェック
					if(tag.getString("id").endsWith("global.saved")) {
						this.saved = true;
					}
				}
			} catch (JSONException e) {
				this.saved = false;
			}
			
			
			
			//-- エントリーの詳細画面用要素 --------------------------------------------//
			
			// OriginId
			try {
				this.originId = source.getString("originId");
			} catch (JSONException e) {
				this.originId = "";
			}
			
			// Alternate(はじめの1つのみ抽出）
			try {
				JSONArray alternate = source.getJSONArray("alternate");
				JSONObject tmp = alternate.getJSONObject(0);
				this.alternate_href = tmp.getString("href");
				
				/*
				for(int i=0; i<alternate.length(); i++)
				{
					JSONObject tmp = alternate.getJSONObject(i);
				}
				*/
				
			} catch (JSONException e1) {
				this.alternate_href = "";
			}
			
			// visual
			try {
				JSONObject visual = source.getJSONObject("visual");
				
				if(visual.getString("contentType").startsWith("image/")) {
					this.visual_url = visual.getString("url");
					try {
						this.visual_width = visual.getInt("width");
					} catch (JSONException e1) {
						this.visual_width = 0;
					}
					/*try {
						this.visual_height = visual.getInt("height");
					} catch (JSONException e1) {
						this.visual_height = 0;
					}*/
				} else {
					this.visual_url = "";
				}
			} catch (JSONException e1) {
				// ない場合はスルー
				this.visual_url =  "";
			}
			
			
			JSONObject content = null;
			try {
				content = source.getJSONObject("content");
				this.content = content.getString("content");
			} catch (JSONException e2) {
				try {
					content = source.getJSONObject("summary");
					this.content = content.getString("content");
				} catch (JSONException e) {
					this.content = "No content";
				}
			}
			
		} //Entry()
		
		
		public String getAlternateHref() { return alternate_href; }
		
		public String getContent() { return content; }
		
		public String getId() { return id; }
		
		public String getOriginId() { return originId; }
		
		public String getOriginTitle() { return origin_title; }
		
		public String getPublished() { return published; }
		
		public String getTitle() { return title; }
		
		public String getVisualUrl() { return visual_url; }
		
		public int getVisualWidth() { return visual_width; }
		
		//public int getVisualHeight() { return visual_height; }
		
		public boolean isSaved() { return saved; }
		
		public boolean isUnread() { return unread; }
		
		public void makeAsRead()
		{
			unread = false;
		}
		
		public void makeAsUnread()
		{
			unread = true;
		}
		
		public void makeAsSaved()
		{
			saved = true;
		}
		
		public void makeAsUnsaved()
		{
			saved = false;
		}
	}
}
