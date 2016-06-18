#!/usr/bin/env python

from flask import Flask
from flask import request
from flask_json import FlaskJSON, JsonError, json_response, as_json
import sys
import threading
import requests
import time

from roku import Roku

app = Flask(__name__)
FlaskJSON(app)

@app.route("/status")
@as_json
def get_status():
    global state
    ret = {k:v for k,v in state.iteritems() if not k.startswith("_")}
    return ret

@app.route("/control", methods=["POST"])
@as_json
def control():
    global state
    data = request.get_json(force=True)
    update(data)
    ret = {k:v for k,v in state.iteritems() if not k.startswith("_")}
    print "Returning %r" % (ret)
    return ret


state = {
    "switch": "off",
}

import json
def load():
    try:
        print "loading"
        with open("state.json") as f:
            new_state = json.load(f)
        print "Loaded", new_state
        state.update(**new_state)
    except:
        print "Exception"
        pass

def save():
    global state
    with open("state.json", "w") as f:
        json.dump(state, f, indent=4)

def do_update(state):
    pass

def update(new_data = {}):
    global state
    print "\nState = %r" % (state)
    try:
        do_update(new_data)
    except Exception as e:
        print "ERROR: %r" % (e)
    return state


def do_update(new_data):
    global roku_control
    print new_data
    try:
        if new_data["switch"] == "off":
            roku_control.power()
            state.update(new_data)
    except KeyError:
        pass
    print 'State = %r, new = %r' % (state, new_data)

import threading
class RokuControl(object):
    def __init__(self):
        self.device = None
        self.thread = threading.Thread(target=self._monitor)
        self.thread.daemon = True
        self.thread.start()


    def power(self):
        try:
            self.device.power()
        except AttributeError:
            pass
    
    @property
    def on(self):
        global state
        return state["switch"] == "on"

    @on.setter
    def on(self, value):
        global state
        state["switch"] = ["off", "on"][value]
        print "RETRIEVED STATE = %r" %(state)

    def _monitor(self):
        curState = ""
        while True:
            delay = 30
            if not self.device:
                devices = Roku.discover(timeout=10)
                try:
                    self.device = devices[0]
                except IndexError:
                    self.on = False

            if self.device:
                try:
                    info = self.device.device_info
                except requests.exceptions.ConnectionError:
                    self.device = None
                    self.on = False
                    continue
                self.on = info.power_mode == 'PowerOn'
                if not self.on:
                    delay = 90
            #print time.time(), self.on, delay
            time.sleep(delay)

def main():
    update()
    app.run(host="0.0.0.0",port=0xf00d)
    sys.exit(0)

if __name__ == "__main__":
    roku_control = RokuControl()
    main()



