(ns katello.tests.setup
  (:require [fn.trace :as tr]
            (katello [tasks :as tasks]
                     [conf :as conf]
                     [api-tasks :as api-tasks]) 
            [test.tree :as test])
  (:use [clojure.contrib.string :only [split]]
        [com.redhat.qe.auto.selenium.selenium :only [connect new-sel browser sel]]))

(defn new-selenium [& [single-thread]]
  (let [sel-addr (@conf/config :selenium-address)
        [host port] (split #":" sel-addr)
        sel-fn (if single-thread
                 connect
                 new-sel)] 
    (sel-fn host (Integer/parseInt port) "" (@conf/config :server-url))))

(defn start-selenium []  
  (browser start)
  (browser open (@conf/config :server-url))
  (tasks/login (@conf/config :admin-user) (@conf/config :admin-password)))

(defn switch-new-admin-user [user pw]
  (tasks/create-user user {:password pw })
  (tasks/assign-role {:user user
                      :roles ["Administrator"]})
  (tasks/logout)
  (tasks/login user pw))

(defn stop-selenium []
  (browser stop))

(defn thread-runner [consume-fn]
  (fn [] (binding [sel (new-selenium)
                  tr/tracer (tr/per-thread-tracer tr/clj-format)
                  katello.conf/*session-user* (str (tasks/uniqueify (.getName (Thread/currentThread))))]
          (tr/dotrace-all {:namespaces [katello.tasks katello.api-tasks]
                           :fns [test/execute
                                 start-selenium stop-selenium switch-new-admin-user]
                           :exclude [katello.tasks/notification
                                     katello.tasks/clear-all-notifications
                                     katello.tasks/success?]}
                          (println "starting a sel")
                          (try (start-selenium)
                               (switch-new-admin-user conf/*session-user* conf/*session-password*)
                               (catch Exception e (.printStackTrace e)))
                          (consume-fn)
                          (stop-selenium)
                          (tr/htmlify "html" [(str (.getName (Thread/currentThread)) ".trace")]
                                      "http://hudson.rhq.lab.eng.bos.redhat.com:8080/shared/syntaxhighlighter/")))))

(def runner-config 
  {:thread-runner thread-runner
   :setup (fn [] (println "initializing.") (conf/init))})
