const UserService = require('./userService');

describe('UserService', () => {
  let userService;

  beforeEach(() => {
    // Create a fresh instance for isolation
    userService = new UserService.constructor();
  });

  describe('updateUser', () => {
    test('should update name and email of an existing user and return updated user', () => {
      const updated = userService.updateUser(1, { name: 'Alice Updated', email: 'alice.updated@example.com' });
      expect(updated).not.toBeNull();
      expect(updated.id).toBe(1);
      expect(updated.name).toBe('Alice Updated');
      expect(updated.email).toBe('alice.updated@example.com');

      // Verify internal state changed
      const user = userService.getUser(1);
      expect(user.name).toBe('Alice Updated');
      expect(user.email).toBe('alice.updated@example.com');
    });

    test('should return null when trying to update a non-existent user', () => {
      const result = userService.updateUser(999, { name: 'No One', email: 'noone@example.com' });
      expect(result).toBeNull();

      // Ensure no user was added or changed
      expect(userService.getUser(999)).toBeUndefined();
      expect(userService.listUsers()).toHaveLength(3);
    });

    test('should update only the name when only name is provided', () => {
      const originalEmail = userService.getUser(2).email;
      const updated = userService.updateUser(2, { name: 'Bob Newname' });
      expect(updated).not.toBeNull();
      expect(updated.name).toBe('Bob Newname');
      expect(updated.email).toBe(originalEmail);

      // Internal state check
      const user = userService.getUser(2);
      expect(user.name).toBe('Bob Newname');
      expect(user.email).toBe(originalEmail);
    });

    test('should update only the email when only email is provided', () => {
      const originalName = userService.getUser(3).name;
      const updated = userService.updateUser(3, { email: 'charlie.new@example.com' });
      expect(updated).not.toBeNull();
      expect(updated.email).toBe('charlie.new@example.com');
      expect(updated.name).toBe(originalName);

      // Internal state check
      const user = userService.getUser(3);
      expect(user.email).toBe('charlie.new@example.com');
      expect(user.name).toBe(originalName);
    });

    test('should accept invalid email format without validation and update user', () => {
      const invalidEmail = 'invalid-email-format';
      const updated = userService.updateUser(1, { email: invalidEmail });
      expect(updated).not.toBeNull();
      expect(updated.email).toBe(invalidEmail);

      // Internal state check
      const user = userService.getUser(1);
      expect(user.email).toBe(invalidEmail);
    });

    test('should accept invalid name (empty string) and update user', () => {
      const updated = userService.updateUser(2, { name: '' });
      expect(updated).not.toBeNull();
      expect(updated.name).toBe('');

      // Internal state check
      const user = userService.getUser(2);
      expect(user.name).toBe('');
    });

    test('should perform multiple consecutive updates on the same user', () => {
      let updated = userService.updateUser(3, { name: 'Charlie 1' });
      expect(updated.name).toBe('Charlie 1');

      updated = userService.updateUser(3, { email: 'charlie1@example.com' });
      expect(updated.email).toBe('charlie1@example.com');

      updated = userService.updateUser(3, { name: 'Charlie 2', email: 'charlie2@example.com' });
      expect(updated.name).toBe('Charlie 2');
      expect(updated.email).toBe('charlie2@example.com');

      // Final state check
      const user = userService.getUser(3);
      expect(user.name).toBe('Charlie 2');
      expect(user.email).toBe('charlie2@example.com');
    });

    test('should return null and not modify users when id is invalid (null, undefined, string)', () => {
      const originalUsers = userService.listUsers().map(u => ({ ...u }));

      expect(userService.updateUser(null, { name: 'X' })).toBeNull();
      expect(userService.updateUser(undefined, { email: 'x@example.com' })).toBeNull();
      expect(userService.updateUser('abc', { name: 'Y' })).toBeNull();

      // No user modified
      const currentUsers = userService.listUsers();
      expect(currentUsers).toEqual(originalUsers);
    });
  });

  describe('deleteUser', () => {
    test('should delete an existing user and return true', () => {
      const result = userService.deleteUser(1);
      expect(result).toBe(true);

      // User should no longer exist
      expect(userService.getUser(1)).toBeUndefined();
      expect(userService.listUsers()).toHaveLength(2);
    });

    test('should return false when trying to delete a non-existent user', () => {
      const result = userService.deleteUser(999);
      expect(result).toBe(false);

      // No user removed
      expect(userService.listUsers()).toHaveLength(3);
    });

    test('should return false and not remove any user when id is invalid (null, undefined, string)', () => {
      const originalUsers = userService.listUsers().map(u => ({ ...u }));

      expect(userService.deleteUser(null)).toBe(false);
      expect(userService.deleteUser(undefined)).toBe(false);
      expect(userService.deleteUser('abc')).toBe(false);

      // No user removed
      const currentUsers = userService.listUsers();
      expect(currentUsers).toEqual(originalUsers);
    });

    test('should handle multiple consecutive deletions for the same user id', () => {
      const firstDelete = userService.deleteUser(2);
      expect(firstDelete).toBe(true);

      const secondDelete = userService.deleteUser(2);
      expect(secondDelete).toBe(false);

      // User 2 should be gone
      expect(userService.getUser(2)).toBeUndefined();
      expect(userService.listUsers()).toHaveLength(2);
    });

    test('should not affect other users when deleting one user', () => {
      const usersBefore = userService.listUsers();
      const result = userService.deleteUser(3);
      expect(result).toBe(true);

      const usersAfter = userService.listUsers();
      expect(usersAfter).toHaveLength(usersBefore.length - 1);
      expect(usersAfter.find(u => u.id === 3)).toBeUndefined();

      // Other users remain unchanged
      for (const user of usersAfter) {
        const beforeUser = usersBefore.find(u => u.id === user.id);
        expect(user).toEqual(beforeUser);
      }
    });
  });
});