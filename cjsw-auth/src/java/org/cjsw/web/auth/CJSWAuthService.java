package org.cjsw.web.auth;

import ca.benow.security.Security;
import ca.benow.security.store.LDAPSecurityBridge;
import ca.benow.security.store.SecurityStore;
import ca.benow.security.store.cache.CachedSecurityStoreBuilder;
import ca.benow.security.store.repository.RepositorySecurityStore;
import ca.benow.web.WebService;
import ca.benow.web.security.invite.InvitationService;
import ca.benow.web.security.register.RegistrationService;

public class CJSWAuthService extends WebService {

	static {
		mainClass = CJSWAuthService.class;

		LDAPSecurityBridge ldap = new LDAPSecurityBridge("localhost", 10389, false);
		ldap.setBindUser("uid=ldapbind,ou=people,ou=cjsw", "ldapb1nd");
		ldap.setUserBase("ou=people,ou=cjsw");
		ldap.setGroupBase("ou=groups,ou=cjsw");
		ldap.setRoleBase("ou=roles,ou=cjsw");

		RepositorySecurityStore overlay = new RepositorySecurityStore();
		SecurityStore store = new CJSWSecurityStore(ldap, overlay);
		store = new CachedSecurityStoreBuilder(store).build();
		Security.setStore(store);

		// default configuration
		RegistrationService.CFG_REGISTRATION_ENABLED.setDefaultValue(false);
		InvitationService.CFG_INVITATION_ENABLED.setDefaultValue(false);
//		Security.CFG_LOGIN_PAGE.setValueAsString("http://login.benow.ca:8080/security/login/index.page");
		setTitle("CJSW: Login");
	}

	public CJSWAuthService() {
		super();
	}

}
