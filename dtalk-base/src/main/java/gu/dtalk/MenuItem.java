package gu.dtalk;
import static com.google.common.base.Preconditions.checkArgument;
import static gu.dtalk.CommonUtils.*;

import java.util.Collections;
import java.util.Map;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;

import gu.dtalk.exception.CmdExecutionException;
import gu.dtalk.exception.UnsupportCmdException;

public class MenuItem extends BaseItem {
	public MenuItem() {
		items.clear();
		CmdItem back = makeBack();
		// 为菜单项添加返回项
		items.put(back.getName(), back);
	}

	@Override
	public final boolean isContainer() {
		return true;
	}

	@Override
	public final ItemType getCatalog() {
		return ItemType.MENU;
	}
	/**
	 * 执行cmdpath指定的命令
	 * @param cmdpath
	 * @param parameters 命令参数,参数名-参数值(json格式),没有参数，可以输入{@code null}或空
	 * @return 返回值，没有返回值则返回null
	 * @throws UnsupportCmdException 
	 * @throws CmdExecutionException 
	 */
	public Object runCmd(String cmdpath,Map<String, ?> parameters) throws UnsupportCmdException, CmdExecutionException{
		checkArgument(!Strings.isNullOrEmpty(cmdpath),"cmd's path is null or empty");
		parameters = MoreObjects.firstNonNull(parameters,Collections.<String, Object>emptyMap());
		CmdItem cmd = findCmd(cmdpath);
		if(cmd == null){
			throw new UnsupportCmdException(cmdpath + "is not a cmd item" );
		}
		return cmd.runCmd(parameters);
	}
}
