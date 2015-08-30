(ns examples.ordered
  (:require [schema.core :as s]
            [flatland.ordered.map :as fom]
            [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]))

;; does not work with AOT
(s/defschema Ordered
  (fom/ordered-map
    :a s/Str
    :b s/Str
    :c s/Str
    :d s/Str
    :e s/Str
    :f s/Str
    :g s/Str
    :h s/Str))

(defroutes* ordered-routes
  (context* "/ordered" []
    :tags ["ordered"]
    ;; Schema validation doesn't work yet for ordered-map
    #_
    (GET* "/ordered-returns" []
      :return Ordered
      :summary "Ordered data"
      (ok
        (fom/ordered-map
          :a "a"
          :b "b"
          :c "c"
          :d "d"
          :e "e"
          :f "f"
          :g "g"
          :h "h")))
    (GET* "/1" []
      (ok))
    (GET* "/2" []
      (ok))
    (GET* "/3" []
      (ok))
    (GET* "/4" []
      (ok))
    (GET* "/5" []
      (ok))
    (GET* "/6" []
      (ok))
    (GET* "/7" []
      (ok))
    (GET* "/8" []
      (ok))
    (GET* "/9" []
      (ok))
    (GET* "/10" []
      (ok))
    ))
