package gu.dtalk.engine;

import java.util.LinkedHashSet;

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
