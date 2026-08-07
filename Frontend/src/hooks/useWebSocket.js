import { useEffect, useRef, useCallback, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client/dist/sockjs';

const WS_URL = '/ws';

export function useWebSocket() {
  const clientRef = useRef(null);
  // Map of destination string -> Map of callbackId -> callback function
  const listenersRef = useRef(new Map());
  // Map of destination string -> STOMP Subscription object
  const stompSubscriptionsRef = useRef(new Map());
  const [connected, setConnected] = useState(false);

  const setupStompSub = (destination) => {
    const client = clientRef.current;
    if (!client || !client.connected || stompSubscriptionsRef.current.has(destination)) {
      return;
    }

    const sub = client.subscribe(destination, (message) => {
      let parsedPayload;
      try {
        parsedPayload = JSON.parse(message.body);
      } catch {
        parsedPayload = message.body;
      }
      const callbacks = listenersRef.current.get(destination);
      if (callbacks) {
        callbacks.forEach((cb) => {
          try {
            cb(parsedPayload);
          } catch (e) {
            console.error('[WS] Error executing subscription callback', e);
          }
        });
      }
    });

    stompSubscriptionsRef.current.set(destination, sub);
  };

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      reconnectDelay: 3000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        setConnected(true);
        console.log('[WS] Connected');
        // Resubscribe all active destinations on reconnect
        listenersRef.current.forEach((callbacks, dest) => {
          if (callbacks.size > 0) {
            setupStompSub(dest);
          }
        });
      },
      onDisconnect: () => {
        setConnected(false);
        stompSubscriptionsRef.current.clear();
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
      stompSubscriptionsRef.current.forEach((sub) => {
        try {
          sub.unsubscribe();
        } catch {
          // ignore cleanup errors
        }
      });
      stompSubscriptionsRef.current.clear();
      client.deactivate();
    };
  }, []);

  const subscribe = useCallback((destination, callback) => {
    const id = Math.random().toString(36).substring(2, 9);
    
    if (!listenersRef.current.has(destination)) {
      listenersRef.current.set(destination, new Map());
    }
    const callbacksMap = listenersRef.current.get(destination);
    callbacksMap.set(id, callback);

    if (clientRef.current && clientRef.current.connected) {
      setupStompSub(destination);
    }

    return () => {
      const currentCallbacks = listenersRef.current.get(destination);
      if (currentCallbacks) {
        currentCallbacks.delete(id);
        if (currentCallbacks.size === 0) {
          listenersRef.current.delete(destination);
          const stompSub = stompSubscriptionsRef.current.get(destination);
          if (stompSub) {
            try {
              stompSub.unsubscribe();
            } catch {
              // ignore
            }
            stompSubscriptionsRef.current.delete(destination);
          }
        }
      }
    };
  }, []);

  return { connected, subscribe };
}
