import unittest
from ledstrip import LedStrip

class TestStringMethods(unittest.TestCase):
    def test_setitem(self):
        l = LedStrip(10)
        l.gamma = range(256)

        l[0] = [127]*3
        self.assertEqual(l.leds[0], bytearray([127]*3))

    def test_setslice(self):
        l = LedStrip(10)
        l.gamma = range(256)

        l[0:1] = [[127]*3 for i in range(3)]
        self.assertEqual(l.leds[0], bytearray([127]*3))

    def test_set(self):
        l = LedStrip(10)
        l.gamma = range(256)

        l.set(1, 127,127,127)
        self.assertEqual(l.leds[1], bytearray([127]*3))

    def test_fill(self):
        l = LedStrip(10)
        l.gamma = range(256)

        l.fill(127,127,127, 1, 5)
        l.fill(255,127,127, 1, 2)
        #self.assertEqual(l.leds[1], bytearray([127]*3))

if __name__ == '__main__':
    unittest.main()
