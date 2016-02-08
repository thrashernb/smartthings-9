from flask import Flask
from flask import request
from flask_json import FlaskJSON, JsonError, json_response, as_json
import sys
#import ledstrip
#import fakestrip as ledstrip
import colorsys

MODES = ["single_color", "dual_color", "rainbow"]

app = Flask(__name__)
FlaskJSON(app)

#strip = ledstrip.LedStrip(NUM_LEDS)

def color_to_rgb(color_idx):
    if color_idx == 0:
        name = "color_raw"
        level = "level"
    else:
        name = "color2_raw"
        level = "level2"
    rgb = colorsys.hsv_to_rgb(state[name]["hue"]/100.0, state[name]["saturation"]/100.0, state[level]/100.0)
    return [ min(255, int(256*i)) for i in rgb ]


@app.route("/status")
@as_json
def get_status():
    global state
    ret = {k:v for k,v in state.iteritems() if not k.endswith("raw")}
    return ret

@app.route("/control", methods=["POST"])
@as_json
def control():
    global state
    data = request.get_json(force=True)
    update(data)
    ret = {k:v for k,v in data.iteritems() if not k.endswith("raw")}
    print "Returning %r" % (ret)
    return ret

@app.route("/next_mode", methods=["POST"])
@as_json
def next_mode():
    global state
    idx = MODES.index(state["mode"])
    idx = (idx+1) % len(MODES)
    state["mode"] = MODES[idx]
    update()
    ret= {"mode": state["mode"]}
    print "Returning %r" % (ret)
    return ret






state = {
    "switch": "off",
    "level": 100,
    "level2": 100,
    "level3": 50,
    "color_raw": {"hue": 0.0, "saturation": 0.0},
    "color2_raw": {"hue": 0.0, "saturation": 0.0},
    "mode": "single_color"
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
    state.update(new_data)
    print "\nState = %r" % (state)
    try:
        if state["switch"] == "off":
            strip.fill(0,0,0)
        elif state["mode"] == "rainbow":
            for i in range(0, len(strip)):
                rgb = colorsys.hsv_to_rgb(i/float(len(strip)), state["level2"]/100.0, state["level"]/100.0)
                r, g, b = [ min(255, int(256*v)) for v in rgb ]
                strip.set(i, r,g,b)
            strip.update()
        elif state["mode"] == "single_color":
            r,g,b = color_to_rgb(0)
            strip.fill(r,g,b)
        elif state["mode"] == "dual_color":
            split = int(len(strip)*(state["level3"]/100.0))
            r,g,b = color_to_rgb(0)
            strip.fill(r,g,b, 0, split)

            r,g,b = color_to_rgb(1)
            strip.fill(r,g,b, split)
    except Exception as e:
        print "ERROR: %r" % (e)
    strip.update()
    save()


def main(strip):
    load()
    globals()["strip"] = strip
    update()
    app.run(host="0.0.0.0",port=5001)
    sys.exit(0)


if __name__ == "__main__":
    import lpd8806_strip
    strip = lpd8806_strip.Lpd8806(num_leds=120)
    main(strip)



