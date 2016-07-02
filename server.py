
import socket
import struct
import sys
from httplib import HTTPResponse
from BaseHTTPServer import BaseHTTPRequestHandler
from StringIO import StringIO

import gtk
import gobject

LIB_ID = 'urn:schemas-upnp-org:device'
MCAST_GRP = '239.255.255.250'
MCAST_PORT = 1900

SERVICE_LOCS = {'python:1': '192.168.200.215:1234'}
SERVICE_LOCS = {'python:1': '192.168.200.215:1235'}

LOCATION_MSG = ('HTTP/1.1 200 OK\r\n' +
                'ST: %(library)s:%(service)s\r\n'
                'USN: %(usn)s\r\n'
                'Location: http://%(loc)s/info\r\n'
                'Cache-Control: max-age=900\r\n\r\n')


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


def interface_addresses(family=socket.AF_INET):
    for fam, _, _, _, sockaddr in socket.getaddrinfo('', None):
        if family == fam:
            yield sockaddr[0]


def server(timeout=5):

    #socket.setdefaulttimeout(timeout)

    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    sock.setsockopt(socket.IPPROTO_IP, socket.IP_MULTICAST_TTL, 2)
    sock.bind(('', MCAST_PORT))

    print sock

    mreq = struct.pack('4sl', socket.inet_aton(MCAST_GRP), socket.INADDR_ANY)
    sock.setsockopt(socket.IPPROTO_IP, socket.IP_ADD_MEMBERSHIP, mreq)
    while True:
        data, addr = sock.recvfrom(4096)
        print "RECV '%r'" % data
        request = Request(data)
        if not request.error_code and \
                request.command == 'M-SEARCH' and \
                request.path == '*' and \
                request.headers['ST'].startswith(LIB_ID) and \
                request.headers['MAN'] == '"ssdp:discover"':

            service = request.headers['ST'].split(':', 3)[-1]
            print 'service %r' % service
            if service in SERVICE_LOCS:
                loc = SERVICE_LOCS[service]
                usn = 'uuid:1'
                msg = LOCATION_MSG % dict(service=service, loc=loc, library=LIB_ID,usn=usn)
                print 'sending %r' % msg
                sock.sendto(msg, addr)

    return True


if __name__ == '__main__':
    if len(sys.argv) > 1 and 'client' in sys.argv[1]:
        client()
    else:
        server()
