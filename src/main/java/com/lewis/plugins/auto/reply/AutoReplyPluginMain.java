package com.lewis.plugins.auto.reply;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lewis.utils.FileUtils;
import com.lewis.utils.HttpUtils;
import com.lewis.utils.ReadPropertiesUtils;
import kotlin.coroutines.CoroutineContext;
import lombok.extern.slf4j.Slf4j;
import net.mamoe.mirai.console.plugin.jvm.JavaPlugin;
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescriptionBuilder;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.GlobalEventChannel;
import net.mamoe.mirai.event.SimpleListenerHost;
import net.mamoe.mirai.event.events.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 插件主体 - 自动回复
 *
 * @author Lewis
 */
@Slf4j
public class AutoReplyPluginMain extends JavaPlugin {

	/**
	 * 静态初始化单例class，必须public static，并且必须命名为INSTANCE
	 */
	public static final AutoReplyPluginMain INSTANCE = new AutoReplyPluginMain();

	/**
	 * 统一配置
	 */
	public static JSONObject configJson = new JSONObject();

	/**
	 * 线程池
	 */
	public static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(10);

	/**
	 * 开关 - 关
	 */
	private final String SWITCH_OFF = "0";

	/**
	 * 开关 - 开
	 */
	private final String SWITCH_ON = "1";

	/**
	 * 初始化配置文件
	 */
	public static int init() {
		// 初始化状态 1：第一次使用插件 2：多次使用插件
		int flag = 1;
		// 检查是否存在配置文件 config.json
		String configPath = System.getProperty("user.dir") + "/config/lewis-qq398529803/auto/reply/config.json";
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
						case "autoReplyAdd":
							configJson.put("autoReplyAddUrl", linkUrl);
							break;
						case "autoReplyList":
							configJson.put("autoReplyListUrl", linkUrl);
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
				// 分群列表
				defaultConfig.put("groupIdList", groupIdList);
				// 密钥
				defaultConfig.put("authorization", "");
				// 自动回复开关 0：关 1：开
				defaultConfig.put("autoReplySwitch", "0");
				// 私聊开关 0：关 1：开
				defaultConfig.put("privateSwitch", "0");
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
				log.info("[ 牛头不对马嘴型自动回复 ]主人是第一次来的吧，小的先给您创建默认的设置，记得手动改下哦~~~~~~(*╹▽╹*)");
				break;
			case 2:
				log.info("[ 牛头不对马嘴型自动回复 ]恭迎主人回家鸭~~~~~~(*╹▽╹*)");
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

			/**
			 * 好友消息处理
			 * @param event
			 */
			@EventHandler
			public void handleMessage(FriendMessageEvent event) {
				if (SWITCH_ON.equals(configJson.getString("privateSwitch"))) {
					EXECUTOR_SERVICE.execute(() -> {
						msgHandle(event);
					});
				}
			}

			/**
			 * 陌生人消息处理
			 * @param event
			 */
			@EventHandler
			public void handleMessage(StrangerMessageEvent event) {
				if (SWITCH_ON.equals(configJson.getString("privateSwitch"))) {
					EXECUTOR_SERVICE.execute(() -> {
						msgHandle(event);
					});
				}
			}

			/**
			 * 群临时消息事件处理
			 * @param event
			 */
			@EventHandler
			public void handleMessage(GroupTempMessageEvent event) {
				if (SWITCH_ON.equals(configJson.getString("privateSwitch"))) {
					EXECUTOR_SERVICE.execute(() -> {
						msgHandle(event);
					});
				}
			}
		});
	}

	private AutoReplyPluginMain() {
		super(new JvmPluginDescriptionBuilder("com.lewis.plugins.auto.reply", "0.1")
				.name("[ Lewis ] 的插件 [ 牛头不对马嘴型自动回复插件 ]")
				.info("[ Lewis-qq398529803 ]\n" +
						"[ Lewis-qq398529803 ]\n" +
						"[ Lewis-qq398529803 ]\n" +
						"[ Lewis-qq398529803 ]\n" +
						"[ Lewis-qq398529803 ]\n")
				.author("Lewis")
				.build());
	}

	String key = "";
	int flag = 1;

	/**
	 * 存库
	 *
	 * @param event
	 */
	private void addCommand(MessageEvent event, String autoReplyAddUrl) {
		// 获取消息文本 - 去除空格
		String msg = event.getMessage().serializeToMiraiCode().trim();
		// qq号
		long qq = event.getSender().getId();

		// 存词库
		// 排除存在类似于 [mirai: 字符串的内容
		if (!msg.contains("[mirai:")) {
			if (flag == 1) {
				key = msg;
				flag++;
			} else {
				// 第二次之后放入数据库 并且重置flag
				addOne(qq, key, msg, autoReplyAddUrl);
				flag = 1;
			}
		}
	}

	/**
	 * 消息处理
	 *
	 * @param event 消息实体
	 */
	private void msgHandle(MessageEvent event) {

		addCommand(event, configJson.getString("autoReplyAddUrl"));

		// 获取消息文本
		String msg = event.getMessage().serializeToMiraiCode();

		// 根据指令调用指定url
		// 若开启自动回复功能
		if (configJson.getString("autoReplySwitch").equals(SWITCH_ON)) {
			// 若存在回复词，则返回对应的回复值
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("command", msg);
			String resultJson = HttpUtils.sendPostWithJson(configJson.getString("autoReplyListUrl"), jsonObject);
			JSONObject json = JSONObject.parseObject(resultJson);
			JSONArray data = json.getJSONArray("data");
			if (data != null && data.size() != 0) {
				List<JSONObject> result = (List<JSONObject>) json.get("data");
				String answer = getRandom(result).getString("result");
				event.getSubject().sendMessage(answer);
			}
		}
	}

	/**
	 * 新增一个指令
	 *
	 * @param qq
	 * @param key
	 * @param value
	 * @return
	 */
	private String addOne(long qq, String key, String value, String autoReplyAddUrl) {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("command", key);
		jsonObject.put("result", value);
		jsonObject.put("forQqCreate", qq);
		String responseJsonStr = HttpUtils.sendPostWithJson(autoReplyAddUrl, jsonObject);
		if (responseJsonStr != null) {
			JSONObject responseJson = JSONObject.parseObject(responseJsonStr);
			Integer code = responseJson.getInteger("code");
			return code == 200 ? "我学会啦w~~~" : "我不太听的懂诶";
		}
		return "主人SAMA的服务器好像没理我诶，请联系我主人理我一下w QAQ";
	}

	/**
	 * 随机从list中获取一个jsonobject元素
	 *
	 * @param jsonObjectList
	 * @return
	 */
	private JSONObject getRandom(List<JSONObject> jsonObjectList) {
		Random random = new Random();
		int n = random.nextInt(jsonObjectList.size());
		return jsonObjectList.get(n);
	}
}