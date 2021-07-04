;;; DIN 562 square nuts.

(ns scad-klupe.din
  (:require [clojure.spec.alpha :as spec]
            [scad-clj.model :as model]
            [scad-tarmi.dfm :as dfm]
            [scad-klupe.base :as base]
            [scad-klupe.iso :as iso]
            [scad-klupe.schema.iso :as iso-schema]
            [scad-klupe.schema.din :as din-schema]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; INTERFACE FUNCTIONS — MINOR ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn datum
  "Retrieve or calculate a fact based on the DIN standards."
  [nominal-diameter key]
  {:pre [(spec/valid? ::iso-schema/m-diameter nominal-diameter)
         (spec/valid? ::din-schema/din-property key)]}
  (let [data (get din-schema/din-data nominal-diameter)]
   (if-let [value (get data key)]
     value
     (throw
       (ex-info "Unknown datum"
                {:nominal-diameter nominal-diameter
                 :requested-property key})))))


;;;;;;;;;;;;;;;;;;;;;;;;
;; INTERNAL FUNCTIONS ;;
;;;;;;;;;;;;;;;;;;;;;;;;

(defn- positive-body
  ;; Identical to iso/positive-body, thus far.
  ;; The two may be merged and moved to base if they do not soon diverge.
  [shape-fn {:keys [m-diameter compensator] :or {compensator dfm/none}
             :as shape-options}]
  (compensator m-diameter {:negative false}
    (-> shape-options (dissoc :compensator) (assoc :negative true) shape-fn)))


;;;;;;;;;;;;;;;;;;;;;;;;;
;; INTERFACE FUNCTIONS ;;
;;;;;;;;;;;;;;;;;;;;;;;;;

(defn square-thin-nut
  "A single DIN 562 square nut centred at [0 0 0].
  This function is patterned after the general ISO (hex) nut function.
  This includes the fact that the compensator is not applied to the height.
  That may change in a future version of scad-klupe, since DIN 562 specifies a
  “minimum” nut height; real-world samples are often thicker while remaining
  thin enough to be in danger of printer accuracy problems."
  [{:keys [m-diameter side height compensator negative]
    :or {compensator dfm/none}
    :as options}]
  {:pre [(spec/valid? ::iso-schema/m-diameter m-diameter)]}
  (let [side (compensator (or side (datum m-diameter :din562-side)))
        height (or height (datum m-diameter :din562-height))]
    (if negative
      (model/cube side side height)
      (model/difference
        (positive-body square-thin-nut options)
        (iso/rod (merge options
                   {:length height :taper-fn base/flare :negative true}))))))
