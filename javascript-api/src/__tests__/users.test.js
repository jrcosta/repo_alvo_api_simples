const request = require('supertest');
const app = require('../app');
const userService = require('../services/userService');

describe('User Routes', () => {
  beforeEach(() => {
    userService.users = [
      { id: 1, name: "Alice", email: "alice@example.com" },
      { id: 2, name: "Bob", email: "bob@example.com" },
      { id: 3, name: "Charlie", email: "charlie@example.com" }
    ];
  });

  describe('GET /users/by-email', () => {
    test('should return user when email exists', async () => {
      const response = await request(app).get('/users/by-email?email=alice@example.com');
      expect(response.status).toBe(200);
      expect(response.body).toEqual({ id: 1, name: "Alice", email: "alice@example.com" });
    });

    test('should return 404 when email does not exist', async () => {
      const response = await request(app).get('/users/by-email?email=notfound@example.com');
      expect(response.status).toBe(404);
      expect(response.body.detail).toBe("Usuário não encontrado");
    });

    test('should return 400 when email parameter is missing', async () => {
      const response = await request(app).get('/users/by-email');
      expect(response.status).toBe(400);
      expect(response.body.detail).toBe("Parâmetro email é obrigatório");
    });
  });
});
