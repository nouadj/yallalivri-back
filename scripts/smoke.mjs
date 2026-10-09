import assert from 'node:assert/strict';
const base = (process.env.SMOKE_API_URL ?? 'http://localhost:8080/api').replace(/\/$/, '');
const suffix = Date.now();
const ids = []; let admin; let orderId;
async function request(path, token, method = 'GET', body) {
  const response = await fetch(base + path, {
    method, headers: { Accept: 'application/json', ...(token ? { Authorization: 'Bearer ' + token } : {}), ...(body ? { 'Content-Type': 'application/json' } : {}) },
    body: body ? JSON.stringify(body) : undefined, signal: AbortSignal.timeout(15000)
  });
  const raw = await response.text();
  if (!response.ok) throw new Error(method + ' ' + path + ': ' + response.status + ' ' + raw);
  return raw ? JSON.parse(raw) : undefined;
}
async function login(email, password) { return (await request('/auth/login', null, 'POST', { email, password })).token; }
try {
  admin = await login(process.env.SMOKE_ADMIN_EMAIL ?? 'admin@yallalivri.dz', process.env.SMOKE_ADMIN_PASSWORD ?? 'admin1234');
  for (const role of ['STORE', 'COURIER']) {
    const user = await request('/users', admin, 'POST', { name: 'Smoke ' + role, email: role.toLowerCase() + suffix + '@example.com', password: 'smoke1234', role, latitude: 36.75, longitude: 3.05 });
    ids.push(user.id);
  }
  const store = await login('store' + suffix + '@example.com', 'smoke1234');
  const courier = await login('courier' + suffix + '@example.com', 'smoke1234');
  const order = await request('/orders', store, 'POST', { storeId: ids[0], customerName: 'Smoke client', customerPhone: '0555000000', customerAddress: 'Alger', amount: 1000, deliveryFee: 200 });
  orderId = order.id; assert.equal(order.status, 'CREATED');
  const inbox = await request('/notifications', courier);
  const notice = inbox.items.find(item => item.orderId === orderId);
  assert(notice && notice.type === 'ORDER_AVAILABLE');
  await request('/notifications/' + notice.id + '/read', courier, 'PATCH');
  assert((await request('/notifications', courier)).items.find(item=>item.id === notice.id).readAt);
  await request('/users/' + ids[1] + '/location', courier, 'PATCH', { latitude: 36.75, longitude: 3.05 });
  assert((await request('/orders/status/CREATED?courierId=' + ids[1], courier)).some(o => o.id === orderId));
  await request('/orders/' + orderId + '/assign', courier, 'PATCH', { courierId: ids[1] });
  await request('/orders/' + orderId + '/status', courier, 'PATCH', { status: 'SHIPPED' });
  await request('/orders/' + orderId + '/status', courier, 'PATCH', { status: 'DELIVERED' });
  assert.equal((await request('/orders/' + orderId, store)).status, 'DELIVERED');
  assert((await request('/orders/courier/' + ids[1], courier)).some(o => o.id === orderId && o.status === 'DELIVERED'));
  const updates = (await request('/notifications', store)).items;
  assert.deepEqual(updates.map(item=>item.type), ['ORDER_DELIVERED','ORDER_SHIPPED','ORDER_ASSIGNED']);
  await request('/notifications/read-all', store, 'POST');
  assert.equal((await request('/notifications', store)).unreadCount, 0);
  console.log('Parcours API et notifications réussi : création → assignation → expédition → livraison → lecture des alertes.');
} finally {
  if (orderId) await request('/orders/' + orderId, admin, 'DELETE');
  for (const id of ids) await request('/users/' + id, admin, 'DELETE');
}
