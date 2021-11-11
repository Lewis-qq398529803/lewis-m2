package com.lewis;

import com.lewis.utils.ReadPropertiesUtils;
import com.lewis.plugins.auto.accept.AutoAcceptPluginMain;
import com.lewis.plugins.auto.reply.AutoReplyPluginMain;
import com.lewis.plugins.bill.BillPluginMain;
import com.lewis.plugins.title.SetSpecialTitlePluginMain;
import lombok.extern.slf4j.Slf4j;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.BotFactory;
import net.mamoe.mirai.console.extension.PluginComponentStorage;
import net.mamoe.mirai.console.terminal.MiraiConsoleImplementationTerminal;
import net.mamoe.mirai.console.terminal.MiraiConsoleTerminalLoader;
import net.mamoe.mirai.utils.BotConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.util.Objects;

/**
 * 应用程序入口
 *
 * @author Lewis
 */
@Slf4j
@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		// 初始化配置文件
		initProperties();
		// 启动springboot
		SpringApplication.run(Application.class, args);
		System.out.println("(♥◠‿◠)ﾉﾞ  Lewis系统启动成功   ლ(´ڡ`ლ)ﾞ\n");
		// 启动时 删除缓存 减少 程序启动后堵塞无法登录的情况
		deleteCache();
		// 启动console 加载插件
		MiraiConsoleTerminalLoader.INSTANCE.startAsDaemon(new MiraiConsoleImplementationTerminal());
		// 实例化机器人自带插件
		initConsole();
		// 创建 Bot 机器人实体
		Bot bot = BotFactory.INSTANCE.newBot(Long.parseLong(ReadPropertiesUtils.getValue("qq")), ReadPropertiesUtils.getValue("password"), getBotConfig());
		// 登录
		bot.login();
	}

	private static void initProperties() {
		// 检测是否存在配置文件
		if (ReadPropertiesUtils.init(System.getProperty("user.dir") + "\\config\\config.properties") < 0) {
			log.error("读取配置文件config.properties失败，请确认运行目录下有该文件");
			System.exit(0);
		}
	}

	/**
	 * 初始化机器人自带功能插件
	 */
	private static void initConsole() {
		// 账单
		PluginComponentStorage storageBillPluginMain = new PluginComponentStorage(BillPluginMain.INSTANCE);
		BillPluginMain.INSTANCE.onLoad(storageBillPluginMain);
		BillPluginMain.INSTANCE.onEnable();
		// 自动回复
		PluginComponentStorage storageAutoReplyPluginMain = new PluginComponentStorage(AutoReplyPluginMain.INSTANCE);
		AutoReplyPluginMain.INSTANCE.onLoad(storageAutoReplyPluginMain);
		AutoReplyPluginMain.INSTANCE.onEnable();
		// 自动同意
		PluginComponentStorage storageAutoAcceptPluginMain = new PluginComponentStorage(AutoAcceptPluginMain.INSTANCE);
		AutoAcceptPluginMain.INSTANCE.onLoad(storageAutoAcceptPluginMain);
		AutoAcceptPluginMain.INSTANCE.onEnable();
		// 改头衔
		PluginComponentStorage storageSetSpecialTitlePluginMain = new PluginComponentStorage(SetSpecialTitlePluginMain.INSTANCE);
		SetSpecialTitlePluginMain.INSTANCE.onLoad(storageSetSpecialTitlePluginMain);
		SetSpecialTitlePluginMain.INSTANCE.onEnable();
	}

	/**
	 * 机器人默认设置
	 *
	 * @return BotConfiguration
	 */
	private static BotConfiguration getBotConfig() {
		BotConfiguration botConfiguration = new BotConfiguration();
		// 登录协议
		botConfiguration.setProtocol(BotConfiguration.MiraiProtocol.ANDROID_PHONE);
		// 心跳协议
		botConfiguration.setHeartbeatStrategy(BotConfiguration.HeartbeatStrategy.STAT_HB);
		// 设置 cache 目录
		botConfiguration.setCacheDir(new File("./cache"));
		// 设置 device
		botConfiguration.fileBasedDeviceInfo("./device/device-" + Long.parseLong(ReadPropertiesUtils.getValue("qq")) + ".json");
		// 设置是否掉线重登录
		botConfiguration.setAutoReconnectOnForceOffline(Boolean.parseBoolean(ReadPropertiesUtils.getValue("auto-re-login")));
		return botConfiguration;
	}

	/**
	 * 删除缓存
	 */
	private static void deleteCache() {
		try {
			File file = new File("./cache");
			if (file.exists()) {
				for (File f : Objects.requireNonNull(file.listFiles())) {
					f.deleteOnExit();
				}
				file.deleteOnExit();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
