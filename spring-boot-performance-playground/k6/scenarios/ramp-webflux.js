import http from 'k6/http';
import { check } from 'k6';

export const options = {
    scenarios: {
        ramp: {
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

const BASE_URL = 'http://localhost:8082';

export default function () {
    const res = http.get(`${BASE_URL}/api/products`);
    check(res, { 'status 200': (r) => r.status === 200 });
}
