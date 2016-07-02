
import socket
import struct
import sys
from httplib import HTTPResponse
from BaseHTTPServer import BaseHTTPRequestHandler
from StringIO import StringIO
import json

MCAST_GRP = '239.255.255.250'
MCAST_PORT = 1900

LOCATION_MSG = ('HTTP/1.1 200 OK\r\n' +
                'ST: %(st)s\r\n'
                'USN: %(uuid)s\r\n'
                'Location: %(location)s\r\n'
                'Cache-Control: max-age=900\r\n\r\n')

DEVICES = json.load(open('devices.json'))
print DEVICES

class Request(BaseHTTPRequestHandler):
    def __init__(self, request_text):
        self.rfile = StringIO(request_text)
        self.raw_requestline = self.rfile.readline()
        self.error_code = self.error_message = None
        self.parse_request()

    def send_error(self, code, message):
        self.error_code = code
        self.error_message = message


class Response(HTTPResponse):
    def __init__(self, response_text):
        self.fp = StringIO(response_text)
        self.debuglevel = 0
        self.strict = 0
        self.msg = None
        self._method = None
        self.begin()

def server(timeout=5):
    #socket.setdefaulttimeout(timeout)
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    sock.setsockopt(socket.IPPROTO_IP, socket.IP_MULTICAST_TTL, 2)
    sock.bind(('', MCAST_PORT))

    mreq = struct.pack('4sl', socket.inet_aton(MCAST_GRP), socket.INADDR_ANY)
    sock.setsockopt(socket.IPPROTO_IP, socket.IP_ADD_MEMBERSHIP, mreq)
    while True:
        data, addr = sock.recvfrom(4096)
        print "RECV '%r'" % data
        request = Request(data)
        if not request.error_code and \
                request.command == 'M-SEARCH' and \
                request.path == '*' and \
                request.headers['MAN'] == '"ssdp:discover"':

            try:
                st = request.headers['ST']
                devs = DEVICES[st]
                print devs
                for uuid, location in DEVICES[st].iteritems():
                    msg = LOCATION_MSG % dict(st=st, uuid=uuid, location=location)
                    print 'sending %r' % msg
                    sock.sendto(msg, addr)
            except:
                pass

    return True


if __name__ == '__main__':
    server()
