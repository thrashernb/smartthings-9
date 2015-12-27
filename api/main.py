from flask import Flask
from flask import request
from flask_json import FlaskJSON, JsonError, json_response, as_json

app = Flask(__name__)
FlaskJSON(app)


state = {
    "switch": "off",
    "level": 100,
    "color": "#ffffff",
    "switch2": "off",
    "level2": 100,
    "color2": "#ffffff"
}

@app.route("/status")
@as_json
def get_status():
    global state
    return state

@app.route("/control", methods=["POST"])
@as_json
def control():
    global state
    data = request.get_json(force=True)
    print repr(data)
    print data
    state.update(data)
    print state
    if state["switch"] == "off":
        state["switch2"] = "off"
    return state

if __name__ == "__main__":
    app.run(host="0.0.0.0")
