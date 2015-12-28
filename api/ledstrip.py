#!/usr/bin/env python
# Source: https://github.com/Sh4d/LPD8806
# Modified by trizz

"""
LPD8806.py: Raspberry Pi library for the Adafruit LPD8806 RGB Strand

Provides the ability to drive a LPD8806 based strand of RGB leds from the Raspberry Pi

Colors are provided as RGB and converted internally to the strand's 7 bit values.

The leds are available here: http://adafruit.com/products/306

Wiring:
	Pi MOSI -> Strand DI
	Pi SCLK -> Strand CI

Make sure to use an external power supply to power the strand

Example:
	>> import LPD8806
	>> led = LPD8806.strand()
	>> led.fill(255, 0, 0)
"""
import time
from pprint import *

class LedStrip(object):
	def __init__(self, leds=32, dev="/dev/spidev0.1"):
		"""
		Variables:
			leds -- strand size
			dev -- spi device
		"""
		self.dev = dev
		self.spi = file(self.dev, "wb")
		self.leds = leds
		self.gamma = bytearray(256)
		self.buffer = [0 for x in range(self.leds)]
		self.wheelOffset = 0

		for led in range(self.leds):
			self.buffer[led] = bytearray(3)

		for i in range(256):
			# Color calculations from http://learn.adafruit.com/light-painting-with-raspberry-pi
			self.gamma[i] = 0x80 | int(pow(float(i) / 255.0, 2.5) * 127.0 + 0.5)

	def fill(self, r, g, b, start=0, end=0):
		"""
		Fill the strand (or a subset) with a single color
		"""
		if start < 0: raise NameError("Start invalid:" + str(start))
		if end == 0: end = self.leds
		if end > self.leds: raise NameError("End invalid: " + str(end))

		for led in range(start, end):
			self.buffer[led][0] = self.gamma[g]
			self.buffer[led][1] = self.gamma[r]
			self.buffer[led][2] = self.gamma[b]

		self.update()

	def set(self, pixel, r, g, b):
		"""
		Set a single LED a specific color
		"""
		self.buffer[pixel][0] = self.gamma[g]
		self.buffer[pixel][1] = self.gamma[r]
		self.buffer[pixel][2] = self.gamma[b]
		
		#self.update()


	def update(self):
		"""
		Flush the buffer to the strand
		"""
		for x in range(self.leds):
			self.spi.write(self.buffer[x])
			#self.spi.flush()
			
		self.spi.write(bytearray(b'\x00'))
		self.spi.flush()
