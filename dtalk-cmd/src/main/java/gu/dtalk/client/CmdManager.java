package gu.dtalk.client;

import static com.google.common.base.Preconditions.*;

import com.google.common.base.Supplier;

import gu.dtalk.DeviceInstruction;
import gu.simplemq.redis.JedisPoolLazy;

/**
 * 
 * (多目标)设备命令发送管理模块<br>
 * @author guyadong
 *
 */
public class CmdManager extends BaseCmdManager{
    private final FreshedChannelSupplier channelSupplier;
    /**
     * 构造方法
     * @param poolLazy 
     * @param cmdChannelSupplier 命令频道名
     */
    public CmdManager(JedisPoolLazy poolLazy, Supplier<String> cmdChannelSupplier) {
    	super(poolLazy);
    	this.channelSupplier = new FreshedChannelSupplier(cmdChannelSupplier);
    }

    /**
     * 发送前检查target是否有定义，未定义则抛出异常
     */
    @Override
    protected long doSendCmd(DeviceInstruction cmd){
        checkArgument(null != cmd.getTarget() && !cmd.getTarget().isEmpty(),"DeviceInstruction.target field must not be null");

        return publisher.publish(channelSupplier.get(), cmd);
    }

}
