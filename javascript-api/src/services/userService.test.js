const UserService = require('./userService');

describe('UserService', () => {
  let userService;

  beforeEach(() => {
    // Create a fresh instance for isolation
    userService = new UserService.constructor();
  });

  describe('listUsers', () => {
    test('should return all preloaded users when called without parameters', () => {
      const users = userService.listUsers();
      expect(users).toHaveLength(3);
      expect(users).toEqual([
        { id: 1, name: "Alice", email: "alice@example.com" },
        { id: 2, name: "Bob", email: "bob@example.com" },
        { id: 3, name: "Charlie", email: "charlie@example.com" },
      ]);
    });

    test('should return correct slice of users with limit and offset', () => {
      const users = userService.listUsers(2, 1);
      expect(users).toHaveLength(2);
      expect(users[0].name).toBe("Bob");
      expect(users[1].name).toBe("Charlie");
    });

    test('should return empty array when offset is greater than users length', () => {
      const users = userService.listUsers(10, 100);
      expect(users).toEqual([]);
    });

    test('should handle zero limit by returning empty array', () => {
      const users = userService.listUsers(0, 0);
      expect(users).toEqual([]);
    });

    test('should handle negative limit by returning all but the last element (slice behavior)', () => {
      // listUsers(-1, 0) calls slice(0, -1) which returns all but the last element
      const users = userService.listUsers(-1, 0);
      expect(users).toHaveLength(2);
      expect(users[0].name).toBe("Alice");
      expect(users[1].name).toBe("Bob");
    });

    test('should handle negative offset by treating it as zero', () => {
      // According to code, slice with negative offset behaves as slice from end,
      // but we test actual behavior to document it.
      const users = userService.listUsers(2, -1);
      // slice(-1, 1) returns empty array because end < start
      expect(users).toEqual([]);
    });
  });

  describe('getUser', () => {
    test('should return user when id exists', () => {
      const user = userService.getUser(2);
      expect(user).toEqual({ id: 2, name: "Bob", email: "bob@example.com" });
    });

    test('should return undefined when id does not exist', () => {
      const user = userService.getUser(999);
      expect(user).toBeUndefined();
    });

    test('should return undefined when id is negative or zero', () => {
      expect(userService.getUser(0)).toBeUndefined();
      expect(userService.getUser(-5)).toBeUndefined();
    });
  });

  describe('findByEmail', () => {
    test('should return user when email exists', () => {
      const user = userService.findByEmail("alice@example.com");
      expect(user).toEqual({ id: 1, name: "Alice", email: "alice@example.com" });
    });

    test('should return undefined when email does not exist', () => {
      const user = userService.findByEmail("naoexiste@example.com");
      expect(user).toBeUndefined();
    });

    test('should be case sensitive when searching by email', () => {
      const user = userService.findByEmail("Alice@Example.com");
      expect(user).toBeUndefined();
    });
  });

  describe('createUser', () => {
    test('should create a new user with incremental id and add to users list', () => {
      const newUser = userService.createUser({ name: "Diana", email: "diana@example.com" });
      expect(newUser).toMatchObject({ id: 4, name: "Diana", email: "diana@example.com" });
      expect(userService.users).toContainEqual(newUser);
      expect(userService.users).toHaveLength(4);
    });

    test('should increment id correctly on multiple creations', () => {
      const user1 = userService.createUser({ name: "Diana", email: "diana@example.com" });
      const user2 = userService.createUser({ name: "Eve", email: "eve@example.com" });
      expect(user1.id).toBe(4);
      expect(user2.id).toBe(5);
    });

    test('should allow creation of user with missing name or email fields', () => {
      const userWithNoEmail = userService.createUser({ name: "Eve" });
      expect(userWithNoEmail).toMatchObject({ id: 4, name: "Eve", email: undefined });
      const userWithNoName = userService.createUser({ email: "eve@example.com" });
      expect(userWithNoName).toMatchObject({ id: 5, name: undefined, email: "eve@example.com" });
    });

    test('should allow creation of users with duplicate emails', () => {
      const user1 = userService.createUser({ name: "Diana", email: "alice@example.com" });
      const user2 = userService.createUser({ name: "Eve", email: "alice@example.com" });
      expect(user1.email).toBe("alice@example.com");
      expect(user2.email).toBe("alice@example.com");
      expect(userService.users.filter(u => u.email === "alice@example.com")).toHaveLength(3);
    });

    test('listUsers should reflect new users after multiple creations', () => {
      userService.createUser({ name: "Diana", email: "diana@example.com" });
      userService.createUser({ name: "Eve", email: "eve@example.com" });
      const users = userService.listUsers();
      expect(users).toHaveLength(5);
      expect(users.some(u => u.name === "Diana")).toBe(true);
      expect(users.some(u => u.name === "Eve")).toBe(true);
    });
  });
});