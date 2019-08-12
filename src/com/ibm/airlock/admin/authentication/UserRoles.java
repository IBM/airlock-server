package com.ibm.airlock.admin.authentication;

import java.security.GeneralSecurityException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.servlet.ServletContext;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Constants.RoleType;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.operations.UserRoleSets;
import com.ibm.airlock.admin.operations.UserRoleSets.UserRoleSet;
import com.ibm.airlock.admin.operations.Roles;

public class UserRoles
{
	static final String WILDCARD = "*";
	static final String LEGACY = "@weather.com"; // replace legacy * wild-card with *@weather.com

	Map<String, Set<RoleType>> method2Roles = new TreeMap<String, Set<RoleType>>();
	Map<String, Set<RoleType>> user2Roles = new TreeMap<String, Set<RoleType>>();
	Map<String, Set<RoleType>> suffix2Roles = new TreeMap<String, Set<RoleType>>();
	
	static public UserRoles get(ServletContext context)
	{
		return (UserRoles) context.getAttribute(Constants.USER_ROLES);
	}

	public UserRoles(Roles roles, UserRoleSets users)
	{
		// create reverse mappings from roles/users
		for (Roles.Role role : roles.getRoles())
		{
			RoleType type = Utilities.valueOf(RoleType.class, role.getRole());
			if (type == null)
				throw new RuntimeException("Invalid role " + role.getRole());

			for (String action : role.getActions())
			{
				addRole(method2Roles, action, type);
			}
		}
		
		resetUsersRoles(users);
/*
		for (Map.Entry<String, AirlockUser_new> entry : users.getAirlockUsers().entrySet()) {
			AirlockUser_new user = entry.getValue();
			if (user.isGroupRepresentation()) {
				//*.IBM.com will be stored as .ibm.com
				setRoles(suffix2Roles, user.getUserIdentifier().substring(1).toLowerCase(), user.getUserRoles());
			}
			else {
				setRoles(user2Roles, user.getUserIdentifier().toLowerCase(), user.getUserRoles());
			}
		}

		// add wild-card roles to each user
		for (Map.Entry<String, Set<RoleType>> ent : user2Roles.entrySet())
		{
			String user = ent.getKey();
			Set<RoleType> userRoles = ent.getValue();

			for (Map.Entry<String, Set<RoleType>> suff : suffix2Roles.entrySet())
			{
				if (user.endsWith(suff.getKey()))
					userRoles.addAll(suff.getValue());
			}
		}*/
	}

	public void resetUsersRoles(UserRoleSets users) {
		suffix2Roles.clear();
		user2Roles.clear();
		for (Map.Entry<String, UserRoleSet> entry : users.getAirlockUsers().entrySet()) {
			UserRoleSet user = entry.getValue();
			if (user.isGroupRepresentation()) {
				//*.IBM.com will be stored as .ibm.com
				setRoles(suffix2Roles, user.getUserIdentifier().substring(1).toLowerCase(), user.getUserRoles());
			}
			else {
				setRoles(user2Roles, user.getUserIdentifier().toLowerCase(), user.getUserRoles());
			}
		}

		// add wild-card roles to each user
		for (Map.Entry<String, Set<RoleType>> ent : user2Roles.entrySet())
		{
			String user = ent.getKey();
			Set<RoleType> userRoles = ent.getValue();

			for (Map.Entry<String, Set<RoleType>> suff : suffix2Roles.entrySet())
			{
				if (user.endsWith(suff.getKey()))
					userRoles.addAll(suff.getValue());
			}
		}
	}
	
	public void addUser (UserRoleSet newUser) {
		if (newUser.isGroupRepresentation()) {
			setRoles(suffix2Roles, newUser.getUserIdentifier().substring(1).toLowerCase(), newUser.getUserRoles());
			// add wild-card roles to each user
			for (Map.Entry<String, Set<RoleType>> ent : user2Roles.entrySet())
			{
				String user = ent.getKey();
				Set<RoleType> userRoles = ent.getValue();

				for (Map.Entry<String, Set<RoleType>> suff : suffix2Roles.entrySet())
				{
					if (user.endsWith(suff.getKey()))
						userRoles.addAll(suff.getValue());
				}
			}
		}
		else {
			setRoles(user2Roles, newUser.getUserIdentifier().toLowerCase(), newUser.getUserRoles());
			String userIdentifierKey = newUser.getUserIdentifier().toLowerCase();
			for (Map.Entry<String, Set<RoleType>> suff : suffix2Roles.entrySet())
			{
				if (userIdentifierKey.endsWith(suff.getKey()))
					user2Roles.get(userIdentifierKey).addAll(suff.getValue());
			}
		}
	}

	public void removeUser (UserRoleSet removedUser, UserRoleSets users) {
		if (removedUser.isGroupRepresentation()) {
			resetUsersRoles(users);
		}
		else {
			user2Roles.remove(removedUser.getUserIdentifier().toLowerCase());
		}
		
	}

	void addRole(Map<String, Set<RoleType>> map, String key, RoleType value)
	{
		Set<RoleType> roles = map.get(key);
		if (roles == null)
		{
			roles = new TreeSet<RoleType>();
			map.put(key, roles);
		}
		roles.add(value);
	}

	void setRoles(Map<String, Set<RoleType>> map, String key, List<RoleType> values)
	{
		map.put(key, new HashSet<RoleType>(Utilities.cloneRoleTypesList(values))); //clone the roles list before changing to set
	}

	public Set<RoleType> getUserRoles(String userId)
	{
		userId = userId.toLowerCase();
		Set<RoleType> userRoles = user2Roles.get(userId);
		if (userRoles != null)
			return userRoles; // exact match found

		// else, try to match on a suffix
		Set<RoleType> customRoles = new TreeSet<RoleType>();
		for (Map.Entry<String, Set<RoleType>> ent : suffix2Roles.entrySet())
		{
			if (userId.endsWith(ent.getKey()))
				customRoles.addAll(ent.getValue());
		}
		return customRoles; // empty if no suffix matched
	}
	void checkPermittedRoles(String methodSignature, String userId) throws GeneralSecurityException
	{
		Set<RoleType> userRoles = getUserRoles(userId);
		checkPermittedRoles(methodSignature, userRoles);
	}
	void checkPermittedRoles(String methodSignature, Set<RoleType> userRoles) throws GeneralSecurityException
	{
		Set<RoleType> methodRoles = method2Roles.get(methodSignature);
		if (methodRoles == null)
			throw new GeneralSecurityException("No roles configured for method " + methodSignature);

		if (intersect(methodRoles, userRoles) == false)
			throw new GeneralSecurityException("User does not have permission to call this method");
	}
	boolean intersect(Set<RoleType> one, Set<RoleType> two)
	{
		for (RoleType role : one)
		{
			if (two.contains(role))
				return true;
		}
		return false;
	}

	public Set<RoleType> getRoleSubset(String userId, Set<RoleType> subset) throws GeneralSecurityException
	{
		Set<RoleType> implied = getUserRoles(userId);

		Set<RoleType> invalid = new TreeSet<RoleType>(subset);
		invalid.removeAll(implied);
		if (!invalid.isEmpty())
			throw new GeneralSecurityException("unauthorized roles requested for user " + userId + " : " + invalid.toString());

		return subset;
	}
	/*
	// compare two user lists and find users that have been deleted or have removed permissions
	static public void compareNewUsers(Roles roles, AirlockUsers oldUsers, AirlockUsers newUsers, Set<String> deleted, Map<String, Set<RoleType>> demoted)
	{
		UserRoles older = new UserRoles(roles, oldUsers);
		UserRoles newer = new UserRoles(roles, newUsers);

		for (String s : older.user2Roles.keySet())
			deleted.add(s);

		deleted.removeAll(newer.user2Roles.keySet());

		for (String s : older.user2Roles.keySet())
		{
			Set<RoleType> newroles = newer.user2Roles.get(s);
			if (newroles != null)
			{
				Set<RoleType> oldroles = new TreeSet<RoleType>(older.user2Roles.get(s));
				oldroles.removeAll(newroles);
				if (!oldroles.isEmpty())
					demoted.put(s, oldroles);
			}
		}
	}
	 */
}
