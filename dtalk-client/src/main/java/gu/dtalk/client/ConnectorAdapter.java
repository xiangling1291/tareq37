package gu.dtalk.client;

import static gu.dtalk.CommonUtils.isAck;

import java.io.PrintStream;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.util.TypeUtils;
import com.google.common.base.Predicate;

import gu.dtalk.Ack;
import gu.dtalk.Ack.Status;
import gu.simplemq.exceptions.SmqUnsubscribeException;
/**
 * 管理端连接控制器简单实现
 * @author guyadong
 *
 */
public class ConnectorAdapter extends TextMessageAdapter<JSONObject> {
	private Predicate<String> onValidPwd;
	@Override
	public void onSubscribe(JSONObject resp) throws SmqUnsubscribeException {
		super.onSubscribe(resp);
		if(isAck(resp)){
			Ack<?> ack = TypeUtils.castToJavaBean(resp, Ack.class);
			render.rendeAck(ack);
			if(ack.getStatus() == Status.OK){
				if(onValidPwd !=null){
					onValidPwd.apply( (String) ack.getValue());
				}
			}
		}
	}
	
	public ConnectorAdapter() {
	}

	public ConnectorAdapter setStream(PrintStream stream) {
		render.setStream(stream);
		return this;
	}

	public ConnectorAdapter setOnValidPwd(Predicate<String> onValidPwd) {
		this.onValidPwd = onValidPwd;
		return this;
	}
}
