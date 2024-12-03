package gu.dtalk;

import com.alibaba.fastjson.TypeReference;

public class BoolOption extends BaseOption<Boolean> {

	public BoolOption() {
		super(new TypeReference<Boolean>() {}.getType());
	}
	@Override
	public final OptionType getType() {
		return OptionType.BOOL;
	}
}
