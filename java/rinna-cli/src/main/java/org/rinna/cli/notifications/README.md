# Rinna CLI Notification System

This package contains the notification system for the Rinna CLI, providing a comprehensive notification infrastructure for the Rinna workflow management system.

## Components

### NotificationType

The `NotificationType` enum defines different categories of notifications:

- `ASSIGNMENT`: Work item assignments to users
- `UPDATE`: Updates to work items
- `COMMENT`: New comments on work items
- `DEADLINE`: Deadline reminders
- `MENTION`: Mentions of users in comments or descriptions
- `SECURITY`: Security-related notifications
- `SYSTEM`: System notifications
- `COMPLETION`: Work item completion
- `ATTENTION`: Work items requiring attention

### Notification

The `Notification` class represents individual notifications with the following properties:

- Unique ID
- Type (from NotificationType)
- Message content
- Source (user or system that generated the notification)
- Target user (recipient of the notification)
- Timestamp
- Related work item ID (if applicable)
- Priority level (LOW, MEDIUM, HIGH, URGENT)
- Read status

### NotificationService

The `NotificationService` manages notifications, including:

- Adding new notifications
- Retrieving notifications for a user
- Filtering notifications by read status
- Marking notifications as read
- Persisting notifications to disk
- Displaying unread notifications

## CLI Command

The notification system is accessed through the `notify` command:

```bash
rin notify               # List all notifications
rin notify list          # List all notifications
rin notify unread        # Show only unread notifications
rin notify read <id>     # Mark a notification as read
rin notify markread <id> # Mark a notification as read
rin notify markall       # Mark all notifications as read
rin notify clear         # Clear old notifications
rin notify help          # Show notification help
```

## Integration with Rinna CLI

The notification system is integrated with the main CLI through:

1. The `handleNotifyCommand` method in `RinnaCli`
2. The `checkUnreadMessages` method which displays unread notifications
3. The addition of "notify" to the help output

## Usage Examples

### Creating Notifications

```java
// Create a system notification
NotificationService notificationService = NotificationService.getInstance();
notificationService.addSystemNotification(
    "System maintenance scheduled for tomorrow", 
    Notification.Priority.HIGH
);

// Create a work item notification
Notification notification = Notification.createWorkItemNotification(
    NotificationType.ASSIGNMENT,
    "You have been assigned to task WI-123",
    "system",
    "username",
    "WI-123",
    Notification.Priority.MEDIUM
);
notificationService.addNotification(notification);
```

### Displaying Notifications

```java
// Display unread notifications
NotificationService notificationService = NotificationService.getInstance();
notificationService.displayUnreadNotifications();

// Get all notifications for current user
List<Notification> notifications = notificationService.getNotificationsForCurrentUser();
for (Notification notification : notifications) {
    System.out.println(notification.format());
}
```

## Storage

Notifications are stored in the user's home directory under:
```
~/.rinna/notifications/<username>.dat
```

## Future Enhancements

1. Add support for notification preferences
2. Implement filtering by work item, project, or type
3. Add desktop/system notifications
4. Email integration for important notifications
5. Notification expiration policies
6. Batch notification summaries