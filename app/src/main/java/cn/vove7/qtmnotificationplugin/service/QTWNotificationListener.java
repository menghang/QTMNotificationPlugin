package cn.vove7.qtmnotificationplugin.service;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.widget.Toast;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.vove7.qtmnotificationplugin.R;
import cn.vove7.qtmnotificationplugin.utils.MyApplication;
import cn.vove7.qtmnotificationplugin.utils.SQLOperator;
import cn.vove7.qtmnotificationplugin.utils.SettingsHelper;
import cn.vove7.qtmnotificationplugin.utils.Utils;

import static android.os.Build.VERSION_CODES.M;

public class QTWNotificationListener extends NotificationListenerService {
   private final String TAG = getClass().getName();
   public static boolean isConnect = false;
   private int notifyNumQQ = 0;
   private int notifyNumWechat = 0;

   public static final String PACKAGE_QQ = "com.tencent.mobileqq";
   public static final String PACKAGE_QQ_NOTIF = "com.inklin.qqnotfandshare";//QQ通知增强
   public static final String PACKAGE_QQ_I = "com.tencent.mobileqqi";
   public static final String PACKAGE_TIM = "com.tencent.tim";

   public static final String PACKAGE_MM = "com.tencent.mm";
   public static final String PACKAGE_WECHAT_NOTIF = "me.zhanghai.android.wechatnotificationtweaks2";//WECHAT通知增强

   private void registerScreenOnReceiver() {
      IntentFilter mScreenOnFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
      registerReceiver(mScreenOReceiver, mScreenOnFilter);
   }

   private void unregisterScreenOnReceiver() {
      unregisterReceiver(mScreenOReceiver);
   }

   @Override
   public void onDestroy() {
      unregisterReceiver(mScreenOReceiver);
      super.onDestroy();
   }

   private BroadcastReceiver mScreenOReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
         String action = intent.getAction();

         if (Intent.ACTION_SCREEN_ON.equals(action)) {
            Log.d("QTWNotification: ", "onReceive  ----> " + "—— SCREEN_ON ——");
            notifyNumQQ = 0;
            notifyNumWechat = 0;
         }
      }
   };

   @Override
   public void onListenerConnected() {
      super.onListenerConnected();
      isConnect = true;
      MyApplication.getInstance().setQTWNotificationListener(this);
      //初始化配置
      SettingsHelper.initPreference(this);

      Log.d(TAG, "QTM提醒已上线");
      if (Build.VERSION.SDK_INT < M)
         Looper.prepare();
      registerScreenOnReceiver();
      Toast.makeText(this, R.string.qtm_online, Toast.LENGTH_SHORT).show();
   }

   @Override
   public void onListenerDisconnected() {
      super.onListenerDisconnected();
      unregisterScreenOnReceiver();
      Log.d(TAG, "QTM提醒已下线");
      isConnect = false;
   }

   public static final String TYPE_QQ_TIM = "QQ/TIM";
   public static final String TYPE_WECHAT = "WECHAT";

   private static final int TYPE_QQ_DIAL = 510;
   private static final int TYPE_QQ_WEB_DL = 868;
   private static final int TYPE_QQ_OK = 102;
   private static final int TYPE_QQ_BACKGROUND = 488;
   private static final int TYPE_QQ_ZONE = 76;
   private static final int TYPE_QQ_PC_LOGIN = 520;
   private static final int TYPE_QQ_RELEVANCE = 444;
   private static final int TYPE_QQ_HELLO = 678;
   private static final int TYPE_QQ_PUBLIC = 595;
   private static final int TYPE_QQ_VERIFY_LOGIN = 191;
   private static final int TYPE_QQ_EMAIL = 859;
   private static final int TYPE_QQ_FRIEND = 373;
   private static final int TYPE_QQ_ALL_MEMBER_MSG = 723;

   long lastTimeQQ = 0;//上次通知post时间
   long lastTimeW = 0;

   boolean isQQNotify = false;
   boolean isWechatNotify = false;

   @Override
   public void onNotificationRemoved(StatusBarNotification sbn) {
      Log.d("Vove :", "onNotificationRemoved  ----> " + sbn.getPackageName());
      long removeTime = System.currentTimeMillis();
      switch (sbn.getPackageName()) {
         case PACKAGE_QQ_I:
         case PACKAGE_QQ_NOTIF: {
            Log.d("Vove :", "onNotificationRemoved  ----> QQ增强读取" + removeTime);
            notifyNumQQ = 0;
         }
         case PACKAGE_QQ://QQ
         case PACKAGE_TIM: //TIM
            new Handler().postDelayed(() -> {//
               Log.d("Vove :", "onNotificationRemoved  ----> " + removeTime + " - " + lastTimeQQ);
               if (!isQQNotify && lastTimeQQ < removeTime) {
                  Log.d("Vove :", "onNotificationRemoved  ----> 读取");
                  notifyNumQQ = 0;
               } else {
                  Log.d("Vove :", "onNotificationRemoved  ----> 新消息");
                  isQQNotify = false;
               }
            }, 300);
            break;

         case PACKAGE_WECHAT_NOTIF: { //MM
            Log.d("Vove :", "onNotificationRemoved  ----> Wechat增强读取" + removeTime);
            notifyNumWechat = 0;
         }
         case PACKAGE_MM: //MM
            new Handler().postDelayed(() -> {
               if (!isWechatNotify && lastTimeW < removeTime) {
                  Log.d("Vove :", "onNotificationRemoved  ----> 读取");
                  notifyNumWechat = 0;
               } else {
                  Log.d("Vove :", "onNotificationRemoved  ----> 新消息");
                  isWechatNotify = false;
               }
            }, 300);
            break;
      }
   }

   @Override
   public void onNotificationPosted(StatusBarNotification sbn) {
      if (!SettingsHelper.getTotalSwitch()) {
         return;
      }
      Log.d("V :", "\n获得通知 PackageName --- " + sbn.getPackageName());
      Notification notification = sbn.getNotification();
      Bundle extras = notification.extras;
      String notificationTitle = extras.getString(Notification.EXTRA_TITLE);
      String notificationText = extras.getString(Notification.EXTRA_TEXT);

      Log.d(TAG, "标题：" + notificationTitle +
              "\n内容：" + notificationText + "\n");

      distributePkg(sbn.getPackageName(), notificationTitle, notificationText);//
      //Log.d("isOnGoing", "" + sbn.isOngoing());
   }

   /**
    * 根据包名分开处理
    *
    * @param packageName 包名索引
    * @param title       通知标题
    * @param content     通知内容
    */
   private void distributePkg(String packageName, String title, String content) {

      switch (packageName) {
         case PACKAGE_QQ_NOTIF:
            isQQNotify = true;
            break;
         case PACKAGE_QQ://QQ
            //notifyQQOrTim(true, title, content);
            //break;
         case PACKAGE_QQ_I:
         case PACKAGE_TIM: //TIM
            lastTimeQQ = System.currentTimeMillis();
            notifyQQOrTim(title, content);
            break;

         case PACKAGE_WECHAT_NOTIF: //MM
            isWechatNotify = true;
            break;
         case PACKAGE_MM: //MM
            lastTimeW = System.currentTimeMillis();
            notifyWechat(title, content);
            break;
         default:
      }
   }

   private static final String MODE_DEFAULT = "default";
   private static final String MODE_ONLY_FA = "only_fa";

   private void notifyQQOrTim(String title, String content) {
      boolean totalSwitch = SettingsHelper.getTotalSwitchQQ();
      int notificationType = parseQQNotificationType(title, content);
      if (!totalSwitch || notificationType == TYPE_QQ_WEB_DL) {
         return;
      }
      //获得好友昵称
      String nickname = getQQNickname(title, content);
      Log.d(TAG, "parseQQNotificationType: 昵称： " + nickname);

      //消息类型
      String mode = SettingsHelper.getModeQQ();
      //仅熄屏
      if ((SettingsHelper.isOnlyScreenOffQQ())
              && Utils.isScreenOn(this)) {
         Log.d(TAG, "notifyQQOrTim: screen on");
         return;
      }
      //blacklist
      Set<String> blackList = SettingsHelper.getBlackListQQ();
      if (blackList.contains(nickname)) {
         Log.d(TAG, "notifyQQOrTim: in black list: " + nickname);
         return;
      }
      //免打扰
      boolean isNoDistrubingQuantum = SettingsHelper.isNoDistrubingOnQQ();
      String[] s = SettingsHelper.getNoDistrubingTimeQuantumQQ().split("-");
      String begin = s[0];
      String end = s[1];

      boolean isFa = isFaQQ(nickname);
      if (isNoDistrubingQuantum && Utils.inTimeQuantum(begin, end, null)) {
         Log.d(TAG, "notifyQQOrTim: no distrubing time quantum");
         boolean isFaOnND = SettingsHelper.isOpenFaOnNDQQ();
         if (!(isFaOnND && isFa)) {//没打开 或者打开不在特别关心中
            Log.d(TAG, "notifyQQOrTim: 免打扰 或者不在特别关心中");
            return;
         }
      }
      String ringtone = SettingsHelper.getRingtoneQQ();
      Log.d("QTWNotificationList :", "notifyQQOrTim fa ----> " + isFa);
      if (isFa)
         ringtone = SettingsHelper.getFaRingtoneQQ();
      switch (mode) {
         case MODE_ONLY_FA: {
            if (!isFa) {//不是特别关心
               Log.d(TAG, "notifyQQOrTim: 特别关心 no");
               return;
            }
            Log.d(this.getClass().getName(), "notifyQQOrTim: 特别关心 yes" + nickname);
         }
         case MODE_DEFAULT: {//默认
            switch (notificationType) {
               case TYPE_QQ_DIAL:
                  return;
               case TYPE_QQ_BACKGROUND:
               case TYPE_QQ_PC_LOGIN:
                  return;
               case TYPE_QQ_ALL_MEMBER_MSG:
                  Log.d("QTWNotification :", "notifyQQOrTim  ----> 全体消息 ");
                  if (!SettingsHelper.notifyAllMsgQQ()) {
                     Log.d("QTWNotification :", "notifyQQOrTim  ----> 不通知");
                     return;
                  }
                  Log.d("QTWNotification :", "notifyQQOrTim  ----> 通知");
               case TYPE_QQ_OK:
                  new SQLOperator(this).insertNickname(nickname, TYPE_QQ_TIM);
               case TYPE_QQ_RELEVANCE:
                  sendNotifyQQ(ringtone);
                  break;
               case TYPE_QQ_ZONE:
                  if (!SettingsHelper.getBoolean(R.string.key_notify_qq_zone, true)) {
                     Log.d("Vove :", "notifyQQOrTim  ----> QQ Zone no");
                     return;
                  }
               case TYPE_QQ_EMAIL:
                  sendNotifyQQ(ringtone);
                  break;
               case TYPE_QQ_PUBLIC:
               case TYPE_QQ_VERIFY_LOGIN:
               case TYPE_QQ_HELLO: {
                  return;
               }
            }
         }
         break;
      }
   }

   private static long lastNotifyTime = 0;

   private void sendNotifyQQ(String ringtone) {
      Log.d("Vove :", "notifyQQOrTim  ----> 通知提醒 begin");
      long now = System.currentTimeMillis();
      if (now - lastNotifyTime < 500) {
         lastNotifyTime = now;
         Log.d("Vove :", "notifyQQOrTim  ----> 防止两条消息重叠 ");
         return;
      }
      lastNotifyTime = now;

      if (notifyNumQQ >= SettingsHelper.getInt(R.string.key_max_msg_num_qq, 99)) {
         Log.d("Vove :", "notifyQQOrTim 达到最大通知次数 ----> " + notifyNumQQ);
         return;
      }
      notifyNumQQ++;
      boolean isVibrator = SettingsHelper.isVibratorQQ();
      int vibratorStrength = SettingsHelper.getVibratorStrengthQQ();
      int repeatNum = SettingsHelper.getRepeatNumQQ();
      boolean isAlarm = SettingsHelper.isAlarmQQ();
      new Handler().postDelayed(() -> {
         if (isVibrator)
            Utils.notificationVibrator(QTWNotificationListener.this,
                    vibratorStrength, repeatNum);
         if (isAlarm)
            Utils.startAlarm(QTWNotificationListener.this, ringtone);
      }, 100);//增强开启分组不提醒

   }

   private boolean isFaQQ(String nickname) {
      Set<String> faSet = SettingsHelper.getFaSetQQ();
      return faSet.contains(nickname);
   }

   private boolean isFaWechat(String nickname) {
      Set<String> faSet = SettingsHelper.getFaSetWechat();
      return faSet.contains(nickname);
   }

   private void notifyWechat(String title, String content) {
      boolean totalSwitch = SettingsHelper.getTotalSwitchWechat();
      int notificationType = parseWechatNotificationType(title, content);

      if (!totalSwitch) {
         return;
      }

      //消息类型
      String mode = SettingsHelper.getModeWechat();
      //仅熄屏
      if ((SettingsHelper.isOnlyScreenOffWechat())
              && Utils.isScreenOn(this)) {
         Log.d(TAG, "notifyWechat: screen on");
         return;
      }
      String ringtone = SettingsHelper.getRingtoneWechat();
      String nickname = title;
      boolean isFa = isFaWechat(nickname);
      switch (mode) {
         case MODE_ONLY_FA: {
            if (!isFa) {//不是特别关心
               Log.d(TAG, "notifyWechat: not fa ");
               return;
            }
         }
         case MODE_DEFAULT:
            switch (notificationType) {
               case TYPE_WECHAT_DIALOG:
               case TYPE_WECHAT_PAL: {
                  return;
               }
               case TYPE_WECHAT_OK: {
                  //blacklist
                  Set<String> blackList = SettingsHelper.getBlackListWechat();
                  if (blackList.contains(nickname)) {
                     Log.d(TAG, "notifyWechat: in black list: " + nickname);
                     return;
                  }
                  //免打扰
                  boolean isNoDistrubingQuantum = SettingsHelper.isNoDistrubingOnWechat();
                  String[] s = SettingsHelper.getNoDistrubingTimeQuantumWechat().split("-");
                  String begin = s[0];
                  String end = s[1];

                  if (isNoDistrubingQuantum && Utils.inTimeQuantum(begin, end, null)) {
                     Log.d(TAG, "notifyWechat: no distrubing time quantum");
                     boolean isFaOnND = SettingsHelper.isOpenFaOnNDWechat();
                     if (!(isFaOnND && isFaWechat(nickname))) {//没打开 或者打开不在特别关心中
                        Log.d(TAG, "notifyWechat: 没打开 或者不在特别关心中");
                        return;
                     }
                  }
                  new SQLOperator(this).insertNickname(title, TYPE_WECHAT);
                  sendNotifyWechat(ringtone);
               }
            }
      }
   }

   private void sendNotifyWechat(String ringtone) {
      long now = System.currentTimeMillis();
      if (now - lastNotifyTime < 500) {
         lastNotifyTime = now;
         Log.d("Vove :", "notifyWechat  ----> 防止两条消息重叠 ");
         return;
      }
      lastNotifyTime = now;
      if (notifyNumWechat >= SettingsHelper.getInt(R.string.key_max_msg_num_wechat, 999)) {
         Log.d("Vove :", "notifyWechat  ----> 达到最大通知次数");
         return;
      }
      notifyNumWechat++;

      boolean isVibrator = SettingsHelper.isVibratorWechat();
      int vibratorStrength = SettingsHelper.getVibratorStrengthWechat();
      int repeatNum = SettingsHelper.getRepeatNumWechat();
      boolean isAlarm = SettingsHelper.isAlarmWechat();
      if (isVibrator)
         Utils.notificationVibrator(this,
                 vibratorStrength,
                 repeatNum);
      if (isAlarm)
         Utils.startAlarm(this, ringtone);
   }

   /**
    * 解析QQ通知
    *
    * @param title   标题
    * @param content 内容
    * @return 类型
    */
   private int parseQQNotificationType(String title, String content) {
      //QQ后台语音通话 or 后台提示
      if (title == null) {
         if (content == null)
            return TYPE_QQ_WEB_DL;
         return TYPE_QQ_DIAL;
      }
      Matcher matcher = Pattern.compile("QQ空间动态(\\(共(\\d+)条未读\\))?$").matcher(title);
      if (matcher.find()) {
         Log.d(TAG, "parseQQNotificationType: QQ空间");
         return TYPE_QQ_ZONE;
      }
      if (content.contains("正在后台运行")) {
         return TYPE_QQ_BACKGROUND;
      }
      switch (title) {
         case "你的帐号在电脑登录":
            return TYPE_QQ_PC_LOGIN;
         case "一键验证":
            return TYPE_QQ_VERIFY_LOGIN;
         case "关联QQ号":
            return TYPE_QQ_RELEVANCE;
         case "一声问候":
            return TYPE_QQ_HELLO;
         case "邮件提醒":
            return TYPE_QQ_EMAIL;
         case "公众号":
            return TYPE_QQ_PUBLIC;
         case "朋友通知":
            return TYPE_QQ_FRIEND;
         //QQI
         case "Qzone":
            if (content.contains("commented on your post") ||
                    content.contains("leave you a message")) {
               return TYPE_QQ_ZONE;
            }
      }
      if (!content.startsWith("@全体成员") && content.contains("@全体成员")) {
         return TYPE_QQ_ALL_MEMBER_MSG;
      }

      //好友-群-组..
      Log.d(TAG, "parseQQNotificationType: 普通消息");
      return TYPE_QQ_OK;
   }

   /**
    * 获取昵称
    *
    * @param title   标题
    * @param content 内容
    * @return 返回昵称
    */
   private String getQQNickname(String title, String content) {
      if (title == null) {
         return null;
      }
      String regexMessageNum = "\\((\\d+)条(以上)?新消息\\)$";
      Matcher matcherNum = Pattern.compile(regexMessageNum).matcher(title);

      String regexMsg = "有\\s(\\d+)\\s个联系人给你发过来(\\d+)条新消息";
      Matcher matcherQQMultiPeo = Pattern.compile(regexMsg).matcher(content);

      if (matcherNum.find())
         return title.substring(0, matcherNum.start() - 1);
      else if ("QQ".equals(title) || "TIM".equals(title) && matcherQQMultiPeo.find()) {
         return null;
      }
      return title;
   }

   /**
    * 解析Wechat通知
    *
    * @param title   标题
    * @param content 内容
    * @return 类型
    */
   private int parseWechatNotificationType(String title, String content) {
      switch (content) {
         case "语音通话中,轻击以继续":
            return TYPE_WECHAT_DIALOG;
      }
      switch (title) {
         case "微信支付":
            return TYPE_WECHAT_PAL;
      }
      return TYPE_WECHAT_OK;
   }

   private static final int TYPE_WECHAT_DIALOG = 240;
   private static final int TYPE_WECHAT_PAL = 527;
   private static final int TYPE_WECHAT_OK = 4;
}
