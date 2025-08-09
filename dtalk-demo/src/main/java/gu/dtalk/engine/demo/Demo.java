package gu.dtalk.engine.demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import gu.dtalk.engine.ItemEngine;
import gu.dtalk.engine.SampleConnector;
import gu.dtalk.redis.RedisConfig;
import gu.simplemq.Channel;
import gu.simplemq.redis.JedisPoolLazy;
import gu.simplemq.redis.RedisFactory;
import gu.simplemq.redis.RedisSubscriber;
import net.gdface.utils.NetworkUtil;
import static gu.dtalk.CommonUtils.*;
import static gu.dtalk.engine.SampleConnector.*;

public class Demo {

	private final SampleConnector connAdapter;
	private final RedisSubscriber subscriber;
	private final byte[] devMac;
	public Demo() {
		JedisPoolLazy pool = JedisPoolLazy.getDefaultInstance();
		subscriber = RedisFactory.getSubscriber(pool);
		DemoMenu root = new DemoMenu().init().register(DemoListener.INSTANCE);
		connAdapter = new SampleConnector(pool).setItemAdapter(new ItemEngine(pool).setRoot(root));
		devMac = DEVINFO_PROVIDER.getMac();
	}
	private void start(){
		System.out.printf("DEVICE MAC address(设备地址): %s\n",NetworkUtil.formatMac(devMac, ":"));
		String connchname = getConnChannel(devMac);
		Channel<String> connch = new Channel<>(connchname, String.class)
				.setAdapter(connAdapter);
		subscriber.register(connch);
		System.out.printf("Connect channel registered(连接频道注册) : %s \n",connchname);
	}
	private static void waitquit(){
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in)); 
		try{
			while(!"quit".equalsIgnoreCase(reader.readLine())){
				return;
			}
		} catch (IOException e) {

		}finally {

		}
	}
	public static void main(String []args){
		try{
			System.out.println("Device talk Demo starting(设备模拟器启动)");
			// 创建redis连接实例
			JedisPoolLazy.createDefaultInstance( RedisConfig.readRedisParam() );
			
			new Demo().start();
			System.out.println("PRESS 'quit' OR 'CTRL-C' to exit");
			waitquit();
		}catch (Exception e) {
			//System.out.println(e.getMessage());
			e.printStackTrace();
			return ;
		}
	}

}
