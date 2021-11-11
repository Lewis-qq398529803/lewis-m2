package com.lewis.plugins.title;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lewis.utils.FileUtils;
import com.lewis.utils.HttpUtils;
import com.lewis.utils.ReadPropertiesUtils;
import kotlin.coroutines.CoroutineContext;
import lombok.extern.slf4j.Slf4j;
import net.mamoe.mirai.console.plugin.jvm.JavaPlugin;
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescriptionBuilder;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.GlobalEventChannel;
import net.mamoe.mirai.event.SimpleListenerHost;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 插件主体
 *
 * @author Lewis
 */
@Slf4j
public class SetSpecialTitlePluginMain extends JavaPlugin {

	/**
	 * 静态初始化单例class，必须public static，并且必须命名为INSTANCE
	 */
	public static final SetSpecialTitlePluginMain INSTANCE = new SetSpecialTitlePluginMain();

	/**
	 * 统一配置
	 */
	public static JSONObject configJson = new JSONObject();

	/**
	 * 线程池
	 */
	public static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(10);

	/**
	 * 初始化配置文件
	 */
	public static int init() {
		// 初始化状态 1：第一次使用插件 2：多次使用插件
		int flag = 1;
		// 检查是否存在配置文件 config.json
		String configPath = System.getProperty("user.dir") + "/config/lewis-qq398529803/bill/config.json";
		File file = new File(configPath);
		if (file.exists()) {
			flag = 2;
			// 存在则直接读取
			configJson = FileUtils.jsonFile2Object(configPath);
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("authorization", configJson.getString("authorization"));
			String resultStr = HttpUtils.sendPostWithJson(ReadPropertiesUtils.getValue("base-url") + ReadPropertiesUtils.getValue("link-list-url"), jsonObject);
			if (!"".equals(resultStr)) {
				JSONObject linkListJson = JSONObject.parseObject(resultStr);
				List<JSONObject> linkList = (List<JSONObject>) linkListJson.get("data");
				for (JSONObject linkListItem : linkList) {
					String linkUrl = linkListItem.getString("linkUrl");
					switch (linkListItem.getString("urlName")) {
						case "billAddUrl":
							configJson.put("billAddUrl", linkUrl);
							break;
						case "billDeleteUrl":
							configJson.put("billDeleteUrl", linkUrl);
							break;
						case "billListUrl":
							configJson.put("billListUrl", linkUrl);
							break;
						case "billCodeListUrl":
							configJson.put("billCodeListUrl", linkUrl);
							break;
						case "billCodeAddUrl":
							configJson.put("billCodeAddUrl", linkUrl);
							break;
						case "billCodeDeleteUrl":
							configJson.put("billCodeDeleteUrl", linkUrl);
							break;
						default:
							break;
					}
				}
			}
		} else {
			// 不存在则创建默认
			try {
				file.getParentFile().mkdirs();
				file.createNewFile();
				JSONObject defaultConfig = new JSONObject();
				List<String> groupIdList = new ArrayList<>();
				defaultConfig.put("groupIdList", groupIdList);
				defaultConfig.put("authorization", "authorization");
				// 账单功能开关 0：关 1：开
				defaultConfig.put("billSwitch", "0");
				// 管理员qq（拥有所有权限）
				defaultConfig.put("administrator", "398529803");
				configJson = FileUtils.object2JsonFile(configPath, defaultConfig);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return flag;
	}

	/**
	 * 启动时自动调用的方法
	 */
	@Override
	public void onEnable() {
		// 初始化
		switch (init()) {
			case 1:
				log.info("[ 改头衔 ]主人是第一次来的吧，小的先给您创建默认的设置，记得手动改下哦~~~~~~(*╹▽╹*)");
				break;
			case 2:
				log.info("[ 改头衔 ]恭迎主人回家鸭~~~~~~(*╹▽╹*)");
				break;
			default:
				break;
		}

		GlobalEventChannel.INSTANCE.registerListenerHost(new SimpleListenerHost() {
			@Override
			public void handleException(@NotNull CoroutineContext context, @NotNull Throwable exception) {
				super.handleException(context, exception);
			}

			/**
			 * 群消息处理
			 * @param event
			 */
			@EventHandler
			public void handleMessage(GroupMessageEvent event) {
				EXECUTOR_SERVICE.execute(() -> {
					// 分群处理
					// 群号
					long groupId = event.getGroup().getId();
					JSONArray groupIdListTest = configJson.getJSONArray("groupIdList");
					List<Long> groupIdList = new ArrayList<>();
					if (groupIdListTest.size() != 0) {
						for (Object o : groupIdListTest) {
							long aLong = Long.parseLong(String.valueOf(o));
							groupIdList.add(aLong);
						}
						// 判断是否群号属于配置文件中的列表中 属于才运行操作
						if (groupIdList.contains(groupId)) {
							msgHandle(event);
						}
					}
				});
			}
		});
	}

	private SetSpecialTitlePluginMain() {
		super(new JvmPluginDescriptionBuilder("com.lewis.set.special.title", "0.1")
				.name("[Lewis] 的插件 [ 改头衔 ]")
				.info("[ Lewis-qq398529803 ]\n" +
						"[ Lewis-qq398529803 ]\n" +
						"[ Lewis-qq398529803 ]\n" +
						"[ Lewis-qq398529803 ]\n" +
						"[ Lewis-qq398529803 ]\n")
				.author("Lewis")
				.build());
	}

	/**
	 * 消息处理
	 *
	 * @param event 消息实体
	 */
	private void msgHandle(MessageEvent event) {

		// 获取消息文本
		String msg = event.getMessage().serializeToMiraiCode();

		// 根据指令调用指定url
		if (msg.startsWith("改头衔")) {
			// 确保有管理员权限
			if (String.valueOf(event.getSender().getId()).equals(configJson.getString("administrator"))) {
				// 获取at对象
				MessageChain message = event.getMessage();
				At at = null;
				long setQq = 0;
				for (Message message1 : message) {
					if (message1 instanceof At) {
						setQq = ((At) message1).getTarget();
						at = (At) message1;
						break;
					}
				}
				// 确保at存在
				if (at == null) {
					event.getSubject().sendMessage("指令格式错误，请按照 “改头衔@xxx 头衔名称” 来告诉米兔w，并且头衔名不允许超过六个字哈");
				} else {
					String[] split = msg.split(" ");
					if (split.length != 2) {
						event.getSubject().sendMessage("指令格式错误，请按照 “改头衔@xxx 头衔名称” 来告诉米兔w，并且头衔名不允许超过六个字哈");
					} else {
						String headName = split[1];
						Group group = (Group) event.getSubject();
						Objects.requireNonNull(group.get(setQq)).setSpecialTitle(headName);
						event.getSubject().sendMessage("头衔设置完毕w~~~~~");
					}
				}
			} else {
				event.getSubject().sendMessage("您没有权限执行该指令w");
			}
		}
	}
}