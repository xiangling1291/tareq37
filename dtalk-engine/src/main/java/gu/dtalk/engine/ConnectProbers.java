package gu.dtalk.engine;

import java.util.LinkedHashSet;

/**
 * DTALK连接侦听器管理器<br>
 * 实现支持多个{@link ConnectProber}实例
 * @author guyadong
 *
 */
public class ConnectProbers extends LinkedHashSet<ConnectProber>implements ConnectProber{
	private static final long serialVersionUID = 1L;

	public ConnectProbers() {
		super();
	}

	@Override
	public void onDisconnect() {
		for(ConnectProber prober:this){
			if(null != prober){
				try {
					prober.onDisconnect();					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void onConnect() {
		for(ConnectProber prober:this){
			if(null != prober){
				try{
					prober.onConnect();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

}
