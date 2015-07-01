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
		
		// �X�N���[�����쐬
		_screen = new Screen_Stream(this);
		
		// �X�N���[���^�C�g����ݒ�
		if(unread_only) {
			_screen.setScreenTitle("(Unread) " + screen_title);
		} else {
			_screen.setScreenTitle(screen_title);
		}
		
		// �X�N���[����\��
		_feedlyclient.pushScreen(_screen);
		
		
		if(Network.isCoverageSufficient())
		{
			//
			// �G���g���[���擾���āA�X�N���[���ɕ\������B
			//
			new Thread()
			{
				public void run()
				{
					try {
						// �A�N�e�B�r�e�B�C���W�P�[�^�[��\��
						_screen.showActivityIndicator();
						
						// �X�g���[�����擾�B
						JSONObject stream_jsonO = getStream("");
						
						// �X�g���[����continuation��ۑ�
						continuation = getContinuationId(stream_jsonO);
						
						// �X�g���[���̃A�C�e�����擾
						JSONArray items_jsonA = stream_jsonO.getJSONArray("items");
						
						// �G���g���[���擾�B ���łɂ��̂��l�܂��Ă���ꍇ�͍폜����B
						if(entries != null){ entries = null; }
						entries = new Vector();
						for(int i=0; i<items_jsonA.length(); i++)
						{
							entries.addElement(new Entry(items_jsonA.getJSONObject(i), _feedlyapi));
						}
						
						// �V�����G���g���[���e�[�u���ɒǉ�����B
						addRowToRichList(entries, 0);
						
						// �A�N�e�B�r�e�B�C���W�P�[�^�[���폜
						_screen.deleteActivityIndicator();
						
					} catch (final Exception e) {
						// �G���[�����M���O
						updateStatus("enter() " + e.toString());
						
						// �Ď��s���Ă��������Ȃ��ꍇ�́AHome�X�e�C�g�ɖ߂�
						UiApplication.getUiApplication().invokeLater(new Runnable()
						{
							public void run()
							{
								
								Dialog.alert("An unexpected error occurred.");
								_feedlyclient.changeStateToHomeState();
							}
						});
						
					} finally {
						// �A�N�e�B�r�e�B�C���W�P�[�^�[���폜
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
					
					// ���Ǎ��̃G���g���[������ꍇ�͓ǂݍ��ށB
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
		// �G���g���[�̃^�C�g��
		out += _entry.getTitle();
		
		// �t�B�[�h�̃^�C�g��
		out +=  " / " + _entry.getOriginTitle();
		
		// �G���g���[��URL
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
		// �G���g���[����0�̏ꍇ�̓_�C�A���O���o���ă��^�[��
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
		
		// ���ׂĊ��ǃ{�^���ǉ�
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
				
				// �C���f�b�N�X��i�߂�B
				if(index == entries.size()-1 && isAvailableMoreEntries()) {
					
					// ���Ǎ��̃G���g���[������ꍇ�͓ǂݍ��ށB
					_entryScreen.showActivityIndicator();
					getMoreEntries();
					_entryScreen.hideActivityIndicator();
					index++;
					
				} else if(index == entries.size()-1) {
					
					// ����ȏ�G���g���[���Ȃ��ꍇ�́A�_�C�A���O���o���āA���^�[���B
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
					// �G���g���[���e���擾
					Entry _entry = (Entry)entries.elementAt(index);
					
					// 
					String title = _entry.getTitle();
					String origin_title = _entry.getOriginTitle();
					String published = _entry.getPublished();
					
					// �S���\�������N
					String url = "";
					if(!_entry.getAlternateHref().equals("")) {
						url = _entry.getAlternateHref();
					} else if(!_entry. getOriginId().equals("")) {
						url = _entry.getOriginId();
					}
					
					// �\��������HTML���쐬�B
					String html = makeHTMLOfEntry(_entry);
					
					// �\�����X�V
					_entryScreen.displayContent(index, title, origin_title, published, url, html);
					
					// ���ǃG���g���[�Ȃ�AFeedly�Ɋ��ǃR�}���h�𑗐M�A�ΏۃG���g���[�����Ǖ\���ɕύX
					if(_entry.isUnread())
					{
						makeEntryAsRead(index);
					}
					
				} catch (final Exception e) {
					// �G���[�����M���O
					updateStatus("displayNextEntry() " + e.toString());
					
					// ���s���܂����_�C�A���O�\��
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
				
				// �C���f�b�N�X��i�߂�B
				if(isFirstEntry(index)) {
					
					// ����ȏ�G���g���[���Ȃ��ꍇ�̓��^�[���B
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
					// �G���g���[���e���擾
					Entry _entry = (Entry)entries.elementAt(index);
					
					// 
					String title = _entry.getTitle();
					String origin_title = _entry.getOriginTitle();
					String published = _entry.getPublished();
					
					// �S���\�������N
					String url = "";
					if(!_entry.getAlternateHref().equals("")) {
						url = _entry.getAlternateHref();
					} else if(!_entry. getOriginId().equals("")) {
						url = _entry.getOriginId();
					}
					
					// �\��������HTML���쐬�B
					String html = makeHTMLOfEntry(_entry);
					
					// �\�����X�V
					_entryScreen.displayContent(index, title, origin_title, published, url, html);
					
					// ���ǃG���g���[�Ȃ�AFeedly�Ɋ��ǃR�}���h�𑗐M�A�ΏۃG���g���[�����Ǖ\���ɕύX
					if(_entry.isUnread())
					{
						makeEntryAsRead(index);
					}
					
				} catch (final Exception e) {
					
					// �G���[�����M���O
					updateStatus("displayPrevEntry() " + e.toString());
					
					// ���s���܂����_�C�A���O�\��
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
		// �A�N�e�B�r�e�B�C���W�P�[�^�[��\��
		_screen.showActivityIndicator();
		
		// �G���g���[���Ȃ��ꍇ�̓��^�[��
		if(!isAvailableMoreEntries())
		{
			// �A�N�e�B�r�e�B�C���W�P�[�^�[���폜
			_screen.deleteActivityIndicator();
			
			// ���s���܂����_�C�A���O�\��
			UiApplication.getUiApplication().invokeLater(new Runnable()
			{
				public void run()
				{
					Dialog.alert("No more entries found.");
				}
			});
			return;
		}
		
		// �L���ȒʐM�o�H���Ȃ��ꍇ�̓��^�[��
		if(!Network.isCoverageSufficient())
		{
			// �A�N�e�B�r�e�B�C���W�P�[�^�[���폜
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
		
		// �ǉ��̃G���g���[���擾
		try {
			// �����G���g���[�����擾
			int start_index = entries.size();
			
			// �ǉ��̃G���g���[���擾
			JSONObject stream_jsonO = getStream(continuation);
			
			// �X�g���[����continuation��ۑ�
			continuation = getContinuationId(stream_jsonO);
			
			// �X�g���[���̃A�C�e�����擾
			JSONArray extent_stream = stream_jsonO.getJSONArray("items");
			
			// �ǉ��G���g���[��ǉ��B
			Vector extent_entries = new Vector();
			for(int i=0; i<extent_stream.length(); i++)
			{
				Entry _entry = new Entry(extent_stream.getJSONObject(i), _feedlyapi);
				extent_entries.addElement(_entry);
				entries.addElement(_entry);
			}
			
			// �V�����G���g���[���e�[�u���ɒǉ�����B
			addRowToRichList(extent_entries, start_index);
			
		} catch (final Exception e) {
			
			// �G���[�����M���O
			updateStatus("getMoreEntries() " + e.toString());
			
			// ���s���܂����_�C�A���O�\��
			UiApplication.getUiApplication().invokeLater(new Runnable()
			{
				public void run()
				{
					
					Dialog.alert("An unexpected error occurred while getting more entries.");
				}
			});
		} finally {
			// �A�N�e�B�r�e�B�C���W�P�[�^�[���폜
			_screen.deleteActivityIndicator();
		}
	} //getMoreEntries()
	
	
	private JSONObject getStream(String continuation) throws Exception
	{
		//
		// �I�v�V������ݒ�
		//
		StringBuffer option = new StringBuffer("&");
		
		// ���ǂ̂�
		if(unread_only) {
			option.append("unreadOnly=true");
		} else {
			option.append("unreadOnly=false");
		}
		
		// �擾�G���g���[��
		//option.append("&count=25");
		
		// �R���e�B�j���[�V����
		if(!continuation.equals("")) {
			option.append("&continuation=" + continuation);
		}
		
		//
		// �X�g���[�����擾���ă��^�[��
		//
		return _feedlyapi.getTheContentOfaStream(streamId + option.toString());
	} //getStream()
	
	
	private void makeAllEntriesAsRead()
	{
		// �L���ȒʐM�o�H���Ȃ��ꍇ�̓��^�[��
		if(!Network.isCoverageSufficient())
		{
			// �A�N�e�B�r�e�B�C���W�P�[�^�[���폜
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
				
		// �_�C�A���O���o���Ď��s���Ă悢���m�F����B
		int select = Dialog.ask(Dialog.D_OK_CANCEL, "Do you really want to make all entries as read?", Dialog.NO);
		if(select == Dialog.NO) { return; }
		
		new Thread()
		{
			public void run()
			{
				Vector ids = new Vector();
				
				// �Ώۂ̃G���g���[�����擾
				int num_items = entries.size();
				
				try {
					// �A�N�e�B�r�e�B�C���W�P�[�^�[��\��
					_screen.showActivityIndicator();
					
					// �G���g���[���Ƃɏ�������
					for(int i=0; i<num_items; i++)
					{
						Entry _entry = (Entry)entries.elementAt(i);
						
						// ���łɊ��ǂ̏ꍇ�̓X�L�b�v
						if(!_entry.isUnread()) { continue; }
						
						// �G���g���[�����ǂɂ���
						_entry.makeAsRead();
							
						// API�ɓ�����G���g���[id���܂Ƃ߂�B
						ids.addElement(_entry.getId());
					}
					
					// ���ǂɂ���G���g���[���Ȃ��ꍇ�̓��^�[��
					if(ids.size() == 0) { return; }
					
					// �\�����X�V����B
					_screen.refreshRichList();
					
					// JSON�f�[�^��POST
					_feedlyapi.markOneOrMultipleArticlesAsRead(ids);

				} catch (final Exception e) {
					
					// ���s������G���g���[�𖢓ǂɖ߂��B
					for(int i=0; i<num_items; i++)
					{
						Entry _entry = (Entry)entries.elementAt(i);
						_entry.makeAsUnread();
					}
					
					// �\�����߂��B
					_screen.refreshRichList();
					
					// �G���[�����M���O
					updateStatus("makeAllEntriesAsRead() " + e.toString());
					
					// ���s���܂����_�C�A���O�\��
					UiApplication.getUiApplication().invokeLater(new Runnable()
					{
						public void run()
						{
							Dialog.alert("An unexpected error occurred while making entries as Read");
						}
					});
					
				} finally {
					// �A�N�e�B�r�e�B�C���W�P�[�^�[���폜
					_screen.deleteActivityIndicator();
				}
			} //run()
		}.start(); //Thread()
	} // makeAllEntriesAsRead()
	
	
	private void makeEntryAsRead(final int row_number)
	{
		// �L���ȒʐM�o�H���Ȃ��ꍇ�̓��^�[��
		if(!Network.isCoverageSufficient())
		{
			// �A�N�e�B�r�e�B�C���W�P�[�^�[���폜
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
					// �A�N�e�B�r�e�B�C���W�P�[�^�[��\��
					//_screen.showActivityIndicator();
					
					// unread���ڂ��A�b�v�f�[�g
					Entry _entry = (Entry)entries.elementAt(row_number);
					_entry.makeAsRead();
					
					
					// �\�����X�V
					_screen.refreshRichList();
					
					// saved�ɂ���G���g���[��ID���擾
					String entryId = _entry.getId();
					
					// API�ɓn���G���g���[ID������
					Vector entryIds = new Vector();
					entryIds.addElement(entryId);
					
					// Feedly API��@���B
					_feedlyapi.markOneOrMultipleArticlesAsRead(entryIds);
					
				} catch (final Exception e) {
					
					// ���s������unread���ڂ�߂��B
					Entry _entry = (Entry)entries.elementAt(row_number);
					_entry.makeAsUnread();
					
					// �\�����߂��B
					_screen.refreshRichList();
					
					// �G���[�����M���O
					updateStatus("makeEntryAsRead() " + e.toString());
					
					// ���s���܂����_�C�A���O�\��
					UiApplication.getUiApplication().invokeLater(new Runnable()
					{
						public void run()
						{
							Dialog.alert("An unexpected error occurred while making entry as Read");
						}
					});
					
				} finally {
					// �A�N�e�B�r�e�B�C���W�P�[�^�[���폜
					//_screen.deleteActivityIndicator();
				}
			} //run()
		}.start(); //Thread()
	} //makeEntryAsRead()
	
	
	private String makeHTMLOfEntry(Entry _entry)
	{
		//
		// �\������HTML���쐬
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
		
		// ��؂�
		body += "<hr />";
		*/

		// Visual
		String visual_url = _entry.getVisualUrl();
		int visual_width = _entry.getVisualWidth();
		
		if(!visual_url.equals(""))
		{
			// �摜�̉�������ʕ��𒴂��Ă���ꍇ�́A�摜���𒲐�����
			if(visual_width > (Display.getWidth()-20)) {
				int width = Display.getWidth() - 20;
				body += "<p class=\"visual\"><img src=\"" + visual_url + "\" width=\"" + width + "\"></p>";
			} else {
				body += "<p class=\"visual\"><img src=\"" + visual_url + "\"></p>";
			}
		}
		
		// Content
		body += "<p>" + _entry.getContent() + "</p>";
		
		// �S���\�������N
		String alternate_href = _entry.getAlternateHref();
		String originId = _entry.getOriginId();
		if(!alternate_href.equals("")) {
			body += "<hr />";
			body += "<p class=\"button\"><input type=\"button\" value=\"Visit Website\" onClick=\"location.href=\'" + alternate_href + "'\"></p>";
		} else if(alternate_href.equals("") && !originId.equals("")) {
			body += "<hr />";
			body += "<p class=\"button\"><input type=\"button\" value=\"Visit Website\" onClick=\"location.href=\'" + originId + "'\"></p>";
		}
		
		// ����
		String out = "<html><head><style type=\"text/css\">" + style + "</style></head><body>" + body + "</body>" + "</html>";
		
		return out;
	} //makeHTMLFromHashtable()
	
	
	private void refresh()
	{
		// �L���ȒʐM�o�H���Ȃ��ꍇ�̓��^�[��
		if(!Network.isCoverageSufficient())
		{
			// �A�N�e�B�r�e�B�C���W�P�[�^�[���폜
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
			// �A�N�e�B�r�e�B�C���W�P�[�^�[��\��
			_screen.showActivityIndicator();
			
			// �X�N���[���^�C�g�����X�V
			if(unread_only) {
				_screen.setScreenTitle("(Unread) " + screen_title);
			} else {
				_screen.setScreenTitle(screen_title);
			}
			
			// �X�g���[�����擾�B
			JSONObject stream_jsonO = getStream("");
			
			// �X�g���[����continuation��ۑ�
			continuation = getContinuationId(stream_jsonO);
			
			// �X�g���[���̃A�C�e�����擾
			JSONArray items_jsonA = stream_jsonO.getJSONArray("items");
			
			// �G���g���[���擾�B ���łɂ��̂��l�܂��Ă���ꍇ�͍폜����B
			if(entries != null){ entries = null; }
			entries = new Vector();
			for(int i=0; i<items_jsonA.length(); i++)
			{
				entries.addElement(new Entry(items_jsonA.getJSONObject(i), _feedlyapi));
			}
			
			// ���ׂẴG���g���[���폜
			_screen.removeAllEntriesFromRichList();
			
			// �V�����G���g���[���e�[�u���ɒǉ�����B
			addRowToRichList(entries, 0);
			
		} catch (final Exception e) {
			
			// �G���[�����M���O
			updateStatus("refresh() " + e.toString());
			
			// ���s���܂����_�C�A���O�\��
			UiApplication.getUiApplication().invokeLater(new Runnable()
			{
				public void run()
				{
					Dialog.alert("An unexpected error occurred while getting entries");
				}
			});
		} finally {
			// �A�N�e�B�r�e�B�C���W�P�[�^�[���폜
			_screen.deleteActivityIndicator();
		}
	} //refresh()
	
	
	private void showEntryScreen(int row_number)
	{
		try {
			// �X�N���[����\��
			if(_entryScreen != null) { _entryScreen = null; }
			
			// BrowserField���o�O��̂ŁA����V�K�쐬�B
			_entryScreen = new Screen_Entry(this);
			
			_feedlyclient.pushScreen(_entryScreen);
			
			// �N���b�N���ꂽ�G���g���[���擾
			Entry _entry = (Entry)entries.elementAt(row_number);
			
			// 
			String title = _entry.getTitle();
			String origin_title = _entry.getOriginTitle();
			String published = _entry.getPublished();
			
			// �S���\�������N
			String url = "";
			if(!_entry.getAlternateHref().equals("")) {
				url = _entry.getAlternateHref();
			} else if(!_entry. getOriginId().equals("")) {
				url = _entry.getOriginId();
			}
			
			// �\��������HTML���쐬�B
			String html = makeHTMLOfEntry(_entry);
			
			_entryScreen.displayContent(row_number, title, origin_title, published, url, html);
			
		} catch (final Exception e) {
			
			// �G���[�����M���O
			updateStatus("showEntryScreen() " + e.toString());
			
			// ���s���܂����_�C�A���O�\��
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
		// ���݂�unread_only�l����t���O���X�V
		if(unread_only) {
			unread_only = false;
		} else {
			unread_only = true;
		}
		
		// �\�����e���X�V����B
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
		// �L���ȒʐM�o�H���Ȃ��ꍇ�̓��^�[��
		if(!Network.isCoverageSufficient())
		{
			// �A�N�e�B�r�e�B�C���W�P�[�^�[���폜
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
					// �A�N�e�B�r�e�B�C���W�P�[�^�[��\��
					_screen.showActivityIndicator();
					
					// �G���g���[��unread�̒l����ړI��ݒ�
					if(isUnreadEntry(index)) {
						desired_unread_status = false;
						current_unread_status = true;
					} else {
						desired_unread_status = true;
						current_unread_status = false;
					}
					
					// unread���ڂ��A�b�v�f�[�g
					Entry _entry = (Entry)entries.elementAt(index);
					if(desired_unread_status) {
						_entry.makeAsUnread();
					} else {
						_entry.makeAsRead();
					}
					
					// �\�����X�V
					_screen.refreshRichList();
					
					// saved�ɂ���G���g���[��ID���擾
					String entryId = _entry.getId();
					
					// API�ɓn���G���g���[ID������
					Vector entryIds = new Vector();
					entryIds.addElement(entryId);
					
					// Feedly API��@���B
					if(current_unread_status) {
						_feedlyapi.markOneOrMultipleArticlesAsRead(entryIds);
					} else {
						_feedlyapi.keepOneOrMultipleArticlesAsUnread(entryIds);
					}
					
				} catch (final Exception e) {
					
					// ���s������unread���ڂ�߂��B
					Entry _entry = (Entry)entries.elementAt(index);
					if(desired_unread_status) {
						_entry.makeAsUnread();
					} else {
						_entry.makeAsRead();
					}
					
					// �\�����߂��B
					_screen.refreshRichList();
					
					// �G���[�����M���O
					updateStatus("toggleUnreadAndRead()" + e.toString());
					
					// ���s���܂����_�C�A���O�\��
					UiApplication.getUiApplication().invokeLater(new Runnable()
					{
						public void run()
						{
							Dialog.alert("An unexpected error occurred while making entry as Unread/Read");
						}
					});
					
				} finally {
					// �A�N�e�B�r�e�B�C���W�P�[�^�[���폜
					_screen.deleteActivityIndicator();
				}
			} //run()
		}.start(); //Thread()
	} //tagEntries()
	
	
	private void toggleUnsavedAndSaved(final int index)
	{
		// �L���ȒʐM�o�H���Ȃ��ꍇ�̓��^�[��
		if(!Network.isCoverageSufficient())
		{
			// �A�N�e�B�r�e�B�C���W�P�[�^�[���폜
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
					// �A�N�e�B�r�e�B�C���W�P�[�^�[��\��
					_screen.showActivityIndicator();
					
					// �G���g���[��Save�󋵂���ړI��ݒ�
					if(isSavedEntry(index)) {
						desired_savedtag_status = false;
						current_savedtag_status = true;
					} else {
						desired_savedtag_status = true;
						current_savedtag_status = false;
					}
					
					// saved���ڂ��A�b�v�f�[�g
					Entry _entry = (Entry)entries.elementAt(index);
					if(desired_savedtag_status) {
						_entry.makeAsSaved();
					} else {
						_entry.makeAsUnsaved();
					}
					
					// �\�����X�V
					_screen.updateSavedAndUpdatedField(index);
					
					// �G���g���[�X�N���[�����\������Ă���ꍇ�́A��������X�V�B
					if(_entryScreen != null && _entryScreen.isVisible())
					{
						_entryScreen.updateSavedAndUpdatedField(index);
					}
					
					//
					String tagId = "user/" + _feedlyclient.getID() + "/tag/global.saved";
					
					// saved�ɂ���G���g���[��ID���擾
					String entryId = _entry.getId();
					
					// Feedly API��@���B
					if(current_savedtag_status) {
						Vector entryIds = new Vector();
						entryIds.addElement(entryId);
						_feedlyapi.untagMultipleEntries(tagId, entryIds);
					} else {
						_feedlyapi.tagEntry(tagId, entryId);
					}
					
				} catch (final Exception e) {
					
					// ���s������saved���ڂ�߂��B
					Entry _entry = (Entry)entries.elementAt(index);
					if(desired_savedtag_status) {
						_entry.makeAsSaved();
					} else {
						_entry.makeAsUnsaved();
					}
					
					// �\�����߂��B
					_screen.updateSavedAndUpdatedField(index);
					
					// �G���g���[�X�N���[�����\������Ă���ꍇ�́A��������߂��B
					if(_entryScreen != null && _entryScreen.isVisible())
					{
						_entryScreen.updateSavedAndUpdatedField(index);
					}
					
					// �G���[�����M���O
					updateStatus("toggleUnsavedAndSaved() " + e.toString());
					
					// ���s���܂����_�C�A���O�\��
					UiApplication.getUiApplication().invokeLater(new Runnable()
					{
						public void run()
						{
							Dialog.alert("An unexpected error occurred while tagging entry.");
						}
					});
					
				} finally {
					// �A�N�e�B�r�e�B�C���W�P�[�^�[���폜
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
						// �t�H�[�J�X���ꂽRow�̃C���f�b�N�X���擾
						final int row_number = _screen.getRowNumberWithFocus();
						
						// EntryScreen��\��
						showEntryScreen(row_number);
						
						// ���ǃG���g���[�Ȃ�AFeedly�Ɋ��ǃR�}���h�𑗐M�A�ΏۃG���g���[�����Ǖ\���ɕύX
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
			// the article�fs title. This string does not contain any HTML markup.
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
			// If present, �gstreamId�h will contain the feed id,
			// �gtitle�h will contain the feed title, and �ghtmlUrl�h will contain the feed�fs website.
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
			// A list of tag objects (�gid�h and �glabel�h) that the user added to this entry.
			try {
				JSONArray tags = source.getJSONArray("tags");
				
				// saved���ڂ́A�f�t�H���g��false��ݒ�B
				this.saved = false;
				
				for(int j=0; j<tags.length(); j++)
				{
					JSONObject tag = tags.getJSONObject(j);
					
					// Saved�^�O���`�F�b�N
					if(tag.getString("id").endsWith("global.saved")) {
						this.saved = true;
					}
				}
			} catch (JSONException e) {
				this.saved = false;
			}
			
			
			
			//-- �G���g���[�̏ڍ׉�ʗp�v�f --------------------------------------------//
			
			// OriginId
			try {
				this.originId = source.getString("originId");
			} catch (JSONException e) {
				this.originId = "";
			}
			
			// Alternate(�͂��߂�1�̂ݒ��o�j
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
				// �Ȃ��ꍇ�̓X���[
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
