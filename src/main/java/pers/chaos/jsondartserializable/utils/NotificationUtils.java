package pers.chaos.jsondartserializable.utils;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;

public class NotificationUtils {
    private static final String NOTIFICATION_GROUP_ID = "json-dart-serializable";

    /**
     * 右下角展示提示信息
     *
     * @param project IDEA项目
     * @param title   标题
     * @param message 内容
     * @param type    消息类型
     */
    public static void show(Project project, String title, String message, NotificationType type) {
        Notification notification = new Notification(NOTIFICATION_GROUP_ID, title, message, type);
        Notifications.Bus.notify(notification, project);
    }
}
