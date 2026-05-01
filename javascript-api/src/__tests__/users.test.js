const request = require('supertest');
const express = require('express');
const userRoutes = require('../routes/users');
const userService = require('../services/userService');

const app = express();
app.use(express.json());
app.use('/users', userRoutes);

describe('Users API Routes', () => {
  beforeEach(() => {
    // Reset userService internal state before each test
    // Since userService is a singleton, we reset its users array and nextId
    userService.users = [
      { id: 1, name: "Alice", email: "alice@example.com" },
      { id: 2, name: "Bob", email: "bob@example.com" },
      { id: 3, name: "Charlie", email: "charlie@example.com" },
    ];
    userService.nextId = 4;
  });

  describe('PUT /users/:user_id', () => {
    test('should update user with valid name and email and return 200 with updated user', async () => {
      const res = await request(app)
        .put('/users/1')
        .send({ name: 'Alice Updated', email: 'alice.updated@example.com' });

      expect(res.statusCode).toBe(200);
      expect(res.body).toMatchObject({
        id: 1,
        name: 'Alice Updated',
        email: 'alice.updated@example.com',
      });

      // Verify internal state changed
      const user = userService.getUser(1);
      expect(user.name).toBe('Alice Updated');
      expect(user.email).toBe('alice.updated@example.com');
    });

    test('should update user partially with only name and return 200', async () => {
      const originalEmail = userService.getUser(2).email;
      const res = await request(app)
        .put('/users/2')
        .send({ name: 'Bob Newname' });

      expect(res.statusCode).toBe(200);
      expect(res.body.name).toBe('Bob Newname');
      expect(res.body.email).toBe(originalEmail);
    });

    test('should update user partially with only email and return 200', async () => {
      const originalName = userService.getUser(3).name;
      const res = await request(app)
        .put('/users/3')
        .send({ email: 'charlie.new@example.com' });

      expect(res.statusCode).toBe(200);
      expect(res.body.email).toBe('charlie.new@example.com');
      expect(res.body.name).toBe(originalName);
    });

    test('should return 422 if no name or email provided in payload', async () => {
      const res = await request(app)
        .put('/users/1')
        .send({});

      expect(res.statusCode).toBe(422);
      expect(res.body).toEqual({ detail: "Pelo menos um dos campos 'name' ou 'email' deve ser informado" });
    });

    test('should return 404 if user does not exist', async () => {
      const res = await request(app)
        .put('/users/999')
        .send({ name: 'No One' });

      expect(res.statusCode).toBe(404);
      expect(res.body).toEqual({ detail: "Usuário não encontrado" });
    });

    test('should return 409 if email is already registered to another user', async () => {
      // Bob has email bob@example.com
      const res = await request(app)
        .put('/users/1')
        .send({ email: 'bob@example.com' });

      expect(res.statusCode).toBe(409);
      expect(res.body).toEqual({ detail: "E-mail já cadastrado" });
    });

    test('should accept invalid email format and update user (no validation)', async () => {
      const invalidEmail = 'invalid-email-format';
      const res = await request(app)
        .put('/users/1')
        .send({ email: invalidEmail });

      expect(res.statusCode).toBe(200);
      expect(res.body.email).toBe(invalidEmail);
    });

    test('should return 422 if user_id is not a valid number', async () => {
      const res = await request(app)
        .put('/users/abc')
        .send({ name: 'Invalid ID' });

      expect(res.statusCode).toBe(422);
      expect(res.body).toEqual({ detail: "ID de usuário inválido" });
    });

    test('should return 422 if payload is missing', async () => {
      const res = await request(app)
        .put('/users/1')
        .send();

      expect(res.statusCode).toBe(422);
      expect(res.body).toEqual({ detail: "Pelo menos um dos campos 'name' ou 'email' deve ser informado" });
    });
  });

  describe('DELETE /users/:user_id', () => {
    test('should delete existing user and return 204', async () => {
      const res = await request(app)
        .delete('/users/1');

      expect(res.statusCode).toBe(204);

      // User should be removed
      expect(userService.getUser(1)).toBeUndefined();
    });

    test('should return 404 when deleting non-existent user', async () => {
      const res = await request(app)
        .delete('/users/999');

      expect(res.statusCode).toBe(404);
      expect(res.body).toEqual({ detail: "Usuário não encontrado" });
    });

    test('should return 422 when user_id is invalid', async () => {
      const res = await request(app)
        .delete('/users/abc');

      expect(res.statusCode).toBe(422);
      expect(res.body).toEqual({ detail: "ID de usuário inválido" });
    });

    test('should handle multiple deletes on same user id', async () => {
      const first = await request(app).delete('/users/2');
      expect(first.statusCode).toBe(204);

      const second = await request(app).delete('/users/2');
      expect(second.statusCode).toBe(404);
      expect(second.body).toEqual({ detail: "Usuário não encontrado" });
    });

    test('should not affect other users when deleting one user', async () => {
      const usersBefore = userService.listUsers();

      const res = await request(app).delete('/users/3');
      expect(res.statusCode).toBe(204);

      const usersAfter = userService.listUsers();
      expect(usersAfter).toHaveLength(usersBefore.length - 1);
      expect(usersAfter.find(u => u.id === 3)).toBeUndefined();

      for (const user of usersAfter) {
        const beforeUser = usersBefore.find(u => u.id === user.id);
        expect(user).toEqual(beforeUser);
      }
    });
  });
});