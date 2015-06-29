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
		// �X�N���[���g�����W�V������ݒ�
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
		
		
		// Feedly�Ƀ��O�C�����Ă��Ȃ��ꍇ�́AAuth�X�e�C�g�֕ύX
		if(!_feedlyclient.isLogin())
		{
			_feedlyclient.changeState(new State_Auth(_feedlyclient));
			return;
		}
		
		// Home�X�N���[����\��
		if(_screen == null)
		{
			_screen = new Screen_Home(this);
		}
		_feedlyclient.pushScreen(_screen);
		
		
		//
		// �w�ǒ��̃t�B�[�h���擾
		//
		// ���łɎ擾���Ă���ꍇ�́A�V���Ɏ擾���Ȃ��B
		if(categories != null) { return; }
		
		
		// �L���ȒʐM�o�H�����邩�m�F
		if(Network.isCoverageSufficient())
		{	
			new Thread()
			{
				public void run()
				{
					try {
						// �A�N�e�B�r�e�B�C���W�P�[�^�[��\��
						_screen.showActivityIndicator();
						
						// �w�ǒ��̃t�B�[�h���擾
						JSONArray subscriptions = _feedlyapi.getUserSubscriptions();
						
						// �t�B�[�h���J�e�S����������
						categories = doCategorize(subscriptions);
						
						// �t�B�[�h���X�N���[���ɒǉ��B
						for(Enumeration e = categories.elements(); e.hasMoreElements();)
						{
							Category tmp_category = (Category)e.nextElement();
							tmp_category.doAddCategoryRichList();
						}
						
						_screen.setFocusToStartPos();
						
						// unread�Aupdated���X�V�B
						refreshUnreadCounts();
						
					} catch (Exception e) {
						// �G���[�����M���O
						updateStatus("enter() " + e.toString());
						
						// �Ď��s���Ă��������Ȃ��ꍇ�́A�A�v�������I��
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
						// �A�N�e�B�r�e�B�C���W�P�[�^�[���폜
						_screen.deleteActivityIndicator();
					}
				} //run()
			}.start(); //Thread()
			
		// �L���ȒʐM�o�H���Ȃ��ꍇ
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
		// �f�o�O�p
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
				// �L���ȒʐM�o�H���Ȃ��ꍇ�̓��^�[��
				if(!Network.isCoverageSufficient())
				{
					Dialog.alert("A communication error has occurred. Please make sure your device is connected to Internet.");
					return;
				}
				
				// �_�C�A���O���o���Ď��s���Ă悢���m�F����B
				int select = Dialog.ask(Dialog.D_OK_CANCEL, "Do you really want to log out?", Dialog.NO);
				if(select == Dialog.NO) { return; }
				
				// ���O�A�E�g
				new Thread()
				{
					public void run()
					{
						try {
							// �A�N�e�B�r�e�B�C���W�P�[�^�[��\��
							_screen.showActivityIndicator();
							
							// ���O�A�E�g
							_feedlyclient.logoutFeedly();
							
						} catch (final Exception e) {
							// �G���[�����M���O
							updateStatus("CMD_logout() " + e.toString());
							
							// ���s�����狭���I�Ƀ��O�A�E�g
							_feedlyclient.forceLogoutFeedly();
							
							// ���s���܂����_�C�A���O�\��
							/*UiApplication.getUiApplication().invokeLater(new Runnable()
							{
								public void run()
								{
									Dialog.alert("An unexpected error occurred while logging out.");
								}
							});*/
							
						} finally {
							// �A�N�e�B�r�e�B�C���W�P�[�^�[���폜
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
				// �L���ȒʐM�o�H���Ȃ��ꍇ�̓��^�[��
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
				// �L���ȒʐM�o�H���Ȃ��ꍇ�̓��^�[��
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
							// �t�H�[�J�X���Ă���J�e�S���̃C���f�b�N�X���擾
							int index = _screen.getRowNumberWithFocus();
							
							// index == 0 ��Global�Ȃ̂ŏ������Ȃ��B
							if(index == 0) { return; }
							
							_screen.showActivityIndicator();
							
							// �t�H�[�J�X���Ă���J�e�S�����擾
							Category _tmp = (Category)categories.elementAt(index);
							
							// ���X�g�̊J�����s
							_tmp.toggleShowAndHideFeeds();
							
							// ���X�g���J���Ă���ꍇ�́Aunread�Aupdated���X�V
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
			// �A�N�e�B�r�e�B�C���W�P�[�^�[��\��
			//_screen.showActivityIndicator();
			
			JSONArray unreadlist = _feedlyapi.getListOfUnreadCounts().getJSONArray("unreadcounts");
			
			for(int i=0; i<unreadlist.length(); i++)
			{
				JSONObject source = unreadlist.getJSONObject(i);
				String id = source.getString("id");
				int count = source.getInt("count");
				String updated =  _feedlyapi.getTime(source.getLong("updated"));
				
				// ���ꂼ��̃J�e�S�����ƂɃ��t���b�V�����\�b�h�����s
				for(Enumeration e = categories.elements(); e.hasMoreElements();)
				{
					Category category = (Category)e.nextElement();
					category.refreshUnreadAndUpdated(id, count, updated);
				}
			}
		} catch (final Exception e) {
			// �G���[�����M���O
			updateStatus("refreshUnreadCounts() " + e.toString());
			
			// ���s���܂����_�C�A���O�\��
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
		
		// ���炩����Global�J�e�S���[��ǉ��B
		out.addElement(new Global("Global"));
		
		// �擾�����w�ǒ��t�B�[�h�ꗗ�Ɉُ킪����ꍇ�́AGlobal�̂ݒǉ����ă��^�[��
		if(subscriptions == null) { return out; }
		
		// �w�ǒ��̃t�B�[�h���Ȃ��ꍇ�́AGlobal�̂ݒǉ����ă��^�[��
		if(subscriptions.length() == 0) { return out; }
		
		// ���炩����Uncategorized�J�e�S���[���쐬
		String streamid_uncategorized = "user/" + _feedlyclient.getID() + "/category/global.uncategorized";
		Category _uncategorized = new Category("Uncategorized", streamid_uncategorized, true);
		
		// �w�ǒ��̃t�B�[�h���ƂɃJ�e�S����U�蕪��
		for(int i=0; i<subscriptions.length(); i++)
		{
			// �t�B�[�h�̏����擾
			JSONObject feed_jsonObject = subscriptions.getJSONObject(i);
			Feed _feed = new Feed(feed_jsonObject);
			
			// �t�B�[�h�̃J�e�S�������擾
			JSONArray categorys = feed_jsonObject.getJSONArray("categories");
			
			// �J�e�S���̎w�肪�Ȃ��ꍇ�́AUncategorized�J�e�S���[�֓o�^����B
			if(categorys.length() == 0)
			{
				_uncategorized.addFeed(_feed);
				continue;
			}
			
			// �t�B�[�h�̃J�e�S���w������
			label: for(int j=0; j<categorys.length(); j++)
			{
				// �t�B�[�h�̃J�e�S�������擾
				JSONObject category = categorys.getJSONObject(j);
				String category_name = category.getString("label");
				String stream_id = category.getString("id");
				
				// �����̃J�e�S���𒲍�
				for(Enumeration e = out.elements(); e.hasMoreElements();)
				{
					// �����̃J�e�S�������擾
					Category _ctgry = (Category)e.nextElement();
					
					// ���v����J�e�S��������ꍇ�́A����ɒǉ��B
					if(_ctgry.getCategoryName().equals(category_name))
					{
						_ctgry.addFeed(_feed);
						break label;
					}
				}
				
				// ���v����J�e�S�����Ȃ��ꍇ�́A�J�e�S����V�K�쐬����B
				Category _new_category = new Category(category_name, stream_id, true);
				_new_category.addFeed(_feed);
				out.addElement(_new_category);
			}
		} //for
		
		// �Ō��Uncategorized�J�e�S���[��ǉ��i1�ȏ�t�B�[�h���ǉ�����Ă���ꍇ�̂݁j
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
			
			// �t�B�[�h�i�[�p�̃x�N�^�[���쐬
			ids = new Vector();
		}
		
		
		public void addFeed(Feed feed)
		{
			ids.addElement(feed);
		}
		
		
		public void doDeleteAllFeedsFromRichList()
		{
			// ���b�`���X�g�ɒǉ�����Ă���v�f�����擾
			int num_of_rows = _list.getModel().getNumberOfRows();
			
			// �J�e�S�����̕������炷
			num_of_rows--;
			
			// �폜
			synchronized (UiApplication.getEventLock()) 
			{
				for(int i=0; i<num_of_rows; i++)
				{
					// 1�s�ڂ̓J�e�S�����Ȃ̂ŁA����2�s�ڂ��폜����B
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
				
				// �Ƃ肠�������ǂɂ��Ă����B���ƂōX�V����B
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
			// ID�����v����ꍇ�A�J�e�S���̏����X�V
			if(id.equals(streamID))
			{
				_screen.refreshCategoryHeaderUnread(_list, category_name, num_unread);
				return;
			}
			
			// ���X�g�����Ă���ꍇ�̓t�B�[�h�̏��͍X�V���Ȃ�
			if(isCollapsed) { return; }
			
			// ID�����v����ꍇ�AFeed�̏����X�V
			for(int i=0; i<ids.size(); i++)
			{
				Feed feed = (Feed)ids.elementAt(i);
				
				if(id.equals(feed.getId()))
				{
					// rowIndex�̓w�b�_�␳�̂���+1����B
					int rowIndex = i+1;
					
					_screen.refreshUnreadAndUpdated(_list, rowIndex, num_unread, updated);
					
					return;
				}
			}
		} //refreshUnreadAndUpdated()
		
		
		public void toggleShowAndHideFeeds()
		{
			if(isCollapsed) {
				// �t�B�[�h��ǉ�����B
				doAddCategoryRow();
				isCollapsed = false;
			} else {
				// ���X�g�ɒǉ�����Ă���t�B�[�h���폜����
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
					// �t�H�[�J�X�ʒu���擾
					int focusrow = _list.getFocusRow();
					
					// �t�H�[�J�X�ʒu��0�̏ꍇ�́A�w�b�_�Ƀt�H�[�J�X�����ꍇ�͂Ȃ̂ŁA�J�e�S����\���������B
					if(focusrow == 0)
					{
						_feedlyclient.changeState(new State_Stream(_feedlyclient, streamID, category_name, false));
						return;
					}
					
					// �t�H�[�J�X�����t�B�[�h���擾�iindex�̓w�b�_�␳�̂���-1����B�j
					Feed _feed = (Feed)ids.elementAt(focusrow-1);
					
					// �X�e�C�g���`�F���W
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
					// �t�H�[�J�X�ʒu���擾
					int focusrow = _list.getFocusRow();
					
					// �t�H�[�J�X�ʒu��0�̏ꍇ�́AALL�X�g���[����\������B
					if(focusrow == 1) {
						_feedlyclient.changeState(new State_Stream(_feedlyclient, "user/" + _feedlyclient.getID() + "/category/global.all", "ALL", true));
					} else if(focusrow == 2) {
						// �t�H�[�J�X�ʒu��1�̏ꍇ�́ASaved�X�g���[����\������B
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
