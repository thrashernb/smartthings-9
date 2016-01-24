#!/usr/bin/env python

from Tkinter import *
import colorsys
import time
import threading
import itertools
import sys
import os
from ledstrip import LedStrip
from main import main

__all__ = [
    "FakeStrip"
]

SIZE=40
DELAY=100


class MyApp(Frame):
    def __init__(self, strip=None, master=None):
        Frame.__init__(self, master)
        self.strip = strip
        self.pack()
        self.createWidgets()

    def quit(self):
        print "QUIT"
        os._exit(0)

    def createWidgets(self):
        self.quit_button = Button(self)
        self.quit_button["text"] = "Quit"
        #self.quit_button["fg"]   = "red"
        self.quit_button["command"] =  self.quit
        self.quit_button.pack({"side": "bottom"})

        self.canvas = Canvas(self, width=(SIZE+5)*11, height=(SIZE+5)*11)
        self.canvas.pack({"side":"top"})
        self.draw_leds()
        self.after(DELAY, self.draw_leds)

    def draw_leds(self):
        self.canvas.delete(ALL)
        #self.canvas.create_rectangle(1,1, (SIZE+5)*11, (SIZE+5)*11)
        for i, led in enumerate(self.strip):
            row = i / 11
            col = i % 11
            if row & 1:
                col = 10 - col
            color = "#" + ("{:02x}"*3).format(*led)
            self.canvas.create_rectangle(col*(SIZE+5)+4, row*(SIZE+5)+4, col*(SIZE+5)+SIZE, row*(SIZE+5)+SIZE, fill=color)
        self.after(DELAY, self.draw_leds)
        self.update_idletasks()

class FakeStrip(LedStrip):
    def _run(self):
        root = Tk()
        app = MyApp(strip=self, master=root)
        app.mainloop()
        sys.exit(0)

    def __init__(self, **kwargs):
        super(FakeStrip, self).__init__(**kwargs)
        self.gamma = range(256)
        thread = threading.Thread(target=self._run)
        thread.daemon = True
        thread.start()

if __name__ == "__main__":
    strip = FakeStrip(num_leds=121)
    main(strip)

