package gu.dtalk;

/**
 * 设备信息访问接口<br>
 * SDK初始化时以SPI(Service Provider Interface)机制加载应用层提供的
 * {@link DeviceInfoProvider}实例,并通过接口实例获取设备信息<br>
 * 
 * @author guyadong
 *
 */
public interface DeviceInfoProvider {
	/**
	 * 应用层通过此方法向底层提供管理密码
	 * @return 返回管理密码,不可为空或{@code null}
	 */
	String getPassword();
	/**
	 * 保存在管理密码.<br>
	 * 应用层通过此方法保存底层传回的管理密码
	 * @param password
	 */
	void savePassword(String password);
	
	/**
	 * 返回当前使用的网卡mac地址,不可为空或{@code null}
	 * @return
	 */
	byte[] getMac();
	/**
	 * 返回当前使用的IP地址,不可为空或{@code null}
	 * @return
	 */
	byte[] getIp();
}
