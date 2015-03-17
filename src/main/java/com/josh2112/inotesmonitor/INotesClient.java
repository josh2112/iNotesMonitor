package com.josh2112.inotesmonitor;

import java.io.File;
import java.net.URI;
import java.security.InvalidAlgorithmParameterException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import javax.net.ssl.SSLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.josh2112.inotesmonitor.CredentialStorage.Credentials;
import com.josh2112.inotesmonitor.database.tables.records.NotesMessageRecord;
import com.josh2112.inotesmonitor.inotesdata.INotesParser;
import com.josh2112.inotesmonitor.inotesdata.NotesMessage;

public class INotesClient {
	
	private Log log = LogFactory.getLog( INotesClient.class );
	
	@SuppressWarnings( "serial" )
	public static class LotusINotesUnknownCommandException extends Exception {
		
		private String errorHtml;
		
		public LotusINotesUnknownCommandException( String errorHtml ) {
			super( "Lotus iNotes returned 'Unknown Command Exception'" );
			this.errorHtml = errorHtml;
		}
		
		public String getErrorHtml() { return errorHtml; }
	}
	
	public static class MessageCheckResult {
		public List<MessageCheckPageResult> pages = new ArrayList<>();
	}
	
	public static class MessageCheckPageResult {
		public String pageNonce;
		public List<NotesMessageRecord> messages = new ArrayList<>();
		
		public MessageCheckPageResult( List<NotesMessageRecord> messages, String pageNonce ) {
			this.messages = messages;
			this.pageNonce = pageNonce;
		}
		
		public LocalDateTime getNewestMessageDate() {
			if( messages.isEmpty() ) return null;
			else return messages.get( 0 ).getDate();
		}
		
		public LocalDateTime getOldestMessageDate() {
			if( messages.isEmpty() ) return null;
			else return messages.get( messages.size() - 1 ).getDate();
		}
	}
	
	public enum AuthenticationResult {
		SUCCESS, FAILED, NEED_CERTIFICATE
	}
	
	private StringProperty username = new SimpleStringProperty();
	public ReadOnlyStringProperty usernameProperty() { return username; }
	public String getUsername() { return username.get(); }
	
	private String host, nsfFilename;
	private CloseableHttpClient httpClient;
	private AuthenticationResult lastAuthenticationResult = null;
	
	private RequestConfig clientConfig = RequestConfig.custom().setCookieSpec( CookieSpecs.BEST_MATCH )
			.setSocketTimeout( 5000 ).setConnectTimeout( 5000 ).setConnectionRequestTimeout( 5000 ).build();
	
	private DoubleProperty messageCheckProgress = new SimpleDoubleProperty();
	public ReadOnlyDoubleProperty messageCheckProgressProperty() { return messageCheckProgress; }
	public double getMessageCheckProgress() { return messageCheckProgress.get(); }
	
	public INotesClient( String host ) {
		this.host = host;
	}

	public AuthenticationResult getLastAuthenticationResult() {
		return lastAuthenticationResult;
	}
	
	public String getHost() {
		return host;
	}
	
	/**
	 * Attempts to log in and returns the result.
	 * @param creds username and password
	 * @param keyStoreFile SSL key store file
	 * @return authentication result
	 * @throws Exception
	 */
	public AuthenticationResult authenticate( Credentials creds, File keyStoreFile  ) throws Exception {
		System.setProperty( "javax.net.ssl.trustStore", keyStoreFile.getAbsolutePath() );
		httpClient = HttpClients.custom().setDefaultRequestConfig( clientConfig ).build();
		
		username.set( String.valueOf( creds.username ) );
		nsfFilename = username.get().toUpperCase() + ".nsf";
		
		List<NameValuePair> formData = new ArrayList<>();
		formData.add( new BasicNameValuePair( "%%ModDate", "0000000000000000" ) );
		formData.add( new BasicNameValuePair( "Username", String.valueOf( creds.username ) ) );
		formData.add( new BasicNameValuePair( "Password", String.valueOf( creds.password ) ) );
		formData.add( new BasicNameValuePair( "RedirectTo", "/webacces.nsf" ) );
		
		HttpPost request = new HttpPost( String.format( "https://%s/names.nsf?Login", host ) );		
		request.setEntity( new UrlEncodedFormEntity( formData, "UTF-8" ));
		
		try {
			CloseableHttpResponse response = httpClient.execute( request );
			
			String responseBody = EntityUtils.toString( response.getEntity() );
			EntityUtils.consumeQuietly( response.getEntity() );
			
			if( response.getStatusLine().getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY ) lastAuthenticationResult = AuthenticationResult.SUCCESS;
			else if( responseBody.contains( "You provided an invalid username or password" ) ||
					 responseBody.contains( "Please identify yourself" ) ) lastAuthenticationResult = AuthenticationResult.FAILED;
			return lastAuthenticationResult;
		}
		catch( SSLException e ) {
			System.err.println( e );
			Throwable e1 = e.getCause();
			if( e1 != null ) {
				Throwable e2 = e1.getCause();
				if( e2 != null && e2 instanceof InvalidAlgorithmParameterException ) {
					return lastAuthenticationResult = AuthenticationResult.NEED_CERTIFICATE;
				}
			}
			// Didn't handle the exception? Rethrow it.
			throw e;
		}
	}
	
	/**
	 * Retrieves messages in descending order of date, a page at a time, until we hit a page
	 * containing messages older than the given date.
	 * @param startDate date of earliest message to retrieve.
	 * @return List of MessageCheckPageResults, each containing a list of messages and the page nonce.
	 * @throws Exception
	 */
	public MessageCheckResult getMessagesSince( LocalDateTime startDate ) throws Exception {
		int startMessageNum = 1, messageCount = 40;
		MessageCheckResult result = new MessageCheckResult();
		MessageCheckPageResult pageResult;
		
		messageCheckProgress.set( 0 );
		long totalMinutesIntoPast = startDate.until( LocalDateTime.now(), ChronoUnit.MINUTES );
		
		do {
			log.debug( String.format( "Retreiving %d messages starting at %d...", messageCount, startMessageNum ) );
			pageResult = getMessages( startMessageNum, messageCount );
			
			if( pageResult.messages.isEmpty() ) {
				log.debug( " - Got empty message list page, assuming we've read all messages." );
				break;
			}
			
			result.pages.add( pageResult );
			log.debug( String.format( " - %d messages, oldest on page dated %s",
					pageResult.messages.size(), pageResult.getOldestMessageDate() ) );
			startMessageNum += messageCount;
			
			// How far back into the past (in minutes) have we reached?
			long currentMinutesIntoPast = pageResult.getOldestMessageDate().until( LocalDateTime.now(), ChronoUnit.MINUTES );
			messageCheckProgress.set( Math.max( 0.0, Math.min( 1.0, currentMinutesIntoPast / totalMinutesIntoPast ) ) );
		}
		while( pageResult.getOldestMessageDate().isAfter( startDate ) );
		
		return result;
	}
	
	/**
	 * Retrieves a page, or a list of messages defined by start message number and message count.
	 * @param startMessageNum number of the message to start with. Messages are numbered
	 * in descending order of date.
	 * @param messageCount number of messages to retrieve.
	 * @return list of messages and page nonce.
	 * @throws Exception
	 */
	MessageCheckPageResult getMessages( int startMessageNum, int messageCount ) throws Exception {
		URI uri = new URI( "https", host, String.format( "/mail/%s/iNotes/Mail/", nsfFilename ),
				String.format( "OpenDocument&Form=m_MailView&start=%d&count=%d", startMessageNum, messageCount ), null );
		String htmlSrc = getUrlContent( uri );
		//try( PrintWriter out = new PrintWriter( "test/inboxsrc.html" ) ) { out.println( htmlSrc ); }
		
		return INotesParser.parseMessages( htmlSrc );
	}
	
	/**
	 * Retrieves the content of the message specified by the given GUID, parses it and fills out
	 * the message fields.
	 * @param record message to retrieve content for.
	 * @return NotesMessage
	 * @throws Exception
	 */
	public NotesMessage getMessageContent( NotesMessageRecord record ) throws Exception {
		log.debug( "getMessageContent( " +  record.getGUID() + " )" );
		
		//String htmlSrc = getUrlContent( String.format( "https://%s/mail/%s/iNotes/%s?OpenDocument&ui=dwa_ulite",
		//		host, nsfFilename, record.getGUID() ) );
				
		URI uri = new URI( "https", host, String.format( "/mail/%s/iNotes/%s", nsfFilename, record.getGUID() ),
				"OpenDocument&ui=dwa_ulite", null );
		String htmlSrc = getUrlContent( uri );
		
		HashMap<String, String> fields = INotesParser.getJavascriptFields( htmlSrc );
		
		NotesMessage message = NotesMessage.fromHtml( record, fields );
		
		String externalBodyLink = null;
		
		// Instead of getting the message body, we might get a reference to
		// another HTML document on the server.  This could either be an
		// a href link in a table or an iframe.  Find the link and download it.
		if( message.getBody().contains( "Additional HTML Attached" ) ) {
			Document bodyDoc = Jsoup.parse( message.getBody() );
			externalBodyLink = bodyDoc.select( "a" ).first().attr( "href" );
		}
		else if( message.getBody().contains( "MIMEmailbodyiframe" )) {
			Document bodyDoc = Jsoup.parse( message.getBody() );
			externalBodyLink = bodyDoc.select( "iframe" ).first().attr( "src" );
			// Weird, this iFrame link says it's relative to XXX.nsf but in reality it's relative to XXX.nsf/iNotes
			if( externalBodyLink.startsWith( "../" ) ) externalBodyLink = externalBodyLink.substring( 3 );
		}	
		
		// TODO: Try this JSoup.connect( "" ).get() stuff
		if( externalBodyLink != null ) {
			URI bodyUri = uri.resolve( externalBodyLink );
			message.setBody( getUrlContent( bodyUri ) );
		}
		
		// The act of downloading the message body marks it as read. We don't want to interfere with the regular
		// mail system, so re-mark any unread messages as unread.
		if( !message.isRead() ) setMessageIsRead( message, false );
		
		return message;
	}
	
	/** 
	 * Marks a message as read on the server.
	 * @param msg
	 * @param read
	 * @throws Exception
	 */
	public void setMessageIsRead( NotesMessage msg, boolean read ) throws Exception {
		log.debug( "setMessageIsRead( " + msg.getGuid() + ", " + read + " ) " );
		
		List<NameValuePair> formData = new ArrayList<>();
		formData.add( new BasicNameValuePair( "h_SetCommand", read ? "h_ShimmerMarkRead" : "h_ShimmerMarkUnread" ) );
		formData.add( new BasicNameValuePair( "%%Nonce", msg.getMessageNonce() ) );
		formData.add( new BasicNameValuePair( "h_SetDeleteList", msg.getGuid() ) );
		formData.add( new BasicNameValuePair( "h_EditAction", "h_Next" ) );
		
		String url = String.format( "https://%s/mail/%s/iNotes/Proxy/?EditDocument&ui=dwa_ulite", host, nsfFilename );
		HttpPost request = new HttpPost( url );		
		request.setEntity( new UrlEncodedFormEntity( formData, "ISO-8859-1" ));

		CloseableHttpResponse response = httpClient.execute( request );
		String result = EntityUtils.toString( response.getEntity() );
		response.close();
		if( isErrorResponse( result ) ) throw new LotusINotesUnknownCommandException( result );
	}
	
	public void deleteMessage( NotesMessage msg ) throws Exception {
		log.debug( "deleteMessage( " + msg.getGuid() + " ) " );
		
		List<NameValuePair> formData = new ArrayList<>();
		formData.add( new BasicNameValuePair( "h_SetCommand", "h_DeletePages") );
		formData.add( new BasicNameValuePair( "%%Nonce", "DA7255645233CB7B5182C7B32AB6808F" ) );//msg.getMessageNonce() ) );
		formData.add( new BasicNameValuePair( "h_SetDeleteList", msg.getGuid() ) );
		formData.add( new BasicNameValuePair( "h_EditAction", "h_Next" ) );
		formData.add( new BasicNameValuePair( "h_SetReturnUrl", String.format( "https://%s/mail/%s/iNotes", host, nsfFilename ) ) );
		
		String url = String.format( "https://%s/mail/%s/iNotes/Proxy/?EditDocument&ui=dwa_ulite", host, nsfFilename );
		HttpPost request = new HttpPost( url );		
		request.setEntity( new UrlEncodedFormEntity( formData, "ISO-8859-1" ));
		
		CloseableHttpResponse response = httpClient.execute( request );
		String result = EntityUtils.toString( response.getEntity() );
		response.close();
		if( isErrorResponse( result ) ) throw new LotusINotesUnknownCommandException( result );
	}

	private boolean isErrorResponse( String result ) {
		return result.contains( "IBM Lotus iNotes Error Report" );
	}
	
	private String getUrlContent( URI uri ) throws Exception {
		log.debug( "getUrlContent( " + uri + " )" );
		CloseableHttpResponse response = httpClient.execute( new HttpGet( uri ) );
		String body = EntityUtils.toString( response.getEntity() );
		response.close();
		return body;
	}

	public void openMessageInBrowser( NotesMessage msg ) {
		INotesMonitorMain.getHostService().showDocument( String.format( "https://%s/mail/%s/($Inbox)/%s/?OpenDocument",
				host, nsfFilename, msg.getGuid() ) );
	}

	public void acceptMeeting( NotesMessage msg ) throws Exception {
		log.debug( "acceptMeeting( " + msg.getGuid() + " ) " );
		
		/* Test data from Chrome
		 * ---------------------
		 * 
		 * Request URL: https://webmail.cem.com/mail/JF334.nsf/38D46BF5E8F08834852564B500129B2C/74B675FBA9F7D15685257C9E007278FE/?EditDocument&Form=h_PageUI&ui=classic
		 *   74B* is the GUID
		 *   38D* is ($Inbox)?
		 * Form data:
		    + h_SetCommand:h_ShimmerSave
			+ %%Nonce:AA8273388D322B1C3477C79055ACA600
			+ h_EditAction:h_Next
			%%ModDate:85257c9e00730947
			%%PostCharset:ISO-8859-1
			h_SceneContext:putAway['publishAction']&&&&&&putAway['publishFolderTitle']&&&&&&putAway['ME']&&&&&&putAway['publishFolderPageUnid']&&&&&&putAway['tocPosition']&&&&&&putAway['tmpText']&&&&&&putAway['selectedFolderIndex']&&&0&&&putAway['BSi']&&&&&&
			h_MeetingCommand:8
			h_SetEditCurrentScene:s_StdPageRead
			h_SetReturnURL:[[./&Form=s_CallRedirectCurrentWinHtml&PresetFields=s_ScanViewName;38D46BF5E8F08834852564B500129B2C,s_ScanUnid;74B675FBA9F7D15685257C9E007278FE,s_ScanFromClient;1,s_ViewName;38D46BF5E8F08834852564B500129B2C,s_Unid;74B675FBA9F7D15685257C9E007278FE,s_InstDate;20140318T130000Z]]
			h_SetSaveDoc:1
			s_ScanFromClient:1
			s_ScanViewName:38D46BF5E8F08834852564B500129B2C
			s_ScanUnid:74B675FBA9F7D15685257C9E007278FE
			SMessage:Accepted:
			tmpLocalizedKeywords:0x15620001|Invitation:|0x15620002|Rescheduled:|0x15620003|Confirmed:|0x15620004|Cancelled:|0x15620005|Broadcast:|0x15620006|To Do:|0x15620007|Update:|0x08ae0001|Accepted:|0x08ae0002|Declined:|0x08ae0003|Countered:|0x08ae0004|Delegated:|0x08ae0005|Completed:|0x08af0001|Request update:|0x08af0002|Tentative:|0x08af0003|Error processing reservation:|0x08af0004|Invitation returned:|0x08af0005|WARNING! Capacity exceeded:|0x08af0006|Resource disabled:|0x08af0007|Insufficient access:|0x08b00001|Information Update - Subject has changed:|0x08b00002|Information Update - Location has changed:|0x08b10001|Information Update - Description has changed:|0x08b10002|Information Update - Room has changed:|0x08b20001|Information Update - Resources have changed:|0x08b20002|Information Update - Invitees have changed:|0x08b30001|Information Update - There are multiple changes:|0x08b30002|Invitation (delegated):|0x08b30003|To do (delegated):|0x08bc0001|Declined:|0x08bc0002|Countered:|0x08bc0003|Delegated:
			s_ActionInProgress:A
			StartTimeZone:Z=5$DO=1$DL=3 2 1 11 1 1$ZN=Eastern
			EndTimeZone:Z=5$DO=1$DL=3 2 1 11 1 1$ZN=Eastern
			LocalTimeZone:Z=5$DO=1$DL=3 2 1 11 1 1$ZN=Eastern
			ThisInstDate:20140318T090000$Z=5$DO=1$DL=3 2 1 11 1 1$ZN=Eastern
		 *
		 * + indicates already included below
		 * How much of the rest is actually needed?
		 */
		
		
		List<NameValuePair> formData = new ArrayList<>();
		formData.add( new BasicNameValuePair( "h_SetCommand", "h_ShimmerSave" ) );
		formData.add( new BasicNameValuePair( "%%Nonce", msg.getMessageNonce() ) );
		formData.add( new BasicNameValuePair( "h_EditAction", "h_Next" ) );
		
		String url = String.format( "https://%s/mail/%s/iNotes/Proxy/?EditDocument&ui=dwa_ulite", host, nsfFilename );
		HttpPost request = new HttpPost( url );		
		request.setEntity( new UrlEncodedFormEntity( formData, "ISO-8859-1" ));

		CloseableHttpResponse response = httpClient.execute( request );
		String result = EntityUtils.toString( response.getEntity() );
		response.close();
		if( isErrorResponse( result ) ) throw new LotusINotesUnknownCommandException( result );
	}
}