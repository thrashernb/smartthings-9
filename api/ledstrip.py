#!/usr/bin/env python
import itertools
from collections import namedtuple

__all__ = [
    "LedStrip",
    "Rgb",
    "Hsv"
]

Rgb = namedtuple('Rgb', 'r g b')
Hsv = namedtuple('Hsv', 'h s v')

class LedStrip(object):
    def __init__(self, num_leds=32, **kwargs):
        self.gamma = bytearray(256)
        
        self.leds = list(itertools.repeat(bytearray(3), times=num_leds))

        for i in range(256):
            # Color calculations from http://learn.adafruit.com/light-painting-with-raspberry-pi
            self.gamma[i] = int(pow(float(i) / 255.0, 2.5) * 255.0 + 0.5)

        super(LedStrip, self).__init__(**kwargs)

    def _to_bytearray(self, r,g,b):
        return bytearray((r,g,b))

    def fill(self, r, g, b, start=None, end=None):
        """
        Fill the strand (or a subset) with a single color
        """
        self[start:end] = (r,g,b)

    def set(self, idx, r, g, b):
        """
        Set a single LED a specific color
        """
        self[idx] = [r,g,b]

    def __setitem__(self, idx, value):
        if not isinstance(value, (list, tuple)):
            raise ValueError("Item must be set to a list or tuple")
        if isinstance(idx, (int, long)):
            value = map(self.gamma.__getitem__, value)
            value = self._to_bytearray(*value)
            self.leds[idx] = value
        elif isinstance(idx, slice):
            if isinstance(value[0], (int, long)):
                count = len(self.leds[idx])
                value = map(self.gamma.__getitem__, value)
                value = self._to_bytearray(*value)
                self.leds[idx] = itertools.repeat(value, times=count)
            else:
                value = [ map(self.gamma.__getitem__, v) for v in value ]
                value = [ self._to_bytearray(*v) for v in value ]
                self.leds[idx] = value
        else:
            raise TypeError("indexes must be integers, not %s" % idx.__class__.__name__)

    def update(self):
        pass
    
    def __getattr__(self, name):
        return getattr(self.leds, name)

    def __iter__(self):
        return self.leds.__iter__()

    def __len__(self):
        return len(self.leds)
