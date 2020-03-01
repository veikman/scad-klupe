# `scad-klupe`: Threaded fasteneas for `scad-clj`

This is a Clojure library for use in CAD work with Matthew Farrell’s
[`scad-clj`](https://github.com/farrellm/scad-clj). It provides models of
threaded fasteners, including nuts and bolts.

[![Clojars
Project](https://img.shields.io/clojars/v/scad-klupe.svg)](https://clojars.org/scad-klupe)

The Lojban word *klupe* refers to a screw or bolt.

Models produced by this library are neither perfectly accurate with respect to
standards, nor engineered for ease of printing. Their main purpose is to form
negative space: Relatively simple shapes used to carve out screw holes, nut
pockets and similar cavities in 3D-printable designs. These holes would then be
filled by ordinary steel nuts and bolts in the assembly of your product.

`scad-klupe` is compatible with
[`scad-tarmi`](https://github.com/veikman/scad-tarmi) compensators to deal with
printer inaccuracy.

## Usage

The main interface is the [`iso`](src/scad_klupe/iso.clj) module. If in your
`ns` declaration you `(:require [scad-klupe.iso :refer [nut]])`, you can then
call `(nut {:m-diameter 6})` for an internal threaded approximation of an ISO
262 M6 hex nut. Its height and external diameter are inferred from the
standard, unless you pass overrides. For a matching 25 mm threaded bolt, call
`(bolt {:m-diameter 6 :total-length 25})`.

If you intend to model an M6 nut to carve out negative space for a real nut,
the correct call would be `(nut {:m-diameter 6 :negative true})`, a less
complicated shape without a hole. The `bolt` function also takes a `negative`
parameter, which elides the drive from the head, but the negative of a bolt is
still threaded by default. If you just want a straight hole that you intend to
tap instead of printing the threading, try passing `:include-threading false`
to the `bolt` function.

## Showcase

You will need to use `scad-clj`, for instance via
[`scad-app`](https://github.com/veikman/scad-app), to produce OpenSCAD code for
the objects you model with `scad-klupe`. Check the [showcase
code](src/showcase/core.clj) for examples of how to do that. A selection of the
models produced by that code are shown [here](showcase/stl).

## Contributing

Volunteers are welcome to submit patch sets for review, including additional
ISO standard data, more types of heads and drives, and non-metric standards.

## Acknowledgements

`scad-klupe` was broken out of `scad-tarmi` in 2020 while making
backwards-incompatible changes to the API for programmatic use and taking a
more consistent stance on bundled defaults.

The thread-drawing function (`scad-klupe.base/threading`) is a reimplementation
in Clojure of a corresponding function in `polyScrewThread_r1.scad`, created by
*aubenc* [at Thingiverse](http://www.thingiverse.com/thing:8796) and released
by the author into the public domain.

## License

Copyright © 2020 Viktor Eikman

This software is distributed under the [Eclipse Public License](LICENSE-EPL),
(EPL) v2.0 or any later version thereof. This software may also be made
available under the [GNU General Public License](LICENSE-GPL) (GPL), v3.0 or
any later version thereof, as a secondary license hereby granted under the
terms of the EPL.
