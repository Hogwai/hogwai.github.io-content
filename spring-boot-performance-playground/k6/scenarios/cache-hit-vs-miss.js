import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    vus: 50,
    duration: '60s',
};

const BASE_URL = 'http://localhost:8081';

export default function () {
    // Mostly hot keys (user 1-10) = high cache hit ratio
    const id = (__VU * 100 + __ITER) % 100 + 1;
    const res = http.get(`${BASE_URL}/api/users/${id}`);
    check(res, { 'status 200': (r) => r.status === 200 });
    sleep(0.05);
}
