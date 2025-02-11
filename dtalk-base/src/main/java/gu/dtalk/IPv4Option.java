package gu.dtalk;

import com.google.common.base.Predicate;

import net.gdface.utils.NetworkUtil;

/**
 * IP(ipv4)地址选项类型
 * @author guyadong
 *
 */
public class IPv4Option extends BaseBinary {
	public static final Predicate<byte[]> VALIDATOR = new Predicate<byte[]>() {
		@Override
		public boolean apply(byte[] input) {
			return input != null && input.length==4;
		}
	};
	public static final Predicate<String> STR_VALIDATOR = new Predicate<String>() {
		@Override
		public boolean apply(String input) {
			return input != null
					&& input.matches("^((?:(?:25[0-5]|2[0-4]\\d|[01]?\\d?\\d)\\.){3}(?:25[0-5]|2[0-4]\\d|[01]?\\d?\\d))$");
		}
	};
	public IPv4Option() {
		setValidator(VALIDATOR);
	}
	@Override
	public OptionType getType() {
		return OptionType.IP;
	}
	@Override
	public String contentOfValue() {
		if(getValue() == null){
			return super.contentOfValue();
		}
		return NetworkUtil.formatIp((byte[]) getValue());		
	}
}
