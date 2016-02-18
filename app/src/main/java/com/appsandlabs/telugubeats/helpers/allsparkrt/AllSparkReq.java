package com.appsandlabs.telugubeats.helpers.allsparkrt;


import com.appsandlabs.telugubeats.datalisteners.GenericListener;
import com.appsandlabs.telugubeats.datalisteners.GenericListener2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.PushbackInputStream;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

public class AllSparkReq{
	private static final String INIT_EVENTS_READER_THREAD = "events_reader";
	private static final String PINGER_THREAD = "pinger_thread";
	private static Thread eventsReaderThread;
	private String statusResponse;

	Socket socket;
		InputStream inputStream;
		OutputStream outputStream;
	private Thread pingingThread;
	public boolean isManuallyClosed;
	private boolean isChunked;


	private AllSparkReq(Socket socket){
			this.socket = socket;
			try {
				inputStream = new PushbackInputStream(socket.getInputStream());
				outputStream = socket.getOutputStream();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public OutputStream getOutputStream() {
			return outputStream;
		}

		public InputStream getInputStream() {
			return inputStream;
		}

	public static void initListenMessagesInThread(final String url, final GenericListener<AllSparkReq> onInit, final GenericListener<String> onEvent,  final GenericListener2<Exception, Boolean> onException){
		 eventsReaderThread = new Thread(INIT_EVENTS_READER_THREAD) {
			@Override
			public void run() {
				final AllSparkReq req;
				try {
					req = AllSparkReq.initRequest(url);
					req.keepPinging(onException);
				} catch (URISyntaxException | IOException e) {
					e.printStackTrace();
					onException.onData(e, false);
					return;
				}
				onInit.onData(req);

				try {
					req.startReadingEvents(onEvent);
				} catch (IOException e) {
					e.printStackTrace();
					onException.onData(e, req.isManuallyClosed);
				}
			}
		};
		eventsReaderThread.start();
	}

	/*
	sends headers and initaitates the socket
	 */
	public static AllSparkReq initRequest(String url) throws URISyntaxException, IOException {
			URI uri = new URI(url);
			String host = uri.getHost();
			String path = uri.getRawPath();
			if (path == null || path.length() == 0) {
				path = "/";
			}

			String query = uri.getRawQuery();
			if (query != null && query.length() > 0) {
				path += "?" + query;
			}

			String protocol = uri.getScheme();
			int port = uri.getPort();
			if (port == -1) {
				if (protocol.equals("http")) {
					port = 80; // http port
				} else if (protocol.equals("https")) {
					port = 443; // https port
				} else {

				}
			}

			AllSparkReq socket = new AllSparkReq(new Socket(host, port));

			PrintWriter request = new PrintWriter(socket.getOutputStream());
			request.print("POST " + path + " HTTP/1.1\r\n" +
					"Host: " + host + "\r\n" +
					"Connection: keep-alive\r\n"+
					"Transfer-Encoding: identity\r\n\r\n"
			);
			request.flush();



			int numBytes = 0;
			byte[] buf = new byte[128];
			HashMap<String, String> responseHeaders = new HashMap<>();

			StringBuilder response = new StringBuilder();
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));


			String line;
			socket.statusResponse = bufferedReader.readLine();
			while (!(line = bufferedReader.readLine()).isEmpty()) {
				String[] headerValues = line.split(":");
			    responseHeaders.put(headerValues[0].toLowerCase().trim(), headerValues[1].toLowerCase().trim());
			}

			if(responseHeaders.get("transfer-encoding")!=null){
				socket.isChunked = responseHeaders.get("transfer-encoding").equalsIgnoreCase("chunked");
				socket.inputStream = new ChunkedInputStream(socket.inputStream);
			}

			return socket;
	}

	public void startReadingEvents(GenericListener<String> message) throws IOException {
		InputStream inpStream = getInputStream();
		StringBuilder s = new StringBuilder(1024);
		byte[] byteBuffer = new byte[1024];
		int bytesRead = -1;
		char[] delimiter = new char[4];

		while ((bytesRead = inpStream.read(byteBuffer)) > 0) { //read forever until the socket is closed or eof reached
			for (int i = 0; i < bytesRead; i++) {
				s.append((char) byteBuffer[i]);
				int l = s.length();
				s.getChars(l > 3 ? (l - 4) : 0, l, delimiter, 0);
				if (delimiter[0] == '\r' && delimiter[1] == '\n' && delimiter[2] == '\r' && delimiter[3] == '\n') {
					message.onData(s.toString());
					delimiter[0] = delimiter[1] = delimiter[2] = delimiter[3] = '\0';
					s.setLength(0);
				}
			}
		}
	}


	public Socket getSocket() {
		return socket;
	}


	private void keepPinging(final GenericListener2<Exception, Boolean> exceptionHandler) {
		final byte[] pingString = "{}\r\n\r\n".getBytes();
		(pingingThread = new Thread(PINGER_THREAD){
			@Override
			public void run() {
				while(true) {
					try {
						int bytesSent = 0;
						getOutputStream().write(pingString);
						getOutputStream().flush();
						sleep(60000);
					} catch (InterruptedException | IOException ex) {
						gracefullyCloseConnection();
						isManuallyClosed = false;
						exceptionHandler.onData(ex , isManuallyClosed);
						break;
					}
				}
			}
		}).start();
	}
	public void gracefullyCloseConnection() {
		isManuallyClosed = true;
			try {
				getSocket().close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
}
