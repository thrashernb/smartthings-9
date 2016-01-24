class LedStrip(object):
	def __init__(self, leds=32, dev="/dev/spidev0.1"):
		pass
	def fill(self, r, g, b, start=0, end=0):
		pass
	def set(self, pixel, r, g, b):
		pass
	def update(self):
		pass
	
from Tkinter import *
import colorsys
import time
import threading
SIZE = 40
NUM_LEDS=119
DELAY = 100
leds = []

for i in range(NUM_LEDS):
    rgb = colorsys.hsv_to_rgb(i/120.0, 1.0, 1.0)
    rgb = [ min(255, int(256*i)) for i in rgb ]
    leds.append(rgb)


class Application(Frame):
    def createWidgets(self):
        self.quit_button = Button(self)
        self.quit_button["text"] = "QUIT"
        self.quit_button["fg"]   = "red"
        self.quit_button["command"] =  self.quit
        self.quit_button.pack({"side": "bottom"})

        self.canvas = Canvas(self, width=(SIZE+5)*11, height=(SIZE+5)*11)
        self.canvas.pack({"side":"top"})
        self.draw_leds()
        self.after(DELAY, self.draw_leds)

    def draw_leds(self): 
        self.canvas.delete(ALL)
        #self.canvas.create_rectangle(1,1, (SIZE+5)*11, (SIZE+5)*11)
        for i, led in enumerate(leds):
            row = i / 11
            col = i % 11
            if row & 1:
                col = 10 - col
            color = "#" + ("{:02x}"*3).format(*led)
            self.canvas.create_rectangle(col*(SIZE+5)+4, row*(SIZE+5)+4, col*(SIZE+5)+SIZE, row*(SIZE+5)+SIZE, fill=color)
        self.after(DELAY, self.draw_leds)
        self.update_idletasks()
            
    def __init__(self, master=None):
        Frame.__init__(self, master)
        self.pack()
        self.createWidgets()

def update_leds():
    cnt = 0
    while True:
        time.sleep(0.100)
        cnt += 1
        #print cnt
        for i in range(NUM_LEDS):
            rgb = colorsys.hsv_to_rgb((i+cnt)/120.0%1, 1.0, 1.0)
            rgb = [ min(255, int(256*c)) for c in rgb ]
            leds[i] = rgb


thread = threading.Thread(target=update_leds)
thread.daemon = True
thread.start()

root = Tk()
app = Application(master=root)
app.mainloop()
root.destroy()
sys.exit(0)

