import { useEffect, useRef, useCallback, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client/dist/sockjs';

const WS_URL = '/ws';

export function useWebSocket() {
  const clientRef = useRef(null);
  const subscriptionsRef = useRef(new Map());
  const [connected, setConnected] = useState(false);

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      reconnectDelay: 3000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        setConnected(true);
        console.log('[WS] Connected');
        // Resubscribe after reconnect
        subscriptionsRef.current.forEach((callback, dest) => {
          client.subscribe(dest, (message) => {
            try {
              callback(JSON.parse(message.body));
            } catch {
              callback(message.body);
            }
          });
        });
      },
      onDisconnect: () => {
        setConnected(false);
        console.log('[WS] Disconnected');
      },
      onStompError: (frame) => {
        console.error('[WS] STOMP error', frame);
        setConnected(false);
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
    };
  }, []);

  const subscribe = useCallback((destination, callback) => {
    subscriptionsRef.current.set(destination, callback);

    const client = clientRef.current;
    if (client && client.connected) {
      const sub = client.subscribe(destination, (message) => {
        try {
          callback(JSON.parse(message.body));
        } catch {
          callback(message.body);
        }
      });
      return () => {
        sub.unsubscribe();
        subscriptionsRef.current.delete(destination);
      };
    }

    return () => {
      subscriptionsRef.current.delete(destination);
    };
  }, []);

  return { connected, subscribe };
}
