package gu.dtalk.engine.demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gu.dtalk.engine.DeviceUtils;
import gu.dtalk.engine.ItemAdapter;
import gu.dtalk.engine.ItemEngineRedisImpl;
import gu.dtalk.engine.SampleConnector;
import gu.dtalk.redis.DefaultCustomRedisConfigProvider;
import gu.dtalk.redis.RedisConfigType;
import gu.simplemq.ISubscriber;
import gu.simplemq.Channel;
import gu.simplemq.redis.JedisPoolLazy;
import gu.simplemq.redis.RedisFactory;
import net.gdface.utils.FaceUtilits;
import net.gdface.utils.NetworkUtil;

import static gu.dtalk.CommonUtils.*;
import static gu.dtalk.engine.demo.DemoConfig.*;

/**
 * dtalk引擎演示
 * @author guyadong
 *
 */
public class Demo {
	private static final Logger logger = LoggerFactory.getLogger(Demo.class);

	private final SampleConnector connAdapter;
	private final ISubscriber subscriber;
	private final byte[] devMac;
	public Demo(RedisConfigType configType) {
		JedisPoolLazy pool = JedisPoolLazy.getInstance(configType.readRedisParam(),false);
		subscriber = RedisFactory.getSubscriber(pool);
		DemoMenu root = new DemoMenu(configType).init().register(DemoListener.INSTANCE);
		devMac = DeviceUtils.DEVINFO_PROVIDER.getMac();
		connAdapter = new SampleConnector(pool)
				.setSelfMac(FaceUtilits.toHex(devMac))
				.setItemAdapter((ItemAdapter) new ItemEngineRedisImpl(pool).setRoot(root));
	}
	/**
	 * 启动连接
	 */
	private void start(){
		System.out.printf("DEVICE MAC address(设备地址): %s\n",NetworkUtil.formatMac(devMac, ":"));
		String connchname = getConnChannel(devMac);
		Channel<String> connch = new Channel<>(connchname, String.class)
				.setAdapter(connAdapter);
		subscriber.register(connch);
		System.out.printf("Connect channel registered(连接频道注册) : %s \n",connchname);
	}
	private static void waitquit(){
		System.out.println("PRESS 'quit' OR 'CTRL-C' to exit");
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in)); 
		try{
			while(!"quit".equalsIgnoreCase(reader.readLine())){				
			}
			System.exit(0);
		} catch (IOException e) {

		}finally {

		}
	}
	public static void main(String []args){		
		try{
			DEMO_CONFIG.parseCommandLine(args);
			DefaultCustomRedisConfigProvider.initredisParameters(DEMO_CONFIG.getRedisParameters());

			System.out.println("Device talk Demo starting(设备模拟器启动)");
			RedisConfigType config = RedisConfigType.lookupRedisConnect();
			logger.info("use config={}",config.toString());
			// 创建redis连接实例
			JedisPoolLazy.createDefaultInstance( config.readRedisParam() );
			
			new Demo(config).start();
			waitquit();
		}catch (Exception e) {
			if(DEMO_CONFIG.isTrace()){
				e.printStackTrace();	
			}else{
				System.out.println(e.getMessage());
			}
		}
	}

}
