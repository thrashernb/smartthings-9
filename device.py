#!/usr/bin/env python

from flask import Flask
from flask import request
from flask import Response
from flask_json import FlaskJSON, JsonError, json_response, as_json
import sys
import threading
import requests
import time
import json


app = Flask(__name__)
FlaskJSON(app)

@app.route("/info")
@as_json
def info():
    return {
        'usn': 'uuid:1',
        'model': 'random_model',
        'serial': '1234',
        'name': 'randomName',
        'device_type': 'Generic REST Device',
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
def subscribe():
    resp = Response()
    resp.headers['SID'] = 'uuid:roku-%s' %(uuid)
    return resp


state = {
    'one': 1,
    'two': 2,
    #"switch": "off",
}

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
        pass
    except KeyError:
        pass
    except Exception as e:
        print "ERROR: %r" % (e)
    return {}

def main():
    app.run(host="0.0.0.0",port=int(sys.argv[1]))
    sys.exit(0)

if __name__ == "__main__":
    main()



