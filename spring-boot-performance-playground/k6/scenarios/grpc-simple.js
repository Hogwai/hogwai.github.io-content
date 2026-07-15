import grpc from 'k6/net/grpc';
import { check } from 'k6';

const client = new grpc.Client();
client.load(['../../common/src/main/proto'], 'user_service.proto');

export default function () {
    if (__ITER == 0) {
        client.connect('localhost:9090', { plaintext: true });
    }
    const response = client.invoke('com.hogwai.perf.common.proto.UserService/GetUser', { id: 1 });
    check(response, { 'gRPC user OK': (r) => r.status === grpc.StatusOK });
}

export function teardown() {
    client.close();
}
