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
		// �^�C�g���o�[���쐬
		//
		_titleBar = new StandardTitleBar();
		_titleBar.addSignalIndicator();
		_titleBar.addNotifications();
		_titleBar.addIcon("img/icon.png");
		setTitleBar(_titleBar);
		
		//
		// ���j���[�A�C�e�����쐬
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
		// ���C���}�l�[�W���[�쐬�B
		//
		_mainVFM = new VerticalFieldManager()
		{
			protected boolean keyChar(char ch, int status, int time)
			{
				switch(ch)
				{
					case Keypad.KEY_SPACE:
						// �I���W�i���R���e���c���f�t�H���g�u���E�U�ŕ\��
						Browser.getDefaultSession().displayPage(url);
						break;
						
					case 'b':
						// �ŉ����փX�N���[���B
						getMainManager().setVerticalScroll(_mainVFM.getContentHeight()-_mainVFM.getVisibleHeight(), false);
						break;
						
					case 'n':
						// ���̃G���g���[��\��
						_state.CMD_displayNextEntry().execute("");
						break;
					
					case 'p':
						// �O�̃G���g���[��\��
						_state.CMD_displayPrevEntry().execute("");
						break;
						
					case 's':
						// Saved�^�O��t�������������B
						_state.CMD_toggleUnsavedAndSaved(index).execute("");
						break;
						
					case 't':
						// �ŏ㕔�փX�N���[���B
						getMainManager().setVerticalScroll(Manager.TOPMOST, false);
						break;
				}
				
				return super.keyChar(ch, status, time);
			} //keyChar()
		};
		add(_mainVFM);
		
		
		//
		// �_�~�[�̃w�b�_���쐬�B
		//
		_mainVFM.add(new VerticalFieldManager());
		
		
		//
		// �u���E�U�t�B�[���h���쐬�B
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
		
		// �i�r�Q�[�V�������[�h���|�C���^�ɐݒ�
		_browserField.getConfig().setProperty(BrowserFieldConfig.NAVIGATION_MODE, BrowserFieldConfig.NAVIGATION_MODE_POINTER);
		
		// �I�[�g�t�H�[�J�X������
		_browserField.getConfig().setProperty(BrowserFieldConfig.DISABLE_AUTO_FOCUS, Boolean.TRUE);
		
		// �r���[�|�[�g��ݒ�
		_browserField.getConfig().setProperty(BrowserFieldConfig.VIEWPORT_WIDTH, new Integer(Display.getWidth()));
		
		_mainVFM.add(_browserField);
		//add(_browserField);
		
		// �A�N�e�B�r�e�B�C���W�P�[�^�[���쐬
		_activity_indicator = new MyActivityIndicator(this);
	}
	
	
	protected void makeMenu(Menu menu, int instance)
	{
		super.makeMenu(menu, instance);
		
		// Send���j���[���쐬
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
		
		// Next�APrev���j���[�A�C�e����ǉ�
		if(_state.isFirstEntry(index) && !_state.isOnlyOneEntry(index)) {
			
			// �O�̃G���g���[�͂Ȃ��B���̃G���g���[�͂���B
			menu.add(makeNextEntryMenuItem());
			
		} else if(_state.isLastEntry(index) && !_state.isOnlyOneEntry(index)){
			
			// ���̃G���g���[�͂Ȃ��B�O�̃G���g���[�͂���B
			menu.add(makePrevEntryMenuItem());
			
		} else if(_state.isOnlyOneEntry(index)) {
			// PASS
		} else {
			
			// ���̃G���g���[���O�̃G���g���[������B
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
		// ���̃G���g���[�̃C���f�b�N�X��ۑ�
		this.index = index;
		
		// �S���\�������N��ۑ�
		this.url = url;
		
		// �X�N���[���^�C�g�����X�V
		synchronized (UiApplication.getEventLock())
		{
			_titleBar.addTitle(title);
		}
		
		
		// �V�����w�b�_���쐬���āA�Â��w�b�_�Ɠ���ւ���
		synchronized (UiApplication.getEventLock())
		{
			VerticalFieldManager _oldHeader = (VerticalFieldManager)_mainVFM.getField(0);
			VerticalFieldManager _newHeader = makeHeader(index, title, origin_title, published);
			_mainVFM.replace(_oldHeader, _newHeader);
		}
		
		// �u���E�U�t�B�[���h��HTML��ǂݍ���
		// BrowserField Encoding problem - BlackBerry Support Community Forums
		// http://supportforums.blackberry.com/t5/Java-Development/BrowserField-Encoding-problem/td-p/1428779
		try {
			_browserField.displayContent(html.getBytes("utf-8"), "text/html; charset=utf-8", "http://localhost/");
		} catch (UnsupportedEncodingException e) {
			// PASS
		}
		
		// �ŏ㕔�փX�N���[�������Ă����B
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
		// �\�����̃��b�`���X�g���ړI�̃t�B�[���h���擾�B
		//Object[] obj = _richList.get(rowIndex);
		VerticalFieldManager _vfm = (VerticalFieldManager)_mainVFM.getField(0);
		LabelField _field = (LabelField)_vfm.getField(2);
		
		// ���ݕ\�����̃e�L�X�g���擾�B'Saved / '���܂܂�Ă���ꍇ�͍폜����B
		String org_str = _field.getText();
		if(org_str.startsWith("Saved / "))
		{
			org_str = org_str.substring(org_str.lastIndexOf('/')+2, org_str.length());
		}
		
		// Saved�t���O�ɉ������e�L�X�g��K������B
		if(_state.isSavedEntry(rowIndex)) {
			_field.setText("Saved / " + org_str);
		} else {
			_field.setText(org_str);
		}
	} //updateSavedAndUpdatedField()
	
	
	private VerticalFieldManager makeHeader(final int index, String title, String feed_title, String update)
	{
		VerticalFieldManager _vfm = new VerticalFieldManager(Manager.NO_VERTICAL_SCROLL);
		
		// �^�C�g��
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
		
		
		// �t�B�[�h��
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
		
		
		// �p�u���b�V������̌o�ߎ���
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
		
		// ��؂��
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