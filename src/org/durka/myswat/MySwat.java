package org.durka.myswat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.LinkedHashMap;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import nu.xom.ParsingException;
import nu.xom.ValidityException;
import nu.xom.XPathContext;

import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import android.util.Log;

public class MySwat implements Serializable
{
	private DefaultHttpClient client;
	private Boolean logged_in;
	private String username, password;
	public final String BASE_URI = "https://myswat.swarthmore.edu",
	 					LOGIN_PROMPT = "/pls/twbkwbis.P_WWWLogin",
	 					LOGIN = "/pls/twbkwbis.P_ValLogin",
	 					LOGOUT = "/pls/twbkwbis.P_Logout",
	 					MAIN_MENU = "/pls/twbkwbis.P_GenMenu?name=bmenu.P_MainMnu&msg=WELCOME+";
	
	private class Carbonite implements Serializable
	{
		private String username, password, sessid;
		
		public Carbonite(String u, String p, DefaultHttpClient client)
		{
			username = u;
			password = p;
			
			sessid = null;
			for (Cookie cookie : client.getCookieStore().getCookies())
			{
				Log.d("myswat", "I HAZ A COOKIE " + cookie.getName() + "=" + cookie.getValue());
				if (cookie.getName().equals("SESSID"))
				{
					sessid = cookie.getValue();
					Log.d("myswat", "freezing session ID " + sessid);
				}
			}
		}
		
		public String getUsername()
		{
			return username;
		}
		
		public String getPassword()
		{
			return password;
		}
		
		public Cookie getSessionCookie()
		{
			if (sessid == null)
			{
				return null;
			}
			
			return new BasicClientCookie("SESSID", sessid);
		}
	}
	
	private void writeObject(ObjectOutputStream out) throws IOException
	{
		out.writeObject(new Carbonite(username, password, client));
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		Carbonite frozen = (Carbonite) in.readObject();
		
		username = frozen.getUsername();
		password = frozen.getPassword();
		
		Cookie cookie = frozen.getSessionCookie();
		client = getNewHttpClient();
		if (cookie == null)
		{
			logged_in = false;
		}
		else
		{
			logged_in = true;
			client.getCookieStore().addCookie(cookie);
			Log.d("myswat", "thawing sesssion ID " + cookie.getValue());
		}
	}
	
	private class TrustingSSLSocketFactory extends SSLSocketFactory
	{
	    SSLContext sslContext = SSLContext.getInstance("TLS");

	    public TrustingSSLSocketFactory(KeyStore truststore) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException
	    {
	        super(truststore);

	        TrustManager tm = new X509TrustManager() {
	            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
	            }

	            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
	            }

	            public X509Certificate[] getAcceptedIssuers() {
	                return null;
	            }
	        };

	        sslContext.init(null, new TrustManager[] { tm }, null);
	    }

	    @Override
	    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
	        return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
	    }

	    @Override
	    public Socket createSocket() throws IOException {
	        return sslContext.getSocketFactory().createSocket();
	    }
	}
	
	public MySwat(String u, String p)
	{
		logged_in = false;
		username = u;
		password = p;
		
		if (client == null)
		{
			client = getNewHttpClient();
		}
		
		Log.d("myswat", "MySwat constructor");
	}
	
	private DefaultHttpClient getNewHttpClient() {
	    try {
	        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
	        trustStore.load(null, null);

	        SSLSocketFactory sf = new TrustingSSLSocketFactory(trustStore);
	        sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

	        HttpParams params = new BasicHttpParams();
	        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
	        HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

	        SchemeRegistry registry = new SchemeRegistry();
	        registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
	        registry.register(new Scheme("https", sf, 443));

	        ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

	        return new DefaultHttpClient(ccm, params);
	    } catch (Exception e) {
	        return new DefaultHttpClient();
	    }
	}
	
	private String do_http(String method, String uri)
	{
		return do_http(method, uri, null);
	}
	
	private String do_http(String method, String uri, String referer)
	{
		if (!logged_in)
		{
			login(username, password);
		}
		
		try
		{
			HttpUriRequest request = null;
			if (method == "GET")
			{
				request = new HttpGet(uri);
			}
			else if (method == "POST")
			{
				request = new HttpPost(uri);
			}
			
			if (referer != null)
			{
				request.addHeader("Referer", referer);
			}
			
			BufferedReader in = new BufferedReader(new InputStreamReader(client.execute(request).getEntity().getContent()));
			StringBuffer sb = new StringBuffer("");
			String line = "", NL = System.getProperty("line.separator");
            while ((line = in.readLine()) != null)
            {
                sb.append(line + NL);
            }
            in.close();
            
            return sb.toString();
		}
		catch (IOException e)
		{
			return "E: " + e.getMessage();
		}
	}
	
	public String getSessionID()
	{
		for (Cookie cookie : client.getCookieStore().getCookies())
		{
			if (cookie.getName().equals("SESSID"))
			{
				return cookie.getValue();
			}
		}
		return null;
	}
	
	public Boolean login(String username, String password)
	{
		// prevent stack overflow
		logged_in = true;
		
		// get cookies first
		do_http("GET", BASE_URI + LOGIN_PROMPT);
		
		// log in
		do_http("POST", BASE_URI + LOGIN + "?sid=" + username + "&PIN=" + password);
		
		return true;
	}
	
	public String get_page(String suburi)
	{
		return do_http("GET", BASE_URI + suburi);
	}
	
	public Boolean logout()
	{
		do_http("GET", BASE_URI + LOGOUT, BASE_URI + MAIN_MENU);
		
		return true;
	}
	
	public Nodes xpath(String html, String query)
	{
		try
        {
			XMLReader tagsoup = XMLReaderFactory.createXMLReader("org.ccil.cowan.tagsoup.Parser");
			tagsoup.setFeature("http://xml.org/sax/features/namespace-prefixes",true);
			Builder bob = new Builder(tagsoup);
			Document doc = bob.build(html, null);
			
			XPathContext context = new XPathContext("html", "http://www.w3.org/1999/xhtml");
			Nodes nodes = doc.query(query, context);
			
			return nodes;
        }
        catch (SAXException e)
        {
        	// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ValidityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParsingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	public LinkedHashMap<String, String> parse_menu(String html)
	{
		LinkedHashMap<String, String> entries = new LinkedHashMap<String, String>();
		
		Nodes table = xpath(html, "//html:table[@class='menuplaintable']//html:td/html:a");
		
		for (int i = 0; i < table.size(); ++i)
		{
			entries.put(table.get(i).getChild(0).toXML(), ((Element) table.get(i)).getAttribute("href").getValue());
		}
		
		return entries;
	}

}
