package gu.dtalk.engine;

/**
 * DTALK连接侦听器接口
 * @author guyadong
 *
 */
public interface ConnectProber {
	void onDisconnect();
	void onConnect();
}
