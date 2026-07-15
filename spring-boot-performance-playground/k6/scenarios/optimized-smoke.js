import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_OPTIONS } from '../config/options.js';

export const options = {
    ...BASE_OPTIONS,
    vus: 10,
    duration: '30s',
};

const BASE_URL = 'http://localhost:8081';

export default function () {
    const endpoints = [
        `${BASE_URL}/api/users/1`,
        `${BASE_URL}/api/stats`,
        `${BASE_URL}/api/dashboard/1`,
        `${BASE_URL}/api/products`,
    ];

    for (const url of endpoints) {
        const res = http.get(url);
        check(res, {
            'status is 200': (r) => r.status === 200,
            'response time < 50ms': (r) => r.timings.duration < 50,
        });
        sleep(0.1);
    }
}
