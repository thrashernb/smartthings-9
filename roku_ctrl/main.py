#!/usr/bin/env python

from flask import Flask
from flask import request
from flask import Response
from flask_json import FlaskJSON, JsonError, json_response, as_json
import sys
import threading
import requests
import time

from roku import Roku
UUID = 'uuid:roku_tv'

callback_url = None

def push(state):
    if not callback_url:
        return
    #url = "http://192.168.200.131:39500/roku_update/"
    headers = {
        'SID':UUID,
    }
    requests.post(callback_url, json=state, headers=headers)

app = Flask(__name__)
FlaskJSON(app)

@app.route("/info")
@as_json
def info():
    return {
        'usn': UUID,
        'model': 'TCL',
        'serial': 'serial',
        'name': 'Roku TV',
        'device_type': 'REST Switch',
    }


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
    ret = update(data)
    return ret


@app.route("/subscribe", methods=["SUBSCRIBE"])
@as_json
def subscribe():
    global callback_url
    callback_url = request.headers["CALLBACK"]
    return {}


state = {
    #"switch": "off",
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

def update(new_data = {}):
    global state
    try:
        if (new_data["switch"] == "on") != roku_control.on:
            roku_control.power()
    except KeyError:
        pass
    except Exception as e:
        print "ERROR: %r" % (e)
    return {}


class RokuControl(object):
    def __init__(self):
        self.device = None
        self.thread = threading.Thread(target=self._monitor)
        self.thread.daemon = True
        self.thread.start()
        self.update = False


    def power(self):
        try:
            self.device.power()
            self.update = True
            self.on = not self.on
        except AttributeError:
            global state
            push(state)
    
    @property
    def on(self):
        global state
        try:
            return state["switch"] == "on"
        except KeyError:
            return None

    @on.setter
    def on(self, value):
        global state
        cur = self.on
        state["switch"] = ["off", "on"][value]
        if value != cur:
            push(state)

    def _monitor(self):
        curState = ""
        delay = 15
        while True:
            if not self.device:
                devices = Roku.discover(timeout=10)
                try:
                    self.device = devices[0]
                except IndexError:
                    delay = 20
                    self.on = False

            if self.device:
                try:
                    info = self.device.device_info
                except requests.exceptions.ConnectionError:
                    self.device = None
                    self.on = False
                    delay = 0
                    continue
                self.on = info.power_mode == 'PowerOn'
                delay = 15
            #print time.time(), self.on, delay
            for i in range(delay):
                if self.update:
                    print "UPDATE"
                    self.update = False
                    break
                time.sleep(1)


def main():
    app.run(host="0.0.0.0",port=0xf00d)
    sys.exit(0)

if __name__ == "__main__":
    roku_control = RokuControl()
    main()



