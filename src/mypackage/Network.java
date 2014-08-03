/*
	Network.java
	
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
import java.io.OutputStream;

import javax.microedition.io.HttpsConnection;

import net.rim.device.api.io.IOUtilities;
import net.rim.device.api.io.transport.ConnectionDescriptor;
import net.rim.device.api.io.transport.ConnectionFactory;
import net.rim.device.api.io.transport.TransportInfo;
import net.rim.device.api.io.transport.options.BisBOptions;

public class Network
{
	private FeedlyClient _feedlyclient = null;
	
	
	public Network(FeedlyClient feedlyclient)
	{
		this._feedlyclient = feedlyclient;
	}
	
	
	public static boolean isCoverageSufficient()
	{
		int [] transportTypes = TransportInfo.getCoverageStatus();
		
		if (transportTypes.length != 0) {
			return true;
		} else {
			return false;
		}
	}
	
	
	public String delete(ConnectionFactory _connectionFactory, String url, String access_token) throws Exception
	{
		
		HttpsConnection conn = null;
		
		try {
			//updateStatus("[Delete] ê⁄ë±íÜ...");
			//updateStatus("[DELETE] " + url);
			
			
			if(_connectionFactory == null) { throw new IOException("[Get] _factory Error"); }
			
			//
			ConnectionDescriptor conDescriptor = _connectionFactory.getConnection(url);
			
			if(conDescriptor == null) {throw new Exception("conDescriptor Error"); }
			
			
			conn = (HttpsConnection) conDescriptor.getConnection();
			
			// Set the request method and headers
			conn.setRequestMethod("DELETE");
			
			if(access_token != "")
			{
				conn.setRequestProperty("Authorization", "OAuth " + access_token);
			}
			
			
			int rc = conn.getResponseCode();
			//updateStatus("Content-Type:" + auth1Con.getHeaderField("Content-Type"));
			//updateStatus("X-RateLimit-Count:" + auth1Con.getHeaderField("X-RateLimit-Count"));
			//updateStatus("X-RateLimit-Limit:" + auth1Con.getHeaderField("X-RateLimit-Limit"));
			//updateStatus("X-RateLimit-Remaining:" + auth1Con.getHeaderField("X-RateLimit-Remaining"));
			//updateStatus("return code:" + rc + " / " + conn.getResponseMessage());
			
			if(rc != 200)
			{
				//InputStream input = conn.openInputStream();
				//byte[] bytes = IOUtilities.streamToBytes(input);
				//String returnstring = new String(bytes, "UTF-8");
				//updateStatus(returnstring);
				
				throw new Exception("ReturnCode:" + rc + " " + conn.getResponseMessage());
			}
			
			return new String(IOUtilities.streamToBytes(conn.openInputStream()), "UTF-8").trim();
			
		} finally {
			if(conn != null){ conn.close(); }
		}
	}
	
	
	/*public Bitmap getWebBitmap(ConnectionFactory _factory, String url) throws Exception
	{
		updateStatus("[GetWebBitmap] " + url);
		
		InputStream is;
		byte[] imageData;
	
		if(_factory == null) {
			throw new IOException("[bitmap] _factory Error");
		}

	
		ConnectionDescriptor conDescriptor = _factory.getConnection(url);
		if(conDescriptor == null) {
			throw new IOException("[bitmap] conDescriptor Error");
		}

		HttpConnection httpconn = (HttpConnection) conDescriptor.getConnection();

		try {
			httpconn.setRequestMethod(HttpConnection.GET);

			int rc = httpconn.getResponseCode();
			if (rc != HttpConnection.HTTP_OK)
				throw new IOException("HTTP response code: " + rc);

			is = httpconn.openInputStream();
			if((imageData = IOUtilities.streamToBytes(is)) == null)
				throw new IOException("[bitmap] imageData Error");
	
			return Bitmap.createBitmapFromBytes(imageData, 0, -1, Bitmap.SCALE_TO_FIT);

		} finally {
			if(httpconn != null){ 
				try {
					httpconn.close();
				} catch (IOException e) {} 
			}
		}
	}*/ //GetWebBitmap
	
	public String get(ConnectionFactory _connectionFactory, String url, String access_token) throws Exception
	{
		
		HttpsConnection conn = null;
		
		try {
			//updateStatus("[Get] ê⁄ë±íÜ...");
			//updateStatus("[Get] " + url);
			
			
			if(_connectionFactory == null) { throw new IOException("[Get] _factory Error"); }
			
			//
			ConnectionDescriptor conDescriptor = _connectionFactory.getConnection(url);
			
			if (conDescriptor == null) { throw new Exception("conDescriptor Error"); }
			
			
			conn = (HttpsConnection) conDescriptor.getConnection();
			
			// Set the request method and headers
			conn.setRequestMethod(HttpsConnection.GET);
			
			if(access_token != "")
			{
				conn.setRequestProperty("Authorization", "OAuth " + access_token);
			}
			
			
			int rc = conn.getResponseCode();
			//updateStatus("Content-Type:" + auth1Con.getHeaderField("Content-Type"));
			//updateStatus("X-RateLimit-Count:" + auth1Con.getHeaderField("X-RateLimit-Count"));
			//updateStatus("X-RateLimit-Limit:" + auth1Con.getHeaderField("X-RateLimit-Limit"));
			//updateStatus("X-RateLimit-Remaining:" + auth1Con.getHeaderField("X-RateLimit-Remaining"));
			//updateStatus("return code:" + rc + " / " + conn.getResponseMessage());
			
			if(rc != 200)
			{
				//InputStream input = conn.openInputStream();
				//byte[] bytes = IOUtilities.streamToBytes(input);
				//String returnstring = new String(bytes, "UTF-8");
				//updateStatus(returnstring);
				
				throw new Exception("ReturnCode:" + rc + " " + conn.getResponseMessage());
			}
			
			return new String(IOUtilities.streamToBytes(conn.openInputStream()), "UTF-8").trim();
		} finally {
			if(conn != null){ conn.close(); }
		}
	}
	
	
	public String post(ConnectionFactory _connectionFactory, final String url, final byte[] body, String access_token) throws IOException, Exception
	{
		HttpsConnection conn = null;
		OutputStream out = null;
		
		try {
			//updateStatus("[POST] ê⁄ë±íÜ...");
			//updateStatus("[POST] " + url);
			
			
			if(_connectionFactory == null) { throw new IOException("[POST] _factory Error"); }
			
			//
			ConnectionDescriptor conDescriptor = _connectionFactory.getConnection(url);
			
			if (conDescriptor == null) { throw new Exception("conDescriptor Error"); }
			
			
			conn = (HttpsConnection) conDescriptor.getConnection();
			
			// Set the request method and headers
			conn.setRequestMethod(HttpsConnection.POST);
			
			//updateStatus(body.toString());
			
			conn.setRequestProperty("Content-Type", "application/json");
			if(access_token != "")
			{
				conn.setRequestProperty("Authorization", "OAuth " + access_token);
			}
			
			out = conn.openOutputStream();
			//out.write(body.toString().getBytes());
			out.write(body);
			out.flush();
			
			int rc = conn.getResponseCode();
			//_app.updateStatus("return code:" + rc + " / " + conn.getResponseMessage());
			
			if(rc != 200)
			{
				throw new Exception("ReturnCode:" + rc + " " + conn.getResponseMessage());
			}
			
			
			//InputStream input = conn.openInputStream();
			//byte[] bytes = IOUtilities.streamToBytes(input);
			//String returnstring = new String(bytes, "UTF-8");
			//_app.updateStatus(returnstring);
			
			return new String(IOUtilities.streamToBytes(conn.openInputStream()), "UTF-8");
			
		} finally {
			if(conn != null){ conn.close(); }
		}
	} //post()
	
	
	public String put(ConnectionFactory _connectionFactory, final String url, final byte[] body, String access_token) throws IOException, Exception
	{
		HttpsConnection conn = null;
		OutputStream out = null;
		
		try {
			//updateStatus("[PUT] ê⁄ë±íÜ...");
			//updateStatus("[PUT] " + url);
			
			if(_connectionFactory == null) { throw new IOException("[PUT] _factory Error"); }
			
			//
			ConnectionDescriptor conDescriptor = _connectionFactory.getConnection(url);
			
			if (conDescriptor == null) { throw new Exception("conDescriptor Error"); }
			
			
			conn = (HttpsConnection) conDescriptor.getConnection();
			
			// Set the request method and headers
			conn.setRequestMethod("PUT");
			
			conn.setRequestProperty("Content-Type", "application/json");
			if(access_token != "")
			{
				conn.setRequestProperty("Authorization", "OAuth " + access_token);
			}
			
			out = conn.openOutputStream();
			out.write(body);
			out.flush();
			
			int rc = conn.getResponseCode();
			//updateStatus("return code:" + rc + " / " + conn.getResponseMessage());
			
			if(rc != 200)
			{
				//InputStream input = conn.openInputStream();
				//byte[] bytes = IOUtilities.streamToBytes(input);
				//String returnstring = new String(bytes, "UTF-8");
				//updateStatus(returnstring);
				throw new Exception("ReturnCode:" + rc + " " + conn.getResponseMessage());
			}
			
			return new String(IOUtilities.streamToBytes(conn.openInputStream()), "UTF-8");
			
		} finally {
			if(conn != null){ conn.close(); }
		}
	} //post()
	
	
	public ConnectionFactory selectTransport()
	{
		ConnectionFactory out = new ConnectionFactory();
		
		//
		out.setConnectionTimeout(15000L);
		
		//
		int[] preferredTransports = new int[]{
				TransportInfo.TRANSPORT_TCP_WIFI,
				TransportInfo.TRANSPORT_BIS_B,
				TransportInfo.TRANSPORT_TCP_CELLULAR
		};
		out.setPreferredTransportTypes(preferredTransports);
		
		//
		int[] disallowedTransportTypes = new int[] {
				//TransportInfo.TRANSPORT_TCP_WIFI,
				//TransportInfo.TRANSPORT_BIS_B,
				//TransportInfo.TRANSPORT_TCP_CELLULAR,
				TransportInfo.TRANSPORT_WAP,
				TransportInfo.TRANSPORT_WAP2,
				TransportInfo.TRANSPORT_MDS
		};
		out.setDisallowedTransportTypes(disallowedTransportTypes);
		
		// BIS-B
		BisBOptions _bisBOptions = new BisBOptions(_feedlyclient.getNetworkSecret());
		out.setTransportTypeOptions(TransportInfo.TRANSPORT_BIS_B, _bisBOptions);
		
		// DirectTCP APN
		//TcpCellularOptions tcpOptions = new TcpCellularOptions();
		//tcpOptions.setApn("mpr.ex-pkt.net");
		//tcpOptions.setTunnelAuthUsername("");
		//tcpOptions.setTunnelAuthPassword("");
		//out.setTransportTypeOptions(TransportInfo.TRANSPORT_TCP_CELLULAR, tcpOptions);
		
		return out;
	} //selectTransport()
	
	
	/*public void updateStatus(final String message)
	{
		_feedlyclient.updateStatus("Network::" + message);
	}*/ //updateStatus()
}