package gu.dtalk.engine;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static gu.dtalk.CommonConstant.DEFAULT_IDLE_TIME_MILLS;
import static gu.dtalk.engine.DeviceUtils.DEVINFO_PROVIDER;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import fi.iki.elonen.NanoWSD.WebSocketFrame.CloseCode;
import fi.iki.elonen.NanoWSD;
import gu.dtalk.Ack;
import gu.dtalk.MenuItem;
import gu.simplemq.exceptions.SmqUnsubscribeException;
import gu.simplemq.json.BaseJsonEncoder;
import net.gdface.utils.FaceUtilits;

import static gu.dtalk.CommonConstant.*;
import static gu.dtalk.Version.*;
/**
 * dtalk http 服务
 * @author guyadong
 *
 */
public class DtalkHttpServer extends NanoWSD {
	private static final Logger logger = LoggerFactory.getLogger(DtalkHttpServer.class);
    /**
     * Standard HTTP header names.
     */
    public static final class HeaderNames {
        /**
         * {@code "Accept"}
         */
        public static final String ACCEPT = "Accept";
        /**
         * {@code "Accept-Charset"}
         */
        public static final String ACCEPT_CHARSET = "Accept-Charset";
        /**
         * {@code "Accept-Encoding"}
         */
        public static final String ACCEPT_ENCODING = "Accept-Encoding";
        /**
         * {@code "Accept-Language"}
         */
        public static final String ACCEPT_LANGUAGE = "Accept-Language";
        /**
         * {@code "Accept-Ranges"}
         */
        public static final String ACCEPT_RANGES = "Accept-Ranges";
        /**
         * {@code "Accept-Patch"}
         */
        public static final String ACCEPT_PATCH = "Accept-Patch";
        /**
         * {@code "Access-Control-Allow-Credentials"}
         */
        public static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
        /**
         * {@code "Access-Control-Allow-Headers"}
         */
        public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
        /**
         * {@code "Access-Control-Allow-Methods"}
         */
        public static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
        /**
         * {@code "Access-Control-Allow-Origin"}
         */
        public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
        /**
         * {@code "Access-Control-Expose-Headers"}
         */
        public static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
        /**
         * {@code "Access-Control-Max-Age"}
         */
        public static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";
        /**
         * {@code "Access-Control-Request-Headers"}
         */
        public static final String ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers";
        /**
         * {@code "Access-Control-Request-Method"}
         */
        public static final String ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";
        /**
         * {@code "Age"}
         */
        public static final String AGE = "Age";
        /**
         * {@code "Allow"}
         */
        public static final String ALLOW = "Allow";
        /**
         * {@code "Authorization"}
         */
        public static final String AUTHORIZATION = "Authorization";
        /**
         * {@code "Cache-Control"}
         */
        public static final String CACHE_CONTROL = "Cache-Control";
        /**
         * {@code "Connection"}
         */
        public static final String CONNECTION = "Connection";
        /**
         * {@code "Content-Base"}
         */
        public static final String CONTENT_BASE = "Content-Base";
        /**
         * {@code "Content-Encoding"}
         */
        public static final String CONTENT_ENCODING = "Content-Encoding";
        /**
         * {@code "Content-Language"}
         */
        public static final String CONTENT_LANGUAGE = "Content-Language";
        /**
         * {@code "Content-Length"}
         */
        public static final String CONTENT_LENGTH = "Content-Length";
        /**
         * {@code "Content-Location"}
         */
        public static final String CONTENT_LOCATION = "Content-Location";
        /**
         * {@code "Content-Transfer-Encoding"}
         */
        public static final String CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";
        /**
         * {@code "Content-MD5"}
         */
        public static final String CONTENT_MD5 = "Content-MD5";
        /**
         * {@code "Content-Range"}
         */
        public static final String CONTENT_RANGE = "Content-Range";
        /**
         * {@code "Content-Type"}
         */
        public static final String CONTENT_TYPE = "Content-Type";
        /**
         * {@code "Cookie"}
         */
        public static final String COOKIE = "Cookie";
        /**
         * {@code "Date"}
         */
        public static final String DATE = "Date";
        /**
         * {@code "ETag"}
         */
        public static final String ETAG = "ETag";
        /**
         * {@code "Expect"}
         */
        public static final String EXPECT = "Expect";
        /**
         * {@code "Expires"}
         */
        public static final String EXPIRES = "Expires";
        /**
         * {@code "From"}
         */
        public static final String FROM = "From";
        /**
         * {@code "Host"}
         */
        public static final String HOST = "Host";
        /**
         * {@code "If-Match"}
         */
        public static final String IF_MATCH = "If-Match";
        /**
         * {@code "If-Modified-Since"}
         */
        public static final String IF_MODIFIED_SINCE = "If-Modified-Since";
        /**
         * {@code "If-None-Match"}
         */
        public static final String IF_NONE_MATCH = "If-None-Match";
        /**
         * {@code "If-Range"}
         */
        public static final String IF_RANGE = "If-Range";
        /**
         * {@code "If-Unmodified-Since"}
         */
        public static final String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";
        /**
         * {@code "Last-Modified"}
         */
        public static final String LAST_MODIFIED = "Last-Modified";
        /**
         * {@code "Location"}
         */
        public static final String LOCATION = "Location";
        /**
         * {@code "Max-Forwards"}
         */
        public static final String MAX_FORWARDS = "Max-Forwards";
        /**
         * {@code "Origin"}
         */
        public static final String ORIGIN = "Origin";
        /**
         * {@code "Pragma"}
         */
        public static final String PRAGMA = "Pragma";
        /**
         * {@code "Proxy-Authenticate"}
         */
        public static final String PROXY_AUTHENTICATE = "Proxy-Authenticate";
        /**
         * {@code "Proxy-Authorization"}
         */
        public static final String PROXY_AUTHORIZATION = "Proxy-Authorization";
        /**
         * {@code "Range"}
         */
        public static final String RANGE = "Range";
        /**
         * {@code "Referer"}
         */
        public static final String REFERER = "Referer";
        /**
         * {@code "Retry-After"}
         */
        public static final String RETRY_AFTER = "Retry-After";
        /**
         * {@code "Sec-WebSocket-Key1"}
         */
        public static final String SEC_WEBSOCKET_KEY1 = "Sec-WebSocket-Key1";
        /**
         * {@code "Sec-WebSocket-Key2"}
         */
        public static final String SEC_WEBSOCKET_KEY2 = "Sec-WebSocket-Key2";
        /**
         * {@code "Sec-WebSocket-Location"}
         */
        public static final String SEC_WEBSOCKET_LOCATION = "Sec-WebSocket-Location";
        /**
         * {@code "Sec-WebSocket-Origin"}
         */
        public static final String SEC_WEBSOCKET_ORIGIN = "Sec-WebSocket-Origin";
        /**
         * {@code "Sec-WebSocket-Protocol"}
         */
        public static final String SEC_WEBSOCKET_PROTOCOL = "Sec-WebSocket-Protocol";
        /**
         * {@code "Sec-WebSocket-Version"}
         */
        public static final String SEC_WEBSOCKET_VERSION = "Sec-WebSocket-Version";
        /**
         * {@code "Sec-WebSocket-Key"}
         */
        public static final String SEC_WEBSOCKET_KEY = "Sec-WebSocket-Key";
        /**
         * {@code "Sec-WebSocket-Accept"}
         */
        public static final String SEC_WEBSOCKET_ACCEPT = "Sec-WebSocket-Accept";
        /**
         * {@code "Server"}
         */
        public static final String SERVER = "Server";
        /**
         * {@code "Set-Cookie"}
         */
        public static final String SET_COOKIE = "Set-Cookie";
        /**
         * {@code "Set-Cookie2"}
         */
        public static final String SET_COOKIE2 = "Set-Cookie2";
        /**
         * {@code "TE"}
         */
        public static final String TE = "TE";
        /**
         * {@code "Trailer"}
         */
        public static final String TRAILER = "Trailer";
        /**
         * {@code "Transfer-Encoding"}
         */
        public static final String TRANSFER_ENCODING = "Transfer-Encoding";
        /**
         * {@code "Upgrade"}
         */
        public static final String UPGRADE = "Upgrade";
        /**
         * {@code "User-Agent"}
         */
        public static final String USER_AGENT = "User-Agent";
        /**
         * {@code "Vary"}
         */
        public static final String VARY = "Vary";
        /**
         * {@code "Via"}
         */
        public static final String VIA = "Via";
        /**
         * {@code "Warning"}
         */
        public static final String WARNING = "Warning";
        /**
         * {@code "WebSocket-Location"}
         */
        public static final String WEBSOCKET_LOCATION = "WebSocket-Location";
        /**
         * {@code "WebSocket-Origin"}
         */
        public static final String WEBSOCKET_ORIGIN = "WebSocket-Origin";
        /**
         * {@code "WebSocket-Protocol"}
         */
        public static final String WEBSOCKET_PROTOCOL = "WebSocket-Protocol";
        /**
         * {@code "WWW-Authenticate"}
         */
        public static final String WWW_AUTHENTICATE = "WWW-Authenticate";

        private HeaderNames() {
        }
    }

    /**
     * Standard HTTP header values.
     */
    public static final class HeaderValues {
        /**
         * {@code "application/x-www-form-urlencoded"}
         */
        public static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
        /**
         * {@code "base64"}
         */
        public static final String BASE64 = "base64";
        /**
         * {@code "binary"}
         */
        public static final String BINARY = "binary";
        /**
         * {@code "boundary"}
         */
        public static final String BOUNDARY = "boundary";
        /**
         * {@code "bytes"}
         */
        public static final String BYTES = "bytes";
        /**
         * {@code "charset"}
         */
        public static final String CHARSET = "charset";
        /**
         * {@code "chunked"}
         */
        public static final String CHUNKED = "chunked";
        /**
         * {@code "close"}
         */
        public static final String CLOSE = "close";
        /**
         * {@code "compress"}
         */
        public static final String COMPRESS = "compress";
        /**
         * {@code "100-continue"}
         */
        public static final String CONTINUE =  "100-continue";
        /**
         * {@code "deflate"}
         */
        public static final String DEFLATE = "deflate";
        /**
         * {@code "gzip"}
         */
        public static final String GZIP = "gzip";
        /**
         * {@code "identity"}
         */
        public static final String IDENTITY = "identity";
        /**
         * {@code "keep-alive"}
         */
        public static final String KEEP_ALIVE = "keep-alive";
        /**
         * {@code "max-age"}
         */
        public static final String MAX_AGE = "max-age";
        /**
         * {@code "max-stale"}
         */
        public static final String MAX_STALE = "max-stale";
        /**
         * {@code "min-fresh"}
         */
        public static final String MIN_FRESH = "min-fresh";
        /**
         * {@code "multipart/form-data"}
         */
        public static final String MULTIPART_FORM_DATA = "multipart/form-data";
        /**
         * {@code "must-revalidate"}
         */
        public static final String MUST_REVALIDATE = "must-revalidate";
        /**
         * {@code "no-cache"}
         */
        public static final String NO_CACHE = "no-cache";
        /**
         * {@code "no-store"}
         */
        public static final String NO_STORE = "no-store";
        /**
         * {@code "no-transform"}
         */
        public static final String NO_TRANSFORM = "no-transform";
        /**
         * {@code "none"}
         */
        public static final String NONE = "none";
        /**
         * {@code "only-if-cached"}
         */
        public static final String ONLY_IF_CACHED = "only-if-cached";
        /**
         * {@code "private"}
         */
        public static final String PRIVATE = "private";
        /**
         * {@code "proxy-revalidate"}
         */
        public static final String PROXY_REVALIDATE = "proxy-revalidate";
        /**
         * {@code "public"}
         */
        public static final String PUBLIC = "public";
        /**
         * {@code "quoted-printable"}
         */
        public static final String QUOTED_PRINTABLE = "quoted-printable";
        /**
         * {@code "s-maxage"}
         */
        public static final String S_MAXAGE = "s-maxage";
        /**
         * {@code "trailers"}
         */
        public static final String TRAILERS = "trailers";
        /**
         * {@code "Upgrade"}
         */
        public static final String UPGRADE = "Upgrade";
        /**
         * {@code "WebSocket"}
         */
        public static final String WEBSOCKET = "WebSocket";

        private HeaderValues() {
        }
    }

	private static final String DTALK_SESSION="dtalk-session"; 
	public static final String APPICATION_JSON="application/json";
	private static final String UNAUTH_SESSION="UNAUTHORIZATION SESSION";
	private static final String AUTH_OK="AUTHORIZATION OK";
	private static final String CLIENT_LOCKED="ANOTHER CLIENT LOCKED";
	private static final String INVALID_PWD="INVALID REQUEST PASSWORD";
	private static final String POST_DATA="postData";
	private static final String DTALK_PREFIX="/dtalk";
	private static final String STATIC_PAGE_PREFIX="/web";
	private static class SingletonTimer{
		private static final Timer instnace = new Timer(true);
	}
	private Timer timer;
	private Timer getTimer(){
		// 懒加载
		if(timer == null){
			timer = SingletonTimer.instnace;
		}
		return timer;
	}
	private long idleTimeLimit = DEFAULT_IDLE_TIME_MILLS;
	private long timerPeriod = 2000;

	private String selfMac;
	
	/**
	 * 当前对话ID
	 */
	private String dtalkSession;
	/**
	 * 当前websocket 连接
	 */
	private final AtomicReference<WebSocket> wsReference = new AtomicReference<>();
	private final Supplier<AtomicReference<WebSocket>> webSocketSupplier = new Supplier<AtomicReference<WebSocket>>() {

		@Override
		public AtomicReference<WebSocket> get() {
			return wsReference;
		}
	};
	private ItemEngineHttpImpl engine = new ItemEngineHttpImpl().setSupplier(webSocketSupplier);
	private boolean debug = false;
	private boolean noAuth = false;
	private static final Map<String,String>MIME_OF_SUFFIX = ImmutableMap.<String,String>builder()
	.put(".jpeg", "image/jpeg")
	.put(".jpg", "image/jpeg")
	.put(".png", "image/png")
	.put(".gif", "image/gif")
	.put(".htm","text/html")
	.put(".html","text/html")
	.put(".txt","text/plain")
	.put(".csv","text/csv")
	.put(".json","application/json")
	.put(".js","application/javascript")
	.put(".xml","application/xml")
	.put(".zip","application/zip")
	.put(".pdf","application/pdf")
	.put(".sql","application/sql")
	.put(".doc","application/msword")    		
	.build();
	public DtalkHttpServer()  {
		this(DEFAULT_HTTP_PORT);
	}
    public DtalkHttpServer(int port)  {
        super(port);
        selfMac = FaceUtilits.toHex(DeviceUtils.DEVINFO_PROVIDER.getMac());
		// 定时检查引擎工作状态，当空闲超时，则中止连接
		getTimer().schedule(new TimerTask() {

			@Override
			public void run() {
				try{
					if(null != dtalkSession && DtalkHttpServer.this.isAlive()){
						long lasthit = engine.lastHitTime();
						if(System.currentTimeMillis() - lasthit > idleTimeLimit){
							resetSession();
						}
					}
				}catch (Exception e) {
					logger.error(e.getMessage());
				}
			}
		}, 0, timerPeriod);

    }
    private boolean isAuthorizationSession(IHTTPSession session){
    	return dtalkSession != null && dtalkSession.equals(session.getCookies().read(DTALK_SESSION));
    }
    private void checkAuthorizationSession(IHTTPSession session){
    	checkState(noAuth  || isAuthorizationSession(session),UNAUTH_SESSION);
    }
    private <T> Response responseAck(Ack<T> ack){
    	String json=BaseJsonEncoder.getEncoder().toJsonString(ack);
    	return newFixedLengthResponse(
    			Ack.Status.OK.equals(ack.getStatus()) ? Status.OK: Status.INTERNAL_ERROR, 
    			APPICATION_JSON, 
    			json);
    }
	private void resetSession(){
		synchronized (wsReference) {
			dtalkSession = null;
			WebSocket wsSocket = wsReference.get();
			if(null != wsSocket){
	    		try {
					wsSocket.close(CloseCode.NormalClosure, "", false);
				} catch (IOException e) {
					e.printStackTrace();
				}
	    		wsSocket=null;
			}
		}
	}
	private Response responseStaticResource(String uri){    	
		InputStream res = getClass().getResourceAsStream(STATIC_PAGE_PREFIX + uri);
		if(null != res){
			String suffix = uri.substring(uri.lastIndexOf('.'));
			if(MIME_OF_SUFFIX.containsKey(suffix)){
				return newChunkedResponse(
						Status.OK, 
						MIME_OF_SUFFIX.get(suffix), 
						res);
			}else{
				return newFixedLengthResponse(
		    			Status.UNSUPPORTED_MEDIA_TYPE, 
		    			NanoHTTPD.MIME_PLAINTEXT, 
		    			String.format("UNSUPPORTED MEDIA TYPE %s", suffix));	
			}
		}
		return newFixedLengthResponse(
				Status.NOT_FOUND, 
				NanoHTTPD.MIME_PLAINTEXT, 
				String.format("NOT FOUND resource %s", uri));	
	}
	private boolean isCORS(IHTTPSession session) {
		Map<String, String> headers = session.getHeaders();
		return Method.OPTIONS.equals(session.getMethod()) && 
			headers.containsKey(HeaderNames.ORIGIN);
	}
	private static Response responseCORS(IHTTPSession session,Response resp) {
		resp = MoreObjects.firstNonNull(resp,newFixedLengthResponse(""));
		resp.addHeader(HeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
		String allowMethods = Joiner.on(',').join(Arrays.asList(Method.POST,Method.GET,Method.OPTIONS));
		resp.addHeader(HeaderNames.ACCESS_CONTROL_ALLOW_METHODS, allowMethods);
		String allowHeaders = Joiner.on(',').join(Arrays.asList(HeaderNames.CONTENT_TYPE,"X-PINGOTHER"	));
		resp.addHeader(HeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, allowHeaders);
		resp.addHeader(HeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
		resp.addHeader(HeaderNames.ACCESS_CONTROL_MAX_AGE, "86400");
		//resp.addHeader(HeaderNames.ACCESS_CONTROL_MAX_AGE, "0");
		resp.addHeader("withCredentials", "true");
		return resp;
	}
	@Override
	public void start(int timeout, boolean daemon) throws IOException {
		if(!isAlive()){
			super.start(timeout, daemon);
			// 定时发送PING
			getTimer().schedule(new TimerTask() {

				@Override
				public void run() {
					
						if(null != dtalkSession && DtalkHttpServer.this.isAlive()){
							
							synchronized (wsReference) {
								try{
									WebSocket wsSocket = wsReference.get();
									if(dtalkSession != null && wsSocket != null && wsSocket.isOpen()){
										wsSocket.ping(new byte[0]);
									}
								}catch (Exception e) {
									logger.error("{}:{}",e.getClass().getName(),e.getMessage());
									//logger.error(e.getMessage(),e);
								}			
							}										
						}

				}
			}, 0, timeout*3/4);
		}
	}
	@Override
    public Response serve(IHTTPSession session) {
    	if (isWebsocketRequested(session) && ! isAuthorizationSession(session)) {
    		return newFixedLengthResponse(
        			Status.INTERNAL_ERROR, 
        					NanoHTTPD.MIME_PLAINTEXT, 
        					UNAUTH_SESSION);
    	}
		return super.serve(session);    	
    }
    @Override
    public Response serveHttp(IHTTPSession session) {
//    	if(isCORS(session)){
//    		return responseCORS(session,null);
//    	}
    	Ack<Object> ack = new Ack<Object>().setStatus(Ack.Status.OK).setDeviceMac(selfMac);
    	try{
    		switch(session.getUri()){
    		case "/login":
    			login(session, ack);
    			break;
    		case "/logout":
    			logout(session, ack);
    			break;
    		case "/":
    		case "/index.html":
    		case "/index.htm":
    		{
    			String msg = new String(FaceUtilits.getBytes(getClass().getResource(STATIC_PAGE_PREFIX + "/index.html")))
    					.replace("{VERSION}", VERSION);
    			return newFixedLengthResponse(Status.OK, NanoHTTPD.MIME_HTML, msg);
    		}
    		default:
    			
				if(session.getUri().startsWith(DTALK_PREFIX )){
    				checkAuthorizationSession(session);
    				if(DTALK_PREFIX.equals(session.getUri())){
    					checkState(Method.POST.equals(session.getMethod()),"POST method supported only");
    				}
    				JSONObject jsonObject = getJSONObject(session);
    				if(session.getUri().startsWith(DTALK_PREFIX + '/')){
	    				String path=session.getUri().substring(DTALK_PREFIX.length());
	    				jsonObject.put("path", path);
    				} 
    				try {
    					engine.onSubscribe(jsonObject);
    					return responseCORS(session,engine.getResponse());
					} catch (SmqUnsubscribeException e) {
						logout(session, ack);
						break;
					}     				
    			}
				return responseStaticResource(session.getUri());
    		}
    	}catch (Exception e) {    		
    		ack.setStatus(Ack.Status.ERROR).setException(e.getClass().getName()).setStatusMessage(e.getMessage());
    	}
    	return responseCORS(session,responseAck(ack));
    }
	/**
	 * @param isMd5 
	 * @return 
	 */
	private boolean validate(String pwd, boolean isMd5) {
		checkArgument(pwd != null,"NULL PASSWORD");
		
		String admPwd = checkNotNull(DEVINFO_PROVIDER.getPassword(),"admin password for device is null");
		checkArgument(!Strings.isNullOrEmpty(pwd),"NULL REQUEST PASSWORD");
		checkArgument(!Strings.isNullOrEmpty(admPwd),"NULL ADMIN PASSWORD");
		if(isMd5){
			String pwdmd5 = FaceUtilits.getMD5String(admPwd.getBytes());
			return pwdmd5.equalsIgnoreCase(pwd);
		}else{
			return admPwd.equals(pwd);
		}
	}
	
	private static  Map<String, String> getParamOfPostBody(IHTTPSession session) throws IOException, ResponseException{
		Map<String,String> postData = Maps.newHashMap();
		session.parseBody(postData);
		return BaseJsonEncoder.getEncoder().fromJson(postData.get(POST_DATA), 
				new TypeReference<Map<String, String>>(){}.getType());		
	}
	@SuppressWarnings("deprecation")
	private static JSONObject getJSONObject(IHTTPSession session) throws IOException, ResponseException{
    	String jsonstr;
    	if(Method.POST.equals(session.getMethod())){ 
    		Map<String,String> postData = Maps.newHashMap();
    		session.parseBody(postData);
    		jsonstr = postData.get(POST_DATA);
    	}else{
    		jsonstr = BaseJsonEncoder.getEncoder().toJsonString(session.getParms());
    	}
		return BaseJsonEncoder.getEncoder().fromJson(jsonstr,JSONObject.class);
	}
    @SuppressWarnings("deprecation")
	private static Map<String, String> getParams(IHTTPSession session) throws IOException, ResponseException{
    	Map<String,String> params = Maps.newHashMap();
    	if(Method.POST.equals(session.getMethod())){    		
			params = getParamOfPostBody(session);			
    	}else{
    		params = session.getParms();
    	}
    	return params;
    }
    
	protected synchronized void login(IHTTPSession session, Ack<Object> ack) throws IOException, ResponseException{

    	Map<String,String> parms = getParams(session);

    	String sid=parms.get(DTALK_SESSION);
    	if(sid ==null){
    		sid=session.getCookies().read(DTALK_SESSION);
    	}
    	
    	if (dtalkSession == null || sid == null ){
			checkState(validate(parms.get("password"), 
					Boolean.valueOf(MoreObjects.firstNonNull(parms.get("isMd5"), "true"))),INVALID_PWD);
			sid = dtalkSession = Long.toHexString(System.nanoTime());
	    	session.getCookies().set(DTALK_SESSION, dtalkSession, 1);
	    	logger.info("session {} connected",dtalkSession);
    	}
    	checkState(Objects.equal(dtalkSession, sid),CLIENT_LOCKED);
        ack.setStatus(Ack.Status.OK).setStatusMessage(AUTH_OK);
       	engine.setLastHitTime(System.currentTimeMillis());

	}
	protected synchronized void logout(IHTTPSession session, Ack<Object> ack) throws IOException, ResponseException{
    	checkAuthorizationSession(session);
   		resetSession();
       	ack.setStatus(Ack.Status.OK).setStatusMessage("logout OK");
    }

	/**
	 * @return engine
	 */
	public ItemEngineHttpImpl getItemAdapter() {
		return engine;
	}

	/**
	 * @param engine 要设置的 engine
	 * @return 
	 */
	public DtalkHttpServer setItemAdapter(ItemEngineHttpImpl engine) {
		this.engine = checkNotNull(engine,"engine is null").setSupplier(webSocketSupplier);
		return this;
	}
	
	/**
	 * @return
	 * @see gu.dtalk.engine.BaseItemEngine#getRoot()
	 */
	public DtalkHttpServer getRoot() {
		engine.getRoot();
		return this;
	}
	/**
	 * @param root
	 * @return
	 * @see gu.dtalk.engine.BaseItemEngine#setRoot(gu.dtalk.MenuItem)
	 */
	public DtalkHttpServer setRoot(MenuItem root) {
		engine.setRoot(root);
		return this;
	}
	/**
	 * 设置定义检查连接的任务时间间隔(毫秒)
	 * @param timerPeriod
	 * @return
	 */
	public DtalkHttpServer setTimerPeriod(long timerPeriod) {
		if(timerPeriod > 0){
			this.timerPeriod = timerPeriod;
		}
		return this;
	}
	@Override
	protected WebSocket openWebSocket(IHTTPSession handshake) {
		return new DtalkWebSocket(handshake);
	}
	private class DtalkWebSocket extends WebSocket {

		public DtalkWebSocket(IHTTPSession handshakeRequest) {
			super(handshakeRequest);
		}

		@Override
		protected void onOpen() {
			synchronized (wsReference) {
				wsReference.set(this);
			}			
		}

		@Override
		protected void onClose(CloseCode code, String reason, boolean initiatedByRemote) {
			if(debug){
	            logger.info("C [" + (initiatedByRemote ? "Remote" : "Self") + "] " + (code != null ? code : "UnknownCloseCode[" + code + "]")
	                    + (reason != null && !reason.isEmpty() ? ": " + reason : ""));
			}
		}

		@Override
		protected void onMessage(WebSocketFrame message) {
            try {
                message.setUnmasked();
                sendFrame(message);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
		}

		@Override
		protected void debugFrameReceived(WebSocketFrame frame) {
			if(debug){
				logger.info("frame:{}",frame);
			}
		}

		@Override
		protected void onPong(WebSocketFrame pong) {
		}

		@Override
		protected void onException(IOException exception) {
			
			logger.info("{}:{}",exception.getClass().getName(),exception.getMessage());
		}
		
	}
	/**
	 * 设置 DEBUG 模式，默认false
	 * @param debug 要设置的 debug
	 * @return 
	 */
	public DtalkHttpServer setDebug(boolean debug) {
		this.debug = debug;
		return this;
	}
	/**
	 * 设置是否不验证session合法性,默认false<br>
	 * 开发模式下可以设置为true,跳过安全验证
	 * @param noAuth 要设置的 noAuth
	 * @return 
	 */
	public DtalkHttpServer setNoAuth(boolean noAuth) {
		this.noAuth = noAuth;
		return this;
	}
}
