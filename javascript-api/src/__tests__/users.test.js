const request = require('supertest');
const express = require('express');
const usersRouter = require('../routes/users');
const userService = require('../services/userService');

jest.mock('../services/userService');

const app = express();
app.use(express.json());
app.use('/users', usersRouter);

describe('PUT /users/:user_id', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('Deve retornar 422 para user_id não numérico', async () => {
    const res = await request(app).put('/users/abc').send({ name: 'New Name' });

    expect(res.statusCode).toBe(422);
    expect(res.body).toEqual({ detail: 'ID de usuário inválido' });
  });

  test.each([{}, null, 'string', []])(
    'Deve retornar 422 para corpo inválido (%p)',
    async (body) => {
      const res = await request(app).put('/users/1').send(body);

      expect(res.statusCode).toBe(422);
      expect(res.body).toEqual({ detail: "Pelo menos um dos campos 'name' ou 'email' deve ser informado" });
    }
  );

  test('Deve retornar 422 para corpo sem name nem email', async () => {
    const res = await request(app).put('/users/1').send({ age: 30 });

    expect(res.statusCode).toBe(422);
    expect(res.body).toEqual({ detail: "Pelo menos um dos campos 'name' ou 'email' deve ser informado" });
  });

  test('Deve retornar 409 se email já cadastrado por outro usuário', async () => {
    userService.findByEmail.mockReturnValue({ id: 2, email: 'existing@example.com' });

    const res = await request(app)
      .put('/users/1')
      .send({ email: 'existing@example.com' });

    expect(res.statusCode).toBe(409);
    expect(res.body).toEqual({ detail: 'E-mail já cadastrado' });
    expect(userService.findByEmail).toHaveBeenCalledWith('existing@example.com');
  });

  test('Deve retornar 404 se usuário não encontrado para update', async () => {
    userService.findByEmail.mockReturnValue(null);
    userService.updateUser.mockReturnValue(null);

    const res = await request(app)
      .put('/users/1')
      .send({ name: 'Updated Name' });

    expect(res.statusCode).toBe(404);
    expect(res.body).toEqual({ detail: 'Usuário não encontrado' });
    expect(userService.updateUser).toHaveBeenCalledWith(1, { name: 'Updated Name', email: undefined });
  });

  test('Deve atualizar usuário com dados válidos e retornar objeto atualizado', async () => {
    userService.findByEmail.mockReturnValue(null);
    const updatedUser = { id: 1, name: 'Updated Name', email: 'updated@example.com' };
    userService.updateUser.mockReturnValue(updatedUser);

    const res = await request(app)
      .put('/users/1')
      .send({ name: 'Updated Name', email: 'updated@example.com' });

    expect(res.statusCode).toBe(200);
    expect(res.body).toEqual(updatedUser);
    expect(userService.updateUser).toHaveBeenCalledWith(1, { name: 'Updated Name', email: 'updated@example.com' });
  });

  test('Deve ignorar campos extras no corpo e atualizar somente name e email', async () => {
    userService.findByEmail.mockReturnValue(null);
    const updatedUser = { id: 1, name: 'Updated Name', email: 'updated@example.com' };
    userService.updateUser.mockReturnValue(updatedUser);

    const res = await request(app)
      .put('/users/1')
      .send({ name: 'Updated Name', email: 'updated@example.com', age: 30, address: 'Rua X' });

    expect(res.statusCode).toBe(200);
    expect(res.body).toEqual(updatedUser);
    expect(userService.updateUser).toHaveBeenCalledWith(1, { name: 'Updated Name', email: 'updated@example.com' });
  });

  test('Deve retornar 422 para user_id zero ou negativo', async () => {
    const resZero = await request(app).put('/users/0').send({ name: 'Name' });
    expect(resZero.statusCode).toBe(422);
    expect(resZero.body).toEqual({ detail: 'ID de usuário inválido' });

    const resNeg = await request(app).put('/users/-5').send({ name: 'Name' });
    expect(resNeg.statusCode).toBe(422);
    expect(resNeg.body).toEqual({ detail: 'ID de usuário inválido' });
  });

  test.each([
    [{ name: 123 }],
    [{ email: 'invalid-email' }],
    [{ name: 123, email: 'invalid-email' }]
  ])('Deve aceitar corpo com campos inválidos e tentar atualizar (sem validação extra)', async (body) => {
    // Como não há validação explícita no código para formato de email ou tipo de name,
    // o teste verifica que a requisição é encaminhada para updateUser.
    userService.findByEmail.mockReturnValue(null);
    userService.updateUser.mockReturnValue({ id: 1, ...body });

    const res = await request(app).put('/users/1').send(body);

    expect(res.statusCode).toBe(200);
    expect(userService.updateUser).toHaveBeenCalledWith(1, body);
  });
});

describe('DELETE /users/:user_id', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('Deve retornar 422 para user_id não numérico', async () => {
    const res = await request(app).delete('/users/abc');

    expect(res.statusCode).toBe(422);
    expect(res.body).toEqual({ detail: 'ID de usuário inválido' });
  });

  test('Deve retornar 422 para user_id zero ou negativo', async () => {
    const resZero = await request(app).delete('/users/0');
    expect(resZero.statusCode).toBe(422);
    expect(resZero.body).toEqual({ detail: 'ID de usuário inválido' });

    const resNeg = await request(app).delete('/users/-10');
    expect(resNeg.statusCode).toBe(422);
    expect(resNeg.body).toEqual({ detail: 'ID de usuário inválido' });
  });

  test('Deve retornar 404 se usuário não encontrado', async () => {
    userService.getUser.mockReturnValue(null);

    const res = await request(app).delete('/users/1');

    expect(res.statusCode).toBe(404);
    expect(res.body).toEqual({ detail: 'Usuário não encontrado' });
    expect(userService.getUser).toHaveBeenCalledWith(1);
  });

  test('Deve retornar 204 e deletar usuário existente', async () => {
    userService.getUser.mockReturnValue({ id: 1, name: 'User', email: 'user@example.com' });
    userService.deleteUser.mockImplementation(() => {});

    const res = await request(app).delete('/users/1');

    expect(res.statusCode).toBe(204);
    expect(res.body).toEqual({});
    expect(userService.getUser).toHaveBeenCalledWith(1);
    expect(userService.deleteUser).toHaveBeenCalledWith(1);
  });

  test('Deve retornar 404 se usuário já deletado (estado inconsistente)', async () => {
    // Simula getUser retornando null (usuário não existe)
    userService.getUser.mockReturnValue(null);

    const res = await request(app).delete('/users/2');

    expect(res.statusCode).toBe(404);
    expect(res.body).toEqual({ detail: 'Usuário não encontrado' });
  });
});