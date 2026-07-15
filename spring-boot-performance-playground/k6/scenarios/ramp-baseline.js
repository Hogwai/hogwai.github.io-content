import http from 'k6/http';
import { check } from 'k6';

export const options = {
    scenarios: {
        baseline: {
            executor: 'ramping-vus',
            startVUs: 10,
            stages: [
                { duration: '30s', target: 100 },
                { duration: '30s', target: 500 },
                { duration: '30s', target: 1000 },
                { duration: '30s', target: 0 },
            ],
        },
    },
};

const BASE_URL = 'http://localhost:8080';

export default function () {
    const endpoints = [
        `${BASE_URL}/api/users/1`,
        `${BASE_URL}/api/stats`,
        `${BASE_URL}/api/products`,
    ];
    const res = http.get(endpoints[__ITER % endpoints.length]);
    check(res, { 'status 200': (r) => r.status === 200 });
}
