package com.lewis.plugins.bill;

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
import net.mamoe.mirai.event.events.*;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 插件主体
 *
 * @author Lewis
 */
@Slf4j
public class BillPluginMain extends JavaPlugin {

	/**
	 * 静态初始化单例class，必须public static，并且必须命名为INSTANCE
	 */
	public static final BillPluginMain INSTANCE = new BillPluginMain();

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
				log.info("[ 记账小先锋 ]主人是第一次来的吧，小的先给您创建默认的设置，记得手动改下哦~~~~~~(*╹▽╹*)");
				break;
			case 2:
				log.info("[ 记账小先锋 ]恭迎主人回家鸭~~~~~~(*╹▽╹*)");
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
				EXECUTOR_SERVICE.execute(() -> {
					msgHandle(event);
				});
			}

			/**
			 * 陌生人消息处理
			 * @param event
			 */
			@EventHandler
			public void handleMessage(StrangerMessageEvent event) {
				EXECUTOR_SERVICE.execute(() -> {
					msgHandle(event);
				});
			}

			/**
			 * 群临时消息事件处理
			 * @param event
			 */
			@EventHandler
			public void handleMessage(GroupTempMessageEvent event) {
				EXECUTOR_SERVICE.execute(() -> {
					msgHandle(event);
				});
			}

			/**
			 * 好友添加请求事件处理
			 * @param event
			 */
			@EventHandler
			public void handleMessage(NewFriendRequestEvent event) {
				// 自动同意
				event.accept();
			}

			/**
			 * 邀请入群请求事件处理
			 * @param event
			 */
			@EventHandler
			public void handleMessage(BotInvitedJoinGroupRequestEvent event) {
				// 自动同意
				event.accept();
			}
		});
	}

	private BillPluginMain() {
		super(new JvmPluginDescriptionBuilder("com.lewis.plugins", "0.4")
				.name("[Lewis] 的插件 [ Lewis-qq398529803 ]")
				.info("[Lewis] 的插件 [ Lewis-qq398529803 ]\n" +
						"[Lewis] 的插件 [ Lewis-qq398529803 ]\n" +
						"[Lewis] 的插件 [ Lewis-qq398529803 ]\n" +
						"[Lewis] 的插件 [ Lewis-qq398529803 ]\n" +
						"[Lewis] 的插件 [ Lewis-qq398529803 ]\n")
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
		// 账单指令开关确认
		if (configJson.getString("billSwitch").equals("1")) {
			if (msg.startsWith("收入")) {
				String[] split = msg.split(" ");
				if (split.length != 4) {
					event.getSubject().sendMessage("输入的指令有误，请按照 “收入 金额 描述 账单id” 来告诉米兔w");
				} else {
					String billCode = split[3];
					String moneyTest = split[1];
					try {
						double money = Double.parseDouble(moneyTest);
						String remark = split[2];
						JSONObject jsonObject = new JSONObject();
						jsonObject.put("qq", event.getSender().getId());
						jsonObject.put("billType", 1);
						jsonObject.put("money", money);
						jsonObject.put("remark", remark);
						jsonObject.put("billCode", billCode);
						String json = HttpUtils.sendPostWithJson(configJson.getString("billAddUrl"), jsonObject);
						JSONObject object = JSONObject.parseObject(json);
						Integer code = object.getInteger("code");
						if (code == 200) {
							event.getSubject().sendMessage("记账完毕，要保持记账的好习惯哦~");
						} else {
							event.getSubject().sendMessage("记账失败，请联系我的主人398529803检查具体情况w");
						}
					} catch (Exception e) {
						event.getSubject().sendMessage("输入的指令有误（金额格式不对哦），请按照 “收入 金额 描述 账单id” 来告诉米兔w");
					}
				}
			} else if (msg.startsWith("支出")) {
				String[] split = msg.split(" ");
				if (split.length != 4) {
					event.getSubject().sendMessage("输入的指令有误，请按照 “支出 金额 描述 账单id” 来告诉米兔w");
				} else {
					String billCode = split[3];
					String moneyTest = split[1];
					try {
						double money = Double.parseDouble(moneyTest);
						String remark = split[2];
						JSONObject jsonObject = new JSONObject();
						jsonObject.put("qq", event.getSender().getId());
						jsonObject.put("billType", 2);
						jsonObject.put("billCode", billCode);
						jsonObject.put("money", money);
						jsonObject.put("remark", remark);
						String json = HttpUtils.sendPostWithJson(configJson.getString("billAddUrl"), jsonObject);
						JSONObject object = JSONObject.parseObject(json);
						Integer code = object.getInteger("code");
						if (code == 200) {
							event.getSubject().sendMessage("记账完毕，要保持记账的好习惯哦~");
						} else {
							event.getSubject().sendMessage("记账失败，请联系我的主人398529803检查具体情况w");
						}
					} catch (Exception e) {
						event.getSubject().sendMessage("输入的指令有误，请按照 “支出 金额 描述 账单id” 来告诉米兔w");
					}
				}
			} else if (msg.startsWith("删除明细")) {
				String[] split = msg.split(" ");
				if (split.length != 2) {
					event.getSubject().sendMessage("输入的指令有误，请按照 “删除明细 id” 来告诉米兔w");
				} else {
					String id = split[1];
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("id", id);
					jsonObject.put("qq", event.getSender().getId());
					String json = HttpUtils.sendPostWithJson(configJson.getString("billDeleteUrl"), jsonObject);
					JSONObject object = JSONObject.parseObject(json);
					Integer code = object.getInteger("code");
					if (code == 200) {
						event.getSubject().sendMessage("删除明细完毕，要保持记账的好习惯哦~");
					} else {
						event.getSubject().sendMessage("删除明细失败，请联系我的主人398529803检查具体情况w");
					}
				}
			} else if (msg.equals("我的账单")) {
				String param = "qq=" + event.getSender().getId();
				String s = HttpUtils.sendGet(configJson.getString("billCodeListUrl"), param);
				JSONObject jsonObject = JSONObject.parseObject(s);
				Integer code = jsonObject.getInteger("code");
				if (code != 200) {
					event.getSubject().sendMessage("查询失败，请联系我的主人398529803检查具体情况w");
				} else {
					JSONArray data = jsonObject.getJSONArray("data");
					At at = new At(event.getSender().getId());
					MessageChainBuilder builder = new MessageChainBuilder();
					builder.append(at).append("\n");
					if (data != null && data.size() != 0) {
						for (int i = 0; i < data.size(); i++) {
							JSONObject object = (JSONObject) data.get(i);
							if (i != 0 && i % 10 == 0) {
								event.getSubject().sendMessage(builder.build());
								builder.clear();
							} else {
								builder.append("\n");
							}
							builder.append(object.getInteger("billCode") + " ").append(object.getString("billName") + "");
							if (i == data.size() - 1) {
								event.getSubject().sendMessage(builder.build());
							}
						}
					} else {
						builder.append("您的账号并未创建账单w，请根据 “创建账单 账单名” 的指令告诉米兔要创建账单的名字w");
						event.getSubject().sendMessage(builder.build());
					}
				}
			} else if (msg.startsWith("创建账单")) {
				String[] split = msg.split(" ");
				if (split.length != 2) {
					event.getSubject().sendMessage("输入的指令有误，请按照 “创建账单 账单名” 来告诉米兔w");
				} else {
					String billName = split[1];
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("qq", event.getSender().getId());
					jsonObject.put("billName", billName);
					String json = HttpUtils.sendPostWithJson(configJson.getString("billCodeAddUrl"), jsonObject);
					JSONObject object = JSONObject.parseObject(json);
					Integer code = object.getInteger("code");
					if (code == 200) {
						event.getSubject().sendMessage("创建完毕，要保持记账的好习惯哦~");
					} else {
						event.getSubject().sendMessage("创建失败，请联系我的主人398529803检查具体情况w");
					}
				}
			} else if (msg.startsWith("删除账单")) {
				String[] split = msg.split(" ");
				if (split.length != 2) {
					event.getSubject().sendMessage("输入的指令有误，请按照 “删除账单 账单id” 来告诉米兔w");
				} else {
					String billCode = split[1];
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("qq", event.getSender().getId());
					jsonObject.put("billCode", billCode);
					String json = HttpUtils.sendPostWithJson(configJson.getString("billCodeDeleteUrl"), jsonObject);
					JSONObject object = JSONObject.parseObject(json);
					Integer code = object.getInteger("code");
					if (code == 200) {
						event.getSubject().sendMessage("删除完毕，要保持记账的好习惯哦~");
					} else {
						event.getSubject().sendMessage("删除失败，请联系我的主人398529803检查具体情况w");
					}
				}
			} else if (msg.startsWith("查询明细")) {
				String[] split = msg.split(" ");
				if (split.length != 2) {
					event.getSubject().sendMessage("输入的指令有误，请按照 “查询明细 账单id” 来告诉米兔w");
				} else {
					String billCode = split[1];
					String param = "qq=" + event.getSender().getId() + "&billCode=" + billCode;
					String s = HttpUtils.sendGet(configJson.getString("billListUrl"), param);
					JSONObject jsonObject = JSONObject.parseObject(s);
					Integer code = jsonObject.getInteger("code");
					if (code != 200) {
						event.getSubject().sendMessage("查询失败，请联系我的主人398529803检查具体情况w");
					} else {
						JSONObject data = (JSONObject) jsonObject.get("data");
						List<JSONObject> billList = (List<JSONObject>) data.get("billList");
						double incomeAll = data.getDouble("incomeAll");
						double payAll = data.getDouble("payAll");
						double balance = data.getDouble("balance");
						At at = new At(event.getSender().getId());
						MessageChainBuilder builder = new MessageChainBuilder();
						builder.append(at).append("\n");
						builder.append("总收入：").append(incomeAll + "").append("\n");
						builder.append("总支出：").append(payAll + "").append("\n");
						builder.append("总余额：").append(balance + "").append("\n");
						if (billList != null && billList.size() != 0) {
							for (int i = 0; i < billList.size(); i++) {
								JSONObject object = billList.get(i);
								if (i != 0 && i % 10 == 0) {
									event.getSubject().sendMessage(builder.build());
									builder.clear();
								} else {
									builder.append("\n");
								}
								builder.append(object.getLong("id") + " ").append(object.getInteger("billType") == 1 ? "收入 " : "支出 ").append(object.getDouble("money") + " ").append(object.getString("remark"));
								if (i == billList.size() - 1) {
									event.getSubject().sendMessage(builder.build());
								}
							}
						} else {
							event.getSubject().sendMessage("该账单下并没有明细哦，请选择账单添加明细w");
						}
					}
				}
			}
		}
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
						at = new At(setQq);
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
						try {
							Group group = (Group) event.getSubject();
							group.get(setQq).setSpecialTitle(headName);
						} catch (Exception e) {
							event.getSubject().sendMessage("不是群组哪来的头衔哦= =");
						}
					}
				}
			} else {
				event.getSubject().sendMessage("您没有权限执行该指令w");
			}
		}
	}
}