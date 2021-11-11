package com.lewis.plugins.auto.accept;

import com.alibaba.fastjson.JSONObject;
import com.lewis.utils.FileUtils;
import kotlin.coroutines.CoroutineContext;
import lombok.extern.slf4j.Slf4j;
import net.mamoe.mirai.console.plugin.jvm.JavaPlugin;
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescriptionBuilder;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.GlobalEventChannel;
import net.mamoe.mirai.event.SimpleListenerHost;
import net.mamoe.mirai.event.events.BotInvitedJoinGroupRequestEvent;
import net.mamoe.mirai.event.events.NewFriendRequestEvent;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

/**
 * 插件主体
 *
 * @author Lewis
 */
@Slf4j
public class AutoAcceptPluginMain extends JavaPlugin {

	/**
	 * 静态初始化单例class，必须public static，并且必须命名为INSTANCE
	 */
	public static final AutoAcceptPluginMain INSTANCE = new AutoAcceptPluginMain();

	/**
	 * 统一配置
	 */
	public static JSONObject configJson = new JSONObject();

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
		} else {
			// 不存在则创建默认
			try {
				file.getParentFile().mkdirs();
				file.createNewFile();
				JSONObject defaultConfig = new JSONObject();
				defaultConfig.put("acceptSwitch", "0");
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
				log.info("[ 自动同意好友添加请求和群邀请请求 ]主人是第一次来的吧，小的先给您创建默认的设置，记得手动改下哦~~~~~~(*╹▽╹*)");
				break;
			case 2:
				log.info("[ 自动同意好友添加请求和群邀请请求 ]恭迎主人回家鸭~~~~~~(*╹▽╹*)");
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
			 * 好友添加请求事件处理
			 * @param event
			 */
			@EventHandler
			public void handleMessage(NewFriendRequestEvent event) {
				// 判断开关
				if ("1".equals(configJson.getString("acceptSwitch"))) {
					// 自动同意
					event.accept();
				}
			}

			/**
			 * 邀请入群请求事件处理
			 * @param event
			 */
			@EventHandler
			public void handleMessage(BotInvitedJoinGroupRequestEvent event) {
				// 判断开关
				if ("1".equals(configJson.getString("acceptSwitch"))) {
					// 自动同意
					event.accept();
				}
			}
		});
	}

	private AutoAcceptPluginMain() {
		super(new JvmPluginDescriptionBuilder("com.lewis.auto.accept", "0.1")
				.name("[Lewis] 的插件 [ Lewis-qq398529803 ]")
				.info("[Lewis] 的插件 [ Lewis-qq398529803 ]\n" +
						"[Lewis] 的插件 [ Lewis-qq398529803 ]\n" +
						"[Lewis] 的插件 [ Lewis-qq398529803 ]\n" +
						"[Lewis] 的插件 [ Lewis-qq398529803 ]\n" +
						"[Lewis] 的插件 [ Lewis-qq398529803 ]\n")
				.author("Lewis")
				.build());
	}
}