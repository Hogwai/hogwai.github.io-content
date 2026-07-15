import http from 'k6/http';
import { check } from 'k6';

export const options = {
    vus: 5,
    duration: '30s',
};

const FILES = ['1MB.dat', '10MB.dat', '100MB.dat'];

export default function () {
    const fileName = FILES[__ITER % FILES.length];
    const res = http.get(`http://localhost:8081/api/files/${fileName}`);
    check(res, {
        'status 200': (r) => r.status === 200,
        'file size matches': (r) => r.body.length > 0,
    });
}
