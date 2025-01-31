package gu.dtalk;

import com.google.common.base.Predicate;

import net.gdface.utils.NetworkUtil;

/**
 * mac(6 bytes)地址选项类型
 * @author guyadong
 *
 */
public class MACOption extends BaseBinary {

	public MACOption() {
		setValidator(new Predicate<byte[]>() {
			@Override
			public boolean apply(byte[] input) {
				return input != null && input.length==6;
			}
		});
	}

	@Override
	public OptionType getType() {
		return OptionType.MAC;
	}

	@Override
	public String contentOfValue() {
		if(getValue() == null){
			return super.contentOfValue();
		}
		return NetworkUtil.formatMac((byte[]) getValue(), ":");		
	}

}
