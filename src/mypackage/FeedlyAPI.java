/*
	FeedlyAPI.java
	
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

import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;


public class FeedlyAPI
{
	private FeedlyClient _feedlyclient = null;
	
	private String client_id = "";
	private String client_secret = "";
	private String service_host = "";
	
	private static final int NUM_OF_TRIALS = 3;
	
	
	public FeedlyAPI(FeedlyClient _feedlyclient, String client_id, String client_secret, boolean sandbox)
	{
		this._feedlyclient = _feedlyclient;
		
		this.client_id = client_id;
		this.client_secret = client_secret;
		
		if(sandbox) {
			this.service_host = "sandbox.feedly.com";
		} else {
			this.service_host = "cloud.feedly.com";
		}
	}
	
	
	
	//
	//-- Authentication -------------------------------------------------------//
	//
	public String get_code_url(String callback_url)
	{
		// Authenticating a user and obtaining a code
		//
		// The authentication scenario begins by redirecting a browser (full page or popup)
		// to a feedly cloud URL with a set of query parameters that indicate
		// the type of cloud API access the application requires.
		// feedly handles the user authentication and consent,
		// and the result is authorization code. feedly returns the code on the redirect of the response.
		//
		// GET /v3/auth/auth
		
		final String SCOPE = "https://cloud.feedly.com/subscriptions";
		final String RESPONSE_TYPE = "code";
		
		String request_url =  "";
		request_url += getEndpoint("/v3/auth/auth") + "?";
		request_url += "client_id=" + client_id + "&";
		request_url += "redirect_uri=" + callback_url + "&";
		request_url += "scope=" + SCOPE + "&";
		request_url += "response_type=" + RESPONSE_TYPE;
		
		return request_url;
	}
	
	
	public JSONObject getAccessToken(String redirect_uri, String code) throws Exception
	{
		// Exchanging a code for a refresh token and an access token
		// After you received the code, you may exchange it for an access token and a refresh token.
		// This request is an HTTPs post, and includes the following parameters:
		//
		// POST /v3/auth/token
		
		JSONObject body = new JSONObject();
		try {
			body.put("client_id", client_id);
			body.put("client_secret", client_secret);
			body.put("grant_type", "authorization_code");
			body.put("redirect_uri", redirect_uri);
			body.put("code", code);
		} catch (JSONException e) {
			throw new Exception("FeedlyAPI::getAccessToken()0");
		}
		
		// 例外を受け取った場合は、指定された回数再試行する。
		for(int i=0; i<NUM_OF_TRIALS; i++)
		{
			try {
				return new JSONObject(doPostNOCheck(getEndpoint("/v3/auth/token"), body));
			} catch (IOException e) {
				continue;
			} catch (Exception e) {
				continue;
			}
		}
		throw new Exception("FeedlyAPI::getAccessToken()1");
	}
	
	
	public JSONObject refreshAccessToken() throws IOException, Exception
	{
		// Using a refresh token
		//
		// Your application may obtain a new access token by sending a refresh token
		// to the feedly Authorization server.
		//
		// POST /v3/auth/token
		
		JSONObject body = new JSONObject();
		try {
			body.put("refresh_token", _feedlyclient.getRefreshToken());
			body.put("client_id", client_id);
			body.put("client_secret", client_secret);
			body.put("grant_type", "refresh_token");
		} catch (JSONException e) {
			throw new Exception("FeedlyAPI::refreshAccessToken()0");
		}
		
		// 例外を受け取った場合は、指定された回数再試行する。
		for(int i=0; i<NUM_OF_TRIALS; i++)
		{
			try {
				return new JSONObject(doPostNOCheck(getEndpoint("/v3/auth/token"), body));
			} catch (IOException e) {
				continue;
			} catch (Exception e) {
				continue;
			}
		}
		throw new Exception("FeedlyAPI::refreshAccessToken()1");
	} //refreshAccessToken()
	
	
	public JSONObject revokeRefreshToken() throws Exception
	{
		// Revoking a refresh token
		// 
		// This is a logout operation:
		// the refresh token passed cannot be reused to generate new access tokens.
		//
		// POST /v3/auth/token
		
		JSONObject body = new JSONObject();
		try {
			body.put("refresh_token", _feedlyclient.getRefreshToken());
			body.put("client_id", client_id);
			body.put("client_secret", client_secret);
			body.put("grant_type", "revoke_token");
		} catch (JSONException e) {
			throw new Exception("FeedlyAPI::revokeRefreshToken()0");
		}
		
		// 例外を受け取った場合は、指定された回数再試行する。
		for(int i=0; i<NUM_OF_TRIALS; i++)
		{
			try {
				return new JSONObject(doPost(getEndpoint("/v3/auth/token"), body));
			} catch (IOException e) {
				continue;
			} catch (Exception e) {
				continue;
			}
		}
		throw new Exception("FeedlyAPI::revokeRefreshToken()1");
	}
	
	
	//
	//-- Feeds ----------------------------------------------------------------//
	//
	/*public JSONArray getMetadataForListOfFeeds(Vector feedIds) throws JSONException, IOException, Exception
	{
		// Get the metadata for a list of feeds
		//
		// POST /v3/feeds/.mget
		
		String url = getEndpoint("/v3/feeds/.mget");
		JSONArray body = new JSONArray(feedIds);
		
		 return new JSONArray(doPostJSONArray(url, body));
	}*/
	
	
	//
	//-- Markers --------------------------------------------------------------//
	//
	public JSONObject getListOfUnreadCounts() throws Exception
	{
		// Get the list of unread counts
		//
		// GET /v3/markers/counts
		
		// 例外を受け取った場合は、指定された回数再試行する。
		for(int i=0; i<NUM_OF_TRIALS; i++)
		{
			try {
				return new JSONObject(doGet(getEndpoint("/v3/markers/counts")));
			} catch (IOException e) {
				continue;
			} catch (Exception e) {
				continue;
			}
		}
		throw new Exception("FeedlyAPI::getListOfUnreadCounts()");
	}
	
	
	public void markOneOrMultipleArticlesAsRead(Vector entryIds) throws Exception
	{
		// Mark one or multiple articles as read
		// POST /v3/markers
		
		if(entryIds.size() == 0)
		{
			throw new Exception("FeedlyAPI::markOneOrMultipleArticlesAsRead()0");
		}
		
		//
		JSONObject body = new JSONObject();
		try {
			body.put("action", "markAsRead");
			body.put("type", "entries");
			body.put("entryIds", entryIds);
		} catch (JSONException e) {
			throw new Exception("FeedlyAPI::markOneOrMultipleArticlesAsRead()1");
		}
		
		// 例外を受け取った場合は、指定された回数再試行する。
		for(int i=0; i<NUM_OF_TRIALS; i++)
		{
			try {
				doPost(getEndpoint("/v3/markers"), body);
				return;
			} catch (IOException e) {
				continue;
			} catch (Exception e) {
				continue;
			}
		}
		throw new Exception("FeedlyAPI::markOneOrMultipleArticlesAsRead()2");
	}
	
	
	public void keepOneOrMultipleArticlesAsUnread(Vector entryIds) throws IOException, Exception
	{
		// Keep one or multiple articles as unread
		// POST /v3/markers
		
		if(entryIds.size() == 0)
		{
			throw new Exception("FeedlyAPI::keepOneOrMultipleArticlesAsUnread()0");
		}
		
		//
		JSONObject json = new JSONObject();
		try {
			json.put("action", "keepUnread");
			json.put("type", "entries");
			json.put("entryIds", entryIds);
		} catch (JSONException e) {
			throw new Exception("FeedlyAPI::keepOneOrMultipleArticlesAsUnread()1");
		}
		
		// 例外を受け取った場合は、指定された回数再試行する。
		for(int i=0; i<NUM_OF_TRIALS; i++)
		{
			try {
				doPost(getEndpoint("/v3/markers"), json);
				return;
			} catch (IOException e) {
				continue;
			} catch (Exception e) {
				continue;
			}
		}
		throw new Exception("FeedlyAPI::keepOneOrMultipleArticlesAsUnread()2");
	}
	
	
	//
	//-- Streams --------------------------------------------------------------//
	//
	public JSONObject getTheContentOfaStream(String streamId) throws Exception
	{
		// GET /v3/streams/:streamId/contents <- NOT WORK
		// or
		// GET /v3/streams/contents?streamId=:streamId
		//
		// [streamId]
		// Feed Id
		// feed/:url example: feed/http://feeds.engadget.com/weblogsinc/engadget
		// Category Id
		// user/:userId/category/:label example: user/c805fcbf-3acf-4302-a97e-d82f9d7c897f/category/tech
		// Tag Id
		// user/:userId/tag/:label example: user/c805fcbf-3acf-4302-a97e-d82f9d7c897f/tag/inspiration
		
		
		//return new JSONObject(doGet(getEndpoint("/v3/streams/contents?streamId=" + streamId)));
		
		
		// 例外を受け取った場合は、指定された回数再試行する。
		for(int i=0; i<NUM_OF_TRIALS; i++)
		{
			try {
				String raw = doGet(getEndpoint("/v3/streams/contents?streamId=" + streamId));
				JSONObject out = new JSONObject(raw);
				return out;
			} catch (JSONException e) {
				continue;
			} catch (IOException e) {
				continue;
			} catch (Exception e) {
				continue;
			}
		}
		throw new Exception("FeedlyAPI::getTheContentOfaStream()");
	}
	
	
	//
	//-- Subscriptions --------------------------------------------------------//
	//
	public JSONArray getUserSubscriptions() throws Exception
	{
		// Get the user’s subscriptions
		//
		// GET /v3/subscriptions
		
		String error = "\n->";
		
		// 例外を受け取った場合は、指定された回数再試行する。
		for(int i=0; i<NUM_OF_TRIALS; i++)
		{
			try {
				return new JSONArray(doGet(getEndpoint("/v3/subscriptions")));
			} catch (JSONException e) {
				error += "\n->" + e.toString();
				continue;
			} catch (IOException e) {
				error += "\n->" + e.toString();
				continue;
			} catch (Exception e) {
				error += "\n->" + e.toString();
				continue;
			}
		}
		throw new Exception("FeedlyAPI::getUserSubscriptions()" + error);
	}
	
	
	//
	//-- Tags -----------------------------------------------------------------//
	//
	/*public JSONArray getListOfTags() throws IOException, Exception
	{
		// Get the list of tags created by the user.
		//
		// GET /v3/tags
		
		// 例外を受け取った場合は、指定された回数再試行する。
		for(int i=0; i<NUM_OF_TRIALS; i++)
		{
			try {
				return new JSONArray(doGet(getEndpoint("/v3/tags")));
			} catch (IOException e) {
				continue;
			} catch (Exception e) {
				continue;
			}
		}
		throw new Exception("FeedlyAPI::getListOfUnreadCounts()");
	}*/
	
	
	public void tagEntry(String tagId, String feedId) throws Exception
	{
		// Tag an existing entry
		//
		// PUT /v3/tags/:tagId1,:tagId2
		
		// !!!!!!! tagIdは'/'もURLエンコードしなくてはならない !!!!!!!
		String url = getEndpoint("/v3/tags/") + encode(tagId);
		
		//
		JSONObject body = new JSONObject();
		try {
			body.put("entryId", feedId);
		} catch (JSONException e) {
			throw new Exception("FeedlyAPI::tagEntry()0");
		}
		
		// 例外を受け取った場合は、指定された回数再試行する。
		for(int i=0; i<NUM_OF_TRIALS; i++)
		{
			try {
				doPut(url, body);
				return;
			} catch (IOException e) {
				continue;
			} catch (Exception e) {
				continue;
			}
		}
		throw new Exception("FeedlyAPI::tagEntry()1");
	}
	
	
	/*public void tagMultipleEntries(String tagId, Vector entryIds) throws Exception
	{
		// 	Tag multiple entries
		//
		// PUT /v3/tags/:tagId1,:tagId2
		
		// Some info about some api - Google グループ
		// https://groups.google.com/forum/#!searchin/feedly-cloud/tag/feedly-cloud/Py15TGUVAyA/pIn7kWwKCRoJ
		
		if(entryIds.size() == 0)
		{
			throw new Exception("FeedlyAPI::tagMultipleEntries()0");
		}
		
		// !!!!!!! tagIdは'/'もURLエンコードしなくてはならない !!!!!!!
		String url = getEndpoint("/v3/tags/") + encode(tagId);
		
		//
		JSONObject json = new JSONObject();
		try {
			json.put("entryIds", entryIds);
		} catch (JSONException e) {
			throw new Exception("FeedlyAPI::tagMultipleEntries()1");
		}
		
		// 例外を受け取った場合は、指定された回数再試行する。
		for(int i=0; i<NUM_OF_TRIALS; i++)
		{
			try {
				doPut(url, json);
				return;
			} catch (IOException e) {
				continue;
			} catch (Exception e) {
				continue;
			}
		}
		throw new Exception("FeedlyAPI::tagMultipleEntries()2");
	}*/
	
	
	public void untagMultipleEntries(String tagId, Vector entryIds) throws Exception
	{
		// Untag multiple entries
		//
		// DELETE /v3/tags/:tagId1,tagId2/:entryId1,entryId2
		
		// Some info about some api - Google グループ
		// https://groups.google.com/forum/#!searchin/feedly-cloud/tag/feedly-cloud/Py15TGUVAyA/pIn7kWwKCRoJ
		
		if(entryIds.size() == 0)
		{
			throw new Exception("FeedlyAPI::untagMultipleEntries()0");
		}
		
		String url = getEndpoint("/v3/tags/");
		
		// tagID
		url += encode(tagId);
		
		// Separater
		url += "/";
		
		// entryIds
		for(Enumeration e = entryIds.elements(); e.hasMoreElements(); )
		{
			// URLエンコードしたentryIdを追加する。
			url += encode((String)e.nextElement());
			
			// Separater
			if(e.hasMoreElements())
			{
				url += ",";
			}
		}
		
		// 例外を受け取った場合は、指定された回数再試行する。
		for(int i=0; i<NUM_OF_TRIALS; i++)
		{
			try {
				doDelete(url);
				return;
			} catch (IOException e) {
				continue;
			} catch (Exception e) {
				continue;
			}
		}
		throw new Exception("FeedlyAPI::untagMultipleEntries()1");
	}
	
	
	//
	//-- Utilities ------------------------------------------------------------//
	//
	private String doDelete(String url) throws IOException, Exception
	{
		return _feedlyclient.doDelete(url);
	}
	
	
	private String doGet(String url) throws IOException, Exception
	{
		return _feedlyclient.doGet(url);
	}
	
	
	private String doPost(String url, JSONObject body) throws IOException, Exception
	{
		return _feedlyclient.doPost(url, body, true);
	}
	
	
	/*private String doPostJSONArray(String url, JSONArray body) throws IOException, Exception
	{
		return _feedlyclient.doPostJSONArray(url, body, true);
	}*/
	
	
	private String doPostNOCheck(String url, JSONObject body) throws IOException, Exception
	{
		return _feedlyclient.doPost(url, body, false);
	}
	
	
	private String doPut(String url, JSONObject body) throws IOException, Exception
	{
		return _feedlyclient.doPut(url, body, true);
	}
	
	
	private String getEndpoint(String path)
	{
		String url = "https://" + service_host;
		
		// URLエンコードする。
		path = urlEncode(path);
		
		if(path != "")
		{
			//url += "/" + path;
			url += path;
		}
		
		return url;
	}
	
	
	public String getTime(long time_ms)
	{
		long diffInSeconds = (System.currentTimeMillis() - time_ms) / 1000;

		long diff[] = new long[] { 0, 0, 0, 0 };
		// sec
		diff[3] = (diffInSeconds >= 60 ? diffInSeconds % 60 : diffInSeconds);
		// min
		diff[2] = (diffInSeconds = (diffInSeconds / 60)) >= 60 ? diffInSeconds % 60 : diffInSeconds;
		// hours
		diff[1] = (diffInSeconds = (diffInSeconds / 60)) >= 24 ? diffInSeconds % 24 : diffInSeconds;
		// days
		diff[0] = (diffInSeconds = (diffInSeconds / 24));
		
		if(diff[0] > 0) {
			if(diff[0] == 1) {
				return String.valueOf(diff[0]) + " day ago";
			} else {
				return String.valueOf(diff[0]) + " days ago";
			}
		} else if(diff[1] > 0) {
			if(diff[1] == 1) {
				return String.valueOf(diff[1]) + " hour ago";
			} else {
				return String.valueOf(diff[1]) + " hours ago";
			}
		} else if(diff[2] > 0) {
			if(diff[2] == 1) {
				return String.valueOf(diff[2]) + " minute ago";
			} else {
				return String.valueOf(diff[2]) + " minutes ago";
			}
		} else {
			return String.valueOf(diff[3]) + " seconds ago";
		}
		
		//e.g. 2008-06-03T12:15:03Z
		//DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		//DateFormat dateFormat = new SimpleDateFormat("HH' hours ago'");
		//return dateFormat.formatLocal(System.currentTimeMillis() - time_ms);
	}
	
	
	//
	// java.net.URLDecoder like? - BlackBerry Support Community Forums
	// http://supportforums.blackberry.com/t5/Java-Development/java-net-URLDecoder-like/td-p/435842
	// Thanks fwest!!
	//
	public static String urlEncode(String s)
	{
		StringBuffer sbuf = new StringBuffer();
		int len = s.length();
		for (int i = 0; i < len; i++)
		{
			int ch = s.charAt(i);
			if ('A' <= ch && ch <= 'Z') { // 'A'..'Z'
				sbuf.append((char)ch);
			} else if ('a' <= ch && ch <= 'z') { // 'a'..'z'
				sbuf.append((char)ch);
			} else if ('0' <= ch && ch <= '9') { // '0'..'9'
				sbuf.append((char)ch);
			} else if (ch == ' ') { // space
				sbuf.append('+');
			} else if (ch == '-' || ch == '_' // unreserved
					|| ch == '.' || ch == '!'
					|| ch == '~' || ch == '*'
					|| ch == '\\' || ch == '('
					|| ch == ')' || ch == '/' 
					|| ch == '&' || ch == '='
					|| ch == '?') {
				sbuf.append((char)ch);
			} else if (ch <= 0x007f) { // other ASCII
				sbuf.append(hex[ch]);
			} else if (ch <= 0x07FF) { // non-ASCII <= 0x7FF
				sbuf.append(hex[0xc0 | (ch >> 6)]);
				sbuf.append(hex[0x80 | (ch & 0x3F)]);
			} else { // 0x7FF < ch <= 0xFFFF
				sbuf.append(hex[0xe0 | (ch >> 12)]);
				sbuf.append(hex[0x80 | ((ch >> 6) & 0x3F)]);
				sbuf.append(hex[0x80 | (ch & 0x3F)]);
			}
		}
		return sbuf.toString();
	}
	 
	 
	// Hex constants.
	private final static String[] hex = {
		"%00", "%01", "%02", "%03", "%04", "%05", "%06", "%07",
		"%08", "%09", "%0a", "%0b", "%0c", "%0d", "%0e", "%0f",
		"%10", "%11", "%12", "%13", "%14", "%15", "%16", "%17",
		"%18", "%19", "%1a", "%1b", "%1c", "%1d", "%1e", "%1f",
		"%20", "%21", "%22", "%23", "%24", "%25", "%26", "%27",
		"%28", "%29", "%2a", "%2b", "%2c", "%2d", "%2e", "%2f",
		"%30", "%31", "%32", "%33", "%34", "%35", "%36", "%37",
		"%38", "%39", "%3a", "%3b", "%3c", "%3d", "%3e", "%3f",
		"%40", "%41", "%42", "%43", "%44", "%45", "%46", "%47",
		"%48", "%49", "%4a", "%4b", "%4c", "%4d", "%4e", "%4f",
		"%50", "%51", "%52", "%53", "%54", "%55", "%56", "%57",
		"%58", "%59", "%5a", "%5b", "%5c", "%5d", "%5e", "%5f",
		"%60", "%61", "%62", "%63", "%64", "%65", "%66", "%67",
		"%68", "%69", "%6a", "%6b", "%6c", "%6d", "%6e", "%6f",
		"%70", "%71", "%72", "%73", "%74", "%75", "%76", "%77",
		"%78", "%79", "%7a", "%7b", "%7c", "%7d", "%7e", "%7f",
		"%80", "%81", "%82", "%83", "%84", "%85", "%86", "%87",
		"%88", "%89", "%8a", "%8b", "%8c", "%8d", "%8e", "%8f",
		"%90", "%91", "%92", "%93", "%94", "%95", "%96", "%97",
		"%98", "%99", "%9a", "%9b", "%9c", "%9d", "%9e", "%9f",
		"%a0", "%a1", "%a2", "%a3", "%a4", "%a5", "%a6", "%a7",
		"%a8", "%a9", "%aa", "%ab", "%ac", "%ad", "%ae", "%af",
		"%b0", "%b1", "%b2", "%b3", "%b4", "%b5", "%b6", "%b7",
		"%b8", "%b9", "%ba", "%bb", "%bc", "%bd", "%be", "%bf",
		"%c0", "%c1", "%c2", "%c3", "%c4", "%c5", "%c6", "%c7",
		"%c8", "%c9", "%ca", "%cb", "%cc", "%cd", "%ce", "%cf",
		"%d0", "%d1", "%d2", "%d3", "%d4", "%d5", "%d6", "%d7",
		"%d8", "%d9", "%da", "%db", "%dc", "%dd", "%de", "%df",
		"%e0", "%e1", "%e2", "%e3", "%e4", "%e5", "%e6", "%e7",
		"%e8", "%e9", "%ea", "%eb", "%ec", "%ed", "%ee", "%ef",
		"%f0", "%f1", "%f2", "%f3", "%f4", "%f5", "%f6", "%f7",
		"%f8", "%f9", "%fa", "%fb", "%fc", "%fd", "%fe", "%ff"
	};
	
	 public static String encode(String s)
	  {
	    StringBuffer sbuf = new StringBuffer();
	    int len = s.length();
	    for (int i = 0; i < len; i++) {
	      int ch = s.charAt(i);
	      if ('A' <= ch && ch <= 'Z') {		// 'A'..'Z'
	        sbuf.append((char)ch);
	      } else if ('a' <= ch && ch <= 'z') {	// 'a'..'z'
		       sbuf.append((char)ch);
	      } else if ('0' <= ch && ch <= '9') {	// '0'..'9'
		       sbuf.append((char)ch);
	      } else if (ch == ' ') {			// space
		       sbuf.append('+');
	      } else if (ch == '-' || ch == '_'		// unreserved
	          || ch == '.' || ch == '!'
	          || ch == '~' || ch == '*'
	          || ch == '\'' || ch == '('
	          || ch == ')') {
	        sbuf.append((char)ch);
	      } else if (ch <= 0x007f) {		// other ASCII
		       sbuf.append(hex[ch]);
	      } else if (ch <= 0x07FF) {		// non-ASCII <= 0x7FF
		       sbuf.append(hex[0xc0 | (ch >> 6)]);
		       sbuf.append(hex[0x80 | (ch & 0x3F)]);
	      } else {					// 0x7FF < ch <= 0xFFFF
		       sbuf.append(hex[0xe0 | (ch >> 12)]);
		       sbuf.append(hex[0x80 | ((ch >> 6) & 0x3F)]);
		       sbuf.append(hex[0x80 | (ch & 0x3F)]);
	      }
	    }
	    return sbuf.toString();
	  }

}