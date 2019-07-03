# DiyLed Binding

This binding is only working in combination with the DiyLed system!
Server and clients can be found on the DiyLed project page on my GitHub profile.

## Supported Things

Since I only needed to control my DiyLed Lights there is currently only one thing to control the basics of a DiyLed Light.

## Discovery

_There is no discovery yet. May implement this later._

## Binding Configuration

_Config is not yet present or implemented._
IP of the server has to be filled into every things property!

## Thing Configuration

A thing currently only has to NEEDED paramters:
IP : The IP address of the DiyLed Server
Name : THe Name of the light that is registered with the EXACT SAME name in the LedServer

## Channels

| channel       | type   | description                      |
|---------------|--------|----------------------------------|
| power         | Switch | State of the light, e.g. on/off  |
| brightness    | Dimmer | Brightness of the light          |
