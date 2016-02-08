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

from ledstrip import LedStrip


class Lpd8806(LedStrip):
    def __init__(self, dev="/dev/spidev0.1", **kwargs):
        super(Lpd8806, self).__init__(**kwargs)
        self.dev = dev
        self.spi = file(self.dev, "wb")
        self.gamma = bytearray(256)

        for i in range(256):
            # Color calculations from http://learn.adafruit.com/light-painting-with-raspberry-pi
            self.gamma[i] = 0x80 | int(pow(float(i) / 255.0, 2.5) * 127.0 + 0.5)


    def _to_bytearray(self, r,g,b):
        return bytearray([g,r,b])

    def update(self):
        """
        Flush the leds to the strand
        """
        for x in self.leds:
            self.spi.write(x)

        self.spi.write(bytearray(b'\x00'))
        self.spi.flush()

