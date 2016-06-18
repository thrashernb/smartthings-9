#!/usr/bin/env python

import socket
import requests
from requests.adapters import HTTPAdapter
from requests.packages.urllib3 import PoolManager, HTTPConnectionPool

try:
    from http.client import HTTPConnection
except ImportError:
    from httplib import HTTPConnection

class MyAdapter(HTTPAdapter):
    def init_poolmanager(self, connections, maxsize, **kwargs):
        self.poolmanager = MyPoolManager(num_pools=connections,
                                         maxsize=maxsize, **kwargs)


class MyPoolManager(PoolManager):
    def _new_pool(self, scheme, host, port):
        # Important!
        if scheme == 'http':
            return MyHTTPConnectionPool(host, port, **self.connection_pool_kw)
        return super(PoolManager, self)._new_pool(self, scheme, host, port)


class MyHTTPConnectionPool(HTTPConnectionPool):
    def _new_conn(self):
        #self.num_connections += 1
        return MyHTTPConnection(host=self.host,
                            port=self.port,
                            strict=self.strict)

class MyHTTPConnection(HTTPConnection):
    def connect(self):
        #global sock
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEPORT, 1)
        sock.bind(('0.0.0.0', 0xf00d))
        self.sock = sock
        print self.host, self.port
        sock.connect((self.host, self.port))
        if self._tunnel_host:
            self._tunnel()
        

url = "http://192.168.200.131:39500/roku_update/"
import sys
data = {"switch":sys.argv[1]}
headers = {
    'NT':'upnp:event',
    'NTS':'upnp:propchange',
    'SID':'uuid:roku-0',
    'SEQ':3,
}
s = requests.Session()
s.mount('http://', MyAdapter())
print s.request('NOTIFY', url, json=data, headers=headers)
