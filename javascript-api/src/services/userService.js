class UserService {
  constructor() {
    this.users = [
      { id: 1, name: "Alice", email: "alice@example.com" },
      { id: 2, name: "Bob", email: "bob@example.com" },
      { id: 3, name: "Charlie", email: "charlie@example.com" },
    ];
    this.nextId = 4;
  }

  listUsers(limit = 100, offset = 0) {
    return this.users.slice(offset, offset + limit);
  }

  getUser(id) {
    return this.users.find(u => u.id === id);
  }

  findByEmail(email) {
    return this.users.find(u => u.email === email);
  }

  createUser(payload) {
    const newUser = {
      id: this.nextId++,
      name: payload.name,
      email: payload.email
    };
    this.users.push(newUser);
    return newUser;
  }
}

// Singleton instance
module.exports = new UserService();