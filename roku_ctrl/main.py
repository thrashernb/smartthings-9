from flask import Flask
from flask import request
from flask_json import FlaskJSON, JsonError, json_response, as_json
import sys
import threading


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
    ret = {k:v for k,v in data.iteritems() if not k.startswith("_")}
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
    state.update(new_data)
    print "\nState = %r" % (state)
    try:
        do_update(state)
    except Exception as e:
        print "ERROR: %r" % (e)
    save()
    return state

def main():
    load()
    update()
    app.run(host="0.0.0.0",port=0xf00d)
    sys.exit(0)

if __name__ == "__main__":
    main(strip)



