from flask import Flask
from flask import request
from flask_json import FlaskJSON, JsonError, json_response, as_json
import ledstrip
#import fakestrip as ledstrip
import colorsys

NUM_LEDS=120

app = Flask(__name__)
FlaskJSON(app)

strip = ledstrip.LedStrip(NUM_LEDS)

state = {
    "switch": "off",
    "level": 100,
    "level2": 100,

    "color_raw": {"hue": 0.0, "saturation": 0.0},
    "color2_raw": {"hue": 0.0, "saturation": 0.0},
    "mode": "single_color"
}


def update(new_data = {}):
    global state
    state.update(new_data)
    print "\nState = %r" % (state)
    try:
        if state["switch"] == "off":
            strip.fill(0,0,0)
        elif state["mode"] == "rainbow":
            for i in range(0, NUM_LEDS):
                rgb = colorsys.hsv_to_rgb(i/float(NUM_LEDS), state["level2"]/100.0, state["level"]/100.0)
                r, g, b = [ min(255, int(256*v)) for v in rgb ]
                strip.set(i, r,g,b)
            strip.update()
        elif state["mode"] == "single_color":
            rgb = colorsys.hsv_to_rgb(state["color_raw"]["hue"]/100.0, state["color_raw"]["saturation"]/100.0, state["level"]/100.0)
            r, g, b = [ min(255, int(256*i)) for i in rgb ]
            strip.fill(r,g,b)
        elif state["mode"] == "dual_color":
            rgb = colorsys.hsv_to_rgb(state["color_raw"]["hue"]/100.0, state["color_raw"]["saturation"]/100.0, state["level"]/100.0)
            r, g, b = [ min(255, int(256*i)) for i in rgb ]
            strip.fill(r,g,b, 0, NUM_LEDS/2)

            rgb = colorsys.hsv_to_rgb(state["color2_raw"]["hue"]/100.0, state["color2_raw"]["saturation"]/100.0, state["level2"]/100.0)
            r2, g2, b2 = [ min(255, int(256*i)) for i in rgb ]
            strip.fill(r2,g2,b2, NUM_LEDS/2)

    except Exception as e:
        print "ERROR: %r" % (e)

MODES = ["single_color", "dual_color", "rainbow"]
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

if __name__ == "__main__":
    update()
    app.run(host="0.0.0.0")

