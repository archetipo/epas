package controllers;

import com.mysema.query.SearchResults;

import dao.NotificationDao;

import models.Notification;

import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import javax.inject.Inject;

/**
 * Controller to manage notifications.
 *
 * @author marco
 * @author cristian
 */
@With(Resecure.class)
public class Notifications extends Controller {

  @Inject
  static NotificationDao notificationDao;
  @Inject
  static SecurityRules rules;

  /**
   * Show current user archiveds notifications.
   */
  public static void archiveds() {
    final SearchResults<Notification> notifications = notificationDao
        .listFor(Security.getUser().get(), true)
        .listResults();
    render(notifications);
  }

  /**
   * Show current user notifications.
   */
  public static void list() {
    final SearchResults<Notification> notifications = notificationDao
        .listFor(Security.getUser().get(), false)
        .listResults();
    render("@archiveds", notifications);
  }

  /**
   * Show current unread user notifications. ajax only
   */
  public static void notifications() {
    final SearchResults<Notification> notifications =
        notificationDao.listUnreadFor(Security.getUser().get()).listResults();
    render(notifications);
  }

  /**
   * Mark a notification as read and redirect the user to the url referred by the notification.
   *
   * @param id id of the Notification to read
   */
  public static void readAndRedirect(int id) {
    final Notification notification = Notification.findById(id);
    notFoundIfNull(notification);
    rules.checkIfPermitted(notification);

    notification.read = true;
    notification.save();
    redirect(notification.getUrl());
  }

  /**
   * Questa chiamata si assume che sia sempre POST+ajax.
   *
   * @param id id of the Notification to read
   */
  public static void read(int id) {
    final Notification notification = Notification.findById(id);
    notFoundIfNull(notification);
    rules.checkIfPermitted(notification);

    notification.read = true;
    notification.save();
    render("/Notifications/notification.html", notification);
  }
}
