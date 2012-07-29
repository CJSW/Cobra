package org.cjsw.web.auth;

import java.sql.SQLException;

import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.log4j.Logger;
import org.cjsw.wp.WordpressAuthUtil;

import ca.benow.security.AuthenticationFailedException;
import ca.benow.security.Security;
import ca.benow.security.authenticate.AuthenticationToken;
import ca.benow.security.authenticate.PasswordToken;
import ca.benow.security.store.LDAPSecurityBridge;
import ca.benow.security.store.LDAPSecurityStore;
import ca.benow.security.store.SecurityStore;
import ca.benow.security.store.UserImpl;
import ca.benow.security.user.User;
import ca.benow.security.user.UserManagementModule;

public class CJSWSecurityStore extends LDAPSecurityStore {

	private static final Logger log = Logger.getLogger(CJSWSecurityStore.class);

	private WordpressAuthUtil wpUtil;

	public CJSWSecurityStore() {
		super();
	}

	public CJSWSecurityStore(LDAPSecurityBridge bridge, SecurityStore overlay) {
		super(bridge, overlay);
		this.wpUtil = new WordpressAuthUtil();
	}

	@Override
	public User authenticate(AuthenticationToken token) throws SecurityException {
		try {
			return super.authenticate(token);
		} catch (AuthenticationFailedException e) {
			if (token instanceof PasswordToken) {
				// authentication failed, try with wordpress
				final PasswordToken pt = (PasswordToken) token;

				try {
					// if the user has a password, then it was incorrect so don't check wp
					if (!ldap.userHasPassword(pt.getUserName())) {

						// the user has no password, so the password should be authenticated
						// against the old wordpress db

						Security.log.info("Authenticating: " + pt.getUserName() + " against wordpress.");
						wpUtil.authenticate(pt.getUserName(), pt.getPassword());

						// authentication succeeded, set the user password
						Security.runAs(Security.getAdministratorUser(), new Runnable() {
							@Override
							public void run() {
								UserImpl u = (UserImpl) getUser(pt.getUserName());
								if (u != null) {
									u.setPassword(pt.getPassword());
									u.store();
									UserManagementModule umm = u.getManagementModule();
									umm.setConfirmed(true);
									umm.store();
									Security.log("Migrated user password from wordpress: " + pt.getUserName());
								} // else, no user here, could create
							}
						});
						// should now succeed
						return super.authenticate(token);
					}
				} catch (AuthenticationFailedException e0) {
					Security.log.warn("Authentication failed against wordpress");
					throw e0;
				} catch (SQLException e1) {
					log.error("Error while authenticating against wordpress", e1);
				} catch (LdapException e2) {
					log.error("Error while authenticating against wordpress", e2);
				}
			}
			throw new AuthenticationFailedException();
		}
	}

}
