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

  updateUser(id, payload) {
    const index = this.users.findIndex(u => u.id === id);
    if (index === -1) return null;
    if (payload.name !== undefined) this.users[index].name = payload.name;
    if (payload.email !== undefined) this.users[index].email = payload.email;
    return this.users[index];
  }

  deleteUser(id) {
    const index = this.users.findIndex(u => u.id === id);
    if (index === -1) return false;
    this.users.splice(index, 1);
    return true;
  }
}

// Singleton instance
module.exports = new UserService();