package org.exoplatform.addons.crowdin;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.Constants;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.UserProfile;
import org.exoplatform.services.organization.UserProfileHandler;
import org.exoplatform.services.security.ConversationRegistry;
import org.exoplatform.services.security.ConversationState;

/**
 * This listener is executed each time an user logs in.
 * It reset the user language to the pseudo-language defined in Crowdin settings for live translation. This language
 * is passed as a system property (crowdin.jipt).
 */
public class LoginCrowdinJIPTListener extends Listener<ConversationRegistry, ConversationState> {

  private static final Log LOG = ExoLogger.getLogger(LoginCrowdinJIPTListener.class);
  public static final String CROWDIN_JIPT_SYSTEM_PROPERTY_NAME = "crowdin.jipt";

  /**
   * Method executed when an user logs in
   *
   * @throws Exception
   */
  @Override
  public void onEvent(Event<ConversationRegistry, ConversationState> event) throws Exception {
    String userId = event.getData().getIdentity().getUserId();
    try {
      OrganizationService svc = PortalContainer.getInstance().getComponentInstanceOfType(OrganizationService.class);
      // Don't rely on UserProfileLifecycle loaded UserProfile when doing
      // an update to avoid a potential overwrite of other changes
      UserProfileHandler userProfileHandler = svc.getUserProfileHandler();
      UserProfile userProfile = userProfileHandler.findUserProfileByName(userId);

      // create the user profile if it does not exist
      if(userProfile == null) {
        UserProfile userProfileInstance = userProfileHandler.createUserProfileInstance(userId);
        userProfileInstance.getUserInfoMap().put("user.name.given", userId);
        userProfileHandler.saveUserProfile(userProfileInstance, true);
        userProfile = userProfileHandler.findUserProfileByName(userId);
      }

      if (userProfile != null && userProfile.getUserInfoMap() != null) {
        // Only save if user's locale has not been set
        String currLocale = userProfile.getUserInfoMap().get(Constants.USER_LANGUAGE);
        String crowdinJiptLanguage = System.getProperty(CROWDIN_JIPT_SYSTEM_PROPERTY_NAME);
        if (currLocale == null || currLocale.trim().equals("") || !currLocale.equals(crowdinJiptLanguage)) {
          //set pseudo-language from crowdin.jipt system property
          userProfile.getUserInfoMap().put(Constants.USER_LANGUAGE, crowdinJiptLanguage);
          userProfileHandler.saveUserProfile(userProfile, false);
          LOG.info("Set Crowdin JIPT user to " + crowdinJiptLanguage);
        }
      }

    } catch (Exception e) {
      LOG.debug("Error while logging the login of user '" + userId + "': " + e.getMessage(), e);
    }
  }
}