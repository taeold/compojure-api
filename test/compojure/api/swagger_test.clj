(ns compojure.api.swagger-test
  (:require [schema.core :as s]
            [compojure.api.sweet :refer :all]
            [compojure.core :refer [defroutes]]
            [compojure.api.swagger :as swagger]
            [compojure.api.test-utils :refer :all]
            [midje.sweet :refer :all]
            [compojure.api.routing :as r]
            [compojure.api.routes :as routes]))

(defn extract-routes [app]
  (-> app r/get-routes routes/->ring-swagger :paths))

(defmacro optional-routes* [p & body] (when p `(routes* ~@body)))
(defmacro GET+ [p & body] `(GET* ~(str "/xxx" p) ~@body))

(fact "extracting compojure paths"

  (fact "all compojure.api.core macros are interpreted"
    (let [app (context* "/a" []
                (routes*
                  (context* "/b" []
                    (let-routes* []
                                 (GET* "/c" [] identity)
                                 (POST* "/d" [] identity)
                                 (PUT* "/e" [] identity)
                                 (DELETE* "/f" [] identity)
                                 (OPTIONS* "/g" [] identity)
                                 (PATCH* "/h" [] identity)))
                  (context* "/:i/:j" []
                    (GET* "/k/:l/m/:n" [] identity))))]

      (extract-routes app)
      => {"/a/b/c" {:get {}}
          "/a/b/d" {:post {}}
          "/a/b/e" {:put {}}
          "/a/b/f" {:delete {}}
          "/a/b/g" {:options {}}
          "/a/b/h" {:patch {}}
          "/a/:i/:j/k/:l/m/:n" {:get {:parameters {:path {:i String
                                                          :j String
                                                          :l String
                                                          :n String}}}}}))

  (fact "runtime code in route is NOT ignored"
    (extract-routes
      (context* "/api" []
        (if false
          (GET* "/true" [] identity)
          (PUT* "/false" [] identity)))) => {"/api/false" {:put {}}})

  (fact "route-macros are expanded"
    (extract-routes
      (context* "/api" []
        (optional-routes* true (GET* "/true" [] identity))
        (optional-routes* false (PUT* "/false" [] identity)))) => {"/api/true" {:get {}}})

  (fact "endpoint-macros are expanded"
    (extract-routes
      (context* "/api" []
        (GET+ "/true" [] identity))) => {"/api/xxx/true" {:get {}}})

  (fact "Vanilla Compojure defroutes are NOT followed"
    (ignore-non-documented-route-warning
      (defroutes even-more-routes (GET* "/even" [] identity))
      (defroutes more-routes (context* "/more" [] even-more-routes))
      (extract-routes
        (context* "/api" []
          (GET* "/true" [] identity)
          more-routes)) => {"/api/true" {:get {}}}))

  (fact "Compojure Api defroutes are followed"
    (defroutes* even-more-routes* (GET* "/even" [] identity))
    (defroutes* more-routes* (context* "/more" [] even-more-routes*))
    (extract-routes
      (context* "/api" []
        (GET* "/true" [] identity)
        more-routes*)) => {"/api/true" {:get {}}
                           "/api/more/even" {:get {}}})

  (fact "Parameter regular expressions are discarded"
    (extract-routes
      (context* "/api" []
        (GET* ["/:param" :param #"[a-z]+"] [] identity)))

    => {"/api/:param" {:get {:parameters {:path {:param String}}}}}))

(fact "context* meta-data"
  (extract-routes
    (context* "/api/:id" []
      :summary "top-summary"
      :path-params [id :- String]
      :tags [:kiss]
      (GET* "/kikka" []
        identity)
      (context* "/ipa" []
        :summary "mid-summary"
        :tags [:wasp]
        (GET* "/kukka/:kukka" []
          :summary "bottom-summary"
          :path-params [kukka :- String]
          :tags [:venom])
        (GET* "/kakka" []
          identity))))

  => {"/api/:id/kikka" {:get {:summary "top-summary"
                              :tags #{:kiss}
                              :parameters {:path {:id String}}}}
      "/api/:id/ipa/kukka/:kukka" {:get {:summary "bottom-summary"
                                         :tags #{:venom}
                                         :parameters {:path {:id String
                                                             :kukka String}}}}
      "/api/:id/ipa/kakka" {:get {:summary "mid-summary"
                                  :tags #{:wasp}
                                  :parameters {:path {:id String}}}}})

(facts "duplicate context merge"
  (let [app (routes*
              (context* "/api" []
                :tags [:kiss]
                (GET* "/kakka" []
                  identity))
              (context* "/api" []
                :tags [:kiss]
                (GET* "/kukka" []
                  identity)))]
    (extract-routes app)
    => {"/api/kukka" {:get {:tags #{:kiss}}}
        "/api/kakka" {:get {:tags #{:kiss}}}}))

(defroutes* r1
  (GET* "/:id" []
    :path-params [id :- s/Str]
    identity))
(defroutes* r2
  (GET* "/kukka/:id" []
    :path-params [id :- Long]
    identity))

(facts "defroutes* path-params"
  (extract-routes (routes* r1 r2))
  => {"/:id" {:get {:parameters {:path {:id String}}}}
      "/kukka/:id" {:get {:parameters {:path {:id Long}}}}})
