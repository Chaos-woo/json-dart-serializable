package pers.chaos.jsondartserializable.utils;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;

public class NotificationUtils {
    private static final String NOTIFICATION_GROUP_ID = "json-dart-serializable";

    public static void showNotification(Project project, String title, String message, NotificationType type) {
        Notification notification = new Notification(NOTIFICATION_GROUP_ID, title, message, type);
        Notifications.Bus.notify(notification, project);
    }
}
