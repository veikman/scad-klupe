;;; ISO 262/4017 fasteners and ISO 7089 washers.

(ns scad-klupe.iso
  (:require [clojure.spec.alpha :as spec]
            [scad-clj.model :as model]
            [scad-tarmi.core :refer [sin cos τ long-hex-diagonal]]
            [scad-tarmi.maybe :as maybe]
            [scad-tarmi.dfm :as dfm]
            [scad-klupe.base :as base]
            [scad-klupe.schema.iso :as schema]))


;;;;;;;;;;;;;;;
;; CONSTANTS ;;
;;;;;;;;;;;;;;;

(def standard-threading-angle
  "The default angle of threading in this module is ISO 262’s 60 degrees,
  approximated by 1.0472 radians. For easier printing, consider a lower,
  standards-noncompliant value."
  1.0472)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; INTERFACE ACCESSOR TO CONSTANTS ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn datum
  "Retrieve or calculate a fact based on the ISO standards."
  [nominal-diameter key]
  {:pre [(spec/valid? ::schema/m-diameter nominal-diameter)
         (spec/valid? ::schema/iso-property key)]}
  (let [data (get schema/iso-data nominal-diameter)]
   (case key
     :hex-head-short-diagonal  ; Flat-to-flat width of a hex head.
       ;; For most sizes, this value is equal to socket diameter.
       (get data key (datum nominal-diameter :socket-diameter))
     :hex-head-long-diagonal  ; Corner-to-corner diameter of a hex head.
       (long-hex-diagonal
         (datum nominal-diameter :hex-head-short-diagonal))
     :head-hex-drive-long-diagonal
       (long-hex-diagonal
         (datum nominal-diameter :head-hex-drive-short-diagonal))
     :socket-height
       nominal-diameter
     :button-diameter
       (* 1.75 nominal-diameter)
     :button-height
       (* 0.55 nominal-diameter)
     :countersunk-diameter
       (* 2 nominal-diameter)
     :countersunk-height
       ;; Nominal chamfer is 89.9°. Treated here as 90°.
       (/ nominal-diameter 2)
     (if-let [value (get data key)]
       value
       (throw
         (ex-info "Unknown datum"
                  {:nominal-diameter nominal-diameter
                   :requested-property key}))))))

(defn head-length
  "Get the axial length of an ISO bolt head.
  This is more commonly thought of as a height, especially given the
  vertical orientation of the model output by the bolt function. The word
  “length” is intended to provide conceptual continuity with the more intuitive
  bolt-length function, below, as well as bolt parameter names.
  This is exposed for predicting the results of the bolt function in this
  module, specifically where the transition from head to body will occur."
  [m-diameter head-type]
  {:pre [(spec/valid? ::schema/m-diameter m-diameter)
         (spec/valid? ::schema/head-type head-type)]}
  (datum m-diameter
    (case head-type
      :hex :iso4017-hex-head-length-nominal
      :socket :socket-height
      :button :button-height
      :countersunk :countersunk-height)))

(defn bolt-length
  "Get the projected length of an ISO bolt, including the head.
  This is exposed for predicting the results of the bolt function in this
  module. Unlike the base/bolt-length function, it takes the same parameters
  as the bolt function."
  [{:keys [m-diameter total-length unthreaded-length threaded-length head-type]
    :as options}]
  {:pre [(spec/valid? ::schema/bolt-parameters options)]}
  (base/bolt-length {:total total-length,
                     :unthreaded unthreaded-length, :threaded threaded-length,
                     :head (head-length m-diameter head-type)}))


;;;;;;;;;;;;;;;;;;;;;;;;
;; INTERNAL FUNCTIONS ;;
;;;;;;;;;;;;;;;;;;;;;;;;

(defn- positive-body
  "Build the main body of the positive version of a fastener.
  Apply a compensator to the shape as a whole but discard that compensator
  so it isn’t reused by the shape function itself. Similarly, describe the
  shape as a negative, so that it will not recurse to include those features
  that will be subtracted from the positive body by the caller."
  [shape-fn {:keys [m-diameter compensator] :or {compensator dfm/none}
             :as shape-options}]
  (compensator m-diameter {:negative false}
    (-> shape-options (dissoc :compensator) (assoc :negative true) shape-fn)))

(defn- hex-item
  ([m-diameter height]
   (hex-item m-diameter height :hex-head-long-diagonal))
  ([m-diameter height diagonal-datum]
   (base/hex (/ (datum m-diameter diagonal-datum) 2) height)))

(defn- bolt-head
  "A model of the head of a bolt, without a drive.
  This function takes an auxiliary ‘countersink-edge-fn’ which computes the
  thickness of a countersunk head at its edge. The computed thickness will,
  effectively, lengthen the head, potentially producing a negative that is too
  shallow for the threaded portion of a real screw.
  The default ‘countersink-edge-fn’ is a slight exaggeration intended
  to make sure the head will not protrude with normal printing defects."
  [{:keys [m-diameter head-type countersink-edge-fn compensator]
    :or {countersink-edge-fn (fn [m-diameter] (/ (Math/log m-diameter) 8))}}]
  {:pre [(spec/valid? ::schema/m-diameter m-diameter)
         (spec/valid? ::schema/head-type head-type)]}
  (let [height (head-length m-diameter head-type)]
    (case head-type
      :hex
        (compensator (datum m-diameter :hex-head-long-diagonal) {}
          (hex-item m-diameter height))
      :socket
        (let [diameter (datum m-diameter :socket-diameter)]
          (model/cylinder (/ (compensator diameter) 2) height))
      :button
        (let [diameter (datum m-diameter :button-diameter)]
          (model/cylinder (/ (compensator diameter) 2) height))
      :countersunk
        (let [diameter (datum m-diameter :countersunk-diameter)
              edge (countersink-edge-fn m-diameter)]
          (model/hull
            (model/translate [0 0 (+ (/ edge -2) (/ height 2))]
              (model/cylinder (/ (compensator diameter) 2) edge))
            (model/translate [0 0 (+ (/ edge -2) (/ height -2))]
              (model/cylinder (/ (compensator m-diameter) 2) edge)))))))

(defn- bolt-drive
  "A model of the thing you stick your bit in."
  [{:keys [m-diameter head-type drive-type drive-recess-depth]}]
  {:pre [(spec/valid? ::schema/m-diameter m-diameter)
         (spec/valid? ::schema/drive-type drive-type)]}
  (let [depth (or drive-recess-depth
                  (/ (head-length m-diameter head-type) 2))]
    (model/translate [0 0 (/ depth -2)]
      (case drive-type
        :hex (hex-item m-diameter depth :head-hex-drive-long-diagonal)))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; INTERFACE FUNCTIONS — MAJOR ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn rod
  "A rod centred at [0 0 0].
  By default, this is a threaded rod. However, if the “include-threading”
  keyword parameter is false, the rod will instead be a plain cylinder. If
  marked as negative, such an unthreaded rod will be shrunk for tapping the
  hole it makes, after printing. Otherwise, it will be left at regular
  DFM-nominal size for threading with a die."
  [{:keys [m-diameter length taper-fn include-threading compensator negative]
    :or {taper-fn base/rounding-taper, include-threading true,
         negative false, compensator dfm/none}
    :as options}]
  {:pre [(spec/valid? ::schema/m-diameter m-diameter)]}
  (let [options (merge {:outer-diameter m-diameter
                        :pitch (datum m-diameter :thread-pitch-coarse)
                        :angle standard-threading-angle
                        :taper-fn taper-fn}
                       options)]
    (compensator m-diameter {:negative negative}
      (if include-threading
        (base/threading options)
        (model/cylinder
          (if negative (base/bolt-inner-radius options) (/ m-diameter 2))
          length)))))

(defn bolt
  "A model of an ISO metric bolt.
  The very top of the head sits at [0 0 0] with the bolt pointing down.
  The total length of the bolt is the sum of head height (computed from
  nominal ISO size), unthreaded and threaded length parameters, plus an
  optional point.
  Though a drive-type parameter is accepted, only a socket-cap-style hex
  drive is supported, and even that will be ignored on a negative.
  Likewise, though a point-type parameter is accepted, the only implemented
  option beyond the default flat point is a cone."
  [{:keys [m-diameter angle total-length unthreaded-length threaded-length
           head-type drive-type point-type
           include-threading negative compensator]
    :or {angle standard-threading-angle, include-threading true,
         negative false, compensator dfm/none}
    :as options}]
  {:pre [(spec/valid? ::schema/bolt-parameters options)]}
  (let [hh (head-length m-diameter head-type)
        lengths (base/shank-section-lengths
                  {:total total-length, :unthreaded unthreaded-length,
                   :threaded threaded-length, :head hh})
        [unthreaded-length threaded-length] lengths
        merged (merge
                 {:outer-diameter m-diameter
                  :pitch (datum m-diameter :thread-pitch-coarse)
                  :angle angle
                  :compensator compensator}
                 options
                 {:unthreaded-length unthreaded-length
                  :threaded-length threaded-length})
        r (/ m-diameter 2)]
    (if negative
      (model/union
        (model/translate [0 0 (/ hh -2)]
          (bolt-head merged))
        (compensator m-diameter {}
          (when (pos? unthreaded-length)
            (model/translate [0 0 (- (- hh) (/ unthreaded-length 2))]
              (model/cylinder r unthreaded-length)))
          (when (pos? threaded-length)
            (model/translate
              [0 0 (- (- (+ hh unthreaded-length)) (/ threaded-length 2))]
              (rod {:m-diameter m-diameter
                    :length threaded-length
                    :angle angle
                    :taper-fn base/bolt-taper
                    :include-threading include-threading
                    :negative negative})))
          (when (= point-type :cone)
            (model/translate
              [0 0 (- (- (+ hh unthreaded-length threaded-length))
                      (/ (base/bolt-inner-radius merged) 2))]
              (base/cone merged)))))
      ;; Else a positive. Consider subtracting a drive from the head.
      (maybe/difference
        (positive-body bolt merged)
        (when drive-type
          (compensator m-diameter {}
            (bolt-drive merged)))))))

(defn nut
  "A single hex nut centred at [0 0 0]."
  [{:keys [m-diameter height compensator negative]
    :or {compensator dfm/none}
    :as options}]
  {:pre [(spec/valid? ::schema/m-diameter m-diameter)]}
  (let [height (or height (datum m-diameter :hex-nut-height))]
    (if negative
      ;; A convex model of a nut.
      (compensator (datum m-diameter :hex-head-long-diagonal) {}
        (hex-item m-diameter height))
      ;; A more complete model.
      (model/difference
        ;; Recurse to make the positive model.
        (positive-body nut options)
        ;; Cut out the threading.
        (rod (merge options
               {:length height :taper-fn base/flare :negative true}))))))

(defn washer
  "A flat, round washer centred at [0 0 0]."
  [{:keys [m-diameter inner-diameter outer-diameter height]}]
  (let [id (or inner-diameter (datum m-diameter :iso7089-inner-diameter))
        od (or outer-diameter (datum m-diameter :iso7089-outer-diameter))
        thickness (or height (datum m-diameter :iso7089-thickness))]
    (model/difference
      (model/cylinder (/ od 2) thickness)
      (model/cylinder (/ id 2) (+ thickness 1)))))

