package gu.dtalk.client;

import java.io.IOException;
import java.util.Map;
import java.util.Scanner;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.google.common.base.MoreObjects;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import gu.dtalk.Ack;
import gu.dtalk.ConnectReq;
import gu.dtalk.ICmd;
import gu.dtalk.IItem;
import gu.dtalk.IOption;
import gu.dtalk.ItemType;
import gu.dtalk.Ack.Status;
import gu.simplemq.Channel;
import gu.simplemq.IUnregistedListener;
import gu.simplemq.redis.JedisPoolLazy;
import gu.simplemq.redis.RedisConsumer;
import gu.simplemq.redis.RedisFactory;
import gu.simplemq.redis.RedisPublisher;
import gu.simplemq.redis.JedisPoolLazy.PropName;
import net.gdface.utils.FaceUtilits;
import net.gdface.utils.NetworkUtil;
import static gu.dtalk.CommonConstant.*;
import static gu.dtalk.CommonUtils.*;
import static com.google.common.base.Preconditions.*;

public class SampleTerminal {
	/** redis 连接参数 */
	final static Map<PropName, Object> redisParam = 
			ImmutableMap.<PropName, Object>of(
					/** redis 主机名 */PropName.host,REDIS_HOST,
					/** redis 端口号 */PropName.port,REDIS_PORT
					);
	private String reqChannel = null;
	final RedisConsumer consumer;
	final RedisPublisher publisher;
	final byte[] mymac;
	private final String ackchname;
	private final String connchname;
	private final TextRenderEngine renderEngine = new TextRenderEngine();
	/**
	 * 构造方法
	 * @param devmac 要连接的设备MAC地址,测试设备程序在本地运行时可为空。
	 */
	private SampleTerminal(String devmac) {
		// 根据连接参数创建默认实例 
		try {
			JedisPoolLazy.createDefaultInstance( redisParam);
			consumer = RedisFactory.getConsumer(JedisPoolLazy.getDefaultInstance());
			publisher = RedisFactory.getPublisher(JedisPoolLazy.getDefaultInstance());

			mymac = NetworkUtil.getCurrentMac(REDIS_HOST, REDIS_PORT);
			System.out.printf("TERMINAL MAC address: %s\n", NetworkUtil.formatMac(mymac, ":"));

			ackchname = getAckChannel(mymac);
			if(Strings.isNullOrEmpty(devmac)){
				// 使用本地地址做为设备MAC地址
				devmac = FaceUtilits.toHex(mymac);
				System.out.println("use local MAC for target DEVICE");
			}
			System.out.printf("DEVICE MAC address: %s\n", devmac);

			connchname = getConnChannel(devmac);
			Channel<String> testch = new Channel<String>(connchname, String.class);
			long rc = publisher.publish(testch, "hello");
			checkState(rc != 0,"TARGET DEVICE NOT online");
			if(rc>1){
				System.out.println("DUPLIDATED TARGET DEVICE WITH same MAC address");
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}		
	}
	/**
	 * 尝试连接目标设备
	 */
	private void connect() {
		final ConnectorAdapter connectorAdapter = new ConnectorAdapter();
		final Channel<Ack<String>> ackChannel = new Channel<Ack<String>>(
				ackchname, 
				new TypeReference<Ack<String>>(){}.getType())
				.setAdapter(connectorAdapter);
		// 连接成功时根据返回的
		ackChannel.addUnregistedListener(new IUnregistedListener<Ack<String>>() {

			@Override
			public void apply(Channel<Ack<String>> channel) {
				reqChannel = connectorAdapter.getReqChannel();				
				System.out.println("Request Channel:" + reqChannel);
				if(!Strings.isNullOrEmpty(reqChannel)){
					Channel<JSONObject> c = new Channel<JSONObject>(ackchname,JSONObject.class)
							.setAdapter(renderEngine.reset());	
					consumer.register(c);
				}
			}
		});
		consumer.register(ackChannel);		
		
	}
	private static String scanLine(Predicate<String>validate){
		Scanner scaner = new Scanner(System.in);
		try{
			return scanLine(validate,scaner);
		}finally {
			scaner.close();
		}
	}
	private static String scanLine(Predicate<String>validate,Scanner scaner){
		scaner.useDelimiter("\r?\n");
		while (scaner.hasNextLine()) {
			String str = scaner.next();
			if(str.isEmpty()){
				return "";
			}
			if(validate.apply(str)){
				return str;
			}
		}
		return "";
	}
	/**
	 * 输入目标设备的MAC地址
	 * @return
	 */
	private static String inputMac(){
		System.out.println("Input MAC address of Device,such as '00:00:7f:2a:39:4A' or '00e8992730FF':"
			+ "(input empty string if target device demo running on localhost)"
				);
		return scanLine(new Predicate<String>() {
			@Override
			public boolean apply(String input) {
				String mac = parseMac(input);
				if(!mac.isEmpty()){
					return true;
				}
				System.out.println("ERROR:Invalid mac adress");
				return false;
			}
		});

	}
	private boolean validatePwd(){
		System.out.println("Input password  of Device,default password is last 4 character of device MAC address(lowercase):");
		ConnectReq req = new ConnectReq();
		req.mac = FaceUtilits.toHex(mymac);
		Channel<ConnectReq> conch = new Channel<>(connchname, ConnectReq.class);
		String pwd;
		while (reqChannel == null 
				&& !(pwd=scanLine(Predicates.<String>alwaysTrue())).isEmpty()) {
			req.pwd = pwd;
			syncPublish(conch,req);
		}
		return reqChannel != null;

	}
	private void waitResp(long timestamp){
		int waitCount = 30;
		while(renderEngine.getLastResp()< timestamp && waitCount > 0){
			try {
				Thread.sleep(100);
				waitCount --;
			} catch (InterruptedException e) {
				System.exit(-1);
			}
		}
		if(waitCount ==0){
			System.out.println("TIMEOUT for response");
			System.exit(-1);
		}
	}
	JSONObject makeItemJSON(String path){
		checkArgument(Strings.isNullOrEmpty(path));
		IItem item = checkNotNull(renderEngine.getCurrentLevel().getChildByPath(path),"NOT FOUND item %s",path);
		JSONObject json = new JSONObject()
			.fluentPut(ITEM_FIELD_NAME, item.getName())
			.fluentPut(ITEM_FIELD_PATH,path)
			.fluentPut(ITEM_FIELD_CATALOG, item.getCatalog());
		if(item instanceof IOption){
			json.put(OPTION_FIELD_TYPE, ((IOption)item).getType());
		}
		return json;
	}
	private <T>boolean syncPublish(Channel<T>channel,T json){
		try{
			long timestamp = System.currentTimeMillis();
			long rc = publisher.publish(channel, json);
			// 没有接收端则抛出异常
			checkState(rc != 0,"target device DISCONNECT");
			waitResp(timestamp);
			return true;
		}catch(Exception e){
			System.out.println(e.getMessage());
		}
		return false;
	}
	private boolean syncPublishReq(Object json){
		Channel<Object> reqCh = new Channel<Object>(checkNotNull(reqChannel), Object.class);
		return syncPublish(reqCh, json);
	}

	private boolean inputOption(Scanner scaner,JSONObject json){
		checkArgument(json !=null && ItemType.OPTION == json.getObject(ITEM_FIELD_CATALOG, ItemType.class));
		IItem item = renderEngine.getCurrentLevel().getChildByPath(json.getString(ITEM_FIELD_PATH));
		checkArgument(item instanceof IOption);
		IOption option = (IOption)item;
		String desc = Strings.isNullOrEmpty(option.getDescription()) ? "" : "("+option.getDescription()+")"; 
		System.out.printf("INPUT VALUE for %s%s:", option.getUiName(),desc);
		String value = scanLine(Predicates.<String>alwaysTrue(),scaner);
		if(!value.isEmpty()){
			json.fluentPut(OPTION_FIELD_VALUE, value);
			return true;
		}
		return false;
	}
	private boolean inputCmd(Scanner scaner,JSONObject json){
		checkArgument(json !=null && ItemType.OPTION == json.getObject(ITEM_FIELD_CATALOG, ItemType.class));
		IItem item = renderEngine.getCurrentLevel().getChildByPath(json.getString(ITEM_FIELD_PATH));
		checkArgument(item instanceof ICmd);
		ICmd cmd = (ICmd)item;
		for(IOption param:cmd.getParameters()){
			JSONObject optjson = makeItemJSON(param.getPath());
			while(inputOption(scaner,optjson)){
				if(syncPublishReq(json)){
					checkState(isAck(renderEngine.getLastRespObj()));
					Status status = ((JSONObject)renderEngine.getLastRespObj()).getObject(ACK_FIELD_STATUS, Status.class);
					if(status != Status.OK){
						// 参数值无效，继续提示输入
						continue;
					}
					break;
				}
				return false;
			}
			//  继续下一个参数
		}		
		return true;
	}
	/**
	 * 键盘命令交互
	 */
	private void cmdInteractive(){
		
		if(!syncPublishReq(makeItemJSON("/"))){
			return ;
		}
	    Scanner scaner = new Scanner(System.in);
	    try{
	    	while (scaner.hasNextLine()) {
	    		String str = scaner.next();
	    		if(str.isEmpty()){
	    			continue;
	    		}
	    		try{
	    			JSONObject json = makeItemJSON(str);
	    			switch (json.getObject(ITEM_FIELD_CATALOG,ItemType.class)) {
	    			case MENU:
	    				// 进入菜单 
	    				syncPublishReq(json);
	    				break;
					case OPTION:
						// 修改参数
						if(inputOption(scaner,json)){
							syncPublishReq(json);
						}
						break;
					case CMD:						
						// 执行命令
						if(inputCmd(scaner,json)){
							syncPublishReq(json);
						}
						if(isQuit(json)){
							return;
						}
						break;
					default:
						break;
					}
	    		}catch (Exception e) {
	    			System.out.println(e.getMessage());
				}
	    	}
	    }finally {
			scaner.close();
		}
	    return;
	}
	private static String parseMac(String input){
		input = MoreObjects.firstNonNull(input, "").trim();
		if(input.matches(MAC_REG)){
			return input.replace(":", "").toLowerCase();
		}
		return "";
	}
	public static void main(String []args){
		String mac;
		if(args.length > 1){
			mac = parseMac(args[0]);
			if(mac.isEmpty()){
				System.out.printf("ERROR:Invalid mac adress %s\n",mac);
				return ;
			}				
		}
		String macstr = inputMac();
		try{
			SampleTerminal client = new SampleTerminal(macstr);
			client.connect();
			if(!client.validatePwd()){
				return;
			}
			client.cmdInteractive();
		}catch (Exception e) {
			System.out.println(e.getMessage());
			return ;
		}
	}
}
