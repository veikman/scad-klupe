;;; Generic logic regarding threaded fasteners.

;;; Use of this module as an interface is deprecated. If you want to use
;;; scad-klupe for non-metric/non-ISO threading, please submit an expansion
;;; module akin to iso.clj.

(ns scad-klupe.base
  (:require [clojure.spec.alpha :as spec]
            [scad-clj.model :as model]
            [scad-tarmi.core :refer [sin cos τ long-hex-diagonal]]
            [scad-klupe.schema.base :as base]))


;;;;;;;;;;;;;;;;;;;;;;;;
;; INTERNAL FUNCTIONS ;;
;;;;;;;;;;;;;;;;;;;;;;;;

(defn- distance-to-end
  "Close over a function that computes the absolute distance to the nearest end
  of an object of passed length. This is used to control tapering."
  [length]
  {:pre [(number? length)]}
  (fn [coordinate] (min coordinate (- length coordinate))))

(defn- flat-end-z
  "Close over a function that limits a z-coordinate to fall within the passed
  length of an object. This is used to prevent lengthwise overshoot of
  threading, except where explicitly passed to this function."
  [limit & {:keys [overshoot] :or {overshoot 0}}]
  {:pre [(number? limit)]}
  (let [floor (- overshoot)
        ceiling (+ limit overshoot)]
    (fn [coordinate] (max floor (min ceiling coordinate)))))


;;;;;;;;;;;;;;;;;;;;;;;;;
;; INTERFACE FUNCTIONS ;;
;;;;;;;;;;;;;;;;;;;;;;;;;

;; The following is intended primarily as an interface for other,
;; standards-specific parts of scad-klupe.

(defn shank-section-lengths
  "Determine the lengths of the unthreaded and threaded parts of a bolt.
  These can be explicit in the parameters to this function, or else they
  are inferred from the total length and the length of the head."
  [{:keys [total unthreaded threaded head] :as parameters}]
  {:pre [(spec/valid? ::base/bolt-length-parameters parameters)]}
  (case (map some? [total unthreaded threaded])
    [true  true  true ] [unthreaded threaded]
    [true  true  false] [unthreaded (- total unthreaded head)]
    [true  false false] [0 (- total head)]
    [false false false] [0 0]  ; Case is contradictory to spec.
    [false false true ] [0 threaded]
    [false true  true ] [unthreaded threaded]
    [true  false true ] [(- total threaded head) threaded]
    [false true  false] [unthreaded 0]))

(defn bolt-length
  "Predict the overall length of a bolt, including the head.
  Use shank-section-lengths partly for its sanity check on the inputs."
  [{:keys [total head] :as parameters}]
  (let [[unthreaded threaded] (shank-section-lengths parameters)]
    (if total total (+ head unthreaded threaded))))

(defn bolt-inner-radius
  "The inner radius of a piece of threading, meaning the distance from the
  center of a threaded rod to the bottom of a valley in its threading.

  The ‘angle’ parameter controls the slope of each peak of the threading, in
  radians. The value will determine the ratio between the inner and outer
  diameters of a bolt."
  [{:keys [outer-diameter pitch angle]}]
  {:pre [(number? outer-diameter) (number? pitch) (number? angle)]}
  (- (/ outer-diameter 2) (/ pitch (* 2 (/ (cos angle) (sin angle))))))

(defn threading
  "A model of threading, as on a screw.
  This model has a solid interior, thus needing no union with an inner
  cylinder. Its grooves are not flattened by default.

  The ‘outer-diameter’ argument corresponds to the nominal major diameter of
  an ISO 262 thread, but is not limited here to any ISO standard.

  The ‘pitch’ describes the interval from one peak to the next, lengthwise.

  The ‘resolution’ parameter affects the number of edges of the thread per
  revolution of the helix: A higher number gives a more detailed model."
  [{:keys [outer-diameter length pitch resolution taper-fn]
    :or {resolution 1, taper-fn (fn [& _] (fn [& a] a))}
    :as options}]
  {:pre [(number? outer-diameter)
         (number? length)
         (number? pitch)]}
  (let [rₒ (/ outer-diameter 2)
        rᵢ (bolt-inner-radius options)
        n-revolutions (+ (int (/ length pitch)) 2)  ; Amount of full turns.
        n-edges (Math/floor (* resolution τ rₒ))  ; Edges per revolution.
        θ (/ τ n-edges)  ; Angle describing each outer edge.
        Δz (/ pitch n-edges)  ; Lengthwise rise per edge.
        tape (taper-fn rᵢ rₒ length)
        turner
          (fn [[base-radius edge base-z]]
            (let [[r z] (tape base-radius base-z)]
              [(* r (cos (* edge θ)))
               (* r (sin (* edge θ)))
               z]))]
    (model/translate [0 0 (/ length -2)]
      (apply model/union
        ;; Unite a series of wedges, each modelled as a polyhedron.
        (reduce
          (fn [coll [rev edge]]
            (conj coll
              (model/polyhedron
                ;; Points, specified here as a tuple of normal radius, edge
                ;; number and normal z coordinate. The turner function produces
                ;; OpenSCAD’s 3-tuples of coordinates from this data.
                (map turner
                  [[0 edge
                    (* (dec rev) pitch)]
                   [rᵢ edge
                    (+ (* rev pitch) (* edge Δz) (- pitch))]
                   [rᵢ (inc edge)
                    (+ (* rev pitch) (* (inc edge) Δz) (- pitch))]
                   [0 0
                    (* rev pitch)]
                   [rₒ edge
                    (+ (* rev pitch) (* edge Δz) (/ pitch -2))]
                   [rₒ (inc edge)
                    (+ (* rev pitch) (* (inc edge) Δz) (/ pitch -2))]
                   [rᵢ edge
                    (+ (* rev pitch) (* edge Δz))]
                   [rᵢ (inc edge)
                    (+ (* rev pitch) (* (inc edge) Δz))]
                   [0 0
                    (* (inc rev) pitch)]])
                ;; Faces:
                [[1 0 3] [1 3 6] [6 3 8] [1 6 4] [0 1 2] [1 4 2] [2 4 5]
                 [5 4 6] [5 6 7] [7 6 8] [7 8 3] [0 2 3] [3 2 7] [7 2 5]])))
          []
          (into [] (for [r (range n-revolutions) e (range n-edges)] [r e])))))))

(defn hex
  "An extruded hexagon, as for a hex head or drive."
  [long-radius height]
  (model/rotate [0 0 (/ Math/PI 6)]
    (model/with-fn 6
      (model/cylinder long-radius height))))

(defn cone
  "A trivial conical point to a bolt. The characteristics of the cone
  follow ISO 7434 (i.e. 90° angle, assuming a long screw; the tip is not
  blunted), but it’s not intended for making slotted set screws, just to
  prevent slicing software from building support inside negatives in the
  bottom of an object, where support is difficult to remove."
  [{:keys [outer-diameter] :as options}]
  (model/cylinder [0 (bolt-inner-radius options)] (/ outer-diameter 2)))


;; The following three functions should rarely be needed in an application.
;; They control how a threaded fastener flares or tapers at its ends.

(defn rounding-taper
  "Close over a function to limit threading measurements as for either end of
  a threaded rod."
  [inner-radius outer-radius length]
  {:pre [(number? inner-radius)
         (number? outer-radius)
         (number? length)]}
  (let [distance-fn (distance-to-end length)
        flattener (flat-end-z length)]
    (fn [base-radius base-z]
      {:pre [(<= base-radius outer-radius)]}
      (let [distance (distance-fn base-z)]
        [(min base-radius (+ inner-radius distance))
         (flattener base-z)]))))

(defn flare
  "Close over a function to limit threading measurements as for the transition
  between the flat part of a long bolt and its threaded section, or the two
  sides of a nut.

  This permits a 1 μm overshoot to improve rendering of flared negatives
  inside hex nuts in OpenSCAD."
  [inner-radius outer-radius length]
  {:pre [(number? inner-radius)
         (number? outer-radius)
         (number? length)]}
  (let [distance-fn (distance-to-end length)
        flattener (flat-end-z length :overshoot 0.001)]
    (fn [base-radius base-z]
      ;; The closure will allow a radius of zero to be unchanged. This special
      ;; case is needed for the segments of a piece of threading to continue to
      ;; build from the middle even when the inner radius converges toward the
      ;; outer, keeping polyhedrons legal.
      {:pre [(<= base-radius outer-radius)]}
      (let [distance (distance-fn base-z)]
        [(if (zero? base-radius)
            0
            (max base-radius (- outer-radius (max 0 distance))))
         (flattener base-z)]))))

(defn bolt-taper
  "Close over a tapering function that goes inward from the outer radius at
  the top and inward again to the inner radius at the bottom, with a stretch
  of neutrality in the middle."
  [inner-radius outer-radius length]
  (let [bottom (rounding-taper inner-radius outer-radius length)
        top (flare inner-radius outer-radius length)
        transition (/ length 2)]
    (fn [base-radius base-z]
      (if (> base-z transition)
        (top base-radius base-z)
        (bottom base-radius base-z)))))

