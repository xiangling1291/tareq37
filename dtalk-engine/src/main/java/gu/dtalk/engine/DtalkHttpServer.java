package gu.dtalk.engine;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static gu.dtalk.CommonConstant.DEFAULT_IDLE_TIME_MILLS;
import static gu.dtalk.engine.DeviceUtils.DEVINFO_PROVIDER;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
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
	private static final String DTALK_SESSION="dtalk-session"; 
	public static final String APPICATION_JSON="application/json";
	private static final String UNAUTH_SESSION="UNAUTHORIZATION SESSION";
	private static final String AUTH_OK="AUTHORIZATION OK";
	private static final String CLIENT_LOCKED="ANOTHER CLIENT LOCKED";
	private static final String INVALID_PWD="INVALID REQUEST PASSWORD";
	private static final String POST_DATA="postData";
	private static final String DTALK_PREFIX="/dtalk";
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
    private Response responseStaticResource(String uri){    	
		InputStream res = getClass().getResourceAsStream(uri);
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

	@Override
    public Response serveHttp(IHTTPSession session) {
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
    		{
    			String msg = "<html><body><h1>Dtalk server "+VERSION+"</h1>\n</body></html>\n";
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
    					return engine.getResponse();
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
    	return responseAck(ack);
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
